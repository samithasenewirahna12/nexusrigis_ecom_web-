<script setup lang="ts">
import AdminSidebar from '../../components/admin/AdminSidebar.vue'


import { ref, computed, onMounted } from 'vue'
import axios from 'axios'

import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  BarElement,
  CategoryScale,
  LinearScale,
  LineElement,
  PointElement,
  ArcElement,
  Filler
} from 'chart.js'
import { Line, Bar, Doughnut } from 'vue-chartjs'

ChartJS.register(
  Title,
  Tooltip,
  Legend,
  BarElement,
  CategoryScale,
  LinearScale,
  LineElement,
  PointElement,
  ArcElement,
  Filler
)

interface StatMetric {
  title: string
  value: string
  change: string
  isPositive: boolean
  icon: string
}

interface InventoryItem {
  id: number
  name: string
  sku: string
  stock: number
  price: number
  category: string
  status: 'In Stock' | 'Low Stock' | 'Out of Stock'
}

interface ActivityLog {
  id: string
  action: string
  user: string
  timestamp: string
  type: 'order' | 'user' | 'inventory' | 'system'
}

const activeTab = ref<'overview' | 'inventory' | 'activity'>('overview')
const isLoading = ref(true)

// Chart Data State
const revenueChartData = ref<any>({
  labels: [],
  datasets: []
})
const orderStatusChartData = ref<any>({
  labels: [],
  datasets: []
})
const inventoryChartData = ref<any>({
  labels: [],
  datasets: []
})

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false }
  },
  scales: {
    y: { beginAtZero: true, grid: { color: 'rgba(0,0,0,0.05)' } },
    x: { grid: { display: false } }
  }
}

const doughnutOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { position: 'right' as const }
  }
}

// Metrics
const metrics = ref<StatMetric[]>([
  {
    title: 'Total Revenue',
    value: 'LKR 128,450.00',
    change: '+14.2%',
    isPositive: true,
    icon: 'currency'
  },
  {
    title: 'Total Orders',
    value: '1,420',
    change: '+8.1%',
    isPositive: true,
    icon: 'cart'
  },
  {
    title: 'Active Users',
    value: '3,890',
    change: '+22.5%',
    isPositive: true,
    icon: 'users'
  },
  {
    title: 'Pending RMAs',
    value: '12',
    change: '-3.4%',
    isPositive: false,
    icon: 'returns'
  }
])

// Inventory Data
const inventory = ref<InventoryItem[]>([])
const searchQuery = ref('')
const selectedCategory = ref('All')

// Activity Logs
const activityLogs = ref<ActivityLog[]>([])

// The backend has no /api/admin/* endpoints — inventory, activity and the
// top metrics are all derived from the real /api/products, /api/orders and
// /api/customers endpoints instead. Field names below (stock, category,
// totalAmount, status, createdAt, etc.) are best guesses based on the
// controllers; adjust them to match your actual DTOs if they differ.
function resolveCategory(product: any): string {
  if (product?.category && typeof product.category === 'object') {
    return product.category.name ?? 'Uncategorized'
  }
  return product?.category ?? product?.categoryName ?? 'Uncategorized'
}

function resolveStockStatus(stock: number): InventoryItem['status'] {
  if (stock <= 0) return 'Out of Stock'
  if (stock < 10) return 'Low Stock'
  return 'In Stock'
}

onMounted(async () => {
  try {
    const [productsRes, ordersRes, customersRes] = await Promise.all([
      axios.get('/api/products'),
      axios.get('/api/orders'),
      axios.get('/api/customers')
    ])

    const products = productsRes.data ?? []
    const orders = ordersRes.data ?? []
    const customers = customersRes.data ?? []

    // Inventory table <- /api/products
    inventory.value = products.map((p: any) => {
      const stock = p.stockQty ?? p.stock ?? p.quantity ?? 0
      return {
        id: p.id ?? p.productId,
        name: p.name,
        sku: p.sku ?? '—',
        stock,
        price: p.price ?? 0,
        category: resolveCategory(p),
        status: resolveStockStatus(stock)
      }
    })

    // Activity log <- most recent /api/orders (no dedicated audit-log endpoint exists)
    activityLogs.value = [...orders]
      .sort((a: any, b: any) => {
        const aTime = new Date(a.createdAt ?? a.orderDate ?? 0).getTime()
        const bTime = new Date(b.createdAt ?? b.orderDate ?? 0).getTime()
        return bTime - aTime
      })
      .slice(0, 10)
      .map((o: any) => ({
        id: `ORD-${o.id}`,
        action: `Order #${o.id} — status: ${o.status}`,
        user: o.customerEmail ?? o.customerId ?? 'unknown',
        timestamp: o.createdAt ?? o.orderDate ?? '',
        type: 'order' as const
      }))

    // Top metric cards, derived from the same three responses
    const totalRevenue = orders.reduce(
      (sum: number, o: any) => sum + (o.totalAmount ?? o.total ?? 0),
      0
    )
    const pendingOrders = orders.filter((o: any) => o.status === 'PENDING').length

    metrics.value[0].value = `LKR ${totalRevenue.toFixed(2)}`
    metrics.value[1].value = String(orders.length)
    metrics.value[2].value = String(customers.length)
    metrics.value[3].title = 'Pending Orders' // repurposed: no RMA data source exists
    metrics.value[3].value = String(pendingOrders)
    // % change figures have no historical data to compare against, so they're left as-is

    // Chart Generation Logic
    // 1. Revenue over last 7 days
    const last7Days = Array.from({length: 7}, (_, i) => {
      const d = new Date();
      d.setDate(d.getDate() - i);
      return d.toISOString().split('T')[0];
    }).reverse();

    const revenueMap: Record<string, number> = {};
    last7Days.forEach(d => revenueMap[d] = 0);

    orders.forEach((o: any) => {
      const dateStr = (o.orderDate || o.createdAt || '').slice(0, 10);
      if (revenueMap[dateStr] !== undefined) {
        const amount = Number(o.totalAmount || o.total || o.amount || 0);
        revenueMap[dateStr] += amount;
      }
    });

    revenueChartData.value = {
      labels: last7Days,
      datasets: [
        {
          label: 'Revenue (LKR)',
          data: last7Days.map(d => revenueMap[d]),
          borderColor: '#2563eb',
          backgroundColor: 'rgba(37, 99, 235, 0.1)',
          fill: true,
          tension: 0.4
        }
      ]
    };

    // 2. Order Statuses
    const statusCounts: Record<string, number> = { 'Pending': 0, 'Processing': 0, 'In Transit': 0, 'Delivered': 0, 'Cancelled': 0, 'Returned': 0 };
    orders.forEach((o: any) => {
      let st = o.status || 'Pending';
      st = st.charAt(0).toUpperCase() + st.slice(1).toLowerCase();
      if (statusCounts[st] !== undefined) statusCounts[st]++;
      else statusCounts['Pending']++;
    });

    orderStatusChartData.value = {
      labels: Object.keys(statusCounts),
      datasets: [
        {
          data: Object.values(statusCounts),
          backgroundColor: ['#f59e0b', '#3b82f6', '#8b5cf6', '#10b981', '#ef4444', '#64748b']
        }
      ]
    };

    // 3. Inventory by Category
    const catCounts: Record<string, number> = {};
    inventory.value.forEach(item => {
      if (!catCounts[item.category]) catCounts[item.category] = 0;
      catCounts[item.category] += item.stock;
    });

    const sortedCats = Object.keys(catCounts).sort((a,b) => catCounts[b] - catCounts[a]).slice(0, 5);
    inventoryChartData.value = {
      labels: sortedCats,
      datasets: [
        {
          label: 'Total Stock',
          data: sortedCats.map(c => catCounts[c]),
          backgroundColor: '#06b6d4',
          borderRadius: 4
        }
      ]
    };
  } catch (err) {
    console.error('Failed to fetch admin dashboard data:', err)

    // Fallback mock data for testing
    inventory.value = [
      {
        id: 1,
        name: 'RTX 4080 Super 16GB',
        sku: 'GPU-4080S-16G',
        stock: 7,
        price: 1199.00,
        category: 'Graphics Cards',
        status: 'Low Stock'
      },
      {
        id: 2,
        name: 'Core i9 Ultra 285K',
        sku: 'CPU-INT-285K',
        stock: 24,
        price: 589.99,
        category: 'Processors',
        status: 'In Stock'
      },
      {
        id: 3,
        name: '32GB DDR5 RAM Kit',
        sku: 'RAM-D5-32G',
        stock: 0,
        price: 149.99,
        category: 'Memory',
        status: 'Out of Stock'
      },
      {
        id: 4,
        name: '1TB NVMe PCIe 4.0 SSD',
        sku: 'SSD-NVME-1TB',
        stock: 42,
        price: 99.99,
        category: 'Storage',
        status: 'In Stock'
      }
    ]

    activityLogs.value = [
      {
        id: 'LOG-101',
        action: 'New Order #ORD-948201 placed',
        user: 'samitha@example.com',
        timestamp: '10 mins ago',
        type: 'order'
      },
      {
        id: 'LOG-102',
        action: 'RMA request #RMA-402 approved',
        user: 'admin_tech',
        timestamp: '1 hour ago',
        type: 'inventory'
      },
      {
        id: 'LOG-103',
        action: 'User registered via OAuth',
        user: 'johndoe@gmail.com',
        timestamp: '2 hours ago',
        type: 'user'
      },
      {
        id: 'LOG-104',
        action: 'Database backup synchronized',
        user: 'SYSTEM',
        timestamp: '5 hours ago',
        type: 'system'
      }
    ]
  } finally {
    isLoading.value = false
  }
})

const categories = computed(() => {
  const unique = Array.from(new Set(inventory.value.map((item) => item.category)))
  return ['All', ...unique]
})

const filteredInventory = computed(() => {
  return inventory.value.filter((item) => {
    const query = searchQuery.value.toLowerCase()

    const matchesSearch =
      item.name.toLowerCase().includes(query) ||
      item.sku.toLowerCase().includes(query)

    const matchesCategory =
      selectedCategory.value === 'All' ||
      item.category === selectedCategory.value

    return matchesSearch && matchesCategory
  })
})

function getStockBadgeStyle(status: InventoryItem['status']) {
  switch (status) {
    case 'In Stock':
      return 'bg-emerald-50 border-emerald-200 text-emerald-600'

    case 'Low Stock':
      return 'bg-amber-50 border-amber-200 text-amber-600'

    case 'Out of Stock':
      return 'bg-red-50 border-red-200 text-red-600'

    default:
      return 'bg-slate-100 border-slate-200 text-slate-500'
  }
}


</script>

<template>
  <div class="min-h-screen bg-slate-100">
    <AdminSidebar />

    <main class="ml-64 min-h-screen">
      <div class="relative min-h-screen overflow-hidden bg-slate-100 text-slate-900">
        <!-- Background -->
        <div class="fixed inset-0 pointer-events-none bg-gradient-to-br from-white via-slate-50 to-blue-50/70"></div>

        <div
          class="fixed -top-40 -right-40 w-[500px] h-[500px] rounded-full bg-cyan-400/10 blur-3xl pointer-events-none">
        </div>

        <div
          class="fixed -bottom-40 -left-40 w-[500px] h-[500px] rounded-full bg-blue-500/10 blur-3xl pointer-events-none">
        </div>

        <!-- Main Content -->
        <div class="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-10">
          <!-- Admin Header -->


          <!-- Page Header -->
          <div
            class="mb-8 border-b border-slate-200/80 pb-6 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div>
              <div class="flex items-center gap-3 flex-wrap">
                <span
                  class="px-2.5 py-1 rounded-md bg-blue-50 border border-blue-200 text-blue-600 text-[10px] font-bold tracking-widest uppercase">
                  Admin Panel
                </span>

                <h2 class="text-2xl sm:text-3xl font-black text-slate-950 tracking-tight">
                  Dashboard Overview
                </h2>
              </div>

              <p class="text-slate-500 text-sm mt-2">
                Manage system operations, track real-time inventory,
                and audit activity logs
              </p>
            </div>

            <!-- Control Tabs -->
            <div
              class="flex items-center bg-white/70 backdrop-blur-xl border border-white/90 p-1 rounded-2xl gap-1 shadow-lg shadow-slate-200/20">
              <button @click="activeTab = 'overview'" :class="[
                'px-4 py-2 rounded-xl text-xs font-bold transition-all',
                activeTab === 'overview'
                  ? 'bg-blue-50 border border-blue-200 text-blue-600 shadow-sm'
                  : 'text-slate-500 hover:text-slate-900 hover:bg-slate-50'
              ]">
                Analytics
              </button>

              <button @click="activeTab = 'inventory'" :class="[
                'px-4 py-2 rounded-xl text-xs font-bold transition-all',
                activeTab === 'inventory'
                  ? 'bg-blue-50 border border-blue-200 text-blue-600 shadow-sm'
                  : 'text-slate-500 hover:text-slate-900 hover:bg-slate-50'
              ]">
                Inventory
              </button>

              <button @click="activeTab = 'activity'" :class="[
                'px-4 py-2 rounded-xl text-xs font-bold transition-all',
                activeTab === 'activity'
                  ? 'bg-blue-50 border border-blue-200 text-blue-600 shadow-sm'
                  : 'text-slate-500 hover:text-slate-900 hover:bg-slate-50'
              ]">
                Audit Logs
              </button>
            </div>
          </div>

          <!-- Loading -->
          <div v-if="isLoading" class="flex items-center justify-center min-h-[300px]">
            <div class="text-center">
              <div class="w-10 h-10 mx-auto mb-4 rounded-full border-4 border-slate-200 border-t-blue-600 animate-spin">
              </div>

              <p class="text-sm font-semibold text-slate-500">
                Loading dashboard data...
              </p>
            </div>
          </div>

          <!-- Dashboard -->
          <div v-else>
            <!-- Core Metrics -->
            <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
              <div v-for="metric in metrics" :key="metric.title"
                class="group bg-white/65 backdrop-blur-2xl border border-white/90 rounded-2xl p-5 shadow-xl shadow-slate-300/15 hover:-translate-y-0.5 hover:shadow-2xl transition-all duration-300">
                <div class="flex justify-between items-start gap-3">
                  <span class="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                    {{ metric.title }}
                  </span>

                  <span :class="[
                    'text-xs font-bold px-2 py-0.5 rounded-full border',
                    metric.isPositive
                      ? 'bg-emerald-50 border-emerald-200 text-emerald-600'
                      : 'bg-red-50 border-red-200 text-red-600'
                  ]">
                    {{ metric.change }}
                  </span>
                </div>

                <div class="mt-4 flex items-end justify-between">
                  <span class="text-2xl font-black text-slate-950 tracking-tight">
                    {{ metric.value }}
                  </span>

                  <div class="w-9 h-9 rounded-xl bg-blue-50 border border-blue-100 flex items-center justify-center">
                    <svg class="w-4 h-4 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M12 8c-1.1 0-2 .67-2 1.5S10.9 11 12 11s2 .67 2 1.5S13.1 14 12 14m0-6V6m0 12v-2m7-4a7 7 0 11-14 0 7 7 0 0114 0z" />
                    </svg>
                  </div>
                </div>
              </div>
            </div>

            <!-- Overview -->
            <div v-if="activeTab === 'overview'" class="space-y-6">

              <!-- Top Row: Revenue & Quick Ops -->
              <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <!-- Revenue Chart -->
                <div class="lg:col-span-2 bg-white/65 backdrop-blur-2xl border border-white/90 rounded-3xl p-6 shadow-xl shadow-slate-300/20">
                  <div class="flex justify-between items-center border-b border-slate-200/80 pb-4 mb-4">
                    <h2 class="text-base font-bold text-slate-950">Revenue Performance</h2>
                    <span class="text-xs text-blue-600 font-semibold">Past 7 Days</span>
                  </div>
                  <div class="h-64 relative">
                    <Line v-if="!isLoading && revenueChartData.datasets.length" :data="revenueChartData" :options="chartOptions" />
                  </div>
                </div>

                <!-- Quick Operations -->
                <div class="bg-white/65 backdrop-blur-2xl border border-white/90 rounded-3xl p-6 shadow-xl shadow-slate-300/20 space-y-4">
                  <h2 class="text-base font-bold text-slate-950 border-b border-slate-200/80 pb-4">Quick Operations</h2>
                  <div class="space-y-3">
                    <button class="w-full py-3 px-4 bg-white/70 border border-slate-200 hover:border-blue-300 hover:bg-blue-50/60 rounded-xl text-xs font-bold text-slate-700 hover:text-blue-600 transition flex items-center justify-between">
                      <span>+ Add New Hardware SKU</span>
                      <span>→</span>
                    </button>
                    <button class="w-full py-3 px-4 bg-white/70 border border-slate-200 hover:border-blue-300 hover:bg-blue-50/60 rounded-xl text-xs font-bold text-slate-700 hover:text-blue-600 transition flex items-center justify-between">
                      <span>Export Financial Report (.CSV)</span>
                      <span>↓</span>
                    </button>
                    <button class="w-full py-3 px-4 bg-white/70 border border-slate-200 hover:border-blue-300 hover:bg-blue-50/60 rounded-xl text-xs font-bold text-slate-700 hover:text-blue-600 transition flex items-center justify-between">
                      <span>Process Pending RMA Queue</span>
                      <span class="px-2 py-0.5 bg-blue-50 text-blue-600 rounded-full text-[10px]">{{ metrics[3].value }}</span>
                    </button>
                  </div>
                </div>
              </div>

              <!-- Bottom Row: Order Status & Inventory -->
              <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <!-- Order Status Doughnut -->
                <div class="bg-white/65 backdrop-blur-2xl border border-white/90 rounded-3xl p-6 shadow-xl shadow-slate-300/20">
                  <div class="flex justify-between items-center border-b border-slate-200/80 pb-4 mb-4">
                    <h2 class="text-base font-bold text-slate-950">Order Status Distribution</h2>
                  </div>
                  <div class="h-64 relative flex justify-center">
                    <Doughnut v-if="!isLoading && orderStatusChartData.datasets.length" :data="orderStatusChartData" :options="doughnutOptions" />
                  </div>
                </div>

                <!-- Inventory Categories Bar -->
                <div class="bg-white/65 backdrop-blur-2xl border border-white/90 rounded-3xl p-6 shadow-xl shadow-slate-300/20">
                  <div class="flex justify-between items-center border-b border-slate-200/80 pb-4 mb-4">
                    <h2 class="text-base font-bold text-slate-950">Top 5 Categories by Stock</h2>
                  </div>
                  <div class="h-64 relative">
                    <Bar v-if="!isLoading && inventoryChartData.datasets.length" :data="inventoryChartData" :options="chartOptions" />
                  </div>
                </div>
              </div>

            </div>

            <!-- Inventory -->
            <div v-else-if="activeTab === 'inventory'"
              class="bg-white/65 backdrop-blur-2xl border border-white/90 rounded-3xl p-5 sm:p-6 shadow-xl shadow-slate-300/20 space-y-6">
              <!-- Search -->
              <div class="flex flex-col sm:flex-row gap-4 justify-between items-center">
                <input v-model="searchQuery" type="text" placeholder="Search product name or SKU..."
                  class="w-full sm:w-80 bg-white/75 border border-slate-200 rounded-xl px-4 py-2.5 text-xs text-slate-900 placeholder-slate-400 focus:outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10" />

                <select v-model="selectedCategory"
                  class="w-full sm:w-auto bg-white/75 border border-slate-200 rounded-xl px-4 py-2.5 text-xs text-slate-700 focus:outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10">
                  <option v-for="cat in categories" :key="cat" :value="cat">
                    {{ cat === 'All' ? 'All Categories' : cat }}
                  </option>
                </select>
              </div>

              <!-- Inventory Table -->
              <div class="overflow-x-auto">
                <table class="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr class="border-b border-slate-200 text-slate-500 uppercase tracking-wider font-semibold">
                      <th class="py-3 px-4">Item Name</th>
                      <th class="py-3 px-4">SKU</th>
                      <th class="py-3 px-4">Category</th>
                      <th class="py-3 px-4">Price</th>
                      <th class="py-3 px-4">Stock</th>
                      <th class="py-3 px-4">Status</th>
                      <th class="py-3 px-4 text-right">Actions</th>
                    </tr>
                  </thead>

                  <tbody class="divide-y divide-slate-200/70">
                    <tr v-for="item in filteredInventory" :key="item.id" class="hover:bg-blue-50/40 transition">
                      <td class="py-3.5 px-4 font-bold text-slate-900">
                        {{ item.name }}
                      </td>

                      <td class="py-3.5 px-4 font-mono text-slate-500">
                        {{ item.sku }}
                      </td>

                      <td class="py-3.5 px-4 text-slate-600">
                        {{ item.category }}
                      </td>

                      <td class="py-3.5 px-4 font-bold text-slate-900">
                        LKR {{ item.price.toFixed(2) }}
                      </td>

                      <td class="py-3.5 px-4 font-bold text-slate-700">
                        {{ item.stock }} units
                      </td>

                      <td class="py-3.5 px-4">
                        <span :class="[
                          'px-2.5 py-1 rounded-full border text-[10px] font-bold',
                          getStockBadgeStyle(item.status)
                        ]">
                          {{ item.status }}
                        </span>
                      </td>

                      <td class="py-3.5 px-4 text-right">
                        <button
                          class="px-3 py-1.5 bg-white border border-slate-200 hover:border-blue-300 hover:bg-blue-50 text-slate-600 hover:text-blue-600 rounded-lg font-semibold transition">
                          Edit
                        </button>
                      </td>
                    </tr>

                    <!-- Empty State -->
                    <tr v-if="filteredInventory.length === 0">
                      <td colspan="7" class="py-12 text-center text-slate-400">
                        No inventory items found.
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <!-- Activity Logs -->
            <div v-else-if="activeTab === 'activity'"
              class="bg-white/65 backdrop-blur-2xl border border-white/90 rounded-3xl p-5 sm:p-6 shadow-xl shadow-slate-300/20 space-y-4">
              <h2 class="text-xs font-bold text-blue-600 uppercase tracking-widest mb-2">
                Audit Trails
              </h2>

              <div class="space-y-3">
                <div v-for="log in activityLogs" :key="log.id"
                  class="p-4 bg-white/65 border border-slate-200/80 rounded-2xl flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 hover:border-blue-200 hover:bg-blue-50/30 transition">
                  <div class="space-y-1">
                    <span class="text-xs font-bold text-slate-900 block">
                      {{ log.action }}
                    </span>

                    <span class="text-[11px] text-slate-500">
                      Initiated by: {{ log.user }}
                    </span>
                  </div>

                  <div class="text-left sm:text-right shrink-0">
                    <span class="text-[10px] font-mono text-slate-400 block">
                      {{ log.timestamp }}
                    </span>

                    <span class="text-[10px] font-bold text-blue-600 uppercase tracking-wider">
                      {{ log.id }}
                    </span>
                  </div>
                </div>

                <div v-if="activityLogs.length === 0" class="py-12 text-center text-slate-400">
                  No activity logs available.
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>