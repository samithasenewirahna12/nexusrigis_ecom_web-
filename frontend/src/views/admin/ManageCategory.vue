<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminConfirmModal, { type ModalType, type AdminModalState } from '../../components/admin/AdminConfirmModal.vue'
import api from '../../services/api'

interface Category {
  id: number | string
  name: string
  description: string
  products: number
  status: 'Active' | 'Inactive'
  image?: string
}

interface ApiCategory {
  categoryId?: number | string
  categoryName?: string
  description?: string
  products?: number | unknown[]
  categoryImage?: string
}

interface ApiProduct {
  name?: string
  categoryId?: number | string
  categoryName?: string
  category?: {
    categoryId?: number | string
    id?: number | string
    categoryName?: string
    name?: string
  } | null
}

const isLoading = ref(false)
const isSaving = ref(false)
const categoryError = ref('')
const searchQuery = ref('')
const selectedStatus = ref('All')

const showCategoryModal = ref(false)
const isEditing = ref(false)
const isDeleting = ref(false)

const selectedCategory = ref<Category | null>(null)
const categoryToDelete = ref<Category | null>(null)

const confirmModal = ref<AdminModalState>({
  show: false,
  type: 'danger',
  title: '',
  message: '',
  target: '',
  description: '',
  confirmText: 'Confirm',
  cancelText: 'Cancel',
  showCancel: true,
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

const categories = ref<Category[]>([])
/*
  {
    id: 1,
    name: 'Graphics Cards',
    description: 'High-performance GPUs for gaming, AI and professional workloads.',
    products: 24,
    status: 'Active',
    createdDate: '2026-01-10',
    image:
      'https://images.unsplash.com/photo-1591488320449-011701bb6704?w=600&auto=format&fit=crop'
  },
  {
    id: 2,
    name: 'Processors',
    description: 'Modern CPUs from leading hardware manufacturers.',
    products: 18,
    status: 'Active',
    createdDate: '2026-01-12',
    image:
      'https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?w=600&auto=format&fit=crop'
  },
  {
    id: 3,
    name: 'Motherboards',
    description: 'Reliable motherboards for gaming and workstation builds.',
    products: 16,
    status: 'Active',
    createdDate: '2026-01-15',
    image:
      'https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&auto=format&fit=crop'
  },
  {
    id: 4,
    name: 'RAM Memory',
    description: 'DDR4 and DDR5 memory modules for high-performance systems.',
    products: 21,
    status: 'Active',
    createdDate: '2026-01-18',
    image:
      'https://images.unsplash.com/photo-1562976540-1502c2145186?w=600&auto=format&fit=crop'
  },
  {
    id: 5,
    name: 'Storage',
    description: 'Fast SSD and HDD storage solutions.',
    products: 19,
    status: 'Active',
    createdDate: '2026-01-22',
    image:
      'https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?w=600&auto=format&fit=crop'
  },
  {
    id: 6,
    name: 'Power Supplies',
    description: 'Efficient and reliable PSUs for custom PC builds.',
    products: 12,
    status: 'Active',
    createdDate: '2026-02-01',
    image:
      'https://images.unsplash.com/photo-1625842268584-8f3296236761?w=600&auto=format&fit=crop'
  },
  {
    id: 7,
    name: 'PC Cases',
    description: 'Modern cases with excellent airflow and cable management.',
    products: 15,
    status: 'Active',
    createdDate: '2026-02-05',
    image:
      'https://images.unsplash.com/photo-1587202372634-32705e3bf49c?w=600&auto=format&fit=crop'
  },
  {
    id: 8,
    name: 'Cooling',
    description: 'CPU coolers, case fans and liquid cooling solutions.',
    products: 14,
    status: 'Inactive',
    createdDate: '2026-02-12',
    image:
      'https://images.unsplash.com/photo-1612815154858-60aa4c59eaa6?w=600&auto=format&fit=crop'
  }
]*/

const getApiList = <T>(data: unknown, key: string): T[] => {
  if (Array.isArray(data)) return data as T[]
  if (data && typeof data === 'object' && 'data' in data) return getApiList<T>((data as { data: unknown }).data, key)
  if (data && typeof data === 'object' && key in data) {
    const list = (data as Record<string, unknown>)[key]
    return Array.isArray(list) ? list as T[] : []
  }
  return []
}

const resolveImageUrl = (image?: string) => {
  if (!image) return ''
  if (/^(https?:|data:|blob:)/i.test(image)) return image
  const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'
  return `${apiUrl.replace(/\/api\/?$/, '')}/${image.replace(/^\//, '')}`
}

const mapCategory = (category: ApiCategory): Category => ({
  id: category.categoryId ?? '',
  name: category.categoryName || '',
  description: category.description || '',
  products: Array.isArray(category.products) ? category.products.length : Number(category.products) || 0,
  status: 'Active',
  image: resolveImageUrl(category.categoryImage),
})

const form = ref({
  name: '',
  description: '',
  status: 'Active' as Category['status'],
  image: ''
})

const filteredCategories = computed(() => {
  const search = searchQuery.value.trim().toLowerCase()

  return categories.value.filter(category => {
    const matchesSearch =
      !search ||
      category.name.toLowerCase().includes(search) ||
      category.description.toLowerCase().includes(search)

    const matchesStatus =
      selectedStatus.value === 'All' ||
      category.status === selectedStatus.value

    return matchesSearch && matchesStatus
  })
})

const totalCategories = computed(() => categories.value.length)

const activeCategories = computed(
  () => categories.value.filter(category => category.status === 'Active').length
)

const inactiveCategories = computed(
  () =>
    categories.value.filter(category => category.status === 'Inactive').length
)

const totalProducts = computed(() =>
  categories.value.reduce((total, category) => total + category.products, 0)
)

const openAddModal = () => {
  isEditing.value = false
  selectedCategory.value = null

  form.value = {
    name: '',
    description: '',
    status: 'Active',
    image: ''
  }

  showCategoryModal.value = true
}

const openEditModal = (category: Category) => {
  isEditing.value = true
  selectedCategory.value = category

  form.value = {
    name: category.name,
    description: category.description,
    status: category.status,
    image: category.image || ''
  }

  showCategoryModal.value = true
}

const closeModal = () => {
  showCategoryModal.value = false
  selectedCategory.value = null
}

const buildCategoryRequest = () => ({
  categoryName: form.value.name.trim(),
  description: form.value.description.trim(),
  categoryImage: form.value.image.trim(),
})

const saveCategory = async () => {
  if (!form.value.name.trim()) {
    showAlert('Validation Error', 'Please enter a category name.', 'warning')
    return
  }

  isSaving.value = true
  categoryError.value = ''
  try {
    const request = buildCategoryRequest()
    if (isEditing.value && selectedCategory.value) {
      await api.put(`/categories/${encodeURIComponent(String(selectedCategory.value.id))}`, request)
    } else {
      await api.post('/categories', request)
    }
    await loadCategories()
    closeModal()
  } catch (error: any) {
    const responseData = error?.response?.data
    categoryError.value = typeof responseData === 'string'
      ? responseData
      : responseData?.message || responseData?.error || 'Failed to save category.'
    console.error('Failed to save category:', error)
  } finally {
    isSaving.value = false
  }
}

const requestDeleteCategory = (category: Category) => {
  categoryToDelete.value = category
  confirmModal.value = {
    show: true,
    type: 'danger',
    title: 'Delete Category?',
    message: 'Are you sure you want to delete category',
    target: category.name,
    description: 'This action cannot be undone and will permanently remove this category.',
    confirmText: 'Delete Category',
    cancelText: 'Cancel',
    showCancel: true,
    loading: false,
    onConfirm: async () => {
      confirmModal.value.loading = true
      categoryError.value = ''
      try {
        await api.delete(`/categories/${encodeURIComponent(String(category.id))}`)
        categories.value = categories.value.filter(item => item.id !== category.id)
        confirmModal.value.show = false
        categoryToDelete.value = null
      } catch (error: any) {
        const responseData = error?.response?.data
        const errorMsg = typeof responseData === 'string'
          ? responseData
          : responseData?.message || responseData?.error || 'Failed to delete category.'
        console.error('Failed to delete category:', error)
        confirmModal.value = {
          show: true,
          type: 'danger',
          title: 'Action Failed',
          message: errorMsg,
          target: '',
          description: '',
          confirmText: 'Dismiss',
          cancelText: '',
          showCancel: false,
          loading: false,
          onConfirm: () => { confirmModal.value.show = false }
        }
      } finally {
        confirmModal.value.loading = false
      }
    }
  }
}

const toggleCategoryStatus = async (category: Category) => {
  const status = category.status === 'Active' ? 'Inactive' : 'Active'
  category.status = status
}

const getStatusClass = (status: Category['status']) => {
  return status === 'Active'
    ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
    : 'bg-amber-50 text-amber-700 border-amber-200'
}

const loadCategories = async () => {
  isLoading.value = true
  categoryError.value = ''

  try {
    const response = await api.get('/categories')
    categories.value = getApiList<ApiCategory>(response.data, 'categories').map(mapCategory).reverse()
  } catch (error: any) {
    categories.value = []
    const responseData = error?.response?.data
    categoryError.value = typeof responseData === 'string'
      ? responseData
      : responseData?.message || responseData?.error || 'Failed to load categories.'
    console.error('Failed to load categories:', error)
  } finally {
    isLoading.value = false
  }
}

const loadProductCounts = async () => {
  try {
    const response = await api.get('/products')
    const products = getApiList<ApiProduct>(response.data, 'products')
    const counts = new Map<string, number>()
    products.forEach(product => {
      const categoryId = String(product.category?.categoryId ?? product.category?.id ?? product.categoryId ?? '')
      const categoryName = product.category?.categoryName || product.category?.name || product.categoryName || ''
      const key = categoryId || categoryName.toLowerCase()
      if (key) counts.set(key, (counts.get(key) || 0) + 1)
    })
    categories.value = categories.value.map(category => ({
      ...category,
      products: counts.get(String(category.id)) ?? counts.get(category.name.toLowerCase()) ?? category.products,
    }))
  } catch (error) {
    console.error('Failed to load product counts:', error)
  }
}

onMounted(async () => {
  await loadCategories()
  await loadProductCounts()
})
</script>

<template>
  <div class="min-h-screen bg-slate-100 text-slate-900">

    <!-- Admin Sidebar -->
    <AdminSidebar />

    <!-- Main Content -->
    <main class="relative ml-64 min-h-screen overflow-hidden">

      <!-- Background Glow -->
      <div class="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden="true">
        <div class="absolute -right-32 -top-32 h-96 w-96 rounded-full bg-blue-200/30 blur-3xl"></div>

        <div class="absolute -left-32 top-[40%] h-96 w-96 rounded-full bg-cyan-200/25 blur-3xl"></div>

        <div class="absolute bottom-0 right-[20%] h-80 w-80 rounded-full bg-indigo-200/20 blur-3xl"></div>
      </div>

      <div class="relative z-10 mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div v-if="categoryError"
          class="mb-6 flex items-center justify-between gap-4 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          <span>{{ categoryError }}</span>
          <button type="button" class="font-bold underline" @click="loadCategories">Retry</button>
        </div>

        <!-- Header -->
        <section class="mb-8">
          <div
            class="rounded-3xl border border-white/90 bg-white/65 p-6 shadow-2xl shadow-slate-300/20 backdrop-blur-2xl">
            <div class="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">

              <div>
                <div class="mb-2 flex items-center gap-2">

                  <span class="flex h-8 w-8 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
                    <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M4 6h16" />
                      <path d="M4 12h16" />
                      <path d="M4 18h16" />
                    </svg>
                  </span>

                  <span class="text-xs font-bold uppercase tracking-[0.2em] text-blue-600">
                    Product Organization
                  </span>
                </div>

                <h1 class="text-3xl font-black tracking-tight text-slate-950 sm:text-4xl">
                  Categories
                </h1>

                <p class="mt-2 max-w-2xl text-sm leading-6 text-slate-500">
                  Organize your hardware catalog into clear product
                  categories and manage category visibility.
                </p>
              </div>

              <!-- Add Button -->
              <button type="button" @click="openAddModal"
                class="inline-flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-5 py-3 text-sm font-bold text-white shadow-lg shadow-blue-500/20 transition hover:-translate-y-0.5 hover:shadow-xl">
                <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 5v14" />
                  <path d="M5 12h14" />
                </svg>

                Add Category
              </button>

            </div>
          </div>
        </section>

        <!-- Statistics -->
        <section class="mb-8 grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-4">

          <!-- Total -->
          <div
            class="rounded-2xl border border-white/90 bg-white/65 p-5 shadow-xl shadow-slate-300/15 backdrop-blur-2xl">
            <div class="flex items-start justify-between">

              <div>
                <p class="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Total Categories
                </p>

                <p class="mt-2 text-3xl font-black text-slate-950">
                  {{ totalCategories }}
                </p>

                <p class="mt-1 text-xs text-slate-500">
                  Catalog categories
                </p>
              </div>

              <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
                <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M4 5h6v6H4z" />
                  <path d="M14 5h6v6h-6z" />
                  <path d="M4 15h6v4H4z" />
                  <path d="M14 15h6v4h-6z" />
                </svg>
              </div>

            </div>
          </div>

          <!-- Active -->
          <div
            class="rounded-2xl border border-white/90 bg-white/65 p-5 shadow-xl shadow-slate-300/15 backdrop-blur-2xl">
            <div class="flex items-start justify-between">

              <div>
                <p class="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Active Categories
                </p>

                <p class="mt-2 text-3xl font-black text-slate-950">
                  {{ activeCategories }}
                </p>

                <p class="mt-1 text-xs text-emerald-600">
                  Visible in catalog
                </p>
              </div>

              <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600">
                <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 6 9 17l-5-5" />
                </svg>
              </div>

            </div>
          </div>

          <!-- Inactive -->
          <div
            class="rounded-2xl border border-white/90 bg-white/65 p-5 shadow-xl shadow-slate-300/15 backdrop-blur-2xl">
            <div class="flex items-start justify-between">

              <div>
                <p class="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Inactive
                </p>

                <p class="mt-2 text-3xl font-black text-slate-950">
                  {{ inactiveCategories }}
                </p>

                <p class="mt-1 text-xs text-amber-600">
                  Hidden categories
                </p>
              </div>

              <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-amber-50 text-amber-600">
                <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="9" />
                  <path d="M12 7v5l3 2" />
                </svg>
              </div>

            </div>
          </div>

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
                  Across all categories
                </p>
              </div>

              <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-cyan-50 text-cyan-600">
                <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="m21 8-9-5-9 5 9 5 9-5Z" />
                  <path d="m3 8 9 5 9-5" />
                  <path d="M3 12l9 5 9-5" />
                  <path d="M3 16l9 5 9-5" />
                </svg>
              </div>

            </div>
          </div>

        </section>

        <!-- Category Management -->
        <section
          class="overflow-hidden rounded-3xl border border-white/90 bg-white/65 shadow-2xl shadow-slate-300/20 backdrop-blur-2xl">

          <!-- Toolbar -->
          <div class="border-b border-slate-200/70 p-5 sm:p-6">
            <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">

              <!-- Search -->
              <div class="relative w-full lg:max-w-md">

                <svg class="pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400"
                  viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="11" cy="11" r="7" />
                  <path d="m20 20-4-4" />
                </svg>

                <input v-model="searchQuery" type="text" placeholder="Search categories..."
                  class="w-full rounded-xl border border-slate-200/80 bg-white/75 py-3 pl-11 pr-4 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10" />

              </div>

              <!-- Status Filter -->
              <select v-model="selectedStatus"
                class="rounded-xl border border-slate-200/80 bg-white/75 px-4 py-3 text-sm font-semibold text-slate-700 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10">
                <option value="All">
                  All Status
                </option>

                <option value="Active">
                  Active
                </option>

                <option value="Inactive">
                  Inactive
                </option>
              </select>

            </div>

            <div class="mt-4">

              <p class="text-sm text-slate-500">
                Showing
                <span class="font-bold text-slate-900">
                  {{ filteredCategories.length }}
                </span>
                of
                <span class="font-bold text-slate-900">
                  {{ totalCategories }}
                </span>
                categories
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
              Loading categories...
            </span>
          </div>

          <!-- Category Grid -->
          <div v-else-if="filteredCategories.length" class="grid grid-cols-1 gap-5 p-5 sm:grid-cols-2 xl:grid-cols-3">

            <article v-for="category in filteredCategories" :key="category.id"
              class="group overflow-hidden rounded-2xl border border-white/90 bg-white/70 shadow-lg shadow-slate-300/10 backdrop-blur-xl transition duration-300 hover:-translate-y-1 hover:shadow-xl hover:shadow-slate-300/20">

              <!-- Image -->
              <div class="relative h-40 overflow-hidden">

                <img v-if="category.image" :src="category.image" :alt="category.name"
                  class="h-full w-full object-cover transition duration-500 group-hover:scale-105" />

                <div v-else
                  class="flex h-full w-full items-center justify-center bg-gradient-to-br from-blue-100 to-cyan-100 text-blue-600">
                  <svg class="h-12 w-12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M4 5h6v6H4z" />
                    <path d="M14 5h6v6h-6z" />
                    <path d="M4 15h6v4H4z" />
                    <path d="M14 15h6v4h-6z" />
                  </svg>
                </div>

                <!-- Overlay -->
                <div class="absolute inset-0 bg-gradient-to-t from-slate-950/40 via-transparent to-transparent"></div>

                <!-- Status -->
                <span class="absolute right-3 top-3 rounded-full border px-3 py-1 text-xs font-bold backdrop-blur-md"
                  :class="getStatusClass(category.status)">
                  {{ category.status }}
                </span>

              </div>

              <!-- Content -->
              <div class="p-5">

                <div class="flex items-start justify-between gap-3">

                  <div>
                    <h3 class="text-lg font-black text-slate-950">
                      {{ category.name }}
                    </h3>

                    <p class="mt-1 text-xs text-slate-400">
                      Category #{{ category.id }}
                    </p>
                  </div>

                  <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-blue-50 text-blue-600">
                    <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M20 7 12 3 4 7l8 4 8-4Z" />
                      <path d="m4 12 8 4 8-4" />
                      <path d="m4 17 8 4 8-4" />
                    </svg>
                  </div>

                </div>

                <p class="mt-3 min-h-[40px] text-sm leading-5 text-slate-500">
                  {{ category.description }}
                </p>

                <!-- Stats -->
                <div class="mt-5 grid grid-cols-2 gap-3">

                  <div class="rounded-xl border border-slate-200/70 bg-slate-50/70 p-3">
                    <p class="text-[10px] font-black uppercase tracking-wider text-slate-400">
                      Products
                    </p>

                    <p class="mt-1 text-lg font-black text-slate-900">
                      {{ category.products }}
                    </p>
                  </div>

                  <div class="rounded-xl border border-slate-200/70 bg-slate-50/70 p-3">
                    <p class="text-[10px] font-black uppercase tracking-wider text-slate-400">
                      Category ID
                    </p>

                    <p class="mt-1 truncate text-sm font-bold text-slate-900">
                      {{ category.id }}
                    </p>
                  </div>

                </div>

                <!-- Actions -->
                <div class="mt-5 flex gap-2 border-t border-slate-200/70 pt-4">

                  <button type="button" @click="openEditModal(category)"
                    class="flex flex-1 items-center justify-center gap-2 rounded-xl border border-slate-200/80 bg-white px-3 py-2.5 text-sm font-bold text-slate-700 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-600">
                    <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M12 20h9" />
                      <path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L8 18l-4 1 1-4Z" />
                    </svg>

                    Edit
                  </button>

                  <button type="button" @click="toggleCategoryStatus(category)"
                    class="flex h-10 w-10 items-center justify-center rounded-xl border border-slate-200/80 bg-white text-slate-500 transition hover:border-amber-200 hover:bg-amber-50 hover:text-amber-600"
                    :title="category.status === 'Active'
                      ? 'Deactivate'
                      : 'Activate'
                      ">
                    <svg v-if="category.status === 'Active'" class="h-4 w-4" viewBox="0 0 24 24" fill="none"
                      stroke="currentColor" stroke-width="2">
                      <path d="M9 18V6" />
                      <path d="m5 10 4-4 4 4" />
                      <path d="M15 6v12" />
                      <path d="m11 14 4 4 4-4" />
                    </svg>

                    <svg v-else class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M12 3v18" />
                      <path d="m7 8 5-5 5 5" />
                      <path d="m7 16 5 5 5-5" />
                    </svg>
                  </button>

                  <button type="button" @click="requestDeleteCategory(category)"
                    class="flex h-10 w-10 items-center justify-center rounded-xl border border-red-100 bg-red-50 text-red-500 transition hover:bg-red-100 hover:text-red-700"
                    title="Delete category">
                    <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M3 6h18" />
                      <path d="M8 6V4h8v2" />
                      <path d="M19 6l-1 14H6L5 6" />
                      <path d="M10 11v5" />
                      <path d="M14 11v5" />
                    </svg>
                  </button>

                </div>

              </div>
            </article>

          </div>

          <!-- Empty State -->
          <div v-if="!isLoading && !filteredCategories.length" class="px-6 py-16 text-center">
            <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-slate-100 text-slate-400">
              <svg class="h-7 w-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="7" />
                <path d="m20 20-4-4" />
              </svg>
            </div>

            <h3 class="mt-4 text-lg font-black text-slate-900">
              No categories found
            </h3>

            <p class="mx-auto mt-2 max-w-md text-sm text-slate-500">
              Try changing your search keyword or status filter.
            </p>
          </div>

        </section>

      </div>
    </main>

    <!-- =====================================================
         CONFIRMATION / ALERT MODAL (ORDER MANAGEMENT DESIGN)
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

    <!-- Add / Edit Category Modal -->
    <Transition name="fade">
      <div v-if="showCategoryModal"
        class="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 p-4 backdrop-blur-sm"
        @click.self="closeModal">

        <div
          class="w-full max-w-lg overflow-hidden rounded-3xl border border-white/90 bg-white/90 shadow-2xl backdrop-blur-2xl">

          <!-- Modal Header -->
          <div class="flex items-center justify-between border-b border-slate-200/70 px-6 py-5">

            <div>
              <p class="text-xs font-black uppercase tracking-[0.18em] text-blue-600">
                Category Management
              </p>

              <h2 class="mt-1 text-xl font-black text-slate-950">
                {{
                  isEditing
                    ? 'Edit Category'
                    : 'Add New Category'
                }}
              </h2>
            </div>

            <button type="button" @click="closeModal"
              class="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-100 text-slate-500 transition hover:bg-red-50 hover:text-red-600">
              <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M18 6 6 18" />
                <path d="m6 6 12 12" />
              </svg>
            </button>

          </div>

          <!-- Form -->
          <form @submit.prevent="saveCategory" class="space-y-5 p-6">

            <!-- Name -->
            <div>
              <label class="mb-2 block text-sm font-bold text-slate-700">
                Category Name
              </label>

              <input v-model="form.name" type="text" placeholder="e.g. Graphics Cards" required
                class="w-full rounded-xl border border-slate-200/80 bg-white/80 px-4 py-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10" />
            </div>

            <!-- Description -->
            <div>
              <label class="mb-2 block text-sm font-bold text-slate-700">
                Description
              </label>

              <textarea v-model="form.description" rows="4" placeholder="Describe this product category..."
                class="w-full resize-none rounded-xl border border-slate-200/80 bg-white/80 px-4 py-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10"></textarea>
            </div>

            <!-- Image -->
            <div>
              <label class="mb-2 block text-sm font-bold text-slate-700">
                Image URL
              </label>

              <input v-model="form.image" type="text" placeholder="https://... or /uploads/category.jpg"
                class="w-full rounded-xl border border-slate-200/80 bg-white/80 px-4 py-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10" />

              <img v-if="form.image" :src="resolveImageUrl(form.image)" alt="Category preview"
                class="mt-3 h-24 w-full rounded-xl object-cover" />
            </div>

            <!-- Status -->
            <div>
              <label class="mb-2 block text-sm font-bold text-slate-700">
                Status
              </label>

              <select v-model="form.status"
                class="w-full rounded-xl border border-slate-200/80 bg-white/80 px-4 py-3 text-sm font-semibold text-slate-700 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10">
                <option value="Active">
                  Active
                </option>

                <option value="Inactive">
                  Inactive
                </option>
              </select>
            </div>

            <!-- Actions -->
            <div class="flex gap-3 border-t border-slate-200/70 pt-5">

              <button type="button" @click="closeModal"
                class="flex-1 rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-bold text-slate-700 transition hover:bg-slate-50">
                Cancel
              </button>

              <button type="submit" :disabled="isSaving"
                class="flex-1 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-4 py-3 text-sm font-bold text-white shadow-lg shadow-blue-500/20 transition hover:-translate-y-0.5 hover:shadow-xl">
                {{
                  isEditing
                    ? 'Save Changes'
                    : 'Create Category'
                }}
              </button>

            </div>

          </form>

        </div>
      </div>
    </Transition>

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