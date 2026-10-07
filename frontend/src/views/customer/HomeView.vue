<script setup lang="ts">
// npm i lucide-vue-next
import { ref, computed, watch, nextTick, onMounted, onUnmounted, defineComponent, h } from 'vue'
import { useRouter } from 'vue-router'
import {
  Cpu, PcCase, MemoryStick, HardDrive, Monitor, Mouse, CircuitBoard, Fan, Boxes,
  Gamepad2, Palette, Wrench, Truck, ShieldCheck, Lock, Headset, RotateCcw, BadgeCheck,
  ChevronLeft, ChevronRight, ChevronDown, ArrowRight, ArrowUp, Pause, Play, Eye, Heart,
  ShoppingCart, Star, X, Plus, Minus, Flame, Sparkles, Zap, Clock, Mail, CreditCard,
  Package, Search, Quote, CheckCircle2, Keyboard, Headphones, Laptop, Plug, Router, Printer, Cable,
  Speaker, Webcam, Smartphone, Gauge, Video, Layers, BrainCircuit, Server, Briefcase, Activity, Shield,
  ArrowUpRight, SlidersHorizontal, Lightbulb,
  Tag
} from 'lucide-vue-next'
import { useProductStore } from '../../stores/productStore'
import { useCartStore } from '../../stores/cartStore'
import { useNotificationStore } from '../../stores/notificationStore'
import { productService } from '../../services/productService'
import { dealService, type DealItem } from '../../services/dealService'
import { getAllReviews, type ReviewResponse } from '../../services/reviewService'
import customPcImage from '../../assets/images/custompc.jpg'
// Tip: convert this to WebP and keep it under ~200 KB
import lightBlueTechBg from '../../assets/images/light_blue_tech_bg.jpg'

const productStore = useProductStore()
const cartStore = useCartStore()
const notify = useNotificationStore()
const router = useRouter()

/* ---------- Single background layer (gradient + image merged into one paint) ---------- */
const bgStyle = {
  backgroundImage: `linear-gradient(to bottom, rgb(255 255 255 / .2), rgb(255 255 255 / .7), rgb(255 255 255 / .5)), url(${lightBlueTechBg})`
}

/* ---------- Isolated small components (keep their re-renders local) ---------- */
const pad = (n: number) => String(n).padStart(2, '0')

const Countdown = defineComponent({
  setup() {
    const end = new Date()
    end.setHours(23, 59, 59, 999)
    const left = ref(Math.max(0, end.getTime() - Date.now()))
    let t: ReturnType<typeof setInterval>
    onMounted(() => { t = setInterval(() => { left.value = Math.max(0, end.getTime() - Date.now()) }, 1000) })
    onUnmounted(() => clearInterval(t))
    return () => {
      const s = Math.floor(left.value / 1000)
      const parts: [string, number][] = [['HRS', Math.floor(s / 3600)], ['MIN', Math.floor((s % 3600) / 60)], ['SEC', s % 60]]
      return h('div', { class: 'flex items-center gap-1.5 font-mono text-sm font-bold' },
        parts.map(([k, v]) => h('span', {
          key: k,
          class: ['rounded-xl border px-2.5 py-1.5 text-center sm:px-3', k === 'SEC' ? 'border-red-200 bg-red-50 text-red-600' : 'border-slate-200 bg-white text-slate-900']
        }, [pad(v), h('span', { class: 'block font-sans text-[9px] font-medium text-slate-400' }, k)])))
    }
  }
})

const CountUp = defineComponent({
  props: { value: { type: Number, default: 0 } },
  setup(props) {
    const shown = ref(0)
    let raf = 0
    function run(to: number) {
      cancelAnimationFrame(raf)
      const from = shown.value
      const t0 = performance.now()
      const step = (t: number) => {
        const k = Math.min(1, (t - t0) / 900)
        shown.value = Math.round(from + (to - from) * (1 - Math.pow(1 - k, 3)))
        if (k < 1) raf = requestAnimationFrame(step)
      }
      raf = requestAnimationFrame(step)
    }
    watch(() => props.value, run, { immediate: true })
    onUnmounted(() => cancelAnimationFrame(raf))
    return () => h('span', shown.value.toLocaleString())
  }
})

/* ---------- Static content ---------- */
const goals = [
  {
    key: 'gaming', title: 'Gaming & Esports', icon: Gamepad2, badge: 'Popular', status: 'Preset ready', tag: 'High FPS', ctaText: 'Preview this build',
    copy: 'High frame rates, ultra-low input latency, and sustained GPU boost clocks.',
    metrics: [{ icon: Gauge, value: '240+ FPS', label: 'Target' }, { icon: Flame, value: '360mm Liquid', label: 'Cooling' }, { icon: Monitor, value: '1440p / 4K', label: 'Display' }]
  },
  {
    key: 'creator', title: 'Content Creation', icon: Video, badge: 'Pro Tier', status: 'Studio ready', tag: 'Render Peak', ctaText: 'Preview this build',
    copy: 'Accelerated 4K/8K video rendering, 3D viewport fluidity, and multi-track audio.',
    metrics: [{ icon: Cpu, value: '16-Core', label: 'Processor' }, { icon: HardDrive, value: '64GB DDR5', label: 'RAM' }, { icon: Layers, value: 'Gen4 NVMe', label: 'Storage' }]
  },
  {
    key: 'ai', title: 'AI & Data Science', icon: BrainCircuit, badge: 'Enterprise', status: 'Model ready', tag: 'Tensor Boost', ctaText: 'Preview this build',
    copy: 'Massive VRAM allocations, tensor core density, and high-throughput data processing.',
    metrics: [{ icon: Zap, value: '24GB VRAM', label: 'GPU Memory' }, { icon: Server, value: 'PCIe 5.0', label: 'Bus Speed' }, { icon: ShieldCheck, value: 'ECC Support', label: 'Integrity' }]
  },
  {
    key: 'office', title: 'Office & Work', icon: Briefcase, badge: 'Value', status: 'Eco ready', tag: 'Silent Operation', ctaText: 'Preview this build',
    copy: 'Silent acoustic profile, ultra-low energy draw, and fast boot times.',
    metrics: [{ icon: Activity, value: '65W TDP', label: 'Low Power' }, { icon: Fan, value: '0dB Mode', label: 'Acoustics' }, { icon: Shield, value: '5-Yr Coverage', label: 'Reliability' }]
  }
]

interface ApiProduct {
  productId?: string | number; id?: string | number; name?: string; brand?: string
  price?: number | string
  category?: string | { categoryId?: string | number; id?: string | number; categoryName?: string; name?: string }
  categoryId?: string | number; categoryName?: string
  stock?: number | string; stockQty?: number | string
  description?: string; discription?: string
  images?: string[]; image?: string
  rating?: number | string; reviewCount?: number | string
  reviews?: { rating: number }[]
}

const slides = [
  { badge: 'Gaming hardware', title: 'Frame rates that', highlight: 'never blink.', subtitle: 'Graphics cards, DDR5 memory, fast NVMe storage and cooling picked for modern gaming rigs.', cta: 'Shop gaming', link: '/catalog', tag: 'Gaming', img: 'https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=1400&q=75' },
  { badge: 'Creator & workstation', title: 'Render faster,', highlight: 'create more.', subtitle: 'Multi-core processors, colour-accurate displays and high-speed storage for demanding workflows.', cta: 'Shop creator gear', link: '/catalog', tag: 'Creator', img: 'https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?w=1400&q=75' },
  { badge: 'Pro peripherals', title: 'Precision in', highlight: 'every click.', subtitle: 'Responsive keyboards, lightweight mice, immersive headsets and high-refresh displays.', cta: 'Browse peripherals', link: '/catalog', tag: 'Peripherals', img: 'https://images.unsplash.com/photo-1593305841991-05c297ba4575?w=1400&q=75' },
  { badge: 'PC builder', title: 'Design your PC with', highlight: 'live pricing.', subtitle: 'Pick components, check the wattage and see your total before you order.', cta: 'Launch PC builder', link: '/builds', tag: 'Builder', img: 'https://images.unsplash.com/photo-1587831990711-23ca6441447b?w=1400&q=75' }
]

const heroBadges = [
  { i: ShieldCheck, t: 'Authorised stock', c: 'Warranty included' },
  { i: Truck, t: 'Islandwide', c: 'Fast dispatch' }
]

const announcements = [
  'Genuine hardware with official warranty', 'Islandwide delivery across Sri Lanka', 'Free build consultation',
  '7-day return window on unopened items', 'Flash deals reset at midnight', 'New: Smart Build Matcher pick a goal and budget, get a live parts list'
]
const announcementLoop = [...announcements, ...announcements]

const announcementIcons = [
  Truck, ShieldCheck, Sparkles, Flame, Headset, BadgeCheck, Zap, Wrench, Tag, Package
]

const trust = [
  { icon: Truck, title: 'Islandwide delivery', copy: 'Fast dispatch across Sri Lanka' },
  { icon: ShieldCheck, title: 'Genuine hardware', copy: 'Official warranty on every item' },
  { icon: Lock, title: 'Secure checkout', copy: 'Protected online payments' },
  { icon: Headset, title: 'Build advice', copy: 'Talk to a hardware specialist' }
]

const budgets = [
  { label: 'Under Rs. 50k', copy: 'Entry upgrades & accessories', key: 'under50k' },
  { label: 'Rs. 50k – 150k', copy: 'Mid-range parts & monitors', key: '50to150k' },
  { label: 'Rs. 150k – 300k', copy: 'High-end GPUs & CPUs', key: '150to300k' },
  { label: 'Rs. 300k+', copy: 'Flagship components', key: 'above300k' }
]

const steps = [
  { icon: Search, title: 'Choose your parts', copy: 'Browse by category or start from a goal.' },
  { icon: CircuitBoard, title: 'Check compatibility', copy: 'The builder flags parts that do not fit together.' },
  { icon: CreditCard, title: 'Pay securely', copy: 'Confirm your total and checkout online.' },
  { icon: Package, title: 'Receive it fast', copy: 'Tracked delivery with warranty paperwork.' }
]

const services = [
  { icon: BadgeCheck, title: 'Authorised distributor stock', copy: 'Every product comes with a valid manufacturer warranty.' },
  { icon: Wrench, title: 'Free build consultation', copy: 'Share your budget and we will suggest a balanced parts list.' },
  { icon: RotateCcw, title: '7-day return window', copy: 'Unopened items can be returned if they are not what you needed.' },
  { icon: Headset, title: 'After-sales support', copy: 'Help with RMA claims, drivers and upgrades.' }
]

const brandPartners = ['ASUS ROG', 'MSI', 'Corsair', 'Logitech G', 'AMD Ryzen', 'Intel Core', 'NVIDIA RTX', 'Gigabyte AORUS', 'Razer', 'Samsung']
const brandLoop = [...brandPartners, ...brandPartners]

const fallbackReviews = [
  { name: 'Kasun Perera', role: 'Competitive gamer, Colombo', item: 'GPU upgrade', text: 'Clear specs and stock info made choosing the right card easy. Delivery was quick.', rating: 5, avatar: '' },
  { name: 'Dulantha Silva', role: '3D artist, Kandy', item: 'Creator workstation', text: 'The builder helped me stay on budget without sacrificing render performance.', rating: 5, avatar: '' },
  { name: 'Ruwan Jayasuriya', role: 'Software architect, Galle', item: 'Ultra-wide display', text: 'Genuine product, proper warranty and a clean ordering experience.', rating: 5, avatar: '' },
  { name: 'Shamika Fernando', role: 'Streamer, Kurunegala', item: 'Peripherals', text: 'Found everything for my streaming desk in one place at a fair price.', rating: 5, avatar: '' }
]

const faqs = [
  { q: 'How do I find the right category?', a: 'Use the category carousel or open the full catalog and filter by brand, price and stock.' },
  { q: 'Can I build a custom PC?', a: 'Yes. Open the PC builder to pick components, see the wattage estimate and total price.' },
  { q: 'Is the hardware genuine?', a: 'All products come from authorised distributors with manufacturer warranty.' },
  { q: 'How fast is delivery?', a: 'In-stock items are dispatched quickly and delivered islandwide with tracking.' },
  { q: 'Can I add items quickly?', a: 'Use the Add button on any in-stock card, or open Quick View for a closer look.' }
]

const categoryRules: [RegExp, any][] = [
  [/proces|cpu|ryzen|intel/i, Cpu],
  [/graphic|gpu|video card|rtx|radeon/i, PcCase],
  [/memory|ram|ddr/i, MemoryStick],
  [/storage|ssd|nvme|hdd|drive|disk/i, HardDrive],
  [/monitor|display|screen/i, Monitor],
  [/keyboard/i, Keyboard],
  [/mouse|mice/i, Mouse],
  [/headset|headphone|earphone|audio/i, Headphones],
  [/speaker/i, Speaker],
  [/laptop|notebook/i, Laptop],
  [/mother|mainboard/i, CircuitBoard],
  [/cool|fan|liquid|aio/i, Fan],
  [/power|psu|supply|ups/i, Plug],
  [/network|router|wifi|wi-fi/i, Router],
  [/cable|adapter|hub|dock/i, Cable],
  [/print|scan/i, Printer],
  [/cam|webcam|stream/i, Webcam],
  [/phone|tablet|mobile/i, Smartphone],
  [/case|chassis|cabinet/i, Boxes],
  [/gaming|controller|console|chair/i, Gamepad2],
  [/periph|access/i, Mouse]
]
const fallbackIcons = [Gauge, Package, Boxes, Cable, Webcam, Speaker]
const tones = [
  'border-blue-200 bg-blue-50 text-blue-600 group-hover:bg-blue-600 group-hover:text-white',
  'border-indigo-200 bg-indigo-50 text-indigo-600 group-hover:bg-indigo-600 group-hover:text-white',
  'border-cyan-200 bg-cyan-50 text-cyan-600 group-hover:bg-cyan-600 group-hover:text-white',
  'border-emerald-200 bg-emerald-50 text-emerald-600 group-hover:bg-emerald-600 group-hover:text-white',
  'border-violet-200 bg-violet-50 text-violet-600 group-hover:bg-violet-600 group-hover:text-white',
  'border-amber-200 bg-amber-50 text-amber-600 group-hover:bg-amber-600 group-hover:text-white',
  'border-rose-200 bg-rose-50 text-rose-600 group-hover:bg-rose-600 group-hover:text-white',
  'border-sky-200 bg-sky-50 text-sky-600 group-hover:bg-sky-600 group-hover:text-white'
]
function iconFor(name: string, index: number) {
  const hit = categoryRules.find(([re]) => re.test(name))
  return hit ? hit[1] : fallbackIcons[index % fallbackIcons.length]
}

/* ---------- Hero slider (CSS-driven progress: zero re-renders while idle) ---------- */
const current = ref(0)
const paused = ref(false)
const next = () => { current.value = (current.value + 1) % slides.length }
const prev = () => { current.value = (current.value - 1 + slides.length) % slides.length }
const goTo = (i: number) => { current.value = i }
/* touch devices fire fake mouseenter on tap, which froze the slider: only pause for a real mouse */
const hoverPause = (e: PointerEvent, v: boolean) => { if (e.pointerType === 'mouse') paused.value = v }
let touchX = 0
const onTouchStart = (e: TouchEvent) => { touchX = e.touches[0].clientX }
const onTouchEnd = (e: TouchEvent) => {
  const dx = e.changedTouches[0].clientX - touchX
  if (Math.abs(dx) > 50) dx < 0 ? next() : prev()
}

/* ---------- Product helpers ---------- */
const allProducts = computed(() => (productStore.products || []) as ApiProduct[])
const pid = (p: ApiProduct | null | undefined) => String(p?.productId ?? p?.id ?? '').trim()
const stock = (p: ApiProduct) => Number(p?.stockQty ?? p?.stock ?? 0) || 0
const money = (v: unknown) => Number(v || 0).toLocaleString('en-LK')
const oldPrice = (v: unknown) => Math.round((Number(v || 0) * 1.14) / 100) * 100
const discount = (id: string) => 10 + ((parseInt(id.replace(/\D/g, ''), 10) || 7) % 8) * 2

function cleanUrl(v: unknown): string {
  if (typeof v !== 'string') return ''
  let u = v.trim()
  if (!u) return ''
  const md = u.indexOf('](')
  if ((u.startsWith('[') || u.startsWith('![')) && md !== -1 && u.endsWith(')')) u = u.slice(md + 2, -1).trim()
  if (u.startsWith('<') && u.endsWith('>')) u = u.slice(1, -1).trim()
  if (/^(".*"|'.*')$/.test(u)) u = u.slice(1, -1).trim()
  if (!u) return ''
  if (/^(https?:|data:|blob:)/i.test(u)) return u
  if (u.startsWith('/src/') || u.startsWith('/assets/')) return u
  const baseUrl = (import.meta.env.VITE_API_URL || 'http://localhost:8080/api').replace(/\/api\/?$/, '')
  return `${baseUrl}/${u.replace(/^\//, '')}`
}
const image = (p: ApiProduct) => cleanUrl(p.images?.[0]) || cleanUrl(p.image)
const catName = (p: ApiProduct) =>
  p.categoryName || (typeof p.category === 'string' ? p.category : p.category?.categoryName ?? p.category?.name ?? '') || ''
const catId = (p: ApiProduct) =>
  p.categoryId !== undefined ? String(p.categoryId) : typeof p.category === 'object' ? String(p.category?.categoryId ?? p.category?.id ?? '') : ''
const shortDesc = (p: ApiProduct) => {
  const t = p.description || p.discription || 'High-performance hardware selected for stability, speed and durability.'
  return t.length > 110 ? `${t.slice(0, 110)}…` : t
}
function imgError(e: Event) {
  const el = e.target as HTMLImageElement
  el.style.display = 'none'
  el.parentElement?.querySelector('.ph')?.classList.remove('hidden')
}

/* ---------- API dynamic data states ---------- */
const rawCategories = ref<any[]>([])
const rawDeals = ref<DealItem[]>([])
const rawReviews = ref<ReviewResponse[]>([])
const loadingCategories = ref(false)
const loadingDeals = ref(false)
const loadingReviews = ref(false)
const brokenCatImages = ref<Record<string, boolean>>({})

function onCatImgError(id: string) {
  brokenCatImages.value[id] = true
}

/* ---------- Reviews aggregation & rating calculation ---------- */
const reviewsByProductId = computed(() => {
  const map = new Map<string, { ratings: number[]; count: number; avg: number }>()
  for (const r of rawReviews.value) {
    const rawId = r.productId != null ? String(r.productId).trim() : ''
    if (!rawId) continue
    const key = rawId.toLowerCase()
    const score = Number(r.rating)
    if (!Number.isFinite(score) || score <= 0) continue
    let entry = map.get(key)
    if (!entry) {
      entry = { ratings: [], count: 0, avg: 0 }
      map.set(key, entry)
    }
    entry.ratings.push(score)
    entry.count++
  }
  for (const entry of map.values()) {
    if (entry.count > 0) {
      entry.avg = entry.ratings.reduce((a, b) => a + b, 0) / entry.count
    }
  }
  return map
})

const rating = (p: ApiProduct | null | undefined): number => {
  if (!p) return 0
  const id = pid(p).toLowerCase()
  if (id && reviewsByProductId.value.has(id)) {
    return Math.min(5, Math.max(1, reviewsByProductId.value.get(id)!.avg))
  }
  if (Array.isArray(p.reviews) && p.reviews.length > 0) {
    const valid = p.reviews.map((r: any) => Number(r.rating)).filter((s) => Number.isFinite(s) && s > 0)
    if (valid.length > 0) return Math.min(5, Math.max(1, valid.reduce((a, b) => a + b, 0) / valid.length))
  }
  const direct = Number(p.rating)
  return Number.isFinite(direct) && direct > 0 ? Math.min(5, Math.max(0, direct)) : 0
}

const reviewCount = (p: ApiProduct | null | undefined): number => {
  if (!p) return 0
  const id = pid(p).toLowerCase()
  if (id && reviewsByProductId.value.has(id)) {
    return reviewsByProductId.value.get(id)!.count
  }
  if (Array.isArray(p.reviews) && p.reviews.length > 0) {
    return p.reviews.length
  }
  const direct = Number(p.reviewCount)
  return Number.isFinite(direct) && direct > 0 ? direct : 0
}

/* ---------- Derived lists ---------- */
const categories = computed(() => {
  if (rawCategories.value.length > 0) {
    return rawCategories.value.map((cat, i) => {
      const id = String(cat.categoryId ?? cat.id ?? '')
      const name = cat.categoryName ?? cat.name ?? ''
      const matchingProducts = allProducts.value.filter((p) => {
        const pCatId = catId(p)
        const pCatName = catName(p)
        return (pCatId && pCatId === id) || (pCatName && pCatName.toLowerCase() === name.toLowerCase())
      })
      const count = matchingProducts.length
      const fallbackProd = matchingProducts.find((p) => image(p))
      const fallbackImg = fallbackProd ? image(fallbackProd) : ''
      const rawCatImg = cat.categoryImage || cat.image || ''
      const resolvedImg = cleanUrl(rawCatImg) || fallbackImg

      return {
        id,
        name,
        description: cat.description || '',
        categoryImage: resolvedImg,
        count,
        icon: iconFor(name, i),
        tone: tones[i % tones.length]
      }
    })
  }

  const map = new Map<string, { id: string; name: string; count: number; sampleImage: string }>()
  allProducts.value.forEach((p) => {
    const name = catName(p)
    if (!name) return
    const id = catId(p) || name
    const img = image(p)
    const e = map.get(id)
    if (e) {
      e.count++
      if (!e.sampleImage && img) e.sampleImage = img
    } else {
      map.set(id, { id, name, count: 1, sampleImage: img })
    }
  })
  return [...map.values()].map((c, i) => ({
    ...c,
    description: '',
    categoryImage: c.sampleImage || '',
    icon: iconFor(c.name, i),
    tone: tones[i % tones.length]
  }))
})

const inStock = computed(() => allProducts.value.filter((p) => stock(p) > 0))

const flashDeals = computed(() => {
  if (rawDeals.value.length > 0) {
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    const validDeals = rawDeals.value.filter((d) => {
      if (!d.endDate) return true
      const end = new Date(d.endDate)
      return end >= today
    })
    const listToUse = validDeals.length > 0 ? validDeals : rawDeals.value

    return listToUse.map((d) => {
      const product = allProducts.value.find((p) => pid(p).toLowerCase() === String(d.productId ?? '').trim().toLowerCase())
      const origPrice = Number(d.productOriginalPrice || product?.price || 0)
      const discPct = Number(d.discountPercentage || 10)
      const discountedPrice = Math.round(origPrice * (1 - discPct / 100))

      return {
        ...(product || {}),
        productId: d.productId,
        id: d.productId,
        name: d.productName || product?.name || 'Exclusive Deal Product',
        brand: product?.brand || 'NexusRigs',
        price: discountedPrice,
        originalPrice: origPrice,
        discountPercentage: discPct,
        badgeText: d.badgeText || 'FLASH DEAL',
        image: cleanUrl(d.productImage) || (product ? image(product) : ''),
        images: product?.images?.length ? product.images : (d.productImage ? [d.productImage] : []),
        stock: product ? stock(product) : 8,
        stockQty: product ? stock(product) : 8,
        rating: product && rating(product) > 0 ? rating(product) : 4.8,
        reviewCount: product && reviewCount(product) > 0 ? reviewCount(product) : 12,
        description: product?.description || product?.discription || 'Special limited-time offer on genuine hardware.',
        discription: product?.description || product?.discription || 'Special limited-time offer on genuine hardware.'
      } as ApiProduct & { originalPrice?: number; discountPercentage?: number; badgeText?: string }
    })
  }

  return inStock.value.slice(0, 10).map((p) => ({
    ...p,
    originalPrice: oldPrice(p.price),
    discountPercentage: discount(pid(p)),
    badgeText: 'FLASH DEAL'
  }))
})

const displayedReviews = computed(() => {
  const apiReviews = rawReviews.value
    .filter((r) => r.customerName && r.rating)
    .map((r) => {
      const prod = allProducts.value.find((p) => pid(p).toLowerCase() === String(r.productId ?? '').trim().toLowerCase())
      return {
        name: r.customerName,
        role: prod ? `Verified Buyer · ${prod.name}` : 'Verified Customer',
        item: prod?.categoryName || 'Hardware',
        text: r.comment && r.comment.trim() ? r.comment : `Verified ${r.rating}★ rating for ${prod?.name || 'hardware'}. Excellent service and official warranty.`,
        rating: Math.min(5, Math.max(1, Number(r.rating) || 5)),
        avatar: cleanUrl((r as any).customerImage)
      }
    })
  return [...apiReviews, ...fallbackReviews].slice(0, 6)
})

const heroProduct = computed(() => [...allProducts.value].sort((a, b) => rating(b) - rating(a))[0] || null)

type Tab = 'trending' | 'topRated' | 'newArrivals' | 'inStock'
const tabs: { key: Tab; label: string; icon: any }[] = [
  { key: 'trending', label: 'Trending', icon: Flame },
  { key: 'topRated', label: 'Top rated', icon: Star },
  { key: 'newArrivals', label: 'New arrivals', icon: Sparkles },
  { key: 'inStock', label: 'In stock', icon: Zap }
]
const activeTab = ref<Tab>('trending')
const budget = ref('all')
const budgetFilters = [{ label: 'All budgets', key: 'all' }, ...budgets]

const displayed = computed(() => {
  const all = allProducts.value
  let list: ApiProduct[]
  if (activeTab.value === 'topRated') {
    const top = all.filter((p) => rating(p) >= 4).sort((a, b) => rating(b) - rating(a) || reviewCount(b) - reviewCount(a))
    list = top.length > 0 ? top : all.filter((p) => rating(p) > 0).sort((a, b) => rating(b) - rating(a))
  } else if (activeTab.value === 'newArrivals') list = [...all].reverse()
  else if (activeTab.value === 'inStock') list = inStock.value
  else list = [...all].sort((a, b) => rating(b) - rating(a) || reviewCount(b) - reviewCount(a))
  list = list.slice(0, 18)
  const ranges: Record<string, (n: number) => boolean> = {
    under50k: (n) => n < 50000, '50to150k': (n) => n >= 50000 && n <= 150000,
    '150to300k': (n) => n > 150000 && n <= 300000, above300k: (n) => n > 300000
  }
  return budget.value === 'all' ? list : list.filter((p) => ranges[budget.value](Number(p.price || 0)))
})

const brandCount = computed(() => new Set(allProducts.value.map((p) => (p.brand || '').trim()).filter(Boolean)).size)
const stats = computed(() => [
  { label: 'Products', value: allProducts.value.length, icon: Boxes },
  { label: 'Categories', value: categories.value.length, icon: CircuitBoard },
  { label: 'Brands', value: brandCount.value, icon: BadgeCheck },
  { label: 'In stock now', value: inStock.value.length, icon: CheckCircle2 }
])

/* =====================================================================
   Smart Build Matcher — pick a goal + budget, get a live parts list
   from your real in-stock catalog.
   ===================================================================== */
const partDefs = {
  gpu: { label: 'Graphics card', icon: PcCase, re: /graphic|gpu|video card|rtx|radeon/i, bar: 'bg-blue-500' },
  cpu: { label: 'Processor', icon: Cpu, re: /proces|cpu|ryzen|intel/i, bar: 'bg-indigo-500' },
  ram: { label: 'Memory', icon: MemoryStick, re: /memory|ram|ddr/i, bar: 'bg-cyan-500' },
  ssd: { label: 'Storage', icon: HardDrive, re: /storage|ssd|nvme|hdd|drive|disk/i, bar: 'bg-emerald-500' },
  cool: { label: 'Cooling', icon: Fan, re: /cool|fan|liquid|aio/i, bar: 'bg-sky-500' },
  psu: { label: 'Power supply', icon: Plug, re: /power|psu|supply/i, bar: 'bg-amber-500' },
  case: { label: 'Case', icon: Boxes, re: /case|chassis|cabinet/i, bar: 'bg-violet-500' }
} as const
type PartKey = keyof typeof partDefs

const matchRules: Record<string, { tiers: string[]; tip: string; split: Record<PartKey, number> }> = {
  gaming: {
    tiers: ['1080p esports', '1080p high refresh', '1440p high refresh', '4K ultra'],
    tip: 'For games, the GPU is the biggest lever, so it gets the largest share of your budget.',
    split: { gpu: 38, cpu: 20, ram: 10, ssd: 10, cool: 8, psu: 7, case: 7 }
  },
  creator: {
    tiers: ['1080p editing', '4K editing', '4K multicam + 3D', '8K & heavy 3D'],
    tip: 'Rendering loves cores and fast scratch storage, so CPU and SSD get a bigger slice.',
    split: { gpu: 28, cpu: 28, ram: 14, ssd: 14, cool: 6, psu: 5, case: 5 }
  },
  ai: {
    tiers: ['Learning & inference', 'Small-model fine-tuning', 'Local LLM workstation', 'Large-model workstation'],
    tip: 'VRAM decides which models fit, so most of the budget goes to the GPU.',
    split: { gpu: 50, cpu: 14, ram: 12, ssd: 10, cool: 5, psu: 6, case: 3 }
  },
  office: {
    tiers: ['Everyday tasks', 'Heavy multitasking', 'Power office & dev', 'Overkill, in a good way'],
    tip: 'A quiet, fast-booting desk PC needs a good SSD and case more than a big GPU.',
    split: { gpu: 8, cpu: 25, ram: 15, ssd: 18, cool: 6, psu: 10, case: 18 }
  }
}
const matchGoal = ref('gaming')
const matchBudget = ref(350000)
const matcherEl = ref<HTMLElement | null>(null)
const tierIdx = computed(() => (matchBudget.value < 150000 ? 0 : matchBudget.value < 300000 ? 1 : matchBudget.value < 600000 ? 2 : 3))

function pickFor(re: RegExp, cap: number): ApiProduct | null {
  const pool = inStock.value.filter((p) => re.test(catName(p)) && Number(p.price || 0) <= cap)
  pool.sort((a, b) => Number(b.price || 0) - Number(a.price || 0) || rating(b) - rating(a))
  return pool[0] || null
}

const matcher = computed(() => {
  const rule = matchRules[matchGoal.value]
  const rows = (Object.entries(rule.split) as [PartKey, number][]).map(([key, pct]) => {
    const def = partDefs[key]
    const cap = (matchBudget.value * pct) / 100
    return { key, pct, cap, label: def.label, icon: def.icon, bar: def.bar, product: pickFor(def.re, cap) }
  })
  const total = rows.reduce((t, r) => t + Number(r.product?.price || 0), 0)
  return { rows, total, picked: rows.filter((r) => r.product).length }
})

function pickGoal(key: string) {
  matchGoal.value = key
  nextTick(() => matcherEl.value?.scrollIntoView({ behavior: 'smooth', block: 'start' }))
}
function openInBuilder() {
  router.push({ path: '/builds', query: { goal: matchGoal.value, budget: String(matchBudget.value) } })
}
const addingAll = ref(false)
async function addPicks() {
  const items = matcher.value.rows.map((r) => r.product).filter(Boolean) as ApiProduct[]
  if (!items.length) return
  addingAll.value = true
  for (const p of items) { try { await cartStore.addItem(pid(p), 1) } catch { /* store reports */ } }
  addingAll.value = false
  notify.success(`${items.length} matched parts added to your cart.`, 'Cart updated')
}

/* spotlight that follows the pointer on goal cards (rAF throttled, mouse only, CSS vars only) */
let spotRaf = 0
function spot(e: PointerEvent) {
  if (e.pointerType !== 'mouse') return
  const el = e.currentTarget as HTMLElement
  const { clientX, clientY } = e
  cancelAnimationFrame(spotRaf)
  spotRaf = requestAnimationFrame(() => {
    const r = el.getBoundingClientRect()
    el.style.setProperty('--mx', `${clientX - r.left}px`)
    el.style.setProperty('--my', `${clientY - r.top}px`)
  })
}

/* =====================================================================
   Ctrl/⌘ + K quick search
   ===================================================================== */
const searchOpen = ref(false)
const searchQ = ref('')
const searchInput = ref<HTMLInputElement | null>(null)
const searchResults = computed(() => {
  const s = searchQ.value.trim().toLowerCase()
  if (!s) return []
  return allProducts.value.filter((p) => `${p.name || ''} ${p.brand || ''} ${catName(p)}`.toLowerCase().includes(s)).slice(0, 6)
})
function openSearch() {
  searchOpen.value = true
  searchQ.value = ''
  nextTick(() => searchInput.value?.focus())
}
function submitSearch() {
  const q = searchQ.value.trim()
  searchOpen.value = false
  goToCatalog(q ? { search: q } : undefined)
}
function onKey(e: KeyboardEvent) {
  const tag = (e.target as HTMLElement | null)?.tagName
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); openSearch(); return }
  if (e.key === '/' && tag !== 'INPUT' && tag !== 'TEXTAREA') { e.preventDefault(); openSearch(); return }
  if (e.key === 'Escape') { searchOpen.value = false; quick.value = null }
}

/* ---------- Navigation & actions ---------- */
function goToCatalog(q?: { categoryId?: string; category?: string; search?: string; maxPrice?: number }) { router.push({ path: '/catalog', query: q }) }
function goToProduct(p: ApiProduct) { const id = pid(p); if (id) router.push(`/product/${id}`) }

const addingId = ref<string | null>(null)
async function addToCart(p: ApiProduct, e?: Event, qty = 1) {
  e?.stopPropagation()
  const id = pid(p)
  if (!id || stock(p) <= 0) return
  try {
    addingId.value = id
    await cartStore.addItem(id, qty)
    notify.success(`${p.name || 'Item'} added to your cart.`, 'Cart updated')
  } catch { /* cartStore reports its own errors */ } finally { addingId.value = null }
}

/* wishlist now survives page refreshes */
const wishlist = ref<Record<string, boolean>>({})
try { wishlist.value = JSON.parse(localStorage.getItem('nx-wishlist') || '{}') } catch { /* ignore */ }
watch(wishlist, (v) => { try { localStorage.setItem('nx-wishlist', JSON.stringify(v)) } catch { /* ignore */ } }, { deep: true })
function toggleWishlist(p: ApiProduct, e: Event) {
  e.stopPropagation()
  const id = pid(p)
  if (!id) return
  wishlist.value[id] = !wishlist.value[id]
  wishlist.value[id] ? notify.success(`"${p.name || 'Item'}" saved to wishlist.`, 'Wishlist') : notify.info('Removed from wishlist.', 'Wishlist')
}

const quick = ref<ApiProduct | null>(null)
const quickQty = ref(1)
const openQuick = (p: ApiProduct, e?: Event) => { e?.stopPropagation(); quick.value = p; quickQty.value = 1 }
async function addQuick() {
  if (!quick.value) return
  await addToCart(quick.value, undefined, quickQty.value)
  if (!addingId.value) quick.value = null
}

const showcaseEl = ref<HTMLElement | null>(null)
function pickBudget(key: string) {
  budget.value = key
  nextTick(() => showcaseEl.value?.scrollIntoView({ behavior: 'smooth', block: 'start' }))
}

const openFaq = ref<number | null>(0)
const email = ref('')
const subscribed = ref(false)
function subscribe() {
  if (!email.value.includes('@')) return
  subscribed.value = true
  notify.success('You are subscribed to restock alerts.', 'Subscribed')
}

/* ---------- Scrolling (rAF throttled, no layout reads inside the scroll handler) ---------- */
const dealsTrack = ref<HTMLElement | null>(null)
const categoryTrack = ref<HTMLElement | null>(null)
const reviewsTrack = ref<HTMLElement | null>(null)
const scrollBy = (el: HTMLElement | null, dir: 1 | -1, d = 320) => el?.scrollBy({ left: dir * d, behavior: 'smooth' })

const progressBar = ref<HTMLElement | null>(null)
const showTop = ref(false)
let ticking = false
let maxScroll = 0
let ro: ResizeObserver | null = null
const measure = () => { maxScroll = document.documentElement.scrollHeight - window.innerHeight }

function onScroll() {
  if (ticking) return
  ticking = true
  requestAnimationFrame(() => {
    const top = window.scrollY
    if (progressBar.value) progressBar.value.style.transform = `scaleX(${maxScroll > 0 ? Math.min(1, top / maxScroll) : 0})`
    const t = top > 500
    if (showTop.value !== t) showTop.value = t
    ticking = false
  })
}
function toTop() { window.scrollTo({ top: 0, behavior: 'smooth' }) }

/* categories, deals and reviews load in parallel instead of one after another */
async function loadHomeData() {
  loadingCategories.value = loadingDeals.value = loadingReviews.value = true
  const [pRes, c, d, r] = await Promise.allSettled([
    productStore.loadProducts(),
    productService.getCategories(),
    dealService.getAll(),
    getAllReviews()
  ])
  if (c.status === 'fulfilled' && Array.isArray(c.value) && c.value.length) rawCategories.value = c.value
  else if (c.status === 'rejected') console.error('Failed to load categories:', c.reason)
  if (d.status === 'fulfilled' && Array.isArray(d.value) && d.value.length) rawDeals.value = d.value
  else if (d.status === 'rejected') console.error('Failed to load deals:', d.reason)
  if (r.status === 'fulfilled' && Array.isArray(r.value)) rawReviews.value = r.value
  else if (r.status === 'rejected') console.error('Failed to load reviews:', r.reason)
  loadingCategories.value = loadingDeals.value = loadingReviews.value = false
}

onMounted(() => {
  loadHomeData()
  measure()
  onScroll()
  ro = new ResizeObserver(measure)
  ro.observe(document.body)
  window.addEventListener('resize', measure, { passive: true })
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('keydown', onKey)
})
onUnmounted(() => {
  ro?.disconnect()
  cancelAnimationFrame(spotRaf)
  window.removeEventListener('resize', measure)
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('keydown', onKey)
})
</script>

<template>
  <div class="home relative min-h-screen overflow-x-hidden text-slate-800 antialiased">
    <!-- background: one fixed GPU layer -->
    <div class="bg-layer" :style="bgStyle" aria-hidden="true" />

    <!-- scroll progress -->
    <div class="pointer-events-none fixed inset-x-0 top-0 z-[100] h-[3px]">
      <div ref="progressBar" class="h-full origin-left bg-blue-600"
        style="transform: scaleX(0); will-change: transform" />
    </div>

    <main class="relative z-10 mx-auto w-full max-w-[1480px] px-4 pb-16 pt-5 sm:px-6 lg:px-8">
      <!-- HERO -->
      <section class="relative touch-pan-y overflow-hidden rounded-3xl bg-slate-950 shadow-2xl shadow-slate-900/20"
        @pointerenter="hoverPause($event, true)" @pointerleave="hoverPause($event, false)"
        @touchstart.passive="onTouchStart" @touchend.passive="onTouchEnd">
        <div class="relative h-[470px] sm:h-[420px] lg:h-[460px]">
          <div v-for="(s, i) in slides" :key="s.tag"
            :class="['absolute inset-0 transition-opacity duration-700', current === i ? 'z-10 opacity-100' : 'pointer-events-none opacity-0']">
            <img :src="s.img" :alt="s.title" class="absolute inset-0 h-full w-full object-cover" decoding="async"
              :loading="i === 0 ? 'eager' : 'lazy'" :fetchpriority="i === 0 ? 'high' : 'auto'" />
            <div class="absolute inset-0 bg-gradient-to-r from-slate-950 via-slate-950/80 to-slate-950/10" />
            <div class="absolute inset-0 bg-slate-950/45 sm:hidden" />
            <div class="relative flex h-full items-center px-5 pb-20 pt-4 sm:px-12 sm:pb-16 sm:pt-0 lg:px-16">
              <div class="max-w-xl">
                <span
                  class="inline-flex items-center rounded-full bg-white/15 px-3 py-1 text-xs font-bold text-cyan-200 ring-1 ring-white/15">{{
                    s.badge }}</span>
                <h1
                  class="mt-4 text-3xl font-extrabold leading-[1.05] tracking-tight text-white min-[400px]:text-4xl sm:text-5xl">
                  {{ s.title }} <span class="text-cyan-300">{{ s.highlight }}</span>
                </h1>
                <p
                  class="mt-3 line-clamp-3 max-w-md text-sm leading-6 text-slate-300 sm:mt-4 sm:line-clamp-none sm:text-base">
                  {{ s.subtitle }}</p>
                <div class="mt-5 flex flex-wrap gap-2 sm:mt-7 sm:gap-3">
                  <button class="btn-primary" @click="router.push(s.link)">{{ s.cta }}
                    <ArrowRight class="h-4 w-4" />
                  </button>
                  <button class="btn-glass" @click="goToCatalog()">Browse catalog</button>
                </div>
              </div>
            </div>
          </div>

          <!-- floating assurance -->
          <div class="absolute right-6 top-6 z-20 hidden flex-col gap-2 lg:flex">
            <div v-for="b in heroBadges" :key="b.t"
              class="flex items-center gap-3 rounded-2xl bg-slate-900/55 px-4 py-3 ring-1 ring-white/15">
              <component :is="b.i" class="h-5 w-5 text-cyan-300" />
              <div>
                <p class="text-xs font-semibold text-white">{{ b.t }}</p>
                <p class="text-[11px] text-slate-300">{{ b.c }}</p>
              </div>
            </div>
          </div>

          <!-- controls -->
          <div
            class="absolute inset-x-4 bottom-4 z-20 flex items-center justify-between gap-3 sm:inset-x-5 sm:bottom-5">
            <div class="flex min-w-0 gap-1.5 overflow-x-auto no-scrollbar">
              <button v-for="(s, i) in slides" :key="s.tag" :aria-label="`Go to ${s.tag}`"
                :class="['rounded-full px-3 py-1.5 text-xs font-semibold transition', current === i ? 'bg-white text-slate-900' : 'bg-white/10 text-slate-200 hover:bg-white/20']"
                @click="goTo(i)">{{ s.tag }}</button>
            </div>
            <div class="flex shrink-0 gap-1.5">
              <button aria-label="Previous slide" class="icon-btn-dark" @click="prev">
                <ChevronLeft class="h-4 w-4" />
              </button>
              <button :aria-label="paused ? 'Resume' : 'Pause'" class="icon-btn-dark" @click="paused = !paused">
                <Play v-if="paused" class="h-4 w-4" />
                <Pause v-else class="h-4 w-4" />
              </button>
              <button aria-label="Next slide" class="icon-btn-dark" @click="next">
                <ChevronRight class="h-4 w-4" />
              </button>
            </div>
          </div>
          <div class="absolute inset-x-0 bottom-0 z-20 h-0.5 bg-white/10">
            <div :key="current" class="progress-bar" :style="{ animationPlayState: paused ? 'paused' : 'running' }"
              @animationend="next" />
          </div>
        </div>
      </section>

      <!-- TRUST STRIP -->
      <section class="mt-5 grid grid-cols-2 gap-3 lg:grid-cols-4">
        <div v-for="t in trust" :key="t.title" class="card flex items-center gap-3 p-3 sm:p-4">
          <span class="icon-tile">
            <component :is="t.icon" class="h-5 w-5" />
          </span>
          <div class="min-w-0">
            <p class="text-[13px] font-bold leading-tight text-slate-900 sm:truncate sm:text-sm">{{ t.title }}</p>
            <p class="mt-0.5 text-[11px] leading-snug text-slate-500 sm:truncate sm:text-xs">{{ t.copy }}</p>
          </div>
        </div>
      </section>

      <!-- ANNOUNCEMENT MARQUEE -->
      <div class="marquee mb-3 mt-5 max-w-full overflow-hidden rounded-lg bg-white/40 py-2" aria-hidden="true">
        <div class="marquee-track">
          <span v-for="(a, i) in announcementLoop" :key="i"
            class="mx-6 flex shrink-0 items-center gap-2 text-xs font-semibold text-slate-600">
            <component :is="announcementIcons[i % announcementIcons.length]"
              class="h-3.5 w-3.5 shrink-0 text-blue-500" />
            {{ a }}
          </span>
        </div>
      </div>

      <!-- SHOP BY GOAL -->
      <section class="mt-14 sm:mt-20">
        <div class="mt-4 flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h2 class="text-2xl font-extrabold tracking-tight text-slate-800 sm:text-3xl lg:text-[2.15rem]">
              Start with what
              <span class="bg-gradient-to-r from-sky-600 via-blue-500 to-sky-500 bg-clip-text text-transparent">you're
                building</span>
            </h2>
            <p class="mt-3 max-w-2xl text-sm leading-6 text-slate-500 sm:text-[15px]">
              Pick your main objective and we'll shape the hardware balance for performance, thermals, reliability and
              future upgrades. Selecting a goal loads a live parts list below.
            </p>
          </div>
          <div
            class="hidden items-center gap-3 rounded-2xl border border-white/70 bg-white/70 px-4 py-2.5 shadow-sm sm:flex">
            <div
              class="flex h-9 w-9 items-center justify-center rounded-xl border border-sky-100 bg-sky-50 text-sky-600">
              <Cpu class="h-5 w-5" />
            </div>
            <div>
              <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">Engine Active</p>
              <p class="text-xs font-bold text-slate-700">Smart Hardware Matcher</p>
            </div>
            <span class="relative ml-1 flex h-2 w-2">
              <span class="absolute inline-flex h-2 w-2 animate-ping rounded-full bg-emerald-400 opacity-75"></span>
              <span class="relative inline-flex h-2 w-2 rounded-full bg-emerald-500"></span>
            </span>
          </div>
        </div>

        <div class="mt-7 grid grid-cols-1 gap-5 sm:mt-9 sm:grid-cols-2 sm:gap-6 lg:grid-cols-4">
          <button v-for="(g, index) in goals" :key="g.key" type="button"
            :class="['goal group', matchGoal === g.key && 'goal-active']" @pointermove="spot" @click="pickGoal(g.key)">
            <span class="goal-num" aria-hidden="true">0{{ index + 1 }}</span>
            <div class="goal-body flex flex-1 flex-col">
              <div class="flex items-start justify-between">
                <div class="flex items-center gap-3">
                  <span class="goal-icon">
                    <component :is="g.icon" class="h-6 w-6" />
                  </span>
                  <div>
                    <span class="goal-chip">{{ g.badge }}</span>
                    <div class="mt-1.5 flex items-center gap-1.5">
                      <span class="h-1.5 w-1.5 rounded-full bg-emerald-500"></span>
                      <span class="text-[10px] font-semibold text-slate-400">{{ g.status }}</span>
                    </div>
                  </div>
                </div>
                <span class="goal-arrow">
                  <ArrowUpRight class="h-4 w-4" />
                </span>
              </div>

              <h3
                class="mt-6 text-lg font-extrabold tracking-tight text-slate-800 transition-colors group-hover:text-sky-600">
                {{ g.title }}</h3>
              <p class="mt-2.5 text-sm leading-6 text-slate-500">{{ g.copy }}</p>

              <div class="goal-profile mt-5">
                <div class="flex items-center justify-between">
                  <span class="flex items-center gap-2 text-sm font-bold text-slate-700">
                    <SlidersHorizontal class="h-4 w-4 text-sky-600" /> Hardware Profile
                  </span>
                  <span
                    class="rounded-md border border-sky-200/60 bg-white px-2 py-1 text-[9px] font-extrabold text-sky-600">{{
                      g.tag }}</span>
                </div>
                <div class="mt-3 grid grid-cols-3 gap-2">
                  <div v-for="m in g.metrics" :key="m.label" class="goal-metric">
                    <component :is="m.icon" class="mx-auto h-4 w-4 text-slate-400 group-hover:text-sky-600" />
                    <span class="mt-1.5 block text-[10px] font-bold text-slate-800">{{ m.value }}</span>
                    <span class="text-[9px] font-medium text-slate-400">{{ m.label }}</span>
                  </div>
                </div>
              </div>

              <div class="mt-auto flex items-center justify-between border-t border-slate-200/70 pt-3 mt-5">
                <p class="text-sm font-bold text-slate-600 transition-colors group-hover:text-sky-600">{{ matchGoal ===
                  g.key ? 'Selected below' : g.ctaText }}</p>
                <span class="goal-go">
                  <ArrowRight class="h-4 w-4" />
                </span>
              </div>
            </div>
          </button>
        </div>
      </section>

      <!-- FLASH DEALS -->
      <section
        class="cv mt-10 sm:mt-14 rounded-3xl border border-amber-200 bg-gradient-to-br from-amber-50 via-white to-orange-50 p-5 sm:p-7">
        <div class="flex flex-col justify-between gap-4 md:flex-row md:items-center">
          <div>
            <span
              class="inline-flex items-center gap-1.5 rounded-full bg-red-600 px-3 py-1 text-xs font-bold text-white">
              <Flame class="h-3.5 w-3.5" /> Flash sale
            </span>
            <h2 class="mt-2 text-2xl font-extrabold tracking-tight text-slate-900 sm:text-3xl">Today's super deals</h2>
          </div>
          <div class="flex flex-wrap items-center gap-3">
            <span class="flex items-center gap-1.5 text-xs font-semibold text-slate-500">
              <Clock class="h-4 w-4" /> Ends tonight in
            </span>
            <Countdown />
            <button aria-label="Scroll deals left" class="icon-btn hide-xs" @click="scrollBy(dealsTrack, -1)">
              <ChevronLeft class="h-4 w-4" />
            </button>
            <button aria-label="Scroll deals right" class="icon-btn hide-xs" @click="scrollBy(dealsTrack, 1)">
              <ChevronRight class="h-4 w-4" />
            </button>
          </div>
        </div>

        <div ref="dealsTrack" class="no-scrollbar mt-5 flex snap-x gap-4 overflow-x-auto pb-2">
          <article v-for="p in flashDeals" :key="pid(p)"
            class="card group flex w-[250px] shrink-0 snap-start flex-col justify-between p-3.5 transition-transform hover:-translate-y-1 hover:border-amber-300 hover:shadow-lg">
            <div>
              <div class="relative h-40 cursor-pointer rounded-xl bg-slate-50 p-3" @click="goToProduct(p)">
                <span
                  class="absolute left-2.5 top-2.5 z-10 rounded-full bg-red-600 px-2 py-0.5 text-[11px] font-bold text-white">-{{
                    (p as any).discountPercentage ?? discount(pid(p)) }}%</span>
                <button aria-label="Toggle wishlist"
                  :class="['icon-btn absolute right-2.5 top-2.5 z-10 !h-8 !w-8', wishlist[pid(p)] && '!border-rose-300 !text-rose-500']"
                  @click="toggleWishlist(p, $event)">
                  <Heart class="h-4 w-4" :fill="wishlist[pid(p)] ? 'currentColor' : 'none'" />
                </button>
                <img v-if="image(p)" :src="image(p)" :alt="p.name" loading="lazy" decoding="async"
                  class="h-full w-full object-contain transition-transform duration-500 group-hover:scale-105"
                  @error="imgError" />
                <div class="ph hidden h-full items-center justify-center text-slate-300">
                  <Cpu class="h-10 w-10" />
                </div>
              </div>
              <p class="mt-3 truncate text-xs font-semibold text-blue-600">{{ p.brand || 'NexusRigs' }}</p>
              <h3
                class="mt-1 line-clamp-2 min-h-[2.5rem] cursor-pointer text-sm font-semibold text-slate-900 group-hover:text-blue-600"
                @click="goToProduct(p)">{{ p.name || 'Unnamed product' }}</h3>
              <div class="mt-2 flex items-center justify-between text-xs text-slate-500"><span>Stock</span><span
                  class="font-semibold text-emerald-700">{{ stock(p) }} left</span></div>
              <div class="mt-1 h-1.5 overflow-hidden rounded-full bg-slate-100">
                <div class="h-full rounded-full bg-gradient-to-r from-amber-400 to-red-500"
                  :style="{ width: Math.min(stock(p) * 12, 90) + '%' }" />
              </div>
            </div>
            <div class="mt-4 flex items-end justify-between border-t border-slate-100 pt-3">
              <div>
                <p class="text-xs text-slate-400 line-through">Rs. {{ money((p as any).originalPrice ??
                  oldPrice(p.price)) }}</p>
                <p class="text-base font-extrabold text-slate-900">Rs. {{ money(p.price) }}</p>
              </div>
              <div class="flex gap-1.5">
                <button aria-label="Quick view" class="icon-btn" @click="openQuick(p, $event)">
                  <Eye class="h-4 w-4" />
                </button>
                <button class="btn-amber" :disabled="addingId === pid(p)" @click="addToCart(p, $event)">
                  <ShoppingCart class="h-4 w-4" /> Add
                </button>
              </div>
            </div>
          </article>
          <p v-if="!flashDeals.length" class="empty">No deals available right now.</p>
        </div>
      </section>

      <!-- CATEGORIES -->
      <section class="cv mt-10 sm:mt-14">
        <div class="flex items-end justify-between gap-4">
          <div>
            <h2 class="h2">Shop by category</h2>
            <p class="sub">Explore hardware components, gear and peripherals.</p>
          </div>
          <div class="flex items-center gap-2">
            <button class="btn-outline hide-xs" @click="goToCatalog()">All collections
              <ArrowRight class="h-4 w-4" />
            </button>
            <button aria-label="Scroll categories left" class="icon-btn hide-xs"
              @click="scrollBy(categoryTrack, -1, 300)">
              <ChevronLeft class="h-4 w-4" />
            </button>
            <button aria-label="Scroll categories right" class="icon-btn hide-xs"
              @click="scrollBy(categoryTrack, 1, 300)">
              <ChevronRight class="h-4 w-4" />
            </button>
          </div>
        </div>

        <!-- Skeleton loader while fetching categories -->
        <div v-if="loadingCategories && !categories.length" class="no-scrollbar mt-5 flex gap-3 overflow-x-auto pb-2">
          <div v-for="n in 6" :key="n" class="card w-[170px] sm:w-[195px] shrink-0 animate-pulse p-3">
            <div class="h-28 sm:h-32 w-full rounded-2xl bg-slate-100" />
            <div class="mx-auto mt-3 h-4 w-3/4 rounded bg-slate-100" />
            <div class="mx-auto mt-2 h-3 w-1/2 rounded bg-slate-100" />
          </div>
        </div>

        <!-- Category list -->
        <div v-else ref="categoryTrack" class="no-scrollbar mt-5 flex snap-x gap-3 overflow-x-auto pb-2">
          <button v-for="c in categories" :key="c.id"
            class="card group relative flex w-[170px] sm:w-[195px] shrink-0 snap-start flex-col overflow-hidden p-3 text-left transition-all duration-300 hover:-translate-y-1.5 hover:border-blue-300 hover:shadow-xl hover:shadow-blue-500/10"
            @click="goToCatalog({ categoryId: c.id, category: c.name })">
            
            <!-- Category Image Viewport -->
            <div class="relative flex h-28 w-full items-center justify-center overflow-hidden rounded-2xl border border-slate-100/80 bg-gradient-to-b from-slate-100/90 via-slate-50 to-white p-2.5 sm:h-32">
              <img
                v-if="c.categoryImage && !brokenCatImages[c.id]"
                :src="c.categoryImage"
                :alt="c.name"
                loading="lazy"
                decoding="async"
                class="h-full w-full object-contain transition-transform duration-500 ease-out group-hover:scale-110"
                @error="onCatImgError(c.id)"
              />
              <div v-else class="flex h-full w-full items-center justify-center">
                <span :class="['flex h-14 w-14 items-center justify-center rounded-2xl border transition group-hover:scale-110', c.tone]">
                  <component :is="c.icon" class="h-7 w-7" />
                </span>
              </div>

              <!-- Category Tone Icon Pill (shown top-left when image is present) -->
              <span
                v-if="c.categoryImage && !brokenCatImages[c.id]"
                :class="['absolute left-2.5 top-2.5 flex h-7 w-7 items-center justify-center rounded-lg border text-xs shadow-sm backdrop-blur-md transition-transform duration-300 group-hover:scale-110', c.tone]">
                <component :is="c.icon" class="h-3.5 w-3.5" />
              </span>

              <!-- Subtle hover highlight overlay -->
              <div class="pointer-events-none absolute inset-0 rounded-2xl bg-gradient-to-t from-slate-900/10 via-transparent to-transparent opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
            </div>

            <!-- Category Info -->
            <div class="mt-3 flex w-full flex-col px-0.5">
              <div class="flex items-center justify-between gap-1">
                <h3 class="truncate text-sm font-bold text-slate-800 transition-colors group-hover:text-blue-600">
                  {{ c.name }}
                </h3>
                <ArrowRight class="h-3.5 w-3.5 shrink-0 -translate-x-1 text-blue-600 opacity-0 transition-all duration-300 group-hover:translate-x-0 group-hover:opacity-100" />
              </div>
              <div class="mt-1 flex items-center justify-between text-xs text-slate-400">
                <span>{{ c.count }} {{ c.count === 1 ? 'item' : 'items' }}</span>
                <span class="text-[10px] font-semibold text-blue-500 opacity-0 transition-opacity group-hover:opacity-100">Shop now</span>
              </div>
            </div>
          </button>

          <p v-if="!categories.length" class="empty">Categories appear once products load.</p>
        </div>
      </section>

      <!-- SHOP BY BUDGET -->
      <section class="cv mt-10 sm:mt-14 grid grid-cols-2 gap-3 lg:grid-cols-4">
        <button v-for="b in budgets" :key="b.key"
          class="card group p-4 text-left transition hover:border-blue-300 hover:shadow-lg sm:p-5"
          @click="pickBudget(b.key)">
          <p class="text-xs font-semibold text-blue-600">Shop by budget</p>
          <p class="mt-1 text-base font-extrabold text-slate-900 sm:text-xl">{{ b.label }}</p>
          <p class="mt-1 flex items-center justify-between gap-2 text-[11px] text-slate-500 sm:text-xs">{{ b.copy }}
            <ArrowRight class="h-4 w-4 text-slate-300 transition group-hover:text-blue-600" />
          </p>
        </button>
      </section>

      <!-- PROMO BLOCKS -->
      <section class="cv mt-10 sm:mt-14 grid gap-4 lg:grid-cols-2">
        <div
          class="relative overflow-hidden rounded-3xl bg-gradient-to-br from-blue-900 to-slate-950 p-6 text-white sm:p-8">
          <Gamepad2 class="absolute -bottom-6 -right-4 h-44 w-44 text-white/5" />
          <div class="relative max-w-sm">
            <h3 class="text-2xl font-extrabold tracking-tight sm:text-3xl">Extreme gaming hardware</h3>
            <p class="mt-2 text-sm text-blue-100">Build around graphics, cooling, memory and peripherals that suit your
              setup.</p>
            <button class="btn-white mt-5" @click="goToCatalog({ search: 'gaming' })">Shop gaming
              <ArrowRight class="h-4 w-4" />
            </button>
          </div>
        </div>
        <div
          class="relative overflow-hidden rounded-3xl bg-gradient-to-br from-indigo-900 to-slate-950 p-6 text-white sm:p-8">
          <Palette class="absolute -bottom-6 -right-4 h-44 w-44 text-white/5" />
          <div class="relative max-w-sm">
            <h3 class="text-2xl font-extrabold tracking-tight sm:text-3xl">Workstation performance</h3>
            <p class="mt-2 text-sm text-indigo-100">Faster renders, smoother timelines and more headroom for heavy apps.
            </p>
            <button class="btn-white mt-5" @click="goToCatalog({ search: 'creator' })">Shop workstations
              <ArrowRight class="h-4 w-4" />
            </button>
          </div>
        </div>
      </section>

      <!-- SHOWCASE -->
      <section ref="showcaseEl" class="cv mt-10 sm:mt-14 scroll-mt-6">
        <div class="flex flex-col justify-between gap-4 md:flex-row md:items-end">
          <div>
            <h2 class="h2">Featured hardware</h2>
            <p class="sub">Live from our catalog.</p>
          </div>
          <div class="no-scrollbar flex max-w-full overflow-x-auto rounded-2xl border border-slate-200 bg-white p-1.5">
            <button v-for="t in tabs" :key="t.key"
              :class="['flex shrink-0 items-center gap-1.5 rounded-xl px-3.5 py-2 text-xs font-semibold transition', activeTab === t.key ? 'bg-blue-600 text-white shadow' : 'text-slate-600 hover:bg-slate-50']"
              @click="activeTab = t.key">
              <component :is="t.icon" class="h-4 w-4" /> {{ t.label }}
            </button>
          </div>
        </div>
        <div class="no-scrollbar mt-4 flex gap-2 overflow-x-auto pb-1">
          <button v-for="f in budgetFilters" :key="f.key"
            :class="['shrink-0 rounded-full border px-3.5 py-1.5 text-xs font-semibold transition', budget === f.key ? 'border-slate-900 bg-slate-900 text-white' : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300']"
            @click="budget = f.key">{{ f.label }}</button>
        </div>

        <div v-if="productStore.loading"
          class="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6">
          <div v-for="n in 6" :key="n" class="card animate-pulse p-3.5">
            <div class="h-40 rounded-xl bg-slate-100" />
            <div class="mt-4 h-3 w-1/3 rounded bg-slate-100" />
            <div class="mt-2 h-4 w-3/4 rounded bg-slate-100" />
          </div>
        </div>
        <div v-else-if="displayed.length"
          class="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6">
          <article v-for="p in displayed" :key="pid(p)"
            class="card group flex min-w-0 flex-col justify-between p-3.5 transition-transform hover:-translate-y-1 hover:shadow-lg">
            <div>
              <div class="relative h-40 cursor-pointer rounded-xl bg-slate-50 p-3" @click="goToProduct(p)">
                <span v-if="stock(p) > 5" class="badge bg-emerald-50 text-emerald-700 ring-emerald-200">In stock</span>
                <span v-else-if="stock(p) > 0" class="badge bg-amber-50 text-amber-700 ring-amber-200">{{ stock(p) }}
                  left</span>
                <span v-else class="badge bg-slate-100 text-slate-500 ring-slate-200">Sold out</span>
                <button aria-label="Toggle wishlist"
                  :class="['icon-btn absolute right-2.5 top-2.5 z-10 !h-8 !w-8', wishlist[pid(p)] && '!border-rose-300 !text-rose-500']"
                  @click="toggleWishlist(p, $event)">
                  <Heart class="h-4 w-4" :fill="wishlist[pid(p)] ? 'currentColor' : 'none'" />
                </button>
                <img v-if="image(p)" :src="image(p)" :alt="p.name" loading="lazy" decoding="async"
                  class="h-full w-full object-contain transition-transform duration-500 group-hover:scale-105"
                  @error="imgError" />
                <div class="ph hidden h-full items-center justify-center text-slate-300">
                  <Cpu class="h-10 w-10" />
                </div>
              </div>
              <div class="mt-3 flex items-center justify-between gap-2">
                <span class="truncate text-xs font-semibold text-blue-600">{{ p.brand || 'NexusRigs' }}</span>
                <span v-if="rating(p) > 0" class="flex shrink-0 items-center gap-1 text-xs font-semibold text-amber-500">
                  <Star class="h-3.5 w-3.5 fill-current text-amber-400" />
                  <span>{{ rating(p).toFixed(1) }}</span>
                  <span v-if="reviewCount(p) > 0" class="text-[10px] font-normal text-slate-400">({{ reviewCount(p) }})</span>
                </span>
                <span v-else class="flex shrink-0 items-center gap-0.5 text-[11px] font-medium text-slate-400">
                  <Star class="h-3.5 w-3.5 text-slate-300" />
                  <span>New</span>
                </span>
              </div>
              <h3
                class="mt-1 line-clamp-2 min-h-[2.5rem] cursor-pointer text-sm font-semibold text-slate-900 group-hover:text-blue-600"
                @click="goToProduct(p)">{{ p.name || 'Unnamed product' }}</h3>
            </div>
            <div
              class="mt-3 flex flex-wrap items-center justify-between gap-x-1.5 gap-y-2 border-t border-slate-100 pt-3">
              <p class="whitespace-nowrap text-sm font-extrabold text-slate-900">Rs. {{ money(p.price) }}</p>
              <div class="ml-auto flex shrink-0 gap-1">
                <button aria-label="Quick view" class="icon-btn" @click="openQuick(p, $event)">
                  <Eye class="h-4 w-4" />
                </button>
                <button aria-label="Add to cart" class="btn-icon-blue" :disabled="stock(p) <= 0 || addingId === pid(p)"
                  @click="addToCart(p, $event)">
                  <ShoppingCart class="h-4 w-4" />
                </button>
              </div>
            </div>
          </article>
        </div>
        <div v-else class="empty mt-5">
          No hardware matches these filters.
          <button class="btn-primary mx-auto mt-4" @click="budget = 'all'">Reset filters</button>
        </div>
      </section>

      <!-- PC BUILDER -->
      <section
        class="cv mt-10 sm:mt-14 overflow-hidden rounded-3xl border border-blue-100 bg-gradient-to-br from-white via-blue-50/60 to-indigo-50 p-6 sm:p-10">
        <div class="grid items-center gap-8 lg:grid-cols-2">
          <div>
            <span
              class="inline-flex items-center gap-2 rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700 ring-1 ring-blue-200">
              <Wrench class="h-3.5 w-3.5" /> Custom PC builder
            </span>
            <h2 class="mt-4 text-3xl font-extrabold tracking-tight text-slate-950 sm:text-4xl">Spec it, price it, build
              it.</h2>
            <p class="mt-3 max-w-lg text-sm leading-6 text-slate-600">Plan processor, graphics, cooling, RAM and budget
              in one place, with live pricing as you go.</p>
            <ul class="mt-5 grid gap-2 text-sm text-slate-700 sm:grid-cols-2">
              <li v-for="f in ['Real-time pricing', 'Wattage estimate', 'Compatibility checks', 'Budget tracking']"
                :key="f" class="flex items-center gap-2">
                <CheckCircle2 class="h-4 w-4 text-blue-600" />{{ f }}
              </li>
            </ul>
            <div class="mt-6 flex flex-wrap gap-3">
              <router-link to="/builds" class="btn-primary">Launch PC builder
                <ArrowRight class="h-4 w-4" />
              </router-link>
              <router-link to="/deals" class="btn-outline">Ready-to-ship deals</router-link>
            </div>
          </div>
          <div class="relative overflow-hidden rounded-2xl shadow-xl">
            <img :src="customPcImage" alt="Custom PC build" loading="lazy" decoding="async"
              class="h-56 w-full object-cover sm:h-72" />
            <div class="absolute inset-0 bg-gradient-to-t from-slate-950/70 to-transparent" />
            <div
              class="absolute inset-x-4 bottom-4 flex items-center justify-between gap-3 rounded-xl bg-slate-900/85 p-3 ring-1 ring-white/15">
              <p class="text-sm font-semibold text-white">Select parts · Check price · Order</p>
              <router-link to="/builds"
                class="shrink-0 rounded-lg bg-cyan-400/20 px-3 py-1.5 text-xs font-semibold text-cyan-200 hover:bg-cyan-400/30">Configure</router-link>
            </div>
          </div>
        </div>
      </section>

      <!-- HOW IT WORKS -->
      <section class="cv mt-10 sm:mt-14">
        <h2 class="h2 text-center">How ordering works</h2>
        <p class="sub text-center">Four steps from browsing to your doorstep.</p>
        <ol class="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <li v-for="(s, i) in steps" :key="s.title" class="card relative p-6">
            <span class="icon-tile">
              <component :is="s.icon" class="h-5 w-5" />
            </span>
            <span class="absolute right-5 top-5 text-3xl font-black text-slate-100">{{ i + 1 }}</span>
            <h3 class="mt-4 text-sm font-bold text-slate-900">{{ s.title }}</h3>
            <p class="mt-1 text-xs leading-5 text-slate-500">{{ s.copy }}</p>
          </li>
        </ol>
      </section>

      <!-- SPOTLIGHT -->
      <section v-if="heroProduct" class="card cv mt-10 sm:mt-14 overflow-hidden">
        <div class="grid lg:grid-cols-2">
          <div
            class="relative flex min-h-[220px] items-center justify-center border-b border-slate-100 bg-slate-50 p-6 sm:min-h-[300px] sm:p-8 lg:border-b-0 lg:border-r">
            <img v-if="image(heroProduct)" :src="image(heroProduct)" :alt="heroProduct.name" loading="lazy"
              decoding="async" class="max-h-64 max-w-full object-contain" @error="imgError" />
            <span class="badge !left-5 !top-5 bg-blue-50 text-blue-700 ring-blue-200">Top rated pick</span>
          </div>
          <div class="flex flex-col justify-center p-6 sm:p-10">
            <p class="text-xs font-semibold text-blue-600">{{ heroProduct.brand || 'NexusRigs' }}</p>
            <h2 class="mt-1 text-2xl font-extrabold tracking-tight text-slate-900 sm:text-3xl">{{ heroProduct.name }}
            </h2>
            <p class="mt-3 text-sm leading-6 text-slate-600">{{ shortDesc(heroProduct) }}</p>
            <dl class="mt-6 grid grid-cols-3 gap-2 border-y border-slate-100 py-4 sm:gap-3">
              <div>
                <dt class="text-xs text-slate-400">Price</dt>
                <dd class="mt-0.5 break-words text-sm font-extrabold text-slate-900 sm:text-base">Rs. {{
                  money(heroProduct.price) }}</dd>
              </div>
              <div>
                <dt class="text-xs text-slate-400">Rating</dt>
                <dd class="mt-0.5 flex items-center gap-1 font-extrabold text-amber-500">
                  <Star class="h-4 w-4 fill-current text-amber-400" />{{ rating(heroProduct) > 0 ? rating(heroProduct).toFixed(1) : 'New' }}
                </dd>
              </div>
              <div>
                <dt class="text-xs text-slate-400">Stock</dt>
                <dd class="mt-0.5 font-extrabold text-emerald-600">{{ stock(heroProduct) }}</dd>
              </div>
            </dl>
            <div class="mt-6 flex flex-wrap gap-3">
              <button class="btn-primary" @click="goToProduct(heroProduct)">View product</button>
              <button class="btn-outline" :disabled="stock(heroProduct) <= 0 || addingId === pid(heroProduct)"
                @click="addToCart(heroProduct, $event)">
                <ShoppingCart class="h-4 w-4" /> {{ addingId === pid(heroProduct) ? 'Adding…' : 'Add to cart' }}
              </button>
            </div>
          </div>
        </div>
      </section>

      <!-- SERVICES -->
      <section class="cv mt-10 sm:mt-14 rounded-3xl bg-slate-900 p-6 text-white sm:p-10">
        <h2 class="text-2xl font-extrabold tracking-tight sm:text-3xl">Why builders choose NexusRigs</h2>
        <div class="mt-6 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          <div v-for="s in services" :key="s.title">
            <span class="flex h-11 w-11 items-center justify-center rounded-xl bg-white/10 text-cyan-300">
              <component :is="s.icon" class="h-5 w-5" />
            </span>
            <h3 class="mt-3 text-sm font-bold">{{ s.title }}</h3>
            <p class="mt-1 text-xs leading-5 text-slate-400">{{ s.copy }}</p>
          </div>
        </div>
      </section>

      <!-- BRANDS (marquee, pauses on hover) -->
      <section class="cv mt-10 sm:mt-14">
        <h2 class="h2">Brands we stock</h2>
        <div class="marquee marquee-slow mt-5 overflow-hidden">
          <div class="marquee-track">
            <button v-for="(b, i) in brandLoop" :key="i"
              class="card mx-1.5 w-44 shrink-0 py-5 text-sm font-bold text-slate-600 transition hover:border-blue-300 hover:text-blue-600 hover:shadow-md"
              @click="goToCatalog({ search: b })">{{ b }}</button>
          </div>
        </div>
      </section>

      <!-- REVIEWS -->
      <section class="cv mt-10 sm:mt-14">
        <div class="flex items-end justify-between gap-4">
          <div>
            <h2 class="h2">What builders say</h2>
            <p class="sub">Sample customer experiences.</p>
          </div>
          <div class="flex gap-2">
            <button aria-label="Scroll reviews left" class="icon-btn hide-xs" @click="scrollBy(reviewsTrack, -1, 340)">
              <ChevronLeft class="h-4 w-4" />
            </button>
            <button aria-label="Scroll reviews right" class="icon-btn hide-xs" @click="scrollBy(reviewsTrack, 1, 340)">
              <ChevronRight class="h-4 w-4" />
            </button>
          </div>
        </div>
        <div ref="reviewsTrack" class="no-scrollbar mt-5 flex snap-x gap-4 overflow-x-auto pb-2">
          <article v-for="r in displayedReviews" :key="r.name + (r.role || '')"
            class="card flex w-[82vw] max-w-[320px] shrink-0 snap-start flex-col justify-between p-5 sm:w-[320px] sm:p-6">
            <div>
              <div class="flex items-center justify-between">
                <span class="flex text-amber-400">
                  <Star v-for="n in (r.rating || 5)" :key="n" class="h-4 w-4 fill-current" />
                  <Star v-for="n in (5 - (r.rating || 5))" :key="'empty-' + n" class="h-4 w-4 text-slate-200" />
                </span>
                <Quote class="h-6 w-6 text-slate-200" />
              </div>
              <p class="mt-4 text-sm leading-6 text-slate-600">{{ r.text }}</p>
            </div>
            <div class="mt-5 flex items-center gap-3 border-t border-slate-100 pt-4">
              <img v-if="r.avatar" :src="r.avatar" :alt="r.name" class="h-10 w-10 rounded-full object-cover"
                @error="imgError" />
              <span v-else
                class="flex h-10 w-10 items-center justify-center rounded-full bg-blue-600 text-sm font-bold text-white">{{
                  r.name[0] }}</span>
              <div class="min-w-0">
                <p class="truncate text-sm font-semibold text-slate-900">{{ r.name }}</p>
                <p class="truncate text-xs text-slate-400">{{ r.role }}</p>
              </div>
              <span class="ml-auto flex shrink-0 items-center gap-1 text-[11px] font-semibold text-emerald-600">
                <BadgeCheck class="h-4 w-4" />Verified
              </span>
            </div>
          </article>
        </div>
      </section>

      <!-- FAQ -->
      <section class="cv mx-auto mt-10 sm:mt-14 max-w-3xl">
        <h2 class="h2 text-center">Frequently asked questions</h2>
        <div class="mt-6 divide-y divide-slate-200 overflow-hidden rounded-2xl border border-slate-200 bg-white">
          <div v-for="(f, i) in faqs" :key="f.q">
            <button
              class="flex w-full items-center justify-between gap-4 px-5 py-4 text-left text-sm font-semibold text-slate-900 hover:bg-slate-50"
              :aria-expanded="openFaq === i" @click="openFaq = openFaq === i ? null : i">
              {{ f.q }}
              <ChevronDown :class="['h-4 w-4 shrink-0 text-slate-400 transition', openFaq === i && 'rotate-180']" />
            </button>
            <p v-show="openFaq === i" class="px-5 pb-4 text-sm leading-6 text-slate-600">{{ f.a }}</p>
          </div>
        </div>
      </section>

      <!-- NEWSLETTER -->
      <section
        class="cv mt-10 sm:mt-14 rounded-3xl bg-gradient-to-r from-blue-600 to-indigo-600 p-6 text-white sm:p-10">
        <div class="flex flex-col items-center justify-between gap-6 md:flex-row">
          <div class="max-w-xl text-center md:text-left">
            <span class="inline-flex items-center gap-2 text-xs font-semibold text-blue-100">
              <Mail class="h-4 w-4" /> Restock alerts
            </span>
            <h3 class="mt-1 text-2xl font-extrabold sm:text-3xl">Never miss a hardware drop</h3>
            <p class="mt-2 text-sm text-blue-100">Get flash sales, restocks and new arrivals in your inbox.</p>
          </div>
          <form v-if="!subscribed" class="flex w-full max-w-md flex-col gap-2 sm:flex-row" @submit.prevent="subscribe">
            <input v-model="email" type="email" required placeholder="you@example.com" aria-label="Email address"
              class="min-w-0 flex-1 rounded-xl border-0 px-4 py-3 text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-white" />
            <button type="submit"
              class="rounded-xl bg-slate-950 px-6 py-3 text-sm font-bold text-white hover:bg-slate-800">Subscribe</button>
          </form>
          <p v-else class="flex items-center gap-2 rounded-xl bg-white/15 px-4 py-3 text-sm font-semibold">
            <CheckCircle2 class="h-5 w-5" />You're on the list.
          </p>
        </div>
      </section>
    </main>

    <!-- QUICK SEARCH -->
    <Transition name="fade">
      <div v-if="searchOpen"
        class="fixed inset-0 z-[1100] flex items-start justify-center bg-slate-900/60 p-3 pt-[8vh] sm:p-4 sm:pt-[12vh]"
        @click="searchOpen = false">
        <div class="w-full max-w-xl overflow-hidden rounded-2xl bg-white shadow-2xl" role="dialog" aria-modal="true"
          aria-label="Search products" @click.stop>
          <div class="flex items-center gap-3 border-b border-slate-100 px-4">
            <Search class="h-4 w-4 text-slate-400" />
            <input ref="searchInput" v-model="searchQ" type="text" placeholder="Search RTX 4070, DDR5, monitors…"
              aria-label="Search" class="min-w-0 flex-1 py-4 text-sm text-slate-900 placeholder-slate-400 outline-none"
              @keydown.enter="submitSearch" />
            <kbd class="rounded border border-slate-200 px-1.5 py-0.5 font-mono text-[10px] text-slate-400">Esc</kbd>
          </div>
          <ul v-if="searchResults.length" class="max-h-[60vh] divide-y divide-slate-50 overflow-y-auto sm:max-h-80">
            <li v-for="p in searchResults" :key="pid(p)">
              <button class="flex w-full items-center gap-3 px-4 py-3 text-left hover:bg-slate-50"
                @click="searchOpen = false; goToProduct(p)">
                <span
                  class="flex h-11 w-11 shrink-0 items-center justify-center overflow-hidden rounded-lg bg-slate-50">
                  <img v-if="image(p)" :src="image(p)" :alt="p.name" loading="lazy"
                    class="h-full w-full object-contain p-1" @error="imgError" />
                  <Cpu v-else class="h-5 w-5 text-slate-300" />
                </span>
                <span class="min-w-0 flex-1">
                  <span class="block truncate text-sm font-semibold text-slate-900">{{ p.name }}</span>
                  <span class="block truncate text-xs text-slate-400">{{ p.brand }} · {{ catName(p) }}</span>
                </span>
                <span class="shrink-0 text-sm font-extrabold text-slate-900">Rs. {{ money(p.price) }}</span>
              </button>
            </li>
          </ul>
          <div v-else class="p-4">
            <p class="text-xs text-slate-400">{{ searchQ ? 'No matches. Press Enter to search the full catalog.'
              : 'Jump to a category' }}</p>
            <div v-if="!searchQ" class="mt-3 flex flex-wrap gap-2">
              <button v-for="c in categories.slice(0, 8)" :key="c.id"
                class="flex items-center gap-1.5 rounded-full border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-600 hover:border-blue-300 hover:text-blue-600"
                @click="searchOpen = false; goToCatalog({ categoryId: c.id, category: c.name })">
                <component :is="c.icon" class="h-3.5 w-3.5" />{{ c.name }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <!-- QUICK VIEW -->
    <Transition name="fade">
      <div v-if="quick"
        class="fixed inset-0 z-[1000] flex items-center justify-center overflow-y-auto bg-slate-900/30 backdrop-blur-sm p-3 sm:p-4"
        @click="quick = null">
        <div class="relative max-h-[92vh] w-full max-w-2xl overflow-y-auto rounded-3xl bg-white shadow-2xl"
          role="dialog" aria-modal="true" @click.stop>
          <button aria-label="Close" class="icon-btn absolute right-4 top-4 z-20" @click="quick = null">
            <X class="h-4 w-4" />
          </button>
          <div class="grid sm:grid-cols-2">
            <div class="flex min-h-[200px] items-center justify-center bg-slate-50 p-5 sm:min-h-[260px] sm:p-6">
              <img v-if="image(quick)" :src="image(quick)" :alt="quick.name"
                class="max-h-44 max-w-full object-contain sm:max-h-64" @error="imgError" />
            </div>
            <div class="flex flex-col justify-between p-6 sm:p-8">
              <div>
                <p class="text-xs font-semibold text-blue-600">{{ quick.brand || 'NexusRigs' }}</p>
                <h3 class="mt-1 text-lg font-extrabold text-slate-900">{{ quick.name }}</h3>
                <p class="mt-2 flex items-center gap-1 text-xs text-slate-400">
                  <Star class="h-4 w-4 fill-current text-amber-400" />
                  <template v-if="rating(quick) > 0">
                    <b class="text-amber-500">{{ rating(quick).toFixed(1) }}</b> ({{ reviewCount(quick) }} {{ reviewCount(quick) === 1 ? 'review' : 'reviews' }})
                  </template>
                  <template v-else>
                    <span class="text-slate-500">No reviews yet</span>
                  </template>
                </p>
                <p class="mt-3 text-xs leading-6 text-slate-500">{{ shortDesc(quick) }}</p>
                <p class="mt-4 text-2xl font-extrabold text-slate-950">Rs. {{ money(quick.price) }}</p>
              </div>
              <div class="mt-6 space-y-3">
                <div class="flex items-center justify-between rounded-xl border border-slate-200 bg-slate-50 px-3 py-2">
                  <span class="text-xs font-semibold text-slate-600">Quantity</span>
                  <div class="flex items-center gap-2">
                    <button aria-label="Decrease" class="icon-btn !h-7 !w-7" @click="quickQty > 1 && quickQty--">
                      <Minus class="h-3.5 w-3.5" />
                    </button>
                    <span class="w-6 text-center text-sm font-bold">{{ quickQty }}</span>
                    <button aria-label="Increase" class="icon-btn !h-7 !w-7"
                      @click="quickQty < stock(quick) && quickQty++">
                      <Plus class="h-3.5 w-3.5" />
                    </button>
                  </div>
                </div>
                <div class="flex gap-2">
                  <button class="btn-primary flex-1 justify-center"
                    :disabled="stock(quick) <= 0 || addingId === pid(quick)" @click="addQuick">{{ addingId ===
                      pid(quick) ? 'Adding…' : 'Add to cart' }}</button>
                  <button class="btn-outline" @click="goToProduct(quick); quick = null">Full page</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <!-- BACK TO TOP -->
    <Transition name="fade">
      <button v-if="showTop" aria-label="Back to top"
        class="icon-btn fixed bottom-6 right-5 z-[900] !h-11 !w-11 !rounded-2xl shadow-xl" @click="toTop">
        <ArrowUp class="h-4 w-4" />
      </button>
    </Transition>
  </div>
</template>

<style scoped>
.home {
  background-color: #eaf2fb;
  font-family: 'Inter', ui-sans-serif, system-ui, -apple-system, 'Segoe UI', sans-serif;
}

/* ---- performance helpers ---- */
/* one fixed layer: gradient + image painted together, promoted to the GPU once */
.bg-layer {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background-size: cover;
  background-position: center top;
  background-repeat: no-repeat;
  transform: translateZ(0);
}

/* skip layout/paint work for sections that are off-screen */
.cv {
  content-visibility: auto;
  contain-intrinsic-size: auto 640px;
}

/* ---- type & surfaces ---- */
.h2 {
  font-size: clamp(1.5rem, 2vw, 2rem);
  font-weight: 800;
  letter-spacing: -0.02em;
  color: rgb(15 23 42);
  line-height: 1.15;
}

.sub {
  margin-top: 0.35rem;
  font-size: 0.875rem;
  color: rgb(100 116 139);
}

/* no backdrop-filter here: it was the #1 cause of scroll lag */
.card {
  background: rgb(255 255 255 / 0.94);
  border: 1px solid rgb(226 232 240);
  border-radius: 1rem;
  box-shadow: 0 1px 2px rgb(15 23 42 / 0.04);
}

.empty {
  width: 100%;
  border: 1px dashed rgb(203 213 225);
  border-radius: 1rem;
  background: #fff;
  padding: 2.5rem 1rem;
  text-align: center;
  font-size: 0.875rem;
  color: rgb(100 116 139);
}

.icon-tile {
  display: inline-flex;
  height: 2.75rem;
  width: 2.75rem;
  flex: none;
  align-items: center;
  justify-content: center;
  border-radius: 0.75rem;
  background: rgb(239 246 255);
  color: rgb(37 99 235);
}

.icon-btn,
.icon-btn-dark {
  display: inline-flex;
  height: 2.25rem;
  width: 2.25rem;
  align-items: center;
  justify-content: center;
  border-radius: 0.75rem;
  transition: background-color 0.2s, color 0.2s, border-color 0.2s;
}

.icon-btn {
  border: 1px solid rgb(226 232 240);
  background: #fff;
  color: rgb(71 85 105);
}

.icon-btn:hover {
  border-color: rgb(96 165 250);
  color: rgb(37 99 235);
}

.icon-btn-dark {
  background: rgb(15 23 42 / 0.5);
  color: #fff;
}

.icon-btn-dark:hover {
  background: #fff;
  color: rgb(15 23 42);
}

.btn-primary,
.btn-glass,
.btn-outline,
.btn-white,
.btn-amber {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  border-radius: 0.75rem;
  padding: 0.7rem 1.25rem;
  font-size: 0.8125rem;
  font-weight: 700;
  transition: background-color 0.2s, color 0.2s, box-shadow 0.2s;
}

.btn-primary {
  background: rgb(37 99 235);
  color: #fff;
  box-shadow: 0 8px 20px -8px rgb(37 99 235 / 0.6);
}

.btn-primary:hover {
  background: rgb(29 78 216);
}

.btn-glass {
  background: rgb(255 255 255 / 0.14);
  color: #fff;
  box-shadow: inset 0 0 0 1px rgb(255 255 255 / 0.2);
}

.btn-glass:hover {
  background: rgb(255 255 255 / 0.24);
}

.btn-outline {
  background: #fff;
  color: rgb(51 65 85);
  box-shadow: inset 0 0 0 1px rgb(226 232 240);
}

.btn-outline:hover {
  box-shadow: inset 0 0 0 1px rgb(96 165 250);
  color: rgb(37 99 235);
}

.btn-white {
  background: #fff;
  color: rgb(15 23 42);
}

.btn-white:hover {
  background: rgb(239 246 255);
}

.btn-amber {
  background: rgb(245 158 11);
  color: #fff;
  padding: 0 0.75rem;
  height: 2.25rem;
}

.btn-amber:hover {
  background: rgb(217 119 6);
}

.btn-icon-blue {
  display: inline-flex;
  height: 2.25rem;
  width: 2.25rem;
  align-items: center;
  justify-content: center;
  border-radius: 0.75rem;
  background: rgb(37 99 235);
  color: #fff;
}

.btn-icon-blue:hover {
  background: rgb(29 78 216);
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

button:focus-visible,
a:focus-visible,
input:focus-visible {
  outline: 2px solid rgb(37 99 235);
  outline-offset: 2px;
}

.badge {
  position: absolute;
  left: 0.625rem;
  top: 0.625rem;
  z-index: 10;
  border-radius: 9999px;
  padding: 0.15rem 0.6rem;
  font-size: 0.6875rem;
  font-weight: 700;
  box-shadow: inset 0 0 0 1px var(--tw-ring-color, transparent);
}

/* ---- hero progress: pure CSS, no JS timer ---- */
.progress-bar {
  height: 100%;
  background: rgb(34 211 238);
  transform-origin: left;
  animation: slideFill 6s linear forwards;
}

@keyframes slideFill {
  from {
    transform: scaleX(0);
  }

  to {
    transform: scaleX(1);
  }
}

/* ---- marquee (transform only = GPU friendly) ---- */
.marquee-track {
  display: flex;
  width: max-content;
  animation: marquee 38s linear infinite;
  will-change: transform;
}

.marquee-slow .marquee-track {
  animation-duration: 55s;
}

.marquee:hover .marquee-track {
  animation-play-state: paused;
}

@keyframes marquee {
  from {
    transform: translateX(0);
  }

  to {
    transform: translateX(-50%);
  }
}

/* ---- goal cards ---- */
.goal {
  position: relative;
  display: flex;
  min-height: 380px;
  flex-direction: column;
  overflow: hidden;
  border-radius: 1.5rem;
  border: 1px solid rgb(255 255 255 / 0.85);
  background: rgb(255 255 255 / 0.74);
  padding: 1.5rem;
  text-align: left;
  box-shadow: 0 8px 30px rgb(15 23 42 / 0.06);
  /* box-shadow is not transitioned: animating a large blurred shadow repaints every frame */
  transition: transform 0.35s ease, border-color 0.3s;
  contain: layout paint;
}

.goal:hover {
  transform: translateY(-6px);
  border-color: rgb(186 230 253);
  box-shadow: 0 20px 50px rgb(14 165 233 / 0.14);
}

.goal-active {
  border-color: rgb(56 189 248);
  box-shadow: 0 0 0 2px rgb(56 189 248 / 0.35), 0 16px 40px rgb(14 165 233 / 0.14);
}

/* pointer-following spotlight */
.goal::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(260px circle at var(--mx, 50%) var(--my, 0%), rgb(14 165 233 / 0.16), transparent 65%);
  opacity: 0;
  transition: opacity 0.3s;
  pointer-events: none;
}

.goal:hover::before,
.goal-active::before {
  opacity: 1;
}

.goal-body {
  position: relative;
  z-index: 1;
}

.goal-num {
  position: absolute;
  bottom: -0.75rem;
  right: -0.25rem;
  font-size: 4rem;
  font-weight: 900;
  line-height: 1;
  color: rgb(203 213 225 / 0.45);
  pointer-events: none;
  user-select: none;
}

.goal-icon {
  display: flex;
  height: 3rem;
  width: 3rem;
  flex: none;
  align-items: center;
  justify-content: center;
  border-radius: 1rem;
  border: 1px solid rgb(186 230 253 / 0.8);
  background: rgb(240 249 255);
  color: rgb(2 132 199);
  transition: background-color 0.3s, color 0.3s, transform 0.3s;
}

.goal:hover .goal-icon,
.goal-active .goal-icon {
  background: rgb(14 165 233);
  color: #fff;
  transform: scale(1.08);
}

.goal-chip {
  border-radius: 9999px;
  border: 1px solid rgb(186 230 253 / 0.7);
  background: rgb(240 249 255);
  padding: 0.125rem 0.5rem;
  font-size: 9px;
  font-weight: 700;
  color: rgb(3 105 161);
}

.goal-arrow,
.goal-go {
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgb(226 232 240);
  background: rgb(255 255 255 / 0.85);
  color: rgb(148 163 184);
  transition: background-color 0.3s, color 0.3s, border-color 0.3s, transform 0.3s;
}

.goal-arrow {
  height: 2rem;
  width: 2rem;
  border-radius: 9999px;
}

.goal-go {
  height: 2.25rem;
  width: 2.25rem;
  border-radius: 0.75rem;
  color: rgb(100 116 139);
}

.goal:hover .goal-arrow {
  background: rgb(240 249 255);
  color: rgb(2 132 199);
  border-color: rgb(186 230 253);
}

.goal:hover .goal-go,
.goal-active .goal-go {
  background: rgb(14 165 233);
  border-color: rgb(14 165 233);
  color: #fff;
  transform: translateX(4px);
}

.goal-profile {
  border-radius: 1rem;
  border: 1px solid rgb(255 255 255 / 0.9);
  background: rgb(248 250 252 / 0.8);
  padding: 1rem;
  box-shadow: inset 0 1px 3px rgb(15 23 42 / 0.05);
}

.goal-metric {
  border-radius: 0.75rem;
  border: 1px solid rgb(241 245 249);
  background: rgb(255 255 255 / 0.9);
  padding: 0.625rem;
  text-align: center;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

.no-scrollbar::-webkit-scrollbar {
  display: none;
}

.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

/* hides an element on phones (needs !important: component classes set display) */
.hide-xs {
  display: none !important;
}

@media (min-width: 640px) {
  .hide-xs {
    display: inline-flex !important;
  }
}

@media (max-width: 639px) {
  .goal {
    min-height: 0;
    padding: 1.25rem;
  }

  .goal-num {
    font-size: 3rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  * {
    scroll-behavior: auto !important;
    transition-duration: 0.01ms !important;
    animation: none !important;
  }
}
</style>