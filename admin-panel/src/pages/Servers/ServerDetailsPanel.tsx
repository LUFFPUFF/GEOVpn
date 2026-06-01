import { useState, useEffect, useMemo } from "react"
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
                    <stop offset="0%" stopColor={color} stopOpacity={0.15}/>
                    <stop offset="100%" stopColor={color} stopOpacity={0}/>
                </linearGradient>
            </defs>
            <polygon points={`0,${H} ${pts} ${W},${H}`} fill={`url(#g${color.replace("#","")})`} />
            <polyline points={pts} fill="none" stroke={color} strokeWidth={2} strokeLinecap="round" strokeLinejoin="round"/>
            {data.length > 0 && (
                <circle cx={W} cy={H - (data[data.length-1]/max)*H} r={3.5} fill="white" stroke={color} strokeWidth={2} />
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
        <button onClick={copy} className="text-slate-400 hover:text-blue-500 cursor-pointer text-xs p-1" title="Копировать">
            {copied ? "✓" : "⎘"}
        </button>
    )
}

export default function ServerDetailsPanel({ server, onClose, onUpdate }: { server: ServerDto, onClose: () => void, onUpdate: () => void }) {
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
            const [xray, db] = await Promise.all([adminApi.getServerUsers(server.id), adminApi.getServerTrafficStats(server.id)])
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
            location: server.location || "", maxConnections: server.maxConnections || 1000,
            realitySni: server.realitySni || "eh.vk.com", isActive: server.isActive,
        })
        if (tab === "users") loadData()
    }, [server.id])

    useEffect(() => {
        if (tab !== "users") return
        loadData()
        const iv = setInterval(loadData, 5000)
        return () => clearInterval(iv)
    }, [tab, server.id])

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

    const totalTraffic = useMemo(() => parsedUsers.reduce((a,u) => a + u.up + u.down, 0), [parsedUsers])

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
    const latColor = !server.avgLatencyMs ? "#64748b" : server.avgLatencyMs < 50 ? "#10b981" : server.avgLatencyMs < 150 ? "#f59e0b" : "#ef4444"

    return (
        <div style={{ height:"100%", display:"flex", flexDirection:"column", background:"var(--bg-card)", overflow:"hidden" }}>
            {toast && (
                <div className="slide-down-fade animate-in fixed top-4 right-4 z-55 bg-slate-900 text-white font-bold rounded-lg px-4 py-2 text-xs shadow-xl">{toast}</div>
            )}

            {/* Header */}
            <div style={{
                padding:"24px 24px 0", background:`linear-gradient(180deg, var(--bg-input) 0%, var(--bg-card) 100%)`,
                borderBottom:"1px solid var(--border-color)"
            }}>
                <div style={{ display:"flex", justifyContent:"space-between", alignItems:"flex-start", marginBottom:16 }}>
                    <div style={{ display:"flex", alignItems:"center", gap:14 }}>
                        <div style={{ fontSize:32 }}>{FLAG[server.countryCode] || "🌐"}</div>
                        <div>
                            <div style={{ fontSize:20, fontWeight:800, fontFamily:"'Space Grotesk',sans-serif" }}>{server.name}</div>
                            <div style={{ fontSize:13, color:"var(--text-muted)", marginTop:2 }}>
                                {server.location} <span style={{ margin:"0 6px" }}>•</span> <span style={{ fontFamily:"'DM Mono',monospace", background:"var(--bg-input)", padding:"2px 6px", borderRadius:4, color:"var(--text-main)" }}>{server.ipAddress}:{server.port}</span>
                            </div>
                        </div>
                    </div>
                    <div style={{ display:"flex", gap:8, alignItems:"center" }}>
                        <button onClick={handleToggle} disabled={toggling} title={server.isActive ? "Отключить" : "Включить"} className="w-9 h-9 flex items-center justify-center rounded-lg cursor-pointer" style={{
                            background: server.isActive ? "#fee2e2" : "#d1fae5",
                            color: server.isActive ? "#ef4444" : "#10b981",
                            opacity: toggling ? 0.6 : 1
                        }}>{server.isActive ? "⏸" : "▶"}</button>
                        <button onClick={onClose} className="w-9 h-9 flex items-center justify-center rounded-lg hover:bg-slate-100 cursor-pointer text-slate-400">✕</button>
                    </div>
                </div>

                {/* Status tokens */}
                <div style={{ display:"flex", gap:8, marginBottom:20, flexWrap:"wrap" }}>
                    <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full" style={{ background: server.isActive ? "#d1fae5" : "#fee2e2", color: server.isActive ? "#059669" : "#dc2626" }}>
                        {server.isActive ? "● Online" : "● Offline"}
                    </span>
                    <span className="text-[10px] font-bold px-2 py-0.5 bg-slate-100 dark:bg-slate-800 text-slate-500 rounded-full">ID #{server.id}</span>
                    <span className="text-[10px] font-bold px-2 py-0.5 bg-slate-100 dark:bg-slate-800 text-slate-500 rounded-full">{server.countryCode}</span>
                    {server.lastHealthCheck && (
                        <span className="text-[10px] font-bold px-2 py-0.5 bg-slate-100 dark:bg-slate-800 text-slate-500 rounded-full">↻ {new Date(server.lastHealthCheck).toLocaleTimeString()}</span>
                    )}
                </div>

                {/* Quick stats grid */}
                <div style={{ display:"grid", gridTemplateColumns:"repeat(4, 1fr)", gap:12, marginBottom:20 }}>
                    {[
                        { label:"Health", value:`${(server.healthScore||0).toFixed(0)}`, color:healthColor, suffix:"%" },
                        { label:"Latency", value:server.avgLatencyMs ? `${server.avgLatencyMs}` : "—", color:latColor, suffix:server.avgLatencyMs ? "ms" : "" },
                        { label:"Conns", value:`${server.currentConnections}`, color:loadColor, suffix:`/${server.maxConnections}` },
                        { label:"Load", value:`${loadPct.toFixed(0)}`, color:loadColor, suffix:"%" },
                    ].map(m => (
                        <div key={m.label} style={{ background:"var(--bg-input)", border:"1px solid var(--border-color)", borderRadius:12, padding:"12px" }}>
                            <div style={{ fontSize:10, color:"var(--text-muted)", fontWeight:700, textTransform:"uppercase", letterSpacing:"0.05em", marginBottom:4 }}>{m.label}</div>
                            <div style={{ fontSize:18, fontWeight:800, color:m.color, fontFamily:"'DM Mono',monospace" }}>
                                {m.value}<span style={{ fontSize:11, color:"var(--text-muted)", fontWeight:600, marginLeft:1 }}>{m.suffix}</span>
                            </div>
                        </div>
                    ))}
                </div>

                {/* Tabs tabs */}
                <div style={{ display:"flex", gap:24, borderBottom:"1px solid var(--border-color)" }}>
                    {[["overview","Обзор"],["users","Сессии"],["settings","Настройки"]].map(([k,l]) => (
                        <button key={k} onClick={() => setTab(k as any)} style={{
                            padding:"0 0 12px 0", fontSize:14, fontWeight:600, cursor:"pointer",
                            background:"none", border:"none",
                            color: tab === k ? "#2563eb" : "var(--text-muted)",
                            borderBottom: tab === k ? "2px solid #2563eb" : "2px solid transparent",
                            marginBottom:-1, transition:"all 0.2s"
                        }}>{l}</button>
                    ))}
                </div>
            </div>

            {/* Tab content inside a base layout container */}
            <div style={{ flex:1, overflowY:"auto", padding:24, background:"var(--bg-base)" }}>

                {/* OVERVIEW TAB */}
                {tab === "overview" && (
                    <div style={{ display:"flex", flexDirection:"column", gap:16 }}>
                        {/* Connection chart */}
                        <div className="info-card">
                            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:12 }}>
                                <div style={{ fontSize:12, fontWeight:700, color:"var(--text-muted)", textTransform:"uppercase", letterSpacing:"0.05em" }}>📈 Live Подключения</div>
                                <span style={{ fontSize:24, fontWeight:800, color:loadColor, fontFamily:"'DM Mono',monospace" }}>{server.currentConnections}</span>
                            </div>
                            {connHistory.length > 1 ? (
                                <div style={{ width:"100%", overflow:"hidden", marginTop:10 }}>
                                    <MiniChart data={connHistory} color="#3b82f6"/>
                                </div>
                            ) : (
                                <div style={{ height:50, display:"flex", alignItems:"center", justifyContent:"center", fontSize:13, color:"var(--text-muted)", background:"var(--bg-input)", borderRadius:8 }}>Накапливаем данные...</div>
                            )}
                            <div style={{ marginTop:16, height:6, background:"var(--border-color)", borderRadius:3, overflow:"hidden" }}>
                                <div style={{ height:"100%", width:`${loadPct}%`, background:loadColor, borderRadius:3 }}/>
                            </div>
                        </div>

                        {/* Config blocks */}
                        {[
                            {
                                title: "🔐 Reality Config",
                                rows: [
                                    { label:"Public Key", value:server.realityPublicKey, mono:true },
                                    { label:"Short ID", value:server.realityShortId, mono:true },
                                    { label:"SNI", value:server.realitySni },
                                ]
                            },
                            {
                                title: "🌐 Сеть",
                                rows: [
                                    { label:"IP-адрес", value:server.ipAddress, mono:true },
                                    { label:"Порт VLESS", value:String(server.port), mono:true },
                                    { label:"gRPC порт", value:server.grpcPort ? String(server.grpcPort) : "—", mono:true },
                                    { label:"Страна", value:`${FLAG[server.countryCode]||""} ${server.countryCode}` },
                                    { label:"Локация", value:server.location },
                                ]
                            },
                            {
                                title: "❤️ Здоровье",
                                rows: [
                                    { label:"Health Score", value:server.healthScore?.toFixed(2) ?? "—", color:healthColor },
                                    { label:"Avg Latency", value:server.avgLatencyMs ? `${server.avgLatencyMs} ms` : "—", color:latColor },
                                    { label:"Max Connections", value:String(server.maxConnections) },
                                    { label:"Last Check", value:server.lastHealthCheck ? new Date(server.lastHealthCheck).toLocaleString() : "—" },
                                ]
                            }
                        ].map((block, i) => (
                            <div key={i} className="info-card" style={{ padding:0 }}>
                                <div style={{ padding:"14px 18px", borderBottom:"1px solid var(--border-color)", background:"var(--bg-input)", borderRadius:"12px 12px 0 0", fontSize:12, fontWeight:700, color:"var(--text-muted)", textTransform:"uppercase", letterSpacing:"0.05em" }}>
                                    {block.title}
                                </div>
                                <div style={{ padding:"4px 0" }}>
                                    {block.rows.map(row => (
                                        <div key={row.label} className="list-row">
                                            <span style={{ fontSize:13, color:"var(--text-muted)", fontWeight:500 }}>{row.label}</span>
                                            <div style={{ display:"flex", alignItems:"center", gap:8 }}>
                                                <span style={{
                                                    fontSize:13, color:(row as any).color || "var(--text-main)", fontWeight:row.mono ? 600 : 500,
                                                    fontFamily: row.mono ? "'DM Mono',monospace" : "inherit", maxWidth:200, overflow:"hidden", textOverflow:"ellipsis", whiteSpace:"nowrap"
                                                }}>{row.value || "—"}</span>
                                                {row.value && row.value !== "—" && !row.color && <CopyBtn value={row.value}/>}
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        ))}
                    </div>
                )}

                {/* SESSIONS TAB */}
                {tab === "users" && (
                    <div style={{ display:"flex", flexDirection:"column", gap:16 }}>
                        <div style={{ display:"grid", gridTemplateColumns:"repeat(3, 1fr)", gap:12 }}>
                            {[
                                { label:"Сессий", value:parsedUsers.length, color:"#3b82f6", bg:"rgba(59, 130, 246, 0.1)" },
                                { label:"Трафик", value:fmtBytes(totalTraffic), color:"#10b981", bg:"rgba(16, 185, 129, 0.1)" },
                                { label:"Статус", value:loading ? "Sync..." : "Live", color:"#f59e0b", bg:"rgba(245, 158, 11, 0.1)" },
                            ].map(s => (
                                <div key={s.label} style={{ background:s.bg, border:`1px solid ${s.color}33`, borderRadius:12, padding:"12px" }}>
                                    <div style={{ fontSize:10, color:s.color, fontWeight:700, textTransform:"uppercase", letterSpacing:"0.05em", marginBottom:4 }}>{s.label}</div>
                                    <div style={{ fontSize:18, fontWeight:800, color:s.color, fontFamily:"'DM Mono',monospace" }}>{s.value}</div>
                                </div>
                            ))}
                        </div>

                        <div className="flex gap-2 w-full">
                            <input className="flex-1 h-10 px-3 border border-slate-200 bg-white rounded-lg text-xs" value={search} onChange={e => setSearch(e.target.value)} placeholder="Поиск сессии по UUID..."/>
                            {parsedUsers.length > 0 && (
                                <button onClick={handleKickAll} className="px-4 h-10 bg-red-100 hover:bg-red-200 text-red-650 rounded-lg text-xs font-bold cursor-pointer">⚡ Сбросить всех</button>
                            )}
                        </div>

                        {!server.isActive ? (
                            <div className="text-center p-12 border-2 border-dashed border-slate-200 rounded-2xl text-xs text-slate-400">⛔<br/>Сервер отключён</div>
                        ) : loading && parsedUsers.length === 0 ? (
                            <div className="text-center p-12 border-2 border-dashed border-slate-200 rounded-2xl text-xs text-slate-400 animate-pulse">Синхронизация Xray...</div>
                        ) : parsedUsers.length === 0 ? (
                            <div className="text-center p-12 border-2 border-dashed border-slate-200 rounded-2xl text-xs text-slate-400">📡<br/>Нет активных туннелей</div>
                        ) : (
                            <div style={{ display:"flex", flexDirection:"column", gap:10 }}>
                                {parsedUsers.map(u => {
                                    const dbStat = dbStats.find(s => s.userId?.toString() === u.uuid)
                                    const totalDb = dbStat ? dbStat.totalUp + dbStat.totalDown : 0
                                    const sessionTotal = u.up + u.down
                                    const barPct = totalDb > 0 ? Math.min(100, sessionTotal/totalDb*100) : 100

                                    return (
                                        <div key={u.uuid} className="info-card">
                                            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"flex-start", marginBottom:12 }}>
                                                <div style={{ display:"flex", alignItems:"center", gap:12 }}>
                                                    <div style={{ width:36, height:36, borderRadius:10, bg:"rgba(16,185,129,0.1)", display:"flex", alignItems:"center", justifyContent:"center", fontSize:16, border:"1px solid #10b981" }} className="relative bg-emerald-50">
                                                        👤
                                                    </div>
                                                    <div>
                                                        <div style={{ fontSize:13, fontWeight:700, color:"var(--text-main)", fontFamily:"'DM Mono',monospace", display:"flex", alignItems:"center", gap:6 }}>
                                                            {u.uuid.substring(0,18)}... <CopyBtn value={u.uuid}/>
                                                        </div>
                                                        <div style={{ fontSize:11, color:"var(--text-muted)", marginTop:1 }}>VLESS • TCP • Reality</div>
                                                    </div>
                                                </div>
                                                <button onClick={() => handleKick(u.uuid)} className="px-3 h-8 bg-red-100 hover:bg-red-200 text-red-650 rounded-lg text-xs font-bold cursor-pointer">Kick</button>
                                            </div>

                                            <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr 1fr", gap:8, marginBottom:12 }}>
                                                <div style={{ background:"var(--bg-input)", borderRadius:8, padding:"8px 12px" }}>
                                                    <div style={{ fontSize:10, color:"var(--text-muted)", fontWeight:600, textTransform:"uppercase", marginBottom:2 }}>↑ Upload</div>
                                                    <div style={{ fontSize:13, fontWeight:700, color:"#3b82f6", fontFamily:"'DM Mono',monospace" }}>{fmtBytes(u.up)}</div>
                                                </div>
                                                <div style={{ background:"var(--bg-input)", borderRadius:8, padding:"8px 12px" }}>
                                                    <div style={{ fontSize:10, color:"var(--text-muted)", fontWeight:600, textTransform:"uppercase", marginBottom:2 }}>↓ Download</div>
                                                    <div style={{ fontSize:13, fontWeight:700, color:"#10b981", fontFamily:"'DM Mono',monospace" }}>{fmtBytes(u.down)}</div>
                                                </div>
                                                <div style={{ background:"rgba(245,158,11,0.1)", borderRadius:8, padding:"8px 12px" }}>
                                                    <div style={{ fontSize:10, color:"#d97706", fontWeight:600, textTransform:"uppercase", marginBottom:2 }}>Total DB</div>
                                                    <div style={{ fontSize:13, fontWeight:700, color:"#d97706", fontFamily:"'DM Mono',monospace" }}>{fmtBytes(totalDb)}</div>
                                                </div>
                                            </div>

                                            {totalDb > 0 && (
                                                <div>
                                                    <div className="flex justify-between text-slate-400 font-medium text-[10px] mb-1">
                                                        <span>В этой сессии: {fmtBytes(sessionTotal)}</span>
                                                        <span>Накоплено: {fmtBytes(totalDb)}</span>
                                                    </div>
                                                    <div style={{ height:4, background:"#e2e8f0", borderRadius:2, overflow:"hidden" }}>
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
                    <div style={{ display:"flex", flexDirection:"column", gap:16 }}>
                        <div className="info-card">
                            <div style={{ fontSize:14, fontWeight:800, color:"var(--text-main)", marginBottom:16 }}>⚙️ Основные настройки</div>
                            {[
                                { label:"Локация", key:"location", type:"text", placeholder:"Amsterdam, NL" },
                                { label:"Max подключений", key:"maxConnections", type:"number", placeholder:"1000" },
                                { label:"Reality SNI", key:"realitySni", type:"text", placeholder:"eh.vk.com" },
                            ].map(f => (
                                <div key={f.key} style={{ marginBottom:16 }}>
                                    <label className="text-[10px] uppercase font-bold text-slate-400 block mb-1">{f.label}</label>
                                    <input type={f.type} className="w-full h-10 px-3 border border-slate-200 bg-white rounded-lg text-xs" value={(form as any)[f.key]} onChange={e => setForm(p => ({ ...p, [f.key]: f.type === "number" ? Number(e.target.value) : e.target.value }))} placeholder={f.placeholder} />
                                </div>
                            ))}
                            <div style={{ display:"flex", gap:12, marginTop:24 }}>
                                <button onClick={handleSave} disabled={saving} className="flex-1 h-11 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-xl cursor-pointer text-sm">
                                    {saving ? "Сохранение..." : "Сохранить"}
                                </button>
                                <button onClick={handleToggle} disabled={toggling} className="px-4 h-11 border border-slate-200 font-bold rounded-xl cursor-pointer text-xs text-slate-700 hover:bg-slate-50">
                                    {server.isActive ? "Выключить" : "Включить"}
                                </button>
                            </div>
                        </div>

                        <div className="info-card" style={{ padding:0 }}>
                            <div style={{ padding:"14px 18px", borderBottom:"1px solid var(--border-color)", background:"var(--bg-input)", borderRadius:"12px 12px 0 0", fontSize:12, fontWeight:700, color:"var(--text-muted)", textTransform:"uppercase" }}>🔒 Только для чтения</div>
                            <div style={{ padding:"4px 0" }}>
                                {[
                                    { label:"Public Key", value:server.realityPublicKey },
                                    { label:"Short ID", value:server.realityShortId },
                                    { label:"IP:Port", value:`${server.ipAddress}:${server.port}` },
                                    { label:"gRPC Port", value:String(server.grpcPort || "—") },
                                ].map(row => (
                                    <div key={row.label} className="list-row">
                                        <span style={{ fontSize:13, color:"var(--text-muted)", fontWeight:500 }}>{row.label}</span>
                                        <div style={{ display:"flex", alignItems:"center", gap:8 }}>
                                            <span style={{ fontSize:12, color:"var(--text-main)", fontFamily:"'DM Mono',monospace", maxWidth:200, overflow:"hidden", textOverflow:"ellipsis", whiteSpace:"nowrap" }}>{row.value || "—"}</span>
                                            {row.value && row.value !== "—" && <CopyBtn value={row.value}/>}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>

                        <div className="info-card" style={{ background:"#fef2f2", borderColor:"#fecaca" }}>
                            <div style={{ fontSize:14, fontWeight:800, color:"#991b1b", marginBottom:12 }}>⚠️ Опасная зона</div>
                            <button onClick={handleDelete} className="w-full h-10 bg-red-600 hover:bg-red-700 text-white font-bold rounded-lg text-xs cursor-pointer">🗑 Удалить сервер</button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    )
}
