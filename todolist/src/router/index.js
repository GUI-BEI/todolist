import { createRouter, createWebHistory } from 'vue-router'
import MainInterface from '../views/MainInterface.vue'
import TaskForm from '../components/TaskForm.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { 
      path: '/', 
      component: MainInterface, 
      meta: { bodyClass: 'home-page-bg' } // 首页特有样式名
    },
    { 
      path: '/add', 
      component: TaskForm, 
      meta: { bodyClass: 'form-page-bg' } // 添加页特有样式名
    }
  ]
})
export default router
