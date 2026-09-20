<template>
  <div class="hashtag-input">
    <div class="row">
      <input
        :id="inputId"
        v-model="draft"
        class="box"
        :maxlength="RULES.HASHTAG_MAX_LENGTH ?? undefined"
        placeholder="Enter 또는 쉼표로 여러 개 입력"
        @keydown.enter="onEnter"
      />
      <button type="button" class="btn" @click="add">추가</button>
    </div>
    <FieldError :message="error" />
    <ul v-if="tags.length" class="chips tag-list">
      <li v-for="tag in tags" :key="tag" class="chip">
        # {{ tag }}
        <button type="button" class="chip-x" :aria-label="`${tag} 삭제`" @click="remove(tag)">×</button>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import FieldError from '@/components/common/FieldError.vue'
import { RULES } from '@/constants/rules'

defineProps({ inputId: { type: String, default: 'hashtag-input' } })
const tags = defineModel({ type: Array, default: () => [] })
const draft = ref('')
const error = ref('')

// 대소문자 통일은 서버(D-09)가 한다. 프론트는 바꾸지 않고 중복 판정도 대소문자를 구분한 그대로 한다.
function add() {
  error.value = ''
  const names = draft.value
    .split(',')
    .map((s) => s.trim().replace(/^#+/, ''))
    .filter(Boolean)
  if (!names.length) return

  const fresh = [...new Set(names)].filter((n) => !tags.value.includes(n))
  if (fresh.length < names.length) error.value = '* 중복된 값입니다.'

  const merged = [...tags.value, ...fresh]
  if (RULES.HASHTAG_MAX_COUNT && merged.length > RULES.HASHTAG_MAX_COUNT) {
    error.value = `* 해시태그는 ${RULES.HASHTAG_MAX_COUNT}개까지 넣을 수 있어요.`
  }
  // defineModel 은 새 배열을 대입해야 부모에게 update:modelValue 가 나간다 (push 금지)
  tags.value = RULES.HASHTAG_MAX_COUNT ? merged.slice(0, RULES.HASHTAG_MAX_COUNT) : merged
  draft.value = ''
}

function onEnter(e) {
  if (e.isComposing) return // 한글 조합 중 Enter 무시 (마지막 글자가 두 번 들어가는 문제)
  e.preventDefault()        // form 안에서 Enter 가 submit 되지 않게
  add()
}

function remove(tag) {
  tags.value = tags.value.filter((t) => t !== tag)
}
</script>

<style scoped>
.tag-list { margin-top: 10px; }
</style>
