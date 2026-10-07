<script setup lang="ts">
withDefaults(defineProps<{
  rating: number;
  interactive?: boolean;
}>(), {
  interactive: false
});

const emit = defineEmits<{
  (e: 'update:rating', value: number): void;
}>();
</script>

<template>
  <div class="flex items-center gap-0.5 text-amber-400 text-xs">
    <button 
      v-for="star in 5" 
      :key="star"
      type="button"
      :disabled="!interactive"
      @click="emit('update:rating', star)"
      :class="['transition-transform', interactive ? 'hover:scale-125 cursor-pointer' : 'cursor-default']"
    >
      {{ star <= Math.round(rating) ? '★' : '☆' }}
    </button>
  </div>
</template>