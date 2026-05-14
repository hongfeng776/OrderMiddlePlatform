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
  paySuccess(orderNo, remark) {
    return request({
      url: `/order/pay-success/${orderNo}`,
      method: 'post',
      data: remark ? { remark } : null
    })
  },
  ship(orderNo, remark) {
    return request({
      url: `/order/ship/${orderNo}`,
      method: 'post',
      data: remark ? { remark } : null
    })
  },
  complete(orderNo, remark) {
    return request({
      url: `/order/complete/${orderNo}`,
      method: 'post',
      data: remark ? { remark } : null
    })
  },
  cancel(orderNo, remark) {
    return request({
      url: `/order/cancel/${orderNo}`,
      method: 'post',
      data: remark ? { remark } : null
    })
  },
  getStatusHistory(orderNo) {
    return request({
      url: `/order/status-history/${orderNo}`,
      method: 'get'
    })
  },
  getNotifications(userId) {
    return request({
      url: `/order/notifications/${userId}`,
      method: 'get'
    })
  },
  getUnreadCount(userId) {
    return request({
      url: `/order/notifications/unread-count/${userId}`,
      method: 'get'
    })
  },
  markAsRead(id) {
    return request({
      url: `/order/notifications/mark-read/${id}`,
      method: 'post'
    })
  },
  markBatchAsRead(userId, ids) {
    return request({
      url: `/order/notifications/mark-batch-read/${userId}`,
      method: 'post',
      data: { ids }
    })
  },
  markAllAsRead(userId) {
    return request({
      url: `/order/notifications/mark-all-read/${userId}`,
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
  process(payNo, success) {
    return request({
      url: `/payment/process/${payNo}?success=${success}`,
      method: 'post'
    })
  },
  getByOrderNo(orderNo) {
    return request({
      url: `/payment/order/${orderNo}`,
      method: 'get'
    })
  },
  getUserPayments(userId) {
    return request({
      url: `/payment/user/${userId}`,
      method: 'get'
    })
  },
  mockPaySuccess(payNo) {
    return request({
      url: `/payment/mock/pay-success?payNo=${payNo}`,
      method: 'post'
    })
  },
  mockPayFail(payNo) {
    return request({
      url: `/payment/mock/pay-fail?payNo=${payNo}`,
      method: 'post'
    })
  }
}

export const refundApi = {
  apply(data) {
    return request({
      url: '/payment/refund/apply',
      method: 'post',
      data
    })
  },
  audit(data) {
    return request({
      url: '/payment/refund/audit',
      method: 'post',
      data
    })
  },
  getUserRefunds(userId) {
    return request({
      url: `/payment/refund/user/${userId}`,
      method: 'get'
    })
  },
  getPendingAudit() {
    return request({
      url: '/payment/refund/pending-audit',
      method: 'get'
    })
  },
  getDetail(refundNo) {
    return request({
      url: `/payment/refund/${refundNo}`,
      method: 'get'
    })
  },
  mockRefundSuccess(refundNo) {
    return request({
      url: `/payment/mock/refund-success?refundNo=${refundNo}`,
      method: 'post'
    })
  },
  mockRefundFail(refundNo) {
    return request({
      url: `/payment/mock/refund-fail?refundNo=${refundNo}`,
      method: 'post'
    })
  }
}

export const callbackApi = {
  getList(callbackType) {
    return request({
      url: `/payment/callback/list?callbackType=${callbackType || ''}`,
      method: 'get'
    })
  }
}
