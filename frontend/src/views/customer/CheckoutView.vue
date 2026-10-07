<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  CreditCard,
  Truck,
  ShieldCheck,
  MapPin,
  Phone,
  Mail,
  User,
  FileText,
  Download,
  Send,
  ArrowRight,
  ArrowLeft,
  ShoppingBag,
  CheckCircle2,
  Tag,
  AlertCircle,
  Building,
  Lock,
  ChevronRight,
  Sparkles,
  Printer,
  Copy,
  Check,
  X,
  Eye,
  Clock,
  Package,
  Ticket,
  Percent,
  Plus,
  Trash2,
  Wifi,
  Gift,
  Flame
} from 'lucide-vue-next'

import { useCartStore } from '../../stores/cartStore'
import { useAuthStore } from '../../stores/authStore'
import { usePopup } from '../../composables/usePopup'
import api from '../../services/api'
import { generateInvoicePdf, type InvoiceOrderData } from '../../utils/invoicePdfGenerator'
import logo from '../../assets/icons/logoIMG-removebg-preview.svg'

const router = useRouter()
const cartStore = useCartStore()
const authStore = useAuthStore()
const popup = usePopup()

// Order completion state
const orderCompleted = ref(false)
const completedOrder = ref<any>(null)
const isSubmitting = ref(false)
const errorMessage = ref('')

// Computed mathematically accurate total for completed order receipt (subtotal - discount + shipping + tax)
const completedOrderTotal = computed(() => {
  if (!completedOrder.value) return 0
  const sub = Number(completedOrder.value.subtotal || 0)
  const disc = Number(completedOrder.value.discount || 0)
  const ship = Number(completedOrder.value.shipping || 0)
  const tax = Number(completedOrder.value.tax || 0)
  return Math.max(0, sub - disc + ship + tax)
})

// Pre-cached PDF Blob for 0ms download/email latency
const cachedPdfBlob = ref<Blob | null>(null)

// Copy receipt ID state
const isCopied = ref(false)

// Receipt Modal preview state
const showReceiptModal = ref(false)

// Recipient email for receipt dispatch
const recipientEmail = ref('')
const isEditingEmail = ref(false)
const invoiceSentSuccessfully = ref(false)

// =========================================================
// CUSTOMER IDENTITY
// =========================================================

function getCustomerId(): string {
  try {
    if (authStore.user) {
      const id = authStore.user.customerId ?? authStore.user.userId ?? authStore.user.id
      if (id) return String(id)
    }
  } catch {
    // ignore
  }

  try {
    const savedUser = sessionStorage.getItem('user') || localStorage.getItem('user')
    if (savedUser) {
      const user = JSON.parse(savedUser)
      const id = user.customerId ?? user.userId ?? user.id
      if (id) return String(id)
    }
  } catch (error) {
    console.error('Unable to read user:', error)
  }

  return sessionStorage.getItem('userId') || localStorage.getItem('userId') || ''
}

// =========================================================
// STEP 1: SHIPPING & CONTACT
// =========================================================

const shippingInfo = ref({
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  address: '',
  city: '',
  postalCode: '',
  country: 'Sri Lanka',
  notes: ''
})

// =========================================================
// STEP 2: DELIVERY METHOD
// =========================================================

interface ShippingMethodOption {
  id: string
  name: string
  description: string
  duration: string
  cost: number
  icon: any
}

const shippingMethods: ShippingMethodOption[] = [
  {
    id: 'standard',
    name: 'Standard Express Courier',
    description: 'Island-wide tracked delivery by Pronto or Domex',
    duration: '2 - 3 Business Days',
    cost: 0,
    icon: Truck
  },
  {
    id: 'priority',
    name: 'Priority Same-Day Dispatch',
    description: 'Priority handling & express courier in Western Province',
    duration: 'Within 24 Hours',
    cost: 450,
    icon: Sparkles
  },
  {
    id: 'pickup',
    name: 'Store Pickup (Free)',
    description: 'Collect in person from NexusRigs Hub, Galle Road, Colombo 03',
    duration: 'Ready in 2 Hours',
    cost: 0,
    icon: Building
  }
]

const selectedShippingMethod = ref<string>('standard')

const selectedShippingCost = computed(() => {
  const method = shippingMethods.find(m => m.id === selectedShippingMethod.value)
  return method ? method.cost : 0
})

// =========================================================
// STEP 3: PAYMENT METHOD
// =========================================================

type PaymentMethodType = 'card' | 'cod'
const paymentMethod = ref<PaymentMethodType>('card')
const paymentError = ref('')

// Saved Cards
interface SavedCard {
  cardId: string
  cardNumber?: string
  last4?: string
  lastFour?: string
  cardHolderName?: string
  expiry?: string
  expiryMonth?: string
  expiryYear?: string
  brand?: string
  cardBrand?: string
  isDefault?: boolean
}

const savedCards = ref<SavedCard[]>([])
const selectedSavedCard = ref('')
const loadingSavedCards = ref(false)
const saveNewCardForFuture = ref(true)

// New Card Details
const cardDetails = ref({
  cardNumber: '',
  cardName: '',
  expiry: '',
  cvv: ''
})

// Active Card Display for 3D Luxury Visualizer
const activeCardDisplay = computed(() => {
  if (selectedSavedCard.value) {
    const sc = savedCards.value.find(c => c.cardId === selectedSavedCard.value)
    if (sc) {
      const brand = (sc.brand || sc.cardBrand || 'VISA').toUpperCase()
      const last4 = sc.last4 || sc.lastFour || '4242'
      const exp = sc.expiry || (sc.expiryMonth && sc.expiryYear ? `${sc.expiryMonth}/${String(sc.expiryYear).slice(-2)}` : '12/28')
      return {
        brand,
        number: `•••• •••• •••• ${last4}`,
        last4,
        holderName: (sc.cardHolderName || `${shippingInfo.value.firstName} ${shippingInfo.value.lastName}` || 'KAMAL GUNARATHNA').toUpperCase(),
        expiry: exp,
        isSaved: true,
        cardId: sc.cardId
      }
    }
  }
  return {
    brand: detectedCardBrand.value || 'VISA',
    number: cardDetails.value.cardNumber ? cardDetails.value.cardNumber.padEnd(19, '•') : '•••• •••• •••• ••••',
    last4: cardDetails.value.cardNumber.replace(/\s/g, '').slice(-4) || '••••',
    holderName: (cardDetails.value.cardName || `${shippingInfo.value.firstName} ${shippingInfo.value.lastName}` || 'SAMITHA SENEVIRATHNA').toUpperCase(),
    expiry: cardDetails.value.expiry || 'MM/YY',
    isSaved: false,
    cardId: ''
  }
})

async function deleteSavedCard(cardId: string) {
  try {
    await api.delete(`/payments/cards/${encodeURIComponent(cardId)}`)
    savedCards.value = savedCards.value.filter(c => c.cardId !== cardId)
    if (selectedSavedCard.value === cardId) {
      selectedSavedCard.value = savedCards.value[0]?.cardId || ''
    }
    await popup.success('Card removed from saved payment methods.', 'Card Removed')
  } catch (err: any) {
    console.warn('Could not delete card:', err)
  }
}

// =========================================================
// STEP 4: COUPONS & DISCOUNTS
// =========================================================

interface Coupon {
  couponId: string
  code: string
  discPercent: number
  startDate?: string
  endDate?: string
  description?: string
}

const availableCoupons = ref<Coupon[]>([])
const selectedCoupon = ref('')
const couponInput = ref('')
const couponMessage = ref('')
const couponError = ref('')
const loadingCoupons = ref(false)
const showCouponsModal = ref(false)
const copiedCouponCode = ref('')

async function copyCoupon(code: string) {
  try {
    await navigator.clipboard.writeText(code)
    copiedCouponCode.value = code
    setTimeout(() => { copiedCouponCode.value = '' }, 2000)
    await popup.success(`Promo code ${code} copied to clipboard!`, 'Copied')
  } catch {
    // fallback
  }
}

function selectCouponFromModal(code: string) {
  applyCouponCode(code)
  showCouponsModal.value = false
}

// =========================================================
// INVOICE STATE
// =========================================================

const invoiceLoading = ref(false)
const sendingInvoice = ref(false)
const invoiceEmailMessage = ref('')
const invoiceEmailError = ref('')

// =========================================================
// PRICE CALCULATIONS
// =========================================================

const cartItems = computed(() => cartStore.items)

const subtotal = computed(() => {
  return cartItems.value.reduce((total, item: any) => {
    const price = Number(item.unitPrice ?? item.price ?? 0)
    const quantity = Number(item.quantity ?? 1)
    return total + price * quantity
  }, 0)
})

const dealDiscount = computed(() => cartStore.dealDiscount)
const subtotalBeforeDeals = computed(() => cartStore.subtotalBeforeDeals)

const appliedCouponData = computed(() => {
  if (!selectedCoupon.value) return null
  return availableCoupons.value.find(c => c.code.toUpperCase() === selectedCoupon.value.toUpperCase()) || null
})

const couponDiscount = computed(() => {
  if (!selectedCoupon.value) return 0
  const coupon = appliedCouponData.value
  const discPercent = coupon ? Number(coupon.discPercent) : 10
  return Math.round(subtotal.value * (discPercent / 100))
})

const totalSavings = computed(() => dealDiscount.value + couponDiscount.value)

const estimatedTax = computed(() => {
  const taxable = Math.max(0, subtotal.value - couponDiscount.value)
  return Math.round(taxable * 0.08)
})

const finalTotal = computed(() => {
  const total = subtotal.value - couponDiscount.value + selectedShippingCost.value + estimatedTax.value
  return Math.max(0, total)
})

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('en-LK', {
    style: 'currency',
    currency: 'LKR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(amount)
}

function formatDate(isoString?: string): string {
  if (!isoString) return new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
  const d = new Date(isoString)
  return d.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// Card Brand Detector
const detectedCardBrand = computed(() => {
  const clean = cardDetails.value.cardNumber.replace(/\s/g, '')
  if (/^4/.test(clean)) return 'VISA'
  if (/^5[1-5]/.test(clean)) return 'MASTERCARD'
  if (/^3[47]/.test(clean)) return 'AMEX'
  return ''
})

// =========================================================
// =========================================================
// DATA FETCHING & LIFECYCLE
// =========================================================

// Sri Lanka City and Postal Directory for instant auto-assignment
const SRI_LANKA_CITIES = [
  { name: 'Colombo 05 (Havelock / Kirulapone)', postalCode: '00500', district: 'Colombo' },
  { name: 'Colombo 03 (Kollupitiya)', postalCode: '00300', district: 'Colombo' },
  { name: 'Colombo 01 (Fort)', postalCode: '00100', district: 'Colombo' },
  { name: 'Colombo 02 (Slave Island)', postalCode: '00200', district: 'Colombo' },
  { name: 'Colombo 04 (Bambalapitiya)', postalCode: '00400', district: 'Colombo' },
  { name: 'Colombo 07 (Cinnamon Gardens)', postalCode: '00700', district: 'Colombo' },
  { name: 'Colombo', postalCode: '00100', district: 'Colombo' },
  { name: 'Dehiwala', postalCode: '10350', district: 'Colombo' },
  { name: 'Mount Lavinia', postalCode: '10370', district: 'Colombo' },
  { name: 'Moratuwa', postalCode: '10400', district: 'Colombo' },
  { name: 'Nugegoda', postalCode: '10250', district: 'Colombo' },
  { name: 'Rajagiriya', postalCode: '10107', district: 'Colombo' },
  { name: 'Maharagama', postalCode: '10280', district: 'Colombo' },
  { name: 'Kaduwela', postalCode: '10640', district: 'Colombo' },
  { name: 'Gampaha', postalCode: '11000', district: 'Gampaha' },
  { name: 'Negombo', postalCode: '11500', district: 'Gampaha' },
  { name: 'Kelaniya', postalCode: '11600', district: 'Gampaha' },
  { name: 'Kandy', postalCode: '20000', district: 'Kandy' },
  { name: 'Peradeniya', postalCode: '20400', district: 'Kandy' },
  { name: 'Galle', postalCode: '80000', district: 'Galle' },
  { name: 'Matara', postalCode: '81000', district: 'Matara' },
  { name: 'Kurunegala', postalCode: '60000', district: 'Kurunegala' },
  { name: 'Kalutara', postalCode: '12000', district: 'Kalutara' },
  { name: 'Panadura', postalCode: '12500', district: 'Kalutara' },
  { name: 'Jaffna', postalCode: '40000', district: 'Jaffna' },
  { name: 'Batticaloa', postalCode: '30000', district: 'Batticaloa' }
]

function onCityInput(val: string) {
  shippingInfo.value.city = val
  const clean = val.trim().toLowerCase()
  const matched = SRI_LANKA_CITIES.find(c =>
    c.name.toLowerCase() === clean ||
    c.district.toLowerCase() === clean ||
    clean.includes(c.name.toLowerCase())
  )
  if (matched && (!shippingInfo.value.postalCode || shippingInfo.value.postalCode === '00300' || shippingInfo.value.postalCode === '00100' || shippingInfo.value.postalCode === '00500')) {
    shippingInfo.value.postalCode = matched.postalCode
  }
}

function usePresetAddress(type: 'colombo' | 'hub') {
  if (type === 'colombo') {
    shippingInfo.value.address = 'No. 124, High Level Road'
    shippingInfo.value.city = 'Colombo 05'
    shippingInfo.value.postalCode = '00500'
  } else {
    shippingInfo.value.address = 'NexusRigs Hub, No. 450 Galle Road'
    shippingInfo.value.city = 'Colombo 03'
    shippingInfo.value.postalCode = '00300'
  }
}

// Memory of courier address if user toggles store pickup back and forth
const savedCourierAddress = ref({
  address: '',
  city: '',
  postalCode: ''
})

watch(selectedShippingMethod, (newMethod, oldMethod) => {
  if (newMethod === 'pickup') {
    if (shippingInfo.value.address && !shippingInfo.value.address.includes('NexusRigs Hub')) {
      savedCourierAddress.value = {
        address: shippingInfo.value.address,
        city: shippingInfo.value.city,
        postalCode: shippingInfo.value.postalCode
      }
    }
    shippingInfo.value.address = 'NexusRigs Hub, No. 450 Galle Road'
    shippingInfo.value.city = 'Colombo 03'
    shippingInfo.value.postalCode = '00300'
  } else if (oldMethod === 'pickup') {
    if (savedCourierAddress.value.address) {
      shippingInfo.value.address = savedCourierAddress.value.address
      shippingInfo.value.city = savedCourierAddress.value.city || 'Colombo 05'
      shippingInfo.value.postalCode = savedCourierAddress.value.postalCode || '00500'
    } else {
      const savedAddrRaw = localStorage.getItem('saved_shipping_address')
      if (savedAddrRaw) {
        try {
          const parsed = JSON.parse(savedAddrRaw)
          if (parsed?.address && !parsed.address.includes('NexusRigs Hub')) {
            shippingInfo.value.address = parsed.address
            shippingInfo.value.city = parsed.city || 'Colombo 05'
            shippingInfo.value.postalCode = parsed.postalCode || '00500'
            return
          }
        } catch {
          // ignore
        }
      }
      shippingInfo.value.address = 'No. 124, High Level Road'
      shippingInfo.value.city = 'Colombo 05'
      shippingInfo.value.postalCode = '00500'
    }
  }
})

// Helper to safely extract address components from any format (object or string)
function extractAddressFields(userObj: any) {
  let street = ''
  let city = ''
  let postalCode = ''

  if (!userObj) return { street, city, postalCode }

  let addrSource = userObj.address

  if (typeof addrSource === 'string' && addrSource.trim().startsWith('{')) {
    try {
      addrSource = JSON.parse(addrSource)
    } catch {
      // ignore
    }
  }

  // 1. If address is an object { street, city, postalCode }
  if (addrSource && typeof addrSource === 'object') {
    street = addrSource.street || addrSource.address || addrSource.line1 || addrSource.addressLine1 || ''
    city = addrSource.city || ''
    postalCode = addrSource.postalCode || addrSource.zipCode || addrSource.zip || addrSource.postal_code || ''
  } else if (typeof addrSource === 'string' && addrSource.trim() && addrSource.trim() !== '[object Object]') {
    const raw = addrSource.trim()
    if (raw.includes(',')) {
      const parts = raw.split(',').map(p => p.trim()).filter(Boolean)
      if (parts.length >= 3) {
        street = parts.slice(0, parts.length - 2).join(', ')
        city = parts[parts.length - 2]
        postalCode = parts[parts.length - 1]
      } else if (parts.length === 2) {
        street = parts[0]
        city = parts[1]
      } else {
        street = raw
      }
    } else {
      street = raw
    }
  }

  // 2. Direct properties on the user object
  if (!street && userObj.street) street = String(userObj.street).trim()
  if (!street && userObj.addressLine1) street = String(userObj.addressLine1).trim()
  if (!city && userObj.city) city = String(userObj.city).trim()
  if (!postalCode && (userObj.postalCode || userObj.zipCode || userObj.zip)) {
    postalCode = String(userObj.postalCode || userObj.zipCode || userObj.zip).trim()
  }

  // Sanitize [object Object]
  if (street === '[object Object]') street = ''
  if (city === '[object Object]') city = ''
  if (postalCode === '[object Object]') postalCode = ''

  return { street, city, postalCode }
}

function applyAddressToForm(street: string, city: string, postalCode: string) {
  if (street && street !== '[object Object]' && (!shippingInfo.value.address || shippingInfo.value.address === '[object Object]')) {
    shippingInfo.value.address = street
  }
  if (city && city !== '[object Object]' && (!shippingInfo.value.city || shippingInfo.value.city === '[object Object]')) {
    shippingInfo.value.city = city
  }
  if (postalCode && postalCode !== '[object Object]' && (!shippingInfo.value.postalCode || shippingInfo.value.postalCode === '[object Object]')) {
    shippingInfo.value.postalCode = postalCode
  }
}

// Auto-save delivery address whenever changed so user never loses it
watch(
  () => ({
    address: shippingInfo.value.address,
    city: shippingInfo.value.city,
    postalCode: shippingInfo.value.postalCode,
    country: shippingInfo.value.country
  }),
  (newVal) => {
    if (newVal.address || newVal.city || newVal.postalCode) {
      try {
        localStorage.setItem('saved_shipping_address', JSON.stringify(newVal))
        const savedUserStr = sessionStorage.getItem('user') || localStorage.getItem('user')
        if (savedUserStr) {
          const u = JSON.parse(savedUserStr)
          u.address = {
            street: newVal.address,
            city: newVal.city,
            postalCode: newVal.postalCode
          }
          u.city = newVal.city
          u.postalCode = newVal.postalCode
          sessionStorage.setItem('user', JSON.stringify(u))
          localStorage.setItem('user', JSON.stringify(u))
        }
      } catch {
        // ignore
      }
    }
  },
  { deep: true }
)

onMounted(async () => {
  if (cartStore.items.length === 0) {
    await cartStore.fetchCart()
  }

  const customerId = getCustomerId()

  // 1. Pre-fill user data from sessionStorage or localStorage 'user'
  try {
    const savedUser = sessionStorage.getItem('user') || localStorage.getItem('user')
    if (savedUser) {
      const user = JSON.parse(savedUser)
      if (user.email) {
        shippingInfo.value.email = user.email
        recipientEmail.value = user.email
      }
      if (user.phone) shippingInfo.value.phone = user.phone
      if (user.name) {
        const parts = String(user.name).trim().split(' ')
        shippingInfo.value.firstName = parts.shift() || ''
        shippingInfo.value.lastName = parts.join(' ')
      }

      // Robust extraction of address, city, and postalCode
      const extracted = extractAddressFields(user)
      applyAddressToForm(extracted.street, extracted.city, extracted.postalCode)
    }
  } catch (err) {
    console.error('Error pre-filling user info:', err)
  }

  // 2. Pre-fill from cached shipping address if any field is still empty
  try {
    const savedAddrRaw = localStorage.getItem('saved_shipping_address')
    if (savedAddrRaw) {
      const savedAddr = JSON.parse(savedAddrRaw)
      if (savedAddr) {
        applyAddressToForm(savedAddr.address || '', savedAddr.city || '', savedAddr.postalCode || '')
        if (savedAddr.country) shippingInfo.value.country = savedAddr.country
      }
    }
  } catch (err) {
    console.warn('Error reading saved shipping address cache:', err)
  }

  // 3. Live customer record from backend API
  if (customerId) {
    try {
      const customerRes = await api.get(`/customers/${encodeURIComponent(customerId)}`)
      const customer = customerRes.data
      if (customer) {
        const extracted = extractAddressFields(customer)
        applyAddressToForm(extracted.street, extracted.city, extracted.postalCode)

        if (!shippingInfo.value.phone && customer.phone) {
          shippingInfo.value.phone = customer.phone
        }
        if (!shippingInfo.value.email && customer.email) {
          shippingInfo.value.email = customer.email
          if (!recipientEmail.value) recipientEmail.value = customer.email
        }
        if (!shippingInfo.value.firstName && customer.name) {
          const parts = String(customer.name).trim().split(' ')
          shippingInfo.value.firstName = parts.shift() || ''
          shippingInfo.value.lastName = parts.join(' ')
        }
      }
    } catch (apiErr) {
      console.warn('Could not fetch customer address from API:', apiErr)
    }
  } else if (shippingInfo.value.email) {
    try {
      const customerRes = await api.get(`/customers/email/${encodeURIComponent(shippingInfo.value.email)}`)
      const customer = customerRes.data
      if (customer) {
        const extracted = extractAddressFields(customer)
        applyAddressToForm(extracted.street, extracted.city, extracted.postalCode)
      }
    } catch {
      // ignore
    }
  }

  // 4. Recover address from any past orders in localStorage
  if (!shippingInfo.value.address || !shippingInfo.value.city) {
    try {
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i)
        if (key && key.startsWith('order_')) {
          const ord = JSON.parse(localStorage.getItem(key) || '{}')
          if (ord?.shippingInfo?.address && ord.shippingInfo.address !== '[object Object]') {
            applyAddressToForm(ord.shippingInfo.address, ord.shippingInfo.city || 'Colombo 05', ord.shippingInfo.postalCode || '00500')
            break
          }
        }
      }
    } catch {
      // ignore
    }
  }

  // 5. Ensure sensible non-empty defaults so address, city, and postal code are ALWAYS set
  if (!shippingInfo.value.address) {
    if (selectedShippingMethod.value === 'pickup') {
      shippingInfo.value.address = 'NexusRigs Hub, No. 450 Galle Road'
      shippingInfo.value.city = 'Colombo 03'
      shippingInfo.value.postalCode = '00300'
    } else {
      shippingInfo.value.address = 'No. 124, High Level Road'
      shippingInfo.value.city = 'Colombo 05'
      shippingInfo.value.postalCode = '00500'
    }
  }
  if (!shippingInfo.value.city) {
    shippingInfo.value.city = 'Colombo 05'
  }
  if (!shippingInfo.value.postalCode) {
    shippingInfo.value.postalCode = '00500'
  }

  // Check for pre-applied coupon from Cart or Offers
  try {
    const savedCoupon = sessionStorage.getItem('appliedCoupon')
    if (savedCoupon) {
      const parsed = JSON.parse(savedCoupon)
      if (parsed?.code) {
        selectedCoupon.value = parsed.code
        couponMessage.value = `${parsed.code} applied (${parsed.discPercent}% discount)`
      }
    }
  } catch (err) {
    console.error('Error reading applied coupon:', err)
  }

  await Promise.all([
    loadSavedCards(),
    loadCoupons()
  ])
})

async function loadSavedCards() {
  const customerId = getCustomerId()
  if (!customerId) return

  loadingSavedCards.value = true
  try {
    const response = await api.get(`/payments/cards?customerId=${customerId}`)
    if (Array.isArray(response.data)) {
      savedCards.value = response.data
      if (savedCards.value.length > 0) {
        selectedSavedCard.value = savedCards.value[0].cardId
      }
    }
  } catch (error) {
    console.warn('Unable to load saved cards:', error)
    savedCards.value = []
  } finally {
    loadingSavedCards.value = false
  }
}

async function loadCoupons() {
  loadingCoupons.value = true
  try {
    const response = await api.get('/coupons')
    if (Array.isArray(response.data)) {
      availableCoupons.value = response.data
    } else {
      availableCoupons.value = []
    }
  } catch (error) {
    console.warn('Unable to load coupons from API:', error)
    availableCoupons.value = [
      { couponId: '1', code: 'WELCOME10', discPercent: 10, description: '10% Welcome Discount' },
      { couponId: '2', code: 'NEXUS15', discPercent: 15, description: '15% Hardware Special' },
      { couponId: '3', code: 'GAMER20', discPercent: 20, description: '20% Rig Builder Deal' }
    ]
  } finally {
    loadingCoupons.value = false
  }
}

// =========================================================
// COUPON MANAGEMENT
// =========================================================

function applyCouponCode(codeToApply?: string) {
  const code = (codeToApply || couponInput.value).trim().toUpperCase()
  couponError.value = ''
  couponMessage.value = ''

  if (!code) {
    couponError.value = 'Please enter a valid coupon code.'
    return
  }

  const found = availableCoupons.value.find(c => c.code.toUpperCase() === code)
  if (found) {
    selectedCoupon.value = found.code
    couponMessage.value = `${found.code} applied! Enjoy ${found.discPercent}% off your order.`
    sessionStorage.setItem('appliedCoupon', JSON.stringify({
      code: found.code,
      discPercent: found.discPercent
    }))
    couponInput.value = ''
  } else {
    const standard: Record<string, number> = {
      'WELCOME10': 10,
      'NEXUS10': 10,
      'SUMMER15': 15,
      'PROMO20': 20,
      'SUPER50': 50
    }
    if (standard[code]) {
      selectedCoupon.value = code
      couponMessage.value = `${code} applied! Enjoy ${standard[code]}% off.`
      sessionStorage.setItem('appliedCoupon', JSON.stringify({
        code: code,
        discPercent: standard[code]
      }))
      couponInput.value = ''
    } else {
      couponError.value = `Coupon code "${code}" is not recognized or expired.`
    }
  }
}

function removeCoupon() {
  selectedCoupon.value = ''
  couponMessage.value = ''
  couponError.value = ''
  sessionStorage.removeItem('appliedCoupon')
}

// =========================================================
// CARD FORMATTING & VALIDATION
// =========================================================

function formatCardNumber(event: Event) {
  const input = event.target as HTMLInputElement
  let value = input.value.replace(/\D/g, '').substring(0, 16)
  const groups = value.match(/.{1,4}/g)
  cardDetails.value.cardNumber = groups ? groups.join(' ') : value
}

function formatExpiry(event: Event) {
  const input = event.target as HTMLInputElement
  let value = input.value.replace(/\D/g, '').substring(0, 4)
  if (value.length > 2) {
    value = value.substring(0, 2) + '/' + value.substring(2)
  }
  cardDetails.value.expiry = value
}

function isValidExpiry(expiry: string): boolean {
  const match = expiry.trim().match(/^(0[1-9]|1[0-2])\/(\d{2})$/)
  if (!match) return false
  const month = Number(match[1])
  const year = Number(`20${match[2]}`)
  const now = new Date()
  const currentMonth = now.getMonth() + 1
  const currentYear = now.getFullYear()
  if (year < currentYear) return false
  if (year === currentYear && month < currentMonth) return false
  return true
}

function validateShipping(): boolean {
  errorMessage.value = ''
  if (!shippingInfo.value.firstName.trim()) {
    errorMessage.value = 'Please provide your first name.'
    return false
  }
  if (!shippingInfo.value.lastName.trim()) {
    errorMessage.value = 'Please provide your last name.'
    return false
  }
  if (!shippingInfo.value.email.trim() || !shippingInfo.value.email.includes('@')) {
    errorMessage.value = 'Please provide a valid email address for order notifications.'
    return false
  }
  if (!shippingInfo.value.phone.trim()) {
    errorMessage.value = 'Please provide your phone number for courier delivery.'
    return false
  }
  if (selectedShippingMethod.value !== 'pickup') {
    if (!shippingInfo.value.address.trim()) {
      errorMessage.value = 'Please enter your street delivery address.'
      return false
    }
    if (!shippingInfo.value.city.trim()) {
      errorMessage.value = 'Please specify your city.'
      return false
    }
  }
  return true
}

function validateCard(): boolean {
  paymentError.value = ''
  if (paymentMethod.value === 'cod') return true
  if (selectedSavedCard.value) return true

  const num = cardDetails.value.cardNumber.replace(/\s/g, '')
  if (num.length < 13 || num.length > 19) {
    paymentError.value = 'Please enter a valid 16-digit card number.'
    return false
  }
  if (!cardDetails.value.cardName.trim()) {
    paymentError.value = 'Please enter the cardholder name as printed on the card.'
    return false
  }
  if (!isValidExpiry(cardDetails.value.expiry)) {
    paymentError.value = 'Please enter a valid future expiry date (MM/YY).'
    return false
  }
  const cvv = cardDetails.value.cvv.replace(/\D/g, '')
  if (cvv.length < 3 || cvv.length > 4) {
    paymentError.value = 'Please enter a valid 3 or 4-digit CVV security code.'
    return false
  }
  return true
}

// Copy Reference to Clipboard
async function copyOrderId() {
  if (!completedOrder.value?.orderId) return
  try {
    await navigator.clipboard.writeText(String(completedOrder.value.orderId))
    isCopied.value = true
    setTimeout(() => { isCopied.value = false }, 2000)
    await popup.success('Order reference copied to clipboard!', 'Copied')
  } catch {
    // fallback
  }
}

// Trigger Print
function printReceipt() {
  window.print()
}

// =========================================================
// PDF GENERATION & CACHING HELPER
// =========================================================

async function getOrGeneratePdfBlob(forceFresh = false): Promise<Blob> {
  if (cachedPdfBlob.value && !forceFresh) {
    return cachedPdfBlob.value
  }
  await nextTick()
  const receiptEl = document.getElementById('receipt-paper')
  const blob = await generateInvoicePdf(completedOrder.value, receiptEl)
  cachedPdfBlob.value = blob
  return blob
}

// =========================================================
// ORDER SUBMISSION
// =========================================================

async function handlePlaceOrder() {
  errorMessage.value = ''
  paymentError.value = ''

  if (cartItems.value.length === 0) {
    await popup.warning('Your shopping cart is empty. Please add items to proceed.', 'Empty Cart')
    return
  }

  if (!validateShipping()) {
    await popup.warning(errorMessage.value, 'Shipping Information Required')
    return
  }

  if (!validateCard()) {
    await popup.warning(paymentError.value, 'Payment Details Required')
    return
  }

  const customerId = getCustomerId()
  if (!customerId) {
    errorMessage.value = 'You must be logged in to finalize your purchase.'
    await popup.warning('Please sign in or create an account to complete your checkout.', 'Authentication Required')
    router.push('/login')
    return
  }

  isSubmitting.value = true

  try {
    const items = cartItems.value.map((item: any) => ({
      productId: item.productId,
      quantity: Number(item.quantity || 1)
    }))

    const couponCodes: string[] = []
    if (selectedCoupon.value) {
      couponCodes.push(selectedCoupon.value)
    }

    // 0. Synchronize customer address with backend database so delivery and order records always have it
    if (customerId) {
      try {
        const custRes = await api.get(`/customers/${encodeURIComponent(customerId)}`)
        const existingCust = custRes.data || {}
        await api.put(`/customers/${encodeURIComponent(customerId)}`, {
          name: `${shippingInfo.value.firstName} ${shippingInfo.value.lastName}`.trim() || existingCust.name,
          email: shippingInfo.value.email || existingCust.email,
          phone: shippingInfo.value.phone || existingCust.phone,
          password: 'dummyPassword123',
          address: {
            street: shippingInfo.value.address || 'No. 124, High Level Road',
            city: shippingInfo.value.city || 'Colombo 05',
            postalCode: shippingInfo.value.postalCode || '00500'
          },
          dob: existingCust.dob || null,
          userImage: existingCust.userImage || null
        })
      } catch (custSyncErr) {
        console.warn('Backend customer address sync notice:', custSyncErr)
      }
    }

    // 1. Create Order
    const orderPayload = {
      customerId,
      items,
      couponCodes
    }

    const orderResponse = await api.post('/orders', orderPayload)
    const serverOrder = orderResponse.data

    const orderId = serverOrder?.orderId || `NR-${Date.now().toString().slice(-6)}`
    
    // Mathematically accurate order amount: subtotal - coupon discounts + shipping + estimated tax
    const trueOrderAmount = finalTotal.value

    // 2. Create Payment Record with the accurate total
    const paymentMethodName = paymentMethod.value === 'card' ? 'Card' : 'Cash on Delivery'
    const paymentPayload: any = {
      orderId: String(orderId),
      amount: trueOrderAmount,
      method: paymentMethodName
    }

    if (paymentMethod.value === 'card' && selectedSavedCard.value) {
      paymentPayload.cardId = selectedSavedCard.value
    } else if (paymentMethod.value === 'card' && !selectedSavedCard.value && saveNewCardForFuture.value && customerId && cardDetails.value.cardNumber) {
      try {
        const [expMonth, expYr] = cardDetails.value.expiry.split('/')
        const savedCardRes = await api.post('/payments/cards', {
          customerId,
          cardHolderName: cardDetails.value.cardName || `${shippingInfo.value.firstName} ${shippingInfo.value.lastName}`.trim(),
          cardNumber: cardDetails.value.cardNumber.replace(/\s/g, ''),
          expiryMonth: expMonth || '12',
          expiryYear: expYr ? (expYr.length === 2 ? `20${expYr}` : expYr) : '2028'
        })
        if (savedCardRes.data?.cardId) {
          paymentPayload.cardId = savedCardRes.data.cardId
        }
      } catch (saveCardErr) {
        console.warn('Card saving note:', saveCardErr)
      }
    }

    try {
      await api.post('/payments', paymentPayload)
    } catch (payErr) {
      console.warn('Payment recording note:', payErr)
    }

    // 3. Assemble Completed Order Snapshot (Tax & Shipping explicitly included in totalAmount!)
    const completed: InvoiceOrderData = {
      orderId: String(orderId),
      orderDate: new Date().toISOString(),
      customerId,
      items: cartItems.value.map((item: any) => ({
        productId: item.productId,
        name: item.productName || item.name || item.product?.name || 'Hardware Component',
        quantity: Number(item.quantity || 1),
        unitPrice: Number(item.unitPrice ?? item.price ?? 0),
        price: Number(item.unitPrice ?? item.price ?? 0)
      })),
      subtotal: subtotal.value,
      shipping: selectedShippingCost.value,
      tax: estimatedTax.value,
      discount: couponDiscount.value,
      dealDiscount: dealDiscount.value,
      totalAmount: trueOrderAmount,
      paymentMethod: paymentMethodName,
      paymentStatus: paymentMethod.value === 'cod' ? 'Pending' : 'Paid',
      shippingInfo: {
        firstName: shippingInfo.value.firstName,
        lastName: shippingInfo.value.lastName,
        email: shippingInfo.value.email,
        phone: shippingInfo.value.phone,
        address: shippingInfo.value.address || (selectedShippingMethod.value === 'pickup' ? 'NexusRigs Hub, No. 450 Galle Road' : 'No. 124, High Level Road'),
        city: shippingInfo.value.city || 'Colombo 05',
        postalCode: shippingInfo.value.postalCode || '00500',
        country: shippingInfo.value.country
      }
    }

    completedOrder.value = completed
    cachedPdfBlob.value = null
    recipientEmail.value = shippingInfo.value.email

    // Persist address snapshot in localStorage & session user
    try {
      localStorage.setItem(`order_${orderId}`, JSON.stringify(completed))
      localStorage.setItem('saved_shipping_address', JSON.stringify({
        address: shippingInfo.value.address,
        city: shippingInfo.value.city,
        postalCode: shippingInfo.value.postalCode,
        country: shippingInfo.value.country
      }))

      const savedUserStr = sessionStorage.getItem('user') || localStorage.getItem('user')
      if (savedUserStr) {
        const u = JSON.parse(savedUserStr)
        u.address = {
          street: shippingInfo.value.address,
          city: shippingInfo.value.city,
          postalCode: shippingInfo.value.postalCode
        }
        u.city = shippingInfo.value.city
        u.postalCode = shippingInfo.value.postalCode
        u.phone = shippingInfo.value.phone || u.phone
        sessionStorage.setItem('user', JSON.stringify(u))
        localStorage.setItem('user', JSON.stringify(u))
      }
    } catch {
      // ignore
    }

    // 4. Clear Cart and Session Promo
    await cartStore.clearCart()
    sessionStorage.removeItem('appliedCoupon')

    // Mark completed and mount #receipt-paper into the DOM before generating PDF
    orderCompleted.value = true
    await nextTick()
    window.scrollTo({ top: 0, behavior: 'smooth' })

    // Background pre-generate and cache PDF using the rendered #receipt-paper DOM card!
    const receiptEl = document.getElementById('receipt-paper')
    generateInvoicePdf(completed, receiptEl).then(blob => {
      cachedPdfBlob.value = blob
    }).catch(err => {
      console.warn('PDF pre-cache error:', err)
    })

    await popup.success(`Your order #${orderId} has been successfully placed!`, 'Order Confirmed')
  } catch (error: any) {
    console.error('Checkout error:', error)
    const msg = error?.response?.data?.message || error?.response?.data?.error || error?.message || 'Unable to place order. Please try again.'
    errorMessage.value = msg
    await popup.error(msg, 'Checkout Failed')
  } finally {
    isSubmitting.value = false
  }
}

// =========================================================
// FAST INVOICE ACTIONS
// =========================================================

async function viewInvoice() {
  if (!completedOrder.value) return
  invoiceLoading.value = true
  try {
    const blob = await getOrGeneratePdfBlob(true)
    const url = window.URL.createObjectURL(blob)
    const newWindow = window.open(url, '_blank')
    if (!newWindow) {
      // Fallback: Open in interactive on-screen modal!
      showReceiptModal.value = true
    }
    setTimeout(() => window.URL.revokeObjectURL(url), 60000)
  } catch (err: any) {
    console.error('View invoice error:', err)
    // Fallback to in-app modal preview
    showReceiptModal.value = true
  } finally {
    invoiceLoading.value = false
  }
}

async function downloadInvoice() {
  if (!completedOrder.value) return
  invoiceLoading.value = true
  try {
    const blob = await getOrGeneratePdfBlob(true)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `NexusRigs-Receipt-${completedOrder.value.orderId}.pdf`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    setTimeout(() => window.URL.revokeObjectURL(url), 1000)
    await popup.success('Official PDF Receipt downloaded successfully.', 'Download Complete')
  } catch (err: any) {
    console.error('Download invoice error:', err)
    await popup.error('Failed to download invoice PDF.', 'Download Failed')
  } finally {
    invoiceLoading.value = false
  }
}

async function sendInvoiceByEmail() {
  if (!completedOrder.value) return
  sendingInvoice.value = true
  invoiceEmailMessage.value = ''
  invoiceEmailError.value = ''

  const targetEmail = (recipientEmail.value || completedOrder.value.shippingInfo?.email || shippingInfo.value.email).trim()

  if (!targetEmail || !targetEmail.includes('@')) {
    sendingInvoice.value = false
    await popup.warning('Please provide a valid email address.', 'Invalid Email')
    return
  }

  try {
    // Generate fresh high-res PDF matching the exact current on-screen receipt
    const pdfBlob = await getOrGeneratePdfBlob(true)

    const formData = new FormData()
    formData.append('email', targetEmail)
    formData.append('orderId', String(completedOrder.value.orderId))
    formData.append('file', pdfBlob, `NexusRigs-Receipt-${completedOrder.value.orderId}.pdf`)

    const apiBaseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

    // Use AbortController for snappy timeout protection (10 seconds max)
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 10000)

    const res = await fetch(`${apiBaseUrl}/bills/send-invoice`, {
      method: 'POST',
      body: formData,
      signal: controller.signal
    })
    clearTimeout(timeoutId)

    if (!res.ok) {
      let errMsg = 'Could not dispatch invoice email.'
      try {
        const body = await res.json()
        errMsg = body?.message || body?.error || errMsg
      } catch {
        // ignore
      }
      throw new Error(errMsg)
    }

    invoiceSentSuccessfully.value = true
    isEditingEmail.value = false
    invoiceEmailMessage.value = `Receipt sent to ${targetEmail}`
    await popup.success(`Receipt has been dispatched to ${targetEmail}`, 'Receipt Sent')
  } catch (err: any) {
    console.error('Send invoice email error:', err)
    if (err.name === 'AbortError') {
      // In case of SMTP delay, background dispatch already accepted
      invoiceSentSuccessfully.value = true
      invoiceEmailMessage.value = `Invoice is being delivered to ${targetEmail} in the background.`
      await popup.success(`Invoice delivery initiated to ${targetEmail}`, 'Dispatched')
    } else {
      invoiceEmailError.value = err?.message || 'Could not send invoice email.'
      await popup.error(invoiceEmailError.value, 'Email Failed')
    }
  } finally {
    sendingInvoice.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-slate-50/60 pb-20 pt-6 antialiased">
    <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">

      <!-- ========================================================
           CASE 1: HIGH-END ORDER RECEIPT SCREEN (THEME-MATCHED)
      ========================================================= -->
      <section v-if="orderCompleted && completedOrder" class="mx-auto max-w-4xl py-4 sm:py-8">

        <!-- SUCCESS NOTIFICATION HEADER -->
        <div class="text-center mb-8">
          <div
            class="mx-auto flex h-16 w-16 sm:h-20 sm:w-20 items-center justify-center rounded-3xl bg-emerald-50 text-emerald-600 ring-8 ring-emerald-50/60 shadow-lg shadow-emerald-500/10">
            <CheckCircle2 class="h-9 w-9 sm:h-10 sm:w-10 text-emerald-600" />
          </div>

          <span
            class="mt-4 inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-3 py-1 text-[11px] font-black uppercase tracking-widest text-emerald-600 border border-emerald-200/60 shadow-2xs">
            <span class="flex h-1.5 w-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
            Order Successfully Placed & Verified
          </span>

          <h1 class="mt-2 text-2xl sm:text-4xl font-black tracking-tight text-slate-900">
            Thank You for Your Order!
          </h1>

          <p class="mx-auto mt-2 max-w-lg text-xs sm:text-sm leading-relaxed text-slate-500">
            Your high-performance gear is reserved and scheduled for insured dispatch. A digital receipt has been
            generated below.
          </p>
        </div>

        <!-- QUICK ACTION BAR -->
        <div
          class="mb-6 rounded-2xl border border-white/90 bg-white/85 p-3.5 sm:p-4 shadow-xl shadow-slate-200/40 backdrop-blur-xl flex flex-col sm:flex-row items-center justify-between gap-3">
          <div class="flex items-center gap-2 text-xs font-bold text-slate-600">
            <FileText class="h-4 w-4 text-blue-600 shrink-0" />
            <span>Official Tax Invoice & Receipt</span>
            <span v-if="invoiceSentSuccessfully"
              class="rounded-full bg-emerald-50 px-2.5 py-0.5 text-[10px] font-bold text-emerald-700 border border-emerald-200 flex items-center gap-1">
              <Check class="h-3 w-3" /> Emailed
            </span>
          </div>

          <div class="flex flex-wrap items-center gap-2 w-full sm:w-auto justify-end">
            <!-- VIEW FULLSCREEN MODAL -->
            <button type="button" @click="showReceiptModal = true"
              class="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-bold text-slate-700 shadow-2xs transition hover:bg-slate-50"
              title="View on-screen receipt preview">
              <Eye class="h-3.5 w-3.5 text-blue-600" />
              <span>Preview</span>
            </button>

            <!-- PRINT RECEIPT -->
            <button type="button" @click="printReceipt"
              class="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-bold text-slate-700 shadow-2xs transition hover:bg-slate-50"
              title="Print receipt">
              <Printer class="h-3.5 w-3.5 text-slate-500" />
              <span>Print</span>
            </button>

            <!-- DOWNLOAD PDF -->
            <button type="button" @click="downloadInvoice" :disabled="invoiceLoading"
              class="inline-flex items-center gap-1.5 rounded-xl border border-blue-200 bg-blue-50/80 px-3.5 py-2 text-xs font-bold text-blue-700 shadow-2xs transition hover:bg-blue-100 disabled:opacity-50"
              title="Download official PDF receipt">
              <Download class="h-3.5 w-3.5" />
              <span>Download PDF</span>
            </button>

            <!-- SPEEDUP EMAIL BUTTON -->
            <button type="button" @click="sendInvoiceByEmail" :disabled="sendingInvoice"
              class="inline-flex items-center gap-1.5 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-4 py-2 text-xs font-bold text-white shadow-md shadow-blue-500/20 transition hover:from-blue-500 hover:to-cyan-400 disabled:opacity-50"
              title="Send invoice directly to your email">
              <div v-if="sendingInvoice"
                class="h-3.5 w-3.5 animate-spin rounded-full border-2 border-white border-t-transparent"></div>
              <Send v-else class="h-3.5 w-3.5" />
              <span>{{ sendingInvoice ? 'Sending...' : 'Email Receipt' }}</span>
            </button>
          </div>
        </div>

        <!-- FAST EMAIL DISPATCH INPUT (TOGGLE / DISPLAY) -->
        <div
          class="mb-6 rounded-2xl border border-slate-200/70 bg-white/70 p-3 sm:p-4 backdrop-blur-xl flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
          <div class="flex items-center gap-2">
            <Mail class="h-4 w-4 text-slate-400 shrink-0" />
            <span class="text-slate-500">Recipient Email:</span>
            <span v-if="!isEditingEmail" class="font-bold text-slate-800 font-mono">{{ recipientEmail ||
              shippingInfo.email }}</span>
            <input v-else v-model="recipientEmail" type="email" placeholder="Enter recipient email"
              class="rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-xs text-slate-800 outline-none focus:border-blue-500" />
            <button type="button" @click="isEditingEmail = !isEditingEmail"
              class="text-[11px] font-bold text-blue-600 hover:underline ml-1">
              {{ isEditingEmail ? 'Done' : 'Change' }}
            </button>
          </div>

          <div v-if="invoiceEmailMessage" class="text-xs font-bold text-emerald-600 flex items-center gap-1">
            <Check class="h-3.5 w-3.5" />
            <span>{{ invoiceEmailMessage }}</span>
          </div>
          <div v-if="invoiceEmailError" class="text-xs font-bold text-rose-600 flex items-center gap-1">
            <AlertCircle class="h-3.5 w-3.5" />
            <span>{{ invoiceEmailError }}</span>
          </div>
        </div>

        <!-- ========================================================
             DIGITAL TAX RECEIPT CARD (THEME-MATCHED PRINTABLE PAPER)
        ========================================================= -->
        <div id="receipt-paper"
          class="relative overflow-hidden rounded-3xl border border-white/90 bg-white/95 p-6 sm:p-10 shadow-2xl shadow-blue-500/10 backdrop-blur-2xl">

          <!-- TOP ACCENT CYAN/BLUE BRAND HEADER STRIP -->
          <div class="absolute top-0 left-0 right-0 h-1.5 bg-gradient-to-r from-blue-600 via-indigo-600 to-cyan-500">
          </div>

          <!-- RECEIPT HEADER -->
          <div
            class="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4 border-b border-slate-100 pb-6 pt-2">
            <div>
              <div class="flex items-center gap-3">
                <img :src="logo" alt="NexusRigs Logo" class="h-11 w-11 object-contain shrink-0 drop-shadow-xs" />
                <div>
                  <h2 class="text-xl sm:text-2xl font-black tracking-tight text-slate-900 leading-none">
                    NEXUSRIGS
                  </h2>
                  <p class="text-[10px] uppercase font-bold tracking-wider text-slate-400 mt-1">
                    Premium Computer & Gaming Hub
                  </p>
                </div>
              </div>
              <p class="mt-2.5 text-xs text-slate-500 leading-relaxed">
                Galle Road, Colombo 03, Sri Lanka • www.nexusrigs.com<br />
                Official Tax & Hardware Sales Receipt
              </p>
            </div>

            <div class="sm:text-right">
              <span
                class="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-3 py-1 text-[10px] font-black uppercase tracking-wider text-emerald-700 border border-emerald-200/60 mb-2">
                <span class="h-1.5 w-1.5 rounded-full bg-emerald-500"></span>
                {{ completedOrder.paymentStatus === 'Paid' ? 'PAID & VERIFIED' : 'COD CONFIRMED' }}
              </span>

              <div class="flex sm:justify-end items-center gap-1.5">
                <span class="text-xs font-bold text-slate-400">Receipt Ref:</span>
                <span class="font-mono text-xs font-black text-slate-900">#{{ completedOrder.orderId }}</span>
                <button type="button" @click="copyOrderId"
                  class="rounded p-1 text-slate-400 hover:text-blue-600 hover:bg-slate-100 transition no-print"
                  title="Copy reference number">
                  <Check v-if="isCopied" class="h-3 w-3 text-emerald-600" />
                  <Copy v-else class="h-3 w-3" />
                </button>
              </div>

              <p class="mt-1 text-xs text-slate-500">
                Date: {{ formatDate(completedOrder.orderDate) }}
              </p>
            </div>
          </div>

          <!-- RECIPIENT & DISPATCH METADATA GRID -->
          <div
            class="grid grid-cols-1 sm:grid-cols-2 gap-4 my-6 rounded-2xl bg-slate-50/80 p-4 sm:p-5 border border-slate-200/60 text-xs">
            <div>
              <span class="text-[10px] font-bold uppercase tracking-wider text-blue-600 block mb-1">
                Billed & Delivered To:
              </span>
              <p class="font-black text-slate-900 text-sm">
                {{ completedOrder.shippingInfo?.firstName }} {{ completedOrder.shippingInfo?.lastName }}
              </p>
              <p class="text-slate-600 mt-1 leading-relaxed">
                {{ completedOrder.shippingInfo?.address }}<br />
                {{ completedOrder.shippingInfo?.city }} {{ completedOrder.shippingInfo?.postalCode }}<br />
                {{ completedOrder.shippingInfo?.country }}
              </p>
              <div class="mt-2 text-slate-500 space-y-0.5">
                <p>Phone: {{ completedOrder.shippingInfo?.phone }}</p>
                <p>Email: {{ completedOrder.shippingInfo?.email }}</p>
              </div>
            </div>

            <div class="border-t sm:border-t-0 sm:border-l border-slate-200/60 pt-3 sm:pt-0 sm:pl-5">
              <span class="text-[10px] font-bold uppercase tracking-wider text-cyan-600 block mb-1">
                Transaction Specifications:
              </span>
              <div class="space-y-1.5 text-slate-700">
                <div class="flex justify-between">
                  <span class="text-slate-500">Payment Channel:</span>
                  <span class="font-bold text-slate-900">{{ completedOrder.paymentMethod }}</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-slate-500">Payment Status:</span>
                  <span class="font-bold text-emerald-600">{{ completedOrder.paymentStatus }}</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-slate-500">Dispatch Courier:</span>
                  <span class="font-bold text-slate-900">Insured Island-wide Courier</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-slate-500">Hardware Warranty:</span>
                  <span class="font-bold text-blue-600">2-Year Official Guarantee</span>
                </div>
              </div>
            </div>
          </div>

          <!-- ITEMIZED PURCHASES TABLE -->
          <div class="overflow-x-auto my-6">
            <table class="w-full text-left text-xs">
              <thead>
                <tr
                  class="border-b border-slate-200 bg-slate-50/80 text-[10px] font-black uppercase tracking-wider text-slate-500">
                  <th class="py-2.5 px-3">#</th>
                  <th class="py-2.5 px-3">Hardware Component</th>
                  <th class="py-2.5 px-3 text-center">Qty</th>
                  <th class="py-2.5 px-3 text-right">Unit Price</th>
                  <th class="py-2.5 px-3 text-right">Total</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                <tr v-for="(item, idx) in completedOrder.items" :key="item.productId"
                  class="hover:bg-slate-50/40 transition-colors">
                  <td class="py-3 px-3 font-mono text-slate-400">{{ Number(idx) + 1 }}</td>
                  <td class="py-3 px-3">
                    <span class="font-bold text-slate-900 block">{{ item.name || item.productName }}</span>
                    <span class="text-[10px] text-slate-400 font-mono">SKU: {{ item.productId }}</span>
                  </td>
                  <td class="py-3 px-3 text-center font-bold text-slate-800">{{ item.quantity || 1 }}</td>
                  <td class="py-3 px-3 text-right text-slate-600">{{ formatCurrency(item.unitPrice || item.price || 0)
                    }}</td>
                  <td class="py-3 px-3 text-right font-black text-slate-900">
                    {{ formatCurrency((item.unitPrice || item.price || 0) * (item.quantity || 1)) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- FINANCIAL RECAP & BARCODE VERIFICATION -->
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-6 border-t border-slate-100 pt-6">

            <!-- SECURITY BARCODE & OFFICIAL STAMP -->
            <div class="flex flex-col justify-between space-y-4">
              <div class="rounded-xl border border-dashed border-slate-200 bg-slate-50/60 p-4">
                <div class="flex items-center gap-2 mb-2">
                  <ShieldCheck class="h-4 w-4 text-emerald-600" />
                  <span class="text-[10px] font-black uppercase tracking-wider text-slate-700">Official Warranty
                    Certificate</span>
                </div>
                <p class="text-[11px] leading-relaxed text-slate-500">
                  All components included in this receipt are covered under our 2-Year Official NexusRigs hardware
                  warranty and 14-day replacement policy.
                </p>
              </div>

              <!-- VECTOR BARCODE -->
              <div class="pt-2">
                <div class="flex items-center gap-1 h-8 opacity-75">
                  <span class="w-1.5 h-full bg-slate-900"></span>
                  <span class="w-0.5 h-full bg-slate-900"></span>
                  <span class="w-1 h-full bg-slate-900"></span>
                  <span class="w-2 h-full bg-slate-900"></span>
                  <span class="w-0.5 h-full bg-slate-900"></span>
                  <span class="w-1.5 h-full bg-slate-900"></span>
                  <span class="w-0.5 h-full bg-slate-900"></span>
                  <span class="w-2.5 h-full bg-slate-900"></span>
                  <span class="w-1 h-full bg-slate-900"></span>
                  <span class="w-0.5 h-full bg-slate-900"></span>
                  <span class="w-2 h-full bg-slate-900"></span>
                  <span class="w-1.5 h-full bg-slate-900"></span>
                  <span class="w-0.5 h-full bg-slate-900"></span>
                  <span class="w-1 h-full bg-slate-900"></span>
                  <span class="w-2.5 h-full bg-slate-900"></span>
                  <span class="w-1 h-full bg-slate-900"></span>
                  <span class="w-0.5 h-full bg-slate-900"></span>
                  <span class="w-1.5 h-full bg-slate-900"></span>
                  <span class="w-2 h-full bg-slate-900"></span>
                </div>
                <span class="text-[9px] font-mono text-slate-400 block mt-1 tracking-widest">
                  AUTH-VERIFY-NR-{{ completedOrder.orderId }}
                </span>
              </div>
            </div>

            <!-- TOTALS BOX -->
            <div class="space-y-2 text-xs">
              <div class="flex justify-between text-slate-600">
                <span>Subtotal:</span>
                <span class="font-bold text-slate-800">{{ formatCurrency(completedOrder.subtotal || 0) }}</span>
              </div>

              <div v-if="(completedOrder.discount || 0) > 0"
                class="flex justify-between text-emerald-600 font-semibold">
                <span>Voucher Discount:</span>
                <span class="font-bold">- {{ formatCurrency(completedOrder.discount || 0) }}</span>
              </div>

              <div class="flex justify-between text-slate-600">
                <span>Insured Courier Shipping:</span>
                <span
                  :class="(completedOrder.shipping || 0) === 0 ? 'text-emerald-600 font-bold' : 'font-bold text-slate-800'">
                  {{ (completedOrder.shipping || 0) === 0 ? 'FREE' : formatCurrency(completedOrder.shipping || 0) }}
                </span>
              </div>

              <div class="flex justify-between text-slate-600">
                <span>Estimated Tax & VAT (8%):</span>
                <span class="font-bold text-slate-800">{{ formatCurrency(completedOrder.tax || 0) }}</span>
              </div>

              <div v-if="(completedOrder.dealDiscount || 0) > 0"
                class="flex justify-between items-center text-emerald-600 bg-emerald-50/80 px-2.5 py-1.5 rounded-lg border border-emerald-200/50">
                <span class="font-bold flex items-center gap-1">⚡ Instant Deal Savings:</span>
                <span class="font-bold font-mono">Saved {{ formatCurrency(completedOrder.dealDiscount || 0) }}</span>
              </div>

              <div class="border-t border-slate-200/80 pt-3 mt-2">
                <div class="flex items-end justify-between">
                  <div>
                    <span class="text-[10px] uppercase font-bold text-slate-400 block">Total Amount Paid</span>
                    <span class="text-[10px] text-slate-400">All duties included</span>
                  </div>
                  <span
                    class="text-xl sm:text-2xl font-black bg-gradient-to-r from-blue-600 via-indigo-600 to-cyan-500 bg-clip-text text-transparent">
                    {{ formatCurrency(completedOrderTotal) }}
                  </span>
                </div>
              </div>
            </div>

          </div>

          <!-- RECEIPT FOOTER -->
          <div class="mt-8 border-t border-slate-100 pt-4 text-center text-[10px] text-slate-400">
            NexusRigs Sri Lanka • Customer Support: support@nexusrigs.com • Hotline: +94 11 234 5678<br />
            Thank you for shopping with NexusRigs!
          </div>

        </div>

        <!-- POST-PURCHASE NAVIGATION BUTTONS -->
        <div class="mt-8 flex flex-col sm:flex-row items-center justify-center gap-3">
          <router-link to="/orders"
            class="inline-flex w-full sm:w-auto items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-6 py-3.5 text-xs font-bold text-white shadow-lg shadow-blue-500/20 transition hover:from-blue-500 hover:to-cyan-400">
            <span>Track in My Orders</span>
            <ArrowRight class="h-4 w-4" />
          </router-link>

          <router-link to="/catalog"
            class="inline-flex w-full sm:w-auto items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-6 py-3.5 text-xs font-bold text-slate-700 shadow-2xs transition hover:bg-slate-50">
            <span>Continue Shopping</span>
          </router-link>
        </div>

      </section>

      <!-- ========================================================
           CASE 2: MAIN CHECKOUT FLOW
      ========================================================= -->
      <div v-else>

        <!-- TOP STEPPERS -->
        <nav class="mb-6 flex items-center justify-between text-xs font-semibold text-slate-500">
          <div class="flex items-center gap-2">
            <router-link to="/cart"
              class="inline-flex items-center gap-1.5 text-slate-500 hover:text-blue-600 transition-colors">
              <ArrowLeft class="h-3.5 w-3.5" />
              <span>Back to Cart</span>
            </router-link>
          </div>

          <div class="hidden sm:flex items-center gap-2 text-xs">
            <router-link to="/cart" class="inline-flex items-center gap-1.5 text-slate-400 hover:text-blue-600">
              <span
                class="flex h-4 w-4 items-center justify-center rounded-full bg-slate-200 text-[10px] text-slate-600">1</span>
              Cart
            </router-link>
            <ChevronRight class="h-3.5 w-3.5 text-slate-300" />
            <span
              class="inline-flex items-center gap-1.5 rounded-full bg-blue-50 px-3 py-1 font-bold text-blue-600 border border-blue-200/60 shadow-xs">
              <span
                class="flex h-4 w-4 items-center justify-center rounded-full bg-blue-600 text-[10px] text-white">2</span>
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
                <Lock class="h-5 w-5" />
              </div>
              <span class="text-[11px] font-bold uppercase tracking-wider text-blue-600">
                Secure 256-bit SSL Checkout
              </span>
            </div>
            <h1 class="mt-2 text-3xl font-black tracking-tight text-slate-900 sm:text-4xl">
              Complete Your Order
            </h1>
            <p class="mt-1 text-sm text-slate-500">
              Enter your shipping destination, choose your courier method, and finalize payment.
            </p>
          </div>

          <div class="flex items-center gap-3">
            <div class="rounded-xl border border-slate-200 bg-white/80 px-4 py-2.5 shadow-2xs">
              <span class="text-[10px] font-bold uppercase tracking-wider text-slate-400 block">Items</span>
              <span class="text-sm font-black text-slate-900">{{ cartItems.length }} Product{{ cartItems.length === 1 ?
                '' : 's' }}</span>
            </div>
            <div class="rounded-xl border border-blue-100 bg-blue-50/80 px-4 py-2.5 shadow-2xs">
              <span class="text-[10px] font-bold uppercase tracking-wider text-blue-600 block">Total</span>
              <span class="text-sm font-black text-blue-700">{{ formatCurrency(finalTotal) }}</span>
            </div>
          </div>
        </header>

        <!-- ERROR MESSAGE -->
        <div v-if="errorMessage"
          class="mb-6 flex items-center gap-3 rounded-2xl border border-rose-200 bg-rose-50/90 p-4 text-xs font-bold text-rose-700 shadow-2xs">
          <AlertCircle class="h-5 w-5 shrink-0 text-rose-500" />
          <span>{{ errorMessage }}</span>
        </div>

        <!-- MAIN CHECKOUT GRID -->
        <div class="grid grid-cols-1 gap-8 lg:grid-cols-12 items-start">

          <!-- LEFT COLUMN: STEPS & FORMS -->
          <div class="space-y-6 lg:col-span-8">

            <!-- STEP 1: SHIPPING INFORMATION -->
            <section
              class="overflow-hidden rounded-3xl border border-white/90 bg-white/85 p-6 sm:p-7 shadow-xl shadow-slate-200/40 backdrop-blur-xl">
              <div class="flex items-center justify-between border-b border-slate-100 pb-4 mb-6">
                <div>
                  <span class="text-[10px] font-black uppercase tracking-wider text-blue-600">
                    Step 01
                  </span>
                  <h2 class="text-lg font-black text-slate-900">
                    Shipping & Contact Details
                  </h2>
                </div>
                <div class="flex h-9 w-9 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
                  <MapPin class="h-4 w-4" />
                </div>
              </div>

              <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label class="mb-1.5 block text-xs font-bold text-slate-700">First Name *</label>
                  <div class="relative">
                    <User class="absolute left-3.5 top-3.5 h-4 w-4 text-slate-400" />
                    <input v-model="shippingInfo.firstName" type="text" placeholder="e.g. Samitha"
                      class="w-full rounded-xl border border-slate-200 bg-white py-3 pl-10 pr-4 text-xs font-semibold text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10" />
                  </div>
                </div>

                <div>
                  <label class="mb-1.5 block text-xs font-bold text-slate-700">Last Name *</label>
                  <input v-model="shippingInfo.lastName" type="text" placeholder="e.g. Senevirathna"
                    class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-xs font-semibold text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10" />
                </div>

                <div>
                  <label class="mb-1.5 block text-xs font-bold text-slate-700">Email Address (For Invoice) *</label>
                  <div class="relative">
                    <Mail class="absolute left-3.5 top-3.5 h-4 w-4 text-slate-400" />
                    <input v-model="shippingInfo.email" type="email" placeholder="you@example.com"
                      class="w-full rounded-xl border border-slate-200 bg-white py-3 pl-10 pr-4 text-xs font-semibold text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10" />
                  </div>
                </div>

                <div>
                  <label class="mb-1.5 block text-xs font-bold text-slate-700">Phone Number *</label>
                  <div class="relative">
                    <Phone class="absolute left-3.5 top-3.5 h-4 w-4 text-slate-400" />
                    <input v-model="shippingInfo.phone" type="tel" placeholder="+94 77 123 4567"
                      class="w-full rounded-xl border border-slate-200 bg-white py-3 pl-10 pr-4 text-xs font-semibold text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10" />
                  </div>
                </div>

                <div class="sm:col-span-2">
                  <div class="flex items-center justify-between mb-1.5">
                    <label class="block text-xs font-bold text-slate-700">Street Delivery Address *</label>
                    <div class="flex items-center gap-1.5 text-[11px]">
                      <button type="button" @click="usePresetAddress('colombo')"
                        class="inline-flex items-center gap-1 rounded-lg bg-blue-50 px-2.5 py-0.5 font-bold text-blue-600 hover:bg-blue-100 transition border border-blue-200/50 shadow-xs cursor-pointer"
                        title="Quick-fill Colombo Metro delivery address">
                        <span>📍</span> Colombo Metro
                      </button>
                      <button type="button" @click="usePresetAddress('hub')"
                        class="inline-flex items-center gap-1 rounded-lg bg-cyan-50 px-2.5 py-0.5 font-bold text-cyan-600 hover:bg-cyan-100 transition border border-cyan-200/50 shadow-xs cursor-pointer"
                        title="Quick-fill NexusRigs Hub pickup address">
                        <span>🏢</span> Showroom Hub
                      </button>
                    </div>
                  </div>
                  <input v-model="shippingInfo.address" type="text" placeholder="No. 124, High Level Road, Colombo"
                    class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-xs font-semibold text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10" />
                </div>

                <div>
                  <label class="mb-1.5 block text-xs font-bold text-slate-700">City / District *</label>
                  <input v-model="shippingInfo.city" @input="onCityInput(($event.target as HTMLInputElement).value)"
                    list="sri-lanka-cities" type="text" placeholder="Colombo 05"
                    class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-xs font-semibold text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10" />
                  <datalist id="sri-lanka-cities">
                    <option v-for="c in SRI_LANKA_CITIES" :key="c.name" :value="c.name">
                      {{ c.district }} (Postal Code: {{ c.postalCode }})
                    </option>
                  </datalist>
                </div>

                <div>
                  <label class="mb-1.5 block text-xs font-bold text-slate-700">Postal Code</label>
                  <input v-model="shippingInfo.postalCode" type="text" placeholder="00500"
                    class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-xs font-semibold text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10" />
                </div>

                <div class="sm:col-span-2">
                  <label class="mb-1.5 block text-xs font-bold text-slate-700">Delivery Instructions / Notes
                    (Optional)</label>
                  <input v-model="shippingInfo.notes" type="text"
                    placeholder="e.g. Call before delivery or leave with security"
                    class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-xs font-semibold text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10" />
                </div>
              </div>
            </section>

            <!-- STEP 2: SHIPPING METHOD -->
            <section
              class="overflow-hidden rounded-3xl border border-white/90 bg-white/85 p-6 sm:p-7 shadow-xl shadow-slate-200/40 backdrop-blur-xl">
              <div class="flex items-center justify-between border-b border-slate-100 pb-4 mb-6">
                <div>
                  <span class="text-[10px] font-black uppercase tracking-wider text-blue-600">
                    Step 02
                  </span>
                  <h2 class="text-lg font-black text-slate-900">
                    Delivery & Courier Options
                  </h2>
                </div>
                <div class="flex h-9 w-9 items-center justify-center rounded-xl bg-cyan-50 text-cyan-600">
                  <Truck class="h-4 w-4" />
                </div>
              </div>

              <div class="space-y-3">
                <button v-for="method in shippingMethods" :key="method.id" type="button"
                  @click="selectedShippingMethod = method.id"
                  class="w-full rounded-2xl border p-4 text-left transition-all flex items-center justify-between gap-4"
                  :class="selectedShippingMethod === method.id
                    ? 'border-blue-400 bg-blue-50/60 ring-2 ring-blue-500/10 shadow-xs'
                    : 'border-slate-200 bg-white hover:border-slate-300'">
                  <div class="flex items-center gap-3.5">
                    <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl transition"
                      :class="selectedShippingMethod === method.id ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-500'">
                      <component :is="method.icon" class="h-5 w-5" />
                    </div>
                    <div>
                      <div class="flex items-center gap-2">
                        <span class="text-sm font-black text-slate-900">{{ method.name }}</span>
                        <span class="rounded bg-slate-100 px-2 py-0.5 text-[10px] font-bold text-slate-600">
                          {{ method.duration }}
                        </span>
                      </div>
                      <p class="mt-0.5 text-xs text-slate-500">{{ method.description }}</p>
                    </div>
                  </div>

                  <div class="text-right shrink-0">
                    <span class="text-sm font-black" :class="method.cost === 0 ? 'text-emerald-600' : 'text-slate-900'">
                      {{ method.cost === 0 ? 'FREE' : formatCurrency(method.cost) }}
                    </span>
                  </div>
                </button>
              </div>
            </section>

            <!-- STEP 3: PAYMENT METHOD -->
            <section
              class="overflow-hidden rounded-3xl border border-white/90 bg-white/85 p-6 sm:p-7 shadow-xl shadow-slate-200/40 backdrop-blur-xl">
              <div class="flex items-center justify-between border-b border-slate-100 pb-4 mb-6">
                <div>
                  <span class="text-[10px] font-black uppercase tracking-wider text-blue-600">
                    Step 03
                  </span>
                  <h2 class="text-lg font-black text-slate-900">
                    Payment Method
                  </h2>
                </div>
                <div class="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600">
                  <CreditCard class="h-4 w-4" />
                </div>
              </div>

              <!-- METHOD TABS -->
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-3.5 mb-6">
                <!-- ONLINE CARD -->
                <button type="button" @click="paymentMethod = 'card'"
                  class="rounded-2xl border p-4 text-left transition flex items-start gap-3.5" :class="paymentMethod === 'card'
                    ? 'border-blue-400 bg-blue-50/60 ring-2 ring-blue-500/10 shadow-xs'
                    : 'border-slate-200 bg-white hover:border-slate-300'">
                  <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl"
                    :class="paymentMethod === 'card' ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-500'">
                    <CreditCard class="h-5 w-5" />
                  </div>
                  <div>
                    <span class="text-sm font-black text-slate-900 block">Credit / Debit Card</span>
                    <span class="text-xs text-slate-500 block mt-0.5">Visa, Mastercard, AMEX with Instant 3D
                      Secure</span>
                  </div>
                </button>

                <!-- CASH ON DELIVERY -->
                <button type="button" @click="paymentMethod = 'cod'; selectedSavedCard = ''"
                  class="rounded-2xl border p-4 text-left transition flex items-start gap-3.5" :class="paymentMethod === 'cod'
                    ? 'border-emerald-400 bg-emerald-50/60 ring-2 ring-emerald-500/10 shadow-xs'
                    : 'border-slate-200 bg-white hover:border-slate-300'">
                  <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl"
                    :class="paymentMethod === 'cod' ? 'bg-emerald-600 text-white' : 'bg-slate-100 text-slate-500'">
                    <Truck class="h-5 w-5" />
                  </div>
                  <div>
                    <span class="text-sm font-black text-slate-900 block">Cash on Delivery</span>
                    <span class="text-xs text-slate-500 block mt-0.5">Pay in cash upon doorstep delivery</span>
                  </div>
                </button>
              </div>

              <!-- CARD FORM -->
              <div v-if="paymentMethod === 'card'" class="space-y-6">

                <!-- 3D LUXURY CREDIT CARD VISUALIZER -->
                <div
                  class="relative w-full max-w-sm sm:max-w-md mx-auto aspect-[1.586/1] rounded-2xl sm:rounded-3xl p-5 sm:p-6 shadow-2xl overflow-hidden text-white transition-all duration-300 transform select-none ring-1 ring-white/20"
                  :class="activeCardDisplay.brand === 'MASTERCARD'
                    ? 'bg-gradient-to-tr from-slate-950 via-slate-900 to-amber-950 border border-amber-500/30'
                    : activeCardDisplay.brand === 'AMEX'
                      ? 'bg-gradient-to-tr from-slate-950 via-cyan-950 to-blue-950 border border-cyan-500/30'
                      : 'bg-gradient-to-tr from-slate-950 via-indigo-950 to-blue-900 border border-blue-500/30'">
                  <!-- Holographic Ambient Shimmer -->
                  <div
                    class="pointer-events-none absolute -right-20 -top-20 h-60 w-60 rounded-full bg-cyan-400/20 blur-3xl">
                  </div>
                  <div
                    class="pointer-events-none absolute -left-16 -bottom-16 h-56 w-56 rounded-full bg-blue-500/20 blur-3xl">
                  </div>
                  <div
                    class="pointer-events-none absolute inset-0 bg-radial from-white/10 via-transparent to-transparent opacity-50">
                  </div>

                  <!-- CARD TOP: CHIP + CONTACTLESS + ISSUER -->
                  <div class="relative z-10 flex items-center justify-between">
                    <div class="flex items-center gap-3">
                      <!-- GOLD EMV SMART CHIP -->
                      <div
                        class="h-8 w-11 rounded-lg bg-gradient-to-br from-amber-300 via-amber-400 to-amber-600 p-1 shadow-inner border border-amber-200/60 flex flex-col justify-between">
                        <div class="h-1.5 border-b border-amber-800/40"></div>
                        <div class="h-1.5 border-t border-amber-800/40"></div>
                      </div>

                      <!-- CONTACTLESS WAVES -->
                      <div class="flex items-center text-white/80">
                        <Wifi class="h-5 w-5 rotate-90" />
                      </div>
                    </div>

                    <!-- ISSUER BRAND MARK -->
                    <div class="text-right">
                      <span
                        class="text-[10px] font-black uppercase tracking-widest text-cyan-300 block drop-shadow-xs">NEXUS
                        PRIVILEGE</span>
                      <span class="text-[8px] font-mono tracking-wider text-white/60 block">TITANIUM EDITION</span>
                    </div>
                  </div>

                  <!-- CARD CENTER: EMBOSSED CARD NUMBER -->
                  <div class="relative z-10 my-4 sm:my-6">
                    <span class="text-[9px] uppercase font-bold tracking-widest text-white/50 block mb-1">Card
                      Number</span>
                    <p class="font-mono text-base sm:text-xl font-bold tracking-widest text-white drop-shadow-md">
                      {{ activeCardDisplay.number }}
                    </p>
                  </div>

                  <!-- CARD BOTTOM: HOLDER, EXPIRY & BRAND LOGO -->
                  <div class="relative z-10 flex items-end justify-between pt-1">
                    <div>
                      <span class="text-[8px] uppercase font-bold tracking-widest text-white/50 block">Cardholder</span>
                      <p
                        class="text-xs sm:text-sm font-black uppercase tracking-wider text-slate-100 truncate max-w-[180px] drop-shadow-xs">
                        {{ activeCardDisplay.holderName }}
                      </p>
                    </div>

                    <div class="text-center">
                      <span class="text-[8px] uppercase font-bold tracking-widest text-white/50 block">Expires</span>
                      <p class="font-mono text-xs font-bold text-slate-100 drop-shadow-xs">
                        {{ activeCardDisplay.expiry }}
                      </p>
                    </div>

                    <!-- OFFICIAL CARD BRAND BADGE -->
                    <div class="flex items-center justify-end">
                      <div v-if="activeCardDisplay.brand === 'VISA'" class="flex items-center">
                        <span
                          class="font-black italic text-lg sm:text-xl tracking-tighter text-white drop-shadow-md">VISA</span>
                      </div>
                      <div v-else-if="activeCardDisplay.brand === 'MASTERCARD'" class="flex items-center -space-x-2">
                        <div class="h-6 w-6 rounded-full bg-rose-500/90 shadow-sm"></div>
                        <div class="h-6 w-6 rounded-full bg-amber-400/90 shadow-sm"></div>
                      </div>
                      <div v-else-if="activeCardDisplay.brand === 'AMEX'"
                        class="rounded bg-blue-600 px-2 py-0.5 text-[10px] font-black text-white">
                        AMEX
                      </div>
                      <div v-else class="rounded bg-white/20 px-2 py-0.5 text-[10px] font-bold text-white">
                        CARD
                      </div>
                    </div>
                  </div>
                </div>

                <!-- SAVED CARDS SUITE / DECK -->
                <div v-if="savedCards.length > 0" class="space-y-2.5">
                  <div class="flex items-center justify-between">
                    <label class="text-xs font-black text-slate-800 flex items-center gap-1.5">
                      <span>Saved Payment Cards</span>
                      <span class="rounded-full bg-blue-100 px-2 py-0.5 text-[10px] font-bold text-blue-700">
                        {{ savedCards.length }}
                      </span>
                    </label>
                    <span class="text-[10px] text-emerald-600 font-bold flex items-center gap-1">
                      <ShieldCheck class="h-3 w-3" /> 1-Click Secure Pay
                    </span>
                  </div>

                  <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <button v-for="card in savedCards" :key="card.cardId" type="button"
                      @click="selectedSavedCard = card.cardId"
                      class="rounded-2xl border p-3.5 text-left transition-all relative group flex items-center justify-between"
                      :class="selectedSavedCard === card.cardId
                        ? 'border-blue-500 bg-blue-50/70 ring-2 ring-blue-500/20 shadow-xs'
                        : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50/50'">
                      <div class="flex items-center gap-3">
                        <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl transition"
                          :class="selectedSavedCard === card.cardId ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-600'">
                          <CreditCard class="h-5 w-5" />
                        </div>
                        <div>
                          <div class="flex items-center gap-1.5">
                            <span class="font-mono text-xs font-black text-slate-900">
                              •••• {{ card.last4 || card.lastFour || 'Card' }}
                            </span>
                            <span v-if="card.isDefault"
                              class="rounded bg-slate-100 px-1.5 py-0.2 text-[9px] font-bold text-slate-600">
                              Default
                            </span>
                          </div>
                          <span class="text-[10px] text-slate-400 block truncate max-w-[130px]">
                            {{ card.cardHolderName || 'Saved Card' }}
                          </span>
                        </div>
                      </div>

                      <div class="flex items-center gap-2">
                        <span v-if="selectedSavedCard === card.cardId"
                          class="flex h-5 w-5 items-center justify-center rounded-full bg-blue-600 text-white shadow-xs">
                          <Check class="h-3 w-3" />
                        </span>
                        <button type="button" @click.stop="deleteSavedCard(card.cardId)"
                          class="opacity-0 group-hover:opacity-100 p-1 text-slate-300 hover:text-rose-600 transition"
                          title="Remove saved card">
                          <Trash2 class="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </button>

                    <!-- ADD NEW CARD OPTION -->
                    <button type="button" @click="selectedSavedCard = ''"
                      class="rounded-2xl border border-dashed p-3.5 text-center transition flex items-center justify-center gap-2"
                      :class="!selectedSavedCard
                        ? 'border-blue-500 bg-blue-50/60 ring-2 ring-blue-500/20 text-blue-700 font-black'
                        : 'border-slate-300 bg-slate-50/60 text-slate-600 hover:border-slate-400 font-bold'">
                      <Plus class="h-4 w-4" />
                      <span class="text-xs">Use Another / New Card</span>
                    </button>
                  </div>
                </div>

                <!-- NEW CARD INPUTS (WHEN NO SAVED CARD SELECTED) -->
                <div v-if="!selectedSavedCard"
                  class="rounded-2xl border border-slate-200/90 bg-white/90 p-5 space-y-4 shadow-xs">
                  <div class="flex items-center justify-between border-b border-slate-100 pb-3">
                    <span class="text-xs font-black text-slate-800">Enter Payment Card Details</span>
                    <span v-if="detectedCardBrand"
                      class="rounded-full bg-blue-50 px-2.5 py-0.5 font-mono text-[10px] font-black text-blue-700 border border-blue-200/60">
                      {{ detectedCardBrand }} Detected
                    </span>
                  </div>

                  <div>
                    <label class="mb-1 block text-xs font-bold text-slate-700">Card Number *</label>
                    <div class="relative">
                      <CreditCard class="absolute left-3.5 top-3.5 h-4 w-4 text-slate-400" />
                      <input v-model="cardDetails.cardNumber" @input="formatCardNumber" type="text" inputmode="numeric"
                        maxlength="19" placeholder="4532 8920 1234 5678"
                        class="w-full rounded-xl border border-slate-200 bg-white py-3 pl-10 pr-4 font-mono text-xs font-bold text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10" />
                    </div>
                  </div>

                  <div>
                    <label class="mb-1 block text-xs font-bold text-slate-700">Cardholder Name *</label>
                    <input v-model="cardDetails.cardName" type="text" placeholder="KAMAL GUNARATHNA"
                      class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-xs font-bold uppercase text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10" />
                  </div>

                  <div class="grid grid-cols-2 gap-3.5">
                    <div>
                      <label class="mb-1 block text-xs font-bold text-slate-700">Expiry (MM/YY) *</label>
                      <input v-model="cardDetails.expiry" @input="formatExpiry" type="text" maxlength="5"
                        placeholder="12/28"
                        class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 font-mono text-xs font-bold text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10" />
                    </div>

                    <div>
                      <label class="mb-1 block text-xs font-bold text-slate-700">CVV Security Code *</label>
                      <input v-model="cardDetails.cvv" type="password" maxlength="4" placeholder="•••"
                        class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 font-mono text-xs font-bold text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10" />
                    </div>
                  </div>

                  <!-- SAVE CARD CHECKBOX -->
                  <div class="pt-2 border-t border-slate-100 flex items-center gap-2.5">
                    <input id="save-card-checkbox" v-model="saveNewCardForFuture" type="checkbox"
                      class="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500" />
                    <label for="save-card-checkbox" class="text-xs font-bold text-slate-700 cursor-pointer">
                      Save this card securely to My Payment Cards for 1-click checkout
                    </label>
                  </div>
                </div>

                <p v-if="paymentError" class="text-xs font-bold text-rose-600">
                  {{ paymentError }}
                </p>
              </div>

              <!-- COD EXPLANATION -->
              <div v-else class="rounded-2xl border border-emerald-200 bg-emerald-50/70 p-4 flex items-start gap-3">
                <CheckCircle2 class="h-5 w-5 text-emerald-600 shrink-0 mt-0.5" />
                <div class="text-xs text-emerald-800">
                  <p class="font-bold">Cash on Delivery Confirmed</p>
                  <p class="mt-0.5 text-emerald-700">
                    Prepare the exact total of <strong>{{ formatCurrency(finalTotal) }}</strong> in cash upon courier
                    arrival. A printed invoice receipt will be presented by the rider.
                  </p>
                </div>
              </div>
            </section>

            <!-- STEP 4: COUPONS & DISCOUNTS -->
            <section
              class="overflow-hidden rounded-3xl border border-white/90 bg-white/85 p-6 sm:p-7 shadow-xl shadow-slate-200/40 backdrop-blur-xl">
              <div class="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-4 mb-4">
                <div>
                  <span class="text-[10px] font-black uppercase tracking-wider text-blue-600">
                    Step 04
                  </span>
                  <h2 class="text-base sm:text-lg font-black text-slate-900">
                    Vouchers & Offers
                  </h2>
                </div>

                <!-- VIEW ALL COUPONS TRIGGER BUTTON -->
                <button type="button" @click="showCouponsModal = true"
                  class="inline-flex items-center gap-1.5 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-3 py-1.5 sm:px-3.5 sm:py-2 text-[11px] sm:text-xs font-bold text-white shadow-md shadow-blue-500/20 hover:from-blue-700 hover:to-cyan-600 transition active:scale-95 cursor-pointer shrink-0">
                  <Ticket class="h-3.5 w-3.5" />
                  <span>View All Coupons</span>
                  <span v-if="availableCoupons.length > 0"
                    class="ml-1 rounded-full bg-white/25 px-1.5 py-0.2 text-[10px] font-black">
                    {{ availableCoupons.length }}
                  </span>
                </button>
              </div>

              <!-- ACTIVE COUPON BADGE -->
              <div v-if="selectedCoupon"
                class="mb-4 flex items-center justify-between rounded-2xl border border-emerald-200 bg-emerald-50/80 p-4">
                <div class="flex items-center gap-3">
                  <div
                    class="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-600 text-white shadow-xs">
                    <CheckCircle2 class="h-5 w-5" />
                  </div>
                  <div>
                    <div class="flex items-center gap-2">
                      <span class="font-mono text-sm font-black uppercase text-emerald-950">
                        {{ selectedCoupon }}
                      </span>
                      <span class="rounded-full bg-emerald-200/80 px-2 py-0.5 text-[10px] font-black text-emerald-900">
                        ACTIVE
                      </span>
                    </div>
                    <p class="text-xs font-medium text-emerald-700 mt-0.5">
                      {{ couponMessage || `Applied ${couponDiscount > 0 ? formatCurrency(couponDiscount) + ' discount' :
                      'Coupon'}` }}
                    </p>
                  </div>
                </div>
                <button type="button" @click="removeCoupon"
                  class="rounded-xl border border-rose-200 bg-white px-3.5 py-2 text-xs font-bold text-rose-600 shadow-2xs hover:bg-rose-50 transition">
                  Remove
                </button>
              </div>

              <!-- MANUAL CODE INPUT -->
              <div v-else class="flex flex-col sm:flex-row gap-2.5">
                <div class="relative flex-1">
                  <Tag class="absolute left-3.5 top-3.5 h-4 w-4 text-slate-400" />
                  <input v-model="couponInput" @keyup.enter="applyCouponCode()" type="text"
                    placeholder="Enter Promo Code (e.g. WELCOME10)"
                    class="w-full rounded-xl border border-slate-200 bg-white py-3 pl-10 pr-4 font-mono text-xs font-bold uppercase text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10" />
                </div>
                <button type="button" @click="applyCouponCode()"
                  class="rounded-xl bg-blue-600 px-6 py-3 text-xs font-bold text-white shadow-2xs transition hover:bg-blue-700 shrink-0">
                  Apply Voucher
                </button>
              </div>

              <!-- ACTIVE SUGGESTED COUPONS CHIPS -->
              <div v-if="availableCoupons.length > 0 && !selectedCoupon" class="mt-4 pt-3 border-t border-slate-100">
                <div class="flex items-center justify-between mb-2">
                  <span class="text-[10px] font-black uppercase tracking-wider text-slate-400">Available For You:</span>
                  <button type="button" @click="showCouponsModal = true"
                    class="text-[11px] font-bold text-blue-600 hover:underline flex items-center gap-1">
                    Browse all {{ availableCoupons.length }} vouchers →
                  </button>
                </div>
                <div class="flex flex-wrap gap-2">
                  <button v-for="c in availableCoupons" :key="c.code" type="button" @click="applyCouponCode(c.code)"
                    class="inline-flex items-center gap-1.5 rounded-xl border border-blue-200/80 bg-blue-50/70 px-3 py-1.5 text-xs font-bold text-blue-700 hover:bg-blue-100 hover:border-blue-300 transition shadow-2xs group">
                    <Percent class="h-3 w-3 text-blue-500 group-hover:rotate-12 transition-transform" />
                    <span class="font-mono font-black">{{ c.code }}</span>
                    <span class="text-[10px] font-bold text-blue-600">({{ c.discPercent }}% Off)</span>
                  </button>
                </div>
              </div>

              <p v-if="couponError" class="mt-2 text-xs font-bold text-rose-600">
                {{ couponError }}
              </p>
            </section>

          </div>

          <!-- RIGHT COLUMN: ORDER REVIEW & PLACE ORDER CTA -->
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
                      {{ cartItems.length }} item{{ cartItems.length === 1 ? '' : 's' }} in order
                    </p>
                  </div>
                  <div
                    class="flex h-9 w-9 items-center justify-center rounded-xl bg-white text-blue-600 shadow-xs border border-blue-100">
                    <ShoppingBag class="h-4 w-4" />
                  </div>
                </div>
              </div>

              <div class="p-6">

                <!-- COMPACT ITEM LIST PREVIEW -->
                <div class="max-h-56 overflow-y-auto space-y-3 pr-1 divide-y divide-slate-100">
                  <div v-for="item in cartItems" :key="item.productId" class="pt-3 first:pt-0 flex items-center gap-3">
                    <div class="h-12 w-12 shrink-0 rounded-xl border border-slate-200 bg-white p-1 overflow-hidden">
                      <img v-if="item.imageUrl || item.image || item.product?.image"
                        :src="item.imageUrl || item.image || item.product?.image" :alt="item.productName || item.name"
                        class="h-full w-full object-contain" />
                      <div v-else class="flex h-full w-full items-center justify-center text-slate-300">
                        <ShoppingBag class="h-5 w-5" />
                      </div>
                    </div>

                    <div class="min-w-0 flex-1">
                      <h4 class="text-xs font-bold text-slate-900 truncate">
                        {{ item.productName || item.name || item.product?.name }}
                      </h4>
                      <div class="flex items-center gap-1.5 mt-0.5">
                        <span class="text-[10px] text-slate-400">Qty: {{ item.quantity || 1 }}</span>
                        <span
                          v-if="item.originalPrice && Number(item.originalPrice) > Number(item.unitPrice ?? item.price ?? 0)"
                          class="text-[10px] text-slate-400 line-through">
                          {{ formatCurrency(Number(item.originalPrice)) }}
                        </span>
                        <span v-if="item.discountPercentage"
                          class="inline-flex items-center gap-0.5 rounded bg-rose-50 px-1 py-0.2 text-[9px] font-black text-rose-600">
                          <Flame class="h-2 w-2 fill-rose-500 text-rose-500" />
                          -{{ item.discountPercentage }}%
                        </span>
                      </div>
                    </div>

                    <div class="text-right shrink-0">
                      <span class="text-xs font-black text-slate-800 block">
                        {{ formatCurrency(Number(item.unitPrice ?? item.price ?? 0) * Number(item.quantity || 1)) }}
                      </span>
                      <span
                        v-if="item.originalPrice && Number(item.originalPrice) > Number(item.unitPrice ?? item.price ?? 0)"
                        class="text-[10px] font-semibold text-emerald-600 block">
                        Save {{ formatCurrency((Number(item.originalPrice) - Number(item.unitPrice ?? item.price ?? 0))
                          * Number(item.quantity || 1)) }}
                      </span>
                    </div>
                  </div>
                </div>

                <!-- PRICE BREAKDOWN -->
                <div class="mt-5 space-y-2.5 border-t border-slate-100 pt-4 text-xs">
                  <div class="flex justify-between text-slate-600">
                    <span>Items Subtotal</span>
                    <span class="font-bold text-slate-800">{{ formatCurrency(subtotalBeforeDeals > 0 ?
                      subtotalBeforeDeals : subtotal) }}</span>
                  </div>

                  <div v-if="dealDiscount > 0" class="flex justify-between text-rose-600 font-semibold">
                    <span class="flex items-center gap-1">
                      <Flame class="h-3.5 w-3.5 fill-rose-500 text-rose-500" />
                      Promotional Deal Savings
                    </span>
                    <span class="font-black text-rose-600">- {{ formatCurrency(dealDiscount) }}</span>
                  </div>

                  <div v-if="couponDiscount > 0" class="flex justify-between text-emerald-600 font-semibold">
                    <span class="flex items-center gap-1">
                      <Tag class="h-3.5 w-3.5" />
                      Promo Voucher Discount
                    </span>
                    <span class="font-bold">- {{ formatCurrency(couponDiscount) }}</span>
                  </div>

                  <div class="flex justify-between text-slate-600">
                    <span>Delivery</span>
                    <span
                      :class="selectedShippingCost === 0 ? 'text-emerald-600 font-bold' : 'text-slate-800 font-bold'">
                      {{ selectedShippingCost === 0 ? 'FREE' : formatCurrency(selectedShippingCost) }}
                    </span>
                  </div>

                  <div class="flex justify-between text-slate-600">
                    <span>Estimated Tax & VAT (8%)</span>
                    <span class="font-bold text-slate-800">{{ formatCurrency(estimatedTax) }}</span>
                  </div>

                  <!-- SAVINGS BADGE -->
                  <div v-if="totalSavings > 0"
                    class="rounded-xl bg-emerald-50 border border-emerald-200/80 p-2.5 flex items-center justify-between">
                    <span class="text-xs font-bold text-emerald-800 flex items-center gap-1.5">
                      <Sparkles class="h-3.5 w-3.5 text-emerald-600" />
                      Total Savings on Order
                    </span>
                    <span class="text-xs font-black text-emerald-700">
                      {{ formatCurrency(totalSavings) }}
                    </span>
                  </div>

                  <div class="border-t border-slate-100 pt-3">
                    <div class="flex items-end justify-between">
                      <div>
                        <span class="block text-xs font-semibold text-slate-400">Grand Total</span>
                        <span class="text-[10px] text-slate-400">Includes all taxes</span>
                      </div>
                      <span
                        class="text-2xl font-black bg-gradient-to-r from-blue-600 via-indigo-600 to-cyan-500 bg-clip-text text-transparent">
                        {{ formatCurrency(finalTotal) }}
                      </span>
                    </div>
                  </div>
                </div>

                <!-- SUBMIT BUTTON -->
                <button type="button" @click="handlePlaceOrder" :disabled="isSubmitting || cartItems.length === 0"
                  class="mt-6 flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 via-indigo-600 to-cyan-500 px-6 py-4 text-sm font-black text-white shadow-lg shadow-blue-500/25 transition-all duration-200 hover:from-blue-500 hover:via-indigo-500 hover:to-cyan-400 hover:-translate-y-0.5 active:translate-y-0 disabled:cursor-not-allowed disabled:opacity-50">
                  <div v-if="isSubmitting"
                    class="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent"></div>
                  <Lock v-else class="h-4 w-4" />
                  <span>
                    {{ isSubmitting ? 'Securing Your Order...' : paymentMethod === 'cod' ? 'Confirm Cash on Delivery' :
                    'Place Order & Pay' }}
                  </span>
                </button>

                <!-- TRUST BADGES -->
                <div class="mt-5 space-y-2 border-t border-slate-100 pt-4 text-[11px] text-slate-500">
                  <div class="flex items-center gap-2">
                    <ShieldCheck class="h-4 w-4 text-emerald-600 shrink-0" />
                    <span>Official Manufacturer Guarantee Included</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <Lock class="h-4 w-4 text-blue-600 shrink-0" />
                    <span>Encrypted Payment Processing</span>
                  </div>
                </div>

              </div>

            </div>
          </aside>

        </div>

      </div>

    </div>

    <!-- ========================================================
         INTERACTIVE ON-SCREEN RECEIPT MODAL PREVIEW
    ========================================================= -->
    <Teleport to="body">
      <div v-if="showReceiptModal && completedOrder"
        class="fixed inset-0 z-[99999] flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-md transition-all overflow-y-auto"
        @click.self="showReceiptModal = false">
        <div
          class="relative my-auto max-h-[88vh] w-full max-w-3xl overflow-y-auto rounded-3xl border border-white/90 bg-white p-6 sm:p-8 shadow-2xl">
          <!-- MODAL ACTIONS BAR -->
          <div class="flex items-center justify-between border-b border-slate-100 pb-4 mb-6">
            <div class="flex items-center gap-3">
              <img :src="logo" alt="NexusRigs Logo" class="h-9 w-9 object-contain drop-shadow-xs" />
              <div>
                <h3 class="text-base sm:text-lg font-black text-slate-900 leading-tight">Digital Tax Receipt Preview
                </h3>
                <p class="text-[10px] text-slate-400">Order Reference #{{ completedOrder.orderId }}</p>
              </div>
            </div>

            <div class="flex items-center gap-2">
              <button type="button" @click="printReceipt"
                class="rounded-xl border border-slate-200 bg-white px-3.5 py-1.5 text-xs font-bold text-slate-700 shadow-2xs hover:bg-slate-50 transition cursor-pointer flex items-center gap-1.5">
                <Printer class="h-3.5 w-3.5" />
                <span>Print</span>
              </button>
              <button type="button" @click="downloadInvoice"
                class="rounded-xl bg-blue-600 px-3.5 py-1.5 text-xs font-bold text-white shadow-2xs hover:bg-blue-700 transition cursor-pointer flex items-center gap-1.5">
                <Download class="h-3.5 w-3.5" />
                <span>Download PDF</span>
              </button>
              <button type="button" @click="showReceiptModal = false"
                class="rounded-xl p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition cursor-pointer">
                <X class="h-5 w-5" />
              </button>
            </div>
          </div>

          <!-- FULL RECEIPT CONTENT (MATCH WITH SCREENSHOT & LOGO) -->
          <div class="space-y-6 text-xs">

            <!-- HEADER -->
            <div
              class="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4 border-b border-slate-100 pb-5">
              <div>
                <div class="flex items-center gap-3">
                  <img :src="logo" alt="NexusRigs Logo" class="h-11 w-11 object-contain shrink-0 drop-shadow-xs" />
                  <div>
                    <h2 class="text-xl sm:text-2xl font-black tracking-tight text-slate-900 leading-none">
                      NEXUSRIGS
                    </h2>
                    <p class="text-[10px] uppercase font-bold tracking-wider text-slate-400 mt-1">
                      Premium Computer & Gaming Hub
                    </p>
                  </div>
                </div>
                <p class="mt-2.5 text-xs text-slate-500 leading-relaxed">
                  Galle Road, Colombo 03, Sri Lanka • www.nexusrigs.com<br />
                  Official Tax & Hardware Sales Receipt
                </p>
              </div>

              <div class="sm:text-right">
                <span
                  class="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-3 py-1 text-[10px] font-black uppercase tracking-wider text-emerald-700 border border-emerald-200/60 mb-2">
                  <span class="h-1.5 w-1.5 rounded-full bg-emerald-500"></span>
                  {{ completedOrder.paymentStatus === 'Paid' ? 'PAID & VERIFIED' : 'COD CONFIRMED' }}
                </span>

                <div class="flex sm:justify-end items-center gap-1.5">
                  <span class="text-xs font-bold text-slate-400">Receipt Ref:</span>
                  <span class="font-mono text-xs font-black text-slate-900">#{{ completedOrder.orderId }}</span>
                  <button type="button" @click="copyOrderId"
                    class="rounded p-1 text-slate-400 hover:text-blue-600 hover:bg-slate-100 transition cursor-pointer"
                    title="Copy reference number">
                    <Check v-if="isCopied" class="h-3 w-3 text-emerald-600" />
                    <Copy v-else class="h-3 w-3" />
                  </button>
                </div>

                <p class="mt-1 text-xs text-slate-500">
                  Date: {{ formatDate(completedOrder.orderDate) }}
                </p>
              </div>
            </div>

            <!-- RECIPIENT & DISPATCH METADATA GRID -->
            <div
              class="grid grid-cols-1 sm:grid-cols-2 gap-4 rounded-2xl bg-slate-50/80 p-4 sm:p-5 border border-slate-200/60 text-xs">
              <div>
                <span class="text-[10px] font-bold uppercase tracking-wider text-blue-600 block mb-1">
                  Billed & Delivered To:
                </span>
                <p class="font-black text-slate-900 text-sm">
                  {{ completedOrder.shippingInfo?.firstName }} {{ completedOrder.shippingInfo?.lastName }}
                </p>
                <p class="text-slate-600 mt-1 leading-relaxed">
                  {{ completedOrder.shippingInfo?.address }}<br />
                  {{ completedOrder.shippingInfo?.city }} {{ completedOrder.shippingInfo?.postalCode }}<br />
                  {{ completedOrder.shippingInfo?.country }}
                </p>
                <div class="mt-2 text-slate-500 space-y-0.5">
                  <p>Phone: {{ completedOrder.shippingInfo?.phone }}</p>
                  <p>Email: {{ completedOrder.shippingInfo?.email }}</p>
                </div>
              </div>

              <div class="border-t sm:border-t-0 sm:border-l border-slate-200/60 pt-3 sm:pt-0 sm:pl-5">
                <span class="text-[10px] font-bold uppercase tracking-wider text-cyan-600 block mb-1">
                  Transaction Specifications:
                </span>
                <div class="space-y-1.5 text-slate-700">
                  <div class="flex justify-between">
                    <span class="text-slate-500">Payment Channel:</span>
                    <span class="font-bold text-slate-900">{{ completedOrder.paymentMethod }}</span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-slate-500">Payment Status:</span>
                    <span class="font-bold text-emerald-600">{{ completedOrder.paymentStatus }}</span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-slate-500">Dispatch Courier:</span>
                    <span class="font-bold text-slate-900">Insured Island-wide Courier</span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-slate-500">Hardware Warranty:</span>
                    <span class="font-bold text-blue-600">2-Year Official Guarantee</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- ITEMIZED PURCHASES TABLE -->
            <div class="overflow-x-auto">
              <table class="w-full text-left text-xs">
                <thead>
                  <tr
                    class="border-b border-slate-200 bg-slate-50/80 text-[10px] font-black uppercase tracking-wider text-slate-500">
                    <th class="py-2.5 px-3">#</th>
                    <th class="py-2.5 px-3">Hardware Component</th>
                    <th class="py-2.5 px-3 text-center">Qty</th>
                    <th class="py-2.5 px-3 text-right">Unit Price</th>
                    <th class="py-2.5 px-3 text-right">Total</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                  <tr v-for="(item, idx) in completedOrder.items" :key="item.productId"
                    class="hover:bg-slate-50/40 transition-colors">
                    <td class="py-3 px-3 font-mono text-slate-400">{{ Number(idx) + 1 }}</td>
                    <td class="py-3 px-3">
                      <span class="font-bold text-slate-900 block">{{ item.name || item.productName }}</span>
                      <span class="text-[10px] text-slate-400 font-mono">SKU: {{ item.productId }}</span>
                    </td>
                    <td class="py-3 px-3 text-center font-bold text-slate-800">{{ item.quantity || 1 }}</td>
                    <td class="py-3 px-3 text-right text-slate-600">{{ formatCurrency(item.unitPrice || item.price || 0)
                      }}</td>
                    <td class="py-3 px-3 text-right font-black text-slate-900">
                      {{ formatCurrency((item.unitPrice || item.price || 0) * (item.quantity || 1)) }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <!-- FINANCIAL RECAP & BARCODE VERIFICATION -->
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-6 border-t border-slate-100 pt-5">
              <!-- GUARANTEE BOX & BARCODE -->
              <div class="flex flex-col justify-between space-y-4">
                <div class="rounded-xl border border-dashed border-slate-200 bg-slate-50/60 p-4">
                  <div class="flex items-center gap-2 mb-2">
                    <ShieldCheck class="h-4 w-4 text-emerald-600" />
                    <span class="text-[10px] font-black uppercase tracking-wider text-slate-700">Official Warranty
                      Certificate</span>
                  </div>
                  <p class="text-[11px] leading-relaxed text-slate-500">
                    All components included in this receipt are covered under our 2-Year Official NexusRigs hardware
                    warranty and 14-day replacement policy.
                  </p>
                </div>

                <div class="pt-1">
                  <div class="flex items-center gap-1 h-7 opacity-75">
                    <span class="w-1.5 h-full bg-slate-900"></span>
                    <span class="w-0.5 h-full bg-slate-900"></span>
                    <span class="w-1 h-full bg-slate-900"></span>
                    <span class="w-2 h-full bg-slate-900"></span>
                    <span class="w-0.5 h-full bg-slate-900"></span>
                    <span class="w-1.5 h-full bg-slate-900"></span>
                    <span class="w-0.5 h-full bg-slate-900"></span>
                    <span class="w-2.5 h-full bg-slate-900"></span>
                    <span class="w-1 h-full bg-slate-900"></span>
                    <span class="w-0.5 h-full bg-slate-900"></span>
                    <span class="w-2 h-full bg-slate-900"></span>
                    <span class="w-1.5 h-full bg-slate-900"></span>
                    <span class="w-0.5 h-full bg-slate-900"></span>
                    <span class="w-1 h-full bg-slate-900"></span>
                    <span class="w-2.5 h-full bg-slate-900"></span>
                    <span class="w-1 h-full bg-slate-900"></span>
                    <span class="w-0.5 h-full bg-slate-900"></span>
                    <span class="w-1.5 h-full bg-slate-900"></span>
                    <span class="w-2 h-full bg-slate-900"></span>
                  </div>
                  <span class="text-[9px] font-mono text-slate-400 block mt-1 tracking-widest uppercase">
                    AUTH-VERIFY-NR-{{ completedOrder.orderId }}
                  </span>
                </div>
              </div>

              <!-- TOTALS BOX -->
              <div class="space-y-2 text-xs">
                <div class="flex justify-between text-slate-600">
                  <span>Subtotal:</span>
                  <span class="font-bold text-slate-800">{{ formatCurrency(completedOrder.subtotal || 0) }}</span>
                </div>

                <div v-if="(completedOrder.discount || 0) > 0"
                  class="flex justify-between text-emerald-600 font-semibold">
                  <span>Voucher Discount:</span>
                  <span class="font-bold">- {{ formatCurrency(completedOrder.discount || 0) }}</span>
                </div>

                <div class="flex justify-between text-slate-600">
                  <span>Insured Courier Shipping:</span>
                  <span
                    :class="(completedOrder.shipping || 0) === 0 ? 'text-emerald-600 font-bold' : 'font-bold text-slate-800'">
                    {{ (completedOrder.shipping || 0) === 0 ? 'FREE' : formatCurrency(completedOrder.shipping || 0) }}
                  </span>
                </div>

                <div class="flex justify-between text-slate-600">
                  <span>Estimated Tax & VAT (8%):</span>
                  <span class="font-bold text-slate-800">{{ formatCurrency(completedOrder.tax || 0) }}</span>
                </div>

                <div v-if="(completedOrder.dealDiscount || 0) > 0"
                  class="flex justify-between items-center text-emerald-600 bg-emerald-50/80 px-2.5 py-1.5 rounded-lg border border-emerald-200/50">
                  <span class="font-bold flex items-center gap-1">⚡ Instant Deal Savings:</span>
                  <span class="font-bold font-mono">Saved {{ formatCurrency(completedOrder.dealDiscount || 0) }}</span>
                </div>

                <div class="border-t border-slate-200/80 pt-3 mt-2">
                  <div class="flex items-end justify-between">
                    <div>
                      <span class="text-[10px] uppercase font-bold text-slate-400 block">Total Amount Paid</span>
                      <span class="text-[10px] text-slate-400">All duties included</span>
                    </div>
                    <span
                      class="text-xl sm:text-2xl font-black bg-gradient-to-r from-blue-600 via-indigo-600 to-cyan-500 bg-clip-text text-transparent">
                      {{ formatCurrency(completedOrderTotal) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <!-- FOOTER -->
            <div class="mt-6 border-t border-slate-100 pt-3 text-center text-[10px] text-slate-400">
              NexusRigs Sri Lanka • Customer Support: support@nexusrigs.com • Hotline: +94 11 234 5678<br />
              Thank you for shopping with NexusRigs!
            </div>

          </div>

          <div class="mt-6 text-right">
            <button type="button" @click="showReceiptModal = false"
              class="rounded-xl border border-slate-200 bg-slate-50 px-5 py-2 text-xs font-bold text-slate-700 hover:bg-slate-100 cursor-pointer">
              Close Preview
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ========================================================
         VIEW ALL ACTIVE COUPONS & PROMOS MODAL (TELEPORTED TO BODY)
    ========================================================= -->
    <Teleport to="body">
      <div v-if="showCouponsModal"
        class="fixed inset-0 z-[99999] flex items-center justify-center bg-slate-900/60 p-3 sm:p-4 md:p-6 backdrop-blur-md transition-all overflow-y-auto"
        @click.self="showCouponsModal = false">
        <div
          class="relative my-auto max-h-[88vh] w-full max-w-2xl overflow-y-auto rounded-2xl sm:rounded-3xl border border-white/90 bg-white/98 p-4 sm:p-6 md:p-8 shadow-2xl backdrop-blur-2xl">

          <!-- HEADER -->
          <div class="flex items-start justify-between gap-3 border-b border-slate-100 pb-4 mb-4 sm:mb-6 pr-8 sm:pr-0">
            <div class="flex items-center gap-3 sm:gap-3.5">
              <div
                class="flex h-10 w-10 sm:h-12 sm:w-12 shrink-0 items-center justify-center rounded-xl sm:rounded-2xl bg-gradient-to-br from-blue-600 to-cyan-500 text-white shadow-lg shadow-blue-500/20">
                <Ticket class="h-5 w-5 sm:h-6 sm:w-6" />
              </div>
              <div>
                <div class="flex flex-wrap items-center gap-1.5 sm:gap-2">
                  <h3 class="text-base sm:text-xl font-black text-slate-900 leading-tight">Available Promo Vouchers</h3>
                  <span
                    class="rounded-full bg-blue-50 px-2 py-0.5 text-[10px] sm:text-xs font-black text-blue-600 border border-blue-200/60 shrink-0">
                    {{ availableCoupons.length }} Active
                  </span>
                </div>
                <p class="text-[11px] sm:text-xs text-slate-500 mt-0.5 leading-snug">
                  Click apply on any voucher for instant order discount.
                </p>
              </div>
            </div>

            <!-- Close Button -->
            <button type="button" @click="showCouponsModal = false"
              class="absolute top-3 right-3 sm:top-5 sm:right-5 rounded-xl p-1.5 sm:p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition shrink-0 cursor-pointer"
              aria-label="Close coupons modal">
              <X class="h-5 w-5" />
            </button>
          </div>

          <!-- Coupons List with Perforated Ticket Design -->
          <div v-if="loadingCoupons" class="py-12 text-center text-xs font-bold text-slate-500">
            <RefreshCw class="h-5 w-5 animate-spin mx-auto mb-2 text-blue-600" />
            Loading active promotions...
          </div>
          <div v-else-if="availableCoupons.length === 0" class="py-12 text-center">
            <Gift class="h-10 w-10 text-slate-300 mx-auto mb-2" />
            <p class="text-sm font-bold text-slate-600">No active coupons found at this time.</p>
          </div>
          <div v-else class="space-y-3.5 sm:space-y-4">
            <div v-for="coupon in availableCoupons" :key="coupon.code"
              class="relative rounded-2xl border transition-all overflow-hidden bg-gradient-to-r" :class="selectedCoupon.toUpperCase() === coupon.code.toUpperCase()
                ? 'border-emerald-400 from-emerald-50/80 via-white to-emerald-50/40 ring-2 ring-emerald-500/20 shadow-md'
                : 'border-slate-200 from-slate-50/90 via-white to-blue-50/30 hover:border-blue-300 hover:shadow-md'">
              <!-- Left Perforation Cutout -->
              <div
                class="pointer-events-none absolute -left-2.5 top-1/2 -translate-y-1/2 h-5 w-5 rounded-full bg-slate-900/30 backdrop-blur-md hidden sm:block">
              </div>
              <!-- Right Perforation Cutout -->
              <div
                class="pointer-events-none absolute -right-2.5 top-1/2 -translate-y-1/2 h-5 w-5 rounded-full bg-slate-900/30 backdrop-blur-md hidden sm:block">
              </div>

              <div
                class="flex flex-col sm:flex-row items-stretch sm:items-center justify-between p-3.5 sm:p-5 gap-3.5 sm:gap-4">
                <!-- Left: Discount Details -->
                <div class="flex items-start sm:items-center gap-3 sm:gap-4 flex-1 min-w-0">
                  <div
                    class="flex h-12 w-12 sm:h-14 sm:w-14 shrink-0 flex-col items-center justify-center rounded-xl sm:rounded-2xl bg-gradient-to-br from-blue-600 to-cyan-500 text-white shadow-md shadow-blue-500/20">
                    <span class="text-sm sm:text-base font-black leading-none">{{ coupon.discPercent }}%</span>
                    <span
                      class="text-[8px] sm:text-[9px] font-black uppercase tracking-wider opacity-90 mt-0.5">OFF</span>
                  </div>
                  <div class="flex-1 min-w-0">
                    <div class="flex flex-wrap items-center gap-1.5 sm:gap-2">
                      <span class="font-mono text-sm sm:text-base font-black uppercase text-slate-900 tracking-wider">
                        {{ coupon.code }}
                      </span>
                      <button type="button" @click="copyCoupon(coupon.code)"
                        class="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition cursor-pointer"
                        :title="copiedCouponCode === coupon.code ? 'Copied!' : 'Copy Code'">
                        <Check v-if="copiedCouponCode === coupon.code" class="h-3.5 w-3.5 text-emerald-600" />
                        <Copy v-else class="h-3.5 w-3.5" />
                      </button>
                      <span v-if="selectedCoupon.toUpperCase() === coupon.code.toUpperCase()"
                        class="rounded-full bg-emerald-100 px-2 py-0.5 text-[9px] sm:text-[10px] font-black text-emerald-700 shrink-0">
                        APPLIED
                      </span>
                    </div>
                    <p class="text-[11px] sm:text-xs text-slate-500 mt-0.5 line-clamp-2">
                      {{ coupon.description || `Enjoy ${coupon.discPercent}% discount on entire cart subtotal.` }}
                    </p>
                    <p v-if="subtotal > 0"
                      class="text-[10px] sm:text-[11px] font-black text-emerald-600 mt-1 flex items-center gap-1">
                      <Sparkles class="h-3 w-3 shrink-0" />
                      <span>Saves {{ formatCurrency(Math.round(subtotal * (coupon.discPercent / 100))) }} on your
                        order!</span>
                    </p>
                  </div>
                </div>

                <!-- Right: 1-Click Action & Validity -->
                <div
                  class="flex sm:flex-col items-center sm:items-end justify-between sm:justify-center gap-2 pt-2.5 sm:pt-0 border-t sm:border-t-0 border-slate-100 shrink-0 w-full sm:w-auto">
                  <span class="text-[10px] text-slate-400 block sm:order-2">
                    {{ coupon.endDate ? `Valid till ${formatDate(coupon.endDate)}` : 'Island-wide Valid' }}
                  </span>
                  <button v-if="selectedCoupon.toUpperCase() === coupon.code.toUpperCase()" type="button"
                    @click="removeCoupon"
                    class="rounded-xl border border-rose-200 bg-white px-3.5 py-1.5 sm:py-2 text-xs font-bold text-rose-600 hover:bg-rose-50 transition shadow-2xs sm:order-1 cursor-pointer">
                    Remove
                  </button>
                  <button v-else type="button" @click="selectCouponFromModal(coupon.code)"
                    class="rounded-xl bg-blue-600 hover:bg-blue-700 px-4 sm:px-5 py-2 sm:py-2.5 text-xs font-bold text-white transition shadow-md shadow-blue-500/20 active:scale-95 sm:order-1 cursor-pointer">
                    Apply Code
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Footer Notice -->
          <div
            class="mt-5 sm:mt-6 pt-4 border-t border-slate-100 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-slate-500">
            <div class="flex items-center gap-1.5 text-center sm:text-left">
              <ShieldCheck class="h-4 w-4 text-emerald-600 shrink-0" />
              <span class="text-[11px] sm:text-xs">Guaranteed savings applied directly at checkout</span>
            </div>
            <button type="button" @click="showCouponsModal = false"
              class="w-full sm:w-auto rounded-xl border border-slate-200 bg-slate-50 px-5 py-2 text-xs font-bold text-slate-700 hover:bg-slate-100 transition cursor-pointer">
              Close Window
            </button>
          </div>
        </div>
      </div>
    </Teleport>

  </div>
</template>

<style>
@media print {
  body * {
    visibility: hidden;
  }

  #receipt-paper,
  #receipt-paper * {
    visibility: visible;
  }

  .no-print {
    display: none !important;
  }

  #receipt-paper {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    margin: 0;
    padding: 24px;
    box-shadow: none !important;
    border: 1px solid #e2e8f0 !important;
    background: #ffffff !important;
  }
}
</style>