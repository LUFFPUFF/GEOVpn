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

export type TabId = 'home' | 'payments' | 'profile' | 'subscriptions';

function detectDeviceType(): string {
    const ua = navigator.userAgent.toLowerCase();
    if (/iphone|ipad|ipod/.test(ua)) return 'IOS';
    if (/android/.test(ua)) return 'ANDROID';
    return 'WINDOWS';
}

interface UserStore {
    user:    UserResponse | null;
    devices: DeviceResponse[];
    configs: VpnConfigResponse[];
    leaderboard: LeaderboardEntry[];
    deviceLimit: DeviceLimitStatus | null;
    activeTab: TabId;
    loading:   boolean;
    error:     string | null;

    lang: Lang;
    t: typeof TRANSLATIONS.ru;
    setLanguage: (lang: Lang) => void;

    fetchAll:            () => Promise<void>;
    setActiveTab:        (tab: TabId) => void;
    purchaseSubscription: (planId: string, months?: number, promo?: boolean) => Promise<boolean>;
    addDevice:           (name: string, type: string) => Promise<void>;
    deleteDevice:        (uuid: string) => Promise<void>;
    createConfig:        (deviceId: number, country?: string) => Promise<void>;
    fetchLeaderboard:    () => Promise<void>;
}

export const useUserStore = create<UserStore>((set, get) => ({
    user:      null,
    devices:   [],
    configs:   [],
    leaderboard: [],
    deviceLimit: null,
    activeTab: 'home',
    loading:   false,
    error:     null,

    lang: 'ru',
    t: TRANSLATIONS.ru,

    setLanguage: (newLang: Lang) => {
        set({
            lang: newLang,
            t: TRANSLATIONS[newLang]
        });
        window.Telegram?.WebApp?.CloudStorage.setItem('lang', newLang);
    },

    setActiveTab: (tab) => set({ activeTab: tab }),

    fetchAll: async () => {
        set({ loading: true });
        try {
            const [profile, devices, configs, limit] = await Promise.all([
                userApi.getProfile().catch(() => null),
                userApi.getDevices().catch(() => []),
                userApi.getConfigs().catch(() => []),
                userApi.getDeviceLimit().catch(() => null)
            ]);

            set({ user: profile, devices, configs, deviceLimit: limit, loading: false });

            if (profile?.hasActiveSubscription && configs.length === 0 && devices.length > 0) {
                console.log("Healing: Active sub found but no config. Requesting...");
                const newConfig = await userApi.createConfig(devices[0].id);
                set(state => ({ configs: [newConfig] }));
            }
        } catch (error: any) {
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
        set({ loading: true });
        try {
            const updatedUser = await userApi.purchaseSubscription(planId, months, promo);
            set({ user: updatedUser, loading: false });
            return true;
        } catch (e) {
            set({ loading: false });
            throw e;
        }
    },

    addDevice: async (name, type) => {
        const device = await userApi.registerDevice(name, type);
        set(s => ({ devices: [...s.devices, device] }));
    },

    deleteDevice: async (uuid) => {
        const dev = get().devices.find(d => d.uuid === uuid);
        if (!dev) return;
        await userApi.deleteDevice(dev.id);
        set(s => ({ devices: s.devices.filter(d => d.uuid !== uuid) }));
    },

    createConfig: async (deviceId, country = 'FI') => {
        try {
            const config = await userApi.createConfig(deviceId, country);
            set(s => ({ configs: [config, ...s.configs] }));
        } catch (e) {
            console.error('Failed to create config:', e);
        }
    },
}));