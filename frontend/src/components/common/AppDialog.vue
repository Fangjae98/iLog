<template>
  <Teleport to="body">
    <!-- @metric A11Y: role="dialog" + aria-modal, 열리면 확인 버튼에 포커스, Esc 로 닫기.
         keydown 은 오버레이에서 받는다(포커스가 내부 버튼에 있어도 버블링으로 올라온다). -->
    <div
      v-if="state.open"
      class="overlay"
      tabindex="-1"
      @click.self="close(false)"
      @keydown.esc="close(false)"
    >
      <div class="dialog" role="dialog" aria-modal="true" aria-labelledby="app-dialog-title">
        <p id="app-dialog-title" class="dialog-title">{{ state.title }}</p>
        <p v-if="state.description" class="dialog-desc">{{ state.description }}</p>
        <div class="dialog-actions">
          <button v-if="state.cancelText" type="button" class="btn" @click="close(false)">
            {{ state.cancelText }}
          </button>
          <button ref="confirmButton" type="button" class="btn btn-primary" @click="close(true)">
            {{ state.confirmText }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
import { useDialog } from '@/composables/useDialog'

const { state, close } = useDialog()
const confirmButton = ref(null)

watch(
  () => state.open,
  async (open) => {
    if (!open) return
    await nextTick()
    confirmButton.value?.focus()
  },
)
</script>

<style scoped>
.overlay { position: fixed; inset: 0; z-index: 50; display: grid; place-items: center; padding: 20px; background: var(--overlay); }
.dialog {
  width: 300px; max-width: 100%; padding: 26px 24px 22px;
  background: var(--modal); color: var(--text-2);
  border-radius: 4px; box-shadow: var(--shadow-modal);
  font-size: var(--fs-sm); line-height: 1.5;
}
.dialog-desc { margin-top: 4px; font-size: var(--fs-xs); color: var(--label); white-space: pre-line; }
/* 디자인 시스템대로 취소가 왼쪽, 실행이 오른쪽 */
.dialog-actions { display: flex; gap: 10px; margin-top: 20px; }
.dialog-actions .btn { flex: 1; }
</style>
