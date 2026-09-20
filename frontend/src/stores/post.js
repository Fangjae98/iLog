import { defineStore } from 'pinia'
import { postApi } from '@/api/post'

export const usePostStore = defineStore('post', {
  state: () => ({
    items: [],
    pageInfo: { page: 0, size: 10, totalElements: 0, totalPages: 0, hasNext: false },
    loading: false,
    error: null,
  }),

  actions: {
    async fetchList(params) {
      this.loading = true
      this.error = null
      try {
        const data = await postApi.list(params)
        this.items = data.content
        const { page, size, totalElements, totalPages, hasNext } = data
        this.pageInfo = { page, size, totalElements, totalPages, hasNext }
      } catch (e) {
        this.items = []
        this.error = e.message
      } finally {
        this.loading = false // 실패해도 로딩은 반드시 내린다
      }
    },
  },
})
