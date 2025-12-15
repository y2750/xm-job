<template>
  <div class="register-page">
    <div class="register-left">
      <div class="brand-content">
        <div class="brand-logo">
          <img src="@/assets/imgs/logo.png" alt="Logo">
        </div>
        <h1 class="brand-title">自由职业者外包平台</h1>
        <p class="brand-slogan">加入我们，开启你的事业新篇章</p>
        <div class="brand-stats">
          <div class="stat-item">
            <div class="stat-number">10000+</div>
            <div class="stat-label">注册用户</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">5000+</div>
            <div class="stat-label">优质项目</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">98%</div>
            <div class="stat-label">好评率</div>
          </div>
        </div>
      </div>
    </div>
    <div class="register-right">
      <div class="register-box">
        <div class="register-header">
          <h2>创建账号</h2>
          <p>填写信息，即刻开始</p>
        </div>
        <a-form :model="form" :rules="rules" ref="formRef" layout="vertical" class="register-form">
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
              placeholder="请输入密码（至少6位）" 
              size="large"
              class="custom-input"
            >
              <template #prefix>
                <LockOutlined class="input-icon" />
              </template>
            </a-input-password>
          </a-form-item>
          <a-form-item name="confirmPassword" label="确认密码">
            <a-input-password 
              v-model:value="form.confirmPassword" 
              placeholder="请再次输入密码" 
              size="large"
              class="custom-input"
            >
              <template #prefix>
                <LockOutlined class="input-icon" />
              </template>
            </a-input-password>
          </a-form-item>
          <a-form-item name="name" label="名称">
            <a-input 
              v-model:value="form.name" 
              placeholder="请输入您的名称" 
              size="large"
              class="custom-input"
            >
              <template #prefix>
                <IdcardOutlined class="input-icon" />
              </template>
            </a-input>
          </a-form-item>
          <a-form-item name="userType" label="用户类型">
            <div class="user-type-selector">
              <div 
                class="type-option" 
                :class="{ active: form.userType === 'freelancer' }"
                @click="form.userType = 'freelancer'"
              >
                <div class="type-icon">
                  <UserOutlined />
                </div>
                <div class="type-info">
                  <h4>自由职业者</h4>
                  <p>寻找项目机会</p>
                </div>
                <div class="type-check" v-if="form.userType === 'freelancer'">
                  <CheckOutlined />
                </div>
              </div>
              <div 
                class="type-option" 
                :class="{ active: form.userType === 'enterprise' }"
                @click="form.userType = 'enterprise'"
              >
                <div class="type-icon enterprise">
                  <TeamOutlined />
                </div>
                <div class="type-info">
                  <h4>企业</h4>
                  <p>发布项目招聘</p>
                </div>
                <div class="type-check" v-if="form.userType === 'enterprise'">
                  <CheckOutlined />
                </div>
              </div>
            </div>
          </a-form-item>
          <a-form-item style="margin-top: 8px;">
            <a-button 
              type="primary" 
              size="large" 
              block 
              @click="handleRegister" 
              :loading="loading"
              class="register-btn"
            >
              立即注册
            </a-button>
          </a-form-item>
          <div class="register-footer">
            <span>已有账号？</span>
            <a href="/login" class="login-link">立即登录</a>
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
  IdcardOutlined, 
  TeamOutlined,
  CheckOutlined 
} from "@ant-design/icons-vue";
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
.register-page {
  min-height: 100vh;
  display: flex;
  background: #f5f7fa;
}

/* 左侧品牌区域 */
.register-left {
  flex: 1;
  background: linear-gradient(135deg, #00a6a7 0%, #008f90 50%, #007a7b 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
  position: relative;
  overflow: hidden;
}

.register-left::before {
  content: '';
  position: absolute;
  top: -30%;
  right: -30%;
  width: 60%;
  height: 60%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.15) 0%, transparent 70%);
  border-radius: 50%;
}

.register-left::after {
  content: '';
  position: absolute;
  bottom: -20%;
  left: -20%;
  width: 50%;
  height: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 70%);
  border-radius: 50%;
}

.brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 400px;
}

.brand-logo {
  margin-bottom: 24px;
}

.brand-logo img {
  width: 80px;
  height: 80px;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
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
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 48px;
}

.brand-stats {
  display: flex;
  justify-content: center;
  gap: 40px;
}

.stat-item {
  text-align: center;
}

.stat-number {
  font-size: 36px;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

/* 右侧注册区域 */
.register-right {
  width: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: #ffffff;
  overflow-y: auto;
}

.register-box {
  width: 100%;
  max-width: 400px;
}

.register-header {
  text-align: center;
  margin-bottom: 32px;
}

.register-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: #14171f;
  margin: 0 0 8px 0;
}

.register-header p {
  font-size: 14px;
  color: #9499a0;
  margin: 0;
}

/* 表单样式 */
.register-form :deep(.ant-form-item-label > label) {
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

/* 用户类型选择器 */
.user-type-selector {
  display: flex;
  gap: 16px;
}

.type-option {
  flex: 1;
  display: flex;
  align-items: center;
  padding: 16px;
  border: 2px solid #e8e9eb;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
}

.type-option:hover {
  border-color: #00a6a7;
  background: #f7fafa;
}

.type-option.active {
  border-color: #00a6a7;
  background: #e6f7f7;
}

.type-icon {
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

.type-icon.enterprise {
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
}

.type-info {
  margin-left: 12px;
  flex: 1;
}

.type-info h4 {
  font-size: 14px;
  font-weight: 600;
  color: #14171f;
  margin: 0 0 2px 0;
}

.type-info p {
  font-size: 12px;
  color: #9499a0;
  margin: 0;
}

.type-check {
  width: 24px;
  height: 24px;
  background: #00a6a7;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 12px;
}

.register-btn {
  height: 48px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #00a6a7 0%, #00c4c4 100%) !important;
  border: none !important;
  box-shadow: 0 4px 12px rgba(0, 166, 167, 0.3);
  transition: all 0.3s ease;
}

.register-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 166, 167, 0.4);
}

.register-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: #9499a0;
}

.login-link {
  color: #00a6a7;
  font-weight: 500;
  margin-left: 8px;
  transition: color 0.2s ease;
}

.login-link:hover {
  color: #008f90;
}

/* 响应式设计 */
@media (max-width: 992px) {
  .register-left {
    display: none;
  }
  
  .register-right {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .register-right {
    padding: 24px;
  }
  
  .register-header h2 {
    font-size: 24px;
  }
  
  .user-type-selector {
    flex-direction: column;
  }
}
</style>
