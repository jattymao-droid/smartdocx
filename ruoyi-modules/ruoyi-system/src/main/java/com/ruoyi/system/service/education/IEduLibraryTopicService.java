package com.ruoyi.system.service.education;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.domain.education.EduLibraryTopic;

public interface IEduLibraryTopicService
{
    EduLibraryTopic selectEduLibraryTopicById(Long topicId, boolean portal, String viewer);

    List<EduLibraryTopic> selectEduLibraryTopicList(EduLibraryTopic query, boolean portal, String viewer);

    int insertEduLibraryTopic(EduLibraryTopic topic, String operator);

    int updateEduLibraryTopic(EduLibraryTopic topic, String operator);

    int deleteEduLibraryTopicByIds(Long[] topicIds);

    void streamTopicZip(Long topicId, String viewer, HttpServletResponse response);
}
