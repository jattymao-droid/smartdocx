package com.ruoyi.system.domain.education;

import java.math.BigDecimal;

public class EduQbPaperTypeStat
{
    private String type;
    private String typeLabel;
    private int count;
    private BigDecimal score;

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getTypeLabel()
    {
        return typeLabel;
    }

    public void setTypeLabel(String typeLabel)
    {
        this.typeLabel = typeLabel;
    }

    public int getCount()
    {
        return count;
    }

    public void setCount(int count)
    {
        this.count = count;
    }

    public BigDecimal getScore()
    {
        return score;
    }

    public void setScore(BigDecimal score)
    {
        this.score = score;
    }
}
