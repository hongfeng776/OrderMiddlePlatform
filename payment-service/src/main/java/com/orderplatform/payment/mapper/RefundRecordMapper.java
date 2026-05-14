package com.orderplatform.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orderplatform.payment.entity.RefundRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RefundRecordMapper extends BaseMapper<RefundRecord> {
}
