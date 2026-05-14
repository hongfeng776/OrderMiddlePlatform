package com.orderplatform.order.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.orderplatform.order.dto.OrderExcelDTO;
import com.orderplatform.order.dto.OrderExportDTO;
import com.orderplatform.order.entity.Order;
import com.orderplatform.order.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class OrderExportService {

    private static final int BATCH_SIZE = 1000;
    private static final String[] STATUS_TEXTS = {"待支付", "已支付", "待发货", "已发货", "已完成", "已取消", "已退款"};
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private MinIOService minIOService;

    public Map<String, Object> exportOrdersToMinIO(OrderExportDTO dto) throws IOException {
        Map<String, Object> queryParams = buildQueryParams(dto);
        
        Long minId = orderMapper.selectMinId(queryParams);
        Long maxId = orderMapper.selectMaxId(queryParams);
        
        if (minId == null || maxId == null) {
            log.warn("没有可导出的订单数据");
            throw new RuntimeException("没有可导出的订单数据");
        }
        
        log.info("开始导出订单，ID范围: [{}, {}]", minId, maxId);

        File tempFile = null;
        ExcelWriter excelWriter = null;
        
        try {
            tempFile = createTempFile();
            excelWriter = EasyExcel.write(tempFile, OrderExcelDTO.class).build();
            WriteSheet writeSheet = EasyExcel.writerSheet("订单列表").build();

            int processedCount = 0;
            long startTime = System.currentTimeMillis();
            long currentStartId = minId;

            while (currentStartId <= maxId) {
                Long currentEndId = Math.min(currentStartId + BATCH_SIZE - 1, maxId);
                queryParams.put("orderIdBegin", currentStartId);
                queryParams.put("orderIdEnd", currentEndId);
                
                List<Order> batchOrders = orderMapper.selectBatchByIdRange(queryParams);
                
                if (!batchOrders.isEmpty()) {
                    List<OrderExcelDTO> excelData = convertToExcelDTO(batchOrders);
                    excelWriter.write(excelData, writeSheet);

                    processedCount += batchOrders.size();

                    if (processedCount % 10000 == 0) {
                        log.info("导出进度: {}，已处理ID范围: [{}, {}]，耗时: {}ms", 
                                processedCount, currentStartId, currentEndId, System.currentTimeMillis() - startTime);
                    }

                    batchOrders.clear();
                    excelData.clear();
                }

                currentStartId = currentEndId + 1;
            }

            excelWriter.finish();
            long fileSize = tempFile.length();
            
            log.info("Excel生成完成，文件大小: {}MB，总耗时: {}ms，实际导出: {} 条", 
                    fileSize / 1024 / 1024, System.currentTimeMillis() - startTime, processedCount);

            byte[] fileData = readFileToByteArray(tempFile);
            String fileName = generateFileName();
            String objectName = minIOService.uploadFile(
                    fileName,
                    fileData,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            );

            String downloadUrl = minIOService.getPresignedDownloadUrl(objectName);

            Map<String, Object> result = new HashMap<>();
            result.put("fileName", fileName);
            result.put("downloadUrl", downloadUrl);
            result.put("totalCount", processedCount);
            result.put("objectName", objectName);
            result.put("fileSize", fileSize);

            log.info("订单导出成功，文件: {}，数量: {}", fileName, processedCount);
            return result;

        } finally {
            if (excelWriter != null) {
                excelWriter.finish();
            }
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                    log.debug("临时文件已删除: {}", tempFile.getAbsolutePath());
                } catch (Exception e) {
                    log.warn("删除临时文件失败: {}", e.getMessage());
                }
            }
        }
    }

    private Map<String, Object> buildQueryParams(OrderExportDTO dto) {
        Map<String, Object> params = new HashMap<>();
        if (dto.getOrderStatus() != null) {
            params.put("orderStatus", dto.getOrderStatus());
        }
        if (dto.getUserId() != null) {
            params.put("userId", dto.getUserId());
        }
        if (dto.getStartTime() != null) {
            params.put("startTime", dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            params.put("endTime", dto.getEndTime());
        }
        return params;
    }

    private File createTempFile() throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        String fileName = "order_export_" + UUID.randomUUID().toString() + ".xlsx";
        File tempFile = new File(tempDir, fileName);
        tempFile.deleteOnExit();
        return tempFile;
    }

    private byte[] readFileToByteArray(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            int offset = 0;
            int bytesRead;
            while (offset < data.length && (bytesRead = fis.read(data, offset, data.length - offset)) != -1) {
                offset += bytesRead;
            }
            return data;
        }
    }

    private List<OrderExcelDTO> convertToExcelDTO(List<Order> orders) {
        List<OrderExcelDTO> result = new ArrayList<>(orders.size());
        for (Order order : orders) {
            OrderExcelDTO dto = new OrderExcelDTO();
            dto.setOrderNo(order.getOrderNo());
            dto.setUserId(order.getUserId());
            dto.setTotalAmount(order.getTotalAmount() != null ? order.getTotalAmount().toString() : "");
            dto.setPayAmount(order.getPayAmount() != null ? order.getPayAmount().toString() : "");
            int status = order.getOrderStatus() != null ? order.getOrderStatus() : 0;
            dto.setOrderStatus(status < STATUS_TEXTS.length ? STATUS_TEXTS[status] : "未知");
            dto.setReceiverName(order.getReceiverName() != null ? order.getReceiverName() : "");
            dto.setReceiverPhone(order.getReceiverPhone() != null ? order.getReceiverPhone() : "");
            dto.setReceiverAddress(order.getReceiverAddress() != null ? order.getReceiverAddress() : "");
            dto.setRemark(order.getRemark() != null ? order.getRemark() : "");
            dto.setCreateTime(order.getCreateTime() != null ? order.getCreateTime().format(FORMATTER) : "");
            result.add(dto);
        }
        return result;
    }

    private String generateFileName() {
        LocalDateTime now = LocalDateTime.now();
        return String.format("订单导出_%s.xlsx",
                now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
    }
}
