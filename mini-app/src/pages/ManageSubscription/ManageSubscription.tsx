import React, { useState, useMemo, useEffect } from 'react';
import { useUserStore } from '../../store/userStore';
import { userApi } from '../../api/user';
import { motion, AnimatePresence } from 'framer-motion';
import {
    Smartphone, Monitor, Plus, ArrowLeft, RefreshCcw,
    Loader2, Globe, Copy, X, Check, Link, Activity,
    Zap, Trash2, KeyRound, ShieldAlert, Settings2,
    ShieldCheck, ChevronDown
} from 'lucide-react';

const safeConfirm = (text: string, callback: (ok: boolean) => void) => {
    if (window.Telegram?.WebApp?.isVersionAtLeast('6.2')) {
        window.Telegram.WebApp.showConfirm(text, callback);
    } else {
        callback(window.confirm(text));
    }
};

export default function ManageSubscription() {
    const { user, devices, configs, deviceLimit, setActiveTab, deleteDevice, regenerateConfig } = useUserStore();

    const [expandedDevId, setExpandedDevId] = useState<number | null>(null);
    const [copyStatus, setCopyStatus] = useState(false);

    const [isBuyingSlot, setIsBuyingSlot] = useState(false);
    const [isRegenerating, setIsRegenerating] = useState<number | null>(null);
    const [isDeleting, setIsDeleting] = useState<number | null>(null);
    const [isTogglingRenew, setIsTogglingRenew] = useState(false);

    const activeDevsCount = devices.length;
    const maxDevsCount = deviceLimit?.maxDevices ?? 1;
    const hasFreeSlots = activeDevsCount < maxDevsCount;

    const isPC = useMemo(() => {
        const p = window.Telegram?.WebApp?.platform || 'unknown';
        return ['tdesktop', 'macos', 'windows'].includes(p);
    }, []);

    const isExpired = useMemo(() => {
        if (!user?.subscriptionExpiresAt) return true;
        return new Date(user.subscriptionExpiresAt).getTime() < new Date().getTime();
    }, [user]);

    useEffect(() => {
        if (isExpired) {
            setActiveTab('payments');
            window.Telegram?.WebApp?.HapticFeedback.notificationOccurred('error');
        }
    }, [isExpired, setActiveTab]);

    if (isExpired) return null;

    const daysLeft = useMemo(() => {
        if (!user?.subscriptionExpiresAt) return 0;
        const diff = new Date(user.subscriptionExpiresAt).getTime() - new Date().getTime();
        return Math.max(0, Math.ceil(diff / (1000 * 3600 * 24)));
    }, [user]);

    const handleAutoConnect = (deviceId: number) => {
        const config = configs.find(c => c.deviceId === deviceId);
        if (!config) return;
        const uuid = config.subscriptionUrl.split('/').pop();
        window.Telegram?.WebApp?.openLink(`https://geovp.ru/api/v1/subscription/${uuid}/import-happ`);
    };

    const copyToClipboard = (text: string) => {
        navigator.clipboard.writeText(text);
        setCopyStatus(true);
        window.Telegram?.WebApp?.HapticFeedback.impactOccurred('medium');
        setTimeout(() => setCopyStatus(false), 2000);
    };

    const handleBuyExtraSlot = async () => {
        safeConfirm("Увеличить лимит устройств на +1 слот? \n\nСтоимость: 100 ₽. Списание с баланса.", async (ok) => {
            if (ok) {
                if (user!.balance < 10000) {
                    setActiveTab('deposit');
                    return;
                }
                try {
                    setIsBuyingSlot(true);
                    await userApi.purchaseExtraSlot();
                    const [updatedLimit, updatedUser] = await Promise.all([userApi.getDeviceLimit(), userApi.getProfile()]);
                    useUserStore.setState({ deviceLimit: updatedLimit, user: updatedUser });
                    window.Telegram?.WebApp?.HapticFeedback.notificationOccurred('success');
                } catch (e: any) {
                    alert("Ошибка: " + (e.response?.data?.error?.message || e.message));
                } finally {
                    setIsBuyingSlot(false);
                }
            }
        });
    };

    const handleRegenerate = async (deviceId: number) => {
        safeConfirm("Вы уверены? Старый ключ перестанет работать на всех устройствах, и вам придется заново импортировать подписку.", async (ok) => {
            if (ok) {
                try {
                    setIsRegenerating(deviceId);
                    await regenerateConfig(deviceId);
                    window.Telegram?.WebApp?.HapticFeedback.notificationOccurred('success');
                    window.Telegram?.WebApp?.showAlert("Ключ успешно обновлен! Сделайте Авто-импорт заново.");
                } catch (e: any) {
                    window.Telegram?.WebApp?.showAlert("Ошибка: " + e.message);
                } finally {
                    setIsRegenerating(null);
                }
            }
        });
    };

    const handleDelete = async (uuid: string, deviceId: number) => {
        if (devices.length <= 1) {
            window.Telegram?.WebApp?.showAlert("Нельзя удалить единственное устройство.");
            return;
        }
        safeConfirm("Удалить это устройство? Доступ к VPN по этому ключу будет немедленно закрыт.", async (ok) => {
            if (ok) {
                try {
                    setIsDeleting(deviceId);
                    await deleteDevice(uuid);
                    setExpandedDevId(null);
                    window.Telegram?.WebApp?.HapticFeedback.notificationOccurred('success');
                } catch (e: any) {
                    window.Telegram?.WebApp?.showAlert("Ошибка: " + e.message);
                } finally {
                    setIsDeleting(null);
                }
            }
        });
    };

    //todo заглушка автопродления
    const handleToggleAutoRenew = async () => {
        try {
            setIsTogglingRenew(true);
            window.Telegram?.WebApp?.HapticFeedback.impactOccurred('light');
            setTimeout(() => setIsTogglingRenew(false), 500);
        } catch (e) {
            setIsTogglingRenew(false);
        }
    };

    return (
        <div className="flex flex-col gap-4 pt-2 pb-32 px-1 animate-in fade-in duration-500 text-left relative h-full">

            <button onClick={() => setActiveTab('profile')} className="flex items-center gap-2 text-white/40 mb-1 font-black text-[10px] uppercase tracking-[0.2em] px-2 outline-none hover:text-white transition-colors">
                <ArrowLeft size={14} /> Назад
            </button>

            {/* ПРЕМИАЛЬНАЯ КАРТОЧКА ТАРИФА */}
            <div className="relative rounded-[2.5rem] p-7 shadow-[0_20px_50px_rgba(0,0,0,0.5)] overflow-hidden bg-gradient-to-br from-[#1a1c29] to-[#0d0e15] border border-white/[0.08]">
                <div className="absolute -top-10 -right-10 w-48 h-48 bg-emerald-500/20 blur-[70px] rounded-full pointer-events-none" />
                <div className="absolute -bottom-10 -left-10 w-40 h-40 bg-blue-500/10 blur-[60px] rounded-full pointer-events-none" />

                <div className="flex justify-between items-start mb-8 relative z-10">
                    <div className="flex flex-col gap-1.5">
                        <div className="flex items-center gap-3">
                            <h2 className="text-[26px] font-black text-white uppercase italic tracking-tighter drop-shadow-lg">{user?.subscriptionType}</h2>
                            <div className="flex items-center gap-1 bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-[8px] font-black uppercase px-2.5 py-1 rounded-full shadow-[0_0_15px_rgba(16,185,129,0.2)]">
                                <div className="w-1.5 h-1.5 bg-emerald-400 rounded-full animate-pulse" /> ACTIVE
                            </div>
                        </div>
                        <p className="text-white/30 text-[9px] font-black uppercase tracking-[0.2em] flex items-center gap-1.5 mt-1">
                            <ShieldCheck size={12} className="text-emerald-500" /> Защита включена
                        </p>
                    </div>
                    <div className="text-right">
                        <span className="text-[42px] font-black text-white leading-none tracking-tighter drop-shadow-xl">{daysLeft}</span>
                        <span className="block text-emerald-500 text-[9px] font-black uppercase tracking-[0.15em] mt-1">Дней осталось</span>
                    </div>
                </div>

                <div className="relative z-10 mb-5">
                    <div className="flex justify-between text-[10px] font-bold uppercase tracking-widest text-white/30 mb-2">
                        <span>Прогресс</span>
                        <span className="text-white/60">{new Date(user?.subscriptionExpiresAt || '').toLocaleDateString()}</span>
                    </div>
                    <div className="h-1.5 w-full bg-black/50 rounded-full overflow-hidden border border-white/5 p-[1px] shadow-inner">
                        <motion.div
                            initial={{ width: 0 }}
                            animate={{ width: `${Math.min(100, (daysLeft / 30) * 100)}%` }}
                            className="h-full bg-gradient-to-r from-emerald-600 via-emerald-400 to-emerald-300 rounded-full relative"
                        >
                            <div className="absolute right-0 top-0 bottom-0 w-4 bg-white/40 blur-sm" />
                        </motion.div>
                    </div>
                </div>

                <div className="flex justify-between items-center pt-4 border-t border-white/[0.05] relative z-10">
                    <span className="flex items-center gap-2 text-[10px] font-black uppercase tracking-widest text-white/40">
                        <Globe size={12} className="text-blue-400" /> Безлимитный канал
                    </span>
                    <span className="bg-white/5 border border-white/10 px-3 py-1.5 rounded-xl text-[10px] font-black uppercase tracking-widest text-white/70 shadow-sm">
                        {activeDevsCount} / {maxDevsCount} устройств
                    </span>
                </div>
            </div>

            {/* СИСТЕМНЫЕ НАСТРОЙКИ */}
            <div className="bg-[#12141d]/80 border border-white/5 rounded-[2rem] p-5 flex items-center justify-between shadow-lg mx-1">
                <div className="flex items-center gap-4">
                    <div className="w-10 h-10 rounded-2xl bg-amber-500/10 flex items-center justify-center border border-amber-500/20 text-amber-500">
                        {isTogglingRenew ? <Loader2 size={18} className="animate-spin" /> : <RefreshCcw size={18} />}
                    </div>
                    <div className="text-left">
                        <p className="text-[13px] font-black text-white uppercase tracking-tight">Автопродление</p>
                        <p className="text-[9px] text-white/40 font-bold uppercase tracking-widest mt-0.5">Списание с баланса</p>
                    </div>
                </div>
                {/* Тумблер */}
                <div onClick={handleToggleAutoRenew} className="w-12 h-6 rounded-full bg-emerald-500 p-1 cursor-pointer transition-colors shadow-inner flex items-center">
                    <motion.div animate={{ x: 24 }} className="w-4 h-4 bg-white rounded-full shadow-md" />
                </div>
            </div>

            {/* МЕНЕДЖЕР ПОДКЛЮЧЕНИЙ */}
            <div className="flex flex-col gap-3 mt-2">
                <div className="flex items-center justify-between px-2">
                    <h3 className="text-[14px] font-black text-white/40 uppercase tracking-[0.2em] italic flex items-center gap-2">
                        <Settings2 size={16} /> Менеджер устройств
                    </h3>
                </div>

                <div className="flex flex-col gap-3">
                    {devices.map((dev, idx) => {
                        const isExpanded = expandedDevId === dev.id;
                        const config = configs.find(c => c.deviceId === dev.id);

                        return (
                            <div
                                key={dev.id}
                                className={`rounded-[2rem] border transition-all duration-300 ${isExpanded ? 'bg-gradient-to-b from-white/[0.08] to-transparent border-white/20 shadow-2xl backdrop-blur-md' : 'bg-[#12141d]/80 border-white/5 shadow-lg'}`}
                            >
                                {/* Шапка устройства (Кликабельная) */}
                                <button
                                    onClick={() => setExpandedDevId(isExpanded ? null : dev.id)}
                                    className="w-full flex items-center p-5 gap-4 outline-none"
                                >
                                    <div className={`w-12 h-12 rounded-2xl flex items-center justify-center border transition-all duration-300 shrink-0 ${isExpanded ? 'bg-emerald-500 border-emerald-400 text-black shadow-[0_0_20px_rgba(16,185,129,0.3)]' : 'bg-white/5 border-white/10 text-white/50'}`}>
                                        {dev.deviceType?.includes('WINDOW') || dev.deviceType?.includes('MACOS') ? <Monitor size={22} /> : <Smartphone size={22} />}
                                    </div>
                                    <div className="flex-1 text-left overflow-hidden">
                                        <p className="text-[16px] font-black text-white uppercase italic tracking-tight leading-none mb-1.5 truncate">
                                            {dev.deviceName}
                                        </p>
                                        <div className="flex items-center gap-2">
                                            <div className="px-2 py-0.5 rounded bg-black/40 border border-white/5 text-[8px] text-white/40 font-bold uppercase tracking-widest shrink-0">
                                                Слот 0{idx + 1}
                                            </div>
                                            <p className="text-[10px] text-emerald-500/70 font-bold uppercase tracking-widest flex items-center gap-1">
                                                <Activity size={10} /> Online
                                            </p>
                                        </div>
                                    </div>
                                </button>

                                <AnimatePresence initial={false}>
                                    {isExpanded && (
                                        <motion.div
                                            initial={{ height: 0, opacity: 0 }}
                                            animate={{ height: 'auto', opacity: 1 }}
                                            exit={{ height: 0, opacity: 0 }}
                                            transition={{ duration: 0.3, ease: "easeInOut" }}
                                        >
                                            <div className="px-5 pb-5 flex flex-col gap-3">

                                                <div className="w-full h-px bg-white/5 mb-2" />

                                                <div className="p-4 bg-black/30 border border-white/5 rounded-[1.5rem] flex flex-col gap-2">
                                                    <button
                                                        onClick={() => handleAutoConnect(dev.id)}
                                                        className="w-full py-4 bg-white text-black rounded-xl flex items-center justify-center gap-3 font-black text-[13px] uppercase active:scale-[0.98] transition-all shadow-[0_10px_20px_rgba(255,255,255,0.1)] outline-none"
                                                    >
                                                        <Zap size={18} className="fill-black" /> Авто-импорт в Happ
                                                    </button>

                                                    {isPC && (
                                                        <button
                                                            onClick={() => copyToClipboard(config?.subscriptionUrl || '')}
                                                            className="w-full py-3 bg-white/5 border border-white/10 text-white rounded-xl flex items-center justify-center gap-2 font-black text-[11px] uppercase tracking-widest active:bg-white/10 transition-all outline-none mt-1"
                                                        >
                                                            {copyStatus ? <Check size={16} className="text-emerald-500" /> : <Link size={16} className="text-white/50" />}
                                                            {copyStatus ? 'Скопировано!' : 'Скопировать ссылку'}
                                                        </button>
                                                    )}
                                                </div>

                                                <div className="flex gap-2 w-full mt-1">
                                                    <button
                                                        onClick={() => handleRegenerate(dev.id)}
                                                        disabled={isRegenerating === dev.id}
                                                        className="flex-1 py-3 bg-blue-500/10 border border-blue-500/20 text-blue-400 rounded-xl flex items-center justify-center gap-2 font-black text-[10px] uppercase tracking-widest active:bg-blue-500/20 transition-all outline-none"
                                                    >
                                                        {isRegenerating === dev.id ? <Loader2 size={16} className="animate-spin" /> : <KeyRound size={16} />}
                                                        Сброс ключа
                                                    </button>

                                                    <button
                                                        onClick={() => handleDelete(dev.uuid, dev.id)}
                                                        disabled={isDeleting === dev.id}
                                                        className="flex-1 py-3 bg-red-500/10 border border-red-500/20 text-red-400 rounded-xl flex items-center justify-center gap-2 font-black text-[10px] uppercase tracking-widest active:bg-red-500/20 transition-all outline-none"
                                                    >
                                                        {isDeleting === dev.id ? <Loader2 size={16} className="animate-spin" /> : <Trash2 size={16} />}
                                                        Удалить
                                                    </button>
                                                </div>

                                                <div className="flex items-start gap-2 mt-2 bg-white/[0.02] p-3 rounded-xl border border-white/5">
                                                    <ShieldAlert size={14} className="text-white/30 shrink-0 mt-0.5" />
                                                    <p className="text-[8px] text-white/40 font-bold uppercase leading-relaxed tracking-widest text-left">
                                                        Сброс ключа помогает, если доступ был скомпрометирован. Удаление стирает устройство навсегда.
                                                    </p>
                                                </div>

                                            </div>
                                        </motion.div>
                                    )}
                                </AnimatePresence>
                            </div>
                        );
                    })}

                    {!hasFreeSlots ? (
                        <button onClick={handleBuyExtraSlot} disabled={isBuyingSlot} className="w-full p-6 bg-gradient-to-b from-[#12141d] to-[#0a0a0f] border border-emerald-500/20 rounded-[2rem] flex flex-col items-center gap-3 active:scale-[0.98] transition-all group mt-2 shadow-[0_10px_30px_rgba(16,185,129,0.05)]">
                            {isBuyingSlot ? <Loader2 size={24} className="animate-spin text-emerald-500" /> : (
                                <>
                                    <div className="w-12 h-12 rounded-full bg-emerald-500/10 flex items-center justify-center text-emerald-500 border border-emerald-500/20 shadow-[0_0_20px_rgba(16,185,129,0.15)] group-active:rotate-90 transition-transform duration-300">
                                        <Plus size={24} strokeWidth={3} />
                                    </div>
                                    <div className="text-center">
                                        <p className="text-emerald-500 font-black text-[13px] uppercase tracking-widest drop-shadow-md">Купить слот за 100 ₽</p>
                                        <p className="text-white/30 text-[9px] font-bold uppercase mt-1 tracking-[0.1em]">До конца текущей подписки</p>
                                    </div>
                                </>
                            )}
                        </button>
                    ) : (
                        <button onClick={() => setActiveTab('profile')} className="w-full p-6 bg-white/5 border border-white/10 rounded-[2rem] flex flex-col items-center gap-3 active:scale-[0.98] transition-all text-white/50 hover:text-white hover:bg-white/10 mt-2 shadow-xl">
                            <div className="w-12 h-12 rounded-full bg-white/5 flex items-center justify-center border border-white/10">
                                <Plus size={24} />
                            </div>
                            <span className="text-[12px] font-black uppercase tracking-[0.2em]">Привязать новое устройство</span>
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
}