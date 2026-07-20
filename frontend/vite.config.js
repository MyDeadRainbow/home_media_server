import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import legacy from '@vitejs/plugin-legacy'
import path from 'path';

export default defineConfig(({mode}) => {
    const env = loadEnv(mode, path.resolve(process.cwd(), '..'))
    const FRONTEND_PORT = env.FRONTEND_PORT || 5173

    return {
        plugins: [
            vue(),
            legacy({
                targets: ['defaults', 'ie >= 11', 'chrome >= 37', 'safari >= 9'],
                additionalLegacyPolyfills: ['regenerator-runtime/runtime'],
                renderLegacyChunks: true,
                modernPolyfills: true
            })
        ],
        server: {
            host: '0.0.0.0',
            port: FRONTEND_PORT
        }
    }
})
