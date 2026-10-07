export interface Coupon {
    id: number;
    code: string;
    discountPercent: number;
    validUntil: string;
    active: boolean;
}