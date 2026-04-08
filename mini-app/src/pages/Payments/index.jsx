import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useUserStore } from '../../store/userStore';
import {
    Zap, Crown, Wallet, CheckCircle2,
    Globe2, Gauge, Lock, X
} from 'lucide-react';

export default function Payments() {
    const { user, t } = useUserStore();
    const [selectedTariff, setSelectedTariff] = useState(null);
    const realBalance = user?.balance ? (user.balance / 100).toFixed(0) : '0';

    const tariffs = [
        {
            id: 'basic',
            name: 'Стандарт',
            duration: '1 Месяц',
            price: 150,
            icon: Zap,
            color: 'text-blue-400',
            description: 'Идеально для повседневного серфинга. Стабильное соединение без лишних переплат.',
            features: ['Скорость до 100 Мбит/с', '10 базовых локаций']
        },
        {
            id: 'premium',
            name: 'Премиум PRO',
            duration: '6 Месяцев',
            price: 750,
            isHit: true,
            icon: Crown,
            color: 'text-amber-400',
            description: 'Максимальный уровень свободы. Стриминг в 4K и полная анонимность.',
            features: ['Скорость до 1 Гбит/с', '28+ премиум локаций']
        }
    ];

    return (
        <div className="space-y-6 pt-2 pb-24 relative select-none">
            <div className="bg-card border border-border rounded-[2rem] p-7 shadow-2xl flex justify-between items-center relative overflow-hidden">
                <div className="relative z-10">
                    <p className="text-muted-foreground text-[11px] font-black uppercase tracking-[0.2em] mb-2">{t.balance}</p>
                    <h2 className="text-[40px] font-black tracking-tighter text-foreground leading-none">
                        {realBalance} <span className="text-[20px] font-bold text-muted-foreground ml-1">₽</span>
                    </h2>
                </div>
                <button className="bg-primary text-primary-foreground px-6 py-4 rounded-2xl font-black text-[13px] uppercase active:scale-95 transition-transform shadow-xl">
                    Пополнить
                </button>
            </div>

            <h3 className="text-[22px] font-black px-1 mt-8 mb-4 text-foreground uppercase tracking-tighter italic leading-none">
                Выбор тарифа
            </h3>

            <div className="space-y-4">
                {tariffs.map((tariff) => (
                    <div
                        key={tariff.id}
                        onClick={() => setSelectedTariff(tariff)}
                        className="glass-card !p-6 flex justify-between items-center relative overflow-hidden cursor-pointer border-border active:scale-[0.98] transition-transform"
                    >
                        <div className="flex items-center gap-5">
                            <div className="w-14 h-14 rounded-[20px] bg-muted flex items-center justify-center border border-border">
                                <tariff.icon size={28} className={tariff.color} />
                            </div>
                            <div>
                                <h4 className="font-black text-[19px] text-foreground uppercase italic tracking-tighter">{tariff.name}</h4>
                                <p className="text-muted-foreground text-[11px] font-bold uppercase tracking-wider">{tariff.duration}</p>
                            </div>
                        </div>
                        <p className="font-black text-[22px] text-foreground">{tariff.price} ₽</p>
                    </div>
                ))}
            </div>

            <AnimatePresence>
                {selectedTariff && (
                    <>
                        <motion.div
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            exit={{ opacity: 0 }}
                            onClick={() => setSelectedTariff(null)}
                            className="fixed inset-0 bg-black/90 backdrop-blur-sm z-[150]"
                        />

                        <motion.div
                            initial={{ y: "100%" }}
                            animate={{ y: 0 }}
                            exit={{ y: "100%" }}
                            transition={{ type: "spring", damping: 25, stiffness: 200 }}

                            drag="y"
                            dragConstraints={{ top: 0, bottom: 0 }}
                            dragElastic={0.05}
                            onDragEnd={(event, info) => {
                                // Если скорость свайпа высокая или расстояние > 100px — закрываем
                                if (info.offset.y > 100 || info.velocity.y > 500) {
                                    setSelectedTariff(null);
                                }
                            }}
                            className="fixed bottom-0 left-0 right-0 z-[160] mx-auto w-full max-w-[480px] bg-card border-t border-border rounded-t-[2.5rem] shadow-[0_-20px_60px_rgba(0,0,0,0.8)] touch-none"
                        >
                            <div className="w-full pt-4 pb-6 flex justify-center">
                                <div className="w-12 h-1.5 bg-muted-foreground/30 rounded-full" />
                            </div>

                            <div className="px-8 pb-12">
                                <div className="flex flex-col items-center mb-8 text-center">
                                    <selectedTariff.icon size={56} className={`${selectedTariff.color} mb-6`} />
                                    <h2 className="text-[32px] font-black tracking-tighter uppercase italic text-foreground leading-none mb-4">
                                        {selectedTariff.name}
                                    </h2>
                                    <p className="text-muted-foreground text-[14px] font-medium leading-relaxed max-w-[300px]">
                                        {selectedTariff.description}
                                    </p>
                                </div>

                                <div className="bg-muted/30 border border-border rounded-[2rem] p-7 mb-10 space-y-4">
                                    {selectedTariff.features.map((feat, i) => (
                                        <div key={i} className="flex items-center gap-4">
                                            <CheckCircle2 size={16} className="text-primary" />
                                            <span className="text-foreground font-bold text-[15px]">{feat}</span>
                                        </div>
                                    ))}
                                    <div className="flex items-center gap-4 pt-4 border-t border-border/50">
                                        <Globe2 size={16} className="text-primary" />
                                        <span className="text-foreground font-bold text-[15px]">Все страны (NL, DE, US, TR...)</span>
                                    </div>
                                </div>

                                <button className="w-full bg-foreground text-background h-[76px] rounded-[24px] flex items-center justify-between px-8 active:scale-[0.97] transition-all shadow-2xl mb-6">
                                    <span className="font-black italic uppercase text-[15px]">Оплатить</span>
                                    <span className="font-black text-[22px]">{selectedTariff.price} ₽</span>
                                </button>

                                <p className="text-center text-[10px] text-muted-foreground font-bold uppercase tracking-widest opacity-30">
                                    Свайпните вниз, чтобы закрыть
                                </p>
                            </div>
                        </motion.div>
                    </>
                )}
            </AnimatePresence>
        </div>
    );
}