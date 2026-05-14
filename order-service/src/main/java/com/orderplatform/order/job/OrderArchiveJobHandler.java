package com.orderplatform.order.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.orderplatform.order.entity.Order;
import com.orderplatform.order.entity.OrderArchive;
import com.orderplatform.order.mapper.OrderArchiveMapper;
import com.orderplatform.order.service.OrderService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class OrderArchiveJobHandler extends AbstractJobHandler {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderArchiveMapper orderArchiveMapper;

    @Value("${order.archive-days:90}")
    private int archiveDays;

    @Override
    protected String getJobName() {
        return "订单数据归档";
    }

    @Override
    protected String getJobHandler() {
        return "orderArchiveJobHandler";
    }

    @Override
    protected void doExecute(String param, int shardIndex, int shardTotal) throws Exception {
        int days = archiveDays;
        if (param != null && !param.isEmpty()) {
            try {
                days = Integer.parseInt(param);
            } catch (NumberFormatException e) {
                log.warn("归档天数参数格式错误，使用默认值: {}", param);
            }
        }

        LocalDateTime archiveTime = LocalDateTime.now().minusDays(days);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .in(Order::getOrderStatus, 4, 5, 6)
                .lt(Order::getUpdateTime, archiveTime);

        if (shardTotal > 1) {
            wrapper.apply("MOD(id, {0}) = {1}", shardTotal, shardIndex);
        }

        List<Order> ordersToArchive = orderService.list(wrapper);

        if (ordersToArchive.isEmpty()) {
            return;
        }

        int batchSize = 100;
        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < ordersToArchive.size(); i += batchSize) {
            int end = Math.min(i + batchSize, ordersToArchive.size());
            List<Order> batch = ordersToArchive.subList(i, end);

            try {
                String batchNo = "BATCH_" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                        + "_" + shardIndex + "_" + i;

                for (Order order : batch) {
                    OrderArchive archive = new OrderArchive();
                    BeanUtils.copyProperties(order, archive);
                    archive.setId(null);
                    archive.setArchiveTime(LocalDateTime.now());
                    archive.setArchiveBatch(batchNo);
                    orderArchiveMapper.insert(archive);
                    orderService.removeById(order.getId());
                }

                successCount += batch.size();
                log.info("分片[{}] 批次归档成功: 数量={}, 范围={}-{}", shardIndex, batch.size(), i, end - 1);

            } catch (Exception e) {
                log.error("分片[{}] 批次归档失败: 范围={}-{}", shardIndex, i, end - 1, e);
                failCount += batch.size();
            }
        }

        String result = String.format("分片[%d/%d] 归档完成: 总数=%d, 成功=%d, 失败=%d",
                shardIndex, shardTotal, ordersToArchive.size(), successCount, failCount);
        log.info(result);
    }

    @XxlJob("orderArchiveJobHandler")
    public void executeJob() {
        super.execute();
    }
}
