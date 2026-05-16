import React, { useState } from 'react';
import { useUserStore } from '../../store/userStore';
import { userApi } from '../../api/user';
import {
    Smartphone, Laptop, Plus, Trash2, Share2,
    ChevronRight, Newspaper, Headphones, BookOpen,
    ShieldAlert, User, ShieldCheck, Fingerprint,
    Crown, CheckCircle2, ArrowLeft, X, Monitor, FileText, Shield,
    Edit2, Save, Gift, AlertCircle, Loader2
} from 'lucide-react';

import ios1 from '../../assets/ios_instruction/1.png';
import ios2 from '../../assets/ios_instruction/2.png';
import ios3 from '../../assets/ios_instruction/3.png';
import ios4 from '../../assets/ios_instruction/4.png';
import and1 from '../../assets/adnroid/1.png';
import and2 from '../../assets/adnroid/2.png';
import and3 from '../../assets/adnroid/3.png';
import win1 from '../../assets/windows_macos/1.png';
import win2 from '../../assets/windows_macos/2.png';
import win3 from '../../assets/windows_macos/3.png';
import win4 from '../../assets/windows_macos/4.png';
import win5 from '../../assets/windows_macos/5.png';

const handleLink = (url: string) => {
    if (window.Telegram?.WebApp?.openTelegramLink) {
        window.Telegram.WebApp.openTelegramLink(url);
    } else {
        window.open(url, '_blank');
    }
};

export default function Profile() {
    const { user, devices, configs, deviceLimit, addDevice, deleteDevice, t } = useUserStore();
    const [subPage, setSubPage] = useState<'main' | 'referral' | 'instructions' | 'privacy' | 'agreement'>('main');
    const [showDeviceModal, setShowDeviceModal] = useState(false);
    const [devName, setDevName] = useState('');
    const [devType, setDevType] = useState('IOS');
    const [activeInstruction, setActiveInstruction] = useState<'ios' | 'android' | 'windows' | null>(null);
    const [deletingUuid, setDeletingUuid] = useState<string | null>(null);
    const [isAddingDevice, setIsAddingDevice] = useState(false);

    const [promoCode, setPromoCode] = useState('');
    const [isApplying, setIsApplying] = useState(false);
    const [isEditingCode, setIsEditingCode] = useState(false);
    const [editCodeValue, setEditCodeValue] = useState('');
    const [isSavingCode, setIsSavingCode] = useState(false);

    const tgUser = window.Telegram?.WebApp?.initDataUnsafe?.user;
    const avatarUrl = tgUser?.photo_url;
    const initials = user?.firstName ? user.firstName.charAt(0).toUpperCase() : 'U';
    const realBalance = user?.balance ? (user.balance / 100).toFixed(0) : '0';
    const inviteLink = `https://t.me/geovpn_bot?start=${user?.referralCode}`;

    const limitReached = deviceLimit?.limitReached ?? false;
    const activeDevs   = deviceLimit?.activeDevices ?? devices.length;
    const maxDevs      = deviceLimit?.maxDevices    ?? 3;

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

    // Проверяем лимит перед открытием модала
    const handleAddDeviceClick = () => {
        if (limitReached) {
            window.Telegram?.WebApp?.HapticFeedback.notificationOccurred('error');
            window.Telegram?.WebApp?.showAlert(`Достигнут лимит устройств (${activeDevs}/${maxDevs}). Удалите одно из существующих устройств, чтобы добавить новое.`);
            return;
        }
        setDevName('');
        setDevType('IOS');
        setShowDeviceModal(true);
    };

    // Добавление устройства с проверкой
    const handleAddDevice = async () => {
        if (!devName.trim()) return;
        if (limitReached) {
            window.Telegram?.WebApp?.showAlert('Достигнут лимит устройств');
            return;
        }
        try {
            setIsAddingDevice(true);
            await addDevice(devName.trim(), devType);
            setShowDeviceModal(false);
            setDevName('');
            window.Telegram?.WebApp?.HapticFeedback.notificationOccurred('success');
        } catch (e: any) {
            window.Telegram?.WebApp?.HapticFeedback.notificationOccurred('error');
            window.Telegram?.WebApp?.showAlert(e.message || 'Ошибка при создании устройства');
        } finally {
            setIsAddingDevice(false);
        }
    };

    const handleDeleteDevice = async (uuid: string) => {
        if (devices.length <= 1) {
            window.Telegram?.WebApp?.HapticFeedback.notificationOccurred('error');
            window.Telegram?.WebApp?.showAlert('Нельзя удалить единственное устройство.');
            return;
        }

        window.Telegram?.WebApp?.HapticFeedback.impactOccurred('medium');
        setDeletingUuid(uuid);

        try {
            await deleteDevice(uuid);
            window.Telegram?.WebApp?.HapticFeedback.notificationOccurred('success');
        } catch (e) {
            console.error('Delete device failed:', e);
            window.Telegram?.WebApp?.HapticFeedback.notificationOccurred('error');
            window.Telegram?.WebApp?.showAlert('Не удалось удалить устройство. Попробуйте ещё раз.');
        } finally {
            setDeletingUuid(null);
        }
    };

    const handleApplyPromo = async () => {
        if (!promoCode.trim()) return;
        try {
            setIsApplying(true);
            window.Telegram?.WebApp?.HapticFeedback.impactOccurred('medium');
            const updatedUser = await userApi.applyPromo(promoCode.trim());
            useUserStore.setState({ user: updatedUser });
            window.Telegram?.WebApp?.HapticFeedback.notificationOccurred('success');
            window.Telegram?.WebApp?.showAlert('Успешно! Вы получили +10 дней к подписке 🎉');
            setPromoCode('');
        } catch (e: any) {
            window.Telegram?.WebApp?.HapticFeedback.notificationOccurred('error');
            const errorMsg = e.response?.data?.error?.message || 'Ошибка активации. Проверьте код.';
            window.Telegram?.WebApp?.showAlert(errorMsg);
        } finally {
            setIsApplying(false);
        }
    };

    const handleSaveCustomCode = async () => {
        if (!editCodeValue.trim() || editCodeValue.length < 3) {
            window.Telegram?.WebApp?.showAlert('Код должен содержать минимум 3 символа');
            return;
        }
        try {
            setIsSavingCode(true);
            window.Telegram?.WebApp?.HapticFeedback.impactOccurred('medium');
            const updatedUser = await userApi.updateReferralCode(editCodeValue.trim());
            useUserStore.setState({ user: updatedUser });
            window.Telegram?.WebApp?.HapticFeedback.notificationOccurred('success');
            setIsEditingCode(false);
        } catch (e: any) {
            window.Telegram?.WebApp?.HapticFeedback.notificationOccurred('error');
            const errorMsg = e.response?.data?.error?.message || 'Этот код уже занят или недопустим.';
            window.Telegram?.WebApp?.showAlert(errorMsg);
        } finally {
            setIsSavingCode(false);
        }
    };

    if (subPage === 'privacy') {
        return (
            <div className="flex flex-col animate-in slide-in-from-right duration-300 overflow-hidden h-[100vh] pb-28 pt-2 px-1 text-left">
                <button onClick={handleBack} className="flex items-center gap-2 text-white/40 mb-4 font-black text-[10px] uppercase tracking-widest px-2 outline-none">
                    <ArrowLeft size={14} /> {t.back}
                </button>
                <div className="bg-[#12141d] border border-white/10 rounded-[2rem] p-6 overflow-y-auto custom-scrollbar flex-1 mb-8">
                    <h2 className="text-[20px] font-black text-white uppercase italic mb-6 leading-tight">Политика конфиденциальности</h2>
                    <div className="space-y-4 text-white/70 text-[13px] leading-relaxed pb-10">
                        <p>Политика конфиденциальности регулирует обработку и защиту пользовательских данных. Собираются идентификаторы аккаунта, техническая информация и история взаимодействий, необходимые для функционирования сервиса. Данные используются для связи с пользователем и улучшения сервиса. Передача информации третьим лицам возможна только в законодательно установленных случаях или с согласия. Администрация принимает разумные меры для защиты данных, но не несёт ответственности за их утрату. Политика может меняться без предупреждения — согласие считается данным при дальнейшем использовании сервиса.</p>
                        <p className="text-emerald-500 font-black mt-4">1. Общие положения</p>
                        <p>1.1. Настоящая Политика конфиденциальности регулирует порядок обработки и защиты информации, которую Пользователь передаёт при использовании сервиса.</p>
                        <p>1.2. Используя Сервис, Пользователь подтверждает своё согласие с условиями Политики.</p>
                        <p className="text-emerald-500 font-black mt-4">2. Сбор информации</p>
                        <p>2.1. Сервис может собирать: идентификаторы аккаунта; техническую информацию (IP-адрес, браузер, устройство); историю взаимодействий.</p>
                        <p className="text-emerald-500 font-black mt-4">3. Использование информации</p>
                        <p>3.1. Данные используются для обеспечения работы функционала, связи с пользователем и улучшения сервиса.</p>
                        <p className="text-emerald-500 font-black mt-4">4. Передача информации третьим лицам</p>
                        <p>4.1. Данные не передаются третьим лицам, кроме случаев требования закона или необходимости исполнения обязательств.</p>
                        <p className="text-emerald-500 font-black mt-4">5. Хранение и защита данных</p>
                        <p>5.1. Данные хранятся в течение срока, необходимого для достижения целей обработки. Администрация принимает разумные меры для защиты данных.</p>
                        <p className="text-emerald-500 font-black mt-4">6. Изменения в Политике</p>
                        <p>6.1. Администрация вправе изменять условия Политики. Продолжение использования Сервиса означает согласие с новой редакцией.</p>
                    </div>
                </div>
            </div>
        );
    }

    if (subPage === 'agreement') {
        return (
            <div className="flex flex-col animate-in slide-in-from-right duration-300 overflow-hidden h-[100vh] pb-28 pt-2 px-1 text-left">
                <button onClick={handleBack} className="flex items-center gap-2 text-white/40 mb-4 font-black text-[10px] uppercase tracking-widest px-2 outline-none">
                    <ArrowLeft size={14} /> {t.back}
                </button>
                <div className="bg-[#12141d] border border-white/10 rounded-[2rem] p-6 overflow-y-auto custom-scrollbar flex-1 mb-8 text-white/70 text-[13px] leading-relaxed">
                    <h2 className="text-[20px] font-black text-white uppercase italic mb-6">Пользовательское соглашение</h2>
                    <div className="space-y-6 pb-10">
                        <div>
                            <p className="text-emerald-500 font-black mb-1 uppercase tracking-widest text-[11px]">1. Общие положения</p>
                            <p>Настоящее Соглашение регулирует порядок использования сервиса. Используя сервис, пользователь принимает условия в полном объёме.</p>
                        </div>
                        <div>
                            <p className="text-emerald-500 font-black mb-1 uppercase tracking-widest text-[11px]">2. Характер услуг</p>
                            <p>Сервис предоставляет цифровые товары и услуги нематериального характера.</p>
                        </div>
                        <div>
                            <p className="text-emerald-500 font-black mb-1 uppercase tracking-widest text-[11px]">3. Отказ от гарантий</p>
                            <p>Сервис предоставляется на условиях «AS IS». Администрация не несёт ответственности за прямые или косвенные убытки.</p>
                        </div>
                        <div>
                            <p className="text-emerald-500 font-black mb-1 uppercase tracking-widest text-[11px]">4. Платежи и возвраты</p>
                            <p>Возврат возможен только если услуга не была оказана по технической вине сервиса. Обращение в поддержку в течение 24 часов.</p>
                        </div>
                        <p className="font-black text-white italic pt-4">Используя Сервис, Пользователь подтверждает согласие с условиями Соглашения.</p>
                    </div>
                </div>
            </div>
        );
    }

    if (subPage === 'referral') {
        return (
            <div className="flex flex-col animate-in fade-in duration-300 overflow-y-auto custom-scrollbar pb-28 pt-2 px-1">
                <button onClick={handleBack} className="flex items-center gap-2 text-white/40 mb-4 font-black text-[10px] uppercase tracking-widest px-2 outline-none">
                    <ArrowLeft size={14} /> {t.back}
                </button>

                <div className="bg-gradient-to-b from-[#1a1c29] to-[#0a0a0f] border border-white/10 rounded-[2rem] p-6 mb-4 shadow-[0_20px_40px_rgba(0,0,0,0.8)] relative overflow-hidden text-left">
                    <div className="absolute -top-[20%] -right-[10%] w-[150%] h-[60%] bg-amber-500/10 blur-[80px] rounded-full pointer-events-none" />

                    <div className="flex items-center gap-3 mb-6 relative z-10">
                        <div className="w-12 h-12 bg-gradient-to-br from-amber-400/20 to-amber-600/5 rounded-2xl flex items-center justify-center border border-amber-500/30 shadow-[0_0_20px_rgba(245,158,11,0.2)]">
                            <Crown size={22} className="text-amber-400" />
                        </div>
                        <div>
                            <h2 className="text-[20px] font-black text-white uppercase tracking-tight leading-none mb-1">Geo Partner</h2>
                            <p className="text-amber-400 text-[9px] font-black uppercase tracking-[0.2em]">Партнерская программа</p>
                        </div>
                    </div>

                    <div className="space-y-3 mb-6 relative z-10">
                        <div className="bg-black/40 backdrop-blur-md border border-white/5 rounded-2xl p-4 flex items-center gap-4">
                            <div className="w-10 h-10 rounded-xl bg-amber-500/10 flex items-center justify-center border border-amber-500/20 shrink-0">
                                <span className="text-amber-500 font-black text-lg">₽</span>
                            </div>
                            <div>
                                <p className="text-[14px] font-black text-white">50₽ на ваш баланс</p>
                                <p className="text-[10px] text-white/40 font-bold uppercase mt-0.5">За каждого приведенного друга</p>
                            </div>
                        </div>
                        <div className="bg-black/40 backdrop-blur-md border border-white/5 rounded-2xl p-4 flex items-center gap-4">
                            <div className="w-10 h-10 rounded-xl bg-emerald-500/10 flex items-center justify-center border border-emerald-500/20 shrink-0">
                                <Gift size={18} className="text-emerald-500" />
                            </div>
                            <div>
                                <p className="text-[14px] font-black text-white">+10 дней другу</p>
                                <p className="text-[10px] text-white/40 font-bold uppercase mt-0.5">При активации вашего кода</p>
                            </div>
                        </div>
                    </div>

                    <div className="relative z-10 mb-2">
                        <p className="text-[10px] text-white/40 font-black uppercase mb-2 ml-1 tracking-widest">Ваш личный промокод</p>
                        <div className="flex flex-col gap-2 p-1.5 bg-black/40 border border-white/10 rounded-[1.25rem] backdrop-blur-md">
                            <div className="flex items-center justify-between pl-3 h-12">
                                {!isEditingCode ? (
                                    <>
                                        <span className="text-[16px] font-mono font-black text-amber-400 tracking-widest uppercase">
                                            {user?.referralCode || 'ЗАГРУЗКА...'}
                                        </span>
                                        <div className="flex gap-1 pr-1">
                                            <button
                                                onClick={() => { setEditCodeValue(user?.referralCode || ''); setIsEditingCode(true); window.Telegram?.WebApp?.HapticFeedback.impactOccurred('light'); }}
                                                className="w-10 h-10 flex items-center justify-center text-white/40 active:text-white transition-colors outline-none"
                                            >
                                                <Edit2 size={16} />
                                            </button>
                                            <button
                                                onClick={() => copyAction(user?.referralCode, 'Код скопирован!')}
                                                className="bg-white text-black px-4 h-10 rounded-xl text-[10px] font-black uppercase tracking-widest active:scale-95 transition-all"
                                            >
                                                Копия
                                            </button>
                                        </div>
                                    </>
                                ) : (
                                    <>
                                        <input
                                            type="text"
                                            value={editCodeValue}
                                            onChange={(e) => setEditCodeValue(e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, ''))}
                                            maxLength={15}
                                            placeholder="ВАШ КОД"
                                            className="flex-1 bg-transparent text-[16px] font-mono font-black text-amber-400 tracking-widest uppercase outline-none placeholder:text-amber-400/30"
                                            autoFocus
                                        />
                                        <div className="flex gap-1 pr-1">
                                            <button onClick={() => { setIsEditingCode(false); setEditCodeValue(''); }} className="w-10 h-10 flex items-center justify-center text-white/40 active:text-white transition-colors outline-none">
                                                <X size={18} />
                                            </button>
                                            <button
                                                onClick={handleSaveCustomCode}
                                                disabled={isSavingCode || !editCodeValue.trim()}
                                                className="bg-amber-500 text-black px-4 h-10 rounded-xl flex items-center justify-center active:scale-95 transition-all disabled:opacity-50"
                                            >
                                                {isSavingCode ? '...' : <Save size={16} className="text-black" />}
                                            </button>
                                        </div>
                                    </>
                                )}
                            </div>
                        </div>
                        <button
                            onClick={() => copyAction(inviteLink, t.link_copied)}
                            className="w-full mt-3 py-4 bg-white/5 border border-white/10 text-white rounded-xl font-black text-[12px] uppercase tracking-widest active:bg-white/10 transition-all flex items-center justify-center gap-2"
                        >
                            <Share2 size={16} /> Скопировать ссылку на бота
                        </button>
                    </div>
                </div>

                <div className="bg-[#12141d] border border-emerald-500/20 rounded-[2rem] p-6 shadow-xl relative overflow-hidden">
                    <div className="absolute top-0 right-0 w-32 h-32 bg-emerald-500/5 blur-[40px] rounded-full pointer-events-none" />
                    <p className="text-[12px] text-white font-black uppercase mb-3 relative z-10 flex items-center gap-2">
                        <Gift size={16} className="text-emerald-500" /> Есть промокод?
                    </p>
                    <div className="flex gap-2 relative z-10 bg-black/40 p-1.5 rounded-[1.25rem] border border-white/5">
                        <input
                            type="text"
                            placeholder="ВВЕДИТЕ КОД"
                            value={promoCode}
                            onChange={e => setPromoCode(e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, ''))}
                            className="flex-1 bg-transparent px-4 text-[14px] font-black outline-none text-white placeholder:text-white/20 uppercase font-mono tracking-widest"
                        />
                        <button
                            onClick={handleApplyPromo}
                            disabled={isApplying || !promoCode.trim()}
                            className={`px-6 h-12 rounded-xl font-black text-[11px] uppercase tracking-widest transition-all shadow-lg ${
                                promoCode.trim() && !isApplying
                                    ? 'bg-emerald-500 text-black active:scale-95 shadow-emerald-500/20'
                                    : 'bg-white/5 text-white/20 cursor-not-allowed shadow-none border border-white/5'
                            }`}
                        >
                            {isApplying ? '...' : 'Ок'}
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    if (subPage === 'instructions') {
        const instructionsData = {
            ios: [
                { title: "Скачайте приложение", text: "Перейдите в App Store и установите бесплатное приложение «Happ - Proxy Utility».", image: ios1 },
                { title: "Скопируйте подписку", text: "Вернитесь в это мини-приложение, выберите раздел VPN и нажмите кнопку «Копировать подписку».", image: ios2 },
                { title: "Импортируйте ключи", text: "Откройте Happ, нажмите «+» и выберите «Вставить из буфера обмена».", image: ios3 },
                { title: "Подключитесь", text: "Выберите локацию и нажмите кнопку включения.", image: ios4 }
            ],
            android: [
                { title: "Скачайте приложение", text: "Перейдите в Google Play и установите «Happ - Proxy Utility».", image: and1 },
                { title: "Скопируйте подписку", text: "В мини-приложении нажмите «Копировать подписку».", image: and2 },
                { title: "Импорт и подключение", text: "Откройте Happ, нажмите «+» и выберите «Вставить из буфера обмена».", image: and3 }
            ],
            windows: [
                { title: "Скачайте клиент", text: "Скачайте «Happ - Proxy Utility Desktop» и распакуйте.", image: win1 },
                { title: "Скопируйте подписку", text: "В мини-приложении нажмите «Копировать подписку».", image: win2 },
                { title: "Настройка серверов", text: "Откройте настройки подписок и вставьте скопированную ссылку.", image: win3 },
                { title: "Обновление списка", text: "Нажмите «Обновить подписку» для загрузки серверов.", image: win4 },
                { title: "Подключение", text: "Выберите сервер, нажмите «Сделать активным» и включите «Системный прокси».", image: win5 }
            ]
        };

        const platforms = [
            { id: 'ios',     name: 'iOS',            icon: Smartphone, desc: t.ios_desc     || 'Инструкция для iPhone/iPad' },
            { id: 'android', name: 'Android',        icon: Smartphone, desc: t.android_desc || 'Инструкция для смартфонов'  },
            { id: 'windows', name: 'Windows / macOS', icon: Laptop,    desc: t.windows_desc || 'Инструкция для ПК'           }
        ];

        return (
            <div className="flex flex-col animate-in fade-in duration-300 overflow-y-auto custom-scrollbar pb-28 pt-2 px-1 text-left">
                <button onClick={handleBack} className="flex items-center gap-2 text-white/40 mb-4 font-black text-[10px] uppercase tracking-widest px-2 outline-none">
                    <ArrowLeft size={14} /> {t.back}
                </button>

                {!activeInstruction ? (
                    <div className="space-y-2 px-1 text-left">
                        <h2 className="text-[22px] font-black text-white uppercase italic mb-4">{t.instructions_title || 'Инструкции'}</h2>
                        {platforms.map(p => (
                            <button
                                key={p.id}
                                onClick={() => { setActiveInstruction(p.id as any); window.Telegram?.WebApp?.HapticFeedback.impactOccurred('light'); }}
                                className="w-full flex items-center justify-between bg-[#12141d] border border-white/10 p-4 rounded-2xl active:bg-white/5 transition-all outline-none group"
                            >
                                <div className="flex items-center gap-4 text-left">
                                    <div className="w-10 h-10 rounded-full bg-white/5 flex items-center justify-center border border-white/10 shrink-0 group-active:bg-emerald-500/20 group-active:border-emerald-500/30 transition-colors">
                                        <p.icon size={18} className="text-white/80 group-active:text-emerald-500 transition-colors" />
                                    </div>
                                    <div>
                                        <p className="text-[15px] font-black text-white">{p.name}</p>
                                        <p className="text-[10px] text-white/40 font-medium uppercase mt-0.5">{p.desc}</p>
                                    </div>
                                </div>
                                <ChevronRight size={16} className="text-white/20" />
                            </button>
                        ))}
                    </div>
                ) : (
                    <div className="px-2 animate-in slide-in-from-right-4 duration-300 text-left">
                        <h2 className="text-[20px] font-black text-white uppercase italic mb-6">
                            Настройка {platforms.find(p => p.id === activeInstruction)?.name}
                        </h2>
                        <div className="relative border-l-2 border-emerald-500/20 ml-3 space-y-6 pb-6">
                            {instructionsData[activeInstruction].map((step, idx) => (
                                <div key={idx} className="relative pl-6" style={{ animationDelay: `${idx * 100}ms` }}>
                                    <div className="absolute -left-[17px] top-0 w-8 h-8 rounded-full bg-[#12141d] border-2 border-emerald-500 flex items-center justify-center font-black text-emerald-500 text-sm shadow-[0_0_15px_rgba(16,185,129,0.2)]">
                                        {idx + 1}
                                    </div>
                                    <div className="bg-[#12141d] border border-white/10 rounded-3xl p-5 shadow-lg">
                                        <h3 className="text-[16px] font-black text-white mb-2 leading-tight">{step.title}</h3>
                                        <p className="text-[13px] text-white/60 mb-5 leading-relaxed">{step.text}</p>
                                        <div className="w-full bg-black/40 rounded-xl overflow-hidden border border-white/5">
                                            <img src={step.image} alt={`Шаг ${idx + 1}`} className="w-full h-auto object-cover" />
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        );
    }

    return (
        <div className="flex flex-col overflow-y-auto custom-scrollbar pb-28 pt-2 px-1 animate-in fade-in duration-500 text-left">

            {/* ШАПКА */}
            <div className="bg-[#12141d] border border-white/10 rounded-[2rem] p-5 mb-3 shadow-xl relative overflow-hidden shrink-0">
                <div className="absolute top-0 left-0 w-full h-0.5 bg-gradient-to-r from-emerald-500/50 to-transparent" />
                <div className="flex items-center justify-between mb-5">
                    <div className="flex items-center gap-3 text-white">
                        {avatarUrl ? (
                            <img src={avatarUrl} alt="avatar" className="w-12 h-12 rounded-full border border-white/10 shrink-0 object-cover" />
                        ) : (
                            <div className="w-12 h-12 rounded-full bg-white/5 border border-white/10 flex items-center justify-center shrink-0">
                                <span className="text-lg font-black">{initials}</span>
                            </div>
                        )}
                        <div>
                            <h2 className="text-[18px] font-black leading-tight">{user?.firstName}</h2>
                            <div className="flex items-center gap-1.5 mt-0.5">
                                <ShieldCheck size={12} className="text-emerald-500" />
                                <span className="text-emerald-500 text-[9px] font-black uppercase tracking-widest">{t.protection_100}</span>
                            </div>
                        </div>
                    </div>
                    <div className="w-10 h-10 bg-white/5 rounded-xl border border-white/10 flex items-center justify-center shrink-0">
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
                <div className="bg-[#12141d] border border-white/10 rounded-2xl p-4 shadow-lg text-white">
                    <p className="text-white/40 text-[9px] font-black uppercase tracking-widest mb-1">{t.balance_label}</p>
                    <div className="flex items-baseline gap-1">
                        <h3 className="text-[22px] font-black leading-none">{realBalance}</h3>
                        <span className="text-emerald-500 text-sm font-bold">₽</span>
                    </div>
                </div>
                <div className="bg-[#12141d] border border-white/10 rounded-2xl p-4 shadow-lg text-white">
                    <p className="text-white/40 text-[9px] font-black uppercase tracking-widest mb-1">{t.status}</p>
                    <h3 className={`text-[15px] font-black uppercase italic leading-none mt-1 truncate ${user?.hasActiveSubscription ? 'text-amber-400' : 'text-white/60'}`}>
                        {user?.hasActiveSubscription ? user.subscriptionType : 'INACTIVE'}
                    </h3>
                </div>
            </div>

            {/* ПАРТНЕРКА */}
            <div className="bg-[#12141d] border border-white/10 rounded-2xl p-1 shadow-xl mb-3 shrink-0">
                <button onClick={() => setSubPage('referral')} className="w-full flex justify-between items-center p-4 active:bg-white/5 transition-all text-white outline-none">
                    <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-lg bg-emerald-500/10 flex items-center justify-center border border-emerald-500/20 shrink-0 text-emerald-500">
                            <Crown size={18} />
                        </div>
                        <div className="text-left">
                            <span className="block text-[14px] font-bold leading-tight">{t.partner_title}</span>
                            <span className="block text-[9px] text-white/30 font-bold uppercase mt-0.5">{t.partner_subtitle}</span>
                        </div>
                    </div>
                    <ChevronRight size={16} className="text-white/20" />
                </button>
            </div>

            {/* МЕНЮ */}
            <div className="bg-[#12141d] border border-white/10 rounded-2xl p-1 shadow-xl mb-3 shrink-0">
                {[
                    { label: t.news,         icon: Newspaper, action: () => handleLink('https://t.me/+yuKUzLhYdJVjOWRi') },
                    { label: t.instructions, icon: BookOpen,  action: () => setSubPage('instructions') },
                    { label: t.support,      icon: Headphones,action: () => handleLink('https://t.me/geo_vpn_support') },
                    { label: 'Политика конфиденциальности', icon: Shield,   action: () => setSubPage('privacy') },
                    { label: 'Пользовательское соглашение', icon: FileText, action: () => setSubPage('agreement') },
                ].map((item, idx, arr) => (
                    <button
                        key={idx}
                        onClick={item.action}
                        className={`w-full flex justify-between items-center p-4 active:bg-white/5 transition-all outline-none text-white/80 ${idx !== arr.length - 1 ? 'border-b border-white/5' : ''}`}
                    >
                        <div className="flex items-center gap-3">
                            <item.icon size={18} className="text-white/40 shrink-0" />
                            <span className="text-[14px] font-bold">{item.label}</span>
                        </div>
                        <ChevronRight size={16} className="text-white/20" />
                    </button>
                ))}
            </div>

            {/* УСТРОЙСТВА */}
            <div className="bg-[#12141d] border border-white/10 rounded-2xl p-5 shadow-xl relative overflow-hidden shrink-0 text-white">
                <div className="flex justify-between items-center mb-3">
                    <div>
                        <h3 className="text-[14px] font-black uppercase italic">{t.my_devices}</h3>
                        {deviceLimit && (
                            <p className={`text-[10px] font-black uppercase tracking-widest mt-0.5 ${limitReached ? 'text-red-400' : 'text-white/30'}`}>
                                {activeDevs}/{maxDevs} устройств
                            </p>
                        )}
                    </div>
                    <button
                        onClick={handleAddDeviceClick}
                        className={`px-3 py-1.5 rounded-xl flex items-center gap-1.5 active:scale-95 transition-all font-black text-[10px] uppercase shadow-lg outline-none ${
                            limitReached
                                ? 'bg-white/10 text-white/30 cursor-not-allowed shadow-none border border-white/5'
                                : 'bg-emerald-500 text-black shadow-emerald-500/20'
                        }`}
                    >
                        {limitReached ? (
                            <>
                                <AlertCircle size={14} /> Лимит
                            </>
                        ) : (
                            <>
                                <Plus size={14} strokeWidth={3} /> Добавить
                            </>
                        )}
                    </button>
                </div>

                {/* Баннер при достижении лимита */}
                {limitReached && (
                    <div className="mb-3 p-3 bg-red-500/10 border border-red-500/30 rounded-xl flex items-center gap-2">
                        <AlertCircle size={16} className="text-red-400 shrink-0" />
                        <p className="text-[11px] text-red-300 font-black uppercase tracking-wide">
                            Лимит устройств достигнут. Удалите существующее устройство.
                        </p>
                    </div>
                )}

                <div className="space-y-2">
                    {devices.length === 0 ? (
                        <p className="text-white/20 text-center text-[11px] font-medium py-2 uppercase tracking-widest">{t.no_devices}</p>
                    ) : (
                        devices.map(dev => {
                            const isDeleting = deletingUuid === dev.uuid;
                            const hasConfig  = configs.some(c => c.deviceId === dev.id);
                            return (
                                <div key={dev.id} className={`flex justify-between items-center bg-black/20 p-3 rounded-xl border transition-opacity ${isDeleting ? 'opacity-40 pointer-events-none' : 'border-white/5'}`}>
                                    <div className="flex items-center gap-3">
                                        <div className="w-8 h-8 rounded-lg bg-white/5 flex items-center justify-center border border-white/10 shrink-0 text-white/40">
                                            {dev.deviceType === 'WINDOWS' || dev.deviceType === 'MACOS' || dev.deviceType === 'LINUX' || dev.deviceType === 'DESKTOP'
                                                ? <Monitor size={14} />
                                                : <Smartphone size={14} />
                                            }
                                        </div>
                                        <div>
                                            <p className="text-[13px] font-black truncate max-w-[120px]">{dev.deviceName}</p>
                                            <div className="flex items-center gap-1.5 mt-0.5">
                                                <p className="text-[9px] text-white/30 font-black uppercase tracking-widest">{dev.deviceType}</p>
                                                {hasConfig && (
                                                    <span className="text-[8px] text-emerald-500 font-black uppercase tracking-widest">• VPN</span>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                    <button
                                        onClick={() => handleDeleteDevice(dev.uuid)}
                                        disabled={isDeleting || devices.length <= 1}
                                        className={`p-2 shrink-0 outline-none transition-colors ${
                                            devices.length <= 1
                                                ? 'text-white/10 cursor-not-allowed'
                                                : 'text-red-500/40 active:text-red-500'
                                        }`}
                                    >
                                        {isDeleting
                                            ? <Loader2 size={16} className="animate-spin text-white/30" />
                                            : <Trash2 size={16} />
                                        }
                                    </button>
                                </div>
                            );
                        })
                    )}
                </div>
            </div>

            {/* МОДАЛ ДОБАВЛЕНИЯ УСТРОЙСТВА */}
            {showDeviceModal && (
                <div className="fixed inset-0 z-[1000] flex items-center justify-center p-6 animate-in fade-in duration-300">
                    <div className="absolute inset-0 bg-black/80 backdrop-blur-md" onClick={() => setShowDeviceModal(false)} />
                    <div className="relative w-full max-w-[340px] bg-[#1a1c26] border border-white/10 rounded-[2.5rem] p-8 shadow-[0_0_40px_-10px_rgba(16,185,129,0.2)] animate-in zoom-in-95 duration-200 text-white">

                        <div className="flex items-center justify-between mb-6">
                            <h2 className="text-[20px] font-black uppercase italic tracking-wide">{t.new_device}</h2>
                            <button onClick={() => setShowDeviceModal(false)} className="w-8 h-8 bg-white/5 rounded-full flex items-center justify-center outline-none">
                                <X size={16} className="text-white/40" />
                            </button>
                        </div>

                        {/* Счётчик лимита */}
                        <div className="mb-5 p-3 bg-white/5 rounded-xl flex items-center justify-between">
                            <span className="text-[10px] text-white/40 font-black uppercase tracking-widest">Осталось слотов</span>
                            <span className={`text-[13px] font-black ${(deviceLimit?.remainingSlots ?? 1) <= 1 ? 'text-amber-400' : 'text-emerald-400'}`}>
                                {deviceLimit?.remainingSlots ?? '—'} из {maxDevs}
                            </span>
                        </div>

                        <div className="space-y-5">
                            <div className="space-y-2 text-left">
                                <label className="text-[9px] font-black text-emerald-500 uppercase ml-1 tracking-widest">Название</label>
                                <input
                                    type="text"
                                    placeholder="Введите имя девайса"
                                    value={devName}
                                    onChange={e => setDevName(e.target.value)}
                                    className="w-full bg-black/40 border border-white/10 rounded-2xl p-4 text-sm outline-none focus:border-emerald-500/50 transition-all placeholder:text-white/10"
                                />
                            </div>

                            <div className="space-y-2 text-left">
                                <label className="text-[9px] font-black text-emerald-500 uppercase ml-1 tracking-widest">Платформа</label>
                                <div className="grid grid-cols-3 gap-2">
                                    {['IOS', 'ANDROID', 'WINDOWS', 'MACOS', 'LINUX', 'UNKNOWN'].map(type => (
                                        <button
                                            key={type}
                                            onClick={() => setDevType(type)}
                                            className={`py-3 rounded-xl text-[9px] font-black uppercase transition-all border outline-none ${
                                                devType === type
                                                    ? 'bg-white text-black border-white shadow-lg'
                                                    : 'bg-white/5 text-white/40 border-white/5 active:scale-95'
                                            }`}
                                        >
                                            {type === 'UNKNOWN' ? 'ДРУГОЕ' : type}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            <div className="pt-2 space-y-3">
                                <button
                                    onClick={handleAddDevice}
                                    disabled={!devName.trim() || isAddingDevice}
                                    className={`w-full py-4 rounded-2xl font-black text-xs uppercase tracking-widest transition-all outline-none flex items-center justify-center gap-2 ${
                                        devName.trim() && !isAddingDevice
                                            ? 'bg-emerald-500 text-black shadow-lg shadow-emerald-500/20 active:scale-[0.97]'
                                            : 'bg-white/5 text-white/20 pointer-events-none'
                                    }`}
                                >
                                    {isAddingDevice ? (
                                        <><Loader2 size={16} className="animate-spin" /> Создание...</>
                                    ) : t.save}
                                </button>
                                <button
                                    onClick={() => setShowDeviceModal(false)}
                                    className="w-full py-2 text-[10px] font-black text-white/20 uppercase tracking-[0.2em] active:text-white/60 transition-colors outline-none"
                                >
                                    Отмена
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}