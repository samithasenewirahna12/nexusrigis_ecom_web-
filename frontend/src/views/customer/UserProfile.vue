<template>
  <div class="relative min-h-screen bg-slate-50">
    <!-- Background: ONE static layer (image + gradients + glows) -->
    <div class="page-bg" :style="{ '--bg-img': `url(${backgroundImage})` }"></div>

    <!-- ========================================================= -->
    <!-- MAIN CONTAINER -->
    <!-- ========================================================= -->

    <div class="relative z-10 mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <!-- ======================================================= -->
      <!-- HEADER -->
      <!-- ======================================================= -->

      <div
        class="relative z-30 mb-6 overflow-visible rounded-3xl border border-white/70 bg-white/80 p-5 shadow-md shadow-slate-200/50 lg:p-6">
        <div class="flex flex-col gap-5 xl:flex-row xl:items-center xl:justify-between">
          <!-- HEADER LEFT -->

          <div class="flex min-w-0 items-center gap-4">
            <div class="min-w-0">
              <p class="text-xs font-bold uppercase tracking-[0.22em] text-blue-600">
                Account Settings
              </p>

              <h1 class="mt-1 truncate text-xl font-black text-slate-900 sm:text-2xl">
                Account Details
              </h1>

              <p class="mt-1 hidden text-sm text-slate-500 sm:block">
                Manage your profile, payment methods and security settings.
              </p>
            </div>
          </div>

          <!-- HEADER RIGHT -->

          <div class="flex w-full flex-wrap items-center justify-start gap-2 xl:w-auto xl:justify-end">
            <!-- PROFESSIONAL QUICK ACCESS -->

            <nav
              class="flex flex-wrap items-center gap-1 rounded-2xl border border-slate-200/80 bg-white/80 p-1.5 shadow-sm shadow-slate-200/40">
              <!-- My Orders -->

              <router-link to="/orders" title="My Orders"
                class="group inline-flex h-10 items-center gap-2 rounded-xl px-3 text-sm font-semibold text-slate-600 transition-colors duration-200 hover:bg-blue-50 hover:text-blue-600 sm:px-3.5">
                <svg xmlns="http://www.w3.org/2000/svg"
                  class="h-[17px] w-[17px] transition-transform duration-200 group-hover:-translate-y-0.5" fill="none"
                  viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.7">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M3 7h18v13H3z" />
                  <path stroke-linecap="round" stroke-linejoin="round" d="M7 7V5a2 2 0 012-2h6a2 2 0 012 2v2" />
                </svg>

                <span>My Orders</span>
              </router-link>

              <!-- Track Order -->

              <router-link to="/orders/track" title="Track Order"
                class="group inline-flex h-10 items-center gap-2 rounded-xl px-3 text-sm font-semibold text-slate-600 transition-colors duration-200 hover:bg-cyan-50 hover:text-cyan-600 sm:px-3.5">
                <svg xmlns="http://www.w3.org/2000/svg"
                  class="h-[17px] w-[17px] transition-transform duration-200 group-hover:-translate-y-0.5" fill="none"
                  viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.7">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M3 7h11v10H3z" />
                  <path stroke-linecap="round" stroke-linejoin="round" d="M14 10h4l3 3v4h-7z" />
                  <circle cx="7" cy="19" r="2" />
                  <circle cx="18" cy="19" r="2" />
                </svg>

                <span>Track Order</span>
              </router-link>

              <!-- Returns -->

              <router-link to="/returns" title="Returns & Refunds"
                class="group inline-flex h-10 items-center gap-2 rounded-xl px-3 text-sm font-semibold text-slate-600 transition-colors duration-200 hover:bg-amber-50 hover:text-amber-600 sm:px-3.5">
                <svg xmlns="http://www.w3.org/2000/svg"
                  class="h-[17px] w-[17px] transition-transform duration-200 group-hover:-translate-y-0.5" fill="none"
                  viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.7">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 14l-3-3 3-3" />
                  <path stroke-linecap="round" stroke-linejoin="round" d="M6 11h7a5 5 0 015 5v1" />
                </svg>

                <span>Returns & Refunds</span>
              </router-link>

              <!-- Warranty -->

              <router-link to="/warranty" title="Warranty"
                class="group inline-flex h-10 items-center gap-2 rounded-xl px-3 text-sm font-semibold text-slate-600 transition-colors duration-200 hover:bg-emerald-50 hover:text-emerald-600 sm:px-3.5">
                <svg xmlns="http://www.w3.org/2000/svg"
                  class="h-[17px] w-[17px] transition-transform duration-200 group-hover:-translate-y-0.5" fill="none"
                  viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.7">
                  <path stroke-linecap="round" stroke-linejoin="round"
                    d="M12 3l7 3v5c0 5-3 8-7 10-4-2-7-5-7-10V6l7-3z" />
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4" />
                </svg>

                <span>Warranty</span>
              </router-link>

              <!-- Divider -->

              <div class="mx-1 hidden h-7 w-px bg-slate-200 sm:block"></div>

              <!-- Sign Out -->

              <button type="button" @click="handleLogout" title="Sign Out"
                class="group inline-flex h-10 items-center gap-2 rounded-xl px-3 text-sm font-semibold text-slate-500 transition-colors duration-200 hover:bg-red-50 hover:text-red-600 sm:px-3.5">
                <svg xmlns="http://www.w3.org/2000/svg"
                  class="h-[17px] w-[17px] transition-transform duration-200 group-hover:translate-x-0.5" fill="none"
                  viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.7">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M10 17l5-5-5-5" />
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15 12H3" />
                  <path stroke-linecap="round" stroke-linejoin="round" d="M21 19V5a2 2 0 00-2-2h-4" />
                </svg>

                <span>Sign Out</span>
              </button>

              <!-- Edit Profile -->

              <template v-if="activeTab === 'profile' && !isEditingProfile">
                <div class="mx-1 hidden h-7 w-px bg-slate-200 sm:block"></div>

                <button type="button" @click="startProfileEdit"
                  class="inline-flex h-10 items-center gap-2 rounded-xl bg-slate-900 px-3.5 text-sm font-semibold text-white transition-colors duration-200 hover:bg-blue-600">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                    stroke="currentColor" stroke-width="1.8">
                    <path stroke-linecap="round" stroke-linejoin="round"
                      d="M11 5h2M5 19l4-1 10-10a2.828 2.828 0 10-4-4L5 14v5z" />
                  </svg>

                  <span>Edit Profile</span>
                </button>
              </template>
            </nav>
          </div>
        </div>
      </div>

      <!-- ========================================================= -->
      <!-- MAIN GRID -->
      <!-- ========================================================= -->

      <div class="grid grid-cols-1 gap-6 lg:grid-cols-[260px_1fr]">
        <!-- ======================================================= -->
        <!-- SIDEBAR -->
        <!-- ======================================================= -->

        <aside class="space-y-6">
          <!-- USER CARD -->

          <div class="rounded-3xl border border-white/70 bg-white/80 p-5 shadow-md shadow-slate-200/50">
            <div class="flex flex-col items-center text-center">
              <!-- PROFILE IMAGE -->

              <div class="relative">
                <input id="profileImageInput" ref="profileImageInput" type="file"
                  accept="image/png,image/jpeg,image/jpg,image/webp" class="hidden"
                  @change="handleProfileImageChange" />

                <button type="button" @click="triggerProfileImageUpload" class="group relative block"
                  title="Change profile image">
                  <img :src="user.avatar" :alt="user.name || 'Profile Image'"
                    class="h-28 w-28 rounded-full border-4 border-white object-cover shadow-xl ring-4 ring-blue-100 transition duration-300 group-hover:scale-105" />

                  <span
                    class="absolute bottom-1 right-1 flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-r from-blue-600 to-cyan-500 text-white shadow-lg ring-4 ring-white transition group-hover:scale-110">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                      stroke="currentColor" stroke-width="2">
                      <path stroke-linecap="round" stroke-linejoin="round"
                        d="M3 7h3l2-3h8l2 3h3a1 1 0 011 1v11a1 1 0 01-1 1H3a1 1 0 01-1-1V8a1 1 0 011-1z" />
                      <circle cx="12" cy="13" r="3" />
                    </svg>
                  </span>
                </button>
              </div>

              <p class="mt-3 text-xs text-slate-400">
                Click image to change
              </p>

              <h2 class="mt-3 text-lg font-black text-slate-900">
                {{ user.name || 'User' }}
              </h2>

              <p class="mt-1 break-all text-sm text-slate-500">
                {{ user.email || 'No email available' }}
              </p>

              <span class="mt-3 rounded-full bg-blue-50 px-3 py-1 text-xs font-bold text-blue-600">
                {{ user.role }}
              </span>
            </div>
          </div>

          <!-- ACCOUNT NAVIGATION -->

          <div class="rounded-3xl border border-white/70 bg-white/80 p-3 shadow-md shadow-slate-200/50">
            <!-- Account Details -->

            <button type="button" @click="openProfileTab" :class="[
              'mb-1 flex w-full items-center gap-3 rounded-2xl px-4 py-3 text-left text-sm font-bold transition',
              activeTab === 'profile'
                ? 'bg-gradient-to-r from-blue-600 to-cyan-500 text-white shadow-lg shadow-blue-500/20'
                : 'text-slate-600 hover:bg-white/80 hover:text-slate-900'
            ]">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24"
                stroke="currentColor" stroke-width="1.8">
                <path stroke-linecap="round" stroke-linejoin="round" d="M15 19a4 4 0 00-6 0" />
                <circle cx="12" cy="8" r="4" />
              </svg>

              Account Details
            </button>

            <!-- Payment -->

            <button type="button" @click="openPaymentsTab" :class="[
              'mb-1 flex w-full items-center gap-3 rounded-2xl px-4 py-3 text-left text-sm font-bold transition',
              activeTab === 'payments'
                ? 'bg-gradient-to-r from-blue-600 to-cyan-500 text-white shadow-lg shadow-blue-500/20'
                : 'text-slate-600 hover:bg-white/80 hover:text-slate-900'
            ]">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24"
                stroke="currentColor" stroke-width="1.8">
                <rect x="3" y="5" width="18" height="14" rx="2" />
                <path stroke-linecap="round" stroke-linejoin="round" d="M3 10h18" />
              </svg>

              Payment Methods
            </button>

            <!-- Security -->

            <button type="button" @click="openSecurityTab" :class="[
              'flex w-full items-center gap-3 rounded-2xl px-4 py-3 text-left text-sm font-bold transition',
              activeTab === 'security'
                ? 'bg-gradient-to-r from-blue-600 to-cyan-500 text-white shadow-lg shadow-blue-500/20'
                : 'text-slate-600 hover:bg-white/80 hover:text-slate-900'
            ]">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24"
                stroke="currentColor" stroke-width="1.8">
                <rect x="4" y="10" width="16" height="10" rx="2" />
                <path stroke-linecap="round" stroke-linejoin="round" d="M8 10V7a4 4 0 118 0v3" />
              </svg>

              Security
            </button>
          </div>
        </aside>

        <!-- ======================================================= -->
        <!-- CONTENT -->
        <!-- ======================================================= -->

        <main>
          <!-- ===================================================== -->
          <!-- ACCOUNT DETAILS -->
          <!-- ===================================================== -->

          <section v-if="activeTab === 'profile'"
            class="rounded-3xl border border-white/70 bg-white/80 p-6 shadow-md shadow-slate-200/50">
            <!-- VIEW -->

            <div v-if="!isEditingProfile">
              <div
                class="flex flex-col gap-4 border-b border-slate-200/70 pb-5 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p class="text-xs font-black uppercase tracking-[0.18em] text-blue-600">
                    Personal Information
                  </p>

                  <h2 class="mt-1 text-2xl font-black text-slate-900">
                    Account Details
                  </h2>

                  <p class="mt-1 text-sm text-slate-500">
                    Your saved account information.
                  </p>
                </div>

                <button type="button" @click="startProfileEdit"
                  class="inline-flex items-center justify-center gap-2 rounded-xl border border-blue-200 bg-blue-50 px-4 py-2.5 text-sm font-bold text-blue-600 transition hover:bg-blue-100">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                    stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round"
                      d="M11 5h2M5 19l4-1 10-10a2.828 2.828 0 10-4-4L5 14v5z" />
                  </svg>

                  Edit Profile
                </button>
              </div>

              <!-- Profile Summary -->

              <div class="mt-6 rounded-2xl border border-blue-100 bg-gradient-to-r from-blue-50/80 to-cyan-50/80 p-5">
                <div class="flex items-center gap-4">
                  <div class="min-w-0">
                    <div class="flex flex-wrap items-center gap-2">
                      <h3 class="text-xl font-black text-slate-900">
                        {{ user.name || 'User' }}
                      </h3>

                      <span class="rounded-full bg-emerald-100 px-2.5 py-1 text-xs font-bold text-emerald-700">
                        Active Account
                      </span>
                    </div>

                    <p class="mt-1 text-sm text-slate-500">
                      {{ user.email || 'No email available' }}
                    </p>

                    <p v-if="user.joinedDate" class="mt-2 text-xs font-medium text-slate-400">
                      Member since
                      {{ formatDate(user.joinedDate) }}
                    </p>
                  </div>
                </div>
              </div>

              <!-- Details -->

              <div class="mt-6 grid grid-cols-1 gap-4 md:grid-cols-2">
                <div class="rounded-2xl border border-slate-200/80 bg-white/75 p-5">
                  <p class="text-xs font-bold uppercase tracking-wider text-slate-400">
                    Full Name
                  </p>

                  <p class="mt-2 text-base font-bold text-slate-900">
                    {{ user.name || 'Not provided' }}
                  </p>
                </div>

                <div class="rounded-2xl border border-slate-200/80 bg-white/75 p-5">
                  <p class="text-xs font-bold uppercase tracking-wider text-slate-400">
                    Email Address
                  </p>

                  <p class="mt-2 break-all text-base font-bold text-slate-900">
                    {{ user.email || 'Not provided' }}
                  </p>
                </div>

                <div class="rounded-2xl border border-slate-200/80 bg-white/75 p-5">
                  <p class="text-xs font-bold uppercase tracking-wider text-slate-400">
                    Phone Number
                  </p>

                  <p class="mt-2 text-base font-bold text-slate-900">
                    {{ user.phone || 'Not provided' }}
                  </p>
                </div>

                <div class="rounded-2xl border border-slate-200/80 bg-white/75 p-5">
                  <p class="text-xs font-bold uppercase tracking-wider text-slate-400">
                    Street Address
                  </p>

                  <p class="mt-2 text-base font-bold text-slate-900">
                    {{ user.address || 'Not provided' }}
                  </p>
                </div>

                <div class="rounded-2xl border border-slate-200/80 bg-white/75 p-5">
                  <p class="text-xs font-bold uppercase tracking-wider text-slate-400">
                    City
                  </p>

                  <p class="mt-2 text-base font-bold text-slate-900">
                    {{ user.city || 'Not provided' }}
                  </p>
                </div>

                <div class="rounded-2xl border border-slate-200/80 bg-white/75 p-5">
                  <p class="text-xs font-bold uppercase tracking-wider text-slate-400">
                    Postal Code
                  </p>

                  <p class="mt-2 text-base font-bold text-slate-900">
                    {{ user.postalCode || 'Not provided' }}
                  </p>
                </div>

                <div class="rounded-2xl border border-slate-200/80 bg-white/75 p-5 md:col-span-2">
                  <p class="text-xs font-bold uppercase tracking-wider text-slate-400">
                    Country
                  </p>

                  <p class="mt-2 text-base font-bold text-slate-900">
                    {{ user.country }}
                  </p>
                </div>

                <div class="rounded-2xl border border-slate-200/80 bg-white/75 p-5">
                  <p class="text-xs font-bold uppercase tracking-wider text-slate-400">
                    Date of Birth
                  </p>
                  <p class="mt-2 text-base font-bold text-slate-900">
                    {{ user.dob ? formatDate(user.dob) : 'Not provided' }}
                  </p>
                </div>

                <div class="rounded-2xl border border-slate-200/80 bg-white/75 p-5">
                  <p class="text-xs font-bold uppercase tracking-wider text-slate-400">
                    Loyalty Points
                  </p>
                  <p class="mt-2 text-base font-bold text-emerald-600">
                    {{ user.loyaltyPoints }} Points
                  </p>
                </div>
              </div>
            </div>

            <!-- EDIT -->

            <div v-else>
              <div class="border-b border-slate-200/70 pb-5">
                <p class="text-xs font-black uppercase tracking-[0.18em] text-blue-600">
                  Edit Profile
                </p>

                <h2 class="mt-1 text-2xl font-black text-slate-900">
                  Update Your Information
                </h2>

                <p class="mt-1 text-sm text-slate-500">
                  Change your details and save your updates.
                </p>
              </div>

              <div class="mt-8">
                <div class="grid grid-cols-1 gap-5 md:grid-cols-2">
                  <!-- Name -->

                  <div>
                    <label class="mb-2 block text-sm font-bold text-slate-700">
                      Full Name
                    </label>

                    <input v-model="user.name" type="text" placeholder="Enter your full name"
                      class="w-full rounded-xl border border-slate-200 bg-white/80 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-100" />
                  </div>

                  <!-- Email -->

                  <div>
                    <label class="mb-2 block text-sm font-bold text-slate-700">
                      Email Address
                    </label>

                    <input v-model="user.email" type="email" readonly
                      class="w-full cursor-not-allowed rounded-xl border border-slate-200 bg-slate-100/80 px-4 py-3 text-sm text-slate-500 outline-none" />

                    <p class="mt-1 text-xs text-slate-400">
                      Email cannot be changed here.
                    </p>
                  </div>

                  <!-- Phone -->

                  <div>
                    <label class="mb-2 block text-sm font-bold text-slate-700">
                      Phone Number
                    </label>

                    <input v-model="user.phone" type="text" placeholder="Enter your phone number"
                      class="w-full rounded-xl border border-slate-200 bg-white/80 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-100" />
                  </div>

                  <!-- Street -->

                  <div>
                    <label class="mb-2 block text-sm font-bold text-slate-700">
                      Street Address
                    </label>

                    <input v-model="user.address" type="text" placeholder="Enter your street address"
                      class="w-full rounded-xl border border-slate-200 bg-white/80 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-100" />
                  </div>

                  <!-- City -->

                  <div>
                    <label class="mb-2 block text-sm font-bold text-slate-700">
                      City
                    </label>

                    <input v-model="user.city" type="text" placeholder="Enter your city"
                      class="w-full rounded-xl border border-slate-200 bg-white/80 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-100" />
                  </div>

                  <!-- Postal -->

                  <div>
                    <label class="mb-2 block text-sm font-bold text-slate-700">
                      Postal Code
                    </label>

                    <input v-model="user.postalCode" type="text" placeholder="Enter postal code"
                      class="w-full rounded-xl border border-slate-200 bg-white/80 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-100" />
                  </div>

                  <!-- Date of Birth -->

                  <div>
                    <label class="mb-2 block text-sm font-bold text-slate-700">
                      Date of Birth
                    </label>

                    <input v-model="user.dob" type="date"
                      class="w-full rounded-xl border border-slate-200 bg-white/80 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-100" />
                  </div>

                  <!-- Country -->

                  <div class="md:col-span-2">
                    <label class="mb-2 block text-sm font-bold text-slate-700">
                      Country
                    </label>

                    <input v-model="user.country" type="text" readonly
                      class="w-full cursor-not-allowed rounded-xl border border-slate-200 bg-slate-100/80 px-4 py-3 text-sm text-slate-500 outline-none" />
                  </div>
                </div>

                <!-- Actions -->

                <div class="mt-8 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
                  <button type="button" @click="cancelProfileEdit"
                    class="rounded-xl border border-slate-200 bg-white px-5 py-3 text-sm font-bold text-slate-600 transition hover:bg-slate-50">
                    Cancel
                  </button>

                  <button type="button" @click="handleSaveProfile" :disabled="profileSaving"
                    class="inline-flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-6 py-3 text-sm font-bold text-white shadow-lg shadow-blue-500/20 transition hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-60">
                    <svg v-if="profileSaving" class="h-4 w-4 animate-spin" xmlns="http://www.w3.org/2000/svg"
                      fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
                    </svg>

                    {{
                      profileSaving
                        ? 'Saving...'
                        : 'Save Profile Changes'
                    }}
                  </button>
                </div>
              </div>
            </div>
          </section>

          <!-- ===================================================== -->
          <!-- PAYMENT METHODS -->
          <!-- ===================================================== -->

          <section v-else-if="activeTab === 'payments'"
            class="rounded-3xl border border-white/70 bg-white/80 p-6 shadow-md shadow-slate-200/50">
            <!-- Payment Header -->

            <div
              class="flex flex-col gap-4 border-b border-slate-200/70 pb-5 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <p class="text-xs font-black uppercase tracking-[0.18em] text-blue-600">
                  Secure Payments
                </p>

                <h2 class="mt-1 text-2xl font-black text-slate-900">
                  My Payment Methods
                </h2>

                <p class="mt-1 text-sm text-slate-500">
                  Save and manage your payment cards for faster checkout.
                </p>
              </div>

              <button type="button" @click="openAddCardModal"
                class="inline-flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-5 py-3 text-sm font-bold text-white shadow-lg shadow-blue-500/20 transition hover:-translate-y-0.5">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                  stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 5v14M5 12h14" />
                </svg>

                Add Card
              </button>
            </div>

            <!-- Loading (first load only, so add/remove/default don't flash a spinner) -->

            <div v-if="cardsLoading && savedCards.length === 0" class="flex min-h-[260px] items-center justify-center">
              <div class="text-center">
                <div class="mx-auto h-10 w-10 animate-spin rounded-full border-4 border-blue-100 border-t-blue-600">
                </div>

                <p class="mt-3 text-sm font-medium text-slate-500">
                  Loading payment methods...
                </p>
              </div>
            </div>

            <!-- Error -->

            <div v-else-if="cardsError" class="mt-6 rounded-2xl border border-red-100 bg-red-50 p-5 text-center">
              <p class="font-bold text-red-600">
                {{ cardsError }}
              </p>

              <button type="button" @click="fetchSavedCards"
                class="mt-3 rounded-lg bg-red-600 px-4 py-2 text-sm font-bold text-white">
                Try Again
              </button>
            </div>

            <!-- Cards -->

            <div v-else-if="savedCards.length > 0" class="mt-6 grid grid-cols-1 gap-5 lg:grid-cols-2">
              <div v-for="card in savedCards" :key="card.cardId"
                class="overflow-hidden rounded-3xl border border-white/80 bg-white/75 shadow-lg shadow-slate-200/50">
                <!-- PAYMENT CARD -->

                <div class="flex justify-center bg-white/30 p-3 sm:p-4">
                  <div
                    class="relative aspect-[1.586/1] w-full max-w-[340px] overflow-hidden rounded-2xl bg-gradient-to-br from-slate-900 via-slate-800 to-slate-700 p-4 text-white shadow-lg sm:p-5">
                    <div
                      class="pointer-events-none absolute -right-14 -top-14 h-36 w-36 bg-[radial-gradient(closest-side,rgba(59,130,246,0.25),transparent)]">
                    </div>

                    <div
                      class="pointer-events-none absolute -bottom-14 -left-14 h-36 w-36 bg-[radial-gradient(closest-side,rgba(34,211,238,0.15),transparent)]">
                    </div>

                    <div class="relative flex h-full flex-col justify-between">
                      <!-- TOP -->

                      <div class="flex items-start justify-between">
                        <div>
                          <p class="text-[7px] font-bold uppercase tracking-[0.18em] text-white/40">
                            Payment Card
                          </p>

                          <p class="mt-0.5 text-sm font-black">
                            {{ card.cardBrand }}
                          </p>
                        </div>

                        <span v-if="isDefaultCard(card)"
                          class="rounded-full bg-white/10 px-2 py-1 text-[7px] font-bold uppercase tracking-wider text-white ring-1 ring-white/15">
                          Default
                        </span>
                      </div>

                      <!-- CHIP -->

                      <div class="flex items-center justify-between">
                        <div
                          class="flex h-7 w-10 items-center justify-center rounded-md border border-yellow-200/40 bg-gradient-to-br from-yellow-100 to-yellow-300 shadow-md">
                          <div class="h-3.5 w-5 rounded border border-yellow-700/20"></div>
                        </div>

                        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-white/50" fill="none"
                          viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.4">
                          <path stroke-linecap="round" stroke-linejoin="round" d="M6 8a8 8 0 010 8" />
                          <path stroke-linecap="round" stroke-linejoin="round" d="M10 5a12 12 0 010 14" />
                          <path stroke-linecap="round" stroke-linejoin="round" d="M14 5a12 12 0 010 14" />
                          <path stroke-linecap="round" stroke-linejoin="round" d="M18 8a8 8 0 010 8" />
                        </svg>
                      </div>

                      <!-- NUMBER -->

                      <div>
                        <p class="text-[6px] font-semibold uppercase tracking-[0.16em] text-white/30">
                          Card Number
                        </p>

                        <p class="mt-0.5 truncate font-mono text-sm font-bold tracking-[0.08em]">
                          {{ card.maskedCardNumber }}
                        </p>
                      </div>

                      <!-- BOTTOM -->

                      <div class="flex items-end justify-between gap-2">
                        <div class="min-w-0 flex-1">
                          <p class="text-[6px] font-semibold uppercase tracking-[0.16em] text-white/30">
                            Card Holder
                          </p>

                          <p class="mt-0.5 truncate text-[10px] font-bold uppercase">
                            {{ card.cardHolderName }}
                          </p>
                        </div>

                        <div class="shrink-0 text-right">
                          <p class="text-[6px] font-semibold uppercase tracking-[0.16em] text-white/30">
                            Valid Thru
                          </p>

                          <p class="mt-0.5 text-[10px] font-bold">
                            {{ card.expiryMonth }}/{{ card.expiryYear.slice(-2) }}
                          </p>
                        </div>

                        <div class="flex shrink-0 items-center">
                          <div class="h-4 w-4 rounded-full bg-red-500/90"></div>

                          <div class="-ml-1.5 h-4 w-4 rounded-full bg-yellow-400/90"></div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- DETAILS -->

                <div class="p-4">
                  <div class="grid grid-cols-3 gap-2">
                    <div class="rounded-xl bg-slate-50 p-3">
                      <p class="text-[9px] font-bold uppercase tracking-wider text-slate-400">
                        Type
                      </p>

                      <p class="mt-1 text-xs font-bold text-slate-800">
                        {{ card.cardBrand }}
                      </p>
                    </div>

                    <div class="rounded-xl bg-slate-50 p-3">
                      <p class="text-[9px] font-bold uppercase tracking-wider text-slate-400">
                        Expiry
                      </p>

                      <p class="mt-1 text-xs font-bold text-slate-800">
                        {{ card.expiryMonth }}/{{ card.expiryYear }}
                      </p>
                    </div>

                    <div class="rounded-xl bg-slate-50 p-3">
                      <p class="text-[9px] font-bold uppercase tracking-wider text-slate-400">
                        Status
                      </p>

                      <p class="mt-1 text-xs font-bold text-emerald-600">
                        Active
                      </p>
                    </div>
                  </div>

                  <!-- ACTIONS -->

                  <div class="mt-3 flex flex-wrap gap-2">
                    <button v-if="!isDefaultCard(card)" type="button" @click="setDefaultCard(card.cardId)"
                      class="rounded-lg bg-blue-50 px-3 py-2 text-xs font-bold text-blue-600 transition hover:bg-blue-100">
                      Set Default
                    </button>

                    <span v-else class="rounded-lg bg-emerald-50 px-3 py-2 text-xs font-bold text-emerald-600">
                      Default Card
                    </span>



                    <button type="button" @click="removeCard(card.cardId)" :disabled="deletingCardId === card.cardId"
                      class="rounded-lg bg-red-50 px-3 py-2 text-xs font-bold text-red-600 transition hover:bg-red-100 disabled:opacity-50">
                      {{
                        deletingCardId === card.cardId
                          ? 'Removing...'
                          : 'Remove'
                      }}
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- EMPTY -->

            <div v-else
              class="mt-6 rounded-3xl border border-dashed border-slate-300 bg-white/50 px-6 py-14 text-center">
              <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-blue-50 text-blue-600">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8" fill="none" viewBox="0 0 24 24"
                  stroke="currentColor" stroke-width="1.6">
                  <rect x="3" y="5" width="18" height="14" rx="2" />
                  <path stroke-linecap="round" stroke-linejoin="round" d="M3 10h18" />
                </svg>
              </div>

              <h3 class="mt-5 text-xl font-black text-slate-900">
                No Payment Cards
              </h3>

              <p class="mx-auto mt-2 max-w-md text-sm text-slate-500">
                Add a debit or credit card to make future checkouts faster.
              </p>

              <button type="button" @click="openAddCardModal"
                class="mt-5 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-5 py-3 text-sm font-bold text-white shadow-lg shadow-blue-500/20">
                Add Your First Card
              </button>
            </div>

            <!-- WALLETS -->

            <div class="mt-8 border-t border-slate-200/70 pt-6">
              <div class="flex items-center justify-between">
                <div>
                  <h3 class="text-lg font-black text-slate-900">
                    My Wallets
                  </h3>

                  <p class="mt-1 text-sm text-slate-500">
                    Connected wallet accounts.
                  </p>
                </div>
              </div>

              <div v-if="walletsLoading"
                class="mt-4 rounded-2xl border border-slate-200 bg-white/70 p-5 text-sm text-slate-500">
                Loading wallets...
              </div>

              <div v-else-if="savedWallets.length" class="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2">
                <div v-for="wallet in savedWallets" :key="wallet.walletId"
                  class="rounded-2xl border border-slate-200 bg-white/75 p-4">
                  <div class="flex items-center justify-between">
                    <div>
                      <p class="font-bold text-slate-900">
                        {{ wallet.walletName }}
                      </p>

                      <p class="mt-1 text-xs text-slate-500">
                        {{ wallet.email }}
                      </p>
                    </div>

                    <span class="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-bold text-emerald-600">
                      {{ wallet.status }}
                    </span>
                  </div>
                </div>
              </div>

              <div v-else
                class="mt-4 rounded-2xl border border-dashed border-slate-300 bg-white/50 p-5 text-sm text-slate-500">
                No connected wallets.
              </div>
            </div>

            <!-- FAST CHECKOUT -->

            <div class="mt-6 flex items-start gap-4 rounded-2xl border border-blue-100 bg-blue-50/70 p-5">
              <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-blue-600 text-white">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24"
                  stroke="currentColor" stroke-width="1.8">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M13 3L4 14h7l-1 7 9-11h-7l1-7z" />
                </svg>
              </div>

              <div>
                <h4 class="font-bold text-slate-900">
                  Fast Checkout
                </h4>

                <p class="mt-1 text-sm text-slate-500">
                  Your saved cards can be selected during checkout without
                  entering the full card number again.
                </p>
              </div>
            </div>
          </section>

          <!-- ===================================================== -->
          <!-- SECURITY -->
          <!-- ===================================================== -->

          <section v-else class="rounded-3xl border border-white/70 bg-white/80 p-6 shadow-md shadow-slate-200/50">
            <div class="border-b border-slate-200/70 pb-5">
              <p class="text-xs font-black uppercase tracking-[0.18em] text-blue-600">
                Account Protection
              </p>

              <h2 class="mt-1 text-2xl font-black text-slate-900">
                Security
              </h2>

              <p class="mt-1 text-sm text-slate-500">
                Manage your account security settings.
              </p>
            </div>

            <div class="mt-6 space-y-4">
              <!-- PASSWORD -->

              <div
                class="flex flex-col gap-4 rounded-2xl border border-slate-200 bg-white/75 p-5 sm:flex-row sm:items-center sm:justify-between">
                <div class="flex items-center gap-4">
                  <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24"
                      stroke="currentColor" stroke-width="1.8">
                      <rect x="4" y="10" width="16" height="10" rx="2" />
                      <path stroke-linecap="round" stroke-linejoin="round" d="M8 10V7a4 4 0 118 0v3" />
                    </svg>
                  </div>

                  <div>
                    <h3 class="font-bold text-slate-900">
                      Password
                    </h3>

                    <p class="mt-1 text-sm text-slate-500">
                      Keep your password secure and up to date.
                    </p>
                  </div>
                </div>

                <button type="button" @click="promptChangePassword"
                  class="rounded-xl bg-slate-100 px-4 py-2.5 text-sm font-bold text-slate-700 transition hover:bg-slate-200">
                  Change Password
                </button>
              </div>

              <!-- STATUS -->

              <div
                class="flex flex-col gap-4 rounded-2xl border border-slate-200 bg-white/75 p-5 sm:flex-row sm:items-center sm:justify-between">
                <div class="flex items-center gap-4">
                  <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24"
                      stroke="currentColor" stroke-width="1.8">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4" />
                      <circle cx="12" cy="12" r="9" />
                    </svg>
                  </div>

                  <div>
                    <h3 class="font-bold text-slate-900">
                      Account Status
                    </h3>

                    <p class="mt-1 text-sm text-slate-500">
                      Your account is currently active.
                    </p>
                  </div>
                </div>

                <span class="rounded-full bg-emerald-50 px-3 py-2 text-xs font-bold text-emerald-600">
                  Active
                </span>
              </div>

              <!-- REMINDER -->

              <div class="rounded-2xl border border-amber-100 bg-amber-50/70 p-5">
                <div class="flex gap-4">
                  <div
                    class="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-amber-100 text-amber-600">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24"
                      stroke="currentColor" stroke-width="1.8">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v4" />
                      <path stroke-linecap="round" stroke-linejoin="round" d="M12 17h.01" />
                      <path stroke-linecap="round" stroke-linejoin="round"
                        d="M10.3 4.5L2.7 18a2 2 0 001.74 3h15.12a2 2 0 001.74-3L13.7 4.5a2 2 0 00-3.4 0z" />
                    </svg>
                  </div>

                  <div>
                    <h3 class="font-bold text-slate-900">
                      Security Reminder
                    </h3>

                    <p class="mt-1 text-sm leading-6 text-slate-600">
                      Never share your password, card number, CVV or
                      verification code with anyone.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </main>
      </div>
    </div>

    <!-- ========================================================= -->
    <!-- CARD MODAL -->
    <!-- ========================================================= -->

    <div v-if="showCardModal" class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4"
      @click.self="closeCardModal">
      <div class="w-full max-w-lg overflow-hidden rounded-3xl border border-white/80 bg-white shadow-2xl">
        <!-- Modal Header -->

        <div class="flex items-center justify-between border-b border-slate-200 p-6">
          <div>
            <p class="text-xs font-black uppercase tracking-[0.18em] text-blue-600">
              Secure Payment
            </p>

            <h2 class="mt-1 text-xl font-black text-slate-900">
              {{
                editingCardId
                  ? 'Edit Payment Card'
                  : 'Add Payment Card'
              }}
            </h2>
          </div>

          <button type="button" @click="closeCardModal"
            class="flex h-9 w-9 items-center justify-center rounded-xl bg-slate-100 text-slate-500 transition hover:bg-slate-200">
            ×
          </button>
        </div>

        <!-- Form -->

        <div class="space-y-5 p-6">
          <!-- Holder -->

          <div>
            <label class="mb-2 block text-sm font-bold text-slate-700">
              Card Holder Name
            </label>

            <input v-model="cardForm.cardHolderName" type="text" placeholder="SAMITHA NIRMALA"
              class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-100" />
          </div>

          <!-- Card Number -->

          <div>
            <label class="mb-2 block text-sm font-bold text-slate-700">
              Card Number
            </label>

            <input v-model="cardForm.cardNumber" type="text" inputmode="numeric" maxlength="19" :placeholder="editingCardId
              ? 'Leave blank to keep current card'
              : '4242 4242 4242 4242'
              "
              class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 font-mono text-sm tracking-wider outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-100" />

            <p class="mt-1 text-xs text-slate-400">
              Only the masked number and last four digits are stored.
            </p>
          </div>

          <!-- Expiry -->

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="mb-2 block text-sm font-bold text-slate-700">
                Expiry Month
              </label>

              <select v-model="cardForm.expiryMonth"
                class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
                <option value="">
                  Month
                </option>

                <option v-for="month in 12" :key="month" :value="String(month).padStart(2, '0')">
                  {{ String(month).padStart(2, '0') }}
                </option>
              </select>
            </div>

            <div>
              <label class="mb-2 block text-sm font-bold text-slate-700">
                Expiry Year
              </label>

              <select v-model="cardForm.expiryYear"
                class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-100">
                <option value="">
                  Year
                </option>

                <option v-for="year in expiryYears" :key="year" :value="String(year)">
                  {{ year }}
                </option>
              </select>
            </div>
          </div>

          <!-- Default -->

          <label class="flex cursor-pointer items-center gap-3 rounded-xl border border-slate-200 bg-slate-50 p-4">
            <input v-model="cardForm.isDefault" type="checkbox"
              class="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500" />

            <div>
              <p class="text-sm font-bold text-slate-800">
                Make this my default card
              </p>

              <p class="mt-1 text-xs text-slate-500">
                This card will be selected first during checkout.
              </p>
            </div>
          </label>

          <!-- Buttons -->

          <div class="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
            <button type="button" @click="closeCardModal"
              class="rounded-xl border border-slate-200 bg-white px-5 py-3 text-sm font-bold text-slate-600 transition hover:bg-slate-50">
              Cancel
            </button>

            <button type="button" @click="saveCard" :disabled="cardSaving"
              class="inline-flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-6 py-3 text-sm font-bold text-white shadow-lg shadow-blue-500/20 transition disabled:cursor-not-allowed disabled:opacity-60">
              <svg v-if="cardSaving" class="h-4 w-4 animate-spin" xmlns="http://www.w3.org/2000/svg" fill="none"
                viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
              </svg>

              {{
                cardSaving
                  ? 'Saving...'
                  : 'Save Card'
              }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- =====================================================
         CUSTOM CENTER POPUP
    ====================================================== -->
    <CustomCenterPopup :visible="popupVisible" :type="popupType" :title="popupTitle" :message="popupMessage"
      :show-cancel="popupShowCancel" :cancel-text="popupCancelText" :confirm-text="popupConfirmText"
      @confirm="confirmPopup" @cancel="closePopup" @close="closePopup" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import api from '../../services/api'
import { useNotificationStore } from '../../stores/notificationStore'
import CustomCenterPopup from '../../components/common/CustomCenterPopup.vue'

// If you compressed the image, point this import at the new .webp file
import backgroundImage from '../../assets/images/vecteezy_abstract-blur-shopping-mall_2795585.jpg'

const router = useRouter()
const notify = useNotificationStore()

/* =========================================================
   TYPES
========================================================= */

interface UserAddress {
  street?: string
  city?: string
  postalCode?: string
}

interface StoredUser {
  userId?: string
  customerId?: string
  id?: string
  name?: string
  email?: string
  phone?: string
  registeredDate?: string
  userImage?: string
  address?: UserAddress
}

interface SavedCard {
  cardId: string
  cardBrand: string
  maskedCardNumber: string
  lastFour?: string
  expiryMonth: string
  expiryYear: string
  cardHolderName: string
  isDefault?: boolean
  default?: boolean
}

interface SavedWallet {
  walletId: string
  walletName: string
  email: string
  status: string
}

/* =========================================================
   CUSTOM CENTER POPUP STATE & CONTROLLER
========================================================= */

type PopupType = 'info' | 'success' | 'warning' | 'error'

const popupVisible = ref(false)
const popupType = ref<PopupType>('info')
const popupTitle = ref('')
const popupMessage = ref('')
const popupShowCancel = ref(false)
const popupCancelText = ref('Cancel')
const popupConfirmText = ref('OK')
const popupOnConfirm = ref<(() => void | Promise<void>) | null>(null)
const popupOnCancel = ref<(() => void) | null>(null)

function showPopup(options: {
  type?: PopupType
  title: string
  message: string
  showCancel?: boolean
  confirmText?: string
  cancelText?: string
  onConfirm?: () => void | Promise<void>
  onCancel?: () => void
}) {
  popupType.value = options.type || 'info'
  popupTitle.value = options.title
  popupMessage.value = options.message
  popupShowCancel.value = options.showCancel ?? false
  popupConfirmText.value = options.confirmText || 'OK'
  popupCancelText.value = options.cancelText || 'Cancel'
  popupOnConfirm.value = options.onConfirm || null
  popupOnCancel.value = options.onCancel || null
  popupVisible.value = true
}

function closePopup() {
  const cancelCb = popupOnCancel.value
  popupVisible.value = false
  popupOnConfirm.value = null
  popupOnCancel.value = null
  if (cancelCb) {
    cancelCb()
  }
}

async function confirmPopup() {
  const confirmCb = popupOnConfirm.value
  popupVisible.value = false
  popupOnConfirm.value = null
  popupOnCancel.value = null
  if (confirmCb) {
    await confirmCb()
  }
}

function promptChangePassword() {
  showPopup({
    type: 'info',
    title: 'Password Management',
    message: 'To change or reset your password, please use the password recovery option on the login page or contact customer support for verification.',
    confirmText: 'Understood'
  })
}

/* =========================================================
   USER
========================================================= */

const user = ref({
  userId: '',
  name: '',
  email: '',
  role: 'Enthusiast Member',
  joinedDate: '',
  avatar: '',
  phone: '',
  address: '',
  city: '',
  postalCode: '',
  country: 'Sri Lanka',
  loyaltyPoints: 0,
  dob: ''
})

/* =========================================================
   TABS
========================================================= */

const activeTab = ref<'profile' | 'payments' | 'security'>('payments')

function openProfileTab() {
  activeTab.value = 'profile'
  isEditingProfile.value = false
}

function openPaymentsTab() {
  activeTab.value = 'payments'
  isEditingProfile.value = false
}

function openSecurityTab() {
  activeTab.value = 'security'
  isEditingProfile.value = false
}

/* =========================================================
   PROFILE
========================================================= */

const isEditingProfile = ref(false)
const profileSaving = ref(false)
const profileImageInput = ref<HTMLInputElement | null>(null)

/* =========================================================
   PAYMENT CARDS
========================================================= */

const savedCards = ref<SavedCard[]>([])
const savedWallets = ref<SavedWallet[]>([])
const selectedCardId = ref<string | null>(null)

const cardsLoading = ref(false)
const walletsLoading = ref(false)

const cardsError = ref('')
const walletsError = ref('')

const showCardModal = ref(false)
const editingCardId = ref<string | null>(null)
const cardSaving = ref(false)
const deletingCardId = ref<string | null>(null)

const cardForm = ref({
  cardHolderName: '',
  cardNumber: '',
  expiryMonth: '',
  expiryYear: '',
  isDefault: false
})

/* =========================================================
   EXPIRY YEARS
========================================================= */

const currentYear = new Date().getFullYear()

const expiryYears = computed(() => {
  const years: number[] = []

  for (let i = 0; i < 12; i++) {
    years.push(currentYear + i)
  }

  return years
})

/* =========================================================
   LOAD USER
========================================================= */

async function loadUser() {
  const storedUser = sessionStorage.getItem('user')
  if (!storedUser) {
    user.value.avatar = 'https://ui-avatars.com/api/?name=User&background=2563eb&color=fff'
    return
  }

  try {
    const userData: StoredUser = JSON.parse(storedUser)
    const id = userData.userId || userData.customerId || userData.id || ''
    user.value.userId = id

    // fallback data
    user.value.name = userData.name || ''
    user.value.email = userData.email || ''
    user.value.phone = userData.phone || ''
    user.value.joinedDate = userData.registeredDate || ''
    user.value.address = userData.address?.street || ''
    user.value.city = userData.address?.city || ''
    user.value.postalCode = userData.address?.postalCode || ''
    user.value.avatar = resolveAvatarUrl(userData.userImage, userData.name)

    if (id) {
      try {
        const res = await api.get(`/customers/${encodeURIComponent(id)}`)
        if (res.data) {
          const apiUser = res.data
          user.value.name = apiUser.fullName || apiUser.name || [apiUser.firstName, apiUser.lastName].filter(Boolean).join(' ') || userData.name || ''
          user.value.email = apiUser.email || userData.email || ''
          user.value.phone = apiUser.phone || apiUser.phoneNumber || userData.phone || ''
          user.value.joinedDate = apiUser.registeredDate || apiUser.joinedDate || apiUser.createdAt || userData.registeredDate || ''
          user.value.address = apiUser.address?.street || userData.address?.street || ''
          user.value.city = apiUser.address?.city || userData.address?.city || ''
          user.value.postalCode = apiUser.address?.postalCode || userData.address?.postalCode || ''
          const rawImg = apiUser.userImage || apiUser.image || apiUser.profileImage || apiUser.avatar || userData.userImage
          user.value.avatar = resolveAvatarUrl(rawImg, user.value.name)
          user.value.loyaltyPoints = apiUser.loyaltyPoints || 0
          user.value.dob = apiUser.dob || ''

          // update session storage with fresh data
          userData.name = user.value.name
          userData.email = user.value.email
          userData.phone = user.value.phone
          userData.userImage = rawImg || user.value.avatar
          if (!userData.address) userData.address = {} as any
          userData.address!.street = user.value.address
          userData.address!.city = user.value.city
          userData.address!.postalCode = user.value.postalCode
          sessionStorage.setItem('user', JSON.stringify(userData))
        }
      } catch (err) {
        console.warn('Could not fetch fresh customer data', err)
      }
    }
  } catch (error) {
    console.error('Failed to load user:', error)
    user.value.avatar = 'https://ui-avatars.com/api/?name=User&background=2563eb&color=fff'
  }
}

/* =========================================================
   PROFILE IMAGE
========================================================= */

function resolveAvatarUrl(image?: string | null, name?: string): string {
  if (!image || typeof image !== 'string') {
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(name || 'User')}&background=2563eb&color=fff`
  }

  const value = image.trim()
  if (!value) {
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(name || 'User')}&background=2563eb&color=fff`
  }

  if (value.startsWith('http://') || value.startsWith('https://') || value.startsWith('data:')) {
    return value
  }

  const backendBase = 'http://localhost:8080'
  if (value.startsWith('/')) {
    return `${backendBase}${value}`
  }

  if (value.startsWith('uploads/')) {
    return `${backendBase}/${value}`
  }

  return `${backendBase}/uploads/${value}`
}

function triggerProfileImageUpload() {
  profileImageInput.value?.click()
}

async function handleProfileImageChange(event: Event) {
  const input = event.target as HTMLInputElement

  if (!input.files || input.files.length === 0) {
    return
  }

  const file = input.files[0]

  if (!file.type.startsWith('image/')) {
    showPopup({
      type: 'warning',
      title: 'Invalid Image Format',
      message: 'Please select a valid image file (PNG, JPG, or WebP).'
    })
    notify.error('Please select a valid image file.', 'Invalid Image')
    input.value = ''
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    showPopup({
      type: 'warning',
      title: 'Image File Too Large',
      message: 'Please select an image smaller than 5MB.'
    })
    notify.error('Please select an image smaller than 5MB.', 'Image Too Large')
    input.value = ''
    return
  }

  profileSaving.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)

    const uploadRes = await api.post('/uploads/image', formData)

    const imageUrl = uploadRes.data?.url
    if (!imageUrl) {
      throw new Error('Upload succeeded but server did not return image URL.')
    }

    user.value.avatar = resolveAvatarUrl(imageUrl, user.value.name)

    await handleSaveProfile(true)
    showPopup({
      type: 'success',
      title: 'Avatar Updated',
      message: 'Your profile picture has been updated and saved successfully.'
    })
    notify.success('Your profile image has been updated successfully.', 'Image Updated')
  } catch (err: any) {
    console.error('Failed to upload profile image:', err)
    showPopup({
      type: 'error',
      title: 'Upload Error',
      message: err?.response?.data?.message || err?.message || 'Failed to upload profile image.'
    })
    notify.error(
      err?.response?.data?.message || err?.message || 'Failed to upload profile image.',
      'Upload Error'
    )
  } finally {
    profileSaving.value = false
    input.value = ''
  }
}

/* =========================================================
   PROFILE EDIT
   Uses a local backup instead of re-calling the API, so an
   in-flight response can't overwrite what the user is typing.
========================================================= */

let profileBackup: typeof user.value | null = null

function startProfileEdit() {
  profileBackup = { ...user.value }
  isEditingProfile.value = true
}

function cancelProfileEdit() {
  const isDirty = profileBackup && (
    user.value.name !== profileBackup.name ||
    user.value.phone !== profileBackup.phone ||
    user.value.address !== profileBackup.address ||
    user.value.city !== profileBackup.city ||
    user.value.postalCode !== profileBackup.postalCode ||
    user.value.dob !== profileBackup.dob
  )

  if (isDirty) {
    showPopup({
      type: 'warning',
      title: 'Discard Changes',
      message: 'You have unsaved changes in your profile. Are you sure you want to discard them?',
      showCancel: true,
      confirmText: 'Discard Changes',
      cancelText: 'Keep Editing',
      onConfirm: () => {
        if (profileBackup) {
          user.value = { ...profileBackup }
        }
        profileBackup = null
        isEditingProfile.value = false
      }
    })
    return
  }

  if (profileBackup) {
    user.value = { ...profileBackup }
  }

  profileBackup = null
  isEditingProfile.value = false
}

async function handleSaveProfile(silent: boolean | unknown = false) {
  const isSilent = silent === true
  const storedUser = sessionStorage.getItem('user')
  if (!storedUser) {
    showPopup({
      type: 'error',
      title: 'Authentication Required',
      message: 'User session not found. Please sign in again.'
    })
    notify.error('User session not found.', 'Authentication Required')
    return
  }

  if (!user.value.name.trim()) {
    showPopup({
      type: 'warning',
      title: 'Missing Name',
      message: 'Please provide your full name before saving.'
    })
    return
  }

  profileSaving.value = true

  try {
    const savedUser: StoredUser = JSON.parse(storedUser)
    const id = savedUser.userId || savedUser.customerId || savedUser.id

    if (id) {
      try {
        const payload = {
          name: user.value.name.trim(),
          email: user.value.email.trim(),
          password: (savedUser as any).password || 'dummyPassword123',
          phone: user.value.phone.trim(),
          userImage: user.value.avatar,
          dob: user.value.dob || undefined,
          address: {
            street: user.value.address.trim(),
            city: user.value.city.trim(),
            postalCode: user.value.postalCode.trim()
          }
        }
        await api.put(`/customers/${encodeURIComponent(id)}`, payload)
      } catch (apiErr: any) {
        console.warn('Could not save to backend API', apiErr)
        throw new Error(apiErr?.response?.data?.message || 'Failed to update profile on server')
      }
    }

    const updatedUser: StoredUser = {
      ...savedUser,
      name: user.value.name.trim(),
      phone: user.value.phone.trim(),
      userImage: user.value.avatar,
      address: {
        street: user.value.address.trim(),
        city: user.value.city.trim(),
        postalCode: user.value.postalCode.trim()
      }
    }

    sessionStorage.setItem('user', JSON.stringify(updatedUser))

    // notify navbar
    window.dispatchEvent(new Event('user-profile-updated'))

    if (!isSilent) {
      showPopup({
        type: 'success',
        title: 'Profile Saved',
        message: 'Your personal information and address have been updated successfully.'
      })
      notify.success('Your profile has been updated successfully.', 'Profile Updated')
    }
    profileBackup = null
    isEditingProfile.value = false
    await loadUser()
  } catch (error: any) {
    console.error('Failed to save profile:', error)
    showPopup({
      type: 'error',
      title: 'Save Failed',
      message: error?.message || 'Failed to save profile details. Please try again.'
    })
    notify.error('Failed to save profile details.', 'Save Error')
  } finally {
    profileSaving.value = false
  }
}

/* =========================================================
   CUSTOMER ID
========================================================= */

function getCustomerId(): string {
  const storedUser = sessionStorage.getItem('user')

  if (!storedUser) {
    return ''
  }

  try {
    const userData: StoredUser = JSON.parse(storedUser)

    return (
      userData.customerId ||
      userData.userId ||
      userData.id ||
      user.value.userId ||
      ''
    )
  } catch {
    return user.value.userId
  }
}

/* =========================================================
   NORMALIZE CARD
========================================================= */

function normalizeCard(card: any): SavedCard {
  return {
    cardId: String(card.cardId || ''),
    cardBrand: card.cardBrand || 'CARD',
    maskedCardNumber:
      card.maskedCardNumber || `**** **** **** ${card.lastFour || '0000'}`,
    lastFour: card.lastFour || '',
    expiryMonth: String(card.expiryMonth || '').padStart(2, '0'),
    expiryYear: String(card.expiryYear || ''),
    cardHolderName: card.cardHolderName || '',
    isDefault: card.isDefault === true || card.default === true,
    default: card.default === true
  }
}

/* =========================================================
   CHECK DEFAULT
========================================================= */

function isDefaultCard(card: SavedCard): boolean {
  return (
    card.isDefault === true ||
    card.default === true ||
    selectedCardId.value === card.cardId
  )
}

/* =========================================================
   FETCH CARDS
========================================================= */

async function fetchSavedCards() {
  const customerId = getCustomerId()

  if (!customerId) {
    cardsError.value = 'Customer information not found.'
    return
  }

  cardsLoading.value = true
  cardsError.value = ''

  try {
    const response = await api.get(
      `/payments/cards?customerId=${encodeURIComponent(customerId)}`
    )

    const data = Array.isArray(response.data) ? response.data : []
    const cards = data.map(normalizeCard)

    savedCards.value = cards

    const defaultCard = cards.find(
      (card: SavedCard) => card.isDefault === true || card.default === true
    )

    selectedCardId.value = defaultCard ? defaultCard.cardId : null
  } catch (error) {
    console.error('Failed to load cards:', error)

    savedCards.value = []
    selectedCardId.value = null
    cardsError.value = 'Unable to load your payment cards.'
  } finally {
    cardsLoading.value = false
  }
}

/* =========================================================
   FETCH WALLETS
========================================================= */

async function fetchSavedWallets() {
  const customerId = getCustomerId()

  if (!customerId) {
    return
  }

  walletsLoading.value = true
  walletsError.value = ''

  try {
    const response = await api.get(
      `/payments/wallets?customerId=${encodeURIComponent(customerId)}`
    )

    savedWallets.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    console.warn('Wallet endpoint is unavailable:', error)

    savedWallets.value = []
    walletsError.value = ''
  } finally {
    walletsLoading.value = false
  }
}

/* =========================================================
   ADD CARD
========================================================= */

function openAddCardModal() {
  editingCardId.value = null

  cardForm.value = {
    cardHolderName: user.value.name || '',
    cardNumber: '',
    expiryMonth: '',
    expiryYear: '',
    isDefault: false
  }

  showCardModal.value = true
}

/* =========================================================
   EDIT CARD
========================================================= */

function openEditCardModal(card: SavedCard) {
  editingCardId.value = card.cardId

  cardForm.value = {
    cardHolderName: card.cardHolderName,
    cardNumber: '',
    expiryMonth: card.expiryMonth,
    expiryYear: card.expiryYear,
    isDefault: isDefaultCard(card)
  }

  showCardModal.value = true
}

/* =========================================================
   CLOSE MODAL
========================================================= */

function closeCardModal() {
  if (cardSaving.value) {
    return
  }

  showCardModal.value = false
  editingCardId.value = null

  cardForm.value = {
    cardHolderName: '',
    cardNumber: '',
    expiryMonth: '',
    expiryYear: '',
    isDefault: false
  }
}

/* =========================================================
   SAVE CARD
========================================================= */

async function saveCard() {
  const customerId = getCustomerId()

  if (!customerId) {
    showPopup({
      type: 'error',
      title: 'Authentication Required',
      message: 'Customer information was not found. Please sign in again.'
    })
    notify.error('Customer information was not found.', 'Authentication Required')
    return
  }

  const holderName = cardForm.value.cardHolderName.trim()
  const cardNumber = cardForm.value.cardNumber.replace(/\s+/g, '')

  if (!holderName) {
    showPopup({
      type: 'warning',
      title: 'Missing Information',
      message: 'Please enter the card holder name.'
    })
    return
  }

  if (!cardForm.value.expiryMonth) {
    showPopup({
      type: 'warning',
      title: 'Missing Information',
      message: 'Please select the expiry month.'
    })
    return
  }

  if (!cardForm.value.expiryYear) {
    showPopup({
      type: 'warning',
      title: 'Missing Information',
      message: 'Please select the expiry year.'
    })
    return
  }

  cardSaving.value = true

  try {
    if (!editingCardId.value) {
      if (!cardNumber) {
        showPopup({
          type: 'warning',
          title: 'Missing Information',
          message: 'Please enter the 16-digit card number.'
        })
        cardSaving.value = false
        return
      }

      await api.post('/payments/cards', {
        customerId,
        cardHolderName: holderName,
        cardNumber,
        expiryMonth: cardForm.value.expiryMonth,
        expiryYear: cardForm.value.expiryYear
      })

      await fetchSavedCards()

      const newCard = savedCards.value[0]

      if (cardForm.value.isDefault && newCard?.cardId) {
        await api.put(`/payments/cards/${newCard.cardId}/default`)
        await fetchSavedCards()
      }

      showPopup({
        type: 'success',
        title: 'Payment Card Added',
        message: 'Your payment card has been saved successfully.'
      })
      notify.success('Your payment card has been saved.', 'Card Added')
    } else {
      await api.put(`/payments/cards/${editingCardId.value}`, {
        customerId,
        cardHolderName: holderName,
        expiryMonth: cardForm.value.expiryMonth,
        expiryYear: cardForm.value.expiryYear,
        default: cardForm.value.isDefault
      })

      if (cardForm.value.isDefault) {
        await api.put(`/payments/cards/${editingCardId.value}/default`)
      }

      await fetchSavedCards()

      showPopup({
        type: 'success',
        title: 'Payment Card Updated',
        message: 'Your payment card has been updated successfully.'
      })
      notify.success('Your payment card has been updated.', 'Card Updated')
    }

    closeCardModal()
  } catch (error: any) {
    console.error('Failed to save card:', error)

    const message =
      error?.response?.data?.message || 'Failed to save the payment card.'

    showPopup({
      type: 'error',
      title: 'Card Error',
      message
    })
    notify.error(message, 'Card Error')
  } finally {
    cardSaving.value = false
  }
}

/* =========================================================
   SET DEFAULT CARD
========================================================= */

async function setDefaultCard(cardId: string) {
  try {
    await api.put(`/payments/cards/${cardId}/default`)
    await fetchSavedCards()

    showPopup({
      type: 'success',
      title: 'Default Card Updated',
      message: 'This payment card is now selected as your primary default method.'
    })
    notify.success('Default payment card updated.', 'Default Card')
  } catch (error: any) {
    console.error('Failed to set default card:', error)

    showPopup({
      type: 'error',
      title: 'Card Error',
      message: error?.response?.data?.message || 'Unable to set this card as default.'
    })
    notify.error(
      error?.response?.data?.message || 'Unable to set this card as default.',
      'Card Error'
    )
  }
}

/* =========================================================
   REMOVE CARD
========================================================= */

function removeCard(cardId: string) {
  showPopup({
    type: 'warning',
    title: 'Remove Payment Card',
    message: 'Are you sure you want to remove this payment card? This action cannot be undone.',
    showCancel: true,
    confirmText: 'Remove Card',
    cancelText: 'Cancel',
    onConfirm: async () => {
      deletingCardId.value = cardId

      try {
        await api.delete(`/payments/cards/${cardId}`)
        await fetchSavedCards()

        showPopup({
          type: 'success',
          title: 'Card Removed',
          message: 'The payment card has been successfully removed.'
        })
        notify.success('The payment card has been removed.', 'Card Removed')
      } catch (error: any) {
        console.error('Failed to remove card:', error)

        showPopup({
          type: 'error',
          title: 'Card Error',
          message: error?.response?.data?.message || 'Unable to remove the payment card.'
        })
        notify.error(
          error?.response?.data?.message || 'Unable to remove the payment card.',
          'Card Error'
        )
      } finally {
        deletingCardId.value = null
      }
    }
  })
}

/* =========================================================
   DATE
========================================================= */

function formatDate(dateValue: string) {
  if (!dateValue) {
    return ''
  }

  const date = new Date(dateValue)

  if (Number.isNaN(date.getTime())) {
    return dateValue
  }

  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

/* =========================================================
   LOGOUT
========================================================= */

function handleLogout() {
  showPopup({
    type: 'warning',
    title: 'Sign Out Confirmation',
    message: 'Are you sure you want to sign out from your account session?',
    showCancel: true,
    confirmText: 'Sign Out',
    cancelText: 'Cancel',
    onConfirm: () => {
      sessionStorage.removeItem('user')
      notify.success('You have been signed out successfully.', 'Signed Out')
      router.push('/login')
    }
  })
}

/* =========================================================
   INITIAL LOAD
========================================================= */

onMounted(() => {
  loadUser()

  // Payment Methods opens first
  activeTab.value = 'payments'

  fetchSavedCards()
  fetchSavedWallets()
})
</script>

<style scoped>
.page-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background-color: #f8fafc;
  background-image:
    radial-gradient(circle 280px at 0% 18%, rgba(96, 165, 250, 0.30), transparent),
    radial-gradient(circle 320px at 100% 40%, rgba(34, 211, 238, 0.28), transparent),
    linear-gradient(to bottom right,
      rgba(255, 255, 255, 0.45),
      rgba(248, 250, 252, 0.80),
      rgba(239, 246, 255, 0.80)),
    var(--bg-img);
  background-size: 100% 100%, 100% 100%, 100% 100%, cover;
  background-position: 0 0, 0 0, 0 0, center;
  background-repeat: no-repeat;
  transform: translateZ(0);
}

/* Custom Popup Transitions */
.popup-enter-active,
.popup-leave-active {
  transition: opacity 0.25s ease, transform 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.popup-enter-from,
.popup-leave-to {
  opacity: 0;
  transform: scale(0.95) translateY(6px);
}
</style>