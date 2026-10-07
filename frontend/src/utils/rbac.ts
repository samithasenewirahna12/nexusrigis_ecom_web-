/**
 * Centralized Role-Based Access Control (RBAC) definitions & helpers.
 */

export type StaffRole =
    | 'ADMINISTRATOR'
    | 'SUPPORT_STAFF'
    | 'DELIVERY_STAFF'
    | 'WAREHOUSE_STAFF'

export const ROLE_ALLOWED_ROUTES: Record<string, string[]> = {
    ADMINISTRATOR: [
        '/admin',
        '/admin/dashboard',
        '/admin/products',
        '/admin/orders',
        '/admin/customers',
        '/admin/categories',
        '/admin/inventory',
        '/admin/coupons',
        '/admin/rating-management',
        '/admin/discount-management',
        '/admin/returns-refunds',
        '/admin/customer-support',
        '/admin/delivery-management',
        '/admin/system-users',
        '/admin/profile',
        '/admin/analytics',
        '/admin/reports',
        '/admin/settings'
    ],
    SUPPORT_STAFF: [
        '/admin/customer-support',
        '/admin/returns-refunds',
        '/admin/rating-management',
        '/admin/profile'
    ],
    DELIVERY_STAFF: [
        '/admin/delivery-management',
        '/admin/profile'
    ],
    WAREHOUSE_STAFF: [
        '/admin/products',
        '/admin/categories',
        '/admin/inventory',
        '/admin/coupons',
        '/admin/discount-management',
        '/admin/profile'
    ]
}

export const ROLE_HOME_ROUTES: Record<string, string> = {
    ADMINISTRATOR: '/admin/dashboard',
    SUPPORT_STAFF: '/admin/customer-support',
    DELIVERY_STAFF: '/admin/delivery-management',
    WAREHOUSE_STAFF: '/admin/products'
}

/** Retrieve the active staff user object from session or local storage */
export function getActiveStaffUser(): any | null {
    try {
        const raw =
            sessionStorage.getItem('staffUser') ||
            sessionStorage.getItem('user') ||
            localStorage.getItem('staffUser') ||
            localStorage.getItem('user')

        if (raw) {
            return JSON.parse(raw)
        }
    } catch {
        // ignore JSON parse error
    }
    return null
}

/** Retrieve the active staff role in UPPERCASE format (e.g. ADMINISTRATOR) */
export function getActiveStaffRole(): string {
    const user = getActiveStaffUser()
    if (user && (user.role || user.userRole)) {
        return String(user.role || user.userRole).toUpperCase()
    }
    const direct =
        sessionStorage.getItem('role') ||
        sessionStorage.getItem('userRole') ||
        localStorage.getItem('role') ||
        ''
    return String(direct).toUpperCase()
}

/** Check if a given role is allowed to access a specific route path */
export function isRoleAllowed(role: string, path: string): boolean {
    if (!role) return false
    const cleanRole = role.toUpperCase()
    if (cleanRole === 'ADMINISTRATOR') return true

    const allowed = ROLE_ALLOWED_ROUTES[cleanRole]
    if (!allowed) return false

    // Normalize path by removing trailing slash
    const normalized = path.replace(/\/+$/, '')
    return allowed.some(p => p.replace(/\/+$/, '') === normalized)
}

/** Check if the currently logged in staff member can access a given route */
export function canCurrentStaffAccess(path: string): boolean {
    const role = getActiveStaffRole()
    return isRoleAllowed(role, path)
}

/** Check if a user represents a Super Admin */
export function isSuperAdmin(
    user: {
        accessLevel?: string | null
        role?: string | null
        email?: string | null
        name?: string | null
        id?: string | null
        userId?: string | null
    } | null | undefined
): boolean {
    if (!user) return false

    const access = String(user.accessLevel || '').toUpperCase()
    const role = String(user.role || '').toUpperCase()
    const email = String(user.email || '').toLowerCase()
    const name = String(user.name || '').toLowerCase()
    const id = String(user.id || user.userId || '').toUpperCase()

    return (
        access === 'SUPER_ADMIN' ||
        access === 'FULL_ACCESS' ||
        role === 'SUPER_ADMIN' ||
        role.includes('SUPER') ||
        email.includes('superadmin') ||
        name.includes('super admin') ||
        id === 'ADM001'
    )
}
