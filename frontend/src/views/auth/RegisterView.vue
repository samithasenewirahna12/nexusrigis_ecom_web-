<script setup lang="ts">

import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

import api from '../../services/api'

import backgroundImage
  from '../../assets/images/vecteezy_smartwatch-collection-on-display-modern-wearable-technology_70382207.jpg'

import logo
  from '../../assets/icons/logoIMG-removebg-preview.svg'

import ResponseBanner
  from '../../components/common/ResponseBanner.vue'


/* =========================================================
   FORM STATE
========================================================= */

const name = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')

const showPassword = ref(false)
const showConfirmPassword = ref(false)

const isLoading = ref(false)
const errorMessage = ref('')

const router = useRouter()


/* =========================================================
   PASSWORD STRENGTH
========================================================= */

const passwordStrength = computed(() => {

  const value = password.value

  if (!value) {
    return {
      label: 'Enter a password',
      width: '0%',
      color: 'bg-slate-700'
    }
  }

  let score = 0

  if (value.length >= 8) {
    score++
  }

  if (/[A-Z]/.test(value)) {
    score++
  }

  if (/[0-9]/.test(value)) {
    score++
  }

  if (/[^A-Za-z0-9]/.test(value)) {
    score++
  }


  if (score <= 1) {
    return {
      label: 'Weak password',
      width: '25%',
      color: 'bg-red-500'
    }
  }


  if (score === 2) {
    return {
      label: 'Fair password',
      width: '50%',
      color: 'bg-amber-500'
    }
  }


  if (score === 3) {
    return {
      label: 'Good password',
      width: '75%',
      color: 'bg-cyan-500'
    }
  }


  return {
    label: 'Strong password',
    width: '100%',
    color: 'bg-emerald-500'
  }
})


/* =========================================================
   PASSWORD MATCH
========================================================= */

const passwordsMatch = computed(() => {

  if (!confirmPassword.value) {
    return true
  }

  return (
    password.value ===
    confirmPassword.value
  )
})


/* =========================================================
   REGISTER
========================================================= */

async function handleRegister() {

  errorMessage.value = ''


  /* -------------------------------------------------------
     NAME
  ------------------------------------------------------- */

  if (!name.value.trim()) {

    errorMessage.value =
      'Please enter your full name.'

    return
  }


  /* -------------------------------------------------------
     EMAIL
  ------------------------------------------------------- */

  if (!email.value.trim()) {

    errorMessage.value =
      'Please enter your email address.'

    return
  }


  /* -------------------------------------------------------
     EMAIL FORMAT
  ------------------------------------------------------- */

  const emailPattern =
    /^[^\s@]+@[^\s@]+\.[^\s@]+$/

  if (
    !emailPattern.test(
      email.value.trim()
    )
  ) {

    errorMessage.value =
      'Please enter a valid email address.'

    return
  }


  /* -------------------------------------------------------
     PASSWORD
  ------------------------------------------------------- */

  if (password.value.length < 8) {

    errorMessage.value =
      'Password must contain at least 8 characters.'

    return
  }


  /* -------------------------------------------------------
     PASSWORD STRENGTH
  ------------------------------------------------------- */

  if (
    !/[A-Z]/.test(password.value) ||
    !/[0-9]/.test(password.value)
  ) {

    errorMessage.value =
      'Password must contain at least one uppercase letter and one number.'

    return
  }


  /* -------------------------------------------------------
     CONFIRM PASSWORD
  ------------------------------------------------------- */

  if (
    password.value !==
    confirmPassword.value
  ) {

    errorMessage.value =
      'Passwords do not match.'

    return
  }


  isLoading.value = true


  try {

    /* =====================================================
       COMMON REGISTRATION API

       Public registration creates CUSTOMER only.
       ===================================================== */

    const response =
      await api.post(
        '/auth/register',
        {
          name:
            name.value.trim(),

          email:
            email.value
              .trim()
              .toLowerCase(),

          password:
            password.value,

          role:
            'CUSTOMER'
        }
      )


    console.log(
      'REGISTER RESPONSE:',
      response.data
    )


    /*
     * The backend returns the newly-created
     * customer information.
     */

    const registeredUser =
      response.data


    /*
     * =====================================================
     * SAVE USER
     *
     * Do not save the password.
     * =====================================================
     */

    if (
      registeredUser &&
      registeredUser.userId
    ) {

      const user = {

        userId:
          registeredUser.userId,

        name:
          registeredUser.name,

        email:
          registeredUser.email,

        phone:
          registeredUser.phone,

        userImage:
          registeredUser.userImage,

        address:
          registeredUser.address,

        role:
          registeredUser.role,

        enabled:
          registeredUser.enabled

      }


      sessionStorage.setItem(
        'user',
        JSON.stringify(user)
      )


      sessionStorage.setItem(
        'isLoggedIn',
        'true'
      )


      /*
       * Customer registration completed.
       */
      router.push('/')

      return
    }


    /*
     * If your backend does not automatically
     * return the created user, go to login.
     */

    router.push('/login')

  } catch (err: any) {

    console.error(
      'Registration error:',
      err
    )


    errorMessage.value =
      err?.response?.data?.message ||
      err?.response?.data?.error ||
      'Registration failed. Please check your information and try again.'

  } finally {

    isLoading.value = false

  }

}

</script>
<template>
  <div class="relative overflow-hidden bg-slate-50 text-slate-900">

    <!-- Background Image -->
    <div class="absolute inset-0 bg-cover bg-center bg-fixed" :style="{ backgroundImage: `url(${backgroundImage})` }">
    </div>

    <!-- Light Overlay -->
    <div class="absolute inset-0 bg-white/75"></div>

    <!-- Gradient Overlay -->
    <div class="absolute inset-0 bg-gradient-to-br from-white/90 via-slate-50/80 to-blue-50/75"></div>

    <!-- Ambient Glow -->
    <div class="absolute -top-40 -left-40 w-[500px] h-[500px] bg-cyan-400/15 rounded-full blur-[120px]"></div>

    <div class="absolute -bottom-40 -right-40 w-[500px] h-[500px] bg-blue-500/15 rounded-full blur-[120px]"></div>

    <div class="absolute top-1/3 right-1/4 w-[400px] h-[400px] bg-indigo-400/10 rounded-full blur-[120px]"></div>

    <!-- Main Content -->
    <div class="relative z-10 min-h-screen flex items-center justify-center px-4 py-6 sm:px-6 lg:px-8">

      <!-- Main Card -->
      <div
        class="w-full max-w-6xl grid lg:grid-cols-2 rounded-[2rem] overflow-hidden border border-white/90 bg-white/60 backdrop-blur-2xl shadow-2xl shadow-slate-400/25">

        <!-- ===================================== -->
        <!-- LEFT SIDE -->
        <!-- ===================================== -->

        <div
          class="hidden lg:flex relative min-h-[600px] overflow-hidden p-10 flex-col justify-between border-r border-white/80">

          <!-- Background -->
          <div class="absolute inset-0 bg-cover bg-center" :style="{ backgroundImage: `url(${backgroundImage})` }">
          </div>

          <!-- Image Overlay -->
          <div class="absolute inset-0 bg-gradient-to-br from-white/90 via-white/75 to-cyan-50/80"></div>

          <!-- Soft Glow -->
          <div class="absolute top-1/4 -left-20 w-64 h-64 bg-cyan-400/15 blur-[90px] rounded-full"></div>

          <div class="absolute bottom-0 right-0 w-64 h-64 bg-blue-500/10 blur-[90px] rounded-full"></div>

          <!-- Content -->
          <div class="relative z-10">

            <!-- Brand -->

            <!-- Logo -->

            <div class="w-15 h-15 flex items-center justify-center overflow-hidden">
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



            <!-- Main Heading -->
            <div class="mt-14 max-w-lg">

              <span
                class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-cyan-500/10 border border-cyan-500/20 text-cyan-700 text-[10px] font-bold uppercase tracking-[0.2em]">
                <span class="w-1.5 h-1.5 rounded-full bg-cyan-500 animate-pulse"></span>

                Join the community
              </span>

              <h1 class="mt-5 text-5xl xl:text-6xl font-black leading-[1.05] tracking-tight text-slate-950">
                Build.
                <br />

                <span class="text-transparent bg-clip-text bg-gradient-to-r from-cyan-500 via-blue-600 to-indigo-600">
                  Upgrade.
                </span>

                <br />

                Dominate.
              </h1>

              <p class="mt-5 text-slate-600 text-sm leading-6 max-w-md">
                Create your TechStore account and unlock a smarter
                way to discover, compare and order high-performance
                hardware.
              </p>
            </div>

            <!-- Feature Cards -->
            <div class="mt-8 grid grid-cols-2 gap-3">

              <!-- Feature 1 -->
              <div
                class="p-3 rounded-2xl bg-white/65 border border-white/90 backdrop-blur-xl shadow-sm shadow-slate-300/20">

                <div
                  class="w-8 h-8 rounded-xl bg-cyan-500/10 border border-cyan-500/10 flex items-center justify-center text-cyan-600 mb-2">
                  <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                  </svg>
                </div>

                <h3 class="text-xs font-bold text-slate-900">
                  Smart Shopping
                </h3>

                <p class="text-[10px] text-slate-500 mt-1">
                  Personalized hardware discovery.
                </p>

              </div>

              <!-- Feature 2 -->
              <div
                class="p-3 rounded-2xl bg-white/65 border border-white/90 backdrop-blur-xl shadow-sm shadow-slate-300/20">

                <div
                  class="w-8 h-8 rounded-xl bg-blue-500/10 border border-blue-500/10 flex items-center justify-center text-blue-600 mb-2">
                  <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round"
                      d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                  </svg>
                </div>

                <h3 class="text-xs font-bold text-slate-900">
                  Secure Account
                </h3>

                <p class="text-[10px] text-slate-500 mt-1">
                  Your information stays protected.
                </p>

              </div>

            </div>
          </div>

          <!-- Bottom -->
          <div class="relative z-10 flex items-center gap-3">

            <div class="flex -space-x-2">

              <div
                class="w-8 h-8 rounded-full border-2 border-white bg-cyan-500 flex items-center justify-center text-[9px] font-black text-white shadow-sm">
                JD
              </div>

              <div
                class="w-8 h-8 rounded-full border-2 border-white bg-blue-500 flex items-center justify-center text-[9px] font-black text-white shadow-sm">
                AM
              </div>

              <div
                class="w-8 h-8 rounded-full border-2 border-white bg-indigo-500 flex items-center justify-center text-[9px] font-black text-white shadow-sm">
                RK
              </div>

            </div>

            <p class="text-[10px] text-slate-500">
              Join thousands of hardware enthusiasts.
            </p>

          </div>
        </div>

        <!-- ===================================== -->
        <!-- RIGHT SIDE -->
        <!-- ===================================== -->

        <div class="relative p-5 sm:p-7 lg:p-9 flex items-center bg-white/70 backdrop-blur-2xl">

          <!-- Decorative Glow -->
          <div class="absolute top-0 right-0 w-64 h-64 bg-cyan-400/10 blur-[100px] rounded-full pointer-events-none">
          </div>

          <div class="relative z-10 w-full max-w-md mx-auto">

            <!-- Mobile Logo -->
            <div class="lg:hidden text-center mb-6">

              <router-link to="/" class="inline-flex items-center gap-2">

                <div
                  class="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-600 via-indigo-500 to-cyan-400 flex items-center justify-center shadow-lg shadow-blue-500/20">
                  <svg class="w-5 h-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor"
                    stroke-width="2.5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
                  </svg>
                </div>

                <span class="font-black text-slate-950">
                  TECHSTORE
                </span>

              </router-link>

            </div>

            <!-- Header -->
            <div class="mb-5">

              <span class="text-[10px] font-bold uppercase tracking-[0.25em] text-cyan-600">
                Account Registration
              </span>

              <h2 class="mt-2 text-3xl sm:text-4xl font-black tracking-tight text-slate-950">
                Create your account
              </h2>

              <p class="mt-1.5 text-sm text-slate-500">
                Start your hardware journey today.
              </p>

            </div>

            <!-- Error -->
            <ResponseBanner v-if="errorMessage" type="error" :message="errorMessage" dismissible
              @close="errorMessage = ''" class="mb-4" />

            <!-- Form -->
            <form @submit.prevent="handleRegister" class="space-y-3.5">

              <!-- Name -->
              <div>

                <label class="block text-[10px] font-bold uppercase tracking-wider text-slate-600 mb-1.5">
                  Full Name
                </label>

                <div class="relative">

                  <svg class="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" fill="none"
                    viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round"
                      d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7-7h14a7 7 0 00-7 7z" />
                  </svg>

                  <input v-model="name" type="text" required placeholder="John Doe"
                    class="w-full bg-white/75 border border-slate-200 rounded-2xl pl-11 pr-4 py-3 text-sm text-slate-900 placeholder-slate-400 shadow-sm focus:outline-none focus:border-cyan-400 focus:bg-white focus:ring-4 focus:ring-cyan-500/10 transition-all" />

                </div>
              </div>

              <!-- Email -->
              <div>

                <label class="block text-[10px] font-bold uppercase tracking-wider text-slate-600 mb-1.5">
                  Email Address
                </label>

                <div class="relative">

                  <svg class="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" fill="none"
                    viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round"
                      d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                  </svg>

                  <input v-model="email" type="email" required placeholder="name@example.com"
                    class="w-full bg-white/75 border border-slate-200 rounded-2xl pl-11 pr-4 py-3 text-sm text-slate-900 placeholder-slate-400 shadow-sm focus:outline-none focus:border-cyan-400 focus:bg-white focus:ring-4 focus:ring-cyan-500/10 transition-all" />

                </div>
              </div>

              <!-- Password -->
              <div>

                <label class="block text-[10px] font-bold uppercase tracking-wider text-slate-600 mb-1.5">
                  Password
                </label>

                <div class="relative">

                  <svg class="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" fill="none"
                    viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round"
                      d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                  </svg>

                  <input v-model="password" :type="showPassword ? 'text' : 'password'" required placeholder="••••••••"
                    class="w-full bg-white/75 border border-slate-200 rounded-2xl pl-11 pr-12 py-3 text-sm text-slate-900 placeholder-slate-400 shadow-sm focus:outline-none focus:border-cyan-400 focus:bg-white focus:ring-4 focus:ring-cyan-500/10 transition-all" />

                  <button type="button" @click="showPassword = !showPassword"
                    class="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 hover:text-cyan-500 transition">
                    <svg v-if="!showPassword" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"
                      stroke-width="2">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />

                      <path stroke-linecap="round" stroke-linejoin="round"
                        d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>

                    <svg v-else class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M3 3l18 18" />

                      <path stroke-linecap="round" stroke-linejoin="round" d="M10.58 10.58a2 2 0 002.84 2.84" />

                      <path stroke-linecap="round" stroke-linejoin="round"
                        d="M9.88 5.09A9.94 9.94 0 0112 5c4.48 0 8.27 2.94 9.54 7a10.06 10.06 0 01-2.15 3.54" />

                      <path stroke-linecap="round" stroke-linejoin="round"
                        d="M6.61 6.61A10.06 10.06 0 002.46 12c1.27 4.06 5.06 7 9.54 7 1.4 0 2.73-.29 3.93-.81" />
                    </svg>
                  </button>

                </div>

                <!-- Password Strength -->
                <div class="mt-2">

                  <div class="h-1 bg-slate-200 rounded-full overflow-hidden">
                    <div :class="[
                      'h-full transition-all duration-300',
                      passwordStrength.color
                    ]" :style="{ width: passwordStrength.width }"></div>
                  </div>

                  <div class="flex justify-between mt-1 text-[9px] text-slate-400">
                    <span>{{ passwordStrength.label }}</span>

                    <span>
                      {{ password.length }}/8+
                    </span>
                  </div>

                </div>
              </div>

              <!-- Confirm Password -->
              <div>

                <label class="block text-[10px] font-bold uppercase tracking-wider text-slate-600 mb-1.5">
                  Confirm Password
                </label>

                <div class="relative">

                  <svg class="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" fill="none"
                    viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round"
                      d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                  </svg>

                  <input v-model="confirmPassword" :type="showConfirmPassword ? 'text' : 'password'" required
                    placeholder="••••••••" :class="[
                      'w-full bg-white/75 border rounded-2xl pl-11 pr-12 py-3 text-sm text-slate-900 placeholder-slate-400 shadow-sm focus:outline-none focus:ring-4 transition-all',
                      passwordsMatch
                        ? 'border-slate-200 focus:border-cyan-400 focus:ring-cyan-500/10'
                        : 'border-red-400 focus:border-red-500 focus:ring-red-500/10'
                    ]" />

                  <button type="button" @click="showConfirmPassword = !showConfirmPassword"
                    class="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 hover:text-cyan-500 transition">
                    <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />

                      <path stroke-linecap="round" stroke-linejoin="round"
                        d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7" />
                    </svg>
                  </button>

                </div>

                <p v-if="!passwordsMatch" class="mt-1.5 text-[10px] text-red-500">
                  Passwords do not match.
                </p>

              </div>

              <!-- Terms -->
              <div class="flex items-start gap-3 pt-0.5">

                <input type="checkbox" required
                  class="mt-0.5 w-4 h-4 rounded border-slate-300 bg-white text-cyan-500 focus:ring-cyan-500/30" />

                <p class="text-[10px] leading-4 text-slate-500">
                  I agree to the

                  <span class="text-cyan-600 hover:underline cursor-pointer">
                    Terms of Service
                  </span>

                  and

                  <span class="text-cyan-600 hover:underline cursor-pointer">
                    Privacy Policy
                  </span>.
                </p>

              </div>

              <!-- Submit -->
              <button type="submit" :disabled="isLoading"
                class="group relative w-full overflow-hidden rounded-2xl bg-gradient-to-r from-blue-600 via-indigo-600 to-cyan-500 py-3 font-bold text-sm text-white shadow-xl shadow-blue-500/20 hover:shadow-cyan-500/40 hover:-translate-y-0.5 active:translate-y-0 transition-all disabled:opacity-50 disabled:cursor-not-allowed">

                <div
                  class="absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent -translate-x-full group-hover:translate-x-full transition-transform duration-700">
                </div>

                <span class="relative flex items-center justify-center gap-2">

                  <svg v-if="isLoading" class="animate-spin w-5 h-5" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />

                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                  </svg>

                  <span>
                    {{
                      isLoading
                        ? 'Creating Account...'
                        : 'Create Account'
                    }}
                  </span>

                  <svg v-if="!isLoading" class="w-4 h-4 group-hover:translate-x-1 transition-transform" fill="none"
                    viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />
                  </svg>

                </span>

              </button>

            </form>

            <!-- Login -->
            <div class="mt-5 pt-4 border-t border-slate-200 text-center">
              <p class="text-xs text-slate-500">
                Already have an account?

                <router-link to="/login" class="ml-1 text-cyan-600 font-bold hover:text-cyan-700 transition">
                  Sign in
                </router-link>
              </p>
            </div>

            <!-- Security -->
            <div class="mt-3 flex items-center justify-center gap-2 text-[9px] text-slate-400">
              <svg class="w-3.5 h-3.5 text-emerald-500" fill="none" viewBox="0 0 24 24" stroke="currentColor"
                stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
              </svg>

              Your account information is securely protected.
            </div>

          </div>
        </div>

      </div>
    </div>
  </div>
</template>