import api from './api.ts';

export interface DealItem {
    dealId: string;
    productId: string;
    productName: string;
    productOriginalPrice: number;
    productImage: string;
    discountPercentage: number;
    badgeText: string;
    startDate?: string;
    endDate?: string;
}

export const dealService = {
    async getAll(): Promise<DealItem[]> {
        const res = await api.get('/deals');
        const list = Array.isArray(res.data) ? res.data : (res.data?.data || res.data?.content || []);
        return list.map((d: any) => ({
            dealId: d.dealId || '',
            productId: d.productId || '',
            productName: d.productName || '',
            productOriginalPrice: Number(d.productOriginalPrice || 0),
            productImage: d.productImage || '',
            discountPercentage: Number(d.discountPercentage || 0),
            badgeText: d.badgeText || 'FLASH DEAL',
            startDate: d.startDate || '',
            endDate: d.endDate || ''
        }));
    }
};
