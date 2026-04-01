import axios from 'axios';

const api = axios.create({
    baseURL: '/api', // Vite проксирует это в http://localhost:8082/api/v1
    headers: {
        'Content-Type': 'application/json',
        'X-Internal-Secret': '53a41724a2428714e21b2cbcbb19ce1ff62f4deb322037575f15f450791d54c3'
    }
});

api.interceptors.request.use((config) => {
    const tg = window.Telegram?.WebApp;
    // Используем ID из ТГ, либо тестовый.
    // ВАЖНО: Бэкенд ждет Long, поэтому убеждаемся, что передаем только цифры.
    const userId = tg?.initDataUnsafe?.user?.id || 970667053;

    config.headers['X-User-Id'] = userId.toString();
    return config;
});

export default api;