package com.ruoyi.system.domain.education;

import java.math.BigDecimal;
import java.util.List;

public class EduQbPaperPreviewResult
{
    private String html;
    private BigDecimal totalScore;
    private List<EduQbPaperTypeStat> typeStats;

    public String getHtml()
    {
        return html;
    }

    public void setHtml(String html)
    {
        this.html = html;
    }

    public BigDecimal getTotalScore()
    {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore)
    {
        this.totalScore = totalScore;
    }

    public List<EduQbPaperTypeStat> getTypeStats()
    {
        return typeStats;
    }

    public void setTypeStats(List<EduQbPaperTypeStat> typeStats)
    {
        this.typeStats = typeStats;
    }
}
