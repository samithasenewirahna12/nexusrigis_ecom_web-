<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import api from '../../services/api'
import backgroundImage from '../../assets/images/image12.png'
import { usePopup } from '../../composables/usePopup'

// =========================================================
// TYPES - BACKEND RESPONSE
// =========================================================

interface ApiOrderItem {
  lineNo: number
  productId: string
  productName: string
  quantity: number
  unitPrice: number
}

interface ApiOrder {
  orderId: string
  orderDate: string
  status: string
  totalAmount: number
  customerId: string
  items: ApiOrderItem[]
  couponCodes: string[]
}

// =========================================================
// FRONTEND TYPES
// =========================================================

interface OrderItem {
  lineNo: number
  productId: string
  name: string
  price: number
  quantity: number
}

interface Order {
  id: string
  date: string
  status: string
  totalAmount: number
  customerId: string
  items: OrderItem[]
  couponCodes: string[]
}

// =========================================================
// STATE
// =========================================================

const orders = ref<Order[]>([])
const isLoading = ref(true)
const expandedOrderId = ref<string | null>(null)
const selectedStatusFilter = ref('All')
const showCancelModal = ref(false)
const selectedOrderForCancel = ref<Order | null>(null)
const isCancellingOrder = ref(false)
const cancelError = ref('')
const cancelSuccess = ref('')

// =========================================================
// FILTERS
// =========================================================

const statusFilters = [
  'All',
  'Pending',
  'Order Confirmed',
  'Picked & Packed',
  'In Transit',
  'Out for Delivery',
  'Delivered',
  'Cancelled'
]

// =========================================================
// NORMALIZE BACKEND STATUS
// =========================================================

function normalizeStatus(backendStatus: string): string {
  const status = String(backendStatus || '').trim().toUpperCase().replace(/-/g, '_').replace(/ /g, '_')
  switch (status) {
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
      return backendStatus || 'Pending'
  }
}

// =========================================================
// CONVERT API ORDER
// =========================================================

function convertOrder(apiOrder: ApiOrder): Order {
  const items: OrderItem[] = (apiOrder.items || []).map((item) => ({
    lineNo: Number(item.lineNo) || 0,
    productId: String(item.productId || ''),
    name: String(item.productName || 'Product'),
    price: Number(item.unitPrice) || 0,
    quantity: Number(item.quantity) || 0
  }))

  return {
    id: String(apiOrder.orderId || ''),
    date: String(apiOrder.orderDate || ''),
    status: normalizeStatus(apiOrder.status),
    totalAmount: Number(apiOrder.totalAmount) || 0,
    customerId: String(apiOrder.customerId || ''),
    items,
    couponCodes: Array.isArray(apiOrder.couponCodes) ? apiOrder.couponCodes : []
  }
}

// =========================================================
// GET CUSTOMER ID
// =========================================================

function getCustomerId(): string | null {
  const savedUser = sessionStorage.getItem('user')
  if (!savedUser) return null

  try {
    const user = JSON.parse(savedUser)
    const customerId = user?.customerId ?? user?.userId ?? user?.id
    return customerId ? String(customerId) : null
  } catch (error) {
    console.error('Failed to read logged-in user:', error)
    return null
  }
}

// =========================================================
// LOAD ORDERS
// =========================================================

async function loadOrders() {
  isLoading.value = true
  cancelError.value = ''
  cancelSuccess.value = ''

  try {
    const customerId = getCustomerId()

    const response = customerId
      ? await api.get('/orders', { params: { customerId } })
      : await api.get('/orders')

    if (!Array.isArray(response.data)) {
      orders.value = []
      return
    }

    orders.value = (response.data as ApiOrder[]).map(convertOrder)
  } catch (error) {
    console.error('Failed to fetch orders history:', error)
    orders.value = []
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadOrders()
})

// =========================================================
// FILTERED ORDERS + COUNTS (computed once, not per render call)
// =========================================================

const filteredOrders = computed(() => {
  if (selectedStatusFilter.value === 'All') return orders.value
  return orders.value.filter((order) => order.status === selectedStatusFilter.value)
})

const filterCounts = computed(() => {
  const counts: Record<string, number> = { All: orders.value.length }
  for (const order of orders.value) {
    counts[order.status] = (counts[order.status] || 0) + 1
  }
  return counts
})

function getFilterCount(status: string): number {
  return filterCounts.value[status] || 0
}

// =========================================================
// TOGGLE ORDER DETAILS
// =========================================================

function toggleOrderDetails(orderId: string) {
  expandedOrderId.value = expandedOrderId.value === orderId ? null : orderId
  cancelError.value = ''
}

// =========================================================
// STATUS BADGE / ICON
// =========================================================

function getStatusBadgeStyle(status: string) {
  switch (status) {
    case 'Paid':
      return 'bg-blue-50 border-blue-200 text-blue-700'
    case 'Processing':
      return 'bg-amber-50 border-amber-200 text-amber-700'
    case 'Shipped':
      return 'bg-indigo-50 border-indigo-200 text-indigo-700'
    case 'Delivered':
      return 'bg-emerald-50 border-emerald-200 text-emerald-700'
    case 'Cancelled':
      return 'bg-red-50 border-red-200 text-red-700'
    default:
      return 'bg-slate-50 border-slate-200 text-slate-600'
  }
}

function getStatusIcon(status: string) {
  switch (status) {
    case 'Paid':
      return '✓'
    case 'Processing':
      return '◷'
    case 'Shipped':
      return '→'
    case 'Delivered':
      return '✓'
    case 'Cancelled':
      return '×'
    default:
      return '•'
  }
}

// =========================================================
// TOTAL ITEMS
// =========================================================

function getTotalItems(order: Order) {
  let total = 0
  for (const item of order.items) total += Number(item.quantity) || 0
  return total
}

const popup = usePopup()

// =========================================================
// ORDER CANCELLATION (allowed while status is PAID)
// =========================================================

function canCancelOrder(order: Order): boolean {
  return order.status === 'Paid'
}

async function openCancelModal(order: Order) {
  if (!canCancelOrder(order)) {
    await popup.warning('This order can no longer be cancelled as it is already being processed.', 'Cannot Cancel')
    return
  }

  const confirmed = await popup.confirm({
    title: 'Cancel Order',
    message: `Are you sure you want to cancel order #${order.id}? This will cancel processing and issue a refund according to store policy.`,
    type: 'warning',
    confirmText: 'Yes, Cancel Order',
    cancelText: 'Keep Order'
  })

  if (!confirmed) return

  isCancellingOrder.value = true
  try {
    const response = await api.put(`/orders/${order.id}/cancel`)

    if (response.data && typeof response.data === 'object' && response.data.status) {
      order.status = normalizeStatus(response.data.status)
    } else {
      order.status = 'Cancelled'
    }

    expandedOrderId.value = order.id
    await popup.success(`Order #${order.id} was cancelled successfully.`, 'Order Cancelled')
  } catch (error: any) {
    console.error('Failed to cancel order:', error)
    const err =
      error?.response?.data?.message ||
      error?.response?.data?.error ||
      'Failed to cancel the order. Please try again.'
    await popup.error(err, 'Cancellation Failed')
  } finally {
    isCancellingOrder.value = false
  }
}
</script>

<template>
  <div class="relative min-h-screen">

    <!-- Background: fixed element (GPU-composited) instead of bg-fixed -->
    <div class="fixed inset-0 bg-cover bg-center will-change-transform pointer-events-none"
      :style="{ backgroundImage: `url(${backgroundImage})` }"></div>

    <!-- Overlay: no blur -->
    <div class="fixed inset-0 bg-white/60 pointer-events-none"></div>


    <!-- =====================================================
         MAIN
    ====================================================== -->

    <div class="relative z-10
             min-h-screen
             px-4 py-6
             sm:px-6
             lg:px-8
             lg:py-8">

      <div class="max-w-7xl mx-auto">

        <!-- =================================================
             HEADER
        ================================================== -->

        <section class="relative
                 overflow-hidden
                 rounded-3xl
                 bg-white/90
                 border border-white/90
                 shadow-xl
                 shadow-slate-200/40
                 p-5 sm:p-7
                 mb-5">

          <!-- Decorative gradients (cheap, no blur) -->
          <div class="absolute -right-16 -top-20 w-64 h-64 rounded-full pointer-events-none
                      bg-[radial-gradient(circle,rgba(96,165,250,0.15),transparent_70%)]"></div>

          <div class="absolute -left-20 -bottom-24 w-60 h-60 rounded-full pointer-events-none
                      bg-[radial-gradient(circle,rgba(34,211,238,0.15),transparent_70%)]"></div>


          <div class="relative z-10
                   flex flex-col
                   lg:flex-row
                   lg:items-center
                   lg:justify-between
                   gap-5">

            <!-- TITLE -->

            <div>

              <div class="inline-flex
                       items-center
                       gap-2
                       px-3 py-1.5
                       rounded-full
                       bg-blue-50
                       border border-blue-100
                       text-[10px]
                       font-bold
                       text-blue-600
                       uppercase
                       tracking-wider
                       mb-3">

                <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M6 2h12v20H6z" />
                  <path d="M9 6h6" />
                  <path d="M9 10h6" />
                  <path d="M9 14h4" />
                </svg>

                My Orders

              </div>

              <h1 class="text-2xl sm:text-3xl font-black text-slate-800">
                Order History
              </h1>

              <p class="mt-1.5 text-xs sm:text-sm text-slate-500">
                View your order details,
                track shipments,
                and cancel eligible orders.
              </p>

            </div>


            <!-- SUMMARY -->

            <div class="flex items-center gap-2">

              <div class="min-w-[85px]
                       px-3 py-2.5
                       rounded-xl
                       bg-white
                       border border-slate-200">

                <p class="text-[9px] uppercase font-bold tracking-wider text-slate-400">
                  Orders
                </p>

                <p class="mt-0.5 text-lg font-black text-slate-800">
                  {{ orders.length }}
                </p>

              </div>

              <div class="min-w-[85px]
                       px-3 py-2.5
                       rounded-xl
                       bg-blue-50
                       border border-blue-100">

                <p class="text-[9px] uppercase font-bold tracking-wider text-blue-500">
                  Showing
                </p>

                <p class="mt-0.5 text-lg font-black text-blue-600">
                  {{ filteredOrders.length }}
                </p>

              </div>

            </div>

          </div>

        </section>


        <!-- =================================================
             SUCCESS
        ================================================== -->

        <div v-if="cancelSuccess" class="mb-5
                 flex items-center gap-3
                 p-4
                 rounded-2xl
                 bg-emerald-50
                 border border-emerald-200
                 text-emerald-700">

          <div class="w-8 h-8
                   rounded-full
                   bg-emerald-100
                   flex items-center
                   justify-center">
            ✓
          </div>

          <span class="text-xs font-semibold">
            {{ cancelSuccess }}
          </span>

        </div>


        <!-- =================================================
             FILTER
        ================================================== -->

        <section class="mb-5
                 bg-white/90
                 border border-white/90
                 rounded-2xl
                 shadow-md
                 shadow-slate-200/30
                 p-3">

          <div class="flex items-center gap-2 overflow-x-auto">

            <div class="flex items-center gap-2 px-2 shrink-0">

              <svg class="w-4 h-4 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2">
                <path d="M4 6h16" />
                <path d="M7 12h10" />
                <path d="M10 18h4" />
              </svg>

              <span class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                Filter
              </span>

            </div>


            <button v-for="status in statusFilters" :key="status" type="button" @click="selectedStatusFilter = status"
              :class="[
                'flex items-center gap-1.5',
                'px-3 py-2',
                'rounded-xl',
                'text-[10px]',
                'font-semibold',
                'whitespace-nowrap',
                'border',
                'transition-colors',

                selectedStatusFilter === status
                  ? 'bg-gradient-to-r from-blue-600 to-cyan-500 text-white border-transparent shadow-md'
                  : 'bg-white text-slate-500 border-slate-200 hover:border-blue-200 hover:text-blue-600'
              ]">

              {{ status }}

              <span :class="[
                'min-w-[18px] h-[18px]',
                'px-1',
                'rounded-full',
                'flex items-center justify-center',
                'text-[8px] font-bold',

                selectedStatusFilter === status
                  ? 'bg-white/20 text-white'
                  : 'bg-slate-100 text-slate-400'
              ]">
                {{ getFilterCount(status) }}
              </span>

            </button>

          </div>

        </section>


        <!-- =================================================
             LOADING
        ================================================== -->

        <div v-if="isLoading" class="space-y-4">

          <div v-for="n in 3" :key="n" class="bg-white/90
                   rounded-2xl
                   p-5
                   animate-pulse">

            <div class="flex justify-between items-center">

              <div class="space-y-2">
                <div class="h-4 bg-slate-200 rounded w-32"></div>
                <div class="h-3 bg-slate-100 rounded w-24"></div>
              </div>

              <div class="h-7 bg-slate-200 rounded-full w-20"></div>

            </div>

          </div>

        </div>


        <!-- =================================================
             EMPTY
        ================================================== -->

        <div v-else-if="filteredOrders.length === 0" class="max-w-xl
                 mx-auto
                 text-center
                 py-16
                 px-6
                 bg-white/90
                 border border-white/90
                 rounded-3xl">

          <div class="w-16 h-16
                   bg-blue-50
                   border border-blue-100
                   rounded-2xl
                   flex items-center
                   justify-center
                   mx-auto mb-4">

            <svg class="w-8 h-8 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round"
                d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </svg>

          </div>

          <h2 class="text-lg font-bold text-slate-800">
            No Orders Found
          </h2>

          <p class="text-xs text-slate-500 mt-1 mb-5">
            You don't have any orders
            matching this filter.
          </p>

          <router-link to="/catalog" class="inline-flex
                   items-center gap-2
                   px-5 py-2.5
                   bg-gradient-to-r
                   from-blue-600
                   to-cyan-500
                   text-white
                   font-semibold
                   text-xs
                   rounded-xl">
            Start Shopping
          </router-link>

        </div>


        <!-- =================================================
             ORDER LIST
        ================================================== -->

        <div v-else class="space-y-4">

          <div v-for="order in filteredOrders" :key="order.id" class="overflow-hidden
                   bg-white/90
                   border border-white/90
                   rounded-2xl
                   shadow-lg
                   shadow-slate-200/30">

            <!-- HEADER -->

            <div class="p-4 sm:p-5
                     flex flex-col
                     sm:flex-row
                     sm:items-center
                     justify-between
                     gap-4">

              <div class="flex items-center gap-3">

                <div class="w-11 h-11
                         rounded-xl
                         bg-blue-50
                         border border-blue-100
                         flex items-center
                         justify-center
                         shrink-0">

                  <svg class="w-5 h-5 text-blue-600" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                    stroke-width="1.8">
                    <path d="M6 2h12v20H6z" />
                    <path d="M9 6h6" />
                    <path d="M9 10h6" />
                    <path d="M9 14h4" />
                  </svg>

                </div>

                <div>

                  <div class="flex flex-wrap items-center gap-2">

                    <span class="text-sm font-black text-slate-800">
                      {{ order.id }}
                    </span>

                    <span :class="[
                      'inline-flex items-center gap-1',
                      'text-[9px] font-bold',
                      'px-2 py-1',
                      'rounded-full border',
                      getStatusBadgeStyle(order.status)
                    ]">
                      <span>{{ getStatusIcon(order.status) }}</span>
                      {{ order.status }}
                    </span>

                  </div>

                  <p class="mt-1 text-[10px] text-slate-400">
                    Placed on
                    {{ order.date }}
                  </p>

                </div>

              </div>


              <!-- RIGHT -->

              <div class="flex items-center justify-between sm:justify-end gap-4">

                <div class="text-left sm:text-right">

                  <span class="block text-[9px] uppercase font-bold tracking-wider text-slate-400">
                    Total
                  </span>

                  <span class="text-base font-black text-slate-800">
                    LKR {{ order.totalAmount.toFixed(2) }}
                  </span>

                </div>

                <button type="button" @click="toggleOrderDetails(order.id)" :class="[
                  'inline-flex items-center gap-2',
                  'px-3.5 py-2.5',
                  'rounded-xl',
                  'text-[10px] font-semibold',
                  'border',

                  expandedOrderId === order.id
                    ? 'bg-blue-50 border-blue-200 text-blue-600'
                    : 'bg-white border-slate-200 text-slate-500'
                ]">

                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M3 12s3.5-6 9-6 9 6 9 6-3.5 6-9 6-9-6-9-6Z" />
                    <circle cx="12" cy="12" r="2.5" />
                  </svg>

                  {{ expandedOrderId === order.id ? 'Hide Details' : 'Order Details' }}

                </button>

              </div>

            </div>


            <!-- DETAILS -->

            <div v-if="expandedOrderId === order.id" class="border-t
                     border-slate-100
                     bg-slate-50
                     p-4 sm:p-5">

              <!-- ORDER DETAILS -->

              <div class="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-5">

                <div class="p-3 rounded-xl bg-white border border-slate-200">
                  <span class="block text-[9px] uppercase font-bold text-slate-400">
                    Order ID
                  </span>
                  <strong class="block mt-1 text-xs font-bold text-slate-700">
                    {{ order.id }}
                  </strong>
                </div>

                <div class="p-3 rounded-xl bg-white border border-slate-200">
                  <span class="block text-[9px] uppercase font-bold text-slate-400">
                    Date
                  </span>
                  <strong class="block mt-1 text-xs font-bold text-slate-700">
                    {{ order.date }}
                  </strong>
                </div>

                <div class="p-3 rounded-xl bg-white border border-slate-200">
                  <span class="block text-[9px] uppercase font-bold text-slate-400">
                    Customer
                  </span>
                  <strong class="block mt-1 text-xs font-bold text-slate-700">
                    {{ order.customerId }}
                  </strong>
                </div>

                <div class="p-3 rounded-xl bg-blue-50 border border-blue-100">
                  <span class="block text-[9px] uppercase font-bold text-blue-500">
                    Total
                  </span>
                  <strong class="block mt-1 text-xs font-black text-blue-600">
                    LKR {{ order.totalAmount.toFixed(2) }}
                  </strong>
                </div>

              </div>


              <!-- COUPONS -->

              <div v-if="order.couponCodes && order.couponCodes.length" class="mb-5
                       p-3
                       rounded-xl
                       bg-emerald-50
                       border border-emerald-100">

                <p class="text-[9px] uppercase font-bold text-emerald-600 mb-1">
                  Coupon Applied
                </p>

                <div class="flex flex-wrap gap-2">

                  <span v-for="coupon in order.couponCodes" :key="coupon" class="px-2.5 py-1
                           rounded-lg
                           bg-white
                           border border-emerald-200
                           text-[10px]
                           font-bold
                           text-emerald-700">
                    {{ coupon }}
                  </span>

                </div>

              </div>


              <!-- ITEMS HEADER -->

              <div class="flex items-center justify-between mb-3">

                <h4 class="text-[10px] font-bold text-slate-500 uppercase tracking-wider">
                  Ordered Items
                </h4>

                <span class="text-[9px] text-slate-400">
                  {{ getTotalItems(order) }}
                  item(s)
                </span>

              </div>


              <!-- ITEMS -->

              <div class="space-y-2">

                <div v-for="item in order.items" :key="item.lineNo" class="flex
                         items-center
                         justify-between
                         gap-3
                         p-3
                         bg-white
                         border border-slate-200/80
                         rounded-xl">

                  <div class="flex items-center gap-3 min-w-0">

                    <!-- ICON -->

                    <div class="w-11 h-11
                             rounded-lg
                             bg-slate-50
                             border border-slate-200
                             flex items-center
                             justify-center
                             shrink-0">

                      <svg class="w-5 h-5 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor"
                        stroke-width="1.5">
                        <path stroke-linecap="round" stroke-linejoin="round"
                          d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z" />
                      </svg>

                    </div>

                    <div class="min-w-0">

                      <h5 class="text-[11px] font-bold text-slate-700 truncate">
                        {{ item.name }}
                      </h5>

                      <span class="block mt-0.5 text-[9px] text-slate-400">
                        Product:
                        {{ item.productId }}
                      </span>

                      <span class="block mt-0.5 text-[9px] text-slate-400">
                        Qty:
                        {{ item.quantity }}
                        ×
                        LKR {{ item.price.toFixed(2) }}
                      </span>

                    </div>

                  </div>

                  <span class="text-xs font-black text-slate-700 shrink-0">
                    LKR {{ (item.price * item.quantity).toFixed(2) }}
                  </span>

                </div>

              </div>


              <!-- CANCEL AREA -->

              <div class="mt-5
                       pt-4
                       border-t
                       border-slate-200
                       flex
                       flex-col
                       sm:flex-row
                       sm:items-center
                       sm:justify-between
                       gap-3">

                <div>

                  <p v-if="canCancelOrder(order)" class="text-[10px] text-slate-400">
                    This order is still being
                    processed and can be cancelled.
                  </p>

                  <p v-else-if="order.status === 'Cancelled'" class="text-[10px] font-semibold text-red-500">
                    This order has been cancelled.
                  </p>

                  <p v-else class="text-[10px] text-slate-400">
                    This order can no longer
                    be cancelled.
                  </p>

                </div>

                <button v-if="canCancelOrder(order)" type="button" class="inline-flex
                         items-center
                         justify-center
                         gap-2
                         px-4 py-2.5
                         rounded-xl
                         bg-red-50
                         border border-red-200
                         text-red-600
                         text-[10px]
                         font-bold
                         hover:bg-red-100
                         transition-colors" @click="openCancelModal(order)">

                  <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round"
                      d="M6 7h12M9 7V4h6v3m-8 0v12a2 2 0 002 2h6a2 2 0 002-2V7M10 11v6M14 11v6" />
                  </svg>

                  Cancel Order

                </button>

              </div>

            </div>

          </div>

        </div>

      </div>

    </div>

  </div>
</template>