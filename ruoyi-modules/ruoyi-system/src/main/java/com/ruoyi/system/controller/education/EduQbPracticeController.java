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
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.domain.education.EduQbPracticeSession;
import com.ruoyi.system.domain.education.EduQbPracticeSubmitBody;
import com.ruoyi.system.domain.education.EduQbStudentPracticeCheckBody;
import com.ruoyi.system.domain.education.EduQbWeakComposeRequest;
import com.ruoyi.system.domain.education.EduQbWrongBook;
import com.ruoyi.system.domain.education.EduQbWrongBookBatchBody;
import com.ruoyi.system.domain.education.EduQbWrongComposeRequest;
import com.ruoyi.system.service.education.IEduQbPracticeService;

@RestController
@RequestMapping("/education/practice")
public class EduQbPracticeController extends BaseController
{
    @Autowired
    private IEduQbPracticeService practiceService;

    @RequiresPermissions("education:paper:preview")
    @PostMapping("/check")
    public AjaxResult check(@RequestBody EduQbStudentPracticeCheckBody body)
    {
        return success(practiceService.checkPracticeAnswer(body));
    }

    @RequiresPermissions("education:paper:preview")
    @PostMapping("/submit")
    public AjaxResult submit(@RequestBody EduQbPracticeSubmitBody body)
    {
        Long sessionId = practiceService.submitPractice(body, SecurityUtils.getUsername());
        return success(sessionId);
    }

    @RequiresPermissions("education:paper:preview")
    @GetMapping("/session/list")
    public TableDataInfo sessionList(EduQbPracticeSession query)
    {
        startPage();
        List<EduQbPracticeSession> list = practiceService.selectPracticeSessionList(query, SecurityUtils.getUsername());
        return getDataTable(list);
    }

    @RequiresPermissions("education:paper:preview")
    @GetMapping("/stats")
    public AjaxResult practiceStats(@RequestParam(required = false) Long subjectId)
    {
        return success(practiceService.selectPracticeStats(subjectId, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:paper:preview")
    @GetMapping("/session/{sessionId}")
    public AjaxResult sessionDetail(@PathVariable Long sessionId)
    {
        return success(practiceService.getPracticeSessionDetail(sessionId, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:paper:preview")
    @GetMapping("/wrong-book/stats")
    public AjaxResult wrongBookStats(@RequestParam(required = false) Long subjectId)
    {
        return success(practiceService.selectWrongBookStats(subjectId, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:paper:preview")
    @GetMapping("/wrong-book/list")
    public TableDataInfo wrongBookList(EduQbWrongBook query)
    {
        startPage();
        List<EduQbWrongBook> list = practiceService.selectWrongBookList(query, SecurityUtils.getUsername());
        return getDataTable(list);
    }

    @RequiresPermissions("education:paper:preview")
    @PutMapping("/wrong-book/{wrongId}/master")
    public AjaxResult markMastered(@PathVariable Long wrongId)
    {
        return toAjax(practiceService.markWrongMastered(wrongId, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:paper:preview")
    @PutMapping("/wrong-book/{wrongId}/restore")
    public AjaxResult restoreWrong(@PathVariable Long wrongId)
    {
        return toAjax(practiceService.restoreWrong(wrongId, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:paper:preview")
    @PutMapping("/wrong-book/batch/master")
    public AjaxResult batchMarkMastered(@RequestBody EduQbWrongBookBatchBody body)
    {
        return toAjax(practiceService.batchMarkWrongMastered(body, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:paper:preview")
    @DeleteMapping("/wrong-book/{wrongId}")
    public AjaxResult deleteWrong(@PathVariable Long wrongId)
    {
        return toAjax(practiceService.deleteWrongBook(wrongId, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:paper:preview")
    @DeleteMapping("/wrong-book/batch")
    public AjaxResult batchDeleteWrong(@RequestBody EduQbWrongBookBatchBody body)
    {
        return toAjax(practiceService.batchDeleteWrongBook(body, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:paper:preview")
    @GetMapping("/weak-points")
    public AjaxResult weakPoints(@RequestParam Long subjectId,
            @RequestParam(required = false, defaultValue = "5") Integer limit)
    {
        return success(practiceService.selectWeakPointStats(subjectId, limit, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:paper:preview")
    @PostMapping("/weak-compose")
    public AjaxResult weakCompose(@RequestBody EduQbWeakComposeRequest request)
    {
        return success(practiceService.weakCompose(request, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:paper:preview")
    @PostMapping("/wrong-compose")
    public AjaxResult wrongCompose(@RequestBody EduQbWrongComposeRequest request)
    {
        return success(practiceService.wrongCompose(request, SecurityUtils.getUsername()));
    }
}
