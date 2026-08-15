package com.ruoyi.system.service.education.impl;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.system.config.EduQbOcrProperties;
import com.ruoyi.system.service.education.support.EduQbLocalFileSupport;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.service.education.support.EduQbSecuritySupport;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.service.education.support.EduQbFileUploadUtils;
import com.ruoyi.common.core.utils.file.FileUtils;
import com.ruoyi.system.domain.education.EduQbConstants;
import com.ruoyi.system.domain.education.EduQbOcrCommitBody;
import com.ruoyi.system.domain.education.EduQbOcrDraft;
import com.ruoyi.system.domain.education.EduQbOcrLine;
import com.ruoyi.system.domain.education.EduQbQuestion;
import com.ruoyi.system.domain.education.EduSubject;
import com.ruoyi.system.mapper.education.EduQbOcrDraftMapper;
import com.ruoyi.system.mapper.education.EduSubjectMapper;
import com.ruoyi.system.service.education.IEduQbOcrService;
import com.ruoyi.system.service.education.IEduQbQuestionService;
import com.ruoyi.system.service.education.support.EduQbQuestionPredictService;
import com.ruoyi.system.service.education.support.EduQbQuestionPredictService.PredictResult;
import com.ruoyi.system.service.education.support.OcrProvider;
import com.ruoyi.system.service.education.support.OcrSidecarResult;
import com.ruoyi.system.service.education.support.PaddleOcrProvider;

@Service
public class EduQbOcrServiceImpl implements IEduQbOcrService
{
    private static final Logger log = LoggerFactory.getLogger(EduQbOcrServiceImpl.class);

    private static final BigDecimal CONFIDENCE_WARN = new BigDecimal("0.8000");

    private static final String STUB_WARNING = "\u5f53\u524d\u4f7f\u7528\u5f00\u53d1\u5360\u4f4d OCR\uff0c\u8bc6\u522b\u7ed3\u679c\u4e3a\u56fa\u5b9a\u793a\u4f8b\u6587\u672c\u3002"
            + "\u8bf7\u542f\u52a8 PaddleOCR \u670d\u52a1\uff08paddleocr-service/start.ps1\uff09\u3001\u914d\u7f6e\u767e\u5ea6 OCR\uff08BAIDU_OCR_API_KEY\uff09"
            + "\u6216\u5b89\u88c5 Tesseract\uff08chi_sim\uff09\u540e\u91cd\u542f\u670d\u52a1\u3002";

    @Autowired
    private EduQbOcrDraftMapper ocrDraftMapper;

    @Autowired
    private EduSubjectMapper subjectMapper;

    @Autowired
    private IEduQbQuestionService questionService;

    @Autowired
    private EduQbQuestionPredictService predictService;

    @Autowired
    private EduQbOcrProperties ocrProperties;

    @Autowired
    @Qualifier("stubOcrProvider")
    private OcrProvider stubOcrProvider;

    @Autowired
    @Qualifier("baiduOcrProvider")
    private OcrProvider baiduOcrProvider;

    @Autowired
    @Qualifier("tesseractOcrProvider")
    private OcrProvider tesseractOcrProvider;

    @Autowired
    @Qualifier("paddleOcrProvider")
    private PaddleOcrProvider paddleOcrProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> recognize(MultipartFile file, Long subjectId, String operator)
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("\u8bf7\u9009\u62e9\u56fe\u7247");
        }
        if (subjectId != null && subjectMapper.selectEduSubjectById(subjectId) == null)
        {
            throw new ServiceException("\u5b66\u79d1\u4e0d\u5b58\u5728");
        }
        String storedPath;
        try
        {
            storedPath = EduQbFileUploadUtils.upload(EduQbLocalFileSupport.getUploadPath(), file,
                    new String[] { "jpg", "jpeg", "png", "bmp", "webp" });
        }
        catch (Exception ex)
        {
            throw new ServiceException("\u56fe\u7247\u4e0a\u4f20\u5931\u8d25\uff1a" + ex.getMessage());
        }
        byte[] imageBytes = readImageBytes(storedPath);
        OcrProvider provider = resolveProvider();
        String providerName = resolveProviderName(provider);
        List<String> ocrWarnings = new ArrayList<>();
        String ocrMode = "";
        log.info("OCR recognize: provider={}, configured={}", providerName, ocrProperties.getProvider());
        List<EduQbOcrLine> lines;
        if (provider == paddleOcrProvider)
        {
            OcrSidecarResult sidecar = paddleOcrProvider.recognizeDetailed(imageBytes);
            lines = sidecar.getLines();
            if (StringUtils.isNotEmpty(sidecar.getProvider()))
            {
                providerName = sidecar.getProvider();
            }
            ocrMode = sidecar.getMode() != null ? sidecar.getMode() : "";
            ocrWarnings = sidecar.getWarnings() != null ? sidecar.getWarnings() : List.of();
        }
        else
        {
            lines = provider.recognize(imageBytes);
        }
        PredictResult predict = predictService.predict(lines);
        String ocrText = buildOcrText(lines);
        BigDecimal avgConfidence = averageConfidence(lines);

        EduQbOcrDraft draft = new EduQbOcrDraft();
        draft.setImagePath(storedPath);
        draft.setOcrText(ocrText);
        draft.setOcrLines(JSON.toJSONString(lines));
        draft.setConfidence(avgConfidence);
        draft.setPredictedType(predict.getQuestionType());
        draft.setPredictedDifficulty(predict.getDifficulty());
        draft.setPredictedOptions(predict.getOptionsJson());
        draft.setSubjectId(subjectId);
        draft.setStatus(EduQbOcrDraft.STATUS_DRAFT);
        draft.setCreateBy(operator);
        draft.setRemark(providerName);
        ocrDraftMapper.insertEduQbOcrDraft(draft);

        Map<String, Object> result = new HashMap<>();
        result.put("draftId", draft.getDraftId());
        result.put("imagePath", storedPath);
        result.put("imageUrl", storedPath);
        result.put("ocrText", ocrText);
        result.put("lines", lines);
        result.put("confidence", avgConfidence);
        result.put("confidenceThreshold", CONFIDENCE_WARN);
        result.put("predictedType", predict.getQuestionType());
        result.put("predictedDifficulty", predict.getDifficulty());
        result.put("predictedContent", predict.getContent());
        result.put("predictedOptions", predict.getOptionsJson());
        result.put("provider", providerName);
        if (StringUtils.isNotEmpty(ocrMode))
        {
            result.put("ocrMode", ocrMode);
        }
        if (!ocrWarnings.isEmpty())
        {
            result.put("ocrWarnings", ocrWarnings);
        }
        if ("stub".equals(providerName))
        {
            result.put("stubWarning", STUB_WARNING);
        }
        attachQualityHint(result, providerName, avgConfidence, lines, ocrWarnings);
        return result;
    }

    @Override
    public List<EduQbOcrDraft> selectOcrDraftList(EduQbOcrDraft query, String operator)
    {
        if (query == null)
        {
            query = new EduQbOcrDraft();
        }
        if (!EduQbSecuritySupport.isQuestionBankManager())
        {
            query.setCreateBy(operator);
        }
        return ocrDraftMapper.selectEduQbOcrDraftList(query);
    }

    @Override
    public EduQbOcrDraft getDraft(Long draftId, String operator)
    {
        EduQbOcrDraft draft = requireDraft(draftId);
        assertDraftOwner(draft, operator);
        return draft;
    }

    @Override
    public Map<String, Object> getDraftDetail(Long draftId, String operator)
    {
        EduQbOcrDraft draft = getDraft(draftId, operator);
        List<EduQbOcrLine> lines = JSON.parseArray(draft.getOcrLines(), EduQbOcrLine.class);
        Map<String, Object> result = new HashMap<>();
        result.put("draftId", draft.getDraftId());
        result.put("imagePath", draft.getImagePath());
        result.put("imageUrl", draft.getImagePath());
        result.put("figurePath", draft.getFigurePath());
        result.put("figureUrl", draft.getFigurePath());
        result.put("ocrText", draft.getOcrText());
        result.put("lines", lines != null ? lines : List.of());
        result.put("confidence", draft.getConfidence());
        result.put("confidenceThreshold", CONFIDENCE_WARN);
        result.put("predictedType", draft.getPredictedType());
        result.put("predictedDifficulty", draft.getPredictedDifficulty());
        result.put("predictedContent", draft.getOcrText());
        result.put("predictedOptions", draft.getPredictedOptions());
        result.put("subjectId", draft.getSubjectId());
        result.put("status", draft.getStatus());
        result.put("questionId", draft.getQuestionId());
        String providerName = StringUtils.isNotEmpty(draft.getRemark()) ? draft.getRemark() : resolveProviderName(resolveProvider());
        result.put("provider", providerName);
        if ("stub".equals(providerName))
        {
            result.put("stubWarning", STUB_WARNING);
        }
        attachQualityHint(result, providerName, draft.getConfidence(), lines, List.of());
        return result;
    }

    @Override
    public Long commit(EduQbOcrCommitBody body, String operator)
    {
        validateCommitBody(body);
        EduQbOcrDraft draft = requireDraft(body.getDraftId());
        assertDraftOwner(draft, operator);
        if (EduQbOcrDraft.STATUS_COMMITTED.equals(draft.getStatus()))
        {
            throw new ServiceException("\u8be5 OCR \u8349\u7a3f\u5df2\u5165\u5e93");
        }
        if (subjectMapper.selectEduSubjectById(body.getSubjectId()) == null)
        {
            throw new ServiceException("\u5b66\u79d1\u4e0d\u5b58\u5728");
        }
        assertNotStubCommit(draft, body);

        EduQbQuestion question = new EduQbQuestion();
        question.setSubjectId(body.getSubjectId());
        question.setChapterId(body.getChapterId());
        question.setChapterText(body.getChapterText());
        question.setKnowledgePoints(body.getKnowledgePoints());
        question.setDifficulty(body.getDifficulty() != null ? body.getDifficulty() : draft.getPredictedDifficulty());
        question.setQuestionType(body.getQuestionType());
        question.setContent(body.getContent().trim());
        question.setOptions(body.getOptions());
        question.setCorrectAnswer(body.getCorrectAnswer() != null ? body.getCorrectAnswer() : buildDefaultAnswer(body.getQuestionType()));
        question.setAnalysis(body.getAnalysis());
        question.setSourceType(EduQbConstants.SOURCE_OCR);
        String imagesJson = resolveQuestionImages(body, draft);
        if (StringUtils.isNotEmpty(imagesJson))
        {
            question.setImages(imagesJson);
        }
        questionService.insertEduQbQuestion(question, operator);

        draft.setStatus(EduQbOcrDraft.STATUS_COMMITTED);
        draft.setQuestionId(question.getQuestionId());
        draft.setSubjectId(body.getSubjectId());
        ocrDraftMapper.updateEduQbOcrDraft(draft);
        return question.getQuestionId();
    }

    @Override
    public List<EduSubject> listSubjects()
    {
        return subjectMapper.selectEduSubjectList(new EduSubject());
    }

    private void assertNotStubCommit(EduQbOcrDraft draft, EduQbOcrCommitBody body)
    {
        String providerName = StringUtils.isNotEmpty(draft.getRemark()) ? draft.getRemark() : resolveProviderName(resolveProvider());
        if (!"stub".equalsIgnoreCase(providerName))
        {
            return;
        }
        if (body != null && Boolean.TRUE.equals(body.getForceStub()))
        {
            return;
        }
        throw new ServiceException(STUB_WARNING + "\u8bf7\u914d\u7f6e\u771f\u5b9e OCR \u5f15\u64ce\u6216\u5728\u786e\u8ba4\u540e\u52fe\u9009\u5f3a\u5236\u5165\u5e93\u3002");
    }

    private OcrProvider resolveProvider()
    {
        String configured = ocrProperties.getProvider();
        if ("baidu".equalsIgnoreCase(configured))
        {
            if (!ocrProperties.getBaidu().isConfigured())
            {
                throw new ServiceException("\u767e\u5ea6 OCR \u672a\u914d\u7f6e\uff0c\u8bf7\u8bbe\u7f6e\u73af\u5883\u53d8\u91cf BAIDU_OCR_API_KEY / BAIDU_OCR_SECRET_KEY");
            }
            return baiduOcrProvider;
        }
        if ("tesseract".equalsIgnoreCase(configured))
        {
            if (!ocrProperties.getTesseract().isConfigured())
            {
                throw new ServiceException("\u672c\u5730 Tesseract \u672a\u5c31\u7eea\uff0c\u8bf7\u5b89\u88c5 Tesseract \u5e76\u914d\u7f6e tessdata \u76ee\u5f55\u4e0e chi_sim \u8bed\u8a00\u5305");
            }
            return tesseractOcrProvider;
        }
        if ("paddle".equalsIgnoreCase(configured))
        {
            if (!paddleOcrProvider.isReachable())
            {
                throw new ServiceException("PaddleOCR \u670d\u52a1\u4e0d\u53ef\u7528\uff0c\u8bf7\u8fd0\u884c hs_managerment/paddleocr-service/start.ps1");
            }
            return paddleOcrProvider;
        }
        if ("stub".equalsIgnoreCase(configured))
        {
            return stubOcrProvider;
        }
        if ("auto".equalsIgnoreCase(configured) || configured == null || configured.isBlank())
        {
            if (ocrProperties.getBaidu().isConfigured())
            {
                log.debug("OCR auto -> baidu");
                return baiduOcrProvider;
            }
            if (ocrProperties.getPaddle().isEnabled() && paddleOcrProvider.isReachable())
            {
                log.debug("OCR auto -> paddleocr (reachable)");
                return paddleOcrProvider;
            }
            log.warn("OCR auto: paddle not reachable at {}", ocrProperties.getPaddle().resolveHealthUrl());
            if (ocrProperties.getTesseract().isConfigured())
            {
                log.debug("OCR auto -> tesseract");
                return tesseractOcrProvider;
            }
            log.warn("OCR auto: tesseract not configured, falling back to stub");
        }
        log.warn("OCR using stub provider (configured={})", configured);
        return stubOcrProvider;
    }

    private String resolveProviderName(OcrProvider provider)
    {
        if (provider == baiduOcrProvider)
        {
            return ocrProperties.getBaidu().isAccurateMode() ? "baidu-accurate" : "baidu-general";
        }
        if (provider == tesseractOcrProvider)
        {
            return "tesseract";
        }
        if (provider == paddleOcrProvider)
        {
            return "paddleocr";
        }
        return "stub";
    }

    private void attachQualityHint(Map<String, Object> result, String providerName,
            BigDecimal avgConfidence, List<EduQbOcrLine> lines, List<String> ocrWarnings)
    {
        if (ocrWarnings != null)
        {
            for (String warning : ocrWarnings)
            {
                if (warning != null && warning.contains("formula_ocr_unavailable"))
                {
                    result.put("qualityHint",
                            "\u672a\u542f\u7528\u516c\u5f0f OCR\uff08Pix2Text\uff09\uff0c\u5206\u6570/\u6570\u5b66\u516c\u5f0f\u53ef\u80fd\u8bc6\u522b\u4e0d\u51c6\u3002"
                                    + "\u8bf7\u5728 paddleocr-service \u76ee\u5f55\u6267\u884c\uff1a"
                                    + "pip install -r requirements-formula.txt \u5e76\u91cd\u542f OCR \u670d\u52a1\u3002");
                    return;
                }
            }
        }
        if ("stub".equals(providerName) || "baidu-accurate".equals(providerName) || "baidu-general".equals(providerName)
                || "paddleocr".equals(providerName) || "pix2text".equals(providerName))
        {
            return;
        }
        int lineCount = lines != null ? lines.size() : 0;
        double conf = avgConfidence != null ? avgConfidence.doubleValue() : 0;
        if (conf >= 0.75 && lineCount >= 5)
        {
            return;
        }
        String hint = "\u8bc6\u522b\u8d28\u91cf\u504f\u4f4e\u3002\u5efa\u8bae\uff1a"
                + "\u2460 \u4e0a\u4f20\u539f\u56fe/\u626b\u63cf\u4ef6\uff08\u907f\u514d\u622a\u56fe\u542b UI \u6807\u7b7e\uff09\uff1b"
                + "\u2461 \u4ec5\u4fdd\u7559\u9898\u5e72\u4e0e\u9009\u9879\u533a\u57df\uff1b"
                + "\u2462 \u542f\u52a8 PaddleOCR \u670d\u52a1\u6216\u914d\u7f6e\u767e\u5ea6 OCR\uff08BAIDU_OCR_API_KEY\uff09\u3002";
        result.put("qualityHint", hint);
    }

    private String resolveQuestionImages(EduQbOcrCommitBody body, EduQbOcrDraft draft)
    {
        if (StringUtils.isNotEmpty(body.getImages()))
        {
            log.info("OCR commit images from client: {}", body.getImages());
            return body.getImages().trim();
        }
        if (StringUtils.isNotEmpty(draft.getFigurePath()))
        {
            log.info("OCR commit images fallback to draft figure: {}", draft.getFigurePath());
            return JSON.toJSONString(new String[] { draft.getFigurePath() });
        }
        if (StringUtils.isNotEmpty(draft.getImagePath()))
        {
            log.warn("OCR commit images fallback to full OCR page (no cropped figure): {}", draft.getImagePath());
            return JSON.toJSONString(new String[] { draft.getImagePath() });
        }
        return null;
    }

    @Override
    public void saveDraftFigure(Long draftId, String figurePath, String operator)
    {
        if (draftId == null)
        {
            throw new ServiceException("\u8349\u7a3f\u4e0d\u5b58\u5728");
        }
        if (StringUtils.isEmpty(figurePath))
        {
            throw new ServiceException("\u63d2\u56fe\u8def\u5f84\u4e0d\u80fd\u4e3a\u7a7a");
        }
        EduQbOcrDraft draft = requireDraft(draftId);
        assertDraftOwner(draft, operator);
        if (EduQbOcrDraft.STATUS_COMMITTED.equals(draft.getStatus()))
        {
            throw new ServiceException("\u8349\u7a3f\u5df2\u5165\u5e93\uff0c\u65e0\u6cd5\u4fee\u6539\u63d2\u56fe");
        }
        EduQbOcrDraft update = new EduQbOcrDraft();
        update.setDraftId(draftId);
        update.setFigurePath(figurePath.trim());
        ocrDraftMapper.updateEduQbOcrDraft(update);
        log.info("OCR draft {} figure saved: {}", draftId, figurePath);
    }

    private byte[] readImageBytes(String storedPath)
    {
        File localFile = EduQbLocalFileSupport.resolveStoredFile(storedPath);
        if (!localFile.exists())
        {
            throw new ServiceException("\u56fe\u7247\u6587\u4ef6\u4e0d\u5b58\u5728");
        }
        try
        {
            return Files.readAllBytes(localFile.toPath());
        }
        catch (Exception ex)
        {
            throw new ServiceException("\u8bfb\u53d6\u56fe\u7247\u5931\u8d25");
        }
    }

    private String buildOcrText(List<EduQbOcrLine> lines)
    {
        StringBuilder sb = new StringBuilder();
        for (EduQbOcrLine line : lines)
        {
            if (line == null || StringUtils.isEmpty(line.getText()))
            {
                continue;
            }
            if (sb.length() > 0)
            {
                sb.append('\n');
            }
            sb.append(line.getText().trim());
        }
        return sb.toString();
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

    private EduQbOcrDraft requireDraft(Long draftId)
    {
        if (draftId == null)
        {
            throw new ServiceException("OCR \u8349\u7a3f ID \u4e0d\u80fd\u4e3a\u7a7a");
        }
        EduQbOcrDraft draft = ocrDraftMapper.selectEduQbOcrDraftById(draftId);
        if (draft == null)
        {
            throw new ServiceException("OCR \u8349\u7a3f\u4e0d\u5b58\u5728");
        }
        return draft;
    }

    private void assertDraftOwner(EduQbOcrDraft draft, String operator)
    {
        if (EduQbSecuritySupport.isQuestionBankManager())
        {
            return;
        }
        if (draft.getCreateBy() != null && operator != null && !draft.getCreateBy().equals(operator))
        {
            throw new ServiceException("\u60a8\u6ca1\u6709\u6743\u9650\u64cd\u4f5c\u6b64 OCR \u8349\u7a3f");
        }
    }

    private void validateCommitBody(EduQbOcrCommitBody body)
    {
        if (body == null || body.getDraftId() == null)
        {
            throw new ServiceException("OCR \u8349\u7a3f\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (body.getSubjectId() == null)
        {
            throw new ServiceException("\u8bf7\u9009\u62e9\u5b66\u79d1");
        }
        if (StringUtils.isEmpty(body.getChapterText()))
        {
            throw new ServiceException("\u8bf7\u8f93\u5165\u7ae0\u8282");
        }
        if (StringUtils.isEmpty(body.getKnowledgePoints()))
        {
            throw new ServiceException("\u8bf7\u81f3\u5c11\u6dfb\u52a0\u4e00\u4e2a\u77e5\u8bc6\u70b9");
        }
        if (StringUtils.isEmpty(body.getQuestionType()))
        {
            throw new ServiceException("\u8bf7\u9009\u62e9\u9898\u578b");
        }
        if (StringUtils.isEmpty(body.getContent()))
        {
            throw new ServiceException("\u8bf7\u8f93\u5165\u9898\u5e72");
        }
    }

    private String buildDefaultAnswer(String questionType)
    {
        if (EduQbConstants.TYPE_JUDGE.equals(questionType))
        {
            return JSON.toJSONString("true");
        }
        if (EduQbConstants.TYPE_MULTI.equals(questionType))
        {
            return JSON.toJSONString(new String[] { "A" });
        }
        if (EduQbConstants.TYPE_FILL.equals(questionType) || EduQbConstants.TYPE_SHORT.equals(questionType))
        {
            return JSON.toJSONString("");
        }
        return JSON.toJSONString("A");
    }
}
