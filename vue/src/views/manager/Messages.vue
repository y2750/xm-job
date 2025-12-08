<template>
  <a-card>
    <template #title>
      <h3>系统通知管理</h3>
    </template>
    
    <!-- 筛选条件 -->
    <a-form :model="searchForm" layout="inline" style="margin-bottom: 20px">
      <a-form-item label="通知内容">
        <a-input v-model:value="searchForm.content" placeholder="请输入通知内容关键词" allow-clear style="width: 200px" />
      </a-form-item>
      <a-form-item label="关联项目">
        <a-input v-model:value="searchForm.projectTitle" placeholder="请输入项目标题" allow-clear style="width: 200px" />
      </a-form-item>
      <a-form-item label="通知类型">
        <a-select v-model:value="searchForm.notificationType" placeholder="请选择" allow-clear style="width: 150px">
          <a-select-option value="system">系统通知</a-select-option>
          <a-select-option value="chat">聊天消息</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="发送者类型">
        <a-select v-model:value="searchForm.senderType" placeholder="请选择" allow-clear style="width: 150px">
          <a-select-option value="ENTERPRISE">企业</a-select-option>
          <a-select-option value="FREELANCER">自由职业者</a-select-option>
        </a-select>
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
          <div style="max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap" :title="record.content">
            {{ record.content }}
          </div>
        </template>
        <template v-if="column.key === 'projectTitle'">
          <a v-if="record.projectId && record.projectTitle" @click="handleViewProject(record.projectId)">
            {{ record.projectTitle }}
          </a>
          <span v-else style="color: #999">无</span>
        </template>
        <template v-if="column.key === 'senderType'">
          <a-tag :color="getSenderTypeColor(record)">
            {{ getSenderTypeText(record) }}
          </a-tag>
        </template>
        <template v-if="column.key === 'isRead'">
          <a-tag :color="record.isRead ? 'default' : 'red'">
            {{ record.isRead ? '已读' : '未读' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-button type="link" @click="handleViewDetail(record)">查看详情</a-button>
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
        <a-descriptions-item label="通知内容">
          <div style="white-space: pre-wrap">{{ currentMessage.content }}</div>
        </a-descriptions-item>
        <a-descriptions-item label="关联项目">
          <a v-if="currentMessage.projectId && currentMessage.projectTitle" @click="handleViewProject(currentMessage.projectId)">
            {{ currentMessage.projectTitle }}
          </a>
          <span v-else style="color: #999">无</span>
        </a-descriptions-item>
        <a-descriptions-item label="发送者类型">
          <a-tag :color="getSenderTypeColor(currentMessage)">
            {{ getSenderTypeText(currentMessage) }}
          </a-tag>
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
  projectTitle: '',
  notificationType: null,
  senderType: null,
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
  { title: '通知内容', key: 'title', ellipsis: true },
  { title: '关联项目', dataIndex: 'projectTitle', key: 'projectTitle', ellipsis: true },
  { title: '发送者类型', key: 'senderType', width: 120 },
  { title: '是否已读', key: 'isRead', width: 100 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 19)
}

const getSenderTypeText = (record) => {
  // 如果senderId为0或null，表示是系统通知
  if (record.senderId === 0 || record.senderId === null) {
    return '系统通知'
  }
  const map = {
    'ENTERPRISE': '企业',
    'FREELANCER': '自由职业者'
  }
  return map[record.senderType] || record.senderType || '未知'
}

const getSenderTypeColor = (record) => {
  // 如果senderId为0或null，表示是系统通知
  if (record.senderId === 0 || record.senderId === null) {
    return 'blue'
  }
  const map = {
    'ENTERPRISE': 'green',
    'FREELANCER': 'orange'
  }
  return map[record.senderType] || 'default'
}

const loadMessages = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    
    // 构建查询条件
    if (searchForm.content) {
      params.content = searchForm.content
    }
    if (searchForm.projectTitle) {
      params.projectTitle = searchForm.projectTitle
    }
    if (searchForm.senderType !== null && searchForm.senderType !== undefined && searchForm.senderType !== '') {
      params.senderType = searchForm.senderType
    }
    if (searchForm.isRead !== null && searchForm.isRead !== undefined && searchForm.isRead !== '') {
      params.isRead = searchForm.isRead
    }
    
    const res = await request.get('/api/messages', { params })
    if (res.code === '200' && res.data) {
      let list = res.data.list || []
      
      // 根据通知类型过滤
      if (searchForm.notificationType === 'system') {
        // 只显示系统通知（senderId为0或null，且没有submissionId）
        list = list.filter(msg => (msg.senderId === 0 || msg.senderId === null) && !msg.submissionId)
      } else if (searchForm.notificationType === 'chat') {
        // 只显示聊天消息（有submissionId的消息）
        list = list.filter(msg => msg.submissionId != null)
      } else {
        // 默认只显示系统通知（过滤掉聊天消息）
        list = list.filter(msg => !msg.submissionId)
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
  searchForm.projectTitle = ''
  searchForm.notificationType = null
  searchForm.senderType = null
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
      const res = await request.put(`/api/messages/${record.id}/read`)
      if (res.code === '200') {
        // 更新本地状态
        record.isRead = true
        currentMessage.value.isRead = true
      }
    } catch (error) {
      console.error('标记已读失败:', error)
    }
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
        const res = await request.delete(`/api/messages/${id}`)
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

