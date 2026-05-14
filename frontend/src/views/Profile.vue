<template>
  <div class="profile-container">
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
          <h2 class="page-title">个人中心</h2>
          
          <el-card>
            <template #header>
              <div class="card-header">
                <span>个人信息</span>
              </div>
            </template>
            
            <el-descriptions :column="2" border>
              <el-descriptions-item label="用户ID">{{ userInfo.userId }}</el-descriptions-item>
              <el-descriptions-item label="用户名">{{ userInfo.username }}</el-descriptions-item>
              <el-descriptions-item label="昵称">{{ userInfo.nickname || '-' }}</el-descriptions-item>
              <el-descriptions-item label="登录状态">
                <el-tag type="success">已登录</el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
          
          <el-card style="margin-top: 20px">
            <template #header>
              <div class="card-header">
                <span>账号管理</span>
              </div>
            </template>
            
            <el-form label-width="100px">
              <el-form-item label="修改密码">
                <el-button type="primary">修改密码</el-button>
              </el-form-item>
              <el-form-item label="退出登录">
                <el-button type="danger" @click="logout">安全退出</el-button>
              </el-form-item>
            </el-form>
          </el-card>
          
          <el-card style="margin-top: 20px">
            <template #header>
              <div class="card-header">
                <span>系统说明</span>
              </div>
            </template>
            
            <el-alert
              title="订单履约中台"
              type="info"
              :closable="false"
              show-icon
              description="这是一个基于 Spring Cloud 和 Vue3 的分布式电商订单履约中台演示项目，包含用户、订单、库存、支付四个微服务，实现了完整的下单、库存扣减、支付流程。"
            >
            </el-alert>
            
            <div style="margin-top: 20px">
              <h4>技术栈：</h4>
              <ul style="margin-top: 10px; line-height: 2; color: #666">
                <li>后端：Spring Boot 2.7 + Spring Cloud Alibaba (Nacos + OpenFeign)</li>
                <li>持久层：MyBatis-Plus + MySQL</li>
                <li>前端：Vue3 + Element Plus + Vuex + Vue Router</li>
                <li>架构：微服务架构，前后端分离</li>
              </ul>
            </div>
          </el-card>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
import { computed } from 'vue'
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'
import { HomeFilled, List, Plus, User } from '@element-plus/icons-vue'

export default {
  name: 'Profile',
  components: { HomeFilled, List, Plus, User },
  setup() {
    const store = useStore()
    const router = useRouter()
    const route = useRoute()
    
    const userInfo = computed(() => store.state.user)
    const activeMenu = computed(() => route.path)
    
    const logout = () => {
      store.dispatch('logout')
      router.push('/login')
    }
    
    return {
      userInfo,
      activeMenu,
      logout
    }
  }
}
</script>

<style scoped>
.profile-container {
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

.page-title {
  margin-bottom: 20px;
  color: #333;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}
</style>
