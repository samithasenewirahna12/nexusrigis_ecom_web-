<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import axios from 'axios'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminConfirmModal, { type ModalType, type AdminModalState } from '../../components/admin/AdminConfirmModal.vue'

// =========================================================
// TYPES
// =========================================================

interface Coupon {
  id: string
  code: string
  description: string
  discount: number
  type: 'percentage'
  startDate: string
  expiryDate: string
  status: 'Active' | 'Inactive' | 'Expired'
}

interface CouponDTO {
  couponId: string
  code: string
  description?: string
  discPercent: number
  startDate: string
  endDate: string
}

// =========================================================
// MAP BACKEND DTO
// =========================================================

const mapDtoToCoupon = (
  dto: CouponDTO
): Coupon => ({
  id: dto.couponId,
  code: dto.code,
  description: dto.description || '',
  discount: Number(
    dto.discPercent ?? 0
  ),
  type: 'percentage',
  startDate: dto.startDate,
  expiryDate: dto.endDate,
  status:
    dto.endDate &&
      new Date(dto.endDate) < new Date()
      ? 'Expired'
      : 'Active'
})

// =========================================================
// ERROR MESSAGE
// =========================================================

const extractErrorMessage = (
  error: any,
  fallback: string
) => {
  return (
    error?.response?.data?.message ||
    error?.response?.data?.error ||
    fallback
  )
}

// =========================================================
// STATE
// =========================================================

const coupons = ref<Coupon[]>([])

const searchQuery = ref('')
const statusFilter = ref('All')

const isLoading = ref(false)

const showModal = ref(false)
const isEditing = ref(false)

const editingCouponId =
  ref<string | null>(null)

// =========================================================
// FORM
// =========================================================

const couponForm = ref({
  code: '',
  description: '',
  discount: 10,
  startDate: '',
  expiryDate: '',
  status: 'Active' as 'Active' | 'Inactive'
})

// =========================================================
// FILTERED COUPONS
// =========================================================

const filteredCoupons = computed(() => {
  return coupons.value.filter(
    (coupon) => {

      const search =
        searchQuery.value
          .toLowerCase()
          .trim()

      const matchesSearch =
        coupon.code
          .toLowerCase()
          .includes(search) ||
        coupon.description
          .toLowerCase()
          .includes(search)

      const matchesStatus =
        statusFilter.value === 'All' ||
        coupon.status ===
        statusFilter.value

      return (
        matchesSearch &&
        matchesStatus
      )
    }
  )
})

// =========================================================
// STATISTICS
// =========================================================

const totalCoupons = computed(() => {
  return coupons.value.length
})

const activeCoupons = computed(() => {
  return coupons.value.filter(
    (coupon) =>
      coupon.status === 'Active'
  ).length
})

const inactiveCoupons =
  computed(() => {
    return coupons.value.filter(
      (coupon) =>
        coupon.status === 'Inactive'
    ).length
  })

const expiredCoupons =
  computed(() => {
    return coupons.value.filter(
      (coupon) =>
        coupon.status === 'Expired'
    ).length
  })

// =========================================================
// ADD MODAL
// =========================================================

const openAddModal = () => {

  isEditing.value = false

  editingCouponId.value = null

  couponForm.value = {
    code: '',
    description: '',
    discount: 10,
    startDate: '',
    expiryDate: '',
    status: 'Active'
  }

  showModal.value = true
}

// =========================================================
// EDIT MODAL
// =========================================================

const openEditModal = (
  coupon: Coupon
) => {

  isEditing.value = true

  editingCouponId.value =
    coupon.id

  couponForm.value = {
    code: coupon.code,
    description:
      coupon.description,
    discount: coupon.discount,
    startDate:
      coupon.startDate,
    expiryDate:
      coupon.expiryDate,
    status:
      coupon.status === 'Expired'
        ? 'Inactive'
        : coupon.status
  }

  showModal.value = true
}

// =========================================================
// CLOSE MODAL
// =========================================================

const closeModal = () => {
  showModal.value = false
}

// =========================================================
// CONFIRMATION & ALERT MODAL STATE (ORDER MANAGEMENT DESIGN)
// =========================================================

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

// =========================================================
// SAVE COUPON
// =========================================================

const saveCoupon = async () => {

  // =======================================================
  // CODE
  // =======================================================

  if (
    !couponForm.value.code.trim()
  ) {
    showAlert(
      'Validation Error',
      'Please enter a coupon code.',
      'warning'
    )
    return
  }

  // =======================================================
  // DESCRIPTION
  // =======================================================

  if (
    !couponForm.value.description.trim()
  ) {
    showAlert(
      'Validation Error',
      'Please enter a coupon description.',
      'warning'
    )
    return
  }

  // =======================================================
  // DISCOUNT
  // =======================================================

  const discount =
    Number(
      couponForm.value.discount
    )

  if (
    !Number.isFinite(discount) ||
    discount <= 0 ||
    discount > 100
  ) {
    showAlert(
      'Validation Error',
      'Discount must be between 1% and 100%.',
      'warning'
    )
    return
  }

  // =======================================================
  // DATES
  // =======================================================

  if (
    !couponForm.value.startDate ||
    !couponForm.value.expiryDate
  ) {
    showAlert(
      'Validation Error',
      'Please select start and expiry dates.',
      'warning'
    )
    return
  }

  // =======================================================
  // DATE ORDER
  // =======================================================

  if (
    couponForm.value.startDate >
    couponForm.value.expiryDate
  ) {
    showAlert(
      'Validation Error',
      'Expiry date cannot be before the start date.',
      'warning'
    )
    return
  }

  // =======================================================
  // BACKEND PAYLOAD
  // =======================================================

  const payload = {
    code:
      couponForm.value.code
        .trim()
        .toUpperCase(),
    
    description: couponForm.value.description.trim(),

    discPercent:
      discount,

    startDate:
      couponForm.value.startDate,

    endDate:
      couponForm.value.expiryDate
  }

  try {

    // =====================================================
    // UPDATE
    // =====================================================

    if (
      isEditing.value &&
      editingCouponId.value !== null
    ) {

      const response =
        await axios.put<CouponDTO>(
          `/api/coupons/${editingCouponId.value}`,
          payload
        )

      const updatedCoupon =
        mapDtoToCoupon(
          response.data
        )

      // Preserve description/status locally
      updatedCoupon.description =
        couponForm.value.description

      updatedCoupon.status =
        couponForm.value.status

      const index =
        coupons.value.findIndex(
          (coupon) =>
            coupon.id ===
            editingCouponId.value
        )

      if (index !== -1) {
        coupons.value[index] =
          updatedCoupon
      }

    } else {

      // ===================================================
      // CREATE
      // ===================================================

      const response =
        await axios.post<CouponDTO>(
          '/api/coupons',
          payload
        )

      const newCoupon =
        mapDtoToCoupon(
          response.data
        )

      newCoupon.description =
        couponForm.value.description

      newCoupon.status =
        couponForm.value.status

      coupons.value.unshift(
        newCoupon
      )
    }

    closeModal()

  } catch (error) {

    console.error(
      'Error saving coupon:',
      error
    )

    showAlert(
      'Save Failed',
      extractErrorMessage(
        error,
        'Failed to save coupon.'
      ),
      'danger'
    )
  }
}

// =========================================================
// DELETE COUPON
// =========================================================

const deleteCoupon = (
  coupon: Coupon
) => {
  confirmModal.value = {
    show: true,
    type: 'danger',
    title: 'Delete Coupon?',
    message: 'Are you sure you want to permanently delete coupon',
    target: coupon.code,
    description: 'This coupon will be deactivated and removed from the active system.',
    confirmText: 'Delete Coupon',
    cancelText: 'Cancel',
    loading: false,
    onConfirm: async () => {
      confirmModal.value.loading = true
      try {
        await axios.delete(`/api/coupons/${coupon.id}`)
        coupons.value = coupons.value.filter(item => item.id !== coupon.id)
        confirmModal.value.show = false
      } catch (error) {
        console.error('Error deleting coupon:', error)
        confirmModal.value = {
          show: true,
          type: 'danger',
          title: 'Delete Failed',
          message: extractErrorMessage(error, 'Failed to delete coupon.'),
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

// =========================================================
// TOGGLE STATUS
// =========================================================

const toggleCouponStatus = (
  coupon: Coupon
) => {

  if (
    coupon.status === 'Expired'
  ) {
    return
  }

  coupon.status =
    coupon.status === 'Active'
      ? 'Inactive'
      : 'Active'
}

// =========================================================
// STATUS CLASS
// =========================================================

const getStatusClass = (
  status: Coupon['status']
) => {

  if (status === 'Active') {
    return (
      'bg-emerald-50 ' +
      'text-emerald-700 ' +
      'border-emerald-200'
    )
  }

  if (status === 'Inactive') {
    return (
      'bg-slate-100 ' +
      'text-slate-600 ' +
      'border-slate-200'
    )
  }

  return (
    'bg-red-50 ' +
    'text-red-600 ' +
    'border-red-200'
  )
}

// =========================================================
// DISCOUNT
// =========================================================

const formatDiscount = (
  coupon: Coupon
) => {
  return `${coupon.discount}%`
}

// =========================================================
// FORMAT DATE
// =========================================================

const formatDate = (
  date: string
) => {

  if (!date) {
    return '-'
  }

  return new Date(
    date
  ).toLocaleDateString(
    'en-US',
    {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    }
  )
}

// =========================================================
// LOAD COUPONS
// =========================================================

const loadCoupons =
  async () => {

    isLoading.value = true

    try {

      const response =
        await axios.get<CouponDTO[]>(
          '/api/coupons'
        )

      if (
        Array.isArray(
          response.data
        )
      ) {

        coupons.value =
          response.data.map(
            mapDtoToCoupon
          ).reverse()
      }

    } catch (error) {

      console.error(
        'Failed to load coupons:',
        error
      )

    } finally {

      isLoading.value = false
    }
  }

// =========================================================
// ON MOUNT
// =========================================================

onMounted(() => {
  loadCoupons()
})
</script>

<template>

  <div class="min-h-screen
           bg-slate-100
           text-slate-900">

    <!-- =================================================
         SIDEBAR
    ================================================== -->

    <AdminSidebar />

    <!-- =================================================
         MAIN
    ================================================== -->

    <main class="ml-64
             min-h-screen">

      <!-- BACKGROUND -->

      <div class="pointer-events-none
               fixed
               inset-0
               overflow-hidden">

        <div class="absolute
                 -top-40
                 -right-40
                 h-96
                 w-96
                 rounded-full
                 bg-blue-200/30
                 blur-3xl"></div>

        <div class="absolute
                 top-1/2
                 -left-40
                 h-96
                 w-96
                 rounded-full
                 bg-cyan-200/20
                 blur-3xl"></div>

      </div>


      <div class="relative
               z-10
               p-6
               lg:p-8">

        <!-- =================================================
             HEADER
        ================================================== -->

        <div class="mb-8
                 flex flex-col
                 gap-5
                 lg:flex-row
                 lg:items-center
                 lg:justify-between">

          <div>

            <div class="mb-2
                     flex items-center
                     gap-2">

              <span class="h-2
                       w-2
                       rounded-full
                       bg-emerald-500
                       shadow-lg
                       shadow-emerald-500/40"></span>

              <span class="text-xs
                       font-bold
                       uppercase
                       tracking-[0.18em]
                       text-emerald-600">
                Promotion Center
              </span>

            </div>


            <h1 class="text-3xl
                     font-black
                     tracking-tight
                     text-slate-950
                     sm:text-4xl">
              Coupons
            </h1>


            <p class="mt-2
                     max-w-2xl
                     text-sm
                     text-slate-500">
              Create, manage and monitor
              promotional discount codes.
            </p>

          </div>


          <!-- CREATE -->

          <button type="button" @click="openAddModal" class="group
                   inline-flex
                   items-center
                   justify-center
                   gap-2
                   rounded-2xl
                   bg-gradient-to-r
                   from-blue-600
                   to-cyan-500
                   px-5
                   py-3.5
                   text-sm
                   font-bold
                   text-white
                   shadow-lg
                   shadow-blue-500/20
                   transition-all
                   duration-300
                   hover:-translate-y-0.5">

            <svg class="h-5 w-5
                     transition-transform
                     group-hover:rotate-90" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16M4 12h16" />
            </svg>

            Create Coupon

          </button>

        </div>


        <!-- =================================================
             STATISTICS
        ================================================== -->

        <div class="mb-8
                 grid
                 grid-cols-1
                 gap-4
                 sm:grid-cols-2
                 xl:grid-cols-4">

          <!-- TOTAL -->

          <div class="rounded-2xl
                   border border-white/90
                   bg-white/70
                   p-5
                   shadow-lg
                   shadow-slate-300/10
                   backdrop-blur-2xl">

            <div class="flex
                     items-center
                     justify-between">

              <div class="flex
                       h-11
                       w-11
                       items-center
                       justify-center
                       rounded-xl
                       bg-blue-50
                       text-blue-600">

                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M9 14.25l6-6m2.25-3.75h.008v.008H17.25V4.5zM6.75 19.5h.008v.008H6.75V19.5zM21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>

              </div>


              <span class="rounded-full
                       bg-blue-50
                       px-2.5
                       py-1
                       text-xs
                       font-bold
                       text-blue-600">
                ALL
              </span>

            </div>


            <p class="mt-5
                     text-2xl
                     font-black
                     text-slate-950">
              {{ totalCoupons }}
            </p>


            <p class="mt-1
                     text-sm
                     text-slate-500">
              Total Coupons
            </p>

          </div>


          <!-- ACTIVE -->

          <div class="rounded-2xl
                   border border-white/90
                   bg-white/70
                   p-5
                   shadow-lg
                   shadow-slate-300/10
                   backdrop-blur-2xl">

            <div class="flex
                     items-center
                     justify-between">

              <div class="flex
                       h-11
                       w-11
                       items-center
                       justify-center
                       rounded-xl
                       bg-emerald-50
                       text-emerald-600">

                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>

              </div>


              <span class="rounded-full
                       bg-emerald-50
                       px-2.5
                       py-1
                       text-xs
                       font-bold
                       text-emerald-600">
                LIVE
              </span>

            </div>


            <p class="mt-5
                     text-2xl
                     font-black
                     text-slate-950">
              {{ activeCoupons }}
            </p>


            <p class="mt-1
                     text-sm
                     text-slate-500">
              Active Coupons
            </p>

          </div>


          <!-- INACTIVE -->

          <div class="rounded-2xl
                   border border-white/90
                   bg-white/70
                   p-5
                   shadow-lg
                   shadow-slate-300/10
                   backdrop-blur-2xl">

            <div class="flex
                     h-11
                     w-11
                     items-center
                     justify-center
                     rounded-xl
                     bg-slate-100
                     text-slate-600">

              <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
              </svg>

            </div>


            <p class="mt-5
                     text-2xl
                     font-black
                     text-slate-950">
              {{ inactiveCoupons }}
            </p>


            <p class="mt-1
                     text-sm
                     text-slate-500">
              Inactive Coupons
            </p>

          </div>


          <!-- EXPIRED -->

          <div class="rounded-2xl
                   border border-white/90
                   bg-white/70
                   p-5
                   shadow-lg
                   shadow-slate-300/10
                   backdrop-blur-2xl">

            <div class="flex
                     h-11
                     w-11
                     items-center
                     justify-center
                     rounded-xl
                     bg-red-50
                     text-red-600">

              <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>

            </div>


            <p class="mt-5
                     text-2xl
                     font-black
                     text-slate-950">
              {{ expiredCoupons }}
            </p>


            <p class="mt-1
                     text-sm
                     text-slate-500">
              Expired Coupons
            </p>

          </div>

        </div>


        <!-- =================================================
             FILTERS
        ================================================== -->

        <div class="mb-6
                 rounded-2xl
                 border border-white/90
                 bg-white/70
                 p-4
                 shadow-lg
                 shadow-slate-300/10
                 backdrop-blur-2xl">

          <div class="flex
                   flex-col
                   gap-3
                   xl:flex-row
                   xl:items-center">

            <!-- SEARCH -->

            <div class="relative
                     flex-1">

              <svg class="absolute
                       left-4
                       top-1/2
                       h-5
                       w-5
                       -translate-y-1/2
                       text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M21 21l-4.35-4.35m2.1-5.4a7.5 7.5 0 11-15 0 7.5 7.5 0 0115 0z" />
              </svg>


              <input v-model="searchQuery" type="text" placeholder="Search coupon code or description..." class="w-full
                       rounded-xl
                       border border-slate-200/80
                       bg-white/80
                       py-3
                       pl-11
                       pr-4
                       text-sm
                       text-slate-900
                       outline-none
                       transition
                       focus:border-blue-400
                       focus:ring-4
                       focus:ring-blue-500/10" />

            </div>


            <!-- STATUS -->

            <select v-model="statusFilter" class="rounded-xl
                     border border-slate-200/80
                     bg-white/80
                     px-4
                     py-3
                     text-sm
                     font-medium
                     text-slate-700
                     outline-none
                     transition
                     focus:border-blue-400
                     focus:ring-4
                     focus:ring-blue-500/10">

              <option value="All">
                All Status
              </option>

              <option value="Active">
                Active
              </option>

              <option value="Inactive">
                Inactive
              </option>

              <option value="Expired">
                Expired
              </option>

            </select>

          </div>

        </div>


        <!-- =================================================
             COUPON MANAGEMENT
        ================================================== -->

        <div class="overflow-hidden
                 rounded-2xl
                 border border-white/90
                 bg-white/70
                 shadow-xl
                 shadow-slate-300/10
                 backdrop-blur-2xl">

          <!-- HEADER -->

          <div class="flex
                   flex-col
                   gap-2
                   border-b
                   border-slate-200/70
                   px-6
                   py-5
                   sm:flex-row
                   sm:items-center
                   sm:justify-between">

            <div>

              <h2 class="text-lg
                       font-black
                       text-slate-950">
                Coupon Management
              </h2>


              <p class="mt-1
                       text-sm
                       text-slate-500">
                {{
                  filteredCoupons.length
                }}
                coupon{{
                  filteredCoupons.length === 1
                    ? ''
                    : 's'
                }}
                displayed
              </p>

            </div>


            <!-- REFRESH -->

            <button type="button" @click="loadCoupons" :disabled="isLoading" class="inline-flex
                     items-center
                     justify-center
                     gap-2
                     rounded-xl
                     border
                     border-slate-200
                     bg-white/80
                     px-4
                     py-2.5
                     text-sm
                     font-bold
                     text-slate-700
                     transition
                     hover:border-blue-200
                     hover:bg-blue-50
                     hover:text-blue-600
                     disabled:opacity-50">

              <svg class="h-4 w-4" :class="{
                'animate-spin':
                  isLoading
              }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M4 4v5h5M20 20v-5h-5M5.05 9A7 7 0 0117.95 7M18.95 15A7 7 0 016.05 17" />
              </svg>

              Refresh

            </button>

          </div>


          <!-- =================================================
               DESKTOP TABLE
          ================================================== -->

          <div class="hidden
                   overflow-x-auto
                   lg:block">

            <table class="w-full">

              <thead>

                <tr class="border-b
                         border-slate-200/70
                         bg-slate-50/60
                         text-left">

                  <th class="px-6
                           py-4
                           text-xs
                           font-black
                           uppercase
                           tracking-wider
                           text-slate-500">
                    Coupon
                  </th>


                  <th class="px-6
                           py-4
                           text-xs
                           font-black
                           uppercase
                           tracking-wider
                           text-slate-500">
                    Discount
                  </th>


                  <th class="px-6
                           py-4
                           text-xs
                           font-black
                           uppercase
                           tracking-wider
                           text-slate-500">
                    Validity
                  </th>


                  <th class="px-6
                           py-4
                           text-xs
                           font-black
                           uppercase
                           tracking-wider
                           text-slate-500">
                    Status
                  </th>


                  <th class="px-6
                           py-4
                           text-right
                           text-xs
                           font-black
                           uppercase
                           tracking-wider
                           text-slate-500">
                    Actions
                  </th>

                </tr>

              </thead>


              <tbody class="divide-y
                       divide-slate-200/60">

                <tr v-for="coupon in filteredCoupons" :key="coupon.id" class="group
                         transition
                         hover:bg-blue-50/30">

                  <!-- COUPON -->

                  <td class="px-6 py-5">

                    <div class="flex
                             items-center
                             gap-4">

                      <div class="flex
                               h-11
                               w-11
                               shrink-0
                               items-center
                               justify-center
                               rounded-xl
                               bg-gradient-to-br
                               from-blue-50
                               to-cyan-50
                               text-blue-600">

                        <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M9 14.25l6-6m2.25-3.75h.008v.008H17.25V4.5zM6.75 19.5h.008v.008H6.75V19.5zM21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>

                      </div>


                      <div class="min-w-0">

                        <div class="font-black
                                 tracking-wide
                                 text-slate-950">
                          {{ coupon.code }}
                        </div>


                        <div class="mt-1
                                 max-w-xs
                                 truncate
                                 text-xs
                                 text-slate-500">
                          {{
                            coupon.description ||
                            'Promotional discount'
                          }}
                        </div>

                      </div>

                    </div>

                  </td>


                  <!-- DISCOUNT -->

                  <td class="px-6 py-5">

                    <span class="rounded-lg
                             border
                             border-blue-200
                             bg-blue-50
                             px-2.5
                             py-1
                             text-sm
                             font-black
                             text-blue-700">
                      {{ formatDiscount(coupon) }}
                    </span>

                  </td>


                  <!-- VALIDITY -->

                  <td class="px-6 py-5">

                    <div class="text-xs">

                      <p class="font-bold
                               text-slate-700">
                        {{
                          formatDate(
                            coupon.startDate
                          )
                        }}
                      </p>


                      <p class="mt-1
                               text-slate-400">
                        →
                        {{
                          formatDate(
                            coupon.expiryDate
                          )
                        }}
                      </p>

                    </div>

                  </td>


                  <!-- STATUS -->

                  <td class="px-6 py-5">

                    <button type="button" @click="
                      toggleCouponStatus(
                        coupon
                      )
                      " :disabled="coupon.status ===
                        'Expired'
                        " :class="[
                        'rounded-full border px-3 py-1.5 text-xs font-bold transition',
                        getStatusClass(
                          coupon.status
                        ),

                        coupon.status !==
                          'Expired'
                          ? 'hover:scale-105'
                          : 'cursor-not-allowed'
                      ]">
                      {{ coupon.status }}
                    </button>

                  </td>


                  <!-- ACTIONS -->

                  <td class="px-6 py-5">

                    <div class="flex
                             justify-end
                             gap-2">

                      <!-- EDIT -->

                      <button type="button" @click="
                        openEditModal(
                          coupon
                        )
                        " class="rounded-xl
                               border
                               border-slate-200
                               bg-white
                               p-2.5
                               text-slate-500
                               transition
                               hover:border-blue-200
                               hover:bg-blue-50
                               hover:text-blue-600" title="Edit coupon">

                        <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M16.862 4.487l1.687-1.688a2.25 2.25 0 113.182 3.182l-9.193 9.193a4.5 4.5 0 01-1.897 1.13l-3.293.94.94-3.293a4.5 4.5 0 011.13-1.897l9.193-9.193z" />
                        </svg>

                      </button>


                      <!-- DELETE -->

                      <button type="button" @click="
                        deleteCoupon(
                          coupon
                        )
                        " class="rounded-xl
                               border
                               border-slate-200
                               bg-white
                               p-2.5
                               text-slate-500
                               transition
                               hover:border-red-200
                               hover:bg-red-50
                               hover:text-red-600" title="Delete coupon">

                        <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6M9 7V4a1 1 0 011-1h4a1 1 0 011 1v3m-8 0h10" />
                        </svg>

                      </button>

                    </div>

                  </td>

                </tr>

              </tbody>

            </table>

          </div>


          <!-- =================================================
               DESKTOP EMPTY
          ================================================== -->

          <div v-if="
            filteredCoupons.length === 0
          " class="hidden
                   px-6
                   py-16
                   text-center
                   lg:block">

            <div class="mx-auto
                     flex
                     h-16
                     w-16
                     items-center
                     justify-center
                     rounded-2xl
                     bg-slate-100
                     text-slate-400">

              <svg class="h-7 w-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M9 14.25l6-6m2.25-3.75h.008v.008H17.25V4.5zM6.75 19.5h.008v.008H6.75V19.5zM21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>

            </div>


            <h3 class="mt-4
                     font-bold
                     text-slate-900">
              No coupons found
            </h3>


            <p class="mt-1
                     text-sm
                     text-slate-500">
              Try changing your search
              or filter options.
            </p>

          </div>


          <!-- =================================================
               MOBILE
          ================================================== -->

          <div class="space-y-4
                   p-4
                   lg:hidden">

            <div v-for="coupon in filteredCoupons" :key="coupon.id" class="rounded-2xl
                     border
                     border-slate-200/80
                     bg-white/80
                     p-4
                     shadow-sm">

              <!-- TOP -->

              <div class="flex
                       items-start
                       justify-between
                       gap-3">

                <div class="flex
                         items-center
                         gap-3">

                  <div class="flex
                           h-10
                           w-10
                           shrink-0
                           items-center
                           justify-center
                           rounded-xl
                           bg-blue-50
                           text-blue-600">

                    <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M9 14.25l6-6m2.25-3.75h.008v.008H17.25V4.5zM6.75 19.5h.008v.008H6.75V19.5zM21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>

                  </div>


                  <div>

                    <h3 class="font-black
                             tracking-wide
                             text-slate-950">
                      {{ coupon.code }}
                    </h3>


                    <p class="mt-0.5
                             text-xs
                             text-slate-500">
                      {{
                        coupon.description ||
                        'Promotional discount'
                      }}
                    </p>

                  </div>

                </div>


                <span :class="[
                  'rounded-full border px-2.5 py-1 text-[10px] font-bold',
                  getStatusClass(
                    coupon.status
                  )
                ]">
                  {{ coupon.status }}
                </span>

              </div>


              <!-- INFO -->

              <div class="mt-4
                       grid
                       grid-cols-2
                       gap-3
                       rounded-xl
                       bg-slate-50/80
                       p-3">

                <div>

                  <p class="text-[10px]
                           font-bold
                           uppercase
                           tracking-wider
                           text-slate-400">
                    Discount
                  </p>

                  <p class="mt-1
                           font-black
                           text-blue-600">
                    {{ formatDiscount(coupon) }}
                  </p>

                </div>


                <div>

                  <p class="text-[10px]
                           font-bold
                           uppercase
                           tracking-wider
                           text-slate-400">
                    Start Date
                  </p>

                  <p class="mt-1
                           font-bold
                           text-slate-700">
                    {{
                      formatDate(
                        coupon.startDate
                      )
                    }}
                  </p>

                </div>


                <div class="col-span-2">

                  <p class="text-[10px]
                           font-bold
                           uppercase
                           tracking-wider
                           text-slate-400">
                    Expires
                  </p>

                  <p class="mt-1
                           font-bold
                           text-slate-700">
                    {{
                      formatDate(
                        coupon.expiryDate
                      )
                    }}
                  </p>

                </div>

              </div>


              <!-- ACTIONS -->

              <div class="mt-4
                       flex
                       gap-2">

                <button type="button" @click="
                  openEditModal(
                    coupon
                  )
                  " class="flex
                         flex-1
                         items-center
                         justify-center
                         gap-2
                         rounded-xl
                         border
                         border-blue-200
                         bg-blue-50
                         py-2.5
                         text-sm
                         font-bold
                         text-blue-600
                         transition
                         hover:bg-blue-100">

                  <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M16.862 4.487l1.687-1.688a2.25 2.25 0 113.182 3.182l-9.193 9.193a4.5 4.5 0 01-1.897 1.13l-3.293.94.94-3.293a4.5 4.5 0 011.13-1.897l9.193-9.193z" />
                  </svg>

                  Edit

                </button>


                <button type="button" @click="
                  deleteCoupon(
                    coupon
                  )
                  " class="flex
                         items-center
                         justify-center
                         rounded-xl
                         border
                         border-red-200
                         bg-red-50
                         px-4
                         py-2.5
                         text-red-600
                         transition
                         hover:bg-red-100">

                  <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6M9 7V4a1 1 0 011-1h4a1 1 0 011 1v3m-8 0h10" />
                  </svg>

                </button>

              </div>

            </div>


            <!-- MOBILE EMPTY -->

            <div v-if="
              filteredCoupons.length === 0
            " class="py-12
                     text-center">

              <div class="mx-auto
                       flex
                       h-14
                       w-14
                       items-center
                       justify-center
                       rounded-2xl
                       bg-slate-100
                       text-slate-400">

                <svg class="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M9 14.25l6-6m2.25-3.75h.008v.008H17.25V4.5zM6.75 19.5h.008v.008H6.75V19.5zM21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>

              </div>


              <p class="mt-4
                       font-bold
                       text-slate-800">
                No coupons found
              </p>

              <p class="mt-1
                       text-sm
                       text-slate-500">
                Try another search
                or filter.
              </p>

            </div>

          </div>

        </div>

      </div>

    </main>


    <!-- =====================================================
         ADD / EDIT MODAL
    ====================================================== -->

    <Transition name="modal">

      <div v-if="showModal" class="fixed
               inset-0
               z-50
               flex
               items-center
               justify-center
               bg-slate-950/30
               p-4
               backdrop-blur-sm" @click.self="closeModal">

        <div class="max-h-[92vh]
                 w-full
                 max-w-2xl
                 overflow-y-auto
                 rounded-3xl
                 border border-white/90
                 bg-white/90
                 p-6
                 shadow-2xl
                 shadow-slate-900/20
                 backdrop-blur-2xl
                 sm:p-8">

          <!-- MODAL HEADER -->

          <div class="mb-6
                   flex
                   items-start
                   justify-between">

            <div>

              <div class="mb-2
                       flex
                       h-10
                       w-10
                       items-center
                       justify-center
                       rounded-xl
                       bg-blue-50
                       text-blue-600">

                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M9 14.25l6-6m2.25-3.75h.008v.008H17.25V4.5zM6.75 19.5h.008v.008H6.75V19.5zM21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>

              </div>


              <h2 class="text-2xl
                       font-black
                       text-slate-950">
                {{
                  isEditing
                    ? 'Edit Coupon'
                    : 'Create Coupon'
                }}
              </h2>


              <p class="mt-1
                       text-sm
                       text-slate-500">
                {{
                  isEditing
                    ? 'Update the coupon promotion details.'
                    : 'Create a new promotional discount code.'
                }}
              </p>

            </div>


            <button type="button" @click="closeModal" class="rounded-xl
                     p-2
                     text-slate-400
                     transition
                     hover:bg-slate-100
                     hover:text-slate-700">

              <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>

            </button>

          </div>


          <!-- FORM -->

          <form @submit.prevent="saveCoupon" class="space-y-5">

            <!-- CODE -->

            <div>

              <label class="mb-2
                       block
                       text-sm
                       font-bold
                       text-slate-700">
                Coupon Code
              </label>

              <input v-model="couponForm.code" type="text" placeholder="e.g. WELCOME10" maxlength="30" class="w-full
                       rounded-xl
                       border
                       border-slate-200
                       bg-white
                       px-4
                       py-3
                       text-sm
                       font-bold
                       uppercase
                       text-slate-900
                       outline-none
                       transition
                       focus:border-blue-400
                       focus:ring-4
                       focus:ring-blue-500/10" />

            </div>


            <!-- DESCRIPTION -->

            <div>

              <label class="mb-2
                       block
                       text-sm
                       font-bold
                       text-slate-700">
                Description
              </label>

              <textarea v-model="couponForm.description
                " rows="3" placeholder="Describe this promotion..." class="w-full
                       resize-none
                       rounded-xl
                       border
                       border-slate-200
                       bg-white
                       px-4
                       py-3
                       text-sm
                       text-slate-900
                       outline-none
                       transition
                       focus:border-blue-400
                       focus:ring-4
                       focus:ring-blue-500/10"></textarea>

            </div>


            <!-- DISCOUNT -->

            <div>

              <label class="mb-2
                       block
                       text-sm
                       font-bold
                       text-slate-700">
                Discount Percentage
              </label>


              <div class="relative">

                <input v-model.number="couponForm.discount
                  " type="number" min="1" max="100" class="w-full
                         rounded-xl
                         border
                         border-slate-200
                         bg-white
                         px-4
                         py-3
                         pr-12
                         text-sm
                         font-bold
                         text-slate-900
                         outline-none
                         transition
                         focus:border-blue-400
                         focus:ring-4
                         focus:ring-blue-500/10" />


                <span class="absolute
                         right-4
                         top-1/2
                         -translate-y-1/2
                         text-sm
                         font-bold
                         text-slate-400">
                  %
                </span>

              </div>

            </div>


            <!-- DATES -->

            <div class="grid
                     grid-cols-1
                     gap-4
                     sm:grid-cols-2">

              <!-- START -->

              <div>

                <label class="mb-2
                         block
                         text-sm
                         font-bold
                         text-slate-700">
                  Start Date
                </label>

                <input v-model="couponForm.startDate
                  " type="date" class="w-full
                         rounded-xl
                         border
                         border-slate-200
                         bg-white
                         px-4
                         py-3
                         text-sm
                         font-medium
                         text-slate-700
                         outline-none
                         transition
                         focus:border-blue-400
                         focus:ring-4
                         focus:ring-blue-500/10" />

              </div>


              <!-- EXPIRY -->

              <div>

                <label class="mb-2
                         block
                         text-sm
                         font-bold
                         text-slate-700">
                  Expiry Date
                </label>

                <input v-model="couponForm.expiryDate
                  " type="date" class="w-full
                         rounded-xl
                         border
                         border-slate-200
                         bg-white
                         px-4
                         py-3
                         text-sm
                         font-medium
                         text-slate-700
                         outline-none
                         transition
                         focus:border-blue-400
                         focus:ring-4
                         focus:ring-blue-500/10" />

              </div>

            </div>


            <!-- STATUS -->

            <div>

              <label class="mb-2
                       block
                       text-sm
                       font-bold
                       text-slate-700">
                Status
              </label>


              <div class="grid
                       grid-cols-2
                       gap-3">

                <button type="button" @click="
                  couponForm.status =
                  'Active'
                  " :class="[
                    'rounded-xl border px-4 py-3 text-sm font-bold transition',

                    couponForm.status ===
                      'Active'
                      ? 'border-emerald-300 bg-emerald-50 text-emerald-700'
                      : 'border-slate-200 bg-white text-slate-500 hover:bg-slate-50'
                  ]">
                  Active
                </button>


                <button type="button" @click="
                  couponForm.status =
                  'Inactive'
                  " :class="[
                    'rounded-xl border px-4 py-3 text-sm font-bold transition',

                    couponForm.status ===
                      'Inactive'
                      ? 'border-slate-300 bg-slate-100 text-slate-700'
                      : 'border-slate-200 bg-white text-slate-500 hover:bg-slate-50'
                  ]">
                  Inactive
                </button>

              </div>

            </div>


            <!-- BUTTONS -->

            <div class="flex
                     flex-col-reverse
                     gap-3
                     border-t
                     border-slate-200/70
                     pt-6
                     sm:flex-row
                     sm:justify-end">

              <button type="button" @click="closeModal" class="rounded-xl
                       border
                       border-slate-200
                       bg-white
                       px-5
                       py-3
                       text-sm
                       font-bold
                       text-slate-600
                       transition
                       hover:bg-slate-50">
                Cancel
              </button>


              <button type="submit" class="rounded-xl
                       bg-gradient-to-r
                       from-blue-600
                       to-cyan-500
                       px-6
                       py-3
                       text-sm
                       font-bold
                       text-white
                       shadow-lg
                       shadow-blue-500/20
                       transition
                       hover:-translate-y-0.5">
                {{
                  isEditing
                    ? 'Update Coupon'
                    : 'Create Coupon'
                }}
              </button>

            </div>

          </form>

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
  transition:
    opacity 0.25s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
</style>