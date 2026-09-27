// 입력 규칙은 이 파일에서 공통으로 관리한다.
export const RULES = {
  EMAIL_PATTERN: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
  PASSWORD_PATTERN: /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,20}$/,
  NICKNAME_PATTERN: /^[가-힣A-Za-z0-9]{2,10}$/,
  NICKNAME_MIN: 2,
  NICKNAME_MAX: 10,
  TITLE_MAX: 100,         // 응답 명세 요청 표: 1~100자 (D-08 최종 확정 필요)
  TITLE_MIN: null,        // TODO U-2 (와이어프레임 메모 "3글자 이상이어야 할 것 같음")
  CONTENT_MAX: null,      // TODO D-08
  URL_MAX_COUNT: null,    // TODO D-08
  HASHTAG_MAX_COUNT: null,// TODO D-09
  HASHTAG_MAX_LENGTH: 50, // 응답 명세 요청 표: 각 1~50자 (D-09 최종 확정 필요)
}
