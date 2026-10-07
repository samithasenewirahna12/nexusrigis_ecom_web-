<script setup lang="ts">
import { ref, watch } from 'vue';

const props = defineProps<{
  categories: string[];
  maxPriceLimit?: number;
}>();

const emit = defineEmits<{
  (e: 'change', filters: { category: string; maxPrice: number; inStockOnly: boolean }): void;
}>();

const selectedCategory = ref('');
const maxPrice = ref(props.maxPriceLimit || 3000);
const inStockOnly = ref(false);

watch([selectedCategory, maxPrice, inStockOnly], () => {
  emit('change', {
    category: selectedCategory.value,
    maxPrice: maxPrice.value,
    inStockOnly: inStockOnly.value
  });
});
</script>

<template>
  <div class="bg-slate-900 border border-slate-800 rounded-2xl p-4 space-y-5">
    <div>
      <h4 class="text-xs font-bold text-white uppercase tracking-wider mb-3">Category</h4>
      <select v-model="selectedCategory" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-cyan-500">
        <option value="">All Categories</option>
        <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
      </select>
    </div>

    <div>
      <div class="flex justify-between text-xs font-bold text-slate-300 mb-2">
        <span>Max Price</span>
        <span class="text-cyan-400">LKR {{ maxPrice }}</span>
      </div>
      <input type="range" min="100" max="5000" step="100" v-model.number="maxPrice" class="w-full accent-cyan-500" />
    </div>

    <label class="flex items-center gap-2 cursor-pointer text-xs font-medium text-slate-300">
      <input type="checkbox" v-model="inStockOnly" class="rounded bg-slate-950 border-slate-800 text-cyan-500 focus:ring-0" />
      <span>In-Stock Items Only</span>
    </label>
  </div>
</template>