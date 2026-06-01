import { useState, useEffect, useMemo } from "react"
import { adminApi } from "@/api/admin"
import { ServerDto, CreateServerRequest, SystemHealthDto } from "@/types/api"
import ServerDetailsPanel from "./ServerDetailsPanel"

const FLAG: Record<string, string> = {
    NL:"🇳🇱", DE:"🇩🇪", FI:"🇫🇮", PL:"🇵🇱", EE:"🇪🇪",
    SE:"🇸🇪", FR:"🇫🇷", GB:"🇬🇧", US:"🇺🇸", RU:"🇷🇺",
}

function HealthDot({ score }: { score: number }) {
    const color = score >= 80 ? "#10b981" : score >= 50 ? "#f59e0b" : "#ef4444"
    return (
        <span style={{ display:"inline-flex", alignItems:"center", gap:6 }}>
            <span className={score >= 80 ? "pulse-dot-static" : ""} style={{ width:8, height:8, borderRadius:"50%", background:color, display:"inline-block" }}/>
            <span style={{ fontSize:13, fontWeight:800, color }}>{score.toFixed(0)}</span>
        </span>
    )
}

function LoadBar({ current, max }: { current: number, max: number }) {
    const pct = max > 0 ? Math.min(100, (current / max) * 100) : 0
    const color = pct > 80 ? "#ef4444" : pct > 50 ? "#f59e0b" : "#3b82f6"
    return (
        <div style={{ display:"flex", alignItems:"center", gap:10, width: "100%" }}>
            <div style={{ flex:1, height:6, background:"#e2e8f0", borderRadius:3, overflow:"hidden" }}>
                <div style={{ height:"100%", width:`${pct}%`, background:color, borderRadius:3, transition:"width 0.5s ease" }}/>
            </div>
            <span style={{ fontSize:11, color:"#64748b", fontWeight:600, fontFamily:"'DM Mono',monospace", minWidth:50, textAlign:"right" }}>
                {current}/{max}
            </span>
        </div>
    )
}

function AddServerModal({ onClose, onCreate }: { onClose:()=>void, onCreate:(d:CreateServerRequest)=>Promise<void> }) {
    const [form, setForm] = useState<CreateServerRequest>({
        name:"", location:"", countryCode:"", ipAddress:"", port:443, grpcPort:62789, realityPublicKey:"",
        realityShortId:"", realitySni:"eh.vk.com", maxConnections:1000
    })
    const [saving, setSaving] = useState(false)
    const [error, setError] = useState("")

    const F = (k: keyof CreateServerRequest) => (e: any) =>
        setForm(p => ({ ...p, [k]: typeof p[k] === "number" ? Number(e.target.value) : e.target.value }))

    const handleSubmit = async () => {
        if (!form.name || !form.ipAddress || !form.realityPublicKey) { setError("Заполните обязательные поля: Имя, IP, Public Key"); return }
        setSaving(true)
        try { await onCreate(form); onClose() } catch(e: any) { setError(e?.message || "Ошибка создания сервера") }
        setSaving(false)
    }

    return (
        <div style={{ position: "fixed", inset: 0, zIndex: 100, display: "flex", alignItems: "center", justifyContent: "center", padding: "16px", background: "rgba(0,0,0,0.5)", backdropFilter: "blur(4px)" }} onClick={onClose}>
            <div style={{ background: "white", padding: "32px", borderRadius: "24px", maxWidth: "600px", width: "100%" }} onClick={e => e.stopPropagation()}>
                <div className="text-xl font-extrabold mb-1">Добавить сервер</div>
                <div className="text-xs text-slate-500 mb-6">Регистрация нового VPN-узла в кластере</div>

                {error && <div className="p-3 bg-red-55 text-red-600 text-xs font-bold rounded-lg mb-4">{error}</div>}

                <div className="grid grid-cols-2 gap-4 mb-6">
                    {[
                        { label:"Имя *", key:"name", placeholder:"Netherlands-02" },
                        { label:"Локация", key:"location", placeholder:"Amsterdam, NL" },
                        { label:"Код страны", key:"countryCode", placeholder:"NL" },
                        { label:"IP-адрес *", key:"ipAddress", placeholder:"193.104.33.100" },
                        { label:"Порт VLESS", key:"port", placeholder:"443", num:true },
                        { label:"gRPC порт", key:"grpcPort", placeholder:"62789", num:true },
                        { label:"Max подключений", key:"maxConnections", placeholder:"1000", num:true },
                        { label:"Reality SNI", key:"realitySni", placeholder:"eh.vk.com" },
                    ].map(f => (
                        <div key={f.key}>
                            <label className="text-[10px] font-bold text-slate-400 uppercase block mb-1">{f.label}</label>
                            <input type={f.num ? "number" : "text"} className="w-full h-10 px-3 bg-slate-50 border border-slate-200 rounded-lg text-sm" value={(form as any)[f.key]} onChange={F(f.key as any)} placeholder={f.placeholder}/>
                        </div>
                    ))}
                </div>

                <div className="mb-4">
                    <label className="text-[10px] font-bold text-slate-400 uppercase block mb-1">Reality Public Key *</label>
                    <input className="w-full h-10 px-3 bg-slate-50 border border-slate-200 rounded-lg font-mono text-sm" value={form.realityPublicKey} onChange={F("realityPublicKey")} placeholder="0aOBytw..."/>
                </div>

                <div className="mb-6">
                    <label className="text-[10px] font-bold text-slate-400 uppercase block mb-1">Reality Short ID</label>
                    <input className="w-full h-10 px-3 bg-slate-50 border border-slate-200 rounded-lg font-mono text-sm" value={form.realityShortId} onChange={F("realityShortId")} placeholder="2c8c7ec..."/>
                </div>

                <div className="flex gap-4">
                    <button onClick={onClose} className="flex-1 h-11 bg-slate-100 hover:bg-slate-200 rounded-xl text-slate-700 font-bold text-sm cursor-pointer">Отмена</button>
                    <button onClick={handleSubmit} disabled={saving} className="flex-1 h-11 bg-blue-600 hover:bg-blue-700 rounded-xl text-white font-bold text-sm cursor-pointer">{saving ? "Создаётся..." : "Добавить сервер"}</button>
                </div>
            </div>
        </div>
    )
}

function ServiceHealthCard({ name, svc }: { name: string, svc: any, key?: any }) {
    const isUp = svc?.status === "UP"
    const memPct = svc?.memoryMaxMb > 0 ? (svc.memoryUsedMb / svc.memoryMaxMb * 100) : 0
    const upHours = svc?.uptime ? Math.floor(svc.uptime / 3600) : 0
    const upMins = svc?.uptime ? Math.floor((svc.uptime % 3600) / 60) : 0

    return (
        <div style={{ border:`1px solid ${isUp ? "#e2e8f0" : "#fecaca"}`, background:"var(--bg-card)", padding: "18px", borderRadius: "16px" }}>
            <div style={{ display:"flex", alignItems:"center", justifyValue:"space-between", marginBottom:20 }}>
                <div className="flex-1">
                    <div style={{ fontSize:16, fontWeight:800, color:"var(--text-main)", marginBottom:4 }}>{name}</div>
                    <div style={{ fontSize:13, color:"var(--text-muted)", fontWeight:500 }}>{svc?.details || "—"}</div>
                </div>
                <span className="text-xs font-bold px-3 py-1 rounded-full" style={{ background: isUp ? "#d1fae5" : "#fee2e2", color: isUp ? "#059669" : "#dc2626" }}>{svc?.status || "UNKNOWN"}</span>
            </div>

            {isUp && (
                <div className="grid grid-cols-3 gap-3">
                    {[
                        { label:"CPU", value:`${svc.cpuUsage?.toFixed(1) || "0"}%`, color:"#3b82f6", bar:svc.cpuUsage||0 },
                        { label:"RAM", value:`${svc.memoryUsedMb || 0}MB`, color:"#10b981", bar:memPct },
                        { label:"Uptime", value:`${upHours}h ${upMins}m`, color:"#f59e0b", sub:"● Stable" }
                    ].map(m => (
                        <div key={m.label} style={{ background:"var(--bg-input)", borderRadius:10, padding:"12px 14px", border:"1px solid var(--border-color)" }}>
                            <div style={{ fontSize:10, color:"var(--text-muted)", fontWeight:700, textTransform:"uppercase", letterSpacing:"0.05em", marginBottom:6 }}>{m.label}</div>
                            <div style={{ fontSize:18, fontWeight:800, color:m.color, fontFamily:"'DM Mono',monospace" }}>{m.value}</div>
                            {m.bar !== undefined ? (
                                <div style={{ marginTop:8, height:4, background:"var(--border-color)", borderRadius:2, overflow:"hidden" }}>
                                    <div style={{ height:"100%", width:`${m.bar}%`, background:m.color, borderRadius:2 }}/>
                                </div>
                            ) : (
                                <div style={{ fontSize:12, color:"#10b981", marginTop:8, fontWeight:600 }}>{m.sub}</div>
                            )}
                        </div>
                    ))}
                </div>
            )}
            {!isUp && <div style={{ padding:"14px", background:"#fef2f2", borderRadius:10, fontSize:13, color:"#dc2626", fontWeight:600 }}>Сервис недоступен · Нет данных мониторинга</div>}
        </div>
    )
}

export default function Servers() {
    const [viewMode, setViewMode] = useState<"fleet"|"infra">("fleet")
    const [servers, setServers] = useState<ServerDto[]>([])
    const [infraHealth, setInfraHealth] = useState<SystemHealthDto | null>(null)
    const [selectedServer, setSelectedServer] = useState<ServerDto | null>(null)
    const [loading, setLoading] = useState(true)
    const [showAddModal, setShowAddModal] = useState(false)
    const [searchQuery, setSearchQuery] = useState("")
    const [filterStatus, setFilterStatus] = useState<"ALL"|"ACTIVE"|"INACTIVE">("ALL")

    const loadData = async () => {
        setLoading(true)
        try {
            if (viewMode === "fleet") {
                const data = await adminApi.getServers()
                setServers(data)
                if (selectedServer) {
                    const updated = data.find(s => s.id === selectedServer.id)
                    if (updated) setSelectedServer(updated)
                }
            } else {
                const health = await adminApi.getInfrastructureHealth()
                setInfraHealth(health)
            }
        } catch(e) { console.error(e) }
        setLoading(false)
    }

    useEffect(() => {
        loadData()
        const iv = setInterval(loadData, 10000)
        return () => clearInterval(iv)
    }, [viewMode])

    const filteredServers = useMemo(() => servers.filter(s => {
        const q = searchQuery.toLowerCase()
        const matchQ = !q || s.name.toLowerCase().includes(q) || s.ipAddress.includes(q) || s.location.toLowerCase().includes(q)
        const matchF = filterStatus === "ALL" || (filterStatus === "ACTIVE" ? s.isActive : !s.isActive)
        return matchQ && matchF
    }), [servers, searchQuery, filterStatus])

    const stats = useMemo(() => ({
        total: servers.length, active: servers.filter(s => s.isActive).length,
        connections: servers.reduce((a, s) => a + (s.currentConnections || 0), 0),
        avgHealth: servers.length ? servers.reduce((a,s) => a + (s.healthScore || 0), 0) / servers.length : 0,
    }), [servers])

    const handleCreate = async (data: CreateServerRequest) => { await adminApi.createServer(data); loadData() }

    return (
        <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-white p-10 flex transition-colors duration-300">
            <style>{`
                .card { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 16px; box-shadow: var(--shadow-card); transition: all 0.2s ease; }
                .card:hover { transform: translateY(-3px); border-color: var(--text-muted); }
                .info-card { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 12px; padding: 18px; box-shadow: var(--shadow-card); }
                
                .input-field { width:100%; padding:12px 14px; background:var(--bg-input); border:1px solid var(--border-color); border-radius:10px; color:var(--text-main); font-size:14px; outline:none; transition:all 0.2s; font-family: inherit; }
                .input-field::placeholder { color: var(--text-muted); }
                .input-field:focus { background:var(--bg-card); border-color:#3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.15); }
                
                .btn-primary { padding:12px 20px; background:linear-gradient(135deg, #2563eb, #1d4ed8); border:none; border-radius:10px; color:white; font-size:14px; font-weight:700; cursor:pointer; transition:all 0.2s; box-shadow: 0 4px 10px -2px rgba(37,99,235,0.3); }
                .btn-primary:hover:not(:disabled) { transform: translateY(-1px); }
                
                .btn-secondary { padding:12px 20px; background:var(--bg-input); border:1px solid var(--border-color); border-radius:10px; color:var(--text-main); font-size:14px; font-weight:700; cursor:pointer; transition:all 0.2s; }
                .btn-secondary:hover { background:var(--border-color); }

                .btn-danger { padding:10px 16px; background:#fee2e2; border:1px solid #fecaca; border-radius:8px; color:#dc2626; font-size:13px; font-weight:700; cursor:pointer; transition:all 0.2s; }
                .btn-danger:hover { background:#fecaca; }
                .btn-danger-outline { padding:8px 14px; background:transparent; border:1px solid #fecaca; border-radius:8px; color:#dc2626; font-size:12px; font-weight:700; cursor:pointer; transition:all 0.2s; }
                .btn-danger-outline:hover { background:#fee2e2; }
                
                .btn-success-outline { padding:8px 14px; background:transparent; border:1px solid #a7f3d0; border-radius:8px; color:#059669; font-size:12px; font-weight:700; cursor:pointer; transition:all 0.2s; }
                .btn-success-outline:hover { background:#d1fae5; }
                
                .icon-btn { width:36px; height:36px; border-radius:10px; border:none; display:flex; align-items:center; justify-content:center; cursor:pointer; font-size:16px; transition:all 0.2s; }
                .icon-btn:hover { transform: scale(1.05); }

                .badge { font-size:11px; font-weight:700; padding:4px 10px; border-radius:20px; display:inline-flex; align-items:center; letter-spacing:0.02em; }
                
                .list-row { display:flex; justify-content:space-between; align-items:center; padding:12px 18px; border-bottom:1px solid var(--border-color); }
                .list-row:last-child { border-bottom: none; }
                
                .copy-btn { background:none; border:none; cursor:pointer; font-size:13px; padding:2px; border-radius:4px; transition:all 0.2s; }
                .copy-btn:hover { background: var(--bg-input); color: var(--text-main) !important; }
            `}</style>

            {/* Main panel */}
            <div className="flex-1 flex flex-col min-w-0 overflow-auto">

                {/* Header */}
                <div className="flex justify-between items-start mb-8">
                    <div>
                        <div className="text-3xl font-extrabold tracking-tight italic" style={{ fontFamily: "Space Grotesk, sans-serif" }}>
                            Серверы
                        </div>
                        <div className="text-xs text-slate-500 mt-1">Клиентские VLESS Reality ноды, мосты и показатели latency.</div>
                    </div>
                    <div className="flex gap-3 items-center">
                        <button onClick={loadData} className="w-10 h-10 flex items-center justify-center border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm rounded-lg hover:bg-slate-50 font-bold cursor-pointer transition-transform duration-200">↻</button>
                        {viewMode === "fleet" && (
                            <button onClick={() => setShowAddModal(true)} className="btn-primary cursor-pointer">+ Добавить узел</button>
                        )}
                    </div>
                </div>

                {/* Switch view */}
                <div className="flex bg-slate-200 dark:bg-slate-800 p-1 rounded-xl mb-6 w-fit border border-slate-100 dark:border-slate-850">
                    {[["fleet","🌐 VPN Fleet"],["infra","🔧 Инфраструктура"]].map(([v,l]) => (
                        <button key={v} onClick={() => { setViewMode(v as any); setSelectedServer(null) }} style={{
                            padding:"8px 16px", borderRadius:10, fontSize:13, fontWeight:700, cursor:"pointer", transition:"all 0.2s",
                            background: viewMode === v ? "var(--bg-card)" : "transparent",
                            border: "none",
                            color: viewMode === v ? "var(--text-main)" : "var(--text-muted)"
                        }}>{l}</button>
                    ))}
                </div>

                {viewMode === "fleet" ? (
                    <>
                        {/* Summary metrics */}
                        <div className="grid grid-cols-1 md:grid-cols-5 gap-4 mb-8">
                            {[
                                { label:"Всего узлов", value:stats.total, color: "text-slate-900 dark:text-white" },
                                { label:"В работе", value:stats.active, color:"text-emerald-500" },
                                { label:"Офлайн", value:stats.total - stats.active, color:"text-red-500" },
                                { label:"Юзеров онлайн", value:stats.connections, color:"text-blue-500" },
                                { label:"Avg Health Score", value:stats.avgHealth.toFixed(1), color:"text-amber-500" },
                            ].map(s => (
                                <div key={s.label} className="card p-5">
                                    <div className="text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-2">{s.label}</div>
                                    <div className={`text-2xl font-black font-mono ${s.color}`}>{s.value}</div>
                                </div>
                            ))}
                        </div>

                        {/* Search and filters */}
                        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
                            <input className="w-full md:col-span-3 h-10 px-3 border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 rounded-lg text-sm" value={searchQuery} onChange={e => setSearchQuery(e.target.value)} placeholder="Поиск сервера по IP, коду страны, названию или локации..."/>
                            <div className="flex bg-slate-200 dark:bg-slate-800 p-1 rounded-lg border border-slate-100 dark:border-slate-900 justify-between">
                                {["ALL","ACTIVE","INACTIVE"].map(f => (
                                    <button key={f} onClick={() => setFilterStatus(f as any)} className={`flex-1 text-xs font-bold py-1 px-2.5 rounded-md ${filterStatus === f ? 'bg-white text-blue-600 shadow-sm' : 'text-slate-500'}`}>
                                        {f === "ALL" ? "Все" : f === "ACTIVE" ? "Онлайн" : "Офлайн"}
                                    </button>
                                ))}
                            </div>
                        </div>

                        {/* Search grid */}
                        {loading && servers.length === 0 ? (
                            <div className="p-16 border-2 border-dashed border-slate-200 dark:border-slate-800 rounded-xl text-center text-xs text-slate-400">Загрузка данных кластера...</div>
                        ) : (
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-5 w-full">
                                {filteredServers.map(server => {
                                    const isSelected = selectedServer?.id === server.id

                                    return (
                                        <div key={server.id} onClick={() => setSelectedServer(isSelected ? null : server)} className="card p-6 cursor-pointer" style={{
                                            borderColor: isSelected ? "#3b82f6" : "var(--border-color)",
                                            boxShadow: isSelected ? "0 0 0 2px rgba(59,130,246,0.2), 0 10px 25px -5px rgba(0,0,0,0.1)" : undefined
                                        }}>
                                            {/* Flag + metadata */}
                                            <div className="flex justify-between items-start mb-4">
                                                <div className="flex items-center gap-3">
                                                    <span className="text-3xl">{FLAG[server.countryCode] || "🌐"}</span>
                                                    <div>
                                                        <div className="text-base font-extrabold text-slate-800 dark:text-slate-200" style={{ fontFamily: "Space Grotesk, sans-serif" }}>{server.name}</div>
                                                        <div className="text-xs text-slate-400 font-semibold">{server.location}</div>
                                                    </div>
                                                </div>
                                                <div className="text-right">
                                                    <span className={`text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full ${server.isActive ? 'bg-emerald-100 text-emerald-600' : 'bg-red-100 text-red-650'}`}>
                                                        {server.isActive ? "Online" : "Offline"}
                                                    </span>
                                                    <div className="text-[10px] font-mono text-slate-300 font-bold mt-1">ID #{server.id}</div>
                                                </div>
                                            </div>

                                            {/* Address parameters */}
                                            <div className="flex gap-2 mb-4">
                                                <span className="text-[10px] font-mono bg-slate-100 dark:bg-slate-900 border border-slate-200 dark:border-slate-800 font-bold px-2 py-0.5 rounded text-slate-600">
                                                    {server.ipAddress}:{server.port}
                                                </span>
                                                {server.grpcPort && (
                                                    <span className="text-[10px] font-mono bg-blue-105 border border-blue-200 text-blue-600 font-bold px-2 py-0.5 rounded">
                                                        gRPC:{server.grpcPort}
                                                    </span>
                                                )}
                                            </div>

                                            {/* KPI widgets */}
                                            <div className="grid grid-cols-2 gap-3 mb-4">
                                                <div className="bg-slate-50 dark:bg-slate-900/40 p-3 rounded-lg border border-slate-100 dark:border-slate-800">
                                                    <div className="text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1">Health & Latency</div>
                                                    <div className="flex items-center gap-2">
                                                        <HealthDot score={server.healthScore || 0}/>
                                                        <span className="text-slate-300 font-light">|</span>
                                                        <span className={`text-xs font-bold font-mono ${server.avgLatencyMs && server.avgLatencyMs < 60 ? 'text-emerald-500' : 'text-slate-500'}`}>
                                                            {server.avgLatencyMs ? `${server.avgLatencyMs}ms` : "—"}
                                                        </span>
                                                    </div>
                                                </div>
                                                <div className="bg-slate-50 dark:bg-slate-900/40 p-3 rounded-lg border border-slate-100 dark:border-slate-800 flex flex-col justify-center">
                                                    <div className="text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-2">Нагрузка</div>
                                                    <LoadBar current={server.currentConnections} max={server.maxConnections}/>
                                                </div>
                                            </div>

                                            {/* Footer timestamp */}
                                            <div className="flex justify-between items-center pt-3 border-t border-dashed border-slate-200 dark:border-slate-800 text-[11px] text-slate-500 font-medium">
                                                <div>SNI: <span className="font-mono font-bold">{server.realitySni || "—"}</span></div>
                                                {server.lastHealthCheck && (
                                                    <div>Ping: {new Date(server.lastHealthCheck).toLocaleTimeString()}</div>
                                                )}
                                            </div>
                                        </div>
                                    )
                                })}
                            </div>
                        )}
                    </>
                ) : (
                    /* Cluster microservices infrastructure stats */
                    <div className="max-w-4xl flex flex-col gap-4">
                        <div className="card p-6 flex justify-between items-center mb-4">
                            <div>
                                <div className="text-lg font-bold">Ядро системы</div>
                                <div className="text-xs text-slate-500">Микросервисы API, кэша и фоновых планировщиков списания.</div>
                            </div>
                            <span className="text-sm font-bold px-4 py-2 border border-emerald-200 bg-emerald-50 text-emerald-600 rounded-lg">
                                СТАТУС: {infraHealth?.status || "LOADING"}
                            </span>
                        </div>

                        {loading && !infraHealth ? (
                            <div className="p-16 border border-dashed border-slate-200 dark:border-slate-850 rounded-xl text-center text-xs text-slate-400">Опрашиваем ноды...</div>
                        ) : infraHealth?.services ? (
                            <div className="flex flex-col gap-4">
                                {Object.entries(infraHealth.services).map(([name, svc]) => (
                                    <ServiceHealthCard key={name} name={name} svc={svc}/>
                                ))}
                            </div>
                        ) : (
                            <div className="p-16 border border-dashed border-slate-200 dark:border-slate-850 rounded-xl text-center text-xs text-slate-400">Нет данных мониторинга.</div>
                        )}
                    </div>
                )}
            </div>

            {/* Sidebar drawer for editing / sessions stats */}
            {selectedServer && (
                <div className="w-[560px] bg-white dark:bg-[#0f172a] overflow-auto shrink-0 z-10 shadow-[-10px_0_30px_rgba(0,0,0,0.05)] border-l border-slate-100 dark:border-slate-800">
                    <ServerDetailsPanel
                        server={selectedServer}
                        onClose={() => setSelectedServer(null)}
                        onUpdate={loadData}
                    />
                </div>
            )}

            {showAddModal && <AddServerModal onClose={() => setShowAddModal(false)} onCreate={handleCreate}/>}
        </div>
    )
}
