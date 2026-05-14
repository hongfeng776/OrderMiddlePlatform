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

export const inventoryLockApi = {
  list(page, size, productId, orderNo, lockStatus) {
    return request({
      url: '/inventory-lock/list',
      method: 'get',
      params: { page, size, productId, orderNo, lockStatus }
    })
  },
  getById(id) {
    return request({
      url: `/inventory-lock/${id}`,
      method: 'get'
    })
  },
  create(data) {
    return request({
      url: '/inventory-lock/create',
      method: 'post',
      data
    })
  },
  confirm(orderNo) {
    return request({
      url: `/inventory-lock/confirm/${orderNo}`,
      method: 'post'
    })
  },
  releaseByOrderNo(orderNo, data) {
    return request({
      url: `/inventory-lock/release-order/${orderNo}`,
      method: 'post',
      data
    })
  },
  manualRelease(id, data) {
    return request({
      url: `/inventory-lock/manual-release/${id}`,
      method: 'post',
      data
    })
  },
  releaseExpired() {
    return request({
      url: '/inventory-lock/release-expired',
      method: 'post'
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
    },
    adminListOrders(orderStatus, startTime, endTime, userId, orderNo) {
        return request({
            url: '/order/admin/list',
            method: 'get',
            params: { orderStatus, startTime, endTime, userId, orderNo }
        })
    },
    batchShip(orderNos, remark, operatorId, operatorName) {
        return request({
            url: '/order/admin/batch-ship',
            method: 'post',
            data: { orderNos, remark, operatorId, operatorName }
        })
    },
    batchCancel(orderNos, remark, operatorId, operatorName) {
        return request({
            url: '/order/admin/batch-cancel',
            method: 'post',
            data: { orderNos, remark, operatorId, operatorName }
        })
    },
    exportOrders(data) {
        return request({
            url: '/order/admin/export',
            method: 'post',
            data
        })
    },
    downloadExport(objectName) {
        return request({
            url: '/order/admin/export/download',
            method: 'get',
            params: { objectName },
            responseType: 'blob'
        })
    },
    getBatchLogs(operationType) {
        return request({
            url: '/order/admin/batch-logs',
            method: 'get',
            params: { operationType }
        })
    },
    getBatchDetails(batchNo) {
        return request({
            url: `/order/admin/batch-details/${batchNo}`,
            method: 'get'
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
  getProgress(refundNo, userId) {
    return request({
      url: `/payment/refund/progress/${refundNo}?userId=${userId}`,
      method: 'get'
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

export const refundApi = {
  apply(data) {
    return request({
      url: '/payment/refund/apply',
      method: 'post',
      data
    })
  },
  getProgress(refundNo, userId) {
    return request({
      url: `/payment/refund/progress/${refundNo}?userId=${userId}`,
      method: 'get'
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
  export(userId) {
    return request({
      url: `/payment/refund/export?userId=${userId || ''}`,
      method: 'get',
      responseType: 'blob'
    })
  },
  getStatistics(startTime, endTime) {
    return request({
      url: `/payment/refund/statistics/reason?startTime=${startTime || ''}&endTime=${endTime || ''}`,
      method: 'get'
    })
  },
  processTimeout() {
    return request({
      url: '/payment/refund/process-timeout',
      method: 'post'
    })
  }
}

export const configApi = {
  getList() {
    return request({
      url: '/payment/config/list',
      method: 'get'
    })
  },
  update(data) {
    return request({
      url: '/payment/config/update',
      method: 'post',
      data
    })
  }
}

export const scheduleJobApi = {
  getJobList() {
    return request({
      url: '/schedule/job/list',
      method: 'get'
    })
  },
  getJobLogs(page, size, jobHandler, status) {
    return request({
      url: '/schedule/job/logs',
      method: 'get',
      params: { page, size, jobHandler, status }
    })
  },
  getRecentLogs(jobHandler, limit) {
    return request({
      url: '/schedule/job/logs/recent',
      method: 'get',
      params: { jobHandler, limit }
    })
  },
  getJobStatistics(jobHandler) {
    return request({
      url: '/schedule/job/statistics',
      method: 'get',
      params: { jobHandler }
    })
  },
  triggerJob(jobHandler, executorParam) {
    return request({
      url: '/schedule/job/trigger',
      method: 'post',
      data: { jobHandler, executorParam }
    })
  },
  stopJob(jobId) {
    return request({
      url: `/schedule/job/stop/${jobId}`,
      method: 'post'
    })
  },
  exportLogs(jobHandler, status) {
    const params = new URLSearchParams()
    if (jobHandler) params.append('jobHandler', jobHandler)
    if (status != null) params.append('status', status)
    const url = `/api/schedule/job/logs/export?${params.toString()}`
    window.open(url, '_blank')
  }
}

export const notificationApi = {
  list(page, size, userId, type) {
    return request({
      url: '/notification/list',
      method: 'get',
      params: { page, size, userId, type }
    })
  },
  getUnreadCount(userId) {
    return request({
      url: '/notification/unread/count',
      method: 'get',
      params: { userId }
    })
  },
  markAsRead(id) {
    return request({
      url: `/notification/read/${id}`,
      method: 'post'
    })
  },
  markBatchAsRead(userId, ids) {
    return request({
      url: '/notification/read/batch',
      method: 'post',
      data: { userId, ids }
    })
  },
  markAllAsRead(userId) {
    return request({
      url: '/notification/read/all',
      method: 'post',
      data: { userId }
    })
  }
}
