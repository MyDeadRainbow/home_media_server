import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './style.css'
import 'core-js/stable'

createApp(App)
	.use(router)
	.mount('#app')
