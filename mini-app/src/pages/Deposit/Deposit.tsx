import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { useUserStore } from "../../store/userStore";
import { billingApi } from "../../api/billing";
import { CreditCard, ChevronLeft, Wallet, Zap } from 'lucide-react';

const QUICK_AMOUNTS = [75, 150, 350, 1000];

export default function Deposit() {
    const { user, t, lang, setActiveTab } = useUserStore();
    const [amount, setAmount] = useState<string>('');
    const [loading, setLoading] = useState(false);

    const isRTL = lang === 'ar' || lang === 'fa';
    const realBalance = user?.balance ? (user.balance / 100).toFixed(0) : '0';

    useEffect(() => {
        const tg = window.Telegram?.WebApp;
        if (tg) {
            tg.ready();
            tg.BackButton.show();
            const handleBack = () => {
                haptic('light');
                setActiveTab('payments');
            };
            tg.BackButton.onClick(handleBack);
            return () => {
                tg.BackButton.offClick(handleBack);
                tg.BackButton.hide();
            };
        }
    }, [setActiveTab]);

    const haptic = (type: any = 'medium') =>
        window.Telegram?.WebApp?.HapticFeedback?.impactOccurred(type);

    const handleAmountClick = (val: number) => {
        haptic('light');
        setAmount(val.toString());
    };

    const handlePay = async () => {
        const numAmount = parseInt(amount);
        if (!numAmount || numAmount < 10) {
            window.Telegram?.WebApp?.showAlert('Минимальная сумма пополнения — 10 ₽');
            haptic('error');
            return;
        }

        haptic('heavy');
        setLoading(true);
        try {
            const response = await billingApi.createDeposit(numAmount);

            console.log("Ответ от биллинга:", response);

            if (response && response.paymentUrl) {
                console.log("Переходим по ссылке:", response.paymentUrl);

                if (window.Telegram?.WebApp?.initData) {
                    window.Telegram.WebApp.openLink(response.paymentUrl);
                } else {
                    window.open(response.paymentUrl, '_blank');
                }
                setActiveTab('payments');
            } else {
                console.error("Ссылка paymentUrl отсутствует в ответе!");
            }
        } catch (error: any) {
            const serverMsg = error?.response?.data?.error?.message;
            window.Telegram?.WebApp?.showAlert(`Ошибка: ${serverMsg || 'Не удалось создать платеж'}`);
        } finally {
            setLoading(false);
        }
    };

    return (
        <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            className={`space-y-6 pt-2 pb-6 ${isRTL ? 'text-right' : 'text-left'}`}
        >
            <div className={`flex items-center gap-3 ${isRTL ? 'flex-row-reverse' : ''}`}>
                <button
                    onClick={() => { haptic('light'); setActiveTab('payments'); }}
                    className="w-10 h-10 bg-white/5 border border-white/10 rounded-full flex items-center justify-center active:scale-90 transition-transform"
                >
                    <ChevronLeft size={20} className="text-white" />
                </button>
                <h2 className="text-[22px] font-black text-white tracking-tight">Пополнение</h2>
            </div>

            <div className={`bg-[#12141d] border border-white/10 rounded-3xl p-5 flex items-center gap-4 ${isRTL ? 'flex-row-reverse' : ''}`}>
                <div className="w-12 h-12 bg-blue-500/10 rounded-2xl flex items-center justify-center border border-blue-500/20 shrink-0">
                    <Wallet size={24} className="text-blue-400" />
                </div>
                <div>
                    <p className="text-white/40 text-[10px] font-black uppercase tracking-[0.2em] mb-0.5">Твой баланс</p>
                    <p className="text-[24px] font-black tracking-tighter text-white leading-none">
                        {realBalance} <span className="text-[14px] text-white/50">₽</span>
                    </p>
                </div>
            </div>

            <div className="space-y-4">
                <p className="text-white/50 text-[12px] font-black uppercase tracking-widest px-2">Сумма пополнения (₽)</p>

                <div className="relative">
                    <div className="absolute inset-y-0 left-5 flex items-center pointer-events-none">
                        <span className="text-white/30 font-black text-[24px]">₽</span>
                    </div>
                    <input
                        type="number"
                        inputMode="numeric"
                        value={amount}
                        onChange={(e) => setAmount(e.target.value)}
                        placeholder="0"
                        className="w-full bg-[#0f1117] border border-white/10 rounded-[1.5rem] h-[72px] pl-12 pr-6 text-[28px] font-black text-white placeholder-white/10 focus:outline-none focus:border-blue-500/50 focus:bg-white/5 transition-all"
                    />
                </div>

                <div className="grid grid-cols-4 gap-2">
                    {QUICK_AMOUNTS.map((val) => (
                        <button
                            key={val}
                            onClick={() => handleAmountClick(val)}
                            className={`h-[48px] rounded-2xl font-black text-[14px] transition-all border ${
                                amount === val.toString()
                                    ? 'bg-blue-500 text-white border-blue-500 shadow-[0_0_15px_rgba(59,130,246,0.3)]'
                                    : 'bg-white/5 text-white/70 border-white/5 active:bg-white/10'
                            }`}
                        >
                            {val}
                        </button>
                    ))}
                </div>
            </div>

            <div className={`p-4 rounded-3xl bg-blue-500/5 border border-blue-500/10 flex items-start gap-3 ${isRTL ? 'flex-row-reverse' : ''}`}>
                <Zap size={18} className="text-blue-400 shrink-0 mt-0.5" />
                <p className="text-white/50 text-[11px] font-medium leading-relaxed">
                    Сумма будет моментально зачислена на твой баланс после успешной оплаты. Мы принимаем СБП, карты и крипту.
                </p>
            </div>

            <button
                onClick={handlePay}
                disabled={loading || !amount || parseInt(amount) < 10}
                className="w-full h-[60px] bg-white text-black rounded-[1.5rem] flex items-center justify-center gap-2 font-black uppercase text-[15px] tracking-wide active:scale-[0.98] transition-all disabled:opacity-30 disabled:active:scale-100 shadow-[0_0_20px_rgba(255,255,255,0.1)] mt-4"
            >
                {loading ? (
                    <span className="animate-pulse">Создаем платеж...</span>
                ) : (
                    <>
                        <CreditCard size={20} />
                        Оплатить {amount ? `${amount} ₽` : ''}
                    </>
                )}
            </button>
        </motion.div>
    );
}