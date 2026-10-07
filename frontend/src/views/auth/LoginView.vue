<script setup lang="ts">

import { ref } from 'vue'
import { useRouter } from 'vue-router'

import logo from '../../assets/icons/logoIMG-removebg-preview.svg'
import api from '../../services/api'
import ResponseBanner from '../../components/common/ResponseBanner.vue'


/* =========================================================
   FORM STATE
========================================================= */

const email = ref('')
const password = ref('')

const showPassword = ref(false)

const isLoading = ref(false)

const errorMessage = ref('')

const router = useRouter()


/* =========================================================
   LOGIN
========================================================= */

async function handleLogin() {

  if (
    !email.value.trim() ||
    !password.value
  ) {
    errorMessage.value =
      'Please enter your email and password.'

    return
  }


  isLoading.value = true

  errorMessage.value = ''


  try {

    const response =
      await api.post(
        '/auth/login',
        {
          email:
            email.value
              .trim()
              .toLowerCase(),

          password:
            password.value
        }
      )


    const user =
      response.data


    console.log(
      'LOGIN RESPONSE:',
      user
    )


    /* =====================================================
       VALIDATE RESPONSE
    ===================================================== */

    if (
      !user ||
      !user.userId ||
      !user.role
    ) {

      errorMessage.value =
        'Invalid login response from server.'

      return
    }


    /* =====================================================
       SAVE USER
    ===================================================== */

    const loggedInUser = {

      userId:
        user.userId,

      name:
        user.name,

      email:
        user.email,

      phone:
        user.phone,

      userImage:
        user.userImage,

      address:
        user.address,

      role:
        user.role,

      enabled:
        user.enabled

    }


    sessionStorage.setItem(
      'user',
      JSON.stringify(
        loggedInUser
      )
    )
    localStorage.setItem(
      'user',
      JSON.stringify(
        loggedInUser
      )
    )
    if (loggedInUser.userId) {
      sessionStorage.setItem('userId', loggedInUser.userId)
      localStorage.setItem('userId', loggedInUser.userId)
    }


    const role =
      String(
        user.role
      )
        .trim()
        .toUpperCase()


    /* =====================================================
       STAFF SESSION
    ===================================================== */

    if (
      role !== 'CUSTOMER'
    ) {

      sessionStorage.setItem(
        'staffUser',
        JSON.stringify(
          loggedInUser
        )
      )

      sessionStorage.setItem(
        'userId',
        loggedInUser.userId
      )

      sessionStorage.setItem(
        'staffId',
        loggedInUser.userId
      )

      sessionStorage.setItem(
        'adminId',
        loggedInUser.userId
      )

      sessionStorage.setItem(
        'role',
        role
      )
    }


    sessionStorage.setItem(
      'isLoggedIn',
      'true'
    )


    console.log(
      'LOGGED IN USER:',
      loggedInUser
    )


    /* =====================================================
       ROLE BASED REDIRECT
    ===================================================== */

    switch (role) {

      case 'CUSTOMER':

        router.push('/')

        return


      case 'SUPER_ADMIN':

        router.push(
          '/admin/dashboard'
        )

        return


      case 'ADMINISTRATOR':

        router.push(
          '/admin/dashboard'
        )

        return


      case 'WAREHOUSE_STAFF':

        router.push(
          '/admin/products'
        )

        return


      case 'SUPPORT_STAFF':

        router.push(
          '/admin/customer-support'
        )

        return


      case 'DELIVERY_STAFF':

        router.push(
          '/admin/delivery-management'
        )

        return


      default:

        errorMessage.value =
          'Your account role is not supported.'

        sessionStorage.removeItem(
          'user'
        )

        sessionStorage.removeItem(
          'staffUser'
        )

        sessionStorage.removeItem(
          'userId'
        )

        sessionStorage.removeItem(
          'staffId'
        )

        sessionStorage.removeItem(
          'adminId'
        )

        sessionStorage.removeItem(
          'role'
        )

        sessionStorage.removeItem(
          'isLoggedIn'
        )

    }


  } catch (err: any) {

    console.error(
      'Login error:',
      err
    )


    errorMessage.value =
      err?.response?.data?.message ||
      err?.response?.data?.error ||
      'Invalid email or password. Please try again.'

  } finally {

    isLoading.value = false

  }

}

</script>


<template>

  <!-- =====================================================
       PAGE
  ====================================================== -->

  <div class="relative
           flex
           min-h-screen
           items-center
           justify-center
           overflow-hidden
           bg-slate-50
           px-4
           py-6
           text-slate-900
           sm:px-6">

    <!-- =====================================================
         BACKGROUND
    ====================================================== -->

    <div class="absolute
             inset-0
             bg-cover
             bg-center
             bg-no-repeat" style="
        background-image:
          url('https://images.unsplash.com/photo-1593640408182-31c70c8268f5?auto=format&fit=crop&w=2200&q=85');
      "></div>


    <!-- LIGHT OVERLAY -->

    <div class="absolute
             inset-0
             bg-white/20"></div>


    <!-- SOFT GRADIENT -->

    <div class="absolute
             inset-0
             bg-gradient-to-br
             from-white/75
             via-slate-50/85
             to-blue-50/55"></div>


    <!-- AMBIENT GLOW -->

    <div class="absolute
             -left-40
             -top-40
             h-80
             w-80
             rounded-full
             bg-cyan-400/15
             blur-[110px]"></div>


    <div class="absolute
             -bottom-40
             -right-40
             h-80
             w-80
             rounded-full
             bg-blue-500/15
             blur-[110px]"></div>


    <div class="absolute
             right-1/4
             top-1/3
             h-72
             w-72
             rounded-full
             bg-indigo-400/10
             blur-[120px]"></div>


    <!-- =====================================================
         MAIN CARD
    ====================================================== -->

    <div class="relative
             z-10
             grid
             w-full
             max-w-5xl
             overflow-hidden
             rounded-[1.75rem]
             border
             border-white/90
             bg-white/60
             shadow-2xl
             shadow-slate-400/25
             backdrop-blur-2xl
             lg:grid-cols-2">

      <!-- ===================================================
           LEFT SIDE
      ==================================================== -->

      <div class="relative
               hidden
               min-h-[620px]
               flex-col
               justify-between
               overflow-hidden
               border-r
               border-white/80
               p-8
               lg:flex">

        <!-- GRID -->

        <div class="absolute
                 inset-0
                 opacity-[0.035]" style="
            background-image:
              linear-gradient(rgba(15,23,42,.5) 1px, transparent 1px),
              linear-gradient(90deg, rgba(15,23,42,.5) 1px, transparent 1px);
            background-size: 40px 40px;
          "></div>


        <!-- GLOW -->

        <div class="absolute
                 -left-20
                 top-1/4
                 h-64
                 w-64
                 rounded-full
                 bg-cyan-400/15
                 blur-[90px]"></div>


        <div class="absolute
                 bottom-0
                 right-0
                 h-64
                 w-64
                 rounded-full
                 bg-blue-500/10
                 blur-[90px]"></div>


        <!-- =================================================
             LOGO
        ================================================== -->

        <div class="relative
                 z-10">

          <div class="flex
                   items-center
                   gap-3">

            <div class="flex
                     h-14
                     w-14
                     shrink-0
                     items-center
                     justify-center
                     overflow-hidden">

              <img :src="logo" alt="Nexus Rigs" class="h-14
                       w-14
                       object-contain" />

            </div>


            <div class="leading-none">

              <div class="text-[17px]
                       font-black
                       tracking-[-0.04em]
                       text-slate-950">

                NEXUS<span class="text-blue-600">
                  RIGS
                </span>

              </div>


              <div class="mt-1
                       text-[8px]
                       font-bold
                       uppercase
                       tracking-[0.22em]
                       text-slate-400">
                Performance Hardware
              </div>

            </div>

          </div>

        </div>


        <!-- =================================================
             HERO CONTENT
        ================================================== -->

        <div class="relative
                 z-10
                 max-w-md">

          <!-- BADGE -->

          <div class="inline-flex
                   items-center
                   gap-2
                   rounded-lg
                   border
                   border-blue-100
                   bg-blue-50/80
                   px-2.5
                   py-1.5
                   text-[9px]
                   font-black
                   uppercase
                   tracking-[0.15em]
                   text-blue-700
                   shadow-sm">

            <span class="h-1.5
                     w-1.5
                     shrink-0
                     rounded-full
                     bg-blue-600"></span>

            Premium Hardware Platform

          </div>


          <!-- HERO TITLE -->

          <h1 class="mt-5
                   text-4xl
                   font-black
                   leading-[1.05]
                   tracking-tight
                   text-slate-950
                   xl:text-5xl">

            Build.

            <span class="bg-gradient-to-r
                     from-cyan-500
                     via-blue-600
                     to-indigo-600
                     bg-clip-text
                     text-transparent">
              Upgrade.
            </span>

            Dominate.

          </h1>


          <p class="mt-4
                   max-w-sm
                   text-xs
                   leading-6
                   text-slate-600">
            Access your personalized hardware dashboard,
            manage orders, configure custom builds, and
            discover the latest performance components.
          </p>


          <!-- =================================================
               FEATURES
          ================================================== -->

          <div class="mt-6
                   space-y-3">

            <!-- FEATURE 1 -->

            <div class="flex
                     items-center
                     gap-3">

              <div class="flex
                       h-9
                       w-9
                       shrink-0
                       items-center
                       justify-center
                       rounded-lg
                       border
                       border-blue-100
                       bg-blue-50
                       text-blue-600
                       shadow-sm">

                <!-- CPU -->

                <svg class="h-4
                         w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">

                  <rect x="6" y="6" width="12" height="12" rx="2" />

                  <rect x="9" y="9" width="6" height="6" rx="1" />

                  <path stroke-linecap="round" d="M9 3v3M12 3v3M15 3v3M9 18v3M12 18v3M15 18v3
                       M3 9h3M3 12h3M3 15h3M18 9h3M18 12h3M18 15h3" />

                </svg>

              </div>


              <div>

                <p class="text-xs
                         font-bold
                         text-slate-900">
                  Performance Hardware
                </p>

                <p class="text-[10px]
                         text-slate-500">
                  Latest CPUs, GPUs & components
                </p>

              </div>

            </div>


            <!-- FEATURE 2 -->

            <div class="flex
                     items-center
                     gap-3">

              <div class="flex
                       h-9
                       w-9
                       shrink-0
                       items-center
                       justify-center
                       rounded-lg
                       border
                       border-blue-100
                       bg-blue-50
                       text-blue-600
                       shadow-sm">

                <!-- SHIELD -->

                <svg class="h-4
                         w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">

                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 3l7 3v5c0 4.5-2.8 7.8-7 10
                       -4.2-2.2-7-5.5-7-10V6l7-3z" />

                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4" />

                </svg>

              </div>


              <div>

                <p class="text-xs
                         font-bold
                         text-slate-900">
                  Secure Shopping
                </p>

                <p class="text-[10px]
                         text-slate-500">
                  Protected account & order management
                </p>

              </div>

            </div>


            <!-- FEATURE 3 -->

            <div class="flex
                     items-center
                     gap-3">

              <div class="flex
                       h-9
                       w-9
                       shrink-0
                       items-center
                       justify-center
                       rounded-lg
                       border
                       border-blue-100
                       bg-blue-50
                       text-blue-600
                       shadow-sm">

                <!-- DELIVERY -->

                <svg class="h-4
                         w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">

                  <path stroke-linecap="round" stroke-linejoin="round" d="M3 6h11v11H3z" />

                  <path stroke-linecap="round" stroke-linejoin="round" d="M14 10h4l3 3v4h-7z" />

                  <circle cx="7" cy="19" r="1.5" />

                  <circle cx="18" cy="19" r="1.5" />

                  <path stroke-linecap="round" d="M14 14h7" />

                </svg>

              </div>


              <div>

                <p class="text-xs
                         font-bold
                         text-slate-900">
                  Fast Delivery
                </p>

                <p class="text-[10px]
                         text-slate-500">
                  Track your hardware from checkout to delivery
                </p>

              </div>

            </div>

          </div>

        </div>


        <!-- =================================================
             STATS
        ================================================== -->

        <div class="relative
                 z-10
                 flex
                 items-center
                 gap-6">

          <div>

            <p class="text-xl
                     font-black
                     text-slate-950">
              10K+
            </p>

            <p class="font-mono
                     text-[8px]
                     uppercase
                     tracking-widest
                     text-slate-500">
              Customers
            </p>

          </div>


          <div class="h-7
                   w-px
                   bg-slate-300"></div>


          <div>

            <p class="text-xl
                     font-black
                     text-slate-950">
              500+
            </p>

            <p class="font-mono
                     text-[8px]
                     uppercase
                     tracking-widest
                     text-slate-500">
              Components
            </p>

          </div>


          <div class="h-7
                   w-px
                   bg-slate-300"></div>


          <div>

            <p class="text-xl
                     font-black
                     text-slate-950">
              24/7
            </p>

            <p class="font-mono
                     text-[8px]
                     uppercase
                     tracking-widest
                     text-slate-500">
              Support
            </p>

          </div>

        </div>

      </div>


      <!-- ===================================================
           RIGHT LOGIN
      ==================================================== -->

      <div class="relative
               flex
               items-center
               bg-white/70
               p-6
               backdrop-blur-2xl
               sm:p-8
               lg:p-9">

        <!-- GLOW -->

        <div class="absolute
                 -right-32
                 -top-32
                 h-64
                 w-64
                 rounded-full
                 bg-cyan-400/10
                 blur-[80px]"></div>


        <div class="relative
                 z-10
                 mx-auto
                 w-full
                 max-w-sm">

          <!-- =================================================
               MOBILE LOGO
          ================================================== -->

          <div class="mb-6
                   flex
                   justify-center
                   lg:hidden">

            <router-link to="/" class="inline-flex
                     items-center
                     gap-3">

              <div class="flex
                       h-10
                       w-10
                       items-center
                       justify-center
                       rounded-xl
                       bg-gradient-to-br
                       from-blue-600
                       via-indigo-500
                       to-cyan-400
                       shadow-lg
                       shadow-blue-500/20">

                <svg class="h-5
                         w-5
                         text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">

                  <path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />

                </svg>

              </div>


              <div>

                <span class="block
                         font-black
                         text-slate-950">
                  NEXUS
                </span>

                <span class="block
                         font-mono
                         text-[8px]
                         tracking-[0.3em]
                         text-cyan-600">
                  HARDWARE
                </span>

              </div>

            </router-link>

          </div>


          <!-- =================================================
               HEADER
          ================================================== -->

          <div class="mb-6">



            <h2 class="text-2xl
                     font-black
                     tracking-tight
                     text-slate-950
                     sm:text-3xl">
              Welcome back.
            </h2>


            <p class="mt-1.5
                     text-xs
                     text-slate-500">
              Sign in to continue to your hardware dashboard.
            </p>

          </div>


          <!-- =================================================
               ERROR
          ================================================== -->

          <ResponseBanner v-if="errorMessage" type="error" :message="errorMessage" dismissible class="mb-4" @close="
            errorMessage = ''
            " />


          <!-- =================================================
               FORM
          ================================================== -->

          <form @submit.prevent="handleLogin" class="space-y-4">

            <!-- EMAIL -->

            <div>

              <label class="mb-1.5
                       block
                       text-[9px]
                       font-bold
                       uppercase
                       tracking-widest
                       text-slate-600">
                Email Address
              </label>


              <div class="group
                       relative">

                <div class="pointer-events-none
                         absolute
                         inset-y-0
                         left-0
                         flex
                         items-center
                         pl-3.5
                         text-slate-400
                         transition-colors
                         group-focus-within:text-cyan-500">

                  <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">

                    <path stroke-linecap="round" stroke-linejoin="round"
                      d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />

                  </svg>

                </div>


                <input v-model="email" type="email" required autocomplete="email" placeholder="you@example.com" class="h-12
                         w-full
                         rounded-xl
                         border
                         border-slate-200
                         bg-white/75
                         pl-10
                         pr-4
                         text-xs
                         text-slate-900
                         shadow-sm
                         transition-all
                         placeholder:text-slate-400
                         focus:border-cyan-400
                         focus:bg-white
                         focus:outline-none
                         focus:ring-4
                         focus:ring-cyan-500/10" />

              </div>

            </div>


            <!-- PASSWORD -->

            <div>

              <div class="mb-1.5
                       flex
                       items-center
                       justify-between">

                <label class="text-[9px]
                         font-bold
                         uppercase
                         tracking-widest
                         text-slate-600">
                  Password
                </label>


                <router-link to="/forgot-password" class="text-[9px]
                         font-semibold
                         text-cyan-600
                         hover:text-cyan-700">
                  Forgot password?
                </router-link>

              </div>


              <div class="group
                       relative">

                <div class="pointer-events-none
                         absolute
                         inset-y-0
                         left-0
                         flex
                         items-center
                         pl-3.5
                         text-slate-400
                         transition-colors
                         group-focus-within:text-cyan-500">

                  <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">

                    <path stroke-linecap="round" stroke-linejoin="round"
                      d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />

                  </svg>

                </div>


                <input v-model="password" :type="showPassword
                  ? 'text'
                  : 'password'
                  " required autocomplete="current-password" placeholder="Enter your password" class="h-12
                         w-full
                         rounded-xl
                         border
                         border-slate-200
                         bg-white/75
                         pl-10
                         pr-11
                         text-xs
                         text-slate-900
                         shadow-sm
                         transition-all
                         placeholder:text-slate-400
                         focus:border-cyan-400
                         focus:bg-white
                         focus:outline-none
                         focus:ring-4
                         focus:ring-cyan-500/10" />


                <button type="button" @click="
                  showPassword =
                  !showPassword
                  " class="absolute
                         inset-y-0
                         right-0
                         flex
                         items-center
                         px-3.5
                         text-slate-400
                         hover:text-cyan-500">

                  <!-- HIDDEN -->

                  <svg v-if="!showPassword" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"
                    stroke-width="2">

                    <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />

                    <path stroke-linecap="round" stroke-linejoin="round"
                      d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />

                  </svg>


                  <!-- VISIBLE -->

                  <svg v-else class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">

                    <path stroke-linecap="round" stroke-linejoin="round" d="M3 3l18 18" />

                    <path stroke-linecap="round" stroke-linejoin="round" d="M10.58 10.58a2 2 0 002.84 2.84" />

                  </svg>

                </button>

              </div>

            </div>


            <!-- REMEMBER -->

            <label class="flex
                     cursor-pointer
                     select-none
                     items-center
                     gap-2.5">

              <input type="checkbox" class="h-3.5
                       w-3.5
                       rounded
                       border-slate-300
                       bg-white
                       text-cyan-500
                       focus:ring-cyan-500/30" />

              <span class="text-[10px]
                       text-slate-500">
                Keep me signed in
              </span>

            </label>


            <!-- SUBMIT -->

            <button type="submit" :disabled="isLoading" class="relative
                     h-12
                     w-full
                     overflow-hidden
                     rounded-xl
                     bg-gradient-to-r
                     from-blue-600
                     via-indigo-600
                     to-cyan-500
                     text-xs
                     font-bold
                     text-white
                     shadow-xl
                     shadow-blue-500/15
                     transition-all
                     hover:-translate-y-0.5
                     hover:shadow-cyan-500/30
                     active:translate-y-0
                     disabled:cursor-not-allowed
                     disabled:opacity-50">

              <span class="relative
                       flex
                       items-center
                       justify-center
                       gap-2">

                <!-- LOADING -->

                <svg v-if="isLoading" class="h-4
                         w-4
                         animate-spin" fill="none" viewBox="0 0 24 24">

                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>

                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>

                </svg>


                <span>

                  {{
                    isLoading
                      ? 'Authenticating...'
                      : 'Sign In'
                  }}

                </span>


                <!-- ARROW -->

                <svg v-if="!isLoading" class="h-4
                         w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">

                  <path stroke-linecap="round" stroke-linejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />

                </svg>

              </span>

            </button>

          </form>


          <!-- =================================================
               DIVIDER
          ================================================== -->

          <div class="my-5
                   flex
                   items-center
                   gap-3">

            <div class="h-px
                     flex-1
                     bg-slate-200"></div>


            <span class="font-mono
                     text-[8px]
                     uppercase
                     tracking-widest
                     text-slate-400">
              New here?
            </span>


            <div class="h-px
                     flex-1
                     bg-slate-200"></div>

          </div>


          <!-- REGISTER -->

          <router-link to="/register" class="flex
                   h-10
                   w-full
                   items-center
                   justify-center
                   rounded-xl
                   border
                   border-slate-200
                   bg-white/60
                   text-[10px]
                   font-bold
                   text-slate-600
                   shadow-sm
                   transition-all
                   hover:border-cyan-300
                   hover:bg-white
                   hover:text-slate-900">
            Create Your Account
          </router-link>


          <!-- SECURITY -->

          <div class="mt-4
                   flex
                   items-center
                   justify-center
                   gap-2
                   font-mono
                   text-[8px]
                   uppercase
                   tracking-wider
                   text-slate-400">

            <svg class="h-3
                     w-3
                     text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">

              <path stroke-linecap="round" stroke-linejoin="round"
                d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />

            </svg>

            Secure encrypted authentication

          </div>

        </div>

      </div>

    </div>

  </div>

</template>