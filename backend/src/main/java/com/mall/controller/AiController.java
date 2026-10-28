package com.mall.controller;

import com.mall.common.Result;
import com.mall.common.UserContext;
import com.mall.entity.AiChat;
import com.mall.entity.Product;
import com.mall.service.AiShoppingService;
import com.mall.service.LlmChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 导购 / 在线客服接口 (需登录)
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiShoppingService aiShoppingService;
    private final LlmChatService llmChatService;

    /** 流式输出用的线程池(守护线程, 不阻塞容器线程) */
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "ai-sse");
        t.setDaemon(true);
        return t;
    });

    /** 发送消息, 一次性返回(非流式, 保留兼容) */
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, String> body) {
        String message = body.get("message");
        String sessionId = body.get("sessionId");
        return Result.success(aiShoppingService.chat(UserContext.requireId(), sessionId, message));
    }

    /**
     * 流式对话 (SSE)。用 fetch + ReadableStream 消费, 因此用 POST。
     * <p>
     * 事件:
     * <ul>
     *   <li>{@code meta}  : 会话ID / 意图 / 使用的引擎(llm 或 rule) / 推荐商品</li>
     *   <li>{@code delta} : 增量文本</li>
     *   <li>{@code done}  : 结束标记</li>
     *   <li>{@code error} : 出错信息</li>
     * </ul>
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, String> body) {
        // 注意: 未登录请求已在 AuthInterceptor 中拦截(SSE 响应一旦提交就无法再返回 JSON 错误)
        Long userId = UserContext.requireId();
        String message = body.get("message");
        String sessionId = body.get("sessionId");

        SseEmitter emitter = new SseEmitter(120_000L);
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> log.debug("SSE 连接异常: {}", e.getMessage()));

        sseExecutor.submit(() -> {
            try {
                AiShoppingService.ChatStreamResult result =
                        aiShoppingService.chatStream(userId, sessionId, message, delta -> {
                            try {
                                emitter.send(SseEmitter.event().name("delta").data(delta));
                            } catch (IOException e) {
                                throw new IllegalStateException("客户端已断开", e);
                            }
                        });

                emitter.send(SseEmitter.event().name("meta").data(metaPayload(result)));
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                log.warn("流式对话失败: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data(Map.of("message", String.valueOf(e.getMessage()))));
                } catch (Exception ignored) {
                    // 连接已断开, 无需处理
                }
                emitter.complete();
            }
        });
        return emitter;
    }

    /** 元信息精简为前端需要的字段, 避免把整个商品详情塞进 SSE */
    private Map<String, Object> metaPayload(AiShoppingService.ChatStreamResult result) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("sessionId", result.sessionId());
        meta.put("intent", result.intent());
        meta.put("engine", result.engine());
        List<Map<String, Object>> products = result.products().stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("mainImage", p.getMainImage());
            m.put("price", p.getPrice());
            m.put("originalPrice", p.getOriginalPrice());
            m.put("merchantName", p.getMerchantName());
            m.put("stock", p.getStock());
            return m;
        }).toList();
        meta.put("products", products);
        meta.put("overBudgetIds", result.overBudgetIds());
        return meta;
    }

    /** 当前使用的引擎与模型(前端可据此显示"由通义千问驱动") */
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("llmEnabled", llmChatService.isEnabled());
        map.put("model", llmChatService.modelName());
        return Result.success(map);
    }

    /** 会话历史 */
    @GetMapping("/history")
    public Result<List<AiChat>> history(@RequestParam(required = false) String sessionId) {
        return Result.success(aiShoppingService.history(UserContext.requireId(), sessionId));
    }

    /** 清空会话 */
    @DeleteMapping("/session")
    public Result<Void> clear(@RequestParam String sessionId) {
        aiShoppingService.clearSession(UserContext.requireId(), sessionId);
        return Result.success("会话已清空", null);
    }
}
