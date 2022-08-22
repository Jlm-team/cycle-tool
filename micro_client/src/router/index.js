import { createRouter,createWebHashHistory} from "vue-router";

const index = ()=>import('@/views/index')
export default createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      name: 'index',
      component:index,
    },
  ],
})
