import React, { useState, useRef } from 'react';
import { useUserStore } from '../../store/userStore';
import { Copy, Smartphone, Laptop, Plus, Trash2, Wallet, Trophy, Share2, ChevronRight, Newspaper, BookText, Headphones, ShieldCheck, Users2, CheckCircle2 } from 'lucide-react';

export default function Profile() {
    const { user, devices, addDevice, deleteDevice, actionLoading, setActiveTab, t } = useUserStore();
    const [activeSlide, setActiveSlide] = useState(0);
    const pointerStart = useRef(null);
    const [showDeviceModal, setShowDeviceModal] = useState(false);
    const [devName, setDevName] = useState('');
    const [devType, setDevType] = useState('IOS');

    const realBalance = user?.balance ? (user.balance / 100).toFixed(0) : '0';
    const inviteLink = `https://t.me/geovpn_bot?start=${user?.referralCode}`;

    const handlePointerDown = (e) => { pointerStart.current = e.clientX; };
    const handlePointerUp = (e) => {
        if (pointerStart.current === null) return;
        const distance = pointerStart.current - e.clientX;
        if (distance > 50 && activeSlide < 1) setActiveSlide(1);
        else if (distance < -50 && activeSlide > 0) setActiveSlide(0);
        pointerStart.current = null;
    };

    const copyAction = (text) => {
        navigator.clipboard.writeText(text);
        window.Telegram?.WebApp?.HapticFeedback.notificationOccurred('success');
    };

    const menuItems = [
        { id: 'ref', icon: Users2, label: 'Реферальная программа', action: () => setActiveSlide(1) },
        { id: 'news', icon: Newspaper, label: 'Новости сервиса', action: () => window.Telegram?.WebApp?.openTelegramLink('https://t.me/geovpn_news') },
        { id: 'support', icon: Headphones, label: 'Поддержка', action: () => window.Telegram?.WebApp?.openTelegramLink('https://t.me/geovpn_support') },
    ];

    return (
        <div className="flex flex-col h-[78vh] relative select-none" style={{ touchAction: 'pan-y' }} onPointerDown={handlePointerDown} onPointerUp={handlePointerUp}>
            <div className="flex gap-6 px-1 mb-6 relative z-10">
                <button onClick={() => setActiveSlide(0)} className={`text-[26px] font-black uppercase italic transition-all ${activeSlide === 0 ? 'text-foreground' : 'text-muted-foreground/40'}`}>{t.settings}</button>
                <button onClick={() => setActiveSlide(1)} className={`text-[26px] font-black uppercase italic transition-all ${activeSlide === 1 ? 'text-foreground' : 'text-muted-foreground/40'}`}>Партнерам</button>
            </div>

            <div className="flex transition-transform duration-500 ease-[cubic-bezier(0.25,1,0.5,1)] h-full" style={{ transform: `translateX(-${activeSlide * 100}%)` }}>
                <div className="w-full flex-shrink-0 px-1 space-y-4 overflow-y-auto pb-24 custom-scrollbar">
                    <div className="bg-card border border-border rounded-[2rem] p-7 shadow-2xl relative overflow-hidden">
                        <div className="flex items-center gap-2 mb-4 text-muted-foreground">
                            <Wallet size={14} /> <p className="text-[11px] font-black uppercase tracking-widest">{t.balance}</p>
                        </div>
                        <div className="flex justify-between items-center relative z-10">
                            <h2 className="text-[40px] font-black tracking-tighter text-foreground">{realBalance} <span className="text-[18px] text-muted-foreground ml-1">₽</span></h2>
                            <button onClick={() => setActiveTab('payments')} className="bg-primary text-primary-foreground px-6 py-3 rounded-xl font-black text-[13px] uppercase active:scale-95 transition-transform">Пополнить</button>
                        </div>
                    </div>

                    <div className="bg-card border border-border rounded-2xl p-6">
                        <div className="flex justify-between items-center border-b border-border pb-3">
                            <span className="text-[13px] font-bold text-muted-foreground">Telegram ID</span>
                            <span className="font-mono text-[13px] font-bold text-foreground">{user?.telegramId}</span>
                        </div>
                    </div>

                    <div className="bg-card border border-border rounded-2xl p-6">
                        <div className="flex justify-between items-center mb-4">
                            <p className="text-muted-foreground text-[10px] font-black uppercase">Мои Устройства</p>
                            <button onClick={() => setShowDeviceModal(true)} className="w-8 h-8 bg-secondary rounded-lg flex items-center justify-center border border-border"><Plus size={16} /></button>
                        </div>
                        <div className="space-y-2">
                            {devices.map((dev) => (
                                <div key={dev.uuid} className="flex justify-between items-center bg-muted/30 p-4 rounded-xl border border-border">
                                    <div className="flex items-center gap-4">
                                        <div className="w-10 h-10 bg-background rounded-xl flex items-center justify-center border border-border">
                                            {dev.deviceType === 'DESKTOP' ? <Laptop size={18} /> : <Smartphone size={18} />}
                                        </div>
                                        <div>
                                            <p className="text-[14px] font-black text-foreground">{dev.deviceName}</p>
                                            <p className="text-[10px] text-muted-foreground font-black uppercase">{dev.deviceType}</p>
                                        </div>
                                    </div>
                                    <button onClick={() => deleteDevice(dev.uuid)} className="text-destructive/40 active:text-destructive p-2"><Trash2 size={16} /></button>
                                </div>
                            ))}
                        </div>
                    </div>

                    <div className="space-y-2">
                        {menuItems.map((item) => (
                            <button key={item.id} onClick={item.action} className="w-full flex justify-between items-center bg-card border border-border p-5 rounded-2xl active:bg-muted transition-colors group">
                                <div className="flex items-center gap-4">
                                    <item.icon size={20} className="text-muted-foreground group-active:text-foreground" />
                                    <span className="text-[15px] font-bold text-foreground tracking-tight">{item.label}</span>
                                </div>
                                <ChevronRight size={18} className="text-muted-foreground/30 group-active:text-foreground" />
                            </button>
                        ))}
                    </div>
                </div>

                <div className="w-full flex-shrink-0 px-1 space-y-6">
                    <div className="bg-card border border-border rounded-[2rem] p-8 text-center">
                        <div className="w-20 h-20 bg-muted rounded-full flex items-center justify-center mx-auto mb-6 border border-border"><Trophy size={40} className="text-amber-400" /></div>
                        <h2 className="text-[28px] font-black text-foreground uppercase italic mb-2">Зарабатывай</h2>
                        <div className="bg-background border border-border p-6 rounded-2xl mb-6 cursor-pointer" onClick={() => copyAction(user?.referralCode)}>
                            <p className="text-[10px] text-muted-foreground uppercase mb-2">Код</p>
                            <span className="font-mono text-[28px] font-black text-foreground tracking-[0.3em]">{user?.referralCode}</span>
                        </div>
                        <button onClick={() => copyAction(inviteLink)} className="btn-primary flex items-center justify-center gap-3"><Share2 size={20} /> ССЫЛКА</button>
                    </div>
                </div>
            </div>

            {showDeviceModal && (
                <>
                    <div className="fixed inset-0 z-[130] bg-black/80 backdrop-blur-sm" onClick={() => setShowDeviceModal(false)} />
                    <div className="bottom-sheet z-[140]">
                        <div className="w-12 h-1 bg-muted rounded-full mx-auto mb-8" />
                        <h2 className="text-[24px] font-black mb-8 text-center text-foreground">Добавить</h2>
                        <input type="text" placeholder="Название..." value={devName} onChange={(e) => setDevName(e.target.value)} className="w-full bg-muted border border-border rounded-xl p-5 text-foreground outline-none mb-4" />
                        <div className="grid grid-cols-3 gap-2 mb-8">
                            {['IOS', 'ANDROID', 'DESKTOP'].map(type => (
                                <button key={type} onClick={() => setDevType(type)} className={`py-4 rounded-xl text-[11px] font-black border transition-all ${devType === type ? 'bg-primary text-primary-foreground border-primary' : 'bg-secondary text-muted-foreground border-border'}`}>{type}</button>
                            ))}
                        </div>
                        <button onClick={() => { addDevice(devName, devType); setShowDeviceModal(false); }} className="btn-primary">СОХРАНИТЬ</button>
                    </div>
                </>
            )}
        </div>
    );
}