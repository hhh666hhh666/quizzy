import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { title: '登录' } },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      { path: '', redirect: '/questions' },
      { path: 'questions', name: 'questions', component: () => import('@/views/QuestionListView.vue'), meta: { title: '题库' } },
      { path: 'papers', name: 'papers', component: () => import('@/views/PaperListView.vue'), meta: { title: '试卷' } },
      { path: 'quiz/quick', name: 'quick', component: () => import('@/views/QuickQuizView.vue'), meta: { title: '快速练习' } },
      { path: 'quiz/:id', name: 'quiz', component: () => import('@/views/QuizView.vue'), meta: { title: '答题' } },
      { path: 'quiz/:id/result', name: 'quiz-result', component: () => import('@/views/QuizResultView.vue'), meta: { title: '答题结果' } },
      { path: 'history', name: 'history', component: () => import('@/views/HistoryView.vue'), meta: { title: '答题记录' } },
      { path: 'wrong-book', name: 'wrong-book', component: () => import('@/views/WrongBookView.vue'), meta: { title: '错题本' } },
      { path: 'favorites', name: 'favorites', component: () => import('@/views/FavoriteView.vue'), meta: { title: '收藏夹' } },
      { path: 'profile', name: 'profile', component: () => import('@/views/ProfileView.vue'), meta: { title: '我的账户' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('quizzy_token')
  if (to.path !== '/login' && !token) {
    return '/login'
  }
  if (to.path === '/login' && token) {
    return '/questions'
  }
  return true
})

// 标签页标题跟着路由走（顶栏那行字与 document.title 同源，都是 meta.title）
router.afterEach((to) => {
  const title = to.meta.title as string | undefined
  document.title = title ? `${title} · Quizzy` : 'Quizzy'
})

export default router
