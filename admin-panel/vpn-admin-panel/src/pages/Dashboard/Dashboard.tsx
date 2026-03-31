import { useState, useEffect } from "react"

// Иконки (те же самые)
const Icons = {
    Users: () => <svg width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>,
    Server: () => <svg width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="2" y="2" width="20" height="8" rx="2" ry="2"/><rect x="2" y="14" width="20" height="8" rx="2" ry="2"/><line x1="6" y1="6" x2="6.01" y2="6"/><line x1="6" y1="18" x2="6.01" y2="18"/></svg>,
    Activity: () => <svg width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>,
    Wallet: () => <svg width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M20 12V8H6a2 2 0 0 1-2-2c0-1.1.9-2 2-2h12v4"/><path d="M4 6v12c0 1.1.9 2 2 2h14v-4"/><path d="M18 12a2 2 0 0 0-2 2c0 1.1.9 2 2 2h4v-4h-4z"/></svg>,
    UserPlus: () => <svg width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><line x1="19" y1="8" x2="19" y2="14"/><line x1="22" y1="11" x2="16" y2="11"/></svg>,
    Zap: () => <svg width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>,
    Refresh: () => <svg width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/></svg>,
    ShieldAlert: () => <svg width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>,
    TrendingUp: () => <svg width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/></svg>,
}

function AreaChart({ data }: { data: number[] }) {
    if (data.length < 2) return null;
    const max = Math.max(...data, 100);
    const W = 800, H = 180;
    const pts = data.map((v, i) => `${(i / (data.length - 1)) * W},${H - (v / max) * H}`).join(" ");
    return (
        <svg width="100%" height="100%" viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none" style={{ overflow: "visible" }}>
            <defs>
                <linearGradient id="chartGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#3b82f6" stopOpacity="0.2"/>
                    <stop offset="100%" stopColor="#3b82f6" stopOpacity="0"/>
                </linearGradient>
            </defs>
            <polygon points={`0,${H} ${pts} ${W},${H}`} fill="url(#chartGrad)" />
            <polyline points={pts} fill="none" stroke="#3b82f6" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"/>
            <circle cx={W} cy={H - (data[data.length - 1] / max) * H} r="5" fill="var(--bg-card)" stroke="#3b82f6" strokeWidth="3" />
        </svg>
    )
}

function DonutChart({ segments }: { segments: { value: number, color: string, label: string }[] }) {
    const total = segments.reduce((acc, s) => acc + s.value, 0);
    const radius = 60;
    const circumference = 2 * Math.PI * radius;
    let currentOffset = 0;

    return (
        <div style={{ display:"flex", alignItems:"center", gap:"32px" }}>
            <div style={{ position:"relative", width:"140px", height:"140px" }}>
                <svg width="140" height="140" viewBox="0 0 140 140">
                    <circle cx="70" cy="70" r={radius} fill="none" stroke="var(--border-color)" strokeWidth="16" />
                    {segments.map((seg, i) => {
                        const dashArray = `${(seg.value / total) * circumference} ${circumference}`;
                        const offset = currentOffset;
                        currentOffset -= (seg.value / total) * circumference;
                        return (
                            <circle key={i} cx="70" cy="70" r={radius} fill="none" stroke={seg.color} strokeWidth="16"
                                    strokeDasharray={dashArray} strokeDashoffset={offset} strokeLinecap="round"
                                    style={{ transformOrigin: "50% 50%", transform: "rotate(-90deg)", transition: "all 1s ease-out" }}
                            />
                        );
                    })}
                </svg>
                <div style={{ position:"absolute", inset:0, display:"flex", flexDirection:"column", alignItems:"center", justifyContent:"center" }}>
                    <span style={{ fontSize:11, color:"var(--text-muted)", fontWeight:700, textTransform:"uppercase" }}>Total</span>
                    <span style={{ fontSize:18, color:"var(--text-main)", fontWeight:800, fontFamily:"'DM Mono',monospace" }}>{total} <span style={{fontSize:11}}>GB</span></span>
                </div>
            </div>

            <div style={{ display:"flex", flexDirection:"column", gap:"12px", flex:1 }}>
                {segments.map((s, i) => (
                    <div key={i} style={{ display:"flex", justifyContent:"space-between", alignItems:"center" }}>
                        <div style={{ display:"flex", alignItems:"center", gap:"8px" }}>
                            <div style={{ width:10, height:10, borderRadius:"3px", background:s.color }} />
                            <span style={{ fontSize:13, fontWeight:600, color:"var(--text-muted)" }}>{s.label}</span>
                        </div>
                        <span style={{ fontSize:13, fontWeight:800, color:"var(--text-main)", fontFamily:"'DM Mono',monospace" }}>
                            {((s.value / total) * 100).toFixed(1)}%
                        </span>
                    </div>
                ))}
            </div>
        </div>
    );
}

export default function Dashboard() {
    const [liveOnline, setLiveOnline] = useState(1245);
    const [liveRevenue, setLiveRevenue] = useState(45231.50);
    const [chartData, setChartData] = useState([120, 150, 180, 140, 200, 250, 220, 300, 350, 320, 400, 450, 420, 500]);

    useEffect(() => {
        const interval = setInterval(() => {
            setLiveOnline(prev => prev + Math.floor(Math.random() * 5) - 2);
            setLiveRevenue(prev => prev + (Math.random() * 15));
            setChartData(prev => [...prev.slice(1), prev[prev.length-1] + (Math.random() * 40 - 15)]);
        }, 3000);
        return () => clearInterval(interval);
    }, []);

    const eventFeed = [
        { time: "14:42:15", type: "KICK", msg: "Отключение пользователя #85844 (Баланс: 0.00₽). Сессия закрыта." },
        { time: "14:40:02", type: "WARN", msg: "Сервер DE-Frankfurt-02: Загрузка TCP > 90%" },
        { time: "14:38:50", type: "PAY", msg: "Пополнение: Пользователь #11294 внес 500.00₽" },
        { time: "14:35:10", type: "NEW", msg: "Новая регистрация: @alex_dev (Реф. код: MAX50)" },
    ];

    const trafficData = [
        { label: "RU-Мост (Антиглуш)", value: 450, color: "#10b981" },
        { label: "Direct VLESS (NL)", value: 820, color: "#3b82f6" },
        { label: "Hysteria 2 (UDP)", value: 180, color: "#8b5cf6" },
    ];

    return (
        <div style={{ minHeight:"100vh", background:"var(--bg-base)", color:"var(--text-main)", padding:"40px", transition: "background 0.3s" }}>
            <style>{`
                .card { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 16px; box-shadow: var(--shadow-card); transition: transform 0.2s, box-shadow 0.2s, background 0.3s, border-color 0.3s; }
                .card:hover { transform: translateY(-2px); }
                .btn-action { display:inline-flex; align-items:center; gap:8px; padding:10px 16px; background:var(--bg-card); border:1px solid var(--border-color); border-radius:10px; font-size:13px; font-weight:700; color:var(--text-main); cursor:pointer; transition:all 0.2s; }
                .btn-action:hover { background:var(--bg-hover); border-color:var(--text-muted); }
                .btn-primary { background:linear-gradient(135deg, #2563eb, #1d4ed8); border:none; color:white; }
                .btn-primary:hover { background:linear-gradient(135deg, #1d4ed8, #1e40af); border:none; color:white; }
                
                @keyframes pulse-ring { 0% { box-shadow: 0 0 0 0 rgba(16,185,129,0.4); } 70% { box-shadow: 0 0 0 6px rgba(16,185,129,0); } 100% { box-shadow: 0 0 0 0 rgba(16,185,129,0); } }
                .live-dot { width:10px; height:10px; background:#10b981; border-radius:50%; animation: pulse-ring 2s infinite; display:inline-block; }
            `}</style>

            {/* --- ШАПКА --- */}
            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"flex-start", marginBottom:"32px" }}>
                <div>
                    <h2 style={{ fontSize:36, fontWeight:800, fontFamily:"'Space Grotesk',sans-serif", letterSpacing:"-0.03em", marginBottom:6 }}>GeoVPN Admin</h2>
                    <p style={{ fontSize:15, color:"var(--text-muted)", fontWeight:500 }}>Аналитика VPN-сети и биллинга в реальном времени</p>
                </div>
                <div style={{ display:"flex", gap:"12px" }}>
                    <button className="btn-action btn-primary"><Icons.UserPlus /> Создать юзера</button>
                    <button className="btn-action"><Icons.Server /> Добавить узел</button>
                </div>
            </div>

            {/* --- БЛОК 1: МЕТРИКИ --- */}
            <div style={{ display:"grid", gridTemplateColumns:"repeat(auto-fit, minmax(240px, 1fr))", gap:"20px", marginBottom:"24px" }}>
                {[
                    { title: "Онлайн прямо сейчас", val: liveOnline, icon: <Icons.Users />, sub: "+12 за последний час", live: true, format: (v:any) => v },
                    { title: "Выручка сегодня", val: liveRevenue, icon: <Icons.Wallet />, sub: "Списано из БД", color: "#3b82f6", format: (v:number) => `${v.toFixed(2)} ₽` },
                    { title: "Трафик за 24 часа", val: "3.4", icon: <Icons.Activity />, sub: "Пик: 1.4 Gbps", suffix: "TB" },
                    { title: "Health Status", val: "98.5", icon: <Icons.Server />, sub: "Общий балл инфраструктуры", color: "#10b981", suffix: "%" },
                ].map((m, i) => (
                    <div key={i} className="card" style={{ padding:"24px" }}>
                        <div style={{ display:"flex", justifyContent:"space-between", marginBottom:"16px", color:"var(--text-muted)" }}>
                            <span style={{ fontSize:13, fontWeight:700, textTransform:"uppercase", letterSpacing:"0.05em" }}>{m.title}</span>
                            {m.icon}
                        </div>
                        <div style={{ display:"flex", alignItems:"center", gap:"10px", marginBottom:"8px" }}>
                            <div style={{ fontSize:32, fontWeight:800, color: m.color || "var(--text-main)", fontFamily:"'Space Grotesk',sans-serif" }}>
                                {m.format ? m.format(m.val) : m.val} {m.suffix && <span style={{ fontSize:16, color:"var(--text-muted)" }}>{m.suffix}</span>}
                            </div>
                            {m.live && <span className="live-dot" title="Live update" />}
                        </div>
                        <div style={{ fontSize:13, color:"var(--text-muted)", fontWeight:500 }}>{m.sub}</div>
                    </div>
                ))}
            </div>

            {/* Остальные блоки аналогично наследуют var(--bg-card) и var(--text-main) */}
            <div style={{ display:"grid", gridTemplateColumns:"2fr 1fr", gap:"24px", marginBottom:"24px" }}>
                <div className="card" style={{ padding:"24px", display:"flex", flexDirection:"column" }}>
                    <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:"24px" }}>
                        <div>
                            <h3 style={{ fontSize:18, fontWeight:800, color:"var(--text-main)" }}>Радар доходов (Burn Rate) 💸</h3>
                            <p style={{ fontSize:13, color:"var(--text-muted)", marginTop:4 }}>Скорость списания копеек из TrafficCollectorJob</p>
                        </div>
                        <div style={{ textAlign:"right", background:"rgba(59,130,246,0.1)", border:"1px solid rgba(59,130,246,0.2)", padding:"8px 16px", borderRadius:"12px" }}>
                            <div style={{ fontSize:11, color:"#3b82f6", fontWeight:700, textTransform:"uppercase" }}>Скорость генерации</div>
                            <div style={{ fontSize:18, fontWeight:800, color:"#3b82f6", fontFamily:"'DM Mono',monospace" }}>~850 ₽ / час</div>
                        </div>
                    </div>
                    <div style={{ flex:1, minHeight:"200px", position:"relative", marginTop:"10px" }}>
                        <AreaChart data={chartData} />
                    </div>
                </div>

                <div className="card" style={{ padding:"24px", display:"flex", flexDirection:"column" }}>
                    <h3 style={{ fontSize:16, fontWeight:800, marginBottom:"4px" }}>Архитектура трафика</h3>
                    <p style={{ fontSize:13, color:"var(--text-muted)", marginBottom:"24px", fontWeight:500 }}>Нагрузка по протоколам (24ч)</p>
                    <div style={{ flex:1, display:"flex", alignItems:"center" }}>
                        <DonutChart segments={trafficData} />
                    </div>
                </div>
            </div>

            {/* Аномалии и Фид событий */}
            <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr 1fr", gap:"24px" }}>
                <div className="card" style={{ padding:"24px" }}>
                    <div style={{ display:"flex", alignItems:"center", gap:"8px", marginBottom:"20px" }}>
                        <div style={{ color:"#2563eb" }}><Icons.TrendingUp /></div>
                        <h3 style={{ fontSize:16, fontWeight:800 }}>Business Insights</h3>
                    </div>
                    <div style={{ marginBottom:"20px" }}>
                        <div style={{ fontSize:12, fontWeight:700, color:"var(--text-muted)", textTransform:"uppercase", marginBottom:6 }}>Прогноз выручки (MRR)</div>
                        <div style={{ fontSize:28, fontWeight:800, color:"var(--text-main)", fontFamily:"'Space Grotesk',sans-serif", marginBottom:8 }}>~ 425,000 ₽ <span style={{ fontSize:14, color:"#10b981", fontFamily:"'Inter'" }}>↑ 18%</span></div>
                        <div style={{ height:6, background:"var(--border-color)", borderRadius:3, overflow:"hidden" }}>
                            <div style={{ height:"100%", width:"65%", background:"linear-gradient(90deg, #3b82f6, #10b981)", borderRadius:3 }}/>
                        </div>
                    </div>
                </div>

                <div className="card" style={{ padding:"24px", background:"rgba(253, 230, 138, 0.05)", borderColor:"rgba(217, 119, 6, 0.3)" }}>
                    <div style={{ display:"flex", alignItems:"center", gap:"8px", marginBottom:"20px" }}>
                        <div style={{ color:"#d97706" }}><Icons.ShieldAlert /></div>
                        <h3 style={{ fontSize:16, fontWeight:800, color:"#d97706" }}>Мониторинг Аномалий</h3>
                    </div>
                    <div style={{ background:"var(--bg-card)", border:"1px solid rgba(217, 119, 6, 0.3)", borderRadius:"12px", padding:"16px", marginBottom:"12px" }}>
                        <div style={{ fontSize:12, fontWeight:700, color:"#d97706", textTransform:"uppercase", marginBottom:8 }}>🔥 Топ сжигателей трафика</div>
                        <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center" }}>
                            <span style={{ fontSize:13, fontWeight:600, color:"var(--text-muted)" }}>ID: 85844 (iOS)</span>
                            <span style={{ fontSize:13, fontWeight:800, color:"#dc2626", fontFamily:"'DM Mono',monospace" }}>18.4 GB/ч</span>
                        </div>
                    </div>
                </div>

                <div className="card" style={{ padding:"24px", display:"flex", flexDirection:"column" }}>
                    <div style={{ display:"flex", alignItems:"center", gap:"8px", marginBottom:"20px" }}>
                        <div style={{ color:"#dc2626" }}><Icons.Zap /></div>
                        <h3 style={{ fontSize:16, fontWeight:800, color:"var(--text-main)" }}>God Mode</h3>
                    </div>
                    <p style={{ fontSize:13, color:"var(--text-muted)", fontWeight:500, marginBottom:"20px", lineHeight:1.4 }}>
                        Быстрые административные действия для управления кластером и кэшем.
                    </p>
                    <div style={{ display:"flex", flexDirection:"column", gap:"10px", marginTop:"auto" }}>
                        <button className="btn-action" style={{ width:"100%", justifyContent:"center" }}>
                            <Icons.Refresh /> Сбросить Redis Cache
                        </button>
                    </div>
                </div>
            </div>

        </div>
    )
}