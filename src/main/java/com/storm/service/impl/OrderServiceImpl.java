package com.storm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storm.entity.Order;
import com.storm.mapper.OrderMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Storm
 * @since 2026-03-31
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements com.storm.service.OrderService {

}
