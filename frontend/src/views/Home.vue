<template>
  <div class="home-container">
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
            <el-menu-item v-if="isAdmin" index="/order-management">
              <el-icon><Setting /></el-icon>
              <span>订单管理</span>
            </el-menu-item>
            <el-menu-item index="/orders">
              <el-icon><List /></el-icon>
              <span>订单列表</span>
            </el-menu-item>
            <el-menu-item index="/create-order">
              <el-icon><Plus /></el-icon>
              <span>创建订单</span>
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
            <el-menu-item index="/profile">
              <el-icon><User /></el-icon>
              <span>个人中心</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        
        <el-main class="main">
          <div class="welcome">
            <h2>欢迎使用订单履约中台</h2>
            <p>一站式订单管理解决方案</p>
          </div>
          
          <el-row :gutter="20" class="stats-row">
            <el-col :span="6">
              <el-card class="stat-card">
                <div class="stat-icon order-icon">
                  <el-icon><ShoppingCart /></el-icon>
                </div>
                <div class="stat-content">
                  <div class="stat-number">{{ orderCount }}</div>
                  <div class="stat-label">我的订单</div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card class="stat-card">
                <div class="stat-icon inventory-icon">
                  <el-icon><Box /></el-icon>
                </div>
                <div class="stat-content">
                  <div class="stat-number">{{ inventoryCount }}</div>
                  <div class="stat-label">商品总数</div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card class="stat-card">
                <div class="stat-icon pending-icon">
                  <el-icon><Clock /></el-icon>
                </div>
                <div class="stat-content">
                  <div class="stat-number">{{ pendingCount }}</div>
                  <div class="stat-label">待支付</div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card class="stat-card">
                <div class="stat-icon completed-icon">
                  <el-icon><CircleCheck /></el-icon>
                </div>
                <div class="stat-content">
                  <div class="stat-number">{{ completedCount }}</div>
                  <div class="stat-label">已完成</div>
                </div>
              </el-card>
            </el-col>
          </el-row>
          
          <div class="quick-actions">
            <h3>快捷操作</h3>
            <el-row :gutter="20">
              <el-col :span="8">
                <el-card class="action-card" @click="$router.push('/create-order')">
                  <el-icon size="40" color="#409eff"><Plus /></el-icon>
                  <div>快速下单</div>
                </el-card>
              </el-col>
              <el-col :span="8">
                <el-card class="action-card" @click="$router.push('/orders')">
                  <el-icon size="40" color="#67c23a"><List /></el-icon>
                  <div>查看订单</div>
                </el-card>
              </el-col>
              <el-col :span="8">
                <el-card class="action-card" @click="$router.push('/profile')">
                  <el-icon size="40" color="#e6a23c"><User /></el-icon>
                  <div>个人中心</div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
import { computed, onMounted, reactive, toRefs, watch } from 'vue'
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'
import { orderApi, inventoryApi } from '@/api'
import { HomeFilled, List, Plus, User, ShoppingCart, Box, Clock, CircleCheck, Wallet, Refund, Check, Document, Setting } from '@element-plus/icons-vue'

export default {
  name: 'Home',
  components: { HomeFilled, List, Plus, User, ShoppingCart, Box, Clock, CircleCheck, Wallet, Refund, Check, Document, Setting },
  setup() {
    const store = useStore()
    const router = useRouter()
    const route = useRoute()
    
    const state = reactive({
      orderCount: 0,
      inventoryCount: 0,
      pendingCount: 0,
      completedCount: 0
    })
    
    const userInfo = computed(() => store.state.user)
    const activeMenu = computed(() => route.path)
    const isAdmin = computed(() => store.getters.isAdmin)
    
    const loadData = async () => {
      try {
        const inventoryRes = await inventoryApi.list()
        state.inventoryCount = inventoryRes.data.length
        
        const orderRes = await orderApi.listByUserId(userInfo.value.userId)
        const orders = orderRes.data
        state.orderCount = orders.length
        state.pendingCount = orders.filter(o => o.orderStatus === 0).length
        state.completedCount = orders.filter(o => o.orderStatus === 4).length
      } catch (error) {
        console.error('加载数据失败:', error)
      }
    }
    
    const logout = () => {
      store.dispatch('logout')
      router.push('/login')
    }
    
    onMounted(() => {
      loadData()
    })
    
    watch(() => route.path, (newPath) => {
      if (newPath === '/') {
        loadData()
      }
    })
    
    return {
      userInfo,
      activeMenu,
      isAdmin,
      logout,
      ...toRefs(state)
    }
  }
}
</script>

<style scoped>
.home-container {
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

.welcome {
  text-align: center;
  padding: 30px 0;
}

.welcome h2 {
  color: #333;
  margin-bottom: 10px;
}

.welcome p {
  color: #666;
}

.stats-row {
  margin-bottom: 30px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 15px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.order-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.inventory-icon {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.pending-icon {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.completed-icon {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-number {
  font-size: 28px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  color: #666;
  font-size: 14px;
}

.quick-actions h3 {
  margin-bottom: 20px;
  color: #333;
}

.action-card {
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.action-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

.action-card div {
  margin-top: 10px;
  color: #333;
  font-weight: 500;
}
</style>
