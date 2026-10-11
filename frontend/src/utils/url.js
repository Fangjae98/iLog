// 사용자가 입력한 URL을 링크로 걸기 전에 검사한다.
// Vue는 {{ }} 출력은 이스케이프하지만 :href 의 javascript: 는 막지 않는다.
export const isSafeUrl = (value) => {
  try {
    return ['http:', 'https:'].includes(new URL(value).protocol)
  } catch {
    return false
  }
}
