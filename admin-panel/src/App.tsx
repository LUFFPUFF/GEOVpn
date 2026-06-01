import { useState, useEffect } from "react"
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom"
import AdminLayout from "./components/layout/AdminLayout"
import Login from "./pages/Login/Login"
import Dashboard from "./pages/Dashboard/Dashboard"
import Users from "./pages/Users/Users"
import Servers from "./pages/Servers/Servers"
import FinOps from "./pages/FinOps/FinOps"
import Settings from "./pages/Settings/Settings"
import VpnConfigs from "./pages/VpnConfigs/VpnConfigs"

export default function App() {
    const [isAuth, setIsAuth] = useState<boolean>(() => {
        if (typeof window !== "undefined") {
            return localStorage.getItem("isAuth") === "true"
        }
        return false
    })

    const handleLogin = () => {
        setIsAuth(true)
    }

    const handleLogout = () => {
        localStorage.removeItem("isAuth")
        setIsAuth(false)
    }

    return (
        <BrowserRouter>
            <Routes>
                {/* Auth Gate Routing */}
                <Route
                    path="/login"
                    element={isAuth ? <Navigate to="/" replace /> : <Login onLogin={handleLogin} />}
                />

                {/* Primary Panel Pages */}
                <Route
                    path="/"
                    element={isAuth ? <AdminLayout onLogout={handleLogout} /> : <Navigate to="/login" replace />}
                >
                    <Route index element={<Dashboard />} />
                    <Route path="users" element={<Users />} />
                    <Route path="servers" element={<Servers />} />
                    <Route path="finops" element={<FinOps />} />
                    <Route path="settings" element={<Settings />} />
                    <Route path="configs" element={<VpnConfigs />} />
                </Route>

                {/* Fallback routing */}
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </BrowserRouter>
    )
}
