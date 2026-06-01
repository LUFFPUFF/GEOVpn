import { useState } from "react"
import { ArrowDownToLine, Calendar, CreditCard, Database, Server, Shield, TrendingDown, TrendingUp } from "lucide-react"

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
                        {/* Revenues */}
                        <rect x={x} y={H - hRev} width={barW} height={hRev} fill="#10b981" rx="4" />
                        {/* Expenses */}
                        <rect x={x + barW + gap} y={H - hExp} width={barW} height={hExp} fill="#ef4444" rx="4" />
                        {/* Month labels */}
                        <text x={x + barW + gap / 2} y={H + 20} fontSize="11" fill="#64748b" textAnchor="middle" fontFamily="Inter">{d.month}</text>
                    </g>
                );
            })}
        </svg>
    )
}

export default function FinOpsPanel() {
    const [infraMode, setInfraMode] = useState<"SIMPLE"|"STABLE">("SIMPLE");

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

    const activeUsers = 1245;
    const grossRevenue = 45231.50; // Month gross user billing 
    const arpu = grossRevenue / activeUsers;

    const euCostPerServer = infraMode === "SIMPLE" ? CONSTANTS.euServerSimple : CONSTANTS.euServerStable;
    const totalEuCost = euCostPerServer * CONSTANTS.euLocations.length;

    const totalExpenses = totalEuCost + CONSTANTS.backendCost + CONSTANTS.ruServerCost;
    const netProfit = grossRevenue - totalExpenses;
    const profitMargin = (netProfit / grossRevenue) * 100;

    const costPerUser = totalExpenses / activeUsers;
    const breakEvenUsers = Math.ceil(totalExpenses / arpu);

    const chartData = [
        { month: "Янв", rev: 28000, exp: 7972 },
        { month: "Фев", rev: 34500, exp: 7972 },
        { month: "Мар", rev: 41200, exp: 7972 },
        { month: "Апр", rev: 45231, exp: totalExpenses },
    ];

    const ledgerData = [
        { id: "TRX-9942", date: "2024-04-15 10:22", category: "Доход", desc: "Пополнение баланса: User #85844", amount: 1500, status: "SUCCESS" },
        { id: "TRX-9941", date: "2024-04-15 09:14", category: "Доход", desc: "Списание TrafficCollector (1.2 TB)", amount: 3450, status: "SUCCESS" },
        { id: "INV-AEZA-04", date: "2024-04-01 00:01", category: "Расход", desc: "Оплата VPS: NL, FR, FI, LV (Aeza)", amount: -totalEuCost, status: "PAID" },
        { id: "INV-YNDX-04", date: "2024-04-01 00:01", category: "Расход", desc: "Оплата: Центральный Бэкенд + БД (Selectel)", amount: -4000, status: "PAID" },
        { id: "INV-ANTI-04", date: "2024-04-01 00:01", category: "Расход", desc: "Оплата: RU Прокси Антиглуш (Yandex)", amount: -1600, status: "PAID" },
        { id: "TRX-9820", date: "2024-03-29 18:40", category: "Доход", desc: "Оплата тарифа: User #11294", amount: 500, status: "SUCCESS" },
    ];

    const exportToCSV = () => {
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
        <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-white p-10 transition-colors duration-300">
            <style>{`
                .card { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 16px; box-shadow: var(--shadow-card); transition: all 0.2s; }
                .card:hover { box-shadow: 0 10px 20px -5px rgba(0,0,0,0.05); }
                
                .btn-primary { display:inline-flex; align-items:center; gap:8px; padding:10px 18px; background:linear-gradient(135deg, #10b981, #059669); border:none; border-radius:10px; color:white; font-size:13px; font-weight:700; cursor:pointer; transition:all 0.2s; box-shadow:0 4px 10px -2px rgba(16,185,129,0.3); }
                .btn-primary:hover { transform:translateY(-1px); box-shadow:0 6px 14px -2px rgba(16,185,129,0.4); }
                
                .switch-btn { padding:10px 20px; font-size:13px; font-weight:700; cursor:pointer; transition:all 0.2s; border:none; }
                .switch-active { background:#0f172a; color:#ffffff; box-shadow:0 2px 4px rgba(0,0,0,0.1); }
                .switch-inactive { background:transparent; color:#64748b; }
                .switch-inactive:hover { color:#0f172a; }

                /* Tables design */
                .ledger-table { width: 100%; border-collapse: collapse; text-align: left; }
                .ledger-table th { padding: 16px; font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.05em; border-bottom: 2px solid #e2e8f0; background: #f8fafc; }
                .ledger-table td { padding: 16px; font-size: 13px; color: #0f172a; border-bottom: 1px solid #f1f5f9; font-weight: 500; }
                .ledger-table tr:hover td { background: #f8fafc; }
                .ledger-badge { font-size: 11px; font-weight: 700; padding: 4px 10px; border-radius: 20px; }
            `}</style>

            <div className="flex justify-between items-start mb-8">
                <div>
                    <h2 className="text-3xl font-extrabold tracking-tight italic" style={{ fontFamily: "Space Grotesk, sans-serif" }}>FinOps Бухучет</h2>
                    <p className="text-sm text-slate-500 dark:text-slate-400">Настройка инфраструктурных тарифов, Окупимость и Unit-экономика</p>
                </div>
                <button className="btn-primary cursor-pointer" onClick={exportToCSV}>
                    <ArrowDownToLine size={16} /> Выгрузить реестр (Excel)
                </button>
            </div>

            {/* --- ARCHITECTURE ALTERNATIVE SWITCH --- */}
            <div className="flex justify-center mb-8">
                <div className="bg-slate-200 dark:bg-slate-800 p-1.5 rounded-xl display: inline-flex gap-2">
                    <button className={`switch-btn ${infraMode === "SIMPLE" ? "switch-active rounded-lg" : "switch-inactive"}`} onClick={() => setInfraMode("SIMPLE")}>
                        Простая архитектура (до 2k юзеров)
                    </button>
                    <button className={`switch-btn ${infraMode === "STABLE" ? "switch-active rounded-lg" : "switch-inactive"}`} onClick={() => setInfraMode("STABLE")}>
                        Стабильная архитектура (PRO)
                    </button>
                </div>
            </div>

            {/* --- KPI TILES --- */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-5 mb-8">
                {[
                    { label: "Выручка (Gross)", val: grossRevenue, color: "text-slate-900 dark:text-white", icon: <TrendingUp className="text-emerald-500" /> },
                    { label: "Расходы (OPEX)", val: totalExpenses, color: "text-red-500", icon: <TrendingDown className="text-red-400" />, minus: true },
                    { label: "Чистая прибыль (Net)", val: netProfit, color: "text-emerald-500", icon: <TrendingUp className="text-emerald-500" /> },
                    { label: "Рентабельность (Margin)", val: profitMargin, color: "text-blue-500", suffix: "%", format: (v:number)=>v.toFixed(1) },
                ].map((kpi, i) => (
                    <div key={i} className={`card p-6 ${i === 2 ? "border border-emerald-500 bg-emerald-50/50 dark:bg-emerald-950/20" : ""}`}>
                        <div className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-2 flex justify-between">
                            <span>{kpi.label}</span>
                            {kpi.icon}
                        </div>
                        <div className={`text-2xl font-extrabold font-mono ${kpi.color}`}>
                            {kpi.minus ? "- " : ""}{kpi.format ? kpi.format(kpi.val) : kpi.val.toLocaleString("ru-RU")}
                            <span className="text-sm font-normal text-slate-400 ml-1">{kpi.suffix || "₽"}</span>
                        </div>
                    </div>
                ))}
            </div>

            {/* --- GRID SPLIT: BILLS VS CHART --- */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
                <div className="card p-6 flex flex-col justify-between">
                    <div className="flex justify-between items-center mb-6">
                        <h3 className="text-base font-bold">Смета инфраструктуры (в месяц)</h3>
                        <span className="text-xs font-extrabold text-red-500 bg-red-100 dark:bg-red-950/40 px-3 py-1.5 rounded-lg font-mono">
                            -{totalExpenses.toLocaleString("ru-RU")} ₽
                        </span>
                    </div>

                    <div className="flex flex-col gap-4">
                        {/* EU server locations list */}
                        <div className="bg-slate-50 dark:bg-slate-900 p-4 border border-slate-100 dark:border-slate-800 rounded-xl">
                            <div className="flex items-center gap-2 text-xs font-bold text-slate-600 dark:text-slate-400 uppercase tracking-widest mb-3">
                                <Server size={14} /> Европейские узлы (VPN)
                            </div>
                            {CONSTANTS.euLocations.map((loc, i) => (
                                <div key={i} className="flex justify-between items-center py-2 border-b border-dashed border-slate-200 dark:border-slate-800 last:border-none">
                                    <span className="text-xs font-medium">{loc.flag} {loc.name}</span>
                                    <span className="text-xs font-bold font-mono text-slate-500">{euCostPerServer} ₽</span>
                                </div>
                            ))}
                        </div>

                        {/* Russian proxy anti-censorship server */}
                        <div className="bg-slate-50 dark:bg-slate-900 p-4 border border-slate-100 dark:border-slate-800 rounded-xl flex justify-between items-center">
                            <div>
                                <div className="flex items-center gap-2 text-xs font-bold text-slate-600 dark:text-slate-400 uppercase tracking-widest mb-1">
                                    <Shield size={14} /> RU Антиглуш / Мост
                                </div>
                                <div className="text-[11px] text-slate-400">Yandex Cloud (Роутинг трафика РФ)</div>
                            </div>
                            <span className="text-sm font-bold font-mono">{CONSTANTS.ruServerCost} ₽</span>
                        </div>

                        {/* Central backend configuration server */}
                        <div className="bg-slate-50 dark:bg-slate-900 p-4 border border-slate-100 dark:border-slate-800 rounded-xl flex justify-between items-center">
                            <div>
                                <div className="flex items-center gap-2 text-xs font-bold text-slate-600 dark:text-slate-400 uppercase tracking-widest mb-1">
                                    <Database size={14} /> Ядро (Бэкенд + БД)
                                </div>
                                <div className="text-[11px] text-slate-400">Selectel Cloud VPS (4 CPU, 8 GB RAM)</div>
                            </div>
                            <span className="text-sm font-bold font-mono">{CONSTANTS.backendCost} ₽</span>
                        </div>
                    </div>
                </div>

                <div className="flex flex-col gap-6">
                    {/* Unit economy calculations card */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div className="card p-5 bg-gradient-to-br from-blue-50/50 to-white dark:from-blue-900/10 border border-blue-200">
                            <div className="text-xs text-blue-600 font-bold uppercase tracking-wider mb-2">Cost per active client</div>
                            <div className="text-2xl font-extrabold text-blue-700 dark:text-blue-400 font-mono">{costPerUser.toFixed(2)} ₽</div>
                            <div className="text-[11px] text-slate-500 mt-2">Себестоимость содержания 1 активного пользователя в месяц</div>
                        </div>

                        <div className="card p-5 bg-gradient-to-br from-amber-500/10 to-white border border-amber-300">
                            <div className="text-xs text-amber-700 font-bold uppercase tracking-wider mb-2">Точка безубыточности</div>
                            <div className="text-2xl font-extrabold text-amber-800 font-mono">{breakEvenUsers} <span className="text-lg">юзеров</span></div>
                            <div className="text-[11px] text-slate-500 mt-2">Минимум клиентов для окупаемости OPEX расходов</div>
                        </div>
                    </div>

                    <div className="card p-6 flex-1 flex flex-col justify-between">
                        <div className="flex justify-between items-center mb-4">
                            <h3 className="text-sm font-bold">Таблица P&L (Прибыль & Убытки)</h3>
                            <div className="flex gap-4 text-xs">
                                <span className="flex items-center gap-1.5"><div className="w-2.5 h-2.5 rounded-sm bg-[#10b981]" /> Доходы</span>
                                <span className="flex items-center gap-1.5"><div className="w-2.5 h-2.5 rounded-sm bg-[#ef4444]" /> Расходы</span>
                            </div>
                        </div>
                        <div className="flex-1 relative pb-4">
                            <PnLChart data={chartData} />
                        </div>
                    </div>
                </div>
            </div>

            {/* --- LEDGER LIST CARD --- */}
            <div className="card overflow-hidden">
                <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex justify-between items-center">
                    <h3 className="text-base font-bold">Финансовый журнал (Реестр операций)</h3>
                    <span className="text-xs text-slate-400 font-semibold">Последние живые транзакции</span>
                </div>
                <div className="overflow-x-auto w-full">
                    <table className="w-full text-left text-xs border-collapse">
                        <thead>
                            <tr className="bg-slate-50 dark:bg-slate-900 border-b border-slate-100 dark:border-slate-850">
                                <th className="p-4 uppercase font-bold text-slate-600">ID Операции</th>
                                <th className="p-4 uppercase font-bold text-slate-600">Дата</th>
                                <th className="p-4 uppercase font-bold text-slate-600">Категория</th>
                                <th className="p-4 uppercase font-bold text-slate-600">Описание</th>
                                <th className="p-4 uppercase font-bold text-slate-600 text-right">Сумма</th>
                                <th className="p-4 uppercase font-bold text-slate-600 text-center">Статус</th>
                            </tr>
                        </thead>
                        <tbody>
                            {ledgerData.map((row, i) => (
                                <tr key={i} className="border-b border-slate-100 dark:border-slate-850 hover:bg-slate-50/50">
                                    <td className="p-4 font-mono font-bold text-slate-500">{row.id}</td>
                                    <td className="p-4 text-slate-500">{row.date}</td>
                                    <td className="p-4">
                                        <span className={`font-bold ${row.category === "Доход" ? "text-emerald-500" : "text-red-500"}`}>{row.category}</span>
                                    </td>
                                    <td className="p-4 font-medium dark:text-slate-300">{row.desc}</td>
                                    <td className="p-4 font-mono font-bold text-right">
                                        {row.amount > 0 ? "+" : ""}{row.amount.toLocaleString("ru-RU")} ₽
                                    </td>
                                    <td className="p-4 text-center">
                                        <span className={`px-2.5 py-1 rounded-full text-[10px] font-extrabold uppercase ${row.status === "SUCCESS" || row.status === "PAID" ? "bg-emerald-100 dark:bg-emerald-950/20 text-emerald-600" : "bg-red-100 text-red-600"}`}>
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
