import React, { useState, useEffect, useMemo } from "react"
import { motion, AnimatePresence } from "framer-motion"
import {
    X,
    Trash2,
    Edit2,
    Check,
    Coins,
    Sparkles,
    ShieldBan,
    Smartphone,
    Network,
    Copy
} from "lucide-react"
import { adminApi } from "@/api/admin"
import { UserResponse, DeviceResponse, DeviceLimitStatus } from "@/types/api"

export default function UserDetailsPanel({
    user,
    onClose,
    onUpdate
}: {
    user: UserResponse
    onClose: () => void
    onUpdate: () => void
}) {
    const [devices, setDevices] = useState<DeviceResponse[]>([])
    const [limitStatus, setLimitStatus] = useState<DeviceLimitStatus | null>(null)
    const [banLogs, setBanLogs] = useState<any[]>([])
    const [loading, setLoading] = useState(false)

    // Edit User Info Mode
    const [isEditMode, setIsEditMode] = useState(false)
    const [editFirstName, setEditFirstName] = useState(user.firstName || "")
    const [editUsername, setEditUsername] = useState(user.username || "")
    const [editTgId, setEditTgId] = useState(String(user.telegramId))
    const [editStatus, setEditStatus] = useState(user.status || "ACTIVE")
    const [editPlan, setEditPlan] = useState(user.subscriptionType || "payg")
    const [editError, setEditError] = useState("")

    // Balance transaction states
    const [addBalanceAmount, setAddBalanceAmount] = useState("")
    const [balanceOp, setBalanceOp] = useState<"ADD" | "SUB">("ADD")
    const [isAddingBalance, setIsAddingBalance] = useState(false)

    // Device actions states
    const [newDeviceName, setNewDeviceName] = useState("")
    const [newDeviceLimit, setNewDeviceLimit] = useState("500") // 500 MB default
    const [isAddingDevice, setIsAddingDevice] = useState(false)

    // Config popups
    const [selectedConfig, setSelectedConfig] = useState<string | null>(null)
    const [copiedIndex, setCopiedIndex] = useState<number | null>(null)

    const loadSubmoduleData = async () => {
        setLoading(true)
        try {
            const [devs, limStatus, logs] = await Promise.all([
                adminApi.getUserDevices(user.telegramId),
                adminApi.getDeviceLimit(user.telegramId),
                adminApi.getBanLogs()
            ])
            setDevices(devs)
            setLimitStatus(limStatus)
            setBanLogs(logs.filter((l: any) => l.telegramId === user.telegramId))
        } catch (e) {
            console.error("Error loading user submodules: ", e)
        }
        setLoading(false)
    }

    useEffect(() => {
        loadSubmoduleData()
        setIsEditMode(false)
        setEditFirstName(user.firstName || "")
        setEditUsername(user.username || "")
        setEditTgId(String(user.telegramId))
        setEditStatus(user.status || "ACTIVE")
        setEditPlan(user.subscriptionType || "payg")
    }, [user.telegramId])

    const handleSaveUserFields = async () => {
        setEditError("")
        if (!editFirstName.trim()) {
            setEditError("Укажите имя")
            return
        }
        if (!editTgId.trim() || isNaN(Number(editTgId))) {
            setEditError("Укажите числовой Telegram ID")
            return
        }

        try {
            await adminApi.updateUserFields(user.telegramId, {
                firstName: editFirstName.trim(),
                username: editUsername.replace("@", "").trim(),
                telegramId: Number(editTgId),
                status: editStatus as any,
                subscriptionType: editPlan
            })
            setIsEditMode(false)
            onUpdate()
        } catch (e: any) {
            setEditError(e.message || "Ошибка обновления полей")
        }
    }

    const handleBanToggle = async () => {
        const isCurrentlyBanned = user.status === "BANNED" || user.isBlocked;
        const actionLabel = isCurrentlyBanned ? "РАЗБЛОКИРОВАТЬ" : "ЗАБЛОКИРОВАТЬ";
        const reasonDefault = isCurrentlyBanned ? "Амнистия администратором" : "Нарушение правил использования сервиса";
        
        const reason = prompt(`Вы уверены, что хотите ${actionLabel} пользователя @${user.username || user.telegramId}?\n\nУкажите причину действия:`, reasonDefault);
        
        if (reason !== null) {
            try {
                if (isCurrentlyBanned) {
                    await adminApi.unbanUser(user.id, user.telegramId, user.username || "", reason, "admin_geovpn");
                    showToast("Пользователь успешно разблокирован!");
                } else {
                    await adminApi.banUser(user.id, user.telegramId, user.username || "", reason, "admin_geovpn");
                    showToast("Пользователь заблокирован!");
                }
                onUpdate()
            } catch (e) {
                alert("Ошибка изменения статуса бана")
            }
        }
    }

    const handleDeleteUser = async () => {
        if (confirm(`⚠️ ВНИМАНИЕ: Это удалит пользователя @${user.username || user.telegramId} и его устройства НАВСЕГДА. Действие необратимо. Продолжить?`)) {
            try {
                await adminApi.deleteUser(user.telegramId)
                onUpdate()
                onClose()
            } catch (e) {
                alert("Ошибка удаления пользователя")
            }
        }
    }

    const handleAddBalanceSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault()
        const rub = Number(addBalanceAmount)
        if (isNaN(rub) || rub <= 0) return

        setIsAddingBalance(true)
        try {
            const finalAmount = balanceOp === "SUB" ? -rub : rub;
            await adminApi.addBalance(user.telegramId, finalAmount * 100) // Convert to kopecks
            setAddBalanceAmount("")
            onUpdate()
            showToast(balanceOp === "SUB" ? "Баланс успешно убавлен (-)" : "Баланс успешно пополнен (+)")
        } catch (e) {
            alert("Ошибка изменения баланса")
        }
        setIsAddingBalance(false)
    }

    const handleCreateDeviceSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault()
        if (!newDeviceName.trim()) return

        setIsAddingDevice(true)
        try {
            await adminApi.registerDevice(user.telegramId, {
                deviceName: newDeviceName.trim(),
                deviceType: "IOS"
            })
            setNewDeviceName("")
            loadSubmoduleData()
            onUpdate()
        } catch (e) {
            alert("Ошибка создания устройства")
        }
        setIsAddingDevice(false)
    }

    const handleConfigView = async (telegramId: number, deviceTableId: number) => {
        try {
            const configObj = await adminApi.getDeviceConfig(deviceTableId)
            const vlessLink = configObj.configs?.[0]?.vlessLink || configObj.subscriptionUrl
            setSelectedConfig(vlessLink)
        } catch (e) {
            alert("Ошибка получения Reality-конфигурации")
        }
    }

    const handleRegenConfig = async (telegramId: number, deviceTableId: number) => {
        if (confirm("Вы уверены, что хотите инвалидировать текущий ключ VLESS и сгенерировать новый? Пользователю придется обновить конфиг.")) {
            try {
                const configObj = await adminApi.regenerateConfig(deviceTableId)
                const vlessLink = configObj.configs?.[0]?.vlessLink || configObj.subscriptionUrl
                setSelectedConfig(vlessLink)
                showToast("VLESS-ключ успешно обновился!")
            } catch (e) {
                alert("Ошибка регенерации конфигурации")
            }
        }
    }

    const handleDeleteDevice = async (deviceTableId: number) => {
        if (confirm("Удалить это устройство и отозвать Reality подписку?")) {
            try {
                await adminApi.deleteDevice(deviceTableId)
                loadSubmoduleData()
                onUpdate()
            } catch (e) {
                alert("Ошибка удаления устройства")
            }
        }
    }

    const handleDeviceLimitChange = async (deviceTableId: number, deltaGb: number) => {
        const device = devices.find(d => d.id === deviceTableId)
        if (!device) return
        const currentLimit = device.trafficLimitGb || 500
        const nextLimit = Math.max(10, currentLimit + deltaGb)
        try {
            // Apply plan/limit changes via standard Mock DB updating
            const updatedDevices = devices.map(d => {
                if (d.id === deviceTableId) {
                    return { ...d, trafficLimitGb: nextLimit }
                }
                return d
            })
            setDevices(updatedDevices)
            // Save to localStorage table inside adminApi
            const fullDB = (adminApi as any).getLocalDB ? (adminApi as any).getLocalDB() : null
            if (fullDB) {
                const devIdx = fullDB.devices.findIndex((d: any) => d.id === deviceTableId)
                if (devIdx !== -1) {
                    fullDB.devices[devIdx].trafficLimitGb = nextLimit
                    ;(adminApi as any).saveLocalDB(fullDB)
                }
            }
            showToast(`Лимит изменён до ${nextLimit} GB`)
        } catch (e) {
            alert("Ошибка изменения лимита")
        }
    }

    const showToast = (msg: string) => {
        const div = document.createElement("div")
        div.className = "fixed bottom-5 right-5 z-[250] bg-slate-900 border border-slate-800 text-white font-black text-xs px-4 py-2.5 rounded-xl uppercase tracking-widest shadow-2xl animate-fade-in"
        div.innerText = msg
        document.body.appendChild(div)
        setTimeout(() => div.remove(), 2500)
    }

    const handleCopyToClipboard = (text: string, idx: number) => {
        navigator.clipboard.writeText(text)
        setCopiedIndex(idx)
        setTimeout(() => setCopiedIndex(null), 2000)
    }

    return (
        <div className="h-full flex flex-col bg-white overflow-hidden text-slate-900">
            {/* Header */}
            <div className="p-8 border-b border-slate-100 bg-slate-50/50 flex justify-between items-center shrink-0">
                <div className="flex items-center gap-4">
                    <div className="h-14 w-14 rounded-[1.2rem] bg-slate-900 text-white flex items-center justify-center font-black text-xl italic shadow-lg shadow-slate-900/10">
                        {user.firstName ? user.firstName[0].toUpperCase() : "?"}
                    </div>
                    <div>
                        <div className="flex items-center gap-2">
                            <h3 className="text-xl font-bold tracking-tight italic" style={{ fontFamily:"Space Grotesk, sans-serif" }}>
                                {user.firstName || "Без имени"}
                            </h3>
                            <button
                                onClick={() => setIsEditMode(!isEditMode)}
                                className={`p-1.5 rounded-md hover:bg-slate-200 transition-colors ${isEditMode ? "text-blue-600 font-bold" : "text-slate-400"}`}
                            >
                                <Edit2 size={13} />
                            </button>
                        </div>
                        <p className="text-xs text-blue-600 font-semibold font-mono">@{user.username || user.telegramId}</p>
                    </div>
                </div>

                <div className="flex items-center gap-3">
                    <button
                        onClick={handleBanToggle}
                        className={`h-9 w-9 flex items-center justify-center rounded-lg border cursor-pointer transition-colors ${user.status === 'BANNED' ? 'bg-red-50 border-red-200 text-red-500' : 'bg-slate-100 border-slate-200 text-slate-500 hover:bg-red-50 hover:text-red-500'}`}
                        title={user.status === 'BANNED' ? 'Разбанить' : 'Забанить'}
                    >
                        <ShieldBan size={16} />
                    </button>
                    <button onClick={onClose} className="h-9 w-9 flex items-center justify-center rounded-lg hover:bg-slate-150 text-slate-400 cursor-pointer">
                        <X size={18} strokeWidth={2.5} />
                    </button>
                </div>
            </div>

            {/* Main Content */}
            <div className="flex-1 overflow-y-auto px-8 py-8 space-y-8 min-h-0 custom-scrollbar">

                {(user.status === "BANNED" || user.isBlocked) && (
                    <div className="p-4 bg-red-50 border border-red-150 rounded-[1.5rem] flex items-start gap-4 text-red-800">
                        <span className="text-xl">⚠️</span>
                        <div>
                            <h5 className="font-black text-xs uppercase tracking-wider mb-0.5 text-red-700">Доступ заблокирован</h5>
                            <p className="text-[11px] text-red-600 leading-relaxed font-semibold">
                                Учетная запись заблокирована администратором. Синхронизированные gRPC Reality лимиты сброшены на 0, а статус всех подписок переведен в статус REVOKED.
                            </p>
                        </div>
                    </div>
                )}

                {/* EDIT FORM CONTAINER */}
                <AnimatePresence>
                    {isEditMode && (
                        <motion.div
                            initial={{ opacity: 0, height: 0 }}
                            animate={{ opacity: 1, height: "auto" }}
                            exit={{ opacity: 0, height: 0 }}
                            className="p-6 bg-slate-50 rounded-[1.8rem] border border-slate-150 overflow-hidden text-xs"
                        >
                            <div className="flex justify-between items-center mb-4">
                                <span className="font-extrabold text-xs uppercase tracking-widest text-slate-450">Редактирование профиля</span>
                                {editError && <span className="text-red-500 font-bold">{editError}</span>}
                            </div>

                            <div className="grid grid-cols-2 gap-4 mb-4">
                                <div>
                                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block mb-1">Имя в UI</label>
                                    <input
                                        type="text"
                                        className="w-full h-10 px-3 bg-white border border-slate-200 rounded-lg outline-none font-semibold text-sm"
                                        value={editFirstName}
                                        onChange={e => setEditFirstName(e.target.value)}
                                    />
                                </div>
                                <div>
                                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block mb-1">Юзернейм TG</label>
                                    <input
                                        type="text"
                                        className="w-full h-10 px-3 bg-white border border-slate-200 rounded-lg outline-none font-semibold text-sm font-mono"
                                        value={editUsername}
                                        onChange={e => setEditUsername(e.target.value)}
                                    />
                                </div>
                                <div>
                                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block mb-1">Telegram ID</label>
                                    <input
                                        type="text"
                                        className="w-full h-10 px-3 bg-white border border-slate-200 rounded-lg outline-none font-semibold text-sm font-mono"
                                        value={editTgId}
                                        onChange={e => setEditTgId(e.target.value)}
                                    />
                                </div>
                                <div className="grid grid-cols-2 gap-2">
                                    <div>
                                        <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block mb-1">База: Статус</label>
                                        <select
                                            className="w-full h-10 px-2 bg-white border border-slate-200 rounded-lg font-bold text-xs"
                                            value={editStatus}
                                            onChange={e => setEditStatus(e.target.value)}
                                        >
                                            <option value="ACTIVE">ACTIVE</option>
                                            <option value="BANNED">BANNED</option>
                                            <option value="INACTIVE">INACTIVE</option>
                                        </select>
                                    </div>
                                    <div>
                                        <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block mb-1">Тариф</label>
                                        <select
                                            className="w-full h-10 px-2 bg-white border border-slate-200 rounded-lg font-bold text-xs"
                                            value={editPlan}
                                            onChange={e => setEditPlan(e.target.value)}
                                        >
                                            <option value="payg">payg</option>
                                            <option value="经典极速classic">Classic</option>
                                            <option value="VIP PRO">VIP PRO</option>
                                        </select>
                                    </div>
                                </div>
                            </div>

                            <div className="flex gap-2">
                                <button
                                    onClick={handleSaveUserFields}
                                    className="flex-1 h-10 bg-blue-600 hover:bg-blue-700 text-white rounded-xl font-bold text-xs uppercase cursor-pointer flex items-center justify-center gap-1"
                                >
                                    <Check size={14} /> Сохранить изменения
                                </button>
                                <button
                                    onClick={() => setIsEditMode(false)}
                                    className="px-4 h-10 bg-slate-200 hover:bg-slate-300 rounded-xl font-bold text-slate-700 cursor-pointer text-xs"
                                >
                                    Отмена
                                </button>
                            </div>
                        </motion.div>
                    )}
                </AnimatePresence>

                {/* KPI metrics info cards */}
                <div className="grid grid-cols-3 gap-4">
                    <div className="p-4 bg-slate-50 border border-slate-100 rounded-[1.5rem] relative">
                        <Coins className="text-amber-500 absolute top-4 right-4" size={16} />
                        <p className="text-[9px] font-black uppercase tracking-widest text-slate-400 mb-1 font-sans">Счет клиента</p>
                        <p className="text-xl font-black font-mono tracking-tighter text-slate-900">{user.balance / 100} <span className="text-[11px] font-sans text-slate-400 font-bold">₽</span></p>
                    </div>

                    <div className="p-4 bg-slate-50 border border-slate-100 rounded-[1.5rem] relative">
                        <Smartphone className="text-blue-500 absolute top-4 right-4" size={16} />
                        <p className="text-[9px] font-black uppercase tracking-widest text-slate-400 mb-1 font-sans">Устройств</p>
                        <p className="text-xl font-black font-mono tracking-tighter text-slate-900">
                            {devices.length} {limitStatus && <span className="text-[11px] text-slate-400">/{limitStatus.maxDevices}</span>}
                        </p>
                    </div>

                    <div className="p-4 bg-slate-50 border border-slate-100 rounded-[1.5rem] relative">
                        <Network className="text-emerald-500 absolute top-4 right-4" size={16} />
                        <p className="text-[9px] font-black uppercase tracking-widest text-slate-400 mb-1 font-sans">Расход за 24ч</p>
                        <p className="text-xl font-black font-mono tracking-tighter text-slate-900">~{devices.reduce((a,d) => a + (d.trafficUsedGb || 0), 0).toFixed(1)} <span className="text-[11px] font-sans text-slate-400 font-bold">GB</span></p>
                    </div>
                </div>

                {/* --- CHUCK 1: INTUITIVE BALANCE MANAGEMENT --- */}
                <div className="p-6 bg-slate-100/50 dark:bg-slate-900 rounded-[2rem] border border-slate-100">
                    <div className="flex justify-between items-center mb-4">
                        <div className="flex items-center gap-2 text-slate-800">
                            <Sparkles className="text-amber-500 animate-pulse fill-amber-500" size={18} />
                            <h4 className="text-sm font-black uppercase tracking-widest">Баланс и взаиморасчеты</h4>
                        </div>
                        <span className="text-[10px] font-bold tracking-widest bg-blue-105 px-3 py-1 rounded-full text-blue-600 uppercase">Pay-As-You-Go</span>
                    </div>

                    <p className="text-xs text-slate-400 leading-relaxed max-w-[400px] mb-4">
                        Пополнение или списание средств кошелька клиента. Списание ("убавление" баланса) полезно для корректировки ошибок биллинга или взимания штрафов.
                    </p>

                    {/* Operation Mode Selector */}
                    <div className="flex bg-slate-200/50 p-1 rounded-xl gap-1 mb-4 select-none">
                        <button
                            type="button"
                            onClick={() => setBalanceOp("ADD")}
                            className={`flex-1 py-1.5 text-[10px] font-bold uppercase tracking-wider rounded-lg transition-colors cursor-pointer ${balanceOp === "ADD" ? "bg-white text-emerald-600 shadow-sm" : "text-slate-500 hover:text-slate-800"}`}
                        >
                            ➕ Пополнение (+)
                        </button>
                        <button
                            type="button"
                            onClick={() => setBalanceOp("SUB")}
                            className={`flex-1 py-1.5 text-[10px] font-bold uppercase tracking-wider rounded-lg transition-colors cursor-pointer ${balanceOp === "SUB" ? "bg-white text-red-600 shadow-sm" : "text-slate-500 hover:text-slate-800"}`}
                        >
                            ➖ Списание (-)
                        </button>
                    </div>

                    <form onSubmit={handleAddBalanceSubmit} className="flex gap-2 mb-4">
                        <div className="relative flex-1">
                            <input
                                type="number"
                                required
                                value={addBalanceAmount}
                                onChange={e => setAddBalanceAmount(e.target.value)}
                                className={`w-full h-12 bg-white outline-none px-4 rounded-xl font-bold font-mono text-sm placeholder:text-slate-350 border ${balanceOp === "SUB" ? "border-red-200 focus:border-red-500 text-red-600 focus:ring-red-50/50" : "border-slate-200 focus:border-blue-500 text-slate-800 focus:ring-blue-50/50"} focus:ring-4 transition-all`}
                                placeholder={balanceOp === "SUB" ? "Сумма списания (RUB)" : "Сумма зачисления (RUB)"}
                            />
                        </div>
                        <button
                            type="submit"
                            disabled={isAddingBalance}
                            className={`h-12 text-white font-bold rounded-xl px-6 text-xs uppercase tracking-widest cursor-pointer disabled:opacity-50 ${balanceOp === "SUB" ? "bg-red-500 hover:bg-red-600" : "bg-emerald-500 hover:bg-emerald-600"}`}
                        >
                            {isAddingBalance ? "..." : (balanceOp === "SUB" ? "Списать" : "Зачислить")}
                        </button>
                    </form>

                    {/* Quick Preset Buttons */}
                    <div className="flex flex-wrap gap-2">
                        {balanceOp === "SUB" ? (
                            <>
                                <button type="button" onClick={() => setAddBalanceAmount("50")} className="h-7 px-3 bg-white hover:bg-red-50 border border-slate-200 text-red-600 rounded-lg text-[10px] font-bold cursor-pointer transition-colors">-50 ₽</button>
                                <button type="button" onClick={() => setAddBalanceAmount("100")} className="h-7 px-3 bg-white hover:bg-red-50 border border-slate-200 text-red-600 rounded-lg text-[10px] font-bold cursor-pointer transition-colors">-100 ₽</button>
                                <button type="button" onClick={() => setAddBalanceAmount("500")} className="h-7 px-3 bg-white hover:bg-red-50 border border-slate-200 text-red-600 rounded-lg text-[10px] font-bold cursor-pointer transition-colors">-500 ₽</button>
                                <button type="button" onClick={() => setAddBalanceAmount("1000")} className="h-7 px-3 bg-white hover:bg-red-50 border border-slate-200 text-red-600 rounded-lg text-[10px] font-bold cursor-pointer transition-colors">-1000 ₽</button>
                            </>
                        ) : (
                            <>
                                <button type="button" onClick={() => setAddBalanceAmount("100")} className="h-7 px-3 bg-white hover:bg-emerald-50 border border-slate-200 text-emerald-600 rounded-lg text-[10px] font-bold cursor-pointer transition-colors">+100 ₽</button>
                                <button type="button" onClick={() => setAddBalanceAmount("500")} className="h-7 px-3 bg-white hover:bg-emerald-50 border border-slate-200 text-emerald-600 rounded-lg text-[10px] font-bold cursor-pointer transition-colors">+500 ₽</button>
                                <button type="button" onClick={() => setAddBalanceAmount("1000")} className="h-7 px-3 bg-white hover:bg-emerald-50 border border-slate-200 text-emerald-600 rounded-lg text-[10px] font-bold cursor-pointer transition-colors">+1000 ₽</button>
                                <button type="button" onClick={() => setAddBalanceAmount("5000")} className="h-7 px-3 bg-white hover:bg-emerald-50 border border-slate-200 text-emerald-600 rounded-lg text-[10px] font-bold cursor-pointer transition-colors">+5000 ₽</button>
                            </>
                        )}
                    </div>
                </div>

                {/* --- CHUCK 2: DEVICE SLOTS & REALITY LINKS --- */}
                <div>
                    <h4 className="text-xs font-black uppercase tracking-wider text-slate-450 mb-4 font-sans">Устройства и VLESS-конфигурации</h4>

                    {loading ? (
                        <div className="py-10 text-center text-xs text-slate-400 border border-dashed rounded-2xl animate-pulse">Опрашиваем БД / Xray...</div>
                    ) : (
                        <div className="space-y-4">
                            {devices.map((device) => {
                                const currentUsed = device.trafficUsedGb || 0
                                const limitGb = device.trafficLimitGb || 500
                                const usagePct = Math.min(100, (currentUsed / limitGb) * 100)

                                return (
                                    <div key={device.id} className="p-6 bg-slate-50 border border-slate-100 rounded-[2rem] gap-4">
                                        <div className="flex justify-between items-start mb-4">
                                            <div className="flex items-center gap-3">
                                                <div className="h-10 w-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center font-bold">
                                                    📱
                                                </div>
                                                <div>
                                                    <div className="font-extrabold text-xs uppercase tracking-tight italic text-slate-800">
                                                        {device.deviceName || "Устройство"}
                                                    </div>
                                                    <div className="text-[10px] font-mono text-slate-400 mt-0.5">UUID: {device.uuid.substring(0, 18)}...</div>
                                                </div>
                                            </div>

                                            <div className="flex items-center gap-2">
                                                <button
                                                    onClick={() => handleConfigView(user.telegramId, device.id)}
                                                    className="px-3.5 h-8 bg-blue-105 hover:bg-blue-200/50 text-blue-600 rounded-lg text-[10px] font-black uppercase tracking-wider cursor-pointer"
                                                >
                                                    Конфиг
                                                </button>
                                                <button
                                                    onClick={() => handleRegenConfig(user.telegramId, device.id)}
                                                    className="px-2 h-8 hover:bg-slate-200 rounded-lg text-xs cursor-pointer text-slate-400"
                                                    title="Заменить VLESS Ключ"
                                                >
                                                    ↻
                                                </button>
                                                <button
                                                    onClick={() => handleDeleteDevice(device.id)}
                                                    className="h-8 w-8 hover:bg-red-50 text-red-500 rounded-lg flex items-center justify-center cursor-pointer"
                                                >
                                                    <Trash2 size={13} />
                                                </button>
                                            </div>
                                        </div>

                                        <div className="pt-4 border-t border-dashed border-slate-200">
                                            <div className="flex justify-between text-[11px] font-mono mb-2">
                                                <span className="text-slate-400 font-sans font-bold">Трафик (Использовано):</span>
                                                <span className="text-slate-800 font-extrabold italic">
                                                    {currentUsed.toFixed(1)} GB / {limitGb} GB
                                                </span>
                                            </div>

                                            <div className="h-2 w-full bg-slate-200/60 rounded-full overflow-hidden mb-4">
                                                <div
                                                    className="h-full bg-gradient-to-r from-blue-500 to-emerald-400 rounded-full"
                                                    style={{ width: `${usagePct}%` }}
                                                />
                                            </div>

                                            {/* Advanced traffic limits configuration triggers */}
                                            <div className="flex justify-between items-center text-[10px] bg-white border border-slate-100 rounded-xl p-2 font-mono">
                                                <span className="text-slate-450 font-sans font-extrabold tracking-wider uppercase pl-2">Лимиты пакета</span>
                                                <div className="flex items-center gap-2">
                                                    <button onClick={() => handleDeviceLimitChange(device.id, -50)} className="h-6 w-11 bg-slate-100 hover:bg-slate-200 rounded font-black cursor-pointer">-50GB</button>
                                                    <button onClick={() => handleDeviceLimitChange(device.id, 50)} className="h-6 w-11 bg-slate-100 hover:bg-slate-200 rounded font-black cursor-pointer">+50GB</button>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                )
                            })}

                            <div className="p-1 border-2 border-dashed border-slate-150 rounded-[2rem]">
                                <form onSubmit={handleCreateDeviceSubmit} className="flex gap-2 p-3 bg-white rounded-[1.8rem]">
                                    <input
                                        type="text"
                                        required
                                        value={newDeviceName}
                                        onChange={e => setNewDeviceName(e.target.value)}
                                        className="flex-1 h-10 px-4 bg-slate-50 border border-slate-100 rounded-xl text-xs font-bold outline-none placeholder:text-slate-400 text-slate-800"
                                        placeholder="Название устройства (iPhone Валера)"
                                    />
                                    <button
                                        type="submit"
                                        disabled={isAddingDevice}
                                        className="h-10 px-4 bg-slate-900 hover:bg-slate-800 text-white font-extrabold rounded-xl text-xs uppercase cursor-pointer"
                                    >
                                        + Добавить
                                    </button>
                                </form>
                            </div>
                        </div>
                    )}
                </div>

                {/* --- CHUCK 2.5: Chronological Ban History Logs (vpn-ban_logs) --- */}
                <div className="pt-6 border-t border-slate-100">
                    <div className="flex justify-between items-center mb-3">
                        <h4 className="text-xs font-black uppercase text-slate-500 font-sans">Журнал блокировок пользователя</h4>
                        <span className="text-[10px] font-mono text-slate-400 font-semibold">vpn-ban_logs ({banLogs.length})</span>
                    </div>

                    {banLogs.length === 0 ? (
                        <div className="p-4 bg-slate-50 border border-slate-100 rounded-xl text-center text-xs text-slate-400 italic">
                            Записи о блокировках и амнистиях отсутствуют
                        </div>
                    ) : (
                        <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                            {banLogs.map((log: any) => (
                                <div key={log.id} className="p-3 bg-slate-50 border border-slate-100 rounded-xl text-[11px] leading-relaxed transition-hover hover:border-slate-200">
                                    <div className="flex justify-between items-center mb-1.5">
                                        <span className={`px-2 py-0.5 rounded text-[9px] font-black uppercase tracking-wider ${log.action === "BAN" ? "bg-red-50 text-red-500 border border-red-100" : "bg-emerald-50 text-emerald-600 border border-emerald-150"}`}>
                                            {log.action === "BAN" ? "⛔ Блокировка" : "🟢 Амнистия"}
                                        </span>
                                        <span className="text-[10px] font-mono text-slate-400 font-bold">
                                            {new Date(log.createdAt).toLocaleDateString("ru-RU")} {new Date(log.createdAt).toLocaleTimeString("ru-RU", { hour: '2-digit', minute: '2-digit' })}
                                        </span>
                                    </div>
                                    <div className="text-slate-700 font-medium mb-1"><span className="text-slate-400 font-bold">Причина:</span> {log.reason}</div>
                                    <div className="flex justify-between text-[10px] text-slate-400 font-mono mt-1 pt-1 border-t border-dashed border-slate-200/60">
                                        <span>User ID: {log.userId}</span>
                                        <span className="font-sans font-bold text-slate-500">Админ: @{log.adminUsername}</span>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                {/* --- CHUCK 3: CRITICAL ADMIN SYSTEM SETTING --- */}
                <div className="pt-6 border-t border-slate-100">
                    <h4 className="text-xs font-black uppercase text-red-500 mb-2 font-sans">Опасная зона администратора</h4>
                    <p className="text-[11px] text-slate-400 leading-relaxed mb-4">
                        Раздел немедленного стирания и сброса пользователя из БД MySQL и отключения всех активных VLESS gRPC туннелей.
                    </p>
                    <button
                        onClick={handleDeleteUser}
                        className="w-full h-11 bg-red-50 hover:bg-red-100 border border-red-200 text-red-500 font-black rounded-xl text-xs uppercase tracking-widest cursor-pointer transition-all"
                    >
                        Стереть клиента и устройства
                    </button>
                </div>
            </div>

            {/* CONFIG POPUP MODAL */}
            {selectedConfig && (
                <div style={{ position:"fixed", inset:0, zIndex:300, display:"flex", alignItems:"center", justifyContent:"center", padding:"16px", background:"rgba(0,0,0,0.5)", backdropFilter:"blur(4px)" }} onClick={() => setSelectedConfig(null)}>
                    <div style={{ background:"white", padding:"32px", borderRadius:24, maxWidth:"560px", width:"100%", boxShadow:"0 20px 40px rgba(0,0,0,0.15)" }} onClick={e => e.stopPropagation()}>
                        <div className="flex justify-between items-center mb-6">
                            <h4 className="text-base font-black uppercase tracking-tight italic text-slate-900" style={{ fontFamily:"Space Grotesk, sans-serif" }}>VLESS Reality Configuration</h4>
                            <button onClick={() => setSelectedConfig(null)} className="w-8 h-8 rounded-full flex items-center justify-center hover:bg-slate-100 text-slate-400 cursor-pointer">
                                <X size={16} />
                            </button>
                        </div>

                        <p className="text-xs text-slate-500 mb-6 leading-relaxed">
                            Скопируйте Reality-ссылку и импортируйте её в любой VLESS клиент (v2rayNG, Nekobox, Shadowrocket, Streisand, Sing-box).
                        </p>

                        <div className="space-y-4">
                            {[
                                { label:"Конфигурация Link (URI)", val: selectedConfig },
                                { label:"Шаблон config.json (Sing-Box)", val: JSON.stringify({
                                    outbounds: [{
                                        type: "vless",
                                        tag: "vless-out",
                                        server: "193.104.33.100",
                                        server_port: 443,
                                        uuid: user.telegramId.toString(),
                                        flow: "xtls-rprx-vision",
                                        tls: {
                                            enabled: true,
                                            server_name: "eh.vk.com",
                                            utls: { enabled: true, fingerprint: "chrome" },
                                            reality: { enabled: true, public_key: "0aOBytw04b_12fbc941a87e38f6b69fdcb034a", short_id: "2c8c7ec1" }
                                        }
                                    }]
                                }, null, 2) }
                            ].map((cfg, idx) => (
                                <div key={idx}>
                                    <div className="flex justify-between items-center mb-1">
                                        <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">{cfg.label}</span>
                                        <button
                                            onClick={() => handleCopyToClipboard(cfg.val, idx)}
                                            className="text-[10px] font-black uppercase tracking-widest text-blue-600 flex items-center gap-1 cursor-pointer"
                                        >
                                            <Copy size={12} /> {copiedIndex === idx ? "Скопировано!" : "Копировать"}
                                        </button>
                                    </div>
                                    <textarea
                                        readOnly
                                        className="w-full h-32 p-3 bg-slate-50 border border-slate-200 rounded-xl font-mono text-xs text-slate-650 outline-none resize-none"
                                        value={cfg.val}
                                    />
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}
