<template>
  <div class="front-layout">
    <div class="front-header">
      <div class="front-header-left">
        <img src="@/assets/imgs/logo.png" alt="">
        <div class="title" @click="router.push('/front/home')">自由职业者外包平台</div>
      </div>
      <div class="front-header-center">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="horizontal"
          class="custom-header-menu"
          @menu-item-click="handleMenuClick"
        >
          <a-menu-item key="/front/home" @click="() => router.push('/front/home')">首页</a-menu-item>
          <a-menu-item v-if="data.user && data.user.role === 'USER'" key="/front/orders" @click="() => router.push('/front/orders')">我的接单</a-menu-item>
          <a-menu-item v-if="data.user && data.user.role === 'USER'" key="/front/submissions" @click="() => router.push('/front/submissions')">我的提交</a-menu-item>
          <a-menu-item v-if="data.user && data.user.role === 'USER'" key="/front/freelancer/profile" @click="() => router.push('/front/freelancer/profile')">个人资料</a-menu-item>
          <a-menu-item v-if="data.user && data.user.role === 'EMPLOY'" key="/front/enterprise/dashboard" @click="() => router.push('/front/enterprise/dashboard')">我的项目</a-menu-item>
          <a-menu-item v-if="data.user && data.user.role === 'EMPLOY'" key="/front/projects/publish" @click="() => router.push('/front/projects/publish')">发布项目</a-menu-item>
          <a-menu-item v-if="data.user && data.user.role === 'EMPLOY'" key="/front/enterprise/profile" @click="() => router.push('/front/enterprise/profile')">企业资料</a-menu-item>
          <a-menu-item v-if="data.user && data.user.id" key="/front/conversation" @click="() => router.push('/front/conversation')">沟通</a-menu-item>
          <a-menu-item v-if="data.user && data.user.id" key="/front/messages" @click="() => router.push('/front/messages')">
            <span style="position: relative; display: inline-block;">
              通知
              <a-badge v-if="unreadCount > 0" :count="unreadCount" :offset="[5, -5]" style="position: absolute; top: 0; right: -10px;" />
            </span>
          </a-menu-item>
        </a-menu>
      </div>
      <div class="front-header-right">
        <div v-if="!data.user.id" class="auth-buttons">
          <a-button ghost @click="router.push('/login')">登录</a-button>
          <a-button type="primary" @click="router.push('/register')">注册</a-button>
        </div>
        <div v-else class="user-dropdown">
          <a-dropdown>
            <div class="user-info">
              <a-avatar :size="36" :src="data.user.avatar" class="user-avatar">
                {{ data.user.name ? data.user.name.charAt(0) : '?' }}
              </a-avatar>
              <span class="user-name">{{ data.user.name }}</span>
              <DownOutlined class="dropdown-icon" />
            </div>
            <template #overlay>
              <a-menu class="user-menu">
                <a-menu-item @click="handleViewBalance">
                  <WalletOutlined />
                  <span>我的余额</span>
                </a-menu-item>
                <a-menu-item @click="handleWithdraw">
                  <DollarOutlined />
                  <span>提现</span>
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item @click="logout" class="logout-item">
                  <LogoutOutlined />
                  <span>退出登录</span>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </div>
    </div>
    <div class="main-body">
      <RouterView @updateUser="updateUser" />
    </div>
    <div class="front-footer">
      <div class="footer-content">
        <div class="footer-links">
          <span class="link-title">友情链接：</span>
          <a href="https://www.baidu.com/" target="_blank">百度一下</a>
          <a href="https://www.google.com" target="_blank">谷歌一下</a>
          <a href="https://www.baidu.com" target="_blank">我的博客</a>
        </div>
        <div class="footer-copyright">
          Copyright ©2024 www.baidu.cn 版权所有   
          <a href="https://beian.miit.gov.cn/" target="_blank">皖ICP备 2023033553号-1</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import router from "@/router/index.js";
import { reactive, ref, watch, onMounted, onUnmounted } from "vue";
import request from "@/utils/request.js";
import { DownOutlined, WalletOutlined, DollarOutlined, LogoutOutlined } from "@ant-design/icons-vue";
import { message } from "ant-design-vue";

const data = reactive({
  user: JSON.parse(localStorage.getItem('xm-user') || '{}')
})

const selectedKeys = ref([router.currentRoute.value.path])
const unreadCount = ref(0)
let unreadCountInterval = null

// 监听路由变化，更新选中菜单
watch(() => router.currentRoute.value.path, (newPath) => {
  selectedKeys.value = [newPath]
}, { immediate: true })

// 加载未读通知数量
const loadUnreadCount = async () => {
  if (!data.user || !data.user.id) {
    unreadCount.value = 0
    return
  }
  try {
    const res = await request.get('/api/notifications/unread/count')
    if (res.code === '200') {
      unreadCount.value = res.data || 0
    }
  } catch (error) {
    console.error('加载未读通知数量失败:', error)
  }
}

const logout = () => {
  localStorage.removeItem('xm-user')
  router.push('/login')
}

const updateUser = () => {
  data.user = JSON.parse(localStorage.getItem('xm-user') || '{}')
}

const handleMenuClick = (e) => {
  console.log('菜单点击事件:', e)
  // Ant Design Vue 4.x 的菜单点击事件参数格式
  const key = e?.key || e?.item?.key || e
  console.log('菜单key:', key, '当前路由:', router.currentRoute.value.path)
  if (key && key !== router.currentRoute.value.path) {
    router.push(key).then(() => {
      console.log('路由跳转成功:', key)
    }).catch(err => {
      console.error('路由跳转失败:', err, key)
    })
  }
}

const handleViewBalance = () => {
  router.push('/front/balance')
}

const handleWithdraw = () => {
  router.push('/front/withdraw')
}

// 组件挂载时加载未读通知数量，并设置定时刷新
onMounted(() => {
  loadUnreadCount()
  // 每10秒刷新一次未读通知数量，实现近实时更新红点
  unreadCountInterval = setInterval(() => {
    loadUnreadCount()
  }, 10000)
})

// 组件卸载时清除定时器和全局函数
onUnmounted(() => {
  if (unreadCountInterval) {
    clearInterval(unreadCountInterval)
  }
  if (window.refreshNotificationBadge) {
    delete window.refreshNotificationBadge
  }
})

// 监听路由变化，刷新未读数量（页面切换时立即刷新）
watch(() => router.currentRoute.value.path, (newPath) => {
  loadUnreadCount()
})

// 暴露loadUnreadCount函数到window对象，供其他组件调用
// 这样当发送通知后，可以立即刷新红点
window.refreshNotificationBadge = loadUnreadCount
</script>

<style scoped>
@import "@/assets/css/front.css";

.front-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-secondary);
}

/* 认证按钮区域 */
.auth-buttons {
  display: flex;
  gap: 12px;
  align-items: center;
}

.auth-buttons .ant-btn {
  height: 36px;
  padding: 0 20px;
  border-radius: 6px;
  font-weight: 500;
  font-size: 14px;
}

.auth-buttons .ant-btn-default {
  background: transparent !important;
  border-color: rgba(255, 255, 255, 0.5) !important;
  color: #ffffff !important;
}

.auth-buttons .ant-btn-default:hover {
  background: rgba(255, 255, 255, 0.1) !important;
  border-color: #ffffff !important;
}

/* 用户下拉菜单 */
.user-dropdown {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  transition: background-color 0.2s ease;
}

.user-info:hover {
  background: rgba(255, 255, 255, 0.1);
}

.user-avatar {
  border: 2px solid var(--primary-color);
  box-shadow: 0 2px 8px rgba(0, 166, 167, 0.3);
}

.user-name {
  margin-left: 10px;
  color: #ffffff;
  font-weight: 500;
  font-size: 14px;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-icon {
  margin-left: 6px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
}

/* 用户菜单样式 */
.user-menu {
  min-width: 160px;
  padding: 8px !important;
  border-radius: 10px !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15) !important;
}

.user-menu .ant-menu-item {
  display: flex !important;
  align-items: center !important;
  gap: 10px !important;
  padding: 10px 16px !important;
  border-radius: 6px !important;
  margin: 2px 0 !important;
  font-size: 14px !important;
  color: var(--text-primary) !important;
}

.user-menu .ant-menu-item:hover {
  background: var(--primary-light) !important;
  color: var(--primary-color) !important;
}

.user-menu .logout-item:hover {
  background: #fff2f0 !important;
  color: var(--error-color) !important;
}

/* 页脚样式 */
.front-footer {
  background: #ffffff;
  border-top: 1px solid var(--border-light);
  margin-top: auto;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
  text-align: center;
}

.footer-links {
  line-height: 32px;
  margin-bottom: 12px;
}

.footer-links .link-title {
  color: var(--text-secondary);
  margin-right: 8px;
}

.footer-links a {
  color: var(--text-tertiary);
  margin: 0 16px;
  font-size: 13px;
  transition: color 0.2s ease;
}

.footer-links a:hover {
  color: var(--primary-color);
}

.footer-copyright {
  color: var(--text-tertiary);
  font-size: 13px;
  line-height: 24px;
}

.footer-copyright a {
  color: var(--text-tertiary);
  margin-left: 16px;
}

.footer-copyright a:hover {
  color: var(--primary-color);
}
</style>

<style>
/* 全局样式，确保菜单项清晰可见 */
.custom-header-menu .ant-menu-item {
  color: rgba(255, 255, 255, 0.85) !important;
  font-weight: 500 !important;
  font-size: 15px !important;
}

.custom-header-menu .ant-menu-item a {
  color: rgba(255, 255, 255, 0.85) !important;
}

.custom-header-menu .ant-menu-item:hover {
  color: #ffffff !important;
  background-color: rgba(0, 166, 167, 0.25) !important;
}

.custom-header-menu .ant-menu-item:hover a {
  color: #ffffff !important;
}

.custom-header-menu .ant-menu-item-selected {
  color: #ffffff !important;
  background-color: var(--primary-color) !important;
  font-weight: 600 !important;
}

.custom-header-menu .ant-menu-item-selected a {
  color: #ffffff !important;
}

.custom-header-menu .ant-menu-item-selected::after {
  border-bottom: none !important;
}
</style>
