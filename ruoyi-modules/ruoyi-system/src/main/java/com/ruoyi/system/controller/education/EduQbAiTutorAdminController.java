package com.ruoyi.system.controller.education;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.system.domain.education.EduQbAiTutorAdminConfig;
import com.ruoyi.system.service.education.IEduQbAiTutorConfigService;

@RestController
@RequestMapping("/education/ai-tutor")
public class EduQbAiTutorAdminController extends BaseController
{
    @Autowired
    private IEduQbAiTutorConfigService aiTutorConfigService;

    @RequiresPermissions("education:ai-tutor:query")
    @GetMapping("/config")
    public AjaxResult getConfig()
    {
        return success(aiTutorConfigService.loadAdminConfig());
    }

    @RequiresPermissions("education:ai-tutor:edit")
    @PutMapping("/config")
    public AjaxResult saveConfig(@RequestBody EduQbAiTutorAdminConfig config)
    {
        aiTutorConfigService.saveAdminConfig(config);
        return success();
    }
}
