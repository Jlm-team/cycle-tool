import Vue from 'vue'
import App from './App'
import router from './router'
import ViewUI from 'view-design'
import axios from 'axios'
import '@babel/polyfill'
import 'view-design/dist/styles/iview.css'
Vue.config.productionTip = false
Vue.use(ViewUI)
axios.defaults.baseURL = 'http://localhost:8086/api'
// 全局注册，之后可在其他组件中通过 this.$axios 发送数据
Vue.prototype.axios = axios
Vue.config.productionTip = false

new Vue({
  el: '#app',
  router,
  components: { App },
  template: '<App/>'
})
