import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
  },
  // sockjs-client (used by @stomp/stompjs's SockJS transport) references the
  // Node.js `global` object (e.g. `global.crypto` in lib/utils/browser-crypto.js).
  // Vite's dev server and Rollup-based production build do not polyfill Node
  // globals the way Webpack did, so without this the browser throws
  // "ReferenceError: global is not defined" as soon as sockjs-client loads.
  // Aliasing the bare identifier `global` to `globalThis` (which every modern
  // browser provides) is the standard, minimal fix — it's a build-time find/
  // replace, not a runtime shim, so it has zero effect on app behavior.
  //
  // Two places need this:
  //  1. `define` — applied by Rollup/esbuild when transforming project source
  //     and during the production build (`vite build`).
  //  2. `optimizeDeps.esbuildOptions.define` — applied when Vite's dev server
  //     pre-bundles node_modules dependencies (esbuild's separate dependency
  //     scan/build step does NOT automatically inherit the top-level `define`).
  // Without both, `npm run build` is fixed but `npm run dev` still throws.
  define: {
    global: 'globalThis',
  },
  optimizeDeps: {
    esbuildOptions: {
      define: {
        global: 'globalThis',
      },
    },
  },
})
