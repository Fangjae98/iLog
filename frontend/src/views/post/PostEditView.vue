<script setup>
defineProps({ postId: { type: String, required: true } })
</script>

<template>
  <section>
    <h1>게시글 수정 {{ postId }}</h1>
  </section>
</template>
