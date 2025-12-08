<template>
  <div class="project-list-container">
    <a-card>
      <template #title>
        <h2>外包项目列表</h2>
      </template>
      
      <!-- 筛选条件 -->
      <a-form :model="searchForm" layout="inline" style="margin-bottom: 20px">
        <a-form-item label="关键词">
          <a-input v-model:value="searchForm.title" placeholder="项目标题" allow-clear />
        </a-form-item>
        <a-form-item label="技能标签">
          <a-input v-model:value="searchForm.skillsRequired" placeholder="技能标签" allow-clear />
        </a-form-item>
        <a-form-item label="发布企业">
          <a-select
            v-model:value="searchForm.enterpriseId"
            placeholder="选择或输入企业名称"
            allow-clear
            show-search
            :filter-option="false"
            :options="enterpriseOptions"
            style="width: 200px"
            @search="handleEnterpriseSearch"
            @change="handleEnterpriseChange"
          >
          </a-select>
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="searchForm.status" placeholder="项目状态" allow-clear style="width: 150px">
            <a-select-option value="PUBLISHED">已发布</a-select-option>
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
              <a-button v-if="userRole === 'USER' && item.status === 'PUBLISHED'" type="link" @click="handleSubmit(item.id)">提交稿件</a-button>
            </template>
          </a-list-item>
        </template>
      </a-list>
      <a-empty v-else :description="loading ? '加载中...' : '暂无项目数据'" />
    </a-card>
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

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  if (typeof dateStr === 'string') {
    return dateStr.replace('T', ' ').substring(0, 19)
  }
  return dateStr
}

const handleSubmit = (projectId) => {
  router.push(`/front/submissions/submit/${projectId}`)
}

onMounted(() => {
  loadProjects()
  loadEnterprises()
})
</script>

<style scoped>
.project-list-container {
  padding: 20px;
}
</style>

