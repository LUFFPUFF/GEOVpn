import React, { useState, useEffect } from 'react';
import './index.css';
import { useTelegram } from './hooks/useTelegram';
import BottomNav from './components/layout/BottomNav';
import Home from './pages/Home';

export default function App() {
    const [activeTab, setActiveTab] = useState('home');
    const { expand, tg } = useTelegram();

    useEffect(() => {
        if (tg) {
            expand();
            if (tg.setHeaderColor) tg.setHeaderColor('#000000');
        }
    }, [tg, expand]);

    return (
        // Здесь БОЛЬШЕ НЕТ фоновых слоев и bg-black, только контент
        <div className="min-h-screen text-white relative flex flex-col">

            {/* HEADER */}
            <header className="px-8 pt-14 pb-10 flex justify-between items-center relative z-10">
                <div className="flex flex-col">
                    <div className="flex items-center gap-2 text-white">
                        <div className="w-2 h-2 bg-white rounded-full shadow-[0_0_8px_#fff]" />
                        <h1 className="text-2xl font-black tracking-tighter uppercase italic">
                            GEO<span className="opacity-40 font-light">VPN</span>
                        </h1>
                    </div>
                    <p className="text-[9px] font-bold text-white/30 uppercase tracking-[0.4em] mt-1.5 ml-4">
                        Encryption Standard
                    </p>
                </div>

                <div className="text-[10px] font-bold text-white/30 uppercase tracking-widest border border-white/10 px-3 py-1 rounded-full backdrop-blur-md">
                    v1.0.4
                </div>
            </header>

            <main className="relative z-10 flex-grow">
                {activeTab === 'home' && <Home />}
            </main>

            <BottomNav activeTab={activeTab} setActiveTab={setActiveTab} />
        </div>
    );
}