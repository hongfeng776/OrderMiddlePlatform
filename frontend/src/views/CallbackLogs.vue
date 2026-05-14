<template>
  <div class="callback-logs-page">
    <div class="container">
      <el-card v-loading="loading">
        <template #header>
          <div class="card-header">
            <span>回调日志</span>
            <el-select v-model="filterType" placeholder="全部类型" size="small" style="width: 150px" @change="loadLogs">
              <el-option label="全部类型" :value="null" />
              <el-option label="支付回调" :value="1" />
              <el-option label="退款回调" :value="2" />
            </el-select>
          </div>
        </template>

        <el-table :data="logs" stripe style="width: 100%">
          <el-table-column prop="callbackNo" label="回调流水号" min-width="180" />
          <el-table-column prop="callbackType" label="回调类型" width="120">
            <template #default="{ row }">
              <el-tag :type="getCallbackTypeColor(row.callbackType)" size="small">
                {{ getCallbackTypeText(row.callbackType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="businessNo" label="业务单号" min-width="180" />
          <el-table-column prop="orderNo" label="订单编号" min-width="150" />
          <el-table-column prop="requestData" label="请求数据" min-width="200" show-overflow-tooltip />
          <el-table-column prop="responseData" label="响应数据" min-width="200" show-overflow-tooltip />
          <el-table-column prop="callbackStatus" label="回调状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.callbackStatus)" size="small">
                {{ getStatusText(row.callbackStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="errorMsg" label="错误信息" min-width="150" show-overflow-tooltip />
          <el-table-column prop="retryCount" label="重试次数" width="80" />
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column prop="updateTime" label="更新时间" width="180" />
        </el-table>

        <el-empty v-if="logs.length === 0" description="暂无回调日志" />
      </el-card>
    </div>
  </div>
</template>

<script>
import { callbackApi } from '@/api'

export default {
  name: 'CallbackLogs',
  data() {
    return {
      loading: false,
      logs: [],
      filterType: null
    }
  },
  mounted() {
    this.loadLogs()
  },
  methods: {
    async loadLogs() {
      this.loading = true
      try {
        const res = await callbackApi.getList(this.filterType)
        if (res.code === 200) {
          this.logs = res.data || []
        }
      } catch (e) {
        this.$message.error('加载回调日志失败')
      } finally {
        this.loading = false
      }
    },
    getCallbackTypeText(type) {
      const map = { 1: '支付回调', 2: '退款回调' }
      return map[type] || '未知'
    },
    getCallbackTypeColor(type) {
      const map = { 1: 'primary', 2: 'warning' }
      return map[type] || 'info'
    },
    getStatusText(status) {
      const map = { 0: '待处理', 1: '成功', 2: '失败' }
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
.callback-logs-page {
  padding: 20px 0;
  background: #f5f7fa;
  min-height: 100vh;
}

.container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: 500;
}
</style>
