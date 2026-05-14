<template>
  <div class="notifications-container">
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
            <el-menu-item index="/notifications" class="notification-menu-item">
              <el-icon><Bell /></el-icon>
              <span>通知中心</span>
              <el-badge v-if="unreadCount > 0" :value="unreadCount" :max="99" class="notification-badge" />
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
          <el-card v-loading="loading">
            <template #header>
              <div class="card-header">
                <span>通知中心</span>
                <div class="header-actions">
                  <el-checkbox v-model="selectAll" :indeterminate="isIndeterminate" @change="handleSelectAll">
                    全选
                  </el-checkbox>
                  <el-button 
                    type="primary" 
                    @click="handleMarkBatchRead" 
                    :disabled="selectedIds.length === 0"
                  >
                    批量已读
                    <el-badge v-if="selectedIds.length > 0" :value="selectedIds.length" :max="99" class="header-badge" />
                  </el-button>
                  <el-button type="success" @click="handleMarkAllRead" :disabled="unreadCount === 0">
                    全部已读
                    <el-badge v-if="unreadCount > 0" :value="unreadCount" :max="99" class="header-badge" />
                  </el-button>
                </div>
              </div>
            </template>
            
            <el-empty v-if="notifications.length === 0" description="暂无通知" />
            
            <div v-else class="notification-list">
              <div
                v-for="notification in notifications"
                :key="notification.id"
                class="notification-item"
                :class="{ 'is-read': notification.readStatus === 1, 'is-selected': selectedIds.includes(notification.id) }"
                @click.stop="handleNotificationClick(notification)"
              >
                <div class="notification-select" @click.stop>
                  <el-checkbox v-model="selectedIds" :label="notification.id" />
                </div>
                <div class="notification-body">
                  <div class="notification-header">
                    <el-tag size="small" :type="notification.readStatus === 0 ? 'danger' : 'info'">
                      {{ notification.readStatus === 0 ? '未读' : '已读' }}
                    </el-tag>
                    <span class="notification-time">{{ notification.createTime }}</span>
                  </div>
                  <div class="notification-title">{{ notification.title }}</div>
                  <div class="notification-content">{{ notification.content }}</div>
                  <div class="notification-order" v-if="notification.orderNo">
                    订单编号：{{ notification.orderNo }}
                  </div>
                </div>
              </div>
            </div>
          </el-card>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
import { computed, onMounted, reactive, toRefs, watch } from 'vue'
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'
import { orderApi } from '@/api'
import { ElMessage } from 'element-plus'
import { HomeFilled, List, Bell, Plus, User } from '@element-plus/icons-vue'

export default {
  name: 'Notifications',
  components: { HomeFilled, List, Bell, Plus, User },
  setup() {
    const store = useStore()
    const router = useRouter()
    const route = useRoute()

    const state = reactive({
      loading: false,
      notifications: [],
      selectedIds: [],
      selectAll: false
    })

    const userInfo = computed(() => store.state.user)
    const activeMenu = computed(() => route.path)
    const unreadCount = computed(() => store.getters.unreadCount)

    const isIndeterminate = computed(() => {
      const unreadNotifications = state.notifications.filter(n => n.readStatus === 0)
      const selectedUnreadCount = unreadNotifications.filter(n => state.selectedIds.includes(n.id)).length
      return selectedUnreadCount > 0 && selectedUnreadCount < unreadNotifications.length
    })

    const loadNotifications = async () => {
      state.loading = true
      try {
        const res = await orderApi.getNotifications(store.state.user.userId)
        state.notifications = res.data
      } catch (error) {
        ElMessage.error('加载通知失败')
      } finally {
        state.loading = false
      }
    }

    const loadUnreadCount = async () => {
      if (!store.state.user.userId) return
      try {
        const res = await orderApi.getUnreadCount(store.state.user.userId)
        store.dispatch('setUnreadCount', res.data)
      } catch (error) {
        console.error('加载未读数量失败', error)
      }
    }

    const handleNotificationClick = async (notification) => {
      if (notification.readStatus === 0) {
        try {
          await orderApi.markAsRead(notification.id)
          notification.readStatus = 1
          store.dispatch('decrementUnreadCount')
        } catch (error) {
          console.error('标记已读失败', error)
        }
      }
      if (notification.orderNo) {
        router.push(`/order/${notification.orderNo}`)
      }
    }

    const handleSelectAll = (value) => {
      if (value) {
        state.selectedIds = state.notifications
          .filter(n => n.readStatus === 0)
          .map(n => n.id)
      } else {
        state.selectedIds = []
      }
      state.selectAll = value
    }

    const handleMarkBatchRead = async () => {
      if (state.selectedIds.length === 0) {
        ElMessage.warning('请选择要标记的通知')
        return
      }
      try {
        await orderApi.markBatchAsRead(store.state.user.userId, state.selectedIds)
        state.notifications.forEach(item => {
          if (state.selectedIds.includes(item.id)) {
            item.readStatus = 1
          }
        })
        const readCount = state.selectedIds.length
        for (let i = 0; i < readCount; i++) {
          store.dispatch('decrementUnreadCount')
        }
        state.selectedIds = []
        state.selectAll = false
        ElMessage.success(`已将 ${readCount} 条通知标记为已读`)
      } catch (error) {
        ElMessage.error('操作失败')
      }
    }

    const handleMarkAllRead = async () => {
      try {
        await orderApi.markAllAsRead(store.state.user.userId)
        state.notifications.forEach(item => {
          item.readStatus = 1
        })
        store.dispatch('clearUnreadCount')
        state.selectedIds = []
        state.selectAll = false
        ElMessage.success('已全部标记为已读')
      } catch (error) {
        ElMessage.error('操作失败')
      }
    }

    const logout = () => {
      store.dispatch('logout')
      router.push('/login')
    }

    onMounted(() => {
      loadNotifications()
      loadUnreadCount()
    })

    watch(() => route.path, (newPath) => {
      if (newPath === '/notifications') {
        loadNotifications()
      }
      loadUnreadCount()
    })

    return {
      userInfo,
      activeMenu,
      unreadCount,
      isIndeterminate,
      logout,
      handleNotificationClick,
      handleSelectAll,
      handleMarkBatchRead,
      handleMarkAllRead,
      ...toRefs(state)
    }
  }
}
</script>

<style scoped>
.notifications-container {
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.header-badge {
  margin-left: 8px;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.notification-item {
  display: flex;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  border-left: 3px solid #409eff;
}

.notification-item:hover {
  background: #ecf5ff;
  transform: translateX(4px);
}

.notification-item.is-read {
  opacity: 0.6;
  border-left-color: #909399;
}

.notification-item.is-selected {
  background: #e6f7ff;
  border-left-color: #1890ff;
}

.notification-select {
  margin-right: 12px;
  display: flex;
  align-items: center;
}

.notification-body {
  flex: 1;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.notification-time {
  font-size: 12px;
  color: #909399;
}

.notification-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}

.notification-content {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.notification-order {
  font-size: 12px;
  color: #909399;
}
</style>
