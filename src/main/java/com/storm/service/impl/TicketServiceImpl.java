package com.storm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storm.entity.Ticket;
import com.storm.mapper.TicketMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Storm
 * @since 2026-03-31
 */
@Service
public class TicketServiceImpl extends ServiceImpl<TicketMapper, Ticket> implements com.storm.service.TicketService {

    @Override
    @Transactional // 重要：确保操作的原子性，如果失败则回滚
    public boolean createSupportTicket(Ticket ticket) {
        // 这里可以加入更多的业务逻辑
        // 例如：校验用户是否存在、校验订单ID是否属于该用户等

        // 调用 MP 的 save 方法保存工单
        return this.save(ticket);
    }

}
