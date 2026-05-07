import { apiClient } from './client';
import {
    UserResponse, UserStatsResponse, DeviceResponse,
    VpnConfigResponse, DeviceLimitStatus, ApiResponse, LeaderboardEntry,
} from '../types/api';

export const userApi = {
    getProfile: (): Promise<UserResponse> =>
        apiClient.get<ApiResponse<UserResponse>>('/users/me').then(r => r.data.data),

    getStats: (): Promise<UserStatsResponse> =>
        apiClient.get<ApiResponse<UserStatsResponse>>('/users/me/stats').then(r => r.data.data),

    purchaseSubscription: (plan: string, months = 1, promo = false): Promise<UserResponse> => {
        return apiClient.post<ApiResponse<UserResponse>>('/users/me/subscribe', null, {
            params: {
                plan: plan.toUpperCase(),
                months: months,
                promo: promo
            }
        }).then(r => r.data.data);
    },

    getDevices: (): Promise<DeviceResponse[]> =>
        apiClient.get<ApiResponse<DeviceResponse[]>>('/devices').then(r => r.data.data),

    registerDevice: (deviceName: string, deviceType: string): Promise<DeviceResponse> =>
        apiClient.post<ApiResponse<DeviceResponse>>('/devices', { deviceName, deviceType })
            .then(r => r.data.data),

    deleteDevice: (deviceId: number): Promise<void> =>
        apiClient.delete(`/devices/${deviceId}`).then(() => undefined),

    getDeviceLimit: (): Promise<DeviceLimitStatus> =>
        apiClient.get<ApiResponse<DeviceLimitStatus>>('/configs/limits/me').then(r => r.data.data),

    getConfigs: (): Promise<VpnConfigResponse[]> =>
        apiClient.get<ApiResponse<VpnConfigResponse[]>>('/configs/configs').then(r => r.data.data),

    createConfig: (deviceId: number, countryCode = 'NL'): Promise<VpnConfigResponse> => {
        return apiClient.post<ApiResponse<VpnConfigResponse>>('/configs', {
            deviceId,
            countryCode
        }).then(r => r.data.data);
    },

    getConfigByDevice: (deviceId: number): Promise<VpnConfigResponse> =>
        apiClient.get<ApiResponse<VpnConfigResponse>>(`/configs/configs/${deviceId}`)
            .then(r => r.data.data),

    getLeaderboard: (): Promise<LeaderboardEntry[]> =>
        apiClient.get<ApiResponse<LeaderboardEntry[]>>('/users/leaderboard').then(r => r.data.data),

    syncDevice: (platform: string): Promise<DeviceResponse> => {
        return apiClient.post<ApiResponse<DeviceResponse>>('/devices/sync', { platform })
            .then(r => r.data.data);
    }
};