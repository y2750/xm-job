<template>
  <div class="submission-submit-container">
    <a-card>
      <template #title>
        <h2>提交稿件</h2>
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
        </a-form-item>
        <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
          <a-button type="primary" @click="handleSubmit" :loading="submitting">提交稿件</a-button>
          <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
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
const projectId = ref(null)

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
  // 先不上传，等提交稿件后再上传
  // 这里只是将文件添加到列表中
  fileList.value.push(file)
  onSuccess()
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    submitting.value = true
    
    // 直接提交稿件（不需要支付保证金）
    const submitData = {
      projectId: projectId.value,
      title: form.title,
      description: form.description,
      quotePrice: form.quotePrice || null // 报价可选
    }
    
    const res = await request.post('/api/submissions', submitData)
    if (res.code === '200') {
      const submissionId = res.data?.id
      
      if (!submissionId) {
        message.error('获取稿件ID失败')
        submitting.value = false
        return
      }
      
      message.success('稿件提交成功')
      
      // 如果有附件，上传附件
      if (fileList.value.length > 0) {
        let successCount = 0
        let failCount = 0
        
        for (const file of fileList.value) {
          try {
            const formData = new FormData()
            // Ant Design Vue 的 Upload 组件，文件对象在 file.originFileObj 中
            const fileObj = file.originFileObj || file
            formData.append('file', fileObj)
            
            // 使用 FormData 时，不需要手动设置 Content-Type，浏览器会自动设置（包括 boundary）
            const uploadRes = await request.post(`/api/attachments/submission/${submissionId}`, formData)
            
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
          if (successCount === fileList.value.length) {
            // 所有附件都上传成功
          } else {
            message.info(`${successCount} 个附件上传成功，${failCount} 个附件上传失败`)
          }
        }
        if (failCount > 0 && successCount === 0) {
          message.warning(`${failCount} 个附件上传失败`)
        }
      }
      
      router.push('/front/submissions')
    } else {
      message.error(res.msg || '提交稿件失败')
    }
  } catch (error) {
    console.error(error)
    if (error.response?.data?.msg) {
      message.error(error.response.data.msg)
    } else {
      message.error('操作失败，请检查网络连接')
    }
  } finally {
    submitting.value = false
  }
}

const handleReset = () => {
  formRef.value.resetFields()
  fileList.value = []
}

const handleCancel = () => {
  router.back()
}

onMounted(() => {
  projectId.value = route.params.projectId
  if (!projectId.value) {
    message.error('项目ID不能为空')
    router.back()
  }
})
</script>

<style scoped>
.submission-submit-container {
  padding: 20px;
}
</style>

