import axios from 'axios';

export const apiClient = axios.create({
    baseURL: '/api/v1',
});

apiClient.interceptors.request.use((config) => {
    const tgUser = window.Telegram?.WebApp?.initDataUnsafe?.user;

    config.headers['X-User-Id'] = tgUser?.id?.toString() || '858441917';

    config.headers['X-Internal-Secret'] = '53a41724a2428714e21b2cbcbb19ce1ff62f4deb322037575f15f450791d54c3';

    const tgInitData = window.Telegram?.WebApp?.initData;

    // if (tgInitData) {
    //     config.headers['Authorization'] = `tma ${tgInitData}`;
    // }

    return config;
});