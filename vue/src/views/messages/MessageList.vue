<template>
  <div class="message-list-container">
    <div class="page-header">
      <div class="header-content">
        <div class="header-left">
          <BellOutlined class="header-icon" />
          <div class="header-text">
            <h2>通知中心</h2>
            <p>查看系统通知和项目状态变更</p>
          </div>
        </div>
        <div class="header-stats">
          <div class="stat-item">
            <span class="stat-value">{{ unreadCount }}</span>
            <span class="stat-label">未读</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value">{{ pagination.total }}</span>
            <span class="stat-label">全部</span>
          </div>
        </div>
      </div>
    </div>

    <div class="notification-content">
      <!-- 快捷筛选 -->
      <div class="filter-bar">
        <a-radio-group v-model:value="filterType" @change="handleFilterChange" button-style="solid">
          <a-radio-button value="all">全部</a-radio-button>
          <a-radio-button value="unread">未读</a-radio-button>
          <a-radio-button value="PROJECT_STATUS_CHANGE">项目通知</a-radio-button>
          <a-radio-button value="SYSTEM">系统通知</a-radio-button>
        </a-radio-group>
        <a-button v-if="unreadCount > 0" type="link" @click="handleMarkAllRead">
          <CheckOutlined />
          全部标为已读
        </a-button>
      </div>

      <!-- 通知列表 -->
      <a-spin :spinning="loading">
        <div v-if="messageList.length > 0" class="notification-list">
          <div 
            v-for="item in messageList" 
            :key="item.id" 
            class="notification-card"
            :class="{ unread: !item.isRead }"
            @click="handleViewDetail(item)"
          >
            <div class="notification-icon-wrapper" :class="getTypeClass(item.type)">
              <component :is="getTypeIcon(item.type)" />
            </div>
            <div class="notification-body">
              <div class="notification-header">
                <span class="notification-type">{{ getTypeText(item.type) }}</span>
                <span class="notification-time">{{ formatDate(item.createdAt) }}</span>
              </div>
              <h4 class="notification-title">{{ item.title }}</h4>
              <p class="notification-content-text">{{ item.content }}</p>
              <div class="notification-footer">
                <a-tag v-if="!item.isRead" color="error" class="unread-tag">
                  <span class="unread-dot"></span>
                  未读
                </a-tag>
                <a-tag v-else class="read-tag">已读</a-tag>
                <a-button type="link" size="small" danger @click.stop="handleDelete(item.id)">
                  <DeleteOutlined />
                  删除
                </a-button>
              </div>
            </div>
          </div>
        </div>
        <a-empty v-else description="暂无通知" class="empty-state">
          <template #image>
            <BellOutlined class="empty-icon" />
          </template>
        </a-empty>
      </a-spin>

      <!-- 分页 -->
      <div v-if="messageList.length > 0" class="pagination-container">
        <a-pagination
          v-model:current="pagination.current"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :show-total="pagination.showTotal"
          show-size-changer
          @change="handlePageChange"
        />
      </div>
    </div>

    <!-- 详情模态框 -->
    <a-modal
      v-model:open="detailVisible"
      title="通知详情"
      :footer="null"
      :width="560"
      class="notification-detail-modal"
    >
      <div v-if="currentMessage" class="detail-content">
        <div class="detail-header">
          <div class="detail-icon-wrapper" :class="getTypeClass(currentMessage.type)">
            <component :is="getTypeIcon(currentMessage.type)" />
          </div>
          <div class="detail-header-info">
            <span class="detail-type">{{ getTypeText(currentMessage.type) }}</span>
            <h3 class="detail-title">{{ currentMessage.title }}</h3>
          </div>
        </div>
        
        <div class="detail-body">
          <div class="detail-section">
            <label>通知内容</label>
            <div class="detail-text">{{ currentMessage.content }}</div>
          </div>
          
          <div class="detail-meta">
            <div class="meta-item">
              <ClockCircleOutlined />
              <span>{{ formatDate(currentMessage.createdAt) }}</span>
            </div>
            <div class="meta-item">
              <a-tag :color="currentMessage.isRead ? 'default' : 'error'">
                {{ currentMessage.isRead ? '已读' : '未读' }}
              </a-tag>
            </div>
          </div>
          
          <div v-if="currentMessage.projectId" class="detail-action">
            <a-button type="primary" @click="handleViewProject(currentMessage.projectId)">
              <EyeOutlined />
              查看关联项目
            </a-button>
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { 
  BellOutlined, 
  CheckOutlined, 
  DeleteOutlined, 
  ClockCircleOutlined,
  EyeOutlined,
  ProjectOutlined,
  SafetyCertificateOutlined,
  FileTextOutlined,
  WarningOutlined,
  NotificationOutlined,
  FileDoneOutlined
} from '@ant-design/icons-vue'
import request from '@/utils/request'

const router = useRouter()

const loading = ref(false)
const messageList = ref([])
const allMessages = ref([])
const detailVisible = ref(false)
const currentMessage = ref(null)
const filterType = ref('all')

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total) => `共 ${total} 条`
})

const unreadCount = computed(() => {
  return allMessages.value.filter(m => !m.isRead).length
})

const getTypeText = (type) => {
  const typeMap = {
    'PROJECT_STATUS_CHANGE': '项目状态变动',
    'SUBMISSION': '投稿通知',
    'VIOLATION': '违约通知',
    'SYSTEM': '系统通知',
    'DELIVERABLE': '成品通知',
    'CERTIFICATION': '认证通知'
  }
  return typeMap[type] || type
}

const getTypeIcon = (type) => {
  const iconMap = {
    'PROJECT_STATUS_CHANGE': ProjectOutlined,
    'SUBMISSION': FileTextOutlined,
    'VIOLATION': WarningOutlined,
    'SYSTEM': NotificationOutlined,
    'DELIVERABLE': FileDoneOutlined,
    'CERTIFICATION': SafetyCertificateOutlined
  }
  return iconMap[type] || BellOutlined
}

const getTypeClass = (type) => {
  const classMap = {
    'PROJECT_STATUS_CHANGE': 'type-project',
    'SUBMISSION': 'type-submission',
    'VIOLATION': 'type-violation',
    'SYSTEM': 'type-system',
    'DELIVERABLE': 'type-deliverable',
    'CERTIFICATION': 'type-certification'
  }
  return classMap[type] || 'type-default'
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) {
    return '今天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } else if (days === 1) {
    return '昨天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } else if (days < 7) {
    return days + '天前'
  } else {
    return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' }) + ' ' + 
           date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
}

const loadMessages = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/notifications')
    if (res.code === '200') {
      allMessages.value = res.data || []
      applyFilter()
    } else {
      message.error(res.msg)
    }
  } catch (error) {
    message.error('加载通知列表失败')
  } finally {
    loading.value = false
  }
}

const applyFilter = () => {
  let filtered = [...allMessages.value]
  
  if (filterType.value === 'unread') {
    filtered = filtered.filter(m => !m.isRead)
  } else if (filterType.value !== 'all') {
    filtered = filtered.filter(m => m.type === filterType.value)
  }
  
  pagination.total = filtered.length
  const start = (pagination.current - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  messageList.value = filtered.slice(start, end)
}

const handleFilterChange = () => {
  pagination.current = 1
  applyFilter()
}

const handlePageChange = (page, pageSize) => {
  pagination.current = page
  pagination.pageSize = pageSize
  applyFilter()
}

const handleViewDetail = async (record) => {
  currentMessage.value = record
  detailVisible.value = true
  
  if (!record.isRead) {
    try {
      const res = await request.put(`/api/notifications/${record.id}/read`)
      if (res.code === '200') {
        record.isRead = true
        currentMessage.value.isRead = true
        const idx = allMessages.value.findIndex(m => m.id === record.id)
        if (idx !== -1) {
          allMessages.value[idx].isRead = true
        }
      }
    } catch (error) {
      console.error('标记已读失败:', error)
    }
  }
}

const handleMarkAllRead = async () => {
  const unreadIds = allMessages.value.filter(m => !m.isRead).map(m => m.id)
  if (unreadIds.length === 0) return
  
  try {
    for (const id of unreadIds) {
      await request.put(`/api/notifications/${id}/read`)
    }
    allMessages.value.forEach(m => m.isRead = true)
    messageList.value.forEach(m => m.isRead = true)
    message.success('已全部标为已读')
  } catch (error) {
    message.error('操作失败')
  }
}

const handleViewProject = (projectId) => {
  detailVisible.value = false
  router.push(`/front/projects/${projectId}`)
}

const handleDelete = (id) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除此通知吗？',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await request.delete(`/api/notifications/${id}`)
        if (res.code === '200') {
          message.success('删除成功')
          loadMessages()
        } else {
          message.error(res.msg || '删除失败')
        }
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
}

onMounted(() => {
  loadMessages()
})
</script>

<style scoped>
.message-list-container {
  min-height: calc(100vh - 140px);
  background: linear-gradient(135deg, #f0f4f8 0%, #e8f0f5 100%);
}

/* 页面头部 */
.page-header {
  background: linear-gradient(135deg, #00a6a7 0%, #00c4c4 100%);
  padding: 32px 0;
  margin-bottom: 0;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  font-size: 40px;
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.15);
  padding: 16px;
  border-radius: 16px;
}

.header-text h2 {
  margin: 0 0 4px 0;
  font-size: 24px;
  font-weight: 600;
  color: #fff;
}

.header-text p {
  margin: 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.header-stats {
  display: flex;
  align-items: center;
  gap: 24px;
  background: rgba(255, 255, 255, 0.15);
  padding: 16px 28px;
  border-radius: 12px;
  backdrop-filter: blur(10px);
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 28px;
  font-weight: 700;
  color: #fff;
}

.stat-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: rgba(255, 255, 255, 0.3);
}

/* 内容区域 */
.notification-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  padding: 16px 20px;
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.filter-bar :deep(.ant-radio-group) {
  display: flex;
  gap: 8px;
}

.filter-bar :deep(.ant-radio-button-wrapper) {
  border-radius: 20px !important;
  border-color: #e8f0f5;
  padding: 0 20px;
  height: 36px;
  line-height: 34px;
}

.filter-bar :deep(.ant-radio-button-wrapper:first-child) {
  border-radius: 20px !important;
}

.filter-bar :deep(.ant-radio-button-wrapper:last-child) {
  border-radius: 20px !important;
}

.filter-bar :deep(.ant-radio-button-wrapper-checked) {
  background: linear-gradient(135deg, #00a6a7 0%, #00c4c4 100%) !important;
  border-color: #00a6a7 !important;
  color: #fff !important;
}

.filter-bar :deep(.ant-btn-link) {
  color: #00a6a7;
  font-weight: 500;
}

/* 通知列表 */
.notification-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notification-card {
  display: flex;
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.25s ease;
  border: 1px solid transparent;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.notification-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border-color: #e8f0f5;
}

.notification-card.unread {
  background: linear-gradient(135deg, #fff 0%, #f8fcfc 100%);
  border-left: 4px solid #00a6a7;
}

.notification-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
  margin-right: 16px;
}

.notification-icon-wrapper.type-project {
  background: linear-gradient(135deg, #e6f7f7 0%, #d4f0f0 100%);
  color: #00a6a7;
}

.notification-icon-wrapper.type-submission {
  background: linear-gradient(135deg, #e6f4ff 0%, #d4e8ff 100%);
  color: #1890ff;
}

.notification-icon-wrapper.type-violation {
  background: linear-gradient(135deg, #fff2e8 0%, #ffe8d4 100%);
  color: #fa8c16;
}

.notification-icon-wrapper.type-system {
  background: linear-gradient(135deg, #f0f5ff 0%, #e0e8ff 100%);
  color: #2f54eb;
}

.notification-icon-wrapper.type-deliverable {
  background: linear-gradient(135deg, #f6ffed 0%, #e8ffd4 100%);
  color: #52c41a;
}

.notification-icon-wrapper.type-certification {
  background: linear-gradient(135deg, #fff0f6 0%, #ffd4e8 100%);
  color: #eb2f96;
}

.notification-icon-wrapper.type-default {
  background: linear-gradient(135deg, #f5f5f5 0%, #e8e8e8 100%);
  color: #8c8c8c;
}

.notification-body {
  flex: 1;
  min-width: 0;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.notification-type {
  font-size: 12px;
  color: #8c9cac;
  font-weight: 500;
  background: #f5f8fc;
  padding: 2px 10px;
  border-radius: 10px;
}

.notification-time {
  font-size: 12px;
  color: #a0b0c0;
}

.notification-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a2b3c;
  margin: 0 0 8px 0;
  line-height: 1.4;
}

.notification-content-text {
  font-size: 14px;
  color: #5a6a7a;
  margin: 0 0 12px 0;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notification-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.unread-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #fff1f0;
  border-color: #ffccc7;
}

.unread-dot {
  width: 6px;
  height: 6px;
  background: #ff4d4f;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.read-tag {
  background: #f5f8fc;
  border-color: #e8f0f5;
  color: #8c9cac;
}

/* 分页 */
.pagination-container {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}

/* 空状态 */
.empty-state {
  background: #fff;
  border-radius: 12px;
  padding: 60px 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.empty-icon {
  font-size: 64px;
  color: #d0d8e0;
}

/* 详情弹窗 */
.notification-detail-modal :deep(.ant-modal-content) {
  border-radius: 16px;
  overflow: hidden;
}

.notification-detail-modal :deep(.ant-modal-header) {
  background: linear-gradient(135deg, #fafcff 0%, #f5f8fc 100%);
  border-bottom: 1px solid #e8f0f5;
  padding: 20px 24px;
}

.notification-detail-modal :deep(.ant-modal-title) {
  font-size: 18px;
  font-weight: 600;
  color: #1a2b3c;
}

.notification-detail-modal :deep(.ant-modal-body) {
  padding: 0;
}

.detail-content {
  padding: 24px;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px dashed #e8f0f5;
}

.detail-icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  flex-shrink: 0;
}

.detail-header-info {
  flex: 1;
}

.detail-type {
  font-size: 13px;
  color: #8c9cac;
  font-weight: 500;
  background: #f5f8fc;
  padding: 4px 12px;
  border-radius: 12px;
  display: inline-block;
  margin-bottom: 8px;
}

.detail-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a2b3c;
  margin: 0;
  line-height: 1.4;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-section label {
  display: block;
  font-size: 13px;
  color: #8c9cac;
  margin-bottom: 8px;
  font-weight: 500;
}

.detail-text {
  font-size: 14px;
  color: #3a4a5a;
  line-height: 1.8;
  background: #f8fafc;
  padding: 16px;
  border-radius: 10px;
  white-space: pre-wrap;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #fafcff;
  border-radius: 10px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #5a6a7a;
}

.meta-item .anticon {
  color: #00a6a7;
}

.detail-action {
  padding-top: 8px;
}

.detail-action :deep(.ant-btn-primary) {
  background: linear-gradient(135deg, #00a6a7 0%, #00c4c4 100%);
  border: none;
  height: 42px;
  padding: 0 24px;
  border-radius: 10px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(0, 166, 167, 0.3);
}

/* 响应式 */
@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    gap: 20px;
    text-align: center;
  }
  
  .header-left {
    flex-direction: column;
  }
  
  .filter-bar {
    flex-direction: column;
    gap: 12px;
  }
  
  .notification-card {
    flex-direction: column;
  }
  
  .notification-icon-wrapper {
    margin-right: 0;
    margin-bottom: 12px;
  }
}
</style>

