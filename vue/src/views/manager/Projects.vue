<template>
  <a-card>
    <template #title>
      <h3>项目管理</h3>
    </template>
    
    <!-- 搜索筛选 -->
    <a-form :model="searchForm" layout="inline" style="margin-bottom: 20px">
      <a-form-item label="关键词">
        <a-input v-model:value="searchForm.title" placeholder="项目标题" allow-clear style="width: 200px" />
      </a-form-item>
      <a-form-item label="状态">
        <a-select v-model:value="searchForm.status" placeholder="项目状态" allow-clear style="width: 150px">
          <a-select-option value="PENDING">待审核</a-select-option>
          <a-select-option value="PUBLISHED">已发布</a-select-option>
          <a-select-option value="REJECTED">已打回</a-select-option>
          <a-select-option value="CLOSED">已截止</a-select-option>
          <a-select-option value="CONFIRMED">已确定合作</a-select-option>
          <a-select-option value="COMPLETED">已完成</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="handleSearch">搜索</a-button>
        <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
      </a-form-item>
    </a-form>

    <!-- 项目列表 -->
    <a-table
      :columns="columns"
      :data-source="projectList"
      :pagination="pagination"
      :loading="loading"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-button type="link" @click="handleView(record.id)">查看</a-button>
          <a-button v-if="record.status === 'PENDING'" type="link" style="color: #52c41a" @click="handleApprove(record.id)">审核通过</a-button>
          <a-button v-if="record.status === 'PENDING'" type="link" danger @click="handleReject(record.id)">打回</a-button>
          <a-button type="link" danger @click="handleDelete(record.id)">删除</a-button>
        </template>
      </template>
    </a-table>
    
    <!-- 打回理由弹窗 -->
    <a-modal
      v-model:open="rejectModalVisible"
      title="打回项目"
      @ok="handleRejectSubmit"
      @cancel="() => { rejectModalVisible = false; rejectReason = ''; rejectProjectId = null }"
    >
      <a-form-item label="打回理由" required>
        <a-textarea
          v-model:value="rejectReason"
          placeholder="请输入打回理由"
          :rows="4"
          :maxlength="500"
          show-count
        />
      </a-form-item>
    </a-modal>
  </a-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()

const loading = ref(false)
const projectList = ref([])

const searchForm = reactive({
  title: '',
  status: ''
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total) => `共 ${total} 条`,
  onChange: (page, pageSize) => {
    pagination.current = page
    pagination.pageSize = pageSize
    loadProjects()
  }
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '项目标题', dataIndex: 'title', key: 'title' },
  { title: '发布企业', dataIndex: 'enterpriseName', key: 'enterpriseName' },
  { title: '预算范围', key: 'budget', customRender: ({ record }) => `${record.budgetMin || 0} - ${record.budgetMax || 0} 元` },
  { title: '截止时间', dataIndex: 'deadline', key: 'deadline', customRender: ({ record }) => formatDate(record.deadline) },
  { title: '状态', key: 'status', width: 120 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', customRender: ({ record }) => formatDate(record.createdAt) },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  if (typeof dateStr === 'string') {
    return dateStr.replace('T', ' ').substring(0, 19)
  }
  return dateStr
}

const getStatusColor = (status) => {
  const colors = {
    'PENDING': 'orange',
    'PUBLISHED': 'green',
    'REJECTED': 'red',
    'CLOSED': 'default',
    'CONFIRMED': 'blue',
    'COMPLETED': 'purple'
  }
  return colors[status] || 'default'
}

const getStatusText = (status) => {
  const texts = {
    'PENDING': '待审核',
    'PUBLISHED': '已发布',
    'REJECTED': '已打回',
    'CLOSED': '已截止',
    'CONFIRMED': '已确定合作',
    'COMPLETED': '已完成'
  }
  return texts[status] || status
}

const loadProjects = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/projects', {
      params: {
        ...searchForm,
        pageNum: pagination.current,
        pageSize: pagination.pageSize
      }
    })
    if (res.code === '200') {
      const data = res.data
      projectList.value = data.list || []
      pagination.total = data.total || 0
    } else {
      message.error(res.msg || '加载项目列表失败')
    }
  } catch (error) {
    console.error('加载项目列表失败:', error)
    message.error('加载项目列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadProjects()
}

const handleReset = () => {
  Object.assign(searchForm, {
    title: '',
    status: ''
  })
  handleSearch()
}

const handleView = (id) => {
  router.push(`/manager/projects/${id}`)
}

const handleDelete = (id) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除此项目吗？删除后不可恢复。',
    onOk: async () => {
      try {
        const res = await request.delete(`/api/projects/${id}`)
        if (res.code === '200') {
          message.success('删除成功')
          loadProjects()
        } else {
          message.error(res.msg || '删除失败')
        }
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
}

const handleApprove = (id) => {
  Modal.confirm({
    title: '确认审核通过',
    content: '确定要审核通过此项目吗？',
    onOk: async () => {
      try {
        const res = await request.put(`/api/projects/${id}/approve`)
        if (res.code === '200') {
          message.success('审核通过成功')
          loadProjects()
        } else {
          message.error(res.msg || '审核失败')
        }
      } catch (error) {
        message.error('审核失败')
      }
    }
  })
}

const rejectReason = ref('')
const rejectModalVisible = ref(false)
const rejectProjectId = ref(null)

const handleReject = (id) => {
  rejectProjectId.value = id
  rejectReason.value = ''
  rejectModalVisible.value = true
}

const handleRejectSubmit = async () => {
  if (!rejectReason.value || !rejectReason.value.trim()) {
    message.error('请输入打回理由')
    return
  }
  try {
    const res = await request.put(`/api/projects/${rejectProjectId.value}/reject`, {
      rejectReason: rejectReason.value
    })
    if (res.code === '200') {
      message.success('打回成功')
      rejectModalVisible.value = false
      rejectReason.value = ''
      rejectProjectId.value = null
      loadProjects()
    } else {
      message.error(res.msg || '打回失败')
    }
  } catch (error) {
    message.error('打回失败')
  }
}

onMounted(() => {
  loadProjects()
})
</script>

