<template>
  <div class="project-edit-container">
    <a-card :loading="loading">
      <template #title>
        <h2>编辑项目</h2>
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
            value-format="YYYY-MM-DD"
            placeholder="选择截止时间"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="交付要求" name="deliveryRequirement">
          <a-textarea v-model:value="form.deliveryRequirement" :rows="4" placeholder="请输入交付要求" />
        </a-form-item>
        
        <a-alert
          v-if="hasOrders"
          message="温馨提示"
          description="该项目已有自由职业者接单，保存修改后系统将自动通知所有已接单的自由职业者。"
          type="info"
          show-icon
          style="margin-bottom: 20px"
        />
        
        <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
          <a-button type="primary" @click="handleSubmit" :loading="submitting">保存修改</a-button>
          <a-button style="margin-left: 8px" @click="handleCancel">取消</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 保证金补缴弹窗 -->
    <a-modal
      v-model:open="supplementModalVisible"
      title="保证金补缴"
      :width="520"
      @ok="handleConfirmSupplement"
      @cancel="supplementModalVisible = false"
      :confirm-loading="submitting"
      okText="确认支付"
      cancelText="取消"
    >
      <div style="padding: 20px 0">
        <a-alert
          message="预算增加，需补缴保证金"
          description="您修改后的预算增加了，需要补缴相应的保证金差额。"
          type="warning"
          show-icon
          style="margin-bottom: 20px"
        />
        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="原预算范围">
            {{ originalBudgetMin }} - {{ originalBudgetMax }} 元
          </a-descriptions-item>
          <a-descriptions-item label="新预算范围">
            {{ form.budgetMin }} - {{ form.budgetMax }} 元
          </a-descriptions-item>
          <a-descriptions-item label="原已缴保证金">
            ¥{{ (depositInfo.oldDeposit || 0).toFixed(2) }}
          </a-descriptions-item>
          <a-descriptions-item label="新预算中位数">
            {{ (depositInfo.newMedian || 0).toFixed(2) }} 元
          </a-descriptions-item>
          <a-descriptions-item label="需补缴保证金">
            <span style="color: #ff4d4f; font-size: 18px; font-weight: bold">
              ¥{{ (depositInfo.changeAmount || 0).toFixed(2) }}
            </span>
          </a-descriptions-item>
          <a-descriptions-item label="账户余额" v-if="accountBalance !== null">
            ¥{{ (accountBalance.balance || 0).toFixed(2) }}
            <a-tag v-if="(accountBalance.balance || 0) >= (depositInfo.changeAmount || 0)" color="green" style="margin-left: 8px">
              余额充足
            </a-tag>
            <a-tag v-else color="red" style="margin-left: 8px">
              余额不足
            </a-tag>
          </a-descriptions-item>
        </a-descriptions>
        <a-form-item label="支付方式" style="margin-top: 20px">
          <a-radio-group v-model:value="paymentMethod">
            <a-radio value="BALANCE" :disabled="accountBalance === null || (accountBalance.balance || 0) < (depositInfo.changeAmount || 0)">
              账户余额支付
            </a-radio>
            <a-radio value="ONLINE">在线支付（模拟）</a-radio>
          </a-radio-group>
        </a-form-item>
        <div style="margin-top: 20px; text-align: center; color: #999; font-size: 12px">
          <p v-if="paymentMethod === 'BALANCE'">将从您的账户余额中扣除保证金</p>
          <p v-else>点击确认后将进行模拟支付</p>
        </div>
      </div>
    </a-modal>

    <!-- 保证金退款确认弹窗 -->
    <a-modal
      v-model:open="refundModalVisible"
      title="保证金退款确认"
      :width="480"
      @ok="handleConfirmRefund"
      @cancel="refundModalVisible = false"
      :confirm-loading="submitting"
      okText="确认修改"
      cancelText="取消"
    >
      <div style="padding: 20px 0">
        <a-alert
          message="预算减少，保证金将退还"
          description="您修改后的预算减少了，多余的保证金将退还至您的账户余额。"
          type="success"
          show-icon
          style="margin-bottom: 20px"
        />
        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="原预算范围">
            {{ originalBudgetMin }} - {{ originalBudgetMax }} 元
          </a-descriptions-item>
          <a-descriptions-item label="新预算范围">
            {{ form.budgetMin }} - {{ form.budgetMax }} 元
          </a-descriptions-item>
          <a-descriptions-item label="原已缴保证金">
            ¥{{ (depositInfo.oldDeposit || 0).toFixed(2) }}
          </a-descriptions-item>
          <a-descriptions-item label="新保证金需求">
            ¥{{ (depositInfo.newRequiredDeposit || 0).toFixed(2) }}
          </a-descriptions-item>
          <a-descriptions-item label="退还保证金">
            <span style="color: #52c41a; font-size: 18px; font-weight: bold">
              ¥{{ (depositInfo.changeAmount || 0).toFixed(2) }}
            </span>
          </a-descriptions-item>
        </a-descriptions>
        <div style="margin-top: 20px; text-align: center; color: #52c41a; font-size: 14px">
          <p>确认修改后，退还的保证金将自动存入您的账户余额</p>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import request from '@/utils/request'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()

const formRef = ref()
const loading = ref(false)
const submitting = ref(false)
const projectId = ref(null)
const hasOrders = ref(false)

// 原始预算信息（用于比较）
const originalBudgetMin = ref(0)
const originalBudgetMax = ref(0)

// 弹窗控制
const supplementModalVisible = ref(false)
const refundModalVisible = ref(false)

// 保证金变化信息
const depositInfo = ref({})
const accountBalance = ref(null)
const paymentMethod = ref('ONLINE')

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

const loadProject = async () => {
  loading.value = true
  try {
    const res = await request.get(`/api/projects/${projectId.value}`)
    if (res.code === '200' && res.data) {
      const project = res.data
      form.title = project.title || ''
      form.description = project.description || ''
      form.skillsRequired = project.skillsRequired || ''
      form.budgetMin = project.budgetMin || null
      form.budgetMax = project.budgetMax || null
      form.deadline = project.deadline || null
      form.deliveryRequirement = project.deliveryRequirement || ''
      
      // 保存原始预算
      originalBudgetMin.value = project.budgetMin || 0
      originalBudgetMax.value = project.budgetMax || 0
      
      // 检查是否有接单人
      hasOrders.value = (project.orderCount || 0) > 0
    } else {
      message.error(res.msg || '加载项目信息失败')
      router.back()
    }
  } catch (error) {
    console.error('加载项目信息失败:', error)
    message.error('加载项目信息失败')
    router.back()
  } finally {
    loading.value = false
  }
}

// 加载账户余额
const loadAccountBalance = async () => {
  try {
    const res = await request.get('/api/payments/balance')
    if (res.code === '200') {
      accountBalance.value = res.data
      // 如果余额充足，默认选择账户余额支付
      if (accountBalance.value && depositInfo.value.changeAmount && 
          accountBalance.value.balance >= depositInfo.value.changeAmount) {
        paymentMethod.value = 'BALANCE'
      } else {
        paymentMethod.value = 'ONLINE'
      }
    }
  } catch (error) {
    console.error('加载账户余额失败:', error)
    accountBalance.value = null
  }
}

// 计算保证金变化
const calculateDepositChange = async () => {
  if (!form.budgetMin || !form.budgetMax) {
    return null
  }
  
  try {
    const res = await request.post(`/api/projects/${projectId.value}/calculate-deposit-change`, {
      budgetMin: form.budgetMin,
      budgetMax: form.budgetMax
    })
    if (res.code === '200') {
      return res.data
    }
    return null
  } catch (error) {
    console.error('计算保证金变化失败:', error)
    return null
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    // 计算保证金变化
    const changeInfo = await calculateDepositChange()
    
    if (changeInfo) {
      depositInfo.value = changeInfo
      
      if (changeInfo.changeType === 'SUPPLEMENT') {
        // 需要补缴，显示支付弹窗
        await loadAccountBalance()
        supplementModalVisible.value = true
        return
      } else if (changeInfo.changeType === 'REFUND') {
        // 需要退款，显示确认弹窗
        refundModalVisible.value = true
        return
      }
    }
    
    // 无需变化保证金，直接提交
    await doSubmit()
  } catch (error) {
    if (error.errorFields) {
      return
    }
    console.error('提交失败:', error)
  }
}

// 确认补缴保证金
const handleConfirmSupplement = async () => {
  await doSubmit(paymentMethod.value)
  supplementModalVisible.value = false
}

// 确认退款
const handleConfirmRefund = async () => {
  await doSubmit()
  refundModalVisible.value = false
}

// 执行提交
const doSubmit = async (payMethod = null) => {
  try {
    submitting.value = true
    
    const submitData = {
      title: form.title,
      description: form.description,
      skillsRequired: form.skillsRequired,
      budgetMin: form.budgetMin,
      budgetMax: form.budgetMax,
      deadline: form.deadline ? (typeof form.deadline === 'string' ? form.deadline : dayjs(form.deadline).format('YYYY-MM-DD')) : null,
      deliveryRequirement: form.deliveryRequirement,
      paymentMethod: payMethod
    }
    
    const res = await request.put(`/api/projects/${projectId.value}/with-payment`, submitData)
    if (res.code === '200') {
      const result = res.data
      
      // 显示保证金变化提示
      if (result.depositAction === 'SUPPLEMENT') {
        message.success(result.message || '项目修改成功，保证金已补缴')
      } else if (result.depositAction === 'REFUND') {
        message.success(result.message || '项目修改成功，保证金已退还至账户余额')
      } else {
        message.success('项目修改成功')
      }
      
      router.push('/front/enterprise/dashboard')
    } else {
      message.error(res.msg || '修改失败')
    }
  } catch (error) {
    console.error('修改失败:', error)
    if (error.response?.data?.msg) {
      message.error(error.response.data.msg)
    } else {
      message.error('修改失败，请重试')
    }
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => {
  router.back()
}

onMounted(() => {
  projectId.value = route.params.id
  if (projectId.value) {
    loadProject()
  } else {
    message.error('项目ID不存在')
    router.back()
  }
})
</script>

<style scoped>
.project-edit-container {
  padding: 24px;
  background: var(--bg-secondary);
  min-height: calc(100vh - 140px);
}

.project-edit-container :deep(.ant-card) {
  border-radius: 12px;
  box-shadow: var(--shadow-sm);
}

.project-edit-container :deep(.ant-card-head) {
  border-bottom: 1px solid var(--border-light);
}

.project-edit-container :deep(.ant-card-head-title h2) {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

/* 弹窗样式优化 */
:deep(.ant-descriptions-item-label) {
  width: 120px;
  background: #fafafa;
}

:deep(.ant-descriptions-item-content) {
  font-weight: 500;
}

:deep(.ant-alert) {
  border-radius: 8px;
}
</style>
