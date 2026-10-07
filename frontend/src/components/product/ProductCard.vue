<script lang="ts">
import api from '../../services/api'

/* =========================================================
   TYPES
========================================================= */

export interface Product {
  id?: string | number
  productId?: string | number
  productID?: string | number

  name: string
  brand?: string

  category?: string
  categoryName?: string

  price: number

  image?: string
  images?: string[]

  rating?: number
  reviewCount?: number
  reviews?: any[]

  description?: string | null
  discription?: string | null

  stockQty?: number
  inStock: boolean
}

/* =========================================================
   SHARED WISHLIST CACHE
========================================================= */

let wishlistPromise: Promise<Set<string>> | null = null
let wishlistCustomerId: string | null = null

export function loadWishlist(customerId: string): Promise<Set<string>> {
  if (wishlistCustomerId !== customerId) {
    wishlistPromise = null
    wishlistCustomerId = customerId
  }

  wishlistPromise ??= api
    .get(`/customers/${customerId}/wishlist`)
    .then((response) =>
      new Set<string>(
        (response.data?.productIds ?? []).map((id: string | number) => String(id))
      )
    )
    .catch((error) => {
      console.error('Error fetching wishlist:', error)
      wishlistPromise = null
      return new Set<string>()
    })

  return wishlistPromise
}

// Created ONCE and shared by every card (was re-created on every call before)
const priceFormatter = new Intl.NumberFormat('en-LK')
const dateFormatter = new Intl.DateTimeFormat('en-LK', {
  day: '2-digit',
  month: 'short',
  year: 'numeric'
})
</script>

<script setup lang="ts">
import { ref, shallowRef, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import router from '../../router'
import { useCartStore } from '../../stores/cartStore'
import { usePopup } from '../../composables/usePopup'

/* =========================================================
   TYPES
========================================================= */

interface Review {
  reviewId: string | number
  rating: number
  comment?: string
  reviewDate?: string
  customerId?: string | number
  customerName?: string
  productId?: string | number
}

/* =========================================================
   PROPS / STORES
========================================================= */

const props = defineProps<{
  product: Product
}>()

const cartStore = useCartStore()
const popup = usePopup()

/* =========================================================
   MODAL STATE
========================================================= */

const showModal = ref(false)
const selectedImage = ref('')
const modalQuantity = ref(1)

/* =========================================================
   CART & BUY NOW STATE
========================================================= */

const isAddingToCart = ref(false)
const isBuyingNow = ref(false)
const justAdded = ref(false)

/* =========================================================
   WISHLIST STATE
========================================================= */

const isWishlisted = ref(false)
const wishlistLoading = ref(false)

/* =========================================================
   REVIEW STATE
========================================================= */

// shallowRef: the list is always replaced, never mutated deeply
const reviews = shallowRef<Review[]>([])
const reviewsLoading = ref(false)
const reviewsError = ref<string | null>(null)
const reviewsLoaded = ref(false)
const reviewLimit = ref(5)

const reviewCount = ref(Number(props.product.reviewCount) || 0)

watch(
  () => props.product.reviewCount,
  (newCount) => {
    if (!reviewsLoaded.value) {
      reviewCount.value = Number(newCount) || 0
    }
  },
  { immediate: true }
)

watch(
  () => props.product.reviews,
  (newReviews) => {
    if (!reviewsLoaded.value && Array.isArray(newReviews) && newReviews.length > 0) {
      reviews.value = newReviews as Review[]
      reviewsLoaded.value = true
      reviewCount.value = newReviews.length
    }
  },
  { immediate: true }
)

const newRating = ref(5)
const newComment = ref('')

const submittingReview = ref(false)
const submitReviewError = ref<string | null>(null)
const showReviewForm = ref(false)

// Inline notifications and in-modal actions (no blocking popups)
const reviewFeedbackMessage = ref<string | null>(null)
const reviewFeedbackIsError = ref(false)
const confirmDeleteReviewId = ref<string | number | null>(null)
const isDeletingReview = ref(false)
const modalCustomerId = ref<string | null>(null)

let feedbackTimer: ReturnType<typeof setTimeout> | undefined
let addedTimer: ReturnType<typeof setTimeout> | undefined

function showFeedback(message: string, isError = false, ms = 4500) {
  clearTimeout(feedbackTimer)
  reviewFeedbackIsError.value = isError
  reviewFeedbackMessage.value = message
  feedbackTimer = setTimeout(() => {
    reviewFeedbackMessage.value = null
  }, ms)
}

/* =========================================================
   IMAGE CLEANER
========================================================= */

function cleanImageUrl(value: unknown): string {
  if (!value || typeof value !== 'string') {
    return ''
  }

  let url = value.trim()
  if (!url) return ''

  const markdownIndex = url.indexOf('](')
  if (
    (url.startsWith('[') || url.startsWith('![')) &&
    markdownIndex !== -1 &&
    url.endsWith(')')
  ) {
    url = url.substring(markdownIndex + 2, url.length - 1).trim()
  }

  if (url.startsWith('<') && url.endsWith('>')) {
    url = url.substring(1, url.length - 1).trim()
  }

  if (
    (url.startsWith('"') && url.endsWith('"')) ||
    (url.startsWith("'") && url.endsWith("'"))
  ) {
    url = url.substring(1, url.length - 1).trim()
  }

  return url
}

/* =========================================================
   PRODUCT IMAGES
========================================================= */

const productImages = computed<string[]>(() => {
  const rawImages: unknown[] = []

  if (props.product.image) {
    rawImages.push(props.product.image)
  }

  if (Array.isArray(props.product.images)) {
    rawImages.push(...props.product.images)
  }

  const seen = new Set<string>()
  rawImages.forEach((image) => {
    const cleanUrl = cleanImageUrl(image)
    if (cleanUrl) seen.add(cleanUrl)
  })

  return [...seen]
})

const mainImage = computed(() => {
  return productImages.value[0] || ''
})

const activeImage = computed(() => selectedImage.value || mainImage.value)

const activeImageIndex = computed(() =>
  Math.max(productImages.value.indexOf(activeImage.value), 0)
)

/* =========================================================
   PRODUCT INFORMATION
========================================================= */

const description = computed(() => {
  const value = props.product.description || props.product.discription
  return value?.trim() || 'High-performance enthusiast grade hardware engineered for supreme reliability and speed.'
})

const categoryName = computed(() => {
  return props.product.category || props.product.categoryName || 'Performance Hardware'
})

const maxStock = computed(() => {
  return props.product.stockQty !== undefined ? props.product.stockQty : 99
})

const priceText = computed(() => priceFormatter.format(props.product.price ?? 0))

/* =========================================================
   RATING
========================================================= */

const displayRating = computed(() => {
  const validRatings = reviews.value
    .map((review) => Number(review.rating))
    .filter((rating) => Number.isFinite(rating) && rating > 0)

  if (validRatings.length > 0) {
    const total = validRatings.reduce((sum, rating) => sum + rating, 0)
    return Math.min(5, total / validRatings.length)
  }

  const fallback = Number(props.product.rating)
  return Number.isFinite(fallback) ? Math.min(5, Math.max(0, fallback)) : 0
})

const roundedRating = computed(() => {
  return Math.round(displayRating.value)
})

// Drives the single-element CSS star display (replaces 5 SVGs per rating)
const ratingPercent = computed(() => `${(roundedRating.value / 5) * 100}%`)

function starPercent(rating: unknown) {
  const value = Math.min(5, Math.max(0, Math.round(Number(rating) || 0)))
  return `${(value / 5) * 100}%`
}

/* =========================================================
   RATING DISTRIBUTION (single pass instead of 5 filters)
========================================================= */

const ratingDistribution = computed(() => {
  const total = reviews.value.length
  const counts = [0, 0, 0, 0, 0, 0]

  reviews.value.forEach((review) => {
    const star = Math.round(Number(review.rating))
    if (star >= 1 && star <= 5) counts[star]++
  })

  return [5, 4, 3, 2, 1].map((star) => ({
    star,
    count: counts[star],
    percentage: total > 0 ? Math.round((counts[star] / total) * 100) : 0
  }))
})

// Only render a few reviews at first; the rest on demand
const visibleReviews = computed(() => reviews.value.slice(0, reviewLimit.value))

/* =========================================================
   IMAGE ERROR
========================================================= */

function handleImageError(event: Event) {
  const image = event.target as HTMLImageElement
  if (!image) return
  image.style.opacity = '0'
}

/* =========================================================
   MODAL CONTROLS
========================================================= */

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') closeModal()
}

// Lock page scroll while the modal is open (stops the heavy grid
// behind from scrolling/repainting) and only listen for keys when needed.
watch(showModal, (open) => {
  if (open) {
    document.documentElement.style.overflow = 'hidden'
    window.addEventListener('keydown', onKeydown)
  } else {
    document.documentElement.style.overflow = ''
    window.removeEventListener('keydown', onKeydown)
  }
})

onBeforeUnmount(() => {
  clearTimeout(feedbackTimer)
  clearTimeout(addedTimer)
  window.removeEventListener('keydown', onKeydown)
  if (showModal.value) document.documentElement.style.overflow = ''
})

function openModal() {
  selectedImage.value = productImages.value[0] || ''
  modalQuantity.value = 1
  modalCustomerId.value = getSilentCustomerId()
  reviewLimit.value = 5
  showModal.value = true
  showReviewForm.value = false
  submitReviewError.value = null
  reviewFeedbackMessage.value = null
  confirmDeleteReviewId.value = null

  if (!reviewsLoaded.value) {
    fetchReviews()
  }
}

function closeModal() {
  showModal.value = false
  showReviewForm.value = false
  confirmDeleteReviewId.value = null
}

function goToProductDetailPage() {
  const id = getProductId()
  if (id) {
    closeModal()
    router.push(`/product/${id}`)
  }
}

function incrementQuantity() {
  if (modalQuantity.value < maxStock.value) {
    modalQuantity.value++
  }
}

function decrementQuantity() {
  if (modalQuantity.value > 1) {
    modalQuantity.value--
  }
}

/* =========================================================
   GALLERY CONTROLS
========================================================= */

function selectImage(image: string) {
  selectedImage.value = image
}

function previousImage() {
  const images = productImages.value
  if (images.length <= 1) return

  const currentIndex = activeImageIndex.value
  const previousIndex = currentIndex <= 0 ? images.length - 1 : currentIndex - 1
  selectedImage.value = images[previousIndex]
}

function nextImage() {
  const images = productImages.value
  if (images.length <= 1) return

  const currentIndex = activeImageIndex.value
  const nextIndex = currentIndex >= images.length - 1 ? 0 : currentIndex + 1
  selectedImage.value = images[nextIndex]
}

function scrollToReviews() {
  const section = document.getElementById('product-reviews-section')
  if (!section) return
  section.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

/* =========================================================
   IDS & CUSTOMER SESSION
========================================================= */

function getProductId(): string | null {
  const id = props.product.productId ?? props.product.productID ?? props.product.id
  if (id === undefined || id === null || id === '') return null
  return String(id)
}

async function getCustomerId(): Promise<string | null> {
  const storedUser = sessionStorage.getItem('user') || localStorage.getItem('user')

  if (!storedUser) {
    await popup.warning('Please log in first to continue with your purchase.', 'Sign In Required')
    router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
    return null
  }

  try {
    const user = JSON.parse(storedUser)
    const customerId = user.customerId ?? user.userId ?? user.id

    if (customerId === undefined || customerId === null || customerId === '') {
      await popup.error('Customer session information could not be found. Please log in again.', 'Account Error')
      router.push('/login')
      return null
    }

    return String(customerId)
  } catch (error) {
    console.error('Invalid user session:', error)
    sessionStorage.removeItem('user')
    localStorage.removeItem('user')
    await popup.warning('Your session is invalid. Please login again.', 'Session Expired')
    router.push('/login')
    return null
  }
}

function getSilentCustomerId(): string | null {
  const storedUser = sessionStorage.getItem('user') || localStorage.getItem('user')
  if (!storedUser) return null
  try {
    const user = JSON.parse(storedUser)
    const customerId = user.customerId ?? user.userId ?? user.id
    if (customerId === undefined || customerId === null || customerId === '') return null
    return String(customerId)
  } catch {
    return null
  }
}

function isOwnReview(review: Review): boolean {
  if (!modalCustomerId.value || review.customerId === undefined) return false
  return String(review.customerId) === modalCustomerId.value
}

function formatReviewDate(value?: string): string {
  if (!value) return 'Recently'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return dateFormatter.format(date)
}

/* =========================================================
   INITIAL WISHLIST
========================================================= */

onMounted(async () => {
  const customerId = getSilentCustomerId()
  const productId = getProductId()
  if (!customerId || !productId) return

  const set = await loadWishlist(customerId)
  isWishlisted.value = set.has(productId)
})

/* =========================================================
   CART ACTIONS
========================================================= */

async function addToCart(qty: number = 1) {
  if (!props.product.inStock || isAddingToCart.value) return

  const customerId = await getCustomerId()
  const productId = getProductId()
  if (!customerId) return

  if (!productId) {
    await popup.error('Product ID not found.', 'Error')
    return
  }

  try {
    isAddingToCart.value = true
    await cartStore.addItem(productId, qty)

    justAdded.value = true
    clearTimeout(addedTimer)
    addedTimer = setTimeout(() => {
      justAdded.value = false
    }, 2000)

    if (showModal.value) {
      closeModal()
    }
  } catch (error) {
    console.error('Add to cart error:', error)
    await popup.error('Unable to add product to cart. Please try again.', 'Cart Error')
  } finally {
    isAddingToCart.value = false
  }
}

async function buyNow(qty: number = 1) {
  if (!props.product.inStock || isBuyingNow.value) return

  const customerId = await getCustomerId()
  if (!customerId) return

  const productId = getProductId()
  if (!productId) {
    await popup.error('Product ID not found.', 'Error')
    return
  }

  try {
    isBuyingNow.value = true
    await cartStore.addItem(productId, qty)

    if (showModal.value) {
      closeModal()
    }

    await router.push('/checkout')
  } catch (error: any) {
    console.error('Buy now error:', error)

    // If item was already added to cart or present in cart, still navigate to checkout
    if (cartStore.items.some((i) => String(i.productId) === String(productId))) {
      if (showModal.value) {
        closeModal()
      }
      await router.push('/checkout')
      return
    }

    const msg = error?.response?.data?.message || error?.message || 'Unable to continue with purchase.'
    await popup.error(msg, 'Checkout Error')
  } finally {
    isBuyingNow.value = false
  }
}

/* =========================================================
   WISHLIST ACTION
========================================================= */

async function toggleWishlist() {
  if (wishlistLoading.value) return

  const customerId = await getCustomerId()
  const productId = getProductId()
  if (!customerId || !productId) return

  wishlistLoading.value = true

  try {
    const set = await loadWishlist(customerId)

    if (isWishlisted.value) {
      await api.delete(`/customers/${customerId}/wishlist/products/${productId}`)
      set.delete(productId)
      isWishlisted.value = false
    } else {
      await api.post(`/customers/${customerId}/wishlist/products/${productId}`)
      set.add(productId)
      isWishlisted.value = true
    }
  } catch (error) {
    console.error('Wishlist error:', error)
    await popup.error('Unable to update wishlist.', 'Wishlist Error')
  } finally {
    wishlistLoading.value = false
  }
}

/* =========================================================
   REVIEWS API
========================================================= */

async function fetchReviews() {
  const productId = getProductId()
  if (!productId) return

  reviewsLoading.value = true
  reviewsError.value = null

  try {
    const response = await api.get('/reviews', { params: { productId } })
    reviews.value = Array.isArray(response.data) ? response.data : []
    reviewCount.value = reviews.value.length
    reviewsLoaded.value = true
  } catch (error) {
    console.error('Error fetching reviews:', error)
    reviewsError.value = 'Unable to load reviews.'
  } finally {
    reviewsLoading.value = false
  }
}

async function toggleReviewForm() {
  if (!showReviewForm.value) {
    const customerId = await getCustomerId()
    if (!customerId) return
    modalCustomerId.value = customerId
  }

  showReviewForm.value = !showReviewForm.value
  submitReviewError.value = null
}

async function submitReview() {
  const customerId = await getCustomerId()
  const productId = getProductId()
  if (!customerId || !productId) return

  if (newRating.value < 1 || newRating.value > 5) {
    submitReviewError.value = 'Please choose a rating between 1 and 5 stars.'
    return
  }

  submittingReview.value = true
  submitReviewError.value = null

  try {
    await api.post('/reviews', {
      customerId,
      productId,
      rating: newRating.value,
      comment: newComment.value.trim()
    })

    newComment.value = ''
    newRating.value = 5
    showReviewForm.value = false

    showFeedback('Thank you! Your review has been submitted successfully.')

    await fetchReviews()
  } catch (error) {
    console.error('Error submitting review:', error)
    submitReviewError.value = 'Unable to submit review. Please try again.'
  } finally {
    submittingReview.value = false
  }
}

/* =========================================================
   DELETE REVIEW (INLINE CONFIRMATION - NO POPUP OVERLAY)
========================================================= */

function requestDeleteReview(reviewId: string | number) {
  confirmDeleteReviewId.value = reviewId
}

function cancelDeleteReview() {
  confirmDeleteReviewId.value = null
}

async function executeDeleteReview(reviewId: string | number) {
  try {
    isDeletingReview.value = true
    await api.delete(`/reviews/${reviewId}`)

    reviews.value = reviews.value.filter(
      (review) => String(review.reviewId) !== String(reviewId)
    )
    reviewCount.value = reviews.value.length
    confirmDeleteReviewId.value = null

    showFeedback('Your review was removed successfully.', false, 4000)
  } catch (error) {
    console.error('Error deleting review:', error)
    showFeedback('Failed to delete review. Please try again.', true)
  } finally {
    isDeletingReview.value = false
  }
}
</script>

<template>
  <!-- =====================================================
       PRODUCT CARD (LIGHT THEME)
  ====================================================== -->
  <article
    class="product-card group relative flex flex-col overflow-hidden rounded-2xl border border-slate-200/90 bg-white shadow-sm hover:border-blue-300 hover:shadow-lg hover:shadow-blue-500/10">

    <!-- PRODUCT IMAGE -->
    <div
      class="relative m-2 aspect-[4/3] cursor-pointer sm:m-3 overflow-hidden rounded-xl border border-slate-100 bg-gradient-to-b from-slate-50/80 via-white to-blue-50/20"
      @click="openModal">

      <img v-if="mainImage" :src="mainImage" :alt="product.name" loading="lazy" decoding="async" width="400"
        height="300" class="card-img absolute inset-0 h-full w-full object-contain p-3 sm:p-5"
        @error="handleImageError" />

      <div v-else class="absolute inset-0 flex flex-col items-center justify-center text-slate-400">
        <svg class="h-9 w-9 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
          <path stroke-linecap="round" stroke-linejoin="round"
            d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14M5 20h14a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v14a1 1 0 001 1z" />
        </svg>
        <span class="mt-2 text-[11px] font-medium text-slate-400">Image unavailable</span>
      </div>

      <!-- STOCK BADGE (no blur, no infinite animation) -->
      <span
        class="absolute left-2 top-2 inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-[9px] font-bold shadow-sm sm:left-3 sm:top-3 sm:gap-1.5 sm:px-2.5 sm:py-1 sm:text-[10px]"
        :class="product.inStock ? 'border-emerald-200 bg-emerald-50 text-emerald-700' : 'border-rose-200 bg-rose-50 text-rose-700'">
        <span class="h-1.5 w-1.5 rounded-full" :class="product.inStock ? 'bg-emerald-500' : 'bg-rose-500'"></span>
        {{ product.inStock ? 'In Stock' : 'Out of Stock' }}
      </span>

      <!-- WISHLIST BUTTON -->
      <button type="button" @click.stop="toggleWishlist" :disabled="wishlistLoading"
        class="absolute right-2 top-2 flex h-7 w-7 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-400 sm:right-3 sm:top-3 sm:h-8 sm:w-8 shadow-sm transition-colors hover:text-rose-500 hover:border-rose-200"
        :class="isWishlisted ? 'border-rose-200 text-rose-500 bg-rose-50/90' : ''"
        :title="isWishlisted ? 'Remove from wishlist' : 'Add to wishlist'">
        <svg v-if="!wishlistLoading" class="h-4 w-4" :fill="isWishlisted ? 'currentColor' : 'none'" viewBox="0 0 24 24"
          stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round"
            d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78L12 21.23l8.84-8.84a5.5 5.5 0 000-7.78Z" />
        </svg>
        <svg v-else class="h-4 w-4 animate-spin text-rose-500" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2" />
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v3a5 5 0 00-5 5H4z" />
        </svg>
      </button>

      <!-- QUICK VIEW BUTTON -->
      <button type="button" @click.stop="openModal"
        class="quick-btn absolute bottom-3 right-3 hidden h-8 w-8 sm:flex items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-600 shadow-sm hover:border-blue-300 hover:text-blue-600"
        title="Quick View Details">
        <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round"
            d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
          <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
        </svg>
      </button>
    </div>

    <!-- CARD CONTENT -->
    <div class="flex flex-1 flex-col px-3 pb-3 sm:px-4 sm:pb-4">
      <div class="mb-1.5 flex items-center justify-between gap-2 sm:mb-2">
        <span v-if="product.brand"
          class="min-w-0 truncate text-[10px] font-bold uppercase tracking-wider text-slate-400">
          {{ product.brand }}
        </span>
        <span :class="product.brand ? 'max-sm:hidden' : ''"
          class="max-w-[60%] truncate rounded-full border border-blue-200/80 bg-blue-50/80 px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider text-blue-700">
          {{ categoryName }}
        </span>
      </div>

      <h3
        class="min-h-[2.5rem] cursor-pointer text-[13px] font-bold leading-5 sm:text-sm text-slate-900 line-clamp-2 transition-colors hover:text-blue-600"
        @click="openModal">
        {{ product.name }}
      </h3>

      <p class="mt-1.5 min-h-[36px] text-xs leading-5 text-slate-500 line-clamp-2 max-sm:hidden">
        {{ description }}
      </p>

      <!-- RATING -->
      <div class="mt-2 flex items-center gap-1.5 whitespace-nowrap sm:mt-3 sm:gap-2">
        <span class="stars text-xs" :style="{ '--pct': ratingPercent }" aria-hidden="true"></span>
        <span class="text-xs font-bold text-slate-700">
          {{ displayRating > 0 ? displayRating.toFixed(1) : 'New' }}
        </span>
        <span v-if="reviewCount > 0" class="text-xs text-slate-400">
          ({{ reviewCount }})
        </span>
      </div>

      <!-- PRICE & CTA (LIGHT THEME) -->
      <div class="mt-auto pt-3">
        <div class="border-t border-slate-100 pt-2 sm:pt-3">
          <p class="text-[9px] font-bold uppercase tracking-wider text-slate-400 max-sm:hidden">Price</p>
          <p class="whitespace-nowrap text-base font-black tracking-tight text-slate-900 sm:mt-0.5 sm:text-lg">
            LKR {{ priceText }}
          </p>
        </div>

        <div class="mt-2.5 flex gap-2 sm:mt-3">
          <!-- Add to Cart (Light Outline) -->
          <button type="button" aria-label="Add to cart" @click="addToCart(1)"
            :disabled="!product.inStock || isAddingToCart"
            class="flex h-9 w-9 shrink-0 items-center justify-center gap-1.5 rounded-xl border border-slate-200 bg-white text-xs sm:h-10 sm:w-auto sm:px-3 font-bold text-slate-700 shadow-sm transition-colors hover:border-blue-300 hover:bg-blue-50/70 hover:text-blue-600 disabled:cursor-not-allowed disabled:text-slate-400">
            <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round"
                d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13l-2 2h13m-4 4a1 1 0 11-2 0m-6 0a1 1 0 11-2 0" />
            </svg>
            <span class="max-sm:hidden">{{ justAdded ? 'Added' : isAddingToCart ? 'Adding...' : 'Add' }}</span>
          </button>

          <!-- Buy Now (Electric Blue/Cyan Gradient - Light Theme) -->
          <button type="button" @click="buyNow(1)" :disabled="!product.inStock || isBuyingNow"
            class="flex h-9 min-w-0 flex-1 items-center justify-center gap-1.5 whitespace-nowrap rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-2 text-[11px] font-bold sm:h-10 sm:px-3 sm:text-xs text-white shadow-md shadow-blue-500/20 transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50">
            <svg v-if="!isBuyingNow" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"
              stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M13 2L4 14h7l-1 8 9-12h-7l1-8z" />
            </svg>
            <svg v-else class="h-4 w-4 animate-spin text-white" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2" />
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v3a5 5 0 00-5 5H4z" />
            </svg>
            {{ isBuyingNow ? 'Buying...' : 'Buy Now' }}
          </button>
        </div>
      </div>
    </div>
  </article>

  <!-- =====================================================
       PRODUCT VIEW MODAL (PURE LIGHT THEME)
  ====================================================== -->
  <Teleport to="body">
    <Transition name="product-modal">
      <div v-if="showModal" class="fixed inset-0 z-[9990] flex items-center justify-center p-3 sm:p-5 md:p-6"
        role="dialog" aria-modal="true">

        <!-- BACKDROP (solid colour: a full-screen blur is the #1 cause of modal lag) -->
        <div class="absolute inset-0 bg-slate-900/40" @click="closeModal"></div>

        <!-- MODAL SHELL (CLEAN WHITE SURFACE) -->
        <div
          class="modal-shell relative z-10 flex max-h-[92vh] w-full max-w-5xl flex-col overflow-hidden rounded-3xl border border-slate-200/90 bg-white shadow-2xl shadow-slate-900/10">

          <!-- =================================================
               MODAL HEADER
          ================================================== -->
          <header
            class="sticky top-0 z-20 flex h-16 shrink-0 items-center justify-between border-b border-slate-100 bg-white px-6 sm:px-8">
            <div class="flex min-w-0 items-center gap-3">
              <div class="min-w-0">
                <div class="flex items-center gap-2">
                  <span class="text-[10px] font-black uppercase tracking-[0.2em] text-blue-600">
                    NEXUSRIGS
                  </span>
                  <span class="text-slate-300">/</span>
                  <span class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                    Quick View
                  </span>
                </div>
                <h2 class="truncate text-xs font-bold text-slate-900 sm:text-sm">
                  {{ product.name }}
                </h2>
              </div>
            </div>

            <div class="flex items-center gap-2">
              <button type="button" @click="goToProductDetailPage"
                class="hidden sm:inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-1.5 text-xs font-bold text-slate-700 transition-colors hover:border-blue-300 hover:bg-blue-50 hover:text-blue-600">
                <span>Full Page</span>
                <svg class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round"
                    d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                </svg>
              </button>

              <button type="button" @click="closeModal"
                class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl border border-slate-200 bg-slate-100 text-slate-500 transition-colors hover:bg-slate-200 hover:text-slate-800"
                aria-label="Close product quick view">
                <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M6 6l12 12M18 6L6 18" />
                </svg>
              </button>
            </div>
          </header>

          <!-- =================================================
               MODAL SCROLLABLE BODY
          ================================================== -->
          <div class="min-h-0 flex-1 overflow-y-auto overscroll-contain">

            <!-- MAIN PRODUCT OVERVIEW GRID -->
            <div class="grid grid-cols-1 gap-6 p-6 sm:p-7 md:grid-cols-[1.1fr_0.9fr] md:gap-8 lg:p-8">

              <!-- ===============================================
                   GALLERY COLUMN
              ================================================ -->
              <section class="flex flex-col gap-3">
                <div class="flex flex-col gap-3 sm:flex-row">

                  <!-- DESKTOP THUMBNAILS -->
                  <div v-if="productImages.length > 1" class="hidden w-16 shrink-0 flex-col gap-2.5 sm:flex">
                    <button v-for="(image, index) in productImages" :key="image" type="button"
                      @click="selectImage(image)"
                      class="relative h-16 w-16 overflow-hidden rounded-xl border transition-colors" :class="selectedImage === image
                        ? 'border-blue-600 bg-white ring-2 ring-blue-500/25 shadow-sm'
                        : 'border-slate-200 bg-slate-50 hover:border-blue-300 hover:bg-white'">
                      <img :src="image" :alt="`${product.name} thumbnail ${index + 1}`" loading="lazy" decoding="async"
                        class="h-full w-full object-contain p-1.5" @error="handleImageError" />
                    </button>
                  </div>

                  <!-- MAIN IMAGE STAGE -->
                  <div
                    class="relative flex h-[320px] min-w-0 flex-1 items-center justify-center overflow-hidden rounded-2xl border border-slate-200 bg-gradient-to-b from-slate-50/80 via-white to-blue-50/20 sm:h-[380px] lg:h-[420px]">

                    <!-- Main Image (glow blob + scale transition removed) -->
                    <img v-if="activeImage" :src="activeImage" :alt="product.name" decoding="async"
                      class="relative z-10 max-h-[85%] max-w-[85%] object-contain p-4" @error="handleImageError" />

                    <div v-else class="relative z-10 flex flex-col items-center text-slate-400">
                      <div
                        class="flex h-12 w-12 items-center justify-center rounded-2xl border border-slate-200 bg-white text-slate-400">
                        <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                          <path stroke-linecap="round" stroke-linejoin="round"
                            d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14M5 20h14a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v14a1 1 0 001 1z" />
                        </svg>
                      </div>
                      <span class="mt-2 text-xs font-medium text-slate-400">Image unavailable</span>
                    </div>

                    <!-- FLOATING STOCK BADGE -->
                    <span
                      class="absolute left-3.5 top-3.5 z-20 inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-[10px] font-bold shadow-sm"
                      :class="product.inStock ? 'border-emerald-200 bg-emerald-50 text-emerald-700' : 'border-rose-200 bg-rose-50 text-rose-700'">
                      <span class="h-1.5 w-1.5 rounded-full"
                        :class="product.inStock ? 'bg-emerald-500' : 'bg-rose-500'"></span>
                      {{ product.inStock ? 'In Stock' : 'Unavailable' }}
                    </span>

                    <!-- PREVIOUS BUTTON -->
                    <button v-if="productImages.length > 1" type="button" @click="previousImage"
                      class="absolute left-3 top-1/2 z-20 flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-600 shadow-sm transition-colors hover:bg-slate-50 hover:text-blue-600 hover:border-blue-300"
                      aria-label="Previous image">
                      <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
                      </svg>
                    </button>

                    <!-- NEXT BUTTON -->
                    <button v-if="productImages.length > 1" type="button" @click="nextImage"
                      class="absolute right-3 top-1/2 z-20 flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-600 shadow-sm transition-colors hover:bg-slate-50 hover:text-blue-600 hover:border-blue-300"
                      aria-label="Next image">
                      <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
                      </svg>
                    </button>

                    <!-- COUNTER BADGE -->
                    <div v-if="productImages.length > 1"
                      class="absolute bottom-3.5 left-1/2 z-20 -translate-x-1/2 rounded-full border border-slate-200 bg-white px-3 py-1 text-[10px] font-bold text-slate-600 shadow-sm">
                      {{ activeImageIndex + 1 }} / {{ productImages.length }}
                    </div>
                  </div>
                </div>

                <!-- MOBILE THUMBNAILS -->
                <div v-if="productImages.length > 1" class="mt-1 flex gap-2 overflow-x-auto pb-1 sm:hidden">
                  <button v-for="(image, index) in productImages" :key="`mobile-${image}`" type="button"
                    @click="selectImage(image)" class="h-14 w-14 shrink-0 overflow-hidden rounded-xl border bg-white"
                    :class="selectedImage === image ? 'border-blue-600 ring-2 ring-blue-500/20' : 'border-slate-200'">
                    <img :src="image" :alt="`${product.name} thumbnail ${index + 1}`" loading="lazy" decoding="async"
                      class="h-full w-full object-contain p-1" @error="handleImageError" />
                  </button>
                </div>
              </section>

              <!-- ===============================================
                   PRODUCT INFO & ACTIONS COLUMN
              ================================================ -->
              <section class="flex min-w-0 flex-col">

                <!-- CATEGORY & BRAND -->
                <div class="flex flex-wrap items-center gap-2">
                  <span
                    class="inline-flex items-center gap-1.5 rounded-full border border-blue-200/80 bg-blue-50/80 px-3 py-1 text-[10px] font-bold uppercase tracking-wider text-blue-700">
                    {{ categoryName }}
                  </span>
                  <span v-if="product.brand" class="text-[11px] font-bold uppercase tracking-[0.16em] text-slate-400">
                    {{ product.brand }}
                  </span>
                </div>

                <!-- TITLE -->
                <h1
                  class="mt-2.5 text-xl font-black leading-tight tracking-tight text-slate-900 sm:text-2xl lg:text-[26px]">
                  {{ product.name }}
                </h1>

                <!-- RATINGS & REVIEWS -->
                <div class="mt-3 flex items-center gap-2.5">
                  <span class="stars text-base" :style="{ '--pct': ratingPercent }" aria-hidden="true"></span>

                  <span class="text-xs font-black text-slate-800">
                    {{ displayRating > 0 ? displayRating.toFixed(1) : 'No rating' }}
                  </span>

                  <span class="h-1 w-1 rounded-full bg-slate-300"></span>

                  <button type="button" @click="scrollToReviews"
                    class="text-xs font-bold text-blue-600 transition-colors hover:text-blue-700 hover:underline">
                    {{ reviewCount }} {{ reviewCount === 1 ? 'review' : 'reviews' }}
                  </button>
                </div>

                <!-- PRICE CARD (LIGHT THEME) -->
                <div
                  class="mt-4 flex items-baseline justify-between rounded-2xl border border-blue-100/90 bg-gradient-to-r from-blue-50/50 via-slate-50/70 to-white p-4 sm:p-5">
                  <div>
                    <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                      Price (LKR)
                    </p>
                    <p class="mt-1 text-2xl font-black tracking-tight text-slate-900 sm:text-3xl">
                      LKR {{ priceText }}
                    </p>
                  </div>

                  <span class="inline-flex items-center gap-1 rounded-lg border px-2.5 py-1 text-[10px] font-bold"
                    :class="product.inStock ? 'border-emerald-200 bg-emerald-50 text-emerald-700' : 'border-rose-200 bg-rose-50 text-rose-700'">
                    {{ product.inStock ? '✓ In Stock & Ready' : '✕ Out of Stock' }}
                  </span>
                </div>

                <!-- DESCRIPTION -->
                <div class="mt-4">
                  <h3 class="text-xs font-bold uppercase tracking-wider text-slate-400">
                    Overview
                  </h3>
                  <p class="mt-1.5 text-xs leading-relaxed text-slate-600">
                    {{ description }}
                  </p>
                </div>

                <!-- TECH SPECS HIGHLIGHTS -->
                <div class="mt-4 grid grid-cols-2 gap-2 rounded-2xl border border-slate-200/80 bg-slate-50/70 p-2.5">
                  <div class="rounded-xl border border-slate-200/60 bg-white p-2.5">
                    <p class="text-[9px] font-bold uppercase tracking-wider text-slate-400">Category</p>
                    <p class="mt-0.5 truncate text-xs font-bold text-slate-800">{{ categoryName }}</p>
                  </div>
                  <div class="rounded-xl border border-slate-200/60 bg-white p-2.5">
                    <p class="text-[9px] font-bold uppercase tracking-wider text-slate-400">Brand</p>
                    <p class="mt-0.5 truncate text-xs font-bold text-slate-800">{{ product.brand || 'NexusRigs Verified'
                      }}</p>
                  </div>
                  <div class="rounded-xl border border-slate-200/60 bg-white p-2.5">
                    <p class="text-[9px] font-bold uppercase tracking-wider text-slate-400">Availability</p>
                    <p class="mt-0.5 text-xs font-bold" :class="product.inStock ? 'text-emerald-600' : 'text-rose-600'">
                      {{ product.inStock ? 'In Stock' : 'Unavailable' }}
                    </p>
                  </div>
                  <div class="rounded-xl border border-slate-200/60 bg-white p-2.5">
                    <p class="text-[9px] font-bold uppercase tracking-wider text-slate-400">Customer Rating</p>
                    <p class="mt-0.5 text-xs font-bold text-slate-800">
                      {{ displayRating > 0 ? `${displayRating.toFixed(1)} / 5.0` : 'New Product' }}
                    </p>
                  </div>
                </div>

                <!-- QUANTITY & ACTIONS -->
                <div class="mt-5 space-y-3 pt-2">

                  <!-- PRIMARY BUTTONS -->
                  <div class="grid grid-cols-2 gap-2.5">
                    <!-- Add to Cart -->
                    <button type="button" @click="addToCart(modalQuantity)"
                      :disabled="!product.inStock || isAddingToCart"
                      class="flex h-12 items-center justify-center gap-2 rounded-xl border-2 border-blue-600 bg-blue-50/70 px-4 text-xs font-black text-blue-700 shadow-sm transition-colors hover:bg-blue-600 hover:text-white disabled:cursor-not-allowed disabled:opacity-50">
                      <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round"
                          d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13l-2 2h13m-4 4a1 1 0 11-2 0m-6 0a1 1 0 11-2 0" />
                      </svg>
                      {{ justAdded ? 'Added to Cart!' : isAddingToCart ? 'Adding...' : 'Add to Cart' }}
                    </button>

                    <!-- Buy Now -->
                    <button type="button" @click="buyNow(modalQuantity)" :disabled="!product.inStock || isBuyingNow"
                      class="flex h-12 items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-4 text-xs font-black text-white shadow-lg shadow-blue-500/25 transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50">
                      <svg v-if="!isBuyingNow" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"
                        stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M13 2L4 14h7l-1 8 9-12h-7l1-8z" />
                      </svg>
                      <svg v-else class="h-4 w-4 animate-spin text-white" fill="none" viewBox="0 0 24 24">
                        <circle class="opacity-25" cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2" />
                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v3a5 5 0 00-5 5H4z" />
                      </svg>
                      {{ isBuyingNow ? 'Processing...' : 'Buy Now' }}
                    </button>
                  </div>

                </div>

              </section>

            </div>

            <!-- =================================================
                 REVIEWS & RATINGS SECTION (LIGHT THEME)
            ================================================== -->
            <section id="product-reviews-section" class="border-t border-slate-100 bg-slate-50/60 p-6 sm:p-8">

              <!-- SECTION HEADER -->
              <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div class="flex items-center gap-3">
                  <div
                    class="flex h-10 w-10 items-center justify-center rounded-xl border border-blue-100 bg-blue-50 text-blue-600 shadow-sm">
                    <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                      <path stroke-linecap="round" stroke-linejoin="round"
                        d="M8 10h.01M12 10h.01M16 10h.01M9 16h6m5-4a8 8 0 01-8 8H8l-4 2 1.5-4.5A8 8 0 0120 12z" />
                    </svg>
                  </div>

                  <div>
                    <h3 class="text-base font-black text-slate-900">
                      Customer Reviews
                    </h3>
                    <p class="text-[11px] font-medium text-slate-400">
                      {{ reviewCount }} verified {{ reviewCount === 1 ? 'rating' : 'ratings' }} from real customers
                    </p>
                  </div>
                </div>

                <!-- Write Review Button -->
                <button type="button" @click="toggleReviewForm"
                  class="inline-flex h-10 items-center justify-center gap-2 rounded-xl border border-blue-200/90 bg-white px-4 text-xs font-bold text-blue-600 shadow-sm transition-colors hover:bg-blue-50">
                  <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M12 20h9" />
                    <path stroke-linecap="round" stroke-linejoin="round"
                      d="M16.5 3.5a2.12 2.12 0 013 3L7 19l-4 1-1 1 1-4L16.5 3.5z" />
                  </svg>
                  {{ showReviewForm ? 'Cancel Review' : 'Write a Review' }}
                </button>
              </div>

              <!-- INLINE FEEDBACK BANNER (NO BLOCKING POPUPS) -->
              <Transition name="feedback-banner">
                <div v-if="reviewFeedbackMessage"
                  class="mt-4 flex items-center justify-between rounded-2xl border px-4 py-3 text-xs font-semibold shadow-sm"
                  :class="reviewFeedbackIsError ? 'border-rose-200 bg-rose-50 text-rose-700' : 'border-emerald-200/90 bg-emerald-50 text-emerald-800'">
                  <div class="flex items-center gap-2">
                    <svg class="h-4 w-4" :class="reviewFeedbackIsError ? 'text-rose-600' : 'text-emerald-600'"
                      fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                      <path v-if="!reviewFeedbackIsError" stroke-linecap="round" stroke-linejoin="round"
                        d="M5 13l4 4L19 7" />
                      <path v-else stroke-linecap="round" stroke-linejoin="round" d="M6 6l12 12M18 6L6 18" />
                    </svg>
                    <span>{{ reviewFeedbackMessage }}</span>
                  </div>
                  <button type="button" @click="reviewFeedbackMessage = null" class="opacity-70 hover:opacity-100"
                    aria-label="Dismiss feedback">
                    ✕
                  </button>
                </div>
              </Transition>

              <!-- RATING BREAKDOWN CARD -->
              <div
                class="mt-4 grid grid-cols-1 overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-sm md:grid-cols-[200px_1fr]">

                <!-- Score Left -->
                <div
                  class="flex flex-col items-center justify-center border-b border-slate-100 p-6 md:border-b-0 md:border-r">
                  <p class="text-[9px] font-bold uppercase tracking-[0.16em] text-slate-400">
                    Average Score
                  </p>
                  <p class="mt-1 text-4xl font-black text-slate-900">
                    {{ displayRating > 0 ? displayRating.toFixed(1) : '—' }}
                  </p>
                  <span class="stars mt-2 text-base" :style="{ '--pct': ratingPercent }" aria-hidden="true"></span>
                  <p class="mt-1.5 text-[10px] font-semibold text-slate-400">
                    {{ reviewCount }} total reviews
                  </p>
                </div>

                <!-- Distribution Right -->
                <div class="p-6">
                  <p class="mb-3 text-[9px] font-bold uppercase tracking-[0.16em] text-slate-400">
                    Rating Distribution
                  </p>
                  <div class="space-y-2">
                    <div v-for="item in ratingDistribution" :key="item.star" class="flex items-center gap-3">
                      <span class="flex w-7 items-center gap-1 text-[10px] font-bold text-slate-600">
                        {{ item.star }} <span class="text-amber-400">★</span>
                      </span>
                      <div class="h-2 flex-1 overflow-hidden rounded-full bg-slate-100">
                        <div class="h-full rounded-full bg-gradient-to-r from-amber-400 to-amber-500"
                          :style="{ width: `${item.percentage}%` }"></div>
                      </div>
                      <span class="w-6 text-right text-[10px] font-semibold text-slate-400">
                        {{ item.count }}
                      </span>
                    </div>
                  </div>
                </div>

              </div>

              <!-- WRITE REVIEW FORM -->
              <Transition name="review-form">
                <div v-if="showReviewForm"
                  class="mt-4 overflow-hidden rounded-2xl border border-slate-200/90 bg-white p-6 shadow-sm">
                  <div class="mb-4 flex items-center justify-between border-b border-slate-100 pb-3">
                    <div>
                      <p class="text-[9px] font-bold uppercase tracking-[0.16em] text-blue-600">
                        Leave Your Feedback
                      </p>
                      <h4 class="mt-0.5 text-sm font-black text-slate-900">
                        Rate & Review this hardware
                      </h4>
                    </div>

                    <button type="button" @click="showReviewForm = false" class="text-slate-400 hover:text-slate-700"
                      aria-label="Close review form">
                      ✕
                    </button>
                  </div>

                  <!-- STAR SELECTION -->
                  <div>
                    <label class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                      Your Rating (Click to select)
                    </label>
                    <div class="mt-2 flex items-center gap-1.5">
                      <button v-for="n in 5" :key="n" type="button" @click="newRating = n"
                        class="flex h-9 w-9 items-center justify-center rounded-xl border text-lg leading-none transition-colors"
                        :class="n <= newRating ? 'bg-amber-50 text-amber-400 border-amber-200' : 'bg-slate-50 text-slate-300 border-slate-200 hover:text-amber-300'"
                        :aria-label="`Rate ${n} stars`">
                        ★
                      </button>

                      <span class="ml-2 text-xs font-black text-slate-700">
                        {{ newRating }} / 5 Stars
                      </span>
                    </div>
                  </div>

                  <!-- REVIEW COMMENT TEXT -->
                  <div class="mt-4">
                    <label class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                      Written Review
                    </label>
                    <textarea v-model="newComment" rows="3" maxlength="1000"
                      placeholder="Share your experience with this product..."
                      class="mt-1.5 w-full resize-none rounded-xl border border-slate-200 bg-slate-50 p-3.5 text-xs text-slate-800 outline-none transition-colors placeholder:text-slate-400 focus:border-blue-400 focus:bg-white focus:ring-4 focus:ring-blue-500/10"></textarea>
                    <div class="mt-1 flex justify-end text-[10px] font-medium text-slate-400">
                      {{ newComment.length }} / 1000
                    </div>
                  </div>

                  <!-- FORM ERROR -->
                  <div v-if="submitReviewError"
                    class="mt-3 flex items-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-3.5 py-2.5 text-xs font-semibold text-rose-600">
                    <svg class="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor"
                      stroke-width="2">
                      <path stroke-linecap="round" d="M6 6l12 12M18 6L6 18" />
                    </svg>
                    {{ submitReviewError }}
                  </div>

                  <!-- FORM ACTIONS -->
                  <div class="mt-4 flex justify-end gap-2.5">
                    <button type="button" @click="showReviewForm = false"
                      class="h-9 rounded-xl border border-slate-200 bg-white px-4 text-xs font-bold text-slate-600 transition-colors hover:bg-slate-50">
                      Cancel
                    </button>

                    <button type="button" @click="submitReview" :disabled="submittingReview"
                      class="flex h-9 items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-5 text-xs font-black text-white shadow-md shadow-blue-500/25 transition-opacity hover:opacity-90 disabled:opacity-50">
                      <svg v-if="!submittingReview" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24"
                        stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M5 12h14M12 5l7 7-7 7" />
                      </svg>
                      <svg v-else class="h-3.5 w-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" />
                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v3a5 5 0 00-5 5H4z" />
                      </svg>
                      {{ submittingReview ? 'Publishing...' : 'Publish Review' }}
                    </button>
                  </div>
                </div>
              </Transition>

              <!-- REVIEWS LIST -->
              <div class="mt-5">
                <!-- LOADING -->
                <div v-if="reviewsLoading" class="rounded-2xl border border-slate-200 bg-white p-8 text-center">
                  <div class="mx-auto h-6 w-6 animate-spin rounded-full border-2 border-slate-200 border-t-blue-600">
                  </div>
                  <p class="mt-3 text-xs font-medium text-slate-400">Loading customer reviews...</p>
                </div>

                <!-- ERROR -->
                <div v-else-if="reviewsError"
                  class="flex items-center gap-2.5 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-xs font-semibold text-rose-600">
                  <svg class="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="9" />
                    <path stroke-linecap="round" d="M12 8v4m0 4h.01" />
                  </svg>
                  {{ reviewsError }}
                  <button type="button" class="ml-1 underline" @click="fetchReviews">Retry</button>
                </div>

                <!-- EMPTY STATE -->
                <div v-else-if="reviews.length === 0"
                  class="rounded-2xl border border-slate-200/80 bg-white p-8 text-center shadow-sm">
                  <div
                    class="mx-auto flex h-11 w-11 items-center justify-center rounded-2xl border border-slate-200 bg-slate-50 text-slate-400">
                    <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
                      <path stroke-linecap="round" stroke-linejoin="round"
                        d="M8 10h.01M12 10h.01M16 10h.01M9 16h6m5-4a8 8 0 01-8 8H8l-4 2 1.5-4.5A8 8 0 0120 12z" />
                    </svg>
                  </div>
                  <h4 class="mt-3 text-sm font-bold text-slate-900">No reviews yet</h4>
                  <p class="mx-auto mt-1 max-w-xs text-xs text-slate-400">
                    Be the first customer to share your thoughts on this product.
                  </p>
                  <button type="button" @click="toggleReviewForm"
                    class="mt-4 rounded-xl border border-slate-200 bg-slate-50 px-4 py-2 text-xs font-bold text-slate-700 transition-colors hover:border-blue-300 hover:bg-blue-50 hover:text-blue-600">
                    Write First Review
                  </button>
                </div>

                <!-- REVIEWS LIST (first 5, then "show more") -->
                <div v-else class="space-y-3">
                  <div v-for="review in visibleReviews" :key="review.reviewId"
                    class="rounded-2xl border border-slate-200/80 bg-white p-4 sm:p-5 shadow-sm">

                    <div class="flex items-start gap-3.5">
                      <!-- Avatar -->
                      <div
                        class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-gradient-to-tr from-blue-600 to-cyan-500 text-xs font-black text-white shadow-sm">
                        {{ (review.customerName || 'A').charAt(0).toUpperCase() }}
                      </div>

                      <!-- Details -->
                      <div class="min-w-0 flex-1">
                        <div class="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
                          <div>
                            <div class="flex flex-wrap items-center gap-2">
                              <span class="text-xs font-bold text-slate-900">
                                {{ review.customerName || 'Verified Buyer' }}
                              </span>
                              <span v-if="isOwnReview(review)"
                                class="rounded-full border border-blue-200 bg-blue-50 px-2 py-0.5 text-[8px] font-bold uppercase tracking-wider text-blue-600">
                                Your Review
                              </span>
                            </div>
                            <p class="mt-0.5 text-[10px] font-medium text-slate-400">
                              {{ formatReviewDate(review.reviewDate) }}
                            </p>
                          </div>

                          <!-- Stars (one element instead of 5 SVGs) -->
                          <div class="flex items-center gap-1.5">
                            <span class="stars text-sm" :style="{ '--pct': starPercent(review.rating) }"
                              aria-hidden="true"></span>
                            <span class="text-[10px] font-bold text-slate-600">
                              {{ Number(review.rating).toFixed(1) }}
                            </span>
                          </div>
                        </div>

                        <!-- Comment -->
                        <p v-if="review.comment" class="mt-2 text-xs leading-relaxed text-slate-700">
                          {{ review.comment }}
                        </p>
                        <p v-else class="mt-2 text-xs italic text-slate-400">
                          No written comment provided.
                        </p>

                        <!-- INLINE DELETE CONFIRMATION -->
                        <div v-if="confirmDeleteReviewId === review.reviewId"
                          class="mt-3 flex items-center justify-between rounded-xl border border-rose-200 bg-rose-50/90 p-2.5">
                          <span class="text-xs font-semibold text-rose-700">
                            Delete your review permanently?
                          </span>
                          <div class="flex items-center gap-2">
                            <button type="button" @click="cancelDeleteReview" :disabled="isDeletingReview"
                              class="rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-[11px] font-bold text-slate-600 hover:bg-slate-50">
                              Cancel
                            </button>
                            <button type="button" @click="executeDeleteReview(review.reviewId)"
                              :disabled="isDeletingReview"
                              class="rounded-lg bg-rose-600 px-2.5 py-1 text-[11px] font-bold text-white hover:bg-rose-700 disabled:opacity-50">
                              {{ isDeletingReview ? 'Deleting...' : 'Delete' }}
                            </button>
                          </div>
                        </div>
                      </div>

                      <!-- DELETE TRIGGER BUTTON -->
                      <button v-if="isOwnReview(review) && confirmDeleteReviewId !== review.reviewId" type="button"
                        @click="requestDeleteReview(review.reviewId)"
                        class="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg border border-slate-200 text-slate-400 transition-colors hover:border-rose-200 hover:bg-rose-50 hover:text-rose-600"
                        title="Delete your review">
                        <svg class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                          <path stroke-linecap="round" stroke-linejoin="round"
                            d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6M9 7V4a1 1 0 011-1h4a1 1 0 011 1v3m-7 0h10" />
                        </svg>
                      </button>
                    </div>

                  </div>

                  <button v-if="reviews.length > reviewLimit" type="button" @click="reviewLimit += 5"
                    class="h-10 w-full rounded-xl border border-slate-200 bg-white text-xs font-bold text-slate-700 shadow-sm transition-colors hover:border-blue-300 hover:bg-blue-50 hover:text-blue-600">
                    Show more reviews ({{ reviews.length - reviewLimit }} remaining)
                  </button>
                </div>

              </div>

            </section>

          </div>

        </div>

      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* ---------- Card: skip rendering when off-screen, cheap hover ---------- */
.product-card {
  content-visibility: auto;
  contain-intrinsic-size: auto 440px;
  transition: transform 0.2s ease, border-color 0.2s ease;
}

.card-img {
  transition: transform 0.3s ease;
}

.quick-btn {
  opacity: 0;
  transform: translateY(4px);
  transition: opacity 0.2s ease, transform 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.product-card:focus-within .quick-btn {
  opacity: 1;
  transform: none;
}

@media (hover: hover) {
  .product-card:hover {
    transform: translateY(-3px);
  }

  .product-card:hover .card-img {
    transform: scale(1.04);
  }

  .product-card:hover .quick-btn {
    opacity: 1;
    transform: none;
  }
}

/* Touch devices have no hover, so keep quick view visible there */
@media (hover: none) {
  .quick-btn {
    opacity: 1;
    transform: none;
  }
}

/* ---------- Stars: one element instead of five SVGs ---------- */
.stars {
  position: relative;
  display: inline-block;
  line-height: 1;
  letter-spacing: 1px;
  white-space: nowrap;
  color: #e2e8f0;
}

.stars::before {
  content: '★★★★★';
}

.stars::after {
  content: '★★★★★';
  position: absolute;
  inset: 0;
  width: var(--pct, 0%);
  overflow: hidden;
  color: #fbbf24;
}

/* ---------- Modal transitions (opacity + transform only) ---------- */
.product-modal-enter-active,
.product-modal-leave-active {
  transition: opacity 0.18s ease;
}

.product-modal-enter-active .modal-shell,
.product-modal-leave-active .modal-shell {
  transition: transform 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.product-modal-enter-from,
.product-modal-leave-to {
  opacity: 0;
}

.product-modal-enter-from .modal-shell,
.product-modal-leave-to .modal-shell {
  transform: translateY(12px);
}

.review-form-enter-active,
.review-form-leave-active,
.feedback-banner-enter-active,
.feedback-banner-leave-active {
  transition: opacity 0.18s ease;
}

.review-form-enter-from,
.review-form-leave-to,
.feedback-banner-enter-from,
.feedback-banner-leave-to {
  opacity: 0;
}

@media (prefers-reduced-motion: reduce) {

  .product-card,
  .card-img,
  .quick-btn,
  .product-modal-enter-active,
  .product-modal-leave-active,
  .product-modal-enter-active .modal-shell,
  .product-modal-leave-active .modal-shell {
    transition: none !important;
  }
}
</style>