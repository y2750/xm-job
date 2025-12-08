<template>
  <div class="manager-container">
    <div class="manager-header">
      <div class="manager-header-left">
        <img src="@/assets/imgs/logo.png" alt="">
        <div class="title">自由职业者外包平台管理后台</div>
      </div>
      <div class="manager-header-center">
        <a-breadcrumb>
          <a-breadcrumb-item>
            <a @click="router.push('/manager/home')">首页</a>
          </a-breadcrumb-item>
          <a-breadcrumb-item>{{ router.currentRoute.value.meta?.name }}</a-breadcrumb-item>
        </a-breadcrumb>
      </div>
      <div class="manager-header-right">
        <a-dropdown>
          <div style="padding-right: 20px; display: flex; align-items: center; cursor: pointer">
            <img style="width: 40px; height: 40px; border-radius: 50%;" :src="data.user.avatar" alt="">
            <span style="margin-left: 5px; color: white">{{ data.user.name }}</span>
            <DownOutlined style="margin-left: 5px; color: white" />
          </div>
          <template #overlay>
            <a-menu>
              <a-menu-item v-if="data.user.role === 'ADMIN'" @click="router.push('/manager/person')">个人资料</a-menu-item>
              <a-menu-item v-if="data.user.role === 'EMPLOY'" @click="router.push('/manager/ePerson')">企业资料</a-menu-item>
              <a-menu-item @click="router.push('/manager/password')">修改密码</a-menu-item>
              <a-menu-item @click="logout">退出登录</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
    </div>
    <!-- 下面部分开始 -->
    <div style="display: flex">
      <div class="manager-main-left">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          v-model:openKeys="openKeys"
          mode="inline"
          @click="handleMenuClick"
        >
          <a-menu-item v-if="data.user.role === 'ADMIN'" key="/manager/home">
            <template #icon>
              <HomeOutlined />
            </template>
            <span>系统首页</span>
          </a-menu-item>
          <a-sub-menu key="1">
            <template #icon>
              <MenuOutlined />
            </template>
            <template #title>业务管理</template>
            <a-menu-item v-if="data.user.role === 'ADMIN'" key="/manager/projects">项目管理</a-menu-item>
            <a-menu-item v-if="data.user.role === 'ADMIN'" key="/manager/submissions">稿件管理</a-menu-item>
          </a-sub-menu>
          <a-sub-menu v-if="data.user.role === 'ADMIN'" key="2">
            <template #icon>
              <MenuOutlined />
            </template>
            <template #title>用户管理</template>
            <a-menu-item key="/manager/admin">管理员信息</a-menu-item>
            <a-menu-item key="/manager/freelancers">自由职业者</a-menu-item>
            <a-menu-item key="/manager/enterprises">企业用户</a-menu-item>
          </a-sub-menu>
          <a-sub-menu v-if="data.user.role === 'ADMIN'" key="3">
            <template #icon>
              <MenuOutlined />
            </template>
            <template #title>系统管理</template>
            <a-menu-item key="/manager/notice">系统公告</a-menu-item>
            <a-menu-item key="/manager/messages">系统通知</a-menu-item>
          </a-sub-menu>
        </a-menu>
      </div>
      <div class="manager-main-right">
        <RouterView @updateUser="updateUser" />
      </div>
    </div>
    <!-- 下面部分结束 -->
  </div>
</template>

<script setup>
import { reactive, ref, watch } from "vue";
import router from "@/router/index.js";
import { message } from "ant-design-vue";
import { HomeOutlined, MenuOutlined, DownOutlined } from "@ant-design/icons-vue";

const data = reactive({
  user: JSON.parse(localStorage.getItem('xm-user') || '{}')
})

const selectedKeys = ref([router.currentRoute.value.path])
const openKeys = ref(['1', '2', '3'])

// 监听路由变化，更新选中菜单
watch(() => router.currentRoute.value.path, (newPath) => {
  selectedKeys.value = [newPath]
  // 根据路径自动展开对应的子菜单
  if (newPath.includes('/manager/projects') || newPath.includes('/manager/submissions')) {
    openKeys.value = ['1']
  } else if (newPath.includes('/manager/admin') || newPath.includes('/manager/freelancers') || newPath.includes('/manager/enterprises')) {
    openKeys.value = ['2']
  } else if (newPath.includes('/manager/notice') || newPath.includes('/manager/messages')) {
    openKeys.value = ['3']
  }
}, { immediate: true })

const logout = () => {
  localStorage.removeItem('xm-user')
  router.push('/login')
}

const updateUser = () => {
  data.user = JSON.parse(localStorage.getItem('xm-user') || '{}')
}

const handleMenuClick = ({ key }) => {
  if (key && key !== router.currentRoute.value.path) {
    router.push(key).then(() => {
      selectedKeys.value = [key]
    }).catch(err => {
      console.error('路由跳转失败:', err)
    })
  }
}

if (!data.user.id) {
  logout()
  message.error('请登录！')
}
</script>

<style scoped>
@import "@/assets/css/manager.css";
</style>
