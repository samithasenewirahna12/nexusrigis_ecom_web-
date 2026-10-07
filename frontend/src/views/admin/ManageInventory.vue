<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminConfirmModal, { type ModalType, type AdminModalState } from '../../components/admin/AdminConfirmModal.vue'
import api from '../../services/api'

interface InventoryItem {
  id: number | string
  sku: string
  name: string
  category: string
  categoryId?: string
  description?: string
  brand?: string
  image?: string
  stock: number
  minStock: number
  price: number
  status: 'In Stock' | 'Low Stock' | 'Out of Stock'
  updatedAt: string
}

interface ApiProduct {
  productId: string | number
  name?: string
  price?: number
  brand?: string
  stockQty?: number
  images?: string[]
  category?: {
    categoryId?: string | number
    categoryName?: string
    name?: string
  } | null
  categoryId?: string | number
  categoryName?: string
  description?: string
}

const isLoading = ref(false)
const inventoryError = ref('')
const isSaving = ref(false)

const searchQuery = ref('')
const selectedCategory = ref('All')
const selectedStatus = ref('All')

const showStockModal = ref(false)
const selectedItem = ref<InventoryItem | null>(null)

const stockAction = ref<'add' | 'remove'>('add')
const stockQuantity = ref(1)

// =========================================================
// ALERT / CONFIRMATION MODAL STATE (ORDER MANAGEMENT DESIGN)
// =========================================================

const confirmModal = ref<AdminModalState>({
  show: false,
  type: 'warning',
  title: '',
  message: '',
  target: '',
  description: '',
  confirmText: 'OK',
  cancelText: '',
  showCancel: false,
  loading: false,
  onConfirm: () => {}
})

const showAlert = (title: string, message: string, type: ModalType = 'warning', description = '') => {
  confirmModal.value = {
    show: true,
    type,
    title,
    message,
    target: '',
    description,
    confirmText: 'OK',
    cancelText: '',
    showCancel: false,
    loading: false,
    onConfirm: () => {
      confirmModal.value.show = false
    }
  }
}

// =========================================================
// HELPERS
// =========================================================

const getApiList = <T>(data: unknown, key: string): T[] => {
  if (Array.isArray(data)) {
    return data as T[]
  }

  if (data && typeof data === 'object' && 'data' in data) {
    return getApiList<T>(
      (data as { data: unknown }).data,
      key
    )
  }

  if (data && typeof data === 'object' && key in data) {
    const list = (data as Record<string, unknown>)[key]

    return Array.isArray(list)
      ? list as T[]
      : []
  }

  return []
}

const getStatus = (
  stock: number,
  minStock: number
): InventoryItem['status'] => {
  if (stock <= 0) {
    return 'Out of Stock'
  }

  if (stock <= minStock) {
    return 'Low Stock'
  }

  return 'In Stock'
}

// =========================================================
// LKR CURRENCY FORMAT
// =========================================================

const formatCurrency = (value: number) => {
  return `LKR ${Number(value || 0).toLocaleString('en-LK', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })}`
}

// =========================================================
// MAP PRODUCT
// =========================================================

const mapProduct = (
  product: ApiProduct
): InventoryItem => {
  const stock = Number(product.stockQty) || 0
  const minStock = 5

  return {
    id: product.productId,

    sku: `PROD-${product.productId}`,

    name: product.name || '',

    category:
      product.category?.categoryName ||
      product.category?.name ||
      product.categoryName ||
      'Uncategorized',

    categoryId: String(
      product.category?.categoryId ??
      product.categoryId ??
      ''
    ),

    description: product.description || '',

    brand: product.brand || '',

    image: product.images?.[0],

    stock,

    minStock,

    price: Number(product.price) || 0,

    status: getStatus(
      stock,
      minStock
    ),

    updatedAt:
      new Date()
        .toISOString()
        .split('T')[0]
  }
}

const inventory = ref<InventoryItem[]>([])

// =========================================================
// CATEGORIES
// =========================================================

const categories = computed(() => {
  return [
    'All',
    ...new Set(
      inventory.value.map(
        item => item.category
      )
    )
  ]
})

// =========================================================
// STATISTICS
// =========================================================

const totalProducts = computed(() =>
  inventory.value.length
)

const totalUnits = computed(() =>
  inventory.value.reduce(
    (total, item) =>
      total + item.stock,
    0
  )
)

const lowStockItems = computed(() =>
  inventory.value.filter(
    item => item.status === 'Low Stock'
  ).length
)

const outOfStockItems = computed(() =>
  inventory.value.filter(
    item => item.status === 'Out of Stock'
  ).length
)

// Total inventory value in LKR
const inventoryValue = computed(() =>
  inventory.value.reduce(
    (total, item) =>
      total +
      item.stock * item.price,
    0
  )
)

// =========================================================
// FILTER INVENTORY
// =========================================================

const filteredInventory = computed(() => {
  const search =
    searchQuery.value
      .trim()
      .toLowerCase()

  return inventory.value.filter(item => {
    const matchesSearch =
      !search ||
      item.name
        .toLowerCase()
        .includes(search) ||
      item.sku
        .toLowerCase()
        .includes(search) ||
      item.category
        .toLowerCase()
        .includes(search)

    const matchesCategory =
      selectedCategory.value === 'All' ||
      item.category ===
      selectedCategory.value

    const matchesStatus =
      selectedStatus.value === 'All' ||
      item.status ===
      selectedStatus.value

    return (
      matchesSearch &&
      matchesCategory &&
      matchesStatus
    )
  })
})

// =========================================================
// DATE FORMAT
// =========================================================

const formatDate = (date: string) => {
  return new Date(date).toLocaleDateString(
    'en-US',
    {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    }
  )
}

// =========================================================
// STATUS STYLE
// =========================================================

const getStatusClass = (
  status: InventoryItem['status']
) => {
  switch (status) {
    case 'In Stock':
      return 'bg-emerald-50 text-emerald-700 border-emerald-200'

    case 'Low Stock':
      return 'bg-amber-50 text-amber-700 border-amber-200'

    case 'Out of Stock':
      return 'bg-red-50 text-red-700 border-red-200'

    default:
      return 'bg-slate-50 text-slate-600 border-slate-200'
  }
}

// =========================================================
// STOCK BAR
// =========================================================

const getStockBarClass = (
  item: InventoryItem
) => {
  if (
    item.status === 'Out of Stock'
  ) {
    return 'bg-red-500'
  }

  if (
    item.status === 'Low Stock'
  ) {
    return 'bg-amber-500'
  }

  return 'bg-emerald-500'
}

const getStockPercentage = (
  item: InventoryItem
) => {
  const recommendedMax =
    Math.max(
      item.minStock * 4,
      20
    )

  return Math.min(
    (item.stock / recommendedMax) * 100,
    100
  )
}

// =========================================================
// STOCK MODAL
// =========================================================

const openStockModal = (
  item: InventoryItem,
  action: 'add' | 'remove'
) => {
  selectedItem.value = item
  stockAction.value = action
  stockQuantity.value = 1
  showStockModal.value = true
}

const closeStockModal = () => {
  showStockModal.value = false
  selectedItem.value = null
  stockQuantity.value = 1
}

// =========================================================
// UPDATE STOCK
// =========================================================

const updateStock = async () => {
  if (!selectedItem.value) {
    return
  }

  if (stockQuantity.value < 1) {
    showAlert('Validation Error', 'Please enter a valid quantity.', 'warning')
    return
  }

  const item = inventory.value.find(
    inventoryItem =>
      inventoryItem.id ===
      selectedItem.value?.id
  )

  if (!item) {
    return
  }

  const newStock =
    stockAction.value === 'add'
      ? item.stock +
      stockQuantity.value
      : Math.max(
        0,
        item.stock -
        stockQuantity.value
      )

  isSaving.value = true
  inventoryError.value = ''

  try {
    await api.put(
      `/products/${encodeURIComponent(
        String(item.id)
      )}`,
      {
        name: item.name,
        price: item.price,
        brand: item.brand || '',
        description:
          item.description || '',
        stockQty: newStock,
        images: item.image
          ? [item.image]
          : [],
        categoryId:
          item.categoryId || ''
      }
    )

    item.stock = newStock

    item.status = getStatus(
      item.stock,
      item.minStock
    )

    item.updatedAt =
      new Date()
        .toISOString()
        .split('T')[0]

    closeStockModal()

  } catch (error: any) {
    const responseData =
      error?.response?.data

    inventoryError.value =
      typeof responseData === 'string'
        ? responseData
        : responseData?.message ||
        responseData?.error ||
        'Failed to update stock.'

    console.error(
      'Failed to update stock:',
      error
    )

  } finally {
    isSaving.value = false
  }
}

// =========================================================
// LOAD INVENTORY
// =========================================================

const loadInventory = async () => {
  isLoading.value = true
  inventoryError.value = ''

  try {
    const response =
      await api.get('/products')

    inventory.value =
      getApiList<ApiProduct>(
        response.data,
        'products'
      ).map(mapProduct).reverse()

  } catch (error: any) {
    inventory.value = []

    const responseData =
      error?.response?.data

    inventoryError.value =
      typeof responseData === 'string'
        ? responseData
        : responseData?.message ||
        responseData?.error ||
        'Failed to load inventory.'

    console.error(
      'Failed to load inventory:',
      error
    )

  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadInventory()
})
</script>

<template>
  <div class="min-h-screen bg-slate-100 text-slate-900">

    <!-- Admin Sidebar -->
    <AdminSidebar />

    <!-- Main Content -->
    <main class="relative ml-64 min-h-screen overflow-hidden">

      <!-- Error -->
      <div v-if="inventoryError" class="relative z-20 mx-auto max-w-7xl px-4 pt-6 text-sm text-red-700 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between gap-4 rounded-xl border border-red-200 bg-red-50 p-4">
          <span>
            {{ inventoryError }}
          </span>

          <button type="button" class="font-bold underline" @click="loadInventory">
            Retry
          </button>
        </div>
      </div>

      <!-- Background Effects -->
      <div class="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden="true">

        <div class="absolute -right-32 -top-32 h-96 w-96 rounded-full bg-blue-200/30 blur-3xl"></div>

        <div class="absolute -left-32 top-[35%] h-96 w-96 rounded-full bg-cyan-200/25 blur-3xl"></div>

        <div class="absolute bottom-0 right-[20%] h-80 w-80 rounded-full bg-indigo-200/20 blur-3xl"></div>

      </div>

      <div class="relative z-10 mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">

        <!-- Header -->
        <section class="mb-8">

          <div
            class="rounded-3xl border border-white/90 bg-white/65 p-6 shadow-2xl shadow-slate-300/20 backdrop-blur-2xl">

            <div class="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">

              <div>

                <div class="mb-2 flex items-center gap-2">

                  <span class="flex h-8 w-8 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
                    <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21 16V8l-9-5-9 5v8l9 5 9-5Z" />

                      <path d="m3.3 7.5 8.7 5 8.7-5" />

                      <path d="M12 22V12.5" />
                    </svg>
                  </span>

                  <span class="text-xs font-bold uppercase tracking-[0.2em] text-blue-600">
                    Stock Management
                  </span>

                </div>

                <h1 class="text-3xl font-black tracking-tight text-slate-950 sm:text-4xl">
                  Inventory
                </h1>

                <p class="mt-2 max-w-2xl text-sm leading-6 text-slate-500">
                  Monitor product stock levels, identify low-stock items
                  and keep your NexusRigs inventory up to date.
                </p>

              </div>

              <button type="button" @click="loadInventory"
                class="inline-flex items-center justify-center gap-2 rounded-xl border border-slate-200/80 bg-white/80 px-5 py-3 text-sm font-bold text-slate-700 shadow-sm transition hover:border-blue-200 hover:bg-white hover:text-blue-600">

                <svg class="h-4 w-4" :class="{ 'animate-spin': isLoading }" viewBox="0 0 24 24" fill="none"
                  stroke="currentColor" stroke-width="2">
                  <path d="M20 11a8.1 8.1 0 0 0-15.5-2M4 5v4h4" />

                  <path d="M4 13a8.1 8.1 0 0 0 15.5 2M20 19v-4h-4" />
                </svg>

                Refresh Inventory

              </button>

            </div>

          </div>

        </section>

        <!-- Statistics -->
        <section class="mb-8 grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-4">

          <!-- Products -->
          <div
            class="rounded-2xl border border-white/90 bg-white/65 p-5 shadow-xl shadow-slate-300/15 backdrop-blur-2xl">

            <div class="flex items-start justify-between">

              <div>

                <p class="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Products
                </p>

                <p class="mt-2 text-3xl font-black text-slate-950">
                  {{ totalProducts }}
                </p>

                <p class="mt-1 text-xs text-slate-500">
                  Inventory products
                </p>

              </div>

              <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
                <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="m21 8-9-5-9 5 9 5 9-5Z" />

                  <path d="m3 8 9 5 9-5" />

                  <path d="M3 12l9 5 9-5" />

                  <path d="M3 16l9 5 9-5" />
                </svg>
              </div>

            </div>

          </div>

          <!-- Units -->
          <div
            class="rounded-2xl border border-white/90 bg-white/65 p-5 shadow-xl shadow-slate-300/15 backdrop-blur-2xl">

            <div class="flex items-start justify-between">

              <div>

                <p class="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Total Units
                </p>

                <p class="mt-2 text-3xl font-black text-slate-950">
                  {{ totalUnits }}
                </p>

                <p class="mt-1 text-xs text-slate-500">
                  Available stock units
                </p>

              </div>

              <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-cyan-50 text-cyan-600">
                <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M4 7h16" />
                  <path d="M4 12h16" />
                  <path d="M4 17h16" />

                  <circle cx="8" cy="7" r="1" />

                  <circle cx="8" cy="12" r="1" />

                  <circle cx="8" cy="17" r="1" />
                </svg>
              </div>

            </div>

          </div>

          <!-- Low Stock -->
          <div
            class="rounded-2xl border border-white/90 bg-white/65 p-5 shadow-xl shadow-slate-300/15 backdrop-blur-2xl">

            <div class="flex items-start justify-between">

              <div>

                <p class="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Low Stock
                </p>

                <p class="mt-2 text-3xl font-black text-slate-950">
                  {{ lowStockItems }}
                </p>

                <p class="mt-1 text-xs text-amber-600">
                  Requires attention
                </p>

              </div>

              <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-amber-50 text-amber-600">
                <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M10.3 3.9 2.6 17a2 2 0 0 0 1.7 3h15.4a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z" />

                  <path d="M12 9v4" />
                  <path d="M12 17h.01" />
                </svg>
              </div>

            </div>

          </div>

          <!-- Inventory Value -->
          <div
            class="rounded-2xl border border-white/90 bg-white/65 p-5 shadow-xl shadow-slate-300/15 backdrop-blur-2xl">

            <div class="flex items-start justify-between">

              <div>

                <p class="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Inventory Value
                </p>

                <p class="mt-2 text-2xl font-black text-slate-950">
                  {{ formatCurrency(inventoryValue) }}
                </p>

                <p v-if="outOfStockItems" class="mt-1 text-xs text-red-600">
                  {{ outOfStockItems }} out of stock
                </p>

                <p v-else class="mt-1 text-xs text-emerald-600">
                  Inventory available
                </p>

              </div>

              <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600">
                <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 1v22" />

                  <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7H14a3.5 3.5 0 0 1 0 7H6" />
                </svg>
              </div>

            </div>

          </div>

        </section>

        <!-- Inventory Table -->
        <section
          class="overflow-hidden rounded-3xl border border-white/90 bg-white/65 shadow-2xl shadow-slate-300/20 backdrop-blur-2xl">

          <!-- Toolbar -->
          <div class="border-b border-slate-200/70 p-5 sm:p-6">

            <div class="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">

              <!-- Search -->
              <div class="relative w-full xl:max-w-md">

                <svg class="pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400"
                  viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="11" cy="11" r="7" />

                  <path d="m20 20-4-4" />
                </svg>

                <input v-model="searchQuery" type="text" placeholder="Search product, SKU or category..."
                  class="w-full rounded-xl border border-slate-200/80 bg-white/75 py-3 pl-11 pr-4 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10" />

              </div>

              <!-- Filters -->
              <div class="flex flex-wrap gap-3">

                <select v-model="selectedCategory"
                  class="rounded-xl border border-slate-200/80 bg-white/75 px-4 py-3 text-sm font-semibold text-slate-700 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10">

                  <option v-for="category in categories" :key="category" :value="category">
                    {{
                      category === 'All'
                        ? 'All Categories'
                        : category
                    }}
                  </option>

                </select>

                <select v-model="selectedStatus"
                  class="rounded-xl border border-slate-200/80 bg-white/75 px-4 py-3 text-sm font-semibold text-slate-700 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10">

                  <option value="All">
                    All Stock Status
                  </option>

                  <option value="In Stock">
                    In Stock
                  </option>

                  <option value="Low Stock">
                    Low Stock
                  </option>

                  <option value="Out of Stock">
                    Out of Stock
                  </option>

                </select>

              </div>

            </div>

            <div class="mt-4 flex items-center justify-between">

              <p class="text-sm text-slate-500">
                Showing

                <span class="font-bold text-slate-900">
                  {{ filteredInventory.length }}
                </span>

                of

                <span class="font-bold text-slate-900">
                  {{ totalProducts }}
                </span>

                products
              </p>

              <p class="hidden text-sm text-slate-400 sm:block">
                Stock units:

                <span class="font-bold text-slate-700">
                  {{ totalUnits }}
                </span>
              </p>

            </div>

          </div>

          <!-- Loading -->
          <div v-if="isLoading" class="flex items-center justify-center gap-3 px-6 py-16 text-slate-500">

            <svg class="h-6 w-6 animate-spin text-blue-600" viewBox="0 0 24 24" fill="none">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />

              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
            </svg>

            <span class="font-semibold">
              Loading inventory...
            </span>

          </div>

          <!-- Desktop Table -->
          <div v-else-if="filteredInventory.length" class="hidden overflow-x-auto lg:block">

            <table class="w-full min-w-[1100px]">

              <thead>
                <tr class="border-b border-slate-200/70 bg-slate-50/50 text-left">

                  <th class="px-6 py-4 text-xs font-black uppercase tracking-wider text-slate-400">
                    Product
                  </th>

                  <th class="px-6 py-4 text-xs font-black uppercase tracking-wider text-slate-400">
                    Category
                  </th>

                  <th class="px-6 py-4 text-xs font-black uppercase tracking-wider text-slate-400">
                    Stock
                  </th>

                  <th class="px-6 py-4 text-xs font-black uppercase tracking-wider text-slate-400">
                    Price
                  </th>

                  <th class="px-6 py-4 text-xs font-black uppercase tracking-wider text-slate-400">
                    Status
                  </th>

                  <th class="px-6 py-4 text-xs font-black uppercase tracking-wider text-slate-400">
                    Updated
                  </th>

                  <th class="px-6 py-4 text-right text-xs font-black uppercase tracking-wider text-slate-400">
                    Actions
                  </th>

                </tr>
              </thead>

              <tbody class="divide-y divide-slate-200/60">

                <tr v-for="item in filteredInventory" :key="item.id" class="group transition hover:bg-blue-50/30">

                  <!-- Product -->
                  <td class="px-6 py-5">

                    <div class="flex items-center gap-3">

                      <img v-if="item.image" :src="item.image" :alt="item.name"
                        class="h-12 w-12 rounded-xl object-cover ring-2 ring-white shadow-sm" />

                      <div v-else
                        class="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
                        <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="m21 8-9-5-9 5 9 5 9-5Z" />

                          <path d="m3 8 9 5 9-5" />
                        </svg>
                      </div>

                      <div>

                        <p class="font-bold text-slate-900">
                          {{ item.name }}
                        </p>

                        <p class="mt-1 text-xs font-semibold text-slate-400">
                          {{ item.sku }}
                        </p>

                      </div>

                    </div>

                  </td>

                  <!-- Category -->
                  <td class="px-6 py-5">

                    <span class="rounded-lg bg-slate-100 px-2.5 py-1 text-xs font-bold text-slate-600">
                      {{ item.category }}
                    </span>

                  </td>

                  <!-- Stock -->
                  <td class="px-6 py-5">

                    <div class="w-36">

                      <div class="mb-2 flex items-center justify-between">

                        <span class="text-sm font-black text-slate-900">
                          {{ item.stock }}
                        </span>

                        <span class="text-[10px] font-semibold text-slate-400">
                          Min {{ item.minStock }}
                        </span>

                      </div>

                      <div class="h-2 overflow-hidden rounded-full bg-slate-200">

                        <div class="h-full rounded-full transition-all" :class="getStockBarClass(item)" :style="{
                          width: `${getStockPercentage(item)}%`
                        }"></div>

                      </div>

                    </div>

                  </td>

                  <!-- LKR Price -->
                  <td class="px-6 py-5">

                    <span class="text-sm font-black text-slate-900 whitespace-nowrap">
                      {{ formatCurrency(item.price) }}
                    </span>

                  </td>

                  <!-- Status -->
                  <td class="px-6 py-5">

                    <span class="inline-flex items-center rounded-full border px-3 py-1 text-xs font-bold"
                      :class="getStatusClass(item.status)">

                      <span class="mr-1.5 h-1.5 w-1.5 rounded-full bg-current"></span>

                      {{ item.status }}

                    </span>

                  </td>

                  <!-- Updated -->
                  <td class="px-6 py-5">

                    <span class="text-sm text-slate-500">
                      {{ formatDate(item.updatedAt) }}
                    </span>

                  </td>

                  <!-- Actions -->
                  <td class="px-6 py-5">

                    <div class="flex justify-end gap-2">

                      <button type="button" @click="
                        openStockModal(
                          item,
                          'add'
                        )
                        "
                        class="flex h-9 w-9 items-center justify-center rounded-xl border border-emerald-200 bg-emerald-50 text-emerald-600 transition hover:bg-emerald-100"
                        title="Add stock">
                        <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M12 5v14" />
                          <path d="M5 12h14" />
                        </svg>
                      </button>

                      <button type="button" @click="
                        openStockModal(
                          item,
                          'remove'
                        )
                        "
                        class="flex h-9 w-9 items-center justify-center rounded-xl border border-amber-200 bg-amber-50 text-amber-600 transition hover:bg-amber-100"
                        title="Remove stock">
                        <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M5 12h14" />
                        </svg>
                      </button>

                    </div>

                  </td>

                </tr>

              </tbody>

            </table>

          </div>

          <!-- Mobile Cards -->
          <div v-if="
            !isLoading &&
            filteredInventory.length
          " class="space-y-4 p-4 lg:hidden">

            <article v-for="item in filteredInventory" :key="item.id"
              class="rounded-2xl border border-white/90 bg-white/70 p-4 shadow-lg shadow-slate-300/10 backdrop-blur-xl">

              <div class="flex items-start gap-3">

                <img v-if="item.image" :src="item.image" :alt="item.name" class="h-14 w-14 rounded-xl object-cover" />

                <div v-else class="flex h-14 w-14 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
                  <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="m21 8-9-5-9 5 9 5 9-5Z" />
                  </svg>
                </div>

                <div class="min-w-0 flex-1">

                  <div class="flex items-start justify-between gap-2">

                    <div>

                      <h3 class="truncate font-bold text-slate-900">
                        {{ item.name }}
                      </h3>

                      <p class="mt-1 text-xs font-semibold text-slate-400">
                        {{ item.sku }}
                      </p>

                    </div>

                    <span class="shrink-0 rounded-full border px-2 py-1 text-[10px] font-bold"
                      :class="getStatusClass(item.status)">
                      {{ item.status }}
                    </span>

                  </div>

                </div>

              </div>

              <div class="mt-4 grid grid-cols-2 gap-3 border-t border-slate-200/70 pt-4">

                <div>

                  <p class="text-[10px] font-black uppercase tracking-wider text-slate-400">
                    Category
                  </p>

                  <p class="mt-1 text-xs font-bold text-slate-700">
                    {{ item.category }}
                  </p>

                </div>

                <div>

                  <p class="text-[10px] font-black uppercase tracking-wider text-slate-400">
                    Price
                  </p>

                  <p class="mt-1 text-sm font-black text-slate-900 whitespace-nowrap">
                    {{ formatCurrency(item.price) }}
                  </p>

                </div>

                <div>

                  <p class="text-[10px] font-black uppercase tracking-wider text-slate-400">
                    Stock
                  </p>

                  <p class="mt-1 text-sm font-black" :class="item.status === 'Out of Stock'
                      ? 'text-red-600'
                      : item.status === 'Low Stock'
                        ? 'text-amber-600'
                        : 'text-emerald-600'
                    ">
                    {{ item.stock }} units
                  </p>

                </div>

                <div>

                  <p class="text-[10px] font-black uppercase tracking-wider text-slate-400">
                    Minimum
                  </p>

                  <p class="mt-1 text-sm font-bold text-slate-700">
                    {{ item.minStock }}
                  </p>

                </div>

              </div>

              <div class="mt-4 flex gap-2">

                <button type="button" @click="
                  openStockModal(
                    item,
                    'add'
                  )
                  "
                  class="flex flex-1 items-center justify-center gap-2 rounded-xl bg-emerald-50 px-3 py-2.5 text-xs font-bold text-emerald-700 transition hover:bg-emerald-100">

                  <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 5v14" />
                    <path d="M5 12h14" />
                  </svg>

                  Add Stock

                </button>

                <button type="button" @click="
                  openStockModal(
                    item,
                    'remove'
                  )
                  "
                  class="flex flex-1 items-center justify-center gap-2 rounded-xl bg-amber-50 px-3 py-2.5 text-xs font-bold text-amber-700 transition hover:bg-amber-100">

                  <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M5 12h14" />
                  </svg>

                  Remove

                </button>

              </div>

            </article>

          </div>

          <!-- Empty State -->
          <div v-if="
            !isLoading &&
            !filteredInventory.length
          " class="px-6 py-16 text-center">

            <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-slate-100 text-slate-400">
              <svg class="h-7 w-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="7" />

                <path d="m20 20-4-4" />
              </svg>
            </div>

            <h3 class="mt-4 text-lg font-black text-slate-900">
              No inventory items found
            </h3>

            <p class="mx-auto mt-2 max-w-md text-sm text-slate-500">
              Try changing your search keyword,
              category or stock status filter.
            </p>

          </div>

        </section>

      </div>
    </main>

    <!-- Stock Adjustment Modal -->
    <Transition name="fade">

      <div v-if="
        showStockModal &&
        selectedItem
      " class="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 p-4 backdrop-blur-sm"
        @click.self="closeStockModal">

        <div
          class="w-full max-w-md overflow-hidden rounded-3xl border border-white/90 bg-white/90 shadow-2xl backdrop-blur-2xl">

          <!-- Modal Header -->
          <div class="flex items-center justify-between border-b border-slate-200/70 px-6 py-5">

            <div>

              <p class="text-xs font-black uppercase tracking-[0.18em] text-blue-600">
                Inventory Adjustment
              </p>

              <h2 class="mt-1 text-xl font-black text-slate-950">
                {{
                  stockAction === 'add'
                    ? 'Add Stock'
                    : 'Remove Stock'
                }}
              </h2>

            </div>

            <button type="button" @click="closeStockModal"
              class="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-100 text-slate-500 transition hover:bg-red-50 hover:text-red-600">
              <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M18 6 6 18" />
                <path d="m6 6 12 12" />
              </svg>
            </button>

          </div>

          <!-- Modal Body -->
          <div class="space-y-5 p-6">

            <!-- Product -->
            <div class="flex items-center gap-3 rounded-2xl border border-slate-200/70 bg-slate-50/70 p-4">

              <img v-if="selectedItem.image" :src="selectedItem.image" :alt="selectedItem.name"
                class="h-12 w-12 rounded-xl object-cover" />

              <div>

                <p class="font-bold text-slate-900">
                  {{ selectedItem.name }}
                </p>

                <p class="mt-1 text-xs text-slate-400">
                  {{ selectedItem.sku }}
                </p>

              </div>

            </div>

            <!-- Current Stock -->
            <div class="grid grid-cols-2 gap-3">

              <div class="rounded-xl border border-slate-200/70 bg-white p-4">

                <p class="text-xs font-bold text-slate-400">
                  Current Stock
                </p>

                <p class="mt-1 text-2xl font-black text-slate-950">
                  {{ selectedItem.stock }}
                </p>

              </div>

              <div class="rounded-xl border border-slate-200/70 bg-white p-4">

                <p class="text-xs font-bold text-slate-400">
                  Minimum Stock
                </p>

                <p class="mt-1 text-2xl font-black text-slate-950">
                  {{ selectedItem.minStock }}
                </p>

              </div>

            </div>

            <!-- Quantity -->
            <div>

              <label class="mb-2 block text-sm font-bold text-slate-700">
                Quantity
              </label>

              <input v-model.number="stockQuantity" type="number" min="1"
                class="w-full rounded-xl border border-slate-200/80 bg-white px-4 py-3 text-lg font-bold text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10" />

            </div>

            <!-- Preview -->
            <div class="rounded-2xl border p-4" :class="stockAction === 'add'
                ? 'border-emerald-200 bg-emerald-50/70'
                : 'border-amber-200 bg-amber-50/70'
              ">

              <div class="flex items-center justify-between">

                <span class="text-sm font-semibold text-slate-600">
                  New stock level
                </span>

                <span class="text-xl font-black" :class="stockAction === 'add'
                    ? 'text-emerald-700'
                    : 'text-amber-700'
                  ">
                  {{
                    stockAction === 'add'
                      ? selectedItem.stock +
                      stockQuantity
                      : Math.max(
                        0,
                        selectedItem.stock -
                        stockQuantity
                      )
                  }}
                </span>

              </div>

            </div>

          </div>

          <!-- Modal Footer -->
          <div class="flex gap-3 border-t border-slate-200/70 bg-slate-50/50 px-6 py-4">

            <button type="button" @click="closeStockModal"
              class="flex-1 rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-bold text-slate-700 transition hover:bg-slate-50">
              Cancel
            </button>

            <button type="button" @click="updateStock" :disabled="isSaving"
              class="flex-1 rounded-xl px-4 py-3 text-sm font-bold text-white shadow-lg transition hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-60"
              :class="stockAction === 'add'
                  ? 'bg-gradient-to-r from-emerald-600 to-teal-500 shadow-emerald-500/20'
                  : 'bg-gradient-to-r from-amber-500 to-orange-500 shadow-amber-500/20'
                ">
              {{
                isSaving
                  ? 'Updating...'
                  : stockAction === 'add'
                    ? 'Add Stock'
                    : 'Remove Stock'
              }}
            </button>

          </div>

        </div>

      </div>

    </Transition>

    <!-- =====================================================
         ALERT / CONFIRMATION MODAL (ORDER MANAGEMENT DESIGN)
         ===================================================== -->
    <AdminConfirmModal
      v-model:show="confirmModal.show"
      :type="confirmModal.type"
      :title="confirmModal.title"
      :message="confirmModal.message"
      :target="confirmModal.target"
      :description="confirmModal.description"
      :confirm-text="confirmModal.confirmText"
      :cancel-text="confirmModal.cancelText"
      :show-cancel="confirmModal.showCancel !== false"
      :loading="confirmModal.loading"
      @confirm="confirmModal.onConfirm"
    />

  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>