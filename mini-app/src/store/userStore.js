import { create } from 'zustand';
import api from '../api/axios';
import { TRANSLATIONS } from '../constants/translations';

/**
 * Вспомогательная функция для получения данных из Telegram WebApp
 */
const getTgData = () => {
    const tg = window.Telegram?.WebApp;
    const user = tg?.initDataUnsafe?.user;
    const startParam = tg?.initDataUnsafe?.start_param || "";

    return {
        telegramId: user?.id ? Number(user.id) : 970667053,
        username: String(user?.username || 'unknown'),
        firstName: String(user?.first_name || 'User'),
        referralCode: String(startParam)
    };
};

export const useUserStore = create((set, get) => ({
    user: null,
    configs: [],
    devices: [],
    activeTab: 'home',
    loading: true,
    actionLoading: false,
    lang: 'ru',
    t: TRANSLATIONS['ru'],

    setActiveTab: (tab) => set({ activeTab: tab }),

    setLanguage: (langCode) => {
        if (TRANSLATIONS[langCode]) {
            set({
                lang: langCode,
                t: TRANSLATIONS[langCode]
            });
        }
    },
    initApp: async () => {
        set({ loading: true });
        const tgData = getTgData();

        try {
            let userRes = await api.get('/users/me').catch(err => {
                console.error("Критическая ошибка /users/me:", err.response?.data || err.message);
                return err.response; // Пробрасываем ошибку дальше для обработки
            });
            if (!userRes || !userRes.data?.success) {
                if (userRes?.status === 500) {
                    console.error("!!! СБОЙ БЭКЕНДА (500) !!!");
                    console.error("Проверь поле subscription_type в базе данных. Допустимые: PAYG, MONTHLY, YEARLY");
                }
                else {
                    console.log("Профиль не найден. Запуск процесса регистрации...");

                    const regRes = await api.post('/users/register', {
                        telegramId: Number(tgData.telegramId),
                        username: String(tgData.username),
                        firstName: String(tgData.firstName),
                        referralCode: String(tgData.referralCode)
                    }).catch(e => {
                        console.error("Ошибка при попытке регистрации (400 Bad Request?):", e.response?.data);
                        return null;
                    });
                    if (regRes?.data?.success) {
                        userRes = await api.get('/users/me').catch(() => null);
                    }
                }
            }
            if (userRes?.data?.success) {
                const userData = userRes.data.data;
                set({ user: userData });
                const [devsRes, cfgsRes] = await Promise.allSettled([
                    api.get('/devices'),  // User-Service (8082)
                    api.get('/configs')   // Config-Service (8083)
                ]);

                if (devsRes.status === 'fulfilled' && devsRes.value.data?.success) {
                    set({ devices: devsRes.value.data.data });
                } else {
                    console.warn("Не удалось загрузить список устройств");
                }

                if (cfgsRes.status === 'fulfilled' && cfgsRes.value.data?.success) {
                    set({ configs: cfgsRes.value.data.data });
                } else {
                    console.error("VPN-сервис недоступен или вернул ошибку 500");
                }
            }
        } catch (e) {
            console.error("Общий сбой инициализации стора:", e);
        } finally {
            set({ loading: false });
        }
    },

    addDevice: async (deviceName, deviceType) => {
        const { user, devices } = get();
        if (!user) return false;

        set({ actionLoading: true });
        try {
            const res = await api.post('/devices', {
                userId: Number(user.telegramId),
                deviceName: String(deviceName),
                deviceType: String(deviceType) // IOS, ANDROID, DESKTOP
            });

            if (res.data?.success) {
                set({ devices: [...devices, res.data.data] });
                return true;
            }
        } catch (e) {
            console.error("Ошибка API при добавлении устройства:", e.response?.data);
        } finally {
            set({ actionLoading: false });
        }
        return false;
    },

    deleteDevice: async (uuid) => {
        set({ actionLoading: true });
        try {
            const res = await api.delete(`/devices/${uuid}`);

            if (res.data?.success) {
                // Удаляем устройство из стейта по тех. UUID
                set(state => ({
                    devices: state.devices.filter(d => d.uuid !== uuid)
                }));
                return true;
            }
        } catch (e) {
            console.error("Ошибка API при удалении устройства:", e);
        } finally {
            set({ actionLoading: false });
        }
        return false;
    },

    createConfig: async (deviceId, preferredCountry = "NL") => {
        const { user, configs } = get();
        if (!user) return false;

        set({ actionLoading: true });
        try {
            const res = await api.post('/configs', {
                userId: Number(user.telegramId),
                deviceId: Number(deviceId), // Внутренний Long ID устройства
                preferredCountry: String(preferredCountry),
                protocol: "VLESS"
            });

            if (res.data?.success) {
                set({ configs: [...configs, res.data.data] });
                // Тактильная отдача для Telegram
                window.Telegram?.WebApp?.HapticFeedback.notificationOccurred('success');
                return true;
            }
        } catch (e) {
            console.error("Ошибка API при генерации VLESS:", e.response?.data);
        } finally {
            set({ actionLoading: false });
        }
        return false;
    },

    regenerateConfig: async (configId) => {
        set({ actionLoading: true });
        try {
            // Принудительно передаем ID как число
            const res = await api.put(`/configs/${Number(configId)}/regenerate`);

            if (res.data?.success) {
                set(state => ({
                    configs: state.configs.map(c =>
                        c.id === configId ? res.data.data : c
                    )
                }));
                return true;
            }
        } catch (e) {
            console.error("Ошибка API при перевыпуске ключа:", e);
        } finally {
            set({ actionLoading: false });
        }
        return false;
    }
}));