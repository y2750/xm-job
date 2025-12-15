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
          <div class="user-dropdown-trigger">
            <a-avatar :size="36" :src="data.user.avatar" class="user-avatar">
              {{ data.user.name ? data.user.name.charAt(0) : '?' }}
            </a-avatar>
            <span class="user-name">{{ data.user.name }}</span>
            <DownOutlined class="dropdown-icon" />
          </div>
          <template #overlay>
            <a-menu class="user-dropdown-menu">
              <a-menu-item v-if="data.user.role === 'ADMIN'" @click="router.push('/manager/person')">
                <UserOutlined />
                <span>个人资料</span>
              </a-menu-item>
              <a-menu-item v-if="data.user.role === 'EMPLOY'" @click="router.push('/manager/ePerson')">
                <BankOutlined />
                <span>企业资料</span>
              </a-menu-item>
              <a-menu-item @click="router.push('/manager/password')">
                <LockOutlined />
                <span>修改密码</span>
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
              <ProjectOutlined />
            </template>
            <template #title>业务管理</template>
            <a-menu-item v-if="data.user.role === 'ADMIN'" key="/manager/projects">
              <FileTextOutlined />
              项目管理
            </a-menu-item>
            <a-menu-item v-if="data.user.role === 'ADMIN'" key="/manager/submissions">
              <ContainerOutlined />
              稿件管理
            </a-menu-item>
          </a-sub-menu>
          <a-sub-menu v-if="data.user.role === 'ADMIN'" key="2">
            <template #icon>
              <TeamOutlined />
            </template>
            <template #title>用户管理</template>
            <a-menu-item key="/manager/admin">
              <UserOutlined />
              管理员信息
            </a-menu-item>
            <a-menu-item key="/manager/freelancers">
              <SolutionOutlined />
              自由职业者
            </a-menu-item>
            <a-menu-item key="/manager/enterprises">
              <BankOutlined />
              企业用户
            </a-menu-item>
          </a-sub-menu>
          <a-sub-menu v-if="data.user.role === 'ADMIN'" key="3">
            <template #icon>
              <SettingOutlined />
            </template>
            <template #title>系统管理</template>
            <a-menu-item key="/manager/notice">
              <NotificationOutlined />
              系统公告
            </a-menu-item>
            <a-menu-item key="/manager/messages">
              <BellOutlined />
              系统通知
            </a-menu-item>
          </a-sub-menu>
        </a-menu>
      </div>
      <div class="manager-main-right">
        <RouterView @updateUser="updateUser" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, watch } from "vue";
import router from "@/router/index.js";
import { message } from "ant-design-vue";
import { 
  HomeOutlined, 
  DownOutlined,
  UserOutlined,
  BankOutlined,
  LockOutlined,
  LogoutOutlined,
  ProjectOutlined,
  FileTextOutlined,
  ContainerOutlined,
  TeamOutlined,
  SolutionOutlined,
  SettingOutlined,
  NotificationOutlined,
  BellOutlined
} from "@ant-design/icons-vue";

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

/* 用户下拉菜单样式 */
.user-dropdown-trigger {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  transition: background-color 0.2s ease;
}

.user-dropdown-trigger:hover {
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
  margin-left: 8px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
}

/* 用户下拉菜单 */
.user-dropdown-menu {
  min-width: 160px;
  padding: 8px !important;
  border-radius: 10px !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15) !important;
}

.user-dropdown-menu :deep(.ant-menu-item) {
  display: flex !important;
  align-items: center !important;
  gap: 10px !important;
  padding: 10px 16px !important;
  border-radius: 6px !important;
  margin: 2px 0 !important;
  font-size: 14px !important;
  color: var(--text-primary) !important;
}

.user-dropdown-menu :deep(.ant-menu-item:hover) {
  background: var(--primary-light) !important;
  color: var(--primary-color) !important;
}

.user-dropdown-menu .logout-item:hover {
  background: #fff2f0 !important;
  color: var(--error-color) !important;
}
</style>
