import React, { useState, useEffect, useMemo } from 'react';
import { useUserStore } from '../../store/userStore';
import {
    Download, Rocket, Smartphone, Laptop, Tv,
    ShieldCheck, Info, Check, ArrowLeft, ArrowRight
} from 'lucide-react';

export default function Subscriptions() {
    const { configs, devices, createConfig } = useUserStore();

    const [step, setStep] = useState(1);
    const [copyStatus, setCopyStatus] = useState('idle');

    const platform = useMemo(() => {
        const ua = navigator.userAgent.toLowerCase();
        if (ua.includes('appletv')) return {
            id: 'tv', name: 'Apple TV', icon: Tv, app: 'Happ TV',
            link: 'https://apps.apple.com/us/app/happ-proxy-utility-for-tv/id6748297274',
            isStore: true
        };
        if (/iphone|ipad|ipod/.test(ua)) return {
            id: 'ios', name: 'iPhone / iPad', icon: Smartphone, app: 'Happ Plus',
            link: 'https://apps.apple.com/ru/app/happ-proxy-utility-plus/id6746188973',
            isStore: true
        };
        if (/android/.test(ua)) return {
            id: 'android', name: 'Android', icon: Smartphone, app: 'Happ Proxy',
            link: 'https://play.google.com/store/apps/details?id=com.happproxy',
            isStore: true
        };
        return {
            id: 'pc', name: 'PC / Windows', icon: Laptop, app: 'Happ Desktop',
            link: 'https://github.com/Happ-proxy/happ-desktop/releases/latest/download/setup-Happ.x64.exe',
            isStore: false
        };
    }, []);

    const ensureConfig = async () => {
        if (configs.length === 0) {
            const deviceId = devices[0]?.id || 0;
            await createConfig(deviceId, "NL");
        }
    };

    const handleAutoConnect = async () => {
        await ensureConfig();
        const vlessLink = configs[0]?.vlessLink;
        if (!vlessLink) return;

        try {
            await navigator.clipboard.writeText(vlessLink);
            setCopyStatus('copied');

            if (window.Telegram?.WebApp?.openLink) {
                window.Telegram.WebApp.openLink(vlessLink);
            } else {
                window.location.href = vlessLink;
            }

            window.Telegram?.WebApp?.HapticFeedback.notificationOccurred('success');
            setTimeout(() => setCopyStatus('idle'), 4000);
        } catch (err) {
            console.error(err);
        }
    };

    const handleDownloadAction = () => {
        if (platform.isStore) {
            window.Telegram?.WebApp?.openLink(platform.link);
        } else {
            const link = document.createElement('a');
            link.href = platform.link;
            link.setAttribute('download', 'Happ_Setup.exe');
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        }
        setStep(2);
    };

    return (
        <div className="px-1 pb-24 animate-in fade-in duration-500">
            <div className="grid grid-cols-3 items-center mb-6 mt-2 px-2">
                <div className="flex justify-start">
                    {step > 1 && (
                        <button
                            onClick={() => setStep(step - 1)}
                            className="flex items-center gap-1.5 text-muted-foreground hover:text-white transition-colors font-black text-[10px] uppercase tracking-widest"
                        >
                            <ArrowLeft size={14} /> НАЗАД
                        </button>
                    )}
                </div>

                <div className="flex justify-center">
                    <div className="flex items-center gap-2 bg-white/5 px-3 py-1.5 rounded-full border border-white/5">
                        <platform.icon size={13} className="text-primary/70" />
                        <span className="text-[9px] font-black uppercase tracking-widest opacity-60 text-white whitespace-nowrap">
                            {platform.name}
                        </span>
                    </div>
                </div>

                {/* ПРАВАЯ ЧАСТЬ: ВПЕРЕД */}
                <div className="flex justify-end">
                    {step < 3 && (
                        <button
                            onClick={() => setStep(step + 1)}
                            className="flex items-center gap-1.5 text-muted-foreground hover:text-white transition-colors font-black text-[10px] uppercase tracking-widest"
                        >
                            ВПЕРЕД <ArrowRight size={14} />
                        </button>
                    )}
                </div>
            </div>

            <div className="text-center mb-8">
                <h2 className="text-[32px] font-black uppercase italic tracking-tighter leading-none mb-2">
                    {step === 1 && "ШАГ 1: КЛИЕНТ"}
                    {step === 2 && "ШАГ 2: МОНТАЖ"}
                    {step === 3 && "ШАГ 3: ФИНАЛ"}
                </h2>
                <p className="text-muted-foreground text-[11px] font-bold uppercase tracking-[0.2em]">
                    {step === 1 && `Загрузка ${platform.app}`}
                    {step === 2 && "Подготовка приложения к работе"}
                    {step === 3 && "Автоматическая активация доступа"}
                </p>
            </div>

            <div className="glass-card border-white/5 bg-[#08080a] p-8 relative shadow-2xl">

                {step === 1 && (
                    <div className="space-y-6 animate-in slide-in-from-left-4">
                        <div className="p-6 bg-white/5 rounded-2xl border border-white/5 text-center">
                            <Download size={40} className="mx-auto mb-4 text-primary" />
                            <p className="text-sm text-foreground mb-6 leading-relaxed">
                                Для работы VPN установите официальное приложение <b>{platform.app}</b> на ваше устройство.
                            </p>
                            <button onClick={handleDownloadAction} className="btn-primary !py-5 shadow-primary/20">
                                {platform.isStore ? "ОТКРЫТЬ STORE" : "СКАЧАТЬ HAPP.EXE"}
                            </button>
                        </div>
                    </div>
                )}

                {step === 2 && (
                    <div className="space-y-6 animate-in slide-in-from-right-4">
                        <div className="p-6 bg-white/5 rounded-2xl border border-white/5 text-center">
                            <ShieldCheck size={40} className="mx-auto mb-4 text-emerald-500" />
                            <p className="text-sm text-foreground mb-6 leading-relaxed">
                                Завершите установку приложения. После этого можно активировать защиту.
                            </p>
                            <button onClick={() => setStep(3)} className="btn-primary !py-5 flex items-center justify-center gap-3">
                                ГОТОВО, ДАЛЕЕ <ArrowRight size={20} />
                            </button>
                        </div>
                    </div>
                )}

                {step === 3 && (
                    <div className="space-y-6 animate-in zoom-in-95">
                        <div className="text-center mb-2">
                            <span className="bg-emerald-500/10 text-emerald-500 px-3 py-1 rounded-full text-[10px] font-black uppercase border border-emerald-500/20 tracking-tighter">
                                Пакет данных сформирован
                            </span>
                        </div>

                        <button
                            onClick={handleAutoConnect}
                            className={`w-full py-8 rounded-[24px] font-black text-xl uppercase tracking-tight flex flex-col items-center justify-center gap-1 transition-all duration-500 ${
                                copyStatus === 'copied' ? 'bg-emerald-500 text-white' : 'bg-white text-black'
                            }`}
                        >
                            <div className="flex items-center gap-3">
                                {copyStatus === 'copied' ? <Check size={32} /> : <Rocket size={32} className="animate-bounce" />}
                                <span>{copyStatus === 'copied' ? 'УСПЕШНО' : 'ПОДКЛЮЧИТЬ'}</span>
                            </div>
                            <span className="text-[10px] opacity-60 font-bold normal-case tracking-normal text-center">
                                {copyStatus === 'copied' ? 'Данные переданы в Happ' : 'Автоматический запуск и вставка'}
                            </span>
                        </button>

                        <div className="p-4 rounded-xl bg-white/5 border border-white/5 flex gap-3">
                            <Info size={18} className="text-primary shrink-0 mt-0.5" />
                            <div className="space-y-1 text-left">
                                <p className="text-[11px] leading-snug text-white font-bold uppercase tracking-tight">Как это работает?</p>
                                <p className="text-[11px] leading-snug text-muted-foreground">
                                    Мы автоматически откроем Happ и импортируем настройки. Вам останется только нажать «Старт» в приложении.
                                </p>
                            </div>
                        </div>
                    </div>
                )}
            </div>

            {/* ИНДИКАТОРЫ ТОЧЕК */}
            <div className="mt-10 flex justify-center items-center gap-3">
                {[1, 2, 3].map((i) => (
                    <button
                        key={i}
                        onClick={() => setStep(i)}
                        className={`h-1.5 rounded-full transition-all duration-500 ${
                            step === i ? 'w-12 bg-primary' : 'w-6 bg-white/10 hover:bg-white/30'
                        }`}
                    />
                ))}
            </div>
        </div>
    );
}