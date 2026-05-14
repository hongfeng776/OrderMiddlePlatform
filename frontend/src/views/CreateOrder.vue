<template>
  <div class="create-order-container">
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
          <h2 class="page-title">创建订单</h2>
          
          <el-card>
            <h3 style="margin-bottom: 15px">选择商品</h3>
            <el-table :data="products" stripe border @selection-change="handleSelectionChange">
              <el-table-column type="selection" width="55"></el-table-column>
              <el-table-column prop="productName" label="商品名称"></el-table-column>
              <el-table-column prop="price" label="价格" width="120">
                <template #default="scope">¥{{ scope.row.price }}</template>
              </el-table-column>
              <el-table-column prop="stockNum" label="库存" width="100"></el-table-column>
              <el-table-column label="购买数量" width="150">
                <template #default="scope">
                  <el-input-number v-model="scope.row.buyCount" :min="1" :max="scope.row.stockNum" size="small"></el-input-number>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
          
          <el-card style="margin-top: 20px">
            <h3 style="margin-bottom: 15px">收货信息</h3>
            <el-form :model="receiverForm" label-width="100px">
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="收货人">
                    <el-input v-model="receiverForm.name" placeholder="请输入收货人姓名"></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="联系电话">
                    <el-input v-model="receiverForm.phone" placeholder="请输入联系电话"></el-input>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="收货地址">
                <el-input v-model="receiverForm.address" type="textarea" :rows="2" placeholder="请输入收货地址"></el-input>
              </el-form-item>
              <el-form-item label="订单备注">
                <el-input v-model="receiverForm.remark" type="textarea" :rows="2" placeholder="请输入订单备注（可选）"></el-input>
              </el-form-item>
            </el-form>
          </el-card>
          
          <div class="order-footer">
            <div class="total-info">
              <span>已选 {{ selectedItems.length }} 件商品</span>
              <span class="total-price">合计：¥{{ totalAmount }}</span>
            </div>
            <el-button type="primary" size="large" :loading="submitting" @click="submitOrder">提交订单</el-button>
          </div>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
import { computed, onMounted, reactive, toRefs } from 'vue'
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'
import { inventoryApi, orderApi } from '@/api'
import { ElMessage } from 'element-plus'
import { HomeFilled, List, Plus, User } from '@element-plus/icons-vue'

export default {
  name: 'CreateOrder',
  components: { HomeFilled, List, Plus, User },
  setup() {
    const store = useStore()
    const router = useRouter()
    const route = useRoute()
    
    const state = reactive({
      loading: false,
      submitting: false,
      products: [],
      selectedItems: [],
      receiverForm: {
        name: '',
        phone: '',
        address: '',
        remark: ''
      }
    })
    
    const userInfo = computed(() => store.state.user)
    const activeMenu = computed(() => route.path)
    
    const totalAmount = computed(() => {
      return state.selectedItems.reduce((total, item) => {
        return total + item.price * (item.buyCount || 1)
      }, 0).toFixed(2)
    })
    
    const loadProducts = async () => {
      state.loading = true
      try {
        const res = await inventoryApi.list()
        state.products = res.data.map(item => ({
          ...item,
          buyCount: 1
        }))
      } catch (error) {
        console.error('加载商品失败:', error)
      } finally {
        state.loading = false
      }
    }
    
    const handleSelectionChange = (selection) => {
      state.selectedItems = selection
    }
    
    const submitOrder = async () => {
      if (state.selectedItems.length === 0) {
        ElMessage.warning('请选择商品')
        return
      }
      
      if (!state.receiverForm.name || !state.receiverForm.phone || !state.receiverForm.address) {
        ElMessage.warning('请填写完整的收货信息')
        return
      }
      
      state.submitting = true
      try {
        const items = state.selectedItems.map(item => ({
          productId: item.productId,
          productName: item.productName,
          productImage: item.productImage,
          productPrice: item.price,
          buyCount: item.buyCount
        }))
        
        const res = await orderApi.create({
          userId: userInfo.value.userId,
          receiverName: state.receiverForm.name,
          receiverPhone: state.receiverForm.phone,
          receiverAddress: state.receiverForm.address,
          remark: state.receiverForm.remark,
          items: items
        })
        
        ElMessage.success('订单创建成功')
        router.push('/orders')
      } catch (error) {
        console.error('创建订单失败:', error)
      } finally {
        state.submitting = false
      }
    }
    
    const logout = () => {
      store.dispatch('logout')
      router.push('/login')
    }
    
    onMounted(() => {
      loadProducts()
    })
    
    return {
      userInfo,
      activeMenu,
      logout,
      handleSelectionChange,
      submitOrder,
      totalAmount,
      ...toRefs(state)
    }
  }
}
</script>

<style scoped>
.create-order-container {
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

.order-footer {
  margin-top: 20px;
  padding: 20px;
  background: white;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.total-info {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 16px;
}

.total-price {
  font-size: 24px;
  font-weight: bold;
  color: #f56c6c;
}
</style>
