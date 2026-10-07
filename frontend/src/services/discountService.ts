import api from './api.ts';
import type { Coupon } from '../types/discount.ts';

export const discountService = {
    async applyCoupon(code: string): Promise<Coupon> {
        const res = await api.post<Coupon>('/coupons/validate', { code });
        return res.data;
    },
};