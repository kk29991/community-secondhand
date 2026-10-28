package com.mall.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mall.config.AiProperties;
import com.mall.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/**
 * 大模型对话服务 (通义千问 / 百炼 DashScope, OpenAI 兼容模式)。
 * <p>
 * 设计要点:
 * 1. 走 OpenAI 兼容接口, 不引入任何厂商 SDK, 只依赖 JDK 自带的 HttpClient;
 * 2. 未配置 API Key 时 {@link #isEnabled()} 返回 false, 由调用方降级到本地规则引擎;
 * 3. 商品目录作为上下文注入 system 提示词, 约束模型不要编造价格与库存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmChatService {

    private final AiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile HttpClient httpClient;

    /** 是否已配置密钥, 可用大模型 */
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public String modelName() {
        return properties.getModel();
    }

    private HttpClient client() {
        if (httpClient == null) {
            synchronized (this) {
                if (httpClient == null) {
                    httpClient = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(15))
                            .followRedirects(HttpClient.Redirect.NORMAL)
                            .build();
                }
            }
        }
        return httpClient;
    }

    // ---------------------------------------------------------
    // 提示词
    // ---------------------------------------------------------

    private static final String SYSTEM_PROMPT = """
            你是「社区二手交易平台」的在线客服兼导购助手, 名字叫「小淘」。
            本平台面向同一个小区(社区)内的业主, 提供闲置物品的转让与置换服务。

            工作要求:
            1. 只推荐【商品目录】中真实存在的闲置物品, 绝不编造名称、价格、成色或卖家。
            2. 价格、成色、库存一律以【商品目录】为准, 不要凭印象猜测。
            3. 推荐时用简洁的列表给出「物品名 - 价格 - 成色」, 并说明推荐理由(1 句即可)。
            4. 若用户有预算约束(如「100 元以内」), 严格按预算筛选。
               若目录里没有预算内的物品, 就如实说「预算内没有找到合适的闲置」,
               并可以补充说明该品类当前的起步价, 但**不要直接把超预算物品当作推荐给出**。
               若用户说了尺寸、房间数等条件(如「三人的沙发」「8 公斤洗衣机」), 也要按描述筛选。
            5. 用户问交易方式、验货、退货等平台规则时, 按下面的【平台规则】回答。
            6. 语气亲切专业, 用中文, 回答控制在 200 字以内, 不要输出 Markdown 标题和代码块。
            7. 与社区闲置交易无关的问题, 礼貌引导回交易话题。
            8. 【商品目录】为空或没有用户想要的品类时, 必须如实回答「没有找到」,
               不要用其他品类的物品充数, 也不要推荐目录里没有的东西。

            【平台规则】
            - 交易方式: 小区门口自提 / 物业服务中心代收, 同小区内不产生物流费用。
            - 平台服务费: 单笔订单商品金额满 99 元免收, 未满收取 10 元(按卖家分别计算)。
            - 验货: 强烈建议当面验货确认无误后再签收; 家电数码请当场通电试机;
              交易地点建议选择小区门口、物业服务中心等公共区域。
            - 大件搬运: 家具、家电等大件需买家自行搬运, 建议提前联系卖家约人搭把手。
            - 退换: 二手物品不支持无理由退货, 但若实物与描述严重不符, 可通过平台申诉处理。
            - 支付: 支持余额、支付宝、微信三种方式(当前为演示环境, 为模拟支付)。
            - 安全提醒: 请勿脱离平台私下转账, 谨防以「保证金」「解冻费」为由的诈骗。
            """;

    /**
     * 构造商品目录上下文
     */
    public String buildCatalog(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return "【商品目录】(当前没有检索到相关商品)";
        }
        StringBuilder sb = new StringBuilder("【商品目录】\n");
        for (Product p : products) {
            sb.append("- ").append(p.getName());
            if (p.getPrice() != null) {
                sb.append(" | 价格: ").append(p.getPrice()).append(" 元");
            }
            if (p.getOriginalPrice() != null) {
                sb.append(" | 原价: ").append(p.getOriginalPrice()).append(" 元");
            }
            if (p.getStock() != null) {
                sb.append(" | 库存: ").append(p.getStock()).append(" 件");
            }
            if (p.getMerchantName() != null) {
                sb.append(" | 店铺: ").append(p.getMerchantName());
            }
            if (p.getSubTitle() != null && !p.getSubTitle().isBlank()) {
                sb.append(" | 卖点: ").append(truncate(p.getSubTitle(), 60));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    /**
     * 组装请求体
     *
     * @param stream 是否流式
     */
    private String buildRequestBody(List<Message> history, String catalog, boolean stream) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.getModel());
        root.put("stream", stream);
        root.put("max_tokens", properties.getMaxTokens());
        root.put("temperature", properties.getTemperature());

        ArrayNode messages = root.putArray("messages");
        ObjectNode system = messages.addObject();
        system.put("role", "system");
        system.put("content", SYSTEM_PROMPT + "\n" + catalog);

        for (Message m : history) {
            ObjectNode node = messages.addObject();
            node.put("role", m.role());
            node.put("content", m.content());
        }
        return objectMapper.writeValueAsString(root);
    }

    /** 对话消息 */
    public record Message(String role, String content) {
    }

    // ---------------------------------------------------------
    // 非流式调用
    // ---------------------------------------------------------

    /**
     * 一次性获取完整回复
     *
     * @return 模型回复; 失败返回 null, 由调用方降级
     */
    public String chatOnce(List<Message> history, String catalog) {
        if (!isEnabled()) {
            return null;
        }
        try {
            HttpRequest request = baseRequest(buildRequestBody(history, catalog, false)).build();
            HttpResponse<String> response = client().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                log.warn("大模型调用失败, HTTP {}: {}", response.statusCode(), truncate(response.body(), 300));
                return null;
            }
            JsonNode node = objectMapper.readTree(response.body());
            JsonNode content = node.path("choices").path(0).path("message").path("content");
            return content.isMissingNode() || content.isNull() ? null : content.asText();
        } catch (Exception e) {
            log.warn("大模型调用异常, 将降级为本地规则引擎: {}", e.getMessage());
            return null;
        }
    }

    private HttpRequest.Builder baseRequest(String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(properties.chatCompletionsUrl()))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "text/event-stream, application/json")
                .header("Authorization", "Bearer " + properties.getApiKey().trim())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
    }

    // ---------------------------------------------------------
    // 流式调用 (SSE)
    // ---------------------------------------------------------

    /**
     * 流式获取回复, 每个增量片段回调一次 {@code onDelta}。
     *
     * @return true 表示调用成功(已流式产出内容); false 表示失败, 调用方应降级
     */
    public boolean chatStream(List<Message> history, String catalog, Consumer<String> onDelta) {
        if (!isEnabled()) {
            return false;
        }
        boolean[] produced = {false};
        try {
            HttpRequest request = baseRequest(buildRequestBody(history, catalog, true)).build();
            HttpResponse<java.util.stream.Stream<String>> response =
                    client().send(request, HttpResponse.BodyHandlers.ofLines());

            if (response.statusCode() != 200) {
                log.warn("大模型流式调用失败, HTTP {}", response.statusCode());
                return false;
            }
            response.body()
                    .takeWhile(line -> !Thread.currentThread().isInterrupted())
                    .forEach(line -> handleSseLine(line, onDelta, produced));
            return produced[0];
        } catch (Exception e) {
            log.warn("大模型流式调用异常, 将降级为本地规则引擎: {}", e.getMessage());
            return produced[0];
        }
    }

    /** 解析一行 SSE, 提取 delta.content 并回调 */
    private void handleSseLine(String line, Consumer<String> onDelta, boolean[] produced) {
        if (line == null || line.isBlank() || !line.startsWith("data:")) {
            return;
        }
        String payload = line.substring(5).trim();
        if (payload.isEmpty() || "[DONE]".equals(payload)) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(payload);
            // 兼容 OpenAI 风格: choices[0].delta.content
            JsonNode delta = node.path("choices").path(0).path("delta").path("content");
            if (delta.isMissingNode() || delta.isNull()) {
                // 少数情况内容在 message.content (非流式回包)
                delta = node.path("choices").path(0).path("message").path("content");
            }
            if (!delta.isMissingNode() && !delta.isNull()) {
                String text = delta.asText();
                if (!text.isEmpty()) {
                    produced[0] = true;
                    onDelta.accept(text);
                }
            }
            JsonNode error = node.path("error");
            if (!error.isMissingNode() && !error.isNull()) {
                log.warn("大模型返回错误: {}", truncate(error.toString(), 300));
            }
        } catch (Exception ignored) {
            // 单行解析失败不影响整体流
        }
    }
}
