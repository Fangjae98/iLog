<template>
  <ul class="rules" aria-label="비밀번호 조건">
    <li v-for="rule in rules" :key="rule.label" :class="{ 'is-on': rule.ok }">{{ rule.label }}</li>
  </ul>
</template>

<script setup>
import { computed } from 'vue'

// 표시 전용. 비밀번호 규칙(D-16)이 확정되기 전까지 이 조건으로 가입·변경 버튼을 막지 않는다.
const props = defineProps({ password: { type: String, default: '' } })

// TODO D-16 / U-8: 확정되면 constants/rules.js 의 PASSWORD_PATTERN 과 문구를 맞춘다
const rules = computed(() => [
  { label: '영문 + 숫자 조합', ok: /[A-Za-z]/.test(props.password) && /\d/.test(props.password) },
  { label: '특수문자 가능', ok: /[^A-Za-z0-9]/.test(props.password) },
  { label: '8자 이상', ok: props.password.length >= 8 },
])
</script>

<style scoped>
.rules { display: grid; gap: 3px; margin: 8px 0 0 2px; font-size: var(--fs-2xs); color: var(--muted); }
.rules li { display: flex; align-items: center; gap: 8px; }
.rules li::before { content: ''; flex: none; width: 8px; height: 8px; border: 1px solid var(--muted); border-radius: 50%; }
.rules li.is-on { color: var(--label); text-decoration: line-through; }
.rules li.is-on::before { background: var(--text-2); border-color: var(--text-2); }
</style>
