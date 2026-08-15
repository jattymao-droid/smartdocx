package com.ruoyi.system.mapper.education;

import java.util.Date;
import java.util.List;
import com.ruoyi.system.domain.education.EduLibraryVipMember;

public interface EduLibraryVipMemberMapper
{
    EduLibraryVipMember selectByUsername(String username);

    List<EduLibraryVipMember> selectVipMemberList(EduLibraryVipMember query);

    int insertVipMember(EduLibraryVipMember member);

    int updateVipMember(EduLibraryVipMember member);

    int markExpiredBefore(Date now);
}
