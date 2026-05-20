import React, { useEffect, useState } from 'react';
import './index.css';
import { useTelegram } from './hooks/useTelegram';
import { useUserStore } from './store/userStore';
import Header from './components/layout/Header';
import BottomNav from './components/layout/BottomNav';
import Home from './pages/Home';
import Profile from './pages/Profile';
import Payments from './pages/Payments';
import Deposit from './pages/Deposit/Deposit'
import Subscriptions from './pages/Subscriptions';
import Leaderboard from './pages/Leaderboard';
import ManageSubscription from './pages/ManageSubscription/ManageSubscription';
import SubscriptionGuard from './components/guards/SubscriptionGuard';

import bgVideo from './assets/fon/video10.mp4';

export default function App() {
    const { expand, tg } = useTelegram();
    const { activeTab, fetchAll, isMember, loading } = useUserStore();
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
            document.body.style.height = `${h}px`;
        };

        setHeight();
        tg.onEvent('viewportChanged', setHeight);
        setTimeout(() => setTgReady(true), 300);

        return () => tg.offEvent('viewportChanged', setHeight);
    }, [tg]);

    useEffect(() => {
        if (!tgReady) return;
        fetchAll();
    }, [tgReady]);

    return (
        <div
            style={{
                position: 'relative',
                width: '100%',
                height: 'var(--tg-height, 100dvh)',
                overflow: 'hidden',
                background: '#000',
            }}
        >
            <video
                src={bgVideo}
                autoPlay loop muted playsInline
                style={{
                    position: 'absolute', inset: 0,
                    width: '100%', height: '100%',
                    objectFit: 'cover', zIndex: 0, pointerEvents: 'none',
                }}
            />
            <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.45)', zIndex: 1, pointerEvents: 'none' }} />

            <div
                style={{
                    position: 'relative',
                    zIndex: 2,
                    display: 'flex',
                    flexDirection: 'column',
                    width: '100%',
                    height: '100%',
                    color: 'white',
                }}
            >
                <div style={{ flexShrink: 0 }}>
                    <Header />
                </div>

                <SubscriptionGuard>
                    <main
                        style={{
                            flex: 1,
                            overflowY: 'auto',
                            overflowX: 'hidden',
                            paddingBottom: 'calc(88px + env(safe-area-inset-bottom, 0px))',
                            paddingLeft: '1rem',
                            paddingRight: '1rem',
                            WebkitOverflowScrolling: 'touch',
                            overscrollBehavior: 'contain',
                            minHeight: 0,
                        } as React.CSSProperties}
                        className="custom-scrollbar"
                    >
                        {activeTab === 'home'          && <Home />}
                        {activeTab === 'profile'       && <Profile />}
                        {activeTab === 'payments'      && <Payments />}
                        {activeTab === 'deposit'       && <Deposit />}
                        {activeTab === 'subscriptions' && <Subscriptions />}
                        {activeTab === 'leaderboard'   && <Leaderboard />}
                        {activeTab === 'manage_subscription' && <ManageSubscription />}
                    </main>

                    <div style={{ flexShrink: 0 }}>
                        <BottomNav />
                    </div>
                </SubscriptionGuard>
            </div>
        </div>
    );
}