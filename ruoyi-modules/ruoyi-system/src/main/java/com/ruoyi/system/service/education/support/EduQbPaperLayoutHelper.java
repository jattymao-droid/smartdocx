package com.ruoyi.system.service.education.support;

import com.ruoyi.system.domain.education.EduQbConstants;

public final class EduQbPaperLayoutHelper
{
    public static final int A4_WIDTH_MM = 210;
    public static final int A4_HEIGHT_MM = 297;
    public static final int A3_WIDTH_MM = 297;
    public static final int A3_HEIGHT_MM = 420;

    private EduQbPaperLayoutHelper()
    {
    }

    public static int[] pageSizeMm(String templateCode)
    {
        if (isA3(templateCode))
        {
            return new int[] { A3_WIDTH_MM, A3_HEIGHT_MM };
        }
        return new int[] { A4_WIDTH_MM, A4_HEIGHT_MM };
    }

    public static boolean isA3(String templateCode)
    {
        return templateCode != null && templateCode.toUpperCase().startsWith("A3");
    }

    public static String pageSizeCss(String templateCode)
    {
        return isA3(templateCode) ? "A3" : "A4";
    }

    public static long mmToTwips(int mm)
    {
        return Math.round(mm / 25.4 * 1440);
    }

    public static int marginMm(java.util.Map<String, Object> exportConfig, int defaultMm)
    {
        if (exportConfig == null)
        {
            return defaultMm;
        }
        Object raw = exportConfig.get("marginMm");
        if (raw instanceof Number)
        {
            return ((Number) raw).intValue();
        }
        if (raw != null)
        {
            try
            {
                return Integer.parseInt(String.valueOf(raw));
            }
            catch (NumberFormatException ignored)
            {
                return defaultMm;
            }
        }
        return defaultMm;
    }

    public static String normalizeTemplateCode(String templateCode)
    {
        if (isA3(templateCode))
        {
            return EduQbConstants.TEMPLATE_A3_1COL;
        }
        return EduQbConstants.TEMPLATE_A4_1COL;
    }
}
