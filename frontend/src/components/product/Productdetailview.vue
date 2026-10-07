<script setup lang="ts">
import {
    ref,
    onMounted,
    onBeforeUnmount
} from 'vue'

import {
    ArrowLeft,
    ArrowRight,
    ShoppingCart,
    Minus,
    Plus,
    Check,
    Truck,
    ShieldCheck,
    RotateCcw,
    Package,
    Cpu,
    Tag,
    Box,
    Sparkles
} from 'lucide-vue-next'

import { useRoute, useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cartStore'
import api from '../../services/api'

interface Product {
    id: string | number
    name: string
    description: string
    price: number
    category: string
    imageUrl?: string
    stock?: number
    specs?: Record<string, string>
}

interface ApiProduct {
    productId: string | number
    name: string
    description?: string
    price: number | string
    brand?: string
    stockQty?: number | string
    images?: string[]
    categoryId?: string | number
    categoryName?: string
}

const mapProduct = (apiProduct: ApiProduct): Product => ({
    id: apiProduct.productId,
    name: apiProduct.name || 'Unnamed Product',
    description:
        apiProduct.description ||
        'No product description available.',
    price: Number(apiProduct.price) || 0,
    category:
        apiProduct.categoryName ||
        'Uncategorized',
    imageUrl:
        Array.isArray(apiProduct.images)
            ? apiProduct.images[0]
            : undefined,
    stock:
        Number(apiProduct.stockQty) || 0,
    specs: apiProduct.brand
        ? {
            Brand: apiProduct.brand
        }
        : undefined
})

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const product = ref<Product | null>(null)
const isLoading = ref(true)
const selectedQuantity = ref(1)
const activeTab = ref<'specs' | 'shipping'>('specs')
const addedNotification = ref(false)

let notificationTimer:
    ReturnType<typeof setTimeout> | null = null

/* =========================================================
   LOAD PRODUCT
========================================================= */

onMounted(async () => {
    const productId = route.params.id

    try {
        const response =
            await api.get(
                `/products/${productId}`
            )

        product.value =
            mapProduct(response.data)
    } catch (err) {
        console.error(
            'Failed to fetch product details:',
            err
        )

        /* Local fallback */
        product.value = {
            id: String(productId) || '1',
            name:
                'RTX 4080 Super 16GB Graphics Card',
            description:
                'The NVIDIA GeForce RTX 4080 Super delivers the ultra performance and features that enthusiast gamers and creators demand. Bring your games and creative projects to life with ray tracing and AI-powered graphics.',
            price: 1199.0,
            category: 'Graphics Cards',
            stock: 7,
            specs: {
                'CUDA Cores': '10240',
                'Memory Size': '16 GB GDDR6X',
                'Memory Bus': '256-bit',
                'Recommended PSU': '750W',
                Interface: 'PCIe 4.0 x16'
            }
        }
    } finally {
        isLoading.value = false
    }
})

/* =========================================================
   QUANTITY
========================================================= */

function updateQuantity(
    delta: number
) {
    const maxStock =
        product.value?.stock || 10

    const newQty =
        selectedQuantity.value + delta

    if (
        newQty >= 1 &&
        newQty <= maxStock
    ) {
        selectedQuantity.value = newQty
    }
}

/* =========================================================
   ADD TO CART
========================================================= */

async function addToCart() {
    if (!product.value) {
        return
    }

    try {
        await cartStore.addItem(
            String(product.value.id),
            selectedQuantity.value
        )

        addedNotification.value = true

        if (notificationTimer) {
            clearTimeout(notificationTimer)
        }

        notificationTimer =
            setTimeout(() => {
                addedNotification.value = false
            }, 2500)
    } catch (err) {
        console.error(
            'Failed to add to cart:',
            err
        )
    }
}

onBeforeUnmount(() => {
    if (notificationTimer) {
        clearTimeout(notificationTimer)
    }
})

/* =========================================================
   FORMAT PRICE
========================================================= */

function formatPrice(
    value: number
) {
    return new Intl.NumberFormat(
        'en-LK',
        {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        }
    ).format(value)
}
</script>

<template>
    <div class="product-page relative min-h-screen
           overflow-hidden">

        <!-- =====================================================
         BACKGROUND
    ====================================================== -->

        <div class="pointer-events-none absolute
             inset-0 overflow-hidden" aria-hidden="true">
            <div class="absolute -left-40 top-24
               h-80 w-80 rounded-full
               bg-sky-200/20 blur-3xl"></div>

            <div class="absolute -right-40 top-[35%]
               h-96 w-96 rounded-full
               bg-blue-200/15 blur-3xl"></div>

            <div class="absolute left-1/3
               bottom-0 h-80 w-80
               rounded-full
               bg-cyan-200/10 blur-3xl"></div>

            <div class="absolute inset-0
               bg-gradient-to-b
               from-white/20
               via-transparent
               to-slate-100/60"></div>
        </div>


        <!-- =====================================================
         MAIN
    ====================================================== -->

        <main class="relative z-10
             mx-auto w-full
             max-w-[1480px]
             px-4 py-6
             sm:px-6
             lg:px-8
             lg:py-10">

            <!-- ===================================================
           BREADCRUMB / BACK
      ==================================================== -->

            <div class="mb-7 flex
               flex-wrap
               items-center
               justify-between gap-3">

                <button type="button" @click="router.back()" class="group inline-flex
                 items-center gap-2
                 rounded-xl
                 border border-white/80
                 bg-white/55
                 px-3.5 py-2
                 text-xs
                 font-bold
                 text-slate-500
                 shadow-sm
                 backdrop-blur-md
                 transition-[background-color,color,border-color,transform]
                 duration-200
                 hover:-translate-x-0.5
                 hover:border-sky-200
                 hover:bg-white/75
                 hover:text-blue-600">
                    <ArrowLeft class="h-4 w-4
                   transition-transform
                   duration-200
                   group-hover:-translate-x-0.5" />

                    Back to Catalog
                </button>


                <div class="hidden items-center
                 gap-2 rounded-xl
                 border border-white/80
                 bg-white/50
                 px-3 py-2
                 text-[10px]
                 font-bold uppercase
                 tracking-wider
                 text-slate-400
                 backdrop-blur-md
                 sm:flex">
                    <ShieldCheck class="h-4 w-4
                   text-emerald-500" />

                    Secure purchase
                </div>
            </div>


            <!-- ===================================================
           LOADING
      ==================================================== -->

            <div v-if="isLoading" class="grid grid-cols-1
               gap-8 lg:grid-cols-2">

                <!-- Image skeleton -->

                <div class="rounded-3xl
                 border border-white/80
                 bg-white/55
                 p-5
                 shadow-sm
                 backdrop-blur-md">

                    <div class="h-[520px]
                   animate-pulse
                   rounded-2xl
                   bg-slate-200/80"></div>

                </div>


                <!-- Content skeleton -->

                <div class="rounded-3xl
                 border border-white/80
                 bg-white/55
                 p-6
                 shadow-sm
                 backdrop-blur-md
                 sm:p-8">

                    <div class="h-4 w-28
                   animate-pulse
                   rounded bg-slate-200"></div>

                    <div class="mt-4 h-12
                   w-4/5
                   animate-pulse
                   rounded-lg
                   bg-slate-200"></div>

                    <div class="mt-4 h-5 w-1/3
                   animate-pulse
                   rounded
                   bg-slate-200"></div>

                    <div class="mt-7 h-28
                   animate-pulse
                   rounded-2xl
                   bg-slate-200"></div>

                    <div class="mt-7 h-16
                   animate-pulse
                   rounded-2xl
                   bg-slate-200"></div>

                    <div class="mt-5 h-12
                   animate-pulse
                   rounded-xl
                   bg-slate-200"></div>

                </div>
            </div>


            <!-- ===================================================
           PRODUCT
      ==================================================== -->

            <div v-else-if="product" class="grid grid-cols-1
               gap-8
               lg:grid-cols-[1.05fr_0.95fr]
               xl:gap-10">

                <!-- =================================================
             IMAGE PANEL
        ================================================== -->

                <section class="product-glass
                 relative overflow-hidden
                 rounded-[2rem]
                 p-4 sm:p-5">

                    <!-- Glow -->

                    <div class="pointer-events-none
                   absolute -right-20
                   -top-20 h-64 w-64
                   rounded-full
                   bg-sky-400/10
                   blur-3xl"></div>


                    <div class="pointer-events-none
                   absolute -bottom-20
                   -left-20 h-56 w-56
                   rounded-full
                   bg-blue-400/10
                   blur-3xl"></div>


                    <!-- Image stage -->

                    <div class="relative flex
                   min-h-[480px]
                   items-center
                   justify-center
                   overflow-hidden
                   rounded-[1.5rem]
                   border border-white/80
                   bg-white/45
                   p-6
                   backdrop-blur-md
                   sm:min-h-[540px]">

                        <!-- Subtle grid -->

                        <div class="pointer-events-none
                     absolute inset-0
                     opacity-40">
                            <div class="product-grid
                       absolute inset-0"></div>
                        </div>


                        <!-- Top labels -->

                        <div class="absolute left-4
                     top-4 z-20
                     flex flex-wrap gap-2">

                            <span class="inline-flex
                       items-center gap-1.5
                       rounded-full
                       border border-sky-200/70
                       bg-white/70
                       px-3 py-1.5
                       text-[10px]
                       font-black uppercase
                       tracking-wider
                       text-sky-700
                       backdrop-blur-md">
                                <Tag class="h-3.5 w-3.5" />

                                {{ product.category }}
                            </span>

                        </div>


                        <!-- Stock -->

                        <div class="absolute right-4
                     top-4 z-20">

                            <span :class="[
                                'inline-flex items-center gap-1.5 rounded-full',
                                'border px-3 py-1.5',
                                'text-[10px] font-bold',
                                'backdrop-blur-md',
                                (product.stock ?? 0) > 0
                                    ? 'border-emerald-200 bg-emerald-50/80 text-emerald-700'
                                    : 'border-red-200 bg-red-50/80 text-red-600'
                            ]">

                                <span class="h-1.5 w-1.5 rounded-full" :class="(product.stock ?? 0) > 0
                                        ? 'bg-emerald-500'
                                        : 'bg-red-500'
                                    "></span>

                                {{
                                    (product.stock ?? 0) > 0
                                        ? `${product.stock} in stock`
                                        : 'Out of stock'
                                }}

                            </span>

                        </div>


                        <!-- Product image -->

                        <img v-if="product.imageUrl" :src="product.imageUrl" :alt="product.name" loading="eager"
                            decoding="async" class="product-main-image
                     relative z-10
                     max-h-[430px]
                     max-w-full
                     object-contain
                     p-8" />


                        <!-- Empty image -->

                        <div v-else class="relative z-10
                     flex flex-col
                     items-center
                     justify-center
                     text-slate-300">
                            <Box class="h-20 w-20" stroke-width="1.2" />

                            <span class="mt-3
                       text-[10px]
                       font-bold uppercase
                       tracking-[0.18em]
                       text-slate-400">
                                Product Preview
                            </span>
                        </div>


                        <!-- Bottom information -->

                        <div class="absolute inset-x-5
                     bottom-5 z-20
                     flex items-center
                     justify-between gap-3
                     rounded-2xl
                     border border-white/80
                     bg-white/65
                     px-4 py-3
                     shadow-sm
                     backdrop-blur-md">

                            <div class="flex items-center
                       gap-2.5">
                                <div class="flex h-9 w-9
                         items-center
                         justify-center
                         rounded-xl
                         bg-blue-50
                         text-blue-600">
                                    <Cpu class="h-4 w-4" />
                                </div>

                                <div>
                                    <p class="text-[9px]
                           font-bold uppercase
                           tracking-wider
                           text-slate-400">
                                        Hardware
                                    </p>

                                    <p class="text-xs
                           font-bold
                           text-slate-700">
                                        Genuine NexusRigs stock
                                    </p>
                                </div>
                            </div>


                            <span class="hidden items-center
                       gap-1.5 text-[10px]
                       font-semibold
                       text-emerald-600
                       sm:flex">
                                <ShieldCheck class="h-4 w-4" />

                                Warranty included
                            </span>

                        </div>

                    </div>
                </section>


                <!-- =================================================
             PRODUCT INFORMATION
        ================================================== -->

                <section class="product-glass
                 rounded-[2rem]
                 p-6 sm:p-8">

                    <!-- Category -->

                    <div class="flex flex-wrap
                   items-center gap-2">

                        <span class="text-[10px]
                     font-black uppercase
                     tracking-[0.18em]
                     text-blue-600">
                            {{ product.category }}
                        </span>

                        <span class="h-1 w-1
                     rounded-full
                     bg-slate-300"></span>

                        <span class="text-[10px]
                     font-semibold
                     text-slate-400">
                            Premium hardware
                        </span>

                    </div>


                    <!-- Title -->

                    <h1 class="mt-3
                   text-3xl
                   font-black
                   leading-tight
                   tracking-tight
                   text-slate-900
                   sm:text-4xl">
                        {{ product.name }}
                    </h1>


                    <!-- Price -->

                    <div class="mt-5
                   flex flex-wrap
                   items-end
                   gap-x-4 gap-y-2">

                        <span class="text-3xl
                     font-black
                     tracking-tight
                     text-slate-950
                     sm:text-4xl">
                            LKR
                            {{ formatPrice(product.price) }}
                        </span>

                        <span class="mb-1
                     text-xs
                     font-semibold
                     text-slate-400">
                            Taxes included
                        </span>

                    </div>


                    <!-- Purchase assurances -->

                    <div class="mt-6 grid
                   grid-cols-1
                   gap-2 sm:grid-cols-3">

                        <div class="rounded-xl
                     border border-white/80
                     bg-white/45
                     p-3
                     backdrop-blur-sm">

                            <Truck class="h-4 w-4
                       text-blue-600" />

                            <p class="mt-2 text-[10px]
                       font-bold
                       text-slate-700">
                                Fast delivery
                            </p>

                            <p class="mt-0.5
                       text-[9px]
                       text-slate-400">
                                Islandwide
                            </p>

                        </div>


                        <div class="rounded-xl
                     border border-white/80
                     bg-white/45
                     p-3
                     backdrop-blur-sm">

                            <ShieldCheck class="h-4 w-4
                       text-emerald-600" />

                            <p class="mt-2 text-[10px]
                       font-bold
                       text-slate-700">
                                Warranty
                            </p>

                            <p class="mt-0.5
                       text-[9px]
                       text-slate-400">
                                Genuine product
                            </p>

                        </div>


                        <div class="rounded-xl
                     border border-white/80
                     bg-white/45
                     p-3
                     backdrop-blur-sm">

                            <RotateCcw class="h-4 w-4
                       text-violet-600" />

                            <p class="mt-2 text-[10px]
                       font-bold
                       text-slate-700">
                                Easy returns
                            </p>

                            <p class="mt-0.5
                       text-[9px]
                       text-slate-400">
                                Hassle-free process
                            </p>

                        </div>

                    </div>


                    <!-- Description -->

                    <div class="mt-7
                   border-y
                   border-slate-200/70
                   py-5">

                        <div class="mb-2 flex
                     items-center gap-2">
                            <Sparkles class="h-4 w-4
                       text-sky-500" />

                            <span class="text-xs
                       font-bold
                       text-slate-700">
                                Product overview
                            </span>
                        </div>

                        <p class="text-sm
                     leading-7
                     text-slate-500">
                            {{ product.description }}
                        </p>

                    </div>


                    <!-- Quantity -->

                    <div class="mt-6">

                        <label class="text-[10px]
                     font-black uppercase
                     tracking-[0.16em]
                     text-slate-400">
                            Quantity
                        </label>


                        <div class="mt-3 flex
                     flex-col
                     gap-3 sm:flex-row">

                            <!-- Stepper -->

                            <div class="flex h-12
                       items-center
                       rounded-xl
                       border border-slate-200
                       bg-white/70">

                                <button type="button" :disabled="selectedQuantity <= 1
                                    " class="flex h-full
                         w-11
                         items-center
                         justify-center
                         text-slate-400
                         transition-colors
                         hover:text-blue-600
                         disabled:cursor-not-allowed
                         disabled:opacity-30" @click="
                            updateQuantity(-1)
                            ">
                                    <Minus class="h-4 w-4" />
                                </button>


                                <span class="flex w-10
                         items-center
                         justify-center
                         text-sm
                         font-black
                         text-slate-800">
                                    {{ selectedQuantity }}
                                </span>


                                <button type="button" :disabled="selectedQuantity >=
                                    (product.stock || 10)
                                    " class="flex h-full
                         w-11
                         items-center
                         justify-center
                         text-slate-400
                         transition-colors
                         hover:text-blue-600
                         disabled:cursor-not-allowed
                         disabled:opacity-30" @click="
                            updateQuantity(1)
                            ">
                                    <Plus class="h-4 w-4" />
                                </button>

                            </div>


                            <!-- Add button -->

                            <button type="button" :disabled="!product.stock ||
                                product.stock === 0
                                " class="flex h-12
                       flex-1
                       items-center
                       justify-center
                       gap-2
                       rounded-xl
                       border
                       border-blue-200
                       bg-white/60
                       px-5
                       text-sm
                       font-black
                       text-blue-600
                       shadow-sm
                       backdrop-blur-md
                       transition-[background-color,border-color,color,box-shadow,transform]
                       duration-200
                       hover:border-blue-300
                       hover:bg-blue-50/70
                       hover:text-blue-700
                       hover:shadow-md
                       active:scale-[0.99]
                       disabled:cursor-not-allowed
                       disabled:border-slate-200
                       disabled:bg-slate-100/70
                       disabled:text-slate-400
                       disabled:shadow-none" @click="addToCart">
                                <ShoppingCart class="h-4.5 w-4.5" />

                                Add to Cart

                                <span class="rounded-md
                         bg-blue-50
                         px-2 py-0.5
                         text-[10px]
                         font-bold">
                                    ×{{ selectedQuantity }}
                                </span>
                            </button>

                        </div>

                    </div>


                    <!-- Success message -->

                    <Transition name="notice">

                        <div v-if="addedNotification" class="mt-4 flex items-center
                     gap-3 rounded-xl
                     border border-emerald-200
                     bg-emerald-50/80
                     px-4 py-3
                     text-sm
                     font-semibold
                     text-emerald-700">

                            <div class="flex h-7 w-7
                       shrink-0
                       items-center
                       justify-center
                       rounded-lg
                       bg-emerald-100">
                                <Check class="h-4 w-4" />
                            </div>

                            <span>
                                {{ selectedQuantity }}
                                item(s) added to your cart.
                            </span>

                        </div>

                    </Transition>


                    <!-- =================================================
               DETAIL TABS
          ================================================== -->

                    <div class="mt-8
                   rounded-2xl
                   border border-white/80
                   bg-white/35
                   backdrop-blur-md">

                        <!-- Tabs -->

                        <div class="flex
                     border-b
                     border-slate-200/70">

                            <button type="button" @click="
                                activeTab = 'specs'
                                " :class="[
                    'relative flex-1 px-4 py-3',
                    'text-xs font-bold',
                    'transition-colors duration-200',
                    activeTab === 'specs'
                        ? 'text-blue-600'
                        : 'text-slate-400 hover:text-slate-700'
                ]">
                                Technical Specifications

                                <span v-if="
                                    activeTab === 'specs'
                                " class="absolute inset-x-4
                         -bottom-px h-0.5
                         rounded-full
                         bg-blue-600"></span>
                            </button>


                            <button type="button" @click="
                                activeTab = 'shipping'
                                " :class="[
                    'relative flex-1 px-4 py-3',
                    'text-xs font-bold',
                    'transition-colors duration-200',
                    activeTab === 'shipping'
                        ? 'text-blue-600'
                        : 'text-slate-400 hover:text-slate-700'
                ]">
                                Warranty & Delivery

                                <span v-if="
                                    activeTab === 'shipping'
                                " class="absolute inset-x-4
                         -bottom-px h-0.5
                         rounded-full
                         bg-blue-600"></span>
                            </button>

                        </div>


                        <!-- Tab content -->

                        <div class="p-4 sm:p-5">

                            <!-- Specs -->

                            <div v-if="
                                activeTab === 'specs'
                            " class="space-y-1">

                                <div v-for="(val, key) in product.specs" :key="key" class="flex items-center
                         justify-between gap-4
                         rounded-lg
                         px-3 py-2.5
                         even:bg-white/45">

                                    <span class="text-xs
                           font-medium
                           text-slate-400">
                                        {{ key }}
                                    </span>

                                    <span class="text-right
                           text-xs
                           font-bold
                           text-slate-700">
                                        {{ val }}
                                    </span>

                                </div>

                            </div>


                            <!-- Shipping -->

                            <div v-else class="space-y-3">

                                <div class="flex items-start gap-3">
                                    <div class="flex h-8 w-8
                           shrink-0
                           items-center
                           justify-center
                           rounded-lg
                           bg-blue-50
                           text-blue-600">
                                        <ShieldCheck class="h-4 w-4" />
                                    </div>

                                    <div>
                                        <p class="text-xs
                             font-bold
                             text-slate-700">
                                            Manufacturer warranty
                                        </p>

                                        <p class="mt-0.5
                             text-[11px]
                             leading-5
                             text-slate-500">
                                            Genuine hardware with
                                            applicable manufacturer
                                            warranty coverage.
                                        </p>
                                    </div>
                                </div>


                                <div class="flex items-start gap-3">
                                    <div class="flex h-8 w-8
                           shrink-0
                           items-center
                           justify-center
                           rounded-lg
                           bg-violet-50
                           text-violet-600">
                                        <RotateCcw class="h-4 w-4" />
                                    </div>

                                    <div>
                                        <p class="text-xs
                             font-bold
                             text-slate-700">
                                            Easy returns
                                        </p>

                                        <p class="mt-0.5
                             text-[11px]
                             leading-5
                             text-slate-500">
                                            Return eligibility depends
                                            on the applicable product
                                            and order policy.
                                        </p>
                                    </div>
                                </div>


                                <div class="flex items-start gap-3">
                                    <div class="flex h-8 w-8
                           shrink-0
                           items-center
                           justify-center
                           rounded-lg
                           bg-emerald-50
                           text-emerald-600">
                                        <Package class="h-4 w-4" />
                                    </div>

                                    <div>
                                        <p class="text-xs
                             font-bold
                             text-slate-700">
                                            Tracked dispatch
                                        </p>

                                        <p class="mt-0.5
                             text-[11px]
                             leading-5
                             text-slate-500">
                                            Orders are prepared
                                            for tracked delivery
                                            according to stock availability.
                                        </p>
                                    </div>
                                </div>

                            </div>

                        </div>
                    </div>

                </section>
            </div>
        </main>
    </div>
</template>


<style scoped>
.product-page {
    background:
        linear-gradient(180deg,
            #eaf4fb 0%,
            #eef5fa 45%,
            #f5f8fb 100%);
}

.product-glass {
    background:
        rgba(255, 255, 255, 0.48);

    border: 1px solid rgba(255, 255, 255, 0.78);

    box-shadow:
        0 12px 38px rgba(15, 23, 42, 0.06);

    backdrop-filter: blur(14px);
    -webkit-backdrop-filter: blur(14px);
}

.product-grid {
    background-image:
        linear-gradient(rgba(59, 130, 246, 0.035) 1px,
            transparent 1px),
        linear-gradient(90deg,
            rgba(59, 130, 246, 0.035) 1px,
            transparent 1px);

    background-size: 32px 32px;

    mask-image:
        linear-gradient(to bottom,
            black,
            transparent 90%);
}

.product-main-image {
    filter:
        drop-shadow(0 20px 28px rgba(15, 23, 42, 0.12));

    transform: translateZ(0);

    transition:
        transform 0.4s ease,
        filter 0.4s ease;
}

.product-main-image:hover {
    transform:
        translateY(-5px) scale(1.015);

    filter:
        drop-shadow(0 26px 34px rgba(15, 23, 42, 0.16));
}

.notice-enter-active,
.notice-leave-active {
    transition:
        opacity 0.2s ease,
        transform 0.2s ease;
}

.notice-enter-from,
.notice-leave-to {
    opacity: 0;
    transform: translateY(-5px);
}

button:focus-visible {
    outline: 2px solid rgb(37 99 235);

    outline-offset: 3px;
}

@media (prefers-reduced-motion: reduce) {

    *,
    *::before,
    *::after {
        transition-duration: 0.01ms !important;
        animation-duration: 0.01ms !important;
    }
}
</style>