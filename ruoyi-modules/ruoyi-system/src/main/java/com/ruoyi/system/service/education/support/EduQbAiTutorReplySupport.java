package com.ruoyi.system.service.education.support;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.domain.education.EduQbAiTutorChoice;
import com.ruoyi.system.domain.education.EduQbAiTutorParsedReply;

public final class EduQbAiTutorReplySupport
{
    private static final Pattern CHOICES_BLOCK = Pattern.compile("\\[CHOICES\\]([\\s\\S]*?)\\[/CHOICES\\]",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern CHOICE_LINE = Pattern.compile("^([A-Da-d])[.\\u3001\\uFF0E)\\]:]\\s*(.+)$");

    private EduQbAiTutorReplySupport()
    {
    }

    public static EduQbAiTutorParsedReply parse(String raw)
    {
        if (StringUtils.isEmpty(raw))
        {
            return new EduQbAiTutorParsedReply("", null, new ArrayList<>());
        }

        Matcher matcher = CHOICES_BLOCK.matcher(raw);
        if (!matcher.find())
        {
            return new EduQbAiTutorParsedReply(raw.trim(), null, new ArrayList<>());
        }

        String reply = (raw.substring(0, matcher.start()) + raw.substring(matcher.end())).trim();
        ParsedChoices parsedChoices = parseChoicesBlock(matcher.group(1));
        return new EduQbAiTutorParsedReply(reply, parsedChoices.question, parsedChoices.choices);
    }

    public static String appendDefaultChoicesBlock(String reply)
    {
        StringBuilder sb = new StringBuilder(reply == null ? "" : reply.trim());
        if (sb.length() > 0)
        {
            sb.append("\n\n");
        }
        sb.append("[CHOICES]\n");
        sb.append("\u95ee\uff1a\u4f60\u63a5\u4e0b\u6765\u60f3\u600e\u4e48\u7ee7\u7eed\uff1f\n");
        sb.append("A. \u8bf7\u518d\u7ed9\u4e00\u4e2a\u601d\u8def\u63d0\u793a\n");
        sb.append("B. \u6211\u60f3\u770b\u8fd9\u4e00\u6b65\u7528\u5230\u7684\u516c\u5f0f\n");
        sb.append("C. \u5e2e\u6211\u5206\u6790\u9898\u76ee\u91cc\u7684\u5173\u952e\u6761\u4ef6\n");
        sb.append("D. \u6211\u8fd8\u4e0d\u592a\u786e\u5b9a\uff0c\u8bf7\u518d\u63d0\u793a\u4e00\u4e0b\n");
        sb.append("[/CHOICES]");
        return sb.toString();
    }

    private static ParsedChoices parseChoicesBlock(String block)
    {
        ParsedChoices result = new ParsedChoices();
        if (StringUtils.isEmpty(block))
        {
            return result;
        }

        String[] lines = block.replace("\r\n", "\n").split("\n");
        for (String line : lines)
        {
            String trimmed = line.trim();
            if (StringUtils.isEmpty(trimmed))
            {
                continue;
            }
            if (trimmed.startsWith("\u95ee\uff1a") || trimmed.startsWith("\u95ee:"))
            {
                result.question = trimmed.substring(2).trim();
                continue;
            }
            Matcher matcher = CHOICE_LINE.matcher(trimmed);
            if (!matcher.matches())
            {
                continue;
            }
            EduQbAiTutorChoice choice = new EduQbAiTutorChoice();
            choice.setId(matcher.group(1).toUpperCase());
            choice.setLabel(matcher.group(2).trim());
            result.choices.add(choice);
        }
        return result;
    }

    private static final class ParsedChoices
    {
        private String question;

        private final List<EduQbAiTutorChoice> choices = new ArrayList<>();
    }
}
