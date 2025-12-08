<template>
  <div style="min-height: 1000px; background-color: #f6f6f8">
    <div style="margin: 25px auto; width: 70%; text-align: center">
      <a-input-search
        v-model:value="searchForm.title"
        placeholder="请输入你感兴趣的项目"
        size="large"
        style="width: 500px; margin-right: 5px"
        @search="handleSearch"
      />
    </div>
    <div style="margin: 0 auto; width: 70%">
      <a-card>
        <template #title>
          <h2>外包项目列表</h2>
        </template>
        
        <!-- 筛选条件 -->
        <a-form :model="searchForm" layout="inline" style="margin-bottom: 20px" class="filter-form">
          <a-form-item label="关键词">
            <a-input v-model:value="searchForm.title" placeholder="项目标题" allow-clear style="width: 180px" />
          </a-form-item>
          <a-form-item label="技能标签">
            <a-input v-model:value="searchForm.skillsRequired" placeholder="技能标签" allow-clear style="width: 180px" />
          </a-form-item>
          <a-form-item label="发布企业">
            <a-select
              v-model:value="searchForm.enterpriseId"
              placeholder="选择或输入企业名称"
              allow-clear
              show-search
              :filter-option="false"
              :options="enterpriseOptions"
              style="width: 180px"
              @search="handleEnterpriseSearch"
              @change="handleEnterpriseChange"
            >
            </a-select>
          </a-form-item>
          <a-form-item label="状态" class="status-item">
            <a-select v-model:value="searchForm.status" placeholder="项目状态" allow-clear style="width: 130px">
              <a-select-option value="PUBLISHED">已发布</a-select-option>
              <a-select-option value="CLOSED">已截止</a-select-option>
              <a-select-option value="CONFIRMED">已确定合作</a-select-option>
              <a-select-option value="COMPLETED">已完成</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item class="button-group">
            <a-button type="primary" @click="handleSearch">搜索</a-button>
            <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
          </a-form-item>
        </a-form>

        <!-- 项目列表 -->
        <a-list
          v-if="projectList.length > 0"
          :data-source="projectList"
          :pagination="pagination"
          :loading="loading"
        >
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta>
                <template #title>
                  <a @click="handleViewDetail(item.id)">{{ item.title }}</a>
                </template>
                <template #description>
                  <div>
                    <a-tag v-for="skill in (item.skillList || [])" :key="skill" style="margin-right: 8px">
                      {{ skill }}
                    </a-tag>
                    <div style="margin-top: 8px">
                      <span>预算：{{ item.budgetMin || 0 }} - {{ item.budgetMax || 0 }} 元</span>
                      <span style="margin-left: 16px">截止时间：{{ formatDate(item.deadline) }}</span>
                      <span style="margin-left: 16px" v-if="item.enterpriseName">发布企业：{{ item.enterpriseName }}</span>
                    </div>
                  </div>
                </template>
              </a-list-item-meta>
              <template #actions>
                <a-button type="link" @click="handleViewDetail(item.id)">查看详情</a-button>
                <a-button v-if="userRole === 'USER' && item.status === 'PUBLISHED' && !item.hasAccepted" type="primary" @click="handleAcceptOrder(item.id)">接单</a-button>
                <a-button v-if="userRole === 'USER' && item.status === 'PUBLISHED' && item.hasAccepted && !item.hasSubmission" type="link" @click="handleSubmit(item.id)">提交稿件</a-button>
                <a-button v-if="userRole === 'USER' && item.status === 'PUBLISHED' && item.hasAccepted && item.hasSubmission" type="link" @click="handleViewSubmission(item.id)">查看稿件</a-button>
              </template>
            </a-list-item>
          </template>
        </a-list>
        <a-empty v-else :description="loading ? '加载中...' : '暂无项目数据'" />
      </a-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import request from '@/utils/request'

const router = useRouter()

const loading = ref(false)
const projectList = ref([])
const userRole = ref(localStorage.getItem('xm-user') ? JSON.parse(localStorage.getItem('xm-user')).role : '')
const myOrders = ref([]) // 我的接单列表
const mySubmissions = ref([]) // 我的稿件列表
const enterpriseList = ref([])
const enterpriseOptions = ref([])
const enterpriseSearchKeyword = ref('')

const searchForm = reactive({
  title: '',
  skillsRequired: '',
  status: '',
  enterpriseId: undefined,
  enterpriseName: ''
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
  return dateStr.replace('T', ' ').substring(0, 19)
}

const loadProjects = async () => {
  loading.value = true
  try {
    const params = {
      title: searchForm.title,
      skillsRequired: searchForm.skillsRequired,
      status: searchForm.status,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    // 如果选择了企业ID，使用ID筛选；否则如果输入了企业名称，使用名称筛选
    if (searchForm.enterpriseId) {
      params.enterpriseId = searchForm.enterpriseId
    } else if (searchForm.enterpriseName) {
      params.enterpriseName = searchForm.enterpriseName
    }
    
    const res = await request.get('/api/projects', { params })
    if (res.code === '200') {
      // PageInfo 对象结构：{ list: [], total: 0, pageNum: 1, pageSize: 10, ... }
      const data = res.data
      if (data) {
        projectList.value = data.list || []
        pagination.total = data.total || 0
        // 处理技能标签列表
        projectList.value.forEach(item => {
          if (item.skillsRequired && !item.skillList) {
            // 支持中文逗号和英文逗号
            item.skillList = item.skillsRequired.split(/[,，]/).filter(s => s.trim())
          }
          // 标记是否已接单
          item.hasAccepted = myOrders.value.some(order => order.projectId === item.id && order.status === 'ACCEPTED')
          // 标记是否已提交稿件（只检查有效状态的稿件：SUBMITTED、INTERESTED、CONFIRMED）
          item.hasSubmission = mySubmissions.value.some(sub => 
            sub.projectId === item.id && 
            (sub.status === 'SUBMITTED' || sub.status === 'INTERESTED' || sub.status === 'CONFIRMED')
          )
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
      pageSize: 1000 // 获取所有企业用于下拉选择
    }
    const res = await request.get('/api/enterprises', { params })
    if (res.code === '200' && res.data) {
      let list = res.data.list || []
      // 前端过滤：如果有关键词，进行模糊匹配
      if (keyword) {
        list = list.filter(e => 
          e.employName && e.employName.toLowerCase().includes(keyword.toLowerCase())
        )
      }
      enterpriseList.value = list
      // 转换为选项格式
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
  // 实时过滤下拉选项
  loadEnterprises(value)
}

const handleEnterpriseChange = (value) => {
  // 如果选择了企业ID，清空企业名称筛选和搜索关键词
  if (value) {
    searchForm.enterpriseName = ''
    enterpriseSearchKeyword.value = ''
  } else {
    // 如果清空选择，清空所有企业相关筛选
    searchForm.enterpriseName = ''
    enterpriseSearchKeyword.value = ''
  }
}

const handleSearch = () => {
  // 如果输入了企业名称但没有选择ID，使用名称筛选
  if (enterpriseSearchKeyword.value && !searchForm.enterpriseId) {
    searchForm.enterpriseName = enterpriseSearchKeyword.value
  } else if (!searchForm.enterpriseId) {
    searchForm.enterpriseName = ''
  }
  pagination.current = 1
  loadProjects()
}

const handleReset = () => {
  Object.assign(searchForm, {
    title: '',
    skillsRequired: '',
    status: '',
    enterpriseId: undefined,
    enterpriseName: ''
  })
  enterpriseSearchKeyword.value = ''
  loadEnterprises()
  handleSearch()
}

const handleViewDetail = (id) => {
  router.push(`/front/projects/${id}`)
}

const handleSubmit = (projectId) => {
  router.push(`/front/submissions/submit/${projectId}`)
}

const handleViewSubmission = async (projectId) => {
  // 查找该项目的稿件
  const submission = mySubmissions.value.find(sub => sub.projectId === projectId)
  if (submission) {
    router.push(`/front/submissions/${submission.id}`)
  } else {
    message.error('未找到稿件信息')
  }
}

const loadMyOrders = async () => {
  if (userRole.value !== 'USER') return
  try {
    const res = await request.get('/api/orders/my')
    if (res.code === '200') {
      myOrders.value = res.data || []
      // 标记项目是否已接单
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
      // 标记项目是否已提交稿件
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
      // 重新加载项目列表以更新接单状态
      await loadProjects()
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

onMounted(async () => {
  await loadProjects()
  await loadMyOrders()
  await loadMySubmissions()
  loadEnterprises()
})
</script>

<style scoped>
.card {
  background-color: white;
  padding: 15px;
  border-radius: 5px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.filter-form :deep(.ant-form) {
  display: flex;
  flex-wrap: nowrap;
  align-items: flex-start;
  overflow-x: auto;
}

.filter-form :deep(.ant-form-item) {
  margin-bottom: 0;
  margin-right: 12px;
  flex-shrink: 0;
  white-space: nowrap;
}

.filter-form :deep(.ant-form-item-label) {
  padding-right: 8px;
  white-space: nowrap;
}

.filter-form :deep(.status-item) {
  margin-right: 0;
}

.filter-form :deep(.button-group) {
  margin-left: -135px;
  margin-right: 0;
  margin-top: 42px;
  align-self: flex-start;
}
</style>
