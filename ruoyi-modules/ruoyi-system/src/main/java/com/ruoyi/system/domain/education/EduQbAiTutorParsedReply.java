package com.ruoyi.system.domain.education;

import java.util.ArrayList;
import java.util.List;

public class EduQbAiTutorParsedReply
{
    private String reply;

    private String choiceQuestion;

    private List<EduQbAiTutorChoice> choices = new ArrayList<>();

    public EduQbAiTutorParsedReply()
    {
    }

    public EduQbAiTutorParsedReply(String reply, String choiceQuestion, List<EduQbAiTutorChoice> choices)
    {
        this.reply = reply;
        this.choiceQuestion = choiceQuestion;
        this.choices = choices != null ? choices : new ArrayList<>();
    }

    public String getReply()
    {
        return reply;
    }

    public void setReply(String reply)
    {
        this.reply = reply;
    }

    public String getChoiceQuestion()
    {
        return choiceQuestion;
    }

    public void setChoiceQuestion(String choiceQuestion)
    {
        this.choiceQuestion = choiceQuestion;
    }

    public List<EduQbAiTutorChoice> getChoices()
    {
        return choices;
    }

    public void setChoices(List<EduQbAiTutorChoice> choices)
    {
        this.choices = choices != null ? choices : new ArrayList<>();
    }
}
