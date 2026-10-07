import api from './api';
import type { SupportTicket } from '../types/support';

export interface SupportInquiryRequest {
    subject: string;
    message: string;
    category?: string;
    priority?: string;
    customerId?: string;
    customerName?: string;
    customerEmail?: string;
    customerPhone?: string;
}

export const supportService = {
    // Get tickets for logged in customer
    async getTickets(customerId?: string): Promise<SupportTicket[]> {
        const params: Record<string, string> = {};
        if (customerId) {
            params.customerId = customerId;
        }
        const response = await api.get('/support/tickets', { params });
        return response.data;
    },

    // Create a new support ticket
    async createTicket(data: SupportInquiryRequest): Promise<SupportTicket> {
        const response = await api.post('/support/tickets', data);
        return response.data;
    },

    // Get all support inquiries (Admin / Staff view)
    async getAllInquiries(): Promise<any[]> {
        const response = await api.get('/support/inquiries');
        return response.data;
    },

    // Reply / Post message to inquiry (Customer or Staff)
    async replyInquiry(inquiryId: string | number, reply: string, supportStaffId?: string, sender: 'CUSTOMER' | 'STAFF' = 'STAFF', senderName?: string): Promise<any> {
        const response = await api.post(`/support/inquiries/${inquiryId}/reply`, {
            reply,
            message: reply,
            sender,
            senderName,
            supportStaffId
        });
        return response.data;
    },

    // Post message from customer or staff
    async sendMessage(inquiryId: string | number, message: string, sender: 'CUSTOMER' | 'STAFF' = 'CUSTOMER', senderName?: string, supportStaffId?: string): Promise<any> {
        const response = await api.post(`/support/tickets/${inquiryId}/reply`, {
            reply: message,
            message,
            sender,
            senderName,
            supportStaffId
        });
        return response.data;
    },

    // Assign staff to inquiry
    async assignStaff(inquiryId: string | number, supportStaffId: string): Promise<any> {
        const response = await api.put(`/support/inquiries/${inquiryId}/assign`, {
            supportStaffId
        });
        return response.data;
    },

    // Update status
    async updateStatus(inquiryId: string | number, status: string): Promise<any> {
        const response = await api.put(`/support/inquiries/${inquiryId}/status`, {
            status
        });
        return response.data;
    },

    // Delete inquiry
    async deleteInquiry(inquiryId: string | number): Promise<any> {
        const response = await api.delete(`/support/inquiries/${inquiryId}`);
        return response.data;
    }
};

export default supportService;
