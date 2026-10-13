package com.ailearn.service;

import com.ailearn.common.PageResult;
import com.ailearn.dto.ExamAutoComposeDTO;
import com.ailearn.dto.ExamDTO;
import com.ailearn.dto.ExamSubmitDTO;
import com.ailearn.entity.Exam;
import com.ailearn.vo.ExamPaperVO;
import com.ailearn.vo.ExamResultVO;
import com.ailearn.vo.ExamVO;
import com.ailearn.vo.ScoreRankVO;

import java.util.List;

/**
 * 智能考试。
 */
public interface ExamService {

    /* ---------------- 管理端 ---------------- */

    /** 考试列表(管理端看全部,学生端看已发布) */
    PageResult<ExamVO> page(String keyword, String status, long page, long size, Long userId, boolean admin);

    /** 手动组卷 */
    Exam create(ExamDTO dto, Long userId);

    /** AI 自动组卷(按题型/难度配比抽题) */
    Exam autoCompose(ExamAutoComposeDTO dto, Long userId);

    /** 修改考试(仅草稿可改) */
    Exam update(ExamDTO dto);

    /** 发布 / 关闭 */
    void changeStatus(Long examId, String status);

    void remove(Long examId);

    /** 成绩排行 */
    List<ScoreRankVO> rank(Long examId, int limit);

    /* ---------------- 学生端 ---------------- */

    /** 开始考试(首次创建记录,已开始则续考) */
    ExamPaperVO start(Long examId, Long userId);

    /** 暂存单题作答(随时可保存,便于超时自动交卷) */
    void saveAnswer(Long examId, Long questionId, String userAnswer, Long userId);

    /** 交卷并判分(客观题自动判分,主观题 AI 评分) */
    ExamResultVO submit(Long examId, ExamSubmitDTO dto, Long userId);

    /** 我的考试结果 */
    ExamResultVO myResult(Long examId, Long userId);

    /** 超时自动交卷(定时任务调用) */
    void autoSubmitExpired();
}
