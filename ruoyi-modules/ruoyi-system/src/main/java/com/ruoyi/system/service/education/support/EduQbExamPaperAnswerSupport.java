package com.ruoyi.system.service.education.support;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.domain.education.EduQbConstants;
import com.ruoyi.system.domain.education.EduQbImportBlock;

/**
 * Parse trailing answer-key sections in exam DOCX.
 */
public final class EduQbExamPaperAnswerSupport
{
    private static final Pattern ANSWER_SECTION = Pattern.compile(
            "^[\\s\\u3000]*(?:\u53c2\u8003\u7b54\u6848|\u7b54\u6848\u4e0e\u89e3\u6790|\u8bd5\u9898\u7b54\u6848|\u6807\u51c6\u7b54\u6848|\u7b54\u6848\u89e3\u6790|\u7b54\u6848\u53ca\u89e3\u6790)[\\s\\u3000]*$");

    private static final Pattern NUMBERED_ANSWER = Pattern.compile(
            "^[\\s\\u3000]*(?:\u7b2c)?(\\d+)[\\.\\uFF0E\\u3001\\)\\uFF09:\\uFF1A\\-\\u2014\\s]+"
                    + "(?:\u3010\u7b54\u6848\u3011|\u3010\u7b54\u3011|\u7b54\u6848|\u53c2\u8003\u7b54\u6848)?[:\\s\\uFF1A]*"
                    + "(.+)$");

    private EduQbExamPaperAnswerSupport()
    {
    }

    public static boolean isAnswerSectionHeader(String text)
    {
        if (StringUtils.isEmpty(text))
        {
            return false;
        }
        String oneLine = text.replace('\n', ' ').trim();
        if (oneLine.length() > 30)
        {
            return false;
        }
        return ANSWER_SECTION.matcher(oneLine).find();
    }

    public static Map<Integer, ParsedAnswer> parseAnswerBlocks(Iterable<EduQbImportBlock> blocks)
    {
        Map<Integer, ParsedAnswer> map = new HashMap<>();
        if (blocks == null)
        {
            return map;
        }
        for (EduQbImportBlock block : blocks)
        {
            if (block == null || StringUtils.isEmpty(block.getText()))
            {
                continue;
            }
            mergeParsed(map, parseAnswerText(block.getText()));
        }
        return map;
    }

    static Map<Integer, ParsedAnswer> parseAnswerText(String text)
    {
        Map<Integer, ParsedAnswer> map = new HashMap<>();
        if (StringUtils.isEmpty(text))
        {
            return map;
        }
        Integer currentOrder = null;
        StringBuilder analysisBuf = new StringBuilder();
        boolean inAnalysis = false;

        for (String line : text.split("\\r?\\n"))
        {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isEmpty())
            {
                continue;
            }
            Matcher numbered = NUMBERED_ANSWER.matcher(trimmed);
            if (numbered.find())
            {
                flushAnalysis(map, currentOrder, analysisBuf);
                inAnalysis = false;
                currentOrder = Integer.parseInt(numbered.group(1));
                ParsedAnswer entry = map.computeIfAbsent(currentOrder, k -> new ParsedAnswer());
                entry.setAnswerRaw(numbered.group(2).trim());
                continue;
            }
            EduQbImportContentSupport.ParsedImportContent inline = EduQbImportContentSupport.parseContent(trimmed);
            if (StringUtils.isNotEmpty(inline.getCorrectAnswer()))
            {
                flushAnalysis(map, currentOrder, analysisBuf);
                inAnalysis = false;
                if (currentOrder == null)
                {
                    currentOrder = map.size() + 1;
                }
                ParsedAnswer entry = map.computeIfAbsent(currentOrder, k -> new ParsedAnswer());
                entry.setAnswerRaw(extractRawAnswer(inline.getCorrectAnswer()));
                if (StringUtils.isNotEmpty(inline.getAnalysis()))
                {
                    entry.setAnalysis(inline.getAnalysis());
                }
                continue;
            }
            if (StringUtils.isNotEmpty(inline.getAnalysis()) || isAnalysisOnlyLine(trimmed))
            {
                inAnalysis = true;
                if (StringUtils.isNotEmpty(inline.getAnalysis()))
                {
                    analysisBuf.append(inline.getAnalysis());
                }
                else
                {
                    analysisBuf.append(stripAnalysisPrefix(trimmed));
                }
                continue;
            }
            if (inAnalysis && currentOrder != null)
            {
                if (analysisBuf.length() > 0)
                {
                    analysisBuf.append('\n');
                }
                analysisBuf.append(trimmed);
            }
        }
        flushAnalysis(map, currentOrder, analysisBuf);
        return map;
    }

    private static void flushAnalysis(Map<Integer, ParsedAnswer> map, Integer order, StringBuilder buf)
    {
        if (order == null || buf.length() == 0)
        {
            buf.setLength(0);
            return;
        }
        ParsedAnswer entry = map.computeIfAbsent(order, k -> new ParsedAnswer());
        if (StringUtils.isEmpty(entry.getAnalysis()))
        {
            entry.setAnalysis(buf.toString().trim());
        }
        buf.setLength(0);
    }

    private static boolean isAnalysisOnlyLine(String text)
    {
        return text.matches("^(?:\u3010\u89e3\u6790\u3011|\u3010\u8be6\u89e3\u3011|\u89e3\u6790|\u8bd5\u9898\u89e3\u6790)[:\\s\\uFF1A]*.*");
    }

    private static String stripAnalysisPrefix(String text)
    {
        return text.replaceFirst("^(?:\u3010\u89e3\u6790\u3011|\u3010\u8be6\u89e3\u3011|\u89e3\u6790|\u8bd5\u9898\u89e3\u6790)[:\\s\\uFF1A]*", "").trim();
    }

    private static String extractRawAnswer(String jsonAnswer)
    {
        if (StringUtils.isEmpty(jsonAnswer))
        {
            return null;
        }
        try
        {
            return JSON.parse(jsonAnswer).toString();
        }
        catch (Exception ex)
        {
            return jsonAnswer;
        }
    }

    private static void mergeParsed(Map<Integer, ParsedAnswer> target, Map<Integer, ParsedAnswer> source)
    {
        for (Map.Entry<Integer, ParsedAnswer> e : source.entrySet())
        {
            ParsedAnswer existing = target.computeIfAbsent(e.getKey(), k -> new ParsedAnswer());
            ParsedAnswer incoming = e.getValue();
            if (StringUtils.isNotEmpty(incoming.getAnswerRaw()))
            {
                existing.setAnswerRaw(incoming.getAnswerRaw());
            }
            if (StringUtils.isNotEmpty(incoming.getAnalysis()))
            {
                existing.setAnalysis(incoming.getAnalysis());
            }
        }
    }

    public static String formatAnswerJson(String rawAnswer, String questionType)
    {
        if (StringUtils.isEmpty(rawAnswer))
        {
            return null;
        }
        String value = rawAnswer.trim();
        if (EduQbConstants.TYPE_JUDGE.equals(questionType))
        {
            if (value.contains("\u6b63\u786e") || value.contains("\u5bf9") || "\u221a".equals(value) || "T".equalsIgnoreCase(value))
            {
                return JSON.toJSONString("true");
            }
            if (value.contains("\u9519\u8bef") || value.contains("\u9519") || "\u00d7".equals(value) || "F".equalsIgnoreCase(value))
            {
                return JSON.toJSONString("false");
            }
        }
        Matcher choice = Pattern.compile("^[A-Da-d](?:[\\.\\uFF0E\\u3001\\)\\uFF09].*)?$").matcher(value);
        if (choice.matches())
        {
            return JSON.toJSONString(value.substring(0, 1).toUpperCase());
        }
        Matcher multi = Pattern.compile("^[A-Da-d](?:\\s*[,\\uFF0C\\u3001\\s]\\s*[A-Da-d])+.*").matcher(value);
        if (multi.matches())
        {
            String letters = value.replaceAll("[^A-Da-d]", "").toUpperCase();
            return JSON.toJSONString(letters);
        }
        return JSON.toJSONString(value);
    }

    public static class ParsedAnswer
    {
        private String answerRaw;
        private String analysis;

        public String getAnswerRaw()
        {
            return answerRaw;
        }

        public void setAnswerRaw(String answerRaw)
        {
            this.answerRaw = answerRaw;
        }

        public String getAnalysis()
        {
            return analysis;
        }

        public void setAnalysis(String analysis)
        {
            this.analysis = analysis;
        }
    }
}
