package com.orderplatform.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orderplatform.payment.entity.CallbackLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CallbackLogMapper extends BaseMapper<CallbackLog> {
}
