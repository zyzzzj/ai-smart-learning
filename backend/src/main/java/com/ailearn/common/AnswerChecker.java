package com.ailearn.common;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 客观题答案判定:单选/多选/判断/填空统一归一化后比较。
 */
@Component
public class AnswerChecker {

    /**
     * 判断作答是否正确。
     */
    public boolean isCorrect(String type, String reference, String userAnswer) {
        if (reference == null || userAnswer == null) {
            return false;
        }
        return switch (type == null ? "" : type.toUpperCase()) {
            case Constants.TYPE_MULTIPLE -> normalizeSet(reference).equals(normalizeSet(userAnswer));
            case Constants.TYPE_JUDGE -> normalizeJudge(reference).equals(normalizeJudge(userAnswer));
            case Constants.TYPE_FILL -> matchFill(reference, userAnswer);
            default -> normalizeChoice(reference).equals(normalizeChoice(userAnswer));
        };
    }

    /** 单选:去空格转大写 */
    private String normalizeChoice(String value) {
        return value.replaceAll("\\s", "").toUpperCase();
    }

    /** 多选:拆分为集合后排序,顺序无关 */
    private List<String> normalizeSet(String value) {
        List<String> list = new ArrayList<>();
        for (String part : value.split("[,，;；\\s]+")) {
            String item = part.trim().toUpperCase();
            if (!item.isEmpty()) {
                list.add(item);
            }
        }
        Collections.sort(list);
        return list;
    }

    /** 判断:对/错 与 T/F/true/false 统一 */
    private String normalizeJudge(String value) {
        String v = value.trim().toLowerCase();
        if (v.startsWith("对") || v.equals("t") || v.equals("true") || v.equals("正确") || v.equals("√")) {
            return "对";
        }
        if (v.startsWith("错") || v.equals("f") || v.equals("false") || v.equals("错误") || v.equals("×")
                || v.equals("x")) {
            return "错";
        }
        return v;
    }

    /** 填空:忽略大小写与空白;参考答案可用 / 或 | 分隔多个可接受答案 */
    private boolean matchFill(String reference, String userAnswer) {
        String user = normalizeFill(userAnswer);
        if (user.isEmpty()) {
            return false;
        }
        List<String> acceptable = Arrays.stream(reference.split("[/|]"))
                .map(this::normalizeFill)
                .filter(s -> !s.isEmpty())
                .toList();
        return acceptable.contains(user);
    }

    private String normalizeFill(String value) {
        return value.replaceAll("[\\s,，。.;；:：]", "").toLowerCase();
    }
}
