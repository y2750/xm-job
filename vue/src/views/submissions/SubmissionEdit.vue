<template>
  <div class="submission-edit-container">
    <a-card>
      <template #title>
        <h2>修改稿件</h2>
      </template>

      <a-form :model="form" :rules="rules" ref="formRef" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="稿件标题" name="title">
          <a-input v-model:value="form.title" placeholder="请输入稿件标题" />
        </a-form-item>
        <a-form-item label="完成思路" name="description">
          <a-textarea v-model:value="form.description" :rows="8" placeholder="请详细描述您的完成思路、时间规划等" />
        </a-form-item>
        <a-form-item label="报价（可选）" name="quotePrice">
          <a-input-number
            v-model:value="form.quotePrice"
            :min="0"
            :precision="2"
            placeholder="请输入报价"
            style="width: 100%"
          />
          <div style="color: #999; font-size: 12px; margin-top: 4px">
            如果项目已明确预算，可以不填写报价
          </div>
        </a-form-item>
        <a-form-item label="附件上传">
          <a-upload
            v-model:file-list="fileList"
            :before-upload="beforeUpload"
            :customRequest="handleUpload"
            multiple
            :max-count="10"
          >
            <a-button>
              <UploadOutlined /> 选择文件
            </a-button>
            <template #tip>
              <div class="ant-upload-tip">
                支持图片、文档等格式，单文件不超过50MB，总大小不超过200MB
              </div>
            </template>
          </a-upload>
          <div v-if="existingAttachments && existingAttachments.length > 0" style="margin-top: 16px">
            <div style="margin-bottom: 8px; font-weight: 500">已有附件：</div>
            <a-list :data-source="existingAttachments" size="small">
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-list-item-meta>
                    <template #title>
                      <a @click="handleDownload(item.id)">{{ item.fileName }}</a>
                    </template>
                    <template #description>
                      {{ formatFileSize(item.fileSize) }} | {{ item.fileType }}
                    </template>
                  </a-list-item-meta>
                  <template #actions>
                    <a-button type="link" danger @click="handleDeleteAttachment(item.id)">删除</a-button>
                  </template>
                </a-list-item>
              </template>
            </a-list>
          </div>
        </a-form-item>
        <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
          <a-button type="primary" @click="handleSubmit" :loading="submitting">保存修改</a-button>
          <a-button style="margin-left: 8px" @click="handleCancel">取消</a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { UploadOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const formRef = ref()
const submitting = ref(false)
const fileList = ref([])
const existingAttachments = ref([])
const submissionId = ref(null)

const form = reactive({
  title: '',
  description: '',
  quotePrice: null
})

const rules = {
  title: [{ required: true, message: '请输入稿件标题', trigger: 'blur' }],
  description: [{ required: true, message: '请输入完成思路', trigger: 'blur' }]
}

const beforeUpload = (file) => {
  const isLt50M = file.size / 1024 / 1024 < 50
  if (!isLt50M) {
    message.error('文件大小不能超过50MB')
    return false
  }
  return false // 阻止自动上传，使用自定义上传
}

const handleUpload = async ({ file, onSuccess, onError }) => {
  // 先不上传，等保存后再上传
  fileList.value.push(file)
  onSuccess()
}

const loadSubmission = async () => {
  try {
    let id = route.params.id
    if (!id) {
      message.error('稿件ID不存在')
      router.back()
      return
    }
    
    // 处理可能的 submission_7 格式，提取数字ID
    if (typeof id === 'string' && id.startsWith('submission_')) {
      id = id.replace('submission_', '')
    }
    // 确保是数字
    const idNum = parseInt(id)
    if (isNaN(idNum)) {
      message.error('稿件ID格式错误')
      router.back()
      return
    }
    
    const res = await request.get(`/api/submissions/${idNum}`)
    if (res.code === '200') {
      const submission = res.data
      submissionId.value = submission.id
      form.title = submission.title || ''
      form.description = submission.description || ''
      form.quotePrice = submission.quotePrice || null
      existingAttachments.value = submission.attachments || []
    } else {
      message.error(res.msg || '加载稿件失败')
      router.back()
    }
  } catch (error) {
    console.error('加载稿件失败:', error)
    message.error('加载稿件失败，请检查网络连接')
    router.back()
  }
}

const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const handleDownload = async (attachmentId) => {
  try {
    // 获取附件信息以获取文件名
    const attachment = existingAttachments.value.find(a => a.id === attachmentId)
    if (!attachment) {
      message.error('附件不存在')
      return
    }
    const fileName = attachment.fileName || 'download'
    
    console.log('开始下载附件:', attachmentId, fileName)
    
    // 使用原生 fetch 以便获取完整响应信息
    const userStr = localStorage.getItem('xm-user') || '{}'
    let token = ''
    try {
      const user = JSON.parse(userStr)
      token = user.token || ''
    } catch (e) {
      console.error('解析用户信息失败:', e)
    }
    
    const baseUrl = import.meta.env.VITE_BASE_URL || 'http://localhost:9090'
    const url = `${baseUrl}/api/attachments/download/${attachmentId}`
    
    console.log('下载URL:', url, 'Token:', token ? '已设置' : '未设置')
    
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'token': token
      }
    })
    
    console.log('响应状态:', response.status, response.statusText)
    console.log('响应头:', {
      'Content-Type': response.headers.get('Content-Type'),
      'Content-Disposition': response.headers.get('Content-Disposition'),
      'Content-Length': response.headers.get('Content-Length')
    })
    
    // 检查响应状态
    if (!response.ok) {
      console.error('响应状态错误:', response.status, response.statusText)
      // 错误响应，尝试解析 JSON
      const contentType = response.headers.get('Content-Type') || ''
      if (contentType.includes('application/json')) {
        const errorData = await response.json()
        console.error('错误响应:', errorData)
        message.error(errorData.msg || '下载失败')
        return
      } else {
        // 尝试解析 Blob 中的 JSON
        const blob = await response.blob()
        console.error('错误响应Blob大小:', blob.size)
        if (blob.size < 1024) {
          const text = await blob.text()
          console.error('错误响应内容:', text)
          try {
            const errorData = JSON.parse(text)
            message.error(errorData.msg || '下载失败')
            return
          } catch (e) {
            message.error('下载失败: ' + response.statusText)
            return
          }
        }
      }
      message.error('下载失败: ' + response.statusText)
      return
    }
    
    // 检查 Content-Type
    const contentType = response.headers.get('Content-Type') || ''
    console.log('Content-Type:', contentType)
    
    // 如果是 JSON 说明是错误响应
    if (contentType.includes('application/json')) {
      const errorData = await response.json()
      console.error('JSON错误响应:', errorData)
      message.error(errorData.msg || '下载失败')
      return
    }
    
    // 正常文件流，下载
    const blob = await response.blob()
    console.log('文件Blob大小:', blob.size, '字节', '类型:', blob.type)
    
    if (blob.size === 0) {
      console.error('文件大小为0')
      message.error('下载失败：文件为空')
      return
    }
    
    // 如果文件很小（小于1KB），可能是错误响应，检查一下
    if (blob.size < 1024) {
      // 克隆 Blob 以便检查
      const clonedBlob = blob.slice()
      const text = await clonedBlob.text()
      console.log('小文件内容预览:', text.substring(0, 200))
      try {
        const errorData = JSON.parse(text)
        if (errorData.code && errorData.code !== '200') {
          console.error('下载错误:', errorData)
          message.error(errorData.msg || '下载失败')
          return
        }
      } catch (e) {
        // 不是 JSON，继续下载（可能是真的小文件）
        console.log('不是JSON错误，继续下载')
      }
    }
    
    // 创建下载链接，强制下载到默认路径
    console.log('创建下载链接，文件名:', fileName, 'Blob大小:', blob.size)
    
    // 确保文件名不包含特殊字符
    const safeFileName = fileName.replace(/[<>:"/\\|?*]/g, '_')
    
    const blobUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = blobUrl
    link.download = safeFileName  // 保持原始文件名（清理特殊字符）
    link.style.display = 'none'
    document.body.appendChild(link)
    
    console.log('触发下载点击，链接:', link.href.substring(0, 50) + '...')
    
    try {
      link.click()
      console.log('点击成功')
    } catch (clickError) {
      console.error('点击失败:', clickError)
      // 如果点击失败，尝试使用 window.open
      window.open(blobUrl, '_blank')
    }
    
    // 延迟删除，确保下载开始
    setTimeout(() => {
      try {
        if (link.parentNode) {
          document.body.removeChild(link)
        }
        window.URL.revokeObjectURL(blobUrl)
        console.log('清理下载链接完成')
      } catch (e) {
        console.warn('清理链接时出错:', e)
      }
    }, 500)  // 增加延迟时间
    
    message.success('下载成功')
  } catch (error) {
    console.error('下载附件失败:', error)
    console.error('错误堆栈:', error.stack)
    if (error.message) {
      console.error('错误消息:', error.message)
    }
    message.error('下载失败：' + (error.message || '未知错误'))
  }
}

const handleDeleteAttachment = (attachmentId) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除此附件吗？',
    onOk: async () => {
      try {
        const res = await request.delete(`/api/attachments/${attachmentId}`)
        if (res.code === '200') {
          message.success('删除成功')
          existingAttachments.value = existingAttachments.value.filter(a => a.id !== attachmentId)
        } else {
          message.error(res.msg || '删除失败')
        }
      } catch (error) {
        console.error('删除附件失败:', error)
        message.error('删除失败')
      }
    }
  })
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    submitting.value = true
    
    // 先更新稿件信息
    const updateData = {
      id: submissionId.value,
      title: form.title,
      description: form.description,
      quotePrice: form.quotePrice
    }
    
    const res = await request.put('/api/submissions', updateData)
    if (res.code === '200') {
      // 如果有新附件，上传附件
      if (fileList.value.length > 0) {
        let successCount = 0
        let failCount = 0
        
        for (const file of fileList.value) {
          try {
            const formData = new FormData()
            const fileObj = file.originFileObj || file
            formData.append('file', fileObj)
            
            const uploadRes = await request.post(`/api/attachments/submission/${submissionId.value}`, formData)
            
            if (uploadRes.code === '200') {
              successCount++
            } else {
              failCount++
              console.error('附件上传失败:', uploadRes.msg)
            }
          } catch (error) {
            failCount++
            console.error('附件上传失败:', error)
          }
        }
        
        if (successCount > 0) {
          message.success(`稿件修改成功，${successCount} 个附件上传成功`)
        }
        if (failCount > 0) {
          message.warning(`${failCount} 个附件上传失败`)
        }
      } else {
        message.success('稿件修改成功')
      }
      
      router.push('/front/submissions')
    } else {
      message.error(res.msg || '修改失败')
    }
  } catch (error) {
    console.error(error)
    if (error.errorFields) {
      // 表单验证错误
      return
    }
    message.error('修改失败，请检查网络连接')
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => {
  router.back()
}

onMounted(() => {
  loadSubmission()
})
</script>

<style scoped>
.submission-edit-container {
  padding: 20px;
}
</style>

