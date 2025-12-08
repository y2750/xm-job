<template>
  <div class="project-submissions-container">
    <a-card>
      <template #title>
        <div class="page-header">
          <a-button type="text" @click="handleBack" style="margin-right: 8px">
            <template #icon><ArrowLeftOutlined /></template>
          </a-button>
          <span>{{ projectTitle }} - 稿件列表</span>
        </div>
      </template>
      <template #extra>
        <a-button type="primary" @click="handleViewDeliverables">
          成品
        </a-button>
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
            <a-select-option value="INTERESTED">有意向</a-select-option>
            <a-select-option value="REJECTED">已拒绝</a-select-option>
            <a-select-option value="CONFIRMED">已确定合作</a-select-option>
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

      <!-- 稿件列表 -->
      <a-table
        :columns="columns"
        :data-source="submissionList"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'index'">
            {{ (pagination.current - 1) * pagination.pageSize + submissionList.indexOf(record) + 1 }}
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'quotePrice'">
            {{ record.quotePrice ? `¥${record.quotePrice}` : '-' }}
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatDate(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'freelancerName'">
            <a-button type="link" @click="handleViewFreelancer(record.freelancerId)" style="padding: 0">
              {{ record.freelancerName || '未知' }}
            </a-button>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" @click="handleViewDetail(record.id)">查看详情</a-button>
              <a-button
                v-if="record.status === 'SUBMITTED'"
                type="link"
                @click="handleMarkInterested(record.id)"
              >
                标记为有意向
              </a-button>
              <a-button
                v-if="record.status === 'SUBMITTED'"
                type="link"
                danger
                @click="handleReject(record.id)"
              >
                拒绝
              </a-button>
              <a-button
                v-if="record.status === 'INTERESTED' && !record.enterpriseConfirmed"
                type="link"
                @click="handleConfirm(record.id)"
              >
                确定合作
              </a-button>
              <a-tag v-if="record.status === 'INTERESTED' && record.enterpriseConfirmed && !record.freelancerConfirmed" color="green">
                企业已确认，等待接单者确认
              </a-tag>
              <a-tag v-if="record.status === 'INTERESTED' && record.freelancerConfirmed" color="blue">
                接单者已确认，等待项目进入合作状态
              </a-tag>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 自由职业者详情弹窗 -->
    <a-modal
      v-model:open="freelancerModalVisible"
      title="提交者信息"
      width="600px"
      :footer="null"
    >
      <div v-if="freelancerDetail" style="padding: 10px 0">
        <div style="display: flex; align-items: center; margin-bottom: 20px">
          <a-avatar :size="60" :src="freelancerDetail.userAvatar" v-if="freelancerDetail.userAvatar">
            <template #icon><UserOutlined /></template>
          </a-avatar>
          <a-avatar :size="60" v-else>
            <template #icon><UserOutlined /></template>
          </a-avatar>
          <div style="margin-left: 15px">
            <h3 style="margin: 0 0 5px 0">{{ freelancerDetail.userName || '-' }}</h3>
            <a-tag :color="freelancerDetail.verified ? 'green' : 'orange'">
              {{ freelancerDetail.verified ? '已认证' : '未认证' }}
            </a-tag>
          </div>
        </div>
        <a-descriptions :column="1" bordered>
          <a-descriptions-item label="姓名">
            {{ freelancerDetail.userName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="联系电话">
            {{ freelancerDetail.userPhone || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="邮箱">
            {{ freelancerDetail.userEmail || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="技能标签">
            <a-tag v-for="skill in (freelancerDetail.skills ? freelancerDetail.skills.split(/[,，]/).filter(s => s.trim()) : [])" :key="skill" style="margin-right: 8px">
              {{ skill }}
            </a-tag>
            <span v-if="!freelancerDetail.skills">-</span>
          </a-descriptions-item>
          <a-descriptions-item label="作品集链接">
            <a v-if="freelancerDetail.portfolioUrl" :href="freelancerDetail.portfolioUrl" target="_blank">
              {{ freelancerDetail.portfolioUrl }}
            </a>
            <span v-else>-</span>
          </a-descriptions-item>
          <a-descriptions-item label="作品数量">
            {{ freelancerDetail.portfolioCount || 0 }}
          </a-descriptions-item>
          <a-descriptions-item label="信誉分">
            <span style="color: #52c41a; font-weight: bold">{{ freelancerDetail.creditScore || 100 }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="认证状态">
            <a-tag :color="freelancerDetail.verified ? 'green' : 'orange'">
              {{ freelancerDetail.verified ? '已认证' : '未认证' }}
            </a-tag>
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { ArrowLeftOutlined, UserOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submissionList = ref([])
const projectTitle = ref('')
const projectId = ref(null)

const filterForm = reactive({
  status: undefined,
  freelancerName: ''
})

const columns = [
  { title: '序号', key: 'index', width: 80 },
  { title: '稿件标题', dataIndex: 'title', key: 'title' },
  { title: '提交者', key: 'freelancerName', width: 120 },
  { title: '报价', key: 'quotePrice', width: 120 },
  { title: '状态', key: 'status', width: 120 },
  { title: '提交时间', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 300 }
]

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total) => `共 ${total} 条`
})

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

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 19)
}

const loadProject = async () => {
  if (!projectId.value) return
  try {
    const res = await request.get(`/api/projects/${projectId.value}`)
    if (res.code === '200') {
      projectTitle.value = res.data?.title || '项目稿件'
    }
  } catch (error) {
    console.error('加载项目信息失败:', error)
  }
}

const loadSubmissions = async () => {
  if (!projectId.value) return
  loading.value = true
  try {
    const params = {
      projectId: projectId.value,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    
    // 添加筛选条件
    if (filterForm.status) {
      params.status = filterForm.status
    }
    if (filterForm.freelancerName) {
      params.freelancerName = filterForm.freelancerName
    }
    
    const res = await request.get('/api/submissions', { params })
    if (res.code === '200') {
      const data = res.data
      if (data) {
        submissionList.value = data.list || []
        pagination.total = data.total || 0
      } else {
        submissionList.value = []
        pagination.total = 0
      }
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

const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadSubmissions()
}

const handleFilter = () => {
  pagination.current = 1
  loadSubmissions()
}

const handleReset = () => {
  filterForm.status = undefined
  filterForm.freelancerName = ''
  pagination.current = 1
  loadSubmissions()
}

const handleViewDetail = (id) => {
  router.push(`/front/submissions/${id}`)
}

const handleMarkInterested = (id) => {
  Modal.confirm({
    title: '确认操作',
    content: '确定要将此稿件标记为"有意向"吗？',
    onOk: async () => {
      try {
        const res = await request.put(`/api/submissions/${id}/status?status=INTERESTED`)
        if (res.code === '200') {
          message.success('操作成功')
          loadSubmissions()
        } else {
          message.error(res.msg)
        }
      } catch (error) {
        message.error('操作失败')
      }
    }
  })
}

const handleReject = (id) => {
  Modal.confirm({
    title: '确认拒绝',
    content: '确定要拒绝此稿件吗？',
    onOk: async () => {
      try {
        const res = await request.put(`/api/submissions/${id}/status?status=REJECTED`)
        if (res.code === '200') {
          message.success('操作成功')
          loadSubmissions()
        } else {
          message.error(res.msg)
        }
      } catch (error) {
        message.error('操作失败')
      }
    }
  })
}

const handleConfirm = (id) => {
  const submission = submissionList.value.find(s => s.id === id)
  if (!submission || !submission.quotePrice) {
    message.error('稿件未设置报价，无法确定合作')
    return
  }
  
  // 跳转到支付界面
  router.push(`/front/payment/confirm/${id}`)
}

const handleBack = () => {
  router.back()
}

const handleViewDeliverables = () => {
  if (!projectId.value) return
  router.push(`/front/projects/${projectId.value}/deliverables`)
}

const freelancerDetail = ref(null)
const freelancerModalVisible = ref(false)

const handleViewFreelancer = async (freelancerId) => {
  if (!freelancerId) {
    message.warning('提交者信息不存在')
    return
  }
  try {
    const res = await request.get(`/api/freelancers/${freelancerId}`)
    if (res.code === '200' && res.data) {
      freelancerDetail.value = res.data
      freelancerModalVisible.value = true
    } else {
      message.error(res.msg || '加载自由职业者信息失败')
    }
  } catch (error) {
    console.error('加载自由职业者信息失败:', error)
    message.error('加载自由职业者信息失败')
  }
}

onMounted(() => {
  projectId.value = parseInt(route.params.projectId)
  if (projectId.value) {
    loadProject()
    loadSubmissions()
  } else {
    message.error('项目ID不存在')
    router.back()
  }
})
</script>

<style scoped>
.project-submissions-container {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  font-size: 16px;
  font-weight: 500;
}
</style>

