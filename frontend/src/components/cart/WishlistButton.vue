<script setup lang="ts">
defineProps<{
  active?: boolean;
}>();

const emit = defineEmits<{
  (e: 'toggle'): void;
}>();
</script>

<template>
  <button 
    @click="emit('toggle')"
    type="button"
    :class="[
      'p-2 rounded-xl border transition-all flex items-center justify-center text-xs font-bold shrink-0',
      active ? 'bg-rose-500/10 border-rose-500/40 text-rose-400' : 'bg-slate-900 border-slate-800 text-slate-400 hover:text-white'
    ]"
  >
    <span>{{ active ? '♥' : '♡' }}</span>
  </button>
</template>