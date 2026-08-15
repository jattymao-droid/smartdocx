package com.ruoyi.system.mapper.education;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.education.EduPayOrder;

public interface EduPayOrderMapper
{
    EduPayOrder selectEduPayOrderByOrderNo(@Param("orderNo") String orderNo);

    EduPayOrder selectPendingOrder(@Param("username") String username,
            @Param("bizType") String bizType,
            @Param("bizId") Long bizId,
            @Param("bizRef") String bizRef);

    List<EduPayOrder> selectMyPayOrderList(EduPayOrder query);

    List<EduPayOrder> selectRecentPaidOrders(@Param("bizType") String bizType, @Param("limit") int limit);

    int insertEduPayOrder(EduPayOrder order);

    int updateEduPayOrderPaid(EduPayOrder order);
}
