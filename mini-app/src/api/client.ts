import axios from 'axios';

export const apiClient = axios.create({
    baseURL: '/api/v1',
});

apiClient.interceptors.request.use((config) => {
    const tg = window.Telegram?.WebApp;

    const userId = tg?.initDataUnsafe?.user?.id;

    if (userId) {
        config.headers['X-User-Id'] = userId.toString();
        config.headers['Authorization'] = `Bearer ${tg?.initData}`;
    } else {
        config.headers['X-User-Id'] = "858441917";
    }

    return config;
});