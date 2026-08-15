package com.ruoyi.system.domain.education;

import java.math.BigDecimal;

public class EduQbOcrLine
{
    private String text;

    private BigDecimal confidence;

    public EduQbOcrLine()
    {
    }

    public EduQbOcrLine(String text, BigDecimal confidence)
    {
        this.text = text;
        this.confidence = confidence;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public BigDecimal getConfidence()
    {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence)
    {
        this.confidence = confidence;
    }
}
