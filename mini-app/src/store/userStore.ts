import { create } from 'zustand';
import { userApi } from '../api/user';
import { TRANSLATIONS, Lang } from '../utils/translations';
import {
    UserResponse,
    UserStatsResponse,
    DeviceResponse,
    VpnConfigResponse,
    DeviceLimitStatus,
    LeaderboardEntry
} from '../types/api';

export type TabId = 'home' | 'profile' | 'payments' | 'subscriptions' | 'leaderboard' | 'deposit';

interface UserStore {
    user:         UserResponse | null;
    devices:      DeviceResponse[];
    configs:      VpnConfigResponse[];
    leaderboard:  LeaderboardEntry[];
    deviceLimit:  DeviceLimitStatus | null;
    activeTab:    TabId;
    loading:      boolean;
    error:        string | null;

    lang: Lang;
    t:    typeof TRANSLATIONS.ru;
    setLanguage: (lang: Lang) => void;

    fetchAll:             () => Promise<void>;
    register:             () => Promise<void>;
    setActiveTab:         (tab: TabId) => void;
    purchaseSubscription: (planId: string, months?: number, promo?: boolean) => Promise<boolean>;
    addDevice:            (name: string, type: string) => Promise<void>;
    deleteDevice:         (uuid: string) => Promise<void>;
    createConfig:         (deviceId: number, country?: string) => Promise<void>;
    fetchLeaderboard:     () => Promise<void>;
}

export const useUserStore = create<UserStore>((set, get) => ({
    user:        null,
    devices:     [],
    configs:     [],
    leaderboard: [],
    deviceLimit: null,
    activeTab:   'home',
    loading:     false,
    error:       null,

    lang: 'ru',
    t:    TRANSLATIONS.ru,

    setLanguage: (newLang: Lang) => {
        set({ lang: newLang, t: TRANSLATIONS[newLang] });
        window.Telegram?.WebApp?.CloudStorage.setItem('lang', newLang);
    },

    setActiveTab: (tab) => set({ activeTab: tab }),

    register: async () => {
        const tg          = window.Telegram?.WebApp;
        const userDetails = tg?.initDataUnsafe?.user;
        const startParam  = tg?.initDataUnsafe?.start_param;

        if (!userDetails) {
            console.error('[register] No telegram user data');
            return;
        }

        try {
            const newUser = await userApi.register(
                userDetails.id,
                userDetails.first_name,
                userDetails.username,
                startParam
            );
            set({ user: newUser });
        } catch (e) {
            console.error('[register] Failed:', e);
            throw e;
        }
    },

    fetchAll: async () => {
        set({ loading: true, error: null });
        try {
            const [profile, devices, configs, limit] = await Promise.all([
                userApi.getProfile().catch(() => null),
                userApi.getDevices().catch(() => []),
                userApi.getConfigs().catch(() => []),
                userApi.getDeviceLimit().catch(() => null),
            ]);

            set({
                user:        profile,
                devices,
                configs,
                deviceLimit: limit,
                loading:     false,
            });
        } catch (error: any) {
            set({ error: error.message, loading: false });
        }
    },

    fetchLeaderboard: async () => {
        try {
            const data = await userApi.getLeaderboard();
            set({ leaderboard: data });
        } catch (e) {
            console.error('[fetchLeaderboard]', e);
        }
    },

    purchaseSubscription: async (planId, months = 1, promo = false) => {
        set({ loading: true });
        try {
            const updatedUser = await userApi.purchaseSubscription(planId, months, promo);
            set({ user: updatedUser, loading: false });
            const limit = await userApi.getDeviceLimit().catch(() => null);
            set({ deviceLimit: limit });
            return true;
        } catch (e) {
            set({ loading: false });
            throw e;
        }
    },

    addDevice: async (name, type) => {
        const { deviceLimit } = get();
        if (deviceLimit && deviceLimit.limitReached) {
            throw new Error('Достигнут лимит устройств');
        }
        const device = await userApi.registerDevice(name, type);
        set(s => ({ devices: [...s.devices, device] }));
        const limit = await userApi.getDeviceLimit().catch(() => null);
        set({ deviceLimit: limit });
    },

    deleteDevice: async (uuid) => {
        const { devices, configs } = get();
        const dev = devices.find(d => d.uuid === uuid);
        if (!dev) return;

        if (configs.some(c => c.deviceId === dev.id)) {
            await userApi.revokeConfig(dev.id).catch(e =>
                console.error('[deleteDevice] Failed to revoke config:', e)
            );
        }

        await userApi.deleteDevice(dev.uuid);

        set(s => ({
            devices: s.devices.filter(d => d.uuid !== uuid),
            configs: s.configs.filter(c => c.deviceId !== dev.id),
        }));

        const limit = await userApi.getDeviceLimit().catch(() => null);
        set({ deviceLimit: limit });
    },

    createConfig: async (deviceId, country = 'RU') => {
        try {
            const config = await userApi.createConfig(deviceId, country);
            set(s => ({ configs: [config, ...s.configs] }));
        } catch (e) {
            console.error('[createConfig] Failed for deviceId:', deviceId, e);
            throw e;
        }
    },
}));