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
  deliveryRequirement: ''
})

const rules = {
  title: [{ required: true, message: '请输入项目标题', trigger: 'blur' }],
  description: [{ required: true, message: '请输入项目描述', trigger: 'blur' }],
  skillsRequired: [{ required: true, message: '请输入所需技能', trigger: 'blur' }],
  deadline: [{ required: true, message: '请选择截止时间', trigger: 'change' }]
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
}
</script>

<style scoped>
.project-publish-container {
  padding: 20px;
}
</style>

