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

import bgVideo from './assets/fon/bg.mp4';

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
            className="text-white flex flex-col relative bg-black"
            style={{ height: 'var(--tg-height, 100dvh)', overflow: 'hidden' }}
        >
            <video
                key={activeTab}
                src={bgVideo}
                autoPlay
                loop
                muted
                playsInline
                className="fixed top-0 left-0 w-full h-full object-cover z-0 pointer-events-none"
            />

            <div className="fixed top-0 left-0 w-full h-full z-0 bg-black/40 pointer-events-none" />

            <div className="relative z-10 flex flex-col h-full w-full overflow-hidden">
                <Header />

                <main className="flex-1 overflow-y-auto custom-scrollbar px-4 pb-24">
                    {activeTab === 'home'          && <Home />}
                    {activeTab === 'profile'       && <Profile />}
                    {activeTab === 'payments'      && <Payments />}
                    {activeTab === 'subscriptions' && <Subscriptions />}
                    {activeTab === 'leaderboard'   && <Leaderboard />}
                </main>

                <BottomNav />
            </div>
        </div>
    );
}