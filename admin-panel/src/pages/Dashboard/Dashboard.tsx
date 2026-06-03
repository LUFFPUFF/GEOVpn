import { useState, useEffect, useMemo, useRef } from "react"
import { useNavigate } from "react-router-dom"
import {
    Users, Server, Activity, Wallet, UserPlus, Zap, RefreshCw, CheckCircle, TrendingUp, X, Terminal
} from "lucide-react"
import { adminApi } from "@/api/admin"
import { AdminDashboardResponse, SystemHealthDto, RevenueStat } from "@/types/api"

interface ChartPoint {
    label: string;
    value: number;
    fullDate: string;
}

interface LogMessage {
    timestamp: string;
    level: "INFO" | "WARN" | "ERROR";
    message: string;
}

interface MaintenanceReport {
    usersProcessed: number;
    usersFailed: number;
    serversCleared: number;
    elapsedSeconds: number;
}

function InteractiveAreaChart({ points }: { points: ChartPoint[] }) {
    const [hoveredIdx, setHoveredIdx] = useState<number | null>(null)
    const [mousePos, setMousePos] = useState({ x: 0, y: 0 })

    if (points.length < 2) {
        return <div className="h-full flex items-center justify-center text-xs text-slate-400">Недостаточно данных</div>
    }

    const maxVal = Math.max(...points.map(p => p.value), 100);
    const minVal = 0;

    const W = 800;
    const H = 200;
    const paddingLeft = 50;
    const paddingRight = 20;
    const paddingTop = 20;
    const paddingBottom = 30;

    const chartWidth = W - paddingLeft - paddingRight;
    const chartHeight = H - paddingTop - paddingBottom;

    const svgPoints = points.map((p, i) => {
        const x = paddingLeft + (i / (points.length - 1)) * chartWidth;
        const y = paddingTop + chartHeight - (p.value / maxVal) * chartHeight;
        return { x, y, ...p }
    });

    const ptsStr = svgPoints.map(p => `${p.x},${p.y}`).join(" ");

    const yGridLevels = [0, 0.33, 0.66, 1].map(pct => {
        const val = minVal + pct * (maxVal - minVal);
        const y = paddingTop + chartHeight - pct * chartHeight;
        return { y, val }
    });

    const handleMouseMove = (e: React.MouseEvent<SVGSVGElement>) => {
        const svg = e.currentTarget;
        const rect = svg.getBoundingClientRect();
        const mouseX = ((e.clientX - rect.left) / rect.width) * W;
        const mouseY = ((e.clientY - rect.top) / rect.height) * H;
        setMousePos({ x: e.clientX - rect.left, y: e.clientY - rect.top });

        let closestIdx = 0;
        let minDist = Infinity;
        svgPoints.forEach((p, idx) => {
            const dist = Math.abs(p.x - mouseX);
            if (dist < minDist) {
                minDist = dist;
                closestIdx = idx;
            }
        });
        setHoveredIdx(closestIdx);
    };

    const activePoint = hoveredIdx !== null && svgPoints[hoveredIdx] ? svgPoints[hoveredIdx] : null;

    return (
        <div className="relative w-full h-full group" onMouseLeave={() => setHoveredIdx(null)}>
            <svg
                width="100%"
                height="100%"
                viewBox={`0 0 ${W} ${H}`}
                preserveAspectRatio="none"
                style={{ overflow: "visible" }}
                onMouseMove={handleMouseMove}
            >
                <defs>
                    <linearGradient id="chartGrad" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="#3b82f6" stopOpacity="0.25"/>
                        <stop offset="100%" stopColor="#3b82f6" stopOpacity="0"/>
                    </linearGradient>
                </defs>

                {yGridLevels.map((lvl, idx) => (
                    <g key={idx}>
                        <line
                            x1={paddingLeft} y1={lvl.y} x2={W - paddingRight} y2={lvl.y}
                            stroke="#e2e8f0" strokeDasharray="4 4" strokeWidth="1"
                            className="dark:stroke-slate-800"
                        />
                        <text
                            x={paddingLeft - 8} y={lvl.y + 4}
                            textAnchor="end"
                            className="text-[10px] font-mono font-bold fill-slate-400"
                        >
                            {Math.round(lvl.val)} ₽
                        </text>
                    </g>
                ))}

                <polygon points={`${paddingLeft},${H - paddingBottom} ${ptsStr} ${W - paddingRight},${H - paddingBottom}`} fill="url(#chartGrad)" />

                <polyline points={ptsStr} fill="none" stroke="#3b82f6" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"/>

                {svgPoints.map((p, i) => {
                    const skipFactor = points.length > 10 ? 5 : 2;
                    if (i % skipFactor !== 0 && i !== points.length - 1) return null;

                    return (
                        <text
                            key={i} x={p.x} y={H - 8}
                            textAnchor="middle"
                            className="text-[9px] font-mono font-bold fill-slate-400"
                        >
                            {p.label}
                        </text>
                    );
                })}

                {activePoint && (
                    <g>
                        <line
                            x1={activePoint.x} y1={paddingTop}
                            x2={activePoint.x} y2={H - paddingBottom}
                            stroke="#3b82f6" strokeWidth="1.5" strokeDasharray="2 2"
                        />
                        <circle
                            cx={activePoint.x} cy={activePoint.y}
                            r="6" fill="#3b82f6" stroke="white" strokeWidth="2.5"
                        />
                    </g>
                )}
            </svg>

            {activePoint && (
                <div
                    className="absolute bg-slate-900 border border-slate-800 text-white p-3 rounded-xl shadow-2xl text-[11px] font-mono pointer-events-none z-50 flex flex-col gap-0.5"
                    style={{ left: `${mousePos.x + 15}px`, top: `${mousePos.y - 65}px` }}
                >
                    <span className="text-slate-400 text-[9px] font-bold">{activePoint.fullDate}</span>
                    <span className="font-extrabold text-blue-400 text-xs">Доход: {activePoint.value} ₽</span>
                </div>
            )}
        </div>
    )
}

function DonutChart({ totalTraffic }: { totalTraffic: number }) {
    const segments = [
        { value: totalTraffic * 0.6, color: "#3b82f6", label: "Direct VLESS (NL/DE)" },
        { value: totalTraffic * 0.3, color: "#10b981", label: "RU-Мост (Антиглуш)" },
        { value: totalTraffic * 0.1, color: "#8b5cf6", label: "Hysteria 2 (UDP)" },
    ];

    const total = segments.reduce((acc, s) => acc + s.value, 0) || 1;
    const radius = 60;
    const circumference = 2 * Math.PI * radius;
    let currentOffset = 0;

    return (
        <div className="flex items-center gap-8 w-full">
            <div className="relative w-[140px] h-[140px] shrink-0">
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
                    <span className="text-[10px] text-slate-400 font-bold uppercase">Трафик</span>
                    <span className="text-sm text-slate-900 dark:text-white font-black font-mono">
                        {total > 1000 ? `${(total/1000).toFixed(1)} TB` : `${total.toFixed(0)} GB`}
                    </span>
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
                            {s.value.toFixed(1)} GB
                        </span>
                    </div>
                ))}
            </div>
        </div>
    );
}

export default function Dashboard() {
    const navigate = useNavigate()

    const [stats, setStats] = useState<AdminDashboardResponse | null>(null)
    const [health, setHealth] = useState<SystemHealthDto | null>(null)
    const [revenueData, setRevenueData] = useState<RevenueStat[]>([])
    const [loading, setLoading] = useState(true)

    const [liveOnline, setLiveOnline] = useState(0);
    const [daysRange, setDaysRange] = useState<7 | 30>(7)
    const [isRefreshing, setIsRefreshing] = useState(false);

    // Модальное окно и логгирование
    const [isConsoleOpen, setIsConsoleOpen] = useState(false);
    const [consoleLogs, setConsoleLogs] = useState<LogMessage[]>([]);
    const [logFilter, setLogFilter] = useState<"ALL" | "INFO" | "WARN" | "ERROR">("ALL"); // Фильтр логов
    const [maintenanceReport, setMaintenanceReport] = useState<MaintenanceReport | null>(null);
    const consoleEndRef = useRef<HTMLDivElement>(null);
    const sseRef = useRef<EventSource | null>(null);

    const loadDashboardData = async () => {
        try {
            const [statsData, healthData, revenueStats] = await Promise.all([
                adminApi.getDashboard(),
                adminApi.getInfrastructureHealth().catch(() => null),
                adminApi.getRevenueStats(daysRange)
            ])
            setStats(statsData)
            setHealth(healthData)
            setRevenueData(revenueStats)
            setLiveOnline(statsData.activeSubscriptions || 0)
        } catch (e) {
            console.error("Failed to load dashboard metrics:", e)
        }
        setLoading(false)
    }

    useEffect(() => {
        loadDashboardData()
    }, [daysRange]);

    // ИСПРАВЛЕНО: Добавлен упущенный useMemo для построения графика
    const chartPoints = useMemo<ChartPoint[]>(() => {
        return revenueData.map(r => {
            const [year, month, day] = r.date.split("-");
            return {
                label: `${day}.${month}`,
                value: r.amountRub,
                fullDate: r.date
            }
        });
    }, [revenueData])

    // Фильтрация логов на основе выбранной вкладки
    const filteredLogs = useMemo(() => {
        if (logFilter === "ALL") return consoleLogs;
        return consoleLogs.filter(log => log.level === logFilter);
    }, [consoleLogs, logFilter]);

    // Автопрокрутка консоли вниз при появлении новых отфильтрованных логов
    useEffect(() => {
        if (consoleEndRef.current) {
            consoleEndRef.current.scrollIntoView({ behavior: "smooth" });
        }
    }, [filteredLogs]);

    // Запуск процесса глобального сброса и синхронизации
    const resetCacheAndSync = async () => {
        setIsRefreshing(true);
        setConsoleLogs([]);
        setLogFilter("ALL");
        setMaintenanceReport(null);
        setIsConsoleOpen(true);

        try {
            const eventSourceUrl = `${import.meta.env.VITE_API_BASE_URL || "/api/v1"}/configs/admin/maintenance/stream`;
            const sse = new EventSource(eventSourceUrl);
            sseRef.current = sse;

            sse.addEventListener("log", (event) => {
                const logData: LogMessage = JSON.parse(event.data);
                setConsoleLogs(prev => [...prev, logData]);
            });

            sse.addEventListener("report", (event) => {
                const report: MaintenanceReport = JSON.parse(event.data);
                setMaintenanceReport(report);
                sse.close();
            });

            sse.onerror = () => {
                setConsoleLogs(prev => [...prev, {
                    timestamp: new Date().toISOString(),
                    level: "ERROR",
                    message: "Соединение с сервером логов потеряно. Процесс может продолжаться асинхронно."
                }]);
                sse.close();
            };

            await adminApi.forceSync();

        } catch (e) {
            setConsoleLogs(prev => [...prev, {
                timestamp: new Date().toISOString(),
                level: "ERROR",
                message: "Критическая ошибка инициализации процесса на бэкенде."
            }]);
        }
        setIsRefreshing(false);
    }

    const closeConsole = () => {
        if (sseRef.current) {
            sseRef.current.close();
        }
        setIsConsoleOpen(false);
        loadDashboardData();
    }

    return (
        <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-white p-10 transition-colors duration-300">
            <style>{`
                .card { background: white; border: 1px solid #e2e8f0; border-radius: 24px; box-shadow: 0 4px 20px -2px rgba(148, 163, 184, 0.08); transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
                .dark .card { background: #09090B; border-color: #1F2937; box-shadow: none; }
                .card:hover { transform: translateY(-3px); box-shadow: 0 10px 30px -3px rgba(148, 163, 184, 0.12); }
                .btn-action { display:inline-flex; align-items:center; gap:8px; padding:12px 20px; background:white; border:1px solid #e2e8f0; border-radius:12px; font-size:13px; font-weight:800; color:#0f172a; cursor:pointer; transition:all 0.2s; }
                .dark .btn-action { background:#18181B; border-color:#27272A; color:white; }
                .btn-action:hover { background:#f8fafc; border-color:#cbd5e1; }
                .dark .btn-action:hover { background:#27272A; }
                .btn-primary { background:linear-gradient(135deg, #2563eb, #1d4ed8); border:none; color:white; }
                .btn-primary:hover { background:linear-gradient(135deg, #1d4ed8, #1e40af); }
                
                @keyframes pulse-ring { 0% { box-shadow: 0 0 0 0 rgba(16,185,129,0.4); } 70% { box-shadow: 0 0 0 8px rgba(16,185,129,0); } 100% { box-shadow: 0 0 0 0 rgba(16,185,129,0); } }
                .live-dot { width:10px; height:10px; background:#10b981; border-radius:50%; animation: pulse-ring 2s infinite; display:inline-block; }
            `}</style>

            <div className="flex justify-between items-start mb-8">
                <div>
                    <h2 className="text-4xl font-black tracking-tight italic" style={{ fontFamily: "Space Grotesk, sans-serif" }}>Панель управления</h2>
                    <p className="text-xs text-slate-400 font-bold uppercase tracking-wider mt-1">Аналитика VPN-сети и биллинга в реальном времени</p>
                </div>
                <div className="flex gap-3">
                    <button onClick={() => navigate("/users")} className="btn-action btn-primary cursor-pointer"><UserPlus size={16} /> Управлять юзерами</button>
                    <button onClick={() => navigate("/servers")} className="btn-action cursor-pointer"><Server size={16} /> Добавить узел</button>
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-4 gap-5 mb-8">
                {[
                    { title: "Активные подписки (Онлайн)", val: liveOnline, icon: <Users className="text-emerald-500" />, sub: `Всего пользователей: ${stats?.totalUsers || 0}`, live: true, format: (v:any) => v },
                    { title: "Оборот всей сети", val: stats?.totalBalanceRub || 0, icon: <Wallet className="text-blue-500" />, sub: "Суммарный баланс кошельков", color: "#3b82f6", format: (v:number) => `${v.toLocaleString("ru-RU")} ₽` },
                    { title: "Расход трафика", val: stats?.totalTrafficGb || 0, icon: <Activity className="text-amber-500" />, sub: "Суммарно пропущено через мосты", suffix: "GB" },
                    { title: "Активные узлы", val: `${stats?.activeServers || 0} / ${stats?.totalServers || 0}`, icon: <Server className="text-blue-600" />, sub: "Состояние gRPC туннелей", color: "#10b981" },
                ].map((m, i) => (
                    <div key={i} className="card p-6 flex flex-col justify-between">
                        <div className="flex justify-between mb-4 text-slate-500 dark:text-slate-400">
                            <span className="text-[10px] font-black uppercase tracking-wider">{m.title}</span>
                            {m.icon}
                        </div>
                        <div className="flex items-center gap-2 mb-2">
                            <div className="text-3.5xl font-black font-mono tracking-tighter text-slate-900 dark:text-white">
                                {m.format ? m.format(m.val) : m.val} {m.suffix && <span className="text-sm font-normal text-slate-500">{m.suffix}</span>}
                            </div>
                            {m.live && <span className="live-dot" title="Live update" />}
                        </div>
                        <div className="text-xs text-slate-400 font-bold">{m.sub}</div>
                    </div>
                ))}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
                <div className="card p-6 md:col-span-2 flex flex-col">
                    <div className="flex justify-between items-center mb-6">
                        <div>
                            <h3 className="text-base font-extrabold text-slate-900 dark:text-white">Радар успешных пополнений 📈</h3>
                            <p className="text-xs text-slate-500 font-semibold">Суммарный объем успешных платежей Platega посуточно</p>
                        </div>

                        <div className="flex bg-slate-100 dark:bg-slate-900 p-1 rounded-xl gap-1">
                            <button
                                onClick={() => setDaysRange(7)}
                                className={`px-3 py-1.5 text-[10px] font-black uppercase tracking-wider rounded-lg transition-all cursor-pointer ${daysRange === 7 ? 'bg-white text-blue-600 shadow-sm' : 'text-slate-400 hover:text-slate-600'}`}
                            >
                                7 дней
                            </button>
                            <button
                                onClick={() => setDaysRange(30)}
                                className={`px-3 py-1.5 text-[10px] font-black uppercase tracking-wider rounded-lg transition-all cursor-pointer ${daysRange === 30 ? 'bg-white text-blue-600 shadow-sm' : 'text-slate-400 hover:text-slate-600'}`}
                            >
                                30 дней
                            </button>
                        </div>
                    </div>

                    <div className="flex-1 min-h-[220px] mt-4 relative">
                        <InteractiveAreaChart points={chartPoints} />
                    </div>
                </div>

                <div className="card p-6 flex flex-col">
                    <h3 className="text-base font-extrabold mb-1 text-slate-900 dark:text-white">Архитектура протоколов</h3>
                    <p className="text-xs text-slate-500 mb-6 font-semibold">Нагрузка по шлюзам (24ч)</p>
                    <div className="flex-1 flex items-center">
                        <DonutChart totalTraffic={stats?.totalTrafficGb || 350} />
                    </div>
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">

                <div className="card p-6 flex flex-col justify-between">
                    <div>
                        <div className="flex items-center gap-2 mb-4 text-blue-600">
                            <TrendingUp size={20} />
                            <h3 className="text-base font-extrabold text-slate-900 dark:text-white">Бизнес-метрики</h3>
                        </div>
                        <div className="mb-4">
                            <div className="text-xs font-bold text-slate-500 uppercase tracking-widest mb-1">Прогноз MRR (Месяц)</div>
                            <div className="text-3xl font-black font-mono text-slate-950 dark:text-white tracking-tight">
                                ~ {stats ? ((stats.totalBalanceRub * 12).toLocaleString("ru-RU")) : "425,000"} ₽
                                <span className="text-xs font-bold text-emerald-500 font-sans ml-2">↑ 18%</span>
                            </div>
                        </div>
                        <div className="h-1.5 w-full bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
                            <div className="h-full w-2/3 bg-gradient-to-r from-blue-500 to-emerald-400 rounded-full" />
                        </div>
                    </div>
                </div>

                <div className="card p-6 bg-slate-50/50 dark:bg-slate-900/10 border border-slate-200/50 dark:border-slate-800">
                    <div className="flex items-center gap-2 mb-4 text-emerald-600 dark:text-emerald-500">
                        <CheckCircle size={20} />
                        <h3 className="text-base font-extrabold text-slate-900 dark:text-white">Здоровье кластера</h3>
                    </div>

                    <div className="space-y-2.5 max-h-40 overflow-y-auto pr-1 custom-scrollbar">
                        {health && health.services ? (
                            Object.entries(health.services).map(([name, svc]) => (
                                <div key={name} className="bg-white dark:bg-[#121214] border border-slate-200/60 dark:border-slate-800/80 rounded-xl p-3 flex justify-between items-center text-xs">
                                    <div>
                                        <p className="font-bold text-slate-800 dark:text-slate-200">{name}</p>
                                        <p className="text-[10px] text-slate-400 font-medium font-sans">{svc.details || "Active status"}</p>
                                    </div>
                                    <span className={`px-2 py-0.5 rounded text-[9px] font-black uppercase tracking-wider border ${
                                        svc.status === "UP" ? "bg-emerald-50 text-emerald-600 border-emerald-100" : "bg-rose-50 text-rose-600 border-rose-100"
                                    }`}>
                                        {svc.status}
                                    </span>
                                </div>
                            ))
                        ) : (
                            <div className="text-center py-8 text-xs text-slate-400 italic">Служба мониторинга недоступна</div>
                        )}
                    </div>
                </div>

                <div className="card p-6 flex flex-col justify-between">
                    <div>
                        <div className="flex items-center gap-2 mb-4 text-red-600 dark:text-red-500">
                            <Zap size={20} />
                            <h3 className="text-base font-extrabold text-slate-900 dark:text-white">Властный режим</h3>
                        </div>
                        <p className="text-xs text-slate-500 leading-relaxed mb-6">
                            Быстрые системные действия для форсирования синхронизации всех XUI панелей и немедленного обновления кэша gRPC.
                        </p>
                    </div>
                    <button
                        onClick={resetCacheAndSync}
                        disabled={isRefreshing}
                        className="btn-action w-full justify-center text-xs tracking-wider cursor-pointer font-extrabold uppercase"
                    >
                        <RefreshCw size={14} className={isRefreshing ? "animate-spin" : ""} />
                        Сбросить Redis & Sync
                    </button>
                </div>
            </div>

            {/* ВСПЛЫВАЮЩЕЕ ОКНО КОНСОЛИ (SSE LOGS TERMINAL) */}
            {isConsoleOpen && (
                <div className="fixed inset-0 bg-black/70 backdrop-blur-md flex items-center justify-center z-50 p-6 animate-fade-in">
                    <div className="bg-slate-950 border border-slate-800 w-full max-w-4xl rounded-2xl overflow-hidden shadow-2xl flex flex-col max-h-[85vh]">
                        {/* ХЕДЕР ТЕРМИНАЛА С ФИЛЬТРАЦИЕЙ ЛОГОВ */}
                        <div className="bg-slate-900 border-b border-slate-800 p-4 flex flex-col sm:flex-row justify-between items-center gap-3">
                            <div className="flex items-center gap-2">
                                <div className="flex gap-1.5 mr-2">
                                    <span className="w-3.5 h-3.5 rounded-full bg-rose-500 inline-block" />
                                    <span className="w-3.5 h-3.5 rounded-full bg-amber-500 inline-block" />
                                    <span className="w-3.5 h-3.5 rounded-full bg-emerald-500 inline-block" />
                                </div>
                                <Terminal size={16} className="text-blue-400" />
                                <span className="font-mono text-sm font-black text-slate-200">global_maintenance_scheduler.sh</span>
                            </div>

                            {/* ВКЛАДКИ ФИЛЬТРАЦИИ ЛОГОВ ПО УРОВНЯМ */}
                            <div className="flex bg-black/40 border border-slate-800/80 p-0.5 rounded-lg gap-1 font-mono text-[9px] font-black uppercase tracking-wider">
                                {(["ALL", "INFO", "WARN", "ERROR"] as const).map(level => {
                                    const count = consoleLogs.filter(l => level === "ALL" || l.level === level).length;
                                    const isActive = logFilter === level;

                                    let activeColor = "text-blue-400 border-blue-500/20 bg-blue-500/5";
                                    if (level === "INFO") activeColor = "text-slate-300 border-slate-700/50 bg-slate-800/20";
                                    if (level === "WARN") activeColor = "text-amber-400 border-amber-500/20 bg-amber-500/5";
                                    if (level === "ERROR") activeColor = "text-rose-500 border-rose-500/20 bg-rose-500/5";

                                    return (
                                        <button
                                            key={level}
                                            onClick={() => setLogFilter(level)}
                                            className={`px-2.5 py-1 rounded-md border transition-all cursor-pointer ${
                                                isActive
                                                    ? `border-solid ${activeColor} shadow-inner`
                                                    : "border-transparent text-slate-500 hover:text-slate-300"
                                            }`}
                                        >
                                            {level} ({count})
                                        </button>
                                    );
                                })}
                            </div>

                            <button
                                onClick={closeConsole}
                                className="text-slate-400 hover:text-white hover:bg-slate-800/80 p-1.5 rounded-lg transition-colors cursor-pointer"
                            >
                                <X size={18} />
                            </button>
                        </div>

                        {/* КОНСОЛЬНЫЙ ВЫВОД (ТЕРМИНАЛ) */}
                        <div className="p-6 overflow-y-auto bg-black flex-1 font-mono text-xs leading-relaxed text-slate-300 min-h-[300px] custom-scrollbar selection:bg-blue-500 selection:text-white">
                            {filteredLogs.length === 0 && (
                                <div className="flex items-center justify-center h-full text-slate-500 italic">
                                    Нет записей для фильтра: {logFilter}
                                </div>
                            )}

                            {filteredLogs.map((logItem, index) => {
                                const time = logItem.timestamp ? logItem.timestamp.substring(11, 19) : "";
                                let colorClass = "text-slate-300"; // INFO
                                if (logItem.level === "WARN") colorClass = "text-amber-400 font-bold";
                                if (logItem.level === "ERROR") colorClass = "text-rose-500 font-black";

                                return (
                                    <div key={index} className="py-0.5 border-b border-slate-950 hover:bg-slate-900/30 transition-colors">
                                        <span className="text-slate-500 font-bold mr-2">[{time}]</span>
                                        <span className={`mr-2 font-black tracking-wider text-[10px] px-1 py-0.2 rounded bg-slate-950 border border-slate-900 text-slate-400`}>
                                            {logItem.level}
                                        </span>
                                        <span className={colorClass}>{logItem.message}</span>
                                    </div>
                                );
                            })}
                            <div ref={consoleEndRef} />
                        </div>

                        {/* БЛОК ОТЧЕТА (REPORT BLOCK) */}
                        <div className="bg-slate-900 border-t border-slate-800 p-6 flex flex-col md:flex-row justify-between items-center gap-4">
                            {maintenanceReport ? (
                                <>
                                    <div className="flex flex-wrap gap-4 text-xs font-mono">
                                        <div className="bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5">
                                            <span className="text-slate-500 font-bold uppercase tracking-wider block text-[9px] mb-0.5">Успешно</span>
                                            <span className="text-emerald-400 font-black text-sm">{maintenanceReport.usersProcessed} юзеров</span>
                                        </div>
                                        <div className="bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5">
                                            <span className="text-slate-500 font-bold uppercase tracking-wider block text-[9px] mb-0.5">Сбои</span>
                                            <span className={`font-black text-sm ${maintenanceReport.usersFailed > 0 ? 'text-rose-500' : 'text-slate-400'}`}>
                                                {maintenanceReport.usersFailed} ошибок
                                            </span>
                                        </div>
                                        <div className="bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5">
                                            <span className="text-slate-500 font-bold uppercase tracking-wider block text-[9px] mb-0.5">Очищено</span>
                                            <span className="text-blue-400 font-black text-sm">{maintenanceReport.serversCleared} серверов</span>
                                        </div>
                                        <div className="bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5">
                                            <span className="text-slate-500 font-bold uppercase tracking-wider block text-[9px] mb-0.5">Время</span>
                                            <span className="text-slate-200 font-black text-sm">{maintenanceReport.elapsedSeconds} сек</span>
                                        </div>
                                    </div>

                                    <button
                                        onClick={closeConsole}
                                        className="btn-action btn-primary py-2.5 text-xs tracking-wider font-extrabold uppercase shrink-0 cursor-pointer"
                                    >
                                        Завершить обслуживание
                                    </button>
                                </>
                            ) : (
                                <div className="flex items-center gap-3 text-xs text-slate-400 italic">
                                    <RefreshCw size={14} className="animate-spin text-blue-500" />
                                    Идет выполнение глобальной очистки и синхронизации. Пожалуйста, не закрывайте вкладку...
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}