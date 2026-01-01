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
      width="800px"
      :footer="null"
    >
      <a-spin :spinning="loadingFreelancerDetail">
        <div v-if="freelancerDetail" style="padding: 10px 0">
          <div style="display: flex; align-items: center; margin-bottom: 20px">
            <a-avatar :size="80" :src="freelancerDetail.userAvatar" v-if="freelancerDetail.userAvatar">
              {{ freelancerDetail.userName ? freelancerDetail.userName.charAt(0) : '?' }}
            </a-avatar>
            <a-avatar :size="80" v-else>
              <template #icon><UserOutlined /></template>
            </a-avatar>
            <div style="margin-left: 15px">
              <h3 style="margin: 0 0 5px 0">{{ freelancerDetail.userName || '-' }}</h3>
              <div style="display: flex; gap: 8px; flex-wrap: wrap">
                <a-tag :color="freelancerDetail.verified ? 'green' : 'orange'">
                  {{ freelancerDetail.verified ? '已认证' : '未认证' }}
                </a-tag>
                <a-tag :color="getExperienceLevelColor(freelancerDetail.experienceLevel)">
                  {{ getExperienceLevelText(freelancerDetail.experienceLevel) }}
                </a-tag>
                <a-tag color="blue">
                  完成 {{ freelancerDetail.completedProjects || 0 }} 个项目
                </a-tag>
              </div>
            </div>
          </div>
          <a-descriptions :column="1" bordered>
            <a-descriptions-item label="经验等级">
              <a-tag :color="getExperienceLevelColor(freelancerDetail.experienceLevel)">
                {{ getExperienceLevelText(freelancerDetail.experienceLevel) }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="完成项目数量">
              {{ freelancerDetail.completedProjects || 0 }} 个
            </a-descriptions-item>
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
            <a-descriptions-item label="信誉分">
              <a-progress 
                :percent="freelancerDetail.creditScore || 100" 
                :status="freelancerDetail.creditScore >= 80 ? 'success' : freelancerDetail.creditScore >= 60 ? 'normal' : 'exception'"
                :stroke-color="{ '0%': '#00a6a7', '100%': '#00c4c4' }"
              />
            </a-descriptions-item>
          </a-descriptions>
          
          <!-- 完成项目详情 -->
          <div v-if="freelancerProjectHistory.length > 0" style="margin-top: 24px">
            <h4 style="margin-bottom: 12px">完成项目详情</h4>
            <a-list :data-source="freelancerProjectHistory" size="small" bordered>
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-list-item-meta>
                    <template #title>
                      <a @click="handleViewProjectDetail(item.projectId, item)">{{ item.projectTitle }}</a>
                    </template>
                    <template #description>
                      <div style="display: flex; gap: 16px; flex-wrap: wrap; margin-top: 8px">
                        <span>成交价格：<strong style="color: #00a6a7">¥{{ item.projectBudget }}</strong></span>
                        <span>完成时间：{{ formatDate(item.completionDate) }}</span>
                        <span v-if="item.enterpriseRating !== null && item.enterpriseRating !== undefined">
                          企业评分：<a-rate :value="item.enterpriseRating" disabled />
                        </span>
                      </div>
                      <div v-if="item.enterpriseComment" style="margin-top: 8px; color: #666">
                        企业评价：{{ item.enterpriseComment }}
                      </div>
                    </template>
                  </a-list-item-meta>
                </a-list-item>
              </template>
            </a-list>
          </div>
          
          <!-- 证书详情 -->
          <div v-if="freelancerCertificates.length > 0" style="margin-top: 24px">
            <h4 style="margin-bottom: 12px">证书详情</h4>
            <a-list :data-source="freelancerCertificates" :grid="{ gutter: 16, xs: 1, sm: 2, md: 2 }" bordered>
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-card hoverable size="small">
                    <template #cover v-if="item.certificateUrl">
                      <img 
                        :src="item.certificateUrl" 
                        :alt="item.certificateName" 
                        style="height: 120px; object-fit: cover; cursor: pointer" 
                        @click="handlePreviewCertificate(item.certificateUrl)"
                      />
                    </template>
                    <a-card-meta>
                      <template #title>
                        <div style="display: flex; justify-content: space-between; align-items: center">
                          <span>{{ item.certificateName }}</span>
                          <a-tag v-if="item.verified" color="green">已认证</a-tag>
                        </div>
                      </template>
                      <template #description>
                        <div style="font-size: 12px; color: #666">
                          <div v-if="item.certificateType">类型：{{ getCertificateTypeText(item.certificateType) }}</div>
                          <div v-if="item.issuingOrganization">颁发机构：{{ item.issuingOrganization }}</div>
                          <div v-if="item.issueDate">颁发日期：{{ item.issueDate }}</div>
                        </div>
                      </template>
                    </a-card-meta>
                  </a-card>
                </a-list-item>
              </template>
            </a-list>
          </div>
        </div>
      </a-spin>
    </a-modal>
    
    <!-- 证书图片预览 -->
    <a-modal
      v-model:open="certificatePreviewVisible"
      :footer="null"
      width="900px"
      centered
      :mask-closable="true"
    >
      <div style="text-align: center; padding: 20px 0">
        <img 
          :src="previewImageUrl" 
          style="max-width: 100%; max-height: 70vh; object-fit: contain; cursor: zoom-in" 
          alt="证书预览"
          @click="certificatePreviewVisible = false"
        />
      </div>
    </a-modal>
    
    <!-- 项目详情弹窗 -->
    <a-modal
      v-model:open="projectDetailVisible"
      title="项目详情"
      width="800px"
      :footer="null"
    >
      <a-spin :spinning="loadingProjectDetail">
        <div v-if="projectDetail" style="padding: 10px 0">
          <!-- 封面图片 -->
          <div v-if="projectDetail.coverImage" style="margin-bottom: 20px; text-align: center">
            <img 
              :src="projectDetail.coverImage" 
              alt="项目封面" 
              style="max-width: 100%; max-height: 300px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1)"
            />
          </div>
          
          <a-descriptions :column="1" bordered>
            <a-descriptions-item label="项目标题">
              <strong style="font-size: 16px">{{ projectDetail.title }}</strong>
            </a-descriptions-item>
            
            <!-- 成交价格（从项目历史记录中获取） -->
            <a-descriptions-item v-if="currentProjectHistory" label="成交价格">
              <span style="color: #00a6a7; font-size: 18px; font-weight: bold">
                ¥{{ currentProjectHistory.projectBudget }}
              </span>
            </a-descriptions-item>
            
            <a-descriptions-item label="预算范围">
              ¥{{ projectDetail.budgetMin }} - ¥{{ projectDetail.budgetMax }}
            </a-descriptions-item>
            
            <a-descriptions-item label="项目描述">
              <div v-html="projectDetail.description" style="line-height: 1.8"></div>
            </a-descriptions-item>
            
            <a-descriptions-item v-if="projectDetail.requirementDetails" label="详细需求说明">
              <div v-html="projectDetail.requirementDetails" style="line-height: 1.8; max-height: 300px; overflow-y: auto"></div>
            </a-descriptions-item>
            
            <a-descriptions-item v-if="projectDetail.deliveryRequirement" label="交付要求">
              <div style="line-height: 1.8; white-space: pre-wrap">{{ projectDetail.deliveryRequirement }}</div>
            </a-descriptions-item>
            
            <a-descriptions-item label="所需技能">
              <a-tag 
                v-for="skill in (projectDetail.skillsRequired ? projectDetail.skillsRequired.split(/[,，]/).filter(s => s.trim()) : [])" 
                :key="skill" 
                color="blue"
                style="margin-right: 8px; margin-bottom: 4px"
              >
                {{ skill }}
              </a-tag>
              <span v-if="!projectDetail.skillsRequired">-</span>
            </a-descriptions-item>
            
            <a-descriptions-item label="难度等级">
              <a-tag :color="getDifficultyColor(projectDetail.difficultyLevel)">
                {{ getDifficultyText(projectDetail.difficultyLevel) }}
              </a-tag>
            </a-descriptions-item>
            
            <a-descriptions-item label="项目类型">
              <a-tag>{{ getProjectTypeText(projectDetail.projectType) }}</a-tag>
            </a-descriptions-item>
            
            <a-descriptions-item label="经验偏好">
              <a-tag :color="projectDetail.preferredExperience === 'NEWBIE' ? 'green' : projectDetail.preferredExperience === 'EXPERIENCED' ? 'orange' : 'blue'">
                {{ projectDetail.preferredExperience === 'NEWBIE' ? '新手' : projectDetail.preferredExperience === 'EXPERIENCED' ? '老手' : '不限' }}
              </a-tag>
            </a-descriptions-item>
            
            <a-descriptions-item label="优先级">
              <a-tag :color="projectDetail.priority === 'HIGH' ? 'red' : projectDetail.priority === 'MEDIUM' ? 'orange' : 'default'">
                {{ projectDetail.priority === 'HIGH' ? '高' : projectDetail.priority === 'MEDIUM' ? '中' : '低' }}
              </a-tag>
            </a-descriptions-item>
            
            <a-descriptions-item label="截止时间">
              {{ formatDate(projectDetail.deadline) }}
            </a-descriptions-item>
            
            <a-descriptions-item v-if="projectDetail.deliveryDeadline" label="交付截止时间">
              {{ formatDate(projectDetail.deliveryDeadline) }}
            </a-descriptions-item>
            
            <!-- 项目历史记录中的评价信息 -->
            <a-descriptions-item v-if="currentProjectHistory && currentProjectHistory.enterpriseRating" label="企业评分">
              <a-rate :value="currentProjectHistory.enterpriseRating" disabled />
              <span style="margin-left: 8px; color: #666">
                ({{ currentProjectHistory.enterpriseRating }}分)
              </span>
            </a-descriptions-item>
            
            <a-descriptions-item v-if="currentProjectHistory && currentProjectHistory.enterpriseComment" label="企业评价">
              <div style="line-height: 1.8; color: #666; padding: 8px; background: #f5f5f5; border-radius: 4px">
                {{ currentProjectHistory.enterpriseComment }}
              </div>
            </a-descriptions-item>
            
            <a-descriptions-item v-if="currentProjectHistory && currentProjectHistory.completionDate" label="完成时间">
              {{ formatDate(currentProjectHistory.completionDate) }}
            </a-descriptions-item>
          </a-descriptions>
        </div>
      </a-spin>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { ArrowLeftOutlined, UserOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'
import dayjs from 'dayjs'

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
  if (!dateStr) return '-'
  if (typeof dateStr === 'string' && dateStr.includes('T')) {
    return dateStr.replace('T', ' ').substring(0, 19)
  }
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm')
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
const loadingFreelancerDetail = ref(false)
const freelancerCertificates = ref([])
const freelancerProjectHistory = ref([])
const certificatePreviewVisible = ref(false)
const previewImageUrl = ref('')
const projectDetailVisible = ref(false)
const projectDetail = ref(null)
const loadingProjectDetail = ref(false)
const currentProjectHistory = ref(null) // 当前查看的项目历史记录

const handleViewFreelancer = async (freelancerId) => {
  if (!freelancerId) {
    message.warning('提交者信息不存在')
    return
  }
  freelancerModalVisible.value = true
  loadingFreelancerDetail.value = true
  try {
    // 加载自由职业者基本信息
    const res = await request.get(`/api/freelancers/${freelancerId}`)
    if (res.code === '200' && res.data) {
      freelancerDetail.value = res.data
      
      // 加载证书信息
      try {
        const certRes = await request.get(`/api/certificates/freelancer/${freelancerId}`)
        if (certRes.code === '200' && certRes.data) {
          freelancerCertificates.value = certRes.data.filter(c => c.verified) // 只显示已认证的证书
        }
      } catch (error) {
        console.error('加载证书失败:', error)
        freelancerCertificates.value = []
      }
      
      // 加载项目历史
      try {
        const historyRes = await request.get(`/api/project-history/freelancer/${freelancerId}`)
        if (historyRes.code === '200' && historyRes.data) {
          freelancerProjectHistory.value = historyRes.data
        }
      } catch (error) {
        console.error('加载项目历史失败:', error)
        freelancerProjectHistory.value = []
      }
    } else {
      message.error(res.msg || '加载自由职业者信息失败')
    }
  } catch (error) {
    console.error('加载自由职业者信息失败:', error)
    message.error('加载自由职业者信息失败')
  } finally {
    loadingFreelancerDetail.value = false
  }
}

const handlePreviewCertificate = (imageUrl) => {
  previewImageUrl.value = imageUrl
  certificatePreviewVisible.value = true
}

const handleViewProjectDetail = async (projectId, projectHistory = null) => {
  if (!projectId) {
    message.warning('项目ID不存在')
    return
  }
  // 保存当前项目历史记录，用于显示成交价格和评价信息
  currentProjectHistory.value = projectHistory
  projectDetailVisible.value = true
  loadingProjectDetail.value = true
  try {
    const res = await request.get(`/api/projects/${projectId}`)
    if (res.code === '200' && res.data) {
      projectDetail.value = res.data
    } else {
      message.error(res.msg || '加载项目详情失败')
    }
  } catch (error) {
    console.error('加载项目详情失败:', error)
    message.error('加载项目详情失败')
  } finally {
    loadingProjectDetail.value = false
  }
}

const getExperienceLevelText = (level) => {
  if (!level) return '未知'
  const texts = {
    'NEWBIE': '新手',
    'JUNIOR': '初级',
    'SENIOR': '高级',
    'EXPERT': '专家'
  }
  return texts[level] || level
}

const getExperienceLevelColor = (level) => {
  const colors = {
    'NEWBIE': 'orange',
    'JUNIOR': 'blue',
    'SENIOR': 'purple',
    'EXPERT': 'red'
  }
  return colors[level] || 'default'
}

const getCertificateTypeText = (type) => {
  if (!type) return '-'
  const texts = {
    'DEGREE': '学历证书',
    'PROFESSIONAL': '职业证书',
    'AWARD': '获奖证书',
    'OTHER': '其他'
  }
  return texts[type] || type
}

const getDifficultyText = (level) => {
  if (!level) return '-'
  const texts = {
    'EASY': '简单',
    'MEDIUM': '中等',
    'HARD': '困难'
  }
  return texts[level] || level
}

const getDifficultyColor = (level) => {
  const colors = {
    'EASY': 'green',
    'MEDIUM': 'orange',
    'HARD': 'red'
  }
  return colors[level] || 'default'
}

const getProjectTypeText = (type) => {
  if (!type) return '-'
  const texts = {
    'WEB': '网站开发',
    'MOBILE': '移动应用',
    'DESIGN': '设计',
    'OTHER': '其他'
  }
  return texts[type] || type
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

