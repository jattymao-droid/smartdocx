package com.ruoyi.system.domain.education;

import java.util.ArrayList;
import java.util.List;

public class EduQbImportParsePayload
{
    private List<EduQbImportBlock> blocks = new ArrayList<>();

    private String previewHtml;

    public List<EduQbImportBlock> getBlocks()
    {
        return blocks;
    }

    public void setBlocks(List<EduQbImportBlock> blocks)
    {
        this.blocks = blocks;
    }

    public String getPreviewHtml()
    {
        return previewHtml;
    }

    public void setPreviewHtml(String previewHtml)
    {
        this.previewHtml = previewHtml;
    }
}
