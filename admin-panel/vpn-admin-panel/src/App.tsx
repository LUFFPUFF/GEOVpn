import { useState, useEffect } from "react"
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom"
import AdminLayout from "@/components/layout/AdminLayout"
import Dashboard from "@/pages/Dashboard/Dashboard"
import Users from "@/pages/Users/Users"
import Servers from "@/pages/Servers/Servers"
import FinOpsPanel from "@/pages/FinOps/FinOps"
import SettingsPanel from "@/pages/Settings/Settings"
import Login from "@/pages/Login/Login"

function App() {
    const [isAuth, setIsAuth] = useState(() => {
        return localStorage.getItem("isAuth") === "true"
    })

    const handleLogin = () => setIsAuth(true)
    const handleLogout = () => {
        localStorage.removeItem("isAuth")
        setIsAuth(false)
    }

    return (
        <BrowserRouter>
            <Routes>
                {!isAuth ? (
                    <>
                        <Route path="/login" element={<Login onLogin={handleLogin} />} />
                        <Route path="*" element={<Navigate to="/login" replace />} />
                    </>
                ) : (
                    <Route path="/" element={<AdminLayout onLogout={handleLogout} />}>
                        <Route index element={<Dashboard />} />
                        <Route path="users" element={<Users />} />
                        <Route path="servers" element={<Servers />} />
                        <Route path="finops" element={<FinOpsPanel />} />
                        <Route path="settings" element={<SettingsPanel />} />
                        <Route path="*" element={<Navigate to="/" replace />} />
                    </Route>
                )}
            </Routes>
        </BrowserRouter>
    )
}

export default App