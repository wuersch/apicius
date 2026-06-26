import { defineConfig } from 'orval'

// Generates the typed client + TanStack Query hooks from the backend's code-first
// OpenAPI document (ADR-0002). Run `npm run generate` with the backend dev server up;
// re-run whenever the API contract changes.
export default defineConfig({
  apicius: {
    input: 'http://localhost:8080/q/openapi',
    output: {
      mode: 'tags-split',
      target: 'src/api/endpoints',
      schemas: 'src/api/model',
      client: 'react-query',
      httpClient: 'fetch',
      // Requests are same-origin in dev (Vite proxies /api → backend, ADR-0007) and in
      // prod (nginx). The generated calls are prefixed with the API base path (ADR-0002).
      baseUrl: '/api/v1',
      clean: true,
      prettier: false,
    },
  },
})
