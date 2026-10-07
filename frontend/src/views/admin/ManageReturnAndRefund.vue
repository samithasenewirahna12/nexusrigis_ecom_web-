<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import axios from 'axios'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminConfirmModal, { type ModalType, type AdminModalState } from '../../components/admin/AdminConfirmModal.vue'

/* =========================================================
   TYPES
   ========================================================= */

interface ReturnRequestItem {
    productId: string | null
    productName: string | null
    quantity: number
    unitPrice: number
}

// Backend statuses: REQUESTED, APPROVED, REJECTED, COMPLETED
type ReturnStatus =
    | 'REQUESTED'
    | 'APPROVED'
    | 'REJECTED'
    | 'COMPLETED'

type RequestType =
    | 'RETURN'
    | 'REFUND'

interface ReturnRequest {
    returnId: string
    orderId: string
    orderDate: string | null
    orderTotal: number | null

    items: ReturnRequestItem[]

    customerId: string | null
    customerName: string | null
    customerEmail: string | null

    reason: string
    description: string | null

    requestType: RequestType
    refundMethod: string | null

    requestDate: string
    status: ReturnStatus

    refundAmount: number | null

    supportStaffId: string | null
    adminNote: string | null

    evidenceImage: string | null
}

// Raw shape from backend /api/returns
interface BackendReturn {
    returnId: string
    orderId: string
    reason: string
    status: string
    requestDate: string
    refundAmount: number | null
    supportStaffId: string | null
    refundMethod: string | null
    evidenceImage?: string | null
    description?: string | null
    orderDate?: string | null
    orderTotal?: number | null
    customerId?: string | null
    customerName?: string | null
    customerEmail?: string | null
    items?: Array<{
        productId?: string | null
        productName?: string | null
        quantity: number
        unitPrice?: number | null
    }> | null
}

/* =========================================================
   API
   ========================================================= */

const RETURN_API = '/api/returns'

/* =========================================================
   DATA
   ========================================================= */

const returnRequests = ref<ReturnRequest[]>([])

/* =========================================================
   FILTERS
   ========================================================= */

const searchQuery = ref('')

const statusFilter =
    ref<'All' | ReturnStatus>('All')

const typeFilter =
    ref<'All' | RequestType>('All')

const isLoading = ref(false)
const isProcessing = ref(false)

/* =========================================================
   REVIEW MODAL
   ========================================================= */

const showReviewModal = ref(false)

const selectedRequest =
    ref<ReturnRequest | null>(null)

const adminNote = ref('')

const refundAmountInput = ref<number | null>(null)

// Read the logged-in staff ID from session / local storage
const resolveSessionStaffId = () => {
    try {
        const raw = sessionStorage.getItem('staffUser') || sessionStorage.getItem('user') || localStorage.getItem('staffUser') || localStorage.getItem('user')
        if (raw) {
            const u = JSON.parse(raw)
            return u.userId || u.staffId || u.adminId || u.id || ''
        }
    } catch {
        // ignore
    }
    return sessionStorage.getItem('staffId') || sessionStorage.getItem('userId') || sessionStorage.getItem('adminId') || localStorage.getItem('staffId') || localStorage.getItem('userId') || localStorage.getItem('adminId') || ''
}

const staffIdInput = ref(resolveSessionStaffId())

/* =========================================================
   USER ROLE & PERMISSIONS
   - Support staff (SUPPORT_STAFF / SUPPORT) and Admins (ADMINISTRATOR / ADMIN) can approve/reject/complete requests.
   - Other staff (e.g. Warehouse Staff, Delivery Staff) have view-only access.
   ========================================================= */

const sessionUserRole = computed(() => {
    try {
        const raw = sessionStorage.getItem('staffUser') || sessionStorage.getItem('user') || localStorage.getItem('staffUser') || localStorage.getItem('user')
        if (raw) {
            const u = JSON.parse(raw)
            return String(u.role || u.userRole || '').toUpperCase()
        }
    } catch {
        // ignore
    }
    return String(sessionStorage.getItem('role') || sessionStorage.getItem('userRole') || localStorage.getItem('role') || '').toUpperCase()
})

const isAdministrator = computed(() => {
    const role = sessionUserRole.value
    return role.includes('ADMIN')
})

const isSupportStaff = computed(() => {
    const role = sessionUserRole.value
    return role.includes('SUPPORT')
})

const canManageRequests = computed(() => {
    const role = sessionUserRole.value
    if (!role) return true
    return (
        role.includes('ADMIN') ||
        role.includes('SUPPORT')
    )
})

/* =========================================================
   HELPERS
   ========================================================= */

const primaryItemName = (
    request: ReturnRequest
) => {
    if (
        !request.items ||
        request.items.length === 0
    ) {
        return 'Order items unavailable'
    }

    const first =
        request.items[0]?.productName ||
        'Unknown Product'

    return request.items.length > 1
        ? `${first} +${request.items.length - 1} more`
        : first
}

const totalQuantity = (
    request: ReturnRequest
) => {
    return (request.items || [])
        .reduce(
            (sum, item) =>
                sum + (item.quantity || 0),
            0
        )
}

const displayAmount = (
    request: ReturnRequest
) => {
    return request.refundAmount ??
        request.orderTotal ??
        0
}

/* =========================================================
   7-DAY REFUND POLICY & LATE TAX
   - Within 7 days of order date  → full refund (no tax)
   - After  7 days of order date  → 10 % late-processing tax deducted
   ========================================================= */

/** Late-processing tax rate applied when return is after 7 days */
const LATE_TAX_RATE = 0.10 // 10 %

/**
 * Returns true when the request was submitted MORE than 7 days
 * after the order was placed.
 */
const isLateReturn = (request: ReturnRequest): boolean => {
    const ref = request.orderDate || request.requestDate
    if (!ref) return false
    const orderMs = new Date(ref).getTime()
    if (isNaN(orderMs)) return false
    const requestMs = request.requestDate
        ? new Date(request.requestDate).getTime()
        : Date.now()
    const diffDays = (requestMs - orderMs) / (1000 * 60 * 60 * 24)
    return diffDays > 7
}

/**
 * Calculates the actual refund amount after applying the late tax
 * (if applicable).
 * Returns { gross, taxAmount, net, isLate }
 */
const computeRefundBreakdown = (request: ReturnRequest) => {
    const gross = displayAmount(request)
    const late = isLateReturn(request)
    const taxAmount = late ? Math.round(gross * LATE_TAX_RATE * 100) / 100 : 0
    const net = Math.round((gross - taxAmount) * 100) / 100
    return { gross, taxAmount, net, isLate: late }
}

/* =========================================================
   FILTERED DATA
   ========================================================= */

const filteredRequests = computed(() => {

    return returnRequests.value.filter(
        (request) => {

            const search =
                searchQuery.value
                    .toLowerCase()
                    .trim()

            const matchesSearch =
                search === '' ||
                request.orderId
                    .toLowerCase()
                    .includes(search) ||
                (request.customerName || '')
                    .toLowerCase()
                    .includes(search) ||
                (request.customerEmail || '')
                    .toLowerCase()
                    .includes(search) ||
                request.reason
                    .toLowerCase()
                    .includes(search) ||
                (request.items || [])
                    .some(
                        item =>
                            (item.productName || '')
                                .toLowerCase()
                                .includes(search)
                    )

            const matchesStatus =
                statusFilter.value === 'All' ||
                request.status === statusFilter.value

            const matchesType =
                typeFilter.value === 'All' ||
                request.requestType === typeFilter.value

            return (
                matchesSearch &&
                matchesStatus &&
                matchesType
            )
        }
    )
})

/* =========================================================
   STATISTICS
   ========================================================= */

const totalRequests = computed(
    () => returnRequests.value.length
)

const pendingRequests = computed(() => {
    return returnRequests.value.filter(
        request =>
            request.status === 'REQUESTED'
    ).length
})

const approvedRequests = computed(() => {
    return returnRequests.value.filter(
        request =>
            request.status === 'APPROVED'
    ).length
})

const completedRequests = computed(() => {
    return returnRequests.value.filter(
        request =>
            request.status === 'COMPLETED'
    ).length
})

const rejectedRequests = computed(() => {
    return returnRequests.value.filter(
        request =>
            request.status === 'REJECTED'
    ).length
})

const totalRefundAmount = computed(() => {
    return returnRequests.value
        .filter(
            request =>
                request.status === 'APPROVED' ||
                request.status === 'COMPLETED'
        )
        .reduce(
            (sum, request) =>
                sum + displayAmount(request),
            0
        )
})

const pendingRefundAmount = computed(() => {
    return returnRequests.value
        .filter(
            request =>
                request.status === 'REQUESTED'
        )
        .reduce(
            (sum, request) =>
                sum + displayAmount(request),
            0
        )
})

/* =========================================================
   MODAL
   ========================================================= */

const openReviewModal = (
    request: ReturnRequest
) => {
    selectedRequest.value = request

    adminNote.value =
        request.adminNote || ''

    // Auto-calculate refund with 7-day policy applied
    const { net } = computeRefundBreakdown(request)
    refundAmountInput.value =
        request.status === 'APPROVED' || request.status === 'COMPLETED'
            ? (request.refundAmount ?? net)  // already handled — keep existing
            : net

    const sessionStaffId = resolveSessionStaffId()
    if (sessionStaffId) {
        staffIdInput.value = sessionStaffId
    } else if (request.supportStaffId) {
        staffIdInput.value = request.supportStaffId
    } else {
        staffIdInput.value = ''
    }

    showReviewModal.value = true
}

const closeReviewModal = () => {
    showReviewModal.value = false
    selectedRequest.value = null
    adminNote.value = ''
    refundAmountInput.value = null
}

/* =========================================================
   CONFIRMATION & ALERT MODAL STATE (ORDER MANAGEMENT DESIGN)
   ========================================================= */

const confirmModal = ref<AdminModalState>({
    show: false,
    type: 'info',
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

/* =========================================================
   STAFF VALIDATION
   ========================================================= */

const requireStaffId = () => {
    if (!staffIdInput.value || !staffIdInput.value.trim()) {
        staffIdInput.value = resolveSessionStaffId()
    }
    if (staffIdInput.value && staffIdInput.value.trim()) {
        return true
    }
    showAlert(
        'Staff Authentication Required',
        'Staff or Administrator ID is required to process this request. Please log in or enter your ID.',
        'warning'
    )
    return false
}

/* =========================================================
   HANDLE REQUEST
   Backend:
   PUT /api/returns/{returnId}/handle
   ========================================================= */

const handleRequest = async (
    request: ReturnRequest,
    status: ReturnStatus,
    refundAmount: number | null
) => {

    if (!canManageRequests.value) {
        showAlert(
            'Access Denied',
            'Only Support Staff and Administrators can process return/refund requests.',
            'danger'
        )
        return
    }

    if (!requireStaffId()) {
        return
    }

    isProcessing.value = true

    const previousStatus = request.status

    try {
        const params: Record<string, any> = {
            supportStaffId: staffIdInput.value.trim(),
            status
        }

        if (refundAmount != null) {
            params.refundAmount = refundAmount
        }

        const response =
            await axios.put<ReturnRequest>(
                `${RETURN_API}/${request.returnId}/handle`,
                null,
                { params }
            )

        /*
         * Update the request in the list using
         * the backend response (re-normalize it).
         */
        const updated = normalizeReturn(response.data as unknown as BackendReturn)
        const idx = returnRequests.value.findIndex(r => r.returnId === request.returnId)
        if (idx !== -1) {
            returnRequests.value[idx] = updated
        }

        closeReviewModal()

    } catch (error: any) {
        console.error(
            `Error updating return request to ${status}:`,
            error
        )
        const msg = error.response?.data?.message || 'Failed to update the request. Please try again.'
        showAlert('Request Failed', msg, 'danger')
        request.status = previousStatus
    } finally {
        isProcessing.value = false
    }
}

/* =========================================================
   APPROVE
   ========================================================= */

const approveRequest = (
    request: ReturnRequest
) => {
    if (!requireStaffId()) {
        return
    }

    const amount = refundAmountInput.value ?? displayAmount(request)

    confirmModal.value = {
        show: true,
        type: 'success',
        title: 'Approve Return Request?',
        message: 'Approve return & refund for order',
        target: request.orderId,
        description: `This will approve the return and authorize a refund of ${formatCurrency(amount)}.`,
        confirmText: 'Approve Request',
        cancelText: 'Cancel',
        loading: false,
        onConfirm: async () => {
            confirmModal.value.loading = true
            try {
                await handleRequest(request, 'APPROVED', amount)
                confirmModal.value.show = false
            } finally {
                confirmModal.value.loading = false
            }
        }
    }
}

/* =========================================================
   REJECT
   ========================================================= */

const rejectRequest = (
    request: ReturnRequest
) => {
    if (!requireStaffId()) {
        return
    }

    confirmModal.value = {
        show: true,
        type: 'danger',
        title: 'Reject Return Request?',
        message: 'Are you sure you want to reject the return for order',
        target: request.orderId,
        description: 'The return request will be closed and marked as rejected.',
        confirmText: 'Reject Request',
        cancelText: 'Cancel',
        loading: false,
        onConfirm: async () => {
            confirmModal.value.loading = true
            try {
                await handleRequest(request, 'REJECTED', null)
                confirmModal.value.show = false
            } finally {
                confirmModal.value.loading = false
            }
        }
    }
}

/* =========================================================
   COMPLETE REFUND
   ========================================================= */

const markAsCompleted = (
    request: ReturnRequest
) => {
    if (!requireStaffId()) {
        return
    }

    const amount = refundAmountInput.value ?? displayAmount(request)

    confirmModal.value = {
        show: true,
        type: 'info',
        title: 'Complete Refund?',
        message: `Confirm that the refund of ${formatCurrency(amount)} has been paid for order`,
        target: request.orderId,
        description: 'This will finalize the return cycle and update the status to COMPLETED.',
        confirmText: 'Complete Refund',
        cancelText: 'Cancel',
        loading: false,
        onConfirm: async () => {
            confirmModal.value.loading = true
            try {
                await handleRequest(request, 'COMPLETED', amount)
                confirmModal.value.show = false
            } finally {
                confirmModal.value.loading = false
            }
        }
    }
}

/* =========================================================
   STATUS HELPERS
   ========================================================= */

const STATUS_LABELS: Record<string, string> = {
    REQUESTED: 'Requested',
    APPROVED: 'Approved',
    REJECTED: 'Rejected',
    COMPLETED: 'Completed'
}

const statusLabel = (
    status: string
) => {
    return STATUS_LABELS[status] || status
}

const getStatusClass = (
    status: string
) => {
    if (status === 'REQUESTED' || status === 'PENDING') {
        return 'bg-amber-50 text-amber-700 border-amber-200'
    }

    if (status === 'APPROVED') {
        return 'bg-blue-50 text-blue-700 border-blue-200'
    }

    if (status === 'COMPLETED') {
        return 'bg-emerald-50 text-emerald-700 border-emerald-200'
    }

    return 'bg-red-50 text-red-600 border-red-200'
}

/* =========================================================
   REQUEST TYPE
   ========================================================= */

const getTypeClass = (
    type: RequestType | null | undefined
) => {
    return type === 'REFUND'
        ? 'bg-cyan-50 text-cyan-700 border-cyan-200'
        : 'bg-violet-50 text-violet-700 border-violet-200'
}

const typeLabel = (
    type: RequestType | null | undefined
) => {
    return type === 'REFUND' ? 'Refund' : 'Return'
}

/* =========================================================
   REASON
   ========================================================= */

const getReasonClass = (
    reason: string
) => {

    if (
        reason?.includes('Defective') ||
        reason?.includes('Not Working')
    ) {
        return 'bg-red-50 text-red-700'
    }

    if (
        reason?.includes('Wrong') ||
        reason?.includes('Damaged')
    ) {
        return 'bg-orange-50 text-orange-700'
    }

    if (
        reason?.includes('Incompatible')
    ) {
        return 'bg-amber-50 text-amber-700'
    }

    return 'bg-slate-100 text-slate-600'
}

/* =========================================================
   FORMATTERS
   ========================================================= */

const formatCurrency = (
    value: number | null | undefined
) => {
    return `LKR ${(value ?? 0).toLocaleString('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    })}`
}

const REFUND_METHOD_LABELS: Record<string, string> = {
    ORIGINAL_PAYMENT_METHOD: 'Original Payment Method',
    BANK_TRANSFER: 'Bank Transfer',
    STORE_CREDIT: 'Store Credit'
}

const formatRefundMethod = (
    method: string | null
) => {
    if (!method) {
        return 'Not specified'
    }
    return REFUND_METHOD_LABELS[method] ?? method
}

const formatDate = (
    date: string | null
) => {
    if (!date) {
        return '-'
    }
    return new Date(date)
        .toLocaleDateString(
            'en-US',
            {
                year: 'numeric',
                month: 'short',
                day: 'numeric'
            }
        )
}

/* =========================================================
   EVIDENCE IMAGE URL
   ========================================================= */

const getEvidenceImageUrl = (
    imagePath: string | null
) => {
    if (!imagePath) {
        return ''
    }
    // Already absolute
    if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
        return imagePath
    }
    // /uploads/... paths are proxied by Vite to localhost:8080 in dev.
    // Return as-is so the Vite proxy handles it correctly.
    if (imagePath.startsWith('/')) {
        return imagePath
    }
    // Relative path without leading slash — prefix backend origin
    return `http://localhost:8080/${imagePath}`
}

/* =========================================================
   NORMALIZE BACKEND RETURN → FRONTEND ReturnRequest
   The backend only returns: returnId, orderId, reason,
   status, requestDate, refundAmount, supportStaffId,
   refundMethod, evidenceImage.
   We map this into the richer frontend type.
   ========================================================= */

function normalizeReturn(raw: BackendReturn): ReturnRequest {
    const refundAmount = raw.refundAmount != null && Number(raw.refundAmount) > 0
        ? Number(raw.refundAmount)
        : (raw.orderTotal != null && Number(raw.orderTotal) > 0
            ? Number(raw.orderTotal)
            : null)

    const items: ReturnRequestItem[] = (raw.items ?? []).map(i => ({
        productId: i.productId ?? null,
        productName: i.productName ?? null,
        quantity: i.quantity ?? 0,
        unitPrice: i.unitPrice != null ? Number(i.unitPrice) : 0
    }))

    return {
        returnId: raw.returnId,
        orderId: raw.orderId || '',
        orderDate: raw.orderDate ?? null,
        orderTotal: raw.orderTotal != null ? Number(raw.orderTotal) : null,
        items,
        customerId: raw.customerId ?? null,
        customerName: raw.customerName ?? null,
        customerEmail: raw.customerEmail ?? null,
        reason: raw.reason || '-',
        description: raw.description ?? null,
        requestType: 'RETURN',
        refundMethod: raw.refundMethod || null,
        requestDate: raw.requestDate || '',
        status: (raw.status || 'REQUESTED') as ReturnStatus,
        refundAmount,
        supportStaffId: raw.supportStaffId || null,
        adminNote: null,
        evidenceImage: raw.evidenceImage || null
    }
}

/* =========================================================
   LOAD DATA
   ========================================================= */

const loadRequests = async () => {

    isLoading.value = true

    try {
        const response = await axios.get<BackendReturn[]>(RETURN_API)

        if (
            response.data &&
            Array.isArray(response.data)
        ) {
            returnRequests.value = response.data.map(normalizeReturn).reverse()
        }

    } catch (error) {
        console.error(
            'Failed to load return/refund requests:',
            error
        )
        returnRequests.value = []
        showAlert(
            'Load Failed',
            'Unable to load return/refund requests from the server.',
            'danger'
        )
    } finally {
        isLoading.value = false
    }
}

/* =========================================================
   INITIAL LOAD
   ========================================================= */

onMounted(() => {
    loadRequests()
})
</script>

<template>

    <div class="min-h-screen bg-slate-100 text-slate-900">

        <!-- =====================================================
             SIDEBAR
             ===================================================== -->

        <AdminSidebar />

        <!-- =====================================================
             MAIN
             ===================================================== -->

        <main class="ml-64 min-h-screen">

            <!-- Background -->

            <div class="pointer-events-none fixed inset-0 overflow-hidden">

                <div class="absolute -right-40 -top-40 h-96 w-96 rounded-full bg-blue-200/30 blur-3xl">
                </div>

                <div class="absolute -left-40 top-1/2 h-96 w-96 rounded-full bg-cyan-200/20 blur-3xl">
                </div>

                <div class="absolute bottom-0 right-1/4 h-80 w-80 rounded-full bg-violet-200/20 blur-3xl">
                </div>

            </div>

            <div class="relative z-10 p-6 lg:p-8">

                <!-- =================================================
                     HEADER
                     ================================================= -->

                <div class="mb-8 flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">

                    <div>

                        <div class="mb-2 flex items-center gap-2">

                            <span class="h-2 w-2 rounded-full bg-amber-500 shadow-lg shadow-amber-500/40">
                            </span>

                            <span class="text-xs font-bold uppercase tracking-[0.18em] text-amber-600">

                                Customer Service

                            </span>

                        </div>

                        <h1 class="text-3xl font-black tracking-tight text-slate-950 sm:text-4xl">

                            Return & Refund

                        </h1>

                        <p class="mt-2 max-w-2xl text-sm text-slate-500">

                            Review customer return requests, inspect evidence,
                            approve refunds and complete refund transactions.

                        </p>

                    </div>

                    <button type="button" @click="loadRequests" :disabled="isLoading"
                        class="inline-flex items-center justify-center gap-2 rounded-2xl border border-white/90 bg-white/80 px-5 py-3.5 text-sm font-bold text-slate-700 shadow-lg shadow-slate-300/10 backdrop-blur-xl transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-600 disabled:opacity-50">

                        <svg class="h-5 w-5" :class="{ 'animate-spin': isLoading }" fill="none" stroke="currentColor"
                            viewBox="0 0 24 24">

                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                d="M4 4v5h5M20 20v-5h-5M5.05 9A7 7 0 0117.95 7M18.95 15A7 7 0 016.05 17" />

                        </svg>

                        Refresh Requests

                    </button>

                </div>

                <!-- =================================================
                     STATISTICS
                     ================================================= -->

                <div class="mb-8 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-5">

                    <!-- Total -->

                    <div
                        class="rounded-2xl border border-white/90 bg-white/70 p-5 shadow-lg shadow-slate-300/10 backdrop-blur-2xl">

                        <div class="flex items-center justify-between">

                            <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-50 text-blue-600">

                                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">

                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                        d="M20 7l-8-4-8 4m16 0v10l-8 4-8-4V7m16 0l-8 4m-8-4l8 4m0 0v10" />

                                </svg>

                            </div>

                            <span class="rounded-full bg-blue-50 px-2.5 py-1 text-xs font-bold text-blue-600">

                                ALL

                            </span>

                        </div>

                        <p class="mt-5 text-2xl font-black text-slate-950">
                            {{ totalRequests }}
                        </p>

                        <p class="mt-1 text-sm text-slate-500">
                            Total Requests
                        </p>

                    </div>

                    <!-- Pending -->

                    <div
                        class="rounded-2xl border border-white/90 bg-white/70 p-5 shadow-lg shadow-slate-300/10 backdrop-blur-2xl">

                        <div class="flex items-center justify-between">

                            <div
                                class="flex h-11 w-11 items-center justify-center rounded-xl bg-amber-50 text-amber-600">

                                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">

                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                        d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />

                                </svg>

                            </div>

                            <span class="rounded-full bg-amber-50 px-2.5 py-1 text-xs font-bold text-amber-600">

                                ACTION

                            </span>

                        </div>

                        <p class="mt-5 text-2xl font-black text-slate-950">
                            {{ pendingRequests }}
                        </p>

                        <p class="mt-1 text-sm text-slate-500">
                            Pending Review
                        </p>

                    </div>

                    <!-- Approved -->

                    <div
                        class="rounded-2xl border border-white/90 bg-white/70 p-5 shadow-lg shadow-slate-300/10 backdrop-blur-2xl">

                        <div class="flex items-center justify-between">

                            <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-50 text-blue-600">

                                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">

                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                        d="M5 13l4 4L19 7" />

                                </svg>

                            </div>

                            <span class="rounded-full bg-blue-50 px-2.5 py-1 text-xs font-bold text-blue-600">

                                APPROVED

                            </span>

                        </div>

                        <p class="mt-5 text-2xl font-black text-slate-950">
                            {{ approvedRequests }}
                        </p>

                        <p class="mt-1 text-sm text-slate-500">
                            Approved Requests
                        </p>

                    </div>

                    <!-- Completed -->

                    <div
                        class="rounded-2xl border border-white/90 bg-white/70 p-5 shadow-lg shadow-slate-300/10 backdrop-blur-2xl">

                        <div class="flex items-center justify-between">

                            <div
                                class="flex h-11 w-11 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600">

                                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">

                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                        d="M5 13l4 4L19 7" />

                                </svg>

                            </div>

                            <span class="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-bold text-emerald-600">

                                DONE

                            </span>

                        </div>

                        <p class="mt-5 text-2xl font-black text-slate-950">
                            {{ completedRequests }}
                        </p>

                        <p class="mt-1 text-sm text-slate-500">
                            Completed Refunds
                        </p>

                    </div>

                    <!-- Refund Amount -->

                    <div
                        class="rounded-2xl border border-white/90 bg-white/70 p-5 shadow-lg shadow-slate-300/10 backdrop-blur-2xl">

                        <div class="flex items-center justify-between">

                            <div
                                class="flex h-11 w-11 items-center justify-center rounded-xl bg-violet-50 text-violet-600">

                                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">

                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                        d="M12 8c-2.21 0-4 1.12-4 2.5S9.79 13 12 13s4 1.12 4 2.5S14.21 18 12 18m0-10V6m0 2c1.657 0 3 .672 3 1.5S13.657 11 12 11s-3-.672-3-1.5S10.343 8 12 8zM12 18v-2" />

                                </svg>

                            </div>

                            <span class="rounded-full bg-violet-50 px-2.5 py-1 text-xs font-bold text-violet-600">

                                VALUE

                            </span>

                        </div>

                        <p class="mt-5 truncate text-2xl font-black text-slate-950"
                            :title="formatCurrency(totalRefundAmount)">

                            {{ formatCurrency(totalRefundAmount) }}

                        </p>

                        <p class="mt-1 text-sm text-slate-500">
                            Approved Refund Value
                        </p>

                    </div>

                </div>

                <!-- =================================================
                     PENDING SUMMARY
                     ================================================= -->

                <div v-if="pendingRequests > 0"
                    class="mb-6 flex flex-col gap-4 rounded-2xl border border-amber-200/70 bg-amber-50/70 p-5 shadow-lg shadow-amber-100/40 backdrop-blur-xl sm:flex-row sm:items-center sm:justify-between">

                    <div class="flex items-center gap-4">

                        <div
                            class="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-white text-amber-600 shadow-sm">

                            <svg class="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">

                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                    d="M12 9v3.75m0 3.75h.008v.008H12V16.5zM10.29 3.86l-8.2 14.2A1.5 1.5 0 003.39 20.25h17.22a1.5 1.5 0 001.3-2.19l-8.2-14.2a1.5 1.5 0 00-2.6 0z" />

                            </svg>

                        </div>

                        <div>

                            <h3 class="font-black text-slate-900">

                                {{ pendingRequests }}
                                request{{ pendingRequests === 1 ? '' : 's' }}
                                awaiting review

                            </h3>

                            <p class="mt-1 text-sm text-slate-600">

                                Pending refund value:

                                <span class="font-black text-amber-700">

                                    {{ formatCurrency(pendingRefundAmount) }}

                                </span>

                            </p>

                        </div>

                    </div>

                    <button type="button" @click="statusFilter = 'REQUESTED'"
                        class="rounded-xl bg-amber-500 px-4 py-2.5 text-sm font-bold text-white shadow-lg shadow-amber-500/20 transition hover:bg-amber-600">

                        View Pending

                    </button>

                </div>

                <!-- =================================================
                     FILTERS
                     ================================================= -->

                <div
                    class="mb-6 rounded-2xl border border-white/90 bg-white/70 p-4 shadow-lg shadow-slate-300/10 backdrop-blur-2xl">

                    <div class="flex flex-col gap-3 xl:flex-row xl:items-center">

                        <!-- Search -->

                        <div class="relative flex-1">

                            <svg class="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" fill="none"
                                stroke="currentColor" viewBox="0 0 24 24">

                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                    d="M21 21l-4.35-4.35m2.1-5.4a7.5 7.5 0 11-15 0 7.5 7.5 0 0115 0z" />

                            </svg>

                            <input v-model="searchQuery" type="text" placeholder="Search order, customer or product..."
                                class="w-full rounded-xl border border-slate-200/80 bg-white/80 py-3 pl-11 pr-4 text-sm text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10" />

                        </div>

                        <!-- Status -->

                        <select v-model="statusFilter"
                            class="rounded-xl border border-slate-200/80 bg-white/80 px-4 py-3 text-sm font-medium text-slate-700 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10">

                            <option value="All">
                                All Status
                            </option>

                            <option value="REQUESTED">
                                Requested
                            </option>

                            <option value="APPROVED">
                                Approved
                            </option>

                            <option value="COMPLETED">
                                Completed
                            </option>

                            <option value="REJECTED">
                                Rejected
                            </option>

                        </select>

                        <!-- Type -->

                        <select v-model="typeFilter"
                            class="rounded-xl border border-slate-200/80 bg-white/80 px-4 py-3 text-sm font-medium text-slate-700 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10">

                            <option value="All">
                                All Request Types
                            </option>

                            <option value="RETURN">
                                Return
                            </option>

                            <option value="REFUND">
                                Refund
                            </option>

                        </select>

                    </div>

                </div>

                <!-- =================================================
                     TABLE
                     ================================================= -->

                <div
                    class="overflow-hidden rounded-2xl border border-white/90 bg-white/70 shadow-xl shadow-slate-300/10 backdrop-blur-2xl">

                    <div
                        class="flex flex-col gap-2 border-b border-slate-200/70 px-6 py-5 sm:flex-row sm:items-center sm:justify-between">

                        <div>

                            <h2 class="text-lg font-black text-slate-950">
                                Return & Refund Requests
                            </h2>

                            <p class="mt-1 text-sm text-slate-500">

                                {{ filteredRequests.length }}
                                request{{ filteredRequests.length === 1 ? '' : 's' }}
                                displayed

                            </p>

                        </div>

                        <span
                            class="inline-flex w-fit items-center gap-2 rounded-full bg-amber-50 px-3 py-1.5 text-xs font-bold text-amber-700">

                            <span class="h-1.5 w-1.5 rounded-full bg-amber-500">
                            </span>

                            Admin Review

                        </span>

                    </div>

                    <!-- Desktop -->

                    <div class="hidden overflow-x-auto lg:block">

                        <!-- FIX:
                             Increased minimum table width so the
                             Actions column does not overlap/collapse.
                        -->

                        <table class="w-full min-w-[1100px]">

                            <thead>
                                <tr class="border-b border-slate-200/70 bg-slate-50/60 text-left">

                                    <th class="px-6 py-4 text-xs font-black uppercase tracking-wider text-slate-500">
                                        Request
                                    </th>

                                    <th class="px-6 py-4 text-xs font-black uppercase tracking-wider text-slate-500">
                                        Product
                                    </th>

                                    <th class="px-6 py-4 text-xs font-black uppercase tracking-wider text-slate-500">
                                        Reason
                                    </th>

                                    <th class="px-6 py-4 text-xs font-black uppercase tracking-wider text-slate-500">
                                        Refund
                                    </th>

                                    <th class="px-6 py-4 text-xs font-black uppercase tracking-wider text-slate-500">
                                        Date
                                    </th>

                                    <th class="px-6 py-4 text-xs font-black uppercase tracking-wider text-slate-500">
                                        Status
                                    </th>

                                    <th
                                        class="w-[150px] min-w-[150px] px-6 py-4 text-right text-xs font-black uppercase tracking-wider text-slate-500">
                                        Actions
                                    </th>

                                </tr>
                            </thead>

                            <tbody class="divide-y divide-slate-200/60">

                                <tr v-for="request in filteredRequests" :key="request.returnId"
                                    class="group transition hover:bg-blue-50/30">

                                    <!-- Request -->

                                    <td class="px-6 py-5">

                                        <div class="flex items-center gap-4">

                                            <div
                                                class="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-amber-50 to-orange-50 text-amber-600">

                                                <svg class="h-5 w-5" fill="none" stroke="currentColor"
                                                    viewBox="0 0 24 24">

                                                    <path stroke-linecap="round" stroke-linejoin="round"
                                                        stroke-width="2"
                                                        d="M9 7h6m-7 4h8m-9 4h10M7 3h10a2 2 0 012 2v14l-7-3-7 3V5a2 2 0 012-2z" />

                                                </svg>

                                            </div>

                                            <div>

                                                <div class="font-black tracking-wide text-slate-950">

                                                    #{{ request.orderId }}

                                                </div>

                                                <span :class="[
                                                    'mt-1 inline-flex rounded-lg border px-2 py-1 text-[10px] font-black',
                                                    getTypeClass(request.requestType)
                                                ]">

                                                    {{ typeLabel(request.requestType) }}

                                                </span>

                                            </div>

                                        </div>

                                    </td>

                                    <!-- Product -->

                                    <td class="px-6 py-5">

                                        <div class="flex max-w-xs items-center gap-3">

                                            <div
                                                class="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl border border-slate-200 bg-slate-50 text-slate-400">

                                                <svg class="h-5 w-5" fill="none" stroke="currentColor"
                                                    viewBox="0 0 24 24">

                                                    <path stroke-linecap="round" stroke-linejoin="round"
                                                        stroke-width="2"
                                                        d="M20.25 7.5l-8.25 4.5L3.75 7.5M3.75 7.5l8.25-4.5 8.25 4.5M3.75 7.5v9l8.25 4.5m0-13.5v13.5m0-13.5l8.25-4.5m-8.25 18l8.25-4.5v-9" />

                                                </svg>

                                            </div>

                                            <div class="min-w-0">

                                                <p class="truncate text-sm font-bold text-slate-800">

                                                    {{ primaryItemName(request) }}

                                                </p>

                                                <p class="mt-1 text-xs text-slate-400">

                                                    Qty:
                                                    {{ totalQuantity(request) }}

                                                </p>

                                            </div>

                                        </div>

                                    </td>

                                    <!-- Reason -->

                                    <td class="px-6 py-5">

                                        <span :class="[
                                            'inline-flex rounded-lg px-2.5 py-1.5 text-xs font-bold',
                                            getReasonClass(request.reason)
                                        ]">

                                            {{ request.reason }}

                                        </span>

                                    </td>

                                    <!-- Refund -->

                                    <td class="px-6 py-5">

                                        <p class="font-black text-slate-900">

                                            {{ formatCurrency(displayAmount(request)) }}

                                        </p>

                                        <p class="mt-1 max-w-[150px] truncate text-xs text-slate-400">

                                            {{ formatRefundMethod(request.refundMethod) }}

                                        </p>

                                    </td>

                                    <!-- Date -->

                                    <td class="px-6 py-5">

                                        <p class="text-xs font-bold text-slate-700">

                                            {{ formatDate(request.requestDate) }}

                                        </p>

                                    </td>

                                    <!-- Status -->

                                    <td class="px-6 py-5">

                                        <span :class="[
                                            'inline-flex rounded-full border px-3 py-1.5 text-xs font-bold',
                                            getStatusClass(request.status)
                                        ]">

                                            {{ statusLabel(request.status) }}

                                        </span>

                                    </td>

                                    <!-- Actions -->

                                    <td class="w-[150px] min-w-[150px] px-6 py-5">

                                        <div class="flex items-center justify-end gap-2 whitespace-nowrap">

                                            <!-- Review -->

                                            <button type="button" @click="openReviewModal(request)"
                                                class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-500 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-600"
                                                title="Review request">

                                                <svg class="h-4 w-4" fill="none" stroke="currentColor"
                                                    viewBox="0 0 24 24">

                                                    <path stroke-linecap="round" stroke-linejoin="round"
                                                        stroke-width="2"
                                                        d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />

                                                    <path stroke-linecap="round" stroke-linejoin="round"
                                                        stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />

                                                </svg>

                                            </button>

                                            <!-- Approve -->

                                            <button v-if="canManageRequests && request.status === 'REQUESTED'" type="button"
                                                @click="openReviewModal(request)"
                                                class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-emerald-200 bg-emerald-50 text-emerald-600 transition hover:bg-emerald-100"
                                                title="Approve request">

                                                <svg class="h-4 w-4" fill="none" stroke="currentColor"
                                                    viewBox="0 0 24 24">

                                                    <path stroke-linecap="round" stroke-linejoin="round"
                                                        stroke-width="2" d="M5 13l4 4L19 7" />

                                                </svg>

                                            </button>

                                            <!-- Complete -->

                                            <button v-if="canManageRequests && request.status === 'APPROVED'" type="button"
                                                @click="openReviewModal(request)"
                                                class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-blue-200 bg-blue-50 text-blue-600 transition hover:bg-blue-100"
                                                title="Complete refund">

                                                <svg class="h-4 w-4" fill="none" stroke="currentColor"
                                                    viewBox="0 0 24 24">

                                                    <path stroke-linecap="round" stroke-linejoin="round"
                                                        stroke-width="2" d="M5 13l4 4L19 7" />

                                                </svg>

                                            </button>

                                        </div>

                                    </td>

                                </tr>

                            </tbody>

                        </table>
                    </div>

                    <!-- Empty -->

                    <div v-if="filteredRequests.length === 0 && !isLoading" class="px-6 py-16 text-center">

                        <div
                            class="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-slate-100 text-slate-400">

                            <svg class="h-7 w-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">

                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                    d="M9 14.25l6-6m2.25-3.75h.008v.008H17.25V4.5zM6.75 19.5h.008v.008H6.75V19.5zM21 12a9 9 0 11-18 0z" />

                            </svg>

                        </div>

                        <h3 class="mt-4 font-bold text-slate-900">
                            No return or refund requests found
                        </h3>

                        <p class="mt-1 text-sm text-slate-500">
                            Try changing your search or filter options.
                        </p>

                    </div>

                    <!-- =================================================
                         MOBILE
                         ================================================= -->

                    <div class="space-y-4 p-4 lg:hidden">

                        <div v-for="request in filteredRequests" :key="request.returnId"
                            class="rounded-2xl border border-slate-200/80 bg-white/80 p-4 shadow-sm">

                            <!-- Top -->

                            <div class="flex items-start justify-between gap-3">

                                <div class="flex min-w-0 items-center gap-3">

                                    <div
                                        class="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl border border-slate-200 bg-slate-50 text-slate-400">

                                        <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">

                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                                d="M20.25 7.5l-8.25 4.5L3.75 7.5M3.75 7.5l8.25-4.5 8.25 4.5M3.75 7.5v9l8.25 4.5m0-13.5v13.5m0-13.5l8.25-4.5m-8.25 18l8.25-4.5v-9" />

                                        </svg>

                                    </div>

                                    <div class="min-w-0">

                                        <h3 class="truncate font-black text-slate-950">

                                            {{ primaryItemName(request) }}

                                        </h3>

                                        <p class="mt-0.5 text-xs text-slate-400">

                                            {{ request.orderId }}

                                        </p>

                                    </div>

                                </div>

                                <span :class="[
                                    'shrink-0 rounded-full border px-2.5 py-1 text-[10px] font-bold',
                                    getStatusClass(request.status)
                                ]">

                                    {{ statusLabel(request.status) }}

                                </span>

                            </div>

                            <!-- Tags -->

                            <div class="mt-4 flex flex-wrap items-center gap-2">

                                <span :class="[
                                    'rounded-lg border px-2 py-1 text-[10px] font-black',
                                    getTypeClass(request.requestType)
                                ]">

                                    {{ typeLabel(request.requestType) }}

                                </span>

                                <span :class="[
                                    'rounded-lg px-2 py-1 text-[10px] font-bold',
                                    getReasonClass(request.reason)
                                ]">

                                    {{ request.reason }}

                                </span>

                            </div>

                            <!-- Evidence -->

                            <div v-if="request.evidenceImage" class="mt-4">

                                <p class="mb-2 text-[10px] font-bold uppercase tracking-wider text-slate-400">

                                    Evidence Image

                                </p>

                                <a :href="getEvidenceImageUrl(request.evidenceImage)" target="_blank"
                                    rel="noopener noreferrer">

                                    <img :src="getEvidenceImageUrl(request.evidenceImage)" alt="Return evidence"
                                        class="h-28 w-full rounded-xl border border-slate-200 object-cover" />

                                </a>

                            </div>

                            <!-- Information -->

                            <div class="mt-4 grid grid-cols-2 gap-3 rounded-xl bg-slate-50/80 p-3">

                                <div>

                                    <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">

                                        Customer

                                    </p>

                                    <p class="mt-1 truncate text-sm font-bold text-slate-700">

                                        {{ request.customerName || 'Unknown Customer' }}

                                    </p>

                                </div>

                                <div>

                                    <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">

                                        Refund

                                    </p>

                                    <p class="mt-1 text-sm font-black text-blue-600">

                                        {{ formatCurrency(displayAmount(request)) }}

                                    </p>

                                </div>

                                <div>

                                    <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">

                                        Quantity

                                    </p>

                                    <p class="mt-1 text-sm font-bold text-slate-700">

                                        {{ totalQuantity(request) }}

                                    </p>

                                </div>

                                <div>

                                    <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">

                                        Request Date

                                    </p>

                                    <p class="mt-1 text-sm font-bold text-slate-700">

                                        {{ formatDate(request.requestDate) }}

                                    </p>

                                </div>

                            </div>

                            <!-- Buttons -->

                            <div class="mt-4 flex gap-2">

                                <button type="button" @click="openReviewModal(request)"
                                    class="flex flex-1 items-center justify-center gap-2 rounded-xl border border-blue-200 bg-blue-50 py-2.5 text-sm font-bold text-blue-600 transition hover:bg-blue-100">

                                    Review

                                </button>

                            </div>

                        </div>

                    </div>

                </div>

            </div>

        </main>

        <!-- =====================================================
             REVIEW MODAL
             ===================================================== -->

        <Transition name="modal">

            <div v-if="showReviewModal && selectedRequest"
                class="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 p-4 backdrop-blur-sm"
                @click.self="closeReviewModal">

                <div
                    class="max-h-[92vh] w-full max-w-3xl overflow-y-auto rounded-3xl border border-white/90 bg-white/95 p-6 shadow-2xl shadow-slate-900/20 backdrop-blur-2xl sm:p-8">

                    <!-- Header -->

                    <div class="mb-6 flex items-start justify-between">

                        <div>

                            <div
                                class="mb-3 flex h-11 w-11 items-center justify-center rounded-xl bg-amber-50 text-amber-600">

                                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">

                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                        d="M9 7h6m-7 4h8m-9 4h10M7 3h10a2 2 0 012 2v14l-7-3-7 3V5a2 2 0 012-2z" />

                                </svg>

                            </div>

                            <h2 class="text-2xl font-black text-slate-950">

                                Review Request

                            </h2>

                            <p class="mt-1 text-sm text-slate-500">

                                Review the request and evidence before taking action.

                            </p>

                        </div>

                        <button type="button" @click="closeReviewModal"
                            class="rounded-xl p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700">

                            <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">

                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                    d="M6 18L18 6M6 6l12 12" />

                            </svg>

                        </button>

                    </div>

                    <!-- Order -->

                    <div class="mb-5 rounded-2xl border border-slate-200 bg-slate-50/80 p-4">

                        <div class="flex flex-wrap items-center justify-between gap-2">

                            <p class="text-xs font-bold uppercase tracking-wider text-slate-400">

                                Order #{{ selectedRequest.orderId }}

                            </p>

                            <div class="flex gap-2">

                                <span :class="[
                                    'rounded-lg border px-2 py-1 text-[10px] font-black',
                                    getTypeClass(selectedRequest.requestType)
                                ]">

                                    {{ typeLabel(selectedRequest.requestType) }}

                                </span>

                                <span :class="[
                                    'rounded-lg border px-2 py-1 text-[10px] font-black',
                                    getStatusClass(selectedRequest.status)
                                ]">

                                    {{ statusLabel(selectedRequest.status) }}

                                </span>

                            </div>

                        </div>

                        <!-- Items -->

                        <div class="mt-3 space-y-2">

                            <div v-for="item in selectedRequest.items" :key="`${item.productId}-${item.quantity}`"
                                class="flex items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white px-3 py-2">

                                <div class="min-w-0">

                                    <p class="truncate text-sm font-bold text-slate-800">

                                        {{ item.productName || 'Unknown Product' }}

                                    </p>

                                    <p class="text-xs text-slate-400">

                                        Qty: {{ item.quantity }}

                                    </p>

                                </div>

                                <p class="shrink-0 text-sm font-black text-slate-700">

                                    {{ formatCurrency(item.unitPrice) }}

                                </p>

                            </div>

                        </div>

                        <div class="mt-4 flex items-center justify-between">

                            <span class="text-sm font-bold text-slate-500">

                                Refund Amount

                            </span>

                            <span class="text-lg font-black text-blue-600">

                                {{ formatCurrency(displayAmount(selectedRequest)) }}

                            </span>

                        </div>

                    </div>

                    <!-- Customer / Refund -->

                    <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">

                        <div class="rounded-2xl border border-slate-200 bg-white p-4">

                            <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">

                                Customer

                            </p>

                            <p class="mt-1 font-black text-slate-800">

                                {{ selectedRequest.customerName || 'Unknown Customer' }}

                            </p>

                            <p class="mt-1 text-xs text-slate-500">

                                {{ selectedRequest.customerEmail || 'No email on file' }}

                            </p>

                        </div>

                        <div class="rounded-2xl border border-slate-200 bg-white p-4">

                            <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">

                                Refund Method

                            </p>

                            <p class="mt-1 font-black text-slate-800">

                                {{ formatRefundMethod(selectedRequest.refundMethod) }}

                            </p>

                        </div>

                        <div class="rounded-2xl border border-slate-200 bg-white p-4">

                            <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">

                                Reason

                            </p>

                            <p class="mt-1 font-black text-slate-800">

                                {{ selectedRequest.reason }}

                            </p>

                        </div>

                        <div class="rounded-2xl border border-slate-200 bg-white p-4">

                            <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">

                                Request Date

                            </p>

                            <p class="mt-1 font-black text-slate-800">

                                {{ formatDate(selectedRequest.requestDate) }}

                            </p>

                        </div>

                    </div>

                    <!-- =================================================
                         EVIDENCE IMAGE
                         ================================================= -->

                    <div class="mt-5 rounded-2xl border border-slate-200 bg-slate-50/70 p-4">

                        <div class="flex items-center justify-between">

                            <div>

                                <p class="text-sm font-black text-slate-800">
                                    Customer Evidence
                                </p>

                                <p class="mt-1 text-xs text-slate-500">
                                    Uploaded image submitted with the request.
                                </p>

                            </div>

                            <span v-if="selectedRequest.evidenceImage"
                                class="rounded-full bg-emerald-50 px-3 py-1 text-xs font-bold text-emerald-600">

                                Evidence Uploaded

                            </span>

                            <span v-else class="rounded-full bg-slate-100 px-3 py-1 text-xs font-bold text-slate-500">

                                No Evidence

                            </span>

                        </div>

                        <div v-if="selectedRequest.evidenceImage" class="mt-4">

                            <a :href="getEvidenceImageUrl(selectedRequest.evidenceImage)" target="_blank"
                                rel="noopener noreferrer">

                                <img :src="getEvidenceImageUrl(selectedRequest.evidenceImage)"
                                    alt="Customer return evidence"
                                    class="max-h-96 w-full rounded-2xl border border-slate-200 bg-white object-contain shadow-sm transition hover:opacity-95" />

                            </a>

                            <p class="mt-2 text-center text-xs text-slate-400">

                                Click the image to open it in a new tab.

                            </p>

                        </div>

                        <div v-else class="mt-4 rounded-xl bg-white p-6 text-center text-sm text-slate-400">

                            No evidence image was uploaded.

                        </div>

                    </div>

                    <!-- Description -->

                    <div v-if="selectedRequest.description" class="mt-5">

                        <label class="mb-2 block text-sm font-bold text-slate-700">

                            Customer Description

                        </label>

                        <div
                            class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4 text-sm leading-6 text-slate-600">

                            {{ selectedRequest.description }}

                        </div>

                    </div>

                    <!-- Staff / Admin Handler -->

                    <div v-if="
                        selectedRequest.status === 'REQUESTED' ||
                        selectedRequest.status === 'APPROVED'
                    " class="mt-5">

                        <div class="mb-2 flex items-center justify-between">
                            <label class="block text-sm font-bold text-slate-700">
                                Handled By ({{ isAdministrator ? 'Administrator ID' : (isSupportStaff ? 'Support Staff ID' : 'Staff / Admin ID') }})
                            </label>

                            <span v-if="resolveSessionStaffId()" class="inline-flex items-center gap-1.5 rounded-full bg-blue-50 px-2.5 py-0.5 text-[11px] font-semibold text-blue-700 border border-blue-100">
                                Logged in: {{ resolveSessionStaffId() }} ({{ isAdministrator ? 'Administrator' : (isSupportStaff ? 'Support Staff' : 'Staff') }})
                            </span>
                        </div>

                        <div class="relative">
                            <input v-model="staffIdInput" type="text"
                                :placeholder="isAdministrator ? 'e.g. ADM001' : 'e.g. SUP001'"
                                :disabled="!canManageRequests"
                                class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10 disabled:bg-slate-100 disabled:text-slate-500 pr-24" />

                            <button v-if="resolveSessionStaffId() && staffIdInput !== resolveSessionStaffId()"
                                type="button"
                                @click="staffIdInput = resolveSessionStaffId()"
                                class="absolute right-2 top-2 rounded-lg bg-blue-50 px-2.5 py-1 text-xs font-bold text-blue-600 hover:bg-blue-100 transition border border-blue-200"
                                title="Use your logged-in session ID">
                                Use My ID
                            </button>
                        </div>

                        <p class="mt-1 text-xs text-slate-400">
                            Auto-filled from your active login session. Required when approving, rejecting or completing a request.
                        </p>

                    </div>

                    <!-- Refund Amount Input + 7-Day Policy Breakdown -->

                    <div v-if="
                        selectedRequest.status === 'REQUESTED' ||
                        selectedRequest.status === 'APPROVED'
                    " class="mt-5">

                        <!-- Late return warning -->
                        <template v-if="isLateReturn(selectedRequest)">
                            <div class="mb-3 flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3">
                                <svg class="mt-0.5 h-4 w-4 shrink-0 text-amber-600" fill="currentColor" viewBox="0 0 20 20">
                                    <path fill-rule="evenodd" d="M8.485 2.495c.673-1.167 2.357-1.167 3.03 0l6.28 10.875c.673 1.167-.17 2.625-1.516 2.625H3.72c-1.347 0-2.189-1.458-1.515-2.625L8.485 2.495zM10 5a.75.75 0 01.75.75v3.5a.75.75 0 01-1.5 0v-3.5A.75.75 0 0110 5zm0 9a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd" />
                                </svg>
                                <div>
                                    <p class="text-xs font-bold text-amber-700">Late Return - Processing Tax Applied</p>
                                    <p class="mt-0.5 text-xs text-amber-600">
                                        This return was submitted more than 7 days after the order date.
                                        A <strong>{{ (LATE_TAX_RATE * 100).toFixed(0) }}% late-processing tax</strong> has been deducted.
                                    </p>
                                </div>
                            </div>
                            <!-- Tax breakdown -->
                            <div class="mb-3 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm">
                                <div class="flex items-center justify-between py-1">
                                    <span class="text-slate-500">Order Total</span>
                                    <span class="font-semibold text-slate-700">{{ formatCurrency(computeRefundBreakdown(selectedRequest).gross) }}</span>
                                </div>
                                <div class="flex items-center justify-between py-1">
                                    <span class="text-red-500">Late Tax ({{ (LATE_TAX_RATE * 100).toFixed(0) }}%)</span>
                                    <span class="font-semibold text-red-600">- {{ formatCurrency(computeRefundBreakdown(selectedRequest).taxAmount) }}</span>
                                </div>
                                <div class="mt-1 flex items-center justify-between border-t border-slate-200 pt-2">
                                    <span class="font-bold text-slate-700">Net Refund</span>
                                    <span class="font-bold text-emerald-600">{{ formatCurrency(computeRefundBreakdown(selectedRequest).net) }}</span>
                                </div>
                            </div>
                        </template>

                        <!-- Within 7 days full refund notice -->
                        <template v-else>
                            <div class="mb-3 flex items-start gap-3 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3">
                                <svg class="mt-0.5 h-4 w-4 shrink-0 text-emerald-600" fill="currentColor" viewBox="0 0 20 20">
                                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.857-9.809a.75.75 0 00-1.214-.882l-3.483 4.79-1.88-1.88a.75.75 0 10-1.06 1.061l2.5 2.5a.75.75 0 001.137-.089l4-5.5z" clip-rule="evenodd" />
                                </svg>
                                <div>
                                    <p class="text-xs font-bold text-emerald-700">Within 7-Day Refund Window</p>
                                    <p class="mt-0.5 text-xs text-emerald-600">Full refund eligible - no late-processing tax applied.</p>
                                </div>
                            </div>
                        </template>

                        <label class="mb-2 block text-sm font-bold text-slate-700">
                            Refund Amount (Rs.)
                        </label>

                        <input
                            v-model.number="refundAmountInput"
                            type="number"
                            min="0"
                            step="0.01"
                            placeholder="Enter refund amount..."
                            :disabled="!canManageRequests"
                            class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10 disabled:bg-slate-100 disabled:text-slate-500" />

                        <p class="mt-1 text-xs text-slate-400">
                            Auto-calculated based on the 7-day refund policy. You may override if needed.
                        </p>

                    </div>

                    <!-- Admin Note -->

                    <div class="mt-5">

                        <label class="mb-2 block text-sm font-bold text-slate-700">

                            Admin Note

                        </label>

                        <textarea v-model="adminNote" rows="3"
                            placeholder="Add an approval, rejection or refund note..."
                            :disabled="!canManageRequests || selectedRequest.status === 'COMPLETED' || selectedRequest.status === 'REJECTED'"
                            class="w-full resize-none rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10 disabled:bg-slate-100 disabled:text-slate-500">
                        </textarea>

                    </div>

                    <!-- Read-Only Banner for non-support staff -->
                    <div v-if="!canManageRequests" class="mt-5 flex items-center gap-3 rounded-xl border border-blue-200 bg-blue-50/80 px-4 py-3 text-xs font-semibold text-blue-800">
                        <svg class="h-4 w-4 shrink-0 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        <span>Read-Only View: Only Support Staff and Administrators can approve or reject return requests.</span>
                    </div>

                    <!-- Current Status -->

                    <div
                        class="mt-5 flex items-center justify-between rounded-2xl border border-slate-200 bg-slate-50/70 px-4 py-3">

                        <span class="text-sm font-bold text-slate-600">

                            Current Status

                        </span>

                        <span :class="[
                            'rounded-full border px-3 py-1.5 text-xs font-bold',
                            getStatusClass(selectedRequest.status)
                        ]">

                            {{ statusLabel(selectedRequest.status) }}

                        </span>

                    </div>

                    <!-- Handled By info for already processed requests -->
                    <div v-if="selectedRequest.supportStaffId && (selectedRequest.status === 'COMPLETED' || selectedRequest.status === 'REJECTED')"
                        class="mt-3 flex items-center justify-between rounded-2xl border border-slate-200 bg-slate-50/70 px-4 py-3">

                        <span class="text-sm font-bold text-slate-600">
                            Handled By
                        </span>

                        <span class="font-mono text-xs font-bold text-slate-800 bg-white border border-slate-200 rounded-lg px-2.5 py-1">
                            {{ selectedRequest.supportStaffId }}
                        </span>

                    </div>

                    <!-- =================================================
                         ACTIONS
                         ================================================= -->

                    <div class="mt-6 flex flex-col gap-3 border-t border-slate-200/70 pt-6 sm:flex-row sm:justify-end">

                        <button type="button" @click="closeReviewModal"
                            class="rounded-xl border border-slate-200 bg-white px-5 py-3 text-sm font-bold text-slate-600 transition hover:bg-slate-50">

                            Close

                        </button>

                        <!-- Reject -->

                        <button v-if="canManageRequests && selectedRequest.status === 'REQUESTED'" type="button" :disabled="isProcessing"
                            @click="rejectRequest(selectedRequest)"
                            class="rounded-xl border border-red-200 bg-red-50 px-5 py-3 text-sm font-bold text-red-600 transition hover:bg-red-100 disabled:opacity-50">

                            Reject Request

                        </button>

                        <!-- Approve -->

                        <button v-if="canManageRequests && selectedRequest.status === 'REQUESTED'" type="button" :disabled="isProcessing"
                            @click="approveRequest(selectedRequest)"
                            class="rounded-xl bg-gradient-to-r from-emerald-600 to-green-500 px-5 py-3 text-sm font-bold text-white shadow-lg shadow-emerald-500/20 transition hover:-translate-y-0.5 hover:shadow-xl disabled:opacity-50">

                            <span v-if="!isProcessing">
                                Approve Request
                            </span>

                            <span v-else class="flex items-center gap-2">

                                <svg class="h-4 w-4 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">

                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                        d="M12 4v2m0 12v2m8-8h-2M6 12H4m13.657-5.657l-1.414 1.414M7.757 16.243l-1.414 1.414m0-11.314L7.757 7.757m8.486 8.486l1.414 1.414" />

                                </svg>

                                Processing...

                            </span>

                        </button>

                        <!-- Complete -->

                        <button v-if="canManageRequests && selectedRequest.status === 'APPROVED'" type="button" :disabled="isProcessing"
                            @click="markAsCompleted(selectedRequest)"
                            class="rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-5 py-3 text-sm font-bold text-white shadow-lg shadow-blue-500/20 transition hover:-translate-y-0.5 hover:shadow-xl disabled:opacity-50">

                            <span v-if="!isProcessing">
                                Mark as Completed
                            </span>

                            <span v-else>
                                Processing...
                            </span>

                        </button>

                    </div>

                </div>

            </div>

        </Transition>

        <!-- =====================================================
             CONFIRMATION MODAL (ORDER MANAGEMENT DESIGN)
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
.modal-enter-active,
.modal-leave-active {
    transition: opacity 0.25s ease;
}

.modal-enter-from,
.modal-leave-to {
    opacity: 0;
}
</style>
