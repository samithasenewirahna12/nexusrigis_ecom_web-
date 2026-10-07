import type { Product } from './product';

export interface CartItem {
    id: number;
    product: Product;
    quantity: number;
}

export interface WishlistItem {
    id: number;
    product: Product;
}