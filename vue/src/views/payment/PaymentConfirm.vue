<template>
  <div class="payment-confirm-container">
    <a-card>
      <template #title>
        <h2>确认合作支付</h2>
      </template>

      <a-alert
        v-if="paymentInfo"
        :message="paymentInfo.message"
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
        <a-descriptions-item label="已支付金额">
          ¥{{ paymentInfo.paidAmount || 0 }}
        </a-descriptions-item>
        <a-descriptions-item label="需支付金额">
          <span style="color: #ff4d4f; font-size: 18px; font-weight: bold">
            ¥{{ paymentInfo.needPay }}
          </span>
        </a-descriptions-item>
        <a-descriptions-item label="支付方式">
          模拟支付
        </a-descriptions-item>
      </a-descriptions>

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

const loadPaymentInfo = async () => {
  try {
    const submissionId = route.params.submissionId
    const res = await request.get(`/api/submissions/${submissionId}`)
    if (res.code === '200') {
      const submission = res.data
      const projectRes = await request.get(`/api/projects/${submission.projectId}`)
      if (projectRes.code === '200') {
        const project = projectRes.data
        const paidAmount = project.paidAmount || 0
        const needPay = (submission.quotePrice || 0) - paidAmount
        
        paymentInfo.value = {
          projectTitle: submission.projectTitle || project.title,
          quotePrice: submission.quotePrice || 0,
          paidAmount: paidAmount,
          needPay: needPay > 0 ? needPay : 0,
          message: '确认合作需要支付剩余尾款',
          description: `报价金额为 ¥${submission.quotePrice}，已支付 ¥${paidAmount}，需补足 ¥${needPay > 0 ? needPay : 0}`
        }
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
    const res = await request.put(`/api/submissions/${submissionId}/enterprise-confirm`)
    if (res.code === '200') {
      message.success('支付成功，已确认合作')
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

