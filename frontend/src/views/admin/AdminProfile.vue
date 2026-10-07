<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import api from '../../services/api'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'

interface StaffAddress {
  street: string
  city: string
  postalCode: string
}

interface StaffProfile {
  id: string
  name: string
  email: string
  phone: string
  role: string
  roleLabel: string
  avatar: string
  joinedDate: string
  lastLogin: string
  status: string
  accessLevel?: string
  warehouseLoc?: string
  department?: string
  vehicleNo?: string
  licenseNo?: string
  address?: StaffAddress
}

const SYSTEM_USER_ENDPOINT = '/admin/system-users'
const ADMIN_ENDPOINT = '/administrators'

const isLoading = ref(false)
const isSaving = ref(false)
const isChangingPassword = ref(false)
const isUploadingAvatar = ref(false)

const activeTab = ref<'profile' | 'security'>('profile')

const showPassword = ref(false)
const showConfirmPassword = ref(false)

const successMessage = ref('')
const errorMessage = ref('')

const avatarFileInput = ref<HTMLInputElement | null>(null)

/* =========================================================
   SESSION STAFF ID
   ========================================================= */

const sessionStaffId = ref(
  sessionStorage.getItem('userId') ||
  sessionStorage.getItem('staffId') ||
  sessionStorage.getItem('adminId') ||
  ''
)

/* =========================================================
   ROLE MAPPINGS
   ========================================================= */

const ROLE_DISPLAY: Record<string, string> = {
  ADMINISTRATOR: 'Administrator',
  WAREHOUSE_STAFF: 'Warehouse Staff',
  SUPPORT_STAFF: 'Support Staff',
  DELIVERY_STAFF: 'Delivery Staff'
}

const getRoleBadgeClasses = (role: string) => {
  switch (role) {
    case 'ADMINISTRATOR':
      return 'bg-blue-50 text-blue-700 border-blue-200 ring-blue-500/20'
    case 'WAREHOUSE_STAFF':
      return 'bg-emerald-50 text-emerald-700 border-emerald-200 ring-emerald-500/20'
    case 'SUPPORT_STAFF':
      return 'bg-violet-50 text-violet-700 border-violet-200 ring-violet-500/20'
    case 'DELIVERY_STAFF':
      return 'bg-cyan-50 text-cyan-700 border-cyan-200 ring-cyan-500/20'
    default:
      return 'bg-slate-100 text-slate-700 border-slate-200 ring-slate-500/20'
  }
}

/* =========================================================
   DEFAULT STAFF PROFILE
   ========================================================= */

const profile = ref<StaffProfile>({
  id: sessionStaffId.value || 'STAFF001',
  name: 'Staff Member',
  email: '',
  phone: '',
  role: 'ADMINISTRATOR',
  roleLabel: 'Staff Member',
  avatar: 'https://images.unsplash.com/photo-1560250097-0b93528c311a?auto=format&fit=crop&w=300&q=80',
  joinedDate: 'Current Session',
  lastLogin: 'Active now',
  status: 'Active',
  address: {
    street: '',
    city: '',
    postalCode: ''
  }
})

/* =========================================================
   PROFILE FORM
   ========================================================= */

const form = ref({
  name: '',
  email: '',
  phone: '',
  accessLevel: '',
  warehouseLoc: '',
  department: '',
  vehicleNo: '',
  licenseNo: '',
  address: {
    street: '',
    city: '',
    postalCode: ''
  }
})

/* =========================================================
   PASSWORD FORM
   ========================================================= */

const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

/* =========================================================
   DYNAMIC TITLES & BADGES
   ========================================================= */

const pageTitle = computed(() => {
  const label = profile.value.roleLabel
  return label ? `${label} Profile` : 'Staff Profile'
})

const pageSubtitle = computed(() => {
  const label = profile.value.roleLabel
  return `View and manage your ${label.toLowerCase()} details, contact information, and security credentials.`
})

/* =========================================================
   PASSWORD STRENGTH & MATCH
   ========================================================= */

const passwordStrength = computed(() => {
  const password = passwordForm.value.newPassword

  if (!password) {
    return {
      label: 'Enter a password',
      width: '0%',
      class: 'bg-slate-200'
    }
  }

  if (password.length < 6) {
    return {
      label: 'Weak (min 6 chars)',
      width: '30%',
      class: 'bg-red-500'
    }
  }

  if (
    password.length >= 8 &&
    /[A-Z]/.test(password) &&
    /[0-9]/.test(password) &&
    /[^A-Za-z0-9]/.test(password)
  ) {
    return {
      label: 'Strong',
      width: '100%',
      class: 'bg-emerald-500'
    }
  }

  return {
    label: 'Medium',
    width: '65%',
    class: 'bg-amber-500'
  }
})

const passwordsMatch = computed(() => {
  if (!passwordForm.value.confirmPassword) {
    return true
  }
  return passwordForm.value.newPassword === passwordForm.value.confirmPassword
})

/* =========================================================
   HELPERS
   ========================================================= */

const formatDate = (date?: string | null) => {
  if (!date) return 'N/A'
  const parsedDate = new Date(date)
  if (Number.isNaN(parsedDate.getTime())) return date
  return parsedDate.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

const resolveAvatar = (image?: string | null, nameFallback: string = 'Staff') => {
  if (!image) {
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(nameFallback)}&background=2563eb&color=fff`
  }
  if (
    image.startsWith('http://') ||
    image.startsWith('https://') ||
    image.startsWith('data:')
  ) {
    return image
  }
  return `http://localhost:8080/uploads/${image.replace(/^\/+/, '')}`
}

/* =========================================================
   INITIALIZE FROM SESSION
   ========================================================= */

const initFromSession = () => {
  try {
    const raw = sessionStorage.getItem('staffUser') || sessionStorage.getItem('user')
    if (raw) {
      const u = JSON.parse(raw)
      const roleKey = (u.role || 'ADMINISTRATOR').toUpperCase()
      profile.value = {
        id: u.userId || sessionStaffId.value || 'STAFF001',
        name: u.name || 'Staff Member',
        email: u.email || '',
        phone: u.phone || '',
        role: roleKey,
        roleLabel: ROLE_DISPLAY[roleKey] || 'Staff Member',
        avatar: resolveAvatar(u.userImage, u.name || 'Staff'),
        joinedDate: formatDate(u.registeredDate),
        lastLogin: 'Active now',
        status: u.enabled === false ? 'Inactive' : 'Active',
        accessLevel: u.accessLevel || '',
        warehouseLoc: u.warehouseLoc || '',
        department: u.department || '',
        vehicleNo: u.vehicleNo || '',
        licenseNo: u.licenseNo || '',
        address: u.address || { street: '', city: '', postalCode: '' }
      }

      form.value.name = profile.value.name
      form.value.email = profile.value.email
      form.value.phone = profile.value.phone
      form.value.accessLevel = profile.value.accessLevel || ''
      form.value.warehouseLoc = profile.value.warehouseLoc || ''
      form.value.department = profile.value.department || ''
      form.value.vehicleNo = profile.value.vehicleNo || ''
      form.value.licenseNo = profile.value.licenseNo || ''
      form.value.address = {
        street: profile.value.address?.street || '',
        city: profile.value.address?.city || '',
        postalCode: profile.value.address?.postalCode || ''
      }
    }
  } catch {
    // fallback
  }
}

/* =========================================================
   LOAD PROFILE FROM API
   ========================================================= */

const loadProfile = async () => {
  isLoading.value = true
  errorMessage.value = ''

  initFromSession()

  const staffId =
    sessionStaffId.value ||
    sessionStorage.getItem('userId') ||
    sessionStorage.getItem('staffId') ||
    sessionStorage.getItem('adminId')

  if (!staffId) {
    isLoading.value = false
    return
  }

  sessionStaffId.value = staffId

  let fetched = false

  // 1. Try unified system-users endpoint (handles all 4 staff roles)
  try {
    const response = await api.get(`${SYSTEM_USER_ENDPOINT}/${encodeURIComponent(staffId)}`)
    const data = response.data

    if (data) {
      const roleKey = (data.role || 'ADMINISTRATOR').toUpperCase()
      profile.value = {
        id: data.userId || staffId,
        name: data.name || '',
        email: data.email || '',
        phone: data.phone || '',
        role: roleKey,
        roleLabel: ROLE_DISPLAY[roleKey] || 'Staff Member',
        avatar: resolveAvatar(data.userImage, data.name || 'Staff'),
        joinedDate: formatDate(data.registeredDate),
        lastLogin: 'Active session',
        status: data.enabled === false ? 'Inactive' : 'Active',
        accessLevel: data.accessLevel || '',
        warehouseLoc: data.warehouseLoc || '',
        department: data.department || '',
        vehicleNo: data.vehicleNo || '',
        licenseNo: data.licenseNo || '',
        address: data.address || { street: '', city: '', postalCode: '' }
      }
      fetched = true
    }
  } catch (err: any) {
    // 2. Fallback to /administrators endpoint
    try {
      const response = await api.get(`${ADMIN_ENDPOINT}/${encodeURIComponent(staffId)}`)
      const data = response.data
      if (data) {
        const roleKey = (data.role || 'ADMINISTRATOR').toUpperCase()
        profile.value = {
          id: data.userId || staffId,
          name: data.name || '',
          email: data.email || '',
          phone: data.phone || '',
          role: roleKey,
          roleLabel: ROLE_DISPLAY[roleKey] || 'Staff Member',
          avatar: resolveAvatar(data.userImage, data.name || 'Staff'),
          joinedDate: formatDate(data.registeredDate),
          lastLogin: 'Active session',
          status: 'Active',
          accessLevel: data.accessLevel || '',
          address: data.address || { street: '', city: '', postalCode: '' }
        }
        fetched = true
      }
    } catch {
      // Use session data
    }
  } finally {
    form.value.name = profile.value.name
    form.value.email = profile.value.email
    form.value.phone = profile.value.phone
    form.value.accessLevel = profile.value.accessLevel || ''
    form.value.warehouseLoc = profile.value.warehouseLoc || ''
    form.value.department = profile.value.department || ''
    form.value.vehicleNo = profile.value.vehicleNo || ''
    form.value.licenseNo = profile.value.licenseNo || ''
    form.value.address = {
      street: profile.value.address?.street || '',
      city: profile.value.address?.city || '',
      postalCode: profile.value.address?.postalCode || ''
    }

    if (fetched) {
      syncSessionStorage()
    }

    isLoading.value = false
  }
}

/* =========================================================
   SAVE PROFILE
   ========================================================= */

const syncSessionStorage = () => {
  try {
    const raw = sessionStorage.getItem('staffUser') || sessionStorage.getItem('user') || '{}'
    const stored = JSON.parse(raw)
    const synced = JSON.stringify({
      ...stored,
      userId: profile.value.id,
      name: profile.value.name,
      email: profile.value.email,
      phone: profile.value.phone,
      role: profile.value.role,
      userImage: profile.value.avatar,
      accessLevel: profile.value.accessLevel,
      warehouseLoc: profile.value.warehouseLoc,
      department: profile.value.department,
      vehicleNo: profile.value.vehicleNo,
      licenseNo: profile.value.licenseNo,
      address: profile.value.address
    })

    sessionStorage.setItem('user', synced)
    sessionStorage.setItem('staffUser', synced)
    sessionStorage.setItem('userId', profile.value.id)
    sessionStorage.setItem('staffId', profile.value.id)
    sessionStorage.setItem('adminId', profile.value.id)

    window.dispatchEvent(new Event('user-profile-updated'))
  } catch {
    // ignore malformed session
  }
}

const handleSaveProfile = async () => {
  successMessage.value = ''
  errorMessage.value = ''

  if (!form.value.name.trim()) {
    errorMessage.value = 'Full name is required.'
    return
  }

  if (!form.value.email.trim()) {
    errorMessage.value = 'Email address is required.'
    return
  }

  isSaving.value = true

  const staffId = sessionStaffId.value || profile.value.id

  const payload: any = {
    name: form.value.name.trim(),
    email: form.value.email.trim(),
    phone: form.value.phone.trim(),
    address: form.value.address
  }

  if (profile.value.role === 'ADMINISTRATOR' && form.value.accessLevel) {
    payload.accessLevel = form.value.accessLevel
  } else if (profile.value.role === 'WAREHOUSE_STAFF') {
    payload.warehouseLoc = form.value.warehouseLoc
  } else if (profile.value.role === 'SUPPORT_STAFF') {
    payload.department = form.value.department
  } else if (profile.value.role === 'DELIVERY_STAFF') {
    payload.vehicleNo = form.value.vehicleNo
    payload.licenseNo = form.value.licenseNo
  }

  try {
    let savedData: any = null

    // 1. Try PUT /admin/system-users/{id}
    try {
      const response = await api.put(
        `${SYSTEM_USER_ENDPOINT}/${encodeURIComponent(staffId)}`,
        payload
      )
      savedData = response.data
    } catch (putErr: any) {
      // 2. Fallback to PUT /administrators/{id}
      const response = await api.put(
        `${ADMIN_ENDPOINT}/${encodeURIComponent(staffId)}`,
        payload
      )
      savedData = response.data
    }

    if (savedData) {
      profile.value.name = savedData.name || form.value.name
      profile.value.email = savedData.email || form.value.email
      profile.value.phone = savedData.phone || form.value.phone
      if (savedData.warehouseLoc) profile.value.warehouseLoc = savedData.warehouseLoc
      if (savedData.department) profile.value.department = savedData.department
      if (savedData.vehicleNo) profile.value.vehicleNo = savedData.vehicleNo
      if (savedData.licenseNo) profile.value.licenseNo = savedData.licenseNo
      if (savedData.address) profile.value.address = savedData.address
      if (savedData.userImage) profile.value.avatar = resolveAvatar(savedData.userImage, profile.value.name)
    } else {
      profile.value.name = form.value.name
      profile.value.email = form.value.email
      profile.value.phone = form.value.phone
      profile.value.warehouseLoc = form.value.warehouseLoc
      profile.value.department = form.value.department
      profile.value.vehicleNo = form.value.vehicleNo
      profile.value.licenseNo = form.value.licenseNo
      profile.value.address = form.value.address
    }

    syncSessionStorage()
    successMessage.value = 'Profile information updated successfully.'
  } catch (error: any) {
    console.error('Failed to update profile:', error)
    errorMessage.value =
      error?.response?.data?.message ||
      error?.response?.data?.error ||
      'Failed to update profile. Please verify your information and try again.'
  } finally {
    isSaving.value = false
    setTimeout(() => {
      successMessage.value = ''
    }, 5000)
  }
}

/* =========================================================
   CHANGE PASSWORD
   ========================================================= */

const handleChangePassword = async () => {
  successMessage.value = ''
  errorMessage.value = ''

  if (
    !passwordForm.value.currentPassword ||
    !passwordForm.value.newPassword ||
    !passwordForm.value.confirmPassword
  ) {
    errorMessage.value = 'Please complete all password fields.'
    return
  }

  if (passwordForm.value.newPassword.length < 6) {
    errorMessage.value = 'New password must contain at least 6 characters.'
    return
  }

  if (!passwordsMatch.value) {
    errorMessage.value = 'New passwords do not match.'
    return
  }

  isChangingPassword.value = true

  const staffId = sessionStaffId.value || profile.value.id

  try {
    try {
      await api.put(
        `${SYSTEM_USER_ENDPOINT}/${encodeURIComponent(staffId)}/password`,
        {
          currentPassword: passwordForm.value.currentPassword,
          newPassword: passwordForm.value.newPassword
        }
      )
    } catch {
      await api.put(
        `${ADMIN_ENDPOINT}/${encodeURIComponent(staffId)}/password`,
        {
          currentPassword: passwordForm.value.currentPassword,
          newPassword: passwordForm.value.newPassword
        }
      )
    }

    successMessage.value = 'Password changed successfully.'
    passwordForm.value = {
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
  } catch (error: any) {
    console.error('Failed to update password:', error)
    errorMessage.value =
      error?.response?.data?.message ||
      error?.response?.data?.error ||
      'Failed to update password. Please check your current password and try again.'
  } finally {
    isChangingPassword.value = false
    setTimeout(() => {
      successMessage.value = ''
    }, 5000)
  }
}

/* =========================================================
   AVATAR UPLOAD
   ========================================================= */

const triggerAvatarUpload = () => {
  avatarFileInput.value?.click()
}

const handleAvatarFileSelected = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  // Validate file size and type
  if (file.size > 5 * 1024 * 1024) {
    errorMessage.value = 'Image size must be smaller than 5MB.'
    return
  }

  const validTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
  if (!validTypes.includes(file.type)) {
    errorMessage.value = 'Please select a valid image file (JPEG, PNG, WEBP, or GIF).'
    return
  }

  isUploadingAvatar.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const formData = new FormData()
    formData.append('file', file)

    const uploadRes = await api.post('/uploads/image', formData)

    const imageUrl = uploadRes.data?.url
    if (!imageUrl) {
      throw new Error('Upload succeeded but server did not return image URL.')
    }

    profile.value.avatar = resolveAvatar(imageUrl, profile.value.name)

    const staffId = sessionStaffId.value || profile.value.id
    try {
      await api.put(`${SYSTEM_USER_ENDPOINT}/${encodeURIComponent(staffId)}`, {
        userImage: imageUrl
      })
    } catch {
      await api.put(`${ADMIN_ENDPOINT}/${encodeURIComponent(staffId)}`, {
        userImage: imageUrl
      })
    }

    syncSessionStorage()
    successMessage.value = 'Profile picture updated successfully.'
  } catch (err: any) {
    console.error('Failed to upload avatar:', err)
    errorMessage.value =
      err?.response?.data?.message ||
      err?.message ||
      'Failed to upload avatar image. Please try again.'
  } finally {
    isUploadingAvatar.value = false
    if (avatarFileInput.value) {
      avatarFileInput.value.value = ''
    }
    setTimeout(() => {
      successMessage.value = ''
    }, 5000)
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<template>
  <div class="min-h-screen bg-slate-100 text-slate-900">

    <!-- =====================================================
         SIDEBAR
    ====================================================== -->
    <AdminSidebar />

    <!-- =====================================================
         MAIN CONTENT
    ====================================================== -->
    <main class="ml-64 min-h-screen">
      <div class="p-4 sm:p-6 lg:p-8 max-w-7xl mx-auto">

        <!-- =================================================
             PAGE HEADER & BREADCRUMBS
        ================================================== -->
        <div class="mb-8">
          <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <div class="mb-2 flex items-center gap-2 text-sm text-slate-500">
                <span class="font-medium text-slate-600">Staff Portal</span>
                <svg class="h-4 w-4 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="m9 18 6-6-6-6" />
                </svg>
                <span class="font-semibold text-blue-600">Profile Management</span>
              </div>

              <h1 class="text-3xl font-black tracking-tight text-slate-950 sm:text-4xl">
                {{ pageTitle }}
              </h1>

              <p class="mt-2 text-sm text-slate-500 sm:text-base max-w-2xl">
                {{ pageSubtitle }}
              </p>
            </div>

            <!-- STATUS BADGE -->
            <div class="inline-flex w-fit items-center gap-2.5 rounded-2xl border border-white/90 bg-white/80 px-4 py-2.5 shadow-lg shadow-slate-300/20 backdrop-blur-xl">
              <span class="relative flex h-2.5 w-2.5">
                <span class="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-60"></span>
                <span class="relative inline-flex h-2.5 w-2.5 rounded-full bg-emerald-500"></span>
              </span>
              <span class="text-xs font-bold uppercase tracking-wider text-slate-600">
                {{ profile.status }} Account
              </span>
            </div>
          </div>
        </div>

        <!-- =================================================
             STAFF HERO CARD
        ================================================== -->
        <section class="relative mb-6 overflow-hidden rounded-3xl border border-white/90 bg-white/75 p-6 sm:p-8 shadow-2xl shadow-slate-300/25 backdrop-blur-2xl">
          <!-- Ambient gradient orbs -->
          <div class="pointer-events-none absolute -right-24 -top-24 h-72 w-72 rounded-full bg-blue-500/10 blur-3xl"></div>
          <div class="pointer-events-none absolute -bottom-24 -left-24 h-72 w-72 rounded-full bg-cyan-400/10 blur-3xl"></div>

          <div class="relative flex flex-col gap-6 sm:flex-row sm:items-center">

            <!-- AVATAR & UPLOAD TRIGGER -->
            <div class="relative shrink-0 group">
              <div class="h-28 w-28 overflow-hidden rounded-3xl border-4 border-white bg-slate-100 shadow-xl shadow-slate-300/30">
                <img :src="profile.avatar" :alt="profile.name" class="h-full w-full object-cover transition duration-300 group-hover:scale-105" />
              </div>

              <!-- Online badge -->
              <span class="absolute -bottom-1 -right-1 flex h-8 w-8 items-center justify-center rounded-full bg-white shadow-lg">
                <span class="h-3.5 w-3.5 rounded-full bg-emerald-500 ring-4 ring-emerald-100"></span>
              </span>

              <!-- Quick upload hover overlay -->
              <button
                type="button"
                @click="triggerAvatarUpload"
                :disabled="isUploadingAvatar"
                title="Click to change photo"
                class="absolute inset-0 flex flex-col items-center justify-center rounded-3xl bg-slate-950/60 opacity-0 transition-opacity duration-200 group-hover:opacity-100 text-white text-[11px] font-bold gap-1 cursor-pointer">
                <svg v-if="!isUploadingAvatar" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                  <polyline points="17 8 12 3 7 8" />
                  <line x1="12" y1="3" x2="12" y2="15" />
                </svg>
                <svg v-else class="h-5 w-5 animate-spin" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-dasharray="40 20" />
                </svg>
                <span>{{ isUploadingAvatar ? 'Uploading...' : 'Change' }}</span>
              </button>

              <!-- Hidden input for file picker -->
              <input
                ref="avatarFileInput"
                type="file"
                accept="image/jpeg,image/png,image/webp,image/gif"
                class="hidden"
                @change="handleAvatarFileSelected"
              />
            </div>

            <!-- PROFILE INFO -->
            <div class="flex-1">
              <div class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <div class="flex flex-wrap items-center gap-3">
                    <h2 class="text-2xl font-black text-slate-950 sm:text-3xl">
                      {{ profile.name }}
                    </h2>

                    <!-- Role Badge -->
                    <span
                      class="inline-flex items-center gap-1.5 rounded-full border px-3.5 py-1 text-xs font-bold ring-1 transition-all"
                      :class="getRoleBadgeClasses(profile.role)"
                    >
                      <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M12 2 4 5v6c0 5 3.5 9.5 8 11 4.5-1.5 8-6 8-11V5l-8-3z" />
                      </svg>
                      {{ profile.roleLabel }}
                    </span>
                  </div>

                  <p class="mt-1.5 text-sm font-medium text-slate-500">
                    {{ profile.email || 'No email registered' }}
                  </p>

                  <!-- METADATA CHIPS -->
                  <div class="mt-4 flex flex-wrap gap-x-6 gap-y-2 text-sm text-slate-600">
                    <!-- Staff ID -->
                    <div class="flex items-center gap-2">
                      <svg class="h-4 w-4 text-blue-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <rect x="3" y="4" width="18" height="16" rx="3" />
                        <circle cx="9" cy="10" r="2" />
                        <line x1="15" y1="8" x2="17" y2="8" />
                        <line x1="15" y1="12" x2="17" y2="12" />
                        <line x1="7" y1="16" x2="17" y2="16" />
                      </svg>
                      <span>ID: <strong class="text-slate-800">#{{ profile.id }}</strong></span>
                    </div>

                    <!-- Role Specific Detail in Hero -->
                    <div v-if="profile.role === 'WAREHOUSE_STAFF' && profile.warehouseLoc" class="flex items-center gap-2">
                      <svg class="h-4 w-4 text-emerald-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M3 7l9-4 9 4v10l-9 4-9-4V7zM3 7l9 4 9-4M12 11v10" />
                      </svg>
                      <span>Location: <strong class="text-slate-800">{{ profile.warehouseLoc }}</strong></span>
                    </div>

                    <div v-else-if="profile.role === 'SUPPORT_STAFF' && profile.department" class="flex items-center gap-2">
                      <svg class="h-4 w-4 text-violet-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M8 10h8M8 14h5M20 11.5a7.5 7.5 0 01-7.5 7.5c-1.4 0-2.7-.38-3.82-1.04L4 19l1.04-4.68A7.5 7.5 0 1120 11.5z" />
                      </svg>
                      <span>Dept: <strong class="text-slate-800">{{ profile.department }}</strong></span>
                    </div>

                    <div v-else-if="profile.role === 'DELIVERY_STAFF' && profile.vehicleNo" class="flex items-center gap-2">
                      <svg class="h-4 w-4 text-cyan-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M3 7h11v10H3zM14 10h4l3 3v4h-7zM6 20a2 2 0 100-4 2 2 0 000 4M18 20a2 2 0 100-4 2 2 0 000 4" />
                      </svg>
                      <span>Vehicle: <strong class="text-slate-800">{{ profile.vehicleNo }}</strong></span>
                    </div>

                    <div v-else-if="profile.role === 'ADMINISTRATOR' && profile.accessLevel" class="flex items-center gap-2">
                      <svg class="h-4 w-4 text-blue-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="9" />
                        <path d="m9 12 2 2 4-4" />
                      </svg>
                      <span>Access: <strong class="text-slate-800">{{ profile.accessLevel }}</strong></span>
                    </div>

                    <!-- Joined Date -->
                    <div class="flex items-center gap-2">
                      <svg class="h-4 w-4 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
                        <line x1="16" y1="2" x2="16" y2="6" />
                        <line x1="8" y1="2" x2="8" y2="6" />
                        <line x1="3" y1="10" x2="21" y2="10" />
                      </svg>
                      <span>Registered: {{ profile.joinedDate }}</span>
                    </div>
                  </div>
                </div>

                <!-- Avatar Change Button -->
                <button
                  type="button"
                  @click="triggerAvatarUpload"
                  :disabled="isUploadingAvatar"
                  class="inline-flex items-center gap-2 rounded-2xl border border-slate-200/90 bg-white/85 px-4 py-2.5 text-sm font-bold text-slate-700 shadow-sm transition hover:-translate-y-0.5 hover:border-blue-300 hover:text-blue-600 hover:shadow-md disabled:cursor-not-allowed disabled:opacity-60"
                >
                  <svg v-if="!isUploadingAvatar" class="h-4 w-4 text-blue-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                    <polyline points="17 8 12 3 7 8" />
                    <line x1="12" y1="3" x2="12" y2="15" />
                  </svg>
                  <svg v-else class="h-4 w-4 animate-spin text-blue-600" viewBox="0 0 24 24" fill="none">
                    <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-dasharray="40 20" />
                  </svg>
                  <span>{{ isUploadingAvatar ? 'Uploading...' : 'Change Photo' }}</span>
                </button>
              </div>
            </div>

          </div>
        </section>

        <!-- =================================================
             FEEDBACK ALERTS
        ================================================== -->
        <div v-if="isLoading" class="mb-6 flex items-center gap-3 rounded-2xl border border-blue-200 bg-blue-50/90 px-5 py-4 text-sm font-semibold text-blue-700 shadow-sm">
          <svg class="h-5 w-5 animate-spin" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-dasharray="40 20" />
          </svg>
          Loading your staff profile details...
        </div>

        <Transition enter-active-class="transition duration-300" enter-from-class="opacity-0 -translate-y-2" enter-to-class="opacity-100 translate-y-0" leave-active-class="transition duration-200" leave-from-class="opacity-100" leave-to-class="opacity-0">
          <div v-if="successMessage" class="mb-6 flex items-center gap-3 rounded-2xl border border-emerald-200 bg-emerald-50/95 px-5 py-4 text-sm font-semibold text-emerald-800 shadow-sm">
            <div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-emerald-100 text-emerald-600">
              <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <path d="m5 12 4 4L19 6" />
              </svg>
            </div>
            <span>{{ successMessage }}</span>
          </div>
        </Transition>

        <Transition enter-active-class="transition duration-300" enter-from-class="opacity-0 -translate-y-2" enter-to-class="opacity-100 translate-y-0" leave-active-class="transition duration-200" leave-from-class="opacity-100" leave-to-class="opacity-0">
          <div v-if="errorMessage" class="mb-6 flex items-center gap-3 rounded-2xl border border-red-200 bg-red-50/95 px-5 py-4 text-sm font-semibold text-red-700 shadow-sm">
            <div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-red-100 text-red-600">
              <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="9" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
            </div>
            <span>{{ errorMessage }}</span>
          </div>
        </Transition>

        <!-- =================================================
             NAVIGATION TABS
        ================================================== -->
        <div class="mb-6 inline-flex rounded-2xl border border-white/90 bg-white/70 p-1.5 shadow-lg shadow-slate-300/15 backdrop-blur-xl">
          <button
            type="button"
            @click="activeTab = 'profile'"
            class="flex items-center gap-2 rounded-xl px-5 py-2.5 text-sm font-bold transition-all"
            :class="activeTab === 'profile' ? 'bg-white text-blue-600 shadow-md shadow-slate-200/50' : 'text-slate-500 hover:text-slate-800'"
          >
            <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="8" r="4" />
              <path d="M4 21a8 8 0 0 1 16 0" />
            </svg>
            Profile Information
          </button>

          <button
            type="button"
            @click="activeTab = 'security'"
            class="flex items-center gap-2 rounded-xl px-5 py-2.5 text-sm font-bold transition-all"
            :class="activeTab === 'security' ? 'bg-white text-blue-600 shadow-md shadow-slate-200/50' : 'text-slate-500 hover:text-slate-800'"
          >
            <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="11" width="18" height="10" rx="2" />
              <path d="M7 11V7a5 5 0 0 1 10 0v4" />
            </svg>
            Password & Security
          </button>
        </div>

        <!-- =================================================
             TAB 1: PROFILE DETAILS
        ================================================== -->
        <section v-if="activeTab === 'profile'" class="grid grid-cols-1 gap-6 xl:grid-cols-3">

          <!-- PROFILE EDIT FORM -->
          <div class="rounded-3xl border border-white/90 bg-white/75 p-6 sm:p-8 shadow-xl shadow-slate-300/20 backdrop-blur-2xl xl:col-span-2">
            <div class="mb-7 flex items-center justify-between border-b border-slate-100 pb-5">
              <div>
                <h2 class="text-xl font-black text-slate-950">
                  Personal & Role Information
                </h2>
                <p class="mt-1 text-sm text-slate-500">
                  Update your contact info and assigned department credentials.
                </p>
              </div>

              <span class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-bold text-slate-600">
                Staff ID: #{{ profile.id }}
              </span>
            </div>

            <form class="space-y-6" @submit.prevent="handleSaveProfile">

              <!-- ROW 1: NAME & EMAIL -->
              <div class="grid grid-cols-1 gap-6 sm:grid-cols-2">
                <div>
                  <label class="mb-2 block text-sm font-bold text-slate-700">
                    Full Name <span class="text-red-500">*</span>
                  </label>
                  <div class="relative">
                    <svg class="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="12" cy="8" r="4" />
                      <path d="M4 21a8 8 0 0 1 16 0" />
                    </svg>
                    <input
                      v-model="form.name"
                      type="text"
                      required
                      placeholder="Enter your full name"
                      class="w-full rounded-2xl border border-slate-200/90 bg-white/80 py-3.5 pl-12 pr-4 text-sm text-slate-900 outline-none transition focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10"
                    />
                  </div>
                </div>

                <div>
                  <label class="mb-2 block text-sm font-bold text-slate-700">
                    Email Address <span class="text-red-500">*</span>
                  </label>
                  <div class="relative">
                    <svg class="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <rect x="3" y="5" width="18" height="14" rx="2" />
                      <polyline points="3 7 12 13 21 7" />
                    </svg>
                    <input
                      v-model="form.email"
                      type="email"
                      required
                      placeholder="staff@example.com"
                      class="w-full rounded-2xl border border-slate-200/90 bg-white/80 py-3.5 pl-12 pr-4 text-sm text-slate-900 outline-none transition focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10"
                    />
                  </div>
                </div>
              </div>

              <!-- ROW 2: PHONE & ROLE DISPLAY -->
              <div class="grid grid-cols-1 gap-6 sm:grid-cols-2">
                <div>
                  <label class="mb-2 block text-sm font-bold text-slate-700">
                    Phone Number
                  </label>
                  <div class="relative">
                    <svg class="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z" />
                    </svg>
                    <input
                      v-model="form.phone"
                      type="tel"
                      placeholder="e.g. 0771234567"
                      class="w-full rounded-2xl border border-slate-200/90 bg-white/80 py-3.5 pl-12 pr-4 text-sm text-slate-900 outline-none transition focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10"
                    />
                  </div>
                </div>

                <div>
                  <label class="mb-2 block text-sm font-bold text-slate-700">
                    System Role (Assigned)
                  </label>
                  <div class="flex items-center justify-between rounded-2xl border border-slate-200/80 bg-slate-50/80 px-4 py-3">
                    <div class="flex items-center gap-2.5">
                      <span class="flex h-8 w-8 items-center justify-center rounded-xl bg-blue-100 text-blue-700">
                        <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M12 2 4 5v6c0 5 3.5 9.5 8 11 4.5-1.5 8-6 8-11V5l-8-3z" />
                        </svg>
                      </span>
                      <span class="text-sm font-bold text-slate-800">{{ profile.roleLabel }}</span>
                    </div>
                    <span class="rounded-full px-2.5 py-0.5 text-xs font-bold" :class="getRoleBadgeClasses(profile.role)">
                      {{ profile.role }}
                    </span>
                  </div>
                </div>
              </div>

              <!-- ROLE-SPECIFIC FIELDS -->
              <!-- WAREHOUSE STAFF -->
              <div v-if="profile.role === 'WAREHOUSE_STAFF'" class="rounded-2xl border border-emerald-100 bg-emerald-50/50 p-4">
                <label class="mb-2 block text-sm font-bold text-emerald-950">
                  Assigned Warehouse Location
                </label>
                <div class="relative">
                  <svg class="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-emerald-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M3 7l9-4 9 4v10l-9 4-9-4V7zM3 7l9 4 9-4M12 11v10" />
                  </svg>
                  <input
                    v-model="form.warehouseLoc"
                    type="text"
                    placeholder="e.g. Colombo Central Warehouse, Bay 4"
                    class="w-full rounded-xl border border-emerald-200 bg-white py-3 pl-12 pr-4 text-sm text-slate-900 outline-none transition focus:border-emerald-500 focus:ring-4 focus:ring-emerald-500/10"
                  />
                </div>
              </div>

              <!-- SUPPORT STAFF -->
              <div v-if="profile.role === 'SUPPORT_STAFF'" class="rounded-2xl border border-violet-100 bg-violet-50/50 p-4">
                <label class="mb-2 block text-sm font-bold text-violet-950">
                  Support Department / Unit
                </label>
                <div class="relative">
                  <svg class="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-violet-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M8 10h8M8 14h5M20 11.5a7.5 7.5 0 01-7.5 7.5c-1.4 0-2.7-.38-3.82-1.04L4 19l1.04-4.68A7.5 7.5 0 1120 11.5z" />
                  </svg>
                  <input
                    v-model="form.department"
                    type="text"
                    placeholder="e.g. Technical Support & RMA"
                    class="w-full rounded-xl border border-violet-200 bg-white py-3 pl-12 pr-4 text-sm text-slate-900 outline-none transition focus:border-violet-500 focus:ring-4 focus:ring-violet-500/10"
                  />
                </div>
              </div>

              <!-- DELIVERY STAFF -->
              <div v-if="profile.role === 'DELIVERY_STAFF'" class="grid grid-cols-1 gap-4 sm:grid-cols-2 rounded-2xl border border-cyan-100 bg-cyan-50/50 p-4">
                <div>
                  <label class="mb-2 block text-sm font-bold text-cyan-950">
                    Vehicle Number
                  </label>
                  <input
                    v-model="form.vehicleNo"
                    type="text"
                    placeholder="e.g. WP CAD-4592"
                    class="w-full rounded-xl border border-cyan-200 bg-white py-3 px-4 text-sm text-slate-900 outline-none transition focus:border-cyan-500 focus:ring-4 focus:ring-cyan-500/10"
                  />
                </div>
                <div>
                  <label class="mb-2 block text-sm font-bold text-cyan-950">
                    Driving License Number
                  </label>
                  <input
                    v-model="form.licenseNo"
                    type="text"
                    placeholder="e.g. B1284950"
                    class="w-full rounded-xl border border-cyan-200 bg-white py-3 px-4 text-sm text-slate-900 outline-none transition focus:border-cyan-500 focus:ring-4 focus:ring-cyan-500/10"
                  />
                </div>
              </div>

              <!-- ADMINISTRATOR -->
              <div v-if="profile.role === 'ADMINISTRATOR'" class="rounded-2xl border border-blue-100 bg-blue-50/50 p-4">
                <label class="mb-2 block text-sm font-bold text-blue-950">
                  Access Level
                </label>
                <input
                  v-model="form.accessLevel"
                  type="text"
                  placeholder="e.g. FULL_ACCESS"
                  class="w-full rounded-xl border border-blue-200 bg-white py-3 px-4 text-sm text-slate-900 outline-none transition focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10"
                />
              </div>

              <!-- ADDRESS SECTION -->
              <div class="rounded-2xl border border-slate-200/90 bg-slate-50/60 p-5">
                <div class="mb-4 flex items-center justify-between">
                  <div>
                    <h3 class="text-sm font-bold text-slate-900">Residential Address</h3>
                    <p class="text-xs text-slate-500">Used for official staff records and correspondence.</p>
                  </div>
                  <svg class="h-5 w-5 text-blue-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0Z" />
                    <circle cx="12" cy="10" r="2.5" />
                  </svg>
                </div>

                <div class="space-y-3">
                  <div>
                    <label class="mb-1 block text-xs font-semibold text-slate-600">Street Address</label>
                    <input
                      v-model="form.address.street"
                      type="text"
                      placeholder="e.g. 15 Galle Road, Suite 4"
                      class="w-full rounded-xl border border-slate-200 bg-white py-2.5 px-3.5 text-sm text-slate-900 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10"
                    />
                  </div>

                  <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
                    <div>
                      <label class="mb-1 block text-xs font-semibold text-slate-600">City</label>
                      <input
                        v-model="form.address.city"
                        type="text"
                        placeholder="e.g. Colombo"
                        class="w-full rounded-xl border border-slate-200 bg-white py-2.5 px-3.5 text-sm text-slate-900 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10"
                      />
                    </div>
                    <div>
                      <label class="mb-1 block text-xs font-semibold text-slate-600">Postal Code</label>
                      <input
                        v-model="form.address.postalCode"
                        type="text"
                        placeholder="e.g. 00300"
                        class="w-full rounded-xl border border-slate-200 bg-white py-2.5 px-3.5 text-sm text-slate-900 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10"
                      />
                    </div>
                  </div>
                </div>
              </div>

              <!-- SUBMIT BUTTON -->
              <div class="flex justify-end pt-2">
                <button
                  type="submit"
                  :disabled="isSaving"
                  class="inline-flex items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-blue-600 via-blue-700 to-indigo-700 px-7 py-3.5 text-sm font-bold text-white shadow-xl shadow-blue-600/25 transition-all hover:-translate-y-0.5 hover:shadow-2xl hover:shadow-blue-600/35 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  <svg v-if="isSaving" class="h-5 w-5 animate-spin" viewBox="0 0 24 24" fill="none">
                    <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-dasharray="40 20" />
                  </svg>
                  <svg v-else class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" />
                    <polyline points="17 21 17 13 7 13 7 21" />
                    <polyline points="7 3 7 8 15 8" />
                  </svg>
                  {{ isSaving ? 'Saving Changes...' : 'Save Profile Changes' }}
                </button>
              </div>

            </form>
          </div>

          <!-- RIGHT COLUMN: ACCOUNT SUMMARY & ROLE HIGHLIGHTS -->
          <div class="space-y-6">

            <!-- SUMMARY CARD -->
            <div class="rounded-3xl border border-white/90 bg-white/75 p-6 sm:p-8 shadow-xl shadow-slate-300/20 backdrop-blur-2xl">
              <h2 class="text-xl font-black text-slate-950">
                Staff Account Overview
              </h2>
              <p class="mt-1 text-sm text-slate-500">
                Summary of your staff system privileges.
              </p>

              <div class="mt-6 space-y-3.5">

                <!-- ID -->
                <div class="rounded-2xl border border-blue-100 bg-blue-50/70 p-4">
                  <p class="text-xs font-bold uppercase tracking-wider text-blue-600">
                    System Staff ID
                  </p>
                  <p class="mt-1 text-xl font-black text-slate-950">
                    #{{ profile.id }}
                  </p>
                </div>

                <!-- ROLE -->
                <div class="rounded-2xl border border-slate-200/70 bg-white/80 p-4">
                  <p class="text-xs font-bold uppercase tracking-wider text-slate-400">
                    Staff Designation
                  </p>
                  <p class="mt-1 text-base font-bold text-slate-900">
                    {{ profile.roleLabel }}
                  </p>
                </div>

                <!-- ASSIGNMENT -->
                <div v-if="profile.role === 'WAREHOUSE_STAFF'" class="rounded-2xl border border-emerald-100 bg-emerald-50/60 p-4">
                  <p class="text-xs font-bold uppercase tracking-wider text-emerald-700">
                    Warehouse Hub
                  </p>
                  <p class="mt-1 text-sm font-bold text-slate-900">
                    {{ profile.warehouseLoc || 'Not assigned yet' }}
                  </p>
                </div>

                <div v-else-if="profile.role === 'SUPPORT_STAFF'" class="rounded-2xl border border-violet-100 bg-violet-50/60 p-4">
                  <p class="text-xs font-bold uppercase tracking-wider text-violet-700">
                    Support Unit
                  </p>
                  <p class="mt-1 text-sm font-bold text-slate-900">
                    {{ profile.department || 'Not assigned yet' }}
                  </p>
                </div>

                <div v-else-if="profile.role === 'DELIVERY_STAFF'" class="rounded-2xl border border-cyan-100 bg-cyan-50/60 p-4">
                  <p class="text-xs font-bold uppercase tracking-wider text-cyan-700">
                    Assigned Vehicle & License
                  </p>
                  <p class="mt-1 text-sm font-bold text-slate-900">
                    {{ profile.vehicleNo ? `${profile.vehicleNo} (${profile.licenseNo || 'N/A'})` : 'Not assigned yet' }}
                  </p>
                </div>

                <div v-else-if="profile.role === 'ADMINISTRATOR'" class="rounded-2xl border border-blue-100 bg-blue-50/60 p-4">
                  <p class="text-xs font-bold uppercase tracking-wider text-blue-700">
                    Access Clearance
                  </p>
                  <p class="mt-1 text-sm font-bold text-slate-900">
                    {{ profile.accessLevel || 'FULL_ACCESS' }}
                  </p>
                </div>

                <!-- STATUS -->
                <div class="rounded-2xl border border-slate-200/70 bg-white/80 p-4 flex items-center justify-between">
                  <div>
                    <p class="text-xs font-bold uppercase tracking-wider text-slate-400">Status</p>
                    <p class="mt-0.5 text-sm font-bold text-emerald-700">{{ profile.status }}</p>
                  </div>
                  <span class="flex h-3 w-3 rounded-full bg-emerald-500 shadow-sm shadow-emerald-500/50"></span>
                </div>

                <!-- REGISTRATION -->
                <div class="rounded-2xl border border-slate-200/70 bg-white/80 p-4">
                  <p class="text-xs font-bold uppercase tracking-wider text-slate-400">
                    Joined Date
                  </p>
                  <p class="mt-1 text-sm font-bold text-slate-800">
                    {{ profile.joinedDate }}
                  </p>
                </div>

              </div>

              <!-- SECURITY BADGE -->
              <div class="mt-6 flex gap-3 rounded-2xl border border-emerald-100 bg-emerald-50/70 p-4">
                <svg class="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 2 4 5v6c0 5 3.5 9.5 8 11 4.5-1.5 8-6 8-11V5l-8-3z" />
                  <path d="m9 12 2 2 4-4" />
                </svg>
                <div>
                  <p class="text-sm font-bold text-emerald-800">Verified Staff Account</p>
                  <p class="mt-1 text-xs leading-5 text-emerald-700">
                    Your staff account is authenticated and protected under role-based system policies.
                  </p>
                </div>
              </div>
            </div>

          </div>

        </section>

        <!-- =================================================
             TAB 2: SECURITY & PASSWORD
        ================================================== -->
        <section v-if="activeTab === 'security'" class="grid grid-cols-1 gap-6 xl:grid-cols-3">

          <!-- CHANGE PASSWORD FORM -->
          <div class="rounded-3xl border border-white/90 bg-white/75 p-6 sm:p-8 shadow-xl shadow-slate-300/20 backdrop-blur-2xl xl:col-span-2">
            <div class="mb-7 flex items-center gap-3.5 border-b border-slate-100 pb-5">
              <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-blue-50 text-blue-600">
                <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="11" width="18" height="10" rx="2" />
                  <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                </svg>
              </div>
              <div>
                <h2 class="text-xl font-black text-slate-950">
                  Update Account Password
                </h2>
                <p class="text-sm text-slate-500">
                  Enter your existing password and choose a secure new password.
                </p>
              </div>
            </div>

            <form class="space-y-6" @submit.prevent="handleChangePassword">

              <!-- CURRENT PASSWORD -->
              <div>
                <label class="mb-2 block text-sm font-bold text-slate-700">
                  Current Password <span class="text-red-500">*</span>
                </label>
                <input
                  v-model="passwordForm.currentPassword"
                  type="password"
                  required
                  placeholder="Enter current password"
                  class="w-full rounded-2xl border border-slate-200/90 bg-white/80 px-4 py-3.5 text-sm text-slate-900 outline-none transition focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10"
                />
              </div>

              <!-- NEW PASSWORD -->
              <div>
                <label class="mb-2 block text-sm font-bold text-slate-700">
                  New Password <span class="text-red-500">*</span>
                </label>
                <div class="relative">
                  <input
                    v-model="passwordForm.newPassword"
                    :type="showPassword ? 'text' : 'password'"
                    required
                    placeholder="Enter new password (min 6 characters)"
                    class="w-full rounded-2xl border border-slate-200/90 bg-white/80 px-4 py-3.5 pr-12 text-sm text-slate-900 outline-none transition focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10"
                  />
                  <button
                    type="button"
                    @click="showPassword = !showPassword"
                    class="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 hover:text-blue-600"
                  >
                    <svg v-if="!showPassword" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10 7-10 7z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                    <svg v-else class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="m3 3 18 18" />
                      <path d="M10.58 10.58a2 2 0 0 0 2.83 2.83" />
                      <path d="M9.88 5.09A10.94 10.94 0 0 1 12 5c6.5 0 10 7 10 7a18.7 18.7 0 0 1-3.03 3.92" />
                      <path d="M6.61 6.61C3.55 8.32 2 12 2 12a18.7 18.7 0 0 0 3.03 3.92A10.94 10.94 0 0 0 12 19c1.61 0 3.08-.34 4.4-.91" />
                    </svg>
                  </button>
                </div>

                <!-- STRENGTH BAR -->
                <div class="mt-3">
                  <div class="mb-1 flex items-center justify-between text-xs">
                    <span class="text-slate-400">Password strength:</span>
                    <span
                      class="font-bold"
                      :class="passwordStrength.label === 'Strong' ? 'text-emerald-600' : passwordStrength.label === 'Medium' ? 'text-amber-600' : 'text-red-600'"
                    >
                      {{ passwordStrength.label }}
                    </span>
                  </div>
                  <div class="h-1.5 overflow-hidden rounded-full bg-slate-200">
                    <div
                      class="h-full rounded-full transition-all duration-300"
                      :class="passwordStrength.class"
                      :style="{ width: passwordStrength.width }"
                    ></div>
                  </div>
                </div>
              </div>

              <!-- CONFIRM PASSWORD -->
              <div>
                <label class="mb-2 block text-sm font-bold text-slate-700">
                  Confirm New Password <span class="text-red-500">*</span>
                </label>
                <div class="relative">
                  <input
                    v-model="passwordForm.confirmPassword"
                    :type="showConfirmPassword ? 'text' : 'password'"
                    required
                    placeholder="Repeat new password"
                    class="w-full rounded-2xl border bg-white/80 px-4 py-3.5 pr-12 text-sm text-slate-900 outline-none transition focus:ring-4 focus:ring-blue-500/10"
                    :class="!passwordsMatch ? 'border-red-300 focus:border-red-400' : 'border-slate-200/90 focus:border-blue-500'"
                  />
                  <button
                    type="button"
                    @click="showConfirmPassword = !showConfirmPassword"
                    class="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 hover:text-blue-600"
                  >
                    <svg v-if="!showConfirmPassword" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10 7-10 7z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                    <svg v-else class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="m3 3 18 18" />
                      <path d="M10.58 10.58a2 2 0 0 0 2.83 2.83" />
                      <path d="M9.88 5.09A10.94 10.94 0 0 1 12 5c6.5 0 10 7 10 7a18.7 18.7 0 0 1-3.03 3.92" />
                      <path d="M6.61 6.61C3.55 8.32 2 12 2 12a18.7 18.7 0 0 0 3.03 3.92A10.94 10.94 0 0 0 12 19c1.61 0 3.08-.34 4.4-.91" />
                    </svg>
                  </button>
                </div>
                <p v-if="!passwordsMatch" class="mt-2 text-xs font-semibold text-red-600">
                  Passwords do not match.
                </p>
              </div>

              <!-- SUBMIT BUTTON -->
              <div class="flex justify-end pt-2">
                <button
                  type="submit"
                  :disabled="isChangingPassword || !passwordsMatch"
                  class="inline-flex items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-blue-600 via-blue-700 to-indigo-700 px-7 py-3.5 text-sm font-bold text-white shadow-xl shadow-blue-600/25 transition-all hover:-translate-y-0.5 hover:shadow-2xl hover:shadow-blue-600/35 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  <svg v-if="isChangingPassword" class="h-5 w-5 animate-spin" viewBox="0 0 24 24" fill="none">
                    <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-dasharray="40 20" />
                  </svg>
                  <svg v-else class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="11" width="18" height="10" rx="2" />
                    <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                  </svg>
                  {{ isChangingPassword ? 'Updating Password...' : 'Update Password' }}
                </button>
              </div>

            </form>
          </div>

          <!-- SECURITY GUIDANCE -->
          <div class="rounded-3xl border border-white/90 bg-white/75 p-6 sm:p-8 shadow-xl shadow-slate-300/20 backdrop-blur-2xl">
            <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-600">
              <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
              </svg>
            </div>

            <h2 class="mt-5 text-xl font-black text-slate-950">
              Security Best Practices
            </h2>

            <div class="mt-5 space-y-4">
              <div class="flex gap-3">
                <span class="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-emerald-500"></span>
                <p class="text-sm leading-6 text-slate-600">
                  Use a minimum of 8 characters with a mix of uppercase letters, numbers, and symbols.
                </p>
              </div>

              <div class="flex gap-3">
                <span class="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-blue-500"></span>
                <p class="text-sm leading-6 text-slate-600">
                  Never share your staff credentials or password with any other colleagues.
                </p>
              </div>

              <div class="flex gap-3">
                <span class="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-cyan-500"></span>
                <p class="text-sm leading-6 text-slate-600">
                  Update your password regularly and avoid using the same password across multiple systems.
                </p>
              </div>

              <div class="flex gap-3">
                <span class="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-violet-500"></span>
                <p class="text-sm leading-6 text-slate-600">
                  Always log out from shared management terminals when finishing your shift.
                </p>
              </div>
            </div>
          </div>

        </section>

      </div>
    </main>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>