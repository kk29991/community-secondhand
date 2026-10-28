package com.mall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.dto.ProductQueryDTO;
import com.mall.entity.AiChat;
import com.mall.entity.Category;
import com.mall.entity.Product;
import com.mall.mapper.AiChatMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * AI 导购服务
 * <p>
 * 采用「意图识别 + 关键词检索」的可解释策略生成导购建议, 不依赖外部大模型,
 * 保证离线可用; 后续可替换为一个 LLM 调用而保持接口不变。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiShoppingService {

    private final AiChatMapper aiChatMapper;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final LlmChatService llmChatService;

    /** 送入大模型的最大历史消息条数(不含本次提问) */
    private static final int MAX_HISTORY = 8;

    /**
     * 预算抽取: 必须带货币单位或上下限词, 否则不认为是预算。
     * <p>
     * 之所以要求这个约束, 是因为「维生素C咀嚼片」这类商品名里也有数字/字母,
     * 宽松匹配会把商品名误判成预算, 导致检索结果被错误过滤。
     */
    private static final java.util.regex.Pattern BUDGET_PATTERN = java.util.regex.Pattern.compile(
            "(\\d{2,6})\\s*(?:元|块钱|块|rmb|RMB)\\s*(?:以内|以下|之内|左右|内)?"
                    + "|(\\d{2,6})\\s*(?:以内|以下|之内)"
                    + "|(?:预算|不超过|最多|最多不超过)\\s*(\\d{2,6})");

    /** 检索噪音词: 这些词本身没有商品指向性, 不应作为关键词去查库 */
    private static final List<String> STOPWORDS = List.of(
            "推荐", "什么", "有没", "没有", "想要", "想买", "帮我", "可以", "怎么", "哪些", "哪个",
            "多少", "钱", "价格", "一下", "一个", "一款", "一件", "有什", "么样", "怎样", "如何",
            "或者", "还是", "就是", "的话", "比较", "适合", "好用", "值得", "不错", "谢谢", "你好"
    );

    /** 命中数量较少时, 允许保留「略超预算」的商品, 交给模型如实说明 */
    private static final int MIN_USEFUL_HITS = 2;

    /**
     * 类目意图词 -> 检索关键词, 优先级最高。
     * <p>
     * 顺序即匹配优先级, 越具体的商品词越靠前(如「手机」先于「耳机」),
     * 这样即使靠前的词命中很多, 也不会把后面的词挤出检索范围。
     */
    private static final Map<String, List<String>> INTENT_KEYWORDS = new LinkedHashMap<>();

    static {
        // 关键词按「社区二手场景」的实际在售品类组织, 与 sql/generate-seed.js 的商品数据对应。
        // 顺序即匹配优先级。
        INTENT_KEYWORDS.put("家具", List.of(
                "沙发", "餐桌", "书桌", "桌子", "椅子", "电脑椅", "书架", "柜", "收纳", "衣帽架", "床垫"));
        INTENT_KEYWORDS.put("家电", List.of(
                "冰箱", "洗衣机", "空调", "电饭煲", "微波炉", "净化器", "电风扇", "风扇", "台灯",
                "热水壶", "吹风机", "加湿器", "挂烫机", "电暖器", "豆浆机", "电磁炉", "压力锅"));
        INTENT_KEYWORDS.put("母婴", List.of(
                "婴儿车", "推车", "婴儿床", "安全座椅", "滑板车", "积木", "乐高", "玩具", "绘本", "儿童"));
        INTENT_KEYWORDS.put("数码", List.of(
                "手机", "笔记本", "电脑", "平板", "iPad", "耳机", "显示器", "键盘", "相机", "单反",
                "路由器", "手环", "AirPods"));
        INTENT_KEYWORDS.put("服饰", List.of(
                "外套", "卫衣", "毛衣", "开衫", "衬衫", "牛仔", "T恤", "衣服", "鞋", "背包", "双肩包", "包"));
        INTENT_KEYWORDS.put("图书", List.of(
                "书", "书籍", "教材", "三体", "小说", "读物", "绘本", "真题", "考研"));
        INTENT_KEYWORDS.put("运动", List.of(
                "自行车", "单车", "山地车", "折叠车", "跑步机", "瑜伽", "哑铃", "羽毛球", "球拍", "健身"));
        INTENT_KEYWORDS.put("宠物", List.of(
                "猫爬架", "猫砂", "猫砂盆", "宠物", "航空箱", "猫", "狗"));
    }

    /**
     * 处理一次用户提问
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> chat(Long userId, String sessionId, String message) {
        String sid = (sessionId == null || sessionId.isBlank())
                ? UUID.randomUUID().toString().replace("-", "")
                : sessionId;
        String text = message == null ? "" : message.trim();

        // 1. 记录用户消息
        AiChat userMsg = new AiChat();
        userMsg.setUserId(userId);
        userMsg.setSessionId(sid);
        userMsg.setRole("user");
        userMsg.setContent(text);
        aiChatMapper.insert(userMsg);

        // 2. 识别意图
        String intent = detectIntent(text);

        // 3. 检索候选商品
        List<Product> candidates = search(text).products();

        // 4. 生成回复文案
        String reply = composeReply(text, intent, candidates);

        // 5. 记录 AI 回复
        AiChat aiMsg = new AiChat();
        aiMsg.setUserId(userId);
        aiMsg.setSessionId(sid);
        aiMsg.setRole("assistant");
        aiMsg.setContent(reply);
        aiMsg.setIntent(intent);
        aiMsg.setRefProductIds(candidates.stream()
                .map(p -> String.valueOf(p.getId()))
                .collect(Collectors.joining(",")));
        aiChatMapper.insert(aiMsg);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sid);
        result.put("intent", intent);
        result.put("reply", reply);
        result.put("products", candidates);
        return result;
    }

    /**
     * 处理一次用户提问 (流式)
     * <p>
     * 优先走大模型(通义千问); 未配置密钥或调用失败时, 自动降级为本地规则引擎,
     * 并把规则引擎生成的完整文案一次性回调出去, 保证前端行为一致。
     *
     * @param onDelta 增量文本回调(逐字输出)
     * @return 本次使用的引擎: llm / rule
     */
    public ChatStreamResult chatStream(Long userId, String sessionId, String message, Consumer<String> onDelta) {
        String sid = (sessionId == null || sessionId.isBlank())
                ? UUID.randomUUID().toString().replace("-", "")
                : sessionId;
        String text = message == null ? "" : message.trim();

        // 1. 记录用户消息
        AiChat userMsg = new AiChat();
        userMsg.setUserId(userId);
        userMsg.setSessionId(sid);
        userMsg.setRole("user");
        userMsg.setContent(text);
        aiChatMapper.insert(userMsg);

        // 2. 本地检索候选商品(既作为大模型上下文, 也作为前端推荐卡片)
        String intent = detectIntent(text);
        SearchResult searchResult = search(text);
        List<Product> candidates = searchResult.products();
        List<Long> overBudgetIds = searchResult.overBudgetIds();

        // 3. 优先大模型
        String engine = "rule";
        StringBuilder collected = new StringBuilder();
        if (llmChatService.isEnabled()) {
            String catalog = llmChatService.buildCatalog(candidates);
            List<LlmChatService.Message> history = buildHistory(userId, sid, text);
            boolean ok = llmChatService.chatStream(history, catalog, delta -> {
                collected.append(delta);
                onDelta.accept(delta);
            });
            if (ok && collected.length() > 0) {
                engine = "llm";
            }
        }

        // 4. 降级: 本地规则引擎
        if (!"llm".equals(engine)) {
            String reply = composeReply(text, intent, candidates);
            collected.setLength(0);
            collected.append(reply);
            onDelta.accept(reply);
        }

        // 5. 落库
        AiChat aiMsg = new AiChat();
        aiMsg.setUserId(userId);
        aiMsg.setSessionId(sid);
        aiMsg.setRole("assistant");
        aiMsg.setContent(collected.toString());
        aiMsg.setIntent(intent);
        aiMsg.setRefProductIds(candidates.stream()
                .map(p -> String.valueOf(p.getId()))
                .collect(Collectors.joining(",")));
        aiChatMapper.insert(aiMsg);

        return new ChatStreamResult(sid, intent, engine, candidates, overBudgetIds, collected.toString());
    }

    /** 流式对话结果 */
    public record ChatStreamResult(String sessionId, String intent, String engine,
                                   List<Product> products, List<Long> overBudgetIds, String reply) {
    }

    /** 检索结果: 候选商品 + 其中超出用户预算的商品ID(用于前端提示与测试断言) */
    public record SearchResult(List<Product> products, List<Long> overBudgetIds) {
    }

    /**
     * 组装送入大模型的历史对话(按时间正序, 最多 {@value MAX_HISTORY} 条)
     */
    private List<LlmChatService.Message> buildHistory(Long userId, String sessionId, String currentText) {
        List<AiChat> rows = aiChatMapper.selectList(Wrappers.<AiChat>lambdaQuery()
                .eq(AiChat::getUserId, userId)
                .eq(AiChat::getSessionId, sessionId)
                .orderByDesc(AiChat::getId)
                .last("LIMIT " + (MAX_HISTORY + 1)));
        List<LlmChatService.Message> history = new ArrayList<>();
        // 倒序取出后反转, 并剔除刚刚写入的当前提问(单独追加, 保证顺序)
        for (int i = rows.size() - 1; i >= 0; i--) {
            AiChat row = rows.get(i);
            if (!"user".equals(row.getRole()) && !"assistant".equals(row.getRole())) {
                continue;
            }
            // 最后一条是本轮提问, 跳过, 稍后追加
            if (i == 0 && "user".equals(row.getRole()) && currentText.equals(row.getContent())) {
                continue;
            }
            history.add(new LlmChatService.Message(row.getRole(), row.getContent()));
        }
        history.add(new LlmChatService.Message("user", currentText));
        return history;
    }

    /**
     * 意图识别
     * <p>
     * 优先级: 类目词(家具/家电/母婴/数码/服饰/图书/运动/宠物) > 问候 > 性价比 > 推荐 > 兜底搜索。
     * 类目优先于「推荐」等泛化词, 否则「我想买件毛衣, 有推荐吗」会被误判为泛推荐。
     */
    public String detectIntent(String text) {
        if (text == null || text.isBlank()) {
            return "闲聊";
        }
        // 类目意图: 既匹配类目名本身(如「服装」), 也匹配该类目的代表性商品词(如「毛衣」)
        for (Map.Entry<String, List<String>> entry : INTENT_KEYWORDS.entrySet()) {
            if (text.contains(entry.getKey()) || entry.getValue().stream().anyMatch(text::contains)) {
                return entry.getKey();
            }
        }
        if (containsAny(text, "你好", "hi", "hello", "在吗")) {
            return "问候";
        }
        if (containsAny(text, "便宜", "性价比", "实惠", "优惠")) {
            return "性价比";
        }
        if (containsAny(text, "推荐", "买什么", "有什么", "适合")) {
            return "推荐";
        }
        return "搜索";
    }

    /**
     * 商品检索: 关键词/类目检索 + 预算过滤。
     * <p>
     * 设计要点:
     * 1. 先把预算解析出来, 尽早过滤, 避免用「预算内热销商品」凑数误导用户与模型;
     * 2. 关键词命中顺序即相关度顺序(精确关键词 > 滑窗匹配);
     * 3. 全部无命中时才回退到热销榜, 并且**回退结果同样受预算约束**;
     * 4. 不重复查库(pageProducts 返回的记录已带 merchantName 等展示字段)。
     */
    private SearchResult search(String text) {
        Integer budget = extractBudget(text);

        // 1. 先按「商品关键词」检索。
        //    注意: 不能要求文本包含类目名(如「数码」「医药」) —— 用户不会说"我要买医药",
        //    只会说"有什么维生素", 所以必须遍历商品关键词本身去匹配。
        Map<Long, Product> hits = new LinkedHashMap<>();
        for (List<String> group : INTENT_KEYWORDS.values()) {
            boolean groupHit = false;
            for (String keyword : group) {
                if (!text.contains(keyword)) {
                    continue;
                }
                int before = hits.size();
                // 若库里存在「与关键词同名的分类」, 优先按分类检索, 而不是按文本模糊匹配。
                // 必要性: 二手商品的标题是品牌型号(如「iPhone 13」「小米 13」), 名称里不含
                // 「手机」这类品类词; 而副标题里反而可能出现(打印机的"手机无线直连"),
                // 只做文本匹配会把整个品类的商品漏掉、把无关商品当成命中。
                int catBefore = hits.size();
                searchByCategoryName(keyword, hits);
                if (hits.size() == catBefore) {
                    // 没有同名分类时才退回文本检索
                    searchByKeyword(keyword, hits);
                }
                groupHit = groupHit || hits.size() > before;
            }
            // 某一类目已命中就不再继续放宽到其他类目
            if (groupHit) {
                break;
            }
        }

        // 2. 商品关键词没有命中时, 才用滑窗关键词兜底(例如用户直接输入商品名片段)
        if (hits.isEmpty()) {
            for (String keyword : slidingKeywords(text)) {
                searchByKeyword(keyword, hits);
                if (hits.size() >= 8) {
                    break;
                }
            }
        }

        // 3. 预算过滤: 优先保留预算内商品
        List<Product> withinBudget = new ArrayList<>();
        List<Product> overBudget = new ArrayList<>();
        for (Product p : hits.values()) {
            if (budget == null || (p.getPrice() != null && p.getPrice().doubleValue() <= budget)) {
                withinBudget.add(p);
            } else {
                overBudget.add(p);
            }
        }

        // 预算内商品太少时, 补上「略超预算」的同类商品(按价格升序),
        // 让模型可以如实告诉用户「有该品类但都超出预算」, 而不是回答「没有这个品类」。
        // 注意: 只有用户**没有明确预算**时才这样兜底。若用户明确说了「100 元以内」,
        // 就必须尊重这个硬约束, 不能把超预算商品当作推荐返回。
        List<Long> overBudgetIds = new ArrayList<>();
        boolean hasExplicitBudget = budget != null;
        if (!hasExplicitBudget && withinBudget.size() < MIN_USEFUL_HITS && !overBudget.isEmpty()) {
            overBudget.sort(java.util.Comparator.comparing(
                    p -> p.getPrice() == null ? java.math.BigDecimal.valueOf(Long.MAX_VALUE) : p.getPrice()));
            for (Product p : overBudget) {
                withinBudget.add(p);
                overBudgetIds.add(p.getId());
                if (withinBudget.size() >= 8) {
                    break;
                }
            }
        }

        // 4. 兜底: 完全无命中时才用热销榜, 且同样尊重预算
        if (withinBudget.isEmpty()) {
            List<Product> hot = productService.hotProducts(12).stream()
                    .filter(p -> budget == null || (p.getPrice() != null && p.getPrice().doubleValue() <= budget))
                    .limit(8)
                    .collect(Collectors.toList());
            return new SearchResult(hot, overBudgetIds);
        }
        return new SearchResult(withinBudget.stream().limit(8).collect(Collectors.toList()), overBudgetIds);
    }

    /** 用单个关键词检索并合并进结果集(按商品ID去重, 保持首次命中的顺序) */
    private void searchByKeyword(String keyword, Map<Long, Product> hits) {
        if (keyword == null || keyword.length() < 2) {
            return;
        }
        ProductQueryDTO query = new ProductQueryDTO();
        query.setKeyword(keyword);
        query.setStatus(1);
        query.setPageSize(6);
        var page = productService.pageProducts(query);
        for (Product p : page.getRecords()) {
            hits.putIfAbsent(p.getId(), p);
        }
    }

    /**
     * 按「与关键词同名的分类」检索。
     * <p>
     * 为什么需要: 二手闲置的标题是品牌型号(「iPhone 13」「捷安特 ATX 660」),
     * 名称里通常不含「手机」「自行车」这类品类词, 只用文本匹配会整个品类漏检,
     * 进而把打印机(副标题里恰好有"手机无线直连")这类无关商品当成命中结果。
     * <p>
     * 匹配规则: 分类名与关键词完全相等, 或分类名包含关键词(如「手机配件」含「手机」)。
     * 用精确/包含匹配而非模糊, 是为了避免把「手机配件」当成「手机」的替代品——
     * 因此名称完全相等的分类优先, 同一关键词最多取 2 个分类。
     */
    private void searchByCategoryName(String keyword, Map<Long, Product> hits) {
        if (keyword == null || keyword.length() < 2) {
            return;
        }
        List<Category> cats = categoryService.listAll(true);
        List<Category> matched = new ArrayList<>();
        for (Category c : cats) {
            if (keyword.equals(c.getName())) {
                matched.add(c);
            }
        }
        if (matched.isEmpty()) {
            for (Category c : cats) {
                if (c.getName() != null && c.getName().contains(keyword)) {
                    matched.add(c);
                    if (matched.size() >= 2) {
                        break;
                    }
                }
            }
        }
        for (Category c : matched) {
            ProductQueryDTO query = new ProductQueryDTO();
            query.setCategoryId(c.getId());
            query.setIncludeChildren(true);   // 一级分类要包含其下二级分类的商品
            query.setStatus(1);
            query.setPageSize(6);
            var page = productService.pageProducts(query);
            for (Product p : page.getRecords()) {
                hits.putIfAbsent(p.getId(), p);
            }
        }
    }

    /**
     * 生成滑窗关键词。
     * <p>
     * 只保留 3-4 字窗口: 2 字窗口会产生「有什」「什么」「推荐」这类噪音词,
     * 它们会匹配到大量无关商品并把真正相关的结果挤出去。
     * 同时对停用词做过滤, 并优先保留更长的窗口(更具区分度)。
     */
    private List<String> slidingKeywords(String text) {
        String cleaned = text.replaceAll("[\\p{Punct}\\s，。！？、]", "");
        java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>();
        for (int len = 4; len >= 3; len--) {
            for (int i = 0; i + len <= cleaned.length(); i++) {
                String sub = cleaned.substring(i, i + len);
                if (isNoiseWord(sub)) {
                    continue;
                }
                result.add(sub);
            }
        }
        return result.stream().limit(12).collect(Collectors.toList());
    }

    /** 判断关键词是否为噪音(纯停用词, 或整体包含在某个停用词里) */
    private boolean isNoiseWord(String word) {
        for (String stop : STOPWORDS) {
            if (stop.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private Integer extractBudget(String text) {
        if (text == null) {
            return null;
        }
        java.util.regex.Matcher matcher = BUDGET_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        for (int g = 1; g <= matcher.groupCount(); g++) {
            String v = matcher.group(g);
            if (v != null && !v.isBlank()) {
                try {
                    return Integer.parseInt(v);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private String composeReply(String text, String intent, List<Product> products) {
        if (products.isEmpty()) {
            return "抱歉, 暂时没有找到符合要求的闲置物品, 你可以换个关键词试试, 比如「沙发」「洗衣机」「婴儿车」。";
        }
        StringBuilder sb = new StringBuilder();
        switch (intent) {
            case "问候" -> sb.append("你好呀! 我是社区二手交易平台的 AI 导购, 可以帮你找闲置、比价格。\n");
            case "性价比" -> sb.append("为你挑了几款价位友好的闲置:\n");
            case "家具" -> sb.append("这几件二手家具成色不错, 你可以在小区内自提:\n");
            case "家电" -> sb.append("这几件二手家电都能正常使用, 建议当场通电试机:\n");
            case "母婴" -> sb.append("这些母婴用品清洗消毒过, 邻里之间转手很合适:\n");
            case "服饰" -> sb.append("这几件二手服饰成色不错, 你可以参考:\n");
            case "数码" -> sb.append("这几件二手数码宝贝性价比可以, 支持当面验机:\n");
            case "图书" -> sb.append("这些图书还在, 价格比买新的便宜很多:\n");
            case "运动" -> sb.append("这些运动器材还能用, 适合捡漏:\n");
            case "宠物" -> sb.append("这些宠物用品清理干净了, 同小区交接很方便:\n");
            default -> sb.append("根据你的需求, 我找到这些闲置物品:\n");
        }
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            sb.append(i + 1).append(". ").append(p.getName())
                    .append(" — ￥").append(p.getPrice());
            if (p.getMerchantName() != null) {
                sb.append(" (").append(p.getMerchantName()).append(")");
            }
            sb.append("\n");
        }
        sb.append("点击下方卡片即可查看详情或直接购买。");
        return sb.toString();
    }

    /**
     * 会话历史
     */
    public List<AiChat> history(Long userId, String sessionId) {
        return aiChatMapper.selectList(Wrappers.<AiChat>lambdaQuery()
                .eq(AiChat::getUserId, userId)
                .eq(sessionId != null && !sessionId.isBlank(), AiChat::getSessionId, sessionId)
                .orderByAsc(AiChat::getCreateTime)
                .last("LIMIT 100"));
    }

    /**
     * 清空会话
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearSession(Long userId, String sessionId) {
        aiChatMapper.delete(Wrappers.<AiChat>lambdaQuery()
                .eq(AiChat::getUserId, userId)
                .eq(AiChat::getSessionId, sessionId));
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.toLowerCase().contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
