import React, { useState, useEffect } from "react"
import { motion, AnimatePresence } from "motion/react"
import {
    Search,
    Smartphone,
    Server,
    Edit,
    Check,
    CheckCircle,
    RefreshCcw,
    Key,
    Wifi,
    Globe,
    FileText,
    ExternalLink,
    Lock
} from "lucide-react"
import { adminApi } from "@/api/admin"
import { VpnConfiguration } from "@/types/api"

export default function VpnConfigs() {
    const [configs, setConfigs] = useState<VpnConfiguration[]>([])
    const [servers, setServers] = useState<any[]>([])
    const [searchQuery, setSearchQuery] = useState("")
    const [loading, setLoading] = useState(true)

    // Editing states
    const [selectedConfig, setSelectedConfig] = useState<VpnConfiguration | null>(null)
    const [isSaving, setIsSaving] = useState(false)
    const [toastMessage, setToastMessage] = useState("")

    // Editor fields
    const [editUuid, setEditUuid] = useState("")
    const [editLink, setEditLink] = useState("")
    const [editStatus, setEditStatus] = useState<"ACTIVE" | "REVOKED">("ACTIVE")
    const [editServerId, setEditServerId] = useState(1)
    const [editProtocol, setEditProtocol] = useState("VLESS")

    // Intelligent convenient extra feature: Auto VLESS parser
    const [parseFeedback, setParseFeedback] = useState("")

    const loadData = async () => {
        setLoading(true)
        try {
            const [configsData, serversData] = await Promise.all([
                adminApi.getVpnConfigurations(),
                adminApi.getServers()
            ])
            setConfigs(configsData)
            setServers(serversData)
        } catch (err) {
            console.error("Error loading configs data:", err)
        }
        setLoading(false)
    }

    useEffect(() => {
        loadData()
    }, [])

    const triggerToast = (msg: string) => {
        setToastMessage(msg)
        setTimeout(() => setToastMessage(""), 3000)
    }

    const startEditing = (cfg: VpnConfiguration) => {
        setSelectedConfig(cfg)
        setEditUuid(cfg.vlessUuid || "")
        setEditLink(cfg.vlessLink || "")
        setEditStatus(cfg.status || "ACTIVE")
        setEditServerId(cfg.serverId || 1)
        setEditProtocol(cfg.protocol || "VLESS")
        setParseFeedback("")
    }

    const cancelEditing = () => {
        setSelectedConfig(null)
    }

    // Dynamic parsing tool - Convenient Extra Feature
    const handleAutoParseLink = () => {
        if (!editLink.trim()) {
            setParseFeedback("⚠ Ссылка пуста")
            return
        }

        try {
            // vless://uuid@ipAddress:port?type=tcp&security=reality&sni=sniValue&pbk=publicKey&sid=shortId#name
            const linkStr = editLink.trim()
            
            // Extract UUID
            const matchUuid = linkStr.match(/vless:\/\/([^@\s]+)@/)
            // Extract IP Address and Port
            const matchHostPort = linkStr.match(/@([^:/?#\s]+):(\d+)/)
            // Extract query parameters
            const matchSni = linkStr.match(/[?&]sni=([^&#\s]+)/)
            const matchPbk = linkStr.match(/[?&]pbk=([^&#\s]+)/)
            const matchSid = linkStr.match(/[?&]sid=([^&#\s]+)/)

            let changes = []
            if (matchUuid) {
                setEditUuid(matchUuid[1])
                changes.push("UUID")
            }
            if (matchHostPort) {
                changes.push(`IP (${matchHostPort[1]}:${matchHostPort[2]})`)
            }
            if (matchSni) changes.push(`SNI (${matchSni[1]})`)
            if (matchPbk) changes.push("Reality PBK Keys")

            if (changes.length > 0) {
                setParseFeedback(`✅ Успешно извлечено: ${changes.join(", ")}. При сохранении эти параметры обновят сервер.`)
            } else {
                setParseFeedback("⚠ Не удалось выявить Reality-информацию. Проверьте формат URI.")
            }
        } catch (err) {
            setParseFeedback("❌ Ошибка парсинга ссылки")
        }
    }

    const handleSaveConfig = async (e: React.FormEvent) => {
        e.preventDefault()
        if (!selectedConfig) return

        setIsSaving(true)
        try {
            // Match corresponding server if available
            const matchedServer = servers.find(s => s.id === editServerId)

            await adminApi.updateVpnConfiguration(selectedConfig.id, {
                vlessUuid: editUuid,
                vlessLink: editLink,
                status: editStatus,
                serverId: editServerId,
                protocol: editProtocol
            })

            triggerToast(`Успешно обновлено и синхронизировано с сервером ${matchedServer ? matchedServer.name : ""}`)
            setSelectedConfig(null)
            await loadData()
        } catch (err: any) {
            alert(err.message || "Ошибка обновления конфигурации")
        }
        setIsSaving(false)
    }

    // Filtered configs mapping
    const filteredConfigs = configs.filter(cfg => {
        const query = searchQuery.toLowerCase().trim()
        if (!query) return true
        return (
            cfg.id.toString() === query ||
            cfg.userId.toString().includes(query) ||
            cfg.vlessUuid.toLowerCase().includes(query) ||
            cfg.vlessLink.toLowerCase().includes(query) ||
            (cfg.deviceOs && cfg.deviceOs.toLowerCase().includes(query)) ||
            (cfg.device_name && cfg.device_name.toLowerCase().includes(query))
        )
    })

    return (
        <div className="h-[calc(100vh-40px)] w-full overflow-hidden bg-[#F8FAFC] rounded-[3rem] border border-slate-200 shadow-2xl flex flex-col p-8 space-y-6">
            
            {/* TOAST NOTIFICATION */}
            <AnimatePresence>
                {toastMessage && (
                    <motion.div
                        initial={{ opacity: 0, y: -20 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: -20 }}
                        className="fixed top-8 left-1/2 -translate-x-1/2 z-[400] bg-slate-900 border border-slate-800 text-white font-extrabold text-xs px-6 py-3.5 rounded-full uppercase tracking-widest shadow-2xl flex items-center gap-2"
                    >
                        <CheckCircle size={14} className="text-emerald-500" />
                        {toastMessage}
                    </motion.div>
                )}
            </AnimatePresence>

            {/* HEADER */}
            <header className="flex justify-between items-center shrink-0">
                <div>
                    <h2 className="text-4xl font-black tracking-tighter text-slate-900 italic">Конфигурации VPN</h2>
                    <div className="text-[10px] font-black uppercase tracking-[0.3em] text-slate-400 mt-2 flex items-center gap-2 bg-slate-50 w-fit px-3 py-1.5 rounded-full border border-slate-100 font-sans">
                        <div className="h-2 w-2 rounded-full bg-blue-600 animate-pulse" />
                        Таблица: vpn-configurations ({filteredConfigs.length})
                    </div>
                </div>
                <button
                    onClick={loadData}
                    className="h-12 w-12 rounded-xl border border-slate-200 bg-white flex items-center justify-center cursor-pointer hover:border-blue-300 hover:text-blue-600 active:scale-95 transition-all text-slate-600 shadow-lg shadow-slate-100/50"
                    title="Обновить"
                >
                    <RefreshCcw size={18} strokeWidth={2.5} className={loading ? "animate-spin" : ""} />
                </button>
            </header>

            {/* LIVE KPI COUNTERS */}
            <div className="grid grid-cols-4 gap-4 shrink-0">
                <div className="p-4 bg-white border border-slate-100 rounded-2xl">
                    <p className="text-[10px] font-black uppercase text-slate-400 tracking-wider">Всего туннелей</p>
                    <p className="text-2xl font-black font-mono mt-1 text-slate-900">{configs.length}</p>
                </div>
                <div className="p-4 bg-white border border-slate-100 rounded-2xl">
                    <p className="text-[10px] font-black uppercase text-slate-400 tracking-wider">Активно links</p>
                    <p className="text-2xl font-black font-mono mt-1 text-emerald-600">{configs.filter(c => c.status === "ACTIVE").length}</p>
                </div>
                <div className="p-4 bg-white border border-slate-100 rounded-2xl">
                    <p className="text-[10px] font-black uppercase text-slate-400 tracking-wider">Пассивные / Revoked</p>
                    <p className="text-2xl font-black font-mono mt-1 text-red-500">{configs.filter(c => c.status === "REVOKED").length}</p>
                </div>
                <div className="p-4 bg-white border border-slate-100 rounded-2xl bg-gradient-to-br from-slate-900 to-slate-800 text-white">
                    <p className="text-[10px] font-black uppercase text-slate-400 tracking-wider">Связано серверов</p>
                    <p className="text-2xl font-black font-mono mt-1 text-blue-400">{servers.length}</p>
                </div>
            </div>

            {/* FILTER & EDIT AREA CONTAINER */}
            <div className="flex-1 flex gap-6 min-h-0">
                
                {/* CONFIGURATIONS TABLE VIEW */}
                <div className="flex-1 bg-white border border-slate-150 rounded-[2.5rem] p-6 flex flex-col min-h-0">
                    <div className="relative mb-4 shrink-0">
                        <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
                        <input
                            type="text"
                            placeholder="Фильтр по Telegram ID, UUID, устр-ву или ОС..."
                            className="w-full h-11 bg-slate-50 border border-slate-100 rounded-xl pl-12 pr-4 outline-none font-bold text-xs text-slate-800 placeholder:text-slate-400 transition-colors focus:border-blue-400 focus:bg-white"
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                        />
                    </div>

                    <div className="flex-1 overflow-y-auto custom-scrollbar">
                        {loading && configs.length === 0 ? (
                            <div className="py-20 text-center text-xs text-slate-400 animate-pulse">Опрашиваем таблицы vpn_configurations...</div>
                        ) : filteredConfigs.length === 0 ? (
                            <div className="py-20 text-center text-xs text-slate-400 italic">Записи не обнаружены</div>
                        ) : (
                            <table className="w-full text-left border-collapse">
                                <thead>
                                    <tr className="border-b border-slate-105 text-[10px] font-black uppercase tracking-wider text-slate-400">
                                        <th className="py-3 px-2">ID</th>
                                        <th className="py-3 px-2">Пользователь</th>
                                        <th className="py-3 px-2">Сервер / Протокол</th>
                                        <th className="py-3 px-2">VLESS Ключ / Ссылка</th>
                                        <th className="py-3 px-2">Устройство</th>
                                        <th className="py-3 px-2 text-center">Статус</th>
                                        <th className="py-3 px-2 text-center">Действия</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {filteredConfigs.map((cfg) => {
                                        const srv = servers.find(s => s.id === cfg.serverId)
                                        const isSelected = selectedConfig?.id === cfg.id

                                        return (
                                            <tr
                                                key={cfg.id}
                                                className={`border-b border-slate-50 text-xs font-semibold hover:bg-slate-50/70 transition-colors ${isSelected ? "bg-blue-50/40" : ""}`}
                                            >
                                                <td className="py-4 px-2 font-mono text-slate-400">{cfg.id}</td>
                                                <td className="py-4 px-2">
                                                    <span className="font-bold text-slate-800 font-mono">TG: {cfg.userId}</span>
                                                    {cfg.deviceOs && <span className="block text-[8px] font-black text-blue-600 uppercase mt-0.5">{cfg.deviceOs}</span>}
                                                </td>
                                                <td className="py-4 px-2">
                                                    <span className="font-extrabold text-slate-800 flex items-center gap-1">
                                                        🇳🇱 {srv ? srv.name : `Сервер #${cfg.serverId}`}
                                                    </span>
                                                    <span className="text-[9px] font-black tracking-widest bg-slate-100 text-slate-500 px-1.5 py-0.5 rounded mt-1 inline-block uppercase">{cfg.protocol}</span>
                                                </td>
                                                <td className="py-4 px-2 max-w-[220px]">
                                                    <div className="font-mono text-[10px] text-slate-400 truncate" title={cfg.vlessLink}>
                                                        {cfg.vlessLink}
                                                    </div>
                                                    <div className="text-[9px] text-slate-350 truncate mt-0.5">UUID: {cfg.vlessUuid}</div>
                                                </td>
                                                <td className="py-4 px-2 text-slate-500">
                                                    <span className="font-mono">{cfg.device_name || "Unknown device"}</span>
                                                </td>
                                                <td className="py-4 px-2 text-center">
                                                    <span className={`px-2 py-0.5 rounded-full text-[9px] font-black uppercase tracking-wider ${cfg.status === "ACTIVE" ? "bg-emerald-50 text-emerald-600" : "bg-red-50 text-red-500"}`}>
                                                        {cfg.status}
                                                    </span>
                                                </td>
                                                <td className="py-4 px-2 text-center">
                                                    <button
                                                        onClick={() => startEditing(cfg)}
                                                        className="p-1.5 bg-slate-100 hover:bg-blue-105 hover:text-blue-600 rounded-lg text-slate-500 transition-colors cursor-pointer inline-flex items-center"
                                                        title="Редактировать конфигурацию"
                                                    >
                                                        <Edit size={12} />
                                                    </button>
                                                </td>
                                            </tr>
                                        )
                                    })}
                                </tbody>
                            </table>
                        )}
                    </div>
                </div>

                {/* ADVANCED RIGHT EDIT PANEL - THE DYNAMIC WRITER */}
                <AnimatePresence mode="wait">
                    {selectedConfig && (
                        <motion.div
                            initial={{ opacity: 0, x: 50, scale: 0.98 }}
                            animate={{ opacity: 1, x: 0, scale: 1 }}
                            exit={{ opacity: 0, x: 50, scale: 0.98 }}
                            className="w-[380px] bg-white border border-slate-150 rounded-[2.5rem] p-6 flex flex-col justify-between shrink-0 shadow-xl shadow-slate-200/40"
                        >
                            <form onSubmit={handleSaveConfig} className="flex-1 flex flex-col justify-between min-h-0">
                                <div className="space-y-4">
                                    <div className="flex justify-between items-center pb-2 border-b border-slate-100">
                                        <h3 className="font-black text-sm uppercase tracking-tight italic text-slate-800 flex items-center gap-1.5">
                                            <Edit size={14} className="text-blue-600" /> Редактор ссылок
                                        </h3>
                                        <span className="text-[10px] font-mono font-bold bg-blue-50 text-blue-600 px-2.5 py-1 rounded-full">Config #{selectedConfig.id}</span>
                                    </div>

                                    {/* Link Input & Dynamic Parser Trigger */}
                                    <div>
                                        <div className="flex justify-between items-center mb-1">
                                            <label className="text-[9px] font-black text-slate-400 uppercase tracking-widest block">Reality Входящая URI ссылка</label>
                                            <button
                                                type="button"
                                                onClick={handleAutoParseLink}
                                                className="text-[9px] font-black text-blue-600 uppercase tracking-wider bg-blue-50 px-2 py-0.5 rounded cursor-pointer hover:bg-blue-100 transition-colors"
                                            >
                                                🔍 Авто-парс параметров
                                            </button>
                                        </div>
                                        <textarea
                                            className="w-full h-24 p-2.5 bg-slate-50 border border-slate-200 rounded-xl font-mono text-[10px] leading-relaxed outline-none focus:border-blue-400 focus:bg-white resize-none"
                                            value={editLink}
                                            required
                                            onChange={(e) => setEditLink(e.target.value)}
                                            placeholder="vless://e4a39...@host:443?type=tcp..."
                                        />
                                        <p className="text-[9px] text-slate-400 mt-1 pl-1">При изменении параметров IP, SNI, Keys – они автоматически отправятся в таблицу серверов.</p>
                                    </div>

                                    {/* Auto-parse Feedback Message */}
                                    {parseFeedback && (
                                        <div className="p-2.5 bg-slate-50 border border-slate-150 rounded-xl text-[10px] leading-relaxed font-semibold">
                                            {parseFeedback}
                                        </div>
                                    )}

                                    {/* UUID Field */}
                                    <div>
                                        <label className="text-[9px] font-black text-slate-400 uppercase tracking-widest block mb-1">VLESS UUID / Клиентский Токен</label>
                                        <input
                                            type="text"
                                            required
                                            className="w-full h-10 px-3 bg-slate-50 border border-slate-200 rounded-xl font-mono text-[10px] outline-none focus:border-blue-400 focus:bg-white"
                                            value={editUuid}
                                            onChange={(e) => setEditUuid(e.target.value)}
                                        />
                                    </div>

                                    {/* Protocol & Target Server Dropdown */}
                                    <div className="grid grid-cols-2 gap-3">
                                        <div>
                                            <label className="text-[9px] font-black text-slate-400 uppercase tracking-widest block mb-1">Сервер привязки</label>
                                            <select
                                                className="w-full h-10 px-2 bg-slate-50 border border-slate-200 rounded-xl font-bold text-[10px]"
                                                value={editServerId}
                                                onChange={(e) => setEditServerId(Number(e.target.value))}
                                            >
                                                {servers.map((s) => (
                                                    <option key={s.id} value={s.id}>
                                                        {s.name}
                                                    </option>
                                                ))}
                                            </select>
                                        </div>
                                        <div>
                                            <label className="text-[9px] font-black text-slate-400 uppercase tracking-widest block mb-1">Протокол</label>
                                            <select
                                                className="w-full h-10 px-2 bg-slate-50 border border-slate-200 rounded-xl font-bold text-[10px]"
                                                value={editProtocol}
                                                onChange={(e) => setEditProtocol(e.target.value)}
                                            >
                                                <option value="VLESS">VLESS (Reality)</option>
                                                <option value="HY2">HY2 (Hysteria 2)</option>
                                                <option value="Shadowsocks">Shadowsocks</option>
                                            </select>
                                        </div>
                                    </div>

                                    {/* Status selection */}
                                    <div>
                                        <label className="text-[9px] font-black text-slate-400 uppercase tracking-widest block mb-1">Статус подключения</label>
                                        <div className="flex gap-2 p-1 bg-slate-100 rounded-xl">
                                            <button
                                                type="button"
                                                onClick={() => setEditStatus("ACTIVE")}
                                                className={`flex-1 py-1.5 text-[9px] font-black uppercase tracking-wider rounded-lg transition-all cursor-pointer ${editStatus === "ACTIVE" ? "bg-white text-emerald-600 shadow-sm" : "text-slate-500 hover:text-slate-800"}`}
                                            >
                                                ACTIVE
                                            </button>
                                            <button
                                                type="button"
                                                onClick={() => setEditStatus("REVOKED")}
                                                className={`flex-1 py-1.5 text-[9px] font-black uppercase tracking-wider rounded-lg transition-all cursor-pointer ${editStatus === "REVOKED" ? "bg-white text-red-500 shadow-sm" : "text-slate-500 hover:text-slate-800"}`}
                                            >
                                                REVOKED
                                            </button>
                                        </div>
                                    </div>
                                </div>

                                {/* Actions buttons */}
                                <div className="flex gap-3 pt-4 border-t border-slate-100 mt-4">
                                    <button
                                        type="submit"
                                        disabled={isSaving}
                                        className="flex-1 h-11 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-xl text-xs uppercase cursor-pointer"
                                    >
                                        {isSaving ? "Запись..." : "Сохранить и Синхрон"}
                                    </button>
                                    <button
                                        type="button"
                                        onClick={cancelEditing}
                                        className="px-4 h-11 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold rounded-xl text-xs cursor-pointer"
                                    >
                                        Отмена
                                    </button>
                                </div>
                            </form>
                        </motion.div>
                    )}
                </AnimatePresence>
            </div>
        </div>
    )
}
