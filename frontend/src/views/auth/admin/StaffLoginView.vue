<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Mail, Lock, Eye, EyeOff, AlertCircle, LogIn, Loader2, ShieldCheck, Users, Truck, ArrowLeft, UserPlus } from 'lucide-vue-next'
import api from '../../../services/api'
import logo from '../../../assets/icons/logoIMG-removebg-preview.svg'

const router = useRouter()

const email = ref('')
const password = ref('')
const loading = ref(false)
const errorMessage = ref('')
const showPassword = ref(false)

const features = [
    { icon: ShieldCheck, title: 'Secure access', copy: 'Protected staff-only sign in.' },
    { icon: Users, title: 'Role-based portal', copy: 'Admin, warehouse, support and delivery.' },
    { icon: Truck, title: 'Live operations', copy: 'Orders, stock and deliveries in one place.' }
]

const handleLogin = async () => {
    errorMessage.value = ''

    if (!email.value.trim()) {
        errorMessage.value = 'Please enter your email address.'
        return
    }
    if (!password.value) {
        errorMessage.value = 'Please enter your password.'
        return
    }

    loading.value = true

    try {
        const response = await api.post('/auth/staff-login', {
            email: email.value.trim(),
            password: password.value
        })

        const user = response.data

        // Save logged-in staff information
        sessionStorage.setItem('user', JSON.stringify(user))
        sessionStorage.setItem('staffUser', JSON.stringify(user))
        sessionStorage.setItem('userId', user.userId)
        sessionStorage.setItem('staffId', user.userId)
        sessionStorage.setItem('adminId', user.userId)
        sessionStorage.setItem('role', user.role)
        window.dispatchEvent(new Event('user-profile-updated'))

        // Redirect according to staff role
        const routes: Record<string, string> = {
            ADMINISTRATOR: '/admin/dashboard',
            WAREHOUSE_STAFF: '/admin/products',
            SUPPORT_STAFF: '/admin/customer-support',
            DELIVERY_STAFF: '/admin/delivery-management'
        }

        if (routes[user.role]) {
            router.push(routes[user.role])
        } else {
            errorMessage.value = 'Your account does not have a valid staff role.'
        }
    } catch (error: any) {
        console.error('Staff login error:', error)

        if (error.response) {
            if (error.response.status === 401) errorMessage.value = 'Invalid email or password.'
            else if (error.response.data?.message) errorMessage.value = error.response.data.message
            else errorMessage.value = 'Login failed. Please try again.'
        } else {
            errorMessage.value = 'Unable to connect to the server.'
        }
    } finally {
        loading.value = false
    }
}

const goToCustomerLogin = () => router.push('/admin/staff-login')
const goToRegister = () => router.push('/admin/staff-register')
</script>

<template>
    <div class="relative flex min-h-screen items-center justify-center overflow-hidden bg-slate-100 px-4 py-8">
        <!-- background -->
        <div class="pointer-events-none absolute inset-0" aria-hidden="true">
            <div class="absolute -right-32 -top-32 h-96 w-96 rounded-full bg-blue-300/30 blur-3xl" />
            <div class="absolute -bottom-32 -left-32 h-96 w-96 rounded-full bg-cyan-300/30 blur-3xl" />
            <div
                class="absolute left-1/2 top-1/2 h-96 w-96 -translate-x-1/2 -translate-y-1/2 rounded-full bg-indigo-200/20 blur-3xl" />
            <div class="absolute inset-0 opacity-[0.35]"
                style="background-image: radial-gradient(rgb(148 163 184 / 0.35) 1px, transparent 1px); background-size: 24px 24px" />
        </div>

        <div
            class="relative z-10 grid w-full max-w-4xl overflow-hidden rounded-3xl border border-white/70 bg-white shadow-2xl shadow-slate-900/10 md:grid-cols-[1fr_1.05fr]">
            <!-- BRAND PANEL -->
            <aside
                class="relative hidden flex-col justify-between overflow-hidden bg-gradient-to-br from-blue-600 via-blue-700 to-indigo-800 p-8 text-white md:flex">
                <div
                    class="pointer-events-none absolute -bottom-20 -right-20 h-64 w-64 rounded-full bg-white/10 blur-2xl" />
                <div
                    class="pointer-events-none absolute -left-16 -top-16 h-48 w-48 rounded-full bg-cyan-300/20 blur-2xl" />

                <div class="relative">
                    <div class="flex items-center gap-3">
                        <span
                            class="flex h-11 w-11 items-center justify-center rounded-xl bg-white p-1.5 shadow-lg"><img
                                :src="logo" alt="NexusRigs logo" class="h-full w-full object-contain" /></span>
                        <div class="leading-none">
                            <p class="text-lg font-black tracking-tight">NEXUS<span class="text-cyan-300">RIGS</span>
                            </p>
                            <p class="mt-1 text-[9px] font-bold uppercase tracking-[0.2em] text-blue-200">Performance
                                Hardware</p>
                        </div>
                    </div>

                    <h2 class="mt-10 text-2xl font-extrabold leading-tight tracking-tight">Staff management<br />portal
                    </h2>
                    <p class="mt-2 max-w-xs text-sm leading-6 text-blue-100">Sign in to manage orders, inventory,
                        support and deliveries.</p>
                </div>

                <ul class="relative mt-8 space-y-4">
                    <li v-for="f in features" :key="f.title" class="flex gap-3">
                        <span
                            class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-white/15 bg-white/10">
                            <component :is="f.icon" class="h-4 w-4" />
                        </span>
                        <div>
                            <p class="text-sm font-semibold">{{ f.title }}</p>
                            <p class="text-xs text-blue-100">{{ f.copy }}</p>
                        </div>
                    </li>
                </ul>
            </aside>

            <!-- FORM -->
            <section class="p-6 sm:p-10">
                <!-- mobile brand -->
                <div class="mb-6 flex items-center gap-3 md:hidden">
                    <img :src="logo" alt="NexusRigs logo" class="h-10 w-10 object-contain" />
                    <p class="text-lg font-black tracking-tight text-slate-950">NEXUS<span
                            class="text-blue-600">RIGS</span></p>
                </div>

                <span
                    class="inline-flex items-center gap-1.5 rounded-full bg-blue-50 px-3 py-1 text-[11px] font-bold text-blue-700 ring-1 ring-blue-100">
                    <ShieldCheck class="h-3.5 w-3.5" /> Authorised staff only
                </span>
                <h1 class="mt-4 text-2xl font-extrabold tracking-tight text-slate-900 sm:text-3xl">Welcome back</h1>
                <p class="mt-1.5 text-sm text-slate-500">Sign in to access the NexusRigs management system.</p>

                <div v-if="errorMessage"
                    class="mt-5 flex items-start gap-2.5 rounded-xl border border-red-200 bg-red-50 px-3.5 py-3 text-red-700"
                    role="alert">
                    <AlertCircle class="mt-0.5 h-4 w-4 shrink-0" />
                    <p class="text-sm leading-5">{{ errorMessage }}</p>
                </div>

                <form class="mt-6 space-y-4" @submit.prevent="handleLogin">
                    <div>
                        <label for="email" class="mb-1.5 block text-sm font-medium text-slate-700">Email address</label>
                        <div class="relative">
                            <Mail
                                class="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                            <input id="email" v-model.trim="email" type="email" placeholder="staff@example.com"
                                autocomplete="email" required class="field pl-10" />
                        </div>
                    </div>

                    <div>
                        <label for="password" class="mb-1.5 block text-sm font-medium text-slate-700">Password</label>
                        <div class="relative">
                            <Lock
                                class="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                            <input id="password" v-model="password" :type="showPassword ? 'text' : 'password'"
                                placeholder="Enter your password" autocomplete="current-password" required
                                class="field px-10" />
                            <button type="button" :aria-label="showPassword ? 'Hide password' : 'Show password'"
                                class="absolute right-2 top-1/2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-lg text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
                                @click="showPassword = !showPassword">
                                <EyeOff v-if="showPassword" class="h-4 w-4" />
                                <Eye v-else class="h-4 w-4" />
                            </button>
                        </div>
                    </div>

                    <button type="submit" :disabled="loading"
                        class="flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 text-sm font-bold text-white shadow-lg shadow-blue-600/25 transition hover:-translate-y-0.5 hover:from-blue-700 hover:to-indigo-700 disabled:translate-y-0 disabled:cursor-not-allowed disabled:opacity-60">
                        <Loader2 v-if="loading" class="h-4 w-4 animate-spin" />
                        <LogIn v-else class="h-4 w-4" />
                        {{ loading ? 'Signing in…' : 'Sign in' }}
                    </button>
                </form>

                <div class="my-5 flex items-center gap-3 text-xs text-slate-400"><span
                        class="h-px flex-1 bg-slate-200" />New staff member?<span class="h-px flex-1 bg-slate-200" />
                </div>
                <button type="button"
                    class="flex h-12 w-full items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white text-sm font-bold text-slate-700 transition hover:-translate-y-0.5 hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700"
                    @click="goToRegister">
                    <UserPlus class="h-4 w-4" /> Create staff account
                </button>

                <div class="mt-5 flex items-center justify-between border-t border-slate-100 pt-5">
                    <button type="button"
                        class="inline-flex items-center gap-1.5 text-sm font-semibold text-blue-600 transition hover:text-blue-800"
                        @click="goToCustomerLogin">
                        <ArrowLeft class="h-4 w-4" /> Customer login
                    </button>
                    <span class="text-xs text-slate-400">Secured connection</span>
                </div>
            </section>
        </div>
    </div>
</template>

<style scoped>
.field {
    height: 3rem;
    width: 100%;
    border-radius: 0.75rem;
    border: 1px solid rgb(203 213 225);
    background: rgb(248 250 252);
    padding-right: 1rem;
    font-size: 0.875rem;
    color: rgb(30 41 59);
    outline: none;
    transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
}

.field::placeholder {
    color: rgb(148 163 184);
}

.field:focus {
    background: #fff;
    border-color: rgb(59 130 246);
    box-shadow: 0 0 0 4px rgb(59 130 246 / 0.12);
}

button:focus-visible {
    outline: 2px solid rgb(59 130 246 / 0.5);
    outline-offset: 2px;
}

@media (prefers-reduced-motion: reduce) {
    * {
        transition-duration: 0.01ms !important;
    }
}
</style>