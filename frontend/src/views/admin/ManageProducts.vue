<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminConfirmModal from '../../components/admin/AdminConfirmModal.vue'
import api from '../../services/api'
import { getActiveStaffUser } from '../../utils/rbac'

interface ApiCategory {
  categoryId?: string | number
  categoryName?: string
  id?: string | number
  name?: string
}

interface ApiProduct {
  productId: string
  name: string
  price: number
  brand: string
  description?: string
  discription?: string
  stockQty: number
  images: string[]
  category?: {
    categoryId?: string | number
    categoryName?: string
    id?: string | number
    name?: string
  } | null
  categoryId?: string | number
  categoryName?: string
  adminId?: string
  warehouseStaffId?: string
}

interface Category {
  categoryId: string
  categoryName: string
}

interface Product {
  id: string
  name: string
  brand: string
  sku: string
  category: string
  categoryId: string
  price: number
  stock: number
  status: 'In Stock' | 'Low Stock' | 'Out of Stock'
  image: string
  images: string[]
  description: string
  discription: string
  adminId?: string
  warehouseStaffId?: string
}

interface ProductRequest {
  productId?: string
  name: string
  price: number
  brand: string
  description: string
  discription: string
  stockQty: number
  images: string[]
  categoryId: string
  adminId?: string
  warehouseStaffId?: string
}

const getLoggedInStaffInfo = () => {
  const staff = getActiveStaffUser()
  const role = String(staff?.role || sessionStorage.getItem('role') || '').toUpperCase()
  const userId = String(
    staff?.userId ||
    staff?.id ||
    sessionStorage.getItem('userId') ||
    sessionStorage.getItem('staffId') ||
    sessionStorage.getItem('adminId') ||
    ''
  ).trim()

  const isAdmin = role.includes('ADMIN') || userId.startsWith('ADM')
  const isWarehouseStaff = role.includes('WAREHOUSE') || userId.startsWith('WH') || userId.startsWith('WS')

  return {
    userId,
    role,
    isAdmin,
    isWarehouseStaff,
  }
}

/* =========================================================
   STATE
========================================================= */

const products = ref<Product[]>([])
const categories = ref<Category[]>([])

const loading = ref(false)
const saving = ref(false)
const deleting = ref(false)

const error = ref('')
const categoryError = ref('')

const searchQuery = ref('')
const selectedCategory = ref('All')
const selectedStock = ref('All')

const currentPage = ref(1)
const itemsPerPage = 5

const showProductModal = ref(false)
const showDeleteModal = ref(false)

const isEditing = ref(false)

const selectedProduct = ref<Product | null>(null)

const selectedImageName = ref('')
const imageError = ref('')
const uploadingImages = ref(false)

const newImageUrl = ref('')

const productForm = ref<Product>({
  id: '',
  name: '',
  brand: '',
  sku: '',
  category: '',
  categoryId: '',
  price: 0,
  stock: 0,
  status: 'In Stock',
  image: '',
  images: [],
  description: '',
  discription: '',
  adminId: '',
  warehouseStaffId: '',
})

/* =========================================================
   HELPERS
========================================================= */

const getApiList = <T>(data: any, key: string): T[] => {
  if (Array.isArray(data)) {
    return data
  }

  if (data?.data) {
    return getApiList<T>(data.data, key)
  }

  if (Array.isArray(data?.[key])) {
    return data[key]
  }

  return []
}

/* =========================================================
   LKR PRICE FORMAT
========================================================= */

const formatLKR = (price: number) => {
  return new Intl.NumberFormat('en-LK', {
    style: 'currency',
    currency: 'LKR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(price) || 0)
}

const getStockStatus = (stock: number): Product['status'] => {
  if (stock <= 0) {
    return 'Out of Stock'
  }

  if (stock <= 5) {
    return 'Low Stock'
  }

  return 'In Stock'
}

/* =========================================================
   MAP PRODUCT
========================================================= */

const mapProduct = (
  product: ApiProduct,
  index: number,
): Product => {
  const id = String(product.productId || '')

  const stock = Number(product.stockQty) || 0

  const imageList = Array.isArray(product.images)
    ? product.images.filter(
      image =>
        typeof image === 'string' &&
        image.trim() !== '',
    )
    : []

  const categoryId = String(
    product.category?.categoryId ??
    product.category?.id ??
    product.categoryId ??
    '',
  )

  const categoryName =
    product.category?.categoryName ||
    product.category?.name ||
    product.categoryName ||
    categories.value.find(
      category => category.categoryId === categoryId,
    )?.categoryName ||
    'Uncategorized'

  return {
    id,

    name: product.name || '',

    brand: product.brand || '',

    sku: `PROD-${String(id || index + 1).padStart(3, '0')}`,

    category: categoryName,

    categoryId,

    price: Number(product.price) || 0,

    stock,

    status: getStockStatus(stock),

    image: imageList[0] || '',

    images: imageList,

    description:
      product.description ||
      product.discription ||
      '',

    discription:
      product.description ||
      product.discription ||
      '',

    adminId: product.adminId || '',

    warehouseStaffId: product.warehouseStaffId || '',
  }
}

/* =========================================================
   FETCH CATEGORIES
========================================================= */

const fetchCategories = async () => {
  categoryError.value = ''

  try {
    const response = await api.get('/categories')

    const apiCategories =
      getApiList<ApiCategory>(
        response.data,
        'categories',
      )

    categories.value = apiCategories.map(
      category => ({
        categoryId: String(
          category.categoryId ??
          category.id ??
          '',
        ),

        categoryName:
          category.categoryName ||
          category.name ||
          '',
      }),
    )
  } catch (err) {
    console.error(
      'Failed to fetch categories:',
      err,
    )

    categoryError.value =
      'Failed to load categories.'
  }
}

/* =========================================================
   FETCH PRODUCTS
========================================================= */

const fetchProducts = async () => {
  loading.value = true
  error.value = ''

  try {
    const response = await api.get('/products')

    const apiProducts =
      getApiList<ApiProduct>(
        response.data,
        'products',
      )

    products.value =
      apiProducts.map(mapProduct).reverse()
  } catch (err) {
    console.error(
      'Failed to fetch products:',
      err,
    )

    error.value =
      'Failed to load products.'
  } finally {
    loading.value = false
  }
}

/* =========================================================
   FILTERING
========================================================= */

const filteredProducts = computed(() => {
  const search =
    searchQuery.value
      .toLowerCase()
      .trim()

  return products.value.filter(
    product => {
      const matchesSearch =
        !search ||
        product.name
          .toLowerCase()
          .includes(search) ||
        product.sku
          .toLowerCase()
          .includes(search) ||
        product.brand
          .toLowerCase()
          .includes(search)

      const matchesCategory =
        selectedCategory.value === 'All' ||
        product.categoryId ===
        selectedCategory.value ||
        product.category ===
        selectedCategory.value

      const matchesStock =
        selectedStock.value === 'All' ||
        product.status ===
        selectedStock.value

      return (
        matchesSearch &&
        matchesCategory &&
        matchesStock
      )
    },
  )
})

const totalPages = computed(() =>
  Math.max(
    1,
    Math.ceil(
      filteredProducts.value.length /
      itemsPerPage,
    ),
  ),
)

const paginatedProducts = computed(() =>
  filteredProducts.value.slice(
    (currentPage.value - 1) *
    itemsPerPage,
    currentPage.value *
    itemsPerPage,
  ),
)

const totalProducts = computed(
  () => products.value.length,
)

const totalStock = computed(() =>
  products.value.reduce(
    (total, product) =>
      total + product.stock,
    0,
  ),
)

const lowStockProducts = computed(
  () =>
    products.value.filter(
      product =>
        product.status ===
        'Low Stock',
    ).length,
)

const outOfStockProducts =
  computed(
    () =>
      products.value.filter(
        product =>
          product.status ===
          'Out of Stock',
      ).length,
  )

const resetPage = () => {
  currentPage.value = 1
}

/* =========================================================
   PRODUCT STATUS
========================================================= */

const updateProductStatus = () => {
  productForm.value.status =
    getStockStatus(
      Number(
        productForm.value.stock,
      ) || 0,
    )
}

/* =========================================================
   OPEN ADD PRODUCT
========================================================= */

const openAddProduct = () => {
  isEditing.value = false

  selectedImageName.value = ''

  imageError.value = ''

  newImageUrl.value = ''

  const staff = getLoggedInStaffInfo()

  productForm.value = {
    id: '',
    name: '',
    brand: '',
    sku: '',
    category: '',
    categoryId:
      categories.value[0]
        ?.categoryId || '',
    price: 0,
    stock: 0,
    status: 'In Stock',
    image: '',
    images: [],
    description: '',
    discription: '',
    adminId: staff.isAdmin && staff.userId ? staff.userId : '',
    warehouseStaffId: staff.isWarehouseStaff && staff.userId ? staff.userId : '',
  }

  onCategoryChange()

  showProductModal.value = true
}

/* =========================================================
   OPEN EDIT PRODUCT
========================================================= */

const openEditProduct = (
  product: Product,
) => {
  isEditing.value = true

  selectedImageName.value = ''

  imageError.value = ''

  newImageUrl.value = ''

  const existingImages =
    Array.isArray(product.images)
      ? [...product.images]
      : []

  const resolvedDescription =
    (product.description || product.discription || '').trim()

  const staff = getLoggedInStaffInfo()

  productForm.value = {
    ...product,

    description: resolvedDescription,

    discription: resolvedDescription,

    adminId: staff.isAdmin && staff.userId ? staff.userId : (product.adminId || ''),

    warehouseStaffId: staff.isWarehouseStaff && staff.userId ? staff.userId : (product.warehouseStaffId || ''),

    image:
      product.image ||
      existingImages[0] ||
      '',

    images: existingImages,
  }

  showProductModal.value = true
}

/* =========================================================
   CLOSE PRODUCT MODAL
========================================================= */

const closeProductModal = () => {
  showProductModal.value = false

  selectedImageName.value = ''

  imageError.value = ''

  newImageUrl.value = ''
}

/* =========================================================
   ADD IMAGE URL
========================================================= */

const addImageUrl = () => {
  imageError.value = ''

  const url =
    newImageUrl.value.trim()

  if (!url) {
    return
  }

  if (
    productForm.value.images.includes(
      url,
    )
  ) {
    imageError.value =
      'This image has already been added.'

    return
  }

  if (url.length > 1500) {
    imageError.value =
      'Image URL cannot be longer than 1500 characters.'

    return
  }

  productForm.value.images.push(url)

  if (!productForm.value.image) {
    productForm.value.image = url
  }

  newImageUrl.value = ''
}

/* =========================================================
   REMOVE IMAGE
========================================================= */

const removeImage = (
  index: number,
) => {
  productForm.value.images.splice(
    index,
    1,
  )

  if (
    productForm.value.images.length ===
    0
  ) {
    productForm.value.image = ''
    return
  }

  if (
    !productForm.value.images.includes(
      productForm.value.image,
    )
  ) {
    productForm.value.image =
      productForm.value.images[0]
  }
}

/* =========================================================
   SET MAIN IMAGE
========================================================= */

const setMainImage = (
  image: string,
) => {
  productForm.value.image = image
}

/* =========================================================
   MULTIPLE IMAGE FILE UPLOAD
========================================================= */

const MAX_IMAGE_FILE_SIZE =
  5 * 1024 * 1024

const handleImageFiles = async (
  event: Event,
) => {
  const input =
    event.target as HTMLInputElement

  imageError.value = ''

  const files = input.files

  if (!files || files.length === 0) {
    return
  }

  selectedImageName.value =
    Array.from(files)
      .map(file => file.name)
      .join(', ')

  uploadingImages.value = true

  for (const file of Array.from(
    files,
  )) {
    if (
      !file.type.startsWith('image/')
    ) {
      continue
    }

    if (
      file.size > MAX_IMAGE_FILE_SIZE
    ) {
      imageError.value =
        'Each image must be smaller than 5MB.'

      continue
    }

    const formData = new FormData()

    formData.append('file', file)

    try {
      const response =
        await api.post(
          '/uploads/image',
          formData,
          {
            headers: {
              'Content-Type':
                'multipart/form-data',
            },
          },
        )

      const url =
        response.data?.url as string

      if (!url) {
        throw new Error(
          'Upload response did not include a URL.',
        )
      }

      if (
        !productForm.value.images.includes(
          url,
        )
      ) {
        productForm.value.images.push(
          url,
        )
      }

      if (
        !productForm.value.image
      ) {
        productForm.value.image = url
      }
    } catch (err: any) {
      console.error(
        'Failed to upload image:',
        err,
      )

      imageError.value =
        err?.response?.data?.message ||
        'Failed to upload one of the selected images.'
    }
  }

  uploadingImages.value = false

  input.value = ''
}

/* =========================================================
   BUILD REQUEST
========================================================= */

const buildRequest =
  (): ProductRequest => {
    const imageList =
      productForm.value.images
        .filter(
          image =>
            typeof image ===
            'string' &&
            image.trim() !== '',
        )
        .map(image =>
          image.trim(),
        )

    const mainImage =
      productForm.value.image.trim()

    const finalImages: string[] = []

    if (mainImage) {
      finalImages.push(mainImage)
    }

    imageList.forEach(image => {
      if (
        !finalImages.includes(
          image,
        )
      ) {
        finalImages.push(image)
      }
    })

    return {
      ...(isEditing.value
        ? {}
        : {
          productId:
            crypto.randomUUID(),
        }),

      name:
        productForm.value.name.trim(),

      price:
        Number(
          productForm.value.price,
        ) || 0,

      brand:
        productForm.value.brand.trim(),

      description:
        (productForm.value.discription || productForm.value.description || '').trim(),

      discription:
        (productForm.value.discription || productForm.value.description || '').trim(),

      stockQty:
        Number(
          productForm.value.stock,
        ) || 0,

      images: finalImages,

      categoryId:
        productForm.value.categoryId,

      adminId: (() => {
        const staff = getLoggedInStaffInfo()
        if (staff.isAdmin && staff.userId) return staff.userId
        return productForm.value.adminId?.trim() || undefined
      })(),

      warehouseStaffId: (() => {
        const staff = getLoggedInStaffInfo()
        if (staff.isWarehouseStaff && staff.userId) return staff.userId
        return productForm.value.warehouseStaffId?.trim() || undefined
      })(),
    }
  }

/* =========================================================
   SAVE ERROR
========================================================= */

const getSaveErrorMessage = (
  err: any,
) => {
  const responseData =
    err?.response?.data

  if (
    typeof responseData ===
    'string' &&
    responseData.trim()
  ) {
    return responseData
  }

  if (responseData?.message) {
    return responseData.message
  }

  if (responseData?.error) {
    return responseData.error
  }

  if (
    err?.response?.status === 409
  ) {
    return (
      'Product could not be saved because it conflicts with an existing product.'
    )
  }

  return 'Failed to save product.'
}

/* =========================================================
   SAVE PRODUCT
========================================================= */

const saveProduct = async () => {
  updateProductStatus()

  error.value = ''

  imageError.value = ''

  if (!productForm.value.categoryId) {
    error.value =
      'Please select a category.'

    return
  }

  productForm.value.images =
    productForm.value.images.filter(
      image =>
        typeof image ===
        'string' &&
        image.trim() !== '',
    )

  if (
    productForm.value.image &&
    !productForm.value.images.includes(
      productForm.value.image,
    )
  ) {
    productForm.value.images.unshift(
      productForm.value.image,
    )
  }

  for (
    const image of productForm.value
      .images
  ) {
    if (image.length > 1500) {
      imageError.value =
        'One of the images is too large for the database. Use image URLs or configure file storage.'

      error.value =
        'One or more product images exceed the database limit of 1500 characters.'

      return
    }
  }

  if (
    productForm.value.images
      .length === 0
  ) {
    error.value =
      'Please add at least one product image.'

    return
  }

  saving.value = true

  try {
    const request =
      buildRequest()

    console.log(
      'Product request:',
      request,
    )

    if (isEditing.value) {
      await api.put(
        `/products/${encodeURIComponent(
          productForm.value.id,
        )}`,
        request,
      )
    } else {
      await api.post(
        '/products',
        request,
      )
    }

    await fetchProducts()

    closeProductModal()
  } catch (err: any) {
    console.error(
      'Failed to save product:',
      err,
    )

    console.error(
      'Save response:',
      err?.response?.data,
    )

    error.value =
      err?.response?.data?.message ===
        'Database constraint violation'
        ? 'Product could not be saved because one of its values violates a database constraint. Check the category and image values.'
        : getSaveErrorMessage(err)
  } finally {
    saving.value = false
  }
}

/* =========================================================
   DELETE PRODUCT
========================================================= */

const confirmDelete = (
  product: Product,
) => {
  selectedProduct.value =
    product

  showDeleteModal.value = true
}

const deleteProduct = async () => {
  if (!selectedProduct.value) {
    return
  }

  deleting.value = true

  error.value = ''

  try {
    await api.delete(
      `/products/${encodeURIComponent(
        selectedProduct.value.id,
      )}`,
    )

    products.value =
      products.value.filter(
        product =>
          product.id !==
          selectedProduct.value?.id,
      )

    selectedProduct.value = null

    showDeleteModal.value = false

    if (
      currentPage.value >
      totalPages.value
    ) {
      currentPage.value =
        totalPages.value
    }
  } catch (err: any) {
    console.error(
      'Failed to delete product:',
      err,
    )

    error.value =
      err?.response?.data?.message ||
      'Failed to delete product.'
  } finally {
    deleting.value = false
  }
}

/* =========================================================
   STYLES
========================================================= */

const getStatusClass = (
  status: Product['status'],
) => {
  if (status === 'In Stock') {
    return 'bg-emerald-50 border-emerald-200 text-emerald-600'
  }

  if (status === 'Low Stock') {
    return 'bg-amber-50 border-amber-200 text-amber-600'
  }

  return 'bg-red-50 border-red-200 text-red-600'
}

const getCategoryClass = (
  category: string,
) => {
  if (
    category ===
    'Graphics Cards'
  ) {
    return 'bg-blue-50 border-blue-200 text-blue-600'
  }

  if (
    category === 'Processors'
  ) {
    return 'bg-violet-50 border-violet-200 text-violet-600'
  }

  if (category === 'Memory') {
    return 'bg-cyan-50 border-cyan-200 text-cyan-600'
  }

  if (category === 'Storage') {
    return 'bg-indigo-50 border-indigo-200 text-indigo-600'
  }

  return 'bg-slate-50 border-slate-200 text-slate-600'
}

/* =========================================================
   PAGINATION
========================================================= */

const goToPage = (
  page: number,
) => {
  if (
    page >= 1 &&
    page <= totalPages.value
  ) {
    currentPage.value = page
  }
}

const clearFilters = () => {
  searchQuery.value = ''

  selectedCategory.value = 'All'

  selectedStock.value = 'All'

  currentPage.value = 1
}

/* =========================================================
   CATEGORY CHANGE
========================================================= */

const onCategoryChange = () => {
  const category =
    categories.value.find(
      category =>
        category.categoryId ===
        productForm.value.categoryId,
    )

  productForm.value.category =
    category?.categoryName || ''
}

/* =========================================================
   INITIAL LOAD
========================================================= */

onMounted(async () => {
  await fetchCategories()

  await fetchProducts()
})
</script>

<template>
  <div class="min-h-screen bg-slate-100">

    <!-- =====================================================
         ADMIN SIDEBAR
    ====================================================== -->

    <AdminSidebar />

    <!-- =====================================================
         MAIN
    ====================================================== -->

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

        <!-- =================================================
             CONTENT
        ================================================== -->

        <div class="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

          <!-- HEADER -->

          <div class="mb-8 flex flex-col lg:flex-row lg:items-center lg:justify-between gap-5">

            <div>

              <div class="flex items-center gap-3 flex-wrap">

                <span
                  class="px-2.5 py-1 rounded-md bg-blue-50 border border-blue-200 text-blue-600 text-[10px] font-bold uppercase tracking-widest">
                  Admin Panel
                </span>

                <span class="text-xs font-semibold text-slate-400">
                  / Products
                </span>

              </div>

              <h1 class="mt-3 text-3xl sm:text-4xl font-black text-slate-950 tracking-tight">
                Product Management
              </h1>

              <p class="mt-2 text-sm text-slate-500">
                Manage hardware products,
                pricing, stock levels,
                images, and product
                information.
              </p>

            </div>

            <button @click="openAddProduct"
              class="inline-flex items-center justify-center gap-2 px-5 py-3 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 text-white text-sm font-bold shadow-lg shadow-blue-500/20 hover:-translate-y-0.5 hover:shadow-xl hover:shadow-blue-500/25 transition-all">

              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>

              Add Product

            </button>

          </div>

          <!-- ERROR -->

          <div v-if="error" class="mb-6 p-4 rounded-xl bg-red-50 border border-red-200 text-sm text-red-600">

            {{ error }}

            <button @click="fetchProducts" class="ml-3 font-bold underline">
              Retry
            </button>

          </div>

          <!-- =================================================
               STATISTICS
          ================================================== -->

          <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-5 mb-8">

            <!-- Products -->

            <div
              class="bg-white/65 backdrop-blur-2xl border border-white/90 rounded-2xl p-5 shadow-xl shadow-slate-300/15">

              <div class="flex items-start justify-between">

                <div>

                  <p class="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                    Total Products
                  </p>

                  <p class="mt-2 text-3xl font-black text-slate-950">
                    {{ totalProducts }}
                  </p>

                </div>

                <div class="w-11 h-11 rounded-xl bg-blue-50 border border-blue-100 flex items-center justify-center">

                  <svg class="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
                  </svg>

                </div>

              </div>

              <p class="mt-3 text-xs text-slate-400">
                Active hardware catalog
              </p>

            </div>

            <!-- Stock -->

            <div
              class="bg-white/65 backdrop-blur-2xl border border-white/90 rounded-2xl p-5 shadow-xl shadow-slate-300/15">

              <div class="flex items-start justify-between">

                <div>

                  <p class="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                    Total Stock
                  </p>

                  <p class="mt-2 text-3xl font-black text-slate-950">
                    {{ totalStock }}
                  </p>

                </div>

                <div
                  class="w-11 h-11 rounded-xl bg-emerald-50 border border-emerald-100 flex items-center justify-center">

                  <svg class="w-5 h-5 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 12l4 4L19 6" />
                  </svg>

                </div>

              </div>

              <p class="mt-3 text-xs text-slate-400">
                Units currently available
              </p>

            </div>

            <!-- Low -->

            <div
              class="bg-white/65 backdrop-blur-2xl border border-white/90 rounded-2xl p-5 shadow-xl shadow-slate-300/15">

              <div class="flex items-start justify-between">

                <div>

                  <p class="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                    Low Stock
                  </p>

                  <p class="mt-2 text-3xl font-black text-slate-950">
                    {{ lowStockProducts }}
                  </p>

                </div>

                <div class="w-11 h-11 rounded-xl bg-amber-50 border border-amber-100 flex items-center justify-center">

                  <svg class="w-5 h-5 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M12 9v4m0 4h.01M10.3 3.8l-8.2 14a2 2 0 001.73 3h16.34a2 2 0 001.73-3l-8.2-14a2 2 0 00-3.4 0z" />
                  </svg>

                </div>

              </div>

              <p class="mt-3 text-xs text-amber-600 font-medium">
                Requires attention
              </p>

            </div>

            <!-- Out -->

            <div
              class="bg-white/65 backdrop-blur-2xl border border-white/90 rounded-2xl p-5 shadow-xl shadow-slate-300/15">

              <div class="flex items-start justify-between">

                <div>

                  <p class="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                    Out of Stock
                  </p>

                  <p class="mt-2 text-3xl font-black text-slate-950">
                    {{ outOfStockProducts }}
                  </p>

                </div>

                <div class="w-11 h-11 rounded-xl bg-red-50 border border-red-100 flex items-center justify-center">

                  <svg class="w-5 h-5 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>

                </div>

              </div>

              <p class="mt-3 text-xs text-red-500 font-medium">
                Restock required
              </p>

            </div>

          </div>

          <!-- =================================================
               PRODUCT TABLE
          ================================================== -->

          <section
            class="bg-white/65 backdrop-blur-2xl border border-white/90 rounded-3xl shadow-xl shadow-slate-300/20 overflow-hidden">

            <div class="p-5 sm:p-6 border-b border-slate-200/80">

              <div class="flex flex-col xl:flex-row xl:items-center xl:justify-between gap-4">

                <div>

                  <h2 class="text-lg font-black text-slate-950">
                    Product Inventory
                  </h2>

                  <p class="mt-1 text-xs text-slate-400">
                    {{ filteredProducts.length }}
                    products found
                  </p>

                </div>

                <div class="flex flex-col sm:flex-row gap-3">

                  <!-- Search -->

                  <div class="relative">

                    <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" fill="none"
                      stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M21 21l-4.35-4.35m2.35-5.65a8 8 0 11-16 0 8 8 0 0116 0z" />
                    </svg>

                    <input v-model="searchQuery" @input="resetPage" type="text" placeholder="Search products..."
                      class="w-full sm:w-64 bg-white/80 border border-slate-200 rounded-xl pl-9 pr-4 py-2.5 text-xs text-slate-900 placeholder-slate-400 focus:outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10" />

                  </div>

                  <!-- Category -->

                  <select v-model="selectedCategory" @change="resetPage"
                    class="bg-white/80 border border-slate-200 rounded-xl px-4 py-2.5 text-xs text-slate-700 focus:outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10">

                    <option value="All">
                      All Categories
                    </option>

                    <option v-for="category in categories" :key="category.categoryId" :value="category.categoryId">
                      {{ category.categoryName }}
                    </option>

                  </select>

                  <!-- Stock -->

                  <select v-model="selectedStock" @change="resetPage"
                    class="bg-white/80 border border-slate-200 rounded-xl px-4 py-2.5 text-xs text-slate-700 focus:outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10">

                    <option value="All">
                      All Stock
                    </option>

                    <option value="In Stock">
                      In Stock
                    </option>

                    <option value="Low Stock">
                      Low Stock
                    </option>

                    <option value="Out of Stock">
                      Out of Stock
                    </option>

                  </select>

                  <button v-if="
                    searchQuery ||
                    selectedCategory !== 'All' ||
                    selectedStock !== 'All'
                  " @click="clearFilters"
                    class="px-4 py-2.5 rounded-xl bg-slate-100 border border-slate-200 text-xs font-bold text-slate-600 hover:bg-slate-200 transition">
                    Clear
                  </button>

                </div>

              </div>

            </div>

            <!-- Loading -->

            <div v-if="loading" class="px-5 py-16 text-center">

              <div class="w-10 h-10 mx-auto border-4 border-slate-200 border-t-blue-600 rounded-full animate-spin">
              </div>

              <p class="mt-4 text-sm font-bold text-slate-600">
                Loading products...
              </p>

            </div>

            <!-- Table -->

            <div v-else class="overflow-x-auto">

              <table class="w-full text-left">

                <thead>

                  <tr class="border-b border-slate-200/80 bg-slate-50/60">

                    <th class="px-5 py-4 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                      Product
                    </th>

                    <th class="px-5 py-4 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                      Product ID
                    </th>

                    <th class="px-5 py-4 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                      Category
                    </th>

                    <th class="px-5 py-4 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                      Price
                    </th>

                    <th class="px-5 py-4 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                      Stock
                    </th>

                    <th class="px-5 py-4 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                      Status
                    </th>

                    <th class="px-5 py-4 text-right text-[10px] font-bold uppercase tracking-wider text-slate-400">
                      Actions
                    </th>

                  </tr>

                </thead>

                <tbody class="divide-y divide-slate-200/60">

                  <tr v-for="product in paginatedProducts" :key="product.id"
                    class="hover:bg-blue-50/30 transition-colors">

                    <!-- Product -->

                    <td class="px-5 py-4">

                      <div class="flex items-center gap-3 min-w-[240px]">

                        <div class="w-12 h-12 rounded-xl overflow-hidden bg-slate-100 border border-slate-200 shrink-0">

                          <img :src="product.images?.[0] ||
                            '/placeholder-product.png'
                            " :alt="product.name" class="w-full h-full object-cover" @error="
                              ($event.target as HTMLImageElement).src =
                              '/placeholder-product.png'
                              " />

                        </div>

                        <div>

                          <p class="text-sm font-bold text-slate-900">
                            {{ product.name }}
                          </p>

                          <p class="text-[11px] text-slate-400 mt-1">
                            {{
                              product.brand ||
                              'No brand specified'
                            }}
                          </p>

                          <p v-if="
                            product.images.length > 1
                          " class="text-[10px] text-blue-500 font-semibold mt-1">
                            {{
                              product.images.length
                            }}
                            images
                          </p>

                        </div>

                      </div>

                    </td>

                    <!-- ID -->

                    <td class="px-5 py-4">

                      <span class="font-mono text-xs text-slate-500">
                        {{ product.id }}
                      </span>

                    </td>

                    <!-- Category -->

                    <td class="px-5 py-4">

                      <span :class="[
                        'inline-flex px-2.5 py-1 rounded-full border text-[10px] font-bold',
                        getCategoryClass(
                          product.category,
                        ),
                      ]">
                        {{ product.category }}
                      </span>

                    </td>

                    <!-- PRICE - LKR -->

                    <td class="px-5 py-4">

                      <span class="text-sm font-black text-slate-900">
                        {{ formatLKR(product.price) }}
                      </span>

                    </td>

                    <!-- Stock -->

                    <td class="px-5 py-4">

                      <span class="text-sm font-bold text-slate-700">
                        {{ product.stock }}
                      </span>

                      <span class="text-[10px] text-slate-400 ml-1">
                        units
                      </span>

                    </td>

                    <!-- Status -->

                    <td class="px-5 py-4">

                      <span :class="[
                        'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full border text-[10px] font-bold',
                        getStatusClass(
                          product.status,
                        ),
                      ]">

                        <span class="w-1.5 h-1.5 rounded-full bg-current"></span>

                        {{ product.status }}

                      </span>

                    </td>

                    <!-- Actions -->

                    <td class="px-5 py-4">

                      <div class="flex items-center justify-end gap-2">

                        <button @click="
                          openEditProduct(product)
                          " title="Edit product"
                          class="w-9 h-9 rounded-lg bg-white border border-slate-200 text-slate-500 hover:text-blue-600 hover:border-blue-300 hover:bg-blue-50 transition flex items-center justify-center">

                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                              d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.5-8.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 8.5-8.5z" />
                          </svg>

                        </button>

                        <button @click="
                          confirmDelete(product)
                          " title="Delete product"
                          class="w-9 h-9 rounded-lg bg-white border border-slate-200 text-slate-500 hover:text-red-600 hover:border-red-300 hover:bg-red-50 transition flex items-center justify-center">

                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6M9 7V4a1 1 0 011-1h4a1 1 0 011 1v3m-7 0h10" />
                          </svg>

                        </button>

                      </div>

                    </td>

                  </tr>

                  <!-- Empty -->

                  <tr v-if="
                    paginatedProducts.length ===
                    0
                  ">

                    <td colspan="7" class="px-5 py-16 text-center">

                      <div
                        class="w-14 h-14 mx-auto rounded-2xl bg-slate-100 border border-slate-200 flex items-center justify-center">

                        <svg class="w-6 h-6 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0H4m5-5h6" />
                        </svg>

                      </div>

                      <p class="mt-4 text-sm font-bold text-slate-700">
                        No products found
                      </p>

                      <p class="mt-1 text-xs text-slate-400">
                        Try changing your search
                        or filters.
                      </p>

                    </td>

                  </tr>

                </tbody>

              </table>

            </div>

            <!-- Pagination -->

            <div
              class="px-5 sm:px-6 py-4 border-t border-slate-200/80 flex flex-col sm:flex-row items-center justify-between gap-4">

              <p class="text-xs text-slate-400">

                Showing

                <span class="font-bold text-slate-600">
                  {{
                    filteredProducts.length ===
                      0
                      ? 0
                      : (currentPage - 1) *
                      itemsPerPage +
                      1
                  }}
                </span>

                -

                <span class="font-bold text-slate-600">
                  {{
                    Math.min(
                      currentPage *
                      itemsPerPage,
                      filteredProducts.length,
                    )
                  }}
                </span>

                of

                <span class="font-bold text-slate-600">
                  {{ filteredProducts.length }}
                </span>

                products

              </p>

              <div class="flex items-center gap-2">

                <button @click="
                  goToPage(
                    currentPage - 1,
                  )
                  " :disabled="currentPage === 1
                    "
                  class="w-9 h-9 rounded-lg bg-white border border-slate-200 text-slate-500 disabled:opacity-40 disabled:cursor-not-allowed hover:border-blue-300 hover:text-blue-600 transition flex items-center justify-center">
                  ‹
                </button>

                <button v-for="page in totalPages" :key="page" @click="
                  goToPage(page)
                  " :class="[
                    'w-9 h-9 rounded-lg text-xs font-bold transition',
                    currentPage === page
                      ? 'bg-blue-600 text-white shadow-md shadow-blue-500/20'
                      : 'bg-white border border-slate-200 text-slate-500 hover:border-blue-300 hover:text-blue-600',
                  ]">
                  {{ page }}
                </button>

                <button @click="
                  goToPage(
                    currentPage + 1,
                  )
                  " :disabled="currentPage ===
                    totalPages
                    "
                  class="w-9 h-9 rounded-lg bg-white border border-slate-200 text-slate-500 disabled:opacity-40 disabled:cursor-not-allowed hover:border-blue-300 hover:text-blue-600 transition flex items-center justify-center">
                  ›
                </button>

              </div>

            </div>

          </section>

        </div>
      </div>
    </main>

    <!-- =====================================================
         ADD / EDIT PRODUCT MODAL
    ====================================================== -->

    <Transition name="modal">

      <div v-if="showProductModal" class="fixed inset-0 z-[100] flex items-center justify-center p-4">

        <!-- Overlay -->

        <div class="absolute inset-0 bg-slate-950/30 backdrop-blur-sm" @click="closeProductModal"></div>

        <!-- Modal -->

        <div
          class="product-modal relative w-full max-w-3xl max-h-[92vh] overflow-y-auto bg-white/95 backdrop-blur-2xl border border-white rounded-3xl shadow-2xl shadow-slate-900/20">

          <!-- Header -->

          <div
            class="sticky top-0 z-20 px-6 py-5 bg-white/95 backdrop-blur-xl border-b border-slate-200/80 flex items-center justify-between">

            <div>

              <h2 class="text-xl font-black text-slate-950">
                {{
                  isEditing
                    ? 'Edit Product'
                    : 'Add New Product'
                }}
              </h2>

              <p class="mt-1 text-xs text-slate-400">
                {{
                  isEditing
                    ? 'Update product information, images and stock.'
                    : 'Add a new hardware product with multiple images.'
                }}
              </p>

            </div>

            <button @click="closeProductModal"
              class="w-9 h-9 rounded-xl bg-slate-100 border border-slate-200 text-slate-500 hover:bg-red-50 hover:border-red-200 hover:text-red-500 transition flex items-center justify-center">
              ×
            </button>

          </div>

          <!-- FORM -->

          <form @submit.prevent="saveProduct" class="p-6 space-y-5">

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-5">

              <!-- Product Name -->

              <div class="sm:col-span-2">

                <label class="block text-xs font-bold text-slate-600 mb-2">
                  Product Name
                </label>

                <input v-model="productForm.name" type="text" required placeholder="e.g. NVIDIA GeForce RTX 4090"
                  class="w-full bg-white/80 border border-slate-200 rounded-xl px-4 py-3 text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10" />

              </div>

              <!-- Product ID -->

              <div v-if="isEditing">

                <label class="block text-xs font-bold text-slate-600 mb-2">
                  Product ID
                </label>

                <input :value="productForm.id" type="text" disabled
                  class="w-full bg-slate-100 border border-slate-200 rounded-xl px-4 py-3 text-sm text-slate-500" />

              </div>

              <p v-else class="self-end pb-3 text-xs text-slate-400">
                Product ID will be generated automatically.
              </p>

              <!-- Category -->

              <div>

                <label class="block text-xs font-bold text-slate-600 mb-2">
                  Category
                </label>

                <select v-model="productForm.categoryId" @change="onCategoryChange" required
                  class="w-full bg-white/80 border border-slate-200 rounded-xl px-4 py-3 text-sm text-slate-700 focus:outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10">

                  <option value="" disabled>
                    Select category
                  </option>

                  <option v-for="category in categories" :key="category.categoryId" :value="category.categoryId">
                    {{ category.categoryName }}
                  </option>

                </select>

              </div>

              <!-- PRICE - LKR -->

              <div>

                <label class="block text-xs font-bold text-slate-600 mb-2">
                  Price (LKR)
                </label>

                <div class="relative">

                  <span class="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 text-sm font-bold">
                    LKR
                  </span>

                  <input v-model.number="productForm.price
                    " type="number" min="0" step="0.01" required
                    class="w-full bg-white/80 border border-slate-200 rounded-xl pl-14 pr-4 py-3 text-sm text-slate-900 focus:outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10" />

                </div>

              </div>

              <!-- Brand -->

              <div>

                <label class="block text-xs font-bold text-slate-600 mb-2">
                  Brand
                </label>

                <input v-model="productForm.brand" type="text" placeholder="e.g. Sony"
                  class="w-full bg-white/80 border border-slate-200 rounded-xl px-4 py-3 text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10" />

              </div>

              <!-- Stock -->

              <div>

                <label class="block text-xs font-bold text-slate-600 mb-2">
                  Stock Quantity
                </label>

                <input v-model.number="productForm.stock
                  " type="number" min="0" required
                  class="w-full bg-white/80 border border-slate-200 rounded-xl px-4 py-3 text-sm text-slate-900 focus:outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10" />

              </div>

              <!-- Description -->

              <div class="sm:col-span-2">

                <label class="block text-xs font-bold text-slate-600 mb-2">
                  Product Description
                </label>

                <textarea v-model="productForm.discription"
                  @input="productForm.description = productForm.discription"
                  rows="4" placeholder="Enter a description for this product..."
                  class="w-full resize-y bg-white/80 border border-slate-200 rounded-xl px-4 py-3 text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10"></textarea>

              </div>

              <!-- =================================================
                   MULTIPLE IMAGE UPLOAD
              ================================================== -->

              <div class="sm:col-span-2">

                <div class="flex items-center justify-between mb-2">

                  <label class="block text-xs font-bold text-slate-600">
                    Product Images
                  </label>

                  <span class="px-2 py-1 rounded-full bg-blue-50 text-blue-600 text-[10px] font-bold">
                    {{
                      productForm.images.length
                    }}
                    images
                  </span>

                </div>

                <!-- File Upload -->

                <input type="file" accept="image/*" multiple :disabled="uploadingImages
                  " @change="
                    handleImageFiles
                  "
                  class="w-full bg-white/80 border border-slate-200 rounded-xl px-4 py-2.5 text-sm text-slate-700 file:mr-4 file:rounded-lg file:border-0 file:bg-blue-50 file:px-3 file:py-2 file:text-xs file:font-bold file:text-blue-700 hover:file:bg-blue-100 disabled:opacity-60" />

                <p class="mt-2 text-[11px] text-slate-400">
                  Select multiple images at once. The first image is used as the main product image.
                </p>

                <p v-if="uploadingImages" class="mt-2 text-xs text-blue-600">
                  Uploading images...
                </p>

                <p v-else-if="
                  selectedImageName
                " class="mt-2 text-xs text-slate-500">
                  Selected:
                  {{ selectedImageName }}
                </p>

                <!-- URL Divider -->

                <div class="my-4 flex items-center gap-3 text-[10px] font-bold uppercase tracking-wider text-slate-400">

                  <span class="h-px flex-1 bg-slate-200"></span>

                  Or add image URL

                  <span class="h-px flex-1 bg-slate-200"></span>

                </div>

                <!-- URL Input -->

                <div class="flex gap-2">

                  <input v-model="newImageUrl" @keyup.enter.prevent="
                    addImageUrl
                  " type="text" placeholder="https://example.com/product-image.jpg"
                    class="flex-1 min-w-0 bg-white/80 border border-slate-200 rounded-xl px-4 py-3 text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10" />

                  <button type="button" @click="addImageUrl"
                    class="px-4 rounded-xl bg-blue-600 text-white text-xs font-bold hover:bg-blue-700 transition">
                    Add
                  </button>

                </div>

                <!-- Image Error -->

                <p v-if="imageError" class="mt-2 text-xs text-red-600">
                  {{ imageError }}
                </p>

                <!-- IMAGE GALLERY -->

                <div v-if="
                  productForm.images.length >
                  0
                " class="mt-4 p-4 rounded-2xl bg-slate-50/80 border border-slate-200">

                  <div class="flex items-center justify-between mb-3">

                    <div>

                      <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                        Image Gallery
                      </p>

                      <p class="text-xs text-slate-500 mt-1">
                        Click an image to make it the main image.
                      </p>

                    </div>

                    <span class="text-[10px] font-bold text-slate-400">
                      {{
                        productForm.images.length
                      }}
                      total
                    </span>

                  </div>

                  <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">

                    <div v-for="(
image, index
                      ) in productForm.images" :key="`${image}-${index}`" class="relative group">

                      <!-- Image -->

                      <button type="button" @click="
                        setMainImage(
                          image,
                        )
                        " :class="[
                          'relative block w-full aspect-square rounded-xl overflow-hidden border-2 transition',
                          productForm.image ===
                            image
                            ? 'border-blue-500 ring-4 ring-blue-500/10'
                            : 'border-slate-200 hover:border-blue-300',
                        ]">

                        <img :src="image" :alt="`Product image ${index + 1
                          }`" class="w-full h-full object-cover" @error="
                            (
                              $event.target as HTMLImageElement
                            ).style.opacity =
                            '0.3'
                            " />

                        <!-- Main Badge -->

                        <span v-if="
                          productForm.image ===
                          image
                        "
                          class="absolute top-2 left-2 px-2 py-1 rounded-md bg-blue-600 text-white text-[9px] font-bold shadow">
                          MAIN
                        </span>

                        <!-- Number -->

                        <span
                          class="absolute bottom-2 left-2 w-6 h-6 rounded-full bg-black/60 text-white text-[10px] font-bold flex items-center justify-center">
                          {{ index + 1 }}
                        </span>

                      </button>

                      <!-- Remove -->

                      <button type="button" @click="
                        removeImage(
                          index,
                        )
                        "
                        class="absolute top-2 right-2 w-7 h-7 rounded-lg bg-white/95 text-red-500 border border-slate-200 shadow-sm opacity-0 group-hover:opacity-100 transition flex items-center justify-center"
                        title="Remove image">

                        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M6 18L18 6M6 6l12 12" />
                        </svg>

                      </button>

                    </div>

                  </div>

                </div>

              </div>

            </div>

            <!-- =================================================
                 PREVIEW
            ================================================== -->

            <div v-if="productForm.image" class="p-4 bg-slate-50/80 border border-slate-200 rounded-2xl">

              <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-3">
                Main Image Preview
              </p>

              <div class="flex items-center gap-4">

                <img :src="productForm.image" alt="Product preview"
                  class="w-20 h-20 object-cover rounded-xl border border-slate-200" />

                <div>

                  <p class="text-sm font-bold text-slate-900">
                    {{
                      productForm.name ||
                      'Product Name'
                    }}
                  </p>

                  <p class="text-xs text-slate-400 mt-1">
                    {{
                      categories.find(
                        category =>
                          category.categoryId ===
                          productForm.categoryId,
                      )?.categoryName ||
                      'No category selected'
                    }}
                  </p>

                  <p class="text-xs text-blue-500 font-semibold mt-1">
                    {{
                      productForm.images.length
                    }}
                    product images
                  </p>

                </div>

              </div>

            </div>

            <!-- =================================================
                 ACTIONS
            ================================================== -->

            <div class="flex flex-col sm:flex-row gap-3 pt-2">

              <button type="button" @click="
                closeProductModal
              "
                class="flex-1 px-5 py-3 rounded-xl bg-white border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50 transition">
                Cancel
              </button>

              <button type="submit" :disabled="saving"
                class="flex-1 px-5 py-3 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 text-white text-sm font-bold shadow-lg shadow-blue-500/20 hover:shadow-xl transition disabled:opacity-50">

                {{
                  saving
                    ? 'Saving...'
                    : isEditing
                      ? 'Update Product'
                      : 'Create Product'
                }}

              </button>

            </div>

          </form>

        </div>

      </div>

    </Transition>

    <!-- =====================================================
         DELETE PRODUCT MODAL (ORDER MANAGEMENT DESIGN)
         ===================================================== -->
    <AdminConfirmModal
      v-model:show="showDeleteModal"
      type="danger"
      title="Delete Product?"
      message="Are you sure you want to delete product"
      :target="selectedProduct?.name"
      description="This action cannot be undone and will permanently remove this product from the catalog."
      confirm-text="Delete Product"
      cancel-text="Cancel"
      :loading="deleting"
      @confirm="deleteProduct"
    />

  </div>
</template>

<style scoped>
.product-modal {
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.product-modal::-webkit-scrollbar {
  display: none;
}

.modal-enter-active,
.modal-leave-active {
  transition: all 0.25s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-active>div:last-child,
.modal-leave-active>div:last-child {
  transition: all 0.25s ease;
}

.modal-enter-from>div:last-child,
.modal-leave-to>div:last-child {
  opacity: 0;
  transform: translateY(10px) scale(0.98);
}
</style>