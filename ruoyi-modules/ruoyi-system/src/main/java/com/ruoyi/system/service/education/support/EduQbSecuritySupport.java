package com.ruoyi.system.service.education.support;

import java.util.Set;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.api.model.LoginUser;

/**
 * Role helpers for question-bank services (Cloud SecurityUtils has no hasRole).
 */
public final class EduQbSecuritySupport
{
    private EduQbSecuritySupport()
    {
    }

    public static boolean hasRole(String roleKey)
    {
        if (SecurityUtils.isAdmin())
        {
            return true;
        }
        LoginUser user = SecurityUtils.getLoginUser();
        if (user == null || user.getRoles() == null || roleKey == null)
        {
            return false;
        }
        Set<String> roles = user.getRoles();
        return roles.contains(roleKey) || roles.contains("ROLE_" + roleKey);
    }

    /** Super admin (userId=1), admin role, or edu_admin may manage any question. */
    public static boolean isQuestionBankManager()
    {
        return hasRole("admin") || hasRole("edu_admin");
    }
}
