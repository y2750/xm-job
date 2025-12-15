<template>
  <div class="dashboard-container">
    <!-- 统计卡片区域 -->
    <div class="stats-section">
      <div class="stats-grid">
        <div class="stat-card published">
          <div class="stat-icon">
            <ProjectOutlined />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.publishedProjects }}</div>
            <div class="stat-label">已发布项目</div>
          </div>
        </div>
        <div class="stat-card submissions">
          <div class="stat-icon">
            <FileTextOutlined />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.totalSubmissions }}</div>
            <div class="stat-label">收到稿件</div>
          </div>
        </div>
        <div class="stat-card confirmed">
          <div class="stat-icon">
            <CheckCircleOutlined />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.confirmedProjects }}</div>
            <div class="stat-label">已确定合作</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 项目列表区域 -->
    <div class="projects-section">
      <a-card class="projects-card">
        <template #title>
          <div class="card-header">
            <h3 class="card-title">
              <UnorderedListOutlined />
              我的项目
            </h3>
            <a-button type="primary" @click="handlePublishProject" class="publish-btn">
              <PlusOutlined />
              发布新项目
            </a-button>
          </div>
        </template>
        
        <a-table
          :columns="columns"
          :data-source="projectList"
          :loading="loading"
          :pagination="pagination"
          @change="handleTableChange"
          class="projects-table"
        >
          <template #headerCell="{ column }">
            <template v-if="column.key === 'submissions'">
              <span class="header-centered">稿件</span>
            </template>
            <template v-else-if="column.key === 'deliverables'">
              <span class="header-centered">成品</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <span class="header-centered">操作</span>
            </template>
          </template>
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'title'">
              <div class="project-title-cell">
                <a-tooltip :title="record.title">
                  {{ record.title }}
                </a-tooltip>
              </div>
            </template>
            <template v-else-if="column.key === 'budget'">
              <span class="budget-text">{{ record.budgetMin || 0 }} - {{ record.budgetMax || 0 }} 元</span>
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
            </template>
            <template v-else-if="column.key === 'submissions'">
              <a-button type="link" @click="handleViewSubmissions(record.id)" class="link-btn">
                <FileSearchOutlined />
                查看稿件 ({{ record.submissionCount || 0 }})
              </a-button>
            </template>
            <template v-else-if="column.key === 'deliverables'">
              <a-button
                v-if="record.status === 'CONFIRMED' || record.status === 'COMPLETED'"
                type="link"
                @click="handleViewDeliverables(record.id)"
                class="link-btn"
              >
                <ContainerOutlined />
                查看成品
              </a-button>
              <span v-else class="no-data">-</span>
            </template>
            <template v-else-if="column.key === 'orderCount'">
              <a-button type="link" @click="handleViewOrders(record.id)" class="link-btn">
                <TeamOutlined />
                {{ record.orderCount || 0 }} 人
              </a-button>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-dropdown :trigger="['click']">
                <a-button type="link" size="small">
                  操作
                  <DownOutlined />
                </a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="view" @click="handleViewDetail(record.id)">
                      <EyeOutlined />
                      查看详情
                    </a-menu-item>
                    <a-menu-item 
                      v-if="record.status === 'PUBLISHED'" 
                      key="edit" 
                      @click="handleEdit(record.id)"
                    >
                      <EditOutlined />
                      编辑项目
                    </a-menu-item>
                    <a-menu-divider v-if="record.status === 'PUBLISHED'" />
                    <a-menu-item 
                      v-if="record.status === 'PUBLISHED'" 
                      key="delete" 
                      danger
                      @click="handleDelete(record.id)"
                    >
                      <DeleteOutlined />
                      删除项目
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <!-- 接单人列表弹窗 -->
    <a-modal
      v-model:open="orderListVisible"
      title="接单人列表"
      :width="800"
      :footer="null"
      class="order-modal"
    >
      <a-list
        :data-source="orderList"
        :loading="loadingOrders"
        item-layout="horizontal"
        class="order-list"
      >
        <template #renderItem="{ item }">
          <a-list-item class="order-item">
            <template #actions>
              <a-button type="link" @click="handleViewFreelancerDetail(item.freelancerId)">
                查看详情
              </a-button>
              <a-button type="primary" size="small" @click="handleStartChat(item.projectId, item.freelancerId)">
                <MessageOutlined />
                沟通
              </a-button>
            </template>
            <a-list-item-meta>
              <template #avatar>
                <a-avatar :src="item.freelancerAvatar" :size="52" class="freelancer-avatar">
                  {{ item.freelancerName ? item.freelancerName.charAt(0) : '?' }}
                </a-avatar>
              </template>
              <template #title>
                <a @click="handleViewFreelancerDetail(item.freelancerId)" class="freelancer-name">
                  {{ item.freelancerName || '未知用户' }}
                </a>
              </template>
              <template #description>
                <div class="order-time">
                  <ClockCircleOutlined />
                  接单时间：{{ formatDate(item.acceptedAt) }}
                </div>
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
        <template #empty>
          <a-empty description="暂无接单人" />
        </template>
      </a-list>
    </a-modal>

    <!-- 自由职业者详情弹窗 -->
    <a-modal
      v-model:open="freelancerDetailVisible"
      title="自由职业者详情"
      :width="600"
      :footer="null"
      class="freelancer-modal"
    >
      <a-spin :spinning="loadingFreelancerDetail">
        <div v-if="freelancerDetail" class="freelancer-detail">
          <div class="freelancer-header">
            <a-avatar :src="freelancerDetail.userAvatar" :size="80" class="detail-avatar">
              {{ freelancerDetail.userName ? freelancerDetail.userName.charAt(0) : '?' }}
            </a-avatar>
            <div class="freelancer-info">
              <h3>{{ freelancerDetail.userName || '-' }}</h3>
              <div class="freelancer-meta">
                <a-tag color="blue">
                  <StarOutlined />
                  {{ freelancerDetail.rating || 0 }}分
                </a-tag>
                <a-tag color="green">
                  完成 {{ freelancerDetail.completedProjects || 0 }} 个项目
                </a-tag>
              </div>
            </div>
          </div>
          <a-descriptions :column="1" bordered class="freelancer-descriptions">
            <a-descriptions-item label="技能标签">
              <div class="skills-tags">
                <a-tag 
                  v-for="skill in (freelancerDetail.skills ? freelancerDetail.skills.split(/[,，]/).filter(s => s.trim()) : [])" 
                  :key="skill" 
                  color="processing"
                >
                  {{ skill }}
                </a-tag>
                <span v-if="!freelancerDetail.skills" class="no-data">-</span>
              </div>
            </a-descriptions-item>
            <a-descriptions-item label="信誉分">
              <a-progress 
                :percent="freelancerDetail.creditScore || 100" 
                :status="freelancerDetail.creditScore >= 80 ? 'success' : freelancerDetail.creditScore >= 60 ? 'normal' : 'exception'"
                :stroke-color="{ '0%': '#00a6a7', '100%': '#00c4c4' }"
              />
            </a-descriptions-item>
            <a-descriptions-item label="邮箱">{{ freelancerDetail.userEmail || '-' }}</a-descriptions-item>
            <a-descriptions-item label="电话">{{ freelancerDetail.userPhone || '-' }}</a-descriptions-item>
          </a-descriptions>
        </div>
      </a-spin>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  ProjectOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  UnorderedListOutlined,
  PlusOutlined,
  FileSearchOutlined,
  ContainerOutlined,
  TeamOutlined,
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  ClockCircleOutlined,
  MessageOutlined,
  StarOutlined,
  DownOutlined
} from '@ant-design/icons-vue'
import request from '@/utils/request'
import dayjs from 'dayjs'

const router = useRouter()

const loading = ref(false)
const projectList = ref([])
const statistics = reactive({
  publishedProjects: 0,
  totalSubmissions: 0,
  confirmedProjects: 0
})

// 接单人列表相关
const orderListVisible = ref(false)
const loadingOrders = ref(false)
const orderList = ref([])
const currentProjectId = ref(null)

// 自由职业者详情相关
const freelancerDetailVisible = ref(false)
const loadingFreelancerDetail = ref(false)
const freelancerDetail = ref(null)

const columns = [
  { title: '项目标题', dataIndex: 'title', key: 'title', ellipsis: true },
  { title: '预算范围', key: 'budget', width: 160 },
  { title: '截止时间', dataIndex: 'deadline', key: 'deadline', width: 120 },
  { title: '状态', key: 'status', width: 100 },
  { title: '接单人数', key: 'orderCount', width: 100 },
  { title: '稿件', key: 'submissions', width: 140 },
  { title: '成品', key: 'deliverables', width: 100 },
  { title: '操作', key: 'action', width: 90, fixed: 'right', align: 'center' }
]

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total) => `共 ${total} 条`
})

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

const loadMyProjects = async () => {
  loading.value = true
  try {
    let enterpriseId = null
    try {
      const enterpriseRes = await request.get('/api/enterprises/profile')
      if (enterpriseRes.code === '200' && enterpriseRes.data) {
        enterpriseId = enterpriseRes.data.id
      }
    } catch (error) {
      console.error('获取企业信息失败:', error)
      message.warning('请先完善企业信息')
    }
    
    if (enterpriseId) {
      try {
        const statsRes = await request.get('/api/projects/statistics', { params: { enterpriseId } })
        if (statsRes.code === '200' && statsRes.data) {
          statistics.publishedProjects = statsRes.data.publishedProjects || 0
          statistics.confirmedProjects = statsRes.data.confirmedProjects || 0
          statistics.totalSubmissions = statsRes.data.totalSubmissions || 0
        }
      } catch (error) {
        console.error('加载统计信息失败:', error)
      }
    } else {
      statistics.publishedProjects = 0
      statistics.confirmedProjects = 0
      statistics.totalSubmissions = 0
    }
    
    const params = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    if (enterpriseId) {
      params.enterpriseId = enterpriseId
    }
    
    const res = await request.get('/api/projects', { params })
    if (res.code === '200') {
      const data = res.data
      if (data) {
        projectList.value = data.list || []
        pagination.total = data.total || 0
        
        projectList.value.forEach(project => {
          if (!project.orderCount) project.orderCount = 0
          if (!project.submissionCount) project.submissionCount = 0
        })
      }
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

const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadMyProjects()
}

const handlePublishProject = () => {
  router.push('/front/projects/publish')
}

const handleViewDetail = (id) => {
  router.push(`/front/projects/${id}`)
}

const handleViewSubmissions = (id) => {
  router.push(`/front/projects/${id}/submissions`)
}

const handleViewDeliverables = (id) => {
  router.push(`/front/projects/${id}/deliverables`)
}

const handleEdit = (id) => {
  router.push(`/front/projects/edit/${id}`)
}

const handleDelete = (id) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除此项目吗？删除后无法恢复。',
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await request.delete(`/api/projects/${id}`)
        if (res.code === '200') {
          message.success('删除成功')
          loadMyProjects()
        } else {
          message.error(res.msg)
        }
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
}

const handleViewOrders = async (projectId) => {
  currentProjectId.value = projectId
  orderListVisible.value = true
  loadingOrders.value = true
  try {
    const res = await request.get(`/api/orders/project/${projectId}`)
    if (res.code === '200') {
      orderList.value = res.data || []
      if (orderList.value.length === 0) {
        message.info('暂无接单人')
      }
    } else {
      message.error(res.msg || '加载接单人列表失败')
    }
  } catch (error) {
    console.error('加载接单人列表失败:', error)
    message.error('加载接单人列表失败')
  } finally {
    loadingOrders.value = false
  }
}

const handleViewFreelancerDetail = async (freelancerId) => {
  if (!freelancerId) {
    message.warning('自由职业者ID不存在')
    return
  }
  
  freelancerDetailVisible.value = true
  loadingFreelancerDetail.value = true
  try {
    const res = await request.get(`/api/freelancers/${freelancerId}`)
    if (res.code === '200' && res.data) {
      freelancerDetail.value = res.data
    } else {
      message.error(res.msg || '加载自由职业者详情失败')
      freelancerDetail.value = null
    }
  } catch (error) {
    console.error('加载自由职业者详情失败:', error)
    message.error('加载自由职业者详情失败')
    freelancerDetail.value = null
  } finally {
    loadingFreelancerDetail.value = false
  }
}

const handleStartChat = async (projectId, freelancerId) => {
  if (!projectId || !freelancerId) {
    message.warning('项目ID或自由职业者ID不存在')
    return
  }
  
  try {
    const submissionsRes = await request.get(`/api/projects/${projectId}/submissions`)
    if (submissionsRes.code === '200' && submissionsRes.data) {
      const submission = submissionsRes.data.find(s => s.freelancerId === freelancerId)
      if (submission && submission.id) {
        router.push(`/front/conversation/${submission.id}`)
        return
      }
    }
    
    router.push(`/front/conversation/project/${projectId}?freelancerId=${freelancerId}`)
  } catch (error) {
    console.error('跳转聊天失败:', error)
    message.error('跳转聊天失败')
  }
}

const formatDate = (date) => {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

onMounted(() => {
  loadMyProjects()
})
</script>

<style scoped>
.dashboard-container {
  padding: 24px;
  background: var(--bg-secondary);
  min-height: calc(100vh - 140px);
}

/* 统计卡片区域 */
.stats-section {
  margin-bottom: 24px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.stat-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  box-shadow: var(--shadow-sm);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #ffffff;
  flex-shrink: 0;
}

.stat-card.published .stat-icon {
  background: linear-gradient(135deg, #00a6a7 0%, #00c4c4 100%);
}

.stat-card.submissions .stat-icon {
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
}

.stat-card.confirmed .stat-icon {
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
}

.stat-info {
  margin-left: 20px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: var(--text-tertiary);
  margin-top: 4px;
}

/* 项目列表区域 */
.projects-section {
  
}

.projects-card {
  border-radius: 12px;
  box-shadow: var(--shadow-sm);
}

.projects-card :deep(.ant-card-head) {
  border-bottom: 1px solid var(--border-light);
  padding: 16px 24px;
}

.projects-card :deep(.ant-card-body) {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-title .anticon {
  color: var(--primary-color);
}

.publish-btn {
  height: 36px;
  border-radius: 6px;
}

.projects-table {
  
}

.projects-table :deep(.ant-table-thead > tr > th) {
  background: var(--bg-secondary) !important;
  font-weight: 600;
  color: var(--text-primary);
  padding: 14px 16px;
}

.projects-table :deep(.ant-table-tbody > tr > td) {
  padding: 14px 16px;
}

.projects-table :deep(.ant-table-tbody > tr:hover > td) {
  background: var(--primary-light) !important;
}

.header-centered {
  display: block;
  text-align: center;
}

.project-title-cell {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
  color: var(--text-primary);
}

.budget-text {
  color: var(--salary-color);
  font-weight: 600;
}

.link-btn {
  padding: 0;
  height: auto;
  display: flex;
  align-items: center;
  gap: 4px;
}

.no-data {
  color: var(--text-disabled);
}

/* 接单人列表弹窗 */
.order-modal :deep(.ant-modal-header) {
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-light);
}

.order-list {
  max-height: 500px;
  overflow-y: auto;
}

.order-item {
  padding: 16px 0;
  border-bottom: 1px solid var(--border-light);
}

.order-item:last-child {
  border-bottom: none;
}

.freelancer-avatar {
  background: var(--primary-light);
  color: var(--primary-color);
}

.freelancer-name {
  font-weight: 500;
  color: var(--text-primary);
  font-size: 15px;
}

.freelancer-name:hover {
  color: var(--primary-color);
}

.order-time {
  font-size: 13px;
  color: var(--text-tertiary);
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 自由职业者详情弹窗 */
.freelancer-detail {
  padding: 8px 0;
}

.freelancer-header {
  display: flex;
  align-items: center;
  padding-bottom: 20px;
  margin-bottom: 20px;
  border-bottom: 1px solid var(--border-light);
}

.detail-avatar {
  background: var(--primary-light);
  color: var(--primary-color);
}

.freelancer-info {
  margin-left: 20px;
}

.freelancer-info h3 {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.freelancer-meta {
  display: flex;
  gap: 8px;
}

.freelancer-descriptions :deep(.ant-descriptions-item-label) {
  background: var(--bg-secondary);
  font-weight: 500;
  width: 80px;
}

.skills-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* 响应式设计 */
@media (max-width: 992px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .dashboard-container {
    padding: 16px;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
