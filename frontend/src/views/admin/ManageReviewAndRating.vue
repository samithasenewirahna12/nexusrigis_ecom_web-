<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminConfirmModal from '../../components/admin/AdminConfirmModal.vue'
import api from '../../services/api'

interface Review {
    id: string
    customerId: string
    customerName: string
    customerImage?: string
    productId: string
    productName: string
    rating: number
    comment: string
    date: string
}

const reviews = ref<Review[]>([])
const isLoading = ref(false)
const loadError = ref('')
const successMessage = ref('')

const searchQuery = ref('')
const selectedRating = ref('All')
const currentPage = ref(1)
const itemsPerPage = 6

const showDetailsModal = ref(false)
const showDeleteModal = ref(false)
const selectedReview = ref<Review | null>(null)

const normalizeReview = (item: any): Review => ({
    id: String(item.reviewId ?? item.id ?? ''),
    customerId: String(item.customerId ?? item.userId ?? '—'),
    customerName: String(item.customerName ?? item.customer?.name ?? item.customerId ?? 'Unknown Customer'),
    customerImage: item.customerImage || '',
    productId: String(item.productId ?? item.product?.productId ?? '—'),
    productName: String(item.productName ?? item.product?.name ?? item.productId ?? 'Unknown Product'),
    rating: Math.min(5, Math.max(0, Number(item.rating ?? 0))),
    comment: String(item.comment ?? item.review ?? ''),
    date: String(item.reviewDate ?? item.createdAt ?? item.date ?? '')
})

const fetchReviews = async () => {
    isLoading.value = true
    loadError.value = ''
    try {
        const { data } = await api.get('/reviews')
        const list = Array.isArray(data) ? data : Array.isArray(data?.content) ? data.content : []
        reviews.value = list.map(normalizeReview).reverse()
    } catch (err: any) {
        loadError.value = err?.response?.data?.message || 'Failed to load ratings.'
    } finally {
        isLoading.value = false
    }
}

onMounted(fetchReviews)

const totalReviews = computed(() => reviews.value.length)
const averageRating = computed(() => {
    if (!reviews.value.length) return 0
    return reviews.value.reduce((sum, review) => sum + review.rating, 0) / reviews.value.length
})
const fiveStarReviews = computed(() => reviews.value.filter(review => review.rating === 5).length)
const lowRatingReviews = computed(() => reviews.value.filter(review => review.rating <= 2).length)

const filteredReviews = computed(() => {
    const search = searchQuery.value.toLowerCase().trim()
    return reviews.value.filter(review => {
        const matchesSearch = !search ||
            review.productName.toLowerCase().includes(search) ||
            review.productId.toLowerCase().includes(search) ||
            review.customerName.toLowerCase().includes(search) ||
            review.comment.toLowerCase().includes(search)

        const matchesRating = selectedRating.value === 'All' || review.rating === Number(selectedRating.value)
        return matchesSearch && matchesRating
    })
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredReviews.value.length / itemsPerPage)))
const paginatedReviews = computed(() => {
    const start = (currentPage.value - 1) * itemsPerPage
    return filteredReviews.value.slice(start, start + itemsPerPage)
})

const resetPage = () => { currentPage.value = 1 }
const clearFilters = () => {
    searchQuery.value = ''
    selectedRating.value = 'All'
    currentPage.value = 1
}
const goToPage = (page: number) => {
    if (page < 1 || page > totalPages.value) return
    currentPage.value = page
}

const openDetails = (review: Review) => {
    selectedReview.value = review
    showDetailsModal.value = true
}
const closeDetails = () => {
    showDetailsModal.value = false
    selectedReview.value = null
}
const openDeleteModal = (review: Review) => {
    selectedReview.value = review
    showDeleteModal.value = true
}

const deleteReview = async () => {
    if (!selectedReview.value?.id) return
    const id = selectedReview.value.id
    try {
        await api.delete(`/reviews/${id}`)
        reviews.value = reviews.value.filter(review => review.id !== id)
        successMessage.value = 'Rating deleted successfully.'
        setTimeout(() => { successMessage.value = '' }, 4000)
    } catch (err: any) {
        loadError.value = err?.response?.data?.message || 'Failed to delete rating.'
        setTimeout(() => { loadError.value = '' }, 5000)
    } finally {
        showDeleteModal.value = false
        selectedReview.value = null
    }
}

const formatDate = (date: string) => {
    if (!date) return '—'
    const parsed = new Date(date)
    if (Number.isNaN(parsed.getTime())) return date
    return parsed.toLocaleDateString('en-LK', { year: 'numeric', month: 'short', day: 'numeric' })
}

const getInitials = (name: string) => {
    if (!name) return 'CU'
    return name.split(' ').map(word => word.charAt(0)).slice(0, 2).join('').toUpperCase()
}

const getRatingClass = (rating: number) => {
    if (rating >= 4) return 'bg-emerald-50 border-emerald-200 text-emerald-700'
    if (rating === 3) return 'bg-amber-50 border-amber-200 text-amber-700'
    return 'bg-red-50 border-red-200 text-red-700'
}
</script>

<template>
    <div class="min-h-screen bg-slate-100">
        <AdminSidebar />

        <main class="ml-64 min-h-screen">
            <div class="relative min-h-screen overflow-hidden bg-slate-100 text-slate-900">
                <div class="fixed inset-0 pointer-events-none bg-gradient-to-br from-white via-slate-50 to-blue-50/70">
                </div>
                <div
                    class="fixed -top-40 -right-40 w-[500px] h-[500px] rounded-full bg-cyan-400/10 blur-3xl pointer-events-none">
                </div>
                <div
                    class="fixed -bottom-40 -left-40 w-[500px] h-[500px] rounded-full bg-blue-500/10 blur-3xl pointer-events-none">
                </div>

                <div class="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                    <header class="mb-8 flex flex-col lg:flex-row lg:items-center lg:justify-between gap-5">
                        <div>
                            <div class="flex items-center gap-3 flex-wrap">
                                <span
                                    class="px-2.5 py-1 rounded-md bg-blue-50 border border-blue-200 text-blue-600 text-[10px] font-bold uppercase tracking-widest">Admin
                                    Panel</span>
                                <span class="text-xs text-slate-400 font-semibold">/ Rating Management</span>
                            </div>
                            <h1 class="mt-3 text-3xl sm:text-4xl font-black text-slate-950 tracking-tight">Rating
                                Management</h1>
                            <p class="mt-2 text-sm text-slate-500">Review customer ratings, product feedback, and remove
                                inappropriate reviews.</p>
                        </div>

                        <div
                            class="flex items-center gap-2 px-4 py-3 rounded-xl bg-white/70 backdrop-blur-xl border border-white/90 shadow-lg shadow-slate-200/20">
                            <span class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                            <span class="text-xs font-bold text-emerald-600">Rating System Online</span>
                        </div>
                    </header>

                    <div v-if="successMessage"
                        class="mb-5 p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-bold flex items-center justify-between">
                        <span>✓ {{ successMessage }}</span>
                        <button @click="successMessage = ''">×</button>
                    </div>
                    <div v-if="loadError"
                        class="mb-5 p-3 rounded-xl bg-red-50 border border-red-200 text-red-700 text-xs font-semibold">
                        {{ loadError }}</div>

                    <section class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-5 gap-5 mb-8">
                        <div class="kpi-card">
                            <div class="flex justify-between items-start">
                                <div>
                                    <p class="kpi-label">Total Ratings</p>
                                    <p class="kpi-value">{{ totalReviews }}</p>
                                </div><span class="kpi-icon bg-blue-50 text-blue-600">★</span>
                            </div>
                            <p class="kpi-note">All customer reviews</p>
                        </div>
                        <div class="kpi-card">
                            <div class="flex justify-between items-start">
                                <div>
                                    <p class="kpi-label">Average Rating</p>
                                    <p class="kpi-value">{{ averageRating.toFixed(1) }} <span
                                            class="text-sm text-amber-500">★</span></p>
                                </div><span class="kpi-icon bg-amber-50 text-amber-600">★</span>
                            </div>
                            <p class="kpi-note text-amber-600">Overall product score</p>
                        </div>
                        <div class="kpi-card">
                            <div class="flex justify-between items-start">
                                <div>
                                    <p class="kpi-label">5 Star</p>
                                    <p class="kpi-value">{{ fiveStarReviews }}</p>
                                </div><span class="kpi-icon bg-emerald-50 text-emerald-600">★</span>
                            </div>
                            <p class="kpi-note text-emerald-600">Excellent feedback</p>
                        </div>
                        <div class="kpi-card">
                            <div class="flex justify-between items-start">
                                <div>
                                    <p class="kpi-label">Low Ratings</p>
                                    <p class="kpi-value">{{ lowRatingReviews }}</p>
                                </div><span class="kpi-icon bg-red-50 text-red-600">!</span>
                            </div>
                            <p class="kpi-note text-red-600">1–2 star reviews</p>
                        </div>
                        <div class="kpi-card kpi-revenue">
                            <div class="flex justify-between items-start">
                                <div>
                                    <p class="text-[10px] font-bold uppercase tracking-[0.14em] text-white/70">Quality
                                        Score</p>
                                    <p class="mt-1.5 text-2xl font-black text-white">{{ averageRating.toFixed(1) }}/5
                                    </p>
                                </div><span class="kpi-icon bg-white/15 text-white">✓</span>
                            </div>
                            <p class="mt-2 text-[10px] font-semibold text-white/70">Customer satisfaction</p>
                        </div>
                    </section>

                    <section
                        class="bg-white/65 backdrop-blur-2xl border border-white/90 rounded-3xl shadow-xl shadow-slate-300/20 overflow-hidden">
                        <div class="p-5 sm:p-6 border-b border-slate-200/80">
                            <div
                                class="mb-5 p-4 rounded-2xl bg-gradient-to-r from-blue-50/90 via-indigo-50/90 to-cyan-50/90 border border-blue-100 flex flex-col md:flex-row md:items-center justify-between gap-4">
                                <div class="flex items-center gap-3">
                                    <div
                                        class="w-10 h-10 rounded-xl bg-blue-600 text-white flex items-center justify-center font-black">
                                        ★</div>
                                    <div>
                                        <h3 class="text-xs font-black text-slate-900 tracking-wide uppercase">Customer
                                            Rating Workflow</h3>
                                        <p class="text-[11px] text-slate-500 mt-0.5">Monitor ratings → inspect feedback
                                            → remove reviews when necessary.</p>
                                    </div>
                                </div>
                                <span class="workflow-step active">Reviews &amp; Ratings</span>
                            </div>

                            <div class="flex flex-col xl:flex-row xl:items-center xl:justify-between gap-4">
                                <div>
                                    <h2 class="text-lg font-black text-slate-950">Customer Ratings</h2>
                                    <p class="mt-1 text-xs text-slate-400">{{ filteredReviews.length }} ratings found
                                    </p>
                                </div>
                                <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 xl:min-w-[520px]">
                                    <label class="search-box"><span class="text-slate-400">⌕</span><input
                                            v-model="searchQuery" @input="resetPage" type="text"
                                            placeholder="Search product, customer or review..." /></label>
                                    <select v-model="selectedRating" @change="resetPage" class="filter-box">
                                        <option value="All">All ratings</option>
                                        <option v-for="rating in [5, 4, 3, 2, 1]" :key="rating" :value="String(rating)">
                                            {{
                                                rating }} star{{ rating > 1 ? 's' : '' }}</option>
                                    </select>
                                </div>
                            </div>
                            <div v-if="searchQuery || selectedRating !== 'All'"
                                class="mt-4 flex items-center justify-between rounded-xl border border-blue-100 bg-blue-50/60 px-3 py-2">
                                <span class="text-[11px] font-semibold text-blue-700">Filters are active</span><button
                                    @click="clearFilters" class="text-[11px] font-black text-blue-700">Clear
                                    all</button>
                            </div>
                        </div>

                        <div v-if="isLoading" class="px-5 py-16 text-center">
                            <div
                                class="w-10 h-10 mx-auto rounded-full border-4 border-slate-200 border-t-blue-600 animate-spin">
                            </div>
                            <p class="mt-4 text-sm font-bold text-slate-600">Loading ratings...</p>
                        </div>
                        <div v-else class="overflow-x-auto">
                            <table class="w-full text-left">
                                <thead>
                                    <tr class="border-b border-slate-200/80 bg-slate-50/60">
                                        <th class="table-head pl-5">Product</th>
                                        <th class="table-head">Customer</th>
                                        <th class="table-head">Rating</th>
                                        <th class="table-head">Review</th>
                                        <th class="table-head">Date</th>
                                        <th class="table-head pr-5 text-right">Actions</th>
                                    </tr>
                                </thead>
                                <tbody class="divide-y divide-slate-200/60">
                                    <tr v-for="review in paginatedReviews" :key="review.id" class="order-row">
                                        <td class="px-5 py-4">
                                            <p class="text-xs font-black text-slate-900">{{ review.productName }}</p>
                                            <p class="mt-1 text-[10px] font-mono text-slate-400">{{ review.productId }}
                                            </p>
                                        </td>
                                        <td class="px-4 py-4">
                                            <div class="flex items-center gap-2.5 min-w-[170px]">
                                                <img v-if="review.customerImage" :src="review.customerImage" :alt="review.customerName" class="w-8 h-8 rounded-full object-cover shrink-0" />
                                                <div v-else
                                                    class="w-8 h-8 rounded-full bg-gradient-to-br from-blue-500 to-cyan-400 text-white flex items-center justify-center text-[10px] font-black shrink-0">
                                                    {{ getInitials(review.customerName) }}</div>
                                                <div class="min-w-0">
                                                    <p class="truncate text-xs font-bold text-slate-900">{{
                                                        review.customerName }}</p>
                                                    <p class="truncate mt-0.5 text-[10px] text-slate-400">{{
                                                        review.customerId }}</p>
                                                </div>
                                            </div>
                                        </td>
                                        <td class="px-4 py-4"><span
                                                :class="['inline-flex items-center gap-1 rounded-full border px-2.5 py-1 text-[9px] font-bold', getRatingClass(review.rating)]">{{
                                                    review.rating.toFixed(1) }} ★</span></td>
                                        <td class="px-4 py-4 max-w-[320px]">
                                            <p class="truncate text-xs font-semibold text-slate-600">{{ review.comment
                                                || 'No written feedback' }}</p>
                                        </td>
                                        <td class="px-4 py-4"><span class="text-xs font-semibold text-slate-500">{{
                                            formatDate(review.date) }}</span></td>
                                        <td class="px-5 py-4">
                                            <div class="flex items-center justify-end gap-2"><button
                                                    @click="openDetails(review)"
                                                    class="icon-action hover:border-blue-300 hover:bg-blue-50 hover:text-blue-600"
                                                    title="View rating">⌁</button><button
                                                    @click="openDeleteModal(review)"
                                                    class="icon-action hover:border-red-300 hover:bg-red-50 hover:text-red-600"
                                                    title="Delete rating">×</button></div>
                                        </td>
                                    </tr>
                                    <tr v-if="paginatedReviews.length === 0">
                                        <td colspan="6" class="px-5 py-16 text-center">
                                            <div
                                                class="w-14 h-14 mx-auto rounded-2xl bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-400 text-xl">
                                                ★</div>
                                            <p class="mt-4 text-sm font-bold text-slate-700">No ratings found</p>
                                            <p class="mt-1 text-xs text-slate-400">Try changing your search or filters.
                                            </p>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>

                        <div
                            class="px-5 sm:px-6 py-4 border-t border-slate-200/80 flex flex-col sm:flex-row items-center justify-between gap-4">
                            <p class="text-xs text-slate-400">Showing <span class="font-bold text-slate-600">{{
                                filteredReviews.length ? (currentPage - 1) * itemsPerPage + 1 : 0 }}</span>–<span
                                    class="font-bold text-slate-600">{{ Math.min(currentPage * itemsPerPage,
                                        filteredReviews.length) }}</span> of <span class="font-bold text-slate-600">{{
                                        filteredReviews.length }}</span> ratings</p>
                            <div class="flex items-center gap-2"><button @click="goToPage(currentPage - 1)"
                                    :disabled="currentPage === 1" class="page-btn">‹</button><button
                                    v-for="page in totalPages" :key="page" @click="goToPage(page)"
                                    :class="['page-btn', currentPage === page ? 'active' : '']">{{ page
                                    }}</button><button @click="goToPage(currentPage + 1)"
                                    :disabled="currentPage === totalPages" class="page-btn">›</button></div>
                        </div>
                    </section>
                </div>
            </div>
        </main>

        <Transition name="modal">
            <div v-if="showDetailsModal && selectedReview" class="modal-shell z-[100]">
                <div class="modal-backdrop" @click="closeDetails"></div>
                <div class="modal-card max-w-2xl max-h-[90vh] overflow-y-auto">
                    <div class="modal-header">
                        <div>
                            <p class="section-kicker text-blue-600">Rating Details</p>
                            <h2 class="mt-1 text-lg font-black text-slate-950">{{ selectedReview.productName }}</h2>
                        </div><button @click="closeDetails" class="modal-close">×</button>
                    </div>
                    <div class="p-5 space-y-4">
                        <div class="grid gap-3 md:grid-cols-2">
                            <div class="detail-card">
                                <p class="section-kicker">Customer</p>
                                <div class="mt-3 flex items-center gap-3">
                                    <img v-if="selectedReview.customerImage" :src="selectedReview.customerImage" :alt="selectedReview.customerName" class="w-10 h-10 rounded-full object-cover shrink-0" />
                                    <div v-else class="w-10 h-10 rounded-full bg-gradient-to-br from-blue-500 to-cyan-400 text-white flex items-center justify-center text-xs font-black shrink-0">
                                        {{ getInitials(selectedReview.customerName) }}
                                    </div>
                                    <div>
                                        <p class="text-sm font-black text-slate-900">{{ selectedReview.customerName }}</p>
                                        <p class="text-xs text-slate-400 mt-0.5">{{ selectedReview.customerId }}</p>
                                    </div>
                                </div>
                            </div>
                            <div class="detail-card">
                                <p class="section-kicker">Rating</p><span
                                    :class="['mt-2 inline-flex rounded-full border px-3 py-1 text-xs font-black', getRatingClass(selectedReview.rating)]">{{
                                        selectedReview.rating.toFixed(1) }} / 5 ★</span>
                            </div>
                        </div>
                        <div class="detail-card">
                            <p class="section-kicker">Customer Feedback</p>
                            <p class="mt-3 text-sm leading-relaxed text-slate-700">{{ selectedReview.comment
                                || 'No written feedback was provided.' }}</p>
                        </div>
                        <div class="grid gap-3 md:grid-cols-2">
                            <div class="detail-card">
                                <p class="section-kicker">Product</p>
                                <p class="mt-2 text-xs font-bold text-slate-800">{{ selectedReview.productName }}</p>
                                <p class="mt-1 text-[10px] font-mono text-slate-400">{{ selectedReview.productId }}</p>
                            </div>
                            <div class="detail-card">
                                <p class="section-kicker">Submitted</p>
                                <p class="mt-2 text-xs font-bold text-slate-800">{{ formatDate(selectedReview.date) }}
                                </p>
                            </div>
                        </div><button @click="closeDetails"
                            class="w-full rounded-xl bg-slate-900 px-5 py-3 text-sm font-bold text-white hover:bg-slate-800">Close
                            Rating</button>
                    </div>
                </div>
            </div>
        </Transition>

        <!-- =====================================================
             DELETE RATING MODAL (ORDER MANAGEMENT DESIGN)
             ===================================================== -->
        <AdminConfirmModal
            v-model:show="showDeleteModal"
            type="danger"
            title="Delete Rating?"
            message="Permanently delete the rating from"
            :target="selectedReview?.customerName"
            :description="selectedReview ? `For product: ${selectedReview.productName}. This action cannot be undone.` : ''"
            confirm-text="Delete Rating"
            cancel-text="Keep Rating"
            @confirm="deleteReview"
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

.modal-close {
    @apply flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 bg-white text-lg leading-none text-slate-400 transition hover:border-slate-300 hover:text-slate-700;
}

.btn-secondary {
    @apply rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-xs font-black text-slate-600 transition hover:bg-slate-50;
}

.detail-card {
    @apply rounded-xl border border-slate-200 bg-slate-50/70 p-4;
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
