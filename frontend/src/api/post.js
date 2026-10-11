// 게시글 관련 API를 한 곳에 모으고, 환경 설정에 따라 mock 또는 실제 백엔드를 호출함
import client from './client'
import { MOCK_ENABLED, mockPost } from './mock'

// VITE_USE_MOCK=true이면 mockPost를, false이면 client를 통해 실제 백엔드를 사용
export const postApi = {
  // 목록·검색·내 글 보기. 조건을 여러 개 보내면 모두 만족하는 글만 온다
  list: (params) =>
    MOCK_ENABLED
      ? mockPost.list(params)
      : client.get('/posts', { params }),
  // params: { page(0부터), size, keyword, nickname, hashtag: [], date, author: 'me' }

  // 게시글 한 건을 조회한다
  get: (postId) =>
    MOCK_ENABLED
      ? mockPost.get(postId)
      : client.get(`/posts/${postId}`),

  // 게시글을 작성한다
  create: (body) =>
    MOCK_ENABLED
      ? mockPost.create(body)
      : client.post('/posts', body),
  // body: { title, content, urls, hashtags }, 성공: 201 { postId }

  // 보낸 필드만 수정한다. urls와 hashtags는 보낸 배열로 통째 교체된다
  update: (postId, body) =>
    MOCK_ENABLED
      ? mockPost.update(postId, body)
      : client.patch(`/posts/${postId}`, body),

  // 게시글을 삭제한다
  remove: (postId) =>
    MOCK_ENABLED
      ? mockPost.remove(postId)
      : client.delete(`/posts/${postId}`),
  // 성공: 204
}
