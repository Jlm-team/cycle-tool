import {createApp} from 'vue'
import App from './App'
import router from './router'
import ViewUI from 'view-ui-plus'
import axios from 'axios'
import 'view-ui-plus/dist/styles/viewuiplus.css'

const app = createApp(App)
app.use(ViewUI)
/* eslint-disable no-new */
// app.config.globalProperties.routerAppend = (path, pathToAppend) => {
//   return path + (path.endsWith('/') ? '' : '/') + pathToAppend
// }
app.use(router)
app.mount('#app')
app.config.globalProperties.axios = axios
