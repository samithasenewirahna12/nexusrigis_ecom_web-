<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
    LogIn, ShieldCheck, Users, Clock, Lock, User, Mail, Eye, EyeOff, AlertCircle, CheckCircle2,
    UserPlus, Shield, Warehouse, Headset, Truck, Check, Loader2, RotateCcw
} from 'lucide-vue-next'
import api from '../../../services/api'
import logo from '../../../assets/icons/logoIMG-removebg-preview.svg'

const router = useRouter()

const name = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const role = ref('')
const status = ref('INACTIVE')

const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const roleError = ref('')
const showPassword = ref(false)
const showConfirmPassword = ref(false)

const staffRoles = [
    { value: 'ADMINISTRATOR', label: 'Administrator', description: 'Manage the whole system', icon: Shield },
    { value: 'WAREHOUSE_STAFF', label: 'Warehouse', description: 'Products & warehouse tasks', icon: Warehouse },
    { value: 'SUPPORT_STAFF', label: 'Support', description: 'Handle customer inquiries', icon: Headset },
    { value: 'DELIVERY_STAFF', label: 'Delivery', description: 'Manage assigned deliveries', icon: Truck }
]

const features = [
    { icon: ShieldCheck, title: 'Secure accounts', copy: 'Password-protected access.' },
    { icon: Users, title: 'Role-based access', copy: 'Permissions match the role.' },
    { icon: Clock, title: 'Admin activation', copy: 'New accounts start inactive.' }
]

function selectRole(value: string) {
    role.value = value
    roleError.value = ''
    errorMessage.value = ''
}

async function registerStaff() {
    errorMessage.value = ''
    successMessage.value = ''
    roleError.value = ''

    const cleanName = name.value.trim()
    const cleanEmail = email.value.trim().toLowerCase()

    if (!cleanName) return void (errorMessage.value = 'Please enter the staff member name.')
    if (cleanName.length < 3) return void (errorMessage.value = 'Staff name must contain at least 3 characters.')
    if (!cleanEmail) return void (errorMessage.value = 'Please enter an email address.')
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(cleanEmail)) return void (errorMessage.value = 'Please enter a valid email address.')
    if (!password.value) return void (errorMessage.value = 'Please enter a password.')
    if (password.value.length < 6) return void (errorMessage.value = 'Password must contain at least 6 characters.')
    if (!confirmPassword.value) return void (errorMessage.value = 'Please confirm the password.')
    if (password.value !== confirmPassword.value) return void (errorMessage.value = 'Passwords do not match.')
    if (!role.value) return void (roleError.value = 'Please select a staff role.')

    loading.value = true
    try {
        await api.post('/auth/staff-register', {
            name: cleanName,
            email: cleanEmail,
            password: password.value,
            role: role.value
        })
        successMessage.value = 'Staff account created. It is inactive until an administrator activates it.'
        clearFields()
    } catch (error: any) {
        const data = error?.response?.data
        errorMessage.value =
            data?.message || (typeof data === 'string' ? data : '') || 'Unable to create the staff account. Please try again.'
    } finally {
        loading.value = false
    }
}

function clearFields() {
    name.value = ''
    email.value = ''
    password.value = ''
    confirmPassword.value = ''
    role.value = ''
    status.value = 'INACTIVE'
    showPassword.value = false
    showConfirmPassword.value = false
}

function resetForm() {
    clearFields()
    errorMessage.value = ''
    successMessage.value = ''
    roleError.value = ''
}

const goToLogin = () => router.push('/admin/staff-login')
</script>

<template>
    <!-- Fits one screen on desktop (no page scroll); scrolls normally on small screens -->
    <div class="relative flex min-h-screen flex-col overflow-hidden bg-slate-100 lg:h-screen">
        <div class="pointer-events-none absolute inset-0" aria-hidden="true">
            <div class="absolute -right-32 -top-32 h-72 w-72 rounded-full bg-blue-200/40 blur-3xl" />
            <div class="absolute -bottom-32 -left-32 h-80 w-80 rounded-full bg-cyan-200/40 blur-3xl" />
        </div>

        <!-- HEADER -->
        <header class="relative z-20 h-14 shrink-0 border-b border-slate-200 bg-white/90 backdrop-blur-xl">
            <div class="mx-auto flex h-full max-w-[1450px] items-center justify-between px-4 lg:px-8">
                <div class="flex items-center gap-3">
                    <img :src="logo" alt="NexusRigs" class="h-8 w-auto object-contain" />
                    <div class="hidden border-l border-slate-200 pl-3 sm:block">
                        <p class="text-xs font-semibold text-slate-700">Staff Portal</p>
                        <p class="text-[10px] text-slate-400">Account registration</p>
                    </div>
                </div>
                <button type="button"
                    class="flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
                    @click="goToLogin">
                    <LogIn class="h-4 w-4" /> Staff login
                </button>
            </div>
        </header>

        <main class="relative z-10 mx-auto flex min-h-0 w-full max-w-[1450px] flex-1 flex-col px-4 py-4 lg:px-8">
            <div class="mb-3 shrink-0">
                <h1 class="text-xl font-bold tracking-tight text-slate-900 lg:text-2xl">Create staff account</h1>
                <p class="text-xs text-slate-500">Register a new staff member and assign their system role.</p>
            </div>

            <div class="grid min-h-0 flex-1 gap-4 xl:grid-cols-[270px_minmax(0,1fr)]">
                <!-- INFO PANEL -->
                <aside
                    class="hidden flex-col rounded-2xl bg-gradient-to-br from-blue-600 via-blue-700 to-indigo-700 p-5 text-white shadow-lg shadow-blue-900/10 xl:flex">
                    <span
                        class="mb-4 flex h-10 w-10 items-center justify-center rounded-xl border border-white/20 bg-white/15">
                        <Lock class="h-5 w-5" />
                    </span>
                    <h2 class="text-lg font-bold">Staff information</h2>
                    <p class="mt-1.5 text-xs leading-5 text-blue-100">Create secure accounts with permissions that match
                        each person's responsibilities.</p>
                    <div class="mt-5 space-y-4">
                        <div v-for="f in features" :key="f.title" class="flex gap-3">
                            <span
                                class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-white/10 bg-white/10">
                                <component :is="f.icon" class="h-4 w-4" />
                            </span>
                            <div>
                                <p class="text-xs font-semibold">{{ f.title }}</p>
                                <p class="mt-0.5 text-[11px] text-blue-100">{{ f.copy }}</p>
                            </div>
                        </div>
                    </div>
                    <p class="mt-auto border-t border-white/15 pt-4 text-[11px] leading-4 text-blue-100">Only create
                        accounts for authorised employees of the organisation.</p>
                </aside>

                <!-- FORM -->
                <section
                    class="flex min-h-0 flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-lg shadow-slate-900/5">
                    <div
                        class="flex shrink-0 items-center justify-between border-b border-slate-100 bg-slate-50/70 px-5 py-3">
                        <div>
                            <h2 class="text-sm font-bold text-slate-800">Account details</h2>
                            <p class="text-[11px] text-slate-500">Complete the required information.</p>
                        </div>
                        <span
                            class="hidden items-center gap-1.5 rounded-lg border border-emerald-100 bg-emerald-50 px-2.5 py-1.5 text-[10px] font-semibold text-emerald-700 sm:flex"><span
                                class="h-1.5 w-1.5 rounded-full bg-emerald-500" /> Secure registration</span>
                    </div>

                    <form class="flex min-h-0 flex-1 flex-col" @submit.prevent="registerStaff">
                        <!-- body scrolls only if the window is very short -->
                        <div class="min-h-0 flex-1 space-y-4 overflow-y-auto p-5">
                            <div v-if="errorMessage"
                                class="flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 p-2.5 text-red-700"
                                role="alert">
                                <AlertCircle class="mt-0.5 h-4 w-4 shrink-0" />
                                <p class="text-xs leading-5">{{ errorMessage }}</p>
                            </div>
                            <div v-if="successMessage"
                                class="flex items-start gap-2 rounded-lg border border-emerald-200 bg-emerald-50 p-2.5 text-emerald-700"
                                role="status">
                                <CheckCircle2 class="mt-0.5 h-4 w-4 shrink-0" />
                                <p class="text-xs leading-5">{{ successMessage }}</p>
                            </div>

                            <!-- 4 fields in one compact grid -->
                            <div class="grid gap-3 sm:grid-cols-2">
                                <label class="block">
                                    <span class="lbl">Full name</span>
                                    <span class="relative block">
                                        <User class="ico" />
                                        <input v-model="name" type="text" placeholder="Enter full name"
                                            autocomplete="name" class="inp pl-9" />
                                    </span>
                                </label>
                                <label class="block">
                                    <span class="lbl">Email address</span>
                                    <span class="relative block">
                                        <Mail class="ico" />
                                        <input v-model="email" type="email" placeholder="staff@example.com"
                                            autocomplete="email" class="inp pl-9" />
                                    </span>
                                </label>
                                <label class="block">
                                    <span class="lbl">Password <em class="font-normal not-italic text-slate-400">(min. 6
                                            characters)</em></span>
                                    <span class="relative block">
                                        <Lock class="ico" />
                                        <input v-model="password" :type="showPassword ? 'text' : 'password'"
                                            placeholder="Enter password" autocomplete="new-password" class="inp px-9" />
                                        <button type="button"
                                            :aria-label="showPassword ? 'Hide password' : 'Show password'" class="eye"
                                            @click="showPassword = !showPassword">
                                            <EyeOff v-if="showPassword" class="h-4 w-4" />
                                            <Eye v-else class="h-4 w-4" />
                                        </button>
                                    </span>
                                </label>
                                <label class="block">
                                    <span class="lbl">Confirm password</span>
                                    <span class="relative block">
                                        <Lock class="ico" />
                                        <input v-model="confirmPassword"
                                            :type="showConfirmPassword ? 'text' : 'password'"
                                            placeholder="Confirm password" autocomplete="new-password"
                                            class="inp px-9" />
                                        <button type="button"
                                            :aria-label="showConfirmPassword ? 'Hide password' : 'Show password'"
                                            class="eye" @click="showConfirmPassword = !showConfirmPassword">
                                            <EyeOff v-if="showConfirmPassword" class="h-4 w-4" />
                                            <Eye v-else class="h-4 w-4" />
                                        </button>
                                    </span>
                                </label>
                            </div>

                            <!-- roles -->
                            <div>
                                <div class="mb-2 flex items-center justify-between">
                                    <span class="lbl !mb-0">Staff role</span>
                                    <span v-if="roleError" class="text-[11px] font-medium text-red-500">{{ roleError
                                        }}</span>
                                </div>
                                <div class="grid grid-cols-2 gap-2.5 lg:grid-cols-4">
                                    <button v-for="r in staffRoles" :key="r.value" type="button"
                                        :aria-pressed="role === r.value"
                                        :class="['relative flex items-center gap-2.5 rounded-xl border p-2.5 text-left transition', role === r.value ? 'border-blue-500 bg-blue-50 ring-2 ring-blue-100' : 'border-slate-200 bg-slate-50 hover:border-slate-300 hover:bg-white']"
                                        @click="selectRole(r.value)">
                                        <span
                                            :class="['flex h-9 w-9 shrink-0 items-center justify-center rounded-lg', role === r.value ? 'bg-blue-600 text-white' : 'border border-slate-200 bg-white text-slate-500']">
                                            <component :is="r.icon" class="h-4 w-4" />
                                        </span>
                                        <span class="min-w-0"><span
                                                class="block truncate text-xs font-semibold text-slate-800">{{ r.label
                                                }}</span><span class="block truncate text-[10px] text-slate-500">{{
                                                    r.description }}</span></span>
                                        <Check v-if="role === r.value"
                                            class="absolute right-2 top-2 h-3.5 w-3.5 text-blue-600" />
                                    </button>
                                </div>
                            </div>
                        </div>

                        <!-- footer: status + actions on one row -->
                        <div
                            class="flex shrink-0 flex-col gap-3 border-t border-slate-100 bg-slate-50/70 px-5 py-3 sm:flex-row sm:items-center sm:justify-between">
                            <div
                                class="flex items-center gap-2.5 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2">
                                <Clock class="h-4 w-4 shrink-0 text-amber-600" />
                                <p class="text-[11px] text-amber-800"><strong class="font-bold">Inactive</strong> ·
                                    administrator activation required</p>
                            </div>
                            <div class="flex items-center gap-2.5">
                                <button type="button" :disabled="loading"
                                    class="inline-flex h-10 items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-4 text-xs font-semibold text-slate-600 transition hover:border-slate-300 hover:bg-slate-50 disabled:opacity-50"
                                    @click="resetForm">
                                    <RotateCcw class="h-3.5 w-3.5" /> Reset
                                </button>
                                <button type="submit" :disabled="loading"
                                    class="inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-blue-600 px-5 text-xs font-semibold text-white shadow-sm shadow-blue-600/20 transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60">
                                    <Loader2 v-if="loading" class="h-4 w-4 animate-spin" />
                                    <UserPlus v-else class="h-4 w-4" />
                                    {{ loading ? 'Creating account…' : 'Create account' }}
                                </button>
                            </div>
                        </div>
                    </form>
                </section>
            </div>
        </main>
    </div>
</template>

<style scoped>
.lbl {
    display: block;
    margin-bottom: 0.3rem;
    font-size: 0.75rem;
    font-weight: 500;
    color: rgb(71 85 105);
}

.inp {
    height: 2.5rem;
    width: 100%;
    border-radius: 0.5rem;
    border: 1px solid rgb(203 213 225);
    background: rgb(248 250 252);
    padding-right: 0.75rem;
    font-size: 0.875rem;
    color: rgb(30 41 59);
    outline: none;
    transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
}

.inp::placeholder {
    color: rgb(148 163 184);
}

.inp:focus {
    background: #fff;
    border-color: rgb(59 130 246);
    box-shadow: 0 0 0 3px rgb(59 130 246 / 0.15);
}

.ico {
    pointer-events: none;
    position: absolute;
    left: 0.75rem;
    top: 50%;
    height: 1rem;
    width: 1rem;
    transform: translateY(-50%);
    color: rgb(148 163 184);
}

.eye {
    position: absolute;
    right: 0.4rem;
    top: 50%;
    display: flex;
    height: 1.75rem;
    width: 1.75rem;
    transform: translateY(-50%);
    align-items: center;
    justify-content: center;
    border-radius: 0.375rem;
    color: rgb(148 163 184);
}

.eye:hover {
    background: rgb(241 245 249);
    color: rgb(71 85 105);
}

button {
    -webkit-tap-highlight-color: transparent;
}

button:focus-visible {
    outline: 2px solid rgb(59 130 246 / 0.5);
    outline-offset: 2px;
}
</style>