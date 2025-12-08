<template>
  <div class="chat-container">
    <a-card :bordered="false" class="chat-card">
      <template #title>
        <div class="chat-header">
          <a-button type="text" @click="handleBack" style="margin-right: 8px">
            <template #icon><ArrowLeftOutlined /></template>
          </a-button>
          <span>{{ chatTitle }}</span>
          <a-badge v-if="unreadCount > 0" :count="unreadCount > 99 ? '99+' : unreadCount" :offset="[10, 0]">
            <span></span>
          </a-badge>
        </div>
      </template>

      <!-- 消息列表区域 -->
      <div class="messages-container" ref="messagesContainerRef" @scroll="handleScroll">
        <a-spin :spinning="loadingMessages">
          <div v-if="messageList.length === 0 && !loadingMessages" class="empty-messages">
            <a-empty description="暂无消息，开始聊天吧~" />
          </div>
          <div v-else class="messages-list">
            <div
              v-for="item in messageList"
              :key="item.id"
              :class="['message-item', { 'message-right': isMyMessage(item) }]"
            >
              <div class="message-avatar">
                <a-avatar :src="item.senderAvatar" :size="40">
                  {{ item.senderName ? item.senderName.charAt(0) : 'U' }}
                </a-avatar>
              </div>
              <div class="message-content-wrapper">
                <div class="message-name">{{ item.senderName || '未知用户' }}</div>
                <div class="message-bubble" :class="{ 'bubble-right': isMyMessage(item) }">
                  <div class="message-text" v-html="formatMessageContent(item.content)"></div>
                  <div class="message-time">{{ formatTime(item.createdAt) }}</div>
                </div>
              </div>
            </div>
          </div>
        </a-spin>
      </div>

      <!-- 输入区域 -->
      <div class="input-area" v-if="!project || project.status !== 'COMPLETED'">
        <div class="input-toolbar">
          <a-button type="text" @click="toggleEmojiPicker" class="toolbar-btn">
            <template #icon><SmileOutlined /></template>
          </a-button>
        </div>
        
        <!-- Emoji选择器 -->
        <div v-if="showEmojiPicker" class="emoji-picker">
          <div class="emoji-categories">
            <a-button
              v-for="category in emojiCategories"
              :key="category.name"
              type="text"
              :class="{ active: currentCategory === category.name }"
              @click="currentCategory = category.name"
            >
              {{ category.icon }}
            </a-button>
          </div>
          <div class="emoji-list">
            <span
              v-for="emoji in getCurrentCategoryEmojis()"
              :key="emoji"
              class="emoji-item"
              @click="insertEmoji(emoji)"
            >
              {{ emoji }}
            </span>
          </div>
        </div>

        <div class="input-wrapper">
          <a-textarea
            v-model:value="messageForm.content"
            :rows="3"
            placeholder="输入消息..."
            :auto-size="{ minRows: 1, maxRows: 4 }"
            @pressEnter="handleSendMessage"
            @keydown.ctrl.enter="handleSendMessage"
            ref="messageInputRef"
          />
          <a-button
            type="primary"
            @click="handleSendMessage"
            :loading="sendingMessage"
            :disabled="!messageForm.content.trim()"
            class="send-btn"
          >
            发送
          </a-button>
        </div>
      </div>
      <div v-else-if="project && project.status === 'COMPLETED'" class="input-area-disabled">
        <a-alert
          message="项目已完成，无法继续聊天"
          type="info"
          show-icon
          :closable="false"
        />
      </div>
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined, SmileOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const loadingMessages = ref(false)
const sendingMessage = ref(false)
const messageList = ref([])
const showEmojiPicker = ref(false)
const currentCategory = ref('smileys')
const messagesContainerRef = ref(null)
const messageInputRef = ref(null)
let refreshTimer = null

const currentUser = ref(JSON.parse(localStorage.getItem('xm-user') || '{}'))
const submission = ref(null)
const project = ref(null)
const lastReadMessageId = ref(0) // 最后一条已读消息的ID
const unreadCount = ref(0) // 未读消息数量

const chatTitle = computed(() => {
  if (submission.value && project.value) {
    return `${project.value.title} - 稿件沟通`
  }
  return '消息沟通'
})

const messageForm = reactive({
  content: ''
})

// Emoji分类和表情
const emojiCategories = [
  { name: 'smileys', icon: '😀', emojis: ['😀', '😃', '😄', '😁', '😆', '😅', '😂', '🤣', '😊', '😇', '🙂', '🙃', '😉', '😌', '😍', '🥰', '😘', '😗', '😙', '😚', '😋', '😛', '😝', '😜', '🤪', '🤨', '🧐', '🤓', '😎', '🤩', '🥳', '😏', '😒', '😞', '😔', '😟', '😕', '🙁', '😣', '😖', '😫', '😩', '🥺', '😢', '😭', '😤', '😠', '😡', '🤬', '🤯', '😳', '🥵', '🥶', '😱', '😨', '😰', '😥', '😓'] },
  { name: 'gestures', icon: '👋', emojis: ['👋', '🤚', '🖐', '✋', '🖖', '👌', '🤏', '✌️', '🤞', '🤟', '🤘', '🤙', '👈', '👉', '👆', '🖕', '👇', '☝️', '👍', '👎', '✊', '👊', '🤛', '🤜', '👏', '🙌', '👐', '🤲', '🤝', '🙏', '✍️', '💪', '🦾', '🦿', '🦵', '🦶', '👂', '🦻', '👃'] },
  { name: 'people', icon: '👤', emojis: ['👤', '👥', '🧑', '👨', '👩', '👧', '👦', '👶', '👴', '👵', '🧓', '👱', '👱‍♀️', '👱‍♂️', '🧔', '👲', '🧕', '👳', '👳‍♀️', '👳‍♂️', '👮', '👮‍♀️', '👮‍♂️', '👷', '👷‍♀️', '👷‍♂️'] },
  { name: 'symbols', icon: '❤️', emojis: ['❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '💔', '❣️', '💕', '💞', '💓', '💗', '💖', '💘', '💝', '💟', '☮️', '✝️', '☪️', '🕉', '☸️', '✡️', '🔯', '🕎', '☯️', '☦️', '🛐', '⛎', '♈', '♉', '♊', '♋', '♌', '♍', '♎', '♏', '♐', '♑', '♒', '♓', '🆔', '⚛️', '🉑', '☢️', '☣️'] }
]

const getCurrentCategoryEmojis = () => {
  const category = emojiCategories.find(c => c.name === currentCategory.value)
  return category ? category.emojis : []
}

const isMyMessage = (msg) => {
  return msg.senderId === currentUser.value.id
}

const formatMessageContent = (content) => {
  if (!content) return ''
  // 将换行符转换为<br>
  return content.replace(/\n/g, '<br>')
}

const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr.replace('T', ' ').replace(/-/g, '/'))
  const now = new Date()
  const diff = now - date
  
  // 今天
  if (diff < 24 * 60 * 60 * 1000 && date.getDate() === now.getDate()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  // 昨天
  if (diff < 48 * 60 * 60 * 1000 && date.getDate() === now.getDate() - 1) {
    return '昨天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  // 更早
  return date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const loadSubmission = async () => {
  try {
    const res = await request.get(`/api/submissions/${route.params.submissionId}`)
    if (res.code === '200') {
      submission.value = res.data
      if (submission.value && submission.value.projectId) {
        await loadProject(submission.value.projectId)
      }
    }
  } catch (error) {
    console.error('加载稿件失败:', error)
  }
}

const loadProject = async (projectId) => {
  try {
    const res = await request.get(`/api/projects/${projectId}`)
    if (res.code === '200') {
      project.value = res.data
    }
  } catch (error) {
    console.error('加载项目失败:', error)
  }
}

const loadMessages = async () => {
  if (!route.params.submissionId) return
  loadingMessages.value = true
  try {
    const res = await request.get(`/api/messages/submission/${route.params.submissionId}`)
    if (res.code === '200') {
      const newMessages = res.data || []
      const hasNewMessages = newMessages.length !== messageList.value.length || 
                            (newMessages.length > 0 && messageList.value.length > 0 && 
                             newMessages[newMessages.length - 1].id !== messageList.value[messageList.value.length - 1].id)
      
      // 更新消息列表
      messageList.value = newMessages
      
      // 计算未读消息数量（只统计对方发送的未读消息，但不在列表中显示）
      if (newMessages.length > 0) {
        const unreadMessages = newMessages.filter(msg => 
          !isMyMessage(msg) && msg.id > lastReadMessageId.value
        )
        unreadCount.value = unreadMessages.length
        
        // 如果有新消息（无论是自己还是对方发送的），都滚动到底部
        if (hasNewMessages) {
          await nextTick()
          scrollToBottom()
        }
      } else {
        unreadCount.value = 0
      }
    }
  } catch (error) {
    console.error('加载消息失败:', error)
  } finally {
    loadingMessages.value = false
  }
}

// 标记消息为已读（当用户滚动查看消息时）
const markMessagesAsRead = () => {
  if (messageList.value.length > 0) {
    const lastMessage = messageList.value[messageList.value.length - 1]
    if (lastMessage.id > lastReadMessageId.value) {
      lastReadMessageId.value = lastMessage.id
      unreadCount.value = 0
    }
  }
}

const scrollToBottom = () => {
  if (messagesContainerRef.value) {
    messagesContainerRef.value.scrollTop = messagesContainerRef.value.scrollHeight
    // 滚动到底部时标记为已读
    markMessagesAsRead()
  }
}

// 处理滚动事件，当滚动到底部时标记消息为已读
const handleScroll = () => {
  if (messagesContainerRef.value) {
    const container = messagesContainerRef.value
    const isAtBottom = container.scrollHeight - container.scrollTop - container.clientHeight < 50
    if (isAtBottom) {
      markMessagesAsRead()
    }
  }
}

const handleSendMessage = async () => {
  if (!messageForm.content.trim()) {
    message.warning('请输入消息内容')
    return
  }

  if (!route.params.submissionId) {
    message.error('稿件ID不存在')
    return
  }
  
  // 检查项目状态，如果已完成，不允许发送消息
  if (project.value && project.value.status === 'COMPLETED') {
    message.warning('项目已完成，无法继续聊天')
    return
  }

  sendingMessage.value = true
  try {
    const res = await request.post('/api/messages', {
      submissionId: parseInt(route.params.submissionId),
      projectId: submission.value?.projectId,
      content: messageForm.content.trim()
    })
    if (res.code === '200') {
      messageForm.content = ''
      showEmojiPicker.value = false
      // 立即刷新消息列表
      await loadMessages()
      // 聚焦输入框
      await nextTick()
      if (messageInputRef.value) {
        messageInputRef.value.focus()
      }
    } else {
      message.error(res.msg)
    }
  } catch (error) {
    message.error('发送失败')
  } finally {
    sendingMessage.value = false
  }
}

const toggleEmojiPicker = () => {
  showEmojiPicker.value = !showEmojiPicker.value
}

const insertEmoji = (emoji) => {
  if (messageInputRef.value) {
    const textarea = messageInputRef.value.$el.querySelector('textarea')
    if (textarea) {
      const start = textarea.selectionStart
      const end = textarea.selectionEnd
      const text = messageForm.content
      messageForm.content = text.substring(0, start) + emoji + text.substring(end)
      // 设置光标位置
      nextTick(() => {
        textarea.focus()
        textarea.setSelectionRange(start + emoji.length, start + emoji.length)
      })
    } else {
      messageForm.content += emoji
    }
  } else {
    messageForm.content += emoji
  }
}

const handleBack = () => {
  router.back()
}

// 自动刷新消息（每3秒）
const startAutoRefresh = () => {
  refreshTimer = setInterval(() => {
    loadMessages()
  }, 3000)
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(async () => {
  await loadSubmission()
  await loadMessages()
  // 初始化最后已读消息ID（设置为当前最后一条消息的ID）
  if (messageList.value.length > 0) {
    lastReadMessageId.value = messageList.value[messageList.value.length - 1].id
    unreadCount.value = 0
  }
  startAutoRefresh()
  // 初始滚动到底部
  await nextTick()
  scrollToBottom()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.chat-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
}

.chat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  margin: 0;
}

.chat-card :deep(.ant-card-body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  font-size: 16px;
  font-weight: 500;
  position: relative;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f5f5;
}

.empty-messages {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.message-item.message-right {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content-wrapper {
  display: flex;
  flex-direction: column;
  max-width: 70%;
  min-width: 0;
}

.message-item.message-right .message-content-wrapper {
  align-items: flex-end;
}

.message-name {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
  padding: 0 8px;
}

.message-item.message-right .message-name {
  text-align: right;
}

.message-bubble {
  background: #fff;
  border-radius: 8px;
  padding: 10px 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  word-wrap: break-word;
  word-break: break-word;
  display: inline-block;
  max-width: 100%;
  min-width: 60px;
}

.message-bubble.bubble-right {
  background: #1890ff;
  color: #fff;
}

.message-text {
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 4px;
}

.message-bubble.bubble-right .message-text {
  color: #fff;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.message-bubble.bubble-right .message-time {
  color: rgba(255, 255, 255, 0.8);
}

.input-area {
  border-top: 1px solid #e8e8e8;
  background: #fff;
  padding: 12px;
}

.input-area-disabled {
  border-top: 1px solid #e8e8e8;
  background: #f5f5f5;
  padding: 12px;
}

.input-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.toolbar-btn {
  font-size: 20px;
  padding: 4px 8px;
}

.emoji-picker {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  background: #fff;
  margin-bottom: 8px;
  max-height: 200px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.emoji-categories {
  display: flex;
  gap: 4px;
  padding: 8px;
  border-bottom: 1px solid #e8e8e8;
  background: #fafafa;
}

.emoji-categories .ant-btn {
  font-size: 18px;
  padding: 4px 8px;
  border: none;
}

.emoji-categories .ant-btn.active {
  background: #e6f7ff;
}

.emoji-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.emoji-item {
  font-size: 24px;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: background 0.2s;
  user-select: none;
}

.emoji-item:hover {
  background: #f0f0f0;
}

.input-wrapper {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.input-wrapper :deep(.ant-input) {
  flex: 1;
  resize: none;
}

.send-btn {
  height: auto;
  padding: 4px 16px;
}
</style>

