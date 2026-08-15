package com.ruoyi.system.controller;

import java.io.IOException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.constant.Constants;
import com.ruoyi.common.core.utils.file.MimeTypeUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.system.service.education.support.EduQbFileUploadUtils;
import com.ruoyi.system.service.education.support.EduQbLocalFileSupport;

/**
 * Common upload for question-bank images (compatible with RuoYi-Vue /common/upload).
 */
@RestController
@RequestMapping("/common")
public class SysCommonController extends BaseController
{
    @PostMapping("/upload")
    public AjaxResult uploadFile(MultipartFile file) throws Exception
    {
        String storedPath = EduQbFileUploadUtils.upload(
                EduQbLocalFileSupport.getUploadPath(), file, MimeTypeUtils.IMAGE_EXTENSION);
        String fileName = Constants.RESOURCE_PREFIX + storedPath;
        AjaxResult ajax = AjaxResult.success();
        ajax.put("fileName", fileName);
        ajax.put("url", fileName);
        return ajax;
    }
}
