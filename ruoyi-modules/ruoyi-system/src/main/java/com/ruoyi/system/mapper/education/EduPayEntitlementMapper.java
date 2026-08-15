package com.ruoyi.system.mapper.education;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.education.EduPayEntitlement;

public interface EduPayEntitlementMapper
{
    int countEntitlement(@Param("username") String username, @Param("bizType") String bizType,
            @Param("bizId") Long bizId, @Param("bizRef") String bizRef);

    int insertEntitlement(EduPayEntitlement entitlement);
}
