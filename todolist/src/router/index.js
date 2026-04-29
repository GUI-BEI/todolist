import { createRouter, createWebHistory } from 'vue-router'
import MainInterface from '../views/MainInterface.vue'
import TaskForm from '../components/TaskForm.vue'
import HistoryPage from '../components/HistoryPage.vue'
import Login from '../components/Login.vue'
import Sign from '../components/Sign.vue'
import Settings from '../components/Settings.vue'

// 需要登录才能访问的路由列表
const protectedRoutes = ['/', '/add', '/history', '/sign', '/settings']

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { 
      path: '/', 
      component: MainInterface,
      meta: { requiresAuth: true }  // 标记需要登录
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
      meta: { requiresAuth: false }  // 登录页不需要登录
    }
  ]
})

// 路由守卫：在每次路由跳转前检查
router.beforeEach((to, from, next) => {
  // 获取 token（判断是否登录）
  const token = localStorage.getItem('token')
  const isAuthenticated = !!token  // 转换为布尔值
  
  // 检查目标路由是否需要登录
  const requiresAuth = to.meta.requiresAuth
  
  if (requiresAuth && !isAuthenticated) {
    // 需要登录但未登录，跳转到登录页
    // 可以携带重定向参数，登录后跳转回来
    next({
      path: '/login',
      query: { redirect: to.fullPath }  // 记录原来想去的页面
    })
  } else if (to.path === '/login' && isAuthenticated) {
    // 已登录用户访问登录页，跳转到首页
    next('/')
  } else {
    // 正常放行
    next()
  }
})

export default router