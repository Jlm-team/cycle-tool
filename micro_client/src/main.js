import { createApp } from 'vue'
import App from './App.vue'
import { Graph } from '@antv/x6';
import axios from 'axios'
import VueGridLayout from "vue-grid-layout/src/components"
import ViewUI from 'view-design'

const app = createApp(App)
app.mount('#app')
app.config.globalProperties.$axios = axios
app.config.globalProperties.$graph = Graph
app.use(VueGridLayout)
app.use(ViewUI)
