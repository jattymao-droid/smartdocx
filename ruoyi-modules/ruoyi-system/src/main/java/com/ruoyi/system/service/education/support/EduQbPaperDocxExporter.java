package com.ruoyi.system.service.education.support;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.utils.file.ImageUtils;
import com.ruoyi.system.domain.education.EduQbPaperPreviewRequest;

public final class EduQbPaperDocxExporter
{
    private EduQbPaperDocxExporter()
    {
    }

    public static void renderToFile(EduQbPaperPreviewRequest request, List<EduQbPaperDocxBlock> blocks, File outputFile)
            throws Exception
    {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists())
        {
            Files.createDirectories(parent.toPath());
        }
        try (OutputStream os = new FileOutputStream(outputFile))
        {
            renderToStream(request, blocks, os);
        }
    }

    public static void renderToStream(EduQbPaperPreviewRequest request, List<EduQbPaperDocxBlock> blocks, OutputStream os)
            throws Exception
    {
        try (XWPFDocument document = new XWPFDocument())
        {
            applyPageLayout(document, request);
            appendHeader(document, request, blocks);
            String lastSection = null;
            for (EduQbPaperDocxBlock block : blocks)
            {
                if (StringUtils.isNotEmpty(block.getSectionTitle()) && !block.getSectionTitle().equals(lastSection))
                {
                    appendSection(document, block.getSectionTitle());
                    lastSection = block.getSectionTitle();
                }
                appendQuestion(document, block);
            }
            document.write(os);
        }
    }

    public static void writeHtmlFile(String html, File outputFile) throws Exception
    {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists())
        {
            Files.createDirectories(parent.toPath());
        }
        Files.write(outputFile.toPath(), html.getBytes(StandardCharsets.UTF_8));
    }

    private static void applyPageLayout(XWPFDocument document, EduQbPaperPreviewRequest request)
    {
        // POI 4.1.2 lite schemas omit CTPageMar/CTPageSz; apply visual spacing on the first block instead.
        if (document == null)
        {
            return;
        }
        XWPFParagraph spacer = document.createParagraph();
        spacer.setSpacingBefore(0);
        spacer.setSpacingAfter(120);
    }

    private static void appendHeader(XWPFDocument document, EduQbPaperPreviewRequest request, List<EduQbPaperDocxBlock> blocks)
    {
        java.util.Map<String, String> header = request.getHeader() != null ? request.getHeader() : new java.util.HashMap<>();
        String schoolName = header.getOrDefault("schoolName", "");
        if (StringUtils.isNotEmpty(schoolName))
        {
            XWPFParagraph p = document.createParagraph();
            p.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun run = p.createRun();
            run.setBold(true);
            run.setFontSize(14);
            run.setFontFamily("SimSun");
            run.setText(schoolName);
        }
        String title = resolveTitle(request, header);
        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setBold(true);
        titleRun.setFontSize(16);
        titleRun.setFontFamily("SimSun");
        titleRun.setText(title);

        BigDecimal totalScore = blocks.stream()
                .map(EduQbPaperDocxBlock::getScoreValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        StringBuilder meta = new StringBuilder();
        String subjectName = header.getOrDefault("subjectName", "");
        String duration = header.getOrDefault("duration", "");
        if (StringUtils.isNotEmpty(subjectName))
        {
            meta.append("\u79d1\u76ee\uff1a").append(subjectName).append("    ");
        }
        if (StringUtils.isNotEmpty(duration))
        {
            meta.append("\u65f6\u95f4\uff1a").append(duration).append("    ");
        }
        meta.append("\u6ee1\u5206\uff1a").append(totalScore.stripTrailingZeros().toPlainString());
        XWPFParagraph metaPara = document.createParagraph();
        metaPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun metaRun = metaPara.createRun();
        metaRun.setFontSize(12);
        metaRun.setFontFamily("SimSun");
        metaRun.setText(meta.toString());
    }

    private static String resolveTitle(EduQbPaperPreviewRequest request, java.util.Map<String, String> header)
    {
        if (StringUtils.isNotEmpty(request.getPaperTitle()))
        {
            return request.getPaperTitle();
        }
        String examTitle = header.getOrDefault("examTitle", "");
        if (StringUtils.isNotEmpty(examTitle))
        {
            return examTitle;
        }
        return "\u8bd5\u5377";
    }

    private static void appendSection(XWPFDocument document, String sectionTitle)
    {
        XWPFParagraph p = document.createParagraph();
        p.setSpacingBefore(200);
        XWPFRun run = p.createRun();
        run.setBold(true);
        run.setFontSize(12);
        run.setFontFamily("SimSun");
        run.setText(sectionTitle);
    }

    private static void appendQuestion(XWPFDocument document, EduQbPaperDocxBlock block) throws Exception
    {
        XWPFParagraph p = document.createParagraph();
        p.setSpacingBefore(120);
        XWPFRun prefixRun = p.createRun();
        prefixRun.setFontSize(12);
        prefixRun.setFontFamily("SimSun");
        prefixRun.setText(block.getQuestionNo() + ". ");
        boolean htmlContent = EduQbQuestionContentSupport.isHtmlContent(block.getContent());
        if (htmlContent)
        {
            EduQbHtmlDocxRenderer.appendHtmlStem(document, p, block.getContent(), 12);
        }
        else
        {
            EduQbFormulaRenderer.appendRichText(p, block.getContent(), 12);
        }
        XWPFParagraph scorePara = document.getParagraphs().get(document.getParagraphs().size() - 1);
        XWPFRun scoreRun = scorePara.createRun();
        scoreRun.setFontSize(12);
        scoreRun.setFontFamily("SimSun");
        scoreRun.setText(" \uff08" + block.getScoreValue().stripTrailingZeros().toPlainString() + "\u5206\uff09");
        if (!htmlContent)
        {
            appendImages(document, block.getImageUrls());
        }
        if (block.getOptions() != null)
        {
            for (String option : block.getOptions())
            {
                XWPFParagraph opt = document.createParagraph();
                opt.setIndentationLeft(480);
                EduQbFormulaRenderer.appendRichText(opt, option, 12);
            }
        }
        if (StringUtils.isNotEmpty(block.getAnswerLine()))
        {
            XWPFParagraph ans = document.createParagraph();
            ans.setIndentationLeft(480);
            XWPFRun ansRun = ans.createRun();
            ansRun.setFontSize(11);
            ansRun.setFontFamily("SimSun");
            ansRun.setColor("CC0000");
            ansRun.setText(block.getAnswerLine());
        }
    }

    private static void appendImages(XWPFDocument document, List<String> imageUrls)
    {
        if (imageUrls == null || imageUrls.isEmpty())
        {
            return;
        }
        for (String url : imageUrls)
        {
            if (StringUtils.isEmpty(url))
            {
                continue;
            }
            byte[] bytes = ImageUtils.readFile(url.trim());
            if (bytes == null || bytes.length == 0)
            {
                continue;
            }
            try
            {
                XWPFParagraph imgPara = document.createParagraph();
                imgPara.setIndentationLeft(480);
                XWPFRun imgRun = imgPara.createRun();
                try (InputStream in = new java.io.ByteArrayInputStream(bytes))
                {
                    imgRun.addPicture(in, pictureType(url), "figure", Units.toEMU(280), Units.toEMU(200));
                }
            }
            catch (Exception ignored)
            {
                // skip broken image
            }
        }
    }

    private static int pictureType(String path)
    {
        String lower = path.toLowerCase();
        if (lower.endsWith(".png"))
        {
            return Document.PICTURE_TYPE_PNG;
        }
        if (lower.endsWith(".gif"))
        {
            return Document.PICTURE_TYPE_GIF;
        }
        if (lower.endsWith(".bmp"))
        {
            return Document.PICTURE_TYPE_BMP;
        }
        return Document.PICTURE_TYPE_JPEG;
    }
}
