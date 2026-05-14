package com.orderplatform.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orderplatform.order.entity.BatchOperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BatchOperationLogMapper extends BaseMapper<BatchOperationLog> {
}
