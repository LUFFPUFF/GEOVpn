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
    const { activeTab, fetchAll } = useUserStore();
    const [tgReady, setTgReady] = useState(false);

    // Состояния для окна предупреждения
    const [showWarning, setShowWarning] = useState(false);
    const [dontShowAgain, setDontShowAgain] = useState(false);

    const isDev = import.meta.env.DEV;

    // Проверка localStorage при загрузке приложения
    useEffect(() => {
        const isHidden = localStorage.getItem('hide_anti_glush_warning');
        if (!isHidden) {
            setShowWarning(true);
        }
    }, []);

    const handleCloseWarning = () => {
        if (dontShowAgain) {
            localStorage.setItem('hide_anti_glush_warning', 'true');
        }
        setShowWarning(false);
    };

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

    const MainContent = (
        <>
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
        </>
    );

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

                {isDev ? (
                    MainContent
                ) : (
                    <SubscriptionGuard>
                        {MainContent}
                    </SubscriptionGuard>
                )}
            </div>

            {/* МОДАЛЬНОЕ ОКНО ПРЕДУПРЕЖДЕНИЯ */}
            {showWarning && (
                <div
                    style={{
                        position: 'fixed',
                        inset: 0,
                        backgroundColor: 'rgba(0, 0, 0, 0.85)',
                        backdropFilter: 'blur(8px)',
                        WebkitBackdropFilter: 'blur(8px)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        zIndex: 9999,
                        padding: '1.5rem',
                    }}
                >
                    <div
                        style={{
                            background: 'rgba(20, 20, 20, 0.95)',
                            border: '1px solid rgba(255, 152, 0, 0.3)',
                            borderRadius: '16px',
                            padding: '1.8rem',
                            maxWidth: '400px',
                            width: '100%',
                            color: '#fff',
                            textAlign: 'center',
                            boxShadow: '0 8px 32px rgba(0, 0, 0, 0.5)',
                        }}
                    >
                        {/* Иконка предупреждения */}
                        <div style={{ fontSize: '3rem', color: '#ff9800', marginBottom: '1rem' }}>⚠️</div>

                        <h2 style={{ fontSize: '1.25rem', fontWeight: 'bold', marginBottom: '1rem', color: '#ff9800', letterSpacing: '0.5px' }}>
                            ВАЖНОЕ ПРАВИЛО ПОЛЬЗОВАНИЯ
                        </h2>

                        <p style={{ fontSize: '0.9rem', lineHeight: '1.4', color: '#e0e0e0', marginBottom: '1.5rem' }}>
                            Пожалуйста, не используйте <strong>Антиглушилки при подключении к домашнему Wi-Fi</strong>.
                            Включайте их только на мобильном интернете во время реальных блокировок операторов.
                            <br /><br />
                            В противном случае доступ к Антиглушилке будет приостановлен до выяснения причин.
                        </p>

                        {/* Чекбокс "Больше не показывать" */}
                        <label
                            style={{
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                gap: '8px',
                                marginBottom: '1.5rem',
                                cursor: 'pointer',
                                fontSize: '0.85rem',
                                color: '#aaa',
                                userSelect: 'none',
                            }}
                        >
                            <input
                                type="checkbox"
                                checked={dontShowAgain}
                                onChange={(e) => setDontShowAgain(e.target.checked)}
                                style={{
                                    accentColor: '#ff9800',
                                    width: '16px',
                                    height: '16px',
                                    cursor: 'pointer',
                                }}
                            />
                            Больше не показывать предупреждение
                        </label>

                        {/* Кнопка закрытия */}
                        <button
                            onClick={handleCloseWarning}
                            style={{
                                background: 'linear-gradient(135deg, #ff9800, #f57c00)',
                                border: 'none',
                                borderRadius: '8px',
                                color: '#fff',
                                padding: '12px 24px',
                                width: '100%',
                                fontWeight: 'bold',
                                fontSize: '0.95rem',
                                cursor: 'pointer',
                                boxShadow: '0 4px 12px rgba(255, 152, 0, 0.2)',
                                transition: 'transform 0.1s ease',
                            }}
                            onMouseDown={(e) => (e.currentTarget.style.transform = 'scale(0.98)')}
                            onMouseUp={(e) => (e.currentTarget.style.transform = 'scale(1)')}
                        >
                            Я ознакомился
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
}