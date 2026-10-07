<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import logo from '../../assets/icons/logoIMG-removebg-preview.svg'
import { getActiveStaffRole, isRoleAllowed, ROLE_HOME_ROUTES } from '../../utils/rbac'
import AdminConfirmModal from './AdminConfirmModal.vue'

const router = useRouter()

const staffName = ref('Staff Member')
const staffRole = ref('Administrator')
const currentRole = ref(getActiveStaffRole())
const staffAvatar = ref(
  'https://images.unsplash.com/photo-1560250097-0b93528c311a?auto=format&fit=crop&w=200&q=80'
)

const formatRole = (role?: string) => {
  if (!role) return 'Staff Member'
  const mapping: Record<string, string> = {
    ADMINISTRATOR: 'Administrator',
    WAREHOUSE_STAFF: 'Warehouse Staff',
    SUPPORT_STAFF: 'Support Staff',
    DELIVERY_STAFF: 'Delivery Staff'
  }
  return mapping[role] || role
}

const syncUserFromStorage = () => {
  try {
    currentRole.value = getActiveStaffRole()
    const raw = sessionStorage.getItem('staffUser') || sessionStorage.getItem('user')
    if (raw) {
      const u = JSON.parse(raw)
      if (u.name) staffName.value = u.name
      if (u.role) staffRole.value = formatRole(u.role)
      if (u.userImage) {
        staffAvatar.value =
          u.userImage.startsWith('http://') ||
            u.userImage.startsWith('https://') ||
            u.userImage.startsWith('data:')
            ? u.userImage
            : `http://localhost:8080/uploads/${u.userImage.replace(/^\/+/, '')}`
      } else if (u.name) {
        staffAvatar.value = `https://ui-avatars.com/api/?name=${encodeURIComponent(u.name)}&background=2563eb&color=fff`
      }
    }
  } catch {
    // fallback
  }
}

const canAccess = (path: string) => {
  return isRoleAllowed(currentRole.value, path)
}

const isAdmin = computed(() => currentRole.value === 'ADMINISTRATOR')

onMounted(() => {
  syncUserFromStorage()
  window.addEventListener('user-profile-updated', syncUserFromStorage)
  window.addEventListener('storage', syncUserFromStorage)
})

onUnmounted(() => {
  window.removeEventListener('user-profile-updated', syncUserFromStorage)
  window.removeEventListener('storage', syncUserFromStorage)
})

const showLogoutModal = ref(false)
const isLoggingOut = ref(false)

const openLogoutConfirm = () => {
  showLogoutModal.value = true
}

const executeLogout = () => {
  isLoggingOut.value = true
  try {
    localStorage.removeItem('user')
    sessionStorage.removeItem('user')
    sessionStorage.removeItem('staffUser')
    sessionStorage.removeItem('userId')
    sessionStorage.removeItem('staffId')
    sessionStorage.removeItem('adminId')
    sessionStorage.removeItem('role')

    showLogoutModal.value = false
    router.push('/admin/staff-login')
  } finally {
    isLoggingOut.value = false
  }
}
</script>

<template>
  <aside class="fixed left-0 top-0 z-50 h-screen w-64
           bg-white/75 backdrop-blur-2xl
           border-r border-white/90
           shadow-2xl shadow-slate-300/20
           flex flex-col">

    <!-- Logo -->
    <div class="h-20 px-5 flex items-center border-b border-slate-200/70">
      <RouterLink :to="canAccess('/admin/dashboard') ? '/admin/dashboard' : (ROLE_HOME_ROUTES[currentRole] || '/admin')" class="flex items-center gap-3">

        <!-- Logo -->

        <div class="w-12 h-12 flex items-center justify-center overflow-hidden">
          <img :src="logo" alt="NX Logo" class="w-14 h-14 object-contain" />
        </div>
        <!-- Brand Name -->
        <div class="hidden sm:block leading-none mt-1">
          <div class="text-[17px] font-black tracking-[-0.04em] text-slate-950">
            NEXUS<span class="text-blue-600">RIGS</span>
          </div>

          <div class="text-[8px] font-bold uppercase tracking-[0.22em] text-slate-400 mt-1">
            Performance Hardware
          </div>
        </div>

      </RouterLink>
    </div>

    <!-- Admin / Staff Profile Link -->
    <div class="p-4">
      <RouterLink to="/admin/profile" class="block rounded-2xl bg-white/60 backdrop-blur-xl
           border border-white/90
           shadow-lg shadow-slate-200/30 p-3
           transition-all duration-200
           hover:bg-white/80 hover:shadow-xl
           hover:-translate-y-0.5
           cursor-pointer" title="View & Edit Profile">
        <div class="flex items-center gap-3">

          <div class="relative shrink-0">
            <img :src="staffAvatar" :alt="staffName" class="w-11 h-11 rounded-xl object-cover
                 border-2 border-white shadow-md bg-slate-100" />

            <span class="absolute -right-0.5 -bottom-0.5
                 w-3 h-3 rounded-full
                 bg-emerald-500 border-2 border-white"></span>
          </div>

          <div class="min-w-0 flex-1">
            <p class="text-sm font-bold text-slate-900 truncate" :title="staffName">
              {{ staffName }}
            </p>

            <p class="text-[11px] font-medium text-blue-600 truncate" :title="staffRole">
              {{ staffRole }}
            </p>
          </div>

        </div>
      </RouterLink>
    </div>
    <!-- Navigation -->
    <nav class="flex-1 px-3 overflow-y-auto scrollbar-hide">

      <!-- Management -->
      <p class="px-3 mb-2 text-[10px] font-black uppercase
               tracking-[0.18em] text-slate-400">
        Management
      </p>

      <div class="space-y-1">

        <!-- Dashboard -->
        <RouterLink v-if="canAccess('/admin/dashboard')" to="/admin/dashboard" class="group flex items-center gap-3 px-3 py-3
                 rounded-xl text-sm font-semibold
                 text-slate-600 hover:text-blue-600
                 hover:bg-blue-50/80 transition-all duration-200" active-class="!bg-blue-50 !text-blue-600 shadow-sm">
          <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-4 0h4" />
          </svg>

          <span>Dashboard</span>
        </RouterLink>

        <!-- Products -->
        <RouterLink v-if="canAccess('/admin/products')" to="/admin/products" class="group flex items-center gap-3 px-3 py-3
                 rounded-xl text-sm font-semibold
                 text-slate-600 hover:text-blue-600
                 hover:bg-blue-50/80 transition-all duration-200" active-class="!bg-blue-50 !text-blue-600 shadow-sm">
          <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
          </svg>

          <span>Products</span>
        </RouterLink>

        <!-- Orders -->
        <RouterLink v-if="canAccess('/admin/orders')" to="/admin/orders" class="group flex items-center gap-3 px-3 py-3
                 rounded-xl text-sm font-semibold
                 text-slate-600 hover:text-blue-600
                 hover:bg-blue-50/80 transition-all duration-200" active-class="!bg-blue-50 !text-blue-600 shadow-sm">
          <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a3 3 0 006 0M9 5h6m-6 7h6m-6 4h4" />
          </svg>

          <span>Orders</span>
        </RouterLink>

        <!-- Customers -->
        <RouterLink v-if="canAccess('/admin/customers')" to="/admin/customers" class="group flex items-center gap-3 px-3 py-3
                 rounded-xl text-sm font-semibold
                 text-slate-600 hover:text-blue-600
                 hover:bg-blue-50/80 transition-all duration-200" active-class="!bg-blue-50 !text-blue-600 shadow-sm">
          <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M17 20h5v-2a4 4 0 00-4-4h-1M9 20H4v-2a4 4 0 014-4h1m4-5a4 4 0 100-8 4 4 0 000 8zm6 1a3 3 0 100-6 3 3 0 000 6z" />
          </svg>

          <span>Customers</span>
        </RouterLink>

        <!-- Categories -->
        <RouterLink v-if="canAccess('/admin/categories')" to="/admin/categories" class="group flex items-center gap-3 px-3 py-3
                 rounded-xl text-sm font-semibold
                 text-slate-600 hover:text-blue-600
                 hover:bg-blue-50/80 transition-all duration-200" active-class="!bg-blue-50 !text-blue-600 shadow-sm">
          <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
          </svg>

          <span>Categories</span>
        </RouterLink>

        <!-- Inventory -->
        <RouterLink v-if="canAccess('/admin/inventory')" to="/admin/inventory" class="group flex items-center gap-3 px-3 py-3
                 rounded-xl text-sm font-semibold
                 text-slate-600 hover:text-blue-600
                 hover:bg-blue-50/80 transition-all duration-200" active-class="!bg-blue-50 !text-blue-600 shadow-sm">
          <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M3 7l9-4 9 4v10l-9 4-9-4V7zM3 7l9 4 9-4M12 11v10" />
          </svg>

          <span>Inventory</span>
        </RouterLink>

        <!-- Coupons -->
        <RouterLink v-if="canAccess('/admin/coupons')" to="/admin/coupons" class="group flex items-center gap-3 px-3 py-3
                 rounded-xl text-sm font-semibold
                 text-slate-600 hover:text-blue-600
                 hover:bg-blue-50/80 transition-all duration-200" active-class="!bg-blue-50 !text-blue-600 shadow-sm">
          <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M15 5l-1.5-1.5a2.121 2.121 0 00-3 0L9 5H6a2 2 0 00-2 2v3l-1.5 1.5a2.121 2.121 0 000 3L4 16v3a2 2 0 002 2h3l1.5 1.5a2.121 2.121 0 003 0L15 21h3a2 2 0 002-2v-3l1.5-1.5a2.121 2.121 0 000-3L20 10V7a2 2 0 00-2-2h-3z" />
          </svg>

          <span>Coupons</span>
        </RouterLink>

        <!-- Rating Management -->
        <RouterLink v-if="canAccess('/admin/rating-management')" to="/admin/rating-management" class="group flex items-center gap-3 px-3 py-3
         rounded-xl text-sm font-semibold
         text-slate-600 hover:text-blue-600
         hover:bg-blue-50/80 transition-all duration-200" active-class="!bg-blue-50 !text-blue-600 shadow-sm">
          <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 17.25l-5.878 3.09 1.122-6.545L2.49 9.41l6.56-.953L12 2.5l2.95 5.957 6.56.953-4.754 4.385 1.122 6.545L12 17.25z" />
          </svg>

          <span>Rating Management</span>
        </RouterLink>

        <!-- Discount Management -->
        <RouterLink v-if="canAccess('/admin/discount-management')" to="/admin/discount-management" class="group flex items-center gap-3 px-3 py-3
         rounded-xl text-sm font-semibold
         text-slate-600 hover:text-blue-600
         hover:bg-blue-50/80 transition-all duration-200" active-class="!bg-blue-50 !text-blue-600 shadow-sm">
          <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M9 14l6-6m-7.5-4h9A2.5 2.5 0 0119 6.5v11a2.5 2.5 0 01-2.5 2.5h-9A2.5 2.5 0 015 17.5v-11A2.5 2.5 0 017.5 4zM8 8h.01M16 16h.01" />
          </svg>

          <span>Discount Management</span>
        </RouterLink>

        <!-- Return & Refund -->
        <RouterLink v-if="canAccess('/admin/returns-refunds')" to="/admin/returns-refunds" class="group flex items-center gap-3 px-3 py-3
         rounded-xl text-sm font-semibold
         text-slate-600 hover:text-blue-600
         hover:bg-blue-50/80 transition-all duration-200" active-class="!bg-blue-50 !text-blue-600 shadow-sm">
          <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 7v6h6
         M20 17v-6h-6
         M18.5 8.5A7 7 0 005.5 7
         M5.5 15.5A7 7 0 0018.5 17
         M4 13l2-2 2 2
         M20 11l-2 2-2-2" />
          </svg>

          <span>Return & Refund</span>
        </RouterLink>

        <!-- Customer Support -->
        <RouterLink v-if="canAccess('/admin/customer-support')" to="/admin/customer-support" class="group flex items-center gap-3 px-3 py-3
         rounded-xl text-sm font-semibold
         text-slate-600 hover:text-blue-600
         hover:bg-blue-50/80 transition-all duration-200" active-class="!bg-blue-50 !text-blue-600 shadow-sm">
          <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h8
         M8 14h5
         M20 11.5a7.5 7.5 0 01-7.5 7.5
         c-1.4 0-2.7-.38-3.82-1.04
         L4 19l1.04-4.68
         A7.5 7.5 0 1120 11.5z" />
          </svg>

          <span>Customer Support</span>
        </RouterLink>

        <!-- Delivery Management -->
        <RouterLink v-if="canAccess('/admin/delivery-management')" to="/admin/delivery-management" class="group flex items-center gap-3 px-3 py-3
         rounded-xl text-sm font-semibold
         text-slate-600 hover:text-blue-600
         hover:bg-blue-50/80 transition-all duration-200" active-class="!bg-blue-50 !text-blue-600 shadow-sm">
          <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7h11v10H3z
         M14 10h4l3 3v4h-7z
         M6 20a2 2 0 100-4 2 2 0 000 4
         M18 20a2 2 0 100-4 2 2 0 000 4" />
          </svg>

          <span>Delivery Management</span>
        </RouterLink>

        <!-- Manage System Users -->
        <RouterLink v-if="canAccess('/admin/system-users')" to="/admin/system-users" class="group flex items-center gap-3 px-3 py-3
         rounded-xl text-sm font-semibold
         text-slate-600 hover:text-blue-600
         hover:bg-blue-50/80 transition-all duration-200" active-class="!bg-blue-50 !text-blue-600 shadow-sm">
          <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 21v-2a4 4 0 00-4-4H6a4 4 0 00-4 4v2
         M9 11a4 4 0 100-8 4 4 0 000 8z
         M22 21v-2a4 4 0 00-3-3.87
         M16 3.13a4 4 0 010 7.75" />
          </svg>

          <span>Manage System Users</span>
        </RouterLink>

      </div>

      <!-- Analytics -->
      <template v-if="canAccess('/admin/analytics')">
        <p class="px-3 mt-7 mb-2 text-[10px] font-black uppercase
                 tracking-[0.18em] text-slate-400">
          Analytics
        </p>

        <div class="space-y-1">

          <!-- Analytics -->
          <RouterLink to="/admin/analytics" class="group flex items-center gap-3 px-3 py-3
                   rounded-xl text-sm font-semibold
                   text-slate-600 hover:text-blue-600
                   hover:bg-blue-50/80 transition-all duration-200" active-class="!bg-blue-50 !text-blue-600 shadow-sm">
            <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M4 19V5m0 14h16M8 16v-4m4 4V8m4 8V6" />
            </svg>

            <span>Analytics</span>
          </RouterLink>

          <!-- Reports -->
          <RouterLink to="/admin/reports" class="group flex items-center gap-3 px-3 py-3
                   rounded-xl text-sm font-semibold
                   text-slate-600 hover:text-blue-600
                   hover:bg-blue-50/80 transition-all duration-200" active-class="!bg-blue-50 !text-blue-600 shadow-sm">
            <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M7 3h10a2 2 0 012 2v14a2 2 0 01-2 2H7a2 2 0 01-2-2V5a2 2 0 012-2zM9 8h6M9 12h6M9 16h4" />
            </svg>

            <span>Reports</span>
          </RouterLink>

        </div>
      </template>

      <!-- System -->
      <template v-if="canAccess('/admin/settings')">
        <p class="px-3 mt-7 mb-2 text-[10px] font-black uppercase
                 tracking-[0.18em] text-slate-400">
          System
        </p>

        <div class="space-y-1">

          <!-- Settings -->
          <RouterLink to="/admin/settings" class="group flex items-center gap-3 px-3 py-3
                   rounded-xl text-sm font-semibold
                   text-slate-600 hover:text-blue-600
                   hover:bg-blue-50/80 transition-all duration-200" active-class="!bg-blue-50 !text-blue-600 shadow-sm">
            <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M12 15.5a3.5 3.5 0 100-7 3.5 3.5 0 000 7z" />

              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M19.4 15a1.7 1.7 0 00.34 1.88l.06.06-1.8 1.8-.06-.06a1.7 1.7 0 00-1.88-.34 1.7 1.7 0 00-1.03 1.56V20h-2.55v-.1a1.7 1.7 0 00-1.03-1.56 1.7 1.7 0 00-1.88.34l-.06.06-1.8-1.8.06-.06A1.7 1.7 0 008.1 15a1.7 1.7 0 00-1.56-1.03H6V11.4h.54A1.7 1.7 0 008.1 10a1.7 1.7 0 00-.34-1.88L7.7 8.06l1.8-1.8.06.06A1.7 1.7 0 0011.44 6a1.7 1.7 0 001.03-1.56V4h2.55v.44A1.7 1.7 0 0016.05 6a1.7 1.7 0 001.88.34l.06-.06 1.8 1.8-.06.06A1.7 1.7 0 0019.4 10a1.7 1.7 0 001.56 1.03H21v2.55h-.04A1.7 1.7 0 0019.4 15z" />
            </svg>

            <span>Settings</span>
          </RouterLink>

        </div>
      </template>

    </nav>

    <!-- Bottom -->
    <div class="p-3 border-t border-slate-200/70 space-y-2">

      <!-- System Status -->
      <div class="flex items-center gap-2 px-3 py-2
               rounded-xl bg-emerald-50/70
               border border-emerald-100">
        <span class="w-2 h-2 rounded-full bg-emerald-500
                 shadow-sm shadow-emerald-500/50"></span>

        <span class="text-[11px] font-semibold text-emerald-700">
          All Systems Operational
        </span>
      </div>

      <!-- Logout -->
      <button @click="openLogoutConfirm" class="w-full flex items-center gap-3 px-3 py-3
               rounded-xl text-sm font-semibold
               text-slate-500 hover:text-red-600
               hover:bg-red-50/80 transition-all duration-200">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013 3v1" />
        </svg>

        <span>Logout</span>
      </button>

    </div>

    <!-- =====================================================
         LOGOUT CONFIRMATION MODAL (ORDER MANAGEMENT DESIGN)
         ===================================================== -->
    <AdminConfirmModal
      v-model:show="showLogoutModal"
      type="danger"
      icon="logout"
      title="Sign Out?"
      message="Are you sure you want to log out of the admin platform"
      :target="staffName"
      description="Your active session will be closed and you will need to sign in again to access administrative modules."
      confirm-text="Log Out"
      cancel-text="Cancel"
      :loading="isLoggingOut"
      @confirm="executeLogout"
    />

  </aside>
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