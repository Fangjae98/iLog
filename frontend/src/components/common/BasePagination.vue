<template>
  <nav v-if="totalPages > 0" class="pagination" aria-label="페이지">
    <button type="button" class="btn btn-sm" :disabled="page <= 1" @click="emit('change', page - 1)">이전</button>
    <button
      v-for="n in pages"
      :key="n"
      type="button"
      class="btn btn-sm num"
      :class="{ 'btn-primary': n === page }"
      :aria-current="n === page ? 'page' : undefined"
      @click="emit('change', n)"
    >
      {{ n }}
    </button>
    <button type="button" class="btn btn-sm" :disabled="page >= totalPages" @click="emit('change', page + 1)">다음</button>
  </nav>
</template>

<script setup>
import { computed } from 'vue'

// page 는 화면 기준 1부터. API 의 0부터와의 변환은 PostListView 의 toApiParams 한 곳에서만 한다.
const props = defineProps({
  page: { type: Number, required: true },
  totalPages: { type: Number, required: true },
})
const emit = defineEmits(['change'])

const WINDOW = 5
const pages = computed(() => {
  const start = Math.max(1, Math.min(props.page - 2, props.totalPages - WINDOW + 1))
  const end = Math.min(props.totalPages, start + WINDOW - 1)
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
})
</script>

<style scoped>
.pagination { display: flex; justify-content: center; gap: 6px; margin-top: 24px; flex-wrap: wrap; }
.pagination .num { min-width: 32px; }
</style>
