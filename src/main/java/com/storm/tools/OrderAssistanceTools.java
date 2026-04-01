package com.storm.tools;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storm.entity.Order;
import com.storm.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Component
public class OrderAssistanceTools {
    // 1. 注入你的业务 Service (方案一：解耦调用)
    private final OrderService orderService;

    private final ObjectMapper objectMapper; // 使用 Jackson 替代 FastJson

    /**
     * 工具方法 1: 根据订单ID查询订单详情
     * 参考文档：@Tool 注解的使用
     */
    /**
     * 工具方法 1: 根据订单ID查询订单详情
     * 参考文档：@Tool 注解的使用
     */
    @Tool(description = "根据订单ID查询详细的订单信息，包括商品、价格和状态")
    public String queryOrderDetail(@ToolParam(description = "订单的唯一ID") String orderId) {
        if (!StringUtils.hasText(orderId)) {
            return "订单ID不能为空";
        }

        // 使用 MP 的 getById 方法，简单高效
        Order order = orderService.getById(orderId);

        if (order == null) {
            return "未找到订单ID为 " + orderId + " 的订单";
        }

        // 使用 Jackson 将对象转为 JSON 字符串
        try {
            return objectMapper.writeValueAsString(order);
        } catch (JsonProcessingException e) {
            return "订单数据序列化失败";
        }
    }

    /**
     * 工具方法 2: 根据用户ID查询用户的订单列表
     * 参考文档：@Tool 注解的使用
     */
    @Tool(description = "根据用户ID查询该用户的所有订单记录")
    public String queryUserOrders(@ToolParam(description = "用户的唯一ID") String userId) {
        if (!StringUtils.hasText(userId)) {
            return "用户ID不能为空";
        }

        // 使用 MP 的 LambdaQueryWrapper 构建查询条件
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreatedAt); // 按创建时间倒序排列<websource>source_group_web_7</websource>

        List<Order> orders = orderService.list(wrapper);

        if (orders.isEmpty()) {
            return "用户ID为 " + userId + " 的用户没有订单";
        }

        // 使用 Jackson 将列表转为 JSON 字符串
        try {
            return objectMapper.writeValueAsString(orders);
        } catch (JsonProcessingException e) {
            return "订单列表数据序列化失败";
        }
    }

}
