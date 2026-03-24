import { BrowserRouter, Routes, Route } from "react-router-dom"
import AdminLayout from "@/components/layout/AdminLayout"
import Dashboard from "@/pages/Dashboard/Dashboard"
import Users from "@/pages/Users/Users"

const Servers = () => <div>Страница Серверов (в разработке)</div>

function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<AdminLayout />}>
                    <Route index element={<Dashboard />} />
                    <Route path="users" element={<Users />} />
                    <Route path="servers" element={<Servers />} />
                    <Route path="settings" element={<div>Настройки</div>} />
                </Route>
            </Routes>
        </BrowserRouter>
    )
}

export default App