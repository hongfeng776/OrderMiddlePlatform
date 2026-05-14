import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import store from '@/store'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/orders',
    name: 'Orders',
    component: () => import('@/views/Orders.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/order/:orderNo',
    name: 'OrderDetail',
    component: () => import('@/views/OrderDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('@/views/Notifications.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/create-order',
    name: 'CreateOrder',
    component: () => import('@/views/CreateOrder.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/pay/:payNo',
    name: 'Payment',
    component: () => import('@/views/Payment.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/payments',
    name: 'Payments',
    component: () => import('@/views/Payments.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/refunds',
    name: 'Refunds',
    component: () => import('@/views/Refunds.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/refund-audit',
    name: 'RefundAudit',
    component: () => import('@/views/RefundAudit.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/callback-logs',
    name: 'CallbackLogs',
    component: () => import('@/views/CallbackLogs.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/system-config',
    name: 'SystemConfig',
    component: () => import('@/views/SystemConfig.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/refund-statistics',
    name: 'RefundStatistics',
    component: () => import('@/views/RefundStatistics.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/order-management',
    name: 'OrderManagement',
    component: () => import('@/views/OrderManagement.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/schedule-job',
    name: 'ScheduleJob',
    component: () => import('@/views/ScheduleJob.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !store.state.user.token) {
    next('/login')
  } else if (to.meta.requiresAdmin && store.state.user.role !== 'ADMIN') {
    ElMessage.error('需要管理员权限')
    next('/')
  } else {
    next()
  }
})

export default router
