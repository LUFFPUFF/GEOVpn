import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useUserStore } from '../../store/userStore';
import {
    Globe2, Smartphone, Users, Briefcase,
    Infinity, Gift, Sparkles, CheckCircle2, Shield, Wifi, Timer,
    LucideIcon
} from 'lucide-react';

// Расширяем глобальный объект Window для работы с Telegram WebApp в TS
declare global {
    interface Window {
        Telegram?: {
            WebApp: any;
        };
    }
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
    bgGlow: string;
    description: string;
    isHit?: boolean;
    isPromo?: boolean;
}

const IS_PROMO_ACTIVE = true;

export default function Payments() {
    const { user, t, purchaseSubscription, setActiveTab } = useUserStore();
    const [selectedTariff, setSelectedTariff] = useState<Tariff | null>(null);
    const [alreadyUsedPromo, setAlreadyUsedPromo] = useState(false);

    const realBalance = user?.balance ? (user.balance / 100).toFixed(0) : '0';

    const isPayg = !user?.subscriptionType ||
        String(user.subscriptionType).toUpperCase() === 'PAYG';

    const promoAvailable = IS_PROMO_ACTIVE && isPayg && !alreadyUsedPromo;

    useEffect(() => {
        const tg = window.Telegram?.WebApp;
        if (tg) {
            tg.ready();
            tg.expand(); // Гарантирует, что TMA открыт на весь экран

            if (selectedTariff) {
                // Если открыта шторка оплаты — отключаем жест сворачивания окна TMA
                tg.disableVerticalSwipes?.();
            } else {
                // Если закрыта — возвращаем нативный свайп
                tg.enableVerticalSwipes?.();
            }
        }
    }, [selectedTariff]);

    const haptic = (type: ImpactStyle = 'medium') =>
        window.Telegram?.WebApp?.HapticFeedback?.impactOccurred(type);

    const handlePay = async () => {
        if (!selectedTariff) return;

        const isPromo = selectedTariff.isPromo === true;

        try {
            const success = await purchaseSubscription(selectedTariff.id, 1, isPromo);
            setSelectedTariff(null);

            if (success) {
                window.Telegram?.WebApp?.showConfirm('Подписка успешно оформлена!', (ok: boolean) => {
                    if (ok) setActiveTab('home');
                });
            } else if (isPromo) {
                setAlreadyUsedPromo(true);
                window.Telegram?.WebApp?.showAlert('Вы уже использовали акцию 🙂');
            }
        } catch (error: any) {
            setSelectedTariff(null);

            // Вытаскиваем точное сообщение с бэкенда или статус сети
            const serverMsg = error?.response?.data?.error?.message;
            const statusCode = error?.response?.status;
            const errorMessage = serverMsg ? `${serverMsg} (Код: ${statusCode})` : error.message;

            window.Telegram?.WebApp?.showAlert(`Ошибка: ${errorMessage}`);
            console.error("Payment failed full error:", error);
        }
    };

    const promoTariff: Tariff = {
        id: 'BASIC',
        name: 'Стандарт',
        devicesLabel: '1 устройство',
        price: 0,
        oldPrice: 150,
        icon: Gift,
        color: 'text-yellow-400',
        bgGlow: 'bg-yellow-500/20',
        description: 'Полный доступ к GeoVPN на 30 дней — абсолютно бесплатно.',
        isPromo: true,
    };

    const tariffs: Tariff[] = [
        {
            id: 'BASIC', name: 'Стандарт', devicesLabel: '1 устройство', price: 150,
            icon: Smartphone, color: 'text-gray-300', bgGlow: 'bg-gray-500/10',
            description: 'Базовый доступ к заблокированным ресурсам без потери скорости.'
        },
        {
            id: 'STANDARD', name: 'Стандарт+', devicesLabel: '3 устройства', price: 400,
            icon: Globe2, color: 'text-blue-400', bgGlow: 'bg-blue-500/10',
            description: 'Три устройства, все серверы, приоритетное подключение.', isHit: true,
        },
        {
            id: 'FAMILY', name: 'Семейный', devicesLabel: '5 устройств', price: 700,
            icon: Users, color: 'text-green-400', bgGlow: 'bg-green-500/10',
            description: 'До пяти устройств для всей семьи.'
        },
        {
            id: 'BUSINESS', name: 'Бизнес', devicesLabel: '15 устройств', price: 2000,
            icon: Briefcase, color: 'text-purple-400', bgGlow: 'bg-purple-500/10',
            description: 'Для команд. Выделенный канал + приоритет трафика.'
        },
        {
            id: 'UNLIMITED', name: 'Безлимит', devicesLabel: '∞ устройств', price: 3000,
            icon: Infinity, color: 'text-rose-400', bgGlow: 'bg-rose-500/10',
            description: 'Неограниченное количество устройств и трафика.'
        },
    ];

    return (
        // Используем pb-32 для безопасной зоны снизу (где системная навигация телефона)
        // touch-none предотвращает "отскоки" браузера на iOS
        <div className="space-y-6 pt-2 pb-32 relative select-none overflow-y-auto custom-scrollbar h-[100vh]">

            {/* ── Баланс ─────────────────────────────────────────────────────── */}
            <div className="bg-gradient-to-br from-[#12141d] to-[#0a0a0f] border border-white/10 rounded-[2rem] p-7 shadow-2xl flex justify-between items-center relative overflow-hidden mx-1">
                <div className="absolute top-[-50%] right-[-10%] w-[100px] h-[100px] bg-primary/20 blur-[50px] rounded-full pointer-events-none" />
                <div className="relative z-10">
                    <p className="text-white/40 text-[11px] font-black uppercase tracking-[0.2em] mb-2">{t.balance}</p>
                    <h2 className="text-[40px] font-black tracking-tighter text-white leading-none">
                        {realBalance} <span className="text-[20px] font-bold text-white/50 ml-1">₽</span>
                    </h2>
                </div>
                <button className="bg-white text-black px-6 py-4 rounded-2xl font-black text-[13px] uppercase active:scale-95 transition-transform shadow-[0_0_15px_rgba(255,255,255,0.2)]">
                    Пополнить
                </button>
            </div>

            {/* ── Промо-блок ─────────────────────────────────────────────────── */}
            {IS_PROMO_ACTIVE && (
                <div className="px-1 space-y-4">
                    <h3 className="text-[20px] font-black px-2 mt-4 mb-2 text-white uppercase tracking-tighter italic leading-none flex items-center gap-2">
                        <Sparkles size={20} className="text-yellow-400" /> Ограниченное предложение
                    </h3>

                    {promoAvailable ? (
                        <motion.div
                            initial={{ scale: 0.93, opacity: 0, y: 12 }}
                            animate={{ scale: 1, opacity: 1, y: 0 }}
                            transition={{ type: 'spring', stiffness: 260, damping: 20 }}
                            onClick={() => { setSelectedTariff(promoTariff); haptic('medium'); }}
                            className="relative rounded-[2.5rem] overflow-hidden cursor-pointer active:scale-[0.98] transition-transform"
                        >
                            <div className="absolute inset-0 bg-gradient-to-br from-yellow-500/25 via-amber-600/10 to-[#0a0a0f]" />
                            <motion.div
                                animate={{ x: [-600, 600] }}
                                transition={{ duration: 3.5, repeat: Infinity, ease: 'linear', repeatDelay: 1 }}
                                className="absolute inset-0 bg-gradient-to-r from-transparent via-white/8 to-transparent skew-x-12 pointer-events-none"
                            />
                            <div className="absolute inset-0 rounded-[2.5rem] border-2 border-yellow-500/40" />

                            <div className="relative z-10 p-8 flex flex-col items-center text-center gap-5">
                                <div className="w-[76px] h-[76px] bg-yellow-500 rounded-[1.5rem] flex items-center justify-center shadow-[0_0_40px_rgba(234,179,8,0.5)]">
                                    <Gift size={38} className="text-black" />
                                </div>
                                <div>
                                    <h4 className="text-[30px] font-black text-white uppercase italic tracking-tighter leading-none">Месяц в подарок</h4>
                                    <p className="text-white/55 text-[13px] font-semibold mt-2">Полный доступ на 30 дней</p>
                                </div>
                                <div className="flex items-center gap-5">
                                    <span className="text-white/25 text-[22px] font-black line-through italic">150 ₽</span>
                                    <span className="text-yellow-400 text-[52px] font-black italic">0 ₽</span>
                                </div>
                                <div className="flex flex-wrap justify-center gap-2">
                                    {[
                                        { icon: Wifi, label: 'Безлимит' },
                                        { icon: Shield, label: 'AES-256' },
                                        { icon: Globe2, label: 'Все серверы' },
                                    ].map((item, idx) => (
                                        <div key={idx} className="flex items-center gap-1.5 bg-white/5 border border-white/8 px-3 py-1.5 rounded-full">
                                            <item.icon size={11} className="text-yellow-400" />
                                            <span className="text-[10px] font-black uppercase text-white/75">{item.label}</span>
                                        </div>
                                    ))}
                                </div>
                                <div className="w-full bg-yellow-500 text-black h-[58px] rounded-2xl flex items-center justify-center gap-3 shadow-[0_4px_24px_rgba(234,179,8,0.4)]">
                                    <Gift size={20} className="text-black" />
                                    <span className="font-black uppercase text-[15px]">Забрать бесплатно</span>
                                </div>
                            </div>
                        </motion.div>
                    ) : (
                        <div className="relative p-6 rounded-[2rem] bg-white/5 border border-white/10 flex items-center gap-4 mx-0">
                            <CheckCircle2 size={32} className="text-yellow-500 shrink-0" />
                            <div>
                                <p className="text-white font-black text-[15px]">Подписка активна</p>
                                <p className="text-white/50 text-[12px] mt-0.5">Бесплатный месяц уже использован или у вас есть активная подписка.</p>
                            </div>
                        </div>
                    )}

                    <div className="flex items-center gap-3 px-2 pt-4">
                        <div className="flex-1 h-px bg-white/8" />
                        <span className="text-white/30 text-[10px] font-black uppercase tracking-widest">или выберите тариф</span>
                        <div className="flex-1 h-px bg-white/8" />
                    </div>
                </div>
            )}

            {/* ── Список тарифов ─────────────────────────────────────────────── */}
            <div className="space-y-3 px-1">
                {tariffs.map((tariff) => (
                    <motion.div
                        key={tariff.id}
                        whileTap={{ scale: 0.98 }}
                        onClick={() => { setSelectedTariff(tariff); haptic('light'); }}
                        className={`relative flex items-center justify-between p-5 rounded-[1.5rem] border cursor-pointer ${
                            tariff.isHit ? 'bg-gradient-to-r from-blue-500/15 to-[#0a0a0f] border-blue-500/30' : 'bg-[#0f1117] border-white/8'
                        }`}
                    >
                        {tariff.isHit && (
                            <div className="absolute top-3 right-3 bg-blue-500 text-white text-[9px] font-black uppercase px-2 py-0.5 rounded-full tracking-widest">Хит</div>
                        )}
                        <div className="flex items-center gap-4">
                            <div className={`w-11 h-11 ${tariff.bgGlow} rounded-2xl flex items-center justify-center border border-white/5`}>
                                <tariff.icon size={22} className={tariff.color} />
                            </div>
                            <div>
                                <p className="text-white font-black text-[15px]">{tariff.name}</p>
                                <p className="text-white/40 text-[11px] font-bold">{tariff.devicesLabel}</p>
                            </div>
                        </div>
                        <div className="text-right">
                            <p className="text-white font-black text-[17px]">{tariff.price} ₽</p>
                            <p className="text-white/30 text-[10px]">/ мес</p>
                        </div>
                    </motion.div>
                ))}
            </div>

            {/* ── Шторка подтверждения ───────────────────────────────────────── */}
            <AnimatePresence>
                {selectedTariff && (
                    <>
                        <motion.div
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            exit={{ opacity: 0 }}
                            onClick={() => setSelectedTariff(null)}
                            // z-[200] гарантирует что затемнение перекроет нижнюю панель навигации (NavigationBar)
                            className="fixed inset-0 bg-black/90 backdrop-blur-md z-[200]"
                        />

                        <motion.div
                            initial={{ y: '100%' }}
                            animate={{ y: 0 }}
                            exit={{ y: '100%' }}
                            transition={{ type: 'spring', stiffness: 350, damping: 35 }}
                            // touch-auto разрешает внутренний скролл, z-[210] поверх затемнения
                            className="fixed bottom-0 left-0 right-0 z-[210] mx-auto w-full max-w-[480px] bg-[#0a0a0f] border-t border-white/10 rounded-t-[2.5rem] shadow-[0_-20px_60px_rgba(0,0,0,0.9)] px-6 pt-8 pb-12 touch-auto"
                        >
                            <div className="w-12 h-1.5 bg-white/20 rounded-full mx-auto mb-8 cursor-grab" />

                            <div className="flex items-center gap-4 mb-8">
                                <div className={`w-16 h-16 ${selectedTariff.bgGlow} rounded-2xl flex items-center justify-center border border-white/10`}>
                                    <selectedTariff.icon size={32} className={selectedTariff.color} />
                                </div>
                                <div className="flex-1">
                                    <p className="text-white font-black text-[20px] leading-none">{selectedTariff.name}</p>
                                    <p className="text-white/40 text-[13px] font-bold mt-1">{selectedTariff.devicesLabel}</p>
                                </div>
                                <div className="text-right">
                                    <p className={`font-black text-[26px] ${selectedTariff.price === 0 ? 'text-yellow-400' : 'text-white'}`}>
                                        {selectedTariff.price === 0 ? '0 ₽' : `${selectedTariff.price} ₽`}
                                    </p>
                                </div>
                            </div>

                            <p className="text-white/50 text-[14px] leading-relaxed mb-10 px-1">
                                {selectedTariff.description}
                            </p>

                            <motion.button
                                whileTap={{ scale: 0.96 }}
                                // onPointerDown отрабатывает мгновенно (без задержки в 300мс на мобильных устройствах)
                                onPointerDown={() => haptic('heavy')}
                                onClick={handlePay}
                                className={`w-full h-[68px] rounded-[1.3rem] flex items-center justify-center font-black text-[16px] uppercase tracking-widest relative overflow-hidden ${
                                    selectedTariff.price === 0
                                        ? 'bg-yellow-500 text-black shadow-[0_10px_30px_rgba(234,179,8,0.3)]'
                                        : 'bg-white text-black shadow-[0_10px_30px_rgba(255,255,255,0.1)]'
                                }`}
                            >
                                {selectedTariff.price === 0 ? '🎁 Забрать бесплатно' : 'Подтвердить оплату'}
                            </motion.button>

                            <div className="h-6" /> {/* Безопасный отступ снизу */}
                        </motion.div>
                    </>
                )}
            </AnimatePresence>
        </div>
    );
}