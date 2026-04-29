import React from 'react';
import { LayoutDashboard, ShoppingBag, UserCircle2, Trophy } from 'lucide-react';
import { useUserStore } from '../../store/userStore';

export default function BottomNav() {
    const { activeTab, setActiveTab, t } = useUserStore();

    const tabs = [
        { id: 'home'        as const, label: t.nav_vpn,     Icon: LayoutDashboard },
        { id: 'leaderboard' as const, label: t.nav_top,     Icon: Trophy },
        { id: 'payments'    as const, label: t.nav_tariffs, Icon: ShoppingBag },
        { id: 'profile'     as const, label: t.nav_profile, Icon: UserCircle2 },
    ];

    const handleTabChange = (id: 'home' | 'profile' | 'payments' | 'subscriptions') => {
        setActiveTab(id);
        window.Telegram?.WebApp?.HapticFeedback.impactOccurred('light');
    };

    return (
        <nav className="fixed bottom-0 left-0 right-0 z-50 px-6 pb-8 pt-2">
            <div className="glass-card !py-3 !px-4 flex justify-around items-center border-white/10 shadow-[0_-10px_40px_rgba(0,0,0,0.5)]">
                {tabs.map(({ id, label, Icon }) => {
                    const isActive = activeTab === id || (id === 'home' && activeTab === 'subscriptions');
                    return (
                        <button
                            key={id}
                            onClick={() => handleTabChange(id)}
                            className={`flex flex-col items-center gap-1.5 transition-all duration-300 relative ${
                                isActive ? 'text-primary scale-110' : 'text-muted-foreground/50 hover:text-muted-foreground'
                            }`}
                        >
                            <div className={isActive ? 'anim-pop relative' : ''}>
                                <Icon
                                    size={22}
                                    strokeWidth={isActive ? 2.5 : 2}
                                    className={isActive ? 'drop-shadow-[0_0_8px_rgba(255,255,255,0.5)]' : ''}
                                />
                                {isActive && (
                                    <div className="absolute -bottom-1 left-1/2 -translate-x-1/2 w-1 h-1 bg-primary rounded-full shadow-[0_0_10px_#fff]" />
                                )}
                            </div>
                            <span className={`text-[10px] font-black uppercase tracking-widest transition-opacity duration-300 ${isActive ? 'opacity-100' : 'opacity-40'}`}>
                                {label}
                            </span>
                        </button>
                    );
                })}
            </div>
        </nav>
    );
}
