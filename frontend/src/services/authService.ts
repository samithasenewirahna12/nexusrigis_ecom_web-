import api from './api.ts';
import type { AuthResponse } from '../types/user.ts';

export const authService = {
    async login(credentials: Record<string, string>): Promise<any> {
        const res = await api.post('/auth/login', credentials);
        return res.data;
    },
    async register(data: Record<string, any>): Promise<any> {
        const res = await api.post('/customers', data);
        return res.data;
    },
};