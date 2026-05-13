import React from 'react';
import { motion } from 'framer-motion';
import {
    Smartphone, Apple, Monitor, Laptop, Terminal, HelpCircle, X
} from 'lucide-react';
import { DeviceType } from '../../types/api';

interface DeviceSelectorProps {
    onSelect: (type: DeviceType) => void;
    onClose: () => void;
}

const DEVICES: { type: DeviceType; label: string; icon: any; color: string }[] = [
    { type: 'IOS',     label: 'iPhone / iPad', icon: Apple,      color: 'text-white' },
    { type: 'ANDROID', label: 'Android',       icon: Smartphone, color: 'text-emerald-400' },
    { type: 'WINDOWS', label: 'Windows PC',    icon: Monitor,    color: 'text-blue-400' },
    { type: 'MACOS',   label: 'MacBook / iMac',icon: Laptop,     color: 'text-purple-400' },
    { type: 'LINUX',   label: 'Linux',         icon: Terminal,   color: 'text-orange-400' },
    { type: 'OTHER',   label: 'Другое',        icon: HelpCircle, color: 'text-gray-400' },
];

export default function DeviceSelector({ onSelect, onClose }: DeviceSelectorProps) {
    return (
        <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[100] flex items-end justify-center bg-black/80 backdrop-blur-sm p-4"
        >
            <motion.div
                initial={{ y: "100%" }}
                animate={{ y: 0 }}
                exit={{ y: "100%" }}
                transition={{ type: 'spring', damping: 25, stiffness: 300 }}
                className="w-full max-w-md bg-[#12141d] border border-white/10 rounded-[2.5rem] p-6 pb-10 shadow-2xl"
            >
                <div className="flex justify-between items-center mb-6">
                    <h3 className="text-[20px] font-black text-white uppercase tracking-tight">Твое устройство</h3>
                    <button onClick={onClose} className="w-8 h-8 bg-white/5 rounded-full flex items-center justify-center">
                        <X size={18} className="text-white/40" />
                    </button>
                </div>

                <div className="grid grid-cols-2 gap-3">
                    {DEVICES.map((dev) => (
                        <button
                            key={dev.type}
                            onClick={() => onSelect(dev.type)}
                            className="flex flex-col items-center gap-3 p-5 rounded-3xl bg-white/5 border border-white/5 active:bg-white/10 active:scale-95 transition-all"
                        >
                            <dev.icon size={32} className={dev.color} />
                            <span className="text-[12px] font-black text-white/80 uppercase tracking-tighter text-center">
                                {dev.label}
                            </span>
                        </button>
                    ))}
                </div>

                <p className="text-center text-white/30 text-[10px] mt-6 font-medium leading-relaxed px-4">
                    Мы подготовим оптимальную конфигурацию именно под твою платформу
                </p>
            </motion.div>
        </motion.div>
    );
}