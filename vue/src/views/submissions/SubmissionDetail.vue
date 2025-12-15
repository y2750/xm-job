<template>
  <div class="submission-detail-container">
    <a-card v-if="submission" :loading="loading">
      <template #title>
        <h2>{{ submission.title }}</h2>
      </template>

      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="项目标题">{{ submission.projectTitle }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getStatusColor(submission.status)">{{ getStatusText(submission.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="报价">
          {{ submission.quotePrice ? `¥${submission.quotePrice}` : '未报价' }}
        </a-descriptions-item>
        <a-descriptions-item label="提交时间">{{ formatDate(submission.createdAt) }}</a-descriptions-item>
        <a-descriptions-item label="完成思路" :span="2">
          <div v-html="submission.description"></div>
        </a-descriptions-item>
      </a-descriptions>

      <a-divider>附件列表</a-divider>
      <a-list
        v-if="submission.attachments && submission.attachments.length > 0"
        :data-source="submission.attachments"
      >
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>
                <a @click="handleDownload(item)">{{ item.fileName }}</a>
              </template>
              <template #description>
                {{ formatFileSize(item.fileSize) }} | {{ item.fileType }}
              </template>
            </a-list-item-meta>
            <template #actions>
              <a-button type="link" @click="handleDownload(item)">下载</a-button>
            </template>
          </a-list-item>
        </template>
      </a-list>
      <a-empty v-else description="暂无附件" />

      <!-- 有意向状态时的聊天和修改报价功能 -->
      <a-divider v-if="submission.status === 'INTERESTED' && submission.projectStatus !== 'COMPLETED'">沟通与报价</a-divider>
      <div v-if="submission.status === 'INTERESTED' && submission.projectStatus !== 'COMPLETED'">
        <!-- 修改报价（仅自由职业者可见） -->
        <div v-if="!isEnterprise" style="margin-bottom: 20px">
          <a-form :model="quoteForm" layout="inline">
            <a-form-item label="报价">
              <a-input-number
                v-model:value="quoteForm.quotePrice"
                :min="0"
                :precision="2"
                :formatter="value => `¥ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')"
                :parser="value => value.replace(/¥\s?|(,*)/g, '')"
                style="width: 200px"
              />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="handleUpdateQuote" :loading="updatingQuote">
                修改报价
              </a-button>
            </a-form-item>
          </a-form>
        </div>

        <!-- 进入聊天界面按钮 -->
        <div style="margin-bottom: 20px; text-align: center">
          <a-button type="primary" size="large" @click="handleGoToChat">
            <template #icon><MessageOutlined /></template>
            进入聊天界面
          </a-button>
        </div>
      </div>

      <!-- 已确认合作状态时的聊天功能 -->
      <a-divider v-if="submission.status === 'CONFIRMED' && submission.projectStatus !== 'COMPLETED'">沟通</a-divider>
      <div v-if="submission.status === 'CONFIRMED' && submission.projectStatus !== 'COMPLETED'">
        <!-- 进入聊天界面按钮 -->
        <div style="margin-bottom: 20px; text-align: center">
          <a-button type="primary" size="large" @click="handleGoToChat">
            <template #icon><MessageOutlined /></template>
            进入聊天界面
          </a-button>
        </div>
      </div>

      <div style="margin-top: 20px; text-align: right">
        <a-button @click="handleBack">返回</a-button>
        <a-button
          v-if="isEnterprise && submission.status === 'SUBMITTED'"
          type="primary"
          style="margin-left: 8px"
          @click="handleMarkInterested"
        >
          标记为有意向
        </a-button>
        <a-button
          v-if="isEnterprise && submission.status === 'SUBMITTED'"
          danger
          style="margin-left: 8px"
          @click="handleReject"
        >
          拒绝
        </a-button>
        <a-button
          v-if="isEnterprise && submission.status === 'INTERESTED' && !submission.enterpriseConfirmed"
          type="primary"
          style="margin-left: 8px"
          @click="handleConfirm"
        >
          确定合作
        </a-button>
        <a-tag v-if="isEnterprise && submission.status === 'INTERESTED' && submission.enterpriseConfirmed" color="green" style="margin-left: 8px">
          企业已确认，等待接单者确认
        </a-tag>
        <a-button
          v-if="!isEnterprise && submission.status === 'INTERESTED' && submission.enterpriseConfirmed && !submission.freelancerConfirmed"
          type="primary"
          style="margin-left: 8px"
          @click="handleFreelancerConfirm"
        >
          确认合作并支付保证金
        </a-button>
        <a-tag v-if="!isEnterprise && submission.status === 'INTERESTED' && submission.freelancerConfirmed" color="green" style="margin-left: 8px">
          您已确认合作
        </a-tag>
      </div>

      <!-- 成品验收区域（企业端，CONFIRMED状态） -->
      <a-divider v-if="isEnterprise && submission.status === 'CONFIRMED'">成品验收</a-divider>
      <div v-if="isEnterprise && submission.status === 'CONFIRMED'">
        <a-list
          v-if="deliverables && deliverables.length > 0"
          :data-source="deliverables"
          style="margin-bottom: 20px"
        >
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta>
                <template #title>
                  {{ item.title }}
                  <a-tag :color="getDeliverableStatusColor(item.status)" style="margin-left: 8px">
                    {{ getDeliverableStatusText(item.status) }}
                  </a-tag>
                </template>
                <template #description>
                  <div v-html="item.description"></div>
                  <div style="margin-top: 8px">
                    提交时间：{{ formatDate(item.submittedAt) }}
                  </div>
                  <div v-if="item.reviewComment">
                    验收意见：{{ item.reviewComment }}
                  </div>
                </template>
              </a-list-item-meta>
              <template #actions>
                <a-button type="link" @click="handleViewDeliverable(item.id)">查看详情</a-button>
                <a-button
                  v-if="item.status === 'SUBMITTED'"
                  type="primary"
                  @click="handleReview(item.id, 'APPROVED')"
                >
                  验收通过
                </a-button>
                <a-button
                  v-if="item.status === 'SUBMITTED'"
                  danger
                  style="margin-left: 8px"
                  @click="handleReview(item.id, 'REJECTED')"
                >
                  验收不通过
                </a-button>
              </template>
            </a-list-item>
          </template>
        </a-list>
        <a-empty v-else description="暂无成品提交" />
      </div>
    </a-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { MessageOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submission = ref(null)
const deliverables = ref([])
const updatingQuote = ref(false)
const userRole = ref(localStorage.getItem('xm-user') ? JSON.parse(localStorage.getItem('xm-user')).role : '')

const isEnterprise = computed(() => userRole.value === 'EMPLOY')

const quoteForm = ref({
  quotePrice: null
})

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 19)
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

const loadSubmission = async () => {
  loading.value = true
  try {
    const res = await request.get(`/api/submissions/${route.params.id}`)
    if (res.code === '200') {
      submission.value = res.data
      // 设置报价表单的初始值
      if (submission.value && submission.value.quotePrice) {
        quoteForm.value.quotePrice = submission.value.quotePrice
      }
      // 如果是企业端且稿件状态为CONFIRMED，加载成品列表
      if (isEnterprise.value && submission.value && submission.value.status === 'CONFIRMED') {
        await loadDeliverables()
      }
    } else {
      message.error(res.msg)
    }
  } catch (error) {
    message.error('加载稿件详情失败')
  } finally {
    loading.value = false
  }
}

const handleUpdateQuote = async () => {
  if (!quoteForm.value.quotePrice || quoteForm.value.quotePrice <= 0) {
    message.warning('请输入有效的报价金额')
    return
  }

  if (!submission.value || !submission.value.id) {
    message.error('稿件信息不存在')
    return
  }

  updatingQuote.value = true
  try {
    const res = await request.put(`/api/submissions/${submission.value.id}/quote`, null, {
      params: {
        quotePrice: quoteForm.value.quotePrice
      }
    })
    if (res.code === '200') {
      message.success('报价修改成功')
      await loadSubmission()
    } else {
      message.error(res.msg)
    }
  } catch (error) {
    message.error('修改报价失败')
  } finally {
    updatingQuote.value = false
  }
}

const handleGoToChat = () => {
  if (!submission.value || !submission.value.id) {
    message.error('稿件信息不存在')
    return
  }
  router.push(`/front/chat/${submission.value.id}`)
}

const handleBack = () => {
  router.back()
}

const handleMarkInterested = () => {
  Modal.confirm({
    title: '确认操作',
    content: '确定要将此稿件标记为"有意向"吗？',
    onOk: async () => {
      try {
        const res = await request.put(`/api/submissions/${route.params.id}/status?status=INTERESTED`)
        if (res.code === '200') {
          message.success('操作成功')
          loadSubmission()
        } else {
          message.error(res.msg)
        }
      } catch (error) {
        message.error('操作失败')
      }
    }
  })
}

const handleReject = () => {
  Modal.confirm({
    title: '确认拒绝',
    content: '确定要拒绝此稿件吗？',
    onOk: async () => {
      try {
        const res = await request.put(`/api/submissions/${route.params.id}/status?status=REJECTED`)
        if (res.code === '200') {
          message.success('操作成功')
          loadSubmission()
        } else {
          message.error(res.msg)
        }
      } catch (error) {
        message.error('操作失败')
      }
    }
  })
}

const handleConfirm = () => {
  if (!submission.value || !submission.value.quotePrice) {
    message.error('稿件未设置报价，无法确定合作')
    return
  }
  
  // 跳转到支付界面
  router.push(`/front/payment/confirm/${route.params.id}`)
}

const handleFreelancerConfirm = () => {
  if (!submission.value || !submission.value.quotePrice) {
    message.error('稿件未设置报价，无法确定合作')
    return
  }
  
  // 跳转到接单者支付界面
  router.push(`/front/payment/freelancer-confirm/${route.params.id}`)
}

const loadDeliverables = async () => {
  if (!submission.value || !submission.value.id) return
  try {
    const res = await request.get('/api/deliverables', {
      params: {
        submissionId: submission.value.id
      }
    })
    if (res.code === '200') {
      deliverables.value = res.data || []
    }
  } catch (error) {
    console.error('加载成品列表失败:', error)
  }
}

const getDeliverableStatusColor = (status) => {
  const colors = {
    'SUBMITTED': 'blue',
    'APPROVED': 'green',
    'REJECTED': 'red',
    'EXPIRED': 'orange'
  }
  return colors[status] || 'default'
}

const getDeliverableStatusText = (status) => {
  const texts = {
    'SUBMITTED': '已提交',
    'APPROVED': '验收通过',
    'REJECTED': '验收不通过',
    'EXPIRED': '已过期'
  }
  return texts[status] || status
}

const handleViewDeliverable = (id) => {
  const deliverable = deliverables.value.find(d => d.id === id)
  if (!deliverable) {
    message.error('成品不存在')
    return
  }
  
  Modal.info({
    title: '成品详情',
    width: 800,
    content: h('div', [
      h('p', [h('strong', '标题：'), deliverable.title]),
      h('p', [h('strong', '描述：'), h('div', { innerHTML: deliverable.description })]),
      h('p', [h('strong', '状态：'), getDeliverableStatusText(deliverable.status)]),
      h('p', [h('strong', '提交时间：'), formatDate(deliverable.submittedAt)]),
      deliverable.reviewComment && h('p', [h('strong', '验收意见：'), deliverable.reviewComment])
    ])
  })
}

const handleReview = (deliverableId, status) => {
  const statusText = status === 'APPROVED' ? '验收通过' : '验收不通过'
  let content = `确定要${statusText}此成品吗？`
  if (status === 'APPROVED') {
    content += '通过后将增加接单者信誉分并支付项目款项。'
  } else {
    content += '不通过后，如果接单者还有提交机会，可以重新提交；如果已达到最大提交次数或超过截止时间，将扣除接单者信誉分并扣除保证金赔付给企业。'
  }
  Modal.confirm({
    title: `确认${statusText}`,
    content: content,
    onOk: async () => {
      try {
        const res = await request.put(`/api/deliverables/${deliverableId}/review`, null, {
          params: {
            status: status,
            reviewComment: ''
          }
        })
        if (res.code === '200') {
          message.success(`${statusText}成功`)
          await loadDeliverables()
        } else {
          message.error(res.msg || `${statusText}失败`)
        }
      } catch (error) {
        console.error(`${statusText}失败:`, error)
        message.error(`${statusText}失败`)
      }
    }
  })
}

const handleDownload = async (attachment) => {
  try {
    const fileName = attachment.fileName || 'download'
    // 从 fileUrl 中提取附件ID，或者直接使用附件ID
    // fileUrl 格式：http://localhost:9090/api/attachments/download/{id}
    let url = attachment.fileUrl
    if (!url || !url.includes('/api/attachments/download/')) {
      // 如果没有 fileUrl 或格式不对，使用附件ID构建URL
      url = `/api/attachments/download/${attachment.id}`
    } else {
      // 提取URL路径部分（去掉域名）
      try {
        const urlObj = new URL(url)
        url = urlObj.pathname + urlObj.search
      } catch (e) {
        // 如果已经是相对路径，直接使用
        if (!url.startsWith('/')) {
          url = `/api/attachments/download/${attachment.id}`
        }
      }
    }
    
    // 使用 axios 下载文件（会自动添加 token，后端会返回文件流）
    const res = await request.get(url, {
      responseType: 'blob'
    })
    
    // 检查是否是错误响应（JSON格式的错误信息）
    if (res instanceof Blob && res.size < 1024) {
      const text = await res.text()
      try {
        const errorData = JSON.parse(text)
        if (errorData.code && errorData.code !== '200') {
          message.error(errorData.msg || '下载失败')
          return
        }
      } catch (e) {
        // 不是 JSON，继续下载
      }
    }
    
    if (!(res instanceof Blob)) {
      message.error('下载失败：无效的响应')
      return
    }
    
    // 创建下载链接，强制下载到默认路径
    const blob = res  // 使用 res 作为 blob
    const safeFileName = (attachment.fileName || 'download').replace(/[<>:"/\\|?*]/g, '_')
    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = safeFileName  // 保持原始文件名（清理特殊字符）
    link.style.display = 'none'
    document.body.appendChild(link)
    
    try {
      link.click()
    } catch (clickError) {
      console.error('点击失败:', clickError)
      // 如果点击失败，尝试使用 window.open
      window.open(downloadUrl, '_blank')
    }
    
    // 延迟删除，确保下载开始
    setTimeout(() => {
      try {
        if (link.parentNode) {
          document.body.removeChild(link)
        }
        window.URL.revokeObjectURL(downloadUrl)
      } catch (e) {
        console.warn('清理链接时出错:', e)
      }
    }, 500)
    
    message.success('下载成功')
  } catch (error) {
    console.error('下载失败:', error)
    // 处理错误响应
    if (error.response && error.response.data) {
      try {
        // 如果响应是 Blob，尝试读取为文本
        if (error.response.data instanceof Blob) {
          const text = await error.response.data.text()
          const errorData = JSON.parse(text)
          message.error(errorData.msg || '下载失败')
        } else if (typeof error.response.data === 'string') {
          const errorData = JSON.parse(error.response.data)
          message.error(errorData.msg || '下载失败')
        } else {
          message.error(error.response.data.msg || '下载失败')
        }
      } catch (e) {
        message.error('下载失败')
      }
    } else {
      message.error('下载失败')
    }
  }
}

onMounted(() => {
  loadSubmission()
})
</script>

<style scoped>
.submission-detail-container {
  padding: 20px;
}
</style>

