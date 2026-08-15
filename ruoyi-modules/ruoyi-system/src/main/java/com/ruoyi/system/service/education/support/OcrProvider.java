package com.ruoyi.system.service.education.support;

import java.util.List;
import com.ruoyi.system.domain.education.EduQbOcrLine;

public interface OcrProvider
{
    List<EduQbOcrLine> recognize(byte[] imageBytes);
}
