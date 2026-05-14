<template>
  <div class="system-config-page">
    <div class="container">
      <el-card v-loading="loading">
        <template #header>
          <div class="card-header">
            <span>系统配置</span>
            <el-button type="primary" size="small" @click="loadConfig">
              刷新
            </el-button>
          </div>
        </template>

        <el-table :data="configList" style="width: 100%">
          <el-table-column prop="configKey" label="配置键" min-width="200" />
          <el-table-column prop="configValue" label="配置值" min-width="200">
            <template #default="{ row }">
              <el-input
                v-model="row.configValue"
                size="small"
                @change="handleValueChange(row)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="configDesc" label="配置描述" min-width="300" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button
                type="primary"
                size="small"
                @click="saveConfig(row)"
                :loading="row.saving"
              >
                保存
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card v-loading="processing" style="margin-top: 20px">
        <template #header>
          <div class="card-header">
            <span>手动操作</span>
          </div>
        </template>

        <div class="action-buttons">
          <el-button type="warning" @click="processTimeoutRefund">
            手动处理超时退款
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
import { configApi, refundApi } from '@/api'

export default {
  name: 'SystemConfig',
  data() {
    return {
      loading: false,
      processing: false,
      configList: []
    }
  },
  mounted() {
    this.loadConfig()
  },
  methods: {
    async loadConfig() {
      this.loading = true
      try {
        const res = await configApi.getList()
        if (res.code === 200) {
          this.configList = Object.entries(res.data).map(([key, value]) => ({
            configKey: key,
            configValue: value,
            configDesc: this.getConfigDesc(key),
            saving: false
          }))
        }
      } catch (e) {
        this.$message.error('加载配置失败')
      } finally {
        this.loading = false
      }
    },
    getConfigDesc(key) {
      const descMap = {
        'refund.timeout.hours': '退款申请超时时间（小时），超时后自动驳回',
        'refund.timeout.enabled': '是否启用退款超时自动驳回功能（true/false）'
      }
      return descMap[key] || key
    },
    handleValueChange(row) {
      row.modified = true
    },
    async saveConfig(row) {
      row.saving = true
      try {
        await configApi.update({
          configKey: row.configKey,
          configValue: row.configValue,
          configDesc: this.getConfigDesc(row.configKey)
        })
        this.$message.success('配置保存成功')
        row.modified = false
      } catch (e) {
        this.$message.error('配置保存失败')
      } finally {
        row.saving = false
      }
    },
    async processTimeoutRefund() {
      this.processing = true
      try {
        await refundApi.processTimeout()
        this.$message.success('超时退款处理完成')
      } catch (e) {
        this.$message.error('处理超时退款失败')
      } finally {
        this.processing = false
      }
    }
  }
}
</script>

<style scoped>
.system-config-page {
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

.action-buttons {
  padding: 10px 0;
}
</style>
