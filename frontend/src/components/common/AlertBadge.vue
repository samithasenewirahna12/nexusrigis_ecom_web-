<script setup lang="ts">
import ResponseBanner from './ResponseBanner.vue'

withDefaults(defineProps<{
  variant?: 'info' | 'success' | 'warning' | 'error';
}>(), {
  variant: 'info'
});
</script>

<template>
  <ResponseBanner mode="label" :type="variant">
    <slot />
  </ResponseBanner>
</template>