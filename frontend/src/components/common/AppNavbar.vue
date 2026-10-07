<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cartStore'
import api from '../../services/api'

import logo from '../../assets/icons/logoIMG-removebg-preview.svg'
import defaultAvatar from '../../assets/images/userImageDemo.png'

/* =========================================================
   TYPES
   ========================================================= */

interface Category {
  categoryId: string | number
  categoryName: string
  description?: string
}

interface NavItem {
  title: string
  description: string
  path: string
}

/* =========================================================
   ROUTER / STORE
   ========================================================= */

const router = useRouter()
const cartStore = useCartStore()

/* =========================================================
   STATE
   ========================================================= */

const searchQuery = ref('')
const categories = ref<Category[]>([])
const categoriesLoading = ref(false)

const isMobileMenuOpen = ref(false)
const mobileDropdown = ref<string | null>(null)

const userAvatar = ref(defaultAvatar)

/* =========================================================
   NAVIGATION MENU DATA
   ========================================================= */

const dealMenu: NavItem[] = [
  {
    title: "Today's Deals",
    description: 'Limited-time discounts',
    path: '/deals'
  },
  {
    title: 'Gaming Deals',
    description: 'Save on gaming hardware',
    path: '/deals?category=gaming'
  },
  {
    title: 'Component Deals',
    description: 'PC component discounts',
    path: '/deals?category=components'
  },
  {
    title: 'Clearance',
    description: 'Last chance offers',
    path: '/deals?category=clearance'
  }
]

const buildMenu: NavItem[] = [
  {
    title: 'Gaming PC',
    description: 'High-performance gaming systems',
    path: '/builds?type=gaming'
  },
  {
    title: 'Creator PC',
    description: 'Systems for creators',
    path: '/builds?type=creator'
  },
  {
    title: 'Workstation',
    description: 'Professional workstations',
    path: '/builds?type=workstation'
  },
  {
    title: 'Custom Build',
    description: 'Configure your own PC',
    path: '/builds'
  }
]

const compareMenu: NavItem[] = [
  {
    title: 'Hardware Compare',
    description: 'Side-by-side component analysis',
    path: '/compare'
  },
  {
    title: 'GPU Gaming Benchmarks',
    description: '1080p, 1440p & 4K FPS metrics',
    path: '/compare?category=gpu'
  },
  {
    title: 'CPU Specs & Clocks',
    description: 'Multi-core performance comparison',
    path: '/compare?category=cpu'
  },
  {
    title: 'Custom PC Rig Builder',
    description: 'Configure your compatible build',
    path: '/builds'
  }
]

const offersMenu: NavItem[] = [
  {
    title: 'Available Offers',
    description: 'Offers available to you',
    path: '/myOffers'
  },
  {
    title: 'Saved Offers',
    description: 'Offers you saved',
    path: '/myOffers?saved=true'
  },
  {
    title: 'Expiring Soon',
    description: 'Use before expiry',
    path: '/myOffers?status=expiring'
  },
  {
    title: 'Personal Deals',
    description: 'Offers selected for you',
    path: '/myOffers?type=personal'
  }
]

const supportMenu: NavItem[] = [
  {
    title: 'Help Center',
    description: 'Common questions and answers',
    path: '/support'
  },
  {
    title: 'Order Support',
    description: 'Help with your orders',
    path: '/support?type=orders'
  },
  {
    title: 'Returns & Refunds',
    description: 'Return and refund assistance',
    path: '/support?type=returns'
  },
  {
    title: 'Contact Support',
    description: 'Talk with our support team',
    path: '/support?type=contact'
  }
]

/* =========================================================
   CART COUNT
   ========================================================= */

const cartDisplayCount = computed(() => {
  const store = cartStore as any

  if (typeof store.itemCount === 'number') {
    return store.itemCount
  }

  if (typeof store.totalItems === 'number') {
    return store.totalItems
  }

  if (Array.isArray(store.items)) {
    return store.items.reduce(
      (total: number, item: any) =>
        total + Number(item.quantity ?? item.qty ?? 1),
      0
    )
  }

  return 0
})

/* =========================================================
   WISHLIST COUNT
   ========================================================= */

const wishlistCount = ref(0)

function loadWishlistCount() {
  try {
    const savedUser = sessionStorage.getItem('user')

    if (!savedUser) {
      wishlistCount.value = 0
      return
    }

    const user = JSON.parse(savedUser)

    if (typeof user.wishlistCount === 'number') {
      wishlistCount.value = user.wishlistCount
      return
    }

    if (Array.isArray(user.wishlist)) {
      wishlistCount.value = user.wishlist.length
      return
    }

    if (Array.isArray(user.wishlistItems)) {
      wishlistCount.value = user.wishlistItems.length
      return
    }

    wishlistCount.value = 0
  } catch {
    wishlistCount.value = 0
  }
}

/* =========================================================
   USER IMAGE
   ========================================================= */

function getBackendBaseUrl() {
  const configuredBaseUrl = api.defaults.baseURL

  if (configuredBaseUrl) {
    return configuredBaseUrl.replace(/\/api\/?$/, '')
  }

  return 'http://localhost:8080'
}

function getUserImageUrl(image: unknown) {
  if (!image || typeof image !== 'string') {
    return defaultAvatar
  }

  const value = image.trim()

  if (!value) {
    return defaultAvatar
  }

  if (
    value.startsWith('http://') ||
    value.startsWith('https://') ||
    value.startsWith('data:')
  ) {
    return value
  }

  const baseUrl = getBackendBaseUrl()

  if (value.startsWith('/')) {
    return `${baseUrl}${value}`
  }

  if (value.startsWith('uploads/')) {
    return `${baseUrl}/${value}`
  }

  return `${baseUrl}/uploads/${value}`
}

function loadUserData() {
  try {
    const savedUser = sessionStorage.getItem('user')

    if (!savedUser) {
      userAvatar.value = defaultAvatar
      return
    }

    const user = JSON.parse(savedUser)

    const image =
      user.userImage ??
      user.image ??
      user.profileImage ??
      user.avatar ??
      ''

    userAvatar.value = getUserImageUrl(image)
  } catch {
    userAvatar.value = defaultAvatar
  }
}

function handleImageError(event: Event) {
  const image = event.target as HTMLImageElement

  if (image.src !== defaultAvatar) {
    image.src = defaultAvatar
  }
}

/* =========================================================
   CATEGORY API
   ========================================================= */

async function loadCategories() {
  categoriesLoading.value = true

  try {
    const response = await api.get('/categories')

    let data = response.data

    if (!Array.isArray(data)) {
      data = data?.content ?? data?.data ?? []
    }

    if (Array.isArray(data)) {
      categories.value = data
        .map((item: any) => ({
          categoryId: item.categoryId ?? item.id,
          categoryName: item.categoryName ?? item.name ?? 'Category',
          description: item.description ?? ''
        }))
        .filter(
          (category: Category) =>
            category.categoryId !== undefined &&
            category.categoryId !== null
        )
    } else {
      categories.value = []
    }
  } catch (error) {
    console.warn('Could not load product categories.', error)
    categories.value = []
  } finally {
    categoriesLoading.value = false
  }
}

/* =========================================================
   SEARCH
   ========================================================= */

function onSearch() {
  const query = searchQuery.value.trim()

  if (!query) {
    return
  }

  closeMobileMenu()

  router.push({
    path: '/catalog',
    query: {
      q: query
    }
  })
}

/* =========================================================
   CATEGORY NAVIGATION
   ========================================================= */

function goToCategory(categoryId: string | number) {
  closeMobileMenu()

  router.push({
    path: '/catalog',
    query: {
      categoryId: String(categoryId)
    }
  })
}

function goToAllProducts() {
  closeMobileMenu()
  router.push('/catalog')
}

/* =========================================================
   MOBILE MENU
   ========================================================= */

function toggleMobileMenu() {
  isMobileMenuOpen.value = !isMobileMenuOpen.value

  if (!isMobileMenuOpen.value) {
    mobileDropdown.value = null
  }
}

function toggleMobileDropdown(name: string) {
  if (mobileDropdown.value === name) {
    mobileDropdown.value = null
  } else {
    mobileDropdown.value = name
  }
}

function closeMobileMenu() {
  isMobileMenuOpen.value = false
  mobileDropdown.value = null
}

/* =========================================================
   MOUNT
   ========================================================= */

onMounted(async () => {
  loadUserData()
  loadWishlistCount()

  window.addEventListener('user-profile-updated', loadUserData)

  await loadCategories()

  try {
    await cartStore.fetchCart()
  } catch (error) {
    console.warn('Could not refresh cart.', error)
  }
})

onUnmounted(() => {
  window.removeEventListener('user-profile-updated', loadUserData)
})
</script>

<template>
  <header
    class="sticky top-0 z-[1000] w-full overflow-visible border-b border-slate-300/60 bg-slate-100/90 shadow-md shadow-slate-300/20 backdrop-blur-2xl">
    <!-- =====================================================
         MAIN HEADER
         ===================================================== -->

    <div class="w-full px-3 sm:px-6 lg:px-8 xl:px-10 2xl:px-12">
      <div class="flex h-[70px] items-center justify-between gap-3 xl:gap-5">

        <!-- =================================================
             BRAND (name now visible on mobile too)
             ================================================= -->

        <router-link to="/" class="group flex shrink-0 items-center gap-1">
          <div class="flex h-12 w-12 items-center justify-center overflow-hidden">
            <img :src="logo" alt="NexusRigs Logo"
              class="h-14 w-14 object-contain transition-transform duration-300 group-hover:scale-105" />
          </div>

          <div class="mt-1 leading-none">
            <div class="text-[15px] font-black tracking-[-0.04em] text-slate-950 sm:text-[17px]">
              NEXUS<span class="text-blue-600">RIGS</span>
            </div>

            <div class="mt-1 hidden text-[8px] font-bold uppercase tracking-[0.22em] text-slate-400 sm:block">
              Performance Hardware
            </div>
          </div>
        </router-link>

        <!-- =================================================
             DESKTOP NAV
             ================================================= -->

        <nav class="relative z-[1001] hidden items-center gap-0.5 xl:flex">

          <!-- =================================================
               PRODUCTS
               ================================================= -->

          <div class="group relative">
            <router-link to="/catalog" aria-haspopup="true"
              class="relative z-20 flex items-center gap-2 rounded-xl px-3.5 py-2.5 text-[13px] font-semibold text-slate-600 transition-all duration-200 hover:bg-white hover:text-blue-600">
              <!-- Package / Products -->
              <svg class="h-[18px] w-[18px] shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                  d="M3.75 7.5 12 3l8.25 4.5M3.75 7.5V16.5L12 21l8.25-4.5V7.5M3.75 7.5 12 12l8.25-4.5M12 12v9" />
              </svg>

              <span>Products</span>

              <svg class="ml-0.5 h-3.5 w-3.5 text-slate-400 transition-transform duration-200 group-hover:rotate-180"
                fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="m6 9 6 6 6-6" />
              </svg>
            </router-link>

            <!-- Products Dropdown -->
            <div
              class="pointer-events-none invisible absolute left-0 top-full z-[1200] w-[680px] translate-y-1 pt-3 opacity-0 transition-all duration-200 group-hover:pointer-events-auto group-hover:visible group-hover:translate-y-0 group-hover:opacity-100">
              <div
                class="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-[0_20px_60px_rgba(15,23,42,0.15)]">
                <!-- Header -->
                <div class="flex items-center justify-between border-b border-slate-100 px-5 py-4">
                  <div class="flex min-w-0 items-center gap-3">
                    <div
                      class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
                      <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                          d="M3.75 7.5 12 3l8.25 4.5M3.75 7.5V16.5L12 21l8.25-4.5V7.5M3.75 7.5 12 12l8.25-4.5M12 12v9" />
                      </svg>
                    </div>

                    <div class="min-w-0">
                      <p class="text-[9px] font-bold uppercase tracking-[0.18em] text-blue-600">
                        Shop Hardware
                      </p>

                      <h3 class="mt-0.5 truncate text-base font-black tracking-tight text-slate-900">
                        Product Categories
                      </h3>

                      <p class="mt-0.5 text-[10px] text-slate-500">
                        Browse our hardware collection by category.
                      </p>
                    </div>
                  </div>

                  <button type="button" @click="goToAllProducts"
                    class="ml-4 inline-flex shrink-0 items-center gap-1.5 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-[10px] font-bold text-slate-700 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-600">
                    View All

                    <svg class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                        d="M5 12h14m-6-6 6 6-6 6" />
                    </svg>
                  </button>
                </div>

                <!-- Body -->
                <div class="p-4">
                  <div v-if="categoriesLoading" class="grid max-h-[300px] grid-cols-2 gap-2 overflow-hidden">
                    <div v-for="item in 6" :key="item" class="h-[58px] animate-pulse rounded-xl bg-slate-100"></div>
                  </div>

                  <div v-else-if="categories.length > 0"
                    class="grid max-h-[300px] grid-cols-2 gap-2 overflow-y-auto pr-1">
                    <button v-for="category in categories" :key="category.categoryId" type="button"
                      @click="goToCategory(category.categoryId)"
                      class="group/category flex min-h-[58px] min-w-0 items-center gap-3 rounded-xl border border-transparent p-2.5 text-left transition-all duration-200 hover:border-slate-200 hover:bg-slate-50">
                      <span
                        class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-sm font-black text-slate-500 transition-all group-hover/category:bg-blue-600 group-hover/category:text-white">
                        {{
                          category.categoryName
                            .charAt(0)
                            .toUpperCase()
                        }}
                      </span>

                      <span class="min-w-0 flex-1">
                        <span
                          class="block truncate text-[11px] font-bold text-slate-800 group-hover/category:text-blue-600">
                          {{ category.categoryName }}
                        </span>

                        <span v-if="category.description" class="mt-0.5 block truncate text-[9px] text-slate-400">
                          {{ category.description }}
                        </span>
                      </span>

                      <span
                        class="shrink-0 text-xs text-slate-300 transition group-hover/category:translate-x-0.5 group-hover/category:text-blue-500">
                        →
                      </span>
                    </button>
                  </div>

                  <div v-else class="rounded-xl border border-dashed border-slate-200 p-6 text-center">
                    <div
                      class="mx-auto flex h-10 w-10 items-center justify-center rounded-xl bg-slate-100 text-slate-400">
                      <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                          d="M3 7.5 12 3l9 4.5M3 7.5v9l9 4.5 9-4.5v-9M3 7.5 12 12l9-4.5M12 12v9" />
                      </svg>
                    </div>

                    <p class="mt-2 text-xs font-bold text-slate-700">
                      No categories available
                    </p>

                    <p class="mt-1 text-[10px] text-slate-400">
                      Browse all products instead.
                    </p>
                  </div>
                </div>

                <!-- Footer -->
                <div class="flex items-center justify-between border-t border-slate-100 bg-slate-50/70 px-5 py-3">
                  <p class="text-[9px] text-slate-400">
                    {{ categories.length }} categories available
                  </p>

                  <button type="button" @click="goToAllProducts"
                    class="text-[10px] font-bold text-blue-600 hover:text-blue-700">
                    Browse complete catalog →
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- =================================================
               DEALS
               ================================================= -->

          <div class="group relative">
            <router-link to="/deals"
              class="relative z-20 flex items-center gap-2 rounded-xl px-3.5 py-2.5 text-[13px] font-semibold text-slate-600 transition-all duration-200 hover:bg-white hover:text-amber-600">
              <!-- Tag -->
              <svg class="h-[18px] w-[18px]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                  d="M20.5 13.5 13.5 20.5a2 2 0 0 1-2.83 0L3.5 13.33V4.5h8.83l7.17 7.17a2 2 0 0 1 0 2.83Z" />
                <circle cx="8" cy="8" r="1.2" stroke-width="1.7" />
              </svg>

              <span>Deals</span>

              <svg class="h-3.5 w-3.5 text-slate-400 transition-transform duration-200 group-hover:rotate-180"
                fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="m6 9 6 6 6-6" />
              </svg>
            </router-link>

            <div
              class="pointer-events-none invisible absolute left-1/2 top-full z-[1200] w-[470px] -translate-x-1/2 translate-y-1 pt-3 opacity-0 transition-all duration-200 group-hover:pointer-events-auto group-hover:visible group-hover:translate-y-0 group-hover:opacity-100">
              <div
                class="overflow-hidden rounded-2xl border border-slate-200 bg-white p-4 shadow-[0_20px_60px_rgba(15,23,42,0.15)]">
                <div class="mb-3 flex items-center gap-3 rounded-xl bg-amber-50 px-4 py-3">
                  <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-white text-amber-600">
                    <svg class="h-[18px] w-[18px] shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                        d="M20.5 13.5 13.5 20.5a2 2 0 0 1-2.83 0L3.5 13.33V4.5h8.83l7.17 7.17a2 2 0 0 1 0 2.83Z" />
                      <circle cx="8" cy="8" r="1.2" stroke-width="1.7" />
                    </svg>
                  </div>

                  <div>
                    <p class="text-[9px] font-bold uppercase tracking-widest text-amber-600">
                      Special Offers
                    </p>

                    <p class="mt-0.5 text-sm font-black text-slate-900">
                      Save more on selected products
                    </p>
                  </div>
                </div>

                <div class="grid grid-cols-2 gap-2">
                  <router-link v-for="item in dealMenu" :key="item.title" :to="item.path"
                    class="rounded-xl border border-transparent p-3 transition hover:border-amber-100 hover:bg-amber-50/60">
                    <p class="text-xs font-bold text-slate-800 hover:text-amber-600">
                      {{ item.title }}
                    </p>

                    <p class="mt-1 text-[9px] leading-4 text-slate-400">
                      {{ item.description }}
                    </p>
                  </router-link>
                </div>
              </div>
            </div>
          </div>

          <!-- =================================================
               CUSTOM BUILDS
               ================================================= -->

          <div class="group relative">
            <router-link to="/builds"
              class="relative z-20 flex items-center gap-2 rounded-xl px-3.5 py-2.5 text-[13px] font-semibold text-slate-600 transition-all duration-200 hover:bg-white hover:text-violet-600">
              <!-- CPU -->
              <svg class="h-[18px] w-[18px]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <rect x="7" y="7" width="10" height="10" rx="1.5" stroke-width="1.7" />
                <path stroke-linecap="round" stroke-width="1.7"
                  d="M9.5 1.75v3M14.5 1.75v3M9.5 19.25v3M14.5 19.25v3M19.25 9.5h3M19.25 14.5h3M1.75 9.5h3M1.75 14.5h3" />
              </svg>

              <span>Custom Builds</span>

              <svg class="h-3.5 w-3.5 text-slate-400 transition-transform duration-200 group-hover:rotate-180"
                fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="m6 9 6 6 6-6" />
              </svg>
            </router-link>

            <div
              class="pointer-events-none invisible absolute left-1/2 top-full z-[1200] w-[470px] -translate-x-1/2 translate-y-1 pt-3 opacity-0 transition-all duration-200 group-hover:pointer-events-auto group-hover:visible group-hover:translate-y-0 group-hover:opacity-100">
              <div
                class="overflow-hidden rounded-2xl border border-slate-200 bg-white p-4 shadow-[0_20px_60px_rgba(15,23,42,0.15)]">
                <div class="mb-3 flex items-center gap-3 rounded-xl bg-violet-50 px-4 py-3">
                  <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-white text-violet-600">
                    <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <rect x="7" y="7" width="10" height="10" rx="1.5" stroke-width="1.7" />
                      <path stroke-linecap="round" stroke-width="1.7"
                        d="M9.5 1.75v3M14.5 1.75v3M9.5 19.25v3M14.5 19.25v3M19.25 9.5h3M19.25 14.5h3M1.75 9.5h3M1.75 14.5h3" />
                    </svg>
                  </div>

                  <div>
                    <p class="text-[9px] font-bold uppercase tracking-widest text-violet-600">
                      PC Builder
                    </p>

                    <p class="mt-0.5 text-sm font-black text-slate-900">
                      Build a system for your needs
                    </p>
                  </div>
                </div>

                <div class="grid grid-cols-2 gap-2">
                  <router-link v-for="item in buildMenu" :key="item.title" :to="item.path"
                    class="rounded-xl border border-transparent p-3 transition hover:border-violet-100 hover:bg-violet-50/60">
                    <p class="text-xs font-bold text-slate-800">
                      {{ item.title }}
                    </p>

                    <p class="mt-1 text-[9px] leading-4 text-slate-400">
                      {{ item.description }}
                    </p>
                  </router-link>
                </div>
              </div>
            </div>
          </div>

          <!-- =================================================
               COUPONS
               ================================================= -->

          <div class="group relative">
            <router-link to="/compare"
              class="relative z-20 flex items-center gap-2 rounded-xl px-3.5 py-2.5 text-[13px] font-semibold text-slate-600 transition-all duration-200 hover:bg-white hover:text-blue-600">
              <!-- Scale / Compare -->
              <svg class="h-[18px] w-[18px]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                  d="M3 6l3 1m0 0l-3 9a5.002 5.002 0 006.001 0M6 7l3 9M6 7l6-2m6 2l3-1m-3 1l-3 9a5.002 5.002 0 006.001 0M18 7l3 9m-3-9l-6-2m0-2v2m0 16V5m0 16H9m3 0h3" />
              </svg>

              <span>Compare & Lab</span>

              <svg class="h-3.5 w-3.5 text-slate-400 transition-transform duration-200 group-hover:rotate-180"
                fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="m6 9 6 6 6-6" />
              </svg>
            </router-link>

            <div
              class="pointer-events-none invisible absolute left-1/2 top-full z-[1200] w-[470px] -translate-x-1/2 translate-y-1 pt-3 opacity-0 transition-all duration-200 group-hover:pointer-events-auto group-hover:visible group-hover:translate-y-0 group-hover:opacity-100">
              <div
                class="overflow-hidden rounded-2xl border border-slate-200 bg-white p-4 shadow-[0_20px_60px_rgba(15,23,42,0.15)]">
                <div class="mb-3 flex items-center gap-3 rounded-xl bg-blue-50 px-4 py-3">
                  <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-white text-blue-600">
                    <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                        d="M3 6l3 1m0 0l-3 9a5.002 5.002 0 006.001 0M6 7l3 9M6 7l6-2m6 2l3-1m-3 1l-3 9a5.002 5.002 0 006.001 0M18 7l3 9m-3-9l-6-2m0-2v2m0 16V5m0 16H9m3 0h3" />
                    </svg>
                  </div>

                  <div>
                    <p class="text-[9px] font-bold uppercase tracking-widest text-blue-600">
                      Hardware Benchmark Lab
                    </p>

                    <p class="mt-0.5 text-sm font-black text-slate-900">
                      Compare specs & gaming FPS
                    </p>
                  </div>
                </div>

                <div class="grid grid-cols-2 gap-2">
                  <router-link v-for="item in compareMenu" :key="item.title" :to="item.path"
                    class="rounded-xl border border-transparent p-3 transition hover:border-blue-100 hover:bg-blue-50/60">
                    <p class="text-xs font-bold text-slate-800">
                      {{ item.title }}
                    </p>

                    <p class="mt-1 text-[9px] leading-4 text-slate-400">
                      {{ item.description }}
                    </p>
                  </router-link>
                </div>
              </div>
            </div>
          </div>

          <!-- =================================================
               MY OFFERS
               ================================================= -->

          <div class="group relative">
            <router-link to="/myOffers"
              class="relative z-20 flex items-center gap-2 rounded-xl px-3.5 py-2.5 text-[13px] font-semibold text-slate-600 transition-all duration-200 hover:bg-white hover:text-blue-600">
              <!-- Sparkles / Offers -->
              <svg class="h-[18px] w-[18px]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                  d="m12 3 1.2 4.1L17 8.5l-3.8 1.4L12 14l-1.2-4.1L7 8.5l3.8-1.4L12 3Zm6.5 9.5.7 2.3 2.3.7-2.3.7-.7 2.3-.7-2.3-2.3-.7 2.3-.7.7-2.3ZM5 14l.8 2.7 2.7.8-2.7.8L5 21l-.8-2.7-2.7-.8 2.7-.8L5 14Z" />
              </svg>

              <span>My Offers</span>

              <svg class="h-3.5 w-3.5 text-slate-400 transition-transform duration-200 group-hover:rotate-180"
                fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="m6 9 6 6 6-6" />
              </svg>
            </router-link>

            <div
              class="pointer-events-none invisible absolute left-1/2 top-full z-[1200] w-[470px] -translate-x-1/2 translate-y-1 pt-3 opacity-0 transition-all duration-200 group-hover:pointer-events-auto group-hover:visible group-hover:translate-y-0 group-hover:opacity-100">
              <div
                class="overflow-hidden rounded-2xl border border-slate-200 bg-white p-4 shadow-[0_20px_60px_rgba(15,23,42,0.15)]">
                <div class="mb-3 flex items-center gap-3 rounded-xl bg-blue-50 px-4 py-3">
                  <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-white text-blue-600">
                    <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                        d="m12 3 1.2 4.1L17 8.5l-3.8 1.4L12 14l-1.2-4.1L7 8.5l3.8-1.4L12 3Zm6.5 9.5.7 2.3 2.3.7-2.3.7-.7 2.3-.7-2.3-2.3-.7 2.3-.7.7-2.3ZM5 14l.8 2.7 2.7.8-2.7.8L5 21l-.8-2.7-2.7-.8 2.7-.8L5 14Z" />
                    </svg>
                  </div>

                  <div>
                    <p class="text-[9px] font-bold uppercase tracking-widest text-blue-600">
                      Your Benefits
                    </p>

                    <p class="mt-0.5 text-sm font-black text-slate-900">
                      Personalized offers
                    </p>
                  </div>
                </div>

                <div class="grid grid-cols-2 gap-2">
                  <router-link v-for="item in offersMenu" :key="item.title" :to="item.path"
                    class="rounded-xl border border-transparent p-3 transition hover:border-blue-100 hover:bg-blue-50/60">
                    <p class="text-xs font-bold text-slate-800">
                      {{ item.title }}
                    </p>

                    <p class="mt-1 text-[9px] leading-4 text-slate-400">
                      {{ item.description }}
                    </p>
                  </router-link>
                </div>
              </div>
            </div>
          </div>

          <!-- =================================================
               SUPPORT
               ================================================= -->

          <div class="group relative">
            <router-link to="/support"
              class="relative z-20 flex items-center gap-2 rounded-xl px-3.5 py-2.5 text-[13px] font-semibold text-slate-600 transition-all duration-200 hover:bg-white hover:text-cyan-600">
              <!-- Headset -->
              <svg class="h-[18px] w-[18px]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                  d="M4 13v-1a8 8 0 0 1 16 0v1M4 13h2a1.5 1.5 0 0 1 1.5 1.5V18H6A2 2 0 0 1 4 16v-3Zm16 0h-2a1.5 1.5 0 0 0-1.5 1.5V18H18a2 2 0 0 0 2-2v-3ZM17 18a5 5 0 0 1-5 3" />
              </svg>

              <span>Support</span>

              <svg class="h-3.5 w-3.5 text-slate-400 transition-transform duration-200 group-hover:rotate-180"
                fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="m6 9 6 6 6-6" />
              </svg>
            </router-link>

            <div
              class="pointer-events-none invisible absolute right-0 top-full z-[1200] w-[470px] translate-y-1 pt-3 opacity-0 transition-all duration-200 group-hover:pointer-events-auto group-hover:visible group-hover:translate-y-0 group-hover:opacity-100">
              <div
                class="overflow-hidden rounded-2xl border border-slate-200 bg-white p-4 shadow-[0_20px_60px_rgba(15,23,42,0.15)]">
                <div class="mb-3 flex items-center gap-3 rounded-xl bg-cyan-50 px-4 py-3">
                  <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-white text-cyan-600">
                    <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                        d="M4 13v-1a8 8 0 0 1 16 0v1M4 13h2a1.5 1.5 0 0 1 1.5 1.5V18H6A2 2 0 0 1 4 16v-3Zm16 0h-2a1.5 1.5 0 0 0-1.5 1.5V18H18a2 2 0 0 0 2-2v-3ZM17 18a5 5 0 0 1-5 3" />
                    </svg>
                  </div>

                  <div>
                    <p class="text-[9px] font-bold uppercase tracking-widest text-cyan-600">
                      Customer Care
                    </p>

                    <p class="mt-0.5 text-sm font-black text-slate-900">
                      How can we help?
                    </p>
                  </div>
                </div>

                <div class="grid grid-cols-2 gap-2">
                  <router-link v-for="item in supportMenu" :key="item.title" :to="item.path"
                    class="rounded-xl border border-transparent p-3 transition hover:border-cyan-100 hover:bg-cyan-50/60">
                    <p class="text-xs font-bold text-slate-800">
                      {{ item.title }}
                    </p>

                    <p class="mt-1 text-[9px] leading-4 text-slate-400">
                      {{ item.description }}
                    </p>
                  </router-link>
                </div>
              </div>
            </div>
          </div>
        </nav>

        <!-- =================================================
             RIGHT CONTROLS
             ================================================= -->

        <div class="ml-auto flex items-center gap-1.5 sm:gap-2">

          <!-- Search -->
          <form @submit.prevent="onSearch" class="hidden md:block">
            <div class="relative w-44 xl:w-52">
              <input v-model="searchQuery" type="text" placeholder="Search hardware"
                class="h-10 w-full rounded-xl border border-slate-200 bg-slate-50/80 pl-10 pr-9 text-[12px] font-medium text-slate-800 outline-none transition-all placeholder:text-slate-400 focus:border-blue-400 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />

              <svg class="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" fill="none"
                viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                  d="m21 21-4.35-4.35m2.35-5.15a7.5 7.5 0 1 1-15 0 7.5 7.5 0 0 1 15 0Z" />
              </svg>

              <button type="submit"
                class="absolute right-2 top-1/2 flex -translate-y-1/2 items-center justify-center text-slate-400 transition hover:text-blue-600"
                aria-label="Search">
                <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="m9 5 7 7-7 7" />
                </svg>
              </button>
            </div>
          </form>

          <!-- Cart -->
          <router-link to="/cart" title="Shopping Cart"
            class="relative flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-600 transition-all duration-200 hover:border-blue-300 hover:bg-slate-50 hover:text-blue-600">
            <svg class="h-[19px] w-[19px]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                d="M3 4h2l1.7 10.2a2 2 0 0 0 2 1.8h7.5a2 2 0 0 0 2-1.6L20 8H6" />
              <circle cx="10" cy="20" r="1.2" stroke-width="1.7" />
              <circle cx="18" cy="20" r="1.2" stroke-width="1.7" />
            </svg>

            <span v-if="cartDisplayCount > 0"
              class="absolute -right-1.5 -top-1.5 flex min-h-[18px] min-w-[18px] items-center justify-center rounded-full border-2 border-white bg-blue-600 px-1 text-[9px] font-black text-white shadow-sm">
              {{ cartDisplayCount }}
            </span>
          </router-link>

          <!-- Wishlist -->
          <router-link to="/wishlist" title="Wishlist"
            class="relative flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-600 transition-all duration-200 hover:border-blue-300 hover:bg-slate-50 hover:text-blue-600">
            <svg class="h-[19px] w-[19px]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                d="M20.8 8.8c0-2.8-2.2-5-5-5-1.7 0-3.2.8-4.1 2.1C10.8 4.6 9.3 3.8 7.6 3.8c-2.8 0-5 2.2-5 5 0 5.2 9.1 10.9 9.1 10.9s9.1-5.7 9.1-10.9Z" />
            </svg>

            <span v-if="wishlistCount > 0"
              class="absolute -right-1.5 -top-1.5 flex min-h-[18px] min-w-[18px] items-center justify-center rounded-full border-2 border-white bg-blue-600 px-1 text-[9px] font-black text-white shadow-sm">
              {{ wishlistCount }}
            </span>
          </router-link>

          <!-- Profile -->
          <router-link to="/profile" title="My Profile"
            class="relative hidden h-10 w-10 shrink-0 overflow-hidden rounded-xl border-2 border-white ring-1 ring-slate-200 transition-all duration-200 hover:ring-blue-400 sm:block">
            <img :src="userAvatar" alt="User profile" class="h-full w-full object-cover" @error="handleImageError" />

            <span
              class="absolute bottom-0.5 right-0.5 h-2.5 w-2.5 rounded-full border-2 border-white bg-emerald-500"></span>
          </router-link>

          <!-- Mobile Menu -->
          <button type="button" @click="toggleMobileMenu"
            class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-600 transition-all hover:border-blue-300 hover:text-blue-600 xl:hidden"
            aria-label="Toggle menu" :aria-expanded="isMobileMenuOpen">
            <svg v-if="!isMobileMenuOpen" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M4 6h16M4 12h16M4 18h16" />
            </svg>

            <svg v-else class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M6 6l12 12M18 6L6 18" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- =====================================================
         MOBILE NAV (scrolls inside when it is taller than the screen)
         ===================================================== -->

    <div v-if="isMobileMenuOpen"
      class="max-h-[calc(100vh-70px)] overflow-y-auto overscroll-contain border-t border-slate-200/80 bg-white/95 backdrop-blur-2xl supports-[height:100dvh]:max-h-[calc(100dvh-70px)] xl:hidden">
      <div class="mx-auto max-w-7xl px-4 py-4">

        <!-- Mobile Search (16px text so iOS does not zoom in on focus) -->
        <form @submit.prevent="onSearch" class="mb-4">
          <div class="relative">
            <input v-model="searchQuery" type="text" enterkeyhint="search" placeholder="Search hardware..."
              class="h-11 w-full rounded-xl border border-slate-200 bg-slate-50 pl-10 pr-4 text-base text-slate-800 outline-none placeholder:text-slate-400 focus:border-blue-400 focus:bg-white focus:ring-4 focus:ring-blue-500/10 sm:text-sm" />

            <svg class="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" fill="none"
              viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                d="m21 21-4.35-4.35m2.35-5.15a7.5 7.5 0 1 1-15 0 7.5 7.5 0 0 1 15 0Z" />
            </svg>
          </div>
        </form>

        <nav class="space-y-1">

          <!-- MOBILE PRODUCTS -->
          <div>
            <div class="flex items-center">
              <router-link to="/catalog" @click="closeMobileMenu"
                class="flex flex-1 items-center gap-3 rounded-l-xl px-4 py-3 text-sm font-semibold text-slate-700 transition hover:bg-blue-50 hover:text-blue-600">
                <svg class="h-[18px] w-[18px] shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                    d="M3.75 7.5 12 3l8.25 4.5M3.75 7.5V16.5L12 21l8.25-4.5V7.5M3.75 7.5 12 12l8.25-4.5M12 12v9" />
                </svg>

                <span>Products</span>
              </router-link>

              <button type="button" @click="toggleMobileDropdown('products')"
                class="flex h-[46px] w-12 items-center justify-center rounded-r-xl text-slate-400 transition hover:bg-blue-50 hover:text-blue-600"
                aria-label="Toggle products menu" :aria-expanded="mobileDropdown === 'products'">
                <svg class="h-4 w-4 transition-transform" :class="{
                  'rotate-180': mobileDropdown === 'products'
                }" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="m6 9 6 6 6-6" />
                </svg>
              </button>
            </div>

            <div v-if="mobileDropdown === 'products'" class="ml-3 border-l border-blue-100 pl-3 pb-2">
              <button type="button" @click="goToAllProducts"
                class="flex w-full items-center justify-between rounded-lg px-3 py-2.5 text-xs font-bold text-blue-600 hover:bg-blue-50">
                <span>All Products</span>
                <span>→</span>
              </button>

              <div v-if="categoriesLoading" class="space-y-1 px-3">
                <div v-for="item in 4" :key="item" class="h-8 animate-pulse rounded-lg bg-slate-100"></div>
              </div>

              <template v-else>
                <button v-for="category in categories" :key="category.categoryId" type="button"
                  @click="goToCategory(category.categoryId)"
                  class="flex w-full items-center justify-between rounded-lg px-3 py-2.5 text-xs font-semibold text-slate-600 transition hover:bg-blue-50 hover:text-blue-600">
                  <span>{{ category.categoryName }}</span>
                  <span class="text-slate-300">→</span>
                </button>
              </template>
            </div>
          </div>

          <!-- MOBILE DEALS -->
          <div>
            <div class="flex items-center">
              <router-link to="/deals" @click="closeMobileMenu"
                class="flex flex-1 items-center gap-3 rounded-l-xl px-4 py-3 text-sm font-semibold text-slate-700 transition hover:bg-amber-50 hover:text-amber-600">
                <svg class="h-[18px] w-[18px] shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                    d="M20.5 13.5 13.5 20.5a2 2 0 0 1-2.83 0L3.5 13.33V4.5h8.83l7.17 7.17a2 2 0 0 1 0 2.83Z" />
                  <circle cx="8" cy="8" r="1.2" stroke-width="1.7" />
                </svg>

                <span>Deals</span>
              </router-link>

              <button type="button" @click="toggleMobileDropdown('deals')"
                class="flex h-[46px] w-12 items-center justify-center rounded-r-xl text-slate-400 transition hover:bg-amber-50 hover:text-amber-600"
                aria-label="Toggle deals menu" :aria-expanded="mobileDropdown === 'deals'">
                <svg class="h-4 w-4 transition-transform" :class="{
                  'rotate-180': mobileDropdown === 'deals'
                }" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="m6 9 6 6 6-6" />
                </svg>
              </button>
            </div>

            <div v-if="mobileDropdown === 'deals'" class="ml-3 border-l border-amber-100 pl-3 pb-2">
              <router-link v-for="item in dealMenu" :key="item.title" :to="item.path" @click="closeMobileMenu"
                class="block rounded-lg px-3 py-2.5 transition hover:bg-amber-50">
                <span class="block text-[13px] font-bold text-slate-700">
                  {{ item.title }}
                </span>

                <span class="mt-0.5 block text-[11px] text-slate-400">
                  {{ item.description }}
                </span>
              </router-link>
            </div>
          </div>

          <!-- MOBILE BUILDS -->
          <div>
            <div class="flex items-center">
              <router-link to="/builds" @click="closeMobileMenu"
                class="flex flex-1 items-center gap-3 rounded-l-xl px-4 py-3 text-sm font-semibold text-slate-700 transition hover:bg-violet-50 hover:text-violet-600">
                <svg class="h-[18px] w-[18px] shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <rect x="7" y="7" width="10" height="10" rx="1.5" stroke-width="1.7" />
                  <path stroke-linecap="round" stroke-width="1.7"
                    d="M9.5 1.75v3M14.5 1.75v3M9.5 19.25v3M14.5 19.25v3M19.25 9.5h3M19.25 14.5h3M1.75 9.5h3M1.75 14.5h3" />
                </svg>

                <span>Custom Builds</span>
              </router-link>

              <button type="button" @click="toggleMobileDropdown('builds')"
                class="flex h-[46px] w-12 items-center justify-center rounded-r-xl text-slate-400 transition hover:bg-violet-50 hover:text-violet-600"
                aria-label="Toggle builds menu" :aria-expanded="mobileDropdown === 'builds'">
                <svg class="h-4 w-4 transition-transform" :class="{
                  'rotate-180': mobileDropdown === 'builds'
                }" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="m6 9 6 6 6-6" />
                </svg>
              </button>
            </div>

            <div v-if="mobileDropdown === 'builds'" class="ml-3 border-l border-violet-100 pl-3 pb-2">
              <router-link v-for="item in buildMenu" :key="item.title" :to="item.path" @click="closeMobileMenu"
                class="block rounded-lg px-3 py-2.5 transition hover:bg-violet-50">
                <span class="block text-[13px] font-bold text-slate-700">
                  {{ item.title }}
                </span>

                <span class="mt-0.5 block text-[11px] text-slate-400">
                  {{ item.description }}
                </span>
              </router-link>
            </div>
          </div>

          <!-- MOBILE COMPARE & LAB -->
          <div>
            <div class="flex items-center">
              <router-link to="/compare" @click="closeMobileMenu"
                class="flex flex-1 items-center gap-3 rounded-l-xl px-4 py-3 text-sm font-semibold text-slate-700 transition hover:bg-blue-50 hover:text-blue-600">
                <svg class="h-[18px] w-[18px] shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                    d="M3 6l3 1m0 0l-3 9a5.002 5.002 0 006.001 0M6 7l3 9M6 7l6-2m6 2l3-1m-3 1l-3 9a5.002 5.002 0 006.001 0M18 7l3 9m-3-9l-6-2m0-2v2m0 16V5m0 16H9m3 0h3" />
                </svg>

                <span>Compare & Lab</span>
              </router-link>

              <button type="button" @click="toggleMobileDropdown('compare')"
                class="flex h-[46px] w-12 items-center justify-center rounded-r-xl text-slate-400 transition hover:bg-blue-50 hover:text-blue-600"
                aria-label="Toggle compare menu" :aria-expanded="mobileDropdown === 'compare'">
                <svg class="h-4 w-4 transition-transform" :class="{
                  'rotate-180': mobileDropdown === 'compare'
                }" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="m6 9 6 6 6-6" />
                </svg>
              </button>
            </div>

            <div v-if="mobileDropdown === 'compare'" class="ml-3 border-l border-blue-100 pl-3 pb-2">
              <router-link v-for="item in compareMenu" :key="item.title" :to="item.path" @click="closeMobileMenu"
                class="block rounded-lg px-3 py-2.5 transition hover:bg-blue-50">
                <span class="block text-[13px] font-bold text-slate-700">
                  {{ item.title }}
                </span>

                <span class="mt-0.5 block text-[11px] text-slate-400">
                  {{ item.description }}
                </span>
              </router-link>
            </div>
          </div>

          <!-- MOBILE MY OFFERS -->
          <div>
            <div class="flex items-center">
              <router-link to="/myOffers" @click="closeMobileMenu"
                class="flex flex-1 items-center gap-3 rounded-l-xl px-4 py-3 text-sm font-semibold text-slate-700 transition hover:bg-blue-50 hover:text-blue-600">
                <svg class="h-[18px] w-[18px] shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                    d="m12 3 1.2 4.1L17 8.5l-3.8 1.4L12 14l-1.2-4.1L7 8.5l3.8-1.4L12 3Zm6.5 9.5.7 2.3 2.3.7-2.3.7-.7 2.3-.7-2.3-2.3-.7 2.3-.7.7-2.3ZM5 14l.8 2.7 2.7.8-2.7.8L5 21l-.8-2.7-2.7-.8 2.7-.8L5 14Z" />
                </svg>

                <span>My Offers</span>
              </router-link>

              <button type="button" @click="toggleMobileDropdown('offers')"
                class="flex h-[46px] w-12 items-center justify-center rounded-r-xl text-slate-400 transition hover:bg-blue-50 hover:text-blue-600"
                aria-label="Toggle offers menu" :aria-expanded="mobileDropdown === 'offers'">
                <svg class="h-4 w-4 transition-transform" :class="{
                  'rotate-180': mobileDropdown === 'offers'
                }" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="m6 9 6 6 6-6" />
                </svg>
              </button>
            </div>

            <div v-if="mobileDropdown === 'offers'" class="ml-3 border-l border-blue-100 pl-3 pb-2">
              <router-link v-for="item in offersMenu" :key="item.title" :to="item.path" @click="closeMobileMenu"
                class="block rounded-lg px-3 py-2.5 transition hover:bg-blue-50">
                <span class="block text-[13px] font-bold text-slate-700">
                  {{ item.title }}
                </span>

                <span class="mt-0.5 block text-[11px] text-slate-400">
                  {{ item.description }}
                </span>
              </router-link>
            </div>
          </div>

          <!-- MOBILE SUPPORT -->
          <div>
            <div class="flex items-center">
              <router-link to="/support" @click="closeMobileMenu"
                class="flex flex-1 items-center gap-3 rounded-l-xl px-4 py-3 text-sm font-semibold text-slate-700 transition hover:bg-cyan-50 hover:text-cyan-600">
                <svg class="h-[18px] w-[18px] shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                    d="M4 13v-1a8 8 0 0 1 16 0v1M4 13h2a1.5 1.5 0 0 1 1.5 1.5V18H6A2 2 0 0 1 4 16v-3Zm16 0h-2a1.5 1.5 0 0 0-1.5 1.5V18H18a2 2 0 0 0 2-2v-3ZM17 18a5 5 0 0 1-5 3" />
                </svg>

                <span>Support</span>
              </router-link>

              <button type="button" @click="toggleMobileDropdown('support')"
                class="flex h-[46px] w-12 items-center justify-center rounded-r-xl text-slate-400 transition hover:bg-cyan-50 hover:text-cyan-600"
                aria-label="Toggle support menu" :aria-expanded="mobileDropdown === 'support'">
                <svg class="h-4 w-4 transition-transform" :class="{
                  'rotate-180': mobileDropdown === 'support'
                }" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="m6 9 6 6 6-6" />
                </svg>
              </button>
            </div>

            <div v-if="mobileDropdown === 'support'" class="ml-3 border-l border-cyan-100 pl-3 pb-2">
              <router-link v-for="item in supportMenu" :key="item.title" :to="item.path" @click="closeMobileMenu"
                class="block rounded-lg px-3 py-2.5 transition hover:bg-cyan-50">
                <span class="block text-[13px] font-bold text-slate-700">
                  {{ item.title }}
                </span>

                <span class="mt-0.5 block text-[11px] text-slate-400">
                  {{ item.description }}
                </span>
              </router-link>
            </div>
          </div>

          <!-- Divider -->
          <div class="my-3 h-px bg-slate-200"></div>

          <!-- Profile -->
          <router-link to="/profile" @click="closeMobileMenu"
            class="flex items-center gap-3 rounded-xl bg-blue-50 px-4 py-3 text-blue-700">
            <img :src="userAvatar" alt="Profile" class="h-9 w-9 rounded-lg object-cover" @error="handleImageError" />

            <div>
              <div class="text-sm font-bold">
                My Profile
              </div>

              <div class="text-[10px] font-medium text-blue-500">
                Account settings
              </div>
            </div>
          </router-link>
        </nav>
      </div>
    </div>
  </header>
</template>

<style scoped>
/* =========================================================
   CLEAN DROPDOWN SCROLLBAR
   ========================================================= */

::-webkit-scrollbar {
  width: 5px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 999px;
}

::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
</style>