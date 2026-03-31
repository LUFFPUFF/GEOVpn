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
        <div style={{ display:"flex", alignItems:"center", gap:10 }}>
            <div style={{ flex:1, height:6, background:"var(--border-color)", borderRadius:3, overflow:"hidden" }}>
                <div style={{ height:"100%", width:`${pct}%`, background:color, borderRadius:3, transition:"width 0.5s ease" }}/>
            </div>
            <span style={{ fontSize:12, color:"var(--text-muted)", fontWeight:600, fontFamily:"'DM Mono',monospace", minWidth:50, textAlign:"right" }}>
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
        <div className="modal-backdrop" onClick={onClose} style={{ background:"rgba(0, 0, 0, 0.4)", backdropFilter:"blur(6px)" }}>
            <div className="modal-content" onClick={e => e.stopPropagation()} style={{ background: "var(--bg-card)", border: "1px solid var(--border-color)" }}>
                <div style={{ fontSize:22, fontWeight:800, color:"var(--text-main)", marginBottom:6, fontFamily:"'Space Grotesk',sans-serif" }}>Добавить сервер</div>
                <div style={{ fontSize:14, color:"var(--text-muted)", marginBottom:24 }}>Регистрация нового VPN-узла в кластере</div>

                {error && <div style={{ padding:"12px 16px", background:"#fef2f2", border:"1px solid #fecaca", borderRadius:10, fontSize:13, color:"#ef4444", fontWeight:500, marginBottom:20 }}>{error}</div>}

                <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:16, marginBottom:20 }}>
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
                            <label className="input-label" style={{ color:"var(--text-muted)" }}>{f.label}</label>
                            <input type={f.num ? "number" : "text"} className="input-field" value={(form as any)[f.key]} onChange={F(f.key as any)} placeholder={f.placeholder}/>
                        </div>
                    ))}
                </div>

                <div style={{ marginBottom:20 }}>
                    <label className="input-label" style={{ color:"var(--text-muted)" }}>Reality Public Key *</label>
                    <input className="input-field" value={form.realityPublicKey} onChange={F("realityPublicKey")} placeholder="0aOBytw..." style={{ fontFamily:"'DM Mono',monospace" }}/>
                </div>

                <div style={{ marginBottom:30 }}>
                    <label className="input-label" style={{ color:"var(--text-muted)" }}>Reality Short ID</label>
                    <input className="input-field" value={form.realityShortId} onChange={F("realityShortId")} placeholder="2c8c7ec..." style={{ fontFamily:"'DM Mono',monospace" }}/>
                </div>

                <div style={{ display:"flex", gap:12 }}>
                    <button onClick={onClose} className="btn-secondary" style={{ flex:1 }}>Отмена</button>
                    <button onClick={handleSubmit} disabled={saving} className="btn-primary" style={{ flex:2 }}>{saving ? "Создаётся..." : "Добавить сервер"}</button>
                </div>
            </div>
        </div>
    )
}

function ServiceHealthCard({ name, svc }: { name: string, svc: any }) {
    const isUp = svc?.status === "UP"
    const memPct = svc?.memoryMaxMb > 0 ? (svc.memoryUsedMb / svc.memoryMaxMb * 100) : 0
    const upHours = svc?.uptime ? Math.floor(svc.uptime / 3600) : 0
    const upMins = svc?.uptime ? Math.floor((svc.uptime % 3600) / 60) : 0

    return (
        <div className="info-card" style={{ border:`1px solid ${isUp ? "#a7f3d0" : "#fecaca"}`, background:"var(--bg-card)" }}>
            <div style={{ display:"flex", alignItems:"center", justifyContent:"space-between", marginBottom:20 }}>
                <div>
                    <div style={{ fontSize:16, fontWeight:800, color:"var(--text-main)", marginBottom:4 }}>{name}</div>
                    <div style={{ fontSize:13, color:"var(--text-muted)", fontWeight:500 }}>{svc?.details || "—"}</div>
                </div>
                <span className="badge" style={{ background: isUp ? "#d1fae5" : "#fee2e2", color: isUp ? "#059669" : "#dc2626", fontSize:12 }}>{svc?.status || "UNKNOWN"}</span>
            </div>

            {isUp && (
                <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr 1fr", gap:12 }}>
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
        <div style={{ minHeight:"100vh", background:"var(--bg-base)", fontFamily:"'Inter',-apple-system,sans-serif", color:"var(--text-main)", display:"flex", transition:"background 0.3s" }}>
            <style>{`
                @import url('https://fonts.googleapis.com/css2?family=DM+Mono:wght@400;500;600&family=Space+Grotesk:wght@500;700;800&family=Inter:wght@400;500;600;700&display=swap');
                * { box-sizing:border-box; margin:0; padding:0; }
                
                /* CSS UI Kit */
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
            <div style={{ flex:1, padding:"40px", display:"flex", flexDirection:"column", minWidth:0, overflow:"auto" }}>

                {/* Header */}
                <div style={{ display:"flex", justifyContent:"space-between", alignItems:"flex-start", marginBottom:32 }}>
                    <div>
                        <div style={{ fontSize:36, fontWeight:800, color:"var(--text-main)", fontFamily:"'Space Grotesk',sans-serif", letterSpacing:"-0.03em", marginBottom:6 }}>
                            Серверы
                        </div>
                        <div style={{ fontSize:15, color:"var(--text-muted)", fontWeight:500 }}>Управление VPN-кластером и инфраструктурой</div>
                    </div>
                    <div style={{ display:"flex", gap:12, alignItems:"center" }}>
                        <button onClick={loadData} className="icon-btn" style={{ background:"var(--bg-card)", border:"1px solid var(--border-color)", color:"var(--text-muted)", boxShadow:"var(--shadow-card)" }}>↻</button>
                        {viewMode === "fleet" && (
                            <button onClick={() => setShowAddModal(true)} className="btn-primary" style={{ padding:"12px 24px" }}>+ Добавить узел</button>
                        )}
                    </div>
                </div>

                {/* View toggle */}
                <div style={{ display:"flex", gap:4, marginBottom:28, background:"var(--bg-card)", padding:6, borderRadius:14, width:"fit-content", border:"1px solid var(--border-color)", boxShadow:"var(--shadow-card)" }}>
                    {[["fleet","🌐 VPN Fleet"],["infra","🔧 Инфраструктура"]].map(([v,l]) => (
                        <button key={v} onClick={() => { setViewMode(v as any); setSelectedServer(null) }} style={{
                            padding:"10px 24px", borderRadius:10, fontSize:14, fontWeight:700, cursor:"pointer", transition:"all 0.2s",
                            background: viewMode === v ? "var(--bg-input)" : "transparent",
                            border: "none",
                            color: viewMode === v ? "var(--text-main)" : "var(--text-muted)"
                        }}>{l}</button>
                    ))}
                </div>

                {viewMode === "fleet" ? (
                    <>
                        {/* Stats */}
                        <div style={{ display:"flex", gap:16, marginBottom:32, flexWrap:"wrap" }}>
                            {[
                                { label:"Всего узлов", value:stats.total, color:"var(--text-muted)" },
                                { label:"В работе", value:stats.active, color:"#10b981" },
                                { label:"Офлайн", value:stats.total - stats.active, color:"#ef4444" },
                                { label:"Live Подключения", value:stats.connections, color:"#3b82f6" },
                                { label:"Avg Health", value:stats.avgHealth.toFixed(1), color:"#f59e0b" },
                            ].map(s => (
                                <div key={s.label} className="card" style={{ padding:"18px 24px", flex:"1 1 140px" }}>
                                    <div style={{ fontSize:11, color:"var(--text-muted)", fontWeight:700, letterSpacing:"0.05em", textTransform:"uppercase", marginBottom:8 }}>{s.label}</div>
                                    <div style={{ fontSize:32, fontWeight:800, color:s.color, fontFamily:"'Space Grotesk',sans-serif" }}>{s.value}</div>
                                </div>
                            ))}
                        </div>

                        {/* Search + filter */}
                        <div style={{ display:"flex", gap:12, marginBottom:24 }}>
                            <div style={{ flex:1, position:"relative" }}>
                                <span style={{ position:"absolute", left:16, top:"50%", transform:"translateY(-50%)", color:"var(--text-muted)", fontSize:16 }}>🔍</span>
                                <input className="input-field" value={searchQuery} onChange={e => setSearchQuery(e.target.value)} placeholder="Поиск сервера по имени, IP или локации..." style={{ paddingLeft:44, fontSize:15 }}/>
                            </div>
                            <div style={{ display:"flex", background:"var(--bg-card)", border:"1px solid var(--border-color)", borderRadius:10, padding:4, boxShadow:"var(--shadow-card)" }}>
                                {["ALL","ACTIVE","INACTIVE"].map(f => (
                                    <button key={f} onClick={() => setFilterStatus(f as any)} style={{
                                        padding:"10px 16px", borderRadius:8, fontSize:13, fontWeight:700, cursor:"pointer", transition:"all 0.2s",
                                        background: filterStatus === f ? "rgba(59,130,246,0.15)" : "transparent",
                                        border: "none",
                                        color: filterStatus === f ? "#2563eb" : "var(--text-muted)"
                                    }}>{f === "ALL" ? "Все" : f === "ACTIVE" ? "Активные" : "Офлайн"}</button>
                                ))}
                            </div>
                        </div>

                        {/* Server grid */}
                        {loading && servers.length === 0 ? (
                            <div className="empty-state" style={{ color:"var(--text-muted)", border:"2px dashed var(--border-color)" }}>Загрузка данных кластера...</div>
                        ) : (
                            <div style={{ display:"grid", gridTemplateColumns:"repeat(auto-fill,minmax(380px,1fr))", gap:20 }}>
                                {filteredServers.map(server => {
                                    const isSelected = selectedServer?.id === server.id

                                    return (
                                        <div key={server.id} onClick={() => setSelectedServer(isSelected ? null : server)} className="card" style={{
                                            padding:"24px", cursor:"pointer",
                                            borderColor: isSelected ? "#3b82f6" : "var(--border-color)",
                                            boxShadow: isSelected ? "0 0 0 2px rgba(59,130,246,0.2), 0 10px 25px -5px rgba(0,0,0,0.1)" : undefined
                                        }}>
                                            {/* Header */}
                                            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"flex-start", marginBottom:20 }}>
                                                <div style={{ display:"flex", alignItems:"center", gap:14 }}>
                                                    <div style={{ fontSize:32, filter:"drop-shadow(0 2px 4px rgba(0,0,0,0.1))" }}>{FLAG[server.countryCode] || "🌐"}</div>
                                                    <div>
                                                        <div style={{ fontSize:18, fontWeight:800, color:"var(--text-main)", fontFamily:"'Space Grotesk',sans-serif", letterSpacing:"-0.01em" }}>{server.name}</div>
                                                        <div style={{ fontSize:13, color:"var(--text-muted)", fontWeight:500, marginTop:2 }}>{server.location}</div>
                                                    </div>
                                                </div>
                                                <div style={{ display:"flex", flexDirection:"column", alignItems:"flex-end", gap:8 }}>
                                                    <span className="badge" style={{ background: server.isActive ? "#d1fae5" : "#fee2e2", color: server.isActive ? "#059669" : "#dc2626" }}>
                                                        {server.isActive ? "Online" : "Offline"}
                                                    </span>
                                                    <span style={{ fontSize:12, color:"var(--text-muted)", fontWeight:600, fontFamily:"'DM Mono',monospace" }}>#{server.id}</span>
                                                </div>
                                            </div>

                                            {/* IP + Port */}
                                            <div style={{ display:"flex", gap:8, marginBottom:20 }}>
                                                <span className="badge" style={{ background:"var(--bg-input)", color:"var(--text-muted)", border:"1px solid var(--border-color)", fontFamily:"'DM Mono',monospace" }}>
                                                    {server.ipAddress}:{server.port}
                                                </span>
                                                {server.grpcPort && (
                                                    <span className="badge" style={{ background:"rgba(59,130,246,0.15)", color:"#2563eb", border:"1px solid #bfdbfe", fontFamily:"'DM Mono',monospace" }}>
                                                        gRPC:{server.grpcPort}
                                                    </span>
                                                )}
                                            </div>

                                            {/* Metrics */}
                                            <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:12, marginBottom:16 }}>
                                                <div style={{ background:"var(--bg-input)", borderRadius:10, padding:"10px 14px", border:"1px solid var(--border-color)" }}>
                                                    <div style={{ fontSize:11, color:"var(--text-muted)", fontWeight:700, marginBottom:4, textTransform:"uppercase", letterSpacing:"0.04em" }}>Health & Latency</div>
                                                    <div style={{ display:"flex", alignItems:"center", gap:12 }}>
                                                        <HealthDot score={server.healthScore || 0}/>
                                                        <span style={{ color:"var(--text-muted)" }}>|</span>
                                                        <span style={{ fontSize:15, fontWeight:800, color: server.avgLatencyMs ? (server.avgLatencyMs < 50 ? "#10b981" : server.avgLatencyMs < 150 ? "#f59e0b" : "#ef4444") : "var(--text-muted)", fontFamily:"'DM Mono',monospace" }}>
                                                            {server.avgLatencyMs ? `${server.avgLatencyMs}ms` : "—"}
                                                        </span>
                                                    </div>
                                                </div>
                                                <div style={{ background:"var(--bg-input)", borderRadius:10, padding:"10px 14px", border:"1px solid var(--border-color)", display:"flex", flexDirection:"column", justifyContent:"center" }}>
                                                    <div style={{ fontSize:11, color:"var(--text-muted)", fontWeight:700, marginBottom:8, textTransform:"uppercase", letterSpacing:"0.04em" }}>Нагрузка узла</div>
                                                    <LoadBar current={server.currentConnections} max={server.maxConnections}/>
                                                </div>
                                            </div>

                                            {/* SNI */}
                                            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginTop:16, paddingTop:16, borderTop:"1px dashed var(--border-color)", fontSize:12 }}>
                                                <div><span style={{ color:"var(--text-muted)", fontWeight:600 }}>SNI:</span> <span style={{ color:"var(--text-main)", fontFamily:"'DM Mono',monospace", fontWeight:600 }}>{server.realitySni || "—"}</span></div>
                                                {server.lastHealthCheck && (
                                                    <div style={{ color:"var(--text-muted)", fontWeight:500 }}>Update: <span style={{ color:"var(--text-muted)" }}>{new Date(server.lastHealthCheck).toLocaleTimeString("ru-RU", {hour:'2-digit', minute:'2-digit'})}</span></div>
                                                )}
                                            </div>
                                        </div>
                                    )
                                })}
                            </div>
                        )}
                    </>
                ) : (
                    /* Infrastructure view */
                    <div style={{ display:"flex", flexDirection:"column", gap:16, maxWidth:900 }}>
                        <div className="card" style={{ padding:"24px", display:"flex", alignItems:"center", justifyContent:"space-between", marginBottom:8 }}>
                            <div>
                                <div style={{ fontSize:20, fontWeight:800, color:"var(--text-main)", marginBottom:4 }}>Ядро системы</div>
                                <div style={{ fontSize:14, color:"var(--text-muted)", fontWeight:500 }}>Мониторинг внутренних микросервисов</div>
                            </div>
                            <span style={{
                                fontWeight:800, padding:"8px 16px", borderRadius:12, fontSize:15,
                                background: infraHealth?.status === "UP" ? "#d1fae5" : infraHealth?.status === "DEGRADED" ? "#fef3c7" : "#fee2e2",
                                color: infraHealth?.status === "UP" ? "#059669" : infraHealth?.status === "DEGRADED" ? "#d97706" : "#dc2626",
                                border: `1px solid ${infraHealth?.status === "UP" ? "#a7f3d0" : infraHealth?.status === "DEGRADED" ? "#fde68a" : "#fecaca"}`
                            }}>СТАТУС: {infraHealth?.status || "LOADING"}</span>
                        </div>

                        {loading && !infraHealth ? (
                            <div className="empty-state" style={{ color:"var(--text-muted)", border:"2px dashed var(--border-color)" }}>Опрос сервисов...</div>
                        ) : infraHealth?.services ? (
                            <div style={{ display:"grid", gap:16 }}>
                                {Object.entries(infraHealth.services).map(([name, svc]) => (
                                    <ServiceHealthCard key={name} name={name} svc={svc}/>
                                ))}
                            </div>
                        ) : (
                            <div className="empty-state" style={{ color:"var(--text-muted)", border:"2px dashed var(--border-color)" }}>Данные недоступны</div>
                        )}
                    </div>
                )}
            </div>

            {/* Detail panel (slide-in) */}
            {selectedServer && (
                <div style={{ width:560, background:"var(--bg-card)", overflow:"auto", flexShrink:0, zIndex:10, boxShadow:"-10px 0 30px rgba(0,0,0,0.05)" }}>
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