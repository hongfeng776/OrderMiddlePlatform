package com.orderplatform.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orderplatform.inventory.entity.InventoryLockRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface InventoryLockRecordMapper extends BaseMapper<InventoryLockRecord> {

    @Select("SELECT * FROM inventory_lock_record WHERE order_no = #{orderNo} AND lock_status = 0")
    List<InventoryLockRecord> selectLockingRecordsByOrderNo(@Param("orderNo") String orderNo);

    @Select("SELECT * FROM inventory_lock_record WHERE product_id = #{productId} AND lock_status = 0")
    List<InventoryLockRecord> selectLockingRecordsByProductId(@Param("productId") Long productId);

    @Select("SELECT * FROM inventory_lock_record WHERE lock_status = 0 AND lock_expire_time <= #{now}")
    List<InventoryLockRecord> selectExpiredLockingRecords(@Param("now") LocalDateTime now);

    @Update("UPDATE inventory_lock_record SET lock_status = 1, confirm_time = #{confirmTime}, update_time = #{confirmTime} " +
            "WHERE id = #{id} AND lock_status = 0")
    int confirmLock(@Param("id") Long id, @Param("confirmTime") LocalDateTime confirmTime);

    @Update("UPDATE inventory_lock_record SET lock_status = 2, release_time = #{releaseTime}, " +
            "release_type = #{releaseType}, operator_id = #{operatorId}, remark = #{remark}, update_time = #{releaseTime} " +
            "WHERE id = #{id} AND lock_status = 0")
    int releaseLock(@Param("id") Long id, @Param("releaseTime") LocalDateTime releaseTime,
                    @Param("releaseType") Integer releaseType, @Param("operatorId") Long operatorId,
                    @Param("remark") String remark);
}
