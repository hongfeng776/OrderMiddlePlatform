package com.orderplatform.payment.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.math.BigDecimal;

@Data
@HeadRowHeight(20)
@ContentRowHeight(18)
@ColumnWidth(25)
public class RefundExportVO {

    @ExcelProperty("退款单号")
    private String refundNo;

    @ExcelProperty("订单编号")
    private String orderNo;

    @ExcelProperty("支付单号")
    private String payNo;

    @ExcelProperty("用户ID")
    private Long userId;

    @ExcelProperty("退款金额")
    private BigDecimal refundAmount;

    @ExcelProperty("退款原因")
    private String refundReason;

    @ExcelProperty("退款状态")
    private String refundStatusText;

    @ExcelProperty("审核备注")
    private String auditRemark;

    @ExcelProperty("退款时间")
    private String refundTime;

    @ExcelProperty("申请时间")
    private String createTime;
}
