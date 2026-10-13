package com.ailearn.common;

/**
 * 缓存名称与键前缀常量。
 */
public final class CacheNames {

    private CacheNames() {
    }

    /** 热门题目(按被作答次数排序) */
    public static final String HOT_QUESTIONS = "hotQuestions";
    /** 题目详情 */
    public static final String QUESTION_DETAIL = "questionDetail";
    /** 科目列表 */
    public static final String SUBJECT_LIST = "subjectList";
    /** 知识点树 */
    public static final String KNOWLEDGE_TREE = "knowledgeTree";
    /** 考试详情(含试卷题目) */
    public static final String EXAM_DETAIL = "examDetail";
    /** 学习统计概览 */
    public static final String USER_STATS = "userStats";

    /** problem 计数用 Redis key 前缀(热门题目 ZSet) */
    public static final String HOT_QUESTION_ZSET = "ailearn:hot:question";
    /** 题目浏览量缓存(Hash,异步落库) */
    public static final String QUESTION_VIEW_HASH = "ailearn:question:views";
}
