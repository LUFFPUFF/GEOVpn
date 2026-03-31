import { useState, useEffect, useMemo, useRef } from "react"
import { adminApi } from "@/api/admin"
import { ServerDto, XrayUserStat, UserTrafficStatsDto } from "@/types/api"

const FLAG: Record<string, string> = {
    NL:"🇳🇱", DE:"🇩🇪", FI:"🇫🇮", PL:"🇵🇱", EE:"🇪🇪",
    SE:"🇸🇪", FR:"🇫🇷", GB:"🇬🇧", US:"🇺🇸", RU:"🇷🇺",
}

function fmtBytes(b: number): string {
    if (b >= 1024**3) return (b/1024**3).toFixed(2)+" GB"
    if (b >= 1024**2) return (b/1024**2).toFixed(1)+" MB"
    if (b >= 1024)    return (b/1024).toFixed(0)+" KB"
    return b+" B"
}

function MiniChart({ data, color = "#3b82f6" }: { data: number[], color?: string }) {
    if (data.length < 2) return null
    const max = Math.max(...data, 1)
    const W = 200, H = 40
    const pts = data.map((v, i) => `${(i/(data.length-1))*W},${H - (v/max)*H}`).join(" ")
    return (
        <svg width={W} height={H} style={{ overflow:"visible" }}>
            <defs>
                <linearGradient id={`g${color.replace("#","")}`} x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor={color} stopOpacity={0.3}/>
                    <stop offset="100%" stopColor={color} stopOpacity={0}/>
                </linearGradient>
            </defs>
            <polygon
                points={`0,${H} ${pts} ${W},${H}`}
                fill={`url(#g${color.replace("#","")})`}
            />
            <polyline points={pts} fill="none" stroke={color} strokeWidth={2} strokeLinecap="round" strokeLinejoin="round"/>
            {/* Last point dot */}
            {data.length > 0 && (
                <circle
                    cx={(data.length-1)/(data.length-1)*W}
                    cy={H - (data[data.length-1]/max)*H}
                    r={3} fill={color}
                />
            )}
        </svg>
    )
}

function CopyBtn({ value }: { value: string }) {
    const [copied, setCopied] = useState(false)
    const copy = () => {
        navigator.clipboard.writeText(value)
        setCopied(true)
        setTimeout(() => setCopied(false), 1500)
    }
    return (
        <button onClick={copy} title="Скопировать" style={{
            background:"none", border:"none", cursor:"pointer",
            color: copied ? "#10b981" : "#334155", fontSize:12, padding:"2px 6px",
            borderRadius:4, transition:"color 0.2s"
        }}>{copied ? "✓" : "⎘"}</button>
    )
}

export default function ServerDetailsPanel({
                                               server, onClose, onUpdate
                                           }: {
    server: ServerDto,
    onClose: () => void,
    onUpdate: () => void
}) {
    const [tab, setTab] = useState<"overview"|"users"|"settings">("overview")
    const [users, setUsers] = useState<XrayUserStat[]>([])
    const [dbStats, setDbStats] = useState<UserTrafficStatsDto[]>([])
    const [connHistory, setConnHistory] = useState<number[]>([])
    const [search, setSearch] = useState("")
    const [loading, setLoading] = useState(false)
    const [saving, setSaving] = useState(false)
    const [toggling, setToggling] = useState(false)
    const [toast, setToast] = useState<string|null>(null)

    const [form, setForm] = useState({
        location: server.location || "",
        maxConnections: server.maxConnections || 1000,
        realitySni: server.realitySni || "eh.vk.com",
        isActive: server.isActive,
    })

    const showToast = (msg: string) => {
        setToast(msg)
        setTimeout(() => setToast(null), 2500)
    }

    const loadData = async () => {
        if (!server.isActive) return
        setLoading(true)
        try {
            const [xray, db] = await Promise.all([
                adminApi.getServerUsers(server.id),
                adminApi.getServerTrafficStats(server.id),
            ])
            setUsers(xray || [])
            setDbStats(db || [])
            setConnHistory(prev => [...prev.slice(-29), server.currentConnections])
        } catch(e) { console.error(e) }
        setLoading(false)
    }

    useEffect(() => {
        setConnHistory([])
        setUsers([])
        setDbStats([])
        setForm({
            location: server.location || "",
            maxConnections: server.maxConnections || 1000,
            realitySni: server.realitySni || "eh.vk.com",
            isActive: server.isActive,
        })
        if (tab === "users") loadData()
    }, [server.id])

    useEffect(() => {
        if (tab !== "users") return
        loadData()
        const iv = setInterval(loadData, 5000)
        return () => clearInterval(iv)
    }, [tab, server.id])

    // Accumulate connection history every 10s regardless of tab
    useEffect(() => {
        const iv = setInterval(() => {
            setConnHistory(prev => [...prev.slice(-29), server.currentConnections])
        }, 10000)
        return () => clearInterval(iv)
    }, [server.currentConnections])

    const parsedUsers = useMemo(() => {
        const map: Record<string, { uuid: string, up: number, down: number }> = {}
        for (const stat of users || []) {
            if (!stat?.name) continue
            const parts = stat.name.split(">>>")
            if (parts.length < 4) continue
            const uuid = parts[1], type = parts[3]
            if (!map[uuid]) map[uuid] = { uuid, up:0, down:0 }
            if (type === "uplink")   map[uuid].up   = stat.value || 0
            if (type === "downlink") map[uuid].down = stat.value || 0
        }
        return Object.values(map)
            .filter(u => u.uuid.toLowerCase().includes(search.toLowerCase()))
            .sort((a,b) => (b.up+b.down)-(a.up+a.down))
    }, [users, search])

    const totalTraffic = useMemo(() =>
            parsedUsers.reduce((a,u) => a + u.up + u.down, 0),
        [parsedUsers]
    )

    const handleSave = async () => {
        setSaving(true)
        try {
            await adminApi.updateServer(server.id, form)
            onUpdate()
            showToast("Настройки сохранены")
        } catch(e) { showToast("Ошибка сохранения") }
        setSaving(false)
    }

    const handleToggle = async () => {
        setToggling(true)
        try {
            await adminApi.toggleServerStatus(server.id, !server.isActive)
            onUpdate()
            showToast(server.isActive ? "Сервер отключён" : "Сервер включён")
        } catch(e) { showToast("Ошибка изменения статуса") }
        setToggling(false)
    }

    const handleDelete = async () => {
        if (!confirm(`Удалить сервер ${server.name}?`)) return
        try {
            await adminApi.deleteServer(server.id)
            onUpdate(); onClose()
        } catch(e) { showToast("Ошибка удаления") }
    }

    const handleKick = async (uuid: string) => {
        try {
            await adminApi.kickUser(server.id, uuid)
            showToast("Пользователь отключён")
            loadData()
        } catch(e) { showToast("Ошибка кика") }
    }

    const handleKickAll = async () => {
        if (!confirm("Сбросить ВСЕ подключения?")) return
        for (const u of parsedUsers) await adminApi.kickUser(server.id, u.uuid)
        showToast(`Сброшено ${parsedUsers.length} подключений`)
        loadData()
    }

    const loadPct = server.maxConnections > 0 ? Math.min(100, server.currentConnections/server.maxConnections*100) : 0
    const loadColor = loadPct > 80 ? "#ef4444" : loadPct > 50 ? "#f59e0b" : "#10b981"
    const healthColor = (server.healthScore||0) >= 80 ? "#10b981" : (server.healthScore||0) >= 50 ? "#f59e0b" : "#ef4444"
    const latColor = !server.avgLatencyMs ? "#475569" : server.avgLatencyMs < 50 ? "#10b981" : server.avgLatencyMs < 150 ? "#f59e0b" : "#ef4444"

    return (
        <div style={{ height:"100%", display:"flex", flexDirection:"column", background:"#080f1a", overflow:"hidden" }}>
            <style>{`
        @import url('https://fonts.googleapis.com/css2?family=DM+Mono:wght@400;500&family=Space+Grotesk:wght@500;700;800&family=Inter:wght@400;500;600&display=swap');
      `}</style>

            {/* Toast */}
            {toast && (
                <div style={{
                    position:"absolute", top:16, right:16, zIndex:100,
                    background:"#0f172a", border:"1px solid rgba(255,255,255,0.1)",
                    borderRadius:10, padding:"10px 16px", fontSize:13, fontWeight:600, color:"#e2e8f0",
                    boxShadow:"0 10px 30px rgba(0,0,0,0.5)"
                }}>{toast}</div>
            )}

            {/* Header */}
            <div style={{
                padding:"24px 24px 0",
                background:`linear-gradient(135deg, rgba(59,130,246,0.06), transparent)`,
                borderBottom:"1px solid rgba(255,255,255,0.06)", flexShrink:0
            }}>
                <div style={{ display:"flex", justifyContent:"space-between", alignItems:"flex-start", marginBottom:16 }}>
                    <div style={{ display:"flex", alignItems:"center", gap:12 }}>
                        <div style={{ fontSize:28 }}>{FLAG[server.countryCode] || "🌐"}</div>
                        <div>
                            <div style={{ fontSize:18, fontWeight:800, color:"#f1f5f9", fontFamily:"'Space Grotesk',sans-serif" }}>{server.name}</div>
                            <div style={{ fontSize:12, color:"#475569", marginTop:2 }}>
                                {server.location} · <span style={{ fontFamily:"'DM Mono',monospace" }}>{server.ipAddress}:{server.port}</span>
                            </div>
                        </div>
                    </div>
                    <div style={{ display:"flex", gap:8, alignItems:"center" }}>
                        <button onClick={handleToggle} disabled={toggling} title={server.isActive ? "Отключить" : "Включить"} style={{
                            padding:"6px 12px", borderRadius:8, fontSize:12, fontWeight:700, cursor:"pointer",
                            background: server.isActive ? "rgba(239,68,68,0.1)" : "rgba(16,185,129,0.1)",
                            border: `1px solid ${server.isActive ? "rgba(239,68,68,0.25)" : "rgba(16,185,129,0.25)"}`,
                            color: server.isActive ? "#ef4444" : "#10b981",
                            opacity: toggling ? 0.6 : 1
                        }}>{server.isActive ? "⏸" : "▶"}</button>
                        <button onClick={onClose} style={{
                            width:32, height:32, borderRadius:8, background:"rgba(255,255,255,0.05)",
                            border:"1px solid rgba(255,255,255,0.08)", color:"#475569", cursor:"pointer", fontSize:18
                        }}>×</button>
                    </div>
                </div>

                {/* Status badges */}
                <div style={{ display:"flex", gap:8, marginBottom:16, flexWrap:"wrap" }}>
          <span style={{
              fontSize:11, fontWeight:700, padding:"3px 10px", borderRadius:20,
              background: server.isActive ? "rgba(16,185,129,0.12)" : "rgba(239,68,68,0.12)",
              color: server.isActive ? "#10b981" : "#ef4444"
          }}>{server.isActive ? "● Online" : "● Offline"}</span>
                    <span style={{ fontSize:11, fontWeight:600, padding:"3px 10px", borderRadius:20, background:"rgba(255,255,255,0.05)", color:"#64748b" }}>
            ID #{server.id}
          </span>
                    <span style={{ fontSize:11, fontWeight:600, padding:"3px 10px", borderRadius:20, background:"rgba(255,255,255,0.05)", color:"#64748b" }}>
            {server.countryCode}
          </span>
                    {server.lastHealthCheck && (
                        <span style={{ fontSize:11, fontWeight:600, padding:"3px 10px", borderRadius:20, background:"rgba(255,255,255,0.05)", color:"#334155" }}>
              ↻ {new Date(server.lastHealthCheck).toLocaleTimeString("ru-RU")}
            </span>
                    )}
                </div>

                {/* Quick metrics */}
                <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr 1fr 1fr", gap:8, marginBottom:16 }}>
                    {[
                        { label:"Health", value:`${(server.healthScore||0).toFixed(0)}`, color:healthColor, suffix:"%" },
                        { label:"Latency", value:server.avgLatencyMs ? `${server.avgLatencyMs}` : "—", color:latColor, suffix:server.avgLatencyMs ? "ms" : "" },
                        { label:"Conns", value:`${server.currentConnections}`, color:loadColor, suffix:`/${server.maxConnections}` },
                        { label:"Load", value:`${loadPct.toFixed(0)}`, color:loadColor, suffix:"%" },
                    ].map(m => (
                        <div key={m.label} style={{ background:"rgba(255,255,255,0.03)", borderRadius:10, padding:"10px 12px" }}>
                            <div style={{ fontSize:9, color:"#475569", fontWeight:600, textTransform:"uppercase", letterSpacing:"0.07em", marginBottom:4 }}>{m.label}</div>
                            <div style={{ fontSize:18, fontWeight:800, color:m.color, fontFamily:"'DM Mono',monospace" }}>
                                {m.value}<span style={{ fontSize:11, color:"#475569" }}>{m.suffix}</span>
                            </div>
                        </div>
                    ))}
                </div>

                {/* Tabs */}
                <div style={{ display:"flex", gap:2 }}>
                    {[["overview","Обзор"],["users","Сессии"],["settings","Настройки"]].map(([k,l]) => (
                        <button key={k} onClick={() => setTab(k as any)} style={{
                            padding:"8px 16px", fontSize:13, fontWeight:600, cursor:"pointer",
                            background:"none", border:"none", borderRadius:"8px 8px 0 0",
                            color: tab === k ? "#60a5fa" : "#475569",
                            borderBottom: tab === k ? "2px solid #3b82f6" : "2px solid transparent",
                            marginBottom:-1
                        }}>{l}</button>
                    ))}
                </div>
            </div>

            {/* Tab content */}
            <div style={{ flex:1, overflowY:"auto", padding:20 }}>

                {/* OVERVIEW TAB */}
                {tab === "overview" && (
                    <div style={{ display:"flex", flexDirection:"column", gap:14 }}>

                        {/* Connection chart */}
                        <div style={{ background:"rgba(255,255,255,0.02)", border:"1px solid rgba(255,255,255,0.05)", borderRadius:14, padding:"16px 18px" }}>
                            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:12 }}>
                                <div style={{ fontSize:12, fontWeight:700, color:"#475569", textTransform:"uppercase", letterSpacing:"0.06em" }}>
                                    📈 Подключения (Live)
                                </div>
                                <span style={{ fontSize:22, fontWeight:800, color:loadColor, fontFamily:"'DM Mono',monospace" }}>
                  {server.currentConnections}
                </span>
                            </div>
                            {connHistory.length > 1 ? (
                                <div style={{ width:"100%", overflow:"hidden" }}>
                                    <MiniChart data={connHistory} color="#3b82f6"/>
                                </div>
                            ) : (
                                <div style={{ height:40, display:"flex", alignItems:"center", fontSize:12, color:"#334155" }}>
                                    Накапливаем данные...
                                </div>
                            )}
                            {/* Load bar */}
                            <div style={{ marginTop:10, height:5, background:"rgba(255,255,255,0.05)", borderRadius:3, overflow:"hidden" }}>
                                <div style={{ height:"100%", width:`${loadPct}%`, background:loadColor, borderRadius:3, transition:"width 0.5s" }}/>
                            </div>
                        </div>

                        {/* Reality config */}
                        <div style={{ background:"rgba(255,255,255,0.02)", border:"1px solid rgba(255,255,255,0.05)", borderRadius:14, overflow:"hidden" }}>
                            <div style={{ padding:"12px 16px", borderBottom:"1px solid rgba(255,255,255,0.04)", fontSize:11, fontWeight:700, color:"#334155", textTransform:"uppercase", letterSpacing:"0.08em" }}>
                                🔐 Reality Config
                            </div>
                            {[
                                { label:"Public Key", value:server.realityPublicKey, mono:true },
                                { label:"Short ID", value:server.realityShortId, mono:true },
                                { label:"SNI", value:server.realitySni },
                            ].map(row => (
                                <div key={row.label} style={{
                                    display:"flex", justifyContent:"space-between", alignItems:"center",
                                    padding:"10px 16px", borderBottom:"1px solid rgba(255,255,255,0.03)"
                                }}>
                                    <span style={{ fontSize:12, color:"#475569" }}>{row.label}</span>
                                    <div style={{ display:"flex", alignItems:"center", gap:4 }}>
                    <span style={{
                        fontSize:12, color:"#94a3b8", maxWidth:220, overflow:"hidden",
                        textOverflow:"ellipsis", whiteSpace:"nowrap",
                        fontFamily: row.mono ? "'DM Mono',monospace" : "inherit"
                    }}>{row.value || "—"}</span>
                                        {row.value && <CopyBtn value={row.value}/>}
                                    </div>
                                </div>
                            ))}
                        </div>

                        {/* Network info */}
                        <div style={{ background:"rgba(255,255,255,0.02)", border:"1px solid rgba(255,255,255,0.05)", borderRadius:14, overflow:"hidden" }}>
                            <div style={{ padding:"12px 16px", borderBottom:"1px solid rgba(255,255,255,0.04)", fontSize:11, fontWeight:700, color:"#334155", textTransform:"uppercase", letterSpacing:"0.08em" }}>
                                🌐 Сеть
                            </div>
                            {[
                                { label:"IP-адрес", value:server.ipAddress, mono:true },
                                { label:"Порт VLESS", value:String(server.port), mono:true },
                                { label:"gRPC порт", value:server.grpcPort ? String(server.grpcPort) : "—", mono:true },
                                { label:"Страна", value:`${FLAG[server.countryCode]||""} ${server.countryCode}` },
                                { label:"Локация", value:server.location },
                            ].map(row => (
                                <div key={row.label} style={{
                                    display:"flex", justifyContent:"space-between", alignItems:"center",
                                    padding:"10px 16px", borderBottom:"1px solid rgba(255,255,255,0.03)"
                                }}>
                                    <span style={{ fontSize:12, color:"#475569" }}>{row.label}</span>
                                    <div style={{ display:"flex", alignItems:"center", gap:4 }}>
                    <span style={{
                        fontSize:12, color:"#94a3b8",
                        fontFamily: row.mono ? "'DM Mono',monospace" : "inherit"
                    }}>{row.value}</span>
                                        {row.mono && row.value !== "—" && <CopyBtn value={row.value}/>}
                                    </div>
                                </div>
                            ))}
                        </div>

                        {/* Health history (from server DTO) */}
                        <div style={{ background:"rgba(255,255,255,0.02)", border:"1px solid rgba(255,255,255,0.05)", borderRadius:14, overflow:"hidden" }}>
                            <div style={{ padding:"12px 16px", borderBottom:"1px solid rgba(255,255,255,0.04)", fontSize:11, fontWeight:700, color:"#334155", textTransform:"uppercase", letterSpacing:"0.08em" }}>
                                ❤️ Здоровье
                            </div>
                            {[
                                { label:"Health Score", value:server.healthScore?.toFixed(2) ?? "—", color:healthColor },
                                { label:"Avg Latency", value:server.avgLatencyMs ? `${server.avgLatencyMs} ms` : "—", color:latColor },
                                { label:"Max Connections", value:String(server.maxConnections) },
                                { label:"Last Check", value:server.lastHealthCheck ? new Date(server.lastHealthCheck).toLocaleString("ru-RU") : "—" },
                            ].map(row => (
                                <div key={row.label} style={{
                                    display:"flex", justifyContent:"space-between", alignItems:"center",
                                    padding:"10px 16px", borderBottom:"1px solid rgba(255,255,255,0.03)"
                                }}>
                                    <span style={{ fontSize:12, color:"#475569" }}>{row.label}</span>
                                    <span style={{ fontSize:13, fontWeight:700, color:(row as any).color || "#94a3b8", fontFamily:"'DM Mono',monospace" }}>
                    {row.value}
                  </span>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {/* SESSIONS TAB */}
                {tab === "users" && (
                    <div style={{ display:"flex", flexDirection:"column", gap:12 }}>
                        {/* Summary */}
                        <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr 1fr", gap:10 }}>
                            {[
                                { label:"Активных сессий", value:parsedUsers.length, color:"#3b82f6" },
                                { label:"Трафик сессии", value:fmtBytes(totalTraffic), color:"#10b981" },
                                { label:"Статус", value:loading ? "Sync..." : "Live", color:"#f59e0b" },
                            ].map(s => (
                                <div key={s.label} style={{ background:"rgba(255,255,255,0.03)", border:"1px solid rgba(255,255,255,0.05)", borderRadius:10, padding:"12px 14px" }}>
                                    <div style={{ fontSize:10, color:"#475569", fontWeight:600, textTransform:"uppercase", letterSpacing:"0.06em", marginBottom:4 }}>{s.label}</div>
                                    <div style={{ fontSize:18, fontWeight:800, color:s.color, fontFamily:"'DM Mono',monospace" }}>{s.value}</div>
                                </div>
                            ))}
                        </div>

                        {/* Search + kick all */}
                        <div style={{ display:"flex", gap:8 }}>
                            <div style={{ flex:1, position:"relative" }}>
                                <span style={{ position:"absolute", left:10, top:"50%", transform:"translateY(-50%)", color:"#334155", fontSize:12 }}>🔍</span>
                                <input value={search} onChange={e => setSearch(e.target.value)}
                                       placeholder="Поиск по UUID..."
                                       style={{
                                           width:"100%", padding:"9px 12px 9px 30px",
                                           background:"rgba(255,255,255,0.04)", border:"1px solid rgba(255,255,255,0.07)",
                                           borderRadius:8, color:"#e2e8f0", fontSize:13, outline:"none", boxSizing:"border-box"
                                       }}/>
                            </div>
                            {parsedUsers.length > 0 && (
                                <button onClick={handleKickAll} style={{
                                    padding:"9px 14px", background:"rgba(239,68,68,0.1)",
                                    border:"1px solid rgba(239,68,68,0.2)", borderRadius:8,
                                    color:"#ef4444", fontSize:12, fontWeight:700, cursor:"pointer", whiteSpace:"nowrap"
                                }}>⚡ Сбросить всех</button>
                            )}
                        </div>

                        {/* Session list */}
                        {!server.isActive ? (
                            <div style={{ textAlign:"center", padding:"40px 0", color:"#334155", border:"2px dashed rgba(255,255,255,0.05)", borderRadius:12 }}>
                                <div style={{ fontSize:28, marginBottom:8 }}>⛔</div>
                                <div style={{ fontSize:13 }}>Сервер отключён</div>
                            </div>
                        ) : loading && parsedUsers.length === 0 ? (
                            <div style={{ textAlign:"center", padding:"40px 0", color:"#334155" }}>
                                <div style={{ fontSize:13 }}>Синхронизация с Xray...</div>
                            </div>
                        ) : parsedUsers.length === 0 ? (
                            <div style={{ textAlign:"center", padding:"40px 0", color:"#334155", border:"2px dashed rgba(255,255,255,0.05)", borderRadius:12 }}>
                                <div style={{ fontSize:28, marginBottom:8 }}>📡</div>
                                <div style={{ fontSize:13 }}>Нет активных сессий</div>
                            </div>
                        ) : (
                            <div style={{ display:"flex", flexDirection:"column", gap:8 }}>
                                {parsedUsers.map(u => {
                                    const dbStat = dbStats.find(s => s.userId?.toString() === u.uuid)
                                    const totalDb = dbStat ? dbStat.totalUp + dbStat.totalDown : 0
                                    const sessionTotal = u.up + u.down
                                    const barPct = totalDb > 0 ? Math.min(100, sessionTotal/totalDb*100) : 100

                                    return (
                                        <div key={u.uuid} style={{
                                            background:"rgba(255,255,255,0.03)", border:"1px solid rgba(255,255,255,0.06)",
                                            borderRadius:12, padding:"14px 16px"
                                        }}>
                                            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"flex-start", marginBottom:10 }}>
                                                <div style={{ display:"flex", alignItems:"center", gap:10 }}>
                                                    <div style={{
                                                        width:32, height:32, borderRadius:8, background:"rgba(16,185,129,0.12)",
                                                        display:"flex", alignItems:"center", justifyContent:"center",
                                                        fontSize:14, flexShrink:0, position:"relative"
                                                    }}>
                                                        <span>👤</span>
                                                        <span style={{
                                                            position:"absolute", top:0, right:0, width:7, height:7,
                                                            borderRadius:"50%", background:"#10b981",
                                                            animation:"pulse 2s infinite"
                                                        }}/>
                                                    </div>
                                                    <div>
                                                        <div style={{ fontSize:12, fontWeight:600, color:"#94a3b8", fontFamily:"'DM Mono',monospace" }}>
                                                            {u.uuid.substring(0,20)}...
                                                            <span style={{ marginLeft:4 }}><CopyBtn value={u.uuid}/></span>
                                                        </div>
                                                        <div style={{ fontSize:11, color:"#334155", marginTop:2 }}>VLESS · TCP · Reality</div>
                                                    </div>
                                                </div>
                                                <button onClick={() => handleKick(u.uuid)} title="Кикнуть" style={{
                                                    padding:"5px 10px", background:"rgba(239,68,68,0.1)",
                                                    border:"1px solid rgba(239,68,68,0.2)", borderRadius:6,
                                                    color:"#ef4444", fontSize:11, fontWeight:700, cursor:"pointer"
                                                }}>Kick</button>
                                            </div>

                                            {/* Traffic breakdown */}
                                            <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr 1fr", gap:8, marginBottom:10 }}>
                                                <div style={{ background:"rgba(255,255,255,0.02)", borderRadius:8, padding:"8px 10px" }}>
                                                    <div style={{ fontSize:9, color:"#475569", fontWeight:600, textTransform:"uppercase", letterSpacing:"0.06em", marginBottom:3 }}>Сессия ↑</div>
                                                    <div style={{ fontSize:13, fontWeight:700, color:"#3b82f6", fontFamily:"'DM Mono',monospace" }}>{fmtBytes(u.up)}</div>
                                                </div>
                                                <div style={{ background:"rgba(255,255,255,0.02)", borderRadius:8, padding:"8px 10px" }}>
                                                    <div style={{ fontSize:9, color:"#475569", fontWeight:600, textTransform:"uppercase", letterSpacing:"0.06em", marginBottom:3 }}>Сессия ↓</div>
                                                    <div style={{ fontSize:13, fontWeight:700, color:"#10b981", fontFamily:"'DM Mono',monospace" }}>{fmtBytes(u.down)}</div>
                                                </div>
                                                <div style={{ background:"rgba(255,255,255,0.02)", borderRadius:8, padding:"8px 10px" }}>
                                                    <div style={{ fontSize:9, color:"#475569", fontWeight:600, textTransform:"uppercase", letterSpacing:"0.06em", marginBottom:3 }}>Всего (БД)</div>
                                                    <div style={{ fontSize:13, fontWeight:700, color:"#f59e0b", fontFamily:"'DM Mono',monospace" }}>{fmtBytes(totalDb)}</div>
                                                </div>
                                            </div>

                                            {/* Session vs total bar */}
                                            {totalDb > 0 && (
                                                <div>
                                                    <div style={{ display:"flex", justifyContent:"space-between", fontSize:10, color:"#334155", marginBottom:4 }}>
                                                        <span>Сессия {fmtBytes(sessionTotal)}</span>
                                                        <span>Total {fmtBytes(totalDb)}</span>
                                                    </div>
                                                    <div style={{ height:3, background:"rgba(255,255,255,0.05)", borderRadius:2, overflow:"hidden" }}>
                                                        <div style={{ height:"100%", width:`${barPct}%`, background:"#f59e0b", borderRadius:2 }}/>
                                                    </div>
                                                </div>
                                            )}
                                        </div>
                                    )
                                })}
                            </div>
                        )}
                    </div>
                )}

                {/* SETTINGS TAB */}
                {tab === "settings" && (
                    <div style={{ display:"flex", flexDirection:"column", gap:14 }}>
                        <div style={{ background:"rgba(255,255,255,0.02)", border:"1px solid rgba(255,255,255,0.05)", borderRadius:14, padding:"18px 20px" }}>
                            <div style={{ fontSize:12, fontWeight:700, color:"#334155", textTransform:"uppercase", letterSpacing:"0.07em", marginBottom:16 }}>Основные настройки</div>

                            {[
                                { label:"Локация", key:"location", type:"text", placeholder:"Amsterdam, NL" },
                                { label:"Max подключений", key:"maxConnections", type:"number", placeholder:"1000" },
                                { label:"Reality SNI", key:"realitySni", type:"text", placeholder:"eh.vk.com" },
                            ].map(f => (
                                <div key={f.key} style={{ marginBottom:14 }}>
                                    <label style={{ fontSize:11, color:"#475569", fontWeight:600, display:"block", marginBottom:6, textTransform:"uppercase", letterSpacing:"0.05em" }}>{f.label}</label>
                                    <input
                                        type={f.type}
                                        value={(form as any)[f.key]}
                                        onChange={e => setForm(p => ({ ...p, [f.key]: f.type === "number" ? Number(e.target.value) : e.target.value }))}
                                        placeholder={f.placeholder}
                                        style={{
                                            width:"100%", padding:"10px 12px",
                                            background:"rgba(255,255,255,0.04)", border:"1px solid rgba(255,255,255,0.08)",
                                            borderRadius:8, color:"#e2e8f0", fontSize:13, outline:"none"
                                        }}/>
                                </div>
                            ))}

                            <div style={{ display:"flex", gap:10, marginTop:6 }}>
                                <button onClick={handleSave} disabled={saving} style={{
                                    flex:2, padding:"11px", background:"linear-gradient(135deg,#3b82f6,#2563eb)",
                                    border:"none", borderRadius:8, color:"white", fontSize:13, fontWeight:700, cursor:"pointer",
                                    opacity:saving ? 0.7 : 1
                                }}>{saving ? "Сохранение..." : "Сохранить"}</button>
                                <button onClick={handleToggle} disabled={toggling} style={{
                                    flex:1, padding:"11px",
                                    background: server.isActive ? "rgba(239,68,68,0.1)" : "rgba(16,185,129,0.1)",
                                    border: `1px solid ${server.isActive ? "rgba(239,68,68,0.2)" : "rgba(16,185,129,0.2)"}`,
                                    borderRadius:8,
                                    color: server.isActive ? "#ef4444" : "#10b981",
                                    fontSize:13, fontWeight:700, cursor:"pointer"
                                }}>{server.isActive ? "Выключить" : "Включить"}</button>
                            </div>
                        </div>

                        {/* Read-only info */}
                        <div style={{ background:"rgba(255,255,255,0.02)", border:"1px solid rgba(255,255,255,0.05)", borderRadius:14, overflow:"hidden" }}>
                            <div style={{ padding:"12px 16px", borderBottom:"1px solid rgba(255,255,255,0.04)", fontSize:11, fontWeight:700, color:"#334155", textTransform:"uppercase", letterSpacing:"0.08em" }}>
                                Только для чтения
                            </div>
                            {[
                                { label:"Public Key", value:server.realityPublicKey },
                                { label:"Short ID", value:server.realityShortId },
                                { label:"IP:Port", value:`${server.ipAddress}:${server.port}` },
                                { label:"gRPC Port", value:String(server.grpcPort || "—") },
                            ].map(row => (
                                <div key={row.label} style={{
                                    display:"flex", justifyContent:"space-between", alignItems:"center",
                                    padding:"10px 16px", borderBottom:"1px solid rgba(255,255,255,0.03)"
                                }}>
                                    <span style={{ fontSize:12, color:"#475569" }}>{row.label}</span>
                                    <div style={{ display:"flex", alignItems:"center", gap:4 }}>
                    <span style={{ fontSize:11, color:"#64748b", fontFamily:"'DM Mono',monospace", maxWidth:220, overflow:"hidden", textOverflow:"ellipsis", whiteSpace:"nowrap" }}>
                      {row.value || "—"}
                    </span>
                                        {row.value && row.value !== "—" && <CopyBtn value={row.value}/>}
                                    </div>
                                </div>
                            ))}
                        </div>

                        {/* Danger zone */}
                        <div style={{ background:"rgba(239,68,68,0.04)", border:"1px solid rgba(239,68,68,0.15)", borderRadius:14, padding:"16px 18px" }}>
                            <div style={{ fontSize:12, fontWeight:700, color:"#7f1d1d", textTransform:"uppercase", letterSpacing:"0.07em", marginBottom:12 }}>Опасная зона</div>
                            <button onClick={handleDelete} style={{
                                width:"100%", padding:"11px", background:"rgba(239,68,68,0.1)",
                                border:"1px solid rgba(239,68,68,0.2)", borderRadius:8,
                                color:"#ef4444", fontSize:13, fontWeight:700, cursor:"pointer"
                            }}>🗑 Удалить сервер</button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    )
}