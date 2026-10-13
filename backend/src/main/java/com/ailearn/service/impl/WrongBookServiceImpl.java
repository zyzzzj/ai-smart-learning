package com.ailearn.service.impl;

import com.ailearn.entity.WrongQuestion;
import com.ailearn.mapper.WrongQuestionMapper;
import com.ailearn.service.WrongBookService;
import com.ailearn.vo.WrongQuestionVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 错题本实现:答错累加重置掌握标记,答对标记已掌握。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WrongBookServiceImpl implements WrongBookService {

    private final WrongQuestionMapper wrongQuestionMapper;

    @Override
    public boolean record(Long userId, Long questionId, String userAnswer, boolean correct) {
        WrongQuestion exists = wrongQuestionMapper.selectOne(Wrappers.<WrongQuestion>lambdaQuery()
                .eq(WrongQuestion::getUserId, userId)
                .eq(WrongQuestion::getQuestionId, questionId));
        if (!correct) {
            if (exists == null) {
                WrongQuestion wrong = new WrongQuestion();
                wrong.setUserId(userId);
                wrong.setQuestionId(questionId);
                wrong.setWrongCount(1);
                wrong.setLastAnswer(userAnswer);
                wrong.setLastWrongTime(LocalDateTime.now());
                wrong.setMastered(0);
                wrongQuestionMapper.insert(wrong);
            } else {
                exists.setWrongCount((exists.getWrongCount() == null ? 0 : exists.getWrongCount()) + 1);
                exists.setLastAnswer(userAnswer);
                exists.setLastWrongTime(LocalDateTime.now());
                exists.setMastered(0);
                wrongQuestionMapper.updateById(exists);
            }
            return true;
        }
        if (exists != null && (exists.getMastered() == null || exists.getMastered() == 0)) {
            exists.setMastered(1);
            wrongQuestionMapper.updateById(exists);
        }
        return false;
    }

    @Override
    public void remove(Long userId, Long questionId) {
        wrongQuestionMapper.delete(Wrappers.<WrongQuestion>lambdaQuery()
                .eq(WrongQuestion::getUserId, userId)
                .eq(WrongQuestion::getQuestionId, questionId));
    }

    @Override
    public List<WrongQuestionVO> list(Long userId, Integer mastered, Long knowledgePointId, int limit) {
        return wrongQuestionMapper.selectWrongList(userId, mastered, knowledgePointId,
                Math.max(1, Math.min(limit, 100)));
    }
}
