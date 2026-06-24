<template>
  <div class="page-container">
    <el-page-header @back="$router.push('/customer/tickets')" content="工单详情" style="margin-bottom:20px" />

    <el-card v-loading="loading">
      <template v-if="ticket">
        <el-descriptions title="工单信息" :column="2" border>
          <el-descriptions-item label="工单号">{{ ticket.ticketNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(ticket.status)" size="small">{{ ticket.statusDesc }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="优先级">
            <el-tag :type="ticket.priority === 0 ? 'danger' : ticket.priority === 1 ? 'warning' : 'info'" size="small">
              {{ ticket.priorityDesc }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="业务类型">{{ ticket.category }}</el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ ticket.title }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ ticket.description || '暂无' }}</el-descriptions-item>
          <el-descriptions-item label="处理人">{{ ticket.assigneeName || ticket.assigneeId || '未分配' }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ ticket.source }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ ticket.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="关闭时间">{{ ticket.closedAt || '未关闭' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="ticket.status === 2" style="margin-top:20px; text-align:center;">
          <el-input-number v-model="satisfaction" :min="1" :max="5" />&nbsp;
          <el-button type="primary" @click="handleClose" :loading="closing">确认关闭 (评分)</el-button>
        </div>
        <div v-else-if="ticket.status === 1" style="margin-top:16px; text-align:center; color:#999;">
          工单处理中，请耐心等待
        </div>
      </template>
      <el-empty v-else description="工单不存在" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { customerApi } from '../../api/customer'

const route = useRoute()
const router = useRouter()
const ticket = ref(null)
const loading = ref(false)
const closing = ref(false)
const satisfaction = ref(5)

function statusType(s) {
  return s === 0 ? 'info' : s === 1 ? 'primary' : s === 2 ? 'success' : ''
}

async function fetchDetail() {
  loading.value = true
  try {
    ticket.value = await customerApi.getById(route.params.id)
  } catch {
    ticket.value = null
  } finally {
    loading.value = false
  }
}

async function handleClose() {
  closing.value = true
  try {
    await customerApi.close(ticket.value.id, 1, satisfaction.value)
    ElMessage.success('工单已关闭')
    fetchDetail()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    closing.value = false
  }
}

onMounted(fetchDetail)
</script>
