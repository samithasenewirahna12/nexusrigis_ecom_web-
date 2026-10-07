<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminConfirmModal from '../../components/admin/AdminConfirmModal.vue'
import api from '../../services/api'

interface OrderItem {
  product: string
  sku: string
  quantity: number
  price: number
  image: string
}

interface Order {
  id: string
  orderNumber: string
  customerName: string
  customerEmail: string
  customerPhone: string
  customerId: string
  customerImage?: string
  date: string
  items: OrderItem[]
  subtotal: number
  shipping: number
  discount: number
  total: number
  paymentMethod: string
  paymentStatus: string
  status: string
  shippingAddress: string
  assignedStaffName?: string
  deliveryId?: string
}

const orders = ref<Order[]>([])
const isLoading = ref(false)
const loadError = ref('')
const assignSuccessMessage = ref('')

const PLACEHOLDER_ITEM_IMAGE =
  'https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=300&q=80'

const normalizeStatus = (raw: string | null | undefined) => {
  if (!raw) return 'Pending'
  const upper = String(raw).trim().toUpperCase().replace(/-/g, '_').replace(/ /g, '_')
  switch (upper) {
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

const getImageUrl = (imageStr: string | null | undefined): string => {
  if (!imageStr) return ''
  if (imageStr.startsWith('http')) return imageStr
  if (imageStr.startsWith('data:')) return imageStr
  return `data:image/jpeg;base64,${imageStr}`
}

const mapOrderDtoToOrder = (
  dto: any,
  payment: any | null,
  delivery: any | null = null,
  productImages: Map<string, string> = new Map()
): Order => {
  const items = (dto.items ?? []).map((item: any) => ({
    product:
      item.productName ??
      item.productId ??
      'Unknown product',

    sku: item.productId ?? '—',

    quantity: Number(item.quantity ?? 0),

    price: Number(item.unitPrice ?? 0),

    image: productImages.get(String(item.productId)) || PLACEHOLDER_ITEM_IMAGE
  }))

  const subtotal = items.reduce(
    (sum: number, item: OrderItem) =>
      sum + item.price * item.quantity,
    0
  )

  const total = Number(
    dto.totalAmount ?? subtotal
  )

  const discount = Math.max(
    0,
    Number((subtotal - total).toFixed(2))
  )

  const normalizedOrderStatus = normalizeStatus(dto.status)
  const staff = delivery?.deliveryStaffName || delivery?.staffName ||
    (['In Transit', 'Out for Delivery', 'Delivered'].includes(normalizedOrderStatus) ? 'Delivery Staff' : '')

  return {
    id: dto.orderId,
    orderNumber: dto.orderId,
    customerName: dto.customerName || dto.customerId || 'Unknown customer',
    customerId: dto.customerId || 'N/A',
    customerEmail: dto.customerEmail || delivery?.customerEmail || '',
    customerPhone: dto.customerPhone || delivery?.customerPhone || '',
    customerImage: getImageUrl(dto.customerImage || ''),
    date: dto.orderDate ?? '',
    items,
    subtotal,
    shipping: 0,
    discount,
    total,
    paymentMethod: payment?.method
      ? normalizeStatus(payment.method)
      : 'N/A',
    paymentStatus: payment?.status
      ? normalizeStatus(payment.status)
      : 'Pending',
    status: normalizedOrderStatus,
    shippingAddress: delivery?.address || 'Not available from the backend yet',
    assignedStaffName: staff,
    deliveryId: delivery?.deliveryId || delivery?.id || ''
  }
}

const deliveryStaffList = ref<{ id: string; name: string }[]>([])

const loadDeliveryStaff = async () => {
  try {
    const { data } = await api.get('/delivery-staff')
    const list = Array.isArray(data) ? data : Array.isArray(data?.content) ? data.content : []
    deliveryStaffList.value = list.map((s: any) => ({
      id: s.userId || s.id || '',
      name: s.name || s.email || 'Delivery Staff'
    }))
  } catch {
    deliveryStaffList.value = []
  }
}

const fetchOrders = async () => {
  isLoading.value = true
  loadError.value = ''

  try {
    const [ordersRes, deliveriesRes, productsRes] = await Promise.allSettled([
      api.get('/orders'),
      api.get('/deliveries'),
      api.get('/products')
    ])

    const ordersData = ordersRes.status === 'fulfilled' ? (ordersRes.value.data ?? []) : []
    const deliveriesData = deliveriesRes.status === 'fulfilled' ? (deliveriesRes.value.data ?? []) : []
    const productsData = productsRes.status === 'fulfilled' ? (productsRes.value.data ?? []) : []

    const deliveryMap = new Map<string, any>()
    for (const del of deliveriesData) {
      if (del.orderId) {
        deliveryMap.set(del.orderId, del)
      }
    }

    const productImages = new Map<string, string>()
    for (const p of productsData) {
      if (p.productId && Array.isArray(p.images) && p.images.length > 0) {
        productImages.set(String(p.productId), getImageUrl(p.images[0]))
      }
    }

    const withPayments = await Promise.all(
      ordersData.map(async (dto: any) => {
        const delivery = deliveryMap.get(dto.orderId) || null
        try {
          const { data: payment } =
            await api.get('/payments', {
              params: {
                orderId: dto.orderId
              }
            })

          return mapOrderDtoToOrder(
            dto,
            payment,
            delivery,
            productImages
          )
        } catch {
          return mapOrderDtoToOrder(
            dto,
            null,
            delivery,
            productImages
          )
        }
      })
    )

    withPayments.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
    orders.value = withPayments
  } catch (err: any) {
    loadError.value =
      err?.response?.data?.message ||
      'Failed to load orders.'
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  fetchOrders()
  loadDeliveryStaff()
})

const searchQuery = ref('')
const selectedStatus = ref('All')
const selectedPaymentStatus = ref('All')
const selectedPaymentMethod = ref('All')

const currentPage = ref(1)
const itemsPerPage = 5

const showDetailsModal = ref(false)
const showCancelModal = ref(false)
const showDeleteModal = ref(false)
const showAssignStaffModal = ref(false)
const orderToAssign = ref<Order | null>(null)
const selectedDeliveryStaff = ref('Delivery Staff')
const selectedExpectedDate = ref('')
const isAssigningStaff = ref(false)

const selectedOrder = ref<Order | null>(null)

const statusOptions = [
  'Pending',
  'Order Confirmed',
  'Picked & Packed',
  'In Transit',
  'Out for Delivery',
  'Delivered',
  'Cancelled'
]

const orderManagementAllowedStatuses = [
  'Pending',
  'Order Confirmed',
  'Picked & Packed',
  'In Transit',
  'Cancelled'
]

const paymentStatusOptions = [
  'Paid',
  'Pending',
  'Failed'
]

const paymentMethodOptions = [
  'Card',
  'Cash on Delivery',
  'PayPal'
]

const filteredOrders = computed(() => {
  const search =
    searchQuery.value
      .toLowerCase()
      .trim()

  return orders.value.filter((order) => {
    const matchesSearch =
      !search ||
      order.orderNumber
        .toLowerCase()
        .includes(search) ||
      order.customerName
        .toLowerCase()
        .includes(search) ||
      order.customerEmail
        .toLowerCase()
        .includes(search)

    const matchesStatus =
      selectedStatus.value === 'All' ||
      order.status ===
      selectedStatus.value

    const matchesPaymentStatus =
      selectedPaymentStatus.value === 'All' ||
      order.paymentStatus ===
      selectedPaymentStatus.value

    const matchesPaymentMethod =
      selectedPaymentMethod.value === 'All' ||
      order.paymentMethod ===
      selectedPaymentMethod.value

    return (
      matchesSearch &&
      matchesStatus &&
      matchesPaymentStatus &&
      matchesPaymentMethod
    )
  })
})

const totalPages = computed(() => {
  return Math.max(
    1,
    Math.ceil(
      filteredOrders.value.length /
      itemsPerPage
    )
  )
})

const paginatedOrders = computed(() => {
  const start =
    (currentPage.value - 1) *
    itemsPerPage

  return filteredOrders.value.slice(
    start,
    start + itemsPerPage
  )
})

const totalOrders = computed(() =>
  orders.value.length
)

const pendingOrders = computed(() =>
  orders.value.filter(
    (order) =>
      order.status === 'Pending'
  ).length
)

const processingOrders = computed(() =>
  orders.value.filter(
    (order) =>
      order.status === 'Processing'
  ).length
)

const deliveredOrders = computed(() =>
  orders.value.filter(
    (order) =>
      order.status === 'Delivered'
  ).length
)

const totalRevenue = computed(() =>
  orders.value
    .filter(
      (order) =>
        order.paymentStatus === 'Paid'
    )
    .reduce(
      (total, order) =>
        total + order.total,
      0
    )
)

const resetPage = () => {
  currentPage.value = 1
}

const clearFilters = () => {
  searchQuery.value = ''
  selectedStatus.value = 'All'
  selectedPaymentStatus.value = 'All'
  selectedPaymentMethod.value = 'All'
  currentPage.value = 1
}

const openOrderDetails = (
  order: Order
) => {
  selectedOrder.value = order
  showDetailsModal.value = true
}

const closeOrderDetails = () => {
  showDetailsModal.value = false
}

const updateOrderStatus = async (
  order: Order,
  status: string
) => {
  const previousStatus =
    order.status

  order.status = status

  try {
    const { data } = await api.put(
      `/orders/${order.id}/status`,
      null,
      {
        params: {
          status
        }
      }
    )
    // trust the server's normalized status
    if (data?.status) order.status = normalizeStatus(data.status)
  } catch (err: any) {
    order.status = previousStatus

    // tell the admin instead of failing silently
    loadError.value =
      err?.response?.data?.message ||
      'Failed to update order status.'
    setTimeout(() => { loadError.value = '' }, 6000)

    console.error(
      'Failed to update order status:',
      err
    )
  }
}

const openCancelModal = (
  order: Order
) => {
  selectedOrder.value = order
  showCancelModal.value = true
}

const cancelOrder = async () => {
  if (!selectedOrder.value) return

  await updateOrderStatus(
    selectedOrder.value,
    'Cancelled'
  )

  showCancelModal.value = false
}

const openDeleteModal = (
  order: Order
) => {
  selectedOrder.value = order
  showDeleteModal.value = true
}

const deleteOrder = async () => {
  if (!selectedOrder.value) return

  const orderToDelete =
    selectedOrder.value

  try {
    await api.delete(
      `/orders/${orderToDelete.id}`
    )

    orders.value =
      orders.value.filter(
        (order) =>
          order.id !==
          orderToDelete.id
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
      'Failed to delete order:',
      err
    )
  } finally {
    selectedOrder.value = null
    showDeleteModal.value = false
  }
}

const getOrderStatusClass = (
  status: Order['status'] | string
) => {
  switch (status) {
    case 'Pending':
      return 'bg-amber-50 border-amber-200 text-amber-700'
    case 'Order Confirmed':
      return 'bg-blue-50 border-blue-200 text-blue-700'
    case 'Picked & Packed':
      return 'bg-indigo-50 border-indigo-200 text-indigo-700'
    case 'In Transit':
      return 'bg-purple-50 border-purple-200 text-purple-700'
    case 'Out for Delivery':
      return 'bg-cyan-50 border-cyan-200 text-cyan-700'
    case 'Delivered':
      return 'bg-emerald-50 border-emerald-200 text-emerald-700'
    case 'Cancelled':
      return 'bg-red-50 border-red-200 text-red-700'
    default:
      return 'bg-slate-50 border-slate-200 text-slate-600'
  }
}

const getNextStatusStep = (status: string) => {
  switch (status) {
    case 'Pending':
      return { next: 'Order Confirmed', isAssign: false, label: '1. Order Confirmed', class: 'bg-blue-600 hover:bg-blue-700 text-white' }
    case 'Order Confirmed':
      return { next: 'Picked & Packed', isAssign: false, label: '2. Picked & Packed', class: 'bg-indigo-600 hover:bg-indigo-700 text-white' }
    case 'Picked & Packed':
      return { next: 'In Transit', isAssign: true, label: '3. In Transit & Assign Staff', class: 'bg-purple-600 hover:bg-purple-700 text-white' }
    default:
      return null
  }
}

const openAssignStaffModal = (order: Order) => {
  orderToAssign.value = order
  selectedDeliveryStaff.value = order.assignedStaffName || (deliveryStaffList.value[0]?.name || 'Delivery Staff')
  // By default, let backend calculate 5 days from order date, so start blank.
  selectedExpectedDate.value = ''
  showAssignStaffModal.value = true
}

const closeAssignStaffModal = () => {
  showAssignStaffModal.value = false
  orderToAssign.value = null
}

const confirmAssignStaff = async () => {
  if (!orderToAssign.value) return
  isAssigningStaff.value = true
  const order = orderToAssign.value
  const staffName = selectedDeliveryStaff.value || 'Delivery Staff'
  try {
    const params: any = {
      orderId: order.id,
      staffName,
      status: 'In Transit'
    }
    if (selectedExpectedDate.value) {
      params.expectedDate = selectedExpectedDate.value
    }

    await api.post('/deliveries/assign-order', null, { params })

    order.status = 'In Transit'
    order.assignedStaffName = staffName
    assignSuccessMessage.value = `Order #${order.orderNumber} successfully set to "In Transit" & assigned to ${staffName}. Handed over to Delivery Management!`
    showAssignStaffModal.value = false
    setTimeout(() => { assignSuccessMessage.value = '' }, 6000)
  } catch (err) {
    console.error('Failed to assign delivery staff:', err)
    // Don't fake success: the old fallback marked the order "In Transit" with a staff name
    // that was never saved, so the order and delivery records drifted apart.
    loadError.value =
      (err as any)?.response?.data?.message ||
      'Failed to assign delivery staff. The order status was not changed.'
    setTimeout(() => { loadError.value = '' }, 6000)
  } finally {
    isAssigningStaff.value = false
  }
}

const handleStepAdvance = (order: Order) => {
  const step = getNextStatusStep(order.status)
  if (!step) return
  if (step.isAssign) {
    openAssignStaffModal(order)
  } else {
    updateOrderStatus(order, step.next)
  }
}

const handleOrderManagementStatusChange = async (order: Order, newStatus: string) => {
  if (newStatus === 'In Transit') {
    openAssignStaffModal(order)
  } else {
    await updateOrderStatus(order, newStatus)
  }
}

const getPaymentStatusClass = (
  status: Order['paymentStatus']
) => {
  switch (status) {
    case 'Paid':
      return 'bg-emerald-50 border-emerald-200 text-emerald-600'

    case 'Pending':
      return 'bg-amber-50 border-amber-200 text-amber-600'

    case 'Failed':
      return 'bg-red-50 border-red-200 text-red-600'

    default:
      return 'bg-slate-50 border-slate-200 text-slate-600'
  }
}

const getInitials = (
  name: string
) => {
  return name
    .split(' ')
    .map((word) =>
      word.charAt(0)
    )
    .slice(0, 2)
    .join('')
    .toUpperCase()
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
    'en-US',
    {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    }
  )
}

/*
 * =========================================================
 * LKR CURRENCY FORMAT
 * =========================================================
 *
 * Examples:
 *
 * 8500      -> LKR 8,500.00
 * 125500    -> LKR 125,500.00
 * 0         -> LKR 0.00
 *
 * en-LK is used for Sri Lankan number formatting.
 */
const formatCurrency = (
  value: number
) => {
  return `LKR ${Number(
    value || 0
  ).toLocaleString('en-LK', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })}`
}
</script>

<template>
  <div class="min-h-screen bg-slate-100">

    <!-- =====================================================
         ADMIN SIDEBAR
         ===================================================== -->

    <AdminSidebar />

    <!-- =====================================================
         MAIN CONTENT
         ===================================================== -->

    <main class="ml-64 min-h-screen">

      <div class="relative min-h-screen overflow-hidden bg-slate-100 text-slate-900">

        <!-- Background -->

        <div class="fixed inset-0 pointer-events-none bg-gradient-to-br from-white via-slate-50 to-blue-50/70"></div>

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
                  / Orders
                </span>

              </div>

              <h1 class="mt-3 text-3xl sm:text-4xl font-black text-slate-950 tracking-tight">
                Order Management
              </h1>

              <p class="mt-2 text-sm text-slate-500">
                Manage customer orders, payments,
                delivery status, and order activity.
              </p>

            </div>

            <div
              class="flex items-center gap-2 px-4 py-3 rounded-xl bg-white/70 backdrop-blur-xl border border-white/90 shadow-lg shadow-slate-200/20">

              <span class="w-2 h-2 rounded-full bg-emerald-500"></span>

              <span class="text-xs font-bold text-emerald-600">
                Order System Online
              </span>

            </div>

          </div>

          <!-- =================================================
               STATISTICS
               ================================================= -->

          <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-5 gap-5 mb-8">

            <!-- Total Orders -->

            <div
              class="bg-white/65 backdrop-blur-2xl border border-white/90 rounded-2xl p-5 shadow-xl shadow-slate-300/15">

              <div class="flex justify-between items-start">

                <div>

                  <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                    Total Orders
                  </p>

                  <p class="mt-2 text-3xl font-black text-slate-950">
                    {{ totalOrders }}
                  </p>

                </div>

                <div class="w-10 h-10 rounded-xl bg-blue-50 border border-blue-100 flex items-center justify-center">

                  <svg class="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a3 3 0 006 0M9 5h6" />
                  </svg>

                </div>

              </div>

              <p class="mt-3 text-xs text-slate-400">
                All customer orders
              </p>

            </div>

            <!-- Pending -->

            <div
              class="bg-white/65 backdrop-blur-2xl border border-white/90 rounded-2xl p-5 shadow-xl shadow-slate-300/15">

              <div class="flex justify-between items-start">

                <div>

                  <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                    Pending
                  </p>

                  <p class="mt-2 text-3xl font-black text-slate-950">
                    {{ pendingOrders }}
                  </p>

                </div>

                <div class="w-10 h-10 rounded-xl bg-amber-50 border border-amber-100 flex items-center justify-center">

                  <svg class="w-5 h-5 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M12 8v4l3 2m6-2a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>

                </div>

              </div>

              <p class="mt-3 text-xs text-amber-600">
                Awaiting processing
              </p>

            </div>

            <!-- Processing -->

            <div
              class="bg-white/65 backdrop-blur-2xl border border-white/90 rounded-2xl p-5 shadow-xl shadow-slate-300/15">

              <div class="flex justify-between items-start">

                <div>

                  <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                    Processing
                  </p>

                  <p class="mt-2 text-3xl font-black text-slate-950">
                    {{ processingOrders }}
                  </p>

                </div>

                <div class="w-10 h-10 rounded-xl bg-blue-50 border border-blue-100 flex items-center justify-center">

                  <svg class="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M4 12a8 8 0 018-8m0 0v4m0-4l3 3M20 12a8 8 0 01-8 8m0 0v-4m0 4l-3-3" />
                  </svg>

                </div>

              </div>

              <p class="mt-3 text-xs text-blue-600">
                Being prepared
              </p>

            </div>

            <!-- Delivered -->

            <div
              class="bg-white/65 backdrop-blur-2xl border border-white/90 rounded-2xl p-5 shadow-xl shadow-slate-300/15">

              <div class="flex justify-between items-start">

                <div>

                  <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                    Delivered
                  </p>

                  <p class="mt-2 text-3xl font-black text-slate-950">
                    {{ deliveredOrders }}
                  </p>

                </div>

                <div
                  class="w-10 h-10 rounded-xl bg-emerald-50 border border-emerald-100 flex items-center justify-center">

                  <svg class="w-5 h-5 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                  </svg>

                </div>

              </div>

              <p class="mt-3 text-xs text-emerald-600">
                Successfully completed
              </p>

            </div>

            <!-- Paid Revenue -->

            <div
              class="bg-gradient-to-br from-blue-600 to-cyan-500 rounded-2xl p-5 shadow-xl shadow-blue-500/20 text-white">

              <div class="flex justify-between items-start">

                <div>

                  <p class="text-[10px] font-bold uppercase tracking-wider text-white/70">
                    Paid Revenue
                  </p>

                  <p class="mt-2 text-2xl font-black">
                    {{ formatCurrency(totalRevenue) }}
                  </p>

                </div>

                <div class="w-10 h-10 rounded-xl bg-white/15 border border-white/20 flex items-center justify-center">

                  <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M12 8c-1.1 0-2 .67-2 1.5S10.9 11 12 11s2 .67 2 1.5S13.1 14 12 14m0-6V6m0 12v-2m7-4a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>

                </div>

              </div>

              <p class="mt-3 text-xs text-white/70">
                Confirmed payments
              </p>

            </div>

          </div>

          <!-- =================================================
               ORDERS CARD
               ================================================= -->

          <section
            class="bg-white/65 backdrop-blur-2xl border border-white/90 rounded-3xl shadow-xl shadow-slate-300/20 overflow-hidden">

            <!-- Toolbar -->

            <div class="p-5 sm:p-6 border-b border-slate-200/80">

              <!-- Professional Workflow Banner -->
              <div class="mb-5 overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-sm">
                <!-- Header -->
                <div
                  class="flex flex-col gap-4 border-b border-slate-200 bg-slate-50/70 px-5 py-4 lg:flex-row lg:items-center lg:justify-between">
                  <div class="flex items-center gap-3">
                    <!-- Icon -->


                    <div>
                      <div class="flex items-center gap-2">
                        <h3 class="text-xs font-black uppercase tracking-[0.14em] text-slate-900">
                          Order Fulfillment Workflow
                        </h3>

                        <span class="rounded-full border border-blue-100 bg-blue-50 px-2 py-0.5
                   text-[9px] font-bold text-blue-700">
                          ORDER MANAGEMENT
                        </span>
                      </div>

                      <p class="mt-1 text-[11px] leading-relaxed text-slate-500">
                        Orders are processed through the first three fulfillment stages before
                        being handed over to Delivery Management.
                      </p>
                    </div>
                  </div>

                  <!-- Current ownership -->

                </div>

                <!-- Workflow -->
                <div class="px-5 py-5">
                  <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">

                    <!-- Step 1 -->
                    <div class="flex min-w-0 flex-1 items-center">
                      <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl
                 bg-blue-600 text-xs font-black text-white shadow-sm">
                        01
                      </div>

                      <div class="ml-3 min-w-0">
                        <p class="text-[10px] font-black uppercase tracking-wide text-blue-600">
                          Step 01
                        </p>
                        <p class="mt-0.5 truncate text-xs font-bold text-slate-800">
                          Order Confirmed
                        </p>
                      </div>
                    </div>

                    <div class="hidden h-px w-8 bg-slate-200 lg:block"></div>

                    <!-- Step 2 -->
                    <div class="flex min-w-0 flex-1 items-center">
                      <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl
                 bg-indigo-600 text-xs font-black text-white shadow-sm">
                        02
                      </div>

                      <div class="ml-3 min-w-0">
                        <p class="text-[10px] font-black uppercase tracking-wide text-indigo-600">
                          Step 02
                        </p>
                        <p class="mt-0.5 truncate text-xs font-bold text-slate-800">
                          Picked &amp; Packed
                        </p>
                      </div>
                    </div>

                    <div class="hidden h-px w-8 bg-slate-200 lg:block"></div>

                    <!-- Step 3 -->
                    <div class="flex min-w-0 flex-1 items-center">
                      <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl
                 bg-purple-600 text-xs font-black text-white shadow-sm">
                        03
                      </div>

                      <div class="ml-3 min-w-0">
                        <p class="text-[10px] font-black uppercase tracking-wide text-purple-600">
                          Step 03
                        </p>

                        <p class="mt-0.5 truncate text-xs font-bold text-slate-800">
                          In Transit
                        </p>

                        <p class="mt-0.5 text-[9px] text-slate-400">
                          Delivery staff assigned
                        </p>
                      </div>
                    </div>

                    <div class="hidden h-px w-8 bg-slate-200 lg:block"></div>

                    <!-- Handoff -->
                    <div class="flex items-center">
                      <router-link to="/admin/delivery-management" class="group inline-flex items-center gap-2 rounded-xl border
                 border-cyan-200 bg-cyan-50 px-3.5 py-2.5
                 text-[10px] font-black text-cyan-700
                 transition-all duration-200
                 hover:border-cyan-300 hover:bg-cyan-100">
                        <div class="flex h-7 w-7 items-center justify-center rounded-lg
                   bg-cyan-600 text-white">
                          <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                              d="M13 5h6v6M19 5l-8 8" />
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                              d="M19 13v4a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h4" />
                          </svg>
                        </div>

                        <div class="text-left">
                          <p class="text-[9px] font-bold uppercase tracking-wide text-cyan-500">
                            Next stage
                          </p>
                          <p class="leading-tight">
                            Delivery Management
                          </p>
                        </div>

                        <svg class="h-3.5 w-3.5 transition-transform duration-200
                   group-hover:translate-x-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                            d="M5 12h14m-6-6 6 6-6 6" />
                        </svg>
                      </router-link>
                    </div>
                  </div>
                </div>

                <!-- Footer description -->
                <div class="flex items-center gap-2 border-t border-slate-100 bg-slate-50/50
           px-5 py-2.5">
                  <svg class="h-3.5 w-3.5 shrink-0 text-slate-400" fill="none" stroke="currentColor"
                    viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                      d="M12 8v4l2.5 1.5M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
                  </svg>

                  <p class="text-[10px] font-medium text-slate-400">
                    Delivery Management continues the shipment with
                    <span class="font-bold text-slate-600">Out for Delivery</span>
                    and
                    <span class="font-bold text-slate-600">Delivered</span>.
                  </p>
                </div>
              </div>

              <!-- Success Alert -->
              <div v-if="assignSuccessMessage"
                class="mb-5 p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-bold flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <span>✓</span>
                  <span>{{ assignSuccessMessage }}</span>
                </div>
                <button @click="assignSuccessMessage = ''"
                  class="text-emerald-600 hover:text-emerald-900 font-bold text-sm">×</button>
              </div>

              <div class="flex flex-col xl:flex-row xl:items-center xl:justify-between gap-4">

                <div>

                  <h2 class="text-lg font-black text-slate-950">
                    Customer Orders
                  </h2>

                  <p class="mt-1 text-xs text-slate-400">
                    {{ filteredOrders.length }}
                    orders found
                  </p>

                </div>

                <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-3">

                  <!-- Search -->

                  <div class="relative">

                    <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" fill="none"
                      stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M21 21l-4.35-4.35m2.35-5.65a8 8 0 11-16 0 8 8 0 0116 0z" />
                    </svg>

                    <input v-model="searchQuery" @input="resetPage" type="text" placeholder="Search orders..."
                      class="w-full bg-white/80 border border-slate-200 rounded-xl pl-9 pr-4 py-2.5 text-xs text-slate-900 placeholder-slate-400 focus:outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10" />

                  </div>

                  <!-- Order Status -->

                  <select v-model="selectedStatus" @change="resetPage"
                    class="bg-white/80 border border-slate-200 rounded-xl px-4 py-2.5 text-xs text-slate-700 focus:outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10">

                    <option value="All">
                      All Order Status
                    </option>

                    <option v-for="status in statusOptions" :key="status" :value="status">
                      {{ status }}
                    </option>

                  </select>

                  <!-- Payment Status -->

                  <select v-model="selectedPaymentStatus" @change="resetPage"
                    class="bg-white/80 border border-slate-200 rounded-xl px-4 py-2.5 text-xs text-slate-700 focus:outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10">

                    <option value="All">
                      All Payment Status
                    </option>

                    <option v-for="status in paymentStatusOptions" :key="status" :value="status">
                      {{ status }}
                    </option>

                  </select>

                  <!-- Payment Method -->

                  <select v-model="selectedPaymentMethod" @change="resetPage"
                    class="bg-white/80 border border-slate-200 rounded-xl px-4 py-2.5 text-xs text-slate-700 focus:outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10">

                    <option value="All">
                      All Payment Methods
                    </option>

                    <option v-for="method in paymentMethodOptions" :key="method" :value="method">
                      {{ method }}
                    </option>

                  </select>

                </div>

              </div>

              <!-- Clear Filters -->

              <div v-if="
                searchQuery ||
                selectedStatus !== 'All' ||
                selectedPaymentStatus !== 'All' ||
                selectedPaymentMethod !== 'All'
              " class="mt-4">

                <button @click="clearFilters"
                  class="px-3 py-2 rounded-lg bg-slate-100 border border-slate-200 text-xs font-bold text-slate-600 hover:bg-slate-200 transition">
                  Clear All Filters
                </button>

              </div>

            </div>

            <!-- =================================================
                 TABLE
                 ================================================= -->

            <div class="overflow-x-auto">

              <table class="w-full text-left">

                <thead>

                  <tr class="border-b border-slate-200/80 bg-slate-50/60">

                    <th class="px-5 py-4 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                      Order
                    </th>

                    <th class="px-5 py-4 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                      Customer
                    </th>

                    <th class="px-5 py-4 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                      Date
                    </th>

                    <th class="px-5 py-4 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                      Total
                    </th>

                    <th class="px-5 py-4 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                      Payment
                    </th>

                    <th class="px-5 py-4 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                      Status
                    </th>

                    <th class="px-5 py-4 text-right text-[10px] font-bold uppercase tracking-wider text-slate-400">
                      Actions
                    </th>

                  </tr>

                </thead>

                <tbody class="divide-y divide-slate-200/60">

                  <tr v-for="order in paginatedOrders" :key="order.id" class="hover:bg-blue-50/30 transition-colors">

                    <!-- Order -->

                    <td class="px-5 py-4">

                      <div>

                        <p class="text-sm font-black text-blue-600">
                          {{ order.orderNumber }}
                        </p>

                        <p class="mt-1 text-[10px] text-slate-400">
                          {{ order.items.length }}
                          item(s)
                        </p>

                      </div>

                    </td>

                    <!-- Customer -->

                    <td class="px-5 py-4">

                      <div class="flex items-center gap-3 min-w-[190px]">

                        <img v-if="order.customerImage" :src="order.customerImage" :alt="order.customerName"
                          class="w-10 h-10 rounded-full object-cover shrink-0" />
                        <div v-else
                          class="w-10 h-10 rounded-full bg-gradient-to-br from-blue-500 to-cyan-400 text-white flex items-center justify-center text-xs font-black shrink-0">
                          {{
                            getInitials(
                              order.customerName
                            )
                          }}
                        </div>

                        <div>

                          <p class="text-xs font-bold text-slate-900">
                            {{ order.customerName }}
                          </p>

                          <p class="mt-1 text-[10px] text-slate-400">
                            {{ order.customerId }} •
                            {{
                              order.customerEmail ||
                              'No email'
                            }}
                          </p>

                        </div>

                      </div>

                    </td>

                    <!-- Date -->

                    <td class="px-5 py-4">

                      <span class="text-xs font-semibold text-slate-600">
                        {{ formatDate(order.date) }}
                      </span>

                    </td>

                    <!-- Total -->

                    <td class="px-5 py-4">

                      <span class="text-sm font-black text-slate-900">
                        {{ formatCurrency(order.total) }}
                      </span>

                    </td>

                    <!-- Payment -->

                    <td class="px-5 py-4">

                      <div class="space-y-1.5">

                        <p class="text-[11px] font-semibold text-slate-600">
                          {{ order.paymentMethod }}
                        </p>

                        <span :class="[
                          'inline-flex px-2 py-0.5 rounded-full border text-[9px] font-bold',
                          getPaymentStatusClass(
                            order.paymentStatus
                          )
                        ]">
                          {{ order.paymentStatus }}
                        </span>

                      </div>

                    </td>

                    <!-- Status -->

                    <td class="px-5 py-4">

                      <span :class="[
                        'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full border text-[10px] font-bold',
                        getOrderStatusClass(
                          order.status
                        )
                      ]">

                        <span class="w-1.5 h-1.5 rounded-full bg-current"></span>

                        {{ order.status }}

                      </span>

                    </td>

                    <!-- Actions -->

                    <td class="px-5 py-4">

                      <div class="flex items-center justify-end gap-2">

                        <!-- Next Step Action Button (Steps 1, 2, 3 only) -->
                        <button v-if="getNextStatusStep(order.status)" @click="handleStepAdvance(order)"
                          :title="getNextStatusStep(order.status)!.label" :class="[
                            'px-2.5 py-1.5 rounded-lg text-[10px] font-bold shadow-sm transition flex items-center gap-1 shrink-0',
                            getNextStatusStep(order.status)!.class
                          ]">
                          <span>{{ getNextStatusStep(order.status)!.label }}</span>
                        </button>

                        <!-- Handed over to Delivery Management (In Transit) -->
                        <div v-else-if="order.status === 'In Transit'"
                          class="flex items-center gap-1.5 shrink-0 px-2.5 py-1 rounded-lg bg-purple-50 border border-purple-200">
                          <span class="text-[9px] font-bold text-purple-700">
                            🚚 Staff: {{ order.assignedStaffName || 'Delivery Staff' }}
                          </span>
                          <router-link to="/admin/delivery-management" title="Sent to Delivery Management"
                            class="text-[9px] font-bold text-purple-600 hover:text-purple-900 underline flex items-center gap-0.5">
                            <span>Delivery Mgmt →</span>
                          </router-link>
                        </div>

                        <!-- Continuing in Delivery Management (Out for Delivery / Delivered) -->
                        <div v-else-if="order.status === 'Out for Delivery' || order.status === 'Delivered'"
                          class="flex items-center gap-1.5 shrink-0 px-2 py-1 rounded-lg bg-cyan-50 border border-cyan-200">
                          <span class="text-[9px] font-bold text-cyan-800">
                            {{ order.status }}
                          </span>
                          <router-link to="/admin/delivery-management" title="Managed in Delivery Management"
                            class="text-[9px] font-bold text-cyan-600 hover:text-cyan-900 underline flex items-center gap-0.5">
                            <span>In Delivery →</span>
                          </router-link>
                        </div>

                        <!-- View -->

                        <button @click="openOrderDetails(order)" title="View order"
                          class="w-9 h-9 rounded-lg bg-white border border-slate-200 text-slate-500 hover:text-blue-600 hover:border-blue-300 hover:bg-blue-50 transition flex items-center justify-center">

                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                              d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />

                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                              d="M2.458 12C3.732 7.943 7.523 5 12 5c4.477 0 8.268 2.943 9.542 7-1.274 4.057-5.065 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                          </svg>

                        </button>

                        <!-- Cancel -->

                        <button v-if="
                          order.status !== 'Cancelled' &&
                          order.status !== 'Delivered'
                        " @click="openCancelModal(order)" title="Cancel order"
                          class="w-9 h-9 rounded-lg bg-white border border-slate-200 text-slate-500 hover:text-red-600 hover:border-red-300 hover:bg-red-50 transition flex items-center justify-center">

                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                              d="M6 18L18 6M6 6l12 12" />
                          </svg>

                        </button>

                        <!-- Delete -->

                        <button @click="openDeleteModal(order)" title="Delete order"
                          class="w-9 h-9 rounded-lg bg-white border border-slate-200 text-slate-500 hover:text-red-600 hover:border-red-300 hover:bg-red-50 transition flex items-center justify-center">

                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6M9 7V4a1 1 0 011-1h4a1 1 0 011 1v3m-7 0h10" />
                          </svg>

                        </button>

                      </div>

                    </td>

                  </tr>

                  <!-- Empty -->

                  <tr v-if="
                    paginatedOrders.length === 0
                  ">

                    <td colspan="7" class="px-5 py-16 text-center">

                      <div
                        class="w-14 h-14 mx-auto rounded-2xl bg-slate-100 border border-slate-200 flex items-center justify-center">

                        <svg class="w-6 h-6 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a3 3 0 006 0M9 5h6" />
                        </svg>

                      </div>

                      <p class="mt-4 text-sm font-bold text-slate-700">
                        No orders found
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
                    filteredOrders.length === 0
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
                      filteredOrders.length
                    )
                  }}
                </span>

                of

                <span class="font-bold text-slate-600">
                  {{ filteredOrders.length }}
                </span>

                orders

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
                  " :disabled="currentPage ===
                    totalPages
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
         ASSIGN DELIVERY STAFF MODAL (Step 3)
         ===================================================== -->

    <Transition name="modal">
      <div v-if="showAssignStaffModal && orderToAssign"
        class="fixed inset-0 z-[100] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-slate-950/40 backdrop-blur-sm" @click="closeAssignStaffModal"></div>

        <div class="relative w-full max-w-lg bg-white rounded-3xl shadow-2xl border border-slate-100 overflow-hidden">
          <div
            class="px-6 py-5 bg-gradient-to-r from-purple-50 to-indigo-50 border-b border-purple-100 flex items-center justify-between">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-xl bg-purple-600 text-white flex items-center justify-center font-black">
                🚚
              </div>
              <div>
                <p class="text-[10px] font-bold uppercase tracking-wider text-purple-600">
                  Step 3: Dispatch &amp; Assign Staff
                </p>
                <h3 class="text-base font-black text-slate-900">
                  Order #{{ orderToAssign.orderNumber }}
                </h3>
              </div>
            </div>
            <button @click="closeAssignStaffModal"
              class="w-8 h-8 rounded-lg bg-white/80 border border-slate-200 text-slate-400 hover:text-slate-600 flex items-center justify-center">
              ×
            </button>
          </div>

          <div class="p-6 space-y-4">
            <div class="p-4 rounded-2xl bg-slate-50 border border-slate-200">
              <div class="flex justify-between items-center text-xs">
                <span class="text-slate-500">Customer:</span>
                <span class="font-bold text-slate-800">{{ orderToAssign.customerName }}</span>
              </div>
              <div class="flex justify-between items-center text-xs mt-2">
                <span class="text-slate-500">Total Amount:</span>
                <span class="font-bold text-slate-800">{{ formatCurrency(orderToAssign.total) }}</span>
              </div>
              <div class="flex justify-between items-center text-xs mt-2">
                <span class="text-slate-500">Target Status:</span>
                <span class="px-2 py-0.5 rounded-full bg-purple-100 text-purple-800 font-bold text-[10px]">
                  In Transit
                </span>
              </div>
            </div>

            <div>
              <label class="block text-xs font-bold text-slate-700 mb-1.5">
                Expected Delivery Date (Optional Override):
              </label>
              <input type="date" v-model="selectedExpectedDate"
                class="w-full bg-white border border-slate-300 rounded-xl px-4 py-2.5 text-xs font-semibold text-slate-800 focus:outline-none focus:ring-4 focus:ring-purple-500/10 focus:border-purple-500" />
            </div>

            <div>
              <label class="block text-xs font-bold text-slate-700 mb-1.5">
                Shipment Assigned to Delivery Staff:
              </label>
              <select v-model="selectedDeliveryStaff"
                class="w-full bg-white border border-slate-300 rounded-xl px-4 py-2.5 text-xs font-semibold text-slate-800 focus:outline-none focus:ring-4 focus:ring-purple-500/10 focus:border-purple-500">
                <option value="Delivery Staff">Delivery Staff (Default)</option>
                <option v-for="staff in deliveryStaffList" :key="staff.id" :value="staff.name">
                  {{ staff.name }}
                </option>
              </select>
              <p class="mt-1.5 text-[11px] text-slate-500">
                This is the final step handled in Order Management. After assignment, the shipment is sent to Delivery
                Management to continue with <strong>Out for Delivery</strong> and <strong>Delivered</strong>.
              </p>
            </div>
          </div>

          <div class="px-6 py-4 bg-slate-50 border-t border-slate-100 flex items-center justify-end gap-3">
            <button @click="closeAssignStaffModal"
              class="px-4 py-2 rounded-xl border border-slate-200 bg-white text-xs font-bold text-slate-600 hover:bg-slate-100 transition">
              Cancel
            </button>
            <button @click="confirmAssignStaff" :disabled="isAssigningStaff"
              class="px-5 py-2 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold shadow-md shadow-purple-500/20 transition disabled:opacity-50 flex items-center gap-1.5">
              <span v-if="isAssigningStaff">Assigning...</span>
              <span v-else>Confirm &amp; Send to Delivery ➔</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- =====================================================
         ORDER DETAILS MODAL
         ===================================================== -->

    <Transition name="modal">

      <div v-if="
        showDetailsModal &&
        selectedOrder
      " class="fixed inset-0 z-[100] flex items-center justify-center p-4">

        <div class="absolute inset-0 bg-slate-950/30 backdrop-blur-sm" @click="closeOrderDetails"></div>

        <div
          class="relative w-full max-w-3xl max-h-[90vh] overflow-y-auto bg-white/95 backdrop-blur-2xl border border-white rounded-3xl shadow-2xl shadow-slate-900/20">

          <!-- Modal Header -->

          <div
            class="sticky top-0 z-10 px-6 py-5 bg-white/90 backdrop-blur-xl border-b border-slate-200/80 flex items-center justify-between">

            <div>

              <p class="text-[10px] font-bold uppercase tracking-widest text-blue-600">
                Order Details
              </p>

              <h2 class="mt-1 text-xl font-black text-slate-950">
                {{ selectedOrder.orderNumber }}
              </h2>

            </div>

            <button @click="closeOrderDetails"
              class="w-9 h-9 rounded-xl bg-slate-100 border border-slate-200 text-slate-500 hover:bg-red-50 hover:border-red-200 hover:text-red-500 transition">
              ×
            </button>

          </div>

          <div class="p-6 space-y-6">

            <!-- Customer + Status -->

            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">

              <!-- Customer -->

              <div class="p-5 rounded-2xl bg-slate-50/80 border border-slate-200">

                <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                  Customer
                </p>

                <div class="mt-3 flex items-center gap-3">

                  <img v-if="selectedOrder.customerImage" :src="selectedOrder.customerImage"
                    :alt="selectedOrder.customerName" class="w-11 h-11 rounded-full object-cover" />
                  <div v-else
                    class="w-11 h-11 rounded-full bg-gradient-to-br from-blue-500 to-cyan-400 text-white flex items-center justify-center text-xs font-black">
                    {{
                      getInitials(
                        selectedOrder.customerName
                      )
                    }}
                  </div>

                  <div>

                    <p class="text-sm font-bold text-slate-900">
                      {{
                        selectedOrder.customerName
                      }}
                    </p>

                    <p class="text-xs text-slate-400 mt-1">
                      {{ selectedOrder.customerId }} •
                      {{
                        selectedOrder.customerEmail ||
                        'No email available'
                      }}
                    </p>

                    <p class="text-xs text-slate-400">
                      {{
                        selectedOrder.customerPhone ||
                        'No phone available'
                      }}
                    </p>

                  </div>

                </div>

              </div>

              <!-- Status -->

              <div class="p-5 rounded-2xl bg-slate-50/80 border border-slate-200">

                <div class="flex items-center justify-between">
                  <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                    Order Status (Order Management)
                  </p>
                  <span v-if="['In Transit', 'Out for Delivery', 'Delivered'].includes(selectedOrder.status)"
                    class="text-[10px] font-bold text-purple-700 bg-purple-50 px-2 py-0.5 rounded-full border border-purple-200">
                    Staff: {{ selectedOrder.assignedStaffName || 'Delivery Staff' }}
                  </span>
                </div>

                <div class="mt-3">

                  <!-- If already under Delivery Management (Out for Delivery or Delivered) -->
                  <div v-if="selectedOrder.status === 'Out for Delivery' || selectedOrder.status === 'Delivered'"
                    class="p-4 bg-cyan-50 border border-cyan-200 rounded-xl space-y-2">
                    <div class="flex items-center justify-between">
                      <span class="text-xs font-black text-cyan-900 flex items-center gap-1.5">
                        <span>🚚</span> Current Status: {{ selectedOrder.status }}
                      </span>
                      <span class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-cyan-200 text-cyan-800">
                        Delivery Mgmt Active
                      </span>
                    </div>
                    <p class="text-[11px] text-cyan-700">
                      Step 4 (Out for Delivery) and Step 5 (Delivered) are exclusively managed in the Delivery
                      Management panel.
                    </p>
                    <router-link to="/admin/delivery-management"
                      class="inline-flex items-center gap-1 text-xs font-bold text-white bg-cyan-600 hover:bg-cyan-700 px-3 py-1.5 rounded-lg transition shadow-sm">
                      Go to Delivery Management →
                    </router-link>
                  </div>

                  <!-- Allowed Order Management Statuses -->
                  <div v-else class="space-y-2">
                    <select :value="selectedOrder.status"
                      @change="handleOrderManagementStatusChange(selectedOrder, ($event.target as HTMLSelectElement).value)"
                      class="w-full bg-white border border-slate-200 rounded-xl px-4 py-3 text-sm font-bold text-slate-700 focus:outline-none focus:border-blue-500">
                      <option value="" disabled>Select status</option>
                      <option v-for="status in orderManagementAllowedStatuses" :key="status" :value="status">
                        {{ status }} {{ status === 'In Transit' ? '(Step 3: Assign Staff)' : '' }}
                      </option>
                    </select>

                    <p class="text-[10px] text-slate-400">
                      Order Management progression: 1. Order Confirmed → 2. Picked &amp; Packed → 3. In Transit
                      (Assigned to Delivery Staff).
                    </p>
                  </div>

                </div>

              </div>

            </div>

            <!-- =================================================
                 PRODUCTS
                 ================================================= -->

            <div>

              <h3 class="text-sm font-black text-slate-950 mb-3">
                Order Items
              </h3>

              <div class="space-y-3">

                <div v-for="item in selectedOrder.items" :key="item.sku"
                  class="p-4 bg-white/70 border border-slate-200 rounded-2xl flex items-center justify-between gap-4">

                  <div class="flex items-center gap-3 min-w-0">

                    <img :src="item.image" :alt="item.product"
                      class="w-14 h-14 object-cover rounded-xl border border-slate-200" />

                    <div class="min-w-0">

                      <p class="text-sm font-bold text-slate-900 truncate">
                        {{ item.product }}
                      </p>

                      <p class="mt-1 text-[10px] font-mono text-slate-400">
                        {{ item.sku }}
                      </p>

                      <p class="mt-1 text-xs text-slate-500">
                        Qty:
                        {{ item.quantity }}
                      </p>

                    </div>

                  </div>

                  <p class="text-sm font-black text-slate-900 shrink-0">
                    {{
                      formatCurrency(
                        item.price *
                        item.quantity
                      )
                    }}
                  </p>

                </div>

              </div>

            </div>

            <!-- =================================================
                 SHIPPING
                 ================================================= -->

            <div class="p-5 bg-slate-50/80 border border-slate-200 rounded-2xl">

              <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                Shipping Address
              </p>

              <p class="mt-2 text-sm font-semibold text-slate-700">
                {{
                  selectedOrder.shippingAddress
                }}
              </p>

            </div>

            <!-- =================================================
                 PAYMENT + SUMMARY
                 ================================================= -->

            <div class="grid grid-cols-1 md:grid-cols-2 gap-5">

              <!-- Payment -->

              <div class="p-5 bg-slate-50/80 border border-slate-200 rounded-2xl">

                <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                  Payment
                </p>

                <p class="mt-3 text-sm font-bold text-slate-800">
                  {{
                    selectedOrder.paymentMethod
                  }}
                </p>

                <span :class="[
                  'inline-flex mt-2 px-2.5 py-1 rounded-full border text-[10px] font-bold',
                  getPaymentStatusClass(
                    selectedOrder.paymentStatus
                  )
                ]">
                  {{
                    selectedOrder.paymentStatus
                  }}
                </span>

              </div>

              <!-- Summary -->

              <div class="p-5 bg-slate-50/80 border border-slate-200 rounded-2xl space-y-2">

                <!-- Subtotal -->

                <div class="flex justify-between text-xs text-slate-500">

                  <span>
                    Subtotal
                  </span>

                  <span>
                    {{
                      formatCurrency(
                        selectedOrder.subtotal
                      )
                    }}
                  </span>

                </div>

                <!-- Shipping -->

                <div class="flex justify-between text-xs text-slate-500">

                  <span>
                    Shipping
                  </span>

                  <span>
                    {{
                      formatCurrency(
                        selectedOrder.shipping
                      )
                    }}
                  </span>

                </div>

                <!-- Discount -->

                <div class="flex justify-between text-xs text-emerald-600">

                  <span>
                    Discount
                  </span>

                  <span>
                    -
                    {{
                      formatCurrency(
                        selectedOrder.discount
                      )
                    }}
                  </span>

                </div>

                <!-- Total -->

                <div class="pt-3 mt-3 border-t border-slate-200 flex justify-between">

                  <span class="text-sm font-black text-slate-900">
                    Total
                  </span>

                  <span class="text-lg font-black text-blue-600">
                    {{
                      formatCurrency(
                        selectedOrder.total
                      )
                    }}
                  </span>

                </div>

              </div>

            </div>

            <!-- Close -->

            <button @click="closeOrderDetails"
              class="w-full px-5 py-3 rounded-xl bg-slate-900 text-white text-sm font-bold hover:bg-slate-800 transition">
              Close Order Details
            </button>

          </div>

        </div>

      </div>

    </Transition>

    <!-- =====================================================
         CANCEL ORDER MODAL (ORDER MANAGEMENT DESIGN)
         ===================================================== -->
    <AdminConfirmModal
      v-model:show="showCancelModal"
      type="warning"
      title="Cancel Order?"
      message="Are you sure you want to cancel order"
      :target="selectedOrder?.orderNumber"
      description="The order status will be changed to Cancelled and customer will be notified."
      confirm-text="Cancel Order"
      cancel-text="Keep Order"
      @confirm="cancelOrder"
    />

    <!-- =====================================================
         DELETE ORDER MODAL (ORDER MANAGEMENT DESIGN)
         ===================================================== -->
    <AdminConfirmModal
      v-model:show="showDeleteModal"
      type="danger"
      title="Delete Order?"
      message="Are you sure you want to permanently delete order"
      :target="selectedOrder?.orderNumber"
      description="This order record will be permanently deleted. This action cannot be undone."
      confirm-text="Delete Order"
      cancel-text="Cancel"
      @confirm="deleteOrder"
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