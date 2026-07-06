import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path';

export default defineConfig(({mode}) => {
    const env = loadEnv(mode, path.resolve(process.cwd(), '..'))
    const FRONTEND_PORT = env.FRONTEND_PORT || 5173

    return {
        plugins: [vue()],
        server: {
            host: '0.0.0.0',
            port: FRONTEND_PORT
        }
    }
})
