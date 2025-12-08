<template>
  <div class="enterprise-dashboard-container">
    <a-row :gutter="16">
      <a-col :span="24">
        <a-card>
          <template #title>
            <h2>企业工作台</h2>
          </template>
          <a-row :gutter="16">
            <a-col :span="8">
              <a-statistic title="已发布项目" :value="statistics.publishedProjects" />
            </a-col>
            <a-col :span="8">
              <a-statistic title="收到稿件" :value="statistics.totalSubmissions" />
            </a-col>
            <a-col :span="8">
              <a-statistic title="已确定合作" :value="statistics.confirmedProjects" />
            </a-col>
          </a-row>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :span="24">
        <a-card>
          <template #title>
            <h3>我的项目</h3>
          </template>
          <template #extra>
            <a-button type="primary" @click="handlePublishProject">发布新项目</a-button>
          </template>
          
          <a-table
            :columns="columns"
            :data-source="projectList"
            :loading="loading"
            :pagination="pagination"
            @change="handleTableChange"
          >
            <template #headerCell="{ column }">
              <template v-if="column.key === 'submissions'">
                <span style="padding-left: 55px; display: inline-block;">稿件</span>
              </template>
              <template v-else-if="column.key === 'deliverables'">
                <span style="padding-left: 55px; display: inline-block;">成品</span>
              </template>
              <template v-else-if="column.key === 'action'">
                <span style="padding-left: 55px; display: inline-block;">操作</span>
              </template>
            </template>
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
              </template>
              <template v-else-if="column.key === 'submissions'">
                <div style="padding-left: 55px;">
                  <a-button type="link" @click="handleViewSubmissions(record.id)">
                    查看稿件 ({{ record.submissionCount || 0 }})
                  </a-button>
                </div>
              </template>
              <template v-else-if="column.key === 'deliverables'">
                <div style="padding-left: 55px;">
                  <a-button
                    v-if="record.status === 'CONFIRMED' || record.status === 'COMPLETED'"
                    type="link"
                    @click="handleViewDeliverables(record.id)"
                  >
                    查看成品
                  </a-button>
                  <span v-else>-</span>
                </div>
              </template>
              <template v-else-if="column.key === 'orderCount'">
                {{ record.orderCount || 0 }} 人
              </template>
              <template v-else-if="column.key === 'action'">
                <div style="padding-left: 55px;">
                  <a-space>
                    <a-button type="link" @click="handleViewDetail(record.id)">查看详情</a-button>
                    <a-button
                      v-if="record.status === 'PUBLISHED'"
                      type="link"
                      @click="handleEdit(record.id)"
                    >
                      编辑
                    </a-button>
                    <a-button
                      v-if="record.status === 'PUBLISHED'"
                      type="link"
                      danger
                      @click="handleDelete(record.id)"
                    >
                      删除
                    </a-button>
                  </a-space>
                </div>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import request from '@/utils/request'

const router = useRouter()

const loading = ref(false)
const projectList = ref([])
const statistics = reactive({
  publishedProjects: 0,
  totalSubmissions: 0,
  confirmedProjects: 0
})

const columns = [
  { title: '项目标题', dataIndex: 'title', key: 'title' },
  { title: '预算范围', key: 'budget', customRender: ({ record }) => `${record.budgetMin || 0} - ${record.budgetMax || 0} 元` },
  { title: '截止时间', dataIndex: 'deadline', key: 'deadline' },
  { title: '状态', key: 'status' },
  { title: '接单人数', key: 'orderCount', width: 100, customRender: ({ record }) => record.orderCount || 0 },
  { title: '稿件', key: 'submissions' },
  { title: '成品', key: 'deliverables' },
  { title: '操作', key: 'action', width: 200 }
]

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total) => `共 ${total} 条`
})

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

const loadMyProjects = async () => {
  loading.value = true
  try {
    // 先获取当前用户的企业信息
    let enterpriseId = null
    try {
      const enterpriseRes = await request.get('/api/enterprises/profile')
      console.log('企业信息接口返回:', enterpriseRes)
      if (enterpriseRes.code === '200' && enterpriseRes.data) {
        enterpriseId = enterpriseRes.data.id
        console.log('获取到企业ID:', enterpriseId)
      } else {
        console.warn('企业信息接口返回异常:', enterpriseRes)
      }
    } catch (error) {
      console.error('获取企业信息失败:', error)
      message.warning('请先完善企业信息')
    }
    
    // 加载统计信息（所有项目，不分页）
    if (enterpriseId) {
      try {
        console.log('调用统计接口，企业ID:', enterpriseId)
        const statsRes = await request.get('/api/projects/statistics', { params: { enterpriseId } })
        console.log('统计接口返回:', statsRes)
        if (statsRes.code === '200' && statsRes.data) {
          statistics.publishedProjects = statsRes.data.publishedProjects || 0
          statistics.confirmedProjects = statsRes.data.confirmedProjects || 0
          statistics.totalSubmissions = statsRes.data.totalSubmissions || 0
          console.log('统计结果已更新:', statistics)
        } else {
          console.error('统计接口返回错误:', statsRes)
          message.error(statsRes.msg || '获取统计信息失败')
        }
      } catch (error) {
        console.error('加载统计信息失败:', error)
        if (error.response?.data?.msg) {
          message.error('加载统计信息失败: ' + error.response.data.msg)
        } else {
          message.error('加载统计信息失败')
        }
      }
    } else {
      console.warn('未获取到企业ID，无法加载统计信息')
      // 如果获取不到企业ID，统计数量保持为0
      statistics.publishedProjects = 0
      statistics.confirmedProjects = 0
      statistics.totalSubmissions = 0
    }
    
    // 如果获取到企业ID，查询该企业的项目
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
        
        // 确保每个项目都有接单人数和稿件数（后端已返回）
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
  router.push(`/front/projects/${id}?edit=true`)
}

const handleDelete = (id) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除此项目吗？删除后无法恢复。',
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

onMounted(() => {
  loadMyProjects()
})
</script>

<style scoped>
.enterprise-dashboard-container {
  padding: 20px;
}

/* 确保表格列对齐 */
:deep(.ant-table) {
  table-layout: auto;
}

:deep(.ant-table-thead > tr > th),
:deep(.ant-table-tbody > tr > td) {
  text-align: left;
  vertical-align: middle;
  padding: 16px;
}

/* 确保表头和内容列的padding一致 - 稿件、成品、操作列 */
:deep(.ant-table-thead > tr > th:nth-child(6)),
:deep(.ant-table-tbody > tr > td:nth-child(6)),
:deep(.ant-table-thead > tr > th:nth-child(7)),
:deep(.ant-table-tbody > tr > td:nth-child(7)),
:deep(.ant-table-thead > tr > th:nth-child(8)),
:deep(.ant-table-tbody > tr > td:nth-child(8)) {
  padding-left: 16px;
}

/* 稿件、成品、操作列内容的padding */
:deep(.ant-table-thead > tr > th:nth-child(6) > span),
:deep(.ant-table-tbody > tr > td:nth-child(6) > div),
:deep(.ant-table-thead > tr > th:nth-child(7) > span),
:deep(.ant-table-tbody > tr > td:nth-child(7) > div),
:deep(.ant-table-thead > tr > th:nth-child(8) > span),
:deep(.ant-table-tbody > tr > td:nth-child(8) > div) {
  padding-left: 55px;
  display: inline-block;
}
</style>

