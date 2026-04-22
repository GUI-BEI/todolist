import { createRouter, createWebHistory } from 'vue-router'
import MainInterface from '../views/MainInterface.vue'
import TaskForm from '../components/TaskForm.vue'
import HistoryPage from '../components/HistoryPage.vue'
import Login from '../components/Login.vue'

// 如果不希望用户在未登录的情况下访问/ /add等界面,可以在此处添加"路由守卫"

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { 
      path: '/', 
      component: MainInterface, 
    },
    { 
      path: '/add', 
      component: TaskForm, 
    },
    { 
      path: '/history', 
      component: HistoryPage, 
    },
    { 
      path: '/login', 
      component: Login, 
    }
  ]
})
export default router
