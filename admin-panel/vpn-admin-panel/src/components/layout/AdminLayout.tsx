import { useState, useEffect } from "react"
import { Link, Outlet, useLocation } from "react-router-dom"
import {
    LayoutDashboard, Users, Server, Settings, LogOut,
    ShieldCheck, LineChart, Bell, Sun, Moon
} from "lucide-react"

export default function AdminLayout() {
    const location = useLocation()

    // Стейт для темной темы с сохранением в localStorage
    const [isDark, setIsDark] = useState(() => {
        if (typeof window !== 'undefined') {
            return localStorage.getItem('theme') === 'dark'
        }
        return false
    })

    useEffect(() => {
        const root = window.document.documentElement
        if (isDark) {
            root.classList.add('dark')
            localStorage.setItem('theme', 'dark')
        } else {
            root.classList.remove('dark')
            localStorage.setItem('theme', 'light')
        }
    }, [isDark])

    const navItems = [
        { name: "Дашборд", path: "/", icon: LayoutDashboard },
        { name: "Пользователи", path: "/users", icon: Users },
        { name: "Серверы", path: "/servers", icon: Server },
        { name: "Бухучет", path: "/finops", icon: LineChart },
        { name: "Настройки", path: "/settings", icon: Settings },
    ]

    const getPageTitle = () => {
        const item = navItems.find(i => i.path === location.pathname);
        return item ? item.name : "Панель управления";
    }

    return (
        <div className="flex h-screen bg-[#f4f7fb] dark:bg-[#020617] font-sans antialiased transition-colors duration-300">
            {/* ГЛОБАЛЬНЫЕ ПЕРЕМЕННЫЕ ДЛЯ ТЕМНОЙ/СВЕТЛОЙ ТЕМЫ */}
            <style>{`
                @import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;700;800&family=Inter:wght@400;500;600;700&display=swap');
                
                :root {
                    --bg-base: #f4f7fb;
                    --bg-card: #ffffff;
                    --border-color: #e2e8f0;
                    --text-main: #0f172a;
                    --text-muted: #64748b;
                    --bg-input: #f8fafc;
                    --bg-hover: #f1f5f9;
                    --shadow-card: 0 1px 3px rgba(0,0,0,0.02);
                }
                
                .dark {
                    --bg-base: #020617;      /* Очень темный синий фон */
                    --bg-card: #0f172a;      /* Фон карточек (чуть светлее) */
                    --border-color: #1e293b; /* Темные бордеры */
                    --text-main: #f8fafc;    /* Белый текст */
                    --text-muted: #94a3b8;   /* Серый текст */
                    --bg-input: #020617;     /* Фон инпутов */
                    --bg-hover: #1e293b;     /* Фон при наведении */
                    --shadow-card: 0 4px 20px rgba(0,0,0,0.4);
                }
            `}</style>

            {/* --- БОКОВОЕ МЕНЮ (SIDEBAR) --- */}
            <aside className="w-[260px] bg-white dark:bg-[#0f172a] border-r border-slate-200 dark:border-slate-800 flex flex-col shadow-[4px_0_24px_rgba(0,0,0,0.02)] dark:shadow-[4px_0_24px_rgba(0,0,0,0.2)] z-10 relative transition-colors duration-300">

                <div className="h-20 flex items-center px-8 border-b border-slate-100 dark:border-slate-800">
                    <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-blue-600 to-blue-700 flex items-center justify-center mr-3 shadow-md shadow-blue-500/20">
                        <ShieldCheck className="h-5 w-5 text-white" />
                    </div>
                    <span className="text-2xl font-extrabold text-slate-900 dark:text-white tracking-tight" style={{ fontFamily: "'Space Grotesk', sans-serif" }}>
                        GeoVPN
                    </span>
                </div>

                <nav className="flex-1 px-4 py-8 space-y-2 overflow-y-auto custom-scroll">
                    <div className="px-4 text-xs font-bold text-slate-400 dark:text-slate-500 uppercase tracking-widest mb-4">
                        Главное меню
                    </div>
                    {navItems.map((item) => {
                        const Icon = item.icon
                        const isActive = location.pathname === item.path

                        return (
                            <Link
                                key={item.path}
                                to={item.path}
                                className={`group flex items-center px-4 py-3.5 text-sm font-semibold rounded-xl transition-all duration-200 ${
                                    isActive
                                        ? "bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-400 shadow-sm"
                                        : "text-slate-500 hover:bg-slate-50 dark:hover:bg-slate-800/50 hover:text-slate-900 dark:hover:text-white"
                                }`}
                            >
                                <Icon className={`h-5 w-5 mr-3 transition-colors ${isActive ? "text-blue-600 dark:text-blue-400" : "text-slate-400 group-hover:text-slate-600 dark:group-hover:text-slate-300"}`} />
                                {item.name}

                                {isActive && (
                                    <div className="ml-auto w-1.5 h-1.5 rounded-full bg-blue-600 dark:bg-blue-400 shadow-[0_0_8px_rgba(37,99,235,0.6)]" />
                                )}
                            </Link>
                        )
                    })}
                </nav>

                <div className="p-6 border-t border-slate-100 dark:border-slate-800">
                    <button className="group flex items-center w-full px-4 py-3 text-sm font-semibold text-slate-500 dark:text-slate-400 rounded-xl hover:bg-red-50 dark:hover:bg-red-900/20 hover:text-red-600 dark:hover:text-red-400 transition-all duration-200">
                        <LogOut className="h-5 w-5 mr-3 text-slate-400 group-hover:text-red-500 transition-colors" />
                        Выйти из системы
                    </button>
                </div>
            </aside>

            {/* --- ОСНОВНАЯ ЧАСТЬ (MAIN) --- */}
            <main className="flex-1 flex flex-col h-screen overflow-hidden relative">

                {/* Шапка (Header) */}
                <header className="h-20 bg-white/70 dark:bg-[#0f172a]/80 backdrop-blur-md border-b border-slate-200 dark:border-slate-800 flex items-center justify-between px-10 sticky top-0 z-20 transition-colors duration-300">
                    <div className="flex items-center">
                        <h1 className="text-xl font-bold text-slate-900 dark:text-white" style={{ fontFamily: "'Space Grotesk', sans-serif" }}>
                            {getPageTitle()}
                        </h1>
                    </div>

                    <div className="flex items-center gap-6">
                        {/* КНОПКА ПЕРЕКЛЮЧЕНИЯ ТЕМЫ */}
                        <button
                            onClick={() => setIsDark(!isDark)}
                            className="p-2.5 text-slate-400 hover:text-blue-500 hover:bg-blue-50 dark:hover:bg-slate-800 transition-all rounded-full"
                        >
                            {isDark ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
                        </button>

                        <button className="relative p-2.5 text-slate-400 hover:text-slate-600 dark:hover:text-white transition-colors rounded-full hover:bg-slate-100 dark:hover:bg-slate-800">
                            <Bell className="h-5 w-5" />
                            <span className="absolute top-2 right-2.5 w-2 h-2 rounded-full bg-red-500 border-2 border-white dark:border-[#0f172a] box-content"></span>
                        </button>

                        <div className="h-8 w-px bg-slate-200 dark:bg-slate-700"></div>

                        <div className="flex items-center gap-3 cursor-pointer group">
                            <div className="text-right hidden md:block">
                                <div className="text-sm font-bold text-slate-900 dark:text-white leading-tight">Admin User</div>
                                <div className="text-xs text-slate-500 dark:text-slate-400 font-medium">geo-vpn@owner.com</div>
                            </div>
                            <div className="h-10 w-10 rounded-full bg-gradient-to-tr from-blue-600 to-indigo-500 flex items-center justify-center text-white font-bold text-sm shadow-md ring-2 ring-white dark:ring-slate-800 transition-all">
                                AD
                            </div>
                        </div>
                    </div>
                </header>

                <div className="flex-1 overflow-y-auto">
                    <Outlet />
                </div>
            </main>

        </div>
    )
}