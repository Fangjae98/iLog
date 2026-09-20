import { reactive, readonly } from 'vue'

// @metric MAINT: 팝업 상태를 컴포저블 하나로 모아 화면마다 따로 만들지 않는다.
// window.alert / window.confirm 대신 쓴다.
// 앱 전체에서 팝업은 하나만 뜨므로 모듈 단위 상태로 둔다.
const state = reactive({
  open: false,
  title: '',
  description: '',
  confirmText: '확인',
  cancelText: '',
  resolve: null,
})

function show(options) {
  // 사용자가 버튼을 누를 때까지 await가 기다리도록 Promise를 반환
  return new Promise((resolve) => {
    Object.assign(state, {
      open: true,
      title: options.title,
      description: options.description ?? '',
      confirmText: options.confirmText ?? '확인',
      cancelText: options.cancelText ?? '',
      resolve,
    })
  })
}

function close(result) {
  state.open = false

  // 확인은 true, 취소는 false를 show()를 호출한 화면에 돌려줌
  state.resolve?.(result)
  state.resolve = null
}

export function useDialog() {
  return {
    state: readonly(state),
    // 확인 버튼 하나. await 하면 닫힐 때까지 기다린다 //
    alert: (options) => show(options),
    // 취소 + 실행 버튼. true(실행) / false(취소) //
    confirm: (options) => show({ cancelText: '취소', ...options }),
    close,
  }
}
