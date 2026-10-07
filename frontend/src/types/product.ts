export interface Category {
    id: string | number;
    name: string;
    description?: string;
}

export interface Product {
    id: string | number;
    productId?: string | number;
    name: string;
    brand: string;
    sku: string;
    category: string;
    categoryId: string | number;
    price: number;
    stock: number;
    status: 'In Stock' | 'Low Stock' | 'Out of Stock';
    image: string;
    images: string[];
    description: string;
    rating: number;
    reviewCount: number;
    reviews: ProductReview[];
}

export interface ProductReview {
    id: string | number;
    rating: number;
    comment: string;
    customerName: string;
    createdAt?: string;
}