// 입력 규칙은 이 파일에서 공통으로 관리한다.
export const RULES = {
  EMAIL_PATTERN: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
  PASSWORD_PATTERN: /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,20}$/,
  NICKNAME_PATTERN: /^[가-힣A-Za-z0-9]{2,10}$/,
  NICKNAME_MIN: 2,
  NICKNAME_MAX: 10,
  // 게시글. 백엔드 PostPolicy와 같은 값이어야 한다.
  TITLE_MAX: 100,
  CONTENT_MAX: 1000,
  URL_MAX_COUNT: 5,
  URL_MAX_LENGTH: 2048,
  HASHTAG_MAX_COUNT: 10,
  HASHTAG_MAX_LENGTH: 20,
  HASHTAG_PATTERN: /^[가-힣a-z0-9_]{1,20}$/, // 다듬은 뒤(# 제거·소문자)의 값에 적용
}
