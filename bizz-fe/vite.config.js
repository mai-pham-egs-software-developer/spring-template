import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Distinct from ../frontend's default 5173 so both dev servers can run at once.
  server: {
    port: 5174,
  },
})
