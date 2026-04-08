import React, { useEffect } from 'react';
import './index.css';
import { useTelegram } from './hooks/useTelegram';
import { useUserStore } from './store/userStore';
import Header from './components/layout/Header';
import BottomNav from './components/layout/BottomNav';
import Home from './pages/Home';
import Profile from './pages/Profile';
import Payments from './pages/Payments';
import Subscriptions from './pages/Subscriptions';
import Leaderboard from "./pages/Leaderboard";

export default function App() {
    const { expand, tg } = useTelegram();
    const { activeTab, fetchAll } = useUserStore();

    useEffect(() => {
        if (tg) {
            expand();
            if (tg.setHeaderColor) tg.setHeaderColor('#000000');
        }
    }, [tg, expand]);

    useEffect(() => { fetchAll(); }, [fetchAll]);

    return (
        <div className="min-h-screen text-white relative flex flex-col">
            <Header />
            <main className="relative z-10 flex-grow px-4">
                {activeTab === 'home' && <Home />}
                {activeTab === 'profile' && <Profile />}
                {activeTab === 'payments' && <Payments />}
                {activeTab === 'subscriptions' && <Subscriptions />}
                {activeTab === 'leaderboard' && <Leaderboard />}
            </main>
            <BottomNav />
        </div>
    );
}