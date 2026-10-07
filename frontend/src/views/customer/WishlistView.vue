<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cartStore'
import { usePopup } from '../../composables/usePopup'
import api from '../../services/api'

interface Product {
  id: string
  name: string
  description?: string
  price: number
  category?: string
  categoryName?: string
  image?: string
  stockQty?: number
}

// Actual shape returned by GET /products/:id (ProductDTO on
// the backend) - productId, not id; images[], not image.
interface RawProduct {
  productId: string
  name: string
  description?: string
  price: number
  categoryId?: string
  categoryName?: string
  images?: string[]
  stockQty?: number
}

function normalizeProduct(
  raw: RawProduct
): Product {
  const firstImage =
    (raw.images ?? []).find(
      (image) =>
        typeof image === 'string' &&
        image.trim() !== ''
    )

  return {
    id: raw.productId,
    name: raw.name,
    description: raw.description,
    price: raw.price,
    categoryName: raw.categoryName,
    image: firstImage,
    stockQty: raw.stockQty
  }
}

const router = useRouter()
const cartStore = useCartStore()
const popup = usePopup()

const wishlistItems = ref<Product[]>([])
const isLoading = ref(true)
const removingId = ref<string | null>(null)
const addingId = ref<string | null>(null)

// Get logged-in customer
function getCustomerId(): string | null {
  const storedUser = sessionStorage.getItem('user')

  if (!storedUser) {
    return null
  }

  try {
    const user = JSON.parse(storedUser)

    return user.userId || null
  } catch {
    return null
  }
}

// Fetch wishlist
// Fetch wishlist
async function fetchWishlist() {
  const customerId = getCustomerId()

  if (!customerId) {
    wishlistItems.value = []
    isLoading.value = false
    return
  }

  try {
    const response = await api.get(
      `/customers/${customerId}/wishlist`
    )

    console.log('Wishlist response:', response.data)
    const data = response.data

    // 1. Check if productIds exists and is an array
    if (data && Array.isArray(data.productIds) && data.productIds.length > 0) {

      // 2. Fetch the full product details for each ID
      // (Assuming your API has a /products/:id endpoint)
      const productPromises = data.productIds.map((id: any) =>
        api.get(`/products/${id}`).then(res => res.data)
      )

      // 3. Wait for all product calls to finish, then normalize
      // (productId -> id, images[] -> image) and set the items
      const rawProducts:
        RawProduct[] =
        await Promise.all(productPromises)

      wishlistItems.value =
        rawProducts.map(normalizeProduct)

    } else {
      // If productIds is missing or empty, clear the list
      wishlistItems.value = []
    }

  } catch (error) {
    console.error('Failed to fetch wishlist:', error)
    wishlistItems.value = []
  } finally {
    isLoading.value = false
  }
}
// Remove from wishlist
async function removeFromWishlist(productId: string) {
  const customerId = getCustomerId()

  if (!customerId) {
    await popup.warning('Please sign in to manage your wishlist.', 'Sign In Required')
    router.push('/login')
    return
  }

  removingId.value = productId

  try {
    await api.delete(
      `/customers/${customerId}/wishlist/products/${productId}`
    )

    wishlistItems.value =
      wishlistItems.value.filter(
        item => item.id !== productId
      )

  } catch (error) {
    console.error(
      'Failed to remove wishlist item:',
      error
    )
    await popup.error('Failed to remove item from wishlist.', 'Wishlist Error')
  } finally {
    removingId.value = null
  }
}

async function handleRemoveFromWishlist(product: Product) {
  const confirmed = await popup.confirm({
    title: 'Remove from Wishlist',
    message: `Are you sure you want to remove "${product.name}" from your wishlist?`,
    type: 'warning',
    confirmText: 'Remove',
    cancelText: 'Keep'
  })
  if (!confirmed) return
  await removeFromWishlist(product.id)
  await popup.success(`"${product.name}" was removed from your wishlist.`, 'Wishlist Updated')
}

// Move product to cart
async function moveToCart(product: Product) {
  if ((product.stockQty ?? 0) <= 0) {
    await popup.warning('This product is currently out of stock.', 'Out of Stock')
    return
  }

  addingId.value = product.id

  try {

    await cartStore.addItem(
      product.id,
      1
    )

    await removeFromWishlist(product.id)
    await popup.success(`"${product.name}" was moved to your cart!`, 'Added to Cart')

  } catch (error) {
    console.error(
      'Failed to move product to cart:',
      error
    )
    await popup.error('Failed to move product to cart. Please try again.', 'Cart Error')
  } finally {
    addingId.value = null
  }
}

// Product image
function getProductImage(product: Product) {
  if (product.image) {
    return product.image
  }

  return ''
}

// Stock status
function getStockText(product: Product) {
  const stock = product.stockQty ?? 0

  if (stock <= 0) {
    return 'Out of Stock'
  }

  if (stock <= 5) {
    return `${stock} left`
  }

  return 'In Stock'
}

function getStockClass(product: Product) {
  const stock = product.stockQty ?? 0

  if (stock <= 0) {
    return 'bg-red-50 text-red-600 border-red-100'
  }

  if (stock <= 5) {
    return 'bg-amber-50 text-amber-600 border-amber-100'
  }

  return 'bg-emerald-50 text-emerald-600 border-emerald-100'
}

onMounted(() => {
  fetchWishlist()
})
</script>

<template>
  <div class="min-h-screen bg-slate-50">

    <!-- Main Container -->
    <div class="max-w-7xl mx-auto
             px-4 sm:px-6 lg:px-8
             py-8">

      <!-- Header -->
      <div class="mb-8
               flex flex-col sm:flex-row
               sm:items-end
               justify-between
               gap-4">

        <div>
          <div class="inline-flex items-center gap-2
                   px-3 py-1.5
                   mb-3
                   rounded-full
                   bg-blue-50
                   border border-blue-100
                   text-blue-600
                   text-[11px]
                   font-bold
                   uppercase
                   tracking-wider">
            <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 24 24">
              <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5
                   2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09
                   C13.09 3.81 14.76 3 16.5 3
                   19.58 3 22 5.42 22 8.5
                   c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" />
            </svg>

            Saved Products
          </div>

          <h1 class="text-3xl
                   sm:text-4xl
                   font-black
                   text-slate-800
                   tracking-tight">
            My Wishlist
          </h1>

          <p class="text-slate-500
                   text-sm
                   mt-2
                   max-w-xl">
            Keep your favorite hardware components here
            and move them to your cart whenever you're ready.
          </p>
        </div>

        <!-- Wishlist Count -->
        <div v-if="!isLoading" class="self-start sm:self-auto
                 px-4 py-2.5
                 rounded-xl
                 bg-white/80
                 backdrop-blur-xl
                 border border-white
                 shadow-sm
                 text-sm">
          <span class="text-slate-400">
            Saved
          </span>

          <span class="ml-1.5
                   font-bold
                   text-slate-700">
            {{ wishlistItems.length }}
          </span>

          <span class="text-slate-400 ml-1">
            {{ wishlistItems.length === 1 ? 'item' : 'items' }}
          </span>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="isLoading" class="grid
               grid-cols-1
               sm:grid-cols-2
               lg:grid-cols-3
               xl:grid-cols-4
               gap-5">

        <div v-for="n in 4" :key="n" class="bg-white
                 rounded-2xl
                 border border-slate-200
                 p-4
                 animate-pulse">
          <div class="h-48
                   bg-slate-100
                   rounded-xl
                   mb-4"></div>

          <div class="h-3
                   bg-slate-100
                   rounded
                   w-1/3
                   mb-2"></div>

          <div class="h-5
                   bg-slate-100
                   rounded
                   w-4/5
                   mb-3"></div>

          <div class="h-3
                   bg-slate-100
                   rounded
                   w-full
                   mb-2"></div>

          <div class="h-3
                   bg-slate-100
                   rounded
                   w-2/3
                   mb-5"></div>

          <div class="h-10
                   bg-slate-100
                   rounded-xl"></div>
        </div>

      </div>

      <!-- Empty Wishlist -->
      <div v-else-if="wishlistItems.length === 0" class="max-w-xl
               mx-auto
               text-center
               py-20
               px-6
               bg-white/75
               backdrop-blur-xl
               border border-white
               rounded-3xl
               shadow-xl
               shadow-slate-200/50">

        <div class="w-20 h-20
                 mx-auto mb-5
                 rounded-2xl
                 bg-gradient-to-br
                 from-blue-50
                 to-cyan-50
                 border border-blue-100
                 flex items-center justify-center">
          <svg class="w-9 h-9 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364
                 L12 20.364l7.682-7.682a4.5 4.5 0
                 00-6.364-6.364L12 7.636
                 l-1.318-1.318a4.5 4.5 0
                 00-6.364 0z" />
          </svg>
        </div>

        <h2 class="text-xl
                 font-black
                 text-slate-800
                 mb-2">
          Your wishlist is empty
        </h2>

        <p class="text-sm
                 text-slate-500
                 mb-7">
          Save hardware you're interested in
          while browsing the catalog.
        </p>

        <router-link to="/catalog" class="inline-flex
                 items-center
                 gap-2
                 px-6 py-3
                 rounded-xl
                 bg-gradient-to-r
                 from-blue-600
                 to-cyan-500
                 hover:from-blue-500
                 hover:to-cyan-400
                 text-white
                 text-sm
                 font-bold
                 shadow-lg
                 shadow-blue-500/20
                 transition-all
                 active:scale-95">
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4" />
          </svg>

          Explore Catalog
        </router-link>

      </div>

      <!-- Wishlist Products -->
      <div v-else class="grid
               grid-cols-1
               sm:grid-cols-2
               lg:grid-cols-3
               xl:grid-cols-4
               gap-5">

        <div v-for="product in wishlistItems" :key="product.id" class="group
                 bg-white/80
                 backdrop-blur-xl
                 border border-white
                 rounded-2xl
                 overflow-hidden
                 shadow-lg
                 shadow-slate-200/40
                 hover:shadow-xl
                 hover:shadow-blue-100/60
                 hover:-translate-y-1
                 transition-all
                 duration-300">

          <!-- Image -->
          <div class="relative
                   h-48
                   bg-slate-50
                   border-b border-slate-100
                   overflow-hidden">

            <img v-if="getProductImage(product)" :src="getProductImage(product)" :alt="product.name" class="w-full h-full
                     object-cover
                     group-hover:scale-105
                     transition-transform
                     duration-500" />

            <!-- Placeholder -->
            <div v-else class="w-full h-full
                     flex flex-col
                     items-center
                     justify-center
                     text-slate-300">
              <svg class="w-12 h-12 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.3">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 3v2m6-2v2M9 19v2m6-2v2
                     M5 9H3m2 6H3m18-6h-2m2
                     6h-2M7 19h10a2 2 0 002-2V7
                     a2 2 0 00-2-2H7a2 2 0
                     00-2 2v10a2 2 0 002 2z
                     M9 9h6v6H9V9z" />
              </svg>

              <span class="text-[10px]
                       font-bold
                       uppercase
                       tracking-widest">
                Hardware
              </span>
            </div>

            <!-- Wishlist Button -->
            <button @click="handleRemoveFromWishlist(product)" :disabled="removingId === product.id" class="absolute
                     top-3 right-3
                     w-9 h-9
                     rounded-xl
                     bg-white/90
                     backdrop-blur-md
                     border border-white
                     shadow-md
                     flex items-center
                     justify-center
                     text-slate-400
                     hover:text-red-500
                     hover:bg-red-50
                     transition-all
                     disabled:opacity-50" title="Remove from wishlist">

              <svg v-if="removingId !== product.id" class="w-4 h-4" fill="none" viewBox="0 0 24 24"
                stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>

              <svg v-else class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2" stroke-dasharray="40" />
              </svg>

            </button>

            <!-- Stock -->
            <span class="absolute
                     bottom-3 left-3
                     px-2.5 py-1
                     rounded-full
                     border
                     text-[10px]
                     font-bold" :class="getStockClass(product)">
              {{ getStockText(product) }}
            </span>

          </div>

          <!-- Content -->
          <div class="p-5">

            <!-- Category -->
            <span class="text-[10px]
                     font-bold
                     uppercase
                     tracking-wider
                     text-blue-600">
              {{ product.categoryName || product.category || 'Hardware' }}
            </span>

            <!-- Name -->
            <h3 class="mt-1
                     text-base
                     font-black
                     text-slate-800
                     line-clamp-1
                     group-hover:text-blue-600
                     transition-colors">
              {{ product.name }}
            </h3>

            <!-- Description -->
            <p class="mt-2
                     text-xs
                     leading-relaxed
                     text-slate-500
                     line-clamp-2
                     min-h-[36px]">
              {{ product.description || 'Premium hardware component.' }}
            </p>

            <!-- Price -->
            <div class="mt-4
                     flex items-end
                     justify-between">

              <div>
                <span class="block
                         text-[10px]
                         uppercase
                         tracking-wider
                         font-semibold
                         text-slate-400">
                  Price
                </span>

                <span class="text-xl
                         font-black
                         text-slate-800">
                  LKR {{ Number(product.price).toFixed(2) }}
                </span>
              </div>

              <span class="text-[10px]
                       text-slate-400">
                Saved item
              </span>

            </div>

            <!-- Action -->
            <button @click="moveToCart(product)" :disabled="(product.stockQty ?? 0) <= 0 ||
              addingId === product.id
              " class="w-full
                     mt-4
                     py-2.5
                     px-4
                     rounded-xl
                     bg-gradient-to-r
                     from-blue-600
                     to-cyan-500
                     hover:from-blue-500
                     hover:to-cyan-400
                     disabled:from-slate-200
                     disabled:to-slate-200
                     disabled:text-slate-400
                     disabled:cursor-not-allowed
                     text-white
                     text-xs
                     font-bold
                     shadow-md
                     shadow-blue-500/15
                     transition-all
                     active:scale-[0.98]
                     flex items-center
                     justify-center
                     gap-2">

              <svg v-if="addingId !== product.id" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"
                stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M3 3h2l.4 2M7 13h10l4-8H5.4
                     M7 13L5.4 5M7 13l-2 4h14
                     M9 21h.01M17 21h.01" />
              </svg>

              <svg v-else class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2" stroke-dasharray="40" />
              </svg>

              <span>
                {{
                  addingId === product.id
                    ? 'Adding...'
                    : (product.stockQty ?? 0) <= 0 ? 'Out of Stock' : 'Move to Cart' }} </span>

            </button>

          </div>
        </div>

      </div>

    </div>
  </div>
</template>