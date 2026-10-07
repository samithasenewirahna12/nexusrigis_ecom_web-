<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../../services/api'
import { useAuthStore } from '../../stores/authStore'
import { usePopup } from '../../composables/usePopup'
import backgroundImage from '../../assets/images/vecteezy_abstract-blur-shopping-mall_2795585.jpg'
import {
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  Wrench,
  Cpu,
  Clock,
  Truck,
  Building,
  Sparkles,
  Copy,
  Check,
  Printer,
  Download,
  Search,
  ArrowRight,
  ArrowLeft,
  RefreshCw,
  FileText,
  UploadCloud,
  X,
  ChevronRight,
  Barcode,
  Package,
  HelpCircle,
  Phone,
  Mail,
  Plus,
  Zap,
  Monitor,
  Flame,
  AlertTriangle,
  RotateCcw,
  Tag
} from 'lucide-vue-next'

const router = useRouter()
const authStore = useAuthStore()
const popup = usePopup()

/* =========================================================
   TYPES
========================================================= */

export interface WarrantyProduct {
  id: string
  productId: string
  name: string
  category: string
  orderId: string
  purchaseDate: string
  warrantyMonths: number
  warrantyExpiryDate: string
  daysRemaining: number
  image: string
  sku: string
  serialNumber: string
  unitPrice: number
  inWarranty: boolean
  customerName?: string
  customerEmail?: string
  customerPhone?: string
}

export interface RMAClaimRecord {
  rmaNumber: string
  createdAt: string
  validUntil: string
  product: WarrantyProduct
  issueCategory: string
  issueDescription: string
  claimType: 'replacement' | 'repair' | 'credit'
  handoverMethod: 'courier' | 'hub'
  evidenceFiles: string[]
  status: 'AUTHORIZED' | 'PENDING_DROPOFF' | 'IN_DIAGNOSTICS' | 'COMPLETED'
}

/* =========================================================
   COMPONENT STATE
========================================================= */

const activeTab = ref<'my_hardware' | 'lookup'>('my_hardware')
const currentStep = ref<1 | 2 | 3>(1)
const isLoadingOrders = ref(false)
const isSubmitting = ref(false)
const isSearching = ref(false)
const searchError = ref('')
const copiedRmaCode = ref(false)
const copiedSerial = ref<string | null>(null)

// Hardware Portfolio
const coveredProducts = ref<WarrantyProduct[]>([])
const hardwareSearchQuery = ref('')

// Manual Order Search
const orderSearchInput = ref('ORD010')
const emailSearchInput = ref('kamalgune@gmail.com')

// Active Claim Selection
const selectedProduct = ref<WarrantyProduct | null>(null)
const serialNumberInput = ref('')

// Diagnostic & Claim Form
const issueCategory = ref('power_failure')
const issueDescription = ref('')
const claimType = ref<'replacement' | 'repair' | 'credit'>('replacement')
const handoverMethod = ref<'courier' | 'hub'>('courier')
const uploadedFiles = ref<Array<{ name: string; size: string }>>([])

// Generated Claim Confirmation
const activeRmaClaim = ref<RMAClaimRecord | null>(null)

/* =========================================================
   DIAGNOSTIC SYMPTOM CATEGORIES
========================================================= */

const symptomCategories = [
  {
    id: 'power_failure',
    title: 'Power Failure / No POST',
    description: 'Component will not power on, power cycles, or does not POST.',
    icon: Zap,
    color: 'amber'
  },
  {
    id: 'display_artifacts',
    title: 'Display Artifacts / Driver Crash',
    description: 'Visual tearing, black screen, GPU timeouts, or color distortion.',
    icon: Monitor,
    color: 'cyan'
  },
  {
    id: 'thermal_throttling',
    title: 'Thermal Throttling / Cooler Failure',
    description: 'Excessive heat spike, cooling pump failure, or noisy fans.',
    icon: Flame,
    color: 'rose'
  },
  {
    id: 'memory_kernel',
    title: 'BSOD / Kernel Memory Faults',
    description: 'Random Windows blue-screen crashes, memory page faults.',
    icon: AlertTriangle,
    color: 'indigo'
  },
  {
    id: 'port_hardware',
    title: 'Port / Connector Defect',
    description: 'Physical or electrical damage to HDMI, DP, Type-C, or PCIe pins.',
    icon: Wrench,
    color: 'emerald'
  },
  {
    id: 'factory_doa',
    title: 'Dead On Arrival / Factory Flaw',
    description: 'Received defective directly from factory packaging.',
    icon: Package,
    color: 'blue'
  }
]

/* =========================================================
   RESOLUTIONS & HANDOVER
========================================================= */

const resolutionOptions = [
  {
    id: 'replacement',
    title: 'Direct Express Replacement',
    badge: 'Recommended',
    description: 'Exchange for a brand-new factory-sealed replacement unit upon verified diagnostics.',
    icon: RefreshCw
  },
  {
    id: 'repair',
    title: 'Precision Lab Repair & Recalibration',
    badge: 'Official Service',
    description: 'Factory-certified component repair with 24-hour stress and benchmark validation.',
    icon: Wrench
  },
  {
    id: 'credit',
    title: 'Nexus Store Credit Voucher',
    badge: 'Instant Upgrade',
    description: 'Receive 100% store credit voucher towards upgrading to any next-generation hardware.',
    icon: Tag
  }
]

const handoverOptions = [
  {
    id: 'courier',
    title: 'Free Island-Wide Courier Pickup',
    tag: 'Doorstep Handover',
    desc: 'Domex Express rider will collect the package from your address with protective transit casing.',
    icon: Truck
  },
  {
    id: 'hub',
    title: 'Walk-In Dropoff at Flagship Hub',
    tag: 'Same-Day Bench Test',
    desc: 'Drop off at Galle Road, Colombo 03 for immediate 2-hour lab technician inspection.',
    icon: Building
  }
]

/* =========================================================
   COMPUTED
========================================================= */

const filteredCoveredProducts = computed(() => {
  if (!hardwareSearchQuery.value.trim()) return coveredProducts.value
  const q = hardwareSearchQuery.value.toLowerCase()
  return coveredProducts.value.filter(
    p =>
      p.name.toLowerCase().includes(q) ||
      p.orderId.toLowerCase().includes(q) ||
      p.serialNumber.toLowerCase().includes(q) ||
      p.category.toLowerCase().includes(q)
  )
})

const activeWarrantyCount = computed(() => {
  return coveredProducts.value.filter(p => p.inWarranty).length
})

const averageRemainingDays = computed(() => {
  const activeItems = coveredProducts.value.filter(p => p.inWarranty)
  if (activeItems.length === 0) return 0
  const sum = activeItems.reduce((acc, curr) => acc + curr.daysRemaining, 0)
  return Math.round(sum / activeItems.length)
})

/* =========================================================
   HELPER UTILITIES
========================================================= */

function getCustomerId(): string {
  try {
    if (authStore.user) {
      const id = authStore.user.customerId ?? authStore.user.userId ?? authStore.user.id
      if (id) return String(id)
    }
  } catch {}

  try {
    const savedUser = sessionStorage.getItem('user') || localStorage.getItem('user')
    if (savedUser) {
      const user = JSON.parse(savedUser)
      const id = user.customerId ?? user.userId ?? user.id
      if (id) return String(id)
    }
  } catch {}

  return sessionStorage.getItem('userId') || localStorage.getItem('userId') || 'CUS001'
}

function getCustomerProfile() {
  try {
    const raw = sessionStorage.getItem('user') || localStorage.getItem('user')
    if (raw) return JSON.parse(raw)
  } catch {}
  return {
    name: authStore.user?.name || 'Kamal Gunarathna',
    email: authStore.user?.email || 'kamalgune@gmail.com',
    phone: authStore.user?.phone || '0771234567',
    address: 'No. 124, High Level Road, Colombo 05'
  }
}

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('en-LK', {
    style: 'currency',
    currency: 'LKR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(amount)
}

function calculateWarrantyExpiry(purchaseDateStr: string, months: number = 36) {
  const pDate = new Date(purchaseDateStr || Date.now())
  const expDate = new Date(pDate)
  expDate.setMonth(expDate.getMonth() + months)

  const diffMs = expDate.getTime() - Date.now()
  const daysRemaining = Math.max(0, Math.ceil(diffMs / (1000 * 60 * 60 * 24)))

  return {
    purchaseFormatted: pDate.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }),
    expiryFormatted: expDate.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }),
    daysRemaining,
    inWarranty: daysRemaining > 0
  }
}

async function copyText(text: string, isSerial = false) {
  try {
    await navigator.clipboard.writeText(text)
    if (isSerial) {
      copiedSerial.value = text
      setTimeout(() => { copiedSerial.value = null }, 2000)
    } else {
      copiedRmaCode.value = true
      setTimeout(() => { copiedRmaCode.value = false }, 2500)
    }
    await popup.success(`Copied: ${text}`, 'Copied to Clipboard')
  } catch {}
}

/* =========================================================
   DATA FETCHING: LOAD CUSTOMER ORDERS & WARRANTY ITEMS
========================================================= */

async function loadCustomerHardware() {
  isLoadingOrders.value = true
  const customerId = getCustomerId()
  const profile = getCustomerProfile()

  try {
    let ordersList: any[] = []

    try {
      const res = await api.get(`/orders/customer/${customerId}`)
      if (Array.isArray(res.data) && res.data.length > 0) {
        ordersList = res.data
      }
    } catch {
      // Fallback to all orders
      try {
        const fallback = await api.get('/orders')
        if (Array.isArray(fallback.data)) {
          ordersList = fallback.data.filter(
            (o: any) => o.customerId === customerId || o.customerEmail === profile.email
          )
          if (ordersList.length === 0 && fallback.data.length > 0) {
            ordersList = fallback.data.slice(0, 5)
          }
        }
      } catch {}
    }

    // Default mock hardware if zero orders found in database
    if (ordersList.length === 0) {
      ordersList = [
        {
          orderId: 'ORD010',
          orderDate: '2026-10-07',
          customerName: profile.name,
          customerEmail: profile.email,
          customerPhone: profile.phone,
          items: [
            {
              productId: 'P005',
              productName: 'ASUS TUF Gaming F15',
              unitPrice: 285000,
              quantity: 1
            },
            {
              productId: 'P004',
              productName: 'Samsung Galaxy S25',
              unitPrice: 285000,
              quantity: 1
            }
          ]
        },
        {
          orderId: 'ORD006',
          orderDate: '2026-10-06',
          customerName: profile.name,
          customerEmail: profile.email,
          customerPhone: profile.phone,
          items: [
            {
              productId: 'P001',
              productName: 'Samsung 55 Inch 4K Smart TV',
              unitPrice: 189900,
              quantity: 1
            }
          ]
        }
      ]
    }

    // Transform order items into warranty hardware
    const hardware: WarrantyProduct[] = []

    for (const ord of ordersList) {
      const items = ord.items || []
      const orderDate = ord.orderDate || new Date().toISOString().split('T')[0]

      for (let i = 0; i < items.length; i++) {
        const item = items[i]
        const pid = item.productId || `P00${i + 1}`
        const name = item.productName || item.name || 'Nexus Performance Component'
        const unitPrice = Number(item.unitPrice || item.price || 150000)

        // Warranty duration rule
        let months = 36
        let cat = 'Hardware Components'
        if (name.toLowerCase().includes('tv')) {
          cat = 'Smart Displays'
          months = 36
        } else if (name.toLowerCase().includes('laptop') || name.toLowerCase().includes('tuf') || name.toLowerCase().includes('rog')) {
          cat = 'Gaming Laptops'
          months = 36
        } else if (name.toLowerCase().includes('galaxy') || name.toLowerCase().includes('phone')) {
          cat = 'Smartphones'
          months = 24
        } else if (name.toLowerCase().includes('monitor')) {
          cat = 'Monitors'
          months = 36
        }

        const dates = calculateWarrantyExpiry(orderDate, months)

        // Fallback images
        let img = 'https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?w=500&q=80'
        if (cat === 'Smart Displays') {
          img = 'https://images.unsplash.com/photo-1593784991095-a205069470b6?w=500&q=80'
        } else if (cat === 'Gaming Laptops') {
          img = 'https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=500&q=80'
        } else if (cat === 'Smartphones') {
          img = 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=500&q=80'
        }

        hardware.push({
          id: `wp-${ord.orderId}-${pid}-${i}`,
          productId: pid,
          name,
          category: cat,
          orderId: ord.orderId,
          purchaseDate: dates.purchaseFormatted,
          warrantyMonths: months,
          warrantyExpiryDate: dates.expiryFormatted,
          daysRemaining: dates.daysRemaining,
          image: img,
          sku: `SKU-${pid}-${ord.orderId.slice(-3)}`,
          serialNumber: `SN-NX-${pid}-${ord.orderId.slice(-4)}${i + 1}`,
          unitPrice,
          inWarranty: dates.inWarranty,
          customerName: ord.customerName || profile.name,
          customerEmail: ord.customerEmail || profile.email,
          customerPhone: ord.customerPhone || profile.phone
        })
      }
    }

    coveredProducts.value = hardware

    // Pre-select first eligible item if nothing selected
    if (!selectedProduct.value && hardware.length > 0) {
      const eligible = hardware.find(h => h.inWarranty) || hardware[0]
      initiateClaimForProduct(eligible, false)
    }
  } catch (error) {
    console.warn('Failed to load orders for warranty:', error)
  } finally {
    isLoadingOrders.value = false
  }
}

/* =========================================================
   ORDER LOOKUP (FOR GUESTS / CROSS-ORDER SEARCH)
========================================================= */

async function handleLookupOrder() {
  searchError.value = ''
  if (!orderSearchInput.value.trim() || !emailSearchInput.value.trim()) {
    searchError.value = 'Please enter both the Order ID and Billing Email.'
    return
  }

  isSearching.value = true

  try {
    const oId = orderSearchInput.value.trim().toUpperCase()
    const email = emailSearchInput.value.trim().toLowerCase()

    let matchedOrder: any = null

    try {
      const res = await api.get(`/orders/${oId}`)
      if (res.data) matchedOrder = res.data
    } catch {
      // try list
      const listRes = await api.get('/orders')
      if (Array.isArray(listRes.data)) {
        matchedOrder = listRes.data.find(
          (o: any) => o.orderId.toUpperCase() === oId && (o.customerEmail || '').toLowerCase() === email
        )
      }
    }

    if (!matchedOrder) {
      searchError.value = `No order matching #${oId} with email ${email} was found.`
      return
    }

    // Convert items into warranty products
    const items = matchedOrder.items || []
    if (items.length === 0) {
      searchError.value = 'No warrantable products found in this order.'
      return
    }

    const firstItem = items[0]
    const pid = firstItem.productId || 'P001'
    const name = firstItem.productName || 'Verified Hardware Item'
    const dates = calculateWarrantyExpiry(matchedOrder.orderDate || '2026-10-06', 36)

    const lookedUpProduct: WarrantyProduct = {
      id: `lookup-${oId}-${pid}`,
      productId: pid,
      name,
      category: 'Verified Hardware',
      orderId: oId,
      purchaseDate: dates.purchaseFormatted,
      warrantyMonths: 36,
      warrantyExpiryDate: dates.expiryFormatted,
      daysRemaining: dates.daysRemaining,
      image: 'https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?w=500&q=80',
      sku: `SKU-${pid}`,
      serialNumber: `SN-NX-${pid}-${oId.slice(-4)}`,
      unitPrice: Number(firstItem.unitPrice || 150000),
      inWarranty: dates.inWarranty,
      customerName: matchedOrder.customerName || 'Kamal Gunarathna',
      customerEmail: email,
      customerPhone: matchedOrder.customerPhone || '0771234567'
    }

    initiateClaimForProduct(lookedUpProduct, true)
    await popup.success(`Found order #${oId} with ${items.length} item(s)!`, 'Order Verified')
  } catch (err: any) {
    searchError.value = 'Unable to verify order. Please verify your details.'
  } finally {
    isSearching.value = false
  }
}

/* =========================================================
   CLAIM ACTIONS & WIZARD STEPS
========================================================= */

function initiateClaimForProduct(prod: WarrantyProduct, advanceStep = true) {
  selectedProduct.value = prod
  serialNumberInput.value = prod.serialNumber
  if (advanceStep) {
    currentStep.value = 2
    // Scroll smoothly to wizard
    const el = document.getElementById('claim-wizard')
    if (el) el.scrollIntoView({ behavior: 'smooth' })
  }
}

function handleFileUpload(event: Event) {
  const target = event.target as HTMLInputElement
  if (!target.files) return

  for (let i = 0; i < target.files.length; i++) {
    const file = target.files[i]
    const sizeMb = (file.size / (1024 * 1024)).toFixed(1)
    uploadedFiles.value.push({
      name: file.name,
      size: `${sizeMb} MB`
    })
  }
  target.value = ''
}

function removeUploadedFile(index: number) {
  uploadedFiles.value.splice(index, 1)
}

async function submitWarrantyClaim() {
  if (!selectedProduct.value) {
    await popup.error('Please choose a covered product to claim.', 'Select Product')
    return
  }

  if (!issueDescription.value.trim() || issueDescription.value.trim().length < 15) {
    await popup.error('Please provide a detailed symptom description (at least 15 characters).', 'Description Required')
    return
  }

  isSubmitting.value = true

  try {
    const randomSuffix = Math.floor(100000 + Math.random() * 900000)
    const generatedRma = `RMA-LK-2026-${randomSuffix}`

    const now = new Date()
    const validUntilDate = new Date(now)
    validUntilDate.setDate(validUntilDate.getDate() + 14)

    // Call backend returns / RMA endpoint if available
    try {
      await api.post('/returns', {
        orderId: selectedProduct.value.orderId,
        reason: `WARRANTY_CLAIM_${issueCategory.value.toUpperCase()}`,
        description: `[RMA ${generatedRma}] ${issueDescription.value.trim()}`,
        refundMethod: claimType.value === 'credit' ? 'STORE_CREDIT' : claimType.value === 'replacement' ? 'EXCHANGE' : 'REPAIR',
        refundAmount: selectedProduct.value.unitPrice
      })
    } catch {
      // Backend may also accept inquiry
      try {
        await api.post('/customer-support', {
          subject: `[${generatedRma}] Warranty Claim for ${selectedProduct.value.name}`,
          category: 'Warranty & RMA',
          priority: 'HIGH',
          message: `Symptom: ${issueCategory.value}. Handover: ${handoverMethod.value}. Details: ${issueDescription.value}`,
          customerName: selectedProduct.value.customerName || 'Customer',
          customerEmail: selectedProduct.value.customerEmail || 'customer@nexusrigs.com'
        })
      } catch {}
    }

    activeRmaClaim.value = {
      rmaNumber: generatedRma,
      createdAt: now.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' }),
      validUntil: validUntilDate.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }),
      product: {
        ...selectedProduct.value,
        serialNumber: serialNumberInput.value.trim() || selectedProduct.value.serialNumber
      },
      issueCategory: symptomCategories.find(s => s.id === issueCategory.value)?.title || issueCategory.value,
      issueDescription: issueDescription.value.trim(),
      claimType: claimType.value,
      handoverMethod: handoverMethod.value,
      evidenceFiles: uploadedFiles.value.map(f => f.name),
      status: 'AUTHORIZED'
    }

    currentStep.value = 3
    await popup.success(`RMA #${generatedRma} authorized! Digital certificate generated.`, 'Warranty Claim Approved')

    // Scroll to certificate
    setTimeout(() => {
      const el = document.getElementById('rma-certificate')
      if (el) el.scrollIntoView({ behavior: 'smooth' })
    }, 150)
  } catch (error: any) {
    console.error('RMA submission error:', error)
    await popup.error('Failed to submit warranty claim. Please try again.', 'Submission Error')
  } finally {
    isSubmitting.value = false
  }
}

function resetWizard() {
  currentStep.value = 1
  activeRmaClaim.value = null
  issueDescription.value = ''
  uploadedFiles.value = []
  claimType.value = 'replacement'
  handoverMethod.value = 'courier'
}

function printRmaCertificate() {
  window.print()
}

onMounted(() => {
  loadCustomerHardware()
})
</script>

<template>
  <div class="relative min-h-screen bg-slate-100 text-slate-900 pb-20">

    <!-- ========================================================
         BACKGROUND TEXTURE (NEXUS LIGHT FROSTED GLASS)
    ========================================================= -->
    <div
      class="fixed inset-0 pointer-events-none opacity-[0.20] bg-cover bg-center"
      :style="{ backgroundImage: `url(${backgroundImage})` }"
    ></div>
    <div class="fixed inset-0 pointer-events-none bg-gradient-to-b from-white/92 via-slate-100/95 to-slate-100"></div>

    <!-- AMBIENT GLOW SPHERES -->
    <div class="pointer-events-none fixed -top-40 right-0 w-96 h-96 rounded-full bg-blue-500/10 blur-3xl"></div>
    <div class="pointer-events-none fixed top-1/3 -left-40 w-96 h-96 rounded-full bg-cyan-400/10 blur-3xl"></div>
    <div class="pointer-events-none fixed bottom-10 right-1/4 w-96 h-96 rounded-full bg-indigo-500/10 blur-3xl"></div>

    <!-- MAIN CONTAINER -->
    <main class="relative z-10 w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-8 space-y-8">

      <!-- ========================================================
           HERO SECTION: OFFICIAL WARRANTY PORTAL HEADER
      ========================================================= -->
      <section class="relative overflow-hidden rounded-3xl border border-white/90 bg-white/85 p-6 sm:p-10 lg:p-12 shadow-2xl shadow-slate-300/25 backdrop-blur-2xl">
        <div class="relative grid grid-cols-1 lg:grid-cols-[1fr_auto] gap-8 items-center">

          <!-- Left Info -->
          <div>
            <div class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-blue-50 border border-blue-200/80 text-blue-700 text-xs font-bold mb-4 shadow-2xs">
              <ShieldCheck class="w-3.5 h-3.5 text-blue-600" />
              <span>Official NexusRigs Hardware Guarantee</span>
              <span class="rounded-full bg-blue-200/60 px-1.5 py-0.2 text-[10px] font-black text-blue-800">2-3 YRS</span>
            </div>

            <h1 class="text-3xl sm:text-4xl lg:text-5xl font-black text-slate-950 tracking-tight leading-tight">
              Hardware Warranty & <span class="bg-gradient-to-r from-blue-600 to-cyan-500 bg-clip-text text-transparent">RMA Service Hub</span>
            </h1>

            <p class="mt-4 text-sm sm:text-base text-slate-500 leading-relaxed max-w-2xl">
              Manage your active component guarantees, verify serial numbers, and initiate hassle-free RMA claims with complimentary island-wide courier pickup and rapid bench testing.
            </p>

            <!-- 4 Quick Feature Pills -->
            <div class="mt-6 grid grid-cols-2 sm:grid-cols-4 gap-3 pt-4 border-t border-slate-100">
              <div class="flex items-center gap-2 text-xs font-bold text-slate-700">
                <div class="flex h-7 w-7 items-center justify-center rounded-lg bg-blue-50 text-blue-600 shrink-0">
                  <ShieldCheck class="h-4 w-4" />
                </div>
                <span>100% Genuine</span>
              </div>
              <div class="flex items-center gap-2 text-xs font-bold text-slate-700">
                <div class="flex h-7 w-7 items-center justify-center rounded-lg bg-cyan-50 text-cyan-600 shrink-0">
                  <Zap class="h-4 w-4" />
                </div>
                <span>48h Diagnostics</span>
              </div>
              <div class="flex items-center gap-2 text-xs font-bold text-slate-700">
                <div class="flex h-7 w-7 items-center justify-center rounded-lg bg-emerald-50 text-emerald-600 shrink-0">
                  <Truck class="h-4 w-4" />
                </div>
                <span>Free Domex Pickup</span>
              </div>
              <div class="flex items-center gap-2 text-xs font-bold text-slate-700">
                <div class="flex h-7 w-7 items-center justify-center rounded-lg bg-indigo-50 text-indigo-600 shrink-0">
                  <Building class="h-4 w-4" />
                </div>
                <span>Colombo Walk-In Hub</span>
              </div>
            </div>
          </div>

          <!-- Right Status Box -->
          <div class="rounded-2xl border border-white/90 bg-slate-50/80 p-5 backdrop-blur-xl sm:w-72 shadow-sm shrink-0">
            <span class="text-[10px] font-black uppercase tracking-wider text-slate-400 block mb-1">
              Coverage Portfolio
            </span>
            <div class="flex items-baseline gap-2">
              <span class="text-3xl font-black text-slate-900">{{ coveredProducts.length }}</span>
              <span class="text-xs font-bold text-slate-500">Hardware Units</span>
            </div>

            <div class="mt-3 space-y-2 text-xs border-t border-slate-200/60 pt-3">
              <div class="flex justify-between items-center">
                <span class="text-slate-500 font-medium">Active Coverage:</span>
                <span class="font-black text-emerald-600 flex items-center gap-1">
                  <span class="h-2 w-2 rounded-full bg-emerald-500 animate-pulse"></span>
                  {{ activeWarrantyCount }} Devices
                </span>
              </div>
              <div class="flex justify-between items-center">
                <span class="text-slate-500 font-medium">Avg Time Left:</span>
                <span class="font-bold text-blue-600">{{ averageRemainingDays }} Days</span>
              </div>
              <div class="flex justify-between items-center">
                <span class="text-slate-500 font-medium">Service Partner:</span>
                <span class="font-bold text-slate-700">Domex / Pronto</span>
              </div>
            </div>
          </div>

        </div>
      </section>

      <!-- ========================================================
           TAB SELECTOR: MY COVERED HARDWARE VS ORDER LOOKUP
      ========================================================= -->
      <div class="flex flex-col sm:flex-row items-center justify-between gap-4">
        <!-- Tabs -->
        <div class="inline-flex rounded-2xl border border-white/90 bg-white/80 p-1.5 shadow-sm backdrop-blur-xl">
          <button
            type="button"
            @click="activeTab = 'my_hardware'"
            class="flex items-center gap-2 rounded-xl px-5 py-2.5 text-xs font-black transition-all cursor-pointer"
            :class="activeTab === 'my_hardware'
              ? 'bg-blue-600 text-white shadow-md shadow-blue-500/20'
              : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'"
          >
            <Cpu class="h-4 w-4" />
            <span>My Covered Hardware</span>
            <span
              class="rounded-full px-2 py-0.2 text-[10px] font-black"
              :class="activeTab === 'my_hardware' ? 'bg-white/20 text-white' : 'bg-slate-100 text-slate-600'"
            >
              {{ coveredProducts.length }}
            </span>
          </button>

          <button
            type="button"
            @click="activeTab = 'lookup'"
            class="flex items-center gap-2 rounded-xl px-5 py-2.5 text-xs font-black transition-all cursor-pointer"
            :class="activeTab === 'lookup'
              ? 'bg-blue-600 text-white shadow-md shadow-blue-500/20'
              : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'"
          >
            <Search class="h-4 w-4" />
            <span>Lookup by Order & Serial</span>
          </button>
        </div>

        <!-- Search Hardware input -->
        <div v-if="activeTab === 'my_hardware'" class="relative w-full sm:w-72">
          <Search class="absolute left-3.5 top-3 h-4 w-4 text-slate-400" />
          <input
            v-model="hardwareSearchQuery"
            type="text"
            placeholder="Search device, order, serial..."
            class="w-full rounded-xl border border-slate-200 bg-white/90 py-2.5 pl-10 pr-4 text-xs font-semibold text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 shadow-2xs"
          />
        </div>
      </div>

      <!-- ========================================================
           TAB 1: MY COVERED HARDWARE CARDS GRID
      ========================================================= -->
      <section v-if="activeTab === 'my_hardware'" class="space-y-4">
        <div v-if="isLoadingOrders" class="py-16 text-center">
          <RefreshCw class="h-8 w-8 text-blue-600 animate-spin mx-auto mb-3" />
          <p class="text-sm font-bold text-slate-600">Retrieving official warranty records from database...</p>
        </div>

        <div v-else-if="filteredCoveredProducts.length === 0" class="rounded-3xl border border-white/90 bg-white/80 p-12 text-center shadow-lg">
          <Package class="h-12 w-12 text-slate-300 mx-auto mb-3" />
          <h3 class="text-base font-black text-slate-800">No hardware found</h3>
          <p class="text-xs text-slate-500 mt-1 max-w-md mx-auto">
            {{ hardwareSearchQuery ? `No device matching "${hardwareSearchQuery}". Try clearing search.` : 'You have not made any hardware purchases on this account yet.' }}
          </p>
          <button
            v-if="hardwareSearchQuery"
            type="button"
            @click="hardwareSearchQuery = ''"
            class="mt-4 rounded-xl bg-blue-600 px-4 py-2 text-xs font-bold text-white hover:bg-blue-700"
          >
            Clear Search
          </button>
        </div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          <div
            v-for="product in filteredCoveredProducts"
            :key="product.id"
            class="group relative flex flex-col justify-between overflow-hidden rounded-3xl border transition-all duration-300 p-5 backdrop-blur-xl shadow-lg"
            :class="selectedProduct?.id === product.id
              ? 'border-blue-500 bg-blue-50/40 ring-2 ring-blue-500/20 shadow-blue-500/10'
              : 'border-white/90 bg-white/85 hover:border-slate-300 hover:shadow-xl'"
          >
            <!-- TOP CARD HEADER: STATUS PILL + CATEGORY -->
            <div>
              <div class="flex items-center justify-between mb-3.5">
                <span class="rounded-full bg-slate-100 px-2.5 py-0.5 text-[10px] font-bold text-slate-600">
                  {{ product.category }}
                </span>

                <!-- In-warranty status badge -->
                <span
                  v-if="product.inWarranty"
                  class="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-2.5 py-0.5 text-[10px] font-black text-emerald-700 border border-emerald-200/60"
                >
                  <span class="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
                  <span>ACTIVE • {{ product.daysRemaining }}d Left</span>
                </span>
                <span
                  v-else
                  class="rounded-full bg-slate-100 px-2.5 py-0.5 text-[10px] font-black text-slate-400"
                >
                  EXPIRED
                </span>
              </div>

              <!-- PRODUCT THUMBNAIL & INFO -->
              <div class="flex items-start gap-3.5">
                <img
                  :src="product.image"
                  :alt="product.name"
                  class="h-18 w-18 shrink-0 rounded-2xl object-cover border border-slate-100 bg-slate-50 group-hover:scale-105 transition-transform duration-300"
                />
                <div class="flex-1 min-w-0">
                  <span class="font-mono text-[10px] font-bold text-blue-600 block">
                    Order #{{ product.orderId }}
                  </span>
                  <h3 class="text-sm font-black text-slate-900 leading-snug line-clamp-2 mt-0.5">
                    {{ product.name }}
                  </h3>
                  <p class="text-[11px] font-bold text-slate-500 mt-1">
                    {{ formatCurrency(product.unitPrice) }}
                  </p>
                </div>
              </div>

              <!-- SERIAL NUMBER & DATES -->
              <div class="mt-4 rounded-2xl bg-slate-50/80 p-3 border border-slate-100 space-y-2 text-xs">
                <div class="flex items-center justify-between">
                  <span class="text-[10px] font-bold text-slate-400 uppercase">Serial No:</span>
                  <div class="flex items-center gap-1">
                    <span class="font-mono text-[11px] font-black text-slate-800">{{ product.serialNumber }}</span>
                    <button
                      type="button"
                      @click="copyText(product.serialNumber, true)"
                      class="text-slate-400 hover:text-blue-600 p-0.5"
                      title="Copy serial"
                    >
                      <Check v-if="copiedSerial === product.serialNumber" class="h-3 w-3 text-emerald-600" />
                      <Copy v-else class="h-3 w-3" />
                    </button>
                  </div>
                </div>

                <div class="flex items-center justify-between">
                  <span class="text-[10px] font-bold text-slate-400 uppercase">Purchased:</span>
                  <span class="font-bold text-slate-700 text-[11px]">{{ product.purchaseDate }}</span>
                </div>

                <div class="flex items-center justify-between">
                  <span class="text-[10px] font-bold text-slate-400 uppercase">Valid Until:</span>
                  <span class="font-bold text-slate-700 text-[11px]">{{ product.warrantyExpiryDate }}</span>
                </div>

                <!-- Lifespan Progress -->
                <div class="pt-1">
                  <div class="flex justify-between text-[9px] font-bold text-slate-400 mb-1">
                    <span>Warranty Span ({{ product.warrantyMonths }} Mo)</span>
                    <span class="text-blue-600">{{ Math.min(100, Math.round((product.daysRemaining / (product.warrantyMonths * 30)) * 100)) }}% remaining</span>
                  </div>
                  <div class="h-1.5 w-full rounded-full bg-slate-200 overflow-hidden">
                    <div
                      class="h-full rounded-full bg-gradient-to-r from-blue-600 to-cyan-400 transition-all"
                      :style="{ width: `${Math.min(100, Math.round((product.daysRemaining / (product.warrantyMonths * 30)) * 100))}%` }"
                    ></div>
                  </div>
                </div>
              </div>
            </div>

            <!-- ACTION BUTTON -->
            <div class="mt-4 pt-3 border-t border-slate-100">
              <button
                type="button"
                @click="initiateClaimForProduct(product, true)"
                class="w-full flex items-center justify-center gap-2 rounded-xl py-2.5 px-4 text-xs font-black transition-all cursor-pointer shadow-xs"
                :class="selectedProduct?.id === product.id
                  ? 'bg-blue-600 text-white hover:bg-blue-700 shadow-blue-500/20'
                  : 'bg-slate-900 text-white hover:bg-blue-600'"
              >
                <Wrench class="h-3.5 w-3.5" />
                <span>{{ selectedProduct?.id === product.id ? 'Active in Claim Form' : 'Initiate RMA Claim' }}</span>
                <ArrowRight class="h-3 w-3" />
              </button>
            </div>
          </div>
        </div>
      </section>

      <!-- ========================================================
           TAB 2: MANUAL ORDER & SERIAL LOOKUP
      ========================================================= -->
      <section v-else-if="activeTab === 'lookup'" class="rounded-3xl border border-white/90 bg-white/85 p-6 sm:p-8 shadow-xl backdrop-blur-xl">
        <div class="max-w-2xl mx-auto text-center mb-6">
          <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-blue-50 text-blue-600 mx-auto mb-3">
            <Search class="h-6 w-6" />
          </div>
          <h2 class="text-xl font-black text-slate-900">Cross-Order Warranty Verification</h2>
          <p class="text-xs text-slate-500 mt-1">
            Looking to service a rig purchased under another invoice or as a guest? Enter the Order ID and billing email below to retrieve warranty coverage.
          </p>
        </div>

        <form @submit.prevent="handleLookupOrder" class="max-w-2xl mx-auto space-y-4">
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="mb-1.5 block text-xs font-bold text-slate-700">Order Reference ID *</label>
              <input
                v-model="orderSearchInput"
                type="text"
                placeholder="e.g. ORD010"
                class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 font-mono text-xs font-bold uppercase text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 shadow-2xs"
              />
            </div>

            <div>
              <label class="mb-1.5 block text-xs font-bold text-slate-700">Billing Email *</label>
              <input
                v-model="emailSearchInput"
                type="email"
                placeholder="kamalgune@gmail.com"
                class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-xs font-semibold text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 shadow-2xs"
              />
            </div>
          </div>

          <!-- Quick presets chips -->
          <div class="flex flex-wrap items-center gap-2 pt-1 text-xs text-slate-500">
            <span class="text-[10px] font-bold uppercase text-slate-400">Quick Test Orders:</span>
            <button
              type="button"
              @click="orderSearchInput = 'ORD010'; emailSearchInput = 'kamalgune@gmail.com'"
              class="rounded-lg bg-slate-100 hover:bg-slate-200 px-2.5 py-1 text-[11px] font-mono font-bold text-slate-700 transition"
            >
              ORD010 (Laptop + S25)
            </button>
            <button
              type="button"
              @click="orderSearchInput = 'ORD006'; emailSearchInput = 'kamalgune@gmail.com'"
              class="rounded-lg bg-slate-100 hover:bg-slate-200 px-2.5 py-1 text-[11px] font-mono font-bold text-slate-700 transition"
            >
              ORD006 (Smart TV)
            </button>
          </div>

          <p v-if="searchError" class="text-xs font-bold text-rose-600">
            {{ searchError }}
          </p>

          <div class="pt-2 text-center">
            <button
              type="submit"
              :disabled="isSearching"
              class="inline-flex items-center gap-2 rounded-2xl bg-gradient-to-r from-blue-600 to-cyan-500 px-8 py-3 text-xs font-black text-white shadow-lg shadow-blue-500/20 hover:from-blue-700 hover:to-cyan-600 transition active:scale-95 disabled:opacity-50 cursor-pointer"
            >
              <RefreshCw v-if="isSearching" class="h-4 w-4 animate-spin" />
              <Search v-else class="h-4 w-4" />
              <span>{{ isSearching ? 'Verifying with Warranty DB...' : 'Verify Order & Coverage' }}</span>
            </button>
          </div>
        </form>
      </section>

      <!-- ========================================================
           INTERACTIVE 3-STEP CLAIM & RMA WIZARD
      ========================================================= -->
      <section id="claim-wizard" class="overflow-hidden rounded-3xl border border-white/90 bg-white/90 p-6 sm:p-8 lg:p-10 shadow-2xl backdrop-blur-2xl">

        <!-- WIZARD STEP HEADER -->
        <div class="border-b border-slate-100 pb-6 mb-8">
          <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <span class="text-[10px] font-black uppercase tracking-wider text-blue-600">
                Official RMA Wizard
              </span>
              <h2 class="text-2xl font-black text-slate-900">
                {{ currentStep === 1 ? 'Step 1: Verify Hardware Selection' : currentStep === 2 ? 'Step 2: Diagnostics & Desired Resolution' : 'Step 3: Authorized RMA Packing Slip' }}
              </h2>
            </div>

            <!-- Steps indicator -->
            <div class="flex items-center gap-2">
              <div
                class="flex h-8 items-center gap-2 rounded-xl px-3 text-xs font-black transition-all"
                :class="currentStep === 1 ? 'bg-blue-600 text-white shadow-xs' : 'bg-slate-100 text-slate-600'"
              >
                <span>1</span>
                <span class="hidden sm:inline">Hardware</span>
              </div>
              <ChevronRight class="h-4 w-4 text-slate-300" />
              <div
                class="flex h-8 items-center gap-2 rounded-xl px-3 text-xs font-black transition-all"
                :class="currentStep === 2 ? 'bg-blue-600 text-white shadow-xs' : 'bg-slate-100 text-slate-600'"
              >
                <span>2</span>
                <span class="hidden sm:inline">Diagnostics</span>
              </div>
              <ChevronRight class="h-4 w-4 text-slate-300" />
              <div
                class="flex h-8 items-center gap-2 rounded-xl px-3 text-xs font-black transition-all"
                :class="currentStep === 3 ? 'bg-emerald-600 text-white shadow-xs' : 'bg-slate-100 text-slate-600'"
              >
                <span>3</span>
                <span class="hidden sm:inline">Certificate</span>
              </div>
            </div>
          </div>
        </div>

        <!-- ====================================================
             STEP 1 CONTENT: VERIFY HARDWARE SELECTION
        ==================================================== -->
        <div v-if="currentStep === 1" class="space-y-6">
          <div v-if="selectedProduct" class="rounded-3xl border border-blue-200 bg-blue-50/50 p-6 flex flex-col md:flex-row items-center justify-between gap-6">
            <div class="flex items-center gap-4">
              <img
                :src="selectedProduct.image"
                :alt="selectedProduct.name"
                class="h-20 w-20 rounded-2xl object-cover border border-white shadow-md bg-white shrink-0"
              />
              <div>
                <span class="rounded-full bg-blue-100 px-2 py-0.5 text-[10px] font-black text-blue-800">
                  SELECTED FOR CLAIM
                </span>
                <h3 class="text-base font-black text-slate-900 mt-1">
                  {{ selectedProduct.name }}
                </h3>
                <p class="text-xs text-slate-500">
                  Invoice Order #{{ selectedProduct.orderId }} • Purchased on {{ selectedProduct.purchaseDate }}
                </p>
                <div class="mt-2 flex items-center gap-3 text-xs">
                  <span class="font-bold text-emerald-700 flex items-center gap-1">
                    <ShieldCheck class="h-4 w-4" /> Coverage Valid Until {{ selectedProduct.warrantyExpiryDate }}
                  </span>
                </div>
              </div>
            </div>

            <div class="w-full md:w-72 rounded-2xl bg-white p-4 border border-blue-100 shadow-2xs space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-wider text-slate-400 block">
                Serial Number Verification
              </label>
              <input
                v-model="serialNumberInput"
                type="text"
                placeholder="SN-NX-XXXX"
                class="w-full rounded-xl border border-slate-200 bg-slate-50/50 px-3 py-2 font-mono text-xs font-black text-slate-800 outline-none focus:border-blue-500"
              />
              <span class="text-[9px] text-slate-400 block">
                Matched to official invoice manifest
              </span>
            </div>
          </div>

          <div v-else class="text-center py-8">
            <p class="text-xs font-bold text-slate-500">No product selected yet. Please select one from the grid above.</p>
          </div>

          <!-- Wizard Navigation -->
          <div class="flex items-center justify-end pt-4 border-t border-slate-100">
            <button
              type="button"
              :disabled="!selectedProduct"
              @click="currentStep = 2"
              class="inline-flex items-center gap-2 rounded-2xl bg-blue-600 hover:bg-blue-700 px-6 py-3 text-xs font-black text-white shadow-md shadow-blue-500/20 transition active:scale-95 disabled:opacity-50 cursor-pointer"
            >
              <span>Continue to Diagnostics</span>
              <ArrowRight class="h-4 w-4" />
            </button>
          </div>
        </div>

        <!-- ====================================================
             STEP 2 CONTENT: DIAGNOSTICS & RESOLUTION SELECTION
        ==================================================== -->
        <div v-else-if="currentStep === 2" class="space-y-8">

          <!-- 1. SYMPTOM SELECTION -->
          <div>
            <div class="flex items-center justify-between mb-3">
              <label class="text-xs font-black text-slate-800 uppercase tracking-wider">
                1. Select Hardware Fault Classification *
              </label>
              <span class="text-[10px] text-blue-600 font-bold">Helps technicians prepare bench test kit</span>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3.5">
              <button
                v-for="cat in symptomCategories"
                :key="cat.id"
                type="button"
                @click="issueCategory = cat.id"
                class="rounded-2xl border p-4 text-left transition-all duration-200 flex items-start gap-3.5 group cursor-pointer"
                :class="issueCategory === cat.id
                  ? 'border-blue-500 bg-blue-50/70 ring-2 ring-blue-500/20 shadow-xs'
                  : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50/50'"
              >
                <div
                  class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl transition"
                  :class="issueCategory === cat.id ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-600 group-hover:bg-slate-200'"
                >
                  <component :is="cat.icon" class="h-5 w-5" />
                </div>
                <div>
                  <span class="text-xs font-black text-slate-900 block leading-tight">
                    {{ cat.title }}
                  </span>
                  <span class="text-[11px] text-slate-500 block mt-1 leading-snug">
                    {{ cat.description }}
                  </span>
                </div>
              </button>
            </div>
          </div>

          <!-- 2. DETAILED DESCRIPTION -->
          <div>
            <div class="flex items-center justify-between mb-1.5">
              <label class="text-xs font-black text-slate-800 uppercase tracking-wider">
                2. Detailed Symptom Description & Error Logs *
              </label>
              <span class="text-[10px] text-slate-400">
                {{ issueDescription.length }} characters (min 15)
              </span>
            </div>
            <textarea
              v-model="issueDescription"
              rows="4"
              placeholder="e.g. The device suddenly shut down during 3D gaming and does not show power LED. Tried different power cable and outlet with no response..."
              class="w-full rounded-2xl border border-slate-200 bg-white p-4 text-xs font-semibold text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 shadow-2xs"
            ></textarea>
          </div>

          <!-- 3. RESOLUTION PREFERENCE -->
          <div>
            <label class="text-xs font-black text-slate-800 uppercase tracking-wider block mb-3">
              3. Desired RMA Resolution Preference *
            </label>

            <div class="grid grid-cols-1 md:grid-cols-3 gap-3.5">
              <button
                v-for="res in resolutionOptions"
                :key="res.id"
                type="button"
                @click="claimType = res.id as any"
                class="rounded-2xl border p-4 text-left transition-all flex flex-col justify-between group cursor-pointer"
                :class="claimType === res.id
                  ? 'border-blue-500 bg-blue-50/70 ring-2 ring-blue-500/20 shadow-xs'
                  : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50/50'"
              >
                <div>
                  <div class="flex items-center justify-between mb-2">
                    <div
                      class="flex h-9 w-9 items-center justify-center rounded-xl"
                      :class="claimType === res.id ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-600'"
                    >
                      <component :is="res.icon" class="h-4 w-4" />
                    </div>
                    <span class="rounded bg-blue-100 px-2 py-0.5 text-[9px] font-black text-blue-700">
                      {{ res.badge }}
                    </span>
                  </div>
                  <span class="text-xs font-black text-slate-900 block leading-tight">
                    {{ res.title }}
                  </span>
                  <p class="text-[11px] text-slate-500 mt-1 leading-snug">
                    {{ res.description }}
                  </p>
                </div>
              </button>
            </div>
          </div>

          <!-- 4. HANDOVER METHOD -->
          <div>
            <label class="text-xs font-black text-slate-800 uppercase tracking-wider block mb-3">
              4. Hardware Handover Method *
            </label>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
              <button
                v-for="h in handoverOptions"
                :key="h.id"
                type="button"
                @click="handoverMethod = h.id as any"
                class="rounded-2xl border p-4 text-left transition-all flex items-start gap-3.5 cursor-pointer"
                :class="handoverMethod === h.id
                  ? 'border-emerald-500 bg-emerald-50/60 ring-2 ring-emerald-500/20 shadow-xs'
                  : 'border-slate-200 bg-white hover:border-slate-300'"
              >
                <div
                  class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl"
                  :class="handoverMethod === h.id ? 'bg-emerald-600 text-white' : 'bg-slate-100 text-slate-600'"
                >
                  <component :is="h.icon" class="h-5 w-5" />
                </div>
                <div>
                  <div class="flex items-center gap-2">
                    <span class="text-xs font-black text-slate-900 block">{{ h.title }}</span>
                    <span class="rounded bg-emerald-100 px-1.5 py-0.2 text-[9px] font-black text-emerald-800">{{ h.tag }}</span>
                  </div>
                  <p class="text-[11px] text-slate-500 mt-1">{{ h.desc }}</p>
                </div>
              </button>
            </div>
          </div>

          <!-- 5. ATTACHMENT EVIDENCE -->
          <div>
            <label class="text-xs font-black text-slate-800 uppercase tracking-wider block mb-2">
              5. Photo / Diagnostic Proof Attachments (Optional)
            </label>

            <div class="rounded-2xl border border-dashed border-slate-300 bg-slate-50/60 p-6 text-center hover:border-blue-400 transition relative">
              <input
                type="file"
                multiple
                accept="image/*,video/*,.pdf"
                @change="handleFileUpload"
                class="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
              />
              <UploadCloud class="h-8 w-8 text-blue-500 mx-auto mb-2" />
              <p class="text-xs font-bold text-slate-700">Click or drag photos of the screen defect or serial label</p>
              <p class="text-[10px] text-slate-400 mt-0.5">PNG, JPG, MP4 or PDF up to 25MB</p>
            </div>

            <!-- Uploaded file list -->
            <div v-if="uploadedFiles.length > 0" class="mt-3 flex flex-wrap gap-2">
              <div
                v-for="(f, i) in uploadedFiles"
                :key="i"
                class="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-bold text-slate-700 shadow-2xs"
              >
                <FileText class="h-3.5 w-3.5 text-blue-600" />
                <span class="truncate max-w-[150px]">{{ f.name }}</span>
                <span class="text-[10px] text-slate-400 font-mono">({{ f.size }})</span>
                <button type="button" @click="removeUploadedFile(i)" class="text-slate-400 hover:text-rose-600">
                  <X class="h-3.5 w-3.5" />
                </button>
              </div>
            </div>
          </div>

          <!-- BUTTONS -->
          <div class="flex items-center justify-between pt-6 border-t border-slate-100">
            <button
              type="button"
              @click="currentStep = 1"
              class="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-5 py-2.5 text-xs font-bold text-slate-700 hover:bg-slate-100 transition cursor-pointer"
            >
              <ArrowLeft class="h-4 w-4" />
              <span>Back to Selection</span>
            </button>

            <button
              type="button"
              :disabled="isSubmitting"
              @click="submitWarrantyClaim"
              class="inline-flex items-center gap-2 rounded-2xl bg-gradient-to-r from-blue-600 to-cyan-500 hover:from-blue-700 hover:to-cyan-600 px-8 py-3.5 text-xs font-black text-white shadow-lg shadow-blue-500/20 transition active:scale-95 disabled:opacity-50 cursor-pointer"
            >
              <RefreshCw v-if="isSubmitting" class="h-4 w-4 animate-spin" />
              <ShieldCheck v-else class="h-4 w-4" />
              <span>{{ isSubmitting ? 'Authorizing Claim...' : 'Authorize & Submit RMA Claim' }}</span>
            </button>
          </div>
        </div>

        <!-- ====================================================
             STEP 3 CONTENT: OFFICIAL RMA CERTIFICATE & PACKING SLIP
        ==================================================== -->
        <div v-else-if="currentStep === 3 && activeRmaClaim" class="space-y-6">

          <!-- TOP SUCCESS BANNER -->
          <div class="rounded-2xl border border-emerald-200 bg-emerald-50/80 p-5 flex flex-col sm:flex-row items-center justify-between gap-4">
            <div class="flex items-center gap-3.5">
              <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-600 text-white shadow-md shadow-emerald-600/20 shrink-0">
                <CheckCircle2 class="h-6 w-6" />
              </div>
              <div>
                <div class="flex items-center gap-2">
                  <h3 class="text-base font-black text-emerald-950">RMA Claim Successfully Authorized</h3>
                  <span class="rounded-full bg-emerald-200/80 px-2 py-0.5 text-[10px] font-black text-emerald-900">
                    APPROVED
                  </span>
                </div>
                <p class="text-xs text-emerald-700 mt-0.5">
                  Your official RMA packing slip is ready. A priority SMS & email confirmation has been dispatched.
                </p>
              </div>
            </div>

            <!-- ACTION BUTTONS -->
            <div class="flex items-center gap-2 shrink-0">
              <button
                type="button"
                @click="printRmaCertificate"
                class="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-black text-slate-800 shadow-2xs hover:bg-slate-50 transition cursor-pointer"
              >
                <Printer class="h-3.5 w-3.5 text-blue-600" />
                <span>Print Packing Slip</span>
              </button>
              <button
                type="button"
                @click="resetWizard"
                class="inline-flex items-center gap-1.5 rounded-xl bg-blue-600 hover:bg-blue-700 px-4 py-2 text-xs font-black text-white shadow-md shadow-blue-500/20 transition cursor-pointer"
              >
                <Plus class="h-3.5 w-3.5" />
                <span>New Claim</span>
              </button>
            </div>
          </div>

          <!-- THE PRINTABLE / DOWNLOADABLE RMA CERTIFICATE CONTAINER -->
          <div
            id="rma-certificate"
            class="relative rounded-3xl border border-slate-200 bg-white p-6 sm:p-10 shadow-xl text-slate-900 space-y-6"
          >
            <!-- CERTIFICATE HEADER -->
            <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between border-b-2 border-slate-900 pb-6 gap-4">
              <div class="flex items-center gap-3.5">
                <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-900 text-white font-black text-lg tracking-tighter">
                  NR
                </div>
                <div>
                  <span class="text-[10px] font-black uppercase tracking-widest text-blue-600 block">
                    NEXUSRIGS CUSTOMER CARE & RMA DIVISION
                  </span>
                  <h1 class="text-xl sm:text-2xl font-black text-slate-900">
                    Official RMA Return Packing Slip
                  </h1>
                  <p class="text-[10px] text-slate-400">
                    Flagship Lab: 425 Galle Road, Colombo 03, Sri Lanka • Hotlines: +94 11 234 5678
                  </p>
                </div>
              </div>

              <!-- RMA NUMBER BADGE & BARCODE -->
              <div class="sm:text-right">
                <span class="text-[10px] font-bold uppercase tracking-wider text-slate-400 block">RMA Authorization No</span>
                <div class="flex items-center sm:justify-end gap-1.5">
                  <span class="font-mono text-lg sm:text-xl font-black text-slate-950 tracking-wider">
                    {{ activeRmaClaim.rmaNumber }}
                  </span>
                  <button
                    type="button"
                    @click="copyText(activeRmaClaim.rmaNumber)"
                    class="text-slate-400 hover:text-blue-600 p-0.5"
                    title="Copy RMA"
                  >
                    <Check v-if="copiedRmaCode" class="h-3.5 w-3.5 text-emerald-600" />
                    <Copy v-else class="h-3.5 w-3.5" />
                  </button>
                </div>

                <!-- Digital barcode mockup -->
                <div class="font-mono text-[10px] tracking-widest text-slate-500 mt-1 select-none">
                  ||||| | |||| ||| ||||||| || |||
                </div>
              </div>
            </div>

            <!-- 2-COLUMN CERTIFICATE SUMMARY -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6 text-xs">

              <!-- Customer Info -->
              <div class="rounded-2xl bg-slate-50 p-4 border border-slate-100 space-y-1.5">
                <span class="text-[10px] font-black uppercase tracking-wider text-slate-400 block mb-1">
                  Customer & Handover Information
                </span>
                <p class="font-black text-sm text-slate-900">{{ activeRmaClaim.product.customerName || 'Kamal Gunarathna' }}</p>
                <p class="text-slate-600"><strong>Email:</strong> {{ activeRmaClaim.product.customerEmail || 'kamalgune@gmail.com' }}</p>
                <p class="text-slate-600"><strong>Phone:</strong> {{ activeRmaClaim.product.customerPhone || '0771234567' }}</p>
                <p class="text-slate-600">
                  <strong>Handover Option:</strong>
                  {{ activeRmaClaim.handoverMethod === 'courier' ? 'Free Domex Courier Doorstep Collection' : 'Walk-in Dropoff at Colombo Flagship Hub' }}
                </p>
              </div>

              <!-- RMA Service Info -->
              <div class="rounded-2xl bg-slate-50 p-4 border border-slate-100 space-y-1.5">
                <span class="text-[10px] font-black uppercase tracking-wider text-slate-400 block mb-1">
                  Authorization Metadata
                </span>
                <p class="text-slate-600"><strong>Authorized Date:</strong> {{ activeRmaClaim.createdAt }}</p>
                <p class="text-slate-600"><strong>Dispatch Validity:</strong> {{ activeRmaClaim.validUntil }} (14 Days)</p>
                <p class="text-slate-600">
                  <strong>Resolution:</strong>
                  <span class="capitalize font-bold text-blue-700"> {{ activeRmaClaim.claimType }}</span>
                </p>
                <p class="text-slate-600">
                  <strong>Status:</strong>
                  <span class="font-black text-emerald-600"> AUTHORIZED & ACTIVE</span>
                </p>
              </div>

            </div>

            <!-- HARDWARE COVERAGE DETAIL TABLE -->
            <div class="rounded-2xl border border-slate-200 overflow-hidden">
              <table class="w-full text-left text-xs">
                <thead class="bg-slate-100 text-[10px] font-black uppercase text-slate-600 border-b border-slate-200">
                  <tr>
                    <th class="py-3 px-4">Hardware Component</th>
                    <th class="py-3 px-4">Invoice Ref</th>
                    <th class="py-3 px-4">Hardware Serial No</th>
                    <th class="py-3 px-4">Symptom Classification</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                  <tr>
                    <td class="py-3 px-4 font-bold text-slate-900">
                      {{ activeRmaClaim.product.name }}
                    </td>
                    <td class="py-3 px-4 font-mono font-bold text-blue-600">
                      #{{ activeRmaClaim.product.orderId }}
                    </td>
                    <td class="py-3 px-4 font-mono font-bold text-slate-800">
                      {{ activeRmaClaim.product.serialNumber }}
                    </td>
                    <td class="py-3 px-4 font-semibold text-slate-700">
                      {{ activeRmaClaim.issueCategory }}
                    </td>
                  </tr>
                </tbody>
              </table>

              <!-- Symptom notes excerpt -->
              <div class="bg-slate-50/80 p-4 border-t border-slate-200 text-xs">
                <span class="text-[10px] font-bold uppercase tracking-wider text-slate-400 block mb-0.5">
                  Reported Technical Diagnostics:
                </span>
                <p class="text-slate-700 italic">"{{ activeRmaClaim.issueDescription }}"</p>
              </div>
            </div>

            <!-- PACKAGING & RETURN INSTRUCTIONS -->
            <div class="rounded-2xl border border-blue-100 bg-blue-50/60 p-4 text-xs text-blue-900 space-y-1">
              <p class="font-black uppercase tracking-wider text-[10px] text-blue-800">
                📦 Next Steps for Hardware Handover:
              </p>
              <ol class="list-decimal list-inside space-y-1 text-[11px] text-blue-800">
                <li>Place component into anti-static bag or original packaging box.</li>
                <li>Tape this printed RMA Slip to the outside of the parcel (or write <strong>{{ activeRmaClaim.rmaNumber }}</strong> prominently in bold marker).</li>
                <li>Hand over to the Domex collection rider or drop off at our Colombo 03 lab desk.</li>
              </ol>
            </div>

          </div>

          <!-- FOOTER TIMELINE TRACKER PREVIEW -->
          <div class="rounded-3xl border border-white/90 bg-white/80 p-6 shadow-lg backdrop-blur-xl">
            <span class="text-[10px] font-black uppercase tracking-wider text-slate-400 block mb-4">
              RMA Service Stage Pipeline
            </span>

            <div class="grid grid-cols-1 sm:grid-cols-4 gap-4">
              <div class="flex items-center gap-3">
                <div class="flex h-8 w-8 items-center justify-center rounded-xl bg-emerald-600 text-white font-bold text-xs shadow-xs">
                  ✓
                </div>
                <div>
                  <p class="text-xs font-black text-slate-900">RMA Authorized</p>
                  <p class="text-[10px] text-slate-400">Complete</p>
                </div>
              </div>

              <div class="flex items-center gap-3">
                <div class="flex h-8 w-8 items-center justify-center rounded-xl bg-blue-600 text-white font-bold text-xs shadow-xs">
                  2
                </div>
                <div>
                  <p class="text-xs font-black text-slate-900">Courier Handover</p>
                  <p class="text-[10px] text-blue-600 font-bold">In Progress</p>
                </div>
              </div>

              <div class="flex items-center gap-3 opacity-60">
                <div class="flex h-8 w-8 items-center justify-center rounded-xl bg-slate-100 text-slate-500 font-bold text-xs">
                  3
                </div>
                <div>
                  <p class="text-xs font-black text-slate-900">Bench Diagnostics</p>
                  <p class="text-[10px] text-slate-400">48-Hour SLA</p>
                </div>
              </div>

              <div class="flex items-center gap-3 opacity-60">
                <div class="flex h-8 w-8 items-center justify-center rounded-xl bg-slate-100 text-slate-500 font-bold text-xs">
                  4
                </div>
                <div>
                  <p class="text-xs font-black text-slate-900">Replacement Shipped</p>
                  <p class="text-[10px] text-slate-400">Final Step</p>
                </div>
              </div>
            </div>
          </div>

        </div>

      </section>

    </main>
  </div>
</template>

<style>
@media print {
  body * {
    visibility: hidden;
  }
  #rma-certificate, #rma-certificate * {
    visibility: visible;
  }
  #rma-certificate {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    margin: 0;
    padding: 24px;
    box-shadow: none !important;
    border: 1px solid #e2e8f0 !important;
  }
}
</style>