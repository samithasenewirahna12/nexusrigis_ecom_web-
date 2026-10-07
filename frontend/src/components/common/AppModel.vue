<script setup lang="ts">
defineProps<{
  isOpen: boolean;
  title?: string;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
}>();
</script>

<template>
  <Teleport to="body">
    <div 
      v-if="isOpen" 
      class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm"
      @click.self="emit('close')"
    >
      <div class="bg-slate-900 border border-slate-800 rounded-3xl max-w-lg w-full max-h-[90vh] overflow-y-auto p-6 sm:p-8 space-y-6 relative shadow-2xl">
        
        <div class="flex items-center justify-between border-b border-slate-800/80 pb-4">
          <h3 class="text-lg font-bold text-white">{{ title || 'Dialog' }}</h3>
          <button @click="emit('close')" class="text-slate-400 hover:text-white text-sm font-bold p-1">✕</button>
        </div>

        <div class="text-slate-300 text-sm">
          <slot />
        </div>

        <div v-if="$slots.footer" class="border-t border-slate-800/80 pt-4 flex justify-end gap-2">
          <slot name="footer" />
        </div>

      </div>
    </div>
  </Teleport>
</template>