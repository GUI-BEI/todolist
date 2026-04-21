import { createRouter, createWebHistory } from 'vue-router'
import MainInterface from '../views/MainInterface.vue'
import TaskForm from '../components/TaskForm.vue'
import HistoryPage from '../components/HistoryPage.vue'

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
    }
  ]
})
export default router
