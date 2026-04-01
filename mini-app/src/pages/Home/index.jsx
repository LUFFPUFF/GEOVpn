import React, { useState, useMemo, useRef } from 'react';
import { useUserStore } from '../../store/userStore';
import { Crown, Zap, MonitorSmartphone, Globe2, Plus, ChevronRight, ArrowRight } from 'lucide-react';

export default function Home() {
    const { user, configs, devices, setActiveTab, t } = useUserStore();
    const [activeSlide, setActiveSlide] = useState(0);
    const pointerStart = useRef(null);

    const hasSub = user?.hasActiveSubscription || false;
    const totalSlides = hasSub ? 2 : 1;

    const { daysLeft, expireDateFormatted } = useMemo(() => {
        if (!user?.subscriptionExpiresAt) return { daysLeft: 0, expireDateFormatted: '---' };
        const expire = new Date(user.subscriptionExpiresAt);
        const diff = Math.ceil((expire - new Date()) / (1000 * 60 * 60 * 24));
        return {
            daysLeft: diff > 0 ? diff : 0,
            expireDateFormatted: expire.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short', year: 'numeric' })
        };
    }, [user]);

    const handlePointerDown = (e) => { pointerStart.current = e.clientX; };
    const handlePointerUp = (e) => {
        if (pointerStart.current === null) return;
        const distance = pointerStart.current - e.clientX;
        const threshold = 50;
        if (distance > threshold && activeSlide < totalSlides - 1) setActiveSlide(prev => prev + 1);
        else if (distance < -threshold && activeSlide > 0) setActiveSlide(prev => prev - 1);
        pointerStart.current = null;
    };

    return (
        <div
            className="flex flex-col h-[75vh] relative pt-4 select-none"
            style={{ touchAction: 'pan-y' }}
            onPointerDown={handlePointerDown}
            onPointerUp={handlePointerUp}
        >
            <div
                className="flex transition-transform duration-500 [transition-timing-function:cubic-bezier(0.25,1,0.5,1)] h-full"
                style={{ transform: `translateX(-${activeSlide * 100}%)` }}
            >
                {hasSub && (
                    <div className="w-full flex-shrink-0 px-2 h-full">
                        <div className="glass-card h-full flex flex-col border-border relative overflow-hidden bg-gradient-to-br from-card to-secondary/20">

                            <div className="flex justify-between items-start mb-8">
                                <div className="flex items-center gap-3">
                                    <div className="w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center border border-primary/20 shadow-lg shadow-primary/5">
                                        <Crown size={28} className="text-amber-400" />
                                    </div>
                                    <div>
                                        <h2 className="text-[20px] font-black uppercase tracking-tight text-foreground leading-none">
                                            {user?.subscriptionType || 'PREMIUM'}
                                        </h2>
                                        <p className="text-emerald-500 text-[11px] font-bold uppercase tracking-widest mt-1">активна</p>
                                    </div>
                                </div>
                                <div className="text-right">
                                    <p className="text-muted-foreground text-[10px] font-bold uppercase tracking-widest leading-none mb-1">до даты</p>
                                    <p className="font-bold text-[13px] text-foreground opacity-80">{expireDateFormatted}</p>
                                </div>
                            </div>

                            <div className="grid grid-cols-3 gap-3 mb-8">
                                {[
                                    { icon: Zap, val: "--- ГБ", label: "трафик" },
                                    { icon: MonitorSmartphone, val: `${devices.length}/5`, label: "устройства" },
                                    { icon: Globe2, val: configs.length, label: "ноды" }
                                ].map((item, i) => (
                                    <div key={i} className="bg-muted/30 p-4 rounded-2xl border border-border flex flex-col items-center">
                                        <item.icon size={18} className="mb-2 text-muted-foreground/60"/>
                                        <span className="text-[14px] font-black text-foreground">{item.val}</span>
                                    </div>
                                ))}
                            </div>

                            <div className="bg-primary/5 rounded-[2rem] p-7 border border-primary/10 mb-6 text-center relative overflow-hidden group">
                                <p className="text-muted-foreground text-[12px] font-bold uppercase tracking-[0.2em] mb-1">До окончания доступа</p>
                                <h3 className="text-[42px] font-black tracking-tighter text-foreground leading-none">
                                    {daysLeft} <span className="text-[16px] text-muted-foreground uppercase ml-1">{t.days}</span>
                                </h3>
                                {/* Декоративное свечение */}
                                <div className="absolute inset-0 bg-primary/5 opacity-0 group-hover:opacity-100 transition-opacity duration-700 pointer-events-none" />
                            </div>

                            <div className="mt-auto space-y-3">
                                <div className="flex gap-3">
                                    <button onClick={() => setActiveTab('payments')} className="flex-1 py-4 bg-secondary border border-border rounded-xl font-bold text-[13px] text-foreground uppercase tracking-wider active:scale-95 transition-all">Продлить</button>
                                    <button onClick={() => setActiveTab('profile')} className="flex-1 py-4 bg-secondary border border-border rounded-xl font-bold text-[13px] text-foreground uppercase tracking-wider active:scale-95 transition-all">Профиль</button>
                                </div>

                                <button
                                    onClick={() => setActiveTab('subscriptions')}
                                    className="btn-primary flex items-center justify-center gap-3 !py-5 text-[14px]"
                                >
                                    <ArrowRight size={20} className="animate-pulse" />
                                    УСТАНОВИТЬ ПОДПИСКУ
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                <div className="w-full flex-shrink-0 px-2 h-full">
                    <div className="glass-card h-full flex flex-col justify-between items-center text-center border-border relative overflow-hidden p-8 bg-card">
                        <div className="mt-12 w-full">
                            <div
                                onClick={() => setActiveTab('payments')}
                                className="w-24 h-24 bg-primary text-primary-foreground rounded-full flex items-center justify-center mx-auto mb-10 shadow-2xl shadow-primary/20 active:scale-90 transition-transform cursor-pointer"
                            >
                                <Plus size={48} strokeWidth={2.5} />
                            </div>
                            <h2 className="text-[36px] font-black leading-[0.9] mb-4 tracking-tighter uppercase italic text-foreground">
                                Добавить<br/>подписку
                            </h2>
                            <p className="text-muted-foreground text-[16px] font-medium leading-tight px-4 max-w-[280px] mx-auto opacity-60">
                                Получите доступ к премиальным локациям и высокой скорости
                            </p>
                        </div>

                        <button
                            onClick={() => setActiveTab('payments')}
                            className="btn-primary !py-5 flex items-center justify-center gap-3 shadow-xl shadow-primary/10"
                        >
                            <span className="mt-0.5">ОФОРМИТЬ ДОСТУП</span>
                            <ChevronRight size={22} />
                        </button>
                    </div>
                </div>
            </div>
            {totalSlides > 1 && (
                <div className="flex justify-center gap-2 mt-8 pb-4">
                    <div
                        onClick={() => setActiveSlide(0)}
                        className={`h-1.5 rounded-full transition-all duration-300 cursor-pointer ${activeSlide === 0 ? 'w-10 bg-primary' : 'w-2 bg-muted'}`}
                    />
                    <div
                        onClick={() => setActiveSlide(1)}
                        className={`h-1.5 rounded-full transition-all duration-300 cursor-pointer ${activeSlide === 1 ? 'w-10 bg-primary' : 'w-2 bg-muted'}`}
                    />
                </div>
            )}
        </div>
    );
}