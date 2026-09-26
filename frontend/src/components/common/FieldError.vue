<!-- 입력 오류 및 요청 실패 메시지를 공통된 형태로 보여주는 컴포넌트 -->

<script setup>
// 부모 컴포넌트로부터 표시할 오류 메시지를 문자열로 전달받는다
defineProps({
  message: {
    type: String,
    default: '',
  },
})
</script>

 <template>
  <!-- @metric A11Y: 메시지가 있을 때만 표시하며, role="alert"로 스크린리더가 바로 읽도록 한다 -->
  <p v-if="message" class="msg msg-err" role="alert">
    {{ message }}
  </p>
</template>

