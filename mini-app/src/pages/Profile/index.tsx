import React, { useState } from 'react';
import { useUserStore } from '../../store/userStore';
import {
    Smartphone, Laptop, Plus, Trash2, Share2,
    ChevronRight, Newspaper, Headphones, BookOpen,
    ShieldAlert, User, ShieldCheck, Fingerprint,
    Crown, CheckCircle2, ArrowLeft
} from 'lucide-react';

// Вспомогательная функция для надежного открытия ссылок
const handleLink = (url: string) => {
    if (window.Telegram?.WebApp?.openTelegramLink) {
        window.Telegram.WebApp.openTelegramLink(url);
    } else {
        window.open(url, '_blank');
    }
};

export default function Profile() {
    const { user, devices, addDevice, deleteDevice, t } = useUserStore();
    const [subPage, setSubPage] = useState<'main' | 'referral' | 'instructions' | 'rules'>('main');
    const [showDeviceModal, setShowDeviceModal] = useState(false);
    const [devName, setDevName] = useState('');
    const [devType, setDevType] = useState('IOS');
    const [activeInstruction, setActiveInstruction] = useState<'ios' | 'android' | 'windows' | null>(null);

    const tgUser = window.Telegram?.WebApp?.initDataUnsafe?.user;
    const avatarUrl = tgUser?.photo_url;
    const initials = user?.firstName ? user.firstName.charAt(0).toUpperCase() : 'U';
    const realBalance = user?.balance ? (user.balance / 100).toFixed(0) : '0';
    const inviteLink = `https://t.me/geovpn_bot?start=${user?.referralCode}`;

    const copyAction = (text: string | undefined, message: string) => {
        if (!text) return;
        navigator.clipboard.writeText(text);
        window.Telegram?.WebApp?.HapticFeedback.notificationOccurred('success');
        window.Telegram?.WebApp?.showAlert(message);
    };

    const handleBack = () => {
        if (activeInstruction) setActiveInstruction(null);
        else setSubPage('main');
        window.Telegram?.WebApp?.HapticFeedback.impactOccurred('light');
    };

    // ─── Реферальная страница ───────────────────────────────────────────────
    if (subPage === 'referral') {
        const progress = 3;
        const goal = 10;
        const percent = (progress / goal) * 100;

        return (
            <div className="flex flex-col animate-in fade-in duration-300 overflow-y-auto custom-scrollbar pb-28 pt-2 px-1">
                <button onClick={handleBack} className="flex items-center gap-2 text-white/40 mb-4 font-black text-[10px] uppercase tracking-widest px-2">
                    <ArrowLeft size={14} /> {t.back}
                </button>

                <div className="bg-[#12141d] border border-white/10 rounded-[2rem] p-6 mb-6 shadow-2xl relative overflow-hidden">
                    <div className="absolute top-0 right-0 w-32 h-32 bg-emerald-500/10 blur-[50px] rounded-full pointer-events-none" />
                    <h2 className="text-[22px] font-black text-white uppercase italic mb-5">{t.referral_title}</h2>

                    <div className="space-y-4 mb-6">
                        <div className="flex items-start gap-3">
                            <div className="w-5 h-5 rounded-full bg-emerald-500/20 flex items-center justify-center shrink-0 mt-0.5">
                                <CheckCircle2 size={12} className="text-emerald-500" />
                            </div>
                            <p className="text-[13px] text-white/70 leading-snug">{t.referral_per_friend} <span className="text-white font-black">50₽</span> {t.referral_per_friend2}</p>
                        </div>
                        <div className="flex items-start gap-3">
                            <div className="w-5 h-5 rounded-full bg-emerald-500/20 flex items-center justify-center shrink-0 mt-0.5">
                                <CheckCircle2 size={12} className="text-emerald-500" />
                            </div>
                            <p className="text-[13px] text-white/70 leading-snug">{t.referral_bonus} <span className="text-emerald-500 font-black">500₽</span>.</p>
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-2 mb-6">
                        <div className="bg-black/40 p-4 rounded-2xl border border-white/5">
                            <p className="text-[9px] text-white/30 font-black uppercase mb-1">{t.referral_clicked}</p>
                            <p className="text-[20px] font-black text-white">12</p>
                        </div>
                        <div className="bg-black/40 p-4 rounded-2xl border border-white/5">
                            <p className="text-[9px] text-white/30 font-black uppercase mb-1">{t.referral_bought}</p>
                            <p className="text-[20px] font-black text-white">{progress}</p>
                        </div>
                    </div>

                    <div className="bg-emerald-500/5 border border-emerald-500/20 rounded-2xl p-4 mb-6">
                        <div className="flex justify-between items-end mb-2.5">
                            <p className="text-[11px] font-black text-white uppercase italic">{t.referral_progress}</p>
                            <p className="text-[11px] font-black text-emerald-500">{progress}/{goal}</p>
                        </div>
                        <div className="w-full h-2.5 bg-white/5 rounded-full overflow-hidden border border-white/5 p-0.5">
                            <div className="h-full bg-emerald-500 rounded-full transition-all duration-1000" style={{ width: `${percent}%` }} />
                        </div>
                    </div>

                    <button
                        onClick={() => copyAction(inviteLink, t.link_copied)}
                        className="w-full py-4 bg-emerald-500 text-white rounded-xl font-black text-[13px] uppercase tracking-widest active:scale-[0.98] transition-all flex items-center justify-center gap-2"
                    >
                        <Share2 size={16} /> {t.referral_invite}
                    </button>
                </div>
            </div>
        );
    }

    // ─── Страница инструкций ────────────────────────────────────────────────
    if (subPage === 'instructions') {
        const platforms = [
            { id: 'ios',     name: 'iOS',     icon: Smartphone, desc: t.ios_desc },
            { id: 'android', name: 'Android', icon: Smartphone, desc: t.android_desc },
            { id: 'windows', name: 'Windows', icon: Laptop,     desc: t.windows_desc }
        ];

        return (
            <div className="flex flex-col animate-in fade-in duration-300 overflow-y-auto custom-scrollbar pb-28 pt-2 px-1">
                <button onClick={handleBack} className="flex items-center gap-2 text-white/40 mb-4 font-black text-[10px] uppercase tracking-widest px-2">
                    <ArrowLeft size={14} /> {t.back}
                </button>

                {!activeInstruction ? (
                    <div className="space-y-2 px-1">
                        <h2 className="text-[22px] font-black text-white uppercase italic mb-4">{t.instructions_title}</h2>
                        {platforms.map(p => (
                            <button
                                key={p.id}
                                onClick={() => { setActiveInstruction(p.id as any); window.Telegram?.WebApp?.HapticFeedback.impactOccurred('light'); }}
                                className="w-full flex items-center justify-between bg-[#12141d] border border-white/10 p-4 rounded-2xl active:bg-white/5 transition-all"
                            >
                                <div className="flex items-center gap-4">
                                    <div className="w-10 h-10 rounded-full bg-white/5 flex items-center justify-center border border-white/10 shrink-0">
                                        <p.icon size={18} className="text-white/80" />
                                    </div>
                                    <div className="text-left">
                                        <p className="text-[15px] font-black text-white">{p.name}</p>
                                        <p className="text-[10px] text-white/40 font-medium uppercase">{p.desc}</p>
                                    </div>
                                </div>
                                <ChevronRight size={16} className="text-white/20" />
                            </button>
                        ))}
                    </div>
                ) : (
                    <div className="px-2 animate-in slide-in-from-right-4 duration-300">
                        <h2 className="text-[20px] font-black text-white uppercase italic mb-5">{t.setup_title} {activeInstruction.toUpperCase()}</h2>
                        <div className="space-y-4">
                            {[
                                { step: 1, title: t.step_download },
                                { step: 2, title: t.step_import }
                            ].map(s => (
                                <div key={s.step} className="bg-[#12141d] border border-white/10 rounded-2xl p-5">
                                    <div className="flex items-center gap-3 mb-4">
                                        <div className="w-7 h-7 rounded-full bg-white text-black flex items-center justify-center font-black text-sm">{s.step}</div>
                                        <p className="text-sm font-bold text-white">{s.title}</p>
                                    </div>
                                    <div className="w-full aspect-video bg-black/40 rounded-xl border border-white/5" />
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        );
    }

    // ─── Главная страница профиля ───────────────────────────────────────────
    return (
        <div className="flex flex-col overflow-y-auto custom-scrollbar pb-28 pt-2 px-1 animate-in fade-in duration-500">

            {/* ШАПКА */}
            <div className="bg-[#12141d] border border-white/10 rounded-[2rem] p-5 mb-3 shadow-xl relative overflow-hidden shrink-0">
                <div className="absolute top-0 left-0 w-full h-0.5 bg-gradient-to-r from-emerald-500/50 to-transparent" />
                <div className="flex items-center justify-between mb-5">
                    <div className="flex items-center gap-3">
                        {avatarUrl ? (
                            <img src={avatarUrl} alt="avatar" className="w-12 h-12 rounded-full border border-white/10 shrink-0 object-cover" />
                        ) : (
                            <div className="w-12 h-12 rounded-full bg-white/5 border border-white/10 flex items-center justify-center shrink-0">
                                <span className="text-lg font-black text-white">{initials}</span>
                            </div>
                        )}
                        <div>
                            <h2 className="text-[18px] font-black text-white leading-tight">{user?.firstName}</h2>
                            <div className="flex items-center gap-1.5 mt-0.5">
                                <ShieldCheck size={12} className="text-emerald-500" />
                                <span className="text-emerald-500 text-[9px] font-black uppercase tracking-widest">{t.protection_100}</span>
                            </div>
                        </div>
                    </div>
                    <div className="w-10 h-10 bg-white/5 rounded-xl border border-white/10 flex items-center justify-center relative overflow-hidden shrink-0">
                        <div className="absolute top-0 left-0 w-full h-[1px] bg-emerald-500 animate-[slide-in-from-bottom-full_2s_infinite_alternate]" />
                        <Fingerprint size={20} className="text-white/20" />
                    </div>
                </div>
                <div className="flex justify-between items-center bg-black/20 p-3 rounded-xl border border-white/5">
                    <div className="flex items-center gap-2 text-white/40">
                        <User size={14} />
                        <span className="text-[10px] font-bold uppercase tracking-widest">Telegram ID</span>
                    </div>
                    <span className="text-[13px] font-mono font-bold text-white">{user?.telegramId}</span>
                </div>
            </div>

            {/* БАЛАНС И СТАТУС */}
            <div className="grid grid-cols-2 gap-2 mb-3 shrink-0">
                <div className="bg-[#12141d] border border-white/10 rounded-2xl p-4 shadow-lg">
                    <p className="text-white/40 text-[9px] font-black uppercase tracking-widest mb-1">{t.balance_label}</p>
                    <div className="flex items-baseline gap-1">
                        <h3 className="text-[22px] font-black text-white leading-none">{realBalance}</h3>
                        <span className="text-emerald-500 text-sm font-bold">₽</span>
                    </div>
                </div>
                <div className="bg-[#12141d] border border-white/10 rounded-2xl p-4 shadow-lg">
                    <p className="text-white/40 text-[9px] font-black uppercase tracking-widest mb-1">{t.status}</p>
                    <h3 className={`text-[15px] font-black uppercase italic leading-none mt-1 truncate ${user?.hasActiveSubscription ? 'text-amber-400' : 'text-white/60'}`}>
                        {user?.hasActiveSubscription ? user.subscriptionType : 'INACTIVE'}
                    </h3>
                </div>
            </div>

            {/* ПАРТНЕРКА */}
            <div className="bg-[#12141d] border border-white/10 rounded-2xl p-1 shadow-xl mb-3 shrink-0">
                <button onClick={() => setSubPage('referral')} className="w-full flex justify-between items-center p-4 active:bg-white/5 transition-all">
                    <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-lg bg-emerald-500/10 flex items-center justify-center border border-emerald-500/20 shrink-0">
                            <Crown size={18} className="text-emerald-500" />
                        </div>
                        <div className="text-left">
                            <span className="block text-[14px] font-bold text-white leading-tight">{t.partner_title}</span>
                            <span className="block text-[9px] text-white/30 font-bold uppercase mt-0.5">{t.partner_subtitle}</span>
                        </div>
                    </div>
                    <ChevronRight size={16} className="text-white/20" />
                </button>
            </div>

            {/* МЕНЮ */}
            <div className="bg-[#12141d] border border-white/10 rounded-2xl p-1 shadow-xl mb-3 shrink-0">
                {[
                    {
                        label: t.news,
                        icon: Newspaper,
                        // СЮДА ВСТАВЬ ССЫЛКУ НА НОВОСТИ
                        action: () => handleLink('https://t.me/ССЫЛКА_НА_НОВОСТИ')
                    },
                    {
                        label: t.instructions,
                        icon: BookOpen,
                        action: () => setSubPage('instructions')
                    },
                    {
                        label: t.support,
                        icon: Headphones,
                        // ПРИВЯЗАНО К ТЕБЕ
                        action: () => handleLink('https://t.me/knyazheskyy')
                    },
                    {
                        label: t.rules,
                        icon: ShieldAlert,
                        action: () => window.Telegram?.WebApp?.showAlert(t.rules_text)
                    }
                ].map((item, idx, arr) => (
                    <button
                        key={idx}
                        onClick={item.action}
                        className={`w-full flex justify-between items-center p-4 active:bg-white/5 transition-all ${idx !== arr.length - 1 ? 'border-b border-white/5' : ''}`}
                    >
                        <div className="flex items-center gap-3">
                            <item.icon size={18} className="text-white/40 shrink-0" />
                            <span className="text-[14px] font-bold text-white/80">{item.label}</span>
                        </div>
                        <ChevronRight size={16} className="text-white/20" />
                    </button>
                ))}
            </div>

            {/* УСТРОЙСТВА */}
            <div className="bg-[#12141d] border border-white/10 rounded-2xl p-5 shadow-xl relative overflow-hidden shrink-0">
                <div className="flex justify-between items-center mb-4">
                    <h3 className="text-[14px] font-black text-white uppercase italic">{t.my_devices}</h3>
                    <button
                        onClick={() => setShowDeviceModal(true)}
                        className="w-7 h-7 bg-white/10 rounded-lg flex items-center justify-center active:scale-90 transition-all text-white"
                    >
                        <Plus size={16} />
                    </button>
                </div>
                <div className="space-y-2">
                    {devices.length === 0 ? (
                        <p className="text-white/20 text-center text-[11px] font-medium py-2">{t.no_devices}</p>
                    ) : (
                        devices.map(dev => (
                            <div key={dev.id} className="flex justify-between items-center bg-black/20 p-3 rounded-xl border border-white/5">
                                <div className="flex items-center gap-3">
                                    <div className="w-8 h-8 rounded-lg bg-white/5 flex items-center justify-center border border-white/10 shrink-0">
                                        {dev.deviceType === 'WINDOWS' ? <Laptop size={14} /> : <Smartphone size={14} />}
                                    </div>
                                    <div>
                                        <p className="text-[13px] font-black text-white truncate max-w-[120px]">{dev.deviceName}</p>
                                        <p className="text-[9px] text-white/30 font-black uppercase tracking-widest">{dev.deviceType}</p>
                                    </div>
                                </div>
                                <button onClick={() => deleteDevice(dev.uuid)} className="text-red-500/40 active:text-red-500 p-2 shrink-0">
                                    <Trash2 size={16} />
                                </button>
                            </div>
                        ))
                    )}
                </div>
            </div>

            {/* МОДАЛКА */}
            {showDeviceModal && (
                <>
                    <div className="fixed inset-0 z-[130] bg-black/90 backdrop-blur-sm" onClick={() => setShowDeviceModal(false)} />
                    <div
                        className="fixed left-0 right-0 mx-auto z-[140] bg-[#0a0a0f] border-t border-white/10 rounded-t-[2.5rem] px-8 pt-6 max-w-[480px] animate-in slide-in-from-bottom-full duration-300"
                        style={{ bottom: 0, paddingBottom: 'calc(80px + env(safe-area-inset-bottom, 0px))' }}
                    >
                        <div className="w-12 h-1 bg-white/10 rounded-full mx-auto mb-6" />
                        <h2 className="text-[20px] font-black mb-6 text-center text-white uppercase italic">{t.new_device}</h2>

                        <input
                            type="text"
                            placeholder={t.device_name_placeholder}
                            value={devName}
                            onChange={e => setDevName(e.target.value)}
                            className="w-full bg-white/5 border border-white/10 rounded-xl p-4 text-white text-sm outline-none mb-4"
                        />

                        <div className="grid grid-cols-3 gap-2 mb-6">
                            {['IOS', 'ANDROID', 'WINDOWS'].map(type => (
                                <button
                                    key={type}
                                    onClick={() => setDevType(type)}
                                    className={`py-3 rounded-lg text-[10px] font-black uppercase tracking-widest border transition-all ${devType === type ? 'bg-white text-black border-white' : 'bg-white/5 text-white/40 border-white/10'}`}
                                >
                                    {type}
                                </button>
                            ))}
                        </div>

                        <button
                            onClick={() => { addDevice(devName, devType); setShowDeviceModal(false); setDevName(''); }}
                            className="w-full py-4 bg-white text-black rounded-xl font-black text-sm uppercase tracking-widest active:scale-[0.98] transition-all"
                        >
                            {t.save}
                        </button>
                    </div>
                </>
            )}
        </div>
    );
}