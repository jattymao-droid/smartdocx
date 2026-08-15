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
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.system.domain.education.EduQbQuestionType;
import com.ruoyi.system.service.education.IEduQbQuestionTypeService;

@RestController
@RequestMapping("/education/question/type")
public class EduQbQuestionTypeController extends BaseController
{
    @Autowired
    private IEduQbQuestionTypeService questionTypeService;

    @RequiresPermissions("education:question:list")
    @GetMapping("/list")
    public TableDataInfo list(EduQbQuestionType query)
    {
        startPage();
        List<EduQbQuestionType> list = questionTypeService.selectEduQbQuestionTypeList(query);
        return getDataTable(list);
    }

    @GetMapping("/options")
    public AjaxResult options()
    {
        return success(questionTypeService.selectEnabledQuestionTypeOptions());
    }

    @RequiresPermissions("education:question:edit")
    @GetMapping("/{typeId}")
    public AjaxResult getInfo(@PathVariable Long typeId)
    {
        return success(questionTypeService.selectEduQbQuestionTypeById(typeId));
    }

    @RequiresPermissions("education:question:edit")
    @PostMapping
    public AjaxResult add(@RequestBody EduQbQuestionType row)
    {
        row.setCreateBy(SecurityUtils.getUsername());
        return toAjax(questionTypeService.insertEduQbQuestionType(row));
    }

    @RequiresPermissions("education:question:edit")
    @PutMapping
    public AjaxResult edit(@RequestBody EduQbQuestionType row)
    {
        row.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(questionTypeService.updateEduQbQuestionType(row));
    }

    @RequiresPermissions("education:question:edit")
    @DeleteMapping("/{typeIds}")
    public AjaxResult remove(@PathVariable Long[] typeIds)
    {
        return toAjax(questionTypeService.deleteEduQbQuestionTypeByIds(typeIds));
    }
}
