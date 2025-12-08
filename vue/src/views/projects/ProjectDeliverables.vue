<template>
  <div class="project-deliverables-container">
    <a-card>
      <template #title>
        <div class="page-header">
          <a-button type="text" @click="handleBack" style="margin-right: 8px">
            <template #icon><ArrowLeftOutlined /></template>
          </a-button>
          <span>{{ projectTitle }} - 成品列表</span>
        </div>
      </template>

      <!-- 筛选条件 -->
      <a-form :model="filterForm" layout="inline" style="margin-bottom: 20px">
        <a-form-item label="状态">
          <a-select
            v-model:value="filterForm.status"
            placeholder="全部状态"
            allow-clear
            style="width: 150px"
            @change="handleFilter"
          >
            <a-select-option value="SUBMITTED">已提交</a-select-option>
            <a-select-option value="APPROVED">验收通过</a-select-option>
            <a-select-option value="REJECTED">验收不通过</a-select-option>
            <a-select-option value="EXPIRED">已过期</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="提交者">
          <a-input
            v-model:value="filterForm.freelancerName"
            placeholder="输入提交者姓名"
            allow-clear
            style="width: 200px"
            @pressEnter="handleFilter"
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleFilter">筛选</a-button>
          <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
        </a-form-item>
      </a-form>

      <!-- 成品列表 -->
      <a-table
        :columns="columns"
        :data-source="deliverableList"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'index'">
            {{ (pagination.current - 1) * pagination.pageSize + deliverableList.indexOf(record) + 1 }}
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getDeliverableStatusColor(record.status)">
              {{ getDeliverableStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'submittedAt'">
            {{ formatDate(record.submittedAt) }}
          </template>
          <template v-else-if="column.key === 'reviewedAt'">
            {{ record.reviewedAt ? formatDate(record.reviewedAt) : '-' }}
          </template>
          <template v-else-if="column.key === 'submitCount'">
            第 {{ record.submitCount || 1 }} 次提交
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" @click="handleViewDetail(record.id)">查看详情</a-button>
              <a-button
                v-if="record.status === 'SUBMITTED'"
                type="primary"
                @click="handleReview(record.id, 'APPROVED')"
              >
                验收通过
              </a-button>
              <a-button
                v-if="record.status === 'SUBMITTED'"
                danger
                @click="handleReview(record.id, 'REJECTED')"
              >
                验收不通过
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 成品详情模态框 -->
    <a-modal
      v-model:open="detailModalVisible"
      title="成品详情"
      :footer="null"
      width="800px"
    >
      <a-descriptions :column="2" bordered v-if="currentDeliverable">
        <a-descriptions-item label="标题" :span="2">
          {{ currentDeliverable.title }}
        </a-descriptions-item>
        <a-descriptions-item label="描述" :span="2">
          <div v-html="currentDeliverable.description"></div>
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getDeliverableStatusColor(currentDeliverable.status)">
            {{ getDeliverableStatusText(currentDeliverable.status) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="提交次数">
          第 {{ currentDeliverable.submitCount || 1 }} 次提交
        </a-descriptions-item>
        <a-descriptions-item label="提交时间">
          {{ formatDate(currentDeliverable.submittedAt) }}
        </a-descriptions-item>
        <a-descriptions-item label="验收时间">
          {{ currentDeliverable.reviewedAt ? formatDate(currentDeliverable.reviewedAt) : '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="提交者" :span="2">
          {{ currentDeliverable.freelancerName || '未知' }}
        </a-descriptions-item>
        <a-descriptions-item v-if="currentDeliverable.reviewComment" label="验收意见" :span="2">
          {{ currentDeliverable.reviewComment }}
        </a-descriptions-item>
        <a-descriptions-item v-if="currentDeliverable.attachments && currentDeliverable.attachments.length > 0" label="附件" :span="2">
          <a-list
            :data-source="currentDeliverable.attachments"
            :bordered="false"
          >
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta>
                  <template #title>
                    <a :href="item.fileUrl" target="_blank">{{ item.fileName }}</a>
                  </template>
                  <template #description>
                    {{ formatFileSize(item.fileSize) }} - {{ item.fileType }}
                  </template>
                </a-list-item-meta>
                <template #actions>
                  <a-button type="link" @click="handleDownload(item.id)">下载</a-button>
                </template>
              </a-list-item>
            </template>
          </a-list>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const projectTitle = ref('')
const deliverableList = ref([])
const detailModalVisible = ref(false)
const currentDeliverable = ref(null)

const filterForm = reactive({
  status: null,
  freelancerName: ''
})

const columns = [
  { title: '序号', key: 'index', width: 80 },
  { title: '标题', dataIndex: 'title', key: 'title' },
  { title: '提交者', dataIndex: 'freelancerName', key: 'freelancerName' },
  { title: '状态', key: 'status', width: 120 },
  { title: '提交次数', key: 'submitCount', width: 100 },
  { title: '提交时间', key: 'submittedAt', width: 180 },
  { title: '验收时间', key: 'reviewedAt', width: 180 },
  { title: '操作', key: 'action', width: 250 }
]

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total) => `共 ${total} 条`
})

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 19)
}

const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const getDeliverableStatusColor = (status) => {
  const colors = {
    'SUBMITTED': 'blue',
    'APPROVED': 'green',
    'REJECTED': 'red',
    'EXPIRED': 'orange'
  }
  return colors[status] || 'default'
}

const getDeliverableStatusText = (status) => {
  const texts = {
    'SUBMITTED': '已提交',
    'APPROVED': '验收通过',
    'REJECTED': '验收不通过',
    'EXPIRED': '已过期'
  }
  return texts[status] || status
}

const loadProject = async () => {
  try {
    const res = await request.get(`/api/projects/${route.params.projectId}`)
    if (res.code === '200' && res.data) {
      projectTitle.value = res.data.title
    }
  } catch (error) {
    console.error('加载项目信息失败:', error)
  }
}

const loadDeliverables = async () => {
  loading.value = true
  try {
    const params = {
      projectId: parseInt(route.params.projectId)
    }
    
    if (filterForm.status) {
      params.status = filterForm.status
    }
    
    const res = await request.get('/api/deliverables', { params })
    if (res.code === '200') {
      let allDeliverables = []
      if (Array.isArray(res.data)) {
        allDeliverables = res.data
      } else if (res.data && res.data.list) {
        allDeliverables = res.data.list || []
      }
      
      // 前端筛选 freelancerName
      if (filterForm.freelancerName) {
        allDeliverables = allDeliverables.filter(d => 
          d.freelancerName && d.freelancerName.includes(filterForm.freelancerName)
        )
      }
      
      // 前端分页
      const start = (pagination.current - 1) * pagination.pageSize
      const end = start + pagination.pageSize
      deliverableList.value = allDeliverables.slice(start, end)
      pagination.total = allDeliverables.length
      
      // 加载每个成品的附件
      for (const deliverable of deliverableList.value) {
        await loadDeliverableAttachments(deliverable)
      }
    } else {
      message.error(res.msg || '加载成品列表失败')
    }
  } catch (error) {
    console.error('加载成品列表失败:', error)
    message.error('加载成品列表失败')
  } finally {
    loading.value = false
  }
}

const loadDeliverableAttachments = async (deliverable) => {
  try {
    // 使用后端 selectById 接口，它会自动加载附件
    const res = await request.get(`/api/deliverables/${deliverable.id}`)
    if (res.code === '200' && res.data && res.data.attachments) {
      deliverable.attachments = Array.isArray(res.data.attachments) ? res.data.attachments : []
    } else {
      deliverable.attachments = []
    }
  } catch (error) {
    console.error('加载附件失败:', error)
    deliverable.attachments = []
  }
}

const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadDeliverables()
}

const handleFilter = () => {
  pagination.current = 1
  loadDeliverables()
}

const handleReset = () => {
  filterForm.status = null
  filterForm.freelancerName = ''
  pagination.current = 1
  loadDeliverables()
}

const handleBack = () => {
  router.back()
}

const handleViewDetail = async (id) => {
  const deliverable = deliverableList.value.find(d => d.id === id)
  if (!deliverable) {
    message.error('成品不存在')
    return
  }
  
  // 如果还没有加载附件，先加载
  if (!deliverable.attachments) {
    await loadDeliverableAttachments(deliverable)
  }
  
  currentDeliverable.value = deliverable
  detailModalVisible.value = true
}

const handleDownload = async (attachmentId) => {
  try {
    const url = `/api/attachments/deliverable/download/${attachmentId}`
    const token = localStorage.getItem('xm-token')
    
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:9090'}${url}`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    
    if (!response.ok) {
      const errorData = await response.json()
      message.error(errorData.msg || '下载失败')
      return
    }
    
    const blob = await response.blob()
    const contentDisposition = response.headers.get('Content-Disposition')
    let fileName = 'download'
    if (contentDisposition) {
      const fileNameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
      if (fileNameMatch && fileNameMatch[1]) {
        fileName = fileNameMatch[1].replace(/['"]/g, '')
        fileName = decodeURIComponent(fileName)
      }
    }
    
    const url2 = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url2
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url2)
    document.body.removeChild(a)
    message.success('下载成功')
  } catch (error) {
    console.error('下载失败:', error)
    message.error('下载失败')
  }
}

const handleReview = (deliverableId, status) => {
  const statusText = status === 'APPROVED' ? '验收通过' : '验收不通过'
  let content = `确定要${statusText}此成品吗？`
  
  if (status === 'REJECTED') {
    content = '确定要验收不通过此成品吗？不通过将扣除接单者信誉分。'
  }
  
  Modal.confirm({
    title: '确认验收',
    content: content,
    onOk: async () => {
      try {
        const res = await request.put(`/api/deliverables/${deliverableId}/review`, null, {
          params: {
            status: status,
            reviewComment: status === 'APPROVED' ? '验收通过' : '验收不通过'
          }
        })
        if (res.code === '200') {
          message.success('验收成功')
          await loadDeliverables()
        } else {
          message.error(res.msg)
        }
      } catch (error) {
        message.error('验收失败')
      }
    }
  })
}

onMounted(async () => {
  await loadProject()
  await loadDeliverables()
})
</script>

<style scoped>
.project-deliverables-container {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
}
</style>

