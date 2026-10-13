package com.ailearn.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 内置模拟生成器:不依赖外部大模型,按提示词里的结构化标记生成符合契约的 JSON。
 *
 * <p>用途:本地开发、演示与自动化测试时秒级跑通全流程。它与真实大模型共用同一套提示词模板、
 * 同一套响应解析与校验逻辑,因此切换 <code>ai.provider</code> 不影响业务代码。</p>
 *
 * <p>提示词里的约定标记:</p>
 * <ul>
 *   <li>出题:【知识点:xxx】【题型:xxx】【难度:x】【数量:x】</li>
 *   <li>评分:【参考答案:xxx】【学生答案:xxx】【满分:x】</li>
 *   <li>建议:【科目:xxx】【知识点统计:...】【错题数:x】</li>
 * </ul>
 */
@RequiredArgsConstructor
@Slf4j
public class MockLlmClient implements LlmClient {

    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    @Override
    public String provider() {
        return "mock";
    }

    @Override
    public String model() {
        return "builtin-simulator";
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        try {
            // 依据模板里独有的标记区分任务,避免关键词互相干扰
            // (出题模板里也含"主观题"字样,所以不能拿它当判据)
            if (userPrompt.contains("【学生答案:") || userPrompt.contains("【参考答案:")) {
                return gradingJson(userPrompt);
            }
            if (userPrompt.contains("【知识点统计:") || userPrompt.contains("【错题数:")) {
                return adviceJson(userPrompt);
            }
            return questionsJson(userPrompt);
        } catch (Exception ex) {
            log.warn("模拟生成失败:{}", ex.getMessage());
            return "{}";
        }
    }

    /* ---------------- 出题 ---------------- */

    private String questionsJson(String prompt) throws Exception {
        String knowledge = extract(prompt, "【知识点:(.+?)】", "综合知识点");
        String type = extract(prompt, "【题型:(.+?)】", "SINGLE");
        int difficulty = Integer.parseInt(extract(prompt, "【难度:(\\d)】", "3"));
        int count = Integer.parseInt(extract(prompt, "【数量:(\\d+)】", "3"));
        count = Math.max(1, Math.min(count, 10));

        List<Map<String, Object>> questions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            questions.add(buildQuestion(knowledge, type, difficulty, i));
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("questions", questions);
        return objectMapper.writeValueAsString(root);
    }

    private Map<String, Object> buildQuestion(String knowledge, String type, int difficulty, int index) {
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("type", type);
        q.put("difficulty", difficulty);
        q.put("knowledgePoint", knowledge);

        switch (type) {
            case "MULTIPLE" -> {
                String[][] variants = {
                        {"关于「%s」,下列说法正确的有(多选)?",
                                "%s 的核心概念需要结合具体场景理解", "%s 与其它知识点完全无关",
                                "%s 的常见考查形式包括概念辨析与场景应用", "%s 在实际项目中不需要考虑边界情况"},
                        {"下列哪些属于「%s」的学习要点(多选)?",
                                "理解 %s 的定义与适用条件", "%s 只需要背诵结论即可",
                                "能够结合实例分析 %s 的应用", "%s 与相邻知识点没有关联"},
                        {"关于「%s」的常见误区,下列描述正确的有(多选)?",
                                "认为 %s 的结论在任何条件下都成立", "%s 需要区分不同场景的边界条件",
                                "忽略 %s 与其它知识点的联系", "只需记住 %s 的结论就能灵活应用"}
                };
                String[] v = variants[index % variants.length];
                q.put("stem", String.format(v[0], knowledge));
                q.put("options", List.of(
                        option("A", String.format(v[1], knowledge)),
                        option("B", String.format(v[2], knowledge)),
                        option("C", String.format(v[3], knowledge)),
                        option("D", String.format(v[4], knowledge))));
                q.put("answer", index % 2 == 0 ? "A,C" : "B,C");
                q.put("analysis", "多选题要逐项判断:" + knowledge + " 的结论有适用条件,且与相邻知识点存在联系,"
                        + "因此表述绝对化或否认关联的选项都不正确。");
            }
            case "JUDGE" -> {
                String[][] variants = {
                        {"判断:" + knowledge + " 只能通过死记硬背掌握,无法通过练习提升。", "错",
                                knowledge + " 属于可练习强化的知识,通过针对性刷题与错题复盘能显著提升掌握度。"},
                        {"判断:学习「" + knowledge + "」时,理解适用条件比记住结论更重要。", "对",
                                "结论只在特定条件下成立,理解适用条件才能在变式题中正确作答。"},
                        {"判断:「" + knowledge + "」与其它知识点之间不存在任何联系。", "错",
                                "知识体系是网状结构," + knowledge + " 往往作为前置或后续知识被其它知识点引用。"}
                };
                String[] v = variants[index % variants.length];
                q.put("stem", v[0]);
                q.put("answer", v[1]);
                q.put("analysis", v[2]);
            }
            case "FILL" -> {
                String[][] variants = {
                        {"在「" + knowledge + "」中,衡量掌握程度最直接的两个指标是正确率与 ______ 。", "完成度",
                                "除正确率外,答题完成度(覆盖率)也是衡量掌握程度的重要指标。"},
                        {"学习「" + knowledge + "」时,把错题按 ______ 归档,能显著提高复习效率。", "知识点",
                                "按知识点归档错题可以发现薄弱环节,避免重复刷已经掌握的题。"},
                        {"「" + knowledge + "」的练习题建议先做 ______ 题,再逐步提高难度。", "基础",
                                "先建立正确概念再提升难度,否则容易在难题上反复受挫、形成错误直觉。"}
                };
                String[] v = variants[index % variants.length];
                q.put("stem", v[0]);
                q.put("answer", v[1]);
                q.put("analysis", v[2]);
            }
            case "SUBJECTIVE" -> {
                String[][] variants = {
                        {"请结合实例说明「%s」的核心要点,以及你在学习该知识点时的常见困难。",
                                "要点一:准确说明 %s 的定义与适用场景;要点二:结合具体例子分析;"
                                        + "要点三:指出常见误区与规避方式;要点四:说明与其它知识点的联系。",
                                "评分关注:概念准确性、实例贴切度、误区识别、知识关联四个维度。"},
                        {"在学习「%s」的过程中,你会如何安排练习与复习?请说明你的方案与依据。",
                                "要点一:明确 %s 的学习目标与前置知识;要点二:分阶段练习(基础→变式→综合);"
                                        + "要点三:错题复盘与周期性重做;要点四:用自测检验掌握程度。",
                                "评分关注:方案完整性、阶段划分合理性、复盘机制、可执行性。"}
                };
                String[] v = variants[index % variants.length];
                q.put("stem", String.format(v[0], knowledge));
                q.put("answer", String.format(v[1], knowledge));
                q.put("analysis", v[2]);
            }
            default -> {
                // 单选:多套模板轮换 + 知识点词替换,保证同批题目有区分度
                String[][] variants = {
                        {"关于「%s」,下列说法正确的是?",
                                "掌握 %s 只需要记住结论,不需要理解推导过程",
                                "%s 的理解需要结合定义、场景与反例三方面",
                                "%s 在任何场景下的结论都完全一致",
                                "学习 %s 时不需要关注边界条件", "B",
                                "理解概念需要定义、场景与反例结合;其余选项都忽略了条件与边界。"},
                        {"在复习「%s」时,下列做法最有效的是?",
                                "只看笔记不做题", "先做题再对照解析,并把错题按知识点归档",
                                "只刷最简单的题", "遇到错题直接跳过", "B",
                                "做题暴露问题、解析修正理解、错题归档避免重复踩坑,是效率最高的组合。"},
                        {"下列对「%s」的描述,哪一项是正确的?",
                                "它与实际项目毫无关系", "它只适用于考试,工作中用不到",
                                "它的结论有适用条件,需要结合场景判断", "只要背下结论就能应对所有变式题", "C",
                                "任何知识点的结论都有前提条件,变式题考查的正是条件识别能力。"},
                        {"学习「%s」时,遇到反复做错的题目应该怎么做?",
                                "记住答案即可", "放弃这类题目",
                                "分析错因并回到知识点补基础,过几天再做一次", "只做同类简单题找信心", "C",
                                "错题的价值在于暴露理解偏差,定位到知识点重新学习后再复做才能巩固。"}
                };
                String[] v = variants[index % variants.length];
                q.put("stem", String.format(v[0], knowledge));
                q.put("options", List.of(
                        option("A", String.format(v[1], knowledge)),
                        option("B", String.format(v[2], knowledge)),
                        option("C", String.format(v[3], knowledge)),
                        option("D", String.format(v[4], knowledge))));
                q.put("answer", v[5]);
                q.put("analysis", v[6]);
            }
        }
        q.put("score", type.equals("SUBJECTIVE") ? 10 : (type.equals("MULTIPLE") ? 6 : 5));
        q.put("tags", knowledge + ",AI生成");
        return q;
    }

    private Map<String, String> option(String key, String content) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("key", key);
        map.put("content", content);
        return map;
    }

    /* ---------------- 主观题评分 ---------------- */

    private String gradingJson(String prompt) throws Exception {
        String reference = extract(prompt, "【参考答案:([\\s\\S]*?)】", "");
        String student = extract(prompt, "【学生答案:([\\s\\S]*?)】", "");
        double fullScore = Double.parseDouble(extract(prompt, "【满分:([0-9.]+)】", "10"));

        List<String> points = splitPoints(reference);
        List<String> hit = new ArrayList<>();
        List<String> miss = new ArrayList<>();
        for (String point : points) {
            if (covered(point, student)) {
                hit.add(point);
            } else {
                miss.add(point);
            }
        }
        double ratio = points.isEmpty() ? 0.6 : (double) hit.size() / points.size();
        // 覆盖度映射为得分:答对要点越全得分越高,全对给满分,完全没打到要点给 30% 基础分
        double score = Math.round(fullScore * (0.3 + 0.7 * ratio) * 10) / 10.0;

        StringBuilder feedback = new StringBuilder();
        feedback.append("得分 ").append(score).append("/").append(fullScore).append("。");
        if (!hit.isEmpty()) {
            feedback.append("命中要点:").append(String.join(";", hit)).append("。");
        }
        if (!miss.isEmpty()) {
            feedback.append("遗漏要点:").append(String.join(";", miss)).append("。建议对照参考答案补齐。");
        } else {
            feedback.append("要点覆盖完整,答题结构清晰。");
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("score", score);
        root.put("fullScore", fullScore);
        root.put("hitPoints", hit);
        root.put("missingPoints", miss);
        root.put("feedback", feedback.toString());
        root.put("confidence", points.isEmpty() ? 0.5 : Math.min(0.95, 0.5 + 0.45 * ratio));
        return objectMapper.writeValueAsString(root);
    }

    /** 参考答案切分为要点 */
    private List<String> splitPoints(String reference) {
        List<String> points = new ArrayList<>();
        if (reference == null || reference.isBlank()) {
            return points;
        }
        for (String part : reference.split("[;；。\\n]")) {
            String p = part.replaceAll("^[0-9]+[)）.、]\\s*", "").trim();
            if (p.length() >= 4) {
                points.add(p);
            }
        }
        return points;
    }

    /** 用 2 字滑窗关键词判断学生答案是否覆盖某个要点 */
    private boolean covered(String point, String studentAnswer) {
        if (studentAnswer == null || studentAnswer.isBlank()) {
            return false;
        }
        String answer = studentAnswer.replaceAll("\\s", "");
        String cleaned = point.replaceAll("[\\s,]|要点[一二三四五六七八九十]", "");
        if (cleaned.length() < 4) {
            return answer.contains(cleaned);
        }
        int hit = 0;
        int total = 0;
        for (int i = 0; i + 2 <= cleaned.length(); i++) {
            total++;
            if (answer.contains(cleaned.substring(i, i + 2))) {
                hit++;
            }
        }
        return total > 0 && (double) hit / total >= 0.35;
    }

    /* ---------------- 学习建议 ---------------- */

    private String adviceJson(String prompt) throws Exception {
        String subject = extract(prompt, "【科目:(.*?)】", "当前科目");
        String stats = extract(prompt, "【知识点统计:([\\s\\S]*?)】", "");
        int wrongCount = Integer.parseInt(extract(prompt, "【错题数:(\\d+)】", "0"));

        List<String> weak = new ArrayList<>();
        Matcher matcher = Pattern.compile("([^,;\\s]+?):(\\d+(?:\\.\\d+)?)%").matcher(stats == null ? "" : stats);
        while (matcher.find()) {
            double rate = Double.parseDouble(matcher.group(2));
            if (rate < 70) {
                weak.add(matcher.group(1));
            }
        }

        StringBuilder content = new StringBuilder();
        content.append("## 学习建议(").append(subject).append(")\n\n");
        content.append("### 现状诊断\n");
        if (weak.isEmpty()) {
            content.append("- 各知识点正确率均不低于 70%,整体掌握情况良好。\n");
        } else {
            content.append("- 薄弱知识点:").append(String.join("、", weak)).append(",正确率低于 70%,建议优先复习。\n");
        }
        content.append("- 错题本累计 ").append(wrongCount).append(" 题,建议每天复盘 5 题。\n\n");
        content.append("### 行动建议\n");
        content.append("1. 针对薄弱知识点做 10 题专项练习,错题当场看解析;\n");
        content.append("2. 三天后重做错题本中标记为未掌握的题目,巩固记忆;\n");
        content.append("3. 用 AI 出题功能按同一知识点生成新题,检验是否真正掌握;\n");
        content.append("4. 每完成一个知识点后做一次小测,及时暴露遗忘。\n\n");
        content.append("### 预期节奏\n");
        content.append("- 每日 20 分钟专项练习 + 10 分钟错题复盘,一周内可将薄弱知识点正确率提升 10-15 个百分点。\n");

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("weakPoints", weak);
        root.put("content", content.toString());
        return objectMapper.writeValueAsString(root);
    }

    private String extract(String text, String regex, String defaultValue) {
        Matcher matcher = Pattern.compile(regex).matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group(1).trim() : defaultValue;
    }
}
