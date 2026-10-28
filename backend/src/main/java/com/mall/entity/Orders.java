package com.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 订单 */
@Data
@TableName("orders")
public class Orders {

    /** 待付款 */
    public static final int STATUS_UNPAID = 0;
    /** 待发货 */
    public static final int STATUS_UNDELIVERED = 1;
    /** 待收货 */
    public static final int STATUS_UNRECEIVED = 2;
    /** 已完成 */
    public static final int STATUS_FINISHED = 3;
    /** 已取消 */
    public static final int STATUS_CANCELED = 4;
    /** 已退款 */
    public static final int STATUS_REFUNDED = 5;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long merchantId;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    private BigDecimal freight;

    /** 0待付款 1待发货 2待收货 3已完成 4已取消 5已退款 */
    private Integer status;

    /** 1余额 2支付宝 3微信 */
    private Integer payType;

    private LocalDateTime payTime;

    private LocalDateTime deliveryTime;

    private LocalDateTime receiveTime;

    private LocalDateTime finishTime;

    private LocalDateTime cancelTime;

    private String cancelReason;

    /** 收货信息快照 */
    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private String remark;

    private String expressNo;

    @TableLogic
    @TableField(select = false)
    private Integer delFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ---------- 非数据库字段 ----------

    @TableField(exist = false)
    private List<OrderItem> items;

    @TableField(exist = false)
    private String storeName;

    /** 状态中文描述 */
    @TableField(exist = false)
    private String statusText;

    public String getStatusText() {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case STATUS_UNPAID -> "待付款";
            case STATUS_UNDELIVERED -> "待发货";
            case STATUS_UNRECEIVED -> "待收货";
            case STATUS_FINISHED -> "已完成";
            case STATUS_CANCELED -> "已取消";
            case STATUS_REFUNDED -> "已退款";
            default -> "未知状态";
        };
    }
}
