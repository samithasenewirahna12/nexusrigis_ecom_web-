<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const searchQuery = ref('')

const categories = [
  { name: 'Graphics', short: 'GPU', query: 'Graphics Cards' },
  { name: 'Processors', short: 'CPU', query: 'Processors' },
  { name: 'Memory', short: 'RAM', query: 'Memory' },
  { name: 'Storage', short: 'SSD', query: 'Storage' },
  { name: 'Power', short: 'PSU', query: 'Power Supplies' }
]

function handleSearch() {
  const query = searchQuery.value.trim()

  if (!query) return

  router.push({
    path: '/catalog',
    query: { q: query }
  })
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}
</script>

<template>
  <div class="relative min-h-[calc(100vh-5rem)] overflow-hidden bg-slate-50">

    <!-- Background -->
    <div class="pointer-events-none absolute inset-0">
      <div class="absolute -left-20 -top-20 h-56 w-56 rounded-full bg-blue-200/30 blur-3xl"></div>
      <div class="absolute -right-20 top-1/3 h-60 w-60 rounded-full bg-cyan-200/25 blur-3xl"></div>
    </div>

    <!-- Main -->
    <main class="relative z-10 flex min-h-[calc(100vh-5rem)] items-center justify-center px-4 py-6">

      <div class="w-full max-w-xl">

        <!-- Compact Card -->
        <div
          class="overflow-hidden rounded-2xl border border-slate-200/80 bg-white/80 shadow-[0_15px_45px_rgba(15,23,42,0.08)] backdrop-blur-xl">

          <!-- Accent -->
          <div class="h-0.5 bg-gradient-to-r from-blue-600 via-indigo-500 to-cyan-400"></div>

          <div class="px-5 py-6 sm:px-7">

            <!-- Status -->
            <div class="flex justify-center">
              <span
                class="inline-flex items-center gap-1.5 rounded-full border border-blue-100 bg-blue-50 px-2.5 py-1 text-[8px] font-bold uppercase tracking-wider text-blue-700">
                <span class="h-1.5 w-1.5 rounded-full bg-blue-600"></span>
                404 Error
              </span>
            </div>

            <!-- 404 -->
            <div class="mt-3 text-center">
              <div class="text-[5rem] font-black leading-none tracking-[-0.08em] text-slate-900 sm:text-[6rem]">
                404
              </div>

              <div class="mx-auto mt-1 h-0.5 w-12 rounded-full bg-blue-600"></div>
            </div>

            <!-- Message -->
            <div class="mt-4 text-center">
              <h1 class="text-xl font-black tracking-tight text-slate-900 sm:text-2xl">
                Signal Lost
              </h1>

              <p class="mx-auto mt-1.5 max-w-sm text-xs leading-5 text-slate-500">
                This page could not be found or has been moved.
              </p>
            </div>

            <!-- Search -->
            <form @submit.prevent="handleSearch" class="mx-auto mt-5 max-w-md">
              <div
                class="flex items-center rounded-xl border border-slate-200 bg-slate-50 p-1 transition focus-within:border-blue-300 focus-within:bg-white focus-within:ring-4 focus-within:ring-blue-500/10">
                <svg class="ml-2.5 h-4 w-4 shrink-0 text-slate-400" fill="none" viewBox="0 0 24 24"
                  stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                    d="m21 21-4.35-4.35m2.35-5.15a7.5 7.5 0 1 1-15 0 7.5 7.5 0 0 1 15 0Z" />
                </svg>

                <input v-model="searchQuery" type="text" placeholder="Search products..."
                  class="h-9 min-w-0 flex-1 bg-transparent px-2 text-xs text-slate-800 outline-none placeholder:text-slate-400" />

                <button type="submit"
                  class="h-8 rounded-lg bg-blue-600 px-3 text-[10px] font-bold text-white transition hover:bg-blue-700">
                  Search
                </button>
              </div>
            </form>

            <!-- Actions -->
            <div class="mt-4 flex justify-center gap-2">
              <button type="button" @click="goBack"
                class="inline-flex h-9 items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-3.5 text-[10px] font-bold text-slate-700 transition hover:bg-slate-50">
                <svg class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M19 12H5m7 7-7-7 7-7" />
                </svg>
                Back
              </button>

              <router-link to="/"
                class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-3.5 text-[10px] font-bold text-white transition hover:bg-blue-700">
                <svg class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                    d="m3 10 9-7 9 7v10a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 20V10Zm6 11.5v-6h6v6" />
                </svg>
                Home
              </router-link>

              <router-link to="/support"
                class="inline-flex h-9 items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-3.5 text-[10px] font-bold text-slate-700 transition hover:border-cyan-200 hover:bg-cyan-50 hover:text-cyan-700">
                <svg class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                    d="M4 13v-1a8 8 0 0 1 16 0v1M4 13h2a1.5 1.5 0 0 1 1.5 1.5V18H6a2 2 0 0 1-2-2v-3Zm16 0h-2a1.5 1.5 0 0 0-1.5 1.5V18H18a2 2 0 0 0 2-2v-3ZM17 18a5 5 0 0 1-5 3" />
                </svg>
                Support
              </router-link>
            </div>

            <!-- Categories -->
            <div class="mt-5 border-t border-slate-200 pt-4">

              <p class="text-center text-[8px] font-bold uppercase tracking-[0.15em] text-slate-400">
                Popular Categories
              </p>

              <div class="mt-3 grid grid-cols-5 gap-1.5">
                <router-link v-for="category in categories" :key="category.name" :to="{
                  path: '/catalog',
                  query: { category: category.query }
                }"
                  class="group flex h-12 flex-col items-center justify-center rounded-lg border border-slate-200 bg-white px-1 transition hover:border-blue-200 hover:bg-blue-50">
                  <span
                    class="flex h-5.5 w-5.5 items-center justify-center rounded-md bg-slate-100 text-[7px] font-black text-slate-500 group-hover:bg-blue-600 group-hover:text-white">
                    {{ category.short }}
                  </span>

                  <span class="mt-1 max-w-full truncate text-[7px] font-bold text-slate-500 group-hover:text-blue-600">
                    {{ category.name }}
                  </span>
                </router-link>
              </div>
            </div>

          </div>
        </div>

        <!-- Footer -->
        <p class="mt-3 text-center text-[8px] font-medium uppercase tracking-wider text-slate-400">
          NexusRigs · Performance Hardware
        </p>

      </div>
    </main>
  </div>
</template>