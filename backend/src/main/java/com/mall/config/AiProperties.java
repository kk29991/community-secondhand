package com.mall.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 大模型(通义千问 / 百炼 DashScope)配置
 * <p>
 * 密钥只从环境变量 {@code DASHSCOPE_API_KEY} 或本地配置读取, 不写入代码仓库。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mall.ai")
public class AiProperties {

    /** API Key, 形如 sk-xxxx; 为空表示未启用大模型, 系统自动降级为本地规则引擎 */
    private String apiKey = "";

    /** OpenAI 兼容模式地址 */
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    /** 模型名称 */
    private String model = "qwen-plus";

    private int timeoutSeconds = 60;

    private int maxTokens = 1024;

    private double temperature = 0.6;

    /** 注入提示词的商品条数上限 */
    private int contextProductLimit = 8;

    /** 是否已配置可用的密钥 */
    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** 拼接出 chat/completions 地址 */
    public String chatCompletionsUrl() {
        String base = baseUrl == null ? "" : baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/chat/completions";
    }
}
