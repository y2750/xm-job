<template>
  <div class="message-list-container">
    <a-card>
      <template #title>
        <h2>通知列表</h2>
      </template>

      <!-- 通知列表表格 -->
      <a-table
        :columns="columns"
        :data-source="messageList"
        :pagination="pagination"
        :loading="loading"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'content'">
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
          <a-descriptions-item label="是否已读">
            <a-tag :color="currentMessage.isRead ? 'default' : 'red'">
              {{ currentMessage.isRead ? '已读' : '未读' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatDate(currentMessage.createdAt) }}</a-descriptions-item>
        </a-descriptions>
      </a-modal>
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import request from '@/utils/request'

const router = useRouter()

const loading = ref(false)
const messageList = ref([])
const detailVisible = ref(false)
const currentMessage = ref(null)

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
  { title: '通知内容', key: 'content', ellipsis: true },
  { title: '关联项目', dataIndex: 'projectTitle', key: 'projectTitle', ellipsis: true },
  { title: '是否已读', key: 'isRead', width: 100 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 19)
}

const loadMessages = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/messages', {
      params: {
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
        excludeSubmissionId: true  // 排除聊天消息
      }
    })
    if (res.code === '200') {
      messageList.value = res.data.list || []
      pagination.total = res.data.total || 0
    } else {
      message.error(res.msg)
    }
  } catch (error) {
    message.error('加载消息列表失败')
  } finally {
    loading.value = false
  }
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
  router.push(`/front/projects/${projectId}`)
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

<style scoped>
.message-list-container {
  padding: 20px;
}
</style>

