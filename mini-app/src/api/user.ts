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

    deleteDevice: (deviceUuid: string) => apiClient.delete(`/devices/${deviceUuid}/permanent`),

    getDeviceLimit: () => apiClient.get<ApiResponse<DeviceLimitStatus>>('/configs/limits/me').then(r => r.data.data),

    getConfigs: () => apiClient.get<ApiResponse<VpnConfigResponse[]>>('/configs/configs').then(r => r.data.data),

    createConfig: (deviceId: number, countryCode = 'FI') =>
        apiClient.post<ApiResponse<VpnConfigResponse>>('/configs', { deviceId, countryCode }).then(r => r.data.data),

    revokeConfig: (deviceId: number) => apiClient.delete(`/configs/configs/${deviceId}`),

    getLeaderboard: () => apiClient.get<ApiResponse<LeaderboardEntry[]>>('/users/leaderboard').then(r => r.data.data),

    register: (telegramId: number, firstName: string, username?: string, referralCode?: string) =>
        apiClient.post<ApiResponse<UserResponse>>('/users/register', {
            telegramId,
            firstName,
            username,
            referralCode
        }).then(r => r.data.data),

    applyPromo: (code: string) =>
        apiClient.post<ApiResponse<UserResponse>>('/users/me/apply-promo', null, {
            params: { code }
        }).then(r => r.data.data),

    updateReferralCode: (code: string) =>
        apiClient.put<ApiResponse<UserResponse>>('/users/me/referral-code', null, {
            params: { code }
        }).then(r => r.data.data),

    claimEasterEgg: () =>
        apiClient.post<ApiResponse<UserResponse>>('/users/me/easter-egg').then(r => r.data.data),

    purchaseExtraSlot: () => apiClient.post<ApiResponse<UserResponse>>('/users/me/purchase-slot').then(r => r.data.data),

    regenerateConfig: (deviceId: number) =>
        apiClient.put<ApiResponse<VpnConfigResponse>>(`/configs/${deviceId}/regenerate`, { preferredCountry: 'RU' }).then(r => r.data.data),

    checkMembership: () => apiClient.get<ApiResponse<boolean>>('/users/me/membership').then(r => r.data.data),

    getInitData: () => apiClient.get<ApiResponse<UserInitResponse>>('/users/me/init').then(r => r.data.data),
};