package com.ruoyi.system.controller.education;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.domain.education.EduQbExamPaperCommitRequest;
import com.ruoyi.system.domain.education.EduQbImportBlock;
import com.ruoyi.system.domain.education.EduQbPaper;
import com.ruoyi.system.service.education.IEduQbExamPaperService;

@RestController
@RequestMapping("/education/exam-paper")
public class EduQbExamPaperController extends BaseController
{
    @Autowired
    private IEduQbExamPaperService examPaperService;

    @RequiresPermissions("education:exam-paper:list")
    @GetMapping("/list")
    public TableDataInfo list(EduQbPaper query)
    {
        startPage();
        List<EduQbPaper> list = examPaperService.selectExamPaperList(query);
        return getDataTable(list);
    }

    @RequiresPermissions("education:exam-paper:query")
    @GetMapping("/{paperId}")
    public AjaxResult getInfo(@PathVariable Long paperId)
    {
        return success(examPaperService.selectExamPaperDetail(paperId, false));
    }

    @RequiresPermissions("education:exam-paper:add")
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "subjectId", required = false) Long subjectId)
    {
        return success(examPaperService.uploadAndParse(file, subjectId, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:exam-paper:add")
    @PostMapping("/analyze")
    public AjaxResult analyze(@RequestBody AnalyzeBody body)
    {
        return success(examPaperService.analyzeBlocks(body.getBlocks(), body.getSubjectId()));
    }

    @RequiresPermissions("education:exam-paper:add")
    @PostMapping("/commit")
    public AjaxResult commit(@RequestBody EduQbExamPaperCommitRequest request)
    {
        Long paperId = examPaperService.commitExamPaper(request, SecurityUtils.getUsername());
        return success(paperId);
    }

    @RequiresPermissions("education:exam-paper:edit")
    @PutMapping("/{paperId}/publish")
    public AjaxResult publish(@PathVariable Long paperId, @RequestParam("status") String status)
    {
        return toAjax(examPaperService.updatePublishStatus(paperId, status));
    }

    @RequiresPermissions("education:exam-paper:remove")
    @DeleteMapping("/{paperId}")
    public AjaxResult remove(@PathVariable Long paperId)
    {
        return toAjax(examPaperService.deleteExamPaper(paperId));
    }

    public static class AnalyzeBody
    {
        private Long subjectId;
        private List<EduQbImportBlock> blocks;

        public Long getSubjectId()
        {
            return subjectId;
        }

        public void setSubjectId(Long subjectId)
        {
            this.subjectId = subjectId;
        }

        public List<EduQbImportBlock> getBlocks()
        {
            return blocks;
        }

        public void setBlocks(List<EduQbImportBlock> blocks)
        {
            this.blocks = blocks;
        }
    }
}
