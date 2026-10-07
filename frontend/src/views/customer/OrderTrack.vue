<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../../services/api'
import backgroundImage from '../../assets/images/image12.png'

interface TrackingStatusStep {
  id: string
  label: string
  description: string
  timestamp: string | null
  completed: boolean
  current: boolean
}

interface OrderItem {
  id: string
  name: string
  category: string
  quantity: number
  price: number
  image: string
  sku: string
}

interface OrderDetails {
  orderNumber: string
  placedDate: string
  estimatedDelivery: string
  carrier: string
  trackingNumber: string
  status: 'pending' | 'processing' | 'shipped' | 'delivered' | 'delayed'
  shippingAddress: {
    name: string
    street: string
    cityStateZip: string
    country: string
  }
  paymentMethod: string
  subtotal: number
  shippingFee: number
  tax: number
  total: number
  items: OrderItem[]
  timeline: TrackingStatusStep[]
}

const route = useRoute()
const searchInput = ref('')
const isSearching = ref(false)
const hasSearched = ref(false)
const searchError = ref('')
const orderData = ref<OrderDetails | null>(null)

const FALLBACK_IMG =
  'https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=300&q=80'

// Cache product images so each product is only fetched once per page visit
const imageCache = new Map<string, string>()

function toImageSrc(img: string) {
  return img.startsWith('http') || img.startsWith('data:') ? img : `data:image/jpeg;base64,${img}`
}

async function loadProductImages(items: any[]) {
  const ids = [...new Set((items || []).map((i) => String(i.productId)).filter((id) => id && id !== 'undefined'))]
  await Promise.all(
    ids.map(async (id) => {
      if (imageCache.has(id)) return
      try {
        const { data: p } = await api.get(`/products/${id}`)
        const img = p?.images?.[0]
        if (img) imageCache.set(id, toImageSrc(img))
      } catch {
        // product images not critical
      }
    })
  )
}

async function fetchOrderData(queryId: string) {
  const cleanId = queryId.trim()
  if (!cleanId) return

  isSearching.value = true
  searchError.value = ''

  try {
    // 1. Fetch order by ID
    const orderRes = await api.get(`/orders/${cleanId}`)
    const order = orderRes.data

    if (!order || !order.orderId) {
      throw new Error(`Order #${cleanId} was not found.`)
    }

    // 2. Delivery, payment and product images in parallel
    const [delivery, payment] = await Promise.all([
      api.get(`/deliveries/order/${order.orderId}`).then((r) => r.data).catch(() => null),
      api.get('/payments', { params: { orderId: order.orderId } }).then((r) => r.data).catch(() => null),
      loadProductImages(order.items)
    ])

    // Map order items
    const mappedItems: OrderItem[] = Array.isArray(order.items)
      ? order.items.map((it: any, idx: number) => ({
        id: it.orderItemId || `item-${idx}`,
        name: it.productName || it.name || it.productId || 'Hardware Component',
        category: 'Hardware & Components',
        quantity: Number(it.quantity || 1),
        price: Number(it.unitPrice || it.price || 0),
        image: imageCache.get(String(it.productId)) || FALLBACK_IMG,
        sku: `SKU-${it.productId || idx}`
      }))
      : []

    const subtotal = mappedItems.reduce((acc, i) => acc + i.price * i.quantity, 0)
    const totalAmount = Number(order.totalAmount || subtotal)

    // A cancelled order must win over a stale delivery status
    const orderIsCancelled = /^cancel/i.test(String(order.status || ''))
    const cleanStatus = String((orderIsCancelled ? order.status : delivery?.status || order.status) || 'PENDING')
      .trim()
      .toUpperCase()
      .replace(/-/g, '_')
      .replace(/ /g, '_')
      .replace(/&/g, 'AND')

    let displayStatus: 'pending' | 'processing' | 'shipped' | 'delivered' | 'delayed' = 'pending'
    if (cleanStatus === 'DELIVERED') {
      displayStatus = 'delivered'
    } else if (['IN_TRANSIT', 'SHIPPED', 'DISPATCHED', 'OUT_FOR_DELIVERY'].includes(cleanStatus)) {
      displayStatus = 'shipped'
    } else if (['CANCELLED', 'CANCELED', 'FAILED'].includes(cleanStatus)) {
      displayStatus = 'delayed'
    } else if (
      ['ORDER_CONFIRMED', 'CONFIRMED', 'PICKED_AND_PACKED', 'PICKED_PACKED', 'PROCESSING'].includes(cleanStatus)
    ) {
      displayStatus = 'processing'
    }

    // Timeline milestone logic
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
    const currentStep = STEP_BY_STATUS[cleanStatus] ?? 1

    const timeline: TrackingStatusStep[] = [
      {
        id: 'step-1',
        label: 'Pending',
        description: 'Order placed successfully and awaiting confirmation.',
        timestamp: currentStep >= 1 ? order.orderDate || 'Placed' : null,
        completed: currentStep >= 1,
        current: currentStep === 0
      },
      {
        id: 'step-2',
        label: 'Order Confirmed',
        description: 'Payment registered and order placed in warehouse system.',
        timestamp: currentStep >= 2 ? (order.orderDate ? `${order.orderDate} • Confirmed` : 'Confirmed') : null,
        completed: currentStep >= 2,
        current: currentStep === 1
      },
      {
        id: 'step-3',
        label: 'Picked & Packed',
        description: 'Components verified, stress-tested, and packed for shipment.',
        timestamp: currentStep >= 3 ? 'Warehouse Processing' : null,
        completed: currentStep >= 3,
        current: currentStep === 2
      },
      {
        id: 'step-4',
        label: 'In Transit',
        description: delivery?.deliveryStaffName
          ? `Shipment assigned to delivery staff: ${delivery.deliveryStaffName}`
          : 'Shipment dispatched to regional courier network.',
        timestamp: currentStep >= 4 ? delivery?.assignedDate || 'In Transit' : null,
        completed: currentStep >= 4,
        current: currentStep === 3
      },
      {
        id: 'step-5',
        label: 'Out for Delivery',
        description: 'Dispatched for local doorstep delivery.',
        timestamp: currentStep >= 5 ? 'Out for Delivery' : null,
        completed: currentStep >= 5,
        current: currentStep === 4
      },
      {
        id: 'step-6',
        label: 'Delivered',
        description: 'Package delivered to recipient address.',
        timestamp: currentStep >= 6 ? delivery?.deliveryDate || 'Delivered' : null,
        completed: currentStep >= 6,
        current: currentStep === 5
      }
    ]

    orderData.value = {
      orderNumber: order.orderId,
      placedDate: order.orderDate || 'Recent',
      estimatedDelivery: delivery?.deliveryDate
        ? `Expected by ${delivery.deliveryDate}`
        : 'Estimated 3-5 business days',
      carrier: delivery?.deliveryStaffName ? `Staff: ${delivery.deliveryStaffName}` : 'Express Logistics',
      trackingNumber: delivery?.deliveryId || `TRK-${order.orderId}`,
      status: displayStatus,

      shippingAddress: {
        name: delivery?.customerName || 'Valued Customer',
        street: delivery?.address || 'Shipping Address',
        cityStateZip: `${delivery?.city || ''} ${delivery?.postalCode || ''}`.trim() || 'Sri Lanka',
        country: 'Sri Lanka'
      },

      paymentMethod: payment?.method || delivery?.paymentMethod || 'Cash on Delivery',
      subtotal,
      shippingFee: 0,
      tax: 0,
      total: totalAmount,
      items: mappedItems,
      timeline
    }

    hasSearched.value = true
  } catch (err: any) {
    console.error('Track Order Error:', err)
    searchError.value =
      err?.response?.data?.message || err?.message || `Unable to find order #${cleanId}. Please verify your order number.`
    orderData.value = null
    hasSearched.value = true
  } finally {
    isSearching.value = false
  }
}

function handleSearch() {
  if (!searchInput.value.trim()) return
  fetchOrderData(searchInput.value)
}

function clearSearch() {
  searchInput.value = ''
  searchError.value = ''
}

function getStatusClass(status: string) {
  if (status === 'delivered') return 'bg-emerald-50 text-emerald-700 border-emerald-200'
  if (status === 'delayed') return 'bg-amber-50 text-amber-700 border-amber-200'
  if (status === 'shipped') return 'bg-blue-50 text-blue-700 border-blue-200'
  if (status === 'pending') return 'bg-amber-50 text-amber-700 border-amber-200'
  return 'bg-slate-100 text-slate-600 border-slate-200'
}

// Number of completed milestones (shows "Step 6 / 6" once delivered)
const completedSteps = computed(() => {
  if (!orderData.value) return 0
  return orderData.value.timeline.filter((step) => step.completed).length
})

onMounted(async () => {
  if (route.query.orderId) {
    searchInput.value = String(route.query.orderId)
    await fetchOrderData(searchInput.value)
  } else {
    // Try to auto-load customer's latest order if logged in
    try {
      const userStr = sessionStorage.getItem('user')
      if (userStr) {
        const user = JSON.parse(userStr)
        const customerId = user.customerId || user.userId || user.id
        if (customerId) {
          const res = await api.get(`/orders/customer/${customerId}`)
          if (Array.isArray(res.data) && res.data.length > 0) {
            // newest order by date, not by whatever order the API happens to return
            const latest = [...res.data]
              .sort(
                (a: any, b: any) =>
                  String(a.orderDate || '').localeCompare(String(b.orderDate || '')) ||
                  String(a.orderId || '').localeCompare(String(b.orderId || ''))
              )
              .pop()
            if (latest?.orderId) {
              searchInput.value = latest.orderId
              await fetchOrderData(latest.orderId)
            }
          }
        }
      }
    } catch {
      // ignore fallback error
    }
  }
})
</script>

<template>
  <div class="relative min-h-screen">

    <!-- Background: fixed element (GPU-composited) instead of bg-fixed -->
    <div class="fixed inset-0 bg-cover bg-center will-change-transform pointer-events-none"
      :style="{ backgroundImage: `url(${backgroundImage})` }"></div>

    <!-- Overlay: no blur -->
    <div class="fixed inset-0 bg-white/60 pointer-events-none"></div>

    <!-- Main content -->
    <div class="relative z-10 min-h-screen px-4 py-6 sm:px-6 lg:px-8 lg:py-8">

      <div class="max-w-7xl mx-auto space-y-6">

        <!-- =====================================================
             HEADER / SEARCH
        ====================================================== -->
        <section class="relative overflow-hidden rounded-3xl
                 bg-white/90
                 border border-white/90
                 shadow-xl shadow-slate-200/40
                 p-5 sm:p-7">

          <!-- Decorative gradients (cheap, no blur) -->
          <div class="absolute -right-20 -top-20 w-64 h-64 rounded-full pointer-events-none
                      bg-[radial-gradient(circle,rgba(96,165,250,0.15),transparent_70%)]"></div>

          <div class="absolute -left-20 -bottom-20 w-56 h-56 rounded-full pointer-events-none
                      bg-[radial-gradient(circle,rgba(34,211,238,0.15),transparent_70%)]"></div>

          <div class="relative z-10">

            <!-- Label -->
            <div class="flex flex-wrap items-center gap-2 mb-3">

              <span class="inline-flex items-center gap-2
                       px-3 py-1.5 rounded-full
                       bg-blue-50
                       border border-blue-100
                       text-[10px] font-bold
                       text-blue-600 uppercase tracking-wider">
                <!-- Package icon -->
                <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="m16.5 9.4-9-5.19" />
                  <path
                    d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z" />
                  <polyline points="3.27 6.96 12 12.01 20.73 6.96" />
                  <line x1="12" y1="22.08" x2="12" y2="12" />
                </svg>

                Order Tracking
              </span>

              <span class="inline-flex items-center gap-1.5
                       text-[10px] font-semibold
                       text-emerald-600">
                <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
                Live Shipment Status
              </span>

            </div>

            <!-- Heading -->
            <h1 class="text-2xl sm:text-4xl
                     font-black text-slate-800
                     tracking-tight">
              Track Your Order
            </h1>

            <p class="mt-2 max-w-2xl
                     text-xs sm:text-sm
                     leading-relaxed
                     text-slate-500">
              Enter your order number or tracking code to check shipment
              progress, delivery updates and estimated arrival.
            </p>

            <!-- Search -->
            <form @submit.prevent="handleSearch" class="mt-5 flex flex-col sm:flex-row gap-2.5 max-w-2xl">

              <div class="relative flex-1">

                <svg class="absolute left-3.5 top-1/2
                         -translate-y-1/2
                         w-4 h-4 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                  stroke-width="2">
                  <circle cx="11" cy="11" r="8" />
                  <path d="m21 21-4.3-4.3" />
                </svg>

                <input v-model="searchInput" type="text" placeholder="Enter order number..." class="w-full
                         bg-white
                         border border-slate-200
                         focus:border-blue-400
                         focus:ring-4 focus:ring-blue-500/10
                         rounded-xl
                         pl-10 pr-10 py-3
                         text-xs sm:text-sm
                         text-slate-700
                         placeholder-slate-400
                         font-medium
                         focus:outline-none
                         transition" />

                <button v-if="searchInput" type="button" @click="clearSearch" class="absolute right-3 top-1/2
                         -translate-y-1/2
                         text-slate-400
                         hover:text-slate-700
                         transition">
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="18" y1="6" x2="6" y2="18" />
                    <line x1="6" y1="6" x2="18" y2="18" />
                  </svg>
                </button>

              </div>

              <button type="submit" :disabled="isSearching" class="inline-flex items-center justify-center
                       gap-2
                       px-5 py-3
                       rounded-xl
                       bg-gradient-to-r
                       from-blue-600 to-cyan-500
                       hover:from-blue-700 hover:to-cyan-600
                       text-white
                       text-xs font-bold
                       shadow-lg shadow-blue-500/20
                       transition-all
                       disabled:opacity-60
                       disabled:cursor-not-allowed
                       whitespace-nowrap">

                <svg v-if="!isSearching" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                  stroke-width="2">
                  <circle cx="11" cy="11" r="8" />
                  <path d="m21 21-4.3-4.3" />
                </svg>

                <svg v-else class="w-4 h-4 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                  stroke-width="2">
                  <circle cx="12" cy="12" r="9" class="opacity-30" />
                  <path d="M21 12a9 9 0 0 1-9 9" />
                </svg>

                {{ isSearching ? 'Searching...' : 'Track Package' }}

              </button>

            </form>

            <div v-if="searchError"
              class="mt-4 rounded-xl border border-red-200 bg-red-50 p-4 text-xs font-semibold text-red-700">
              {{ searchError }}
            </div>

            <div v-else-if="hasSearched && !orderData && !isSearching"
              class="mt-6 rounded-2xl border border-slate-200 bg-white/90 p-8 text-center shadow-lg">
              <div
                class="mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-full bg-amber-50 text-amber-600">
                <svg class="h-7 w-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
              </div>
              <h3 class="text-base font-bold text-slate-800">No Order Found</h3>
              <p class="mt-1 text-xs text-slate-500">Please verify your order ID and try again, or check your order
                history in your user profile.</p>
            </div>

          </div>
        </section>


        <!-- =====================================================
             ORDER CONTENT
        ====================================================== -->
        <div v-if="hasSearched && orderData" class="grid grid-cols-1 lg:grid-cols-3 gap-5 items-start">

          <!-- ===================================================
               LEFT CONTENT
          ==================================================== -->
          <div class="lg:col-span-2 space-y-5">

            <!-- STATUS SUMMARY -->
            <section class="bg-white/90
                     border border-white/90
                     rounded-2xl
                     shadow-lg shadow-slate-200/35
                     p-5">

              <div class="flex flex-col sm:flex-row
                       sm:items-center
                       justify-between
                       gap-4">

                <div>

                  <div class="flex flex-wrap items-center gap-2">

                    <span class="text-[10px]
                             font-mono font-bold
                             text-slate-400">
                      ORDER #{{ orderData.orderNumber }}
                    </span>

                    <span :class="[
                      'px-2.5 py-1 rounded-full border',
                      'text-[9px] font-bold uppercase tracking-wide',
                      getStatusClass(orderData.status)
                    ]">
                      {{ orderData.status }}
                    </span>

                  </div>

                  <h2 class="mt-1.5
                           text-base sm:text-lg
                           font-bold text-slate-800">
                    Estimated Delivery
                  </h2>

                  <p class="text-xs text-slate-500 mt-0.5">
                    {{ orderData.estimatedDelivery }}
                  </p>

                </div>


                <!-- Tracking -->
                <div class="rounded-xl
                         bg-slate-50
                         border border-slate-200
                         px-4 py-3
                         min-w-[230px]">

                  <div class="flex items-center gap-2
                           text-[9px]
                           font-bold
                           uppercase
                           tracking-wider
                           text-slate-400">

                    <!-- Truck icon -->
                    <svg class="w-3.5 h-3.5 text-blue-500" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                      stroke-width="2">
                      <rect x="1" y="3" width="15" height="13" />
                      <polygon points="16 8 20 8 23 11 23 16 16 16 16 8" />
                      <circle cx="5.5" cy="18.5" r="2.5" />
                      <circle cx="18.5" cy="18.5" r="2.5" />
                    </svg>

                    Carrier & Tracking

                  </div>

                  <p class="mt-1 text-xs
                           font-bold text-slate-700">
                    {{ orderData.carrier }}
                  </p>

                  <p class="mt-0.5
                           text-[10px]
                           font-mono
                           font-semibold
                           text-blue-600">
                    {{ orderData.trackingNumber }}
                  </p>

                </div>

              </div>
            </section>


            <!-- TIMELINE -->
            <section class="bg-white/90
                     border border-white/90
                     rounded-2xl
                     shadow-lg shadow-slate-200/35
                     p-5 sm:p-6">

              <div class="flex items-center
                       justify-between
                       border-b border-slate-100
                       pb-3 mb-6">

                <div class="flex items-center gap-2">

                  <div class="w-8 h-8 rounded-lg
                           bg-blue-50
                           flex items-center justify-center">
                    <svg class="w-4 h-4 text-blue-600" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                      stroke-width="2">
                      <path d="M12 6v6l4 2" />
                      <circle cx="12" cy="12" r="9" />
                    </svg>
                  </div>

                  <div>
                    <h3 class="text-xs font-bold
                             text-slate-800">
                      Delivery Progress
                    </h3>

                    <p class="text-[10px] text-slate-400">
                      Shipment milestone timeline
                    </p>
                  </div>

                </div>

                <span class="text-[10px]
                         font-mono
                         font-semibold
                         text-slate-400">
                  Step {{ completedSteps }} /
                  {{ orderData.timeline.length }}
                </span>

              </div>


              <!-- Timeline -->
              <div class="relative pl-9">

                <!-- Vertical line -->
                <div class="absolute left-[14px]
                         top-3 bottom-3
                         w-px bg-slate-200"></div>

                <div v-for="(step, idx) in orderData.timeline" :key="step.id" class="relative pb-7 last:pb-0">

                  <!-- Node -->
                  <div :class="[
                    'absolute -left-9 top-0',
                    'w-7 h-7 rounded-full',
                    'flex items-center justify-center',
                    'border-2 z-10',
                    step.completed
                      ? 'bg-blue-600 border-blue-600 text-white shadow-md shadow-blue-500/20'
                      : step.current
                        ? 'bg-white border-blue-500 text-blue-600 ring-4 ring-blue-500/10'
                        : 'bg-white border-slate-200 text-slate-400'
                  ]">

                    <!-- Completed -->
                    <svg v-if="step.completed" class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                      stroke-width="3">
                      <polyline points="20 6 9 17 4 12" />
                    </svg>

                    <!-- Upcoming -->
                    <span v-else class="text-[9px] font-bold">
                      {{ idx + 1 }}
                    </span>

                  </div>


                  <!-- Step content -->
                  <div class="rounded-xl
                           px-3 py-3
                           transition" :class="step.current
                            ? 'bg-blue-50/70 border border-blue-100'
                            : ''
                            ">

                    <div class="flex flex-col sm:flex-row
                             sm:items-center
                             justify-between gap-1.5">

                      <h4 :class="[
                        'text-xs font-bold',
                        step.completed || step.current
                          ? 'text-slate-800'
                          : 'text-slate-400'
                      ]">
                        {{ step.label }}
                      </h4>

                      <span v-if="step.timestamp" class="text-[9px]
                               font-mono
                               text-slate-400">
                        {{ step.timestamp }}
                      </span>

                    </div>

                    <p class="mt-1
                             text-[10px]
                             leading-relaxed
                             text-slate-500">
                      {{ step.description }}
                    </p>

                    <span v-if="step.current" class="inline-flex items-center gap-1.5
                             mt-2
                             text-[9px]
                             font-bold
                             text-blue-600">
                      <span class="w-1.5 h-1.5
                               rounded-full
                               bg-blue-500
                               animate-pulse"></span>
                      Current shipment status
                    </span>

                  </div>

                </div>

              </div>
            </section>


            <!-- =================================================
                 SHIPMENT ITEMS
            ================================================== -->
            <section class="bg-white/90
                     border border-white/90
                     rounded-2xl
                     shadow-lg shadow-slate-200/35
                     p-5">

              <div class="flex items-center justify-between
                       border-b border-slate-100
                       pb-3 mb-3">

                <div class="flex items-center gap-2">

                  <div class="w-8 h-8 rounded-lg
                           bg-cyan-50
                           flex items-center justify-center">
                    <svg class="w-4 h-4 text-cyan-600" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                      stroke-width="2">
                      <path d="m21 16-9 5-9-5" />
                      <path d="m21 12-9 5-9-5" />
                      <path d="m21 8-9 5-9-5 9-5 9 5Z" />
                    </svg>
                  </div>

                  <h3 class="text-xs font-bold
                           text-slate-800">
                    Shipment Contents
                  </h3>

                </div>

                <span class="text-[10px]
                         text-slate-400">
                  {{ orderData.items.length }} Items
                </span>

              </div>


              <div class="divide-y divide-slate-100">

                <div v-for="item in orderData.items" :key="item.id" class="py-3 first:pt-1 last:pb-0
                         flex items-center
                         justify-between gap-3">

                  <div class="flex items-center
                           gap-3 min-w-0">

                    <div class="w-14 h-14
                             rounded-xl
                             overflow-hidden
                             bg-slate-50
                             border border-slate-200
                             shrink-0">
                      <img :src="item.image" :alt="item.name" loading="lazy" decoding="async"
                        class="w-full h-full object-cover" />
                    </div>

                    <div class="min-w-0">

                      <span class="text-[9px]
                               font-bold
                               uppercase
                               tracking-wide
                               text-blue-600">
                        {{ item.category }}
                      </span>

                      <h4 class="mt-0.5
                               text-xs font-bold
                               text-slate-700
                               truncate">
                        {{ item.name }}
                      </h4>

                      <p class="mt-0.5
                               text-[9px]
                               font-mono
                               text-slate-400">
                        SKU: {{ item.sku }}
                        <span class="mx-1">•</span>
                        Qty: {{ item.quantity }}
                      </p>

                    </div>

                  </div>

                  <span class="text-xs
                           font-bold
                           text-slate-700
                           shrink-0">
                    LKR {{ (item.price * item.quantity).toLocaleString() }}
                  </span>

                </div>

              </div>
            </section>

          </div>


          <!-- ===================================================
               RIGHT SIDEBAR
          ==================================================== -->
          <aside class="lg:sticky lg:top-24
                   space-y-4">

            <!-- DELIVERY DETAILS -->
            <section class="bg-white/90
                     border border-white/90
                     rounded-2xl
                     shadow-lg shadow-slate-200/35
                     p-5">

              <div class="flex items-center gap-2
                       border-b border-slate-100
                       pb-3">

                <div class="w-8 h-8 rounded-lg
                         bg-emerald-50
                         flex items-center justify-center">
                  <svg class="w-4 h-4 text-emerald-600" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                    stroke-width="2">
                    <path d="M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0Z" />
                    <circle cx="12" cy="10" r="2.5" />
                  </svg>
                </div>

                <h3 class="text-xs font-bold
                         text-slate-800">
                  Delivery Details
                </h3>

              </div>


              <!-- Address -->
              <div class="py-4">

                <div class="flex items-center gap-1.5
                         text-[9px]
                         uppercase
                         tracking-wider
                         font-bold
                         text-slate-400">

                  <svg class="w-3.5 h-3.5 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                    stroke-width="2">
                    <path d="M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0Z" />
                    <circle cx="12" cy="10" r="2.5" />
                  </svg>

                  Destination Address

                </div>

                <p class="mt-2
                         text-xs font-bold
                         text-slate-700">
                  {{ orderData.shippingAddress.name }}
                </p>

                <p class="mt-1
                         text-[10px]
                         leading-relaxed
                         text-slate-500">
                  {{ orderData.shippingAddress.street }}<br />
                  {{ orderData.shippingAddress.cityStateZip }}<br />
                  {{ orderData.shippingAddress.country }}
                </p>

              </div>


              <div class="border-t border-slate-100"></div>


              <!-- Payment -->
              <div class="py-4">

                <div class="flex items-center gap-1.5
                         text-[9px]
                         uppercase
                         tracking-wider
                         font-bold
                         text-slate-400">

                  <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="2" y="5" width="20" height="14" rx="2" />
                    <line x1="2" y1="10" x2="22" y2="10" />
                  </svg>

                  Payment Summary

                </div>


                <div class="mt-3
                         space-y-2
                         text-[10px]">

                  <div class="flex justify-between
                           text-slate-500">
                    <span>Hardware Subtotal</span>
                    <span class="font-mono text-slate-700">
                      LKR {{ orderData.subtotal.toLocaleString() }}
                    </span>
                  </div>

                  <div class="flex justify-between
                           text-slate-500">
                    <span>Express Shipping</span>
                    <span class="font-bold
                             text-emerald-600">
                      FREE
                    </span>
                  </div>

                  <div class="flex justify-between
                           text-slate-500">
                    <span>Estimated Tax</span>
                    <span class="font-mono text-slate-700">
                      LKR {{ orderData.tax.toFixed(2) }}
                    </span>
                  </div>


                  <div class="pt-3 mt-2
                           border-t border-slate-100
                           flex justify-between
                           items-center">

                    <span class="text-xs
                             font-bold
                             text-slate-700">
                      Total Paid
                    </span>

                    <span class="text-lg
                             font-black
                             font-mono
                             text-blue-600">
                      LKR {{ orderData.total.toLocaleString() }}
                    </span>

                  </div>

                </div>

                <p class="mt-2
                         text-[9px]
                         text-slate-400">
                  Billed to {{ orderData.paymentMethod }}
                </p>

              </div>


              <div class="border-t border-slate-100"></div>


              <!-- Actions -->
              <div class="pt-4 space-y-2">

                <button type="button" class="w-full
                         flex items-center
                         justify-center gap-2
                         px-4 py-2.5
                         rounded-xl
                         border border-slate-200
                         bg-white
                         text-xs font-semibold
                         text-slate-600
                         hover:border-blue-200
                         hover:text-blue-600
                         hover:bg-blue-50/50
                         transition">

                  <!-- Download -->
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 3v12" />
                    <path d="m7 10 5 5 5-5" />
                    <path d="M5 21h14" />
                  </svg>

                  Download Invoice

                </button>


                <button type="button" class="w-full
                         flex items-center
                         justify-center gap-2
                         px-4 py-2.5
                         rounded-xl
                         border border-blue-100
                         bg-blue-50/60
                         text-xs font-semibold
                         text-blue-600
                         hover:bg-blue-100
                         transition">

                  <!-- Support -->
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="9" />
                    <path d="M8 14s1.5 2 4 2 4-2 4-2" />
                    <line x1="9" y1="9" x2="9.01" y2="9" />
                    <line x1="15" y1="9" x2="15.01" y2="9" />
                  </svg>

                  Contact Support

                </button>

              </div>

            </section>


            <!-- ORDER INFO -->
            <section class="bg-white/90
                     border border-white/90
                     rounded-2xl
                     p-4
                     shadow-md shadow-slate-200/30">

              <div class="flex items-center
                       justify-between">

                <div>

                  <p class="text-[9px]
                           uppercase
                           tracking-wider
                           font-bold
                           text-slate-400">
                    Order Placed
                  </p>

                  <p class="mt-1
                           text-xs font-semibold
                           text-slate-700">
                    {{ orderData.placedDate }}
                  </p>

                </div>

                <div class="w-9 h-9 rounded-xl
                         bg-blue-50
                         flex items-center justify-center">

                  <svg class="w-4 h-4 text-blue-600" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                    stroke-width="2">
                    <rect x="3" y="4" width="18" height="18" rx="2" />
                    <line x1="16" y1="2" x2="16" y2="6" />
                    <line x1="8" y1="2" x2="8" y2="6" />
                    <line x1="3" y1="10" x2="21" y2="10" />
                  </svg>

                </div>

              </div>

            </section>

          </aside>

        </div>

      </div>

    </div>
  </div>
</template>