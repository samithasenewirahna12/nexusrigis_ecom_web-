import api from './api.ts';
import type { Order } from '../types/order.ts';

export const orderService = {
    async checkout(orderPayload: object): Promise<Order> {
        const res = await api.post<Order>('/orders', orderPayload);
        return res.data;
    },
    async createOrder(orderPayload: object): Promise<Order> {
        const res = await api.post<Order>('/orders', orderPayload);
        return res.data;
    },
    async getOrders(customerId?: string): Promise<Order[]> {
        const res = await api.get<Order[]>('/orders', {
            params: customerId ? { customerId } : undefined
        });
        return res.data;
    },
    async getOrderById(id: string): Promise<Order> {
        const res = await api.get<Order>(`/orders/${id}`);
        return res.data;
    },
    async cancelOrder(id: string): Promise<Order> {
        const res = await api.put<Order>(`/orders/${id}/cancel`);
        return res.data;
    }
};