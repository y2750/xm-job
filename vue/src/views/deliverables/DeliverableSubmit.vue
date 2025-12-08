<template>
  <div class="deliverable-submit-container">
    <a-card>
      <template #title>
        <h2>提交成品</h2>
        <div v-if="existingDeliverable" style="margin-top: 10px">
          <a-alert
            v-if="existingDeliverable.status === 'SUBMITTED'"
            message="企业尚未验收，请等待企业验收后再提交"
            type="warning"
            show-icon
          />
          <a-alert
            v-else
            :message="`当前为第 ${existingDeliverable.submitCount || 1} 次提交，剩余 ${remainingSubmits} 次提交机会`"
            type="info"
            show-icon
          />
        </div>
        <div v-else style="margin-top: 10px">
          <a-alert
            message="首次提交，共有 3 次提交机会"
            type="info"
            show-icon
          />
        </div>
      </template>

      <a-form :model="form" :rules="rules" ref="formRef" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="成品标题" name="title">
          <a-input v-model:value="form.title" placeholder="请输入成品标题" />
        </a-form-item>
        <a-form-item label="成品描述" name="description">
          <a-textarea v-model:value="form.description" :rows="8" placeholder="请详细描述成品内容、功能特点等" />
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
        </a-form-item>
        <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
          <a-button 
            type="primary" 
            @click="handleSubmit" 
            :loading="submitting"
            :disabled="existingDeliverable && existingDeliverable.status === 'SUBMITTED'"
          >
            提交成品
          </a-button>
          <a-button style="margin-left: 8px" @click="handleCancel">取消</a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UploadOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const formRef = ref()
const submitting = ref(false)
const fileList = ref([])
const submissionId = ref(null)
const projectId = ref(null)
const existingDeliverable = ref(null)
const remainingSubmits = ref(3)

const form = reactive({
  title: '',
  description: ''
})

const rules = {
  title: [{ required: true, message: '请输入成品标题', trigger: 'blur' }],
  description: [{ required: true, message: '请输入成品描述', trigger: 'blur' }]
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
  // 先不上传，等提交后再上传
  fileList.value.push(file)
  onSuccess()
}

const loadSubmission = async () => {
  try {
    const id = route.params.submissionId
    if (!id) {
      message.error('稿件ID不存在')
      router.back()
      return
    }
    
    submissionId.value = id
    
    const res = await request.get(`/api/submissions/${id}`)
    if (res.code === '200') {
      const submission = res.data
      projectId.value = submission.projectId
      
      // 加载现有成品信息
      try {
        const deliverableRes = await request.get('/api/deliverables', {
          params: {
            submissionId: id
          }
        })
        if (deliverableRes.code === '200' && deliverableRes.data) {
          const deliverableList = Array.isArray(deliverableRes.data) ? deliverableRes.data : []
          if (deliverableList.length > 0) {
            existingDeliverable.value = deliverableList[0]
            const currentCount = existingDeliverable.value.submitCount || 1
            remainingSubmits.value = Math.max(0, 3 - currentCount)
            
            // 如果已有成品且状态不是 APPROVED，填充表单
            if (existingDeliverable.value.status !== 'APPROVED') {
              if (existingDeliverable.value.title) {
                form.title = existingDeliverable.value.title
              }
              if (existingDeliverable.value.description) {
                form.description = existingDeliverable.value.description
              }
            }
          } else {
            remainingSubmits.value = 3
          }
        } else {
          remainingSubmits.value = 3
        }
      } catch (error) {
        console.error('加载成品信息失败:', error)
        remainingSubmits.value = 3
      }
    } else {
      message.error(res.msg || '加载稿件信息失败')
      router.back()
    }
  } catch (error) {
    console.error('加载稿件信息失败:', error)
    message.error('加载稿件信息失败，请检查网络连接')
    router.back()
  }
}

const handleSubmit = async () => {
  try {
    // 检查是否已有待验收的成品
    if (existingDeliverable && existingDeliverable.status === 'SUBMITTED') {
      message.warning('企业尚未验收，请等待企业验收后再提交')
      return
    }
    
    await formRef.value.validate()
    
    submitting.value = true
    
    // 先提交成品信息
    const submitData = {
      submissionId: submissionId.value,
      projectId: projectId.value,
      title: form.title,
      description: form.description
    }
    
    const res = await request.post('/api/deliverables', submitData)
    if (res.code === '200') {
      // 获取成品ID（后端应该返回deliverable对象）
      const deliverable = res.data
      const deliverableId = deliverable?.id
      
      if (!deliverableId) {
        message.error('获取成品ID失败')
        return
      }
      
      // 更新剩余提交次数
      if (deliverable.submitCount) {
        remainingSubmits.value = Math.max(0, 3 - deliverable.submitCount)
      }
      
      // 如果有附件，上传附件
      if (fileList.value.length > 0) {
        let successCount = 0
        let failCount = 0
        
        for (const file of fileList.value) {
          try {
            const formData = new FormData()
            const fileObj = file.originFileObj || file
            formData.append('file', fileObj)
            
            // 使用成品附件上传接口
            const uploadRes = await request.post(`/api/attachments/deliverable/${deliverableId}`, formData)
            
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
          message.success(`成品提交成功，${successCount} 个附件上传成功`)
        }
        if (failCount > 0) {
          message.warning(`${failCount} 个附件上传失败`)
        }
      } else {
        message.success('成品提交成功')
      }
      
      router.push('/front/submissions')
    } else {
      message.error(res.msg || '提交失败')
    }
  } catch (error) {
    console.error(error)
    if (error.errorFields) {
      // 表单验证错误
      return
    }
    message.error('提交失败，请检查网络连接')
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
.deliverable-submit-container {
  padding: 20px;
}
</style>

