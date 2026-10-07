<script setup lang="ts">
import { useNotificationStore } from '../../stores/notificationStore'
import ResponseBanner from './ResponseBanner.vue'

const store = useNotificationStore()
</script>

<template>
  <aside class="fixed top-5 right-5 z-[9999] flex flex-col gap-3 max-w-sm w-full pointer-events-none px-4 sm:px-0"
    aria-live="assertive" aria-label="Notifications">
    <TransitionGroup enter-active-class="transform ease-out duration-300 transition"
      enter-from-class="translate-y-2 opacity-0 sm:translate-y-0 sm:translate-x-4"
      enter-to-class="translate-y-0 opacity-100 sm:translate-x-0" leave-active-class="transition ease-in duration-200"
      leave-from-class="opacity-100 scale-100" leave-to-class="opacity-0 scale-95">
      <div v-for="item in store.notifications" :key="item.id"
        class="pointer-events-auto rounded-2xl overflow-hidden shadow-lg shadow-slate-900/5">
        <ResponseBanner :type="item.type" :title="item.title" :message="item.message" :dismissible="item.dismissible"
          @close="store.remove(item.id)" />
      </div>
    </TransitionGroup>
  </aside>
</template>