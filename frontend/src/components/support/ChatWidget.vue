<script setup lang="ts">
import { ref } from 'vue';

const isOpen = ref(false);
const message = ref('');
const messages = ref([
  { id: 1, sender: 'agent', text: 'Welcome to Nexus Systems Support. How can we assist your setup today?' }
]);

function sendMessage() {
  if (!message.value.trim()) return;
  messages.value.push({ id: Date.now(), sender: 'user', text: message.value });
  message.value = '';
}
</script>

<template>
  <div class="fixed bottom-5 right-5 z-50">
    <button 
      @click="isOpen = !isOpen"
      class="w-12 h-12 rounded-full bg-gradient-to-tr from-blue-600 to-cyan-400 text-white font-bold flex items-center justify-center shadow-lg shadow-cyan-500/20 hover:scale-105 transition-transform"
    >
      💬
    </button>

    <div 
      v-if="isOpen"
      class="absolute bottom-16 right-0 w-80 sm:w-96 bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl flex flex-col h-96 overflow-hidden"
    >
      <div class="p-3 bg-slate-950 border-b border-slate-800 flex justify-between items-center">
        <span class="text-xs font-bold text-white">Live Hardware Diagnostics</span>
        <button @click="isOpen = false" class="text-slate-400 hover:text-white text-xs">✕</button>
      </div>

      <div class="flex-1 p-3 overflow-y-auto space-y-2 text-xs">
        <div 
          v-for="msg in messages" 
          :key="msg.id" 
          :class="['p-2.5 rounded-xl max-w-[80%]', msg.sender === 'user' ? 'bg-cyan-500 text-slate-950 ml-auto font-medium' : 'bg-slate-950 text-slate-300 border border-slate-800']"
        >
          {{ msg.text }}
        </div>
      </div>

      <form @submit.prevent="sendMessage" class="p-2 bg-slate-950 border-t border-slate-800 flex gap-1">
        <input v-model="message" type="text" placeholder="Type message..." class="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-1.5 text-xs text-white focus:outline-none" />
        <button type="submit" class="px-3 py-1.5 bg-cyan-500 text-slate-950 font-bold text-xs rounded-xl">Send</button>
      </form>
    </div>
  </div>
</template>