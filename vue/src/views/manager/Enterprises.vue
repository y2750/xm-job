<template>
  <a-card>
    <template #title>
      <h3>企业用户管理</h3>
    </template>
    
    <!-- 搜索筛选 -->
    <a-form :model="searchForm" layout="inline" style="margin-bottom: 20px">
      <a-form-item label="关键词">
        <a-input v-model:value="searchForm.name" placeholder="企业名称" allow-clear style="width: 200px" />
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

    <!-- 企业列表 -->
    <a-table
      :columns="columns"
      :data-source="enterpriseList"
      :pagination="pagination"
      :loading="loading"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'verified'">
          <a-tag :color="record.verified ? 'green' : 'default'">{{ record.verified ? '已认证' : '未认证' }}</a-tag>
        </template>
        <template v-if="column.key === 'businessLicense'">
          <img 
            v-if="record.businessLicense" 
            :src="record.businessLicense" 
            style="max-width: 100px; max-height: 60px; border: 1px solid #d9d9d9; border-radius: 4px; cursor: pointer; object-fit: contain"
            @click="previewImage = record.businessLicense; previewVisible = true"
          />
          <span v-else style="color: #999">未上传</span>
        </template>
        <template v-if="column.key === 'action'">
          <a-button type="link" @click="handleView(record.id)">查看</a-button>
          <a-button v-if="!record.verified" type="link" @click="handleVerify(record.id)">认证</a-button>
          <a-button v-if="record.verified" type="link" danger @click="handleUnverify(record.id)">取消认证</a-button>
        </template>
      </template>
    </a-table>
  </a-card>

  <!-- 企业详情弹窗 -->
  <a-modal
    v-model:open="detailVisible"
    title="企业详情"
    :footer="null"
    width="800px"
  >
    <a-descriptions v-if="enterpriseDetail" bordered :column="2">
      <a-descriptions-item label="企业ID">{{ enterpriseDetail.id }}</a-descriptions-item>
      <a-descriptions-item label="企业名称">{{ enterpriseDetail.employName }}</a-descriptions-item>
      <a-descriptions-item label="城市">{{ enterpriseDetail.employCity }}</a-descriptions-item>
      <a-descriptions-item label="地址">{{ enterpriseDetail.employAddress }}</a-descriptions-item>
      <a-descriptions-item label="营业执照" :span="2">
        <img 
          v-if="enterpriseDetail.businessLicense" 
          :src="enterpriseDetail.businessLicense" 
          style="max-width: 200px; max-height: 150px; border: 1px solid #d9d9d9; border-radius: 4px; cursor: pointer; object-fit: contain"
          @click="previewImage = enterpriseDetail.businessLicense; previewVisible = true"
        />
        <span v-else>未填写</span>
      </a-descriptions-item>
      <a-descriptions-item label="认证状态">
        <a-tag :color="enterpriseDetail.verified ? 'green' : 'default'">
          {{ enterpriseDetail.verified ? '已认证' : '未认证' }}
        </a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="认证时间" :span="2">
        {{ enterpriseDetail.verifiedAt || '未认证' }}
      </a-descriptions-item>
      <a-descriptions-item label="创建时间" :span="2">
        {{ enterpriseDetail.createdAt }}
      </a-descriptions-item>
      <a-descriptions-item label="更新时间" :span="2">
        {{ enterpriseDetail.updatedAt }}
      </a-descriptions-item>
    </a-descriptions>
  </a-modal>

  <!-- 营业执照预览弹窗 -->
  <a-modal
    v-model:open="previewVisible"
    title="营业执照预览"
    :footer="null"
    :width="800"
  >
    <img :src="previewImage" alt="营业执照" style="width: 100%; height: auto" />
  </a-modal>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import request from '@/utils/request'

const loading = ref(false)
const enterpriseList = ref([])

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
    loadEnterprises()
  }
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '企业名称', dataIndex: 'employName', key: 'employName' },
  { title: '城市', dataIndex: 'employCity', key: 'employCity' },
  { title: '地址', dataIndex: 'employAddress', key: 'employAddress', ellipsis: true },
  { title: '营业执照', dataIndex: 'businessLicense', key: 'businessLicense', ellipsis: true },
  { title: '认证状态', key: 'verified', width: 120 },
  { title: '认证时间', dataIndex: 'verifiedAt', key: 'verifiedAt' },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt' },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]

const loadEnterprises = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/enterprises', {
      params: {
        ...searchForm,
        pageNum: pagination.current,
        pageSize: pagination.pageSize
      }
    })
    if (res.code === '200') {
      const data = res.data
      enterpriseList.value = data.list || []
      pagination.total = data.total || 0
    } else {
      message.error(res.msg || '加载企业列表失败')
    }
  } catch (error) {
    console.error('加载企业列表失败:', error)
    message.error('加载企业列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadEnterprises()
}

const handleReset = () => {
  Object.assign(searchForm, {
    name: '',
    verified: undefined
  })
  handleSearch()
}

const enterpriseDetail = ref(null)
const detailVisible = ref(false)
const previewVisible = ref(false)
const previewImage = ref('')

const handleView = async (id) => {
  try {
    const res = await request.get(`/api/enterprises/${id}`)
    if (res.code === '200') {
      enterpriseDetail.value = res.data
      detailVisible.value = true
    } else {
      message.error(res.msg || '获取企业详情失败')
    }
  } catch (error) {
    console.error('获取企业详情失败:', error)
    message.error('获取企业详情失败')
  }
}

const handleVerify = async (id) => {
  Modal.confirm({
    title: '确认认证',
    content: '确定要认证此企业吗？',
    onOk: async () => {
      try {
        const enterprise = enterpriseList.value.find(e => e.id === id)
        if (enterprise) {
          const res = await request.put('/api/enterprises', {
            id: id,
            verified: true,
            verifiedAt: new Date().toISOString().substring(0, 19).replace('T', ' ')
          })
          if (res.code === '200') {
            message.success('认证成功')
            loadEnterprises()
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
    content: '确定要取消此企业的认证吗？取消认证后，该企业将无法发布项目。',
    onOk: async () => {
      try {
        const res = await request.put('/api/enterprises', {
          id: id,
          verified: false,
          verifiedAt: null
        })
        if (res.code === '200') {
          message.success('取消认证成功')
          loadEnterprises()
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

onMounted(() => {
  loadEnterprises()
})
</script>

