<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 class="login-title">订单履约中台</h2>
      <el-form ref="loginForm" :model="loginForm" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" @keyup.enter="handleLogin"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" @click="handleLogin" :loading="loading">登录</el-button>
        </el-form-item>
      </el-form>
      <div class="tips">
        <p>测试账号: admin / 123456</p>
      </div>
    </el-card>
  </div>
</template>

<script>
import { userApi } from '@/api'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'

export default {
  name: 'Login',
  setup() {
    const store = useStore()
    const router = useRouter()
    
    const loginForm = {
      username: 'admin',
      password: '123456'
    }
    
    const loading = false
    
    const handleLogin = async () => {
      try {
        const res = await userApi.login(loginForm)
        store.dispatch('login', res.data)
        router.push('/')
      } catch (error) {
        console.error('登录失败:', error)
      }
    }
    
    return {
      loginForm,
      loading,
      handleLogin
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.login-title {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.login-btn {
  width: 100%;
}

.tips {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
  text-align: center;
  color: #999;
  font-size: 14px;
}
</style>
