package com.storm.tools;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storm.entity.Ticket;
import com.storm.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;

/**
 * 工单相关的 AI 工具类
 * 参考文档：Methods as Tools - Taking Actions
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class TicketAssistanceTools {

    private final TicketService ticketService;

    /**
     * 工具方法 1: 为用户创建一个新的支持工单
     * 参考文档：@Tool 注解的使用
     */
    @Tool(description = "为指定用户创建一个新的客服工单，记录问题类别和详细描述")
    public String createSupportTicket(
            @ToolParam(description = "提交工单的用户ID") String userId,
            @ToolParam(description = "工单的类别，例如: 售后问题, 物流问题, 产品质量") String category,
            @ToolParam(description = "工单的详细描述") String description,
            @ToolParam(description = "工单关联的订单ID，如果没有则为空字符串") String orderId) {

        // 1. 参数校验
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(category) || !StringUtils.hasText(description)) {
            return "创建工单失败：用户ID、工单类别和描述不能为空";
        }

        // 2. 创建工单对象
        Ticket ticket = new Ticket();
        ticket.setUserId(userId);
        ticket.setCategory(category);
        ticket.setDescription(description);
        // 如果提供了订单ID，则关联
        if (StringUtils.hasText(orderId)) {
            ticket.setOrderId(orderId);
        }
        ticket.setTitle(generateTitleFromDescription(description)); // 自动生成一个标题
        ticket.setStatus(1); // 默认状态：待处理
        ticket.setPriority(1); // 默认优先级：低
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        // 3. 调用 Service 层创建工单
        // 注意：这个完整的业务逻辑应该在 TicketServiceImpl 中实现
        boolean success = ticketService.createSupportTicket(ticket);

        // 4. 返回结果给 AI
        if (success) {
            try {
                // 假设 Service 成功后设置了 ticketId，将其返回
                return "工单创建成功。工单号: " + ticket.getTicketId() + "。我们会尽快为您处理。";
            } catch (Exception e) {
                // 如果返回 ticketId 有问题，至少告知成功
                return "工单创建成功。我们会尽快为您处理。";
            }
        } else {
            return "工单创建失败，可能由于系统繁忙，请稍后再试。";
        }
    }

    // 辅助方法：根据描述生成一个简短的标题
    private String generateTitleFromDescription(String description) {
        // 取描述的前 20 个字符作为标题，如果不足则全部使用
        return description.substring(0, Math.min(description.length(), 20));
    }
}