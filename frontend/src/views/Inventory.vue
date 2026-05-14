<template>
  <div class="inventory-container">
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
            <el-menu-item index="/order-management">
              <el-icon><Setting /></el-icon>
              <span>订单管理</span>
            </el-menu-item>
            <el-menu-item index="/inventory">
              <el-icon><Box /></el-icon>
              <span>库存管理</span>
            </el-menu-item>
            <el-menu-item index="/inventory-lock">
              <el-icon><Lock /></el-icon>
              <span>库存锁定</span>
            </el-menu-item>
            <el-menu-item index="/schedule-job">
              <el-icon><Timer /></el-icon>
              <span>定时任务</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        
        <el-main class="main">
          <div class="page-header">
            <h2>库存管理</h2>
            <el-button type="primary" @click="loadInventoryList">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
          
          <el-row :gutter="20" class="stats-row" v-if="inventoryList.length > 0">
            <el-col :span="8">
              <el-card class="stat-card">
                <div class="stat-icon total-icon">
                  <el-icon><Box /></el-icon>
                </div>
                <div class="stat-content">
                  <div class="stat-number">{{ totalStock }}</div>
                  <div class="stat-label">总库存数</div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card class="stat-card">
                <div class="stat-icon lock-icon">
                  <el-icon><Lock /></el-icon>
                </div>
                <div class="stat-content">
                  <div class="stat-number">{{ totalLockStock }}</div>
                  <div class="stat-label">锁定库存</div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card class="stat-card">
                <div class="stat-icon available-icon">
                  <el-icon><CircleCheck /></el-icon>
                </div>
                <div class="stat-content">
                  <div class="stat-number">{{ totalAvailableStock }}</div>
                  <div class="stat-label">可用库存</div>
                </div>
              </el-card>
            </el-col>
          </el-row>
          
          <el-card class="table-card">
            <el-table :data="inventoryList" stripe v-loading="loading">
              <el-table-column prop="productId" label="商品ID" width="100" />
              <el-table-column prop="productName" label="商品名称" min-width="150" />
              <el-table-column prop="price" label="价格" width="120">
                <template #default="{ row }">
                  ¥{{ row.price }}
                </template>
              </el-table-column>
              <el-table-column prop="stockNum" label="总库存" width="100" />
              <el-table-column prop="lockStock" label="锁定库存" width="100">
                <template #default="{ row }">
                  <el-tag v-if="row.lockStock > 0" type="warning" size="small">
                    {{ row.lockStock }}
                  </el-tag>
                  <span v-else>0</span>
                </template>
              </el-table-column>
              <el-table-column label="可用库存" width="100">
                <template #default="{ row }">
                  <span :class="{ 'low-stock': (row.stockNum - row.lockStock) < 10 }">
                    {{ row.stockNum - row.lockStock }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                    {{ row.status === 1 ? '上架' : '下架' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150">
                <template #default="{ row }">
                  <el-button type="primary" size="small" @click="viewLockRecords(row)">
                    查看锁定记录
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
import { computed, onMounted, reactive, toRefs } from 'vue'
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'
import { inventoryApi } from '@/api'
import { ElMessage } from 'element-plus'
import { HomeFilled, Setting, Box, Lock, Timer, Refresh, CircleCheck } from '@element-plus/icons-vue'

export default {
  name: 'Inventory',
  components: { HomeFilled, Setting, Box, Lock, Timer, Refresh, CircleCheck },
  setup() {
    const store = useStore()
    const router = useRouter()
    const route = useRoute()
    
    const state = reactive({
      loading: false,
      inventoryList: []
    })
    
    const userInfo = computed(() => store.state.user)
    const activeMenu = computed(() => route.path)
    
    const totalStock = computed(() => {
      return state.inventoryList.reduce((sum, item) => sum + (item.stockNum || 0), 0)
    })
    
    const totalLockStock = computed(() => {
      return state.inventoryList.reduce((sum, item) => sum + (item.lockStock || 0), 0)
    })
    
    const totalAvailableStock = computed(() => {
      return state.inventoryList.reduce((sum, item) => sum + ((item.stockNum || 0) - (item.lockStock || 0)), 0)
    })
    
    const loadInventoryList = async () => {
      state.loading = true
      try {
        const res = await inventoryApi.list()
        state.inventoryList = res.data || []
      } catch (error) {
        ElMessage.error('加载库存列表失败')
        console.error('加载库存列表失败:', error)
      } finally {
        state.loading = false
      }
    }
    
    const viewLockRecords = (row) => {
      router.push({
        path: '/inventory-lock',
        query: { productId: row.productId }
      })
    }
    
    const logout = () => {
      store.dispatch('logout')
      router.push('/login')
    }
    
    onMounted(() => {
      loadInventoryList()
    })
    
    return {
      userInfo,
      activeMenu,
      totalStock,
      totalLockStock,
      totalAvailableStock,
      loadInventoryList,
      viewLockRecords,
      logout,
      ...toRefs(state)
    }
  }
}
</script>

<style scoped>
.inventory-container {
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

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  color: #333;
}

.stats-row {
  margin-bottom: 20px;
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

.total-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.lock-icon {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.available-icon {
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

.table-card {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.low-stock {
  color: #f56c6c;
  font-weight: bold;
}
</style>
