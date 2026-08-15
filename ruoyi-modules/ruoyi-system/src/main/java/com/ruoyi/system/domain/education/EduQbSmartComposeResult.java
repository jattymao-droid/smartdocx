package com.ruoyi.system.domain.education;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class EduQbSmartComposeResult
{
    private String paperTitle;
    private List<EduQbSmartComposeQuestion> questions = new ArrayList<>();
    private BigDecimal totalScore;
    private List<String> warnings = new ArrayList<>();

    public String getPaperTitle()
    {
        return paperTitle;
    }

    public void setPaperTitle(String paperTitle)
    {
        this.paperTitle = paperTitle;
    }

    public List<EduQbSmartComposeQuestion> getQuestions()
    {
        return questions;
    }

    public void setQuestions(List<EduQbSmartComposeQuestion> questions)
    {
        this.questions = questions;
    }

    public BigDecimal getTotalScore()
    {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore)
    {
        this.totalScore = totalScore;
    }

    public List<String> getWarnings()
    {
        return warnings;
    }

    public void setWarnings(List<String> warnings)
    {
        this.warnings = warnings;
    }
}
