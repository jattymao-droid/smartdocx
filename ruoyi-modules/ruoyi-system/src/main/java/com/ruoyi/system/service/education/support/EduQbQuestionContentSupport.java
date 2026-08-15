package com.ruoyi.system.service.education.support;

import java.util.regex.Pattern;
import com.ruoyi.common.core.utils.StringUtils;

public final class EduQbQuestionContentSupport
{
    private static final Pattern HTML_CONTENT = Pattern.compile(
            "<(table|img|p|div|span|tbody|tr|td|sub|sup|br)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern LEADING_QNO = Pattern.compile(
            "^[\\s\\u3000]*(?:"
                    + "\u7b2c\\s*\\d+\\s*\u9898[\\.\uFF0E\u3001:\uFF1A\\-\\u2014]?\\s*"
                    + "|\u7b2c\\s*[\u4e00-\u9fff]+\\s*\u9898[\\.\uFF0E\u3001:\uFF1A\\-\\u2014]?\\s*"
                    + "|[\\(\\uFF08]\\s*\\d+\\s*[\\)\\uFF09]\\s*[\\.\uFF0E\u3001]?\\s*"
                    + "|\\d+[\\.\uFF0E\u3001:\uFF1A\\-\\u2014]\\s*"
                    + "|[\u4e00-\u9fff]+[\u3001\uFF0E.]\\s*"
                    + ")");

    private EduQbQuestionContentSupport()
    {
    }

    public static boolean isHtmlContent(String text)
    {
        return StringUtils.isNotEmpty(text) && HTML_CONTENT.matcher(text).find();
    }

    public static String stripLeadingQuestionNo(String text)
    {
        if (StringUtils.isEmpty(text))
        {
            return "";
        }
        if (isHtmlContent(text))
        {
            return text.replace("\r", "").trim();
        }
        String result = text.replace("\r", "").trim();
        String prev;
        do
        {
            prev = result;
            result = LEADING_QNO.matcher(result).replaceFirst("").trim();
        }
        while (!result.equals(prev));
        return result;
    }
}
