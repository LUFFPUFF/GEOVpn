import React, { useState } from 'react';
import { Languages, BookOpen, Headphones, Settings, ChevronRight, Check } from 'lucide-react';
import { useUserStore } from '../../store/userStore';
import { Lang } from '../../utils/translations';

type MenuView = 'main' | 'lang';

const handleLink = (url: string) => {
    if (window.Telegram?.WebApp?.openTelegramLink) {
        window.Telegram.WebApp.openTelegramLink(url);
    } else {
        window.open(url, '_blank');
    }
};

export default function Header() {
    const { lang, setLanguage, t } = useUserStore();
    const [open, setOpen] = useState(false);
    const [view, setView] = useState<MenuView>('main');

    const languages: { id: Lang; label: string }[] = [
        { id: 'ru', label: 'Русский' },
        { id: 'en', label: 'English' },
        { id: 'tg', label: 'Тоҷикӣ' },
        { id: 'uz', label: "O'zbek" },
        { id: 'fa', label: 'فارسی' },
        { id: 'ar', label: 'العربية' },
        { id: 'tt', label: 'Татарча' }
    ];

    const handleOpen = () => {
        setView('main');
        setOpen(true);
    };

    const handleClose = () => {
        setOpen(false);
        setTimeout(() => setView('main'), 200);
    };

    const handleLangSelect = (id: Lang) => {
        setLanguage(id);
        handleClose();
        window.Telegram?.WebApp?.HapticFeedback.impactOccurred('medium');
    };

    return (
        <header
            className="relative z-[100] px-5 pb-4 flex-shrink-0"
            style={{ paddingTop: 'calc(1rem + env(safe-area-inset-top, 0px))' }}
        >
            <div className="grid grid-cols-3 items-center">
                <div />

                <div className="flex justify-center">
                    <h1 className="text-[22px] tracking-[-0.05em] uppercase leading-none flex items-center">
                        <span className="font-black text-foreground">GEO</span>
                        <span className="font-light text-muted-foreground ml-0.5">VPN</span>
                    </h1>
                </div>

                <div className="flex justify-end items-center">
                    <div className="relative">
                        <button
                            onClick={handleOpen}
                            className="w-9 h-9 bg-secondary/50 backdrop-blur-md rounded-full flex items-center justify-center border border-border active:scale-90 transition-all tap-target"
                        >
                            <Settings size={17} className="text-foreground" />
                        </button>

                        {open && (
                            <>
                                {/* Оверлей — внутри #root, не на весь viewport */}
                                <div
                                    className="absolute inset-0 z-10"
                                    style={{
                                        position: 'fixed',
                                        top: 0,
                                        left: 0,
                                        right: 0,
                                        bottom: 0,
                                    }}
                                    onClick={handleClose}
                                />
                                <div className="absolute right-0 mt-3 bg-card border border-border rounded-2xl overflow-hidden shadow-2xl z-[110] w-56 animate-in fade-in zoom-in-95 duration-200">

                                    {view === 'main' && (
                                        <>
                                            <button
                                                onClick={() => setView('lang')}
                                                className="w-full px-4 py-3.5 flex items-center justify-between border-b border-border active:bg-muted transition-colors"
                                            >
                                                <div className="flex items-center gap-3 text-left">
                                                    <Languages size={15} className="text-muted-foreground" />
                                                    <span className="text-[13px] font-medium text-foreground">{t.language}</span>
                                                </div>
                                                <div className="flex items-center gap-1.5">
                                                    <span className="text-[11px] font-bold text-emerald-500 uppercase">{lang}</span>
                                                    <ChevronRight size={13} className="text-muted-foreground" />
                                                </div>
                                            </button>

                                            <button
                                                onClick={() => { handleLink('https://t.me/geovpn_news'); handleClose(); }}
                                                className="w-full px-4 py-3.5 flex items-center gap-3 border-b border-border active:bg-muted transition-colors text-left"
                                            >
                                                <BookOpen size={15} className="text-muted-foreground" />
                                                <span className="text-[13px] font-medium text-foreground">{t.instruction}</span>
                                            </button>

                                            <button
                                                onClick={() => { handleLink('https://t.me/geo_vpn_support'); handleClose(); }}
                                                className="w-full px-4 py-3.5 flex items-center gap-3 active:bg-muted transition-colors text-left"
                                            >
                                                <Headphones size={15} className="text-muted-foreground" />
                                                <span className="text-[13px] font-medium text-foreground">{t.support}</span>
                                            </button>
                                        </>
                                    )}

                                    {view === 'lang' && (
                                        <div className="max-h-[60vh] overflow-y-auto custom-scrollbar">
                                            <button
                                                onClick={() => setView('main')}
                                                className="w-full px-4 py-3 flex items-center gap-2 border-b border-border sticky top-0 bg-card active:bg-muted transition-colors z-10"
                                            >
                                                <ChevronRight size={13} className="text-muted-foreground rotate-180" />
                                                <span className="text-[11px] font-semibold text-muted-foreground uppercase tracking-wide">{t.language}</span>
                                            </button>

                                            {languages.map(l => (
                                                <button
                                                    key={l.id}
                                                    onClick={() => handleLangSelect(l.id)}
                                                    className="w-full px-4 py-3.5 flex items-center justify-between border-b border-border last:border-none active:bg-muted transition-colors"
                                                >
                                                    <span className={`text-[13px] font-medium ${lang === l.id ? 'text-emerald-500' : 'text-foreground'}`}>
                                                        {l.label}
                                                    </span>
                                                    {lang === l.id && <Check size={14} className="text-emerald-500" />}
                                                </button>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            </>
                        )}
                    </div>
                </div>
            </div>
        </header>
    );
}