import { useState } from "react"

// --- ИКОНКИ ---
const Icons = {
    User: () => <svg width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>,
    Bot: () => <svg width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="11" width="18" height="10" rx="2"/><circle cx="12" cy="5" r="2"/><path d="M12 7v4"/><line x1="8" y1="16" x2="8.01" y2="16"/><line x1="16" y1="16" x2="16.01" y2="16"/></svg>,
    CreditCard: () => <svg width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="1" y="4" width="22" height="16" rx="2" ry="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>,
    Settings: () => <svg width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>,
    Shield: () => <svg width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>,
    Save: () => <svg width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>,
    Bell: () => <svg width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>,
}

// Кастомный iOS-стиль свитч (переключатель)
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

    // MOCK State настроек
    const [settings, setSettings] = useState({
        // System
        pricePerGb: 15,
        trialGb: 2,
        maintenanceMode: false,
        autoKick: true,
        // Telegram Bot
        botToken: "123456789:ABCdefGHIjklMNOpqrsTUVwxyz",
        supportUsername: "@geovpn_support",
        welcomeMessage: "Привет! Я бот GeoVPN. Здесь ты можешь купить лучший VPN с антиглушилкой.",
        // Billing
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
        // Эмуляция запроса к API
        setTimeout(() => {
            setSaving(false);
            showToast("Настройки успешно сохранены!");
        }, 800);
    }

    const update = (key: string, val: any) => {
        setSettings(p => ({ ...p, [key]: val }));
    }

    const tabs = [
        { id: "system", label: "Система и Биллинг", icon: <Icons.Settings /> },
        { id: "bot", label: "Telegram Бот", icon: <Icons.Bot /> },
        { id: "payments", label: "Платежные шлюзы", icon: <Icons.CreditCard /> },
        { id: "notifications", label: "Уведомления", icon: <Icons.Bell /> },
        { id: "security", label: "Безопасность", icon: <Icons.Shield /> },
    ];

    return (
        <div style={{ minHeight:"100vh", background:"#f4f7fb", fontFamily:"'Inter',-apple-system,sans-serif", color:"#0f172a", padding:"40px" }}>
            <style>{`
                @import url('https://fonts.googleapis.com/css2?family=DM+Mono:wght@400;500;600&family=Space+Grotesk:wght@500;700;800&family=Inter:wght@400;500;600;700&display=swap');
                * { box-sizing:border-box; margin:0; padding:0; }
                
                .card { background: #ffffff; border: 1px solid #e2e8f0; border-radius: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.02); }
                
                .input-field { width:100%; padding:12px 14px; background:#f8fafc; border:1px solid #e2e8f0; border-radius:10px; color:#0f172a; font-size:14px; outline:none; transition:all 0.2s; font-family: inherit; }
                .input-field:focus { background:#ffffff; border-color:#3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.15); }
                .input-field:disabled { background:#f1f5f9; color:#94a3b8; cursor:not-allowed; }
                
                .input-label { font-size:12px; color:#64748b; font-weight:700; display:block; margin-bottom:8px; text-transform:uppercase; letter-spacing:0.04em; }
                
                .btn-primary { display:inline-flex; align-items:center; justify-content:center; gap:8px; padding:12px 24px; background:linear-gradient(135deg, #2563eb, #1d4ed8); border:none; border-radius:10px; color:white; font-size:14px; font-weight:700; cursor:pointer; transition:all 0.2s; box-shadow:0 4px 10px -2px rgba(37,99,235,0.3); }
                .btn-primary:hover:not(:disabled) { transform:translateY(-1px); box-shadow:0 6px 14px -2px rgba(37,99,235,0.4); }
                .btn-primary:disabled { opacity:0.7; cursor:wait; }

                .tab-btn { display:flex; align-items:center; gap:12px; width:100%; padding:14px 18px; border:none; background:transparent; border-radius:12px; font-size:14px; font-weight:600; cursor:pointer; transition:all 0.2s; text-align:left; color:#64748b; }
                .tab-btn:hover:not(.tab-active) { background:#f8fafc; color:#0f172a; }
                .tab-active { background:#eff6ff; color:#2563eb; }

                .setting-row { display:flex; justify-content:space-between; align-items:center; padding:20px 0; border-bottom:1px solid #f1f5f9; }
                .setting-row:last-child { border-bottom:none; }
                
                .slide-down-fade { animation: slideDownFade 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
                @keyframes slideDownFade { from{opacity:0; transform:translateY(-10px)} to{opacity:1; transform:translateY(0)} }
            `}</style>

            {/* Всплывающее уведомление */}
            {toast && (
                <div className="slide-down-fade" style={{ position:"fixed", top:32, right:40, zIndex:100, background:"#0f172a", borderRadius:12, padding:"12px 20px", fontSize:14, fontWeight:600, color:"#ffffff", boxShadow:"0 10px 30px rgba(0,0,0,0.15)", display:"flex", alignItems:"center", gap:10 }}>
                    <div style={{ width:8, height:8, background:"#10b981", borderRadius:"50%" }}/> {toast}
                </div>
            )}

            {/* --- ШАПКА --- */}
            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"flex-start", marginBottom:"32px" }}>
                <div>
                    <h2 style={{ fontSize:36, fontWeight:800, fontFamily:"'Space Grotesk',sans-serif", letterSpacing:"-0.03em", marginBottom:6 }}>Настройки</h2>
                    <p style={{ fontSize:15, color:"#64748b", fontWeight:500 }}>Глобальные параметры системы, бот и платежи</p>
                </div>
                <button className="btn-primary" onClick={handleSave} disabled={saving}>
                    {saving ? "Сохранение..." : <><Icons.Save /> Сохранить изменения</>}
                </button>
            </div>

            <div style={{ display:"grid", gridTemplateColumns:"260px 1fr", gap:"32px", alignItems:"start" }}>

                {/* --- БОКОВОЕ МЕНЮ ВКЛАДОК --- */}
                <div style={{ display:"flex", flexDirection:"column", gap:"4px" }}>
                    {tabs.map(t => (
                        <button key={t.id} onClick={() => setActiveTab(t.id)} className={`tab-btn ${activeTab === t.id ? 'tab-active' : ''}`}>
                            <span style={{ opacity: activeTab === t.id ? 1 : 0.7 }}>{t.icon}</span>
                            {t.label}
                        </button>
                    ))}
                </div>

                {/* --- КОНТЕНТ НАСТРОЕК --- */}
                <div className="card" style={{ padding:"32px", minHeight:"500px" }}>

                    {/* ВКЛАДКА: СИСТЕМА */}
                    {activeTab === "system" && (
                        <div style={{ animation:"slideDownFade 0.2s ease" }}>
                            <h3 style={{ fontSize:20, fontWeight:800, color:"#0f172a", marginBottom:8 }}>Система и Биллинг (PAYG)</h3>
                            <p style={{ fontSize:14, color:"#64748b", marginBottom:32 }}>Настройка тарифов TrafficCollectorJob и глобальные лимиты.</p>

                            <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:"24px", marginBottom:"32px" }}>
                                <div>
                                    <label className="input-label">Цена за 1 GB трафика (₽)</label>
                                    <input type="number" className="input-field" value={settings.pricePerGb} onChange={e => update("pricePerGb", Number(e.target.value))} />
                                    <p style={{ fontSize:12, color:"#94a3b8", marginTop:8 }}>Базовая ставка, которую списывает скрипт.</p>
                                </div>
                                <div>
                                    <label className="input-label">Пробный период (GB)</label>
                                    <input type="number" className="input-field" value={settings.trialGb} onChange={e => update("trialGb", Number(e.target.value))} />
                                    <p style={{ fontSize:12, color:"#94a3b8", marginTop:8 }}>Выдается бесплатно при старте в боте.</p>
                                </div>
                            </div>

                            <div className="setting-row" style={{ borderTop:"1px solid #f1f5f9" }}>
                                <div>
                                    <div style={{ fontSize:15, fontWeight:700, color:"#0f172a", marginBottom:4 }}>Строгий биллинг (Auto-Kick)</div>
                                    <div style={{ fontSize:13, color:"#64748b" }}>Автоматически отключать сессию через Xray gRPC, если баланс упал ниже 0.</div>
                                </div>
                                <ToggleSwitch checked={settings.autoKick} onChange={v => update("autoKick", v)} />
                            </div>
                            <div className="setting-row">
                                <div>
                                    <div style={{ fontSize:15, fontWeight:700, color:"#ef4444", marginBottom:4 }}>Режим обслуживания (Maintenance)</div>
                                    <div style={{ fontSize:13, color:"#64748b" }}>Запретить новые подключения к кластеру. Текущие сессии не пострадают.</div>
                                </div>
                                <ToggleSwitch checked={settings.maintenanceMode} onChange={v => update("maintenanceMode", v)} />
                            </div>
                        </div>
                    )}

                    {/* ВКЛАДКА: BOT */}
                    {activeTab === "bot" && (
                        <div style={{ animation:"slideDownFade 0.2s ease" }}>
                            <h3 style={{ fontSize:20, fontWeight:800, color:"#0f172a", marginBottom:8 }}>Telegram Бот</h3>
                            <p style={{ fontSize:14, color:"#64748b", marginBottom:32 }}>Настройки основного интерфейса пользователей.</p>

                            <div style={{ marginBottom:"24px" }}>
                                <label className="input-label">Токен Бота (BotFather)</label>
                                <input type="password" className="input-field" value={settings.botToken} onChange={e => update("botToken", e.target.value)} style={{ fontFamily:"'DM Mono',monospace" }} />
                            </div>
                            <div style={{ marginBottom:"24px" }}>
                                <label className="input-label">Юзернейм Поддержки</label>
                                <input type="text" className="input-field" value={settings.supportUsername} onChange={e => update("supportUsername", e.target.value)} />
                                <p style={{ fontSize:12, color:"#94a3b8", marginTop:8 }}>Сюда бот будет отправлять пользователей с проблемами.</p>
                            </div>
                            <div style={{ marginBottom:"24px" }}>
                                <label className="input-label">Приветственное сообщение (/start)</label>
                                <textarea className="input-field" rows={4} value={settings.welcomeMessage} onChange={e => update("welcomeMessage", e.target.value)} style={{ resize:"vertical" }} />
                            </div>
                        </div>
                    )}

                    {/* ВКЛАДКА: ПЛАТЕЖИ */}
                    {activeTab === "payments" && (
                        <div style={{ animation:"slideDownFade 0.2s ease" }}>
                            <h3 style={{ fontSize:20, fontWeight:800, color:"#0f172a", marginBottom:8 }}>Платежные шлюзы</h3>
                            <p style={{ fontSize:14, color:"#64748b", marginBottom:32 }}>Подключение методов пополнения баланса.</p>

                            <div style={{ background:"#f8fafc", border:"1px solid #e2e8f0", borderRadius:"12px", padding:"20px", marginBottom:"24px" }}>
                                <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:"20px" }}>
                                    <div style={{ display:"flex", alignItems:"center", gap:12 }}>
                                        <div style={{ width:40, height:40, background:"#2563eb", borderRadius:10, display:"flex", alignItems:"center", justifyContent:"center", color:"white", fontWeight:800, fontSize:20 }}>₿</div>
                                        <div>
                                            <div style={{ fontSize:16, fontWeight:700, color:"#0f172a" }}>CryptoBot Pay</div>
                                            <div style={{ fontSize:13, color:"#64748b" }}>Прием USDT, TON, BTC без комиссий.</div>
                                        </div>
                                    </div>
                                    <ToggleSwitch checked={settings.enableCrypto} onChange={v => update("enableCrypto", v)} />
                                </div>
                                <div style={{ opacity: settings.enableCrypto ? 1 : 0.5, pointerEvents: settings.enableCrypto ? "auto" : "none" }}>
                                    <label className="input-label">Crypto Pay API Token</label>
                                    <input type="password" className="input-field" value={settings.cryptoBotToken} onChange={e => update("cryptoBotToken", e.target.value)} style={{ fontFamily:"'DM Mono',monospace" }} />
                                </div>
                            </div>

                            <div style={{ background:"#f8fafc", border:"1px solid #e2e8f0", borderRadius:"12px", padding:"20px" }}>
                                <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:"20px" }}>
                                    <div style={{ display:"flex", alignItems:"center", gap:12 }}>
                                        <div style={{ width:40, height:40, background:"#8b5cf6", borderRadius:10, display:"flex", alignItems:"center", justifyContent:"center", color:"white", fontWeight:800, fontSize:20 }}>💳</div>
                                        <div>
                                            <div style={{ fontSize:16, fontWeight:700, color:"#0f172a" }}>ЮKassa (Банковские карты РФ)</div>
                                            <div style={{ fontSize:13, color:"#64748b" }}>Прием рублей с карт МИР, Visa, Mastercard.</div>
                                        </div>
                                    </div>
                                    <ToggleSwitch checked={settings.enableCards} onChange={v => update("enableCards", v)} />
                                </div>
                                <div style={{ opacity: settings.enableCards ? 1 : 0.5, pointerEvents: settings.enableCards ? "auto" : "none" }}>
                                    <div style={{ display:"grid", gridTemplateColumns:"1fr 2fr", gap:16 }}>
                                        <div>
                                            <label className="input-label">Shop ID</label>
                                            <input type="text" className="input-field" value={settings.yookassaId} onChange={e => update("yookassaId", e.target.value)} style={{ fontFamily:"'DM Mono',monospace" }} />
                                        </div>
                                        <div>
                                            <label className="input-label">Secret Key</label>
                                            <input type="password" className="input-field" value={settings.yookassaSecret} onChange={e => update("yookassaSecret", e.target.value)} style={{ fontFamily:"'DM Mono',monospace" }} />
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* ВКЛАДКИ ЗАГЛУШКИ ДЛЯ КРАСОТЫ */}
                    {activeTab === "notifications" && (
                        <div style={{ animation:"slideDownFade 0.2s ease" }}>
                            <h3 style={{ fontSize:20, fontWeight:800, color:"#0f172a", marginBottom:8 }}>Уведомления администратора</h3>
                            <p style={{ fontSize:14, color:"#64748b", marginBottom:32 }}>Настройте алерты о состоянии системы.</p>
                            <div className="setting-row" style={{ borderTop:"1px solid #f1f5f9" }}>
                                <div>
                                    <div style={{ fontSize:15, fontWeight:700, color:"#0f172a", marginBottom:4 }}>Алерт: Падение сервера</div>
                                    <div style={{ fontSize:13, color:"#64748b" }}>Отправлять сообщение в админ-чат, если сервер не отвечает на gRPC.</div>
                                </div>
                                <ToggleSwitch checked={true} onChange={() => {}} />
                            </div>
                        </div>
                    )}

                    {activeTab === "security" && (
                        <div style={{ animation:"slideDownFade 0.2s ease" }}>
                            <h3 style={{ fontSize:20, fontWeight:800, color:"#0f172a", marginBottom:8 }}>Безопасность панели</h3>
                            <p style={{ fontSize:14, color:"#64748b", marginBottom:32 }}>Управление доступом к GeoVPN Admin.</p>

                            <div style={{ marginBottom:"24px" }}>
                                <label className="input-label">Сменить пароль администратора</label>
                                <div style={{ display:"flex", gap:12 }}>
                                    <input type="password" placeholder="Новый пароль" className="input-field" />
                                    <button className="btn-primary" style={{ padding:"12px 20px", whiteSpace:"nowrap" }}>Обновить</button>
                                </div>
                            </div>

                            <div style={{ background:"#fef2f2", border:"1px solid #fecaca", borderRadius:"12px", padding:"20px", marginTop:40 }}>
                                <h4 style={{ fontSize:15, fontWeight:800, color:"#dc2626", marginBottom:8 }}>Отзыв всех API-сессий</h4>
                                <p style={{ fontSize:13, color:"#991b1b", marginBottom:16 }}>Если вы подозреваете утечку токена, вы можете сбросить все сессии панели управления.</p>
                                <button style={{ background:"#dc2626", color:"white", border:"none", padding:"10px 16px", borderRadius:8, fontWeight:700, cursor:"pointer" }}>Сбросить токены</button>
                            </div>
                        </div>
                    )}

                </div>
            </div>
        </div>
    )
}