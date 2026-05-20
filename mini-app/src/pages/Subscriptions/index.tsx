import React, { useState, useMemo, useEffect } from 'react';
import { useUserStore } from '../../store/userStore';
import {
    Download, Rocket, Smartphone, Laptop, Tv,
    ShieldCheck, Info, Check, ArrowLeft, ArrowRight,
    Loader2, Copy, ExternalLink, HelpCircle, ChevronRight, Zap
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { apiClient } from '../../api/client';

export default function Subscriptions() {
    const { configs, user, devices, setActiveTab } = useUserStore();
    const [step, setStep] = useState(1);
    const [isConnecting, setIsConnecting] = useState(false);
    const [copyStatus, setCopyStatus] = useState(false);

    const [selectedDeviceId, setSelectedDeviceId] = useState<number | null>(null);

    const platform = useMemo(() => {
        const ua = navigator.userAgent.toLowerCase();
        if (ua.includes('appletv')) return { id: 'tv', name: 'Apple TV', icon: Tv, app: 'Happ TV', link: 'https://apps.apple.com/us/app/happ-proxy-utility-for-tv/id6748297274' };
        if (/iphone|ipad|ipod/.test(ua)) return { id: 'ios', name: 'iPhone / iPad', icon: Smartphone, app: 'Happ Plus', link: 'https://apps.apple.com/ru/app/happ-proxy-utility-plus/id6746188973' };
        if (/android/.test(ua)) return { id: 'android', name: 'Android', icon: Smartphone, app: 'Happ Proxy', link: 'https://play.google.com/store/apps/details?id=com.happproxy' };
        return { id: 'pc', name: 'PC / Windows', icon: Laptop, app: 'Happ Desktop', link: 'https://github.com/Happ-proxy/happ-desktop/releases/latest' };
    }, []);

    useEffect(() => {
        if (configs.length > 0 && !selectedDeviceId) {
            setSelectedDeviceId(configs[0].deviceId);
        }
    }, [configs, selectedDeviceId]);

    const activeConfig = configs.find(c => c.deviceId === selectedDeviceId) || configs[0];

    const handleAutoConnect = () => {
        if (!activeConfig?.subscriptionUrl) return;
        setIsConnecting(true);
        window.Telegram?.WebApp?.HapticFeedback.impactOccurred('heavy');

        const urlParts = activeConfig.subscriptionUrl.split('/');
        const uuid = urlParts[urlParts.length - 1];

        const redirectUrl = `https://geovp.ru/api/v1/subscription/${uuid}/import-happ`;

        if (window.Telegram?.WebApp) {
            window.Telegram.WebApp.openLink(redirectUrl);
        } else {
            window.location.href = redirectUrl;
        }

        setTimeout(() => setIsConnecting(false), 3000);
    };

    const handleCopyLink = async () => {
        if (!activeConfig) return;

        await navigator.clipboard.writeText(activeConfig.subscriptionUrl);

        setCopyStatus(true);
        window.Telegram?.WebApp?.HapticFeedback.notificationOccurred('success');
        setTimeout(() => setCopyStatus(false), 3000);
    };

    const haptic = (s: 'light' | 'medium' = 'light') =>
        window.Telegram?.WebApp?.HapticFeedback.impactOccurred(s);

    return (
        <div className="flex flex-col min-h-screen overflow-y-auto custom-scrollbar px-3 pb-40 pt-2 animate-in fade-in duration-500">

            <div className="flex items-center justify-between mb-6 px-1">
                <button
                    onClick={() => { setStep(s => Math.max(1, s - 1)); haptic(); }}
                    className={`p-2 -ml-2 text-white/40 active:text-white transition-colors ${step === 1 ? 'opacity-0 pointer-events-none' : ''}`}
                >
                    <ArrowLeft size={20} />
                </button>

                <div className="flex gap-1.5">
                    {[1, 2, 3].map(i => (
                        <div key={i} className={`h-1 rounded-full transition-all duration-300 ${step >= i ? 'w-6 bg-white' : 'w-2 bg-white/10'}`} />
                    ))}
                </div>

                <div className="flex items-center gap-2 bg-white/5 px-3 py-1 rounded-full border border-white/10">
                    <platform.icon size={12} className="text-white/60" />
                    <span className="text-[10px] font-black uppercase text-white/40 tracking-widest">{platform.name}</span>
                </div>
            </div>

            <AnimatePresence mode="wait">
                {step === 1 && (
                    <motion.div
                        key="step1"
                        initial={{ x: 20, opacity: 0 }} animate={{ x: 0, opacity: 1 }} exit={{ x: -20, opacity: 0 }}
                        className="space-y-4"
                    >
                        <div className="bg-gradient-to-b from-[#1a1c29] to-[#0a0a0f] border border-white/10 rounded-[2.5rem] p-8 shadow-2xl relative overflow-hidden text-center">
                            <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-transparent via-white/20 to-transparent" />
                            <div className="w-20 h-20 bg-white/5 rounded-[2rem] flex items-center justify-center mx-auto mb-6 border border-white/10 shadow-inner">
                                <Download size={36} className="text-white" />
                            </div>
                            <h3 className="text-[24px] font-black text-white uppercase italic mb-3 tracking-tight">Шаг 1. Клиент</h3>
                            <p className="text-[15px] text-white/50 leading-relaxed mb-8">
                                Для работы VPN необходимо установить приложение <span className="text-white font-bold">{platform.app}</span>
                            </p>

                            <div className="space-y-3">
                                <button
                                    onClick={() => window.Telegram?.WebApp?.openLink(platform.link)}
                                    className="w-full py-5 bg-white text-black rounded-2xl font-black text-[14px] uppercase tracking-widest active:scale-[0.98] transition-all flex items-center justify-center gap-2 shadow-[0_10px_25px_rgba(255,255,255,0.15)]"
                                >
                                    <span>Установить</span>
                                    <ExternalLink size={18} />
                                </button>

                                <button
                                    onClick={() => { setStep(3); haptic('medium'); }}
                                    className="w-full py-4 bg-white/5 border border-white/10 text-white/60 rounded-2xl font-black text-[11px] uppercase tracking-widest active:bg-white/10 transition-all"
                                >
                                    Приложение уже есть
                                </button>
                            </div>
                        </div>

                        <button
                            onClick={() => { setStep(2); haptic(); }}
                            className="w-full py-5 bg-emerald-500 text-black rounded-[1.8rem] font-black text-[14px] uppercase tracking-widest flex items-center justify-center gap-2 active:scale-95 transition-all"
                        >
                            <span>Далее</span>
                            <ArrowRight size={20} />
                        </button>
                    </motion.div>
                )}

                {step === 2 && (
                    <motion.div
                        key="step2"
                        initial={{ x: 20, opacity: 0 }} animate={{ x: 0, opacity: 1 }} exit={{ x: -20, opacity: 0 }}
                        className="space-y-4"
                    >
                        <div className="bg-gradient-to-b from-[#1a1c29] to-[#0a0a0f] border border-white/10 rounded-[2.5rem] p-8 shadow-2xl relative overflow-hidden text-center">
                            <div className="w-20 h-20 bg-emerald-500/10 rounded-[2rem] flex items-center justify-center mx-auto mb-6 border border-emerald-500/20 shadow-[0_0_30px_rgba(16,185,129,0.1)]">
                                <ShieldCheck size={36} className="text-emerald-500" />
                            </div>
                            <h3 className="text-[24px] font-black text-white uppercase italic mb-3 tracking-tight">Шаг 2. Запуск</h3>
                            <p className="text-[15px] text-white/50 leading-relaxed mb-8">
                                Откройте <span className="text-white font-bold">{platform.app}</span> и разрешите установку VPN-конфигурации в системе.
                            </p>

                            <div className="bg-black/40 border border-white/5 rounded-2xl p-4 flex items-start gap-3 text-left">
                                <Info size={18} className="text-amber-500 shrink-0 mt-1" />
                                <p className="text-[12px] text-white/40 font-medium">Это стандартная процедура для любого VPN-приложения, чтобы оно могло управлять трафиком.</p>
                            </div>
                        </div>

                        <button
                            onClick={() => { setStep(3); haptic('medium'); }}
                            className="w-full py-5 bg-white text-black rounded-[1.8rem] font-black text-[14px] uppercase tracking-widest flex items-center justify-center gap-2 active:scale-95 transition-all"
                        >
                            <span>Все готово, далее</span>
                            <ArrowRight size={20} />
                        </button>
                    </motion.div>
                )}

                {step === 3 && (
                    <motion.div
                        key="step3"
                        initial={{ y: 20, opacity: 0 }} animate={{ y: 0, opacity: 1 }} exit={{ y: -20, opacity: 0 }}
                        className="space-y-4"
                    >
                        <div className="bg-gradient-to-b from-[#1a1c29] to-[#0a0a0f] border border-white/10 rounded-[2.5rem] p-8 shadow-2xl relative overflow-hidden">
                            <h3 className="text-[22px] font-black text-white uppercase italic mb-6 text-center tracking-tight">Шаг 3. Подключение</h3>

                            {configs.length > 1 && (
                                <div className="mb-6 -mx-2">
                                    <p className="text-[9px] font-black text-white/40 uppercase tracking-widest mb-3 text-center">
                                        Выберите устройство
                                    </p>
                                    <div className="flex gap-2 overflow-x-auto custom-scrollbar pb-2 justify-start px-2">
                                        {configs.map((c) => {
                                            const dev = devices.find(d => d.id === c.deviceId);
                                            const isActive = activeConfig?.deviceId === c.deviceId;
                                            return (
                                                <button
                                                    key={c.id}
                                                    onClick={() => { setSelectedDeviceId(c.deviceId); haptic('light'); }}
                                                    className={`px-4 py-2.5 rounded-xl text-[11px] font-black uppercase tracking-widest whitespace-nowrap transition-all flex-shrink-0 ${
                                                        isActive
                                                            ? 'bg-emerald-500 text-black shadow-[0_0_15px_rgba(16,185,129,0.3)]'
                                                            : 'bg-white/5 border border-white/10 text-white/50 active:bg-white/10'
                                                    }`}
                                                >
                                                    {dev?.deviceName || `Устр. ${c.deviceId}`}
                                                </button>
                                            );
                                        })}
                                    </div>
                                </div>
                            )}

                            <button
                                onClick={handleAutoConnect}
                                disabled={isConnecting || configs.length === 0}
                                className={`w-full py-8 rounded-[2rem] font-black text-[18px] uppercase tracking-[0.1em] flex flex-col items-center justify-center gap-2 transition-all border-2 ${
                                    isConnecting || configs.length === 0
                                        ? 'bg-white/5 border-white/10 text-white/20'
                                        : 'bg-white text-black border-white active:scale-[0.97] shadow-[0_15px_40px_rgba(255,255,255,0.15)]'
                                }`}
                            >
                                <div className="flex items-center gap-3">
                                    {isConnecting ? <Loader2 size={24} className="animate-spin" /> : <Zap size={24} className="fill-current" />}
                                    <span>{isConnecting ? 'Загрузка...' : 'Авто-импорт'}</span>
                                </div>
                                <span className="text-[10px] opacity-50 font-bold uppercase tracking-widest">Запустить в {platform.app}</span>
                            </button>

                            {/*<div className="flex items-center gap-4 my-8">*/}
                            {/*    <div className="h-px bg-white/10 flex-1" />*/}
                            {/*    <span className="text-[10px] font-black text-white/20 uppercase tracking-[0.2em]">Manual</span>*/}
                            {/*    <div className="h-px bg-white/10 flex-1" />*/}
                            {/*</div>*/}

                            {/*<button*/}
                            {/*    onClick={handleCopyLink}*/}
                            {/*    disabled={configs.length === 0}*/}
                            {/*    className="w-full py-4 bg-white/5 border border-white/10 rounded-2xl flex items-center justify-between px-6 active:bg-white/10 transition-all group disabled:opacity-20"*/}
                            {/*>*/}
                            {/*    <div className="flex items-center gap-4">*/}
                            {/*        <div className="w-10 h-10 rounded-xl bg-white/5 flex items-center justify-center border border-white/10 group-active:text-emerald-500 transition-colors">*/}
                            {/*            {copyStatus ? <Check size={20} className="text-emerald-500" /> : <Copy size={20} className="text-white/40" />}*/}
                            {/*        </div>*/}
                            {/*        <div className="text-left">*/}
                            {/*            <p className="text-[13px] font-black text-white">{copyStatus ? 'Скопировано!' : 'Скопировать ссылку'}</p>*/}
                            {/*            <p className="text-[9px] text-white/30 font-bold uppercase mt-0.5">Для ручной вставки</p>*/}
                            {/*        </div>*/}
                            {/*    </div>*/}
                            {/*    <ChevronRight size={18} className="text-white/20" />*/}
                            {/*</button>*/}
                        </div>

                        <div className="bg-[#12141d] border border-white/10 rounded-[2.5rem] p-6 shadow-xl">
                            <div className="flex items-center gap-3 mb-5 border-b border-white/5 pb-4">
                                <div className="w-8 h-8 rounded-xl bg-blue-500/10 flex items-center justify-center border border-blue-500/20">
                                    <HelpCircle size={18} className="text-blue-400" />
                                </div>
                                <h4 className="text-[13px] font-black text-white uppercase tracking-wider">Инструкция</h4>
                            </div>

                            <div className="space-y-4">
                                {[
                                    { s: '01', t: 'Выше выберите нужное устройство (если их несколько)' },
                                    { s: '02', t: 'Нажмите «Авто-импорт»' },
                                    { s: '03', t: `Разрешите открыть приложение ${platform.app}` },
                                    { s: '04', t: 'Профиль добавится автоматически' }
                                ].map((item, idx) => (
                                    <div key={idx} className="flex items-start gap-4">
                                        <span className="text-[11px] font-black text-emerald-500 mt-1 font-mono">{item.s}</span>
                                        <p className="text-[13px] text-white/50 leading-tight font-medium">{item.t}</p>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
}