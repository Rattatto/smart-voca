// 2026-11-25 - Smart Voca - 배포 단계 화면 스위치와 고정 값 (설정 숨김, 임시 동사 묶음, 공유 주소)
// 파일 위치: web/src/appConfig.ts

/**
 * 현재 배포 단계에서는 설정 화면(일일 개수, 난이도, CSV 가져오기)을 숨긴다 (2026-11-25 사용자 결정).
 * 설정 코드는 그대로 두었으므로 true 로 바꾸면 다시 보인다.
 */
export const SHOW_SETTINGS = false

/** DB 에 단어가 하나도 없을 때 자동으로 넣는 임시 동사 묶음 (직접 작성, 120개, 100% 동사). `public/` 에 있다. */
export const STARTER_WORDS_FILE_NAME = 'starter_verbs.csv'

/** 학습 카드에서 영단어를 따라 써야 하는 횟수. */
export const STUDY_TYPING_REPEAT = 2

/** QR 코드가 가리키는 배포 주소. QR 이미지는 `public/qr-smart-voca.svg` (ai-agent/scripts 로 재생성 아님, 주소가 바뀌면 다시 만든다). */
export const APP_URL = 'https://rattatto.github.io/smart-voca/'
export const QR_IMAGE_FILE_NAME = 'qr-smart-voca.svg'
