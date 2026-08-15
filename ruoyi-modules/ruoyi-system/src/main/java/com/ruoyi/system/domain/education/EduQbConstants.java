package com.ruoyi.system.domain.education;

public final class EduQbConstants
{
    private EduQbConstants()
    {
    }

    public static final int MAX_QUESTION_IMAGES = 5;

    public static final String TYPE_SINGLE = "single";
    public static final String TYPE_MULTI = "multi";
    public static final String TYPE_JUDGE = "judge";
    public static final String TYPE_FILL = "fill";
    public static final String TYPE_SHORT = "short";
    public static final String TYPE_EXPERIMENT = "experiment";
    public static final String TYPE_ANSWER = "answer";
    public static final String TYPE_COMPREHENSIVE = "comprehensive";
    public static final String TYPE_READING = "reading";
    public static final String TYPE_DRAWING = "drawing";
    public static final String TYPE_KNOWLEDGE_FILL = "knowledge_fill";

    public static boolean isSubjectiveType(String type)
    {
        return TYPE_SHORT.equals(type) || TYPE_EXPERIMENT.equals(type) || TYPE_ANSWER.equals(type)
                || TYPE_COMPREHENSIVE.equals(type) || TYPE_READING.equals(type) || TYPE_DRAWING.equals(type)
                || TYPE_KNOWLEDGE_FILL.equals(type);
    }

    public static final String SOURCE_MANUAL = "manual";
    public static final String SOURCE_DOCX = "docx";
    public static final String SOURCE_OCR = "ocr";
    public static final String SOURCE_EXAM = "exam_paper";

    public static final String PAPER_TYPE_USER = "user";
    public static final String PAPER_TYPE_EXAM = "exam";

    public static final String PUBLISH_PUBLISHED = "0";
    public static final String PUBLISH_DRAFT = "1";

    public static final String STATUS_APPROVED = "0";
    public static final String STATUS_PENDING = "1";
    public static final String STATUS_REJECTED = "2";

    public static final String DEL_NORMAL = "0";
    public static final String DEL_DELETED = "2";

    public static final String SORT_TYPE_DIFF = "TYPE_THEN_DIFFICULTY";
    public static final String SORT_DIFFICULTY = "DIFFICULTY";
    public static final String SORT_BASKET_ORDER = "BASKET_ORDER";
    public static final String EXPORT_STUDENT = "student";
    public static final String EXPORT_TEACHER = "teacher";
    public static final String TEMPLATE_A4_1COL = "A4_1COL";
    public static final String TEMPLATE_A3_1COL = "A3_1COL";

    /** Default stem length cap for most question types. */
    public static final int MAX_CONTENT_LEN = 2000;

    /** Reading / long-passage stems (e.g. Chinese reading comprehension). */
    public static final int MAX_CONTENT_LEN_READING = 20000;

    /** HTML stems imported from external sources (tables, inline images). */
    public static final int MAX_CONTENT_LEN_HTML = 20000;

    public static int resolveMaxContentLength(String questionType)
    {
        if (TYPE_READING.equals(questionType))
        {
            return MAX_CONTENT_LEN_READING;
        }
        return MAX_CONTENT_LEN;
    }
}
