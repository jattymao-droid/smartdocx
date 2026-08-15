package com.ruoyi.system.mapper.education;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.education.EduQbPaperShare;

public interface EduQbPaperShareMapper
{
    int insertEduQbPaperShare(EduQbPaperShare row);

    EduQbPaperShare selectEduQbPaperShareById(@Param("shareId") String shareId);
}
