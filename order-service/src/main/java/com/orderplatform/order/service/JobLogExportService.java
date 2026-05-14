package com.orderplatform.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.orderplatform.order.entity.ScheduleJobLog;
import com.orderplatform.order.mapper.ScheduleJobLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class JobLogExportService {

    @Autowired
    private ScheduleJobLogMapper scheduleJobLogMapper;

    public void exportLogs(String jobHandler, Integer status, HttpServletResponse response) throws IOException {
        LambdaQueryWrapper<ScheduleJobLog> wrapper = new LambdaQueryWrapper<>();
        if (jobHandler != null && !jobHandler.isEmpty()) {
            wrapper.eq(ScheduleJobLog::getJobHandler, jobHandler);
        }
        if (status != null) {
            wrapper.eq(ScheduleJobLog::getStatus, status);
        }
        wrapper.orderByDesc(ScheduleJobLog::getExecuteTime);
        List<ScheduleJobLog> logs = scheduleJobLogMapper.selectList(wrapper);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("任务执行日志");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        String[] headers = {
                "ID", "任务名称", "任务处理器", "任务参数", "执行状态",
                "执行结果", "执行时间", "耗时(ms)", "分片索引", "分片总数",
                "执行器地址", "重试次数", "告警状态", "告警时间", "错误信息"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        int rowNum = 1;
        for (ScheduleJobLog log : logs) {
            Row row = sheet.createRow(rowNum++);

            createCell(row, 0, log.getId(), dataStyle);
            createCell(row, 1, log.getJobName(), dataStyle);
            createCell(row, 2, log.getJobHandler(), dataStyle);
            createCell(row, 3, log.getJobParam(), dataStyle);
            createCell(row, 4, log.getStatus() == 1 ? "成功" : "失败", dataStyle);
            createCell(row, 5, log.getExecuteResult(), dataStyle);
            createCell(row, 6, log.getExecuteTime() != null ? log.getExecuteTime().format(formatter) : "", dataStyle);
            createCell(row, 7, log.getDuration(), dataStyle);
            createCell(row, 8, log.getShardIndex(), dataStyle);
            createCell(row, 9, log.getShardTotal(), dataStyle);
            createCell(row, 10, log.getExecutorAddress(), dataStyle);
            createCell(row, 11, log.getRetryCount(), dataStyle);
            createCell(row, 12, getAlertStatusText(log.getAlertStatus()), dataStyle);
            createCell(row, 13, log.getAlertTime() != null ? log.getAlertTime().format(formatter) : "", dataStyle);
            createCell(row, 14, log.getErrorMessage(), dataStyle);
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            if (width < 3000) {
                sheet.setColumnWidth(i, 3000);
            } else if (width > 15000) {
                sheet.setColumnWidth(i, 15000);
            }
        }

        String fileName = "任务执行日志_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);

        OutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();
        workbook.close();

        log.info("任务日志导出完成, 共导出{}条记录", logs.size());
    }

    private void createCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
        cell.setCellStyle(style);
    }

    private String getAlertStatusText(Integer alertStatus) {
        if (alertStatus == null) return "未告警";
        switch (alertStatus) {
            case 1: return "已告警";
            case 2: return "告警失败";
            default: return "未告警";
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);

        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }
}
