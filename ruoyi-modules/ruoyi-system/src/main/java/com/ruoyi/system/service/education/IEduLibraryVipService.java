package com.ruoyi.system.service.education;

import java.util.List;
import com.ruoyi.system.domain.education.EduLibraryVipConfig;
import com.ruoyi.system.domain.education.EduLibraryVipPlan;
import com.ruoyi.system.domain.education.EduLibraryVipRecentOrder;
import com.ruoyi.system.domain.education.EduLibraryVipExtendBody;
import com.ruoyi.system.domain.education.EduLibraryVipGrantBody;
import com.ruoyi.system.domain.education.EduLibraryVipMember;
import com.ruoyi.system.domain.education.EduLibraryVipStatus;
import com.ruoyi.system.domain.education.EduPayOrder;

public interface IEduLibraryVipService
{
    EduLibraryVipConfig resolveConfig();

    void saveConfig(EduLibraryVipConfig config);

    EduLibraryVipStatus getStatus(String username);

    List<EduLibraryVipPlan> resolvePlans();

    EduLibraryVipPlan resolvePlan(String planCode);

    List<EduLibraryVipRecentOrder> selectRecentVipOrders(int limit);

    boolean isActiveVip(String username);

    boolean grantsFreeDownload(String username);

    int resolvePreviewMaxPages(String username, int defaultPages);

    List<EduLibraryVipMember> selectVipMemberList(EduLibraryVipMember query);

    void grantVip(EduLibraryVipGrantBody body, String operator);

    void extendVip(EduLibraryVipExtendBody body, String operator);

    void disableVip(String username, String operator);

    void activateFromOrder(EduPayOrder order);
}
