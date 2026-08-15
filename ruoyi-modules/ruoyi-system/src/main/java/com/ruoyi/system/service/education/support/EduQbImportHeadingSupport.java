package com.ruoyi.system.service.education.support;

import java.util.regex.Pattern;
import com.ruoyi.common.core.utils.StringUtils;

public final class EduQbImportHeadingSupport
{
    private static final String CN_NUM = "\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u4e03\u516b\u4e5d\u5341\u767e\u5343";
    private static final Pattern CHAPTER_HEADING = Pattern.compile(
            "^(\\u7b2c[0-9" + CN_NUM + "]+[\\u7ae0\\u8282\\u7f16\\u8bfe]|"
                    + "[0-9]+(\\.[0-9]+){0,2}\\s+[^0-9\\?\\uFF1F].+|"
                    + "[" + CN_NUM + "]+[\\u3001\\uFF0E.]\\s*\\S+).+",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern QUESTION_START = Pattern.compile(
            "^\\s*([0-9]+[.\\u3001\\uFF0E\\)]|[\\uFF08(][0-9]+[\\)\\uFF09]|[\\u2460-\\u2469])");

    private EduQbImportHeadingSupport()
    {
    }

    public static boolean isChapterHeading(String text)
    {
        if (StringUtils.isEmpty(text))
        {
            return false;
        }
        String normalized = text.trim();
        if (normalized.length() > 80 || normalized.length() < 2)
        {
            return false;
        }
        if (normalized.contains("\uFF1F") || normalized.contains("?"))
        {
            return false;
        }
        if (QUESTION_START.matcher(normalized).find())
        {
            return false;
        }
        if (normalized.matches(".*[A-Ha-h][.\\u3001\\uFF0E:\\uFF1A].*") && normalized.length() > 30)
        {
            return false;
        }
        return CHAPTER_HEADING.matcher(normalized).matches();
    }

    public static String cleanHeadingHint(String text)
    {
        if (StringUtils.isEmpty(text))
        {
            return "";
        }
        String normalized = text.trim().replaceAll("\\s+", " ");
        normalized = normalized.replaceFirst("^\\u7b2c[0-9" + CN_NUM + "]+[\\u7ae0\\u8282\\u7f16\\u8bfe]\\s*", "");
        normalized = normalized.replaceFirst("^[0-9]+(\\.[0-9]+)*\\s+", "");
        normalized = normalized.replaceFirst("^[" + CN_NUM + "]+[\\u3001\\uFF0E.]\\s*", "");
        return normalized.trim();
    }
}
