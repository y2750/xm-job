import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import '@/assets/css/global.css'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'

// 配置 dayjs 中文
dayjs.locale('zh-cn')

const app = createApp(App)

app.use(router)
app.use(Antd, {
  locale: zhCN
})
app.mount('#app')
