import React from 'react';
import { LayoutDashboard, ShoppingBag, UserCircle2, Trophy } from 'lucide-react';
import { useUserStore } from '../../store/userStore';
import type { TabId } from '../../store/userStore';

export default function BottomNav() {
    const { activeTab, setActiveTab, t } = useUserStore();

    const tabs: { id: TabId; label: string; Icon: React.ElementType }[] = [
        { id: 'home',        label: t.nav_vpn,     Icon: LayoutDashboard },
        { id: 'leaderboard', label: t.nav_top,     Icon: Trophy          },
        { id: 'payments',    label: t.nav_tariffs, Icon: ShoppingBag     },
        { id: 'profile',     label: t.nav_profile, Icon: UserCircle2     },
    ];

    return (
        <nav className="bottom-nav-container">
            <div className="glass-card !py-3 !px-4 flex justify-around items-center border-white/10 shadow-[0_-10px_40px_rgba(0,0,0,0.5)]">
                {tabs.map(({ id, label, Icon }) => {
                    const isActive = activeTab === id || (id === 'home' && activeTab === 'subscriptions');
                    return (
                        <button
                            key={id}
                            onClick={() => { setActiveTab(id); window.Telegram?.WebApp?.HapticFeedback.impactOccurred('light'); }}
                            className={`flex flex-col items-center gap-1 transition-all duration-300 relative tap-target px-3 ${
                                isActive ? 'text-primary scale-110' : 'text-muted-foreground/50'
                            }`}
                        >
                            <div className={isActive ? 'anim-pop relative' : 'relative'}>
                                <Icon size={22} strokeWidth={isActive ? 2.5 : 2}
                                      className={isActive ? 'drop-shadow-[0_0_8px_rgba(255,255,255,0.5)]' : ''} />
                                {isActive && (
                                    <div className="absolute -bottom-1 left-1/2 -translate-x-1/2 w-1 h-1 bg-primary rounded-full shadow-[0_0_10px_#fff]" />
                                )}
                            </div>
                            <span className={`text-[9px] font-black uppercase tracking-widest ${isActive ? 'opacity-100' : 'opacity-40'}`}>
                                {label}
                            </span>
                        </button>
                    );
                })}
            </div>
        </nav>
    );
}
