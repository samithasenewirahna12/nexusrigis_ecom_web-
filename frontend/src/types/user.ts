export type Role = 'CUSTOMER' | 'ADMIN' | 'SUPPORT' | 'WAREHOUSE' | 'DELIVERY' | 'Customer' | 'Administrator' | 'Support Staff' | 'Warehouse Staff' | 'Delivery Staff' | string;

export interface User {
    id?: string | number;
    userId?: string;
    email: string;
    name: string;
    role?: Role;
    phone?: string;
    userImage?: string;
    address?: any;
    registeredDate?: string;
}

export interface AuthResponse {
    token?: string;
    user?: User;
    userId?: string;
    name?: string;
    email?: string;
    role?: Role;
}