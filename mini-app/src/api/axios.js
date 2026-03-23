import axios from 'axios';
import { APP_CONFIG } from '../config/constants';
import WebApp from '@twa-dev/sdk';

export const apiClient = axios.create({
    baseURL: APP_CONFIG.API_URL,
});

// Автоматически добавляем данные из Telegram в каждый запрос к бэкенду
apiClient.interceptors.request.use((config) => {
    const initData = WebApp.initData;
    if (initData) {
        config.headers.Authorization = `tma ${initData}`;
    }
    return config;
});