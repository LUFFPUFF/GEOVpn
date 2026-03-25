// src/pages/UserDetailsPanel.tsx
import { useState, useEffect } from "react"
import { motion, AnimatePresence } from "framer-motion"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import {
    Wallet, ShieldBan, X, ArrowRightLeft, Copy, ChevronDown,
    RefreshCw, Zap, Database, UserCircle, Calendar, Users,
    Smartphone, Laptop, Code2, Globe, HardDrive, MonitorSmartphone,
    Plus, Activity, AlertTriangle, CheckCircle2, ServerCog, Wifi
} from "lucide-react"
import { adminApi } from "@/api/admin"
import { UserResponse, DeviceResponse, VpnConfigResponse, UserStatsResponse, DeviceLimitStatus } from "@/types/api"

function ExpandableDeviceCard({ device, stats, onUpdate }: { device: DeviceResponse, stats: UserStatsResponse | null, onUpdate: () => void }) {
    const [isExpanded, setIsExpanded] = useState(false)
    const [config, setConfig] = useState<VpnConfigResponse | null>(null)
    const [loading, setLoading] = useState(false)

    const getDeviceIcon = (type: string) => {
        const t = type?.toUpperCase()
        if (t === 'IOS' || t === 'ANDROID') return <Smartphone size={24} strokeWidth={2} />
        if (t === 'WINDOWS' || t === 'MAC' || t === 'LINUX') return <Laptop size={24} strokeWidth={2} />
        return <HardDrive size={24} strokeWidth={2} />
    }

    const toggleExpand = async () => {
        if (!isExpanded && !config) {
            setLoading(true)
            try {
                const data = await adminApi.getDeviceConfig(device.id)
                setConfig(data)
            } catch (e) { console.error(e) }
            setLoading(false)
        }
        setIsExpanded(!isExpanded)
    }

    const handleCopy = (text: string | undefined) => {
        if (!text) return
        navigator.clipboard.writeText(text)
    }

    const handleRegen = async () => {
        try {
            const newData = await adminApi.regenerateConfig(device.id)
            setConfig(newData)
        } catch (e) { console.error(e) }
    }

    return (
        <motion.div
            layout
            className={`border-2 transition-all duration-500 rounded-[2rem] overflow-hidden ${isExpanded ? 'bg-white border-blue-500 shadow-2xl shadow-blue-500/10' : 'bg-white border-slate-100 hover:border-blue-200 hover:shadow-xl'}`}
        >
            <div className="p-6 flex justify-between items-center cursor-pointer group" onClick={toggleExpand}>
                <div className="flex items-center gap-5">
                    <div className={`h-16 w-16 rounded-[1.5rem] flex items-center justify-center transition-all duration-500 shadow-inner ${isExpanded ? 'bg-gradient-to-br from-blue-600 to-blue-500 text-white rotate-6 scale-110' : 'bg-slate-50 text-slate-400 group-hover:bg-blue-50 group-hover:text-blue-500'}`}>
                        {getDeviceIcon(device.deviceType)}
                    </div>
                    <div>
                        <p className="font-black text-slate-900 uppercase text-sm tracking-widest">{device.deviceName}</p>
                        <div className="flex items-center gap-2 mt-1">
                            <span className={`flex h-2 w-2 rounded-full ${device.isActive ? 'bg-emerald-500 animate-pulse' : 'bg-slate-300'}`} />
                            <p className="text-[10px] text-slate-400 font-mono tracking-wider">{device.uuid.split('-')[0]}...{device.uuid.split('-')[4]}</p>
                        </div>
                    </div>
                </div>
                <div className="flex items-center gap-4">
                    <ChevronDown className={`text-slate-300 transition-transform duration-500 ${isExpanded ? 'rotate-180 text-blue-500 scale-125' : 'group-hover:text-blue-400'}`} strokeWidth={3} />
                </div>
            </div>

            <AnimatePresence>
                {isExpanded && (
                    <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: "auto", opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        className="px-6 pb-6"
                    >
                        <div className="h-px bg-gradient-to-r from-transparent via-slate-200 to-transparent w-full mb-6" />

                        {loading ? (
                            <div className="py-12 flex flex-col items-center justify-center gap-4">
                                <div className="h-10 w-10 border-4 border-blue-100 border-t-blue-600 rounded-full animate-spin" />
                                <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest animate-pulse">Установка защищенного соединения...</span>
                            </div>
                        ) : (
                            <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
                                <div className="grid grid-cols-2 gap-4">
                                    <div className="p-5 bg-slate-50 rounded-[1.5rem] border border-slate-100 relative overflow-hidden">
                                        <div className="absolute -right-4 -bottom-4 h-20 w-20 bg-blue-500/10 rounded-full blur-xl" />
                                        <p className="text-[9px] font-black text-slate-400 uppercase tracking-widest flex items-center gap-2 mb-2">
                                            <Activity size={12} className="text-blue-500" /> Исходящий
                                        </p>
                                        <p className="text-2xl font-black text-slate-900 font-mono">
                                            {stats?.totalTrafficBytes ? (stats.totalTrafficBytes / 2 / (1024 ** 3)).toFixed(2) : '0.00'}
                                            <span className="text-[10px] text-slate-400 ml-1">GB</span>
                                        </p>
                                    </div>
                                    <div className="p-5 bg-slate-50 rounded-[1.5rem] border border-slate-100 relative overflow-hidden">
                                        <div className="absolute -right-4 -bottom-4 h-20 w-20 bg-emerald-500/10 rounded-full blur-xl" />
                                        <p className="text-[9px] font-black text-slate-400 uppercase tracking-widest flex items-center gap-2 mb-2">
                                            <Globe size={12} className="text-emerald-500" /> Входящий
                                        </p>
                                        <p className="text-2xl font-black text-slate-900 font-mono">
                                            {stats?.totalTrafficBytes ? (stats.totalTrafficBytes / 2 / (1024 ** 3)).toFixed(2) : '0.00'}
                                            <span className="text-[10px] text-slate-400 ml-1">GB</span>
                                        </p>
                                    </div>
                                </div>

                                <div className="space-y-4 bg-slate-900 p-6 rounded-[1.5rem] text-white relative overflow-hidden">
                                    <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-blue-500 via-emerald-400 to-blue-500" />
                                    <div className="flex items-center justify-between mb-4">
                                        <h4 className="text-[10px] font-black text-slate-400 uppercase tracking-[0.2em] flex items-center gap-2">
                                            <ServerCog size={14} className="text-blue-400" /> Маршруты ({config?.configs?.length || 0})
                                        </h4>
                                    </div>
                                    <div className="grid gap-3">
                                        {config?.configs?.map((c, i) => (
                                            <div key={i} className="flex items-center gap-4 p-3 bg-white/5 hover:bg-white/10 rounded-xl transition-colors border border-white/5">
                                                <div className="h-10 w-10 bg-white/10 rounded-lg flex items-center justify-center text-xl backdrop-blur-sm">{c.countryEmoji}</div>
                                                <div className="flex-1 min-w-0">
                                                    <p className="text-xs font-black text-white uppercase tracking-wider truncate">{c.serverName}</p>
                                                    <p className="text-[9px] text-slate-400 font-mono truncate mt-0.5">{c.vlessLink}</p>
                                                </div>
                                                <Button size="icon" variant="ghost" className="rounded-lg text-slate-300 hover:text-white hover:bg-white/20" onClick={() => handleCopy(c.vlessLink)}>
                                                    <Copy size={16} />
                                                </Button>
                                            </div>
                                        ))}
                                    </div>
                                </div>

                                <div className="space-y-3">
                                    <h4 className="text-[10px] font-black text-slate-400 uppercase tracking-[0.2em] ml-2">Конфигурация</h4>
                                    <div className="p-2 bg-blue-50/50 border border-blue-100 rounded-[1.5rem] flex items-center gap-2">
                                        <div className="flex-1 px-4 py-3 bg-white rounded-xl border border-slate-100 font-mono text-[10px] text-slate-500 truncate">
                                            {config?.subscriptionUrl || "Недоступно"}
                                        </div>
                                        <Button className="h-10 rounded-xl px-6 bg-blue-600 hover:bg-blue-700 text-[10px] font-black tracking-widest shadow-lg shadow-blue-200" onClick={() => handleCopy(config?.subscriptionUrl)}>
                                            COPY URL
                                        </Button>
                                    </div>
                                </div>

                                <div className="flex gap-4 pt-2">
                                    <Button onClick={handleRegen} variant="outline" className="flex-1 h-14 rounded-2xl border-slate-200 font-black text-[10px] uppercase tracking-widest hover:bg-slate-50 hover:text-blue-600 transition-all">
                                        <RefreshCw size={14} className="mr-2" /> Сброс ключей
                                    </Button>
                                </div>
                            </motion.div>
                        )}
                    </motion.div>
                )}
            </AnimatePresence>
        </motion.div>
    )
}

export default function UserDetailsPanel({ user, onClose, onUpdate }: { user: UserResponse, onClose: () => void, onUpdate: () => void }) {
    const [activeTab, setActiveTab] = useState("info")
    const [devices, setDevices] = useState<DeviceResponse[]>([])
    const [stats, setStats] = useState<UserStatsResponse | null>(null)
    const [limit, setLimit] = useState<DeviceLimitStatus | null>(null)
    const [loading, setLoading] = useState(false)
    const [topUpAmount, setTopUpAmount] = useState("")

    const [isAddingDevice, setIsAddingDevice] = useState(false)
    const [newDeviceName, setNewDeviceName] = useState("")
    const [newDeviceType, setNewDeviceType] = useState("IOS")
    const [addingLoad, setAddingLoad] = useState(false)

    const load = async () => {
        setLoading(true)
        try {
            if (activeTab === "devices") {
                const data = await adminApi.getUserDevices(user.telegramId)
                setDevices(data)
            }
            const [statData, limitData] = await Promise.all([
                adminApi.getUserStats(user.telegramId),
                adminApi.getDeviceLimit(user.telegramId)
            ])
            setStats(statData)
            setLimit(limitData)
        } catch (e) { console.error(e) }
        setLoading(false)
    }

    useEffect(() => { load() }, [activeTab, user])

    const handleAddBalance = async () => {
        if (!topUpAmount) return
        try {
            await adminApi.addBalance(user.telegramId, Number(topUpAmount) * 100)
            setTopUpAmount("")
            load()
            onUpdate()
        } catch (e) { console.error(e) }
    }

    const handleCreateDevice = async () => {
        if (!newDeviceName) return
        setAddingLoad(true)
        try {
            // Если у тебя нет adminApi.registerDevice, это нужно добавить в api.ts:
            // registerDevice: (telegramId, data) => api.post(`/admin/users/${telegramId}/devices`, data).then(res => res.data.data),
            // Пока имитируем обновление
            setIsAddingDevice(false)
            setNewDeviceName("")
            load()
        } catch (e) { console.error(e) }
        setAddingLoad(false)
    }

    const handleBanUser = async () => {
        // Замени на актуальный эндпоинт бана, если есть
        if (confirm("Вы уверены, что хотите заблокировать этого пользователя?")) {
            console.log("Ban requested");
        }
    }

    const canAddDevice = limit ? limit.activeDevices < limit.maxDevices : false
    const usagePercentage = limit ? (limit.activeDevices / limit.maxDevices) * 100 : 0
    const STATUS_CONFIG: Record<string, { color: string, label: string }> = {
        ACTIVE: { color: "bg-emerald-500", label: "Активен" },
        BANNED: { color: "bg-red-500", label: "Бан" },
        INACTIVE: { color: "bg-slate-300", label: "Спит" }
    }

    return (
        <div className="h-full flex flex-col bg-white shadow-[-30px_0_60px_rgba(0,0,0,0.05)] border-l border-slate-100 relative">
            <div className="absolute top-0 right-0 p-6 flex gap-3 z-50">
                <Button variant="outline" size="icon" className="h-10 w-10 rounded-xl border-slate-200 text-slate-400 hover:text-red-500 hover:border-red-200 hover:bg-red-50 transition-colors" onClick={handleBanUser}>
                    <ShieldBan size={16} />
                </Button>
                <Button variant="ghost" size="icon" onClick={onClose} className="h-10 w-10 rounded-xl bg-slate-50 text-slate-500 hover:bg-slate-200 transition-colors">
                    <X size={18} strokeWidth={2.5} />
                </Button>
            </div>

            <div className="p-10 pb-8 relative overflow-hidden">
                <div className="absolute top-0 right-0 w-64 h-64 bg-gradient-to-bl from-blue-50 to-transparent rounded-bl-full opacity-50 pointer-events-none" />

                <div className="flex items-start gap-8 relative z-10">
                    <div className="relative">
                        <Avatar className="h-28 w-28 rounded-[2rem] ring-4 ring-white shadow-2xl shadow-slate-200/50">
                            <AvatarFallback className="bg-gradient-to-br from-slate-900 to-slate-800 text-white text-4xl font-black italic">
                                {user.firstName ? user.firstName[0].toUpperCase() : '?'}
                            </AvatarFallback>
                        </Avatar>
                        <div className={`absolute -bottom-2 -right-2 h-8 w-8 rounded-xl flex items-center justify-center text-white border-4 border-white shadow-lg ${STATUS_CONFIG[user.status || 'ACTIVE']?.color || 'bg-emerald-500'}`}>
                            <CheckCircle2 size={14} strokeWidth={3} />
                        </div>
                    </div>

                    <div className="flex-1 pt-2">
                        <div className="flex items-center gap-4 mb-3">
                            <h3 className="text-4xl font-black text-slate-900 tracking-tighter italic drop-shadow-sm">{user.firstName || 'Без имени'}</h3>
                            <Badge className="bg-gradient-to-r from-blue-600 to-blue-500 text-white border-none font-black uppercase text-[9px] tracking-[0.2em] px-3 py-1.5 rounded-lg shadow-md shadow-blue-500/20">
                                {limit?.planName || user.subscriptionType}
                            </Badge>
                        </div>

                        <div className="flex flex-wrap gap-3 items-center">
                            <div className="text-blue-600 font-bold text-lg italic mr-2">@{user.username || user.telegramId}</div>
                            <div className="flex items-center gap-2 text-slate-500 font-bold uppercase text-[9px] tracking-[0.2em] bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-200">
                                <Calendar size={12} className="text-blue-500" /> {new Date(user.createdAt).toLocaleDateString('ru-RU')}
                            </div>
                            <div className="flex items-center gap-2 text-slate-500 font-bold uppercase text-[9px] tracking-[0.2em] bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-200">
                                <Code2 size={12} className="text-blue-500" /> {user.telegramId}
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div className="px-10 mb-6">
                <div className="flex p-1.5 bg-slate-100/50 rounded-[1.5rem] gap-2 border border-slate-100 relative">
                    <motion.div
                        className="absolute top-1.5 bottom-1.5 rounded-xl bg-white shadow-md border border-slate-100"
                        layoutId="activeTabIndicator"
                        initial={false}
                        animate={{
                            width: "calc(33.333% - 5.33px)",
                            x: activeTab === "info" ? 0 : activeTab === "devices" ? "100%" : "200%",
                            marginLeft: activeTab === "info" ? 0 : activeTab === "devices" ? "8px" : "16px"
                        }}
                        transition={{ type: "spring", stiffness: 400, damping: 30 }}
                    />
                    {["инфо", "устройства", "логи"].map((label, i) => {
                        const ids = ["info", "devices", "logs"]
                        const isActive = activeTab === ids[i]
                        return (
                            <button
                                key={ids[i]}
                                onClick={() => setActiveTab(ids[i])}
                                className={`flex-1 h-12 relative z-10 text-[10px] font-black tracking-[0.2em] transition-colors duration-300 rounded-xl ${isActive ? "text-blue-600" : "text-slate-400 hover:text-slate-600"}`}
                            >
                                {label.toUpperCase()}
                            </button>
                        )
                    })}
                </div>
            </div>

            <div className="flex-1 overflow-y-auto px-10 pb-10 custom-scrollbar">
                <AnimatePresence mode="wait">
                    {activeTab === "info" && (
                        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -10 }} transition={{ duration: 0.2 }} className="space-y-6">

                            <div className="p-8 bg-gradient-to-br from-slate-900 to-slate-800 rounded-[2.5rem] text-white flex justify-between items-end shadow-2xl shadow-slate-900/20 relative overflow-hidden group">
                                <div className="absolute -right-20 -top-20 h-64 w-64 bg-blue-500 rounded-full blur-[80px] opacity-20 group-hover:opacity-30 transition-opacity duration-700" />
                                <div className="absolute left-0 bottom-0 w-full h-1/2 bg-gradient-to-t from-slate-900 to-transparent opacity-50" />
                                <div className="relative z-10">
                                    <p className="text-[10px] font-black text-slate-400 uppercase tracking-[0.3em] mb-4 flex items-center gap-2">
                                        <Wallet size={12} className="text-blue-400" /> БАЛАНС СЧЕТА
                                    </p>
                                    <div className="flex items-baseline gap-2">
                                        <span className="text-6xl font-black italic tracking-tighter drop-shadow-md">{user.balance / 100}</span>
                                        <span className="text-xl font-black text-blue-400 uppercase">РУБ</span>
                                    </div>
                                </div>
                                <div className="text-right relative z-10">
                                    <div className="bg-emerald-500/20 border border-emerald-500/30 px-4 py-2 rounded-xl backdrop-blur-sm">
                                        <p className="text-[8px] font-black text-emerald-300 uppercase tracking-widest mb-0.5">СТАТУС ПОДПИСКИ</p>
                                        <p className="text-emerald-400 font-black text-xs uppercase tracking-wider">{user.hasActiveSubscription ? "АКТИВНА" : "ИСТЕКЛА"}</p>
                                    </div>
                                </div>
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div className="p-6 bg-white rounded-[2rem] border-2 border-slate-100 shadow-sm relative overflow-hidden group hover:border-orange-200 transition-colors">
                                    <div className="absolute right-0 top-0 h-full w-1 bg-slate-100 group-hover:bg-orange-400 transition-colors" />
                                    <div className="flex justify-between items-start mb-6">
                                        <p className="text-[9px] font-black text-slate-400 uppercase tracking-widest flex items-center gap-2">
                                            <MonitorSmartphone size={12} className="text-orange-500" /> КВОТА УСТРОЙСТВ
                                        </p>
                                        <span className="text-xl font-black italic tracking-tighter text-slate-900">{limit?.activeDevices || 0}<span className="text-sm text-slate-300">/{limit?.maxDevices || 0}</span></span>
                                    </div>
                                    <div className="h-2 w-full bg-slate-100 rounded-full overflow-hidden">
                                        <motion.div
                                            initial={{ width: 0 }}
                                            animate={{ width: `${usagePercentage}%` }}
                                            transition={{ duration: 1, ease: "easeOut" }}
                                            className={`h-full rounded-full ${usagePercentage >= 100 ? 'bg-red-500' : 'bg-gradient-to-r from-orange-400 to-orange-500'}`}
                                        />
                                    </div>
                                    <p className="text-[8px] font-bold text-slate-400 uppercase tracking-wider mt-3 text-right">
                                        {canAddDevice ? "ЕСТЬ СВОБОДНЫЕ СЛОТЫ" : "ЛИМИТ ИСЧЕРПАН"}
                                    </p>
                                </div>
                                <div className="p-6 bg-white rounded-[2rem] border-2 border-slate-100 shadow-sm relative overflow-hidden group hover:border-emerald-200 transition-colors">
                                    <div className="absolute right-0 top-0 h-full w-1 bg-slate-100 group-hover:bg-emerald-400 transition-colors" />
                                    <p className="text-[9px] font-black text-slate-400 uppercase tracking-widest mb-4 flex items-center gap-2">
                                        <Zap size={12} className="text-emerald-500" /> СТАТИСТИКА
                                    </p>
                                    <div className="space-y-3">
                                        <div className="flex justify-between items-center">
                                            <span className="text-[10px] font-bold text-slate-500">Рефералов</span>
                                            <span className="font-mono font-black text-sm">{stats?.totalReferrals || 0}</span>
                                        </div>
                                        <div className="flex justify-between items-center">
                                            <span className="text-[10px] font-bold text-slate-500">Сессий</span>
                                            <span className="font-mono font-black text-sm">{stats?.totalConnections || 0}</span>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div className="p-2 border-2 border-slate-100 rounded-[2rem] bg-slate-50/50 flex gap-2">
                                <Input
                                    type="number"
                                    placeholder="Сумма (₽)"
                                    value={topUpAmount}
                                    onChange={(e) => setTopUpAmount(e.target.value)}
                                    className="h-14 rounded-[1.5rem] border-none shadow-none bg-white font-black text-slate-900 px-6 text-base focus-visible:ring-0"
                                />
                                <Button onClick={handleAddBalance} className="h-14 px-8 bg-slate-900 hover:bg-blue-600 rounded-[1.5rem] font-black uppercase text-[10px] tracking-[0.2em] transition-all">
                                    ЗАЧИСЛИТЬ
                                </Button>
                            </div>
                        </motion.div>
                    )}

                    {activeTab === "devices" && (
                        <motion.div initial={{ opacity: 0, x: 10 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -10 }} transition={{ duration: 0.2 }} className="space-y-4">

                            <div className="flex items-center justify-between bg-slate-50 p-4 rounded-[1.5rem] border border-slate-100 mb-6">
                                <div className="flex items-center gap-4">
                                    <div className="h-10 w-10 rounded-xl bg-white flex items-center justify-center border border-slate-200 shadow-sm">
                                        <Database size={16} className="text-blue-500" />
                                    </div>
                                    <div>
                                        <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Использование квоты</p>
                                        <p className="text-sm font-black text-slate-900">{limit?.activeDevices || 0} из {limit?.maxDevices || 0} доступно</p>
                                    </div>
                                </div>
                                <div className="h-full w-px bg-slate-200 mx-2" />
                                <Button
                                    onClick={() => setIsAddingDevice(true)}
                                    disabled={!canAddDevice || isAddingDevice}
                                    className={`h-10 px-6 rounded-xl font-black uppercase text-[10px] tracking-widest transition-all ${canAddDevice ? 'bg-blue-600 hover:bg-blue-700 shadow-md shadow-blue-200 text-white' : 'bg-slate-200 text-slate-400 cursor-not-allowed'}`}
                                >
                                    <Plus size={14} className="mr-2" /> Добавить
                                </Button>
                            </div>

                            <AnimatePresence>
                                {isAddingDevice && (
                                    <motion.div
                                        initial={{ opacity: 0, height: 0, scale: 0.95 }}
                                        animate={{ opacity: 1, height: "auto", scale: 1 }}
                                        exit={{ opacity: 0, height: 0, scale: 0.95 }}
                                        className="mb-6 overflow-hidden"
                                    >
                                        <div className="p-6 bg-white border-2 border-blue-500 rounded-[2rem] shadow-xl shadow-blue-500/10 space-y-4">
                                            <div className="flex items-center justify-between mb-2">
                                                <h4 className="text-xs font-black text-slate-900 uppercase tracking-widest">Новое устройство</h4>
                                                <Button variant="ghost" size="icon" onClick={() => setIsAddingDevice(false)} className="h-6 w-6 rounded-full hover:bg-slate-100">
                                                    <X size={12} />
                                                </Button>
                                            </div>
                                            <div className="flex gap-2">
                                                {['IOS', 'ANDROID', 'WINDOWS', 'MAC'].map(type => (
                                                    <Button
                                                        key={type}
                                                        variant={newDeviceType === type ? "default" : "outline"}
                                                        onClick={() => setNewDeviceType(type)}
                                                        className={`flex-1 h-12 rounded-xl text-[10px] font-black tracking-wider ${newDeviceType === type ? 'bg-slate-900 text-white' : 'border-slate-200 text-slate-500'}`}
                                                    >
                                                        {type}
                                                    </Button>
                                                ))}
                                            </div>
                                            <div className="flex gap-3 pt-2">
                                                <Input
                                                    placeholder="Название (например: iPhone 13)"
                                                    value={newDeviceName}
                                                    onChange={e => setNewDeviceName(e.target.value)}
                                                    className="h-14 rounded-2xl bg-slate-50 border-slate-200 font-bold"
                                                />
                                                <Button onClick={handleCreateDevice} disabled={addingLoad || !newDeviceName} className="h-14 px-8 rounded-2xl bg-blue-600 text-white font-black text-[10px] uppercase tracking-widest">
                                                    {addingLoad ? <RefreshCw className="animate-spin" size={14} /> : 'Создать'}
                                                </Button>
                                            </div>
                                        </div>
                                    </motion.div>
                                )}
                            </AnimatePresence>

                            {loading && !isAddingDevice ? (
                                <div className="py-20 text-center text-[10px] font-black text-slate-400 animate-pulse tracking-widest uppercase">Загрузка терминалов...</div>
                            ) : devices.length > 0 ? (
                                <div className="grid gap-4">
                                    {devices.map(device => (
                                        <ExpandableDeviceCard key={device.id} device={device} stats={stats} onUpdate={load} />
                                    ))}
                                </div>
                            ) : (
                                <div className="py-24 text-center border-2 border-dashed border-slate-200 rounded-[2.5rem] bg-slate-50">
                                    <HardDrive size={32} className="mx-auto text-slate-300 mb-4" strokeWidth={1.5} />
                                    <p className="text-[10px] font-black uppercase tracking-[0.3em] text-slate-400">Слоты пусты</p>
                                </div>
                            )}
                        </motion.div>
                    )}

                    {activeTab === "logs" && (
                        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="py-20 text-center">
                            <AlertTriangle size={32} className="mx-auto text-amber-400 mb-4" strokeWidth={1.5} />
                            <p className="text-[10px] font-black uppercase tracking-[0.3em] text-slate-400">Система логгирования в разработке</p>
                        </motion.div>
                    )}
                </AnimatePresence>
            </div>
        </div>
    )
}