import { apiClient } from './client';
import { ApiResponse, DepositResponse } from '../types/api';

export const billingApi = {
    createDeposit: (amount: number) =>
        apiClient.post<ApiResponse<DepositResponse>>('/billing/deposit', { amount })
            .then(r => r.data.data)
};