import React, { useEffect } from 'react';
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

    useEffect(() => {
        if (!tg) return;

        expand();
        tg.setHeaderColor?.('#000000');
        tg.setBackgroundColor?.('#000000');

        // Отключаем нативный свайп-закрыть в TMA
        tg.disableVerticalSwipes?.();

        if (tg.platform) {
            userApi.syncDevice(tg.platform)
                .then(device => console.log('[App] Device synced:', device.deviceType))
                .catch(err  => console.error('[App] Device sync failed:', err));
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
            className="text-white flex flex-col relative"
            style={{ height: 'var(--tg-height, 100dvh)', overflow: 'hidden' }}
        >
            {/* Хедер — фиксированная высота сверху */}
            <Header />

            {/* Контент страниц — скроллится внутри, отступ под навбар встроен в page-scroll */}
            <main className="page-scroll px-4 relative z-10 custom-scrollbar">
                {activeTab === 'home'          && <Home />}
                {activeTab === 'profile'       && <Profile />}
                {activeTab === 'payments'      && <Payments />}
                {activeTab === 'subscriptions' && <Subscriptions />}
                {activeTab === 'leaderboard'   && <Leaderboard />}
            </main>

            {/* Нижняя навигация */}
            <BottomNav />
        </div>
    );
}
