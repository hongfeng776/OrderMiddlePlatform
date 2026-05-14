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
            <template #header>
              <div class="card-header">
                <span>选择商品</span>
                <el-input
                  v-model="searchKeyword"
                  placeholder="搜索商品名称"
                  style="width: 250px"
                  clearable
                  prefix-icon="Search"
                />
              </div>
            </template>
            
            <el-alert
              title="请至少选择一件商品"
              type="warning"
              :closable="false"
              style="margin-bottom: 15px"
              v-show="selectedItems.length === 0"
            />
            
            <el-table
              ref="productTableRef"
              :data="filteredProducts"
              stripe
              border
              @selection-change="handleSelectionChange"
              style="margin-bottom: 20px"
            >
              <el-table-column type="selection" width="55"></el-table-column>
              <el-table-column prop="productName" label="商品名称" min-width="200"></el-table-column>
              <el-table-column prop="price" label="价格" width="120">
                <template #default="scope">
                  <span style="color: #f56c6c; font-weight: bold">¥{{ scope.row.price }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="stockNum" label="库存" width="100">
                <template #default="scope">
                  <el-tag v-if="scope.row.stockNum < 10" type="danger">仅剩{{ scope.row.stockNum }}</el-tag>
                  <span v-else>{{ scope.row.stockNum }}</span>
                </template>
              </el-table-column>
              <el-table-column label="购买数量" width="150">
                <template #default="scope">
                  <el-input-number
                    v-model="scope.row.buyCount"
                    :min="1"
                    :max="scope.row.stockNum"
                    size="small"
                    @change="handleQuantityChange(scope.row)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="小计" width="120">
                <template #default="scope">
                  <span style="color: #f56c6c; font-weight: bold">
                    ¥{{ (scope.row.price * scope.row.buyCount).toFixed(2) }}
                  </span>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
          
          <el-card style="margin-top: 20px">
            <template #header>
              <div class="card-header">
                <span>收货信息</span>
              </div>
            </template>
            
            <el-form
              ref="orderFormRef"
              :model="orderForm"
              :rules="orderRules"
              label-width="100px"
            >
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="收货人" prop="receiverName">
                    <el-select
                      v-model="orderForm.receiverName"
                      placeholder="选择或输入收货人"
                      filterable
                      allow-create
                      default-first-option
                      style="width: 100%"
                      @change="handleAddressSelect"
                    >
                      <el-option
                        v-for="(addr, index) in savedAddresses"
                        :key="index"
                        :label="addr.name"
                        :value="addr.name"
                      >
                        <div>{{ addr.name }} - {{ addr.phone }}</div>
                        <div style="font-size: 12px; color: #999">{{ addr.address }}</div>
                      </el-option>
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="联系电话" prop="receiverPhone">
                    <el-input v-model="orderForm.receiverPhone" placeholder="请输入联系电话" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="收货地址" prop="receiverAddress">
                <el-input
                  v-model="orderForm.receiverAddress"
                  type="textarea"
                  :rows="2"
                  placeholder="请输入收货地址"
                />
              </el-form-item>
              <el-form-item label="订单备注">
                <el-input
                  v-model="orderForm.remark"
                  type="textarea"
                  :rows="2"
                  placeholder="请输入订单备注（可选）"
                />
              </el-form-item>
            </el-form>
          </el-card>
          
          <div class="order-footer">
            <div class="total-info">
              <span>已选 {{ selectedItems.length }} 件商品</span>
              <span class="total-price">合计：¥{{ totalAmount }}</span>
            </div>
            <el-button type="primary" size="large" :loading="submitting" @click="showConfirmDialog">
              提交订单
            </el-button>
          </div>
        </el-main>
      </el-container>
    </el-container>
    
    <el-dialog
      v-model="confirmDialogVisible"
      title="确认订单"
      width="600px"
      :close-on-click-modal="false"
    >
      <div v-if="confirmOrderData">
        <el-descriptions :column="2" border style="margin-bottom: 20px">
          <el-descriptions-item label="收货人">{{ confirmOrderData.receiverName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ confirmOrderData.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item :span="2" label="收货地址">{{ confirmOrderData.receiverAddress }}</el-descriptions-item>
          <el-descriptions-item label="订单金额">
            <span style="color: #f56c6c; font-weight: bold; font-size: 18px">¥{{ confirmOrderData.totalAmount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="商品数量">{{ confirmOrderData.itemCount }} 件</el-descriptions-item>
        </el-descriptions>
        
        <h4 style="margin-bottom: 10px">商品清单</h4>
        <el-table :data="confirmOrderData.items" border size="small">
          <el-table-column prop="productName" label="商品名称"></el-table-column>
          <el-table-column prop="price" label="单价" width="100">
            <template #default="scope">¥{{ scope.row.price }}</template>
          </el-table-column>
          <el-table-column prop="buyCount" label="数量" width="80"></el-table-column>
          <el-table-column label="小计" width="100">
            <template #default="scope">
              ¥{{ (scope.row.price * scope.row.buyCount).toFixed(2) }}
            </template>
          </el-table-column>
        </el-table>
      </div>
      
      <template #footer>
        <el-button @click="confirmDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitOrder">确认下单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { computed, onMounted, reactive, toRefs } from 'vue'
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'
import { inventoryApi, orderApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
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
      searchKeyword: '',
      selectedItems: [],
      confirmDialogVisible: false,
      confirmOrderData: null,
      savedAddresses: [
        { name: '张三', phone: '13800138001', address: '北京市朝阳区某某街道123号' },
        { name: '李四', phone: '13800138002', address: '上海市浦东新区某某路456号' },
        { name: '王五', phone: '13800138003', address: '广州市天河区某某大道789号' }
      ],
      orderForm: {
        receiverName: '',
        receiverPhone: '',
        receiverAddress: '',
        remark: ''
      },
      orderRules: {
        receiverName: [
          { required: true, message: '请输入收货人姓名', trigger: 'blur' },
          { min: 2, max: 20, message: '姓名长度在 2 到 20 个字符', trigger: 'blur' }
        ],
        receiverPhone: [
          { required: true, message: '请输入联系电话', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
        ],
        receiverAddress: [
          { required: true, message: '请输入收货地址', trigger: 'blur' },
          { min: 5, max: 200, message: '地址长度在 5 到 200 个字符', trigger: 'blur' }
        ]
      }
    })
    
    const userInfo = computed(() => store.state.user)
    const activeMenu = computed(() => route.path)
    
    const filteredProducts = computed(() => {
      if (!state.searchKeyword) {
        return state.products
      }
      return state.products.filter(p => 
        p.productName.toLowerCase().includes(state.searchKeyword.toLowerCase())
      )
    })
    
    const totalAmount = computed(() => {
      return state.selectedItems.reduce((total, item) => {
        return total + item.price * item.buyCount
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
    
    const handleQuantityChange = (row) => {
    }
    
    const handleAddressSelect = (name) => {
      const address = state.savedAddresses.find(a => a.name === name)
      if (address) {
        state.orderForm.receiverPhone = address.phone
        state.orderForm.receiverAddress = address.address
      }
    }
    
    const showConfirmDialog = async () => {
      if (state.selectedItems.length === 0) {
        ElMessage.warning('请至少选择一件商品')
        return
      }
      
      const valid = await validateForm()
      if (!valid) {
        ElMessage.warning('请填写完整的收货信息')
        return
      }
      
      for (const item of state.selectedItems) {
        if (item.buyCount > item.stockNum) {
          ElMessage.warning(`商品 ${item.productName} 库存不足，当前库存: ${item.stockNum}`)
          return
        }
      }
      
      state.confirmOrderData = {
        receiverName: state.orderForm.receiverName,
        receiverPhone: state.orderForm.receiverPhone,
        receiverAddress: state.orderForm.receiverAddress,
        totalAmount: totalAmount.value,
        itemCount: state.selectedItems.length,
        items: [...state.selectedItems]
      }
      
      state.confirmDialogVisible = true
    }
    
    const validateForm = () => {
      return new Promise((resolve) => {
        const { receiverName, receiverPhone, receiverAddress } = state.orderForm
        const phoneReg = /^1[3-9]\d{9}$/
        
        if (!receiverName || receiverName.length < 2 || receiverName.length > 20) {
          resolve(false)
          return
        }
        
        if (!receiverPhone || !phoneReg.test(receiverPhone)) {
          resolve(false)
          return
        }
        
        if (!receiverAddress || receiverAddress.length < 5 || receiverAddress.length > 200) {
          resolve(false)
          return
        }
        
        resolve(true)
      })
    }
    
    const submitOrder = async () => {
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
          receiverName: state.orderForm.receiverName,
          receiverPhone: state.orderForm.receiverPhone,
          receiverAddress: state.orderForm.receiverAddress,
          remark: state.orderForm.remark,
          items: items
        })
        
        state.confirmDialogVisible = false
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
      handleQuantityChange,
      handleAddressSelect,
      showConfirmDialog,
      submitOrder,
      filteredProducts,
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
  background: linear-gradient(135deg, #667eea 0, #764ba2 100%);
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
