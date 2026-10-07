<script setup lang="ts">
import { computed } from 'vue'
import { usePopupStore, type PopupType } from '../../stores/popupStore'

/* =========================================================
   PROPS & EMITS (TypeScript Strict Types)
========================================================= */

export interface PopupProps {
  visible?: boolean
  type?: PopupType
  title?: string
  message?: string
  category?: string
  showCancel?: boolean
  confirmText?: string
  cancelText?: string
}

const props = withDefaults(defineProps<PopupProps>(), {
  visible: undefined,
  type: undefined,
  title: undefined,
  message: undefined,
  category: undefined,
  showCancel: undefined,
  confirmText: undefined,
  cancelText: undefined
})

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'confirm'): void
  (e: 'cancel'): void
  (e: 'close'): void
}>()

const popupStore = usePopupStore()

/* =========================================================
   COMPUTED STATE (Bridging Props with Global Store)
========================================================= */

const isVisible = computed(() => {
  return props.visible !== undefined ? props.visible : popupStore.visible
})

const activeType = computed<PopupType>(() => {
  return props.type || popupStore.type || 'info'
})

const activeTitle = computed(() => {
  return props.title !== undefined ? props.title : popupStore.title
})

const activeMessage = computed(() => {
  return props.message !== undefined ? props.message : popupStore.message
})

const activeCategory = computed(() => {
  if (props.category) return props.category
  switch (activeType.value) {
    case 'success':
      return 'Success'
    case 'warning':
      return 'Confirmation'
    case 'error':
      return 'Something went wrong'
    case 'info':
    default:
      return 'Information'
  }
})

const activeShowCancel = computed(() => {
  return props.showCancel !== undefined ? props.showCancel : popupStore.showCancel
})

const activeConfirmText = computed(() => {
  return props.confirmText || popupStore.confirmText || 'OK'
})

const activeCancelText = computed(() => {
  return props.cancelText || popupStore.cancelText || 'Cancel'
})

/* =========================================================
   EVENT HANDLERS
========================================================= */

function handleClose() {
  emit('update:visible', false)
  emit('cancel')
  emit('close')

  if (props.visible === undefined) {
    popupStore.close()
  }
}

function handleConfirm() {
  emit('confirm')

  if (props.visible === undefined) {
    popupStore.handleConfirm()
  } else {
    emit('update:visible', false)
  }
}
</script>

<template>
  <!-- =====================================================
        CUSTOM CENTER POPUP (Light Theme)
  ====================================================== -->
  <Teleport to="body">
    <Transition name="popup">
      <div v-if="isVisible"
        class="fixed inset-0 z-[100000] flex items-center justify-center p-4 pointer-events-auto select-none"
        role="dialog" aria-modal="true" :aria-label="activeTitle">
        <!-- BACKDROP -->
        <div class="absolute inset-0 bg-slate-950/40 backdrop-blur-md transition-opacity duration-300"
          @click="activeShowCancel ? handleClose() : null"></div>

        <!-- POPUP CARD -->
        <div
          class="relative z-10 w-full max-w-sm sm:max-w-md overflow-hidden rounded-3xl border border-slate-200/90 bg-white/95 shadow-2xl shadow-slate-900/10 backdrop-blur-2xl transition-all">
          <!-- CONTENT -->
          <div class="p-6 sm:p-7 text-center">
            <!-- ICON BADGE -->
            <slot name="icon">
              <div
                class="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl border transition-all duration-200"
                :class="{
                  'bg-blue-50 text-blue-600 border-blue-100': activeType === 'info',
                  'bg-emerald-50 text-emerald-600 border-emerald-100': activeType === 'success',
                  'bg-amber-50 text-amber-600 border-amber-100': activeType === 'warning',
                  'bg-rose-50 text-rose-600 border-rose-100': activeType === 'error'
                }">
                <!-- INFO ICON -->
                <svg v-if="activeType === 'info'" class="h-7 w-7" fill="none" viewBox="0 0 24 24" stroke="currentColor"
                  stroke-width="2">
                  <circle cx="12" cy="12" r="9" />
                  <path stroke-linecap="round" d="M12 11v5" />
                  <circle cx="12" cy="8" r=".5" fill="currentColor" />
                </svg>

                <!-- SUCCESS ICON -->
                <svg v-else-if="activeType === 'success'" class="h-7 w-7" fill="none" viewBox="0 0 24 24"
                  stroke="currentColor" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                </svg>

                <!-- WARNING ICON -->
                <svg v-else-if="activeType === 'warning'" class="h-7 w-7" fill="none" viewBox="0 0 24 24"
                  stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round"
                    d="M12 9v3m0 4h.01M5.5 20h13a2 2 0 001.73-3L13.73 4a2 2 0 00-3.46 0l-6.5 13A2 2 0 005.5 20z" />
                </svg>

                <!-- ERROR ICON -->
                <svg v-else class="h-7 w-7" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                  <path stroke-linecap="round" d="M6 6l12 12M18 6L6 18" />
                </svg>
              </div>
            </slot>

            <!-- CATEGORY / STATUS LABEL -->
            <slot name="category">
              <p class="mt-4 text-[11px] font-bold uppercase tracking-[0.18em]" :class="{
                'text-blue-600': activeType === 'info',
                'text-emerald-600': activeType === 'success',
                'text-amber-600': activeType === 'warning',
                'text-rose-600': activeType === 'error'
              }">
                {{ activeCategory }}
              </p>
            </slot>

            <!-- TITLE -->
            <slot name="title">
              <h3 class="mt-1.5 text-xl sm:text-2xl font-extrabold tracking-tight text-slate-900">
                {{ activeTitle }}
              </h3>
            </slot>

            <!-- MESSAGE -->
            <slot name="message">
              <p class="mt-2 text-sm leading-relaxed text-slate-500">
                {{ activeMessage }}
              </p>
            </slot>

            <!-- ACTIONS -->
            <slot name="actions">
              <div class="mt-6 flex items-center gap-3">
                <!-- CANCEL (SECONDARY ACTION) -->
                <button v-if="activeShowCancel" type="button" @click="handleClose"
                  class="h-11 sm:h-12 flex-1 rounded-2xl border border-slate-200 bg-slate-50 text-sm font-bold text-slate-700 hover:bg-slate-100 transition-colors focus:outline-none focus:ring-2 focus:ring-slate-400/20 active:scale-[0.98]">
                  {{ activeCancelText }}
                </button>

                <!-- CONFIRM (HARMONIZED NEUTRAL THEME) -->
                <button type="button" @click="handleConfirm"
                  class="h-11 sm:h-12 flex-1 rounded-2xl border border-slate-200 bg-slate-50 text-sm font-bold text-slate-900 hover:bg-slate-100 transition-colors focus:outline-none focus:ring-2 focus:ring-slate-400/20 active:scale-[0.98]">
                  {{ activeConfirmText }}
                </button>
              </div>
            </slot>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.popup-enter-active,
.popup-leave-active {
  transition: opacity 0.25s ease, transform 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.popup-enter-from,
.popup-leave-to {
  opacity: 0;
  transform: scale(0.95) translateY(6px);
}
</style>