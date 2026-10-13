-- ============================================================
-- 增量升级脚本:v1 -> v2(刷题会话增加题目清单)
-- 已有数据库执行本脚本即可,新装环境直接用 ai_learning.sql
-- ============================================================
USE ai_learning;

-- 刷题会话记录本次抽到的题目 id 列表(JSON 数组),用于校验作答归属
ALTER TABLE practice_session
    ADD COLUMN question_ids json DEFAULT NULL COMMENT '本次抽题 id 列表(JSON)' AFTER mode;
