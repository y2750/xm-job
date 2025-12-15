<template>
  <div class="login-page">
    <div class="login-left">
      <div class="brand-content">
        <div class="brand-logo">
          <img src="@/assets/imgs/logo.png" alt="Logo">
        </div>
        <h1 class="brand-title">自由职业者外包平台</h1>
        <p class="brand-slogan">连接企业与人才，开启高效协作</p>
        <div class="brand-features">
          <div class="feature-item">
            <div class="feature-icon">
              <CheckCircleOutlined />
            </div>
            <div class="feature-text">
              <h4>海量项目</h4>
              <p>丰富的项目资源，满足各类技能需求</p>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <SafetyOutlined />
            </div>
            <div class="feature-text">
              <h4>安全保障</h4>
              <p>资金托管，权益有保障</p>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <ThunderboltOutlined />
            </div>
            <div class="feature-text">
              <h4>高效沟通</h4>
              <p>在线即时沟通，项目推进更顺畅</p>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="login-right">
      <div class="login-box">
        <div class="login-header">
          <h2>欢迎登录</h2>
          <p>登录后开启你的职业之旅</p>
        </div>
        <a-form :model="form" :rules="rules" ref="formRef" layout="vertical" class="login-form">
          <a-form-item name="username" label="账号">
            <a-input 
              v-model:value="form.username" 
              placeholder="请输入账号" 
              size="large"
              class="custom-input"
            >
              <template #prefix>
                <UserOutlined class="input-icon" />
              </template>
            </a-input>
          </a-form-item>
          <a-form-item name="password" label="密码">
            <a-input-password 
              v-model:value="form.password" 
              placeholder="请输入密码" 
              size="large"
              class="custom-input"
            >
              <template #prefix>
                <LockOutlined class="input-icon" />
              </template>
            </a-input-password>
          </a-form-item>
          <a-form-item name="role" label="角色">
            <a-select 
              v-model:value="form.role" 
              placeholder="请选择角色" 
              size="large"
              class="custom-select"
            >
              <a-select-option value="ADMIN">管理员</a-select-option>
              <a-select-option value="EMPLOY">企业</a-select-option>
              <a-select-option value="USER">自由职业者</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item style="margin-top: 8px;">
            <a-button 
              type="primary" 
              size="large" 
              block 
              @click="handleLogin" 
              :loading="loading"
              class="login-btn"
            >
              登 录
            </a-button>
          </a-form-item>
          <div class="login-footer">
            <span>还没有账号？</span>
            <a href="/register" class="register-link">立即注册</a>
          </div>
        </a-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { 
  UserOutlined, 
  LockOutlined, 
  CheckCircleOutlined, 
  SafetyOutlined, 
  ThunderboltOutlined 
} from "@ant-design/icons-vue";
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
.login-page {
  min-height: 100vh;
  display: flex;
  background: #f5f7fa;
}

/* 左侧品牌区域 */
.login-left {
  flex: 1;
  background: linear-gradient(135deg, #1e2838 0%, #2a3f5f 50%, #1e2838 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
  position: relative;
  overflow: hidden;
}

.login-left::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(0, 166, 167, 0.1) 0%, transparent 50%);
  animation: pulse 15s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.1); opacity: 0.8; }
}

.brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 420px;
}

.brand-logo {
  margin-bottom: 24px;
}

.brand-logo img {
  width: 80px;
  height: 80px;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 166, 167, 0.3);
}

.brand-title {
  font-size: 32px;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 12px;
  letter-spacing: 2px;
}

.brand-slogan {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 48px;
}

.brand-features {
  text-align: left;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 28px;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  transition: all 0.3s ease;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.08);
  transform: translateX(8px);
}

.feature-icon {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #00a6a7 0%, #00c4c4 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #ffffff;
  flex-shrink: 0;
}

.feature-text {
  margin-left: 16px;
}

.feature-text h4 {
  font-size: 15px;
  font-weight: 600;
  color: #ffffff;
  margin: 0 0 4px 0;
}

.feature-text p {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
}

/* 右侧登录区域 */
.login-right {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: #ffffff;
}

.login-box {
  width: 100%;
  max-width: 360px;
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.login-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: #14171f;
  margin: 0 0 8px 0;
}

.login-header p {
  font-size: 14px;
  color: #9499a0;
  margin: 0;
}

/* 表单样式 */
.login-form :deep(.ant-form-item-label > label) {
  font-weight: 500;
  color: #14171f;
}

.custom-input {
  border-radius: 8px;
  border-color: #e8e9eb;
  transition: all 0.2s ease;
}

.custom-input:hover {
  border-color: #00a6a7;
}

.custom-input:focus,
.custom-input.ant-input-affix-wrapper-focused {
  border-color: #00a6a7;
  box-shadow: 0 0 0 2px rgba(0, 166, 167, 0.1);
}

.input-icon {
  color: #9499a0;
}

.custom-select :deep(.ant-select-selector) {
  border-radius: 8px !important;
  border-color: #e8e9eb !important;
}

.custom-select:hover :deep(.ant-select-selector) {
  border-color: #00a6a7 !important;
}

.custom-select.ant-select-focused :deep(.ant-select-selector) {
  border-color: #00a6a7 !important;
  box-shadow: 0 0 0 2px rgba(0, 166, 167, 0.1) !important;
}

.login-btn {
  height: 48px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #00a6a7 0%, #00c4c4 100%) !important;
  border: none !important;
  box-shadow: 0 4px 12px rgba(0, 166, 167, 0.3);
  transition: all 0.3s ease;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 166, 167, 0.4);
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: #9499a0;
}

.register-link {
  color: #00a6a7;
  font-weight: 500;
  margin-left: 8px;
  transition: color 0.2s ease;
}

.register-link:hover {
  color: #008f90;
}

/* 响应式设计 */
@media (max-width: 992px) {
  .login-left {
    display: none;
  }
  
  .login-right {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .login-right {
    padding: 24px;
  }
  
  .login-header h2 {
    font-size: 24px;
  }
}
</style>
