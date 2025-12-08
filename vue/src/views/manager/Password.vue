<template>
  <a-card>
    <template #title>
      <h3>修改密码</h3>
    </template>
    
    <a-form :model="form" :rules="rules" ref="formRef" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="原密码" name="password">
        <a-input-password v-model:value="form.password" placeholder="请输入原密码" />
      </a-form-item>
      <a-form-item label="新密码" name="newPassword">
        <a-input-password v-model:value="form.newPassword" placeholder="请输入新密码" />
      </a-form-item>
      <a-form-item label="确认密码" name="confirmPassword">
        <a-input-password v-model:value="form.confirmPassword" placeholder="请再次输入新密码" />
      </a-form-item>
      <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
        <a-button type="primary" @click="handleSubmit" :loading="submitting">修改密码</a-button>
        <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
      </a-form-item>
    </a-form>
  </a-card>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import request from '@/utils/request'
import { useRouter } from 'vue-router'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)

const form = reactive({
  username: '',
  password: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value) => {
  if (!value) {
    return Promise.reject('请再次输入新密码')
  }
  if (value !== form.newPassword) {
    return Promise.reject('两次输入的密码不一致')
  }
  return Promise.resolve()
}

const rules = {
  password: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const loadUser = () => {
  const user = JSON.parse(localStorage.getItem('xm-user') || '{}')
  if (user.username) {
    form.username = user.username
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true
    const res = await request.put('/updatePassword', {
      username: form.username,
      password: form.password,
      newPassword: form.newPassword,
      role: 'ADMIN'
    })
    if (res.code === '200') {
      message.success('密码修改成功，请重新登录')
      setTimeout(() => {
        localStorage.removeItem('xm-user')
        router.push('/login')
      }, 1500)
    } else {
      message.error(res.msg || '密码修改失败')
    }
  } catch (error) {
    console.error('修改密码失败:', error)
  } finally {
    submitting.value = false
  }
}

const handleReset = () => {
  formRef.value?.resetFields()
  loadUser()
}

loadUser()
</script>
