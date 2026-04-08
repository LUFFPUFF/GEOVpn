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
    SystemHealthDto
} from '../types/api';

const api = axios.create({
    baseURL: 'http://localhost:8080/api/v1',
});

api.interceptors.request.use(config => {
    config.headers['X-User-Id'] = '858441917';
    return config;
});

export const adminApi = {
    getDashboard: (): Promise<AdminDashboardResponse> =>
        api.get('/admin/dashboard').then(res => res.data.data),

    getInfrastructureHealth: (): Promise<SystemHealthDto> =>
        api.get('/servers/infrastructure/health').then(res => res.data.data),

    getUsers: (page = 0, size = 50): Promise<UserResponse[]> =>
        api.get(`/admin/users?page=${page}&size=${size}`).then(res => res.data.data),

    getUserStats: (telegramId: number): Promise<UserStatsResponse> =>
        api.get(`/admin/users/${telegramId}/stats`).then(res => res.data.data),

    addBalance: (telegramId: number, amount: number): Promise<any> =>
        api.post(`/admin/users/${telegramId}/add-balance?amount=${amount}`).then(res => res.data.data),

    deactivateUser: (telegramId: number): Promise<void> =>
        api.post(`/admin/users/${telegramId}/deactivate`).then(res => res.data.data),

    getUserDevices: (telegramId: number): Promise<DeviceResponse[]> =>
        api.get(`/admin/users/${telegramId}/devices`).then(res => res.data.data),

    getDeviceLimit: (userId: number): Promise<DeviceLimitStatus> =>
        api.get(`/admin/device-limits/${userId}`).then(res => res.data.data),

    registerDevice: (telegramId: number, data: { deviceName: string, deviceType: string }): Promise<DeviceResponse> =>
        api.post('/devices', data, { headers: { 'X-User-Id': telegramId.toString() } }).then(res => res.data.data),

    changePlan: (userId: number, planName: string): Promise<DeviceLimitStatus> =>
        api.put(`/admin/device-limits/${userId}/plan`, { planName }).then(res => res.data.data),

    getServers: (): Promise<ServerDto[]> =>
        api.get('/servers').then(res => res.data.data),

    createServer: (payload: CreateServerRequest): Promise<ServerDto> =>
        api.post('/servers', payload).then(res => res.data.data),

    updateServer: (id: number, payload: Partial<CreateServerRequest | { isActive: boolean }>): Promise<ServerDto> =>
        api.put(`/servers/${id}`, payload).then(res => res.data.data),

    toggleServerStatus: (id: number, isActive: boolean): Promise<ServerDto> =>
        api.put(`/servers/${id}`, { isActive }).then(res => res.data.data),

    deleteServer: (id: number): Promise<void> =>
        api.delete(`/servers/${id}`).then(res => res.data.data),

    getServerUsers: (serverId: number): Promise<XrayUserStat[]> =>
        api.get(`/servers/${serverId}/users`).then(res => res.data.data),

    getServerTrafficStats: (serverId: number): Promise<UserTrafficStatsDto[]> =>
        api.get(`/traffic/servers/${serverId}/stats`).then(res => res.data.data),

    kickUser: (serverId: number, email: string): Promise<boolean> =>
        api.post(`/servers/${serverId}/users/${email}/kick`).then(res => res.data.data),

    getDeviceConfig: (deviceId: number): Promise<any> =>
        api.get(`/admin/devices/${deviceId}/config`).then(res => res.data.data),

    regenerateConfig: (deviceId: number): Promise<any> =>
        api.put(`/configs/${deviceId}/regenerate`, { reason: "Admin reset" }).then(res => res.data.data),
};