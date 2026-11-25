// 2026-11-25 - Smart Voca - Vite 빌드 설정 (React, PWA, GitHub Pages 경로)
// 파일 위치: web/vite.config.ts

import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import { VitePWA } from 'vite-plugin-pwa'

/**
 * GitHub Pages 는 https://rattatto.github.io/smart-voca/ 처럼 저장소 이름 아래에 올라간다.
 * 그래서 배포용 빌드에서만 기본 경로를 `/smart-voca/` 로 두고, 개발 서버는 `/` 그대로 쓴다.
 */
const PAGES_BASE = '/smart-voca/'

export default defineConfig(({ command, isPreview }) => ({
  // `vite preview` 는 command 가 'serve' 라서 isPreview 로 따로 잡는다. 빌드 결과를 배포와 같은 경로로 미리 본다
  base: command === 'build' || isPreview ? PAGES_BASE : '/',
  plugins: [
    react(),
    VitePWA({
      // 새 버전을 배포하면 다음에 열 때 서비스 워커가 알아서 바꾼다 (사용자 확인 없이)
      registerType: 'autoUpdate',
      // 서비스 워커 등록 코드를 index.html 에 자동으로 넣는다 (화면 코드 수정 불필요)
      injectRegister: 'auto',
      includeAssets: ['favicon.svg', 'icons/app-icon.svg'],
      manifest: {
        name: '스마트보카',
        short_name: '스마트보카',
        description: '동사만 알아도 대박이다. 매일 보는 영어 단어장',
        lang: 'ko',
        display: 'standalone',
        // intro.html 팔레트: 돌판 (상단 띠) 과 크림 (바탕)
        theme_color: '#262C34',
        background_color: '#FAF5EC',
        icons: [
          { src: 'icons/icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: 'icons/icon-512.png', sizes: '512x512', type: 'image/png' },
          { src: 'icons/icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
        ],
      },
      workbox: {
        // 오프라인에서도 열리도록 빌드 결과 전체와 기능어 CSV 를 미리 저장한다 (11/11 요청 2.2, 개발 중단 후에도 동작)
        globPatterns: ['**/*.{js,css,html,svg,png,csv}'],
      },
    }),
  ],
}))
