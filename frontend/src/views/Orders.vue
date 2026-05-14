<template>
  <div class="orders-container">
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
          <h2 class="page-title">我的订单</h2>
          
          <el-table :data="orders" stripe border v-loading="loading">
            <el-table-column prop="orderNo" label="订单编号" width="200"></el-table-column>
            <el-table-column prop="totalAmount" label="订单金额" width="120">
              <template #default="scope">¥{{ scope.row.totalAmount }}</template>
            </el-table-column>
            <el-table-column prop="payAmount" label="实付金额" width="120">
              <template #default="scope">¥{{ scope.row.payAmount }}</template>
            </el-table-column>
            <el-table-column prop="orderStatus" label="订单状态" width="120">
              <template #default="scope">
                <el-tag :type="getStatusType(scope.row.orderStatus)">
                  {{ getStatusText(scope.row.orderStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="receiverName" label="收货人" width="100"></el-table-column>
            <el-table-column prop="receiverPhone" label="收货电话" width="130"></el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="180"></el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="scope">
                <el-button type="primary" size="small" @click="viewDetail(scope.row)">查看</el-button>
                <el-button v-if="scope.row.orderStatus === 0" type="success" size="small" @click="payOrder(scope.row)">支付</el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <el-empty v-if="!loading && orders.length === 0" description="暂无订单"></el-empty>
        </el-main>
      </el-container>
    </el-container>
    
    <el-dialog v-model="detailVisible" title="订单详情" width="700px">
      <div v-if="orderDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单编号">{{ orderDetail.order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单金额">¥{{ orderDetail.order.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="实付金额">¥{{ orderDetail.order.payAmount }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="getStatusType(orderDetail.order.orderStatus)">
              {{ getStatusText(orderDetail.order.orderStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="收货人">{{ orderDetail.order.receiverName }}</el-descriptions-item>
          <el-descriptions-item label="收货电话">{{ orderDetail.order.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item :span="2" label="收货地址">{{ orderDetail.order.receiverAddress }}</el-descriptions-item>
        </el-descriptions>
        
        <h4 style="margin: 20px 0 10px">商品明细</h4>
        <el-table :data="orderDetail.items" border size="small">
          <el-table-column prop="productName" label="商品名称"></el-table-column>
          <el-table-column prop="productPrice" label="单价" width="100">
            <template #default="scope">¥{{ scope.row.productPrice }}</template>
          </el-table-column>
          <el-table-column prop="buyCount" label="数量" width="80"></el-table-column>
          <el-table-column prop="totalAmount" label="小计" width="100">
            <template #default="scope">¥{{ scope.row.totalAmount }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { computed, onMounted, reactive, toRefs, watch } from 'vue'
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'
import { orderApi } from '@/api'
import { ElMessage } from 'element-plus'
import { HomeFilled, List, Plus, User } from '@element-plus/icons-vue'

export default {
  name: 'Orders',
  components: { HomeFilled, List, Plus, User },
  setup() {
    const store = useStore()
    const router = useRouter()
    const route = useRoute()
    
    const state = reactive({
      loading: false,
      orders: [],
      detailVisible: false,
      orderDetail: null
    })
    
    const userInfo = computed(() => store.state.user)
    const activeMenu = computed(() => route.path)
    
    const loadOrders = async () => {
      state.loading = true
      try {
        const res = await orderApi.listByUserId(userInfo.value.userId)
        state.orders = res.data
      } catch (error) {
        console.error('加载订单失败:', error)
      } finally {
        state.loading = false
      }
    }
    
    const getStatusType = (status) => {
      const types = {
        0: 'warning',
        1: 'primary',
        2: 'info',
        3: 'info',
        4: 'success',
        5: 'danger'
      }
      return types[status] || 'info'
    }
    
    const getStatusText = (status) => {
      const texts = {
        0: '待支付',
        1: '已支付',
        2: '待发货',
        3: '已发货',
        4: '已完成',
        5: '已取消'
      }
      return texts[status] || '未知'
    }
    
    const viewDetail = async (order) => {
      try {
        const res = await orderApi.getDetail(order.orderNo)
        state.orderDetail = res.data
        state.detailVisible = true
      } catch (error) {
        console.error('加载订单详情失败:', error)
      }
    }
    
    const payOrder = async (order) => {
      try {
        await orderApi.paySuccess(order.orderNo)
        ElMessage.success('支付成功')
        loadOrders()
      } catch (error) {
        console.error('支付失败:', error)
      }
    }
    
    const logout = () => {
      store.dispatch('logout')
      router.push('/login')
    }
    
    onMounted(() => {
      loadOrders()
    })
    
    watch(() => route.path, (newPath) => {
      if (newPath === '/orders') {
        loadOrders()
      }
    })
    
    return {
      userInfo,
      activeMenu,
      logout,
      getStatusType,
      getStatusText,
      viewDetail,
      payOrder,
      ...toRefs(state)
    }
  }
}
</script>

<style scoped>
.orders-container {
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

.page-title {
  margin-bottom: 20px;
  color: #333;
}
</style>
