<template>
  <div class="refund-statistics-page">
    <div class="container">
      <el-card v-loading="loading">
        <template #header>
          <div class="card-header">
            <span>退款原因统计</span>
            <el-button type="primary" size="small" @click="loadStatistics">
              刷新
            </el-button>
          </div>
        </template>

        <div class="filter-area">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            size="small"
            style="width: 400px"
            @change="loadStatistics"
          />
          <el-button type="primary" size="small" @click="dateRange = null; loadStatistics()">
            清除筛选
          </el-button>
        </div>

        <div v-if="statistics.length > 0" class="summary">
          <el-statistic title="总退款单数" :value="totalCount" />
          <el-statistic title="总退款金额">
            <template #formatter>
              ¥{{ totalAmount }}
            </template>
          </el-statistic>
          <el-statistic title="退款原因种类" :value="statistics.length" />
        </div>

        <el-table :data="statistics" style="width: 100%; margin-top: 20px">
          <el-table-column prop="refundReason" label="退款原因" min-width="200" />
          <el-table-column prop="count" label="退款数量" width="120" />
          <el-table-column prop="totalAmount" label="退款总金额" width="150">
            <template #default="{ row }">
              ¥{{ row.totalAmount }}
            </template>
          </el-table-column>
          <el-table-column prop="percentage" label="占比" width="120">
            <template #default="{ row }">
              <el-progress
                :percentage="parseFloat(row.percentage)"
                :stroke-width="12"
              />
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="statistics.length === 0" description="暂无统计数据" />
      </el-card>
    </div>
  </div>
</template>

<script>
import { refundApi } from '@/api'

export default {
  name: 'RefundStatistics',
  data() {
    return {
      loading: false,
      dateRange: null,
      statistics: []
    }
  },
  computed: {
    totalCount() {
      return this.statistics.reduce((sum, item) => sum + item.count, 0)
    },
    totalAmount() {
      return this.statistics
        .reduce((sum, item) => sum + parseFloat(item.totalAmount), 0)
        .toFixed(2)
    }
  },
  mounted() {
    this.loadStatistics()
  },
  methods: {
    async loadStatistics() {
      this.loading = true
      try {
        let startTime = ''
        let endTime = ''
        if (this.dateRange && this.dateRange.length === 2) {
          startTime = this.formatDateTime(this.dateRange[0])
          endTime = this.formatDateTime(this.dateRange[1])
        }
        const res = await refundApi.getStatistics(startTime, endTime)
        if (res.code === 200) {
          this.statistics = res.data || []
        }
      } catch (e) {
        this.$message.error('加载统计数据失败')
      } finally {
        this.loading = false
      }
    },
    formatDateTime(date) {
      if (!date) return ''
      const d = new Date(date)
      const pad = (n) => n.toString().padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
    }
  }
}
</script>

<style scoped>
.refund-statistics-page {
  padding: 20px 0;
  background: #f5f7fa;
  min-height: 100vh;
}

.container {
  max-width: 1200px;
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

.filter-area {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 20px;
}

.summary {
  display: flex;
  gap: 40px;
  padding: 20px;
  background: #f9fafb;
  border-radius: 8px;
}
</style>
