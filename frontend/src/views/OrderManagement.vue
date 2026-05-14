<template>
  <div class="order-management-container">
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
              <el-icon><List /></el-icon>
              <span>订单管理</span>
            </el-menu-item>
            <el-menu-item index="/orders">
              <el-icon><Document /></el-icon>
              <span>我的订单</span>
            </el-menu-item>
            <el-menu-item index="/notifications" class="notification-menu-item">
              <el-icon><Bell /></el-icon>
              <span>通知中心</span>
              <el-badge v-if="unreadCount > 0" :value="unreadCount" :max="99" class="notification-badge" />
            </el-menu-item>
            <el-menu-item index="/profile">
              <el-icon><User /></el-icon>
              <span>个人中心</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        
        <el-main class="main">
          <h2 class="page-title">订单管理</h2>
          
          <el-card class="filter-card">
            <el-form :inline="true" :model="filterForm" class="filter-form">
              <el-form-item label="订单编号">
                <el-input 
                  v-model="filterForm.orderNo" 
                  placeholder="请输入订单编号" 
                  clearable
                  style="width: 200px"
                />
              </el-form-item>
              <el-form-item label="用户ID">
                <el-input 
                  v-model="filterForm.userId" 
                  placeholder="请输入用户ID" 
                  clearable
                  style="width: 150px"
                />
              </el-form-item>
              <el-form-item label="订单状态">
                <el-select v-model="filterForm.orderStatus" placeholder="请选择" clearable style="width: 150px">
                  <el-option label="全部" :value="null"></el-option>
                  <el-option label="待支付" :value="0"></el-option>
                  <el-option label="已支付" :value="1"></el-option>
                  <el-option label="待发货" :value="2"></el-option>
                  <el-option label="已发货" :value="3"></el-option>
                  <el-option label="已完成" :value="4"></el-option>
                  <el-option label="已取消" :value="5"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="创建时间">
                <el-date-picker
                  v-model="filterDateRange"
                  type="datetimerange"
                  range-separator="至"
                  start-placeholder="开始时间"
                  end-placeholder="结束时间"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  style="width: 380px"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="loadOrders">
                  <el-icon><Search /></el-icon>
                  查询
                </el-button>
                <el-button @click="resetFilter">
                  <el-icon><Refresh /></el-icon>
                  重置
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <el-card class="action-card">
            <div class="action-buttons">
              <el-button 
                type="primary" 
                :disabled="selectedOrders.length === 0"
                @click="handleBatchShip"
              >
                <el-icon><Van /></el-icon>
                批量发货
              </el-button>
              <el-button 
                type="warning" 
                :disabled="selectedOrders.length === 0"
                @click="handleBatchCancel"
              >
                <el-icon><Close /></el-icon>
                批量取消
              </el-button>
              <el-button 
                type="success" 
                @click="handleExport" 
                :loading="exportLoading"
              >
                <el-icon><Download /></el-icon>
                导出Excel
              </el-button>
              <el-button type="info" @click="viewBatchLogs">
                <el-icon><Document /></el-icon>
                批量操作日志
              </el-button>
            </div>
            <div class="selection-info" v-if="selectedOrders.length > 0">
              已选择 <strong>{{ selectedOrders.length }}</strong> 条订单
            </div>
            <div v-if="exportLoading" class="export-progress">
              <div class="progress-header">
                <span>正在导出数据...</span>
                <span>{{ exportProgress }}%</span>
              </div>
              <el-progress :percentage="exportProgress" :show-text="false" status="success" />
            </div>
          </el-card>
          
          <el-table 
            :data="orders" 
            stripe 
            border 
            v-loading="loading"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column prop="orderNo" label="订单编号" width="200" />
            <el-table-column prop="userId" label="用户ID" width="100" />
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
            <el-table-column prop="receiverName" label="收货人" width="100" />
            <el-table-column prop="receiverPhone" label="收货电话" width="130" />
            <el-table-column prop="receiverAddress" label="收货地址" min-width="200" />
            <el-table-column prop="createTime" label="创建时间" width="180" />
          </el-table>
          
          <el-empty v-if="!loading && orders.length === 0" description="暂无订单"></el-empty>
        </el-main>
      </el-container>
    </el-container>

    <el-dialog v-model="batchOperationVisible" :title="batchOperationTitle" width="550px">
      <el-alert
        :title="`即将对 ${selectedOrders.length} 条订单进行${batchOperationType === 'ship' ? '发货' : '取消'}操作"`
        type="warning"
        :closable="false"
        style="margin-bottom: 20px"
      >
        <template #default>
          <div style="margin-top: 8px">
            <p>• 操作将立即生效，且无法撤销</p>
            <p>• 请确认您选择的订单是否正确</p>
          </div>
        </template>
      </el-alert>
      <el-form :model="batchForm" label-width="80px">
        <el-form-item label="操作备注">
          <el-input 
            v-model="batchForm.remark" 
            type="textarea" 
            :rows="3" 
            placeholder="请输入操作备注（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchOperationVisible = false">取消</el-button>
        <el-button type="primary" @click="showSecondConfirm" :loading="batchOperationLoading">
          下一步
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="secondConfirmVisible"
      title="最终确认"
      width="500px"
      :close-on-click-modal="false"
    >
      <div style="text-align: center; padding: 20px 0">
        <el-icon size="60" color="#e6a23c">
          <Warning />
        </el-icon>
        <h3 style="margin: 20px 0; color: #606266">确认执行批量操作？</h3>
        <p style="color: #909399; margin-bottom: 10px">
          即将对 <strong style="color: #e6a23c">{{ selectedOrders.length }}</strong> 条订单进行
          <strong style="color: #409eff">{{ batchOperationType === 'ship' ? '发货' : '取消' }}</strong>操作
        </p>
        <p v-if="batchForm.remark" style="color: #909399">
          操作备注：{{ batchForm.remark }}
        </p>
      </div>
      <template #footer>
        <el-button @click="secondConfirmVisible = false">返回</el-button>
        <el-button type="primary" @click="confirmBatchOperation" :loading="batchOperationLoading">
          确认执行
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchLogsVisible" title="批量操作日志" width="900px">
      <el-table :data="batchLogs" stripe border v-loading="logsLoading">
        <el-table-column prop="batchNo" label="批次号" width="200" />
        <el-table-column prop="operationType" label="操作类型" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.operationType === 'SHIP' ? 'primary' : 'warning'">
              {{ scope.row.operationType === 'SHIP' ? '批量发货' : '批量取消' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="totalCount" label="总数" width="80" />
        <el-table-column prop="successCount" label="成功数" width="80">
          <template #default="scope">
            <el-tag type="success">{{ scope.row.successCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="failCount" label="失败数" width="80">
          <template #default="scope">
            <el-tag type="danger">{{ scope.row.failCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '已完成' : '处理中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="操作时间" width="180" />
        <el-table-column label="操作" width="100">
          <template #default="scope">
            <el-button type="primary" size="small" @click="viewBatchDetail(scope.row.batchNo)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="batchDetailVisible" :title="`批次详情 - ${currentBatchNo}`" width="800px">
      <el-table :data="batchDetails" stripe border v-loading="detailLoading">
        <el-table-column prop="orderNo" label="订单编号" width="200" />
        <el-table-column prop="success" label="操作结果" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.success === 1 ? 'success' : 'danger'">
              {{ scope.row.success === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="错误信息" min-width="200" />
        <el-table-column prop="createTime" label="操作时间" width="180" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { computed, onMounted, reactive, toRefs, watch } from 'vue'
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'
import { orderApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  HomeFilled, 
  List, 
  Bell, 
  User, 
  Document, 
  Warning, 
  Search, 
  Refresh, 
  Download,
  Van,
  Close
} from '@element-plus/icons-vue'

export default {
  name: 'OrderManagement',
  components: { 
    HomeFilled, 
    List, 
    Bell, 
    User, 
    Document, 
    Warning, 
    Search, 
    Refresh, 
    Download,
    Van,
    Close 
  },
  setup() {
    const store = useStore()
    const router = useRouter()
    const route = useRoute()
    
    const state = reactive({
      loading: false,
      orders: [],
      selectedOrders: [],
      filterForm: {
        orderStatus: null,
        startTime: '',
        endTime: '',
        userId: null,
        orderNo: ''
      },
      batchOperationVisible: false,
      secondConfirmVisible: false,
      batchOperationType: '',
      batchOperationLoading: false,
      batchForm: {
        remark: ''
      },
      batchLogsVisible: false,
      logsLoading: false,
      batchLogs: [],
      batchDetailVisible: false,
      detailLoading: false,
      currentBatchNo: '',
      batchDetails: [],
      exportProgress: 0,
      exportLoading: false
    })
    
    const userInfo = computed(() => store.state.user)
    const activeMenu = computed(() => route.path)
    const unreadCount = computed(() => store.getters.unreadCount)
    
    const batchOperationTitle = computed(() => {
      return state.batchOperationType === 'ship' ? '批量发货' : '批量取消'
    })
    
    const filterDateRange = computed({
      get: () => {
        if (state.filterForm.startTime && state.filterForm.endTime) {
          return [state.filterForm.startTime, state.filterForm.endTime]
        }
        return null
      },
      set: (val) => {
        if (val && val.length === 2) {
          state.filterForm.startTime = val[0]
          state.filterForm.endTime = val[1]
        } else {
          state.filterForm.startTime = ''
          state.filterForm.endTime = ''
        }
      }
    })
    
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
    
    const loadOrders = async () => {
      state.loading = true
      try {
        const res = await orderApi.adminListOrders(
          state.filterForm.orderStatus,
          state.filterForm.startTime,
          state.filterForm.endTime,
          state.filterForm.userId,
          state.filterForm.orderNo
        )
        state.orders = res.data
        if (res.data && res.data.length > 0) {
          ElMessage.success(`已加载 ${res.data.length} 条订单`)
        }
      } catch (error) {
        console.error('加载订单失败:', error)
        state.loading = false
        if (error.response && error.response.status === 403) {
          ElMessage.error('没有访问权限，请联系管理员')
        } else {
          ElMessage.error('加载订单失败')
        }
      } finally {
        state.loading = false
      }
    }
    
    const resetFilter = () => {
      state.filterForm = {
        orderStatus: null,
        startTime: '',
        endTime: '',
        userId: null,
        orderNo: ''
      }
      loadOrders()
    }
    
    const handleSelectionChange = (selection) => {
      state.selectedOrders = selection
    }
    
    const handleBatchShip = () => {
      if (state.selectedOrders.length === 0) {
        ElMessage.warning('请选择要操作的订单')
        return
      }
      state.batchOperationType = 'ship'
      state.batchForm.remark = ''
      state.batchOperationVisible = true
    }
    
    const handleBatchCancel = () => {
      if (state.selectedOrders.length === 0) {
        ElMessage.warning('请选择要操作的订单')
        return
      }
      state.batchOperationType = 'cancel'
      state.batchForm.remark = ''
      state.batchOperationVisible = true
    }
    
    const showSecondConfirm = () => {
      state.batchOperationVisible = false
      state.secondConfirmVisible = true
    }
    
    const confirmBatchOperation = async () => {
      state.secondConfirmVisible = false
      state.batchOperationLoading = true
      try {
        const orderNos = state.selectedOrders.map(order => order.orderNo)
        let res
        
        if (state.batchOperationType === 'ship') {
          res = await orderApi.batchShip(
            orderNos, 
            state.batchForm.remark,
            userInfo.value.userId,
            userInfo.value.nickname || userInfo.value.username
          )
        } else {
          res = await orderApi.batchCancel(
            orderNos, 
            state.batchForm.remark,
            userInfo.value.userId,
            userInfo.value.nickname || userInfo.value.username
          )
        }
        
        const { totalCount, successCount, failCount } = res.data
        
        ElMessageBox.alert(
          `<div style="text-align: center">
            <p style="font-size: 16px; margin-bottom: 10px">批量操作完成</p>
            <p style="color: #67c23a; font-size: 14px">成功：${successCount} 条</p>
            <p style="color: #f56c6c; font-size: 14px">失败：${failCount} 条</p>
            <p style="color: #909399; font-size: 14px">共计：${totalCount} 条</p>
          </div>`,
          '操作结果',
          {
            dangerouslyUseHTMLString: true,
            confirmButtonText: '确定',
            type: failCount > 0 ? 'warning' : 'success'
          }
        )
        
        state.selectedOrders = []
        loadOrders()
      } catch (error) {
        console.error('批量操作失败:', error)
        state.batchOperationLoading = false
        if (error.response && error.response.status === 403) {
          ElMessage.error('没有操作权限，请联系管理员')
        } else {
          ElMessage.error('批量操作失败')
        }
      }
    }
    
    const handleExport = async () => {
      state.exportLoading = true
      state.exportProgress = 0
      
      try {
        const progressInterval = setInterval(() => {
          if (state.exportProgress < 70) {
            state.exportProgress += 10
          }
        }, 300)
        
        const exportData = {
          orderStatus: state.filterForm.orderStatus,
          startTime: state.filterForm.startTime,
          endTime: state.filterForm.endTime,
          userId: state.filterForm.userId
        }
        
        const res = await orderApi.exportOrders(exportData)
        
        clearInterval(progressInterval)
        state.exportProgress = 90
        
        const { fileName, downloadUrl, objectName, totalCount } = res.data
        
        const downloadRes = await orderApi.downloadExport(objectName)
        
        state.exportProgress = 100
        
        const blob = new Blob([downloadRes], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        })
        
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = fileName
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
        
        setTimeout(async () => {
          ElMessageBox.alert(
            `<div style="text-align: center">
              <el-icon size="48" color="#67c23a" style="margin-bottom: 10px">
                <circle-check />
              </el-icon>
              <p style="font-size: 16px; font-weight: bold; margin-bottom: 5px">导出成功！</p>
              <p style="color: #909399">共导出 ${totalCount} 条订单数据</p>
              <p style="color: #909399; font-size: 12px">文件名：${fileName}</p>
            </div>`,
            '导出完成',
            {
              dangerouslyUseHTMLString: true,
              confirmButtonText: '确定',
              type: 'success'
            }
          )
          state.exportLoading = false
          state.exportProgress = 0
        }, 500)
      } catch (error) {
        console.error('导出失败:', error)
        state.exportLoading = false
        state.exportProgress = 0
        if (error.response && error.response.status === 403) {
          ElMessage.error('没有导出权限，请联系管理员')
        } else {
          ElMessage.error('导出失败')
        }
      }
    }
    
    const viewBatchLogs = async () => {
      state.batchLogsVisible = true
      state.logsLoading = true
      try {
        const res = await orderApi.getBatchLogs()
        state.batchLogs = res.data
      } catch (error) {
        console.error('加载批量操作日志失败:', error)
      } finally {
        state.logsLoading = false
      }
    }
    
    const viewBatchDetail = async (batchNo) => {
      state.currentBatchNo = batchNo
      state.batchDetailVisible = true
      state.detailLoading = true
      try {
        const res = await orderApi.getBatchDetails(batchNo)
        state.batchDetails = res.data
      } catch (error) {
        console.error('加载批量操作详情失败:', error)
      } finally {
        state.detailLoading = false
      }
    }
    
    const logout = () => {
      store.dispatch('logout')
      router.push('/login')
    }
    
    onMounted(() => {
      loadOrders()
    })
    
    return {
      userInfo,
      activeMenu,
      unreadCount,
      batchOperationTitle,
      filterDateRange,
      getStatusType,
      getStatusText,
      loadOrders,
      resetFilter,
      handleSelectionChange,
      handleBatchShip,
      handleBatchCancel,
      showSecondConfirm,
      confirmBatchOperation,
      handleExport,
      viewBatchLogs,
      viewBatchDetail,
      logout,
      ...toRefs(state)
    }
  }
}
</script>

<style scoped>
.order-management-container {
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

.notification-menu-item {
  position: relative;
}

.notification-badge {
  position: absolute;
  top: 8px;
  right: 15px;
}

.main {
  background: #f0f2f5;
}

.page-title {
  margin-bottom: 20px;
  color: #333;
}

.filter-card,
.action-card {
  margin-bottom: 20px;
}

.action-buttons {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.selection-info {
  margin-top: 10px;
  padding: 8px 12px;
  background: #ecf5ff;
  border-radius: 4px;
  color: #409eff;
}

.export-progress {
  margin-top: 15px;
  padding: 15px;
  background: #f0f9ff;
  border-radius: 4px;
  border: 1px solid #b3d8ff;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  color: #409eff;
  font-size: 14px;
}
</style>
