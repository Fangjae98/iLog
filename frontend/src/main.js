import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import '@/assets/styles/base.css'

const app = createApp(App)

// @metric ERROR: 렌더링·이벤트 핸들러에서 잡히지 않은 에러를 한 곳에서 기록한다 (에러 수집 도구 연결 자리)
app.config.errorHandler = (err, instance, info) => {
  console.error('[vue-error]', info, err)
  // TODO U-11: 에러 수집 도구(Sentry 등) 도입 시 여기서 전송
}

// @metric ERROR: await 없이 버려진 Promise 실패도 기록한다
window.addEventListener('unhandledrejection', (e) => {
  console.error('[unhandled-rejection]', e.reason)
})

app.use(createPinia()) // ① Pinia를 먼저: 라우터 가드가 첫 이동 때 useAuthStore()를 부른다
app.use(router)        // ②
app.mount('#app')      // ③

// @metric BUNDLE: 개발 서버에서만 불러와 운영 빌드에는 측정 코드가 들어가지 않는다
if (import.meta.env.DEV) {
  import('@/utils/vitals').then(({ reportWebVitals }) => reportWebVitals())
}
