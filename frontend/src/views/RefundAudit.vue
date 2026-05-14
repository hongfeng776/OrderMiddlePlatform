<template>
  <div class="refund-audit-page">
    <div class="container">
      <el-card v-loading="loading">
        <template #header>
          <div class="card-header">
            <span>退款审核</span>
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
          <el-table-column prop="createTime" label="申请时间" width="180" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button 
                type="success" 
                size="small" 
                @click="openAuditDialog(row, true)"
              >
                通过
              </el-button>
              <el-button 
                type="danger" 
                size="small" 
                @click="openAuditDialog(row, false)"
              >
                拒绝
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="refunds.length === 0" description="暂无待审核退款" />
      </el-card>
    </div>

    <el-dialog
      v-model="auditDialogVisible"
      :title="auditPass ? '审核通过' : '审核拒绝'"
      width="500px"
      @close="resetAuditForm"
    >
      <el-form :model="auditForm" label-width="100px">
        <el-form-item label="退款单号">
          <span>{{ currentAuditRefund?.refundNo }}</span>
        </el-form-item>
        <el-form-item label="退款金额">
          <span class="amount">¥{{ currentAuditRefund?.refundAmount }}</span>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input
            v-model="auditForm.auditRemark"
            type="textarea"
            :rows="4"
            placeholder="请输入审核备注"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditing" @click="submitAudit">
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { refundApi } from '@/api'
import store from '@/store'

export default {
  name: 'RefundAudit',
  data() {
    return {
      loading: false,
      auditing: false,
      refunds: [],
      auditDialogVisible: false,
      auditPass: true,
      currentAuditRefund: null,
      auditForm: {
        auditRemark: ''
      }
    }
  },
  mounted() {
    this.loadPendingAudit()
  },
  methods: {
    async loadPendingAudit() {
      this.loading = true
      try {
        const res = await refundApi.getPendingAudit()
        if (res.code === 200) {
          this.refunds = res.data || []
        }
      } catch (e) {
        this.$message.error('加载待审核退款失败')
      } finally {
        this.loading = false
      }
    },
    openAuditDialog(refund, pass) {
      this.currentAuditRefund = refund
      this.auditPass = pass
      this.auditForm.auditRemark = ''
      this.auditDialogVisible = true
    },
    resetAuditForm() {
      this.currentAuditRefund = null
      this.auditForm.auditRemark = ''
    },
    async submitAudit() {
      this.auditing = true
      try {
        const res = await refundApi.audit({
          refundNo: this.currentAuditRefund.refundNo,
          auditUserId: store.state.user.id,
          pass: this.auditPass,
          auditRemark: this.auditForm.auditRemark
        })

        if (res.code === 200) {
          this.$message.success(this.auditPass ? '审核通过成功' : '审核拒绝成功')
          this.auditDialogVisible = false
          this.loadPendingAudit()
        }
      } catch (e) {
        this.$message.error('审核失败')
      } finally {
        this.auditing = false
      }
    }
  }
}
</script>

<style scoped>
.refund-audit-page {
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
</style>
