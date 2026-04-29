import React, { useState, useMemo, useRef } from 'react';
import { useUserStore } from '../../store/userStore';
import { Crown, Zap, MonitorSmartphone, Globe2, Plus, ChevronRight, ArrowRight } from 'lucide-react';

export default function Home() {
    const { user, deviceLimit, devices, setActiveTab, t } = useUserStore();
    const [activeSlide, setActiveSlide] = useState(0);
    const pointerStart = useRef<number | null>(null);

    const hasSub = user?.hasActiveSubscription || false;
    const totalSlides = hasSub ? 2 : 1;

    const { daysLeft, expireDateFormatted } = useMemo(() => {
        if (!user?.subscriptionExpiresAt) return { daysLeft: 0, expireDateFormatted: '---' };
        const expire = new Date(user.subscriptionExpiresAt);
        const diff = Math.ceil((expire.getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24));
        return {
            daysLeft: diff > 0 ? diff : 0,
            expireDateFormatted: expire.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short', year: 'numeric' })
        };
    }, [user]);

    const activeDevs = deviceLimit?.activeDevices ?? devices.length;
    const maxDevs = deviceLimit?.maxDevices ?? 5;

    const stats = [
        { icon: Zap,               val: "∞",                  label: t.traffic },
        { icon: MonitorSmartphone, val: `${activeDevs}/${maxDevs}`, label: t.devices },
        { icon: Globe2,            val: t.all_locations,       label: t.locations },
    ];

    const handlePointerDown = (e: React.PointerEvent<HTMLDivElement>) => { pointerStart.current = e.clientX; };
    const handlePointerUp = (e: React.PointerEvent<HTMLDivElement>) => {
        if (pointerStart.current === null) return;
        const distance = pointerStart.current - e.clientX;
        if (distance > 50 && activeSlide < totalSlides - 1) {
            setActiveSlide(prev => prev + 1);
            window.Telegram?.WebApp?.HapticFeedback.impactOccurred('light');
        } else if (distance < -50 && activeSlide > 0) {
            setActiveSlide(prev => prev - 1);
            window.Telegram?.WebApp?.HapticFeedback.impactOccurred('light');
        }
        pointerStart.current = null;
    };

    return (
        <div className="flex flex-col h-[82vh] max-h-[85vh] relative pt-2 pb-6 select-none overflow-x-hidden overflow-y-auto custom-scrollbar"
             style={{ touchAction: 'pan-y' }}
             onPointerDown={handlePointerDown}
             onPointerUp={handlePointerUp}>

            <div className="flex transition-transform duration-500 [transition-timing-function:cubic-bezier(0.25,1,0.5,1)] min-h-max"
                 style={{ transform: `translateX(-${activeSlide * 100}%)` }}>

                {hasSub && (
                    <div className="w-full flex-shrink-0 px-2 flex flex-col">
                        <div className="flex flex-col relative overflow-hidden rounded-[2.5rem] p-7 border border-white/10 bg-gradient-to-b from-[#12141d] to-[#0a0a0f] shadow-[0_20px_40px_rgba(0,0,0,0.8)] h-max min-h-full">
                            <div className="absolute top-[-20%] left-[-10%] w-[150%] h-[50%] bg-amber-500/10 blur-[80px] rounded-full pointer-events-none" />

                            <div className="flex justify-between items-start mb-6 relative z-10">
                                <div className="flex items-center gap-4">
                                    <div className="w-14 h-14 bg-gradient-to-br from-amber-400/20 to-amber-600/5 rounded-[1.2rem] flex items-center justify-center border border-amber-500/30 shadow-[0_0_15px_rgba(251,191,36,0.15)]">
                                        <Crown size={28} className="text-amber-400 drop-shadow-[0_0_8px_rgba(251,191,36,0.5)]" />
                                    </div>
                                    <div>
                                        <h2 className="text-[22px] font-black uppercase tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-white to-white/70 leading-none mb-1.5">
                                            {user?.subscriptionType || 'UNLIMITED'}
                                        </h2>
                                        <div className="flex items-center gap-1.5">
                                            <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse shadow-[0_0_8px_#10b981]" />
                                            <p className="text-emerald-500 text-[10px] font-black uppercase tracking-[0.2em]">{t.active}</p>
                                        </div>
                                    </div>
                                </div>
                                <div className="text-right mt-1 shrink-0">
                                    <p className="text-white/40 text-[9px] font-black uppercase tracking-[0.2em] mb-1.5">{t.until_date}</p>
                                    <p className="font-bold text-[13px] text-white/90 bg-white/5 px-2.5 py-1 rounded-md border border-white/5 whitespace-nowrap">{expireDateFormatted}</p>
                                </div>
                            </div>

                            <div className="grid grid-cols-3 gap-3 mb-4 relative z-10">
                                {stats.map((item, i) => (
                                    <div key={i} className="bg-black/40 backdrop-blur-md rounded-2xl border border-white/5 flex flex-col items-center justify-center py-5 shadow-inner">
                                        <item.icon size={20} className="mb-2 text-white/40"/>
                                        <span className="text-[17px] font-black text-white tracking-tight">{item.val}</span>
                                        <span className="text-[9px] font-black uppercase tracking-widest text-white/30 mt-1 text-center">{item.label}</span>
                                    </div>
                                ))}
                            </div>

                            <div className="flex items-center gap-4 mb-6 bg-black/40 p-3 rounded-2xl border border-white/5 relative z-10">
                                <div className="flex-1 flex items-center gap-3">
                                    <div className="w-8 h-8 rounded-full bg-emerald-500/10 flex items-center justify-center border border-emerald-500/20 relative">
                                        <div className="absolute inset-0 rounded-full border border-emerald-500/30 animate-ping opacity-50"></div>
                                        <Zap size={14} className="text-emerald-400" />
                                    </div>
                                    <div>
                                        <p className="text-[9px] text-white/40 font-black uppercase tracking-widest">Ping</p>
                                        <p className="text-[14px] font-black text-white font-mono">12 <span className="text-[10px] text-white/50">ms</span></p>
                                    </div>
                                </div>
                                <div className="w-[1px] h-8 bg-white/10"></div>
                                <div className="flex-1 flex items-center gap-3 pl-2">
                                    <div className="w-8 h-8 rounded-full bg-blue-500/10 flex items-center justify-center border border-blue-500/20">
                                        <Globe2 size={14} className="text-blue-400" />
                                    </div>
                                    <div>
                                        <p className="text-[9px] text-white/40 font-black uppercase tracking-widest">{t.channel}</p>
                                        <p className="text-[14px] font-black text-white font-mono">1 <span className="text-[10px] text-white/50">Gbit/s</span></p>
                                    </div>
                                </div>
                            </div>

                            <div className="bg-black/40 backdrop-blur-xl rounded-[2rem] p-5 border border-white/10 mt-auto text-center relative overflow-hidden group shadow-[inset_0_1px_1px_rgba(255,255,255,0.05)] z-10">
                                <p className="text-white/40 text-[11px] font-black uppercase tracking-[0.3em] mb-1">{t.remains}</p>
                                <div className="flex items-baseline justify-center gap-2">
                                    <h3 className="text-[48px] font-black tracking-tighter text-white leading-none drop-shadow-lg">
                                        {daysLeft}
                                    </h3>
                                    <span className="text-[16px] text-white/50 font-bold uppercase tracking-widest">{t.days}</span>
                                </div>
                            </div>

                            <div className="space-y-3 relative z-10 mt-5">
                                <div className="flex gap-3">
                                    <button onClick={() => { setActiveTab('payments'); window.Telegram?.WebApp?.HapticFeedback.impactOccurred('light'); }} className="flex-1 py-3.5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl font-black text-[12px] text-white/80 uppercase tracking-widest active:scale-[0.98] transition-all">{t.renew}</button>
                                    <button onClick={() => { setActiveTab('profile'); window.Telegram?.WebApp?.HapticFeedback.impactOccurred('light'); }} className="flex-1 py-3.5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl font-black text-[12px] text-white/80 uppercase tracking-widest active:scale-[0.98] transition-all">{t.profile}</button>
                                </div>
                                <button onClick={() => { setActiveTab('subscriptions'); window.Telegram?.WebApp?.HapticFeedback.impactOccurred('medium'); }} className="w-full py-4 bg-white text-black rounded-2xl font-black text-[14px] uppercase tracking-[0.1em] active:scale-[0.98] transition-all flex items-center justify-center gap-3 shadow-[0_0_20px_rgba(255,255,255,0.2)]">
                                    <ArrowRight size={20} className="animate-pulse" />
                                    {t.setup_subscription}
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                <div className="w-full flex-shrink-0 px-2 flex flex-col h-full">
                    <div className="flex flex-col justify-center items-center text-center relative overflow-hidden rounded-[2.5rem] p-8 border border-white/5 bg-[#0a0a0f] shadow-2xl h-max min-h-full">
                        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-full h-full bg-primary/5 blur-[100px] pointer-events-none rounded-full" />

                        <div className="mt-10 w-full relative z-10 mb-10">
                            <div onClick={() => { setActiveTab('payments'); window.Telegram?.WebApp?.HapticFeedback.impactOccurred('light'); }} className="w-24 h-24 bg-white text-black rounded-full flex items-center justify-center mx-auto mb-10 shadow-[0_0_30px_rgba(255,255,255,0.15)] active:scale-90 transition-transform cursor-pointer">
                                <Plus size={44} strokeWidth={2.5} />
                            </div>
                            <h2 className="text-[38px] font-black leading-[0.9] mb-5 tracking-tighter uppercase italic text-white">
                                {t.add_subscription}
                            </h2>
                            <p className="text-white/40 text-[15px] font-medium leading-relaxed max-w-[260px] mx-auto">
                                {t.add_subscription_desc}
                            </p>
                        </div>

                        <button onClick={() => { setActiveTab('payments'); window.Telegram?.WebApp?.HapticFeedback.impactOccurred('medium'); }} className="w-full py-5 bg-white text-black rounded-2xl font-black text-[14px] uppercase tracking-[0.1em] active:scale-[0.98] transition-all flex items-center justify-center gap-3 relative z-10 mt-auto">
                            <span>{t.get_access}</span>
                            <ChevronRight size={22} />
                        </button>
                    </div>
                </div>
            </div>

            {totalSlides > 1 && (
                <div className="flex justify-center gap-2 mt-6 pb-2 shrink-0">
                    <div onClick={() => { setActiveSlide(0); window.Telegram?.WebApp?.HapticFeedback.impactOccurred('light'); }} className={`h-1.5 rounded-full transition-all duration-500 cursor-pointer ${activeSlide === 0 ? 'w-10 bg-white' : 'w-2 bg-white/20 hover:bg-white/40'}`} />
                    <div onClick={() => { setActiveSlide(1); window.Telegram?.WebApp?.HapticFeedback.impactOccurred('light'); }} className={`h-1.5 rounded-full transition-all duration-500 cursor-pointer ${activeSlide === 1 ? 'w-10 bg-white' : 'w-2 bg-white/20 hover:bg-white/40'}`} />
                </div>
            )}
        </div>
    );
}
