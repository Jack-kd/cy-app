package com.cy.app.data

import com.cy.app.data.model.AiProvider

/**
 * 内置的 OpenAI 兼容 AI 提供方。
 * 地址与模型清单与 Web 端「模型 & API」设置保持一致。
 */
object AiProviders {

    val all: List<AiProvider> = listOf(
        AiProvider(
            id = "deepseek",
            displayName = "DeepSeek",
            defaultBaseUrl = "https://api.deepseek.com/v1",
            models = listOf("deepseek-chat", "deepseek-reasoner"),
        ),
        AiProvider(
            id = "openai",
            displayName = "OpenAI",
            defaultBaseUrl = "https://api.openai.com/v1",
            models = listOf("gpt-4o-mini", "gpt-4o"),
        ),
        AiProvider(
            id = "qwen",
            displayName = "阿里云百炼",
            defaultBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
            models = listOf("qwen-plus", "qwen-turbo"),
        ),
        AiProvider(
            id = "glm",
            displayName = "智谱GLM",
            defaultBaseUrl = "https://open.bigmodel.cn/api/paas/v4",
            models = listOf("glm-4-flash", "glm-4-air"),
        ),
    )

    fun byId(id: String): AiProvider = all.firstOrNull { it.id == id } ?: all.first()
}
