import request from './request'

export const customerApi = {
  create(data, idempotentKey) {
    return request.post('/customer/tickets', data, {
      headers: idempotentKey ? { 'Idempotent-Key': idempotentKey } : {}
    })
  },
  list(customerId, pageNum = 1, pageSize = 10) {
    return request.get('/customer/tickets', { params: { customerId, pageNum, pageSize } })
  },
  getById(id) {
    return request.get(`/customer/tickets/${id}`)
  },
  close(id, customerId, satisfaction) {
    return request.post(`/customer/tickets/${id}/close`, null, {
      params: { customerId, satisfaction }
    })
  },
  search(params) {
    return request.get('/customer/tickets/search', { params })
  }
}
