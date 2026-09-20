<template>
  <!-- @metric FALLBACK: 불러오는 중 / 실패 / 정상 -->
  <p v-if="loading" class="state">불러오는 중이에요…</p>
  <p v-else-if="loadError" class="state">{{ loadError }}</p>
  <PostForm
    v-else-if="initial"
    mode="edit"
    :heading-date="formatDotDate(createdAt)"
    :initial="initial"
    :submitting="submitting"
    :field-errors="fieldErrors"
    @submit="onSubmit"
    @cancel="router.push({ name: 'post-detail', params: { postId } })"
  />
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import PostForm from '@/components/post/PostForm.vue'
import { useDialog } from '@/composables/useDialog'
import { postApi } from '@/api/post'
import { formatDotDate } from '@/utils/date'

const props = defineProps({ postId: { type: String, required: true } })
const router = useRouter()
const dialog = useDialog()

const initial = ref(null)
const createdAt = ref(null)
const loading = ref(false)
const loadError = ref('')
const submitting = ref(false)
const fieldErrors = ref({})

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const post = await postApi.get(props.postId)
    // 본인 글이 아니면 상세로 되돌린다
    if (!post.isMine) {
      await dialog.alert({ title: '본인이 작성한 글만 수정할 수 있어요.' })
      return router.replace({ name: 'post-detail', params: { postId: props.postId } })
    }
    createdAt.value = post.createdAt
    initial.value = { title: post.title, content: post.content, urls: post.urls, hashtags: post.hashtags }
  } catch (e) {
    if (e.code === 'POST_NOT_FOUND') {
      await dialog.alert({ title: e.message })
      router.replace({ name: 'post-list' })
    } else {
      loadError.value = e.message
    }
  } finally {
    loading.value = false
  }
}

watch(() => props.postId, load, { immediate: true })

// 응답 명세 기준으로 hashtags 까지 포함해 통째로 교체한다 (D-06)
async function onSubmit(body) {
  submitting.value = true
  fieldErrors.value = {}
  try {
    await postApi.update(props.postId, body)
    router.replace({ name: 'post-detail', params: { postId: props.postId } })
  } catch (e) {
    fieldErrors.value = e.fieldErrors ?? {}
    if (e.code === 'POST_NOT_OWNED') {
      await dialog.alert({ title: e.message })
      router.replace({ name: 'post-detail', params: { postId: props.postId } })
    } else if (e.code === 'POST_NOT_FOUND') {
      await dialog.alert({ title: e.message })
      router.replace({ name: 'post-list' })
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.state { margin-top: 48px; text-align: center; color: var(--muted); }
</style>
