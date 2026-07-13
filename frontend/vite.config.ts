import path from 'node:path'
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    // Single-origin in dev: proxy /api/* to the Quarkus backend (ADR-0007, no CORS).
    // The trailing slash is load-bearing: a bare '/api' prefix would also swallow
    // SPA routes like /apis/:id on hard loads (FEAT-002).
    // 127.0.0.1, not localhost: Node resolves localhost to ::1 first, where other
    // tools (e.g. OrbStack) may listen on 8080; Quarkus serves IPv4.
    proxy: {
      '/api/': 'http://127.0.0.1:8080',
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
    coverage: {
      // Information, not a gate: no thresholds on purpose — the uncovered-lines
      // report is for reading at PR time, not a percentage to satisfy.
      include: ['src/**'],
      // Generated (orval) and test-harness code would only pad the numbers.
      exclude: ['src/api/**', 'src/test/**'],
      reporter: ['text', 'html'],
    },
  },
})
