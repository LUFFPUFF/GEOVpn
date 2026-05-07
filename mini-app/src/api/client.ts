import axios from 'axios';

export const apiClient = axios.create({
    baseURL: '/api/v1',
});

function getTelegramUserId(): string | undefined {
    const tg = window.Telegram?.WebApp;
    if (!tg) return undefined;
    const id = tg.initDataUnsafe?.user?.id;
    return id ? id.toString() : undefined;
}

apiClient.interceptors.request.use((config) => {
    const tg = window.Telegram?.WebApp;
    const userId = getTelegramUserId();

    if (userId) {
        config.headers['X-User-Id'] = userId;
        config.headers['Authorization'] = `Bearer ${tg?.initData}`;
    }

    return config;
});
