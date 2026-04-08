import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useUserStore } from '../../store/userStore';
import { Zap, Crown, CheckCircle2, Globe2, Smartphone, Users, Briefcase, Infinity } from 'lucide-react';

interface Tariff {
    id: string;
    name: string;
    devicesLabel: string;
    price: number;
    icon: React.ElementType;
    color: string;
    bgGlow: string;
    description: string;
    isHit?: boolean;
}

export default function Payments() {
    const { user, t, purchaseSubscription, setActiveTab } = useUserStore();
    const [selectedTariff, setSelectedTariff] = useState<Tariff | null>(null);
    const realBalance = user?.balance ? (user.balance / 100).toFixed(0) : '0';

    const handlePay = async () => {
        if (!selectedTariff) return;

        const success = await purchaseSubscription(selectedTariff.id, selectedTariff.price);

        if (success) {
            setSelectedTariff(null);
            window.Telegram?.WebApp?.showConfirm("Подписка успешно оформлена! Перейти на главную?", (ok) => {
                if (ok) setActiveTab('home');
            });
        }
    };

    const tariffs: Tariff[] = [
        {
            id: 'BASIC', name: 'Стандарт', devicesLabel: '1 устройство', price: 150,
            icon: Smartphone, color: 'text-gray-300', bgGlow: 'bg-gray-500/10',
            description: 'Базовый доступ к заблокированным ресурсам без потери скорости.'
        },
        {
            id: 'STANDARD', name: 'Оптима', devicesLabel: '3 устройства', price: 400,
            icon: Zap, color: 'text-blue-400', bgGlow: 'bg-blue-500/10',
            description: 'Отличный выбор для смартфона, планшета и ПК.'
        },
        {
            id: 'FAMILY', name: 'Семья', devicesLabel: '5 устройств', price: 700, isHit: true,
            icon: Users, color: 'text-emerald-400', bgGlow: 'bg-emerald-500/10',
            description: 'Идеально для защиты всех гаджетов в вашей семье.'
        },
        {
            id: 'BUSINESS', name: 'Бизнес', devicesLabel: '15 устройств', price: 2000,
            icon: Briefcase, color: 'text-purple-400', bgGlow: 'bg-purple-500/10',
            description: 'Коллективный доступ для небольшой команды или офиса.'
        },
        {
            id: 'UNLIMITED', name: 'Безлимит', devicesLabel: '∞ устройств', price: 3000,
            icon: Infinity, color: 'text-amber-400', bgGlow: 'bg-amber-500/10',
            description: 'Максимальный уровень свободы. Подключайте всё, что угодно.'
        }
    ];

    return (
        <div className="space-y-6 pt-2 pb-24 relative select-none overflow-y-auto custom-scrollbar h-[85vh]">

            {/* БЛОК БАЛАНСА */}
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

            <h3 className="text-[20px] font-black px-2 mt-8 mb-2 text-white uppercase tracking-tighter italic leading-none">
                Выберите тариф
            </h3>

            {/* СПИСОК ТАРИФОВ */}
            <div className="space-y-3 px-1">
                {tariffs.map((tariff) => (
                    <div
                        key={tariff.id}
                        onClick={() => { setSelectedTariff(tariff); window.Telegram?.WebApp?.HapticFeedback.impactOccurred('light'); }}
                        className={`p-5 flex justify-between items-center relative overflow-hidden cursor-pointer rounded-[1.5rem] active:scale-[0.98] transition-all border 
                            ${tariff.isHit
                            ? 'bg-gradient-to-r from-emerald-500/10 to-[#0a0a0f] border-emerald-500/30 shadow-[0_0_20px_rgba(16,185,129,0.1)]'
                            : 'bg-[#12141d] border-white/5 hover:border-white/10 shadow-lg'}`}
                    >
                        {/* Неоновый блик слева */}
                        <div className={`absolute top-0 left-0 w-1 h-full ${tariff.bgGlow} opacity-50`} />

                        <div className="flex items-center gap-4 relative z-10">
                            <div className={`w-12 h-12 rounded-[1rem] flex items-center justify-center border border-white/5 ${tariff.bgGlow}`}>
                                <tariff.icon size={24} className={tariff.color} />
                            </div>
                            <div>
                                <div className="flex items-center gap-2 mb-0.5">
                                    <h4 className="font-black text-[17px] text-white uppercase italic tracking-tighter leading-none">{tariff.name}</h4>
                                    {tariff.isHit && (
                                        <span className="bg-emerald-500 text-white text-[9px] font-black uppercase px-2 py-0.5 rounded-full tracking-wider animate-pulse">Hit</span>
                                    )}
                                </div>
                                <p className="text-white/40 text-[11px] font-bold uppercase tracking-wider">{tariff.devicesLabel}</p>
                            </div>
                        </div>
                        <p className="font-black text-[20px] text-white relative z-10">{tariff.price} <span className="text-[14px] text-white/50">₽</span></p>
                    </div>
                ))}
            </div>

            {/* ШТОРКА ОПЛАТЫ */}
            <AnimatePresence>
                {selectedTariff && (
                    <>
                        <motion.div
                            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                            onClick={() => setSelectedTariff(null)}
                            className="fixed inset-0 bg-black/80 backdrop-blur-sm z-[150]"
                        />
                        <motion.div
                            initial={{ y: "100%" }} animate={{ y: 0 }} exit={{ y: "100%" }}
                            transition={{ type: "spring", damping: 25, stiffness: 200 }}
                            drag="y" dragConstraints={{ top: 0, bottom: 0 }} dragElastic={0.05}
                            onDragEnd={(event, info) => {
                                if (info.offset.y > 100 || info.velocity.y > 500) setSelectedTariff(null);
                            }}
                            className="fixed bottom-0 left-0 right-0 z-[160] mx-auto w-full max-w-[480px] bg-[#0a0a0f] border-t border-white/10 rounded-t-[2.5rem] shadow-[0_-20px_60px_rgba(0,0,0,0.8)] touch-none"
                        >
                            <div className="w-full pt-4 pb-6 flex justify-center">
                                <div className="w-12 h-1.5 bg-white/10 rounded-full" />
                            </div>

                            <div className="px-8 pb-12">
                                <div className="flex flex-col items-center mb-8 text-center">
                                    <div className={`w-20 h-20 rounded-[2rem] flex items-center justify-center border border-white/5 ${selectedTariff.bgGlow} mb-5 relative`}>
                                        <div className={`absolute inset-0 blur-xl opacity-50 ${selectedTariff.bgGlow} rounded-[2rem]`} />
                                        <selectedTariff.icon size={40} className={`${selectedTariff.color} relative z-10`} />
                                    </div>
                                    <h2 className="text-[32px] font-black tracking-tighter uppercase italic text-white leading-none mb-3">
                                        {selectedTariff.name}
                                    </h2>
                                    <p className="text-white/50 text-[14px] font-medium leading-relaxed max-w-[300px]">
                                        {selectedTariff.description}
                                    </p>
                                </div>

                                <div className="bg-white/5 border border-white/5 rounded-[2rem] p-6 mb-8 space-y-4">
                                    <div className="flex items-center gap-4">
                                        <selectedTariff.icon size={18} className={selectedTariff.color} />
                                        <span className="text-white font-bold text-[15px]">Лимит: {selectedTariff.devicesLabel}</span>
                                    </div>
                                    <div className="flex items-center gap-4">
                                        <Zap size={18} className="text-white/60" />
                                        <span className="text-white font-bold text-[15px]">Безлимитный трафик</span>
                                    </div>
                                    <div className="flex items-center gap-4 pt-4 border-t border-white/10">
                                        <Globe2 size={18} className="text-white/60" />
                                        <span className="text-white font-bold text-[15px]">Все локации открыты</span>
                                    </div>
                                </div>

                                <button
                                    onClick={() => { handlePay(); window.Telegram?.WebApp?.HapticFeedback.impactOccurred('heavy'); }}
                                    className="w-full bg-white text-black h-[70px] rounded-2xl flex items-center justify-between px-8 active:scale-[0.97] transition-all shadow-[0_0_20px_rgba(255,255,255,0.15)] mb-6"
                                >
                                    <span className="font-black italic uppercase text-[15px] tracking-wider">Оплатить</span>
                                    <span className="font-black text-[22px]">{selectedTariff.price} ₽</span>
                                </button>
                            </div>
                        </motion.div>
                    </>
                )}
            </AnimatePresence>
        </div>
    );
}