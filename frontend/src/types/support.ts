export interface SupportTicket {
    id: number;
    subject: string;
    message: string;
    status: 'OPEN' | 'IN_PROGRESS' | 'CLOSED';
    createdAt: string;
}

export interface Review {
    id: number;
    productId: number;
    userName: string;
    rating: number;
    comment: string;
    createdAt: string;
}