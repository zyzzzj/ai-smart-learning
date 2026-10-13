import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue'), meta: { title: '登录' } },

  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/home',
    children: [
      { path: 'home', name: 'home', component: () => import('../views/student/Home.vue'), meta: { title: '学习首页', icon: 'HomeFilled' } },
      { path: 'practice', name: 'practice', component: () => import('../views/student/Practice.vue'), meta: { title: '智能刷题', icon: 'EditPen' } },
      { path: 'wrong-book', name: 'wrong-book', component: () => import('../views/student/WrongBook.vue'), meta: { title: '错题本', icon: 'Notebook' } },
      { path: 'exams', name: 'exams', component: () => import('../views/student/Exams.vue'), meta: { title: '考试中心', icon: 'Trophy' } },
      { path: 'exam/:id', name: 'exam-take', component: () => import('../views/student/ExamTake.vue'), meta: { title: '在线考试', hidden: true } },
      { path: 'exam/:id/result', name: 'exam-result', component: () => import('../views/student/ExamResult.vue'), meta: { title: '考试成绩', hidden: true } },
      { path: 'report', name: 'report', component: () => import('../views/student/Report.vue'), meta: { title: '学习报告', icon: 'DataAnalysis' } },
      { path: 'history', name: 'history', component: () => import('../views/student/History.vue'), meta: { title: '刷题记录', icon: 'Clock' } },
      { path: 'profile', name: 'profile', component: () => import('../views/student/Profile.vue'), meta: { title: '个人中心', icon: 'User' } },

      { path: 'admin/questions', name: 'admin-questions', component: () => import('../views/admin/Questions.vue'), meta: { title: '题库管理', icon: 'Files', admin: true } },
      { path: 'admin/ai-generate', name: 'admin-ai', component: () => import('../views/admin/AiGenerate.vue'), meta: { title: 'AI 智能出题', icon: 'MagicStick', admin: true } },
      { path: 'admin/exams', name: 'admin-exams', component: () => import('../views/admin/ExamManage.vue'), meta: { title: '考试与组卷', icon: 'Tickets', admin: true } },
      { path: 'admin/knowledge', name: 'admin-knowledge', component: () => import('../views/admin/Knowledge.vue'), meta: { title: '科目与知识点', icon: 'Collection', admin: true } },
      { path: 'admin/files', name: 'admin-files', component: () => import('../views/admin/Files.vue'), meta: { title: '资源管理', icon: 'FolderOpened', admin: true } },
      { path: 'admin/tasks', name: 'admin-tasks', component: () => import('../views/admin/AiTasks.vue'), meta: { title: 'AI 任务记录', icon: 'List', admin: true } }
    ]
  },

  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} · AI 智能学习系统` : 'AI 智能学习系统'
  const token = localStorage.getItem('ai_token')
  if (to.path === '/login') {
    return next()
  }
  if (!token) {
    return next('/login')
  }
  if (to.meta.admin) {
    const user = JSON.parse(localStorage.getItem('ai_user') || 'null')
    if (!user || user.role !== 'ADMIN') {
      return next('/home')
    }
  }
  next()
})

export default router
