import axios from 'axios';

export const apiClient = axios.create({
    baseURL: '/api/v1',
});

function getTelegramUserId(): string | undefined {
    const tg = window.Telegram?.WebApp;
    const id = tg?.initDataUnsafe?.user?.id;

    if (id) {
        return id.toString();
    }

    if (import.meta.env.DEV) {
        console.warn("⚠️ LOCAL DEV MODE: Using mocked Telegram ID");
        return "858441917";
    }

    return undefined;
}

apiClient.interceptors.request.use((config) => {
    const tg = window.Telegram?.WebApp;
    const userId = getTelegramUserId();

    if (userId) {
        config.headers['X-User-Id'] = userId;
        const initData = tg?.initData || (import.meta.env.DEV ? "test_local_init_data" : "");
        config.headers['Authorization'] = `Bearer ${initData}`;
    }

    return config;
});