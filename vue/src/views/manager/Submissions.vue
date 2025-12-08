<template>
  <a-card>
    <template #title>
      <h3>稿件管理</h3>
    </template>
    
    <!-- 搜索筛选 -->
    <a-form :model="searchForm" layout="inline" style="margin-bottom: 20px">
      <a-form-item label="关键词">
        <a-input v-model:value="searchForm.title" placeholder="稿件标题" allow-clear style="width: 200px" />
      </a-form-item>
      <a-form-item label="状态">
        <a-select v-model:value="searchForm.status" placeholder="稿件状态" allow-clear style="width: 150px">
          <a-select-option value="SUBMITTED">已提交</a-select-option>
          <a-select-option value="INTERESTED">有意向</a-select-option>
          <a-select-option value="REJECTED">已拒绝</a-select-option>
          <a-select-option value="CONFIRMED">已确定合作</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="handleSearch">搜索</a-button>
        <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
      </a-form-item>
    </a-form>

    <!-- 稿件列表 -->
    <a-table
      :columns="columns"
      :data-source="submissionList"
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
          <a-button type="link" danger @click="handleDelete(record.id)">删除</a-button>
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()

const loading = ref(false)
const submissionList = ref([])

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
    loadSubmissions()
  }
})

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  if (typeof dateStr === 'string') {
    return dateStr.replace('T', ' ').substring(0, 19)
  }
  return dateStr
}

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '稿件标题', dataIndex: 'title', key: 'title' },
  { title: '项目标题', dataIndex: 'projectTitle', key: 'projectTitle' },
  { title: '提交者', dataIndex: 'freelancerName', key: 'freelancerName' },
  { title: '报价', key: 'quotePrice', customRender: ({ record }) => record.quotePrice ? `¥${record.quotePrice}` : '未报价' },
  { title: '状态', key: 'status', width: 120 },
  { title: '提交时间', dataIndex: 'createdAt', key: 'createdAt', customRender: ({ record }) => formatDate(record.createdAt) },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]

const getStatusColor = (status) => {
  const colors = {
    'SUBMITTED': 'blue',
    'INTERESTED': 'green',
    'REJECTED': 'red',
    'CONFIRMED': 'purple'
  }
  return colors[status] || 'default'
}

const getStatusText = (status) => {
  const texts = {
    'SUBMITTED': '已提交',
    'INTERESTED': '有意向',
    'REJECTED': '已拒绝',
    'CONFIRMED': '已确定合作'
  }
  return texts[status] || status
}

const loadSubmissions = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/submissions', {
      params: {
        ...searchForm,
        pageNum: pagination.current,
        pageSize: pagination.pageSize
      }
    })
    if (res.code === '200') {
      const data = res.data
      submissionList.value = data.list || []
      pagination.total = data.total || 0
    } else {
      message.error(res.msg || '加载稿件列表失败')
    }
  } catch (error) {
    console.error('加载稿件列表失败:', error)
    message.error('加载稿件列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadSubmissions()
}

const handleReset = () => {
  Object.assign(searchForm, {
    title: '',
    status: ''
  })
  handleSearch()
}

const handleView = (id) => {
  router.push(`/manager/submissions/${id}`)
}

const handleDelete = (id) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除此稿件吗？删除后不可恢复。',
    onOk: async () => {
      try {
        const res = await request.delete(`/api/submissions/${id}`)
        if (res.code === '200') {
          message.success('删除成功')
          loadSubmissions()
        } else {
          message.error(res.msg || '删除失败')
        }
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
}

onMounted(() => {
  loadSubmissions()
})
</script>

