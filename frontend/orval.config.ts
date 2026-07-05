import { defineConfig } from 'orval'

// Generates the typed client + TanStack Query hooks from the backend's code-first
// OpenAPI document (ADR-0002). Run `npm run generate` with the backend dev server up;
// re-run whenever the API contract changes.
export default defineConfig({
  apicius: {
    input: 'http://127.0.0.1:8080/q/openapi',
    output: {
      mode: 'tags-split',
      target: 'src/api/endpoints',
      schemas: 'src/api/model',
      client: 'react-query',
      httpClient: 'fetch',
      // Requests are same-origin in dev (Vite proxies /api → backend, ADR-0007) and in
      // prod (nginx). No baseUrl: the spec's paths already carry /api/v1 (ADR-0002).
      clean: true,
      prettier: false,
      override: {
        // Every generated call goes through the auth-aware fetch wrapper, which
        // attaches the in-memory Bearer token (ADR-0005).
        mutator: {
          path: 'src/api/mutator/custom-fetch.ts',
          name: 'customFetch',
        },
      },
    },
  },
})
