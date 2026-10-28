package com.mall.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 占位图接口。
 * <p>
 * 商城种子数据需要商品图/轮播图/头像, 为了不依赖外网图床, 这里用 SVG 动态生成占位图,
 * 形如: /api/public/placeholder.svg?text=寻喜卫衣&w=400&h=400&bg=ff7a45
 * 线上接入真实图片后可直接替换为 OSS/CDN 地址。
 */
@RestController
@RequestMapping("/public")
public class PlaceholderController {

    private static final String[] PALETTE = {
            "#ff7a45", "#36cfc9", "#597ef7", "#9254de", "#f759ab",
            "#faad14", "#13c2c2", "#eb2f96", "#52c41a", "#2f54eb"
    };

    @GetMapping("/placeholder.svg")
    public ResponseEntity<byte[]> placeholder(@RequestParam(defaultValue = "商品") String text,
                                              @RequestParam(defaultValue = "400") int w,
                                              @RequestParam(defaultValue = "400") int h,
                                              @RequestParam(required = false) String bg) {
        int width = Math.min(Math.max(w, 40), 1600);
        int height = Math.min(Math.max(h, 40), 1600);
        String color = (bg == null || bg.isBlank()) ? pickColor(text) : normalize(bg);
        String safeText = escape(text);
        int fontSize = Math.max(14, Math.min(width, height) / 9);

        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" viewBox="0 0 %d %d">
                  <defs>
                    <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0%%" stop-color="%s" stop-opacity="0.95"/>
                      <stop offset="100%%" stop-color="%s" stop-opacity="0.65"/>
                    </linearGradient>
                  </defs>
                  <rect width="100%%" height="100%%" fill="url(#g)"/>
                  <circle cx="%d" cy="%d" r="%d" fill="#ffffff" fill-opacity="0.10"/>
                  <circle cx="%d" cy="%d" r="%d" fill="#ffffff" fill-opacity="0.08"/>
                  <text x="50%%" y="50%%" fill="#ffffff" font-size="%d" font-weight="600"
                        font-family="'Microsoft YaHei','PingFang SC',sans-serif"
                        text-anchor="middle" dominant-baseline="central">%s</text>
                </svg>
                """.formatted(width, height, width, height, color, color,
                (int) (width * 0.78), (int) (height * 0.22), (int) (width * 0.16),
                (int) (width * 0.18), (int) (height * 0.85), (int) (width * 0.22),
                fontSize, safeText);

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(svg.getBytes(StandardCharsets.UTF_8));
    }

    private String pickColor(String text) {
        if (text == null || text.isEmpty()) {
            return PALETTE[0];
        }
        int hash = Math.abs(text.hashCode());
        return PALETTE[hash % PALETTE.length];
    }

    /** 允许 ff7a45 或 #ff7a45 两种写法 */
    private String normalize(String bg) {
        String value = bg.trim();
        if (value.matches("^[0-9a-fA-F]{6}$")) {
            return "#" + value;
        }
        if (value.matches("^#[0-9a-fA-F]{6}$")) {
            return value;
        }
        return PALETTE[0];
    }

    private String escape(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
