<template>
  <div class="refunds-page">
    <div class="container">
      <el-card v-loading="loading">
        <template #header>
          <div class="card-header">
            <span>我的退款</span>
            <el-button type="success" size="small" @click="exportRefundList">
              导出Excel
            </el-button>
          </div>
        </template>

        <el-table :data="refunds" stripe style="width: 100%">
          <el-table-column prop="refundNo" label="退款单号" min-width="180" />
          <el-table-column prop="orderNo" label="订单编号" min-width="180" />
          <el-table-column prop="payNo" label="支付单号" min-width="180" />
          <el-table-column prop="refundAmount" label="退款金额" width="120">
            <template #default="{ row }">
              <span class="amount">¥{{ row.refundAmount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="refundReason" label="退款原因" min-width="150" />
          <el-table-column prop="refundStatus" label="退款状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.refundStatus)" size="small">
                {{ getRefundStatusText(row.refundStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="申请时间" width="180" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="viewProgress(row.refundNo)">
                查看进度
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="refunds.length === 0" description="暂无退款记录" />
      </el-card>
    </div>

    <el-dialog
      v-model="progressDialogVisible"
      title="退款进度"
      width="700px"
    >
      <div v-if="progress" class="progress-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="退款单号">{{ progress.refundNo }}</el-descriptions-item>
          <el-descriptions-item label="订单编号">{{ progress.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="退款金额">
            <span class="amount">¥{{ progress.refundAmount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="退款状态">
            <el-tag :type="getStatusType(progress.refundStatus)" size="small">
              {{ progress.refundStatusDesc }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="退款原因" :span="2">{{ progress.refundReason }}</el-descriptions-item>
          <el-descriptions-item v-if="progress.refundVouchers && progress.refundVouchers.length > 0" label="退款凭证" :span="2">
            <div class="voucher-list">
              <img
                v-for="(voucher, index) in progress.refundVouchers"
                :key="index"
                :src="voucher"
                class="voucher-image"
                @click="previewImage(voucher)"
              />
            </div>
          </el-descriptions-item>
          <el-descriptions-item v-if="progress.auditRemark" label="审核备注" :span="2">
            {{ progress.auditRemark }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="progress-timeline">
          <h4>退款进度</h4>
          <el-timeline>
            <el-timeline-item
              v-for="(step, index) in progress.progressSteps"
              :key="index"
              :timestamp="step.time"
              :type="getStepType(step.status)"
            >
              <div class="timeline-content">
                <span class="step-name">{{ step.stepName }}</span>
                <span v-if="step.remark" class="step-remark">{{ step.remark }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
    </el-dialog>

    <el-image-viewer
      v-if="imageViewerVisible"
      :url-list="[currentPreviewImage]"
      :initial-index="0"
      @close="imageViewerVisible = false"
    />
  </div>
</template>

<script>
import { refundApi } from '@/api'
import store from '@/store'

export default {
  name: 'Refunds',
  data() {
    return {
      loading: false,
      refunds: [],
      progressDialogVisible: false,
      progress: null,
      imageViewerVisible: false,
      currentPreviewImage: ''
    }
  },
  mounted() {
    this.loadRefunds()
  },
  methods: {
    async loadRefunds() {
      this.loading = true
      try {
        const res = await refundApi.getUserRefunds(store.state.user.id)
        if (res.code === 200) {
          this.refunds = res.data || []
        }
      } catch (e) {
        this.$message.error('加载退款记录失败')
      } finally {
        this.loading = false
      }
    },
    async viewProgress(refundNo) {
      this.progress = null
      this.progressDialogVisible = true
      try {
        const res = await refundApi.getProgress(refundNo, store.state.user.id)
        if (res.code === 200) {
          this.progress = res.data
        }
      } catch (e) {
        this.$message.error('加载退款进度失败')
      }
    },
    previewImage(url) {
      this.currentPreviewImage = url
      this.imageViewerVisible = true
    },
    getRefundStatusText(status) {
      const map = {
        0: '待审核',
        1: '审核通过',
        2: '审核拒绝',
        3: '退款中',
        4: '退款成功',
        5: '退款失败'
      }
      return map[status] || '未知'
    },
    getStatusType(status) {
      const map = {
        0: 'warning',
        1: 'primary',
        2: 'danger',
        3: 'info',
        4: 'success',
        5: 'danger'
      }
      return map[status] || 'info'
    },
    getStepType(status) {
      const map = {
        completed: 'success',
        processing: 'primary',
        failed: 'danger'
      }
      return map[status] || 'info'
    },
    async exportRefundList() {
      try {
        const res = await refundApi.export(this.$store.state.user.id)
        const blob = new Blob([res], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = `退款记录_${new Date().getTime()}.xlsx`
        link.click()
        window.URL.revokeObjectURL(url)
        this.$message.success('导出成功')
      } catch (e) {
        this.$message.error('导出失败')
      }
    }
  }
}
</script>

<style scoped>
.refunds-page {
  padding: 20px 0;
  background: #f5f7fa;
  min-height: 100vh;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.card-header {
  font-size: 18px;
  font-weight: 500;
}

.amount {
  color: #f56c6c;
  font-weight: 500;
}

.progress-content {
  padding: 10px 0;
}

.voucher-list {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.voucher-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #e4e7ed;
  transition: transform 0.3s;
}

.voucher-image:hover {
  transform: scale(1.05);
}

.progress-timeline {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e4e7ed;
}

.progress-timeline h4 {
  margin-bottom: 16px;
  color: #303133;
}

.timeline-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.step-name {
  font-weight: 500;
  color: #303133;
}

.step-remark {
  font-size: 12px;
  color: #909399;
}
</style>
