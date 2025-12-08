<template>
  <div>
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
          <a-menu-item v-if="data.user && data.user.id" key="/front/messages" @click="() => router.push('/front/messages')">
            <span style="position: relative; display: inline-block;">
              通知
              <a-badge v-if="unreadCount > 0" :count="unreadCount" :offset="[5, -5]" style="position: absolute; top: 0; right: -10px;" />
            </span>
          </a-menu-item>
        </a-menu>
      </div>
      <div class="front-header-right">
        <div v-if="!data.user.id">
          <a-button @click="router.push('/login')">登录</a-button>
          <a-button type="primary" @click="router.push('/register')" style="margin-left: 8px">注册</a-button>
        </div>
        <div v-else>
          <a-dropdown>
            <div style="display: flex; align-items: center; cursor: pointer; height: 50px">
              <img style="width: 35px; height: 35px; border-radius: 50%; border: 2px solid #34daa9" :src="data.user.avatar" alt="">
              <span style="margin-left: 5px; color: white">{{ data.user.name }}</span>
              <DownOutlined style="margin-left: 5px" />
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="handleViewBalance">我的余额</a-menu-item>
                <a-menu-item @click="handleWithdraw">提现</a-menu-item>
                <a-menu-divider />
                <a-menu-item @click="logout">退出登录</a-menu-item>
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
      <div style="width: 70%; margin: 0 auto; line-height: 50px; height: 50px; font-size: 14px; color: #93999F; text-align: center">
        <span style="color: #666666">友情链接：</span>
        <span style="margin-left: 10px"><a style="margin-left: 10px; color: #93999F" href="https://www.baidu.com/" target="_blank">百度一下</a></span>
        <span style="margin-left: 10px"><a style="margin-left: 10px; color: #93999F" href="https://www.google.com" target="_blank">谷歌一下</a></span>
        <span style="margin-left: 10px"><a style="margin-left: 10px; color: #93999F" href="https://www.baidu.com" target="_blank">我的博客</a></span>
      </div>
      <div style="text-align: center; line-height: 30px; font-size: 13px; margin-bottom: 10px; color: #93999F">
        Copyright ©2024 www.baidu.cn 版权所有   <a style="margin-left: 10px; color: #93999F" href="https://beian.miit.gov.cn/" target="_blank">皖ICP备 2023033553号-1</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import router from "@/router/index.js";
import { reactive, ref, watch, onMounted, onUnmounted } from "vue";
import request from "@/utils/request.js";
import { DownOutlined } from "@ant-design/icons-vue";
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
    const res = await request.get('/api/messages/unread/count')
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
  // 每30秒刷新一次未读通知数量
  unreadCountInterval = setInterval(() => {
    loadUnreadCount()
  }, 30000)
})

// 组件卸载时清除定时器
onUnmounted(() => {
  if (unreadCountInterval) {
    clearInterval(unreadCountInterval)
  }
})

// 监听路由变化，当进入通知页面时刷新未读数量
watch(() => router.currentRoute.value.path, (newPath) => {
  if (newPath === '/front/messages') {
    loadUnreadCount()
  }
})
</script>

<style scoped>
@import "@/assets/css/front.css";
</style>

<style>
/* 全局样式，确保菜单项清晰可见 */
.custom-header-menu .ant-menu-item {
  color: #ffffff !important;
  font-weight: 500 !important;
  font-size: 15px !important;
}

.custom-header-menu .ant-menu-item a {
  color: #ffffff !important;
}

.custom-header-menu .ant-menu-item:hover {
  color: #1890ff !important;
  background-color: rgba(24, 144, 255, 0.2) !important;
}

.custom-header-menu .ant-menu-item:hover a {
  color: #1890ff !important;
}

.custom-header-menu .ant-menu-item-selected {
  color: #1890ff !important;
  background-color: rgba(24, 144, 255, 0.25) !important;
  font-weight: 600 !important;
}

.custom-header-menu .ant-menu-item-selected a {
  color: #1890ff !important;
}

.custom-header-menu .ant-menu-item-selected::after {
  border-bottom: 3px solid #1890ff !important;
}
</style>
