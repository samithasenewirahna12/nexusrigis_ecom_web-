<script setup lang="ts">
import { reactive } from 'vue';

const emit = defineEmits<{
  (e: 'submit', formData: typeof form): void;
}>();

const form = reactive({
  fullName: '',
  email: '',
  address: '',
  city: '',
  zip: '',
  paymentMethod: 'card'
});

function handleSubmit() {
  emit('submit', { ...form });
}
</script>

<template>
  <form @submit.prevent="handleSubmit" class="bg-slate-900 border border-slate-800 rounded-2xl p-6 space-y-4">
    <h3 class="text-xs font-bold text-white uppercase tracking-wider mb-2">Shipping Details</h3>

    <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
      <input v-model="form.fullName" required type="text" placeholder="Full Name" class="bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-cyan-500" />
      <input v-model="form.email" required type="email" placeholder="Email Address" class="bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-cyan-500" />
    </div>

    <input v-model="form.address" required type="text" placeholder="Street Address" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-cyan-500" />

    <div class="grid grid-cols-2 gap-3">
      <input v-model="form.city" required type="text" placeholder="City" class="bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-cyan-500" />
      <input v-model="form.zip" required type="text" placeholder="ZIP / Postal Code" class="bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-cyan-500" />
    </div>

    <button type="submit" class="w-full py-3 bg-gradient-to-r from-blue-600 to-cyan-500 hover:from-blue-500 hover:to-cyan-400 text-white font-bold rounded-xl text-xs shadow-md shadow-cyan-500/20">
      Proceed to Payment
    </button>
  </form>
</template>