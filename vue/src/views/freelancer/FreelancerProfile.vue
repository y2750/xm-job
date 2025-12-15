<template>
  <div class="profile-container">
    <a-card class="profile-card">
      <template #title>
        <div class="card-header">
          <h2 class="card-title">
            <UserOutlined />
            个人资料
          </h2>
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
            <div class="info-section">
              <div class="section-header">
                <IdcardOutlined />
                <span>基本信息</span>
              </div>
              <div class="profile-header">
                <a-avatar :size="80" :src="form.userAvatar" v-if="form.userAvatar && form.userAvatar.trim()" class="profile-avatar">
                  <template #icon><UserOutlined /></template>
                </a-avatar>
                <a-avatar :size="80" v-else class="profile-avatar default">
                  <template #icon><UserOutlined /></template>
                </a-avatar>
                <div class="profile-info">
                  <h3 class="profile-name">{{ form.userName || '未设置姓名' }}</h3>
                  <div class="profile-meta">
                    <a-tag :color="form.verified ? 'success' : 'warning'">
                      {{ form.verified ? '已认证' : '未认证' }}
                    </a-tag>
                    <a-tag color="processing">
                      <StarOutlined />
                      信誉 {{ form.creditScore || 100 }}分
                    </a-tag>
                  </div>
                </div>
              </div>
              <div class="info-list">
                <div class="info-item">
                  <span class="info-label">
                    <PhoneOutlined />
                    电话
                  </span>
                  <span class="info-value">{{ form.userPhone || '未设置' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">
                    <MailOutlined />
                    邮箱
                  </span>
                  <span class="info-value">{{ form.userEmail || '未设置' }}</span>
                </div>
              </div>
            </div>
          </a-col>

          <!-- 职业信息卡片 -->
          <a-col :xs="24" :md="12">
            <div class="info-section">
              <div class="section-header">
                <FileTextOutlined />
                <span>职业信息</span>
              </div>
              <div class="info-list">
                <div class="info-item">
                  <span class="info-label">
                    <TagsOutlined />
                    技能标签
                  </span>
                  <div class="info-value">
                    <div class="skills-tags" v-if="skillList.length > 0">
                      <a-tag v-for="skill in skillList" :key="skill" color="processing">
                        {{ skill }}
                      </a-tag>
                    </div>
                    <span v-else class="empty-text">未设置</span>
                  </div>
                </div>
                <div class="info-item">
                  <span class="info-label">
                    <LinkOutlined />
                    作品集链接
                  </span>
                  <span class="info-value">
                    <a v-if="form.portfolioUrl" :href="form.portfolioUrl" target="_blank" class="portfolio-link">
                      {{ form.portfolioUrl }}
                      <ExportOutlined />
                    </a>
                    <span v-else class="empty-text">未设置</span>
                  </span>
                </div>
                <div class="info-item">
                  <span class="info-label">
                    <FolderOutlined />
                    作品数量
                  </span>
                  <span class="info-value">{{ form.portfolioCount || 0 }} 个</span>
                </div>
                <div class="info-item">
                  <span class="info-label">
                    <TrophyOutlined />
                    信誉分
                  </span>
                  <span class="info-value credit-score-value">
                    <a-progress 
                      :percent="form.creditScore || 100" 
                      :size="[160, 10]"
                      :status="form.creditScore >= 80 ? 'success' : form.creditScore >= 60 ? 'normal' : 'exception'"
                      :stroke-color="{ '0%': '#00a6a7', '100%': '#00c4c4' }"
                      :format="(percent) => `${percent}分`"
                    />
                  </span>
                </div>
              </div>
            </div>
          </a-col>
        </a-row>
      </div>

      <!-- 编辑模式 -->
      <a-form v-else :model="form" :rules="rules" ref="formRef" layout="vertical" class="edit-form">
        <a-row :gutter="24">
          <a-col :xs="24" :md="12">
            <div class="edit-section">
              <div class="section-header">
                <IdcardOutlined />
                <span>基本信息</span>
              </div>
              
              <a-form-item label="姓名" name="userName">
                <a-input v-model:value="form.userName" placeholder="请输入姓名" size="large" />
              </a-form-item>
              
              <a-form-item label="头像" name="userAvatar">
                <div class="avatar-upload">
                  <a-avatar :size="100" :src="form.userAvatar" v-if="form.userAvatar && form.userAvatar.trim()" class="upload-avatar">
                    <template #icon><UserOutlined /></template>
                  </a-avatar>
                  <a-avatar :size="100" v-else class="upload-avatar default">
                    <template #icon><UserOutlined /></template>
                  </a-avatar>
                  <a-upload
                    :show-upload-list="false"
                    :before-upload="beforeUpload"
                    :customRequest="handleUpload"
                    accept="image/*"
                  >
                    <a-button type="primary" :loading="uploading" class="upload-btn">
                      <template #icon><UploadOutlined /></template>
                      上传头像
                    </a-button>
                  </a-upload>
                </div>
              </a-form-item>
              
              <a-form-item label="电话" name="userPhone">
                <a-input v-model:value="form.userPhone" placeholder="请输入联系电话" size="large">
                  <template #prefix><PhoneOutlined class="input-icon" /></template>
                </a-input>
              </a-form-item>
              
              <a-form-item label="邮箱" name="userEmail">
                <a-input v-model:value="form.userEmail" placeholder="请输入邮箱地址" size="large">
                  <template #prefix><MailOutlined class="input-icon" /></template>
                </a-input>
              </a-form-item>
            </div>
          </a-col>

          <a-col :xs="24" :md="12">
            <div class="edit-section">
              <div class="section-header">
                <FileTextOutlined />
                <span>职业信息</span>
              </div>
              
              <a-form-item label="技能标签" name="skills">
                <a-input 
                  v-model:value="form.skills" 
                  placeholder="请输入技能标签，用逗号分隔" 
                  size="large"
                >
                  <template #prefix><TagsOutlined class="input-icon" /></template>
                </a-input>
                <div class="form-tip">多个技能请用逗号分隔（支持中文逗号，和英文逗号,）</div>
              </a-form-item>
              
              <a-form-item label="作品集链接" name="portfolioUrl">
                <a-input v-model:value="form.portfolioUrl" placeholder="请输入作品集链接" size="large">
                  <template #prefix><LinkOutlined class="input-icon" /></template>
                </a-input>
              </a-form-item>
              
              <a-form-item label="作品数量" name="portfolioCount">
                <a-input-number 
                  v-model:value="form.portfolioCount" 
                  :min="0" 
                  placeholder="作品数量"
                  size="large"
                  style="width: 100%"
                />
              </a-form-item>
              
              <a-form-item label="认证状态">
                <a-tag :color="form.verified ? 'success' : 'warning'" class="status-tag">
                  {{ form.verified ? '已认证' : '未认证' }}
                </a-tag>
                <div class="form-tip">认证状态由管理员审核</div>
              </a-form-item>
              
              <a-form-item label="信誉分">
                <div class="credit-display">
                  <a-progress 
                    :percent="form.creditScore || 100" 
                    :size="[180, 10]"
                    :status="form.creditScore >= 80 ? 'success' : form.creditScore >= 60 ? 'normal' : 'exception'"
                    :stroke-color="{ '0%': '#00a6a7', '100%': '#00c4c4' }"
                    :format="(percent) => `${percent}分`"
                  />
                </div>
                <div class="form-tip">信誉分根据项目完成情况自动计算</div>
              </a-form-item>
            </div>
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
  StarOutlined,
  IdcardOutlined,
  PhoneOutlined,
  MailOutlined,
  TagsOutlined,
  FolderOutlined,
  TrophyOutlined,
  ExportOutlined
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

const skillList = computed(() => {
  if (!form.skills || form.skills.trim() === '') {
    return []
  }
  return form.skills.split(/[,，]/).map(s => s.trim()).filter(s => s)
})

const loadProfile = async () => {
  try {
    const res = await request.get('/api/freelancers/profile')
    if (res.code === '200' && res.data) {
      const data = res.data
      form.userName = data.userName || ''
      form.userAvatar = (data.userAvatar && data.userAvatar.trim()) ? data.userAvatar.trim() : ''
      form.userPhone = data.userPhone || ''
      form.userEmail = data.userEmail || ''
      form.skills = data.skills || ''
      form.portfolioUrl = data.portfolioUrl || ''
      form.portfolioCount = data.portfolioCount || 0
      form.verified = data.verified || false
      form.creditScore = data.creditScore || 100
      
      Object.assign(originalForm, { ...form })
    }
  } catch (error) {
    console.error('加载资料失败', error)
    message.error('加载资料失败')
  }
}

const handleEdit = () => {
  isEditMode.value = true
  Object.assign(originalForm, { ...form })
}

const handleCancel = () => {
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
      const user = JSON.parse(localStorage.getItem('xm-user') || '{}')
      if (form.userName) user.name = form.userName
      if (form.userAvatar) user.avatar = form.userAvatar
      if (form.userPhone) user.phone = form.userPhone
      if (form.userEmail) user.email = form.userEmail
      localStorage.setItem('xm-user', JSON.stringify(user))
      
      Object.assign(originalForm, { ...form })
      isEditMode.value = false
    } else {
      message.error(res.msg || '保存失败')
    }
  } catch (error) {
    if (error.errorFields) {
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
  background: var(--bg-secondary);
  min-height: calc(100vh - 140px);
}

.profile-card {
  border-radius: 12px;
  box-shadow: var(--shadow-sm);
}

.profile-card :deep(.ant-card-head) {
  border-bottom: 1px solid var(--border-light);
  padding: 20px 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-title .anticon {
  color: var(--primary-color);
}

/* 查看模式样式 */
.view-mode {
  padding: 8px 0;
}

.info-section {
  background: var(--bg-secondary);
  border-radius: 12px;
  padding: 24px;
  height: 100%;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: var(--primary-color);
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
}

.profile-header {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px dashed var(--border-light);
}

.profile-avatar {
  flex-shrink: 0;
  border: 3px solid var(--primary-color);
  box-shadow: 0 4px 12px rgba(0, 166, 167, 0.2);
}

.profile-avatar.default {
  background: var(--primary-light);
  color: var(--primary-color);
}

.profile-info {
  margin-left: 20px;
}

.profile-name {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 10px 0;
}

.profile-meta {
  display: flex;
  gap: 8px;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-label {
  font-size: 13px;
  color: var(--text-tertiary);
  display: flex;
  align-items: center;
  gap: 6px;
}

.info-value {
  font-size: 15px;
  color: var(--text-primary);
}

.skills-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.empty-text {
  color: var(--text-disabled);
  font-style: italic;
}

.portfolio-link {
  color: var(--primary-color);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  word-break: break-all;
}

.portfolio-link:hover {
  text-decoration: underline;
}

/* 编辑模式样式 */
.edit-form {
  padding: 8px 0;
}

.edit-section {
  background: var(--bg-secondary);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 16px;
}

.avatar-upload {
  display: flex;
  align-items: center;
  gap: 20px;
}

.upload-avatar {
  border: 3px solid var(--primary-color);
  box-shadow: 0 4px 12px rgba(0, 166, 167, 0.2);
}

.upload-avatar.default {
  background: var(--primary-light);
  color: var(--primary-color);
}

.upload-btn {
  border-radius: 6px;
}

.input-icon {
  color: var(--text-tertiary);
}

.form-tip {
  color: var(--text-tertiary);
  font-size: 12px;
  margin-top: 6px;
}

.status-tag {
  font-size: 14px;
  padding: 4px 12px;
}

.credit-display {
  max-width: 200px;
}

.credit-score-value {
  display: flex;
  align-items: center;
}

.credit-score-value :deep(.ant-progress) {
  margin: 0;
}

.credit-score-value :deep(.ant-progress-text) {
  font-size: 14px;
  font-weight: 600;
  color: #00a6a7;
  margin-left: 12px;
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
  
  .profile-header {
    flex-direction: column;
    text-align: center;
  }
  
  .profile-info {
    margin-left: 0;
    margin-top: 16px;
  }
  
  .avatar-upload {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
