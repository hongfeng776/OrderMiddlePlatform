<template>
  <div class="schedule-job-container">
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
            <el-menu-item index="/notifications">
              <el-icon><Bell /></el-icon>
              <span>通知中心</span>
            </el-menu-item>
            <el-menu-item index="/schedule-job">
              <el-icon><Timer /></el-icon>
              <span>定时任务</span>
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
          <el-card v-loading="loading" class="job-card">
            <template #header>
              <div class="card-header">
                <span>定时任务管理</span>
              </div>
            </template>

            <el-row :gutter="20" class="statistics-row" v-if="statistics">
              <el-col :span="6">
                <div class="stat-card">
                  <div class="stat-value">{{ statistics.totalCount || 0 }}</div>
                  <div class="stat-label">总执行次数</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-card success">
                  <div class="stat-value">{{ statistics.successCount || 0 }}</div>
                  <div class="stat-label">成功次数</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-card danger">
                  <div class="stat-value">{{ statistics.failCount || 0 }}</div>
                  <div class="stat-label">失败次数</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-card warning">
                  <div class="stat-value">{{ statistics.avgDuration || 0 }}ms</div>
                  <div class="stat-label">平均耗时</div>
                </div>
              </el-col>
            </el-row>

            <div class="job-list">
              <div v-for="job in jobs" :key="job.handler" class="job-item">
                <div class="job-header">
                  <div class="job-name">
                    <el-icon :color="getJobIconColor(job.handler)">
                      <component :is="getJobIcon(job.handler)" />
                    </el-icon>
                    {{ job.name }}
                  </div>
                  <div class="job-tags">
                    <el-tag size="small" type="success">运行中</el-tag>
                    <el-tag v-if="job.sharding" size="small" type="warning">支持分片</el-tag>
                    <el-tag v-if="job.alertEnabled" size="small" type="danger">告警开启</el-tag>
                  </div>
                </div>
                <div class="job-info">
                  <div class="info-item">
                    <span class="label">任务处理器：</span>
                    <span class="value">{{ job.handler }}</span>
                  </div>
                  <div class="info-item">
                    <span class="label">执行计划：</span>
                    <span class="value">{{ job.schedule }}</span>
                  </div>
                  <div class="info-item">
                    <span class="label">Cron表达式：</span>
                    <span class="value code">{{ job.cron }}</span>
                  </div>
                  <div class="info-item">
                    <span class="label">任务描述：</span>
                    <span class="value">{{ job.description }}</span>
                  </div>
                  <div class="info-item">
                    <span class="label">最大重试：</span>
                    <span class="value">{{ job.maxRetry }}次</span>
                  </div>
                  <div class="info-item">
                    <span class="label">重试间隔：</span>
                    <span class="value">{{ job.retryDelay }}</span>
                  </div>
                </div>
                <div class="job-actions">
                  <el-button size="small" type="primary" @click="viewLogs(job.handler)">
                    查看执行日志
                  </el-button>
                  <el-button size="small" type="success" @click="viewStatistics(job.handler)">
                    查看统计
                  </el-button>
                  <el-button size="small" type="warning" @click="openTriggerDialog(job)">
                    手动触发
                  </el-button>
                  <el-button size="small" type="danger" @click="stopJob(job.jobId)">
                    终止任务
                  </el-button>
                </div>
              </div>
            </div>
          </el-card>

          <el-card v-if="selectedJob" v-loading="logLoading" class="log-card" style="margin-top: 20px;">
            <template #header>
              <div class="card-header">
                <span>执行日志 - {{ selectedJob }}</span>
                <div class="header-actions">
                  <el-button size="small" type="success" @click="exportLogs">
                    导出Excel
                  </el-button>
                  <el-select v-model="filterStatus" placeholder="状态筛选" size="small" style="width: 120px; margin-right: 10px;" @change="loadJobLogs">
                    <el-option label="全部" :value="null" />
                    <el-option label="成功" :value="1" />
                    <el-option label="失败" :value="0" />
                  </el-select>
                  <el-button size="small" @click="selectedJob = null">关闭</el-button>
                </div>
              </div>
            </template>

            <el-table :data="jobLogs" stripe style="width: 100%;" @row-click="showLogDetail">
              <el-table-column prop="executeTime" label="执行时间" width="180" />
              <el-table-column prop="shardIndex" label="分片" width="80" align="center">
                <template #default="scope">
                  <span v-if="scope.row.shardTotal > 1">{{ scope.row.shardIndex }}/{{ scope.row.shardTotal }}</span>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100" align="center">
                <template #default="scope">
                  <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
                    {{ scope.row.status === 1 ? '成功' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="retryCount" label="重试次数" width="100" align="center">
                <template #default="scope">
                  <el-tag v-if="scope.row.retryCount > 0" size="small" type="warning">
                    {{ scope.row.retryCount }}次
                  </el-tag>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column prop="duration" label="耗时(ms)" width="100" align="center" />
              <el-table-column prop="executorAddress" label="执行器" width="150" />
              <el-table-column prop="executeResult" label="执行结果" min-width="200" show-overflow-tooltip />
              <el-table-column prop="alertStatus" label="告警" width="80" align="center">
                <template #default="scope">
                  <el-tag v-if="scope.row.alertStatus === 1" size="small" type="danger">已发送</el-tag>
                  <span v-else>-</span>
                </template>
              </el-table-column>
            </el-table>

            <el-pagination
              v-model:current-page="logPage"
              v-model:page-size="logSize"
              :total="logTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadJobLogs"
              @current-change="loadJobLogs"
              style="margin-top: 20px; justify-content: flex-end;"
            />
          </el-card>

          <el-dialog v-model="triggerDialogVisible" title="手动触发任务" width="500px">
            <el-form label-width="100px">
              <el-form-item label="任务名称">
                <span>{{ currentJob?.name }}</span>
              </el-form-item>
              <el-form-item label="任务处理器">
                <span>{{ currentJob?.handler }}</span>
              </el-form-item>
              <el-form-item label="执行参数">
                <el-input
                  v-model="triggerParam"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入执行参数（可选）"
                />
              </el-form-item>
            </el-form>
            <template #footer>
              <el-button @click="triggerDialogVisible = false">取消</el-button>
              <el-button type="primary" @click="confirmTrigger" :loading="triggerLoading">确认触发</el-button>
            </template>
          </el-dialog>

          <el-dialog v-model="logDetailVisible" title="执行详情" width="700px">
            <el-descriptions v-if="currentLog" :column="1" border>
              <el-descriptions-item label="执行时间">{{ currentLog.executeTime }}</el-descriptions-item>
              <el-descriptions-item label="任务处理器">{{ currentLog.jobHandler }}</el-descriptions-item>
              <el-descriptions-item label="分片信息">
                {{ currentLog.shardTotal > 1 ? `${currentLog.shardIndex}/${currentLog.shardTotal}` : '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="执行状态">
                <el-tag :type="currentLog.status === 1 ? 'success' : 'danger'">
                  {{ currentLog.status === 1 ? '成功' : '失败' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="执行耗时">{{ currentLog.duration }}ms</el-descriptions-item>
              <el-descriptions-item label="重试次数">{{ currentLog.retryCount || 0 }}次</el-descriptions-item>
              <el-descriptions-item label="执行器地址">{{ currentLog.executorAddress }}</el-descriptions-item>
              <el-descriptions-item label="执行结果">{{ currentLog.executeResult || '-' }}</el-descriptions-item>
              <el-descriptions-item label="错误信息" v-if="currentLog.errorMessage">
                <div style="color: #f56c6c; white-space: pre-wrap;">{{ currentLog.errorMessage }}</div>
              </el-descriptions-item>
              <el-descriptions-item label="告警状态" v-if="currentLog.alertStatus">
                <el-tag v-if="currentLog.alertStatus === 1" type="danger">已发送告警</el-tag>
                <el-tag v-else-if="currentLog.alertStatus === 2" type="warning">告警发送失败</el-tag>
                <span v-else>未告警</span>
              </el-descriptions-item>
              <el-descriptions-item label="告警时间" v-if="currentLog.alertTime">{{ currentLog.alertTime }}</el-descriptions-item>
            </el-descriptions>
          </el-dialog>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
import { computed, onMounted, reactive, toRefs } from 'vue'
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'
import { scheduleJobApi } from '@/api'
import { ElMessage } from 'element-plus'
import { HomeFilled, List, Bell, Timer, Plus, User, AlarmClock, Box, Warning } from '@element-plus/icons-vue'

export default {
  name: 'ScheduleJob',
  components: { HomeFilled, List, Bell, Timer, Plus, User, AlarmClock, Box, Warning },
  setup() {
    const store = useStore()
    const router = useRouter()
    const route = useRoute()

    const state = reactive({
      loading: false,
      logLoading: false,
      triggerLoading: false,
      jobs: [],
      jobLogs: [],
      selectedJob: null,
      filterStatus: null,
      logPage: 1,
      logSize: 10,
      logTotal: 0,
      statistics: null,
      logDetailVisible: false,
      triggerDialogVisible: false,
      currentLog: null,
      currentJob: null,
      triggerParam: ''
    })

    const userInfo = computed(() => store.state.user)
    const activeMenu = computed(() => route.path)

    const getJobIcon = (handler) => {
      const iconMap = {
        'orderTimeoutCancelJobHandler': AlarmClock,
        'orderArchiveJobHandler': Box,
        'inventoryWarningJobHandler': Warning
      }
      return iconMap[handler] || Timer
    }

    const getJobIconColor = (handler) => {
      const colorMap = {
        'orderTimeoutCancelJobHandler': '#f56c6c',
        'orderArchiveJobHandler': '#409eff',
        'inventoryWarningJobHandler': '#e6a23c'
      }
      return colorMap[handler] || '#909399'
    }

    const loadJobs = async () => {
      state.loading = true
      try {
        const res = await scheduleJobApi.getJobList()
        state.jobs = res.data
        await loadOverallStatistics()
      } catch (error) {
        ElMessage.error('加载任务列表失败')
      } finally {
        state.loading = false
      }
    }

    const loadOverallStatistics = async () => {
      try {
        const res = await scheduleJobApi.getJobStatistics(null)
        state.statistics = res.data
      } catch (error) {
        console.error('加载统计信息失败', error)
      }
    }

    const viewLogs = async (jobHandler) => {
      state.selectedJob = jobHandler
      state.logPage = 1
      state.filterStatus = null
      await loadJobLogs()
    }

    const viewStatistics = async (jobHandler) => {
      try {
        const res = await scheduleJobApi.getJobStatistics(jobHandler)
        const stats = res.data
        ElMessage.success(`${jobHandler} 统计：总执行 ${stats.totalCount} 次，成功 ${stats.successCount} 次，失败 ${stats.failCount} 次，平均耗时 ${stats.avgDuration}ms`)
      } catch (error) {
        ElMessage.error('加载任务统计失败')
      }
    }

    const loadJobLogs = async () => {
      state.logLoading = true
      try {
        const res = await scheduleJobApi.getJobLogs(state.logPage, state.logSize, state.selectedJob, state.filterStatus)
        state.jobLogs = res.data.records
        state.logTotal = res.data.total
      } catch (error) {
        ElMessage.error('加载任务日志失败')
      } finally {
        state.logLoading = false
      }
    }

    const showLogDetail = (row) => {
      state.currentLog = row
      state.logDetailVisible = true
    }

    const openTriggerDialog = (job) => {
      state.currentJob = job
      state.triggerParam = ''
      state.triggerDialogVisible = true
    }

    const confirmTrigger = async () => {
      state.triggerLoading = true
      try {
        await scheduleJobApi.triggerJob(state.currentJob.handler, state.triggerParam)
        ElMessage.success('任务触发成功')
        state.triggerDialogVisible = false
      } catch (error) {
        ElMessage.error('任务触发失败')
      } finally {
        state.triggerLoading = false
      }
    }

    const stopJob = async (jobId) => {
      try {
        await scheduleJobApi.stopJob(jobId)
        ElMessage.success('任务终止成功')
      } catch (error) {
        ElMessage.error('任务终止失败')
      }
    }

    const exportLogs = () => {
      scheduleJobApi.exportLogs(state.selectedJob, state.filterStatus)
      ElMessage.success('导出任务已提交，文件将自动下载')
    }

    const logout = () => {
      store.dispatch('logout')
      router.push('/login')
    }

    onMounted(() => {
      loadJobs()
    })

    return {
      userInfo,
      activeMenu,
      getJobIcon,
      getJobIconColor,
      viewLogs,
      viewStatistics,
      showLogDetail,
      openTriggerDialog,
      confirmTrigger,
      stopJob,
      exportLogs,
      logout,
      ...toRefs(state)
    }
  }
}
</script>

<style scoped>
.schedule-job-container {
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

.header-actions {
  display: flex;
  align-items: center;
}

.statistics-row {
  margin-bottom: 24px;
}

.stat-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  color: white;
}

.stat-card.success {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

.stat-card.danger {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
}

.stat-card.warning {
  background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
}

.job-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.job-item {
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  border-left: 4px solid #409eff;
  transition: all 0.3s;
}

.job-item:hover {
  background: #ecf5ff;
  transform: translateX(4px);
}

.job-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.job-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.job-tags {
  display: flex;
  gap: 8px;
}

.job-info {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-item .label {
  color: #909399;
  font-size: 14px;
}

.info-item .value {
  color: #303133;
  font-size: 14px;
}

.info-item .value.code {
  font-family: monospace;
  background: #fff;
  padding: 2px 8px;
  border-radius: 4px;
}

.job-actions {
  display: flex;
  gap: 12px;
}
</style>
