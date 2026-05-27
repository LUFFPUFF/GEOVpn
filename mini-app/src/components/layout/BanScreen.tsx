import React from 'react';

interface BanScreenProps {
    supportLink: string;
}

export default function BanScreen({ supportLink }: BanScreenProps) {
    return (
        <div
            style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                height: '100%',
                padding: '2rem',
                textAlign: 'center',
                zIndex: 10,
            }}
        >
            <div style={{ fontSize: '4.5rem', marginBottom: '1.5rem', filter: 'drop-shadow(0 0 10px rgba(255,77,77,0.3))' }}>
                ⛔
            </div>
            <h1 style={{
                fontSize: '1.6rem',
                fontWeight: 'bold',
                color: '#ff4d4d',
                marginBottom: '1rem',
                letterSpacing: '0.5px'
            }}>
                ДОСТУП ОГРАНИЧЕН
            </h1>
            <p style={{
                fontSize: '0.95rem',
                color: '#e0e0e0',
                lineHeight: '1.5',
                marginBottom: '2rem',
                maxWidth: '280px'
            }}>
                Ваш аккаунт был приостановлен из-за подозрительной активности или нарушения правил пользования сервисом.
            </p>
            <a
                href={supportLink}
                target="_blank"
                rel="noopener noreferrer"
                style={{
                    background: 'linear-gradient(135deg, #ff4d4d, #d32f2f)',
                    color: '#fff',
                    padding: '14px 28px',
                    borderRadius: '10px',
                    textDecoration: 'none',
                    fontWeight: 'bold',
                    fontSize: '0.95rem',
                    boxShadow: '0 4px 15px rgba(255, 77, 77, 0.3)',
                    transition: 'transform 0.1s ease',
                    display: 'inline-block'
                }}
            >
                Написать в поддержку
            </a>
        </div>
    );
}