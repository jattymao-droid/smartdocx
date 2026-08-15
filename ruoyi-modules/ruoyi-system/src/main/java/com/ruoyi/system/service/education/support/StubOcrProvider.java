package com.ruoyi.system.service.education.support;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import com.ruoyi.system.domain.education.EduQbOcrLine;

@Component("stubOcrProvider")
public class StubOcrProvider implements OcrProvider
{
    @Override
    public List<EduQbOcrLine> recognize(byte[] imageBytes)
    {
        List<EduQbOcrLine> lines = new ArrayList<>();
        lines.add(new EduQbOcrLine("1. \u4e0b\u5217\u8bf4\u6cd5\u6b63\u786e\u7684\u662f\uff08  \uff09", new BigDecimal("0.92")));
        lines.add(new EduQbOcrLine("A. \u9009\u9879\u4e00", new BigDecimal("0.88")));
        lines.add(new EduQbOcrLine("B. \u9009\u9879\u4e8c", new BigDecimal("0.76")));
        lines.add(new EduQbOcrLine("C. \u9009\u9879\u4e09", new BigDecimal("0.65")));
        lines.add(new EduQbOcrLine("D. \u9009\u9879\u56db", new BigDecimal("0.91")));
        return lines;
    }
}
