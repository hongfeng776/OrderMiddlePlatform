package com.orderplatform.order.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

@Data
@HeadRowHeight(20)
@ContentRowHeight(18)
@ColumnWidth(20)
public class OrderExcelDTO {

    @ExcelProperty("订单编号")
    @ColumnWidth(30)
    private String orderNo;

    @ExcelProperty("用户ID")
    private Long userId;

    @ExcelProperty("订单金额")
    @ColumnWidth(15)
    private String totalAmount;

    @ExcelProperty("实付金额")
    @ColumnWidth(15)
    private String payAmount;

    @ExcelProperty("订单状态")
    @ColumnWidth(12)
    private String orderStatus;

    @ExcelProperty("收货人")
    @ColumnWidth(15)
    private String receiverName;

    @ExcelProperty("收货电话")
    @ColumnWidth(15)
    private String receiverPhone;

    @ExcelProperty("收货地址")
    @ColumnWidth(40)
    private String receiverAddress;

    @ExcelProperty("备注")
    @ColumnWidth(30)
    private String remark;

    @ExcelProperty("创建时间")
    @ColumnWidth(25)
    private String createTime;
}
