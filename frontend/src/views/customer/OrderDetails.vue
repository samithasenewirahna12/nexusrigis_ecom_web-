<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../../services/api'

const route = useRoute()
const router = useRouter()

// =========================================================
// STATE
// =========================================================

const loading = ref(true)
const errorMessage = ref('')
const order = ref<any>(null)
const delivery = ref<any>(null)

// =========================================================
// ORDER ID / SUCCESS
// =========================================================

const orderId = computed(() => String(route.params.id || ''))
const isNewOrder = computed(() => route.query.success === 'true')

// =========================================================
// HELPERS
// =========================================================

function getProductName(item: any): string {
    return item?.productName || item?.name || item?.product?.name || 'Product'
}

function getProductPrice(item: any): number {
    const price = item?.unitPrice ?? item?.price ?? item?.productPrice ?? item?.product?.price ?? 0
    const value = Number(price)
    return Number.isFinite(value) ? value : 0
}

function getQuantity(item: any): number {
    const quantity = Number(item?.quantity)
    return Number.isFinite(quantity) && quantity > 0 ? quantity : 1
}

function getItemTotal(item: any): number {
    return getProductPrice(item) * getQuantity(item)
}

function formatMoney(value: any): string {
    const amount = Number(value)
    if (!Number.isFinite(amount)) return '0.00'
    return amount.toFixed(2)
}

function formatDate(value: any): string {
    if (!value) return '-'
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return String(value)
    return date.toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })
}

// =========================================================
// ORDER ITEMS / TOTALS
// =========================================================

const orderItems = computed(() => {
    if (!order.value) return []
    return order.value.items || order.value.orderItems || []
})

const subtotal = computed(() => {
    let total = 0
    for (const item of orderItems.value) total += getItemTotal(item)
    return total
})

const orderTotal = computed(() => {
    const possibleValues = [order.value?.totalAmount, order.value?.total, order.value?.grandTotal]
    for (const value of possibleValues) {
        const amount = Number(value)
        if (Number.isFinite(amount)) return amount
    }
    return subtotal.value
})

// =========================================================
// STATUS
// =========================================================

const orderStatus = computed(() => order.value?.status || order.value?.orderStatus || 'Processing')

function statusClass(status: string): string {
    const value = status.toLowerCase()
    if (value.includes('deliver')) return 'bg-emerald-50 text-emerald-700 border-emerald-200'
    if (value.includes('cancel')) return 'bg-red-50 text-red-700 border-red-200'
    if (value.includes('complete')) return 'bg-blue-50 text-blue-700 border-blue-200'
    return 'bg-amber-50 text-amber-700 border-amber-200'
}

// =========================================================
// TRACKING TIMELINE
// =========================================================

const normalizeStatusKey = (raw: string | null | undefined): string => {
    if (!raw) return 'PENDING'
    const clean = String(raw).trim().toUpperCase().replace(/-/g, '_').replace(/ /g, '_').replace(/&/g, 'AND')
    if (clean === 'PLACED') return 'PENDING'
    return clean
}

const STEP_BY_STATUS: Record<string, number> = {
    PENDING: 1,
    ORDER_CONFIRMED: 2,
    PICKED_AND_PACKED: 3,
    IN_TRANSIT: 4,
    OUT_FOR_DELIVERY: 5,
    DELIVERED: 6,
    CANCELLED: 1,
    PAID: 1
}

const trackingTimeline = computed(() => {
    const raw = normalizeStatusKey(delivery.value?.status || order.value?.status)
    const currentStep = STEP_BY_STATUS[raw] ?? 1

    return [
        {
            id: 1,
            label: 'Pending',
            description: 'Order placed successfully and awaiting confirmation.',
            completed: currentStep >= 1,
            current: currentStep === 0
        },
        {
            id: 2,
            label: 'Order Confirmed',
            description: 'Your order has been received and confirmed.',
            completed: currentStep >= 2,
            current: currentStep === 1
        },
        {
            id: 3,
            label: 'Picked & Packed',
            description: 'Items verified and packed for shipment.',
            completed: currentStep >= 3,
            current: currentStep === 2
        },
        {
            id: 4,
            label: 'In Transit',
            description: delivery.value?.deliveryStaffName
                ? `Assigned to: ${delivery.value.deliveryStaffName}`
                : 'Shipment dispatched to delivery team.',
            completed: currentStep >= 4,
            current: currentStep === 3
        },
        {
            id: 5,
            label: 'Out for Delivery',
            description: 'Your order is out for doorstep delivery.',
            completed: currentStep >= 5,
            current: currentStep === 4
        },
        {
            id: 6,
            label: 'Delivered',
            description: 'Package delivered successfully.',
            completed: currentStep >= 6,
            current: currentStep === 5
        }
    ]
})

// =========================================================
// LOAD ORDER (order + delivery fetched in parallel)
// =========================================================

async function loadOrder() {
    loading.value = true
    errorMessage.value = ''

    try {
        if (!orderId.value) {
            errorMessage.value = 'Order ID was not found.'
            return
        }

        const [orderRes, deliveryData] = await Promise.all([
            api.get(`/orders/${orderId.value}`),
            // delivery may not exist yet, so a failure here is fine
            api
                .get(`/deliveries/order/${orderId.value}`)
                .then((r) => r.data)
                .catch(() => null)
        ])

        order.value = orderRes.data
        delivery.value = deliveryData
    } catch (error: any) {
        console.error('Order details error:', error)
        errorMessage.value =
            error?.response?.data?.message || error?.response?.data?.error || 'Unable to load order details.'
    } finally {
        loading.value = false
    }
}

// =========================================================
// NAVIGATION
// =========================================================

function goHome() {
    router.push('/')
}

function goOrders() {
    router.push('/orders')
}

function goCatalog() {
    router.push('/catalog')
}

function goBack() {
    router.back()
}

onMounted(() => {
    loadOrder()
})
</script>

<template>
    <div class="min-h-screen bg-slate-50 py-8 sm:py-10">

        <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">

            <!-- =================================================
           TOP NAVIGATION
      ================================================== -->

            <div class="flex flex-wrap items-center justify-between gap-4 mb-6">

                <button type="button" class="inline-flex items-center gap-2
                 px-4 py-2.5
                 rounded-xl
                 bg-white
                 border border-slate-200
                 text-sm font-semibold
                 text-slate-600
                 hover:text-blue-600
                 hover:border-blue-300
                 transition" @click="goBack">
                    <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
                    </svg>

                    Back
                </button>

                <div class="flex items-center gap-2">

                    <button type="button" class="px-4 py-2.5 rounded-xl
                   bg-white
                   border border-slate-200
                   text-sm font-semibold
                   text-slate-600
                   hover:border-blue-300
                   hover:text-blue-600
                   transition" @click="goHome">
                        Home
                    </button>

                    <button type="button" class="px-4 py-2.5 rounded-xl
                   bg-blue-600
                   text-white
                   text-sm font-bold
                   hover:bg-blue-500
                   transition
                   shadow-sm" @click="goOrders">
                        My Orders
                    </button>

                </div>

            </div>

            <!-- =================================================
           SUCCESS
      ================================================== -->

            <div v-if="isNewOrder" class="mb-6 rounded-3xl
               bg-gradient-to-r
               from-emerald-50
               to-cyan-50
               border border-emerald-100
               p-5 sm:p-6">

                <div class="flex items-start gap-4">

                    <div class="w-11 h-11 shrink-0
                   rounded-2xl
                   bg-emerald-500
                   text-white
                   flex items-center justify-center">
                        <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                        </svg>
                    </div>

                    <div>

                        <h1 class="text-xl sm:text-2xl font-black text-slate-800">
                            Order placed successfully!
                        </h1>

                        <p class="text-sm text-slate-500 mt-1">
                            Thank you for your purchase.
                            Your order has been successfully created.
                        </p>

                    </div>

                </div>

            </div>

            <!-- =================================================
           LOADING
      ================================================== -->

            <div v-if="loading" class="bg-white rounded-3xl
               p-12
               border border-slate-100
               shadow-sm
               text-center">

                <div class="w-12 h-12
                 mx-auto
                 rounded-full
                 border-4
                 border-blue-100
                 border-t-blue-600
                 animate-spin"></div>

                <p class="mt-4 text-sm font-semibold text-slate-500">
                    Loading order details...
                </p>

            </div>

            <!-- =================================================
           ERROR
      ================================================== -->

            <div v-else-if="errorMessage" class="bg-white
               rounded-3xl
               p-8
               border border-red-100
               shadow-sm
               text-center">

                <div class="w-14 h-14
                 mx-auto
                 rounded-2xl
                 bg-red-50
                 text-red-500
                 flex items-center justify-center">

                    <svg class="w-7 h-7" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round"
                            d="M12 9v2m0 4h.01M5.5 20h13a2 2 0 001.73-3L13.73 4a2 2 0 00-3.46 0l-6.5 13A2 2 0 005.5 20z" />
                    </svg>

                </div>

                <h2 class="mt-4 text-lg font-black text-slate-800">
                    Unable to load order
                </h2>

                <p class="mt-2 text-sm text-slate-500">
                    {{ errorMessage }}
                </p>

                <div class="flex flex-wrap justify-center gap-3 mt-6">

                    <button type="button" class="px-5 py-3
                   rounded-xl
                   bg-blue-600
                   text-white
                   text-sm
                   font-bold" @click="loadOrder">
                        Try Again
                    </button>

                    <button type="button" class="px-5 py-3
                   rounded-xl
                   bg-slate-100
                   text-slate-700
                   text-sm
                   font-bold" @click="goOrders">
                        My Orders
                    </button>

                </div>

            </div>

            <!-- =================================================
           ORDER CONTENT
      ================================================== -->

            <div v-else-if="order" class="space-y-6">

                <!-- ORDER HEADER -->
                <div class="bg-white
                 rounded-3xl
                 border border-slate-100
                 shadow-lg
                 shadow-slate-200/40
                 p-6 sm:p-7">

                    <div class="flex flex-col
                   sm:flex-row
                   sm:items-center
                   sm:justify-between
                   gap-5">

                        <div>

                            <p class="text-xs uppercase tracking-wider font-bold text-slate-400">
                                Order Details
                            </p>

                            <h2 class="text-2xl sm:text-3xl font-black text-slate-800 mt-1">
                                #{{ orderId }}
                            </h2>

                            <p class="text-sm text-slate-400 mt-2">
                                Ordered
                                {{ formatDate(order.orderDate || order.createdAt || order.date) }}
                            </p>

                        </div>

                        <div class="flex flex-col sm:items-end gap-3">

                            <span class="inline-flex
                       items-center
                       px-4 py-2
                       rounded-full
                       border
                       text-xs
                       font-bold" :class="statusClass(orderStatus)">
                                {{ orderStatus }}
                            </span>

                            <p class="text-2xl
                       font-black
                       bg-gradient-to-r
                       from-blue-600
                       to-cyan-500
                       bg-clip-text
                       text-transparent">
                                LKR {{ formatMoney(orderTotal) }}
                            </p>

                        </div>

                    </div>

                </div>

                <!-- ORDER TRACKING TIMELINE -->
                <div class="bg-white
                 rounded-3xl
                 border border-slate-100
                 shadow-lg
                 shadow-slate-200/40
                 p-6 sm:p-7">

                    <div class="flex items-center justify-between mb-6">
                        <div>
                            <h3 class="text-lg font-black text-slate-800">Order Tracking</h3>
                            <p class="text-xs text-slate-400 mt-1">Live step-by-step status</p>
                        </div>
                        <router-link :to="`/orders/track?orderId=${orderId}`"
                            class="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-blue-50 border border-blue-200 text-xs font-bold text-blue-700 hover:bg-blue-100 transition">
                            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                    d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
                            </svg>
                            Full Tracking
                        </router-link>
                    </div>

                    <!-- Timeline Steps -->
                    <div class="relative">

                        <!-- Connecting Line -->
                        <div class="absolute left-5 top-5 bottom-5 w-0.5 bg-slate-100" aria-hidden="true"></div>

                        <div class="space-y-4">

                            <div v-for="step in trackingTimeline" :key="step.id"
                                class="relative flex items-start gap-4">

                                <!-- Step Icon -->
                                <div :class="[
                                    'relative z-10 w-10 h-10 rounded-full flex items-center justify-center text-sm font-black shrink-0 border-2',
                                    step.completed
                                        ? 'bg-blue-600 border-blue-600 text-white shadow-md shadow-blue-500/25'
                                        : step.current
                                            ? 'bg-amber-400 border-amber-400 text-white shadow-md shadow-amber-400/25 animate-pulse'
                                            : 'bg-white border-slate-200 text-slate-300'
                                ]">
                                    <span v-if="step.completed">✓</span>
                                    <span v-else-if="step.current">{{ step.id }}</span>
                                    <span v-else class="text-xs">{{ step.id }}</span>
                                </div>

                                <!-- Step Content -->
                                <div class="flex-1 pb-1 pt-1.5">
                                    <p :class="[
                                        'text-sm font-bold',
                                        step.completed
                                            ? 'text-slate-900'
                                            : step.current
                                                ? 'text-amber-700'
                                                : 'text-slate-400'
                                    ]">
                                        {{ step.label }}
                                        <span v-if="step.current"
                                            class="ml-2 inline-flex items-center px-1.5 py-0.5 rounded-md bg-amber-100 text-amber-700 text-[9px] font-bold uppercase tracking-wider">
                                            Current
                                        </span>
                                    </p>
                                    <p class="text-xs text-slate-400 mt-0.5">{{ step.description }}</p>
                                </div>

                            </div>

                        </div>

                    </div>

                </div>

                <!-- QUICK ACTIONS -->
                <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">

                    <button type="button" class="bg-white
                   rounded-2xl
                   border border-slate-100
                   p-4
                   flex items-center
                   gap-3
                   text-left
                   hover:border-blue-300
                   hover:shadow-md
                   transition" @click="goHome">

                        <div class="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
                            <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"
                                stroke-width="1.8">
                                <path stroke-linecap="round" stroke-linejoin="round"
                                    d="M3 10.5L12 3l9 7.5V21a1 1 0 01-1 1H4a1 1 0 01-1-1v-10.5z" />
                                <path stroke-linecap="round" stroke-linejoin="round" d="M9 22v-7h6v7" />
                            </svg>
                        </div>

                        <div>
                            <p class="text-sm font-bold text-slate-800">Home</p>
                            <p class="text-xs text-slate-400">Back to homepage</p>
                        </div>

                    </button>

                    <button type="button" class="bg-white
                   rounded-2xl
                   border border-slate-100
                   p-4
                   flex items-center
                   gap-3
                   text-left
                   hover:border-blue-300
                   hover:shadow-md
                   transition" @click="goOrders">

                        <div class="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
                            <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"
                                stroke-width="1.8">
                                <path stroke-linecap="round" stroke-linejoin="round" d="M5 4h14v17H5z" />
                                <path stroke-linecap="round" stroke-linejoin="round" d="M8 8h8M8 12h8M8 16h5" />
                            </svg>
                        </div>

                        <div>
                            <p class="text-sm font-bold text-slate-800">My Orders</p>
                            <p class="text-xs text-slate-400">View all orders</p>
                        </div>

                    </button>

                    <button type="button" class="bg-white
                   rounded-2xl
                   border border-slate-100
                   p-4
                   flex items-center
                   gap-3
                   text-left
                   hover:border-blue-300
                   hover:shadow-md
                   transition" @click="goCatalog">

                        <div class="w-10 h-10 rounded-xl bg-cyan-50 text-cyan-600 flex items-center justify-center">
                            <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"
                                stroke-width="1.8">
                                <path stroke-linecap="round" stroke-linejoin="round"
                                    d="M4 5h6v6H4zM14 5h6v6h-6zM4 15h6v6H4zM14 15h6v6h-6z" />
                            </svg>
                        </div>

                        <div>
                            <p class="text-sm font-bold text-slate-800">Continue Shopping</p>
                            <p class="text-xs text-slate-400">Browse products</p>
                        </div>

                    </button>

                </div>

                <!-- MAIN GRID -->
                <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">

                    <!-- PRODUCTS -->
                    <div class="lg:col-span-2
                   bg-white
                   rounded-3xl
                   border border-slate-100
                   shadow-lg
                   shadow-slate-200/40
                   p-6">

                        <div class="flex items-center justify-between mb-5">

                            <div>

                                <h3 class="text-lg font-black text-slate-800">
                                    Ordered Items
                                </h3>

                                <p class="text-xs text-slate-400 mt-1">
                                    {{ orderItems.length }}
                                    item(s)
                                </p>

                            </div>

                        </div>

                        <div class="space-y-3">

                            <div v-for="(item, index) in orderItems" :key="String(item?.productId || item?.id || index)"
                                class="flex items-center
                       justify-between
                       gap-4
                       p-4
                       rounded-2xl
                       bg-slate-50
                       border border-slate-100">

                                <div class="flex items-center gap-3 min-w-0">

                                    <div class="w-11 h-11
                           shrink-0
                           rounded-xl
                           bg-blue-100
                           text-blue-600
                           flex items-center
                           justify-center
                           text-xs
                           font-black">
                                        {{ getQuantity(item) }}x
                                    </div>

                                    <div class="min-w-0">

                                        <p class="text-sm font-bold text-slate-800 truncate">
                                            {{ getProductName(item) }}
                                        </p>

                                        <p class="text-xs text-slate-400 mt-1">
                                            LKR {{ formatMoney(getProductPrice(item)) }}
                                            each
                                        </p>

                                    </div>

                                </div>

                                <p class="text-sm font-black text-slate-800 shrink-0">
                                    LKR {{ formatMoney(getItemTotal(item)) }}
                                </p>

                            </div>

                            <div v-if="orderItems.length === 0" class="py-8 text-center text-sm text-slate-400">
                                No order items available.
                            </div>

                        </div>

                    </div>

                    <!-- SUMMARY -->
                    <div class="bg-white
                   rounded-3xl
                   border border-slate-100
                   shadow-lg
                   shadow-slate-200/40
                   p-6
                   h-fit">

                        <h3 class="text-lg font-black text-slate-800 mb-5">
                            Payment Summary
                        </h3>

                        <div class="space-y-4 text-sm">

                            <div class="flex justify-between text-slate-500">
                                <span>Subtotal</span>

                                <span class="font-semibold text-slate-700">
                                    LKR {{ formatMoney(subtotal) }}
                                </span>
                            </div>

                            <div class="flex justify-between text-slate-500">
                                <span>Shipping</span>

                                <span class="font-bold text-emerald-600">
                                    FREE
                                </span>
                            </div>

                            <div v-if="order?.discountAmount || order?.couponDiscount"
                                class="flex justify-between text-emerald-600">
                                <span>Discount</span>

                                <span class="font-bold">
                                    -LKR {{ formatMoney(order?.discountAmount ?? order?.couponDiscount) }}
                                </span>
                            </div>

                        </div>

                        <div class="border-t border-slate-200 mt-5 pt-5 flex items-end justify-between">

                            <div>

                                <p class="text-xs uppercase tracking-wider text-slate-400">
                                    Total
                                </p>

                                <p class="text-xs text-slate-400 mt-1">
                                    Payment completed
                                </p>

                            </div>

                            <span class="text-2xl
                       font-black
                       bg-gradient-to-r
                       from-blue-600
                       to-cyan-500
                       bg-clip-text
                       text-transparent">
                                LKR {{ formatMoney(orderTotal) }}
                            </span>

                        </div>

                    </div>

                </div>

                <!-- BOTTOM NAVIGATION -->
                <div class="flex flex-col sm:flex-row justify-center gap-3 pt-2">

                    <button type="button" class="px-6 py-3
                   rounded-xl
                   bg-white
                   border border-slate-200
                   text-sm
                   font-bold
                   text-slate-700
                   hover:border-blue-300
                   hover:text-blue-600
                   transition" @click="goHome">
                        Back to Home
                    </button>

                    <button type="button" class="px-6 py-3
                   rounded-xl
                   bg-blue-600
                   text-white
                   text-sm
                   font-bold
                   hover:bg-blue-500
                   shadow-lg
                   shadow-blue-500/20
                   transition" @click="goOrders">
                        View My Orders
                    </button>

                    <button type="button" class="px-6 py-3
                   rounded-xl
                   bg-gradient-to-r
                   from-blue-600
                   to-cyan-500
                   text-white
                   text-sm
                   font-bold
                   hover:from-blue-500
                   hover:to-cyan-400
                   transition" @click="goCatalog">
                        Continue Shopping
                    </button>

                </div>

            </div>

        </div>

    </div>
</template>