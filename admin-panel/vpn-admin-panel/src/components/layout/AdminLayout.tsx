import { Link, Outlet, useLocation } from "react-router-dom"
import { LayoutDashboard, Users, Server, Settings, LogOut, ShieldCheck } from "lucide-react"

export default function AdminLayout() {
    const location = useLocation()

    const navItems = [
        { name: "Дашборд", path: "/", icon: LayoutDashboard },
        { name: "Пользователи", path: "/users", icon: Users },
        { name: "Серверы", path: "/servers", icon: Server },
        { name: "Настройки", path: "/settings", icon: Settings },
    ]

    return (
        <div className="flex h-screen bg-slate-50 dark:bg-slate-950">

            <aside className="w-64 border-r bg-white dark:bg-slate-900 flex flex-col">
                <div className="h-16 flex items-center px-6 border-b">
                    <ShieldCheck className="h-6 w-6 text-blue-600 mr-2" />
                    <span className="text-lg font-bold text-slate-900 dark:text-white">GeoVPN Admin</span>
                </div>

                <nav className="flex-1 px-4 py-6 space-y-2 overflow-y-auto">
                    {navItems.map((item) => {
                        const Icon = item.icon
                        const isActive = location.pathname === item.path

                        return (
                            <Link
                                key={item.path}
                                to={item.path}
                                className={`flex items-center px-4 py-3 text-sm font-medium rounded-lg transition-colors ${
                                    isActive
                                        ? "bg-blue-50 text-blue-700 dark:bg-slate-800 dark:text-blue-400"
                                        : "text-slate-600 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-white"
                                }`}
                            >
                                <Icon className="h-5 w-5 mr-3" />
                                {item.name}
                            </Link>
                        )
                    })}
                </nav>

                <div className="p-4 border-t">
                    <button className="flex items-center w-full px-4 py-3 text-sm font-medium text-red-600 rounded-lg hover:bg-red-50 dark:hover:bg-red-950/30 transition-colors">
                        <LogOut className="h-5 w-5 mr-3" />
                        Выйти
                    </button>
                </div>
            </aside>

            <main className="flex-1 overflow-y-auto">
                <header className="h-16 border-b bg-white dark:bg-slate-900 flex items-center justify-between px-8">
                    <h1 className="text-sm font-medium text-slate-500">Панель управления</h1>
                    <div className="h-8 w-8 rounded-full bg-slate-200"></div>
                </header>

                <div className="p-8">
                    <Outlet />
                </div>
            </main>

        </div>
    )
}