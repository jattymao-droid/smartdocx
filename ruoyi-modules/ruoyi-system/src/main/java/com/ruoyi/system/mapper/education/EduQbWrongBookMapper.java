package com.ruoyi.system.mapper.education;



import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.ruoyi.system.domain.education.EduQbWrongBook;

import com.ruoyi.system.domain.education.EduQbWrongBookStats;



public interface EduQbWrongBookMapper

{

    List<EduQbWrongBook> selectWrongBookList(EduQbWrongBook query);



    EduQbWrongBook selectWrongBookByUserQuestion(@Param("userName") String userName,

            @Param("questionId") Long questionId);



    int insertWrongBook(EduQbWrongBook row);



    int upsertWrongBook(EduQbWrongBook row);



    int markMastered(@Param("wrongId") Long wrongId, @Param("userName") String userName);



    int restoreWrong(@Param("wrongId") Long wrongId, @Param("userName") String userName);



    int deleteWrongBook(@Param("wrongId") Long wrongId, @Param("userName") String userName);



    int batchMarkMastered(@Param("wrongIds") List<Long> wrongIds, @Param("userName") String userName);



    int batchDeleteWrongBook(@Param("wrongIds") List<Long> wrongIds, @Param("userName") String userName);



    List<Long> selectActiveWrongQuestionIds(@Param("userName") String userName,

            @Param("subjectId") Long subjectId);



    List<EduQbWrongBook> selectActiveWrongBooks(@Param("userName") String userName,

            @Param("subjectId") Long subjectId,

            @Param("questionIds") List<Long> questionIds,

            @Param("limit") Integer limit);



    EduQbWrongBook selectWrongBookByIdForUser(@Param("wrongId") Long wrongId, @Param("userName") String userName);

    EduQbWrongBookStats selectWrongBookStats(@Param("userName") String userName,

            @Param("subjectId") Long subjectId);

}

