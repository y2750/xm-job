<template>
  <div class="message-detail-container">
    <a-card>
      <template #title>
        <h2>项目消息</h2>
      </template>

      <div v-if="project" style="margin-bottom: 20px">
        <a-descriptions :column="2" bordered>
          <a-descriptions-item label="项目标题">{{ project.title }}</a-descriptions-item>
          <a-descriptions-item label="项目状态">
            <a-tag :color="getStatusColor(project.status)">{{ getStatusText(project.status) }}</a-tag>
          </a-descriptions-item>
        </a-descriptions>
      </div>

      <a-divider>消息记录</a-divider>
      
      <a-list
        :data-source="messageList"
        :loading="loading"
      >
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>
                <span>{{ item.senderName }}</span>
                <span style="margin-left: 8px; color: #999; font-size: 12px">
                  {{ formatDate(item.createdAt) }}
                </span>
              </template>
              <template #description>
                <div>{{ item.content }}</div>
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>

      <a-divider>发送消息</a-divider>
      
      <a-form :model="messageForm" ref="messageFormRef" layout="inline">
        <a-form-item style="width: 100%">
          <a-textarea
            v-model:value="messageForm.content"
            :rows="4"
            placeholder="请输入消息内容"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleSend" :loading="sending">发送</a-button>
          <a-button style="margin-left: 8px" @click="handleBack">返回</a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const sending = ref(false)
const messageList = ref([])
const project = ref(null)

const messageForm = reactive({
  content: ''
})

const messageFormRef = ref()

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 19)
}

const getStatusColor = (status) => {
  const colors = {
    'PUBLISHED': 'green',
    'CLOSED': 'orange',
    'CONFIRMED': 'blue'
  }
  return colors[status] || 'default'
}

const getStatusText = (status) => {
  const texts = {
    'PUBLISHED': '已发布',
    'CLOSED': '已截止',
    'CONFIRMED': '已确定合作'
  }
  return texts[status] || status
}

const loadProject = async () => {
  try {
    const res = await request.get(`/api/projects/${route.params.projectId}`)
    if (res.code === '200') {
      project.value = res.data
    }
  } catch (error) {
    console.error('加载项目失败', error)
  }
}

const loadMessages = async () => {
  loading.value = true
  try {
    const res = await request.get(`/api/messages/project/${route.params.projectId}`)
    if (res.code === '200') {
      messageList.value = res.data || []
    } else {
      message.error(res.msg)
    }
  } catch (error) {
    message.error('加载消息失败')
  } finally {
    loading.value = false
  }
}

const handleSend = async () => {
  if (!messageForm.content.trim()) {
    message.warning('请输入消息内容')
    return
  }

  sending.value = true
  try {
    const user = JSON.parse(localStorage.getItem('xm-user') || '{}')
    const res = await request.post('/api/messages', {
      projectId: route.params.projectId,
      content: messageForm.content,
      recipientId: user.role === 'EMPLOY' ? 1 : 1 // TODO: 需要根据实际情况设置接收者ID
    })
    if (res.code === '200') {
      message.success('发送成功')
      messageForm.content = ''
      loadMessages()
    } else {
      message.error(res.msg)
    }
  } catch (error) {
    message.error('发送失败')
  } finally {
    sending.value = false
  }
}

const handleBack = () => {
  router.back()
}

onMounted(() => {
  loadProject()
  loadMessages()
})
</script>

<style scoped>
.message-detail-container {
  padding: 20px;
}
</style>

