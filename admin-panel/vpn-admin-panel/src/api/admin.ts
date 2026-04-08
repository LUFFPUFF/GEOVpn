import axios from 'axios';
import {
    AdminDashboardResponse,
    UserResponse,
    DeviceResponse,
    UserStatsResponse,
    DeviceLimitStatus,
    ServerDto,
    CreateServerRequest,
    XrayUserStat,
    UserTrafficStatsDto,
    SystemHealthDto,
    ApiResponse
} from '../types/api';

export const apiClient = axios.create({
    baseURL: '/api/v1',
});

apiClient.interceptors.request.use((config) => {
    config.headers['X-Internal-Secret'] = '53a41724a2428714e21b2cbcbb19ce1ff62f4deb322037575f15f450791d54c3';
    config.headers['X-User-Id'] = '858441917';
    return config;
});

export const adminApi = {

    getDashboard: (): Promise<AdminDashboardResponse> =>
        apiClient.get('/admin/dashboard').then(res => res.data.data),

    getInfrastructureHealth: (): Promise<SystemHealthDto> =>
        apiClient.get('/servers/infrastructure/health').then(res => res.data.data),

    getUsers: (page = 0, size = 50): Promise<UserResponse[]> =>
        apiClient.get(`/admin/users?page=${page}&size=${size}`).then(res => res.data.data),

    getUserStats: (telegramId: number): Promise<UserStatsResponse> =>
        apiClient.get(`/admin/users/${telegramId}/stats`).then(res => res.data.data),

    addBalance: (telegramId: number, amount: number): Promise<any> =>
        apiClient.post(`/admin/users/${telegramId}/add-balance?amount=${amount}`).then(res => res.data.data),

    deactivateUser: (telegramId: number): Promise<void> =>
        apiClient.post(`/admin/users/${telegramId}/deactivate`).then(res => res.data.data),

    getUserDevices: (telegramId: number): Promise<DeviceResponse[]> =>
        apiClient.get(`/admin/users/${telegramId}/devices`).then(res => res.data.data),

    getDeviceLimit: (userId: number): Promise<DeviceLimitStatus> =>
        apiClient.get(`/admin/device-limits/${userId}`).then(res => res.data.data),

    registerDevice: (telegramId: number, data: { deviceName: string, deviceType: string }): Promise<DeviceResponse> =>
        apiClient.post('/devices', data, { headers: { 'X-User-Id': telegramId.toString() } }).then(res => res.data.data),

    changePlan: (userId: number, planName: string): Promise<DeviceLimitStatus> =>
        apiClient.put(`/admin/device-limits/${userId}/plan`, { planName }).then(res => res.data.data),

    getServers: (): Promise<ServerDto[]> =>
        apiClient.get('/servers').then(res => res.data.data),

    createServer: (payload: CreateServerRequest): Promise<ServerDto> =>
        apiClient.post('/servers', payload).then(res => res.data.data),

    updateServer: (id: number, payload: Partial<CreateServerRequest | { isActive: boolean }>): Promise<ServerDto> =>
        apiClient.put(`/servers/${id}`, payload).then(res => res.data.data),

    toggleServerStatus: (id: number, isActive: boolean): Promise<ServerDto> =>
        apiClient.put(`/servers/${id}`, { isActive }).then(res => res.data.data),

    deleteServer: (id: number): Promise<void> =>
        apiClient.delete(`/servers/${id}`).then(res => res.data.data),

    getServerUsers: (serverId: number): Promise<XrayUserStat[]> =>
        apiClient.get(`/servers/${serverId}/users`).then(res => res.data.data),

    getServerTrafficStats: (serverId: number): Promise<UserTrafficStatsDto[]> =>
        apiClient.get(`/traffic/servers/${serverId}/stats`).then(res => res.data.data),

    kickUser: (serverId: number, email: string): Promise<boolean> =>
        apiClient.post(`/servers/${serverId}/users/${email}/kick`).then(res => res.data.data),

    getDeviceConfig: (deviceId: number): Promise<any> =>
        apiClient.get(`/admin/devices/${deviceId}/config`).then(res => res.data.data),

    regenerateConfig: (deviceId: number): Promise<any> =>
        apiClient.put(`/configs/${deviceId}/regenerate`, { reason: "Admin reset" }).then(res => res.data.data),
};