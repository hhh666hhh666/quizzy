import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue') },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      { path: '', redirect: '/questions' },
      { path: 'questions', name: 'questions', component: () => import('@/views/QuestionListView.vue') },
      { path: 'papers', name: 'papers', component: () => import('@/views/PaperListView.vue') },
      { path: 'quiz/quick', name: 'quick', component: () => import('@/views/QuickQuizView.vue') },
      { path: 'quiz/:id', name: 'quiz', component: () => import('@/views/QuizView.vue') },
      { path: 'quiz/:id/result', name: 'quiz-result', component: () => import('@/views/QuizResultView.vue') },
      { path: 'history', name: 'history', component: () => import('@/views/HistoryView.vue') },
      { path: 'wrong-book', name: 'wrong-book', component: () => import('@/views/WrongBookView.vue') }
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

export default router
