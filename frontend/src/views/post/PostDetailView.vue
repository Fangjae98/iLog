<template>
  <!-- @metric FALLBACK: 로딩 / 에러 / 정상 세 상태를 모두 그린다 -->
  <p v-if="loading" class="state">불러오는 중이에요…</p>
  <p v-else-if="loadError" class="state">{{ loadError }}</p>

  <article v-else-if="post" class="post-detail">
    <div class="detail-head">
      <h1>
        <span class="num">{{ formatDotDate(post.createdAt) }}</span>
        {{ post.isMine ? '의 기록' : `${post.author.nickname}의 기록` }}
      </h1>
      <div class="side">
        <p class="meta">
          작성 일자 <span class="num">{{ formatDotDate(post.createdAt) }} {{ formatClock(post.createdAt) }}</span>
          <template v-if="post.updatedAt"> (업데이트 됨)</template>
        </p>
        <!-- 수정·삭제는 isMine 으로만 판단한다. 닉네임 비교로 판단하지 않는다. -->
        <div v-if="post.isMine" class="btn-group">
          <RouterLink class="btn" :to="{ name: 'post-edit', params: { postId } }">수정하기</RouterLink>
          <button type="button" class="btn" :disabled="deleting" @click="onDelete">글 삭제</button>
        </div>
      </div>
    </div>

    <div class="field">
      <p class="label-lg">제목</p>
      <p class="readonly">{{ post.title }}</p>
    </div>
    <div class="field">
      <p class="label-lg">내용</p>
      <!-- 본문은 평문이므로 {{ }} + white-space: pre-wrap. v-html 금지 -->
      <div class="content" :class="{ 'is-empty': !post.content }">{{ post.content || '작성된 내용이 없습니다.' }}</div>
    </div>
    <div class="field">
      <p class="label-lg">URL (하이퍼링크)</p>
      <ul v-if="safeUrls.length" class="url-list">
        <li v-for="url in safeUrls" :key="url">
          <a :href="url" target="_blank" rel="noopener noreferrer">{{ url }}</a>
        </li>
      </ul>
      <p v-else class="msg msg-hint">등록된 URL이 없습니다.</p>
    </div>
    <div class="field">
      <p class="label-lg">#해시태그</p>
      <ul v-if="post.hashtags.length" class="chips">
        <li v-for="tag in post.hashtags" :key="tag">
          <button type="button" class="chip" @click="goTag(tag)"># {{ tag }}</button>
        </li>
      </ul>
      <p v-else class="msg msg-hint">등록된 해시태그가 없습니다.</p>
    </div>

    <div class="foot">
      <RouterLink class="btn" :to="{ name: 'post-list' }">이전</RouterLink>
    </div>
  </article>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useDialog } from '@/composables/useDialog'
import { postApi } from '@/api/post'
import { formatClock, formatDotDate } from '@/utils/date'
import { isSafeUrl } from '@/utils/url'

const props = defineProps({ postId: { type: String, required: true } })
const router = useRouter()
const dialog = useDialog()

const post = ref(null)
const loading = ref(false)
const loadError = ref('')
const deleting = ref(false)

// isSafeUrl 을 통과한 것만 <a> 로 그린다
const safeUrls = computed(() => (post.value?.urls ?? []).filter(isSafeUrl))

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    post.value = await postApi.get(props.postId)
  } catch (e) {
    if (e.code === 'POST_NOT_FOUND') {
      await dialog.alert({ title: e.message })
      // 뒤로 가기로 다시 404 가 뜨지 않게 replace
      router.replace({ name: 'post-list' })
    } else {
      loadError.value = e.message
    }
  } finally {
    loading.value = false
  }
}

// 상세에서 다른 상세로 이동해도 다시 불러온다 (같은 컴포넌트가 재사용되어 onMounted 가 안 돎)
watch(() => props.postId, load, { immediate: true })

function goTag(tag) {
  router.push({ name: 'post-list', query: { hashtag: tag, page: 1 } })
}

async function onDelete() {
  const ok = await dialog.confirm({ title: '게시글을 정말로 삭제하시겠습니까?', confirmText: '글 삭제' })
  if (!ok) return
  deleting.value = true
  try {
    await postApi.remove(props.postId)
    await dialog.alert({ title: '게시글이 삭제되었습니다.' })
    router.replace({ name: 'post-list' })
  } catch (e) {
    await dialog.alert({ title: e.message })
    if (e.code === 'POST_NOT_FOUND') router.replace({ name: 'post-list' })
  } finally {
    deleting.value = false
  }
}
</script>

<style scoped>
.state { margin-top: 48px; text-align: center; color: var(--muted); }
.post-detail { max-width: 920px; margin: 32px auto 0; }
.detail-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; flex-wrap: wrap; margin-bottom: 24px; }
.detail-head h1 { font-family: var(--font-hand); font-weight: 700; font-size: var(--fs-2xl); line-height: 35px; }
.detail-head h1 .num { margin-right: 4px; font-size: 0.7em; font-weight: 500; }
.side { display: flex; flex-direction: column; align-items: flex-end; gap: 10px; }
.meta { font-size: var(--fs-xs); color: var(--muted); }
/* @metric CLS: min-height 로 본문이 짧거나 늦게 와도 아래 칸 위치가 크게 안 바뀐다 */
.content {
  min-height: 72px; padding: 12px 14px;
  border: 1px solid var(--line-soft); border-radius: var(--radius);
  font-size: var(--fs-md); line-height: 1.8; color: var(--text-2);
  white-space: pre-wrap; word-break: break-word;
}
.content.is-empty { font-size: var(--fs-sm); color: var(--muted); }
.url-list { display: grid; gap: 8px; }
.url-list a { font-size: var(--fs-sm); color: var(--hashtag); word-break: break-all; }
.foot { display: flex; justify-content: center; margin-top: 40px; }
.foot .btn { min-width: 110px; }
</style>
