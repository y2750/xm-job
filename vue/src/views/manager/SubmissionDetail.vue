<template>
  <div class="submission-detail-container">
    <a-card :loading="loading">
    <template #title>
      <a-space>
        <a-button type="link" @click="handleBack">
          <template #icon>
            <ArrowLeftOutlined />
          </template>
          返回
        </a-button>
        <span>稿件详情</span>
      </a-space>
    </template>

    <div v-if="!loading && !submission" style="padding: 40px; text-align: center;">
      <a-empty description="稿件不存在或加载失败" />
      <div style="margin-top: 20px; color: #999; font-size: 12px;">
        <p>请检查：</p>
        <p>1. 稿件ID是否正确</p>
        <p>2. 网络连接是否正常</p>
        <p>3. 查看浏览器控制台的错误信息</p>
      </div>
    </div>

    <a-descriptions v-if="submission" :column="2" bordered>
      <a-descriptions-item label="稿件标题">{{ submission.title || '未填写' }}</a-descriptions-item>
      <a-descriptions-item label="项目标题">{{ submission.projectTitle || '未知' }}</a-descriptions-item>
      <a-descriptions-item label="提交者">{{ submission.freelancerName || '未知' }}</a-descriptions-item>
      <a-descriptions-item label="报价">
        {{ submission.quotePrice ? `¥${submission.quotePrice}` : '未报价' }}
      </a-descriptions-item>
      <a-descriptions-item label="稿件状态">
        <a-tag :color="getStatusColor(submission.status)">{{ getStatusText(submission.status) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="提交时间">{{ formatDate(submission.createdAt) }}</a-descriptions-item>
      <a-descriptions-item label="稿件描述" :span="2">
        <div v-if="submission.description" v-html="submission.description"></div>
        <span v-else style="color: #999">未填写</span>
      </a-descriptions-item>
    </a-descriptions>

    <a-divider v-if="submission">稿件附件</a-divider>
    <div v-if="submission && submission.attachments && submission.attachments.length > 0" style="margin-bottom: 20px">
      <a-list :data-source="submission.attachments">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>
                <a @click="handleDownload(item.id)" style="cursor: pointer;">{{ item.fileName }}</a>
              </template>
              <template #description>
                <div>文件大小：{{ formatFileSize(item.fileSize) }}</div>
                <div>文件类型：{{ item.fileType }}</div>
                <div>上传时间：{{ formatDate(item.uploadTime) }}</div>
              </template>
            </a-list-item-meta>
            <template #actions>
              <a-button type="link" @click="handleDownload(item.id)">下载</a-button>
            </template>
          </a-list-item>
        </template>
      </a-list>
    </div>
    <a-empty v-else-if="submission" description="暂无附件" />
    </a-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submission = ref(null)

const loadSubmission = async () => {
  loading.value = true
  submission.value = null
  try {
    const id = route.params.id
    if (!id) {
      message.error('稿件ID不存在')
      loading.value = false
      return
    }
    console.log('=== 开始加载稿件详情 ===')
    console.log('稿件ID:', id)
    console.log('请求URL:', `/api/submissions/${id}`)
    
    const res = await request.get(`/api/submissions/${id}`)
    console.log('=== 收到响应 ===')
    console.log('响应完整数据:', res)
    console.log('响应code:', res.code)
    console.log('响应msg:', res.msg)
    console.log('响应data:', res.data)
    
    if (res && res.code === '200') {
      if (res.data) {
        submission.value = res.data
        console.log('=== 稿件数据设置成功 ===')
        console.log('稿件对象:', submission.value)
        console.log('稿件标题:', submission.value.title)
        console.log('项目标题:', submission.value.projectTitle)
        console.log('提交者:', submission.value.freelancerName)
        // 确保 attachments 是数组
        if (!submission.value.attachments) {
          submission.value.attachments = []
        }
        console.log('附件数量:', submission.value.attachments.length)
      } else {
        console.warn('响应data为空')
        message.warning('稿件数据为空')
        submission.value = null
      }
    } else {
      const errorMsg = res?.msg || '加载稿件详情失败'
      console.error('加载失败:', errorMsg)
      message.error(errorMsg)
      submission.value = null
    }
  } catch (error) {
    console.error('=== 加载稿件详情异常 ===')
    console.error('错误对象:', error)
    console.error('错误信息:', error.message)
    console.error('错误响应:', error.response)
    if (error.response) {
      console.error('响应状态:', error.response.status)
      console.error('响应数据:', error.response.data)
      message.error(error.response.data?.msg || `加载失败: ${error.response.status}`)
    } else {
      message.error('加载稿件详情失败，请检查网络连接')
    }
    submission.value = null
  } finally {
    loading.value = false
    console.log('=== 加载完成 ===')
    console.log('loading状态:', loading.value)
    console.log('submission值:', submission.value)
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  if (typeof dateStr === 'string') {
    return dateStr.replace('T', ' ').substring(0, 19)
  }
  return dateStr
}

const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const getStatusColor = (status) => {
  const colors = {
    'SUBMITTED': 'blue',
    'INTERESTED': 'green',
    'REJECTED': 'red',
    'CONFIRMED': 'purple'
  }
  return colors[status] || 'default'
}

const getStatusText = (status) => {
  const texts = {
    'SUBMITTED': '已提交',
    'INTERESTED': '有意向',
    'REJECTED': '已拒绝',
    'CONFIRMED': '已确定合作'
  }
  return texts[status] || status
}

const handleBack = () => {
  router.push('/manager/submissions')
}

const downloadFile = async (url, fileName) => {
  try {
    const res = await request.get(url, {
      responseType: 'blob'
    })
    if (res instanceof Blob) {
      // 创建下载链接
      const blobUrl = window.URL.createObjectURL(res)
      const link = document.createElement('a')
      link.href = blobUrl
      link.download = fileName
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(blobUrl)
      message.success('下载成功')
    } else {
      message.error('下载失败')
    }
  } catch (error) {
    console.error('下载附件失败:', error)
    // 如果是 JSON 错误响应，尝试解析
    if (error.response && error.response.data instanceof Blob) {
      const reader = new FileReader()
      reader.onload = () => {
        try {
          const errorData = JSON.parse(reader.result)
          message.error(errorData.msg || '下载失败')
        } catch (e) {
          message.error('下载失败')
        }
      }
      reader.readAsText(error.response.data)
    } else {
      message.error('下载失败')
    }
  }
}

const handleDownload = (attachmentId) => {
  if (!submission.value || !submission.value.attachments) {
    message.error('附件数据不存在')
    return
  }
  const attachment = submission.value.attachments.find(a => a.id === attachmentId)
  if (!attachment) {
    message.error('附件不存在')
    return
  }
  
  // 使用 fileUrl 或构建下载 URL
  let url = attachment.fileUrl
  if (!url || !url.includes('/api/attachments/')) {
    url = `/api/attachments/download/${attachmentId}`
  }
  
  downloadFile(url, attachment.fileName)
}

onMounted(() => {
  loadSubmission()
})
</script>

<style scoped>
.submission-detail-container {
  padding: 20px;
}

.ant-descriptions-item-label {
  font-weight: 500;
}
</style>

