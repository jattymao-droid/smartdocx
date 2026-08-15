package com.ruoyi.system.domain.education;

public class EduQbWeakComposeRequest extends EduQbSmartComposeRequest
{
    /** Max weak chapters to include; default 3 */
    private Integer weakChapterLimit;

    public Integer getWeakChapterLimit()
    {
        return weakChapterLimit;
    }

    public void setWeakChapterLimit(Integer weakChapterLimit)
    {
        this.weakChapterLimit = weakChapterLimit;
    }
}
