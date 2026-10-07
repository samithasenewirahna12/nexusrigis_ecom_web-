export interface ReturnRequest {
    id: number;
    orderId: number;
    reason: string;
    status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'REFUNDED';
    createdAt: string;
}