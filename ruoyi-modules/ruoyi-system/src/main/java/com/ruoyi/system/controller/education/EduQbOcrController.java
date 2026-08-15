package com.ruoyi.system.controller.education;

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
import java.util.Map;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.system.domain.education.EduQbOcrCommitBody;
import com.ruoyi.system.domain.education.EduQbOcrDraft;
import com.ruoyi.system.service.education.IEduQbOcrService;

@RestController
@RequestMapping("/education/question/ocr")
public class EduQbOcrController extends BaseController
{
    @Autowired
    private IEduQbOcrService ocrService;

    @RequiresPermissions("education:question:import")
    @PostMapping("/recognize")
    public AjaxResult recognize(@RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long subjectId)
    {
        return success(ocrService.recognize(file, subjectId, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:question:import")
    @GetMapping("/drafts")
    public TableDataInfo listDrafts(EduQbOcrDraft query)
    {
        startPage();
        return getDataTable(ocrService.selectOcrDraftList(query, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:question:import")
    @GetMapping("/draft/{draftId}")
    public AjaxResult getDraft(@PathVariable Long draftId)
    {
        return success(ocrService.getDraftDetail(draftId, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:question:import")
    @PostMapping("/commit")
    public AjaxResult commit(@RequestBody EduQbOcrCommitBody body)
    {
        return success(ocrService.commit(body, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:question:import")
    @PostMapping("/draft/{draftId}/figure")
    public AjaxResult saveDraftFigure(@PathVariable Long draftId, @RequestBody Map<String, String> body)
    {
        ocrService.saveDraftFigure(draftId, body != null ? body.get("figurePath") : null, SecurityUtils.getUsername());
        return success();
    }
}
