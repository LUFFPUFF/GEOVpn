import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useUserStore } from '../../store/userStore';
import {
    Globe2, Smartphone, Users,
    Gift, Sparkles, CheckCircle2, Shield, Wifi,
    ChevronDown, Star, LucideIcon
} from 'lucide-react';

import promoImage from '../../assets/img.png';

declare global {
    interface Window { Telegram?: { WebApp: any } }
}

type ImpactStyle = 'light' | 'medium' | 'heavy' | 'rigid' | 'soft';

interface Tariff {
    id: string;
    name: string;
    devicesLabel: string;
    price: number;
    oldPrice?: number;
    icon: LucideIcon | React.ElementType;
    color: string;
    bgColor: string;
    borderColor: string;
    description: string;
    features: string[];
    isHit?: boolean;
    isPromo?: boolean;
}

const IS_PROMO_ACTIVE = true;

export default function Payments() {
    const { user, t, purchaseSubscription, setActiveTab, lang } = useUserStore();
    const [expandedId, setExpandedId] = useState<string | null>(null);
    const [alreadyUsedPromo, setAlreadyUsedPromo] = useState(false);
    const [payingId, setPayingId] = useState<string | null>(null);

    const isRTL = lang === 'ar' || lang === 'fa';
    const realBalanceValue = user?.balance || 0;
    const realBalanceDisplay = (realBalanceValue / 100).toFixed(0);

    const isPayg = !user?.subscriptionType || String(user.subscriptionType).toUpperCase() === 'PAYG';
    const hasActiveSub = user?.hasActiveSubscription ?? false;

    const promoAvailable = IS_PROMO_ACTIVE && isPayg && !alreadyUsedPromo;

    useEffect(() => {
        const tg = window.Telegram?.WebApp;
        if (tg) { tg.ready(); tg.expand(); }
    }, []);

    const haptic = (type: ImpactStyle = 'medium') =>
        window.Telegram?.WebApp?.HapticFeedback?.impactOccurred(type);

    const handleToggle = (id: string) => {
        haptic('light');
        setExpandedId(prev => prev === id ? null : id);
    };

    const handlePay = async (tariff: Tariff) => {
        if (payingId) return;

        const priceInKopecks = tariff.price * 100;
        if (!tariff.isPromo && realBalanceValue < priceInKopecks) {
            haptic('medium');
            window.Telegram?.WebApp?.showConfirm(
                `На балансе недостаточно средств (${realBalanceDisplay} ₽). Перейти к пополнению?`,
                (ok: boolean) => {
                    if (ok) setActiveTab('deposit');
                }
            );
            return;
        }

        haptic('heavy');
        setPayingId(tariff.id);
        try {
            const success = await purchaseSubscription(tariff.id, 1, tariff.isPromo === true);
            setExpandedId(null);
            if (success) {
                window.Telegram?.WebApp?.showConfirm('Подписка успешно оформлена!', (ok: boolean) => {
                    if (ok) setActiveTab('home');
                });
            }
        } catch (error: any) {
            setExpandedId(null);
            const serverMsg = error?.response?.data?.error?.message;
            window.Telegram?.WebApp?.showAlert(`Ошибка: ${serverMsg || error.message}`);
        } finally {
            setPayingId(null);
        }
    };

    const promoTariff: Tariff = {
        id: 'BASIC', name: 'Стандарт', devicesLabel: '1 устройство',
        price: 0, oldPrice: 100, icon: Gift,
        color: 'text-white', bgColor: 'bg-white/10', borderColor: 'border-white/20',
        description: 'Полный доступ к 3 локациям GeoVPN на 30 дней — абсолютно бесплатно.',
        features: ['3 сервера', 'AES-256', 'Безлимит трафика'], isPromo: true,
    };

    const tariffs: Tariff[] = [
        {
            id: 'BASIC', name: 'Стандарт', devicesLabel: '1 устройство', price: 100,
            icon: Smartphone, color: 'text-gray-300', bgColor: 'bg-gray-500/10', borderColor: 'border-white/10',
            description: 'Базовый доступ к 3 выбранным серверам без потери скорости.',
            features: ['3 сервера', 'AES-256', 'Без ограничений трафика'],
        },
        {
            id: 'STANDARD', name: 'Премиум', devicesLabel: '2 устройства', price: 150, isHit: true,
            icon: Globe2, color: 'text-blue-400', bgColor: 'bg-blue-500/10', borderColor: 'border-blue-500/25',
            description: 'Все серверы, два устройства одновременно, приоритетное подключение.',
            features: ['Все серверы', 'AES-256', '2 устройства', 'Приоритетный канал'],
        },
        {
            id: 'FAMILY', name: 'Семья', devicesLabel: '3 устройства', price: 350,
            icon: Users, color: 'text-green-400', bgColor: 'bg-green-500/10', borderColor: 'border-white/10',
            description: 'Все серверы для всей семьи — три устройства одновременно.',
            features: ['Все серверы', 'AES-256', '3 устройства', 'Приоритетный канал'],
        },
    ];

    return (
        <div className={`space-y-4 pt-2 pb-10 ${isRTL ? 'text-right' : 'text-left'}`}>

            {/* БАЛАНС */}
            <div className={`bg-[#12141d] border border-white/10 rounded-[2rem] p-5 shadow-2xl flex justify-between items-center relative overflow-hidden ${isRTL ? 'flex-row-reverse' : 'flex-row'}`}>
                <div className="absolute top-[-50%] right-[-10%] w-[100px] h-[100px] bg-primary/20 blur-[50px] rounded-full pointer-events-none" />
                <div className="relative z-10">
                    <p className="text-white/40 text-[10px] font-black uppercase tracking-[0.2em] mb-1">{t.balance}</p>
                    <h2 className="text-[32px] font-black tracking-tighter text-white leading-none">
                        {(user?.balance ? user.balance / 100 : 0).toFixed(0)} <span className="text-[16px] font-bold text-white/50">₽</span>
                    </h2>
                </div>
                <button
                    onClick={() => { haptic('light'); setActiveTab('deposit'); }}
                    className="bg-white text-black px-5 py-3 rounded-2xl font-black text-[12px] uppercase tracking-widest active:scale-95 transition-all"
                >
                    {t.top_up}
                </button>
            </div>

            {IS_PROMO_ACTIVE && (
                <div className="space-y-3">
                    <h3 className={`text-[17px] font-black text-white uppercase tracking-tighter italic leading-none flex items-center gap-2 px-1 ${isRTL ? 'flex-row-reverse' : ''}`}>
                        <Sparkles size={17} className="text-white/60" /> Ограниченное предложение
                    </h3>

                    {promoAvailable ? (
                        <motion.div
                            initial={{ scale: 0.93, opacity: 0, y: 12 }}
                            animate={{ scale: 1, opacity: 1, y: 0 }}
                            className="relative rounded-[2rem] overflow-hidden"
                            style={{ minHeight: 220 }}
                        >
                            <div className="absolute inset-0 bg-cover bg-center" style={{ backgroundImage: `url(${promoImage})` }} />
                            <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/40 to-black/10" />

                            <div className="relative z-10 p-5 flex flex-col gap-3 h-full" style={{ minHeight: 220 }}>
                                <div className="bg-white/15 backdrop-blur-sm border border-white/20 px-3 py-1 rounded-full w-fit flex items-center gap-1.5">
                                    <Gift size={11} className="text-white" />
                                    <span className="text-white text-[10px] font-black uppercase tracking-widest">GEO VPN</span>
                                </div>

                                <div className="mt-auto">
                                    <h4 className="text-[28px] font-black text-white uppercase italic tracking-tighter leading-none">Месяц в подарок</h4>
                                    <p className="text-white/60 text-[12px] font-semibold mt-1">3 сервера и безопасное соединение</p>

                                    <div className="flex items-baseline gap-3 mt-2">
                                        <span className="text-white/30 text-[15px] font-black line-through italic">100 ₽</span>
                                        <span className="text-white text-[36px] font-black italic leading-none">0 ₽</span>
                                    </div>

                                    <button
                                        onClick={() => handlePay(promoTariff)}
                                        disabled={!!payingId || user?.hasActiveSubscription}
                                        className="w-full bg-white text-black h-[50px] rounded-2xl flex items-center justify-center gap-2 font-black uppercase text-[13px] mt-4 active:scale-[0.98] transition-all disabled:opacity-60"
                                    >
                                        <Gift size={16} />
                                        {user?.hasActiveSubscription ? 'Подписка уже активна' : (payingId === 'BASIC' ? 'Оформляем...' : 'Забрать бесплатно')}
                                    </button>
                                </div>
                            </div>
                        </motion.div>
                    ) : (
                        <div className="p-5 rounded-[2rem] bg-white/5 border border-white/10 flex items-center gap-4">
                            <CheckCircle2 size={28} className="text-emerald-500/50 shrink-0" />
                            <div>
                                <p className="text-white font-black text-[14px]">Подписка активна</p>
                                <p className="text-white/50 text-[12px] mt-0.5">Бесплатный период уже был использован.</p>
                            </div>
                        </div>
                    )}
                </div>
            )}

            <div className="space-y-2">
                {tariffs.map((tariff) => {
                    const isExpanded = expandedId === tariff.id;
                    const hasActiveSub = user?.hasActiveSubscription;

                    return (
                        <div key={tariff.id} className={`rounded-[1.5rem] border transition-all duration-300 ${isExpanded ? 'bg-white/5 border-white/20' : 'bg-[#0f1117] border-white/8'}`}>
                            <button onClick={() => handleToggle(tariff.id)} className={`w-full flex items-center p-4 gap-3 active:bg-white/5 transition-colors ${isRTL ? 'flex-row-reverse' : 'flex-row'}`}>
                                <div className={`w-12 h-12 ${tariff.bgColor} rounded-2xl flex items-center justify-center border border-white/5 shrink-0`}>
                                    <tariff.icon size={22} className={tariff.color} />
                                </div>
                                <div className={`flex-1 min-w-0 ${isRTL ? 'text-right' : 'text-left'}`}>
                                    <div className="flex items-center gap-2">
                                        <p className="text-white font-black text-[16px] leading-tight truncate">{tariff.name}</p>
                                        {tariff.isHit && <div className="bg-blue-500 text-white text-[8px] font-black uppercase px-2 py-0.5 rounded-full">Хит</div>}
                                    </div>
                                    <p className="text-white/30 text-[11px] font-bold mt-1 uppercase tracking-tighter">{tariff.devicesLabel}</p>
                                </div>
                                <div className="text-right">
                                    <p className="text-white font-black text-[17px] leading-none">{tariff.price} ₽</p>
                                    <p className="text-white/20 text-[9px] font-bold mt-1.5">/ мес</p>
                                </div>
                                <div className={`w-8 h-8 rounded-full flex items-center justify-center bg-white/5 transition-transform ${isExpanded ? 'rotate-180' : ''}`}>
                                    <ChevronDown size={14} className="text-white/40" />
                                </div>
                            </button>

                            <AnimatePresence>
                                {isExpanded && (
                                    <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }}>
                                        <div className="px-4 pb-4 border-t border-white/5 bg-black/20">
                                            <p className="text-white/50 text-[13px] leading-relaxed mt-4 mb-4">{tariff.description}</p>
                                            <div className="flex flex-wrap gap-2 mb-5">
                                                {tariff.features.map((feat, i) => (
                                                    <div key={i} className="flex items-center gap-1.5 px-3 py-1.5 rounded-full border border-white/10 bg-white/5 text-[10px] font-black uppercase">
                                                        <CheckCircle2 size={10} className="text-emerald-500" />
                                                        <span className="text-white/70">{feat}</span>
                                                    </div>
                                                ))}
                                            </div>
                                            <button
                                                onClick={() => handlePay(tariff)}
                                                disabled={!!payingId || hasActiveSub}
                                                className={`w-full h-[54px] rounded-2xl flex items-center justify-center font-black text-[14px] uppercase transition-all ${tariff.isHit ? 'bg-blue-500 text-white shadow-blue-500/20' : 'bg-white text-black'} disabled:bg-white/10 disabled:text-white/20`}
                                            >
                                                {payingId === tariff.id ? '...' : (hasActiveSub ? 'Тариф уже активен' : `Оформить за ${tariff.price} ₽`)}
                                            </button>
                                            {hasActiveSub && (
                                                <p className="text-center text-[10px] text-amber-500/40 font-bold uppercase mt-3 tracking-widest">
                                                    Смена тарифа будет доступна после окончания текущего
                                                </p>
                                            )}
                                        </div>
                                    </motion.div>
                                )}
                            </AnimatePresence>
                        </div>
                    );
                })}
            </div>
        </div>
    );
}
