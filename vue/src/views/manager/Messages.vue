<template>
  <a-card>
    <template #title>
      <h3>系统通知管理</h3>
    </template>
    
    <!-- 筛选条件 -->
    <a-form :model="searchForm" layout="inline" style="margin-bottom: 20px">
      <a-form-item label="通知内容">
        <a-input v-model:value="searchForm.content" placeholder="请输入通知标题或内容关键词" allow-clear style="width: 200px" />
      </a-form-item>
      <a-form-item label="是否已读">
        <a-select v-model:value="searchForm.isRead" placeholder="请选择" allow-clear style="width: 120px">
          <a-select-option :value="false">未读</a-select-option>
          <a-select-option :value="true">已读</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="loadMessages">搜索</a-button>
        <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
      </a-form-item>
    </a-form>

    <!-- 通知列表 -->
    <a-table
      :columns="columns"
      :data-source="messageList"
      :pagination="pagination"
      :loading="loading"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'title'">
          <div style="max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap" :title="record.title">
            <strong>{{ record.title }}</strong>
            <div style="font-size: 12px; color: #666; margin-top: 4px">{{ record.content }}</div>
          </div>
        </template>
        <template v-if="column.key === 'type'">
          <a-tag :color="getTypeColor(record.type)">
            {{ getTypeText(record.type) }}
          </a-tag>
        </template>
        <template v-if="column.key === 'projectTitle'">
          <a v-if="record.projectId && record.projectTitle" @click="handleViewProject(record.projectId)">
            {{ record.projectTitle }}
          </a>
          <span v-else style="color: #999">无</span>
        </template>
        <template v-if="column.key === 'isRead'">
          <a-tag :color="record.isRead ? 'default' : 'red'">
            {{ record.isRead ? '已读' : '未读' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-button type="link" @click="handleViewDetail(record)">查看详情</a-button>
          <a-button v-if="record.type === 'CERTIFICATE' && record.deliverableId" type="link" @click="handleViewCertificate(record.deliverableId)">查看证书</a-button>
          <a-button type="link" danger @click="handleDelete(record.id)">删除</a-button>
        </template>
      </template>
    </a-table>

    <!-- 详情模态框 -->
    <a-modal
      v-model:open="detailVisible"
      title="通知详情"
      :footer="null"
      :width="600"
    >
      <a-descriptions :column="1" bordered v-if="currentMessage">
        <a-descriptions-item label="通知标题">
          <strong>{{ currentMessage.title }}</strong>
        </a-descriptions-item>
        <a-descriptions-item label="通知内容">
          <div style="white-space: pre-wrap">{{ currentMessage.content }}</div>
        </a-descriptions-item>
        <a-descriptions-item label="通知类型">
          <a-tag :color="getTypeColor(currentMessage.type)">
            {{ getTypeText(currentMessage.type) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="关联项目">
          <a v-if="currentMessage.projectId && currentMessage.projectTitle" @click="handleViewProject(currentMessage.projectId)">
            {{ currentMessage.projectTitle }}
          </a>
          <span v-else style="color: #999">无</span>
        </a-descriptions-item>
        <a-descriptions-item label="是否已读">
          <a-tag :color="currentMessage.isRead ? 'default' : 'red'">
            {{ currentMessage.isRead ? '已读' : '未读' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ formatDate(currentMessage.createdAt) }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </a-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()

const loading = ref(false)
const messageList = ref([])
const detailVisible = ref(false)
const currentMessage = ref(null)

const searchForm = reactive({
  content: '',
  isRead: null
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total) => `共 ${total} 条`,
  onChange: (page, pageSize) => {
    pagination.current = page
    pagination.pageSize = pageSize
    loadMessages()
  }
})

const columns = [
  { title: '通知标题', key: 'title', ellipsis: true, width: 300 },
  { title: '通知类型', key: 'type', width: 120 },
  { title: '关联项目', key: 'projectTitle', ellipsis: true, width: 150 },
  { title: '是否已读', key: 'isRead', width: 100 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
]

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 19)
}

const getTypeText = (type) => {
  const map = {
    'CERTIFICATE': '证书通知',
    'PROJECT_STATUS_CHANGE': '项目通知',
    'CERTIFICATION': '认证通知'
  }
  return map[type] || type || '未知'
}

const getTypeColor = (type) => {
  const map = {
    'CERTIFICATE': 'orange',
    'PROJECT_STATUS_CHANGE': 'blue',
    'CERTIFICATION': 'green'
  }
  return map[type] || 'default'
}

const handleViewCertificate = async (certificateId) => {
  // 通过证书ID获取证书信息，然后跳转到对应的自由职业者详情
  try {
    const res = await request.get(`/api/certificates/admin/${certificateId}`)
    if (res.code === '200' && res.data && res.data.freelancerId) {
      // 跳转到自由职业者管理页面并打开详情
      router.push('/manager/freelancers')
      // 使用 nextTick 确保路由跳转完成后再打开详情
      setTimeout(() => {
        // 触发查看自由职业者详情的事件
        window.dispatchEvent(new CustomEvent('viewFreelancerDetail', { detail: { freelancerId: res.data.freelancerId } }))
      }, 300)
    } else {
      message.error('获取证书信息失败')
    }
  } catch (error) {
    console.error('获取证书信息失败:', error)
    message.error('获取证书信息失败')
  }
}

const loadMessages = async () => {
  loading.value = true
  try {
    // 管理员使用通知接口，只查看管理员相关的通知
    const res = await request.get('/api/notifications')
    if (res.code === '200' && res.data) {
      let list = res.data || []
      
      // 管理员只应该看到以下类型的通知：
      // 1. CERTIFICATE（证书上传通知）
      // 2. PROJECT_STATUS_CHANGE（项目状态变更，如新项目发布需要审核）
      // 3. CERTIFICATION（企业认证相关通知）
      // 过滤掉其他类型的通知
      list = list.filter(notif => {
        const allowedTypes = ['CERTIFICATE', 'PROJECT_STATUS_CHANGE', 'CERTIFICATION']
        return allowedTypes.includes(notif.type)
      })
      
      // 根据搜索条件过滤
      if (searchForm.content) {
        list = list.filter(notif => 
          (notif.title && notif.title.includes(searchForm.content)) ||
          (notif.content && notif.content.includes(searchForm.content))
        )
      }
      
      if (searchForm.isRead !== null && searchForm.isRead !== undefined && searchForm.isRead !== '') {
        list = list.filter(notif => notif.isRead === searchForm.isRead)
      }
      
      // 加载项目标题（如果有projectId）
      for (const notif of list) {
        if (notif.projectId) {
          try {
            const projectRes = await request.get(`/api/projects/${notif.projectId}`)
            if (projectRes.code === '200' && projectRes.data) {
              notif.projectTitle = projectRes.data.title
            }
          } catch (error) {
            console.error('加载项目信息失败:', error)
          }
        }
      }
      
      messageList.value = list
      pagination.total = list.length
    } else {
      message.error(res.msg || '加载通知列表失败')
    }
  } catch (error) {
    console.error('加载通知列表失败:', error)
    message.error('加载通知列表失败')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  searchForm.content = ''
  searchForm.isRead = null
  pagination.current = 1
  loadMessages()
}

const handleViewDetail = async (record) => {
  currentMessage.value = record
  detailVisible.value = true
  
  // 如果未读，标记为已读
  if (!record.isRead) {
    try {
      const res = await request.put(`/api/notifications/${record.id}/read`)
      if (res.code === '200') {
        // 更新本地状态
        record.isRead = true
        currentMessage.value.isRead = true
      }
    } catch (error) {
      console.error('标记已读失败:', error)
    }
  }
  
  // 如果是证书通知，可以跳转到证书详情
  if (record.type === 'CERTIFICATE' && record.deliverableId) {
    // deliverableId 字段在这里存储的是 certificateId
    // 可以跳转到自由职业者详情页面查看证书
  }
}

const handleViewProject = (projectId) => {
  router.push(`/manager/projects/${projectId}`)
}

const handleDelete = (id) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除此通知吗？',
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

