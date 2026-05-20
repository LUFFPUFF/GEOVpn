import React, { useEffect, useMemo } from 'react';
import { useUserStore } from '../../store/userStore';
import { Trophy, Medal, Award, Users, Star, Crown, Zap, TrendingUp, ChevronRight } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

export default function Leaderboard() {
    const { leaderboard, fetchLeaderboard, t, user, setActiveTab } = useUserStore();

    useEffect(() => {
        fetchLeaderboard();
    }, []);

    const topThree = useMemo(() => leaderboard.slice(0, 3), [leaderboard]);
    const others = useMemo(() => leaderboard.slice(3), [leaderboard]);
    const isWinnerDay = leaderboard.length > 0 && leaderboard[0].isWinner;

    const haptic = (s: 'light' | 'medium' = 'light') =>
        window.Telegram?.WebApp?.HapticFeedback.impactOccurred(s);

    const containerVariants = {
        hidden: { opacity: 0 },
        visible: { opacity: 1, transition: { staggerChildren: 0.1 } }
    };

    const itemVariants = {
        hidden: { y: 20, opacity: 0 },
        visible: { y: 0, opacity: 1 }
    };

    return (
        <div className="flex flex-col h-[85vh] relative overflow-y-auto custom-scrollbar pb-32 pt-2 px-3">

            <div className="absolute top-0 left-1/2 -translate-x-1/2 w-full h-[300px] bg-emerald-500/5 blur-[120px] pointer-events-none" />

            <motion.div
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                className="mb-8 text-center relative z-10"
            >
                <h2 className="text-[36px] font-black uppercase italic text-white tracking-tighter leading-none mb-2">
                    {t.hall_of_fame}
                </h2>
                <p className="text-emerald-500 font-bold text-[11px] uppercase tracking-[0.3em]">
                    {isWinnerDay ? t.winner_last_month : t.top_partners}
                </p>
            </motion.div>

            {leaderboard.length === 0 ? (
                <motion.div
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    className="text-center bg-white/5 border border-white/5 rounded-[2.5rem] p-12 mt-4"
                >
                    <div className="w-20 h-20 bg-white/5 rounded-full flex items-center justify-center mx-auto mb-6 border border-white/10 text-white/10">
                        <Star size={32} />
                    </div>
                    <h3 className="text-[20px] font-black text-white mb-2 uppercase italic">{t.leaderboard_empty}</h3>
                    <p className="text-[13px] text-white/30 px-6">{t.leaderboard_empty_desc}</p>
                </motion.div>
            ) : (
                <>
                    <div className="grid grid-cols-3 gap-2 items-end mb-8 relative z-10 min-h-[200px]">

                        <div className="flex flex-col items-center">
                            {topThree[1] ? (
                                <motion.div
                                    initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} transition={{ delay: 0.2 }}
                                    className="w-full flex flex-col items-center"
                                >
                                    <div className="relative mb-3">
                                        <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-slate-400/20 to-transparent border border-slate-400/30 flex items-center justify-center overflow-hidden">
                                            <span className="text-white font-black text-xl">{(topThree[1].firstName || 'U').charAt(0)}</span>
                                            <div className="absolute bottom-0 right-0 bg-slate-400 p-1 rounded-tl-lg">
                                                <Medal size={10} className="text-slate-900" />
                                            </div>
                                        </div>
                                    </div>
                                    <div className="bg-white/5 border border-white/10 rounded-2xl p-3 w-full text-center backdrop-blur-md">
                                        <p className="text-white font-black text-[11px] truncate mb-1">{topThree[1].firstName}</p>
                                        <p className="text-slate-400 font-mono text-[14px] font-black">{topThree[1].referralCount}</p>
                                    </div>
                                </motion.div>
                            ) : <div className="w-full h-16 bg-white/5 rounded-2xl border border-white/5 border-dashed opacity-10" />}
                        </div>

                        <div className="flex flex-col items-center relative z-20">
                            {topThree[0] ? (
                                <motion.div
                                    initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ type: "spring", bounce: 0.5 }}
                                    className="w-full flex flex-col items-center"
                                >
                                    <div className="absolute -top-8 left-1/2 -translate-x-1/2">
                                        <motion.div animate={{ y: [0, -5, 0] }} transition={{ repeat: Infinity, duration: 2 }}>
                                            <Crown size={32} className="text-amber-400 drop-shadow-[0_0_15px_rgba(251,191,36,0.5)]" />
                                        </motion.div>
                                    </div>
                                    <div className="relative mb-4">
                                        <div className="w-20 h-20 rounded-[2rem] bg-gradient-to-br from-amber-400/30 via-amber-400/5 to-transparent border-2 border-amber-400/50 flex items-center justify-center shadow-[0_0_30px_rgba(251,191,36,0.2)]">
                                            <span className="text-white font-black text-2xl">{(topThree[0].firstName || 'U').charAt(0)}</span>
                                        </div>
                                    </div>
                                    <div className="bg-amber-400 border border-amber-500 rounded-2xl p-4 w-full text-center shadow-lg shadow-amber-400/20">
                                        <p className="text-black font-black text-[13px] truncate mb-1">{topThree[0].firstName}</p>
                                        <div className="flex items-center justify-center gap-1">
                                            <Zap size={12} className="text-black fill-current" />
                                            <p className="text-black font-mono text-[18px] font-black">{topThree[0].referralCount}</p>
                                        </div>
                                    </div>
                                </motion.div>
                            ) : null}
                        </div>

                        <div className="flex flex-col items-center">
                            {topThree[2] ? (
                                <motion.div
                                    initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} transition={{ delay: 0.3 }}
                                    className="w-full flex flex-col items-center"
                                >
                                    <div className="relative mb-3">
                                        <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-orange-400/20 to-transparent border border-orange-400/30 flex items-center justify-center">
                                            <span className="text-white font-black text-xl">{(topThree[2].firstName || 'U').charAt(0)}</span>
                                            <div className="absolute bottom-0 right-0 bg-orange-400 p-1 rounded-tl-lg">
                                                <Award size={10} className="text-orange-900" />
                                            </div>
                                        </div>
                                    </div>
                                    <div className="bg-white/5 border border-white/10 rounded-2xl p-3 w-full text-center backdrop-blur-md">
                                        <p className="text-white font-black text-[11px] truncate mb-1">{topThree[2].firstName}</p>
                                        <p className="text-orange-400 font-mono text-[14px] font-black">{topThree[2].referralCount}</p>
                                    </div>
                                </motion.div>
                            ) : <div className="w-full h-16 bg-white/5 rounded-2xl border border-white/5 border-dashed opacity-10" />}
                        </div>
                    </div>

                    <motion.div
                        variants={containerVariants}
                        initial="hidden"
                        animate="visible"
                        className="space-y-2 relative z-10"
                    >
                        {others.map((partner, index) => {
                            const rank = index + 4;
                            const isMe = partner.username === user?.username;

                            return (
                                <motion.div
                                    key={partner.username || index}
                                    variants={itemVariants}
                                    whileTap={{ scale: 0.98 }}
                                    className={`group flex items-center justify-between p-4 rounded-2xl border transition-all duration-300 ${
                                        isMe
                                            ? 'bg-emerald-500/10 border-emerald-500/30 shadow-[0_0_20px_rgba(16,185,129,0.1)]'
                                            : 'bg-[#0d0e12] border-white/5 hover:border-white/10'
                                    }`}
                                >
                                    <div className="flex items-center gap-4 text-left">
                                        <div className="w-10 h-10 rounded-xl bg-white/5 flex items-center justify-center border border-white/5 group-hover:bg-white/10 transition-colors">
                                            <span className="text-[14px] font-mono font-black text-white/20 group-hover:text-white/40">
                                                {rank < 10 ? `0${rank}` : rank}
                                            </span>
                                        </div>
                                        <div>
                                            <div className="flex items-center gap-2">
                                                <h4 className="text-[14px] font-black text-white leading-none">
                                                    {partner.firstName}
                                                </h4>
                                                {isMe && (
                                                    <span className="text-[8px] bg-emerald-500 text-black px-1.5 py-0.5 rounded font-black uppercase">Вы</span>
                                                )}
                                            </div>
                                            {partner.username && (
                                                <p className="text-[10px] text-white/30 font-medium mt-1 uppercase tracking-widest">
                                                    @{partner.username}
                                                </p>
                                            )}
                                        </div>
                                    </div>

                                    <div className="flex items-center gap-3">
                                        <div className="text-right">
                                            <p className="text-[18px] font-black text-white leading-none font-mono">
                                                {partner.referralCount}
                                            </p>
                                            <p className="text-[8px] text-white/20 font-black uppercase tracking-widest mt-1">
                                                {t.friends}
                                            </p>
                                        </div>
                                        <ChevronRight size={14} className="text-white/10" />
                                    </div>
                                </motion.div>
                            );
                        })}
                    </motion.div>
                </>
            )}

            <motion.div
                initial={{ opacity: 0, y: 30 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.5 }}
                className="mt-8 p-6 bg-gradient-to-r from-emerald-500/20 to-blue-500/10 border border-emerald-500/20 rounded-[2.5rem] relative overflow-hidden text-left"
            >
                <div className="absolute -right-4 -bottom-4 opacity-10 rotate-12 text-white">
                    <Trophy size={100} />
                </div>
                <div className="relative z-10">
                    <h4 className="text-white font-black uppercase italic text-[18px] mb-1">Стань королём VPN</h4>
                    <p className="text-white/50 text-[11px] leading-snug mb-4">
                        Приглашай друзей, копи бонусы и занимай первое место в зале славы GEO VPN!
                    </p>
                    <button
                        onClick={() => { setActiveTab?.('profile'); haptic('medium'); }}
                        className="px-6 py-3 bg-white text-black rounded-xl font-black text-[11px] uppercase tracking-widest active:scale-95 transition-all"
                    >
                        Пригласить
                    </button>
                </div>
            </motion.div>

        </div>
    );
}