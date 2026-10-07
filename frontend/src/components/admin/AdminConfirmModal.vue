<script setup lang="ts">
import { computed } from 'vue'

export type ModalType = 'danger' | 'warning' | 'info' | 'success'
export type ModalIcon = 'delete' | 'logout' | 'warning' | 'info' | 'success'

export interface AdminModalState {
  show: boolean
  type?: ModalType
  icon?: ModalIcon
  title: string
  message?: string
  target?: string
  description?: string
  confirmText?: string
  cancelText?: string
  confirmClass?: string
  loading?: boolean
  zIndex?: string | number
  showCancel?: boolean
  onConfirm?: () => void | Promise<void>
}

const props = withDefaults(
  defineProps<{
    show: boolean
    type?: ModalType
    icon?: ModalIcon
    title: string
    message?: string
    target?: string
    description?: string
    confirmText?: string
    cancelText?: string
    confirmClass?: string
    loading?: boolean
    zIndex?: string | number
    showCancel?: boolean
  }>(),
  {
    type: 'danger',
    icon: undefined,
    message: '',
    target: '',
    description: '',
    confirmText: 'Confirm',
    cancelText: 'Cancel',
    confirmClass: '',
    loading: false,
    zIndex: 99999,
    showCancel: true
  }
)

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
  (e: 'confirm'): void
  (e: 'cancel'): void
}>()

const close = () => {
  if (props.loading) return
  emit('update:show', false)
  emit('cancel')
}

const onConfirm = () => {
  if (props.loading) return
  emit('confirm')
}

const resolvedIcon = computed<ModalIcon>(() => {
  if (props.icon) return props.icon
  const t = (props.title || '').toLowerCase()
  if (t.includes('sign out') || t.includes('logout') || t.includes('log out')) {
    return 'logout'
  }
  if (props.type === 'danger') return 'delete'
  if (props.type === 'warning') return 'warning'
  if (props.type === 'success') return 'success'
  return 'info'
})

const theme = computed(() => {
  switch (props.type) {
    case 'warning':
      return {
        iconBg: 'bg-amber-50 border-amber-200 text-amber-500',
        confirmBtn: 'bg-amber-500 hover:bg-amber-600 focus:ring-amber-400'
      }
    case 'success':
      return {
        iconBg: 'bg-emerald-50 border-emerald-200 text-emerald-600',
        confirmBtn: 'bg-emerald-600 hover:bg-emerald-700 focus:ring-emerald-400'
      }
    case 'info':
      return {
        iconBg: 'bg-blue-50 border-blue-200 text-blue-600',
        confirmBtn: 'bg-blue-600 hover:bg-blue-700 focus:ring-blue-400'
      }
    case 'danger':
    default:
      return {
        iconBg: 'bg-red-50 border-red-200 text-red-500',
        confirmBtn: 'bg-red-500 hover:bg-red-600 focus:ring-red-400'
      }
  }
})
</script>

<template>
  <Teleport to="body">
    <Transition name="admin-modal">
      <div
        v-if="show"
        class="fixed inset-0 flex items-center justify-center p-4 select-none pointer-events-auto"
        :style="{ zIndex: zIndex || 99999 }"
      >
      <!-- Backdrop -->
      <div
        class="absolute inset-0 bg-slate-950/30 backdrop-blur-sm transition-opacity"
        @click="close"
      ></div>

      <!-- Modal Dialog -->
      <div
        class="relative w-full max-w-md bg-white/95 backdrop-blur-2xl border border-white rounded-3xl shadow-2xl p-6 text-center overflow-hidden"
      >
        <!-- Icon Badge -->
        <div
          class="w-14 h-14 mx-auto rounded-2xl border flex items-center justify-center transition-transform hover:scale-105"
          :class="theme.iconBg"
        >
          <!-- Logout / Sign Out Icon -->
          <svg
            v-if="resolvedIcon === 'logout'"
            class="w-6 h-6"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"
            />
          </svg>

          <!-- Warning Icon -->
          <svg
            v-else-if="resolvedIcon === 'warning'"
            class="w-6 h-6"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M12 9v4m0 4h.01M10.3 3.8l-8.2 14a2 2 0 001.73 3h16.34a2 2 0 001.73-3l-8.2-14a2 2 0 00-3.4 0z"
            />
          </svg>

          <!-- Danger / Delete Icon -->
          <svg
            v-else-if="resolvedIcon === 'delete'"
            class="w-6 h-6"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6M9 7V4a1 1 0 011-1h4a1 1 0 011 1v3m-7 0h10"
            />
          </svg>

          <!-- Success Icon -->
          <svg
            v-else-if="resolvedIcon === 'success'"
            class="w-6 h-6"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>

          <!-- Info Icon -->
          <svg
            v-else
            class="w-6 h-6"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
        </div>

        <!-- Title -->
        <h2 class="mt-5 text-xl font-black text-slate-950 tracking-tight">
          {{ title }}
        </h2>

        <!-- Message Body -->
        <p class="mt-2 text-sm text-slate-500 leading-relaxed">
          <span v-if="message">{{ message }}</span>
          <span v-if="message && target">&nbsp;</span>
          <span v-if="target" class="font-bold text-slate-800">
            {{ target }}
          </span>
          <span v-if="message && target && !description">?</span>
        </p>

        <p v-if="description" class="mt-1 text-xs text-slate-400">
          {{ description }}
        </p>

        <!-- Action Buttons -->
        <div class="mt-6 flex flex-col sm:flex-row gap-3">
          <button
            v-if="showCancel && cancelText"
            type="button"
            @click="close"
            :disabled="loading"
            class="flex-1 px-5 py-3 rounded-xl bg-white border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50 hover:text-slate-800 active:scale-[0.98] transition disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ cancelText }}
          </button>

          <button
            type="button"
            @click="onConfirm"
            :disabled="loading"
            :class="[
              confirmClass || theme.confirmBtn,
              loading ? 'opacity-70 cursor-wait' : 'hover:shadow-lg active:scale-[0.98]',
              (!showCancel || !cancelText) ? 'w-full' : 'flex-1'
            ]"
            class="px-5 py-3 rounded-xl text-white text-sm font-bold transition flex items-center justify-center gap-2"
          >
            <svg
              v-if="loading"
              class="animate-spin h-4 w-4 text-white"
              fill="none"
              viewBox="0 0 24 24"
            >
              <circle
                class="opacity-25"
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                stroke-width="4"
              />
              <path
                class="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
              />
            </svg>
            <span>{{ loading ? 'Processing...' : (confirmText || 'Confirm') }}</span>
          </button>
        </div>
      </div>
    </div>
  </Transition>
</Teleport>
</template>

<style scoped>
.admin-modal-enter-active,
.admin-modal-leave-active {
  transition: all 0.25s ease;
}

.admin-modal-enter-from,
.admin-modal-leave-to {
  opacity: 0;
}

.admin-modal-enter-active > div:last-child,
.admin-modal-leave-active > div:last-child {
  transition: all 0.25s ease;
}

.admin-modal-enter-from > div:last-child,
.admin-modal-leave-to > div:last-child {
  opacity: 0;
  transform: translateY(10px) scale(0.98);
}
</style>
