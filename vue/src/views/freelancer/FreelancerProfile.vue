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
                <div class="info-item" v-if="form.experienceLevel">
                  <span class="info-label">
                    <TrophyOutlined />
                    经验等级
                  </span>
                  <span class="info-value">
                    <a-tag :color="getExperienceLevelColor(form.experienceLevel)">
                      {{ getExperienceLevelText(form.experienceLevel) }}
                    </a-tag>
                  </span>
                </div>
              </div>
            </div>
          </a-col>
        </a-row>

        <!-- 证书管理 -->
        <a-row :gutter="[24, 24]" style="margin-top: 24px">
          <a-col :xs="24">
            <div class="info-section">
              <div class="section-header">
                <FileTextOutlined />
                <span>证书管理</span>
                <a-button type="link" @click="showCertificateModal = true" style="margin-left: auto">
                  <PlusOutlined /> 添加证书
                </a-button>
              </div>
              <div v-if="certificates.length > 0" class="certificate-list">
                <a-list :data-source="certificates" :grid="{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 3, xl: 3, xxl: 3 }">
                  <template #renderItem="{ item }">
                    <a-list-item>
                      <a-card hoverable>
                        <template #cover v-if="item.certificateUrl">
                          <img :src="item.certificateUrl" :alt="item.certificateName" style="height: 150px; object-fit: cover" />
                        </template>
                        <a-card-meta>
                          <template #title>
                            <div style="display: flex; justify-content: space-between; align-items: center">
                              <span>{{ item.certificateName }}</span>
                              <a-tag v-if="item.verified" color="green">已验证</a-tag>
                              <a-tag v-else color="orange">未验证</a-tag>
                            </div>
                          </template>
                          <template #description>
                            <div style="font-size: 12px; color: #999">
                              <div>类型：{{ getCertificateTypeText(item.certificateType) }}</div>
                              <div v-if="item.issuingOrganization">机构：{{ item.issuingOrganization }}</div>
                              <div v-if="item.issueDate">颁发日期：{{ item.issueDate }}</div>
                            </div>
                          </template>
                        </a-card-meta>
                        <template #actions>
                          <a-button type="link" danger @click="handleDeleteCertificate(item.id)">删除</a-button>
                        </template>
                      </a-card>
                    </a-list-item>
                  </template>
                </a-list>
              </div>
              <a-empty v-else description="暂无证书，点击上方按钮添加" />
            </div>
          </a-col>
        </a-row>

        <!-- 历史战绩 -->
        <a-row :gutter="[24, 24]" style="margin-top: 24px">
          <a-col :xs="24">
            <div class="info-section">
              <div class="section-header">
                <TrophyOutlined />
                <span>历史战绩</span>
              </div>
              <div v-if="projectHistory.length > 0" class="history-list">
                <a-timeline>
                  <a-timeline-item v-for="item in projectHistory" :key="item.id">
                    <div class="history-item">
                      <div class="history-header">
                        <h4>{{ item.projectTitle }}</h4>
                        <a-space>
                          <a-tag :color="item.status === 'COMPLETED' ? 'green' : 'red'">
                            {{ item.status === 'COMPLETED' ? '已完成' : '已取消' }}
                          </a-tag>
                          <a-button type="link" size="small" @click="handleViewProjectDetail(item.projectId)">查看详情</a-button>
                        </a-space>
                      </div>
                      <div class="history-content">
                        <div><strong>成交价格：</strong>¥{{ item.projectBudget || 0 }}</div>
                        <div v-if="item.completionDate"><strong>完成时间：</strong>{{ item.completionDate }}</div>
                        <div v-if="item.enterpriseRating">
                          <strong>企业评分：</strong>
                          <a-rate :value="item.enterpriseRating" disabled allow-half />
                          ({{ item.enterpriseRating }}分)
                        </div>
                        <div v-if="item.enterpriseComment" style="margin-top: 8px; color: #666">
                          <strong>评价：</strong>{{ item.enterpriseComment }}
                        </div>
                      </div>
                    </div>
                  </a-timeline-item>
                </a-timeline>
              </div>
              <a-empty v-else description="暂无历史战绩" />
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

    <!-- 添加证书弹窗 -->
    <a-modal
      v-model:open="showCertificateModal"
      title="添加证书"
      @ok="handleAddCertificate"
      @cancel="handleCancelCertificate"
    >
      <a-form :model="certificateForm" layout="vertical">
        <a-form-item label="证书类型" required>
          <a-select v-model:value="certificateForm.certificateType">
            <a-select-option value="DEGREE">学历证书</a-select-option>
            <a-select-option value="PROFESSIONAL">职业证书</a-select-option>
            <a-select-option value="AWARD">获奖证书</a-select-option>
            <a-select-option value="OTHER">其他</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="证书名称" required>
          <a-input v-model:value="certificateForm.certificateName" placeholder="请输入证书名称" />
        </a-form-item>
        <a-form-item label="颁发机构">
          <a-input v-model:value="certificateForm.issuingOrganization" placeholder="请输入颁发机构" />
        </a-form-item>
        <a-form-item label="颁发日期">
          <a-date-picker v-model:value="certificateForm.issueDate" style="width: 100%" />
        </a-form-item>
        <a-form-item label="到期日期">
          <a-date-picker v-model:value="certificateForm.expiryDate" style="width: 100%" />
        </a-form-item>
        <a-form-item label="证书文件" required>
          <a-upload
            v-model:file-list="certificateFileList"
            :before-upload="beforeCertificateUpload"
            :custom-request="handleCertificateUpload"
            :max-count="1"
            accept="image/*"
            list-type="picture"
            :show-upload-list="true"
          >
            <a-button :loading="certificateUploading">
              <UploadOutlined /> 上传证书
            </a-button>
            <template #tip>
              <div style="color: #999; font-size: 12px; margin-top: 4px">仅支持图片格式（jpg、png、gif等），选择文件后将自动上传</div>
            </template>
          </a-upload>
          <div v-if="certificateForm.certificateUrl" style="margin-top: 8px; color: #52c41a; font-size: 12px">
            ✓ 证书已上传
          </div>
        </a-form-item>
      </a-form>
    </a-modal>
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
  ExportOutlined,
  PlusOutlined
} from '@ant-design/icons-vue'
import request from '@/utils/request'
import dayjs from 'dayjs'

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
  creditScore: 100,
  experienceLevel: ''
})

const certificates = ref([])
const projectHistory = ref([])
const showCertificateModal = ref(false)
const certificateForm = reactive({
  certificateType: 'DEGREE',
  certificateName: '',
  issuingOrganization: '',
  issueDate: null,
  expiryDate: null,
  certificateUrl: ''
})
const certificateFileList = ref([])
const certificateUploading = ref(false)

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

const getExperienceLevelColor = (level) => {
  const colors = {
    'NEWBIE': 'orange',
    'JUNIOR': 'blue',
    'SENIOR': 'purple',
    'EXPERT': 'red'
  }
  return colors[level] || 'default'
}

const getExperienceLevelText = (level) => {
  const texts = {
    'NEWBIE': '新手',
    'JUNIOR': '初级',
    'SENIOR': '高级',
    'EXPERT': '专家'
  }
  return texts[level] || level
}

const getCertificateTypeText = (type) => {
  const texts = {
    'DEGREE': '学历证书',
    'PROFESSIONAL': '职业证书',
    'AWARD': '获奖证书',
    'OTHER': '其他'
  }
  return texts[type] || type
}

const loadCertificates = async () => {
  try {
    const res = await request.get('/api/certificates/my')
    if (res.code === '200') {
      certificates.value = res.data || []
    }
  } catch (error) {
    console.error('加载证书失败:', error)
    certificates.value = []
  }
}

const loadProjectHistory = async () => {
  try {
    const res = await request.get('/api/project-history/my')
    if (res.code === '200') {
      projectHistory.value = res.data || []
    }
  } catch (error) {
    console.error('加载历史战绩失败:', error)
    projectHistory.value = []
  }
}

const handleViewProjectDetail = (projectId) => {
  if (projectId) {
    router.push(`/front/projects/${projectId}`)
  }
}

const handleDeleteCertificate = async (id) => {
  try {
    const res = await request.delete(`/api/certificates/${id}`)
    if (res.code === '200') {
      message.success('删除成功')
      loadCertificates()
    } else {
      message.error(res.msg || '删除失败')
    }
  } catch (error) {
    console.error('删除证书失败:', error)
    message.error('删除失败，请重试')
  }
}

const beforeCertificateUpload = (file) => {
  console.log('beforeCertificateUpload 被调用:', file.name, file.type)
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    message.error('只能上传图片文件！')
    return false
  }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    message.error('图片大小不能超过 5MB!')
    return false
  }
  // 返回true允许文件添加到列表，然后custom-request会立即执行上传
  console.log('beforeCertificateUpload 返回 true，文件将被添加到列表')
  return true
}

const handleCertificateUpload = async ({ file, onSuccess, onError }) => {
  console.log('handleCertificateUpload 被调用，开始上传:', file.name, file.type, file.size)
  
  // 再次验证文件类型
  if (!file.type || !file.type.startsWith('image/')) {
    message.error('只能上传图片文件！')
    onError()
    // 从文件列表中移除
    const index = certificateFileList.value.findIndex(item => item.uid === file.uid)
    if (index > -1) {
      certificateFileList.value.splice(index, 1)
    }
    return
  }
  
  certificateUploading.value = true
  
  // 更新文件状态为上传中（文件已通过beforeUpload添加到列表）
  const fileIndex = certificateFileList.value.findIndex(item => item.uid === file.uid)
  console.log('文件索引:', fileIndex, '文件列表:', certificateFileList.value)
  if (fileIndex > -1) {
    certificateFileList.value[fileIndex].status = 'uploading'
  } else {
    // 如果文件不在列表中，手动添加
    console.log('文件不在列表中，手动添加')
    certificateFileList.value = [{
      uid: file.uid || Date.now(),
      name: file.name,
      status: 'uploading',
      url: undefined
    }]
  }
  
  try {
    const formData = new FormData()
    formData.append('file', file)
    
    console.log('开始上传证书文件到COS:', file.name, file.type, file.size)
    
    // 不要手动设置Content-Type，让request拦截器自动处理FormData
    const res = await request.post('/files/upload', formData)
    
    console.log('证书上传响应:', res)
    
    if (res.code === '200' && res.data) {
      const uploadedUrl = res.data
      // 确保URL被正确设置到reactive对象
      certificateForm.certificateUrl = uploadedUrl
      
      console.log('证书上传成功，URL已设置:', uploadedUrl)
      console.log('certificateForm对象:', JSON.parse(JSON.stringify(certificateForm)))
      console.log('certificateForm.certificateUrl:', certificateForm.certificateUrl)
      
      // 更新文件列表显示
      if (fileIndex > -1) {
        certificateFileList.value[fileIndex].status = 'done'
        certificateFileList.value[fileIndex].url = uploadedUrl
      }
      
      certificateUploading.value = false
      onSuccess()
      message.success('证书上传成功')
    } else {
      certificateUploading.value = false
      // 更新文件状态为错误
      if (fileIndex > -1) {
        certificateFileList.value[fileIndex].status = 'error'
      }
      onError()
      message.error(res.msg || '上传失败')
    }
  } catch (error) {
    console.error('证书上传失败:', error)
    certificateUploading.value = false
    // 更新文件状态为错误
    const errorFileIndex = certificateFileList.value.findIndex(item => item.uid === file.uid)
    if (errorFileIndex > -1) {
      certificateFileList.value[errorFileIndex].status = 'error'
    }
    onError()
    message.error('上传失败，请重试: ' + (error.message || '未知错误'))
  }
}

const handleCancelCertificate = () => {
  // 重置表单
  Object.assign(certificateForm, {
    certificateType: 'DEGREE',
    certificateName: '',
    issuingOrganization: '',
    issueDate: null,
    expiryDate: null,
    certificateUrl: ''
  })
  certificateFileList.value = []
  certificateUploading.value = false
  showCertificateModal.value = false
}

const handleAddCertificate = async () => {
  if (!certificateForm.certificateName || !certificateForm.certificateName.trim()) {
    message.error('请输入证书名称')
    return
  }
  
  // 检查是否正在上传
  if (certificateUploading.value) {
    message.warning('证书正在上传中，请稍候...')
    return
  }
  
  // 验证证书文件URL
  console.log('检查证书URL - certificateForm:', JSON.parse(JSON.stringify(certificateForm)))
  console.log('检查证书URL - certificateForm.certificateUrl:', certificateForm.certificateUrl)
  console.log('检查证书URL - certificateFileList:', certificateFileList.value)
  
  if (!certificateForm.certificateUrl || !certificateForm.certificateUrl.trim()) {
    message.error('请上传证书文件')
    return
  }
  // 验证证书URL是否为图片
  const imageExtensions = /\.(jpg|jpeg|png|gif|bmp|webp)(\?|$)/i
  if (!imageExtensions.test(certificateForm.certificateUrl)) {
    message.error('证书文件必须是图片格式')
    return
  }
  
  console.log('提交证书数据:', {
    ...certificateForm,
    certificateUrl: certificateForm.certificateUrl
  })
  
  try {
    const submitData = {
      ...certificateForm,
      issueDate: certificateForm.issueDate ? dayjs(certificateForm.issueDate).format('YYYY-MM-DD') : null,
      expiryDate: certificateForm.expiryDate ? dayjs(certificateForm.expiryDate).format('YYYY-MM-DD') : null
    }
    const res = await request.post('/api/certificates', submitData)
    if (res.code === '200') {
      message.success('添加证书成功')
      showCertificateModal.value = false
      Object.assign(certificateForm, {
        certificateType: 'DEGREE',
        certificateName: '',
        issuingOrganization: '',
        issueDate: null,
        expiryDate: null,
        certificateUrl: ''
      })
      certificateFileList.value = []
      loadCertificates()
    } else {
      message.error(res.msg || '添加失败')
    }
  } catch (error) {
    console.error('添加证书失败:', error)
    message.error('添加失败，请重试')
  }
}

onMounted(() => {
  loadProfile()
  loadCertificates()
  loadProjectHistory()
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
