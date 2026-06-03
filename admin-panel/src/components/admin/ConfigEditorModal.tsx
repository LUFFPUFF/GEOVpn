import React, { useState, useEffect } from "react"
import { motion, AnimatePresence } from "framer-motion"
import { X, Save, Server, Shield, Zap, RefreshCw, Copy, Check, Link2 } from "lucide-react"
import { adminApi } from "@/api/admin"
import { AdminConfigDetailResponse } from "@/types/api"

const parseUrlAdvanced = (url: string) => {
    try {
        const u = new URL(url)
        const params: Record<string, string> = {}
        u.searchParams.forEach((val, key) => {
            params[key] = val
        })
        let hash = u.hash
        try { hash = decodeURIComponent(u.hash) } catch (e) {}

        return {
            protocol: u.protocol,
            username: u.username,
            host: u.hostname,
            port: u.port,
            params,
            hash: hash.replace('#', '')
        }
    } catch {
        return null
    }
}

const buildUrl = (parsed: ReturnType<typeof parseUrlAdvanced>) => {
    if (!parsed) return ""
    try {
        const u = new URL(`${parsed.protocol}//${parsed.username}@${parsed.host}:${parsed.port}`)
        Object.entries(parsed.params).forEach(([k, v]) => {
            if (v !== undefined && v !== null && v !== "") {
                u.searchParams.set(k, v)
            }
        })
        if (parsed.hash) u.hash = parsed.hash
        return u.toString()
    } catch {
        return ""
    }
}

const ConfigBlock = ({
                         item,
                         onChange
                     }: {
    item: any,
    onChange: (newUrl: string) => void
}) => {
    const [parsed, setParsed] = useState(() => parseUrlAdvanced(item.link))
    const [copied, setCopied] = useState(false)

    useEffect(() => {
        setParsed(parseUrlAdvanced(item.link))
    }, [item.link])

    if (!parsed) {
        return <textarea className="w-full bg-red-50 text-red-500 p-4 font-mono text-xs rounded-xl border border-red-200" value={item.link} readOnly />
    }

    const updateField = (field: string, value: string) => {
        const newParsed = { ...parsed, [field]: value }
        setParsed(newParsed)
        onChange(buildUrl(newParsed))
    }

    const updateParam = (key: string, value: string) => {
        const newParams = { ...parsed.params, [key]: value }
        const newParsed = { ...parsed, params: newParams }
        setParsed(newParsed)
        onChange(buildUrl(newParsed))
    }

    const copyToClipboard = () => {
        navigator.clipboard.writeText(buildUrl(parsed))
        setCopied(true)
        setTimeout(() => setCopied(false), 2000)
    }

    // Выделяем важные параметры для показа в первых рядах
    const importantKeys = ['sni', 'pbk', 'sid', 'type', 'security']
    const sortedParamKeys = Object.keys(parsed.params).sort((a, b) => {
        const aImp = importantKeys.indexOf(a)
        const bImp = importantKeys.indexOf(b)
        if (aImp !== -1 && bImp !== -1) return aImp - bImp
        if (aImp !== -1) return -1
        if (bImp !== -1) return 1
        return a.localeCompare(b)
    })

    return (
        <div className="bg-white rounded-[1.5rem] border border-slate-200 shadow-sm overflow-hidden transition-all hover:shadow-md hover:border-blue-200">
            {/* ШАПКА */}
            <div className="bg-slate-50/80 px-5 py-3.5 flex justify-between items-center border-b border-slate-100">
                <div className="flex items-center gap-2.5">
                    <div className="h-6 w-6 rounded-md bg-blue-100 text-blue-600 flex items-center justify-center">
                        <Link2 size={12} strokeWidth={3} />
                    </div>
                    <span className="text-sm font-extrabold text-slate-800 tracking-tight">
                        {item.displayName || item.serverName}
                    </span>
                    <span className="text-[10px] font-black text-slate-400 bg-slate-200/50 px-2 py-0.5 rounded-full ml-1">
                        ID: {item.serverId}
                    </span>
                </div>
                <button onClick={copyToClipboard} className="flex items-center gap-1.5 text-[10px] font-bold text-slate-500 hover:text-blue-600 bg-white border border-slate-200 hover:border-blue-200 px-2.5 py-1.5 rounded-lg transition-colors cursor-pointer">
                    {copied ? <Check size={12} className="text-emerald-500" /> : <Copy size={12} />}
                    {copied ? "СКОПИРОВАНО" : "КОПИРОВАТЬ"}
                </button>
            </div>

            <div className="p-5">
                {/* PREVIEW ССЫЛКИ (Только для чтения, красивая подсветка) */}
                <div className="mb-5 bg-slate-50 border border-slate-100 rounded-xl p-3.5 font-mono text-[11px] leading-relaxed break-all selection:bg-blue-200 text-slate-500">
                    <span className="text-slate-400">{parsed.protocol}//{parsed.username}@</span>
                    <span className="text-blue-600 font-bold">{parsed.host}</span>
                    <span className="text-slate-400">:</span>
                    <span className="text-orange-500 font-bold">{parsed.port}</span>
                    <span className="text-slate-400">?</span>
                    {Object.entries(parsed.params).map(([key, val], idx) => (
                        <React.Fragment key={key}>
                            {idx > 0 && <span className="text-slate-300">&</span>}
                            <span className="text-purple-600">{key}</span>
                            <span className="text-slate-400">=</span>
                            <span className="text-emerald-600 font-medium">{val}</span>
                        </React.Fragment>
                    ))}
                    <span className="text-slate-400">#</span>
                    <span className="text-amber-500 font-bold">{parsed.hash}</span>
                </div>

                {/* ФОРМЫ РЕДАКТИРОВАНИЯ */}
                <div className="space-y-4">
                    {/* Базовые настройки (IP, Порт, Имя) */}
                    <div className="grid grid-cols-12 gap-3">
                        <div className="col-span-6 md:col-span-7">
                            <label className="block text-[9px] font-black uppercase text-slate-400 tracking-wider mb-1.5 ml-1">IP / Домен (Host)</label>
                            <input type="text" value={parsed.host} onChange={e => updateField('host', e.target.value)}
                                   className="w-full bg-white border-2 border-slate-100 focus:border-blue-400 focus:ring-4 focus:ring-blue-50 rounded-xl px-3.5 py-2 text-xs font-mono font-bold text-slate-800 outline-none transition-all" />
                        </div>
                        <div className="col-span-3 md:col-span-2">
                            <label className="block text-[9px] font-black uppercase text-slate-400 tracking-wider mb-1.5 ml-1">Порт</label>
                            <input type="text" value={parsed.port} onChange={e => updateField('port', e.target.value)}
                                   className="w-full bg-white border-2 border-slate-100 focus:border-blue-400 focus:ring-4 focus:ring-blue-50 rounded-xl px-3 py-2 text-xs font-mono font-bold text-slate-800 outline-none transition-all text-center" />
                        </div>
                        <div className="col-span-3 md:col-span-3">
                            <label className="block text-[9px] font-black uppercase text-slate-400 tracking-wider mb-1.5 ml-1">Имя (Hash)</label>
                            <input type="text" value={parsed.hash} onChange={e => updateField('hash', e.target.value)}
                                   className="w-full bg-white border-2 border-slate-100 focus:border-blue-400 focus:ring-4 focus:ring-blue-50 rounded-xl px-3 py-2 text-xs font-semibold text-slate-800 outline-none transition-all" />
                        </div>
                    </div>

                    <hr className="border-slate-100 border-dashed" />

                    {/* Параметры запроса (Query Params) */}
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                        {sortedParamKeys.map((key) => {
                            // Делаем важные поля на всю ширину или половину
                            const isImportant = importantKeys.includes(key)
                            const colSpan = key === 'pbk' ? 'col-span-1 md:col-span-2 lg:col-span-3' : 'col-span-1'

                            return (
                                <div key={key} className={colSpan}>
                                    <label className={`block text-[9px] font-black uppercase tracking-wider mb-1.5 ml-1 flex items-center gap-1 ${isImportant ? 'text-blue-500' : 'text-slate-400'}`}>
                                        {key}
                                        {isImportant && <span className="w-1.5 h-1.5 rounded-full bg-blue-500"></span>}
                                    </label>
                                    <input
                                        type="text"
                                        value={parsed.params[key]}
                                        onChange={e => updateParam(key, e.target.value)}
                                        className="w-full bg-slate-50/50 border-2 border-slate-100 focus:bg-white focus:border-blue-400 focus:ring-4 focus:ring-blue-50 rounded-xl px-3 py-2 text-xs font-mono text-slate-700 outline-none transition-all"
                                    />
                                </div>
                            )
                        })}
                    </div>
                </div>
            </div>
        </div>
    )
}

// --- ОСНОВНОЙ КОМПОНЕНТ МОДАЛКИ ---
export default function ConfigEditorModal({ deviceId, onClose }: { deviceId: number, onClose: () => void }) {
    const [config, setConfig] = useState<AdminConfigDetailResponse | null>(null)
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [activeTab, setActiveTab] = useState<'direct' | 'relay' | 'hy2'>('direct')

    useEffect(() => {
        adminApi.getAdminConfigDetails(deviceId)
            .then(res => {
                setConfig(res)
                if (res.vlessLinks.length === 0 && res.relayLinks.length > 0) setActiveTab('relay')
                setLoading(false)
            })
            .catch(() => setLoading(false))
    }, [deviceId])

    const handleSave = async () => {
        if (!config) return
        setSaving(true)
        try {
            await adminApi.updateDeviceConfig(deviceId, {
                vlessLinks: config.vlessLinks,
                relayLinks: config.relayLinks,
                hy2Links: config.hy2Links
            })
            alert("Конфигурация успешно сохранена и синхронизирована с серверами!")
            onClose()
        } catch (e) {
            alert("Ошибка сохранения конфигурации")
        }
        setSaving(false)
    }

    if (loading) {
        return (
            <div className="fixed inset-0 z-[300] flex items-center justify-center bg-slate-900/40 backdrop-blur-sm">
                <div className="animate-spin text-blue-600"><RefreshCw size={32} /></div>
            </div>
        )
    }

    if (!config) return null

    return (
        <AnimatePresence>
            <motion.div
                initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                className="fixed inset-0 z-[300] flex items-center justify-center p-4 lg:p-10 bg-slate-900/40 backdrop-blur-sm"
                onClick={onClose}
            >
                <motion.div
                    initial={{ scale: 0.98, y: 10 }} animate={{ scale: 1, y: 0 }} exit={{ scale: 0.98, y: 10 }}
                    transition={{ type: "spring", stiffness: 300, damping: 25 }}
                    className="bg-[#F8FAFC] w-full max-w-5xl h-full max-h-[85vh] rounded-[2.5rem] shadow-2xl flex flex-col overflow-hidden border border-slate-200"
                    onClick={e => e.stopPropagation()}
                >

                    {/* --- ШАПКА --- */}
                    <div className="px-8 py-6 bg-white border-b border-slate-200 flex justify-between items-center shrink-0">
                        <div>
                            <h3 className="text-2xl font-black uppercase tracking-tight text-slate-900 flex items-center gap-3 italic">
                                <span className="bg-slate-900 text-white p-2 rounded-[1rem] shadow-md shadow-slate-900/20"><Server size={20} strokeWidth={2.5} /></span>
                                Редактор узлов
                            </h3>
                            <div className="flex items-center gap-3 mt-2">
                                <span className="text-xs font-bold text-blue-700 bg-blue-100 px-2.5 py-1 rounded-md uppercase tracking-widest border border-blue-200">
                                    Device #{deviceId}
                                </span>
                                <p className="text-[11px] text-slate-400 font-mono tracking-wider font-semibold">UUID: {config.vlessUuid}</p>
                            </div>
                        </div>
                        <button onClick={onClose} className="w-10 h-10 flex items-center justify-center hover:bg-slate-100 rounded-full transition-colors cursor-pointer text-slate-400 hover:text-slate-600">
                            <X size={20} strokeWidth={2.5} />
                        </button>
                    </div>

                    {/* --- ВКЛАДКИ (TABS) --- */}
                    <div className="px-8 pt-4 bg-white border-b border-slate-200 flex gap-2 shrink-0">
                        <button onClick={() => setActiveTab('direct')} className={`pb-3 px-2 text-[11px] font-black uppercase tracking-widest flex items-center gap-2 border-b-[3px] transition-colors ${activeTab === 'direct' ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-400 hover:text-slate-600'}`}>
                            <Server size={14} /> Direct Узлы ({config.vlessLinks.length})
                        </button>
                        <button onClick={() => setActiveTab('relay')} className={`pb-3 px-2 text-[11px] font-black uppercase tracking-widest flex items-center gap-2 border-b-[3px] transition-colors ${activeTab === 'relay' ? 'border-amber-500 text-amber-600' : 'border-transparent text-slate-400 hover:text-slate-600'}`}>
                            <Shield size={14} /> Антиглушилки ({config.relayLinks.length})
                        </button>
                        <button onClick={() => setActiveTab('hy2')} className={`pb-3 px-2 text-[11px] font-black uppercase tracking-widest flex items-center gap-2 border-b-[3px] transition-colors ${activeTab === 'hy2' ? 'border-emerald-500 text-emerald-600' : 'border-transparent text-slate-400 hover:text-slate-600'}`}>
                            <Zap size={14} /> Hysteria 2 ({config.hy2Links.length})
                        </button>
                    </div>

                    {/* --- ОБЛАСТЬ КОНТЕНТА --- */}
                    <div className="flex-1 overflow-y-auto p-6 md:p-8 custom-scrollbar">

                        {activeTab === 'direct' && (
                            <div className="space-y-6">
                                {config.vlessLinks.length === 0 && <p className="text-center text-slate-400 text-sm font-bold uppercase tracking-widest py-10">Нет Direct узлов</p>}
                                {config.vlessLinks.map((item, idx) => (
                                    <ConfigBlock
                                        key={idx} item={item}
                                        onChange={(newUrl) => {
                                            const newConfig = { ...config }
                                            newConfig.vlessLinks[idx].link = newUrl
                                            setConfig(newConfig)
                                        }}
                                    />
                                ))}
                            </div>
                        )}

                        {activeTab === 'relay' && (
                            <div className="space-y-6">
                                {config.relayLinks.length === 0 && <p className="text-center text-slate-400 text-sm font-bold uppercase tracking-widest py-10">Нет Relay узлов</p>}
                                {config.relayLinks.map((item, idx) => (
                                    <ConfigBlock
                                        key={idx} item={item}
                                        onChange={(newUrl) => {
                                            const newConfig = { ...config }
                                            newConfig.relayLinks[idx].link = newUrl
                                            setConfig(newConfig)
                                        }}
                                    />
                                ))}
                            </div>
                        )}

                        {activeTab === 'hy2' && (
                            <div className="space-y-4">
                                {config.hy2Links.length === 0 && <p className="text-center text-slate-400 text-sm font-bold uppercase tracking-widest py-10">Нет Hysteria2 ссылок</p>}
                                {config.hy2Links.map((link, idx) => (
                                    <div key={idx} className="bg-white rounded-2xl border border-slate-200 shadow-sm p-4 hover:border-emerald-200 transition-colors">
                                        <textarea
                                            className="w-full bg-slate-50 border-2 border-slate-100 text-slate-700 font-mono text-xs rounded-xl px-4 py-3 outline-none resize-none h-20 focus:border-emerald-400 focus:ring-4 focus:ring-emerald-50 transition-all"
                                            value={link}
                                            onChange={e => {
                                                const newConfig = { ...config }
                                                newConfig.hy2Links[idx] = e.target.value
                                                setConfig(newConfig)
                                            }}
                                        />
                                    </div>
                                ))}
                            </div>
                        )}

                    </div>

                    {/* --- ФУТЕР --- */}
                    <div className="px-8 py-5 bg-white border-t border-slate-200 flex justify-between items-center shrink-0 rounded-b-[2.5rem]">
                        <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest hidden md:block">
                            Изменения применятся к базе данных серверов
                        </p>
                        <button
                            onClick={handleSave}
                            disabled={saving}
                            className="h-12 px-8 bg-blue-600 hover:bg-blue-700 active:scale-95 text-white font-black rounded-[1rem] text-[11px] uppercase tracking-[0.2em] flex items-center gap-2 disabled:opacity-50 transition-all shadow-lg shadow-blue-600/20 cursor-pointer"
                        >
                            {saving ? <RefreshCw size={16} className="animate-spin"/> : <Save size={18} />}
                            {saving ? "СИНХРОНИЗАЦИЯ..." : "СОХРАНИТЬ КОНФИГУРАЦИЮ"}
                        </button>
                    </div>
                </motion.div>
            </motion.div>
        </AnimatePresence>
    )
}