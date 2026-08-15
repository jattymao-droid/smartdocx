package com.ruoyi.system.domain.education;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class EduQbPaperPreviewRequest
{
    private String paperTitle;
    private Map<String, String> header;
    private String templateCode;
    private String sortMode;
    private String exportMode;
    private Map<String, Object> exportConfig;
    private List<EduQbPaperItemRequest> items;

    public String getPaperTitle()
    {
        return paperTitle;
    }

    public void setPaperTitle(String paperTitle)
    {
        this.paperTitle = paperTitle;
    }

    public Map<String, String> getHeader()
    {
        return header;
    }

    public void setHeader(Map<String, String> header)
    {
        this.header = header;
    }

    public String getTemplateCode()
    {
        return templateCode;
    }

    public void setTemplateCode(String templateCode)
    {
        this.templateCode = templateCode;
    }

    public String getSortMode()
    {
        return sortMode;
    }

    public void setSortMode(String sortMode)
    {
        this.sortMode = sortMode;
    }

    public String getExportMode()
    {
        return exportMode;
    }

    public void setExportMode(String exportMode)
    {
        this.exportMode = exportMode;
    }

    public Map<String, Object> getExportConfig()
    {
        return exportConfig;
    }

    public void setExportConfig(Map<String, Object> exportConfig)
    {
        this.exportConfig = exportConfig;
    }

    public List<EduQbPaperItemRequest> getItems()
    {
        return items;
    }

    public void setItems(List<EduQbPaperItemRequest> items)
    {
        this.items = items;
    }
}
