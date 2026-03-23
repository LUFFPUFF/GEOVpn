import React from 'react';
import { Shield, Activity, Smartphone, Globe, ChevronRight, Zap, ArrowUpRight } from 'lucide-react';
import { useUserStore } from '../../store/userStore';
import { formatBytes } from '../../utils/formatters';

export default function Home() {
    const { user } = useUserStore();

    return (
        <div className="px-6 pb-32 animate-in fade-in duration-700">

            {/* ГЛАВНЫЙ СТАТУС (Минимализм) */}
            <div className="vpn-card p-8 mb-5 relative overflow-hidden">
                <div className="flex justify-between items-start mb-12">
                    <div>
                        <h2 className="text-sm font-bold uppercase tracking-[0.2em] text-grey-muted mb-1">Status</h2>
                        <div className="flex items-center gap-2">
                            <div className={`w-2 h-2 rounded-full ${user.active ? 'bg-white shadow-[0_0_8px_#fff]' : 'bg-white/10'}`} />
                            <p className="text-base font-bold tracking-tight">
                                {user.active ? 'System Encrypted' : 'Protection Off'}
                            </p>
                        </div>
                    </div>
                    <Shield className="text-white/10" size={28} />
                </div>

                <div className="space-y-1">
                    <p className="text-[11px] font-bold uppercase tracking-[0.2em] text-grey-muted">Remaining Time</p>
                    <div className="flex items-baseline gap-2">
                        <span className="text-7xl font-light tracking-tighter">{user.daysLeft}</span>
                        <span className="text-lg font-bold uppercase tracking-tighter text-grey-muted">days</span>
                    </div>
                </div>
            </div>

            {/* ХАРАКТЕРИСТИКИ (Сетчатый вид) */}
            <div className="grid grid-cols-2 gap-4 mb-6">
                <div className="vpn-card p-5">
                    <Activity size={18} className="text-white/40 mb-4" />
                    <p className="text-[10px] font-bold uppercase tracking-widest text-grey-muted mb-1">Data Usage</p>
                    <p className="text-xl font-bold tracking-tight">{formatBytes(user.trafficUsed)}</p>
                </div>

                <div className="vpn-card p-5">
                    <Smartphone size={18} className="text-white/40 mb-4" />
                    <p className="text-[10px] font-bold uppercase tracking-widest text-grey-muted mb-1">Nodes</p>
                    <p className="text-xl font-bold tracking-tight">{user.devicesCount} / 5</p>
                </div>

                <button className="vpn-card p-5 col-span-2 flex items-center justify-between active:bg-white/[0.05] transition-all">
                    <div className="flex items-center gap-4">
                        <div className="w-10 h-10 rounded-full bg-white/5 flex items-center justify-center">
                            <Globe size={20} className="text-white/60" />
                        </div>
                        <div className="text-left">
                            <p className="text-[10px] font-bold uppercase tracking-widest text-grey-muted">Access Point</p>
                            <p className="text-sm font-bold uppercase tracking-tight">{user.currentLocation}</p>
                        </div>
                    </div>
                    <ArrowUpRight className="text-white/20" size={18} />
                </button>
            </div>

            {/* ОСНОВНАЯ КНОПКА (High Contrast) */}
            <button className="w-full btn-premium py-5 text-sm uppercase tracking-[0.2em] flex items-center justify-center gap-3 shadow-2xl">
                <Zap size={18} fill="currentColor" />
                Initialize Protocol
            </button>
        </div>
    );
}