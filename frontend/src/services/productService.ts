import api from './api.ts';
import type { Product, Category } from '../types/product.ts';

export const productService = {
    async getAll(): Promise<Product[]> {
        const res = await api.get<any>('/products');
        const list = Array.isArray(res.data) ? res.data : (res.data?.data || res.data?.products || []);
        return list.map((p: any) => ({
            ...p,
            id: p.productId ?? p.id,
            productId: p.productId ?? p.id,
            stock: Number(p.stockQty ?? p.stock ?? 0),
            stockQty: Number(p.stockQty ?? p.stock ?? 0),
            category: p.categoryName || (typeof p.category === 'string' ? p.category : p.category?.categoryName || p.category?.name || ''),
            categoryName: p.categoryName || (typeof p.category === 'string' ? p.category : p.category?.categoryName || p.category?.name || ''),
            categoryId: p.categoryId ?? (typeof p.category === 'object' ? p.category?.categoryId || p.category?.id : undefined),
            image: (Array.isArray(p.images) && p.images[0]) || p.image || '',
            images: Array.isArray(p.images) ? p.images : (p.image ? [p.image] : []),
            description: p.description || p.discription || '',
            discription: p.description || p.discription || '',
            price: Number(p.price || 0)
        }));
    },
    async getById(id: string | number): Promise<Product> {
        const res = await api.get<any>(`/products/${id}`);
        const p = res.data;
        if (!p) return p;
        return {
            ...p,
            id: p.productId ?? p.id,
            productId: p.productId ?? p.id,
            stock: Number(p.stockQty ?? p.stock ?? 0),
            stockQty: Number(p.stockQty ?? p.stock ?? 0),
            category: p.categoryName || (typeof p.category === 'string' ? p.category : p.category?.categoryName || p.category?.name || ''),
            categoryName: p.categoryName || (typeof p.category === 'string' ? p.category : p.category?.categoryName || p.category?.name || ''),
            categoryId: p.categoryId ?? (typeof p.category === 'object' ? p.category?.categoryId || p.category?.id : undefined),
            image: (Array.isArray(p.images) && p.images[0]) || p.image || '',
            images: Array.isArray(p.images) ? p.images : (p.image ? [p.image] : []),
            description: p.description || p.discription || '',
            discription: p.description || p.discription || '',
            price: Number(p.price || 0)
        };
    },
    async getCategories(): Promise<Category[]> {
        const res = await api.get<any>('/categories');
        const list = Array.isArray(res.data) ? res.data : (res.data?.data || res.data?.categories || []);
        return list.map((c: any) => ({
            ...c,
            id: c.categoryId ?? c.id,
            categoryId: c.categoryId ?? c.id,
            name: c.categoryName ?? c.name ?? '',
            categoryName: c.categoryName ?? c.name ?? '',
            description: c.description || '',
            categoryImage: c.categoryImage || c.image || '',
            image: c.categoryImage || c.image || ''
        }));
    },
    async createProduct(product: Partial<Product>): Promise<Product> {
        const res = await api.post<Product>('/products', product);
        return res.data;
    },
};