<script setup>
import { useRoute } from 'vue-router'
import AppDialog from '@/components/common/AppDialog.vue'

const route = useRoute()
</script>

<template>
  <div class="page" :class="{ 'page-flush': route.name === 'login' }">
    <main class="page-body">
      <RouterView />
    </main>
  </div>

  <AppDialog />
</template>