package com.storm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storm.entity.User;
import com.storm.mapper.UserMapper;
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
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements com.storm.service.UserService {
    //这里的“查询-计算-更新”过程，在高并发场景下，会产生一个经典的“竞态条件”问题。
    //这个问题需要通过事务隔离和乐观锁或悲观锁来解决。这已经超出了 MP 通用方法的能力范围，是需要在 Service 层精心设计的业务逻辑。
    // ... 其他方法 ...
    @Override
    @Transactional // 重要：确保操作的原子性
    public boolean updateUserPoints(String userId, int pointsChange) {
        User existingUser = this.getById(userId);
        if (existingUser == null) {
            return false; // 用户不存在
        }

        // 计算新积分（防止积分变为负数）
        long newPoints = Math.max(0, existingUser.getPointsBalance() + pointsChange);
        existingUser.setPointsBalance(newPoints);

        // 更新数据库
        return this.updateById(existingUser);
    }

}
