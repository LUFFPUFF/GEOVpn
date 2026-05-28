import React from 'react';

interface BanScreenProps {
    supportLink: string;
    reason?: string;
}

export default function BanScreen({ supportLink, reason }: BanScreenProps) {
    const getReadableReason = (rawReason?: string) => {
        if (!rawReason) return "Подозрительная активность или нарушение правил";
        switch (rawReason) {
            case "TORRENT_PROTOCOL_DETECTED":
                return "Использование P2P/Торрентов на серверах Антиглушилок (запрещено правилами)";
            case "TRAFFIC_SPIKE_ABUSE":
                return "Аномально высокое потребление трафика за короткий промежуток времени";
            case "WEEKLY_RELATIVE_OVERCONSUMPTION":
                return "Превышение среднего лимита потребления трафика сети за неделю";
            case "MANUAL_ADMIN_BAN":
                return "Доступ временно ограничен администратором сервиса";
            default:
                return rawReason;
        }
    };

    const getErrorCode = (rawReason?: string) => {
        if (!rawReason) return "ERR_SEC_POL_VIOLATION";
        return "ERR_SEC_" + rawReason.toUpperCase();
    };

    const inlineStyles = `
        @keyframes fadeInUp {
            from {
                opacity: 0;
                transform: translateY(30px) scale(0.96);
            }
            to {
                opacity: 1;
                transform: translateY(0) scale(1);
            }
        }
        @keyframes pulseGlow {
            0%, 100% {
                box-shadow: 0 0 20px rgba(255, 42, 84, 0.2), inset 0 0 15px rgba(255, 42, 84, 0.1);
                border-color: rgba(255, 42, 84, 0.3);
            }
            50% {
                box-shadow: 0 0 40px rgba(255, 42, 84, 0.4), inset 0 0 25px rgba(255, 42, 84, 0.2);
                border-color: rgba(255, 42, 84, 0.6);
            }
        }
        @keyframes cyberScan {
            0% { background-position: 0% 0%; }
            100% { background-position: 0% 100%; }
        }
        .cyber-card {
            animation: fadeInUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
        }
        .glowing-badge {
            animation: pulseGlow 3s infinite ease-in-out;
        }
        .premium-btn:hover {
            filter: brightness(1.2);
            box-shadow: 0 0 30px rgba(255, 42, 84, 0.5);
            transform: translateY(-2px);
        }
        .premium-btn:active {
            transform: translateY(0);
        }
    `;

    return (
        <div
            style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                height: '100%',
                padding: '1.5rem',
                textAlign: 'center',
                zIndex: 10,
                fontFamily: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
            }}
        >
            <style dangerouslySetInnerHTML={{ __html: inlineStyles }} />

            <div
                className="cyber-card"
                style={{
                    background: 'linear-gradient(135deg, rgba(18, 11, 14, 0.9) 0%, rgba(9, 7, 8, 0.95) 100%)',
                    backdropFilter: 'blur(30px)',
                    WebkitBackdropFilter: 'blur(30px)',
                    border: '1px solid rgba(255, 42, 84, 0.2)',
                    boxShadow: '0 30px 70px rgba(0, 0, 0, 0.8), 0 0 50px rgba(255, 42, 84, 0.05), inset 0 1px 0 rgba(255,255,255,0.05)',
                    borderRadius: '28px',
                    padding: '3rem 2rem 2.5rem 2rem',
                    maxWidth: '340px',
                    width: '90%',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    position: 'relative',
                    overflow: 'hidden'
                }}
            >
                <div
                    style={{
                        position: 'absolute',
                        inset: 0,
                        backgroundImage: 'linear-gradient(rgba(255, 42, 84, 0.03) 1px, transparent 1px)',
                        backgroundSize: '100% 4px',
                        pointerEvents: 'none',
                        zIndex: 1
                    }}
                />

                <div
                    className="glowing-badge"
                    style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        width: '84px',
                        height: '84px',
                        borderRadius: '50%',
                        background: 'rgba(255, 42, 84, 0.04)',
                        border: '1px solid rgba(255, 42, 84, 0.3)',
                        transition: 'all 0.3s ease',
                        marginBottom: '1.75rem',
                        zIndex: 2
                    }}
                >
                    <svg width="42" height="42" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" style={{ filter: 'drop-shadow(0 0 8px rgba(255,42,84,0.6))' }}>
                        <path d="M12 22C12 22 20 18 20 12V5L12 2L4 5V12C4 18 12 22 12 22Z" stroke="#FF2A54" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                        <path d="M12 8V13" stroke="#FF2A54" strokeWidth="2" strokeLinecap="round" />
                        <circle cx="12" cy="16" r="1" fill="#FF2A54" />
                    </svg>
                </div>

                <h1 style={{
                    fontSize: '1.45rem',
                    fontWeight: 900,
                    color: '#ffffff',
                    margin: '0 0 0.5rem 0',
                    letterSpacing: '1.5px',
                    textTransform: 'uppercase',
                    zIndex: 2
                }}>
                    ДОСТУП ОГРАНИЧЕН
                </h1>

                <div style={{
                    fontSize: '0.65rem',
                    fontFamily: 'monospace',
                    color: '#FF2A54',
                    background: 'rgba(255, 42, 84, 0.1)',
                    padding: '2px 8px',
                    borderRadius: '4px',
                    marginBottom: '1.25rem',
                    letterSpacing: '1px',
                    fontWeight: 'bold',
                    border: '1px solid rgba(255, 42, 84, 0.15)',
                    zIndex: 2
                }}>
                    {getErrorCode(reason)}
                </div>

                <p style={{
                    fontSize: '0.85rem',
                    color: '#a0a0a5',
                    lineHeight: '1.6',
                    margin: '0 0 2rem 0',
                    maxWidth: '260px',
                    zIndex: 2
                }}>
                    Ваше подключение временно заблокировано системой предотвращения злоупотреблений.
                </p>

                <div style={{
                    background: 'rgba(0, 0, 0, 0.5)',
                    border: '1px solid rgba(255, 42, 84, 0.15)',
                    borderRadius: '14px',
                    padding: '16px',
                    fontSize: '0.8rem',
                    color: '#e2e2e9',
                    width: '100%',
                    boxSizing: 'border-box',
                    textAlign: 'left',
                    lineHeight: '1.5',
                    marginBottom: '2.5rem',
                    zIndex: 2,
                    boxShadow: 'inset 0 0 20px rgba(0, 0, 0, 0.6)'
                }}>
                    <div style={{
                        fontSize: '0.65rem',
                        textTransform: 'uppercase',
                        color: '#FF2A54',
                        fontWeight: 'bold',
                        letterSpacing: '1.5px',
                        marginBottom: '8px',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '6px'
                    }}>
                        <span style={{ display: 'inline-block', width: '6px', height: '6px', background: '#FF2A54', borderRadius: '50%' }} />
                        Нарушение протокола безопасности:
                    </div>
                    <span style={{ color: '#ffffff', fontWeight: '500' }}>
                        {getReadableReason(reason)}
                    </span>
                </div>

                <a
                    href={supportLink}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="premium-btn"
                    style={{
                        background: 'linear-gradient(135deg, #FF2A54 0%, #9B001C 100%)',
                        color: '#fff',
                        padding: '16px 32px',
                        borderRadius: '16px',
                        textDecoration: 'none',
                        fontWeight: 'bold',
                        fontSize: '0.9rem',
                        width: '100%',
                        boxSizing: 'border-box',
                        boxShadow: '0 8px 24px rgba(255, 42, 84, 0.3), inset 0 -3px 0 rgba(0,0,0,0.25)',
                        transition: 'all 0.2s cubic-bezier(0.16, 1, 0.3, 1)',
                        display: 'inline-block',
                        letterSpacing: '0.5px',
                        zIndex: 2
                    }}
                >
                    Апелляция и поддержка
                </a>
            </div>
        </div>
    );
}