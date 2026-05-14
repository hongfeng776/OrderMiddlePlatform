<template>
  <div class="order-detail-container">
    <el-container>
      <el-header class="header">
        <div class="logo">订单履约中台</div>
        <div class="user-info">
          <span>{{ userInfo.nickname || userInfo.username }}</span>
          <el-button type="text" @click="logout">退出登录</el-button>
        </div>
      </el-header>
      
      <el-container>
        <el-aside width="200px" class="aside">
          <el-menu :default-active="activeMenu" router class="menu">
            <el-menu-item index="/">
              <el-icon><HomeFilled /></el-icon>
              <span>首页</span>
            </el-menu-item>
            <el-menu-item index="/orders">
              <el-icon><List /></el-icon>
              <span>订单列表</span>
            </el-menu-item>
            <el-menu-item index="/payments">
              <el-icon><Wallet /></el-icon>
              <span>支付记录</span>
            </el-menu-item>
            <el-menu-item index="/refunds">
              <el-icon><Refund /></el-icon>
              <span>我的退款</span>
            </el-menu-item>
            <el-menu-item index="/refund-audit">
              <el-icon><Check /></el-icon>
              <span>退款审核</span>
            </el-menu-item>
            <el-menu-item index="/callback-logs">
              <el-icon><Document /></el-icon>
              <span>回调日志</span>
            </el-menu-item>
            <el-menu-item index="/create-order">
              <el-icon><Plus /></el-icon>
              <span>创建订单</span>
            </el-menu-item>
            <el-menu-item index="/profile">
              <el-icon><User /></el-icon>
              <span>个人中心</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        
        <el-main class="main">
          <el-card v-loading="loading">
            <template #header>
              <div class="card-header">
                <span>订单详情</span>
                <el-button type="primary" @click="$router.back()">返回</el-button>
              </div>
            </template>
            
            <div v-if="order" class="order-info">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="订单编号">{{ order.orderNo }}</el-descriptions-item>
                <el-descriptions-item label="订单状态">
                  <el-tag :type="getStatusType(order.orderStatus)">
                    {{ getStatusText(order.orderStatus) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="订单金额">¥{{ order.totalAmount }}</el-descriptions-item>
                <el-descriptions-item label="实付金额">¥{{ order.payAmount }}</el-descriptions-item>
                <el-descriptions-item label="收货人">{{ order.receiverName }}</el-descriptions-item>
                <el-descriptions-item label="联系电话">{{ order.receiverPhone }}</el-descriptions-item>
                <el-descriptions-item label="收货地址" :span="2">{{ order.receiverAddress }}</el-descriptions-item>
                <el-descriptions-item label="下单时间" :span="2">{{ order.createTime }}</el-descriptions-item>
              </el-descriptions>

              <div class="section-title">商品信息</div>
              <el-table :data="items" style="width: 100%">
                <el-table-column prop="productName" label="商品名称" />
                <el-table-column prop="productPrice" label="单价">
                  <template #default="scope">¥{{ scope.row.productPrice }}</template>
                </el-table-column>
                <el-table-column prop="buyCount" label="数量" />
                <el-table-column label="小计">
                  <template #default="scope">
                    ¥{{ (scope.row.productPrice * scope.row.buyCount).toFixed(2) }}
                  </template>
                </el-table-column>
              </el-table>

              <div class="section-title">状态流转</div>
              <el-timeline>
                <el-timeline-item
                  v-for="(log, index) in statusHistory"
                  :key="log.id"
                  :timestamp="log.createTime"
                  placement="top"
                >
                  <div class="log-item">
                    <el-tag :type="index === statusHistory.length - 1 ? 'success' : 'info'">
                      {{ log.actionType || '状态变更' }}
                    </el-tag>
                    <div class="log-remark" v-if="log.remark">
                      {{ log.remark }}
                    </div>
                    <div class="log-operator" v-if="log.operatorName">
                      操作人：{{ log.operatorName }}
                    </div>
                  </div>
                </el-timeline-item>
              </el-timeline>

              <div class="section-title">操作</div>
              <el-form :model="actionForm" label-width="80px">
                <el-form-item label="备注信息">
                  <el-input
                    v-model="actionForm.remark"
                    type="textarea"
                    :rows="2"
                    placeholder="请输入操作备注（可选）"
                  />
                </el-form-item>
                <el-form-item>
                  <div class="action-buttons">
                    <el-button
                      v-if="order.orderStatus === 0"
                      type="primary"
                      @click="handlePay"
                    >
                      立即支付
                    </el-button>
                    <el-button
                      v-if="order.orderStatus === 0 || order.orderStatus === 1"
                      type="danger"
                      @click="handleCancel"
                    >
                      取消订单
                    </el-button>
                    <el-button
                      v-if="order.orderStatus === 1"
                      type="warning"
                      @click="handleShip"
                    >
                      发货
                    </el-button>
                    <el-button
                      v-if="order.orderStatus === 3"
                      type="success"
                      @click="handleComplete"
                    >
                      确认收货
                    </el-button>
                    <el-button
                      v-if="order.orderStatus === 1 || order.orderStatus === 2 || order.orderStatus === 3 || order.orderStatus === 4"
                      type="warning"
                      @click="openRefundDialog"
                    >
                      申请退款
                    </el-button>
                  </div>
                </el-form-item>
              </el-form>
            </div>
          </el-card>

          <el-dialog
            v-model="refundDialogVisible"
            title="申请退款"
            width="500px"
          >
            <el-form :model="refundForm" label-width="100px">
              <el-form-item label="订单编号">
                <span>{{ order?.orderNo }}</span>
              </el-form-item>
              <el-form-item label="支付单号">
                <span>{{ payment?.payNo }}</span>
              </el-form-item>
              <el-form-item label="退款金额">
                <el-input-number
                  v-model="refundForm.refundAmount"
                  :min="0.01"
                  :max="payment?.payAmount || 0"
                  :precision="2"
                  style="width: 100%"
                />
              </el-form-item>
              <el-form-item label="退款原因">
                <el-input
                  v-model="refundForm.refundReason"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入退款原因"
                />
              </el-form-item>
              <el-form-item label="退款凭证">
                <el-upload
                  v-model:file-list="refundVoucherList"
                  class="upload-demo"
                  action="#"
                  list-type="picture-card"
                  :auto-upload="false"
                  :on-preview="handlePreview"
                  :on-remove="handleRemove"
                  :on-change="handleChange"
                  :limit="5"
                >
                  <el-icon><Plus /></el-icon>
                </el-upload>
                <div class="upload-tip">最多上传5张图片，支持jpg、png格式</div>
              </el-form-item>
            </el-form>

            <template #footer>
              <el-button @click="refundDialogVisible = false">取消</el-button>
              <el-button type="primary" :loading="submittingRefund" @click="submitRefund">
                提交申请
              </el-button>
            </template>
          </el-dialog>

          <el-image-viewer
            v-if="previewVisible"
            :url-list="[previewUrl]"
            @close="previewVisible = false"
          />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
import { computed, onMounted, reactive, toRefs, watch } from 'vue'
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'
import { orderApi, paymentApi, refundApi } from '@/api'
import { ElMessage } from 'element-plus'
import { HomeFilled, List, Bell, Plus, User, Wallet, Refund, Check, Document } from '@element-plus/icons-vue'

export default {
  name: 'OrderDetail',
  components: { HomeFilled, List, Bell, Plus, User, Wallet, Refund, Check, Document },
  setup() {
    const store = useStore()
    const router = useRouter()
    const route = useRoute()

    const state = reactive({
      loading: false,
      order: null,
      items: [],
      statusHistory: [],
      payment: null,
      actionForm: {
        remark: ''
      },
      refundDialogVisible: false,
      refundForm: {
        refundAmount: '',
        refundReason: ''
      },
      refundVoucherList: [],
      submittingRefund: false,
      previewVisible: false,
      previewUrl: ''
    })

    const userInfo = computed(() => store.state.user)
    const activeMenu = computed(() => route.path)
    const unreadCount = computed(() => store.getters.unreadCount)

    const loadOrderDetail = async () => {
      state.loading = true
      try {
        const res = await orderApi.getDetail(route.params.orderNo)
        state.order = res.data.order
        state.items = res.data.items
        state.statusHistory = res.data.statusHistory.sort((a, b) => {
          return new Date(a.createTime) - new Date(b.createTime)
        })
        
        if (state.order.orderStatus >= 1) {
          const payRes = await paymentApi.getByOrderNo(route.params.orderNo)
          state.payment = payRes.data
        }
      } catch (error) {
        ElMessage.error('加载订单详情失败')
      } finally {
        state.loading = false
      }
    }

    const openRefundDialog = () => {
      if (!state.payment) {
        ElMessage.error('支付信息不存在')
        return
      }
      state.refundForm.refundAmount = state.payment.payAmount
      state.refundForm.refundReason = ''
      state.refundVoucherList = []
      state.refundDialogVisible = true
    }

    const submitRefund = async () => {
      if (!state.refundForm.refundReason) {
        ElMessage.error('请输入退款原因')
        return
      }
      state.submittingRefund = true
      try {
        const vouchers = state.refundVoucherList.map(file => {
          if (file.response && file.response.url) {
            return file.response.url
          }
          return file.url || `https://picsum.photos/200/200?random=${Math.random()}`
        })

        await refundApi.apply({
          orderNo: state.order.orderNo,
          payNo: state.payment.payNo,
          userId: store.state.user.id,
          refundAmount: state.refundForm.refundAmount,
          refundReason: state.refundForm.refundReason,
          refundVouchers: vouchers
        })
        ElMessage.success('退款申请提交成功')
        state.refundDialogVisible = false
      } catch (error) {
        ElMessage.error('退款申请提交失败')
      } finally {
        state.submittingRefund = false
      }
    }

    const handlePreview = (file) => {
      state.previewUrl = file.url
      state.previewVisible = true
    }

    const handleRemove = (file, fileList) => {
      state.refundVoucherList = fileList
    }

    const handleChange = (file, fileList) => {
      state.refundVoucherList = fileList
    }

    const loadUnreadCount = async () => {
      if (!store.state.user.userId) return
      try {
        const res = await orderApi.getUnreadCount(store.state.user.userId)
        store.dispatch('setUnreadCount', res.data)
      } catch (error) {
        console.error('加载未读数量失败', error)
      }
    }

    const getStatusText = (status) => {
      const statusMap = {
        0: '待支付',
        1: '已支付',
        2: '待发货',
        3: '已发货',
        4: '已完成',
        5: '已取消'
      }
      return statusMap[status] || '未知状态'
    }

    const getStatusType = (status) => {
      const typeMap = {
        0: 'warning',
        1: 'primary',
        2: 'info',
        3: 'warning',
        4: 'success',
        5: 'danger'
      }
      return typeMap[status] || 'info'
    }

    const handlePay = async () => {
      try {
        await orderApi.paySuccess(state.order.orderNo, state.actionForm.remark)
        ElMessage.success('支付成功')
        state.actionForm.remark = ''
        loadOrderDetail()
      } catch (error) {
        ElMessage.error('支付失败')
      }
    }

    const handleCancel = async () => {
      try {
        await orderApi.cancel(state.order.orderNo, state.actionForm.remark)
        ElMessage.success('取消成功')
        state.actionForm.remark = ''
        loadOrderDetail()
      } catch (error) {
        ElMessage.error('取消失败')
      }
    }

    const handleShip = async () => {
      try {
        await orderApi.ship(state.order.orderNo, state.actionForm.remark)
        ElMessage.success('发货成功')
        state.actionForm.remark = ''
        loadOrderDetail()
      } catch (error) {
        ElMessage.error('发货失败')
      }
    }

    const handleComplete = async () => {
      try {
        await orderApi.complete(state.order.orderNo, state.actionForm.remark)
        ElMessage.success('确认收货成功')
        state.actionForm.remark = ''
        loadOrderDetail()
      } catch (error) {
        ElMessage.error('确认收货失败')
      }
    }

    const logout = () => {
      store.dispatch('logout')
      router.push('/login')
    }

    onMounted(() => {
      loadOrderDetail()
      loadUnreadCount()
    })

    watch(() => route.path, () => {
      loadUnreadCount()
    })

    return {
      userInfo,
      activeMenu,
      unreadCount,
      logout,
      getStatusText,
      getStatusType,
      handlePay,
      handleCancel,
      handleShip,
      handleComplete,
      openRefundDialog,
      submitRefund,
      handlePreview,
      handleRemove,
      handleChange,
      ...toRefs(state)
    }
  }
}
</script>

<style scoped>
.order-detail-container {
  min-height: 100vh;
}

.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.logo {
  font-size: 20px;
  font-weight: bold;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-info .el-button {
  color: white;
}

.aside {
  background: #f5f7fa;
  border-right: 1px solid #e4e7ed;
}

.menu {
  border: none;
}

.main {
  background: #f0f2f5;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-info {
  margin-top: 20px;
}

.section-title {
  margin: 24px 0 16px;
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.log-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.log-remark {
  font-size: 14px;
  color: #606266;
}

.log-operator {
  font-size: 12px;
  color: #909399;
}

.action-buttons {
  margin-top: 24px;
  text-align: center;
  display: flex;
  justify-content: center;
  gap: 12px;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
</style>
