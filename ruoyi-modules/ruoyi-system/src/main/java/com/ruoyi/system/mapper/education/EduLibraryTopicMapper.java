package com.ruoyi.system.mapper.education;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.education.EduLibraryDocument;
import com.ruoyi.system.domain.education.EduLibraryTopic;

public interface EduLibraryTopicMapper
{
    EduLibraryTopic selectEduLibraryTopicById(Long topicId);

    List<EduLibraryTopic> selectEduLibraryTopicList(EduLibraryTopic query);

    int insertEduLibraryTopic(EduLibraryTopic topic);

    int updateEduLibraryTopic(EduLibraryTopic topic);

    int deleteEduLibraryTopicByIds(Long[] topicIds);

    int deleteTopicDocumentsByTopicId(Long topicId);

    int batchInsertTopicDocuments(@Param("topicId") Long topicId, @Param("documentIds") Long[] documentIds);

    List<EduLibraryDocument> selectTopicDocuments(@Param("topicId") Long topicId, @Param("portal") boolean portal);

    int incrementDownloadCount(Long topicId);
}
