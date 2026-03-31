package com.storm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author Storm
 * @since 2026-03-31
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_user")
@ApiModel(value="User对象", description="")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID (主键)
     */
    @TableId(value = "user_id", type = IdType.ASSIGN_ID) //
    private String userId;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 手机号码
     */
    @TableField("phone_number")
    private String phoneNumber;

    /**
     * 电子邮箱
     */
    @TableField("email")
    private String email;

    /**
     * VIP等级 (0: 普通用户, 1: 青铜, 2: 白银, 3: 黄金 ...)
     */
    @TableField("vip_level")
    private Integer vipLevel;

    /**
     * 积分余额
     */
    @TableField("points_balance")
    private Long pointsBalance;

    /**
     * 用户创建时间
     * 注意：由于数据库为 TIMESTAMPTZ 类型，此处暂时使用 Object 类型以绕开时区转换问题。
     * 待后续配置解决后，可改为 LocalDateTime。
     */
    @TableField("created_at")
    private Object createdAt;

    /**
     * 用户信息更新时间
     * 注意：由于数据库为 TIMESTAMPTZ 类型，此处暂时使用 Object 类型以绕开时区转换问题。
     * 待后续配置解决后，可改为 LocalDateTime。
     */
    @TableField("updated_at")
    private Object updatedAt;


}
