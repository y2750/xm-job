import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/front/home' },
    {
      path: '/manager',
      component: () => import('@/views/Manager.vue'),
      children: [
        { path: 'home', meta: { name: '系统首页' }, component: () => import('@/views/manager/Home.vue'),  },
        { path: 'projects', meta: { name: '项目管理' }, component: () => import('@/views/manager/Projects.vue'), },
        { path: 'projects/:id', meta: { name: '项目详情' }, component: () => import('@/views/manager/ProjectDetail.vue'), },
        { path: 'submissions', meta: { name: '稿件管理' }, component: () => import('@/views/manager/Submissions.vue'), },
        { path: 'submissions/:id', meta: { name: '稿件详情' }, component: () => import('@/views/manager/SubmissionDetail.vue'), },
        { path: 'admin', meta: { name: '管理员信息' }, component: () => import('@/views/manager/Admin.vue'), },
        { path: 'freelancers', meta: { name: '自由职业者' }, component: () => import('@/views/manager/Freelancers.vue'), },
        { path: 'enterprises', meta: { name: '企业用户' }, component: () => import('@/views/manager/Enterprises.vue'), },
        { path: 'notice', meta: { name: '系统公告' }, component: () => import('@/views/manager/Notice.vue'), },
        { path: 'messages', meta: { name: '系统通知' }, component: () => import('@/views/manager/Messages.vue'), },
        { path: 'person', meta: { name: '个人资料' }, component: () => import('@/views/manager/Person.vue'), },
        { path: 'password', meta: { name: '修改密码' }, component: () => import('@/views/manager/Password.vue'), },
      ]
    },
    {
      path: '/front',
      component: () => import('@/views/Front.vue'),
      children: [
        { path: 'home', component: () => import('@/views/front/Home.vue'),  },
        { path: 'person', component: () => import('@/views/front/Person.vue'),  },
        { path: 'positionDetail', component: () => import('@/views/front/PositionDetail.vue'),  },
        { path: 'collect', component: () => import('@/views/front/Collect.vue'),  },
        { path: 'resume', component: () => import('@/views/front/Resume.vue'),  },
        { path: 'resumeEdit', component: () => import('@/views/front/ResumeEdit.vue'),  },
        { path: 'submit', component: () => import('@/views/front/Submit.vue'),  },
        { path: 'employ', component: () => import('@/views/front/Employ.vue'),  },
        { path: 'search', component: () => import('@/views/front/Search.vue'),  },
        // 项目相关路由（使用 Front 布局）
        { path: 'projects', component: () => import('@/views/projects/ProjectList.vue') },
        { path: 'projects/:id', component: () => import('@/views/projects/ProjectDetail.vue') },
        { path: 'projects/:projectId/submissions', component: () => import('@/views/projects/ProjectSubmissions.vue') },
        { path: 'projects/:projectId/deliverables', component: () => import('@/views/projects/ProjectDeliverables.vue') },
        { path: 'projects/publish', component: () => import('@/views/projects/ProjectPublish.vue') },
        // 接单相关路由（使用 Front 布局）
        { path: 'orders', component: () => import('@/views/orders/MyOrders.vue') },
        // 稿件相关路由（使用 Front 布局）
        { path: 'submissions', component: () => import('@/views/submissions/SubmissionList.vue') },
        { path: 'submissions/:id', component: () => import('@/views/submissions/SubmissionDetail.vue') },
        { path: 'submissions/submit/:projectId', component: () => import('@/views/submissions/SubmissionSubmit.vue') },
        { path: 'submissions/edit/:id', component: () => import('@/views/submissions/SubmissionEdit.vue') },
        // 成品相关路由（使用 Front 布局）
        { path: 'deliverables/submit/:submissionId', component: () => import('@/views/deliverables/DeliverableSubmit.vue') },
        // 用户资料路由（使用 Front 布局）
        { path: 'freelancer/profile', component: () => import('@/views/freelancer/FreelancerProfile.vue') },
        { path: 'enterprise/profile', component: () => import('@/views/enterprise/EnterpriseProfile.vue') },
        { path: 'enterprise/dashboard', component: () => import('@/views/enterprise/EnterpriseDashboard.vue') },
        // 消息相关路由（使用 Front 布局）
        { path: 'messages', component: () => import('@/views/messages/MessageList.vue') },
        { path: 'messages/:projectId', component: () => import('@/views/messages/MessageDetail.vue') },
        { path: 'chat/:submissionId', component: () => import('@/views/messages/Chat.vue') },
        // 支付相关路由（使用 Front 布局）
        { path: 'balance', component: () => import('@/views/payment/Balance.vue') },
        { path: 'withdraw', component: () => import('@/views/payment/Withdraw.vue') },
        { path: 'payment/confirm/:submissionId', component: () => import('@/views/payment/PaymentConfirm.vue') },
        { path: 'payment/freelancer-confirm/:submissionId', component: () => import('@/views/payment/FreelancerPaymentConfirm.vue') },
      ]
    },
    { path: '/login', component: () => import('@/views/Login.vue') },
    { path: '/register', component: () => import('@/views/Register.vue') },
    { path: '/404', component: () => import('@/views/404.vue') },
    { path: '/resumeView', component: () => import('@/views/ResumeView.vue') },
    // 项目相关路由
    { path: '/projects', component: () => import('@/views/projects/ProjectList.vue') },
    { path: '/projects/:id', component: () => import('@/views/projects/ProjectDetail.vue') },
    { path: '/projects/publish', component: () => import('@/views/projects/ProjectPublish.vue') },
    // 稿件相关路由
    { path: '/submissions', component: () => import('@/views/submissions/SubmissionList.vue') },
    { path: '/submissions/:id', component: () => import('@/views/submissions/SubmissionDetail.vue') },
    { path: '/submissions/submit/:projectId', component: () => import('@/views/submissions/SubmissionSubmit.vue') },
    // 用户资料路由
    { path: '/freelancer/profile', component: () => import('@/views/freelancer/FreelancerProfile.vue') },
    { path: '/enterprise/profile', component: () => import('@/views/enterprise/EnterpriseProfile.vue') },
    { path: '/enterprise/dashboard', component: () => import('@/views/enterprise/EnterpriseDashboard.vue') },
    // 消息相关路由
    { path: '/messages', component: () => import('@/views/messages/MessageList.vue') },
    { path: '/messages/:projectId', component: () => import('@/views/messages/MessageDetail.vue') },
    { path: '/chat/:submissionId', component: () => import('@/views/messages/Chat.vue') },
    { path: '/:pathMatch(.*)', redirect: '/404' }
  ]
})

export default router
