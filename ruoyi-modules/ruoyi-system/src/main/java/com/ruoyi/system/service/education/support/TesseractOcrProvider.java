package com.ruoyi.system.service.education.support;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.system.config.EduQbOcrProperties;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.system.domain.education.EduQbOcrLine;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;

@Component("tesseractOcrProvider")
public class TesseractOcrProvider implements OcrProvider
{
    private static final Logger log = LoggerFactory.getLogger(TesseractOcrProvider.class);

    private static final BigDecimal DEFAULT_CONFIDENCE = new BigDecimal("0.6500");

    private static final int[] PSM_CANDIDATES = { 6, 4, 3, 11 };

    private static final OcrImagePreprocessor.Mode[] PREPROCESS_MODES = {
        OcrImagePreprocessor.Mode.SOFT,
        OcrImagePreprocessor.Mode.BINARY,
        OcrImagePreprocessor.Mode.RAW
    };

    @Autowired
    private EduQbOcrProperties ocrProperties;

    @Override
    public List<EduQbOcrLine> recognize(byte[] imageBytes)
    {
        EduQbOcrProperties.TesseractConfig cfg = ocrProperties.getTesseract();
        if (!cfg.isConfigured())
        {
            throw new ServiceException(buildNotConfiguredMessage(cfg));
        }
        try
        {
            BufferedImage raw = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (raw == null)
            {
                throw new ServiceException("\u65e0\u6cd5\u89e3\u6790\u56fe\u7247\u683c\u5f0f");
            }

            RecognizeAttempt best = null;
            OcrImagePreprocessor.Mode[] modes = cfg.isPreprocessEnabled()
                    ? PREPROCESS_MODES
                    : new OcrImagePreprocessor.Mode[] { OcrImagePreprocessor.Mode.RAW };

            for (OcrImagePreprocessor.Mode mode : modes)
            {
                BufferedImage image = OcrImagePreprocessor.prepare(raw, cfg.getMinWidth(), mode);
                for (int psm : resolvePsmCandidates(cfg))
                {
                    Tesseract tess = createTesseract(cfg, psm);
                    RecognizeAttempt attempt = recognizeOnce(tess, image, cfg, psm, mode.name());
                    if (attempt == null || attempt.lines.isEmpty())
                    {
                        continue;
                    }
                    if (best == null || attempt.qualityScore.compareTo(best.qualityScore) > 0)
                    {
                        best = attempt;
                    }
                }
            }

            if (best == null || best.lines.isEmpty())
            {
                throw new ServiceException("\u672a\u8bc6\u522b\u5230\u6587\u5b57\uff0c\u8bf7\u6362\u5f20\u66f4\u6e05\u6670\u7684\u56fe\u7247\u6216\u624b\u52a8\u5f55\u5165");
            }
            log.info("Tesseract OCR best mode={} psm={} lines={} score={} avgConf={}",
                    best.preprocessMode, best.psm, best.lines.size(), best.qualityScore, best.avgConfidence);
            return best.lines;
        }
        catch (TesseractException ex)
        {
            log.error("Tesseract OCR failed", ex);
            throw new ServiceException("\u672c\u5730 OCR \u8bc6\u522b\u5931\u8d25\uff1a" + ex.getMessage());
        }
        catch (ServiceException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            log.error("Tesseract OCR unexpected error", ex);
            throw new ServiceException("\u672c\u5730 OCR \u8bc6\u522b\u5931\u8d25\uff1a" + ex.getMessage());
        }
    }

    private int[] resolvePsmCandidates(EduQbOcrProperties.TesseractConfig cfg)
    {
        int configured = cfg.getPageSegMode();
        List<Integer> list = new ArrayList<>();
        list.add(configured);
        for (int psm : PSM_CANDIDATES)
        {
            if (psm != configured)
            {
                list.add(psm);
            }
        }
        return list.stream().mapToInt(Integer::intValue).toArray();
    }

    private RecognizeAttempt recognizeOnce(Tesseract tess, BufferedImage image,
            EduQbOcrProperties.TesseractConfig cfg, int psm, String preprocessMode) throws TesseractException
    {
        List<LineBox> boxes = extractLineBoxes(tess, image);
        List<EduQbOcrLine> lines;
        if (boxes.isEmpty())
        {
            lines = fallbackFullText(tess, image);
        }
        else
        {
            lines = orderByLayout(boxes, image.getWidth(), cfg.getColumnSplitRatio());
        }
        lines = filterNoiseLines(lines);
        BigDecimal avg = averageConfidence(lines);
        BigDecimal score = qualityScore(lines, avg);
        return new RecognizeAttempt(psm, preprocessMode, lines, avg, score);
    }

    private BigDecimal qualityScore(List<EduQbOcrLine> lines, BigDecimal avgConf)
    {
        if (lines == null || lines.isEmpty())
        {
            return BigDecimal.ZERO;
        }
        int lineCount = lines.size();
        int chinese = 0;
        int options = 0;
        for (EduQbOcrLine line : lines)
        {
            if (line == null || line.getText() == null)
            {
                continue;
            }
            String t = line.getText();
            for (char c : t.toCharArray())
            {
                if (c >= 0x4E00 && c <= 0x9FFF)
                {
                    chinese++;
                }
            }
            if (t.matches("^[A-Da-d][\\.\\uFF0E\\u3001\\)\\uFF09:].*"))
            {
                options++;
            }
        }
        double confPart = avgConf != null ? avgConf.doubleValue() : 0;
        double linePart = Math.min(lineCount / 6.0, 1.0);
        double cnPart = Math.min(chinese / 40.0, 1.0);
        double optPart = Math.min(options / 4.0, 1.0);
        double score = confPart * 0.35 + linePart * 0.25 + cnPart * 0.25 + optPart * 0.15;
        return BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);
    }

    private Tesseract createTesseract(EduQbOcrProperties.TesseractConfig cfg, int pageSegMode)
            throws TesseractException
    {
        Tesseract tess = new Tesseract();
        tess.setDatapath(cfg.resolveDatapath());
        tess.setLanguage(cfg.getLanguage());
        tess.setPageSegMode(pageSegMode);
        tess.setOcrEngineMode(1);
        if (cfg.getDpi() > 0)
        {
            tess.setTessVariable("user_defined_dpi", String.valueOf(cfg.getDpi()));
        }
        tess.setTessVariable("preserve_interword_spaces", "1");
        return tess;
    }

    private List<LineBox> extractLineBoxes(Tesseract tess, BufferedImage image) throws TesseractException
    {
        List<Word> words = tess.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_TEXTLINE);
        List<LineBox> boxes = new ArrayList<>();
        if (words == null)
        {
            return boxes;
        }
        for (Word word : words)
        {
            if (word == null || word.getText() == null)
            {
                continue;
            }
            String text = normalizeText(word.getText());
            if (text.isEmpty())
            {
                continue;
            }
            Rectangle rect = word.getBoundingBox();
            int y = rect != null ? rect.y : boxes.size() * 20;
            int x = rect != null ? rect.x : 0;
            int w = rect != null ? rect.width : 0;
            boxes.add(new LineBox(text, toConfidence(word.getConfidence()), y, x, w));
        }
        return boxes;
    }

    private List<EduQbOcrLine> orderByLayout(List<LineBox> boxes, int imageWidth, double columnSplitRatio)
    {
        if (boxes.isEmpty())
        {
            return List.of();
        }
        int splitX = (int) Math.round(imageWidth * columnSplitRatio);
        List<LineBox> left = new ArrayList<>();
        List<LineBox> right = new ArrayList<>();
        for (LineBox box : boxes)
        {
            int centerX = box.x + box.width / 2;
            if (centerX < splitX)
            {
                left.add(box);
            }
            else
            {
                right.add(box);
            }
        }
        Comparator<LineBox> byY = Comparator.comparingInt((LineBox b) -> b.y).thenComparingInt(b -> b.x);
        left.sort(byY);
        right.sort(byY);

        List<EduQbOcrLine> result = new ArrayList<>();
        for (LineBox box : left)
        {
            result.add(box.toLine());
        }
        for (LineBox box : right)
        {
            result.add(box.toLine());
        }
        if (result.size() < 3 && boxes.size() >= 3)
        {
            boxes.sort(byY);
            result.clear();
            for (LineBox box : boxes)
            {
                result.add(box.toLine());
            }
        }
        return result;
    }

    private List<EduQbOcrLine> filterNoiseLines(List<EduQbOcrLine> lines)
    {
        List<EduQbOcrLine> filtered = new ArrayList<>();
        for (EduQbOcrLine line : lines)
        {
            if (line == null || line.getText() == null)
            {
                continue;
            }
            String text = line.getText().trim();
            if (text.isEmpty() || isLikelyNoise(text, line.getConfidence()))
            {
                continue;
            }
            filtered.add(line);
        }
        return filtered;
    }

    private boolean isLikelyNoise(String text, BigDecimal confidence)
    {
        int chinese = 0;
        int digits = 0;
        int letters = 0;
        for (char c : text.toCharArray())
        {
            if (Character.isWhitespace(c))
            {
                continue;
            }
            if (c >= 0x4E00 && c <= 0x9FFF)
            {
                chinese++;
            }
            else if (Character.isDigit(c))
            {
                digits++;
            }
            else if (Character.isLetter(c))
            {
                letters++;
            }
        }
        int meaningful = chinese + digits + letters;
        if (meaningful == 0)
        {
            return true;
        }
        if (text.length() <= 2 && digits >= 1 && chinese == 0)
        {
            return false;
        }
        if (confidence != null && confidence.doubleValue() >= 0.72)
        {
            return false;
        }
        if (chinese == 0 && letters >= 5 && confidence != null && confidence.doubleValue() < 0.5)
        {
            return true;
        }
        return meaningful <= 1 && confidence != null && confidence.doubleValue() < 0.4;
    }

    private List<EduQbOcrLine> fallbackFullText(Tesseract tess, BufferedImage image) throws TesseractException
    {
        String full = tess.doOCR(image);
        List<EduQbOcrLine> lines = new ArrayList<>();
        if (full == null || full.isBlank())
        {
            return lines;
        }
        for (String part : full.split("\\R"))
        {
            String text = normalizeText(part);
            if (!text.isEmpty())
            {
                lines.add(new EduQbOcrLine(text, DEFAULT_CONFIDENCE));
            }
        }
        return lines;
    }

    private String normalizeText(String text)
    {
        if (text == null)
        {
            return "";
        }
        return text.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    private BigDecimal toConfidence(float confidence)
    {
        if (confidence <= 0f)
        {
            return DEFAULT_CONFIDENCE;
        }
        return BigDecimal.valueOf(confidence / 100f).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal averageConfidence(List<EduQbOcrLine> lines)
    {
        if (lines == null || lines.isEmpty())
        {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (EduQbOcrLine line : lines)
        {
            if (line != null && line.getConfidence() != null)
            {
                sum = sum.add(line.getConfidence());
                count++;
            }
        }
        if (count == 0)
        {
            return BigDecimal.ZERO;
        }
        return sum.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
    }

    private String buildNotConfiguredMessage(EduQbOcrProperties.TesseractConfig cfg)
    {
        String path = cfg.resolveDatapath();
        if (path == null || path.isBlank())
        {
            return "\u672c\u5730 Tesseract \u672a\u627e\u5230 tessdata \u76ee\u5f55\uff0c\u8bf7\u5b89\u88c5 Tesseract \u5e76\u914d\u7f6e edu.qb.ocr.tesseract.datapath \u6216\u73af\u5883\u53d8\u91cf TESSDATA_PREFIX";
        }
        return "\u672c\u5730 Tesseract \u672a\u5c31\u7eea\uff08\u76ee\u5f55 " + path
                + "\uff09\uff0c\u8bf7\u4e0b\u8f7d\u8bed\u8a00\u5305 " + cfg.getLanguage()
                + " \u5230 tessdata \u76ee\u5f55\uff08\u5982 chi_sim.traineddata\uff09";
    }

    private static final class LineBox
    {
        private final String text;
        private final BigDecimal confidence;
        private final int y;
        private final int x;
        private final int width;

        private LineBox(String text, BigDecimal confidence, int y, int x, int width)
        {
            this.text = text;
            this.confidence = confidence;
            this.y = y;
            this.x = x;
            this.width = width;
        }

        private EduQbOcrLine toLine()
        {
            return new EduQbOcrLine(text, confidence);
        }
    }

    private static final class RecognizeAttempt
    {
        private final int psm;
        private final String preprocessMode;
        private final List<EduQbOcrLine> lines;
        private final BigDecimal avgConfidence;
        private final BigDecimal qualityScore;

        private RecognizeAttempt(int psm, String preprocessMode, List<EduQbOcrLine> lines,
                BigDecimal avgConfidence, BigDecimal qualityScore)
        {
            this.psm = psm;
            this.preprocessMode = preprocessMode;
            this.lines = lines;
            this.avgConfidence = avgConfidence;
            this.qualityScore = qualityScore;
        }
    }
}
