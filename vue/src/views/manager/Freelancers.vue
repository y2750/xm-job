<template>
  <a-card>
    <template #title>
      <h3>自由职业者管理</h3>
    </template>
    
    <!-- 搜索筛选 -->
    <a-form :model="searchForm" layout="inline" style="margin-bottom: 20px">
      <a-form-item label="关键词">
        <a-input v-model:value="searchForm.name" placeholder="姓名/用户名" allow-clear style="width: 200px" />
      </a-form-item>
      <a-form-item label="认证状态">
        <a-select v-model:value="searchForm.verified" placeholder="认证状态" allow-clear style="width: 150px">
          <a-select-option :value="true">已认证</a-select-option>
          <a-select-option :value="false">未认证</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="handleSearch">搜索</a-button>
        <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
      </a-form-item>
    </a-form>

    <!-- 自由职业者列表 -->
    <a-table
      :columns="columns"
      :data-source="freelancerList"
      :pagination="pagination"
      :loading="loading"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'verified'">
          <a-tag :color="record.verified ? 'green' : 'default'">{{ record.verified ? '已认证' : '未认证' }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-button type="link" @click="handleView(record.id)">查看</a-button>
          <a-button v-if="!record.verified" type="link" @click="handleVerify(record.id)">认证</a-button>
          <a-button v-if="record.verified" type="link" danger @click="handleUnverify(record.id)">取消认证</a-button>
        </template>
      </template>
    </a-table>
  </a-card>

  <!-- 自由职业者详情弹窗 -->
  <a-modal
    v-model:open="detailVisible"
    title="自由职业者详情"
    :footer="null"
    width="800px"
  >
    <a-descriptions v-if="freelancerDetail" bordered :column="2">
      <a-descriptions-item label="ID">{{ freelancerDetail.id }}</a-descriptions-item>
      <a-descriptions-item label="姓名">{{ freelancerDetail.userName }}</a-descriptions-item>
      <a-descriptions-item label="电话">{{ freelancerDetail.userPhone || '未填写' }}</a-descriptions-item>
      <a-descriptions-item label="邮箱">{{ freelancerDetail.userEmail || '未填写' }}</a-descriptions-item>
      <a-descriptions-item label="技能" :span="2">
        {{ freelancerDetail.skills || '未填写' }}
      </a-descriptions-item>
      <a-descriptions-item label="作品集链接" :span="2">
        <a v-if="freelancerDetail.portfolioUrl" :href="freelancerDetail.portfolioUrl" target="_blank">
          {{ freelancerDetail.portfolioUrl }}
        </a>
        <span v-else>未填写</span>
      </a-descriptions-item>
      <a-descriptions-item label="作品数量">{{ freelancerDetail.portfolioCount || 0 }}</a-descriptions-item>
      <a-descriptions-item label="评分">{{ freelancerDetail.rating || '0.00' }}</a-descriptions-item>
      <a-descriptions-item label="完成项目数">{{ freelancerDetail.completedProjects || 0 }}</a-descriptions-item>
      <a-descriptions-item label="认证状态">
        <a-tag :color="freelancerDetail.verified ? 'green' : 'default'">
          {{ freelancerDetail.verified ? '已认证' : '未认证' }}
        </a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="认证时间" :span="2">
        {{ freelancerDetail.verifiedAt || '未认证' }}
      </a-descriptions-item>
      <a-descriptions-item label="创建时间" :span="2">
        {{ freelancerDetail.createdAt }}
      </a-descriptions-item>
      <a-descriptions-item label="更新时间" :span="2">
        {{ freelancerDetail.updatedAt }}
      </a-descriptions-item>
    </a-descriptions>
    
    <!-- 证书管理 -->
    <a-divider>证书管理</a-divider>
    <a-spin :spinning="loadingCertificates">
      <div v-if="freelancerCertificates.length > 0">
        <a-list :data-source="freelancerCertificates" :grid="{ gutter: 16, xs: 1, sm: 2, md: 2 }" bordered>
          <template #renderItem="{ item }">
            <a-list-item>
              <a-card hoverable size="small">
                <template #cover v-if="item.certificateUrl">
                  <img :src="item.certificateUrl" :alt="item.certificateName" style="height: 120px; object-fit: cover; cursor: pointer" @click="window.open(item.certificateUrl, '_blank')" />
                </template>
                <a-card-meta>
                  <template #title>
                    <div style="display: flex; justify-content: space-between; align-items: center">
                      <span>{{ item.certificateName }}</span>
                      <a-tag :color="item.verified ? 'green' : 'orange'">
                        {{ item.verified ? '已认证' : '未认证' }}
                      </a-tag>
                    </div>
                  </template>
                  <template #description>
                    <div style="font-size: 12px; color: #666">
                      <div v-if="item.certificateType">类型：{{ getCertificateTypeText(item.certificateType) }}</div>
                      <div v-if="item.issuingOrganization">颁发机构：{{ item.issuingOrganization }}</div>
                      <div v-if="item.issueDate">颁发日期：{{ item.issueDate }}</div>
                    </div>
                    <div style="margin-top: 8px">
                      <a-button size="small" :type="item.verified ? 'default' : 'primary'" @click="handleUpdateCertificateStatus(item.id, !item.verified)">
                        {{ item.verified ? '取消认证' : '认证' }}
                      </a-button>
                    </div>
                  </template>
                </a-card-meta>
              </a-card>
            </a-list-item>
          </template>
        </a-list>
      </div>
      <a-empty v-else description="暂无证书" />
    </a-spin>
  </a-modal>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import request from '@/utils/request'

const loading = ref(false)
const freelancerList = ref([])

const searchForm = reactive({
  name: '',
  verified: undefined
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total) => `共 ${total} 条`,
  onChange: (page, pageSize) => {
    pagination.current = page
    pagination.pageSize = pageSize
    loadFreelancers()
  }
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '姓名', dataIndex: 'userName', key: 'userName' },
  { title: '电话', dataIndex: 'userPhone', key: 'userPhone' },
  { title: '邮箱', dataIndex: 'userEmail', key: 'userEmail' },
  { title: '技能', dataIndex: 'skills', key: 'skills' },
  { title: '评分', dataIndex: 'rating', key: 'rating', customRender: ({ record }) => record.rating || '0.00' },
  { title: '完成项目数', dataIndex: 'completedProjects', key: 'completedProjects' },
  { title: '认证状态', key: 'verified', width: 120 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt' },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
]

const loadFreelancers = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/freelancers', {
      params: {
        ...searchForm,
        pageNum: pagination.current,
        pageSize: pagination.pageSize
      }
    })
    if (res.code === '200') {
      const data = res.data
      freelancerList.value = data.list || []
      pagination.total = data.total || 0
    } else {
      message.error(res.msg || '加载自由职业者列表失败')
    }
  } catch (error) {
    console.error('加载自由职业者列表失败:', error)
    message.error('加载自由职业者列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadFreelancers()
}

const handleReset = () => {
  Object.assign(searchForm, {
    name: '',
    verified: undefined
  })
  handleSearch()
}

const freelancerDetail = ref(null)
const detailVisible = ref(false)

const freelancerCertificates = ref([])
const loadingCertificates = ref(false)

const handleView = async (id) => {
  try {
    const res = await request.get(`/api/freelancers/${id}`)
    if (res.code === '200') {
      freelancerDetail.value = res.data
      detailVisible.value = true
      
      // 加载证书列表
      loadingCertificates.value = true
      try {
        const certRes = await request.get(`/api/certificates/freelancer/${id}`)
        if (certRes.code === '200' && certRes.data) {
          freelancerCertificates.value = certRes.data
        }
      } catch (error) {
        console.error('加载证书失败:', error)
        freelancerCertificates.value = []
      } finally {
        loadingCertificates.value = false
      }
    } else {
      message.error(res.msg || '获取自由职业者详情失败')
    }
  } catch (error) {
    console.error('获取自由职业者详情失败:', error)
    message.error('获取自由职业者详情失败')
  }
}

const handleUpdateCertificateStatus = async (certificateId, verified) => {
  try {
    const res = await request.put(`/api/certificates/${certificateId}`, {
      verified: verified
    })
    if (res.code === '200') {
      message.success(verified ? '证书已认证' : '证书已取消认证')
      // 重新加载证书列表
      if (freelancerDetail.value && freelancerDetail.value.id) {
        const certRes = await request.get(`/api/certificates/freelancer/${freelancerDetail.value.id}`)
        if (certRes.code === '200' && certRes.data) {
          freelancerCertificates.value = certRes.data
        }
      }
    } else {
      message.error(res.msg || '更新证书状态失败')
    }
  } catch (error) {
    console.error('更新证书状态失败:', error)
    message.error('更新证书状态失败')
  }
}

const handleVerify = async (id) => {
  Modal.confirm({
    title: '确认认证',
    content: '确定要认证此自由职业者吗？',
    onOk: async () => {
      try {
        const freelancer = freelancerList.value.find(f => f.id === id)
        if (freelancer) {
          const res = await request.put('/api/freelancers', {
            id: id,
            verified: true,
            verifiedAt: new Date().toISOString().substring(0, 19).replace('T', ' ')
          })
          if (res.code === '200') {
            message.success('认证成功')
            loadFreelancers()
          } else {
            message.error(res.msg || '认证失败')
          }
        }
      } catch (error) {
        console.error('认证失败:', error)
        message.error('认证失败')
      }
    }
  })
}

const handleUnverify = async (id) => {
  Modal.confirm({
    title: '确认取消认证',
    content: '确定要取消此自由职业者的认证吗？取消认证后，该用户将无法提交稿件。',
    onOk: async () => {
      try {
        const res = await request.put('/api/freelancers', {
          id: id,
          verified: false,
          verifiedAt: null
        })
        if (res.code === '200') {
          message.success('取消认证成功')
          loadFreelancers()
        } else {
          message.error(res.msg || '取消认证失败')
        }
      } catch (error) {
        console.error('取消认证失败:', error)
        message.error('取消认证失败')
      }
    }
  })
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

// 监听来自通知页面的查看自由职业者详情事件
onMounted(() => {
  window.addEventListener('viewFreelancerDetail', (event) => {
    const freelancerId = event.detail?.freelancerId
    if (freelancerId) {
      handleView(freelancerId)
    }
  })
  loadFreelancers()
})
</script>

