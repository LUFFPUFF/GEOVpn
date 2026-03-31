import { useState } from "react"
import { ShieldCheck, Lock, User, ArrowRight } from "lucide-react"
import { motion } from "framer-motion"

export default function Login({ onLogin }: { onLogin: () => void }) {
    const [username, setUsername] = useState("")
    const [password, setPassword] = useState("")
    const [error, setError] = useState(false)

    const ADMIN_CREDENTIALS = {
        user: "admin",
        pass: "admin"
    }

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault()
        if (username === ADMIN_CREDENTIALS.user && password === ADMIN_CREDENTIALS.pass) {
            localStorage.setItem("isAuth", "true")
            onLogin()
        } else {
            setError(true)
            setTimeout(() => setError(false), 2000)
        }
    }

    return (
        <div className="min-h-screen flex items-center justify-center bg-[#f4f7fb] dark:bg-[#020617] p-6 transition-colors duration-300">
            <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                className="w-full max-w-[400px]"
            >
                <div className="bg-white dark:bg-[#0f172a] rounded-[2.5rem] p-10 shadow-2xl shadow-blue-500/5 border border-slate-100 dark:border-slate-800">

                    {/* Логотип */}
                    <div className="flex flex-col items-center mb-10">
                        <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-blue-600 to-blue-700 flex items-center justify-center mb-4 shadow-lg shadow-blue-500/20">
                            <ShieldCheck className="h-8 w-8 text-white" />
                        </div>
                        <h1 className="text-3xl font-black text-slate-900 dark:text-white tracking-tight" style={{ fontFamily: "'Space Grotesk', sans-serif" }}>
                            GeoVPN <span className="text-blue-600">Admin</span>
                        </h1>
                        <p className="text-slate-400 dark:text-slate-500 font-medium text-sm mt-2">Введите данные администратора</p>
                    </div>

                    <form onSubmit={handleSubmit} className="space-y-4">
                        {/* Поле Логин */}
                        <div className="relative group">
                            <User className="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 text-slate-400 group-focus-within:text-blue-500 transition-colors" />
                            <input
                                type="text"
                                placeholder="Логин"
                                className="w-full h-14 pl-12 pr-4 bg-slate-50 dark:bg-[#020617] border border-slate-200 dark:border-slate-800 rounded-2xl outline-none text-slate-900 dark:text-white font-semibold focus:border-blue-500 transition-all"
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                            />
                        </div>

                        {/* Поле Пароль */}
                        <div className="relative group">
                            <Lock className="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 text-slate-400 group-focus-within:text-blue-500 transition-colors" />
                            <input
                                type="password"
                                placeholder="Пароль"
                                className={`w-full h-14 pl-12 pr-4 bg-slate-50 dark:bg-[#020617] border rounded-2xl outline-none text-slate-900 dark:text-white font-semibold transition-all ${error ? 'border-red-500 animate-shake' : 'border-slate-200 dark:border-slate-800 focus:border-blue-500'}`}
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                            />
                        </div>

                        {error && (
                            <p className="text-red-500 text-xs font-bold text-center uppercase tracking-widest animate-pulse">
                                Ошибка доступа
                            </p>
                        )}

                        <button
                            type="submit"
                            className="w-full h-14 bg-gradient-to-r from-blue-600 to-blue-700 text-white rounded-2xl font-bold flex items-center justify-center gap-2 shadow-lg shadow-blue-500/25 hover:shadow-blue-500/40 hover:-translate-y-0.5 active:scale-95 transition-all mt-6"
                        >
                            Войти в панель <ArrowRight className="h-5 w-5" />
                        </button>
                    </form>
                </div>

                <p className="text-center text-slate-400 dark:text-slate-600 text-xs mt-8 font-medium">
                    © 2026 GeoVPN Infrastructure. Все права защищены.
                </p>
            </motion.div>

            <style>{`
                @keyframes shake {
                    0%, 100% { transform: translateX(0); }
                    25% { transform: translateX(-5px); }
                    75% { transform: translateX(5px); }
                }
                .animate-shake { animation: shake 0.2s ease-in-out 0s 2; }
            `}</style>
        </div>
    )
}