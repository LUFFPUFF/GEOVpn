import React, { useState } from 'react';
import { Languages, BookOpen, Headphones } from 'lucide-react';
import { useUserStore } from '../../store/userStore';

export default function Header() {
    const { lang, setLanguage, t } = useUserStore();
    const [showLangs, setShowLangs] = useState(false);

    const languages = [
        { id: 'ru', label: 'RU' },
        { id: 'tg', label: 'TG' },
        { id: 'uz', label: 'UZ' },
        { id: 'fa', label: 'FA' }
    ];

    return (
        <header className="relative z-[100] px-5 pt-6 pb-4">
            <div className="grid grid-cols-3 items-center">
                <div></div>
                <div className="flex justify-center">
                    <h1 className="text-[22px] tracking-[-0.05em] uppercase leading-none flex items-center">
                        <span className="font-black text-foreground">GEO</span>
                        <span className="font-light text-muted-foreground ml-0.5">VPN</span>
                    </h1>
                </div>


                <div className="flex justify-center items-center gap-1.5">


                    <div className="relative">
                        <button
                            onClick={() => setShowLangs(!showLangs)}
                            className="w-9 h-9 bg-secondary/50 backdrop-blur-md rounded-full flex items-center justify-center border border-border active:scale-90 transition-all"
                        >
                            <Languages size={17} className="text-foreground" />
                        </button>

                        {showLangs && (
                            <>
                                <div className="fixed inset-0 z-10" onClick={() => setShowLangs(false)} />
                                <div className="absolute right-0 mt-3 bg-card border border-border rounded-2xl overflow-hidden shadow-2xl z-[110] w-16 animate-in fade-in zoom-in duration-200">
                                    {languages.map(l => (
                                        <button
                                            key={l.id}
                                            onClick={() => { setLanguage(l.id); setShowLangs(false); }}
                                            className={`w-full py-3 text-[11px] font-bold border-b border-border last:border-none transition-colors ${lang === l.id ? 'bg-primary text-primary-foreground' : 'text-muted-foreground active:bg-muted'}`}
                                        >
                                            {l.label}
                                        </button>
                                    ))}
                                </div>
                            </>
                        )}
                    </div>


                    <button
                        className="w-9 h-9 bg-secondary/50 backdrop-blur-md rounded-full flex items-center justify-center border border-border active:scale-90 transition-all"
                        onClick={() => window.Telegram?.WebApp?.HapticFeedback.impactOccurred('light')}
                    >
                        <BookOpen size={17} className="text-foreground" />
                    </button>


                    <button
                        onClick={() => window.Telegram?.WebApp?.openTelegramLink('https://t.me/geovpn_support')}
                        className="w-9 h-9 bg-secondary/50 backdrop-blur-md rounded-full flex items-center justify-center border border-border active:scale-90 transition-all"
                    >
                        <Headphones size={17} className="text-foreground" />
                    </button>
                </div>
            </div>
        </header>
    );
}