package com.ailearn.common;

/**
 * 枚举常量集中定义。
 */
public final class Constants {

    private Constants() {
    }

    /** 角色 */
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_STUDENT = "STUDENT";

    /** 题型 */
    public static final String TYPE_SINGLE = "SINGLE";
    public static final String TYPE_MULTIPLE = "MULTIPLE";
    public static final String TYPE_JUDGE = "JUDGE";
    public static final String TYPE_FILL = "FILL";
    public static final String TYPE_SUBJECTIVE = "SUBJECTIVE";

    /** 题目来源 */
    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_AI = "AI";
    public static final String SOURCE_IMPORT = "IMPORT";

    /** AI 任务类型 */
    public static final String AI_GENERATE_QUESTION = "GENERATE_QUESTION";
    public static final String AI_GRADE_SUBJECTIVE = "GRADE_SUBJECTIVE";
    public static final String AI_LEARNING_ADVICE = "LEARNING_ADVICE";

    /** AI 任务状态 */
    public static final String AI_STATUS_PENDING = "PENDING";
    public static final String AI_STATUS_RUNNING = "RUNNING";
    public static final String AI_STATUS_SUCCESS = "SUCCESS";
    public static final String AI_STATUS_FAILED = "FAILED";

    /** 刷题模式 */
    public static final String PRACTICE_RANDOM = "RANDOM";
    public static final String PRACTICE_KNOWLEDGE = "KNOWLEDGE";
    public static final String PRACTICE_WRONG = "WRONG";
    public static final String PRACTICE_DIFFICULTY = "DIFFICULTY";

    /** 考试状态 */
    public static final String EXAM_DRAFT = "DRAFT";
    public static final String EXAM_PUBLISHED = "PUBLISHED";
    public static final String EXAM_CLOSED = "CLOSED";

    /** 考试记录状态 */
    public static final String RECORD_ONGOING = "ONGOING";
    public static final String RECORD_SUBMITTED = "SUBMITTED";
    public static final String RECORD_GRADED = "GRADED";

    /** 文件分类 */
    public static final String FILE_VIDEO = "VIDEO";
    public static final String FILE_IMAGE = "IMAGE";
    public static final String FILE_DOC = "DOC";
    public static final String FILE_OTHER = "OTHER";
}
