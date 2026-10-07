<script setup lang="ts">
import { ref, computed } from 'vue'
import PrimaryButton from '../../components/common/PrimaryButton.vue'
import backgroundImage from '../../assets/images/vecteezy_smartwatch-collection-on-display-modern-wearable-technology_70382207.jpg'

interface ComponentOption {
  id: string
  name: string
  price: number
  watts: number
  perfScore: number
}

interface Category {
  key: string
  label: string
  options: ComponentOption[]
}

const targetResolution = ref<'1080p' | '1440p' | '4k'>('1440p')
const buildTier = ref<'budget' | 'balanced' | 'enthusiast'>('balanced')
const assemblyFeeOption = ref<'diy' | 'pro'>('pro')

const profileTiers = [
  'budget',
  'balanced',
  'enthusiast'
] as const

const resolutions = ['1080p', '1440p', '4k'] as const

/* =========================================================
   COMPONENT DATA
   ========================================================= */

const categories: Category[] = [
  {
    key: 'cpu',
    label: 'Processor',
    options: [
      {
        id: 'cpu-1',
        name: 'Intel Core i5-13400F / AMD Ryzen 5 7600',
        price: 68000,
        watts: 65,
        perfScore: 58
      },
      {
        id: 'cpu-2',
        name: 'Intel Core i7-14700K / AMD Ryzen 7 7800X3D',
        price: 135000,
        watts: 125,
        perfScore: 88
      },
      {
        id: 'cpu-3',
        name: 'Intel Core i9-14900K / AMD Ryzen 9 7950X3D',
        price: 195000,
        watts: 253,
        perfScore: 98
      }
    ]
  },

  {
    key: 'gpu',
    label: 'Graphics Card',
    options: [
      {
        id: 'gpu-1',
        name: 'NVIDIA RTX 4060 8GB / AMD RX 7600',
        price: 105000,
        watts: 115,
        perfScore: 48
      },
      {
        id: 'gpu-2',
        name: 'NVIDIA RTX 4070 Super 12GB / AMD RX 7800 XT',
        price: 195000,
        watts: 220,
        perfScore: 78
      },
      {
        id: 'gpu-3',
        name: 'NVIDIA RTX 4080 Super 16GB / AMD RX 7900 XTX',
        price: 325000,
        watts: 320,
        perfScore: 92
      },
      {
        id: 'gpu-4',
        name: 'NVIDIA RTX 4090 24GB Flagship',
        price: 575000,
        watts: 450,
        perfScore: 100
      }
    ]
  },

  {
    key: 'ram',
    label: 'Memory',
    options: [
      {
        id: 'ram-1',
        name: '16GB (2x8GB) DDR5 5600MHz',
        price: 22000,
        watts: 10,
        perfScore: 45
      },
      {
        id: 'ram-2',
        name: '32GB (2x16GB) DDR5 6000MHz CL30',
        price: 39000,
        watts: 15,
        perfScore: 80
      },
      {
        id: 'ram-3',
        name: '64GB (2x32GB) DDR5 6400MHz',
        price: 72000,
        watts: 20,
        perfScore: 96
      }
    ]
  },

  {
    key: 'storage',
    label: 'Storage',
    options: [
      {
        id: 'storage-1',
        name: '1TB PCIe Gen4 NVMe SSD',
        price: 26000,
        watts: 7,
        perfScore: 65
      },
      {
        id: 'storage-2',
        name: '2TB PCIe Gen4 NVMe SSD (7000MB/s)',
        price: 46000,
        watts: 8,
        perfScore: 88
      },
      {
        id: 'storage-3',
        name: '4TB PCIe Gen5 NVMe SSD',
        price: 98000,
        watts: 12,
        perfScore: 99
      }
    ]
  },

  {
    key: 'motherboard',
    label: 'Motherboard',
    options: [
      {
        id: 'mb-1',
        name: 'B650 / B760 WiFi',
        price: 48000,
        watts: 30,
        perfScore: 62
      },
      {
        id: 'mb-2',
        name: 'X670E / Z790 Gaming',
        price: 92000,
        watts: 45,
        perfScore: 86
      },
      {
        id: 'mb-3',
        name: 'Z790 / X670E ROG Flagship',
        price: 165000,
        watts: 60,
        perfScore: 98
      }
    ]
  },

  {
    key: 'power',
    label: 'Power Supply',
    options: [
      {
        id: 'psu-1',
        name: '650W 80+ Bronze Semi-Modular',
        price: 26000,
        watts: 0,
        perfScore: 50
      },
      {
        id: 'psu-2',
        name: '850W 80+ Gold ATX 3.0 Fully Modular',
        price: 48000,
        watts: 0,
        perfScore: 82
      },
      {
        id: 'psu-3',
        name: '1000W 80+ Titanium ATX 3.0 Modular',
        price: 78000,
        watts: 0,
        perfScore: 96
      }
    ]
  },

  {
    key: 'cooling',
    label: 'Cooling & Case',
    options: [
      {
        id: 'cool-1',
        name: 'Air cooler + mid-tower airflow case',
        price: 32000,
        watts: 15,
        perfScore: 55
      },
      {
        id: 'cool-2',
        name: '240mm AIO + ARGB glass case',
        price: 58000,
        watts: 25,
        perfScore: 82
      },
      {
        id: 'cool-3',
        name: '360mm LCD AIO + dual-chamber case',
        price: 95000,
        watts: 35,
        perfScore: 98
      }
    ]
  }
]

/* =========================================================
   BUILD PRESETS
   ========================================================= */

const tiers = {
  budget: {
    cpu: 'cpu-1',
    gpu: 'gpu-1',
    ram: 'ram-1',
    storage: 'storage-1',
    motherboard: 'mb-1',
    power: 'psu-1',
    cooling: 'cool-1'
  },

  balanced: {
    cpu: 'cpu-2',
    gpu: 'gpu-2',
    ram: 'ram-2',
    storage: 'storage-2',
    motherboard: 'mb-1',
    power: 'psu-2',
    cooling: 'cool-2'
  },

  enthusiast: {
    cpu: 'cpu-3',
    gpu: 'gpu-3',
    ram: 'ram-3',
    storage: 'storage-3',
    motherboard: 'mb-3',
    power: 'psu-3',
    cooling: 'cool-3'
  }
} as const

const selections = ref<Record<string, string>>({
  ...tiers.balanced
})

function presetTier(tier: keyof typeof tiers) {
  buildTier.value = tier

  selections.value = {
    ...tiers[tier]
  }
}

/* =========================================================
   SELECTED ITEMS
   ========================================================= */

const selectedItems = computed(() =>
  categories.flatMap(category => {
    const option = category.options.find(
      item =>
        item.id === selections.value[category.key]
    )

    return option
      ? [
        {
          cat: category.label,
          key: category.key,
          ...option
        }
      ]
      : []
  })
)

/* =========================================================
   PRICE
   ========================================================= */

const baseHardwarePrice = computed(() =>
  selectedItems.value.reduce(
    (sum, item) => sum + item.price,
    0
  )
)

const assemblyFee = computed(() =>
  assemblyFeeOption.value === 'pro'
    ? 29000
    : 0
)

const totalPrice = computed(
  () =>
    baseHardwarePrice.value +
    assemblyFee.value
)

const estimatedPriceMin = computed(() =>
  Math.round(totalPrice.value * 0.95)
)

const estimatedPriceMax = computed(() =>
  Math.round(totalPrice.value * 1.05)
)

/* =========================================================
   POWER
   ========================================================= */

const totalPowerDrawWatts = computed(
  () =>
    selectedItems.value.reduce(
      (sum, item) => sum + item.watts,
      0
    ) + 25
)

const recommendedPsuWattage = computed(
  () =>
    Math.ceil(
      (totalPowerDrawWatts.value * 1.35) / 50
    ) * 50
)

const selectedPsuCapacity = computed(() => {
  const power = selections.value.power

  if (power === 'psu-1') {
    return 650
  }

  if (power === 'psu-2') {
    return 850
  }

  return 1000
})

const isPowerAdequate = computed(
  () =>
    selectedPsuCapacity.value >=
    totalPowerDrawWatts.value * 1.15
)

const powerPercent = computed(() =>
  Math.min(
    (totalPowerDrawWatts.value /
      selectedPsuCapacity.value) *
    100,
    100
  )
)

/* =========================================================
   PERFORMANCE
   ========================================================= */

const systemPerformanceScore = computed(() => {
  if (!selectedItems.value.length) {
    return 0
  }

  return Math.round(
    selectedItems.value.reduce(
      (sum, item) => sum + item.perfScore,
      0
    ) / selectedItems.value.length
  )
})

const estimatedFps = computed(() => {
  const gpu =
    selectedItems.value.find(item =>
      item.id.startsWith('gpu')
    )?.perfScore ?? 50

  const cpu =
    selectedItems.value.find(item =>
      item.id.startsWith('cpu')
    )?.perfScore ?? 50

  let fps =
    gpu * 1.7 +
    cpu * 0.4

  if (targetResolution.value === '1080p') {
    fps *= 1.35
  }

  if (targetResolution.value === '4k') {
    fps *= 0.58
  }

  return Math.round(fps)
})

/* =========================================================
   HELPERS
   ========================================================= */

const money = (value: number) =>
  `LKR ${value.toLocaleString('en-LK')}`

const tierDescription = computed(() => {
  if (buildTier.value === 'budget') {
    return 'Essential performance for everyday gaming'
  }

  if (buildTier.value === 'enthusiast') {
    return 'High-end hardware for maximum performance'
  }

  return 'Balanced hardware for gaming and productivity'
})
</script>

<template>
  <div class="relative min-h-screen overflow-x-hidden bg-slate-100 text-slate-900">

    <!-- =====================================================
         BACKGROUND
         ===================================================== -->

    <div class="absolute inset-0 -z-10 bg-cover bg-center" :style="{
      backgroundImage: `url(${backgroundImage})`
    }"></div>

    <div class="absolute inset-0 -z-10 bg-white/75"></div>

    <div class="absolute inset-0 -z-10 bg-gradient-to-br from-white/75 via-slate-50/70 to-blue-50/60"></div>

    <div class="pointer-events-none absolute -left-24 top-24 -z-10 h-64 w-64 rounded-full bg-cyan-200/20"></div>

    <div class="pointer-events-none absolute -right-24 bottom-24 -z-10 h-72 w-72 rounded-full bg-blue-200/20"></div>

    <!-- =====================================================
         FULL WIDTH
         ===================================================== -->

    <main class="relative z-10 w-full px-4 py-6 sm:px-6 lg:px-8 lg:py-8">

      <!-- =================================================
           HEADER
           ================================================= -->

      <header class="mb-6">

        <div
          class="rounded-[2rem] border border-white/90 bg-white/70 p-5 shadow-lg shadow-slate-300/20 backdrop-blur-xl sm:p-6">

          <div class="flex flex-col gap-6 xl:flex-row xl:items-center xl:justify-between">

            <!-- TITLE -->

            <div class="max-w-3xl">

              <div class="inline-flex items-center gap-2 rounded-full border border-blue-100 bg-blue-50 px-3 py-1.5">

                <span class="h-1.5 w-1.5 rounded-full bg-blue-600"></span>

                <span class="text-[10px] font-black uppercase tracking-[0.18em] text-blue-700">
                  NexusRigs PC Configurator
                </span>

              </div>

              <h1 class="mt-4 text-3xl font-black tracking-tight text-slate-950 sm:text-4xl lg:text-5xl">
                Build your perfect
                <span class="text-blue-600">
                  PC
                </span>
              </h1>

              <p class="mt-3 max-w-2xl text-sm leading-6 text-slate-500">
                Select your components and instantly
                compare your estimated price, performance
                and power requirements.
              </p>

            </div>

            <!-- PROFILE SELECTOR -->

            <div class="w-full rounded-2xl border border-white/90 bg-white/85 p-1.5 shadow-sm xl:w-auto">

              <div class="grid grid-cols-3 gap-1">

                <button v-for="tier in profileTiers" :key="tier" type="button" @click="presetTier(tier)"
                  class="min-w-[100px] rounded-xl px-4 py-3 text-left transition-colors duration-200" :class="buildTier === tier
                    ? 'bg-blue-600 text-white shadow-md shadow-blue-600/20'
                    : 'text-slate-500 hover:bg-blue-50 hover:text-slate-800'
                    ">

                  <span class="block text-xs font-black capitalize">
                    {{ tier }}
                  </span>

                  <span class="mt-1 block text-[9px]" :class="buildTier === tier
                    ? 'text-blue-100'
                    : 'text-slate-400'
                    ">
                    {{
                      tier === 'budget'
                        ? 'Essential'
                        : tier === 'balanced'
                          ? 'Recommended'
                          : 'Maximum'
                    }}
                  </span>

                </button>

              </div>
            </div>
          </div>

          <!-- STATUS -->

          <div
            class="mt-5 flex flex-col gap-3 rounded-2xl border border-white/90 bg-white/70 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">

            <div class="flex items-center gap-3">

              <div class="flex h-9 w-9 items-center justify-center rounded-xl bg-blue-50 text-blue-600">

                <svg viewBox="0 0 24 24" fill="none" class="h-4 w-4" stroke="currentColor" stroke-width="1.8">
                  <path d="M12 3l7 4v5c0 4.5-2.8 7.7-7 9-4.2-1.3-7-4.5-7-9V7l7-4z" />
                  <path d="M9 12l2 2 4-4" />
                </svg>

              </div>

              <div>

                <div class="text-xs font-black text-slate-800">
                  {{
                    buildTier.charAt(0).toUpperCase() +
                    buildTier.slice(1)
                  }}
                  build
                </div>

                <div class="mt-0.5 text-[10px] text-slate-400">
                  {{ tierDescription }}
                </div>

              </div>

            </div>

            <div class="flex flex-wrap items-center gap-3 text-[10px] text-slate-400">

              <span class="rounded-lg bg-white px-2.5 py-1.5 shadow-sm">
                {{ selectedItems.length }} components
              </span>

              <span class="rounded-lg bg-white px-2.5 py-1.5 shadow-sm">
                {{ targetResolution }} target
              </span>

            </div>

          </div>
        </div>
      </header>

      <!-- =================================================
           MAIN GRID
           ================================================= -->

      <div class="grid w-full items-start gap-6 xl:grid-cols-[minmax(0,1fr)_390px]">

        <!-- =================================================
             CONFIGURATION
             ================================================= -->

        <div class="min-w-0 space-y-5">

          <!-- COMPONENT SECTIONS -->

          <section v-for="cat in categories" :key="cat.key"
            class="overflow-hidden rounded-[1.5rem] border border-white/90 bg-white/65 shadow-lg shadow-slate-300/15 backdrop-blur-xl"
            style="
              content-visibility: auto;
              contain-intrinsic-size: 500px;
            ">

            <!-- CATEGORY HEADER -->

            <div class="flex items-center justify-between border-b border-white/90 px-5 py-4">

              <div class="flex items-center gap-3">

                <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-50 text-blue-600">

                  <!-- CPU -->

                  <svg v-if="cat.key === 'cpu'" viewBox="0 0 24 24" fill="none" class="h-5 w-5" stroke="currentColor"
                    stroke-width="1.7">
                    <rect x="6" y="6" width="12" height="12" rx="2" />
                    <path d="M9 2v4M15 2v4M9 18v4M15 18v4M18 9h4M18 15h4M2 9h4M2 15h4" />
                  </svg>

                  <!-- GPU -->

                  <svg v-else-if="cat.key === 'gpu'" viewBox="0 0 24 24" fill="none" class="h-5 w-5"
                    stroke="currentColor" stroke-width="1.7">
                    <rect x="3" y="6" width="18" height="12" rx="2" />
                    <circle cx="9" cy="12" r="2.5" />
                    <path d="M16 9h2M16 12h2M16 15h2" />
                  </svg>

                  <!-- RAM -->

                  <svg v-else-if="cat.key === 'ram'" viewBox="0 0 24 24" fill="none" class="h-5 w-5"
                    stroke="currentColor" stroke-width="1.7">
                    <rect x="3" y="7" width="18" height="10" rx="2" />
                    <path d="M7 7v10M11 7v10M15 7v10M19 7v10M6 19h2M10 19h2M14 19h2" />
                  </svg>

                  <!-- Storage -->

                  <svg v-else-if="cat.key === 'storage'" viewBox="0 0 24 24" fill="none" class="h-5 w-5"
                    stroke="currentColor" stroke-width="1.7">
                    <rect x="4" y="4" width="16" height="16" rx="2" />
                    <path d="M8 8h8M8 12h8M8 16h4" />
                  </svg>

                  <!-- Motherboard -->

                  <svg v-else-if="cat.key === 'motherboard'" viewBox="0 0 24 24" fill="none" class="h-5 w-5"
                    stroke="currentColor" stroke-width="1.7">
                    <rect x="5" y="3" width="14" height="18" rx="2" />
                    <path d="M8 7h3v3H8zM13 7h3v3h-3zM8 13h8v3H8z" />
                  </svg>

                  <!-- Power -->

                  <svg v-else-if="cat.key === 'power'" viewBox="0 0 24 24" fill="none" class="h-5 w-5"
                    stroke="currentColor" stroke-width="1.7">
                    <path d="M13 2L5 13h6l-1 9 8-11h-6l1-9z" />
                  </svg>

                  <!-- Cooling -->

                  <svg v-else viewBox="0 0 24 24" fill="none" class="h-5 w-5" stroke="currentColor" stroke-width="1.7">
                    <path d="M4 12a8 8 0 111.8 5" />
                    <path d="M4 16v-4h4" />
                    <path d="M12 8v4l3 2" />
                  </svg>

                </div>

                <div>

                  <h2 class="text-sm font-black text-slate-900">
                    {{ cat.label }}
                  </h2>

                  <p class="mt-0.5 text-[10px] text-slate-400">
                    Choose your preferred configuration
                  </p>

                </div>

              </div>

              <span class="rounded-full border border-blue-100 bg-blue-50 px-3 py-1 text-[9px] font-bold text-blue-600">
                {{ cat.options.length }} options
              </span>

            </div>

            <!-- OPTIONS -->

            <div class="grid gap-3 p-4 sm:grid-cols-2" :class="cat.options.length === 3
              ? 'lg:grid-cols-3'
              : 'lg:grid-cols-2'
              ">

              <button v-for="option in cat.options" :key="option.id" type="button" @click="
                selections[cat.key] =
                option.id
                "
                class="group relative overflow-hidden rounded-2xl border p-4 text-left transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-blue-300"
                :class="selections[cat.key] === option.id
                  ? 'border-blue-300 bg-blue-50/80 shadow-md shadow-blue-500/10'
                  : 'border-white/90 bg-white/65 hover:border-blue-200 hover:bg-white/85 hover:shadow-sm'
                  ">

                <!-- Selected accent -->

                <div v-if="
                  selections[cat.key] ===
                  option.id
                " class="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-blue-600 to-cyan-500"></div>

                <div>

                  <!-- TOP -->

                  <div class="flex items-start justify-between gap-3">

                    <div class="flex min-w-0 items-start gap-3">

                      <!-- RADIO -->

                      <div class="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full border-2"
                        :class="selections[cat.key] ===
                          option.id
                          ? 'border-blue-600 bg-blue-600'
                          : 'border-slate-300 bg-white'
                          ">

                        <div v-if="
                          selections[cat.key] ===
                          option.id
                        " class="h-1.5 w-1.5 rounded-full bg-white"></div>

                      </div>

                      <div class="min-w-0">

                        <div class="text-xs font-black leading-5 text-slate-900">
                          {{ option.name }}
                        </div>

                        <div class="mt-2 flex flex-wrap gap-1.5">

                          <span v-if="option.watts"
                            class="rounded-md bg-slate-100 px-2 py-1 text-[9px] font-semibold text-slate-500">
                            {{ option.watts }}W
                          </span>

                          <span class="rounded-md bg-slate-100 px-2 py-1 text-[9px] font-semibold text-slate-500">
                            Score {{ option.perfScore }}
                          </span>

                        </div>

                      </div>

                    </div>

                    <!-- PRICE -->

                    <span class="shrink-0 text-sm font-black text-slate-900">
                      {{ money(option.price) }}
                    </span>

                  </div>

                  <!-- PERFORMANCE -->

                  <div class="mt-4">

                    <div class="flex items-center justify-between">

                      <span class="text-[9px] font-bold uppercase tracking-[0.14em] text-slate-400">
                        Performance
                      </span>

                      <span class="text-[9px] font-black" :class="selections[cat.key] ===
                        option.id
                        ? 'text-blue-600'
                        : 'text-slate-400'
                        ">
                        {{ option.perfScore }}/100
                      </span>

                    </div>

                    <div class="mt-2 h-1.5 overflow-hidden rounded-full bg-slate-200">

                      <div class="h-full rounded-full transition-all duration-300" :class="selections[cat.key] ===
                        option.id
                        ? 'bg-gradient-to-r from-blue-600 to-cyan-500'
                        : 'bg-slate-300'
                        " :style="{
                          width:
                            `${option.perfScore}%`
                        }"></div>

                    </div>

                    <div class="mt-3 flex items-center justify-between">

                      <span class="text-[9px] text-slate-400">
                        {{
                          option.watts
                            ? `${option.watts}W typical usage`
                            : 'Power capacity component'
                        }}
                      </span>

                      <span class="text-[9px] font-black uppercase tracking-[0.12em]" :class="selections[cat.key] ===
                        option.id
                        ? 'text-blue-600'
                        : 'text-slate-400'
                        ">
                        {{
                          selections[cat.key] ===
                            option.id
                            ? 'Selected'
                            : 'Choose'
                        }}
                      </span>

                    </div>

                  </div>
                </div>
              </button>

            </div>
          </section>

          <!-- =================================================
               RESOLUTION
               ================================================= -->

          <section
            class="rounded-[1.5rem] border border-white/90 bg-white/65 p-5 shadow-lg shadow-slate-300/15 backdrop-blur-xl">

            <div class="flex items-center justify-between">

              <div>

                <p class="text-[9px] font-black uppercase tracking-[0.16em] text-blue-600">
                  Gaming setup
                </p>

                <h2 class="mt-1 text-sm font-black text-slate-900">
                  Gaming resolution
                </h2>

                <p class="mt-1 text-[10px] text-slate-400">
                  Select your target resolution
                </p>

              </div>

              <span
                class="rounded-full border border-blue-100 bg-blue-50 px-3 py-1 text-[9px] font-black uppercase tracking-wider text-blue-600">
                {{ targetResolution }}
              </span>

            </div>

            <div class="mt-4 grid grid-cols-3 gap-2">

              <button v-for="resolution in ['1080p', '1440p', '4k']" :key="resolution" type="button" @click="
                targetResolution =
                resolution
                " class="rounded-xl border px-4 py-3 text-left transition-all duration-200" :class="targetResolution ===
                    resolution
                    ? 'border-blue-300 bg-blue-50 shadow-sm'
                    : 'border-white/90 bg-white/70 hover:border-blue-200 hover:bg-white'
                    ">

                <div class="flex items-center justify-between">

                  <span class="text-xs font-black" :class="targetResolution ===
                    resolution
                    ? 'text-blue-700'
                    : 'text-slate-700'
                    ">
                    {{ resolution }}
                  </span>

                  <span class="flex h-4 w-4 items-center justify-center rounded-full border" :class="targetResolution ===
                    resolution
                    ? 'border-blue-600 bg-blue-600'
                    : 'border-slate-300 bg-white'
                    ">

                    <span v-if="
                      targetResolution ===
                      resolution
                    " class="h-1.5 w-1.5 rounded-full bg-white"></span>

                  </span>

                </div>

                <span class="mt-2 block text-[9px] text-slate-400">
                  {{
                    resolution === '1080p'
                      ? 'Full HD • High FPS'
                      : resolution === '1440p'
                        ? 'QHD • Balanced'
                        : 'Ultra HD • Maximum'
                  }}
                </span>

              </button>

            </div>
          </section>

          <!-- =================================================
               ASSEMBLY
               ================================================= -->

          <section
            class="rounded-[1.5rem] border border-white/90 bg-white/65 p-5 shadow-lg shadow-slate-300/15 backdrop-blur-xl">

            <div>

              <p class="text-[9px] font-black uppercase tracking-[0.16em] text-blue-600">
                Service
              </p>

              <h2 class="mt-1 text-sm font-black text-slate-900">
                Assembly service
              </h2>

              <p class="mt-1 text-[10px] text-slate-400">
                Choose how you want your system prepared
              </p>

            </div>

            <div class="mt-4 grid gap-3 sm:grid-cols-2">

              <!-- DIY -->

              <button type="button" @click="assemblyFeeOption = 'diy'"
                class="rounded-2xl border p-4 text-left transition-all duration-200" :class="assemblyFeeOption === 'diy'
                  ? 'border-blue-300 bg-blue-50 shadow-sm'
                  : 'border-white/90 bg-white/70 hover:border-blue-200 hover:bg-white'">

                <div class="flex items-center justify-between gap-3">

                  <div class="flex items-center gap-3">

                    <div class="flex h-9 w-9 items-center justify-center rounded-xl" :class="assemblyFeeOption === 'diy'
                      ? 'bg-blue-600 text-white'
                      : 'bg-slate-100 text-slate-500'">

                      <svg viewBox="0 0 24 24" fill="none" class="h-4 w-4" stroke="currentColor" stroke-width="1.8">
                        <path d="M14.7 6.3a4 4 0 01-5 5L4 17l3 3 5.7-5.7a4 4 0 005-5l-2 2-2-2 2-3z" />
                      </svg>

                    </div>

                    <div>

                      <div class="text-xs font-black text-slate-800">
                        Build it yourself
                      </div>

                      <div class="mt-0.5 text-[10px] text-slate-400">
                        DIY assembly
                      </div>

                    </div>

                  </div>

                  <span class="text-xs font-black" :class="assemblyFeeOption === 'diy'
                    ? 'text-blue-600'
                    : 'text-slate-500'">
                    Free
                  </span>

                </div>
              </button>

              <!-- PROFESSIONAL -->

              <button type="button" @click="assemblyFeeOption = 'pro'"
                class="rounded-2xl border p-4 text-left transition-all duration-200" :class="assemblyFeeOption === 'pro'
                  ? 'border-blue-300 bg-blue-50 shadow-sm'
                  : 'border-white/90 bg-white/70 hover:border-blue-200 hover:bg-white'">

                <div class="flex items-center justify-between gap-3">

                  <div class="flex items-center gap-3">

                    <div class="flex h-9 w-9 items-center justify-center rounded-xl" :class="assemblyFeeOption === 'pro'
                      ? 'bg-blue-600 text-white'
                      : 'bg-slate-100 text-slate-500'">

                      <svg viewBox="0 0 24 24" fill="none" class="h-4 w-4" stroke="currentColor" stroke-width="1.8">
                        <path d="M12 3l7 4v5c0 4.5-2.8 7.7-7 9-4.2-1.3-7-4.5-7-9V7l7-4z" />
                        <path d="M9 12l2 2 4-4" />
                      </svg>

                    </div>

                    <div>

                      <div class="text-xs font-black text-slate-800">
                        Professional assembly
                      </div>

                      <div class="mt-0.5 text-[10px] text-slate-400">
                        Built and tested
                      </div>

                    </div>

                  </div>

                  <span class="text-xs font-black" :class="assemblyFeeOption === 'pro'
                    ? 'text-blue-600'
                    : 'text-slate-500'">
                    LKR 29,000
                  </span>

                </div>
              </button>

            </div>
          </section>

        </div>

        <!-- =================================================
             SUMMARY
             ================================================= -->

        <aside class="xl:sticky xl:top-6 xl:self-start">

          <div
            class="overflow-hidden rounded-[1.75rem] border border-white/90 bg-white/85 shadow-xl shadow-slate-300/20 backdrop-blur-xl">

            <!-- BLUE ACCENT -->

            <div class="h-1 bg-gradient-to-r from-blue-600 via-cyan-500 to-blue-400"></div>

            <!-- SUMMARY HEADER -->

            <div class="border-b border-slate-100 px-6 py-6">

              <div class="flex items-start justify-between">

                <div>

                  <p class="text-[9px] font-black uppercase tracking-[0.18em] text-blue-600">
                    Build Summary
                  </p>

                  <div class="mt-2 text-3xl font-black tracking-tight text-slate-950 sm:text-4xl">
                    {{ money(totalPrice) }}
                  </div>

                  <p class="mt-2 text-[10px] text-slate-400">
                    Estimated range

                    <span class="font-bold text-slate-600">
                      {{ money(estimatedPriceMin) }}
                      –
                      {{ money(estimatedPriceMax) }}
                    </span>
                  </p>

                </div>

                <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-50 text-blue-600">

                  <svg viewBox="0 0 24 24" fill="none" class="h-5 w-5" stroke="currentColor" stroke-width="1.7">
                    <rect x="5" y="5" width="14" height="14" rx="2" />
                    <path d="M9 2v3M15 2v3M9 19v3M15 19v3M19 9h3M19 15h3M2 9h3M2 15h3" />
                  </svg>

                </div>

              </div>
            </div>

            <!-- STATS -->

            <div class="grid grid-cols-2 border-b border-slate-100">

              <div class="px-5 py-4">

                <p class="text-[9px] font-black uppercase tracking-wider text-slate-400">
                  Estimated FPS
                </p>

                <p class="mt-1 text-2xl font-black text-slate-950">
                  {{ estimatedFps }}
                </p>

                <p class="text-[10px] text-slate-400">
                  at {{ targetResolution }}
                </p>

              </div>

              <div class="border-l border-slate-100 px-5 py-4">

                <p class="text-[9px] font-black uppercase tracking-wider text-slate-400">
                  Performance
                </p>

                <p class="mt-1 text-2xl font-black text-slate-950">
                  {{ systemPerformanceScore }}
                </p>

                <p class="text-[10px] text-slate-400">
                  out of 100
                </p>

              </div>

            </div>

            <!-- POWER -->

            <div class="p-5">

              <div class="rounded-2xl border border-white/90 bg-slate-50 p-4">

                <div class="flex items-center justify-between">

                  <div>

                    <p class="text-[9px] font-black uppercase tracking-wider text-slate-400">
                      Power usage
                    </p>

                    <p class="mt-1 text-xs font-black text-slate-800">
                      {{ totalPowerDrawWatts }}W

                      <span class="font-normal text-slate-400">
                        /
                        {{ selectedPsuCapacity }}W
                      </span>
                    </p>

                  </div>

                  <span class="rounded-full px-2.5 py-1 text-[8px] font-black" :class="isPowerAdequate
                    ? 'bg-emerald-50 text-emerald-600'
                    : 'bg-red-50 text-red-600'
                    ">
                    {{
                      isPowerAdequate
                        ? 'SUFFICIENT'
                        : 'UPGRADE'
                    }}
                  </span>

                </div>

                <div class="mt-3 h-1.5 overflow-hidden rounded-full bg-slate-200">

                  <div class="h-full rounded-full transition-all duration-300" :class="isPowerAdequate
                    ? 'bg-gradient-to-r from-blue-600 to-cyan-500'
                    : 'bg-red-500'
                    " :style="{
                      width:
                        `${powerPercent}%`
                    }"></div>

                </div>

                <div class="mt-2 flex justify-between text-[9px]">

                  <span class="text-slate-400">
                    {{ Math.round(powerPercent) }}% load
                  </span>

                  <span :class="isPowerAdequate
                    ? 'text-slate-400'
                    : 'font-black text-red-500'
                    ">
                    {{
                      isPowerAdequate
                        ? 'Healthy headroom'
                        : `${recommendedPsuWattage}W recommended`
                    }}
                  </span>

                </div>

              </div>
            </div>

            <!-- COMPONENTS -->

            <div class="px-5 pb-5">

              <div class="mb-3 flex items-center justify-between">

                <p class="text-xs font-black text-slate-900">
                  Selected components
                </p>

                <span class="rounded-full bg-slate-100 px-2 py-1 text-[9px] font-bold text-slate-400">
                  {{ selectedItems.length }} items
                </span>

              </div>

              <div class="max-h-[300px] overflow-y-auto rounded-2xl border border-slate-100 bg-slate-50">

                <div v-for="item in selectedItems" :key="item.id"
                  class="flex items-center justify-between gap-3 border-b border-slate-100 px-3.5 py-3 last:border-0">

                  <div class="min-w-0">

                    <p class="text-[8px] font-black uppercase tracking-wider text-blue-600">
                      {{ item.cat }}
                    </p>

                    <p class="mt-0.5 truncate text-[10px] font-semibold text-slate-600">
                      {{ item.name }}
                    </p>

                  </div>

                  <span class="shrink-0 text-[10px] font-black text-slate-800">
                    {{ money(item.price) }}
                  </span>

                </div>

                <div class="flex items-center justify-between border-t border-slate-100 bg-white px-3.5 py-3">

                  <span class="text-[10px] text-slate-400">
                    Assembly
                  </span>

                  <span class="text-[10px] font-black text-slate-800">
                    {{
                      assemblyFee
                        ? money(assemblyFee)
                        : 'Free'
                    }}
                  </span>

                </div>

              </div>
            </div>

            <!-- ACTIONS -->

            <div class="border-t border-slate-100 bg-slate-50/80 p-5">

              <PrimaryButton variant="cyan" size="lg" class="w-full">
                Order this build
              </PrimaryButton>

              <PrimaryButton variant="slate" size="md" class="mt-2.5 w-full">
                Save quote
              </PrimaryButton>

              <div class="mt-3 flex items-center justify-center gap-1.5 text-center text-[9px] text-slate-400">

                <svg viewBox="0 0 24 24" fill="none" class="h-3 w-3" stroke="currentColor" stroke-width="1.8">
                  <path d="M12 3l7 4v5c0 4.5-2.8 7.7-7 9-4.2-1.3-7-4.5-7-9V7l7-4z" />
                  <path d="M9 12l2 2 4-4" />
                </svg>

                Live configuration and pricing

              </div>

            </div>
          </div>
        </aside>

      </div>
    </main>
  </div>
</template>