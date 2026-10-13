package com.ailearn.common;

import com.ailearn.dto.QuestionOption;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 题目结构校验:手工录入与 AI 生成共用同一套规则,保证入库题目质量一致。
 */
@Component
public class QuestionValidator {

    /**
     * @return null 表示校验通过,否则返回失败原因
     */
    public String validate(String type, List<QuestionOption> options, String answer) {
        if (!StringUtils.hasText(type)) {
            return "题型不能为空";
        }
        if (!StringUtils.hasText(answer)) {
            return "参考答案不能为空";
        }
        boolean choice = Constants.TYPE_SINGLE.equals(type) || Constants.TYPE_MULTIPLE.equals(type);
        if (choice) {
            if (options == null || options.size() < 2) {
                return "选择题至少需要两个选项";
            }
            Set<String> keys = new LinkedHashSet<>();
            for (QuestionOption option : options) {
                if (option == null || !StringUtils.hasText(option.getKey()) || !StringUtils.hasText(option.getContent())) {
                    return "选项的标识与内容都不能为空";
                }
                keys.add(option.getKey().trim().toUpperCase());
            }
            String[] answers = answer.split("[,，]");
            if (Constants.TYPE_SINGLE.equals(type) && answers.length != 1) {
                return "单选题只能有一个正确答案";
            }
            for (String a : answers) {
                if (!keys.contains(a.trim().toUpperCase())) {
                    return "参考答案 " + a + " 不在选项范围内";
                }
            }
        }
        if (Constants.TYPE_JUDGE.equals(type)) {
            String value = answer.trim();
            boolean ok = "对".equals(value) || "错".equals(value)
                    || "T".equalsIgnoreCase(value) || "F".equalsIgnoreCase(value)
                    || "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
            if (!ok) {
                return "判断题答案只能是 对/错";
            }
        }
        if (Constants.TYPE_FILL.equals(type) && answer.trim().length() > 200) {
            return "填空题答案过长,请精简";
        }
        return null;
    }
}
