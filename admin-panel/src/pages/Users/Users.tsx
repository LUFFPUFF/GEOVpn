import React, { useState, useEffect, useMemo } from "react"
import { motion, AnimatePresence } from "framer-motion"
import {
    Search,
    Users as UsersIcon,
    TrendingUp,
    RefreshCcw,
    Wallet,
    UserPlus,
    X,
    Check
} from "lucide-react"
import { Input } from "@/components/ui/input"
import { ResizableHandle, ResizablePanel, ResizablePanelGroup } from "@/components/ui/resizable"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { adminApi } from "@/api/admin"
import { UserResponse, AdminDashboardResponse } from "@/types/api"
import UserDetailsPanel from "./UserDetailsPanel"

export const STATUS_CONFIG: Record<string, { color: string, label: string }> = {
    ACTIVE: { color: "bg-emerald-500", label: "Активен" },
    BANNED: { color: "bg-red-500", label: "Бан" },
    INACTIVE: { color: "bg-slate-300", label: "Спит" }
}

export default function Users() {
    const [users, setUsers] = useState<UserResponse[]>([])
    const [stats, setStats] = useState<AdminDashboardResponse | null>(null)
    const [selectedUser, setSelectedUser] = useState<UserResponse | null>(null)
    const [search, setSearch] = useState("")
    const [loading, setLoading] = useState(true)

    // Create User Modal state
    const [showCreateModal, setShowCreateModal] = useState(false)
    const [newTgId, setNewTgId] = useState("")
    const [newUsername, setNewUsername] = useState("")
    const [newFirstName, setNewFirstName] = useState("")
    const [newSubPlan, setNewSubPlan] = useState("payg")
    const [newBalance, setNewBalance] = useState("")
    const [createError, setCreateError] = useState("")

    const loadData = async (searchQuery: string) => {
        setLoading(true)
        try {
            const [usersData, statsData] = await Promise.all([
                adminApi.getUsers(0, 100, searchQuery),
                adminApi.getDashboard()
            ])

            setUsers(Array.isArray(usersData) ? usersData : [])
            setStats(statsData)

            if (selectedUser) {
                const refreshed = Array.isArray(usersData)
                    ? usersData.find(u => u.telegramId === selectedUser.telegramId)
                    : null
                if (refreshed) {
                    setSelectedUser(refreshed)
                }
            }
        } catch (e) {
            console.error(e)
            setUsers([])
        }
        setLoading(false)
    }

    useEffect(() => {
        loadData(search)
    }, [search])

    const handleCreateUserSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        setCreateError("")
        if (!newFirstName.trim()) {
            setCreateError("Укажите имя клиента")
            return
        }
        if (!newTgId.trim() || isNaN(Number(newTgId))) {
            setCreateError("Укажите числовой Telegram ID")
            return
        }

        try {
            await adminApi.createUser({
                telegramId: Number(newTgId),
                username: newUsername.replace("@", "").trim(),
                firstName: newFirstName.trim(),
                subscriptionType: newSubPlan,
                balance: Number(newBalance) * 100 || 0, // Convert to kopecks
                status: "ACTIVE"
            })
            setShowCreateModal(false)
            setNewTgId("")
            setNewUsername("")
            setNewFirstName("")
            setNewSubPlan("payg")
            setNewBalance("")
            loadData(search)
        } catch (err: any) {
            setCreateError(err.message || "Ошибка создания пользователя")
        }
    }

    const listContainer = {
        hidden: { opacity: 0 },
        show: {
            opacity: 1,
            transition: { staggerChildren: 0.05 }
        }
    }

    const listItem = {
        hidden: { opacity: 0, y: 20, scale: 0.95 },
        show: { opacity: 1, y: 0, scale: 1, transition: { type: "spring", stiffness: 300, damping: 24 } }
    }

    return (
        <div className="h-[calc(100vh-40px)] w-full overflow-hidden bg-[#F8FAFC] rounded-[3rem] border border-slate-200 shadow-2xl flex">
            <ResizablePanelGroup direction="horizontal">
                <ResizablePanel defaultSize={42} minSize={35} className="bg-white relative flex flex-col">
                    <div className="absolute top-0 left-0 w-full h-32 bg-gradient-to-b from-slate-50 to-transparent z-0 pointer-events-none" />

                    <div className="p-8 pb-4 relative z-10">
                        <header className="flex justify-between items-end mb-6">
                            <div>
                                <h2 className="text-4xl font-black tracking-tighter text-slate-900 italic drop-shadow-sm">Клиенты</h2>
                                <div className="text-[10px] font-black uppercase tracking-[0.3em] text-slate-400 mt-2 flex items-center gap-2 bg-slate-50 w-fit px-3 py-1.5 rounded-full border border-slate-100 font-sans">
                                    <div className="h-2 w-2 rounded-full bg-blue-600 animate-pulse shadow-[0_0_10px_rgba(37,99,235,0.5)]" />
                                    Всего: {stats?.totalUsers || 2585}
                                </div>
                            </div>
                            <div className="flex gap-2">
                                <Button
                                    variant="outline"
                                    onClick={() => setShowCreateModal(true)}
                                    className="h-12 border-slate-200 px-4 rounded-xl gap-2 font-bold text-xs bg-slate-900 hover:bg-slate-800 text-white cursor-pointer shadow-lg shadow-slate-900/10 active:scale-95 transition-all"
                                >
                                    <UserPlus size={16} /> Новый клиент
                                </Button>
                                <Button
                                    variant="outline"
                                    size="icon"
                                    onClick={() => loadData(search)}
                                    className={`h-12 w-12 rounded-xl border-slate-200 bg-white cursor-pointer shadow-lg shadow-slate-200/20 transition-all hover:border-blue-200 hover:text-blue-600 active:scale-95 ${loading ? "animate-spin text-blue-600 border-blue-200" : "text-slate-600"}`}
                                >
                                    <RefreshCcw size={18} strokeWidth={2.5} />
                                </Button>
                            </div>
                        </header>

                        <div className="grid grid-cols-2 gap-4 mb-6">
                            <motion.div whileHover={{ y: -2 }} className="p-4 bg-gradient-to-br from-slate-900 to-slate-800 rounded-[1.5rem] text-white shadow-xl shadow-slate-900/20 relative overflow-hidden">
                                <div className="absolute -right-4 -top-4 h-16 w-16 bg-emerald-500 rounded-full blur-[30px] opacity-20" />
                                <p className="text-[9px] font-black uppercase tracking-widest text-slate-400 mb-2 flex items-center gap-1.5 relative z-10 font-sans">
                                    <TrendingUp size={11} className="text-emerald-400" /> Подписки
                                </p>
                                <p className="text-2xl font-black italic tracking-tighter relative z-10 leading-none">{stats?.activeSubscriptions || 0}</p>
                            </motion.div>
                            <motion.div whileHover={{ y: -2 }} className="p-4 bg-white border-2 border-slate-100 rounded-[1.5rem] shadow-xl shadow-slate-200/20 relative overflow-hidden">
                                <div className="absolute -right-4 -top-4 h-16 w-16 bg-blue-500 rounded-full blur-[30px] opacity-10" />
                                <p className="text-[9px] font-black uppercase tracking-widest text-slate-400 mb-2 flex items-center gap-1.5 relative z-10 font-sans">
                                    <Wallet size={11} className="text-blue-600" /> Оборот
                                </p>
                                <p className="text-2xl font-black italic tracking-tighter text-slate-900 relative z-10 leading-none">{(stats?.totalBalanceRub || 0)} <span className="text-xs text-slate-300">₽</span></p>
                            </motion.div>
                        </div>

                        <div className="relative mb-2 group">
                            <Search className="absolute left-6 top-1/2 -translate-y-1/2 h-5 w-5 text-slate-300 group-focus-within:text-blue-600 transition-colors" strokeWidth={2.5} />
                            <Input
                                placeholder="Поиск по ID, нику или имени..."
                                className="pl-16 h-14 bg-slate-50 border-2 border-slate-100 rounded-2xl font-bold text-slate-950 placeholder:text-slate-300 shadow-inner transition-all focus-visible:ring-4 focus-visible:ring-blue-50 focus-visible:border-blue-300 text-sm"
                                value={search}
                                onChange={(e) => setSearch(e.target.value)}
                            />
                        </div>
                    </div>

                    <div className="flex-1 overflow-y-auto px-8 pb-8 custom-scrollbar">
                        {loading && users.length === 0 ? (
                            <div className="h-full flex items-center justify-center">
                                <div className="animate-pulse flex flex-col items-center gap-3">
                                    <div className="h-10 w-10 border-4 border-blue-650 border-t-transparent rounded-full animate-spin" />
                                    <p className="text-[9px] font-black tracking-widest text-slate-400 uppercase font-sans">Синхронизация...</p>
                                </div>
                            </div>
                        ) : (
                            <motion.div variants={listContainer} initial="hidden" animate="show" className="space-y-3 pt-2">
                                {Array.isArray(users) && users.map((user) => {
                                    const isSelected = selectedUser?.telegramId === user.telegramId
                                    const statusColor = STATUS_CONFIG[user.status || 'ACTIVE']?.color || "bg-emerald-500"

                                    return (
                                        <motion.div
                                            variants={listItem}
                                            key={user.id}
                                            onClick={() => setSelectedUser(user)}
                                            className={`group p-4.5 rounded-[1.8rem] cursor-pointer transition-all duration-300 relative overflow-hidden border-2 ${
                                                isSelected
                                                    ? "bg-slate-900 text-white border-slate-900 shadow-2xl shadow-slate-900/30 scale-[1.01] z-10"
                                                    : "bg-white border-slate-100 hover:border-blue-200 hover:shadow-xl hover:shadow-blue-500/10 text-slate-900"
                                            }`}
                                        >
                                            <div className="flex items-center justify-between relative z-10">
                                                <div className="flex items-center gap-4">
                                                    <div className="relative">
                                                        <Avatar className={`h-12 w-12 rounded-[1rem] transition-all duration-550 shadow-sm ${isSelected ? "rotate-6 scale-110 ring-4 ring-white/10" : "group-hover:rotate-6"}`}>
                                                            <AvatarFallback className={`${(user.status === "BANNED" || user.isBlocked) ? "bg-red-500 text-white" : (isSelected ? "bg-blue-500 text-white" : "bg-slate-100 text-slate-600")} font-black text-base`}>
                                                                {user.firstName ? user.firstName[0].toUpperCase() : '?'}
                                                            </AvatarFallback>
                                                        </Avatar>
                                                        <div className={`absolute -bottom-1 -right-1 h-3.5 w-3.5 rounded-full border-2 ${isSelected ? "border-slate-900" : "border-white"} ${statusColor} transition-colors`} />
                                                    </div>
                                                    <div>
                                                        <p className={`font-extrabold text-sm uppercase tracking-tight italic truncate max-w-[150px] flex items-center gap-1.5 ${user.status === 'BANNED' || user.isBlocked ? 'line-through text-red-500/80 opacity-75' : ''}`}>
                                                            {user.firstName || 'Без имени'}
                                                            {(user.status === 'BANNED' || user.isBlocked) && <span className="text-xs no-underline font-normal inline-block" title="Пользователь заблокирован">🚫</span>}
                                                        </p>
                                                        <div className="flex items-center gap-2 mt-0.5">
                                                            <span className={`text-[10px] font-bold tracking-wider ${isSelected ? "text-slate-300" : ((user.status === 'BANNED' || user.isBlocked) ? "text-red-400 font-semibold" : "text-blue-650")}`}>
                                                                @{user.username || user.telegramId}
                                                            </span>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div className="text-right">
                                                    <p className={`font-mono font-extrabold text-lg italic tracking-tighter ${isSelected ? "text-white" : "text-slate-900"}`}>
                                                        {user.balance / 100} <span className="text-[9px]">₽</span>
                                                    </p>
                                                    <Badge className={`mt-1.5 border-none text-[8px] font-black uppercase tracking-widest px-2 py-0.5 rounded-md ${isSelected ? "bg-blue-500 text-white" : "bg-slate-100 text-slate-500"}`}>
                                                        {user.subscriptionType}
                                                    </Badge>
                                                </div>
                                            </div>
                                        </motion.div>
                                    )
                                })}
                                {(!users || users.length === 0) && (
                                    <div className="py-20 text-center flex flex-col items-center justify-center opacity-50">
                                        <Search size={44} strokeWidth={1} className="text-slate-300 mb-4" />
                                        <p className="text-[9px] font-black uppercase tracking-[0.3em] text-slate-400 font-sans">Клиентов не найдено</p>
                                    </div>
                                )}
                            </motion.div>
                        )}
                    </div>
                </ResizablePanel>

                <ResizableHandle withHandle className="w-1.5 bg-slate-100 hover:bg-blue-200 transition-colors active:bg-blue-500" />

                <ResizablePanel defaultSize={58} minSize={40} className="bg-white">
                    <AnimatePresence mode="wait">
                        {selectedUser ? (
                            <motion.div
                                key={selectedUser.id}
                                initial={{ opacity: 0, scale: 0.98, x: 20 }}
                                animate={{ opacity: 1, scale: 1, x: 0 }}
                                exit={{ opacity: 0, scale: 0.98, x: -20 }}
                                transition={{ type: "spring", stiffness: 300, damping: 30 }}
                                className="h-full"
                            >
                                <UserDetailsPanel
                                    user={selectedUser}
                                    onClose={() => setSelectedUser(null)}
                                    onUpdate={() => loadData(search)}
                                />
                            </motion.div>
                        ) : (
                            <motion.div
                                initial={{ opacity: 0 }}
                                animate={{ opacity: 1 }}
                                exit={{ opacity: 0 }}
                                className="h-full flex flex-col items-center justify-center space-y-5 bg-slate-50"
                            >
                                <div className="relative group">
                                    <div className="absolute inset-0 bg-blue-500 blur-[80px] opacity-10 group-hover:opacity-20 transition-opacity duration-1000 rounded-full" />
                                    <div className="relative h-32 w-32 rounded-[2.5rem] border-4 border-dashed border-slate-200 flex items-center justify-center text-slate-300 bg-white shadow-xl shadow-slate-200/20 transition-transform duration-700 group-hover:scale-105 group-hover:rotate-3">
                                        <UsersIcon size={44} strokeWidth={1.5} />
                                    </div>
                                </div>
                                <div className="text-center">
                                    <p className="font-extrabold uppercase text-[10px] tracking-[0.35em] text-slate-400 font-sans">Инспектор клиентов</p>
                                    <p className="text-xs text-slate-400 mt-2 italic font-medium">Выберите профиль для детального анализа и редактирования</p>
                                </div>
                            </motion.div>
                        )}
                    </AnimatePresence>
                </ResizablePanel>
            </ResizablePanelGroup>

            {/* CREATE CLIENT MODAL */}
            {showCreateModal && (
                <div style={{ position: "fixed", inset: 0, zIndex: 100, display: "flex", alignItems: "center", justifyContent: "center", padding: "16px", background: "rgba(0,0,0,0.5)", backdropFilter: "blur(4px)" }} onClick={() => setShowCreateModal(false)}>
                    <div style={{ background: "white", padding: "32px", borderRadius: "24px", maxWidth: "480px", width: "100%", boxShadow: "0 20px 40px rgba(0,0,0,0.15)" }} onClick={e => e.stopPropagation()}>
                        <div className="flex justify-between items-center mb-6">
                            <h3 className="text-xl font-extrabold text-slate-900 tracking-tight italic" style={{ fontFamily: "Space Grotesk, sans-serif" }}>Новый клиент</h3>
                            <button onClick={() => setShowCreateModal(false)} className="w-8 h-8 rounded-full flex items-center justify-center hover:bg-slate-100 text-slate-400 cursor-pointer">
                                <X size={16} />
                            </button>
                        </div>

                        {createError && (
                            <div className="p-3 bg-red-50 border border-red-100 text-red-650 text-xs font-bold rounded-xl mb-4">
                                {createError}
                            </div>
                        )}

                        <form onSubmit={handleCreateUserSubmit} className="space-y-4">
                            <div>
                                <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block mb-1">Имя пользователя *</label>
                                <input
                                    type="text"
                                    required
                                    className="w-full h-11 px-4.5 bg-slate-50 border border-slate-250 rounded-xl text-sm outline-none focus:border-blue-500 font-bold"
                                    placeholder="Например: ВАЛЕРА"
                                    value={newFirstName}
                                    onChange={e => setNewFirstName(e.target.value)}
                                />
                            </div>

                            <div>
                                <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block mb-1">Никнейм в Telegram</label>
                                <input
                                    type="text"
                                    className="w-full h-11 px-4.5 bg-slate-50 border border-slate-250 rounded-xl text-sm outline-none focus:border-blue-500 font-bold font-mono"
                                    placeholder="osemeth"
                                    value={newUsername}
                                    onChange={e => setNewUsername(e.target.value)}
                                />
                            </div>

                            <div>
                                <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block mb-1">Telegram ID *</label>
                                <input
                                    type="text"
                                    required
                                    className="w-full h-11 px-4.5 bg-slate-50 border border-slate-250 rounded-xl text-sm outline-none focus:border-blue-500 font-bold font-mono"
                                    placeholder="6262243631"
                                    value={newTgId}
                                    onChange={e => setNewTgId(e.target.value)}
                                />
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block mb-1">Тарифный план</label>
                                    <select
                                        className="w-full h-11 px-3 bg-slate-50 border border-slate-250 rounded-xl text-xs font-bold"
                                        value={newSubPlan}
                                        onChange={e => setNewSubPlan(e.target.value)}
                                    >
                                        <option value="payg">PAYG (По трафику)</option>
                                        <option value="经典极速classic">经典极速classic</option>
                                        <option value="VIP PRO">VIP PRO</option>
                                    </select>
                                </div>
                                <div>
                                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block mb-1">Баланс при старте (₽)</label>
                                    <input
                                        type="number"
                                        className="w-full h-11 px-4.5 bg-slate-50 border border-slate-250 rounded-xl text-sm outline-none focus:border-blue-500 font-bold font-mono"
                                        placeholder="0"
                                        value={newBalance}
                                        onChange={e => setNewBalance(e.target.value)}
                                    />
                                </div>
                            </div>

                            <button
                                type="submit"
                                className="w-full h-12 bg-slate-900 hover:bg-slate-800 text-white rounded-xl font-bold flex items-center justify-center gap-1.5 cursor-pointer shadow-lg shadow-slate-900/10 active:scale-95 transition-all mt-6 text-xs uppercase tracking-widest"
                            >
                                <Check size={16} /> Создать аккаунт
                            </button>
                        </form>
                    </div>
                </div>
            )}
        </div>
    )
}
