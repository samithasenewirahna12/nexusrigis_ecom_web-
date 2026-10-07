<script setup lang="ts">
import {
  ref,
  computed,
  onMounted,
  onBeforeUnmount,
  watch
} from 'vue'
import { useRouter } from 'vue-router'
import api from '../../services/api'
import { useCartStore } from '../../stores/cartStore'
import { useNotificationStore } from '../../stores/notificationStore'
import type { Product } from '../../components/product/ProductCard.vue'
import backgroundImage from '../../assets/images/vecteezy_abstract-blur-shopping-mall_2795585.jpg'
import {
  Flame,
  Zap,
  Tag,
  Clock,
  ShoppingCart,
  CheckCircle2,
  Eye,
  Star,
  Search,
  ArrowRight,
  ShieldCheck,
  Package,
  Layers,
  X,
  Sparkles,
  Percent
} from 'lucide-vue-next'

/* =========================================================
   TYPES
========================================================= */

interface DealProduct extends Product {
  originalPrice: number
  discountPercentage: number
  badgeText: string
  endDate?: string
  features?: string[]
}

const router = useRouter()
const cartStore = useCartStore()
const notificationStore = useNotificationStore()

/* =========================================================
   STATE
========================================================= */

const activeTab = ref<'all' | 'flash' | 'clearance' | 'bundles'>('all')
const selectedCategory = ref<string>('All')
const searchQuery = ref('')
const sortBy = ref<'highest' | 'price-low' | 'price-high'>('highest')
const isLoading = ref(false)
const addingToCartId = ref<string | number | null | undefined>(null)

const selectedDeal = ref<DealProduct | null>(null)
const showDealModal = ref(false)

/* =========================================================
   COUNTDOWN TIMER (HOURS, MINUTES, SECONDS)
========================================================= */

const remainingTime = ref({
  hours: 14,
  minutes: 38,
  seconds: 42
})

let countdownTimer: ReturnType<typeof setInterval> | null = null

const startCountdown = () => {
  countdownTimer = setInterval(() => {
    if (remainingTime.value.seconds > 0) {
      remainingTime.value.seconds--
      return
    }
    remainingTime.value.seconds = 59
    if (remainingTime.value.minutes > 0) {
      remainingTime.value.minutes--
      return
    }
    remainingTime.value.minutes = 59
    if (remainingTime.value.hours > 0) {
      remainingTime.value.hours--
      return
    }
    remainingTime.value = {
      hours: 23,
      minutes: 59,
      seconds: 59
    }
  }, 1000)
}

/* =========================================================
   CURATED BASELINE HARDWARE DEALS
========================================================= */

const fallbackDeals: DealProduct[] = [
  {
    id: 'deal-gpu-1',
    productId: 'P005',
    name: 'ASUS ROG Strix GeForce RTX 4080 Super 16GB OC',
    category: 'Graphics Cards',
    price: 319900,
    originalPrice: 399900,
    discountPercentage: 20,
    image: 'https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=600&q=80',
    rating: 4.9,
    reviewCount: 42,
    inStock: true,
    badgeText: 'FLASH DEAL',
    features: ['16GB GDDR6X', 'Ada Lovelace Architecture', 'Triple Axial-tech Fans', 'Aura Sync RGB']
  },
  {
    id: 'deal-cpu-1',
    productId: 'cpu-7800x3d',
    name: 'AMD Ryzen 7 7800X3D 8-Core 16-Thread Processor',
    category: 'Processors',
    price: 149900,
    originalPrice: 182900,
    discountPercentage: 18,
    image: 'https://images.unsplash.com/photo-1555680202-c86f0e12f086?w=600&q=80',
    rating: 5.0,
    reviewCount: 88,
    inStock: true,
    badgeText: 'HOT OFFER',
    features: ['3D V-Cache Technology', 'Up to 5.0 GHz Boost', '104MB Total Cache', 'AM5 Socket']
  },
  {
    id: 'deal-ram-1',
    productId: 'deal-ram-1',
    name: 'Corsair Dominator Titanium RGB 32GB (2x16GB) DDR5 6000MHz',
    category: 'Memory',
    price: 58900,
    originalPrice: 79900,
    discountPercentage: 26,
    image: 'https://images.unsplash.com/photo-1562976540-1502c2145186?w=600&q=80',
    rating: 4.8,
    reviewCount: 29,
    inStock: true,
    badgeText: 'CLEARANCE',
    features: ['CL30 Ultra Low Latency', 'Patented DHX Cooling', 'iCUE Compatible', 'Intel XMP / AMD EXPO']
  },
  {
    id: 'deal-ssd-1',
    productId: 'deal-ssd-1',
    name: 'Samsung 990 PRO 2TB PCIe 4.0 NVMe M.2 Internal SSD',
    category: 'Storage',
    price: 64500,
    originalPrice: 94900,
    discountPercentage: 32,
    image: 'https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?w=600&q=80',
    rating: 4.9,
    reviewCount: 65,
    inStock: true,
    badgeText: 'FLASH DEAL',
    features: ['7450 MB/s Read', '6900 MB/s Write', 'Nickel-coated Controller', 'Thermal Guard']
  },
  {
    id: 'deal-aio-1',
    productId: 'deal-aio-1',
    name: 'Lian Li Galahad II LCD 360mm AIO Liquid Cooler',
    category: 'Cooling',
    price: 69900,
    originalPrice: 92000,
    discountPercentage: 24,
    image: 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=600&q=80',
    rating: 4.7,
    reviewCount: 19,
    inStock: true,
    badgeText: 'BUNDLE SAVER',
    features: ['2.88" IPS LCD Display', 'Asetek 8th Gen Pump', 'Daisy-chain ARGB Fans', 'Zero RPM Mode']
  },
  {
    id: 'deal-case-1',
    productId: 'deal-case-1',
    name: 'Lian Li O11 Dynamic EVO RGB Dual-Chamber Mid-Tower Case',
    category: 'Chassis',
    price: 49900,
    originalPrice: 64900,
    discountPercentage: 23,
    image: 'https://images.unsplash.com/photo-1587202372634-32705e3bf49c?w=600&q=80',
    rating: 4.9,
    reviewCount: 54,
    inStock: true,
    badgeText: 'HOT OFFER',
    features: ['Panoramic Glass', 'Dual-Direction RGB Strips', 'Supports 3x 360mm Radiators', 'Modular Layout']
  }
]

const deals = ref<DealProduct[]>([...fallbackDeals])

/* =========================================================
   FETCH DEALS FROM BACKEND
========================================================= */

const fetchDeals = async () => {
  isLoading.value = true
  try {
    const { data } = await api.get('/deals')
    const list = Array.isArray(data) ? data : data?.content || []

    if (list.length > 0) {
      const mappedDeals = list.map((deal: any) => {
        let originalPrice = Number(deal.productOriginalPrice || 0)
        let discountPercentage = Number(deal.discountPercentage || 0)

        // Convert mock USD numbers into realistic LKR if needed
        if (originalPrice > 0 && originalPrice < 5000) {
          originalPrice = Math.round(originalPrice * 310)
        }

        const discountedPrice = discountPercentage > 0
          ? Math.round(originalPrice * (1 - discountPercentage / 100))
          : originalPrice

        return {
          id: deal.productId || deal.dealId,
          productId: String(deal.productId || deal.dealId),
          name: deal.productName || 'Enthusiast Hardware Component',
          category: 'Hardware Deals',
          price: discountedPrice > 0 ? discountedPrice : originalPrice,
          originalPrice: originalPrice > 0 ? originalPrice : Math.round(discountedPrice * 1.25),
          discountPercentage: discountPercentage > 0 ? discountPercentage : 15,
          image: deal.productImage || 'https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=600&q=80',
          rating: 4.9,
          reviewCount: 38,
          inStock: true,
          badgeText: deal.badgeText || 'FLASH DEAL',
          endDate: deal.endDate
        }
      })
      deals.value = [...mappedDeals, ...fallbackDeals]
    } else {
      deals.value = [...fallbackDeals]
    }
  } catch (error) {
    console.warn('Backend /deals unavailable, using curated baseline hardware deals:', error)
    deals.value = [...fallbackDeals]
  } finally {
    isLoading.value = false
  }
}

/* =========================================================
   FILTER & SORT
========================================================= */

const categories = ['All', 'Graphics Cards', 'Processors', 'Memory', 'Storage', 'Cooling', 'Chassis']

const filteredDeals = computed(() => {
  return deals.value.filter(deal => {
    // 1. Tab filter
    if (activeTab.value === 'flash' && !(deal.badgeText === 'FLASH DEAL' || deal.badgeText === 'HOT OFFER')) {
      return false
    }
    if (activeTab.value === 'clearance' && deal.badgeText !== 'CLEARANCE') {
      return false
    }
    if (activeTab.value === 'bundles' && deal.badgeText !== 'BUNDLE SAVER') {
      return false
    }

    // 2. Category filter
    if (selectedCategory.value !== 'All' && deal.category !== selectedCategory.value) {
      return false
    }

    // 3. Search query
    if (searchQuery.value.trim()) {
      const q = searchQuery.value.toLowerCase().trim()
      const matchName = deal.name.toLowerCase().includes(q)
      const matchBadge = deal.badgeText.toLowerCase().includes(q)
      if (!matchName && !matchBadge) return false
    }

    return true
  }).sort((a, b) => {
    if (sortBy.value === 'highest') {
      return (b.discountPercentage || 0) - (a.discountPercentage || 0)
    }
    if (sortBy.value === 'price-low') {
      return a.price - b.price
    }
    if (sortBy.value === 'price-high') {
      return b.price - a.price
    }
    return 0
  })
})

const maxDiscountPercent = computed(() => {
  if (deals.value.length === 0) return 35
  return Math.max(...deals.value.map(d => d.discountPercentage || 0))
})

/* =========================================================
   ACTIONS
========================================================= */

const handleAddToCart = async (product: DealProduct) => {
  addingToCartId.value = product.id
  try {
    const targetId = String(product.productId || product.id)
    
    // Register deal originalPrice so cart and checkout show the discount
    try {
      const registry = JSON.parse(localStorage.getItem('deal_registry') || '{}')
      registry[targetId] = {
        productId: targetId,
        name: product.name,
        price: product.price,
        originalPrice: product.originalPrice,
        discountPercentage: product.discountPercentage
      }
      localStorage.setItem('deal_registry', JSON.stringify(registry))
    } catch {}

    await cartStore.addItem(targetId, 1)
  } catch {
    // Cart store dispatches its own alerts
  } finally {
    addingToCartId.value = null
  }
}

const openDealDetails = (deal: DealProduct) => {
  selectedDeal.value = deal
  showDealModal.value = true
}

const formatPrice = (val: number) => {
  return new Intl.NumberFormat('en-LK', {
    style: 'currency',
    currency: 'LKR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(val)
}

onMounted(() => {
  fetchDeals()
  startCountdown()
})

onBeforeUnmount(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
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
        <div class="relative grid grid-cols-1 lg:grid-cols-[1fr_auto] gap-8 items-center">

          <!-- Left Info -->
          <div class="max-w-3xl">
            <div class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-blue-50 border border-blue-200/80 text-blue-700 text-xs font-bold mb-4">
              <span class="w-2 h-2 rounded-full bg-blue-600 animate-ping"></span>
              <span>Save Up to {{ maxDiscountPercent }}% OFF Hardware Drops</span>
            </div>

            <h1 class="text-3xl sm:text-4xl lg:text-5xl font-black text-slate-950 tracking-tight leading-tight">
              High-Performance <span class="bg-gradient-to-r from-blue-600 to-cyan-500 bg-clip-text text-transparent">Hardware Deals</span>.
            </h1>

            <p class="mt-4 text-sm sm:text-base text-slate-500 leading-relaxed max-w-2xl">
              Exclusive promotional pricing on flagship GPUs, multi-core CPUs, DDR5 memory kits, and liquid cooling gear. Limited inventory allocation per account.
            </p>

            <div class="mt-6 flex flex-wrap items-center gap-3">
              <div class="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-blue-50/80 border border-blue-100 text-xs font-bold text-blue-700">
                <Flame class="w-4 h-4 text-orange-500" />
                <span>{{ deals.length }} Active Drops</span>
              </div>

              <div class="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-slate-50 border border-slate-200 text-xs font-semibold text-slate-600">
                <ShieldCheck class="w-4 h-4 text-emerald-600" />
                <span>Full Manufacturer Warranty</span>
              </div>
            </div>
          </div>

          <!-- Right Countdown Card (LIGHT THEME - NO DARK THEME) -->
          <div class="rounded-2xl border border-white/90 bg-gradient-to-br from-blue-50/90 to-cyan-50/70 p-6 shadow-xl shadow-blue-500/10 min-w-[280px]">
            <div class="flex items-center justify-between mb-3">
              <span class="text-[10px] font-black uppercase tracking-widest text-slate-400">Flash Event Ends In</span>
              <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
            </div>

            <!-- Ticker boxes (Frosted white, no dark mode) -->
            <div class="flex items-center justify-center gap-2">

              <div class="min-w-[56px] rounded-xl border border-slate-200 bg-white/90 p-2.5 text-center shadow-sm">
                <span class="font-mono text-xl font-black text-blue-600">{{ String(remainingTime.hours).padStart(2, '0') }}</span>
                <p class="text-[8px] font-bold uppercase tracking-wider text-slate-400 mt-0.5">Hours</p>
              </div>

              <span class="font-black text-slate-400">:</span>

              <div class="min-w-[56px] rounded-xl border border-slate-200 bg-white/90 p-2.5 text-center shadow-sm">
                <span class="font-mono text-xl font-black text-blue-600">{{ String(remainingTime.minutes).padStart(2, '0') }}</span>
                <p class="text-[8px] font-bold uppercase tracking-wider text-slate-400 mt-0.5">Minutes</p>
              </div>

              <span class="font-black text-slate-400">:</span>

              <div class="min-w-[56px] rounded-xl border border-slate-200 bg-white/90 p-2.5 text-center shadow-sm">
                <span class="font-mono text-xl font-black text-cyan-600">{{ String(remainingTime.seconds).padStart(2, '0') }}</span>
                <p class="text-[8px] font-bold uppercase tracking-wider text-slate-400 mt-0.5">Seconds</p>
              </div>

            </div>

            <p class="text-center text-[10px] font-medium text-slate-500 mt-4">
              Discounts refresh automatically upon countdown expiration.
            </p>
          </div>

        </div>
      </section>

      <!-- ===================================================
           FILTER & SEARCH ROW
      ==================================================== -->
      <section class="mb-6 flex flex-col md:flex-row gap-4 items-stretch md:items-center justify-between">

        <!-- TABS (ACTIVE PILL STYLE) -->
        <div class="flex items-center gap-1.5 p-1.5 rounded-2xl bg-white/80 border border-white/90 shadow-lg shadow-slate-200/30 backdrop-blur-xl overflow-x-auto scrollbar-hide">
          <button
            @click="activeTab = 'all'"
            :class="[
              'px-4 py-2.5 rounded-xl text-xs font-bold transition whitespace-nowrap',
              activeTab === 'all'
                ? 'bg-blue-600 text-white shadow-md shadow-blue-500/30'
                : 'text-slate-600 hover:text-blue-600 hover:bg-blue-50/60'
            ]"
          >
            All Hardware Drops
          </button>

          <button
            @click="activeTab = 'flash'"
            :class="[
              'px-4 py-2.5 rounded-xl text-xs font-bold transition whitespace-nowrap',
              activeTab === 'flash'
                ? 'bg-blue-600 text-white shadow-md shadow-blue-500/30'
                : 'text-slate-600 hover:text-blue-600 hover:bg-blue-50/60'
            ]"
          >
            ⚡ Flash Sales
          </button>

          <button
            @click="activeTab = 'clearance'"
            :class="[
              'px-4 py-2.5 rounded-xl text-xs font-bold transition whitespace-nowrap',
              activeTab === 'clearance'
                ? 'bg-blue-600 text-white shadow-md shadow-blue-500/30'
                : 'text-slate-600 hover:text-blue-600 hover:bg-blue-50/60'
            ]"
          >
            Clearance Specials
          </button>

          <button
            @click="activeTab = 'bundles'"
            :class="[
              'px-4 py-2.5 rounded-xl text-xs font-bold transition whitespace-nowrap',
              activeTab === 'bundles'
                ? 'bg-blue-600 text-white shadow-md shadow-blue-500/30'
                : 'text-slate-600 hover:text-blue-600 hover:bg-blue-50/60'
            ]"
          >
            Bundle Savers
          </button>
        </div>

        <!-- SEARCH & SORT -->
        <div class="flex flex-wrap items-center gap-3">
          <div class="relative min-w-[220px]">
            <Search class="absolute left-3.5 top-3 w-4 h-4 text-slate-400" />
            <input
              v-model="searchQuery"
              type="text"
              placeholder="Search deals..."
              class="w-full pl-10 pr-4 py-2.5 rounded-2xl bg-white/80 border border-slate-200/90 text-xs font-medium text-slate-900 placeholder:text-slate-400 shadow-sm outline-none focus:bg-white focus:border-blue-500 transition"
            />
          </div>

          <select
            v-model="sortBy"
            class="px-4 py-2.5 rounded-2xl bg-white/80 border border-slate-200/90 text-xs font-bold text-slate-800 shadow-sm outline-none transition hover:bg-white"
          >
            <option value="highest">Biggest Savings (%)</option>
            <option value="price-low">Price: Low to High</option>
            <option value="price-high">Price: High to Low</option>
          </select>
        </div>

      </section>

      <!-- CATEGORY CHIPS -->
      <div class="mb-8 flex items-center gap-2 overflow-x-auto scrollbar-hide py-1">
        <button
          v-for="cat in categories"
          :key="cat"
          @click="selectedCategory = cat"
          :class="[
            'px-4 py-2 rounded-xl text-xs font-bold transition whitespace-nowrap border',
            selectedCategory === cat
              ? 'bg-white border-blue-500 text-blue-600 shadow-sm'
              : 'bg-white/60 border-slate-200/80 text-slate-600 hover:bg-white hover:text-slate-900'
          ]"
        >
          {{ cat }}
        </button>
      </div>

      <!-- ===================================================
           DEAL CARDS GRID (NO TOP COLORED LINE ACCENTS)
      ==================================================== -->
      <section v-if="filteredDeals.length > 0" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">

        <article
          v-for="product in filteredDeals"
          :key="product.id"
          class="group relative flex flex-col justify-between rounded-3xl border border-white/90 bg-white/85 p-5 sm:p-6 shadow-xl shadow-slate-200/25 backdrop-blur-xl transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl hover:shadow-blue-500/10 overflow-hidden"
        >
          <div>
            <!-- Top Badges -->
            <div class="flex items-center justify-between gap-2 mb-3">
              <span
                :class="[
                  'px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-wider',
                  product.badgeText === 'FLASH DEAL' ? 'bg-amber-50 text-amber-700 border border-amber-200' :
                  product.badgeText === 'CLEARANCE' ? 'bg-purple-50 text-purple-700 border border-purple-200' :
                  'bg-blue-50 text-blue-700 border border-blue-200'
                ]"
              >
                {{ product.badgeText }}
              </span>

              <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl bg-red-50 border border-red-200 text-red-700 text-xs font-black">
                -{{ Math.round(product.discountPercentage) }}% OFF
              </span>
            </div>

            <!-- Product Image -->
            <div
              @click="openDealDetails(product)"
              class="relative h-52 w-full rounded-2xl bg-slate-50 flex items-center justify-center p-4 cursor-pointer overflow-hidden group/img mb-4"
            >
              <img
                :src="product.image"
                :alt="product.name"
                class="h-full w-full object-contain transition-transform duration-300 group-hover/img:scale-105"
              />
              <div class="absolute inset-0 bg-blue-600/5 opacity-0 group-hover/img:opacity-100 transition-opacity"></div>
            </div>

            <!-- Category & Title -->
            <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
              {{ product.category }}
            </p>

            <h3
              @click="openDealDetails(product)"
              class="mt-1 text-base sm:text-lg font-black text-slate-900 group-hover:text-blue-600 transition leading-snug cursor-pointer line-clamp-2"
            >
              {{ product.name }}
            </h3>

            <!-- Rating snippet -->
            <div class="mt-2 flex items-center gap-1.5 text-xs text-slate-500">
              <div class="flex items-center text-amber-400">
                <Star class="w-3.5 h-3.5 fill-current" />
              </div>
              <span class="font-bold text-slate-700">{{ product.rating || 4.9 }}</span>
              <span class="text-slate-400">({{ product.reviewCount || 24 }})</span>
            </div>
          </div>

          <!-- Pricing & Actions -->
          <div class="mt-5 pt-4 border-t border-slate-100">
            <div class="flex items-baseline gap-2 mb-3">
              <span class="text-2xl font-black text-slate-950">{{ formatPrice(product.price) }}</span>
              <span class="text-sm font-semibold text-slate-400 line-through">{{ formatPrice(product.originalPrice) }}</span>
            </div>

            <div class="grid grid-cols-2 gap-2">
              <button
                @click="openDealDetails(product)"
                class="px-3 py-2.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 text-xs font-bold transition flex items-center justify-center gap-1.5"
              >
                <Eye class="w-3.5 h-3.5 text-slate-500" />
                <span>Quick View</span>
              </button>

              <button
                @click="handleAddToCart(product)"
                :disabled="addingToCartId === product.id"
                class="px-3 py-2.5 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 text-white text-xs font-bold shadow-md shadow-blue-500/25 hover:shadow-blue-500/40 hover:-translate-y-0.5 transition flex items-center justify-center gap-1.5"
              >
                <ShoppingCart class="w-3.5 h-3.5" />
                <span>{{ addingToCartId === product.id ? 'Adding...' : 'Add to Cart' }}</span>
              </button>
            </div>
          </div>

        </article>

      </section>

      <!-- EMPTY STATE -->
      <section
        v-else
        class="rounded-3xl border border-white/90 bg-white/80 p-12 text-center shadow-xl shadow-slate-200/20 backdrop-blur-xl"
      >
        <Package class="w-12 h-12 text-slate-400 mx-auto mb-3" />
        <h3 class="text-lg font-black text-slate-900">No Hardware Deals Found</h3>
        <p class="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
          No hardware discounts match your current category or search filters.
        </p>
        <button
          @click="activeTab = 'all'; selectedCategory = 'All'; searchQuery = ''"
          class="mt-4 px-4 py-2 rounded-xl bg-blue-600 text-white text-xs font-bold shadow-md shadow-blue-500/20"
        >
          Reset Filters
        </button>
      </section>

    </main>

    <!-- ===================================================
         QUICK VIEW DEAL MODAL
    ==================================================== -->
    <div
      v-if="showDealModal && selectedDeal"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/40 backdrop-blur-sm"
      @click.self="showDealModal = false"
    >
      <div class="relative w-full max-w-2xl rounded-3xl border border-white/90 bg-white p-6 sm:p-8 shadow-2xl shadow-slate-400/30 overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        <button
          @click="showDealModal = false"
          class="absolute top-5 right-5 p-2 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition"
        >
          <X class="w-5 h-5" />
        </button>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-6 items-center">
          <div class="h-64 rounded-2xl bg-slate-50 p-4 flex items-center justify-center">
            <img :src="selectedDeal.image" :alt="selectedDeal.name" class="h-full w-full object-contain" />
          </div>

          <div>
            <div class="flex items-center gap-2 mb-2">
              <span class="px-2.5 py-0.5 rounded-full bg-blue-50 text-blue-700 text-[10px] font-black uppercase">
                {{ selectedDeal.badgeText }}
              </span>
              <span class="text-xs text-slate-400 font-semibold">{{ selectedDeal.category }}</span>
            </div>

            <h3 class="text-xl font-black text-slate-900 leading-snug">
              {{ selectedDeal.name }}
            </h3>

            <div class="mt-4 flex items-baseline gap-3">
              <span class="text-3xl font-black text-blue-600">{{ formatPrice(selectedDeal.price) }}</span>
              <span class="text-base text-slate-400 line-through">{{ formatPrice(selectedDeal.originalPrice) }}</span>
              <span class="px-2 py-0.5 rounded-lg bg-red-100 text-red-700 font-black text-xs">
                Save {{ Math.round(selectedDeal.discountPercentage) }}%
              </span>
            </div>

            <div v-if="selectedDeal.features && selectedDeal.features.length > 0" class="mt-4 space-y-1.5">
              <div v-for="(feat, idx) in selectedDeal.features" :key="idx" class="flex items-center gap-2 text-xs text-slate-600">
                <CheckCircle2 class="w-3.5 h-3.5 text-blue-600 shrink-0" />
                <span>{{ feat }}</span>
              </div>
            </div>

            <div class="mt-6 flex items-center gap-3">
              <button
                @click="handleAddToCart(selectedDeal); showDealModal = false"
                class="flex-1 py-3 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 text-white text-xs font-bold shadow-md shadow-blue-500/25 hover:shadow-blue-500/40 hover:-translate-y-0.5 transition flex items-center justify-center gap-2"
              >
                <ShoppingCart class="w-4 h-4" />
                <span>Add to Cart</span>
              </button>
              <button
                @click="router.push(`/product/${selectedDeal.productId || selectedDeal.id}`)"
                class="px-4 py-3 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-bold transition"
              >
                Full Specs
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.scrollbar-hide {
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.scrollbar-hide::-webkit-scrollbar {
  display: none;
}
</style>