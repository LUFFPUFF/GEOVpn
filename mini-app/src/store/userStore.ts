import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { userApi } from '../api/user';
import {
    UserResponse,
    UserStatsResponse,
    DeviceResponse,
    VpnConfigResponse,
    DeviceLimitStatus,
    LeaderboardEntry
} from '../types/api';
import { Lang, TRANSLATIONS, Translations } from '../utils/translations';

export type TabId = 'home' | 'payments' | 'profile' | 'subscriptions';

function detectDeviceType(): string {
    const ua = navigator.userAgent.toLowerCase();
    if (/iphone|ipad|ipod/.test(ua)) return 'IOS';
    if (/android/.test(ua)) return 'ANDROID';
    return 'WINDOWS';
}

function detectLang(): Lang {
    const tgLang = window.Telegram?.WebApp?.initDataUnsafe?.user?.language_code;
    if (tgLang === 'tg') return 'tg';
    if (tgLang === 'uz') return 'uz';
    if (tgLang === 'fa') return 'fa';
    return 'ru';
}

interface UserStore {
    user:        UserResponse | null;
    stats:       UserStatsResponse | null;
    devices:     DeviceResponse[];
    configs:     VpnConfigResponse[];
    leaderboard: LeaderboardEntry[];
    deviceLimit: DeviceLimitStatus | null;

    activeTab: TabId;
    loading:   boolean;
    error:     string | null;

    lang: Lang;
    t:    Translations;

    fetchAll:             () => Promise<void>;
    setActiveTab:         (tab: TabId) => void;
    setLang:              (lang: Lang) => void;
    // Алиас для Header.tsx (там используется setLanguage)
    setLanguage:          (lang: Lang) => void;
    purchaseSubscription: (planId: string, tariffPrice: number) => Promise<boolean>;
    addDevice:            (name: string, type: string) => Promise<void>;
    deleteDevice:         (uuid: string) => Promise<void>;
    createConfig:         (deviceId: number, country?: string) => Promise<void>;
    fetchLeaderboard:     () => Promise<void>;
}

export const useUserStore = create<UserStore>()(
    persist(
        (set, get) => {
            const initialLang = detectLang();

            return {
                user:        null,
                stats:       null,
                devices:     [],
                configs:     [],
                activeTab:   'home',
                loading:     false,
                error:       null,
                deviceLimit: null,
                leaderboard: [],

                lang: initialLang,
                t:    TRANSLATIONS[initialLang],

                setLang: (lang: Lang) => {
                    set({ lang, t: TRANSLATIONS[lang] });
                },

                setLanguage: (lang: Lang) => {
                    set({ lang, t: TRANSLATIONS[lang] });
                },

                setActiveTab: (tab) => set({ activeTab: tab }),

                fetchAll: async () => {
                    set({ loading: true, error: null });
                    try {
                        const [profile, devices, configs, limit] = await Promise.all([
                            userApi.getProfile().catch(e => { console.error('Profile error:', e); return null; }),
                            userApi.getDevices().catch(e => { console.error('Devices error:', e); return []; }),
                            userApi.getConfigs().catch(e => { console.error('Configs error:', e); return []; }),
                            userApi.getDeviceLimit().catch(e => { console.error('Limit error:', e); return null; }),
                        ]);

                        if (profile) {
                            set({ user: profile, devices, configs, deviceLimit: limit, loading: false });
                        } else {
                            set({ error: 'Не удалось загрузить профиль', loading: false });
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

                purchaseSubscription: async (planId, tariffPrice) => {
                    try {
                        const updatedUser = await userApi.purchaseSubscription(planId, 1);
                        set({ user: updatedUser });

                        let { devices } = get();
                        if (devices.length === 0) {
                            const tgUser = window.Telegram?.WebApp?.initDataUnsafe?.user;
                            const deviceName = tgUser?.first_name ? `${tgUser.first_name}'s Device` : 'My Device';
                            try {
                                const newDevice = await userApi.registerDevice(deviceName, detectDeviceType());
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

                        return true;
                    } catch (e: unknown) {
                        const msg = (e as { response?: { data?: { error?: { message?: string } } } })
                            ?.response?.data?.error?.message || 'Ошибка оплаты';
                        set({ error: msg });
                        return false;
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
                    const dev = get().devices.find(d => d.uuid === uuid);
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
            };
        },
        {
            name: 'geo-vpn-lang',
            partialize: (state) => ({ lang: state.lang }),
            onRehydrateStorage: () => (state) => {
                if (state?.lang) {
                    state.t = TRANSLATIONS[state.lang];
                }
            },
        }
    )
);