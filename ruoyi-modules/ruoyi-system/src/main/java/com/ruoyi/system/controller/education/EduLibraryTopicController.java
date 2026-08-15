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
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.domain.education.EduLibraryTopic;
import com.ruoyi.system.service.education.IEduLibraryTopicService;

@RestController
@RequestMapping("/education/library/topic")
public class EduLibraryTopicController extends BaseController
{
    @Autowired
    private IEduLibraryTopicService topicService;

    @GetMapping("/list")
    public TableDataInfo list(EduLibraryTopic query,
            @RequestParam(value = "portal", defaultValue = "false") boolean portal)
    {
        startPage();
        List<EduLibraryTopic> list = topicService.selectEduLibraryTopicList(query, portal, safeUsername());
        return getDataTable(list);
    }

    @RequiresPermissions("education:library:topic")
    @GetMapping("/admin/list")
    public TableDataInfo adminList(EduLibraryTopic query)
    {
        startPage();
        List<EduLibraryTopic> list = topicService.selectEduLibraryTopicList(query, false, SecurityUtils.getUsername());
        return getDataTable(list);
    }

    @GetMapping("/{topicId}")
    public AjaxResult getInfo(@PathVariable Long topicId,
            @RequestParam(value = "portal", defaultValue = "false") boolean portal)
    {
        EduLibraryTopic topic = topicService.selectEduLibraryTopicById(topicId, portal, safeUsername());
        if (topic == null)
        {
            return error("\u4e13\u9898\u4e0d\u5b58\u5728");
        }
        return success(topic);
    }

    @RequiresPermissions("education:library:topic:query")
    @GetMapping("/admin/{topicId}")
    public AjaxResult adminGetInfo(@PathVariable Long topicId)
    {
        EduLibraryTopic topic = topicService.selectEduLibraryTopicById(topicId, false, SecurityUtils.getUsername());
        if (topic == null)
        {
            return error("\u4e13\u9898\u4e0d\u5b58\u5728");
        }
        if (topic.getDocuments() != null)
        {
            Long[] ids = topic.getDocuments().stream()
                    .map(d -> d.getDocumentId())
                    .toArray(Long[]::new);
            topic.setDocumentIds(ids);
        }
        return success(topic);
    }

    @RequiresPermissions("education:library:topic:add")
    @PostMapping
    public AjaxResult add(@RequestBody EduLibraryTopic topic)
    {
        return toAjax(topicService.insertEduLibraryTopic(topic, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:library:topic:edit")
    @PutMapping
    public AjaxResult edit(@RequestBody EduLibraryTopic topic)
    {
        return toAjax(topicService.updateEduLibraryTopic(topic, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:library:topic:remove")
    @DeleteMapping("/{topicIds}")
    public AjaxResult remove(@PathVariable Long[] topicIds)
    {
        return toAjax(topicService.deleteEduLibraryTopicByIds(topicIds));
    }

    @GetMapping("/{topicId}/download")
    public void download(@PathVariable Long topicId, HttpServletResponse response)
    {
        topicService.streamTopicZip(topicId, SecurityUtils.getUsername(), response);
    }

    private String safeUsername()
    {
        try
        {
            String username = SecurityUtils.getUsername();
            return StringUtils.isEmpty(username) ? null : username;
        }
        catch (Exception ex)
        {
            return null;
        }
    }
}
