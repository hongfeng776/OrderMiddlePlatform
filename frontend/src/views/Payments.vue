<template>
  <div class="payments-page">
    <div class="container">
      <el-card v-loading="loading">
        <template #header>
          <div class="card-header">
            <span>支付记录</span>
          </div>
        </template>

        <el-table :data="payments" stripe style="width: 100%">
          <el-table-column prop="payNo" label="支付单号" min-width="180" />
          <el-table-column prop="orderNo" label="订单编号" min-width="180" />
          <el-table-column prop="payAmount" label="支付金额" width="120">
            <template #default="{ row }">
              <span class="amount">¥{{ row.payAmount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="payType" label="支付方式" width="100">
            <template #default="{ row }">
              {{ getPayTypeText(row.payType) }}
            </template>
          </el-table-column>
          <el-table-column prop="payStatus" label="支付状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.payStatus)" size="small">
                {{ getPayStatusText(row.payStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="payTime" label="支付时间" width="180">
            <template #default="{ row }">
              {{ row.payTime || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="thirdPartyNo" label="第三方流水号" min-width="150" />
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button 
                v-if="row.payStatus === 0" 
                type="primary" 
                size="small" 
                @click="goPay(row.payNo)"
              >
                去支付
              </el-button>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="payments.length === 0" description="暂无支付记录" />
      </el-card>
    </div>
  </div>
</template>

<script>
import { paymentApi } from '@/api'
import store from '@/store'

export default {
  name: 'Payments',
  data() {
    return {
      loading: false,
      payments: []
    }
  },
  mounted() {
    this.loadPayments()
  },
  methods: {
    async loadPayments() {
      this.loading = true
      try {
        const res = await paymentApi.getUserPayments(store.state.user.id)
        if (res.code === 200) {
          this.payments = res.data || []
        }
      } catch (e) {
        this.$message.error('加载支付记录失败')
      } finally {
        this.loading = false
      }
    },
    goPay(payNo) {
      this.$router.push(`/pay/${payNo}`)
    },
    getPayTypeText(type) {
      const map = { 1: '支付宝', 2: '微信支付', 3: '银行卡' }
      return map[type] || '未知'
    },
    getPayStatusText(status) {
      const map = { 0: '待支付', 1: '支付成功', 2: '支付失败' }
      return map[status] || '未知'
    },
    getStatusType(status) {
      const map = { 0: 'warning', 1: 'success', 2: 'danger' }
      return map[status] || 'info'
    }
  }
}
</script>

<style scoped>
.payments-page {
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
