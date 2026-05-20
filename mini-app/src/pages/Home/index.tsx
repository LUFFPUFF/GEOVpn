import React, { useState, useMemo } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { useUserStore } from '../../store/userStore';
import { userApi } from '../../api/user';
import DeviceSelector from '../../components/modals/DeviceSelector';
import {
    Crown, Zap, MonitorSmartphone, Globe2, ChevronRight,
    ArrowRight, AlertCircle, Loader2, X, CheckCircle2,
    ShieldAlert, Activity
} from 'lucide-react';
import bgImage from '../../assets/vpn-bg.png';
import bugImage from '../../assets/bug-67.png';
import { DeviceType } from '../../types/api';

type EggState = 'hidden' | 'playing' | 'loading' | 'success' | 'error';

export default function Home() {
    const { user, deviceLimit, devices, configs, setActiveTab, t, fetchAll, loading } = useUserStore();
    const [showDeviceSelect, setShowDeviceSelect] = useState(false);
    const [isSettingUp, setIsSettingUp] = useState(false);

    const [eggState, setEggState] = useState<EggState>('hidden');
    const [eggClicks, setEggClicks] = useState(0);
    const [eggErrorMsg, setEggErrorMsg] = useState('');

    const isInitialized = user !== null && user !== undefined;
    const hasSub = user?.hasActiveSubscription ?? false;
    const needsDevice = hasSub && devices.length === 0;

    const getPlanNameRu = (type?: string) => {
        if (!type) return '---';
        switch (type.toUpperCase()) {
            case 'DAILY':    return 'Пробная';
            case 'BASIC':    return 'Стандартная';
            case 'STANDARD': return 'Премиум';
            case 'FAMILY':   return 'Семейная';
            default:         return type;
        }
    };

    const { daysLeft, expireDateFormatted } = useMemo(() => {
        if (!user?.subscriptionExpiresAt) return { daysLeft: 0, expireDateFormatted: '---' };
        const expire = new Date(user.subscriptionExpiresAt);
        const diff = Math.ceil((expire.getTime() - Date.now()) / 86_400_000);
        return {
            daysLeft: diff > 0 ? diff : 0,
            expireDateFormatted: expire.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short', year: 'numeric' })
        };
    }, [user]);

    const activeDevs = devices.length;
    const maxDevs = deviceLimit?.maxDevices ?? 1;

    const stats = [
        { icon: Zap, val: '∞', label: t?.traffic || 'Трафик' },
        { icon: MonitorSmartphone, val: `${activeDevs}/${maxDevs}`, label: t?.devices || 'Устройства' },
        { icon: Globe2, val: t?.all_locations || 'Все', label: t?.locations || 'Локации' },
    ];

    const haptic = (s: 'light' | 'medium' | 'heavy' | 'success' | 'error' = 'light') => {
        const tg = window.Telegram?.WebApp;
        if (tg) {
            if (s === 'success' || s === 'error') tg.HapticFeedback.notificationOccurred(s);
            else tg.HapticFeedback.impactOccurred(s);
        }
    };

    const handleDeviceSelected = async (type: DeviceType) => {
        setShowDeviceSelect(false);
        try {
            await userApi.registerDevice(`${type.toUpperCase()} Device`, type.toUpperCase());
            await fetchAll();
            setActiveTab('payments');
        } catch (e: any) {
            window.Telegram?.WebApp?.showAlert('Ошибка при создании устройства');
        }
    };

    const handleSetupSubscription = async () => {
        haptic('medium');
        if (needsDevice) {
            setActiveTab('profile');
            return;
        }
        setIsSettingUp(true);
        try {
            const currentConfigs = useUserStore.getState().configs;
            const existingIds = new Set(currentConfigs.map(c => c.deviceId));
            const toCreate = devices.filter(d => !existingIds.has(d.id));

            for (const dev of toCreate) {
                await userApi.createConfig(dev.id);
            }
            await fetchAll();
            setActiveTab('subscriptions');
        } catch (error) {
            console.error(error);
        } finally {
            setIsSettingUp(false);
        }
    };

    const handleEggClick = async () => {
        if (eggState !== 'playing') return;
        haptic('light');
        const newCount = eggClicks + 1;
        setEggClicks(newCount);
        if (newCount >= 67) {
            setEggState('loading');
            try {
                await userApi.claimEasterEgg();
                await fetchAll();
                setEggState('success');
                haptic('success');
            } catch (e: any) {
                setEggErrorMsg('Вы уже забирали этот бонус.');
                setEggState('error');
            }
        }
    };

    if (loading && !isInitialized) {
        return (
            <div className="flex items-center justify-center" style={{ height: 'calc(var(--tg-height, 100dvh) - 180px)' }}>
                <Loader2 size={28} className="animate-spin text-white/30" />
            </div>
        );
    }

    return (
        <div className="flex flex-col pt-2 pb-10 px-2 animate-in fade-in duration-500">
            {hasSub ? (
                <div className="flex flex-col gap-4">
                    <div className="relative overflow-hidden rounded-[2.5rem] p-6 border border-white/10 bg-gradient-to-b from-[#12141d] to-[#0a0a0f] shadow-2xl">
                        <div className="absolute top-[-20%] left-[-10%] w-[150%] h-[50%] bg-emerald-500/10 blur-[80px] rounded-full pointer-events-none" />

                        <div className="flex justify-between items-start mb-5 relative z-10 text-left">
                            <div className="flex items-center gap-3">
                                <div className="w-12 h-12 bg-gradient-to-br from-amber-400/20 to-amber-600/5 rounded-[1.2rem] flex items-center justify-center border border-amber-500/30 shadow-lg">
                                    <Crown size={24} className="text-amber-400" />
                                </div>
                                <div className="text-left">
                                    <h2 className="text-[18px] font-black uppercase tracking-tight text-white leading-none mb-1">
                                        {getPlanNameRu(user?.subscriptionType)}
                                    </h2>
                                    <div className="flex items-center gap-1.5">
                                        <div className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
                                        <p className="text-emerald-500 text-[10px] font-black uppercase tracking-[0.2em]">Защита активна</p>
                                    </div>
                                </div>
                            </div>
                            <div className="text-right shrink-0">
                                <p className="text-white/40 text-[9px] font-black uppercase tracking-[0.2em] mb-1">До даты</p>
                                <p className="font-bold text-[11px] text-white/90 bg-white/5 px-2 py-1 rounded-md border border-white/5">{expireDateFormatted}</p>
                            </div>
                        </div>

                        <AnimatePresence>
                            {needsDevice && (
                                <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1, marginBottom: 16 }} onClick={() => setActiveTab('profile')}>
                                    <div className="p-3.5 bg-amber-500/10 border border-amber-500/30 rounded-2xl flex items-center gap-3 active:scale-95 transition-all">
                                        <AlertCircle className="text-amber-500 shrink-0" size={20} />
                                        <div className="flex-1 text-left">
                                            <p className="text-white text-[11px] font-black uppercase">Устройство не найдено</p>
                                            <p className="text-white/60 text-[9px] mt-0.5">Нажмите здесь, чтобы создать устройство.</p>
                                        </div>
                                        <ChevronRight size={16} className="text-white/40" />
                                    </div>
                                </motion.div>
                            )}
                        </AnimatePresence>

                        <div className="grid grid-cols-3 gap-2 mb-4 relative z-10">
                            {stats.map((item, i) => (
                                <div key={i} className="bg-black/40 rounded-2xl border border-white/5 flex flex-col items-center justify-center py-4 shadow-inner">
                                    <item.icon size={18} className="mb-1.5 text-white/40" />
                                    <span className="text-[15px] font-black text-white">{item.val}</span>
                                    <span className="text-[8px] font-black uppercase tracking-widest text-white/30 mt-0.5">{item.label}</span>
                                </div>
                            ))}
                        </div>

                        <div className="flex items-center gap-3 mb-5 bg-black/40 p-3 rounded-2xl border border-white/5 relative z-10 shadow-inner">
                            <div className="flex-1 flex items-center gap-2">
                                <div className="w-7 h-7 rounded-full bg-emerald-500/10 flex items-center justify-center border border-emerald-500/20">
                                    <Zap size={12} className="text-emerald-400" />
                                </div>
                                <div className="text-left">
                                    <p className="text-[8px] text-white/40 font-black uppercase">Ping</p>
                                    <p className="text-[13px] font-black text-white">12 <span className="text-[9px] text-white/50">ms</span></p>
                                </div>
                            </div>
                            <div className="w-px h-7 bg-white/10" />
                            <div className="flex-1 flex items-center gap-2 pl-1">
                                <div className="w-7 h-7 rounded-full bg-blue-500/10 flex items-center justify-center border border-blue-500/20">
                                    <Globe2 size={12} className="text-blue-400" />
                                </div>
                                <div className="text-left">
                                    <p className="text-[8px] text-white/40 font-black uppercase">Канал</p>
                                    <p className="text-[13px] font-black text-white">1 <span className="text-[9px] text-white/50">Gbit/s</span></p>
                                </div>
                            </div>
                        </div>

                        <div className="bg-black/40 rounded-[2rem] p-5 border border-white/10 text-center relative z-10 mb-5 shadow-inner">
                            <p className="text-white/40 text-[10px] font-black uppercase tracking-[0.3em] mb-1">Осталось</p>
                            <div className="flex items-baseline justify-center gap-2">
                                <span className="text-[44px] font-black tracking-tighter text-white leading-none" onClick={() => setEggState('playing')}>{daysLeft}</span>
                                <span className="text-[14px] text-white/50 font-bold uppercase tracking-widest">Дней</span>
                            </div>
                        </div>

                        <div className="space-y-2.5 relative z-10">
                            <div className="flex gap-2.5">
                                <button onClick={() => setActiveTab('payments')} className="flex-1 py-4 bg-white/5 border border-white/10 rounded-2xl font-black text-[11px] text-white/80 uppercase tracking-widest active:scale-95 transition-all">Продлить</button>
                                <button onClick={() => setActiveTab('profile')} className="flex-1 py-4 bg-white/5 border border-white/10 rounded-2xl font-black text-[11px] text-white/80 uppercase tracking-widest active:scale-95 transition-all">Профиль</button>
                            </div>
                            <button onClick={handleSetupSubscription} disabled={isSettingUp} className="w-full py-4 bg-white text-black rounded-2xl font-black text-[13px] uppercase tracking-[0.1em] transition-all flex items-center justify-center gap-2 active:scale-[0.98] shadow-lg">
                                {isSettingUp ? <><Loader2 size={18} className="animate-spin" /> Настройка...</> : <><ArrowRight size={18} /> Установить подписку</>}
                            </button>
                        </div>
                    </div>

                    <button onClick={() => setActiveTab('payments')} className="w-full p-4 bg-white/5 border border-white/10 rounded-3xl flex items-center justify-between active:bg-white/10 transition-all">
                        <div className="flex items-center gap-3">
                            <div className="w-10 h-10 rounded-xl bg-amber-500/10 flex items-center justify-center text-amber-500"><Crown size={18}/></div>
                            <div className="text-left"><p className="text-[13px] font-black text-white uppercase">Улучшить тариф</p><p className="text-[9px] text-white/40 font-bold uppercase">Больше устройств и скорости</p></div>
                        </div>
                        <ChevronRight size={18} className="text-white/20"/>
                    </button>
                </div>
            ) : (
                <div className="relative overflow-hidden rounded-[2.5rem] border border-white/10 shadow-2xl bg-[#0a0a0a]">
                    <img src={bgImage} alt="GEO VPN" className="w-full h-auto object-contain" style={{ maxHeight: 'calc(var(--tg-height, 100dvh) - 160px)' }} />
                    <div className="absolute inset-0 bg-gradient-to-t from-black via-black/20 to-transparent" />
                    <div className="absolute bottom-0 left-0 right-0 p-6 z-10">
                        <h2 className="text-[28px] font-black text-white uppercase italic tracking-tighter mb-2 text-left">Твой доступ к свободе</h2>
                        <p className="text-white/50 text-[14px] mb-8 text-left leading-tight">Безопасное соединение, высокая скорость и анонимность в один клик.</p>
                        <button onClick={() => setShowDeviceSelect(true)} className="w-full py-5 bg-white text-black rounded-2xl font-black text-[14px] uppercase tracking-widest active:scale-95 flex items-center justify-center gap-2 shadow-[0_15px_30px_rgba(255,255,255,0.1)]">
                            <span>Оформить доступ</span> <ChevronRight size={20} />
                        </button>
                    </div>
                </div>
            )}

            <AnimatePresence>
                {eggState !== 'hidden' && (
                    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-xl">
                        <motion.div initial={{ scale: 0.8 }} animate={{ scale: 1 }} className="bg-[#0d0e15] border border-white/10 rounded-[2.5rem] p-6 w-full max-w-sm text-center relative">
                            <button onClick={() => setEggState('hidden')} className="absolute top-5 right-5 text-white/20"><X size={20}/></button>
                            {eggState === 'playing' && (
                                <div className="flex flex-col items-center">
                                    <ShieldAlert className="text-rose-500 mb-4" size={32} />
                                    <h3 className="text-xl font-black text-rose-500 uppercase mb-2">Системный Баг</h3>
                                    <motion.img src={bugImage} whileTap={{ scale: 0.8 }} onClick={handleEggClick} className="w-40 h-40 mx-auto mb-8 cursor-pointer" />
                                    <div className="w-full bg-white/5 h-2 rounded-full overflow-hidden border border-white/5">
                                        <motion.div className="h-full bg-gradient-to-r from-rose-500 to-amber-500" animate={{ width: `${(eggClicks/67)*100}%` }} />
                                    </div>
                                    <p className="text-white font-black text-sm mt-3">{eggClicks} / 67</p>
                                </div>
                            )}
                            {eggState === 'loading' && <div className="py-10"><Loader2 size={40} className="animate-spin text-amber-500 mx-auto mb-4"/><p className="text-white font-black uppercase text-xs animate-pulse">Взлом системы...</p></div>}
                            {eggState === 'success' && <div className="py-8"><CheckCircle2 size={56} className="text-emerald-500 mx-auto mb-4"/><h3 className="text-white font-black uppercase text-xl">Успех!</h3><p className="text-emerald-500/70 font-bold uppercase text-[10px] mt-2">+2 дня PREMIUM добавлено</p></div>}
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>

            <AnimatePresence>
                {showDeviceSelect && <DeviceSelector onSelect={handleDeviceSelected} onClose={() => setShowDeviceSelect(false)} />}
            </AnimatePresence>
        </div>
    );
}