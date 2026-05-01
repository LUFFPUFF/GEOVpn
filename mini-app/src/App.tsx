import React, { useEffect } from 'react';
import './index.css';
import { useTelegram } from './hooks/useTelegram';
import { useUserStore } from './store/userStore';
import { userApi } from './api/user'; // Импортируем твой API сервис
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

    useEffect(() => {
        if (!tg) return;

        expand();

        tg.setHeaderColor?.('#000000');
        tg.setBackgroundColor?.('#000000');

        if (tg.platform) {
            userApi.syncDevice(tg.platform)
                .then(device => {
                    console.log('[App] Device synced successfully:', device.deviceType);
                })
                .catch(err => {
                    console.error('[App] Device sync failed:', err);
                });
        }

        const setHeight = () => {
            const h = tg.viewportStableHeight || window.innerHeight;
            document.documentElement.style.setProperty('--tg-height', `${h}px`);
        };

        setHeight();
        tg.onEvent('viewportChanged', setHeight);

        return () => tg.offEvent('viewportChanged', setHeight);
    }, [tg, expand]);

    useEffect(() => {
        fetchAll();
    }, [fetchAll]);


    return (
        <div
            className="text-white relative flex flex-col overflow-hidden"
            style={{ height: 'var(--tg-height, 100dvh)' }}
        >
            <Header />

            <main className="relative z-10 flex-1 overflow-y-auto px-4 custom-scrollbar pb-safe">
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