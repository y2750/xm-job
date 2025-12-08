<template>
  <div class="profile-container">
    <a-card>
      <template #title>
        <h2>企业资料</h2>
      </template>

      <a-descriptions :column="2" bordered v-if="!editing">
        <a-descriptions-item label="企业名称">{{ enterpriseInfo.employName || '未设置' }}</a-descriptions-item>
        <a-descriptions-item label="企业头像">
          <img v-if="enterpriseInfo.employAvatar" :src="enterpriseInfo.employAvatar" style="width: 60px; height: 60px; border-radius: 4px" />
          <span v-else>未设置</span>
        </a-descriptions-item>
        <a-descriptions-item label="所在城市">{{ enterpriseInfo.employCity || '未设置' }}</a-descriptions-item>
        <a-descriptions-item label="企业地址">{{ enterpriseInfo.employAddress || '未设置' }}</a-descriptions-item>
        <a-descriptions-item label="企业规模">{{ enterpriseInfo.scale || '未设置' }}</a-descriptions-item>
        <a-descriptions-item label="融资阶段">{{ enterpriseInfo.stage || '未设置' }}</a-descriptions-item>
        <a-descriptions-item label="营业执照" :span="2">
          <img 
            v-if="form.businessLicense" 
            :src="form.businessLicense" 
            style="max-width: 150px; max-height: 100px; border: 1px solid #d9d9d9; border-radius: 4px; cursor: pointer; object-fit: contain"
            @click="previewImage = form.businessLicense; previewVisible = true"
          />
          <span v-else>未设置</span>
        </a-descriptions-item>
        <a-descriptions-item label="认证状态">
          <a-tag :color="form.verified ? 'green' : 'orange'">
            {{ form.verified ? '已认证' : '未认证' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="认证时间">{{ formatDate(form.verifiedAt) || '未认证' }}</a-descriptions-item>
        <a-descriptions-item :span="2">
          <a-button type="primary" @click="editing = true">
            <template #icon><EditOutlined /></template>
            修改资料
          </a-button>
        </a-descriptions-item>
      </a-descriptions>

      <a-form :model="form" :rules="rules" ref="formRef" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }" v-else>
        <a-form-item label="企业名称" name="name">
          <a-input v-model:value="form.name" placeholder="请输入企业名称" />
        </a-form-item>
        <a-form-item label="企业头像" name="avatar">
          <a-upload
            v-model:file-list="avatarFileList"
            :before-upload="beforeUploadAvatar"
            :customRequest="handleUploadAvatar"
            list-type="picture-card"
            :max-count="1"
            accept="image/*"
          >
            <div v-if="!form.avatar">
              <PlusOutlined />
              <div style="margin-top: 8px">上传头像</div>
            </div>
          </a-upload>
          <div v-if="form.avatar" style="margin-top: 8px">
            <img :src="form.avatar" style="width: 100px; height: 100px; border: 1px solid #d9d9d9; border-radius: 4px" />
          </div>
        </a-form-item>
        <a-form-item label="所在城市" name="city">
          <a-input v-model:value="form.city" placeholder="请输入所在城市" />
        </a-form-item>
        <a-form-item label="企业地址" name="address">
          <a-input v-model:value="form.address" placeholder="请输入企业地址" />
        </a-form-item>
        <a-form-item label="所属行业" name="industryId">
          <a-select v-model:value="form.industryId" placeholder="请选择所属行业" allow-clear>
            <a-select-option v-for="industry in industryList" :key="industry.id" :value="industry.id">
              {{ industry.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="企业规模" name="scale">
          <a-select v-model:value="form.scale" placeholder="请选择企业规模" allow-clear>
            <a-select-option value="1-50人">1-50人</a-select-option>
            <a-select-option value="50-100人">50-100人</a-select-option>
            <a-select-option value="100-500人">100-500人</a-select-option>
            <a-select-option value="500-1000人">500-1000人</a-select-option>
            <a-select-option value="1000-5000人">1000-5000人</a-select-option>
            <a-select-option value="5000-10000人">5000-10000人</a-select-option>
            <a-select-option value="10000人以上">10000人以上</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="融资阶段" name="stage">
          <a-select v-model:value="form.stage" placeholder="请选择融资阶段" allow-clear>
            <a-select-option value="不需要融资">不需要融资</a-select-option>
            <a-select-option value="种子轮">种子轮</a-select-option>
            <a-select-option value="天使轮">天使轮</a-select-option>
            <a-select-option value="A轮">A轮</a-select-option>
            <a-select-option value="B轮">B轮</a-select-option>
            <a-select-option value="C轮">C轮</a-select-option>
            <a-select-option value="D轮及以上">D轮及以上</a-select-option>
            <a-select-option value="已上市">已上市</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="营业执照" name="businessLicense">
          <a-upload
            v-model:file-list="fileList"
            :before-upload="beforeUpload"
            :customRequest="handleUpload"
            list-type="picture-card"
            :max-count="1"
            accept="image/*"
            @preview="handlePreview"
            @remove="handleRemove"
          >
            <div v-if="fileList.length < 1">
              <PlusOutlined />
              <div style="margin-top: 8px">上传营业执照</div>
            </div>
          </a-upload>
        </a-form-item>
        <a-form-item label="认证状态">
          <a-tag :color="form.verified ? 'green' : 'orange'">
            {{ form.verified ? '已认证' : '未认证' }}
          </a-tag>
          <span v-if="form.verified" style="margin-left: 8px; color: #ff4d4f; font-size: 12px">
            注意：修改资料后将自动取消认证状态，需要重新审核
          </span>
        </a-form-item>
        <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
          <a-button type="primary" @click="handleSubmit" :loading="submitting">保存</a-button>
          <a-button style="margin-left: 8px" @click="handleCancel">取消</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 图片预览模态框 -->
    <a-modal
      v-model:open="previewVisible"
      title="营业执照预览"
      :footer="null"
      :width="800"
      @cancel="previewVisible = false"
    >
      <div style="text-align: center; padding: 20px">
        <img 
          :src="previewImage" 
          style="max-width: 100%; max-height: 600px; border: 1px solid #d9d9d9; border-radius: 4px"
          alt="营业执照"
        />
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { EditOutlined, PlusOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const formRef = ref()
const submitting = ref(false)
const editing = ref(false)
const fileList = ref([])
const avatarFileList = ref([])
const previewVisible = ref(false)
const previewImage = ref('')
const industryList = ref([])
const enterpriseInfo = reactive({
  employName: '',
  employAvatar: '',
  employCity: '',
  employAddress: '',
  scale: '',
  stage: ''
})

const form = reactive({
  name: '',
  avatar: '',
  city: '',
  address: '',
  industryId: null,
  scale: '',
  stage: '',
  businessLicense: '',
  verified: false,
  verifiedAt: null
})

const originalForm = reactive({
  name: '',
  avatar: '',
  city: '',
  address: '',
  industryId: null,
  scale: '',
  stage: '',
  businessLicense: ''
})

const rules = {}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 19)
}

const beforeUpload = (file) => {
  console.log('=== beforeUpload 被调用 ===')
  console.log('文件:', file)
  console.log('文件类型:', file.type)
  console.log('文件大小:', file.size)
  
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    message.error('只能上传图片文件')
    return false
  }
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    message.error('图片大小不能超过10MB')
    return false
  }
  // 使用 customRequest 时，返回 false 会阻止 customRequest 被调用
  // 应该返回 true 或者不返回，让 customRequest 处理上传
  console.log('beforeUpload 验证通过，返回 true 以允许 customRequest 执行')
  return true
}

const beforeUploadAvatar = (file) => {
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
  return true // 使用 customRequest 时，返回 true 以允许上传
}

const handlePreview = (file) => {
  previewImage.value = file.url || file.thumbUrl || form.businessLicense
  previewVisible.value = true
}

const handleRemove = () => {
  form.businessLicense = ''
  fileList.value = []
  return true
}

const handleUpload = async ({ file }) => {
  const formData = new FormData()
  formData.append('file', file)
  
  console.log('=== 开始上传营业执照 ===')
  console.log('当前editing状态:', editing.value)
  console.log('上传前的form.businessLicense:', form.businessLicense)
  
  try {
    const res = await request.post('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    console.log('营业执照上传响应:', res)
    console.log('响应类型:', typeof res)
    console.log('响应code:', res?.code)
    console.log('响应data:', res?.data)
    console.log('响应完整内容:', JSON.stringify(res, null, 2))
    
    // 处理不同的响应格式
    let url = null
    
    // 首先检查标准格式：{code: '200', data: 'url'}
    if (res && res.code === '200') {
      if (res.data) {
        url = res.data
        console.log('✓ 从res.data提取URL:', url)
      } else {
        console.warn('⚠ 响应code为200但data为空')
      }
    }
    // 如果还没有URL，尝试其他格式
    if (!url) {
      if (res && typeof res === 'string') {
        // 字符串格式
        url = res
        console.log('✓ 从字符串格式提取URL:', url)
      } else if (res && res.data) {
        // 只有data字段
        url = res.data
        console.log('✓ 从res.data提取URL（无code）:', url)
      } else if (res && typeof res === 'object' && !res.code) {
        // 直接返回URL对象
        url = res
        console.log('✓ 从对象格式提取URL:', url)
      }
    }
    
    console.log('最终提取的URL:', url)
    console.log('URL类型:', typeof url)
    console.log('URL值:', url)
    console.log('URL是否为空:', !url)
    console.log('URL是否为字符串:', typeof url === 'string')
    console.log('URL trim后是否为空:', url && typeof url === 'string' ? url.trim() === '' : 'N/A')
    
    if (url && typeof url === 'string' && url.trim() !== '') {
      const trimmedUrl = url.trim()
      console.log('=== 准备更新form.businessLicense ===')
      console.log('更新前的值:', form.businessLicense)
      console.log('新值:', trimmedUrl)
      console.log('当前editing状态:', editing.value)
      
      // 直接赋值
      form.businessLicense = trimmedUrl
      
      console.log('更新后的值:', form.businessLicense)
      console.log('form.businessLicense类型:', typeof form.businessLicense)
      console.log('form.businessLicense长度:', form.businessLicense ? form.businessLicense.length : 0)
      
      // 验证是否真的更新了
      if (form.businessLicense === trimmedUrl) {
        console.log('✓ form.businessLicense更新成功')
      } else {
        console.error('✗ form.businessLicense更新失败，值不匹配')
        console.error('期望值:', trimmedUrl)
        console.error('实际值:', form.businessLicense)
      }
      
      // 更新fileList以显示上传的图片
      fileList.value = [{
        uid: '-1',
        name: '营业执照.jpg',
        status: 'done',
        url: trimmedUrl
      }]
      
      // 使用nextTick确保响应式更新
      await nextTick()
      console.log('nextTick后的form.businessLicense:', form.businessLicense)
      console.log('nextTick后的editing状态:', editing.value)
      console.log('完整的form对象:', JSON.parse(JSON.stringify(form)))
      console.log('=== 营业执照上传完成 ===')
      
      message.success('上传成功')
    } else {
      console.error('=== 无法获取上传URL ===')
      console.error('完整响应对象:', JSON.stringify(res, null, 2))
      console.error('提取的URL值:', url)
      console.error('URL类型:', typeof url)
      console.error('URL是否为空:', !url)
      console.error('URL是否为字符串:', typeof url !== 'string')
      message.error('上传失败：无法获取文件URL，请查看控制台日志')
    }
  } catch (error) {
    console.error('上传失败', error)
    message.error('上传失败')
  }
}

const handleUploadAvatar = async ({ file }) => {
  const formData = new FormData()
  formData.append('file', file)
  
  try {
    const res = await request.post('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    if (res.code === '200' || res.data) {
      const url = res.data || res
      form.avatar = url
      message.success('上传成功')
    } else {
      message.error(res.msg || '上传失败')
    }
  } catch (error) {
    console.error('上传失败', error)
    message.error('上传失败')
  }
}

const loadIndustries = async () => {
  try {
    const res = await request.get('/industry/selectAll')
    if (res.code === '200' && res.data) {
      industryList.value = res.data
    }
  } catch (error) {
    console.error('加载行业列表失败', error)
  }
}

const loadProfile = async () => {
  try {
    const res = await request.get('/api/enterprises/profile')
    if (res.code === '200' && res.data) {
      // 如果正在编辑模式，不要覆盖已上传的营业执照
      if (!editing.value) {
        form.businessLicense = res.data.businessLicense || ''
      }
      form.verified = res.data.verified || false
      form.verifiedAt = res.data.verifiedAt || null
      
      // 加载企业基本信息
      enterpriseInfo.employName = res.data.employName || ''
      enterpriseInfo.employAvatar = res.data.employAvatar || ''
      enterpriseInfo.employCity = res.data.employCity || ''
      enterpriseInfo.employAddress = res.data.employAddress || ''
      
      // 需要从employ表获取更多信息
      const userInfo = JSON.parse(localStorage.getItem('xm-user') || '{}')
      if (userInfo.id) {
        try {
          const employRes = await request.get(`/employ/selectById/${userInfo.id}`)
          if (employRes.code === '200' && employRes.data) {
            const employ = employRes.data
            // 如果正在编辑模式，不要覆盖已修改的表单数据
            if (!editing.value) {
              // 填充表单数据
              form.name = employ.name || ''
              form.avatar = employ.avatar || ''
              form.city = employ.city || ''
              form.address = employ.address || ''
              form.industryId = employ.industryId || null
              form.scale = employ.scale || ''
              form.stage = employ.stage || ''
            }
            
            // 如果已有营业执照，更新fileList以显示图片（只在非编辑模式或fileList为空时）
            if (form.businessLicense && fileList.value.length === 0) {
              fileList.value = [{
                uid: '-1',
                name: '营业执照.jpg',
                status: 'done',
                url: form.businessLicense
              }]
            }
            
            // 保存原始值用于重置（只在非编辑模式时更新）
            if (!editing.value) {
              originalForm.name = form.name
              originalForm.avatar = form.avatar
              originalForm.city = form.city
              originalForm.address = form.address
              originalForm.industryId = form.industryId
              originalForm.scale = form.scale
              originalForm.stage = form.stage
              originalForm.businessLicense = form.businessLicense
            }
            
            // 显示用数据
            enterpriseInfo.scale = employ.scale || ''
            enterpriseInfo.stage = employ.stage || ''
          }
        } catch (error) {
          console.error('加载企业详细信息失败', error)
        }
      }
    }
  } catch (error) {
    console.error('加载资料失败', error)
  }
}

const handleSubmit = async () => {
  try {
    submitting.value = true
    
    // 更新Employ表
    const userInfo = JSON.parse(localStorage.getItem('xm-user') || '{}')
    if (userInfo.id) {
      await request.put('/employ/update', {
        id: userInfo.id,
        name: form.name,
        avatar: form.avatar,
        city: form.city,
        address: form.address,
        industryId: form.industryId,
        scale: form.scale,
        stage: form.stage
      })
    }
    
    // 更新Enterprise表
    console.log('准备发送营业执照数据:', form.businessLicense)
    console.log('form.businessLicense类型:', typeof form.businessLicense)
    console.log('form对象完整内容:', JSON.parse(JSON.stringify(form)))
    
    // 检查营业执照
    // 允许保留原有值（包括图片URL或文本），但如果为空则提示
    const businessLicenseValue = form.businessLicense ? String(form.businessLicense).trim() : ''
    
    if (!businessLicenseValue) {
      console.warn('营业执照为空')
      message.warning('请先上传营业执照图片')
      submitting.value = false
      return
    }
    
    console.log('营业执照验证通过，准备发送:', businessLicenseValue)
    
    const res = await request.put('/api/enterprises/profile', {
      businessLicense: form.businessLicense
    })
    console.log('后端返回结果:', res)
    if (res.code === '200') {
      message.success('保存成功，如已认证状态将被取消，需要重新审核')
      editing.value = false
      loadProfile()
    } else {
      message.error(res.msg || '保存失败')
    }
  } catch (error) {
    console.error('保存失败', error)
    message.error('保存失败')
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => {
  form.name = originalForm.name
  form.avatar = originalForm.avatar
  form.city = originalForm.city
  form.address = originalForm.address
  form.industryId = originalForm.industryId
  form.scale = originalForm.scale
  form.stage = originalForm.stage
  form.businessLicense = originalForm.businessLicense
  fileList.value = []
  avatarFileList.value = []
  editing.value = false
}

onMounted(() => {
  loadIndustries()
  loadProfile()
})
</script>

<style scoped>
.profile-container {
  padding: 20px;
}
</style>

