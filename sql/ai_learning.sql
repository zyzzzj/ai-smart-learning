-- ============================================================
-- 基于AI大模型的智能学习系统 数据库初始化脚本
-- MySQL 8.0 / utf8mb4
-- 用法: mysql --default-character-set=utf8mb4 -uroot -p < ai_learning.sql
-- 说明: 用户初始密码统一为 123456(明文),应用首次启动会自动升级为 BCrypt 密文
-- ============================================================

CREATE DATABASE IF NOT EXISTS ai_learning DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE ai_learning;

-- ------------------------------------------------------------
-- 系统用户(学生 / 管理员)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user
(
    id          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    varchar(50)  NOT NULL COMMENT '登录账号',
    password    varchar(100) NOT NULL COMMENT '密码(BCrypt)',
    nickname    varchar(50)           DEFAULT NULL COMMENT '昵称',
    avatar      varchar(500)          DEFAULT NULL COMMENT '头像地址',
    email       varchar(100)          DEFAULT NULL COMMENT '邮箱',
    phone       varchar(20)           DEFAULT NULL COMMENT '手机号',
    role        varchar(20)  NOT NULL DEFAULT 'STUDENT' COMMENT '角色 STUDENT/ADMIN',
    status      tinyint      NOT NULL DEFAULT 1 COMMENT '状态 1正常 0禁用',
    last_login  datetime              DEFAULT NULL COMMENT '最后登录时间',
    deleted     tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB COMMENT '系统用户';

-- ------------------------------------------------------------
-- 科目 / 课程
-- ------------------------------------------------------------
DROP TABLE IF EXISTS subject;
CREATE TABLE subject
(
    id          bigint       NOT NULL AUTO_INCREMENT,
    name        varchar(64)  NOT NULL COMMENT '科目名称',
    description varchar(255)          DEFAULT NULL COMMENT '简介',
    cover       varchar(500)          DEFAULT NULL COMMENT '封面图',
    sort        int          NOT NULL DEFAULT 0 COMMENT '排序',
    status      tinyint      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    deleted     tinyint      NOT NULL DEFAULT 0,
    create_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_subject_name (name)
) ENGINE = InnoDB COMMENT '科目/课程';

-- ------------------------------------------------------------
-- 知识点(树形,parent_id=0 为根)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS knowledge_point;
CREATE TABLE knowledge_point
(
    id          bigint       NOT NULL AUTO_INCREMENT,
    subject_id  bigint       NOT NULL COMMENT '所属科目',
    parent_id   bigint       NOT NULL DEFAULT 0 COMMENT '父知识点,0 为根',
    name        varchar(64)  NOT NULL COMMENT '知识点名称',
    description varchar(500)          DEFAULT NULL COMMENT '说明',
    difficulty  tinyint      NOT NULL DEFAULT 3 COMMENT '难度 1-5',
    sort        int          NOT NULL DEFAULT 0,
    status      tinyint      NOT NULL DEFAULT 1,
    deleted     tinyint      NOT NULL DEFAULT 0,
    create_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_kp_subject (subject_id),
    KEY idx_kp_parent (parent_id)
) ENGINE = InnoDB COMMENT '知识点';

-- ------------------------------------------------------------
-- 题目
-- ------------------------------------------------------------
DROP TABLE IF EXISTS question;
CREATE TABLE question
(
    id                 bigint         NOT NULL AUTO_INCREMENT,
    subject_id         bigint         NOT NULL COMMENT '所属科目',
    knowledge_point_id bigint                  DEFAULT NULL COMMENT '所属知识点',
    type               varchar(20)    NOT NULL COMMENT '题型 SINGLE/MULTIPLE/JUDGE/FILL/SUBJECTIVE',
    stem               text           NOT NULL COMMENT '题干',
    options            json                    DEFAULT NULL COMMENT '选项 [{"key":"A","content":"..."}]',
    answer             text                    DEFAULT NULL COMMENT '参考答案',
    analysis           text                    DEFAULT NULL COMMENT '解析',
    difficulty         tinyint        NOT NULL DEFAULT 3 COMMENT '难度 1-5',
    score              decimal(5, 1)  NOT NULL DEFAULT 5.0 COMMENT '建议分值',
    source             varchar(20)    NOT NULL DEFAULT 'MANUAL' COMMENT '来源 MANUAL/AI/IMPORT',
    tags               varchar(255)            DEFAULT NULL COMMENT '标签,逗号分隔',
    ai_task_id         bigint                  DEFAULT NULL COMMENT 'AI 生成任务 id',
    view_count         int            NOT NULL DEFAULT 0 COMMENT '被作答次数',
    correct_count      int            NOT NULL DEFAULT 0 COMMENT '答对次数',
    status             tinyint        NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    create_user        bigint                  DEFAULT NULL,
    deleted            tinyint        NOT NULL DEFAULT 0,
    create_time        datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_q_subject (subject_id),
    KEY idx_q_kp (knowledge_point_id),
    KEY idx_q_type (type),
    KEY idx_q_difficulty (difficulty),
    KEY idx_q_source (source)
) ENGINE = InnoDB COMMENT '题目';

-- ------------------------------------------------------------
-- AI 任务(出题 / 评分 / 学习建议)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS ai_task;
CREATE TABLE ai_task
(
    id                 bigint       NOT NULL AUTO_INCREMENT,
    type               varchar(30)  NOT NULL COMMENT 'GENERATE_QUESTION/GRADE_SUBJECTIVE/LEARNING_ADVICE',
    provider           varchar(20)  NOT NULL DEFAULT 'mock' COMMENT 'mock/ollama',
    model              varchar(64)           DEFAULT NULL COMMENT '模型名',
    subject_id         bigint                DEFAULT NULL,
    knowledge_point_id bigint                DEFAULT NULL,
    user_id            bigint                DEFAULT NULL COMMENT '发起人',
    request_params     json                  DEFAULT NULL COMMENT '请求参数',
    prompt             text                  DEFAULT NULL COMMENT '提示词',
    response           mediumtext            DEFAULT NULL COMMENT '模型原始返回',
    result             json                  DEFAULT NULL COMMENT '解析后的结果',
    status             varchar(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/FAILED',
    error              varchar(500)          DEFAULT NULL,
    cost_ms            bigint                DEFAULT NULL COMMENT '耗时毫秒',
    create_time        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finish_time        datetime              DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_ai_type (type),
    KEY idx_ai_status (status)
) ENGINE = InnoDB COMMENT 'AI 任务记录';

-- ------------------------------------------------------------
-- 刷题会话
-- ------------------------------------------------------------
DROP TABLE IF EXISTS practice_session;
CREATE TABLE practice_session
(
    id                 bigint       NOT NULL AUTO_INCREMENT,
    user_id            bigint       NOT NULL,
    subject_id         bigint                DEFAULT NULL,
    knowledge_point_id bigint                DEFAULT NULL,
    mode               varchar(20)  NOT NULL DEFAULT 'RANDOM' COMMENT 'RANDOM/KNOWLEDGE/WRONG/DIFFICULTY/AI',
    question_ids       json                  DEFAULT NULL COMMENT '本次抽题 id 列表(JSON)',
    question_count     int          NOT NULL DEFAULT 0,
    answered_count     int          NOT NULL DEFAULT 0,
    correct_count      int          NOT NULL DEFAULT 0,
    total_score        decimal(6, 1) NOT NULL DEFAULT 0,
    status             varchar(20)  NOT NULL DEFAULT 'ONGOING' COMMENT 'ONGOING/FINISHED',
    start_time         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finish_time        datetime              DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_ps_user (user_id)
) ENGINE = InnoDB COMMENT '刷题会话';

-- ------------------------------------------------------------
-- 刷题答题明细
-- ------------------------------------------------------------
DROP TABLE IF EXISTS practice_record;
CREATE TABLE practice_record
(
    id           bigint        NOT NULL AUTO_INCREMENT,
    session_id   bigint        NOT NULL,
    user_id      bigint        NOT NULL,
    question_id  bigint        NOT NULL,
    user_answer  text                   DEFAULT NULL,
    is_correct   tinyint                DEFAULT NULL COMMENT '1对 0错 NULL待判定(主观题)',
    score        decimal(5, 1) NOT NULL DEFAULT 0,
    ai_feedback  text                   DEFAULT NULL COMMENT 'AI 评分反馈',
    time_spent   int           NOT NULL DEFAULT 0 COMMENT '作答耗时(秒)',
    create_time  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_pr_session (session_id),
    KEY idx_pr_user (user_id),
    KEY idx_pr_question (question_id)
) ENGINE = InnoDB COMMENT '刷题记录';

-- ------------------------------------------------------------
-- 错题本
-- ------------------------------------------------------------
DROP TABLE IF EXISTS wrong_question;
CREATE TABLE wrong_question
(
    id              bigint       NOT NULL AUTO_INCREMENT,
    user_id         bigint       NOT NULL,
    question_id     bigint       NOT NULL,
    wrong_count     int          NOT NULL DEFAULT 1 COMMENT '累计答错次数',
    last_answer     text                  DEFAULT NULL COMMENT '最近一次错误答案',
    last_wrong_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mastered        tinyint      NOT NULL DEFAULT 0 COMMENT '1已掌握(连续答对后置位)',
    create_time     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_question (user_id, question_id),
    KEY idx_wq_user (user_id)
) ENGINE = InnoDB COMMENT '错题本';

-- ------------------------------------------------------------
-- 考试
-- ------------------------------------------------------------
DROP TABLE IF EXISTS exam;
CREATE TABLE exam
(
    id            bigint        NOT NULL AUTO_INCREMENT,
    name          varchar(100)  NOT NULL COMMENT '考试名称',
    subject_id    bigint        NOT NULL,
    description   varchar(500)           DEFAULT NULL,
    duration      int           NOT NULL DEFAULT 60 COMMENT '时长(分钟)',
    total_score   decimal(6, 1) NOT NULL DEFAULT 100,
    pass_score    decimal(6, 1) NOT NULL DEFAULT 60,
    question_count int          NOT NULL DEFAULT 0,
    start_time    datetime               DEFAULT NULL COMMENT '开考时间',
    end_time      datetime               DEFAULT NULL COMMENT '截止时间',
    status        varchar(20)   NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/CLOSED',
    create_user   bigint                 DEFAULT NULL,
    deleted       tinyint       NOT NULL DEFAULT 0,
    create_time   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_exam_subject (subject_id)
) ENGINE = InnoDB COMMENT '考试';

-- ------------------------------------------------------------
-- 试卷题目
-- ------------------------------------------------------------
DROP TABLE IF EXISTS exam_question;
CREATE TABLE exam_question
(
    id          bigint        NOT NULL AUTO_INCREMENT,
    exam_id     bigint        NOT NULL,
    question_id bigint        NOT NULL,
    score       decimal(5, 1) NOT NULL DEFAULT 5.0,
    sort        int           NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_exam_question (exam_id, question_id),
    KEY idx_eq_exam (exam_id)
) ENGINE = InnoDB COMMENT '试卷题目';

-- ------------------------------------------------------------
-- 考试记录
-- ------------------------------------------------------------
DROP TABLE IF EXISTS exam_record;
CREATE TABLE exam_record
(
    id               bigint        NOT NULL AUTO_INCREMENT,
    exam_id          bigint        NOT NULL,
    user_id          bigint        NOT NULL,
    start_time       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submit_time      datetime               DEFAULT NULL,
    objective_score  decimal(6, 1) NOT NULL DEFAULT 0 COMMENT '客观题得分',
    subjective_score decimal(6, 1) NOT NULL DEFAULT 0 COMMENT '主观题得分',
    total_score      decimal(6, 1) NOT NULL DEFAULT 0,
    correct_count    int           NOT NULL DEFAULT 0,
    status           varchar(20)   NOT NULL DEFAULT 'ONGOING' COMMENT 'ONGOING/SUBMITTED/GRADED',
    ai_comment       text                   DEFAULT NULL COMMENT 'AI 学习建议',
    create_time      datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_exam_user (exam_id, user_id),
    KEY idx_er_user (user_id)
) ENGINE = InnoDB COMMENT '考试记录';

-- ------------------------------------------------------------
-- 考试作答明细
-- ------------------------------------------------------------
DROP TABLE IF EXISTS exam_answer;
CREATE TABLE exam_answer
(
    id             bigint        NOT NULL AUTO_INCREMENT,
    exam_record_id bigint        NOT NULL,
    question_id    bigint        NOT NULL,
    user_answer    text                   DEFAULT NULL,
    is_correct     tinyint                DEFAULT NULL,
    score          decimal(5, 1) NOT NULL DEFAULT 0,
    ai_feedback    text                   DEFAULT NULL COMMENT '主观题 AI 评分理由',
    graded         tinyint       NOT NULL DEFAULT 0,
    create_time    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_record_question (exam_record_id, question_id),
    KEY idx_ea_record (exam_record_id)
) ENGINE = InnoDB COMMENT '考试作答明细';

-- ------------------------------------------------------------
-- 学习建议记录
-- ------------------------------------------------------------
DROP TABLE IF EXISTS learning_advice;
CREATE TABLE learning_advice
(
    id          bigint       NOT NULL AUTO_INCREMENT,
    user_id     bigint       NOT NULL,
    subject_id  bigint                DEFAULT NULL,
    content     text         NOT NULL COMMENT '建议内容(Markdown)',
    source      varchar(20)  NOT NULL DEFAULT 'AI' COMMENT 'AI/RULE',
    ai_task_id  bigint                DEFAULT NULL,
    create_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_la_user (user_id)
) ENGINE = InnoDB COMMENT 'AI 学习建议';

-- ------------------------------------------------------------
-- 学习资源文件(MinIO / 本地存储)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_file;
CREATE TABLE sys_file
(
    id            bigint       NOT NULL AUTO_INCREMENT,
    bucket        varchar(64)           DEFAULT NULL COMMENT '存储桶',
    object_key    varchar(500) NOT NULL COMMENT '对象键',
    original_name varchar(255)          DEFAULT NULL COMMENT '原始文件名',
    content_type  varchar(100)          DEFAULT NULL,
    size          bigint       NOT NULL DEFAULT 0,
    category      varchar(20)  NOT NULL DEFAULT 'OTHER' COMMENT 'VIDEO/IMAGE/DOC/OTHER',
    biz_type      varchar(30)           DEFAULT NULL COMMENT '业务类型 QUESTION/SUBJECT/KNOWLEDGE/AVATAR',
    biz_id        bigint                DEFAULT NULL COMMENT '业务 id',
    url           varchar(1000)         DEFAULT NULL COMMENT '访问地址',
    uploader_id   bigint                DEFAULT NULL,
    deleted       tinyint      NOT NULL DEFAULT 0,
    create_time   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_file_biz (biz_type, biz_id)
) ENGINE = InnoDB COMMENT '资源文件';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 用户:admin / 123456(管理员)、student / 123456(学生)
INSERT INTO sys_user (username, password, nickname, role, email, status)
VALUES ('admin', '123456', '系统管理员', 'ADMIN', 'admin@ailearn.local', 1),
       ('student', '123456', '小明同学', 'STUDENT', 'student@ailearn.local', 1),
       ('student2', '123456', '小红同学', 'STUDENT', 'student2@ailearn.local', 1);

-- 科目
INSERT INTO subject (id, name, description, sort, status)
VALUES (1, 'Java 程序设计', 'Java 基础语法、面向对象、集合与并发', 1, 1),
       (2, '数据结构与算法', '线性表、树、图、排序与查找', 2, 1),
       (3, '计算机网络', 'TCP/IP、HTTP、网络分层与协议', 3, 1);

-- 知识点(两级)
INSERT INTO knowledge_point (id, subject_id, parent_id, name, description, difficulty, sort)
VALUES (1, 1, 0, 'Java 基础', '变量、运算符、流程控制', 2, 1),
       (2, 1, 1, '数据类型与变量', '基本类型、引用类型、自动装箱', 2, 1),
       (3, 1, 1, '流程控制', 'if/switch/for/while', 2, 2),
       (4, 1, 0, '面向对象', '封装、继承、多态', 3, 2),
       (5, 1, 4, '继承与多态', '方法重写、动态绑定', 4, 1),
       (6, 1, 4, '接口与抽象类', '接口默认方法、抽象类区别', 4, 2),
       (7, 1, 0, '集合框架', 'List/Set/Map 与底层实现', 4, 3),
       (8, 2, 0, '线性表', '数组、链表、栈、队列', 3, 1),
       (9, 2, 0, '树与二叉树', '遍历、二叉搜索树、平衡树', 4, 2),
       (10, 3, 0, '传输层', 'TCP/UDP、三次握手、流量控制', 4, 1);

-- 题目:单选
INSERT INTO question (subject_id, knowledge_point_id, type, stem, options, answer, analysis, difficulty, score, source, tags)
VALUES (1, 2, 'SINGLE', '在 Java 中,以下哪个是基本数据类型?',
        '[{"key":"A","content":"String"},{"key":"B","content":"int"},{"key":"C","content":"Integer"},{"key":"D","content":"ArrayList"}]',
        'B', 'int 是 Java 的八种基本数据类型之一;String、Integer、ArrayList 都是引用类型。', 1, 5.0, 'MANUAL', '基础,数据类型'),
       (1, 5, 'SINGLE', '关于 Java 多态,下列说法正确的是?',
        '[{"key":"A","content":"父类引用指向子类对象,调用重写方法时执行子类实现"},{"key":"B","content":"多态依赖方法重载实现"},{"key":"C","content":"静态方法可以被重写"},{"key":"D","content":"多态会在编译期确定调用的具体实现"}]',
        'A', '多态的基础是继承 + 方法重写 + 父类引用指向子类对象,运行时动态绑定;静态方法属于类,不能被重写。', 3, 5.0, 'MANUAL', '面向对象,多态'),
       (1, 7, 'SINGLE', 'HashMap 在 JDK8 中,链表长度超过多少会转为红黑树?',
        '[{"key":"A","content":"6"},{"key":"B","content":"7"},{"key":"C","content":"8"},{"key":"D","content":"16"}]',
        'C', 'JDK8 中链表长度达到 8 且数组容量不小于 64 时转为红黑树。', 4, 5.0, 'MANUAL', '集合,HashMap'),
       (2, 9, 'SINGLE', '二叉搜索树的中序遍历结果是?',
        '[{"key":"A","content":"无序的"},{"key":"B","content":"升序排列"},{"key":"C","content":"降序排列"},{"key":"D","content":"与插入顺序相同"}]',
        'B', '二叉搜索树左子树小于根、右子树大于根,中序遍历得到递增序列。', 3, 5.0, 'MANUAL', '树,遍历'),
       (3, 10, 'SINGLE', 'TCP 三次握手中,客户端发送的第二个报文是?',
        '[{"key":"A","content":"SYN"},{"key":"B","content":"SYN + ACK"},{"key":"C","content":"ACK"},{"key":"D","content":"FIN"}]',
        'C', '三次握手:客户端 SYN → 服务端 SYN+ACK → 客户端 ACK。', 4, 5.0, 'MANUAL', 'TCP,握手');

-- 题目:多选 / 判断 / 填空 / 主观
INSERT INTO question (subject_id, knowledge_point_id, type, stem, options, answer, analysis, difficulty, score, source, tags)
VALUES (1, 6, 'MULTIPLE', '以下关于 Java 接口的说法,正确的有?',
        '[{"key":"A","content":"接口中的方法默认是 public abstract"},{"key":"B","content":"JDK8 之后接口可以有 default 方法"},{"key":"C","content":"接口可以定义实例字段"},{"key":"D","content":"一个类可以实现多个接口"}]',
        'A,B,D', '接口不能定义实例字段(只能是 public static final 常量),因此 C 错误。', 3, 6.0, 'MANUAL', '接口,JDK8'),
       (1, 3, 'JUDGE', 'switch 语句在 JDK7 之后可以作用于 String 类型。',
        NULL, '对', 'JDK7 起 switch 支持 String,底层通过 hashCode 与 equals 实现。', 2, 4.0, 'MANUAL', '流程控制'),
       (2, 8, 'FILL', '在最坏情况下,单链表按值查找的时间复杂度是 ______。',
        NULL, 'O(n)', '需要从头依次遍历,平均与最坏均为线性时间。', 2, 4.0, 'MANUAL', '链表,复杂度'),
       (1, 5, 'SUBJECTIVE', '请简述 Java 中重载(Overload)与重写(Override)的区别,并各举一个使用场景。',
        NULL, '重载:同一个类中方法名相同、参数列表不同,编译期确定,用于提供多种调用形式;重写:子类重新实现父类方法,方法签名相同,运行时动态绑定,用于多态扩展。',
        '答题要点:1) 定义与发生位置;2) 参数/返回值/访问权限要求;3) 绑定时机(编译期 vs 运行期);4) 各自典型场景。', 3, 10.0, 'MANUAL', '面向对象,重载,重写'),
       (3, 10, 'SUBJECTIVE', '请说明 TCP 为什么需要三次握手而不是两次,并简述四次挥手的过程。',
        NULL, '两次握手无法确认客户端的接收能力与历史连接请求,可能导致服务端建立无效连接;三次握手可同步双方初始序号并确认双向可达。四次挥手:FIN → ACK → FIN → ACK,因 TCP 全双工需双向各自关闭。',
        '答题要点:1) 防止历史失效连接;2) 同步初始序列号;3) 双向确认能力;4) 挥手四个报文与 TIME_WAIT 的作用。', 4, 10.0, 'MANUAL', 'TCP,握手,挥手');

-- 考试
INSERT INTO exam (id, name, subject_id, description, duration, total_score, pass_score, question_count, status, create_user)
VALUES (1, 'Java 基础阶段测评', 1, '覆盖 Java 基础、面向对象与集合框架', 60, 60, 36, 6, 'PUBLISHED', 1);

INSERT INTO exam_question (exam_id, question_id, score, sort)
VALUES (1, 1, 5.0, 1),
       (1, 2, 5.0, 2),
       (1, 3, 5.0, 3),
       (1, 6, 6.0, 4),
       (1, 7, 4.0, 5),
       (1, 10, 10.0, 6);
