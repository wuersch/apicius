import '@testing-library/jest-dom/vitest'

// jsdom lacks ResizeObserver; Radix primitives (radio-group's use-size) observe elements on
// mount. A no-op stand-in is enough — no test asserts on resize behavior.
class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}

globalThis.ResizeObserver ??= ResizeObserverStub as unknown as typeof ResizeObserver
