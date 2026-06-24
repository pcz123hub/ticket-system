<template>
  <div>
    <el-card>
      <el-form :inline="true" size="small" class="mb-16">
        <el-form-item label="关键字">
          <el-input v-model="search.keyword" placeholder="标题/工单号" clearable style="width:180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="search.status" placeholder="全部" clearable style="width:110px">
            <el-option label="待分配" :value="0" />
            <el-option label="处理中" :value="1" />
            <el-option label="已解决" :value="2" />
            <el-option label="已关闭" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="search.priority" placeholder="全部" clearable style="width:110px">
            <el-option label="紧急" :value="0" />
            <el-option label="高" :value="1" />
            <el-option label="普通" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务类型">
          <el-select v-model="search.category" placeholder="全部" clearable style="width:110px">
            <el-option label="咨询" value="咨询" />
            <el-option label="投诉" value="投诉" />
            <el-option label="售后" value="售后" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe style="width:100%" empty-text="暂无工单">
        <el-table-column label="工单号" prop="ticketNo" width="155" />
        <el-table-column label="标题" prop="title" min-width="180" show-overflow-tooltip />
        <el-table-column label="客户" prop="customerId" width="70" align="center" />
        <el-table-column label="处理人" width="80" align="center">
          <template #default="{ row }">{{ row.assigneeName || row.assigneeId || '-' }}</template>
        </el-table-column>
        <el-table-column label="优先级" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.priority === 0 ? 'danger' : row.priority === 1 ? 'warning' : 'info'" size="small">
              {{ row.priorityDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="['info','primary','success',''][row.status]" size="small">{{ row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="业务类型" prop="category" width="80" />
        <el-table-column label="创建时间" width="165">
          <template #default="{ row }">{{ row.createdAt }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="$router.push(`/admin/tickets/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-if="total > 0" background layout="prev, pager, next"
        :total="total" :page-size="pageSize" v-model:current-page="pageNum"
        @current-change="fetchData" style="margin-top:16px; justify-content:center;" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminApi } from '../../api/admin'

const tableData = ref([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const search = reactive({ keyword: '', status: '', priority: '', category: '' })

async function fetchData() {
  loading.value = true
  try {
    const res = await adminApi.listTickets({ ...search, pageNum: pageNum.value, pageSize: pageSize.value })
    tableData.value = res.list || []
    total.value = res.total || 0
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() { pageNum.value = 1; fetchData() }
function resetSearch() {
  Object.assign(search, { keyword: '', status: '', priority: '', category: '' })
  handleSearch()
}

onMounted(fetchData)
</script>
