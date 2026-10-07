import { createRouter, createWebHistory } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';
import HomeView from '../views/customer/HomeView.vue';

const routes: RouteRecordRaw[] = [
    { path: '/', name: 'Home', component: HomeView },
    { path: '/catalog', name: 'Catalog', component: () => import('../views/customer/CatalogView.vue') },
    { path: '/product/:id', name: 'ProductDetail', component: () => import('../views/customer/ProductDetailView.vue') },
    { path: '/products/:id', redirect: to => `/product/${to.params.id}` },
    { path: '/cart', name: 'Cart', component: () => import('../views/customer/CartView.vue') },
    { path: '/checkout', name: 'Checkout', component: () => import('../views/customer/CheckoutView.vue') },
    { path: '/profile', name: 'Profile', component: () => import('../views/customer/UserProfile.vue') },
    { path: '/orders', name: 'OrderHistory', component: () => import('../views/customer/OrderHistoryView.vue') },
    { path: '/orders/track', name: 'OrderTrack', component: () => import('../views/customer/OrderTrack.vue') },
    { path: '/deals', name: 'Deals', component: () => import('../views/customer/DealsView.vue') },
    { path: '/warranty', name: 'Warranty', component: () => import('../views/customer/ClamWarranty.vue') },
    { path: '/builds', name: 'CustomBuilds', component: () => import('../views/customer/CustomBuildPricePrediter.vue') },
    { path: '/builds23', redirect: '/builds' },
    { path: '/wishlist', name: 'Wishlist', component: () => import('../views/customer/WishlistView.vue') },
    { path: '/compare', name: 'Compare', component: () => import('../views/customer/CompareView.vue') },
    { path: '/coupons', redirect: '/compare' },
    { path: '/myOffers', name: 'MyOffers', component: () => import('../views/customer/MyOffersView.vue') },
    { path: '/returns', name: 'ReturnRequest', component: () => import('../views/customer/ReturnRequestView.vue') },
    { path: '/support', name: 'Support', component: () => import('../views/customer/SupportView.vue') },
    { path: '/login', name: 'Login', component: () => import('../views/auth/LoginView.vue') },
    { path: '/register', name: 'Register', component: () => import('../views/auth/RegisterView.vue') },
    { path: '/order-details/:id', name: 'OrderDetails', component: () => import('../views/customer/OrderDetails.vue') },
    { path: '/admin', redirect: '/admin/dashboard' },
    { path: '/admin/dashboard', name: 'adminDashboard', component: () => import('../views/admin/AdminDashboard.vue') },
    { path: '/admin/staff-login', name: 'adminLogin', component: () => import('../views/auth/admin/StaffLoginView.vue') },
    { path: '/admin/staff-register', name: 'adminRegister', component: () => import('../views/auth/admin/StaffRegister.vue') },
    { path: '/admin/profile', name: 'adminProfile', component: () => import('../views/admin/AdminProfile.vue') },
    { path: '/admin/discount-management', name: 'adminDiscounts', component: () => import('../views/admin/ManageDiscounts.vue') },
    { path: '/admin/rating-management', name: 'adminRatings', component: () => import('../views/admin/ManageReviewAndRating.vue') },
    { path: '/admin/products', name: 'ManageProducts', component: () => import('../views/admin/ManageProducts.vue') },
    { path: '/admin/delivery-management', name: 'ManageDelivery', component: () => import('../views/admin/ManageDelivery.vue') },
    { path: '/admin/customer-support', name: 'ManageCustomerSupport', component: () => import('../views/admin/ManageCustomerSupport.vue') },
    { path: '/admin/orders', name: 'ManageOrders', component: () => import('../views/admin/ManageOrders.vue') },
    { path: '/admin/customers', name: 'ManageCustomers', component: () => import('../views/admin/ManageCustomers.vue') },
    { path: '/admin/categories', name: 'ManageCategory', component: () => import('../views/admin/ManageCategory.vue') },
    { path: '/admin/inventory', name: 'ManageInventory', component: () => import('../views/admin/ManageInventory.vue') },
    { path: '/admin/coupons', name: 'ManageCoupons', component: () => import('../views/admin/ManageCoupons.vue') },
    { path: '/admin/returns-refunds', name: 'ManageReturns', component: () => import('../views/admin/ManageReturnAndRefund.vue') },
    { path: '/admin/system-users', name: 'ManageSystemUsers', component: () => import('../views/admin/ManageSystemUsers.vue') },
    { path: '/admin/analytics', name: 'AdminAnalytics', component: () => import('../views/admin/AdminAnalytics.vue') },
    { path: '/admin/reports', name: 'AdminReports', component: () => import('../views/admin/AdminReports.vue') },
    { path: '/admin/settings', name: 'AdminSettings', component: () => import('../views/admin/AdminSettings.vue') },

    { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('../views/NotFoundView.vue') },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

import { getActiveStaffRole, isRoleAllowed, ROLE_HOME_ROUTES } from '../utils/rbac';

router.beforeEach((to, _from, next) => {
    if (to.path.startsWith('/admin')) {
        if (to.path === '/admin/staff-login' || to.path === '/admin/staff-register') {
            return next();
        }

        const role = getActiveStaffRole();
        if (!role || role === 'CUSTOMER') {
            return next('/admin/staff-login');
        }

        if (to.path === '/admin') {
            const home = ROLE_HOME_ROUTES[role] || '/admin/dashboard';
            return next(home);
        }

        if (isRoleAllowed(role, to.path)) {
            return next();
        }

        const fallback = ROLE_HOME_ROUTES[role] || '/admin/staff-login';
        return next(fallback);
    }

    next();
});

export default router;