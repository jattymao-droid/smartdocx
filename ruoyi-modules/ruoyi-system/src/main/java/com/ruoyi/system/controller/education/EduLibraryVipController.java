package com.ruoyi.system.controller.education;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.domain.education.EduLibraryVipConfig;
import com.ruoyi.system.domain.education.EduLibraryVipExtendBody;
import com.ruoyi.system.domain.education.EduLibraryVipGrantBody;
import com.ruoyi.system.domain.education.EduLibraryVipMember;
import com.ruoyi.system.service.education.IEduLibraryVipService;

@RestController
@RequestMapping("/education/library/vip")
public class EduLibraryVipController extends BaseController
{
    @Autowired
    private IEduLibraryVipService vipService;

    @GetMapping("/status")
    public AjaxResult status()
    {
        return success(vipService.getStatus(safeUsername()));
    }

    @GetMapping("/recent")
    public AjaxResult recent(@org.springframework.web.bind.annotation.RequestParam(value = "limit", defaultValue = "8") int limit)
    {
        return success(vipService.selectRecentVipOrders(limit));
    }

    @RequiresPermissions("education:library:vip")
    @GetMapping("/admin/config")
    public AjaxResult adminConfig()
    {
        return success(vipService.resolveConfig());
    }

    @RequiresPermissions("education:library:vip")
    @PutMapping("/admin/config")
    public AjaxResult saveAdminConfig(@RequestBody EduLibraryVipConfig config)
    {
        vipService.saveConfig(config);
        return success();
    }

    @RequiresPermissions("education:library:vip")
    @GetMapping("/admin/list")
    public TableDataInfo adminList(EduLibraryVipMember query)
    {
        startPage();
        return getDataTable(vipService.selectVipMemberList(query));
    }

    @RequiresPermissions("education:library:vip")
    @PostMapping("/admin/grant")
    public AjaxResult grant(@RequestBody EduLibraryVipGrantBody body)
    {
        vipService.grantVip(body, SecurityUtils.getUsername());
        return success();
    }

    @RequiresPermissions("education:library:vip")
    @PutMapping("/admin/extend")
    public AjaxResult extend(@RequestBody EduLibraryVipExtendBody body)
    {
        vipService.extendVip(body, SecurityUtils.getUsername());
        return success();
    }

    @RequiresPermissions("education:library:vip")
    @DeleteMapping("/admin/{username}")
    public AjaxResult disable(@PathVariable String username)
    {
        vipService.disableVip(username, SecurityUtils.getUsername());
        return success();
    }

    private String safeUsername()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (Exception ex)
        {
            return null;
        }
    }
}
