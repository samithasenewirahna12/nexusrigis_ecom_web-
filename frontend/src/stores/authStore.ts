import { defineStore } from 'pinia';
import { ref } from 'vue';
import { authService } from '../services/authService';

export const useAuthStore = defineStore('auth', () => {
    const rawUser = sessionStorage.getItem('user') || localStorage.getItem('user');
    let initialUser: any = null;
    try {
        initialUser = rawUser ? JSON.parse(rawUser) : null;
    } catch {
        initialUser = null;
    }

    const user = ref<any>(initialUser);
    const token = ref<string | null>(localStorage.getItem('jwt_token'));

    function setUser(userData: any) {
        user.value = userData;
        if (userData) {
            const cleanUser = { ...userData };
            delete cleanUser.password;
            sessionStorage.setItem('user', JSON.stringify(cleanUser));
            localStorage.setItem('user', JSON.stringify(cleanUser));
            const uid = String(cleanUser.userId || cleanUser.customerId || cleanUser.id || '');
            if (uid) {
                sessionStorage.setItem('userId', uid);
                localStorage.setItem('userId', uid);
            }
            sessionStorage.setItem('isLoggedIn', 'true');
            localStorage.setItem('isLoggedIn', 'true');
        } else {
            sessionStorage.removeItem('user');
            localStorage.removeItem('user');
            sessionStorage.removeItem('userId');
            localStorage.removeItem('userId');
            sessionStorage.removeItem('isLoggedIn');
            localStorage.removeItem('isLoggedIn');
        }
    }

    async function login(credentials: Record<string, string>) {
        const data = await authService.login(credentials);
        const userData = data.user || data;
        token.value = data.token || 'session_token';
        setUser(userData);
        if (data.token) {
            localStorage.setItem('jwt_token', data.token);
        }
        return userData;
    }

    async function register(customerData: Record<string, any>) {
        const data = await authService.register(customerData);
        setUser(data);
        return data;
    }

    function logout() {
        token.value = null;
        setUser(null);
        localStorage.removeItem('jwt_token');
    }

    return { user, token, setUser, login, register, logout };
});