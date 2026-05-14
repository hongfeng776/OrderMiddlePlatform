package com.orderplatform.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.orderplatform.common.exception.BusinessException;
import com.orderplatform.common.result.ResultCode;
import com.orderplatform.user.entity.User;
import com.orderplatform.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    public User login(String username, String password) {
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getStatus, 1));
        
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        
        if (!password.equals(user.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }
        
        return user;
    }

    public User getUserById(Long id) {
        return getById(id);
    }
}
