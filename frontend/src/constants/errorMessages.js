// 결정 보드 논점 03(B안): 화면 문구는 프론트가 소유한다.
// 서버 message 는 매핑이 없을 때의 예비 문구로만 쓴다.
export const ERROR_MESSAGES = {
  UNAUTHORIZED: '로그인이 필요해요. 다시 로그인해 주세요.',
  INTERNAL_ERROR: '요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요.',
  INVALID_INPUT: '입력값을 확인해 주세요.',
  LOGIN_FAILED: '이메일 또는 비밀번호를 다시 확인해 주세요.',
  ACCOUNT_WITHDRAWN: '탈퇴 처리된 계정이에요.',
  MEMBER_NOT_MATCHED: '입력하신 이름과 이메일로 가입된 회원이 없어요.',
  PASSWORD_MISMATCH: '비밀번호가 일치하지 않아요.',
  PASSWORD_CONFIRM_MISMATCH: '비밀번호 확인이 일치하지 않아요.',
  PASSWORD_RECENTLY_USED: '최근에 사용한 비밀번호는 다시 쓸 수 없어요.',
  EMAIL_DUPLICATED: '이미 가입된 이메일이에요.',
  NICKNAME_DUPLICATED: '이미 사용 중인 닉네임이에요.',
  NICKNAME_SAME_AS_CURRENT: '지금 쓰고 있는 닉네임과 같아요.',
  POST_NOT_FOUND: '삭제되었거나 존재하지 않는 글이에요.',
  POST_NOT_OWNED: '본인이 작성한 글만 수정·삭제할 수 있어요.',
  HASHTAG_NOT_FOUND: '이미 삭제된 해시태그예요.',
  NETWORK_ERROR: '서버에 연결할 수 없어요. 네트워크를 확인해 주세요.',
}

const DEFAULT_MESSAGE = '요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요.'

export const resolveMessage = (code, serverMessage) =>
  ERROR_MESSAGES[code] ?? serverMessage ?? DEFAULT_MESSAGE
