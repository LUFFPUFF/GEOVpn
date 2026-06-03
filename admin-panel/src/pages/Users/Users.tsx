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
    Check,
    SlidersHorizontal,
    Download,
    ChevronLeft,
    ChevronRight
} from "lucide-react"
import { Input } from "@/components/ui/input"
import { ResizableHandle, ResizablePanel, ResizablePanelGroup } from "@/components/ui/resizable"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { adminApi } from "@/api/admin"
import { UserResponse, AdminDashboardResponse } from "@/types/api"
import UserDetailsPanel from "./UserDetailsPanel"

export default function Users() {
    const [users, setUsers] = useState<UserResponse[]>([])
    const [stats, setStats] = useState<AdminDashboardResponse | null>(null)
    const [selectedUser, setSelectedUser] = useState<UserResponse | null>(null)
    const [search, setSearch] = useState("")
    const [loading, setLoading] = useState(true)

    const [currentPage, setCurrentPage] = useState(0)
    const [pageSize, setPageSize] = useState(25)

    const [showFilters, setShowFilters] = useState(false)
    const [filterStatus, setFilterStatus] = useState<'ALL' | 'ACTIVE' | 'INACTIVE' | 'BANNED'>('ALL')
    const [filterPlan, setFilterPlan] = useState<'ALL' | 'payg' | 'classic' | 'vip'>('ALL')
    const [sortBy, setSortBy] = useState<'date_desc' | 'balance_desc' | 'balance_asc' | 'name_asc'>('date_desc')

    const [showCreateModal, setShowCreateModal] = useState(false)
    const [newTgId, setNewTgId] = useState("")
    const [newUsername, setNewUsername] = useState("")
    const [newFirstName, setNewFirstName] = useState("")
    const [newSubPlan, setNewSubPlan] = useState("PAYG")
    const [newBalance, setNewBalance] = useState("")
    const [createError, setCreateError] = useState("")

    const loadData = async (searchQuery: string) => {
        setLoading(true)
        try {
            const [usersData, statsData] = await Promise.all([
                adminApi.getUsers(currentPage, pageSize, searchQuery),
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
    }, [search, currentPage, pageSize])

    const totalUsersCount = stats?.totalUsers || users.length
    const totalPages = Math.ceil(totalUsersCount / pageSize)

    const processedUsers = useMemo(() => {
        let result = [...users]

        if (filterStatus !== 'ALL') {
            result = result.filter(u => {
                const isBanned = u.isBanned || (u as any).banned || u.status === "BANNED"
                if (filterStatus === 'BANNED') return isBanned
                if (filterStatus === 'ACTIVE') return !isBanned && u.hasActiveSubscription
                if (filterStatus === 'INACTIVE') return !isBanned && !u.hasActiveSubscription
                return true
            })
        }

        if (filterPlan !== 'ALL') {
            result = result.filter(u => {
                const plan = (u.subscriptionType || "").toUpperCase();
                return plan === filterPlan;
            });
        }

        result.sort((a, b) => {
            if (sortBy === 'balance_desc') return b.balance - a.balance
            if (sortBy === 'balance_asc') return a.balance - b.balance
            if (sortBy === 'name_asc') return (a.firstName || "").localeCompare(b.firstName || "")
            return b.id - a.id
        })

        return result
    }, [users, filterStatus, filterPlan, sortBy])

    const handleExportCSV = () => {
        const headers = ["ID", "Telegram ID", "Username", "First Name", "Balance (RUB)", "Tariff", "Banned"]
        const rows = processedUsers.map(u => [
            u.id,
            u.telegramId,
            u.username || "N/A",
            u.firstName || "N/A",
            u.balance / 100,
            u.subscriptionType,
            (u.isBanned || (u as any).banned) ? "Yes" : "No"
        ])

        const csvContent = "data:text/csv;charset=utf-8,\uFEFF"
            + [headers.join(";"), ...rows.map(e => e.join(";"))].join("\n")

        const encodedUri = encodeURI(csvContent)
        const link = document.createElement("a")
        link.setAttribute("href", encodedUri)
        link.setAttribute("download", `users_export_page_${currentPage + 1}.csv`)
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
    }

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
                balance: Number(newBalance) * 100 || 0,
                status: "ACTIVE"
            })
            setShowCreateModal(false)
            setNewTgId("")
            setNewUsername("")
            setNewFirstName("")
            setNewSubPlan("PAYG")
            setNewBalance("")
            loadData(search)
        } catch (err: any) {
            setCreateError(err.message || "Ошибка создания пользователя")
        }
    }

    const listContainer = {
        hidden: { opacity: 0 },
        show: { opacity: 1, transition: { staggerChildren: 0.03 } }
    }

    const listItem = {
        hidden: { opacity: 0, y: 10 },
        show: { opacity: 1, y: 0, transition: { type: "spring", stiffness: 300, damping: 26 } }
    }

    return (
        <div className="h-[calc(100vh-40px)] w-full overflow-hidden bg-[#F8FAFC] rounded-[3rem] border border-slate-200 shadow-2xl flex">
            <ResizablePanelGroup direction="horizontal">
                <ResizablePanel defaultSize={42} minSize={35} className="bg-white relative flex flex-col">
                    <div className="absolute top-0 left-0 w-full h-32 bg-gradient-to-b from-slate-50 to-transparent z-0 pointer-events-none" />

                    <div className="p-8 pb-3 relative z-10">
                        <header className="flex justify-between items-end mb-6">
                            <div>
                                <h2 className="text-4xl font-black tracking-tighter text-slate-900 italic drop-shadow-sm">Клиенты</h2>
                                <div className="text-[10px] font-black uppercase tracking-[0.3em] text-slate-400 mt-2 flex items-center gap-2 bg-slate-50 w-fit px-3 py-1.5 rounded-full border border-slate-100 font-sans">
                                    <div className="h-2 w-2 rounded-full bg-blue-600 animate-pulse shadow-[0_0_10px_rgba(37,99,235,0.5)]" />
                                    Всего в БД: {totalUsersCount}
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

                        {/* KPI Cards */}
                        <div className="grid grid-cols-2 gap-4 mb-4">
                            <div className="p-4 bg-gradient-to-br from-slate-900 to-slate-800 rounded-[1.5rem] text-white shadow-xl shadow-slate-900/20 relative overflow-hidden">
                                <div className="absolute -right-4 -top-4 h-16 w-16 bg-emerald-500 rounded-full blur-[30px] opacity-20" />
                                <p className="text-[9px] font-black uppercase tracking-widest text-slate-400 mb-2 flex items-center gap-1.5 relative z-10 font-sans">
                                    <TrendingUp size={11} className="text-emerald-400" /> Подписки
                                </p>
                                <p className="text-2xl font-black italic tracking-tighter relative z-10 leading-none">{stats?.activeSubscriptions || 0}</p>
                            </div>
                            <div className="p-4 bg-white border-2 border-slate-100 rounded-[1.5rem] shadow-xl shadow-slate-200/20 relative overflow-hidden">
                                <div className="absolute -right-4 -top-4 h-16 w-16 bg-blue-500 rounded-full blur-[30px] opacity-10" />
                                <p className="text-[9px] font-black uppercase tracking-widest text-slate-400 mb-2 flex items-center gap-1.5 relative z-10 font-sans">
                                    <Wallet size={11} className="text-blue-600" /> Оборот
                                </p>
                                <p className="text-2xl font-black italic tracking-tighter text-slate-900 relative z-10 leading-none">{(stats?.totalBalanceRub || 0)} <span className="text-xs text-slate-300">₽</span></p>
                            </div>
                        </div>

                        {/* Поисковая строка и триггер фильтров */}
                        <div className="flex gap-2 mb-2">
                            <div className="relative flex-1 group">
                                <Search className="absolute left-6 top-1/2 -translate-y-1/2 h-5 w-5 text-slate-300 group-focus-within:text-blue-600 transition-colors" strokeWidth={2.5} />
                                <Input
                                    placeholder="Поиск по ID, нику или имени..."
                                    className="pl-16 h-14 bg-slate-50 border-2 border-slate-100 rounded-2xl font-bold text-slate-950 placeholder:text-slate-300 shadow-inner transition-all focus-visible:ring-4 focus-visible:ring-blue-50 focus-visible:border-blue-300 text-sm"
                                    value={search}
                                    onChange={(e) => { setSearch(e.target.value); setCurrentPage(0); }}
                                />
                            </div>
                            <button
                                onClick={() => setShowFilters(!showFilters)}
                                className={`w-14 h-14 rounded-2xl border-2 flex items-center justify-center transition-all cursor-pointer ${showFilters ? 'bg-blue-55 text-blue-600 border-blue-200' : 'bg-slate-50 border-slate-100 hover:border-slate-200 text-slate-500'}`}
                            >
                                <SlidersHorizontal size={18} />
                            </button>
                        </div>

                        {/* --- ВЫЕЗЖАЮЩАЯ ПАНЕЛЬ РАСШИРЕННЫХ ФИЛЬТРОВ --- */}
                        <AnimatePresence>
                            {showFilters && (
                                <motion.div
                                    initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: "auto" }} exit={{ opacity: 0, height: 0 }}
                                    className="bg-slate-50 border-2 border-slate-100 rounded-2xl p-4 mb-4 grid grid-cols-3 gap-3 overflow-hidden shadow-inner"
                                >
                                    <div>
                                        <label className="text-[8px] font-black uppercase text-slate-400 tracking-wider block mb-1">Тарифный план</label>
                                        <select value={filterPlan} onChange={e => { setFilterPlan(e.target.value as any); setCurrentPage(0); }} className="w-full h-9 bg-white border border-slate-200 rounded-lg text-xs font-bold px-2 outline-none">
                                            <option value="ALL">Все тарифы</option>
                                            <option value="PAYG">PAYG</option>
                                            <option value="BASIC">BASIC</option>
                                            <option value="DAILY">DAILY</option>
                                            <option value="STANDARD">STANDARD</option>
                                            <option value="FAMILY">FAMILY</option>
                                        </select>
                                    </div>
                                    <div>
                                        <label className="text-[8px] font-black uppercase text-slate-400 tracking-wider block mb-1">Тарифный план</label>
                                        <select value={filterPlan} onChange={e => setFilterPlan(e.target.value as any)} className="w-full h-9 bg-white border border-slate-200 rounded-lg text-xs font-bold px-2 outline-none">
                                            <option value="ALL">Все тарифы</option>
                                            <option value="payg">PAYG</option>
                                            <option value="classic">Classic</option>
                                            <option value="vip">VIP PRO</option>
                                        </select>
                                    </div>
                                    <div>
                                        <label className="text-[8px] font-black uppercase text-slate-400 tracking-wider block mb-1">Сортировка</label>
                                        <select value={sortBy} onChange={e => setSortBy(e.target.value as any)} className="w-full h-9 bg-white border border-slate-200 rounded-lg text-xs font-bold px-2 outline-none">
                                            <option value="date_desc">Сначала новые</option>
                                            <option value="balance_desc">Баланс: Сначала много</option>
                                            <option value="balance_asc">Баланс: Сначала мало</option>
                                            <option value="name_asc">По алфавиту (А-Я)</option>
                                        </select>
                                    </div>
                                    <div className="col-span-3 flex justify-end pt-2 border-t border-slate-200/50 mt-1">
                                        <button onClick={handleExportCSV} className="text-[9px] font-black uppercase tracking-wider text-blue-600 hover:text-blue-700 flex items-center gap-1.5 cursor-pointer bg-blue-50/50 hover:bg-blue-100 px-3 py-1.5 rounded-lg border border-blue-100">
                                            <Download size={12} /> Экспорт в CSV (Excel)
                                        </button>
                                    </div>
                                </motion.div>
                            )}
                        </AnimatePresence>
                    </div>

                    {/* СПИСОК ПОЛЬЗОВАТЕЛЕЙ */}
                    <div className="flex-1 overflow-y-auto px-8 pb-4 custom-scrollbar">
                        {loading && users.length === 0 ? (
                            <div className="h-full flex items-center justify-center">
                                <div className="animate-pulse flex flex-col items-center gap-3">
                                    <div className="h-10 w-10 border-4 border-blue-650 border-t-transparent rounded-full animate-spin" />
                                    <p className="text-[9px] font-black tracking-widest text-slate-400 uppercase font-sans">Синхронизация...</p>
                                </div>
                            </div>
                        ) : (
                            <motion.div variants={listContainer} initial="hidden" animate="show" className="space-y-3 pt-1">
                                {processedUsers.map((user) => {
                                    const isSelected = selectedUser?.telegramId === user.telegramId
                                    const isBanned = user.isBanned || (user as any).banned || user.status === "BANNED"

                                    const statusColor = isBanned
                                        ? "bg-red-500"
                                        : (user.hasActiveSubscription ? "bg-emerald-500" : "bg-slate-300")

                                    const cardStyle = isSelected
                                        ? (isBanned ? "bg-[#2D0B0E] text-white border-[#541217] shadow-[0_0_30px_rgba(239,68,68,0.25)] scale-[1.01]" : "bg-slate-900 text-white border-slate-900 shadow-2xl scale-[1.01]")
                                        : (isBanned ? "bg-red-50/30 border-red-100 hover:border-red-200" : "bg-white border-slate-100 hover:border-blue-200")

                                    return (
                                        <motion.div
                                            variants={listItem}
                                            key={user.id}
                                            onClick={() => setSelectedUser(user)}
                                            className={`group p-4.5 rounded-[1.8rem] cursor-pointer transition-all duration-300 relative overflow-hidden border-2 ${cardStyle}`}
                                        >
                                            <div className="flex items-center justify-between relative z-10">
                                                <div className="flex items-center gap-4">
                                                    <div className="relative">
                                                        <Avatar className={`h-12 w-12 rounded-[1rem] transition-all duration-550 shadow-sm ${isSelected ? "rotate-6 scale-110 ring-4 ring-white/10" : "group-hover:rotate-6"}`}>
                                                            <AvatarFallback className={`${isBanned ? (isSelected ? "bg-red-500 text-white" : "bg-red-100 text-red-600") : (isSelected ? "bg-blue-500 text-white" : "bg-slate-100 text-slate-600")} font-black text-base`}>
                                                                {isBanned ? '🚫' : (user.firstName ? user.firstName[0].toUpperCase() : '?')}
                                                            </AvatarFallback>
                                                        </Avatar>
                                                        <div className={`absolute -bottom-1 -right-1 h-3.5 w-3.5 rounded-full border-2 ${isSelected ? (isBanned ? "border-[#2D0B0E]" : "border-slate-900") : "border-white"} ${statusColor} transition-colors`} />
                                                    </div>
                                                    <div>
                                                        <p className={`font-extrabold text-sm uppercase tracking-tight italic truncate max-w-[150px] flex items-center gap-1.5 ${isBanned ? 'line-through text-red-400/80 opacity-90' : ''}`}>
                                                            {user.firstName || 'Без имени'}
                                                        </p>
                                                        <div className="flex items-center gap-2 mt-0.5">
                                                            <span className={`text-[10px] font-bold tracking-wider ${isSelected ? (isBanned ? "text-red-300/80" : "text-slate-300") : (isBanned ? "text-red-500" : "text-blue-650")}`}>
                                                                @{user.username || user.telegramId}
                                                            </span>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div className="text-right">
                                                    <p className={`font-mono font-extrabold text-lg italic tracking-tighter ${isSelected ? "text-white" : "text-slate-900"}`}>
                                                        {user.balance / 100} <span className="text-[9px]">₽</span>
                                                    </p>
                                                    <Badge className={`mt-1.5 border-none text-[8px] font-black uppercase tracking-widest px-2 py-0.5 rounded-md ${isSelected ? (isBanned ? "bg-red-500 text-white" : "bg-blue-500 text-white") : (isBanned ? "bg-red-100 text-red-600" : "bg-slate-100 text-slate-500")}`}>
                                                        {isBanned ? "BANNED" : user.subscriptionType}
                                                    </Badge>
                                                </div>
                                            </div>
                                        </motion.div>
                                    )
                                })}
                                {processedUsers.length === 0 && (
                                    <div className="py-20 text-center flex flex-col items-center justify-center opacity-50">
                                        <Search size={44} strokeWidth={1} className="text-slate-300 mb-4" />
                                        <p className="text-[9px] font-black uppercase tracking-[0.3em] text-slate-400 font-sans">Клиентов не найдено</p>
                                    </div>
                                )}
                            </motion.div>
                        )}
                    </div>

                    {/* --- ФУТЕР С ПАГИНАЦИЕЙ (PAGINATION PANEL) --- */}
                    <div className="px-8 py-4 border-t border-slate-100 flex items-center justify-between bg-slate-50/50 shrink-0">
                        <div className="flex items-center gap-2 text-xs text-slate-500 font-bold">
                            <span className="hidden sm:inline">Показывать:</span>
                            <select
                                value={pageSize}
                                onChange={e => { setPageSize(Number(e.target.value)); setCurrentPage(0); }}
                                className="h-8 bg-white border border-slate-200 rounded-lg px-1.5 font-bold text-slate-700 outline-none"
                            >
                                <option value={10}>10</option>
                                <option value={25}>25</option>
                                <option value={50}>50</option>
                                <option value={100}>100</option>
                            </select>
                        </div>

                        <div className="flex items-center gap-1.5">
                            <button
                                disabled={currentPage === 0}
                                onClick={() => setCurrentPage(prev => Math.max(0, prev - 1))}
                                className="w-8 h-8 flex items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-50 disabled:opacity-30 cursor-pointer"
                            >
                                <ChevronLeft size={16} />
                            </button>
                            <span className="text-xs font-mono font-bold text-slate-600 px-3">
                                Страница {currentPage + 1} из {Math.max(1, totalPages)}
                            </span>
                            <button
                                disabled={currentPage >= totalPages - 1}
                                onClick={() => setCurrentPage(prev => Math.min(totalPages - 1, prev + 1))}
                                className="w-8 h-8 flex items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-50 disabled:opacity-30 cursor-pointer"
                            >
                                <ChevronRight size={16} />
                            </button>
                        </div>
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
                                        <option value="PAYG">PAYG (По трафику)</option>
                                        <option value="BASIC">BASIC</option>
                                        <option value="DAILY">DAILY</option>
                                        <option value="STANDARD">STANDARD</option>
                                        <option value="FAMILY">FAMILY</option>
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