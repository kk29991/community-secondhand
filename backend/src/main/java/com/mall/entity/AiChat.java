package com.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** AI 导购对话记录 */
@Data
@TableName("ai_chat")
public class AiChat {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 同一次会话共用 */
    private String sessionId;

    /** user / assistant */
    private String role;

    private String content;

    /** 识别到的意图 */
    private String intent;

    /** 推荐商品ID, 逗号分隔 */
    private String refProductIds;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
