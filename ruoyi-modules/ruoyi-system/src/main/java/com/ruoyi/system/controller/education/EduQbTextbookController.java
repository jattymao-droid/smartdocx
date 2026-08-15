package com.ruoyi.system.controller.education;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import com.ruoyi.common.security.annotation.Logical;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.system.domain.education.EduQbCatalogChapter;
import com.ruoyi.system.domain.education.EduQbTextbook;
import com.ruoyi.system.domain.education.EduQbTextbookVersion;
import com.ruoyi.system.service.education.IEduQbTextbookService;

@RestController
@RequestMapping("/education/textbook")
public class EduQbTextbookController extends BaseController
{
    @Autowired
    private IEduQbTextbookService textbookService;

    @GetMapping("/versions")
    public AjaxResult versions(@RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String schoolStage)
    {
        List<EduQbTextbookVersion> list = textbookService.selectVersions(subjectId, schoolStage);
        return success(list);
    }

    @RequiresPermissions("education:textbook:list")
    @GetMapping("/versions/admin")
    public AjaxResult versionsAdmin(@RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String schoolStage)
    {
        List<EduQbTextbookVersion> list = textbookService.selectVersionsAdmin(subjectId, schoolStage);
        return success(list);
    }

    @RequiresPermissions("education:textbook:query")
    @GetMapping("/version/{versionId}")
    public AjaxResult getVersion(@PathVariable Long versionId)
    {
        return success(textbookService.selectVersionById(versionId));
    }

    @RequiresPermissions("education:textbook:add")
    @PostMapping("/version")
    public AjaxResult addVersion(@RequestBody EduQbTextbookVersion version)
    {
        int rows = textbookService.insertVersion(version);
        if (rows > 0)
        {
            return success(version.getVersionId());
        }
        return toAjax(rows);
    }

    @RequiresPermissions("education:textbook:edit")
    @PutMapping("/version")
    public AjaxResult editVersion(@RequestBody EduQbTextbookVersion version)
    {
        return toAjax(textbookService.updateVersion(version));
    }

    @RequiresPermissions("education:textbook:remove")
    @DeleteMapping("/version/{versionIds}")
    public AjaxResult removeVersion(@PathVariable Long[] versionIds)
    {
        return toAjax(textbookService.deleteVersionByIds(versionIds));
    }

    @GetMapping("/list")
    public AjaxResult textbooks(@RequestParam Long versionId)
    {
        List<EduQbTextbook> list = textbookService.selectTextbooks(versionId);
        return success(list);
    }

    @RequiresPermissions("education:textbook:list")
    @GetMapping("/list/admin")
    public AjaxResult textbooksAdmin(@RequestParam Long versionId)
    {
        List<EduQbTextbook> list = textbookService.selectTextbooksAdmin(versionId);
        return success(list);
    }

    @RequiresPermissions("education:textbook:query")
    @GetMapping("/{textbookId}")
    public AjaxResult getTextbook(@PathVariable Long textbookId)
    {
        return success(textbookService.selectTextbookById(textbookId));
    }

    @RequiresPermissions("education:textbook:add")
    @PostMapping
    public AjaxResult addTextbook(@RequestBody EduQbTextbook textbook)
    {
        int rows = textbookService.insertTextbook(textbook);
        if (rows > 0)
        {
            return success(textbook.getTextbookId());
        }
        return toAjax(rows);
    }

    @RequiresPermissions("education:textbook:edit")
    @PutMapping
    public AjaxResult editTextbook(@RequestBody EduQbTextbook textbook)
    {
        return toAjax(textbookService.updateTextbook(textbook));
    }

    @RequiresPermissions("education:textbook:remove")
    @DeleteMapping("/{textbookIds}")
    public AjaxResult removeTextbook(@PathVariable Long[] textbookIds)
    {
        return toAjax(textbookService.deleteTextbookByIds(textbookIds));
    }

    @GetMapping("/chapter/tree")
    public AjaxResult chapterTree(@RequestParam Long textbookId,
            @RequestParam(required = false) Long subjectId)
    {
        return success(textbookService.selectChapterTree(textbookId, subjectId));
    }

    @RequiresPermissions("education:textbook:list")
    @GetMapping("/chapter/list")
    public AjaxResult chapterList(@RequestParam Long textbookId)
    {
        return success(textbookService.selectChapterList(textbookId));
    }

    @RequiresPermissions("education:textbook:query")
    @GetMapping("/chapter/{chapterId}")
    public AjaxResult getChapter(@PathVariable Long chapterId)
    {
        return success(textbookService.selectChapterById(chapterId));
    }

    @RequiresPermissions("education:textbook:add")
    @PostMapping("/chapter")
    public AjaxResult addChapter(@RequestBody EduQbCatalogChapter chapter)
    {
        int rows = textbookService.insertChapter(chapter);
        if (rows > 0)
        {
            return success(chapter.getChapterId());
        }
        return toAjax(rows);
    }

    @RequiresPermissions("education:textbook:edit")
    @PutMapping("/chapter")
    public AjaxResult editChapter(@RequestBody EduQbCatalogChapter chapter)
    {
        return toAjax(textbookService.updateChapter(chapter));
    }

    @RequiresPermissions("education:textbook:remove")
    @DeleteMapping("/chapter/{chapterIds}")
    public AjaxResult removeChapter(@PathVariable Long[] chapterIds)
    {
        return toAjax(textbookService.deleteChapterByIds(chapterIds));
    }
}
