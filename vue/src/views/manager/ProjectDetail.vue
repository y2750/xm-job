<template>
  <a-card :loading="loading">
    <template #title>
      <a-space>
        <a-button type="link" @click="handleBack">
          <template #icon>
            <ArrowLeftOutlined />
          </template>
          返回
        </a-button>
        <span>项目详情</span>
      </a-space>
    </template>

    <a-descriptions v-if="project" :column="2" bordered>
      <a-descriptions-item label="项目标题">{{ project.title }}</a-descriptions-item>
      <a-descriptions-item label="发布企业">{{ project.enterpriseName || '未知' }}</a-descriptions-item>
      <a-descriptions-item label="项目状态">
        <a-tag :color="getStatusColor(project.status)">{{ getStatusText(project.status) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="预算范围">
        {{ project.budgetMin || 0 }} - {{ project.budgetMax || 0 }} 元
      </a-descriptions-item>
      <a-descriptions-item label="截止时间">{{ formatDate(project.deadline) }}</a-descriptions-item>
      <a-descriptions-item label="所需技能" :span="2">
        <a-tag v-for="skill in project.skillList" :key="skill" style="margin-right: 8px">
          {{ skill }}
        </a-tag>
        <span v-if="!project.skillList || project.skillList.length === 0">未设置</span>
      </a-descriptions-item>
      <a-descriptions-item label="项目描述" :span="2">
        <div v-if="project.description" v-html="project.description"></div>
        <span v-else style="color: #999">未填写</span>
      </a-descriptions-item>
      <a-descriptions-item label="交付要求" :span="2">
        <div v-if="project.deliveryRequirement" v-html="project.deliveryRequirement"></div>
        <span v-else style="color: #999">未填写</span>
      </a-descriptions-item>
      <a-descriptions-item label="创建时间" :span="2">
        {{ formatDate(project.createdAt) }}
      </a-descriptions-item>
    </a-descriptions>

    <a-divider>项目稿件</a-divider>
    <div v-if="submissions.length > 0" style="margin-bottom: 20px">
      <a-table :columns="submissionColumns" :data-source="submissions" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getSubmissionStatusColor(record.status)">
              {{ getSubmissionStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="link" @click="handleViewSubmission(record.id)">查看详情</a-button>
          </template>
        </template>
      </a-table>
    </div>
    <a-empty v-else description="暂无稿件" />
  </a-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const project = ref(null)
const submissions = ref([])

const submissionColumns = [
  { title: '序号', key: 'index', width: 80, customRender: ({ index }) => index + 1 },
  { title: '稿件标题', dataIndex: 'title', key: 'title' },
  { title: '提交者', dataIndex: 'freelancerName', key: 'freelancerName', customRender: ({ record }) => record.freelancerName || '未知' },
  { title: '报价', dataIndex: 'quotePrice', key: 'quotePrice', customRender: ({ record }) => record.quotePrice ? `¥${record.quotePrice}` : '未报价' },
  { title: '状态', key: 'status', width: 120 },
  { title: '提交时间', dataIndex: 'createdAt', key: 'createdAt', customRender: ({ record }) => formatDate(record.createdAt) },
  { title: '操作', key: 'action', width: 100 }
]

const loadProject = async () => {
  loading.value = true
  try {
    const res = await request.get(`/api/projects/${route.params.id}`)
    if (res.code === '200') {
      project.value = res.data
      // 处理技能标签列表（支持中文逗号和英文逗号）
      if (project.value && project.value.skillsRequired) {
        // 无论后端是否已设置skillList，都重新处理以确保正确分割中文逗号
        project.value.skillList = project.value.skillsRequired.split(/[,，]/).filter(s => s.trim())
      }
    } else {
      message.error(res.msg || '加载项目详情失败')
    }
  } catch (error) {
    console.error('加载项目详情失败:', error)
    message.error('加载项目详情失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}

const loadSubmissions = async () => {
  try {
    const res = await request.get(`/api/projects/${route.params.id}/submissions`)
    if (res.code === '200') {
      submissions.value = res.data || []
    } else {
      message.error(res.msg || '加载稿件列表失败')
    }
  } catch (error) {
    console.error('加载稿件列表失败:', error)
    message.error('加载稿件列表失败')
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  if (typeof dateStr === 'string') {
    return dateStr.replace('T', ' ').substring(0, 19)
  }
  return dateStr
}

const getStatusColor = (status) => {
  const colors = {
    'PUBLISHED': 'green',
    'CLOSED': 'orange',
    'CONFIRMED': 'blue',
    'COMPLETED': 'purple'
  }
  return colors[status] || 'default'
}

const getStatusText = (status) => {
  const texts = {
    'PUBLISHED': '已发布',
    'CLOSED': '已截止',
    'CONFIRMED': '已确定合作',
    'COMPLETED': '已完成'
  }
  return texts[status] || status
}

const getSubmissionStatusColor = (status) => {
  const colors = {
    'SUBMITTED': 'blue',
    'INTERESTED': 'green',
    'REJECTED': 'red',
    'CONFIRMED': 'purple'
  }
  return colors[status] || 'default'
}

const getSubmissionStatusText = (status) => {
  const texts = {
    'SUBMITTED': '已提交',
    'INTERESTED': '有意向',
    'REJECTED': '已拒绝',
    'CONFIRMED': '已确定合作'
  }
  return texts[status] || status
}

const handleBack = () => {
  router.push('/manager/projects')
}

const handleViewSubmission = (id) => {
  router.push(`/manager/submissions/${id}`)
}

onMounted(() => {
  loadProject()
  loadSubmissions()
})
</script>

<style scoped>
.ant-descriptions-item-label {
  font-weight: 500;
}
</style>

