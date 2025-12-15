<template>
  <div class="submission-container">
    <a-card class="submission-card">
      <template #title>
        <div class="card-header">
          <h2 class="card-title">
            <FileTextOutlined />
            我的提交
          </h2>
        </div>
      </template>

      <!-- 筛选条件 -->
      <div class="filter-section">
        <a-form :model="filterForm" layout="inline" class="filter-form">
          <a-form-item label="类型">
            <a-select
              v-model:value="filterForm.type"
              placeholder="全部类型"
              allow-clear
              style="width: 120px"
              @change="handleFilter"
            >
              <a-select-option value="SUBMISSION">稿件</a-select-option>
              <a-select-option value="DELIVERABLE">成品</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="项目状态">
            <a-select
              v-model:value="filterForm.projectStatus"
              placeholder="全部状态"
              allow-clear
              style="width: 140px"
              @change="handleFilter"
            >
              <a-select-option value="PUBLISHED">已发布</a-select-option>
              <a-select-option value="CLOSED">已截止</a-select-option>
              <a-select-option value="CONFIRMED">已确定合作</a-select-option>
              <a-select-option value="COMPLETED">已完成</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="提交状态">
            <a-select
              v-model:value="filterForm.status"
              placeholder="全部状态"
              allow-clear
              style="width: 140px"
              @change="handleFilter"
            >
              <template v-if="!filterForm.type || filterForm.type === 'SUBMISSION'">
                <a-select-option value="SUBMITTED">已提交</a-select-option>
                <a-select-option value="INTERESTED">有意向</a-select-option>
                <a-select-option value="REJECTED">已拒绝</a-select-option>
                <a-select-option value="CONFIRMED">已确定合作</a-select-option>
              </template>
              <template v-if="!filterForm.type || filterForm.type === 'DELIVERABLE'">
                <a-select-option value="SUBMITTED">已提交</a-select-option>
                <a-select-option value="APPROVED">验收通过</a-select-option>
                <a-select-option value="REJECTED">验收不通过</a-select-option>
                <a-select-option value="EXPIRED">已过期</a-select-option>
              </template>
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

      <a-table
        :columns="columns"
        :data-source="displayList"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
        class="submission-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <a-tag :color="record.type === 'SUBMISSION' ? 'processing' : 'success'">
              {{ record.type === 'SUBMISSION' ? '稿件' : '成品' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'title'">
            <div class="title-cell">
              <a-tooltip :title="record.title">
                {{ record.title }}
              </a-tooltip>
            </div>
          </template>
          <template v-else-if="column.key === 'projectTitle'">
            <div class="title-cell">
              <a-tooltip :title="record.projectTitle">
                {{ record.projectTitle }}
              </a-tooltip>
            </div>
          </template>
          <template v-else-if="column.key === 'projectStatus'">
            <a-tag :color="getProjectStatusColor(record.projectStatus)">
              {{ getProjectStatusText(record.projectStatus) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status, record.type)">
              {{ getStatusText(record.status, record.type) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'quotePrice'">
            <span class="price-text" v-if="record.quotePrice">¥{{ record.quotePrice }}</span>
            <span v-else class="no-data">-</span>
          </template>
          <template v-else-if="column.key === 'submitCount'">
            {{ record.submitCount || '-' }}
          </template>
          <template v-else-if="column.key === 'createdAt'">
            <span class="time-text">
              <ClockCircleOutlined />
              {{ formatDate(record.createdAt || record.submittedAt) }}
            </span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small" wrap>
              <a-button type="link" size="small" @click="handleViewDetail(record)">
                <EyeOutlined />
                {{ record.type === 'SUBMISSION' ? '查看详情' : '查看成品' }}
              </a-button>
              <template v-if="record.type === 'SUBMISSION'">
                <a-button
                  v-if="record.status === 'SUBMITTED'"
                  type="link"
                  size="small"
                  @click="handleEdit(record.originalId)"
                >
                  <EditOutlined />
                  修改
                </a-button>
                <a-button
                  v-if="record.status === 'SUBMITTED'"
                  type="link"
                  danger
                  size="small"
                  @click="handleWithdraw(record.originalId)"
                >
                  <DeleteOutlined />
                  撤回
                </a-button>
                <a-button
                  v-if="record.status === 'INTERESTED'"
                  type="primary"
                  size="small"
                  @click="handleEditQuote(record)"
                >
                  <DollarOutlined />
                  修改报价
                </a-button>
              </template>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, h } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { InputNumber } from 'ant-design-vue'
import {
  FileTextOutlined,
  SearchOutlined,
  ReloadOutlined,
  ClockCircleOutlined,
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  DollarOutlined
} from '@ant-design/icons-vue'
import request from '@/utils/request'

const router = useRouter()

const loading = ref(false)
const submissionList = ref([])
const deliverableList = ref([])

const filterForm = reactive({
  type: undefined,
  projectStatus: undefined,
  status: undefined
})

const columns = [
  { title: '类型', key: 'type', width: 80 },
  { title: '标题', key: 'title', ellipsis: true },
  { title: '项目标题', key: 'projectTitle', ellipsis: true },
  { title: '项目状态', key: 'projectStatus', width: 110 },
  { title: '提交状态', key: 'status', width: 110 },
  { title: '报价', key: 'quotePrice', width: 100 },
  { title: '提交次数', key: 'submitCount', width: 90 },
  { title: '提交时间', key: 'createdAt', width: 160 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
]

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total) => `共 ${total} 条`
})

const displayList = computed(() => {
  let list = []
  
  submissionList.value.forEach(item => {
    list.push({
      ...item,
      type: 'SUBMISSION',
      id: `submission_${item.id}`,
      originalId: item.id
    })
  })
  
  deliverableList.value.forEach(item => {
    list.push({
      ...item,
      type: 'DELIVERABLE',
      id: `deliverable_${item.id}`,
      originalId: item.id
    })
  })
  
  let filtered = list
  if (filterForm.type) {
    filtered = filtered.filter(item => item.type === filterForm.type)
  }
  if (filterForm.projectStatus) {
    filtered = filtered.filter(item => item.projectStatus === filterForm.projectStatus)
  }
  if (filterForm.status) {
    filtered = filtered.filter(item => item.status === filterForm.status)
  }
  
  filtered.sort((a, b) => {
    const timeA = a.createdAt || a.submittedAt || ''
    const timeB = b.createdAt || b.submittedAt || ''
    return timeB.localeCompare(timeA)
  })
  
  pagination.total = filtered.length
  
  const start = (pagination.current - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  return filtered.slice(start, end)
})

const getProjectStatusColor = (status) => {
  const colors = {
    'PUBLISHED': 'success',
    'CLOSED': 'warning',
    'CONFIRMED': 'processing',
    'COMPLETED': 'purple'
  }
  return colors[status] || 'default'
}

const getProjectStatusText = (status) => {
  const texts = {
    'PUBLISHED': '已发布',
    'CLOSED': '已截止',
    'CONFIRMED': '已确定合作',
    'COMPLETED': '已完成'
  }
  return texts[status] || status || '-'
}

const getStatusColor = (status, type) => {
  if (type === 'SUBMISSION') {
    const colors = {
      'SUBMITTED': 'processing',
      'INTERESTED': 'success',
      'REJECTED': 'error',
      'CONFIRMED': 'purple'
    }
    return colors[status] || 'default'
  } else {
    const colors = {
      'SUBMITTED': 'processing',
      'APPROVED': 'success',
      'REJECTED': 'error',
      'EXPIRED': 'warning'
    }
    return colors[status] || 'default'
  }
}

const getStatusText = (status, type) => {
  if (type === 'SUBMISSION') {
    const texts = {
      'SUBMITTED': '已提交',
      'INTERESTED': '有意向',
      'REJECTED': '已拒绝',
      'CONFIRMED': '已确定合作'
    }
    return texts[status] || status
  } else {
    const texts = {
      'SUBMITTED': '已提交',
      'APPROVED': '验收通过',
      'REJECTED': '验收不通过',
      'EXPIRED': '已过期'
    }
    return texts[status] || status
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 16)
}

const loadSubmissions = async () => {
  try {
    const res = await request.get('/api/submissions/my')
    if (res.code === '200') {
      submissionList.value = res.data || []
    } else {
      message.error(res.msg)
    }
  } catch (error) {
    console.error('加载稿件列表失败:', error)
  }
}

const loadDeliverables = async () => {
  try {
    const res = await request.get('/api/deliverables/my')
    if (res.code === '200') {
      deliverableList.value = res.data || []
    } else {
      console.error('加载成品列表失败:', res.msg)
    }
  } catch (error) {
    console.error('加载成品列表失败:', error)
  }
}

const loadData = async () => {
  loading.value = true
  try {
    await Promise.all([loadSubmissions(), loadDeliverables()])
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
}

const handleFilter = () => {
  pagination.current = 1
}

const handleReset = () => {
  filterForm.type = undefined
  filterForm.projectStatus = undefined
  filterForm.status = undefined
  pagination.current = 1
}

const handleViewDetail = (record) => {
  if (record.type === 'SUBMISSION') {
    router.push(`/front/submissions/${record.originalId}`)
  } else {
    router.push(`/front/submissions/${record.submissionId}`)
  }
}

const handleEdit = (id) => {
  router.push(`/front/submissions/edit/${id}`)
}

const handleEditQuote = (record) => {
  const quoteRef = ref(record.quotePrice || null)
  
  Modal.confirm({
    title: '修改报价',
    width: 500,
    content: h => {
      return h('div', { style: { padding: '16px 0' } }, [
        h('p', { style: { marginBottom: '16px', color: '#5e6068' } }, 
          `当前报价：${record.quotePrice ? '¥' + record.quotePrice : '未报价'}`
        ),
        h('div', { style: { display: 'flex', alignItems: 'center', gap: '12px' } }, [
          h('span', { style: { color: '#14171f', fontWeight: '500' } }, '新报价：'),
          h(InputNumber, {
            modelValue: quoteRef.value,
            'onUpdate:modelValue': (val) => { quoteRef.value = val },
            min: 0,
            precision: 2,
            style: { width: '200px' },
            placeholder: '请输入新报价',
            prefix: '¥'
          })
        ])
      ])
    },
    okText: '确认修改',
    cancelText: '取消',
    onOk: async () => {
      if (quoteRef.value === null || quoteRef.value === undefined) {
        message.warning('请输入报价')
        return Promise.reject()
      }
      
      if (quoteRef.value === record.quotePrice) {
        message.info('报价未发生变化')
        return Promise.reject()
      }
      
      try {
        const res = await request.put(`/api/submissions/${record.originalId}/quote`, null, {
          params: {
            quotePrice: quoteRef.value
          }
        })
        if (res.code === '200') {
          message.success('报价修改成功')
          loadData()
        } else {
          message.error(res.msg || '修改报价失败')
        }
      } catch (error) {
        console.error('修改报价失败:', error)
        message.error('修改报价失败')
      }
    }
  })
}

const handleWithdraw = (id) => {
  Modal.confirm({
    title: '确认撤回',
    content: '确定要撤回此稿件吗？撤回后无法恢复。',
    okText: '确认撤回',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await request.delete(`/api/submissions/${id}`)
        if (res.code === '200') {
          message.success('撤回成功')
          loadData()
        } else {
          message.error(res.msg)
        }
      } catch (error) {
        message.error('撤回失败')
      }
    }
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.submission-container {
  padding: 24px;
  background: var(--bg-secondary);
  min-height: calc(100vh - 140px);
}

.submission-card {
  border-radius: 12px;
  box-shadow: var(--shadow-sm);
}

.submission-card :deep(.ant-card-head) {
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

/* 表格样式 */
.submission-table :deep(.ant-table-thead > tr > th) {
  background: var(--bg-secondary) !important;
  font-weight: 600;
  color: var(--text-primary);
  padding: 14px 16px;
}

.submission-table :deep(.ant-table-tbody > tr > td) {
  padding: 14px 16px;
}

.submission-table :deep(.ant-table-tbody > tr:hover > td) {
  background: var(--primary-light) !important;
}

.title-cell {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
  color: var(--text-primary);
}

.price-text {
  color: var(--salary-color);
  font-weight: 600;
}

.no-data {
  color: var(--text-disabled);
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
  .submission-container {
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
