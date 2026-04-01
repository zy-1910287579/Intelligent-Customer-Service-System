package com.storm.tools;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storm.entity.User;
import com.storm.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 用户相关的 AI 工具类
 * 参考文档：Methods as Tools - Declarative Specification: @Tool
 */

@RequiredArgsConstructor
@Slf4j
@Component
public class UserAssistanceTools {
    private final UserService userService;
    private final ObjectMapper objectMapper; // 使用 Jackson
    /**
     * 工具方法 1: 根据用户ID查询用户详细信息
     * 参考文档：@Tool 注解的使用
     */
    @Tool(description = "根据用户ID查询用户的详细信息，包括会员等级和积分余额")
    public String queryUserInfo(@ToolParam(description = "用户的唯一ID") String userId) {
        if (!StringUtils.hasText(userId)) {
            return "用户ID不能为空";
        }

        // 使用 MP 的 getById 方法查询
        User user = userService.getById(userId);

        if (user == null) {
            return "未找到用户ID为 " + userId + " 的用户";
        }

        // 使用 Jackson 将对象转为 JSON 字符串
        try {
            return objectMapper.writeValueAsString(user);
        } catch (JsonProcessingException e) {
            return "用户数据序列化失败";
        }
    }

    /**
     * 工具方法 2: 根据用户ID更新用户积分
     * 参考文档：Taking Actions - Methods that Return a Value
     */
    @Tool(description = "为指定用户增加或扣除积分，正数为增加，负数为扣除")
    public String updateUserPoints(
            @ToolParam(description = "用户的唯一ID") String userId,
            @ToolParam(description = "要变更的积分数额，正数为增加，负数为扣除") Integer pointsChange) {

        if (!StringUtils.hasText(userId) || pointsChange == null) {
            return "用户ID和积分数额都不能为空";
        }

        // 使用 MP 的 Service 进行业务逻辑处理
        // 注意：这里应该是一个完整的业务方法，而不是直接更新。为了演示，我们调用一个假设的 service 方法。
        // 你需要在 UserServiceImpl 中实现这个逻辑，包括事务处理和幂等性检查。
        boolean success = userService.updateUserPoints(userId, pointsChange);

        if (success) {
            return "用户ID为 " + userId + " 的积分已成功变更 " + pointsChange + " 分";
        } else {
            return "用户积分变更失败，可能用户不存在或发生其他错误";
        }
    }
}
