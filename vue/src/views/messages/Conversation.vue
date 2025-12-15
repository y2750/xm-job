<template>
  <div class="conversation-container">
    <!-- 左侧：聊天列表 -->
    <div class="left-panel">
      <div class="chat-list-header">
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="搜索30天内的联系人"
          style="width: 100%"
          @search="handleSearch"
        />
      </div>
      
      <div class="filter-header">
        <a-button type="text" @click="showFilterTabs = !showFilterTabs">
          <template #icon><FilterOutlined /></template>
          筛选
        </a-button>
      </div>
      
      <div v-if="showFilterTabs" class="filter-tabs">
        <a-button
          v-for="tab in filterTabs"
          :key="tab.key"
          :type="activeTab === tab.key ? 'primary' : 'text'"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </a-button>
      </div>

      <div class="chat-list-scroll">
        <a-spin :spinning="loadingChatList">
          <div v-if="chatList.length > 0" class="chat-list">
            <div
              v-for="chat in filteredChatList"
              :key="chat.submissionId || `project_${chat.projectId}`"
              class="chat-item"
              :class="{ active: (chat.submissionId && selectedSubmissionId === chat.submissionId) || (!chat.submissionId && selectedChat && selectedChat.projectId === chat.projectId) }"
              @click="handleSelectChat(chat)"
            >
              <div class="chat-avatar">
                <a-avatar :src="getChatAvatar(chat)" :size="48">
                  {{ getChatName(chat).charAt(0) }}
                </a-avatar>
                <a-badge
                  v-if="getUnreadCount(chat) > 0"
                  :count="getUnreadCount(chat)"
                  :offset="[-5, 5]"
                />
              </div>
              <div class="chat-content">
                <div class="chat-header">
                  <span class="chat-name">{{ getChatName(chat) }}</span>
                  <span class="chat-time">{{ formatTime(chat.createdAt) }}</span>
                </div>
                <div class="chat-meta">
                  <span class="chat-company">{{ getChatCompany(chat) }}</span>
                  <a-tag v-if="isOnline(chat)" color="green" size="small" style="margin-left: 8px">● 在线</a-tag>
                </div>
                <div class="chat-preview" v-html="getPreviewContent(chat.content)"></div>
              </div>
            </div>
          </div>
          <a-empty v-else :description="loadingChatList ? '加载中...' : '暂无聊天记录'" />
        </a-spin>
      </div>
    </div>

    <!-- 右侧：聊天详情 -->
    <div class="right-panel">
      <div v-if="selectedChat" class="chat-detail">
        <!-- 聊天头部信息 -->
        <div class="chat-detail-header">
          <div class="header-left">
            <a-avatar
              :src="getChatAvatar(selectedChat)"
              :size="40"
              @click="handleViewProfile"
              style="cursor: pointer"
            >
              {{ getChatName(selectedChat).charAt(0) }}
            </a-avatar>
            <div class="header-info">
              <div class="header-name">{{ getChatName(selectedChat) }}</div>
              <div class="header-meta">
                <span>{{ getChatCompany(selectedChat) }}</span>
                <a-tag v-if="isOnline(selectedChat)" color="green" size="small" style="margin-left: 8px">● 在线</a-tag>
              </div>
            </div>
          </div>
          <div class="header-right">
            <!-- 企业端不显示发资料按钮 -->
            <a-button type="link" @click="handleViewProject">
              查看项目 >
            </a-button>
          </div>
        </div>

        <a-divider style="margin: 0" />

        <!-- 项目信息 -->
        <div class="project-info-bar">
          <div class="project-title">{{ selectedChat.projectTitle }}</div>
          <div class="project-subtitle" v-if="selectedChat.submissionTitle">
            {{ selectedChat.submissionTitle }}
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="messages-scroll" ref="messagesContainerRef">
          <a-spin :spinning="loadingMessages">
            <div v-if="messageList.length > 0" class="messages-list">
              <div
                v-for="item in messageList"
                :key="item.id"
                :class="['message-item', { 'message-right': isMyMessage(item) }]"
              >
                <div class="message-avatar">
                  <a-avatar
                    :src="item.senderAvatar"
                    :size="36"
                    @click="handleViewSenderProfile(item)"
                    style="cursor: pointer"
                  >
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
            <a-empty v-else :description="loadingMessages ? '加载中...' : '暂无消息，开始聊天吧~'" />
          </a-spin>
        </div>

        <!-- 输入区域 -->
        <div class="input-area" v-if="!project || project.status !== 'COMPLETED'">
          <div class="input-toolbar">
            <a-button type="text" @click="toggleEmojiPicker" class="toolbar-btn">
              <template #icon><SmileOutlined /></template>
            </a-button>
            <a-button type="text" @click="toggleCommonPhrases" class="toolbar-btn">常用语</a-button>
            <a-upload
              :before-upload="beforeUploadImage"
              :customRequest="handleUploadImage"
              :show-upload-list="false"
              accept="image/*"
            >
              <a-button type="text" class="toolbar-btn">
                <template #icon><PictureOutlined /></template>
              </a-button>
            </a-upload>
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
                @click.stop="insertEmoji(emoji)"
              >
                {{ emoji }}
              </span>
            </div>
          </div>
          
          <!-- 常用语选择器 -->
          <div v-if="showCommonPhrases" class="common-phrases-picker">
            <div class="phrases-list">
              <a-button
                v-for="phrase in commonPhrases"
                :key="phrase"
                type="text"
                size="small"
                class="phrase-item"
                @click.stop="insertPhrase(phrase)"
              >
                {{ phrase }}
              </a-button>
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
            <div class="input-actions" v-if="!isEnterprise">
              <a-button type="link" size="small" @click="handleSendProfile">发资料</a-button>
            </div>
            <div class="input-hint">
              按Enter键发送,按Ctrl+Enter键换行
            </div>
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
          <a-alert message="项目已完成，无法继续聊天" type="info" show-icon />
        </div>
      </div>
      <a-empty v-else description="请从左侧选择一个聊天" />
    </div>

    <!-- 自由职业者详情弹窗 -->
    <a-modal
      v-model:open="profileModalVisible"
      title="自由职业者详情"
      width="600px"
      :footer="null"
      v-if="selectedFreelancer"
    >
      <div style="padding: 10px 0">
        <div style="display: flex; align-items: center; margin-bottom: 20px">
          <a-avatar :size="60" :src="selectedFreelancer.userAvatar" v-if="selectedFreelancer.userAvatar">
            <template #icon><UserOutlined /></template>
          </a-avatar>
          <a-avatar :size="60" v-else>
            <template #icon><UserOutlined /></template>
          </a-avatar>
          <div style="margin-left: 15px">
            <h3 style="margin: 0 0 5px 0">{{ selectedFreelancer.userName || '-' }}</h3>
            <a-tag :color="selectedFreelancer.verified ? 'green' : 'orange'">
              {{ selectedFreelancer.verified ? '已认证' : '未认证' }}
            </a-tag>
          </div>
        </div>
        <a-descriptions :column="1" bordered>
          <a-descriptions-item label="姓名">
            {{ selectedFreelancer.userName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="电话">
            {{ selectedFreelancer.userPhone || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="邮箱">
            {{ selectedFreelancer.userEmail || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="技能标签">
            <a-tag v-for="skill in (selectedFreelancer.skills ? selectedFreelancer.skills.split(/[,，]/).filter(s => s.trim()) : [])" :key="skill" style="margin-right: 8px">
              {{ skill }}
            </a-tag>
            <span v-if="!selectedFreelancer.skills">-</span>
          </a-descriptions-item>
          <a-descriptions-item label="作品集链接">
            <a v-if="selectedFreelancer.portfolioUrl" :href="selectedFreelancer.portfolioUrl" target="_blank">
              {{ selectedFreelancer.portfolioUrl }}
            </a>
            <span v-else>-</span>
          </a-descriptions-item>
          <a-descriptions-item label="作品数量">
            {{ selectedFreelancer.portfolioCount || 0 }}
          </a-descriptions-item>
          <a-descriptions-item label="评分">
            {{ selectedFreelancer.rating || '0.00' }}
          </a-descriptions-item>
          <a-descriptions-item label="认证状态">
            <a-tag :color="selectedFreelancer.verified ? 'green' : 'orange'">
              {{ selectedFreelancer.verified ? '已认证' : '未认证' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="信誉分">
            <span style="color: #52c41a; font-weight: bold">{{ selectedFreelancer.creditScore || 100 }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="完成项目数">
            {{ selectedFreelancer.completedProjects || 0 }}
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>

    <!-- 图片预览弹窗 -->
    <a-modal
      v-model:open="imagePreviewVisible"
      :footer="null"
      :width="'auto'"
      :style="{ padding: '0' }"
      :mask-closable="true"
      :closable="true"
      centered
      @cancel="imagePreviewVisible = false"
      wrap-class-name="image-preview-modal"
      :mask="true"
      :mask-style="{ backgroundColor: 'rgba(0, 0, 0, 0.5)' }"
    >
      <div class="image-preview-container">
        <img
          :src="previewImageUrl"
          alt="图片预览"
          class="preview-image"
          ref="previewImageRef"
          @error="handleImageError"
          @load="handleImageLoad"
        />
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { SmileOutlined, PictureOutlined, DownOutlined, UserOutlined, FilterOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const loadingChatList = ref(false)
const loadingMessages = ref(false)
const sendingMessage = ref(false)
const chatList = ref([])
const selectedSubmissionId = ref(null)
const selectedChat = ref(null)
const messageList = ref([])
const searchKeyword = ref('')
const activeTab = ref('all')
const project = ref(null)
const selectedFreelancer = ref(null)
const profileModalVisible = ref(false)
const imagePreviewVisible = ref(false)
const previewImageUrl = ref('')
const previewImageRef = ref(null)

const userRole = ref(localStorage.getItem('xm-user') ? JSON.parse(localStorage.getItem('xm-user')).role : '')
const isEnterprise = computed(() => userRole.value === 'EMPLOY')

const showFilterTabs = ref(false)
const filterTabs = [
  { key: 'all', label: '全部' },
  { key: 'unread', label: '未读' },
  { key: 'submitted', label: '已提交稿件' },
  { key: 'interested', label: '已接单' },
  { key: 'confirmed', label: '已合作' }
]

const messageForm = reactive({
  content: ''
})

const showEmojiPicker = ref(false)
const showCommonPhrases = ref(false)
const currentCategory = ref('smile')
const messagesContainerRef = ref(null)
const messageInputRef = ref(null)

// 自由职业者常用语
const freelancerPhrases = [
  '你好',
  '请问这个项目还在招人吗？',
  '我对这个项目很感兴趣',
  '我可以胜任这个项目',
  '请问项目预算是多少？',
  '我的报价是',
  '谢谢',
  '好的',
  '没问题',
  '稍等',
  '请问还有其他要求吗？'
]

// 企业端常用语
const enterprisePhrases = [
  '你好',
  '请问您对这个项目感兴趣吗？',
  '请介绍一下您的相关经验',
  '您的报价是多少？',
  '请问您什么时候可以开始？',
  '请提供您的作品集',
  '好的，我们考虑一下',
  '谢谢',
  '稍等',
  '请问还有其他问题吗？',
  '期待与您合作'
]

// 根据用户角色返回对应的常用语
const commonPhrases = computed(() => {
  return isEnterprise.value ? enterprisePhrases : freelancerPhrases
})

const emojiCategories = [
  { name: 'smile', icon: '😀' },
  { name: 'gesture', icon: '👍' },
  { name: 'people', icon: '👤' },
  { name: 'nature', icon: '🌳' },
  { name: 'food', icon: '🍎' },
  { name: 'activity', icon: '⚽' },
  { name: 'travel', icon: '🚗' },
  { name: 'objects', icon: '💡' },
  { name: 'symbols', icon: '❤️' }
]

const emojiMap = {
  smile: ['😀', '😃', '😄', '😁', '😆', '😅', '😂', '🤣', '😊', '😇', '🙂', '🙃', '😉', '😌', '😍', '🥰', '😘', '😗', '😙', '😚', '😋', '😛', '😝', '😜', '🤪', '🤨', '🧐', '🤓', '😎', '🤩', '🥳', '😏', '😒', '😞', '😔', '😟', '😕', '🙁', '☹️', '😣', '😖', '😫', '😩', '🥺', '😢', '😭', '😤', '😠', '😡', '🤬', '🤯', '😳', '🥵', '🥶', '😱', '😨', '😰', '😥', '😓', '🤗', '🤔', '🤭', '🤫', '🤥', '😶', '😐', '😑', '😬', '🙄', '😯', '😦', '😧', '😮', '😲', '🥱', '😴', '🤤', '😪', '😵', '🤐', '🥴', '🤢', '🤮', '🤧', '😷', '🤒', '🤕'],
  gesture: ['👍', '👎', '👊', '✊', '🤛', '🤜', '🤞', '✌️', '🤟', '🤘', '👌', '🤌', '🤏', '👈', '👉', '👆', '🖕', '👇', '☝️', '👍', '👏', '🙌', '👐', '🤲', '🤝', '🙏'],
  people: ['👤', '👥', '👶', '🧒', '👦', '👧', '🧑', '👨', '👩', '🧓', '👴', '👵', '🙍', '🙎', '🙅', '🙆', '💁', '🙋', '🧏', '🤦', '🤷', '👮', '🕵️', '💂', '🥷', '👷', '🤴', '👸', '👳', '👲', '🧕', '🤵', '👰', '🤰', '🤱', '👼', '🎅', '🤶', '🦸', '🦹', '🧙', '🧚', '🧛', '🧜', '🧝', '🧞', '🧟', '💆', '💇', '🚶', '🏃', '💃', '🕺', '🕴️', '👯', '🧘', '🧗', '🤺', '🏇', '⛷️', '🏂', '🏌️', '🏄', '🚣', '🏊', '⛹️', '🏋️', '🚴', '🚵', '🤸', '🤼', '🤽', '🤾', '🤹', '🧗', '🛀', '🛌'],
  nature: ['🐶', '🐱', '🐭', '🐹', '🐰', '🦊', '🐻', '🐼', '🐨', '🐯', '🦁', '🐮', '🐷', '🐽', '🐸', '🐵', '🙈', '🙉', '🙊', '🐒', '🐔', '🐧', '🐦', '🐤', '🐣', '🐥', '🦆', '🦅', '🦉', '🦇', '🐺', '🐗', '🐴', '🦄', '🐝', '🐛', '🦋', '🐌', '🐞', '🐜', '🦟', '🦗', '🕷️', '🦂', '🐢', '🐍', '🦎', '🦖', '🦕', '🐙', '🦑', '🦐', '🦞', '🦀', '🐡', '🐠', '🐟', '🐬', '🐳', '🐋', '🦈', '🐊', '🐅', '🐆', '🦓', '🦍', '🦧', '🐘', '🦛', '🦏', '🐪', '🐫', '🦒', '🦘', '🦬', '🐃', '🐂', '🐄', '🐎', '🐖', '🐏', '🐑', '🦙', '🐐', '🦌', '🐕', '🐩', '🦮', '🐕‍🦺', '🐈', '🐈‍⬛', '🪶', '🐓', '🦃', '🦤', '🦚', '🦜', '🦢', '🦩', '🕊️', '🐇', '🦝', '🦨', '🦡', '🦫', '🦭', '🐁', '🐀', '🐿️', '🦔', '🌲', '🌳', '🌴', '🌵', '🌶️', '🌷', '🌸', '🌹', '🌺', '🌻', '🌼', '🌽', '🌾', '🌿', '☘️', '🍀', '🍁', '🍂', '🍃'],
  food: ['🍇', '🍈', '🍉', '🍊', '🍋', '🍌', '🍍', '🥭', '🍎', '🍏', '🍐', '🍑', '🍒', '🍓', '🥝', '🍅', '🥥', '🥑', '🍆', '🥔', '🥕', '🌽', '🌶️', '🥒', '🥬', '🥦', '🧄', '🧅', '🍄', '🥜', '🌰', '🍞', '🥐', '🥖', '🫓', '🥨', '🥯', '🥞', '🧇', '🥓', '🥩', '🍗', '🍖', '🦴', '🌭', '🍔', '🍟', '🍕', '🫓', '🥪', '🥙', '🧆', '🌮', '🌯', '🫔', '🥗', '🥘', '🥫', '🫕', '🍝', '🍜', '🍲', '🍛', '🍣', '🍱', '🥟', '🦪', '🍤', '🍙', '🍚', '🍘', '🍥', '🥠', '🥮', '🍢', '🍡', '🍧', '🍨', '🍦', '🥧', '🧁', '🍰', '🎂', '🍮', '🍭', '🍬', '🍫', '🍿', '🍩', '🍪', '🌰', '🥜', '🍯', '🥛', '🍼', '🫖', '☕', '🍵', '🧃', '🥤', '🧋', '🍶', '🍺', '🍻', '🥂', '🍷', '🥃', '🍸', '🍹', '🧉', '🍾', '🧊'],
  activity: ['⚽', '⚾', '🥎', '🏀', '🏐', '🏈', '🏉', '🎾', '🥏', '🎳', '🏏', '🏑', '🏒', '🥍', '🏓', '🏸', '🥊', '🥋', '🥅', '⛳', '🏹', '🎣', '🤿', '🥌', '🎽', '🎿', '🛷', '🥌', '🎯', '🎮', '🕹️', '🎰', '🎲', '🧩', '♟️', '🎯', '🎳', '🎴', '🎭', '🖼️', '🎨', '🧵', '🪡', '🧶', '🪢'],
  travel: ['🚗', '🚕', '🚙', '🚌', '🚎', '🏎️', '🚓', '🚑', '🚒', '🚐', '🛻', '🚚', '🚛', '🚜', '🛴', '🚲', '🛵', '🏍️', '🛺', '🚨', '🚔', '🚍', '🚘', '🚖', '🚡', '🚠', '🚟', '🚃', '🚋', '🚞', '🚝', '🚄', '🚅', '🚈', '🚂', '🚆', '🚇', '🚊', '🚉', '✈️', '🛫', '🛬', '🛩️', '💺', '🚁', '🚟', '🚠', '🚡', '🛰️', '🚀', '🛸', '🛎️', '🧳', '⌛', '⏳', '⌚', '⏰', '⏱️', '⏲️', '🕰️', '🕛', '🕧', '🕐', '🕜', '🕑', '🕝', '🕒', '🕞', '🕓', '🕟', '🕔', '🕠', '🕕', '🕡', '🕖', '🕢', '🕗', '🕣', '🕘', '🕤', '🕙', '🕥', '🕚', '🕦'],
  objects: ['💡', '🔦', '🕯️', '🪔', '🧯', '🛢️', '💸', '💵', '💴', '💶', '💷', '💰', '💳', '💎', '⚖️', '🪜', '🧰', '🪛', '🔧', '🔨', '⚒️', '🛠️', '⛏️', '🪚', '🔩', '⚙️', '🪤', '🧱', '⛓️', '🧲', '🔫', '💣', '🧨', '🪓', '🔪', '🗡️', '⚔️', '🛡️', '🚬', '⚰️', '🪦', '⚱️', '🏺', '🔮', '📿', '🧿', '💈', '⚗️', '🔭', '🔬', '🕳️', '🩹', '🩺', '💊', '💉', '🩸', '🧬', '🦠', '🧫', '🧪', '🌡️', '🧹', '🪠', '🧺', '🧻', '🚽', '🚿', '🛁', '🛀', '🧼', '🪥', '🪒', '🧽', '🪣', '🧴', '🛎️', '🔑', '🗝️', '🚪', '🪑', '🛋️', '🛏️', '🛌', '🧸', '🪆', '🖼️', '🪞', '🪟', '🛍️', '🛒', '🎁', '🎈', '🎀', '🪄', '🪅', '🎊', '🎉', '🎎', '🏮', '🎐', '🧧', '✉️', '📩', '📨', '📧', '💌', '📥', '📤', '📦', '🏷️', '🪧', '📪', '📫', '📬', '📭', '📮', '📯', '📜', '📃', '📄', '📑', '🧾', '📊', '📈', '📉', '🗒️', '🗓️', '📆', '📅', '🗑️', '📇', '🗃️', '🗳️', '🗄️', '📋', '📁', '📂', '🗂️', '🗂️', '📓', '📔', '📒', '📕', '📗', '📘', '📙', '📚', '📖', '🔖', '🧷', '🔗', '📎', '🖇️', '📐', '📏', '🧮', '📌', '📍', '✂️', '🖊️', '🖋️', '✒️', '🖌️', '🖍️', '📝', '✏️', '🔍', '🔎', '🔏', '🔐', '🔒', '🔓'],
  symbols: ['❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '💔', '❣️', '💕', '💞', '💓', '💗', '💖', '💘', '💝', '💟', '☮️', '✝️', '☪️', '🕉️', '☸️', '✡️', '🔯', '🕎', '☯️', '☦️', '🛐', '⛎', '♈', '♉', '♊', '♋', '♌', '♍', '♎', '♏', '♐', '♑', '♒', '♓', '🆔', '⚛️', '🉑', '☢️', '☣️', '📴', '📳', '🈶', '🈚', '🈸', '🈺', '🈷️', '✴️', '🆚', '💮', '🉐', '㊙️', '㊗️', '🈴', '🈵', '🈹', '🈲', '🅰️', '🅱️', '🆎', '🆑', '🅾️', '🆘', '❌', '⭕', '🛑', '⛔', '📛', '🚫', '💯', '💢', '♨️', '🚷', '🚯', '🚳', '🚱', '🔞', '📵', '🚭', '❗', '❓', '❕', '❔', '‼️', '⁉️', '🔅', '🔆', '〽️', '⚠️', '🚸', '🔱', '⚜️', '🔰', '♻️', '✅', '🈯', '💹', '❇️', '✳️', '❎', '🌐', '💠', 'Ⓜ️', '🌀', '💤', '🏧', '🚾', '♿', '🅿️', '🈳', '🈂️', '🛂', '🛃', '🛄', '🛅', '🚹', '🚺', '🚼', '🚻', '🚮', '🎦', '📶', '🈁', '🔣', 'ℹ️', '🔤', '🔡', '🔠', '🆖', '🆗', '🆙', '🆒', '🆕', '🆓', '0️⃣', '1️⃣', '2️⃣', '3️⃣', '4️⃣', '5️⃣', '6️⃣', '7️⃣', '8️⃣', '9️⃣', '🔟', '🔢', '#️⃣', '*️⃣', '⏏️', '▶️', '⏸️', '⏯️', '⏹️', '⏺️', '⏭️', '⏮️', '⏩', '⏪', '⏫', '⏬', '◀️', '🔼', '🔽', '➡️', '⬅️', '⬆️', '⬇️', '↗️', '↘️', '↙️', '↖️', '↕️', '↔️', '↪️', '↩️', '⤴️', '⤵️', '🔀', '🔁', '🔂', '🔄', '🔃', '🎵', '🎶', '➕', '➖', '➗', '✖️', '💲', '💱', '™️', '©️', '®️', '〰️', '➰', '➿', '🔚', '🔙', '🔛', '🔜', '🔝', '🛐', '⏏️', '🎚️', '🔊', '🔉', '🔈', '🔇', '📢', '📣', '📯', '🔔', '🔕', '📻', '📡', '💬', '💭', '🗯️', '♠️', '♣️', '♥️', '♦️', '🃏', '🎴', '🀄', '🕐', '🕑', '🕒', '🕓', '🕔', '🕕', '🕖', '🕗', '🕘', '🕙', '🕚', '🕛', '🕜', '🕝', '🕞', '🕟', '🕠', '🕡', '🕢', '🕣', '🕤', '🕥', '🕦', '🕧']
}

const filteredChatList = computed(() => {
  let list = [...chatList.value] // 创建副本以避免修改原数组
  
  // 按时间倒序排列（最新的在前）
  list.sort((a, b) => {
    const timeA = a.createdAt ? new Date(a.createdAt).getTime() : 0
    const timeB = b.createdAt ? new Date(b.createdAt).getTime() : 0
    return timeB - timeA // 倒序
  })
  
  if (searchKeyword.value) {
    list = list.filter(chat => 
      getChatName(chat).toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
      getChatCompany(chat).toLowerCase().includes(searchKeyword.value.toLowerCase())
    )
  }
  
  // 根据筛选条件过滤
  if (activeTab.value === 'unread') {
    // 未读：未读消息数大于0
    console.log('筛选未读消息，原始列表长度:', list.length)
    list = list.filter(chat => {
      const unreadCount = getUnreadCount(chat)
      console.log('聊天项未读数:', { submissionId: chat.submissionId, projectId: chat.projectId, unreadCount, rawUnreadCount: chat.unreadCount })
      return unreadCount > 0
    })
    console.log('筛选后未读消息列表长度:', list.length)
  } else if (activeTab.value === 'submitted') {
    // 已提交稿件：submission状态为SUBMITTED，且必须有submissionId
    console.log('筛选已提交稿件，原始列表长度:', list.length)
    list = list.filter(chat => {
      const hasSubmissionId = !!(chat.submissionId || chat.submission_id)
      // 支持驼峰和下划线两种格式
      const status = chat.submissionStatus || chat.submission_status
      const isSubmitted = status === 'SUBMITTED'
      console.log('聊天项状态:', { 
        submissionId: chat.submissionId || chat.submission_id, 
        submissionStatus: status, 
        hasSubmissionId, 
        isSubmitted,
        allStatusFields: { submissionStatus: chat.submissionStatus, submission_status: chat.submission_status }
      })
      return hasSubmissionId && isSubmitted
    })
    console.log('筛选后已提交稿件列表长度:', list.length)
  } else if (activeTab.value === 'interested') {
    // 已接单：submission状态为INTERESTED
    list = list.filter(chat => {
      const status = chat.submissionStatus || chat.submission_status
      return status === 'INTERESTED'
    })
  } else if (activeTab.value === 'confirmed') {
    // 已合作：submission状态为CONFIRMED
    list = list.filter(chat => {
      const status = chat.submissionStatus || chat.submission_status
      return status === 'CONFIRMED'
    })
  }
  return list
})

const loadChatList = async () => {
  loadingChatList.value = true
  try {
    const res = await request.get('/api/messages/chats')
    if (res.code === '200') {
      chatList.value = res.data || []
      // 调试：打印聊天列表，检查freelancerId、submissionStatus和unreadCount
      console.log('聊天列表加载成功，共', chatList.value.length, '条')
      console.log('完整聊天列表数据:', JSON.stringify(chatList.value, null, 2))
      chatList.value.forEach((chat, index) => {
        console.log(`聊天${index + 1}:`, {
          submissionId: chat.submissionId,
          projectId: chat.projectId,
          freelancerId: chat.freelancerId,
          submissionStatus: chat.submissionStatus,
          senderType: chat.senderType,
          unreadCount: chat.unreadCount,
          // 检查所有可能的字段名
          unread_count: chat.unread_count,
          submission_status: chat.submission_status,
          allKeys: Object.keys(chat)
        })
      })
    } else {
      message.error(res.msg || '加载聊天列表失败')
    }
  } catch (error) {
    console.error('加载聊天列表失败:', error)
    message.error('加载聊天列表失败')
  } finally {
    loadingChatList.value = false
  }
}

const handleSelectChat = async (chat) => {
  const chatKey = chat.submissionId || `project_${chat.projectId}`
  const currentKey = selectedSubmissionId.value ? selectedSubmissionId.value.toString() : (selectedChat.value ? `project_${selectedChat.value.projectId}` : null)
  
  if (currentKey === chatKey) {
    selectedSubmissionId.value = null
    selectedChat.value = null
    messageList.value = []
    return
  }
  
  selectedSubmissionId.value = chat.submissionId
  selectedChat.value = chat
  if (chat.submissionId) {
    await loadMessages(chat.submissionId)
  } else {
    await loadMessagesByProject(chat.projectId)
  }
  await loadProject(chat.projectId)
}

const loadMessages = async (submissionId) => {
  loadingMessages.value = true
  try {
    const res = await request.get(`/api/messages/submission/${submissionId}`)
    if (res.code === '200') {
      messageList.value = res.data || []
      // 标记消息为已读
      const unreadMessages = messageList.value.filter(msg => !msg.isRead && !isMyMessage(msg))
      if (unreadMessages.length > 0) {
        for (const msg of unreadMessages) {
          try {
            await request.put(`/api/messages/${msg.id}/read`)
          } catch (error) {
            console.error('标记消息已读失败:', error)
          }
        }
        // 标记已读后，重新加载聊天列表以更新未读消息数
        await loadChatList()
        // 重新选中当前聊天项
        if (selectedChat.value) {
          const updatedChat = chatList.value.find(c => 
            (c.submissionId && c.submissionId === selectedChat.value.submissionId) ||
            (!c.submissionId && c.projectId === selectedChat.value.projectId)
          )
          if (updatedChat) {
            selectedChat.value = updatedChat
          }
        }
      }
      nextTick(() => {
        scrollToBottom()
      })
    } else {
      message.error(res.msg || '加载消息失败')
    }
  } catch (error) {
    console.error('加载消息失败:', error)
    message.error('加载消息失败')
  } finally {
    loadingMessages.value = false
  }
}

const loadMessagesByProject = async (projectId) => {
  loadingMessages.value = true
  try {
    const res = await request.get(`/api/messages/project/${projectId}`)
    if (res.code === '200') {
      messageList.value = res.data || []
      // 标记消息为已读
      const unreadMessages = messageList.value.filter(msg => !msg.isRead && !isMyMessage(msg))
      if (unreadMessages.length > 0) {
        for (const msg of unreadMessages) {
          try {
            await request.put(`/api/messages/${msg.id}/read`)
          } catch (error) {
            console.error('标记消息已读失败:', error)
          }
        }
        // 标记已读后，重新加载聊天列表以更新未读消息数
        await loadChatList()
        // 重新选中当前聊天项
        if (selectedChat.value) {
          const updatedChat = chatList.value.find(c => 
            (c.submissionId && c.submissionId === selectedChat.value.submissionId) ||
            (!c.submissionId && c.projectId === selectedChat.value.projectId)
          )
          if (updatedChat) {
            selectedChat.value = updatedChat
          }
        }
      }
      nextTick(() => {
        scrollToBottom()
      })
    } else {
      message.error(res.msg || '加载消息失败')
    }
  } catch (error) {
    console.error('加载消息失败:', error)
    message.error('加载消息失败')
  } finally {
    loadingMessages.value = false
  }
}

const loadProject = async (projectId) => {
  try {
    const res = await request.get(`/api/projects/${projectId}`)
    if (res.code === '200') {
      project.value = res.data
    }
  } catch (error) {
    console.error('加载项目信息失败:', error)
  }
}

const handleSendMessage = async () => {
  if (!messageForm.content.trim() || !selectedChat.value) {
    return
  }
  
  if (project.value && project.value.status === 'COMPLETED') {
    message.warning('项目已完成，无法继续聊天')
    return
  }
  
  sendingMessage.value = true
  try {
    const msg = {
      projectId: selectedChat.value.projectId,
      content: messageForm.content.trim()
    }
    // 如果有submissionId，也传递
    if (selectedChat.value.submissionId) {
      msg.submissionId = selectedChat.value.submissionId
    }
    
    const res = await request.post('/api/messages', msg)
    if (res.code === '200') {
      messageForm.content = ''
      // 发送消息后，重新加载聊天列表（这样虚拟聊天项会被真实聊天项替换）
      await loadChatList()
      
      // 重新选中聊天项（从列表中查找）
      if (selectedChat.value.submissionId) {
        const chat = chatList.value.find(c => c.submissionId === selectedChat.value.submissionId)
        if (chat) {
          selectedChat.value = chat
        }
        await loadMessages(selectedChat.value.submissionId)
      } else {
        const chat = chatList.value.find(c => c.projectId === selectedChat.value.projectId && !c.submissionId)
        if (chat) {
          selectedChat.value = chat
        }
        await loadMessagesByProject(selectedChat.value.projectId)
      }
      
      nextTick(() => {
        scrollToBottom()
        messageInputRef.value?.focus()
      })
    } else {
      message.error(res.msg || '发送消息失败')
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    if (error.response?.data?.msg) {
      message.error(error.response.data.msg)
    } else {
      message.error('发送消息失败')
    }
  } finally {
    sendingMessage.value = false
  }
}

const handleViewProfile = async (freelancerIdParam = null) => {
  if (!isEnterprise.value) {
    return
  }
  
  // 获取自由职业者ID
  let freelancerId = freelancerIdParam || selectedChat.value?.freelancerId
  
  // 如果没有freelancerId，优先从submission获取（最可靠的方法）
  if (!freelancerId && selectedChat.value?.submissionId) {
    try {
      const submissionRes = await request.get(`/api/submissions/${selectedChat.value.submissionId}`)
      if (submissionRes.code === '200' && submissionRes.data) {
        // 从submission中获取freelancerId
        const submission = submissionRes.data
        if (submission.freelancerId) {
          freelancerId = submission.freelancerId
        } else if (submission.freelancer && submission.freelancer.id) {
          freelancerId = submission.freelancer.id
        }
      }
    } catch (error) {
      console.error('加载稿件信息失败:', error)
    }
  }
  
  // 如果还是没有freelancerId，尝试从消息列表中查找第一条自由职业者发送的消息
  // 然后通过该消息的submissionId获取freelancerId
  if (!freelancerId && messageList.value && messageList.value.length > 0) {
    const freelancerMessage = messageList.value.find(msg => msg.senderType === 'FREELANCER')
    if (freelancerMessage && freelancerMessage.submissionId) {
      try {
        const submissionRes = await request.get(`/api/submissions/${freelancerMessage.submissionId}`)
        if (submissionRes.code === '200' && submissionRes.data) {
          const submission = submissionRes.data
          if (submission.freelancerId) {
            freelancerId = submission.freelancerId
          }
        }
      } catch (error) {
        console.error('通过消息获取稿件信息失败:', error)
      }
    }
  }
  
  // 如果还是没有freelancerId，尝试从project获取（对于没有submission的聊天）
  if (!freelancerId && selectedChat.value?.projectId && !selectedChat.value?.submissionId) {
    // 对于project-based的聊天，需要从消息中查找自由职业者的userId
    // 然后通过userId找到对应的freelancer
    // 但前端没有这个API，所以这里只能提示
    console.warn('无法从project-based聊天获取freelancerId')
  }
  
  if (freelancerId) {
    try {
      console.log('加载自由职业者信息，freelancerId:', freelancerId)
      const res = await request.get(`/api/freelancers/${freelancerId}`)
      if (res.code === '200') {
        console.log('自由职业者信息加载成功:', res.data)
        selectedFreelancer.value = res.data
        profileModalVisible.value = true
      } else {
        console.error('加载自由职业者信息失败，响应:', res)
        message.error(res.msg || '加载自由职业者信息失败')
      }
    } catch (error) {
      console.error('加载自由职业者信息失败:', error)
      message.error('加载自由职业者信息失败: ' + (error.response?.data?.msg || error.message))
    }
  } else {
    console.warn('无法获取freelancerId，selectedChat:', selectedChat.value)
    message.warning('无法获取自由职业者信息，请确保已选择正确的聊天')
  }
}

const handleViewSenderProfile = async (msg) => {
  if (!isEnterprise.value || msg.senderType !== 'FREELANCER') {
    return
  }
  
  // 优先从submission获取freelancerId（最可靠）
  if (selectedChat.value?.submissionId) {
    try {
      const submissionRes = await request.get(`/api/submissions/${selectedChat.value.submissionId}`)
      if (submissionRes.code === '200' && submissionRes.data) {
        const submission = submissionRes.data
        if (submission.freelancerId) {
          await handleViewProfile(submission.freelancerId)
          return
        }
      }
    } catch (error) {
      console.error('加载稿件信息失败:', error)
    }
  }
  
  // 如果从submission获取失败，尝试从selectedChat获取
  if (selectedChat.value && selectedChat.value.freelancerId) {
    await handleViewProfile(selectedChat.value.freelancerId)
  } else {
    // 最后尝试从消息的senderId推断（如果消息是自由职业者发送的，senderId就是user.id）
    // 但前端没有通过userId获取freelancer的API，所以这里只能提示错误
    console.warn('无法获取freelancerId，消息senderId:', msg.senderId, 'submissionId:', selectedChat.value?.submissionId)
    await handleViewProfile()
  }
}

const handleViewProject = () => {
  if (selectedChat.value && selectedChat.value.projectId) {
    router.push(`/front/projects/${selectedChat.value.projectId}`)
  }
}

const handleMenuClick = ({ key }) => {
  if (key === 'sendProfile') {
    handleSendProfile()
  }
}

const handleSendProfile = async () => {
  if (!selectedChat.value) {
    return
  }
  
  const currentUser = JSON.parse(localStorage.getItem('xm-user') || '{}')
  const isFreelancer = currentUser.role === 'USER'
  
  if (!isFreelancer) {
    message.warning('只有自由职业者可以发送个人资料')
    return
  }
  
  try {
    // 获取自由职业者信息
    const freelancerRes = await request.get('/api/freelancers/profile')
    if (freelancerRes.code !== '200' || !freelancerRes.data) {
      message.error('获取个人资料失败')
      return
    }
    
    const freelancer = freelancerRes.data
    const user = JSON.parse(localStorage.getItem('xm-user') || '{}')
    
    // 构建个人资料卡片内容
    const profileCard = `
      <div style="padding: 16px; background: #f5f5f5; border-radius: 8px; max-width: 400px;">
        <div style="display: flex; align-items: center; margin-bottom: 12px;">
          <img src="${user.avatar || ''}" style="width: 60px; height: 60px; border-radius: 50%; margin-right: 12px;" onerror="this.style.display='none'">
          <div>
            <div style="font-size: 16px; font-weight: bold; margin-bottom: 4px;">${user.name || '未知'}</div>
            <div style="font-size: 12px; color: #8c8c8c;">${freelancer.skills || '暂无技能'}</div>
          </div>
        </div>
        <div style="font-size: 14px; color: #262626; margin-bottom: 8px;">
          <div><strong>评分：</strong>${freelancer.rating || 0}分</div>
          <div><strong>完成项目：</strong>${freelancer.completedProjects || 0}个</div>
          <div><strong>信誉分：</strong>${freelancer.creditScore || 100}分</div>
        </div>
        <div style="font-size: 12px; color: #8c8c8c; margin-top: 8px; padding-top: 8px; border-top: 1px solid #e8e8e8;">
          <a href="javascript:void(0)" onclick="window.handleViewProfileCard && window.handleViewProfileCard()" style="color: #1890ff; text-decoration: underline;">点击查看完整资料</a>
        </div>
      </div>
    `
    
    // 发送个人资料卡片
    const msg = {
      projectId: selectedChat.value.projectId,
      content: profileCard
    }
    if (selectedChat.value.submissionId) {
      msg.submissionId = selectedChat.value.submissionId
    }
    
    const res = await request.post('/api/messages', msg)
    if (res.code === '200') {
      message.success('个人资料已发送')
      // 重新加载消息列表
      if (selectedChat.value.submissionId) {
        await loadMessages(selectedChat.value.submissionId)
      } else {
        await loadMessagesByProject(selectedChat.value.projectId)
      }
      await loadChatList()
      nextTick(() => {
        scrollToBottom()
      })
    } else {
      message.error(res.msg || '发送失败')
    }
  } catch (error) {
    console.error('发送个人资料失败:', error)
    message.error('发送个人资料失败')
  }
}

// 查看个人资料卡片（从消息中提取）
const handleViewProfileCard = async () => {
  console.log('handleViewProfileCard: 开始处理')
  if (!selectedChat.value || !isEnterprise.value) {
    console.warn('handleViewProfileCard: selectedChat为空或不是企业端')
    return
  }
  
  console.log('handleViewProfileCard: selectedChat信息', {
    submissionId: selectedChat.value.submissionId,
    projectId: selectedChat.value.projectId,
    freelancerId: selectedChat.value.freelancerId
  })
  
  // 优先从selectedChat获取freelancerId
  let freelancerId = selectedChat.value.freelancerId
  
  // 如果没有，从submission获取（最可靠的方法）
  if (!freelancerId && selectedChat.value.submissionId) {
    console.log('handleViewProfileCard: 从submission获取freelancerId, submissionId=', selectedChat.value.submissionId)
    try {
      const submissionRes = await request.get(`/api/submissions/${selectedChat.value.submissionId}`)
      if (submissionRes.code === '200' && submissionRes.data) {
        const submission = submissionRes.data
        console.log('handleViewProfileCard: submission信息', {
          freelancerId: submission.freelancerId,
          freelancer: submission.freelancer
        })
        if (submission.freelancerId) {
          freelancerId = submission.freelancerId
        } else if (submission.freelancer && submission.freelancer.id) {
          freelancerId = submission.freelancer.id
        }
      }
    } catch (error) {
      console.error('handleViewProfileCard: 加载稿件信息失败:', error)
    }
  }
  
  console.log('handleViewProfileCard: 最终freelancerId=', freelancerId)
  if (freelancerId) {
    await handleViewProfile(freelancerId)
  } else {
    console.warn('handleViewProfileCard: 无法获取freelancerId，尝试其他方法')
    await handleViewProfile()
  }
}

// 预览图片
const handlePreviewImage = (imageUrl) => {
  console.log('handlePreviewImage: 图片URL', imageUrl)
  if (!imageUrl) {
    message.error('图片URL为空')
    return
  }
  previewImageUrl.value = imageUrl
  imagePreviewVisible.value = true
  console.log('handlePreviewImage: 预览模态框已打开，URL:', previewImageUrl.value)
}

// 图片加载错误处理
const handleImageError = () => {
  message.error('图片加载失败')
  imagePreviewVisible.value = false
}

// 图片加载成功处理
const handleImageLoad = (event) => {
  const img = event.target
  // 确保图片能够完整显示，按实际比例放大，容器完全贴合图片
  if (img.naturalWidth > 0 && img.naturalHeight > 0) {
    // 获取视口尺寸
    const viewportWidth = window.innerWidth
    const viewportHeight = window.innerHeight
    
    // 计算按比例缩放的尺寸，确保图片完整显示在视口内（留出一些边距）
    const maxWidth = viewportWidth * 0.9
    const maxHeight = viewportHeight * 0.85
    const scale = Math.min(maxWidth / img.naturalWidth, maxHeight / img.naturalHeight, 1)
    
    // 设置图片尺寸，保持原始比例
    const displayWidth = img.naturalWidth * scale
    const displayHeight = img.naturalHeight * scale
    
    img.style.width = displayWidth + 'px'
    img.style.height = displayHeight + 'px'
    img.style.maxWidth = 'none'
    img.style.maxHeight = 'none'
    img.style.margin = '0'
    img.style.padding = '0'
    img.style.display = 'block'
    
    // 调整容器尺寸以完全匹配图片尺寸，不留任何空白
    const container = img.parentElement
    if (container) {
      container.style.width = displayWidth + 'px'
      container.style.height = displayHeight + 'px'
      container.style.minHeight = '0'
      container.style.padding = '0'
      container.style.margin = '0'
    }
    
    // 调整模态框内容区域尺寸
    nextTick(() => {
      const modalContent = document.querySelector('.image-preview-modal .ant-modal-content')
      const modalBody = document.querySelector('.image-preview-modal .ant-modal-body')
      if (modalContent) {
        modalContent.style.width = displayWidth + 'px'
        modalContent.style.height = displayHeight + 'px'
        modalContent.style.padding = '0'
        modalContent.style.margin = '0'
      }
      if (modalBody) {
        modalBody.style.width = displayWidth + 'px'
        modalBody.style.height = displayHeight + 'px'
        modalBody.style.padding = '0'
        modalBody.style.margin = '0'
      }
    })
  }
}

const getChatName = (chat) => {
  if (isEnterprise.value) {
    // 企业端：应该显示自由职业者的信息
    // 如果最后一条消息是企业发送的，显示接收者（自由职业者）的信息
    // 如果最后一条消息是自由职业者发送的，显示发送者（自由职业者）的信息
    if (chat.senderType === 'ENTERPRISE') {
      return chat.recipientName || '未知用户'
    } else {
      return chat.senderName || '未知用户'
    }
  } else {
    // 自由职业者端：应该显示企业的信息
    // 如果最后一条消息是自由职业者发送的，显示接收者（企业）的信息
    // 如果最后一条消息是企业发送的，显示发送者（企业）的信息
    if (chat.senderType === 'FREELANCER') {
      return chat.recipientName || '未知企业'
    } else {
      return chat.senderName || '未知企业'
    }
  }
}

const getChatAvatar = (chat) => {
  if (isEnterprise.value) {
    // 企业端：应该显示自由职业者的头像
    if (chat.senderType === 'ENTERPRISE') {
      return chat.recipientAvatar
    } else {
      return chat.senderAvatar
    }
  } else {
    // 自由职业者端：应该显示企业的头像
    if (chat.senderType === 'FREELANCER') {
      return chat.recipientAvatar
    } else {
      return chat.senderAvatar
    }
  }
}

const getChatCompany = (chat) => {
  if (isEnterprise.value) {
    // 企业端显示自由职业者信息，这里需要从submission获取
    return chat.projectTitle || '未知项目'
  } else {
    return chat.recipientName || '未知企业'
  }
}

const getChatRole = (chat) => {
  // 移除HR字样，返回空字符串
  return ''
}

// 提取图片URL的正则表达式（更强大的匹配）
const extractImageUrls = (text) => {
  if (!text) return []
  
  // 图片扩展名列表（包括ico和svg）
  const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'ico', 'svg']
  const imageExtPattern = imageExtensions.join('|')
  
  // 匹配各种图片URL格式：
  // 1. 标准格式：https://example.com/image.jpg
  // 2. COS格式：https://bucket.cos.region.myqcloud.com/path/image.jpg
  // 3. 带查询参数：https://example.com/image.jpg?param=value
  // 4. 带锚点：https://example.com/image.jpg#anchor
  const patterns = [
    // 匹配以图片扩展名结尾的URL（包括查询参数和锚点）
    // 这个正则会匹配所有以图片扩展名结尾的URL，包括COS URL
    new RegExp(`https?://[^\\s<>"'\\n\\r]+\\.(${imageExtPattern})(\\?[^\\s<>"'\\n\\r]*)?(#[^\\s<>"'\\n\\r]*)?`, 'gi')
  ]
  
  const urls = []
  patterns.forEach(pattern => {
    const matches = text.match(pattern)
    if (matches) {
      urls.push(...matches)
    }
  })
  
  // 去重并过滤掉HTML标签中的URL（避免重复处理）
  const uniqueUrls = [...new Set(urls)]
  
  // 调试：打印提取到的URL
  if (uniqueUrls.length > 0) {
    console.log('提取到的图片URL:', uniqueUrls)
  }
  
  return uniqueUrls
}

const getPreviewContent = (content) => {
  if (!content) return ''
  
  // 检查是否是个人资料卡片
  if (content.includes('个人资料') || content.includes('background: #f5f5f5')) {
    return '[个人资料卡片]'
  }
  
  // 提取所有图片URL
  const imageUrls = extractImageUrls(content)
  
  if (imageUrls && imageUrls.length > 0) {
    // 移除所有图片URL，检查是否还有其他文本
    let textWithoutImage = content
    imageUrls.forEach(url => {
      textWithoutImage = textWithoutImage.replace(url, '')
    })
    textWithoutImage = textWithoutImage.replace(/<[^>]*>/g, '').trim()
    
    if (!textWithoutImage) {
      // 只有图片，显示小缩略图（点击可预览）
      const escapedUrl = imageUrls[0].replace(/'/g, "\\'")
      return `<img src="${imageUrls[0]}" alt="图片" style="max-width: 40px; max-height: 40px; border-radius: 4px; vertical-align: middle; object-fit: cover; cursor: pointer;" onclick="window.handlePreviewImage && window.handlePreviewImage('${escapedUrl}')" />`
    } else {
      // 有文字和图片，显示文字和图片标识
      const text = textWithoutImage.replace(/\n/g, ' ')
      const textPreview = text.length > 20 ? text.substring(0, 20) + '...' : text
      return `${textPreview} <span style="color: #1890ff;">[图片]</span>`
    }
  }
  
  // 移除HTML标签
  const text = content.replace(/<[^>]*>/g, '').replace(/\n/g, ' ')
  return text.length > 30 ? text.substring(0, 30) + '...' : text
}

const getUnreadCount = (chat) => {
  // 从后端返回的聊天项中获取未读消息数
  // 支持驼峰和下划线两种格式
  if (chat) {
    const count = chat.unreadCount !== undefined && chat.unreadCount !== null 
      ? chat.unreadCount 
      : (chat.unread_count !== undefined && chat.unread_count !== null ? chat.unread_count : null)
    if (count !== null && count !== undefined) {
      return Number(count) || 0
    }
  }
  return 0
}

// 创建虚拟聊天项（当聊天列表中没有对应项时）
const createVirtualChat = async (submissionId, projectId, freelancerId = null) => {
  if (!projectId) {
    console.error('createVirtualChat: projectId为空')
    return null
  }
  
  try {
    console.log('createVirtualChat: 开始创建虚拟聊天项, projectId=', projectId, 'submissionId=', submissionId, 'freelancerId=', freelancerId)
    // 加载项目信息
    const projectRes = await request.get(`/api/projects/${projectId}`)
    console.log('createVirtualChat: 项目信息响应', projectRes)
    if (projectRes.code !== '200' || !projectRes.data) {
      console.error('createVirtualChat: 加载项目信息失败', projectRes)
      return null
    }
    
    const projectData = projectRes.data
    const currentUser = JSON.parse(localStorage.getItem('xm-user') || '{}')
    const isFreelancer = currentUser.role === 'USER'
    
    console.log('createVirtualChat: 项目信息加载成功', {
      projectId: projectData.id,
      enterpriseName: projectData.enterpriseName,
      enterpriseAvatar: projectData.enterpriseAvatar,
      isFreelancer
    })
    
    // 创建虚拟聊天项
    const virtualChat = {
      projectId: projectData.id,
      submissionId: submissionId || null,
      projectTitle: projectData.title,
      submissionTitle: null,
      content: '',
      createdAt: new Date().toISOString(),
      senderName: '',
      senderAvatar: '',
      recipientName: isFreelancer ? (projectData.enterpriseName || '未知企业') : '未知用户',
      recipientAvatar: isFreelancer ? (projectData.enterpriseAvatar || '') : '',
      freelancerId: freelancerId || null
    }
    
    // 如果是通过submissionId，尝试加载submission信息
    if (submissionId) {
      try {
        const submissionRes = await request.get(`/api/submissions/${submissionId}`)
        if (submissionRes.code === '200' && submissionRes.data) {
          virtualChat.submissionTitle = submissionRes.data.title
          if (isFreelancer) {
            // 自由职业者查看，recipient是企业 - 直接使用projectData中的企业信息
            virtualChat.recipientName = projectData.enterpriseName || '未知企业'
            virtualChat.recipientAvatar = projectData.enterpriseAvatar || ''
            virtualChat.senderType = 'FREELANCER' // 设置发送者类型
          } else {
            // 企业查看，recipient是自由职业者
            virtualChat.senderType = 'ENTERPRISE' // 设置发送者类型
            const targetFreelancerId = freelancerId || submissionRes.data.freelancerId
            if (targetFreelancerId) {
              virtualChat.freelancerId = targetFreelancerId
              try {
                const freelancerRes = await request.get(`/api/freelancers/${targetFreelancerId}`)
                if (freelancerRes.code === '200' && freelancerRes.data) {
                  const freelancer = freelancerRes.data
                  // 后端返回的是扁平化的字段：userName, userAvatar
                  virtualChat.recipientName = freelancer.userName || '未知用户'
                  virtualChat.recipientAvatar = freelancer.userAvatar || ''
                }
              } catch (e) {
                console.error('加载自由职业者信息失败:', e)
              }
            }
          }
        }
      } catch (error) {
        console.error('加载submission信息失败:', error)
      }
    } else {
      // 如果没有submissionId，设置默认的senderType
      virtualChat.senderType = isFreelancer ? 'FREELANCER' : 'ENTERPRISE'
      // 如果没有submissionId，自由职业者端直接使用projectData中的企业信息
      if (isFreelancer) {
        virtualChat.recipientName = projectData.enterpriseName || '未知企业'
        virtualChat.recipientAvatar = projectData.enterpriseAvatar || ''
      } else {
        // 企业端，如果有freelancerId，加载自由职业者信息
        if (freelancerId) {
          virtualChat.freelancerId = freelancerId
          try {
            const freelancerRes = await request.get(`/api/freelancers/${freelancerId}`)
            if (freelancerRes.code === '200' && freelancerRes.data) {
              const freelancer = freelancerRes.data
              virtualChat.recipientName = freelancer.userName || '未知用户'
              virtualChat.recipientAvatar = freelancer.userAvatar || ''
            }
          } catch (e) {
            console.error('加载自由职业者信息失败:', e)
          }
        }
      }
    }
    
    console.log('createVirtualChat: 虚拟聊天项创建成功', virtualChat)
    return virtualChat
  } catch (error) {
    console.error('创建虚拟聊天项失败:', error)
    return null
  }
}

const isOnline = (chat) => {
  // 暂时返回false，后续可以通过WebSocket或轮询获取在线状态
  return false
}

const isMyMessage = (msg) => {
  const currentUser = JSON.parse(localStorage.getItem('xm-user') || '{}')
  return msg.senderId === currentUser.id
}

const formatMessageContent = (content) => {
  if (!content) return ''
  // 检查是否是个人资料卡片（包含特定样式）
  if (content.includes('个人资料') || content.includes('background: #f5f5f5')) {
    // 如果是个人资料卡片，添加点击查看详情功能
    return content.replace(
      /点击查看完整资料/g,
      '<a href="javascript:void(0)" onclick="window.handleViewProfileCard && window.handleViewProfileCard()" style="color: #1890ff; text-decoration: underline;">点击查看完整资料</a>'
    )
  }
  
  // 先提取所有图片URL（在原始内容中提取，因为URL可能跨行）
  const imageUrls = extractImageUrls(content)
  
  // 处理换行
  let formatted = content.replace(/\n/g, '<br>')
  
  if (imageUrls && imageUrls.length > 0) {
    // 替换每个图片URL为img标签
    imageUrls.forEach(url => {
      // 检查URL是否已经被img标签包裹
      const urlInFormatted = url.replace(/\n/g, '<br>')
      if (!formatted.includes(`<img`) || !formatted.includes(urlInFormatted)) {
        // 转义URL中的特殊字符用于正则匹配
        const escapedUrl = urlInFormatted.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
        const urlPattern = new RegExp(escapedUrl, 'g')
        formatted = formatted.replace(urlPattern, (match) => {
          // 恢复原始URL（去掉<br>标签）
          const originalUrl = match.replace(/<br>/g, '\n').trim()
          // 转义URL中的单引号，避免在onclick中出错
          const escapedUrl = originalUrl.replace(/'/g, "\\'")
          return `<img src="${originalUrl}" alt="图片" style="max-width: 300px; max-height: 300px; width: auto; height: auto; object-fit: contain; border-radius: 4px; cursor: pointer; display: block; margin: 8px 0;" onclick="window.handlePreviewImage && window.handlePreviewImage('${escapedUrl}')" />`
        })
      }
    })
  }
  
  return formatted
}

const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } else if (days === 1) {
    return '昨天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } else if (days < 7) {
    return days + '天前'
  } else {
    return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
  }
}

const toggleEmojiPicker = () => {
  showEmojiPicker.value = !showEmojiPicker.value
  if (showEmojiPicker.value) {
    showCommonPhrases.value = false
  }
}

const toggleCommonPhrases = () => {
  showCommonPhrases.value = !showCommonPhrases.value
  if (showCommonPhrases.value) {
    showEmojiPicker.value = false
  }
}

const insertPhrase = (phrase) => {
  messageForm.content += phrase
  showCommonPhrases.value = false
  // 保持输入框焦点，防止页面滚动
  nextTick(() => {
    if (messageInputRef.value) {
      messageInputRef.value.focus()
    }
  })
}

const getCurrentCategoryEmojis = () => {
  return emojiMap[currentCategory.value] || []
}

const insertEmoji = (emoji) => {
  messageForm.content += emoji
  showEmojiPicker.value = false
  // 保持输入框焦点，防止页面滚动
  nextTick(() => {
    if (messageInputRef.value) {
      messageInputRef.value.focus()
    }
  })
}

// 图片上传前的验证
const beforeUploadImage = (file) => {
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
  return true
}

// 处理图片上传
const handleUploadImage = async ({ file, onSuccess, onError }) => {
  try {
    const formData = new FormData()
    formData.append('file', file)
    
    const res = await request.post('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    if (res.code === '200' && res.data) {
      // 上传成功后，将图片URL添加到消息内容中
      const imageUrl = res.data
      // 如果消息内容不为空，先添加换行
      if (messageForm.content.trim()) {
        messageForm.content += '\n' + imageUrl
      } else {
        messageForm.content = imageUrl
      }
      message.success('图片上传成功')
      onSuccess()
      // 上传成功后自动发送消息
      await handleSendMessage()
    } else {
      message.error(res.msg || '图片上传失败')
      onError(new Error(res.msg || '图片上传失败'))
    }
  } catch (error) {
    console.error('图片上传失败:', error)
    message.error('图片上传失败')
    onError(error)
  }
}

const scrollToBottom = () => {
  if (messagesContainerRef.value) {
    messagesContainerRef.value.scrollTop = messagesContainerRef.value.scrollHeight
  }
}

const handleSearch = () => {
  // 搜索逻辑已在computed中处理
}

// 定时刷新聊天列表和消息
let refreshInterval = null

onMounted(async () => {
  // 注册全局函数，用于个人资料卡片点击和图片预览
  window.handleViewProfileCard = handleViewProfileCard
  window.handlePreviewImage = handlePreviewImage
  
  console.log('Conversation onMounted: 路由参数', route.params)
  
  await loadChatList()
  
  // 如果路由中有参数但聊天列表中没有对应项，创建虚拟聊天项
  if (route.params.submissionId) {
    console.log('Conversation onMounted: 检测到submissionId参数', route.params.submissionId)
    const chat = chatList.value.find(c => c.submissionId === parseInt(route.params.submissionId))
    if (chat) {
      handleSelectChat(chat)
    } else {
      try {
        const submissionRes = await request.get(`/api/submissions/${route.params.submissionId}`)
        if (submissionRes.code === '200' && submissionRes.data) {
          const virtualChat = await createVirtualChat(parseInt(route.params.submissionId), submissionRes.data.projectId)
          if (virtualChat) {
            selectedChat.value = virtualChat
            selectedSubmissionId.value = parseInt(route.params.submissionId)
            await loadMessages(parseInt(route.params.submissionId))
            await loadProject(virtualChat.projectId)
          } else {
            console.error('创建虚拟聊天项失败: virtualChat为null')
            message.error('无法创建聊天会话，请稍后重试')
          }
        } else {
          message.error('加载稿件信息失败')
        }
      } catch (error) {
        console.error('加载submission失败:', error)
        message.error('加载稿件信息失败: ' + (error.response?.data?.msg || error.message))
      }
    }
  } else if (route.params.projectId) {
    console.log('Conversation onMounted: 检测到projectId参数', route.params.projectId, 'query:', route.query)
    const freelancerId = route.query.freelancerId ? parseInt(route.query.freelancerId) : null
    // 如果有freelancerId，需要查找对应的聊天项（可能通过submission关联）
    let chat = null
    if (freelancerId) {
      // 先尝试查找有submission的聊天项
      chat = chatList.value.find(c => 
        c.projectId === parseInt(route.params.projectId) && 
        c.freelancerId === freelancerId
      )
    }
    // 如果没有找到，查找没有submissionId的项目聊天
    if (!chat) {
      chat = chatList.value.find(c => c.projectId === parseInt(route.params.projectId) && !c.submissionId)
    }
    if (chat) {
      console.log('Conversation onMounted: 找到现有聊天项', chat)
      handleSelectChat(chat)
    } else {
      console.log('Conversation onMounted: 未找到现有聊天项，创建虚拟聊天项')
      try {
        const virtualChat = await createVirtualChat(null, parseInt(route.params.projectId), freelancerId)
        if (virtualChat) {
          console.log('Conversation onMounted: 虚拟聊天项创建成功，设置selectedChat', virtualChat)
          selectedChat.value = virtualChat
          selectedSubmissionId.value = null
          await loadMessagesByProject(parseInt(route.params.projectId))
          await loadProject(virtualChat.projectId)
        } else {
          console.error('创建虚拟聊天项失败: virtualChat为null')
          message.error('无法创建聊天会话，请稍后重试')
        }
      } catch (error) {
        console.error('创建虚拟聊天项失败:', error)
        message.error('创建聊天会话失败: ' + (error.response?.data?.msg || error.message))
      }
    }
  } else {
    console.log('Conversation onMounted: 没有路由参数')
  }
  
  // 每30秒刷新一次聊天列表
  refreshInterval = setInterval(() => {
    loadChatList()
    if (selectedSubmissionId.value) {
      loadMessages(selectedSubmissionId.value)
    } else if (selectedChat.value && selectedChat.value.projectId && !selectedChat.value.submissionId) {
      loadMessagesByProject(selectedChat.value.projectId)
    }
  }, 30000)
})

// 监听路由变化，如果有submissionId参数，自动选中或创建虚拟聊天项
watch(() => route.params.submissionId, async (newId, oldId) => {
  // 避免重复处理（onMounted已经处理过了）
  if (newId === oldId) {
    return
  }
  if (newId) {
    console.log('watch submissionId: 路由参数变化', newId)
    await loadChatList()
    const chat = chatList.value.find(c => c.submissionId === parseInt(newId))
    if (chat) {
      handleSelectChat(chat)
    } else {
      // 如果聊天列表中没有，创建虚拟聊天项
      try {
        const submissionRes = await request.get(`/api/submissions/${newId}`)
        if (submissionRes.code === '200' && submissionRes.data) {
          const virtualChat = await createVirtualChat(parseInt(newId), submissionRes.data.projectId)
          if (virtualChat) {
            selectedChat.value = virtualChat
            selectedSubmissionId.value = parseInt(newId)
            await loadMessages(parseInt(newId))
            await loadProject(virtualChat.projectId)
          } else {
            console.error('创建虚拟聊天项失败: virtualChat为null')
            message.error('无法创建聊天会话，请稍后重试')
          }
        } else {
          message.error('加载稿件信息失败')
        }
      } catch (error) {
        console.error('加载submission失败:', error)
        message.error('加载稿件信息失败: ' + (error.response?.data?.msg || error.message))
      }
    }
  }
}, { immediate: false })

watch(() => route.params.projectId, async (newId, oldId) => {
  // 避免重复处理（onMounted已经处理过了）
  if (newId === oldId) {
    return
  }
  if (newId && !route.params.submissionId) {
    console.log('watch projectId: 路由参数变化', newId, 'query:', route.query)
    const freelancerId = route.query.freelancerId ? parseInt(route.query.freelancerId) : null
    await loadChatList()
    // 如果有freelancerId，先尝试查找对应的聊天项
    let chat = null
    if (freelancerId) {
      chat = chatList.value.find(c => 
        c.projectId === parseInt(newId) && 
        c.freelancerId === freelancerId
      )
    }
    // 如果没有找到，查找没有submissionId的项目聊天
    if (!chat) {
      chat = chatList.value.find(c => c.projectId === parseInt(newId) && !c.submissionId)
    }
    if (chat) {
      handleSelectChat(chat)
    } else {
      // 如果聊天列表中没有，创建虚拟聊天项
      try {
        const virtualChat = await createVirtualChat(null, parseInt(newId), freelancerId)
        if (virtualChat) {
          selectedChat.value = virtualChat
          selectedSubmissionId.value = null
          await loadMessagesByProject(parseInt(newId))
          await loadProject(virtualChat.projectId)
        } else {
          console.error('创建虚拟聊天项失败: virtualChat为null')
          message.error('无法创建聊天会话，请稍后重试')
        }
      } catch (error) {
        console.error('创建虚拟聊天项失败:', error)
        message.error('创建聊天会话失败: ' + (error.response?.data?.msg || error.message))
      }
    }
  }
}, { immediate: false })

// 组件卸载时清除定时器和全局函数
onUnmounted(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval)
  }
  if (window.handleViewProfileCard) {
    delete window.handleViewProfileCard
  }
  if (window.handlePreviewImage) {
    delete window.handlePreviewImage
  }
})
</script>

<style scoped>
.conversation-container {
  display: flex;
  height: calc(100vh - 64px);
  background: linear-gradient(135deg, #f0f4f8 0%, #e8f0f5 100%);
}

/* 左侧聊天列表 */
.left-panel {
  width: 380px;
  background-color: #fff;
  display: flex;
  flex-direction: column;
  border-right: none;
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.06);
  border-radius: 0 16px 16px 0;
  overflow: hidden;
}

.chat-list-header {
  padding: 20px;
  background: linear-gradient(135deg, #00a6a7 0%, #00c4c4 100%);
  border-bottom: none;
}

.chat-list-header :deep(.ant-input-search) {
  border-radius: 24px;
  overflow: hidden;
}

.chat-list-header :deep(.ant-input) {
  border-radius: 24px 0 0 24px;
  border: none;
  padding-left: 16px;
  height: 42px;
}

.chat-list-header :deep(.ant-input-search-button) {
  border-radius: 0 24px 24px 0 !important;
  height: 42px;
  background: #fff;
  border: none;
  color: #00a6a7;
}

.filter-header {
  padding: 12px 20px;
  background: #fafcff;
  border-bottom: 1px solid #e8f0f5;
  display: flex;
  justify-content: flex-end;
}

.filter-header :deep(.ant-btn) {
  color: #00a6a7;
  font-weight: 500;
}

.filter-tabs {
  padding: 12px 20px;
  background: #fafcff;
  border-bottom: 1px solid #e8f0f5;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  animation: slideDown 0.3s ease-out;
}

.filter-tabs :deep(.ant-btn-primary) {
  background: #00a6a7;
  border-color: #00a6a7;
}

@keyframes slideDown {
  from {
    opacity: 0;
    max-height: 0;
  }
  to {
    opacity: 1;
    max-height: 100px;
  }
}

.chat-list-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  background: #fff;
}

.chat-list {
  padding: 8px;
}

.chat-item {
  display: flex;
  padding: 16px;
  cursor: pointer;
  border-radius: 12px;
  margin-bottom: 4px;
  transition: all 0.25s ease;
  border: 1px solid transparent;
}

.chat-item:hover {
  background: linear-gradient(135deg, #f0f9f9 0%, #e8f5f5 100%);
  transform: translateX(4px);
}

.chat-item.active {
  background: linear-gradient(135deg, #e6f7f7 0%, #d4f0f0 100%);
  border-color: #00a6a7;
  box-shadow: 0 4px 12px rgba(0, 166, 167, 0.15);
}

.chat-avatar {
  margin-right: 14px;
  position: relative;
}

.chat-avatar :deep(.ant-avatar) {
  border: 2px solid #e8f0f5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.chat-item.active .chat-avatar :deep(.ant-avatar) {
  border-color: #00a6a7;
}

.chat-content {
  flex: 1;
  min-width: 0;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.chat-name {
  font-weight: 600;
  color: #1a2b3c;
  font-size: 15px;
}

.chat-time {
  font-size: 12px;
  color: #8c9cac;
  background: #f0f4f8;
  padding: 2px 8px;
  border-radius: 10px;
}

.chat-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  font-size: 13px;
  color: #5a6a7a;
}

.chat-company {
  margin-right: 4px;
  font-weight: 500;
}

.chat-role {
  margin-left: 4px;
}

.chat-preview {
  font-size: 13px;
  color: #8c9cac;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f8fafc;
  padding: 6px 10px;
  border-radius: 8px;
}

.chat-preview img {
  flex-shrink: 0;
  max-width: 40px;
  max-height: 40px;
  border-radius: 6px;
  object-fit: cover;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

/* 右侧聊天详情 */
.right-panel {
  flex: 1;
  background-color: #fff;
  display: flex;
  flex-direction: column;
  margin: 12px;
  margin-left: 0;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.chat-detail {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background: linear-gradient(135deg, #fafcff 0%, #f5f8fc 100%);
  border-bottom: 1px solid #e8f0f5;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-left :deep(.ant-avatar) {
  border: 3px solid #00a6a7;
  box-shadow: 0 4px 12px rgba(0, 166, 167, 0.2);
}

.header-info {
  display: flex;
  flex-direction: column;
}

.header-name {
  font-weight: 600;
  font-size: 16px;
  color: #1a2b3c;
}

.header-meta {
  font-size: 13px;
  color: #5a6a7a;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
}

.divider {
  margin: 0 4px;
  color: #d9d9d9;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-right :deep(.ant-btn-link) {
  color: #00a6a7;
  font-weight: 500;
}

.project-info-bar {
  padding: 8px 20px;
  background: linear-gradient(135deg, #e6f7f7 0%, #d4f0f0 100%);
  border-bottom: 1px solid #c5e8e8;
}

.project-title {
  font-weight: 600;
  font-size: 15px;
  color: #00787a;
  margin-bottom: 4px;
}

.project-subtitle {
  font-size: 13px;
  color: #4a8a8c;
}

.messages-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px 20px;
  background: linear-gradient(180deg, #fafcff 0%, #fff 100%);
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-item {
  display: flex;
  gap: 14px;
}

.message-item.message-right {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-avatar :deep(.ant-avatar) {
  border: 2px solid #e8f0f5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.message-content-wrapper {
  max-width: 70%;
  display: flex;
  flex-direction: column;
}

.message-item.message-right .message-content-wrapper {
  align-items: flex-end;
}

.message-name {
  font-size: 12px;
  color: #8c9cac;
  margin-bottom: 6px;
  font-weight: 500;
}

.message-bubble {
  background: linear-gradient(135deg, #f5f8fc 0%, #eef2f7 100%);
  padding: 12px 16px;
  border-radius: 16px;
  border-bottom-left-radius: 4px;
  max-width: 100%;
  word-wrap: break-word;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #e8f0f5;
}

.bubble-right {
  background: linear-gradient(135deg, #00a6a7 0%, #00c4c4 100%);
  color: #fff;
  border-radius: 16px;
  border-bottom-right-radius: 4px;
  border-color: transparent;
  box-shadow: 0 4px 12px rgba(0, 166, 167, 0.3);
}

.message-text {
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 4px;
}

.message-text img {
  display: block;
  margin: 8px 0;
  max-width: 300px;
  max-height: 300px;
  width: auto;
  height: auto;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.bubble-right .message-text {
  color: #fff;
}

.bubble-right .message-text img {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
}

.message-time {
  font-size: 11px;
  color: #a0b0c0;
  margin-top: 6px;
}

.bubble-right .message-time {
  color: rgba(255, 255, 255, 0.8);
}

.input-area {
  border-top: 1px solid #e8f0f5;
  padding: 10px 20px;
  background: linear-gradient(180deg, #fff 0%, #fafcff 100%);
  position: relative;
  flex-shrink: 0;
}

.input-toolbar {
  display: flex;
  gap: 4px;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #e8f0f5;
}

.toolbar-btn {
  padding: 6px 12px;
  border-radius: 8px;
  color: #5a6a7a;
  transition: all 0.2s ease;
}

.toolbar-btn:hover {
  background: #e6f7f7;
  color: #00a6a7;
}

.emoji-picker {
  border: 1px solid #e8f0f5;
  border-radius: 12px;
  background: #fff;
  padding: 12px;
  margin-bottom: 12px;
  max-height: 220px;
  overflow-y: auto;
  position: relative;
  z-index: 10;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.emoji-categories {
  display: flex;
  gap: 4px;
  margin-bottom: 12px;
  border-bottom: 1px solid #e8f0f5;
  padding-bottom: 12px;
}

.emoji-categories :deep(.ant-btn) {
  border-radius: 8px;
}

.emoji-categories :deep(.ant-btn.active) {
  background: #e6f7f7;
  color: #00a6a7;
}

.emoji-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.emoji-item {
  font-size: 22px;
  cursor: pointer;
  padding: 6px;
  border-radius: 8px;
  transition: all 0.2s ease;
  user-select: none;
}

.emoji-item:hover {
  background: #e6f7f7;
  transform: scale(1.15);
}

.emoji-item:active {
  background: #d4f0f0;
}

.input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-wrapper :deep(.ant-input) {
  border-radius: 12px;
  border-color: #e8f0f5;
  resize: none;
}

.input-wrapper :deep(.ant-input:focus) {
  border-color: #00a6a7;
  box-shadow: 0 0 0 3px rgba(0, 166, 167, 0.1);
}

.input-actions {
  display: flex;
  gap: 8px;
}

.input-actions :deep(.ant-btn-link) {
  color: #00a6a7;
}

.input-hint {
  font-size: 12px;
  color: #a0b0c0;
}

.send-btn {
  align-self: flex-end;
  background: linear-gradient(135deg, #00a6a7 0%, #00c4c4 100%);
  border: none;
  border-radius: 10px;
  padding: 0 24px;
  height: 38px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(0, 166, 167, 0.3);
  transition: all 0.2s ease;
}

.send-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 166, 167, 0.4);
}

.send-btn:disabled {
  background: #d0d8e0;
  box-shadow: none;
}

.input-area-disabled {
  border-top: 1px solid #e8f0f5;
  padding: 16px 24px;
  background: #fff;
}

.common-phrases-picker {
  border: 1px solid #e8f0f5;
  border-radius: 12px;
  background: #fff;
  padding: 12px;
  margin-bottom: 12px;
  max-height: 200px;
  overflow-y: auto;
  position: relative;
  z-index: 10;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.phrases-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.phrase-item {
  padding: 6px 14px;
  border-radius: 20px;
  background: #f5f8fc;
  border: 1px solid #e8f0f5;
  color: #5a6a7a;
  transition: all 0.2s ease;
  user-select: none;
}

.phrase-item:hover {
  background: #e6f7f7;
  border-color: #00a6a7;
  color: #00a6a7;
}

.phrase-item:active {
  background: #d4f0f0;
}

/* 图片预览模态框样式 */
:deep(.image-preview-modal .ant-modal) {
  padding: 0;
}

:deep(.image-preview-modal .ant-modal-content) {
  padding: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
  border: none !important;
  margin: 0 auto;
  position: relative;
  top: 50%;
  transform: translateY(-50%);
}

:deep(.image-preview-modal .ant-modal-body) {
  padding: 0 !important;
  margin: 0;
  background: transparent !important;
}

:deep(.image-preview-modal .ant-modal-close) {
  color: #fff;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  width: 36px;
  height: 36px;
  top: 12px;
  right: 12px;
  z-index: 1000;
  line-height: 36px;
  transition: all 0.2s ease;
}

:deep(.image-preview-modal .ant-modal-close:hover) {
  background: rgba(0, 0, 0, 0.8);
  transform: scale(1.1);
}

/* 图片预览容器 */
.image-preview-container {
  padding: 0 !important;
  margin: 0 !important;
  background: transparent !important;
  display: block;
  overflow: visible;
  position: relative;
  width: auto;
  height: auto;
  line-height: 0;
}

.preview-image {
  width: auto;
  height: auto;
  max-width: none;
  max-height: none;
  object-fit: contain;
  border-radius: 8px;
  cursor: zoom-out;
  transition: transform 0.3s ease;
  display: block;
  margin: 0 auto;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

/* 空状态样式 */
.right-panel :deep(.ant-empty) {
  margin-top: 120px;
}

.right-panel :deep(.ant-empty-description) {
  color: #8c9cac;
  font-size: 14px;
}
</style>

