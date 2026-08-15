package com.ruoyi.system.domain.education;

import java.util.List;

public class EduQbQuestionAuditBody
{
    public static final String ACTION_APPROVE = "approve";
    public static final String ACTION_REJECT = "reject";

    private List<Long> questionIds;

    /** approve | reject */
    private String action;

    /** Required when action is reject */
    private String remark;

    public List<Long> getQuestionIds()
    {
        return questionIds;
    }

    public void setQuestionIds(List<Long> questionIds)
    {
        this.questionIds = questionIds;
    }

    public String getAction()
    {
        return action;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }
}
