// 2026-11-25 - Smart Voca - 배포 주소 QR 코드와 링크 (다른 기기·사람에게 앱 공유)
// 파일 위치: web/src/ui/SharePanel.tsx

import { APP_URL, QR_IMAGE_FILE_NAME } from '../appConfig'
import './study.css'

/** QR 이미지는 정적 파일이라 오프라인에서도 보인다 (서비스 워커가 미리 저장). */
export function SharePanel() {
  return (
    <footer className="share" aria-label="앱 공유">
      <img className="share-qr" src={`${import.meta.env.BASE_URL}${QR_IMAGE_FILE_NAME}`} alt={`스마트보카 주소 QR 코드: ${APP_URL}`} width={120} height={120} />
      <div className="share-text">
        <p className="field-label">휴대폰으로 열기</p>
        <p className="muted">카메라로 QR 코드를 찍으면 이 앱이 열립니다. 홈 화면에 추가하면 앱처럼 쓸 수 있습니다.</p>
        <a className="share-url" href={APP_URL}>
          {APP_URL}
        </a>
      </div>
    </footer>
  )
}
