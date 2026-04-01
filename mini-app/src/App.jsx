// src/App.jsx
import React, { useEffect } from 'react';
import { useUserStore } from './store/userStore';

import Home from './pages/Home';
import Subscriptions from './pages/Subscriptions'; // Заменил Access/Nodes на Subscriptions
import Payments from './pages/Payments';
import Profile from './pages/Profile';
import BottomNav from './components/layout/BottomNav';
import Header from './components/layout/Header';

export default function App() {
    const { initApp, loading, activeTab, setActiveTab } = useUserStore();

    useEffect(() => {
        initApp();
    }, []);

    if (loading) {
        return (
            <div className="h-screen flex items-center justify-center">
                <div className="w-8 h-8 border-4 border-[#1c1c1e] border-t-white rounded-full animate-spin" />
            </div>
        );
    }

    return (
        <div className="min-h-screen text-white relative">
            <Header />
            <main className="pb-24 px-4">
                {activeTab === 'home' && <Home />}
                {activeTab === 'subscriptions' && <Subscriptions />}
                {activeTab === 'payments' && <Payments />}
                {activeTab === 'profile' && <Profile />}
            </main>
            <BottomNav activeTab={activeTab} setActiveTab={setActiveTab} />
        </div>
    );
}