<script setup lang="ts">

import { computed, ref, onMounted } from 'vue'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminConfirmModal from '../../components/admin/AdminConfirmModal.vue'
import api from '../../services/api'
import { getActiveStaffRole } from '../../utils/rbac'

const isAdmin = computed(() => getActiveStaffRole() === 'ADMINISTRATOR')


// =========================================================
// TYPES
// =========================================================

interface Delivery {
    id: string
    deliveryId: string
    orderId: string
    customerName: string
    customerEmail: string
    customerPhone: string
    customerId: string
    customerImage?: string
    address: string
    city: string
    postalCode: string

    staffId: string
    staffName: string
    staffImage?: string

    assignedDate: string
    expectedDate: string
    deliveredDate: string

    status: string
    paymentMethod: string
    paymentStatus: string

    amount: number
}


// =========================================================
// STATE
// =========================================================

const deliveries = ref<Delivery[]>([])

const isLoading = ref(false)
const loadError = ref('')

const searchQuery = ref('')

const selectedStatus = ref('All')

const selectedStaff = ref('All')

const currentPage = ref(1)

const itemsPerPage = 6

// Tracks the date input value in the "edit expected date" UI
const editingExpectedDate = ref('')

const isSavingDate = ref(false)


// =========================================================
// MODALS
// =========================================================

const showDetailsModal = ref(false)

const showAssignModal = ref(false)

const showDeleteModal = ref(false)

const selectedDelivery = ref<Delivery | null>(null)

const deliveryStaffList = ref<{ id: string; name: string; image?: string }[]>([])


// =========================================================
// OPTIONS
// =========================================================

const statusOptions = [
    'In Transit',
    'Out for Delivery',
    'Delivered',
    'Failed',
    'Returned'
]


// =========================================================
// PLACEHOLDER
// =========================================================

const PLACEHOLDER_STAFF = 'Delivery Staff'


// =========================================================
// NORMALIZE STATUS
// =========================================================

const normalizeStatus = (
    raw: string | null | undefined
) => {

    if (!raw) {
        return 'Pending'
    }

    const clean = raw.trim().toUpperCase().replace(/-/g, '_').replace(/ /g, '_')
    switch (clean) {
        case 'PENDING':
            return 'Pending'
        case 'ORDER_CONFIRMED':
            return 'Order Confirmed'
        case 'PICKED_AND_PACKED':
            return 'Picked & Packed'
        case 'IN_TRANSIT':
            return 'In Transit'
        case 'OUT_FOR_DELIVERY':
            return 'Out for Delivery'
        case 'DELIVERED':
            return 'Delivered'
        case 'CANCELLED':
            return 'Cancelled'
        case 'PAID':
            return 'Paid'
        default:
            return raw
    }
}

// Only show delivery steps for orders that have been handed over from Order Management
// Order Management ends at "In Transit" → Delivery Management picks up from there
const getNextDeliveryStep = (status: string) => {
    switch (status) {
        case 'In Transit':
            return { next: 'Out for Delivery', label: '1. Out for Delivery', class: 'bg-cyan-600 hover:bg-cyan-700 text-white shadow-cyan-500/20' }
        case 'Out for Delivery':
            return { next: 'Delivered', label: '2. Mark Delivered', class: 'bg-emerald-600 hover:bg-emerald-700 text-white shadow-emerald-500/20' }
        default:
            return null
    }
}

// Returns true if the order is still in Order Management (not yet handed over)
const isStillInOrderManagement = (status: string) => {
    return ['Pending', 'Order Confirmed', 'Picked & Packed'].includes(status)
}


// =========================================================
// LOAD DELIVERIES
// =========================================================

const fetchDeliveries = async () => {

    isLoading.value = true

    loadError.value = ''

    try {

        const { data } =
            await api.get('/deliveries')

        deliveries.value =
            (data ?? []).map((item: any) => {

                return {

                    id:
                        item.deliveryId ??
                        item.id ??
                        '',

                    deliveryId:
                        item.deliveryId ??
                        item.id ??
                        'DEL-000',

                    orderId:
                        item.orderId ??
                        '—',

                    customerName:
                        item.customerName ||
                        item.customerId ||
                        'Unknown Customer',

                    customerId: item.customerId || 'N/A',

                    customerImage: item.customerImage || '',

                    customerEmail:
                        item.customerEmail ??
                        '',

                    customerPhone:
                        item.customerPhone ??
                        '',

                    address:
                        item.address ??
                        item.shippingAddress ??
                        'Address not available',

                    city:
                        item.city ??
                        '',

                    postalCode:
                        item.postalCode ??
                        '',

                    staffId:
                        item.deliveryStaffId ??
                        item.staffId ??
                        '',

                    staffName:
                        item.deliveryStaffName ??
                        item.staffName ??
                        PLACEHOLDER_STAFF,

                    staffImage: 
                        item.deliveryStaffImage ?? 
                        item.staffImage ?? 
                        '',

                    assignedDate:
                        item.assignedDate ??
                        item.createdAt ??
                        '',

                    // Backend field `deliveryDate` = the scheduled/expected delivery date
                    expectedDate:
                        item.expectedDate ??
                        item.estimatedDeliveryDate ??
                        item.deliveryDate ??
                        '',

                    // Delivered date is only set once status becomes 'Delivered'
                    deliveredDate:
                        item.deliveredDate ??
                        (item.status === 'Delivered' ? item.deliveryDate : '') ??
                        '',

                    status:
                        normalizeStatus(
                            item.status
                        ),

                    paymentMethod:
                        normalizeStatus(
                            item.paymentMethod ??
                            'N/A'
                        ),

                    paymentStatus:
                        normalizeStatus(
                            item.paymentStatus ??
                            'Pending'
                        ),

                    amount:
                        Number(
                            item.amount ??
                            item.totalAmount ??
                            0
                        )
                }

            }).reverse()

    } catch (err: any) {

        loadError.value =
            err?.response?.data?.message ??
            'Failed to load deliveries.'

        console.error(
            'Failed to load deliveries:',
            err
        )

    } finally {

        isLoading.value = false

    }

}


// =========================================================
// LOAD DELIVERY STAFF
// =========================================================

const loadDeliveryStaff = async () => {
    try {
        const { data } = await api.get('/delivery-staff')
        const list = Array.isArray(data) ? data : Array.isArray(data?.content) ? data.content : []
        deliveryStaffList.value = list.map((s: any) => ({
            id: s.userId || s.id || '',
            name: s.name || s.email || 'Staff',
            image: s.userImage || s.image || s.profileImage || s.avatar || s.imageUrl || ''
        }))
    } catch {
        deliveryStaffList.value = []
    }
}


// =========================================================
// ON MOUNT
// =========================================================

onMounted(() => {
    fetchDeliveries()
    loadDeliveryStaff()
})


// =========================================================
// STAFF LIST
// =========================================================

const staffOptions = computed(() => {

    // Prefer loaded staff from backend
    if (deliveryStaffList.value.length > 0) {
        return deliveryStaffList.value.map(s => s.name)
    }

    // Fallback: names from existing deliveries
    const names =
        deliveries.value
            .map(delivery =>
                delivery.staffName
            )
            .filter(name =>
                name &&
                name !== PLACEHOLDER_STAFF
            )

    return [
        ...new Set(names)
    ]

})

const getStaffImage = (name: string) => {
    const staff = deliveryStaffList.value.find(s => s.name === name)
    return staff?.image || ''
}


// =========================================================
// FILTER
// =========================================================

const filteredDeliveries = computed(() => {

    const search =
        searchQuery.value
            .toLowerCase()
            .trim()

    return deliveries.value.filter(
        delivery => {
            const normalizedStatus = normalizeStatus(delivery.status)
            const isDeliveryStage = !['Pending', 'Order Confirmed', 'Picked & Packed', 'Paid'].includes(normalizedStatus)

            const matchesSearch =
                !search ||

                delivery.deliveryId
                    .toLowerCase()
                    .includes(search) ||

                delivery.orderId
                    .toLowerCase()
                    .includes(search) ||

                delivery.customerName
                    .toLowerCase()
                    .includes(search) ||

                delivery.staffName
                    .toLowerCase()
                    .includes(search) ||

                delivery.city
                    .toLowerCase()
                    .includes(search)


            const matchesStatus =
                selectedStatus.value === 'All' ||
                normalizedStatus ===
                selectedStatus.value


            const matchesStaff =
                selectedStaff.value === 'All' ||
                delivery.staffName ===
                selectedStaff.value


            return (
                isDeliveryStage &&
                matchesSearch &&
                matchesStatus &&
                matchesStaff
            )

        }
    )

})


// =========================================================
// PAGINATION
// =========================================================

const totalPages = computed(() => {

    return Math.max(
        1,
        Math.ceil(
            filteredDeliveries.value.length /
            itemsPerPage
        )
    )

})


const paginatedDeliveries = computed(() => {

    const start =
        (currentPage.value - 1) *
        itemsPerPage

    return filteredDeliveries.value.slice(
        start,
        start + itemsPerPage
    )

})


// =========================================================
// STATISTICS
// =========================================================

const visibleDeliveries = computed(() =>
    deliveries.value.filter(
        delivery => !['Pending', 'Order Confirmed', 'Picked & Packed', 'Paid'].includes(normalizeStatus(delivery.status))
    )
)

const totalDeliveries = computed(() =>
    visibleDeliveries.value.length
)


const pendingDeliveries = computed(() =>
    visibleDeliveries.value.filter(
        delivery =>
            normalizeStatus(delivery.status) === 'In Transit'
    ).length
)


const outForDelivery = computed(() =>
    visibleDeliveries.value.filter(
        delivery =>
            normalizeStatus(delivery.status) ===
            'Out for Delivery'
    ).length
)


const deliveredCount = computed(() =>
    visibleDeliveries.value.filter(
        delivery =>
            normalizeStatus(delivery.status) ===
            'Delivered'
    ).length
)


const failedDeliveries = computed(() =>
    visibleDeliveries.value.filter(
        delivery =>
            normalizeStatus(delivery.status) === 'Failed' ||
            normalizeStatus(delivery.status) === 'Returned' ||
            normalizeStatus(delivery.status) === 'Cancelled'
    ).length
)


// =========================================================
// PAGINATION HELPERS
// =========================================================

const resetPage = () => {

    currentPage.value = 1

}


const goToPage = (
    page: number
) => {

    if (
        page < 1 ||
        page > totalPages.value
    ) {
        return
    }

    currentPage.value = page

}


const clearFilters = () => {

    searchQuery.value = ''

    selectedStatus.value = 'All'

    selectedStaff.value = 'All'

    currentPage.value = 1

}


// =========================================================
// FORMAT DATE
// =========================================================

const formatDate = (
    date: string
) => {

    if (!date) {
        return '—'
    }

    const parsedDate =
        new Date(date)

    if (
        Number.isNaN(
            parsedDate.getTime()
        )
    ) {
        return date
    }

    return parsedDate.toLocaleDateString(
        'en-LK',
        {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        }
    )

}


// =========================================================
// FORMAT CURRENCY
// =========================================================

const formatCurrency = (
    value: number
) => {

    return `LKR ${Number(
        value || 0
    ).toLocaleString(
        'en-LK',
        {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        }
    )}`

}


// =========================================================
// INITIALS
// =========================================================

const getInitials = (
    name: string
) => {

    if (!name) {
        return 'DS'
    }

    return name
        .split(' ')
        .map(word =>
            word.charAt(0)
        )
        .slice(0, 2)
        .join('')
        .toUpperCase()

}


// =========================================================
// DELIVERY STATUS STYLE
// =========================================================

const getDeliveryStatusClass = (
    status: string
) => {

    switch (status) {

        case 'Pending':
            return 'bg-amber-50 border-amber-200 text-amber-700'

        case 'Order Confirmed':
        case 'Assigned':
            return 'bg-blue-50 border-blue-200 text-blue-700'

        case 'Picked & Packed':
        case 'Picked Up':
            return 'bg-indigo-50 border-indigo-200 text-indigo-700'

        case 'In Transit':
            return 'bg-purple-50 border-purple-200 text-purple-700'

        case 'Out for Delivery':
            return 'bg-cyan-50 border-cyan-200 text-cyan-700'

        case 'Delivered':
            return 'bg-emerald-50 border-emerald-200 text-emerald-700'

        case 'Failed':
            return 'bg-red-50 border-red-200 text-red-700'

        case 'Returned':
            return 'bg-slate-100 border-slate-200 text-slate-700'

        default:
            return 'bg-slate-50 border-slate-200 text-slate-600'

    }

}


// =========================================================
// PAYMENT STYLE
// =========================================================

const getPaymentStatusClass = (
    status: string
) => {

    switch (status) {

        case 'Paid':

            return `
        bg-emerald-50
        border-emerald-200
        text-emerald-600
      `

        case 'Pending':

            return `
        bg-amber-50
        border-amber-200
        text-amber-600
      `

        case 'Failed':

            return `
        bg-red-50
        border-red-200
        text-red-600
      `

        default:

            return `
        bg-slate-50
        border-slate-200
        text-slate-600
      `

    }

}


// =========================================================
// OPEN DETAILS
// =========================================================

const openDetails = (
    delivery: Delivery
) => {

    selectedDelivery.value =
        delivery

    showDetailsModal.value =
        true

}


// =========================================================
// CLOSE DETAILS
// =========================================================

const closeDetails = () => {

    showDetailsModal.value =
        false

}


// =========================================================
// OPEN ASSIGN
// =========================================================

const openAssignModal = (
    delivery: Delivery
) => {

    selectedDelivery.value =
        delivery

    showAssignModal.value =
        true

}


// =========================================================
// ASSIGN STAFF
// =========================================================

const assignStaff = async (
    staffName: string
) => {

    if (
        !selectedDelivery.value ||
        !staffName
    ) {
        return
    }

    const delivery =
        selectedDelivery.value

    const previousStaff =
        delivery.staffName

    const previousStatus =
        delivery.status

    delivery.staffName =
        staffName

    delivery.status =
        'Assigned'

    try {

        const { data } = await api.put(
            `/deliveries/${delivery.id}/assign`,
            null,
            {
                params: {
                    staffName
                }
            }
        )

        // use what the server actually saved instead of the optimistic guess
        delivery.staffName = data?.deliveryStaffName ?? staffName
        delivery.staffId = data?.deliveryStaffId ?? delivery.staffId
        delivery.status = normalizeStatus(data?.status)
        delivery.id = data?.deliveryId ?? delivery.id
        delivery.deliveryId = data?.deliveryId ?? delivery.deliveryId

        showAssignModal.value =
            false

    } catch (err: any) {

        delivery.staffName =
            previousStaff

        delivery.status =
            previousStatus

        loadError.value =
            err?.response?.data?.message ??
            'Failed to assign delivery staff.'
        setTimeout(() => { loadError.value = '' }, 6000)

        console.error(
            'Failed to assign delivery staff:',
            err
        )

    }

}


// =========================================================
// UPDATE STATUS
// =========================================================

const successMessage = ref('')

const updateDeliveryStatus = async (
    delivery: Delivery,
    status: string
) => {

    const previousStatus =
        delivery.status

    delivery.status =
        status

    try {

        const { data } = await api.put(
            `/deliveries/${delivery.id}/status`,
            null,
            {
                params: {
                    status
                }
            }
        )

        // a "DEL-<orderId>" placeholder row gets a real id after the first save
        if (data?.deliveryId) {
            delivery.id = data.deliveryId
            delivery.deliveryId = data.deliveryId
        }
        delivery.status = normalizeStatus(data?.status ?? status)
        if (data?.paymentStatus) {
            delivery.paymentStatus = normalizeStatus(data.paymentStatus)
        }

        // Show success message
        successMessage.value = `Delivery updated to "${status}" successfully!`
        setTimeout(() => { successMessage.value = '' }, 5000)

    } catch (err: any) {

        delivery.status =
            previousStatus

        loadError.value =
            err?.response?.data?.message ??
            'Failed to update delivery status.'
        setTimeout(() => { loadError.value = '' }, 6000)

        console.error(
            'Failed to update delivery status:',
            err
        )

    }

}


// =========================================================
// DELETE
// =========================================================

const openDeleteModal = (
    delivery: Delivery
) => {

    selectedDelivery.value =
        delivery

    showDeleteModal.value =
        true

}


const deleteDelivery = async () => {

    if (
        !selectedDelivery.value
    ) {
        return
    }

    const delivery =
        selectedDelivery.value

    try {

        await api.delete(
            `/deliveries/${delivery.id}`
        )

        deliveries.value =
            deliveries.value.filter(
                item =>
                    item.id !== delivery.id
            )

        if (
            currentPage.value >
            totalPages.value
        ) {
            currentPage.value =
                totalPages.value
        }

    } catch (err) {

        console.error(
            'Failed to delete delivery:',
            err
        )

    } finally {

        selectedDelivery.value =
            null

        showDeleteModal.value =
            false

    }

}


// =========================================================
// UPDATE EXPECTED DATE
// =========================================================

const updateExpectedDate = async (
    delivery: Delivery,
    newDate: string
) => {

    if (!newDate || !delivery.id) return

    const previous = delivery.expectedDate

    isSavingDate.value = true

    // Optimistic update
    delivery.expectedDate = newDate

    try {

        const { data } = await api.put(
            `/deliveries/${delivery.id}/expected-date`,
            null,
            { params: { expectedDate: newDate } }
        )

        delivery.expectedDate =
            data?.expectedDate ??
            newDate

        successMessage.value = `Expected delivery date updated to ${formatDate(delivery.expectedDate)}.`
        setTimeout(() => { successMessage.value = '' }, 5000)

    } catch (err: any) {

        delivery.expectedDate = previous

        loadError.value =
            err?.response?.data?.message ??
            'Failed to update expected date.'
        setTimeout(() => { loadError.value = '' }, 6000)

        console.error('Failed to update expected date:', err)

    } finally {

        isSavingDate.value = false

    }

}


// Helper: convert a date string to YYYY-MM-DD for <input type="date">
const toInputDate = (dateStr: string): string => {
    if (!dateStr) return ''
    try {
        const d = new Date(dateStr)
        if (isNaN(d.getTime())) return ''
        return d.toISOString().split('T')[0]
    } catch {
        return ''
    }
}


// Min allowed date for the date picker = today
const todayISO = new Date().toISOString().split('T')[0]

</script>


<template>

    <div class="min-h-screen bg-slate-100">

        <!-- =====================================================
         SIDEBAR
         ===================================================== -->

        <AdminSidebar />


        <!-- =====================================================
         MAIN
         ===================================================== -->

        <main class="ml-64 min-h-screen">

            <div class="relative min-h-screen overflow-hidden bg-slate-100 text-slate-900">

                <!-- BACKGROUND -->

                <div class="fixed inset-0 pointer-events-none bg-gradient-to-br from-white via-slate-50 to-blue-50/70">
                </div>

                <div
                    class="fixed -top-40 -right-40 w-[500px] h-[500px] rounded-full bg-cyan-400/10 blur-3xl pointer-events-none">
                </div>

                <div
                    class="fixed -bottom-40 -left-40 w-[500px] h-[500px] rounded-full bg-blue-500/10 blur-3xl pointer-events-none">
                </div>


                <div class="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">


                    <!-- =================================================
               HEADER
               ================================================= -->

                    <div class="mb-8 flex flex-col lg:flex-row lg:items-center lg:justify-between gap-5">

                        <div>

                            <div class="flex items-center gap-3 flex-wrap">

                                <span
                                    class="px-2.5 py-1 rounded-md bg-blue-50 border border-blue-200 text-blue-600 text-[10px] font-bold uppercase tracking-widest">
                                    Admin Panel
                                </span>

                                <span class="text-xs text-slate-400 font-semibold">
                                    / Delivery Management
                                </span>

                            </div>


                            <h1 class="mt-3 text-3xl sm:text-4xl font-black text-slate-950 tracking-tight">
                                Delivery Management
                            </h1>


                            <p class="mt-2 text-sm text-slate-500">
                                Manage delivery assignments, delivery staff,
                                shipment progress, and customer deliveries.
                            </p>

                        </div>


                        <div
                            class="flex items-center gap-2 px-4 py-3 rounded-xl bg-white/70 backdrop-blur-xl border border-white/90 shadow-lg shadow-slate-200/20">

                            <span class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>

                            <span class="text-xs font-bold text-emerald-600">
                                Delivery System Online
                            </span>

                        </div>

                    </div>


                    <!-- =================================================
               STATISTICS
               ================================================= -->

                    <!-- Success Alert -->
                    <div v-if="successMessage"
                        class="mb-5 p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-bold flex items-center justify-between">
                        <div class="flex items-center gap-2">
                            <span>✓</span>
                            <span>{{ successMessage }}</span>
                        </div>
                        <button @click="successMessage = ''"
                            class="text-emerald-600 hover:text-emerald-900 font-bold text-sm">×</button>
                    </div>

                    <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-5 gap-5 mb-8">


                        <!-- TOTAL -->

                        <div
                            class="bg-white/65 backdrop-blur-2xl border border-white/90 rounded-2xl p-5 shadow-xl shadow-slate-300/15">

                            <div class="flex justify-between items-start">

                                <div>

                                    <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                                        Total Deliveries
                                    </p>

                                    <p class="mt-2 text-3xl font-black text-slate-950">
                                        {{ totalDeliveries }}
                                    </p>

                                </div>


                                <div
                                    class="w-10 h-10 rounded-xl bg-blue-50 border border-blue-100 flex items-center justify-center">

                                    <svg class="w-5 h-5 text-blue-600" fill="none" stroke="currentColor"
                                        viewBox="0 0 24 24">

                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                            d="M3 7h11v10H3zM14 10h4l3 3v4h-7zM6 20a2 2 0 100-4 2 2 0 000 4zm12 0a2 2 0 100-4 2 2 0 000 4z" />

                                    </svg>

                                </div>

                            </div>

                            <p class="mt-3 text-xs text-slate-400">
                                All delivery records
                            </p>

                        </div>


                        <!-- PENDING -->

                        <div
                            class="bg-white/65 backdrop-blur-2xl border border-white/90 rounded-2xl p-5 shadow-xl shadow-slate-300/15">

                            <div class="flex justify-between items-start">

                                <div>

                                    <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                                        Pending
                                    </p>

                                    <p class="mt-2 text-3xl font-black text-slate-950">
                                        {{ pendingDeliveries }}
                                    </p>

                                </div>


                                <div
                                    class="w-10 h-10 rounded-xl bg-amber-50 border border-amber-100 flex items-center justify-center">

                                    <svg class="w-5 h-5 text-amber-600" fill="none" stroke="currentColor"
                                        viewBox="0 0 24 24">

                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                            d="M12 8v4l3 2m6-2a9 9 0 11-18 0 9 9 0 0118 0z" />

                                    </svg>

                                </div>

                            </div>

                            <p class="mt-3 text-xs text-amber-600">
                                Awaiting assignment
                            </p>

                        </div>


                        <!-- OUT FOR DELIVERY -->

                        <div
                            class="bg-white/65 backdrop-blur-2xl border border-white/90 rounded-2xl p-5 shadow-xl shadow-slate-300/15">

                            <div class="flex justify-between items-start">

                                <div>

                                    <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                                        Out for Delivery
                                    </p>

                                    <p class="mt-2 text-3xl font-black text-slate-950">
                                        {{ outForDelivery }}
                                    </p>

                                </div>


                                <div
                                    class="w-10 h-10 rounded-xl bg-violet-50 border border-violet-100 flex items-center justify-center">

                                    <svg class="w-5 h-5 text-violet-600" fill="none" stroke="currentColor"
                                        viewBox="0 0 24 24">

                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                            d="M3 11l3-7h12l3 7M5 11v8h14v-8M8 15h8" />

                                    </svg>

                                </div>

                            </div>

                            <p class="mt-3 text-xs text-violet-600">
                                Currently in transit
                            </p>

                        </div>


                        <!-- DELIVERED -->

                        <div
                            class="bg-white/65 backdrop-blur-2xl border border-white/90 rounded-2xl p-5 shadow-xl shadow-slate-300/15">

                            <div class="flex justify-between items-start">

                                <div>

                                    <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                                        Delivered
                                    </p>

                                    <p class="mt-2 text-3xl font-black text-slate-950">
                                        {{ deliveredCount }}
                                    </p>

                                </div>


                                <div
                                    class="w-10 h-10 rounded-xl bg-emerald-50 border border-emerald-100 flex items-center justify-center">

                                    <svg class="w-5 h-5 text-emerald-600" fill="none" stroke="currentColor"
                                        viewBox="0 0 24 24">

                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                            d="M5 13l4 4L19 7" />

                                    </svg>

                                </div>

                            </div>

                            <p class="mt-3 text-xs text-emerald-600">
                                Successfully completed
                            </p>

                        </div>


                        <!-- FAILED -->

                        <div
                            class="bg-gradient-to-br from-slate-800 to-slate-950 rounded-2xl p-5 shadow-xl shadow-slate-900/20 text-white">

                            <div class="flex justify-between items-start">

                                <div>

                                    <p class="text-[10px] font-bold uppercase tracking-wider text-white/60">
                                        Failed / Returned
                                    </p>

                                    <p class="mt-2 text-3xl font-black">
                                        {{ failedDeliveries }}
                                    </p>

                                </div>


                                <div
                                    class="w-10 h-10 rounded-xl bg-white/10 border border-white/10 flex items-center justify-center">

                                    <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor"
                                        viewBox="0 0 24 24">

                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                            d="M6 18L18 6M6 6l12 12" />

                                    </svg>

                                </div>

                            </div>

                            <p class="mt-3 text-xs text-white/60">
                                Requires attention
                            </p>

                        </div>

                    </div>


                    <!-- =================================================
               DELIVERY TABLE
               ================================================= -->

                    <section
                        class="bg-white/65 backdrop-blur-2xl border border-white/90 rounded-3xl shadow-xl shadow-slate-300/20 overflow-hidden">


                        <!-- TOOLBAR -->

                        <div class="p-5 sm:p-6 border-b border-slate-200/80">

                            <!-- Professional Delivery Workflow Banner -->
                            <div class="mb-5 overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-sm">
                                <!-- Header -->
                                <div
                                    class="flex flex-col gap-4 border-b border-slate-200 bg-slate-50/70 px-5 py-4 lg:flex-row lg:items-center lg:justify-between">
                                    <div class="flex items-center gap-3">
                                        <!-- Icon -->

                                        <div>
                                            <div class="flex flex-wrap items-center gap-2">
                                                <h3
                                                    class="text-xs font-black uppercase tracking-[0.14em] text-slate-900">
                                                    Delivery Fulfillment Workflow
                                                </h3>

                                                <span class="rounded-full border border-cyan-100 bg-cyan-50 px-2 py-0.5
                   text-[9px] font-bold text-cyan-700">
                                                    DELIVERY MANAGEMENT
                                                </span>
                                            </div>

                                            <p class="mt-1 text-[11px] leading-relaxed text-slate-500">
                                                Shipments are received from Order Management after staff assignment
                                                and completed through the final delivery stages.
                                            </p>
                                        </div>
                                    </div>



                                </div>

                                <!-- Workflow -->
                                <div class="px-5 py-5">
                                    <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">

                                        <!-- Previous Module (Admin only) -->
                                        <router-link v-if="isAdmin" to="/admin/orders" class="group flex min-w-0 items-center rounded-xl border border-slate-200
               bg-white px-3 py-2.5 transition-all duration-200
               hover:border-blue-200 hover:bg-blue-50/60">
                                            <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl
                 bg-blue-50 text-blue-600">
                                                <svg class="h-4 w-4" fill="none" stroke="currentColor"
                                                    viewBox="0 0 24 24">
                                                    <path stroke-linecap="round" stroke-linejoin="round"
                                                        stroke-width="1.8" d="M19 12H5m7 7-7-7 7-7" />
                                                </svg>
                                            </div>

                                            <div class="ml-3 min-w-0">
                                                <p class="text-[9px] font-bold uppercase tracking-wide text-slate-400">
                                                    Previous stage
                                                </p>

                                                <p class="mt-0.5 truncate text-xs font-black text-slate-800">
                                                    Order Management
                                                </p>

                                                <p class="mt-0.5 text-[9px] text-slate-400">
                                                    Steps 01–03 completed
                                                </p>
                                            </div>
                                        </router-link>

                                        <!-- Connector -->
                                        <div class="hidden h-px flex-1 bg-slate-200 lg:block"></div>

                                        <!-- Step 1 -->
                                        <div class="flex min-w-0 flex-1 items-center rounded-xl
               border border-cyan-100 bg-cyan-50/60 px-3 py-2.5">
                                            <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl
                 bg-cyan-600 text-xs font-black text-white shadow-sm">
                                                01
                                            </div>

                                            <div class="ml-3 min-w-0">
                                                <p class="text-[9px] font-black uppercase tracking-wide text-cyan-600">
                                                    Step 01
                                                </p>

                                                <p class="mt-0.5 truncate text-xs font-bold text-slate-800">
                                                    Out for Delivery
                                                </p>

                                                <p class="mt-0.5 text-[9px] text-slate-400">
                                                    Shipment is with the customer route
                                                </p>
                                            </div>
                                        </div>

                                        <!-- Connector -->
                                        <div class="hidden h-px flex-1 bg-slate-200 lg:block"></div>

                                        <!-- Step 2 -->
                                        <div class="flex min-w-0 flex-1 items-center rounded-xl
               border border-emerald-100 bg-emerald-50/60 px-3 py-2.5">
                                            <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl
                 bg-emerald-600 text-xs font-black text-white shadow-sm">
                                                02
                                            </div>

                                            <div class="ml-3 min-w-0">
                                                <p
                                                    class="text-[9px] font-black uppercase tracking-wide text-emerald-600">
                                                    Step 02
                                                </p>

                                                <p class="mt-0.5 truncate text-xs font-bold text-slate-800">
                                                    Delivered
                                                </p>

                                                <p class="mt-0.5 text-[9px] text-slate-400">
                                                    Order successfully completed
                                                </p>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <!-- Footer -->
                                <div class="flex items-center gap-2 border-t border-slate-100 bg-slate-50/50
           px-5 py-2.5">
                                    <svg class="h-3.5 w-3.5 shrink-0 text-slate-400" fill="none" stroke="currentColor"
                                        viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                                            d="M12 8v4l2.5 1.5M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
                                    </svg>

                                    <p class="text-[10px] font-medium text-slate-400">
                                        Delivery Management owns the final shipment stages:
                                        <span class="font-bold text-cyan-700">Out for Delivery</span>
                                        →
                                        <span class="font-bold text-emerald-700">Delivered</span>.
                                    </p>
                                </div>
                            </div>
                            <div class="flex flex-col xl:flex-row xl:items-center xl:justify-between gap-4">

                                <div>

                                    <h2 class="text-lg font-black text-slate-950">
                                        Delivery Operations
                                    </h2>

                                    <p class="mt-1 text-xs text-slate-400">
                                        {{ filteredDeliveries.length }}
                                        deliveries found
                                    </p>

                                </div>


                                <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-3">


                                    <!-- SEARCH -->

                                    <div class="relative">

                                        <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400"
                                            fill="none" stroke="currentColor" viewBox="0 0 24 24">

                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                                d="M21 21l-4.35-4.35m2.35-5.65a8 8 0 11-16 0 8 8 0 0116 0z" />

                                        </svg>


                                        <input v-model="searchQuery" @input="resetPage" type="text"
                                            placeholder="Search delivery..."
                                            class="w-full bg-white/80 border border-slate-200 rounded-xl pl-9 pr-4 py-2.5 text-xs text-slate-900 placeholder-slate-400 focus:outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10" />

                                    </div>


                                    <!-- STATUS -->

                                    <select v-model="selectedStatus" @change="resetPage"
                                        class="bg-white/80 border border-slate-200 rounded-xl px-4 py-2.5 text-xs text-slate-700 focus:outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10">

                                        <option value="All">
                                            All Delivery Status
                                        </option>

                                        <option v-for="status in statusOptions" :key="status" :value="status">
                                            {{ status }}
                                        </option>

                                    </select>


                                    <!-- STAFF -->

                                    <select v-model="selectedStaff" @change="resetPage"
                                        class="bg-white/80 border border-slate-200 rounded-xl px-4 py-2.5 text-xs text-slate-700 focus:outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10">

                                        <option value="All">
                                            All Delivery Staff
                                        </option>

                                        <option v-for="staff in staffOptions" :key="staff" :value="staff">
                                            {{ staff }}
                                        </option>

                                    </select>

                                </div>

                            </div>


                            <!-- CLEAR -->

                            <div v-if="
                                searchQuery ||
                                selectedStatus !== 'All' ||
                                selectedStaff !== 'All'
                            " class="mt-4">

                                <button @click="clearFilters"
                                    class="px-3 py-2 rounded-lg bg-slate-100 border border-slate-200 text-xs font-bold text-slate-600 hover:bg-slate-200 transition">
                                    Clear All Filters
                                </button>

                            </div>

                        </div>


                        <!-- LOADING -->

                        <div v-if="isLoading" class="px-5 py-16 text-center">

                            <div
                                class="w-10 h-10 mx-auto rounded-full border-4 border-slate-200 border-t-blue-600 animate-spin">
                            </div>

                            <p class="mt-4 text-sm font-bold text-slate-600">
                                Loading deliveries...
                            </p>

                        </div>


                        <!-- ERROR -->

                        <div v-else-if="loadError"
                            class="m-5 p-4 rounded-xl bg-red-50 border border-red-200 text-sm text-red-600">

                            {{ loadError }}

                        </div>


                        <!-- TABLE -->

                        <div v-else class="overflow-x-auto">

                            <table class="w-full text-left">

                                <thead>

                                    <tr class="border-b border-slate-200/80 bg-slate-50/60">

                                        <th
                                            class="px-5 py-4 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                                            Delivery
                                        </th>

                                        <th
                                            class="px-5 py-4 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                                            Customer
                                        </th>

                                        <th
                                            class="px-5 py-4 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                                            Delivery Staff
                                        </th>

                                        <th
                                            class="px-5 py-4 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                                            Destination
                                        </th>

                                        <th
                                            class="px-5 py-4 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                                            Expected
                                        </th>

                                        <th
                                            class="px-5 py-4 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                                            Status
                                        </th>

                                        <th
                                            class="px-5 py-4 text-right text-[10px] font-bold uppercase tracking-wider text-slate-400">
                                            Actions
                                        </th>

                                    </tr>

                                </thead>


                                <tbody class="divide-y divide-slate-200/60">


                                    <!-- ROW -->

                                    <tr v-for="delivery in paginatedDeliveries" :key="delivery.id"
                                        class="hover:bg-blue-50/30 transition-colors">


                                        <!-- DELIVERY -->

                                        <td class="px-5 py-4">

                                            <p class="text-sm font-black text-blue-600">
                                                {{ delivery.deliveryId }}
                                            </p>

                                            <p class="mt-1 text-[10px] font-semibold text-slate-400">
                                                Order {{ delivery.orderId }}
                                            </p>

                                            <p class="mt-1 text-[10px] text-slate-400">
                                                {{ formatCurrency(delivery.amount) }}
                                            </p>

                                        </td>


                                        <!-- CUSTOMER -->

                                        <td class="px-5 py-4">

                                            <div class="flex items-center gap-3 min-w-[190px]">

                                                <img v-if="delivery.customerImage" :src="delivery.customerImage"
                                                    :alt="delivery.customerName"
                                                    class="w-10 h-10 rounded-full object-cover shrink-0" />
                                                <div v-else
                                                    class="w-10 h-10 rounded-full bg-gradient-to-br from-blue-500 to-cyan-400 text-white flex items-center justify-center text-xs font-black shrink-0">
                                                    {{
                                                        getInitials(
                                                            delivery.customerName
                                                        )
                                                    }}
                                                </div>

                                                <div>

                                                    <p class="text-xs font-bold text-slate-900">
                                                        {{ delivery.customerName }}
                                                    </p>

                                                    <p class="mt-1 text-[10px] text-slate-400">
                                                        {{ delivery.customerId }} •
                                                        {{
                                                            delivery.customerPhone ||
                                                            'No phone'
                                                        }}
                                                    </p>

                                                </div>

                                            </div>

                                        </td>


                                        <!-- STAFF -->

                                        <td class="px-5 py-4">

                                            <div class="flex items-center gap-2">

                                                <img v-if="getStaffImage(delivery.staffName)" :src="getStaffImage(delivery.staffName)" :alt="delivery.staffName" class="w-8 h-8 rounded-lg object-cover border border-slate-200 shrink-0" />
                                                <img v-else
                                                    :src="`https://ui-avatars.com/api/?name=${encodeURIComponent(delivery.staffName || 'Staff')}&background=f1f5f9&color=475569`"
                                                    :alt="delivery.staffName"
                                                    class="w-8 h-8 rounded-lg object-cover border border-slate-200 shrink-0" 
                                                />

                                                <div>

                                                    <p class="text-xs font-bold text-slate-700">
                                                        {{ delivery.staffName }}
                                                    </p>

                                                    <button @click="openAssignModal(delivery)"
                                                        class="mt-1 text-[10px] font-bold text-blue-600 hover:text-blue-700">
                                                        {{
                                                            delivery.staffId
                                                                ? 'Reassign'
                                                                : 'Assign Staff'
                                                        }}
                                                    </button>

                                                </div>

                                            </div>

                                        </td>


                                        <!-- DESTINATION -->

                                        <td class="px-5 py-4">

                                            <div class="max-w-[180px]">

                                                <p class="text-xs font-bold text-slate-700 truncate">
                                                    {{ delivery.city || 'Sri Lanka' }}
                                                </p>

                                                <p class="mt-1 text-[10px] text-slate-400 truncate">
                                                    {{ delivery.address }}
                                                </p>

                                            </div>

                                        </td>


                                        <!-- DATE -->

                                        <td class="px-5 py-4">

                                            <span class="text-xs font-semibold text-slate-600">
                                                {{
                                                    formatDate(
                                                        delivery.expectedDate
                                                    )
                                                }}
                                            </span>

                                        </td>


                                        <!-- STATUS -->

                                        <td class="px-5 py-4">

                                            <span :class="[
                                                'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full border text-[10px] font-bold',
                                                getDeliveryStatusClass(
                                                    delivery.status
                                                )
                                            ]">

                                                <span class="w-1.5 h-1.5 rounded-full bg-current"></span>

                                                {{ delivery.status }}

                                            </span>

                                        </td>


                                        <!-- ACTIONS -->

                                        <td class="px-5 py-4">

                                            <div class="flex items-center justify-end gap-2">


                                                <!-- STEP ADVANCE — Only for In Transit & Out for Delivery (handed over from Order Mgmt) -->

                                                <button v-if="getNextDeliveryStep(delivery.status)"
                                                    @click="updateDeliveryStatus(delivery, getNextDeliveryStep(delivery.status)!.next)"
                                                    :title="getNextDeliveryStep(delivery.status)!.label" :class="[
                                                        'px-2.5 py-1.5 rounded-lg text-[10px] font-bold shadow-sm transition flex items-center gap-1 shrink-0',
                                                        getNextDeliveryStep(delivery.status)!.class
                                                    ]">
                                                    <span>{{ getNextDeliveryStep(delivery.status)!.label }}</span>
                                                </button>

                                                <!-- Still in Order Management -->
                                                <template v-else-if="isStillInOrderManagement(delivery.status)">
                                                    <router-link v-if="isAdmin"
                                                        to="/admin/orders"
                                                        class="inline-flex items-center gap-1 px-2 py-1 rounded-lg bg-amber-50 border border-amber-200 text-[9px] font-bold text-amber-700 hover:bg-amber-100 transition shrink-0"
                                                        title="Process in Order Management first">
                                                        📦 Order Mgmt →
                                                    </router-link>
                                                    <span v-else
                                                        class="inline-flex items-center gap-1 px-2 py-1 rounded-lg bg-amber-50 border border-amber-200 text-[9px] font-bold text-amber-700 shrink-0"
                                                        title="Order is being prepared in Order Management">
                                                        📦 In Prep
                                                    </span>
                                                </template>

                                                <!-- Delivered badge (no further action) -->
                                                <span v-else-if="delivery.status === 'Delivered'"
                                                    class="px-2.5 py-1 rounded-lg bg-emerald-50 border border-emerald-200 text-[10px] font-bold text-emerald-700 shrink-0">
                                                    ✓ Delivered
                                                </span>

                                                <!-- VIEW -->

                                                <button @click="openDetails(delivery)" title="View delivery"
                                                    class="w-9 h-9 rounded-lg bg-white border border-slate-200 text-slate-500 hover:text-blue-600 hover:border-blue-300 hover:bg-blue-50 transition flex items-center justify-center">

                                                    <svg class="w-4 h-4" fill="none" stroke="currentColor"
                                                        viewBox="0 0 24 24">

                                                        <path stroke-linecap="round" stroke-linejoin="round"
                                                            stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />

                                                        <path stroke-linecap="round" stroke-linejoin="round"
                                                            stroke-width="2"
                                                            d="M2.458 12C3.732 7.943 7.523 5 12 5c4.477 0 8.268 2.943 9.542 7-1.274 4.057-5.065 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />

                                                    </svg>

                                                </button>


                                                <!-- ASSIGN -->

                                                <button @click="openAssignModal(delivery)" title="Assign staff"
                                                    class="w-9 h-9 rounded-lg bg-white border border-slate-200 text-slate-500 hover:text-blue-600 hover:border-blue-300 hover:bg-blue-50 transition flex items-center justify-center">

                                                    <svg class="w-4 h-4" fill="none" stroke="currentColor"
                                                        viewBox="0 0 24 24">

                                                        <path stroke-linecap="round" stroke-linejoin="round"
                                                            stroke-width="2"
                                                            d="M16 21v-2a4 4 0 00-4-4H6a4 4 0 00-4 4v2M9 11a4 4 0 100-8 4 4 0 000 8zM22 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75" />

                                                    </svg>

                                                </button>


                                                <!-- DELETE -->

                                                <button @click="openDeleteModal(delivery)" title="Delete delivery"
                                                    class="w-9 h-9 rounded-lg bg-white border border-slate-200 text-slate-500 hover:text-red-600 hover:border-red-300 hover:bg-red-50 transition flex items-center justify-center">

                                                    <svg class="w-4 h-4" fill="none" stroke="currentColor"
                                                        viewBox="0 0 24 24">

                                                        <path stroke-linecap="round" stroke-linejoin="round"
                                                            stroke-width="2"
                                                            d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6V9m-7-2h10" />

                                                        <path stroke-linecap="round" stroke-linejoin="round"
                                                            stroke-width="2" d="M9 7V4a1 1 0 011-1h4a1 1 0 011 1v3" />

                                                    </svg>

                                                </button>

                                            </div>

                                        </td>

                                    </tr>


                                    <!-- EMPTY -->

                                    <tr v-if="
                                        paginatedDeliveries.length === 0
                                    ">

                                        <td colspan="7" class="px-5 py-16 text-center">

                                            <div
                                                class="w-14 h-14 mx-auto rounded-2xl bg-slate-100 border border-slate-200 flex items-center justify-center">

                                                <svg class="w-6 h-6 text-slate-400" fill="none" stroke="currentColor"
                                                    viewBox="0 0 24 24">

                                                    <path stroke-linecap="round" stroke-linejoin="round"
                                                        stroke-width="2" d="M3 7h11v10H3zM14 10h4l3 3v4h-7z" />

                                                </svg>

                                            </div>

                                            <p class="mt-4 text-sm font-bold text-slate-700">
                                                No deliveries found
                                            </p>

                                            <p class="mt-1 text-xs text-slate-400">
                                                Try changing your search or filters.
                                            </p>

                                        </td>

                                    </tr>

                                </tbody>

                            </table>

                        </div>


                        <!-- =================================================
                 PAGINATION
                 ================================================= -->

                        <div
                            class="px-5 sm:px-6 py-4 border-t border-slate-200/80 flex flex-col sm:flex-row items-center justify-between gap-4">

                            <p class="text-xs text-slate-400">

                                Showing

                                <span class="font-bold text-slate-600">
                                    {{
                                        filteredDeliveries.length === 0
                                            ? 0
                                            : (currentPage - 1) *
                                            itemsPerPage +
                                            1
                                    }}
                                </span>

                                -

                                <span class="font-bold text-slate-600">
                                    {{
                                        Math.min(
                                            currentPage *
                                            itemsPerPage,
                                            filteredDeliveries.length
                                        )
                                    }}
                                </span>

                                of

                                <span class="font-bold text-slate-600">
                                    {{ filteredDeliveries.length }}
                                </span>

                                deliveries

                            </p>


                            <div class="flex items-center gap-2">

                                <button @click="
                                    goToPage(
                                        currentPage - 1
                                    )
                                    " :disabled="currentPage === 1
                                        "
                                    class="w-9 h-9 rounded-lg bg-white border border-slate-200 text-slate-500 disabled:opacity-40 disabled:cursor-not-allowed hover:border-blue-300 hover:text-blue-600 transition">
                                    ‹
                                </button>


                                <button v-for="page in totalPages" :key="page" @click="goToPage(page)" :class="[
                                    'w-9 h-9 rounded-lg text-xs font-bold transition',

                                    currentPage === page
                                        ? 'bg-blue-600 text-white shadow-md shadow-blue-500/20'
                                        : 'bg-white border border-slate-200 text-slate-500 hover:border-blue-300 hover:text-blue-600'
                                ]">
                                    {{ page }}
                                </button>


                                <button @click="
                                    goToPage(
                                        currentPage + 1
                                    )
                                    " :disabled="currentPage === totalPages
                                        "
                                    class="w-9 h-9 rounded-lg bg-white border border-slate-200 text-slate-500 disabled:opacity-40 disabled:cursor-not-allowed hover:border-blue-300 hover:text-blue-600 transition">
                                    ›
                                </button>

                            </div>

                        </div>

                    </section>

                </div>

            </div>

        </main>


        <!-- =====================================================
         DELIVERY DETAILS MODAL
         ===================================================== -->

        <Transition name="modal">

            <div v-if="
                showDetailsModal &&
                selectedDelivery
            " class="fixed inset-0 z-[100] flex items-center justify-center p-4">

                <div class="absolute inset-0 bg-slate-950/30 backdrop-blur-sm" @click="closeDetails"></div>


                <div
                    class="relative w-full max-w-3xl max-h-[90vh] overflow-y-auto bg-white/95 backdrop-blur-2xl border border-white rounded-3xl shadow-2xl shadow-slate-900/20">


                    <!-- HEADER -->

                    <div
                        class="sticky top-0 z-10 px-6 py-5 bg-white/90 backdrop-blur-xl border-b border-slate-200/80 flex items-center justify-between">

                        <div>

                            <p class="text-[10px] font-bold uppercase tracking-widest text-blue-600">
                                Delivery Details
                            </p>

                            <h2 class="mt-1 text-xl font-black text-slate-950">
                                {{ selectedDelivery.deliveryId }}
                            </h2>

                        </div>


                        <button @click="closeDetails"
                            class="w-9 h-9 rounded-xl bg-slate-100 border border-slate-200 text-slate-500 hover:bg-red-50 hover:border-red-200 hover:text-red-500 transition">
                            ×
                        </button>

                    </div>


                    <div class="p-6 space-y-6">


                        <!-- CUSTOMER + STATUS -->

                        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">


                            <!-- CUSTOMER -->

                            <div class="p-5 rounded-2xl bg-slate-50/80 border border-slate-200">

                                <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                                    Customer
                                </p>


                                <div class="mt-3 flex items-center gap-3">

                                    <img v-if="selectedDelivery.customerImage" :src="selectedDelivery.customerImage"
                                        :alt="selectedDelivery.customerName"
                                        class="w-11 h-11 rounded-full object-cover" />
                                    <div v-else
                                        class="w-11 h-11 rounded-full bg-gradient-to-br from-blue-500 to-cyan-400 text-white flex items-center justify-center text-xs font-black">
                                        {{
                                            getInitials(
                                                selectedDelivery.customerName
                                            )
                                        }}
                                    </div>


                                    <div>

                                        <p class="text-sm font-bold text-slate-900">
                                            {{
                                                selectedDelivery.customerName
                                            }}
                                        </p>

                                        <p class="text-xs text-slate-400 mt-1">
                                            {{ selectedDelivery.customerId }} •
                                            {{
                                                selectedDelivery.customerEmail ||
                                                'No email available'
                                            }}
                                        </p>

                                        <p class="text-xs text-slate-400">
                                            {{
                                                selectedDelivery.customerPhone ||
                                                'No phone available'
                                            }}
                                        </p>

                                    </div>

                                </div>

                            </div>


                            <!-- STATUS -->

                            <div class="p-5 rounded-2xl bg-slate-50/80 border border-slate-200">

                                <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                                    Delivery Status
                                </p>


                                <select :value="selectedDelivery.status
                                    " @change="
                                        updateDeliveryStatus(
                                            selectedDelivery,
                                            (
                                                $event.target as HTMLSelectElement
                                            ).value
                                        )
                                        "
                                    class="mt-3 w-full bg-white border border-slate-200 rounded-xl px-4 py-3 text-sm font-bold text-slate-700 focus:outline-none focus:border-blue-500">

                                    <option v-for="status in statusOptions" :key="status" :value="status">
                                        {{ status }}
                                    </option>

                                </select>

                            </div>

                        </div>


                        <!-- =================================================
                 DELIVERY STAFF
                 ================================================= -->

                        <div class="p-5 rounded-2xl bg-slate-50/80 border border-slate-200">

                            <div class="flex items-center justify-between gap-4">


                                <div>

                                    <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                                        Assigned Delivery Staff
                                    </p>

                                    <div class="mt-2 flex items-center gap-3">
                                        <img v-if="getStaffImage(selectedDelivery.staffName)" :src="getStaffImage(selectedDelivery.staffName)" :alt="selectedDelivery.staffName" class="w-8 h-8 rounded-lg object-cover border border-slate-200 shrink-0" />
                                        <img v-else
                                            :src="`https://ui-avatars.com/api/?name=${encodeURIComponent(selectedDelivery.staffName || 'Staff')}&background=f1f5f9&color=475569`"
                                            :alt="selectedDelivery.staffName"
                                            class="w-8 h-8 rounded-lg object-cover border border-slate-200 shrink-0" 
                                        />
                                        <p class="text-sm font-bold text-slate-800">
                                            {{
                                                selectedDelivery.staffName
                                            }}
                                        </p>
                                    </div>

                                </div>


                                <button @click="openAssignModal(selectedDelivery)"
                                    class="px-4 py-2.5 rounded-xl bg-blue-600 text-white text-xs font-bold hover:bg-blue-700 transition">
                                    Reassign Staff
                                </button>

                            </div>

                        </div>


                        <!-- ADDRESS -->

                        <div class="p-5 bg-slate-50/80 border border-slate-200 rounded-2xl">

                            <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                                Delivery Address
                            </p>

                            <p class="mt-2 text-sm font-semibold text-slate-700">
                                {{ selectedDelivery.address }}
                            </p>

                            <p class="mt-1 text-xs text-slate-400">
                                {{ selectedDelivery.city }}
                                {{ selectedDelivery.postalCode }}
                            </p>

                        </div>


                        <!-- DATES -->

                        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">

                            <div class="p-4 rounded-2xl bg-white border border-slate-200">

                                <p class="text-[10px] uppercase font-bold tracking-wider text-slate-400">
                                    Assigned
                                </p>

                                <p class="mt-2 text-sm font-bold text-slate-700">
                                    {{
                                        formatDate(
                                            selectedDelivery.assignedDate
                                        )
                                    }}
                                </p>

                            </div>


                            <div
                                class="p-4 rounded-2xl bg-white border border-slate-200 relative group overflow-hidden">

                                <p class="text-[10px] uppercase font-bold tracking-wider text-slate-400">
                                    Expected
                                </p>

                                <div class="flex items-center justify-between mt-2">
                                    <p class="text-sm font-bold text-blue-600">
                                        {{ formatDate(selectedDelivery.expectedDate) }}
                                    </p>

                                    <!-- Invisible Date Picker Overlaid -->
                                    <input type="date" :min="todayISO"
                                        :value="toInputDate(selectedDelivery.expectedDate)"
                                        @change="(e) => updateExpectedDate(selectedDelivery!, (e.target as HTMLInputElement).value)"
                                        :disabled="isSavingDate"
                                        class="absolute inset-0 w-full h-full opacity-0 cursor-pointer disabled:cursor-not-allowed z-10"
                                        title="Click to change expected date" />

                                    <svg class="w-4 h-4 text-blue-400 opacity-0 group-hover:opacity-100 transition z-0"
                                        fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                            d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                                    </svg>
                                </div>

                                <div v-if="isSavingDate" class="absolute top-2 right-2 flex space-x-1 z-0">
                                    <span class="w-1.5 h-1.5 bg-blue-400 rounded-full animate-bounce"></span>
                                </div>

                            </div>


                            <div class="p-4 rounded-2xl bg-white border border-slate-200">

                                <p class="text-[10px] uppercase font-bold tracking-wider text-slate-400">
                                    Delivered
                                </p>

                                <p class="mt-2 text-sm font-bold text-emerald-600">
                                    {{
                                        formatDate(
                                            selectedDelivery.deliveredDate
                                        )
                                    }}
                                </p>

                            </div>

                        </div>


                        <!-- PAYMENT -->

                        <div class="grid grid-cols-1 md:grid-cols-2 gap-5">

                            <div class="p-5 bg-slate-50/80 border border-slate-200 rounded-2xl">

                                <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                                    Payment
                                </p>

                                <p class="mt-3 text-sm font-bold text-slate-800">
                                    {{
                                        selectedDelivery.paymentMethod
                                    }}
                                </p>

                                <span :class="[
                                    'inline-flex mt-2 px-2.5 py-1 rounded-full border text-[10px] font-bold',
                                    getPaymentStatusClass(
                                        selectedDelivery.paymentStatus
                                    )
                                ]">
                                    {{
                                        selectedDelivery.paymentStatus
                                    }}
                                </span>

                            </div>


                            <div class="p-5 bg-slate-50/80 border border-slate-200 rounded-2xl">

                                <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                                    Order Amount
                                </p>

                                <p class="mt-3 text-2xl font-black text-blue-600">
                                    {{
                                        formatCurrency(
                                            selectedDelivery.amount
                                        )
                                    }}
                                </p>

                                <p class="mt-1 text-xs text-slate-400">
                                    Order {{ selectedDelivery.orderId }}
                                </p>

                            </div>

                        </div>


                        <!-- CLOSE -->

                        <button @click="closeDetails"
                            class="w-full px-5 py-3 rounded-xl bg-slate-900 text-white text-sm font-bold hover:bg-slate-800 transition">
                            Close Delivery Details
                        </button>

                    </div>

                </div>

            </div>

        </Transition>


        <!-- =====================================================
         ASSIGN STAFF MODAL
         ===================================================== -->

        <Transition name="modal">

            <div v-if="
                showAssignModal &&
                selectedDelivery
            " class="fixed inset-0 z-[110] flex items-center justify-center p-4">

                <div class="absolute inset-0 bg-slate-950/30 backdrop-blur-sm" @click="
                    showAssignModal = false
                    "></div>


                <div
                    class="relative w-full max-w-md bg-white/95 backdrop-blur-2xl border border-white rounded-3xl shadow-2xl p-6">

                    <div>

                        <p class="text-[10px] font-bold uppercase tracking-widest text-blue-600">
                            Delivery Assignment
                        </p>

                        <h2 class="mt-2 text-xl font-black text-slate-950">
                            Assign Delivery Staff
                        </h2>

                        <p class="mt-2 text-sm text-slate-500">
                            Select a delivery staff member for
                            {{ selectedDelivery.deliveryId }}.
                        </p>

                    </div>


                    <div class="mt-6 space-y-2">

                        <button v-for="staff in staffOptions" :key="staff" @click="assignStaff(staff)"
                            class="w-full flex items-center gap-3 p-4 rounded-xl bg-slate-50 border border-slate-200 hover:bg-blue-50 hover:border-blue-300 transition text-left">

                            <img v-if="getStaffImage(staff)" :src="getStaffImage(staff)" :alt="staff" class="w-10 h-10 rounded-xl object-cover shrink-0 shadow-sm" />
                            <img v-else
                                :src="`https://ui-avatars.com/api/?name=${encodeURIComponent(staff || 'Staff')}&background=3b82f6&color=fff`"
                                :alt="staff"
                                class="w-10 h-10 rounded-xl object-cover shrink-0 shadow-sm" 
                            />

                            <div>

                                <p class="text-sm font-bold text-slate-800">
                                    {{ staff }}
                                </p>

                                <p class="text-[10px] text-slate-400">
                                    Delivery Staff
                                </p>

                            </div>

                        </button>


                        <div v-if="staffOptions.length === 0"
                            class="p-4 rounded-xl bg-amber-50 border border-amber-200 text-xs text-amber-700">
                            No delivery staff available.
                        </div>

                    </div>


                    <button @click="
                        showAssignModal = false
                        "
                        class="mt-5 w-full px-5 py-3 rounded-xl bg-slate-100 border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-200 transition">
                        Cancel
                    </button>

                </div>

            </div>

        </Transition>


        <!-- =====================================================
         DELETE MODAL
         ===================================================== -->

        <!-- =====================================================
             DELETE DELIVERY MODAL (ORDER MANAGEMENT DESIGN)
             ===================================================== -->
        <AdminConfirmModal
            v-model:show="showDeleteModal"
            type="danger"
            title="Delete Delivery?"
            message="Are you sure you want to permanently delete delivery record"
            :target="selectedDelivery?.deliveryId"
            description="This delivery record will be removed from the system. This action cannot be undone."
            confirm-text="Delete Delivery"
            cancel-text="Cancel"
            @confirm="deleteDelivery"
        />

    </div>

</template>


<style scoped>
.modal-enter-active,
.modal-leave-active {
    transition: all 0.25s ease;
}


.modal-enter-from,
.modal-leave-to {
    opacity: 0;
}


.modal-enter-active>div:last-child,
.modal-leave-active>div:last-child {
    transition: all 0.25s ease;
}


.modal-enter-from>div:last-child,
.modal-leave-to>div:last-child {
    opacity: 0;
    transform: translateY(10px) scale(0.98);
}
</style>