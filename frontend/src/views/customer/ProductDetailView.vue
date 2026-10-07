<script setup lang="ts">

import {
  ref,
  onMounted,
  computed
} from 'vue'

import {
  useRoute,
  useRouter
} from 'vue-router'

import {
  useCartStore
} from '../../stores/cartStore'

import api from '../../services/api'
import { usePopup } from '../../composables/usePopup'


/* =========================================================
   TYPES
========================================================= */

interface Product {

  id: string | number

  name: string

  description: string

  price: number

  category: string

  imageUrl?: string

  images: string[]

  stock?: number

  specs?: Record<string, string>

}


interface Review {

  reviewId: string | number

  customerId?: string | number

  customerName?: string

  rating: number

  comment?: string

  reviewDate?: string

}


/* =========================================================
   ROUTER / CART
========================================================= */

const route = useRoute()

const router = useRouter()

const cartStore = useCartStore()
const popup = usePopup()


/* =========================================================
   PRODUCT
========================================================= */

const product =
  ref<Product | null>(null)

const isLoading =
  ref(true)

const selectedQuantity =
  ref(1)

const activeTab =
  ref<'specs' | 'shipping'>(
    'specs'
  )

const addedNotification =
  ref(false)


/* =========================================================
   IMAGE GALLERY
========================================================= */

const selectedImageIndex =
  ref(0)

const productImages = computed(() => {

  if (!product.value) {
    return []
  }

  return product.value.images || []

})


const selectedImage = computed(() => {

  if (!product.value) {
    return ''
  }


  if (
    productImages.value.length > 0 &&
    productImages.value[
    selectedImageIndex.value
    ]
  ) {

    return productImages.value[
      selectedImageIndex.value
    ]

  }


  return product.value.imageUrl || ''

})


function selectImage(
  index: number
) {

  if (
    index < 0 ||
    index >= productImages.value.length
  ) {
    return
  }

  selectedImageIndex.value =
    index

}


function nextImage() {

  const total =
    productImages.value.length


  if (total <= 1) {
    return
  }


  selectedImageIndex.value =
    (
      selectedImageIndex.value + 1
    ) % total

}


function previousImage() {

  const total =
    productImages.value.length


  if (total <= 1) {
    return
  }


  if (
    selectedImageIndex.value === 0
  ) {

    selectedImageIndex.value =
      total - 1

    return

  }


  selectedImageIndex.value--

}


function handleImageError(
  index: number
) {

  if (!product.value) {
    return
  }


  if (
    index < 0 ||
    index >= product.value.images.length
  ) {
    return
  }


  product.value.images[index] =
    ''


  if (
    index === selectedImageIndex.value
  ) {

    let nextValidIndex = -1


    for (
      let i = 0;
      i < product.value.images.length;
      i++
    ) {

      if (
        product.value.images[i]
      ) {

        nextValidIndex =
          i

        break

      }

    }


    if (nextValidIndex >= 0) {

      selectedImageIndex.value =
        nextValidIndex

    }

  }

}


/* =========================================================
   PHOTO MAGNIFIER
========================================================= */

const magnifierVisible =
  ref(false)

const mouseX =
  ref(0)

const mouseY =
  ref(0)

const zoomPositionX =
  ref(50)

const zoomPositionY =
  ref(50)


function toggleMagnifier() {

  magnifierVisible.value =
    !magnifierVisible.value

}


function closeMagnifier() {

  magnifierVisible.value =
    false

}


function handleMagnifierMove(
  event: MouseEvent
) {

  const element =
    event.currentTarget as HTMLElement


  const rect =
    element.getBoundingClientRect()


  const x =
    event.clientX -
    rect.left


  const y =
    event.clientY -
    rect.top


  mouseX.value =
    Math.max(
      0,
      Math.min(
        rect.width,
        x
      )
    )


  mouseY.value =
    Math.max(
      0,
      Math.min(
        rect.height,
        y
      )
    )


  zoomPositionX.value =
    Math.max(
      0,
      Math.min(
        100,
        (x / rect.width) * 100
      )
    )


  zoomPositionY.value =
    Math.max(
      0,
      Math.min(
        100,
        (y / rect.height) * 100
      )
    )

}


/* =========================================================
   REVIEWS
========================================================= */

const reviews =
  ref<Review[]>([])

const reviewsLoading =
  ref(false)

const reviewsError =
  ref('')

const reviewSubmitting =
  ref(false)

const reviewRating =
  ref(5)

const reviewComment =
  ref('')

const submitReviewError =
  ref('')


const reviewCount =
  computed(() => {

    return reviews.value.length

  })


const averageRating =
  computed(() => {

    if (
      reviews.value.length === 0
    ) {

      return 0

    }


    let total = 0


    for (
      const review of reviews.value
    ) {

      total +=
        Number(
          review.rating || 0
        )

    }


    return Number(
      (
        total /
        reviews.value.length
      ).toFixed(1)
    )

  })


/* =========================================================
   CUSTOMER ID
========================================================= */

function getCustomerId(): string {

  const storedUser =
    sessionStorage.getItem(
      'user'
    )


  if (!storedUser) {
    return ''
  }


  try {

    const user =
      JSON.parse(
        storedUser
      )


    return String(
      user.userId ||
      user.id ||
      user.customerId ||
      ''
    )

  } catch {

    return ''

  }

}


/* =========================================================
   LOAD PRODUCT
========================================================= */

onMounted(async () => {

  const productId =
    route.params.id


  try {

    const response =
      await api.get(
        `/products/${productId}`
      )


    const data =
      response.data


    if (data) {

      let imageList:
        string[] = []


      /*
       * Keep ALL uploaded images
       */

      if (
        Array.isArray(
          data.images
        )
      ) {

        imageList =
          data.images
            .filter(
              (
                image: unknown
              ) =>
                typeof image ===
                'string' &&
                image.trim() !== ''
            )
            .filter(
              (
                image: string,
                index: number,
                array: string[]
              ) =>
                array.indexOf(
                  image
                ) === index
            )

      }


      const fallbackImage =
        data.image ||
        data.imageUrl ||
        ''


      if (
        imageList.length === 0 &&
        fallbackImage
      ) {

        imageList =
          [
            fallbackImage
          ]

      }


      product.value = {

        id:
          data.productId ||
          data.id ||
          String(
            productId
          ),

        name:
          data.name ||
          'Product',

        description:
          data.discription ||
          data.description ||
          '',

        price:
          Number(
            data.price
          ) || 0,

        category:
          data.categoryName ||
          (
            typeof data.category ===
              'string'
              ? data.category
              : data.category
                ?.categoryName
          ) ||
          'Hardware',

        imageUrl:
          imageList[0] ||
          fallbackImage,

        images:
          imageList,

        stock:
          Number(
            data.stockQty ??
            data.stock ??
            0
          ),

        specs:
          data.specs ||
          {

            Brand:
              data.brand ||
              'Premium Grade',

            Category:
              data.categoryName ||
              'Hardware',

            Availability:
              (
                data.stockQty ??
                data.stock ??
                0
              ) > 0
                ? 'In Stock'
                : 'Out of Stock'

          }

      }


      selectedImageIndex.value =
        0


      await fetchReviews()

    }

  } catch (error) {

    console.error(
      'Failed to fetch product details:',
      error
    )


    /*
     * Fallback data
     */

    product.value = {

      id:
        String(
          productId
        ) || '1',

      name:
        'RTX 4080 Super 16GB Graphics Card',

      description:
        'The NVIDIA GeForce RTX 4080 Super delivers the ultra performance and features that enthusiast gamers and creators demand. Bring your games and creative projects to life with ray tracing and AI-powered graphics.',

      price:
        1199.00,

      category:
        'Graphics Cards',

      imageUrl:
        '',

      images:
        [
          ''
        ],

      stock:
        7,

      specs:
      {

        'CUDA Cores':
          '10240',

        'Memory Size':
          '16 GB GDDR6X',

        'Memory Bus':
          '256-bit',

        'Recommended PSU':
          '750W',

        'Interface':
          'PCIe 4.0 x16'

      }

    }


    await fetchReviews()

  } finally {

    isLoading.value =
      false

  }

})


/* =========================================================
   FETCH REVIEWS
========================================================= */

async function fetchReviews() {

  if (!product.value) {
    return
  }


  const productId =
    String(
      product.value.id
    )


  if (!productId) {
    return
  }


  reviewsLoading.value =
    true

  reviewsError.value =
    ''


  try {

    const response =
      await api.get(
        '/reviews',
        {
          params: {
            productId
          }
        }
      )


    if (
      Array.isArray(
        response.data
      )
    ) {

      reviews.value =
        response.data

    } else if (
      Array.isArray(
        response.data?.reviews
      )
    ) {

      reviews.value =
        response.data.reviews

    } else {

      reviews.value =
        []

    }

  } catch (error) {

    console.error(
      'Error fetching reviews:',
      error
    )


    reviews.value =
      []


    reviewsError.value =
      'Unable to load reviews.'

  } finally {

    reviewsLoading.value =
      false

  }

}


/* =========================================================
   SUBMIT REVIEW
========================================================= */

async function submitReview() {

  if (!product.value) {
    return
  }


  const customerId =
    getCustomerId()


  if (!customerId) {

    submitReviewError.value =
      'Please log in to submit a review.'

    return

  }


  const rating =
    Number(
      reviewRating.value
    )


  if (
    rating < 1 ||
    rating > 5
  ) {

    submitReviewError.value =
      'Please select a rating between 1 and 5.'

    return

  }


  if (
    !reviewComment.value.trim()
  ) {

    submitReviewError.value =
      'Please write a review.'

    return

  }


  reviewSubmitting.value =
    true

  submitReviewError.value =
    ''


  try {

    await api.post(
      '/reviews',
      {

        customerId:

          customerId,

        productId:

          String(
            product.value.id
          ),

        rating:

          rating,

        comment:

          reviewComment.value.trim()

      }
    )


    reviewComment.value =
      ''

    reviewRating.value =
      5


    await fetchReviews()

  } catch (error: any) {

    console.error(
      'Error submitting review:',
      error
    )


    submitReviewError.value =
      error?.response?.data?.message ||
      'Unable to submit review. Please try again.'

  } finally {

    reviewSubmitting.value =
      false

  }

}


/* =========================================================
   DELETE REVIEW
========================================================= */

async function deleteReview(
  reviewId: string | number
) {

  const confirmed = await popup.confirm({
    title: 'Delete Review',
    message: 'Are you sure you want to delete this review?',
    type: 'warning',
    confirmText: 'Delete',
    cancelText: 'Cancel'
  })

  if (!confirmed) {
    return
  }

  try {

    await api.delete(
      `/reviews/${reviewId}`
    )

    reviews.value =
      reviews.value.filter(
        (
          review
        ) =>
          String(
            review.reviewId
          ) !==
          String(
            reviewId
          )
      )

    await popup.success(
      'Your review has been successfully removed.',
      'Review Deleted'
    )

  } catch (error) {

    console.error(
      'Error deleting review:',
      error
    )

    await popup.error(
      'Unable to delete review. Please try again.',
      'Delete Failed'
    )

  }

}


/* =========================================================
   QUANTITY
========================================================= */

function updateQuantity(
  delta: number
) {

  const newQty =
    selectedQuantity.value +
    delta


  if (
    newQty >= 1 &&
    newQty <=
    (
      product.value?.stock ||
      10
    )
  ) {

    selectedQuantity.value =
      newQty

  }

}


/* =========================================================
   ADD TO CART
========================================================= */

async function addToCart() {

  if (!product.value) {
    return
  }


  if (
    !product.value.stock ||
    product.value.stock <= 0
  ) {

    return

  }


  try {

    await cartStore.addItem(
      String(
        product.value.id
      ),
      selectedQuantity.value
    )


    addedNotification.value =
      true


    setTimeout(() => {

      addedNotification.value =
        false

    }, 2500)

  } catch (error) {

    console.error(
      'Failed to add to cart:',
      error
    )

  }

}

</script>


<template>

  <!-- =====================================================
         PAGE
    ====================================================== -->

  <div class="min-h-screen
               bg-slate-100
               relative
               overflow-hidden">

    <!-- BACKGROUND DECORATIONS -->

    <div class="fixed
                   inset-0
                   pointer-events-none
                   overflow-hidden">

      <div class="absolute
                       -top-40
                       -left-40
                       w-[420px]
                       h-[420px]
                       rounded-full
                       bg-blue-300/15
                       blur-3xl"></div>


      <div class="absolute
                       top-1/3
                       -right-40
                       w-[420px]
                       h-[420px]
                       rounded-full
                       bg-cyan-300/15
                       blur-3xl"></div>


      <div class="absolute
                       bottom-[-180px]
                       left-1/3
                       w-[420px]
                       h-[420px]
                       rounded-full
                       bg-indigo-300/10
                       blur-3xl"></div>

    </div>


    <!-- MAIN CONTENT -->

    <div class="relative
                   z-10
                   max-w-6xl
                   mx-auto
                   px-4
                   sm:px-6
                   lg:px-8
                   py-6
                   sm:py-8">

      <!-- BACK -->

      <button type="button" @click="router.back()" class="group
                       inline-flex
                       items-center
                       gap-2
                       mb-6
                       px-3.5
                       py-2.5
                       rounded-xl
                       bg-white/90
                       backdrop-blur-xl
                       border border-white
                       shadow-sm
                       text-sm
                       font-bold
                       text-slate-600
                       hover:bg-blue-50
                       hover:border-blue-200
                       hover:text-blue-700
                       transition">

        <svg class="w-4
                           h-4
                           group-hover:-translate-x-0.5
                           transition" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">

          <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />

        </svg>

        Back to Catalog

      </button>


      <!-- =================================================
                 LOADING
            ================================================== -->

      <div v-if="isLoading" class="grid
                       grid-cols-1
                       lg:grid-cols-2
                       gap-6">

        <div class="h-[450px]
                           rounded-3xl
                           bg-white/80
                           border border-white
                           shadow-xl
                           shadow-slate-200/40
                           p-5
                           animate-pulse">

          <div class="w-full
                               h-full
                               rounded-2xl
                               bg-slate-200"></div>

        </div>


        <div class="rounded-3xl
                           bg-white/80
                           border border-white
                           shadow-xl
                           shadow-slate-200/40
                           p-6
                           animate-pulse">

          <div class="h-4
                               w-24
                               bg-slate-200
                               rounded"></div>


          <div class="h-9
                               w-3/4
                               bg-slate-200
                               rounded
                               mt-4"></div>


          <div class="h-28
                               bg-slate-200
                               rounded-2xl
                               mt-6"></div>


          <div class="h-12
                               w-1/2
                               bg-slate-200
                               rounded-xl
                               mt-6"></div>

        </div>

      </div>


      <!-- =================================================
                 PRODUCT
            ================================================== -->

      <div v-else-if="product" class="space-y-6">

        <!-- =================================================
                     TOP PRODUCT SECTION
                ================================================== -->

        <div class="grid
                           grid-cols-1
                           lg:grid-cols-2
                           gap-6
                           items-start">

          <!-- =================================================
                         IMAGE GALLERY
                    ================================================== -->

          <div class="rounded-3xl
                               bg-white/90
                               backdrop-blur-2xl
                               border border-white
                               shadow-xl
                               shadow-slate-200/40
                               p-5
                               sm:p-6">

            <!-- MAIN IMAGE -->

            <div class="relative
                                   h-[380px]
                                   sm:h-[450px]
                                   rounded-2xl
                                   bg-gradient-to-br
                                   from-slate-50
                                   via-white
                                   to-blue-50
                                   border border-slate-100
                                   overflow-hidden
                                   flex
                                   items-center
                                   justify-center" @mousemove="
                                    magnifierVisible
                                      ? handleMagnifierMove($event)
                                      : undefined
                                    ">

              <!-- SOFT LIGHT -->

              <div class="absolute
                                       -top-20
                                       -right-20
                                       w-52
                                       h-52
                                       rounded-full
                                       bg-blue-100/40
                                       blur-3xl"></div>


              <div class="absolute
                                       -bottom-20
                                       -left-20
                                       w-52
                                       h-52
                                       rounded-full
                                       bg-cyan-100/40
                                       blur-3xl"></div>


              <!-- =================================================
                                 SMALL MAGNIFIER BUTTON
                            ================================================== -->

              <button v-if="selectedImage" type="button" @click="toggleMagnifier" class="absolute
                                       left-3
                                       top-3
                                       z-50
                                       w-9
                                       h-9
                                       rounded-xl
                                       bg-white/95
                                       backdrop-blur-md
                                       border border-slate-200
                                       shadow-md
                                       text-slate-500
                                       hover:text-blue-600
                                       hover:border-blue-300
                                       hover:bg-blue-50
                                       transition" :class="magnifierVisible
                                        ? 'text-blue-600 border-blue-300 bg-blue-50'
                                        : ''
                                        " :title="magnifierVisible
                                          ? 'Close magnifier'
                                          : 'Zoom image'
                                          ">

                <!-- SEARCH ICON -->

                <svg v-if="
                  !magnifierVisible
                " class="w-4
                                           h-4
                                           mx-auto" fill="none" viewBox="0 0 24 24" stroke="currentColor"
                  stroke-width="2">

                  <circle cx="11" cy="11" r="7" />

                  <path stroke-linecap="round" d="M16.5 16.5L21 21" />

                </svg>


                <!-- CLOSE ICON -->

                <svg v-else class="w-4
                                           h-4
                                           mx-auto" fill="none" viewBox="0 0 24 24" stroke="currentColor"
                  stroke-width="2">

                  <path stroke-linecap="round" stroke-linejoin="round" d="M6 6l12 12M18 6L6 18" />

                </svg>

              </button>


              <!-- PRODUCT IMAGE -->

              <img v-if="selectedImage" :src="selectedImage" :alt="product.name" class="relative
                                       z-10
                                       max-h-full
                                       max-w-full
                                       object-contain
                                       p-8
                                       drop-shadow-xl
                                       cursor-default
                                       select-none" @error="
                                        handleImageError(
                                          selectedImageIndex
                                        )
                                        " draggable="false" />


              <!-- NO IMAGE -->

              <div v-else class="relative
                                       z-10
                                       text-center">

                <div class="w-20
                                           h-20
                                           mx-auto
                                           rounded-2xl
                                           bg-white
                                           border border-slate-200
                                           flex
                                           items-center
                                           justify-center
                                           shadow-sm">

                  <svg class="w-9
                                               h-9
                                               text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor"
                    stroke-width="1.5">

                    <path stroke-linecap="round" stroke-linejoin="round" d="M4 16l4-4 3 3 5-6 4 5M4 20h16" />

                  </svg>

                </div>


                <p class="mt-3
                                           text-xs
                                           font-bold
                                           text-slate-400">
                  No image available
                </p>

              </div>


              <!-- =================================================
                                 ZOOM LENS
                            ================================================== -->

              <div v-if="
                magnifierVisible &&
                selectedImage
              " class="hidden
                                       lg:block
                                       absolute
                                       z-[60]
                                       w-44
                                       h-44
                                       rounded-full
                                       overflow-hidden
                                       bg-white
                                       border-4
                                       border-white
                                       shadow-2xl
                                       ring-1
                                       ring-slate-200
                                       pointer-events-none" :style="{
                                        left: `${mouseX - 88}px`,
                                        top: `${mouseY - 88}px`,
                                        backgroundImage:
                                          `url(${selectedImage})`,
                                        backgroundRepeat:
                                          'no-repeat',
                                        backgroundSize:
                                          '250%',
                                        backgroundPosition:
                                          `${zoomPositionX}% ${zoomPositionY}%`
                                      }">

                <div class="absolute
                                           top-2
                                           left-1/2
                                           -translate-x-1/2
                                           px-2
                                           py-1
                                           rounded-lg
                                           bg-slate-900/80
                                           backdrop-blur-md
                                           text-[9px]
                                           font-black
                                           uppercase
                                           tracking-wider
                                           text-white">
                  Zoom
                </div>

              </div>


              <!-- =================================================
                                 PREVIOUS IMAGE
                            ================================================== -->

              <button v-if="
                productImages.length > 1
              " type="button" @click="
                previousImage
              " class="absolute
                                       left-3
                                       top-1/2
                                       -translate-y-1/2
                                       z-30
                                       w-10
                                       h-10
                                       rounded-full
                                       bg-white/95
                                       border border-slate-200
                                       shadow-md
                                       text-slate-600
                                       hover:text-blue-600
                                       hover:scale-105
                                       transition" title="Previous image">

                <svg class="w-5
                                           h-5
                                           mx-auto" fill="none" viewBox="0 0 24 24" stroke="currentColor"
                  stroke-width="2">

                  <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />

                </svg>

              </button>


              <!-- =================================================
                                 NEXT IMAGE
                            ================================================== -->

              <button v-if="
                productImages.length > 1
              " type="button" @click="
                nextImage
              " class="absolute
                                       right-3
                                       top-1/2
                                       -translate-y-1/2
                                       z-30
                                       w-10
                                       h-10
                                       rounded-full
                                       bg-white/95
                                       border border-slate-200
                                       shadow-md
                                       text-slate-600
                                       hover:text-blue-600
                                       hover:scale-105
                                       transition" title="Next image">

                <svg class="w-5
                                           h-5
                                           mx-auto" fill="none" viewBox="0 0 24 24" stroke="currentColor"
                  stroke-width="2">

                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />

                </svg>

              </button>


              <!-- STOCK -->

              <span :class="[
                'absolute top-4 right-4 z-30',
                'inline-flex items-center gap-2',
                'px-3 py-1.5 rounded-full',
                'border text-[11px] font-black',

                (product.stock ?? 0) > 0

                  ? 'bg-emerald-50/95 border-emerald-200 text-emerald-700'

                  : 'bg-rose-50/95 border-rose-200 text-rose-700'
              ]">

                <span class="w-1.5
                                           h-1.5
                                           rounded-full" :class="(product.stock ?? 0) > 0
                                            ? 'bg-emerald-500'
                                            : 'bg-rose-500'
                                            "></span>


                {{
                  (product.stock ?? 0) > 0
                    ? `In Stock (${product.stock})`
                    : 'Out of Stock'
                }}

              </span>


              <!-- IMAGE COUNTER -->

              <div v-if="
                productImages.length > 1
              " class="absolute
                                       bottom-3
                                       left-1/2
                                       -translate-x-1/2
                                       z-30
                                       px-3
                                       py-1.5
                                       rounded-full
                                       bg-slate-900/70
                                       text-white
                                       text-[11px]
                                       font-black">

                {{ selectedImageIndex + 1 }}

                /

                {{ productImages.length }}

              </div>

            </div>


            <!-- =================================================
                             THUMBNAILS
                        ================================================== -->

            <div v-if="
              productImages.length > 1
            " class="mt-4
                                   flex
                                   gap-2.5
                                   overflow-x-auto
                                   pb-1">

              <button v-for="(
image,
  index
                                ) in productImages" :key="`${image}-${index}`
                                  " type="button" @click="
                                    selectImage(
                                      index
                                    );
                                  closeMagnifier()
                                    " class="relative
                                       w-16
                                       h-16
                                       sm:w-[70px]
                                       sm:h-[70px]
                                       shrink-0
                                       overflow-hidden
                                       rounded-xl
                                       bg-white
                                       border
                                       transition" :class="selectedImageIndex === index

                                        ? 'border-blue-500 ring-2 ring-blue-500/15'

                                        : 'border-slate-200 hover:border-blue-300'
                                        ">

                <img v-if="image" :src="image" :alt="`${product.name} image ${index + 1
                  }`
                  " class="w-full
                                           h-full
                                           object-contain
                                           p-1" @error="
                                            handleImageError(
                                              index
                                            )
                                            " />


                <span v-else class="w-full
                                           h-full
                                           flex
                                           items-center
                                           justify-center
                                           text-slate-300">
                  —
                </span>

              </button>

            </div>


            <!-- GALLERY INFO -->

            <div class="flex
                                   items-center
                                   justify-between
                                   gap-3
                                   mt-4">

              <div>

                <p class="text-[10px]
                                           uppercase
                                           tracking-[0.16em]
                                           font-black
                                           text-slate-400">
                  Product Gallery
                </p>


                <p class="mt-1
                                           text-xs
                                           text-slate-500">

                  {{
                    productImages.length
                  }}

                  image(s) available

                </p>

              </div>


              <div class="w-10
                                       h-10
                                       rounded-xl
                                       bg-blue-50
                                       border border-blue-100
                                       text-blue-600
                                       flex
                                       items-center
                                       justify-center">

                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.7">

                  <path stroke-linecap="round" stroke-linejoin="round" d="M4 5h16v14H4z" />

                  <circle cx="9" cy="10" r="1.5" />

                  <path stroke-linecap="round" stroke-linejoin="round" d="M4 16l4-4 3 3 3-4 6 5" />

                </svg>

              </div>

            </div>

          </div>


          <!-- =================================================
                         PRODUCT INFORMATION
                    ================================================== -->

          <div class="rounded-3xl
                               bg-white/90
                               backdrop-blur-2xl
                               border border-white
                               shadow-xl
                               shadow-slate-200/40
                               p-6
                               sm:p-7">

            <!-- CATEGORY -->

            <span class="inline-flex
                                   w-fit
                                   px-3
                                   py-1.5
                                   rounded-full
                                   bg-blue-50
                                   border border-blue-100
                                   text-[10px]
                                   font-black
                                   uppercase
                                   tracking-[0.14em]
                                   text-blue-700">
              {{ product.category }}
            </span>


            <!-- NAME -->

            <h1 class="mt-4
                                   text-2xl
                                   sm:text-3xl
                                   font-black
                                   leading-tight
                                   tracking-tight
                                   text-slate-900">
              {{ product.name }}
            </h1>


            <!-- RATING -->

            <div class="flex
                                   items-center
                                   gap-3
                                   mt-4">

              <div class="flex
                                       items-center
                                       gap-0.5">

                <span v-for="star in 5" :key="star" class="text-base" :class="star <=
                  Math.round(
                    averageRating
                  )

                  ? 'text-amber-400'

                  : 'text-slate-300'
                  ">
                  ★
                </span>

              </div>


              <span class="text-sm
                                       font-black
                                       text-slate-700">

                {{
                  averageRating > 0
                    ? averageRating.toFixed(1)
                    : 'No rating'
                }}

              </span>


              <span class="text-xs
                                       text-slate-400">

                (
                {{ reviewCount }}
                reviews
                )

              </span>

            </div>


            <!-- PRICE -->

            <div class="mt-5
                                   rounded-2xl
                                   bg-gradient-to-br
                                   from-blue-50
                                   via-white
                                   to-cyan-50
                                   border border-blue-100
                                   p-4">

              <p class="text-[10px]
                                       uppercase
                                       tracking-[0.16em]
                                       font-black
                                       text-slate-400">
                Product Price
              </p>


              <p class="mt-1
                                       text-3xl
                                       font-black
                                       tracking-tight
                                       bg-gradient-to-r
                                       from-blue-600
                                       to-cyan-500
                                       bg-clip-text
                                       text-transparent">

                LKR

                {{
                  product.price.toLocaleString(
                    'en-LK',
                    {
                      minimumFractionDigits:
                        2,

                      maximumFractionDigits:
                        2
                    }
                  )
                }}

              </p>

            </div>


            <!-- DESCRIPTION -->

            <div class="mt-6">

              <h3 class="text-sm
                                       font-black
                                       text-slate-900">
                Description
              </h3>


              <p class="mt-2
                                       text-sm
                                       leading-6
                                       text-slate-600">
                {{ product.description }}
              </p>

            </div>


            <!-- CART -->

            <div class="mt-6
                                   rounded-2xl
                                   bg-slate-50/80
                                   border border-slate-100
                                   p-4">

              <label class="block
                                       text-[10px]
                                       font-black
                                       uppercase
                                       tracking-[0.14em]
                                       text-slate-400
                                       mb-2">
                Select Quantity
              </label>


              <div class="flex
                                       flex-col
                                       sm:flex-row
                                       gap-3">

                <div class="inline-flex
                                           w-fit
                                           items-center
                                           bg-white
                                           border border-slate-200
                                           rounded-xl
                                           overflow-hidden">

                  <button type="button" @click="
                    updateQuantity(-1)
                    " :disabled="selectedQuantity <= 1
                      " class="w-11
                                               h-11
                                               text-slate-500
                                               hover:bg-blue-50
                                               hover:text-blue-600
                                               disabled:opacity-30
                                               disabled:cursor-not-allowed
                                               transition">
                    −
                  </button>


                  <span class="w-12
                                               text-center
                                               text-sm
                                               font-black
                                               text-slate-800">
                    {{ selectedQuantity }}
                  </span>


                  <button type="button" @click="
                    updateQuantity(1)
                    " :disabled="selectedQuantity >=
                      (
                        product.stock ||
                        10
                      )
                      " class="w-11
                                               h-11
                                               text-slate-500
                                               hover:bg-blue-50
                                               hover:text-blue-600
                                               disabled:opacity-30
                                               disabled:cursor-not-allowed
                                               transition">
                    +
                  </button>

                </div>


                <button type="button" @click="addToCart" :disabled="!product.stock ||
                  product.stock === 0
                  " class="flex-1
                                           h-11
                                           px-6
                                           rounded-xl
                                           bg-gradient-to-r
                                           from-blue-600
                                           to-cyan-500
                                           text-white
                                           text-sm
                                           font-black
                                           shadow-lg
                                           shadow-blue-500/15
                                           hover:from-blue-700
                                           hover:to-cyan-600
                                           disabled:from-slate-300
                                           disabled:to-slate-300
                                           disabled:text-slate-500
                                           disabled:cursor-not-allowed
                                           transition">

                  Add to Cart
                  ({{ selectedQuantity }})

                </button>

              </div>


              <div v-if="
                addedNotification
              " class="mt-3
                                       rounded-xl
                                       bg-emerald-50
                                       border border-emerald-100
                                       px-3.5
                                       py-3
                                       text-xs
                                       font-bold
                                       text-emerald-700">

                ✓ Successfully added
                {{ selectedQuantity }}
                item(s) to your cart.

              </div>

            </div>


            <!-- =================================================
                             PRODUCT TABS
                        ================================================== -->

            <div class="mt-6
                                   pt-6
                                   border-t border-slate-100">

              <div class="flex
                                       gap-6
                                       border-b
                                       border-slate-200">

                <button type="button" @click="
                  activeTab = 'specs'
                  " :class="[
                    'pb-3 text-xs font-black uppercase tracking-wider border-b-2 transition',

                    activeTab === 'specs'

                      ? 'border-blue-600 text-blue-700'

                      : 'border-transparent text-slate-400 hover:text-slate-700'
                  ]">
                  Technical Specifications
                </button>


                <button type="button" @click="
                  activeTab = 'shipping'
                  " :class="[
                    'pb-3 text-xs font-black uppercase tracking-wider border-b-2 transition',

                    activeTab === 'shipping'

                      ? 'border-blue-600 text-blue-700'

                      : 'border-transparent text-slate-400 hover:text-slate-700'
                  ]">
                  Warranty & Returns
                </button>

              </div>


              <!-- SPECS -->

              <div v-if="
                activeTab === 'specs'
              " class="mt-4
                                       rounded-2xl
                                       bg-slate-50/80
                                       border border-slate-100
                                       overflow-hidden">

                <div v-for="(
val,
  key
                                    ) in product.specs" :key="key" class="flex
                                           items-center
                                           justify-between
                                           gap-4
                                           px-4
                                           py-3
                                           border-b
                                           border-slate-100
                                           last:border-b-0">

                  <span class="text-xs
                                               font-semibold
                                               text-slate-500">
                    {{ key }}
                  </span>


                  <span class="text-xs
                                               font-black
                                               text-slate-800
                                               text-right">
                    {{ val }}
                  </span>

                </div>

              </div>


              <!-- SHIPPING -->

              <div v-else class="mt-4
                                       rounded-2xl
                                       bg-slate-50/80
                                       border border-slate-100
                                       p-4">

                <div class="space-y-3">

                  <p class="text-xs
                                               leading-5
                                               text-slate-600">
                    • 2-Year Manufacturer
                    Warranty included with
                    automated registration.
                  </p>


                  <p class="text-xs
                                               leading-5
                                               text-slate-600">
                    • Hassle-free 30-day
                    money-back return policy
                    for unopened items.
                  </p>


                  <p class="text-xs
                                               leading-5
                                               text-slate-600">
                    • Tracked dispatch within
                    24 business hours.
                  </p>

                </div>

              </div>

            </div>

          </div>

        </div>


        <!-- =================================================
                     REVIEWS
                ================================================== -->

        <div class="rounded-3xl
                           bg-white/90
                           backdrop-blur-2xl
                           border border-white
                           shadow-xl
                           shadow-slate-200/40
                           p-6
                           sm:p-7">

          <!-- REVIEW HEADER -->

          <div class="flex
                               flex-col
                               sm:flex-row
                               sm:items-center
                               sm:justify-between
                               gap-4">

            <div>

              <p class="text-[10px]
                                       uppercase
                                       tracking-[0.18em]
                                       font-black
                                       text-blue-600">
                Customer Feedback
              </p>


              <h2 class="mt-1
                                       text-xl
                                       font-black
                                       text-slate-900">
                Customer Reviews
              </h2>


              <p class="mt-1
                                       text-xs
                                       text-slate-400">

                {{ reviewCount }}

                {{
                  reviewCount === 1
                    ? 'review'
                    : 'reviews'
                }}

              </p>

            </div>


            <div class="flex
                                   items-center
                                   gap-3">

              <div class="flex
                                       items-center
                                       gap-0.5">

                <span v-for="star in 5" :key="star" class="text-lg" :class="star <=
                  Math.round(
                    averageRating
                  )

                  ? 'text-amber-400'

                  : 'text-slate-300'
                  ">
                  ★
                </span>

              </div>


              <span class="text-sm
                                       font-black
                                       text-slate-700">

                {{
                  averageRating > 0
                    ? averageRating.toFixed(1)
                    : '—'
                }}

              </span>

            </div>

          </div>


          <!-- ERROR -->

          <div v-if="reviewsError" class="mt-5
                               rounded-xl
                               bg-rose-50
                               border border-rose-100
                               px-4
                               py-3
                               text-sm
                               font-semibold
                               text-rose-600">

            {{ reviewsError }}

          </div>


          <!-- LOADING -->

          <div v-if="reviewsLoading" class="mt-6
                               rounded-2xl
                               bg-slate-50
                               border border-slate-100
                               py-8
                               text-center">

            <div class="w-8
                                   h-8
                                   mx-auto
                                   rounded-full
                                   border-4
                                   border-blue-100
                                   border-t-blue-600
                                   animate-spin"></div>


            <p class="mt-3
                                   text-xs
                                   font-semibold
                                   text-slate-400">
              Loading reviews...
            </p>

          </div>


          <!-- EMPTY -->

          <div v-else-if="
            reviews.length === 0 &&
            !reviewsError
          " class="mt-5
                               rounded-2xl
                               bg-slate-50/80
                               border border-slate-100
                               px-5
                               py-8
                               text-center">

            <div class="w-12
                                   h-12
                                   mx-auto
                                   rounded-2xl
                                   bg-blue-50
                                   border border-blue-100
                                   flex
                                   items-center
                                   justify-center
                                   text-blue-600
                                   text-lg">
              ★
            </div>


            <p class="mt-3
                                   text-sm
                                   font-black
                                   text-slate-700">
              No reviews yet
            </p>


            <p class="mt-1
                                   text-xs
                                   text-slate-400">
              Be the first customer to review
              this product.
            </p>

          </div>


          <!-- REVIEW LIST -->

          <div v-else class="mt-5
                               space-y-3">

            <div v-for="
review in reviews
                            " :key="review.reviewId
                              " class="rounded-2xl
                                   bg-slate-50/80
                                   border border-slate-100
                                   p-4
                                   hover:bg-white
                                   hover:border-blue-100
                                   transition">

              <div class="flex
                                       items-start
                                       justify-between
                                       gap-3">

                <!-- USER -->

                <div class="flex
                                           items-center
                                           gap-3
                                           min-w-0">

                  <div class="w-10
                                               h-10
                                               shrink-0
                                               rounded-full
                                               bg-blue-50
                                               border border-blue-100
                                               text-blue-600
                                               flex
                                               items-center
                                               justify-center
                                               text-xs
                                               font-black">

                    {{
                      (
                        review.customerName ||
                        'A'
                      )
                        .charAt(0)
                        .toUpperCase()
                    }}

                  </div>


                  <div class="min-w-0">

                    <p class="text-sm
                                                   font-black
                                                   text-slate-800
                                                   truncate">

                      {{
                        review.customerName ||
                        'Anonymous'
                      }}

                    </p>


                    <div class="flex
                                                   items-center
                                                   gap-0.5
                                                   mt-0.5">

                      <span v-for="
star in 5
                                                " :key="star
                                                  " class="text-xs" :class="star <=
                                                    Number(
                                                      review.rating
                                                    )

                                                    ? 'text-amber-400'

                                                    : 'text-slate-300'
                                                    ">
                        ★
                      </span>

                    </div>

                  </div>

                </div>


                <!-- DATE / DELETE -->

                <div class="flex
                                           items-center
                                           gap-2
                                           shrink-0">

                  <span v-if="
                    review.reviewDate
                  " class="hidden
                                               sm:block
                                               text-[10px]
                                               text-slate-400">

                    {{
                      review.reviewDate
                    }}

                  </span>


                  <button v-if="
                    getCustomerId() &&
                    String(
                      review.customerId
                    ) ===
                    String(
                      getCustomerId()
                    )
                  " type="button" @click="
                    deleteReview(
                      review.reviewId
                    )
                    " class="w-8
                                               h-8
                                               rounded-lg
                                               bg-white
                                               border border-slate-200
                                               text-slate-400
                                               hover:bg-rose-50
                                               hover:border-rose-200
                                               hover:text-rose-600
                                               transition" title="Delete review">

                    <svg class="w-4
                                                   h-4
                                                   mx-auto" fill="none" viewBox="0 0 24 24" stroke="currentColor"
                      stroke-width="2">

                      <path stroke-linecap="round" stroke-linejoin="round"
                        d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6M9 7V4a1 1 0 011-1h4a1 1 0 011 1v3m-7 0h10" />

                    </svg>

                  </button>

                </div>

              </div>


              <!-- COMMENT -->

              <p v-if="
                review.comment
              " class="mt-3
                                       pl-12
                                       text-[13px]
                                       leading-5
                                       text-slate-600">

                {{ review.comment }}

              </p>

            </div>

          </div>


          <!-- =================================================
                         WRITE REVIEW
                    ================================================== -->

          <div class="mt-5
                               rounded-2xl
                               bg-white
                               border border-slate-100
                               p-4">

            <div class="flex
                                   flex-col
                                   sm:flex-row
                                   sm:items-center
                                   sm:justify-between
                                   gap-3">

              <div>

                <h3 class="text-sm
                                           font-black
                                           text-slate-800">
                  Write a review
                </h3>


                <p class="mt-1
                                           text-xs
                                           text-slate-400">
                  Share your experience with
                  this product.
                </p>

              </div>


              <!-- RATING -->

              <div class="flex
                                       items-center
                                       gap-1">

                <button v-for="
star in 5
                                    " :key="star
                                      " type="button" @click="
                                        reviewRating =
                                        star
                                        " class="text-2xl
                                           leading-none
                                           transition" :class="star <=
                                            reviewRating

                                            ? 'text-amber-400'

                                            : 'text-slate-300 hover:text-amber-300'
                                            ">
                  ★
                </button>

              </div>

            </div>


            <!-- COMMENT -->

            <textarea v-model="reviewComment
              " rows="3" placeholder="Share your experience with this product..." class="mt-4
                                   w-full
                                   rounded-xl
                                   border border-slate-200
                                   bg-slate-50/50
                                   px-3.5
                                   py-3
                                   text-sm
                                   text-slate-700
                                   placeholder:text-slate-400
                                   outline-none
                                   resize-none
                                   focus:border-blue-400
                                   focus:ring-4
                                   focus:ring-blue-500/10"></textarea>


            <p v-if="
              submitReviewError
            " class="mt-2
                                   text-xs
                                   font-semibold
                                   text-rose-600">

              {{ submitReviewError }}

            </p>


            <div class="flex
                                   justify-end
                                   mt-3">

              <button type="button" @click="
                submitReview
              " :disabled="reviewSubmitting ||
                !reviewComment.trim()
                " class="h-10
                                       px-5
                                       rounded-xl
                                       bg-gradient-to-r
                                       from-blue-600
                                       to-cyan-500
                                       text-white
                                       text-sm
                                       font-black
                                       shadow-md
                                       shadow-blue-500/15
                                       hover:from-blue-700
                                       hover:to-cyan-600
                                       disabled:opacity-50
                                       disabled:cursor-not-allowed
                                       transition">

                {{
                  reviewSubmitting
                    ? 'Submitting...'
                    : 'Submit Review'
                }}

              </button>

            </div>

          </div>

        </div>

      </div>

    </div>

  </div>

</template>