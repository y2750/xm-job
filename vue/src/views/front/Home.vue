<template>
  <div class="home-container">
    <!-- 顶部搜索栏 -->
    <div class="search-header">
      <div class="search-content">
        <div class="search-wrapper">
          <a-input-search
            v-model:value="searchForm.title"
            placeholder="搜索项目、企业、技能标签..."
            size="large"
            class="search-input"
            @search="handleSearch"
          >
            <template #enterButton>
              <a-button type="primary" class="search-btn">
                <SearchOutlined />
                搜索
              </a-button>
            </template>
          </a-input-search>
        </div>
        <div class="hot-tags">
          <span class="hot-label">热门：</span>
          <a-tag class="hot-tag" @click="quickSearch('Java')">Java</a-tag>
          <a-tag class="hot-tag" @click="quickSearch('Python')">Python</a-tag>
          <a-tag class="hot-tag" @click="quickSearch('前端')">前端</a-tag>
          <a-tag class="hot-tag" @click="quickSearch('设计')">设计</a-tag>
          <a-tag class="hot-tag" @click="quickSearch('运营')">运营</a-tag>
        </div>
      </div>
    </div>

    <!-- 筛选条件栏 -->
    <div class="filter-bar">
      <div class="filter-content">
        <a-form :model="searchForm" layout="inline" class="filter-form">
          <a-form-item label="关键词">
            <a-input 
              v-model:value="searchForm.title" 
              placeholder="项目标题" 
              allow-clear 
              style="width: 160px" 
            />
          </a-form-item>
          <a-form-item label="技能标签">
            <a-input 
              v-model:value="searchForm.skillsRequired" 
              placeholder="技能标签" 
              allow-clear 
              style="width: 160px" 
            />
          </a-form-item>
          <a-form-item label="发布企业">
            <a-select
              v-model:value="searchForm.enterpriseId"
              placeholder="选择企业"
              allow-clear
              show-search
              :filter-option="false"
              :options="enterpriseOptions"
              style="width: 160px"
              @search="handleEnterpriseSearch"
              @change="handleEnterpriseChange"
            >
            </a-select>
          </a-form-item>
          <a-form-item label="项目类型">
            <a-select 
              v-model:value="searchForm.projectType" 
              placeholder="项目类型" 
              allow-clear 
              style="width: 130px"
            >
              <a-select-option value="WEB">网站开发</a-select-option>
              <a-select-option value="MOBILE">移动应用</a-select-option>
              <a-select-option value="DESIGN">设计</a-select-option>
              <a-select-option value="OTHER">其他</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="难度等级">
            <a-select 
              v-model:value="searchForm.difficultyLevel" 
              placeholder="难度等级" 
              allow-clear 
              style="width: 130px"
            >
              <a-select-option value="EASY">简单</a-select-option>
              <a-select-option value="MEDIUM">中等</a-select-option>
              <a-select-option value="HARD">困难</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="状态">
            <a-select 
              v-model:value="searchForm.status" 
              placeholder="项目状态" 
              allow-clear 
              style="width: 130px"
            >
              <a-select-option value="PUBLISHED">已发布</a-select-option>
              <a-select-option value="CLOSED">已截止</a-select-option>
              <a-select-option value="CONFIRMED">已确定合作</a-select-option>
              <a-select-option value="COMPLETED">已完成</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="handleSearch">
                <SearchOutlined />
                搜索
              </a-button>
              <a-button @click="handleReset">
                <ReloadOutlined />
                清空
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>
    </div>

    <!-- 主内容区域：左右分栏 -->
    <div class="main-content">
      <!-- 左侧：项目列表 -->
      <div class="left-panel">
        <div class="panel-header">
          <h3 class="panel-title">项目列表</h3>
          <span class="panel-count">共 {{ pagination.total }} 个项目</span>
        </div>
        <div class="project-list-scroll">
          <a-spin :spinning="loading">
            <div v-if="projectList.length > 0" class="project-list">
              <div
                v-for="item in projectList"
                :key="item.id"
                class="project-card"
                :class="{ active: selectedProjectId === item.id }"
                @click="handleSelectProject(item)"
              >
                <!-- 封面图 -->
                <div v-if="item.coverImage" class="project-cover">
                  <img :src="item.coverImage" :alt="item.title" />
                </div>
                <div class="project-card-header">
                  <h3 class="project-title">{{ item.title }}</h3>
                  <div class="project-budget">
                    <span class="budget-text">{{ item.budgetMin || 0 }} - {{ item.budgetMax || 0 }}</span>
                    <span class="budget-unit">元</span>
                  </div>
                </div>
                <!-- 项目类型和难度标签 -->
                <div class="project-meta-tags">
                  <a-tag v-if="item.projectType" :color="getProjectTypeColor(item.projectType)">
                    {{ getProjectTypeText(item.projectType) }}
                  </a-tag>
                  <a-tag v-if="item.difficultyLevel" :color="getDifficultyColor(item.difficultyLevel)">
                    {{ getDifficultyText(item.difficultyLevel) }}
                  </a-tag>
                  <a-tag v-if="item.preferredExperience && item.preferredExperience !== 'BOTH'" color="orange">
                    {{ item.preferredExperience === 'NEWBIE' ? '偏向新手' : '偏向老手' }}
                  </a-tag>
                </div>
                <div class="project-tags">
                  <a-tag 
                    v-for="skill in (item.skillList || []).slice(0, 4)" 
                    :key="skill" 
                    class="skill-tag"
                  >
                    {{ skill }}
                  </a-tag>
                  <a-tag v-if="(item.skillList || []).length > 4" class="skill-tag more">
                    +{{ (item.skillList || []).length - 4 }}
                  </a-tag>
                </div>
                <div class="project-footer">
                  <div class="project-company">
                    <span class="company-name">{{ item.enterpriseName || '未知企业' }}</span>
                    <a-tag :color="getStatusColor(item.status)" class="status-tag">
                      {{ getStatusText(item.status) }}
                    </a-tag>
                  </div>
                  <div class="project-deadline">
                    <ClockCircleOutlined />
                    <span>截止：{{ formatDate(item.deadline) }}</span>
                  </div>
                </div>
                <div class="project-actions" v-if="userRole === 'USER' && item.status === 'PUBLISHED'">
                  <a-button
                    v-if="!item.hasAccepted"
                    type="primary"
                    size="small"
                    @click.stop="handleAcceptOrder(item.id)"
                  >
                    立即接单
                  </a-button>
                  <a-button
                    v-if="item.hasAccepted && !item.hasSubmission"
                    type="primary"
                    ghost
                    size="small"
                    @click.stop="handleSubmit(item.id)"
                  >
                    提交稿件
                  </a-button>
                  <a-button
                    v-if="item.hasAccepted && item.hasSubmission"
                    type="link"
                    size="small"
                    @click.stop="handleViewSubmission(item.id)"
                  >
                    查看稿件
                  </a-button>
                </div>
              </div>
            </div>
            <a-empty v-else :description="loading ? '加载中...' : '暂无项目数据'" class="empty-state" />
          </a-spin>
        </div>
        
        <!-- 分页 -->
        <div v-if="projectList.length > 0" class="pagination-container">
          <a-pagination
            v-model:current="pagination.current"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :show-total="pagination.showTotal"
            :page-size-options="['10', '20', '50']"
            show-size-changer
            size="small"
            @change="handlePageChange"
            @show-size-change="handlePageChange"
          />
        </div>
      </div>

      <!-- 右侧：项目详情 -->
      <div class="right-panel">
        <div class="detail-scroll">
          <div class="detail-container">
            <a-spin :spinning="detailLoading">
              <div v-if="selectedProject" class="project-detail">
                <div class="detail-header">
                  <h2 class="detail-title">{{ selectedProject.title }}</h2>
                  <div class="detail-budget">
                    <span class="budget-amount">{{ selectedProject.budgetMin || 0 }} - {{ selectedProject.budgetMax || 0 }}</span>
                    <span class="budget-suffix">元/项目</span>
                  </div>
                  <div class="detail-meta">
                    <a-tag :color="getStatusColor(selectedProject.status)" class="meta-tag">
                      {{ getStatusText(selectedProject.status) }}
                    </a-tag>
                    <a-button type="link" @click="handleViewEnterprise" class="company-link">
                      <BankOutlined />
                      {{ selectedProject.enterpriseName || '未知企业' }}
                    </a-button>
                    <span class="meta-divider">|</span>
                    <span class="meta-deadline">
                      <ClockCircleOutlined />
                      截止：{{ formatDate(selectedProject.deadline) }}
                    </span>
                  </div>
                  <div class="detail-actions">
                    <a-button
                      v-if="userRole === 'USER' && selectedProject.status === 'PUBLISHED'"
                      type="primary"
                      size="large"
                      class="action-btn chat-btn"
                      @click="handleStartChat(selectedProject)"
                    >
                      <MessageOutlined />
                      立即沟通
                    </a-button>
                    <a-button
                      v-if="userRole === 'USER' && selectedProject.status === 'PUBLISHED' && !selectedProject.hasAccepted"
                      type="primary"
                      size="large"
                      class="action-btn"
                      @click="handleAcceptOrder(selectedProject.id)"
                    >
                      <CheckOutlined />
                      立即接单
                    </a-button>
                    <a-button
                      v-if="userRole === 'USER' && selectedProject.status === 'PUBLISHED' && selectedProject.hasAccepted && !selectedProject.hasSubmission"
                      size="large"
                      class="action-btn submit-btn"
                      @click="handleSubmit(selectedProject.id)"
                    >
                      <UploadOutlined />
                      提交稿件
                    </a-button>
                    <a-button
                      v-if="userRole === 'USER' && selectedProject.status === 'PUBLISHED' && selectedProject.hasAccepted && selectedProject.hasSubmission"
                      size="large"
                      class="action-btn"
                      @click="handleViewSubmission(selectedProject.id)"
                    >
                      <EyeOutlined />
                      查看稿件
                    </a-button>
                  </div>
                </div>

                <a-divider />

                <!-- 封面图 -->
                <div v-if="selectedProject.coverImage" class="detail-cover-image">
                  <img :src="selectedProject.coverImage" :alt="selectedProject.title" />
                </div>

                <!-- 项目类型和难度 -->
                <div class="detail-section">
                  <h3 class="section-title">
                    <TagsOutlined />
                    项目信息
                  </h3>
                  <div class="project-info-tags">
                    <a-tag v-if="selectedProject.projectType" :color="getProjectTypeColor(selectedProject.projectType)">
                      <AppstoreOutlined /> {{ getProjectTypeText(selectedProject.projectType) }}
                    </a-tag>
                    <a-tag v-if="selectedProject.difficultyLevel" :color="getDifficultyColor(selectedProject.difficultyLevel)">
                      <ThunderboltOutlined /> {{ getDifficultyText(selectedProject.difficultyLevel) }}
                    </a-tag>
                    <a-tag v-if="selectedProject.preferredExperience && selectedProject.preferredExperience !== 'BOTH'" color="orange">
                      <UserOutlined /> {{ selectedProject.preferredExperience === 'NEWBIE' ? '偏向新手' : '偏向老手' }}
                    </a-tag>
                  </div>
                </div>

                <div class="detail-section">
                  <h3 class="section-title">
                    <TagsOutlined />
                    所需技能
                  </h3>
                  <div class="skill-tags-large">
                    <a-tag 
                      v-for="skill in (selectedProject.skillList || [])" 
                      :key="skill" 
                      class="skill-tag-item"
                    >
                      {{ skill }}
                    </a-tag>
                    <span v-if="!selectedProject.skillList || selectedProject.skillList.length === 0" class="no-data">
                      暂无要求
                    </span>
                  </div>
                </div>

                <div class="detail-section">
                  <h3 class="section-title">
                    <FileTextOutlined />
                    项目描述
                  </h3>
                  <div class="section-content" v-html="selectedProject.description || '暂无描述'"></div>
                </div>

                <div class="detail-section" v-if="selectedProject.requirementDetails">
                  <h3 class="section-title">
                    <FileTextOutlined />
                    详细需求说明
                  </h3>
                  <div class="section-content" v-html="selectedProject.requirementDetails"></div>
                </div>

                <!-- 项目附件 -->
                <div class="detail-section" v-if="projectAttachments.length > 0">
                  <h3 class="section-title">
                    <PaperClipOutlined />
                    项目附件
                  </h3>
                  <div class="attachment-list">
                    <a-list :data-source="projectAttachments" size="small">
                      <template #renderItem="{ item }">
                        <a-list-item>
                          <a-list-item-meta>
                            <template #title>
                              <a :href="item.fileUrl" target="_blank" :download="item.fileName">
                                <FileOutlined /> {{ item.fileName }}
                              </a>
                            </template>
                            <template #description>
                              <span style="color: #999; font-size: 12px">
                                {{ formatFileSize(item.fileSize) }} | {{ item.fileType }}
                              </span>
                            </template>
                          </a-list-item-meta>
                        </a-list-item>
                      </template>
                    </a-list>
                  </div>
                </div>

                <div class="detail-section" v-if="selectedProject.deliveryRequirement">
                  <h3 class="section-title">
                    <ProfileOutlined />
                    交付要求
                  </h3>
                  <div class="section-content" v-html="selectedProject.deliveryRequirement"></div>
                </div>

                <div class="detail-section">
                  <h3 class="section-title">
                    <InfoCircleOutlined />
                    项目信息
                  </h3>
                  <div class="info-grid">
                    <div class="info-item">
                      <span class="info-label">发布企业</span>
                      <a-button type="link" @click="handleViewEnterprise" class="info-link">
                        {{ selectedProject.enterpriseName }}
                      </a-button>
                    </div>
                    <div class="info-item">
                      <span class="info-label">项目状态</span>
                      <a-tag :color="getStatusColor(selectedProject.status)">
                        {{ getStatusText(selectedProject.status) }}
                      </a-tag>
                    </div>
                    <div class="info-item">
                      <span class="info-label">预算范围</span>
                      <span class="info-value budget">{{ selectedProject.budgetMin }} - {{ selectedProject.budgetMax }} 元</span>
                    </div>
                    <div class="info-item">
                      <span class="info-label">截止时间</span>
                      <span class="info-value">{{ formatDate(selectedProject.deadline) }}</span>
                    </div>
                    <div class="info-item" v-if="selectedProject.orderCount !== undefined">
                      <span class="info-label">接单人数</span>
                      <span class="info-value">{{ selectedProject.orderCount || 0 }} 人</span>
                    </div>
                    <div class="info-item" v-if="selectedProject.submissionCount !== undefined">
                      <span class="info-label">提交稿件</span>
                      <span class="info-value">{{ selectedProject.submissionCount || 0 }} 份</span>
                    </div>
                  </div>
                </div>
              </div>
              <div v-else class="empty-detail">
                <a-empty description="请从左侧选择一个项目查看详情">
                  <template #image>
                    <FileSearchOutlined class="empty-icon" />
                  </template>
                </a-empty>
              </div>
            </a-spin>
          </div>
        </div>
      </div>
    </div>

    <!-- 企业详情弹窗 -->
    <a-modal
      v-model:open="enterpriseModalVisible"
      title="企业详情"
      width="560px"
      :footer="null"
      class="enterprise-modal"
    >
      <div v-if="enterpriseDetail" class="enterprise-info">
        <div class="enterprise-header">
          <a-avatar :size="72" :src="enterpriseDetail.employAvatar" v-if="enterpriseDetail.employAvatar">
            <template #icon><UserOutlined /></template>
          </a-avatar>
          <a-avatar :size="72" v-else class="default-avatar">
            <template #icon><BankOutlined /></template>
          </a-avatar>
          <div class="enterprise-basic">
            <h3>{{ enterpriseDetail.employName || '-' }}</h3>
            <a-tag :color="enterpriseDetail.verified ? 'success' : 'warning'">
              {{ enterpriseDetail.verified ? '已认证' : '未认证' }}
            </a-tag>
          </div>
        </div>
        <a-descriptions :column="1" bordered class="enterprise-descriptions">
          <a-descriptions-item label="企业名称">
            {{ enterpriseDetail.employName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="所在城市">
            {{ enterpriseDetail.employCity || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="企业地址">
            {{ enterpriseDetail.employAddress || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="认证状态">
            <a-tag :color="enterpriseDetail.verified ? 'success' : 'warning'">
              {{ enterpriseDetail.verified ? '已认证' : '未认证' }}
            </a-tag>
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { 
  UserOutlined, 
  SearchOutlined, 
  ReloadOutlined, 
  ClockCircleOutlined,
  BankOutlined,
  MessageOutlined,
  CheckOutlined,
  UploadOutlined,
  EyeOutlined,
  TagsOutlined,
  FileTextOutlined,
  ProfileOutlined,
  InfoCircleOutlined,
  FileSearchOutlined,
  AppstoreOutlined,
  ThunderboltOutlined,
  PaperClipOutlined,
  FileOutlined
} from '@ant-design/icons-vue'
import request from '@/utils/request'

const router = useRouter()

const loading = ref(false)
const detailLoading = ref(false)
const projectList = ref([])
const selectedProjectId = ref(null)
const selectedProject = ref(null)
const userRole = ref(localStorage.getItem('xm-user') ? JSON.parse(localStorage.getItem('xm-user')).role : '')
const myOrders = ref([])
const mySubmissions = ref([])
const enterpriseList = ref([])
const enterpriseOptions = ref([])
const enterpriseSearchKeyword = ref('')
const enterpriseDetail = ref(null)
const enterpriseModalVisible = ref(false)
const projectAttachments = ref([])

const searchForm = reactive({
  title: '',
  skillsRequired: '',
  status: '',
  enterpriseId: undefined,
  enterpriseName: '',
  projectType: undefined,
  difficultyLevel: undefined
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

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 10)
}

const quickSearch = (keyword) => {
  searchForm.skillsRequired = keyword
  handleSearch()
}

const loadProjects = async () => {
  loading.value = true
  try {
    const params = {
      title: searchForm.title,
      skillsRequired: searchForm.skillsRequired,
      status: searchForm.status,
      projectType: searchForm.projectType,
      difficultyLevel: searchForm.difficultyLevel,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    if (searchForm.enterpriseId) {
      params.enterpriseId = searchForm.enterpriseId
    } else if (searchForm.enterpriseName) {
      params.enterpriseName = searchForm.enterpriseName
    }
    
    const res = await request.get('/api/projects', { params })
    if (res.code === '200') {
      const data = res.data
      if (data) {
        projectList.value = data.list || []
        pagination.total = data.total || 0
        projectList.value.forEach(item => {
          if (item.skillsRequired && !item.skillList) {
            item.skillList = item.skillsRequired.split(/[,，]/).filter(s => s.trim())
          }
          item.hasAccepted = myOrders.value.some(order => order.projectId === item.id && order.status === 'ACCEPTED')
          const submission = mySubmissions.value.find(sub => 
            sub.projectId === item.id && 
            (sub.status === 'SUBMITTED' || sub.status === 'INTERESTED' || sub.status === 'CONFIRMED')
          )
          item.hasSubmission = !!submission
          if (submission) {
            item.mySubmission = submission
            item.canChat = ['SUBMITTED', 'INTERESTED', 'CONFIRMED'].includes(submission.status)
          } else {
            item.canChat = false
          }
        })
      } else {
        projectList.value = []
        pagination.total = 0
      }
    } else {
      message.error(res.msg || '加载项目列表失败')
      projectList.value = []
      pagination.total = 0
    }
  } catch (error) {
    console.error('加载项目列表失败:', error)
    message.error('加载项目列表失败，请检查网络连接')
    projectList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const loadEnterprises = async (keyword = '') => {
  try {
    const params = {
      pageNum: 1,
      pageSize: 1000
    }
    const res = await request.get('/api/enterprises', { params })
    if (res.code === '200' && res.data) {
      let list = res.data.list || []
      if (keyword) {
        list = list.filter(e => 
          e.employName && e.employName.toLowerCase().includes(keyword.toLowerCase())
        )
      }
      enterpriseList.value = list
      enterpriseOptions.value = list.map(e => ({
        value: e.id,
        label: e.employName || `企业${e.id}`
      }))
    }
  } catch (error) {
    console.error('加载企业列表失败:', error)
  }
}

const handleEnterpriseSearch = (value) => {
  enterpriseSearchKeyword.value = value
  loadEnterprises(value)
}

const handleEnterpriseChange = (value) => {
  if (value) {
    searchForm.enterpriseName = ''
    enterpriseSearchKeyword.value = ''
  } else {
    searchForm.enterpriseName = ''
    enterpriseSearchKeyword.value = ''
  }
}

const handleSearch = () => {
  if (enterpriseSearchKeyword.value && !searchForm.enterpriseId) {
    searchForm.enterpriseName = enterpriseSearchKeyword.value
  } else if (!searchForm.enterpriseId) {
    searchForm.enterpriseName = ''
  }
  pagination.current = 1
  selectedProjectId.value = null
  selectedProject.value = null
  loadProjects()
}

const handleReset = () => {
  Object.assign(searchForm, {
    title: '',
    skillsRequired: '',
    status: '',
    enterpriseId: undefined,
    enterpriseName: '',
    projectType: undefined,
    difficultyLevel: undefined
  })
  enterpriseSearchKeyword.value = ''
  selectedProjectId.value = null
  selectedProject.value = null
  loadEnterprises()
  handleSearch()
}

const handleSelectProject = async (project) => {
  if (selectedProjectId.value === project.id) {
    selectedProjectId.value = null
    selectedProject.value = null
    return
  }
  
  selectedProjectId.value = project.id
  if (project.description && project.deliveryRequirement) {
    selectedProject.value = project
  } else {
    await loadProjectDetail(project.id)
  }
}

const loadProjectDetail = async (id) => {
  detailLoading.value = true
  try {
    const res = await request.get(`/api/projects/${id}`)
    if (res.code === '200') {
      const project = res.data
      if (project && project.skillsRequired) {
        project.skillList = project.skillsRequired.split(/[,，]/).filter(s => s.trim())
      }
      project.hasAccepted = myOrders.value.some(order => order.projectId === project.id && order.status === 'ACCEPTED')
      project.hasSubmission = mySubmissions.value.some(sub => 
        sub.projectId === project.id && 
        (sub.status === 'SUBMITTED' || sub.status === 'INTERESTED' || sub.status === 'CONFIRMED')
      )
      
      if (userRole.value === 'USER' && project.hasAccepted && project.hasSubmission) {
        try {
          const submissionRes = await request.get(`/api/submissions/project/${id}/my`)
          if (submissionRes.code === '200' && submissionRes.data) {
            project.mySubmission = submissionRes.data
            project.canChat = ['SUBMITTED', 'INTERESTED', 'CONFIRMED'].includes(submissionRes.data.status)
          } else {
            project.canChat = false
          }
        } catch (error) {
          console.error('加载我的稿件失败:', error)
          project.canChat = false
        }
      } else {
        project.canChat = false
      }
      
      selectedProject.value = project
      
      // 加载项目附件
      await loadProjectAttachments(id)
    } else {
      message.error(res.msg || '加载项目详情失败')
    }
  } catch (error) {
    console.error('加载项目详情失败:', error)
    message.error('加载项目详情失败，请检查网络连接')
  } finally {
    detailLoading.value = false
  }
}

const loadProjectAttachments = async (projectId) => {
  try {
    const res = await request.get(`/api/projects/${projectId}/attachments`)
    if (res.code === '200') {
      projectAttachments.value = res.data || []
    }
  } catch (error) {
    console.error('加载项目附件失败:', error)
    projectAttachments.value = []
  }
}

const getProjectTypeColor = (type) => {
  const colors = {
    'WEB': 'blue',
    'MOBILE': 'green',
    'DESIGN': 'purple',
    'OTHER': 'default'
  }
  return colors[type] || 'default'
}

const getProjectTypeText = (type) => {
  const texts = {
    'WEB': '网站开发',
    'MOBILE': '移动应用',
    'DESIGN': '设计',
    'OTHER': '其他'
  }
  return texts[type] || type
}

const getDifficultyColor = (difficulty) => {
  const colors = {
    'EASY': 'green',
    'MEDIUM': 'orange',
    'HARD': 'red'
  }
  return colors[difficulty] || 'default'
}

const getDifficultyText = (difficulty) => {
  const texts = {
    'EASY': '简单',
    'MEDIUM': '中等',
    'HARD': '困难'
  }
  return texts[difficulty] || difficulty
}

const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const handleViewDetail = (id) => {
  router.push(`/front/projects/${id}`)
}

const handleSubmit = (projectId) => {
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

const handleStartChat = async (project) => {
  if (!project) {
    return
  }
  
  if (project.mySubmission && project.mySubmission.id) {
    router.push(`/front/conversation/${project.mySubmission.id}`)
  } else {
    router.push(`/front/conversation/project/${project.id}`)
  }
}

const loadMyOrders = async () => {
  if (userRole.value !== 'USER') return
  try {
    const res = await request.get('/api/orders/my')
    if (res.code === '200') {
      myOrders.value = res.data || []
      projectList.value.forEach(project => {
        project.hasAccepted = myOrders.value.some(order => order.projectId === project.id && order.status === 'ACCEPTED')
      })
    }
  } catch (error) {
    console.error('加载接单列表失败:', error)
  }
}

const loadMySubmissions = async () => {
  if (userRole.value !== 'USER') return
  try {
    const res = await request.get('/api/submissions/my')
    if (res.code === '200') {
      mySubmissions.value = res.data || []
      projectList.value.forEach(project => {
        project.hasSubmission = mySubmissions.value.some(sub => 
          sub.projectId === project.id && 
          (sub.status === 'SUBMITTED' || sub.status === 'INTERESTED' || sub.status === 'CONFIRMED')
        )
      })
    }
  } catch (error) {
    console.error('加载稿件列表失败:', error)
  }
}

const handleAcceptOrder = async (projectId) => {
  try {
    const res = await request.post(`/api/orders/accept/${projectId}`)
    if (res.code === '200') {
      message.success('接单成功，现在可以提交稿件了')
      await loadMyOrders()
      await loadMySubmissions()
      await loadProjects()
      if (selectedProjectId.value === projectId && selectedProject.value) {
        selectedProject.value.hasAccepted = true
      }
    } else {
      message.error(res.msg || '接单失败')
    }
  } catch (error) {
    console.error('接单失败:', error)
    if (error.response?.data?.msg) {
      message.error(error.response.data.msg)
    } else {
      message.error('接单失败，请检查网络连接')
    }
  }
}

const handleViewEnterprise = async () => {
  if (selectedProject.value && selectedProject.value.enterpriseId) {
    try {
      const res = await request.get(`/api/enterprises/${selectedProject.value.enterpriseId}`)
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

const getStatusColor = (status) => {
  const colors = {
    'PUBLISHED': 'success',
    'CLOSED': 'warning',
    'CONFIRMED': 'processing',
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

const handlePageChange = (page, pageSize) => {
  pagination.current = page
  pagination.pageSize = pageSize
  loadProjects()
}

onMounted(async () => {
  await loadMyOrders()
  await loadMySubmissions()
  loadEnterprises()
  await loadProjects()
})
</script>

<style scoped>
.home-container {
  min-height: calc(100vh - 140px);
  background-color: var(--bg-secondary);
  display: flex;
  flex-direction: column;
}

/* 搜索头部 */
.search-header {
  background: linear-gradient(135deg, #1e2838 0%, #2a3f5f 100%);
  padding: 32px 0 28px;
}

.search-content {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 24px;
}

.search-wrapper {
  margin-bottom: 16px;
}

.search-input {
  width: 100%;
}

.search-input :deep(.ant-input) {
  height: 48px;
  font-size: 15px;
  border-radius: 8px 0 0 8px;
  border: none;
}

.search-input :deep(.ant-input-group-addon) {
  background: transparent;
  border: none;
  padding: 0;
}

.search-btn {
  height: 48px;
  padding: 0 28px;
  font-size: 15px;
  border-radius: 0 8px 8px 0 !important;
  background: var(--primary-color) !important;
  border-color: var(--primary-color) !important;
}

.hot-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.hot-label {
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
}

.hot-tag {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.85);
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: 4px;
}

.hot-tag:hover {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: #ffffff;
}

/* 筛选栏 */
.filter-bar {
  background: #ffffff;
  border-bottom: 1px solid var(--border-light);
  padding: 16px 0;
}

.filter-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
}

.filter-form :deep(.ant-form-item) {
  margin-bottom: 0;
  margin-right: 16px;
}

.filter-form :deep(.ant-form-item-label > label) {
  color: var(--text-secondary);
  font-size: 13px;
}

/* 主内容区域 */
.main-content {
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
  padding: 20px 24px;
  display: flex;
  gap: 20px;
  flex: 1;
  min-height: 0;
}

/* 左侧项目列表 */
.left-panel {
  flex: 0 0 420px;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  height: calc(100vh - 280px);
}

.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.panel-count {
  font-size: 13px;
  color: var(--text-tertiary);
}

.project-list-scroll {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.project-list {
  padding: 8px;
}

.project-card {
  padding: 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
  margin-bottom: 8px;
}

.project-cover {
  width: 100%;
  height: 120px;
  margin-bottom: 12px;
  border-radius: 6px;
  overflow: hidden;
  background: #f5f5f5;
}

.project-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.project-meta-tags {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.project-card:hover {
  background: var(--bg-secondary);
}

.project-card.active {
  background: var(--primary-light);
  border-color: var(--primary-color);
}

.project-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}

.project-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  flex: 1;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.project-budget {
  flex-shrink: 0;
  margin-left: 12px;
  text-align: right;
}

.budget-text {
  font-size: 18px;
  font-weight: 700;
  color: var(--salary-color);
}

.budget-unit {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-left: 2px;
}

.project-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.skill-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  background: var(--bg-tertiary);
  border: none;
  color: var(--text-secondary);
}

.skill-tag.more {
  background: var(--primary-light);
  color: var(--primary-color);
}

.project-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.project-company {
  display: flex;
  align-items: center;
  gap: 8px;
}

.company-name {
  font-size: 13px;
  color: var(--text-secondary);
}

.status-tag {
  font-size: 11px;
}

.project-deadline {
  font-size: 12px;
  color: var(--text-tertiary);
  display: flex;
  align-items: center;
  gap: 4px;
}

.project-actions {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--border-light);
}

.pagination-container {
  padding: 12px 16px;
  border-top: 1px solid var(--border-light);
  text-align: center;
}

/* 右侧详情面板 */
.right-panel {
  flex: 1;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: var(--shadow-sm);
  height: calc(100vh - 280px);
  display: flex;
  flex-direction: column;
}

.detail-scroll {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.detail-container {
  padding: 24px;
}

.detail-header {
  margin-bottom: 20px;
}

.detail-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 12px 0;
  line-height: 1.4;
}

.detail-budget {
  margin-bottom: 16px;
}

.budget-amount {
  font-size: 28px;
  font-weight: 700;
  color: var(--salary-color);
}

.budget-suffix {
  font-size: 14px;
  color: var(--text-tertiary);
  margin-left: 4px;
}

.detail-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}

.detail-meta :deep(.ant-tag) {
  display: inline-flex;
  align-items: center;
  max-width: fit-content;
  white-space: nowrap;
}

.meta-tag {
  margin: 0;
  flex-shrink: 0;
  max-width: fit-content;
}

.company-link {
  padding: 0;
  height: auto;
  color: var(--primary-color);
  font-size: 14px;
}

.meta-divider {
  color: var(--border-dark);
}

.meta-deadline {
  font-size: 14px;
  color: var(--text-tertiary);
  display: flex;
  align-items: center;
  gap: 4px;
}

.detail-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.action-btn {
  height: 44px;
  padding: 0 24px;
  font-size: 15px;
  font-weight: 500;
  border-radius: 6px;
}

.chat-btn {
  background: var(--success-color) !important;
  border-color: var(--success-color) !important;
}

.submit-btn {
  background: var(--info-color) !important;
  border-color: var(--info-color) !important;
  color: #ffffff !important;
}

.detail-section {
  margin-bottom: 28px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title .anticon {
  color: var(--primary-color);
}

.section-content {
  color: var(--text-secondary);
  line-height: 1.8;
  font-size: 14px;
}

.skill-tags-large {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.skill-tag-item {
  font-size: 13px;
  padding: 4px 12px;
  border-radius: 4px;
  background: var(--primary-light);
  border: 1px solid var(--primary-color);
  color: var(--primary-color);
}

.no-data {
  color: var(--text-tertiary);
  font-style: italic;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-grid :deep(.ant-tag) {
  display: inline-flex;
  max-width: fit-content;
  white-space: nowrap;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 13px;
  color: var(--text-tertiary);
}

.info-value {
  font-size: 14px;
  color: var(--text-primary);
}

.info-value.budget {
  color: var(--salary-color);
  font-weight: 600;
}

.info-link {
  padding: 0;
  height: auto;
  font-size: 14px;
}

.empty-detail {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 400px;
}

.empty-icon {
  font-size: 64px;
  color: var(--text-disabled);
}

.empty-state {
  padding: 60px 20px;
}

/* 企业详情弹窗 */
.enterprise-info {
  padding: 8px 0;
}

.enterprise-header {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-light);
}

.default-avatar {
  background: var(--primary-light);
  color: var(--primary-color);
}

.enterprise-basic {
  margin-left: 16px;
}

.enterprise-basic h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: var(--text-primary);
}

.enterprise-descriptions :deep(.ant-descriptions-item-label) {
  background: var(--bg-secondary);
  font-weight: 500;
  width: 100px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .main-content {
    flex-direction: column;
  }

  .left-panel {
    flex: none;
    height: auto;
    max-height: 500px;
  }

  .right-panel {
    flex: none;
    height: auto;
    min-height: 400px;
  }
}

@media (max-width: 768px) {
  .filter-form {
    flex-wrap: wrap;
  }
  
  .filter-form :deep(.ant-form-item) {
    margin-bottom: 8px;
  }
  
  .left-panel {
    flex: 0 0 100%;
  }
  
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
