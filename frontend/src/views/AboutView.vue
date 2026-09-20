<template>
  <section class="about">
    <h1>iLog 에 오신 것을 환영합니다!</h1>
    <!-- TODO U-4: 서비스 설명 문구 팀 확정 후 교체 -->
    <p>
      iLog는 하루에 하나씩 공부한 내용을 기록하는 게시판입니다.<br />
      제목과 내용, 참고한 URL, 해시태그를 남기고<br />
      날짜와 해시태그로 지난 기록을 다시 찾아볼 수 있습니다.
    </p>
  </section>
</template>

<style scoped>
.about {
  max-width: 400px; margin: 48px auto 0; padding: 36px 28px 40px;
  background: var(--postit-yellow); color: var(--postit-text);
  box-shadow: var(--shadow-postit); transform: rotate(-0.8deg);
  text-align: center;
}
.about h1 { margin-bottom: 14px; font-family: var(--font-hand); font-weight: 700; font-size: var(--fs-2xl); line-height: 35px; }
.about p { font-size: var(--fs-md); line-height: 1.8; }
</style>
