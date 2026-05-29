import React, { useEffect, useState, Suspense, lazy } from 'react';
import './index.css';

import { useTelegram } from './hooks/useTelegram';
import { useUserStore } from './store/userStore';

import Header from './components/layout/Header';
import BottomNav from './components/layout/BottomNav';
import BanScreen from './components/layout/BanScreen';
import SubscriptionGuard from './components/guards/SubscriptionGuard';

import { BANNED_TELEGRAM_IDS, SUPPORT_LINK } from './bannedUsers';
import bgVideo from './assets/fon/video10.mp4';

const Home = lazy(() => import('./pages/Home'));
const Profile = lazy(() => import('./pages/Profile'));
const Payments = lazy(() => import('./pages/Payments'));
const Deposit = lazy(() => import('./pages/Deposit/Deposit'));
const Subscriptions = lazy(() => import('./pages/Subscriptions'));
const Leaderboard = lazy(() => import('./pages/Leaderboard'));
const ManageSubscription = lazy(() => import('./pages/ManageSubscription/ManageSubscription'));

export default function App() {
    const { expand, tg } = useTelegram();
    const { activeTab, fetchAll, user } = useUserStore();

    const [tgReady, setTgReady] = useState(false);
    const [showWarning, setShowWarning] = useState(false);
    const [dontShowAgain, setDontShowAgain] = useState(false);

    const isDev = import.meta.env.DEV;
    const currentTgId = tg?.initDataUnsafe?.user?.id;
    const isStaticBanned = currentTgId ? BANNED_TELEGRAM_IDS.includes(Number(currentTgId)) : false;
    const isDynamicBanned = user?.isBanned === true || user?.banned === true;
    const isBanned = isStaticBanned || isDynamicBanned;

    useEffect(() => {
        if (isBanned) return;
        const isHidden = localStorage.getItem('hide_anti_glush_warning');
        if (!isHidden) {
            setShowWarning(true);
        }
    }, [isBanned]);

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

        const checkUserInitialization = () => {
            const currentTgId = tg?.initDataUnsafe?.user?.id;
            if (currentTgId || isDev) {
                setTgReady(true);
            } else {
                setTimeout(checkUserInitialization, 100);
            }
        };
        checkUserInitialization();

        return () => tg.offEvent('viewportChanged', setHeight);
    }, [tg]);

    useEffect(() => {
        if (!tgReady) return;
        fetchAll();
    }, [tgReady]);

    const handleCloseWarning = () => {
        if (dontShowAgain) {
            localStorage.setItem('hide_anti_glush_warning', 'true');
        }
        setShowWarning(false);
    };

    const PageSkeleton = () => (
        <div className="w-full h-48 bg-white/5 rounded-2xl animate-pulse flex flex-col p-6 justify-between border border-white/5">
            <div className="h-6 bg-white/10 rounded w-1/3" />
            <div className="space-y-2">
                <div className="h-4 bg-white/10 rounded w-full" />
                <div className="h-4 bg-white/10 rounded w-5/6" />
            </div>
        </div>
    );

    const renderActivePage = () => (
        <Suspense fallback={<PageSkeleton />}>
            {activeTab === 'home' && <Home />}
            {activeTab === 'profile' && <Profile />}
            {activeTab === 'payments' && <Payments />}
            {activeTab === 'deposit' && <Deposit />}
            {activeTab === 'subscriptions' && <Subscriptions />}
            {activeTab === 'leaderboard' && <Leaderboard />}
            {activeTab === 'manage_subscription' && <ManageSubscription />}
        </Suspense>
    );

    const renderWarningModal = () => {
        if (!showWarning) return null;
        return (
            <div style={STYLES.modalOverlay}>
                <div style={STYLES.modalContainer}>
                    <div className="text-5xl text-[#ff9800] mb-4">⚠️</div>
                    <h2 className="text-xl font-bold mb-4 text-[#ff9800] tracking-wide uppercase">
                        Важное правило пользования
                    </h2>
                    <p className="text-sm leading-relaxed text-gray-300 mb-6">
                        Пожалуйста, не используйте <strong>Антиглушилки при подключении к домашнему Wi-Fi</strong>.
                        Включайте их только на мобильном интернете во время реальных блокировок операторов.
                        <br /><br />
                        В противном случае доступ к Антиглушилке будет приостановлен до выяснения причин.
                    </p>
                    <label style={STYLES.checkboxLabel}>
                        <input
                            type="checkbox"
                            checked={dontShowAgain}
                            onChange={(e) => setDontShowAgain(e.target.checked)}
                            className="accent-[#ff9800] w-4 h-4 cursor-pointer"
                        />
                        Больше не показывать предупреждение
                    </label>
                    <button
                        onClick={handleCloseWarning}
                        style={STYLES.modalButton}
                        className="active:scale-[0.98] transition-transform duration-75"
                    >
                        Я ознакомился
                    </button>
                </div>
            </div>
        );
    };

    const renderAppContent = () => {
        if (isBanned) {
            return <BanScreen supportLink={SUPPORT_LINK} reason={user?.banReason} />;
        }
        return (
            <>
                <main style={STYLES.mainContainer} className="custom-scrollbar">
                    {renderActivePage()}
                </main>
                <div style={STYLES.flexShrinkZero}>
                    <BottomNav />
                </div>
                {renderWarningModal()}
            </>
        );
    };

    return (
        <div style={STYLES.appWrapper}>
            <video
                src={bgVideo}
                autoPlay loop muted playsInline
                style={STYLES.videoBackground}
            />
            <div style={STYLES.videoOverlay} />

            <div style={STYLES.contentLayout}>
                <div style={STYLES.flexShrinkZero}>
                    <Header />
                </div>
                {isDev ? renderAppContent() : (
                    <SubscriptionGuard>
                        {renderAppContent()}
                    </SubscriptionGuard>
                )}
            </div>
        </div>
    );
}

const STYLES = {
    appWrapper: {
        position: 'relative',
        width: '100%',
        height: 'var(--tg-height, 100dvh)',
        overflow: 'hidden',
        background: '#000',
    } as React.CSSProperties,

    videoBackground: {
        position: 'absolute',
        inset: 0,
        width: '100%',
        height: '100%',
        objectFit: 'cover',
        zIndex: 0,
        pointerEvents: 'none',
    } as React.CSSProperties,

    videoOverlay: {
        position: 'absolute',
        inset: 0,
        background: 'rgba(0, 0, 0, 0.45)',
        zIndex: 1,
        pointerEvents: 'none',
    } as React.CSSProperties,

    contentLayout: {
        position: 'relative',
        zIndex: 2,
        display: 'flex',
        flexDirection: 'column',
        width: '100%',
        height: '100%',
        color: 'white',
    } as React.CSSProperties,

    mainContainer: {
        flex: 1,
        overflowY: 'auto',
        overflowX: 'hidden',
        paddingBottom: 'calc(88px + env(safe-area-inset-bottom, 0px))',
        paddingLeft: '1rem',
        paddingRight: '1rem',
        WebkitOverflowScrolling: 'touch',
        overscrollBehavior: 'contain',
        minHeight: 0,
    } as React.CSSProperties,

    flexShrinkZero: {
        flexShrink: 0,
    } as React.CSSProperties,

    modalOverlay: {
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
    } as React.CSSProperties,

    modalContainer: {
        background: 'rgba(20, 20, 20, 0.95)',
        border: '1px solid rgba(255, 152, 0, 0.3)',
        borderRadius: '16px',
        padding: '1.8rem',
        maxWidth: '400px',
        width: '100%',
        color: '#fff',
        textAlign: 'center',
        boxShadow: '0 8px 32px rgba(0, 0, 0, 0.5)',
    } as React.CSSProperties,

    checkboxLabel: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '8px',
        marginBottom: '1.5rem',
        cursor: 'pointer',
        fontSize: '0.85rem',
        color: '#aaa',
        userSelect: 'none',
    } as React.CSSProperties,

    modalButton: {
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
    } as React.CSSProperties,
};