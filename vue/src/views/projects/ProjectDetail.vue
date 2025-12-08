<template>
  <div class="project-detail-container">
    <a-card v-if="project" :loading="loading">
      <template #title>
        <h2>{{ project.title }}</h2>
      </template>
      
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="发布企业">
          <a-button type="link" @click="handleViewEnterprise" style="padding: 0">
            {{ project.enterpriseName }}
          </a-button>
        </a-descriptions-item>
        <a-descriptions-item label="项目状态">
          <a-tag :color="getStatusColor(project.status)">{{ getStatusText(project.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="预算范围">
          {{ project.budgetMin }} - {{ project.budgetMax }} 元
        </a-descriptions-item>
        <a-descriptions-item label="已支付保证金" v-if="project.paidAmount">
          <span style="color: #52c41a; font-weight: bold">¥{{ project.paidAmount }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="截止时间">{{ project.deadline }}</a-descriptions-item>
        <a-descriptions-item label="接单人数">
          {{ project.orderCount || 0 }} 人
        </a-descriptions-item>
        <a-descriptions-item label="提交稿件数">
          {{ project.submissionCount || 0 }} 份
        </a-descriptions-item>
        <a-descriptions-item label="所需技能" :span="2">
          <a-tag v-for="skill in project.skillList" :key="skill" style="margin-right: 8px">
            {{ skill }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="项目描述" :span="2">
          <div v-html="project.description"></div>
        </a-descriptions-item>
        <a-descriptions-item label="交付要求" :span="2">
          <div v-html="project.deliveryRequirement"></div>
        </a-descriptions-item>
      </a-descriptions>

      <a-divider v-if="userRole === 'EMPLOY'">项目稿件</a-divider>
      <div v-if="userRole === 'EMPLOY' && submissions.length > 0" style="margin-bottom: 20px">
        <a-list :data-source="submissions">
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta>
                <template #title>
                  <a @click="handleViewSubmission(item.id)">{{ item.title }}</a>
                  <a-tag :color="getSubmissionStatusColor(item.status)" style="margin-left: 8px">
                    {{ getSubmissionStatusText(item.status) }}
                  </a-tag>
                </template>
                <template #description>
                  <div>提交者：{{ item.freelancerName }}</div>
                  <div>报价：{{ item.quotePrice ? `¥${item.quotePrice}` : '未报价' }}</div>
                  <div>提交时间：{{ formatDate(item.createdAt) }}</div>
                </template>
              </a-list-item-meta>
            </a-list-item>
          </template>
        </a-list>
      </div>
      <a-empty v-if="userRole === 'EMPLOY' && submissions.length === 0" description="暂无稿件" />

      <div style="margin-top: 20px; text-align: right">
        <a-button v-if="userRole === 'USER' && project && project.status === 'PUBLISHED'" type="primary" @click="handleSubmit">提交稿件</a-button>
        <a-button style="margin-left: 8px" @click="handleBack">返回</a-button>
      </div>
    </a-card>

    <!-- 企业详情弹窗 -->
    <a-modal
      v-model:open="enterpriseModalVisible"
      title="企业详情"
      width="600px"
      :footer="null"
    >
      <div v-if="enterpriseDetail" style="padding: 10px 0">
        <div style="display: flex; align-items: center; margin-bottom: 20px">
          <a-avatar :size="60" :src="enterpriseDetail.employAvatar" v-if="enterpriseDetail.employAvatar">
            <template #icon><UserOutlined /></template>
          </a-avatar>
          <a-avatar :size="60" v-else>
            <template #icon><UserOutlined /></template>
          </a-avatar>
          <div style="margin-left: 15px">
            <h3 style="margin: 0 0 5px 0">{{ enterpriseDetail.employName || '-' }}</h3>
            <a-tag :color="enterpriseDetail.verified ? 'green' : 'orange'">
              {{ enterpriseDetail.verified ? '已认证' : '未认证' }}
            </a-tag>
          </div>
        </div>
        <a-descriptions :column="1" bordered>
          <a-descriptions-item label="企业名称">
            {{ enterpriseDetail.employName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="所在城市">
            {{ enterpriseDetail.employCity || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="企业地址">
            {{ enterpriseDetail.employAddress || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="营业执照">
            {{ enterpriseDetail.businessLicense || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="认证状态">
            <a-tag :color="enterpriseDetail.verified ? 'green' : 'orange'">
              {{ enterpriseDetail.verified ? '已认证' : '未认证' }}
            </a-tag>
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const project = ref(null)
const submissions = ref([])
const userRole = ref(localStorage.getItem('xm-user') ? JSON.parse(localStorage.getItem('xm-user')).role : '')

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

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 19)
}

const loadSubmissions = async () => {
  if (userRole.value === 'EMPLOY') {
    try {
      const res = await request.get(`/api/projects/${route.params.id}/submissions`)
      if (res.code === '200') {
        submissions.value = res.data || []
      }
    } catch (error) {
      console.error('加载稿件列表失败', error)
    }
  }
}

const handleViewSubmission = (id) => {
  router.push(`/front/submissions/${id}`)
}

const handleSubmit = () => {
  router.push(`/front/submissions/submit/${route.params.id}`)
}

const handleBack = () => {
  router.back()
}

const enterpriseDetail = ref(null)
const enterpriseModalVisible = ref(false)

const handleViewEnterprise = async () => {
  if (project.value && project.value.enterpriseId) {
    try {
      const res = await request.get(`/api/enterprises/${project.value.enterpriseId}`)
      if (res.code === '200' && res.data) {
        enterpriseDetail.value = res.data
        enterpriseModalVisible.value = true
      } else {
        message.error(res.msg || '加载企业信息失败')
      }
    } catch (error) {
      console.error('加载企业信息失败:', error)
      message.error('加载企业信息失败')
    }
  }
}

onMounted(() => {
  loadProject()
  loadSubmissions()
})
</script>

<style scoped>
.project-detail-container {
  padding: 20px;
}
</style>

