<script setup lang="ts">
import ProductCard, { type Product } from './ProductCard.vue';

defineProps<{
  products: Product[];
  loading?: boolean;
}>();

const emit = defineEmits<{
  (e: 'add-to-cart', product: Product): void;
}>();
</script>

<template>
  <div v-if="loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
    <div v-for="i in 8" :key="i" class="h-72 bg-slate-900/50 border border-slate-800/80 rounded-2xl animate-pulse"></div>
  </div>

  <div v-else-if="products.length === 0" class="text-center py-16 bg-slate-900/40 border border-slate-800 rounded-2xl">
    <p class="text-slate-400 text-xs font-semibold">No hardware found matching criteria.</p>
  </div>

  <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
    <ProductCard 
      v-for="item in products" 
      :key="item.id" 
      :product="item" 
      @add-to-cart="(p: any) => emit('add-to-cart', p)" 
    />
  </div>
</template>
