import React, { useState } from 'react';
import { useUserStore } from '../../store/userStore';
import {
    Smartphone, Laptop, Plus, Trash2, Share2,
    ChevronRight, Newspaper, Headphones, BookOpen,
    ShieldAlert, User, ShieldCheck, Fingerprint,
    Crown, CheckCircle2, ArrowLeft, X, Monitor, FileText, Shield
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
    const [subPage, setSubPage] = useState<'main' | 'referral' | 'instructions' | 'privacy' | 'agreement'>('main');
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

    // ─── СТРАНИЦА: ПОЛИТИКА КОНФИДЕНЦИАЛЬНОСТИ (ВЕСЬ ТЕКСТ) ───────────────────
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
                        <p>1.1. Настоящая Политика конфиденциальности (далее — «Политика») регулирует порядок обработки и защиты информации, которую Пользователь передаёт при использовании сервиса (далее — «Сервис»).</p>
                        <p>1.2. Используя Сервис, Пользователь подтверждает своё согласие с условиями Политики. Если Пользователь не согласен с условиями — он обязан прекратить использование Сервиса.</p>

                        <p className="text-emerald-500 font-black mt-4">2. Сбор информации</p>
                        <p>2.1. Сервис может собирать следующие типы данных:</p>
                        <ul className="list-disc pl-5 space-y-1">
                            <li>идентификаторы аккаунта (логин, ID, никнейм и т.п.);</li>
                            <li>техническую информацию (IP-адрес, данные о браузере, устройстве и операционной системе);</li>
                            <li>историю взаимодействий с Сервисом.</li>
                        </ul>
                        <p>2.2. Сервис не требует от Пользователя предоставления паспортных данных, документов, фотографий или другой личной информации, кроме минимально необходимой для работы.</p>

                        <p className="text-emerald-500 font-black mt-4">3. Использование информации</p>
                        <p>3.1. Сервис может использовать полученную информацию исключительно для: обеспечения работы функционала; связи с Пользователем (в том числе для уведомлений и поддержки); анализа и улучшения работы Сервиса.</p>

                        <p className="text-emerald-500 font-black mt-4">4. Передача информации третьим лицам</p>
                        <p>4.1. Администрация не передаёт полученные данные третьим лицам, за исключением случаев: если это требуется по закону; если это необходимо для исполнения обязательств перед Пользователем (например, при работе с платёжными системами); если Пользователь сам дал на это согласие.</p>

                        <p className="text-emerald-500 font-black mt-4">5. Хранение и защита данных</p>
                        <p>5.1. Данные хранятся в течение срока, необходимого для достижения целей обработки. 5.2. Администрация принимает разумные меры для защиты данных, но не гарантирует абсолютную безопасность информации при передаче через интернет.</p>

                        <p className="text-emerald-500 font-black mt-4">6. Отказ от ответственности</p>
                        <p>6.1. Пользователь понимает и соглашается, что передача информации через интернет всегда сопряжена с рисками. 6.2. Администрация не несёт ответственности за утрату, кражу или раскрытие данных, если это произошло по вине третьих лиц или самого Пользователя.</p>

                        <p className="text-emerald-500 font-black mt-4">7. Изменения в Политике</p>
                        <p>7.1. Администрация вправе изменять условия Политики без предварительного уведомления. 7.2. Продолжение использования Сервиса после внесения изменений означает согласие Пользователя с новой редакцией Политики.</p>
                    </div>
                </div>
            </div>
        );
    }

    // ─── СТРАНИЦА: ПОЛЬЗОВАТЕЛЬСКОЕ СОГЛАШЕНИЕ (ВЕСЬ ТЕКСТ) ─────────────────
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
                            <p>1.1. Настоящее Пользовательское соглашение (далее — «Соглашение») регулирует порядок использования онлайн-сервиса (далее — «Сервис»), предоставляемого Администрацией.</p>
                            <p>1.2. Используя Сервис, включая запуск бота, регистрацию, оплату услуг или получение доступа к материалам, Пользователь подтверждает, что полностью ознакомился с условиями настоящего Соглашения и принимает их в полном объёме.</p>
                            <p>1.3. В случае несогласия с условиями Соглашения Пользователь обязан прекратить использование Сервиса.</p>
                        </div>

                        <div>
                            <p className="text-emerald-500 font-black mb-1 uppercase tracking-widest text-[11px]">2. Характер услуг и цифровых товаров</p>
                            <p>2.1. Сервис предоставляет цифровые товары и услуги нематериального характера, включая информационные материалы, обучающие программы, консультации, цифровые продукты и сервисные услуги.</p>
                            <p>2.2. Материалы могут включать: информацию из открытых источников; авторские материалы Администрации и/или третьих лиц; аналитические обзоры, подборки, рекомендации.</p>
                            <p>2.3. Пользователь соглашается, что ценность заключается в систематизации, анализе, форме подачи и сопровождении.</p>
                            <p>2.4. Сервис не гарантирует уникальность отдельных элементов материалов вне Сервиса.</p>
                        </div>

                        <div>
                            <p className="text-emerald-500 font-black mb-1 uppercase tracking-widest text-[11px]">3. Отказ от гарантий и ответственности</p>
                            <p>3.1. Сервис предоставляется на условиях «AS IS» («как есть»).</p>
                            <p>3.2. Администрация не гарантирует соответствие ожиданиям Пользователя, достижение финансовых результатов или бесперебойную работу.</p>
                            <p>3.3. Администрация не несёт ответственности за прямые или косвенные убытки, включая упущенную выгоду, тех. сбои или действия третьих лиц.</p>
                            <p>3.4. Все решения о применении материалов принимаются Пользователем самостоятельно.</p>
                        </div>

                        <div>
                            <p className="text-emerald-500 font-black mb-1 uppercase tracking-widest text-[11px]">4. Законность использования</p>
                            <p>4.1. Сервис не предназначен для поощрения противоправной деятельности. 4.2. Пользователь обязуется использовать Сервис исключительно в рамках законодательства. 4.3. Ответственность полностью возлагается на Пользователя.</p>
                        </div>

                        <div>
                            <p className="text-emerald-500 font-black mb-1 uppercase tracking-widest text-[11px]">5. Интеллектуальная собственность</p>
                            <p>5.1. Все материалы охраняются законодательством. 5.2. Пользователю запрещается копировать, распространять, перепродавать или передавать материалы третьим лицам без разрешения. 5.3. Нарушение прав влечет ограничение доступа без компенсации.</p>
                        </div>

                        <div>
                            <p className="text-emerald-500 font-black mb-1 uppercase tracking-widest text-[11px]">6. Ограничение доступа</p>
                            <p>6.1. Администрация вправе приостановить доступ в случае нарушения Соглашения, злоупотреблений или требований закона. 6.2. Ограничение не освобождает от прошлых обязательств. 6.3. Право отказа в обслуживании при рисках для Сервиса.</p>
                        </div>

                        <div>
                            <p className="text-emerald-500 font-black mb-1 uppercase tracking-widest text-[11px]">7. Платежи и возвраты</p>
                            <p>7.1. Оплата производится на условиях, указанных в Сервисе. 7.2. В связи с нематериальным характером, возврат после предоставления доступа не осуществляется. 7.3. Возврат возможен только если услуга не была оказана по технической вине Сервиса. 7.4. Обращение в поддержку в течение 24 часов. 7.5. Решение принимается индивидуально. 7.6. Пользователь обязуется не инициировать chargeback без обращения в поддержку.</p>
                        </div>

                        <div>
                            <p className="text-emerald-500 font-black mb-1 uppercase tracking-widest text-[11px]">8. Конфиденциальность</p>
                            <p>8.1. Сбор минимально необходимых технических данных. 8.2. Разумные меры защиты информации.</p>
                        </div>

                        <div>
                            <p className="text-emerald-500 font-black mb-1 uppercase tracking-widest text-[11px]">9. Изменение условий</p>
                            <p>9.1. Администрация вправе вносить изменения. 9.2. Актуальная версия публикуется в Сервисе. 9.3. Продолжение использования — согласие с новыми условиями.</p>
                        </div>

                        <div>
                            <p className="text-emerald-500 font-black mb-1 uppercase tracking-widest text-[11px]">10. Контактная информация</p>
                            <p>10.1. По всем вопросам обращаться в службу поддержки через форму в боте.</p>
                        </div>

                        <p className="font-black text-white italic pt-4">Используя Сервис (в том числе запуская бота и/или вводя команду /start), Пользователь подтверждает, что ознакомлен с настоящим Соглашением и принимает его условия в полном объёме.</p>
                    </div>
                </div>
            </div>
        );
    }

    // ─── РЕФЕРАЛЬНАЯ СТРАНИЦА (ОРИГИНАЛЬНАЯ) ─────────────────────────────────
    if (subPage === 'referral') {
        const progress = 3;
        const goal = 10;
        const percent = (progress / goal) * 100;

        return (
            <div className="flex flex-col animate-in fade-in duration-300 overflow-y-auto custom-scrollbar pb-28 pt-2 px-1">
                <button onClick={handleBack} className="flex items-center gap-2 text-white/40 mb-4 font-black text-[10px] uppercase tracking-widest px-2 outline-none">
                    <ArrowLeft size={14} /> {t.back}
                </button>

                <div className="bg-[#12141d] border border-white/10 rounded-[2rem] p-6 mb-6 shadow-2xl relative overflow-hidden text-left">
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

    // ─── СТРАНИЦА ИНСТРУКЦИЙ (ОРИГИНАЛЬНАЯ) ───────────────────────────────────
    if (subPage === 'instructions') {
        const platforms = [
            { id: 'ios',     name: 'iOS',     icon: Smartphone, desc: t.ios_desc },
            { id: 'android', name: 'Android', icon: Smartphone, desc: t.android_desc },
            { id: 'windows', name: 'Windows', icon: Laptop,     desc: t.windows_desc }
        ];

        return (
            <div className="flex flex-col animate-in fade-in duration-300 overflow-y-auto custom-scrollbar pb-28 pt-2 px-1">
                <button onClick={handleBack} className="flex items-center gap-2 text-white/40 mb-4 font-black text-[10px] uppercase tracking-widest px-2 outline-none">
                    <ArrowLeft size={14} /> {t.back}
                </button>

                {!activeInstruction ? (
                    <div className="space-y-2 px-1 text-left">
                        <h2 className="text-[22px] font-black text-white uppercase italic mb-4">{t.instructions_title}</h2>
                        {platforms.map(p => (
                            <button
                                key={p.id}
                                onClick={() => { setActiveInstruction(p.id as any); window.Telegram?.WebApp?.HapticFeedback.impactOccurred('light'); }}
                                className="w-full flex items-center justify-between bg-[#12141d] border border-white/10 p-4 rounded-2xl active:bg-white/5 transition-all outline-none"
                            >
                                <div className="flex items-center gap-4 text-left">
                                    <div className="w-10 h-10 rounded-full bg-white/5 flex items-center justify-center border border-white/10 shrink-0">
                                        <p.icon size={18} className="text-white/80" />
                                    </div>
                                    <div>
                                        <p className="text-[15px] font-black text-white">{p.name}</p>
                                        <p className="text-[10px] text-white/40 font-medium uppercase">{p.desc}</p>
                                    </div>
                                </div>
                                <ChevronRight size={16} className="text-white/20" />
                            </button>
                        ))}
                    </div>
                ) : (
                    <div className="px-2 animate-in slide-in-from-right-4 duration-300 text-left">
                        <h2 className="text-[20px] font-black text-white uppercase italic mb-5">{t.setup_title} {activeInstruction.toUpperCase()}</h2>
                        <div className="space-y-4">
                            {[1, 2].map(s => (
                                <div key={s} className="bg-[#12141d] border border-white/10 rounded-2xl p-5">
                                    <div className="flex items-center gap-3 mb-4 text-white">
                                        <div className="w-7 h-7 rounded-full bg-white text-black flex items-center justify-center font-black text-sm">{s}</div>
                                        <p className="text-sm font-bold">{s === 1 ? t.step_download : t.step_import}</p>
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

    // ─── ГЛАВНАЯ СТРАНИЦА ПРОФИЛЯ ───────────────────────────────────────────
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

            {/* МЕНЮ (С НОВЫМИ ПУНКТАМИ) */}
            <div className="bg-[#12141d] border border-white/10 rounded-2xl p-1 shadow-xl mb-3 shrink-0">
                {[
                    { label: t.news, icon: Newspaper, action: () => handleLink('https://t.me/geovpn_news') },
                    { label: t.instructions, icon: BookOpen, action: () => setSubPage('instructions') },
                    { label: t.support, icon: Headphones, action: () => handleLink('https://t.me/geo_vpn_support') },
                    { label: "Политика конфиденциальности", icon: Shield, action: () => setSubPage('privacy') },
                    { label: "Пользовательское соглашение", icon: FileText, action: () => setSubPage('agreement') }
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
                <div className="flex justify-between items-center mb-4">
                    <h3 className="text-[14px] font-black uppercase italic">{t.my_devices}</h3>
                    <button
                        onClick={() => setShowDeviceModal(true)}
                        className="bg-emerald-500 text-black px-3 py-1.5 rounded-xl flex items-center gap-1.5 active:scale-95 transition-all font-black text-[10px] uppercase shadow-lg shadow-emerald-500/20 outline-none"
                    >
                        <Plus size={14} strokeWidth={3} /> Добавить
                    </button>
                </div>
                <div className="space-y-2">
                    {devices.length === 0 ? (
                        <p className="text-white/20 text-center text-[11px] font-medium py-2 uppercase tracking-widest">{t.no_devices}</p>
                    ) : (
                        devices.map(dev => (
                            <div key={dev.id} className="flex justify-between items-center bg-black/20 p-3 rounded-xl border border-white/5">
                                <div className="flex items-center gap-3">
                                    <div className="w-8 h-8 rounded-lg bg-white/5 flex items-center justify-center border border-white/10 shrink-0 text-white/40">
                                        {dev.deviceType === 'WINDOWS' ? <Monitor size={14} /> : <Smartphone size={14} />}
                                    </div>
                                    <div>
                                        <p className="text-[13px] font-black truncate max-w-[120px]">{dev.deviceName}</p>
                                        <p className="text-[9px] text-white/30 font-black uppercase tracking-widest">{dev.deviceType}</p>
                                    </div>
                                </div>
                                <button onClick={() => deleteDevice(dev.uuid)} className="text-red-500/40 active:text-red-500 p-2 shrink-0 outline-none transition-colors">
                                    <Trash2 size={16} />
                                </button>
                            </div>
                        ))
                    )}
                </div>
            </div>

            {/* ЦЕНТРАЛЬНОЕ ОКНО ДОБАВЛЕНИЯ (ФИКС КРИВИЗНЫ) */}
            {showDeviceModal && (
                <div className="fixed inset-0 z-[1000] flex items-center justify-center p-6 animate-in fade-in duration-300">
                    <div
                        className="absolute inset-0 bg-black/80 backdrop-blur-md"
                        onClick={() => setShowDeviceModal(false)}
                    />

                    <div className="relative w-full max-w-[340px] bg-[#1a1c26] border border-white/10 rounded-[2.5rem] p-8 shadow-[0_0_40px_-10px_rgba(16,185,129,0.2)] animate-in zoom-in-95 duration-200 text-white">
                        <h2 className="text-[20px] font-black mb-8 text-center uppercase italic tracking-wide">
                            {t.new_device}
                        </h2>

                        <div className="space-y-6">
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
                                    {['IOS', 'ANDROID', 'WINDOWS'].map(type => (
                                        <button
                                            key={type}
                                            onClick={() => setDevType(type)}
                                            className={`py-3 rounded-xl text-[10px] font-black uppercase transition-all border outline-none ${
                                                devType === type
                                                    ? 'bg-white text-black border-white shadow-lg'
                                                    : 'bg-white/5 text-white/40 border-white/5 active:scale-95'
                                            }`}
                                        >
                                            {type}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            <div className="pt-2 space-y-3">
                                <button
                                    onClick={() => { addDevice(devName, devType); setShowDeviceModal(false); setDevName(''); }}
                                    className={`w-full py-4 rounded-2xl font-black text-xs uppercase tracking-widest transition-all outline-none ${
                                        devName.trim()
                                            ? 'bg-emerald-500 text-black shadow-lg shadow-emerald-500/20 active:scale-[0.97]'
                                            : 'bg-white/5 text-white/20 pointer-events-none'
                                    }`}
                                >
                                    {t.save}
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