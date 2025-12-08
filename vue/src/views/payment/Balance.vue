<template>
  <div class="balance-container">
    <a-card>
      <template #title>
        <h2>我的余额</h2>
      </template>
      
      <div v-if="balance" style="padding: 20px 0">
        <a-row :gutter="24">
          <a-col :span="8">
            <a-statistic
              title="账户余额"
              :value="balance.balance"
              :precision="2"
              prefix="¥"
              :value-style="{ color: '#3f8600' }"
            />
          </a-col>
          <a-col :span="8">
            <a-statistic
              title="冻结余额"
              :value="balance.frozenBalance"
              :precision="2"
              prefix="¥"
              :value-style="{ color: '#cf1322' }"
            />
          </a-col>
          <a-col :span="8">
            <a-statistic
              title="可用余额"
              :value="balance.balance"
              :precision="2"
              prefix="¥"
              :value-style="{ color: '#1890ff' }"
            />
          </a-col>
        </a-row>
        
        <a-divider />
        
        <div style="text-align: center; margin-top: 30px">
          <a-button type="primary" size="large" @click="handleWithdraw" :disabled="balance.balance <= 0">
            提现
          </a-button>
        </div>
      </div>
      
      <a-spin v-else :spinning="loading" />
    </a-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import request from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const balance = ref(null)

const loadBalance = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/payments/balance')
    if (res.code === '200') {
      balance.value = res.data
    } else {
      message.error(res.msg || '加载余额失败')
    }
  } catch (error) {
    console.error('加载余额失败:', error)
    message.error('加载余额失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}

const handleWithdraw = () => {
  router.push('/front/withdraw')
}

onMounted(() => {
  loadBalance()
})
</script>

<style scoped>
.balance-container {
  padding: 20px;
}
</style>

