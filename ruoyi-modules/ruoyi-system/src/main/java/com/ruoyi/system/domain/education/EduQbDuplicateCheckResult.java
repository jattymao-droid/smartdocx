package com.ruoyi.system.domain.education;

import java.util.ArrayList;
import java.util.List;

public class EduQbDuplicateCheckResult
{
    private String contentHash;

    private List<EduQbQuestion> exactMatches = new ArrayList<>();

    private List<EduQbSimilarQuestion> similarMatches = new ArrayList<>();

    public String getContentHash()
    {
        return contentHash;
    }

    public void setContentHash(String contentHash)
    {
        this.contentHash = contentHash;
    }

    public List<EduQbQuestion> getExactMatches()
    {
        return exactMatches;
    }

    public void setExactMatches(List<EduQbQuestion> exactMatches)
    {
        this.exactMatches = exactMatches;
    }

    public List<EduQbSimilarQuestion> getSimilarMatches()
    {
        return similarMatches;
    }

    public void setSimilarMatches(List<EduQbSimilarQuestion> similarMatches)
    {
        this.similarMatches = similarMatches;
    }
}
