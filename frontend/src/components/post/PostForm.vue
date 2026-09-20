<template>
  <form class="post-form" novalidate @submit.prevent="onSubmit">
    <div class="form-head">
      <h1><span class="num">{{ headingDate }}</span> 의 기록</h1>
      <div v-if="mode === 'edit'" class="btn-group">
        <!-- @metric INP, REQUEST: 요청 중에는 버튼을 막아 연타로 요청이 중복되지 않게 한다 -->
        <button type="submit" class="btn btn-primary" :disabled="submitting">수정 완료</button>
        <button type="button" class="btn" @click="emit('cancel')">취소</button>
      </div>
    </div>

    <div class="field">
      <!-- @metric CLS: 에러 문구를 라벨 옆에 둬서 문구가 생겨도 입력칸이 밀리지 않는다 -->
      <div class="label-row">
        <label class="label-lg" for="post-title">제목</label>
        <FieldError :message="titleError || fieldErrors.title" />
      </div>
      <input id="post-title" v-model="form.title" class="box" :maxlength="RULES.TITLE_MAX" />
    </div>

    <div class="field">
      <div class="label-row">
        <label class="label-lg" for="post-content">내용</label>
        <FieldError :message="fieldErrors.content" />
      </div>
      <!-- 내용 필수 여부는 U-1 미결. 프론트에서 막지 않고 서버 INVALID_INPUT 을 표시한다 -->
      <textarea id="post-content" v-model="form.content" class="box" rows="6" />
    </div>

    <div class="field">
      <label class="label-lg" for="post-url">URL (하이퍼링크)</label>
      <UrlInput v-model="form.urls" input-id="post-url" />
      <FieldError :message="fieldErrors.urls" />
    </div>

    <div class="field">
      <div class="label-row">
        <label class="label-lg" for="post-hashtag">#해시태그</label>
        <span class="helper">공부 주제, 스터디 이름 등을 입력해보세요</span>
      </div>
      <HashtagInput v-model="form.hashtags" input-id="post-hashtag" />
      <FieldError :message="fieldErrors.hashtags" />
    </div>

    <div v-if="mode === 'create'" class="form-foot">
      <button type="submit" class="btn btn-primary" :disabled="submitting">글 게시하기</button>
    </div>
  </form>
</template>

<script setup>
// @metric MAINT: 작성·수정이 같은 폼을 쓴다. 같은 폼을 두 번 만들지 않는다.
import { reactive, ref, watch } from 'vue'
import FieldError from '@/components/common/FieldError.vue'
import HashtagInput from './HashtagInput.vue'
import UrlInput from './UrlInput.vue'
import { RULES } from '@/constants/rules'

const props = defineProps({
  mode: { type: String, default: 'create' }, // 'create' | 'edit'
  headingDate: { type: String, required: true }, // formatDotDate 결과
  initial: {
    type: Object,
    default: () => ({ title: '', content: '', urls: [], hashtags: [] }),
  },
  submitting: { type: Boolean, default: false },
  fieldErrors: { type: Object, default: () => ({}) },
})
const emit = defineEmits(['submit', 'cancel'])

// props 를 직접 고치지 않고 로컬 복사본을 편집한다
const form = reactive({ title: '', content: '', urls: [], hashtags: [] })
const titleError = ref('')

watch(
  () => props.initial,
  (v) => {
    form.title = v.title
    form.content = v.content
    form.urls = [...v.urls]
    form.hashtags = [...v.hashtags]
  },
  { immediate: true },
)

function onSubmit() {
  titleError.value = ''
  if (!form.title.trim()) {
    titleError.value = '* 제목을 입력해주세요'
    return
  }
  emit('submit', {
    title: form.title.trim(),
    content: form.content,
    urls: form.urls,
    hashtags: form.hashtags,
  })
}
</script>

<style scoped>
.post-form { max-width: 920px; margin: 32px auto 0; }
.form-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; flex-wrap: wrap; margin-bottom: 24px; }
.form-head h1 { font-family: var(--font-hand); font-weight: 700; font-size: var(--fs-2xl); line-height: 35px; }
.form-head .num { margin-right: 4px; font-size: 0.7em; font-weight: 500; }
.form-foot { display: flex; justify-content: center; margin-top: 40px; }
</style>
