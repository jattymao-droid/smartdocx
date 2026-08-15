package com.ruoyi.system.service.education.impl;

import java.math.BigDecimal;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.utils.file.ImageUtils;
import com.ruoyi.system.service.education.support.EduQbLocalFileSupport;
import com.ruoyi.system.domain.education.EduQbConstants;
import com.ruoyi.system.domain.education.EduQbPaperExportResult;
import com.ruoyi.system.domain.education.EduQbPaperItemRequest;
import com.ruoyi.system.domain.education.EduQbPaperPreviewRequest;
import com.ruoyi.system.domain.education.EduQbPaperPreviewResult;
import com.ruoyi.system.domain.education.EduQbPaperTypeStat;
import com.ruoyi.system.domain.education.EduQbSmartComposeQuestion;
import com.ruoyi.system.domain.education.EduQbSmartComposeRequest;
import com.ruoyi.system.domain.education.EduQbSmartComposeResult;
import com.ruoyi.system.domain.education.EduQbSmartComposeTypeRule;
import com.ruoyi.system.domain.education.EduQbQuestion;
import com.ruoyi.system.mapper.education.EduQbQuestionMapper;
import com.ruoyi.system.service.education.IEduQbPaperService;
import com.ruoyi.system.service.education.IEduQbQuestionTypeService;
import com.ruoyi.system.service.education.support.EduQbPaperPdfExporter;
import com.ruoyi.system.service.education.support.EduQbPaperDocxBlock;
import com.ruoyi.system.service.education.support.EduQbPaperDocxExporter;
import com.ruoyi.system.service.education.support.EduQbPaperLayoutHelper;
import com.ruoyi.system.service.education.support.EduQbFormulaRenderer;
import com.ruoyi.system.service.education.support.EduQbQuestionContentSupport;

@Service
public class EduQbPaperServiceImpl implements IEduQbPaperService
{
    private static final String[] SECTION_NUMERALS = { "\u4e00", "\u4e8c", "\u4e09", "\u56db", "\u4e94" };

    @Autowired
    private EduQbQuestionMapper questionMapper;

    @Autowired
    private IEduQbQuestionTypeService questionTypeService;

    @Override
    public EduQbPaperPreviewResult previewPaper(EduQbPaperPreviewRequest request)
    {
        List<RenderItem> renderItems = prepareRenderItems(request);
        String html = buildHtml(request, renderItems);
        EduQbPaperPreviewResult result = new EduQbPaperPreviewResult();
        result.setHtml(html);
        result.setTotalScore(calcTotalScore(renderItems));
        result.setTypeStats(buildTypeStats(renderItems));
        return result;
    }

    @Override
    public EduQbPaperExportResult exportPdf(EduQbPaperPreviewRequest request)
    {
        EduQbPaperPreviewResult preview = previewPaper(request);
        return saveExportFile(preview.getHtml(), request, "pdf", file -> EduQbPaperPdfExporter.renderToFile(
                preview.getHtml(), file, request.getTemplateCode()));
    }

    @Override
    public EduQbPaperExportResult exportHtml(EduQbPaperPreviewRequest request)
    {
        EduQbPaperPreviewResult preview = previewPaper(request);
        return saveExportFile(preview.getHtml(), request, "html", file -> EduQbPaperDocxExporter.writeHtmlFile(preview.getHtml(), file));
    }

    @Override
    public EduQbPaperExportResult exportDocx(EduQbPaperPreviewRequest request)
    {
        List<RenderItem> renderItems = prepareRenderItems(request);
        List<EduQbPaperDocxBlock> blocks = buildDocxBlocks(request, renderItems);
        return saveExportFile(null, request, "docx", file -> EduQbPaperDocxExporter.renderToFile(request, blocks, file));
    }

    private List<RenderItem> prepareRenderItems(EduQbPaperPreviewRequest request)
    {
        validateRequest(request);
        Long[] ids = request.getItems().stream()
                .map(EduQbPaperItemRequest::getQuestionId)
                .distinct()
                .toArray(Long[]::new);
        List<EduQbQuestion> questions = questionMapper.selectEduQbQuestionByIds(ids);
        Map<Long, EduQbQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(EduQbQuestion::getQuestionId, q -> q));
        for (EduQbPaperItemRequest item : request.getItems())
        {
            if (!questionMap.containsKey(item.getQuestionId()))
            {
                throw new ServiceException("\u8bd5\u9898\u4e0d\u5b58\u5728\u6216\u5df2\u5220\u9664\uff1a" + item.getQuestionId());
            }
            EduQbQuestion question = questionMap.get(item.getQuestionId());
            if (!EduQbConstants.STATUS_APPROVED.equals(question.getStatus()))
            {
                throw new ServiceException("\u4ec5\u5df2\u901a\u8fc7\u5ba1\u6838\u7684\u8bd5\u9898\u53ef\u7528\u4e8e\u7ec4\u5377\uff1a" + question.getQuestionCode());
            }
        }
        List<RenderItem> renderItems = buildRenderItems(request, questionMap);
        sortRenderItems(renderItems, request.getSortMode());
        assignSections(renderItems, request.getSortMode());
        return renderItems;
    }

    @FunctionalInterface
    private interface ExportFileWriter
    {
        void write(File file) throws Exception;
    }

    private EduQbPaperExportResult saveExportFile(String ignored, EduQbPaperPreviewRequest request, String ext,
            ExportFileWriter writer)
    {
        String storedName = System.currentTimeMillis() + "_" + buildDownloadFileName(request, ext);
        File outputFile = new File(EduQbLocalFileSupport.getDownloadPath(), storedName);
        try
        {
            writer.write(outputFile);
        }
        catch (Exception ex)
        {
            throw new ServiceException(exportLabel(ext) + " \u5bfc\u51fa\u5931\u8d25\uff1a" + ex.getMessage());
        }
        EduQbPaperExportResult result = new EduQbPaperExportResult();
        result.setFileName(storedName);
        result.setUrl("/profile/download/" + storedName);
        return result;
    }

    private String exportLabel(String ext)
    {
        if ("docx".equals(ext))
        {
            return "DOCX";
        }
        if ("html".equals(ext))
        {
            return "HTML";
        }
        return "PDF";
    }

    private List<EduQbPaperDocxBlock> buildDocxBlocks(EduQbPaperPreviewRequest request, List<RenderItem> items)
    {
        boolean teacherMode = EduQbConstants.EXPORT_TEACHER.equals(request.getExportMode());
        List<EduQbPaperDocxBlock> blocks = new ArrayList<>();
        String lastSection = null;
        int qNo = 0;
        for (RenderItem ri : items)
        {
            if (StringUtils.isNotEmpty(ri.sectionTitle) && !ri.sectionTitle.equals(lastSection))
            {
                lastSection = ri.sectionTitle;
                qNo = 0;
            }
            qNo++;
            EduQbQuestion q = ri.question;
            EduQbPaperDocxBlock block = new EduQbPaperDocxBlock();
            block.setSectionTitle(ri.sectionTitle);
            block.setQuestionNo(qNo);
            block.setContent(q.getContent());
            block.setScoreValue(ri.scoreValue);
            block.setOptions(readOptionTexts(q));
            block.setImageUrls(readImageUrls(q));
            if (teacherMode)
            {
                StringBuilder answerLine = new StringBuilder("\u3010\u7b54\u3011").append(formatAnswer(q));
                if (StringUtils.isNotEmpty(q.getAnalysis()))
                {
                    answerLine.append("  \u3010\u89e3\u6790\u3011").append(q.getAnalysis());
                }
                block.setAnswerLine(answerLine.toString());
            }
            blocks.add(block);
        }
        return blocks;
    }

    private List<String> readImageUrls(EduQbQuestion q)
    {
        JSONArray images = parseJsonArray(q.getImages());
        if (images == null)
        {
            return null;
        }
        List<String> list = new ArrayList<>();
        for (int i = 0; i < images.size(); i++)
        {
            String url = images.getString(i);
            if (StringUtils.isNotEmpty(url))
            {
                list.add(url.trim());
            }
        }
        return list.isEmpty() ? null : list;
    }

    private List<String> readOptionTexts(EduQbQuestion q)
    {
        String type = q.getQuestionType();
        if (!EduQbConstants.TYPE_SINGLE.equals(type) && !EduQbConstants.TYPE_MULTI.equals(type))
        {
            return null;
        }
        JSONArray options = parseJsonArray(q.getOptions());
        if (options == null)
        {
            return null;
        }
        List<String> list = new ArrayList<>();
        for (int i = 0; i < options.size(); i++)
        {
            list.add(options.getString(i));
        }
        return list;
    }

    private String buildDownloadFileName(EduQbPaperPreviewRequest request, String ext)
    {
        String base = request.getPaperTitle();
        if (StringUtils.isEmpty(base) && request.getHeader() != null)
        {
            base = request.getHeader().get("examTitle");
        }
        if (StringUtils.isEmpty(base) && request.getHeader() != null)
        {
            base = request.getHeader().get("subjectName");
        }
        if (StringUtils.isEmpty(base))
        {
            base = "\u8bd5\u5377";
        }
        base = base.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (EduQbConstants.EXPORT_TEACHER.equals(request.getExportMode()))
        {
            base = base + "_\u6559\u5e08\u7248";
        }
        else
        {
            base = base + "_\u5b66\u751f\u7248";
        }
        String suffix = "." + ext.toLowerCase();
        if (!base.toLowerCase().endsWith(suffix))
        {
            base = base + suffix;
        }
        return base;
    }

    private void validateRequest(EduQbPaperPreviewRequest request)
    {
        if (request == null || request.getItems() == null || request.getItems().isEmpty())
        {
            throw new ServiceException("\u8bf7\u81f3\u5c11\u9009\u62e9\u4e00\u9053\u8bd5\u9898");
        }
        if (StringUtils.isEmpty(request.getSortMode()))
        {
            request.setSortMode(EduQbConstants.SORT_TYPE_DIFF);
        }
        if (StringUtils.isEmpty(request.getExportMode()))
        {
            request.setExportMode(EduQbConstants.EXPORT_STUDENT);
        }
        if (StringUtils.isEmpty(request.getTemplateCode()))
        {
            request.setTemplateCode(EduQbConstants.TEMPLATE_A4_1COL);
        }
        else
        {
            request.setTemplateCode(EduQbPaperLayoutHelper.normalizeTemplateCode(request.getTemplateCode()));
        }
    }

    private List<RenderItem> buildRenderItems(EduQbPaperPreviewRequest request, Map<Long, EduQbQuestion> questionMap)
    {
        List<RenderItem> list = new ArrayList<>();
        for (EduQbPaperItemRequest item : request.getItems())
        {
            EduQbQuestion q = questionMap.get(item.getQuestionId());
            RenderItem ri = new RenderItem();
            ri.question = q;
            ri.scoreValue = item.getScoreValue() != null ? item.getScoreValue() : new BigDecimal("5");
            ri.basketOrder = item.getOrderNum() != null ? item.getOrderNum() : list.size() + 1;
            ri.answerAreaLines = item.getAnswerAreaLines();
            ri.answerAreaStyle = item.getAnswerAreaStyle();
            list.add(ri);
        }
        return list;
    }

    private void sortRenderItems(List<RenderItem> items, String sortMode)
    {
        if (EduQbConstants.SORT_BASKET_ORDER.equals(sortMode))
        {
            items.sort(Comparator.comparingInt(i -> i.basketOrder));
            return;
        }
        if (EduQbConstants.SORT_DIFFICULTY.equals(sortMode))
        {
            items.sort(Comparator.comparing(i -> i.question.getDifficulty() != null ? i.question.getDifficulty() : BigDecimal.ZERO));
            return;
        }
        items.sort(Comparator
                .comparingInt((RenderItem i) -> typeIndex(i.question.getQuestionType()))
                .thenComparing(i -> i.question.getDifficulty() != null ? i.question.getDifficulty() : BigDecimal.ZERO));
    }

    private int typeIndex(String type)
    {
        return questionTypeService.resolveTypeSortIndex(type);
    }

    private void assignSections(List<RenderItem> items, String sortMode)
    {
        if (EduQbConstants.SORT_BASKET_ORDER.equals(sortMode))
        {
            for (RenderItem item : items)
            {
                item.sectionTitle = "";
            }
            return;
        }
        LinkedHashMap<String, List<RenderItem>> groups = new LinkedHashMap<>();
        for (RenderItem item : items)
        {
            String type = item.question.getQuestionType();
            groups.computeIfAbsent(type, k -> new ArrayList<>()).add(item);
        }
        int sectionIdx = 0;
        for (Map.Entry<String, List<RenderItem>> entry : groups.entrySet())
        {
            String type = entry.getKey();
            List<RenderItem> group = entry.getValue();
            BigDecimal sectionScore = group.stream()
                    .map(i -> i.scoreValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            String numeral = sectionIdx < SECTION_NUMERALS.length ? SECTION_NUMERALS[sectionIdx] : String.valueOf(sectionIdx + 1);
            String label = questionTypeService.resolveTypeLabel(type);
            BigDecimal perScore = group.get(0).scoreValue;
            boolean sameScore = group.stream().allMatch(i -> i.scoreValue.compareTo(perScore) == 0);
            String sectionTitle;
            if (sameScore)
            {
                sectionTitle = numeral + "\u3001" + label + "\uff08\u6bcf\u9898" + perScore.stripTrailingZeros().toPlainString()
                        + "\u5206\uff0c\u5171" + sectionScore.stripTrailingZeros().toPlainString() + "\u5206\uff09";
            }
            else
            {
                sectionTitle = numeral + "\u3001" + label + "\uff08\u5171" + sectionScore.stripTrailingZeros().toPlainString() + "\u5206\uff09";
            }
            for (RenderItem ri : group)
            {
                ri.sectionTitle = sectionTitle;
            }
            sectionIdx++;
        }
    }

    private BigDecimal calcTotalScore(List<RenderItem> items)
    {
        return items.stream().map(i -> i.scoreValue).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<EduQbPaperTypeStat> buildTypeStats(List<RenderItem> items)
    {
        Map<String, List<RenderItem>> byType = new LinkedHashMap<>();
        for (RenderItem item : items)
        {
            byType.computeIfAbsent(item.question.getQuestionType(), k -> new ArrayList<>()).add(item);
        }
        List<EduQbPaperTypeStat> stats = new ArrayList<>();
        for (Map.Entry<String, List<RenderItem>> entry : byType.entrySet())
        {
            EduQbPaperTypeStat stat = new EduQbPaperTypeStat();
            stat.setType(entry.getKey());
            stat.setTypeLabel(questionTypeService.resolveTypeLabel(entry.getKey()));
            stat.setCount(entry.getValue().size());
            stat.setScore(entry.getValue().stream().map(i -> i.scoreValue).reduce(BigDecimal.ZERO, BigDecimal::add));
            stats.add(stat);
        }
        return stats;
    }

    private String buildHtml(EduQbPaperPreviewRequest request, List<RenderItem> items)
    {
        Map<String, String> header = request.getHeader() != null ? request.getHeader() : new HashMap<>();
        String schoolName = header.getOrDefault("schoolName", "");
        String examTitle = header.getOrDefault("examTitle", "");
        String subjectName = header.getOrDefault("subjectName", "");
        String duration = header.getOrDefault("duration", "");
        BigDecimal totalScore = calcTotalScore(items);
        boolean teacherMode = EduQbConstants.EXPORT_TEACHER.equals(request.getExportMode());

        int marginMm = EduQbPaperLayoutHelper.marginMm(request.getExportConfig(), 20);
        String pageSizeCss = EduQbPaperLayoutHelper.pageSizeCss(request.getTemplateCode());

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>");
        sb.append("@page{size:").append(pageSizeCss).append(";margin:").append(marginMm).append("mm;}");
        sb.append("body{font-family:SimSun,serif;font-size:12pt;line-height:1.6;margin:0;color:#000;}");
        sb.append(".school{text-align:center;font-size:14pt;font-weight:bold;}");
        sb.append(".title{text-align:center;font-size:16pt;font-weight:bold;margin:8px 0;}");
        sb.append(".meta{text-align:center;margin-bottom:16px;border-bottom:1px solid #333;padding-bottom:8px;}");
        sb.append(".section{margin-top:16px;font-weight:bold;}");
        sb.append(".question{margin:12px 0 8px;}");
        sb.append(".option{margin-left:24px;}");
        sb.append(".q-images{margin:8px 0 8px 24px;}");
        sb.append(".q-image{margin:6px 0;}");
        sb.append(".q-image img{max-width:320px;max-height:240px;}");
        sb.append(".answer{color:#c00;margin:4px 0 0 24px;font-size:11pt;}");
        sb.append(".answer-area{margin:12px 0 8px 24px;}");
        sb.append(".answer-line{border-bottom:1px solid #999;height:28px;margin-bottom:6px;}");
        sb.append(".answer-blank{min-height:48px;border:1px dashed #bbb;margin-bottom:8px;}");
        sb.append(".formula-img{vertical-align:middle;max-height:1.35em;}");
        sb.append(".question table{border-collapse:collapse;margin:8px 0;max-width:100%;}");
        sb.append(".question td,.question th{border:1px solid #333;padding:4px 8px;text-align:center;vertical-align:middle;}");
        sb.append(".question img{max-width:320px;max-height:240px;vertical-align:middle;}");
        sb.append(".option img{max-width:240px;max-height:180px;vertical-align:middle;}");
        sb.append("</style></head><body>");
        if (StringUtils.isNotEmpty(schoolName))
        {
            sb.append("<div class=\"school\">").append(escapeHtml(schoolName)).append("</div>");
        }
        String title = StringUtils.isNotEmpty(request.getPaperTitle()) ? request.getPaperTitle()
                : (StringUtils.isNotEmpty(examTitle) ? examTitle : "\u8bd5\u5377");
        sb.append("<div class=\"title\">").append(escapeHtml(title)).append("</div>");
        sb.append("<div class=\"meta\">");
        if (StringUtils.isNotEmpty(subjectName))
        {
            sb.append("\u79d1\u76ee\uff1a").append(escapeHtml(subjectName)).append("&#160;&#160;&#160;");
        }
        if (StringUtils.isNotEmpty(duration))
        {
            sb.append("\u65f6\u95f4\uff1a").append(escapeHtml(duration)).append("&#160;&#160;&#160;");
        }
        sb.append("\u6ee1\u5206\uff1a").append(totalScore.stripTrailingZeros().toPlainString());
        sb.append("</div>");

        String lastSection = null;
        int qNo = 0;
        for (RenderItem ri : items)
        {
            if (StringUtils.isNotEmpty(ri.sectionTitle) && !ri.sectionTitle.equals(lastSection))
            {
                sb.append("<div class=\"section\">").append(escapeHtml(ri.sectionTitle)).append("</div>");
                lastSection = ri.sectionTitle;
                qNo = 0;
            }
            qNo++;
            EduQbQuestion q = ri.question;
            sb.append("<div class=\"question\">").append(qNo).append(". ")
                    .append(EduQbFormulaRenderer.renderToHtml(q.getContent())).append(" \uff08")
                    .append(ri.scoreValue.stripTrailingZeros().toPlainString())
                    .append("\u5206\uff09</div>");
            if (!EduQbQuestionContentSupport.isHtmlContent(q.getContent()))
            {
                appendImages(sb, q);
            }
            appendOptions(sb, q);
            appendAnswerArea(sb, ri);
            if (teacherMode)
            {
                appendTeacherAnswer(sb, q);
            }
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private void appendImages(StringBuilder sb, EduQbQuestion q)
    {
        List<String> urls = readImageUrls(q);
        if (urls == null || urls.isEmpty())
        {
            return;
        }
        sb.append("<div class=\"q-images\">");
        for (String url : urls)
        {
            String src = resolveImageSrc(url);
            if (StringUtils.isEmpty(src))
            {
                continue;
            }
            sb.append("<div class=\"q-image\"><img src=\"").append(src).append("\" alt=\"figure\"/></div>");
        }
        sb.append("</div>");
    }

    private String resolveImageSrc(String url)
    {
        if (StringUtils.isEmpty(url))
        {
            return "";
        }
        String trimmed = url.trim();
        if (trimmed.startsWith("data:"))
        {
            return trimmed;
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://"))
        {
            return escapeHtml(trimmed);
        }
        byte[] bytes = ImageUtils.readFile(trimmed);
        if (bytes == null || bytes.length == 0)
        {
            return "";
        }
        String mime = guessImageMime(trimmed);
        return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    private String guessImageMime(String path)
    {
        String lower = path.toLowerCase();
        if (lower.endsWith(".png"))
        {
            return "image/png";
        }
        if (lower.endsWith(".gif"))
        {
            return "image/gif";
        }
        if (lower.endsWith(".webp"))
        {
            return "image/webp";
        }
        if (lower.endsWith(".bmp"))
        {
            return "image/bmp";
        }
        return "image/jpeg";
    }

    private void appendOptions(StringBuilder sb, EduQbQuestion q)
    {
        String type = q.getQuestionType();
        if (!EduQbConstants.TYPE_SINGLE.equals(type) && !EduQbConstants.TYPE_MULTI.equals(type))
        {
            return;
        }
        JSONArray options = parseJsonArray(q.getOptions());
        if (options == null)
        {
            return;
        }
        for (int i = 0; i < options.size(); i++)
        {
            sb.append("<div class=\"option\">").append(EduQbFormulaRenderer.renderToHtml(options.getString(i))).append("</div>");
        }
    }

    private void appendAnswerArea(StringBuilder sb, RenderItem ri)
    {
        if (ri.answerAreaLines == null || ri.answerAreaLines <= 0)
        {
            return;
        }
        int lines = Math.min(ri.answerAreaLines, 30);
        String style = ri.answerAreaStyle != null ? ri.answerAreaStyle : "ruled";
        sb.append("<div class=\"answer-area\">");
        if ("blank".equals(style))
        {
            int height = Math.max(48, lines * 28);
            sb.append("<div class=\"answer-blank\" style=\"min-height:").append(height).append("px\"></div>");
        }
        else
        {
            for (int i = 0; i < lines; i++)
            {
                sb.append("<div class=\"answer-line\"></div>");
            }
        }
        sb.append("</div>");
    }

    private void appendTeacherAnswer(StringBuilder sb, EduQbQuestion q)
    {
        String answer = formatAnswer(q);
        sb.append("<div class=\"answer\">\u3010\u7b54\u3011").append(EduQbFormulaRenderer.renderToHtml(answer));
        if (StringUtils.isNotEmpty(q.getAnalysis()))
        {
            sb.append("&#160;&#160;\u3010\u89e3\u6790\u3011").append(EduQbFormulaRenderer.renderToHtml(q.getAnalysis()));
        }
        sb.append("</div>");
    }

    private String formatAnswer(EduQbQuestion q)
    {
        String raw = q.getCorrectAnswer();
        if (StringUtils.isEmpty(raw))
        {
            return "";
        }
        try
        {
            Object parsed = JSON.parse(raw);
            if (parsed instanceof JSONArray)
            {
                JSONArray arr = (JSONArray) parsed;
                List<String> parts = new ArrayList<>();
                for (int i = 0; i < arr.size(); i++)
                {
                    parts.add(String.valueOf(arr.get(i)));
                }
                return String.join("\u3001", parts);
            }
            return String.valueOf(parsed);
        }
        catch (Exception ex)
        {
            return raw;
        }
    }

    private JSONArray parseJsonArray(String raw)
    {
        if (StringUtils.isEmpty(raw))
        {
            return null;
        }
        try
        {
            return JSON.parseArray(raw);
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    @Override
    public EduQbSmartComposeResult smartCompose(EduQbSmartComposeRequest request)
    {
        validateSmartComposeRequest(request);
        EduQbSmartComposeResult result = new EduQbSmartComposeResult();
        result.setPaperTitle(resolveComposePaperTitle(request));

        Set<Long> usedIds = new HashSet<>();
        if (request.getExcludeQuestionIds() != null)
        {
            usedIds.addAll(request.getExcludeQuestionIds());
        }
        Set<String> usedHashes = new HashSet<>();
        List<EduQbSmartComposeQuestion> composed = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int order = 1;

        for (EduQbSmartComposeTypeRule rule : request.getTypeRules())
        {
            if (rule == null || StringUtils.isEmpty(rule.getQuestionType()))
            {
                continue;
            }
            int need = rule.getCount() != null ? rule.getCount() : 0;
            if (need <= 0)
            {
                continue;
            }
            questionTypeService.assertEnabledType(rule.getQuestionType());
            BigDecimal scorePer = rule.getScorePerQuestion() != null ? rule.getScorePerQuestion() : BigDecimal.valueOf(5);
            List<DifficultySlot> slots = buildDifficultySlots(need, request);

            for (DifficultySlot slot : slots)
            {
                if (slot.count <= 0)
                {
                    continue;
                }
                EduQbQuestion query = new EduQbQuestion();
                query.setSubjectId(request.getSubjectId());
                query.setChapterId(request.getChapterId());
                query.setChapterText(request.getChapterText());
                if (request.getChapterIds() != null && !request.getChapterIds().isEmpty())
                {
                    query.getParams().put("chapterIds", request.getChapterIds());
                    query.setChapterId(null);
                    query.setChapterText(null);
                }
                query.setQuestionType(rule.getQuestionType());
                query.setStatus(EduQbConstants.STATUS_APPROVED);
                query.setDifficultyMin(slot.min);
                query.setDifficultyMax(slot.max);

                List<EduQbQuestion> pool = questionMapper.selectEduQbQuestionList(query);
                Collections.shuffle(pool);

                int pickedInSlot = 0;
                for (EduQbQuestion candidate : pool)
                {
                    if (pickedInSlot >= slot.count)
                    {
                        break;
                    }
                    if (usedIds.contains(candidate.getQuestionId()))
                    {
                        continue;
                    }
                    String hash = candidate.getContentHash();
                    if (StringUtils.isNotEmpty(hash) && usedHashes.contains(hash))
                    {
                        continue;
                    }
                    EduQbSmartComposeQuestion item = toComposeQuestion(candidate, scorePer, order++);
                    composed.add(item);
                    usedIds.add(candidate.getQuestionId());
                    if (StringUtils.isNotEmpty(hash))
                    {
                        usedHashes.add(hash);
                    }
                    pickedInSlot++;
                }
                if (pickedInSlot < slot.count)
                {
                    warnings.add(buildShortageWarning(rule.getQuestionType(), slot.label, slot.count - pickedInSlot));
                }
            }
        }

        if (composed.isEmpty())
        {
            throw new ServiceException("\u672a\u627e\u5230\u7b26\u5408\u6761\u4ef6\u7684\u5ba1\u6838\u901a\u8fc7\u8bd5\u9898\uff0c\u8bf7\u8c03\u6574\u7ae0\u8282\u3001\u96be\u5ea6\u6216\u9898\u578b\u8981\u6c42");
        }

        result.setQuestions(composed);
        result.setWarnings(warnings);
        result.setTotalScore(composed.stream()
                .map(EduQbSmartComposeQuestion::getScoreValue)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return result;
    }

    private void validateSmartComposeRequest(EduQbSmartComposeRequest request)
    {
        if (request == null)
        {
            throw new ServiceException("\u8bf7\u63d0\u4f9b\u7ec4\u5377\u53c2\u6570");
        }
        if (request.getSubjectId() == null)
        {
            throw new ServiceException("\u8bf7\u9009\u62e9\u5b66\u79d1");
        }
        if (request.getTypeRules() == null || request.getTypeRules().isEmpty())
        {
            throw new ServiceException("\u8bf7\u8bbe\u7f6e\u9898\u578b\u6570\u91cf");
        }
        boolean hasCount = false;
        for (EduQbSmartComposeTypeRule rule : request.getTypeRules())
        {
            if (rule != null && rule.getCount() != null && rule.getCount() > 0)
            {
                hasCount = true;
                break;
            }
        }
        if (!hasCount)
        {
            throw new ServiceException("\u9898\u578b\u6570\u91cf\u4e0d\u80fd\u5168\u4e3a 0");
        }
    }

    private String resolveComposePaperTitle(EduQbSmartComposeRequest request)
    {
        if (StringUtils.isNotEmpty(request.getPaperTitle()))
        {
            return request.getPaperTitle().trim();
        }
        String template = request.getTemplateCode();
        if ("midterm".equalsIgnoreCase(template))
        {
            return "\u671f\u4e2d\u6d4b\u8bd5\u5377";
        }
        if ("final".equalsIgnoreCase(template))
        {
            return "\u671f\u672b\u6d4b\u8bd5\u5377";
        }
        if ("unit".equalsIgnoreCase(template))
        {
            return "\u5355\u5143\u6d4b\u9a8c\u5377";
        }
        return "\u667a\u80fd\u7ec4\u5377";
    }

    private EduQbSmartComposeQuestion toComposeQuestion(EduQbQuestion question, BigDecimal scoreValue, int orderNum)
    {
        EduQbSmartComposeQuestion item = new EduQbSmartComposeQuestion();
        item.setQuestionId(question.getQuestionId());
        item.setQuestionCode(question.getQuestionCode());
        item.setContent(question.getContent());
        item.setQuestionType(question.getQuestionType());
        item.setDifficulty(question.getDifficulty());
        item.setOptions(question.getOptions());
        item.setImages(question.getImages());
        item.setScoreValue(scoreValue);
        item.setOrderNum(orderNum);
        return item;
    }

    private String buildShortageWarning(String questionType, String difficultyLabel, int shortage)
    {
        String typeLabel = questionTypeService.resolveTypeLabel(questionType);
        if (StringUtils.isNotEmpty(difficultyLabel))
        {
            return typeLabel + "\uff08" + difficultyLabel + "\uff09\u7f3a\u5c11 " + shortage + " \u9898";
        }
        return typeLabel + " \u7f3a\u5c11 " + shortage + " \u9898";
    }

    private List<DifficultySlot> buildDifficultySlots(int total, EduQbSmartComposeRequest request)
    {
        Integer easyPct = request.getEasyPercent();
        Integer mediumPct = request.getMediumPercent();
        Integer hardPct = request.getHardPercent();
        boolean useDistribution = easyPct != null || mediumPct != null || hardPct != null;
        if (!useDistribution)
        {
            DifficultySlot slot = new DifficultySlot();
            slot.label = "\u5168\u90e8";
            slot.min = request.getDifficultyMin() != null ? request.getDifficultyMin() : BigDecimal.ZERO;
            slot.max = request.getDifficultyMax() != null ? request.getDifficultyMax() : BigDecimal.ONE;
            slot.count = total;
            return List.of(slot);
        }

        int easy = easyPct != null ? Math.max(0, easyPct) : 0;
        int medium = mediumPct != null ? Math.max(0, mediumPct) : 0;
        int hard = hardPct != null ? Math.max(0, hardPct) : 0;
        int sumPct = easy + medium + hard;
        if (sumPct <= 0)
        {
            easy = 30;
            medium = 50;
            hard = 20;
            sumPct = 100;
        }

        int easyCount = total * easy / sumPct;
        int mediumCount = total * medium / sumPct;
        int hardCount = total - easyCount - mediumCount;

        BigDecimal globalMin = request.getDifficultyMin() != null ? request.getDifficultyMin() : BigDecimal.ZERO;
        BigDecimal globalMax = request.getDifficultyMax() != null ? request.getDifficultyMax() : BigDecimal.ONE;

        List<DifficultySlot> slots = new ArrayList<>();
        if (easyCount > 0)
        {
            DifficultySlot slot = new DifficultySlot();
            slot.label = "\u8f83\u6613";
            slot.min = globalMin.max(BigDecimal.ZERO);
            slot.max = globalMax.min(new BigDecimal("0.35"));
            slot.count = easyCount;
            slots.add(slot);
        }
        if (mediumCount > 0)
        {
            DifficultySlot slot = new DifficultySlot();
            slot.label = "\u4e2d\u7b49";
            slot.min = globalMin.max(new BigDecimal("0.35"));
            slot.max = globalMax.min(new BigDecimal("0.70"));
            slot.count = mediumCount;
            slots.add(slot);
        }
        if (hardCount > 0)
        {
            DifficultySlot slot = new DifficultySlot();
            slot.label = "\u8f83\u96be";
            slot.min = globalMin.max(new BigDecimal("0.70"));
            slot.max = globalMax.min(BigDecimal.ONE);
            slot.count = hardCount;
            slots.add(slot);
        }
        return slots;
    }

    private static class DifficultySlot
    {
        private String label;
        private BigDecimal min;
        private BigDecimal max;
        private int count;
    }

    private String escapeHtml(String text)
    {
        if (text == null)
        {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static class RenderItem
    {
        private EduQbQuestion question;
        private BigDecimal scoreValue;
        private int basketOrder;
        private String sectionTitle = "";
        private Integer answerAreaLines;
        private String answerAreaStyle;
    }
}
