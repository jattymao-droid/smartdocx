package com.ruoyi.system.domain.education;

import com.ruoyi.common.core.web.domain.BaseEntity;

public class EduQbImportTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    public static final String STATUS_PARSED = "parsed";
    public static final String STATUS_IMPORTING = "importing";
    public static final String STATUS_DONE = "done";
    public static final String STATUS_FAILED = "failed";

    private Long taskId;
    private String fileName;
    private String filePath;
    private Long subjectId;
    private String status;
    private Integer blockCount;
    private Integer importedCount;
    private String parseResult;
    private String subjectName;

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String getFilePath()
    {
        return filePath;
    }

    public void setFilePath(String filePath)
    {
        this.filePath = filePath;
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

    public Integer getBlockCount()
    {
        return blockCount;
    }

    public void setBlockCount(Integer blockCount)
    {
        this.blockCount = blockCount;
    }

    public Integer getImportedCount()
    {
        return importedCount;
    }

    public void setImportedCount(Integer importedCount)
    {
        this.importedCount = importedCount;
    }

    public String getParseResult()
    {
        return parseResult;
    }

    public void setParseResult(String parseResult)
    {
        this.parseResult = parseResult;
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
