<script setup lang="ts">
export type OrderStatus = 'pending' | 'processing' | 'shipped' | 'delivered' | 'cancelled';

defineProps<{
  status: OrderStatus;
}>();
</script>

<template>
  <span 
    :class="[
      'px-2.5 py-1 rounded-full text-[10px] font-mono font-bold uppercase tracking-wider inline-block shrink-0 whitespace-nowrap',
      status === 'pending' ? 'bg-amber-500/10 text-amber-400 border border-amber-500/30' : '',
      status === 'processing' ? 'bg-blue-500/10 text-blue-400 border border-blue-500/30' : '',
      status === 'shipped' ? 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/30' : '',
      status === 'delivered' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30' : '',
      status === 'cancelled' ? 'bg-rose-500/10 text-rose-400 border border-rose-500/30' : ''
    ]"
  >
    {{ status }}
  </span>
</template>