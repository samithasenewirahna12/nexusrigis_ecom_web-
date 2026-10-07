<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../../services/api'
import backgroundImage from '../../assets/images/vecteezy_abstract-blur-shopping-mall_2795585.jpg'
import {
  Tag,
  Percent,
  Clock,
  Sparkles,
  Bookmark,
  Check,
  Copy,
  ExternalLink,
  Search,
  ArrowRight,
  ShieldCheck,
  Zap,
  Flame,
  ShoppingBag,
  X,
  Layers,
  Award,
  Cpu,
  Monitor,
  Headphones,
  CheckCircle2,
  Gift
} from 'lucide-vue-next'

/* =========================================================
   TYPES
========================================================= */

export interface OfferItem {
  id: string | number
  dealId?: string
  productId?: string
  code?: string
  tag: string
  title: string
  description: string
  discount: string
  discountPercentage?: number
  validity: string
  expiryDate?: string
  category: string
  minSpend?: string
  type: 'flash' | 'personal' | 'bundle' | 'general'
  isExpiringSoon?: boolean
  isPersonal?: boolean
  terms?: string[]
}

/* =========================================================
   ROUTER & ROUTE QUERY INTEGRATION
========================================================= */

const route = useRoute()
const router = useRouter()

type TabType = 'available' | 'personal' | 'saved' | 'expiring'
const activeTab = ref<TabType>('available')

/* Sync with Navbar Query Parameters:
   - /myOffers                -> 'available'
   - /myOffers?saved=true     -> 'saved'
   - /myOffers?status=expiring-> 'expiring'
   - /myOffers?type=personal  -> 'personal'
*/
const syncTabFromRoute = () => {
  if (route.query.saved === 'true') {
    activeTab.value = 'saved'
  } else if (route.query.status === 'expiring') {
    activeTab.value = 'expiring'
  } else if (route.query.type === 'personal') {
    activeTab.value = 'personal'
  } else {
    activeTab.value = 'available'
  }
}

watch(() => route.query, syncTabFromRoute)

const setTab = (tab: TabType) => {
  activeTab.value = tab
  if (tab === 'saved') {
    router.replace({ path: '/myOffers', query: { saved: 'true' } })
  } else if (tab === 'expiring') {
    router.replace({ path: '/myOffers', query: { status: 'expiring' } })
  } else if (tab === 'personal') {
    router.replace({ path: '/myOffers', query: { type: 'personal' } })
  } else {
    router.replace({ path: '/myOffers' })
  }
}

/* =========================================================
   STATE & DATA
========================================================= */

const isLoading = ref(false)
const searchQuery = ref('')
const selectedCategory = ref('All')
const sortBy = ref<'highest' | 'ending' | 'newest'>('highest')
const copiedCode = ref('')
const selectedOffer = ref<OfferItem | null>(null)
const showOfferModal = ref(false)
const toastMessage = ref('')

// Saved offers persisted in localStorage
const savedOfferIds = ref<Set<string | number>>(new Set())

const loadSavedOffers = () => {
  try {
    const raw = localStorage.getItem('nexus_saved_offers')
    if (raw) {
      const arr = JSON.parse(raw)
      savedOfferIds.value = new Set(arr)
    }
  } catch {
    savedOfferIds.value = new Set()
  }
}

const toggleSaveOffer = (offer: OfferItem) => {
  const id = offer.id
  if (savedOfferIds.value.has(id)) {
    savedOfferIds.value.delete(id)
    showToast(`Removed "${offer.title}" from saved offers`)
  } else {
    savedOfferIds.value.add(id)
    showToast(`Saved "${offer.title}" to your offers!`)
  }
  localStorage.setItem('nexus_saved_offers', JSON.stringify(Array.from(savedOfferIds.value)))
}

const showToast = (msg: string) => {
  toastMessage.value = msg
  setTimeout(() => {
    if (toastMessage.value === msg) {
      toastMessage.value = ''
    }
  }, 3500)
}

/* =========================================================
   COPY CODE TO CLIPBOARD
========================================================= */

const copyPromoCode = async (code: string) => {
  if (!code) return
  try {
    await navigator.clipboard.writeText(code)
    copiedCode.value = code
    showToast(`Promo code ${code} copied to clipboard!`)
    setTimeout(() => {
      if (copiedCode.value === code) {
        copiedCode.value = ''
      }
    }, 3000)
  } catch {
    showToast(`Code: ${code}`)
  }
}

/* =========================================================
   BASELINE CURATED HARDWARE OFFERS
========================================================= */

const defaultOffers: OfferItem[] = [
  {
    id: 'off-1',
    code: 'NEXUS-VIP25',
    tag: 'FLASH DROP',
    title: 'Flagship RTX 4090 GPU Bundle Rebate',
    description: 'Exclusive member savings on GeForce RTX 40-series cards, custom liquid cooling loops & high-wattage titanium power supplies.',
    discount: '25% OFF',
    discountPercentage: 25,
    validity: 'Oct 14, 2026',
    expiryDate: '2026-10-14',
    category: 'Gaming',
    minSpend: '$500 min. order',
    type: 'flash',
    isExpiringSoon: true,
    isPersonal: true,
    terms: [
      'Valid on NVIDIA GeForce RTX 40-Series and AMD Radeon RX 7000 Series.',
      'Requires minimum cart spend of $500 before taxes.',
      'One-time use per customer account.',
      'Cannot be combined with clearance pricing.'
    ]
  },
  {
    id: 'off-2',
    code: 'RIG-BUILD15',
    tag: 'PERSONAL PERK',
    title: 'Custom Rig Builder Component Discount',
    description: 'Save 15% across all selected DDR5 memory kits, PCIe 5.0 NVMe SSDs and high-airflow PC cases when configured in our custom rig estimator.',
    discount: '15% OFF',
    discountPercentage: 15,
    validity: 'Oct 28, 2026',
    expiryDate: '2026-10-28',
    category: 'Gaming',
    minSpend: '$250 min. order',
    type: 'personal',
    isExpiringSoon: false,
    isPersonal: true,
    terms: [
      'Valid for components assembled through the Custom Build Estimator.',
      'Applicable on DDR5 RAM, Gen5 SSDs, and enthusiast chassis.',
      'Discount applies automatically when applying code.'
    ]
  },
  {
    id: 'off-3',
    code: 'PERIPH-20',
    tag: 'LIMITED DEAL',
    title: 'Pro Esports Peripherals & Audio Bundle',
    description: 'Elevate your competitive edge with rapid-trigger mechanical keyboards, lightweight wireless mice and studio-grade wireless headsets.',
    discount: '20% OFF',
    discountPercentage: 20,
    validity: 'Oct 18, 2026',
    expiryDate: '2026-10-18',
    category: 'Accessories',
    minSpend: '$100 min. order',
    type: 'bundle',
    isExpiringSoon: true,
    isPersonal: false,
    terms: [
      'Valid on gaming keyboards, mice, and studio headsets.',
      'Valid until Oct 18, 2026 or while warehouse inventory lasts.'
    ]
  },
  {
    id: 'off-4',
    code: 'CREATOR-30',
    tag: 'MEMBER ONLY',
    title: 'Workstation & OLED Display Upgrade',
    description: 'Special creator discount on 240Hz OLED gaming monitors, color-accurate displays, and dual-monitor ergonomic articulating arms.',
    discount: '30% OFF',
    discountPercentage: 30,
    validity: 'Nov 02, 2026',
    expiryDate: '2026-11-02',
    category: 'Laptops & Monitors',
    minSpend: '$400 min. order',
    type: 'general',
    isExpiringSoon: false,
    isPersonal: false,
    terms: [
      'Valid on OLED and high-refresh gaming displays.',
      'Includes complimentary premium pixel-defect warranty.'
    ]
  },
  {
    id: 'off-5',
    code: 'FREESHIP-NR',
    tag: 'FREE SHIPPING',
    title: 'Complimentary Express Domestic Air Freight',
    description: 'Enjoy free insured express delivery on any desktop hardware purchase. Delivered securely in reinforced courier transit packaging.',
    discount: 'FREE SHIPPING',
    discountPercentage: 10,
    validity: 'Oct 12, 2026',
    expiryDate: '2026-10-12',
    category: 'Accessories',
    minSpend: '$150 min. order',
    type: 'flash',
    isExpiringSoon: true,
    isPersonal: true,
    terms: [
      'Covers nationwide express domestic delivery.',
      'Signature on delivery required for high-value hardware.'
    ]
  },
  {
    id: 'off-6',
    code: 'COOLING-18',
    tag: 'HOT DEAL',
    title: 'Thermal Advantage: AIO Coolers & PWM Fans',
    description: 'Keep your CPU thermals icy during intensive gaming sessions. Special pricing on 360mm liquid AIO radiators and low-noise magnetic fans.',
    discount: '18% OFF',
    discountPercentage: 18,
    validity: 'Oct 30, 2026',
    expiryDate: '2026-10-30',
    category: 'Gaming',
    minSpend: '$80 min. order',
    type: 'general',
    isExpiringSoon: false,
    isPersonal: false,
    terms: [
      'Valid on all liquid AIO and high-static pressure fans.',
      'Limit 4 fan packs per customer.'
    ]
  }
]

const offers = ref<OfferItem[]>([...defaultOffers])

/* =========================================================
   BACKEND INGESTION (DEALS & COUPONS)
========================================================= */

const fetchPromotions = async () => {
  isLoading.value = true
  try {
    const [dealsRes, couponsRes] = await Promise.allSettled([
      api.get('/deals'),
      api.get('/coupons')
    ])

    const fetchedItems: OfferItem[] = []

    // 1. Process /api/deals
    if (dealsRes.status === 'fulfilled' && Array.isArray(dealsRes.value?.data)) {
      dealsRes.value.data.forEach((d: any, idx: number) => {
        const disc = Number(d.discountPercentage || 0)
        if (disc > 0) {
          fetchedItems.push({
            id: `deal-${d.dealId || idx}`,
            dealId: d.dealId,
            productId: d.productId,
            code: `DEAL-${d.dealId?.slice(0, 6).toUpperCase() || 'NEXUS'}`,
            tag: d.badgeText || 'HOT DEAL',
            title: d.productName ? `${d.productName} Special Deal` : 'Hardware Promotion',
            description: `Exclusive limited-time discount of ${disc}% off the original retail price of $${d.productOriginalPrice || 0}.`,
            discount: `${disc}% OFF`,
            discountPercentage: disc,
            validity: d.endDate ? new Date(d.endDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : 'Limited Time',
            expiryDate: d.endDate,
            category: 'Gaming',
            minSpend: 'No minimum',
            type: 'flash',
            isExpiringSoon: true,
            isPersonal: false,
            terms: [
              'Valid on indicated hardware item while current inventory lasts.',
              'Applied automatically at checkout when deal item is added to cart.'
            ]
          })
        }
      })
    }

    // 2. Process /api/coupons
    if (couponsRes.status === 'fulfilled' && Array.isArray(couponsRes.value?.data)) {
      couponsRes.value.data.forEach((c: any, idx: number) => {
        const disc = Number(c.discPercent || 0)
        const isExpiring = c.endDate ? new Date(c.endDate).getTime() - Date.now() < 7 * 24 * 3600 * 1000 : false
        fetchedItems.push({
          id: `coupon-${c.couponId || idx}`,
          code: c.code,
          tag: 'PROMO CODE',
          title: `Storewide ${disc}% Voucher: ${c.code}`,
          description: `Apply coupon code ${c.code} at checkout to save ${disc}% on your performance hardware order.`,
          discount: `${disc}% OFF`,
          discountPercentage: disc,
          validity: c.endDate ? new Date(c.endDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : 'No Expiry',
          expiryDate: c.endDate,
          category: 'Accessories',
          minSpend: '$50 min. order',
          type: 'general',
          isExpiringSoon: isExpiring,
          isPersonal: true,
          terms: [
            `Use promotional code ${c.code} in checkout cart.`,
            'Valid across all hardware components and gaming peripherals.'
          ]
        })
      })
    }

    if (fetchedItems.length > 0) {
      // Merge unique items with default offers
      offers.value = [...fetchedItems, ...defaultOffers]
    } else {
      offers.value = [...defaultOffers]
    }
  } catch (err) {
    console.warn('Using baseline hardware offers:', err)
    offers.value = [...defaultOffers]
  } finally {
    isLoading.value = false
  }
}

/* =========================================================
   CATEGORIES & FILTERING
========================================================= */

const categories = [
  'All',
  'Gaming',
  'Accessories',
  'Laptops & Monitors'
]

const tabCounts = computed(() => {
  const allCount = offers.value.length
  const personalCount = offers.value.filter(o => o.isPersonal || o.type === 'personal').length
  const savedCount = offers.value.filter(o => savedOfferIds.value.has(o.id)).length
  const expiringCount = offers.value.filter(o => o.isExpiringSoon).length
  return { allCount, personalCount, savedCount, expiringCount }
})

const filteredOffers = computed(() => {
  return offers.value.filter(offer => {
    // 1. Tab filter
    if (activeTab.value === 'personal' && !(offer.isPersonal || offer.type === 'personal')) {
      return false
    }
    if (activeTab.value === 'saved' && !savedOfferIds.value.has(offer.id)) {
      return false
    }
    if (activeTab.value === 'expiring' && !offer.isExpiringSoon) {
      return false
    }

    // 2. Category filter
    if (selectedCategory.value !== 'All' && offer.category !== selectedCategory.value) {
      return false
    }

    // 3. Search filter
    if (searchQuery.value.trim()) {
      const q = searchQuery.value.toLowerCase().trim()
      const matchTitle = offer.title.toLowerCase().includes(q)
      const matchDesc = offer.description.toLowerCase().includes(q)
      const matchCode = (offer.code || '').toLowerCase().includes(q)
      const matchTag = offer.tag.toLowerCase().includes(q)
      if (!matchTitle && !matchDesc && !matchCode && !matchTag) return false
    }

    return true
  }).sort((a, b) => {
    if (sortBy.value === 'highest') {
      return (b.discountPercentage || 0) - (a.discountPercentage || 0)
    }
    if (sortBy.value === 'ending') {
      return (a.isExpiringSoon ? 0 : 1) - (b.isExpiringSoon ? 0 : 1)
    }
    return 0
  })
})

const featuredOffer = computed(() => {
  return filteredOffers.value[0] || offers.value[0]
})

const openDetailsModal = (offer: OfferItem) => {
  selectedOffer.value = offer
  showOfferModal.value = true
}

const applyToCartAndShop = (offer: OfferItem) => {
  if (offer.code) {
    sessionStorage.setItem('appliedCoupon', offer.code)
    showToast(`Code ${offer.code} applied! Redirecting to catalog...`)
  }
  showOfferModal.value = false
  if (offer.productId) {
    router.push(`/product/${offer.productId}`)
  } else {
    router.push('/catalog')
  }
}

onMounted(() => {
  loadSavedOffers()
  syncTabFromRoute()
  fetchPromotions()
})
</script>

<template>
  <div class="relative min-h-screen bg-slate-100 text-slate-900 pb-20">

    <!-- =====================================================
         BACKGROUND TEXTURE (LIGHT & OPTIMIZED)
    ====================================================== -->
    <div
      class="fixed inset-0 pointer-events-none opacity-[0.25] bg-cover bg-center"
      :style="{ backgroundImage: `url(${backgroundImage})` }"
    ></div>
    <div class="fixed inset-0 pointer-events-none bg-gradient-to-b from-white/90 via-slate-100/95 to-slate-100"></div>

    <!-- AMBIENT ACCENTS -->
    <div class="pointer-events-none fixed -top-40 right-0 w-96 h-96 rounded-full bg-blue-500/10 blur-3xl"></div>
    <div class="pointer-events-none fixed top-1/3 -left-40 w-96 h-96 rounded-full bg-cyan-400/10 blur-3xl"></div>

    <!-- =====================================================
         MAIN CONTAINER
    ====================================================== -->
    <main class="relative z-10 w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-8">

      <!-- =================================================
           HERO BANNER
      ================================================== -->
      <section class="relative overflow-hidden rounded-3xl border border-white/90 bg-white/80 p-6 sm:p-10 lg:p-12 shadow-2xl shadow-slate-300/25 backdrop-blur-2xl mb-8">
        <div class="relative grid grid-cols-1 lg:grid-cols-[1fr_340px] gap-8 items-center">

          <!-- Left Info -->
          <div>
            <div class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-blue-50 border border-blue-200/80 text-blue-700 text-xs font-bold mb-4">
              <span class="w-2 h-2 rounded-full bg-blue-600 animate-ping"></span>
              <span>NexusRigs VIP Promotions</span>
            </div>

            <h1 class="text-3xl sm:text-4xl lg:text-5xl font-black text-slate-950 tracking-tight leading-tight">
              Deals Worth <span class="bg-gradient-to-r from-blue-600 to-cyan-500 bg-clip-text text-transparent">Upgrading</span> For.
            </h1>

            <p class="mt-4 text-sm sm:text-base text-slate-500 max-w-xl leading-relaxed">
              Explore exclusive member pricing, hardware rebate vouchers, and limited drops across custom gaming rigs, workstation GPUs, and enthusiast peripherals.
            </p>

            <div class="mt-6 flex flex-wrap items-center gap-3">
              <div class="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-blue-50/80 border border-blue-100 text-xs font-bold text-blue-700">
                <Tag class="w-4 h-4 text-blue-600" />
                <span>{{ offers.length }} Active Offers</span>
              </div>

              <div class="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-slate-50 border border-slate-200 text-xs font-semibold text-slate-600">
                <ShieldCheck class="w-4 h-4 text-emerald-600" />
                <span>Verified Direct Savings</span>
              </div>

              <div class="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-cyan-50/80 border border-cyan-100 text-xs font-semibold text-cyan-800">
                <Clock class="w-4 h-4 text-cyan-600" />
                <span>Updated Daily</span>
              </div>
            </div>
          </div>

          <!-- Right Showcase Card -->
          <div class="relative rounded-2xl border border-white/90 bg-gradient-to-br from-blue-50/90 to-cyan-50/70 p-6 shadow-xl shadow-blue-500/10">
            <div class="flex items-center justify-between mb-4">
              <span class="text-[10px] font-black uppercase tracking-widest text-blue-600">Spotlight Feature</span>
              <span class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-bold">
                <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span> Active
              </span>
            </div>

            <div class="w-14 h-14 rounded-2xl bg-white shadow-md flex items-center justify-center text-blue-600 mb-4">
              <Zap class="w-7 h-7" />
            </div>

            <h3 class="text-lg font-black text-slate-900 leading-snug">
              {{ featuredOffer.title }}
            </h3>
            <p class="text-xs text-slate-500 mt-1 line-clamp-2">
              {{ featuredOffer.description }}
            </p>

            <div class="mt-5 pt-4 border-t border-slate-200/80 flex items-center justify-between">
              <div>
                <span class="text-[10px] font-bold uppercase tracking-wider text-slate-400">Save Up To</span>
                <p class="text-2xl font-black text-blue-600">{{ featuredOffer.discount }}</p>
              </div>

              <button
                @click="openDetailsModal(featuredOffer)"
                class="px-4 py-2 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 text-white text-xs font-bold shadow-md shadow-blue-500/25 hover:shadow-blue-500/40 hover:-translate-y-0.5 transition"
              >
                View Deal
              </button>
            </div>
          </div>

        </div>
      </section>

      <!-- =================================================
           NAVIGATION TABS (SYNCED WITH APPNAVBAR DROPDOWN)
      ================================================== -->
      <div class="mb-6 flex flex-col md:flex-row gap-4 items-stretch md:items-center justify-between">

        <!-- TAB PILLS -->
        <div class="flex items-center gap-1.5 p-1.5 rounded-2xl bg-white/80 border border-white/90 shadow-lg shadow-slate-200/30 backdrop-blur-xl overflow-x-auto scrollbar-hide">
          <button
            @click="setTab('available')"
            :class="[
              'flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold transition whitespace-nowrap',
              activeTab === 'available'
                ? 'bg-blue-600 text-white shadow-md shadow-blue-500/30'
                : 'text-slate-600 hover:text-blue-600 hover:bg-blue-50/60'
            ]"
          >
            <Tag class="w-4 h-4" />
            <span>Available Offers</span>
            <span :class="['px-1.5 py-0.5 rounded-md text-[10px] font-black', activeTab === 'available' ? 'bg-white/20 text-white' : 'bg-slate-100 text-slate-600']">
              {{ tabCounts.allCount }}
            </span>
          </button>

          <button
            @click="setTab('personal')"
            :class="[
              'flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold transition whitespace-nowrap',
              activeTab === 'personal'
                ? 'bg-blue-600 text-white shadow-md shadow-blue-500/30'
                : 'text-slate-600 hover:text-blue-600 hover:bg-blue-50/60'
            ]"
          >
            <Sparkles class="w-4 h-4" />
            <span>Personal Deals</span>
            <span :class="['px-1.5 py-0.5 rounded-md text-[10px] font-black', activeTab === 'personal' ? 'bg-white/20 text-white' : 'bg-slate-100 text-slate-600']">
              {{ tabCounts.personalCount }}
            </span>
          </button>

          <button
            @click="setTab('saved')"
            :class="[
              'flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold transition whitespace-nowrap',
              activeTab === 'saved'
                ? 'bg-blue-600 text-white shadow-md shadow-blue-500/30'
                : 'text-slate-600 hover:text-blue-600 hover:bg-blue-50/60'
            ]"
          >
            <Bookmark class="w-4 h-4" />
            <span>Saved Offers</span>
            <span :class="['px-1.5 py-0.5 rounded-md text-[10px] font-black', activeTab === 'saved' ? 'bg-white/20 text-white' : 'bg-slate-100 text-slate-600']">
              {{ tabCounts.savedCount }}
            </span>
          </button>

          <button
            @click="setTab('expiring')"
            :class="[
              'flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold transition whitespace-nowrap',
              activeTab === 'expiring'
                ? 'bg-blue-600 text-white shadow-md shadow-blue-500/30'
                : 'text-slate-600 hover:text-blue-600 hover:bg-blue-50/60'
            ]"
          >
            <Clock class="w-4 h-4" />
            <span>Expiring Soon</span>
            <span :class="['px-1.5 py-0.5 rounded-md text-[10px] font-black', activeTab === 'expiring' ? 'bg-white/20 text-white' : 'bg-amber-100 text-amber-700 font-bold']">
              {{ tabCounts.expiringCount }}
            </span>
          </button>
        </div>

        <!-- SORT DROPDOWN -->
        <div class="flex items-center gap-3 self-end md:self-auto">
          <span class="text-xs text-slate-500 font-medium hidden sm:inline">Sort:</span>
          <select
            v-model="sortBy"
            class="px-4 py-2.5 rounded-2xl bg-white/80 border border-slate-200/90 text-xs font-bold text-slate-800 shadow-sm outline-none transition hover:bg-white"
          >
            <option value="highest">Highest Discount</option>
            <option value="ending">Ending Soonest</option>
            <option value="newest">Featured First</option>
          </select>
        </div>

      </div>

      <!-- =================================================
           CATEGORY PILLS & SEARCH BAR
      ================================================== -->
      <div class="mb-8 flex flex-col md:flex-row gap-4 items-stretch md:items-center justify-between">

        <!-- Category pills -->
        <div class="flex items-center gap-2 overflow-x-auto scrollbar-hide py-1">
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

        <!-- Search input -->
        <div class="relative min-w-[260px] sm:min-w-[320px]">
          <Search class="absolute left-3.5 top-3 w-4 h-4 text-slate-400" />
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Search offers or promo code..."
            class="w-full pl-10 pr-4 py-2.5 rounded-2xl bg-white/80 border border-slate-200/90 text-xs font-medium text-slate-900 placeholder:text-slate-400 shadow-sm outline-none focus:bg-white focus:border-blue-500 transition"
          />
          <button
            v-if="searchQuery"
            @click="searchQuery = ''"
            class="absolute right-3 top-2.5 text-slate-400 hover:text-slate-600"
          >
            <X class="w-4 h-4" />
          </button>
        </div>

      </div>

      <!-- =================================================
           OFFERS GRID
      ================================================== -->
      <section v-if="filteredOffers.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">

        <article
          v-for="offer in filteredOffers"
          :key="offer.id"
          class="group relative flex flex-col justify-between rounded-3xl border border-white/90 bg-white/85 p-6 shadow-xl shadow-slate-200/25 backdrop-blur-xl transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl hover:shadow-blue-500/10"
        >
          <!-- CARD HEADER -->
          <div>
            <div class="flex items-center justify-between gap-2 mb-4">
              <!-- Tag badge -->
              <span
                :class="[
                  'px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-wider',
                  offer.tag === 'FLASH DROP' ? 'bg-amber-50 text-amber-700 border border-amber-200' :
                  offer.tag === 'PERSONAL PERK' ? 'bg-purple-50 text-purple-700 border border-purple-200' :
                  offer.tag === 'FREE SHIPPING' ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' :
                  'bg-blue-50 text-blue-700 border border-blue-200'
                ]"
              >
                {{ offer.tag }}
              </span>

              <!-- Bookmark Save Button -->
              <button
                @click="toggleSaveOffer(offer)"
                :class="[
                  'p-2 rounded-xl border transition',
                  savedOfferIds.has(offer.id)
                    ? 'bg-blue-50 border-blue-200 text-blue-600'
                    : 'bg-slate-50 border-slate-200/80 text-slate-400 hover:text-blue-600 hover:bg-blue-50'
                ]"
                :title="savedOfferIds.has(offer.id) ? 'Remove from Saved' : 'Save Offer'"
              >
                <Bookmark class="w-4 h-4" :class="savedOfferIds.has(offer.id) ? 'fill-current' : ''" />
              </button>
            </div>

            <!-- Category & Title -->
            <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
              {{ offer.category }}
            </p>
            <h3 class="mt-1 text-lg font-black text-slate-900 group-hover:text-blue-600 transition leading-snug">
              {{ offer.title }}
            </h3>

            <p class="mt-2 text-xs text-slate-500 leading-relaxed line-clamp-3">
              {{ offer.description }}
            </p>

            <!-- Promo code box (if available) -->
            <div
              v-if="offer.code"
              class="mt-4 flex items-center justify-between px-3.5 py-2.5 rounded-xl bg-slate-50 border border-dashed border-slate-300/90 text-xs"
            >
              <div class="flex items-center gap-2 min-w-0">
                <Tag class="w-3.5 h-3.5 text-blue-600 shrink-0" />
                <code class="font-mono font-bold text-slate-800 truncate">{{ offer.code }}</code>
              </div>
              <button
                @click="copyPromoCode(offer.code)"
                class="flex items-center gap-1 text-[11px] font-bold text-blue-600 hover:text-blue-700 ml-2 shrink-0"
              >
                <Check v-if="copiedCode === offer.code" class="w-3.5 h-3.5 text-emerald-600" />
                <Copy v-else class="w-3.5 h-3.5" />
                <span>{{ copiedCode === offer.code ? 'Copied' : 'Copy' }}</span>
              </button>
            </div>
          </div>

          <!-- CARD FOOTER -->
          <div class="mt-6 pt-5 border-t border-slate-100 flex items-end justify-between gap-4">
            <div>
              <p class="text-[9px] font-bold uppercase tracking-wider text-slate-400">Discount</p>
              <p class="text-2xl font-black text-blue-600 leading-none mt-0.5">
                {{ offer.discount }}
              </p>
              <p class="mt-1.5 flex items-center gap-1 text-[10px] font-medium text-slate-400">
                <Clock class="w-3 h-3 text-slate-400" />
                <span>Expires {{ offer.validity }}</span>
              </p>
            </div>

            <button
              @click="openDetailsModal(offer)"
              class="px-4 py-2.5 rounded-xl border border-slate-200 bg-white hover:bg-blue-50 hover:border-blue-300 hover:text-blue-600 text-slate-700 text-xs font-bold transition shadow-sm"
            >
              View Offer
            </button>
          </div>

        </article>

      </section>

      <!-- =================================================
           EMPTY STATE
      ================================================== -->
      <section
        v-else
        class="rounded-3xl border border-white/90 bg-white/80 p-12 text-center shadow-xl shadow-slate-200/20 backdrop-blur-xl"
      >
        <div class="w-16 h-16 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center mx-auto mb-4">
          <Gift class="w-8 h-8" />
        </div>
        <h3 class="text-lg font-black text-slate-900">No Offers Found</h3>
        <p class="text-xs text-slate-500 mt-1 max-w-md mx-auto">
          We couldn't find any promotional offers matching your active filters or search terms. Try clearing your filters or exploring all available promotions.
        </p>
        <button
          @click="activeTab = 'available'; selectedCategory = 'All'; searchQuery = ''"
          class="mt-5 px-5 py-2.5 rounded-xl bg-blue-600 text-white text-xs font-bold shadow-md shadow-blue-500/20 hover:bg-blue-700 transition"
        >
          View All Offers
        </button>
      </section>

      <!-- =================================================
           BENEFITS & REWARDS STRIP
      ================================================== -->
      <section class="mt-12 rounded-3xl border border-white/90 bg-white/70 p-6 shadow-xl shadow-slate-200/20 backdrop-blur-xl">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">

          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0">
              <ShieldCheck class="w-6 h-6" />
            </div>
            <div>
              <h4 class="text-sm font-black text-slate-900">Official Hardware Warranty</h4>
              <p class="text-xs text-slate-500 mt-0.5">All discounted components carry full manufacturer warranties & 30-day RMA coverage.</p>
            </div>
          </div>

          <div class="flex items-center gap-4 border-t md:border-t-0 md:border-l border-slate-200/70 pt-4 md:pt-0 md:pl-6">
            <div class="w-12 h-12 rounded-2xl bg-cyan-50 text-cyan-600 flex items-center justify-center shrink-0">
              <Zap class="w-6 h-6" />
            </div>
            <div>
              <h4 class="text-sm font-black text-slate-900">Instant Cart Redemption</h4>
              <p class="text-xs text-slate-500 mt-0.5">Codes automatically apply to qualifying items in your shopping bag at checkout.</p>
            </div>
          </div>

          <div class="flex items-center gap-4 border-t md:border-t-0 md:border-l border-slate-200/70 pt-4 md:pt-0 md:pl-6">
            <div class="w-12 h-12 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center shrink-0">
              <Award class="w-6 h-6" />
            </div>
            <div>
              <h4 class="text-sm font-black text-slate-900">Nexus Elite Loyalty</h4>
              <p class="text-xs text-slate-500 mt-0.5">Higher spending unlocks personalized hardware vouchers & priority build queuing.</p>
            </div>
          </div>

        </div>
      </section>

    </main>

    <!-- =====================================================
         OFFER DETAILS MODAL
    ====================================================== -->
    <div
      v-if="showOfferModal && selectedOffer"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/40 backdrop-blur-sm"
      @click.self="showOfferModal = false"
    >
      <div
        class="relative w-full max-w-lg rounded-3xl border border-white/90 bg-white p-6 sm:p-8 shadow-2xl shadow-slate-400/30 overflow-hidden animate-in fade-in zoom-in-95 duration-200"
      >
        <!-- Close button -->
        <button
          @click="showOfferModal = false"
          class="absolute top-5 right-5 p-2 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition"
        >
          <X class="w-5 h-5" />
        </button>

        <!-- Modal Header -->
        <div class="flex items-center gap-2 mb-3">
          <span class="px-2.5 py-1 rounded-full bg-blue-50 text-blue-700 text-[10px] font-black uppercase tracking-wider">
            {{ selectedOffer.tag }}
          </span>
          <span class="text-xs font-bold text-slate-400">{{ selectedOffer.category }}</span>
        </div>

        <h3 class="text-xl font-black text-slate-900 leading-snug">
          {{ selectedOffer.title }}
        </h3>

        <div class="my-5 p-4 rounded-2xl bg-blue-50/70 border border-blue-100 flex items-center justify-between">
          <div>
            <span class="text-[10px] font-bold uppercase tracking-wider text-slate-400">Promo Value</span>
            <p class="text-3xl font-black text-blue-600">{{ selectedOffer.discount }}</p>
          </div>
          <div v-if="selectedOffer.code" class="text-right">
            <span class="text-[10px] font-bold uppercase tracking-wider text-slate-400">Coupon Code</span>
            <div class="flex items-center gap-2 mt-1">
              <code class="px-2.5 py-1 rounded-lg bg-white border border-blue-200 font-mono font-black text-slate-800 text-xs">
                {{ selectedOffer.code }}
              </code>
              <button
                @click="copyPromoCode(selectedOffer.code)"
                class="p-1.5 rounded-lg bg-blue-600 hover:bg-blue-700 text-white transition"
                title="Copy Code"
              >
                <Check v-if="copiedCode === selectedOffer.code" class="w-3.5 h-3.5" />
                <Copy v-else class="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>

        <p class="text-xs text-slate-600 leading-relaxed">
          {{ selectedOffer.description }}
        </p>

        <!-- Terms & Conditions -->
        <div v-if="selectedOffer.terms && selectedOffer.terms.length > 0" class="mt-5 pt-4 border-t border-slate-100">
          <h5 class="text-[11px] font-black uppercase tracking-wider text-slate-700 mb-2">Offer Terms & Eligibility</h5>
          <ul class="space-y-1.5">
            <li
              v-for="(t, i) in selectedOffer.terms"
              :key="i"
              class="flex items-start gap-2 text-[11px] text-slate-500"
            >
              <CheckCircle2 class="w-3.5 h-3.5 text-blue-500 shrink-0 mt-0.5" />
              <span>{{ t }}</span>
            </li>
          </ul>
        </div>

        <div class="mt-4 flex items-center gap-2 text-[11px] text-slate-400">
          <Clock class="w-3.5 h-3.5 text-slate-400" />
          <span>Valid until <strong>{{ selectedOffer.validity }}</strong></span>
        </div>

        <!-- Action Buttons -->
        <div class="mt-6 pt-5 border-t border-slate-100 flex items-center justify-end gap-3">
          <button
            @click="showOfferModal = false"
            class="px-4 py-2.5 rounded-xl border border-slate-200 text-xs font-bold text-slate-600 hover:bg-slate-100 transition"
          >
            Close
          </button>

          <button
            @click="applyToCartAndShop(selectedOffer)"
            class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 text-white text-xs font-bold shadow-md shadow-blue-500/25 hover:shadow-blue-500/40 hover:-translate-y-0.5 transition"
          >
            <span>Shop This Deal</span>
            <ArrowRight class="w-3.5 h-3.5" />
          </button>
        </div>

      </div>
    </div>

    <!-- =====================================================
         FLOATING TOAST MESSAGE
    ====================================================== -->
    <transition
      enter-active-class="transform transition duration-300 ease-out"
      enter-from-class="-translate-y-8 opacity-0"
      enter-to-class="translate-y-0 opacity-100"
      leave-active-class="transform transition duration-200 ease-in"
      leave-from-class="translate-y-0 opacity-100"
      leave-to-class="-translate-y-8 opacity-0"
    >
      <div
        v-if="toastMessage"
        class="fixed top-6 right-6 z-50 flex items-center gap-3 px-5 py-3.5 rounded-2xl bg-white/95 border border-slate-200 shadow-xl shadow-slate-300/30 backdrop-blur-xl"
      >
        <CheckCircle2 class="w-5 h-5 text-emerald-600 shrink-0" />
        <span class="text-xs font-bold text-slate-900">{{ toastMessage }}</span>
      </div>
    </transition>

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