<template>
  <div class="profile-container">
    <a-card>
      <template #title>
        <div class="card-header">
          <h2>个人资料</h2>
          <a-space v-if="!isEditMode">
            <a-button type="primary" @click="handleEdit">
              <template #icon><EditOutlined /></template>
              修改资料
            </a-button>
          </a-space>
          <a-space v-else>
            <a-button @click="handleCancel">取消</a-button>
            <a-button type="primary" @click="handleSubmit" :loading="submitting">
              <template #icon><SaveOutlined /></template>
              保存
            </a-button>
          </a-space>
        </div>
      </template>

      <!-- 查看模式 -->
      <div v-if="!isEditMode" class="view-mode">
        <a-row :gutter="[24, 24]">
          <!-- 基本信息卡片 -->
          <a-col :xs="24" :md="12">
            <a-card class="info-card" :bordered="false">
              <template #title>
                <div class="card-title">
                  <UserOutlined />
                  <span>基本信息</span>
                </div>
              </template>
              <a-descriptions :column="1" bordered>
                <a-descriptions-item label="姓名">
                  <span class="info-value">{{ form.userName || '-' }}</span>
                </a-descriptions-item>
                <a-descriptions-item label="头像">
                  <a-avatar :size="64" :src="form.userAvatar" v-if="form.userAvatar && form.userAvatar.trim()">
                    <template #icon><UserOutlined /></template>
                  </a-avatar>
                  <span v-else class="info-empty">未设置</span>
                </a-descriptions-item>
                <a-descriptions-item label="电话">
                  <span class="info-value">{{ form.userPhone || '-' }}</span>
                </a-descriptions-item>
                <a-descriptions-item label="邮箱">
                  <span class="info-value">{{ form.userEmail || '-' }}</span>
                </a-descriptions-item>
              </a-descriptions>
            </a-card>
          </a-col>

          <!-- 职业信息卡片 -->
          <a-col :xs="24" :md="12">
            <a-card class="info-card" :bordered="false">
              <template #title>
                <div class="card-title">
                  <FileTextOutlined />
                  <span>职业信息</span>
                </div>
              </template>
              <a-descriptions :column="1" bordered>
                <a-descriptions-item label="技能标签">
                  <div class="skills-tags">
                    <a-tag v-for="skill in skillList" :key="skill" color="blue">
                      {{ skill }}
                    </a-tag>
                    <span v-if="skillList.length === 0" class="info-empty">未设置</span>
                  </div>
                </a-descriptions-item>
                <a-descriptions-item label="作品集链接">
                  <a v-if="form.portfolioUrl" :href="form.portfolioUrl" target="_blank" class="info-link">
                    {{ form.portfolioUrl }}
                    <LinkOutlined />
                  </a>
                  <span v-else class="info-empty">未设置</span>
                </a-descriptions-item>
                <a-descriptions-item label="作品数量">
                  <span class="info-value">{{ form.portfolioCount || 0 }} 个</span>
                </a-descriptions-item>
                <a-descriptions-item label="认证状态">
                  <a-tag :color="form.verified ? 'green' : 'orange'">
                    {{ form.verified ? '已认证' : '未认证' }}
                  </a-tag>
                </a-descriptions-item>
                <a-descriptions-item label="信誉分">
                  <a-tag color="blue" class="credit-score">
                    <StarOutlined />
                    {{ form.creditScore || 100 }} 分
                  </a-tag>
                </a-descriptions-item>
              </a-descriptions>
            </a-card>
          </a-col>
        </a-row>
      </div>

      <!-- 编辑模式 -->
      <a-form v-else :model="form" :rules="rules" ref="formRef" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-row :gutter="24">
          <a-col :xs="24" :md="12">
            <a-card class="edit-card" :bordered="false">
              <template #title>
                <div class="card-title">
                  <UserOutlined />
                  <span>基本信息</span>
                </div>
              </template>
              
              <a-form-item label="姓名" name="userName">
                <a-input v-model:value="form.userName" placeholder="请输入姓名" />
              </a-form-item>
              
              <a-form-item label="头像" name="userAvatar">
                <div class="avatar-upload">
                  <a-avatar :size="100" :src="form.userAvatar" v-if="form.userAvatar && form.userAvatar.trim()">
                    <template #icon><UserOutlined /></template>
                  </a-avatar>
                  <a-avatar :size="100" v-else>
                    <template #icon><UserOutlined /></template>
                  </a-avatar>
                  <a-upload
                    :show-upload-list="false"
                    :before-upload="beforeUpload"
                    :customRequest="handleUpload"
                    accept="image/*"
                    class="upload-btn"
                  >
                    <a-button type="primary" :loading="uploading">
                      <template #icon><UploadOutlined /></template>
                      上传头像
                    </a-button>
                  </a-upload>
                </div>
              </a-form-item>
              
              <a-form-item label="电话" name="userPhone">
                <a-input v-model:value="form.userPhone" placeholder="请输入联系电话" />
              </a-form-item>
              
              <a-form-item label="邮箱" name="userEmail">
                <a-input v-model:value="form.userEmail" placeholder="请输入邮箱地址" />
              </a-form-item>
            </a-card>
          </a-col>

          <a-col :xs="24" :md="12">
            <a-card class="edit-card" :bordered="false">
              <template #title>
                <div class="card-title">
                  <FileTextOutlined />
                  <span>职业信息</span>
                </div>
              </template>
              
              <a-form-item label="技能标签" name="skills">
                <a-input 
                  v-model:value="form.skills" 
                  placeholder="请输入技能标签，用逗号分隔（如：Java,Spring,Vue 或 Java，Spring，Vue）" 
                />
                <div class="form-tip">多个技能请用逗号分隔（支持中文逗号，和英文逗号,）</div>
              </a-form-item>
              
              <a-form-item label="作品集链接" name="portfolioUrl">
                <a-input v-model:value="form.portfolioUrl" placeholder="请输入作品集链接" />
              </a-form-item>
              
              <a-form-item label="作品数量" name="portfolioCount">
                <a-input-number 
                  v-model:value="form.portfolioCount" 
                  :min="0" 
                  placeholder="作品数量"
                  style="width: 100%"
                />
              </a-form-item>
              
              <a-form-item label="认证状态">
                <a-tag :color="form.verified ? 'green' : 'orange'">
                  {{ form.verified ? '已认证' : '未认证' }}
                </a-tag>
                <div class="form-tip">认证状态由管理员审核</div>
              </a-form-item>
              
              <a-form-item label="信誉分">
                <a-tag color="blue" class="credit-score">
                  <StarOutlined />
                  {{ form.creditScore || 100 }} 分
                </a-tag>
                <div class="form-tip">信誉分根据项目完成情况自动计算</div>
              </a-form-item>
            </a-card>
          </a-col>
        </a-row>
      </a-form>
    </a-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { 
  UserOutlined, 
  UploadOutlined, 
  EditOutlined, 
  SaveOutlined,
  FileTextOutlined,
  LinkOutlined,
  StarOutlined
} from '@ant-design/icons-vue'
import request from '@/utils/request'

const formRef = ref()
const submitting = ref(false)
const uploading = ref(false)
const isEditMode = ref(false)

const form = reactive({
  userName: '',
  userAvatar: '',
  userPhone: '',
  userEmail: '',
  skills: '',
  portfolioUrl: '',
  portfolioCount: 0,
  verified: false,
  creditScore: 100
})

// 保存原始数据用于取消时恢复
const originalForm = reactive({})

const rules = {
  userName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  skills: [{ required: true, message: '请输入技能标签', trigger: 'blur' }],
  userPhone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  userEmail: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

// 计算技能标签列表（支持中文逗号和英文逗号）
const skillList = computed(() => {
  if (!form.skills || form.skills.trim() === '') {
    return []
  }
  // 同时支持中文逗号（，）和英文逗号（,）
  return form.skills.split(/[,，]/).map(s => s.trim()).filter(s => s)
})

const loadProfile = async () => {
  try {
    const res = await request.get('/api/freelancers/profile')
    if (res.code === '200' && res.data) {
      const data = res.data
      form.userName = data.userName || ''
      // 头像字段：如果为 null、undefined 或空字符串，则设为空字符串
      form.userAvatar = (data.userAvatar && data.userAvatar.trim()) ? data.userAvatar.trim() : ''
      form.userPhone = data.userPhone || ''
      form.userEmail = data.userEmail || ''
      form.skills = data.skills || ''
      form.portfolioUrl = data.portfolioUrl || ''
      form.portfolioCount = data.portfolioCount || 0
      form.verified = data.verified || false
      form.creditScore = data.creditScore || 100
      
      // 保存原始数据
      Object.assign(originalForm, { ...form })
    }
  } catch (error) {
    console.error('加载资料失败', error)
    message.error('加载资料失败')
  }
}

const handleEdit = () => {
  isEditMode.value = true
  // 保存当前数据作为原始数据
  Object.assign(originalForm, { ...form })
}

const handleCancel = () => {
  // 恢复原始数据
  Object.assign(form, { ...originalForm })
  isEditMode.value = false
  formRef.value?.resetFields()
}

const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    message.error('只能上传图片文件')
    return false
  }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    message.error('图片大小不能超过5MB')
    return false
  }
  return true
}

const handleUpload = async ({ file }) => {
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    
    const res = await request.post('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    if (res.code === '200') {
      form.userAvatar = res.data
      message.success('头像上传成功')
    } else {
      message.error(res.msg || '头像上传失败')
    }
  } catch (error) {
    console.error('头像上传失败', error)
    message.error('头像上传失败')
  } finally {
    uploading.value = false
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true
    
    const res = await request.put('/api/freelancers/profile', form)
    if (res.code === '200') {
      message.success('保存成功')
      // 更新本地存储的用户信息
      const user = JSON.parse(localStorage.getItem('xm-user') || '{}')
      if (form.userName) user.name = form.userName
      if (form.userAvatar) user.avatar = form.userAvatar
      if (form.userPhone) user.phone = form.userPhone
      if (form.userEmail) user.email = form.userEmail
      localStorage.setItem('xm-user', JSON.stringify(user))
      
      // 更新原始数据
      Object.assign(originalForm, { ...form })
      isEditMode.value = false
    } else {
      message.error(res.msg || '保存失败')
    }
  } catch (error) {
    if (error.errorFields) {
      // 表单验证错误
      return
    }
    console.error('保存失败', error)
    message.error('保存失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
.profile-container {
  padding: 24px;
  background: #f0f2f5;
  min-height: calc(100vh - 64px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #262626;
}

/* 查看模式样式 */
.view-mode {
  padding: 8px 0;
}

.info-card {
  height: 100%;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border-radius: 8px;
  transition: all 0.3s;
}

.info-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1890ff;
}

.card-title .anticon {
  font-size: 18px;
}

.info-value {
  color: #262626;
  font-size: 14px;
}

.info-empty {
  color: #8c8c8c;
  font-style: italic;
}

.info-link {
  color: #1890ff;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.info-link:hover {
  text-decoration: underline;
}

.skills-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.credit-score {
  font-size: 14px;
  font-weight: 500;
}

/* 编辑模式样式 */
.edit-card {
  height: 100%;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border-radius: 8px;
}

.avatar-upload {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 16px;
}

.upload-btn {
  margin-top: 8px;
}

.form-tip {
  color: #8c8c8c;
  font-size: 12px;
  margin-top: 4px;
}

/* 响应式 */
@media (max-width: 768px) {
  .profile-container {
    padding: 16px;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .card-header h2 {
    font-size: 18px;
  }
}
</style>
