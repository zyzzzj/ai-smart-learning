package com.ailearn.ai;

/**
 * 大模型调用适配层。
 *
 * <p>两种实现:</p>
 * <ul>
 *   <li>{@link OllamaLlmClient} —— 真实调用本地 Ollama 部署的 DeepSeek</li>
 *   <li>{@link MockLlmClient} —— 内置模拟生成器,不依赖外部服务,秒级返回</li>
 * </ul>
 *
 * <p>由配置项 <code>ai.provider</code>(mock / ollama)决定注入哪个实现,业务代码只依赖本接口。</p>
 */
public interface LlmClient {

    /** 提供方标识:mock / ollama */
    String provider();

    /** 模型名 */
    String model();

    /**
     * 单轮对话:给定系统提示词与用户提示词,返回模型原始文本。
     *
     * @throws com.ailearn.common.BusinessException 调用失败时抛出,由上层记录到 ai_task
     */
    String chat(String systemPrompt, String userPrompt);
}
