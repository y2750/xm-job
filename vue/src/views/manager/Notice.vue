<template>
  <a-card>
    <template #title>
      <h3>系统公告管理</h3>
    </template>
    
    <a-button type="primary" style="margin-bottom: 20px" @click="handleAdd">新增公告</a-button>

    <!-- 公告列表 -->
    <a-table
      :columns="columns"
      :data-source="noticeList"
      :pagination="pagination"
      :loading="loading"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-button type="link" @click="handleEdit(record)">编辑</a-button>
          <a-button type="link" danger @click="handleDelete(record.id)">删除</a-button>
        </template>
      </template>
    </a-table>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      @ok="handleSubmit"
      @cancel="handleCancel"
    >
      <a-form :model="form" :rules="rules" ref="formRef" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="公告标题" name="title">
          <a-input v-model:value="form.title" placeholder="请输入公告标题" />
        </a-form-item>
        <a-form-item label="公告内容" name="content">
          <a-textarea v-model:value="form.content" :rows="6" placeholder="请输入公告内容" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import request from '@/utils/request'

const loading = ref(false)
const noticeList = ref([])
const modalVisible = ref(false)
const modalTitle = ref('新增公告')
const formRef = ref()
const editingId = ref(null)

const form = reactive({
  title: '',
  content: ''
})

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total) => `共 ${total} 条`,
  onChange: (page, pageSize) => {
    pagination.current = page
    pagination.pageSize = pageSize
    loadNotices()
  }
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '公告标题', dataIndex: 'title', key: 'title' },
  { title: '公告内容', dataIndex: 'content', key: 'content', ellipsis: true },
  { title: '发布时间', dataIndex: 'time', key: 'time' },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]

const loadNotices = async () => {
  loading.value = true
  try {
    const res = await request.get('/notice/selectAll')
    if (res.code === '200') {
      noticeList.value = res.data || []
      pagination.total = noticeList.value.length
    } else {
      message.error(res.msg || '加载公告列表失败')
    }
  } catch (error) {
    console.error('加载公告列表失败:', error)
    message.error('加载公告列表失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  editingId.value = null
  modalTitle.value = '新增公告'
  Object.assign(form, {
    title: '',
    content: ''
  })
  modalVisible.value = true
}

const handleEdit = (record) => {
  editingId.value = record.id
  modalTitle.value = '编辑公告'
  Object.assign(form, {
    title: record.title,
    content: record.content
  })
  modalVisible.value = true
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    if (editingId.value) {
      // 编辑
      const res = await request.put('/notice/update', {
        id: editingId.value,
        ...form
      })
      if (res.code === '200') {
        message.success('更新成功')
        modalVisible.value = false
        loadNotices()
      } else {
        message.error(res.msg || '更新失败')
      }
    } else {
      // 新增
      const res = await request.post('/notice/add', form)
      if (res.code === '200') {
        message.success('新增成功')
        modalVisible.value = false
        loadNotices()
      } else {
        message.error(res.msg || '新增失败')
      }
    }
  } catch (error) {
    console.error('提交失败:', error)
  }
}

const handleCancel = () => {
  formRef.value?.resetFields()
  modalVisible.value = false
}

const handleDelete = (id) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除此公告吗？',
    onOk: async () => {
      try {
        const res = await request.delete(`/notice/delete/${id}`)
        if (res.code === '200') {
          message.success('删除成功')
          loadNotices()
        } else {
          message.error(res.msg || '删除失败')
        }
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
}

onMounted(() => {
  loadNotices()
})
</script>
