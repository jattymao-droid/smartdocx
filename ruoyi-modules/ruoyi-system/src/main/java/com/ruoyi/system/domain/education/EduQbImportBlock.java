package com.ruoyi.system.domain.education;

import java.util.ArrayList;
import java.util.List;

public class EduQbImportBlock
{
    private int blockId;
    private int orderNum;
    private String text;
    /** heading | content */
    private String blockKind;
    /** Extracted inline image paths, e.g. /profile/upload/... */
    private List<String> imageUrls = new ArrayList<>();

    public int getBlockId()
    {
        return blockId;
    }

    public void setBlockId(int blockId)
    {
        this.blockId = blockId;
    }

    public int getOrderNum()
    {
        return orderNum;
    }

    public void setOrderNum(int orderNum)
    {
        this.orderNum = orderNum;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public String getBlockKind()
    {
        return blockKind;
    }

    public void setBlockKind(String blockKind)
    {
        this.blockKind = blockKind;
    }

    public List<String> getImageUrls()
    {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls)
    {
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
    }
}
