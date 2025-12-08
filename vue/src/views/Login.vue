<template>
  <div class="login-container">
    <div class="login-box">
      <div style="font-weight: bold; font-size: 24px; text-align: center; margin-bottom: 30px; color: #1450aa">欢迎登录自由职业者外包平台</div>
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
        <a-form-item name="role" label="角色">
          <a-select v-model:value="form.role" placeholder="请选择角色" size="large">
            <a-select-option value="ADMIN">管理员</a-select-option>
            <a-select-option value="EMPLOY">企业</a-select-option>
            <a-select-option value="USER">自由职业者</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" size="large" block @click="handleLogin" :loading="loading">登 录</a-button>
        </a-form-item>
        <div style="text-align: right">
          还没有账号？请 <a href="/register">注册</a>
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
  role: 'USER'
});

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
};

const handleLogin = async () => {
  try {
    await formRef.value.validate();
    loading.value = true;
    
    const res = await request.post('/api/auth/login', form);
    if (res.code === '200') {
      message.success('登录成功');
      // 存储用户信息到浏览器的缓存
      localStorage.setItem('xm-user', JSON.stringify(res.data));
      
      // 确定目标路径
      let targetPath = '/front/home';
      if ('USER' === res.data.role) {
        targetPath = '/front/home';
      } else if ('ADMIN' === res.data.role) {
        targetPath = '/manager/home';
      } else if ('EMPLOY' === res.data.role) {
        // 企业用户跳转到企业工作台
        targetPath = '/front/enterprise/dashboard';
      }
      
      // 使用 window.location.href 强制刷新页面并跳转
      // 这样可以确保页面完全刷新，避免路由缓存问题
      window.location.href = targetPath;
    } else {
      message.error(res.msg);
    }
  } catch (error) {
    console.error('登录失败', error);
    message.error('登录失败，请重试');
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
  background-image: url("@/assets/imgs/bg.jpg");
  background-size: cover;
}
.login-box {
  width: 400px;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.1);
  background-color: rgba(255, 255, 255, 0.95);
}
</style>
