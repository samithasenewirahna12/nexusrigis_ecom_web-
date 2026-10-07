<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminConfirmModal from '../../components/admin/AdminConfirmModal.vue'
import api from '../../services/api'

interface Deal {
  id: string
  code: string
  productId: string
  productName: string
  discount: number
  badgeText: string
  startDate: string
  endDate: string
  enabled: boolean
}

const deals = ref<Deal[]>([])
const isLoading = ref(false)
const loadError = ref('')
const successMessage = ref('')

const searchQuery = ref('')
const selectedStatus = ref('All')
const currentPage = ref(1)
const itemsPerPage = 6

const showFormModal = ref(false)
const showDeleteModal = ref(false)
const isSaving = ref(false)
const editingDealId = ref('')
const selectedDeal = ref<Deal | null>(null)

const form = ref({ applyType: 'single', productId: '', productIds: [] as string[], categoryId: '', discount: 10, badgeText: 'FLASH DEAL', startDate: '', endDate: '', enabled: true })

const todayISO = new Date().toISOString().split('T')[0]

const normalizeDeal = (item: any): Deal => ({
  id: String(item.dealId ?? item.id ?? ''),
  code: String(item.code ?? item.dealCode ?? item.couponCode ?? item.promoCode ?? item.discountCode ?? ''),
  productId: String(item.productId ?? ''),
  productName: String(item.productName ?? ''),
  discount: Number(item.discountPercentage ?? item.discount ?? 0),
  badgeText: String(item.badgeText ?? ''),
  startDate: String(item.startDate ?? ''),
  endDate: String(item.endDate ?? ''),
  enabled: item.enabled !== false && String(item.status ?? '').toLowerCase() !== 'inactive'
})

const products = ref<any[]>([])
const categories = ref<any[]>([])
const fetchProducts = async () => {
  try {
    const response = await api.get('/products')
    const list = Array.isArray(response.data) ? response.data : (response.data?.content || [])
    products.value = list

    const catRes = await api.get('/categories')
    categories.value = catRes.data || []
  } catch (err) {
    console.error(err)
  }
}

const fetchDeals = async () => {
  isLoading.value = true
  loadError.value = ''
  try {
    const response = await api.get('/deals')
    const data = response.data
    const list = Array.isArray(data) ? data : Array.isArray(data?.content) ? data.content : []
    deals.value = list.map(normalizeDeal).reverse()
  } catch (err: any) {
    loadError.value = err?.response?.data?.message || 'Failed to load discounts.'
  } finally {
    isLoading.value = false
  }
}

onMounted(() => { fetchDeals(); fetchProducts(); })

const getStatus = (deal: Deal) => {
  if (!deal.enabled) return 'Inactive'
  const now = new Date()
  const start = deal.startDate ? new Date(deal.startDate) : null
  const end = deal.endDate ? new Date(deal.endDate) : null
  if (start && !Number.isNaN(start.getTime()) && now < start) return 'Scheduled'
  if (end && !Number.isNaN(end.getTime()) && now > end) return 'Expired'
  return 'Active'
}

const totalDiscounts = computed(() => deals.value.length)
const activeDiscounts = computed(() => deals.value.filter(deal => getStatus(deal) === 'Active').length)
const scheduledDiscounts = computed(() => deals.value.filter(deal => getStatus(deal) === 'Scheduled').length)
const expiredDiscounts = computed(() => deals.value.filter(deal => getStatus(deal) === 'Expired').length)
const averageDiscount = computed(() => deals.value.length ? deals.value.reduce((sum, deal) => sum + deal.discount, 0) / deals.value.length : 0)

const filteredDeals = computed(() => {
  const search = searchQuery.value.toLowerCase().trim()
  return deals.value.filter(deal => {
    const matchesSearch = !search || deal.productName.toLowerCase().includes(search)
      || deal.productId.toLowerCase().includes(search)
      || deal.id.toLowerCase().includes(search)
      || deal.code.toLowerCase().includes(search)
    const matchesStatus = selectedStatus.value === 'All' || getStatus(deal) === selectedStatus.value
    return matchesSearch && matchesStatus
  })
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredDeals.value.length / itemsPerPage)))
const paginatedDeals = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  return filteredDeals.value.slice(start, start + itemsPerPage)
})

const resetPage = () => { currentPage.value = 1 }
const clearFilters = () => { searchQuery.value = ''; selectedStatus.value = 'All'; currentPage.value = 1 }
const goToPage = (page: number) => { if (page >= 1 && page <= totalPages.value) currentPage.value = page }

const emptyForm = () => ({ applyType: 'single', productId: '', productIds: [], categoryId: '', discount: 10, badgeText: 'FLASH DEAL', startDate: todayISO, endDate: '', enabled: true })
const openCreateModal = () => {
  editingDealId.value = ''
  form.value = emptyForm()
  showFormModal.value = true
}
const openEditModal = (deal: Deal) => {
  editingDealId.value = deal.id
  form.value = { applyType: 'single', productId: deal.productId, productIds: [], categoryId: '', discount: deal.discount, badgeText: deal.badgeText, startDate: deal.startDate?.slice(0, 10) || '', endDate: deal.endDate?.slice(0, 10) || '', enabled: deal.enabled }
  showFormModal.value = true
}
const closeFormModal = () => { if (!isSaving.value) showFormModal.value = false }

const saveDeal = async () => {
  if (form.value.applyType === 'single' && !form.value.productId) { loadError.value = 'Product is required.'; return }
  if (form.value.applyType === 'multiple' && (!form.value.productIds || form.value.productIds.length === 0)) { loadError.value = 'Please select at least one product.'; return }
  if (form.value.applyType === 'category' && !form.value.categoryId) { loadError.value = 'Category is required.'; return }
  if (form.value.discount < 1 || form.value.discount > 100) { loadError.value = 'Discount must be between 1% and 100%.'; return }
  if (!form.value.startDate) { loadError.value = 'Start date is required.'; return }
  if (!form.value.endDate) { loadError.value = 'End date is required.'; return }
  if (form.value.endDate < form.value.startDate) { loadError.value = 'End date cannot be before start date.'; return }

  isSaving.value = true
  loadError.value = ''
  
  const payload = { 
    productId: form.value.applyType === 'single' ? form.value.productId : undefined,
    productIds: form.value.applyType === 'multiple' ? form.value.productIds : undefined,
    categoryId: form.value.applyType === 'category' ? form.value.categoryId : undefined,
    discountPercentage: form.value.discount, 
    badgeText: form.value.badgeText, 
    startDate: form.value.startDate, 
    endDate: form.value.endDate 
  }
  
  try {
    if (editingDealId.value) {
      const { data } = await api.put(`/deals/${editingDealId.value}`, payload)
      const index = deals.value.findIndex(deal => deal.id === editingDealId.value)
      if (index >= 0) deals.value[index] = normalizeDeal(data ?? { ...payload, dealId: editingDealId.value })
      successMessage.value = 'Discount updated successfully.'
    } else {
      const { data } = await api.post('/deals/bulk', payload)
      if (Array.isArray(data)) {
        deals.value = [...data.map(normalizeDeal), ...deals.value]
      } else {
        deals.value.unshift(normalizeDeal(data ?? { ...payload, dealId: `LOCAL-${Date.now()}` }))
      }
      successMessage.value = 'Discount(s) created successfully.'
    }
    showFormModal.value = false
    setTimeout(() => { successMessage.value = '' }, 4000)
  } catch (err: any) {
    loadError.value = err?.response?.data?.message || 'Failed to save discount.'
  } finally {
    isSaving.value = false
  }
}

const openDeleteModal = (deal: Deal) => { selectedDeal.value = deal; showDeleteModal.value = true }
const deleteDeal = async () => {
  if (!selectedDeal.value?.id) return
  const id = selectedDeal.value.id
  try {
    await api.delete(`/deals/${id}`)
    deals.value = deals.value.filter(deal => deal.id !== id)
    successMessage.value = 'Discount deleted successfully.'
    setTimeout(() => { successMessage.value = '' }, 4000)
  } catch (err: any) {
    loadError.value = err?.response?.data?.message || 'Failed to delete discount.'
  } finally {
    showDeleteModal.value = false
    selectedDeal.value = null
  }
}

const toggleDeal = async (deal: Deal) => {
  deal.enabled = !deal.enabled
  // Note: Backend does not store 'enabled' flag, status is based entirely on startDate/endDate.
}

const formatDate = (date: string) => {
  if (!date) return '—'
  const d = new Date(date)
  if (Number.isNaN(d.getTime())) return date
  return d.toLocaleDateString('en-LK', { year: 'numeric', month: 'short', day: 'numeric' })
}

const getStatusClass = (status: string) => {
  switch (status) {
    case 'Active': return 'bg-emerald-50 border-emerald-200 text-emerald-700'
    case 'Scheduled': return 'bg-blue-50 border-blue-200 text-blue-700'
    case 'Expired': return 'bg-red-50 border-red-200 text-red-700'
    default: return 'bg-slate-100 border-slate-200 text-slate-600'
  }
}
</script>

<template>
  <div class="min-h-screen bg-slate-100">
    <AdminSidebar />

    <main class="ml-64 min-h-screen">
      <div class="relative min-h-screen overflow-hidden bg-slate-100 text-slate-900">
        <div class="fixed inset-0 pointer-events-none bg-gradient-to-br from-white via-slate-50 to-blue-50/70"></div>
        <div
          class="fixed -top-40 -right-40 w-[500px] h-[500px] rounded-full bg-cyan-400/10 blur-3xl pointer-events-none">
        </div>
        <div
          class="fixed -bottom-40 -left-40 w-[500px] h-[500px] rounded-full bg-blue-500/10 blur-3xl pointer-events-none">
        </div>

        <div class="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <header class="mb-8 flex flex-col lg:flex-row lg:items-center lg:justify-between gap-5">
            <div>
              <div class="flex items-center gap-3 flex-wrap"><span
                  class="px-2.5 py-1 rounded-md bg-blue-50 border border-blue-200 text-blue-600 text-[10px] font-bold uppercase tracking-widest">Admin
                  Panel</span><span class="text-xs text-slate-400 font-semibold">/ Discount Management</span></div>
              <h1 class="mt-3 text-3xl sm:text-4xl font-black text-slate-950 tracking-tight">Discount Management</h1>
              <p class="mt-2 text-sm text-slate-500">Create, monitor, activate, and manage promotional deal discounts.
              </p>
            </div>
            <button @click="openCreateModal"
              class="inline-flex items-center justify-center gap-2 rounded-xl bg-blue-600 px-4 py-3 text-xs font-black text-white shadow-md shadow-blue-500/20 transition hover:bg-blue-700">+
              Create Discount</button>
          </header>

          <div v-if="successMessage"
            class="mb-5 p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-bold flex items-center justify-between">
            <span>✓ {{ successMessage }}</span><button @click="successMessage = ''">×</button>
          </div>
          <div v-if="loadError"
            class="mb-5 p-3 rounded-xl bg-red-50 border border-red-200 text-red-700 text-xs font-semibold">{{ loadError
            }}</div>

          <section class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-5 gap-5 mb-8">
            <div class="kpi-card">
              <div class="flex justify-between items-start">
                <div>
                  <p class="kpi-label">Total Discounts</p>
                  <p class="kpi-value">{{ totalDiscounts }}</p>
                </div><span class="kpi-icon bg-blue-50 text-blue-600">%</span>
              </div>
              <p class="kpi-note">All deal campaigns</p>
            </div>
            <div class="kpi-card">
              <div class="flex justify-between items-start">
                <div>
                  <p class="kpi-label">Active</p>
                  <p class="kpi-value">{{ activeDiscounts }}</p>
                </div><span class="kpi-icon bg-emerald-50 text-emerald-600">✓</span>
              </div>
              <p class="kpi-note text-emerald-600">Currently available</p>
            </div>
            <div class="kpi-card">
              <div class="flex justify-between items-start">
                <div>
                  <p class="kpi-label">Scheduled</p>
                  <p class="kpi-value">{{ scheduledDiscounts }}</p>
                </div><span class="kpi-icon bg-blue-50 text-blue-600">◷</span>
              </div>
              <p class="kpi-note text-blue-600">Starting in the future</p>
            </div>
            <div class="kpi-card">
              <div class="flex justify-between items-start">
                <div>
                  <p class="kpi-label">Expired</p>
                  <p class="kpi-value">{{ expiredDiscounts }}</p>
                </div><span class="kpi-icon bg-red-50 text-red-600">×</span>
              </div>
              <p class="kpi-note text-red-600">Past campaigns</p>
            </div>
            <div class="kpi-card kpi-revenue">
              <div class="flex justify-between items-start">
                <div>
                  <p class="text-[10px] font-bold uppercase tracking-[0.14em] text-white/70">Average Discount</p>
                  <p class="mt-1.5 text-2xl font-black text-white">{{ averageDiscount.toFixed(0) }}%</p>
                </div><span class="kpi-icon bg-white/15 text-white">%</span>
              </div>
              <p class="mt-2 text-[10px] font-semibold text-white/70">Across all deals</p>
            </div>
          </section>

          <section
            class="bg-white/65 backdrop-blur-2xl border border-white/90 rounded-3xl shadow-xl shadow-slate-300/20 overflow-hidden">
            <div class="p-5 sm:p-6 border-b border-slate-200/80">
              <div
                class="mb-5 p-4 rounded-2xl bg-gradient-to-r from-blue-50/90 via-indigo-50/90 to-cyan-50/90 border border-blue-100 flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-xl bg-blue-600 text-white flex items-center justify-center font-black">%
                  </div>
                  <div>
                    <h3 class="text-xs font-black text-slate-900 tracking-wide uppercase">Discount Workflow</h3>
                    <p class="text-[11px] text-slate-500 mt-0.5">Create campaign → schedule dates → activate → monitor
                      expiry.</p>
                  </div>
                </div><span class="workflow-step active">Deal Campaigns</span>
              </div>
              <div class="flex flex-col xl:flex-row xl:items-center xl:justify-between gap-4">
                <div>
                  <h2 class="text-lg font-black text-slate-950">Discount Campaigns</h2>
                  <p class="mt-1 text-xs text-slate-400">{{ filteredDeals.length }} discounts found</p>
                </div>
                <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 xl:min-w-[520px]"><label class="search-box"><span
                      class="text-slate-400">⌕</span><input v-model="searchQuery" @input="resetPage" type="text"
                      placeholder="Search deal code..." /></label><select v-model="selectedStatus" @change="resetPage"
                    class="filter-box">
                    <option value="All">All statuses</option>
                    <option value="Active">Active</option>
                    <option value="Scheduled">Scheduled</option>
                    <option value="Expired">Expired</option>
                    <option value="Inactive">Inactive</option>
                  </select></div>
              </div>
              <div v-if="searchQuery || selectedStatus !== 'All'"
                class="mt-4 flex items-center justify-between rounded-xl border border-blue-100 bg-blue-50/60 px-3 py-2">
                <span class="text-[11px] font-semibold text-blue-700">Filters are active</span><button
                  @click="clearFilters" class="text-[11px] font-black text-blue-700">Clear all</button>
              </div>
            </div>

            <div v-if="isLoading" class="px-5 py-16 text-center">
              <div class="w-10 h-10 mx-auto rounded-full border-4 border-slate-200 border-t-blue-600 animate-spin">
              </div>
              <p class="mt-4 text-sm font-bold text-slate-600">Loading discounts...</p>
            </div>
            <div v-else class="overflow-x-auto">
              <table class="w-full text-left">
                <thead>
                  <tr class="border-b border-slate-200/80 bg-slate-50/60">
                    <th class="table-head pl-5">Code</th>
                    <th class="table-head">Discount</th>
                    <th class="table-head">Validity</th>
                    <th class="table-head">Status</th>
                    <th class="table-head">Control</th>
                    <th class="table-head pr-5 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-200/60">
                  <tr v-for="deal in paginatedDeals" :key="deal.id" class="order-row">
                    <td class="px-5 py-4">
                      <p class="text-sm font-black text-slate-900 tracking-wide">{{ deal.code }}</p>
                      <p class="mt-1 text-[10px] font-mono text-slate-400">{{ deal.id }}</p>
                    </td>
                    <td class="px-4 py-4"><span
                        class="inline-flex rounded-full border border-blue-200 bg-blue-50 px-2.5 py-1 text-[10px] font-black text-blue-700">{{
                          deal.discount }}% OFF</span></td>
                    <td class="px-4 py-4">
                      <p class="text-xs font-semibold text-slate-700">{{ formatDate(deal.startDate) }}</p>
                      <p class="mt-1 text-[10px] text-slate-400">to {{ formatDate(deal.endDate) }}</p>
                    </td>
                    <td class="px-4 py-4"><span
                        :class="['inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[9px] font-bold', getStatusClass(getStatus(deal))]"><span
                          class="h-1.5 w-1.5 rounded-full bg-current"></span>{{ getStatus(deal) }}</span></td>
                    <td class="px-4 py-4"><button @click="toggleDeal(deal)"
                        :class="['relative h-6 w-11 rounded-full transition', deal.enabled ? 'bg-blue-600' : 'bg-slate-300']"><span
                          :class="['absolute top-1 h-4 w-4 rounded-full bg-white shadow transition', deal.enabled ? 'left-6' : 'left-1']"></span></button><span
                        class="ml-2 text-[10px] font-bold text-slate-500">{{ deal.enabled ? 'Enabled' : 'Disabled'
                        }}</span></td>
                    <td class="px-5 py-4">
                      <div class="flex items-center justify-end gap-2"><button @click="openEditModal(deal)"
                          class="icon-action hover:border-blue-300 hover:bg-blue-50 hover:text-blue-600"
                          title="Edit discount">✎</button><button @click="openDeleteModal(deal)"
                          class="icon-action hover:border-red-300 hover:bg-red-50 hover:text-red-600"
                          title="Delete discount">×</button></div>
                    </td>
                  </tr>
                  <tr v-if="paginatedDeals.length === 0">
                    <td colspan="6" class="px-5 py-16 text-center">
                      <div
                        class="w-14 h-14 mx-auto rounded-2xl bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-400 text-xl">
                        %</div>
                      <p class="mt-4 text-sm font-bold text-slate-700">No discounts found</p>
                      <p class="mt-1 text-xs text-slate-400">Create a discount or change your filters.</p>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div
              class="px-5 sm:px-6 py-4 border-t border-slate-200/80 flex flex-col sm:flex-row items-center justify-between gap-4">
              <p class="text-xs text-slate-400">Showing <span class="font-bold text-slate-600">{{ filteredDeals.length
                ? (currentPage - 1) * itemsPerPage + 1 : 0 }}</span>–<span class="font-bold text-slate-600">{{
                    Math.min(currentPage * itemsPerPage, filteredDeals.length) }}</span> of <span
                  class="font-bold text-slate-600">{{ filteredDeals.length }}</span> discounts</p>
              <div class="flex items-center gap-2"><button @click="goToPage(currentPage - 1)"
                  :disabled="currentPage === 1" class="page-btn">‹</button><button v-for="page in totalPages"
                  :key="page" @click="goToPage(page)" :class="['page-btn', currentPage === page ? 'active' : '']">{{
                    page }}</button><button @click="goToPage(currentPage + 1)" :disabled="currentPage === totalPages"
                  class="page-btn">›</button></div>
            </div>
          </section>
        </div>
      </div>
    </main>

    <Transition name="modal">
      <div v-if="showFormModal" class="modal-shell z-[100]">
        <div class="modal-backdrop" @click="closeFormModal"></div>
        <div class="modal-card max-w-lg max-h-[calc(100vh-2rem)] flex flex-col">
          <div class="modal-header bg-gradient-to-r from-blue-50 to-cyan-50 shrink-0">
            <div>
              <p class="section-kicker text-blue-600">{{ editingDealId ? 'Edit Discount' : 'New Campaign' }}</p>
              <h2 class="mt-1 text-lg font-black text-slate-950">{{ editingDealId ? 'Update deal'
                : 'Create discount' }}</h2>
            </div><button @click="closeFormModal" class="modal-close">×</button>
          </div>
          <div class="p-5 space-y-4 overflow-y-auto">
            <div v-if="!editingDealId" class="flex flex-col gap-1.5">
              <label class="text-xs font-bold text-slate-700">Apply To</label>
              <div class="flex flex-wrap gap-4 mb-2">
                <label class="flex items-center gap-2"><input type="radio" v-model="form.applyType" value="single" class="accent-blue-600"/> <span class="text-xs font-semibold">Single Product</span></label>
                <label class="flex items-center gap-2"><input type="radio" v-model="form.applyType" value="multiple" class="accent-blue-600"/> <span class="text-xs font-semibold">Multiple Products</span></label>
                <label class="flex items-center gap-2"><input type="radio" v-model="form.applyType" value="category" class="accent-blue-600"/> <span class="text-xs font-semibold">Category</span></label>
              </div>
            </div>

            <div v-if="form.applyType === 'single' || editingDealId">
              <label class="form-label">Product</label>
              <select v-model="form.productId" class="form-input">
                <option value="" disabled>Select a product...</option>
                <option v-for="product in products" :key="product.productId || product.id" :value="product.productId || product.id">{{ product.name || product.productName || 'Unnamed' }}</option>
              </select>
            </div>
            
            <div v-if="form.applyType === 'multiple' && !editingDealId">
              <label class="form-label mb-2 block">Select Products</label>
              <div class="max-h-48 overflow-y-auto rounded-xl border border-slate-200 bg-slate-50 p-2 space-y-1">
                <label v-for="product in products" :key="product.productId || product.id" class="flex items-center gap-3 p-2 hover:bg-white rounded-lg cursor-pointer border border-transparent hover:border-slate-200 transition-colors">
                  <div class="relative flex items-center">
                    <input type="checkbox" v-model="form.productIds" :value="product.productId || product.id" class="peer sr-only">
                    <div class="w-10 h-5 bg-slate-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-blue-600"></div>
                  </div>
                  <span class="text-sm text-slate-700 font-medium select-none">{{ product.name || product.productName || 'Unnamed' }}</span>
                </label>
              </div>
            </div>

            <div v-if="form.applyType === 'category' && !editingDealId">
              <label class="form-label">Category</label>
              <select v-model="form.categoryId" class="form-input">
                <option value="" disabled>Select a category...</option>
                <option v-for="cat in categories" :key="cat.categoryId || cat.id" :value="cat.categoryId || cat.id">{{ cat.categoryName || cat.name || 'Unnamed' }}</option>
              </select>
            </div>
<div>
  <label class="form-label">Badge Text</label>
  <input v-model="form.badgeText" type="text" placeholder="e.g. FLASH DEAL" class="form-input" />
</div>
            <div><label class="form-label">Discount Percentage</label>
              <div class="relative"><input v-model.number="form.discount" min="1" max="100" type="number"
                  class="form-input pr-10" /><span
                  class="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-black text-slate-400">%</span></div>
            </div>
            <div class="grid gap-3 sm:grid-cols-2">
              <div><label class="form-label">Start Date</label><input v-model="form.startDate" type="date"
                  class="form-input" /></div>
              <div><label class="form-label">End Date</label><input v-model="form.endDate" type="date"
                  :min="form.startDate || todayISO" class="form-input" /></div>
            </div>
            <div class="flex items-center justify-between rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-3 shrink-0">
              <div>
                <p class="text-xs font-bold text-slate-800">Enable campaign</p>
                <p class="mt-0.5 text-[10px] text-slate-400">Customers can use the deal while enabled.</p>
              </div><button @click="form.enabled = !form.enabled"
                :class="['relative h-6 w-11 rounded-full transition', form.enabled ? 'bg-blue-600' : 'bg-slate-300']"><span
                  :class="['absolute top-1 h-4 w-4 rounded-full bg-white shadow transition', form.enabled ? 'left-6' : 'left-1']"></span></button>
            </div>
          </div>
          <div class="modal-footer shrink-0"><button @click="closeFormModal" class="btn-secondary">Cancel</button><button
              @click="saveDeal" :disabled="isSaving" class="btn-primary bg-blue-600 hover:bg-blue-700">{{ isSaving ?
                'Saving...' : (editingDealId ? 'Update Discount' : 'Create Discount') }}</button></div>
        </div>
      </div>
    </Transition>

    <!-- =====================================================
         DELETE DISCOUNT MODAL (ORDER MANAGEMENT DESIGN)
         ===================================================== -->
    <AdminConfirmModal
      v-model:show="showDeleteModal"
      type="danger"
      title="Delete Discount?"
      message="Permanently delete deal for"
      :target="selectedDeal?.productName"
      description="This discount deal will be permanently removed. This action cannot be undone."
      confirm-text="Delete Discount"
      cancel-text="Keep Discount"
      @confirm="deleteDeal"
    />
  </div>
</template>

<style scoped>
@reference "../../style.css";

.kpi-card {
  @apply rounded-2xl border border-slate-200/80 bg-white p-4 shadow-sm transition duration-200 hover:-translate-y-0.5 hover:shadow-md;
}

.kpi-revenue {
  @apply border-transparent bg-gradient-to-br from-blue-600 via-blue-600 to-cyan-500 shadow-lg shadow-blue-500/15;
}

.kpi-label {
  @apply text-[10px] font-bold uppercase tracking-[0.14em] text-slate-400;
}

.kpi-value {
  @apply mt-1.5 text-2xl font-black tracking-tight text-slate-950;
}

.kpi-note {
  @apply mt-2 text-[10px] font-semibold text-slate-400;
}

.kpi-icon {
  @apply flex h-9 w-9 shrink-0 items-center justify-center rounded-xl font-black;
}

.workflow-step {
  @apply rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-slate-600 shadow-sm;
}

.workflow-step.active {
  @apply border-blue-100 bg-blue-50 text-blue-700;
}

.search-box {
  @apply flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2.5 transition focus-within:border-blue-400 focus-within:ring-4 focus-within:ring-blue-500/10;
}

.search-box input {
  @apply min-w-0 w-full bg-transparent text-xs text-slate-800 outline-none placeholder:text-slate-400;
}

.filter-box {
  @apply w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-xs font-semibold text-slate-700 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10;
}

.table-head {
  @apply px-4 py-3 text-[9px] font-black uppercase tracking-[0.14em] text-slate-400;
}

.order-row {
  @apply transition-colors hover:bg-blue-50/30;
}

.icon-action {
  @apply flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-400 text-sm font-black transition;
}

.page-btn {
  @apply flex h-8 min-w-8 items-center justify-center rounded-lg border border-slate-200 bg-white px-2 text-[10px] font-black text-slate-500 transition hover:border-blue-300 hover:text-blue-600 disabled:cursor-not-allowed disabled:opacity-35;
}

.page-btn.active {
  @apply border-blue-600 bg-blue-600 text-white shadow-sm shadow-blue-500/20;
}

.form-label {
  @apply mb-1.5 block text-xs font-bold text-slate-700;
}

.form-input {
  @apply w-full rounded-xl border border-slate-200 bg-white px-3.5 py-3 text-sm font-semibold text-slate-800 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10;
}

.modal-shell {
  @apply fixed inset-0 flex items-center justify-center p-4;
}

.modal-backdrop {
  @apply absolute inset-0 bg-slate-950/30 backdrop-blur-sm;
}

.modal-card {
  @apply relative w-full overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl shadow-slate-950/20;
}

.modal-header {
  @apply flex items-center justify-between border-b border-slate-200 px-5 py-4;
}

.modal-footer {
  @apply flex items-center justify-end gap-2.5 border-t border-slate-200 bg-slate-50 px-5 py-3.5;
}

.modal-close {
  @apply flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 bg-white text-lg leading-none text-slate-400 transition hover:border-slate-300 hover:text-slate-700;
}

.btn-secondary {
  @apply rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-xs font-black text-slate-600 transition hover:bg-slate-50;
}

.btn-primary {
  @apply rounded-xl px-4 py-2.5 text-xs font-black text-white shadow-sm transition disabled:cursor-not-allowed disabled:opacity-50;
}

.section-kicker {
  @apply text-[9px] font-black uppercase tracking-[0.16em] text-slate-400;
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity .2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-active .modal-card,
.modal-leave-active .modal-card {
  transition: transform .22s ease, opacity .22s ease;
}

.modal-enter-from .modal-card,
.modal-leave-to .modal-card {
  opacity: 0;
  transform: translateY(10px) scale(.985);
}

@media (max-width: 1023px) {
  main {
    margin-left: 0 !important;
  }
}
</style>
