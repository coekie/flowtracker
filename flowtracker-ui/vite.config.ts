import {defineConfig} from 'vite';
import {svelte} from '@sveltejs/vite-plugin-svelte';

// https://vitejs.dev/config/
export default defineConfig({
  base: './', // relative links, for serving snapshots on a different url
  plugins: [svelte()],
  server: {
    proxy: {
      '/tracker': 'http://localhost:8011',
      '/tree': 'http://localhost:8011',
      '/settings': 'http://localhost:8011',
      '/code': 'http://localhost:8011',
    },
  },
  build: {
    outDir: 'dist/static',
  },
});
