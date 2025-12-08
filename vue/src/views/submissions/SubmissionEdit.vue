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
    const id = route.params.id
    if (!id) {
      message.error('稿件ID不存在')
      router.back()
      return
    }
    
    const res = await request.get(`/api/submissions/${id}`)
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
    const res = await request.get(`/api/attachments/download/${attachmentId}`, {
      responseType: 'blob'
    })
    if (res instanceof Blob) {
      const blobUrl = window.URL.createObjectURL(res)
      const link = document.createElement('a')
      link.href = blobUrl
      const attachment = existingAttachments.value.find(a => a.id === attachmentId)
      link.download = attachment ? attachment.fileName : 'download'
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

