import React, { useState, useEffect } from "react"
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
    ArrowUpRight,
    ArrowDownLeft,
    Calendar,
    Plus,
    RefreshCw
} from "lucide-react"
import { adminApi } from "@/api/admin"
import { UserResponse, DeviceResponse, DeviceLimitStatus, TransactionResponse } from "@/types/api"
import ConfigEditorModal from "../../components/admin/ConfigEditorModal"

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
    const [transactions, setTransactions] = useState<TransactionResponse[]>([])
    const [loading, setLoading] = useState(false)

    // Вкладки сайдбара
    const [activeTab, setActiveTab] = useState<'billing' | 'devices' | 'security'>('billing')

    const [isEditMode, setIsEditMode] = useState(false)
    const [editFirstName, setEditFirstName] = useState(user.firstName || "")
    const [editUsername, setEditUsername] = useState(user.username || "")
    const [editTgId, setEditTgId] = useState(String(user.telegramId))
    const [editStatus, setEditStatus] = useState(user.status || "ACTIVE")
    const [editPlan, setEditPlan] = useState(user.subscriptionType || "payg")
    const [editError, setEditError] = useState("")

    const [addBalanceAmount, setAddBalanceAmount] = useState("")
    const [balanceOp, setBalanceOp] = useState<"ADD" | "SUB">("ADD")
    const [isAddingBalance, setIsAddingBalance] = useState(false)

    const [newDeviceName, setNewDeviceName] = useState("")
    const [isAddingDevice, setIsAddingDevice] = useState(false)

    const [editingDeviceId, setEditingDeviceId] = useState<number | null>(null)

    const loadSubmoduleData = async () => {
        setLoading(true)
        try {
            const [devs, limStatus, logs, txs] = await Promise.all([
                adminApi.getUserDevices(user.telegramId),
                adminApi.getDeviceLimit(user.telegramId),
                adminApi.getBanLogs(),
                adminApi.getUserTransactions(user.telegramId).catch(() => [])
            ])
            setDevices(devs)
            setLimitStatus(limStatus)
            setBanLogs(logs.filter((l: any) => l.telegramId === user.telegramId))
            setTransactions(txs)
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
        const isCurrentlyBanned = user.isBanned || (user as any).banned || user.status === "BANNED";
        const actionLabel = isCurrentlyBanned ? "РАЗБЛОКИРОВАТЬ" : "ЗАБЛОКИРОВАТЬ";
        const reasonDefault = isCurrentlyBanned
            ? "Амнистия администратором"
            : "Нарушение правил использования сервиса";

        const reason = prompt(
            `Вы уверены, что хотите ${actionLabel} пользователя @${user.username || user.telegramId}?\n\nУкажите причину действия:`,
            reasonDefault
        );

        if (reason !== null) {
            try {
                if (isCurrentlyBanned) {
                    await adminApi.unbanUser(user.telegramId);
                    showToast("Пользователь успешно разблокирован!");
                } else {
                    await adminApi.banUser(user.telegramId, reason);
                    showToast("Пользователь заблокирован!");
                }
                onUpdate();
            } catch (e) {
                alert("Ошибка изменения статуса бана");
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

    const handleRegenConfig = async (telegramId: number, deviceTableId: number) => {
        if (confirm("Вы уверены, что хотите инвалидировать текущий ключ VLESS и сгенерировать новый? Пользователю придется обновить конфиг.")) {
            try {
                await adminApi.regenerateConfig(deviceTableId)
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
            const updatedDevices = devices.map(d => {
                if (d.id === deviceTableId) return { ...d, trafficLimitGb: nextLimit }
                return d
            })
            setDevices(updatedDevices)
            // Имитация сохранения локально
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

    const isBanned = user.isBanned || (user as any).banned || user.status === "BANNED";

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
                            <h3 className="text-xl font-bold tracking-tight italic" style={{ fontFamily: "Space Grotesk, sans-serif" }}>
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
                    <button onClick={onClose} className="w-10 h-10 flex items-center justify-center hover:bg-slate-100 rounded-full transition-colors cursor-pointer text-slate-400">
                        <X size={20} strokeWidth={2.5} />
                    </button>
                </div>
            </div>

            {/* Scrollable Area */}
            <div className="flex-1 overflow-y-auto px-8 py-6 space-y-6 min-h-0 custom-scrollbar">

                {isBanned && (
                    <div className="p-4.5 bg-rose-50 border border-rose-150 rounded-[1.8rem] flex items-start gap-4 text-rose-800 shadow-sm shadow-rose-500/5">
                        <span className="text-xl">⚠️</span>
                        <div>
                            <h5 className="font-extrabold text-xs uppercase tracking-wider mb-0.5 text-rose-700">Доступ заблокирован</h5>
                            <p className="text-[11px] text-rose-600 leading-relaxed font-semibold">
                                Учетная запись заблокирована администратором. Синхронизированные gRPC Reality лимиты сброшены на 0, а статус всех подписок переведен в статус REVOKED.
                            </p>
                        </div>
                    </div>
                )}

                {/* EDIT FORM CONTAINER */}
                <AnimatePresence>
                    {isEditMode && (
                        <motion.div
                            initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: "auto" }} exit={{ opacity: 0, height: 0 }}
                            className="p-6 bg-slate-50 rounded-[1.8rem] border border-slate-150 overflow-hidden text-xs"
                        >
                            {/* ... код формы редактирования без изменений ... */}
                            <div className="flex justify-between items-center mb-4">
                                <span className="font-extrabold text-xs uppercase tracking-widest text-slate-450">Редактирование профиля</span>
                                {editError && <span className="text-red-500 font-bold">{editError}</span>}
                            </div>

                            <div className="grid grid-cols-2 gap-4 mb-4">
                                <div>
                                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block mb-1">Имя в UI</label>
                                    <input type="text" className="w-full h-10 px-3 bg-white border border-slate-200 rounded-lg outline-none font-semibold text-sm" value={editFirstName} onChange={e => setEditFirstName(e.target.value)} />
                                </div>
                                <div>
                                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block mb-1">Юзернейм TG</label>
                                    <input type="text" className="w-full h-10 px-3 bg-white border border-slate-200 rounded-lg outline-none font-semibold text-sm font-mono" value={editUsername} onChange={e => setEditUsername(e.target.value)} />
                                </div>
                                <div>
                                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block mb-1">Telegram ID</label>
                                    <input type="text" className="w-full h-10 px-3 bg-white border border-slate-200 rounded-lg outline-none font-semibold text-sm font-mono" value={editTgId} onChange={e => setEditTgId(e.target.value)} />
                                </div>
                                <div className="grid grid-cols-2 gap-2">
                                    <div>
                                        <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block mb-1">База: Статус</label>
                                        <select className="w-full h-10 px-2 bg-white border border-slate-200 rounded-lg font-bold text-xs" value={editStatus} onChange={e => setEditStatus(e.target.value)}>
                                            <option value="ACTIVE">ACTIVE</option>
                                            <option value="BANNED">BANNED</option>
                                            <option value="INACTIVE">INACTIVE</option>
                                        </select>
                                    </div>
                                    <div>
                                        <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block mb-1">Тариф</label>
                                        <select className="w-full h-10 px-2 bg-white border border-slate-200 rounded-lg font-bold text-xs" value={editPlan} onChange={e => setEditPlan(e.target.value)}>
                                            <option value="payg">payg</option>
                                            <option value="经典极速classic">Classic</option>
                                            <option value="VIP PRO">VIP PRO</option>
                                        </select>
                                    </div>
                                </div>
                            </div>

                            <div className="flex gap-2">
                                <button onClick={handleSaveUserFields} className="flex-1 h-10 bg-blue-600 hover:bg-blue-700 text-white rounded-xl font-bold text-xs uppercase cursor-pointer flex items-center justify-center gap-1">
                                    <Check size={14} /> Сохранить изменения
                                </button>
                                <button onClick={() => setIsEditMode(false)} className="px-4 h-10 bg-slate-200 hover:bg-slate-300 rounded-xl font-bold text-slate-700 cursor-pointer text-xs">
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

                {/* --- СЕГМЕНТИРОВАННЫЙ ТАБ-БАР (ГРУППИРОВКА) --- */}
                <div className="flex bg-slate-100 p-1.5 rounded-[1.5rem] gap-1 select-none">
                    <button
                        onClick={() => setActiveTab('billing')}
                        className={`flex-1 py-3 text-xs font-black uppercase tracking-widest rounded-2xl transition-all flex items-center justify-center gap-2 cursor-pointer ${activeTab === 'billing' ? 'bg-white text-blue-600 shadow-sm' : 'text-slate-500 hover:text-slate-800'}`}
                    >
                        <Coins size={14} /> Финансы
                    </button>
                    <button
                        onClick={() => setActiveTab('devices')}
                        className={`flex-1 py-3 text-xs font-black uppercase tracking-widest rounded-2xl transition-all flex items-center justify-center gap-2 cursor-pointer ${activeTab === 'devices' ? 'bg-white text-blue-600 shadow-sm' : 'text-slate-500 hover:text-slate-800'}`}
                    >
                        <Smartphone size={14} /> Устройства ({devices.length})
                    </button>
                    <button
                        onClick={() => setActiveTab('security')}
                        className={`flex-1 py-3 text-xs font-black uppercase tracking-widest rounded-2xl transition-all flex items-center justify-center gap-2 cursor-pointer ${activeTab === 'security' ? 'bg-white text-red-600 shadow-sm' : 'text-slate-500 hover:text-red-500/80'}`}
                    >
                        <ShieldBan size={14} /> Безопасность
                    </button>
                </div>

                {/* --- АНИМИРОВАННАЯ СМЕНА ВКЛАДОК --- */}
                <div className="min-h-0">
                    <AnimatePresence mode="wait">
                        {activeTab === 'billing' && (
                            <motion.div key="billing" initial={{ opacity: 0, y: 5 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -5 }} className="space-y-6">
                                {/* Баланс */}
                                <div className="p-6 bg-slate-50 border border-slate-100 rounded-[2rem] shadow-sm">
                                    <div className="flex justify-between items-center mb-4">
                                        <div className="flex items-center gap-2 text-slate-800">
                                            <Sparkles className="text-amber-500 animate-pulse fill-amber-500" size={18} />
                                            <h4 className="text-sm font-black uppercase tracking-widest">Баланс кошелька</h4>
                                        </div>
                                        <span className="text-[10px] font-bold tracking-widest bg-blue-100 px-3 py-1 rounded-full text-blue-600 uppercase">Pay-As-You-Go</span>
                                    </div>

                                    <div className="flex bg-slate-200/50 p-1 rounded-xl gap-1 mb-4 select-none">
                                        <button type="button" onClick={() => setBalanceOp("ADD")} className={`flex-1 py-1.5 text-[10px] font-bold uppercase tracking-wider rounded-lg transition-colors cursor-pointer ${balanceOp === "ADD" ? "bg-white text-emerald-600 shadow-sm" : "text-slate-500 hover:text-slate-800"}`}>➕ Пополнение</button>
                                        <button type="button" onClick={() => setBalanceOp("SUB")} className={`flex-1 py-1.5 text-[10px] font-bold uppercase tracking-wider rounded-lg transition-colors cursor-pointer ${balanceOp === "SUB" ? "bg-white text-red-600 shadow-sm" : "text-slate-500 hover:text-slate-800"}`}>➖ Списание</button>
                                    </div>

                                    <form onSubmit={handleAddBalanceSubmit} className="flex gap-2 mb-4">
                                        <input type="number" required value={addBalanceAmount} onChange={e => setAddBalanceAmount(e.target.value)} className={`flex-1 h-12 bg-white outline-none px-4 rounded-xl font-bold font-mono text-sm placeholder:text-slate-350 border ${balanceOp === "SUB" ? "border-red-200 focus:border-red-500 text-red-600" : "border-slate-200 focus:border-blue-500 text-slate-800"} transition-all`} placeholder={balanceOp === "SUB" ? "Сумма списания (RUB)" : "Сумма зачисления (RUB)"} />
                                        <button type="submit" disabled={isAddingBalance} className={`h-12 text-white font-bold rounded-xl px-6 text-xs uppercase tracking-widest cursor-pointer disabled:opacity-50 ${balanceOp === "SUB" ? "bg-red-500 hover:bg-red-600" : "bg-emerald-500 hover:bg-emerald-600"}`}>{isAddingBalance ? "..." : (balanceOp === "SUB" ? "Списать" : "Зачислить")}</button>
                                    </form>

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

                                {/* История транзакций */}
                                <div className="p-6 bg-white border border-slate-100 rounded-[2rem] space-y-4 shadow-sm">
                                    <div className="flex justify-between items-center pb-2 border-b border-slate-100">
                                        <div className="flex items-center gap-2 text-slate-800">
                                            <Calendar className="text-blue-500" size={16} />
                                            <h4 className="text-xs font-black uppercase tracking-widest">История транзакций ({transactions.length})</h4>
                                        </div>
                                        <span className="text-[10px] font-mono text-slate-400 font-bold">billing_ledger</span>
                                    </div>

                                    {transactions.length === 0 ? (
                                        <div className="py-6 text-center text-xs text-slate-400 italic">Транзакции по кошельку отсутствуют</div>
                                    ) : (
                                        <div className="space-y-3 max-h-64 overflow-y-auto pr-1 custom-scrollbar">
                                            {transactions.map((tx) => {
                                                const isDeposit = tx.transactionType === "DEPOSIT" || tx.amount > 0;
                                                const amountRub = Math.abs(tx.amount / 100);
                                                const statusColors: Record<string, string> = {
                                                    COMPLETED: "bg-emerald-50 text-emerald-600 border-emerald-100",
                                                    SUCCESS: "bg-emerald-50 text-emerald-600 border-emerald-100",
                                                    PENDING: "bg-amber-50 text-amber-600 border-amber-100",
                                                    FAILED: "bg-rose-50 text-rose-600 border-rose-100",
                                                };
                                                const badgeClass = statusColors[tx.status] || "bg-slate-50 text-slate-500 border-slate-100";

                                                return (
                                                    <div key={tx.id} className="p-3 bg-slate-50 border border-slate-100 rounded-xl flex items-center justify-between text-xs transition-hover hover:border-slate-200">
                                                        <div className="flex items-center gap-3">
                                                            <div className={`h-8 w-8 rounded-lg flex items-center justify-center border ${isDeposit ? "bg-emerald-50 text-emerald-600 border-emerald-100" : "bg-rose-50 text-rose-600 border-rose-100"}`}>
                                                                {isDeposit ? <ArrowUpRight size={14} /> : <ArrowDownLeft size={14} />}
                                                            </div>
                                                            <div>
                                                                <p className="font-bold text-slate-800">{tx.description || (isDeposit ? "Пополнение" : "Списание")}</p>
                                                                <p className="text-[10px] text-slate-400 font-mono mt-0.5">
                                                                    {new Date(tx.createdAt).toLocaleDateString("ru-RU")} {new Date(tx.createdAt).toLocaleTimeString("ru-RU", { hour: '2-digit', minute: '2-digit' })}
                                                                </p>
                                                            </div>
                                                        </div>
                                                        <div className="text-right flex flex-col items-end gap-1">
                                                            <p className={`font-mono font-black text-sm ${isDeposit ? "text-emerald-600" : "text-rose-600"}`}>{isDeposit ? "+" : "-"}{amountRub} ₽</p>
                                                            <span className={`px-2 py-0.5 rounded text-[9px] font-black uppercase tracking-wider border ${badgeClass}`}>{tx.status}</span>
                                                        </div>
                                                    </div>
                                                )
                                            })}
                                        </div>
                                    )}
                                </div>
                            </motion.div>
                        )}

                        {activeTab === 'devices' && (
                            <motion.div key="devices" initial={{ opacity: 0, y: 5 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -5 }} className="space-y-6">
                                <div className="space-y-4">
                                    {devices.map((device) => {
                                        const currentUsed = device.trafficUsedGb || 0
                                        const limitGb = device.trafficLimitGb || 500
                                        const usagePct = Math.min(100, (currentUsed / limitGb) * 100)

                                        return (
                                            <div key={device.id} className="p-6 bg-white border border-slate-200 rounded-[2rem] shadow-sm space-y-4">
                                                <div className="flex justify-between items-start">
                                                    <div className="flex items-center gap-3">
                                                        <div className="h-10 w-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center font-bold">📱</div>
                                                        <div>
                                                            <div className="font-extrabold text-xs uppercase tracking-tight italic text-slate-800">{device.deviceName || "Устройство"}</div>
                                                            <div className="text-[10px] font-mono text-slate-400 mt-0.5">UUID: {device.uuid.substring(0, 18)}...</div>
                                                        </div>
                                                    </div>
                                                    <div className="flex items-center gap-2">
                                                        <button onClick={() => setEditingDeviceId(device.id)} className="px-3.5 h-8 bg-blue-50 hover:bg-blue-100 text-blue-600 rounded-lg text-[10px] font-black uppercase tracking-wider cursor-pointer">Конфиг</button>
                                                        <button onClick={() => handleRegenConfig(user.telegramId, device.id)} className="px-2 h-8 hover:bg-slate-100 rounded-lg text-xs cursor-pointer text-slate-400">↻</button>
                                                        <button onClick={() => handleDeleteDevice(device.id)} className="h-8 w-8 hover:bg-rose-50 text-rose-500 rounded-lg flex items-center justify-center cursor-pointer"><Trash2 size={13} /></button>
                                                    </div>
                                                </div>
                                                <div className="pt-3 border-t border-dashed border-slate-100">
                                                    <div className="flex justify-between text-[11px] font-mono mb-2">
                                                        <span className="text-slate-400 font-sans font-bold">Трафик (Использовано):</span>
                                                        <span className="text-slate-800 font-extrabold italic">{currentUsed.toFixed(1)} GB / {limitGb} GB</span>
                                                    </div>
                                                    <div className="h-2 w-full bg-slate-100 rounded-full overflow-hidden mb-4">
                                                        <div className="h-full bg-gradient-to-r from-blue-500 to-emerald-400 rounded-full" style={{ width: `${usagePct}%` }} />
                                                    </div>
                                                    <div className="flex justify-between items-center text-[10px] bg-slate-50 border border-slate-100 rounded-xl p-2 font-mono">
                                                        <span className="text-slate-450 font-sans font-extrabold tracking-wider uppercase pl-2">Лимиты пакета</span>
                                                        <div className="flex items-center gap-2">
                                                            <button onClick={() => handleDeviceLimitChange(device.id, -50)} className="h-6 w-11 bg-white hover:bg-slate-100 rounded border border-slate-200 font-black cursor-pointer">-50GB</button>
                                                            <button onClick={() => handleDeviceLimitChange(device.id, 50)} className="h-6 w-11 bg-white hover:bg-slate-100 rounded border border-slate-200 font-black cursor-pointer">+50GB</button>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        )
                                    })}

                                    <div className="p-1 border-2 border-dashed border-slate-150 rounded-[2rem]">
                                        <form onSubmit={handleCreateDeviceSubmit} className="flex gap-2 p-3 bg-white rounded-[1.8rem]">
                                            <input type="text" required value={newDeviceName} onChange={e => setNewDeviceName(e.target.value)} className="flex-1 h-10 px-4 bg-slate-50 border border-slate-100 rounded-xl text-xs font-bold outline-none placeholder:text-slate-400 text-slate-800" placeholder="Название устройства (iPhone Валера)" />
                                            <button type="submit" disabled={isAddingDevice} className="h-10 px-4 bg-slate-900 hover:bg-slate-800 text-white font-extrabold rounded-xl text-xs uppercase cursor-pointer">+ Добавить</button>
                                        </form>
                                    </div>
                                </div>
                            </motion.div>
                        )}

                        {activeTab === 'security' && (
                            <motion.div key="security" initial={{ opacity: 0, y: 5 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -5 }} className="space-y-6">
                                {/* Пульт Блокировки */}
                                <div className="p-6 bg-rose-50/40 border border-rose-100 rounded-[2rem] space-y-4">
                                    <div className="flex items-center gap-2 text-rose-800">
                                        <ShieldBan size={18} className={isBanned ? "" : "animate-pulse"} />
                                        <h4 className="text-xs font-black uppercase tracking-widest">Безопасность и лимиты доступа</h4>
                                    </div>
                                    <p className="text-[11px] text-slate-500 leading-relaxed">Управление авторизацией клиента в VPN-сети. Блокировка моментально отзывает все сессии и сбрасывает лимиты устройств на 0.</p>
                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
                                        {isBanned ? (
                                            <button onClick={handleBanToggle} className="h-12 bg-emerald-500 hover:bg-emerald-600 text-white font-black rounded-xl text-xs uppercase tracking-widest cursor-pointer flex items-center justify-center gap-2 active:scale-95 transition-all"><Check size={16} strokeWidth={2.5} /> Амнистировать</button>
                                        ) : (
                                            <button onClick={handleBanToggle} className="h-12 bg-red-600 hover:bg-red-700 text-white font-black rounded-xl text-xs uppercase tracking-widest cursor-pointer flex items-center justify-center gap-2 active:scale-95 transition-all"><ShieldBan size={16} strokeWidth={2.5} /> Заблокировать</button>
                                        )}
                                        <button onClick={handleDeleteUser} className="h-12 bg-white hover:bg-rose-50 border-2 border-rose-100 text-rose-600 font-bold rounded-xl text-xs uppercase tracking-widest cursor-pointer flex items-center justify-center gap-2 active:scale-95 transition-all"><Trash2 size={16} /> Стереть аккаунт</button>
                                    </div>
                                </div>

                                {/* Логи банов */}
                                <div className="space-y-3">
                                    <div className="flex justify-between items-center px-1">
                                        <h4 className="text-xs font-black uppercase text-slate-500 font-sans">Журнал блокировок</h4>
                                        <span className="text-[10px] font-mono text-slate-400 font-bold">vpn-ban_logs ({banLogs.length})</span>
                                    </div>
                                    {banLogs.length === 0 ? (
                                        <div className="p-5 bg-white border border-slate-100 rounded-2xl text-center text-xs text-slate-400 italic">Записи о блокировках отсутствуют</div>
                                    ) : (
                                        <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                                            {banLogs.map((log: any) => (
                                                <div key={log.id} className="p-3 bg-white border border-slate-150 rounded-xl text-[11px] leading-relaxed">
                                                    <div className="flex justify-between items-center mb-1">
                                                        <span className={`px-2 py-0.5 rounded text-[9px] font-black uppercase tracking-wider ${log.action === "BAN" ? "bg-red-50 text-red-500 border border-red-100" : "bg-emerald-50 text-emerald-600 border border-emerald-100"}`}>{log.action === "BAN" ? "⛔ Блокировка" : "🟢 Амнистия"}</span>
                                                        <span className="text-[10px] font-mono text-slate-400 font-bold">{new Date(log.createdAt).toLocaleDateString("ru-RU")}</span>
                                                    </div>
                                                    <div className="text-slate-700"><span className="text-slate-400 font-bold">Причина:</span> {log.reason}</div>
                                                    <div className="text-[9px] text-slate-400 font-mono mt-1 pt-1 border-t border-dashed border-slate-100 flex justify-between">
                                                        <span>ID: {log.id}</span>
                                                        <span>Админ: @{log.adminUsername}</span>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            </motion.div>
                        )}
                    </AnimatePresence>
                </div>

            </div>

            {editingDeviceId && (
                <ConfigEditorModal
                    deviceId={editingDeviceId}
                    onClose={() => setEditingDeviceId(null)}
                />
            )}
        </div>
    )
}