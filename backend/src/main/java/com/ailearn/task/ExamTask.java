package com.ailearn.task;

import com.ailearn.service.ExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 考试定时任务:每分钟把超时未交卷的考试自动交卷判分。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExamTask {

    private final ExamService examService;

    @Scheduled(cron = "0 * * * * ?")
    public void autoSubmitExpiredExams() {
        examService.autoSubmitExpired();
    }
}
