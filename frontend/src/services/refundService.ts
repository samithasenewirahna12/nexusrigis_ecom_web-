import api from './api.ts';
import type { ReturnRequest } from '../types/refund.ts';

export const refundService = {
    async requestReturn(orderId: number, reason: string): Promise<ReturnRequest> {
        const res = await api.post<ReturnRequest>('/returns', { orderId, reason });
        return res.data;
    },
};