<template>
  <div class="payment-confirm-container">
    <a-card>
      <template #title>
        <h2>确认合作并支付保证金</h2>
      </template>

      <a-alert
        v-if="paymentInfo"
        message="确认合作需要支付接单保证金"
        :description="paymentInfo.description"
        type="info"
        show-icon
        style="margin-bottom: 20px"
      />

      <a-descriptions :column="1" bordered v-if="paymentInfo">
        <a-descriptions-item label="项目标题">
          {{ paymentInfo.projectTitle }}
        </a-descriptions-item>
        <a-descriptions-item label="报价金额">
          ¥{{ paymentInfo.quotePrice }}
        </a-descriptions-item>
        <a-descriptions-item label="需支付保证金">
          <span style="color: #ff4d4f; font-size: 18px; font-weight: bold">
            ¥{{ paymentInfo.depositAmount }}
          </span>
        </a-descriptions-item>
        <a-descriptions-item label="账户余额" v-if="accountBalance !== null">
          ¥{{ (accountBalance.balance || 0).toFixed(2) }}
          <a-tag v-if="(accountBalance.balance || 0) >= parseFloat(paymentInfo.depositAmount)" color="green" style="margin-left: 8px">
            余额充足
          </a-tag>
          <a-tag v-else color="red" style="margin-left: 8px">
            余额不足
          </a-tag>
        </a-descriptions-item>
      </a-descriptions>
      <a-form-item label="支付方式" style="margin-top: 20px" v-if="paymentInfo">
        <a-radio-group v-model:value="paymentMethod">
          <a-radio value="BALANCE" :disabled="accountBalance === null || (accountBalance.balance || 0) < parseFloat(paymentInfo.depositAmount)">
            账户余额支付
          </a-radio>
          <a-radio value="ONLINE">在线支付（模拟）</a-radio>
        </a-radio-group>
      </a-form-item>
      <div style="margin-top: 20px; text-align: center; color: #999; font-size: 12px" v-if="paymentInfo">
        <p v-if="paymentMethod === 'BALANCE'">将从您的账户余额中扣除保证金</p>
        <p v-else>点击确认后将进行模拟支付，支付成功后确认合作</p>
      </div>

      <div style="margin-top: 30px; text-align: center">
        <a-button type="primary" size="large" @click="handlePay" :loading="paying">
          确认支付
        </a-button>
        <a-button style="margin-left: 12px" @click="handleCancel">取消</a-button>
      </div>
    </a-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const paying = ref(false)
const paymentInfo = ref(null)
const paymentMethod = ref('ONLINE') // 默认在线支付
const accountBalance = ref(null)

const loadAccountBalance = async () => {
  try {
    const res = await request.get('/api/payments/balance')
    if (res.code === '200') {
      accountBalance.value = res.data
    }
  } catch (error) {
    console.error('加载账户余额失败:', error)
    accountBalance.value = null
  }
}

const loadPaymentInfo = async () => {
  try {
    const submissionId = route.params.submissionId
    const res = await request.get(`/api/submissions/${submissionId}`)
    if (res.code === '200') {
      const submission = res.data
      const depositAmount = ((submission.quotePrice || 0) * 0.05).toFixed(2)
      
      paymentInfo.value = {
        projectTitle: submission.projectTitle,
        quotePrice: submission.quotePrice || 0,
        depositAmount: depositAmount,
        description: `接单保证金为报价的5%，即 ¥${depositAmount}。`
      }
      
      // 加载账户余额
      await loadAccountBalance()
      // 根据余额设置默认支付方式
      if (accountBalance.value && (accountBalance.value.balance || 0) >= parseFloat(depositAmount)) {
        paymentMethod.value = 'BALANCE'
      } else {
        paymentMethod.value = 'ONLINE'
      }
    } else {
      message.error(res.msg || '加载支付信息失败')
    }
  } catch (error) {
    console.error('加载支付信息失败:', error)
    message.error('加载支付信息失败')
  }
}

const handlePay = async () => {
  paying.value = true
  try {
    const submissionId = route.params.submissionId
    const res = await request.put(`/api/submissions/${submissionId}/freelancer-confirm`, null, {
      params: {
        paymentMethod: paymentMethod.value
      }
    })
    if (res.code === '200') {
      const paymentText = paymentMethod.value === 'BALANCE' ? '已从账户余额扣除' : '已支付'
      message.success(`${paymentText}保证金，已确认合作`)
      router.push(`/front/submissions/${submissionId}`)
    } else {
      message.error(res.msg || '支付失败')
    }
  } catch (error) {
    console.error('支付失败:', error)
    if (error.response?.data?.msg) {
      message.error(error.response.data.msg)
    } else {
      message.error('支付失败，请检查网络连接')
    }
  } finally {
    paying.value = false
  }
}

const handleCancel = () => {
  router.back()
}

onMounted(() => {
  loadPaymentInfo()
})
</script>

<style scoped>
.payment-confirm-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}
</style>

