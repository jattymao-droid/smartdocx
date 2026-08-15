package com.ruoyi.system.domain.education;

public class EduQbStudentPracticeCheckResult
{
    private Boolean correct;
    private Boolean subjective;
    private String correctAnswer;
    private String analysis;

    public Boolean getCorrect()
    {
        return correct;
    }

    public void setCorrect(Boolean correct)
    {
        this.correct = correct;
    }

    public Boolean getSubjective()
    {
        return subjective;
    }

    public void setSubjective(Boolean subjective)
    {
        this.subjective = subjective;
    }

    public String getCorrectAnswer()
    {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer)
    {
        this.correctAnswer = correctAnswer;
    }

    public String getAnalysis()
    {
        return analysis;
    }

    public void setAnalysis(String analysis)
    {
        this.analysis = analysis;
    }
}
