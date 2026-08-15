package com.ruoyi.system.service.education.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.utils.StringUtils;

/**
 * Evaluate student practice answers (aligned with frontend practiceAnswer.js).
 */
public final class EduQbPracticeAnswerSupport
{
    private static final Pattern OPTION_LETTER = Pattern.compile("^[A-H]$");

    private EduQbPracticeAnswerSupport()
    {
    }

    public static boolean isSubjectiveType(String questionType)
    {
        if (StringUtils.isEmpty(questionType))
        {
            return true;
        }
        switch (questionType)
        {
            case "single":
            case "multi":
            case "judge":
            case "fill":
            case "knowledge_fill":
                return false;
            default:
                return true;
        }
    }

    public static boolean evaluate(String questionType, String pickedAnswer, String correctAnswerRaw)
    {
        if (isSubjectiveType(questionType))
        {
            return false;
        }
        if ("multi".equals(questionType))
        {
            return isMultiCorrect(pickedAnswer, correctAnswerRaw);
        }
        if ("judge".equals(questionType))
        {
            return isJudgeCorrect(pickedAnswer, correctAnswerRaw);
        }
        if ("fill".equals(questionType) || "knowledge_fill".equals(questionType))
        {
            return isFillCorrect(pickedAnswer, correctAnswerRaw);
        }
        return isSingleCorrect(pickedAnswer, correctAnswerRaw);
    }

    public static String formatCorrectAnswerDisplay(String questionType, String correctAnswerRaw)
    {
        if (StringUtils.isEmpty(correctAnswerRaw))
        {
            return "";
        }
        Object val = coerceValue(correctAnswerRaw);
        if ("judge".equals(questionType))
        {
            String s = String.valueOf(val).toLowerCase(Locale.ROOT);
            if ("true".equals(s) || "1".equals(s))
            {
                return "\u6b63\u786e";
            }
            return "\u9519\u8bef";
        }
        if ("multi".equals(questionType))
        {
            return String.join("\u3001", parseLetters(correctAnswerRaw));
        }
        if (val instanceof List)
        {
            List<?> list = (List<?>) val;
            List<String> parts = new ArrayList<>();
            for (Object item : list)
            {
                parts.add(String.valueOf(item));
            }
            return String.join(" | ", parts);
        }
        return String.valueOf(val).replaceAll("^\"|\"$", "");
    }

    private static boolean isSingleCorrect(String picked, String correctAnswerRaw)
    {
        List<String> letters = parseLetters(correctAnswerRaw);
        return letters.contains(StringUtils.upperCase(StringUtils.trim(picked)));
    }

    private static boolean isMultiCorrect(String picked, String correctAnswerRaw)
    {
        List<String> pickedLetters = parseLetters(picked);
        List<String> correctLetters = parseLetters(correctAnswerRaw);
        if (pickedLetters.isEmpty() || pickedLetters.size() != correctLetters.size())
        {
            return false;
        }
        return pickedLetters.stream().allMatch(correctLetters::contains);
    }

    private static boolean isJudgeCorrect(String picked, String correctAnswerRaw)
    {
        Object raw = coerceValue(correctAnswerRaw);
        boolean correctTrue = raw == Boolean.TRUE
                || "true".equalsIgnoreCase(String.valueOf(raw))
                || "1".equals(String.valueOf(raw))
                || "\u6b63\u786e".equals(String.valueOf(raw));
        String pick = StringUtils.trim(picked).toLowerCase(Locale.ROOT);
        boolean pickedTrue = "true".equals(pick) || "1".equals(pick) || "\u6b63\u786e".equals(pick);
        boolean pickedFalse = "false".equals(pick) || "0".equals(pick) || "\u9519\u8bef".equals(pick);
        if (pickedTrue)
        {
            return correctTrue;
        }
        if (pickedFalse)
        {
            return !correctTrue;
        }
        return false;
    }

    private static boolean isFillCorrect(String picked, String correctAnswerRaw)
    {
        String user = normalizeText(picked);
        if (StringUtils.isEmpty(user))
        {
            return false;
        }
        Object val = coerceValue(correctAnswerRaw);
        if (val instanceof List)
        {
            for (Object item : (List<?>) val)
            {
                if (user.equals(normalizeText(String.valueOf(item))))
                {
                    return true;
                }
            }
            return false;
        }
        if (user.equals(normalizeText(String.valueOf(val))))
        {
            return true;
        }
        return user.equals(normalizeText(formatCorrectAnswerDisplay("fill", correctAnswerRaw)));
    }

    private static String normalizeText(String s)
    {
        return StringUtils.trim(s).replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static List<String> parseLetters(String raw)
    {
        if (StringUtils.isEmpty(raw))
        {
            return Collections.emptyList();
        }
        Object val = coerceValue(raw);
        List<String> letters = new ArrayList<>();
        if (val instanceof List)
        {
            for (Object item : (List<?>) val)
            {
                expandToken(String.valueOf(item), letters);
            }
        }
        else
        {
            expandToken(String.valueOf(val), letters);
        }
        Collections.sort(letters);
        return letters;
    }

    private static void expandToken(String token, List<String> letters)
    {
        String s = StringUtils.trim(token).toUpperCase(Locale.ROOT);
        if (StringUtils.isEmpty(s))
        {
            return;
        }
        if (OPTION_LETTER.matcher(s).matches())
        {
            pushLetter(s, letters);
            return;
        }
        String[] parts = s.split("[,????\\s]+");
        if (parts.length > 1)
        {
            for (String part : parts)
            {
                expandToken(part, letters);
            }
            return;
        }
        String chars = s.replaceAll("[^A-H]", "");
        if (!chars.isEmpty() && chars.chars().allMatch(c -> OPTION_LETTER.matcher(String.valueOf((char) c)).matches()))
        {
            for (char c : chars.toCharArray())
            {
                pushLetter(String.valueOf(c), letters);
            }
            return;
        }
        pushLetter(s, letters);
    }

    private static void pushLetter(String letter, List<String> letters)
    {
        String l = StringUtils.upperCase(StringUtils.trim(letter));
        if (OPTION_LETTER.matcher(l).matches() && !letters.contains(l))
        {
            letters.add(l);
        }
    }

    private static Object coerceValue(String raw)
    {
        if (raw == null)
        {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("[") || trimmed.startsWith("{"))
        {
            try
            {
                return JSON.parse(trimmed);
            }
            catch (Exception ignored)
            {
                return raw;
            }
        }
        return raw;
    }
}
