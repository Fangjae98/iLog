<template>
  <div class="url-input">
    <div class="row">
      <input
        :id="inputId"
        v-model.trim="draft"
        class="box"
        inputmode="url"
        placeholder="https://"
        @keydown.enter="onEnter"
      />
      <button type="button" class="btn" @click="add">추가</button>
    </div>
    <FieldError :message="error" />
    <ul v-if="urls.length" class="url-list">
      <li v-for="url in urls" :key="url">
        <a v-if="isSafeUrl(url)" :href="url" target="_blank" rel="noopener noreferrer">{{ url }}</a>
        <span v-else>{{ url }}</span>
        <button type="button" class="btn btn-sm" @click="remove(url)">삭제</button>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import FieldError from '@/components/common/FieldError.vue'
import { RULES } from '@/constants/rules'
import { isSafeUrl } from '@/utils/url'

defineProps({ inputId: { type: String, default: 'url-input' } })
const urls = defineModel({ type: Array, default: () => [] })
const draft = ref('')
const error = ref('')

function add() {
  error.value = ''
  if (!draft.value) return
  const value = /^https?:\/\//i.test(draft.value) ? draft.value : `https://${draft.value}`

  if (!isSafeUrl(value)) return (error.value = '* http 또는 https 주소만 넣을 수 있어요.')
  if (urls.value.includes(value)) return (error.value = '* 중복된 값입니다.')
  if (RULES.URL_MAX_COUNT && urls.value.length >= RULES.URL_MAX_COUNT) {
    return (error.value = `* URL은 ${RULES.URL_MAX_COUNT}개까지 넣을 수 있어요.`)
  }
  urls.value = [...urls.value, value]
  draft.value = ''
}

function onEnter(e) {
  if (e.isComposing) return
  e.preventDefault()
  add()
}

function remove(url) {
  urls.value = urls.value.filter((u) => u !== url)
}
</script>

<style scoped>
.url-list { display: grid; gap: 8px; margin-top: 10px; }
.url-list li { display: flex; align-items: center; gap: 12px; }
.url-list a, .url-list span { font-size: var(--fs-sm); color: var(--hashtag); word-break: break-all; }
</style>
