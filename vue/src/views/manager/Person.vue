<template>
  <a-card>
    <template #title>
      <h3>个人资料</h3>
    </template>
    
    <a-form :model="form" :rules="rules" ref="formRef" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="账号" name="username">
        <a-input v-model:value="form.username" disabled />
      </a-form-item>
      <a-form-item label="姓名" name="name">
        <a-input v-model:value="form.name" placeholder="请输入姓名" />
      </a-form-item>
      <a-form-item label="电话" name="phone">
        <a-input v-model:value="form.phone" placeholder="请输入电话" />
      </a-form-item>
      <a-form-item label="邮箱" name="email">
        <a-input v-model:value="form.email" placeholder="请输入邮箱" />
      </a-form-item>
      <a-form-item label="头像" name="avatar">
        <a-input v-model:value="form.avatar" placeholder="请输入头像URL" />
      </a-form-item>
      <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
        <a-button type="primary" @click="handleSubmit" :loading="submitting">保存</a-button>
        <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
      </a-form-item>
    </a-form>
  </a-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import request from '@/utils/request'

const formRef = ref()
const submitting = ref(false)

const form = reactive({
  id: null,
  username: '',
  name: '',
  phone: '',
  email: '',
  avatar: ''
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

const loadUser = () => {
  const user = JSON.parse(localStorage.getItem('xm-user') || '{}')
  if (user.id) {
    Object.assign(form, {
      id: user.id,
      username: user.username || '',
      name: user.name || '',
      phone: user.phone || '',
      email: user.email || '',
      avatar: user.avatar || ''
    })
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true
    const res = await request.put('/admin/update', form)
    if (res.code === '200') {
      message.success('保存成功')
      // 更新本地存储的用户信息
      const user = JSON.parse(localStorage.getItem('xm-user') || '{}')
      Object.assign(user, form)
      localStorage.setItem('xm-user', JSON.stringify(user))
    } else {
      message.error(res.msg || '保存失败')
    }
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    submitting.value = false
  }
}

const handleReset = () => {
  loadUser()
  formRef.value?.resetFields()
}

onMounted(() => {
  loadUser()
})
</script>
