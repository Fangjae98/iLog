// 팀 확정 전까지 값을 임의로 넣지 않는다. 확정되면 여기만 고친다.
export const RULES = {
  PASSWORD_PATTERN: null, // TODO D-16
  NICKNAME_MIN: null,     // TODO D-16
  NICKNAME_MAX: null,     // TODO D-16
  TITLE_MAX: 100,         // 응답 명세 요청 표: 1~100자 (D-08 최종 확정 필요)
  TITLE_MIN: null,        // TODO U-2 (와이어프레임 메모 "3글자 이상이어야 할 것 같음")
  CONTENT_MAX: null,      // TODO D-08
  URL_MAX_COUNT: null,    // TODO D-08
  HASHTAG_MAX_COUNT: null,// TODO D-09
  HASHTAG_MAX_LENGTH: 50, // 응답 명세 요청 표: 각 1~50자 (D-09 최종 확정 필요)
}
