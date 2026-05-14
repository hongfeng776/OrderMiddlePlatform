<template>
  <div class="inventory-lock-container">
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
            <h2>库存锁定记录</h2>
            <div class="header-actions">
              <el-button type="warning" @click="releaseExpired" :loading="releasingExpired">
                <el-icon><Timer /></el-icon>
                释放超时锁定
              </el-button>
              <el-button type="primary" @click="loadLockRecords">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </div>
          
          <el-card class="filter-card">
            <el-form :inline="true" :model="filterForm" class="filter-form">
              <el-form-item label="商品ID">
                <el-input v-model="filterForm.productId" placeholder="请输入商品ID" style="width: 150px" />
              </el-form-item>
              <el-form-item label="订单号">
                <el-input v-model="filterForm.orderNo" placeholder="请输入订单号" style="width: 200px" />
              </el-form-item>
              <el-form-item label="锁定状态">
                <el-select v-model="filterForm.lockStatus" placeholder="全部" style="width: 120px" clearable>
                  <el-option label="锁定中" :value="0" />
                  <el-option label="已确认扣减" :value="1" />
                  <el-option label="已释放" :value="2" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="loadLockRecords">查询</el-button>
                <el-button @click="resetFilter">重置</el-button>
              </el-form-item>
            </el-form>
          </el-card>
          
          <el-card class="table-card">
            <el-table :data="lockRecords" stripe v-loading="loading">
              <el-table-column prop="lockNo" label="锁定单号" min-width="180" />
              <el-table-column prop="productId" label="商品ID" width="100" />
              <el-table-column prop="productName" label="商品名称" min-width="150" />
              <el-table-column prop="orderNo" label="订单号" min-width="180" />
              <el-table-column prop="userId" label="用户ID" width="100" />
              <el-table-column prop="lockQuantity" label="锁定数量" width="100" />
              <el-table-column prop="lockStatus" label="锁定状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="getLockStatusType(row.lockStatus)" size="small">
                    {{ getLockStatusText(row.lockStatus) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="lockType" label="锁定类型" width="120">
                <template #default="{ row }">
                  <el-tag :type="row.lockType === 1 ? 'primary' : 'info'" size="small">
                    {{ row.lockType === 1 ? '下单预扣' : '手动锁定' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="lockExpireTime" label="过期时间" width="180">
                <template #default="{ row }">
                  <span :class="{ 'expired': isExpired(row.lockExpireTime) && row.lockStatus === 0 }">
                    {{ formatTime(row.lockExpireTime) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150" fixed="right">
                <template #default="{ row }">
                  <el-button 
                    v-if="row.lockStatus === 0" 
                    type="danger" 
                    size="small" 
                    @click="openReleaseDialog(row)"
                  >
                    手动解锁
                  </el-button>
                  <span v-else>-</span>
                </template>
              </el-table-column>
            </el-table>
            
            <el-pagination
              v-model:current-page="pagination.page"
              v-model:page-size="pagination.size"
              :total="pagination.total"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadLockRecords"
              @current-change="loadLockRecords"
              class="pagination"
            />
          </el-card>
        </el-main>
      </el-container>
    </el-container>
    
    <el-dialog
      v-model="releaseDialogVisible"
      title="手动解锁"
      width="500px"
      :before-close="cancelRelease"
    >
      <el-form :model="releaseForm" label-width="80px">
        <el-form-item label="锁定单号">
          <span>{{ currentRecord?.lockNo }}</span>
        </el-form-item>
        <el-form-item label="商品名称">
          <span>{{ currentRecord?.productName }}</span>
        </el-form-item>
        <el-form-item label="锁定数量">
          <span>{{ currentRecord?.lockQuantity }}</span>
        </el-form-item>
        <el-form-item label="解锁备注">
          <el-input
            v-model="releaseForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入解锁备注（可选）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelRelease">取消</el-button>
        <el-button type="primary" @click="confirmRelease" :loading="releasing">
          确认解锁
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { computed, onMounted, reactive, toRefs } from 'vue'
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'
import { inventoryLockApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { HomeFilled, Setting, Box, Lock, Timer, Refresh } from '@element-plus/icons-vue'

export default {
  name: 'InventoryLock',
  components: { HomeFilled, Setting, Box, Lock, Timer, Refresh },
  setup() {
    const store = useStore()
    const router = useRouter()
    const route = useRoute()
    
    const state = reactive({
      loading: false,
      releasing: false,
      releasingExpired: false,
      lockRecords: [],
      releaseDialogVisible: false,
      currentRecord: null,
      filterForm: {
        productId: route.query.productId || '',
        orderNo: '',
        lockStatus: null
      },
      releaseForm: {
        remark: ''
      },
      pagination: {
        page: 1,
        size: 10,
        total: 0
      }
    })
    
    const userInfo = computed(() => store.state.user)
    const activeMenu = computed(() => route.path)
    
    const getLockStatusType = (status) => {
      const types = { 0: 'warning', 1: 'success', 2: 'info' }
      return types[status] || 'info'
    }
    
    const getLockStatusText = (status) => {
      const texts = { 0: '锁定中', 1: '已确认扣减', 2: '已释放' }
      return texts[status] || '未知'
    }
    
    const formatTime = (time) => {
      if (!time) return '-'
      return new Date(time).toLocaleString('zh-CN')
    }
    
    const isExpired = (expireTime) => {
      if (!expireTime) return false
      return new Date(expireTime) < new Date()
    }
    
    const loadLockRecords = async () => {
      state.loading = true
      try {
        const res = await inventoryLockApi.list(
          state.pagination.page,
          state.pagination.size,
          state.filterForm.productId || undefined,
          state.filterForm.orderNo || undefined,
          state.filterForm.lockStatus
        )
        state.lockRecords = res.data.records || []
        state.pagination.total = res.data.total || 0
      } catch (error) {
        ElMessage.error('加载锁定记录失败')
        console.error('加载锁定记录失败:', error)
      } finally {
        state.loading = false
      }
    }
    
    const resetFilter = () => {
      state.filterForm.productId = ''
      state.filterForm.orderNo = ''
      state.filterForm.lockStatus = null
      state.pagination.page = 1
      loadLockRecords()
    }
    
    const openReleaseDialog = (record) => {
      state.currentRecord = record
      state.releaseForm.remark = ''
      state.releaseDialogVisible = true
    }
    
    const cancelRelease = () => {
      state.releaseDialogVisible = false
      state.currentRecord = null
      state.releaseForm.remark = ''
    }
    
    const confirmRelease = async () => {
      if (!state.currentRecord) return
      
      state.releasing = true
      try {
        await inventoryLockApi.manualRelease(state.currentRecord.id, {
          operatorId: userInfo.value.userId,
          remark: state.releaseForm.remark
        })
        ElMessage.success('解锁成功')
        cancelRelease()
        loadLockRecords()
      } catch (error) {
        ElMessage.error('解锁失败')
        console.error('解锁失败:', error)
      } finally {
        state.releasing = false
      }
    }
    
    const releaseExpired = async () => {
      state.releasingExpired = true
      try {
        const res = await inventoryLockApi.releaseExpired()
        ElMessage.success(`已释放 ${res.data} 条超时锁定记录`)
        loadLockRecords()
      } catch (error) {
        ElMessage.error('释放超时锁定失败')
        console.error('释放超时锁定失败:', error)
      } finally {
        state.releasingExpired = false
      }
    }
    
    const logout = () => {
      store.dispatch('logout')
      router.push('/login')
    }
    
    onMounted(() => {
      loadLockRecords()
    })
    
    return {
      userInfo,
      activeMenu,
      getLockStatusType,
      getLockStatusText,
      formatTime,
      isExpired,
      loadLockRecords,
      resetFilter,
      openReleaseDialog,
      cancelRelease,
      confirmRelease,
      releaseExpired,
      logout,
      ...toRefs(state)
    }
  }
}
</script>

<style scoped>
.inventory-lock-container {
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

.header-actions {
  display: flex;
  gap: 10px;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-form {
  margin: 0;
}

.table-card {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.pagination {
  margin-top: 20px;
  text-align: right;
}

.expired {
  color: #f56c6c;
  font-weight: bold;
}
</style>
