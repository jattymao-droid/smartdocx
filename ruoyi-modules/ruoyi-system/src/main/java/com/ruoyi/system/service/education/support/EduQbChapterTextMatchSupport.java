package com.ruoyi.system.service.education.support;

/**
 * Chapter text fallback matching rules (legacy rows without chapter_id).
 * SQL fragments in mappers follow the same segment-boundary logic as {@link #matchesSegment}.
 */
public final class EduQbChapterTextMatchSupport
{
    private EduQbChapterTextMatchSupport()
    {
    }

    /**
     * Whether {@code chapterText} contains {@code segmentName} as a path segment
     * (exact leaf/parent label or delimited by " &gt; "), not as an arbitrary substring.
     */
    public static boolean matchesSegment(String chapterText, String segmentName)
    {
        if (chapterText == null || segmentName == null)
        {
            return false;
        }
        String text = chapterText.trim();
        String name = segmentName.trim();
        if (text.isEmpty() || name.isEmpty())
        {
            return false;
        }
        if (text.equals(name))
        {
            return true;
        }
        if (text.startsWith(name + " > "))
        {
            return true;
        }
        if (text.endsWith(" > " + name))
        {
            return true;
        }
        return text.contains(" > " + name + " > ");
    }
}
