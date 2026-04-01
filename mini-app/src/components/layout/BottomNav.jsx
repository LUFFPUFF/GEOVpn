import React, { useRef } from 'react';
import { LayoutDashboard, ServerCrash, ShoppingCart, Settings } from 'lucide-react';

export default function BottomNav({ activeTab, setActiveTab }) {
    const tabs = [
        { id: 'home', icon: LayoutDashboard, label: 'ДАШБОРД', animClass: 'anim-pop' },
        { id: 'subscriptions', icon: ServerCrash, label: 'НОДЫ', animClass: 'anim-jump' },
        { id: 'payments', icon: ShoppingCart, label: 'МАГАЗИН', animClass: 'anim-wobble' },
        { id: 'profile', icon: Settings, label: 'НАСТРОЙКИ', animClass: 'anim-spin' }
    ];

    const activeIndex = tabs.findIndex(tab => tab.id === activeTab);
    const navRef = useRef(null);

    const handlePointerMove = (e) => {
        if (e.buttons !== 1 && e.pointerType !== 'touch') return;
        if (!navRef.current) return;
        const rect = navRef.current.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const width = rect.width;
        let index = Math.floor((x / width) * tabs.length);
        index = Math.max(0, Math.min(index, tabs.length - 1));
        if (tabs[index].id !== activeTab) setActiveTab(tabs[index].id);
    };

    return (
        <nav className="fixed bottom-0 left-0 right-0 z-50 bg-background border-t border-border pb-safe select-none" style={{ touchAction: 'none' }}>
            <div
                ref={navRef}
                className="relative w-full max-w-[480px] mx-auto h-[64px] cursor-pointer"
                onPointerDown={handlePointerMove}
                onPointerMove={handlePointerMove}
            >
                {}
                <div className="absolute inset-0 flex">
                    {tabs.map(tab => (
                        <div key={`bg-${tab.id}`} className="flex-1 flex flex-col items-center justify-center">
                            <tab.icon size={22} strokeWidth={2} className="text-muted-foreground/30 mb-0.5" />
                            <span className="text-[9px] font-bold text-muted-foreground/30 tracking-widest">{tab.label}</span>
                        </div>
                    ))}
                </div>

                {}
                {}
                <div
                    className="absolute top-1 bottom-1 w-1/4 p-1 pointer-events-none transition-transform duration-300 ease-[cubic-bezier(0.4,0,0.2,1)]"
                    style={{ transform: `translateX(${activeIndex * 100}%)` }}
                >
                    <div className="w-full h-full bg-secondary rounded-xl border border-white/10 shadow-lg overflow-hidden relative">
                        {}
                        <div
                            className="absolute top-0 left-0 h-full w-[400%] flex transition-transform duration-300 ease-[cubic-bezier(0.4,0,0.2,1)]"
                            style={{ transform: `translateX(-${activeIndex * 25}%)` }}
                        >
                            {tabs.map(tab => {
                                const isActive = activeTab === tab.id;
                                return (
                                    <div key={`active-${tab.id}`} className="w-1/4 h-full flex flex-col items-center justify-center">
                                        <div key={isActive ? 'a' : 'i'} className={isActive ? tab.animClass : ''}>
                                            <tab.icon size={22} strokeWidth={2.5} className="text-foreground mb-0.5" />
                                        </div>
                                        <span className="text-[9px] font-black text-foreground tracking-widest">{tab.label}</span>
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                </div>
            </div>
        </nav>
    );
}