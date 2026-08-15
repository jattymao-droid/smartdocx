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
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.system.domain.education.EduSubject;
import com.ruoyi.system.service.education.IEduSubjectService;

@RestController
@RequestMapping("/education/subject")
public class EduSubjectController extends BaseController
{
    @Autowired
    private IEduSubjectService subjectService;

    @GetMapping("/list")
    public TableDataInfo list(EduSubject subject)
    {
        startPage();
        List<EduSubject> list = subjectService.selectEduSubjectList(subject);
        return getDataTable(list);
    }

    @GetMapping("/options")
    public AjaxResult options()
    {
        return success(subjectService.selectSubjectOptions());
    }

    @RequiresPermissions("education:subject:query")
    @GetMapping("/{subjectId}")
    public AjaxResult getInfo(@PathVariable Long subjectId)
    {
        return success(subjectService.selectEduSubjectById(subjectId));
    }

    @RequiresPermissions("education:subject:add")
    @PostMapping
    public AjaxResult add(@RequestBody EduSubject subject)
    {
        subject.setCreateBy(SecurityUtils.getUsername());
        int rows = subjectService.insertEduSubject(subject);
        if (rows > 0)
        {
            return success(subject.getSubjectId());
        }
        return toAjax(rows);
    }

    @RequiresPermissions("education:subject:edit")
    @PutMapping
    public AjaxResult edit(@RequestBody EduSubject subject)
    {
        subject.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(subjectService.updateEduSubject(subject));
    }

    @RequiresPermissions("education:subject:remove")
    @DeleteMapping("/{subjectIds}")
    public AjaxResult remove(@PathVariable Long[] subjectIds)
    {
        return toAjax(subjectService.deleteEduSubjectByIds(subjectIds));
    }
}
