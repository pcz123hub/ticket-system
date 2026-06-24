<template>
  <div class="page-container">
    <div class="flex-between mb-16">
      <h2>我的工单</h2>
      <el-button type="primary" @click="$router.push('/customer/tickets/create')">
        <el-icon><Plus /></el-icon> 提交工单
      </el-button>
    </div>

    <el-card>
      <el-form :inline="true" size="small" class="mb-16">
        <el-form-item label="客户ID">
          <el-input v-model="searchForm.customerId" placeholder="请输入客户ID" style="width:160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px">
            <el-option label="待分配" :value="0" />
            <el-option label="处理中" :value="1" />
            <el-option label="已解决" :value="2" />
            <el-option label="已关闭" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="searchForm.keyword" placeholder="标题/工单号" style="width:200px" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" empty-text="暂无工单" stripe style="width:100%">
        <el-table-column label="工单号" prop="ticketNo" width="160" />
        <el-table-column label="标题" prop="title" min-width="200" show-overflow-tooltip />
        <el-table-column label="优先级" width="80">
          <template #default="{ row }">
            <el-tag :type="priorityType(row.priority)" size="small">{{ row.priorityDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="业务类型" prop="category" width="100" />
        <el-table-column label="来源" prop="source" width="90" />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ row.createdAt }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="$router.push(`/customer/tickets/${row.id}`)">详情</el-button>
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
import { ref, onMounted } from 'vue'
import { customerApi } from '../../api/customer'

const tableData = ref([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const searchForm = ref({ customerId: '', status: '', keyword: '' })

function priorityType(p) {
  return p === 0 ? 'danger' : p === 1 ? 'warning' : 'info'
}
function statusType(s) {
  return s === 0 ? 'info' : s === 1 ? 'primary' : s === 2 ? 'success' : ''
}

async function fetchData() {
  if (!searchForm.value.customerId) return
  loading.value = true
  try {
    const params = { ...searchForm.value, pageNum: pageNum.value, pageSize: pageSize.value }
    const res = await customerApi.search(params)
    tableData.value = res.list || []
    total.value = res.total || 0
  } catch (e) {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() { pageNum.value = 1; fetchData() }

onMounted(() => {
  searchForm.value.customerId = '1'
  fetchData()
})
</script>
