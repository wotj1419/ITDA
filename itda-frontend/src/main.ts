import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'

// Styles
import './assets/styles/variables.css'
import './assets/styles/base.css'
import './assets/styles/utilities.css'
import './assets/styles/node-canvas.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
