import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { Product } from '../types/product.ts';
import { productService } from '../services/productService.ts';
import { getAllReviews } from '../services/reviewService.ts';

export const useProductStore = defineStore('product', () => {
    const products = ref<Product[]>([]);
    const loading = ref(false);

    async function loadProducts() {
        loading.value = true;
        try {
            const [prodsRes, reviewsRes] = await Promise.allSettled([
                productService.getAll(),
                getAllReviews()
            ]);

            const prods = prodsRes.status === 'fulfilled' ? prodsRes.value : [];
            const allReviews = reviewsRes.status === 'fulfilled' && Array.isArray(reviewsRes.value) ? reviewsRes.value : [];

            const reviewMap = new Map<string, { ratings: number[]; count: number; avg: number; reviews: any[] }>();
            for (const r of allReviews) {
                const pid = String(r.productId ?? '').trim().toLowerCase();
                if (!pid) continue;
                const score = Number(r.rating);
                if (!Number.isFinite(score) || score <= 0) continue;
                let entry = reviewMap.get(pid);
                if (!entry) {
                    entry = { ratings: [], count: 0, avg: 0, reviews: [] };
                    reviewMap.set(pid, entry);
                }
                entry.ratings.push(score);
                entry.count++;
                entry.reviews.push(r);
            }
            for (const entry of reviewMap.values()) {
                if (entry.count > 0) {
                    entry.avg = entry.ratings.reduce((a, b) => a + b, 0) / entry.count;
                }
            }

            products.value = prods.map((p) => {
                const key = String(p.productId ?? p.id ?? '').trim().toLowerCase();
                const revData = reviewMap.get(key);
                const avgRating = revData && revData.count > 0 ? Number(revData.avg.toFixed(1)) : (Number(p.rating) || 0);
                const revCount = revData ? revData.count : (Number(p.reviewCount) || 0);
                const revList = revData ? revData.reviews : (p.reviews || []);

                return {
                    ...p,
                    rating: avgRating,
                    reviewCount: revCount,
                    reviews: revList
                };
            });
        } finally {
            loading.value = false;
        }
    }

    return { products, loading, loadProducts };
});