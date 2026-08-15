package com.ruoyi.system.service.education;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.education.EduPayCheckResult;
import com.ruoyi.system.domain.education.EduPayCreateBody;
import com.ruoyi.system.domain.education.EduPayOrder;

public interface IEduPayService
{
    int PAY_REQUIRED_CODE = 402;

    EduPayCheckResult checkAccess(String bizType, Long bizId, String bizRef, String username);

    void assertAccess(String bizType, Long bizId, String bizRef, String username);

    EduPayOrder createOrder(EduPayCreateBody body, String username, String clientIp);

    EduPayOrder getOrderStatus(String orderNo, String username);

    List<EduPayOrder> selectMyOrderList(EduPayOrder query, String username);

    String handleZPayNotify(Map<String, String> params);
}
