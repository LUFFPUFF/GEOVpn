import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080/api/v1',
});

api.interceptors.request.use(config => {
    config.headers['X-User-Id'] = '858441917';
    return config;
});

export const adminApi = {
    getDashboard: () => api.get('/admin/dashboard').then(res => res.data.data),

    getUsers: (page = 0, size = 50) =>
        api.get(`/admin/users?page=${page}&size=${size}`).then(res => res.data.data),

    getUserDevices: (telegramId: number) =>
        api.get(`/admin/users/${telegramId}/devices`).then(res => res.data.data),

    getUserStats: (telegramId) =>
        api.get(`/admin/users/${telegramId}/stats`).then(res => res.data.data),

    addBalance: (telegramId: number, amount: number) =>
        api.post(`/admin/users/${telegramId}/add-balance?amount=${amount}`).then(res => res.data.data),

    changePlan: (userId: number, planName: string) =>
        api.put(`/admin/device-limits/${userId}/plan`, { planName }).then(res => res.data.data),

    getDeviceConfig: (deviceId: number) =>
        api.get(`/admin/devices/${deviceId}/config`).then(res => res.data.data),

    regenerateConfig: (deviceId: number) =>
        api.put(`/configs/${deviceId}/regenerate`, { reason: "Admin reset" }).then(res => res.data.data),

    getDeviceLimit: (userId: number) =>
        api.get(`/admin/device-limits/${userId}`).then(res => res.data.data),
};