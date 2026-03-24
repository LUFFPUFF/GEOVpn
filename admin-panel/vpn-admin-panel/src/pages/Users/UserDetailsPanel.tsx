import { useState, useEffect } from "react"
import { motion, AnimatePresence } from "framer-motion"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import {
    Wallet, ShieldBan, X, ArrowRightLeft, Copy, ChevronDown,
    RefreshCw, Zap, Database, UserCircle, Calendar, Users,
    Smartphone, Laptop, Code2, QrCode, Globe, Info, HardDrive, MonitorSmartphone
} from "lucide-react"
import { adminApi } from "@/api/admin"
import { UserResponse, DeviceResponse, VpnConfigResponse, UserStatsResponse, DeviceLimitStatus } from "@/types/api"

function ExpandableDeviceCard({ device, stats }: { device: DeviceResponse, stats: UserStatsResponse | null }) {
    const [isExpanded, setIsExpanded] = useState(false);
    const [config, setConfig] = useState<VpnConfigResponse | null>(null);
    const [loading, setLoading] = useState(false);

    const getDeviceIcon = (type: string) => {
        const t = type?.toUpperCase();
        if (t === 'IOS') return <Smartphone size={22} />;
        if (t === 'ANDROID') return <Smartphone size={22} className="rotate-12" />;
        if (t === 'WINDOWS') return <Laptop size={22} />;
        return <HardDrive size={22} />;
    };

    const toggleExpand = async () => {
        if (!isExpanded && !config) {
            setLoading(true);
            try {
                const data = await adminApi.getDeviceConfig(device.id);
                setConfig(data);
            } catch (e) { console.error(e); }
            setLoading(false);
        }
        setIsExpanded(!isExpanded);
    };

    const handleCopy = (text: string | undefined) => {
        if (!text) return;
        navigator.clipboard.writeText(text);
    };

    const handleRegen = async () => {
        if (!confirm("Сбросить ключи? Это аннулирует текущие подключения.")) return;
        try {
            const newData = await adminApi.regenerateConfig(device.id);
            setConfig(newData);
        } catch (e) { console.error(e); }
    };

    return (
        <motion.div
            layout
            className={`border transition-all duration-500 rounded-[2.5rem] overflow-hidden ${isExpanded ? 'bg-white border-blue-500 shadow-2xl scale-[1.01]' : 'bg-slate-50/50 border-slate-100 hover:border-slate-300'}`}
        >
            <div className="p-7 flex justify-between items-center cursor-pointer" onClick={toggleExpand}>
                <div className="flex items-center gap-5">
                    <div className={`h-14 w-14 rounded-2xl flex items-center justify-center transition-all duration-500 ${isExpanded ? 'bg-blue-600 text-white rotate-6' : 'bg-white shadow-sm text-slate-400'}`}>
                        {getDeviceIcon(device.deviceType)}
                    </div>
                    <div>
                        <p className="font-black text-slate-900 uppercase text-xs tracking-widest">{device.deviceName}</p>
                        <p className="text-[9px] text-slate-400 font-mono mt-1 tracking-tight">{device.uuid}</p>
                    </div>
                </div>
                <div className="flex items-center gap-4">
                    <Badge className="bg-emerald-500 text-white border-none text-[8px] font-black px-2 py-0.5">АКТИВЕН</Badge>
                    <ChevronDown className={`text-slate-300 transition-transform duration-500 ${isExpanded ? 'rotate-180 text-blue-500' : ''}`} />
                </div>
            </div>

            <AnimatePresence>
                {isExpanded && (
                    <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: "auto", opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        className="px-7 pb-8 space-y-8"
                    >
                        <div className="h-px bg-slate-100 w-full" />

                        {loading ? (
                            <div className="py-12 text-center flex flex-col items-center gap-3">
                                <RefreshCw className="animate-spin text-blue-600" size={24} />
                                <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Получение данных с узлов...</span>
                            </div>
                        ) : (
                            <div className="space-y-8 animate-in fade-in duration-500">
                                <div className="grid grid-cols-2 gap-4">
                                    <div className="p-5 bg-slate-50 rounded-3xl border border-slate-100">
                                        <p className="text-[9px] font-black text-slate-400 uppercase tracking-widest flex items-center gap-2 mb-2">
                                            <Zap size={12} className="text-blue-500" /> ТРАФИК
                                        </p>
                                        <p className="text-2xl font-black text-slate-900 font-mono">
                                            {(stats?.totalTrafficBytes ? stats.totalTrafficBytes / (1024 ** 3) : 0).toFixed(2)}
                                            <span className="text-xs text-slate-400 ml-1">GB</span>
                                        </p>
                                    </div>
                                    <div className="p-5 bg-slate-50 rounded-3xl border border-slate-100">
                                        <p className="text-[9px] font-black text-slate-400 uppercase tracking-widest flex items-center gap-2 mb-2">
                                            <Globe size={12} className="text-emerald-500" /> СТАТУС
                                        </p>
                                        <p className="text-xs font-black text-emerald-600 uppercase italic">В СЕТИ</p>
                                    </div>
                                </div>

                                <div className="space-y-4">
                                    <h4 className="text-[10px] font-black text-slate-900 uppercase tracking-[0.2em] ml-2">ПРЯМЫЕ ССЫЛКИ (VLESS)</h4>
                                    <div className="grid gap-3">
                                        {config?.configs.map((c, i) => (
                                            <div key={i} className="flex items-center gap-3 p-3 bg-slate-50 rounded-2xl border border-slate-100 hover:border-blue-200 transition-colors">
                                                <div className="h-10 w-10 bg-white rounded-xl flex items-center justify-center text-lg shadow-sm">{c.countryEmoji}</div>
                                                <div className="flex-1 min-w-0">
                                                    <p className="text-[10px] font-black text-slate-900 uppercase truncate">{c.serverName}</p>
                                                    <p className="text-[9px] text-slate-400 font-mono truncate">{c.vlessLink}</p>
                                                </div>
                                                <Button size="icon" variant="ghost" className="rounded-xl hover:text-blue-600" onClick={() => handleCopy(c.vlessLink)}>
                                                    <Copy size={14} />
                                                </Button>
                                            </div>
                                        ))}
                                    </div>
                                </div>

                                <div className="space-y-4">
                                    <h4 className="text-[10px] font-black text-slate-900 uppercase tracking-[0.2em] ml-2">ПОДПИСКА (URL & BASE64)</h4>
                                    <div className="p-5 bg-blue-50/50 border border-blue-100 rounded-3xl flex flex-col gap-4">
                                        <div className="flex items-center justify-between">
                                            <div className="flex items-center gap-3">
                                                <Database size={16} className="text-blue-600" />
                                                <span className="text-[10px] font-black text-blue-600 uppercase tracking-widest">SUB URL</span>
                                            </div>
                                            <Button size="sm" variant="outline" className="h-8 rounded-xl text-[9px] font-black" onClick={() => handleCopy(config?.subscriptionUrl)}>КОПИРОВАТЬ URL</Button>
                                        </div>
                                        <div className="h-px bg-blue-100" />
                                        <div className="flex items-center justify-between">
                                            <div className="flex items-center gap-3">
                                                <Code2 size={16} className="text-blue-600" />
                                                <span className="text-[10px] font-black text-blue-600 uppercase tracking-widest">BASE64</span>
                                            </div>
                                            <Button size="sm" variant="outline" className="h-8 rounded-xl text-[9px] font-black" onClick={() => handleCopy(config?.subscriptionBase64)}>КОПИРОВАТЬ B64</Button>
                                        </div>
                                        <div className="p-3 bg-white/50 rounded-xl border border-blue-50 font-mono text-[8px] text-slate-400 break-all max-h-20 overflow-y-auto">
                                            {config?.subscriptionBase64 || "Нет данных"}
                                        </div>
                                    </div>
                                </div>

                                <div className="flex gap-3">
                                    <Button onClick={handleRegen} className="flex-1 h-14 rounded-2xl bg-slate-900 text-white font-black text-[10px] uppercase tracking-widest hover:bg-blue-600 transition-all">
                                        <RefreshCw size={14} className="mr-3" /> ПЕРЕВЫПУСТИТЬ КЛЮЧИ
                                    </Button>
                                    <Button variant="outline" className="h-14 w-14 rounded-2xl border-red-100 text-red-500 hover:bg-red-50">
                                        <ShieldBan size={18} />
                                    </Button>
                                </div>
                            </div>
                        )}
                    </motion.div>
                )}
            </AnimatePresence>
        </motion.div>
    );
}

export default function UserDetailsPanel({ user, onClose, onUpdate }: { user: UserResponse, onClose: () => void, onUpdate: () => void }) {
    const [activeTab, setActiveTab] = useState("info")
    const [devices, setDevices] = useState<DeviceResponse[]>([])
    const [stats, setStats] = useState<UserStatsResponse | null>(null)
    const [limit, setLimit] = useState<DeviceLimitStatus | null>(null)
    const [loading, setLoading] = useState(false)
    const [topUpAmount, setTopUpAmount] = useState("")

    useEffect(() => {
        const load = async () => {
            setLoading(true);
            try {
                if (activeTab === "devices") {
                    const data = await adminApi.getUserDevices(user.telegramId);
                    setDevices(data);
                }
                const [statData, limitData] = await Promise.all([
                    adminApi.getUserStats(user.telegramId),
                    adminApi.getDeviceLimit(user.telegramId)
                ]);
                setStats(statData);
                setLimit(limitData);
            } catch (e) { console.error(e); }
            setLoading(false);
        }
        load();
    }, [activeTab, user]);

    const handleAddBalance = async () => {
        if (!topUpAmount) return;
        try {
            // Переводим рубли в копейки для бэкенда
            await adminApi.addBalance(user.telegramId, Number(topUpAmount) * 100);
            setTopUpAmount("");
            const statData = await adminApi.getUserStats(user.telegramId);
            setStats(statData);
            onUpdate();
        } catch (e) { console.error(e); }
    }

    return (
        <div className="h-full flex flex-col bg-white shadow-[-20px_0_50px_rgba(0,0,0,0.05)] border-l border-slate-100">
            {/* HEADER */}
            <div className="p-10 pb-8 relative">
                <Button variant="ghost" size="icon" onClick={onClose} className="absolute top-8 right-8 rounded-full hover:bg-slate-100"><X size={20} /></Button>

                <div className="flex items-start gap-8">
                    <div className="relative">
                        <Avatar className="h-32 w-32 rounded-[2.5rem] ring-8 ring-slate-50 shadow-inner">
                            <AvatarFallback className="bg-slate-900 text-white text-4xl font-black italic">{user.firstName[0]}</AvatarFallback>
                        </Avatar>
                        <div className="absolute -bottom-2 -right-2 h-10 w-10 bg-blue-600 rounded-2xl flex items-center justify-center text-white border-4 border-white shadow-xl">
                            <UserCircle size={20} />
                        </div>
                    </div>

                    <div className="flex-1 pt-2">
                        <div className="flex items-center gap-4 mb-3">
                            <h3 className="text-5xl font-black text-slate-900 tracking-tighter italic">{user.firstName}</h3>
                            <Badge className="bg-blue-600 text-white border-none font-black uppercase text-[10px] tracking-widest px-4 py-1.5 rounded-full">
                                {limit?.planName || user.subscriptionType}
                            </Badge>
                        </div>

                        <div className="flex flex-wrap gap-4 items-center">
                            <div className="text-blue-600 font-bold text-xl italic mr-2">@{user.username || 'user'}</div>
                            <div className="flex items-center gap-2 text-slate-400 font-black uppercase text-[9px] tracking-[0.2em] bg-slate-50 px-3 py-2 rounded-xl border border-slate-100">
                                <Calendar size={12} className="text-blue-500" /> {new Date(user.createdAt).toLocaleDateString('ru-RU')}
                            </div>
                            <div className="flex items-center gap-2 text-slate-400 font-black uppercase text-[9px] tracking-[0.2em] bg-slate-50 px-3 py-2 rounded-xl border border-slate-100">
                                <Code2 size={12} className="text-blue-500" /> ID: {user.telegramId}
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* TABS */}
            <div className="px-10 mb-8">
                <div className="flex p-1.5 bg-slate-100/80 backdrop-blur rounded-[1.8rem] gap-2">
                    {["инфо", "устройства", "логи"].map((label, i) => {
                        const ids = ["info", "devices", "logs"];
                        return (
                            <button
                                key={ids[i]}
                                onClick={() => setActiveTab(ids[i])}
                                className={`flex-1 h-14 rounded-2xl text-[10px] font-black tracking-widest transition-all duration-500 ${activeTab === ids[i] ? "bg-white text-blue-600 shadow-xl scale-[1.02]" : "text-slate-500 hover:text-slate-800"}`}
                            >
                                {label.toUpperCase()}
                            </button>
                        );
                    })}
                </div>
            </div>

            {/* CONTENT */}
            <div className="flex-1 overflow-y-auto px-10 pb-10 custom-scrollbar">
                <AnimatePresence mode="wait">
                    {activeTab === "info" && (
                        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -20 }} className="space-y-10">
                            {/* БАЛАНС */}
                            <div className="p-10 bg-slate-900 rounded-[3rem] text-white flex justify-between items-end shadow-2xl shadow-slate-200 relative overflow-hidden group">
                                <div className="absolute -right-10 -top-10 h-40 w-40 bg-blue-600 rounded-full blur-[80px] opacity-20 group-hover:opacity-40 transition-opacity" />
                                <div className="relative z-10">
                                    <p className="text-[10px] font-black text-slate-500 uppercase tracking-[0.3em] mb-6 flex items-center gap-3">
                                        <Wallet size={14} className="text-blue-500" /> БАЛАНС КОШЕЛЬКА
                                    </p>
                                    <div className="flex items-baseline gap-3">
                                        <span className="text-7xl font-black italic tracking-tighter">{(stats?.balance || 0) / 100}</span>
                                        <span className="text-2xl font-black text-blue-600 uppercase">РУБ</span>
                                    </div>
                                </div>
                                <div className="text-right pb-2 relative z-10">
                                    <p className="text-[10px] font-black text-slate-500 uppercase tracking-widest">СТАТУС</p>
                                    <p className="text-emerald-400 font-bold italic text-sm">Активен</p>
                                </div>
                            </div>

                            {/* СТАТЫ */}
                            <div className="grid grid-cols-3 gap-5">
                                <div className="p-8 bg-white rounded-[2.5rem] border border-slate-100 shadow-sm">
                                    <p className="text-[9px] font-black text-slate-400 uppercase tracking-widest mb-4 flex items-center gap-2">
                                        <Users size={12} className="text-blue-500" /> РЕФЕРАЛЫ
                                    </p>
                                    <p className="text-4xl font-black text-slate-900 italic tracking-tighter">{stats?.totalReferrals || 0}</p>
                                </div>
                                <div className="p-8 bg-white rounded-[2.5rem] border border-slate-100 shadow-sm">
                                    <p className="text-[9px] font-black text-slate-400 uppercase tracking-widest mb-4 flex items-center gap-2">
                                        <MonitorSmartphone size={12} className="text-orange-500" /> УСТРОЙСТВА
                                    </p>
                                    <p className="text-4xl font-black text-slate-900 italic tracking-tighter">
                                        {limit?.activeDevices || 0} <span className="text-lg text-slate-300">/ {limit?.maxDevices || 0}</span>
                                    </p>
                                </div>
                                <div className="p-8 bg-white rounded-[2.5rem] border border-slate-100 shadow-sm">
                                    <p className="text-[9px] font-black text-slate-400 uppercase tracking-widest mb-4 flex items-center gap-2">
                                        <Zap size={12} className="text-emerald-500" /> ТРАФИК (GB)
                                    </p>
                                    <p className="text-4xl font-black text-slate-900 italic tracking-tighter">
                                        {(stats?.totalTrafficBytes ? stats.totalTrafficBytes / (1024 ** 3) : 0).toFixed(1)}
                                    </p>
                                </div>
                            </div>

                            {/* ПОПОЛНЕНИЕ */}
                            <div className="p-10 border-2 border-slate-50 rounded-[3rem] space-y-8 bg-slate-50/30">
                                <h4 className="text-[10px] font-black text-slate-900 uppercase tracking-[0.3em] flex items-center gap-3">
                                    <ArrowRightLeft size={16} className="text-blue-600" /> ОПЕРАЦИИ С БАЛАНСОМ
                                </h4>
                                <div className="flex gap-4">
                                    <Input
                                        placeholder="Сумма пополнения (₽)"
                                        value={topUpAmount}
                                        onChange={(e) => setTopUpAmount(e.target.value)}
                                        className="h-16 rounded-[1.5rem] border-slate-100 font-bold text-slate-900 px-8 text-lg bg-white"
                                    />
                                    <Button onClick={handleAddBalance} className="h-16 px-12 bg-blue-600 hover:bg-blue-700 rounded-[1.5rem] font-black uppercase text-xs tracking-widest shadow-xl shadow-blue-100">
                                        ПОПОЛНИТЬ
                                    </Button>
                                </div>
                            </div>
                        </motion.div>
                    )}

                    {activeTab === "devices" && (
                        <motion.div initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -20 }} className="space-y-6">
                            {loading ? (
                                <div className="py-20 text-center text-[10px] font-black text-slate-400 animate-pulse tracking-widest uppercase">СКАНИРОВАНИЕ ТЕРМИНАЛОВ...</div>
                            ) : devices.length > 0 ? (
                                <div className="grid gap-6">
                                    {devices.map(device => <ExpandableDeviceCard key={device.id} device={device} stats={stats} />)}
                                    <Button className="h-24 bg-blue-600 hover:bg-slate-900 transition-all duration-700 font-black uppercase text-xs tracking-[0.4em] rounded-[3rem] shadow-2xl shadow-blue-100 group">
                                        <QrCode size={24} className="mr-4 group-hover:rotate-90 transition-transform duration-500" /> НОВОЕ УСТРОЙСТВО
                                    </Button>
                                </div>
                            ) : (
                                <div className="py-40 text-center border-4 border-dotted border-slate-100 rounded-[4rem] text-slate-300 italic font-black uppercase text-[10px] tracking-widest">НЕТ ПОДКЛЮЧЕННЫХ УСТРОЙСТВ</div>
                            )}
                        </motion.div>
                    )}
                </AnimatePresence>
            </div>
        </div>
    )
}