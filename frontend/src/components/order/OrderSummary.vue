<script setup lang="ts">
import { ref } from 'vue';

const props = defineProps<{
  subtotal: number;
  shippingFee?: number;
  taxRate?: number;
}>();

const promoCode = ref('');
const discount = ref(0);

function applyPromo() {
  if (promoCode.value.toUpperCase() === 'NEXUS10') {
    discount.value = props.subtotal * 0.1;
  }
}
</script>

<template>
  <div class="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-4">
    <h3 class="text-xs font-bold text-white uppercase tracking-wider border-b border-slate-800 pb-3">Order Summary</h3>

    <div class="space-y-2 text-xs">
      <div class="flex justify-between text-slate-400">
        <span>Subtotal</span>
        <span class="text-white font-semibold">LKR {{ subtotal.toLocaleString() }}</span>
      </div>
      <div class="flex justify-between text-slate-400">
        <span>Estimated Shipping</span>
        <span class="text-white font-semibold">LKR {{ (shippingFee || 15).toLocaleString() }}</span>
      </div>
      <div v-if="discount > 0" class="flex justify-between text-emerald-400">
        <span>Discount</span>
        <span>-LKR {{ discount.toLocaleString() }}</span>
      </div>
    </div>

    <form @submit.prevent="applyPromo" class="flex gap-2 pt-2">
      <input v-model="promoCode" type="text" placeholder="Promo code" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-1.5 text-xs text-white uppercase" />
      <button type="submit" class="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-white font-bold rounded-xl text-xs shrink-0 whitespace-nowrap">Apply</button>
    </form>

    <div class="border-t border-slate-800 pt-3 flex justify-between text-sm font-black text-white">
      <span>Total</span>
      <span class="text-cyan-400">LKR {{ (subtotal + (shippingFee || 15) - discount).toLocaleString() }}</span>
    </div>
  </div>
</template>