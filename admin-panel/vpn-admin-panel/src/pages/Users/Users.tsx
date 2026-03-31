import { useState, useEffect, useMemo } from "react"
import { motion, AnimatePresence } from "framer-motion"
import {
    Search,
    Users as UsersIcon,
    TrendingUp,
    RefreshCcw,
    Wallet
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

    const loadData = async () => {
        setLoading(true)
        try {
            const [usersData, statsData] = await Promise.all([
                adminApi.getUsers(0, 100),
                adminApi.getDashboard()
            ])
            setUsers(usersData)
            setStats(statsData)
        } catch (e) {
            console.error(e)
        }
        setLoading(false)
    }

    useEffect(() => {
        loadData()
    }, [])

    const filteredUsers = useMemo(() => {
        return users.filter(u =>
            u.firstName?.toLowerCase().includes(search.toLowerCase()) ||
            u.telegramId?.toString().includes(search) ||
            u.username?.toLowerCase().includes(search.toLowerCase())
        )
    }, [users, search])

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
                <ResizablePanel defaultSize={40} minSize={35} className="bg-white relative flex flex-col">
                    <div className="absolute top-0 left-0 w-full h-32 bg-gradient-to-b from-slate-50 to-transparent z-0 pointer-events-none" />

                    <div className="p-8 pb-4 relative z-10">
                        <header className="flex justify-between items-end mb-8">
                            <div>
                                <h2 className="text-5xl font-black tracking-tighter text-slate-900 italic drop-shadow-sm">Клиенты</h2>
                                <div className="text-[10px] font-black uppercase tracking-[0.3em] text-slate-400 mt-2 flex items-center gap-2 bg-slate-50 w-fit px-3 py-1.5 rounded-full border border-slate-100">
                                    <div className="h-2 w-2 rounded-full bg-blue-600 animate-pulse shadow-[0_0_10px_rgba(37,99,235,0.5)]" />
                                    Всего в базе: {stats?.totalUsers || 0}
                                </div>
                            </div>
                            <Button
                                variant="outline"
                                size="icon"
                                onClick={loadData}
                                className={`h-14 w-14 rounded-2xl border-slate-200 bg-white shadow-lg shadow-slate-200/20 transition-all hover:border-blue-200 hover:text-blue-600 active:scale-95 ${loading ? "animate-spin text-blue-600 border-blue-200" : "text-slate-600"}`}
                            >
                                <RefreshCcw size={20} strokeWidth={2.5} />
                            </Button>
                        </header>

                        <div className="grid grid-cols-2 gap-4 mb-8">
                            <motion.div whileHover={{ y: -2 }} className="p-5 bg-gradient-to-br from-slate-900 to-slate-800 rounded-[2rem] text-white shadow-xl shadow-slate-900/20 relative overflow-hidden">
                                <div className="absolute -right-4 -top-4 h-16 w-16 bg-emerald-500 rounded-full blur-[30px] opacity-20" />
                                <p className="text-[9px] font-black uppercase tracking-widest text-slate-400 mb-2 flex items-center gap-2 relative z-10">
                                    <TrendingUp size={12} className="text-emerald-400" /> Подписки
                                </p>
                                <p className="text-3xl font-black italic tracking-tighter relative z-10">{stats?.activeSubscriptions || 0}</p>
                            </motion.div>
                            <motion.div whileHover={{ y: -2 }} className="p-5 bg-white border-2 border-slate-100 rounded-[2rem] shadow-xl shadow-slate-200/20 relative overflow-hidden">
                                <div className="absolute -right-4 -top-4 h-16 w-16 bg-blue-500 rounded-full blur-[30px] opacity-10" />
                                <p className="text-[9px] font-black uppercase tracking-widest text-slate-400 mb-2 flex items-center gap-2 relative z-10">
                                    <Wallet size={12} className="text-blue-600" /> Оборот
                                </p>
                                <p className="text-3xl font-black italic tracking-tighter text-slate-900 relative z-10">{(stats?.totalBalanceRub || 0)} <span className="text-sm text-slate-300">₽</span></p>
                            </motion.div>
                        </div>

                        <div className="relative mb-2 group">
                            <Search className="absolute left-6 top-1/2 -translate-y-1/2 h-5 w-5 text-slate-300 group-focus-within:text-blue-600 transition-colors" strokeWidth={2.5} />
                            <Input
                                placeholder="Поиск по ID, нику или имени..."
                                className="pl-16 h-16 bg-slate-50/80 backdrop-blur-xl border-2 border-slate-100 rounded-[1.8rem] font-bold text-slate-900 placeholder:text-slate-300 shadow-inner transition-all focus-visible:ring-4 focus-visible:ring-blue-100 focus-visible:border-blue-300 text-base"
                                value={search}
                                onChange={(e) => setSearch(e.target.value)}
                            />
                        </div>
                    </div>

                    <div className="flex-1 overflow-y-auto px-8 pb-8 custom-scrollbar">
                        {loading && users.length === 0 ? (
                            <div className="h-full flex items-center justify-center">
                                <div className="animate-pulse flex flex-col items-center gap-4">
                                    <div className="h-12 w-12 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
                                    <p className="text-[10px] font-black tracking-widest text-slate-400 uppercase">Синхронизация...</p>
                                </div>
                            </div>
                        ) : (
                            <motion.div variants={listContainer} initial="hidden" animate="show" className="space-y-4 pt-2">
                                {filteredUsers.map((user) => {
                                    const isSelected = selectedUser?.id === user.id
                                    const statusColor = STATUS_CONFIG[user.status || 'ACTIVE']?.color || "bg-emerald-500"

                                    return (
                                        <motion.div
                                            variants={listItem}
                                            layoutId={`user-card-${user.id}`}
                                            key={user.id}
                                            onClick={() => setSelectedUser(user)}
                                            className={`group p-5 rounded-[2rem] cursor-pointer transition-all duration-300 relative overflow-hidden border-2 ${
                                                isSelected
                                                    ? "bg-slate-900 text-white border-slate-900 shadow-2xl shadow-slate-900/30 scale-[1.02] z-10"
                                                    : "bg-white border-slate-100 hover:border-blue-200 hover:shadow-xl hover:shadow-blue-500/10 text-slate-900"
                                            }`}
                                        >
                                            <AnimatePresence>
                                                {isSelected && (
                                                    <motion.div
                                                        initial={{ opacity: 0, scale: 0 }}
                                                        animate={{ opacity: 0.4, scale: 1 }}
                                                        exit={{ opacity: 0, scale: 0 }}
                                                        className="absolute -right-8 -top-8 h-32 w-32 bg-blue-500 rounded-full blur-[40px]"
                                                    />
                                                )}
                                            </AnimatePresence>

                                            <div className="flex items-center justify-between relative z-10">
                                                <div className="flex items-center gap-5">
                                                    <div className="relative">
                                                        <Avatar className={`h-14 w-14 rounded-[1.2rem] transition-all duration-500 shadow-sm ${isSelected ? "rotate-6 scale-110 ring-4 ring-white/10" : "group-hover:rotate-6"}`}>
                                                            <AvatarFallback className={`${isSelected ? "bg-blue-500 text-white" : "bg-slate-100 text-slate-600"} font-black text-lg`}>
                                                                {user.firstName ? user.firstName[0].toUpperCase() : '?'}
                                                            </AvatarFallback>
                                                        </Avatar>
                                                        <div className={`absolute -bottom-1 -right-1 h-4 w-4 rounded-full border-[3px] ${isSelected ? "border-slate-900" : "border-white"} ${statusColor} transition-colors`} />
                                                    </div>
                                                    <div>
                                                        <p className="font-black text-base uppercase tracking-tight italic truncate max-w-[150px]">
                                                            {user.firstName || 'Без имени'}
                                                        </p>
                                                        <div className="flex items-center gap-2 mt-1">
                                                            <span className={`text-[10px] font-bold tracking-wider ${isSelected ? "text-slate-300" : "text-blue-600"}`}>
                                                                @{user.username || user.telegramId}
                                                            </span>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div className="text-right">
                                                    <p className={`font-mono font-black text-xl italic tracking-tighter ${isSelected ? "text-white" : "text-slate-900"}`}>
                                                        {user.balance / 100} <span className="text-[10px]">₽</span>
                                                    </p>
                                                    <Badge className={`mt-2 border-none text-[8px] font-black uppercase tracking-widest px-2.5 py-1 rounded-lg ${isSelected ? "bg-blue-500 text-white" : "bg-slate-100 text-slate-500"}`}>
                                                        {user.subscriptionType}
                                                    </Badge>
                                                </div>
                                            </div>
                                        </motion.div>
                                    )
                                })}
                                {filteredUsers.length === 0 && (
                                    <div className="py-20 text-center flex flex-col items-center justify-center opacity-50">
                                        <Search size={48} strokeWidth={1} className="text-slate-300 mb-4" />
                                        <p className="text-[10px] font-black uppercase tracking-[0.3em] text-slate-400">Совпадений не найдено</p>
                                    </div>
                                )}
                            </motion.div>
                        )}
                    </div>
                </ResizablePanel>

                <ResizableHandle withHandle className="w-1.5 bg-slate-100 hover:bg-blue-200 transition-colors active:bg-blue-500" />

                <ResizablePanel defaultSize={60} minSize={40} className="bg-white">
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
                                    onUpdate={loadData}
                                />
                            </motion.div>
                        ) : (
                            <motion.div
                                initial={{ opacity: 0 }}
                                animate={{ opacity: 1 }}
                                exit={{ opacity: 0 }}
                                className="h-full flex flex-col items-center justify-center space-y-6 bg-slate-50/50"
                            >
                                <div className="relative group">
                                    <div className="absolute inset-0 bg-blue-500 blur-[80px] opacity-10 group-hover:opacity-20 transition-opacity duration-1000 rounded-full" />
                                    <div className="relative h-40 w-40 rounded-[3rem] border-4 border-dashed border-slate-200 flex items-center justify-center text-slate-300 bg-white shadow-xl shadow-slate-200/20 transition-transform duration-700 group-hover:scale-105 group-hover:rotate-3">
                                        <UsersIcon size={56} strokeWidth={1.5} />
                                    </div>
                                </div>
                                <div className="text-center">
                                    <p className="font-black uppercase text-[11px] tracking-[0.4em] text-slate-400">Инспектор узлов</p>
                                    <p className="text-sm text-slate-400 mt-3 italic font-medium">Выберите профиль для детального анализа</p>
                                </div>
                            </motion.div>
                        )}
                    </AnimatePresence>
                </ResizablePanel>
            </ResizablePanelGroup>
        </div>
    )
}