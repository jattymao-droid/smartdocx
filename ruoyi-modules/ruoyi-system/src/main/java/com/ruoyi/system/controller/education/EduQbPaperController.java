package com.ruoyi.system.controller.education;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.domain.education.EduQbPaper;
import com.ruoyi.system.domain.education.EduQbPaperPreviewRequest;
import com.ruoyi.system.domain.education.EduQbPaperSaveRequest;
import com.ruoyi.system.domain.education.EduQbComposeTemplate;
import com.ruoyi.system.domain.education.EduQbSchoolPaperPublishRequest;
import com.ruoyi.system.domain.education.EduQbSmartComposeRequest;
import com.ruoyi.system.service.education.IEduQbExamPaperService;
import com.ruoyi.system.service.education.IEduQbComposeTemplateService;
import com.ruoyi.system.service.education.IEduQbPaperArchiveService;
import com.ruoyi.system.service.education.IEduQbPaperService;
import com.ruoyi.system.service.education.IEduQbPaperShareService;

@RestController
@RequestMapping("/education/paper")
public class EduQbPaperController extends BaseController
{
    @Autowired
    private IEduQbPaperService paperService;

    @Autowired
    private IEduQbPaperArchiveService paperArchiveService;

    @Autowired
    private IEduQbPaperShareService paperShareService;

    @Autowired
    private IEduQbExamPaperService examPaperService;

    @Autowired
    private IEduQbComposeTemplateService composeTemplateService;

    @RequiresPermissions("education:paper:preview")
    @PostMapping("/preview")
    public AjaxResult preview(@RequestBody EduQbPaperPreviewRequest request)
    {
        return success(paperService.previewPaper(request));
    }

    @RequiresPermissions("education:question:export")
    @PostMapping("/export/pdf")
    public AjaxResult exportPdf(@RequestBody EduQbPaperPreviewRequest request)
    {
        return success(paperService.exportPdf(request));
    }

    @RequiresPermissions("education:question:export")
    @PostMapping("/export/html")
    public AjaxResult exportHtml(@RequestBody EduQbPaperPreviewRequest request)
    {
        return success(paperService.exportHtml(request));
    }

    @RequiresPermissions("education:question:export")
    @PostMapping("/export/docx")
    public AjaxResult exportDocx(@RequestBody EduQbPaperPreviewRequest request)
    {
        return success(paperService.exportDocx(request));
    }

    @RequiresPermissions("education:paper:preview")
    @GetMapping("/mine/list")
    public TableDataInfo mineList(EduQbPaper query)
    {
        startPage();
        List<EduQbPaper> list = paperArchiveService.selectMyPaperList(query, SecurityUtils.getUsername());
        return getDataTable(list);
    }

    @RequiresPermissions("education:paper:preview")
    @GetMapping("/mine/{paperId}")
    public AjaxResult mineDetail(@PathVariable Long paperId)
    {
        return success(paperArchiveService.selectMyPaperDetail(paperId, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:paper:preview")
    @PostMapping("/mine/save")
    public AjaxResult mineSave(@RequestBody EduQbPaperSaveRequest request)
    {
        Long paperId = paperArchiveService.saveMyPaper(request, SecurityUtils.getUsername());
        return success(paperId);
    }

    @RequiresPermissions("education:paper:preview")
    @DeleteMapping("/mine/{paperId}")
    public AjaxResult mineDelete(@PathVariable Long paperId)
    {
        return toAjax(paperArchiveService.deleteMyPaper(paperId, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:paper:preview")
    @PostMapping("/share")
    public AjaxResult createShare(@RequestBody Map<String, Object> body)
    {
        Object snapshot = body != null ? body.get("snapshot") : null;
        if (snapshot == null)
        {
            return error("\u5206\u4eab\u5185\u5bb9\u4e0d\u80fd\u4e3a\u7a7a");
        }
        String json = com.alibaba.fastjson2.JSON.toJSONString(snapshot);
        String shareId = paperShareService.createShare(json, SecurityUtils.getUsername());
        return success(shareId);
    }

    /** Public read by share id (gateway whitelist). */
    @GetMapping("/share/{shareId}")
    public AjaxResult getShare(@PathVariable String shareId)
    {
        String json = paperShareService.getShareSnapshot(shareId);
        return success(com.alibaba.fastjson2.JSON.parse(json));
    }

    @RequiresPermissions("education:paper:preview")
    @GetMapping("/compose-template/list")
    public AjaxResult composeTemplateList(@RequestParam(required = false) Long subjectId)
    {
        return success(composeTemplateService.selectAvailableTemplates(subjectId, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:paper:preview")
    @GetMapping("/compose-template/{templateId}")
    public AjaxResult composeTemplateDetail(@PathVariable Long templateId)
    {
        return success(composeTemplateService.selectTemplateById(templateId));
    }

    @RequiresPermissions("education:paper:preview")
    @PostMapping("/compose-template/save")
    public AjaxResult composeTemplateSave(@RequestBody EduQbComposeTemplate template)
    {
        return success(composeTemplateService.saveUserTemplate(template, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:paper:preview")
    @DeleteMapping("/compose-template/{templateId}")
    public AjaxResult composeTemplateDelete(@PathVariable Long templateId)
    {
        return toAjax(composeTemplateService.deleteUserTemplate(templateId, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:paper:preview")
    @PostMapping("/smart-compose")
    public AjaxResult smartCompose(@RequestBody EduQbSmartComposeRequest request)
    {
        return success(paperService.smartCompose(request));
    }

    @RequiresPermissions("education:paper:preview")
    @PostMapping("/publish-school")
    public AjaxResult publishSchool(@RequestBody EduQbSchoolPaperPublishRequest request)
    {
        Long paperId = examPaperService.publishSchoolExamPaper(request, SecurityUtils.getUsername());
        return success(paperId);
    }
}
