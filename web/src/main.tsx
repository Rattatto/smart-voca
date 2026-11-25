// 2026-11-25 - Smart Voca - 웹 앱 시작점 (React 루트 연결)
// 파일 위치: web/src/main.tsx

import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.tsx'
import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
