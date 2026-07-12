import { defineConfig } from 'astro/config'

/** GitHub Pages project site: https://heatzyv2.github.io/prime-client/ */
export default defineConfig({
  site: 'https://heatzyv2.github.io',
  base: '/prime-client',
  output: 'static',
  compressHTML: true
})
