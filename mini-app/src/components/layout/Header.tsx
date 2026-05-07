import React, { useState } from 'react';
import { Languages, BookOpen, Headphones, Settings, ChevronRight, Check } from 'lucide-react';
import { useUserStore } from '../../store/userStore';
import { Lang } from '../../utils/translations';

type MenuView = 'main' | 'lang';

// Вспомогательная функция для надежного открытия ссылок
const handleLink = (url: string) => {
    if (window.Telegram?.WebApp?.openTelegramLink) {
        window.Telegram.WebApp.openTelegramLink(url);
    } else {
        window.open(url, '_blank');
    }
};

export default function Header() {
    const { lang, setLanguage, t } = useUserStore(); // Достаем t для переводов
    const [open, setOpen] = useState(false);
    const [view, setView] = useState<MenuView>('main');

    // Полный список языков
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
        // Вибрация при смене языка
        window.Telegram?.WebApp?.HapticFeedback.impactOccurred('medium');
    };

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

                <div className="flex justify-end items-center">
                    <div className="relative">
                        <button
                            onClick={handleOpen}
                            className="w-9 h-9 bg-secondary/50 backdrop-blur-md rounded-full flex items-center justify-center border border-border active:scale-90 transition-all"
                        >
                            <Settings size={17} className="text-foreground" />
                        </button>

                        {open && (
                            <>
                                <div className="fixed inset-0 z-10" onClick={handleClose} />
                                <div className="absolute right-0 mt-3 bg-card border border-border rounded-2xl overflow-hidden shadow-2xl z-[110] w-56 animate-in fade-in zoom-in duration-200">

                                    {/* Главное меню */}
                                    {view === 'main' && (
                                        <>
                                            {/* Язык — открывает подменю */}
                                            <button
                                                onClick={() => setView('lang')}
                                                className="w-full px-4 py-3.5 flex items-center justify-between border-b border-border active:bg-muted transition-colors hover:bg-muted/50"
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

                                            {/* Инструкция */}
                                            <button
                                                onClick={() => {
                                                    handleLink('https://t.me/geovpn_news');
                                                    handleClose();
                                                }}
                                                className="w-full px-4 py-3.5 flex items-center gap-3 border-b border-border active:bg-muted transition-colors hover:bg-muted/50 text-left"
                                            >
                                                <BookOpen size={15} className="text-muted-foreground" />
                                                <span className="text-[13px] font-medium text-foreground">{t.instruction}</span>
                                            </button>

                                            {/* Поддержка */}
                                            <button
                                                onClick={() => {
                                                    handleLink('https://t.me/geo_vpn_support');
                                                    handleClose();
                                                }}
                                                className="w-full px-4 py-3.5 flex items-center gap-3 active:bg-muted transition-colors hover:bg-muted/50 text-left"
                                            >
                                                <Headphones size={15} className="text-muted-foreground" />
                                                <span className="text-[13px] font-medium text-foreground">{t.support}</span>
                                            </button>
                                        </>
                                    )}

                                    {/* Подменю языков */}
                                    {view === 'lang' && (
                                        <div className="max-h-[70vh] overflow-y-auto custom-scrollbar">
                                            {/* Заголовок подменю */}
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
                                                    className="w-full px-4 py-3.5 flex items-center justify-between border-b border-border last:border-none active:bg-muted transition-colors hover:bg-muted/50"
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