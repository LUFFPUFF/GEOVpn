import React from 'react';
export default function Card({ children, className = "" }) {
    return (
        <div className={`bg-[#0f0f12] border border-white/5 rounded-[24px] p-5 shadow-xl ${className}`}>
            {children}
        </div>
    );
}