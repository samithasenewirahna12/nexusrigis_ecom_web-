<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ShoppingBag,
  Trash2,
  Plus,
  Minus,
  ArrowRight,
  ShieldCheck,
  Truck,
  RotateCcw,
  Tag,
  CheckCircle2,
  X,
  Sparkles,
  ArrowLeft,
  ChevronRight,
  Zap,
  Package,
  Flame
} from 'lucide-vue-next'
import { useCartStore } from '../../stores/cartStore'
import { usePopup } from '../../composables/usePopup'
import api from '../../services/api'

const cartStore = useCartStore()
const router = useRouter()
const popup = usePopup()

// Promo code state
const promoInput = ref('')
const promoLoading = ref(false)
const promoError = ref('')
const appliedCoupon = ref<{
  code: string
  discPercent: number
  description?: string
} | null>(null)

// Free delivery threshold in LKR
const FREE_SHIPPING_THRESHOLD = 15000

onMounted(async () => {
  await cartStore.fetchCart()

  // Load any previously applied coupon from sessionStorage
  try {
    const savedCoupon = sessionStorage.getItem('appliedCoupon')
    if (savedCoupon) {
      const parsed = JSON.parse(savedCoupon)
      if (parsed?.code && parsed?.discPercent) {
        appliedCoupon.value = parsed
      }
    }
  } catch (err) {
    console.error('Failed to parse applied coupon from storage:', err)
  }
})

// ================================
// CALCULATIONS
// ================================

const subtotal = computed(() => {
  return cartStore.items.reduce((total, item: any) => {
    const price = Number(item.unitPrice ?? item.price ?? 0)
    const qty = Number(item.quantity ?? 1)
    return total + price * qty
  }, 0)
})

const dealDiscount = computed(() => {
  return cartStore.dealDiscount
})

const subtotalBeforeDeals = computed(() => {
  return cartStore.subtotalBeforeDeals
})

const freeShippingProgress = computed(() => {
  if (subtotal.value <= 0) return 0
  const pct = (subtotal.value / FREE_SHIPPING_THRESHOLD) * 100
  return Math.min(100, Math.round(pct))
})

const remainingForFreeShipping = computed(() => {
  return Math.max(0, FREE_SHIPPING_THRESHOLD - subtotal.value)
})

const estimatedShipping = computed(() => {
  if (subtotal.value === 0) return 0
  return subtotal.value >= FREE_SHIPPING_THRESHOLD ? 0 : 450
})

const couponDiscount = computed(() => {
  if (!appliedCoupon.value || !appliedCoupon.value.discPercent) return 0
  return Math.round(subtotal.value * (Number(appliedCoupon.value.discPercent) / 100))
})

const totalCombinedSavings = computed(() => {
  return dealDiscount.value + couponDiscount.value
})

const estimatedTax = computed(() => {
  // 8% estimated VAT/handling
  const taxableAmount = Math.max(0, subtotal.value - couponDiscount.value)
  return Math.round(taxableAmount * 0.08)
})

const grandTotal = computed(() => {
  const total = subtotal.value - couponDiscount.value + estimatedShipping.value + estimatedTax.value
  return Math.max(0, total)
})

// Format currency
function formatLkr(amount: number): string {
  return new Intl.NumberFormat('en-LK', {
    style: 'currency',
    currency: 'LKR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(amount)
}

// ================================
// QUANTITY ADJUSTMENTS
// ================================

async function increaseQuantity(item: any) {
  try {
    const currentQty = Number(item.quantity || 1)
    const maxStock = Number(item.stockQty ?? item.product?.stockQty ?? 99)
    if (currentQty >= maxStock) {
      await popup.warning(`Only ${maxStock} units currently available in stock.`, 'Stock Limit Reached')
      return
    }
    await cartStore.updateQuantity(item.productId, currentQty + 1)
  } catch (error) {
    console.error('Failed to increase quantity:', error)
  }
}

async function decreaseQuantity(item: any) {
  const currentQty = Number(item.quantity || 1)
  if (currentQty <= 1) return

  try {
    await cartStore.updateQuantity(item.productId, currentQty - 1)
  } catch (error) {
    console.error('Failed to decrease quantity:', error)
  }
}

// ================================
// REMOVE ITEM
// ================================

async function removeItem(item: any) {
  const itemName = item.productName || item.title || item.name || 'this item'
  const confirmed = await popup.confirm({
    title: 'Remove Item',
    message: `Are you sure you want to remove "${itemName}" from your cart?`,
    type: 'warning',
    confirmText: 'Remove',
    cancelText: 'Keep'
  })
  if (!confirmed) return

  try {
    await cartStore.removeItem(item.productId)
  } catch (error) {
    console.error('Failed to remove item:', error)
    await popup.error('Failed to remove the item from cart.', 'Cart Error')
  }
}

// ================================
// CLEAR CART
// ================================

async function handleClearCart() {
  const confirmed = await popup.confirm({
    title: 'Clear Shopping Cart',
    message: 'Are you sure you want to remove all items from your cart? This action cannot be undone.',
    type: 'warning',
    confirmText: 'Clear All',
    cancelText: 'Cancel'
  })
  if (!confirmed) return

  try {
    await cartStore.clearCart()
    removeCoupon()
  } catch (error) {
    console.error('Failed to clear cart:', error)
    await popup.error('Failed to clear cart.', 'Cart Error')
  }
}

// ================================
// PROMO / COUPON HANDLING
// ================================

async function applyPromoCode() {
  const code = promoInput.value.trim().toUpperCase()
  promoError.value = ''

  if (!code) {
    promoError.value = 'Please enter a voucher or promo code.'
    return
  }

  promoLoading.value = true
  try {
    // 1. Try checking backend coupons
    let matchedCoupon: any = null
    try {
      const res = await api.get('/coupons')
      if (Array.isArray(res.data)) {
        matchedCoupon = res.data.find(
          (c: any) => c.code?.toUpperCase() === code
        )
      }
    } catch {
      // Fallback
    }

    // 2. Fallback check by code endpoint
    if (!matchedCoupon) {
      try {
        const singleRes = await api.get(`/coupons/code/${encodeURIComponent(code)}`)
        if (singleRes.data && singleRes.data.code) {
          matchedCoupon = singleRes.data
        }
      } catch {
        // Not found
      }
    }

    // 3. Fallback standard coupons if backend mock
    if (!matchedCoupon) {
      const standardCoupons: Record<string, number> = {
        'NEXUS10': 10,
        'WELCOME10': 10,
        'SUMMER15': 15,
        'PROMO20': 20,
        'GAMER25': 25,
        'SUPER50': 50
      }
      if (standardCoupons[code]) {
        matchedCoupon = {
          code: code,
          discPercent: standardCoupons[code],
          description: `${standardCoupons[code]}% Storewide Promo`
        }
      }
    }

    if (!matchedCoupon) {
      promoError.value = `Promo code "${code}" is invalid or expired.`
      return
    }

    appliedCoupon.value = {
      code: matchedCoupon.code,
      discPercent: Number(matchedCoupon.discPercent || 10),
      description: matchedCoupon.description || `${matchedCoupon.discPercent}% Discount`
    }

    sessionStorage.setItem('appliedCoupon', JSON.stringify(appliedCoupon.value))
    promoInput.value = ''
    await popup.success(`Coupon ${matchedCoupon.code} applied! You get ${matchedCoupon.discPercent}% off.`, 'Coupon Applied')
  } catch (err: any) {
    console.error('Apply promo error:', err)
    promoError.value = 'Could not validate promo code. Please try again.'
  } finally {
    promoLoading.value = false
  }
}

function removeCoupon() {
  appliedCoupon.value = null
  sessionStorage.removeItem('appliedCoupon')
  promoError.value = ''
}

// ================================
// PROCEED TO CHECKOUT
// ================================

function handleCheckout() {
  if (cartStore.items.length === 0) {
    popup.warning('Your shopping cart is empty. Please add items to proceed.', 'Cart is Empty')
    return
  }

  if (appliedCoupon.value) {
    sessionStorage.setItem('appliedCoupon', JSON.stringify(appliedCoupon.value))
  }

  router.push('/checkout')
}
</script>

<template>
  <div class="min-h-screen bg-slate-50/60 pb-20 pt-6 antialiased">
    <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">

      <!-- STEP BREADCRUMBS -->
      <nav class="mb-6 flex items-center justify-between text-xs font-semibold text-slate-500">
        <div class="flex items-center gap-2">
          <router-link to="/catalog"
            class="inline-flex items-center gap-1.5 text-slate-500 hover:text-blue-600 transition-colors">
            <ArrowLeft class="h-3.5 w-3.5" />
            <span>Continue Shopping</span>
          </router-link>
        </div>

        <div class="hidden sm:flex items-center gap-2 text-xs">
          <span
            class="inline-flex items-center gap-1.5 rounded-full bg-blue-50 px-3 py-1 font-bold text-blue-600 border border-blue-200/60 shadow-xs">
            <span
              class="flex h-4 w-4 items-center justify-center rounded-full bg-blue-600 text-[10px] text-white">1</span>
            Cart
          </span>
          <ChevronRight class="h-3.5 w-3.5 text-slate-300" />
          <span class="inline-flex items-center gap-1.5 text-slate-400">
            <span
              class="flex h-4 w-4 items-center justify-center rounded-full bg-slate-200 text-[10px] text-slate-600">2</span>
            Checkout
          </span>
          <ChevronRight class="h-3.5 w-3.5 text-slate-300" />
          <span class="inline-flex items-center gap-1.5 text-slate-400">
            <span
              class="flex h-4 w-4 items-center justify-center rounded-full bg-slate-200 text-[10px] text-slate-600">3</span>
            Confirmation
          </span>
        </div>
      </nav>

      <!-- PAGE HEADER -->
      <header class="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div class="flex items-center gap-2.5">
            <div
              class="flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br from-blue-600 to-cyan-500 text-white shadow-md shadow-blue-500/20">
              <ShoppingBag class="h-5 w-5" />
            </div>
            <span class="text-[11px] font-bold uppercase tracking-wider text-blue-600">
              Your Hardware Bag
            </span>
          </div>
          <h1 class="mt-2 text-3xl font-black tracking-tight text-slate-900 sm:text-4xl">
            Shopping Cart
          </h1>
          <p class="mt-1 text-sm text-slate-500">
            Review your selected components, adjust quantities, and unlock exclusive discounts.
          </p>
        </div>

        <div v-if="cartStore.items.length > 0" class="flex items-center gap-3">
          <button type="button" @click="handleClearCart"
            class="inline-flex items-center gap-2 rounded-xl border border-rose-200 bg-rose-50/80 px-4 py-2.5 text-xs font-bold text-rose-600 shadow-xs transition hover:bg-rose-100 hover:border-rose-300">
            <Trash2 class="h-3.5 w-3.5" />
            <span>Clear Cart</span>
          </button>
        </div>
      </header>

      <!-- LOADING STATE -->
      <div v-if="cartStore.loading" class="flex flex-col items-center justify-center py-24">
        <div class="h-12 w-12 animate-spin rounded-full border-4 border-blue-100 border-t-blue-600"></div>
        <p class="mt-4 text-sm font-semibold text-slate-500">Retrieving your cart items...</p>
      </div>

      <!-- EMPTY CART STATE -->
      <div v-else-if="cartStore.items.length === 0" class="mx-auto max-w-2xl py-12">
        <div
          class="rounded-3xl border border-white/90 bg-white/85 p-8 text-center shadow-xl shadow-slate-200/50 backdrop-blur-xl sm:p-14">
          <div
            class="mx-auto flex h-20 w-20 items-center justify-center rounded-3xl bg-gradient-to-br from-blue-50 to-cyan-50 border border-blue-100/80 text-blue-600 shadow-xs">
            <ShoppingBag class="h-10 w-10 text-blue-500" />
          </div>

          <h2 class="mt-6 text-2xl font-black text-slate-900">
            Your shopping cart is empty
          </h2>
          <p class="mx-auto mt-2 max-w-md text-sm leading-relaxed text-slate-500">
            Looks like you haven't added any high-performance components or custom rigs yet. Check out our latest
            products and deals!
          </p>

          <div class="mt-8 flex flex-col sm:flex-row items-center justify-center gap-3">
            <router-link to="/catalog"
              class="inline-flex w-full sm:w-auto items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-7 py-3.5 text-sm font-bold text-white shadow-lg shadow-blue-500/25 transition hover:from-blue-500 hover:to-cyan-400">
              <span>Explore Catalog</span>
              <ArrowRight class="h-4 w-4" />
            </router-link>

            <router-link to="/deals"
              class="inline-flex w-full sm:w-auto items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-6 py-3.5 text-sm font-bold text-slate-700 shadow-xs transition hover:bg-slate-50">
              <Zap class="h-4 w-4 text-amber-500" />
              <span>Browse Today's Deals</span>
            </router-link>
          </div>
        </div>
      </div>

      <!-- CART CONTENT -->
      <div v-else class="grid grid-cols-1 gap-8 lg:grid-cols-12 items-start">

        <!-- LEFT COLUMN: ITEMS & FREE SHIPPING METER -->
        <div class="space-y-6 lg:col-span-8">

          <!-- FREE SHIPPING PROGRESS CARD -->
          <div
            class="overflow-hidden rounded-2xl border border-white/90 bg-white/85 p-5 shadow-lg shadow-slate-200/40 backdrop-blur-xl sm:p-6">
            <div class="flex items-center justify-between gap-4">
              <div class="flex items-center gap-3">
                <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl"
                  :class="remainingForFreeShipping === 0 ? 'bg-emerald-50 text-emerald-600' : 'bg-blue-50 text-blue-600'">
                  <Truck class="h-5 w-5" />
                </div>
                <div>
                  <h3 class="text-sm font-black text-slate-900">
                    <span v-if="remainingForFreeShipping === 0" class="text-emerald-600">
                      Free Express Delivery Unlocked!
                    </span>
                    <span v-else>
                      Free Express Delivery Progress
                    </span>
                  </h3>
                  <p class="text-xs text-slate-500">
                    <span v-if="remainingForFreeShipping === 0">
                      Your order qualifies for zero shipping fees across Sri Lanka.
                    </span>
                    <span v-else>
                      Add <strong class="text-blue-600 font-bold">{{ formatLkr(remainingForFreeShipping) }}</strong>
                      more to unlock FREE delivery!
                    </span>
                  </p>
                </div>
              </div>
              <span class="rounded-full px-2.5 py-1 text-[11px] font-black"
                :class="remainingForFreeShipping === 0 ? 'bg-emerald-50 text-emerald-600' : 'bg-blue-50 text-blue-600'">
                {{ freeShippingProgress }}%
              </span>
            </div>

            <!-- PROGRESS BAR -->
            <div class="mt-4 h-2.5 w-full overflow-hidden rounded-full bg-slate-100">
              <div class="h-full rounded-full transition-all duration-500"
                :class="remainingForFreeShipping === 0 ? 'bg-gradient-to-r from-emerald-500 to-teal-400' : 'bg-gradient-to-r from-blue-600 to-cyan-500'"
                :style="{ width: `${freeShippingProgress}%` }"></div>
            </div>
          </div>

          <!-- CART ITEMS LIST -->
          <div
            class="overflow-hidden rounded-3xl border border-white/90 bg-white/85 shadow-xl shadow-slate-200/40 backdrop-blur-xl">
            <div class="flex items-center justify-between border-b border-slate-100 px-6 py-4">
              <h2 class="text-base font-black text-slate-900">
                Cart Items ({{ cartStore.items.length }})
              </h2>
              <span class="text-xs font-semibold text-slate-400">
                Price & Quantity
              </span>
            </div>

            <div class="divide-y divide-slate-100">
              <div v-for="item in cartStore.items" :key="item.productId"
                class="group p-5 sm:p-6 transition-colors hover:bg-slate-50/40">
                <div class="flex flex-col sm:flex-row sm:items-center gap-4">

                  <!-- PRODUCT THUMBNAIL -->
                  <div
                    class="relative h-22 w-22 sm:h-24 sm:w-24 shrink-0 overflow-hidden rounded-2xl border border-slate-200 bg-white p-2">
                    <img v-if="item.imageUrl || item.image || item.product?.image"
                      :src="item.imageUrl || item.image || item.product?.image" :alt="item.productName || item.name"
                      class="h-full w-full object-contain transition-transform duration-300 group-hover:scale-105"
                      loading="lazy" />
                    <div v-else class="flex h-full w-full items-center justify-center text-slate-300">
                      <Package class="h-8 w-8" />
                    </div>
                  </div>

                  <!-- PRODUCT INFO -->
                  <div class="min-w-0 flex-1">
                    <div class="flex items-center gap-2">
                      <span
                        class="rounded-md bg-blue-50 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider text-blue-600">
                        {{ item.product?.categoryName || 'Hardware' }}
                      </span>
                      <span class="text-[10px] font-semibold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-md">
                        In Stock
                      </span>
                    </div>

                    <h3 class="mt-1 text-base font-black text-slate-900 line-clamp-2">
                      {{ item.productName || item.name || item.product?.name }}
                    </h3>

                    <div class="mt-1.5 flex flex-wrap items-center gap-2 text-xs">
                      <span class="font-bold text-slate-800">
                        {{ formatLkr(Number(item.unitPrice ?? item.price ?? 0)) }}
                      </span>
                      <span
                        v-if="item.originalPrice && Number(item.originalPrice) > Number(item.unitPrice ?? item.price ?? 0)"
                        class="text-xs text-slate-400 line-through">
                        {{ formatLkr(Number(item.originalPrice)) }}
                      </span>
                      <span
                        v-if="item.discountPercentage"
                        class="inline-flex items-center gap-0.5 rounded bg-rose-50 px-1.5 py-0.5 text-[10px] font-black text-rose-600">
                        <Flame class="h-2.5 w-2.5 fill-rose-500 text-rose-500" />
                        -{{ item.discountPercentage }}%
                      </span>
                      <span class="text-slate-300">•</span>
                      <span class="text-slate-500">Unit Price</span>
                    </div>
                  </div>

                  <!-- CONTROLS & LINE TOTAL -->
                  <div
                    class="flex items-center justify-between sm:justify-end gap-5 border-t border-slate-100 pt-3 sm:border-0 sm:pt-0">

                    <!-- QUANTITY STEPPER -->
                    <div
                      class="flex items-center overflow-hidden rounded-xl border border-slate-200 bg-slate-50/80 shadow-2xs">
                      <button type="button" @click="decreaseQuantity(item)" :disabled="Number(item.quantity || 1) <= 1"
                        class="flex h-9 w-9 items-center justify-center text-slate-600 transition hover:bg-white hover:text-blue-600 disabled:cursor-not-allowed disabled:opacity-30"
                        title="Decrease quantity">
                        <Minus class="h-3.5 w-3.5" />
                      </button>

                      <span
                        class="flex h-9 min-w-[36px] items-center justify-center border-x border-slate-200 bg-white text-xs font-black text-slate-900">
                        {{ item.quantity || 1 }}
                      </span>

                      <button type="button" @click="increaseQuantity(item)"
                        class="flex h-9 w-9 items-center justify-center text-slate-600 transition hover:bg-white hover:text-blue-600"
                        title="Increase quantity">
                        <Plus class="h-3.5 w-3.5" />
                      </button>
                    </div>

                    <!-- LINE TOTAL -->
                    <div class="text-right min-w-[100px]">
                      <span class="block text-base font-black text-slate-900">
                        {{ formatLkr(Number(item.unitPrice ?? item.price ?? 0) * Number(item.quantity || 1)) }}
                      </span>
                      <span
                        v-if="item.originalPrice && Number(item.originalPrice) > Number(item.unitPrice ?? item.price ?? 0)"
                        class="block text-[11px] font-semibold text-emerald-600">
                        Save {{ formatLkr((Number(item.originalPrice) - Number(item.unitPrice ?? item.price ?? 0)) * Number(item.quantity || 1)) }}
                      </span>
                      <span v-else class="text-[10px] font-medium text-slate-400">Total</span>
                    </div>

                    <!-- REMOVE BUTTON -->
                    <button type="button" @click="removeItem(item)"
                      class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl text-slate-400 transition hover:bg-rose-50 hover:text-rose-600"
                      title="Remove item">
                      <Trash2 class="h-4 w-4" />
                    </button>

                  </div>

                </div>
              </div>
            </div>
          </div>

          <!-- PROMO CODE INPUT ACCORDION / BOX -->
          <div
            class="overflow-hidden rounded-3xl border border-white/90 bg-white/85 p-6 shadow-xl shadow-slate-200/40 backdrop-blur-xl">
            <div class="flex items-center gap-2.5 mb-3">
              <div class="flex h-8 w-8 items-center justify-center rounded-xl bg-violet-50 text-violet-600">
                <Tag class="h-4 w-4" />
              </div>
              <div>
                <h3 class="text-sm font-black text-slate-900">Have a Voucher or Promo Code?</h3>
                <p class="text-xs text-slate-500">Apply coupons to get instant cart discounts.</p>
              </div>
            </div>

            <!-- APPLIED COUPON BADGE -->
            <div v-if="appliedCoupon"
              class="mt-3 flex items-center justify-between rounded-xl border border-emerald-200 bg-emerald-50/80 p-3.5">
              <div class="flex items-center gap-3">
                <CheckCircle2 class="h-5 w-5 text-emerald-600" />
                <div>
                  <div class="flex items-center gap-2">
                    <span class="font-mono text-xs font-black uppercase text-emerald-800">
                      {{ appliedCoupon.code }}
                    </span>
                    <span class="rounded bg-emerald-200/70 px-2 py-0.5 text-[10px] font-black text-emerald-800">
                      {{ appliedCoupon.discPercent }}% OFF
                    </span>
                  </div>
                  <p class="text-xs text-emerald-700 font-medium">
                    You save {{ formatLkr(couponDiscount) }} on this order!
                  </p>
                </div>
              </div>

              <button type="button" @click="removeCoupon"
                class="rounded-lg bg-white p-1.5 text-xs font-bold text-rose-600 shadow-xs transition hover:bg-rose-50"
                title="Remove Coupon">
                <X class="h-4 w-4" />
              </button>
            </div>

            <!-- INPUT FIELD -->
            <div v-else class="mt-4 flex flex-col sm:flex-row gap-2.5">
              <div class="relative flex-1">
                <input v-model="promoInput" @keyup.enter="applyPromoCode" type="text"
                  placeholder="Enter code (e.g. WELCOME10, NEXUS10)"
                  class="w-full rounded-xl border border-slate-200 bg-white px-4 py-2.5 font-mono text-xs font-bold uppercase tracking-wider text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10" />
              </div>

              <button type="button" @click="applyPromoCode" :disabled="promoLoading || !promoInput.trim()"
                class="inline-flex items-center justify-center gap-2 rounded-xl bg-blue-600 px-5 py-2.5 text-xs font-bold text-white shadow-xs transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50">
                <Sparkles v-if="!promoLoading" class="h-3.5 w-3.5" />
                <div v-else class="h-3.5 w-3.5 animate-spin rounded-full border-2 border-white border-t-transparent">
                </div>
                <span>Apply Code</span>
              </button>
            </div>

            <p v-if="promoError" class="mt-2 text-xs font-semibold text-rose-600">
              {{ promoError }}
            </p>
          </div>

        </div>

        <!-- RIGHT COLUMN: ORDER SUMMARY ASIDE -->
        <aside class="lg:col-span-4">
          <div
            class="sticky top-24 overflow-hidden rounded-3xl border border-white/90 bg-white/90 shadow-xl shadow-slate-200/50 backdrop-blur-xl">

            <!-- SUMMARY HEADER -->
            <div class="border-b border-slate-100 bg-gradient-to-r from-blue-50/80 to-cyan-50/60 px-6 py-5">
              <div class="flex items-center justify-between">
                <div>
                  <h2 class="text-base font-black text-slate-900">
                    Order Summary
                  </h2>
                  <p class="text-xs text-slate-500">
                    {{ cartStore.cartCount }} item{{ cartStore.cartCount === 1 ? '' : 's' }} selected
                  </p>
                </div>
                <div
                  class="flex h-9 w-9 items-center justify-center rounded-xl bg-white text-blue-600 shadow-xs border border-blue-100">
                  <ShoppingBag class="h-4 w-4" />
                </div>
              </div>
            </div>

            <!-- SUMMARY BREAKDOWN -->
            <div class="p-6">
              <div class="space-y-3.5 text-sm">

                <!-- SUBTOTAL -->
                <div class="flex items-center justify-between">
                  <span class="text-slate-500">Items Subtotal</span>
                  <span class="font-bold text-slate-800">
                    {{ formatLkr(subtotalBeforeDeals > 0 ? subtotalBeforeDeals : subtotal) }}
                  </span>
                </div>

                <!-- FLASH DEAL DISCOUNT -->
                <div v-if="dealDiscount > 0" class="flex items-center justify-between">
                  <span class="text-rose-600 flex items-center gap-1.5 font-semibold">
                    <Flame class="h-3.5 w-3.5 fill-rose-500 text-rose-500" />
                    Flash Deal Savings
                  </span>
                  <span class="font-black text-rose-600">
                    - {{ formatLkr(dealDiscount) }}
                  </span>
                </div>

                <!-- COUPON DISCOUNT -->
                <div v-if="couponDiscount > 0" class="flex items-center justify-between">
                  <span class="text-emerald-600 flex items-center gap-1 font-semibold">
                    <Tag class="h-3.5 w-3.5" />
                    Coupon Discount ({{ appliedCoupon?.discPercent }}%)
                  </span>
                  <span class="font-bold text-emerald-600">
                    - {{ formatLkr(couponDiscount) }}
                  </span>
                </div>

                <!-- TOTAL SAVINGS BADGE -->
                <div v-if="totalCombinedSavings > 0"
                  class="rounded-xl bg-emerald-50 border border-emerald-200/80 p-2.5 flex items-center justify-between">
                  <span class="text-xs font-bold text-emerald-800 flex items-center gap-1.5">
                    <Sparkles class="h-3.5 w-3.5 text-emerald-600" />
                    Total Savings on Order
                  </span>
                  <span class="text-xs font-black text-emerald-700">
                    {{ formatLkr(totalCombinedSavings) }}
                  </span>
                </div>

                <!-- SHIPPING -->
                <div class="flex items-center justify-between">
                  <span class="text-slate-500">Estimated Shipping</span>
                  <span :class="estimatedShipping === 0 ? 'text-emerald-600 font-bold' : 'text-slate-800 font-bold'">
                    {{ estimatedShipping === 0 ? 'FREE' : formatLkr(estimatedShipping) }}
                  </span>
                </div>

                <!-- TAX -->
                <div class="flex items-center justify-between">
                  <span class="text-slate-500">Estimated Tax & Handling (8%)</span>
                  <span class="font-bold text-slate-800">
                    {{ formatLkr(estimatedTax) }}
                  </span>
                </div>

                <!-- DIVIDER -->
                <div class="border-t border-slate-100 pt-4">
                  <div class="flex items-end justify-between">
                    <div>
                      <span class="block text-xs font-semibold text-slate-400">Total Payable</span>
                      <span class="text-[10px] text-slate-400">Includes all applicable duties</span>
                    </div>
                    <span
                      class="text-2xl font-black bg-gradient-to-r from-blue-600 to-cyan-500 bg-clip-text text-transparent">
                      {{ formatLkr(grandTotal) }}
                    </span>
                  </div>
                </div>

              </div>

              <!-- CTA BUTTON -->
              <button type="button" @click="handleCheckout"
                class="mt-6 flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 via-indigo-600 to-cyan-500 px-6 py-4 text-sm font-black text-white shadow-lg shadow-blue-500/25 transition-all duration-200 hover:from-blue-500 hover:via-indigo-500 hover:to-cyan-400 hover:-translate-y-0.5 active:translate-y-0">
                <span>Proceed to Checkout</span>
                <ArrowRight class="h-4 w-4" />
              </button>

              <router-link to="/catalog"
                class="mt-3 flex w-full items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-3 text-xs font-bold text-slate-700 transition hover:bg-slate-50">
                <span>Continue Shopping</span>
              </router-link>

              <!-- TRUST BADGES -->
              <div class="mt-6 space-y-2.5 border-t border-slate-100 pt-5 text-xs text-slate-500">
                <div class="flex items-center gap-2.5">
                  <ShieldCheck class="h-4 w-4 text-blue-600 shrink-0" />
                  <span>100% Genuine Tech Hardware with Warranty</span>
                </div>
                <div class="flex items-center gap-2.5">
                  <Truck class="h-4 w-4 text-cyan-600 shrink-0" />
                  <span>Insured Express Courier Islandwide</span>
                </div>
                <div class="flex items-center gap-2.5">
                  <RotateCcw class="h-4 w-4 text-emerald-600 shrink-0" />
                  <span>14-Day Replacement Policy</span>
                </div>
              </div>

            </div>

          </div>
        </aside>

      </div>

    </div>
  </div>
</template>