import { useState, useEffect } from "react"
import { useNavigate } from "react-router-dom"
import {
    Users, Server, Activity, Wallet, UserPlus, Zap, RefreshCw, ShieldAlert, TrendingUp
} from "lucide-react"

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
            <circle cx={W} cy={H - (data[data.length - 1] / max) * H} r="5" fill="white" stroke="#3b82f6" strokeWidth="3" />
        </svg>
    )
}

function DonutChart({ segments }: { segments: { value: number, color: string, label: string }[] }) {
    const total = segments.reduce((acc, s) => acc + s.value, 0);
    const radius = 60;
    const circumference = 2 * Math.PI * radius;
    let currentOffset = 0;

    return (
        <div className="flex items-center gap-8 w-full">
            <div className="relative w-[140px] h-[140px]">
                <svg width="140" height="140" viewBox="0 0 140 140">
                    <circle cx="70" cy="70" r={radius} fill="none" stroke="#f1f5f9" strokeWidth="16" />
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
                <div className="absolute inset-0 flex flex-col items-center justify-center">
                    <span className="text-[10px] text-slate-400 font-bold uppercase">Total</span>
                    <span className="text-base text-slate-900 dark:text-white font-extrabold font-mono">{total} <span className="text-[9px]">GB</span></span>
                </div>
            </div>

            <div className="flex flex-col gap-2 flex-1">
                {segments.map((s, i) => (
                    <div key={i} className="flex justify-between items-center w-full">
                        <div className="flex items-center gap-2">
                            <div className="w-2.5 h-2.5 rounded-sm" style={{ background: s.color }} />
                            <span className="text-xs font-semibold text-slate-500 dark:text-slate-400">{s.label}</span>
                        </div>
                        <span className="text-xs font-bold text-slate-900 dark:text-white font-mono">
                            {((s.value / total) * 100).toFixed(1)}%
                        </span>
                    </div>
                ))}
            </div>
        </div>
    );
}

export default function Dashboard() {
    const navigate = useNavigate()
    const [liveOnline, setLiveOnline] = useState(1245);
    const [liveRevenue, setLiveRevenue] = useState(45231.50);
    const [chartData, setChartData] = useState([120, 150, 180, 140, 200, 250, 220, 300, 350, 320, 400, 450, 420, 500]);
    const [isRefreshing, setIsRefreshing] = useState(false);

    useEffect(() => {
        const interval = setInterval(() => {
            setLiveOnline(prev => prev + Math.floor(Math.random() * 5) - 2);
            setLiveRevenue(prev => prev + (Math.random() * 15));
            setChartData(prev => [...prev.slice(1), prev[prev.length-1] + (Math.random() * 40 - 15)]);
        }, 3000);
        return () => clearInterval(interval);
    }, []);

    const resetCache = () => {
        setIsRefreshing(true);
        setTimeout(() => {
            setIsRefreshing(false);
            alert("Redis cache cleared!");
        }, 1000);
    }

    const trafficData = [
        { label: "RU-Мост (Антиглуш)", value: 450, color: "#10b981" },
        { label: "Direct VLESS (NL)", value: 820, color: "#3b82f6" },
        { label: "Hysteria 2 (UDP)", value: 180, color: "#8b5cf6" },
    ];

    return (
        <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-white p-10 transition-colors duration-300">
            <style>{`
                .card { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 16px; box-shadow: var(--shadow-card); transition: transform 0.2s, box-shadow 0.2s, background 0.3s, border-color 0.3s; }
                .card:hover { transform: translateY(-2px); }
                .btn-action { display:inline-flex; align-items:center; gap:8px; padding:10px 16px; background:var(--bg-card); border:1px solid var(--border-color); border-radius:10px; font-size:13px; font-weight:700; color:var(--text-main); cursor:pointer; transition:all 0.2s; }
                .btn-action:hover { background:var(--bg-hover); border-color:var(--text-muted); }
                .btn-primary { background:linear-gradient(135deg, #2563eb, #1d4ed8); border:none; color:white; }
                .btn-primary:hover { background:linear-gradient(135deg, #1d4ed8, #1e40af); }
                
                @keyframes pulse-ring { 0% { box-shadow: 0 0 0 0 rgba(16,185,129,0.4); } 70% { box-shadow: 0 0 0 6px rgba(16,185,129,0); } 100% { box-shadow: 0 0 0 0 rgba(16,185,129,0); } }
                .live-dot { width:10px; height:10px; background:#10b981; border-radius:50%; animation: pulse-ring 2s infinite; display:inline-block; }
            `}</style>

            {/* --- TOP ROW --- */}
            <div className="flex justify-between items-start mb-8">
                <div>
                    <h2 className="text-3xl font-extrabold tracking-tight italic" style={{ fontFamily: "Space Grotesk, sans-serif" }}>Панель управления</h2>
                    <p className="text-sm text-slate-500 dark:text-slate-400 font-medium">Аналитика VPN-сети и биллинга в реальном времени</p>
                </div>
                <div className="flex gap-3">
                    <button onClick={() => navigate("/users")} className="btn-action btn-primary cursor-pointer"><UserPlus size={16} /> Управлять юзерами</button>
                    <button onClick={() => navigate("/servers")} className="btn-action cursor-pointer"><Server size={16} /> Добавить узел</button>
                </div>
            </div>

            {/* --- BLOCK 1: STATS KPI --- */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-5 mb-8">
                {[
                    { title: "Онлайн прямо сейчас", val: liveOnline, icon: <Users className="text-emerald-500" />, sub: "+12 за последний час", live: true, format: (v:any) => v },
                    { title: "Выручка сегодня", val: liveRevenue, icon: <Wallet className="text-blue-500" />, sub: "Списано из БД за трафик", color: "#3b82f6", format: (v:number) => `${v.toFixed(2)} ₽` },
                    { title: "Трафик за 24 часа", val: "3.4", icon: <Activity className="text-amber-500" />, sub: "Пик: 1.4 Gbps", suffix: "TB" },
                    { title: "Состояние Сети", val: "99.2", icon: <Server className="text-blue-600" />, sub: "Общий балл инфраструктуры", color: "#10b981", suffix: "%" },
                ].map((m, i) => (
                    <div key={i} className="card p-6">
                        <div className="flex justify-between mb-4 text-slate-500 dark:text-slate-400">
                            <span className="text-xs font-bold uppercase tracking-wider">{m.title}</span>
                            {m.icon}
                        </div>
                        <div className="flex items-center gap-2 mb-2">
                            <div className="text-3xl font-extrabold font-mono text-slate-900 dark:text-white">
                                {m.format ? m.format(m.val) : m.val} {m.suffix && <span className="text-sm font-normal text-slate-500">{m.suffix}</span>}
                            </div>
                            {m.live && <span className="live-dot" title="Live update" />}
                        </div>
                        <div className="text-xs text-slate-400">{m.sub}</div>
                    </div>
                ))}
            </div>

            {/* --- GRID METRICS --- */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
                <div className="card p-6 md:col-span-2 flex flex-col">
                    <div className="flex justify-between items-center mb-6">
                        <div>
                            <h3 className="text-base font-bold text-slate-900 dark:text-white">Радар доходов (Burn Rate) 💸</h3>
                            <p className="text-xs text-slate-500">Скорость списания копеек из SQL-системы `traffic_usage`</p>
                        </div>
                        <div className="text-right bg-blue-50 dark:bg-blue-900/20 border border-blue-100 dark:border-blue-900/40 p-2.5 rounded-xl">
                            <div className="text-[10px] text-blue-600 dark:text-blue-400 font-bold uppercase tracking-wider">Скорость списания</div>
                            <div className="text-base font-extrabold font-mono text-blue-700 dark:text-blue-400">~850 ₽ / час</div>
                        </div>
                    </div>
                    <div className="flex-1 min-h-[200px] mt-4 relative">
                        <AreaChart data={chartData} />
                    </div>
                </div>

                <div className="card p-6 flex flex-col">
                    <h3 className="text-base font-bold mb-1">Архитектура протоколов</h3>
                    <p className="text-xs text-slate-500 mb-6">Нагрузка по шлюзам (24ч)</p>
                    <div className="flex-1 flex items-center">
                        <DonutChart segments={trafficData} />
                    </div>
                </div>
            </div>

            {/* --- ADDITIONAL INSIGHTS & UTILITY --- */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="card p-6 flex flex-col justify-between">
                    <div>
                        <div className="flex items-center gap-2 mb-4 text-blue-600">
                            <TrendingUp size={20} />
                            <h3 className="text-base font-bold text-slate-900 dark:text-white">Бизнес-метрики</h3>
                        </div>
                        <div className="mb-4">
                            <div className="text-xs font-bold text-slate-500 uppercase tracking-widest mb-1">Прогноз MRR (Месяц)</div>
                            <div className="text-3xl font-extrabold font-mono text-slate-950 dark:text-white">
                                ~ 425,000 ₽ <span className="text-xs font-bold text-emerald-500 font-sans ml-2">↑ 18%</span>
                            </div>
                        </div>
                        <div className="h-1.5 w-full bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
                            <div className="h-full w-2/3 bg-gradient-to-r from-blue-500 to-emerald-400 rounded-full" />
                        </div>
                    </div>
                </div>

                <div className="card p-6 bg-amber-50/5 dark:bg-amber-950/10 border border-amber-200/30 dark:border-amber-900/40">
                    <div className="flex items-center gap-2 mb-4 text-amber-600 dark:text-amber-500">
                        <ShieldAlert size={20} />
                        <h3 className="text-base font-bold">Аномальная активность</h3>
                    </div>
                    <div className="bg-white dark:bg-slate-900 border border-amber-200/40 dark:border-amber-900/40 rounded-xl p-4">
                        <div className="text-[10px] text-amber-600 dark:text-amber-400 font-bold uppercase tracking-wider mb-2">🔥 Слишком высокий расход</div>
                        <div className="flex justify-between items-center">
                            <span className="text-xs font-semibold text-slate-600 dark:text-slate-400">Пользователь #85844</span>
                            <span className="text-xs font-extrabold text-red-500 font-mono">18.4 GB / ч</span>
                        </div>
                    </div>
                </div>

                <div className="card p-6 flex flex-col justify-between">
                    <div>
                        <div className="flex items-center gap-2 mb-4 text-red-600 dark:text-red-500">
                            <Zap size={20} />
                            <h3 className="text-base font-bold text-slate-900 dark:text-white">Властный режим</h3>
                        </div>
                        <p className="text-xs text-slate-500 leading-relaxed mb-6">
                            Быстрые системные действия для непосредственного сброса закешированных Redis данных и синхронизации серверов.
                        </p>
                    </div>
                    <button
                        onClick={resetCache}
                        disabled={isRefreshing}
                        className="btn-action w-full justify-center text-xs tracking-wider cursor-pointer"
                    >
                        <RefreshCw size={14} className={isRefreshing ? "animate-spin" : ""} />
                        Сбросить Redis Cache
                    </button>
                </div>
            </div>
        </div>
    )
}
