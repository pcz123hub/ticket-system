<template>
  <div>
    <h3 style="margin-bottom:16px;">客服管理</h3>
    <el-card v-loading="loading">
      <el-table :data="agents" stripe empty-text="暂无客服" style="width:100%">
        <el-table-column label="ID" prop="id" width="80" />
        <el-table-column label="姓名" prop="name" width="150" />
        <el-table-column label="技能标签" prop="skills" min-width="200" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最大负载" prop="maxLoad" width="100" align="center" />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ row.createdAt }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '../../api/admin'

const agents = ref([])
const loading = ref(false)

async function fetchAgents() {
  loading.value = true
  try { agents.value = await adminApi.listAgents() || [] }
  catch { agents.value = [] }
  finally { loading.value = false }
}

onMounted(fetchAgents)
</script>
