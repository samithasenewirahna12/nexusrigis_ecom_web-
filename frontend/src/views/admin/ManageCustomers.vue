<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminConfirmModal from '../../components/admin/AdminConfirmModal.vue'
import api from '../../services/api'

interface Customer {
  id: number | string
  name: string
  email: string
  phone: string
  avatar?: string
  orders: number
  totalSpent: number
  status: 'Active' | 'Inactive' | 'Blocked'
  joinedDate: string
  lastOrder?: string
}

interface ApiCustomer {
  id?: number | string
  userId?: number | string
  customerId?: number | string
  name?: string
  fullName?: string
  firstName?: string
  lastName?: string
  email?: string
  phone?: string
  phoneNumber?: string
  avatar?: string
  image?: string
  profileImage?: string
  orders?: number
  orderCount?: number
  totalSpent?: number
  totalOrdersValue?: number
  status?: string
  joinedDate?: string
  createdAt?: string
  lastOrder?: string
  lastOrderDate?: string
}

const isLoading = ref(false)
const customerError = ref('')
const searchQuery = ref('')
const selectedStatus = ref('All')
const selectedSort = ref('Newest')

const showCustomerModal = ref(false)
const selectedCustomer = ref<Customer | null>(null)
const deletingCustomerId = ref<Customer['id'] | null>(null)

const customers = ref<Customer[]>([])

const getApiList = <T>(data: unknown, key: string): T[] => {
  if (Array.isArray(data)) return data as T[]
  if (data && typeof data === 'object' && 'data' in data) {
    return getApiList<T>((data as { data: unknown }).data, key)
  }
  if (data && typeof data === 'object' && key in data) {
    const list = (data as Record<string, unknown>)[key]
    return Array.isArray(list) ? list as T[] : []
  }
  return []
}

const normalizeStatus = (status?: string): Customer['status'] => {
  const value = status?.toLowerCase()
  if (value === 'blocked') return 'Blocked'
  if (value === 'inactive' || value === 'disabled') return 'Inactive'
  return 'Active'
}

const resolveAvatar = (image?: string | null, nameFallback: string = 'Customer') => {
  if (!image) {
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(nameFallback)}&background=2563eb&color=fff`
  }
  if (
    image.startsWith('http://') ||
    image.startsWith('https://') ||
    image.startsWith('data:')
  ) {
    return image
  }
  return `http://localhost:8080/uploads/${image.replace(/^\/+/, '')}`
}

const mapCustomer = (customer: any): Customer => {
  const name = customer.name || customer.fullName || [customer.firstName, customer.lastName].filter(Boolean).join(' ') || 'Unnamed Customer'
  const rawImg = customer.avatar || customer.image || customer.profileImage || customer.userImage
  return {
    id: customer.id ?? customer.customerId ?? customer.userId ?? '',
    name,
    email: customer.email || '',
    phone: customer.phone || customer.phoneNumber || '',
    avatar: resolveAvatar(rawImg, name),
    orders: Number(customer.orders ?? customer.orderCount) || 0,
    totalSpent: Number(customer.totalSpent ?? customer.totalOrdersValue) || 0,
    status: normalizeStatus(customer.status),
    joinedDate: customer.joinedDate || customer.createdAt || customer.registeredDate || new Date().toISOString(),
    lastOrder: customer.lastOrder || customer.lastOrderDate,
  }
}

const filteredCustomers = computed(() => {
  let result = [...customers.value]

  const search = searchQuery.value.trim().toLowerCase()

  if (search) {
    result = result.filter(customer =>
      [
        customer.name,
        customer.email,
        customer.phone,
        String(customer.id)
      ].some(value => value.toLowerCase().includes(search))
    )
  }

  if (selectedStatus.value !== 'All') {
    result = result.filter(
      customer => customer.status === selectedStatus.value
    )
  }

  if (selectedSort.value === 'Newest') {
    result.sort(
      (a, b) =>
        new Date(b.joinedDate).getTime() -
        new Date(a.joinedDate).getTime()
    )
  }

  if (selectedSort.value === 'Oldest') {
    result.sort(
      (a, b) =>
        new Date(a.joinedDate).getTime() -
        new Date(b.joinedDate).getTime()
    )
  }

  if (selectedSort.value === 'Highest Spent') {
    result.sort((a, b) => b.totalSpent - a.totalSpent)
  }

  if (selectedSort.value === 'Most Orders') {
    result.sort((a, b) => b.orders - a.orders)
  }

  return result
})

const totalCustomers = computed(() => customers.value.length)

const activeCustomers = computed(
  () => customers.value.filter(customer => customer.status === 'Active').length
)

const inactiveCustomers = computed(
  () => customers.value.filter(customer => customer.status === 'Inactive').length
)

const totalRevenue = computed(() =>
  customers.value.reduce((total, customer) => total + customer.totalSpent, 0)
)

const averageCustomerValue = computed(() => {
  if (!customers.value.length) return 0
  return totalRevenue.value / customers.value.length
})

const formatCurrency = (value: number) => {
  return `LKR ${value.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })}`
}

const formatDate = (date: string) => {
  return new Date(date).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

const getStatusClass = (status: Customer['status']) => {
  switch (status) {
    case 'Active':
      return 'bg-emerald-50 text-emerald-700 border-emerald-200'
    case 'Inactive':
      return 'bg-amber-50 text-amber-700 border-amber-200'
    case 'Blocked':
      return 'bg-red-50 text-red-700 border-red-200'
    default:
      return 'bg-slate-50 text-slate-600 border-slate-200'
  }
}

const viewCustomer = (customer: Customer) => {
  selectedCustomer.value = customer
  showCustomerModal.value = true
}

const closeCustomerModal = () => {
  showCustomerModal.value = false
  selectedCustomer.value = null
}

const updateCustomerStatus = (
  customer: Customer,
  status: Customer['status']
) => {
  customer.status = status
}

const showDeleteConfirmModal = ref(false)
const isDeletingCustomer = ref(false)

const confirmDeleteCustomer = () => {
  if (!selectedCustomer.value) return
  showDeleteConfirmModal.value = true
}

const executeDeleteCustomer = async () => {
  const customer = selectedCustomer.value
  if (!customer) return

  isDeletingCustomer.value = true
  deletingCustomerId.value = customer.id
  customerError.value = ''
  try {
    await api.delete(`/customers/${encodeURIComponent(String(customer.id))}`)
    customers.value = customers.value.filter(item => item.id !== customer.id)
    showDeleteConfirmModal.value = false
    closeCustomerModal()
  } catch (error: any) {
    const responseData = error?.response?.data
    customerError.value = typeof responseData === 'string'
      ? responseData
      : responseData?.message || responseData?.error || 'Failed to delete customer.'
    console.error('Failed to delete customer:', error)
  } finally {
    isDeletingCustomer.value = false
    deletingCustomerId.value = null
  }
}

const loadCustomers = async () => {
  isLoading.value = true
  customerError.value = ''

  try {
    const response = await api.get('/customers')
    customers.value = getApiList<ApiCustomer>(response.data, 'customers').map(mapCustomer).reverse()
  } catch (error: any) {
    customers.value = []
    console.error('Failed to load customers:', error)
    const responseData = error?.response?.data
    customerError.value = typeof responseData === 'string'
      ? responseData
      : responseData?.message || responseData?.error || 'Failed to load customers from the server.'
  } finally {
    isLoading.value = false
  }
}

const exportCustomers = () => {
  const headers = [
    'Customer ID',
    'Name',
    'Email',
    'Phone',
    'Orders',
    'Total Spent',
    'Status',
    'Joined Date',
    'Last Order'
  ]

  const rows = filteredCustomers.value.map(customer => [
    customer.id,
    customer.name,
    customer.email,
    customer.phone,
    customer.orders,
    customer.totalSpent,
    customer.status,
    customer.joinedDate,
    customer.lastOrder || ''
  ])

  const csv = [
    headers,
    ...rows
  ]
    .map(row =>
      row
        .map(value => `"${String(value).replace(/"/g, '""')}"`)
        .join(',')
    )
    .join('\n')

  const blob = new Blob([csv], {
    type: 'text/csv;charset=utf-8;'
  })

  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')

  link.href = url
  link.download = 'nexusrigs-customers.csv'
  link.click()

  URL.revokeObjectURL(url)
}

onMounted(() => {
  loadCustomers()
})
</script>

<template>
  <div class="min-h-screen bg-slate-100 text-slate-900">
    <!-- Admin Sidebar -->
    <AdminSidebar />

    <!-- Main Content -->
    <main class="ml-64 min-h-screen relative overflow-hidden">
      <!-- Background Effects -->
      <div class="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden="true">
        <div class="absolute -top-32 right-0 h-96 w-96 rounded-full bg-blue-200/30 blur-3xl"></div>

        <div class="absolute top-[35%] -left-32 h-96 w-96 rounded-full bg-cyan-200/25 blur-3xl"></div>

        <div class="absolute bottom-0 right-[25%] h-80 w-80 rounded-full bg-indigo-200/20 blur-3xl"></div>
      </div>

      <div class="relative z-10 mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <!-- Page Header -->
        <section class="mb-8">
          <div
            class="rounded-3xl border border-white/90 bg-white/65 p-6 shadow-2xl shadow-slate-300/20 backdrop-blur-2xl">
            <div class="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <div class="mb-2 flex items-center gap-2">
                  <span class="inline-flex h-8 w-8 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
                    <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
                      <circle cx="9" cy="7" r="4" />
                      <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
                      <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                    </svg>
                  </span>

                  <span class="text-xs font-bold uppercase tracking-[0.2em] text-blue-600">
                    Customer Management
                  </span>
                </div>

                <h1 class="text-3xl font-black tracking-tight text-slate-950 sm:text-4xl">
                  Customers
                </h1>

                <p class="mt-2 max-w-2xl text-sm leading-6 text-slate-500">
                  Manage customer accounts, activity, order history and
                  spending information from one professional dashboard.
                </p>
              </div>

              <div class="flex flex-wrap gap-3">
                <button type="button" @click="loadCustomers"
                  class="inline-flex items-center justify-center gap-2 rounded-xl border border-slate-200/80 bg-white/75 px-4 py-2.5 text-sm font-bold text-slate-700 shadow-sm transition hover:border-blue-200 hover:bg-white hover:text-blue-600">
                  <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20 11a8.1 8.1 0 0 0-15.5-2M4 5v4h4" />
                    <path d="M4 13a8.1 8.1 0 0 0 15.5 2M20 19v-4h-4" />
                  </svg>
                  Refresh
                </button>

                <button type="button" @click="exportCustomers"
                  class="inline-flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-4 py-2.5 text-sm font-bold text-white shadow-lg shadow-blue-500/20 transition hover:-translate-y-0.5 hover:shadow-xl">
                  <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 3v12" />
                    <path d="m7 10 5 5 5-5" />
                    <path d="M5 21h14" />
                  </svg>
                  Export CSV
                </button>
              </div>
            </div>
          </div>
        </section>

        <!-- Statistics -->
        <section class="mb-8 grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-4">
          <!-- Total Customers -->
          <div
            class="rounded-2xl border border-white/90 bg-white/65 p-5 shadow-xl shadow-slate-300/15 backdrop-blur-2xl">
            <div class="flex items-start justify-between">
              <div>
                <p class="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Total Customers
                </p>

                <p class="mt-2 text-3xl font-black text-slate-950">
                  {{ totalCustomers }}
                </p>

                <p class="mt-1 text-xs text-slate-500">
                  Registered accounts
                </p>
              </div>

              <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
                <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
                  <circle cx="9" cy="7" r="4" />
                  <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
                  <path d="M16 3.13a4 4 0 0 1 0 7.75" />
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
                  Active Customers
                </p>

                <p class="mt-2 text-3xl font-black text-slate-950">
                  {{ activeCustomers }}
                </p>

                <p class="mt-1 text-xs text-emerald-600">
                  Currently active
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
                  {{ inactiveCustomers }}
                </p>

                <p class="mt-1 text-xs text-amber-600">
                  Need engagement
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

          <!-- Revenue -->
          <div
            class="rounded-2xl border border-white/90 bg-white/65 p-5 shadow-xl shadow-slate-300/15 backdrop-blur-2xl">
            <div class="flex items-start justify-between">
              <div>
                <p class="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Customer Value
                </p>

                <p class="mt-2 text-2xl font-black text-slate-950">
                  {{ formatCurrency(averageCustomerValue) }}
                </p>

                <p class="mt-1 text-xs text-slate-500">
                  Average customer spend
                </p>
              </div>

              <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-cyan-50 text-cyan-600">
                <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 1v22" />
                  <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7H14a3.5 3.5 0 0 1 0 7H6" />
                </svg>
              </div>
            </div>
          </div>
        </section>

        <div v-if="customerError"
          class="mb-6 flex items-center justify-between gap-4 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          <span>{{ customerError }}</span>
          <button type="button" class="font-bold underline" @click="loadCustomers">Retry</button>
        </div>

        <!-- Customer Management Card -->
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

                <input v-model="searchQuery" type="text" placeholder="Search customers..."
                  class="w-full rounded-xl border border-slate-200/80 bg-white/75 py-3 pl-11 pr-4 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10" />
              </div>

              <!-- Filters -->
              <div class="flex flex-wrap gap-3">
                <select v-model="selectedStatus"
                  class="rounded-xl border border-slate-200/80 bg-white/75 px-4 py-3 text-sm font-semibold text-slate-700 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10">
                  <option value="All">All Status</option>
                  <option value="Active">Active</option>
                  <option value="Inactive">Inactive</option>
                  <option value="Blocked">Blocked</option>
                </select>

                <select v-model="selectedSort"
                  class="rounded-xl border border-slate-200/80 bg-white/75 px-4 py-3 text-sm font-semibold text-slate-700 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10">
                  <option value="Newest">Newest</option>
                  <option value="Oldest">Oldest</option>
                  <option value="Highest Spent">Highest Spent</option>
                  <option value="Most Orders">Most Orders</option>
                </select>
              </div>
            </div>

            <div class="mt-4 flex items-center justify-between">
              <p class="text-sm text-slate-500">
                Showing
                <span class="font-bold text-slate-900">
                  {{ filteredCustomers.length }}
                </span>
                of
                <span class="font-bold text-slate-900">
                  {{ totalCustomers }}
                </span>
                customers
              </p>

              <p class="hidden text-sm text-slate-400 sm:block">
                Total customer revenue:
                <span class="font-bold text-slate-700">
                  {{ formatCurrency(totalRevenue) }}
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
              Loading customers...
            </span>
          </div>

          <!-- Desktop Table -->
          <div v-else-if="filteredCustomers.length" class="hidden overflow-x-auto lg:block">
            <table class="w-full min-w-[1100px]">
              <thead>
                <tr class="border-b border-slate-200/70 bg-slate-50/50 text-left">
                  <th class="px-6 py-4 text-xs font-black uppercase tracking-wider text-slate-400">
                    Customer
                  </th>

                  <th class="px-6 py-4 text-xs font-black uppercase tracking-wider text-slate-400">
                    Contact
                  </th>

                  <th class="px-6 py-4 text-xs font-black uppercase tracking-wider text-slate-400">
                    Orders
                  </th>

                  <th class="px-6 py-4 text-xs font-black uppercase tracking-wider text-slate-400">
                    Total Spent
                  </th>

                  <th class="px-6 py-4 text-xs font-black uppercase tracking-wider text-slate-400">
                    Status
                  </th>

                  <th class="px-6 py-4 text-xs font-black uppercase tracking-wider text-slate-400">
                    Joined
                  </th>

                  <th class="px-6 py-4 text-right text-xs font-black uppercase tracking-wider text-slate-400">
                    Action
                  </th>
                </tr>
              </thead>

              <tbody class="divide-y divide-slate-200/60">
                <tr v-for="customer in filteredCustomers" :key="customer.id"
                  class="group transition hover:bg-blue-50/30">
                  <!-- Customer -->
                  <td class="px-6 py-5">
                    <div class="flex items-center gap-3">
                      <div class="relative">
                        <img v-if="customer.avatar" :src="customer.avatar" :alt="customer.name"
                          class="h-11 w-11 rounded-xl object-cover ring-2 ring-white shadow-sm" />

                        <div v-else
                          class="flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-blue-500 to-cyan-400 text-sm font-black text-white">
                          {{ customer.name.charAt(0) }}
                        </div>

                        <span v-if="customer.status === 'Active'"
                          class="absolute -bottom-0.5 -right-0.5 h-3.5 w-3.5 rounded-full border-2 border-white bg-emerald-500"></span>
                      </div>

                      <div>
                        <p class="font-bold text-slate-900">
                          {{ customer.name }}
                        </p>

                        <p class="mt-0.5 text-xs text-slate-400">
                          #{{ customer.id }}
                        </p>
                      </div>
                    </div>
                  </td>

                  <!-- Contact -->
                  <td class="px-6 py-5">
                    <p class="text-sm font-medium text-slate-700">
                      {{ customer.email }}
                    </p>

                    <p class="mt-1 text-xs text-slate-400">
                      {{ customer.phone }}
                    </p>
                  </td>

                  <!-- Orders -->
                  <td class="px-6 py-5">
                    <span class="inline-flex rounded-lg bg-blue-50 px-2.5 py-1 text-sm font-bold text-blue-700">
                      {{ customer.orders }}
                    </span>
                  </td>

                  <!-- Spent -->
                  <td class="px-6 py-5">
                    <p class="text-sm font-black text-slate-900">
                      {{ formatCurrency(customer.totalSpent) }}
                    </p>

                    <p v-if="customer.lastOrder" class="mt-1 text-xs text-slate-400">
                      Last:
                      {{ formatDate(customer.lastOrder) }}
                    </p>
                  </td>

                  <!-- Status -->
                  <td class="px-6 py-5">
                    <span class="inline-flex items-center rounded-full border px-3 py-1 text-xs font-bold"
                      :class="getStatusClass(customer.status)">
                      <span class="mr-1.5 h-1.5 w-1.5 rounded-full bg-current"></span>

                      {{ customer.status }}
                    </span>
                  </td>

                  <!-- Joined -->
                  <td class="px-6 py-5">
                    <span class="text-sm text-slate-600">
                      {{ formatDate(customer.joinedDate) }}
                    </span>
                  </td>

                  <!-- Action -->
                  <td class="px-6 py-5 text-right">
                    <button type="button" @click="viewCustomer(customer)"
                      class="inline-flex items-center gap-2 rounded-xl border border-slate-200/80 bg-white/80 px-3.5 py-2 text-sm font-bold text-slate-700 shadow-sm transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-600">
                      <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7Z" />
                        <circle cx="12" cy="12" r="3" />
                      </svg>

                      View
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Mobile Cards -->
          <div v-if="!isLoading && filteredCustomers.length" class="space-y-4 p-4 lg:hidden">
            <div v-for="customer in filteredCustomers" :key="customer.id"
              class="rounded-2xl border border-white/90 bg-white/70 p-4 shadow-lg shadow-slate-300/10 backdrop-blur-xl">
              <div class="flex items-start justify-between gap-3">
                <div class="flex items-center gap-3">
                  <img v-if="customer.avatar" :src="customer.avatar" :alt="customer.name"
                    class="h-12 w-12 rounded-xl object-cover ring-2 ring-white" />

                  <div v-else
                    class="flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br from-blue-500 to-cyan-400 font-black text-white">
                    {{ customer.name.charAt(0) }}
                  </div>

                  <div>
                    <h3 class="font-bold text-slate-900">
                      {{ customer.name }}
                    </h3>

                    <p class="text-xs text-slate-400">
                      #{{ customer.id }}
                    </p>
                  </div>
                </div>

                <span class="rounded-full border px-2.5 py-1 text-xs font-bold"
                  :class="getStatusClass(customer.status)">
                  {{ customer.status }}
                </span>
              </div>

              <div class="mt-4 grid grid-cols-2 gap-3 border-t border-slate-200/70 pt-4">
                <div>
                  <p class="text-[10px] font-black uppercase tracking-wider text-slate-400">
                    Email
                  </p>

                  <p class="mt-1 truncate text-xs font-semibold text-slate-700">
                    {{ customer.email }}
                  </p>
                </div>

                <div>
                  <p class="text-[10px] font-black uppercase tracking-wider text-slate-400">
                    Phone
                  </p>

                  <p class="mt-1 text-xs font-semibold text-slate-700">
                    {{ customer.phone }}
                  </p>
                </div>

                <div>
                  <p class="text-[10px] font-black uppercase tracking-wider text-slate-400">
                    Orders
                  </p>

                  <p class="mt-1 text-sm font-black text-blue-600">
                    {{ customer.orders }}
                  </p>
                </div>

                <div>
                  <p class="text-[10px] font-black uppercase tracking-wider text-slate-400">
                    Total Spent
                  </p>

                  <p class="mt-1 text-sm font-black text-slate-900">
                    {{ formatCurrency(customer.totalSpent) }}
                  </p>
                </div>
              </div>

              <button type="button" @click="viewCustomer(customer)"
                class="mt-4 flex w-full items-center justify-center gap-2 rounded-xl bg-slate-950 px-4 py-2.5 text-sm font-bold text-white transition hover:bg-blue-600">
                <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7Z" />
                  <circle cx="12" cy="12" r="3" />
                </svg>

                View Customer
              </button>
            </div>
          </div>

          <!-- Empty State -->
          <div v-if="!isLoading && !filteredCustomers.length" class="px-6 py-16 text-center">
            <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-slate-100 text-slate-400">
              <svg class="h-7 w-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="7" />
                <path d="m20 20-4-4" />
              </svg>
            </div>

            <h3 class="mt-4 text-lg font-black text-slate-900">
              No customers found
            </h3>

            <p class="mx-auto mt-2 max-w-md text-sm text-slate-500">
              Try changing your search keyword or status filter.
            </p>
          </div>
        </section>
      </div>
    </main>

    <!-- Customer Details Modal -->
    <Transition name="fade">
      <div v-if="showCustomerModal && selectedCustomer"
        class="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 p-4 backdrop-blur-sm"
        @click.self="closeCustomerModal">
        <div
          class="w-full max-w-lg overflow-hidden rounded-3xl border border-white/90 bg-white/90 shadow-2xl backdrop-blur-2xl">
          <!-- Modal Header -->
          <div class="flex items-center justify-between border-b border-slate-200/70 px-6 py-5">
            <div>
              <p class="text-xs font-black uppercase tracking-[0.18em] text-blue-600">
                Customer Details
              </p>

              <h2 class="mt-1 text-xl font-black text-slate-950">
                {{ selectedCustomer.name }}
              </h2>
            </div>

            <button type="button" @click="closeCustomerModal"
              class="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-100 text-slate-500 transition hover:bg-red-50 hover:text-red-600">
              <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M18 6 6 18" />
                <path d="m6 6 12 12" />
              </svg>
            </button>
          </div>

          <!-- Modal Body -->
          <div class="space-y-5 p-6">
            <div class="flex items-center gap-4">
              <img v-if="selectedCustomer.avatar" :src="selectedCustomer.avatar" :alt="selectedCustomer.name"
                class="h-16 w-16 rounded-2xl object-cover ring-4 ring-white shadow-lg" />

              <div v-else
                class="flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-blue-500 to-cyan-400 text-xl font-black text-white">
                {{ selectedCustomer.name.charAt(0) }}
              </div>

              <div>
                <h3 class="text-lg font-black text-slate-950">
                  {{ selectedCustomer.name }}
                </h3>

                <p class="text-sm text-slate-500">
                  Customer #{{ selectedCustomer.id }}
                </p>

                <span class="mt-2 inline-flex rounded-full border px-2.5 py-1 text-xs font-bold"
                  :class="getStatusClass(selectedCustomer.status)">
                  {{ selectedCustomer.status }}
                </span>
              </div>
            </div>

            <div class="grid grid-cols-2 gap-3">
              <div class="rounded-2xl border border-slate-200/70 bg-slate-50/70 p-4">
                <p class="text-xs font-bold text-slate-400">
                  Total Orders
                </p>

                <p class="mt-1 text-xl font-black text-slate-950">
                  {{ selectedCustomer.orders }}
                </p>
              </div>

              <div class="rounded-2xl border border-slate-200/70 bg-slate-50/70 p-4">
                <p class="text-xs font-bold text-slate-400">
                  Total Spent
                </p>

                <p class="mt-1 text-xl font-black text-slate-950">
                  {{ formatCurrency(selectedCustomer.totalSpent) }}
                </p>
              </div>
            </div>

            <div class="space-y-3">
              <div class="flex items-center justify-between rounded-xl bg-slate-50/70 px-4 py-3">
                <span class="text-sm font-semibold text-slate-500">
                  Email
                </span>

                <span class="max-w-[60%] truncate text-right text-sm font-bold text-slate-800">
                  {{ selectedCustomer.email }}
                </span>
              </div>

              <div class="flex items-center justify-between rounded-xl bg-slate-50/70 px-4 py-3">
                <span class="text-sm font-semibold text-slate-500">
                  Phone
                </span>

                <span class="text-sm font-bold text-slate-800">
                  {{ selectedCustomer.phone }}
                </span>
              </div>

              <div class="flex items-center justify-between rounded-xl bg-slate-50/70 px-4 py-3">
                <span class="text-sm font-semibold text-slate-500">
                  Joined
                </span>

                <span class="text-sm font-bold text-slate-800">
                  {{ formatDate(selectedCustomer.joinedDate) }}
                </span>
              </div>

              <div class="flex items-center justify-between rounded-xl bg-slate-50/70 px-4 py-3">
                <span class="text-sm font-semibold text-slate-500">
                  Last Order
                </span>

                <span class="text-sm font-bold text-slate-800">
                  {{
                    selectedCustomer.lastOrder
                      ? formatDate(selectedCustomer.lastOrder)
                      : 'No orders'
                  }}
                </span>
              </div>
            </div>

            <!-- Status Controls -->
            <div>
              <p class="mb-2 text-xs font-black uppercase tracking-wider text-slate-400">
                Account Status
              </p>

              <div class="grid grid-cols-3 gap-2">
                <button type="button" @click="updateCustomerStatus(selectedCustomer, 'Active')"
                  class="rounded-xl border px-3 py-2 text-xs font-bold transition" :class="selectedCustomer.status === 'Active'
                    ? 'border-emerald-300 bg-emerald-50 text-emerald-700'
                    : 'border-slate-200 bg-white text-slate-500 hover:border-emerald-200 hover:text-emerald-600'
                    ">
                  Active
                </button>

                <button type="button" @click="updateCustomerStatus(selectedCustomer, 'Inactive')"
                  class="rounded-xl border px-3 py-2 text-xs font-bold transition" :class="selectedCustomer.status === 'Inactive'
                    ? 'border-amber-300 bg-amber-50 text-amber-700'
                    : 'border-slate-200 bg-white text-slate-500 hover:border-amber-200 hover:text-amber-600'
                    ">
                  Inactive
                </button>

                <button type="button" @click="updateCustomerStatus(selectedCustomer, 'Blocked')"
                  class="rounded-xl border px-3 py-2 text-xs font-bold transition" :class="selectedCustomer.status === 'Blocked'
                    ? 'border-red-300 bg-red-50 text-red-700'
                    : 'border-slate-200 bg-white text-slate-500 hover:border-red-200 hover:text-red-600'
                    ">
                  Blocked
                </button>
              </div>
            </div>
          </div>

          <!-- Modal Footer -->
          <div class="flex justify-between border-t border-slate-200/70 bg-slate-50/50 px-6 py-4">
            <button type="button" @click="confirmDeleteCustomer" :disabled="isDeletingCustomer"
              class="rounded-xl border border-red-200 px-5 py-2.5 text-sm font-bold text-red-600 transition hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50">
              {{ isDeletingCustomer ? 'Deleting...' : 'Delete Customer' }}
            </button>

            <button type="button" @click="closeCustomerModal"
              class="rounded-xl bg-slate-950 px-5 py-2.5 text-sm font-bold text-white transition hover:bg-blue-600">
              Close
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- =====================================================
         DELETE CONFIRMATION MODAL (ORDER MANAGEMENT DESIGN)
         ===================================================== -->
    <AdminConfirmModal
      v-model:show="showDeleteConfirmModal"
      type="danger"
      title="Delete Customer?"
      message="Are you sure you want to permanently delete customer"
      :target="selectedCustomer?.name"
      description="This customer account and records will be deleted. This action cannot be undone."
      confirm-text="Delete Customer"
      cancel-text="Cancel"
      :loading="isDeletingCustomer"
      @confirm="executeDeleteCustomer"
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