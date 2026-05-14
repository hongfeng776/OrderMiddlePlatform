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
              <el-badge :value="abnormalCount" :hidden="abnormalCount === 0" class="abnormal-badge">
                <el-button type="danger" @click="loadAbnormalLocks" :loading="loadingAbnormal">
                  <el-icon><Warning /></el-icon>
                  异常锁定
                </el-button>
              </el-badge>
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
          
          <el-card v-if="showAbnormal && abnormalLocks.length > 0" class="abnormal-card">
            <template #header>
              <div class="abnormal-header">
                <el-icon><Warning /></el-icon>
                <span>异常锁定记录（超期未处理）</span>
              </div>
            </template>
            <el-table :data="abnormalLocks" stripe size="small">
              <el-table-column prop="lockNo" label="锁定单号" min-width="180" />
              <el-table-column prop="productId" label="商品ID" width="100" />
              <el-table-column prop="productName" label="商品名称" min-width="150" />
              <el-table-column prop="orderNo" label="订单号" min-width="180" />
              <el-table-column prop="lockQuantity" label="锁定数量" width="100" />
              <el-table-column prop="lockExpireTime" label="过期时间" width="180">
                <template #default="{ row }">
                  <span class="expired">{{ formatTime(row.lockExpireTime) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="180" fixed="right">
                <template #default="{ row }">
                  <el-button 
                    type="danger" 
                    size="small" 
                    @click="openForceReleaseDialog(row)"
                  >
                    <el-icon><Unlock /></el-icon>
                    强制解锁
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
          
          <el-card class="table-card">
            <el-table :data="lockRecords" stripe v-loading="loading">
              <el-table-column prop="lockNo" label="锁定单号" min-width="180">
                <template #default="{ row }">
                  <span v-if="isAbnormalLock(row)" class="abnormal-flag">
                    <el-icon><Warning /></el-icon>
                    {{ row.lockNo }}
                  </span>
                  <span v-else>{{ row.lockNo }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="productId" label="商品ID" width="100" />
              <el-table-column prop="productName" label="商品名称" min-width="150" />
              <el-table-column prop="orderNo" label="订单号" min-width="180" />
              <el-table-column prop="userId" label="用户ID" width="100" />
              <el-table-column prop="lockQuantity" label="锁定数量" width="100" />
              <el-table-column prop="lockStatus" label="锁定状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="getLockStatusType(row)" size="small">
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
              <el-table-column label="操作" width="180" fixed="right">
                <template #default="{ row }">
                  <template v-if="row.lockStatus === 0">
                    <el-button 
                      type="danger" 
                      size="small" 
                      @click="openReleaseDialog(row)"
                    >
                      手动解锁
                    </el-button>
                    <el-button 
                      v-if="isAbnormalLock(row)"
                      type="warning" 
                      size="small" 
                      @click="openForceReleaseDialog(row)"
                    >
                      强制解锁
                    </el-button>
                  </template>
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

    <el-dialog
      v-model="forceReleaseDialogVisible"
      title="强制解锁（异常锁定）"
      width="500px"
      :before-close="cancelForceRelease"
    >
      <el-alert
        title="此操作将强制释放异常锁定的库存"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
      >
        <template #default>
          该锁定已超过正常处理时间，可能是订单流程异常导致。强制解锁后库存将被释放。
        </template>
      </el-alert>
      <el-form :model="forceReleaseForm" label-width="80px">
        <el-form-item label="锁定单号">
          <span>{{ currentRecord?.lockNo }}</span>
        </el-form-item>
        <el-form-item label="商品名称">
          <span>{{ currentRecord?.productName }}</span>
        </el-form-item>
        <el-form-item label="锁定数量">
          <span>{{ currentRecord?.lockQuantity }}</span>
        </el-form-item>
        <el-form-item label="过期时间">
          <span class="expired">{{ formatTime(currentRecord?.lockExpireTime) }}</span>
        </el-form-item>
        <el-form-item label="解锁备注">
          <el-input
            v-model="forceReleaseForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入解锁备注（必填）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelForceRelease">取消</el-button>
        <el-button type="danger" @click="confirmForceRelease" :loading="forceReleasing">
          确认强制解锁
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
import { HomeFilled, Setting, Box, Lock, Timer, Refresh, Warning, Unlock } from '@element-plus/icons-vue'

export default {
  name: 'InventoryLock',
  components: { HomeFilled, Setting, Box, Lock, Timer, Refresh, Warning, Unlock },
  setup() {
    const store = useStore()
    const router = useRouter()
    const route = useRoute()
    
    const state = reactive({
      loading: false,
      releasing: false,
      forceReleasing: false,
      releasingExpired: false,
      loadingAbnormal: false,
      lockRecords: [],
      abnormalLocks: [],
      showAbnormal: false,
      releaseDialogVisible: false,
      forceReleaseDialogVisible: false,
      currentRecord: null,
      filterForm: {
        productId: route.query.productId || '',
        orderNo: '',
        lockStatus: null
      },
      releaseForm: {
        remark: ''
      },
      forceReleaseForm: {
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
    
    const abnormalCount = computed(() => {
      return state.lockRecords.filter(record => isAbnormalLock(record)).length
    })
    
    const getLockStatusType = (row) => {
      const status = row.lockStatus
      if (status === 0 && isAbnormalLock(row)) {
        return 'danger'
      }
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
    
    const isAbnormalLock = (record) => {
      if (!record || record.lockStatus !== 0) return false
      if (!record.lockExpireTime) return false
      const expireTime = new Date(record.lockExpireTime)
      const now = new Date()
      const diffMinutes = (now - expireTime) / (1000 * 60)
      return diffMinutes > 30
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
    
    const loadAbnormalLocks = async () => {
      state.loadingAbnormal = true
      try {
        const res = await inventoryLockApi.getAbnormalLocks()
        state.abnormalLocks = res.data || []
        state.showAbnormal = true
        if (state.abnormalLocks.length === 0) {
          ElMessage.info('当前没有异常锁定记录')
        }
      } catch (error) {
        ElMessage.error('加载异常锁定记录失败')
        console.error('加载异常锁定记录失败:', error)
      } finally {
        state.loadingAbnormal = false
      }
    }
    
    const resetFilter = () => {
      state.filterForm.productId = ''
      state.filterForm.orderNo = ''
      state.filterForm.lockStatus = null
      state.pagination.page = 1
      state.showAbnormal = false
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
    
    const openForceReleaseDialog = (record) => {
      state.currentRecord = record
      state.forceReleaseForm.remark = ''
      state.forceReleaseDialogVisible = true
    }
    
    const cancelForceRelease = () => {
      state.forceReleaseDialogVisible = false
      state.currentRecord = null
      state.forceReleaseForm.remark = ''
    }
    
    const confirmForceRelease = async () => {
      if (!state.currentRecord) return
      if (!state.forceReleaseForm.remark || state.forceReleaseForm.remark.trim() === '') {
        ElMessage.warning('请填写解锁备注')
        return
      }
      
      try {
        await ElMessageBox.confirm(
          '确定要强制解锁该异常锁定吗？此操作将直接释放库存并标记为异常处理。',
          '强制解锁确认',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
          }
        )
      } catch {
        return
      }
      
      state.forceReleasing = true
      try {
        await inventoryLockApi.forceRelease(state.currentRecord.id, {
          operatorId: userInfo.value.userId,
          remark: state.forceReleaseForm.remark
        })
        ElMessage.success('强制解锁成功')
        cancelForceRelease()
        loadLockRecords()
        if (state.showAbnormal) {
          loadAbnormalLocks()
        }
      } catch (error) {
        ElMessage.error('强制解锁失败')
        console.error('强制解锁失败:', error)
      } finally {
        state.forceReleasing = false
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
      abnormalCount,
      getLockStatusType,
      getLockStatusText,
      formatTime,
      isExpired,
      isAbnormalLock,
      loadLockRecords,
      loadAbnormalLocks,
      resetFilter,
      openReleaseDialog,
      cancelRelease,
      confirmRelease,
      openForceReleaseDialog,
      cancelForceRelease,
      confirmForceRelease,
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

.abnormal-badge {
  margin-right: 10px;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-form {
  margin: 0;
}

.abnormal-card {
  margin-bottom: 20px;
  border: 1px solid #f56c6c;
}

.abnormal-header {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #f56c6c;
  font-weight: bold;
}

.abnormal-flag {
  color: #f56c6c;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 4px;
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
