<template>
  <div class="orders-container">
    <a-card class="orders-card">
      <template #title>
        <div class="card-header">
          <h2 class="card-title">
            <UnorderedListOutlined />
            我的接单
          </h2>
        </div>
      </template>

      <!-- 筛选条件 -->
      <div class="filter-section">
        <a-form :model="filterForm" layout="inline" class="filter-form">
          <a-form-item label="项目标题">
            <a-input
              v-model:value="filterForm.projectTitle"
              placeholder="输入项目标题"
              allow-clear
              style="width: 220px"
              @pressEnter="handleFilter"
            >
              <template #prefix><SearchOutlined class="input-icon" /></template>
            </a-input>
          </a-form-item>
          <a-form-item label="状态">
            <a-select
              v-model:value="filterForm.status"
              placeholder="全部状态"
              allow-clear
              style="width: 140px"
              @change="handleFilter"
            >
              <a-select-option value="ACCEPTED">已接单</a-select-option>
              <a-select-option value="COMPLETED">已完成</a-select-option>
              <a-select-option value="CANCELLED">已取消</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="handleFilter">
                <SearchOutlined />
                筛选
              </a-button>
              <a-button @click="handleReset">
                <ReloadOutlined />
                重置
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- 接单列表 -->
      <a-table
        :columns="columns"
        :data-source="orderList"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
        class="orders-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'projectTitle'">
            <div class="project-title-cell">
              <a-tooltip :title="record.projectTitle">
                {{ record.projectTitle }}
              </a-tooltip>
            </div>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status, record.projectStatus)">
              {{ getStatusText(record.status, record.projectStatus) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'acceptedAt'">
            <span class="time-text">
              <ClockCircleOutlined />
              {{ formatDate(record.acceptedAt) }}
            </span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small" wrap>
              <a-button type="link" size="small" @click="handleViewProject(record.projectId)">
                <EyeOutlined />
                查看项目
              </a-button>
              <a-button
                v-if="record.status === 'ACCEPTED' && !record.hasSubmission && record.projectStatus !== 'CONFIRMED'"
                type="primary"
                size="small"
                @click="handleSubmitSubmission(record.projectId)"
              >
                <UploadOutlined />
                提交稿件
              </a-button>
              <a-button
                v-if="record.status === 'ACCEPTED' && record.hasSubmission && record.projectStatus !== 'CONFIRMED'"
                type="link"
                size="small"
                @click="handleViewSubmission(record.projectId)"
              >
                <FileSearchOutlined />
                查看稿件
              </a-button>
              <a-button
                v-if="record.status === 'ACCEPTED' && record.projectStatus === 'CONFIRMED'"
                type="primary"
                size="small"
                @click="handleSubmitDeliverable(record.projectId)"
              >
                <ContainerOutlined />
                提交成品
              </a-button>
              <a-button
                v-if="record.status === 'ACCEPTED' && !record.abandoned && record.projectStatus !== 'CONFIRMED'"
                type="link"
                danger
                size="small"
                @click="handleCancel(record.id)"
              >
                <CloseCircleOutlined />
                取消
              </a-button>
              <a-button
                v-if="record.status === 'ACCEPTED' && !record.abandoned && record.projectStatus === 'CONFIRMED'"
                type="link"
                danger
                size="small"
                @click="handleCancelWithDeposit(record.id)"
              >
                <CloseCircleOutlined />
                取消
              </a-button>
              <a-tag
                v-if="record.status === 'ACCEPTED' && record.abandoned"
                color="default"
              >
                已放弃
              </a-tag>
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
import {
  UnorderedListOutlined,
  SearchOutlined,
  ReloadOutlined,
  ClockCircleOutlined,
  EyeOutlined,
  UploadOutlined,
  FileSearchOutlined,
  ContainerOutlined,
  CloseCircleOutlined
} from '@ant-design/icons-vue'
import request from '@/utils/request'

const router = useRouter()

const loading = ref(false)
const orderList = ref([])
const mySubmissions = ref([])

const filterForm = reactive({
  projectTitle: '',
  status: undefined
})

const columns = [
  { title: '项目标题', dataIndex: 'projectTitle', key: 'projectTitle', ellipsis: true },
  { title: '状态', key: 'status', width: 120 },
  { title: '接单时间', key: 'acceptedAt', width: 180 },
  { title: '操作', key: 'action', width: 320, fixed: 'right' }
]

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total) => `共 ${total} 条`
})

const getStatusColor = (status, projectStatus) => {
  if (projectStatus === 'COMPLETED') {
    return 'purple'
  }
  if (projectStatus === 'CONFIRMED') {
    return 'purple'
  }
  const colors = {
    'ACCEPTED': 'success',
    'COMPLETED': 'processing',
    'CANCELLED': 'error'
  }
  return colors[status] || 'default'
}

const getStatusText = (status, projectStatus) => {
  if (projectStatus === 'COMPLETED') {
    return '已完成'
  }
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
  return dateStr.replace('T', ' ').substring(0, 16)
}

const loadOrders = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/orders/my')
    if (res.code === '200') {
      let list = res.data || []
      
      if (filterForm.projectTitle) {
        list = list.filter(item => 
          item.projectTitle && item.projectTitle.includes(filterForm.projectTitle)
        )
      }
      if (filterForm.status) {
        list = list.filter(item => item.status === filterForm.status)
      }
      
      list.forEach(order => {
        order.hasSubmission = mySubmissions.value.some(sub => 
          sub.projectId === order.projectId && 
          (sub.status === 'SUBMITTED' || sub.status === 'INTERESTED' || sub.status === 'CONFIRMED')
        )
        if (!order.projectStatus) {
          order.projectStatus = null
        }
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
    okText: '确认取消',
    cancelText: '我再想想',
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
    okType: 'danger',
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
    okText: '确认放弃',
    okType: 'danger',
    cancelText: '取消',
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
.orders-container {
  padding: 24px;
  background: var(--bg-secondary);
  min-height: calc(100vh - 140px);
}

.orders-card {
  border-radius: 12px;
  box-shadow: var(--shadow-sm);
}

.orders-card :deep(.ant-card-head) {
  border-bottom: 1px solid var(--border-light);
  padding: 20px 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-title .anticon {
  color: var(--primary-color);
}

/* 筛选区域 */
.filter-section {
  padding: 20px 24px;
  background: var(--bg-secondary);
  border-radius: 8px;
  margin-bottom: 20px;
}

.filter-form :deep(.ant-form-item) {
  margin-bottom: 0;
}

.input-icon {
  color: var(--text-tertiary);
}

/* 表格样式 */
.orders-table :deep(.ant-table-thead > tr > th) {
  background: var(--bg-secondary) !important;
  font-weight: 600;
  color: var(--text-primary);
  padding: 14px 16px;
}

.orders-table :deep(.ant-table-tbody > tr > td) {
  padding: 14px 16px;
}

.orders-table :deep(.ant-table-tbody > tr:hover > td) {
  background: var(--primary-light) !important;
}

.project-title-cell {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
  color: var(--text-primary);
}

.time-text {
  font-size: 13px;
  color: var(--text-tertiary);
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 响应式 */
@media (max-width: 768px) {
  .orders-container {
    padding: 16px;
  }
  
  .filter-form {
    flex-wrap: wrap;
  }
  
  .filter-form :deep(.ant-form-item) {
    margin-bottom: 12px;
  }
}
</style>
