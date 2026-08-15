package com.ruoyi.system.controller.education;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.security.utils.SecurityUtils;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.system.domain.education.EduQbImportCommitBody;
import com.ruoyi.system.domain.education.EduQbImportTask;
import com.ruoyi.system.service.education.IEduQbImportService;
import com.ruoyi.system.service.education.support.EduQbChapterMatchService;

@RestController
@RequestMapping("/education/question/import")
public class EduQbImportController extends BaseController
{
    @Autowired
    private IEduQbImportService importService;

    @Autowired
    private EduQbChapterMatchService chapterMatchService;

    @RequiresPermissions("education:question:import")
    @PostMapping("/docx")
    public AjaxResult uploadDocx(@RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long subjectId)
    {
        return success(importService.uploadAndParse(file, subjectId, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:question:import")
    @GetMapping("/tasks")
    public TableDataInfo listTasks(EduQbImportTask query)
    {
        startPage();
        return getDataTable(importService.selectImportTaskList(query, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:question:import")
    @GetMapping("/task/{taskId}")
    public AjaxResult getTask(@PathVariable Long taskId)
    {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("task", importService.getTask(taskId, SecurityUtils.getUsername()));
        data.put("blocks", importService.getBlocks(taskId, SecurityUtils.getUsername()));
        data.put("previewHtml", importService.getPreviewHtml(taskId, SecurityUtils.getUsername()));
        return success(data);
    }

    @RequiresPermissions("education:question:import")
    @PostMapping("/match-chapters")
    public AjaxResult matchChapters(@RequestBody java.util.Map<String, Object> body)
    {
        Long textbookId = body.get("textbookId") != null ? Long.valueOf(String.valueOf(body.get("textbookId"))) : null;
        @SuppressWarnings("unchecked")
        List<String> hints = body.get("hints") instanceof List ? (List<String>) body.get("hints") : List.of();
        return success(chapterMatchService.matchChapters(textbookId, hints));
    }

    @RequiresPermissions("education:question:import")
    @PostMapping("/commit")
    public AjaxResult commit(@RequestBody EduQbImportCommitBody body)
    {
        int count = importService.commitImport(body, SecurityUtils.getUsername());
        return success(count);
    }
}
