import { create } from 'zustand';
import { userApi } from '../api/user';
import {
    UserResponse,
    UserStatsResponse,
    DeviceResponse,
    VpnConfigResponse,
    DeviceLimitStatus,
    LeaderboardEntry
} from '../types/api';

export type TabId = 'home' | 'payments' | 'profile' | 'subscriptions';

function detectDeviceType(): string {
    const ua = navigator.userAgent.toLowerCase();
    if (/iphone|ipad|ipod/.test(ua)) return 'IOS';
    if (/android/.test(ua)) return 'ANDROID';
    return 'WINDOWS';
}

interface UserStore {
    user:    UserResponse | null;
    stats:   UserStatsResponse | null;
    devices: DeviceResponse[];
    configs: VpnConfigResponse[];
    leaderboard: LeaderboardEntry[];
    deviceLimit: DeviceLimitStatus | null;

    activeTab: TabId;
    loading:   boolean;
    error:     string | null;

    t: typeof RU;

    fetchAll:            () => Promise<void>;
    setActiveTab:        (tab: TabId) => void;
    purchaseSubscription: (planId: string, months?: number, promo?: boolean) => Promise<boolean>;
    addDevice:           (name: string, type: string) => Promise<void>;
    deleteDevice:        (uuid: string) => Promise<void>;
    createConfig:        (deviceId: number, country?: string) => Promise<void>;
    fetchLeaderboard:    () => Promise<void>;
}

const RU = {
    dashboard: 'Дашборд',
    shop:      'Магазин',
    settings:  'Настройки',
    balance:   'Баланс',
    active:    'Активна',
    inactive:  'Неактивна',
    remains:   'Осталось',
    days:      'дней',
};

export const useUserStore = create<UserStore>((set, get) => ({
    user:      null,
    stats:     null,
    devices:   [],
    configs:   [],
    activeTab: 'home',
    loading:   false,
    error:     null,
    t:         RU,
    deviceLimit: null,
    leaderboard: [],

    setActiveTab: (tab) => set({ activeTab: tab }),

    fetchAll: async () => {
        set({ loading: true, error: null });
        try {
            const profilePromise = userApi.getProfile().catch(e => { console.error('Profile error:', e); return null; });
            const devicesPromise = userApi.getDevices().catch(e => { console.error('Devices error:', e); return []; });
            const configsPromise = userApi.getConfigs().catch(e => { console.error('Configs error:', e); return []; });
            const limitPromise = userApi.getDeviceLimit().catch(e => { console.error('Limit error:', e); return null; });

            const [profile, devices, configs, limit] = await Promise.all([
                profilePromise,
                devicesPromise,
                configsPromise,
                limitPromise
            ]);

            if (profile) {
                set({ user: profile, devices, configs, deviceLimit: limit, loading: false });
            } else {
                set({ error: "Не удалось загрузить профиль", loading: false });
            }
        } catch (error: any) {
            console.error("Critical fetchAll error:", error);
            set({ error: error.message, loading: false });
        }
    },

    fetchLeaderboard: async () => {
        try {
            const data = await userApi.getLeaderboard();
            set({ leaderboard: data });
        } catch (e) {
            console.error('Leaderboard error', e);
        }
    },

    purchaseSubscription: async (planId: string, months = 1, promo = false) => {
        set({ loading: true, error: null });
        try {
            const updatedUser = await userApi.purchaseSubscription(planId, months, promo);

            // Если мы просили промо, сервер вернул статус 200, но тип остался PAYG — значит акция уже использована
            if (promo && updatedUser.subscriptionType === 'PAYG') {
                set({ loading: false });
                return false;
            }

            set({ user: updatedUser });

            let { devices } = get();
            if (devices.length === 0) {
                const tgUser = window.Telegram?.WebApp?.initDataUnsafe?.user;
                const deviceName = tgUser?.first_name
                    ? `${tgUser.first_name}'s Device`
                    : 'My Device';
                const deviceType = detectDeviceType();

                try {
                    const newDevice = await userApi.registerDevice(deviceName, deviceType);
                    devices = [newDevice];
                    set({ devices });
                } catch (e) {
                    console.warn('Device registration failed', e);
                }
            }

            const { configs } = get();
            if (configs.length === 0 && devices.length > 0) {
                try {
                    const newConfig = await userApi.createConfig(devices[0].id);
                    set({ configs: [newConfig] });
                } catch (e) {
                    console.warn('Config creation failed', e);
                }
            }

            set({ loading: false });
            return true;

        } catch (e: any) {
            const msg = e?.response?.data?.error?.message || 'Ошибка сервера при оплате';
            set({ error: msg, loading: false });
            console.error('Purchase failed:', e);
            // Пробрасываем ошибку дальше, чтобы компонент знал, что произошла ошибка сети/бэкенда, а не отказ в акции
            throw e;
        }
    },

    addDevice: async (name, type) => {
        try {
            const device = await userApi.registerDevice(name, type);
            set(s => ({ devices: [...s.devices, device] }));
        } catch (e) {
            console.error('addDevice failed', e);
        }
    },

    deleteDevice: async (uuid) => {
        const { devices } = get();
        const dev = devices.find(d => d.uuid === uuid);
        if (!dev) return;
        try {
            await userApi.deleteDevice(dev.id);
            set(s => ({ devices: s.devices.filter(d => d.uuid !== uuid) }));
        } catch (e) {
            console.error('deleteDevice failed', e);
        }
    },

    createConfig: async (deviceId, country = 'NL') => {
        try {
            const config = await userApi.createConfig(deviceId, country);
            set(s => ({ configs: [...s.configs, config] }));
        } catch (e) {
            console.error('createConfig failed', e);
        }
    },
}));