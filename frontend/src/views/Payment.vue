<template>
  <div class="payment-page">
    <div class="container">
      <el-card v-loading="loading" class="payment-card">
        <template #header>
          <div class="card-header">
            <span>订单支付</span>
          </div>
        </template>

        <div v-if="payment" class="payment-info">
          <div class="info-item">
            <span class="label">支付单号：</span>
            <span class="value">{{ payment.payNo }}</span>
          </div>
          <div class="info-item">
            <span class="label">订单编号：</span>
            <span class="value">{{ payment.orderNo }}</span>
          </div>
          <div class="info-item">
            <span class="label">支付金额：</span>
            <span class="value amount">¥{{ payment.payAmount }}</span>
          </div>
          <div class="info-item">
            <span class="label">支付方式：</span>
            <span class="value">{{ getPayTypeText(payment.payType) }}</span>
          </div>
          <div class="info-item">
            <span class="label">支付状态：</span>
            <el-tag :type="getStatusType(payment.payStatus)">
              {{ getPayStatusText(payment.payStatus) }}
            </el-tag>
          </div>
        </div>

        <div v-if="payment && payment.payStatus === 0" class="pay-methods">
          <h4>选择支付方式</h4>
          <el-radio-group v-model="selectedPayType" class="pay-method-list">
            <el-radio :label="1" class="pay-method-item">
              <i class="icon alipay"></i>
              <span>支付宝</span>
            </el-radio>
            <el-radio :label="2" class="pay-method-item">
              <i class="icon wechat"></i>
              <span>微信支付</span>
            </el-radio>
            <el-radio :label="3" class="pay-method-item">
              <i class="icon bank"></i>
              <span>银行卡</span>
            </el-radio>
          </el-radio-group>
        </div>

        <div class="action-buttons" v-if="payment && payment.payStatus === 0">
          <el-button type="primary" size="large" @click="confirmPay" :loading="paying">
            确认支付
          </el-button>
          <el-button size="large" @click="simulateFail">
            模拟支付失败
          </el-button>
        </div>

        <div class="back-btn" v-if="payment && payment.payStatus !== 0">
          <el-button type="primary" @click="$router.push('/orders')">
            返回订单列表
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
import { paymentApi } from '@/api'

export default {
  name: 'Payment',
  data() {
    return {
      loading: false,
      paying: false,
      payment: null,
      selectedPayType: 1
    }
  },
  computed: {
    payNo() {
      return this.$route.params.payNo
    }
  },
  mounted() {
    this.loadPayment()
  },
  methods: {
    async loadPayment() {
      this.loading = true
      try {
        const res = await paymentApi.getByOrderNo(this.payNo.replace('PAY', 'ORD'))
        if (res.code === 200) {
          this.payment = res.data
        }
      } catch (e) {
        this.$message.error('加载支付信息失败')
      } finally {
        this.loading = false
      }
    },
    async confirmPay() {
      this.paying = true
      try {
        const res = await paymentApi.mockPaySuccess(this.payment.payNo)
        if (res.code === 200) {
          this.$message.success('支付成功！')
          this.loadPayment()
        }
      } catch (e) {
        this.$message.error('支付失败')
      } finally {
        this.paying = false
      }
    },
    async simulateFail() {
      this.paying = true
      try {
        const res = await paymentApi.mockPayFail(this.payment.payNo)
        if (res.code === 200) {
          this.$message.error('支付失败！')
          this.loadPayment()
        }
      } catch (e) {
        this.$message.error('操作失败')
      } finally {
        this.paying = false
      }
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
.payment-page {
  padding: 20px 0;
  background: #f5f7fa;
  min-height: 100vh;
}

.container {
  max-width: 600px;
  margin: 0 auto;
  padding: 0 20px;
}

.payment-card {
  border-radius: 8px;
}

.card-header {
  font-size: 18px;
  font-weight: 500;
}

.payment-info {
  margin-bottom: 30px;
}

.info-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item .label {
  width: 100px;
  color: #666;
}

.info-item .value {
  flex: 1;
  color: #333;
}

.info-item .value.amount {
  color: #f56c6c;
  font-size: 24px;
  font-weight: bold;
}

.pay-methods {
  margin-bottom: 30px;
}

.pay-methods h4 {
  margin-bottom: 15px;
  color: #333;
}

.pay-method-list {
  display: block;
}

.pay-method-item {
  display: flex;
  align-items: center;
  padding: 15px 20px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.3s;
}

.pay-method-item:hover,
.pay-method-item.is-active {
  border-color: #409eff;
  background: #ecf5ff;
}

.pay-method-item .icon {
  width: 32px;
  height: 32px;
  margin-right: 12px;
  border-radius: 4px;
  display: inline-block;
}

.pay-method-item .icon.alipay {
  background: #1677ff;
}

.pay-method-item .icon.wechat {
  background: #07c160;
}

.pay-method-item .icon.bank {
  background: #606266;
}

.action-buttons {
  display: flex;
  gap: 15px;
  justify-content: center;
}

.back-btn {
  text-align: center;
}
</style>
