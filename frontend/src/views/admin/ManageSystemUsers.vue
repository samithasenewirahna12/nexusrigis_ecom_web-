<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import api from '../../services/api'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminConfirmModal, { type ModalType, type AdminModalState } from '../../components/admin/AdminConfirmModal.vue'
import { isSuperAdmin } from '../../utils/rbac'

/* =========================================================
   TYPES
   ========================================================= */

type UserRole =
    | 'Administrator'
    | 'Delivery Staff'
    | 'Support Staff'
    | 'Warehouse Staff'

type UserStatus = 'Active' | 'Inactive'

/** What the page works with */
interface SystemUser {
    id: string
    name: string
    email: string
    phone: string
    role: UserRole
    department: string
    userImage: string
    status: UserStatus
    joinedDate: string
    lastLogin: string
    accessLevel: string
    warehouseLoc: string
    supportDept: string
    vehicleNo: string
    licenseNo: string
}

/** What GET /api/admin/system-users returns */
interface SystemUserApi {
    userId: string
    name: string
    email: string
    phone?: string | null
    userImage?: string | null
    role: string
    enabled: boolean
    registeredDate?: string | null
    accessLevel?: string | null
    warehouseLoc?: string | null
    department?: string | null
    vehicleNo?: string | null
    licenseNo?: string | null
}

/* =========================================================
   API
   ========================================================= */

// "api" already has the /api base URL (same as AdminProfile uses '/administrators')
const USER_API = '/admin/system-users'

/* =========================================================
   USERS (loaded from the API)
   ========================================================= */

const users = ref<SystemUser[]>([])

/* =========================================================
   STATE
   ========================================================= */

const searchQuery = ref('')
const roleFilter = ref('All')
const statusFilter = ref('All')

const isLoading = ref(false)
const isSaving = ref(false)

const showModal = ref(false)
const isEditing = ref(false)
const editingUserId = ref<string | null>(null)

const showViewModal = ref(false)
const selectedUser = ref<SystemUser | null>(null)

/* =========================================================
   FORM
   ========================================================= */

const emptyForm = () => ({
    name: '',
    email: '',
    phone: '',
    password: '',
    role: 'Delivery Staff' as UserRole,
    accessLevel: '',
    warehouseLoc: '',
    supportDept: '',
    vehicleNo: '',
    licenseNo: '',
    userImage: '',
    status: 'Active' as UserStatus
})

const userForm = ref(emptyForm())

/* =========================================================
   FILTERED USERS
   ========================================================= */

const filteredUsers = computed(() => {
    return users.value.filter((user) => {
        const search = searchQuery.value.toLowerCase().trim()

        const matchesSearch =
            user.name.toLowerCase().includes(search) ||
            user.email.toLowerCase().includes(search) ||
            user.phone.toLowerCase().includes(search) ||
            user.department.toLowerCase().includes(search)

        const matchesRole =
            roleFilter.value === 'All' ||
            user.role === roleFilter.value

        const matchesStatus =
            statusFilter.value === 'All' ||
            user.status === statusFilter.value

        return matchesSearch && matchesRole && matchesStatus
    })
})

/* =========================================================
   STATISTICS
   ========================================================= */

const totalUsers = computed(() => {
    return users.value.length
})

const activeUsers = computed(() => {
    return users.value.filter(
        user => user.status === 'Active'
    ).length
})

const inactiveUsers = computed(() => {
    return users.value.filter(
        user => user.status === 'Inactive'
    ).length
})

const administratorCount = computed(() => {
    return users.value.filter(
        user => user.role === 'Administrator'
    ).length
})

const staffCount = computed(() => {
    return users.value.filter(
        user => user.role !== 'Administrator'
    ).length
})

/* =========================================================
   ROLE CLASS
   ========================================================= */

const getRoleClass = (role: UserRole) => {
    if (role === 'Administrator') {
        return 'bg-blue-50 text-blue-700 border-blue-200'
    }

    if (role === 'Delivery Staff') {
        return 'bg-cyan-50 text-cyan-700 border-cyan-200'
    }

    if (role === 'Support Staff') {
        return 'bg-violet-50 text-violet-700 border-violet-200'
    }

    return 'bg-emerald-50 text-emerald-700 border-emerald-200'
}

/* =========================================================
   STATUS CLASS
   ========================================================= */

const getStatusClass = (status: UserStatus) => {
    return status === 'Active'
        ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
        : 'bg-slate-100 text-slate-600 border-slate-200'
}

/* =========================================================
   API MAPPING
   ========================================================= */

const ROLE_TO_API: Record<UserRole, string> = {
    'Administrator': 'ADMINISTRATOR',
    'Delivery Staff': 'DELIVERY_STAFF',
    'Support Staff': 'SUPPORT_STAFF',
    'Warehouse Staff': 'WAREHOUSE_STAFF'
}

const ROLE_FROM_API: Record<string, UserRole> = {
    ADMINISTRATOR: 'Administrator',
    DELIVERY_STAFF: 'Delivery Staff',
    SUPPORT_STAFF: 'Support Staff',
    WAREHOUSE_STAFF: 'Warehouse Staff'
}

const avatarFor = (name: string) =>
    `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=2563eb&color=fff`

const mapUser = (u: SystemUserApi): SystemUser => {
    const role = ROLE_FROM_API[u.role] ?? 'Support Staff'

    let department = 'Administration'
    if (role === 'Delivery Staff') department = 'Delivery'
    if (role === 'Support Staff') department = u.department || 'Customer Support'
    if (role === 'Warehouse Staff') department = u.warehouseLoc || 'Warehouse'

    return {
        id: u.userId,
        name: u.name,
        email: u.email,
        phone: u.phone || '',
        role,
        department,
        userImage: u.userImage || avatarFor(u.name),
        status: u.enabled ? 'Active' : 'Inactive',
        joinedDate: u.registeredDate || '-',
        lastLogin: '-', // not tracked by the backend yet
        accessLevel: u.accessLevel || '',
        warehouseLoc: u.warehouseLoc || '',
        supportDept: u.department || '',
        vehicleNo: u.vehicleNo || '',
        licenseNo: u.licenseNo || ''
    }
}

const replaceUser = (updated: SystemUser) => {
    const index = users.value.findIndex(item => item.id === updated.id)

    if (index !== -1) {
        users.value[index] = updated
    }
}

const getErrorMessage = (error: any, fallback: string) =>
    error?.response?.data?.message || fallback

/** Role-specific fields the backend requires for the selected role */
const roleFieldsError = (): string => {
    const f = userForm.value

    if (f.role === 'Administrator' && !f.accessLevel.trim()) return 'Please select the access level.'
    if (f.role === 'Warehouse Staff' && !f.warehouseLoc.trim()) return 'Please enter the warehouse location.'
    if (f.role === 'Support Staff' && !f.supportDept.trim()) return 'Please enter the support department.'
    if (f.role === 'Delivery Staff' && !f.vehicleNo.trim()) return 'Please enter the vehicle number.'
    if (f.role === 'Delivery Staff' && !f.licenseNo.trim()) return 'Please enter the license number.'

    return ''
}

const buildPayload = () => {
    const f = userForm.value

    const payload: Record<string, any> = {
        name: f.name.trim(),
        email: f.email.trim(),
        phone: f.phone.trim(),
        userImage: f.userImage.trim(),
        enabled: f.status === 'Active'
    }

    if (f.role === 'Administrator') payload.accessLevel = f.accessLevel.trim()
    if (f.role === 'Warehouse Staff') payload.warehouseLoc = f.warehouseLoc.trim()
    if (f.role === 'Support Staff') payload.department = f.supportDept.trim()
    if (f.role === 'Delivery Staff') {
        payload.vehicleNo = f.vehicleNo.trim()
        payload.licenseNo = f.licenseNo.trim()
    }

    return payload
}

/* =========================================================
   CONFIRMATION & ALERT MODAL STATE (ORDER MANAGEMENT DESIGN)
   ========================================================= */

const confirmModal = ref<AdminModalState>({
    show: false,
    type: 'danger',
    title: '',
    message: '',
    target: '',
    description: '',
    confirmText: 'Confirm',
    cancelText: 'Cancel',
    showCancel: true,
    loading: false,
    onConfirm: () => {}
})

const showAlert = (title: string, message: string, type: ModalType = 'warning', description = '') => {
    confirmModal.value = {
        show: true,
        type,
        title,
        message,
        target: '',
        description,
        confirmText: 'OK',
        cancelText: '',
        showCancel: false,
        loading: false,
        onConfirm: () => {
            confirmModal.value.show = false
        }
    }
}

/* =========================================================
   OPEN ADD MODAL
   ========================================================= */

const openAddModal = () => {
    isEditing.value = false
    editingUserId.value = null

    userForm.value = emptyForm()

    showModal.value = true
}

/* =========================================================
   OPEN EDIT MODAL
   ========================================================= */

const openEditModal = (user: SystemUser) => {
    if (isSuperAdmin(user)) {
        showAlert('Protected Account', 'Super Admin is protected and cannot be edited.', 'warning', 'Administrators with FULL_ACCESS cannot be modified.')
        return
    }

    isEditing.value = true
    editingUserId.value = user.id

    userForm.value = {
        name: user.name,
        email: user.email,
        phone: user.phone,
        password: '',
        role: user.role,
        accessLevel: user.accessLevel,
        warehouseLoc: user.warehouseLoc,
        supportDept: user.supportDept,
        vehicleNo: user.vehicleNo,
        licenseNo: user.licenseNo,
        userImage: user.userImage,
        status: user.status
    }

    showModal.value = true
}

/* =========================================================
   CLOSE MODAL
   ========================================================= */

const closeModal = () => {
    showModal.value = false
}

/* =========================================================
   VIEW USER
   ========================================================= */

const openViewModal = (user: SystemUser) => {
    selectedUser.value = user
    showViewModal.value = true
}

const closeViewModal = () => {
    showViewModal.value = false
    selectedUser.value = null
}

/* =========================================================
   EDIT SELECTED USER
   FIX FOR VUE TEMPLATE PARSER ERROR
   ========================================================= */

const editSelectedUser = () => {
    if (!selectedUser.value) {
        return
    }

    const user = selectedUser.value
    if (isSuperAdmin(user)) {
        showAlert('Protected Account', 'Super Admin is protected and cannot be edited.', 'warning', 'Administrators with FULL_ACCESS cannot be modified.')
        return
    }

    closeViewModal()
    openEditModal(user)
}

/* =========================================================
   SAVE USER
   ========================================================= */

const saveUser = async () => {
    if (!userForm.value.name.trim()) {
        showAlert('Validation Error', 'Please enter the user name.', 'warning')
        return
    }

    if (!userForm.value.email.trim()) {
        showAlert('Validation Error', 'Please enter the email address.', 'warning')
        return
    }

    if (!userForm.value.phone.trim()) {
        showAlert('Validation Error', 'Please enter the phone number.', 'warning')
        return
    }

    const roleError = roleFieldsError()

    if (roleError) {
        showAlert('Validation Error', roleError, 'warning')
        return
    }

    const password = userForm.value.password

    if (!isEditing.value && password.length < 6) {
        showAlert('Validation Error', 'Password must contain at least 6 characters.', 'warning')
        return
    }

    if (isEditing.value && password && password.length < 6) {
        showAlert('Validation Error', 'New password must contain at least 6 characters.', 'warning')
        return
    }

    isSaving.value = true

    try {
        if (isEditing.value && editingUserId.value !== null) {
            const targetUser = users.value.find(u => u.id === editingUserId.value)
            if (isSuperAdmin(targetUser)) {
                showAlert('Protected Account', 'Super Admin is protected: details and access level cannot be edited.', 'warning')
                return
            }

            const payload = buildPayload()

            // Only send a password when the admin typed a new one
            if (password) {
                payload.password = password
            }

            const response = await api.put(
                `${USER_API}/${encodeURIComponent(editingUserId.value)}`,
                payload
            )

            replaceUser(mapUser(response.data))
        } else {
            const response = await api.post(USER_API, {
                ...buildPayload(),
                role: ROLE_TO_API[userForm.value.role],
                password
            })

            users.value.unshift(mapUser(response.data))
        }

        closeModal()
    } catch (error) {
        console.error('Error saving system user:', error)

        showAlert('Save Failed', getErrorMessage(error, 'Failed to save system user.'), 'danger')
    } finally {
        isSaving.value = false
    }
}

/* =========================================================
   TOGGLE STATUS
   ========================================================= */

const toggleUserStatus = (user: SystemUser) => {
    if (isSuperAdmin(user)) {
        confirmModal.value = {
            show: true,
            type: 'warning',
            title: 'Protected Account',
            message: 'Super Admin is protected and cannot be deactivated.',
            target: user.name,
            description: 'Administrators with FULL_ACCESS must remain enabled.',
            confirmText: 'Understood',
            cancelText: 'Close',
            loading: false,
            onConfirm: () => {
                confirmModal.value.show = false
            }
        }
        return
    }

    const newStatus: UserStatus =
        user.status === 'Active'
            ? 'Inactive'
            : 'Active'
    const isDeactivating = newStatus === 'Inactive'

    confirmModal.value = {
        show: true,
        type: isDeactivating ? 'warning' : 'info',
        title: isDeactivating ? 'Deactivate User?' : 'Activate User?',
        message: `Change user status to ${newStatus} for `,
        target: user.name,
        description: isDeactivating
            ? 'This staff member will not be able to log in until reactivated.'
            : 'This staff member will immediately regain access to their assigned modules.',
        confirmText: isDeactivating ? 'Deactivate' : 'Activate',
        cancelText: 'Cancel',
        loading: false,
        onConfirm: async () => {
            confirmModal.value.loading = true
            try {
                const response = await api.patch(
                    `${USER_API}/${encodeURIComponent(user.id)}/status`,
                    { enabled: newStatus === 'Active' }
                )
                replaceUser(mapUser(response.data))
                confirmModal.value.show = false
            } catch (error) {
                console.error('Error changing user status:', error)
                confirmModal.value = {
                    show: true,
                    type: 'danger',
                    title: 'Action Failed',
                    message: getErrorMessage(error, 'Failed to update user status.'),
                    target: '',
                    description: '',
                    confirmText: 'Dismiss',
                    cancelText: 'Close',
                    loading: false,
                    onConfirm: () => { confirmModal.value.show = false }
                }
            } finally {
                confirmModal.value.loading = false
            }
        }
    }
}

/* =========================================================
   DELETE USER
   ========================================================= */

const deleteUser = (user: SystemUser) => {
    if (isSuperAdmin(user)) {
        confirmModal.value = {
            show: true,
            type: 'warning',
            title: 'Protected Account',
            message: 'Super Admin is protected and cannot be deleted.',
            target: user.name,
            description: 'System administrators with FULL_ACCESS cannot be removed.',
            confirmText: 'Understood',
            cancelText: 'Close',
            loading: false,
            onConfirm: () => {
                confirmModal.value.show = false
            }
        }
        return
    }

    confirmModal.value = {
        show: true,
        type: 'danger',
        title: 'Delete System User?',
        message: 'Are you sure you want to permanently delete user ',
        target: user.name,
        description: 'This will immediately revoke their access. This action cannot be undone.',
        confirmText: 'Delete User',
        cancelText: 'Cancel',
        loading: false,
        onConfirm: async () => {
            confirmModal.value.loading = true
            try {
                await api.delete(`${USER_API}/${encodeURIComponent(user.id)}`)
                users.value = users.value.filter(item => item.id !== user.id)
                confirmModal.value.show = false
            } catch (error) {
                console.error('Error deleting user:', error)
                confirmModal.value = {
                    show: true,
                    type: 'danger',
                    title: 'Delete Failed',
                    message: getErrorMessage(error, 'Failed to delete user.'),
                    target: '',
                    description: '',
                    confirmText: 'Dismiss',
                    cancelText: 'Close',
                    loading: false,
                    onConfirm: () => { confirmModal.value.show = false }
                }
            } finally {
                confirmModal.value.loading = false
            }
        }
    }
}

/* =========================================================
   FORMAT DATE
   ========================================================= */

const formatDate = (date: string) => {
    if (!date || date === '-') {
        return '-'
    }

    return new Date(date).toLocaleDateString(
        'en-US',
        {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        }
    )
}

/* =========================================================
   LOAD USERS
   ========================================================= */

const loadUsers = async () => {
    isLoading.value = true

    try {
        const response = await api.get<SystemUserApi[]>(USER_API)

        users.value = (Array.isArray(response.data)
            ? response.data.map(mapUser)
            : []).reverse()
    } catch (error) {
        console.error('Error loading system users:', error)

        showAlert('Load Failed', getErrorMessage(error, 'Unable to load system users.'), 'danger')
    } finally {
        isLoading.value = false
    }
}

/* =========================================================
   MOUNT
   ========================================================= */

onMounted(() => {
    loadUsers()
})
</script>

<template>
    <div class="min-h-screen bg-slate-100 text-slate-900">

        <!-- =====================================================
         ADMIN SIDEBAR
         ===================================================== -->

        <AdminSidebar />

        <!-- =====================================================
         MAIN CONTENT
         ===================================================== -->

        <main class="ml-64 min-h-screen">

            <!-- Background Glow -->

            <div class="pointer-events-none fixed inset-0 overflow-hidden">

                <div class="absolute -top-40 -right-40 h-96 w-96 rounded-full bg-blue-200/30 blur-3xl"></div>

                <div class="absolute top-1/2 -left-40 h-96 w-96 rounded-full bg-cyan-200/20 blur-3xl"></div>

                <div class="absolute bottom-0 right-1/4 h-80 w-80 rounded-full bg-violet-200/20 blur-3xl"></div>

            </div>

            <div class="relative z-10 p-6 lg:p-8">

                <!-- =================================================
             HEADER
             ================================================= -->

                <div class="mb-8 flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">

                    <div>

                        <div class="mb-2 flex items-center gap-2">

                            <span class="h-2 w-2 rounded-full bg-blue-500 shadow-lg shadow-blue-500/40"></span>

                            <span class="text-xs font-bold uppercase tracking-[0.18em] text-blue-600">
                                User Administration
                            </span>

                        </div>

                        <h1 class="text-3xl font-black tracking-tight text-slate-950 sm:text-4xl">
                            Manage System Users
                        </h1>

                        <p class="mt-2 max-w-2xl text-sm text-slate-500">
                            Create, manage and monitor staff accounts
                            and system access.
                        </p>

                    </div>

                    <div class="flex flex-col gap-3 sm:flex-row">

                        <button @click="loadUsers" :disabled="isLoading"
                            class="inline-flex items-center justify-center gap-2 rounded-2xl border border-white/90 bg-white/80 px-5 py-3.5 text-sm font-bold text-slate-700 shadow-lg shadow-slate-300/10 backdrop-blur-xl transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-600 disabled:opacity-50">

                            <svg class="h-5 w-5" :class="{ 'animate-spin': isLoading }" fill="none"
                                stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                    d="M4 4v5h5M20 20v-5h-5M5.05 9A7 7 0 0117.95 7M18.95 15A7 7 0 016.05 17" />
                            </svg>

                            Refresh

                        </button>

                        <button @click="openAddModal"
                            class="group inline-flex items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-blue-600 to-cyan-500 px-5 py-3.5 text-sm font-bold text-white shadow-lg shadow-blue-500/20 transition-all duration-300 hover:-translate-y-0.5 hover:shadow-xl hover:shadow-blue-500/30">

                            <svg class="h-5 w-5 transition-transform group-hover:rotate-90" fill="none"
                                stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                    d="M12 4v16m8-8H4" />
                            </svg>

                            Add User

                        </button>

                    </div>

                </div>

                <!-- =================================================
             STATISTICS
             ================================================= -->

                <div class="mb-8 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-5">

                    <!-- Total -->

                    <div
                        class="rounded-2xl border border-white/90 bg-white/70 p-5 shadow-lg shadow-slate-300/10 backdrop-blur-2xl">

                        <div class="flex items-center justify-between">

                            <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-50 text-blue-600">

                                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                        d="M16 21v-2a4 4 0 00-4-4H6a4 4 0 00-4 4v2m7-10a4 4 0 100-8 4 4 0 000 8zm7-4a4 4 0 110 8m4 6v-2a4 4 0 00-3-3.87" />
                                </svg>

                            </div>

                            <span class="rounded-full bg-blue-50 px-2.5 py-1 text-xs font-bold text-blue-600">
                                ALL
                            </span>

                        </div>

                        <p class="mt-5 text-2xl font-black text-slate-950">
                            {{ totalUsers }}
                        </p>

                        <p class="mt-1 text-sm text-slate-500">
                            Total Users
                        </p>

                    </div>

                    <!-- Active -->

                    <div
                        class="rounded-2xl border border-white/90 bg-white/70 p-5 shadow-lg shadow-slate-300/10 backdrop-blur-2xl">

                        <div class="flex items-center justify-between">

                            <div
                                class="flex h-11 w-11 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600">

                                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                        d="M5 13l4 4L19 7" />
                                </svg>

                            </div>

                            <span class="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-bold text-emerald-600">
                                LIVE
                            </span>

                        </div>

                        <p class="mt-5 text-2xl font-black text-slate-950">
                            {{ activeUsers }}
                        </p>

                        <p class="mt-1 text-sm text-slate-500">
                            Active Users
                        </p>

                    </div>

                    <!-- Inactive -->

                    <div
                        class="rounded-2xl border border-white/90 bg-white/70 p-5 shadow-lg shadow-slate-300/10 backdrop-blur-2xl">

                        <div class="flex items-center justify-between">

                            <div
                                class="flex h-11 w-11 items-center justify-center rounded-xl bg-slate-100 text-slate-600">

                                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                        d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                                </svg>

                            </div>

                            <span class="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-bold text-slate-600">
                                OFF
                            </span>

                        </div>

                        <p class="mt-5 text-2xl font-black text-slate-950">
                            {{ inactiveUsers }}
                        </p>

                        <p class="mt-1 text-sm text-slate-500">
                            Inactive Users
                        </p>

                    </div>

                    <!-- Administrators -->

                    <div
                        class="rounded-2xl border border-white/90 bg-white/70 p-5 shadow-lg shadow-slate-300/10 backdrop-blur-2xl">

                        <div class="flex items-center justify-between">

                            <div
                                class="flex h-11 w-11 items-center justify-center rounded-xl bg-violet-50 text-violet-600">

                                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                        d="M12 15a3 3 0 100-6 3 3 0 000 6zM19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 01-2.83 2.83l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09a1.65 1.65 0 00-1-1.51 1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 11-2.83-2.83l.06-.06a1.65 1.65 0 00.33-1.82 1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09a1.65 1.65 0 001.51-1 1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 112.83-2.83l.06.06a1.65 1.65 0 001.82.33h.05a1.65 1.65 0 00.95-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 112.83 2.83l-.06.06a1.65 1.65 0 00-.33 1.82v.05a1.65 1.65 0 001.51.95H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z" />
                                </svg>

                            </div>

                            <span class="rounded-full bg-violet-50 px-2.5 py-1 text-xs font-bold text-violet-600">
                                ADMIN
                            </span>

                        </div>

                        <p class="mt-5 text-2xl font-black text-slate-950">
                            {{ administratorCount }}
                        </p>

                        <p class="mt-1 text-sm text-slate-500">
                            Administrators
                        </p>

                    </div>

                    <!-- Staff -->

                    <div
                        class="rounded-2xl border border-white/90 bg-white/70 p-5 shadow-lg shadow-slate-300/10 backdrop-blur-2xl">

                        <div class="flex items-center justify-between">

                            <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-cyan-50 text-cyan-600">

                                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                        d="M17 20h5v-2a4 4 0 00-4-4h-1M9 20H4v-2a4 4 0 014-4h1m7-7a4 4 0 11-8 0 4 4 0 018 0zm4 2a3 3 0 10-6 0" />
                                </svg>

                            </div>

                            <span class="rounded-full bg-cyan-50 px-2.5 py-1 text-xs font-bold text-cyan-600">
                                STAFF
                            </span>

                        </div>

                        <p class="mt-5 text-2xl font-black text-slate-950">
                            {{ staffCount }}
                        </p>

                        <p class="mt-1 text-sm text-slate-500">
                            Staff Members
                        </p>

                    </div>

                </div>

                <!-- =================================================
             FILTERS
             ================================================= -->

                <div
                    class="mb-6 rounded-2xl border border-white/90 bg-white/70 p-4 shadow-lg shadow-slate-300/10 backdrop-blur-2xl">

                    <div class="flex flex-col gap-3 xl:flex-row xl:items-center">

                        <!-- Search -->

                        <div class="relative flex-1">

                            <svg class="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" fill="none"
                                stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                    d="M21 21l-4.35-4.35m2.1-5.4a7.5 7.5 0 11-15 0 7.5 7.5 0 0115 0z" />
                            </svg>

                            <input v-model="searchQuery" type="text"
                                placeholder="Search name, email, phone or department..."
                                class="w-full rounded-xl border border-slate-200/80 bg-white/80 py-3 pl-11 pr-4 text-sm text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10" />

                        </div>

                        <!-- Role -->

                        <select v-model="roleFilter"
                            class="rounded-xl border border-slate-200/80 bg-white/80 px-4 py-3 text-sm font-medium text-slate-700 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10">
                            <option value="All">
                                All Roles
                            </option>

                            <option value="Administrator">
                                Administrator
                            </option>

                            <option value="Delivery Staff">
                                Delivery Staff
                            </option>

                            <option value="Support Staff">
                                Support Staff
                            </option>

                            <option value="Warehouse Staff">
                                Warehouse Staff
                            </option>
                        </select>

                        <!-- Status -->

                        <select v-model="statusFilter"
                            class="rounded-xl border border-slate-200/80 bg-white/80 px-4 py-3 text-sm font-medium text-slate-700 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10">
                            <option value="All">
                                All Status
                            </option>

                            <option value="Active">
                                Active
                            </option>

                            <option value="Inactive">
                                Inactive
                            </option>
                        </select>

                    </div>

                </div>

                <!-- =================================================
             USER TABLE
             ================================================= -->

                <div
                    class="overflow-hidden rounded-2xl border border-white/90 bg-white/70 shadow-xl shadow-slate-300/10 backdrop-blur-2xl">

                    <!-- Table Header -->

                    <div
                        class="flex flex-col gap-2 border-b border-slate-200/70 px-6 py-5 sm:flex-row sm:items-center sm:justify-between">

                        <div>

                            <h2 class="text-lg font-black text-slate-950">
                                System Users
                            </h2>

                            <p class="mt-1 text-sm text-slate-500">
                                {{ filteredUsers.length }}
                                user{{ filteredUsers.length === 1 ? '' : 's' }}
                                displayed
                            </p>

                        </div>

                        <span
                            class="inline-flex w-fit items-center gap-2 rounded-full bg-blue-50 px-3 py-1.5 text-xs font-bold text-blue-600">
                            <span class="h-1.5 w-1.5 rounded-full bg-blue-500"></span>

                            User Management
                        </span>

                    </div>

                    <!-- Desktop Table -->

                    <div class="hidden overflow-x-auto lg:block">

                        <table class="w-full min-w-[1100px]">

                            <thead>

                                <tr class="border-b border-slate-200/70 bg-slate-50/60 text-left">

                                    <th class="px-6 py-4 text-xs font-black uppercase tracking-wider text-slate-500">
                                        User
                                    </th>

                                    <th class="px-6 py-4 text-xs font-black uppercase tracking-wider text-slate-500">
                                        Contact
                                    </th>

                                    <th class="px-6 py-4 text-xs font-black uppercase tracking-wider text-slate-500">
                                        Role
                                    </th>

                                    <th class="px-6 py-4 text-xs font-black uppercase tracking-wider text-slate-500">
                                        Department
                                    </th>

                                    <th class="px-6 py-4 text-xs font-black uppercase tracking-wider text-slate-500">
                                        Joined
                                    </th>

                                    <th class="px-6 py-4 text-xs font-black uppercase tracking-wider text-slate-500">
                                        Status
                                    </th>

                                    <th
                                        class="px-6 py-4 text-right text-xs font-black uppercase tracking-wider text-slate-500">
                                        Actions
                                    </th>

                                </tr>

                            </thead>

                            <tbody class="divide-y divide-slate-200/60">

                                <tr v-for="user in filteredUsers" :key="user.id"
                                    class="group transition hover:bg-blue-50/30">

                                    <!-- User -->

                                    <td class="px-6 py-5">

                                        <div class="flex items-center gap-4">

                                            <img :src="user.userImage" :alt="user.name"
                                                class="h-11 w-11 shrink-0 rounded-xl border border-slate-200 object-cover" />

                                            <div class="min-w-0">

                                                <div class="font-black text-slate-950">
                                                    {{ user.name }}
                                                </div>

                                                <div class="mt-1 text-xs text-slate-400">
                                                    ID:
                                                    {{ user.id }}
                                                </div>

                                            </div>

                                        </div>

                                    </td>

                                    <!-- Contact -->

                                    <td class="px-6 py-5">

                                        <div>

                                            <p class="font-bold text-slate-700">
                                                {{ user.email }}
                                            </p>

                                            <p class="mt-1 text-xs text-slate-400">
                                                {{ user.phone }}
                                            </p>

                                        </div>

                                    </td>

                                    <!-- Role -->

                                    <td class="px-6 py-5">

                                        <span :class="[
                                            'inline-flex rounded-lg border px-2.5 py-1.5 text-xs font-bold',
                                            getRoleClass(user.role)
                                        ]">
                                            {{ user.role }}
                                        </span>

                                    </td>

                                    <!-- Department -->

                                    <td class="px-6 py-5">

                                        <span class="text-sm font-bold text-slate-700">
                                            {{ user.department }}
                                        </span>

                                    </td>

                                    <!-- Joined -->

                                    <td class="px-6 py-5">

                                        <span class="text-sm font-bold text-slate-700">
                                            {{ formatDate(user.joinedDate) }}
                                        </span>

                                    </td>

                                    <!-- Status -->

                                    <td class="px-6 py-5">

                                        <template v-if="isSuperAdmin(user)">
                                            <span class="inline-flex items-center gap-1 rounded-full border px-3 py-1.5 text-xs font-bold bg-emerald-50 text-emerald-700 border-emerald-200">
                                                Active
                                            </span>
                                        </template>
                                        <template v-else>
                                            <button @click="toggleUserStatus(user)" :class="[
                                                'rounded-full border px-3 py-1.5 text-xs font-bold transition',
                                                getStatusClass(user.status),
                                                'hover:scale-105'
                                            ]">
                                                {{ user.status }}
                                            </button>
                                        </template>

                                    </td>

                                    <!-- Actions -->

                                    <td class="px-6 py-5">

                                        <div class="flex justify-end items-center gap-2">

                                            <!-- View -->

                                            <button @click="openViewModal(user)"
                                                class="rounded-xl border border-slate-200 bg-white p-2.5 text-slate-500 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-600"
                                                title="View user">

                                                <svg class="h-4 w-4" fill="none" stroke="currentColor"
                                                    viewBox="0 0 24 24">
                                                    <path stroke-linecap="round" stroke-linejoin="round"
                                                        stroke-width="2"
                                                        d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />

                                                    <path stroke-linecap="round" stroke-linejoin="round"
                                                        stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                                </svg>

                                            </button>

                                            <template v-if="isSuperAdmin(user)">
                                                <span class="inline-flex items-center gap-1 rounded-xl bg-amber-50 border border-amber-200 px-3 py-1.5 text-xs font-bold text-amber-700 select-none" title="Protected: Super Admin cannot be edited or deleted">
                                                    🛡️ Super Admin
                                                </span>
                                            </template>
                                            <template v-else>
                                                <!-- Edit -->

                                                <button @click="openEditModal(user)"
                                                    class="rounded-xl border border-slate-200 bg-white p-2.5 text-slate-500 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-600"
                                                    title="Edit user">

                                                    <svg class="h-4 w-4" fill="none" stroke="currentColor"
                                                        viewBox="0 0 24 24">
                                                        <path stroke-linecap="round" stroke-linejoin="round"
                                                            stroke-width="2"
                                                            d="M16.862 4.487l1.687-1.688a2.25 2.25 0 113.182 3.182l-9.193 9.193a4.5 4.5 0 01-1.897 1.13l-3.293.94.94-3.293a4.5 4.5 0 011.13-1.897l9.193-9.193z" />
                                                    </svg>

                                                </button>

                                                <!-- Delete -->

                                                <button @click="deleteUser(user)"
                                                    class="rounded-xl border border-slate-200 bg-white p-2.5 text-slate-500 transition hover:border-red-200 hover:bg-red-50 hover:text-red-600"
                                                    title="Delete user">

                                                    <svg class="h-4 w-4" fill="none" stroke="currentColor"
                                                        viewBox="0 0 24 24">
                                                        <path stroke-linecap="round" stroke-linejoin="round"
                                                            stroke-width="2"
                                                            d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6M9 7V4a1 1 0 011-1h4a1 1 0 011 1v3m-8 0h10" />
                                                    </svg>

                                                </button>
                                            </template>

                                        </div>

                                    </td>

                                </tr>

                            </tbody>

                        </table>

                    </div>

                    <!-- Empty Desktop -->

                    <div v-if="filteredUsers.length === 0" class="hidden px-6 py-16 text-center lg:block">

                        <div
                            class="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-slate-100 text-slate-400">

                            <svg class="h-7 w-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                    d="M17 20h5v-2a4 4 0 00-4-4h-1M9 20H4v-2a4 4 0 014-4h1m7-7a4 4 0 11-8 0 4 4 0 018 0zm4 2a3 3 0 10-6 0" />
                            </svg>

                        </div>

                        <h3 class="mt-4 font-bold text-slate-900">
                            No users found
                        </h3>

                        <p class="mt-1 text-sm text-slate-500">
                            Try changing your search or filter options.
                        </p>

                    </div>

                    <!-- =================================================
               MOBILE CARDS
               ================================================= -->

                    <div class="space-y-4 p-4 lg:hidden">

                        <div v-for="user in filteredUsers" :key="user.id"
                            class="rounded-2xl border border-slate-200/80 bg-white/80 p-4 shadow-sm">

                            <div class="flex items-start justify-between gap-3">

                                <div class="flex min-w-0 items-center gap-3">

                                    <img :src="user.userImage" :alt="user.name"
                                        class="h-11 w-11 shrink-0 rounded-xl border border-slate-200 object-cover" />

                                    <div class="min-w-0">

                                        <h3 class="truncate font-black text-slate-950">
                                            {{ user.name }}
                                        </h3>

                                        <p class="mt-0.5 truncate text-xs text-slate-500">
                                            {{ user.email }}
                                        </p>

                                    </div>

                                </div>

                                <span :class="[
                                    'shrink-0 rounded-full border px-2.5 py-1 text-[10px] font-bold',
                                    getStatusClass(user.status)
                                ]">
                                    {{ user.status }}
                                </span>

                            </div>

                            <div class="mt-3">

                                <span :class="[
                                    'inline-flex rounded-lg border px-2.5 py-1 text-[10px] font-black',
                                    getRoleClass(user.role)
                                ]">
                                    {{ user.role }}
                                </span>

                            </div>

                            <div class="mt-4 grid grid-cols-2 gap-3 rounded-xl bg-slate-50/80 p-3">

                                <div>

                                    <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                                        Phone
                                    </p>

                                    <p class="mt-1 text-sm font-bold text-slate-700">
                                        {{ user.phone }}
                                    </p>

                                </div>

                                <div>

                                    <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                                        Department
                                    </p>

                                    <p class="mt-1 truncate text-sm font-bold text-slate-700">
                                        {{ user.department }}
                                    </p>

                                </div>

                                <div>

                                    <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                                        Joined
                                    </p>

                                    <p class="mt-1 text-sm font-bold text-slate-700">
                                        {{ formatDate(user.joinedDate) }}
                                    </p>

                                </div>

                                <div>

                                    <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                                        Last Login
                                    </p>

                                    <p class="mt-1 text-sm font-bold text-slate-700">
                                        {{ formatDate(user.lastLogin) }}
                                    </p>

                                </div>

                            </div>

                            <div class="mt-4 flex gap-2">

                                <button @click="openViewModal(user)"
                                    class="flex flex-1 items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white py-2.5 text-sm font-bold text-slate-600 transition hover:bg-slate-50">

                                    <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                            d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />

                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                            d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                    </svg>

                                    View

                                </button>

                                <template v-if="isSuperAdmin(user)">
                                    <span class="flex flex-1 items-center justify-center gap-1 rounded-xl bg-amber-50 border border-amber-200 py-2.5 text-xs font-bold text-amber-700 select-none">
                                        🛡️ Super Admin
                                    </span>
                                </template>
                                <template v-else>
                                    <button @click="openEditModal(user)"
                                        class="flex flex-1 items-center justify-center gap-2 rounded-xl border border-blue-200 bg-blue-50 py-2.5 text-sm font-bold text-blue-600 transition hover:bg-blue-100">

                                        <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                                d="M16.862 4.487l1.687-1.688a2.25 2.25 0 113.182 3.182l-9.193 9.193a4.5 4.5 0 01-1.897 1.13l-3.293.94.94-3.293a4.5 4.5 0 011.13-1.897l9.193-9.193z" />
                                        </svg>

                                        Edit

                                    </button>

                                    <button @click="deleteUser(user)"
                                        class="flex items-center justify-center rounded-xl border border-red-200 bg-red-50 px-4 py-2.5 text-red-600 transition hover:bg-red-100"
                                        title="Delete">

                                        <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                                d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6M9 7V4a1 1 0 011-1h4a1 1 0 011 1v3m-8 0h10" />
                                        </svg>

                                    </button>
                                </template>

                            </div>

                        </div>

                        <!-- Empty Mobile -->

                        <div v-if="filteredUsers.length === 0" class="py-12 text-center">

                            <div
                                class="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-100 text-slate-400">

                                <svg class="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                        d="M17 20h5v-2a4 4 0 00-4-4h-1M9 20H4v-2a4 4 0 014-4h1m7-7a4 4 0 11-8 0 4 4 0 018 0zm4 2a3 3 0 10-6 0" />
                                </svg>

                            </div>

                            <p class="mt-4 font-bold text-slate-800">
                                No users found
                            </p>

                            <p class="mt-1 text-sm text-slate-500">
                                Try another search or filter.
                            </p>

                        </div>

                    </div>

                </div>

            </div>

        </main>

        <!-- =====================================================
         ADD / EDIT USER MODAL
         ===================================================== -->

        <Transition name="modal">

            <div v-if="showModal"
                class="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 p-4 backdrop-blur-sm"
                @click.self="closeModal">

                <div
                    class="max-h-[92vh] w-full max-w-2xl overflow-y-auto rounded-3xl border border-white/90 bg-white/95 p-6 shadow-2xl shadow-slate-900/20 backdrop-blur-2xl sm:p-8">

                    <!-- Header -->

                    <div class="mb-6 flex items-start justify-between">

                        <div>

                            <div
                                class="mb-3 flex h-11 w-11 items-center justify-center rounded-xl bg-blue-50 text-blue-600">

                                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                        d="M16 21v-2a4 4 0 00-4-4H6a4 4 0 00-4 4v2m7-10a4 4 0 100-8 4 4 0 000 8zm7-4a4 4 0 110 8m4 6v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75" />
                                </svg>

                            </div>

                            <h2 class="text-2xl font-black text-slate-950">
                                {{
                                    isEditing
                                        ? 'Edit System User'
                                        : 'Add System User'
                                }}
                            </h2>

                            <p class="mt-1 text-sm text-slate-500">
                                {{
                                    isEditing
                                        ? 'Update account and role information.'
                                        : 'Create a new staff account.'
                                }}
                            </p>

                        </div>

                        <button @click="closeModal"
                            class="rounded-xl p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700">

                            <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                    d="M6 18L18 6M6 6l12 12" />
                            </svg>

                        </button>

                    </div>

                    <!-- Form -->

                    <form @submit.prevent="saveUser" class="space-y-5">

                        <!-- Name -->

                        <div>

                            <label class="mb-2 block text-sm font-bold text-slate-700">
                                Full Name
                            </label>

                            <input v-model="userForm.name" type="text" placeholder="Enter full name"
                                class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-medium text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10" />

                        </div>

                        <!-- Email / Phone -->

                        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">

                            <div>

                                <label class="mb-2 block text-sm font-bold text-slate-700">
                                    Email Address
                                </label>

                                <input v-model="userForm.email" type="email" placeholder="user@example.com"
                                    class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-medium text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10" />

                            </div>

                            <div>

                                <label class="mb-2 block text-sm font-bold text-slate-700">
                                    Phone Number
                                </label>

                                <input v-model="userForm.phone" type="text" placeholder="0771234567"
                                    class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-medium text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10" />

                            </div>

                        </div>

                        <!-- Password -->

                        <div>

                            <label class="mb-2 block text-sm font-bold text-slate-700">
                                {{ isEditing ? 'New Password (optional)' : 'Password' }}
                            </label>

                            <input v-model="userForm.password" type="password" autocomplete="new-password"
                                :placeholder="isEditing ? 'Leave blank to keep current password' : 'At least 6 characters'"
                                class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-medium text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10" />

                        </div>

                        <!-- Role / Department -->

                        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">

                            <div>

                                <label class="mb-2 block text-sm font-bold text-slate-700">
                                    System Role
                                </label>

                                <select v-model="userForm.role" :disabled="isEditing"
                                    class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-medium text-slate-700 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10">

                                    <option value="Administrator">
                                        Administrator
                                    </option>

                                    <option value="Delivery Staff">
                                        Delivery Staff
                                    </option>

                                    <option value="Support Staff">
                                        Support Staff
                                    </option>

                                    <option value="Warehouse Staff">
                                        Warehouse Staff
                                    </option>

                                </select>

                            </div>

                            <div>

                                <template v-if="userForm.role === 'Administrator'">
                                    <label class="mb-2 block text-sm font-bold text-slate-700">Access Level</label>
                                    <select v-model="userForm.accessLevel"
                                        class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-medium text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10">
                                        <option value="">Select access level</option>
                                        <option value="FULL_ACCESS">Full access</option>
                                        <option value="LIMITED_ACCESS">Limited access</option>
                                    </select>
                                </template>

                                <template v-else-if="userForm.role === 'Warehouse Staff'">
                                    <label class="mb-2 block text-sm font-bold text-slate-700">Warehouse
                                        Location</label>
                                    <input v-model="userForm.warehouseLoc" type="text"
                                        placeholder="e.g. Colombo Main Warehouse"
                                        class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-medium text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10" />
                                </template>

                                <template v-else-if="userForm.role === 'Support Staff'">
                                    <label class="mb-2 block text-sm font-bold text-slate-700">Department</label>
                                    <input v-model="userForm.supportDept" type="text"
                                        placeholder="e.g. Customer Support"
                                        class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-medium text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10" />
                                </template>

                                <template v-else>
                                    <label class="mb-2 block text-sm font-bold text-slate-700">Vehicle No</label>
                                    <input v-model="userForm.vehicleNo" type="text" placeholder="e.g. CAB-1234"
                                        class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-medium text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10" />
                                    <label class="mb-2 block text-sm font-bold text-slate-700 mt-3">License No</label>
                                    <input v-model="userForm.licenseNo" type="text" placeholder="e.g. B1234567"
                                        class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-medium text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10" />
                                </template>

                            </div>

                        </div>

                        <!-- Image -->

                        <div>

                            <label class="mb-2 block text-sm font-bold text-slate-700">
                                User Image URL
                            </label>

                            <input v-model="userForm.userImage" type="text" placeholder="https://example.com/user.jpg"
                                class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-medium text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-500/10" />

                        </div>

                        <!-- Status -->

                        <div>

                            <label class="mb-2 block text-sm font-bold text-slate-700">
                                Account Status
                            </label>

                            <div class="grid grid-cols-2 gap-3">

                                <button type="button" @click="userForm.status = 'Active'" :class="[
                                    'rounded-xl border px-4 py-3 text-sm font-bold transition',
                                    userForm.status === 'Active'
                                        ? 'border-emerald-300 bg-emerald-50 text-emerald-700'
                                        : 'border-slate-200 bg-white text-slate-500 hover:bg-slate-50'
                                ]">
                                    Active
                                </button>

                                <button type="button" @click="userForm.status = 'Inactive'" :class="[
                                    'rounded-xl border px-4 py-3 text-sm font-bold transition',
                                    userForm.status === 'Inactive'
                                        ? 'border-slate-300 bg-slate-100 text-slate-700'
                                        : 'border-slate-200 bg-white text-slate-500 hover:bg-slate-50'
                                ]">
                                    Inactive
                                </button>

                            </div>

                        </div>

                        <!-- Buttons -->

                        <div
                            class="flex flex-col-reverse gap-3 border-t border-slate-200/70 pt-6 sm:flex-row sm:justify-end">

                            <button type="button" @click="closeModal"
                                class="rounded-xl border border-slate-200 bg-white px-5 py-3 text-sm font-bold text-slate-600 transition hover:bg-slate-50">
                                Cancel
                            </button>

                            <button type="submit" :disabled="isSaving"
                                class="rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-6 py-3 text-sm font-bold text-white shadow-lg shadow-blue-500/20 transition hover:-translate-y-0.5 hover:shadow-xl disabled:opacity-50">

                                <span v-if="!isSaving">
                                    {{
                                        isEditing
                                            ? 'Update User'
                                            : 'Create User'
                                    }}
                                </span>

                                <span v-else class="flex items-center gap-2">

                                    <svg class="h-4 w-4 animate-spin" fill="none" stroke="currentColor"
                                        viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                            d="M12 4v2m0 12v2m8-8h-2M6 12H4m13.657-5.657l-1.414 1.414M7.757 16.243l-1.414 1.414m0-11.314L7.757 7.757m8.486 8.486l1.414 1.414" />
                                    </svg>

                                    Saving...

                                </span>

                            </button>

                        </div>

                    </form>

                </div>

            </div>

        </Transition>

        <!-- =====================================================
         VIEW USER MODAL
         ===================================================== -->

        <Transition name="modal">

            <div v-if="showViewModal && selectedUser"
                class="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 p-4 backdrop-blur-sm"
                @click.self="closeViewModal">

                <div
                    class="w-full max-w-lg rounded-3xl border border-white/90 bg-white/95 p-6 shadow-2xl shadow-slate-900/20 backdrop-blur-2xl sm:p-8">

                    <!-- Header -->

                    <div class="flex items-start justify-between">

                        <div>

                            <p class="text-xs font-bold uppercase tracking-[0.18em] text-blue-600">
                                User Profile
                            </p>

                            <h2 class="mt-1 text-2xl font-black text-slate-950">
                                User Details
                            </h2>

                        </div>

                        <button @click="closeViewModal"
                            class="rounded-xl p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700">

                            <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                    d="M6 18L18 6M6 6l12 12" />
                            </svg>

                        </button>

                    </div>

                    <!-- User Profile -->

                    <div class="mt-6 text-center">

                        <img :src="selectedUser.userImage" :alt="selectedUser.name"
                            class="mx-auto h-24 w-24 rounded-3xl border-4 border-white object-cover shadow-lg" />

                        <h3 class="mt-4 text-xl font-black text-slate-950">
                            {{ selectedUser.name }}
                        </h3>

                        <p class="mt-1 text-sm text-slate-500">
                            {{ selectedUser.email }}
                        </p>

                        <div class="mt-3 flex justify-center">

                            <span :class="[
                                'rounded-full border px-3 py-1.5 text-xs font-bold',
                                getStatusClass(selectedUser.status)
                            ]">
                                {{ selectedUser.status }}
                            </span>

                        </div>

                    </div>

                    <!-- Details -->

                    <div class="mt-6 space-y-3 rounded-2xl bg-slate-50/80 p-4">

                        <div class="flex items-center justify-between gap-4">

                            <span class="text-sm text-slate-500">
                                User ID
                            </span>

                            <span class="font-bold text-slate-800">
                                {{ selectedUser.id }}
                            </span>

                        </div>

                        <div class="flex items-center justify-between gap-4">

                            <span class="text-sm text-slate-500">
                                Phone
                            </span>

                            <span class="font-bold text-slate-800">
                                {{ selectedUser.phone }}
                            </span>

                        </div>

                        <div class="flex items-center justify-between gap-4">

                            <span class="text-sm text-slate-500">
                                Role
                            </span>

                            <span :class="[
                                'rounded-lg border px-2.5 py-1 text-xs font-bold',
                                getRoleClass(selectedUser.role)
                            ]">
                                {{ selectedUser.role }}
                            </span>

                        </div>

                        <div class="flex items-center justify-between gap-4">

                            <span class="text-sm text-slate-500">
                                Department
                            </span>

                            <span class="font-bold text-slate-800">
                                {{ selectedUser.department }}
                            </span>

                        </div>

                        <div class="flex items-center justify-between gap-4">

                            <span class="text-sm text-slate-500">
                                Joined Date
                            </span>

                            <span class="font-bold text-slate-800">
                                {{ formatDate(selectedUser.joinedDate) }}
                            </span>

                        </div>

                        <div class="flex items-center justify-between gap-4">

                            <span class="text-sm text-slate-500">
                                Last Login
                            </span>

                            <span class="font-bold text-slate-800">
                                {{ formatDate(selectedUser.lastLogin) }}
                            </span>

                        </div>

                    </div>

                    <!-- Modal Buttons -->

                    <div class="mt-6 flex gap-3">

                        <!-- FIXED -->
                        <button v-if="!isSuperAdmin(selectedUser)" @click="editSelectedUser"
                            class="flex-1 rounded-xl bg-blue-600 px-5 py-3 text-sm font-bold text-white transition hover:bg-blue-700">
                            Edit User
                        </button>

                        <button @click="closeViewModal"
                            :class="[
                                'rounded-xl border border-slate-200 bg-white px-5 py-3 text-sm font-bold text-slate-600 transition hover:bg-slate-50',
                                isSuperAdmin(selectedUser) ? 'w-full' : ''
                            ]">
                            Close
                        </button>

                    </div>

                </div>

            </div>

        </Transition>

        <!-- =====================================================
             CONFIRMATION MODAL (ORDER MANAGEMENT DESIGN)
             ===================================================== -->
        <AdminConfirmModal
            v-model:show="confirmModal.show"
            :type="confirmModal.type"
            :title="confirmModal.title"
            :message="confirmModal.message"
            :target="confirmModal.target"
            :description="confirmModal.description"
            :confirm-text="confirmModal.confirmText"
            :cancel-text="confirmModal.cancelText"
            :show-cancel="confirmModal.showCancel !== false"
            :loading="confirmModal.loading"
            @confirm="confirmModal.onConfirm"
        />

    </div>
</template>

<style scoped>
.modal-enter-active,
.modal-leave-active {
    transition: opacity 0.25s ease;
}

.modal-enter-from,
.modal-leave-to {
    opacity: 0;
}
</style>