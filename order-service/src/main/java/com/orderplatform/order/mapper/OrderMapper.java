package com.orderplatform.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orderplatform.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Update("<script>" +
            "UPDATE `order` SET order_status = #{status}, ship_time = #{shipTime}, update_time = #{shipTime} " +
            "WHERE id IN " +
            "<foreach collection='orderNos' item='orderNo' open='(' separator=',' close=')'>" +
            "#{orderNo}" +
            "</foreach>" +
            "</script>")
    int batchUpdateShipStatus(@Param("orderNos") List<String> orderNos, 
                               @Param("status") Integer status, 
                               @Param("shipTime") LocalDateTime shipTime);

    @Update("<script>" +
            "UPDATE `order` SET order_status = #{status}, cancel_time = #{cancelTime}, update_time = #{cancelTime} " +
            "WHERE id IN " +
            "<foreach collection='orderNos' item='orderNo' open='(' separator=',' close=')'>" +
            "#{orderNo}" +
            "</foreach>" +
            "</script>")
    int batchUpdateCancelStatus(@Param("orderNos") List<String> orderNos, 
                                 @Param("status") Integer status, 
                                 @Param("cancelTime") LocalDateTime cancelTime);

    @Select("<script>" +
            "SELECT MIN(id) FROM `order` " +
            "<where>" +
            "<if test='orderStatus != null'> AND order_status = #{orderStatus} </if>" +
            "<if test='userId != null'> AND user_id = #{userId} </if>" +
            "<if test='startTime != null'> AND create_time >= #{startTime} </if>" +
            "<if test='endTime != null'> AND create_time <= #{endTime} </if>" +
            "</where>" +
            "</script>")
    Long selectMinId(Map<String, Object> params);

    @Select("<script>" +
            "SELECT MAX(id) FROM `order` " +
            "<where>" +
            "<if test='orderStatus != null'> AND order_status = #{orderStatus} </if>" +
            "<if test='userId != null'> AND user_id = #{userId} </if>" +
            "<if test='startTime != null'> AND create_time >= #{startTime} </if>" +
            "<if test='endTime != null'> AND create_time <= #{endTime} </if>" +
            "</where>" +
            "</script>")
    Long selectMaxId(Map<String, Object> params);

    @Select("<script>" +
            "SELECT * FROM `order` " +
            "<where>" +
            " AND id >= #{orderIdBegin} AND id <= #{orderIdEnd} " +
            "<if test='orderStatus != null'> AND order_status = #{orderStatus} </if>" +
            "<if test='userId != null'> AND user_id = #{userId} </if>" +
            "<if test='startTime != null'> AND create_time >= #{startTime} </if>" +
            "<if test='endTime != null'> AND create_time <= #{endTime} </if>" +
            "</where>" +
            " ORDER BY id ASC " +
            "</script>")
    List<Order> selectBatchByIdRange(Map<String, Object> params);
}
