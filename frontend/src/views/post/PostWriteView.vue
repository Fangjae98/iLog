<template>
  <!-- @metric FALLBACK: 실패 시 fieldErrors 를 PostForm 각 칸 아래에 그리고, 칸에 못 붙는 에러는 팝업으로 알린다 -->
  <PostForm
    mode="create"
    :heading-date="today"
    :submitting="submitting"
    :field-errors="fieldErrors"
    @submit="onSubmit"
  />
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import PostForm from '@/components/post/PostForm.vue'
import { useDialog } from '@/composables/useDialog'
import { postApi } from '@/api/post'
import { formatDotDate } from '@/utils/date'

const router = useRouter()
const dialog = useDialog()
const today = formatDotDate(Date.now())
const submitting = ref(false)
const fieldErrors = ref({})

// 작성자·작성일은 요청에 넣지 않는다. 서버가 토큰과 현재 시각으로 정한다.
async function onSubmit(body) {
  submitting.value = true
  fieldErrors.value = {}
  try {
    const data = await postApi.create(body)
    // 뒤로 가기로 빈 작성 폼이 다시 뜨지 않게 replace
    router.replace({ name: 'post-detail', params: { postId: data.postId } })
  } catch (e) {
    fieldErrors.value = e.fieldErrors ?? {}
    // 해시태그 개수 초과처럼 어느 칸인지 서버가 알려 주지 않는 에러
    if (!Object.keys(fieldErrors.value).length) await dialog.alert({ title: e.message })
  } finally {
    submitting.value = false
  }
}
</script>
