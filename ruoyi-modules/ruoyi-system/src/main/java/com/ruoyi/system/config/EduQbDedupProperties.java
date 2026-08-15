package com.ruoyi.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Question bank deduplication configuration.
 */
@Component
@ConfigurationProperties(prefix = "edu.qb.dedup")
public class EduQbDedupProperties
{
    /** Enable duplicate detection on save and via check API. */
    private boolean enabled = true;

    /** Reject insert/update when an exact hash match exists in the same subject. */
    private boolean blockExactDuplicate = false;

    /** Minimum Jaccard similarity (0~1) to report as a similar question. */
    private double similarityThreshold = 0.85;

    /** Max recent questions scanned per subject for fuzzy matching. */
    private int candidateLimit = 300;

    /** Max similar hits returned to the client. */
    private int resultLimit = 10;

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public boolean isBlockExactDuplicate()
    {
        return blockExactDuplicate;
    }

    public void setBlockExactDuplicate(boolean blockExactDuplicate)
    {
        this.blockExactDuplicate = blockExactDuplicate;
    }

    public double getSimilarityThreshold()
    {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold)
    {
        this.similarityThreshold = similarityThreshold;
    }

    public int getCandidateLimit()
    {
        return candidateLimit;
    }

    public void setCandidateLimit(int candidateLimit)
    {
        this.candidateLimit = candidateLimit;
    }

    public int getResultLimit()
    {
        return resultLimit;
    }

    public void setResultLimit(int resultLimit)
    {
        this.resultLimit = resultLimit;
    }
}
