import { useState } from "react"
import { Bot, CreditCard, Save, Shield, Settings, Bell } from "lucide-react"

function ToggleSwitch({ checked, onChange }: { checked: boolean, onChange: (v: boolean) => void }) {
    return (
        <div
            onClick={() => onChange(!checked)}
            style={{
                width: 44, height: 24, borderRadius: 12, cursor: "pointer",
                background: checked ? "#10b981" : "#e2e8f0",
                position: "relative", transition: "background 0.3s"
            }}
        >
            <div style={{
                width: 20, height: 20, borderRadius: "50%", background: "#ffffff",
                position: "absolute", top: 2, left: checked ? 22 : 2,
                transition: "left 0.3s", boxShadow: "0 2px 4px rgba(0,0,0,0.1)"
            }} />
        </div>
    )
}

export default function SettingsPanel() {
    const [activeTab, setActiveTab] = useState("system");
    const [saving, setSaving] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const [settings, setSettings] = useState({
        pricePerGb: 15,
        trialGb: 2,
        maintenanceMode: false,
        autoKick: true,
        botToken: "123456789:ABCdefGHIjklMNOpqrsTUVwxyz",
        supportUsername: "@geovpn_support",
        welcomeMessage: "Привет! Я бот GeoVPN. Здесь ты можешь купить лучший VPN с антиглушилкой.",
        cryptoBotToken: "1234:CRYPTO_BOT_TOKEN_SECRET",
        yookassaId: "884210",
        yookassaSecret: "test_XXXXXXXXXXXXX",
        enableCrypto: true,
        enableCards: false,
    });

    const showToast = (msg: string) => {
        setToast(msg);
        setTimeout(() => setToast(null), 3000);
    }

    const handleSave = () => {
        setSaving(true);
        setTimeout(() => {
            setSaving(false);
            showToast("Настройки успешно сохранены!");
        }, 800);
    }

    const update = (key: string, val: any) => {
        setSettings(p => ({ ...p, [key]: val }));
    }

    const tabs = [
        { id: "system", label: "Система и Биллинг", icon: <Settings size={16} /> },
        { id: "bot", label: "Telegram Бот", icon: <Bot size={16} /> },
        { id: "payments", label: "Платежные шлюзы", icon: <CreditCard size={16} /> },
        { id: "notifications", label: "Уведомления", icon: <Bell size={16} /> },
        { id: "security", label: "Безопасность", icon: <Shield size={16} /> },
    ];

    return (
        <div style={{ minHeight:"100vh", background:"var(--bg-base)", color:"var(--text-main)", padding:"40px", transition: "background 0.3s" }}>
            <style>{`
                .card { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 16px; box-shadow: var(--shadow-card); }
                
                .input-field { width:100%; padding:12px 14px; background:var(--bg-input); border:1px solid var(--border-color); border-radius:10px; color:var(--text-main); font-size:14px; outline:none; transition:all 0.2s; font-family: inherit; }
                .input-field:focus { background:var(--bg-card); border-color:#3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.15); }
                .input-field:disabled { background:var(--border-color); color:#94a3b8; cursor:not-allowed; }
                
                .input-label { font-size:12px; color:var(--text-muted); font-weight:700; display:block; margin-bottom:8px; text-transform:uppercase; letter-spacing:0.04em; }
                
                .btn-primary { display:inline-flex; align-items:center; justify-content:center; gap:8px; padding:12px 24px; background:linear-gradient(135deg, #2563eb, #1d4ed8); border:none; border-radius:10px; color:white; font-size:14px; font-weight:700; cursor:pointer; transition:all 0.2s; box-shadow:0 4px 10px -2px rgba(37,99,235,0.3); }
                .btn-primary:hover:not(:disabled) { transform:translateY(-1px); box-shadow:0 6px 14px -2px rgba(37,99,235,0.4); }
                .btn-primary:disabled { opacity:0.7; cursor:wait; }

                .tab-btn { display:flex; align-items:center; gap:12px; width:100%; padding:14px 18px; border:none; background:transparent; border-radius:12px; font-size:14px; font-weight:600; cursor:pointer; transition:all 0.2s; text-align:left; color:#64748b; }
                .tab-btn:hover:not(.tab-active) { background:var(--bg-hover); color:var(--text-main); }
                .tab-active { background:rgba(37, 99, 235, 0.1); color:#2563eb; }

                .setting-row { display:flex; justify-content:space-between; align-items:center; padding:20px 0; border-bottom:1px solid var(--border-color); }
                .setting-row:last-child { border-bottom:none; }
                
                .slide-down-fade { animation: slideDownFade 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
                @keyframes slideDownFade { from{opacity:0; transform:translateY(-10px)} to{opacity:1; transform:translateY(0)} }
            `}</style>

            {/* Notification toast alert */}
            {toast && (
                <div className="slide-down-fade fixed top-8 right-10 z-[100] bg-slate-900 dark:bg-white text-white dark:text-slate-900 rounded-xl px-5 py-3 text-sm font-bold shadow-2xl flex items-center gap-2">
                    <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" /> {toast}
                </div>
            )}

            <div className="flex justify-between items-start mb-8">
                <div>
                    <h2 className="text-3xl font-extrabold tracking-tight italic" style={{ fontFamily:"Space Grotesk, sans-serif" }}>Настройки</h2>
                    <p className="text-sm text-slate-500 dark:text-slate-400">Глобальные системные параметры, шлюзы и интеграции</p>
                </div>
                <button className="btn-primary cursor-pointer" onClick={handleSave} disabled={saving}>
                    {saving ? "Сохранение..." : <><Save size={16} /> Сохранить изменения</>}
                </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-[260px_1fr] gap-8">
                {/* --- SIDE NAVIGATION --- */}
                <div className="flex flex-col gap-1">
                    {tabs.map(t => (
                        <button key={t.id} onClick={() => setActiveTab(t.id)} className={`tab-btn ${activeTab === t.id ? 'tab-active' : ''}`}>
                            <span style={{ opacity: activeTab === t.id ? 1 : 0.7 }}>{t.icon}</span>
                            {t.label}
                        </button>
                    ))}
                </div>

                {/* --- CORE FORM CONTENT --- */}
                <div className="card p-8 min-h-[500px]">

                    {/* SYSTEM SCHEMA TAB */}
                    {activeTab === "system" && (
                        <div className="slide-down-fade duration-150">
                            <h3 className="text-lg font-bold text-slate-900 dark:text-white mb-2">Система и Биллинг (PAYG)</h3>
                            <p className="text-xs text-slate-500 mb-8">Параметры списания скрипта `TrafficCollector` и общие лимиты за GB.</p>

                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
                                <div>
                                    <label className="input-label">Цена за 1 GB трафика (₽)</label>
                                    <input type="number" className="input-field" value={settings.pricePerGb} onChange={e => update("pricePerGb", Number(e.target.value))} />
                                    <p className="text-[10px] text-slate-400 mt-2">Базовая ставка биллинга.</p>
                                </div>
                                <div>
                                    <label className="input-label">Пробный период (GB)</label>
                                    <input type="number" className="input-field" value={settings.trialGb} onChange={e => update("trialGb", Number(e.target.value))} />
                                    <p className="text-[10px] text-slate-400 mt-2">Бесплатный лимит новым регистрациям.</p>
                                </div>
                            </div>

                            <div className="setting-row">
                                <div>
                                    <div className="text-sm font-bold">Строгий биллинг (Auto-Kick)</div>
                                    <div className="text-xs text-slate-500">Автоматически выключать сессии через Xray API, если баланс ниже 0.</div>
                                </div>
                                <ToggleSwitch checked={settings.autoKick} onChange={v => update("autoKick", v)} />
                            </div>
                            <div className="setting-row">
                                <div>
                                    <div className="text-sm font-bold text-red-500">Режим тех. обслуживания (Maintenance)</div>
                                    <div className="text-xs text-slate-500">Отклонять новые gRPC туннели к нодам. Текущие останутся.</div>
                                </div>
                                <ToggleSwitch checked={settings.maintenanceMode} onChange={v => update("maintenanceMode", v)} />
                            </div>
                        </div>
                    )}

                    {/* TG BOT TAB */}
                    {activeTab === "bot" && (
                        <div className="slide-down-fade duration-150">
                            <h3 className="text-lg font-bold mb-2">Telegram Бот</h3>
                            <p className="text-xs text-slate-500 mb-8">Настройки бота управления подписками.</p>

                            <div className="mb-6">
                                <label className="input-label">Токен Бота (BotFather)</label>
                                <input type="password" className="input-field font-mono" value={settings.botToken} onChange={e => update("botToken", e.target.value)} />
                            </div>
                            <div className="mb-6">
                                <label className="input-label">Юзернейм Поддержки</label>
                                <input type="text" className="input-field" value={settings.supportUsername} onChange={e => update("supportUsername", e.target.value)} />
                                <p className="text-[10px] text-slate-400 mt-2">Ссылка на хелпдеск.</p>
                            </div>
                            <div className="mb-6">
                                <label className="input-label">Приветственное сообщение (/start)</label>
                                <textarea className="input-field" rows={4} value={settings.welcomeMessage} onChange={e => update("welcomeMessage", e.target.value)} style={{ resize:"vertical" }} />
                            </div>
                        </div>
                    )}

                    {/* GATEWAYS TAB */}
                    {activeTab === "payments" && (
                        <div className="slide-down-fade duration-150">
                            <h3 className="text-lg font-bold mb-2">Получение оплат</h3>
                            <p className="text-xs text-slate-500 mb-8">Подключение встроенных касс и Crypto API.</p>

                            <div className="bg-slate-50 dark:bg-slate-900 border border-slate-100 dark:border-slate-800 p-5 rounded-xl mb-6">
                                <div className="flex justify-between items-center mb-4">
                                    <div className="flex items-center gap-3">
                                        <div className="w-10 h-10 bg-blue-600 rounded-lg flex items-center justify-center text-white font-extrabold text-lg">₿</div>
                                        <div>
                                            <div className="text-sm font-bold">CryptoBot Pay</div>
                                            <div className="text-xs text-slate-500">Автоматический прием USDT, TON, BTC.</div>
                                        </div>
                                    </div>
                                    <ToggleSwitch checked={settings.enableCrypto} onChange={v => update("enableCrypto", v)} />
                                </div>
                                <div style={{ opacity: settings.enableCrypto ? 1 : 0.5, pointerEvents: settings.enableCrypto ? "auto" : "none" }}>
                                    <label className="input-label">Crypto Pay API Token</label>
                                    <input type="password" className="input-field font-mono" value={settings.cryptoBotToken} onChange={e => update("cryptoBotToken", e.target.value)} />
                                </div>
                            </div>

                            <div className="bg-slate-50 dark:bg-slate-900 border border-slate-100 dark:border-slate-800 p-5 rounded-xl">
                                <div className="flex justify-between items-center mb-4">
                                    <div className="flex items-center gap-3">
                                        <div className="w-10 h-10 bg-purple-600 rounded-lg flex items-center justify-center text-white font-extrabold text-lg">💳</div>
                                        <div>
                                            <div className="text-sm font-bold">ЮKassa (Карты РФ)</div>
                                            <div className="text-xs text-slate-500">Прием платежей Visa, Mastercard (РФ).</div>
                                        </div>
                                    </div>
                                    <ToggleSwitch checked={settings.enableCards} onChange={v => update("enableCards", v)} />
                                </div>
                                <div style={{ opacity: settings.enableCards ? 1 : 0.5, pointerEvents: settings.enableCards ? "auto" : "none" }}>
                                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                                        <div className="md:col-span-1">
                                            <label className="input-label">Shop ID</label>
                                            <input type="text" className="input-field font-mono" value={settings.yookassaId} onChange={e => update("yookassaId", e.target.value)} />
                                        </div>
                                        <div className="md:col-span-2">
                                            <label className="input-label">Secret Key</label>
                                            <input type="password" className="input-field font-mono" value={settings.yookassaSecret} onChange={e => update("yookassaSecret", e.target.value)} />
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* ALERTS TAB */}
                    {activeTab === "notifications" && (
                        <div className="slide-down-fade duration-150">
                            <h3 className="text-lg font-bold mb-2">Уведомления администратора</h3>
                            <p className="text-xs text-slate-500 mb-8">Настройте оповещения системы мониторинга.</p>
                            <div className="setting-row">
                                <div>
                                    <div className="text-sm font-bold">Алерт: Падение ноды</div>
                                    <div className="text-xs text-slate-500">Присылать аварию в админ-чат, если сервер не отвечает на gRPC.</div>
                                </div>
                                <ToggleSwitch checked={true} onChange={() => {}} />
                            </div>
                        </div>
                    )}

                    {/* CREDENTIALS TAB */}
                    {activeTab === "security" && (
                        <div className="slide-down-fade duration-150">
                            <h3 className="text-lg font-bold mb-2">Безопасность панели</h3>
                            <p className="text-xs text-slate-500 mb-8">Смена конфигурации доступа к GeoVPN Admin.</p>

                            <div className="mb-8">
                                <label className="input-label">Новый пароль администратора</label>
                                <div className="flex gap-3">
                                    <input type="password" placeholder="Пароль" className="input-field" />
                                    <button className="btn-primary" style={{ padding:"12px 20px", whiteSpace:"nowrap" }}>Обновить</button>
                                </div>
                            </div>

                            <div className="bg-red-50 dark:bg-red-950/20 border border-red-200 dark:border-red-900/40 p-5 rounded-xl">
                                <h4 className="text-sm font-bold text-red-600 mb-1">Сбросить все сессии панели</h4>
                                <p className="text-xs text-slate-500 mb-4">Разлогинить всех активных администраторов во всех браузерах.</p>
                                <button className="bg-red-600 hover:bg-red-700 text-white border-none py-2.5 px-4 rounded-lg text-xs font-bold cursor-pointer">Сбросить сессии</button>
                            </div>
                        </div>
                    )}

                </div>
            </div>
        </div>
    )
}
