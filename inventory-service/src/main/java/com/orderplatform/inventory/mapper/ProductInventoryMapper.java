package com.orderplatform.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orderplatform.inventory.entity.ProductInventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductInventoryMapper extends BaseMapper<ProductInventory> {

    @Update("UPDATE product_inventory SET stock_num = stock_num - #{count}, lock_stock = lock_stock + #{count} " +
            "WHERE product_id = #{productId} AND stock_num >= #{count} AND status = 1")
    int lockStock(@Param("productId") Long productId, @Param("count") Integer count);

    @Update("UPDATE product_inventory SET lock_stock = lock_stock - #{count} " +
            "WHERE product_id = #{productId} AND lock_stock >= #{count}")
    int unlockStock(@Param("productId") Long productId, @Param("count") Integer count);

    @Update("UPDATE product_inventory SET stock_num = stock_num - #{count}, lock_stock = lock_stock + #{count} " +
            "WHERE product_id = #{productId} AND stock_num >= #{count} AND status = 1")
    int preDeductStock(@Param("productId") Long productId, @Param("count") Integer count);

    @Update("UPDATE product_inventory SET lock_stock = lock_stock - #{count} " +
            "WHERE product_id = #{productId} AND lock_stock >= #{count}")
    int confirmDeductStock(@Param("productId") Long productId, @Param("count") Integer count);

    @Update("UPDATE product_inventory SET stock_num = stock_num + #{count}, lock_stock = lock_stock - #{count} " +
            "WHERE product_id = #{productId} AND lock_stock >= #{count}")
    int rollbackStock(@Param("productId") Long productId, @Param("count") Integer count);
}
