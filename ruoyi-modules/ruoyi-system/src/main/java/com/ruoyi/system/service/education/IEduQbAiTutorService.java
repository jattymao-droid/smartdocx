package com.ruoyi.system.service.education;

import com.ruoyi.system.domain.education.EduQbAiTutorConfigVO;
import com.ruoyi.system.domain.education.EduQbAiTutorRequest;
import com.ruoyi.system.domain.education.EduQbAiTutorResponse;

public interface IEduQbAiTutorService
{
    EduQbAiTutorConfigVO selectConfig();

    EduQbAiTutorResponse chat(EduQbAiTutorRequest request);
}
