<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../../services/api'
import { useCartStore } from '../../stores/cartStore'
import backgroundImage from '../../assets/images/vecteezy_abstract-blur-shopping-mall_2795585.jpg'
import {
  Scale,
  Cpu,
  Monitor,
  Zap,
  Flame,
  CheckCircle2,
  X,
  ShoppingCart,
  Wrench,
  ChevronRight,
  Sparkles,
  Layers,
  ArrowRight,
  ExternalLink,
  ShieldCheck,
  BarChart3,
  Search,
  Plus
} from 'lucide-vue-next'

/* =========================================================
   TYPES
========================================================= */

interface ComponentSpec {
  id: string
  name: string
  brand: string
  category: 'GPU' | 'CPU' | 'RAM' | 'SSD'
  price: number
  image: string
  inStock: boolean
  specs: Record<string, string | number>
  benchmarks: {
    game: string
    fps1080p: number
    fps1440p: number
    fps4k: number
  }[]
  tdp: number
  recommendedPsu: number
}

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

/* =========================================================
   BENCHMARK DATABASE
========================================================= */

const sampleComponents: ComponentSpec[] = [
  // GPUs
  {
    id: 'gpu-4090',
    name: 'NVIDIA GeForce RTX 4090 24GB',
    brand: 'NVIDIA',
    category: 'GPU',
    price: 625000,
    image: 'https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=500&q=80',
    inStock: true,
    specs: {
      'Architecture': 'Ada Lovelace (4nm)',
      'CUDA Cores': 16384,
      'Base Clock': '2235 MHz',
      'Boost Clock': '2520 MHz',
      'VRAM': '24 GB GDDR6X',
      'Memory Bus': '384-bit',
      'Ray Tracing Cores': '3rd Gen (128 Cores)',
      'Tensor Cores': '4th Gen (512 Cores)'
    },
    benchmarks: [
      { game: 'Cyberpunk 2077 (RT Ultra)', fps1080p: 165, fps1440p: 128, fps4k: 82 },
      { game: 'Black Myth: Wukong (Cinematic)', fps1080p: 180, fps1440p: 140, fps4k: 90 },
      { game: 'Call of Duty: MW III', fps1080p: 310, fps1440p: 245, fps4k: 160 },
      { game: 'Valorant (High)', fps1080p: 620, fps1440p: 540, fps4k: 410 }
    ],
    tdp: 450,
    recommendedPsu: 850
  },
  {
    id: 'gpu-4080s',
    name: 'NVIDIA GeForce RTX 4080 Super 16GB',
    brand: 'NVIDIA',
    category: 'GPU',
    price: 329000,
    image: 'https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=500&q=80',
    inStock: true,
    specs: {
      'Architecture': 'Ada Lovelace (4nm)',
      'CUDA Cores': 10240,
      'Base Clock': '2295 MHz',
      'Boost Clock': '2550 MHz',
      'VRAM': '16 GB GDDR6X',
      'Memory Bus': '256-bit',
      'Ray Tracing Cores': '3rd Gen (80 Cores)',
      'Tensor Cores': '4th Gen (320 Cores)'
    },
    benchmarks: [
      { game: 'Cyberpunk 2077 (RT Ultra)', fps1080p: 138, fps1440p: 104, fps4k: 64 },
      { game: 'Black Myth: Wukong (Cinematic)', fps1080p: 152, fps1440p: 115, fps4k: 72 },
      { game: 'Call of Duty: MW III', fps1080p: 260, fps1440p: 202, fps4k: 132 },
      { game: 'Valorant (High)', fps1080p: 560, fps1440p: 480, fps4k: 360 }
    ],
    tdp: 320,
    recommendedPsu: 750
  },
  {
    id: 'gpu-7900xtx',
    name: 'AMD Radeon RX 7900 XTX 24GB',
    brand: 'AMD',
    category: 'GPU',
    price: 305000,
    image: 'https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?w=500&q=80',
    inStock: true,
    specs: {
      'Architecture': 'RDNA 3 (5nm + 6nm)',
      'Stream Processors': 6144,
      'Base Clock': '1900 MHz',
      'Boost Clock': '2500 MHz',
      'VRAM': '24 GB GDDR6',
      'Memory Bus': '384-bit',
      'Ray Accelerators': '2nd Gen (96 Cores)',
      'Infinity Cache': '96 MB'
    },
    benchmarks: [
      { game: 'Cyberpunk 2077 (RT Ultra)', fps1080p: 110, fps1440p: 82, fps4k: 48 },
      { game: 'Black Myth: Wukong (Cinematic)', fps1080p: 142, fps1440p: 108, fps4k: 68 },
      { game: 'Call of Duty: MW III', fps1080p: 285, fps1440p: 220, fps4k: 145 },
      { game: 'Valorant (High)', fps1080p: 580, fps1440p: 510, fps4k: 390 }
    ],
    tdp: 355,
    recommendedPsu: 800
  },

  // CPUs
  {
    id: 'cpu-7800x3d',
    name: 'AMD Ryzen 7 7800X3D Processor',
    brand: 'AMD',
    category: 'CPU',
    price: 149900,
    image: 'https://images.unsplash.com/photo-1555680202-c86f0e12f086?w=500&q=80',
    inStock: true,
    specs: {
      'Architecture': 'Zen 4 (5nm)',
      'Cores / Threads': '8 Cores / 16 Threads',
      'Base Clock': '4.2 GHz',
      'Boost Clock': '5.0 GHz',
      'L3 Cache': '96 MB 3D V-Cache',
      'Socket': 'AM5',
      'PCIe Support': 'PCIe 5.0',
      'Memory Support': 'DDR5-5200'
    },
    benchmarks: [
      { game: 'Cyberpunk 2077 (RT Ultra)', fps1080p: 175, fps1440p: 135, fps4k: 85 },
      { game: 'Black Myth: Wukong (Cinematic)', fps1080p: 182, fps1440p: 142, fps4k: 92 },
      { game: 'Call of Duty: MW III', fps1080p: 320, fps1440p: 250, fps4k: 165 },
      { game: 'Valorant (High)', fps1080p: 680, fps1440p: 590, fps4k: 440 }
    ],
    tdp: 120,
    recommendedPsu: 650
  },
  {
    id: 'cpu-14900k',
    name: 'Intel Core i9-14900K Processor',
    brand: 'Intel',
    category: 'CPU',
    price: 179900,
    image: 'https://images.unsplash.com/photo-1555680202-c86f0e12f086?w=500&q=80',
    inStock: true,
    specs: {
      'Architecture': 'Raptor Lake Refresh (Intel 7)',
      'Cores / Threads': '24 Cores (8P + 16E) / 32 Threads',
      'Base Clock': '3.2 GHz (P-core)',
      'Boost Clock': '6.0 GHz (Thermal Velocity)',
      'L3 Cache': '36 MB Intel Smart Cache',
      'Socket': 'LGA 1700',
      'PCIe Support': 'PCIe 5.0 / 4.0',
      'Memory Support': 'DDR5-5600 / DDR4-3200'
    },
    benchmarks: [
      { game: 'Cyberpunk 2077 (RT Ultra)', fps1080p: 168, fps1440p: 132, fps4k: 84 },
      { game: 'Black Myth: Wukong (Cinematic)', fps1080p: 176, fps1440p: 138, fps4k: 90 },
      { game: 'Call of Duty: MW III', fps1080p: 305, fps1440p: 240, fps4k: 160 },
      { game: 'Valorant (High)', fps1080p: 640, fps1440p: 560, fps4k: 420 }
    ],
    tdp: 253,
    recommendedPsu: 750
  }
]

/* =========================================================
   STATE
========================================================= */

const selectedCategory = ref<'GPU' | 'CPU'>('GPU')
const selectedSlot1 = ref<ComponentSpec>(sampleComponents[0])
const selectedSlot2 = ref<ComponentSpec>(sampleComponents[1])
const resolution = ref<'1080p' | '1440p' | '4k'>('1440p')

const categoryComponents = computed(() => {
  return sampleComponents.filter(c => c.category === selectedCategory.value)
})

watch(selectedCategory, (newCat) => {
  const filtered = sampleComponents.filter(c => c.category === newCat)
  if (filtered.length >= 2) {
    selectedSlot1.value = filtered[0]
    selectedSlot2.value = filtered[1]
  }
})

// Check route query
onMounted(() => {
  if (route.query.category === 'cpu') {
    selectedCategory.value = 'CPU'
  }
})

/* =========================================================
   COMPUTED COMPARISON METRICS
========================================================= */

const combinedTdp = computed(() => {
  return (selectedSlot1.value?.tdp || 0) + (selectedSlot2.value?.tdp || 0)
})

const formatPrice = (val: number) => {
  return new Intl.NumberFormat('en-LK', {
    style: 'currency',
    currency: 'LKR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(val)
}

const getFps = (comp: ComponentSpec, gameIdx: number) => {
  const b = comp.benchmarks[gameIdx]
  if (!b) return 0
  if (resolution.value === '1080p') return b.fps1080p
  if (resolution.value === '1440p') return b.fps1440p
  return b.fps4k
}

const handleAddToCart = async (comp: ComponentSpec) => {
  try {
    await cartStore.addItem(comp.id, 1)
  } catch {}
}
</script>

<template>
  <div class="relative min-h-screen bg-slate-100 text-slate-900 pb-20">

    <!-- BACKGROUND TEXTURE (LIGHT & FROSTED) -->
    <div
      class="fixed inset-0 pointer-events-none opacity-[0.22] bg-cover bg-center"
      :style="{ backgroundImage: `url(${backgroundImage})` }"
    ></div>
    <div class="fixed inset-0 pointer-events-none bg-gradient-to-b from-white/90 via-slate-100/95 to-slate-100"></div>

    <!-- AMBIENT GLOW -->
    <div class="pointer-events-none fixed -top-40 right-0 w-96 h-96 rounded-full bg-blue-500/10 blur-3xl"></div>
    <div class="pointer-events-none fixed top-1/2 -left-40 w-96 h-96 rounded-full bg-cyan-400/10 blur-3xl"></div>

    <!-- MAIN CONTAINER -->
    <main class="relative z-10 w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-8">

      <!-- ===================================================
           HERO SECTION
      ==================================================== -->
      <section class="relative overflow-hidden rounded-3xl border border-white/90 bg-white/80 p-6 sm:p-10 lg:p-12 shadow-2xl shadow-slate-300/25 backdrop-blur-2xl mb-8">
        <div class="max-w-3xl">
          <div class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-blue-50 border border-blue-200/80 text-blue-700 text-xs font-bold mb-4">
            <Scale class="w-3.5 h-3.5 text-blue-600" />
            <span>Hardware Benchmark & Comparison Lab</span>
          </div>

          <h1 class="text-3xl sm:text-4xl lg:text-5xl font-black text-slate-950 tracking-tight leading-tight">
            Compare Hardware. <span class="bg-gradient-to-r from-blue-600 to-cyan-500 bg-clip-text text-transparent">Benchmark Performance</span>.
          </h1>

          <p class="mt-4 text-sm sm:text-base text-slate-500 leading-relaxed max-w-2xl">
            Analyze architectural specs, real-world gaming FPS metrics, power draw (TDP), and price-to-performance value side-by-side before you build.
          </p>
        </div>
      </section>

      <!-- ===================================================
           CATEGORY SWITCHER & RESOLUTION SELECTOR
      ==================================================== -->
      <section class="mb-6 flex flex-col sm:flex-row gap-4 items-stretch sm:items-center justify-between">

        <!-- Category switch -->
        <div class="flex items-center gap-1.5 p-1.5 rounded-2xl bg-white/80 border border-white/90 shadow-lg shadow-slate-200/30 backdrop-blur-xl">
          <button
            @click="selectedCategory = 'GPU'"
            :class="[
              'px-5 py-2.5 rounded-xl text-xs font-bold transition flex items-center gap-2',
              selectedCategory === 'GPU'
                ? 'bg-blue-600 text-white shadow-md shadow-blue-500/30'
                : 'text-slate-600 hover:text-blue-600 hover:bg-blue-50/60'
            ]"
          >
            <Monitor class="w-4 h-4" />
            <span>Graphics Cards (GPUs)</span>
          </button>

          <button
            @click="selectedCategory = 'CPU'"
            :class="[
              'px-5 py-2.5 rounded-xl text-xs font-bold transition flex items-center gap-2',
              selectedCategory === 'CPU'
                ? 'bg-blue-600 text-white shadow-md shadow-blue-500/30'
                : 'text-slate-600 hover:text-blue-600 hover:bg-blue-50/60'
            ]"
          >
            <Cpu class="w-4 h-4" />
            <span>Processors (CPUs)</span>
          </button>
        </div>

        <!-- Resolution Pills -->
        <div class="flex items-center gap-2 self-end sm:self-auto">
          <span class="text-xs font-bold text-slate-400 uppercase tracking-wider">Benchmark Resolution:</span>
          <div class="flex items-center gap-1 p-1 rounded-xl bg-white/80 border border-slate-200/90 shadow-sm">
            <button
              v-for="res in (['1080p', '1440p', '4k'] as const)"
              :key="res"
              @click="resolution = res"
              :class="[
                'px-3 py-1.5 rounded-lg text-xs font-bold transition uppercase',
                resolution === res ? 'bg-blue-600 text-white shadow-sm' : 'text-slate-500 hover:text-slate-800'
              ]"
            >
              {{ res }}
            </button>
          </div>
        </div>

      </section>

      <!-- ===================================================
           SIDE-BY-SIDE SELECTION & SUMMARY CARDS
      ==================================================== -->
      <section class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">

        <!-- COMPONENT 1 CARD -->
        <div class="rounded-3xl border border-white/90 bg-white/85 p-6 shadow-xl shadow-slate-200/25 backdrop-blur-xl flex flex-col justify-between">
          <div>
            <div class="flex items-center justify-between mb-4">
              <span class="px-3 py-1 rounded-full bg-blue-50 text-blue-700 text-[10px] font-black uppercase">Component Slot A</span>
              <select
                v-model="selectedSlot1"
                class="px-3 py-1.5 rounded-xl bg-slate-50 border border-slate-200 text-xs font-bold text-slate-800 outline-none"
              >
                <option v-for="c in categoryComponents" :key="c.id" :value="c">{{ c.name }}</option>
              </select>
            </div>

            <div class="flex items-center gap-4 mb-4">
              <div class="w-24 h-24 rounded-2xl bg-slate-50 p-2 flex items-center justify-center shrink-0">
                <img :src="selectedSlot1.image" :alt="selectedSlot1.name" class="h-full w-full object-contain" />
              </div>
              <div>
                <h3 class="text-lg font-black text-slate-900 leading-snug">{{ selectedSlot1.name }}</h3>
                <p class="text-2xl font-black text-blue-600 mt-1">{{ formatPrice(selectedSlot1.price) }}</p>
                <span class="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-600 mt-1">
                  <CheckCircle2 class="w-3.5 h-3.5" /> In Stock & Ready to Ship
                </span>
              </div>
            </div>
          </div>

          <div class="flex items-center gap-2 pt-4 border-t border-slate-100">
            <button
              @click="handleAddToCart(selectedSlot1)"
              class="flex-1 py-2.5 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 text-white text-xs font-bold shadow-md shadow-blue-500/20 hover:shadow-blue-500/40 hover:-translate-y-0.5 transition flex items-center justify-center gap-1.5"
            >
              <ShoppingCart class="w-3.5 h-3.5" />
              <span>Add to Cart</span>
            </button>
            <button
              @click="router.push('/builds')"
              class="px-4 py-2.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 text-xs font-bold transition flex items-center gap-1.5"
            >
              <Wrench class="w-3.5 h-3.5" />
              <span>Build Rig</span>
            </button>
          </div>
        </div>

        <!-- COMPONENT 2 CARD -->
        <div class="rounded-3xl border border-white/90 bg-white/85 p-6 shadow-xl shadow-slate-200/25 backdrop-blur-xl flex flex-col justify-between">
          <div>
            <div class="flex items-center justify-between mb-4">
              <span class="px-3 py-1 rounded-full bg-cyan-50 text-cyan-700 border border-cyan-100 text-[10px] font-black uppercase">Component Slot B</span>
              <select
                v-model="selectedSlot2"
                class="px-3 py-1.5 rounded-xl bg-slate-50 border border-slate-200 text-xs font-bold text-slate-800 outline-none"
              >
                <option v-for="c in categoryComponents" :key="c.id" :value="c">{{ c.name }}</option>
              </select>
            </div>

            <div class="flex items-center gap-4 mb-4">
              <div class="w-24 h-24 rounded-2xl bg-slate-50 p-2 flex items-center justify-center shrink-0">
                <img :src="selectedSlot2.image" :alt="selectedSlot2.name" class="h-full w-full object-contain" />
              </div>
              <div>
                <h3 class="text-lg font-black text-slate-900 leading-snug">{{ selectedSlot2.name }}</h3>
                <p class="text-2xl font-black text-cyan-600 mt-1">{{ formatPrice(selectedSlot2.price) }}</p>
                <span class="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-600 mt-1">
                  <CheckCircle2 class="w-3.5 h-3.5" /> In Stock & Ready to Ship
                </span>
              </div>
            </div>
          </div>

          <div class="flex items-center gap-2 pt-4 border-t border-slate-100">
            <button
              @click="handleAddToCart(selectedSlot2)"
              class="flex-1 py-2.5 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 text-white text-xs font-bold shadow-md shadow-blue-500/20 hover:shadow-blue-500/40 hover:-translate-y-0.5 transition flex items-center justify-center gap-1.5"
            >
              <ShoppingCart class="w-3.5 h-3.5" />
              <span>Add to Cart</span>
            </button>
            <button
              @click="router.push('/builds')"
              class="px-4 py-2.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 text-xs font-bold transition flex items-center gap-1.5"
            >
              <Wrench class="w-3.5 h-3.5" />
              <span>Build Rig</span>
            </button>
          </div>
        </div>

      </section>

      <!-- ===================================================
           GAMING BENCHMARKS PERFORMANCE SECTION
      ==================================================== -->
      <section class="rounded-3xl border border-white/90 bg-white/85 p-6 sm:p-8 shadow-xl shadow-slate-200/25 backdrop-blur-xl mb-8">
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6 pb-4 border-b border-slate-100">
          <div>
            <h3 class="text-lg font-black text-slate-950 flex items-center gap-2">
              <BarChart3 class="w-5 h-5 text-blue-600" />
              <span>Simulated Gaming FPS Benchmarks ({{ resolution.toUpperCase() }})</span>
            </h3>
            <p class="text-xs text-slate-500 mt-0.5">Average framerates rendered under maximum settings with high-speed DDR5 memory.</p>
          </div>
          <div class="flex items-center gap-4 text-xs font-bold">
            <span class="flex items-center gap-1.5 text-blue-600">
              <span class="w-3 h-3 rounded-full bg-blue-600"></span> {{ selectedSlot1.name.slice(0, 20) }}...
            </span>
            <span class="flex items-center gap-1.5 text-cyan-600">
              <span class="w-3 h-3 rounded-full bg-cyan-500"></span> {{ selectedSlot2.name.slice(0, 20) }}...
            </span>
          </div>
        </div>

        <div class="space-y-6">
          <div v-for="(game, idx) in selectedSlot1.benchmarks" :key="game.game" class="space-y-2">
            <div class="flex items-center justify-between text-xs font-bold">
              <span class="text-slate-800">{{ game.game }}</span>
              <div class="flex items-center gap-4 font-mono text-[11px]">
                <span class="text-blue-600 font-black">{{ getFps(selectedSlot1, idx) }} FPS</span>
                <span class="text-slate-300">vs</span>
                <span class="text-cyan-600 font-black">{{ getFps(selectedSlot2, idx) }} FPS</span>
              </div>
            </div>

            <!-- Proportional Dual Progress Bars -->
            <div class="space-y-1">
              <div class="h-3 w-full bg-slate-100 rounded-full overflow-hidden">
                <div
                  class="h-full bg-blue-600 rounded-full transition-all duration-500"
                  :style="{ width: `${Math.min(100, (getFps(selectedSlot1, idx) / 350) * 100)}%` }"
                ></div>
              </div>
              <div class="h-3 w-full bg-slate-100 rounded-full overflow-hidden">
                <div
                  class="h-full bg-cyan-500 rounded-full transition-all duration-500"
                  :style="{ width: `${Math.min(100, (getFps(selectedSlot2, idx) / 350) * 100)}%` }"
                ></div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ===================================================
           DETAILED ARCHITECTURE SPECS TABLE
      ==================================================== -->
      <section class="rounded-3xl border border-white/90 bg-white/85 p-6 sm:p-8 shadow-xl shadow-slate-200/25 backdrop-blur-xl">
        <h3 class="text-lg font-black text-slate-950 mb-4 flex items-center gap-2">
          <Layers class="w-5 h-5 text-blue-600" />
          <span>Technical Specifications Comparison</span>
        </h3>

        <div class="overflow-x-auto">
          <table class="w-full text-left text-xs border-collapse">
            <thead>
              <tr class="border-b border-slate-200/80 text-slate-400 uppercase text-[10px] font-black">
                <th class="py-3 px-4">Specification Parameter</th>
                <th class="py-3 px-4 text-blue-700 bg-blue-50/50 rounded-tl-xl">{{ selectedSlot1.name }}</th>
                <th class="py-3 px-4 text-cyan-700 bg-cyan-50/50 rounded-tr-xl">{{ selectedSlot2.name }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100 font-medium text-slate-700">
              <tr v-for="(val1, key) in selectedSlot1.specs" :key="key" class="hover:bg-slate-50/60 transition-colors">
                <td class="py-3.5 px-4 font-bold text-slate-900">{{ key }}</td>
                <td class="py-3.5 px-4 bg-blue-50/20 font-semibold text-slate-800">{{ val1 }}</td>
                <td class="py-3.5 px-4 bg-cyan-50/20 font-semibold text-slate-800">{{ selectedSlot2.specs[key] || 'N/A' }}</td>
              </tr>
              <!-- Power draw -->
              <tr class="hover:bg-slate-50/60">
                <td class="py-3.5 px-4 font-bold text-slate-900">Thermal Design Power (TDP)</td>
                <td class="py-3.5 px-4 bg-blue-50/20 font-black text-blue-600">{{ selectedSlot1.tdp }} Watts</td>
                <td class="py-3.5 px-4 bg-cyan-50/20 font-black text-cyan-600">{{ selectedSlot2.tdp }} Watts</td>
              </tr>
              <!-- Recommended PSU -->
              <tr class="hover:bg-slate-50/60">
                <td class="py-3.5 px-4 font-bold text-slate-900">Minimum Recommended PSU</td>
                <td class="py-3.5 px-4 bg-blue-50/20 font-black text-slate-800">{{ selectedSlot1.recommendedPsu }}W Gold</td>
                <td class="py-3.5 px-4 bg-cyan-50/20 font-black text-slate-800">{{ selectedSlot2.recommendedPsu }}W Gold</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

    </main>

  </div>
</template>
