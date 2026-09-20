// MOCK: 백엔드 API가 머지되면 이 파일을 통째로 지우고 .env 의 VITE_USE_MOCK 도 없앤다.
// 구현 지침 10장. 반환 모양은 인터셉터를 거친 뒤(봉투를 벗긴 data)와 똑같이 맞춘다.
import { resolveMessage } from '@/constants/errorMessages'

export const MOCK_ENABLED = import.meta.env.VITE_USE_MOCK === 'true'

// client.js 인터셉터가 만드는 에러 객체와 같은 모양으로 던진다
const fail = (status, code, fieldErrors = {}) =>
  Promise.reject({ status, code, message: resolveMessage(code), fieldErrors })

const delay = (ms = 220) => new Promise((r) => setTimeout(r, ms))
const iso = (d) => new Date(d).toISOString()
const daysAgo = (n) => iso(Date.now() - n * 86400000)
const minsAgo = (n) => iso(Date.now() - n * 60000)

const db = {
  users: [
    { userId: 1, email: 'test@gmail.com', password: 'test1234!', name: '김일일', nickname: '쇠똥구리', status: 'ACTIVE', passwordResetRequired: false, createdAt: daysAgo(120) },
    { userId: 2, email: 'temp@gmail.com', password: 'temp1234', name: '이임시', nickname: '뷰정복러', status: 'ACTIVE', passwordResetRequired: true, createdAt: daysAgo(40) },
    { userId: 3, email: 'bye@gmail.com', password: 'bye12345', name: '박탈퇴', nickname: '잠깐쉬는중', status: 'WITHDRAWN', passwordResetRequired: false, createdAt: daysAgo(200) },
    { userId: 4, email: 'fairy@naver.com', password: 'fairy1234', name: '최로그', nickname: '로그요정', status: 'ACTIVE', passwordResetRequired: false, createdAt: daysAgo(15) },
  ],
  posts: [
    { postId: 1, userId: 4, title: 'JPA N+1 문제 정리', content: '연관관계를 LAZY로 두고 목록을 조회하면 연관 엔티티마다 쿼리가 한 번씩 더 나간다.\nfetch join과 @EntityGraph를 비교해봤다.', urls: ['https://velog.io/'], hashtags: ['JPA', '오늘의로그'], createdAt: daysAgo(3), updatedAt: null },
    { postId: 2, userId: 1, title: '인덱스와 카디널리티', content: '카디널리티가 낮은 컬럼에 인덱스를 걸면 옵티마이저가 풀스캔을 고른다.', urls: [], hashtags: ['DB'], createdAt: daysAgo(2), updatedAt: null },
    { postId: 3, userId: 2, title: '이번 주 배운 것', content: '', urls: [], hashtags: ['회고'], createdAt: daysAgo(1), updatedAt: null },
    { postId: 4, userId: 1, title: 'Vue 3 상태 관리 정리', content: 'Pinia 스토어를 storeToRefs 로 꺼내야 반응형이 끊기지 않는다.', urls: ['https://vuejs.org/', 'https://pinia.vuejs.org/'], hashtags: ['오늘의로그', 'Vue', '뷰_정복기'], createdAt: minsAgo(190), updatedAt: minsAgo(40) },
    { postId: 5, userId: 4, title: '오늘의 회고', content: '트랜잭션 전파 속성 REQUIRED와 REQUIRES_NEW 차이를 테스트 코드로 확인했다.', urls: [], hashtags: ['오늘의로그'], createdAt: minsAgo(35), updatedAt: null },
  ],
  seq: 6,
  currentUserId: null,
}

const userById = (id) => db.users.find((u) => u.userId === id)
const userByEmail = (email) => db.users.find((u) => u.email === email)
const me = () => userById(db.currentUserId)
const toListItem = (p) => ({
  postId: p.postId, title: p.title, nickname: userById(p.userId).nickname,
  hashtags: [...p.hashtags], createdAt: p.createdAt,
})
const toDetail = (p) => ({
  postId: p.postId, title: p.title, content: p.content,
  urls: [...p.urls], hashtags: [...p.hashtags],
  author: { userId: p.userId, nickname: userById(p.userId).nickname },
  createdAt: p.createdAt, updatedAt: p.updatedAt,
  isMine: p.userId === db.currentUserId,
})
const requireLogin = () => (me() ? null : fail(401, 'UNAUTHORIZED'))
const maskEmail = (email) => {
  const [local, domain] = email.split('@')
  return `${local.slice(0, 1)}***@${domain}`
}

export const mockAuth = {
  async login({ email, password }) {
    await delay()
    const u = userByEmail(email)
    if (!u || u.password !== password) return fail(401, 'LOGIN_FAILED')
    if (u.status === 'WITHDRAWN') return fail(401, 'ACCOUNT_WITHDRAWN')
    db.currentUserId = u.userId
    return {
      accessToken: `mock-token-${u.userId}-${Date.now()}`,
      expiresIn: 7200,
      user: { userId: u.userId, nickname: u.nickname },
      passwordResetRequired: u.passwordResetRequired,
    }
  },
  async logout() {
    await delay(80)
    db.currentUserId = null
    return null // 204
  },
  async issueTempPassword({ email, name }) {
    await delay()
    const u = userByEmail(email)
    if (!u || u.name !== name || u.status === 'WITHDRAWN') return fail(400, 'MEMBER_NOT_MATCHED')
    u.passwordResetRequired = true
    return { email: maskEmail(u.email) } // 임시 비밀번호는 응답에 없다
  },
  async verifyPassword({ password }) {
    await delay()
    const guard = requireLogin(); if (guard) return guard
    const u = me()
    if (u.password !== password) return fail(401, 'PASSWORD_MISMATCH', { password: '비밀번호가 일치하지 않습니다' })
    return { userId: u.userId, email: u.email, name: u.name, nickname: u.nickname, createdAt: u.createdAt }
  },
  async changePassword({ currentPassword, newPassword, newPasswordConfirm }) {
    await delay()
    const guard = requireLogin(); if (guard) return guard
    const u = me()
    if (u.password !== currentPassword) return fail(401, 'PASSWORD_MISMATCH', { currentPassword: '비밀번호가 일치하지 않습니다' })
    if (newPassword !== newPasswordConfirm) return fail(400, 'PASSWORD_CONFIRM_MISMATCH', { newPasswordConfirm: '비밀번호 확인이 일치하지 않습니다' })
    if (newPassword === currentPassword) return fail(400, 'PASSWORD_RECENTLY_USED', { newPassword: '최근에 사용한 비밀번호입니다' })
    u.password = newPassword
    u.passwordResetRequired = false
    return null // 204
  },
}

export const mockUser = {
  async signup({ email, password, passwordConfirm, name, nickname }) {
    await delay()
    if (password !== passwordConfirm) return fail(400, 'PASSWORD_CONFIRM_MISMATCH', { passwordConfirm: '비밀번호 확인이 일치하지 않습니다' })
    if (userByEmail(email)) return fail(400, 'EMAIL_DUPLICATED', { email: '이미 사용 중입니다' })
    if (db.users.some((u) => u.nickname === nickname)) return fail(400, 'NICKNAME_DUPLICATED', { nickname: '이미 사용 중입니다' })
    const userId = db.users.length + 1
    db.users.push({ userId, email, password, name, nickname, status: 'ACTIVE', passwordResetRequired: false, createdAt: iso(Date.now()) })
    return { userId }
  },
  async checkEmail(email) {
    await delay(150)
    const u = userByEmail(email)
    if (!u) return { available: true, reason: null }
    return { available: false, reason: u.status === 'WITHDRAWN' ? 'WITHDRAWN' : 'IN_USE' }
  },
  async checkNickname(nickname) {
    await delay(150)
    return { available: !db.users.some((u) => u.nickname === nickname) }
  },
  async updateNickname(nickname) {
    await delay()
    const guard = requireLogin(); if (guard) return guard
    const u = me()
    if (nickname === u.nickname) return fail(400, 'NICKNAME_SAME_AS_CURRENT', { nickname: '지금 쓰고 있는 닉네임입니다' })
    if (db.users.some((x) => x.nickname === nickname)) return fail(400, 'NICKNAME_DUPLICATED', { nickname: '이미 사용 중입니다' })
    u.nickname = nickname
    return { userId: u.userId, nickname }
  },
  async withdraw(password) {
    await delay()
    const guard = requireLogin(); if (guard) return guard
    const u = me()
    if (u.password !== password) return fail(401, 'PASSWORD_MISMATCH', { password: '비밀번호가 일치하지 않습니다' })
    u.status = 'WITHDRAWN'
    db.currentUserId = null
    return { recoverableUntil: iso(Date.now() + 30 * 86400000) }
  },
}

export const mockPost = {
  async list(params = {}) {
    await delay()
    const guard = requireLogin(); if (guard) return guard
    const { page = 0, size = 10, keyword, nickname, date } = params
    const hashtags = [].concat(params.hashtag ?? [])

    let rows = db.posts.filter((p) => {
      if (keyword) {
        const k = keyword.toLowerCase()
        if (!p.title.toLowerCase().includes(k) && !p.content.toLowerCase().includes(k)) return false
      }
      if (nickname && userById(p.userId).nickname !== nickname) return false
      if (date && p.createdAt.slice(0, 10) !== date) return false
      if (hashtags.length && !hashtags.every((t) => p.hashtags.includes(t))) return false
      return true
    })
    rows = rows.sort((a, b) => b.createdAt.localeCompare(a.createdAt))

    const totalElements = rows.length
    const totalPages = Math.ceil(totalElements / size)
    return {
      content: rows.slice(page * size, page * size + size).map(toListItem),
      page, size, totalElements, totalPages,
      hasNext: page + 1 < totalPages,
    }
  },
  async get(postId) {
    await delay()
    const guard = requireLogin(); if (guard) return guard
    const p = db.posts.find((x) => x.postId === Number(postId))
    if (!p) return fail(404, 'POST_NOT_FOUND')
    return toDetail(p)
  },
  async create({ title, content, urls, hashtags }) {
    await delay()
    const guard = requireLogin(); if (guard) return guard
    if (!title?.trim()) return fail(400, 'INVALID_INPUT', { title: '제목은 필수입니다' })
    const postId = db.seq++
    db.posts.push({
      postId, userId: db.currentUserId, title: title.trim(), content: content ?? '',
      urls: [...(urls ?? [])], hashtags: [...(hashtags ?? [])],
      createdAt: iso(Date.now()), updatedAt: null,
    })
    return { postId }
  },
  async update(postId, body) {
    await delay()
    const guard = requireLogin(); if (guard) return guard
    const p = db.posts.find((x) => x.postId === Number(postId))
    if (!p) return fail(404, 'POST_NOT_FOUND')
    if (p.userId !== db.currentUserId) return fail(403, 'POST_NOT_OWNED')
    // 보낸 필드만 수정. 해시태그는 통째로 교체 (D-06)
    for (const key of ['title', 'content', 'urls', 'hashtags']) {
      if (body[key] !== undefined) p[key] = body[key]
    }
    p.updatedAt = iso(Date.now())
    return toDetail(p)
  },
  async remove(postId) {
    await delay()
    const guard = requireLogin(); if (guard) return guard
    const p = db.posts.find((x) => x.postId === Number(postId))
    if (!p) return fail(404, 'POST_NOT_FOUND')
    if (p.userId !== db.currentUserId) return fail(403, 'POST_NOT_OWNED')
    db.posts = db.posts.filter((x) => x.postId !== p.postId)
    return null // 204
  },
}

// 새로고침해도 로그인 상태가 유지되도록, 저장된 세션의 사용자를 되살린다
export const mockRestoreSession = (userId) => {
  if (userById(userId)) db.currentUserId = userId
}
