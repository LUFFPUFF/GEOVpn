import React, { useState, useMemo, useRef, useCallback } from 'react';
import { useUserStore } from '../../store/userStore';
import { Crown, Zap, MonitorSmartphone, Globe2, Plus, ChevronRight, ArrowRight } from 'lucide-react';

export default function Home() {
    const { user, deviceLimit, devices, setActiveTab, t } = useUserStore();
    const [activeSlide, setActiveSlide] = useState(0);
    const touchStartX = useRef<number | null>(null);
    const touchStartY = useRef<number | null>(null);
    const isDragging  = useRef(false);

    const hasSub      = user?.hasActiveSubscription || false;
    const totalSlides = hasSub ? 2 : 1;

    const { daysLeft, expireDateFormatted } = useMemo(() => {
        if (!user?.subscriptionExpiresAt) return { daysLeft: 0, expireDateFormatted: '---' };
        const expire = new Date(user.subscriptionExpiresAt);
        const diff   = Math.ceil((expire.getTime() - Date.now()) / 86_400_000);
        return {
            daysLeft: diff > 0 ? diff : 0,
            expireDateFormatted: expire.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short', year: 'numeric' })
        };
    }, [user]);

    const activeDevs = deviceLimit?.activeDevices ?? devices.length;
    const maxDevs    = deviceLimit?.maxDevices    ?? 3;

    const stats = [
        { icon: Zap,               val: '∞',                       label: t.traffic   },
        { icon: MonitorSmartphone, val: `${activeDevs}/${maxDevs}`, label: t.devices   },
        { icon: Globe2,            val: t.all_locations,            label: t.locations },
    ];

    const haptic = (s: 'light' | 'medium' = 'light') =>
        window.Telegram?.WebApp?.HapticFeedback.impactOccurred(s);

    const goNext = useCallback(() => {
        if (activeSlide < totalSlides - 1) { setActiveSlide(p => p + 1); haptic(); }
    }, [activeSlide, totalSlides]);

    const goPrev = useCallback(() => {
        if (activeSlide > 0) { setActiveSlide(p => p - 1); haptic(); }
    }, [activeSlide]);

    const onTouchStart = (e: React.TouchEvent) => {
        touchStartX.current = e.touches[0].clientX;
        touchStartY.current = e.touches[0].clientY;
        isDragging.current  = false;
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
        isDragging.current  = false;
    };

    return (
        <div className="flex flex-col pt-2">
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
                    {/* Слайд 1: активная подписка */}
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
                                        <button onClick={() => { setActiveTab('payments'); haptic(); }}
                                                className="flex-1 py-3.5 bg-white/5 border border-white/10 rounded-2xl font-black text-[11px] text-white/80 uppercase tracking-widest active:scale-[0.98] transition-all tap-target">
                                            {t.renew}
                                        </button>
                                        <button onClick={() => { setActiveTab('profile'); haptic(); }}
                                                className="flex-1 py-3.5 bg-white/5 border border-white/10 rounded-2xl font-black text-[11px] text-white/80 uppercase tracking-widest active:scale-[0.98] transition-all tap-target">
                                            {t.profile}
                                        </button>
                                    </div>
                                    <button onClick={() => { setActiveTab('subscriptions'); haptic('medium'); }}
                                            className="w-full py-4 bg-white text-black rounded-2xl font-black text-[13px] uppercase tracking-[0.1em] active:scale-[0.98] transition-all flex items-center justify-center gap-2 tap-target">
                                        <ArrowRight size={18} className="animate-pulse" />
                                        {t.setup_subscription}
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Слайд 2: нет подписки */}
                    <div className="flex flex-col px-2 pb-4" style={{ width: `${100 / totalSlides}%` }}>
                        <div className="flex flex-col justify-center items-center text-center relative overflow-hidden rounded-[2.5rem] p-8 border border-white/5 bg-[#0a0a0f] shadow-2xl min-h-[65vh]">
                            <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-full h-full bg-primary/5 blur-[100px] pointer-events-none rounded-full" />
                            <div className="w-full relative z-10 mb-8 mt-6">
                                <div onClick={() => { setActiveTab('payments'); haptic(); }}
                                     className="w-20 h-20 bg-white text-black rounded-full flex items-center justify-center mx-auto mb-8 shadow-[0_0_30px_rgba(255,255,255,0.15)] active:scale-90 transition-transform cursor-pointer tap-target">
                                    <Plus size={40} strokeWidth={2.5} />
                                </div>
                                <h2 className="text-[32px] font-black leading-none mb-4 tracking-tighter uppercase italic text-white">
                                    {t.add_subscription}
                                </h2>
                                <p className="text-white/40 text-[14px] font-medium leading-relaxed max-w-[260px] mx-auto">
                                    {t.add_subscription_desc}
                                </p>
                            </div>
                            <button onClick={() => { setActiveTab('payments'); haptic('medium'); }}
                                    className="w-full py-5 bg-white text-black rounded-2xl font-black text-[13px] uppercase tracking-[0.1em] active:scale-[0.98] transition-all flex items-center justify-center gap-2 relative z-10 mt-auto tap-target">
                                <span>{t.get_access}</span>
                                <ChevronRight size={20} />
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            {totalSlides > 1 && (
                <div className="flex justify-center gap-2 py-4">
                    {Array.from({ length: totalSlides }).map((_, i) => (
                        <div key={i} onClick={() => { setActiveSlide(i); haptic(); }}
                             className={`h-1.5 rounded-full transition-all duration-500 cursor-pointer ${activeSlide === i ? 'w-10 bg-white' : 'w-2 bg-white/20'}`} />
                    ))}
                </div>
            )}
        </div>
    );
}
