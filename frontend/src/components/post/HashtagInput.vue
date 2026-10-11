<template>
  <div class="hashtag-input">
    <div class="row">
      <input
        :id="inputId"
        v-model="draft"
        class="box"
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

// 서버(PostPolicy.normalizeHashtag)와 같은 순서로 다듬는다: 앞뒤 공백 제거 → 맨 앞 # 제거 → 영문 소문자.
// 다듬은 값으로 검사해야 Spring 과 spring 을 같은 태그로 본다.
const normalize = (raw) => raw.trim().replace(/^#+/, '').trim().toLowerCase()

// 형식 → 중복 → 개수 순서로 검사한다. 걸리면 입력값을 지우지 않고 남겨 고칠 수 있게 한다.
function add() {
  error.value = ''
  const names = [...new Set(draft.value.split(',').map(normalize).filter(Boolean))]
  if (!names.length) return

  if (names.some((n) => !RULES.HASHTAG_PATTERN.test(n))) {
    error.value = `* 해시태그는 ${RULES.HASHTAG_MAX_LENGTH}자 이하의 한글·영문·숫자·_ 만 쓸 수 있어요.`
    return
  }

  const fresh = names.filter((n) => !tags.value.includes(n))
  if (!fresh.length) {
    error.value = '* 중복된 값입니다.'
    return
  }

  if (tags.value.length + fresh.length > RULES.HASHTAG_MAX_COUNT) {
    error.value = `* 해시태그는 ${RULES.HASHTAG_MAX_COUNT}개까지 넣을 수 있어요.`
    return
  }

  if (fresh.length < names.length) error.value = '* 중복된 값은 빼고 추가했어요.'
  // defineModel 은 새 배열을 대입해야 부모에게 update:modelValue 가 나간다 (push 금지)
  tags.value = [...tags.value, ...fresh]
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
