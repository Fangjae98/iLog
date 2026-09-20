<template>
  <!-- @metric A11Y: role="alert" 로 에러가 생기면 스크린리더가 바로 읽는다 -->
  <p v-if="message" class="msg msg-err" role="alert">{{ message }}</p>
</template>

<script setup>
defineProps({ message: { type: String, default: '' } })
</script>
