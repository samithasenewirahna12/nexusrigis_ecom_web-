<script setup lang="ts">
import { reactive } from 'vue';

const emit = defineEmits<{
  (e: 'submit', ticket: typeof form): void;
}>();

const form = reactive({
  subject: '',
  category: 'Hardware Issue',
  priority: 'normal',
  message: ''
});

function onSubmit() {
  emit('submit', { ...form });
}
</script>

<template>
  <form @submit.prevent="onSubmit" class="bg-slate-900 border border-slate-800 rounded-2xl p-6 space-y-4">
    <h3 class="text-xs font-bold text-white uppercase tracking-wider">Submit Technical Ticket</h3>

    <input v-model="form.subject" required type="text" placeholder="Issue Subject" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-cyan-500" />

    <div class="grid grid-cols-2 gap-3">
      <select v-model="form.category" class="bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none">
        <option>Hardware Issue</option>
        <option>Order Inquiries</option>
        <option>Warranty & RMA</option>
      </select>

      <select v-model="form.priority" class="bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none">
        <option value="low">Low Priority</option>
        <option value="normal">Normal Priority</option>
        <option value="urgent">Urgent</option>
      </select>
    </div>

    <textarea v-model="form.message" required rows="4" placeholder="Describe your technical request..." class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-cyan-500"></textarea>

    <button type="submit" class="w-full py-2.5 bg-gradient-to-r from-blue-600 to-cyan-500 text-white font-bold rounded-xl text-xs">
      Submit Ticket
    </button>
  </form>
</template>