import { useState, useEffect, useMemo } from "react"
import { motion, AnimatePresence } from "framer-motion"
import {
    Search,
    Users as UsersIcon,
    TrendingUp,
    Zap,
    RefreshCcw,
    LayoutGrid,
    Wallet,
    ArrowUpRight,
    Filter
} from "lucide-react"
import { Input } from "@/components/ui/input"
import { ResizableHandle, ResizablePanel, ResizablePanelGroup } from "@/components/ui/resizable"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { adminApi } from "@/api/admin"
import { UserResponse, AdminDashboardResponse } from "@/types/api"
import UserDetailsPanel from "./UserDetailsPanel"

export const STATUS_CONFIG: any = {
    ACTIVE: { color: "bg-emerald-500", label: "Активен" },
    BANNED: { color: "bg-red-500", label: "Бан" },
    INACTIVE: { color: "bg-slate-300", label: "Спит" }
};

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
                adminApi.getUsers(),
                adminApi.getDashboard()
            ]);
            setUsers(usersData);
            setStats(statsData);
        } catch (e) { console.error("Load failed", e) }
        setLoading(false)
    }

    useEffect(() => { loadData() }, [])

    const filteredUsers = useMemo(() => {
        return users.filter(u =>
            u.firstName.toLowerCase().includes(search.toLowerCase()) ||
            u.telegramId.toString().includes(search) ||
            u.username?.toLowerCase().includes(search.toLowerCase())
        )
    }, [users, search])

    return (
        <div className="h-[calc(100vh-40px)] w-full overflow-hidden bg-[#F8FAFC] rounded-[3rem] border border-slate-200 shadow-2xl flex">
            <ResizablePanelGroup direction="horizontal">
                <ResizablePanel defaultSize={40} minSize={35} className="bg-white">
                    <div className="flex flex-col h-full">
                        <div className="p-8 pb-0">
                            <header className="flex justify-between items-end mb-8">
                                <div>
                                    <h2 className="text-5xl font-black tracking-tighter text-slate-900 italic">Клиенты</h2>
                                    <p className="text-[10px] font-black uppercase tracking-[0.3em] text-slate-400 mt-2 flex items-center gap-2">
                                        <div className="h-2 w-2 rounded-full bg-blue-600 animate-pulse" />
                                        Всего в базе: {stats?.totalUsers || 0}
                                    </p>
                                </div>
                                <Button
                                    variant="outline"
                                    size="icon"
                                    onClick={loadData}
                                    className={`h-12 w-12 rounded-2xl border-slate-100 shadow-sm transition-all active:scale-95 ${loading ? "animate-spin" : ""}`}
                                >
                                    <RefreshCcw size={18} className="text-slate-600" />
                                </Button>
                            </header>

                            <div className="grid grid-cols-2 gap-3 mb-8">
                                <div className="p-4 bg-slate-900 rounded-[1.5rem] text-white">
                                    <p className="text-[8px] font-black uppercase tracking-widest text-slate-500 mb-1 flex items-center gap-2">
                                        <TrendingUp size={10} className="text-emerald-400" /> Подписки
                                    </p>
                                    <p className="text-xl font-black italic tracking-tighter">{stats?.activeSubscriptions || 0}</p>
                                </div>
                                <div className="p-4 bg-white border border-slate-100 rounded-[1.5rem] shadow-sm">
                                    <p className="text-[8px] font-black uppercase tracking-widest text-slate-400 mb-1 flex items-center gap-2">
                                        <Wallet size={10} className="text-blue-600" /> Оборот
                                    </p>
                                    <p className="text-xl font-black italic tracking-tighter">{(stats?.totalBalanceRub || 0) / 100} ₽</p>
                                </div>
                            </div>

                            <div className="relative mb-6 group">
                                <Search className="absolute left-5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-300 group-focus-within:text-blue-600 transition-colors" />
                                <Input
                                    placeholder="Поиск по ID или нику..."
                                    className="pl-14 h-16 bg-slate-50 border-none rounded-[1.4rem] font-bold text-slate-900 placeholder:text-slate-300 shadow-inner transition-all focus-visible:ring-2 focus-visible:ring-blue-100"
                                    value={search}
                                    onChange={(e) => setSearch(e.target.value)}
                                />
                            </div>
                        </div>

                        <div className="flex-1 overflow-y-auto px-6 pb-8 custom-scrollbar space-y-3">
                            {filteredUsers.map((user) => (
                                <motion.div
                                    layout
                                    key={user.id}
                                    onClick={() => setSelectedUser(user)}
                                    className={`group p-5 rounded-[2rem] cursor-pointer transition-all duration-500 relative overflow-hidden ${
                                        selectedUser?.id === user.id
                                            ? "bg-slate-900 text-white shadow-2xl scale-[1.02]"
                                            : "bg-white border border-slate-100 hover:border-blue-200 hover:shadow-xl hover:shadow-blue-500/5 text-slate-900"
                                    }`}
                                >
                                    {selectedUser?.id === user.id && (
                                        <div className="absolute -right-4 -top-4 h-20 w-20 bg-blue-600 rounded-full blur-[40px] opacity-30" />
                                    )}

                                    <div className="flex items-center justify-between relative z-10">
                                        <div className="flex items-center gap-5">
                                            <div className="relative">
                                                <Avatar className={`h-12 w-12 rounded-2xl transition-all duration-500 ${selectedUser?.id === user.id ? "rotate-12 scale-110" : ""}`}>
                                                    <AvatarFallback className={`${selectedUser?.id === user.id ? "bg-blue-600 text-white" : "bg-slate-100 text-slate-400"} font-black text-sm`}>
                                                        {user.firstName[0]}
                                                    </AvatarFallback>
                                                </Avatar>
                                                <div className={`absolute -bottom-1 -right-1 h-3.5 w-3.5 rounded-full border-4 border-white ${STATUS_CONFIG[user.status || 'ACTIVE'].color}`} />
                                            </div>
                                            <div>
                                                <p className="font-black text-sm uppercase tracking-tight italic">{user.firstName}</p>
                                                <div className="flex items-center gap-2 mt-1">
                                                    <span className={`text-[10px] font-bold ${selectedUser?.id === user.id ? "text-slate-400" : "text-blue-600"}`}>
                                                        @{user.username || 'user'}
                                                    </span>
                                                </div>
                                            </div>
                                        </div>
                                        <div className="text-right">
                                            <p className={`font-mono font-black text-base italic tracking-tighter ${selectedUser?.id === user.id ? "text-white" : "text-slate-900"}`}>
                                                {user.balance / 100} <span className="text-[10px]">₽</span>
                                            </p>
                                            <Badge className={`mt-1 border-none text-[8px] font-black uppercase tracking-tighter px-2 py-0.5 ${selectedUser?.id === user.id ? "bg-blue-600 text-white" : "bg-slate-100 text-slate-400"}`}>
                                                {user.subscriptionType}
                                            </Badge>
                                        </div>
                                    </div>
                                </motion.div>
                            ))}
                        </div>
                    </div>
                </ResizablePanel>

                <ResizableHandle withHandle className="w-[1px] bg-slate-100" />

                <ResizablePanel defaultSize={60} minSize={40}>
                    <AnimatePresence mode="wait">
                        {selectedUser ? (
                            <motion.div
                                key={selectedUser.id}
                                initial={{ opacity: 0, x: 20 }}
                                animate={{ opacity: 1, x: 0 }}
                                exit={{ opacity: 0, x: -20 }}
                                transition={{ duration: 0.5, ease: "anticipate" }}
                                className="h-full"
                            >
                                <UserDetailsPanel
                                    user={selectedUser}
                                    onClose={() => setSelectedUser(null)}
                                    onUpdate={loadData}
                                />
                            </motion.div>
                        ) : (
                            <div className="h-full flex flex-col items-center justify-center space-y-6 bg-white">
                                <div className="relative">
                                    <div className="absolute inset-0 bg-blue-600 blur-[100px] opacity-10 rounded-full" />
                                    <div className="relative h-32 w-32 rounded-[3rem] border-4 border-dashed border-slate-100 flex items-center justify-center text-slate-200">
                                        <UsersIcon size={48} strokeWidth={1.5} />
                                    </div>
                                </div>
                                <div className="text-center">
                                    <p className="font-black uppercase text-[10px] tracking-[0.4em] text-slate-400">Панель управления</p>
                                    <p className="text-xs text-slate-300 mt-2 italic font-medium">Выберите клиента для просмотра терминалов</p>
                                </div>
                            </div>
                        )}
                    </AnimatePresence>
                </ResizablePanel>
            </ResizablePanelGroup>
        </div>
    )
}