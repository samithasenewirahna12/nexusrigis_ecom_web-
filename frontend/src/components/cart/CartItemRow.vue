<script setup lang="ts">
export interface CartItem {
  id: string | number;
  name: string;
  price: number;
  image: string;
  quantity: number;
  sku?: string;
}

defineProps<{
  item: CartItem;
}>();

const emit = defineEmits<{
  (e: 'update-quantity', id: string | number, quantity: number): void;
  (e: 'remove', id: string | number): void;
}>();
</script>

<template>
  <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between p-4 bg-slate-900 border border-slate-800 rounded-2xl gap-4">
    <div class="flex items-center gap-4">
      <div class="w-16 h-16 bg-slate-950 rounded-xl border border-slate-800 p-2 shrink-0 flex items-center justify-center">
        <img :src="item.image" :alt="item.name" class="max-h-full max-w-full object-contain" />
      </div>
      <div>
        <h4 class="text-xs font-bold text-white">{{ item.name }}</h4>
        <span v-if="item.sku" class="text-[10px] font-mono text-slate-500">SKU: {{ item.sku }}</span>
        <div class="text-xs font-black text-cyan-400 mt-1">LKR {{ item.price.toLocaleString() }}</div>
      </div>
    </div>

    <div class="flex items-center justify-between sm:justify-end w-full sm:w-auto gap-4">
      <div class="flex items-center bg-slate-950 border border-slate-800 rounded-xl p-1">
        <button @click="emit('update-quantity', item.id, item.quantity - 1)" class="w-6 h-6 text-slate-400 hover:text-white flex items-center justify-center text-xs font-bold">-</button>
        <span class="w-8 text-center text-xs font-bold text-white">{{ item.quantity }}</span>
        <button @click="emit('update-quantity', item.id, item.quantity + 1)" class="w-6 h-6 text-slate-400 hover:text-white flex items-center justify-center text-xs font-bold">+</button>
      </div>

      <div class="text-xs font-black text-white shrink-0 whitespace-nowrap min-w-[70px] text-right">
        LKR {{ (item.price * item.quantity).toLocaleString() }}
      </div>

      <button @click="emit('remove', item.id)" class="text-slate-500 hover:text-rose-400 text-xs font-bold p-1 shrink-0">
        🗑
      </button>
    </div>
  </div>
</template>
