<script setup lang="ts">

import { computed } from 'vue'


export type NotificationType =
  | 'info'
  | 'success'
  | 'warning'
  | 'error'


const props = withDefaults(
  defineProps<{
    type?: NotificationType
    mode?: 'banner' | 'label' | 'inline'
    title?: string
    message?: string
    dismissible?: boolean
    compact?: boolean
  }>(),
  {
    type: 'info',
    mode: 'banner',
    dismissible: false,
    compact: true
  }
)


const emit = defineEmits<{
  (e: 'close'): void
}>()


const styles = computed(() => {

  switch (props.type) {

    case 'success':
      return {
        labelBg:
          'bg-emerald-50 text-emerald-700 border-emerald-200',

        iconBadgeBg:
          'bg-emerald-50 text-emerald-600 border-emerald-200',

        dot:
          'bg-emerald-500'
      }

    case 'error':
      return {
        labelBg:
          'bg-rose-50 text-rose-700 border-rose-200',

        iconBadgeBg:
          'bg-rose-50 text-rose-600 border-rose-200',

        dot:
          'bg-rose-500'
      }

    case 'warning':
      return {
        labelBg:
          'bg-amber-50 text-amber-700 border-amber-200',

        iconBadgeBg:
          'bg-amber-50 text-amber-600 border-amber-200',

        dot:
          'bg-amber-500'
      }

    case 'info':
    default:
      return {
        labelBg:
          'bg-blue-50 text-blue-700 border-blue-200',

        iconBadgeBg:
          'bg-blue-50 text-blue-600 border-blue-200',

        dot:
          'bg-blue-500'
      }
  }

})

</script>


<template>

  <!-- =========================================================
       LABEL MODE
  ========================================================== -->

  <span v-if="mode === 'label'" :class="[
    'inline-flex items-center gap-1.5',
    'rounded-full border px-3 py-1',
    'text-xs font-semibold',
    'backdrop-blur-md',
    styles.labelBg
  ]">

    <span :class="[
      'h-1.5 w-1.5 rounded-full animate-pulse',
      styles.dot
    ]"></span>

    <slot>
      {{ message }}
    </slot>

  </span>


  <!-- =========================================================
       INLINE MODE
  ========================================================== -->

  <div v-else-if="mode === 'inline'" class="flex items-center gap-2.5 py-1 text-xs font-medium text-slate-500">

    <!-- SUCCESS -->

    <svg v-if="type === 'success'" class="h-4 w-4 shrink-0 text-emerald-600" fill="none" viewBox="0 0 24 24"
      stroke="currentColor" stroke-width="2">
      <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
    </svg>


    <!-- ERROR -->

    <svg v-else-if="type === 'error'" class="h-4 w-4 shrink-0 text-rose-600" fill="none" viewBox="0 0 24 24"
      stroke="currentColor" stroke-width="2">
      <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
    </svg>


    <!-- WARNING -->

    <svg v-else-if="type === 'warning'" class="h-4 w-4 shrink-0 text-amber-600" fill="none" viewBox="0 0 24 24"
      stroke="currentColor" stroke-width="2">
      <path stroke-linecap="round" stroke-linejoin="round"
        d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
    </svg>


    <!-- INFO -->

    <svg v-else class="h-4 w-4 shrink-0 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor"
      stroke-width="2">
      <path stroke-linecap="round" stroke-linejoin="round"
        d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>


    <div class="min-w-0 flex-1">

      <slot>
        {{ message }}
      </slot>

    </div>

  </div>


  <!-- =========================================================
       BANNER MODE
       COMPACT TOP-RIGHT TOAST
  ========================================================== -->

  <Teleport to="body">

    <Transition name="response-toast">

      <div v-if="mode === 'banner' && message"
        class="pointer-events-none fixed right-1 top-[75px] z-[99999] w-[calc(100%-2.5rem)] max-w-[380px]" role="alert"
        aria-live="polite">

        <!-- ===================================================
             TOAST CARD
        ==================================================== -->

        <div
          class="pointer-events-auto flex min-h-[46px] items-center gap-2.5 rounded-xl border border-slate-200/80 bg-white/95 px-3 py-2 shadow-lg shadow-slate-900/10 backdrop-blur-xl">

          <!-- =================================================
               ICON
          ================================================== -->

          <div :class="[
            'flex h-7 w-7 shrink-0',
            'items-center justify-center',
            'rounded-full border',
            styles.iconBadgeBg
          ]">

            <!-- SUCCESS -->

            <svg v-if="type === 'success'" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"
              stroke-width="2.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
            </svg>


            <!-- ERROR -->

            <svg v-else-if="type === 'error'" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"
              stroke-width="2.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>


            <!-- WARNING -->

            <svg v-else-if="type === 'warning'" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24"
              stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round"
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>


            <!-- INFO -->

            <svg v-else class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="9" />

              <path stroke-linecap="round" d="M12 11v5" />

              <circle cx="12" cy="8" r=".5" fill="currentColor" />
            </svg>

          </div>


          <!-- =================================================
               CONTENT
          ================================================== -->

          <div class="min-w-0 flex-1">

            <h4 v-if="title" class="truncate text-[12px] font-black leading-tight text-slate-900">
              {{ title }}
            </h4>


            <p class="truncate text-[10px] leading-4 text-slate-500">
              <slot>
                {{ message }}
              </slot>
            </p>

          </div>


          <!-- =================================================
               CLOSE BUTTON
          ================================================== -->

          <button v-if="dismissible" type="button" @click="emit('close')"
            class="flex h-6 w-6 shrink-0 items-center justify-center rounded-lg text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
            aria-label="Close notification">

            <svg class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>

          </button>

        </div>

      </div>

    </Transition>

  </Teleport>

</template>


<style scoped>
.response-toast-enter-active,
.response-toast-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.response-toast-enter-from,
.response-toast-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>