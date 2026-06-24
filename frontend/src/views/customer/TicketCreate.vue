<template>
  <div class="page-container">
    <el-page-header @back="$router.push('/customer/tickets')" content="提交工单" style="margin-bottom:20px" />

    <el-card style="max-width:700px; margin:0 auto;">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" label-position="right">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请简要描述问题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="5" placeholder="请详细描述您的问题" />
        </el-form-item>
        <el-form-item label="业务类型" prop="category">
          <el-select v-model="form.category" placeholder="请选择" style="width:100%">
            <el-option label="咨询" value="咨询" />
            <el-option label="投诉" value="投诉" />
            <el-option label="售后" value="售后" />
            <el-option label="建议" value="建议" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-radio-group v-model="form.priority">
            <el-radio-button :value="2">普通</el-radio-button>
            <el-radio-button :value="1">高</el-radio-button>
            <el-radio-button :value="0">紧急</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting" style="width:100%">
            {{ submitting ? '提交中...' : '提交工单' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { customerApi } from '../../api/customer'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  title: '',
  description: '',
  category: '',
  priority: 2,
  customerId: 1,
  source: '门户'
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }]
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => {})
  if (!valid) return
  submitting.value = true
  try {
    const key = 'tk_' + Date.now()
    await customerApi.create(form, key)
    ElMessage.success('工单提交成功！')
    router.push('/customer/tickets')
  } catch (e) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>
