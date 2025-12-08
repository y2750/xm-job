<template>
  <div class="my-orders-container">
    <a-card>
      <template #title>
        <h2>我的接单</h2>
      </template>

      <!-- 筛选条件 -->
      <a-form :model="filterForm" layout="inline" style="margin-bottom: 20px">
        <a-form-item label="项目标题">
          <a-input
            v-model:value="filterForm.projectTitle"
            placeholder="输入项目标题"
            allow-clear
            style="width: 200px"
            @pressEnter="handleFilter"
          />
        </a-form-item>
        <a-form-item label="状态">
          <a-select
            v-model:value="filterForm.status"
            placeholder="全部状态"
            allow-clear
            style="width: 150px"
            @change="handleFilter"
          >
            <a-select-option value="ACCEPTED">已接单</a-select-option>
            <a-select-option value="COMPLETED">已完成</a-select-option>
            <a-select-option value="CANCELLED">已取消</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleFilter">筛选</a-button>
          <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
        </a-form-item>
      </a-form>

      <!-- 接单列表 -->
      <a-table
        :columns="columns"
        :data-source="orderList"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status, record.projectStatus)">{{ getStatusText(record.status, record.projectStatus) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'acceptedAt'">
            {{ formatDate(record.acceptedAt) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" @click="handleViewProject(record.projectId)">查看项目</a-button>
              <a-button
                v-if="record.status === 'ACCEPTED' && !record.hasSubmission && record.projectStatus !== 'CONFIRMED'"
                type="link"
                @click="handleSubmitSubmission(record.projectId)"
              >
                提交稿件
              </a-button>
              <a-button
                v-if="record.status === 'ACCEPTED' && record.hasSubmission && record.projectStatus !== 'CONFIRMED'"
                type="link"
                @click="handleViewSubmission(record.projectId)"
              >
                查看稿件
              </a-button>
              <a-button
                v-if="record.status === 'ACCEPTED' && record.projectStatus === 'CONFIRMED'"
                type="link"
                @click="handleSubmitDeliverable(record.projectId)"
              >
                提交成品
              </a-button>
              <a-button
                v-if="record.status === 'ACCEPTED' && !record.abandoned && record.projectStatus !== 'CONFIRMED'"
                type="link"
                danger
                @click="handleCancel(record.id)"
              >
                取消接单
              </a-button>
              <a-button
                v-if="record.status === 'ACCEPTED' && !record.abandoned && record.projectStatus === 'CONFIRMED'"
                type="link"
                danger
                @click="handleCancelWithDeposit(record.id)"
              >
                取消接单
              </a-button>
              <a-button
                v-if="record.status === 'ACCEPTED' && record.abandoned"
                type="link"
                disabled
              >
                已放弃
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import request from '@/utils/request'

const router = useRouter()

const loading = ref(false)
const orderList = ref([])
const mySubmissions = ref([]) // 我的稿件列表

const filterForm = reactive({
  projectTitle: '',
  status: undefined
})

const columns = [
  { title: '项目标题', dataIndex: 'projectTitle', key: 'projectTitle' },
  { title: '状态', key: 'status', width: 120 },
  { title: '接单时间', key: 'acceptedAt', width: 180 },
  { title: '操作', key: 'action', width: 250 }
]

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total) => `共 ${total} 条`
})

const getStatusColor = (status, projectStatus) => {
  // 如果项目已完成，显示紫色
  if (projectStatus === 'COMPLETED') {
    return 'purple'
  }
  // 如果项目已确认合作，显示蓝色（与我的稿件保持一致）
  if (projectStatus === 'CONFIRMED') {
    return 'purple'
  }
  const colors = {
    'ACCEPTED': 'green',
    'COMPLETED': 'blue',
    'CANCELLED': 'red'
  }
  return colors[status] || 'default'
}

const getStatusText = (status, projectStatus) => {
  // 如果项目已完成，显示"已完成"
  if (projectStatus === 'COMPLETED') {
    return '已完成'
  }
  // 如果项目已确认合作，显示"已确定合作"（与我的稿件保持一致）
  if (projectStatus === 'CONFIRMED') {
    return '已确定合作'
  }
  const texts = {
    'ACCEPTED': '已接单',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return texts[status] || status
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 19)
}

const loadOrders = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/orders/my')
    if (res.code === '200') {
      let list = res.data || []
      
      // 前端筛选
      if (filterForm.projectTitle) {
        list = list.filter(item => 
          item.projectTitle && item.projectTitle.includes(filterForm.projectTitle)
        )
      }
      if (filterForm.status) {
        list = list.filter(item => item.status === filterForm.status)
      }
      
      // 标记是否已提交稿件，并确保字段存在
      list.forEach(order => {
        order.hasSubmission = mySubmissions.value.some(sub => 
          sub.projectId === order.projectId && 
          (sub.status === 'SUBMITTED' || sub.status === 'INTERESTED' || sub.status === 'CONFIRMED')
        )
        // 确保projectStatus字段存在
        if (!order.projectStatus) {
          order.projectStatus = null
        }
        // 确保abandoned字段存在
        if (order.abandoned === undefined || order.abandoned === null) {
          order.abandoned = false
        }
      })
      
      orderList.value = list
      pagination.total = list.length
    } else {
      message.error(res.msg || '加载接单列表失败')
    }
  } catch (error) {
    console.error('加载接单列表失败:', error)
    message.error('加载接单列表失败')
  } finally {
    loading.value = false
  }
}

const loadMySubmissions = async () => {
  try {
    const res = await request.get('/api/submissions/my')
    if (res.code === '200') {
      mySubmissions.value = res.data || []
    }
  } catch (error) {
    console.error('加载稿件列表失败:', error)
  }
}

const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
}

const handleFilter = () => {
  pagination.current = 1
  loadOrders()
}

const handleReset = () => {
  filterForm.projectTitle = ''
  filterForm.status = undefined
  pagination.current = 1
  loadOrders()
}

const handleViewProject = (projectId) => {
  router.push(`/front/projects/${projectId}`)
}

const handleSubmitSubmission = (projectId) => {
  router.push(`/front/submissions/submit/${projectId}`)
}

const handleViewSubmission = async (projectId) => {
  // 查找该项目的稿件
  const submission = mySubmissions.value.find(sub => sub.projectId === projectId)
  if (submission) {
    router.push(`/front/submissions/${submission.id}`)
  } else {
    message.error('未找到稿件信息')
  }
}

const handleCancel = (orderId) => {
  Modal.confirm({
    title: '确认取消',
    content: '确定要取消此接单吗？',
    onOk: async () => {
      try {
        const res = await request.put(`/api/orders/cancel/${orderId}`)
        if (res.code === '200') {
          message.success('取消接单成功')
          loadOrders()
        } else {
          message.error(res.msg)
        }
      } catch (error) {
        message.error('取消接单失败')
      }
    }
  })
}

const handleCancelWithDeposit = (orderId) => {
  Modal.confirm({
    title: '确认取消接单',
    content: '项目已确认合作，取消接单将导致您的保证金赔付给企业，无法恢复。确定要继续吗？',
    okText: '确定取消',
    cancelText: '我再想想',
    onOk: async () => {
      try {
        const res = await request.put(`/api/orders/abandon/${orderId}`)
        if (res.code === '200') {
          message.success('取消接单成功，保证金已赔付给企业')
          loadOrders()
        } else {
          message.error(res.msg)
        }
      } catch (error) {
        if (error.response?.data?.msg) {
          message.error(error.response.data.msg)
        } else {
          message.error('取消接单失败')
        }
      }
    }
  })
}

const handleSubmitDeliverable = async (projectId) => {
  // 查找该项目的稿件
  const submission = mySubmissions.value.find(sub => sub.projectId === projectId && sub.status === 'CONFIRMED')
  if (submission) {
    router.push(`/front/deliverables/submit/${submission.id}`)
  } else {
    message.error('未找到已确认的稿件信息')
  }
}

const handleAbandon = (orderId) => {
  Modal.confirm({
    title: '确认放弃',
    content: '确定要放弃此接单吗？放弃后，您的保证金将赔付给企业，无法恢复。',
    onOk: async () => {
      try {
        const res = await request.put(`/api/orders/abandon/${orderId}`)
        if (res.code === '200') {
          message.success('放弃接单成功，保证金已赔付给企业')
          loadOrders()
        } else {
          message.error(res.msg)
        }
      } catch (error) {
        if (error.response?.data?.msg) {
          message.error(error.response.data.msg)
        } else {
          message.error('放弃接单失败')
        }
      }
    }
  })
}

onMounted(async () => {
  await loadMySubmissions()
  await loadOrders()
})
</script>

<style scoped>
.my-orders-container {
  padding: 20px;
}
</style>

