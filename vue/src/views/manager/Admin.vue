<template>
  <a-card>
    <template #title>
      <h3>管理员信息管理</h3>
    </template>
    
    <a-button type="primary" style="margin-bottom: 20px" @click="handleAdd">新增管理员</a-button>

    <!-- 管理员列表 -->
    <a-table
      :columns="columns"
      :data-source="adminList"
      :pagination="pagination"
      :loading="loading"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-button type="link" @click="handleEdit(record)">编辑</a-button>
          <a-button type="link" danger @click="handleDelete(record.id)" :disabled="record.id === currentUserId">删除</a-button>
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
        <a-form-item label="账号" name="username">
          <a-input v-model:value="form.username" :disabled="!!form.id" placeholder="请输入账号" />
        </a-form-item>
        <a-form-item label="姓名" name="name">
          <a-input v-model:value="form.name" placeholder="请输入姓名" />
        </a-form-item>
        <a-form-item label="电话" name="phone">
          <a-input v-model:value="form.phone" placeholder="请输入电话" />
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="form.email" placeholder="请输入邮箱" />
        </a-form-item>
        <a-form-item v-if="!form.id" label="密码" name="password">
          <a-input-password v-model:value="form.password" placeholder="留空则使用默认密码" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import request from '@/utils/request'

const loading = ref(false)
const adminList = ref([])
const modalVisible = ref(false)
const modalTitle = ref('新增管理员')
const formRef = ref()

const currentUserId = computed(() => {
  const user = JSON.parse(localStorage.getItem('xm-user') || '{}')
  return user.id
})

const form = reactive({
  id: null,
  username: '',
  name: '',
  phone: '',
  email: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total) => `共 ${total} 条`,
  onChange: (page, pageSize) => {
    pagination.current = page
    pagination.pageSize = pageSize
    loadAdmins()
  }
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '账号', dataIndex: 'username', key: 'username' },
  { title: '姓名', dataIndex: 'name', key: 'name' },
  { title: '电话', dataIndex: 'phone', key: 'phone' },
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]

const loadAdmins = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/selectPage', {
      params: {
        pageNum: pagination.current,
        pageSize: pagination.pageSize
      }
    })
    if (res.code === '200') {
      const data = res.data
      adminList.value = data.list || []
      pagination.total = data.total || 0
    } else {
      message.error(res.msg || '加载管理员列表失败')
    }
  } catch (error) {
    console.error('加载管理员列表失败:', error)
    message.error('加载管理员列表失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  Object.assign(form, {
    id: null,
    username: '',
    name: '',
    phone: '',
    email: '',
    password: ''
  })
  modalTitle.value = '新增管理员'
  modalVisible.value = true
}

const handleEdit = (record) => {
  Object.assign(form, {
    id: record.id,
    username: record.username,
    name: record.name,
    phone: record.phone,
    email: record.email,
    password: ''
  })
  modalTitle.value = '编辑管理员'
  modalVisible.value = true
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    if (form.id) {
      // 编辑
      const res = await request.put('/admin/update', {
        id: form.id,
        name: form.name,
        phone: form.phone,
        email: form.email
      })
      if (res.code === '200') {
        message.success('更新成功')
        modalVisible.value = false
        loadAdmins()
      } else {
        message.error(res.msg || '更新失败')
      }
    } else {
      // 新增
      const res = await request.post('/admin/add', form)
      if (res.code === '200') {
        message.success('新增成功')
        modalVisible.value = false
        loadAdmins()
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
    content: '确定要删除此管理员吗？删除后不可恢复。',
    onOk: async () => {
      try {
        const res = await request.delete(`/admin/delete/${id}`)
        if (res.code === '200') {
          message.success('删除成功')
          loadAdmins()
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
  loadAdmins()
})
</script>
