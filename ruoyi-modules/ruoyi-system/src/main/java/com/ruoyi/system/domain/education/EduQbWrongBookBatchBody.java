package com.ruoyi.system.domain.education;

import java.util.List;

public class EduQbWrongBookBatchBody
{
    private List<Long> wrongIds;

    public List<Long> getWrongIds()
    {
        return wrongIds;
    }

    public void setWrongIds(List<Long> wrongIds)
    {
        this.wrongIds = wrongIds;
    }
}
