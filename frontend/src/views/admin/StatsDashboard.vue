<template>
  <div>
    <h3 style="margin-bottom:16px;">统计看板</h3>
    <el-row :gutter="16" v-loading="loading">
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="text-align:center; padding:10px 0;">
            <div style="font-size:28px; font-weight:700; color:#409eff;">{{ getVal('avg_process_minutes') }}</div>
            <div style="color:#999; font-size:13px; margin-top:8px;">平均处理时长(分钟)</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="text-align:center; padding:10px 0;">
            <div style="font-size:28px; font-weight:700; color:#67c23a;">{{ getVal('status_0') || 0 }}</div>
            <div style="color:#999; font-size:13px; margin-top:8px;">待分配</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="text-align:center; padding:10px 0;">
            <div style="font-size:28px; font-weight:700; color:#e6a23c;">{{ getVal('status_1') || 0 }}</div>
            <div style="color:#999; font-size:13px; margin-top:8px;">处理中</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="text-align:center; padding:10px 0;">
            <div style="font-size:28px; font-weight:700; color:#909399;">{{ getVal('status_3') || 0 }}</div>
            <div style="color:#999; font-size:13px; margin-top:8px;">已关闭</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:16px;" v-loading="loading">
      <template #header><span>客服负载</span></template>
      <el-table :data="agentLoads" stripe empty-text="暂无数据">
        <el-table-column label="客服ID" prop="id" width="100" />
        <el-table-column label="在处理数">
          <template #default="{ row }">{{ getVal('agent_' + row.id) || 0 }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-empty v-if="!loading && Object.keys(dashboard).length === 0" description="暂无统计数据，系统将在5分钟内自动生成" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '../../api/admin'

const dashboard = ref({})
const loading = ref(false)
const agentLoads = ref([])

function getVal(key) { return dashboard.value[key] }

async function fetchStats() {
  loading.value = true
  try {
    dashboard.value = await adminApi.getDashboard()
  } catch { dashboard.value = {} }
  finally { loading.value = false }
}

onMounted(fetchStats)
</script>
