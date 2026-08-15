package com.ruoyi.system.controller.education;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.system.domain.education.EduQbQuestion;
import com.ruoyi.system.domain.education.EduQbQuestionAuditBody;
import com.ruoyi.system.domain.education.EduQbDuplicateCheckBody;
import com.ruoyi.system.domain.education.EduQbDuplicateCheckResult;
import com.ruoyi.system.domain.education.EduQbQuestionFeedbackBody;
import com.ruoyi.system.service.education.IEduQbQuestionService;

@RestController
@RequestMapping("/education/question")
public class EduQbQuestionController extends BaseController
{
    @Autowired
    private IEduQbQuestionService questionService;

    @GetMapping("/list")
    public TableDataInfo list(EduQbQuestion question)
    {
        startPage();
        List<EduQbQuestion> list = questionService.selectEduQbQuestionList(question);
        return getDataTable(list);
    }

    @GetMapping("/{questionId}")
    public AjaxResult getInfo(@PathVariable Long questionId)
    {
        EduQbQuestion question = questionService.selectEduQbQuestionById(questionId);
        if (question == null)
        {
            return error("\u8bd5\u9898\u4e0d\u5b58\u5728");
        }
        String username = SecurityUtils.getUsername();
        question.getParams().put("canManage", StringUtils.isNotEmpty(username)
            && questionService.canManage(question, username));
        return success(question);
    }

    @RequiresPermissions("education:question:add")
    @PostMapping
    public AjaxResult add(@RequestBody EduQbQuestion question)
    {
        return toAjax(questionService.insertEduQbQuestion(question, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:question:edit")
    @PutMapping
    public AjaxResult edit(@RequestBody EduQbQuestion question)
    {
        return toAjax(questionService.updateEduQbQuestion(question, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:question:remove")
    @DeleteMapping("/{questionIds}")
    public AjaxResult remove(@PathVariable Long[] questionIds)
    {
        return toAjax(questionService.deleteEduQbQuestionByIds(questionIds, SecurityUtils.getUsername()));
    }

    @GetMapping("/knowledge/tags")
    public AjaxResult knowledgeTags(@RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String keyword)
    {
        return success(questionService.selectKnowledgeTags(subjectId, keyword));
    }

    @GetMapping("/knowledge/tree")
    public AjaxResult knowledgeTree(@RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long textbookId,
            @RequestParam(required = false) String keyword)
    {
        return success(questionService.selectKnowledgeTree(textbookId, subjectId, keyword));
    }

    /**
     * @deprecated Use {@code GET /education/textbook/chapter/tree} with textbookId instead.
     *             This endpoint builds a tree from free-text chapter_text stats (legacy data only).
     */
    @Deprecated
    @GetMapping("/chapter/tree")
    public AjaxResult chapterTree(@RequestParam(required = false) Long subjectId)
    {
        return success(questionService.selectChapterTree(subjectId));
    }

    @RequiresPermissions("education:question:audit")
    @PostMapping("/audit")
    public AjaxResult audit(@RequestBody EduQbQuestionAuditBody body)
    {
        return toAjax(questionService.auditQuestions(body, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:question:audit")
    @GetMapping("/audit/pending-count")
    public AjaxResult pendingCount()
    {
        return success(questionService.countPendingQuestions());
    }

    @RequiresPermissions("education:question:query")
    @PostMapping("/duplicate/check")
    public AjaxResult checkDuplicate(@RequestBody EduQbDuplicateCheckBody body)
    {
        return success(questionService.checkDuplicates(body));
    }

    @RequiresPermissions("education:question:query")
    @GetMapping("/{questionId}/duplicates")
    public AjaxResult listDuplicates(@PathVariable Long questionId)
    {
        EduQbQuestion question = questionService.selectEduQbQuestionById(questionId);
        if (question == null)
        {
            return success(new EduQbDuplicateCheckResult());
        }
        EduQbDuplicateCheckBody body = new EduQbDuplicateCheckBody();
        body.setSubjectId(question.getSubjectId());
        body.setContent(question.getContent());
        body.setQuestionId(questionId);
        return success(questionService.checkDuplicates(body));
    }

    @RequiresPermissions("education:question:query")
    @PostMapping("/feedback")
    public AjaxResult submitFeedback(@RequestBody EduQbQuestionFeedbackBody body)
    {
        return toAjax(questionService.submitQuestionFeedback(body, SecurityUtils.getUsername()));
    }
}
