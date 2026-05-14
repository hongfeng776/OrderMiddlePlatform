import request from '@/utils/request'

export const userApi = {
  login(data) {
    return request({
      url: '/user/login',
      method: 'post',
      data
    })
  },
  getUserInfo(id) {
    return request({
      url: `/user/${id}`,
      method: 'get'
    })
  }
}

export const inventoryApi = {
  list() {
    return request({
      url: '/inventory/list',
      method: 'get'
    })
  },
  getByProductId(productId) {
    return request({
      url: `/inventory/${productId}`,
      method: 'get'
    })
  }
}

export const orderApi = {
  create(data) {
    return request({
      url: '/order/create',
      method: 'post',
      data
    })
  },
  listByUserId(userId) {
    return request({
      url: `/order/list/${userId}`,
      method: 'get'
    })
  },
  getDetail(orderNo) {
    return request({
      url: `/order/${orderNo}`,
      method: 'get'
    })
  },
  paySuccess(orderNo) {
    return request({
      url: `/order/pay-success/${orderNo}`,
      method: 'post'
    })
  }
}

export const paymentApi = {
  create(data) {
    return request({
      url: '/payment/create',
      method: 'post',
      data
    })
  },
  process(payNo) {
    return request({
      url: `/payment/process/${payNo}`,
      method: 'post'
    })
  }
}
