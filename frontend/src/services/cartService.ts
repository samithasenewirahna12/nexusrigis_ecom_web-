import api from './api'

export async function getCart(
    customerId: string
) {
    const response = await api.get(
        `/customers/${customerId}/cart`
    )

    return response.data
}

export async function addToCart(
    customerId: string,
    productId: string,
    quantity: number = 1
) {
    const response = await api.post(
        `/customers/${customerId}/cart/items`,
        {
            productId: productId,
            quantity: quantity
        }
    )

    return response.data
}

export async function removeCartItem(
    customerId: string,
    productId: string
) {
    const response = await api.delete(
        `/customers/${customerId}/cart/items/${productId}`
    )

    return response.data
}

export async function updateCartItemQuantity(
    customerId: string,
    productId: string,
    quantity: number
) {
    const response = await api.put(
        `/customers/${customerId}/cart/items/${productId}`,
        {
            quantity: quantity
        }
    )

    return response.data
}

export async function clearCustomerCart(
    customerId: string
) {
    const response = await api.delete(
        `/customers/${customerId}/cart`
    )

    return response.data
}