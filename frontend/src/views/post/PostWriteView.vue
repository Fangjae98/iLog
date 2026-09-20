<template>
  <!-- @metric FALLBACK: 실패 시 fieldErrors 를 PostForm 각 칸 아래에 그린다 -->
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
import { postApi } from '@/api/post'
import { formatDotDate } from '@/utils/date'

const router = useRouter()
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
  } finally {
    submitting.value = false
  }
}
</script>
