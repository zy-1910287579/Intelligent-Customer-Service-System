package com.storm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storm.entity.Ticket;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Storm
 * @since 2026-03-31
 */
public interface TicketService extends IService<Ticket> {

    boolean createSupportTicket(Ticket ticket);

}
