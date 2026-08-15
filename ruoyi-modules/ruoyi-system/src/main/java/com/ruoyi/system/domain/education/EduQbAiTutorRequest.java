package com.ruoyi.system.domain.education;

import java.util.ArrayList;
import java.util.List;

public class EduQbAiTutorRequest
{
    /** explain | hint | chat */
    private String mode;

    private Long questionId;

    private String message;

    private List<EduQbAiTutorMessage> history = new ArrayList<>();

    public String getMode()
    {
        return mode;
    }

    public void setMode(String mode)
    {
        this.mode = mode;
    }

    public Long getQuestionId()
    {
        return questionId;
    }

    public void setQuestionId(Long questionId)
    {
        this.questionId = questionId;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public List<EduQbAiTutorMessage> getHistory()
    {
        return history;
    }

    public void setHistory(List<EduQbAiTutorMessage> history)
    {
        this.history = history != null ? history : new ArrayList<>();
    }
}
