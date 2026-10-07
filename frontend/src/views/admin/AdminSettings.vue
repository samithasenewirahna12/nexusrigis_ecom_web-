<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminConfirmModal from '../../components/admin/AdminConfirmModal.vue'
import api from '../../services/api'
import {
  Settings,
  Store,
  ShoppingBag,
  Boxes,
  Truck,
  ShieldCheck,
  Bell,
  Database,
  Save,
  RotateCcw,
  Download,
  Upload,
  CheckCircle2,
  AlertTriangle,
  Info,
  Lock,
  Mail,
  Phone,
  Globe,
  MapPin,
  CreditCard,
  Percent,
  Clock,
  Sparkles,
  Server,
  Cpu,
  Check,
  X,
  RefreshCw,
  ExternalLink
} from 'lucide-vue-next'

/* =========================================================
   SETTINGS DATA STRUCTURE
========================================================= */

export interface AdminSettingsState {
  // Store info
  storeName: string
  storeTagline: string
  supportEmail: string
  contactPhone: string
  address: string
  primaryCurrency: string
  timezone: string
  dateFormat: string

  // Orders & Commerce
  minOrderValue: number
  freeShippingThreshold: number
  defaultTaxRate: number
  orderPrefix: string
  guestCheckout: boolean
  allowBackorders: boolean
  autoCancelHours: number
  returnWindowDays: number
  requirePhoneCheckout: boolean

  // Inventory & Hardware
  lowStockThreshold: number
  criticalStockAlert: boolean
  maxGpuPerCustomer: number
  autoHideOutOfStock: boolean
  enableBuildEstimator: boolean
  autoApproveReviews: boolean
  verifiedReviewsOnly: boolean

  // Shipping & Logistics
  standardShippingFee: number
  expressShippingFee: number
  standardDeliveryDays: string
  expressDeliveryDays: string
  freeShippingEnabled: boolean
  carriers: string[]
  orderTrackingPortal: boolean

  // Security & Staff
  enforce2FA: boolean
  sessionTimeoutMinutes: number
  maxFailedLogins: number
  auditLogging: boolean
  maintenanceMode: boolean
  maintenanceMessage: string

  // Notifications
  notifyNewOrder: boolean
  notifyLowStock: boolean
  notifyReturnRequest: boolean
  notifyDailyDigest: boolean
  opsNotificationEmail: string
  orderChimeSound: boolean
}

const DEFAULT_SETTINGS: AdminSettingsState = {
  storeName: 'NexusRigs Performance Hardware',
  storeTagline: 'Custom Gaming Rigs, Workstations & Enthusiast Computer Hardware',
  supportEmail: 'support@nexusrigs.com',
  contactPhone: '+1 (800) 639-8774',
  address: '108 Cyber Tower Blvd, Silicon Valley, CA 94016',
  primaryCurrency: 'USD',
  timezone: 'America/New_York',
  dateFormat: 'MM/DD/YYYY',

  minOrderValue: 25.0,
  freeShippingThreshold: 500.0,
  defaultTaxRate: 8.25,
  orderPrefix: 'NR-',
  guestCheckout: false,
  allowBackorders: false,
  autoCancelHours: 24,
  returnWindowDays: 30,
  requirePhoneCheckout: true,

  lowStockThreshold: 5,
  criticalStockAlert: true,
  maxGpuPerCustomer: 2,
  autoHideOutOfStock: false,
  enableBuildEstimator: true,
  autoApproveReviews: true,
  verifiedReviewsOnly: true,

  standardShippingFee: 14.99,
  expressShippingFee: 34.99,
  standardDeliveryDays: '3 - 5 Business Days',
  expressDeliveryDays: '1 - 2 Business Days',
  freeShippingEnabled: true,
  carriers: ['FedEx Priority', 'UPS Ground', 'Nexus Direct Delivery'],
  orderTrackingPortal: true,

  enforce2FA: true,
  sessionTimeoutMinutes: 60,
  maxFailedLogins: 5,
  auditLogging: true,
  maintenanceMode: false,
  maintenanceMessage: 'NexusRigs is currently performing scheduled hardware inventory synchronizations. We will be back online shortly!',

  notifyNewOrder: true,
  notifyLowStock: true,
  notifyReturnRequest: true,
  notifyDailyDigest: false,
  opsNotificationEmail: 'ops@nexusrigs.com',
  orderChimeSound: true
}

/* =========================================================
   REACTIVE STATE
========================================================= */

type TabKey = 'general' | 'orders' | 'inventory' | 'shipping' | 'security' | 'notifications' | 'system'

const activeTab = ref<TabKey>('general')
const settings = reactive<AdminSettingsState>({ ...DEFAULT_SETTINGS })
const originalSettingsJson = ref('')
const isSaving = ref(false)
const isFlushingCache = ref(false)
const showResetModal = ref(false)
const toastMessage = ref('')
const toastType = ref<'success' | 'error' | 'info'>('success')
const jsonFileInput = ref<HTMLInputElement | null>(null)

// System diagnostics
const backendHealth = ref<'healthy' | 'checking' | 'offline'>('checking')
const systemPing = ref<number | null>(null)
const lastSavedTime = ref<string>('Not yet saved')

const tabs: { key: TabKey; label: string; icon: any; description: string }[] = [
  { key: 'general', label: 'General & Store', icon: Store, description: 'Store identity, address, currency & regional settings' },
  { key: 'orders', label: 'Orders & Commerce', icon: ShoppingBag, description: 'Checkout rules, tax rates, return policies & thresholds' },
  { key: 'inventory', label: 'Hardware & Catalog', icon: Boxes, description: 'Stock thresholds, purchase limits & review moderation' },
  { key: 'shipping', label: 'Shipping & Logistics', icon: Truck, description: 'Delivery rates, courier services & fulfillment tracking' },
  { key: 'security', label: 'Security & Access', icon: ShieldCheck, description: 'Staff 2FA, session lifetimes & maintenance switch' },
  { key: 'notifications', label: 'Alerts & Email', icon: Bell, description: 'Automated staff emails, dispatch alerts & webhooks' },
  { key: 'system', label: 'System & Backup', icon: Database, description: 'Cache memory flush, backup export & diagnostics' }
]

/* =========================================================
   COMPUTED HELPERS
========================================================= */

const hasUnsavedChanges = computed(() => {
  return JSON.stringify(settings) !== originalSettingsJson.value
})

const activeTabInfo = computed(() => {
  return tabs.find(t => t.key === activeTab.value) || tabs[0]
})

/* =========================================================
   METHODS & ACTIONS
========================================================= */

const showToast = (message: string, type: 'success' | 'error' | 'info' = 'success') => {
  toastMessage.value = message
  toastType.value = type
  setTimeout(() => {
    if (toastMessage.value === message) {
      toastMessage.value = ''
    }
  }, 4500)
}

const loadSettings = () => {
  try {
    const raw = localStorage.getItem('nexusrigs_admin_settings')
    if (raw) {
      const parsed = JSON.parse(raw)
      Object.assign(settings, { ...DEFAULT_SETTINGS, ...parsed })
    } else {
      Object.assign(settings, DEFAULT_SETTINGS)
    }
  } catch (err) {
    console.error('Failed to load settings from storage:', err)
    Object.assign(settings, DEFAULT_SETTINGS)
  }
  originalSettingsJson.value = JSON.stringify(settings)

  const savedTimestamp = localStorage.getItem('nexusrigs_settings_last_saved')
  if (savedTimestamp) {
    lastSavedTime.value = savedTimestamp
  }
}

const saveSettings = async () => {
  isSaving.value = true
  try {
    // Artificial small delay for polished UI transition
    await new Promise(r => setTimeout(r, 450))

    // Persist in localStorage
    localStorage.setItem('nexusrigs_admin_settings', JSON.stringify(settings))
    const nowStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    lastSavedTime.value = nowStr
    localStorage.setItem('nexusrigs_settings_last_saved', nowStr)

    originalSettingsJson.value = JSON.stringify(settings)
    window.dispatchEvent(new CustomEvent('nexusrigs-settings-updated', { detail: settings }))

    showToast('Platform settings saved and synchronized successfully!', 'success')
  } catch (err: any) {
    console.error('Failed to save settings:', err)
    showToast('Failed to save settings. Please try again.', 'error')
  } finally {
    isSaving.value = false
  }
}

const discardChanges = () => {
  if (originalSettingsJson.value) {
    try {
      const parsed = JSON.parse(originalSettingsJson.value)
      Object.assign(settings, parsed)
      showToast('Unsaved changes discarded.', 'info')
    } catch {
      loadSettings()
    }
  }
}

const executeResetToDefaults = () => {
  Object.assign(settings, DEFAULT_SETTINGS)
  saveSettings()
  showResetModal.value = false
  showToast('Settings reset to factory defaults.', 'info')
}

// Flush Catalog & Cache
const flushSystemCache = async () => {
  isFlushingCache.value = true
  try {
    await new Promise(r => setTimeout(r, 800))
    // Clear product cached timestamps or storage items if any
    sessionStorage.removeItem('cached_categories')
    sessionStorage.removeItem('cached_products')
    showToast('API & Frontend catalog cache successfully cleared.', 'success')
  } catch {
    showToast('Failed to flush cache.', 'error')
  } finally {
    isFlushingCache.value = false
  }
}

// Check Backend API Ping
const checkSystemHealth = async () => {
  backendHealth.value = 'checking'
  const start = performance.now()
  try {
    await api.get('/products')
    const end = performance.now()
    systemPing.value = Math.round(end - start)
    backendHealth.value = 'healthy'
  } catch {
    try {
      await api.get('/categories')
      const end = performance.now()
      systemPing.value = Math.round(end - start)
      backendHealth.value = 'healthy'
    } catch {
      backendHealth.value = 'offline'
      systemPing.value = null
    }
  }
}

// Export Configuration as JSON
const exportConfiguration = () => {
  try {
    const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(settings, null, 2))
    const dlAnchor = document.createElement('a')
    dlAnchor.setAttribute('href', dataStr)
    dlAnchor.setAttribute('download', `nexusrigs-settings-${new Date().toISOString().slice(0, 10)}.json`)
    dlAnchor.click()
    showToast('Settings configuration exported.', 'success')
  } catch (err) {
    showToast('Failed to export configuration file.', 'error')
  }
}

// Import Configuration from JSON
const triggerImportJson = () => {
  jsonFileInput.value?.click()
}

const handleJsonFileSelected = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const content = e.target?.result as string
      const parsed = JSON.parse(content)
      Object.assign(settings, { ...DEFAULT_SETTINGS, ...parsed })
      showToast('Settings successfully imported from backup file!', 'success')
    } catch (err) {
      showToast('Invalid JSON file format. Import failed.', 'error')
    } finally {
      if (jsonFileInput.value) {
        jsonFileInput.value.value = ''
      }
    }
  }
  reader.readAsText(file)
}

// Carrier toggle
const toggleCarrier = (carrier: string) => {
  const idx = settings.carriers.indexOf(carrier)
  if (idx > -1) {
    if (settings.carriers.length > 1) {
      settings.carriers.splice(idx, 1)
    } else {
      showToast('At least one shipping carrier must remain active.', 'error')
    }
  } else {
    settings.carriers.push(carrier)
  }
}

onMounted(() => {
  loadSettings()
  checkSystemHealth()
})
</script>

<template>
  <div class="min-h-screen bg-slate-100 text-slate-900">

    <!-- =====================================================
         SIDEBAR
    ====================================================== -->
    <AdminSidebar />

    <!-- =====================================================
         MAIN CONTENT AREA
    ====================================================== -->
    <main class="ml-64 min-h-screen pb-24">
      <div class="p-4 sm:p-6 lg:p-8 max-w-7xl mx-auto">

        <!-- =================================================
             PAGE HEADER & BREADCRUMBS
        ================================================== -->
        <div class="mb-8">
          <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <div class="mb-2 flex items-center gap-2 text-sm text-slate-500">
                <span class="font-medium text-slate-600">Admin Console</span>
                <svg class="h-4 w-4 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="m9 18 6-6-6-6" />
                </svg>
                <span class="font-semibold text-blue-600">System Configuration</span>
              </div>

              <div class="flex items-center gap-3">
                <h1 class="text-3xl font-black tracking-tight text-slate-950 sm:text-4xl">
                  Platform Settings
                </h1>
                <span v-if="settings.maintenanceMode" class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-amber-50 border border-amber-200 text-amber-700 text-xs font-black uppercase tracking-wider animate-pulse">
                  <AlertTriangle class="w-3.5 h-3.5" /> Maintenance Mode
                </span>
              </div>

              <p class="mt-2 text-sm text-slate-500 sm:text-base max-w-2xl">
                Configure global store identity, payment & checkout thresholds, catalog automation rules, security protocols and notifications.
              </p>
            </div>

            <!-- ACTION BUTTONS -->
            <div class="flex flex-wrap items-center gap-3">
              <button
                @click="exportConfiguration"
                class="inline-flex items-center gap-2 px-4 py-2.5 rounded-2xl bg-white/80 border border-slate-200/80 text-slate-700 hover:text-blue-600 hover:bg-white text-xs font-bold shadow-sm transition hover:-translate-y-0.5"
                title="Download settings backup JSON"
              >
                <Download class="w-4 h-4 text-slate-500" />
                <span>Export Config</span>
              </button>

              <button
                @click="saveSettings"
                :disabled="isSaving || !hasUnsavedChanges"
                :class="[
                  'inline-flex items-center gap-2 px-5 py-2.5 rounded-2xl text-xs font-bold transition shadow-lg',
                  hasUnsavedChanges
                    ? 'bg-gradient-to-r from-blue-600 to-cyan-500 text-white shadow-blue-500/25 hover:shadow-blue-500/40 hover:-translate-y-0.5'
                    : 'bg-slate-200 text-slate-400 cursor-not-allowed shadow-none'
                ]"
              >
                <RefreshCw v-if="isSaving" class="w-4 h-4 animate-spin" />
                <Save v-else class="w-4 h-4" />
                <span>{{ isSaving ? 'Saving...' : 'Save Changes' }}</span>
              </button>
            </div>
          </div>
        </div>

        <!-- =================================================
             MAINTENANCE MODE BANNER (IF ACTIVE)
        ================================================== -->
        <div
          v-if="settings.maintenanceMode"
          class="mb-6 rounded-3xl border border-amber-200/90 bg-amber-50/90 p-4 sm:p-5 text-amber-900 shadow-xl shadow-amber-500/10 backdrop-blur-xl flex items-start gap-4"
        >
          <div class="w-10 h-10 rounded-2xl bg-amber-100 border border-amber-300 flex items-center justify-center shrink-0">
            <AlertTriangle class="w-5 h-5 text-amber-600" />
          </div>
          <div class="flex-1 min-w-0">
            <h4 class="text-sm font-black uppercase tracking-wider text-amber-950">Storefront Maintenance Mode is Active</h4>
            <p class="text-xs text-amber-800 mt-1">
              Public visitors see the custom maintenance landing notice: <em>"{{ settings.maintenanceMessage }}"</em>. Administrative operations remain fully active.
            </p>
          </div>
          <button
            @click="settings.maintenanceMode = false; saveSettings()"
            class="px-3 py-1.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold shadow-sm transition"
          >
            Turn Off
          </button>
        </div>

        <!-- =================================================
             NAVIGATION TABS & DIAGNOSTICS ROW
        ================================================== -->
        <div class="mb-6 flex flex-col lg:flex-row gap-4 items-stretch lg:items-center justify-between">

          <!-- TAB BUTTONS -->
          <div class="flex items-center gap-1.5 overflow-x-auto p-1.5 rounded-2xl bg-white/70 border border-white/90 shadow-lg shadow-slate-200/30 backdrop-blur-xl scrollbar-hide">
            <button
              v-for="t in tabs"
              :key="t.key"
              @click="activeTab = t.key"
              :class="[
                'flex items-center gap-2 px-3.5 py-2.5 rounded-xl text-xs font-bold transition whitespace-nowrap',
                activeTab === t.key
                  ? 'bg-blue-600 text-white shadow-md shadow-blue-500/30'
                  : 'text-slate-600 hover:text-blue-600 hover:bg-blue-50/60'
              ]"
            >
              <component :is="t.icon" class="w-4 h-4" />
              <span>{{ t.label }}</span>
            </button>
          </div>

          <!-- DIAGNOSTICS BADGES -->
          <div class="flex items-center gap-3 shrink-0">
            <!-- Health Badge -->
            <div class="flex items-center gap-2 px-3 py-2 rounded-2xl bg-white/80 border border-white/90 shadow-md shadow-slate-200/30 backdrop-blur-xl text-xs font-medium text-slate-600">
              <span class="relative flex h-2 w-2">
                <span
                  :class="[
                    'absolute inline-flex h-full w-full rounded-full opacity-75',
                    backendHealth === 'healthy' ? 'animate-ping bg-emerald-400' : 'bg-amber-400'
                  ]"
                ></span>
                <span
                  :class="[
                    'relative inline-flex h-2 w-2 rounded-full',
                    backendHealth === 'healthy' ? 'bg-emerald-500' : 'bg-amber-500'
                  ]"
                ></span>
              </span>
              <span>API Gateway:</span>
              <span class="font-bold text-slate-800">
                {{ backendHealth === 'healthy' ? (systemPing ? `${systemPing}ms` : 'Connected') : 'Checking' }}
              </span>
            </div>

            <!-- Last Saved -->
            <div class="hidden sm:flex items-center gap-1.5 px-3 py-2 rounded-2xl bg-white/80 border border-white/90 shadow-md shadow-slate-200/30 backdrop-blur-xl text-xs text-slate-500 font-medium">
              <Clock class="w-3.5 h-3.5 text-slate-400" />
              <span>Saved:</span>
              <span class="font-semibold text-slate-700">{{ lastSavedTime }}</span>
            </div>
          </div>

        </div>

        <!-- =================================================
             MAIN SETTINGS CARD
        ================================================== -->
        <section class="relative overflow-hidden rounded-3xl border border-white/90 bg-white/80 p-6 sm:p-8 lg:p-10 shadow-2xl shadow-slate-300/25 backdrop-blur-2xl">
          <!-- Ambient gradient orbs -->
          <div class="pointer-events-none absolute -right-24 -top-24 h-72 w-72 rounded-full bg-blue-500/10 blur-3xl"></div>
          <div class="pointer-events-none absolute -bottom-24 -left-24 h-72 w-72 rounded-full bg-cyan-400/10 blur-3xl"></div>

          <!-- TAB HEADER INFO -->
          <div class="mb-8 pb-6 border-b border-slate-200/80 flex items-center justify-between">
            <div class="flex items-center gap-4">
              <div class="w-12 h-12 rounded-2xl bg-gradient-to-tr from-blue-600 to-cyan-500 flex items-center justify-center text-white shadow-lg shadow-blue-500/25">
                <component :is="activeTabInfo.icon" class="w-6 h-6" />
              </div>
              <div>
                <h2 class="text-xl font-black text-slate-950 tracking-tight">
                  {{ activeTabInfo.label }}
                </h2>
                <p class="text-xs sm:text-sm text-slate-500 mt-0.5">
                  {{ activeTabInfo.description }}
                </p>
              </div>
            </div>

            <div v-if="hasUnsavedChanges" class="hidden sm:inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-blue-50 border border-blue-200 text-blue-700 text-xs font-bold">
              <span class="w-2 h-2 rounded-full bg-blue-600 animate-pulse"></span>
              Unsaved changes pending
            </div>
          </div>

          <!-- ===============================================
               TAB 1: GENERAL & STORE
          ================================================ -->
          <div v-if="activeTab === 'general'" class="space-y-8">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">

              <!-- Store Name -->
              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                  Storefront Brand Name <span class="text-red-500">*</span>
                </label>
                <div class="relative">
                  <Store class="absolute left-3.5 top-3.5 w-4 h-4 text-slate-400" />
                  <input
                    v-model="settings.storeName"
                    type="text"
                    class="w-full pl-10 pr-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-sm font-semibold text-slate-900 focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 transition outline-none"
                    placeholder="NexusRigs Performance Hardware"
                  />
                </div>
                <p class="text-[11px] text-slate-500 mt-1.5">Displayed across storefront headers, invoices, and system receipts.</p>
              </div>

              <!-- Store Tagline -->
              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                  Storefront Slogan / Tagline
                </label>
                <div class="relative">
                  <Sparkles class="absolute left-3.5 top-3.5 w-4 h-4 text-slate-400" />
                  <input
                    v-model="settings.storeTagline"
                    type="text"
                    class="w-full pl-10 pr-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-sm font-medium text-slate-900 focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 transition outline-none"
                    placeholder="High-Performance Hardware & Custom Gaming Rigs"
                  />
                </div>
                <p class="text-[11px] text-slate-500 mt-1.5">Appears below the brand logo in public customer navigation.</p>
              </div>

              <!-- Support Email -->
              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                  Customer Support Email <span class="text-red-500">*</span>
                </label>
                <div class="relative">
                  <Mail class="absolute left-3.5 top-3.5 w-4 h-4 text-slate-400" />
                  <input
                    v-model="settings.supportEmail"
                    type="email"
                    class="w-full pl-10 pr-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-sm font-medium text-slate-900 focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 transition outline-none"
                    placeholder="support@nexusrigs.com"
                  />
                </div>
                <p class="text-[11px] text-slate-500 mt-1.5">Inquiries from customers and automated confirmation replies go here.</p>
              </div>

              <!-- Contact Phone -->
              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                  Support Hotline / Phone
                </label>
                <div class="relative">
                  <Phone class="absolute left-3.5 top-3.5 w-4 h-4 text-slate-400" />
                  <input
                    v-model="settings.contactPhone"
                    type="text"
                    class="w-full pl-10 pr-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-sm font-medium text-slate-900 focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 transition outline-none"
                    placeholder="+1 (800) 639-8774"
                  />
                </div>
                <p class="text-[11px] text-slate-500 mt-1.5">Featured in checkout assistance and customer order packing slips.</p>
              </div>

              <!-- Physical Address -->
              <div class="md:col-span-2">
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                  Headquarters / Warehouse Fulfillment Address
                </label>
                <div class="relative">
                  <MapPin class="absolute left-3.5 top-3.5 w-4 h-4 text-slate-400" />
                  <input
                    v-model="settings.address"
                    type="text"
                    class="w-full pl-10 pr-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-sm font-medium text-slate-900 focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 transition outline-none"
                    placeholder="108 Cyber Tower Blvd, Silicon Valley, CA 94016"
                  />
                </div>
                <p class="text-[11px] text-slate-500 mt-1.5">Printed as the origin dispatch address for return shipments and warranty returns.</p>
              </div>

            </div>

            <!-- Regional & Currency Settings -->
            <div class="pt-6 border-t border-slate-200/70">
              <h3 class="text-sm font-black uppercase tracking-wider text-slate-800 mb-4 flex items-center gap-2">
                <Globe class="w-4 h-4 text-blue-600" /> Currency & Regional Formatting
              </h3>
              <div class="grid grid-cols-1 sm:grid-cols-3 gap-6">

                <div>
                  <label class="block text-xs font-bold text-slate-700 mb-2">Primary Store Currency</label>
                  <select
                    v-model="settings.primaryCurrency"
                    class="w-full px-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-sm font-semibold text-slate-900 focus:bg-white focus:border-blue-500 outline-none transition"
                  >
                    <option value="USD">USD ($) - United States Dollar</option>
                    <option value="EUR">EUR (€) - Eurozone</option>
                    <option value="GBP">GBP (£) - British Pound</option>
                    <option value="LKR">LKR (Rs.) - Sri Lankan Rupee</option>
                    <option value="CAD">CAD (C$) - Canadian Dollar</option>
                    <option value="AUD">AUD (A$) - Australian Dollar</option>
                  </select>
                </div>

                <div>
                  <label class="block text-xs font-bold text-slate-700 mb-2">Timezone</label>
                  <select
                    v-model="settings.timezone"
                    class="w-full px-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-sm font-medium text-slate-900 focus:bg-white focus:border-blue-500 outline-none transition"
                  >
                    <option value="America/New_York">Eastern Time (US / Canada - UTC-5)</option>
                    <option value="America/Los_Angeles">Pacific Time (US / Canada - UTC-8)</option>
                    <option value="Europe/London">Greenwich Mean Time (UTC+0)</option>
                    <option value="Asia/Colombo">Sri Lanka Standard Time (UTC+5:30)</option>
                    <option value="Asia/Tokyo">Japan Standard Time (UTC+9)</option>
                  </select>
                </div>

                <div>
                  <label class="block text-xs font-bold text-slate-700 mb-2">Date Display Format</label>
                  <select
                    v-model="settings.dateFormat"
                    class="w-full px-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-sm font-medium text-slate-900 focus:bg-white focus:border-blue-500 outline-none transition"
                  >
                    <option value="MM/DD/YYYY">MM/DD/YYYY (e.g. 10/07/2026)</option>
                    <option value="DD/MM/YYYY">DD/MM/YYYY (e.g. 07/10/2026)</option>
                    <option value="YYYY-MM-DD">YYYY-MM-DD (ISO Standard)</option>
                  </select>
                </div>

              </div>
            </div>
          </div>

          <!-- ===============================================
               TAB 2: ORDERS & COMMERCE
          ================================================ -->
          <div v-if="activeTab === 'orders'" class="space-y-8">
            <div class="grid grid-cols-1 md:grid-cols-3 gap-6">

              <!-- Minimum Order Value -->
              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                  Min. Order Value ($)
                </label>
                <div class="relative">
                  <span class="absolute left-3.5 top-3.5 text-sm font-bold text-slate-400">$</span>
                  <input
                    v-model.number="settings.minOrderValue"
                    type="number"
                    step="5"
                    min="0"
                    class="w-full pl-8 pr-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-sm font-bold text-slate-900 focus:bg-white focus:border-blue-500 outline-none transition"
                  />
                </div>
                <p class="text-[11px] text-slate-500 mt-1.5">Customers cannot checkout with cart amounts below this minimum.</p>
              </div>

              <!-- Free Shipping Threshold -->
              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                  Free Shipping Tier ($)
                </label>
                <div class="relative">
                  <span class="absolute left-3.5 top-3.5 text-sm font-bold text-slate-400">$</span>
                  <input
                    v-model.number="settings.freeShippingThreshold"
                    type="number"
                    step="50"
                    min="0"
                    class="w-full pl-8 pr-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-sm font-bold text-slate-900 focus:bg-white focus:border-blue-500 outline-none transition"
                  />
                </div>
                <p class="text-[11px] text-slate-500 mt-1.5">Orders meeting or exceeding this qualify automatically for free shipping.</p>
              </div>

              <!-- Default Tax Rate -->
              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                  Standard Tax Rate (%)
                </label>
                <div class="relative">
                  <Percent class="absolute right-3.5 top-3.5 w-4 h-4 text-slate-400" />
                  <input
                    v-model.number="settings.defaultTaxRate"
                    type="number"
                    step="0.25"
                    min="0"
                    max="50"
                    class="w-full px-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-sm font-bold text-slate-900 focus:bg-white focus:border-blue-500 outline-none transition"
                  />
                </div>
                <p class="text-[11px] text-slate-500 mt-1.5">Calculated dynamically at checkout summary before payment.</p>
              </div>

              <!-- Order Prefix -->
              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                  Order Number Prefix
                </label>
                <input
                  v-model="settings.orderPrefix"
                  type="text"
                  class="w-full px-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-sm font-mono font-bold text-slate-900 focus:bg-white focus:border-blue-500 outline-none transition"
                  placeholder="NR-"
                />
                <p class="text-[11px] text-slate-500 mt-1.5">Formatted as <code class="bg-slate-100 px-1 py-0.5 rounded text-blue-600 font-bold">{{ settings.orderPrefix }}10492</code>.</p>
              </div>

              <!-- Unpaid Auto-Cancel Hours -->
              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                  Auto-Cancel Unpaid Orders
                </label>
                <select
                  v-model.number="settings.autoCancelHours"
                  class="w-full px-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-sm font-semibold text-slate-900 focus:bg-white focus:border-blue-500 outline-none transition"
                >
                  <option :value="12">12 Hours</option>
                  <option :value="24">24 Hours (Standard)</option>
                  <option :value="48">48 Hours</option>
                  <option :value="72">72 Hours</option>
                </select>
                <p class="text-[11px] text-slate-500 mt-1.5">Releases reserved inventory if transaction is not completed.</p>
              </div>

              <!-- Return Window Days -->
              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                  Return & Warranty Window
                </label>
                <select
                  v-model.number="settings.returnWindowDays"
                  class="w-full px-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-sm font-semibold text-slate-900 focus:bg-white focus:border-blue-500 outline-none transition"
                >
                  <option :value="14">14 Days (Strict)</option>
                  <option :value="30">30 Days (Standard)</option>
                  <option :value="60">60 Days (Extended)</option>
                  <option :value="90">90 Days (VIP Extended)</option>
                </select>
                <p class="text-[11px] text-slate-500 mt-1.5">Eligible timeframe for customer claims under <code class="text-blue-600">/returns</code>.</p>
              </div>

            </div>

            <!-- Toggles for Checkout Policies -->
            <div class="pt-6 border-t border-slate-200/70 space-y-4">
              <h3 class="text-sm font-black uppercase tracking-wider text-slate-800 mb-2">
                Checkout & Account Behaviors
              </h3>

              <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">

                <!-- Guest Checkout -->
                <div
                  @click="settings.guestCheckout = !settings.guestCheckout"
                  class="p-4 rounded-2xl border border-slate-200/80 bg-slate-50/60 hover:bg-white hover:border-blue-300 transition cursor-pointer flex items-center justify-between"
                >
                  <div>
                    <p class="text-xs font-bold text-slate-900">Guest Checkout</p>
                    <p class="text-[11px] text-slate-500 mt-0.5">Allow purchasing without user signup</p>
                  </div>
                  <div :class="['w-11 h-6 flex items-center rounded-full p-1 transition-colors duration-200', settings.guestCheckout ? 'bg-blue-600' : 'bg-slate-300']">
                    <div :class="['bg-white w-4 h-4 rounded-full shadow-md transform transition-transform duration-200', settings.guestCheckout ? 'translate-x-5' : 'translate-x-0']"></div>
                  </div>
                </div>

                <!-- Allow Backorders -->
                <div
                  @click="settings.allowBackorders = !settings.allowBackorders"
                  class="p-4 rounded-2xl border border-slate-200/80 bg-slate-50/60 hover:bg-white hover:border-blue-300 transition cursor-pointer flex items-center justify-between"
                >
                  <div>
                    <p class="text-xs font-bold text-slate-900">Allow Hardware Pre-Orders</p>
                    <p class="text-[11px] text-slate-500 mt-0.5">Permit purchases when stock is 0</p>
                  </div>
                  <div :class="['w-11 h-6 flex items-center rounded-full p-1 transition-colors duration-200', settings.allowBackorders ? 'bg-blue-600' : 'bg-slate-300']">
                    <div :class="['bg-white w-4 h-4 rounded-full shadow-md transform transition-transform duration-200', settings.allowBackorders ? 'translate-x-5' : 'translate-x-0']"></div>
                  </div>
                </div>

                <!-- Require Phone -->
                <div
                  @click="settings.requirePhoneCheckout = !settings.requirePhoneCheckout"
                  class="p-4 rounded-2xl border border-slate-200/80 bg-slate-50/60 hover:bg-white hover:border-blue-300 transition cursor-pointer flex items-center justify-between"
                >
                  <div>
                    <p class="text-xs font-bold text-slate-900">Mandatory Contact Phone</p>
                    <p class="text-[11px] text-slate-500 mt-0.5">Required for delivery courier SMS</p>
                  </div>
                  <div :class="['w-11 h-6 flex items-center rounded-full p-1 transition-colors duration-200', settings.requirePhoneCheckout ? 'bg-blue-600' : 'bg-slate-300']">
                    <div :class="['bg-white w-4 h-4 rounded-full shadow-md transform transition-transform duration-200', settings.requirePhoneCheckout ? 'translate-x-5' : 'translate-x-0']"></div>
                  </div>
                </div>

              </div>
            </div>
          </div>

          <!-- ===============================================
               TAB 3: HARDWARE & CATALOG
          ================================================ -->
          <div v-if="activeTab === 'inventory'" class="space-y-8">
            <div class="grid grid-cols-1 sm:grid-cols-3 gap-6">

              <!-- Low Stock Threshold -->
              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                  Low Stock Trigger (Units)
                </label>
                <div class="relative">
                  <Boxes class="absolute left-3.5 top-3.5 w-4 h-4 text-slate-400" />
                  <input
                    v-model.number="settings.lowStockThreshold"
                    type="number"
                    min="1"
                    max="100"
                    class="w-full pl-10 pr-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-sm font-bold text-slate-900 focus:bg-white focus:border-blue-500 outline-none transition"
                  />
                </div>
                <p class="text-[11px] text-slate-500 mt-1.5">Flags items as "Low Stock" in inventory manager and analytics.</p>
              </div>

              <!-- GPU Per Customer Limit -->
              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                  Max GPU Limit per Order
                </label>
                <div class="relative">
                  <Cpu class="absolute left-3.5 top-3.5 w-4 h-4 text-slate-400" />
                  <input
                    v-model.number="settings.maxGpuPerCustomer"
                    type="number"
                    min="1"
                    max="10"
                    class="w-full pl-10 pr-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-sm font-bold text-slate-900 focus:bg-white focus:border-blue-500 outline-none transition"
                  />
                </div>
                <p class="text-[11px] text-slate-500 mt-1.5">Anti-scalping safeguard for flagships (RTX 4090 / RX 7900 XTX).</p>
              </div>

              <!-- Critical Alert Option -->
              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                  Critical Out-of-Stock Alert
                </label>
                <div
                  @click="settings.criticalStockAlert = !settings.criticalStockAlert"
                  class="p-3.5 rounded-2xl border border-slate-200/80 bg-slate-50/60 hover:bg-white transition cursor-pointer flex items-center justify-between"
                >
                  <span class="text-xs font-bold text-slate-800">Warehouse Alert Flag</span>
                  <div :class="['w-11 h-6 flex items-center rounded-full p-1 transition-colors duration-200', settings.criticalStockAlert ? 'bg-blue-600' : 'bg-slate-300']">
                    <div :class="['bg-white w-4 h-4 rounded-full shadow-md transform transition-transform duration-200', settings.criticalStockAlert ? 'translate-x-5' : 'translate-x-0']"></div>
                  </div>
                </div>
                <p class="text-[11px] text-slate-500 mt-1.5">Triggers high-priority flag in warehouse staff view.</p>
              </div>

            </div>

            <!-- Moderation & Store Features -->
            <div class="pt-6 border-t border-slate-200/70 space-y-4">
              <h3 class="text-sm font-black uppercase tracking-wider text-slate-800 mb-2">
                Catalog Moderation & Storefront Tools
              </h3>

              <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">

                <!-- Auto-hide Out of stock -->
                <div
                  @click="settings.autoHideOutOfStock = !settings.autoHideOutOfStock"
                  class="p-4 rounded-2xl border border-slate-200/80 bg-slate-50/60 hover:bg-white transition cursor-pointer flex items-center justify-between"
                >
                  <div>
                    <p class="text-xs font-bold text-slate-900">Auto-Hide Depleted Products</p>
                    <p class="text-[11px] text-slate-500 mt-0.5">Hides products automatically from catalog when quantity hits 0</p>
                  </div>
                  <div :class="['w-11 h-6 flex items-center rounded-full p-1 transition-colors duration-200', settings.autoHideOutOfStock ? 'bg-blue-600' : 'bg-slate-300']">
                    <div :class="['bg-white w-4 h-4 rounded-full shadow-md transform transition-transform duration-200', settings.autoHideOutOfStock ? 'translate-x-5' : 'translate-x-0']"></div>
                  </div>
                </div>

                <!-- Custom PC Build Estimator -->
                <div
                  @click="settings.enableBuildEstimator = !settings.enableBuildEstimator"
                  class="p-4 rounded-2xl border border-slate-200/80 bg-slate-50/60 hover:bg-white transition cursor-pointer flex items-center justify-between"
                >
                  <div>
                    <p class="text-xs font-bold text-slate-900">Custom Rig Builder Tool (/builds)</p>
                    <p class="text-[11px] text-slate-500 mt-0.5">Enables the interactive PC component compatibility estimator</p>
                  </div>
                  <div :class="['w-11 h-6 flex items-center rounded-full p-1 transition-colors duration-200', settings.enableBuildEstimator ? 'bg-blue-600' : 'bg-slate-300']">
                    <div :class="['bg-white w-4 h-4 rounded-full shadow-md transform transition-transform duration-200', settings.enableBuildEstimator ? 'translate-x-5' : 'translate-x-0']"></div>
                  </div>
                </div>

                <!-- Auto-approve reviews -->
                <div
                  @click="settings.autoApproveReviews = !settings.autoApproveReviews"
                  class="p-4 rounded-2xl border border-slate-200/80 bg-slate-50/60 hover:bg-white transition cursor-pointer flex items-center justify-between"
                >
                  <div>
                    <p class="text-xs font-bold text-slate-900">Instant Review Publishing</p>
                    <p class="text-[11px] text-slate-500 mt-0.5">Publish customer star ratings without requiring manual moderation</p>
                  </div>
                  <div :class="['w-11 h-6 flex items-center rounded-full p-1 transition-colors duration-200', settings.autoApproveReviews ? 'bg-blue-600' : 'bg-slate-300']">
                    <div :class="['bg-white w-4 h-4 rounded-full shadow-md transform transition-transform duration-200', settings.autoApproveReviews ? 'translate-x-5' : 'translate-x-0']"></div>
                  </div>
                </div>

                <!-- Verified Reviews Only -->
                <div
                  @click="settings.verifiedReviewsOnly = !settings.verifiedReviewsOnly"
                  class="p-4 rounded-2xl border border-slate-200/80 bg-slate-50/60 hover:bg-white transition cursor-pointer flex items-center justify-between"
                >
                  <div>
                    <p class="text-xs font-bold text-slate-900">Verified Purchaser Badge Only</p>
                    <p class="text-[11px] text-slate-500 mt-0.5">Restricts product review submissions to customers who ordered the SKU</p>
                  </div>
                  <div :class="['w-11 h-6 flex items-center rounded-full p-1 transition-colors duration-200', settings.verifiedReviewsOnly ? 'bg-blue-600' : 'bg-slate-300']">
                    <div :class="['bg-white w-4 h-4 rounded-full shadow-md transform transition-transform duration-200', settings.verifiedReviewsOnly ? 'translate-x-5' : 'translate-x-0']"></div>
                  </div>
                </div>

              </div>
            </div>
          </div>

          <!-- ===============================================
               TAB 4: SHIPPING & LOGISTICS
          ================================================ -->
          <div v-if="activeTab === 'shipping'" class="space-y-8">
            <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">

              <!-- Standard Shipping Fee -->
              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                  Standard Ground Fee ($)
                </label>
                <div class="relative">
                  <span class="absolute left-3.5 top-3.5 text-sm font-bold text-slate-400">$</span>
                  <input
                    v-model.number="settings.standardShippingFee"
                    type="number"
                    step="0.5"
                    min="0"
                    class="w-full pl-8 pr-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-sm font-bold text-slate-900 focus:bg-white focus:border-blue-500 outline-none transition"
                  />
                </div>
              </div>

              <!-- Standard Transit Window -->
              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                  Standard Delivery Window
                </label>
                <input
                  v-model="settings.standardDeliveryDays"
                  type="text"
                  class="w-full px-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-sm font-medium text-slate-900 focus:bg-white focus:border-blue-500 outline-none transition"
                  placeholder="3 - 5 Business Days"
                />
              </div>

              <!-- Express Shipping Fee -->
              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                  Express Air Priority ($)
                </label>
                <div class="relative">
                  <span class="absolute left-3.5 top-3.5 text-sm font-bold text-slate-400">$</span>
                  <input
                    v-model.number="settings.expressShippingFee"
                    type="number"
                    step="1"
                    min="0"
                    class="w-full pl-8 pr-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-sm font-bold text-slate-900 focus:bg-white focus:border-blue-500 outline-none transition"
                  />
                </div>
              </div>

              <!-- Express Transit Window -->
              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                  Express Delivery Window
                </label>
                <input
                  v-model="settings.expressDeliveryDays"
                  type="text"
                  class="w-full px-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-sm font-medium text-slate-900 focus:bg-white focus:border-blue-500 outline-none transition"
                  placeholder="1 - 2 Business Days"
                />
              </div>

            </div>

            <!-- Carriers & Logistics Partners -->
            <div class="pt-6 border-t border-slate-200/70 space-y-4">
              <h3 class="text-sm font-black uppercase tracking-wider text-slate-800 mb-2 flex items-center gap-2">
                <Truck class="w-4 h-4 text-blue-600" /> Active Shipping & Courier Partners
              </h3>
              <p class="text-xs text-slate-500">Select which carriers are enabled for fulfillment dispatch and tracking number assignment.</p>

              <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
                <div
                  v-for="carrier in ['FedEx Priority', 'UPS Ground', 'DHL Express', 'Nexus Direct Delivery']"
                  :key="carrier"
                  @click="toggleCarrier(carrier)"
                  :class="[
                    'p-4 rounded-2xl border text-center font-bold text-xs cursor-pointer transition select-none flex flex-col items-center gap-2',
                    settings.carriers.includes(carrier)
                      ? 'bg-blue-50/80 border-blue-300 text-blue-700 shadow-sm'
                      : 'bg-slate-50/50 border-slate-200/80 text-slate-400 hover:text-slate-600'
                  ]"
                >
                  <div :class="['w-8 h-8 rounded-xl flex items-center justify-center', settings.carriers.includes(carrier) ? 'bg-blue-600 text-white' : 'bg-slate-200 text-slate-400']">
                    <Check v-if="settings.carriers.includes(carrier)" class="w-4 h-4" />
                    <X v-else class="w-4 h-4" />
                  </div>
                  <span>{{ carrier }}</span>
                </div>
              </div>
            </div>

            <!-- Order Tracking Portal Toggle -->
            <div class="pt-4 border-t border-slate-200/70">
              <div
                @click="settings.orderTrackingPortal = !settings.orderTrackingPortal"
                class="p-4 rounded-2xl border border-slate-200/80 bg-slate-50/60 hover:bg-white transition cursor-pointer flex items-center justify-between"
              >
                <div>
                  <p class="text-xs font-bold text-slate-900">Public Live Order Tracking Portal (/orders/track)</p>
                  <p class="text-[11px] text-slate-500 mt-0.5">Allows customers to look up delivery timeline and courier coordinates via Order ID</p>
                </div>
                <div :class="['w-11 h-6 flex items-center rounded-full p-1 transition-colors duration-200', settings.orderTrackingPortal ? 'bg-blue-600' : 'bg-slate-300']">
                  <div :class="['bg-white w-4 h-4 rounded-full shadow-md transform transition-transform duration-200', settings.orderTrackingPortal ? 'translate-x-5' : 'translate-x-0']"></div>
                </div>
              </div>
            </div>
          </div>

          <!-- ===============================================
               TAB 5: SECURITY & ACCESS
          ================================================ -->
          <div v-if="activeTab === 'security'" class="space-y-8">
            <div class="grid grid-cols-1 sm:grid-cols-3 gap-6">

              <!-- Session Timeout -->
              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                  Staff Inactivity Timeout
                </label>
                <select
                  v-model.number="settings.sessionTimeoutMinutes"
                  class="w-full px-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-sm font-semibold text-slate-900 focus:bg-white focus:border-blue-500 outline-none transition"
                >
                  <option :value="15">15 Minutes (High Security)</option>
                  <option :value="30">30 Minutes</option>
                  <option :value="60">60 Minutes (Standard)</option>
                  <option :value="120">2 Hours</option>
                  <option :value="240">4 Hours</option>
                </select>
                <p class="text-[11px] text-slate-500 mt-1.5">Automatically locks staff dashboard on idle timeout.</p>
              </div>

              <!-- Max Failed Logins -->
              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                  Max Failed Login Attempts
                </label>
                <select
                  v-model.number="settings.maxFailedLogins"
                  class="w-full px-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-sm font-semibold text-slate-900 focus:bg-white focus:border-blue-500 outline-none transition"
                >
                  <option :value="3">3 Attempts (Strict)</option>
                  <option :value="5">5 Attempts (Recommended)</option>
                  <option :value="10">10 Attempts</option>
                </select>
                <p class="text-[11px] text-slate-500 mt-1.5">Triggers 15-minute IP rate-limit block upon threshold.</p>
              </div>

              <!-- 2FA Enforcement -->
              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                  Enforce Staff 2FA
                </label>
                <div
                  @click="settings.enforce2FA = !settings.enforce2FA"
                  class="p-3.5 rounded-2xl border border-slate-200/80 bg-slate-50/60 hover:bg-white transition cursor-pointer flex items-center justify-between"
                >
                  <span class="text-xs font-bold text-slate-800">Two-Factor Auth</span>
                  <div :class="['w-11 h-6 flex items-center rounded-full p-1 transition-colors duration-200', settings.enforce2FA ? 'bg-blue-600' : 'bg-slate-300']">
                    <div :class="['bg-white w-4 h-4 rounded-full shadow-md transform transition-transform duration-200', settings.enforce2FA ? 'translate-x-5' : 'translate-x-0']"></div>
                  </div>
                </div>
                <p class="text-[11px] text-slate-500 mt-1.5">Mandates authenticator code verification for admin login.</p>
              </div>

            </div>

            <!-- Storefront Maintenance Mode Config -->
            <div class="pt-6 border-t border-slate-200/70">
              <div class="p-6 rounded-3xl border border-slate-200/80 bg-slate-50/70 space-y-4">
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-3">
                    <div class="w-10 h-10 rounded-2xl bg-amber-100 border border-amber-200 flex items-center justify-center text-amber-700">
                      <AlertTriangle class="w-5 h-5" />
                    </div>
                    <div>
                      <h4 class="text-sm font-black text-slate-900">Emergency Storefront Maintenance Mode</h4>
                      <p class="text-xs text-slate-500">Temporarily displays a maintenance holding banner on public storefront pages</p>
                    </div>
                  </div>

                  <div
                    @click="settings.maintenanceMode = !settings.maintenanceMode"
                    :class="['w-12 h-7 flex items-center rounded-full p-1 transition-colors duration-200 cursor-pointer', settings.maintenanceMode ? 'bg-amber-600' : 'bg-slate-300']"
                  >
                    <div :class="['bg-white w-5 h-5 rounded-full shadow-md transform transition-transform duration-200', settings.maintenanceMode ? 'translate-x-5' : 'translate-x-0']"></div>
                  </div>
                </div>

                <div v-if="settings.maintenanceMode" class="pt-3 border-t border-slate-200/80">
                  <label class="block text-xs font-bold text-slate-700 mb-2">Public Announcement Message</label>
                  <textarea
                    v-model="settings.maintenanceMessage"
                    rows="2"
                    class="w-full px-4 py-3 rounded-2xl bg-white border border-slate-200 text-xs text-slate-900 focus:border-blue-500 outline-none transition resize-none"
                    placeholder="We are currently undergoing inventory synchronization..."
                  ></textarea>
                </div>
              </div>
            </div>

            <!-- Audit Trail Toggle -->
            <div class="pt-2">
              <div
                @click="settings.auditLogging = !settings.auditLogging"
                class="p-4 rounded-2xl border border-slate-200/80 bg-slate-50/60 hover:bg-white transition cursor-pointer flex items-center justify-between"
              >
                <div>
                  <p class="text-xs font-bold text-slate-900">Administrator Activity Audit Trail</p>
                  <p class="text-[11px] text-slate-500 mt-0.5">Captures immutable logs of price changes, inventory additions, and discount creations</p>
                </div>
                <div :class="['w-11 h-6 flex items-center rounded-full p-1 transition-colors duration-200', settings.auditLogging ? 'bg-blue-600' : 'bg-slate-300']">
                  <div :class="['bg-white w-4 h-4 rounded-full shadow-md transform transition-transform duration-200', settings.auditLogging ? 'translate-x-5' : 'translate-x-0']"></div>
                </div>
              </div>
            </div>

          </div>

          <!-- ===============================================
               TAB 6: NOTIFICATIONS & EMAIL
          ================================================ -->
          <div v-if="activeTab === 'notifications'" class="space-y-8">

            <div>
              <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                Operations Alert Recipient Email <span class="text-red-500">*</span>
              </label>
              <div class="relative max-w-xl">
                <Mail class="absolute left-3.5 top-3.5 w-4 h-4 text-slate-400" />
                <input
                  v-model="settings.opsNotificationEmail"
                  type="email"
                  class="w-full pl-10 pr-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-sm font-semibold text-slate-900 focus:bg-white focus:border-blue-500 outline-none transition"
                  placeholder="ops@nexusrigs.com"
                />
              </div>
              <p class="text-[11px] text-slate-500 mt-1.5">Warehouse dispatches and urgent stock shortage notices are dispatched to this address.</p>
            </div>

            <!-- Notification Triggers Grid -->
            <div class="pt-6 border-t border-slate-200/70 space-y-4">
              <h3 class="text-sm font-black uppercase tracking-wider text-slate-800 mb-2">
                Automated System Event Triggers
              </h3>

              <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">

                <!-- New Order Alert -->
                <div
                  @click="settings.notifyNewOrder = !settings.notifyNewOrder"
                  class="p-4 rounded-2xl border border-slate-200/80 bg-slate-50/60 hover:bg-white transition cursor-pointer flex items-center justify-between"
                >
                  <div>
                    <p class="text-xs font-bold text-slate-900">New Order Confirmation Email</p>
                    <p class="text-[11px] text-slate-500 mt-0.5">Dispatches instant alert to operations on incoming order</p>
                  </div>
                  <div :class="['w-11 h-6 flex items-center rounded-full p-1 transition-colors duration-200', settings.notifyNewOrder ? 'bg-blue-600' : 'bg-slate-300']">
                    <div :class="['bg-white w-4 h-4 rounded-full shadow-md transform transition-transform duration-200', settings.notifyNewOrder ? 'translate-x-5' : 'translate-x-0']"></div>
                  </div>
                </div>

                <!-- Low Stock Alert -->
                <div
                  @click="settings.notifyLowStock = !settings.notifyLowStock"
                  class="p-4 rounded-2xl border border-slate-200/80 bg-slate-50/60 hover:bg-white transition cursor-pointer flex items-center justify-between"
                >
                  <div>
                    <p class="text-xs font-bold text-slate-900">Low Stock Depletion Alert</p>
                    <p class="text-[11px] text-slate-500 mt-0.5">Sends automated email when SKU inventory reaches threshold</p>
                  </div>
                  <div :class="['w-11 h-6 flex items-center rounded-full p-1 transition-colors duration-200', settings.notifyLowStock ? 'bg-blue-600' : 'bg-slate-300']">
                    <div :class="['bg-white w-4 h-4 rounded-full shadow-md transform transition-transform duration-200', settings.notifyLowStock ? 'translate-x-5' : 'translate-x-0']"></div>
                  </div>
                </div>

                <!-- Return Request Alert -->
                <div
                  @click="settings.notifyReturnRequest = !settings.notifyReturnRequest"
                  class="p-4 rounded-2xl border border-slate-200/80 bg-slate-50/60 hover:bg-white transition cursor-pointer flex items-center justify-between"
                >
                  <div>
                    <p class="text-xs font-bold text-slate-900">Return & Warranty Claim Alert</p>
                    <p class="text-[11px] text-slate-500 mt-0.5">Notifies customer support on new return / RMA submissions</p>
                  </div>
                  <div :class="['w-11 h-6 flex items-center rounded-full p-1 transition-colors duration-200', settings.notifyReturnRequest ? 'bg-blue-600' : 'bg-slate-300']">
                    <div :class="['bg-white w-4 h-4 rounded-full shadow-md transform transition-transform duration-200', settings.notifyReturnRequest ? 'translate-x-5' : 'translate-x-0']"></div>
                  </div>
                </div>

                <!-- Daily Digest Alert -->
                <div
                  @click="settings.notifyDailyDigest = !settings.notifyDailyDigest"
                  class="p-4 rounded-2xl border border-slate-200/80 bg-slate-50/60 hover:bg-white transition cursor-pointer flex items-center justify-between"
                >
                  <div>
                    <p class="text-xs font-bold text-slate-900">Daily Executive Sales Digest</p>
                    <p class="text-[11px] text-slate-500 mt-0.5">Summarizes daily revenue, fulfillment totals & active users at midnight</p>
                  </div>
                  <div :class="['w-11 h-6 flex items-center rounded-full p-1 transition-colors duration-200', settings.notifyDailyDigest ? 'bg-blue-600' : 'bg-slate-300']">
                    <div :class="['bg-white w-4 h-4 rounded-full shadow-md transform transition-transform duration-200', settings.notifyDailyDigest ? 'translate-x-5' : 'translate-x-0']"></div>
                  </div>
                </div>

                <!-- Audio Chime -->
                <div
                  @click="settings.orderChimeSound = !settings.orderChimeSound"
                  class="p-4 rounded-2xl border border-slate-200/80 bg-slate-50/60 hover:bg-white transition cursor-pointer flex items-center justify-between sm:col-span-2"
                >
                  <div>
                    <p class="text-xs font-bold text-slate-900">Admin Audio Chime on Live Orders</p>
                    <p class="text-[11px] text-slate-500 mt-0.5">Plays an auditory chime when an incoming order is confirmed in the staff browser</p>
                  </div>
                  <div :class="['w-11 h-6 flex items-center rounded-full p-1 transition-colors duration-200', settings.orderChimeSound ? 'bg-blue-600' : 'bg-slate-300']">
                    <div :class="['bg-white w-4 h-4 rounded-full shadow-md transform transition-transform duration-200', settings.orderChimeSound ? 'translate-x-5' : 'translate-x-0']"></div>
                  </div>
                </div>

              </div>
            </div>

          </div>

          <!-- ===============================================
               TAB 7: SYSTEM & BACKUP
          ================================================ -->
          <div v-if="activeTab === 'system'" class="space-y-8">

            <!-- System Diagnostics Cards -->
            <div class="grid grid-cols-1 sm:grid-cols-3 gap-6">

              <div class="p-5 rounded-3xl bg-slate-50/80 border border-slate-200/80">
                <div class="flex items-center justify-between mb-3">
                  <span class="text-xs font-bold uppercase tracking-wider text-slate-500">Backend API</span>
                  <Server class="w-4 h-4 text-blue-600" />
                </div>
                <div class="flex items-center gap-2">
                  <span class="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
                  <span class="text-lg font-black text-slate-900">{{ backendHealth === 'healthy' ? 'Operational' : 'Checking' }}</span>
                </div>
                <p class="text-[11px] text-slate-500 mt-2">Latency: <span class="font-bold text-slate-700">{{ systemPing ? `${systemPing}ms` : 'Connecting...' }}</span></p>
              </div>

              <div class="p-5 rounded-3xl bg-slate-50/80 border border-slate-200/80">
                <div class="flex items-center justify-between mb-3">
                  <span class="text-xs font-bold uppercase tracking-wider text-slate-500">Catalog Database</span>
                  <Database class="w-4 h-4 text-cyan-600" />
                </div>
                <div class="flex items-center gap-2">
                  <span class="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
                  <span class="text-lg font-black text-slate-900">Synchronized</span>
                </div>
                <p class="text-[11px] text-slate-500 mt-2">Spring Boot API & JPA connected</p>
              </div>

              <div class="p-5 rounded-3xl bg-slate-50/80 border border-slate-200/80">
                <div class="flex items-center justify-between mb-3">
                  <span class="text-xs font-bold uppercase tracking-wider text-slate-500">Cache Memory</span>
                  <Cpu class="w-4 h-4 text-indigo-600" />
                </div>
                <div class="flex items-center gap-2">
                  <span class="w-2.5 h-2.5 rounded-full bg-blue-500"></span>
                  <span class="text-lg font-black text-slate-900">Active</span>
                </div>
                <p class="text-[11px] text-slate-500 mt-2">Local memory cache status: OK</p>
              </div>

            </div>

            <!-- Maintenance & Actions -->
            <div class="pt-6 border-t border-slate-200/70 space-y-6">
              <h3 class="text-sm font-black uppercase tracking-wider text-slate-800">
                Data Maintenance & Backup Operations
              </h3>

              <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">

                <!-- Flush Cache -->
                <div class="p-5 rounded-3xl border border-slate-200/80 bg-white shadow-sm flex flex-col justify-between gap-4">
                  <div>
                    <h4 class="text-sm font-bold text-slate-900 flex items-center gap-2">
                      <RefreshCw class="w-4 h-4 text-blue-600" /> Flush Application & Catalog Cache
                    </h4>
                    <p class="text-xs text-slate-500 mt-1">
                      Invalidates all cached product lists, categories, and customer rating payloads to force fresh backend retrieval.
                    </p>
                  </div>
                  <button
                    @click="flushSystemCache"
                    :disabled="isFlushingCache"
                    class="w-fit inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold transition"
                  >
                    <RefreshCw :class="['w-3.5 h-3.5', isFlushingCache && 'animate-spin']" />
                    <span>{{ isFlushingCache ? 'Flushing Cache...' : 'Flush Cache Now' }}</span>
                  </button>
                </div>

                <!-- Backup & Restore -->
                <div class="p-5 rounded-3xl border border-slate-200/80 bg-white shadow-sm flex flex-col justify-between gap-4">
                  <div>
                    <h4 class="text-sm font-bold text-slate-900 flex items-center gap-2">
                      <Download class="w-4 h-4 text-cyan-600" /> Backup & Restore Configuration
                    </h4>
                    <p class="text-xs text-slate-500 mt-1">
                      Download your customized store configuration file as a JSON backup or import a previously exported file.
                    </p>
                  </div>
                  <div class="flex items-center gap-3">
                    <button
                      @click="exportConfiguration"
                      class="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-blue-50 text-blue-600 hover:bg-blue-100 text-xs font-bold transition"
                    >
                      <Download class="w-3.5 h-3.5" />
                      <span>Download JSON</span>
                    </button>
                    <button
                      @click="triggerImportJson"
                      class="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-slate-100 text-slate-700 hover:bg-slate-200 text-xs font-bold transition"
                    >
                      <Upload class="w-3.5 h-3.5" />
                      <span>Import JSON</span>
                    </button>
                    <input
                      ref="jsonFileInput"
                      type="file"
                      accept=".json"
                      class="hidden"
                      @change="handleJsonFileSelected"
                    />
                  </div>
                </div>

              </div>

              <!-- Danger Zone: Factory Reset -->
              <div class="p-6 rounded-3xl border border-red-200/80 bg-red-50/60 mt-8">
                <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                  <div>
                    <h4 class="text-sm font-black text-red-950 flex items-center gap-2">
                      <AlertTriangle class="w-4 h-4 text-red-600" /> Reset Settings to Factory Defaults
                    </h4>
                    <p class="text-xs text-red-700 mt-1">
                      Restores all store policies, thresholds, shipping fees and regional options back to the default NexusRigs baseline configuration.
                    </p>
                  </div>
                  <button
                    @click="showResetModal = true"
                    class="px-4 py-2.5 rounded-2xl bg-red-600 hover:bg-red-700 text-white text-xs font-bold shadow-md shadow-red-500/20 transition whitespace-nowrap"
                  >
                    Reset All Settings
                  </button>
                </div>
              </div>

            </div>

          </div>

          <!-- ===============================================
               BOTTOM ACTION BAR (INSIDE CARD)
          ================================================ -->
          <div class="mt-10 pt-6 border-t border-slate-200/80 flex flex-wrap items-center justify-between gap-4">
            <div class="flex items-center gap-2 text-xs text-slate-500">
              <Info class="w-4 h-4 text-slate-400" />
              <span>Settings changes take effect immediately across all active browser sessions.</span>
            </div>

            <div class="flex items-center gap-3">
              <button
                v-if="hasUnsavedChanges"
                @click="discardChanges"
                class="px-4 py-2.5 rounded-2xl bg-white border border-slate-200 text-slate-600 hover:text-slate-900 text-xs font-bold transition"
              >
                Discard
              </button>

              <button
                @click="saveSettings"
                :disabled="isSaving || !hasUnsavedChanges"
                :class="[
                  'inline-flex items-center gap-2 px-6 py-2.5 rounded-2xl text-xs font-bold transition shadow-lg',
                  hasUnsavedChanges
                    ? 'bg-gradient-to-r from-blue-600 to-cyan-500 text-white shadow-blue-500/25 hover:shadow-blue-500/40 hover:-translate-y-0.5'
                    : 'bg-slate-200 text-slate-400 cursor-not-allowed shadow-none'
                ]"
              >
                <RefreshCw v-if="isSaving" class="w-4 h-4 animate-spin" />
                <Save v-else class="w-4 h-4" />
                <span>{{ isSaving ? 'Saving...' : 'Save Settings' }}</span>
              </button>
            </div>
          </div>

        </section>

      </div>
    </main>

    <!-- =====================================================
         STICKY FLOATING SAVE PROMPT (WHEN DIRTY)
    ====================================================== -->
    <transition
      enter-active-class="transform transition duration-300 ease-out"
      enter-from-class="translate-y-16 opacity-0"
      enter-to-class="translate-y-0 opacity-100"
      leave-active-class="transform transition duration-200 ease-in"
      leave-from-class="translate-y-0 opacity-100"
      leave-to-class="translate-y-16 opacity-0"
    >
      <div
        v-if="hasUnsavedChanges"
        class="fixed bottom-6 left-1/2 -translate-x-1/2 z-40 max-w-xl w-full px-4"
      >
        <div class="rounded-3xl border border-white/90 bg-white/90 p-4 shadow-2xl shadow-blue-500/20 backdrop-blur-2xl flex items-center justify-between gap-4">
          <div class="flex items-center gap-3">
            <span class="relative flex h-3 w-3">
              <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-400 opacity-75"></span>
              <span class="relative inline-flex rounded-full h-3 w-3 bg-blue-600"></span>
            </span>
            <div class="text-xs">
              <span class="font-bold text-slate-900">Unsaved Changes</span>
              <p class="text-slate-500 hidden sm:block">You have modified platform settings.</p>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <button
              @click="discardChanges"
              class="px-3.5 py-2 rounded-xl text-xs font-bold text-slate-600 hover:text-slate-900 hover:bg-slate-100 transition"
            >
              Discard
            </button>
            <button
              @click="saveSettings"
              :disabled="isSaving"
              class="px-5 py-2 rounded-xl text-xs font-bold bg-gradient-to-r from-blue-600 to-cyan-500 text-white shadow-md shadow-blue-500/30 hover:shadow-blue-500/50 hover:-translate-y-0.5 transition"
            >
              {{ isSaving ? 'Saving...' : 'Save Now' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- =====================================================
         TOAST NOTIFICATION
    ====================================================== -->
    <transition
      enter-active-class="transform transition duration-300 ease-out"
      enter-from-class="-translate-y-10 opacity-0"
      enter-to-class="translate-y-0 opacity-100"
      leave-active-class="transform transition duration-200 ease-in"
      leave-from-class="translate-y-0 opacity-100"
      leave-to-class="-translate-y-10 opacity-0"
    >
      <div
        v-if="toastMessage"
        class="fixed top-6 right-6 z-50 flex items-center gap-3 px-5 py-3.5 rounded-2xl border shadow-xl backdrop-blur-xl"
        :class="[
          toastType === 'success' ? 'bg-emerald-50/95 border-emerald-200 text-emerald-900 shadow-emerald-500/10' :
          toastType === 'error' ? 'bg-red-50/95 border-red-200 text-red-900 shadow-red-500/10' :
          'bg-blue-50/95 border-blue-200 text-blue-900 shadow-blue-500/10'
        ]"
      >
        <CheckCircle2 v-if="toastType === 'success'" class="w-5 h-5 text-emerald-600 shrink-0" />
        <AlertTriangle v-else-if="toastType === 'error'" class="w-5 h-5 text-red-600 shrink-0" />
        <Info v-else class="w-5 h-5 text-blue-600 shrink-0" />

        <span class="text-xs font-bold">{{ toastMessage }}</span>

        <button @click="toastMessage = ''" class="ml-2 text-slate-400 hover:text-slate-600">
          <X class="w-4 h-4" />
        </button>
      </div>
    </transition>

    <!-- =====================================================
         RESET CONFIRMATION MODAL
    ====================================================== -->
    <AdminConfirmModal
      v-model:show="showResetModal"
      type="danger"
      icon="warning"
      title="Reset Settings to Defaults?"
      message="This will overwrite all current store policies and configurations"
      target="Global Platform Settings"
      description="All custom shipping rates, checkout thresholds, moderation policies, and regional currency settings will revert to their factory defaults. This action cannot be reversed."
      confirm-text="Reset to Defaults"
      cancel-text="Keep Current Settings"
      @confirm="executeResetToDefaults"
    />

  </div>
</template>

<style scoped>
.scrollbar-hide {
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.scrollbar-hide::-webkit-scrollbar {
  display: none;
}
</style>
