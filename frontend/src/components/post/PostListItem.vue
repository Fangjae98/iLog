<template>
  <tr class="post-row" @click="goDetail">
    <td class="c-no num">{{ post.postId }}</td>
    <td class="c-title">
      <RouterLink :to="detailRoute" @click.stop>{{ post.title }}</RouterLink>
      <ul v-if="post.hashtags.length" class="chips row-tags">
        <li v-for="tag in post.hashtags" :key="tag">
          <button type="button" class="chip chip-sm" @click.stop="emit('tag', tag)"># {{ tag }}</button>
        </li>
      </ul>
    </td>
    <td class="c-nick">{{ post.nickname }}</td>
    <td class="c-time num">{{ timeLabel }}</td>
  </tr>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { formatClock, formatDotDate, isTodayKst } from '@/utils/date'

const props = defineProps({ post: { type: Object, required: true } })
const emit = defineEmits(['tag'])
const router = useRouter()

const detailRoute = computed(() => ({ name: 'post-detail', params: { postId: props.post.postId } }))
// 오늘 글은 시각(14:20), 지난 글은 월.일(09.18)
const timeLabel = computed(() =>
  isTodayKst(props.post.createdAt) ? formatClock(props.post.createdAt) : formatDotDate(props.post.createdAt).slice(5),
)

function goDetail() {
  router.push(detailRoute.value)
}
</script>

<style scoped>
.post-row { cursor: pointer; }
.post-row:hover td { background: var(--row-hover); }
td { padding: 12px 14px; border-top: 1px solid var(--line-soft); font-size: 13px; color: var(--text-2); vertical-align: middle; }
.c-no { width: 64px; color: var(--btn); }
.c-title a { text-decoration: none; }
.c-title a:hover { text-decoration: underline; }
.row-tags { gap: 4px; margin-top: 5px; }
.c-nick { width: 110px; white-space: nowrap; }
.c-time { width: 84px; color: var(--label); white-space: nowrap; }
@media (max-width: 560px) { .c-no { display: none; } }
</style>
