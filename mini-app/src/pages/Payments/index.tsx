import React, { useState, useMemo, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useUserStore } from '../../store/userStore';
import {
    Smartphone, Users, Gift, Sparkles,
    CheckCircle2, ChevronDown,
    LucideIcon, Calculator, Zap, RefreshCcw,
    Lock, FastForward, HelpCircle, Timer,
    Headphones
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
    weight: number;
    icon: LucideIcon | React.ElementType;
    color: string;
    bgColor: string;
    description: string;
    features: string[];
    isPromo?: boolean;
}

const IS_PROMO_ACTIVE = true;

const safeAlert = (text: string) => {
    if (window.Telegram?.WebApp?.isVersionAtLeast('6.2')) {
        window.Telegram.WebApp.showAlert(text);
    } else {
        alert(text);
    }
};

export default function Payments() {
    const { user, purchaseSubscription, setActiveTab } = useUserStore();
    const [expandedId, setExpandedId] = useState<string | null>(null);
    const [payingId, setPayingId] = useState<string | null>(null);

    useEffect(() => {
        const tg = window.Telegram?.WebApp;
        if (tg) { tg.ready(); tg.expand(); }
    }, []);

    const isExpired = useMemo(() => {
        if (!user?.subscriptionExpiresAt) return true;
        return new Date(user.subscriptionExpiresAt).getTime() < new Date().getTime();
    }, [user]);

    const realBalanceValue = user?.balance || 0;
    const realBalanceDisplay = (realBalanceValue / 100).toFixed(0);

    const hasActiveSub = (user?.hasActiveSubscription ?? false) && !isExpired;
    const currentPlanId = user?.subscriptionType?.toUpperCase() || 'PAYG';
    const isPayg = currentPlanId === 'PAYG';

    // Промо доступно только если включено и юзер на PAYG (никогда не платил)
    const promoAvailable = IS_PROMO_ACTIVE && isPayg;

    const haptic = (type: ImpactStyle = 'medium') =>
        window.Telegram?.WebApp?.HapticFeedback?.impactOccurred(type);

    const handleToggle = (id: string) => {
        haptic('light');
        setExpandedId(prev => prev === id ? null : id);
    };

    const handlePay = async (tariff: Tariff) => {
        if (payingId) return;

        // Обработка промо-тарифа
        if (tariff.isPromo) {
            if (hasActiveSub || !isPayg) {
                safeAlert('Бесплатный период доступен только для новых пользователей без подписки.');
                return;
            }
        } else {
            // Обычная покупка: блокировки работают только если подписка НЕ истекла
            if (!isExpired) {
                if (currentPlanId === tariff.id) {
                    safeAlert("Этот тариф уже активен.");
                    return;
                }
                const currentWeight = tariffs.find(t => t.id === currentPlanId)?.weight || 0;
                if (currentWeight > tariff.weight) {
                    safeAlert("Выбранный тариф ниже вашего текущего. Дождитесь окончания подписки.");
                    return;
                }
            }

            const priceInKopecks = tariff.price * 100;
            if (realBalanceValue < priceInKopecks) {
                haptic('medium');
                const msg = `Недостаточно средств. Нужно: ${tariff.price} ₽. Пополнить баланс?`;
                if (window.Telegram?.WebApp?.isVersionAtLeast('6.2')) {
                    window.Telegram.WebApp.showConfirm(msg, (ok: boolean) => { if (ok) setActiveTab('deposit'); });
                } else if (window.confirm(msg)) {
                    setActiveTab('deposit');
                }
                return;
            }
        }

        haptic('heavy');
        setPayingId(tariff.id);
        try {
            const success = await purchaseSubscription(tariff.id, 1, tariff.isPromo === true);
            if (success) {
                haptic('rigid');
                setActiveTab('home');
                safeAlert(tariff.isPromo ? 'Бесплатный месяц успешно активирован! 🎉' : 'Подписка успешно оформлена!');
            }
        } catch (error: any) {
            const serverMsg = error?.response?.data?.error?.message;
            safeAlert(`Ошибка: ${serverMsg || error.message}`);
        } finally {
            setPayingId(null);
            setExpandedId(null);
        }
    };

    const promoTariff: Tariff = {
        id: 'BASIC', name: 'Стандарт', devicesLabel: '1 устройство',
        price: 0, weight: 1, icon: Gift, color: 'text-white', bgColor: 'bg-white/10',
        description: 'Полный доступ к 3 локациям GeoVPN на 30 дней — абсолютно бесплатно.',
        features: ['3 локации', 'AES-256', 'Безлимит трафика'], isPromo: true,
    };

    const tariffs: Tariff[] = [
        {
            id: 'DAILY', name: 'Пробный', devicesLabel: '1 устройство', price: 6, weight: 0.5,
            icon: Timer, color: 'text-fuchsia-400', bgColor: 'bg-fuchsia-500/10',
            description: 'Доступ на 1 день. Идеально для проверки скорости и качества.',
            features: ['3 локации', 'AES-256', 'Доступ на 24 часа'],
        },
        {
            id: 'BASIC', name: 'Стандарт', devicesLabel: '1 устройство', price: 100, weight: 1,
            icon: Smartphone, color: 'text-gray-400', bgColor: 'bg-white/5',
            description: 'Базовый доступ к серверам и защита одного устройства.',
            features: ['3 локации', 'AES-256', 'БЕЗ ОГРАНИЧЕНИЙ ТРАФИКА'],
        },
        {
            id: 'STANDARD', name: 'Премиум', devicesLabel: '2 устройства', price: 150, weight: 2,
            icon: Zap, color: 'text-blue-400', bgColor: 'bg-blue-500/10',
            description: 'Оптимальный выбор для активных пользователей. Телефон + ПК.',
            features: ['Все локации', '2 устройства', 'ПРИОРИТЕТНЫЙ КАНАЛ'],
        },
        {
            id: 'FAMILY', name: 'Семья', devicesLabel: '3 устройства', price: 350, weight: 3,
            icon: Users, color: 'text-emerald-400', bgColor: 'bg-emerald-500/10',
            description: 'Максимальный тариф для всех ваших гаджетов или близких.',
            features: ['Все локации', '3 устройства', 'ПРИОРИТЕТНЫЙ КАНАЛ'],
        },
    ];

    const currentPlanWeight = !isExpired ? (tariffs.find(t => t.id === currentPlanId)?.weight || 0) : 0;

    return (
        <div className="flex flex-col gap-4 pt-2 pb-32 px-1 text-left animate-in fade-in duration-500">

            {/* БАЛАНС */}
            <div className="bg-[#12141d] border border-white/10 rounded-[2rem] p-5 shadow-2xl flex justify-between items-center relative overflow-hidden">
                <div className="absolute top-[-50%] right-[-10%] w-[100px] h-[100px] bg-emerald-500/10 blur-[50px] rounded-full pointer-events-none" />
                <div className="relative z-10">
                    <p className="text-white/40 text-[10px] font-black uppercase tracking-[0.2em] mb-1">Мой счет</p>
                    <h2 className="text-[32px] font-black tracking-tighter text-white leading-none">
                        {realBalanceDisplay} <span className="text-[16px] font-bold text-white/50">₽</span>
                    </h2>
                </div>
                <button onClick={() => { haptic('light'); setActiveTab('deposit'); }} className="bg-white text-black px-6 py-3 rounded-2xl font-black text-[12px] uppercase tracking-widest active:scale-95 transition-all shadow-lg shadow-white/5 relative z-10">
                    Пополнить
                </button>
            </div>

            {/* ПРОМО БЛОК */}
            {IS_PROMO_ACTIVE && (
                <div className="space-y-3 mt-2">
                    <h3 className="text-[15px] font-black text-white uppercase tracking-tighter italic leading-none flex items-center gap-2 px-1">
                        <Sparkles size={17} className="text-white/60" /> Ограниченное предложение
                    </h3>

                    {promoAvailable ? (
                        <motion.div initial={{ scale: 0.93, opacity: 0, y: 12 }} animate={{ scale: 1, opacity: 1, y: 0 }} className="relative rounded-[2rem] overflow-hidden" style={{ minHeight: 220 }}>
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
                                        disabled={!!payingId || !isPayg}
                                        className="w-full bg-white text-black h-[50px] rounded-2xl flex items-center justify-center gap-2 font-black uppercase text-[13px] mt-4 active:scale-[0.98] transition-all disabled:opacity-60"
                                    >
                                        <Gift size={16} />
                                        {payingId === 'BASIC' ? 'Оформляем...' : 'Забрать бесплатно'}
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

            {/* ИНФО ОБ АПГРЕЙДЕ */}
            {hasActiveSub && currentPlanId !== 'FAMILY' && (
                <div className="bg-emerald-500/5 border border-emerald-500/20 rounded-[1.8rem] p-5 relative overflow-hidden mt-2">
                    <div className="flex items-center gap-4 relative z-10 text-left">
                        <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 flex items-center justify-center shrink-0 border border-emerald-500/10">
                            <Calculator size={24} className="text-emerald-500" />
                        </div>
                        <div>
                            <p className="text-[13px] font-black text-white uppercase italic leading-tight">Умный Апгрейд</p>
                            <p className="text-[10px] text-white/40 mt-1 leading-snug font-medium">
                                Остаток дней будет переведен в рубли и вычтен из стоимости нового тарифа.
                            </p>
                        </div>
                    </div>
                </div>
            )}

            {/* СПИСОК ТАРИФОВ */}
            <div className="flex flex-col gap-3 mt-2">
                <h3 className="text-[13px] font-black text-white/20 uppercase tracking-[0.2em] ml-2 italic">Доступные тарифы</h3>
                {tariffs.map((tariff) => {
                    const isExpanded = expandedId === tariff.id;
                    const isCurrent = currentPlanId === tariff.id && !isExpired;
                    const isLower = currentPlanWeight > tariff.weight && !isExpired;

                    return (
                        <div key={tariff.id} className={`rounded-[2rem] border transition-all duration-500 overflow-hidden ${isCurrent ? 'bg-emerald-500/5 border-emerald-500/40 shadow-[0_0_20px_rgba(16,185,129,0.05)]' : (isExpanded ? 'bg-white/5 border-white/20' : 'bg-[#12141d] border-white/5')}`}>
                            <button onClick={() => handleToggle(tariff.id)} className="w-full flex items-center p-5 gap-4 outline-none">
                                <div className={`w-12 h-12 ${tariff.bgColor} rounded-2xl flex items-center justify-center border border-white/5 shrink-0 shadow-inner`}>
                                    <tariff.icon size={22} className={tariff.color} />
                                </div>
                                <div className="flex-1 text-left">
                                    <div className="flex items-center gap-2 mb-0.5">
                                        <p className="text-white font-black text-[17px] italic uppercase tracking-tight">{tariff.name}</p>
                                        {isCurrent && <div className="bg-emerald-500 text-black text-[7px] font-black uppercase px-2 py-0.5 rounded-md shadow-sm">Активен</div>}
                                        {isLower && <div className="bg-white/5 text-white/20 text-[7px] font-black uppercase px-2 py-0.5 rounded-md border border-white/5">Ниже</div>}
                                    </div>
                                    <p className="text-white/30 text-[10px] font-black uppercase tracking-widest">{tariff.devicesLabel}</p>
                                </div>
                                <div className="text-right shrink-0">
                                    <p className="text-white font-black text-[18px] leading-none">{tariff.price} ₽</p>
                                    <p className="text-white/20 text-[8px] font-black mt-1.5 uppercase tracking-tighter">{tariff.id === 'DAILY' ? '/ день' : '/ месяц'}</p>
                                </div>
                                <ChevronDown size={18} className={`text-white/10 transition-transform duration-300 shrink-0 ${isExpanded ? 'rotate-180' : ''}`} />
                            </button>

                            <AnimatePresence>
                                {isExpanded && (
                                    <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }}>
                                        <div className="px-5 pb-6 border-t border-white/5 text-left">
                                            <p className="text-white/50 text-[13px] leading-relaxed my-5 font-medium">{tariff.description}</p>
                                            <div className="flex flex-wrap gap-2 mb-8">
                                                {tariff.features.map((feat, i) => (
                                                    <div key={i} className="flex items-center gap-1.5 px-3 py-2 rounded-xl border border-white/5 bg-black/30 text-[8px] font-black uppercase text-white/70 tracking-wide">
                                                        <CheckCircle2 size={12} className="text-emerald-500" /> {feat}
                                                    </div>
                                                ))}
                                            </div>

                                            <button
                                                onClick={() => handlePay(tariff)}
                                                disabled={!!payingId || isCurrent || isLower}
                                                className={`w-full h-[60px] rounded-2xl flex items-center justify-center gap-3 font-black text-[14px] uppercase transition-all shadow-xl ${
                                                    isCurrent || isLower
                                                        ? 'bg-white/5 text-white/40 cursor-default border border-white/5'
                                                        : 'bg-white text-black active:scale-[0.98] shadow-white/5'
                                                }`}
                                            >
                                                {payingId === tariff.id ? (
                                                    <RefreshCcw size={22} className="animate-spin" />
                                                ) : isCurrent ? (
                                                    'Тариф активирован'
                                                ) : isLower ? (
                                                    'Тариф ниже текущего'
                                                ) : (
                                                    <><Zap size={18} className="fill-current" /> Оформить за {tariff.price} ₽</>
                                                )}
                                            </button>
                                        </div>
                                    </motion.div>
                                )}
                            </AnimatePresence>
                        </div>
                    );
                })}
            </div>

            {/* ПРЕИМУЩЕСТВА */}
            <div className="grid grid-cols-2 gap-3 mt-2">
                <div className="bg-[#12141d] border border-white/5 p-5 rounded-[2rem] flex flex-col gap-4 text-left shadow-lg">
                    <div className="w-10 h-10 rounded-2xl bg-emerald-500/10 flex items-center justify-center border border-emerald-500/10">
                        <Lock size={20} className="text-emerald-500" />
                    </div>
                    <div>
                        <p className="text-white font-black text-[12px] uppercase italic tracking-tight">Анонимность</p>
                        <p className="text-[9px] text-white/30 leading-snug mt-1.5 font-bold uppercase">Без логов и хранения данных сессий.</p>
                    </div>
                </div>
                <div className="bg-[#12141d] border border-white/5 p-5 rounded-[2rem] flex flex-col gap-4 text-left shadow-lg">
                    <div className="w-10 h-10 rounded-2xl bg-blue-500/10 flex items-center justify-center border border-blue-500/10">
                        <FastForward size={20} className="text-blue-500" />
                    </div>
                    <div>
                        <p className="text-white font-black text-[12px] uppercase italic tracking-tight">Скорость</p>
                        <p className="text-[9px] text-white/30 leading-snug mt-1.5 font-bold uppercase">До 10 Гбит/с на каждом выделенном сервере.</p>
                    </div>
                </div>
            </div>

            {/* ПОДДЕРЖКА */}
            <div className="mt-4 p-8 bg-[#12141d]/50 border border-dashed border-white/10 rounded-[2.2rem] text-center flex flex-col items-center">
                <div className="w-12 h-12 rounded-full bg-white/5 flex items-center justify-center mb-4">
                    <Headphones size={24} className="text-white/20" />
                </div>
                <h4 className="text-white font-black text-[14px] uppercase italic mb-1">Нужна помощь?</h4>
                <p className="text-[10px] text-white/30 font-bold uppercase tracking-widest mb-4">
                    Вопросы по оплате или работе сервиса
                </p>
                <button
                    onClick={() => window.Telegram.WebApp.openTelegramLink('https://t.me/geovpn_support')}
                    className="bg-emerald-500 text-black px-8 py-3.5 rounded-2xl font-black text-[11px] uppercase tracking-widest active:scale-95 transition-all shadow-lg shadow-emerald-500/10"
                >
                    Написать в поддержку
                </button>
            </div>
        </div>
    );
}