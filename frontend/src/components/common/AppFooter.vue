<script setup lang="ts">
import logo from '../../assets/icons/logoIMG-removebg-preview.svg'
const year = new Date().getFullYear()
</script>

<template>
  <footer
    class="sticky top-0 z-50 w-full border-b border-slate-300/60 bg-slate-100/80 backdrop-blur-2xl shadow-md shadow-slate-300/20">
    <!-- Soft Background Glow -->
    <div
      class="absolute -top-32 left-1/2 -translate-x-1/2 w-[500px] h-[250px] bg-blue-500/5 blur-3xl rounded-full pointer-events-none">
    </div>

    <div class="relative w-full px-4 sm:px-6 lg:px-8 xl:px-10 2xl:px-12 pt-14 pb-8">

      <!-- ================= TOP FOOTER ================= -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-10 pb-12">

        <!-- ================= BRAND ================= -->
        <div class="lg:pr-8">

          <router-link to="/" class="inline-flex items-center gap-1 group">
            <!-- Logo -->

            <div class="w-13 h-13 flex items-center justify-center overflow-hidden">
              <img :src="logo" alt="NX Logo" class="w-14 h-14 object-contain" />
            </div>

            <div class="leading-none mt-1">
              <div class="text-[17px] font-black tracking-[-0.04em] text-slate-950">
                NEXUS<span class="text-blue-600">RIGS</span>
              </div>

              <div class="text-[8px] font-bold uppercase tracking-[0.2em] text-slate-400 mt-1">
                Performance Hardware
              </div>
            </div>
          </router-link>

          <p class="mt-5 text-sm leading-6 text-slate-500 max-w-sm">
            High-performance PC components, enthusiast workstations,
            and next-generation hardware built for serious performance.
          </p>

          <!-- Social Icons -->
          <div class="flex items-center gap-2 mt-6">

            <a href="#" aria-label="Facebook"
              class="w-9 h-9 rounded-xl bg-white border border-slate-200 flex items-center justify-center text-slate-500 hover:text-blue-600 hover:border-blue-300 hover:bg-blue-50 transition-all">
              <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
                <path d="M14 8h3V4h-3c-3.31 0-5 1.69-5 5v3H6v4h3v8h4v-8h3.2l.8-4H13V9c0-.55.45-1 1-1z" />
              </svg>
            </a>

            <a href="#" aria-label="Instagram"
              class="w-9 h-9 rounded-xl bg-white border border-slate-200 flex items-center justify-center text-slate-500 hover:text-pink-600 hover:border-pink-300 hover:bg-pink-50 transition-all">
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <rect x="3" y="3" width="18" height="18" rx="5" stroke-width="1.8" />
                <circle cx="12" cy="12" r="4" stroke-width="1.8" />
                <circle cx="17.5" cy="6.5" r="1" fill="currentColor" stroke="none" />
              </svg>
            </a>

            <a href="#" aria-label="GitHub"
              class="w-9 h-9 rounded-xl bg-white border border-slate-200 flex items-center justify-center text-slate-500 hover:text-slate-950 hover:border-slate-400 hover:bg-slate-100 transition-all">
              <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
                <path fill-rule="evenodd"
                  d="M12 2C6.48 2 2 6.58 2 12.23c0 4.52 2.87 8.35 6.84 9.71.5.1.68-.22.68-.49v-1.7c-2.78.62-3.37-1.22-3.37-1.22-.45-1.18-1.11-1.5-1.11-1.5-.91-.64.07-.63.07-.63 1 .07 1.53 1.06 1.53 1.06.9 1.57 2.35 1.12 2.92.86.09-.67.35-1.12.63-1.38-2.22-.26-4.56-1.14-4.56-5.07 0-1.12.39-2.04 1.03-2.76-.1-.26-.45-1.31.1-2.73 0 0 .84-.28 2.75 1.05A9.2 9.2 0 0112 7.07c.85 0 1.71.12 2.51.36 1.91-1.33 2.75-1.05 2.75-1.05.55 1.42.2 2.47.1 2.73.64.72 1.03 1.64 1.03 2.76 0 3.94-2.34 4.8-4.57 5.06.36.32.68.94.68 1.9v2.82c0 .27.18.59.69.49A10.27 10.27 0 0022 12.23C22 6.58 17.52 2 12 2z"
                  clip-rule="evenodd" />
              </svg>
            </a>

            <a href="#" aria-label="YouTube"
              class="w-9 h-9 rounded-xl bg-white border border-slate-200 flex items-center justify-center text-slate-500 hover:text-red-600 hover:border-red-300 hover:bg-red-50 transition-all">
              <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
                <path
                  d="M23.5 6.2a3 3 0 00-2.1-2.1C19.5 3.6 12 3.6 12 3.6s-7.5 0-9.4.5A3 3 0 00.5 6.2 31.8 31.8 0 000 12a31.8 31.8 0 00.5 5.8 3 3 0 002.1 2.1c1.9.5 9.4.5 9.4.5s7.5 0 9.4-.5a3 3 0 002.1-2.1A31.8 31.8 0 0024 12a31.8 31.8 0 00-.5-5.8zM9.6 15.6V8.4l6.3 3.6-6.3 3.6z" />
              </svg>
            </a>

          </div>
        </div>

        <!-- ================= SHOP ================= -->
        <div>
          <h4 class="text-[11px] font-black uppercase tracking-[0.16em] text-slate-900 mb-5">
            Shop
          </h4>

          <ul class="space-y-3">

            <li>
              <router-link to="/catalog?cat=gpu" class="text-sm text-slate-500 hover:text-blue-600 transition-colors">
                Graphics Cards
              </router-link>
            </li>

            <li>
              <router-link to="/catalog?cat=cpu" class="text-sm text-slate-500 hover:text-blue-600 transition-colors">
                Processors
              </router-link>
            </li>

            <li>
              <router-link to="/catalog?cat=ram" class="text-sm text-slate-500 hover:text-blue-600 transition-colors">
                Memory & Storage
              </router-link>
            </li>

            <li>
              <router-link to="/catalog" class="text-sm text-slate-500 hover:text-blue-600 transition-colors">
                All Hardware
              </router-link>
            </li>

          </ul>
        </div>

        <!-- ================= SUPPORT ================= -->
        <div>
          <h4 class="text-[11px] font-black uppercase tracking-[0.16em] text-slate-900 mb-5">
            Support
          </h4>

          <ul class="space-y-3">

            <li>
              <router-link to="/support" class="text-sm text-slate-500 hover:text-blue-600 transition-colors">
                Help Center
              </router-link>
            </li>

            <li>
              <router-link to="/orders/track" class="text-sm text-slate-500 hover:text-blue-600 transition-colors">
                Order Status
              </router-link>
            </li>

            <li>
              <router-link to="/warranty" class="text-sm text-slate-500 hover:text-blue-600 transition-colors">
                RMA & Warranty
              </router-link>
            </li>

            <li>
              <router-link to="/support" class="text-sm text-slate-500 hover:text-blue-600 transition-colors">
                Contact Support
              </router-link>
            </li>

          </ul>
        </div>

        <!-- ================= NEWSLETTER ================= -->
        <div>
          <h4 class="text-[11px] font-black uppercase tracking-[0.16em] text-slate-900 mb-5">
            Stay Updated
          </h4>

          <p class="text-sm leading-6 text-slate-500 mb-4">
            Get restock alerts, exclusive hardware drops and special offers.
          </p>

          <form class="flex items-center p-1.5 bg-white border border-slate-200 rounded-xl shadow-sm">
            <input type="email" placeholder="Email address"
              class="min-w-0 flex-1 bg-transparent px-2.5 py-2 text-xs text-slate-800 placeholder:text-slate-400 focus:outline-none" />

            <button type="submit"
              class="shrink-0 px-3.5 py-2 rounded-lg bg-gradient-to-r from-blue-600 to-cyan-500 text-white text-xs font-bold shadow-sm shadow-blue-500/20 hover:shadow-md hover:shadow-blue-500/25 hover:from-blue-700 hover:to-cyan-600 transition-all">
              Join
            </button>
          </form>

          <div class="flex items-center gap-2 mt-4 text-[10px] text-slate-400">
            <svg class="w-3.5 h-3.5 text-emerald-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>

            No spam. Unsubscribe anytime.
          </div>
        </div>

      </div>

      <!-- ================= FOOTER BOTTOM ================= -->
      <div class="border-t border-slate-200 pt-6 flex flex-col md:flex-row justify-between items-center gap-4">

        <p class="text-[11px] text-slate-400 text-center md:text-left">
          © {{ year }} NexusRigs Inc. All hardware systems operational.
        </p>

        <div class="flex items-center gap-5">

          <a href="#" class="text-[11px] font-medium text-slate-400 hover:text-slate-700 transition-colors">
            Privacy Policy
          </a>

          <a href="#" class="text-[11px] font-medium text-slate-400 hover:text-slate-700 transition-colors">
            Terms of Service
          </a>

          <span class="hidden sm:block w-px h-3 bg-slate-300"></span>

          <span class="flex items-center gap-1.5 text-[10px] font-semibold text-slate-400">
            <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>

            System Online
          </span>

        </div>

      </div>

    </div>
  </footer>
</template>