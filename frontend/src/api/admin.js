import request from './request'

export const adminApi = {
  listTickets(params) {
    return request.get('/admin/tickets', { params })
  },
  getTicket(id) {
    return request.get(`/admin/tickets/${id}`)
  },
  assign(data) {
    return request.post('/admin/tickets/assign', data)
  },
  transfer(data, operatorId) {
    return request.post('/admin/tickets/transfer', data, { params: { operatorId } })
  },
  resolve(ticketId, operatorId) {
    return request.post('/admin/tickets/resolve', null, { params: { ticketId, operatorId } })
  },
  close(ticketId, operatorId) {
    return request.post('/admin/tickets/close', null, { params: { ticketId, operatorId } })
  },
  getDashboard() {
    return request.get('/admin/stats/dashboard')
  },
  listAgents() {
    return request.get('/admin/agents')
  }
}
