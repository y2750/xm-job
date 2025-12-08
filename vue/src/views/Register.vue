<template>
  <div class="login-container">
    <div class="login-box">
      <div style="font-weight: bold; font-size: 24px; text-align: center; margin-bottom: 30px; color: #1450aa">欢迎注册</div>
      <a-form :model="form" :rules="rules" ref="formRef" layout="vertical">
        <a-form-item name="username" label="账号">
          <a-input v-model:value="form.username" placeholder="请输入账号" size="large">
            <template #prefix>
              <UserOutlined />
            </template>
          </a-input>
        </a-form-item>
        <a-form-item name="password" label="密码">
          <a-input-password v-model:value="form.password" placeholder="请输入密码" size="large">
            <template #prefix>
              <LockOutlined />
            </template>
          </a-input-password>
        </a-form-item>
        <a-form-item name="confirmPassword" label="确认密码">
          <a-input-password v-model:value="form.confirmPassword" placeholder="请确认密码" size="large">
            <template #prefix>
              <LockOutlined />
            </template>
          </a-input-password>
        </a-form-item>
        <a-form-item name="name" label="名称">
          <a-input v-model:value="form.name" placeholder="请输入名称" size="large">
            <template #prefix>
              <UserOutlined />
            </template>
          </a-input>
        </a-form-item>
        <a-form-item name="userType" label="用户类型">
          <a-radio-group v-model:value="form.userType" size="large">
            <a-radio value="freelancer">自由职业者</a-radio>
            <a-radio value="enterprise">企业</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" size="large" block @click="handleRegister" :loading="loading">注 册</a-button>
        </a-form-item>
        <div style="text-align: right">
          已有账号？请 <a href="/login">登录</a>
        </div>
      </a-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { UserOutlined, LockOutlined } from "@ant-design/icons-vue";
import request from "@/utils/request.js";
import { message } from "ant-design-vue";
import router from "@/router/index.js";

const formRef = ref();
const loading = ref(false);

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  name: '',
  userType: 'freelancer'
});

const validateConfirmPassword = async (_rule, value) => {
  if (!value) {
    return Promise.reject('请确认密码');
  }
  if (value !== form.password) {
    return Promise.reject('确认密码与原密码不一致');
  }
  return Promise.resolve();
};

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入名称', trigger: 'blur' }
  ],
  userType: [
    { required: true, message: '请选择用户类型', trigger: 'change' }
  ]
};

const handleRegister = async () => {
  try {
    await formRef.value.validate();
    loading.value = true;
    
    const registerUrl = form.userType === 'freelancer' 
      ? '/api/auth/register/freelancer' 
      : '/api/auth/register/enterprise';
    
    const res = await request.post(registerUrl, {
      username: form.username,
      password: form.password,
      name: form.name
    });
    
    if (res.code === '200') {
      message.success('注册成功');
      router.push('/login');
    } else {
      message.error(res.msg);
    }
  } catch (error) {
    console.error('注册失败', error);
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-container {
  height: 100vh;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(to top, #00467f, #a5cc82);
}
.login-box {
  width: 400px;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.1);
  background-color: rgba(255, 255, 255, 0.95);
}
</style>
