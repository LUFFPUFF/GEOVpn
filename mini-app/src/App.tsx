import React, { useEffect, useState } from 'react';
import './index.css';
import { useTelegram } from './hooks/useTelegram';
import { useUserStore } from './store/userStore';
import { userApi } from './api/user';
import Header from './components/layout/Header';
import BottomNav from './components/layout/BottomNav';
import Home from './pages/Home';
import Profile from './pages/Profile';
import Payments from './pages/Payments';
import Subscriptions from './pages/Subscriptions';
import Leaderboard from './pages/Leaderboard';

export default function App() {
    const { expand, tg } = useTelegram();
    const { activeTab, fetchAll } = useUserStore();
    const [tgReady, setTgReady] = useState(false);

    useEffect(() => {
        if (!tg) return;

        tg.ready();
        expand();
        tg.setHeaderColor?.('#000000');
        tg.setBackgroundColor?.('#000000');
        tg.disableVerticalSwipes?.();

        const setHeight = () => {
            const h = tg.viewportStableHeight || window.innerHeight;
            document.documentElement.style.setProperty('--tg-height', `${h}px`);
        };
        setHeight();
        tg.onEvent('viewportChanged', setHeight);

        setTimeout(() => {
            setTgReady(true);
        }, 300);

        return () => tg.offEvent('viewportChanged', setHeight);
    }, [tg]);

    useEffect(() => {
        if (!tgReady) return;

        if (tg?.platform) {
            userApi.syncDevice(tg.platform).catch(err => {
                console.error('[App] Device sync failed:', err);
            });
        }

        fetchAll();
    }, [tgReady]);

    return (
        <div 
            className="text-white flex flex-col relative"
            style={{ height: 'var(--tg-height, 100dvh)', overflow: 'hidden' }}
        >
            <Header />
            <main className="page-scroll px-4 relative z-10 custom-scrollbar pb-safe flex-1 overflow-y-auto">
                {activeTab === 'home'          && <Home />}
                {activeTab === 'profile'       && <Profile />}
                {activeTab === 'payments'      && <Payments />}
                {activeTab === 'subscriptions' && <Subscriptions />}
                {activeTab === 'leaderboard'   && <Leaderboard />}
            </main>
            <BottomNav />
        </div>
    );
}
