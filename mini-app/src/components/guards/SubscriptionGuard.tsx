import React from 'react';
import { useUserStore } from '../../store/userStore';
import { motion } from 'framer-motion';
import { Bell, ArrowRight, CheckCircle2, RefreshCcw } from 'lucide-react';

export default function SubscriptionGuard({ children }: { children: React.ReactNode }) {
    // todo Временно отключаем проверку подписки и сразу рендерим приложение
    return <>{children}</>;

    const { isMember, checkMembership, loading } = useUserStore();

    if (isMember) return <>{children}</>;

    return (
        <div className="fixed inset-0 z-[9999] bg-[#0a0a0f] flex flex-col items-center justify-center p-8 text-center">
            <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-64 h-64 bg-emerald-500/10 blur-[80px] rounded-full pointer-events-none" />

            <motion.div
                initial={{ scale: 0.9, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                className="relative z-10 w-full max-w-sm"
            >
                <div className="w-20 h-20 bg-emerald-500/10 border border-emerald-500/20 rounded-[2rem] flex items-center justify-center mx-auto mb-8 shadow-[0_0_30px_rgba(16,185,129,0.1)]">
                    <Bell size={40} className="text-emerald-500 animate-bounce" />
                </div>

                <h2 className="text-[28px] font-black text-white uppercase italic tracking-tighter leading-tight mb-4">
                    Почти готово!
                </h2>

                <p className="text-white/50 text-[15px] leading-relaxed mb-10 px-4">
                    Для использования GeoVPN необходимо быть подписанным на наш официальный канал. Там мы публикуем важные обновления и новые локации.
                </p>

                <div className="space-y-4">
                    <button
                        onClick={() => window.Telegram.WebApp.openTelegramLink('https://t.me/+yuKUzLhYdJVjOWRi')}
                        className="w-full py-5 bg-white text-black rounded-2xl font-black text-[14px] uppercase tracking-widest flex items-center justify-center gap-2 active:scale-[0.98] transition-all shadow-xl"
                    >
                        <span>Подписаться на канал</span>
                        <ArrowRight size={18} />
                    </button>

                    <button
                        onClick={checkMembership}
                        disabled={loading}
                        className="w-full py-4 bg-white/5 border border-white/10 text-white/60 rounded-2xl font-black text-[12px] uppercase tracking-widest active:bg-white/10 transition-all flex items-center justify-center gap-2"
                    >
                        {loading ? <RefreshCcw className="animate-spin" size={16} /> : <CheckCircle2 size={16} />}
                        <span>Я подписался</span>
                    </button>
                </div>
            </motion.div>
        </div>
    );
}