import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [svelte()],
  server: {
    proxy: {
      '/tracker' : 'http://localhost:8011',
      '/settings' : 'http://localhost:8011',
    }
  }
})
