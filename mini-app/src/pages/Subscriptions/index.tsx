import React, { useState, useMemo } from 'react';
import { useUserStore } from '../../store/userStore';
import {
    Download, Rocket, Smartphone, Laptop, Tv,
    ShieldCheck, Info, Check, ArrowLeft, ArrowRight,
    Loader2, Copy, ExternalLink, HelpCircle, ChevronRight
} from 'lucide-react';

export default function Subscriptions() {
    const { configs } = useUserStore();
    const [step, setStep] = useState(1);
    const [isConnecting, setIsConnecting] = useState(false);
    const [copyStatus, setCopyStatus] = useState(false);

    const platform = useMemo(() => {
        const ua = navigator.userAgent.toLowerCase();
        if (ua.includes('appletv')) return { id: 'tv', name: 'Apple TV', icon: Tv, app: 'Happ TV', link: 'https://apps.apple.com/us/app/happ-proxy-utility-for-tv/id6748297274' };
        if (/iphone|ipad|ipod/.test(ua)) return { id: 'ios', name: 'iPhone / iPad', icon: Smartphone, app: 'Happ Plus', link: 'https://apps.apple.com/ru/app/happ-proxy-utility-plus/id6746188973' };
        if (/android/.test(ua)) return { id: 'android', name: 'Android', icon: Smartphone, app: 'Happ Proxy', link: 'https://play.google.com/store/apps/details?id=com.happproxy' };
        return { id: 'pc', name: 'PC / Windows', icon: Laptop, app: 'Happ Desktop', link: 'https://github.com/Happ-proxy/happ-desktop/releases/latest' };
    }, []);

    const activeConfig = configs[0];

    const handleAutoConnect = () => {
        if (!activeConfig) return;

        setIsConnecting(true);
        window.Telegram?.WebApp?.HapticFeedback.impactOccurred('heavy');

        const redirectUrl = activeConfig.subscriptionUrl.replace('/subscription/', '/import-happ/');

        if (window.Telegram?.WebApp) {
            window.Telegram.WebApp.openLink(redirectUrl);
        } else {
            window.open(redirectUrl, '_blank');
        }

        setTimeout(() => setIsConnecting(false), 2000);
    };

    const handleCopyLink = () => {
        if (!activeConfig) return;

        navigator.clipboard.writeText(activeConfig.subscriptionUrl);
        setCopyStatus(true);

        window.Telegram?.WebApp?.HapticFeedback.notificationOccurred('success');
        setTimeout(() => setCopyStatus(false), 3000);
    };

    return (
        <div className="flex flex-col h-[82vh] overflow-y-auto custom-scrollbar px-1 pb-32 pt-2 animate-in fade-in duration-500">

            {/* НАВИГАЦИЯ ПО ШАГАМ */}
            <div className="flex justify-between items-center mb-6 px-2 shrink-0">
                <button
                    onClick={() => setStep(Math.max(1, step - 1))}
                    className={`flex items-center gap-1 text-[10px] font-black uppercase tracking-widest transition-opacity ${step === 1 ? 'opacity-0 pointer-events-none' : 'text-white/40'}`}
                >
                    <ArrowLeft size={14} /> Назад
                </button>
                <div className="flex items-center gap-2 bg-white/5 px-3 py-1.5 rounded-full border border-white/5">
                    <platform.icon size={12} className="text-primary/70" />
                    <span className="text-[9px] font-black uppercase tracking-widest text-white/60">{platform.name}</span>
                </div>
                <button
                    onClick={() => setStep(Math.min(3, step + 1))}
                    className={`flex items-center gap-1 text-[10px] font-black uppercase tracking-widest transition-opacity ${step === 3 ? 'opacity-0 pointer-events-none' : 'text-white/40'}`}
                >
                    Далее <ArrowRight size={14} />
                </button>
            </div>

            {/* ОСНОВНАЯ КАРТОЧКА */}
            <div className="bg-[#12141d] border border-white/10 rounded-[2.5rem] p-6 mb-4 shadow-xl relative overflow-hidden shrink-0">
                <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 blur-[60px] rounded-full pointer-events-none" />

                {step === 1 && (
                    <div className="text-center animate-in slide-in-from-left-4">
                        <div className="w-16 h-16 bg-white/5 rounded-2xl flex items-center justify-center mx-auto mb-6 border border-white/10">
                            <Download size={32} className="text-white/20" />
                        </div>
                        <h3 className="text-[20px] font-black text-white uppercase italic mb-3">Шаг 1. Клиент</h3>
                        <p className="text-[14px] text-white/50 leading-relaxed mb-8 px-4">
                            Для работы VPN установите приложение <span className="text-white font-bold">{platform.app}</span>.
                        </p>
                        <button
                            onClick={() => window.Telegram?.WebApp?.openLink(platform.link)}
                            className="w-full py-4 bg-white text-black rounded-xl font-black text-[13px] uppercase tracking-widest active:scale-[0.98] transition-all flex items-center justify-center gap-2 shadow-lg"
                        >
                            Установить <ExternalLink size={16} />
                        </button>
                    </div>
                )}

                {step === 2 && (
                    <div className="text-center animate-in slide-in-from-right-4">
                        <div className="w-16 h-16 bg-emerald-500/10 rounded-2xl flex items-center justify-center mx-auto mb-6 border border-emerald-500/20">
                            <ShieldCheck size={32} className="text-emerald-500/50" />
                        </div>
                        <h3 className="text-[20px] font-black text-white uppercase italic mb-3">Шаг 2. Доверие</h3>
                        <p className="text-[14px] text-white/50 leading-relaxed mb-8 px-4">
                            Запустите установленное приложение и разрешите добавление VPN-конфигураций.
                        </p>
                        <button
                            onClick={() => { setStep(3); window.Telegram?.WebApp?.HapticFeedback.impactOccurred('light'); }}
                            className="w-full py-4 bg-white text-black rounded-xl font-black text-[13px] uppercase tracking-widest active:scale-[0.98] transition-all"
                        >
                            Я установил, далее
                        </button>
                    </div>
                )}

                {step === 3 && (
                    <div className="animate-in zoom-in-95">
                        <h3 className="text-[20px] font-black text-white uppercase italic mb-6 text-center">Шаг 3. Подключение</h3>

                        <button
                            onClick={handleAutoConnect}
                            disabled={isConnecting}
                            className={`w-full py-6 rounded-2xl font-black text-[16px] uppercase tracking-widest flex flex-col items-center justify-center gap-1 transition-all border ${
                                isConnecting
                                    ? 'bg-emerald-600 border-emerald-500 text-white'
                                    : 'bg-primary text-white border-primary/20 active:scale-95 shadow-[0_10px_30px_rgba(var(--primary-rgb),0.3)]'
                            }`}
                        >
                            <div className="flex items-center gap-2">
                                {isConnecting ? <Loader2 size={22} className="animate-spin" /> : <Rocket size={22} />}
                                <span>{isConnecting ? 'ОТКРЫВАЕМ...' : 'АВТО-ИМПОРТ'}</span>
                            </div>
                            {!isConnecting && <span className="text-[9px] opacity-40 lowercase">Открыть в {platform.app}</span>}
                        </button>

                        <div className="flex items-center gap-3 my-6">
                            <div className="h-[1px] bg-white/5 flex-1" />
                            <span className="text-[10px] font-black text-white/20 uppercase tracking-widest">или вручную</span>
                            <div className="h-[1px] bg-white/5 flex-1" />
                        </div>

                        <button
                            onClick={handleCopyLink}
                            className="w-full py-4 bg-white/5 border border-white/10 rounded-xl flex items-center justify-between px-5 active:bg-white/10 transition-all group"
                        >
                            <div className="flex items-center gap-3">
                                <Copy size={18} className={copyStatus ? 'text-emerald-500' : 'text-white/40'} />
                                <span className="text-[13px] font-bold text-white/80">
                                    {copyStatus ? 'Ссылка скопирована' : 'Копировать подписку'}
                                </span>
                            </div>
                            {copyStatus ? (
                                <Check size={16} className="text-emerald-500" />
                            ) : (
                                <ChevronRight size={16} className="text-white/20" />
                            )}
                        </button>
                    </div>
                )}
            </div>

            {/* БЛОК ИНСТРУКЦИИ ДЛЯ ШАГА 3 */}
            {step === 3 && (
                <div className="bg-[#12141d] border border-white/10 rounded-[2.5rem] p-6 shrink-0 shadow-lg animate-in slide-in-from-bottom-4">
                    <div className="flex items-center gap-2 mb-5">
                        <div className="w-6 h-6 rounded-full bg-primary/10 flex items-center justify-center border border-primary/20">
                            <HelpCircle size={14} className="text-primary" />
                        </div>
                        <h4 className="text-[11px] font-black text-white uppercase tracking-[0.1em]">Инструкция</h4>
                    </div>

                    <div className="space-y-4">
                        {[
                            { s: '1', t: 'Нажмите «Копировать подписку» выше.' },
                            { s: '2', t: `Откройте приложение ${platform.app}.` },
                            { s: '3', t: 'Нажмите «Add Subscription» или иконку «+».' },
                            { s: '4', t: 'Вставьте скопированную ссылку и сохраните.' }
                        ].map((item, idx) => (
                            <div key={idx} className="flex items-start gap-4">
                                <div className="w-5 h-5 rounded-full bg-white/5 border border-white/10 flex items-center justify-center text-[10px] font-black text-white/40 shrink-0 mt-0.5">
                                    {item.s}
                                </div>
                                <p className="text-[13px] text-white/60 leading-tight">{item.t}</p>
                            </div>
                        ))}
                    </div>

                    <div className="mt-6 pt-5 border-t border-white/5 flex items-center gap-3">
                        <Info size={14} className="text-white/20" />
                        <p className="text-[11px] text-white/30 italic">
                            Все ваши локации подтянутся автоматически одной ссылкой.
                        </p>
                    </div>
                </div>
            )}

            {/* ИНДИКАТОРЫ ШАГОВ */}
            <div className="mt-8 flex justify-center items-center gap-2 shrink-0">
                {[1, 2, 3].map((i) => (
                    <div
                        key={i}
                        className={`h-1.5 rounded-full transition-all duration-500 ${step === i ? 'w-10 bg-primary' : 'w-2 bg-white/10'}`}
                    />
                ))}
            </div>
        </div>
    );
}