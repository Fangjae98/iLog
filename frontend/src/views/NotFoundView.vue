<template>
  <div class="not-found" role="alert">
    <p>페이지를 찾을 수 없어요</p>
    <RouterLink class="btn btn-primary" :to="{ name: 'post-list' }">목록으로</RouterLink>
  </div>
</template>

<style scoped>
.not-found {
  width: 300px; max-width: 100%; margin: 96px auto 0; padding: 32px 26px 24px;
  background: var(--modal); border: 1px solid var(--line); border-radius: 4px;
  box-shadow: var(--shadow-paper); text-align: center;
}
.not-found .btn { min-width: 110px; margin-top: 24px; }
</style>
