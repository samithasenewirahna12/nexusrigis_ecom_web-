<script setup lang="ts">
withDefaults(defineProps<{
  variant?: 'cyan' | 'slate' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  disabled?: boolean;
  loading?: boolean;
  type?: 'button' | 'submit' | 'reset';
}>(), {
  variant: 'cyan',
  size: 'md',
  disabled: false,
  loading: false,
  type: 'button'
});
</script>

<template>
  <button
    :type="type"
    :disabled="disabled || loading"
    :class="[
      'font-bold rounded-xl transition-all flex items-center justify-center gap-2 shrink-0 whitespace-nowrap disabled:opacity-50 disabled:cursor-not-allowed',
      size === 'sm' ? 'px-3 py-1.5 text-xs' : size === 'lg' ? 'px-6 py-3.5 text-sm' : 'px-4 py-2.5 text-xs',
      variant === 'cyan' ? 'bg-gradient-to-r from-blue-600 to-cyan-500 hover:from-blue-500 hover:to-cyan-400 text-white shadow-md shadow-cyan-500/20' : '',
      variant === 'slate' ? 'bg-slate-900 border border-slate-800 hover:border-slate-700 text-slate-300 hover:text-white' : '',
      variant === 'danger' ? 'bg-rose-600 hover:bg-rose-500 text-white' : ''
    ]"
  >
    <span v-if="loading" class="animate-spin text-xs">🌀</span>
    <slot />
  </button>
</template>