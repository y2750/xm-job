<template>
  <div class="project-publish-container">
    <a-card>
      <template #title>
        <h2>发布外包项目</h2>
      </template>

      <a-form :model="form" :rules="rules" ref="formRef" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="项目标题" name="title">
          <a-input v-model:value="form.title" placeholder="请输入项目标题" />
        </a-form-item>
        <a-form-item label="项目描述" name="description">
          <a-textarea v-model:value="form.description" :rows="6" placeholder="请输入项目描述" />
        </a-form-item>
        <a-form-item label="所需技能" name="skillsRequired">
          <a-input v-model:value="form.skillsRequired" placeholder="请输入技能标签，用逗号（，或,）分隔" />
        </a-form-item>
        <a-form-item label="预算范围">
          <a-input-group compact>
            <a-input-number v-model:value="form.budgetMin" :min="0" placeholder="最低预算" style="width: 50%" />
            <a-input-number v-model:value="form.budgetMax" :min="0" placeholder="最高预算" style="width: 50%" />
          </a-input-group>
        </a-form-item>
        <a-form-item label="截止时间" name="deadline">
          <a-date-picker
            v-model:value="form.deadline"
            format="YYYY-MM-DD"
            placeholder="选择截止时间"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="项目类型" name="projectType">
          <a-select v-model:value="form.projectType" placeholder="请选择项目类型">
            <a-select-option value="WEB">网站开发</a-select-option>
            <a-select-option value="MOBILE">移动应用</a-select-option>
            <a-select-option value="DESIGN">设计</a-select-option>
            <a-select-option value="OTHER">其他</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="难度等级" name="difficultyLevel">
          <a-select v-model:value="form.difficultyLevel" placeholder="请选择难度等级">
            <a-select-option value="EASY">简单</a-select-option>
            <a-select-option value="MEDIUM">中等</a-select-option>
            <a-select-option value="HARD">困难</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="偏向经验" name="preferredExperience">
          <a-select v-model:value="form.preferredExperience" placeholder="请选择偏向经验">
            <a-select-option value="NEWBIE">新手（价格相对较低）</a-select-option>
            <a-select-option value="EXPERIENCED">老手（完成效率更高）</a-select-option>
            <a-select-option value="BOTH">不限</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="封面图片">
          <a-upload
            v-model:file-list="coverImageList"
            :before-upload="beforeUploadCover"
            :custom-request="handleCoverUpload"
            list-type="picture-card"
            :max-count="1"
            accept="image/*"
          >
            <div v-if="coverImageList.length < 1">
              <plus-outlined />
              <div style="margin-top: 8px">上传封面</div>
            </div>
          </a-upload>
        </a-form-item>
        <a-form-item label="项目附件">
          <a-upload
            v-model:file-list="attachmentList"
            :before-upload="beforeUploadAttachment"
            :custom-request="handleAttachmentUpload"
            :max-count="10"
          >
            <a-button>
              <upload-outlined /> 上传附件
            </a-button>
          </a-upload>
        </a-form-item>
        <a-form-item label="详细需求说明">
          <a-textarea v-model:value="form.requirementDetails" :rows="6" placeholder="请输入详细需求说明（支持富文本）" />
        </a-form-item>
        <a-form-item label="交付要求" name="deliveryRequirement">
          <a-textarea v-model:value="form.deliveryRequirement" :rows="4" placeholder="请输入交付要求" />
        </a-form-item>
        <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
          <a-button type="primary" @click="handleSubmit" :loading="submitting">发布项目</a-button>
          <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 支付确认弹窗 -->
    <a-modal
      v-model:open="paymentModalVisible"
      title="支付保证金"
      :width="500"
      @ok="handleConfirmPayment"
      @cancel="paymentModalVisible = false"
      :confirm-loading="submitting"
    >
      <div style="padding: 20px 0">
        <a-alert
          message="发布项目需要支付保证金"
          description="保证金为项目预算中位数的50%，项目发布成功后将从您的账户扣除。"
          type="info"
          show-icon
          style="margin-bottom: 20px"
        />
        <a-descriptions :column="1" bordered>
          <a-descriptions-item label="预算范围">
            {{ form.budgetMin }} - {{ form.budgetMax }} 元
          </a-descriptions-item>
          <a-descriptions-item label="预算中位数">
            {{ ((form.budgetMin + form.budgetMax) / 2).toFixed(2) }} 元
          </a-descriptions-item>
          <a-descriptions-item label="需支付保证金">
            <span style="color: #ff4d4f; font-size: 18px; font-weight: bold">
              ¥{{ depositAmount }}
            </span>
          </a-descriptions-item>
          <a-descriptions-item label="账户余额" v-if="accountBalance !== null">
            ¥{{ (accountBalance.balance || 0).toFixed(2) }}
            <a-tag v-if="(accountBalance.balance || 0) >= parseFloat(depositAmount)" color="green" style="margin-left: 8px">
              余额充足
            </a-tag>
            <a-tag v-else color="red" style="margin-left: 8px">
              余额不足
            </a-tag>
          </a-descriptions-item>
        </a-descriptions>
        <a-form-item label="支付方式" style="margin-top: 20px">
          <a-radio-group v-model:value="paymentMethod">
            <a-radio value="BALANCE" :disabled="accountBalance === null || (accountBalance.balance || 0) < parseFloat(depositAmount)">
              账户余额支付
            </a-radio>
            <a-radio value="ONLINE">在线支付（模拟）</a-radio>
          </a-radio-group>
        </a-form-item>
        <div style="margin-top: 20px; text-align: center; color: #999; font-size: 12px">
          <p v-if="paymentMethod === 'BALANCE'">将从您的账户余额中扣除保证金</p>
          <p v-else>点击确认后将进行模拟支付，支付成功后项目将自动发布</p>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined, UploadOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'
import dayjs from 'dayjs'

const router = useRouter()

const formRef = ref()
const submitting = ref(false)

const form = reactive({
  title: '',
  description: '',
  skillsRequired: '',
  budgetMin: null,
  budgetMax: null,
  deadline: null,
  deliveryRequirement: '',
  projectType: 'OTHER',
  difficultyLevel: 'MEDIUM',
  preferredExperience: 'BOTH',
  coverImage: '',
  requirementDetails: ''
})

const coverImageList = ref([])
const attachmentList = ref([])
const uploadedAttachments = ref([])

// 计算文本长度（不包含空格和符号）
const getTextLength = (text) => {
  if (!text) return 0
  // 移除空格、标点符号等
  return text.replace(/[\s\p{P}]/gu, '').length
}

const rules = {
  title: [{ required: true, message: '请输入项目标题', trigger: 'blur' }],
  description: [
    { required: true, message: '请输入项目描述', trigger: 'blur' },
    { 
      validator: (rule, value) => {
        if (!value || getTextLength(value) < 20) {
          return Promise.reject('项目描述至少需要20个字（不包含空格和符号）')
        }
        return Promise.resolve()
      },
      trigger: 'blur'
    }
  ],
  skillsRequired: [{ required: true, message: '请输入所需技能', trigger: 'blur' }],
  deadline: [{ required: true, message: '请选择截止时间', trigger: 'change' }],
  requirementDetails: [
    {
      validator: (rule, value) => {
        if (value && getTextLength(value) < 50) {
          return Promise.reject('详细需求说明至少需要50个字（不包含空格和符号）')
        }
        return Promise.resolve()
      },
      trigger: 'blur'
    }
  ]
}

// 计算保证金
const calculateDeposit = () => {
  if (form.budgetMin && form.budgetMax) {
    const median = (form.budgetMin + form.budgetMax) / 2
    return (median * 0.5).toFixed(2)
  }
  return 0
}

const paymentModalVisible = ref(false)
const depositAmount = ref(0)
const paymentMethod = ref('ONLINE') // 默认在线支付
const accountBalance = ref(null)

const loadAccountBalance = async () => {
  try {
    const res = await request.get('/api/payments/balance')
    if (res.code === '200') {
      accountBalance.value = res.data
      // 如果余额充足，默认选择账户余额支付
      if (accountBalance.value && accountBalance.value.balance >= depositAmount.value) {
        paymentMethod.value = 'BALANCE'
      }
    }
  } catch (error) {
    console.error('加载账户余额失败:', error)
    accountBalance.value = null
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    // 计算保证金
    const deposit = calculateDeposit()
    if (deposit <= 0) {
      message.error('请设置有效的预算范围')
      return
    }
    
    depositAmount.value = deposit
    // 加载账户余额
    await loadAccountBalance()
    // 根据余额设置默认支付方式
    if (accountBalance.value && (accountBalance.value.balance || 0) >= parseFloat(depositAmount.value)) {
      paymentMethod.value = 'BALANCE'
    } else {
      paymentMethod.value = 'ONLINE'
    }
    paymentModalVisible.value = true
  } catch (error) {
    console.error(error)
  }
}

const handleConfirmPayment = async () => {
  try {
    submitting.value = true
    const submitData = {
      ...form,
      deadline: form.deadline ? dayjs(form.deadline).format('YYYY-MM-DD') : null,
      paymentMethod: paymentMethod.value // 传递支付方式
    }
    
    const res = await request.post('/api/projects', submitData)
    if (res.code === '200') {
      const projectId = res.data?.id || res.data
      
      // 上传附件
      if (uploadedAttachments.value.length > 0) {
        for (const attachment of uploadedAttachments.value) {
          try {
            await request.post(`/api/projects/${projectId}/attachments`, attachment)
          } catch (error) {
            console.error('上传附件失败:', error)
          }
        }
      }
      
      const paymentText = paymentMethod.value === 'BALANCE' ? '已从账户余额扣除' : '已支付'
      message.success(`项目发布成功，${paymentText}保证金`)
      paymentModalVisible.value = false
      // 企业用户跳转到企业工作台，自由职业者跳转到项目列表
      const user = JSON.parse(localStorage.getItem('xm-user') || '{}')
      if (user.role === 'EMPLOY') {
        router.push('/front/enterprise/dashboard')
      } else {
        router.push('/front/projects')
      }
    } else {
      message.error(res.msg)
    }
  } catch (error) {
    console.error(error)
    if (error.response?.data?.msg) {
      message.error(error.response.data.msg)
    } else {
      message.error('发布失败，请重试')
    }
  } finally {
    submitting.value = false
  }
}

const handleReset = () => {
  formRef.value.resetFields()
  coverImageList.value = []
  attachmentList.value = []
  uploadedAttachments.value = []
}

// 封面图上传
const beforeUploadCover = (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    message.error('只能上传图片文件！')
    return false
  }
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    message.error('图片大小不能超过10MB！')
    return false
  }
  return false // 阻止自动上传，使用custom-request
}

const handleCoverUpload = async ({ file, onSuccess, onError }) => {
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await request.post('/api/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.code === '200') {
      form.coverImage = res.data
      onSuccess()
      message.success('封面图上传成功')
    } else {
      onError()
      message.error(res.msg || '上传失败')
    }
  } catch (error) {
    onError()
    message.error('上传失败，请重试')
  }
}

// 附件上传
const beforeUploadAttachment = (file) => {
  const isLt50M = file.size / 1024 / 1024 < 50
  if (!isLt50M) {
    message.error('文件大小不能超过50MB！')
    return false
  }
  return false // 阻止自动上传，使用custom-request
}

const handleAttachmentUpload = async ({ file, onSuccess, onError }) => {
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await request.post('/api/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.code === '200') {
      const attachment = {
        fileUrl: res.data,
        fileName: file.name,
        fileType: file.type.startsWith('image/') ? 'IMAGE' : 'DOCUMENT',
        fileSize: file.size
      }
      uploadedAttachments.value.push(attachment)
      onSuccess()
      message.success('附件上传成功')
    } else {
      onError()
      message.error(res.msg || '上传失败')
    }
  } catch (error) {
    onError()
    message.error('上传失败，请重试')
  }
}
</script>

<style scoped>
.project-publish-container {
  padding: 20px;
}
</style>

