<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'

import AppNavbar from './components/common/AppNavbar.vue'
import AppFooter from './components/common/AppFooter.vue'
import GlobalToast from './components/common/GlobalToast.vue'
import CustomCenterPopup from './components/common/CustomCenterPopup.vue'

const route = useRoute()

// Pages that should NOT have Navbar and Footer
const isAuthPage = computed(() => {
  return (
    route.path === '/login' ||
    route.path === '/register' ||
    route.path === '/admin' ||
    route.path.startsWith('/admin/')
  )
})
</script>

<template>
  <div class="flex min-h-screen supports-[height:100dvh]:min-h-[100dvh] flex-col overflow-x-clip font-sans">

    <!-- Global Toast Notifications for entire app -->
    <GlobalToast />

    <!-- Global Center Popup for all user view interfaces -->
    <CustomCenterPopup />

    <!-- Navbar only on normal pages -->
    <AppNavbar v-if="!isAuthPage" />

    <!-- Page Content -->
    <main class="w-full flex-1">
      <RouterView />
    </main>

    <!-- Footer only on normal pages -->
    <AppFooter v-if="!isAuthPage" />

  </div>
</template>