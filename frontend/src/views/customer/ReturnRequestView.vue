<script setup lang="ts">
import {
  ref,
  computed,
  onMounted,
  onBeforeUnmount
} from 'vue'

import { useRouter } from 'vue-router'
import axios from 'axios'
import { useNotificationStore } from '../../stores/notificationStore'
import ResponseBanner from '../../components/common/ResponseBanner.vue'

import demoProductImage from '../../assets/images/imageDemo.jpg'

// =====================================================
// TYPES
// =====================================================

interface OrderItem {
  id: string | number
  productId?: string | number
  name: string
  price: number
  quantity: number
}

interface Order {
  id: string
  date: string
  items: OrderItem[]
  total?: number
}

interface ReturnRequestResponse {
  returnId: string
  reason: string
  description?: string
  status: string
  requestDate: string
  refundAmount: number | null
  orderId: string
  supportStaffId: string | null
  refundMethod?: string | null
  evidenceImage?: string | null
}

interface BackendOrderItem {
  orderItemId?: string | number
  id?: string | number
  productId?: string | number
  productName?: string
  name?: string
  unitPrice?: number
  price?: number
  quantity: number
}

interface BackendOrder {
  orderId: string
  orderDate: string
  items?: BackendOrderItem[]
  orderTotal?: number
  total?: number
}

// =====================================================
// ROUTER
// =====================================================

const router = useRouter()

// =====================================================
// STATE
// =====================================================

const orders = ref<Order[]>([])

const returnRequests =
  ref<ReturnRequestResponse[]>([])

const isLoading = ref(true)
const isLoadingReturns = ref(false)
const isSubmitting = ref(false)

// =====================================================
// LOGGED-IN CUSTOMER SESSION
// =====================================================

const loggedInCustomerId = (() => {
  try {
    const raw = sessionStorage.getItem('user')
    if (raw) {
      const u = JSON.parse(raw)
      return u.userId || u.customerId || ''
    }
  } catch {
    // ignore
  }
  return ''
})()

const submitSuccess = ref(false)
const submitError = ref('')

const submittedReturn =
  ref<ReturnRequestResponse | null>(null)

// =====================================================
// FORM
// =====================================================

const selectedOrderId = ref('')
const selectedReason = ref('')
const returnDescription = ref('')
const selectedRefundMethod = ref('')

// =====================================================
// EVIDENCE IMAGE
// =====================================================

const evidenceImage =
  ref<File | null>(null)

const evidencePreview =
  ref('')

// =====================================================
// RETURN REASONS
// =====================================================

const reasonsList = [
  'Defective / Not Working',
  'Wrong Item Received',
  'Item Damaged During Shipping',
  'Changed My Mind',
  'Incompatible Hardware'
]

// =====================================================
// REFUND METHODS
// =====================================================

const refundMethods = [
  {
    value: 'ORIGINAL_PAYMENT_METHOD',
    label: 'Original Payment Method',
    description:
      'Refund to the payment method used for your order'
  },
  {
    value: 'BANK_TRANSFER',
    label: 'Bank Transfer',
    description:
      'Receive the refund through a bank transfer'
  },
  {
    value: 'STORE_CREDIT',
    label: 'Store Credit',
    description:
      'Receive the amount as credit for future purchases'
  }
]

// =====================================================
// PRODUCT IMAGES
// =====================================================

const productImages =
  ref<Record<string, string>>({})

async function fetchProductImages() {
  try {
    const response =
      await axios.get<
        Array<{
          productId: string
          images?: string[]
        }>
      >('/api/products')

    const map: Record<string, string> = {}

    for (const product of response.data ?? []) {
      const firstImage =
        (product.images ?? []).find(
          (image) =>
            typeof image === 'string' &&
            image.trim() !== ''
        )

      if (
        product.productId &&
        firstImage
      ) {
        map[product.productId] =
          firstImage
      }
    }

    productImages.value = map

  } catch (err) {
    console.error(
      'Failed to load product images:',
      err
    )
  }
}

function getProductImage(
  productId:
    | string
    | number
    | undefined
): string {

  if (
    productId === undefined ||
    productId === null
  ) {
    return demoProductImage
  }

  const image =
    productImages.value[
    String(productId)
    ]

  return image || demoProductImage
}

function handleProductImageError(
  event: Event
) {

  const image =
    event.target as HTMLImageElement

  if (
    image.src !==
    demoProductImage
  ) {
    image.src =
      demoProductImage
  }
}

// =====================================================
// MAP BACKEND ORDER
// =====================================================

function mapOrder(
  order: BackendOrder
): Order {

  return {
    id: order.orderId,

    date: order.orderDate,

    total: Number(
      order.orderTotal ??
      order.total ??
      0
    ),

    items:
      (order.items ?? []).map(
        (item) => {

          const productId =
            item.productId !== undefined
              ? String(item.productId)
              : ''

          return {

            id:
              item.orderItemId ??
              item.id ??
              item.productId ??
              '',

            productId,

            name:
              item.productName ??
              item.name ??
              'Unknown Product',

            price: Number(
              item.unitPrice ??
              item.price ??
              0
            ),

            quantity:
              Number(
                item.quantity ?? 0
              )
          }
        }
      )
  }
}

// =====================================================
// FETCH ORDERS
// =====================================================

async function fetchOrders() {

  isLoading.value = true
  submitError.value = ''

  try {

    // Only load orders belonging to the logged-in customer
    const params: Record<string, string> = {}
    if (loggedInCustomerId) {
      params.customerId = loggedInCustomerId
    }

    const response =
      await axios.get<BackendOrder[]>(
        '/api/orders',
        { params }
      )

    orders.value =
      Array.isArray(response.data)
        ? response.data.map(mapOrder)
        : []

  } catch (err) {

    console.error(
      'Failed to fetch orders:',
      err
    )

    submitError.value =
      'Unable to load your orders. Please try again.'

  } finally {

    isLoading.value = false
  }
}

// =====================================================
// FETCH RETURN REQUESTS
// =====================================================

async function fetchReturnRequests() {

  isLoadingReturns.value = true

  try {

    // Only load return requests for the logged-in customer
    const params: Record<string, string> = {}
    if (loggedInCustomerId) {
      params.customerId = loggedInCustomerId
    }

    const response =
      await axios.get(
        '/api/returns',
        { params }
      )

    /*
     * Supports:
     *
     * [
     *   {...}
     * ]
     *
     * or
     *
     * {
     *   content: [...]
     * }
     *
     * or
     *
     * {
     *   data: [...]
     * }
     */

    let data: any[] = []

    if (
      Array.isArray(response.data)
    ) {

      data = response.data

    } else if (
      Array.isArray(
        response.data?.content
      )
    ) {

      data =
        response.data.content

    } else if (
      Array.isArray(
        response.data?.data
      )
    ) {

      data =
        response.data.data

    }

    returnRequests.value =
      data.map(
        (item: any) => {

          return {

            returnId:
              String(
                item.returnId ??
                item.id ??
                ''
              ),

            reason:
              item.reason ??
              '-',

            description:
              item.description ??
              '',

            status:
              item.status ??
              'PENDING',

            requestDate:
              item.requestDate ??
              item.createdAt ??
              item.returnDate ??
              '',

            refundAmount:
              item.refundAmount !== null &&
                item.refundAmount !== undefined &&
                Number(item.refundAmount) > 0
                ? Number(
                  item.refundAmount
                )
                : (item.orderTotal && Number(item.orderTotal) > 0
                  ? Number(item.orderTotal)
                  : (orders.value.find(o => o.id === String(item.orderId ?? ''))?.total ?? null)),

            orderId:
              String(
                item.orderId ??
                ''
              ),

            supportStaffId:
              item.supportStaffId ??
              null,

            refundMethod:
              item.refundMethod ??
              null,

            evidenceImage:
              item.evidenceImage ??
              item.evidenceImageUrl ??
              null
          }
        }
      )

    /*
     * Newest request first
     */
    returnRequests.value.sort(
      (a, b) => {

        const dateA =
          new Date(
            a.requestDate || 0
          ).getTime()

        const dateB =
          new Date(
            b.requestDate || 0
          ).getTime()

        return dateB - dateA
      }
    )

  } catch (err) {

    console.error(
      'Failed to fetch return requests:',
      err
    )

    /*
     * Do not show a red error over
     * the complete return form.
     */
    returnRequests.value = []

  } finally {

    isLoadingReturns.value = false
  }
}

// =====================================================
// SELECTED ORDER
// =====================================================

const currentOrder =
  computed(() => {

    return orders.value.find(
      (order) =>
        order.id ===
        selectedOrderId.value
    )
  })

// =====================================================
// ACTIVE RETURN REQUEST CHECK
// One order can only have ONE active (non-rejected) return request.
// If rejected, the customer can submit a new request.
// =====================================================

function getActiveReturnForOrder(orderId: string) {
  if (!orderId) return null
  return returnRequests.value.find(
    (req) =>
      req.orderId === orderId &&
      String(req.status).toUpperCase() !== 'REJECTED'
  ) || null
}

const activeReturnForSelectedOrder =
  computed(() => {
    return getActiveReturnForOrder(selectedOrderId.value)
  })

// =====================================================
// ORDER ITEM COUNT
// =====================================================

const currentOrderItemCount =
  computed(() => {

    if (!currentOrder.value) {
      return 0
    }

    return currentOrder.value.items.reduce(
      (total, item) =>
        total + item.quantity,
      0
    )
  })

// =====================================================
// ORDER TOTAL
// =====================================================

const currentOrderTotal =
  computed(() => {

    if (!currentOrder.value) {
      return 0
    }

    if (
      currentOrder.value.total &&
      currentOrder.value.total > 0
    ) {
      return currentOrder.value.total
    }

    return currentOrder.value.items.reduce(
      (total, item) =>
        total +
        item.price *
        item.quantity,
      0
    )
  })

// =====================================================
// FORMAT CURRENCY
// =====================================================

function formatCurrency(
  value: number | null
): string {

  if (
    value === null ||
    value === undefined
  ) {
    return '-'
  }

  return new Intl.NumberFormat(
    'en-LK',
    {
      style: 'currency',
      currency: 'LKR',
      minimumFractionDigits: 2
    }
  ).format(value)
}

// =====================================================
// FORMAT DATE
// =====================================================

function formatDate(
  date: string
): string {

  if (!date) {
    return '-'
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
    'en-GB',
    {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    }
  )
}

// =====================================================
// FORMAT DATE TIME
// =====================================================

function formatDateTime(
  date: string
): string {

  if (!date) {
    return '-'
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

  return parsedDate.toLocaleString(
    'en-GB',
    {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }
  )
}

// =====================================================
// STATUS TEXT
// =====================================================

function formatStatus(
  status: string
): string {

  if (!status) {
    return 'Pending'
  }

  return status
    .replaceAll('_', ' ')
    .toLowerCase()
    .replace(/\b\w/g, (letter) =>
      letter.toUpperCase()
    )
}

// =====================================================
// STATUS CLASS
// =====================================================

function getStatusClass(
  status: string
): string {

  const value =
    String(status)
      .toUpperCase()

  if (
    value === 'APPROVED' ||
    value === 'COMPLETED' ||
    value === 'REFUNDED' ||
    value === 'SUCCESS'
  ) {

    return `
      bg-emerald-50
      text-emerald-700
      border-emerald-200
    `
  }

  if (
    value === 'REJECTED' ||
    value === 'CANCELLED' ||
    value === 'DENIED'
  ) {

    return `
      bg-red-50
      text-red-700
      border-red-200
    `
  }

  if (
    value === 'PROCESSING' ||
    value === 'UNDER_REVIEW' ||
    value === 'IN_PROGRESS'
  ) {

    return `
      bg-purple-50
      text-purple-700
      border-purple-200
    `
  }

  return `
    bg-amber-50
    text-amber-700
    border-amber-200
  `
}

// =====================================================
// STATUS DOT
// =====================================================

function getStatusDot(
  status: string
): string {

  const value =
    String(status)
      .toUpperCase()

  if (
    value === 'APPROVED' ||
    value === 'COMPLETED' ||
    value === 'REFUNDED' ||
    value === 'SUCCESS'
  ) {
    return 'bg-emerald-500'
  }

  if (
    value === 'REJECTED' ||
    value === 'CANCELLED' ||
    value === 'DENIED'
  ) {
    return 'bg-red-500'
  }

  if (
    value === 'PROCESSING' ||
    value === 'UNDER_REVIEW' ||
    value === 'IN_PROGRESS'
  ) {
    return 'bg-purple-500'
  }

  return 'bg-amber-500'
}

// =====================================================
// REFUND METHOD LABEL
// =====================================================

function getRefundMethodLabel(
  value:
    | string
    | null
    | undefined
): string {

  if (!value) {
    return '-'
  }

  const method =
    refundMethods.find(
      (item) =>
        item.value === value
    )

  if (method) {
    return method.label
  }

  return value
    .replaceAll('_', ' ')
}

// =====================================================
// EVIDENCE URL
// =====================================================

function getEvidenceUrl(
  image:
    | string
    | null
    | undefined
): string {

  if (!image) {
    return ''
  }

  if (
    image.startsWith('http://') ||
    image.startsWith('https://') ||
    image.startsWith('blob:')
  ) {
    return image
  }

  // /uploads/... paths are proxied by Vite to localhost:8080 — return as-is
  if (image.startsWith('/')) {
    return image
  }

  return `http://localhost:8080/${image}`
}

// =====================================================
// EVIDENCE IMAGE ERROR
// =====================================================

function handleEvidenceImageError(
  event: Event
) {

  const image =
    event.target as HTMLImageElement

  image.style.display =
    'none'
}

// =====================================================
// FILE SELECT
// =====================================================

function handleEvidenceImage(
  event: Event
) {

  const input =
    event.target as HTMLInputElement

  const file =
    input.files?.[0]

  if (!file) {
    return
  }

  submitError.value = ''

  if (
    !file.type.startsWith(
      'image/'
    )
  ) {

    submitError.value =
      'Please select a valid image file.'

    input.value = ''

    return
  }

  const maxSize =
    5 * 1024 * 1024

  if (
    file.size > maxSize
  ) {

    submitError.value =
      'Evidence image must be smaller than 5 MB.'

    input.value = ''

    return
  }

  if (
    evidencePreview.value
  ) {

    URL.revokeObjectURL(
      evidencePreview.value
    )
  }

  evidenceImage.value =
    file

  evidencePreview.value =
    URL.createObjectURL(file)
}

// =====================================================
// REMOVE EVIDENCE
// =====================================================

function removeEvidenceImage() {

  evidenceImage.value =
    null

  if (
    evidencePreview.value
  ) {

    URL.revokeObjectURL(
      evidencePreview.value
    )

    evidencePreview.value =
      ''
  }
}

// =====================================================
// FILE SIZE
// =====================================================

function formatFileSize(
  bytes: number
): string {

  if (bytes < 1024) {
    return `${bytes} B`
  }

  if (
    bytes <
    1024 * 1024
  ) {

    return `${(
      bytes / 1024
    ).toFixed(1)} KB`
  }

  return `${(
    bytes /
    (1024 * 1024)
  ).toFixed(1)} MB`
}

// =====================================================
// FORM VALIDATION
// =====================================================

const canSubmit =
  computed(() => {

    return (
      !!selectedOrderId.value &&
      !activeReturnForSelectedOrder.value &&
      !!selectedReason.value &&
      !!returnDescription.value.trim() &&
      !!selectedRefundMethod.value &&
      !isSubmitting.value
    )
  })

// =====================================================
// SUBMIT RETURN REQUEST
// =====================================================

async function handleReturnSubmit() {

  if (activeReturnForSelectedOrder.value) {
    submitError.value =
      `Order ${selectedOrderId.value} already has an active return request (${formatStatus(activeReturnForSelectedOrder.value.status)}). A new request can only be submitted if previous requests were rejected.`
    return
  }

  if (!canSubmit.value) {
    return
  }

  isSubmitting.value =
    true

  submitError.value =
    ''

  try {

    let response

    if (evidenceImage.value) {
      // Multipart only when an image is attached.
      // Do NOT set Content-Type manually — let the browser set the correct
      // multipart boundary automatically (Axios with FormData does this by
      // default; setting it manually appends charset=UTF-8 which Spring rejects).
      const formData = new FormData()
      formData.append('orderId', selectedOrderId.value)
      formData.append('reason', selectedReason.value)
      formData.append('description', returnDescription.value.trim())
      formData.append('refundMethod', selectedRefundMethod.value)
      if (currentOrderTotal.value > 0) {
        formData.append('refundAmount', currentOrderTotal.value.toString())
      }
      formData.append('evidenceImage', evidenceImage.value)

      response = await axios.post<ReturnRequestResponse>(
        '/api/returns',
        formData
        // No headers override — Axios + browser sets correct multipart/form-data boundary
      )
    } else {
      // No image — send plain JSON (matched by Spring's APPLICATION_JSON_VALUE mapping)
      response = await axios.post<ReturnRequestResponse>(
        '/api/returns',
        {
          orderId: selectedOrderId.value,
          reason: selectedReason.value,
          description: returnDescription.value.trim(),
          refundMethod: selectedRefundMethod.value,
          refundAmount: currentOrderTotal.value > 0 ? currentOrderTotal.value : undefined
        },
        {
          headers: { 'Content-Type': 'application/json' }
        }
      )
    }

    console.log(
      'Return request response:',
      response.data
    )

    submittedReturn.value =
      response.data

    submitSuccess.value =
      true

    /*
     * IMPORTANT:
     * Reload return history so the newly
     * submitted request appears there.
     */

    await fetchReturnRequests()

  } catch (err) {

    console.error(
      'Failed to submit return request:',
      err
    )

    if (
      axios.isAxiosError(err) &&
      err.response?.data?.message
    ) {

      submitError.value =
        err.response.data.message

    } else {

      submitError.value =
        'Something went wrong submitting your return request. Please try again.'
    }

  } finally {

    isSubmitting.value =
      false
  }
}

// =====================================================
// RESET FORM
// =====================================================

function resetForm() {

  selectedOrderId.value =
    ''

  selectedReason.value =
    ''

  returnDescription.value =
    ''

  selectedRefundMethod.value =
    ''

  removeEvidenceImage()

  submittedReturn.value =
    null

  submitSuccess.value =
    false

  submitError.value =
    ''
}

// =====================================================
// NEW REQUEST
// =====================================================

function startNewRequest() {

  resetForm()

  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })
}

// =====================================================
// CLEANUP
// =====================================================

onBeforeUnmount(() => {

  if (
    evidencePreview.value
  ) {

    URL.revokeObjectURL(
      evidencePreview.value
    )
  }
})

// =====================================================
// LOAD
// =====================================================

onMounted(() => {

  fetchOrders()

  fetchProductImages()

  fetchReturnRequests()
})
</script>

<template>

  <!-- ===================================================== -->
  <!-- PAGE -->
  <!-- ===================================================== -->

  <div class="min-h-screen
           bg-gradient-to-br
           from-slate-50
           via-white
           to-blue-50/50
           px-4
           sm:px-6
           lg:px-8
           py-8
           sm:py-10">

    <div class="max-w-5xl mx-auto">

      <!-- =================================================
           HEADER
      ================================================== -->

      <div class="mb-7
               rounded-[28px]
               bg-white/65
               backdrop-blur-2xl
               border border-white/90
               shadow-xl
               shadow-slate-300/20
               p-5
               sm:p-6">

        <div class="flex
                 flex-col
                 sm:flex-row
                 sm:items-center
                 sm:justify-between
                 gap-4">

          <div>

            <div class="flex
                     items-center
                     gap-2
                     mb-2">

              <div class="w-10
                       h-10
                       rounded-xl
                       bg-orange-50
                       text-orange-500
                       border border-orange-100
                       flex
                       items-center
                       justify-center">

                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">

                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                    d="M9 7H5a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2v-3" />

                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M8 11 4 7l4-4" />

                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M12 5h5a4 4 0 0 1 4 4v3" />

                </svg>

              </div>

              <span class="text-[9px]
                       uppercase
                       tracking-[0.18em]
                       font-black
                       text-orange-500">
                Customer Support
              </span>

            </div>

            <h1 class="text-2xl
                     sm:text-3xl
                     font-black
                     tracking-tight
                     text-slate-950">
              Returns & Refunds
            </h1>

            <p class="text-sm
                     text-slate-500
                     mt-1">
              Submit a return request and track
              the result of your requests.
            </p>

          </div>

          <button type="button" @click="router.push('/orders')" class="inline-flex
                   items-center
                   justify-center
                   gap-2
                   px-4
                   py-2.5
                   rounded-xl
                   bg-white/75
                   border border-slate-200
                   text-slate-600
                   hover:bg-blue-50
                   hover:border-blue-200
                   hover:text-blue-600
                   text-xs
                   font-bold
                   transition-all
                   shadow-sm">

            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">

              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M15 18l-6-6 6-6" />

            </svg>

            Back to Orders

          </button>

        </div>

      </div>

      <!-- =================================================
           RETURN HISTORY
      ================================================== -->

      <section class="mb-7
               rounded-[28px]
               bg-white/65
               backdrop-blur-2xl
               border border-white/90
               shadow-xl
               shadow-slate-300/15
               p-5
               sm:p-6">

        <!-- HEADER -->

        <div class="flex
                 flex-col
                 sm:flex-row
                 sm:items-center
                 sm:justify-between
                 gap-4
                 mb-5">

          <div class="flex
                   items-center
                   gap-3">

            <div class="w-10
                     h-10
                     rounded-xl
                     bg-blue-50
                     text-blue-600
                     border border-blue-100
                     flex
                     items-center
                     justify-center">

              <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">

                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                  d="M9 12h6m-6 4h4M7 3h10a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z" />

              </svg>

            </div>

            <div>

              <p class="text-[9px]
                       uppercase
                       tracking-[0.15em]
                       font-black
                       text-blue-500">
                Return Center
              </p>

              <h2 class="text-lg
                       sm:text-xl
                       font-black
                       text-slate-900">
                My Return Requests
              </h2>

            </div>

          </div>

          <div class="flex
                   items-center
                   gap-2">

            <span class="px-3
                     py-1.5
                     rounded-full
                     bg-blue-50
                     border border-blue-100
                     text-blue-600
                     text-[10px]
                     font-black">
              {{ returnRequests.length }}
              Requests
            </span>

            <button type="button" @click="fetchReturnRequests" class="px-3
                     py-1.5
                     rounded-xl
                     bg-white
                     border border-slate-200
                     text-slate-500
                     hover:text-blue-600
                     hover:border-blue-200
                     text-[10px]
                     font-bold
                     transition-all">
              Refresh
            </button>

          </div>

        </div>

        <!-- LOADING -->

        <div v-if="isLoadingReturns" class="flex
                 items-center
                 justify-center
                 gap-2
                 py-10
                 text-slate-400
                 text-xs">

          <svg class="animate-spin w-5 h-5" fill="none" viewBox="0 0 24 24">

            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />

            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />

          </svg>

          Loading your return requests...

        </div>

        <!-- EMPTY -->

        <div v-else-if="returnRequests.length === 0" class="rounded-2xl
                 bg-slate-50/80
                 border border-slate-200
                 py-10
                 px-5
                 text-center">

          <div class="w-14
                   h-14
                   mx-auto
                   rounded-2xl
                   bg-white
                   border border-slate-200
                   text-slate-400
                   flex
                   items-center
                   justify-center">

            <svg class="w-7 h-7" fill="none" viewBox="0 0 24 24" stroke="currentColor">

              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                d="M9 12h6m-6 4h4M7 3h10a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z" />

            </svg>

          </div>

          <h3 class="mt-4
                   text-sm
                   font-black
                   text-slate-700">
            No return requests yet
          </h3>

          <p class="text-[11px]
                   text-slate-400
                   mt-1">
            Your submitted return requests and
            their results will appear here.
          </p>

        </div>

        <!-- RETURN REQUESTS -->

        <div v-else class="space-y-4">

          <div v-for="request in returnRequests" :key="request.returnId" class="relative
                   overflow-hidden
                   rounded-2xl
                   bg-white/80
                   border border-slate-200
                   p-4
                   sm:p-5
                   hover:border-blue-200
                   hover:shadow-lg
                   hover:shadow-blue-500/5
                   transition-all">

            <!-- TOP -->

            <div class="flex
                     flex-col
                     lg:flex-row
                     lg:items-start
                     lg:justify-between
                     gap-4">

              <div>

                <div class="flex
                         flex-wrap
                         items-center
                         gap-2">

                  <span class="text-sm
                           font-black
                           text-slate-900">
                    {{ request.returnId }}
                  </span>

                  <span class="px-2.5
                           py-1
                           rounded-full
                           border
                           text-[9px]
                           font-black
                           inline-flex
                           items-center
                           gap-1.5" :class="getStatusClass(
                            request.status
                          )
                            ">

                    <span class="w-1.5
                             h-1.5
                             rounded-full" :class="getStatusDot(
                              request.status
                            )
                              "></span>

                    {{ formatStatus(request.status) }}

                  </span>

                </div>

                <p class="text-[10px]
                         text-slate-400
                         mt-2">
                  Requested:
                  {{ formatDateTime(request.requestDate) }}
                </p>

              </div>

              <div class="lg:text-right">

                <p class="text-[9px]
                         uppercase
                         tracking-wider
                         font-black
                         text-slate-400">
                  Order
                </p>

                <p class="text-sm
                         font-black
                         text-blue-600
                         font-mono">
                  {{ request.orderId }}
                </p>

              </div>

            </div>

            <!-- DETAILS -->

            <div class="mt-4
                     grid
                     grid-cols-1
                     sm:grid-cols-2
                     lg:grid-cols-4
                     gap-3">

              <!-- REASON -->

              <div class="rounded-xl
                       bg-slate-50
                       border border-slate-200
                       p-3">

                <p class="text-[8px]
                         uppercase
                         tracking-wider
                         font-black
                         text-slate-400">
                  Reason
                </p>

                <p class="text-xs
                         font-bold
                         text-slate-700
                         mt-1">
                  {{ request.reason }}
                </p>

              </div>

              <!-- REFUND -->

              <div class="rounded-xl
                       bg-slate-50
                       border border-slate-200
                       p-3">

                <p class="text-[8px]
                         uppercase
                         tracking-wider
                         font-black
                         text-slate-400">
                  Refund Amount
                </p>

                <p class="text-xs
                         font-black
                         text-emerald-600
                         mt-1">
                  {{
                    formatCurrency(
                      request.refundAmount
                    )
                  }}
                </p>

              </div>

              <!-- REFUND METHOD -->

              <div class="rounded-xl
                       bg-slate-50
                       border border-slate-200
                       p-3">

                <p class="text-[8px]
                         uppercase
                         tracking-wider
                         font-black
                         text-slate-400">
                  Refund Method
                </p>

                <p class="text-xs
                         font-bold
                         text-slate-700
                         mt-1">
                  {{
                    getRefundMethodLabel(
                      request.refundMethod
                    )
                  }}
                </p>

              </div>

              <!-- SUPPORT STAFF -->

              <div class="rounded-xl
                       bg-slate-50
                       border border-slate-200
                       p-3">

                <p class="text-[8px]
                         uppercase
                         tracking-wider
                         font-black
                         text-slate-400">
                  Support Staff
                </p>

                <p class="text-xs
                         font-bold
                         text-slate-700
                         mt-1
                ">
                  {{
                    request.supportStaffId ||
                    'Not assigned'
                  }}
                </p>

              </div>

            </div>

            <!-- DESCRIPTION -->

            <div v-if="request.description" class="mt-3
                     rounded-xl
                     bg-blue-50/60
                     border border-blue-100
                     p-3">

              <p class="text-[8px]
                       uppercase
                       tracking-wider
                       font-black
                       text-blue-400">
                Your Description
              </p>

              <p class="text-xs
                       text-slate-600
                       mt-1
                       leading-5">
                {{ request.description }}
              </p>

            </div>

            <!-- EVIDENCE -->

            <div v-if="request.evidenceImage" class="mt-3
                     flex
                     items-center
                     gap-3">

              <img :src="getEvidenceUrl(
                request.evidenceImage
              )
                " alt="Return evidence" @error="handleEvidenceImageError" class="w-16
                       h-16
                       rounded-xl
                       object-cover
                       border border-slate-200" />

              <div>

                <p class="text-[9px]
                         uppercase
                         tracking-wider
                         font-black
                         text-slate-400">
                  Evidence
                </p>

                <p class="text-xs
                         font-bold
                         text-emerald-600
                         mt-1">
                  Evidence image submitted
                </p>

              </div>

            </div>

            <!-- STATUS MESSAGE -->

            <div class="mt-4
                     rounded-xl
                     px-3
                     py-2.5
                     text-[10px]
                     font-bold" :class="String(request.status).toUpperCase() === 'APPROVED' ||
                      String(request.status).toUpperCase() === 'COMPLETED' ||
                      String(request.status).toUpperCase() === 'REFUNDED'
                      ? 'bg-emerald-50 text-emerald-700 border border-emerald-100'
                      : String(request.status).toUpperCase() === 'REJECTED' ||
                        String(request.status).toUpperCase() === 'DENIED'
                        ? 'bg-red-50 text-red-700 border border-red-100'
                        : 'bg-amber-50 text-amber-700 border border-amber-100'
                      ">

              <span v-if="
                String(request.status).toUpperCase() ===
                'APPROVED' ||
                String(request.status).toUpperCase() ===
                'COMPLETED' ||
                String(request.status).toUpperCase() ===
                'REFUNDED'
              ">
                ✓ Your return request has been approved
                and the refund process is being handled.
              </span>

              <span v-else-if="
                String(request.status).toUpperCase() ===
                'REJECTED' ||
                String(request.status).toUpperCase() ===
                'DENIED'
              ">
                Your return request was not approved.
                Please contact customer support for more
                information.
              </span>

              <span v-else>
                Your return request is currently being
                reviewed by our support team.
              </span>

            </div>

          </div>

        </div>

      </section>

      <!-- =================================================
           SUCCESS
      ================================================== -->

      <div v-if="submitSuccess" class="relative
               overflow-hidden
               rounded-[30px]
               bg-white/70
               backdrop-blur-2xl
               border border-white/90
               shadow-2xl
               shadow-slate-300/25
               p-7
               sm:p-10
               text-center
               mb-7">

        <div class="absolute
                 -top-20
                 -right-20
                 w-48
                 h-48
                 rounded-full
                 bg-emerald-400/10
                 blur-3xl"></div>

        <div class="absolute
                 -bottom-20
                 -left-20
                 w-48
                 h-48
                 rounded-full
                 bg-blue-400/10
                 blur-3xl"></div>

        <div class="relative">

          <div class="w-16
                   h-16
                   mx-auto
                   rounded-2xl
                   bg-emerald-50
                   border border-emerald-100
                   text-emerald-600
                   flex
                   items-center
                   justify-center">

            <svg class="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">

              <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />

            </svg>

          </div>

          <h2 class="text-2xl
                   font-black
                   text-slate-900
                   mt-5">
            Return Request Submitted
          </h2>

          <p class="text-slate-500
                   text-sm
                   max-w-md
                   mx-auto
                   mt-2
                   leading-6">

            Your return request for order

            <strong class="text-blue-600
                     font-mono">
              {{ selectedOrderId }}
            </strong>

            has been submitted successfully.

          </p>

          <p v-if="submittedReturn" class="text-slate-400
                   text-[11px]
                   font-mono
                   mt-1">

            Reference:
            {{ submittedReturn.returnId }}

          </p>

          <div class="inline-flex
                   items-center
                   gap-2
                   mt-4
                   px-3
                   py-2
                   rounded-xl
                   bg-blue-50
                   border border-blue-100
                   text-blue-600
                   text-[11px]
                   font-bold">

            <span class="w-2
                     h-2
                     rounded-full
                     bg-blue-500"></span>

            Support team review pending

          </div>

          <div v-if="submittedReturn" class="max-w-md
                   mx-auto
                   mt-6
                   grid
                   grid-cols-1
                   sm:grid-cols-2
                   gap-3">

            <div class="rounded-xl
                     bg-white/80
                     border border-slate-200
                     p-3
                     text-left">

              <p class="text-[9px]
                       uppercase
                       tracking-wider
                       font-black
                       text-slate-400">
                Status
              </p>

              <p class="text-xs
                       font-bold
                       text-amber-600
                       mt-1">
                {{
                  formatStatus(
                    submittedReturn.status
                  )
                }}
              </p>

            </div>

            <div class="rounded-xl
                     bg-white/80
                     border border-slate-200
                     p-3
                     text-left">

              <p class="text-[9px]
                       uppercase
                       tracking-wider
                       font-black
                       text-slate-400">
                Refund Method
              </p>

              <p class="text-xs
                       font-bold
                       text-slate-700
                       mt-1">
                {{
                  getRefundMethodLabel(
                    submittedReturn.refundMethod ||
                    selectedRefundMethod
                  )
                }}
              </p>

            </div>

          </div>

          <div class="pt-6
                   flex
                   flex-col
                   sm:flex-row
                   justify-center
                   gap-3">

            <button type="button" @click="router.push('/orders')" class="px-6
                     py-3
                     bg-white
                     border border-slate-200
                     hover:border-blue-300
                     hover:bg-blue-50
                     text-slate-600
                     hover:text-blue-600
                     rounded-xl
                     text-xs
                     font-bold
                     transition-all
                     shadow-sm">
              View Orders
            </button>

            <button type="button" @click="startNewRequest" class="px-6
                     py-3
                     bg-gradient-to-r
                     from-blue-600
                     to-cyan-500
                     hover:from-blue-500
                     hover:to-cyan-400
                     text-white
                     rounded-xl
                     text-xs
                     font-bold
                     transition-all
                     shadow-lg
                     shadow-blue-500/20">
              New Return Request
            </button>

          </div>

        </div>

      </div>

      <!-- =================================================
           MAIN FORM
      ================================================== -->

      <form v-else @submit.prevent="handleReturnSubmit" enctype="multipart/form-data" class="space-y-5">

        <!-- =================================================
             STEP 1
        ================================================== -->

        <div class="relative
                 overflow-hidden
                 bg-white/65
                 backdrop-blur-2xl
                 border border-white/90
                 rounded-[28px]
                 shadow-xl
                 shadow-slate-300/15
                 p-5
                 sm:p-6">

          <div class="flex
                   items-center
                   gap-3
                   mb-4">

            <div class="w-9
                     h-9
                     rounded-xl
                     bg-blue-50
                     text-blue-600
                     border border-blue-100
                     flex
                     items-center
                     justify-center
                     text-xs
                     font-black">
              01
            </div>

            <div>

              <p class="text-[9px]
                       uppercase
                       tracking-[0.15em]
                       font-black
                       text-blue-500">
                Step 1
              </p>

              <h2 class="text-sm
                       font-black
                       text-slate-900">
                Choose Order
              </h2>

            </div>

          </div>

          <div v-if="isLoading" class="flex
                   items-center
                   justify-center
                   gap-2
                   py-8
                   text-slate-400
                   text-xs">

            <svg class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">

              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />

              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />

            </svg>

            Loading orders...

          </div>

          <select v-else v-model="selectedOrderId" class="w-full
                   bg-white/80
                   border border-slate-200
                   rounded-xl
                   px-4
                   py-3
                   text-sm
                   text-slate-700
                   font-medium
                   focus:outline-none
                   focus:border-blue-400
                   focus:ring-4
                   focus:ring-blue-500/10
                   cursor-pointer
                   transition-all">

            <option value="" disabled>
              Select an eligible order...
            </option>

            <option v-for="order in orders" :key="order.id" :value="order.id">

              {{ order.id }}
              —
              Placed on
              {{ formatDate(order.date) }}
              {{ getActiveReturnForOrder(order.id) ? ' (Active Request: ' + formatStatus(getActiveReturnForOrder(order.id)!.status) + ')' : '' }}

            </option>

          </select>

        </div>

        <!-- =================================================
             STEP 2
        ================================================== -->

        <div v-if="currentOrder" class="relative
                 overflow-hidden
                 bg-white/65
                 backdrop-blur-2xl
                 border border-white/90
                 rounded-[28px]
                 shadow-xl
                 shadow-slate-300/15
                 p-5
                 sm:p-6">

          <!-- Active Return Request Warning Banner -->
          <div v-if="activeReturnForSelectedOrder" class="mb-5 flex items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50/90 p-4 text-amber-800 shadow-sm">
            <svg class="h-5 w-5 shrink-0 text-amber-600 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <div>
              <p class="text-sm font-bold text-amber-900">Return Request Already Active</p>
              <p class="mt-1 text-xs text-amber-700 leading-relaxed">
                Order <strong>{{ selectedOrderId }}</strong> already has an active return request with status 
                <span class="font-semibold uppercase text-amber-800">{{ formatStatus(activeReturnForSelectedOrder.status) }}</span>.
                An order can only have one active request at a time. If support rejects your request, you may submit a new request.
              </p>
            </div>
          </div>

          <div class="flex
                   items-center
                   justify-between
                   gap-3
                   mb-5">

            <div class="flex
                     items-center
                     gap-3">

              <div class="w-9
                       h-9
                       rounded-xl
                       bg-cyan-50
                       text-cyan-600
                       border border-cyan-100
                       flex
                       items-center
                       justify-center
                       text-xs
                       font-black">
                02
              </div>

              <div>

                <p class="text-[9px]
                         uppercase
                         tracking-[0.15em]
                         font-black
                         text-cyan-500">
                  Step 2
                </p>

                <h2 class="text-sm
                         font-black
                         text-slate-900">
                  Order Items
                </h2>

              </div>

            </div>

            <span class="px-3
                     py-1.5
                     rounded-full
                     bg-cyan-50
                     border border-cyan-100
                     text-cyan-600
                     text-[10px]
                     font-black">
              {{ currentOrderItemCount }}
              Items
            </span>

          </div>

          <div class="space-y-3">

            <div v-for="item in currentOrder.items" :key="item.id" class="rounded-2xl
                     bg-white/80
                     border border-slate-200/80
                     p-3
                     sm:p-4
                     flex
                     items-center
                     gap-4">

              <div class="w-20
                       h-20
                       sm:w-24
                       sm:h-24
                       shrink-0
                       rounded-2xl
                       bg-slate-100
                       border border-slate-200
                       overflow-hidden">

                <img :src="getProductImage(
                  item.productId
                )
                  " :alt="item.name" @error="
                    handleProductImageError
                  " class="w-full
                         h-full
                         object-cover" />

              </div>

              <div class="flex-1
                       min-w-0">

                <h3 class="text-sm
                         sm:text-base
                         font-black
                         text-slate-800">
                  {{ item.name }}
                </h3>

                <p v-if="item.productId" class="text-[10px]
                         text-slate-400
                         font-mono
                         mt-1">

                  Product ID:
                  {{ item.productId }}

                </p>

                <div class="flex
                         items-center
                         justify-between
                         mt-3">

                  <span class="text-[10px]
                           font-bold
                           text-slate-500">

                    Quantity:
                    {{ item.quantity }}

                  </span>

                  <span class="text-sm
                           font-black
                           text-slate-900">

                    {{ formatCurrency(item.price) }}

                  </span>

                </div>

              </div>

            </div>

          </div>

          <div class="mt-5
                   rounded-2xl
                   bg-gradient-to-r
                   from-blue-50
                   to-cyan-50
                   border border-blue-100
                   p-4">

            <div class="flex
                     items-center
                     justify-between">

              <div>

                <p class="text-[9px]
                         uppercase
                         tracking-wider
                         font-black
                         text-slate-400">
                  Order Total
                </p>

                <p class="text-xs
                         text-slate-500
                         mt-1">

                  {{ currentOrderItemCount }}
                  product items

                </p>

              </div>

              <span class="text-lg
                       font-black
                       text-slate-900">

                {{
                  formatCurrency(
                    currentOrderTotal
                  )
                }}

              </span>

            </div>

          </div>

        </div>

        <!-- =================================================
             STEP 3
        ================================================== -->

        <div v-if="currentOrder" class="relative
                 overflow-hidden
                 bg-white/65
                 backdrop-blur-2xl
                 border border-white/90
                 rounded-[28px]
                 shadow-xl
                 shadow-slate-300/15
                 p-5
                 sm:p-6">

          <div class="flex
                   items-center
                   gap-3
                   mb-5">

            <div class="w-9
                     h-9
                     rounded-xl
                     bg-purple-50
                     text-purple-600
                     border border-purple-100
                     flex
                     items-center
                     justify-center
                     text-xs
                     font-black">
              03
            </div>

            <div>

              <p class="text-[9px]
                       uppercase
                       tracking-[0.15em]
                       font-black
                       text-purple-500">
                Step 3
              </p>

              <h2 class="text-sm
                       font-black
                       text-slate-900">
                Reason for Return
              </h2>

            </div>

          </div>

          <label class="block
                   text-[10px]
                   font-black
                   text-slate-500
                   uppercase
                   tracking-wider
                   mb-2">

            Reason

            <span class="text-red-500">*</span>

          </label>

          <select v-model="selectedReason" required class="w-full
                   bg-white/80
                   border border-slate-200
                   rounded-xl
                   px-4
                   py-3
                   text-sm
                   text-slate-700
                   font-medium
                   focus:outline-none
                   focus:border-blue-400
                   focus:ring-4
                   focus:ring-blue-500/10
                   cursor-pointer
                   transition-all">

            <option value="" disabled>
              Select a reason...
            </option>

            <option v-for="reason in reasonsList" :key="reason" :value="reason">
              {{ reason }}
            </option>

          </select>

          <div class="mt-5">

            <div class="flex
                     items-center
                     justify-between
                     mb-2">

              <label class="block
                       text-[10px]
                       font-black
                       text-slate-500
                       uppercase
                       tracking-wider">

                Description

                <span class="text-red-500">*</span>

              </label>

              <span class="text-[9px]
                       text-slate-400">
                {{ returnDescription.length }}/500
              </span>

            </div>

            <textarea v-model="returnDescription" required maxlength="500" rows="5"
              placeholder="Please describe the problem with your product..." class="w-full
                     bg-white/80
                     border border-slate-200
                     rounded-xl
                     px-4
                     py-3
                     text-sm
                     text-slate-700
                     placeholder:text-slate-400
                     font-medium
                     resize-none
                     focus:outline-none
                     focus:border-blue-400
                     focus:ring-4
                     focus:ring-blue-500/10
                     transition-all"></textarea>

            <p class="text-[10px]
                     text-slate-400
                     mt-2">
              Please provide details that can help
              our support team understand the issue.
            </p>

          </div>

        </div>

        <!-- =================================================
             STEP 4
        ================================================== -->

        <div v-if="currentOrder" class="relative
                 overflow-hidden
                 bg-white/65
                 backdrop-blur-2xl
                 border border-white/90
                 rounded-[28px]
                 shadow-xl
                 shadow-slate-300/15
                 p-5
                 sm:p-6">

          <div class="flex
                   items-center
                   gap-3
                   mb-6">

            <div class="w-9
                     h-9
                     rounded-xl
                     bg-emerald-50
                     text-emerald-600
                     border border-emerald-100
                     flex
                     items-center
                     justify-center
                     text-xs
                     font-black">
              04
            </div>

            <div>

              <p class="text-[9px]
                       uppercase
                       tracking-[0.15em]
                       font-black
                       text-emerald-500">
                Step 4
              </p>

              <h2 class="text-sm
                       font-black
                       text-slate-900">
                Refund & Evidence
              </h2>

            </div>

          </div>

          <!-- REFUND METHOD -->

          <div>

            <label class="block
                     text-[10px]
                     font-black
                     text-slate-500
                     uppercase
                     tracking-wider
                     mb-3">

              Refund Type

              <span class="text-red-500">*</span>

            </label>

            <div class="space-y-3">

              <label v-for="method in refundMethods" :key="method.value" class="flex
                       items-start
                       gap-3
                       p-4
                       rounded-2xl
                       bg-white/80
                       border
                       cursor-pointer
                       transition-all" :class="selectedRefundMethod ===
                        method.value
                        ? 'border-blue-400 bg-blue-50/50 ring-2 ring-blue-500/10'
                        : 'border-slate-200 hover:border-blue-200'
                        ">

                <input v-model="selectedRefundMethod" type="radio" name="refundMethod" :value="method.value" class="mt-1
                         w-4
                         h-4
                         text-blue-600
                         border-slate-300
                         focus:ring-blue-500" />

                <div>

                  <p class="text-sm
                           font-black
                           text-slate-800">
                    {{ method.label }}
                  </p>

                  <p class="text-[10px]
                           text-slate-400
                           mt-1
                           leading-5">
                    {{ method.description }}
                  </p>

                </div>

              </label>

            </div>

          </div>

          <div class="my-6
                   h-px
                   bg-slate-200"></div>

          <!-- EVIDENCE -->

          <div>

            <div class="flex
                     items-center
                     justify-between
                     mb-2">

              <label class="block
                       text-[10px]
                       font-black
                       text-slate-500
                       uppercase
                       tracking-wider">

                Evidence Image

                <span class="text-slate-400
                         normal-case">
                  (Optional)
                </span>

              </label>

              <span class="text-[9px]
                       text-slate-400">
                JPG, JPEG, PNG · Max 5 MB
              </span>

            </div>

            <div class="mb-3
                     px-3
                     py-2.5
                     rounded-xl
                     bg-slate-50
                     border border-slate-200
                     text-[10px]
                     text-slate-500">

              An evidence image is optional.
              You can submit the return request
              without uploading a photo.

            </div>

            <!-- UPLOAD -->

            <div v-if="!evidenceImage" class="relative
                     rounded-2xl
                     border-2
                     border-dashed
                     border-slate-300
                     bg-white/70
                     hover:border-blue-400
                     hover:bg-blue-50/30
                     transition-all">

              <input type="file" accept="image/png,image/jpeg,image/jpg" @change="handleEvidenceImage" class="absolute
                       inset-0
                       w-full
                       h-full
                       opacity-0
                       cursor-pointer" />

              <div class="flex
                       flex-col
                       items-center
                       justify-center
                       py-10
                       px-5
                       text-center
                       pointer-events-none">

                <div class="w-14
                         h-14
                         rounded-2xl
                         bg-blue-50
                         border border-blue-100
                         text-blue-600
                         flex
                         items-center
                         justify-center
                         mb-4">

                  <svg class="w-7 h-7" fill="none" viewBox="0 0 24 24" stroke="currentColor">

                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                      d="M12 16V4m0 0-4 4m4-4 4 4" />

                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                      d="M4 16v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2" />

                  </svg>

                </div>

                <p class="text-sm
                         font-black
                         text-slate-700">
                  Upload Evidence Image
                </p>

                <p class="text-[10px]
                         text-slate-400
                         mt-2">
                  Optional photo showing damaged,
                  defective, or incorrect items.
                </p>

                <span class="inline-flex
                         items-center
                         gap-2
                         mt-4
                         px-4
                         py-2
                         rounded-xl
                         bg-blue-600
                         text-white
                         text-[10px]
                         font-bold">
                  Choose Image
                </span>

              </div>

            </div>

            <!-- PREVIEW -->

            <div v-else class="rounded-2xl
                     bg-white/80
                     border border-slate-200
                     p-4">

              <div class="flex
                       flex-col
                       sm:flex-row
                       gap-4">

                <div class="w-full
                         sm:w-40
                         h-48
                         sm:h-32
                         rounded-xl
                         overflow-hidden
                         bg-slate-100
                         border border-slate-200
                         shrink-0">

                  <img :src="evidencePreview" alt="Return evidence preview" class="w-full
                           h-full
                           object-cover" />

                </div>

                <div class="flex-1
                         flex
                         flex-col
                         justify-between">

                  <div>

                    <div class="flex
                             items-center
                             gap-2">

                      <div class="w-8
                               h-8
                               rounded-lg
                               bg-emerald-50
                               text-emerald-600
                               flex
                               items-center
                               justify-center">

                        <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">

                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M5 13l4 4L19 7" />

                        </svg>

                      </div>

                      <span class="text-xs
                               font-black
                               text-emerald-600">
                        Evidence uploaded
                      </span>

                    </div>

                    <p class="text-xs
                             font-bold
                             text-slate-700
                             mt-3
                             break-all">
                      {{ evidenceImage.name }}
                    </p>

                    <p class="text-[10px]
                             text-slate-400
                             mt-1">
                      {{
                        formatFileSize(
                          evidenceImage.size
                        )
                      }}
                    </p>

                  </div>

                  <button type="button" @click="removeEvidenceImage" class="mt-4
                           sm:mt-0
                           inline-flex
                           items-center
                           justify-center
                           gap-2
                           w-fit
                           px-3
                           py-2
                           rounded-lg
                           bg-red-50
                           border border-red-100
                           text-red-600
                           text-[10px]
                           font-bold
                           hover:bg-red-100
                           transition-all">

                    Remove Image

                  </button>

                </div>

              </div>

            </div>

          </div>

        </div>

        <!-- ERROR -->

        <div v-if="submitError" class="rounded-2xl
                 bg-red-50
                 border border-red-100
                 text-red-600
                 text-xs
                 font-bold
                 px-4
                 py-3">
          {{ submitError }}
        </div>

        <!-- SUBMIT -->

        <button type="submit" :disabled="!canSubmit ||
          isSubmitting
          " class="w-full
                 py-3.5
                 px-6
                 bg-gradient-to-r
                 from-blue-600
                 via-indigo-600
                 to-cyan-500
                 hover:from-blue-500
                 hover:via-indigo-500
                 hover:to-cyan-400
                 text-white
                 font-bold
                 rounded-2xl
                 shadow-lg
                 shadow-blue-500/20
                 transition-all
                 flex
                 items-center
                 justify-center
                 gap-2
                 disabled:opacity-40
                 disabled:cursor-not-allowed">

          <svg v-if="isSubmitting" class="animate-spin w-5 h-5" fill="none" viewBox="0 0 24 24">

            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />

            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />

          </svg>

          <svg v-else class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">

            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M22 2 11 13" />

            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="m22 2-7 20-4-9-9-4 20-7Z" />

          </svg>

          <span>
            {{
              isSubmitting
                ? 'Submitting Return Request...'
                : 'Submit Return Request'
            }}
          </span>

        </button>

        <p class="text-center
                 text-[10px]
                 text-slate-400
                 px-4">
          Select an order, return reason,
          provide a description, and choose
          a refund type before submitting.
          Evidence image is optional.
        </p>

      </form>

    </div>

  </div>

</template>