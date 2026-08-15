package com.ruoyi.system.domain.education;

import java.math.BigDecimal;
import com.ruoyi.common.core.web.domain.BaseEntity;

public class EduQbOcrDraft extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    public static final String STATUS_DRAFT = "draft";
    public static final String STATUS_COMMITTED = "committed";

    private Long draftId;
    private String imagePath;
    /** Manually cropped figure path (not full OCR page) */
    private String figurePath;
    private String ocrText;
    private String ocrLines;
    private BigDecimal confidence;
    private String predictedType;
    private BigDecimal predictedDifficulty;
    private String predictedOptions;
    private Long subjectId;
    private String status;
    private Long questionId;
    private String subjectName;

    public Long getDraftId()
    {
        return draftId;
    }

    public void setDraftId(Long draftId)
    {
        this.draftId = draftId;
    }

    public String getImagePath()
    {
        return imagePath;
    }

    public void setImagePath(String imagePath)
    {
        this.imagePath = imagePath;
    }

    public String getFigurePath()
    {
        return figurePath;
    }

    public void setFigurePath(String figurePath)
    {
        this.figurePath = figurePath;
    }

    public String getOcrText()
    {
        return ocrText;
    }

    public void setOcrText(String ocrText)
    {
        this.ocrText = ocrText;
    }

    public String getOcrLines()
    {
        return ocrLines;
    }

    public void setOcrLines(String ocrLines)
    {
        this.ocrLines = ocrLines;
    }

    public BigDecimal getConfidence()
    {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence)
    {
        this.confidence = confidence;
    }

    public String getPredictedType()
    {
        return predictedType;
    }

    public void setPredictedType(String predictedType)
    {
        this.predictedType = predictedType;
    }

    public BigDecimal getPredictedDifficulty()
    {
        return predictedDifficulty;
    }

    public void setPredictedDifficulty(BigDecimal predictedDifficulty)
    {
        this.predictedDifficulty = predictedDifficulty;
    }

    public String getPredictedOptions()
    {
        return predictedOptions;
    }

    public void setPredictedOptions(String predictedOptions)
    {
        this.predictedOptions = predictedOptions;
    }

    public Long getSubjectId()
    {
        return subjectId;
    }

    public void setSubjectId(Long subjectId)
    {
        this.subjectId = subjectId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Long getQuestionId()
    {
        return questionId;
    }

    public void setQuestionId(Long questionId)
    {
        this.questionId = questionId;
    }

    public String getSubjectName()
    {
        return subjectName;
    }

    public void setSubjectName(String subjectName)
    {
        this.subjectName = subjectName;
    }
}
