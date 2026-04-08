import React, { useEffect } from 'react';
import { useUserStore } from '../../store/userStore';
import { Trophy, Medal, Award, Users, Star } from 'lucide-react';

export default function Leaderboard() {
    const { leaderboard, fetchLeaderboard, t } = useUserStore();

    useEffect(() => {
        fetchLeaderboard();
    }, [fetchLeaderboard]);

    const isWinnerDay = leaderboard.length > 0 && leaderboard[0].isWinner;

    return (
        <div className="flex flex-col h-[78vh] relative overflow-y-auto custom-scrollbar pb-24 pt-2 px-1">

            <div className="mb-6 text-center">
                <h2 className="text-[32px] font-black uppercase italic text-white tracking-tighter leading-none mb-2">Зал славы</h2>
                <p className="text-white/40 text-[12px] font-bold uppercase tracking-widest">
                    {isWinnerDay ? 'Победитель прошлого месяца' : 'Топ партнеров за этот месяц'}
                </p>
            </div>

            {/* ЕСЛИ СЕГОДНЯ 1-Е ЧИСЛО И ЕСТЬ ПОБЕДИТЕЛЬ */}
            {isWinnerDay && leaderboard.length > 0 && (
                <div className="bg-gradient-to-b from-amber-500/20 to-[#12141d] border border-amber-500/30 rounded-[2rem] p-8 shadow-[0_0_40px_rgba(251,191,36,0.15)] relative overflow-hidden mb-6 text-center">
                    <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[200px] h-[100px] bg-amber-500/30 blur-[60px] pointer-events-none" />

                    <Trophy size={64} className="text-amber-400 mx-auto mb-4 drop-shadow-[0_0_15px_rgba(251,191,36,0.6)] animate-bounce" />

                    <h3 className="text-amber-400 text-[14px] font-black uppercase tracking-[0.2em] mb-1">Король VPN</h3>
                    <h2 className="text-[28px] font-black text-white leading-none mb-4">{leaderboard[0].firstName}</h2>

                    <div className="inline-flex items-center gap-2 bg-black/40 border border-white/10 px-4 py-2 rounded-xl">
                        <Users size={16} className="text-white/50" />
                        <span className="text-[16px] font-black text-white">{leaderboard[0].referralCount}</span>
                        <span className="text-[12px] font-bold text-white/50 uppercase">приглашений</span>
                    </div>
                </div>
            )}

            {/* ЖИВОЙ ЛИДЕРБОРД (Топ-10) */}
            {!isWinnerDay && (
                <div className="space-y-3">
                    {leaderboard.map((user, index) => {
                        // Цвета для топ-3
                        const isFirst = index === 0;
                        const isSecond = index === 1;
                        const isThird = index === 2;

                        let borderClass = "border-white/5";
                        let bgClass = "bg-[#12141d]";
                        let icon = <span className="font-black text-white/20 text-[18px]">#{index + 1}</span>;

                        if (isFirst) {
                            borderClass = "border-amber-500/30 shadow-[0_0_20px_rgba(251,191,36,0.1)]";
                            bgClass = "bg-gradient-to-r from-amber-500/10 to-[#12141d]";
                            icon = <Trophy size={24} className="text-amber-400 drop-shadow-md" />;
                        } else if (isSecond) {
                            borderClass = "border-slate-300/30 shadow-[0_0_15px_rgba(203,213,225,0.05)]";
                            bgClass = "bg-gradient-to-r from-slate-300/10 to-[#12141d]";
                            icon = <Medal size={24} className="text-slate-300 drop-shadow-md" />;
                        } else if (isThird) {
                            borderClass = "border-orange-400/30 shadow-[0_0_15px_rgba(251,146,60,0.05)]";
                            bgClass = "bg-gradient-to-r from-orange-400/10 to-[#12141d]";
                            icon = <Award size={24} className="text-orange-400 drop-shadow-md" />;
                        }

                        return (
                            <div key={index} className={`flex items-center justify-between p-5 rounded-[1.5rem] border ${bgClass} ${borderClass} transition-all`}>
                                <div className="flex items-center gap-4">
                                    <div className="w-12 h-12 rounded-full bg-black/40 border border-white/5 flex items-center justify-center shrink-0">
                                        {icon}
                                    </div>
                                    <div>
                                        <h4 className={`text-[16px] font-black leading-none mb-1 ${isFirst ? 'text-amber-400' : 'text-white'}`}>
                                            {user.firstName}
                                        </h4>
                                        {user.username && <p className="text-[11px] text-white/40 font-medium">@{user.username}</p>}
                                    </div>
                                </div>
                                <div className="text-right">
                                    <p className="text-[20px] font-black text-white leading-none">{user.referralCount}</p>
                                    <p className="text-[9px] text-white/30 font-black uppercase tracking-widest mt-1">друзей</p>
                                </div>
                            </div>
                        );
                    })}

                    {leaderboard.length === 0 && (
                        <div className="text-center bg-[#12141d] border border-white/5 rounded-[2rem] p-8 mt-10">
                            <Star size={40} className="text-white/20 mx-auto mb-4" />
                            <h3 className="text-[18px] font-black text-white mb-2">Пока пусто</h3>
                            <p className="text-[13px] text-white/40">Станьте первым, кто пригласит друзей в этом месяце!</p>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}