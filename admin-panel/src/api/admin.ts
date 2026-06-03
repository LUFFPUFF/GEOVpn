import axios from 'axios';
import {
    AdminDashboardResponse,
    UserResponse,
    DeviceResponse,
    UserStatsResponse,
    DeviceLimitStatus,
    AdminConfigDetailResponse
} from '../types/api';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api/v1";
const INTERNAL_SECRET = import.meta.env.VITE_INTERNAL_SECRET || "";
const ADMIN_TOKEN = import.meta.env.VITE_ADMIN_TOKEN || "";
const DEFAULT_ADMIN_ID = import.meta.env.VITE_DEFAULT_ADMIN_ID || "1";

export const apiClient = axios.create({
    baseURL: BASE_URL,
});

apiClient.interceptors.request.use((config) => {
    if (INTERNAL_SECRET) {
        config.headers['X-Internal-Secret'] = INTERNAL_SECRET;
    }

    if (ADMIN_TOKEN) {
        config.headers['X-Admin-Token'] = ADMIN_TOKEN;
    }

    if (DEFAULT_ADMIN_ID) {
        config.headers['X-User-Id'] = DEFAULT_ADMIN_ID;
    }

    return config;
});

apiClient.interceptors.response.use(
    (response) => {
        return response.data.data;
    },
    (error) => Promise.reject(error.response?.data || error)
);

export const adminApi = {
    getDashboard: (): Promise<AdminDashboardResponse> => {
        return apiClient.get('/admin/dashboard');
    },

    getUsers: (page = 0, size = 50, search = ""): Promise<UserResponse[]> => {
        return apiClient.get('/admin/users', { params: { page, size, search } });
    },

    getUserStats: (telegramId: number): Promise<UserStatsResponse> => {
        return apiClient.get(`/admin/users/${telegramId}/stats`);
    },

    getUserDevices: (telegramId: number): Promise<DeviceResponse[]> => {
        return apiClient.get(`/admin/users/${telegramId}/devices`);
    },

    addBalance: (telegramId: number, amount: number): Promise<UserResponse> => {
        if (amount < 0) {
            return apiClient.post(`/admin/users/${telegramId}/deduct-balance`, null, {
                params: { amount: Math.abs(amount) }
            });
        }
        return apiClient.post(`/admin/users/${telegramId}/add-balance`, null, {
            params: { amount }
        });
    },

    getDeviceLimit: (userId: number): Promise<DeviceLimitStatus> => {
        return apiClient.get(`/admin/device-limits/${userId}`);
    },

    changePlan: (userId: number, planName: string): Promise<DeviceLimitStatus> => {
        return apiClient.put(`/admin/device-limits/${userId}/plan`, { planName });
    },

    banUser: (telegramId: number, reason: string): Promise<any> => {
        return apiClient.post(`/configs/admin/sync/ban/${telegramId}`, null, {
            params: { reason }
        });
    },

    unbanUser: (telegramId: number): Promise<any> => {
        return apiClient.post(`/configs/admin/sync/unban/${telegramId}`);
    },

    createUser: (user: any): Promise<UserResponse> => {
        return apiClient.post('/users/register', user);
    },
    updateUserFields: (telegramId: number, fields: any): Promise<UserResponse> => {
        return apiClient.put(`/admin/users/${telegramId}`, fields);
    },
    deleteUser: (telegramId: number): Promise<void> => {
        return apiClient.delete(`/admin/users/${telegramId}`);
    },

    getBanLogs: (): Promise<any[]> => {
        return Promise.resolve([]);
    },

    getDeviceConfig: (deviceId: number): Promise<any> => {
        return apiClient.get(`/configs/admin/devices/${deviceId}/config`);
    },

    regenerateConfig: (deviceId: number): Promise<any> => {
        return apiClient.put(`/configs/${deviceId}/regenerate`, { reason: "Admin reset" });
    },

    registerDevice: (telegramId: number, data: { deviceName: string, deviceType: string }): Promise<DeviceResponse> => {
        return apiClient.post('/devices', data, {
            headers: { 'X-User-Id': telegramId.toString() }
        });
    },

    deleteDevice: (deviceId: number): Promise<void> => {
        return apiClient.delete(`/devices/internal/${deviceId}`);
    },

    getAdminConfigDetails: (deviceId: number): Promise<AdminConfigDetailResponse> => {
        return apiClient.get(`/configs/admin/devices/${deviceId}/config/details`);
    },

    updateDeviceConfig: (deviceId: number, data: {
        vlessLinks: any[];
        relayLinks: any[];
        hy2Links: string[];
    }): Promise<AdminConfigDetailResponse> => {
        return apiClient.put(`/configs/admin/devices/${deviceId}/config`, data);
    },

    getUserTransactions: (userId: number): Promise<TransactionResponse[]> => {
        return apiClient.get(`/billing/admin/transactions/${userId}`);
    },

    forceSync: (): Promise<string> => {
        return apiClient.post('/configs/admin/maintenance/global-reset');
    },

    getInfrastructureHealth: (): Promise<SystemHealthDto> => {
        return apiClient.get('/servers/infrastructure/health');
    },

    getRevenueStats: (days: number): Promise<RevenueStat[]> => {
        return apiClient.get(`/billing/admin/revenue-stats`, { params: { days } });
    },
};