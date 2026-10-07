import { defineStore } from 'pinia'
import {
    addToCart,
    getCart,
    removeCartItem,
    updateCartItemQuantity,
    clearCustomerCart
} from '../services/cartService'
import { useNotificationStore } from './notificationStore'

export interface CartItem {
    productId: string
    productName: string
    unitPrice: number
    quantity: number
    imageUrl?: string
    stockQty?: number
    price?: number
    originalPrice?: number
    discountPercentage?: number
    name?: string
    image?: string
    product?: {
        productId?: string
        name?: string
        price?: number
        originalPrice?: number
        discountPercentage?: number
        image?: string
        stockQty?: number
        categoryName?: string
    }
}

// Baseline regular prices for known hardware to calculate deal discounts
const CATALOG_REGULAR_PRICES: Record<string, number> = {
    'P001': 219900, // TV: regular 219,900, deal 189,900 (save 30,000)
    'P004': 315000, // S25: regular 315,000, deal 285,000 (save 30,000)
    'P005': 320000, // ASUS TUF: regular 320,000, deal 285,000 (save 35,000)
    'deal-gpu-1': 399900,
    'deal-cpu-1': 182900,
    'deal-ram-1': 79900,
    'deal-ssd-1': 94900,
    'deal-aio-1': 92000,
    'deal-case-1': 64900,
    'gpu-4090': 699000,
    'gpu-4080s': 399000,
    'gpu-7900xtx': 365000,
    'cpu-7800x3d': 182900,
    'cpu-14900k': 219000
}

function cleanImageUrl(value: unknown): string {
    if (!value || typeof value !== 'string') {
        return ''
    }

    let url = value.trim()
    if (!url) return ''

    const markdownIndex = url.indexOf('](')
    if (
        (url.startsWith('[') || url.startsWith('![')) &&
        markdownIndex !== -1 &&
        url.endsWith(')')
    ) {
        url = url.substring(markdownIndex + 2, url.length - 1).trim()
    }

    if (url.startsWith('<') && url.endsWith('>')) {
        url = url.substring(1, url.length - 1).trim()
    }

    if (
        (url.startsWith('"') && url.endsWith('"')) ||
        (url.startsWith("'") && url.endsWith("'"))
    ) {
        url = url.substring(1, url.length - 1).trim()
    }

    if (url.startsWith('uploads/')) {
        url = '/' + url
    }

    return url
}

function normalizeItem(rawItem: any): CartItem {
    const productId = String(rawItem.productId || rawItem.product?.productId || rawItem.id || '')
    const productName = rawItem.productName || rawItem.product?.name || rawItem.name || 'Hardware Component'
    const unitPrice = Number(rawItem.unitPrice ?? rawItem.product?.price ?? rawItem.price ?? 0)
    const quantity = Number(rawItem.quantity || 1)
    const rawImg =
        rawItem.imageUrl ||
        rawItem.image ||
        rawItem.product?.image ||
        (Array.isArray(rawItem.images) && rawItem.images.length > 0 ? rawItem.images[0] : '') ||
        (Array.isArray(rawItem.product?.images) && rawItem.product.images.length > 0 ? rawItem.product.images[0] : '') ||
        ''
    const imageUrl = cleanImageUrl(rawImg)
    const stockQty = Number(rawItem.stockQty ?? rawItem.product?.stockQty ?? rawItem.stock ?? 99)
    const categoryName = rawItem.categoryName || rawItem.product?.categoryName || 'Component'

    // Retrieve original price from deal registry or catalog baseline
    let originalPrice = Number(rawItem.originalPrice ?? rawItem.product?.originalPrice ?? rawItem.regularPrice ?? 0)
    if (!originalPrice || originalPrice <= unitPrice) {
        // Check registered deal in storage
        try {
            const registered = JSON.parse(localStorage.getItem('deal_registry') || '{}')
            if (registered[productId]?.originalPrice) {
                originalPrice = Number(registered[productId].originalPrice)
            }
        } catch {}
    }
    if (!originalPrice && CATALOG_REGULAR_PRICES[productId] && CATALOG_REGULAR_PRICES[productId] > unitPrice) {
        originalPrice = CATALOG_REGULAR_PRICES[productId]
    }

    const discountPercentage = originalPrice > unitPrice
        ? Math.round(((originalPrice - unitPrice) / originalPrice) * 100)
        : Number(rawItem.discountPercentage ?? 0)

    return {
        productId,
        productName,
        unitPrice,
        quantity,
        originalPrice: originalPrice > unitPrice ? originalPrice : undefined,
        discountPercentage: discountPercentage > 0 ? discountPercentage : undefined,
        imageUrl,
        stockQty,
        price: unitPrice,
        name: productName,
        image: imageUrl,
        product: {
            productId,
            name: productName,
            price: unitPrice,
            originalPrice: originalPrice > unitPrice ? originalPrice : undefined,
            discountPercentage: discountPercentage > 0 ? discountPercentage : undefined,
            image: imageUrl,
            stockQty,
            categoryName
        }
    }
}

function getLoggedInUserId(): string | null {
    const storedUser = sessionStorage.getItem('user') || localStorage.getItem('user')
    if (!storedUser) return null
    try {
        const user = JSON.parse(storedUser)
        const id = user.customerId ?? user.userId ?? user.id
        return id ? String(id) : null
    } catch {
        return null
    }
}

export const useCartStore = defineStore('cart', {
    state: () => ({
        items: [] as CartItem[],
        loading: false,
        cartCount: 0
    }),

    getters: {
        itemCount(state): number {
            return state.items.reduce(
                (total, item) => total + (item.quantity || 0),
                0
            )
        },
        subtotal(state): number {
            return state.items.reduce(
                (total, item) => total + (item.unitPrice * (item.quantity || 1)),
                0
            )
        },
        dealDiscount(state): number {
            return state.items.reduce((total, item) => {
                const orig = Number(item.originalPrice || 0)
                const unit = Number(item.unitPrice || 0)
                const qty = Number(item.quantity || 1)
                if (orig > unit) {
                    return total + (orig - unit) * qty
                }
                return total
            }, 0)
        },
        subtotalBeforeDeals(): number {
            return this.subtotal + this.dealDiscount
        },
        shipping(): number {
            return this.subtotal > 15000 || this.subtotal === 0 ? 0 : 450
        },
        tax(): number {
            return this.subtotal * 0.08
        },
        grandTotal(): number {
            return this.subtotal + this.shipping + this.tax
        }
    },

    actions: {
        // Fetch cart from backend
        async fetchCart() {
            const userId = getLoggedInUserId()
            if (!userId) {
                this.items = []
                this.cartCount = 0
                return
            }

            this.loading = true
            try {
                const response = await getCart(userId)
                const rawItems = Array.isArray(response?.items) ? response.items : []
                this.items = rawItems.map(normalizeItem)
                this.cartCount = this.items.reduce(
                    (total, item) => total + (item.quantity || 0),
                    0
                )
            } catch (error) {
                console.error('Fetch cart error:', error)
                this.items = []
                this.cartCount = 0
            } finally {
                this.loading = false
            }
        },

        // Add product to cart
        async addItem(productId: string, quantity: number = 1) {
            const userId = getLoggedInUserId()
            const notify = useNotificationStore()

            if (!userId) {
                notify.warning('Please login to add items to your cart.', 'Authentication Required')
                throw new Error('User not logged in')
            }

            try {
                const response = await addToCart(userId, String(productId), quantity)
                const rawItems = Array.isArray(response?.items) ? response.items : []
                this.items = rawItems.map(normalizeItem)
                this.cartCount = this.items.reduce(
                    (total, item) => total + (item.quantity || 0),
                    0
                )
                notify.success('Item added to your cart successfully!', 'Cart Updated')
                return response
            } catch (error: any) {
                // 409 = item already in cart (unique constraint) → increment quantity instead
                if (error?.response?.status === 409) {
                    if (this.items.length === 0) {
                        try {
                            await this.fetchCart()
                        } catch {
                            // ignore fetch error
                        }
                    }
                    const existing = this.items.find(
                        (i) => i.productId === String(productId)
                    )
                    const newQty = (existing?.quantity ?? 1) + quantity
                    try {
                        const updated = await this.updateQuantity(String(productId), newQty)
                        notify.success('Cart quantity updated!', 'Cart Updated')
                        return updated
                    } catch (updateError: any) {
                        console.error('Update quantity after 409 error:', updateError)
                        const msg =
                            updateError?.message || 'Failed to update cart quantity'
                        notify.error(msg, 'Cart Error')
                        throw new Error(msg)
                    }
                }

                console.error('Add to cart error:', error)
                const message =
                    error?.response?.data?.message ||
                    error?.message ||
                    'Failed to add item to cart'
                notify.error(message, 'Cart Error')
                throw new Error(message)
            }
        },

        // Update item quantity in cart
        async updateQuantity(productId: string, quantity: number) {
            if (quantity <= 0) {
                return this.removeItem(productId)
            }

            const userId = getLoggedInUserId()
            if (!userId) {
                // Local fallback if not logged in
                const item = this.items.find(i => i.productId === String(productId))
                if (item) {
                    item.quantity = quantity
                    this.cartCount = this.items.reduce((total, i) => total + (i.quantity || 0), 0)
                }
                return
            }

            try {
                const response = await updateCartItemQuantity(userId, String(productId), quantity)
                const rawItems = Array.isArray(response?.items) ? response.items : []
                this.items = rawItems.map(normalizeItem)
                this.cartCount = this.items.reduce(
                    (total, item) => total + (item.quantity || 0),
                    0
                )
                return response
            } catch (error: any) {
                console.error('Update quantity error:', error)
                // If backend call fails, reload cart to stay in sync
                await this.fetchCart()
                const message =
                    error?.response?.data?.message ||
                    error?.message ||
                    'Failed to update quantity'
                throw new Error(message)
            }
        },

        // Remove item from cart
        async removeItem(productId: string) {
            const userId = getLoggedInUserId()
            if (!userId) {
                this.items = this.items.filter(i => i.productId !== String(productId))
                this.cartCount = this.items.reduce((total, i) => total + (i.quantity || 0), 0)
                return
            }

            try {
                const response = await removeCartItem(userId, String(productId))
                const rawItems = Array.isArray(response?.items) ? response.items : []
                this.items = rawItems.map(normalizeItem)
                this.cartCount = this.items.reduce(
                    (total, item) => total + (item.quantity || 0),
                    0
                )
                return response
            } catch (error: any) {
                console.error('Remove cart item error:', error)
                await this.fetchCart()
                const message =
                    error?.response?.data?.message ||
                    error?.message ||
                    'Failed to remove item'
                throw new Error(message)
            }
        },

        // Clear all items from cart
        async clearCart() {
            const userId = getLoggedInUserId()
            if (userId) {
                try {
                    await clearCustomerCart(userId)
                } catch (error) {
                    console.error('Clear cart error:', error)
                }
            }
            this.items = []
            this.cartCount = 0
        }
    }
})