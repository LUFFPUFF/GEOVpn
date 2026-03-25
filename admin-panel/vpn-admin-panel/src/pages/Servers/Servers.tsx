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
      <span style={{
          width:8, height:8, borderRadius:"50%", background:color,
          boxShadow:`0 0 8px ${color}88`, display:"inline-block",
          animation: score >= 80 ? "pulse 2s infinite" : "none"
      }}/>
      <span style={{ fontSize:12, fontWeight:700, color }}>{score.toFixed(0)}</span>
    </span>
    )
}

function LoadBar({ current, max }: { current: number, max: number }) {
    const pct = max > 0 ? Math.min(100, (current / max) * 100) : 0
    const color = pct > 80 ? "#ef4444" : pct > 50 ? "#f59e0b" : "#10b981"
    return (
        <div style={{ display:"flex", alignItems:"center", gap:8 }}>
            <div style={{ flex:1, height:4, background:"rgba(255,255,255,0.06)", borderRadius:2, overflow:"hidden" }}>
                <div style={{ height:"100%", width:`${pct}%`, background:color, borderRadius:2, transition:"width 0.5s" }}/>
            </div>
            <span style={{ fontSize:11, color:"#64748b", fontFamily:"'DM Mono',monospace", minWidth:50 }}>
        {current}/{max}
      </span>
        </div>
    )
}

function AddServerModal({ onClose, onCreate }: { onClose:()=>void, onCreate:(d:CreateServerRequest)=>Promise<void> }) {
    const [form, setForm] = useState<CreateServerRequest>({
        name:"", location:"", countryCode:"", ipAddress:"",
        port:443, grpcPort:62789, realityPublicKey:"",
        realityShortId:"", realitySni:"eh.vk.com", maxConnections:1000
    })
    const [saving, setSaving] = useState(false)
    const [error, setError] = useState("")

    const F = (k: keyof CreateServerRequest) => (e: any) =>
        setForm(p => ({ ...p, [k]: typeof p[k] === "number" ? Number(e.target.value) : e.target.value }))

    const handleSubmit = async () => {
        if (!form.name || !form.ipAddress || !form.realityPublicKey) {
            setError("Заполните обязательные поля: Имя, IP, Public Key")
            return
        }
        setSaving(true)
        try { await onCreate(form); onClose() }
        catch(e: any) { setError(e?.message || "Ошибка создания сервера") }
        setSaving(false)
    }

    return (
        <div style={{
            position:"fixed", inset:0, zIndex:1000,
            background:"rgba(0,0,0,0.85)", backdropFilter:"blur(8px)",
            display:"flex", alignItems:"center", justifyContent:"center"
        }} onClick={onClose}>
            <div style={{
                width:560, background:"#0a1628",
                border:"1px solid rgba(255,255,255,0.1)",
                borderRadius:20, padding:32, boxShadow:"0 40px 80px rgba(0,0,0,0.8)"
            }} onClick={e => e.stopPropagation()}>
                <div style={{ fontSize:20, fontWeight:800, color:"#f1f5f9", marginBottom:4 }}>Добавить сервер</div>
                <div style={{ fontSize:13, color:"#334155", marginBottom:24 }}>Регистрация нового VPN-узла в кластере</div>

                {error && <div style={{ padding:"10px 14px", background:"rgba(239,68,68,0.1)", border:"1px solid rgba(239,68,68,0.2)", borderRadius:10, fontSize:13, color:"#ef4444", marginBottom:16 }}>{error}</div>}

                <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:14, marginBottom:16 }}>
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
                            <label style={{ fontSize:11, color:"#475569", fontWeight:600, display:"block", marginBottom:6, textTransform:"uppercase", letterSpacing:"0.05em" }}>{f.label}</label>
                            <input type={f.num ? "number" : "text"} value={(form as any)[f.key]} onChange={F(f.key as any)}
                                   placeholder={f.placeholder}
                                   style={{
                                       width:"100%", padding:"10px 12px", background:"rgba(255,255,255,0.04)",
                                       border:"1px solid rgba(255,255,255,0.08)", borderRadius:8,
                                       color:"#e2e8f0", fontSize:13, outline:"none", boxSizing:"border-box"
                                   }}/>
                        </div>
                    ))}
                </div>

                <div style={{ marginBottom:16 }}>
                    <label style={{ fontSize:11, color:"#475569", fontWeight:600, display:"block", marginBottom:6, textTransform:"uppercase", letterSpacing:"0.05em" }}>Reality Public Key *</label>
                    <input value={form.realityPublicKey} onChange={F("realityPublicKey")}
                           placeholder="0aOBytw1IiaspVe0c-p2YrIEIvyNH7ZUQ1pLx78l42I"
                           style={{
                               width:"100%", padding:"10px 12px", background:"rgba(255,255,255,0.04)",
                               border:"1px solid rgba(255,255,255,0.08)", borderRadius:8,
                               color:"#e2e8f0", fontSize:13, outline:"none", boxSizing:"border-box",
                               fontFamily:"'DM Mono',monospace"
                           }}/>
                </div>

                <div style={{ marginBottom:24 }}>
                    <label style={{ fontSize:11, color:"#475569", fontWeight:600, display:"block", marginBottom:6, textTransform:"uppercase", letterSpacing:"0.05em" }}>Reality Short ID</label>
                    <input value={form.realityShortId} onChange={F("realityShortId")}
                           placeholder="2c8c7ec15fb55d91"
                           style={{
                               width:"100%", padding:"10px 12px", background:"rgba(255,255,255,0.04)",
                               border:"1px solid rgba(255,255,255,0.08)", borderRadius:8,
                               color:"#e2e8f0", fontSize:13, outline:"none", boxSizing:"border-box",
                               fontFamily:"'DM Mono',monospace"
                           }}/>
                </div>

                <div style={{ display:"flex", gap:10 }}>
                    <button onClick={onClose} style={{
                        flex:1, padding:12, background:"rgba(255,255,255,0.04)",
                        border:"1px solid rgba(255,255,255,0.08)", borderRadius:10,
                        color:"#64748b", fontSize:14, fontWeight:600, cursor:"pointer"
                    }}>Отмена</button>
                    <button onClick={handleSubmit} disabled={saving} style={{
                        flex:2, padding:12, background:"linear-gradient(135deg,#3b82f6,#2563eb)",
                        border:"none", borderRadius:10, color:"white",
                        fontSize:14, fontWeight:700, cursor:"pointer", opacity:saving ? 0.7 : 1
                    }}>{saving ? "Создаётся..." : "Добавить сервер"}</button>
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
        <div style={{
            background:"rgba(255,255,255,0.03)", border:`1px solid ${isUp ? "rgba(16,185,129,0.2)" : "rgba(239,68,68,0.2)"}`,
            borderRadius:16, padding:"20px 22px"
        }}>
            <div style={{ display:"flex", alignItems:"center", justifyContent:"space-between", marginBottom:16 }}>
                <div>
                    <div style={{ fontSize:15, fontWeight:700, color:"#e2e8f0", marginBottom:3 }}>{name}</div>
                    <div style={{ fontSize:12, color:"#475569" }}>{svc?.details || "—"}</div>
                </div>
                <span style={{
                    fontSize:11, fontWeight:700, padding:"4px 12px", borderRadius:20,
                    background: isUp ? "rgba(16,185,129,0.12)" : "rgba(239,68,68,0.12)",
                    color: isUp ? "#10b981" : "#ef4444"
                }}>{svc?.status || "UNKNOWN"}</span>
            </div>

            {isUp && (
                <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr 1fr", gap:10 }}>
                    <div style={{ background:"rgba(255,255,255,0.03)", borderRadius:10, padding:"10px 12px" }}>
                        <div style={{ fontSize:10, color:"#475569", fontWeight:600, marginBottom:6, textTransform:"uppercase", letterSpacing:"0.06em" }}>CPU</div>
                        <div style={{ fontSize:22, fontWeight:800, color:"#3b82f6", fontFamily:"'DM Mono',monospace" }}>
                            {svc.cpuUsage?.toFixed(1) || "0"}%
                        </div>
                        <div style={{ marginTop:6, height:4, background:"rgba(255,255,255,0.06)", borderRadius:2, overflow:"hidden" }}>
                            <div style={{ height:"100%", width:`${svc.cpuUsage || 0}%`, background:"#3b82f6", borderRadius:2 }}/>
                        </div>
                    </div>

                    <div style={{ background:"rgba(255,255,255,0.03)", borderRadius:10, padding:"10px 12px" }}>
                        <div style={{ fontSize:10, color:"#475569", fontWeight:600, marginBottom:6, textTransform:"uppercase", letterSpacing:"0.06em" }}>RAM</div>
                        <div style={{ fontSize:22, fontWeight:800, color:"#10b981", fontFamily:"'DM Mono',monospace" }}>
                            {svc.memoryUsedMb || 0}<span style={{ fontSize:12, color:"#475569" }}>MB</span>
                        </div>
                        <div style={{ marginTop:6, height:4, background:"rgba(255,255,255,0.06)", borderRadius:2, overflow:"hidden" }}>
                            <div style={{ height:"100%", width:`${memPct}%`, background:"#10b981", borderRadius:2 }}/>
                        </div>
                    </div>

                    <div style={{ background:"rgba(255,255,255,0.03)", borderRadius:10, padding:"10px 12px" }}>
                        <div style={{ fontSize:10, color:"#475569", fontWeight:600, marginBottom:6, textTransform:"uppercase", letterSpacing:"0.06em" }}>Uptime</div>
                        <div style={{ fontSize:18, fontWeight:800, color:"#f59e0b", fontFamily:"'DM Mono',monospace" }}>
                            {upHours}h {upMins}m
                        </div>
                        <div style={{ fontSize:11, color:"#10b981", marginTop:6, fontWeight:600 }}>● Stable</div>
                    </div>
                </div>
            )}

            {!isUp && (
                <div style={{ padding:"12px 16px", background:"rgba(239,68,68,0.06)", borderRadius:10, fontSize:13, color:"#ef4444" }}>
                    Сервис недоступен · Нет данных мониторинга
                </div>
            )}
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
        total: servers.length,
        active: servers.filter(s => s.isActive).length,
        connections: servers.reduce((a, s) => a + (s.currentConnections || 0), 0),
        avgHealth: servers.length ? servers.reduce((a,s) => a + (s.healthScore || 0), 0) / servers.length : 0,
    }), [servers])

    const handleCreate = async (data: CreateServerRequest) => {
        await adminApi.createServer(data)
        loadData()
    }

    return (
        <div style={{ minHeight:"100vh", background:"#060d18", fontFamily:"'Inter',-apple-system,sans-serif", color:"#e2e8f0", display:"flex" }}>
            <style>{`
        @import url('https://fonts.googleapis.com/css2?family=DM+Mono:wght@400;500&family=Space+Grotesk:wght@400;500;600;700;800&family=Inter:wght@400;500;600&display=swap');
        * { box-sizing:border-box; margin:0; padding:0; }
        input::placeholder { color:#334155; }
        ::-webkit-scrollbar { width:4px; }
        ::-webkit-scrollbar-thumb { background:#1e293b; border-radius:2px; }
        button:hover { opacity:0.85; }
        @keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.4} }
      `}</style>

            {/* Main panel */}
            <div style={{ flex:1, padding:"32px 36px", display:"flex", flexDirection:"column", minWidth:0, overflow:"auto" }}>

                {/* Header */}
                <div style={{ display:"flex", justifyContent:"space-between", alignItems:"flex-start", marginBottom:28 }}>
                    <div>
                        <div style={{ fontSize:28, fontWeight:800, color:"#f8fafc", fontFamily:"'Space Grotesk',sans-serif", marginBottom:4 }}>
                            Серверы
                        </div>
                        <div style={{ fontSize:14, color:"#334155" }}>Управление VPN-кластером и инфраструктурой</div>
                    </div>
                    <div style={{ display:"flex", gap:10, alignItems:"center" }}>
                        <button onClick={loadData} style={{
                            width:40, height:40, borderRadius:10, background:"rgba(255,255,255,0.04)",
                            border:"1px solid rgba(255,255,255,0.07)", color:"#475569", cursor:"pointer",
                            display:"flex", alignItems:"center", justifyContent:"center", fontSize:16
                        }}>↻</button>
                        {viewMode === "fleet" && (
                            <button onClick={() => setShowAddModal(true)} style={{
                                padding:"10px 18px", background:"linear-gradient(135deg,#3b82f6,#2563eb)",
                                border:"none", borderRadius:10, color:"white",
                                fontSize:13, fontWeight:700, cursor:"pointer"
                            }}>+ Добавить сервер</button>
                        )}
                    </div>
                </div>

                {/* View toggle */}
                <div style={{ display:"flex", gap:4, marginBottom:24, background:"rgba(255,255,255,0.03)", padding:4, borderRadius:12, width:"fit-content", border:"1px solid rgba(255,255,255,0.06)" }}>
                    {[["fleet","🌐 VPN Fleet"],["infra","🔧 Инфраструктура"]].map(([v,l]) => (
                        <button key={v} onClick={() => { setViewMode(v as any); setSelectedServer(null) }} style={{
                            padding:"8px 20px", borderRadius:8, fontSize:13, fontWeight:600, cursor:"pointer",
                            background: viewMode === v ? "rgba(59,130,246,0.2)" : "none",
                            border: viewMode === v ? "1px solid rgba(59,130,246,0.3)" : "1px solid transparent",
                            color: viewMode === v ? "#60a5fa" : "#475569"
                        }}>{l}</button>
                    ))}
                </div>

                {viewMode === "fleet" ? (
                    <>
                        {/* Stats */}
                        <div style={{ display:"flex", gap:12, marginBottom:24, flexWrap:"wrap" }}>
                            {[
                                { label:"Всего серверов", value:stats.total, color:"#94a3b8" },
                                { label:"Активных", value:stats.active, color:"#10b981" },
                                { label:"Неактивных", value:stats.total - stats.active, color:"#ef4444" },
                                { label:"Подключений", value:stats.connections, color:"#3b82f6" },
                                { label:"Avg Health", value:stats.avgHealth.toFixed(1), color:"#f59e0b" },
                            ].map(s => (
                                <div key={s.label} style={{
                                    background:"rgba(255,255,255,0.03)", border:"1px solid rgba(255,255,255,0.06)",
                                    borderRadius:14, padding:"14px 20px", flex:"1 1 120px"
                                }}>
                                    <div style={{ fontSize:11, color:"#475569", fontWeight:600, letterSpacing:"0.07em", textTransform:"uppercase", marginBottom:6 }}>{s.label}</div>
                                    <div style={{ fontSize:26, fontWeight:800, color:s.color, fontFamily:"'Space Grotesk',sans-serif" }}>{s.value}</div>
                                </div>
                            ))}
                        </div>

                        {/* Search + filter */}
                        <div style={{ display:"flex", gap:10, marginBottom:16 }}>
                            <div style={{ flex:1, position:"relative" }}>
                                <span style={{ position:"absolute", left:12, top:"50%", transform:"translateY(-50%)", color:"#334155", fontSize:14 }}>🔍</span>
                                <input value={searchQuery} onChange={e => setSearchQuery(e.target.value)}
                                       placeholder="Поиск по имени, IP, локации..."
                                       style={{
                                           width:"100%", padding:"10px 14px 10px 36px",
                                           background:"rgba(255,255,255,0.04)", border:"1px solid rgba(255,255,255,0.07)",
                                           borderRadius:10, color:"#e2e8f0", fontSize:13, outline:"none"
                                       }}/>
                            </div>
                            {["ALL","ACTIVE","INACTIVE"].map(f => (
                                <button key={f} onClick={() => setFilterStatus(f as any)} style={{
                                    padding:"10px 14px", borderRadius:10, fontSize:12, fontWeight:600, cursor:"pointer",
                                    background: filterStatus === f ? "rgba(59,130,246,0.15)" : "rgba(255,255,255,0.03)",
                                    border: filterStatus === f ? "1px solid rgba(59,130,246,0.3)" : "1px solid rgba(255,255,255,0.06)",
                                    color: filterStatus === f ? "#60a5fa" : "#475569"
                                }}>{f === "ALL" ? "Все" : f === "ACTIVE" ? "Активные" : "Неактивные"}</button>
                            ))}
                        </div>

                        {/* Server grid */}
                        {loading && servers.length === 0 ? (
                            <div style={{ textAlign:"center", padding:"60px 0", color:"#334155" }}>Загрузка серверов...</div>
                        ) : (
                            <div style={{ display:"grid", gridTemplateColumns:"repeat(auto-fill,minmax(380px,1fr))", gap:12 }}>
                                {filteredServers.map(server => {
                                    const isSelected = selectedServer?.id === server.id
                                    const loadPct = server.maxConnections > 0 ? (server.currentConnections / server.maxConnections * 100) : 0
                                    const loadColor = loadPct > 80 ? "#ef4444" : loadPct > 50 ? "#f59e0b" : "#10b981"

                                    return (
                                        <div key={server.id} onClick={() => setSelectedServer(isSelected ? null : server)} style={{
                                            background: isSelected ? "rgba(59,130,246,0.08)" : "rgba(255,255,255,0.03)",
                                            border: `1px solid ${isSelected ? "rgba(59,130,246,0.35)" : "rgba(255,255,255,0.06)"}`,
                                            borderRadius:16, padding:"18px 20px", cursor:"pointer",
                                            transition:"all 0.15s"
                                        }}>
                                            {/* Header */}
                                            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"flex-start", marginBottom:14 }}>
                                                <div style={{ display:"flex", alignItems:"center", gap:10 }}>
                                                    <div style={{ fontSize:24 }}>{FLAG[server.countryCode] || "🌐"}</div>
                                                    <div>
                                                        <div style={{ fontSize:15, fontWeight:700, color:"#e2e8f0" }}>{server.name}</div>
                                                        <div style={{ fontSize:12, color:"#475569" }}>{server.location}</div>
                                                    </div>
                                                </div>
                                                <div style={{ display:"flex", flexDirection:"column", alignItems:"flex-end", gap:6 }}>
                          <span style={{
                              fontSize:10, fontWeight:700, padding:"3px 10px", borderRadius:20,
                              background: server.isActive ? "rgba(16,185,129,0.12)" : "rgba(239,68,68,0.12)",
                              color: server.isActive ? "#10b981" : "#ef4444"
                          }}>{server.isActive ? "Online" : "Offline"}</span>
                                                    <span style={{ fontSize:11, color:"#334155", fontFamily:"'DM Mono',monospace" }}>#{server.id}</span>
                                                </div>
                                            </div>

                                            {/* IP + Port */}
                                            <div style={{ display:"flex", gap:8, marginBottom:12 }}>
                        <span style={{
                            fontSize:12, padding:"3px 10px", borderRadius:8,
                            background:"rgba(255,255,255,0.04)", border:"1px solid rgba(255,255,255,0.06)",
                            color:"#64748b", fontFamily:"'DM Mono',monospace"
                        }}>{server.ipAddress}:{server.port}</span>
                                                {server.grpcPort && (
                                                    <span style={{
                                                        fontSize:12, padding:"3px 10px", borderRadius:8,
                                                        background:"rgba(59,130,246,0.07)", border:"1px solid rgba(59,130,246,0.15)",
                                                        color:"#3b82f6", fontFamily:"'DM Mono',monospace"
                                                    }}>gRPC:{server.grpcPort}</span>
                                                )}
                                            </div>

                                            {/* Metrics */}
                                            <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr 1fr", gap:8, marginBottom:12 }}>
                                                <div style={{ background:"rgba(255,255,255,0.02)", borderRadius:8, padding:"8px 10px" }}>
                                                    <div style={{ fontSize:10, color:"#475569", fontWeight:600, marginBottom:3, textTransform:"uppercase" }}>Health</div>
                                                    <HealthDot score={server.healthScore || 0}/>
                                                </div>
                                                <div style={{ background:"rgba(255,255,255,0.02)", borderRadius:8, padding:"8px 10px" }}>
                                                    <div style={{ fontSize:10, color:"#475569", fontWeight:600, marginBottom:3, textTransform:"uppercase" }}>Latency</div>
                                                    <span style={{ fontSize:14, fontWeight:800, color: server.avgLatencyMs ? (server.avgLatencyMs < 50 ? "#10b981" : server.avgLatencyMs < 150 ? "#f59e0b" : "#ef4444") : "#475569", fontFamily:"'DM Mono',monospace" }}>
                            {server.avgLatencyMs ? `${server.avgLatencyMs}ms` : "—"}
                          </span>
                                                </div>
                                                <div style={{ background:"rgba(255,255,255,0.02)", borderRadius:8, padding:"8px 10px" }}>
                                                    <div style={{ fontSize:10, color:"#475569", fontWeight:600, marginBottom:3, textTransform:"uppercase" }}>Conns</div>
                                                    <span style={{ fontSize:14, fontWeight:800, color:loadColor, fontFamily:"'DM Mono',monospace" }}>
                            {server.currentConnections}
                          </span>
                                                </div>
                                            </div>

                                            {/* Load bar */}
                                            <LoadBar current={server.currentConnections} max={server.maxConnections}/>

                                            {/* SNI */}
                                            <div style={{ marginTop:10, fontSize:11, color:"#334155" }}>
                                                SNI: <span style={{ color:"#475569", fontFamily:"'DM Mono',monospace" }}>{server.realitySni || "—"}</span>
                                                {server.lastHealthCheck && (
                                                    <span style={{ marginLeft:12 }}>Check: <span style={{ color:"#475569" }}>{new Date(server.lastHealthCheck).toLocaleTimeString("ru-RU")}</span></span>
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
                    <div style={{ display:"flex", flexDirection:"column", gap:12 }}>
                        <div style={{ display:"flex", alignItems:"center", justifyContent:"space-between", marginBottom:8 }}>
                            <div style={{ fontSize:14, color:"#334155" }}>
                                Статус кластера: {" "}
                                <span style={{
                                    fontWeight:700, padding:"3px 12px", borderRadius:20, fontSize:13,
                                    background: infraHealth?.status === "UP" ? "rgba(16,185,129,0.12)" : "rgba(239,68,68,0.12)",
                                    color: infraHealth?.status === "UP" ? "#10b981" : infraHealth?.status === "DEGRADED" ? "#f59e0b" : "#ef4444"
                                }}>{infraHealth?.status || "LOADING"}</span>
                            </div>
                        </div>
                        {loading && !infraHealth ? (
                            <div style={{ textAlign:"center", padding:"60px 0", color:"#334155" }}>Опрос сервисов...</div>
                        ) : infraHealth?.services ? (
                            Object.entries(infraHealth.services).map(([name, svc]) => (
                                <ServiceHealthCard key={name} name={name} svc={svc}/>
                            ))
                        ) : (
                            <div style={{ textAlign:"center", padding:"60px 0", color:"#334155" }}>Данные недоступны</div>
                        )}
                    </div>
                )}
            </div>

            {/* Detail panel (slide-in) */}
            {selectedServer && (
                <div style={{
                    width:520, borderLeft:"1px solid rgba(255,255,255,0.06)",
                    background:"#080f1a", overflow:"auto", flexShrink:0
                }}>
                    <ServerDetailsPanel
                        server={selectedServer}
                        onClose={() => setSelectedServer(null)}
                        onUpdate={loadData}
                    />
                </div>
            )}

            {showAddModal && (
                <AddServerModal onClose={() => setShowAddModal(false)} onCreate={handleCreate}/>
            )}
        </div>
    )
}