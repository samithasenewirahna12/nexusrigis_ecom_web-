<script setup lang="ts">

import {
  computed,
  onMounted,
  onUnmounted,
  ref,
  watch
} from 'vue'

import { useRoute } from 'vue-router'

import {
  Search,
  SlidersHorizontal,
  X,
  ChevronDown,
  Star,
  LayoutGrid,
  Grid3x3,
  PackageSearch,
  RotateCcw,
  ArrowUp,
  Check,
  Package,
  Cpu,
  Monitor,
  Headphones,
  Gamepad2,
  MemoryStick,
  HardDrive
} from 'lucide-vue-next'

import { useProductStore } from '../../stores/productStore'
import ProductCard from '../../components/product/ProductCard.vue'
import { getAllReviews, type ReviewResponse } from '../../services/reviewService'

import backgroundImage from '../../assets/images/vecteezy_smartwatch-collection-on-display-modern-wearable-technology_70382207.jpg'


/* =========================================================
   STORE / ROUTE
========================================================= */

const productStore = useProductStore()
const route = useRoute()


/* =========================================================
   TYPES
========================================================= */

interface ApiProduct {

  productId?: string | number
  id?: string | number

  name?: string
  brand?: string
  sku?: string

  price?: number | string

  category?:
  | string
  | {
    categoryId?: string | number
    id?: string | number
    categoryName?: string
    name?: string
  }
  | null

  categoryId?: string | number
  categoryName?: string

  stock?: number | string
  stockQty?: number | string

  description?: string | null
  discription?: string | null

  images?: string[] | null
  image?: string | null

  rating?: number | string

  reviewCount?: number | string
  reviewsCount?: number | string
  totalReviews?: number | string
  ratingCount?: number | string

  reviews?: {
    rating?: number | string
  }[]
}


interface NormalizedProduct {

  productId: string
  name: string
  brand: string

  price: number

  category: string
  categoryId: string

  description: string

  image: string
  images: string[]

  rating: number
  reviewCount: number
  reviews?: any[]

  inStock: boolean

  sku: string
  stock: number
}


/* =========================================================
   FILTER STATE
========================================================= */

const searchQuery = ref('')

const selectedCategory = ref('All')

const selectedBrands =
  ref<string[]>([])

const selectedStock = ref('All')

const minRating = ref(0)

const selectedSort =
  ref('featured')

const minPrice =
  ref<number | null>(null)

const maxPrice =
  ref<number | null>(null)

const showFilters = ref(false)

const dense = ref(false)

const PAGE = 24

const visibleLimit = ref(PAGE)


/* =========================================================
   IMAGE URL CLEANER
========================================================= */

function cleanImageUrl(
  value: unknown
): string {

  if (
    typeof value !== 'string' ||
    !value.trim()
  ) {
    return ''
  }

  let url = value.trim()


  /* Markdown */

  const markdownPosition =
    url.indexOf('](')

  if (
    (
      url.startsWith('[') ||
      url.startsWith('![')
    ) &&
    markdownPosition !== -1 &&
    url.endsWith(')')
  ) {

    url = url
      .slice(
        markdownPosition + 2,
        -1
      )
      .trim()
  }


  /* <url> */

  if (
    url.startsWith('<') &&
    url.endsWith('>')
  ) {

    url = url
      .slice(1, -1)
      .trim()
  }


  /* Quotes */

  if (
    (
      url.startsWith('"') &&
      url.endsWith('"')
    ) ||
    (
      url.startsWith("'") &&
      url.endsWith("'")
    )
  ) {

    url = url
      .slice(1, -1)
      .trim()
  }

  return url
}


/* =========================================================
   REVIEW STATE & AGGREGATION
========================================================= */

const rawReviews = ref<ReviewResponse[]>([])

const reviewsByProductId = computed(() => {
  const map = new Map<string, { ratings: number[]; count: number; avg: number; reviews: any[] }>()
  for (const r of rawReviews.value) {
    const rawId = r.productId != null ? String(r.productId).trim() : ''
    if (!rawId) continue
    const key = rawId.toLowerCase()
    const score = Number(r.rating)
    if (!Number.isFinite(score) || score <= 0) continue
    let entry = map.get(key)
    if (!entry) {
      entry = { ratings: [], count: 0, avg: 0, reviews: [] }
      map.set(key, entry)
    }
    entry.ratings.push(score)
    entry.count++
    entry.reviews.push(r)
  }
  for (const entry of map.values()) {
    if (entry.count > 0) {
      entry.avg = entry.ratings.reduce((a, b) => a + b, 0) / entry.count
    }
  }
  return map
})

/* =========================================================
   RATING
========================================================= */

function ratingOf(
  product: ApiProduct
): number {

  const id = String(
    product.productId ??
    product.id ??
    ''
  ).trim().toLowerCase()

  if (id && reviewsByProductId.value.has(id)) {
    return Math.min(5, Math.max(1, reviewsByProductId.value.get(id)!.avg))
  }

  const directRating =
    Number(product.rating)

  if (
    Number.isFinite(directRating) &&
    directRating > 0
  ) {

    return Math.min(
      5,
      Math.max(
        0,
        directRating
      )
    )
  }


  const reviewRatings =
    (product.reviews || [])
      .map((review) =>
        Number(review.rating)
      )
      .filter(
        (rating) =>
          Number.isFinite(rating) &&
          rating > 0
      )


  if (
    reviewRatings.length === 0
  ) {
    return 0
  }


  const total =
    reviewRatings.reduce(
      (sum, value) =>
        sum + value,
      0
    )


  return Math.min(
    5,
    Math.max(
      0,
      total / reviewRatings.length
    )
  )
}


/* =========================================================
   NORMALIZE PRODUCT
========================================================= */

function normalize(
  product: ApiProduct
): NormalizedProduct {

  const images = [
    product.image,
    ...(product.images || [])
  ]
    .map(cleanImageUrl)
    .filter(Boolean)


  const uniqueImages = [
    ...new Set(images)
  ]


  const categoryData =
    product.category


  let category = ''


  if (
    typeof categoryData === 'string'
  ) {

    category =
      categoryData.trim()

  } else if (
    categoryData &&
    typeof categoryData === 'object'
  ) {

    category =
      (
        categoryData.categoryName ??
        categoryData.name ??
        ''
      ).trim()
  }


  if (!category) {

    category =
      (
        product.categoryName ??
        ''
      ).trim()
  }


  if (!category) {
    category = 'Uncategorized'
  }


  let categoryId = ''


  if (
    product.categoryId !== undefined &&
    product.categoryId !== null
  ) {

    categoryId =
      String(product.categoryId)

  } else if (
    categoryData &&
    typeof categoryData === 'object'
  ) {

    categoryId =
      String(
        categoryData.categoryId ??
        categoryData.id ??
        ''
      )
  }


  const stock =
    Number(
      product.stockQty ??
      product.stock ??
      0
    ) || 0


  const id = String(
    product.productId ??
    product.id ??
    ''
  ).trim().toLowerCase()

  const revEntry = id ? reviewsByProductId.value.get(id) : undefined

  const reviewCountFromApi =
    Number(
      product.reviewCount ??
      product.reviewsCount ??
      product.totalReviews ??
      product.ratingCount ??
      0
    )

  const reviewCount =
    revEntry
      ? revEntry.count
      : (reviewCountFromApi > 0
        ? reviewCountFromApi
        : product.reviews?.length ?? 0)

  const productRating =
    revEntry && revEntry.count > 0
      ? revEntry.avg
      : ratingOf(product)

  const productReviews =
    revEntry
      ? revEntry.reviews
      : (product.reviews || [])

  return {

    productId: String(
      product.productId ??
      product.id ??
      ''
    ),

    name: String(
      product.name ||
      'Unnamed Product'
    ).trim(),

    brand: String(
      product.brand ||
      'Unknown Brand'
    ).trim(),

    price:
      Number(product.price) || 0,

    category,
    categoryId,

    description: String(
      product.description ??
      product.discription ??
      ''
    ).trim(),

    image:
      uniqueImages[0] || '',

    images:
      uniqueImages,

    rating:
      productRating,

    reviewCount,

    reviews:
      productReviews,

    inStock:
      stock > 0,

    sku: String(
      product.sku || ''
    ).trim(),

    stock

  }
}


/* =========================================================
   PRODUCTS
========================================================= */

const products =
  computed<NormalizedProduct[]>(
    () => {
      const items =
        (productStore.products ?? []) as ApiProduct[]

      return items
        .map((product) => normalize(product))
        .filter(
          (product) =>
            product.productId !== ''
        )
    }
  )


/* =========================================================
   CATEGORY OPTIONS
========================================================= */

const categoryOptions =
  computed(() => {

    const counts =
      new Map<string, number>()


    for (
      const product of products.value
    ) {

      if (
        product.category ===
        'Uncategorized'
      ) {
        continue
      }


      const current =
        counts.get(
          product.category
        ) || 0


      counts.set(
        product.category,
        current + 1
      )
    }


    return [
      ...counts.entries()
    ]
      .sort((a, b) =>
        a[0].localeCompare(b[0])
      )
      .map(
        ([name, count]) => ({
          name,
          count
        })
      )
  })


/* =========================================================
   BRAND OPTIONS
========================================================= */

const brandOptions =
  computed(() => {

    const counts =
      new Map<string, number>()


    for (
      const product of products.value
    ) {

      const current =
        counts.get(
          product.brand
        ) || 0


      counts.set(
        product.brand,
        current + 1
      )
    }


    return [
      ...counts.entries()
    ]
      .sort(
        (a, b) =>
          b[1] - a[1]
      )
      .map(
        ([name, count]) => ({
          name,
          count
        })
      )
  })


/* =========================================================
   PRICE BOUNDS
========================================================= */

const priceBounds =
  computed(() => {

    const prices: number[] =
      products.value
        .map((product: ApiProduct) => {
          const value = product.price
          return typeof value === 'number'
            ? value
            : Number(value ?? 0)
        })
        .filter(
          (price: number) =>
            price > 0
        )


    return {

      min:
        prices.length > 0
          ? Math.min(...prices)
          : 0,

      max:
        prices.length > 0
          ? Math.max(...prices)
          : 0

    }
  })


/* =========================================================
   STOCK OPTIONS
========================================================= */

const stockOptions = [
  'All',
  'In Stock',
  'Low Stock',
  'Out of Stock'
]


/* =========================================================
   SORT OPTIONS
========================================================= */

const sortOptions = [

  {
    v: 'featured',
    l: 'Featured'
  },

  {
    v: 'name',
    l: 'Name: A–Z'
  },

  {
    v: 'price-low',
    l: 'Price: Low to High'
  },

  {
    v: 'price-high',
    l: 'Price: High to Low'
  },

  {
    v: 'rating',
    l: 'Highest Rated'
  },

  {
    v: 'reviews',
    l: 'Most Reviewed'
  }

]


/* =========================================================
   CATEGORY ICON
========================================================= */

function getCategoryIcon(
  category: string
) {

  const name =
    category.toLowerCase()


  if (
    name.includes('gpu') ||
    name.includes('graphic') ||
    name.includes('graphics') ||
    name.includes('processor') ||
    name.includes('cpu')
  ) {
    return Cpu
  }


  if (
    name.includes('monitor') ||
    name.includes('display') ||
    name.includes('screen')
  ) {
    return Monitor
  }


  if (
    name.includes('headphone') ||
    name.includes('headset') ||
    name.includes('audio') ||
    name.includes('speaker')
  ) {
    return Headphones
  }


  if (
    name.includes('gaming') ||
    name.includes('game') ||
    name.includes('console')
  ) {
    return Gamepad2
  }


  if (
    name.includes('memory') ||
    name.includes('ram')
  ) {
    return MemoryStick
  }


  if (
    name.includes('storage') ||
    name.includes('ssd') ||
    name.includes('hdd') ||
    name.includes('hard')
  ) {
    return HardDrive
  }


  return Package
}


/* =========================================================
   SEARCH
========================================================= */

const search =
  computed(() => {

    return searchQuery.value
      .trim()
      .toLowerCase()

  })


/* =========================================================
   FILTERED PRODUCTS
========================================================= */

const filtered =
  computed(() => {

    const minimum =
      Number(minPrice.value) || 0

    const maximum =
      Number(maxPrice.value) || 0


    const result =
      products.value.filter(
        (product: NormalizedProduct) => {


          /* SEARCH */

          if (
            search.value &&
            !`${product.name} ${product.brand} ${product.sku} ${product.category} ${product.description}`
              .toLowerCase()
              .includes(search.value)
          ) {
            return false
          }


          /* CATEGORY */

          if (
            selectedCategory.value !==
            'All' &&
            product.category !==
            selectedCategory.value
          ) {
            return false
          }


          /* BRAND */

          if (
            selectedBrands.value.length > 0 &&
            !selectedBrands.value.includes(
              product.brand
            )
          ) {
            return false
          }


          /* STOCK */

          if (
            selectedStock.value ===
            'In Stock' &&
            product.stock <= 5
          ) {
            return false
          }


          if (
            selectedStock.value ===
            'Low Stock' &&
            (
              product.stock <= 0 ||
              product.stock > 5
            )
          ) {
            return false
          }


          if (
            selectedStock.value ===
            'Out of Stock' &&
            product.stock > 0
          ) {
            return false
          }


          /* RATING */

          if (
            minRating.value > 0 &&
            product.rating <
            minRating.value
          ) {
            return false
          }


          /* MIN PRICE */

          if (
            product.price <
            minimum
          ) {
            return false
          }


          /* MAX PRICE */

          if (
            maximum > 0 &&
            product.price >
            maximum
          ) {
            return false
          }


          return true
        }
      )


    const sorters: Record<
      string,
      (
        a: NormalizedProduct,
        b: NormalizedProduct
      ) => number
    > = {


      'price-low':
        (a, b) =>
          a.price - b.price,


      'price-high':
        (a, b) =>
          b.price - a.price,


      rating:
        (a, b) =>
          b.rating - a.rating,


      reviews:
        (a, b) =>
          b.reviewCount -
          a.reviewCount,


      name:
        (a, b) =>
          a.name.localeCompare(
            b.name
          )

    }


    const sorter =
      sorters[
      selectedSort.value
      ]


    if (sorter) {
      result.sort(sorter)
    }


    return result
  })


/* =========================================================
   SHOWN PRODUCTS
========================================================= */

const shown =
  computed(() => {

    return filtered.value.slice(
      0,
      visibleLimit.value
    )

  })


const hasMore =
  computed(() => {

    return (
      filtered.value.length >
      visibleLimit.value
    )

  })


/* =========================================================
   ACTIVE FILTER CHIPS
========================================================= */

const activeChips =
  computed(() => {

    const chips: {
      label: string
      clear: () => void
    }[] = []


    if (search.value) {

      chips.push({

        label:
          `Search: ${searchQuery.value.trim()}`,

        clear: () => {
          searchQuery.value = ''
        }

      })
    }


    if (
      selectedCategory.value !==
      'All'
    ) {

      chips.push({

        label:
          selectedCategory.value,

        clear: () => {
          selectedCategory.value = 'All'
        }

      })
    }


    for (
      const brand of selectedBrands.value
    ) {

      chips.push({

        label: brand,

        clear: () => {

          selectedBrands.value =
            selectedBrands.value.filter(
              (item) =>
                item !== brand
            )

        }

      })
    }


    if (
      selectedStock.value !==
      'All'
    ) {

      chips.push({

        label:
          selectedStock.value,

        clear: () => {
          selectedStock.value = 'All'
        }

      })
    }


    if (
      minRating.value
    ) {

      chips.push({

        label:
          `${minRating.value}★ & up`,

        clear: () => {
          minRating.value = 0
        }

      })
    }


    if (
      minPrice.value ||
      maxPrice.value
    ) {

      chips.push({

        label:
          `${fmt(minPrice.value || 0)} – ${maxPrice.value
            ? fmt(maxPrice.value)
            : 'Any'
          } LKR`,

        clear: () => {

          minPrice.value = null
          maxPrice.value = null

        }

      })
    }


    return chips
  })


/* =========================================================
   ACTIVE FILTERS
========================================================= */

const hasActive =
  computed(() => {

    return (
      activeChips.value.length > 0 ||
      selectedSort.value !==
      'featured'
    )

  })


/* =========================================================
   RESET
========================================================= */

function reset() {

  searchQuery.value = ''

  selectedCategory.value =
    'All'

  selectedBrands.value = []

  selectedStock.value =
    'All'

  minRating.value = 0

  selectedSort.value =
    'featured'

  minPrice.value = null
  maxPrice.value = null
}


/* =========================================================
   BRAND TOGGLE
========================================================= */

function toggleBrand(
  brand: string
) {

  if (
    selectedBrands.value.includes(
      brand
    )
  ) {

    selectedBrands.value =
      selectedBrands.value.filter(
        (item) =>
          item !== brand
      )

    return
  }


  selectedBrands.value = [
    ...selectedBrands.value,
    brand
  ]
}


/* =========================================================
   FORMAT PRICE
========================================================= */

function fmt(
  value: number
): string {

  return new Intl.NumberFormat(
    'en-LK',
    {
      maximumFractionDigits: 0
    }
  ).format(
    Number(value) || 0
  )
}


/* =========================================================
   RESET PAGINATION
========================================================= */

watch(
  [
    search,
    selectedCategory,
    selectedBrands,
    selectedStock,
    minRating,
    selectedSort,
    minPrice,
    maxPrice
  ],
  () => {

    visibleLimit.value =
      PAGE

  },
  {
    deep: true
  }
)


/* =========================================================
   APPLY ROUTE FILTERS
========================================================= */

function applyRoute() {

  const query =
    route.query


  if (
    typeof query.search ===
    'string'
  ) {

    searchQuery.value =
      query.search

  }


  if (
    typeof query.categoryId ===
    'string'
  ) {

    const hit =
      products.value.find(
        (product: NormalizedProduct) =>
          product.categoryId ===
          query.categoryId ||
          product.category ===
          query.categoryId
      )


    if (hit) {

      selectedCategory.value =
        hit.category

    }

  }


  if (query.maxPrice) {

    maxPrice.value =
      Number(query.maxPrice) ||
      null

  }
}


/* =========================================================
   GRID
========================================================= */

const grid =
  computed(() => {

    const base = `
      grid
      gap-2.5
      min-[420px]:gap-3
      sm:gap-4
      grid-cols-2
      sm:grid-cols-3
    `.replace(
      /\s+/g,
      ' '
    )


    if (dense.value) {

      return `
        ${base}
        lg:grid-cols-4
        xl:grid-cols-5
        2xl:grid-cols-6
      `.replace(
        /\s+/g,
        ' '
      )

    }


    return `
      ${base}
      xl:grid-cols-4
      2xl:grid-cols-5
    `.replace(
      /\s+/g,
      ' '
    )
  })


/* =========================================================
   BACK TO TOP
========================================================= */

const showTop = ref(false)


function handleScroll() {

  showTop.value =
    window.scrollY > 600
}


function toTop() {

  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })
}


/* =========================================================
   FILTER SCROLL LOCK
========================================================= */

watch(
  showFilters,
  (visible) => {

    document.body.style.overflow =
      visible
        ? 'hidden'
        : ''

  }
)


/* =========================================================
   LOAD
========================================================= */

onMounted(
  async () => {

    const [pRes, rRes] = await Promise.allSettled([
      productStore.loadProducts(),
      getAllReviews()
    ])

    if (rRes.status === 'fulfilled' && Array.isArray(rRes.value)) {
      rawReviews.value = rRes.value
    }

    applyRoute()

    window.addEventListener(
      'scroll',
      handleScroll,
      {
        passive: true
      }
    )

  }
)


/* =========================================================
   UNMOUNT
========================================================= */

onUnmounted(() => {

  window.removeEventListener(
    'scroll',
    handleScroll
  )

  document.body.style.overflow =
    ''
})


/* =========================================================
   ROUTE WATCH
========================================================= */

watch(
  () => route.query,
  () => {

    applyRoute()

  }
)

</script>


<template>

  <div class="catalog-page min-h-screen bg-slate-100">

    <!-- =====================================================
         HERO
    ====================================================== -->

    <section class="catalog-hero">

      <div class="hero-background" :style="{
        backgroundImage:
          `url(${backgroundImage})`
      }"></div>

      <div class="hero-overlay"></div>


      <div class="relative z-10 w-full px-3 py-6 sm:px-6 sm:py-8 lg:px-10 xl:px-14 2xl:px-20">

        <!-- HEADER -->

        <div class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">

          <div>

            <div
              class="flex items-center gap-2 text-[9px] font-bold uppercase tracking-[0.18em] text-blue-300 sm:text-[11px]">

              <span class="h-1.5 w-1.5 rounded-full bg-blue-400"></span>

              NEXUSRIGS

              <span class="text-slate-500">
                /
              </span>

              Product Catalog

            </div>


            <h1 class="mt-2 text-2xl font-black tracking-tight text-white sm:mt-3 sm:text-4xl lg:text-5xl">
              All Products
            </h1>


            <p class="mt-2 max-w-2xl text-xs leading-5 text-slate-300 sm:mt-3 sm:text-base sm:leading-6">
              Explore our complete selection
              of gaming hardware, components,
              peripherals and electronics.
            </p>

          </div>


          <!-- STATS -->

          <div
            class="grid w-full grid-cols-3 overflow-hidden rounded-xl border border-white/10 bg-white/5 backdrop-blur-sm sm:w-auto">

            <div class="hero-metric">

              <strong>
                {{ products.length }}
              </strong>

              <span>
                Products
              </span>

            </div>


            <div class="hero-metric">

              <strong>
                {{ categoryOptions.length }}
              </strong>

              <span>
                Categories
              </span>

            </div>


            <div class="hero-metric">

              <strong>
                {{ brandOptions.length }}
              </strong>

              <span>
                Brands
              </span>

            </div>

          </div>

        </div>


        <!-- SEARCH -->

        <div class="mt-5 max-w-4xl sm:mt-8">

          <div
            class="relative overflow-hidden rounded-xl border border-slate-200/50 bg-white shadow-2xl shadow-black/20">

            <Search
              class="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400 sm:left-4 sm:h-5 sm:w-5" />


            <input v-model="searchQuery" type="text" placeholder="Search products, brands, SKU or category..."
              aria-label="Search products"
              class="h-12 w-full bg-white pl-10 pr-10 text-xs font-medium text-slate-800 outline-none placeholder:text-slate-400 focus:ring-4 focus:ring-blue-500/10 sm:h-14 sm:pl-12 sm:pr-12 sm:text-sm" />


            <button v-if="searchQuery" type="button" aria-label="Clear search"
              class="absolute right-2.5 top-1/2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-slate-700"
              @click="
                searchQuery = ''
                ">

              <X class="h-4 w-4" />

            </button>

          </div>

        </div>

      </div>

    </section>


    <!-- =====================================================
         CATEGORY NAVIGATION
    ====================================================== -->

    <section class="border-b border-slate-200 bg-white">

      <div class="w-full px-3 sm:px-6 lg:px-10 xl:px-14 2xl:px-20">

        <div class="flex items-center gap-1.5 overflow-x-auto py-2.5 scrollbar-none sm:gap-2 sm:py-3">

          <!-- ALL -->

          <button type="button" class="catalog-category" :class="selectedCategory === 'All'
            ? 'catalog-category-active'
            : ''
            " @click="
              selectedCategory = 'All'
              ">

            <LayoutGrid class="h-4 w-4 shrink-0" :stroke-width="1.8" />

            <span>
              All Products
            </span>

            <span class="catalog-category-count">
              {{ products.length }}
            </span>

          </button>


          <!-- CATEGORIES -->

          <button v-for="category in categoryOptions" :key="category.name" type="button" class="catalog-category"
            :class="selectedCategory === category.name
              ? 'catalog-category-active'
              : ''
              " @click="
                selectedCategory =
                category.name
                ">

            <component :is="getCategoryIcon(
              category.name
            )
              " class="h-4 w-4 shrink-0" :stroke-width="1.8" />

            <span class="max-w-28 truncate sm:max-w-36">
              {{ category.name }}
            </span>

            <span class="catalog-category-count">
              {{ category.count }}
            </span>

          </button>

        </div>

      </div>

    </section>


    <!-- =====================================================
         MAIN
    ====================================================== -->

    <main class="w-full px-3 py-4 sm:px-6 sm:py-6 lg:px-10 lg:py-8 xl:px-14 2xl:px-20">

      <div class="grid gap-4 lg:grid-cols-[270px_minmax(0,1fr)] lg:gap-6">

        <!-- =================================================
             FILTER SIDEBAR
        ================================================== -->

        <aside :class="showFilters
          ? 'fixed inset-0 z-[80] flex items-end bg-slate-950/45 backdrop-blur-sm lg:static lg:block lg:bg-transparent lg:backdrop-blur-none'
          : 'hidden lg:block'
          " @click.self="
            showFilters = false
            ">

          <div
            class="w-full max-h-[88vh] overflow-y-auto px-2 pb-2 sm:px-4 sm:pb-4 lg:max-h-none lg:overflow-visible lg:px-0 lg:pb-0">

            <div class="filter-card mx-auto lg:sticky lg:top-5">

              <!-- HEADER -->

              <div class="flex items-center justify-between border-b border-slate-200 px-4 py-3.5 sm:px-5">

                <div class="flex items-center gap-2.5">

                  <span class="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-50 text-blue-600">

                    <SlidersHorizontal class="h-4 w-4" />

                  </span>


                  <div>

                    <h2 class="text-sm font-bold text-slate-900">
                      Filters
                    </h2>

                    <p class="text-[10px] text-slate-400">
                      Refine results
                    </p>

                  </div>

                </div>


                <div class="flex items-center gap-2">

                  <button type="button" :disabled="!hasActive" class="filter-reset" @click="reset">
                    Reset
                  </button>


                  <button type="button"
                    class="flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 text-slate-500 lg:hidden"
                    @click="
                      showFilters = false
                      " aria-label="Close filters">
                    <X class="h-4 w-4" />
                  </button>

                </div>

              </div>


              <!-- CATEGORY -->

              <section class="filter-section">

                <h3 class="filter-heading">
                  Category
                </h3>


                <div class="mt-2 space-y-0.5">

                  <button type="button" class="filter-item" :class="selectedCategory === 'All'
                    ? 'filter-item-active'
                    : ''
                    " @click="
                      selectedCategory = 'All'
                      ">

                    <LayoutGrid class="h-3.5 w-3.5 shrink-0" />

                    <span class="filter-item-label">
                      All products
                    </span>

                    <span class="filter-item-count">
                      {{ products.length }}
                    </span>

                  </button>


                  <button v-for="category in categoryOptions" :key="category.name" type="button" class="filter-item"
                    :class="selectedCategory === category.name
                      ? 'filter-item-active'
                      : ''
                      " @click="
                        selectedCategory =
                        category.name
                        ">

                    <component :is="getCategoryIcon(
                      category.name
                    )
                      " class="h-3.5 w-3.5 shrink-0" :stroke-width="1.8" />

                    <span class="filter-item-label truncate">
                      {{ category.name }}
                    </span>

                    <span class="filter-item-count">
                      {{ category.count }}
                    </span>

                  </button>

                </div>

              </section>


              <!-- BRAND -->

              <section class="filter-section">

                <h3 class="filter-heading">
                  Brand
                </h3>


                <div class="mt-2 max-h-44 space-y-0.5 overflow-y-auto pr-1">

                  <button v-for="brand in brandOptions" :key="brand.name" type="button" class="brand-filter" @click="
                    toggleBrand(
                      brand.name
                    )
                    ">

                    <span :class="[
                      'brand-checkbox',
                      selectedBrands.includes(
                        brand.name
                      )
                        ? 'brand-checkbox-active'
                        : ''
                    ]">

                      <Check v-if="
                        selectedBrands.includes(
                          brand.name
                        )
                      " class="h-3 w-3" />

                    </span>


                    <span class="flex-1 truncate text-left">
                      {{ brand.name }}
                    </span>


                    <span class="filter-item-count">
                      {{ brand.count }}
                    </span>

                  </button>

                </div>

              </section>


              <!-- AVAILABILITY -->

              <section class="filter-section">

                <h3 class="filter-heading">
                  Availability
                </h3>


                <div class="mt-2 space-y-1">

                  <button v-for="stockOption in stockOptions" :key="stockOption" type="button"
                    class="availability-filter" :class="selectedStock === stockOption
                      ? 'availability-filter-active'
                      : ''
                      " @click="
                        selectedStock =
                        stockOption
                        ">

                    <span class="availability-dot" :class="{
                      'dot-all':
                        stockOption === 'All',
                      'dot-stock':
                        stockOption === 'In Stock',
                      'dot-low':
                        stockOption === 'Low Stock',
                      'dot-out':
                        stockOption === 'Out of Stock'
                    }"></span>


                    <span>
                      {{ stockOption }}
                    </span>


                    <Check v-if="
                      selectedStock ===
                      stockOption
                    " class="ml-auto h-3.5 w-3.5 text-blue-600" />

                  </button>

                </div>

              </section>


              <!-- PRICE -->

              <section class="filter-section">

                <h3 class="filter-heading">
                  Price Range
                </h3>


                <div class="mt-2 grid grid-cols-2 gap-2">

                  <input v-model.number="minPrice" type="number" min="0" placeholder="Min" class="price-input" />

                  <input v-model.number="maxPrice" type="number" min="0" placeholder="Max" class="price-input" />

                </div>


                <p class="mt-2 text-[10px] text-slate-400">
                  {{ fmt(priceBounds.min) }}
                  -
                  {{ fmt(priceBounds.max) }}
                  LKR
                </p>

              </section>


              <!-- RATING -->

              <section class="filter-section">

                <h3 class="filter-heading">
                  Customer Rating
                </h3>


                <div class="mt-2 space-y-1">

                  <button v-for="rating in [4, 3, 2]" :key="rating" type="button" class="rating-filter" :class="minRating === rating
                    ? 'rating-filter-active'
                    : ''
                    " @click="
                      minRating =
                      minRating === rating
                        ? 0
                        : rating
                      ">

                    <span class="flex items-center gap-0.5">

                      <Star v-for="n in 5" :key="n" class="h-3.5 w-3.5" :class="n <= rating
                        ? 'fill-current text-amber-400'
                        : 'text-slate-200'
                        " />

                    </span>


                    <span class="text-xs text-slate-500">
                      & up
                    </span>

                  </button>

                </div>

              </section>


              <!-- MOBILE APPLY -->

              <div class="border-t border-slate-200 p-3 lg:hidden">

                <button type="button"
                  class="flex h-10 w-full items-center justify-center rounded-xl bg-slate-900 text-xs font-bold text-white"
                  @click="
                    showFilters = false
                    ">
                  Apply Filters
                </button>

              </div>

            </div>

          </div>

        </aside>


        <!-- =================================================
             RESULTS
        ================================================== -->

        <section class="min-w-0">

          <!-- TOOLBAR -->

          <div class="mb-3 rounded-xl border border-slate-200 bg-white p-2.5 shadow-sm sm:p-3">

            <div class="flex flex-col gap-2.5 sm:flex-row sm:items-center sm:justify-between">

              <!-- LEFT -->

              <div class="flex min-w-0 items-center justify-between gap-2">

                <div class="flex items-center gap-2">

                  <button type="button" class="mobile-filter lg:hidden" @click="
                    showFilters = true
                    ">

                    <SlidersHorizontal class="h-4 w-4" />

                    Filters


                    <span v-if="activeChips.length"
                      class="flex h-5 min-w-5 items-center justify-center rounded-full bg-blue-600 px-1.5 text-[10px] font-bold text-white">
                      {{ activeChips.length }}
                    </span>

                  </button>


                  <p class="whitespace-nowrap text-[11px] text-slate-400">
                    Showing

                    <strong class="font-bold text-slate-700">
                      {{ shown.length }}
                    </strong>

                    of

                    <strong class="font-bold text-slate-700">
                      {{ filtered.length }}
                    </strong>
                  </p>

                </div>


                <!-- MOBILE VIEW -->

                <div class="flex items-center gap-1 sm:hidden">

                  <button type="button" class="view-button" :class="!dense
                    ? 'view-button-active'
                    : ''
                    " @click="
                      dense = false
                      " aria-label="Comfortable grid">
                    <LayoutGrid class="h-4 w-4" />
                  </button>


                  <button type="button" class="view-button" :class="dense
                    ? 'view-button-active'
                    : ''
                    " @click="
                      dense = true
                      " aria-label="Compact grid">
                    <Grid3x3 class="h-4 w-4" />
                  </button>

                </div>

              </div>


              <!-- RIGHT -->

              <div class="flex w-full items-center gap-2 sm:w-auto">

                <!-- SORT -->

                <div class="relative flex-1 sm:flex-none">

                  <select v-model="selectedSort" class="sort-control w-full sm:w-auto" aria-label="Sort products">

                    <option v-for="option in sortOptions" :key="option.v" :value="option.v">
                      Sort: {{ option.l }}
                    </option>

                  </select>


                  <ChevronDown
                    class="pointer-events-none absolute right-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" />

                </div>


                <!-- DESKTOP VIEW -->

                <div class="hidden items-center gap-1 rounded-lg border border-slate-200 bg-slate-50 p-1 sm:flex">

                  <button type="button" class="view-button" :class="!dense
                    ? 'view-button-active'
                    : ''
                    " @click="
                      dense = false
                      " aria-label="Comfortable grid">
                    <LayoutGrid class="h-4 w-4" />
                  </button>


                  <button type="button" class="view-button" :class="dense
                    ? 'view-button-active'
                    : ''
                    " @click="
                      dense = true
                      " aria-label="Compact grid">
                    <Grid3x3 class="h-4 w-4" />
                  </button>

                </div>

              </div>

            </div>

          </div>


          <!-- ACTIVE FILTERS -->

          <div v-if="activeChips.length" class="mb-4 flex flex-wrap items-center gap-1.5">

            <span class="mr-1 text-[10px] font-bold uppercase tracking-wider text-slate-400">
              Filters:
            </span>


            <button v-for="chip in activeChips" :key="chip.label" type="button" class="active-filter-chip" @click="
              chip.clear()
              ">

              <span class="max-w-[160px] truncate">
                {{ chip.label }}
              </span>

              <X class="h-3 w-3 shrink-0" />

            </button>


            <button type="button" class="clear-filters" @click="reset">
              Clear all
            </button>

          </div>


          <!-- PRODUCT AREA -->

          <div class="product-area">

            <!-- LOADING -->

            <div v-if="productStore.loading" :class="grid">

              <div v-for="n in 12" :key="n" class="skeleton-card">

                <div class="aspect-[4/3] bg-slate-100"></div>


                <div class="space-y-2.5 p-3 sm:space-y-3 sm:p-4">

                  <div class="h-2.5 w-1/3 rounded-full bg-slate-100"></div>

                  <div class="h-4 w-4/5 rounded-full bg-slate-100"></div>

                  <div class="h-3 w-2/3 rounded-full bg-slate-100"></div>

                  <div class="h-5 w-2/5 rounded-full bg-slate-100"></div>

                </div>

              </div>

            </div>


            <!-- PRODUCTS -->

            <template v-else-if="shown.length">

              <div :class="grid">

                <ProductCard v-for="product in shown" :key="product.productId" :product="product" />

              </div>


              <!-- LOAD MORE -->

              <div class="mt-6 border-t border-slate-200 pt-5 sm:mt-8 sm:pt-6">

                <div class="flex flex-col items-center gap-3 sm:flex-row sm:justify-between">

                  <p class="text-[11px] text-slate-400">
                    Showing

                    <strong class="text-slate-700">
                      {{ shown.length }}
                    </strong>

                    of

                    <strong class="text-slate-700">
                      {{ filtered.length }}
                    </strong>
                  </p>


                  <div class="flex w-full items-center gap-3 sm:w-auto">

                    <div class="h-1.5 flex-1 overflow-hidden rounded-full bg-slate-200 sm:w-36 sm:flex-none">

                      <div class="h-full rounded-full bg-blue-600 transition-all duration-500" :style="{
                        width:
                          (
                            shown.length /
                            Math.max(
                              filtered.length,
                              1
                            )
                          ) *
                          100 +
                          '%'
                      }"></div>

                    </div>


                    <button v-if="hasMore" type="button" class="load-more shrink-0" @click="
                      visibleLimit += PAGE
                      ">
                      Load more
                    </button>

                  </div>

                </div>

              </div>

            </template>


            <!-- EMPTY -->

            <div v-else class="empty-products">

              <div class="empty-products-icon">

                <PackageSearch class="h-8 w-8" />

              </div>


              <h3 class="mt-4 text-base font-bold text-slate-900 sm:text-lg">
                No products found
              </h3>


              <p class="mt-2 max-w-md text-center text-xs leading-5 text-slate-500 sm:text-sm sm:leading-6">
                No products match your current
                search and filter selection.
                Try changing your filters.
              </p>


              <button type="button" class="reset-button" @click="reset">

                <RotateCcw class="h-4 w-4" />

                Reset filters

              </button>

            </div>

          </div>

        </section>

      </div>

    </main>


    <!-- =====================================================
         BACK TO TOP
    ====================================================== -->

    <Transition name="fade">

      <button v-if="showTop" type="button" aria-label="Back to top" class="back-to-top" @click="toTop">

        <ArrowUp class="h-4 w-4" />

      </button>

    </Transition>

  </div>

</template>


<style scoped>
/* =========================================================
   GLOBAL
========================================================= */

.catalog-page {
  color: rgb(15 23 42);
}

.scrollbar-none {
  scrollbar-width: none;
}

.scrollbar-none::-webkit-scrollbar {
  display: none;
}


/* =========================================================
   HERO
========================================================= */

.catalog-hero {
  position: relative;
  overflow: hidden;
  min-height: 350px;
  background: #08111f;
}

.hero-background {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  opacity: 0.9;
}

.hero-overlay {
  position: absolute;
  inset: 0;

  background:
    linear-gradient(90deg,
      rgba(2, 6, 23, 0.96),
      rgba(2, 6, 23, 0.78));
}

.hero-metric {
  display: flex;
  min-width: 92px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.15rem;

  padding:
    0.8rem 1rem;

  border-right:
    1px solid rgb(255 255 255 / 0.08);
}

.hero-metric:last-child {
  border-right: 0;
}

.hero-metric strong {
  font-size: 1rem;
  font-weight: 800;
  color: white;
}

.hero-metric span {
  font-size: 0.625rem;
  color: rgb(148 163 184);
}


/* =========================================================
   CATEGORY NAVIGATION
========================================================= */

.catalog-category {
  display: inline-flex;
  min-height: 38px;
  flex-shrink: 0;
  align-items: center;
  gap: 0.5rem;

  border:
    1px solid transparent;

  border-radius:
    0.65rem;

  padding:
    0 0.7rem;

  font-size:
    0.72rem;

  font-weight:
    700;

  color:
    rgb(71 85 105);

  transition:
    color 0.15s ease,
    background 0.15s ease,
    border-color 0.15s ease;
}

.catalog-category:hover {
  background:
    rgb(248 250 252);

  color:
    rgb(15 23 42);
}

.catalog-category-active {
  border-color:
    rgb(191 219 254);

  background:
    rgb(239 246 255);

  color:
    rgb(29 78 216);
}

.catalog-category-count {
  display:
    inline-flex;

  min-width:
    20px;

  align-items:
    center;

  justify-content:
    center;

  border-radius:
    999px;

  background:
    rgb(241 245 249);

  padding:
    0.15rem 0.35rem;

  font-size:
    0.58rem;

  color:
    rgb(100 116 139);
}

.catalog-category-active .catalog-category-count {
  background:
    rgb(219 234 254);

  color:
    rgb(37 99 235);
}


/* =========================================================
   FILTER CARD
========================================================= */

.filter-card {
  overflow: hidden;

  border:
    1px solid rgb(226 232 240);

  border-radius:
    1rem;

  background:
    white;

  box-shadow:
    0 12px 30px -24px rgb(15 23 42 / 0.35);
}

.filter-reset {
  font-size:
    0.68rem;

  font-weight:
    700;

  color:
    rgb(37 99 235);
}

.filter-reset:hover {
  color:
    rgb(29 78 216);
}

.filter-reset:disabled {
  cursor:
    not-allowed;

  color:
    rgb(203 213 225);
}

.filter-section {
  padding:
    0.25rem 1rem 1rem;
}

.filter-heading {
  font-size:
    0.66rem;

  font-weight:
    800;

  text-transform:
    uppercase;

  letter-spacing:
    0.07em;

  color:
    rgb(100 116 139);
}

.filter-item,
.brand-filter,
.availability-filter {
  display:
    flex;

  width:
    100%;

  align-items:
    center;

  gap:
    0.55rem;

  border-radius:
    0.55rem;

  padding:
    0.48rem 0.5rem;

  font-size:
    0.74rem;

  color:
    rgb(71 85 105);

  transition:
    background 0.15s ease,
    color 0.15s ease;
}

.filter-item:hover,
.brand-filter:hover,
.availability-filter:hover {
  background:
    rgb(248 250 252);
}

.filter-item-active {
  background:
    rgb(239 246 255);

  color:
    rgb(29 78 216);

  font-weight:
    700;
}

.filter-item-label {
  flex:
    1;

  text-align:
    left;
}

.filter-item-count {
  margin-left:
    auto;

  font-size:
    0.6rem;

  color:
    rgb(148 163 184);
}


/* =========================================================
   BRAND CHECKBOX
========================================================= */

.brand-checkbox {
  display:
    flex;

  height:
    16px;

  width:
    16px;

  flex-shrink:
    0;

  align-items:
    center;

  justify-content:
    center;

  border:
    1px solid rgb(203 213 225);

  border-radius:
    4px;

  background:
    white;
}

.brand-checkbox-active {
  border-color:
    rgb(37 99 235);

  background:
    rgb(37 99 235);

  color:
    white;
}


/* =========================================================
   AVAILABILITY
========================================================= */

.availability-filter-active {
  background:
    rgb(239 246 255);

  color:
    rgb(29 78 216);

  font-weight:
    700;
}

.availability-dot {
  height:
    7px;

  width:
    7px;

  flex-shrink:
    0;

  border-radius:
    999px;
}

.dot-all {
  background:
    rgb(148 163 184);
}

.dot-stock {
  background:
    rgb(34 197 94);
}

.dot-low {
  background:
    rgb(245 158 11);
}

.dot-out {
  background:
    rgb(239 68 68);
}


/* =========================================================
   PRICE
========================================================= */

.price-input {
  height:
    38px;

  width:
    100%;

  border:
    1px solid rgb(226 232 240);

  border-radius:
    0.55rem;

  background:
    white;

  padding:
    0 0.7rem;

  font-size:
    0.75rem;

  color:
    rgb(30 41 59);

  outline:
    none;
}

.price-input:focus {
  border-color:
    rgb(147 197 253);

  box-shadow:
    0 0 0 3px rgb(59 130 246 / 0.08);
}


/* =========================================================
   RATING
========================================================= */

.rating-filter {
  display:
    flex;

  width:
    100%;

  align-items:
    center;

  gap:
    0.45rem;

  border-radius:
    0.55rem;

  padding:
    0.5rem 0.55rem;
}

.rating-filter:hover {
  background:
    rgb(248 250 252);
}

.rating-filter-active {
  background:
    rgb(255 247 237);
}


/* =========================================================
   MOBILE FILTER BUTTON
========================================================= */

.mobile-filter {
  display:
    inline-flex;

  height:
    36px;

  align-items:
    center;

  gap:
    0.4rem;

  border:
    1px solid rgb(226 232 240);

  border-radius:
    0.6rem;

  background:
    white;

  padding:
    0 0.7rem;

  font-size:
    0.7rem;

  font-weight:
    700;

  color:
    rgb(51 65 85);
}


/* =========================================================
   TOOLBAR
========================================================= */

.sort-control {
  height:
    36px;

  min-width:
    150px;

  appearance:
    none;

  border:
    1px solid rgb(226 232 240);

  border-radius:
    0.6rem;

  background:
    white;

  padding:
    0 2rem 0 0.7rem;

  font-size:
    0.69rem;

  font-weight:
    700;

  color:
    rgb(51 65 85);

  outline:
    none;
}

.sort-control:focus {
  border-color:
    rgb(147 197 253);

  box-shadow:
    0 0 0 3px rgb(59 130 246 / 0.08);
}

.view-button {
  display:
    flex;

  height:
    30px;

  width:
    30px;

  align-items:
    center;

  justify-content:
    center;

  border-radius:
    0.45rem;

  color:
    rgb(100 116 139);
}

.view-button:hover {
  background:
    white;
}

.view-button-active {
  background:
    white;

  color:
    rgb(37 99 235);

  box-shadow:
    0 1px 4px rgb(15 23 42 / 0.08);
}


/* =========================================================
   ACTIVE FILTERS
========================================================= */

.active-filter-chip {
  display:
    inline-flex;

  max-width:
    200px;

  align-items:
    center;

  gap:
    0.4rem;

  border:
    1px solid rgb(191 219 254);

  border-radius:
    999px;

  background:
    rgb(239 246 255);

  padding:
    0.35rem 0.6rem 0.35rem 0.7rem;

  font-size:
    0.63rem;

  font-weight:
    700;

  color:
    rgb(29 78 216);
}

.active-filter-chip:hover {
  background:
    rgb(219 234 254);
}

.clear-filters {
  margin-left:
    0.2rem;

  font-size:
    0.63rem;

  font-weight:
    700;

  color:
    rgb(37 99 235);
}

.clear-filters:hover {
  text-decoration:
    underline;

  text-underline-offset:
    3px;
}


/* =========================================================
   PRODUCT AREA
========================================================= */

.product-area {
  min-height:
    300px;
}


/* =========================================================
   SKELETON
========================================================= */

.skeleton-card {
  overflow:
    hidden;

  border:
    1px solid rgb(226 232 240);

  border-radius:
    0.9rem;

  background:
    white;

  animation:
    pulse-card 1.6s ease-in-out infinite;
}

@keyframes pulse-card {

  0%,
  100% {
    opacity:
      0.7;
  }

  50% {
    opacity:
      1;
  }

}


/* =========================================================
   LOAD MORE
========================================================= */

.load-more {
  height:
    36px;

  border:
    1px solid rgb(191 219 254);

  border-radius:
    0.6rem;

  background:
    white;

  padding:
    0 0.9rem;

  font-size:
    0.68rem;

  font-weight:
    800;

  color:
    rgb(29 78 216);
}

.load-more:hover {
  border-color:
    rgb(96 165 250);

  background:
    rgb(239 246 255);
}


/* =========================================================
   EMPTY
========================================================= */

.empty-products {
  display:
    flex;

  min-height:
    380px;

  flex-direction:
    column;

  align-items:
    center;

  justify-content:
    center;

  border:
    1px solid rgb(226 232 240);

  border-radius:
    1rem;

  background:
    white;

  padding:
    2rem 1rem;
}

.empty-products-icon {
  display:
    flex;

  height:
    58px;

  width:
    58px;

  align-items:
    center;

  justify-content:
    center;

  border-radius:
    1rem;

  background:
    rgb(239 246 255);

  color:
    rgb(37 99 235);
}

.reset-button {
  display:
    inline-flex;

  align-items:
    center;

  gap:
    0.5rem;

  margin-top:
    1.1rem;

  border-radius:
    0.6rem;

  background:
    rgb(15 23 42);

  padding:
    0.6rem 0.9rem;

  font-size:
    0.7rem;

  font-weight:
    700;

  color:
    white;
}

.reset-button:hover {
  background:
    rgb(30 41 59);
}


/* =========================================================
   BACK TO TOP
========================================================= */

.back-to-top {
  position:
    fixed;

  right:
    1rem;

  bottom:
    1rem;

  z-index:
    50;

  display:
    flex;

  height:
    40px;

  width:
    40px;

  align-items:
    center;

  justify-content:
    center;

  border:
    1px solid rgb(226 232 240);

  border-radius:
    0.7rem;

  background:
    rgb(15 23 42);

  color:
    white;

  box-shadow:
    0 10px 30px rgb(15 23 42 / 0.25);

  transition:
    background 0.15s ease,
    transform 0.15s ease;
}

.back-to-top:hover {
  background:
    rgb(30 41 59);

  transform:
    translateY(-2px);
}


/* =========================================================
   NUMBER INPUT
========================================================= */

input[type='number']::-webkit-inner-spin-button,
input[type='number']::-webkit-outer-spin-button {
  margin:
    0;

  -webkit-appearance:
    none;
}

input[type='number'] {
  -moz-appearance:
    textfield;
}


/* =========================================================
   TRANSITION
========================================================= */

.fade-enter-active,
.fade-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity:
    0;

  transform:
    translateY(8px);
}


/* =========================================================
   MOBILE
========================================================= */

@media (max-width: 639px) {

  .catalog-hero {
    min-height:
      0;
  }

  .hero-overlay {
    background:
      linear-gradient(180deg,
        rgba(2, 6, 23, 0.94),
        rgba(2, 6, 23, 0.84));
  }

  .hero-metric {
    min-width:
      0;

    padding:
      0.65rem 0.35rem;
  }

  .hero-metric strong {
    font-size:
      0.85rem;
  }

  .hero-metric span {
    font-size:
      0.54rem;
  }

  .sort-control {
    min-width:
      0;

    width:
      100%;
  }

  .active-filter-chip {
    max-width:
      160px;
  }

  .filter-card {
    max-width:
      520px;

    margin:
      0 auto;

    border-radius:
      1.25rem 1.25rem 0 0;

    box-shadow:
      0 -15px 40px rgb(15 23 42 / 0.15);
  }

}


/* =========================================================
   VERY SMALL PHONES
========================================================= */

@media (max-width: 380px) {

  .hero-metric {
    padding-left:
      0.2rem;

    padding-right:
      0.2rem;
  }

  .hero-metric strong {
    font-size:
      0.78rem;
  }

  .hero-metric span {
    font-size:
      0.5rem;
  }

  .mobile-filter {
    padding-left:
      0.55rem;

    padding-right:
      0.55rem;
  }

}


/* =========================================================
   REDUCED MOTION
========================================================= */

@media (prefers-reduced-motion: reduce) {

  *,
  *::before,
  *::after {
    animation-duration:
      0.01ms !important;

    animation-iteration-count:
      1 !important;

    scroll-behavior:
      auto !important;

    transition-duration:
      0.01ms !important;
  }

}
</style>