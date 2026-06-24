<template>
  <el-container style="min-height: 100vh">
    <el-aside width="200px" style="background: #304156;">
      <div style="height:60px; display:flex; align-items:center; justify-content:center; color:#fff; font-size:16px; font-weight:600;">
        工单管理后台
      </div>
      <el-menu :default-active="activeMenu" background-color="#304156" text-color="#bfcbd9"
               active-text-color="#409eff" router>
        <el-menu-item index="/admin/tickets">
          <el-icon><Ticket /></el-icon><span>工单管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/agents">
          <el-icon><User /></el-icon><span>客服管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/stats">
          <el-icon><DataAnalysis /></el-icon><span>统计看板</span>
        </el-menu-item>
        <el-menu-item index="/customer/tickets" @click="$router.push('/customer/tickets')">
          <el-icon><Switch /></el-icon><span>切换客户门户</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="background:#fff; border-bottom:1px solid #e6e6e6; display:flex; align-items:center; justify-content:space-between;">
        <span style="font-size:16px; font-weight:500;">{{ pageTitle }}</span>
      </el-header>
      <el-main style="background:#f0f2f5;">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = computed(() => route.path)
const pageTitle = computed(() => {
  const map = {
    '/admin/tickets': '工单管理',
    '/admin/agents': '客服管理',
    '/admin/stats': '统计看板'
  }
  return map[route.path] || '工单管理后台'
})
</script>
