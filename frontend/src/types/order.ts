import type { Product } from './product';

export interface OrderItem {
    id: number;
    product: Product;
    quantity: number;
    priceAtPurchase: number;
}

export interface Order {
    id: number;
    orderDate: string;
    status: 'Pending' | 'Order Confirmed' | 'Picked & Packed' | 'In Transit' | 'Out for Delivery' | 'Delivered' | 'Cancelled' | 'PENDING' | 'ORDER_CONFIRMED' | 'PICKED_AND_PACKED' | 'IN_TRANSIT' | 'OUT_FOR_DELIVERY' | 'DELIVERED' | 'CANCELLED';
    totalAmount: number;
    items: OrderItem[];
}