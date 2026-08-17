import { defineConfig } from 'vite';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig({
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
    strictPort: true,
    allowedHosts: true,
    hmr: {
      protocol: 'ws',
      clientPort: 5173,
    },
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.setHeader('Origin', 'http://localhost:5173');
          });
        },
      },
      '/ws': {
        target: 'http://127.0.0.1:8080',
        ws: true,
        changeOrigin: true,
        configure: (proxy) => {
          const asLocalhost = (proxyReq: { setHeader: (name: string, value: string) => void }) => {
            proxyReq.setHeader('Origin', 'http://localhost:5173');
          };
          proxy.on('proxyReq', asLocalhost);
          proxy.on('proxyReqWs', asLocalhost);
        },
      },
    },
  },
});
