package com.ruoyi.system.service.education.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.ruoyi.system.domain.education.EduQbOcrLine;

/** OCR sidecar response metadata (Paddle/Pix2Text). */
public class OcrSidecarResult
{
    private List<EduQbOcrLine> lines = new ArrayList<>();

    private String provider = "paddleocr";

    private String mode = "text";

    private List<String> warnings = new ArrayList<>();

    public List<EduQbOcrLine> getLines()
    {
        return lines;
    }

    public void setLines(List<EduQbOcrLine> lines)
    {
        this.lines = lines != null ? lines : new ArrayList<>();
    }

    public String getProvider()
    {
        return provider;
    }

    public void setProvider(String provider)
    {
        this.provider = provider;
    }

    public String getMode()
    {
        return mode;
    }

    public void setMode(String mode)
    {
        this.mode = mode;
    }

    public List<String> getWarnings()
    {
        return warnings;
    }

    public void setWarnings(List<String> warnings)
    {
        this.warnings = warnings != null ? warnings : Collections.emptyList();
    }
}
