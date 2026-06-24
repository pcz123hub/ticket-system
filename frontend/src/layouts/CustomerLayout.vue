<template>
  <el-container style="min-height: 100vh">
    <el-header style="background: #409eff; color: #fff; display: flex; align-items: center; padding: 0 20px;">
      <h2 style="font-size: 18px; margin-right: 40px;">客服工单系统</h2>
      <el-menu mode="horizontal" :ellipsis="false" :default-active="activeMenu"
               style="flex:1; background:transparent; border:0;"
               text-color="#fff" active-text-color="#fff">
        <el-menu-item index="/customer/tickets" @click="$router.push('/customer/tickets')">我的工单</el-menu-item>
        <el-menu-item index="/customer/tickets/create" @click="$router.push('/customer/tickets/create')">提交工单</el-menu-item>
      </el-menu>
      <el-button size="small" @click="$router.push('/admin/tickets')" style="margin-left:auto;">
        <el-icon><Setting /></el-icon> 管理后台
      </el-button>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
const route = useRoute()
const activeMenu = computed(() => route.path)
</script>
