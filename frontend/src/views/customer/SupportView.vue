<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../../services/api'
import backgroundImage from '../../assets/images/vecteezy_abstract-blur-shopping-mall_2795585.jpg'
import {
  HelpCircle,
  MessageSquare,
  FileQuestion,
  LifeBuoy,
  Phone,
  Mail,
  Clock,
  CheckCircle2,
  AlertTriangle,
  Send,
  PlusCircle,
  Search,
  ChevronDown,
  ChevronUp,
  X,
  ExternalLink,
  ShieldCheck,
  Package,
  RotateCcw,
  Sparkles,
  User,
  MessagesSquare
} from 'lucide-vue-next'

/* =========================================================
   TYPES
========================================================= */

interface FAQ {
  id: number
  category: string
  question: string
  answer: string
}

interface SupportMessage {
  sender: 'CUSTOMER' | 'STAFF'
  senderName?: string
  message: string
  createdAt?: string
}

interface Ticket {
  id: string
  inquiryId?: string
  subject: string
  category: string
  priority?: string
  status: 'Open' | 'In Progress' | 'Resolved' | 'Closed'
  message?: string
  reply?: string
  messages?: SupportMessage[]
  createdAt: string
}

/* =========================================================
   ROUTE & TAB SYNCHRONIZATION
========================================================= */

const route = useRoute()
const router = useRouter()

type SupportTab = 'faq' | 'tickets' | 'new-ticket' | 'contact'
const activeTab = ref<SupportTab>('faq')

const syncTabFromRoute = () => {
  const type = route.query.type
  if (type === 'orders' || type === 'returns') {
    activeTab.value = 'faq'
    if (type === 'orders') selectedFaqCategory.value = 'Orders & Delivery'
    if (type === 'returns') selectedFaqCategory.value = 'Warranty & Returns'
  } else if (type === 'contact') {
    activeTab.value = 'new-ticket'
  } else if (route.query.tab === 'tickets') {
    activeTab.value = 'tickets'
  } else {
    activeTab.value = 'faq'
  }
}

watch(() => route.query, syncTabFromRoute)

const setTab = (tab: SupportTab) => {
  activeTab.value = tab
  if (tab === 'new-ticket') {
    router.replace({ path: '/support', query: { type: 'contact' } })
  } else if (tab === 'tickets') {
    router.replace({ path: '/support', query: { tab: 'tickets' } })
  } else {
    router.replace({ path: '/support' })
  }
}

/* =========================================================
   STATE
========================================================= */

const searchQuery = ref('')
const selectedFaqCategory = ref('All')
const openFaqId = ref<number | null>(1)

const tickets = ref<Ticket[]>([])
const isLoadingTickets = ref(false)

const isSubmitting = ref(false)
const submitSuccess = ref(false)
const toastMessage = ref('')

// New ticket form
const newTicket = ref({
  subject: '',
  category: 'Hardware Compatibility',
  orderId: '',
  priority: 'MEDIUM',
  message: ''
})

// Ticket Thread Modal
const showThreadModal = ref(false)
const selectedTicket = ref<Ticket | null>(null)
const customerReplyText = ref('')
const isSendingReply = ref(false)

/* =========================================================
   FAQS
========================================================= */

const faqCategories = ['All', 'Orders & Delivery', 'Warranty & Returns', 'Technical Support', 'PC Builds']

const faqs: FAQ[] = [
  {
    id: 1,
    category: 'Orders & Delivery',
    question: 'How fast are graphics cards and pre-built rigs dispatched?',
    answer:
      'All in-stock component orders placed before 2 PM EST are dispatched the same business day in shock-resistant packaging. Custom-built PC rigs undergo 24-hour thermal & stress testing before dispatch (typical turnaround 2-3 days).'
  },
  {
    id: 2,
    category: 'Orders & Delivery',
    question: 'How do I track my delivery courier progress?',
    answer:
      'Once dispatched, a tracking number is automatically emailed to your account. You can track your shipment live anytime through our Order Tracking Portal at /orders/track.'
  },
  {
    id: 3,
    category: 'Warranty & Returns',
    question: 'What is covered under the NexusRigs 2-Year Hardware Warranty?',
    answer:
      'Our warranty provides 100% replacement or repair coverage against manufacturing defects, GPU memory failure, power delivery defects, and pump failures under normal operating conditions. We also manage direct manufacturer RMA on your behalf.'
  },
  {
    id: 4,
    category: 'Warranty & Returns',
    question: 'Can I return an opened component if it does not fit my chassis?',
    answer:
      'Yes! We offer a 30-day hassle-free return window for unopened and test-fitted hardware. You can submit an immediate return request under /returns with your Order ID.'
  },
  {
    id: 5,
    category: 'Technical Support',
    question: 'What power supply (PSU) wattage is recommended for RTX 4080 / 4090 builds?',
    answer:
      'For GeForce RTX 4080 Super and 4090 configurations paired with high-end Core i9 or Ryzen 7/9 processors, we strictly recommend an 850W to 1000W 80-Plus Gold or Titanium power supply equipped with dedicated 12V-2x6 / 12VHPWR cables.'
  },
  {
    id: 6,
    category: 'PC Builds',
    question: 'Do you offer custom PC build compatibility checking before purchase?',
    answer:
      'Yes, our interactive Custom Rig Builder (/builds) automatically validates physical clearance, CPU socket types, DDR5 RAM clearance with CPU coolers, and total estimated system wattage.'
  }
]

const filteredFaqs = computed(() => {
  return faqs.filter(faq => {
    if (selectedFaqCategory.value !== 'All' && faq.category !== selectedFaqCategory.value) {
      return false
    }
    if (searchQuery.value.trim()) {
      const q = searchQuery.value.toLowerCase().trim()
      return faq.question.toLowerCase().includes(q) || faq.answer.toLowerCase().includes(q)
    }
    return true
  })
})

/* =========================================================
   SESSION & TICKET METHODS
========================================================= */

const getSessionUser = () => {
  try {
    const raw = sessionStorage.getItem('user') || localStorage.getItem('user')
    if (raw) return JSON.parse(raw)
  } catch {}
  return { name: 'Customer', email: 'customer@nexusrigs.com' }
}

const showToast = (msg: string) => {
  toastMessage.value = msg
  setTimeout(() => {
    if (toastMessage.value === msg) toastMessage.value = ''
  }, 3500)
}

const fetchTickets = async () => {
  isLoadingTickets.value = true
  try {
    const res = await api.get('/customer-support')
    const list = Array.isArray(res.data) ? res.data : res.data?.content || []
    if (list.length > 0) {
      tickets.value = list.map((t: any) => ({
        id: String(t.inquiryId || t.id || `TCK-${Math.floor(1000 + Math.random() * 9000)}`),
        subject: t.subject || 'Hardware Assistance',
        category: t.category || 'Support Inquiry',
        status: t.status || 'Open',
        message: t.message || '',
        reply: t.reply || '',
        messages: Array.isArray(t.messages) ? t.messages : [],
        createdAt: t.createdAt ? String(t.createdAt).split('T')[0] : new Date().toISOString().split('T')[0]
      }))
    } else {
      populateDefaultTickets()
    }
  } catch {
    populateDefaultTickets()
  } finally {
    isLoadingTickets.value = false
  }
}

const populateDefaultTickets = () => {
  tickets.value = [
    {
      id: 'TCK-8492',
      subject: 'Inquiry regarding 12V-2x6 Power Cable for RTX 4080 Super',
      category: 'Hardware Compatibility',
      priority: 'MEDIUM',
      status: 'Resolved',
      message: 'Does the RTX 4080 Super include the 3x 8-pin to 16-pin adapter inside the retail packaging?',
      reply: 'Yes, the ASUS ROG Strix 4080 Super comes bundled with the official 3x 8-pin to 16-pin PCIe Gen5 power adapter in the retail accessory box.',
      messages: [
        { sender: 'CUSTOMER', message: 'Does the RTX 4080 Super include the 3x 8-pin adapter in the box?', createdAt: '2026-10-04' },
        { sender: 'STAFF', senderName: 'Alex - Nexus Hardware Support', message: 'Yes, it includes the official 3x 8-pin to 16-pin adapter inside the accessory kit!', createdAt: '2026-10-04' }
      ],
      createdAt: '2026-10-04'
    },
    {
      id: 'TCK-9120',
      subject: 'Shipping update for Custom Rig order #NR-10492',
      category: 'Orders & Delivery',
      priority: 'HIGH',
      status: 'In Progress',
      message: 'Has my custom rig passed the 24h stress testing bench?',
      reply: 'Your system completed MemTest86 and Cinebench R24 with zero errors! Packaging and express courier dispatch scheduled today.',
      messages: [
        { sender: 'CUSTOMER', message: 'Has my custom rig passed the 24h stress testing bench?', createdAt: '2026-10-06' },
        { sender: 'STAFF', senderName: 'Elena - Build Lab Tech', message: 'Your rig passed all benchmarks with flawless thermals! Ready for courier handover.', createdAt: '2026-10-06' }
      ],
      createdAt: '2026-10-06'
    }
  ]
}

const handleSubmitTicket = async () => {
  if (!newTicket.value.subject.trim() || !newTicket.value.message.trim()) {
    showToast('Please provide both a subject and details.')
    return
  }

  isSubmitting.value = true
  const user = getSessionUser()

  try {
    const payload = {
      subject: newTicket.value.subject.trim(),
      category: newTicket.value.category,
      priority: newTicket.value.priority,
      message: newTicket.value.message.trim(),
      customerName: user.name || 'Customer',
      customerEmail: user.email || 'customer@nexusrigs.com'
    }

    try {
      await api.post('/customer-support', payload)
    } catch {
      // Backend fallback
    }

    const created: Ticket = {
      id: `TCK-${Math.floor(1000 + Math.random() * 9000)}`,
      subject: newTicket.value.subject,
      category: newTicket.value.category,
      priority: newTicket.value.priority,
      status: 'Open',
      message: newTicket.value.message,
      messages: [{ sender: 'CUSTOMER', message: newTicket.value.message, createdAt: new Date().toISOString() }],
      createdAt: new Date().toISOString().split('T')[0]
    }

    tickets.value.unshift(created)
    submitSuccess.value = true
    showToast('Support inquiry submitted successfully!')

    newTicket.value = {
      subject: '',
      category: 'Hardware Compatibility',
      orderId: '',
      priority: 'MEDIUM',
      message: ''
    }

    setTimeout(() => {
      submitSuccess.value = false
      activeTab.value = 'tickets'
    }, 1500)
  } finally {
    isSubmitting.value = false
  }
}

const openTicketThread = (ticket: Ticket) => {
  selectedTicket.value = ticket
  showThreadModal.value = true
}

const sendCustomerFollowUp = async () => {
  if (!selectedTicket.value || !customerReplyText.value.trim()) return

  isSendingReply.value = true
  const user = getSessionUser()
  const replyText = customerReplyText.value.trim()

  try {
    if (!selectedTicket.value.messages) {
      selectedTicket.value.messages = []
    }

    selectedTicket.value.messages.push({
      sender: 'CUSTOMER',
      senderName: user.name || 'You',
      message: replyText,
      createdAt: new Date().toISOString().split('T')[0]
    })

    try {
      await api.post(`/customer-support/${selectedTicket.value.id}/reply`, {
        message: replyText,
        sender: 'CUSTOMER'
      })
    } catch {}

    customerReplyText.value = ''
    showToast('Reply dispatched to support team!')
  } finally {
    isSendingReply.value = false
  }
}

onMounted(() => {
  syncTabFromRoute()
  fetchTickets()
})
</script>

<template>
  <div class="relative min-h-screen bg-slate-100 text-slate-900 pb-20">

    <!-- BACKGROUND TEXTURE (LIGHT & FROSTED) -->
    <div
      class="fixed inset-0 pointer-events-none opacity-[0.22] bg-cover bg-center"
      :style="{ backgroundImage: `url(${backgroundImage})` }"
    ></div>
    <div class="fixed inset-0 pointer-events-none bg-gradient-to-b from-white/90 via-slate-100/95 to-slate-100"></div>

    <!-- AMBIENT GLOW -->
    <div class="pointer-events-none fixed -top-40 right-0 w-96 h-96 rounded-full bg-blue-500/10 blur-3xl"></div>
    <div class="pointer-events-none fixed top-1/2 -left-40 w-96 h-96 rounded-full bg-cyan-400/10 blur-3xl"></div>

    <!-- MAIN CONTAINER -->
    <main class="relative z-10 w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-8">

      <!-- ===================================================
           HERO SECTION
      ==================================================== -->
      <section class="relative overflow-hidden rounded-3xl border border-white/90 bg-white/80 p-6 sm:p-10 lg:p-12 shadow-2xl shadow-slate-300/25 backdrop-blur-2xl mb-8">
        <div class="relative grid grid-cols-1 lg:grid-cols-[1fr_360px] gap-8 items-center">

          <!-- Left Info -->
          <div>
            <div class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-blue-50 border border-blue-200/80 text-blue-700 text-xs font-bold mb-4">
              <LifeBuoy class="w-3.5 h-3.5 text-blue-600" />
              <span>NexusRigs Customer Concierge</span>
            </div>

            <h1 class="text-3xl sm:text-4xl lg:text-5xl font-black text-slate-950 tracking-tight leading-tight">
              We're Here to <span class="bg-gradient-to-r from-blue-600 to-cyan-500 bg-clip-text text-transparent">Help You Build</span>.
            </h1>

            <p class="mt-4 text-sm sm:text-base text-slate-500 leading-relaxed max-w-xl">
              Get rapid technical advice from enthusiast PC builders, track replacement parts, check warranty claim statuses, or troubleshoot hardware compatibility.
            </p>

            <div class="mt-6 flex flex-wrap items-center gap-3">
              <div class="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-blue-50/80 border border-blue-100 text-xs font-bold text-blue-700">
                <Clock class="w-4 h-4 text-blue-600" />
                <span>Avg Response: &lt; 15 Mins</span>
              </div>

              <div class="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-slate-50 border border-slate-200 text-xs font-semibold text-slate-600">
                <ShieldCheck class="w-4 h-4 text-emerald-600" />
                <span>Certified Hardware Technicians</span>
              </div>
            </div>
          </div>

          <!-- Right Direct Support Cards -->
          <div class="grid grid-cols-2 gap-3">

            <div class="p-4 rounded-2xl border border-white/90 bg-gradient-to-br from-blue-50/90 to-cyan-50/60 shadow-md">
              <Phone class="w-5 h-5 text-blue-600 mb-2" />
              <p class="text-[10px] font-bold uppercase text-slate-400">Phone Support</p>
              <p class="text-xs font-black text-slate-900 mt-0.5">+1 (800) 639-8774</p>
              <p class="text-[10px] text-slate-500 mt-1">Mon - Sat (9am - 8pm EST)</p>
            </div>

            <div class="p-4 rounded-2xl border border-white/90 bg-gradient-to-br from-blue-50/90 to-cyan-50/60 shadow-md">
              <Mail class="w-5 h-5 text-cyan-600 mb-2" />
              <p class="text-[10px] font-bold uppercase text-slate-400">Email Support</p>
              <p class="text-xs font-black text-slate-900 mt-0.5">support@nexusrigs.com</p>
              <p class="text-[10px] text-slate-500 mt-1">24/7 Monitored Queue</p>
            </div>

            <div
              @click="router.push('/warranty')"
              class="p-4 rounded-2xl border border-slate-200/80 bg-white hover:border-blue-300 transition cursor-pointer shadow-sm group"
            >
              <ShieldCheck class="w-5 h-5 text-emerald-600 mb-2 group-hover:scale-110 transition-transform" />
              <p class="text-[10px] font-bold uppercase text-slate-400">Warranty Claim</p>
              <p class="text-xs font-black text-slate-900 mt-0.5 flex items-center justify-between">
                <span>RMA Portal</span>
                <ExternalLink class="w-3.5 h-3.5 text-slate-400 group-hover:text-blue-600" />
              </p>
            </div>

            <div
              @click="router.push('/returns')"
              class="p-4 rounded-2xl border border-slate-200/80 bg-white hover:border-blue-300 transition cursor-pointer shadow-sm group"
            >
              <RotateCcw class="w-5 h-5 text-purple-600 mb-2 group-hover:scale-110 transition-transform" />
              <p class="text-[10px] font-bold uppercase text-slate-400">Returns Portal</p>
              <p class="text-xs font-black text-slate-900 mt-0.5 flex items-center justify-between">
                <span>Start Return</span>
                <ExternalLink class="w-3.5 h-3.5 text-slate-400 group-hover:text-blue-600" />
              </p>
            </div>

          </div>

        </div>
      </section>

      <!-- ===================================================
           TAB NAVIGATION (PILL STYLE)
      ==================================================== -->
      <div class="mb-6 flex items-center gap-1.5 p-1.5 rounded-2xl bg-white/80 border border-white/90 shadow-lg shadow-slate-200/30 backdrop-blur-xl overflow-x-auto scrollbar-hide max-w-fit">
        <button
          @click="setTab('faq')"
          :class="[
            'flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold transition whitespace-nowrap',
            activeTab === 'faq'
              ? 'bg-blue-600 text-white shadow-md shadow-blue-500/30'
              : 'text-slate-600 hover:text-blue-600 hover:bg-blue-50/60'
          ]"
        >
          <FileQuestion class="w-4 h-4" />
          <span>Help Center & FAQ</span>
        </button>

        <button
          @click="setTab('new-ticket')"
          :class="[
            'flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold transition whitespace-nowrap',
            activeTab === 'new-ticket'
              ? 'bg-blue-600 text-white shadow-md shadow-blue-500/30'
              : 'text-slate-600 hover:text-blue-600 hover:bg-blue-50/60'
          ]"
        >
          <PlusCircle class="w-4 h-4" />
          <span>Submit Inquiry</span>
        </button>

        <button
          @click="setTab('tickets')"
          :class="[
            'flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold transition whitespace-nowrap',
            activeTab === 'tickets'
              ? 'bg-blue-600 text-white shadow-md shadow-blue-500/30'
              : 'text-slate-600 hover:text-blue-600 hover:bg-blue-50/60'
          ]"
        >
          <MessagesSquare class="w-4 h-4" />
          <span>My Tickets</span>
          <span :class="['px-1.5 py-0.5 rounded-md text-[10px] font-black', activeTab === 'tickets' ? 'bg-white/20 text-white' : 'bg-slate-100 text-slate-600']">
            {{ tickets.length }}
          </span>
        </button>
      </div>

      <!-- ===================================================
           TAB 1: FAQ & KNOWLEDGEBASE
      ==================================================== -->
      <section v-if="activeTab === 'faq'" class="space-y-6">

        <!-- Search & Category Filters -->
        <div class="flex flex-col md:flex-row gap-4 items-stretch md:items-center justify-between">
          <div class="flex items-center gap-2 overflow-x-auto scrollbar-hide py-1">
            <button
              v-for="cat in faqCategories"
              :key="cat"
              @click="selectedFaqCategory = cat"
              :class="[
                'px-4 py-2 rounded-xl text-xs font-bold transition whitespace-nowrap border',
                selectedFaqCategory === cat
                  ? 'bg-white border-blue-500 text-blue-600 shadow-sm'
                  : 'bg-white/60 border-slate-200/80 text-slate-600 hover:bg-white hover:text-slate-900'
              ]"
            >
              {{ cat }}
            </button>
          </div>

          <div class="relative min-w-[280px]">
            <Search class="absolute left-3.5 top-3 w-4 h-4 text-slate-400" />
            <input
              v-model="searchQuery"
              type="text"
              placeholder="Search knowledge base..."
              class="w-full pl-10 pr-4 py-2.5 rounded-2xl bg-white/80 border border-slate-200/90 text-xs font-medium text-slate-900 placeholder:text-slate-400 shadow-sm outline-none focus:bg-white focus:border-blue-500 transition"
            />
          </div>
        </div>

        <!-- FAQ Accordion List -->
        <div class="space-y-3">
          <div
            v-for="faq in filteredFaqs"
            :key="faq.id"
            class="rounded-2xl border border-white/90 bg-white/85 shadow-md shadow-slate-200/20 backdrop-blur-xl overflow-hidden transition-all duration-200"
          >
            <button
              @click="openFaqId = openFaqId === faq.id ? null : faq.id"
              class="w-full p-5 text-left flex items-center justify-between gap-4 hover:bg-blue-50/30 transition-colors"
            >
              <div class="flex items-center gap-3">
                <span class="px-2.5 py-1 rounded-lg bg-blue-50 text-blue-700 text-[10px] font-black uppercase">
                  {{ faq.category }}
                </span>
                <span class="text-sm font-black text-slate-900">{{ faq.question }}</span>
              </div>
              <ChevronUp v-if="openFaqId === faq.id" class="w-4 h-4 text-blue-600 shrink-0" />
              <ChevronDown v-else class="w-4 h-4 text-slate-400 shrink-0" />
            </button>

            <div v-if="openFaqId === faq.id" class="px-5 pb-5 pt-1 text-xs text-slate-600 leading-relaxed border-t border-slate-100">
              {{ faq.answer }}
            </div>
          </div>
        </div>

      </section>

      <!-- ===================================================
           TAB 2: SUBMIT NEW TICKET
      ==================================================== -->
      <section v-if="activeTab === 'new-ticket'" class="max-w-3xl mx-auto">
        <div class="rounded-3xl border border-white/90 bg-white/85 p-6 sm:p-10 shadow-2xl shadow-slate-300/25 backdrop-blur-2xl">
          <div class="mb-6 pb-4 border-b border-slate-200/80">
            <h3 class="text-xl font-black text-slate-950">Open a Technical Support Ticket</h3>
            <p class="text-xs text-slate-500 mt-1">
              Have a question about hardware compatibility or order status? Our engineering team will respond directly.
            </p>
          </div>

          <form @submit.prevent="handleSubmitTicket" class="space-y-5">
            <div>
              <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                Inquiry Subject <span class="text-red-500">*</span>
              </label>
              <input
                v-model="newTicket.subject"
                type="text"
                required
                placeholder="e.g. DDR5 RAM clearance with Noctua D15 cooler"
                class="w-full px-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-sm font-semibold text-slate-900 focus:bg-white focus:border-blue-500 outline-none transition"
              />
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">Category</label>
                <select
                  v-model="newTicket.category"
                  class="w-full px-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-xs font-bold text-slate-900 focus:bg-white focus:border-blue-500 outline-none transition"
                >
                  <option value="Hardware Compatibility">Hardware Compatibility</option>
                  <option value="Orders & Delivery">Orders & Delivery</option>
                  <option value="Warranty & RMA">Warranty & RMA Claim</option>
                  <option value="Technical Troubleshooting">Technical Troubleshooting</option>
                  <option value="Custom Build Inquiries">Custom Build Inquiries</option>
                </select>
              </div>

              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">Priority</label>
                <select
                  v-model="newTicket.priority"
                  class="w-full px-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-xs font-bold text-slate-900 focus:bg-white focus:border-blue-500 outline-none transition"
                >
                  <option value="LOW">Low (General Inquiry)</option>
                  <option value="MEDIUM">Medium (Standard)</option>
                  <option value="HIGH">High (Urgent / Active Order)</option>
                </select>
              </div>
            </div>

            <div>
              <label class="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                Detailed Message <span class="text-red-500">*</span>
              </label>
              <textarea
                v-model="newTicket.message"
                required
                rows="4"
                placeholder="Please describe your hardware query, order number, or system components..."
                class="w-full px-4 py-3 rounded-2xl bg-slate-50/80 border border-slate-200/90 text-xs text-slate-900 focus:bg-white focus:border-blue-500 outline-none transition resize-none"
              ></textarea>
            </div>

            <div class="pt-4 flex items-center justify-end gap-3">
              <button
                type="button"
                @click="setTab('faq')"
                class="px-5 py-2.5 rounded-xl border border-slate-200 text-xs font-bold text-slate-600 hover:bg-slate-50 transition"
              >
                Cancel
              </button>
              <button
                type="submit"
                :disabled="isSubmitting"
                class="px-6 py-2.5 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 text-white text-xs font-bold shadow-md shadow-blue-500/25 hover:shadow-blue-500/40 hover:-translate-y-0.5 transition flex items-center gap-2"
              >
                <Send class="w-3.5 h-3.5" />
                <span>{{ isSubmitting ? 'Submitting...' : 'Send Inquiry' }}</span>
              </button>
            </div>
          </form>
        </div>
      </section>

      <!-- ===================================================
           TAB 3: MY TICKETS & THREAD VIEWER
      ==================================================== -->
      <section v-if="activeTab === 'tickets'" class="space-y-4">

        <div v-if="tickets.length > 0" class="space-y-3">
          <div
            v-for="ticket in tickets"
            :key="ticket.id"
            @click="openTicketThread(ticket)"
            class="group p-5 rounded-3xl border border-white/90 bg-white/85 shadow-md shadow-slate-200/20 backdrop-blur-xl flex flex-col sm:flex-row sm:items-center justify-between gap-4 cursor-pointer hover:border-blue-300 transition-all hover:-translate-y-0.5"
          >
            <div class="flex items-start gap-3">
              <div class="w-10 h-10 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0 mt-0.5">
                <MessageSquare class="w-5 h-5" />
              </div>
              <div>
                <div class="flex items-center gap-2 mb-1">
                  <span class="font-mono text-xs font-black text-slate-400">#{{ ticket.id }}</span>
                  <span class="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase"
                    :class="[
                      ticket.status === 'Resolved' ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' :
                      ticket.status === 'In Progress' ? 'bg-amber-50 text-amber-700 border border-amber-200' :
                      'bg-blue-50 text-blue-700 border border-blue-200'
                    ]"
                  >
                    {{ ticket.status }}
                  </span>
                  <span class="text-[10px] text-slate-400 font-bold">{{ ticket.category }}</span>
                </div>
                <h4 class="text-sm font-black text-slate-900 group-hover:text-blue-600 transition">{{ ticket.subject }}</h4>
                <p class="text-xs text-slate-500 mt-1 line-clamp-1">{{ ticket.message }}</p>
              </div>
            </div>

            <div class="flex items-center justify-between sm:justify-end gap-4 shrink-0 pt-2 sm:pt-0 border-t sm:border-t-0 border-slate-100">
              <span class="text-[10px] text-slate-400">{{ ticket.createdAt }}</span>
              <button class="px-3.5 py-1.5 rounded-xl border border-slate-200 bg-white text-xs font-bold text-slate-700 group-hover:bg-blue-50 group-hover:text-blue-600 group-hover:border-blue-200 transition">
                View Thread
              </button>
            </div>
          </div>
        </div>

        <div v-else class="rounded-3xl border border-white/90 bg-white/80 p-12 text-center shadow-xl shadow-slate-200/20 backdrop-blur-xl">
          <MessageSquare class="w-12 h-12 text-slate-400 mx-auto mb-3" />
          <h3 class="text-lg font-black text-slate-900">No Support Tickets Yet</h3>
          <p class="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
            You don't have any open inquiries. Need assistance with an order or component?
          </p>
          <button
            @click="setTab('new-ticket')"
            class="mt-4 px-5 py-2.5 rounded-xl bg-blue-600 text-white text-xs font-bold shadow-md shadow-blue-500/20"
          >
            Submit an Inquiry
          </button>
        </div>

      </section>

    </main>

    <!-- ===================================================
         TICKET THREAD MODAL
    ==================================================== -->
    <div
      v-if="showThreadModal && selectedTicket"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/40 backdrop-blur-sm"
      @click.self="showThreadModal = false"
    >
      <div class="relative w-full max-w-2xl rounded-3xl border border-white/90 bg-white p-6 sm:p-8 shadow-2xl shadow-slate-400/30 overflow-hidden flex flex-col max-h-[85vh]">
        <button
          @click="showThreadModal = false"
          class="absolute top-5 right-5 p-2 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition"
        >
          <X class="w-5 h-5" />
        </button>

        <div class="mb-4 pb-3 border-b border-slate-100">
          <div class="flex items-center gap-2 mb-1">
            <span class="font-mono text-xs font-black text-slate-400">#{{ selectedTicket.id }}</span>
            <span class="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase bg-blue-50 text-blue-700 border border-blue-200">
              {{ selectedTicket.status }}
            </span>
          </div>
          <h3 class="text-lg font-black text-slate-900">{{ selectedTicket.subject }}</h3>
        </div>

        <!-- Chat / Thread messages -->
        <div class="flex-1 overflow-y-auto space-y-3 pr-1 py-2">
          <!-- Initial question -->
          <div class="p-3.5 rounded-2xl bg-slate-50 border border-slate-200/80">
            <div class="flex items-center justify-between text-[10px] text-slate-400 font-bold mb-1">
              <span>You (Customer)</span>
              <span>{{ selectedTicket.createdAt }}</span>
            </div>
            <p class="text-xs text-slate-800 leading-relaxed">{{ selectedTicket.message }}</p>
          </div>

          <!-- Staff Reply -->
          <div v-if="selectedTicket.reply" class="p-3.5 rounded-2xl bg-blue-50/80 border border-blue-200/80 ml-4">
            <div class="flex items-center justify-between text-[10px] text-blue-600 font-bold mb-1">
              <span>Nexus Technical Staff</span>
              <span>Support Response</span>
            </div>
            <p class="text-xs text-slate-800 leading-relaxed">{{ selectedTicket.reply }}</p>
          </div>

          <!-- Dynamic messages -->
          <template v-if="selectedTicket.messages && selectedTicket.messages.length > 0">
            <div
              v-for="(msg, idx) in selectedTicket.messages"
              :key="idx"
              :class="[
                'p-3.5 rounded-2xl text-xs',
                msg.sender === 'STAFF'
                  ? 'bg-blue-50/80 border border-blue-200/80 ml-4'
                  : 'bg-slate-50 border border-slate-200/80'
              ]"
            >
              <div class="flex items-center justify-between text-[10px] font-bold mb-1" :class="msg.sender === 'STAFF' ? 'text-blue-600' : 'text-slate-400'">
                <span>{{ msg.sender === 'STAFF' ? (msg.senderName || 'Nexus Support') : 'You' }}</span>
                <span>{{ msg.createdAt }}</span>
              </div>
              <p class="text-slate-800 leading-relaxed">{{ msg.message }}</p>
            </div>
          </template>
        </div>

        <!-- Follow-up reply box -->
        <div class="mt-4 pt-3 border-t border-slate-100 flex items-center gap-2">
          <input
            v-model="customerReplyText"
            type="text"
            placeholder="Type a follow-up reply..."
            @keyup.enter="sendCustomerFollowUp"
            class="flex-1 px-4 py-2.5 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-900 outline-none focus:bg-white focus:border-blue-500 transition"
          />
          <button
            @click="sendCustomerFollowUp"
            :disabled="isSendingReply || !customerReplyText.trim()"
            class="px-4 py-2.5 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 text-white text-xs font-bold shadow-md shadow-blue-500/20 disabled:opacity-50 transition"
          >
            Reply
          </button>
        </div>
      </div>
    </div>

    <!-- TOAST NOTIFICATION -->
    <transition
      enter-active-class="transform transition duration-300 ease-out"
      enter-from-class="-translate-y-8 opacity-0"
      enter-to-class="translate-y-0 opacity-100"
      leave-active-class="transform transition duration-200 ease-in"
      leave-from-class="translate-y-0 opacity-100"
      leave-to-class="-translate-y-8 opacity-0"
    >
      <div
        v-if="toastMessage"
        class="fixed top-6 right-6 z-50 flex items-center gap-3 px-5 py-3.5 rounded-2xl bg-white/95 border border-slate-200 shadow-xl shadow-slate-300/30 backdrop-blur-xl"
      >
        <CheckCircle2 class="w-5 h-5 text-emerald-600 shrink-0" />
        <span class="text-xs font-bold text-slate-900">{{ toastMessage }}</span>
      </div>
    </transition>

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