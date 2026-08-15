package com.ruoyi.system.controller.wx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.security.utils.SecurityUtils;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.system.domain.education.EduQbOcrCommitBody;
import com.ruoyi.system.service.education.IEduQbOcrService;

@RestController
@RequestMapping("/wx/teacher/question")
public class WxTeacherQuestionController extends BaseController
{
    @Autowired
    private IEduQbOcrService ocrService;

    @GetMapping("/subjects")
    public AjaxResult subjects()
    {
        return success(ocrService.listSubjects());
    }

    @PostMapping("/ocr/recognize")
    public AjaxResult recognize(@RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long subjectId)
    {
        return success(ocrService.recognize(file, subjectId, SecurityUtils.getUsername()));
    }

    @PostMapping("/ocr/commit")
    public AjaxResult commit(@RequestBody EduQbOcrCommitBody body)
    {
        return success(ocrService.commit(body, SecurityUtils.getUsername()));
    }
}
