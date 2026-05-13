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

import bgVideo from './assets/fon/video10.mp4';

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
            // Берём стабильную высоту — не прыгает при появлении клавиатуры
            const h = tg.viewportStableHeight || window.innerHeight;
            document.documentElement.style.setProperty('--tg-height', `${h}px`);
            // Дублируем на body для надёжности
            document.body.style.height = `${h}px`;
        };

        setHeight();
        tg.onEvent('viewportChanged', setHeight);
        setTimeout(() => setTgReady(true), 300);

        return () => tg.offEvent('viewportChanged', setHeight);
    }, [tg]);

    useEffect(() => {
        if (!tgReady) return;
        if (tg?.platform) {
            userApi.syncDevice(tg.platform).catch(err =>
                console.error('[App] Device sync failed:', err)
            );
        }
        fetchAll();
    }, [tgReady]);

    return (
        // Внешний контейнер — только размеры, без flex
        <div
            style={{
                position: 'relative',
                width: '100%',
                height: 'var(--tg-height, 100dvh)',
                overflow: 'hidden',
                background: '#000',
            }}
        >
            {/* Фоновое видео — абсолютное, на весь контейнер */}
            <video
                src={bgVideo}
                autoPlay
                loop
                muted
                playsInline
                style={{
                    position: 'absolute',
                    inset: 0,
                    width: '100%',
                    height: '100%',
                    objectFit: 'cover',
                    zIndex: 0,
                    pointerEvents: 'none',
                }}
            />

            {/* Затемнение */}
            <div
                style={{
                    position: 'absolute',
                    inset: 0,
                    background: 'rgba(0,0,0,0.45)',
                    zIndex: 1,
                    pointerEvents: 'none',
                }}
            />

            {/* Основной flex-контейнер */}
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
                {/* Хедер — фиксированная высота, не сжимается */}
                <div style={{ flexShrink: 0 }}>
                    <Header />
                </div>

                {/* Скролл-область — занимает всё свободное место */}
                <main
                    style={{
                        flex: 1,
                        overflowY: 'auto',
                        overflowX: 'hidden',
                        // Отступ снизу = навбар + safe area
                        paddingBottom: 'calc(88px + env(safe-area-inset-bottom, 0px))',
                        paddingLeft: '1rem',
                        paddingRight: '1rem',
                        // Momentum scroll на iOS
                        WebkitOverflowScrolling: 'touch',
                        overscrollBehavior: 'contain',
                        // Минимум — чтобы flex не схлопнул
                        minHeight: 0,
                    } as React.CSSProperties}
                    className="custom-scrollbar"
                >
                    {activeTab === 'home'          && <Home />}
                    {activeTab === 'profile'       && <Profile />}
                    {activeTab === 'payments'      && <Payments />}
                    {activeTab === 'subscriptions' && <Subscriptions />}
                    {activeTab === 'leaderboard'   && <Leaderboard />}
                </main>

                {/* Навбар — фиксированная высота, не сжимается */}
                <div style={{ flexShrink: 0 }}>
                    <BottomNav />
                </div>
            </div>
        </div>
    );
}