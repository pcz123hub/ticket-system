<template>
  <div>
    <el-page-header @back="$router.push('/admin/tickets')" content="工单详情" style="margin-bottom:16px" />

    <el-row :gutter="16">
      <el-col :span="16">
        <el-card v-loading="loading">
          <template v-if="ticket">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="工单号">{{ ticket.ticketNo }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="['info','primary','success',''][ticket.status]" size="small">{{ ticket.statusDesc }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="优先级">
                <el-tag :type="ticket.priority===0?'danger':ticket.priority===1?'warning':'info'" size="small">{{ ticket.priorityDesc }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="业务类型">{{ ticket.category }}</el-descriptions-item>
              <el-descriptions-item label="标题" :span="2">{{ ticket.title }}</el-descriptions-item>
              <el-descriptions-item label="描述" :span="2">{{ ticket.description || '暂无' }}</el-descriptions-item>
              <el-descriptions-item label="客户ID">{{ ticket.customerId }}</el-descriptions-item>
              <el-descriptions-item label="处理人">{{ ticket.assigneeName || ticket.assigneeId || '未分配' }}</el-descriptions-item>
              <el-descriptions-item label="来源">{{ ticket.source }}</el-descriptions-item>
              <el-descriptions-item label="满意度">{{ ticket.satisfaction || '-' }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ ticket.createdAt }}</el-descriptions-item>
              <el-descriptions-item label="关闭时间">{{ ticket.closedAt || '-' }}</el-descriptions-item>
            </el-descriptions>
          </template>
          <el-empty v-else description="工单不存在" />
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card v-if="assignMode">
          <template #header><span>分配工单</span></template>
          <el-form :model="assignForm">
            <el-form-item label="处理人">
              <el-select v-model="assignForm.agentId" placeholder="自动分配" clearable style="width:100%">
                <el-option v-for="a in agents" :key="a.id" :label="a.name" :value="a.id" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleAssign" :loading="assignLoading" style="width:100%">确认分配</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card v-else style="margin-top:16px">
          <template #header><span>操作</span></template>
          <div style="display:flex; flex-direction:column; gap:10px;">
            <el-button v-if="ticket && ticket.status === 1" type="primary" @click="handleResolve" :loading="resLoading">标记已解决</el-button>
            <el-button v-if="ticket && ticket.status === 1" @click="showTransfer">转交</el-button>
            <el-button v-if="ticket && ticket.status === 1" type="warning" @click="handleClose" :loading="closeLoading">强制关闭</el-button>
            <el-button v-if="ticket && ticket.status === 0 && agents.length > 0" @click="openAssign">分配</el-button>

            <el-dialog v-model="transferVisible" title="转交工单" width="400px">
              <el-form :model="transferForm">
                <el-form-item label="目标处理人">
                  <el-select v-model="transferForm.targetAgentId" placeholder="请选择" style="width:100%">
                    <el-option v-for="a in agents" :key="a.id" :label="a.name" :value="a.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="原因">
                  <el-input v-model="transferForm.reason" type="textarea" :rows="3" />
                </el-form-item>
              </el-form>
              <template #footer>
                <el-button @click="transferVisible = false">取消</el-button>
                <el-button type="primary" @click="handleTransfer" :loading="transferLoading">确认转交</el-button>
              </template>
            </el-dialog>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminApi } from '../../api/admin'

const route = useRoute()
const router = useRouter()
const ticket = ref(null)
const loading = ref(false)
const agents = ref([])
const assignMode = ref(false)
const assignLoading = ref(false)
const resLoading = ref(false)
const closeLoading = ref(false)
const transferLoading = ref(false)
const transferVisible = ref(false)

const assignForm = ref({ ticketId: null, agentId: null })
const transferForm = ref({ ticketId: null, targetAgentId: null, reason: '' })

async function fetchDetail() {
  loading.value = true
  try {
    ticket.value = await adminApi.getTicket(route.params.id)
    assignForm.value.ticketId = ticket.value.id
    transferForm.value.ticketId = ticket.value.id
  } catch { ticket.value = null }
  finally { loading.value = false }
}

async function loadAgents() {
  try { agents.value = await adminApi.listAgents() || [] } catch { agents.value = [] }
}

function openAssign() { assignMode.value = true }

async function handleAssign() {
  assignLoading.value = true
  try {
    await adminApi.assign({ ticketId: ticket.value.id, agentId: assignForm.value.agentId || null })
    ElMessage.success('分配成功')
    assignMode.value = false
    fetchDetail()
  } catch (e) { ElMessage.error(e.message) }
  finally { assignLoading.value = false }
}

function showTransfer() {
  transferForm.value.targetAgentId = null
  transferForm.value.reason = ''
  transferVisible.value = true
}

async function handleTransfer() {
  if (!transferForm.value.targetAgentId) { ElMessage.warning('请选择处理人'); return }
  transferLoading.value = true
  try {
    await adminApi.transfer(transferForm.value, 1)
    ElMessage.success('转交成功')
    transferVisible.value = false
    fetchDetail()
  } catch (e) { ElMessage.error(e.message) }
  finally { transferLoading.value = false }
}

async function handleResolve() {
  resLoading.value = true
  try {
    await adminApi.resolve(ticket.value.id, 1)
    ElMessage.success('已标记解决')
    fetchDetail()
  } catch (e) { ElMessage.error(e.message) }
  finally { resLoading.value = false }
}

async function handleClose() {
  closeLoading.value = true
  try {
    await adminApi.close(ticket.value.id, 1)
    ElMessage.success('工单已关闭')
    fetchDetail()
  } catch (e) { ElMessage.error(e.message) }
  finally { closeLoading.value = false }
}

onMounted(() => { fetchDetail(); loadAgents() })
</script>
