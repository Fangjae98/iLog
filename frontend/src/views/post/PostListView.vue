<template>
  <section class="post-list">
    <div class="list-head">
      <h1><span class="num">{{ today }}</span> 오늘의 기록</h1>
      <RouterLink class="btn btn-primary" :to="{ name: 'post-write' }">+ 글쓰기</RouterLink>
    </div>

    <PostSearchBar :initial="route.query" @search="onSearch" @reset="onReset" @mine="showMyPosts" />

    <div class="table-wrap">
      <table class="post-table">
        <!-- @metric FCP, CLS: 로딩 중에도 머리글을 먼저 그려 결과가 들어올 때 위쪽 배치가 그대로다 -->
        <!-- @metric A11Y: scope="col" 로 머리글과 칸의 관계를 스크린리더에 알린다 -->
        <thead>
          <tr>
            <th scope="col" class="c-no">인덱스</th>
            <th scope="col">제목</th>
            <th scope="col" class="c-nick">닉네임</th>
            <th scope="col" class="c-time">작성 시간</th>
          </tr>
        </thead>
        <tbody>
          <!-- @metric FALLBACK: 로딩 / 에러(다시 시도) / 빈 결과를 모두 그린다 -->
          <tr v-if="loading" class="state-row"><td colspan="4">불러오는 중이에요…</td></tr>
          <tr v-else-if="error" class="state-row">
            <td colspan="4">{{ error }} <button type="button" class="btn btn-sm" @click="reload">다시 시도</button></td>
          </tr>
          <tr v-else-if="!items.length" class="state-row"><td colspan="4">조건에 맞는 로그가 없어요</td></tr>
          <template v-else>
            <PostListItem v-for="post in items" :key="post.postId" :post="post" @tag="searchTag" />
          </template>
        </tbody>
      </table>
    </div>

    <BasePagination :page="currentPage" :total-pages="pageInfo.totalPages" @change="onPageChange" />
  </section>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import BasePagination from '@/components/common/BasePagination.vue'
import PostSearchBar from '@/components/post/PostSearchBar.vue'
import PostListItem from '@/components/post/PostListItem.vue'
import { usePostStore } from '@/stores/post'
import { useAuthStore } from '@/stores/auth'
import { formatDotDate } from '@/utils/date'

const route = useRoute()
const router = useRouter()
const postStore = usePostStore()
const { items, pageInfo, loading, error } = storeToRefs(postStore)
const auth = useAuthStore()

// @metric TBT, INP: 한 번에 그리는 행 수를 제한한다
const PAGE_SIZE = 10
const today = formatDotDate(Date.now())

// URL 쿼리(화면 기준 1부터) → API 파라미터(서버 기준 0부터). 변환은 여기 한 곳에서만.
function toApiParams(q) {
  const params = {
    page: Math.max(Number(q.page) || 1, 1) - 1,
    size: PAGE_SIZE,
    sort: 'createdAt,desc', // MVP 는 정렬 선택 없음
    keyword: q.keyword,
    hashtag: [].concat(q.hashtag ?? []), // 1개면 문자열, 여러 개면 배열로 옴
    nickname: q.nickname,
    date: q.date,
  }
  return Object.fromEntries(
    Object.entries(params).filter(([, v]) => v !== undefined && v !== '' && !(Array.isArray(v) && !v.length)),
  )
}

// @metric REQUEST: watch immediate 가 onMounted 역할까지 한다.
// 여기에 onMounted 를 더하면 첫 진입 때 요청이 두 번 나간다.
watch(() => route.query, (q) => postStore.fetchList(toApiParams(q)), { immediate: true })

const currentPage = computed(() => pageInfo.value.page + 1)

function onSearch(filters) {
  router.push({ query: { ...filters, page: 1 } }) // 조건이 바뀌면 1페이지부터
}
function onReset() {
  router.push({ query: { page: 1 } })
}
function onPageChange(page) {
  router.push({ query: { ...route.query, page } })
}
function searchTag(tag) {
  router.push({ query: { hashtag: tag, page: 1 } })
}
function showMyPosts() {
  router.push({ query: { nickname: auth.user.nickname, page: 1 } })
}
function reload() {
  postStore.fetchList(toApiParams(route.query))
}
</script>

<style scoped>
.list-head { display: flex; justify-content: space-between; align-items: flex-end; gap: 12px; flex-wrap: wrap; margin: 40px 0 32px; }
.list-head h1 { font-family: var(--font-hand); font-weight: 700; font-size: var(--fs-3xl); line-height: 45px; }
.list-head .num { margin-right: 4px; font-size: 0.7em; font-weight: 500; }
.table-wrap { overflow-x: auto; }
.post-table {
  width: 100%; border-collapse: separate; border-spacing: 0; overflow: hidden;
  background: var(--paper); border: 1px solid var(--line-soft); border-radius: 6px;
}
.post-table th {
  padding: 12px 14px; background: var(--head);
  font-size: var(--fs-2xs); font-weight: 400; color: var(--label); text-align: left; white-space: nowrap;
}
.post-table th.c-no { width: 64px; }
.post-table th.c-nick { width: 110px; }
.post-table th.c-time { width: 84px; }
.state-row td { padding: 32px 14px; border-top: 1px solid var(--line-soft); text-align: center; font-size: 13px; color: var(--muted); }
@media (max-width: 560px) { .post-table th.c-no { display: none; } }
</style>
