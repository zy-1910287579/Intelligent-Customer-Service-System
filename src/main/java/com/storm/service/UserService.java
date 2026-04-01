package com.storm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storm.entity.User;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Storm
 * @since 2026-03-31
 */
public interface UserService extends IService<User> {

    boolean updateUserPoints(String userId, int pointsChange);

}
