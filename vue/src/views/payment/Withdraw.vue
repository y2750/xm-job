<template>
  <div class="withdraw-container">
    <a-card>
      <template #title>
        <h2>提现</h2>
      </template>
      
      <a-form :model="form" :rules="rules" ref="formRef" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="账户余额">
          <span style="font-size: 18px; color: #3f8600; font-weight: bold">
            ¥{{ balance ? balance.balance.toFixed(2) : '0.00' }}
          </span>
        </a-form-item>
        <a-form-item label="提现金额" name="amount">
          <a-input-number
            v-model:value="form.amount"
            :min="0.01"
            :max="balance ? balance.balance : 0"
            :precision="2"
            placeholder="请输入提现金额"
            style="width: 100%"
          />
          <div style="margin-top: 8px; color: #999; font-size: 12px">
            手续费：3%，实际到账：{{ actualAmount.toFixed(2) }} 元
          </div>
        </a-form-item>
        <a-form-item label="银行名称" name="bankName">
          <a-input v-model:value="form.bankName" placeholder="请输入银行名称" />
        </a-form-item>
        <a-form-item label="银行账户" name="bankAccount">
          <a-input v-model:value="form.bankAccount" placeholder="请输入银行账户" />
        </a-form-item>
        <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
          <a-button type="primary" @click="handleSubmit" :loading="submitting">确认提现</a-button>
          <a-button style="margin-left: 8px" @click="handleBack">返回</a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import request from '@/utils/request'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)
const balance = ref(null)

const form = reactive({
  amount: null,
  bankName: '',
  bankAccount: ''
})

const rules = {
  amount: [
    { required: true, message: '请输入提现金额', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '提现金额必须大于0', trigger: 'blur' }
  ],
  bankName: [{ required: true, message: '请输入银行名称', trigger: 'blur' }],
  bankAccount: [{ required: true, message: '请输入银行账户', trigger: 'blur' }]
}

const actualAmount = computed(() => {
  if (form.amount && form.amount > 0) {
    return form.amount * 0.97
  }
  return 0
})

const loadBalance = async () => {
  try {
    const res = await request.get('/api/payments/balance')
    if (res.code === '200') {
      balance.value = res.data
    }
  } catch (error) {
    console.error('加载余额失败:', error)
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    if (!balance.value || form.amount > balance.value.balance) {
      message.error('提现金额不能超过账户余额')
      return
    }
    
    submitting.value = true
    const res = await request.post('/api/payments/withdraw', null, {
      params: {
        amount: form.amount,
        bankAccount: form.bankAccount,
        bankName: form.bankName
      }
    })
    
    if (res.code === '200') {
      message.success('提现成功')
      router.push('/front/balance')
    } else {
      message.error(res.msg || '提现失败')
    }
  } catch (error) {
    console.error('提现失败:', error)
    if (error.response?.data?.msg) {
      message.error(error.response.data.msg)
    } else {
      message.error('提现失败，请检查网络连接')
    }
  } finally {
    submitting.value = false
  }
}

const handleBack = () => {
  router.back()
}

onMounted(() => {
  loadBalance()
})
</script>

<style scoped>
.withdraw-container {
  padding: 20px;
}
</style>

