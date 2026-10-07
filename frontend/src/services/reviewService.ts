import api from './api'

export interface ReviewCreatePayload {
    customerId: string
    productId: string
    rating: number
    comment: string
}

export interface ReviewResponse {
    reviewId: string
    rating: number
    comment: string
    reviewDate: string
    customerId: string
    customerName: string
    productId: string
}

/** Submit a new review */
export async function createReview(payload: ReviewCreatePayload): Promise<ReviewResponse> {
    const response = await api.post('/reviews', payload)
    return response.data
}

/** Get all reviews for a product */
export async function getReviewsByProduct(productId: string): Promise<ReviewResponse[]> {
    const response = await api.get('/reviews', { params: { productId } })
    return Array.isArray(response.data) ? response.data : []
}

/** Get all reviews by a customer */
export async function getReviewsByCustomer(customerId: string): Promise<ReviewResponse[]> {
    const response = await api.get('/reviews', { params: { customerId } })
    return Array.isArray(response.data) ? response.data : []
}

/** Delete a review by ID */
export async function deleteReview(reviewId: string): Promise<void> {
    await api.delete(`/reviews/${reviewId}`)
}

/** Get all reviews */
export async function getAllReviews(): Promise<ReviewResponse[]> {
    const response = await api.get('/reviews')
    return Array.isArray(response.data) ? response.data : (response.data?.data || [])
}

