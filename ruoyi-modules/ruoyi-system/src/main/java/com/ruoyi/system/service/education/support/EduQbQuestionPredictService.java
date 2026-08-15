package com.ruoyi.system.service.education.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.domain.education.EduQbConstants;
import com.ruoyi.system.domain.education.EduQbOcrLine;

@Component
public class EduQbQuestionPredictService
{
    private static final Pattern OPTION_PATTERN = Pattern.compile("^[A-Ha-h][\\.\\uFF0E\\u3001\\)\\uFF09:]\\s*(.*)$");

    public static class PredictResult
    {
        private String questionType;
        private BigDecimal difficulty;
        private String content;
        private String optionsJson;

        public String getQuestionType()
        {
            return questionType;
        }

        public void setQuestionType(String questionType)
        {
            this.questionType = questionType;
        }

        public BigDecimal getDifficulty()
        {
            return difficulty;
        }

        public void setDifficulty(BigDecimal difficulty)
        {
            this.difficulty = difficulty;
        }

        public String getContent()
        {
            return content;
        }

        public void setContent(String content)
        {
            this.content = content;
        }

        public String getOptionsJson()
        {
            return optionsJson;
        }

        public void setOptionsJson(String optionsJson)
        {
            this.optionsJson = optionsJson;
        }
    }

    public PredictResult predict(List<EduQbOcrLine> lines)
    {
        PredictResult result = new PredictResult();
        if (lines == null || lines.isEmpty())
        {
            result.setQuestionType(EduQbConstants.TYPE_SHORT);
            result.setDifficulty(new BigDecimal("0.50"));
            result.setContent("");
            return result;
        }
        List<String> optionLines = new ArrayList<>();
        List<String> stemLines = new ArrayList<>();
        for (EduQbOcrLine line : lines)
        {
            if (line == null || StringUtils.isEmpty(line.getText()))
            {
                continue;
            }
            Matcher matcher = OPTION_PATTERN.matcher(line.getText().trim());
            if (matcher.matches())
            {
                String label = line.getText().trim().substring(0, 1).toUpperCase(Locale.ROOT);
                optionLines.add(label + "." + matcher.group(1).trim());
            }
            else
            {
                stemLines.add(line.getText().trim());
            }
        }
        String fullText = joinLines(lines);
        String content = stemLines.isEmpty() ? fullText : String.join("\n", stemLines);
        result.setContent(content);
        result.setQuestionType(detectTypeFromText(fullText, optionLines.size(), null));
        if (optionLines.size() >= 2)
        {
            result.setOptionsJson(JSON.toJSONString(optionLines));
        }
        result.setDifficulty(estimateDifficulty(fullText, optionLines.size()));
        return result;
    }

    public static String detectTypeFromText(String fullText, int optionCount, String sectionName)
    {
        String fromSection = detectTypeFromSection(sectionName);
        if (fromSection != null)
        {
            return fromSection;
        }
        return detectTypeFromContent(fullText, optionCount);
    }

    public static int countOptionLines(String text)
    {
        if (StringUtils.isEmpty(text))
        {
            return 0;
        }
        int count = 0;
        for (String line : text.split("\\r?\\n"))
        {
            if (OPTION_PATTERN.matcher(line.trim()).matches())
            {
                count++;
            }
        }
        return count;
    }

    private static String detectTypeFromSection(String sectionName)
    {
        if (StringUtils.isEmpty(sectionName))
        {
            return null;
        }
        String section = sectionName.replace('\n', ' ').trim();
        if (section.contains("\u591a\u9009") || section.contains("\u591a\u9879\u9009\u62e9"))
        {
            return EduQbConstants.TYPE_MULTI;
        }
        if (section.contains("\u5355\u9009") || section.contains("\u9009\u62e9\u9898"))
        {
            return EduQbConstants.TYPE_SINGLE;
        }
        if (section.contains("\u5224\u65ad"))
        {
            return EduQbConstants.TYPE_JUDGE;
        }
        if (section.contains("\u77e5\u8bc6\u70b9\u586b\u7a7a") || section.contains("\u77e5\u8bc6\u586b\u7a7a"))
        {
            return EduQbConstants.TYPE_KNOWLEDGE_FILL;
        }
        if (section.contains("\u586b\u7a7a"))
        {
            return EduQbConstants.TYPE_FILL;
        }
        if (section.contains("\u5b9e\u9a8c"))
        {
            return EduQbConstants.TYPE_EXPERIMENT;
        }
        if (section.contains("\u4f5c\u56fe"))
        {
            return EduQbConstants.TYPE_DRAWING;
        }
        if (section.contains("\u9605\u8bfb"))
        {
            return EduQbConstants.TYPE_READING;
        }
        if (section.contains("\u7efc\u5408"))
        {
            return EduQbConstants.TYPE_COMPREHENSIVE;
        }
        if (section.contains("\u89e3\u7b54") || section.contains("\u8ba1\u7b97") || section.contains("\u8bc1\u660e"))
        {
            return EduQbConstants.TYPE_ANSWER;
        }
        if (section.contains("\u7b80\u7b54"))
        {
            return EduQbConstants.TYPE_SHORT;
        }
        return null;
    }

    private static String detectTypeFromContent(String fullText, int optionCount)
    {
        String text = fullText == null ? "" : fullText;
        if (text.contains("\u591a\u9009") || text.contains("\u591a\u9879\u9009\u62e9"))
        {
            return EduQbConstants.TYPE_MULTI;
        }
        if (optionCount >= 2)
        {
            return EduQbConstants.TYPE_SINGLE;
        }
        if (text.contains("\u5224\u65ad") || text.contains("\u5bf9\u9519") || text.contains("\u221a") || text.contains("\u00d7"))
        {
            return EduQbConstants.TYPE_JUDGE;
        }
        if (text.contains("\u77e5\u8bc6\u70b9\u586b\u7a7a") || text.contains("\u77e5\u8bc6\u586b\u7a7a"))
        {
            return EduQbConstants.TYPE_KNOWLEDGE_FILL;
        }
        if (text.contains("\u5b9e\u9a8c") || text.contains("\u6d4b\u5b9a") || text.contains("\u88c5\u7f6e\u56fe"))
        {
            return EduQbConstants.TYPE_EXPERIMENT;
        }
        if (text.contains("\u4f5c\u56fe") || text.contains("\u753b\u51fa"))
        {
            return EduQbConstants.TYPE_DRAWING;
        }
        if (text.contains("\u9605\u8bfb\u7406\u89e3") || text.contains("\u9605\u8bfb\u9898")
                || (text.contains("\u9605\u8bfb") && text.length() > 120))
        {
            return EduQbConstants.TYPE_READING;
        }
        if (text.contains("\u7efc\u5408"))
        {
            return EduQbConstants.TYPE_COMPREHENSIVE;
        }
        if (text.contains("\u89e3\u7b54\u9898") || text.contains("\u8ba1\u7b97\u9898") || text.contains("\u8bc1\u660e\u9898"))
        {
            return EduQbConstants.TYPE_ANSWER;
        }
        if (text.contains("____") || text.contains("\uff08  \uff09") || text.contains("\u3010  \u3011")
                || text.matches(".*(_{2,}|\uff3f{2,}).*"))
        {
            return EduQbConstants.TYPE_FILL;
        }
        return EduQbConstants.TYPE_SHORT;
    }

    private String detectType(String fullText, int optionCount)
    {
        return detectTypeFromContent(fullText, optionCount);
    }

    private BigDecimal estimateDifficulty(String text, int optionCount)
    {
        int len = text == null ? 0 : text.length();
        double score = 0.35;
        if (len > 80)
        {
            score += 0.15;
        }
        if (len > 160)
        {
            score += 0.10;
        }
        if (optionCount >= 4)
        {
            score += 0.10;
        }
        if (text != null && (text.contains("\u8ba1\u7b97") || text.contains("\u63a8\u5bfc") || text.contains("\u8bc1\u660e")))
        {
            score += 0.15;
        }
        if (score > 0.95)
        {
            score = 0.95;
        }
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    private String joinLines(List<EduQbOcrLine> lines)
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
}
