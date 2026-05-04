import { createRouter, createWebHistory } from 'vue-router'
import MainInterface from '../views/MainInterface.vue'
import MonthView from '../views/MonthView.vue'
import TaskForm from '../components/TaskForm.vue'
import HistoryPage from '../components/HistoryPage.vue'
import Login from '../components/Login.vue'
import Sign from '../components/Sign.vue'
import Settings from '../components/Settings.vue'
import ForgotPassword from '../components/ForgotPassword.vue'
import RecurringTaskForm from '../components/RecurringTaskForm.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { 
      path: '/', 
      redirect: '/login'  // 添加这行，将根路径重定向到登录页
    },
    { 
      path: '/week', 
      component: MainInterface,
      meta: { requiresAuth: true }
    },
    { 
      path: '/month', 
      component: MonthView,
      meta: { requiresAuth: true }
    },
    { 
      path: '/add', 
      component: TaskForm,
      meta: { requiresAuth: true }
    },
    { 
      path: '/history', 
      component: HistoryPage,
      meta: { requiresAuth: true }
    },
    { 
      path: '/sign', 
      component: Sign,
      meta: { requiresAuth: true }
    },
    { 
      path: '/settings', 
      component: Settings,
      meta: { requiresAuth: true }
    },
    { 
      path: '/login', 
      component: Login,
      meta: { requiresAuth: false }
    },
    { 
      path: '/forgot-password',  // 新增忘记密码路由
      component: ForgotPassword,
      meta: { requiresAuth: false }
    },
    { 
      path: '/recurring', 
      component: RecurringTaskForm,
      meta: { requiresAuth: true }
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const isAuthenticated = !!token
  const requiresAuth = to.meta.requiresAuth
  
  if (requiresAuth && !isAuthenticated) {
    next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
  } else if (to.path === '/login' && isAuthenticated) {
    next('/week')  // 改成 /week，因为原来的 '/' 现在是重定向到 login
  } else {
    next()
  }
})

export default router