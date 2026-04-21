import { createRouter, createWebHistory } from 'vue-router'
import MainInterface from '../views/MainInterface.vue'
import TaskForm from '../components/TaskForm.vue'

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
    }
  ]
})
export default router
