import { useState, useMemo } from "react"

// --- ИКОНКИ ---
const Icons = {
    TrendingUp: () => <svg width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/></svg>,
    TrendingDown: () => <svg width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="23 18 13.5 8.5 8.5 13.5 1 6"/><polyline points="17 18 23 18 23 12"/></svg>,
    Download: () => <svg width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>,
    Server: () => <svg width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="2" y="2" width="20" height="8" rx="2" ry="2"/><rect x="2" y="14" width="20" height="8" rx="2" ry="2"/><line x1="6" y1="6" x2="6.01" y2="6"/><line x1="6" y1="18" x2="6.01" y2="18"/></svg>,
    Database: () => <svg width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3"/><path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"/></svg>,
    Shield: () => <svg width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>,
}

// Кастомный график "Доходы vs Расходы" (Bar Chart)
function PnLChart({ data }: { data: { month: string, rev: number, exp: number }[] }) {
    const W = 400, H = 160;
    const maxVal = Math.max(...data.map(d => Math.max(d.rev, d.exp))) * 1.1;
    const barW = 24;
    const gap = 8;
    const step = W / data.length;

    return (
        <svg width="100%" height="100%" viewBox={`0 0 ${W} ${H}`} style={{ overflow: "visible" }}>
            {data.map((d, i) => {
                const x = i * step + step / 2 - barW - gap / 2;
                const hRev = (d.rev / maxVal) * H;
                const hExp = (d.exp / maxVal) * H;
                return (
                    <g key={i}>
                        {/* Столбец Доходов */}
                        <rect x={x} y={H - hRev} width={barW} height={hRev} fill="#10b981" rx="4" />
                        {/* Столбец Расходов */}
                        <rect x={x + barW + gap} y={H - hExp} width={barW} height={hExp} fill="#ef4444" rx="4" />
                        {/* Подпись месяца */}
                        <text x={x + barW + gap / 2} y={H + 20} fontSize="11" fill="#64748b" textAnchor="middle" fontFamily="Inter">{d.month}</text>
                    </g>
                );
            })}
        </svg>
    )
}

export default function FinOpsPanel() {
    const [infraMode, setInfraMode] = useState<"SIMPLE"|"STABLE">("SIMPLE");

    // Входные данные (Константы из твоих требований)
    const CONSTANTS = {
        backendCost: 4000,
        ruServerCost: 1600,
        euServerSimple: 593,
        euServerStable: 1187,
        euLocations: [
            { id: "FI", name: "Финляндия (Aeza)", flag: "🇫🇮" },
            { id: "FR", name: "Франция (Aeza)", flag: "🇫🇷" },
            { id: "NL", name: "Нидерланды (Aeza)", flag: "🇳🇱" },
            { id: "LV", name: "Латвия (Aeza)", flag: "🇱🇻" },
        ]
    };

    // Текущие бизнес-метрики (Live)
    const activeUsers = 1245;
    const grossRevenue = 45231.50; // Грязная выручка за месяц от юзеров
    const arpu = grossRevenue / activeUsers; // Средний чек

    // Вычисления Бухучета
    const euCostPerServer = infraMode === "SIMPLE" ? CONSTANTS.euServerSimple : CONSTANTS.euServerStable;
    const totalEuCost = euCostPerServer * CONSTANTS.euLocations.length;

    const totalExpenses = totalEuCost + CONSTANTS.backendCost + CONSTANTS.ruServerCost;
    const netProfit = grossRevenue - totalExpenses;
    const profitMargin = (netProfit / grossRevenue) * 100;

    // Unit-экономика
    const costPerUser = totalExpenses / activeUsers;
    const breakEvenUsers = Math.ceil(totalExpenses / arpu); // Сколько юзеров нужно для окупаемости

    // История для графика P&L
    const chartData = [
        { month: "Янв", rev: 28000, exp: 7972 },
        { month: "Фев", rev: 34500, exp: 7972 },
        { month: "Мар", rev: 41200, exp: 7972 },
        { month: "Апр", rev: 45231, exp: totalExpenses }, // Текущий
    ];

    // MOCK Данные для Эксель-таблицы (Журнал операций)
    const ledgerData = [
        { id: "TRX-9942", date: "2024-04-15 10:22", category: "Доход", desc: "Пополнение баланса: User #85844", amount: 1500, status: "SUCCESS" },
        { id: "TRX-9941", date: "2024-04-15 09:14", category: "Доход", desc: "Списание TrafficCollector (1.2 TB)", amount: 3450, status: "SUCCESS" },
        { id: "INV-AEZA-04", date: "2024-04-01 00:01", category: "Расход", desc: "Оплата VPS: NL, FR, FI, LV (Aeza)", amount: -totalEuCost, status: "PAID" },
        { id: "INV-YNDX-04", date: "2024-04-01 00:01", category: "Расход", desc: "Оплата: Центральный Бэкенд + БД (Selectel)", amount: -4000, status: "PAID" },
        { id: "INV-ANTI-04", date: "2024-04-01 00:01", category: "Расход", desc: "Оплата: RU Прокси Антиглуш (Yandex Cloud)", amount: -1600, status: "PAID" },
        { id: "TRX-9820", date: "2024-03-29 18:40", category: "Доход", desc: "Оплата тарифа: User #11294", amount: 500, status: "SUCCESS" },
    ];

    // Экспорт в CSV (Excel)
    const exportToCSV = () => {
        // Добавляем BOM (\uFEFF) чтобы Excel корректно читал русский язык (UTF-8)
        const csvContent = "\uFEFF" +
            "ID Операции;Дата;Категория;Описание;Сумма (RUB);Статус\n" +
            ledgerData.map(row => `${row.id};${row.date};${row.category};${row.desc};${row.amount};${row.status}`).join("\n");

        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement("a");
        const url = URL.createObjectURL(blob);
        link.setAttribute("href", url);
        link.setAttribute("download", `GeoVPN_FinOps_${new Date().toISOString().split('T')[0]}.csv`);
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    };

    return (
        <div style={{ minHeight:"100vh", background:"#f4f7fb", fontFamily:"'Inter',-apple-system,sans-serif", color:"#0f172a", padding:"40px" }}>
            <style>{`
                @import url('https://fonts.googleapis.com/css2?family=DM+Mono:wght@400;500;600&family=Space+Grotesk:wght@500;700;800&family=Inter:wght@400;500;600;700&display=swap');
                * { box-sizing:border-box; margin:0; padding:0; }
                
                .card { background: #ffffff; border: 1px solid #e2e8f0; border-radius: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.02); transition: all 0.2s; }
                .card:hover { box-shadow: 0 10px 20px -5px rgba(0,0,0,0.05); }
                
                .btn-primary { display:inline-flex; align-items:center; gap:8px; padding:10px 18px; background:linear-gradient(135deg, #10b981, #059669); border:none; border-radius:10px; color:white; font-size:13px; font-weight:700; cursor:pointer; transition:all 0.2s; box-shadow:0 4px 10px -2px rgba(16,185,129,0.3); }
                .btn-primary:hover { transform:translateY(-1px); box-shadow:0 6px 14px -2px rgba(16,185,129,0.4); }
                
                .switch-btn { padding:10px 20px; font-size:13px; font-weight:700; cursor:pointer; transition:all 0.2s; border:none; }
                .switch-active { background:#0f172a; color:#ffffff; box-shadow:0 2px 4px rgba(0,0,0,0.1); }
                .switch-inactive { background:transparent; color:#64748b; }
                .switch-inactive:hover { color:#0f172a; }

                /* Стили для таблицы (Ledger) */
                .ledger-table { width: 100%; border-collapse: collapse; text-align: left; }
                .ledger-table th { padding: 16px; font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.05em; border-bottom: 2px solid #e2e8f0; background: #f8fafc; }
                .ledger-table td { padding: 16px; font-size: 13px; color: #0f172a; border-bottom: 1px solid #f1f5f9; font-weight: 500; }
                .ledger-table tr:hover td { background: #f8fafc; }
                .ledger-badge { font-size: 11px; font-weight: 700; padding: 4px 10px; border-radius: 20px; }
            `}</style>

            {/* --- ШАПКА --- */}
            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"flex-start", marginBottom:"32px" }}>
                <div>
                    <h2 style={{ fontSize:36, fontWeight:800, fontFamily:"'Space Grotesk',sans-serif", letterSpacing:"-0.03em", marginBottom:6 }}>FinOps Бухучет</h2>
                    <p style={{ fontSize:15, color:"#64748b", fontWeight:500 }}>Финансовая аналитика, расходы на сервера и Unit-экономика</p>
                </div>
                <button className="btn-primary" onClick={exportToCSV}>
                    <Icons.Download /> Выгрузить реестр (Excel)
                </button>
            </div>

            {/* --- ПЕРЕКЛЮЧАТЕЛЬ ТАРИФОВ ИНФРАСТРУКТУРЫ --- */}
            <div style={{ display:"flex", justifyContent:"center", marginBottom:"32px" }}>
                <div style={{ background:"#e2e8f0", padding:4, borderRadius:12, display:"inline-flex", gap:4 }}>
                    <button className={`switch-btn ${infraMode === "SIMPLE" ? "switch-active" : "switch-inactive"}`} style={{ borderRadius:10 }} onClick={() => setInfraMode("SIMPLE")}>
                        Простая архитектура (до 2k юзеров)
                    </button>
                    <button className={`switch-btn ${infraMode === "STABLE" ? "switch-active" : "switch-inactive"}`} style={{ borderRadius:10 }} onClick={() => setInfraMode("STABLE")}>
                        Стабильная архитектура (PRO)
                    </button>
                </div>
            </div>

            {/* --- БЛОК 1: ГЛАВНЫЕ KPI --- */}
            <div style={{ display:"grid", gridTemplateColumns:"repeat(4, 1fr)", gap:"20px", marginBottom:"24px" }}>
                {[
                    { label: "Выручка (Gross)", val: grossRevenue, color: "#0f172a", icon: <Icons.TrendingUp /> },
                    { label: "Расходы (OPEX)", val: totalExpenses, color: "#ef4444", icon: <Icons.TrendingDown />, minus: true },
                    { label: "Чистая прибыль (Net)", val: netProfit, color: "#10b981", icon: <Icons.TrendingUp /> },
                    { label: "Рентабельность (Margin)", val: profitMargin, color: "#3b82f6", suffix: "%", format: (v:number)=>v.toFixed(1) },
                ].map((kpi, i) => (
                    <div key={i} className="card" style={{ padding:"24px", border: i === 2 ? "1px solid #10b981" : "1px solid #e2e8f0", background: i === 2 ? "#ecfdf5" : "#ffffff" }}>
                        <div style={{ fontSize:12, color: i===2 ? "#059669" : "#64748b", fontWeight:700, textTransform:"uppercase", letterSpacing:"0.05em", marginBottom:8, display:"flex", justifyContent:"space-between" }}>
                            <span>{kpi.label}</span>
                            <span style={{ color: kpi.minus ? "#ef4444" : "#10b981" }}>{kpi.icon}</span>
                        </div>
                        <div style={{ fontSize:32, fontWeight:800, color:kpi.color, fontFamily:"'Space Grotesk',sans-serif" }}>
                            {kpi.minus ? "- " : ""}{kpi.format ? kpi.format(kpi.val) : kpi.val.toLocaleString("ru-RU")}
                            <span style={{ fontSize:18, color: i===2 ? "#059669" : "#94a3b8", marginLeft:4 }}>{kpi.suffix || "₽"}</span>
                        </div>
                    </div>
                ))}
            </div>

            {/* --- БЛОК 2: СМЕТА ИНФРАСТРУКТУРЫ & UNIT-ЭКОНОМИКА --- */}
            <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:"24px", marginBottom:"32px" }}>

                {/* Лево: Смета Расходов (The Bill) */}
                <div className="card" style={{ padding:"28px" }}>
                    <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:"24px" }}>
                        <h3 style={{ fontSize:18, fontWeight:800, color:"#0f172a" }}>Смета инфраструктуры (в месяц)</h3>
                        <span style={{ fontSize:14, fontWeight:800, color:"#ef4444", background:"#fee2e2", padding:"6px 12px", borderRadius:10 }}>
                            Итого: -{totalExpenses.toLocaleString("ru-RU")} ₽
                        </span>
                    </div>

                    <div style={{ display:"flex", flexDirection:"column", gap:"16px" }}>
                        {/* EU Сервера */}
                        <div style={{ background:"#f8fafc", padding:"16px", borderRadius:"12px", border:"1px solid #f1f5f9" }}>
                            <div style={{ display:"flex", alignItems:"center", gap:8, fontSize:13, fontWeight:700, color:"#475569", textTransform:"uppercase", marginBottom:12 }}>
                                <Icons.Server /> Европейские узлы (VPN)
                            </div>
                            {CONSTANTS.euLocations.map((loc, i) => (
                                <div key={i} style={{ display:"flex", justifyContent:"space-between", padding:"6px 0", borderBottom: i === 3 ? "none" : "1px dashed #e2e8f0" }}>
                                    <span style={{ fontSize:14, fontWeight:600, color:"#0f172a" }}>{loc.flag} {loc.name}</span>
                                    <span style={{ fontSize:14, fontWeight:700, fontFamily:"'DM Mono',monospace", color:"#64748b" }}>{euCostPerServer} ₽</span>
                                </div>
                            ))}
                        </div>

                        {/* RU Сервер */}
                        <div style={{ background:"#f8fafc", padding:"16px", borderRadius:"12px", border:"1px solid #f1f5f9", display:"flex", justifyContent:"space-between", alignItems:"center" }}>
                            <div>
                                <div style={{ display:"flex", alignItems:"center", gap:8, fontSize:13, fontWeight:700, color:"#475569", textTransform:"uppercase", marginBottom:4 }}><Icons.Shield /> RU Антиглуш / Мост</div>
                                <div style={{ fontSize:13, color:"#64748b", fontWeight:500 }}>Yandex Cloud (Роутинг трафика РФ)</div>
                            </div>
                            <span style={{ fontSize:15, fontWeight:800, fontFamily:"'DM Mono',monospace", color:"#0f172a" }}>{CONSTANTS.ruServerCost} ₽</span>
                        </div>

                        {/* Backend */}
                        <div style={{ background:"#f8fafc", padding:"16px", borderRadius:"12px", border:"1px solid #f1f5f9", display:"flex", justifyContent:"space-between", alignItems:"center" }}>
                            <div>
                                <div style={{ display:"flex", alignItems:"center", gap:8, fontSize:13, fontWeight:700, color:"#475569", textTransform:"uppercase", marginBottom:4 }}><Icons.Database /> Ядро (Бэкенд + БД)</div>
                                <div style={{ fontSize:13, color:"#64748b", fontWeight:500 }}>Selectel / Yandex (2 CPU, 4 GB RAM)</div>
                            </div>
                            <span style={{ fontSize:15, fontWeight:800, fontFamily:"'DM Mono',monospace", color:"#0f172a" }}>{CONSTANTS.backendCost} ₽</span>
                        </div>
                    </div>
                </div>

                {/* Право: Unit-Экономика и График P&L */}
                <div style={{ display:"flex", flexDirection:"column", gap:"24px" }}>

                    {/* Unit Metrics */}
                    <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:"20px" }}>
                        <div className="card" style={{ padding:"20px", background:"linear-gradient(135deg, #eff6ff 0%, #ffffff 100%)", borderColor:"#bfdbfe" }}>
                            <div style={{ fontSize:12, color:"#2563eb", fontWeight:700, textTransform:"uppercase", marginBottom:8 }}>Cost per User (Содержание)</div>
                            <div style={{ fontSize:28, fontWeight:800, color:"#1d4ed8", fontFamily:"'DM Mono',monospace" }}>{costPerUser.toFixed(2)} ₽</div>
                            <div style={{ fontSize:12, color:"#64748b", marginTop:4, fontWeight:500 }}>Себестоимость 1 активного юзера</div>
                        </div>
                        <div className="card" style={{ padding:"20px", background:"linear-gradient(135deg, #fef3c7 0%, #ffffff 100%)", borderColor:"#fde68a" }}>
                            <div style={{ fontSize:12, color:"#d97706", fontWeight:700, textTransform:"uppercase", marginBottom:8 }}>Точка безубыточности</div>
                            <div style={{ fontSize:28, fontWeight:800, color:"#92400e", fontFamily:"'DM Mono',monospace" }}>{breakEvenUsers} <span style={{fontSize:14}}>юзеров</span></div>
                            <div style={{ fontSize:12, color:"#64748b", marginTop:4, fontWeight:500 }}>Чтобы окупить {totalExpenses} ₽/мес</div>
                        </div>
                    </div>

                    {/* P&L Chart */}
                    <div className="card" style={{ padding:"24px", flex:1, display:"flex", flexDirection:"column" }}>
                        <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:"20px" }}>
                            <h3 style={{ fontSize:16, fontWeight:800, color:"#0f172a" }}>Динамика P&L (Прибыль и убытки)</h3>
                            <div style={{ display:"flex", gap:12, fontSize:12, fontWeight:600 }}>
                                <span style={{ display:"flex", alignItems:"center", gap:6 }}><div style={{width:10, height:10, background:"#10b981", borderRadius:3}}/> Доходы</span>
                                <span style={{ display:"flex", alignItems:"center", gap:6 }}><div style={{width:10, height:10, background:"#ef4444", borderRadius:3}}/> Расходы</span>
                            </div>
                        </div>
                        <div style={{ flex:1, position:"relative", paddingBottom:"20px" }}>
                            <PnLChart data={chartData} />
                        </div>
                    </div>

                </div>
            </div>

            {/* --- БЛОК 3: ТАБЛИЦА ОПЕРАЦИЙ (LEDGER) --- */}
            <div className="card" style={{ overflow:"hidden" }}>
                <div style={{ padding:"24px", borderBottom:"1px solid #e2e8f0", display:"flex", justifyContent:"space-between", alignItems:"center" }}>
                    <h3 style={{ fontSize:18, fontWeight:800, color:"#0f172a" }}>Финансовый журнал (Ledger)</h3>
                    <span style={{ fontSize:13, color:"#64748b", fontWeight:600 }}>Последние 50 транзакций</span>
                </div>
                <div style={{ overflowX: "auto" }}>
                    <table className="ledger-table">
                        <thead>
                        <tr>
                            <th>ID Операции</th>
                            <th>Дата и Время</th>
                            <th>Категория</th>
                            <th>Описание</th>
                            <th style={{ textAlign: "right" }}>Сумма</th>
                            <th style={{ textAlign: "center" }}>Статус</th>
                        </tr>
                        </thead>
                        <tbody>
                        {ledgerData.map((row, i) => (
                            <tr key={i}>
                                <td style={{ fontFamily:"'DM Mono',monospace", color:"#64748b" }}>{row.id}</td>
                                <td>{row.date}</td>
                                <td>
                                    <span style={{ color: row.category === "Доход" ? "#059669" : "#dc2626", fontWeight:700 }}>{row.category}</span>
                                </td>
                                <td>{row.desc}</td>
                                <td style={{ textAlign: "right", fontFamily:"'DM Mono',monospace", fontWeight:700, color: row.amount > 0 ? "#10b981" : "#0f172a" }}>
                                    {row.amount > 0 ? "+" : ""}{row.amount.toLocaleString("ru-RU")} ₽
                                </td>
                                <td style={{ textAlign: "center" }}>
                                        <span className="ledger-badge" style={{
                                            background: row.status === "SUCCESS" || row.status === "PAID" ? "#d1fae5" : "#fee2e2",
                                            color: row.status === "SUCCESS" || row.status === "PAID" ? "#059669" : "#dc2626"
                                        }}>
                                            {row.status}
                                        </span>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            </div>

        </div>
    )
}