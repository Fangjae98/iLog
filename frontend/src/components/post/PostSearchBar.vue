<template>
  <!-- @metric INP: 입력마다가 아니라 제출할 때만 검색한다. 타이핑 중에 요청·렌더링이 돌지 않는다 -->
  <form class="search" role="search" @submit.prevent="onSubmit">
    <label class="sr-only" for="search-text">검색어</label>
    <input id="search-text" v-model="text" class="box search-text" placeholder="검색어 입력" />
    <select v-model="field" class="box" aria-label="검색 범위">
      <option value="keyword">제목+내용</option>
      <option value="hashtag">해시태그</option>
      <option value="nickname">닉네임</option>
    </select>
    <input v-model="date" class="box" type="date" aria-label="날짜 선택" />
    <button type="submit" class="btn btn-primary" aria-label="검색">🔍</button>
    <button v-if="active" type="button" class="btn" @click="emit('reset')">초기화</button>
    <button type="button" class="btn" @click="emit('mine')">내 글 보기</button>
  </form>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

// 검색어를 어느 파라미터(keyword / hashtag / nickname)로 보낼지 드롭다운으로 고른다.
// 한 번에 하나만 보낸다. 여러 조건 동시 검색은 D-01 확정 후.
const props = defineProps({ initial: { type: Object, default: () => ({}) } }) // 현재 URL 쿼리
const emit = defineEmits(['search', 'reset', 'mine'])

const field = ref('keyword')
const text = ref('')
const date = ref('')

watch(
  () => props.initial,
  (q) => {
    if (q.hashtag) {
      field.value = 'hashtag'
      text.value = [].concat(q.hashtag).join(', ')
    } else if (q.nickname) {
      field.value = 'nickname'
      text.value = q.nickname
    } else {
      field.value = 'keyword'
      text.value = q.keyword ?? ''
    }
    date.value = q.date ?? ''
  },
  { immediate: true },
)

const active = computed(() => ['keyword', 'hashtag', 'nickname', 'date'].some((k) => props.initial[k]))

function onSubmit() {
  const value = text.value.trim()
  const filters = {}
  if (value) {
    filters[field.value] =
      field.value === 'hashtag'
        ? value.split(',').map((s) => s.trim().replace(/^#+/, '')).filter(Boolean)
        : value
  }
  if (date.value) filters.date = date.value
  emit('search', filters)
}
</script>

<style scoped>
.search { display: flex; align-items: flex-end; gap: 10px; flex-wrap: wrap; margin-bottom: 20px; }
.search-text { flex: 1 1 180px; }
.search select, .search input[type='date'] { width: auto; }
</style>
