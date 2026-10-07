<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminConfirmModal from '../../components/admin/AdminConfirmModal.vue'
import api from '../../services/api'

/* =========================================================
   TYPES
========================================================= */

interface Customer {
    customerId?: string | number
    name?: string
    email?: string
    phone?: string
}

interface SupportStaff {
    supportStaffId?: string | number
    id?: string | number
    name?: string
    email?: string
}

interface SupportMessage {
    sender: 'CUSTOMER' | 'STAFF'
    senderName?: string
    message: string
    createdAt?: string
}

interface Inquiry {
    inquiryId: string | number
    customerId?: string | number
    customerName: string
    customerImage?: string
    customerEmail: string
    customerPhone?: string

    subject: string
    message: string

    category: string
    priority: string
    status: string

    createdAt: string
    updatedAt?: string

    assignedStaffId?: string | number | null
    assignedStaffName?: string | null

    reply?: string
    repliedAt?: string

    messages?: SupportMessage[]
}

/* =========================================================
   STATE
========================================================= */

const inquiries = ref<Inquiry[]>([])
const supportStaff = ref<SupportStaff[]>([])

const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const searchQuery = ref('')
const selectedStatus = ref('All')
const selectedPriority = ref('All')
const selectedCategory = ref('All')

const currentPage = ref(1)
const itemsPerPage = ref(8)

/* =========================================================
   MODALS
========================================================= */

const showViewModal = ref(false)
const showReplyModal = ref(false)
const showAssignModal = ref(false)
const showDeleteModal = ref(false)

/* =========================================================
   SELECTED INQUIRY
========================================================= */

const selectedInquiry = ref<Inquiry | null>(null)

const replyText = ref('')
const selectedStaffId = ref('')
const selectedNewStatus = ref('')

/* =========================================================
   CONSTANTS
========================================================= */

const statuses = [
    'Open',
    'Pending',
    'In Progress',
    'Resolved',
    'Closed'
]

const priorities = [
    'Low',
    'Medium',
    'High',
    'Urgent'
]

const categories = [
    'Order',
    'Payment',
    'Delivery',
    'Return & Refund',
    'Product',
    'Account',
    'Technical',
    'Other'
]

/* =========================================================
   SAMPLE DATA
   Used only if backend has no data.
========================================================= */

const sampleInquiries: Inquiry[] = [
    {
        inquiryId: 'INQ-1001',
        customerId: 'CUS-001',
        customerName: 'Kasun Perera',
        customerEmail: 'kasun@gmail.com',
        customerPhone: '0771234567',
        subject: 'Order has not arrived',
        message:
            'My order was supposed to arrive yesterday, but I have not received it yet. Can you please check the delivery status?',
        category: 'Delivery',
        priority: 'High',
        status: 'Open',
        createdAt: '2026-09-30T09:30:00',
        assignedStaffName: null
    },

    {
        inquiryId: 'INQ-1002',
        customerId: 'CUS-002',
        customerName: 'Nimal Fernando',
        customerEmail: 'nimal@gmail.com',
        customerPhone: '0712345678',
        subject: 'Payment was charged twice',
        message:
            'I made one payment but my bank account appears to have been charged twice.',
        category: 'Payment',
        priority: 'Urgent',
        status: 'In Progress',
        createdAt: '2026-09-29T14:15:00',
        assignedStaffName: 'Support Staff'
    },

    {
        inquiryId: 'INQ-1003',
        customerId: 'CUS-003',
        customerName: 'Amali Silva',
        customerEmail: 'amali@gmail.com',
        customerPhone: '0764567890',
        subject: 'Product information request',
        message:
            'Could you provide more information about the warranty available for this product?',
        category: 'Product',
        priority: 'Low',
        status: 'Pending',
        createdAt: '2026-09-28T11:20:00',
        assignedStaffName: 'Support Staff'
    },

    {
        inquiryId: 'INQ-1004',
        customerId: 'CUS-004',
        customerName: 'Dinesh Kumar',
        customerEmail: 'dinesh@gmail.com',
        customerPhone: '0751234567',
        subject: 'Return request assistance',
        message:
            'I would like to return the product because the item I received is damaged.',
        category: 'Return & Refund',
        priority: 'High',
        status: 'Open',
        createdAt: '2026-09-27T16:40:00',
        assignedStaffName: null
    },

    {
        inquiryId: 'INQ-1005',
        customerId: 'CUS-005',
        customerName: 'Tharushi Perera',
        customerEmail: 'tharushi@gmail.com',
        customerPhone: '0789876543',
        subject: 'Unable to login',
        message:
            'I forgot my password and I am unable to access my account.',
        category: 'Account',
        priority: 'Medium',
        status: 'Resolved',
        createdAt: '2026-09-26T10:05:00',
        assignedStaffName: 'Support Staff',
        reply:
            'Your password reset request has been processed successfully.',
        repliedAt: '2026-09-26T12:10:00'
    },

    {
        inquiryId: 'INQ-1006',
        customerId: 'CUS-006',
        customerName: 'Ravindu Jayasinghe',
        customerEmail: 'ravindu@gmail.com',
        customerPhone: '0723456789',
        subject: 'Wrong product received',
        message:
            'The product delivered to me is different from the product I ordered.',
        category: 'Order',
        priority: 'High',
        status: 'In Progress',
        createdAt: '2026-09-25T13:25:00',
        assignedStaffName: 'Support Staff'
    },

    {
        inquiryId: 'INQ-1007',
        customerId: 'CUS-007',
        customerName: 'Shehan Dias',
        customerEmail: 'shehan@gmail.com',
        customerPhone: '0701234567',
        subject: 'Website issue',
        message:
            'The checkout page is not loading correctly when I try to place an order.',
        category: 'Technical',
        priority: 'Medium',
        status: 'Open',
        createdAt: '2026-09-24T15:00:00'
    },

    {
        inquiryId: 'INQ-1008',
        customerId: 'CUS-008',
        customerName: 'Hiruni Silva',
        customerEmail: 'hiruni@gmail.com',
        customerPhone: '0779876543',
        subject: 'Refund status',
        message:
            'I submitted a return request several days ago. Could you please update me about the refund?',
        category: 'Return & Refund',
        priority: 'Medium',
        status: 'Pending',
        createdAt: '2026-09-23T09:45:00'
    }
]

/* =========================================================
   LOAD INQUIRIES
========================================================= */

async function loadInquiries() {
    loading.value = true
    errorMessage.value = ''

    try {
        const response = await api.get('/support/inquiries')

        if (Array.isArray(response.data)) {
            inquiries.value = response.data.map(mapInquiry).reverse()
        } else if (Array.isArray(response.data?.content)) {
            inquiries.value = response.data.content.map(mapInquiry).reverse()
        } else {
            inquiries.value = []
        }
    } catch (error) {
        console.log('Support inquiry API unavailable:', error)
        inquiries.value = []
    } finally {
        loading.value = false
    }
}

/* =========================================================
   LOAD SUPPORT STAFF
========================================================= */

async function loadSupportStaff() {
    try {
        let list: any[] = []
        try {
            const res = await api.get('/admin/system-users')
            const users = Array.isArray(res.data) ? res.data : Array.isArray(res.data?.content) ? res.data.content : []
            list = users.filter((u: any) => {
                const role = String(u.role || '').toUpperCase()
                return role.includes('SUPPORT') || role.includes('ADMIN')
            }).map((u: any) => ({
                supportStaffId: u.userId || u.id,
                id: u.userId || u.id,
                name: u.name || u.email,
                email: u.email
            }))
        } catch {
            const response = await api.get('/support-staff')
            list = Array.isArray(response.data) ? response.data : []
        }

        supportStaff.value = list.length > 0 ? list : [
            { supportStaffId: 'SS001', name: 'Support Staff' }
        ]
    } catch (error) {
        console.log('Support staff API unavailable:', error)

        supportStaff.value = [
            {
                supportStaffId: 'SS001',
                name: 'Support Staff'
            }
        ]
    }
}

/* =========================================================
   MAP API DATA
========================================================= */

function mapInquiry(item: any): Inquiry {
    return {
        inquiryId:
            item.inquiryId ??
            item.id ??
            item.supportInquiryId ??
            'N/A',

        customerId:
            item.customerId ??
            item.customer?.customerId,

        customerName:
            item.customerName ??
            item.customer?.name ??
            'Unknown Customer',
            
        customerImage: item.customerImage || '',

        customerEmail:
            item.customerEmail ??
            item.customer?.email ??
            'No email',

        customerPhone:
            item.customerPhone ??
            item.customer?.phone,

        subject:
            item.subject ??
            item.title ??
            'No Subject',

        message:
            item.message ??
            item.description ??
            '',

        category:
            item.category ??
            'Other',

        priority:
            item.priority ??
            'Medium',

        status:
            item.status ??
            'Open',

        createdAt:
            item.createdAt ??
            item.createdDate ??
            item.requestDate ??
            new Date().toISOString(),

        updatedAt:
            item.updatedAt,

        assignedStaffId:
            item.assignedStaffId ??
            item.supportStaffId ??
            null,

        assignedStaffName:
            item.assignedStaffName ??
            item.supportStaffName ??
            null,

        reply:
            item.reply ??
            item.response ??
            null,

        repliedAt:
            item.repliedAt,

        messages:
            Array.isArray(item.messages) ? item.messages : []
    }
}

/* =========================================================
   FILTERED INQUIRIES
========================================================= */

const filteredInquiries = computed(() => {
    let result = [...inquiries.value]

    const search = searchQuery.value.toLowerCase().trim()

    if (search) {
        result = result.filter(function (item) {
            return (
                String(item.inquiryId).toLowerCase().includes(search) ||
                item.customerName.toLowerCase().includes(search) ||
                item.customerEmail.toLowerCase().includes(search) ||
                item.subject.toLowerCase().includes(search)
            )
        })
    }

    if (selectedStatus.value !== 'All') {
        result = result.filter(function (item) {
            return item.status === selectedStatus.value
        })
    }

    if (selectedPriority.value !== 'All') {
        result = result.filter(function (item) {
            return item.priority === selectedPriority.value
        })
    }

    if (selectedCategory.value !== 'All') {
        result = result.filter(function (item) {
            return item.category === selectedCategory.value
        })
    }

    return result
})

/* =========================================================
   PAGINATION
========================================================= */

const totalPages = computed(() => {
    return Math.max(
        1,
        Math.ceil(
            filteredInquiries.value.length /
            itemsPerPage.value
        )
    )
})

const paginatedInquiries = computed(() => {
    const start =
        (currentPage.value - 1) *
        itemsPerPage.value

    const end =
        start +
        itemsPerPage.value

    return filteredInquiries.value.slice(start, end)
})

const pageNumbers = computed(() => {
    const pages: number[] = []

    for (let i = 1; i <= totalPages.value; i++) {
        pages.push(i)
    }

    return pages
})

function goToPage(page: number) {
    if (page < 1 || page > totalPages.value) {
        return
    }

    currentPage.value = page
}

/* =========================================================
   STATISTICS
========================================================= */

const totalInquiries = computed(() => {
    return inquiries.value.length
})

const openInquiries = computed(() => {
    return inquiries.value.filter(function (item) {
        return item.status === 'Open'
    }).length
})

const pendingInquiries = computed(() => {
    return inquiries.value.filter(function (item) {
        return item.status === 'Pending'
    }).length
})

const inProgressInquiries = computed(() => {
    return inquiries.value.filter(function (item) {
        return item.status === 'In Progress'
    }).length
})

const resolvedInquiries = computed(() => {
    return inquiries.value.filter(function (item) {
        return item.status === 'Resolved'
    }).length
})

/* =========================================================
   VIEW INQUIRY
========================================================= */

function viewInquiry(inquiry: Inquiry) {
    selectedInquiry.value = inquiry
    showViewModal.value = true
}

/* =========================================================
   REPLY
========================================================= */

function resolveSessionStaff() {
    try {
        const raw = sessionStorage.getItem('user') || sessionStorage.getItem('staffUser') || localStorage.getItem('user')
        if (raw) {
            const u = JSON.parse(raw)
            return {
                id: u.userId || u.staffId || u.adminId || u.id || '',
                name: u.name || 'Support Staff'
            }
        }
    } catch {
        // ignore
    }
    return {
        id: sessionStorage.getItem('userId') || sessionStorage.getItem('staffId') || '',
        name: 'Support Staff'
    }
}

function openReplyModal(inquiry: Inquiry) {
    selectedInquiry.value = inquiry
    replyText.value = ''
    showViewModal.value = true
}

function getInquiryMessages(inquiry: Inquiry): SupportMessage[] {
    if (inquiry.messages && inquiry.messages.length > 0) {
        return [...inquiry.messages]
    }
    const msgs: SupportMessage[] = []
    if (inquiry.message) {
        msgs.push({
            sender: 'CUSTOMER',
            senderName: inquiry.customerName || 'Customer',
            message: inquiry.message,
            createdAt: inquiry.createdAt
        })
    }
    if (inquiry.reply) {
        msgs.push({
            sender: 'STAFF',
            senderName: inquiry.assignedStaffName || 'Support Representative',
            message: inquiry.reply,
            createdAt: inquiry.repliedAt || inquiry.createdAt
        })
    }
    return msgs
}

async function sendReply() {
    if (!selectedInquiry.value) {
        return
    }

    if (!replyText.value.trim()) {
        errorMessage.value = 'Please enter a reply.'
        return
    }

    saving.value = true
    errorMessage.value = ''
    const staff = resolveSessionStaff()
    const text = replyText.value.trim()

    try {
        const res = await api.post(
            `/support/inquiries/${selectedInquiry.value.inquiryId}/reply`,
            {
                reply: text,
                message: text,
                sender: 'STAFF',
                senderName: staff.name,
                supportStaffId: staff.id
            }
        )

        const updatedData = res.data ? mapInquiry(res.data) : null

        if (updatedData) {
            updateLocalInquiry(selectedInquiry.value.inquiryId, updatedData)
        } else {
            const existingMsgs = getInquiryMessages(selectedInquiry.value)
            existingMsgs.push({
                sender: 'STAFF',
                senderName: staff.name,
                message: text,
                createdAt: new Date().toISOString()
            })
            updateLocalInquiry(
                selectedInquiry.value.inquiryId,
                {
                    reply: text,
                    repliedAt: new Date().toISOString(),
                    status: 'In Progress',
                    messages: existingMsgs
                }
            )
        }

        replyText.value = ''
        showSuccess('Reply sent successfully.')
    } catch (error) {
        console.log('Reply API error:', error)

        const existingMsgs = getInquiryMessages(selectedInquiry.value)
        existingMsgs.push({
            sender: 'STAFF',
            senderName: staff.name,
            message: text,
            createdAt: new Date().toISOString()
        })

        updateLocalInquiry(
            selectedInquiry.value.inquiryId,
            {
                reply: text,
                repliedAt: new Date().toISOString(),
                status: 'In Progress',
                messages: existingMsgs
            }
        )

        replyText.value = ''
        showSuccess('Reply saved successfully.')
    } finally {
        saving.value = false
    }
}

/* =========================================================
   ASSIGN STAFF
========================================================= */

function openAssignModal(inquiry: Inquiry) {
    selectedInquiry.value = inquiry

    selectedStaffId.value =
        inquiry.assignedStaffId
            ? String(inquiry.assignedStaffId)
            : ''

    showAssignModal.value = true
}

async function assignStaff() {
    if (!selectedInquiry.value) {
        return
    }

    if (!selectedStaffId.value) {
        errorMessage.value =
            'Please select a support staff member.'

        return
    }

    saving.value = true
    errorMessage.value = ''

    try {
        await api.put(
            `/support/inquiries/${selectedInquiry.value.inquiryId}/assign`,
            {
                supportStaffId:
                    selectedStaffId.value
            }
        )

        const staff =
            supportStaff.value.find(function (item) {
                return String(
                    item.supportStaffId ??
                    item.id
                ) === selectedStaffId.value
            })

        updateLocalInquiry(
            selectedInquiry.value.inquiryId,
            {
                assignedStaffId:
                    selectedStaffId.value,

                assignedStaffName:
                    staff?.name || 'Support Staff'
            }
        )

        showAssignModal.value = false
        showSuccess('Inquiry assigned successfully.')
    } catch (error) {
        console.log('Assign API error:', error)

        const staff =
            supportStaff.value.find(function (item) {
                return String(
                    item.supportStaffId ??
                    item.id
                ) === selectedStaffId.value
            })

        updateLocalInquiry(
            selectedInquiry.value.inquiryId,
            {
                assignedStaffId:
                    selectedStaffId.value,

                assignedStaffName:
                    staff?.name || 'Support Staff'
            }
        )

        showAssignModal.value = false
        showSuccess('Inquiry assignment saved.')
    } finally {
        saving.value = false
    }
}

/* =========================================================
   STATUS
========================================================= */

async function updateStatus(
    inquiry: Inquiry,
    status: string
) {
    try {
        await api.put(
            `/support/inquiries/${inquiry.inquiryId}/status`,
            {
                status: status
            }
        )
    } catch (error) {
        console.log('Status API error:', error)
    }

    updateLocalInquiry(
        inquiry.inquiryId,
        {
            status: status
        }
    )

    showSuccess('Inquiry status updated.')
}

/* =========================================================
   PRIORITY
========================================================= */

async function updatePriority(
    inquiry: Inquiry,
    priority: string
) {
    try {
        await api.put(
            `/support/inquiries/${inquiry.inquiryId}/priority`,
            {
                priority: priority
            }
        )
    } catch (error) {
        console.log('Priority API error:', error)
    }

    updateLocalInquiry(
        inquiry.inquiryId,
        {
            priority: priority
        }
    )

    showSuccess('Inquiry priority updated.')
}

/* =========================================================
   DELETE
========================================================= */

function openDeleteModal(inquiry: Inquiry) {
    selectedInquiry.value = inquiry
    showDeleteModal.value = true
}

async function deleteInquiry() {
    if (!selectedInquiry.value) {
        return
    }

    saving.value = true

    try {
        await api.delete(
            `/support/inquiries/${selectedInquiry.value.inquiryId}`
        )
    } catch (error) {
        console.log('Delete API error:', error)
    }

    inquiries.value =
        inquiries.value.filter(function (item) {
            return (
                String(item.inquiryId) !==
                String(
                    selectedInquiry.value?.inquiryId
                )
            )
        })

    showDeleteModal.value = false
    selectedInquiry.value = null

    showSuccess('Inquiry deleted successfully.')

    if (
        currentPage.value > totalPages.value
    ) {
        currentPage.value = totalPages.value
    }

    saving.value = false
}

/* =========================================================
   LOCAL UPDATE
========================================================= */

function updateLocalInquiry(
    inquiryId: string | number,
    changes: Partial<Inquiry>
) {
    const index =
        inquiries.value.findIndex(function (item) {
            return (
                String(item.inquiryId) ===
                String(inquiryId)
            )
        })

    if (index === -1) {
        return
    }

    inquiries.value[index] = {
        ...inquiries.value[index],
        ...changes,
        updatedAt:
            new Date().toISOString()
    }

    if (
        selectedInquiry.value &&
        String(
            selectedInquiry.value.inquiryId
        ) === String(inquiryId)
    ) {
        selectedInquiry.value = {
            ...selectedInquiry.value,
            ...changes
        }
    }
}

/* =========================================================
   HELPERS
========================================================= */

function formatDate(date: string) {
    if (!date) {
        return '-'
    }

    const parsedDate = new Date(date)

    if (Number.isNaN(parsedDate.getTime())) {
        return date
    }

    return parsedDate.toLocaleDateString(
        'en-LK',
        {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        }
    )
}

function formatTime(date: string) {
    if (!date) {
        return ''
    }

    const parsedDate = new Date(date)

    if (Number.isNaN(parsedDate.getTime())) {
        return ''
    }

    return parsedDate.toLocaleTimeString(
        'en-LK',
        {
            hour: '2-digit',
            minute: '2-digit'
        }
    )
}

function getStatusClass(status: string) {
    switch (status) {
        case 'Open':
            return 'bg-blue-50 text-blue-700 border-blue-100'

        case 'Pending':
            return 'bg-amber-50 text-amber-700 border-amber-100'

        case 'In Progress':
            return 'bg-violet-50 text-violet-700 border-violet-100'

        case 'Resolved':
            return 'bg-emerald-50 text-emerald-700 border-emerald-100'

        case 'Closed':
            return 'bg-slate-100 text-slate-600 border-slate-200'

        default:
            return 'bg-slate-100 text-slate-600 border-slate-200'
    }
}

function getPriorityClass(priority: string) {
    switch (priority) {
        case 'Urgent':
            return 'bg-red-50 text-red-700 border-red-100'

        case 'High':
            return 'bg-orange-50 text-orange-700 border-orange-100'

        case 'Medium':
            return 'bg-amber-50 text-amber-700 border-amber-100'

        case 'Low':
            return 'bg-slate-100 text-slate-600 border-slate-200'

        default:
            return 'bg-slate-100 text-slate-600 border-slate-200'
    }
}

function getCategoryClass(category: string) {
    switch (category) {
        case 'Order':
            return 'bg-blue-50 text-blue-700'

        case 'Payment':
            return 'bg-emerald-50 text-emerald-700'

        case 'Delivery':
            return 'bg-cyan-50 text-cyan-700'

        case 'Return & Refund':
            return 'bg-orange-50 text-orange-700'

        case 'Product':
            return 'bg-violet-50 text-violet-700'

        case 'Account':
            return 'bg-pink-50 text-pink-700'

        case 'Technical':
            return 'bg-indigo-50 text-indigo-700'

        default:
            return 'bg-slate-100 text-slate-600'
    }
}

function getInitials(name: string) {
    if (!name) {
        return '?'
    }

    const words =
        name.trim().split(' ')

    if (words.length === 1) {
        return words[0]
            .substring(0, 2)
            .toUpperCase()
    }

    return (
        words[0][0] +
        words[words.length - 1][0]
    ).toUpperCase()
}

function showSuccess(message: string) {
    successMessage.value = message

    setTimeout(function () {
        successMessage.value = ''
    }, 3000)
}

function clearFilters() {
    searchQuery.value = ''
    selectedStatus.value = 'All'
    selectedPriority.value = 'All'
    selectedCategory.value = 'All'
    currentPage.value = 1
}

function refreshData() {
    loadInquiries()
    loadSupportStaff()
}

function closeAllModals() {
    showViewModal.value = false
    showReplyModal.value = false
    showAssignModal.value = false
    showDeleteModal.value = false
    selectedInquiry.value = null
}

/* =========================================================
   WATCH FILTER CHANGES
========================================================= */

function resetPagination() {
    currentPage.value = 1
}

/* =========================================================
   INITIAL LOAD
========================================================= */

onMounted(function () {
    loadInquiries()
    loadSupportStaff()
})
</script>

<template>
    <div class="relative min-h-screen bg-slate-100 overflow-x-hidden">

        <!-- =====================================================
         BACKGROUND
    ====================================================== -->

        <div class="fixed inset-0 pointer-events-none overflow-hidden">
            <div class="absolute -top-40 -right-40
               w-96 h-96
               bg-blue-200/30
               rounded-full
               blur-3xl"></div>

            <div class="absolute top-1/3 -left-40
               w-96 h-96
               bg-cyan-200/25
               rounded-full
               blur-3xl"></div>

            <div class="absolute -bottom-40 right-1/4
               w-96 h-96
               bg-indigo-200/20
               rounded-full
               blur-3xl"></div>
        </div>

        <!-- =====================================================
         SIDEBAR
    ====================================================== -->

        <AdminSidebar />

        <!-- =====================================================
         MAIN
    ====================================================== -->

        <main class="relative ml-64 min-h-screen p-8">

            <!-- ===================================================
           TOP HEADER
      ==================================================== -->

            <div class="flex flex-col xl:flex-row
               xl:items-center
               xl:justify-between
               gap-6 mb-8">

                <div>
                    <div class="flex items-center gap-2
                   text-sm text-slate-500 mb-2">
                        <span>Admin Panel</span>

                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                        </svg>

                        <span class="text-blue-600 font-medium">
                            Customer Support
                        </span>
                    </div>

                    <h1 class="text-3xl font-bold
                   text-slate-800 tracking-tight">
                        Customer Support
                    </h1>

                    <p class="text-slate-500 mt-1">
                        Manage customer inquiries,
                        support requests and resolutions.
                    </p>
                </div>

                <div class="flex items-center gap-3">

                    <!-- Online status -->

                    <div class="hidden sm:flex items-center gap-2
                   px-4 py-2.5
                   bg-white/70
                   backdrop-blur-xl
                   border border-white
                   rounded-xl
                   shadow-sm">
                        <span class="relative flex h-2.5 w-2.5">
                            <span class="animate-ping absolute
                       inline-flex h-full w-full
                       rounded-full
                       bg-emerald-400 opacity-75"></span>

                            <span class="relative inline-flex
                       rounded-full h-2.5 w-2.5
                       bg-emerald-500"></span>
                        </span>

                        <span class="text-sm font-semibold
                     text-slate-600">
                            Support System Online
                        </span>
                    </div>

                    <!-- Refresh -->

                    <button @click="refreshData" class="flex items-center gap-2
                   px-4 py-2.5
                   bg-white/80
                   hover:bg-white
                   border border-slate-200
                   rounded-xl
                   text-sm font-semibold
                   text-slate-600
                   shadow-sm
                   transition-all
                   hover:shadow-md">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h5
                   M20 20v-5h-5
                   M5.5 15A7 7 0 0019 9
                   M19 9a7 7 0 00-13.5-4" />
                        </svg>

                        Refresh
                    </button>

                </div>
            </div>

            <!-- ===================================================
           SUCCESS MESSAGE
      ==================================================== -->

            <Transition name="slide-down">
                <div v-if="successMessage" class="mb-6 flex items-center gap-3
                 rounded-xl
                 border border-emerald-200
                 bg-emerald-50
                 px-4 py-3
                 text-emerald-700
                 shadow-sm">
                    <div class="w-8 h-8 rounded-full
                   bg-emerald-100
                   flex items-center justify-center">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                        </svg>
                    </div>

                    <span class="font-medium text-sm">
                        {{ successMessage }}
                    </span>
                </div>
            </Transition>

            <!-- ===================================================
           ERROR MESSAGE
      ==================================================== -->

            <div v-if="errorMessage" class="mb-6 flex items-center
               justify-between
               rounded-xl
               border border-red-200
               bg-red-50
               px-4 py-3
               text-red-700">
                <div class="flex items-center gap-3">

                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v4
                 M12 17h.01
                 M10.29 3.86L1.82 18
                 a2 2 0 001.71 3h16.94
                 a2 2 0 001.71-3L13.71 3.86
                 a2 2 0 00-3.42 0z" />
                    </svg>

                    <span class="text-sm font-medium">
                        {{ errorMessage }}
                    </span>
                </div>

                <button @click="errorMessage = ''" class="text-red-500 hover:text-red-700">
                    ×
                </button>
            </div>

            <!-- ===================================================
           STAT CARDS
      ==================================================== -->

            <div class="grid grid-cols-1
               sm:grid-cols-2
               lg:grid-cols-5
               gap-5 mb-8">

                <!-- Total -->

                <div class="group bg-white/75
                 backdrop-blur-xl
                 border border-white
                 rounded-2xl
                 p-5
                 shadow-sm
                 hover:shadow-lg
                 hover:-translate-y-0.5
                 transition-all">
                    <div class="flex items-center justify-between">

                        <div>
                            <p class="text-xs font-semibold
                       uppercase tracking-wider
                       text-slate-400">
                                Total Inquiries
                            </p>

                            <p class="text-3xl font-bold
                       text-slate-800 mt-2">
                                {{ totalInquiries }}
                            </p>
                        </div>

                        <div class="w-11 h-11 rounded-xl
                     bg-blue-50
                     text-blue-600
                     flex items-center justify-center">
                            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h8
                     M8 14h5
                     M20 11.5a7.5 7.5 0 01-7.5 7.5
                     c-1.4 0-2.7-.38-3.82-1.04
                     L4 19l1.04-4.68
                     A7.5 7.5 0 1120 11.5z" />
                            </svg>
                        </div>

                    </div>
                </div>

                <!-- Open -->

                <div class="group bg-white/75
                 backdrop-blur-xl
                 border border-white
                 rounded-2xl
                 p-5
                 shadow-sm
                 hover:shadow-lg
                 hover:-translate-y-0.5
                 transition-all">
                    <div class="flex items-center justify-between">

                        <div>
                            <p class="text-xs font-semibold
                       uppercase tracking-wider
                       text-slate-400">
                                Open
                            </p>

                            <p class="text-3xl font-bold
                       text-blue-600 mt-2">
                                {{ openInquiries }}
                            </p>
                        </div>

                        <div class="w-11 h-11 rounded-xl
                     bg-blue-50
                     text-blue-600
                     flex items-center justify-center">
                            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 2
                     M21 12a9 9 0 11-18 0
                     9 9 0 0118 0z" />
                            </svg>
                        </div>

                    </div>
                </div>

                <!-- Pending -->

                <div class="group bg-white/75
                 backdrop-blur-xl
                 border border-white
                 rounded-2xl
                 p-5
                 shadow-sm
                 hover:shadow-lg
                 hover:-translate-y-0.5
                 transition-all">
                    <div class="flex items-center justify-between">

                        <div>
                            <p class="text-xs font-semibold
                       uppercase tracking-wider
                       text-slate-400">
                                Pending
                            </p>

                            <p class="text-3xl font-bold
                       text-amber-600 mt-2">
                                {{ pendingInquiries }}
                            </p>
                        </div>

                        <div class="w-11 h-11 rounded-xl
                     bg-amber-50
                     text-amber-600
                     flex items-center justify-center">
                            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6l4 2
                     M21 12a9 9 0 11-18 0
                     9 9 0 0118 0z" />
                            </svg>
                        </div>

                    </div>
                </div>

                <!-- In Progress -->

                <div class="group bg-white/75
                 backdrop-blur-xl
                 border border-white
                 rounded-2xl
                 p-5
                 shadow-sm
                 hover:shadow-lg
                 hover:-translate-y-0.5
                 transition-all">
                    <div class="flex items-center justify-between">

                        <div>
                            <p class="text-xs font-semibold
                       uppercase tracking-wider
                       text-slate-400">
                                In Progress
                            </p>

                            <p class="text-3xl font-bold
                       text-violet-600 mt-2">
                                {{ inProgressInquiries }}
                            </p>
                        </div>

                        <div class="w-11 h-11 rounded-xl
                     bg-violet-50
                     text-violet-600
                     flex items-center justify-center">
                            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v12
                     M6 12h12" />
                            </svg>
                        </div>

                    </div>
                </div>

                <!-- Resolved -->

                <div class="group bg-white/75
                 backdrop-blur-xl
                 border border-white
                 rounded-2xl
                 p-5
                 shadow-sm
                 hover:shadow-lg
                 hover:-translate-y-0.5
                 transition-all">
                    <div class="flex items-center justify-between">

                        <div>
                            <p class="text-xs font-semibold
                       uppercase tracking-wider
                       text-slate-400">
                                Resolved
                            </p>

                            <p class="text-3xl font-bold
                       text-emerald-600 mt-2">
                                {{ resolvedInquiries }}
                            </p>
                        </div>

                        <div class="w-11 h-11 rounded-xl
                     bg-emerald-50
                     text-emerald-600
                     flex items-center justify-center">
                            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                    d="M5 13l4 4L19 7" />
                            </svg>
                        </div>

                    </div>
                </div>

            </div>

            <!-- ===================================================
           MAIN TABLE CARD
      ==================================================== -->

            <div class="bg-white/75
               backdrop-blur-xl
               border border-white
               rounded-3xl
               shadow-sm
               overflow-hidden">

                <!-- =================================================
             TABLE HEADER
        ================================================== -->

                <div class="p-6 border-b border-slate-200/70">

                    <div class="flex flex-col
                   xl:flex-row
                   xl:items-center
                   xl:justify-between
                   gap-5">

                        <div>
                            <h2 class="text-lg font-bold
                       text-slate-800">
                                Support Inquiries
                            </h2>

                            <p class="text-sm text-slate-500 mt-1">
                                Review and manage customer
                                support requests.
                            </p>
                        </div>

                        <!-- Search -->

                        <div class="relative w-full
                     xl:w-80">
                            <svg class="absolute left-3.5 top-1/2
                       -translate-y-1/2
                       w-4 h-4
                       text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-4.35-4.35
                     M10.5 18a7.5 7.5 0
                     1 1 0-15 7.5 7.5
                     0 010 15z" />
                            </svg>

                            <input v-model="searchQuery" @input="resetPagination" type="text"
                                placeholder="Search inquiries..." class="w-full pl-10 pr-4 py-2.5
                       bg-slate-50/80
                       border border-slate-200
                       rounded-xl
                       text-sm
                       text-slate-700
                       placeholder:text-slate-400
                       outline-none
                       focus:ring-2
                       focus:ring-blue-500/20
                       focus:border-blue-400
                       transition" />
                        </div>

                    </div>

                    <!-- FILTERS -->

                    <div class="flex flex-wrap
                   items-center
                   gap-3 mt-5">

                        <!-- Status -->

                        <select v-model="selectedStatus" @change="resetPagination" class="px-3.5 py-2.5
                     bg-slate-50/80
                     border border-slate-200
                     rounded-xl
                     text-sm
                     font-medium
                     text-slate-600
                     outline-none
                     focus:ring-2
                     focus:ring-blue-500/20">
                            <option value="All">
                                All Status
                            </option>

                            <option v-for="status in statuses" :key="status" :value="status">
                                {{ status }}
                            </option>
                        </select>

                        <!-- Priority -->

                        <select v-model="selectedPriority" @change="resetPagination" class="px-3.5 py-2.5
                     bg-slate-50/80
                     border border-slate-200
                     rounded-xl
                     text-sm
                     font-medium
                     text-slate-600
                     outline-none
                     focus:ring-2
                     focus:ring-blue-500/20">
                            <option value="All">
                                All Priority
                            </option>

                            <option v-for="priority in priorities" :key="priority" :value="priority">
                                {{ priority }}
                            </option>
                        </select>

                        <!-- Category -->

                        <select v-model="selectedCategory" @change="resetPagination" class="px-3.5 py-2.5
                     bg-slate-50/80
                     border border-slate-200
                     rounded-xl
                     text-sm
                     font-medium
                     text-slate-600
                     outline-none
                     focus:ring-2
                     focus:ring-blue-500/20">
                            <option value="All">
                                All Categories
                            </option>

                            <option v-for="category in categories" :key="category" :value="category">
                                {{ category }}
                            </option>
                        </select>

                        <button v-if="
                            searchQuery ||
                            selectedStatus !== 'All' ||
                            selectedPriority !== 'All' ||
                            selectedCategory !== 'All'
                        " @click="clearFilters" class="px-4 py-2.5
                     rounded-xl
                     text-sm
                     font-semibold
                     text-blue-600
                     hover:bg-blue-50
                     transition">
                            Clear Filters
                        </button>

                    </div>
                </div>

                <!-- =================================================
             TABLE
        ================================================== -->

                <div class="overflow-x-auto">

                    <table class="w-full min-w-[1100px]">

                        <thead class="bg-slate-50/80
                     border-b
                     border-slate-200/70">
                            <tr>

                                <th class="px-6 py-4
                         text-left
                         text-[11px]
                         font-bold
                         uppercase
                         tracking-wider
                         text-slate-400">
                                    Inquiry
                                </th>

                                <th class="px-6 py-4
                         text-left
                         text-[11px]
                         font-bold
                         uppercase
                         tracking-wider
                         text-slate-400">
                                    Customer
                                </th>

                                <th class="px-6 py-4
                         text-left
                         text-[11px]
                         font-bold
                         uppercase
                         tracking-wider
                         text-slate-400">
                                    Category
                                </th>

                                <th class="px-6 py-4
                         text-left
                         text-[11px]
                         font-bold
                         uppercase
                         tracking-wider
                         text-slate-400">
                                    Priority
                                </th>

                                <th class="px-6 py-4
                         text-left
                         text-[11px]
                         font-bold
                         uppercase
                         tracking-wider
                         text-slate-400">
                                    Status
                                </th>

                                <th class="px-6 py-4
                         text-left
                         text-[11px]
                         font-bold
                         uppercase
                         tracking-wider
                         text-slate-400">
                                    Assigned To
                                </th>

                                <th class="px-6 py-4
                         text-left
                         text-[11px]
                         font-bold
                         uppercase
                         tracking-wider
                         text-slate-400">
                                    Date
                                </th>

                                <th class="px-6 py-4
                         text-right
                         text-[11px]
                         font-bold
                         uppercase
                         tracking-wider
                         text-slate-400">
                                    Actions
                                </th>

                            </tr>
                        </thead>

                        <tbody class="divide-y
                     divide-slate-100">

                            <!-- Loading -->

                            <tr v-if="loading">

                                <td colspan="8" class="px-6 py-16 text-center">

                                    <div class="flex flex-col
                           items-center">
                                        <div class="w-10 h-10
                             border-4
                             border-blue-100
                             border-t-blue-600
                             rounded-full
                             animate-spin"></div>

                                        <p class="text-sm
                             text-slate-500
                             mt-4">
                                            Loading inquiries...
                                        </p>
                                    </div>

                                </td>

                            </tr>

                            <!-- Empty -->

                            <tr v-else-if="
                                paginatedInquiries.length === 0
                            ">

                                <td colspan="8" class="px-6 py-16 text-center">

                                    <div class="flex flex-col
                           items-center">

                                        <div class="w-14 h-14
                             rounded-2xl
                             bg-slate-100
                             text-slate-400
                             flex items-center
                             justify-center">
                                            <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h8
                             M8 14h5
                             M20 11.5a7.5 7.5 0
                             01-7.5 7.5
                             c-1.4 0-2.7-.38-3.82-1.04
                             L4 19l1.04-4.68
                             A7.5 7.5 0
                             118.5 4
                             a7.5 7.5 0
                             0111.5 7.5z" />
                                            </svg>
                                        </div>

                                        <h3 class="font-semibold
                             text-slate-700
                             mt-4">
                                            No inquiries found
                                        </h3>

                                        <p class="text-sm
                             text-slate-400
                             mt-1">
                                            Try changing your search
                                            or filters.
                                        </p>

                                    </div>

                                </td>

                            </tr>

                            <!-- Rows -->

                            <tr v-for="inquiry in paginatedInquiries" :key="inquiry.inquiryId" class="group hover:bg-blue-50/30
                       transition-colors">

                                <!-- Inquiry -->

                                <td class="px-6 py-4">

                                    <div class="max-w-[270px]">

                                        <div class="flex items-center
                             gap-2">
                                            <span class="font-semibold
                               text-slate-800
                               text-sm">
                                                {{ inquiry.subject }}
                                            </span>
                                        </div>

                                        <p class="text-xs
                             text-slate-400
                             mt-1">
                                            #{{ inquiry.inquiryId }}
                                        </p>

                                    </div>

                                </td>

                                <!-- Customer -->

                                <td class="px-6 py-4">

                                    <div class="flex items-center
                           gap-3">

                                        <img v-if="inquiry.customerImage" :src="inquiry.customerImage" :alt="inquiry.customerName" class="w-9 h-9 rounded-full object-cover shadow-sm shrink-0" />
                                        <div v-else class="w-9 h-9
                             rounded-full
                             bg-gradient-to-br
                             from-blue-500
                             to-cyan-500
                             text-white
                             flex items-center
                             justify-center
                             text-xs
                             font-bold
                             shadow-sm shrink-0">
                                            {{ getInitials(
                                                inquiry.customerName
                                            ) }}
                                        </div>

                                        <div>

                                            <p class="text-sm
                               font-semibold
                               text-slate-700">
                                                {{ inquiry.customerName }}
                                            </p>

                                            <p class="text-xs
                               text-slate-400
                               mt-0.5">
                                                {{ inquiry.customerEmail }}
                                            </p>

                                        </div>

                                    </div>

                                </td>

                                <!-- Category -->

                                <td class="px-6 py-4">

                                    <span class="inline-flex
                           items-center
                           px-2.5 py-1
                           rounded-lg
                           text-xs
                           font-semibold" :class="getCategoryClass(
                            inquiry.category
                        )
                            ">
                                        {{ inquiry.category }}
                                    </span>

                                </td>

                                <!-- Priority -->

                                <td class="px-6 py-4">

                                    <select :value="inquiry.priority" @change="
                                        updatePriority(
                                            inquiry,
                                            ($event.target as HTMLSelectElement).value
                                        )
                                        " class="px-2.5 py-1.5
                           rounded-lg
                           border
                           text-xs
                           font-semibold
                           outline-none
                           bg-white" :class="getPriorityClass(
                            inquiry.priority
                        )
                            ">

                                        <option v-for="priority in priorities" :key="priority" :value="priority">
                                            {{ priority }}
                                        </option>

                                    </select>

                                </td>

                                <!-- Status -->

                                <td class="px-6 py-4">

                                    <select :value="inquiry.status" @change="
                                        updateStatus(
                                            inquiry,
                                            ($event.target as HTMLSelectElement).value
                                        )
                                        " class="px-2.5 py-1.5
                           rounded-lg
                           border
                           text-xs
                           font-semibold
                           outline-none
                           bg-white" :class="getStatusClass(
                            inquiry.status
                        )
                            ">

                                        <option v-for="status in statuses" :key="status" :value="status">
                                            {{ status }}
                                        </option>

                                    </select>

                                </td>

                                <!-- Assigned -->

                                <td class="px-6 py-4">

                                    <button @click="openAssignModal(inquiry)" class="group/assign
                           flex items-center gap-2
                           text-left">

                                        <div v-if="inquiry.assignedStaffName" class="w-8 h-8
                             rounded-full
                             bg-violet-100
                             text-violet-700
                             flex items-center
                             justify-center
                             text-xs
                             font-bold">
                                            {{
                                                getInitials(
                                                    inquiry.assignedStaffName
                                            )
                                            }}
                                        </div>

                                        <div v-else class="w-8 h-8
                             rounded-full
                             border border-dashed
                             border-slate-300
                             text-slate-400
                             flex items-center
                             justify-center">
                                            +
                                        </div>

                                        <div>

                                            <p class="text-xs
                               font-semibold
                               text-slate-600
                               group-hover/assign:text-blue-600">
                                                {{
                                                    inquiry.assignedStaffName ||
                                                'Assign Staff'
                                                }}
                                            </p>

                                        </div>

                                    </button>

                                </td>

                                <!-- Date -->

                                <td class="px-6 py-4">

                                    <p class="text-sm
                           font-medium
                           text-slate-600">
                                        {{ formatDate(
                                            inquiry.createdAt
                                        ) }}
                                    </p>

                                    <p class="text-xs
                           text-slate-400 mt-0.5">
                                        {{ formatTime(
                                            inquiry.createdAt
                                        ) }}
                                    </p>

                                </td>

                                <!-- Actions -->

                                <td class="px-6 py-4">

                                    <div class="flex justify-end
                           items-center gap-1">

                                        <!-- View -->

                                        <button @click="viewInquiry(inquiry)" title="View Inquiry" class="p-2
                             rounded-lg
                             text-slate-500
                             hover:text-blue-600
                             hover:bg-blue-50
                             transition">
                                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.5 12s3.5-6
                             9.5-6 9.5 6
                             9.5 6-3.5 6
                             -9.5 6-9.5-6z
                             M12 15a3 3 0
                             100-6 3 3 0
                             000 6z" />
                                            </svg>
                                        </button>

                                        <!-- Reply -->

                                        <button @click="
                                            openReplyModal(inquiry)
                                            " title="Reply" class="p-2
                             rounded-lg
                             text-slate-500
                             hover:text-violet-600
                             hover:bg-violet-50
                             transition">
                                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 11.5a8.38 8.38
                             0 01-.9 3.8
                             8.5 8.5 0
                             01-7.6 4.7
                             8.38 8.38
                             0 01-3.8-.9
                             L3 21l1.9-5.7
                             A8.38 8.38
                             0 014 11.5
                             8.5 8.5
                             0112.5 3
                             a8.5 8.5
                             0 018.5 8.5z" />
                                            </svg>
                                        </button>

                                        <!-- Delete -->

                                        <button @click="
                                            openDeleteModal(inquiry)
                                            " title="Delete" class="p-2
                             rounded-lg
                             text-slate-500
                             hover:text-red-600
                             hover:bg-red-50
                             transition">
                                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 6h18
                             M8 6V4h8v2
                             M19 6l-1 14H6L5 6
                             M10 11v5
                             M14 11v5" />
                                            </svg>
                                        </button>

                                    </div>

                                </td>

                            </tr>

                        </tbody>

                    </table>

                </div>

                <!-- =================================================
             PAGINATION
        ================================================== -->

                <div class="px-6 py-4
                 border-t
                 border-slate-200/70
                 flex flex-col
                 sm:flex-row
                 sm:items-center
                 sm:justify-between
                 gap-4">

                    <p class="text-sm
                   text-slate-500">
                        Showing

                        <span class="font-semibold
                     text-slate-700">
                            {{
                                filteredInquiries.length === 0
                                    ? 0
                                    : (currentPage - 1) *
                            itemsPerPage + 1
                            }}
                        </span>

                        to

                        <span class="font-semibold
                     text-slate-700">
                            {{
                                Math.min(
                                    currentPage *
                                    itemsPerPage,
                                    filteredInquiries.length
                            )
                            }}
                        </span>

                        of

                        <span class="font-semibold
                     text-slate-700">
                            {{ filteredInquiries.length }}
                        </span>

                        inquiries
                    </p>

                    <div class="flex items-center gap-1">

                        <button @click="goToPage(currentPage - 1)" :disabled="currentPage === 1" class="w-9 h-9
                     rounded-lg
                     border border-slate-200
                     bg-white
                     text-slate-500
                     flex items-center
                     justify-center
                     hover:bg-slate-50
                     disabled:opacity-40
                     disabled:cursor-not-allowed
                     transition">
                            ‹
                        </button>

                        <button v-for="page in pageNumbers" :key="page" @click="goToPage(page)" class="w-9 h-9
                     rounded-lg
                     text-sm
                     font-semibold
                     transition" :class="currentPage === page
                            ? 'bg-blue-600 text-white shadow-sm'
                            : 'bg-white border border-slate-200 text-slate-600 hover:bg-blue-50 hover:text-blue-600'
                        ">
                            {{ page }}
                        </button>

                        <button @click="goToPage(currentPage + 1)" :disabled="currentPage === totalPages
                            " class="w-9 h-9
                     rounded-lg
                     border border-slate-200
                     bg-white
                     text-slate-500
                     flex items-center
                     justify-center
                     hover:bg-slate-50
                     disabled:opacity-40
                     disabled:cursor-not-allowed
                     transition">
                            ›
                        </button>

                    </div>

                </div>

            </div>

        </main>

        <!-- =====================================================
         VIEW INQUIRY MODAL
    ====================================================== -->

        <Transition name="modal">

            <div v-if="
                showViewModal &&
                selectedInquiry
            " class="fixed inset-0 z-50
               flex items-center
               justify-center
               p-4">

                <div class="absolute inset-0
                 bg-slate-900/40
                 backdrop-blur-sm" @click="closeAllModals"></div>

                <div class="relative w-full
                 max-w-2xl
                 max-h-[90vh]
                 overflow-y-auto
                 bg-white
                 rounded-3xl
                 shadow-2xl
                 border border-white">

                    <!-- Modal Header -->

                    <div class="px-6 py-5
                   border-b border-slate-100
                   flex items-start
                   justify-between">

                        <div>

                            <div class="flex items-center gap-2 mb-2">
                                <span class="text-xs
                         font-bold
                         text-blue-600
                         uppercase
                         tracking-wider">
                                    Inquiry #{{
                                        selectedInquiry.inquiryId
                                    }}
                                </span>
                            </div>

                            <h2 class="text-xl font-bold
                       text-slate-800">
                                {{ selectedInquiry.subject }}
                            </h2>

                        </div>

                        <button @click="closeAllModals" class="w-9 h-9
                     rounded-xl
                     bg-slate-100
                     text-slate-500
                     hover:bg-slate-200
                     flex items-center
                     justify-center
                     transition">
                            ×
                        </button>

                    </div>

                    <div class="p-6 space-y-6">

                        <!-- Customer -->

                        <div class="rounded-2xl
                     bg-slate-50
                     border border-slate-100
                     p-5">

                            <p class="text-xs
                       uppercase
                       tracking-wider
                       font-bold
                       text-slate-400
                       mb-4">
                                Customer Information
                            </p>

                            <div class="flex items-center gap-4">

                                <img v-if="selectedInquiry.customerImage" :src="selectedInquiry.customerImage" :alt="selectedInquiry.customerName" class="w-12 h-12 rounded-xl object-cover" />
                                <div v-else class="w-12 h-12
                         rounded-xl
                         bg-gradient-to-br
                         from-blue-500
                         to-cyan-500
                         text-white
                         flex items-center
                         justify-center
                         font-bold">
                                    {{
                                        getInitials(
                                            selectedInquiry.customerName
                                    )
                                    }}
                                </div>

                                <div>

                                    <p class="font-bold
                           text-slate-800">
                                        {{
                                            selectedInquiry.customerName
                                        }}
                                    </p>

                                    <p class="text-sm
                           text-slate-500">
                                        {{
                                            selectedInquiry.customerEmail
                                        }}
                                    </p>

                                    <p v-if="
                                        selectedInquiry.customerPhone
                                    " class="text-sm
                           text-slate-500">
                                        {{
                                            selectedInquiry.customerPhone
                                        }}
                                    </p>

                                </div>

                            </div>

                        </div>

                        <!-- Meta -->

                        <div class="grid grid-cols-1
                     sm:grid-cols-3 gap-4">

                            <div class="p-4
                       rounded-xl
                       bg-slate-50
                       border border-slate-100">
                                <p class="text-xs
                         text-slate-400
                         font-semibold">
                                    Category
                                </p>

                                <span class="inline-flex
                         mt-2
                         px-2.5 py-1
                         rounded-lg
                         text-xs
                         font-semibold" :class="getCategoryClass(
                            selectedInquiry.category
                        )
                            ">
                                    {{
                                        selectedInquiry.category
                                    }}
                                </span>
                            </div>

                            <div class="p-4
                       rounded-xl
                       bg-slate-50
                       border border-slate-100">
                                <p class="text-xs
                         text-slate-400
                         font-semibold">
                                    Priority
                                </p>

                                <span class="inline-flex
                         mt-2
                         px-2.5 py-1
                         rounded-lg
                         border
                         text-xs
                         font-semibold" :class="getPriorityClass(
                            selectedInquiry.priority
                        )
                            ">
                                    {{
                                        selectedInquiry.priority
                                    }}
                                </span>
                            </div>

                            <div class="p-4
                       rounded-xl
                       bg-slate-50
                       border border-slate-100">
                                <p class="text-xs
                         text-slate-400
                         font-semibold">
                                    Status
                                </p>

                                <span class="inline-flex
                         mt-2
                         px-2.5 py-1
                         rounded-lg
                         border
                         text-xs
                         font-semibold" :class="getStatusClass(
                            selectedInquiry.status
                        )
                            ">
                                    {{
                                        selectedInquiry.status
                                    }}
                                </span>
                            </div>

                        </div>

                        <!-- Message Thread History -->
                        <div class="space-y-4">
                            <p class="text-sm font-bold text-slate-700">
                                Conversation Thread
                            </p>

                            <!-- Fallback if no messages array -->
                            <template v-if="!selectedInquiry.messages || selectedInquiry.messages.length === 0">
                                <div>
                                    <div class="flex items-center justify-between mb-2">
                                        <p class="text-xs font-semibold text-blue-600">Customer Message</p>
                                        <span class="text-xs text-slate-400">{{ formatDate(selectedInquiry.createdAt) }}</span>
                                    </div>
                                    <div class="rounded-2xl bg-blue-50/60 border border-blue-100 p-4 text-sm leading-6 text-slate-700">
                                        {{ selectedInquiry.message }}
                                    </div>
                                </div>

                                <div v-if="selectedInquiry.reply" class="mt-4">
                                    <div class="flex items-center justify-between mb-2">
                                        <p class="text-xs font-semibold text-emerald-600">Support Response</p>
                                        <span v-if="selectedInquiry.repliedAt" class="text-xs text-slate-400">{{ formatDate(selectedInquiry.repliedAt) }}</span>
                                    </div>
                                    <div class="rounded-2xl bg-emerald-50/60 border border-emerald-100 p-4 text-sm leading-6 text-slate-700">
                                        {{ selectedInquiry.reply }}
                                    </div>
                                </div>
                            </template>

                            <!-- Message timeline if messages exist -->
                            <template v-else>
                                <div v-for="(msg, idx) in selectedInquiry.messages" :key="idx" class="space-y-1">
                                    <div class="flex items-center justify-between">
                                        <span class="text-xs font-bold" :class="msg.sender === 'CUSTOMER' ? 'text-blue-600' : 'text-emerald-600'">
                                            {{ msg.senderName || (msg.sender === 'CUSTOMER' ? 'Customer' : 'Support Representative') }}
                                        </span>
                                        <span v-if="msg.createdAt" class="text-[11px] text-slate-400">{{ formatDate(msg.createdAt) }} {{ formatTime(msg.createdAt) }}</span>
                                    </div>
                                    <div class="rounded-2xl p-4 text-sm leading-6 border" :class="msg.sender === 'CUSTOMER' ? 'bg-blue-50/70 border-blue-100 text-slate-700' : 'bg-emerald-50/70 border-emerald-100 text-slate-700'">
                                        {{ msg.message }}
                                    </div>
                                </div>
                            </template>
                        </div>

                    </div>

                    <!-- Integrated Staff Reply Form at Modal Footer -->
                    <div class="p-4 border-t border-slate-100 bg-slate-50/80">
                        <form @submit.prevent="sendReply" class="flex flex-col sm:flex-row items-stretch gap-2">
                            <textarea
                                v-model="replyText"
                                rows="2"
                                placeholder="Type a staff response message..."
                                required
                                :disabled="saving || selectedInquiry.status === 'Closed'"
                                class="flex-1 rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs text-slate-800 placeholder-slate-400 outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-500/10 transition resize-none disabled:bg-slate-100" />
                            <div class="flex items-center justify-end gap-2 shrink-0">
                                <button
                                    type="button"
                                    @click="closeAllModals"
                                    class="px-4 py-2.5 rounded-xl bg-slate-200 hover:bg-slate-300 text-slate-700 font-semibold text-xs transition">
                                    Close
                                </button>
                                <button
                                    type="submit"
                                    :disabled="saving || !replyText.trim() || selectedInquiry.status === 'Closed'"
                                    class="rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:opacity-95 text-white font-bold text-xs px-5 py-2.5 shadow-md shadow-blue-500/20 transition disabled:opacity-50 flex items-center justify-center gap-1.5">
                                    <span v-if="!saving">Send Response</span>
                                    <span v-else>Sending...</span>
                                </button>
                            </div>
                        </form>
                    </div>

                </div>

            </div>

        </Transition>



        <!-- =====================================================
         ASSIGN STAFF MODAL
    ====================================================== -->

        <Transition name="modal">

            <div v-if="
                showAssignModal &&
                selectedInquiry
            " class="fixed inset-0 z-50
               flex items-center
               justify-center
               p-4">

                <div class="absolute inset-0
                 bg-slate-900/40
                 backdrop-blur-sm" @click="closeAllModals"></div>

                <div class="relative w-full
                 max-w-md
                 bg-white
                 rounded-3xl
                 shadow-2xl">

                    <div class="p-6">

                        <div class="w-12 h-12
                     rounded-2xl
                     bg-violet-50
                     text-violet-600
                     flex items-center
                     justify-center
                     mb-4">
                            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 21v-2
                     a4 4 0 00-4-4H6
                     a4 4 0 00-4 4v2
                     M9 11a4 4 0
                     100-8 4 4 0
                     000 8z
                     M22 21v-2
                     a4 4 0 00-3-3.87
                     M16 3.13a4 4 0
                     010 7.75" />
                            </svg>
                        </div>

                        <h2 class="text-xl
                     font-bold
                     text-slate-800">
                            Assign Support Staff
                        </h2>

                        <p class="text-sm
                     text-slate-500
                     mt-1 mb-6">
                            Assign this inquiry to a
                            support team member.
                        </p>

                        <select v-model="selectedStaffId" class="w-full
                     px-4 py-3
                     rounded-xl
                     border border-slate-200
                     bg-slate-50
                     text-sm
                     text-slate-700
                     outline-none
                     focus:border-blue-400
                     focus:ring-4
                     focus:ring-blue-500/10">

                            <option value="">
                                Select Support Staff
                            </option>

                            <option v-for="staff in supportStaff" :key="staff.supportStaffId ??
                                staff.id
                                " :value="String(
                    staff.supportStaffId ??
                    staff.id
                )
                    ">
                                {{
                                    staff.name ||
                                    staff.email ||
                                'Support Staff'
                                }}
                            </option>

                        </select>

                    </div>

                    <div class="px-6 py-4
                   border-t border-slate-100
                   flex justify-end gap-3">

                        <button @click="closeAllModals" class="px-5 py-2.5
                     rounded-xl
                     bg-slate-100
                     text-slate-600
                     text-sm
                     font-semibold">
                            Cancel
                        </button>

                        <button @click="assignStaff" :disabled="saving" class="px-5 py-2.5
                     rounded-xl
                     bg-violet-600
                     text-white
                     text-sm
                     font-semibold
                     hover:bg-violet-700
                     disabled:opacity-50">
                            {{
                                saving
                                    ? 'Assigning...'
                                    : 'Assign Staff'
                            }}
                        </button>

                    </div>

                </div>

            </div>

        </Transition>

        <!-- =====================================================
             DELETE INQUIRY MODAL (ORDER MANAGEMENT DESIGN)
             ===================================================== -->
        <AdminConfirmModal
            v-model:show="showDeleteModal"
            type="danger"
            title="Delete Inquiry?"
            message="Are you sure you want to delete inquiry"
            :target="selectedInquiry ? `#${selectedInquiry.inquiryId}` : ''"
            description="This customer inquiry will be permanently deleted. This action cannot be undone."
            confirm-text="Delete Inquiry"
            cancel-text="Cancel"
            :loading="saving"
            @confirm="deleteInquiry"
        />

    </div>
</template>

<style scoped>
/* =========================================================
   MODAL ANIMATION
========================================================= */

.modal-enter-active,
.modal-leave-active {
    transition: opacity 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
    opacity: 0;
}

.modal-enter-active>div:last-child,
.modal-leave-active>div:last-child {
    transition:
        transform 0.2s ease,
        opacity 0.2s ease;
}

.modal-enter-from>div:last-child {
    opacity: 0;
    transform: translateY(10px) scale(0.98);
}

.modal-leave-to>div:last-child {
    opacity: 0;
    transform: translateY(10px) scale(0.98);
}

/* =========================================================
   SUCCESS ANIMATION
========================================================= */

.slide-down-enter-active,
.slide-down-leave-active {
    transition:
        opacity 0.25s ease,
        transform 0.25s ease;
}

.slide-down-enter-from,
.slide-down-leave-to {
    opacity: 0;
    transform: translateY(-8px);
}

/* =========================================================
   SCROLLBAR
========================================================= */

::-webkit-scrollbar {
    width: 7px;
    height: 7px;
}

::-webkit-scrollbar-track {
    background: transparent;
}

::-webkit-scrollbar-thumb {
    background: #cbd5e1;
    border-radius: 999px;
}

::-webkit-scrollbar-thumb:hover {
    background: #94a3b8;
}

/* =========================================================
   MOBILE
========================================================= */

@media (max-width: 1024px) {
    main {
        margin-left: 0;
        padding: 1.25rem;
    }
}
</style>