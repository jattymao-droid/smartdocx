package com.ruoyi.system.domain.education;

public class EduQbWrongBookStats
{
    private Integer activeCount;
    private Integer masteredCount;
    private Integer totalWrongAttempts;

    public Integer getActiveCount()
    {
        return activeCount;
    }

    public void setActiveCount(Integer activeCount)
    {
        this.activeCount = activeCount;
    }

    public Integer getMasteredCount()
    {
        return masteredCount;
    }

    public void setMasteredCount(Integer masteredCount)
    {
        this.masteredCount = masteredCount;
    }

    public Integer getTotalWrongAttempts()
    {
        return totalWrongAttempts;
    }

    public void setTotalWrongAttempts(Integer totalWrongAttempts)
    {
        this.totalWrongAttempts = totalWrongAttempts;
    }
}
