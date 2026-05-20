import { create } from 'zustand';
import { userApi } from '../api/user';
import { TRANSLATIONS, Lang } from '../utils/translations';
import {
    UserResponse,
    DeviceResponse,
    VpnConfigResponse,
    DeviceLimitStatus,
    LeaderboardEntry
} from '../types/api';

export type TabId = 'home' | 'profile' | 'payments' | 'manage_subscription' | 'leaderboard' | 'deposit' | 'subscriptions';

interface UserStore {
    user:         UserResponse | null;
    devices:      DeviceResponse[];
    configs:      VpnConfigResponse[];
    leaderboard:  LeaderboardEntry[];
    deviceLimit:  DeviceLimitStatus | null;
    activeTab:    TabId;
    loading:      boolean;
    error:        string | null;
    profileTab:   'main' | 'referral' | 'instructions' | 'privacy' | 'agreement';
    isMember:     boolean;

    lang: Lang;
    t:    typeof TRANSLATIONS.ru;
    setLanguage: (lang: Lang) => void;
    setProfileTab: (tab: 'main' | 'referral' | 'instructions' | 'privacy' | 'agreement') => void;

    checkMembership:      () => Promise<void>;
    fetchAll:             () => Promise<void>;
    register:             () => Promise<void>;
    regenerateConfig:     (deviceId: number) => Promise<void>;
    setActiveTab:         (tab: TabId) => void;
    purchaseSubscription: (planId: string, months?: number, promo?: boolean) => Promise<boolean>;
    purchaseExtraSlot:    () => Promise<void>;
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
    loading:     true,
    error:       null,
    profileTab:  'main',
    isMember:    true,

    lang: 'ru',
    t:    TRANSLATIONS.ru,

    setLanguage: (newLang: Lang) => {
        set({ lang: newLang, t: TRANSLATIONS[newLang] });
        window.Telegram?.WebApp?.CloudStorage.setItem('lang', newLang);
    },

    setProfileTab: (tab) => set({ profileTab: tab, activeTab: 'profile' }),

    setActiveTab: (tab) => set({ activeTab: tab }),

    checkMembership: async () => {
        try {
            set({ loading: true });
            const isMember = await userApi.checkMembership();
            set({ isMember, loading: false });
        } catch (e) {
            console.error('[checkMembership] Failed', e);
            set({ isMember: false, loading: false });
        }
    },

    register: async () => {
        const tg          = window.Telegram?.WebApp;
        const userDetails = tg?.initDataUnsafe?.user;
        const startParam  = tg?.initDataUnsafe?.start_param;

        let telegramId: number;
        let firstName: string;
        let username: string | undefined;

        if (import.meta.env.DEV && !userDetails?.id) {
            telegramId = 858441917;
            firstName = "Local Tester";
            username = "local_tester";
        } else if (userDetails?.id) {
            telegramId = userDetails.id;
            firstName = userDetails.first_name;
            username = userDetails.username;
        } else {
            throw new Error("Не удалось получить данные Telegram");
        }

        try {
            set({ loading: true });
            const newUser = await userApi.register(telegramId, firstName, username, startParam);
            set({ user: newUser, loading: false });
        } catch (e: any) {
            set({ loading: false });
            if (e.response?.status === 400 || e.response?.status === 409) {
                const profile = await userApi.getProfile().catch(() => null);
                if (profile) set({ user: profile });
                return;
            }
            throw e;
        }
    },

    regenerateConfig: async (deviceId) => {
        try {
            const newConfig = await userApi.regenerateConfig(deviceId);
            set(s => ({
                configs: s.configs.map(c => c.deviceId === deviceId ? newConfig : c)
            }));
        } catch (e) {
            console.error('[regenerateConfig] Failed', e);
            throw e;
        }
    },

    fetchAll: async () => {
        set({ loading: true, error: null });
        try {
            const [profile, devices, configs, limit, isMember] = await Promise.all([
                userApi.getProfile().catch(() => null),
                userApi.getDevices().catch(() => []),
                userApi.getConfigs().catch(() => []),
                userApi.getDeviceLimit().catch(() => null),
                userApi.checkMembership().catch(() => false),
            ]);

            set({
                user:        profile,
                devices,
                configs,
                deviceLimit: limit,
                isMember,
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
            const limit = await userApi.getDeviceLimit().catch(() => null);
            set({ user: updatedUser, deviceLimit: limit, loading: false });
            return true;
        } catch (e) {
            set({ loading: false });
            throw e;
        }
    },

    purchaseExtraSlot: async () => {
        set({ loading: true });
        try {
            const updatedUser = await userApi.purchaseExtraSlot();
            const limit = await userApi.getDeviceLimit().catch(() => null);
            set({
                user: updatedUser,
                deviceLimit: limit,
                loading: false
            });
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
            await userApi.revokeConfig(dev.id).catch(() => {});
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