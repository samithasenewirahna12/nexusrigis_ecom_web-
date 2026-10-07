<script setup lang="ts">
import { computed, onMounted, ref, shallowRef } from 'vue'
import axios from 'axios'
import backgroundImage from '../../assets/images/vecteezy_abstract-blur-shopping-mall_2795585.jpg'

/* =========================================================
   TYPES
========================================================= */

interface Coupon {
    id: string
    code: string
    title: string
    description: string
    discount: string
    minOrder: string
    expires: string
    type: 'percentage' | 'fixed'
    active: boolean
    accent: 'blue' | 'cyan' | 'violet' | 'emerald'
}

interface CouponDTO {
    couponId: string
    code: string
    discPercent: number
    startDate: string
    endDate: string
}

const ACCENTS: Coupon['accent'][] = ['blue', 'cyan', 'violet', 'emerald']

/* =========================================================
   BACKGROUND
   One fixed layer instead of five. Soft light spots are
   radial gradients instead of blur-3xl circles, which force
   the GPU to blur huge areas on every scroll frame.
========================================================= */

const backgroundStyle = {
    backgroundImage: [
        'radial-gradient(circle at 0% 20%, rgba(147,197,253,0.28), transparent 28%)',
        'radial-gradient(circle at 100% 55%, rgba(103,232,249,0.2), transparent 28%)',
        'linear-gradient(to bottom, rgba(255,255,255,0.35), rgba(255,255,255,0.1), rgba(241,245,249,0.4))',
        'linear-gradient(rgba(255,255,255,0.72), rgba(255,255,255,0.72))',
        `url("${backgroundImage}")`
    ].join(', ')
}

/* =========================================================
   DTO MAPPING
========================================================= */

const mapDtoToCoupon = (dto: CouponDTO, index: number): Coupon => {
    const isExpired = dto.endDate ? new Date(dto.endDate) < new Date() : false

    return {
        id: dto.couponId,
        code: dto.code,
        title: dto.code,
        description:
            'Apply this promotional code during checkout and enjoy an exclusive saving on your purchase.',
        discount: `${dto.discPercent ?? 0}%`,
        minOrder: '',
        expires: dto.endDate
            ? new Date(dto.endDate).toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric',
                year: 'numeric'
            })
            : 'No expiry',
        type: 'percentage',
        active: !isExpired,
        accent: ACCENTS[index % ACCENTS.length]
    }
}

/* =========================================================
   STATE
   shallowRef: coupons are display-only, so Vue doesn't need
   to make every field of every coupon deeply reactive.
========================================================= */

const coupons = shallowRef<Coupon[]>([])

const copiedCode = ref('')
const selectedCoupon = ref<Coupon | null>(null)

const showDetailsModal = ref(false)
const searchQuery = ref('')
const isLoading = ref(false)

/* =========================================================
   LOAD
========================================================= */

const loadCoupons = async () => {
    isLoading.value = true

    try {
        const response = await axios.get<CouponDTO[]>('/api/coupons')

        if (Array.isArray(response.data)) {
            coupons.value = response.data.map(mapDtoToCoupon)
        }
    } catch (error) {
        console.error('Failed to load coupons:', error)
    } finally {
        isLoading.value = false
    }
}

onMounted(() => {
    loadCoupons()
})

/* =========================================================
   COMPUTED
========================================================= */

const activeCount = computed(() => coupons.value.filter((c) => c.active).length)

const percentageCount = computed(
    () => coupons.value.filter((c) => c.active && c.type === 'percentage').length
)

const filteredCoupons = computed(() => {
    const search = searchQuery.value.toLowerCase().trim()

    if (!search) return coupons.value

    return coupons.value.filter(
        (c) =>
            c.code.toLowerCase().includes(search) ||
            c.title.toLowerCase().includes(search)
    )
})

/* =========================================================
   COPY
========================================================= */

let copyTimer: ReturnType<typeof setTimeout> | undefined

const copyCoupon = async (code: string) => {
    try {
        if (navigator.clipboard && window.isSecureContext) {
            await navigator.clipboard.writeText(code)
        } else {
            const textarea = document.createElement('textarea')

            textarea.value = code
            textarea.style.position = 'fixed'
            textarea.style.opacity = '0'

            document.body.appendChild(textarea)

            textarea.focus()
            textarea.select()

            document.execCommand('copy')
            textarea.remove()
        }

        copiedCode.value = code

        clearTimeout(copyTimer)
        copyTimer = setTimeout(() => {
            copiedCode.value = ''
        }, 1800)
    } catch (error) {
        console.error('Unable to copy coupon:', error)
    }
}

/* =========================================================
   MODAL
========================================================= */

const openDetails = (coupon: Coupon) => {
    selectedCoupon.value = coupon
    showDetailsModal.value = true
}

const closeDetails = () => {
    showDetailsModal.value = false
    selectedCoupon.value = null
}

/* =========================================================
   ACCENT (static lookup, built once, not rebuilt per call)
   "glow" is a radial gradient instead of a blur-3xl circle.
========================================================= */

const ACCENT_STYLES = {
    blue: {
        rail: 'from-blue-600 to-cyan-500',
        soft: 'bg-blue-50/80',
        icon: 'bg-blue-50 text-blue-600',
        text: 'text-blue-700',
        border: 'border-blue-200/80',
        glow: 'bg-[radial-gradient(circle,rgba(147,197,253,0.3),transparent_70%)]'
    },
    cyan: {
        rail: 'from-cyan-500 to-blue-500',
        soft: 'bg-cyan-50/80',
        icon: 'bg-cyan-50 text-cyan-600',
        text: 'text-cyan-700',
        border: 'border-cyan-200/80',
        glow: 'bg-[radial-gradient(circle,rgba(103,232,249,0.3),transparent_70%)]'
    },
    violet: {
        rail: 'from-violet-500 to-blue-500',
        soft: 'bg-violet-50/80',
        icon: 'bg-violet-50 text-violet-600',
        text: 'text-violet-700',
        border: 'border-violet-200/80',
        glow: 'bg-[radial-gradient(circle,rgba(196,181,253,0.28),transparent_70%)]'
    },
    emerald: {
        rail: 'from-emerald-500 to-cyan-500',
        soft: 'bg-emerald-50/80',
        icon: 'bg-emerald-50 text-emerald-600',
        text: 'text-emerald-700',
        border: 'border-emerald-200/80',
        glow: 'bg-[radial-gradient(circle,rgba(110,231,183,0.28),transparent_70%)]'
    }
} as const

const getAccent = (accent: Coupon['accent']) =>
    ACCENT_STYLES[accent] ?? ACCENT_STYLES.blue
</script>

<template>
    <div class="relative min-h-screen overflow-hidden bg-slate-100">

        <!-- BACKGROUND (single fixed layer) -->

        <div class="pointer-events-none fixed inset-0 bg-cover bg-center" :style="backgroundStyle"></div>

        <!-- =====================================================
             CONTENT
        ====================================================== -->

        <main class="relative z-10 w-full px-4 py-6 sm:px-6 lg:px-8 lg:py-8">

            <!-- HERO / HEADER -->

            <section
                class="relative mb-7 overflow-hidden rounded-[1.75rem] border border-white/90 bg-white/85 shadow-xl shadow-slate-300/20">

                <div class="p-6 lg:p-7">

                    <div class="flex flex-col gap-7 xl:flex-row xl:items-center xl:justify-between">

                        <!-- LEFT -->

                        <div class="max-w-2xl">

                            <div class="mb-3 flex items-center gap-2.5">

                                <div
                                    class="flex h-9 w-9 items-center justify-center rounded-xl border border-blue-100 bg-blue-50 text-blue-600">
                                    <svg class="h-[18px] w-[18px]" fill="none" stroke="currentColor"
                                        viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                                            d="M9 14.25l6-6m2.25-3.75h.008v.008H17.25V4.5zM6.75 19.5h.008v.008H6.75V19.5zM21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                    </svg>
                                </div>

                                <span class="text-[11px] font-bold uppercase tracking-[0.18em] text-blue-600">
                                    NexusRigs Rewards
                                </span>

                            </div>

                            <h1 class="text-3xl font-black tracking-tight text-slate-950 sm:text-4xl">
                                Coupons & Offers
                            </h1>

                            <p class="mt-2 max-w-xl text-sm leading-6 text-slate-500">
                                Unlock exclusive savings with promotional
                                codes created for your next NexusRigs order.
                            </p>

                        </div>

                        <!-- STATS -->

                        <div
                            class="grid w-full max-w-xl grid-cols-3 overflow-hidden rounded-2xl border border-slate-200/80 bg-white/75 shadow-sm xl:w-auto">

                            <div class="min-w-[110px] px-5 py-4">
                                <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">Available</p>
                                <p class="mt-1 text-2xl font-black text-slate-900">{{ activeCount }}</p>
                            </div>

                            <div class="min-w-[110px] border-l border-slate-200/80 px-5 py-4">
                                <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">Total</p>
                                <p class="mt-1 text-2xl font-black text-slate-900">{{ coupons.length }}</p>
                            </div>

                            <div class="min-w-[110px] border-l border-slate-200/80 px-5 py-4">
                                <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">Percentage</p>
                                <p class="mt-1 text-2xl font-black text-blue-600">{{ percentageCount }}</p>
                            </div>

                        </div>

                    </div>

                    <!-- SEARCH -->

                    <div
                        class="mt-7 flex flex-col gap-3 border-t border-slate-200/70 pt-5 sm:flex-row sm:items-center sm:justify-between">

                        <div>
                            <h2 class="text-base font-bold text-slate-900">Explore available offers</h2>
                            <p class="mt-0.5 text-xs text-slate-500">Copy a code and use it at checkout.</p>
                        </div>

                        <div class="relative w-full sm:max-w-sm">

                            <svg class="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" fill="none"
                                stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                                    d="M21 21l-4.35-4.35m2.1-5.4a7.5 7.5 0 11-15 0 7.5 7.5 0 0115 0z" />
                            </svg>

                            <input v-model="searchQuery" type="text" placeholder="Search coupon code..."
                                class="w-full rounded-xl border border-slate-200 bg-white py-3 pl-10 pr-4 text-sm font-medium text-slate-800 shadow-sm outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10" />

                        </div>

                    </div>

                </div>

            </section>

            <!-- RESULT INFO -->

            <div class="mb-4 flex items-center justify-between">

                <div>
                    <p class="text-[10px] font-black uppercase tracking-[0.18em] text-blue-600">Member Savings</p>
                    <h2 class="mt-1 text-xl font-black text-slate-900">Available Coupons</h2>
                </div>

                <div
                    class="rounded-full border border-white/90 bg-white/90 px-3 py-1.5 text-xs font-bold text-slate-500 shadow-sm">
                    {{ filteredCoupons.length }}
                    {{ filteredCoupons.length === 1 ? 'offer' : 'offers' }}
                </div>

            </div>

            <!-- LOADING -->

            <div v-if="isLoading" class="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">

                <div v-for="item in 6" :key="item"
                    class="overflow-hidden rounded-[1.5rem] border border-white/90 bg-white/90 p-5 shadow-lg">

                    <div class="animate-pulse">

                        <div class="flex items-center gap-4">
                            <div class="h-12 w-12 rounded-xl bg-slate-200"></div>

                            <div class="flex-1">
                                <div class="h-2.5 w-20 rounded bg-slate-200"></div>
                                <div class="mt-2.5 h-5 w-32 rounded bg-slate-200"></div>
                            </div>
                        </div>

                        <div class="mt-6 h-20 rounded-xl bg-slate-100"></div>
                        <div class="mt-4 h-11 rounded-xl bg-slate-100"></div>

                    </div>

                </div>

            </div>

            <!-- COUPON GRID -->

            <div v-else-if="filteredCoupons.length" class="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">

                <article v-for="coupon in filteredCoupons" :key="coupon.id"
                    class="group relative overflow-hidden rounded-[1.5rem] border border-white/95 bg-white shadow-lg shadow-slate-300/20 transition-[transform,box-shadow] duration-300 hover:-translate-y-1 hover:shadow-xl hover:shadow-slate-400/20">

                    <!-- vertical accent -->

                    <div :class="[
                        'absolute left-0 top-0 h-full w-1 bg-gradient-to-b',
                        getAccent(coupon.accent).rail
                    ]"></div>

                    <!-- ambient card glow (radial gradient, no blur filter) -->

                    <div :class="[
                        'pointer-events-none absolute -right-16 -top-16 h-40 w-40 rounded-full',
                        getAccent(coupon.accent).glow
                    ]"></div>

                    <div class="relative p-6">

                        <!-- CARD HEADER -->

                        <div class="flex items-start justify-between gap-4">

                            <div class="flex min-w-0 items-center gap-3.5">

                                <div :class="[
                                    'flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl border shadow-sm',
                                    getAccent(coupon.accent).icon,
                                    getAccent(coupon.accent).border
                                ]">

                                    <svg class="h-[22px] w-[22px]" fill="none" stroke="currentColor"
                                        viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                                            d="M9 14.25l6-6m2.25-3.75h.008v.008H17.25V4.5zM6.75 19.5h.008v.008H6.75V19.5zM21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                    </svg>

                                </div>

                                <div class="min-w-0">

                                    <p class="text-[10px] font-bold uppercase tracking-[0.16em] text-slate-400">
                                        Exclusive Coupon
                                    </p>

                                    <h3 class="mt-1 truncate text-lg font-black tracking-tight text-slate-900">
                                        {{ coupon.code }}
                                    </h3>

                                </div>

                            </div>

                            <span :class="[
                                'shrink-0 rounded-full border px-2.5 py-1 text-[9px] font-black uppercase tracking-wider',
                                coupon.active
                                    ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
                                    : 'border-slate-200 bg-slate-100 text-slate-500'
                            ]">
                                {{ coupon.active ? 'Active' : 'Expired' }}
                            </span>

                        </div>

                        <!-- DISCOUNT -->

                        <div :class="[
                            'relative mt-6 overflow-hidden rounded-2xl border p-5',
                            getAccent(coupon.accent).soft,
                            getAccent(coupon.accent).border
                        ]">

                            <div class="flex items-end justify-between gap-4">

                                <div>

                                    <p class="text-[10px] font-bold uppercase tracking-[0.16em] text-slate-400">
                                        Special saving
                                    </p>

                                    <div class="mt-1 flex items-end gap-2">

                                        <span :class="[
                                            'text-[2.15rem] font-black leading-none tracking-tight',
                                            getAccent(coupon.accent).text
                                        ]">
                                            {{ coupon.discount }}
                                        </span>

                                        <span class="mb-0.5 text-xs font-semibold text-slate-500">OFF</span>

                                    </div>

                                </div>

                                <div class="text-right">

                                    <p class="text-[9px] font-bold uppercase tracking-wider text-slate-400">
                                        Valid until
                                    </p>

                                    <p class="mt-1 text-xs font-bold text-slate-700">
                                        {{ coupon.expires }}
                                    </p>

                                </div>

                            </div>

                        </div>

                        <!-- DESCRIPTION -->

                        <p class="mt-4 min-h-[42px] text-xs leading-5 text-slate-500">
                            {{ coupon.description }}
                        </p>

                        <!-- COUPON CODE -->

                        <div class="mt-5">

                            <div class="mb-2 flex items-center justify-between">

                                <p class="text-[10px] font-black uppercase tracking-[0.15em] text-slate-400">
                                    Coupon code
                                </p>

                                <span :class="['text-[10px] font-bold', getAccent(coupon.accent).text]">
                                    {{ coupon.type === 'percentage' ? 'Percentage discount' : 'Fixed discount' }}
                                </span>

                            </div>

                            <div
                                class="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50/90 p-1.5">

                                <div
                                    class="min-w-0 flex-1 rounded-lg border border-slate-100 bg-white px-3 py-2.5 shadow-sm">

                                    <p class="truncate font-mono text-sm font-black tracking-[0.16em] text-slate-800">
                                        {{ coupon.code }}
                                    </p>

                                </div>

                                <button type="button" :disabled="!coupon.active" @click="copyCoupon(coupon.code)"
                                    :class="[
                                        'min-w-[76px] rounded-lg px-3 py-2.5 text-xs font-black text-white shadow-sm transition-colors',
                                        !coupon.active
                                            ? 'cursor-not-allowed bg-slate-300'
                                            : copiedCode === coupon.code
                                                ? 'bg-emerald-500'
                                                : 'bg-blue-600 hover:bg-blue-700'
                                    ]">
                                    {{ copiedCode === coupon.code ? 'Copied' : 'Copy' }}
                                </button>

                            </div>

                        </div>

                        <!-- FOOTER -->

                        <div class="mt-5 flex items-center justify-between border-t border-slate-200/80 pt-4">

                            <div class="flex items-center gap-2.5">

                                <div
                                    class="flex h-8 w-8 items-center justify-center rounded-lg bg-slate-100 text-slate-500">
                                    <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                                            d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                                    </svg>
                                </div>

                                <div>
                                    <p class="text-[9px] font-bold uppercase tracking-wider text-slate-400">
                                        Offer expiry
                                    </p>

                                    <p class="mt-0.5 text-xs font-bold text-slate-700">
                                        {{ coupon.expires }}
                                    </p>
                                </div>

                            </div>

                            <button type="button" @click="openDetails(coupon)"
                                class="group/button inline-flex items-center gap-1.5 rounded-lg px-2.5 py-2 text-xs font-black text-blue-600 transition-colors hover:bg-blue-50">
                                View details

                                <svg class="h-3.5 w-3.5 transition-transform duration-200 group-hover/button:translate-x-0.5"
                                    fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                        d="M9 5l7 7-7 7" />
                                </svg>

                            </button>

                        </div>

                    </div>

                </article>

            </div>

            <!-- EMPTY -->

            <div v-else class="rounded-[1.5rem] border border-white/90 bg-white/90 px-6 py-16 text-center shadow-xl">

                <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-blue-50 text-blue-600">

                    <svg class="h-7 w-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                            d="M9 14.25l6-6m2.25-3.75h.008v.008H17.25V4.5zM6.75 19.5h.008v.008H6.75V19.5zM21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>

                </div>

                <h3 class="mt-5 text-xl font-black text-slate-900">No offers found</h3>

                <p class="mx-auto mt-2 max-w-sm text-sm text-slate-500">
                    We couldn't find a coupon matching your search.
                </p>

                <button type="button" @click="searchQuery = ''"
                    class="mt-5 rounded-xl bg-blue-600 px-5 py-2.5 text-xs font-black text-white shadow-md shadow-blue-600/20 transition-colors hover:bg-blue-700">
                    View all offers
                </button>

            </div>

            <!-- BENEFITS -->

            <section class="mt-7 rounded-[1.5rem] border border-white/90 bg-white/80 p-3 shadow-lg shadow-slate-300/15">

                <div class="grid grid-cols-1 divide-y divide-slate-200/70 md:grid-cols-3 md:divide-x md:divide-y-0">

                    <div class="flex items-center gap-3 px-4 py-4">

                        <div
                            class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
                            <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                                    d="M20 12v8a2 2 0 01-2 2H6a2 2 0 01-2-2v-8m16 0V7a2 2 0 00-2-2h-3.5M20 12H4m4-7V3h8v2" />
                            </svg>
                        </div>

                        <div>
                            <p class="text-sm font-black text-slate-800">Exclusive promotions</p>
                            <p class="mt-0.5 text-xs text-slate-500">Special customer offers</p>
                        </div>

                    </div>

                    <div class="flex items-center gap-3 px-4 py-4">

                        <div
                            class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-cyan-50 text-cyan-600">
                            <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                                    d="M5 13l4 4L19 7" />
                            </svg>
                        </div>

                        <div>
                            <p class="text-sm font-black text-slate-800">Simple redemption</p>
                            <p class="mt-0.5 text-xs text-slate-500">Copy and apply at checkout</p>
                        </div>

                    </div>

                    <div class="flex items-center gap-3 px-4 py-4">

                        <div
                            class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-violet-50 text-violet-600">
                            <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7"
                                    d="M12 6v6l4 2m5-2a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                        </div>

                        <div>
                            <p class="text-sm font-black text-slate-800">Limited-time savings</p>
                            <p class="mt-0.5 text-xs text-slate-500">Check the expiry before ordering</p>
                        </div>

                    </div>

                </div>

            </section>

        </main>

        <!-- =====================================================
             MODAL
        ====================================================== -->

        <Transition name="modal">

            <div v-if="showDetailsModal && selectedCoupon"
                class="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 p-4"
                @click.self="closeDetails">

                <div
                    class="w-full max-w-lg overflow-hidden rounded-[1.5rem] border border-white/90 bg-white shadow-2xl shadow-slate-900/20">

                    <!-- MODAL HEADER -->

                    <div
                        class="relative overflow-hidden border-b border-white/20 bg-gradient-to-r from-blue-600 to-cyan-500 px-6 py-5 text-white">

                        <div class="absolute -right-12 -top-12 h-32 w-32 rounded-full bg-white/10"></div>

                        <div class="relative flex items-start justify-between">

                            <div>

                                <p class="text-[10px] font-black uppercase tracking-[0.17em] text-white/75">
                                    Coupon Details
                                </p>

                                <h2 class="mt-1.5 text-2xl font-black">
                                    {{ selectedCoupon.title }}
                                </h2>

                                <p class="mt-1 font-mono text-xs text-white/80">
                                    {{ selectedCoupon.code }}
                                </p>

                            </div>

                            <button type="button" @click="closeDetails"
                                class="rounded-xl bg-white/10 p-2.5 text-white transition-colors hover:bg-white/20">
                                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                        d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>

                        </div>

                    </div>

                    <!-- MODAL CONTENT -->

                    <div class="p-6">

                        <p class="text-sm leading-6 text-slate-600">
                            {{ selectedCoupon.description }}
                        </p>

                        <!-- INFO -->

                        <div class="mt-5 grid grid-cols-2 gap-3">

                            <div class="rounded-xl border border-slate-200 bg-slate-50 p-4">

                                <p class="text-[9px] font-black uppercase tracking-wider text-slate-400">Discount</p>

                                <p :class="['mt-1 text-2xl font-black', getAccent(selectedCoupon.accent).text]">
                                    {{ selectedCoupon.discount }}
                                </p>

                            </div>

                            <div class="rounded-xl border border-slate-200 bg-slate-50 p-4">

                                <p class="text-[9px] font-black uppercase tracking-wider text-slate-400">Status</p>

                                <span :class="[
                                    'mt-2 inline-flex rounded-full border px-2.5 py-1 text-[10px] font-bold',
                                    selectedCoupon.active
                                        ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
                                        : 'border-slate-200 bg-slate-100 text-slate-500'
                                ]">
                                    {{ selectedCoupon.active ? 'Active' : 'Expired' }}
                                </span>

                            </div>

                            <div class="rounded-xl border border-slate-200 bg-slate-50 p-4">

                                <p class="text-[9px] font-black uppercase tracking-wider text-slate-400">
                                    Minimum order
                                </p>

                                <p class="mt-1 text-sm font-bold text-slate-800">
                                    {{ selectedCoupon.minOrder || 'No minimum' }}
                                </p>

                            </div>

                            <div class="rounded-xl border border-slate-200 bg-slate-50 p-4">

                                <p class="text-[9px] font-black uppercase tracking-wider text-slate-400">
                                    Valid until
                                </p>

                                <p class="mt-1 text-sm font-bold text-slate-800">
                                    {{ selectedCoupon.expires }}
                                </p>

                            </div>

                        </div>

                        <!-- CODE -->

                        <div class="mt-5">

                            <p class="mb-2 text-[10px] font-black uppercase tracking-wider text-slate-400">
                                Coupon code
                            </p>

                            <div class="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 p-1.5">

                                <div
                                    class="flex-1 rounded-lg border border-slate-100 bg-white px-4 py-3 text-center shadow-sm">
                                    <span class="font-mono text-sm font-black tracking-[0.18em] text-slate-800">
                                        {{ selectedCoupon.code }}
                                    </span>
                                </div>

                                <button type="button" :disabled="!selectedCoupon.active"
                                    @click="copyCoupon(selectedCoupon.code)" :class="[
                                        'rounded-lg px-4 py-3 text-xs font-black text-white transition-colors',
                                        !selectedCoupon.active
                                            ? 'cursor-not-allowed bg-slate-300'
                                            : copiedCode === selectedCoupon.code
                                                ? 'bg-emerald-500'
                                                : 'bg-blue-600 hover:bg-blue-700'
                                    ]">
                                    {{ copiedCode === selectedCoupon.code ? 'Copied' : 'Copy' }}
                                </button>

                            </div>

                        </div>

                        <!-- ACTIONS -->

                        <div class="mt-6 flex justify-end gap-2">

                            <button type="button" @click="closeDetails"
                                class="rounded-xl border border-slate-200 bg-white px-5 py-2.5 text-xs font-bold text-slate-600 transition-colors hover:bg-slate-50">
                                Close
                            </button>

                            <button type="button" :disabled="!selectedCoupon.active"
                                @click="copyCoupon(selectedCoupon.code)" :class="[
                                    'rounded-xl px-5 py-2.5 text-xs font-black text-white shadow-sm transition-colors',
                                    !selectedCoupon.active
                                        ? 'cursor-not-allowed bg-slate-300'
                                        : 'bg-blue-600 hover:bg-blue-700'
                                ]">
                                Copy Coupon
                            </button>

                        </div>

                    </div>

                </div>

            </div>

        </Transition>

    </div>
</template>

<style scoped>
.modal-enter-active,
.modal-leave-active {
    transition:
        opacity 0.22s ease,
        transform 0.22s ease;
}

.modal-enter-from,
.modal-leave-to {
    opacity: 0;
}

.modal-enter-from>div,
.modal-leave-to>div {
    transform: translateY(12px) scale(0.98);
}
</style>