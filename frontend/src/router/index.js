import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/customer/tickets' },
  {
    path: '/customer',
    component: () => import('../layouts/CustomerLayout.vue'),
    children: [
      { path: 'tickets', component: () => import('../views/customer/TicketList.vue') },
      { path: 'tickets/create', component: () => import('../views/customer/TicketCreate.vue') },
      { path: 'tickets/:id', component: () => import('../views/customer/TicketDetail.vue') },
    ]
  },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    children: [
      { path: 'tickets', component: () => import('../views/admin/TicketList.vue') },
      { path: 'tickets/:id', component: () => import('../views/admin/TicketDetail.vue') },
      { path: 'stats', component: () => import('../views/admin/StatsDashboard.vue') },
      { path: 'agents', component: () => import('../views/admin/AgentList.vue') },
    ]
  }
]

export default createRouter({ history: createWebHistory(), routes })
