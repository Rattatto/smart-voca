// 2026-11-25 - Smart Voca - 웹 앱 시작점 (React 루트 연결)
// 파일 위치: web/src/main.tsx

import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
// 기본 스타일(index.css)을 먼저 불러야 화면별 CSS(layout.css, study.css)가 그 위에 덮어쓸 수 있다
import './index.css'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
