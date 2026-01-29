import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'

// Styles
import './assets/styles/variables.css'
import './assets/styles/base.css'
import './assets/styles/utilities.css'
import '@vue-flow/node-resizer/dist/style.css'
import './assets/styles/node-canvas.css'

import './assets/styles/node-canvas.css'

async function enableMocking() {
  const useMock = import.meta.env.VITE_USE_MOCK === 'true'

  if (import.meta.env.DEV && useMock) {
    const { worker } = await import('./mocks/browser')
    return worker.start({
      onUnhandledRequest: 'bypass',
    })
  }
}

const app = createApp(App)

app.use(createPinia())
app.use(router)

enableMocking().then(() => {
    app.mount('#app')
})
