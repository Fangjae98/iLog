// 백엔드 게시글 담당 영역
import client from './client'
import { MOCK_ENABLED, mockPost } from './mock' // MOCK: 백엔드 머지 후 이 줄을 지운다

export const postApi = {
  list: (params) =>
    MOCK_ENABLED ? mockPost.list(params) : client.get('/posts', { params }),
  // { page, size, sort, keyword, hashtag: [], nickname, date }
  get: (postId) =>
    MOCK_ENABLED ? mockPost.get(postId) : client.get(`/posts/${postId}`),
  create: (body) =>
    MOCK_ENABLED ? mockPost.create(body) : client.post('/posts', body), // { title, content, urls, hashtags }
  update: (postId, body) =>
    MOCK_ENABLED ? mockPost.update(postId, body) : client.patch(`/posts/${postId}`, body), // 보낸 필드만 수정
  remove: (postId) =>
    MOCK_ENABLED ? mockPost.remove(postId) : client.delete(`/posts/${postId}`),
  addHashtags: (postId, names) => client.post(`/posts/${postId}/hashtags`, { names }),
  removeHashtag: (postId, hashtagId) => client.delete(`/posts/${postId}/hashtags/${hashtagId}`),
}
