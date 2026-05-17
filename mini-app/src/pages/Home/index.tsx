import React, { useState, useMemo, useRef, useCallback } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { useUserStore } from '../../store/userStore';
import { userApi } from '../../api/user';
import DeviceSelector from '../../components/modals/DeviceSelector';
import { Crown, Zap, MonitorSmartphone, Globe2, ChevronRight, ArrowRight, AlertCircle, Loader2, X, CheckCircle2, ShieldAlert } from 'lucide-react';
import bgImage from '../../assets/vpn-bg.png';
import bugImage from '../../assets/bug-67.png';
import { DeviceType } from '../../types/api';

type EggState = 'hidden' | 'playing' | 'loading' | 'success' | 'error';

export default function Home() {
    const { user, deviceLimit, devices, configs, setActiveTab, t, register, fetchAll } = useUserStore();
    const [activeSlide, setActiveSlide] = useState(0);
    const [showDeviceSelect, setShowDeviceSelect] = useState(false);
    const [isSettingUp, setIsSettingUp] = useState(false);

    const [eggState, setEggState] = useState<EggState>('hidden');
    const [eggClicks, setEggClicks] = useState(0);
    const [eggErrorMsg, setEggErrorMsg] = useState('');

    const touchStartX = useRef<number | null>(null);
    const touchStartY = useRef<number | null>(null);
    const isDragging = useRef(false);

    const isInitialized = user !== null && user !== undefined;
    const hasSub = user?.hasActiveSubscription ?? false;
    const totalSlides = hasSub ? 3 : 1;
    const needsDevice = hasSub && devices.length === 0;

    const { daysLeft, expireDateFormatted } = useMemo(() => {
        if (!user?.subscriptionExpiresAt) return { daysLeft: 0, expireDateFormatted: '---' };
        const expire = new Date(user.subscriptionExpiresAt);
        const diff = Math.ceil((expire.getTime() - Date.now()) / 86_400_000);
        return {
            daysLeft: diff > 0 ? diff : 0,
            expireDateFormatted: expire.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short', year: 'numeric' })
        };
    }, [user]);

    const activeDevs = deviceLimit?.activeDevices ?? devices.length;
    const maxDevs = deviceLimit?.maxDevices ?? 3;

    const stats = [
        { icon: Zap, val: '∞', label: t?.traffic || 'Трафик' },
        { icon: MonitorSmartphone, val: `${activeDevs}/${maxDevs}`, label: t?.devices || 'Устройства' },
        { icon: Globe2, val: t?.all_locations || 'Все', label: t?.locations || 'Локации' },
    ];

    const haptic = (s: 'light' | 'medium' | 'heavy' | 'success' | 'error' = 'light') => {
        const tg = window.Telegram?.WebApp;
        if (!tg) return;
        if (s === 'success' || s === 'error') {
            tg.HapticFeedback.notificationOccurred(s);
        } else {
            tg.HapticFeedback.impactOccurred(s);
        }
    };

    const handleGetAccess = () => {
        haptic('medium');
        setShowDeviceSelect(true);
    };

    const handleDeviceSelected = async (type: DeviceType) => {
        setShowDeviceSelect(false);
        try {
            if (!user) {
                await register();
            }

            const limit = await userApi.getDeviceLimit().catch(() => null);
            if (limit && limit.limitReached) {
                window.Telegram?.WebApp?.showAlert('Достигнут лимит устройств');
                return;
            }

            const deviceName = `${type} Device`;
            await userApi.registerDevice(deviceName, type);

            await fetchAll();
            setActiveTab('payments');
        } catch (e: any) {
            console.error('Device registration failed:', e);
            window.Telegram?.WebApp?.showAlert('Ошибка при создании устройства. Попробуйте ещё раз.');
        }
    };

    const handleSetupSubscription = async () => {
        haptic('medium');

        if (needsDevice) {
            window.Telegram?.WebApp?.showAlert('Сначала создайте устройство во вкладке Профиль!');
            setActiveTab('profile');
            return;
        }

        try {
            setIsSettingUp(true);

            const existingDeviceIds = new Set(configs.map(c => c.deviceId));
            const devicesWithoutConfig = devices.filter(d => !existingDeviceIds.has(d.id));

            if (devicesWithoutConfig.length === 0) {
                setActiveTab('subscriptions');
                return;
            }

            for (const device of devicesWithoutConfig) {
                try {
                    await userApi.createConfig(device.id);
                } catch (e) {
                    console.error(`Failed to create config for device ${device.id}:`, e);
                }
            }

            await fetchAll();
            setActiveTab('subscriptions');
        } catch (error) {
            console.error('Ошибка при подготовке конфигурации:', error);
            window.Telegram?.WebApp?.showAlert('Не удалось подготовить конфигурацию. Обратитесь в поддержку.');
        } finally {
            setIsSettingUp(false);
        }
    };

    const goNext = useCallback(() => {
        if (activeSlide < totalSlides - 1) {
            setActiveSlide(p => p + 1);
            haptic('light');
        } else {
            haptic('heavy');
            setEggState('playing');
            setEggClicks(0);
        }
    }, [activeSlide, totalSlides]);

    const goPrev = useCallback(() => {
        if (activeSlide > 0) {
            setActiveSlide(p => p - 1);
            haptic('light');
        } else {
            haptic('heavy');
            setEggState('playing');
            setEggClicks(0);
        }
    }, [activeSlide]);

    const handleEggClick = async () => {
        if (eggState !== 'playing') return;

        haptic('light');
        const newCount = eggClicks + 1;
        setEggClicks(newCount);

        if (newCount >= 67) {
            setEggState('loading');
            haptic('medium');
            try {
                await userApi.claimEasterEgg();
                await fetchAll();
                setEggState('success');
                haptic('success');
            } catch (e: any) {
                setEggErrorMsg(e.response?.data?.message || 'Похоже, вы уже забирали этот бонус.');
                setEggState('error');
                haptic('error');
            }
        }
    };

    const onTouchStart = (e: React.TouchEvent) => {
        touchStartX.current = e.touches[0].clientX;
        touchStartY.current = e.touches[0].clientY;
        isDragging.current = false;
    };

    const onTouchMove = (e: React.TouchEvent) => {
        if (!touchStartX.current || !touchStartY.current) return;
        const dx = Math.abs(e.touches[0].clientX - touchStartX.current);
        const dy = Math.abs(e.touches[0].clientY - touchStartY.current);
        if (dx > dy && dx > 8) isDragging.current = true;
    };

    const onTouchEnd = (e: React.TouchEvent) => {
        if (!isDragging.current || touchStartX.current === null) {
            touchStartX.current = touchStartY.current = null;
            return;
        }
        const dist = touchStartX.current - e.changedTouches[0].clientX;
        if (dist > 50) goNext();
        else if (dist < -50) goPrev();
        touchStartX.current = touchStartY.current = null;
        isDragging.current = false;
    };

    if (!isInitialized) {
        return (
            <div
                className="flex items-center justify-center"
                style={{ height: 'calc(var(--tg-height, 100dvh) - 180px)' }}
            >
                <Loader2 size={28} className="animate-spin text-white/30" />
            </div>
        );
    }

    const eggProgress = Math.min((eggClicks / 67) * 100, 100);

    return (
        <div className="flex flex-col pt-2 relative">
            <div
                className="overflow-hidden"
                onTouchStart={onTouchStart}
                onTouchMove={onTouchMove}
                onTouchEnd={onTouchEnd}
            >
                <div
                    className="flex transition-transform duration-500"
                    style={{
                        transform: `translateX(-${activeSlide * 100}%)`,
                        transitionTimingFunction: 'cubic-bezier(0.25,1,0.5,1)',
                        width: `${totalSlides * 100}%`,
                    }}
                >
                    {hasSub && (
                        <div className="flex flex-col px-2 pb-4" style={{ width: `${100 / totalSlides}%` }}>
                            <div className="relative overflow-hidden rounded-[2.5rem] p-6 border border-white/10 bg-gradient-to-b from-[#12141d] to-[#0a0a0f] shadow-[0_20px_40px_rgba(0,0,0,0.8)]">
                                <div className="absolute top-[-20%] left-[-10%] w-[150%] h-[50%] bg-amber-500/10 blur-[80px] rounded-full pointer-events-none" />

                                <div className="flex justify-between items-start mb-5 relative z-10">
                                    <div className="flex items-center gap-3">
                                        <div className="w-12 h-12 bg-gradient-to-br from-amber-400/20 to-amber-600/5 rounded-[1.2rem] flex items-center justify-center border border-amber-500/30">
                                            <Crown size={24} className="text-amber-400" />
                                        </div>
                                        <div>
                                            <h2 className="text-[20px] font-black uppercase tracking-tight text-white leading-none mb-1">
                                                {user?.subscriptionType || 'PREMIUM'}
                                            </h2>
                                            <div className="flex items-center gap-1.5">
                                                <div className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
                                                <p className="text-emerald-500 text-[10px] font-black uppercase tracking-[0.2em]">{t.active}</p>
                                            </div>
                                        </div>
                                    </div>
                                    <div className="text-right shrink-0">
                                        <p className="text-white/40 text-[9px] font-black uppercase tracking-[0.2em] mb-1">{t.until_date}</p>
                                        <p className="font-bold text-[12px] text-white/90 bg-white/5 px-2 py-1 rounded-md border border-white/5 whitespace-nowrap">{expireDateFormatted}</p>
                                    </div>
                                </div>

                                <AnimatePresence>
                                    {needsDevice && (
                                        <motion.div
                                            initial={{ height: 0, opacity: 0, marginBottom: 0 }}
                                            animate={{ height: 'auto', opacity: 1, marginBottom: 16 }}
                                            exit={{ height: 0, opacity: 0, marginBottom: 0 }}
                                            onClick={() => { setActiveTab('profile'); haptic('medium'); }}
                                            className="overflow-hidden"
                                        >
                                            <div className="p-3.5 bg-amber-500/10 border border-amber-500/30 rounded-2xl flex items-center gap-3 cursor-pointer tap-target active:scale-[0.98] transition-transform">
                                                <AlertCircle className="text-amber-500 shrink-0" size={24} />
                                                <div className="flex-1">
                                                    <p className="text-white text-[12px] font-black uppercase tracking-wide">Устройство не найдено</p>
                                                    <p className="text-white/60 text-[10px] mt-0.5 leading-tight">Нажмите здесь, чтобы создать устройство и активировать VPN.</p>
                                                </div>
                                                <ChevronRight size={18} className="text-white/40 shrink-0" />
                                            </div>
                                        </motion.div>
                                    )}
                                </AnimatePresence>

                                <div className="grid grid-cols-3 gap-2 mb-4 relative z-10">
                                    {stats.map((item, i) => (
                                        <div key={i} className="bg-black/40 rounded-2xl border border-white/5 flex flex-col items-center justify-center py-4">
                                            <item.icon size={18} className="mb-1.5 text-white/40" />
                                            <span className="text-[15px] font-black text-white">{item.val}</span>
                                            <span className="text-[8px] font-black uppercase tracking-widest text-white/30 mt-0.5 text-center">{item.label}</span>
                                        </div>
                                    ))}
                                </div>

                                <div className="flex items-center gap-3 mb-5 bg-black/40 p-3 rounded-2xl border border-white/5 relative z-10">
                                    <div className="flex-1 flex items-center gap-2">
                                        <div className="w-7 h-7 rounded-full bg-emerald-500/10 flex items-center justify-center border border-emerald-500/20 relative shrink-0">
                                            <div className="absolute inset-0 rounded-full border border-emerald-500/30 animate-ping opacity-50" />
                                            <Zap size={12} className="text-emerald-400" />
                                        </div>
                                        <div>
                                            <p className="text-[8px] text-white/40 font-black uppercase tracking-widest">Ping</p>
                                            <p className="text-[13px] font-black text-white font-mono">12 <span className="text-[9px] text-white/50">ms</span></p>
                                        </div>
                                    </div>
                                    <div className="w-px h-7 bg-white/10" />
                                    <div className="flex-1 flex items-center gap-2 pl-1">
                                        <div className="w-7 h-7 rounded-full bg-blue-500/10 flex items-center justify-center border border-blue-500/20 shrink-0">
                                            <Globe2 size={12} className="text-blue-400" />
                                        </div>
                                        <div>
                                            <p className="text-[8px] text-white/40 font-black uppercase tracking-widest">{t.channel}</p>
                                            <p className="text-[13px] font-black text-white font-mono">1 <span className="text-[9px] text-white/50">Gbit/s</span></p>
                                        </div>
                                    </div>
                                </div>

                                <div className="bg-black/40 rounded-[2rem] p-5 border border-white/10 text-center relative z-10 mb-5">
                                    <p className="text-white/40 text-[10px] font-black uppercase tracking-[0.3em] mb-1">{t.remains}</p>
                                    <div className="flex items-baseline justify-center gap-2">
                                        <span className="text-[44px] font-black tracking-tighter text-white leading-none">{daysLeft}</span>
                                        <span className="text-[14px] text-white/50 font-bold uppercase tracking-widest">{t.days}</span>
                                    </div>
                                </div>

                                <div className="space-y-2.5 relative z-10">
                                    <div className="flex gap-2.5">
                                        <button
                                            onClick={() => { setActiveTab('payments'); haptic(); }}
                                            className="flex-1 py-3.5 bg-white/5 border border-white/10 rounded-2xl font-black text-[11px] text-white/80 uppercase tracking-widest active:scale-[0.98] transition-all tap-target"
                                        >
                                            {t.renew}
                                        </button>
                                        <button
                                            onClick={() => { setActiveTab('profile'); haptic(); }}
                                            className="flex-1 py-3.5 bg-white/5 border border-white/10 rounded-2xl font-black text-[11px] text-white/80 uppercase tracking-widest active:scale-[0.98] transition-all tap-target"
                                        >
                                            {t.profile}
                                        </button>
                                    </div>
                                    <button
                                        onClick={handleSetupSubscription}
                                        disabled={isSettingUp}
                                        className={`w-full py-4 rounded-2xl font-black text-[13px] uppercase tracking-[0.1em] transition-all flex items-center justify-center gap-2 tap-target
                                            ${isSettingUp
                                            ? 'bg-white/50 text-black/50 cursor-not-allowed'
                                            : 'bg-white text-black active:scale-[0.98]'
                                        }`}
                                    >
                                        {isSettingUp ? (
                                            <>
                                                <Loader2 size={18} className="animate-spin" />
                                                Настройка...
                                            </>
                                        ) : (
                                            <>
                                                <ArrowRight size={18} className="animate-pulse" />
                                                {t.setup_subscription}
                                            </>
                                        )}
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}

                    {hasSub && (
                        <div className="flex flex-col px-2 pb-4" style={{ width: `${100 / totalSlides}%` }}>
                            <div
                                className="relative overflow-hidden rounded-[2.5rem] border border-white/10 shadow-2xl"
                                style={{ background: '#0a0a0a' }}
                            >
                                <img
                                    src={bgImage}
                                    alt="GEO VPN"
                                    style={{
                                        display: 'block',
                                        width: '100%',
                                        height: 'auto',
                                        maxHeight: 'calc(var(--tg-height, 100dvh) - 180px)',
                                        objectFit: 'contain',
                                    }}
                                />

                                <div
                                    style={{
                                        position: 'absolute',
                                        inset: 0,
                                        background: 'linear-gradient(to top, rgba(0,0,0,0.95) 0%, rgba(0,0,0,0.1) 40%, transparent 100%)',
                                        pointerEvents: 'none',
                                    }}
                                />

                                <div
                                    style={{
                                        position: 'absolute',
                                        bottom: 0,
                                        left: 0,
                                        right: 0,
                                        padding: '1.25rem',
                                        zIndex: 2,
                                    }}
                                >
                                    <button
                                        onClick={() => { setActiveTab('payments'); haptic('medium'); }}
                                        className="w-full bg-white text-black rounded-2xl font-black text-[13px] uppercase tracking-[0.1em] active:scale-[0.98] transition-all flex items-center justify-center gap-2 tap-target"
                                        style={{ minHeight: 56 }}
                                    >
                                        <Crown size={18} />
                                        <span>{t.upgrade_subscription ?? 'Улучшить подписку'}</span>
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}

                    <div className="flex flex-col px-2 pb-4" style={{ width: `${100 / totalSlides}%` }}>
                        <div
                            className="relative overflow-hidden rounded-[2.5rem] border border-white/10 shadow-2xl"
                            style={{ background: '#0a0a0a' }}
                        >
                            <img
                                src={bgImage}
                                alt="GEO VPN"
                                style={{
                                    display: 'block',
                                    width: '100%',
                                    height: 'auto',
                                    maxHeight: 'calc(var(--tg-height, 100dvh) - 180px)',
                                    objectFit: 'contain',
                                }}
                            />

                            <div
                                style={{
                                    position: 'absolute',
                                    inset: 0,
                                    background: 'linear-gradient(to top, rgba(0,0,0,0.95) 0%, rgba(0,0,0,0.1) 40%, transparent 100%)',
                                    pointerEvents: 'none',
                                }}
                            />

                            <div
                                style={{
                                    position: 'absolute',
                                    bottom: 0,
                                    left: 0,
                                    right: 0,
                                    padding: '1.25rem',
                                    zIndex: 2,
                                }}
                            >
                                <button
                                    onClick={handleGetAccess}
                                    className="w-full bg-white text-black rounded-2xl font-black text-[13px] uppercase tracking-[0.1em] active:scale-[0.98] transition-all flex items-center justify-center gap-2 tap-target"
                                    style={{ minHeight: 56 }}
                                >
                                    <span>{t.get_access}</span>
                                    <ChevronRight size={20} />
                                </button>
                            </div>
                        </div>
                    </div>

                </div>
            </div>

            {totalSlides > 1 && (
                <div className="flex justify-center gap-2 py-4">
                    {Array.from({ length: totalSlides }).map((_, i) => (
                        <div
                            key={i}
                            onClick={() => { setActiveSlide(i); haptic('light'); }}
                            className={`h-1.5 rounded-full transition-all duration-500 cursor-pointer ${
                                activeSlide === i ? 'w-10 bg-white' : 'w-2 bg-white/20'
                            }`}
                        />
                    ))}
                </div>
            )}

            <AnimatePresence>
                {eggState !== 'hidden' && (
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-xl"
                    >
                        <motion.div
                            initial={{ scale: 0.8, y: 50, opacity: 0 }}
                            animate={{ scale: 1, y: 0, opacity: 1 }}
                            exit={{ scale: 0.8, y: 50, opacity: 0 }}
                            transition={{ type: "spring", bounce: 0.4, duration: 0.6 }}
                            className="w-full max-w-sm relative flex flex-col items-center text-center"
                        >
                            <div className="absolute inset-0 bg-gradient-to-br from-rose-500/20 via-[#12141d] to-amber-500/20 rounded-[2.5rem] blur-xl" />

                            <div className="relative bg-[#0d0e15]/90 border border-white/10 rounded-[2.5rem] p-6 w-full shadow-[0_0_50px_rgba(244,63,94,0.15)] overflow-hidden flex flex-col items-center">

                                <div className="absolute inset-0 opacity-[0.03] bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-white via-transparent to-transparent bg-[length:20px_20px]" style={{ backgroundImage: 'radial-gradient(circle, #ffffff 1px, transparent 1px)' }} />

                                <button
                                    onClick={() => setEggState('hidden')}
                                    className="absolute top-5 right-5 w-8 h-8 flex items-center justify-center bg-white/5 border border-white/10 rounded-full text-white/40 hover:text-white hover:bg-white/10 transition-all z-10"
                                >
                                    <X size={16} />
                                </button>

                                {eggState === 'playing' && (
                                    <motion.div
                                        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                                        className="flex flex-col items-center w-full z-10"
                                    >
                                        <div className="w-16 h-16 relative flex items-center justify-center mb-4">
                                            <div className="absolute inset-0 bg-rose-500/20 rounded-2xl blur animate-pulse" />
                                            <div className="relative bg-gradient-to-br from-rose-500/10 to-transparent border border-rose-500/30 w-full h-full rounded-2xl flex items-center justify-center">
                                                <ShieldAlert className="text-rose-400" size={32} />
                                            </div>
                                        </div>

                                        <h3 className="text-xl font-black text-transparent bg-clip-text bg-gradient-to-r from-rose-400 to-amber-400 uppercase tracking-widest mb-2">
                                            Системный Баг
                                        </h3>
                                        <p className="text-white/60 text-xs mb-6 px-4 leading-relaxed">
                                            Мы не стали его убирать. Решите загадку и разблокируйте <b className="text-white">2 дня Premium</b>!
                                        </p>

                                        <motion.div
                                            whileTap={{ scale: 0.85, rotate: (Math.random() - 0.5) * 10 }}
                                            onClick={handleEggClick}
                                            className="relative mb-8 cursor-pointer select-none tap-target"
                                        >
                                            <div className="absolute inset-0 bg-white/5 blur-2xl rounded-full" />
                                            <img
                                                src={bugImage}
                                                alt="Riddle"
                                                className="w-40 h-40 object-contain relative z-10 drop-shadow-[0_0_15px_rgba(255,255,255,0.2)]"
                                                draggable="false"
                                            />
                                        </motion.div>

                                        <div className="w-full bg-white/5 border border-white/10 rounded-2xl p-4">
                                            <div className="flex justify-between items-end mb-2">
                                                <span className="text-[10px] font-black text-white/40 uppercase tracking-widest">Процесс взлома</span>
                                                <span className="text-[14px] font-black text-white">{eggClicks} <span className="text-white/40 text-[10px]">/ 67</span></span>
                                            </div>
                                            <div className="h-2 w-full bg-black/50 rounded-full overflow-hidden border border-white/5 relative">
                                                <motion.div
                                                    className="absolute top-0 left-0 h-full bg-gradient-to-r from-rose-500 to-amber-400"
                                                    initial={{ width: 0 }}
                                                    animate={{ width: `${eggProgress}%` }}
                                                    transition={{ type: 'spring', stiffness: 300, damping: 30 }}
                                                />
                                            </div>
                                        </div>
                                    </motion.div>
                                )}

                                {eggState === 'loading' && (
                                    <div className="flex flex-col items-center justify-center py-10 z-10">
                                        <Loader2 size={48} className="text-amber-400 animate-spin mb-4" />
                                        <p className="text-sm font-black text-white/70 uppercase tracking-widest animate-pulse">Применение эксплоита...</p>
                                    </div>
                                )}

                                {eggState === 'success' && (
                                    <motion.div
                                        initial={{ scale: 0.9, opacity: 0 }} animate={{ scale: 1, opacity: 1 }}
                                        className="flex flex-col items-center w-full z-10 py-4"
                                    >
                                        <div className="w-24 h-24 relative flex items-center justify-center mb-6">
                                            <div className="absolute inset-0 bg-emerald-500/30 rounded-full blur-2xl animate-pulse" />
                                            <div className="relative bg-emerald-500 text-white w-16 h-16 rounded-full flex items-center justify-center shadow-[0_0_30px_rgba(16,185,129,0.5)]">
                                                <CheckCircle2 size={36} />
                                            </div>
                                        </div>
                                        <h3 className="text-2xl font-black text-white uppercase tracking-tight mb-2">
                                            Взлом успешен!
                                        </h3>
                                        <p className="text-emerald-400 text-sm font-bold uppercase tracking-widest mb-6">
                                            +2 дня Premium добавлено
                                        </p>
                                        <button
                                            onClick={() => setEggState('hidden')}
                                            className="w-full py-4 bg-white text-black rounded-2xl font-black text-[13px] uppercase tracking-[0.1em] active:scale-[0.98] transition-transform"
                                        >
                                            Продолжить
                                        </button>
                                    </motion.div>
                                )}

                                {eggState === 'error' && (
                                    <motion.div
                                        initial={{ scale: 0.9, opacity: 0 }} animate={{ scale: 1, opacity: 1 }}
                                        className="flex flex-col items-center w-full z-10 py-4"
                                    >
                                        <div className="w-16 h-16 bg-red-500/10 border border-red-500/30 rounded-2xl flex items-center justify-center mb-6">
                                            <ShieldAlert className="text-red-500" size={32} />
                                        </div>
                                        <h3 className="text-xl font-black text-white uppercase tracking-tight mb-2">
                                            Доступ закрыт
                                        </h3>
                                        <p className="text-white/60 text-sm mb-6 px-2">
                                            {eggErrorMsg}
                                        </p>
                                        <button
                                            onClick={() => setEggState('hidden')}
                                            className="w-full py-4 bg-white/10 text-white rounded-2xl font-black text-[13px] uppercase tracking-[0.1em] active:scale-[0.98] transition-transform"
                                        >
                                            Понятно
                                        </button>
                                    </motion.div>
                                )}

                            </div>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>

            <AnimatePresence>
                {showDeviceSelect && (
                    <DeviceSelector onSelect={handleDeviceSelected} onClose={() => setShowDeviceSelect(false)} />
                )}
            </AnimatePresence>
        </div>
    );
}