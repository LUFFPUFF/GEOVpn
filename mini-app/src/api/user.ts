import { apiClient } from './client';
import {
    UserResponse, UserStatsResponse, DeviceResponse,
    VpnConfigResponse, DeviceLimitStatus, ApiResponse, LeaderboardEntry,
} from '../types/api';

export const userApi = {
    getProfile: () => apiClient.get<ApiResponse<UserResponse>>('/users/me').then(r => r.data.data),

    getStats: () => apiClient.get<ApiResponse<UserStatsResponse>>('/users/me/stats').then(r => r.data.data),

    purchaseSubscription: (plan: string, months = 1, promo = false) =>
        apiClient.post<ApiResponse<UserResponse>>('/users/me/subscribe', null, {
            params: { plan: plan.toUpperCase(), months, promo }
        }).then(r => r.data.data),

    getDevices: () => apiClient.get<ApiResponse<DeviceResponse[]>>('/devices').then(r => r.data.data),

    registerDevice: (deviceName: string, deviceType: string) =>
        apiClient.post<ApiResponse<DeviceResponse>>('/devices', { deviceName, deviceType }).then(r => r.data.data),

    deleteDevice: (deviceId: number) => apiClient.delete(`/devices/${deviceId}`),

    getDeviceLimit: () => apiClient.get<ApiResponse<DeviceLimitStatus>>('/configs/limits/me').then(r => r.data.data),

    getConfigs: () => apiClient.get<ApiResponse<VpnConfigResponse[]>>('/configs/configs').then(r => r.data.data),

    createConfig: (deviceId: number, countryCode = 'FI') =>
        apiClient.post<ApiResponse<VpnConfigResponse>>('/configs', { deviceId, countryCode }).then(r => r.data.data),

    getLeaderboard: () => apiClient.get<ApiResponse<LeaderboardEntry[]>>('/users/leaderboard').then(r => r.data.data),

    syncDevice: (platform: string) =>
        apiClient.post<ApiResponse<DeviceResponse>>('/devices/sync', { platform }).then(r => r.data.data),

    register: (telegramId: number, firstName: string, username?: string, referralCode?: string) =>
        apiClient.post<ApiResponse<UserResponse>>('/users/register', {
            telegramId,
            firstName,
            username,
            referralCode
        }).then(r => r.data.data),

    syncOnStart: (platform: DeviceType): Promise<UserResponse> => {
        return apiClient.post<ApiResponse<UserResponse>>('/devices/sync', {
            platform
        }).then(r => r.data.data);
    }
};