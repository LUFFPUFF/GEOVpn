import axios from 'axios';
import {
    AdminDashboardResponse,
    UserResponse,
    DeviceResponse,
    UserStatsResponse,
    DeviceLimitStatus,
    ServerDto,
    CreateServerRequest,
    XrayUserStat,
    UserTrafficStatsDto,
    SystemHealthDto,
    VpnConfigResponse
} from '../types/api';

const BASE_URL = (import.meta as any).env.VITE_API_BASE_URL || "";
const INTERNAL_SECRET = (import.meta as any).env.VITE_INTERNAL_SECRET || "";
const DEFAULT_ADMIN_ID = (import.meta as any).env.VITE_DEFAULT_ADMIN_ID || "";

export const apiClient = axios.create({
    baseURL: BASE_URL,
});

apiClient.interceptors.request.use((config) => {
    if (INTERNAL_SECRET) config.headers['X-Internal-Secret'] = INTERNAL_SECRET;
    if (DEFAULT_ADMIN_ID) config.headers['X-User-Id'] = DEFAULT_ADMIN_ID;
    return config;
});

// Persistent localStorage relational DB mirroring the database tables screenshots
const INITIAL_USERS: UserResponse[] = [
    { id: 1, telegramId: 6262243631, username: "osemeth", firstName: "best", balance: 0, subscriptionType: "payg", subscriptionExpiresAt: undefined, hasActiveSubscription: false, createdAt: "2026-05-28T19:24:51Z", status: "ACTIVE", isBlocked: false },
    { id: 2, telegramId: 7464576295, username: "user_7464576295", firstName: "ВАЛЕРА", balance: 12000, subscriptionType: "经典极速classic", subscriptionExpiresAt: "2026-08-15T00:00:00Z", hasActiveSubscription: true, createdAt: "2026-05-20T10:15:00Z", status: "ACTIVE", isBlocked: false },
    { id: 3, telegramId: 8345719321, username: "fffffrmaaaakkk", firstName: "FRAK", balance: 0, subscriptionType: "payg", subscriptionExpiresAt: undefined, hasActiveSubscription: false, createdAt: "2026-05-18T12:30:00Z", status: "ACTIVE", isBlocked: false },
    { id: 4, telegramId: 9845123984, username: "Bacon_General", firstName: "🥓BACON GENERAL🥓", balance: 4500, subscriptionType: "经典极速classic", subscriptionExpiresAt: "2026-07-28T00:00:00Z", hasActiveSubscription: true, createdAt: "2026-05-19T08:00:00Z", status: "ACTIVE", isBlocked: false },
    { id: 5, telegramId: 8686167295, username: "user_8686167295", firstName: "ZKHAR68", balance: -500, subscriptionType: "payg", subscriptionExpiresAt: undefined, hasActiveSubscription: false, createdAt: "2026-05-22T14:45:00Z", status: "BANNED", isBlocked: true },
    { id: 6, telegramId: 1284591244, username: "Lol122562", firstName: "ЧУВАК", balance: 1500, subscriptionType: "payg", subscriptionExpiresAt: undefined, hasActiveSubscription: false, createdAt: "2026-05-21T09:12:00Z", status: "ACTIVE", isBlocked: false },
    { id: 7, telegramId: 1948512304, username: "Angeliss_try", firstName: "ANGLESSS UIU", balance: 0, subscriptionType: "payg", subscriptionExpiresAt: undefined, hasActiveSubscription: false, createdAt: "2026-05-23T11:22:00Z", status: "ACTIVE", isBlocked: false },
    { id: 8, telegramId: 3948512349, username: "arshavkin71", firstName: "АРТЁМ", balance: 60000, subscriptionType: "经典极速classic", subscriptionExpiresAt: "2026-07-28T00:00:00Z", hasActiveSubscription: true, createdAt: "2026-05-24T16:40:00Z", status: "ACTIVE", isBlocked: false },
    { id: 9, telegramId: 4918451230, username: "xaradzukov", firstName: "XARA", balance: 0, subscriptionType: "payg", subscriptionExpiresAt: undefined, hasActiveSubscription: false, createdAt: "2026-05-25T13:05:00Z", status: "ACTIVE", isBlocked: false },
    { id: 10, telegramId: 5912845102, username: "millonka", firstName: "MILANA", balance: 0, subscriptionType: "payg", subscriptionExpiresAt: undefined, hasActiveSubscription: false, createdAt: "2026-05-26T21:10:00Z", status: "ACTIVE", isBlocked: false },
    { id: 11, telegramId: 7941203945, username: "YaRanala1000", firstName: "MAKSYSUK TY", balance: 0, subscriptionType: "经典极速classic", subscriptionExpiresAt: "2026-06-25T00:00:00Z", hasActiveSubscription: true, createdAt: "2026-05-26T18:32:00Z", status: "INACTIVE", isBlocked: false },
    { id: 12, telegramId: 2948510239, username: "tanyazwx1", firstName: "KITTWXUQ", balance: 500, subscriptionType: "payg", subscriptionExpiresAt: undefined, hasActiveSubscription: false, createdAt: "2026-05-27T10:14:00Z", status: "ACTIVE", isBlocked: false }
];

const INITIAL_DEVICES: DeviceResponse[] = [
    { id: 101, userId: 6262243631, deviceName: "iPhone 15 Pro", deviceType: "IOS", uuid: "d3b07384-d113-4c57-8e6f-d3fdccd9cbe3", isActive: true },
    { id: 102, userId: 7464576295, deviceName: "Валера Android", deviceType: "ANDROID", uuid: "a49214a1-b514-490b-9dfc-1122ab941285", isActive: true },
    { id: 103, userId: 3948512349, deviceName: "iPad Pro", deviceType: "IOS", uuid: "19ee34fa-e514-4e94-8efb-d02bc249ca3f", isActive: true },
    { id: 104, userId: 3948512349, deviceName: "MacBook Air M2", deviceType: "MAC", uuid: "49de1210-ca19-480c-9e23-de12bc4fbc3b", isActive: true },
];

const INITIAL_LIMITS: DeviceLimitStatus[] = [
    { userId: 6262243631, activeDevices: 1, maxDevices: 1, planName: "PAYG" },
    { userId: 7464576295, activeDevices: 1, maxDevices: 3, planName: "经典极速classic" },
    { userId: 8345719321, activeDevices: 0, maxDevices: 1, planName: "PAYG" },
    { userId: 9845123984, activeDevices: 0, maxDevices: 3, planName: "经典极速classic" },
    { userId: 8686167295, activeDevices: 0, maxDevices: 0, planName: "BANNED" },
    { userId: 1284591244, activeDevices: 0, maxDevices: 1, planName: "PAYG" },
    { userId: 1948512304, activeDevices: 0, maxDevices: 1, planName: "PAYG" },
    { userId: 3948512349, activeDevices: 2, maxDevices: 5, planName: "经典极速classic" },
    { userId: 4918451230, activeDevices: 0, maxDevices: 1, planName: "PAYG" },
    { userId: 5912845102, activeDevices: 0, maxDevices: 1, planName: "PAYG" },
    { userId: 7941203945, activeDevices: 0, maxDevices: 3, planName: "经典极速classic" },
    { userId: 2948510239, activeDevices: 0, maxDevices: 1, planName: "PAYG" },
];

const INITIAL_SERVERS: ServerDto[] = [
    { id: 1, name: "Netherlands-01", location: "Amsterdam, NL", countryCode: "NL", ipAddress: "193.104.33.100", port: 443, grpcPort: 62789, realityPublicKey: "0aOBytw04b_12fbc941a87e38f6b69fdcb034a", realityShortId: "2c8c7ec1", realitySni: "eh.vk.com", maxConnections: 1000, currentConnections: 154, healthScore: 98, avgLatencyMs: 38, isActive: true, lastHealthCheck: "2026-05-28T19:24:00Z" },
    { id: 2, name: "Frankfurt-02", location: "Frankfurt, DE", countryCode: "DE", ipAddress: "185.220.101.44", port: 443, grpcPort: 62789, realityPublicKey: "f3a0937c_91fba3c8bd0d4bde7c10acfe023a", realityShortId: "de88bc3f", realitySni: "eh.vk.com", maxConnections: 1000, currentConnections: 280, healthScore: 91, avgLatencyMs: 45, isActive: true, lastHealthCheck: "2026-05-28T19:24:10Z" },
    { id: 3, name: "Helsinki-01", location: "Helsinki, FI", countryCode: "FI", ipAddress: "95.217.33.82", port: 443, grpcPort: 62788, realityPublicKey: "9dc384be0dca9bfdc9ef0ba3f8bcdeab101235bc", realityShortId: "fa117e3c", realitySni: "eh.vk.com", maxConnections: 1000, currentConnections: 45, healthScore: 84, avgLatencyMs: 55, isActive: true, lastHealthCheck: "2026-05-28T19:23:55Z" }
];

const INITIAL_VPN_CONFIGURATIONS: any[] = [
    {
        id: 1,
        deviceId: 101,
        userId: 6262243631,
        serverId: 1,
        vlessUuid: "d3b07384-d113-4c57-8e6f-d3fdccd9cbe3",
        vlessLink: "vless://d3b07384-d113-4c57-8e6f-d3fdccd9cbe3@193.104.33.100:443?type=tcp&security=reality&sni=eh.vk.com&pbk=0aOBytw04b_12fbc941a87e38f6b69fdcb034a&sid=2c8c7ec1#NL-Amsterdam-01",
        protocol: "VLESS",
        status: "ACTIVE",
        createdAt: "2026-05-28T19:24:51Z",
        updatedAt: "2026-05-28T19:24:51Z",
        deviceOs: "IOS",
        device_name: "iPhone 15 Pro",
        vlessLinksJson: JSON.stringify([
            "vless://d3b07384-d113-4c57-8e6f-d3fdccd9cbe3@193.104.33.100:443?type=tcp&security=reality&sni=eh.vk.com&pbk=0aOBytw04b_12fbc941a87e38f6b69fdcb034a&sid=2c8c7ec1#NL-Amsterdam-01",
            "vless://d3b07384-d113-4c57-8e6f-d3fdccd9cbe3@185.220.101.44:443?type=tcp&security=reality&sni=eh.vk.com&pbk=f3a0937c_91fba3c8bd0d4bde7c10acfe023a&sid=de88bc3f#DE-Frankfurt-02"
        ])
    },
    {
        id: 2,
        deviceId: 102,
        userId: 7464576295,
        serverId: 2,
        vlessUuid: "a49214a1-b514-490b-9dfc-1122ab941285",
        vlessLink: "vless://a49214a1-b514-490b-9dfc-1122ab941285@185.220.101.44:443?type=tcp&security=reality&sni=eh.vk.com&pbk=f3a0937c_91fba3c8bd0d4bde7c10acfe023a&sid=de88bc3f#DE-Frankfurt-02",
        protocol: "VLESS",
        status: "ACTIVE",
        createdAt: "2026-05-20T10:15:00Z",
        updatedAt: "2026-05-20T10:15:00Z",
        deviceOs: "ANDROID",
        device_name: "Валера Android",
        vlessLinksJson: JSON.stringify([
            "vless://a49214a1-b514-490b-9dfc-1122ab941285@185.220.101.44:443?type=tcp&security=reality&sni=eh.vk.com&pbk=f3a0937c_91fba3c8bd0d4bde7c10acfe023a&sid=de88bc3f#DE-Frankfurt-02"
        ])
    }
];

const INITIAL_BAN_LOGS: any[] = [
    {
        id: 1,
        userId: 5,
        telegramId: 8686167295,
        username: "user_8686167295",
        action: "BAN",
        reason: "Подозрительный трафик со сканированием турецких подсетей",
        adminUsername: "admin_geovpn",
        createdAt: "2026-05-24T18:00:00Z"
    }
];

function getLocalDB() {
    if (typeof window === "undefined") {
        return {
            users: INITIAL_USERS,
            devices: INITIAL_DEVICES,
            limits: INITIAL_LIMITS,
            servers: INITIAL_SERVERS,
            configs: INITIAL_VPN_CONFIGURATIONS,
            banLogs: INITIAL_BAN_LOGS
        };
    }
    let users = localStorage.getItem("geovpn_users");
    let devices = localStorage.getItem("geovpn_devices");
    let limits = localStorage.getItem("geovpn_limits");
    let servers = localStorage.getItem("geovpn_servers");
    let configs = localStorage.getItem("geovpn_configs");
    let banLogs = localStorage.getItem("geovpn_ban_logs");

    if (!users) {
        localStorage.setItem("geovpn_users", JSON.stringify(INITIAL_USERS));
        localStorage.setItem("geovpn_devices", JSON.stringify(INITIAL_DEVICES));
        localStorage.setItem("geovpn_limits", JSON.stringify(INITIAL_LIMITS));
        localStorage.setItem("geovpn_servers", JSON.stringify(INITIAL_SERVERS));
        localStorage.setItem("geovpn_configs", JSON.stringify(INITIAL_VPN_CONFIGURATIONS));
        localStorage.setItem("geovpn_ban_logs", JSON.stringify(INITIAL_BAN_LOGS));
        return {
            users: INITIAL_USERS,
            devices: INITIAL_DEVICES,
            limits: INITIAL_LIMITS,
            servers: INITIAL_SERVERS,
            configs: INITIAL_VPN_CONFIGURATIONS,
            banLogs: INITIAL_BAN_LOGS
        };
    }

    return {
        users: JSON.parse(users),
        devices: JSON.parse(devices || "[]"),
        limits: JSON.parse(limits || "[]"),
        servers: JSON.parse(servers || "[]"),
        configs: JSON.parse(configs || JSON.stringify(INITIAL_VPN_CONFIGURATIONS)),
        banLogs: JSON.parse(banLogs || JSON.stringify(INITIAL_BAN_LOGS))
    };
}

function saveLocalDB(data: {
    users: UserResponse[],
    devices: DeviceResponse[],
    limits: DeviceLimitStatus[],
    servers: ServerDto[],
    configs?: any[],
    banLogs?: any[]
}) {
    if (typeof window === "undefined") return;
    localStorage.setItem("geovpn_users", JSON.stringify(data.users));
    localStorage.setItem("geovpn_devices", JSON.stringify(data.devices));
    localStorage.setItem("geovpn_limits", JSON.stringify(data.limits));
    localStorage.setItem("geovpn_servers", JSON.stringify(data.servers));
    if (data.configs) localStorage.setItem("geovpn_configs", JSON.stringify(data.configs));
    if (data.banLogs) localStorage.setItem("geovpn_ban_logs", JSON.stringify(data.banLogs));
}

// Global server environment override detector
const useMock = !BASE_URL;

export const adminApi = {
    getDashboard: (): Promise<AdminDashboardResponse> => {
        if (!useMock) {
            return apiClient.get('/admin/dashboard').then(res => res.data.data);
        }
        const db = getLocalDB();
        const activeSubCount = db.users.filter(u => u.hasActiveSubscription).length;
        const totalBalance = db.users.reduce((sum, u) => sum + (u.balance / 100), 0);
        return Promise.resolve({
            totalUsers: 2585, // Display 2585 to match screenshots, or db.users.length as fallback in UI
            activeSubscriptions: activeSubCount,
            totalBalanceRub: parseFloat(totalBalance.toFixed(2))
        });
    },

    getInfrastructureHealth: (): Promise<SystemHealthDto> => {
        if (!useMock) {
            return apiClient.get('/servers/infrastructure/health').then(res => res.data.data);
        }
        return Promise.resolve({
            status: "UP",
            services: {
                "Central Backend": { status: "UP", details: "Node v22.14.0 · Selectel", cpuUsage: 4.2, memoryUsedMb: 184, memoryMaxMb: 4096, uptime: 1245600 },
                "Redis Session Cache": { status: "UP", details: "Redis v7.2 · Selectel Cluster", cpuUsage: 1.1, memoryUsedMb: 12, memoryMaxMb: 512, uptime: 1245600 },
                "TrafficCollector Scheduler": { status: "UP", details: "Cron daemon v1.5 · Active", cpuUsage: 0.5, memoryUsedMb: 42, memoryMaxMb: 1024, uptime: 1245600 }
            }
        });
    },

    // RESOLVES CRITICAL USER SEARCHING - Hits backend mock database with standard pagination, fuzzy mapping id, names, telegram_id.
    getUsers: (page = 0, size = 50, query = ""): Promise<UserResponse[]> => {
        if (!useMock) {
            return apiClient.get(`/admin/users?page=${page}&size=${size}&search=${encodeURIComponent(query)}`).then(res => res.data.data);
        }
        const db = getLocalDB();
        let filtered = db.users;

        if (query) {
            const q = query.toLowerCase().trim();
            const rawQ = q.startsWith("@") ? q.substring(1) : q;
            filtered = db.users.filter(u => 
                u.id.toString() === q ||
                u.telegramId.toString().includes(q) ||
                (u.username && u.username.toLowerCase().includes(rawQ)) ||
                (u.firstName && u.firstName.toLowerCase().includes(q)) ||
                (u.subscriptionType && u.subscriptionType.toLowerCase().includes(q))
            );
        }

        // Return a slice matching page / size
        const start = page * size;
        const slice = filtered.slice(start, start + size);
        return Promise.resolve(slice);
    },

    getUserStats: (telegramId: number): Promise<UserStatsResponse> => {
        if (!useMock) {
            return apiClient.get(`/admin/users/${telegramId}/stats`).then(res => res.data.data);
        }
        // Return structured analytics
        return Promise.resolve({
            totalTrafficBytes: 38450123984, // ~38GB
            totalReferrals: telegramId % 3 === 0 ? 4 : 1,
            totalConnections: telegramId % 2 === 0 ? 8 : 3
        });
    },

    createUser: (user: Partial<UserResponse>): Promise<UserResponse> => {
        const db = getLocalDB();
        const newId = db.users.reduce((max, u) => u.id > max ? u.id : max, 0) + 1;
        const newUser: UserResponse = {
            id: newId,
            telegramId: Number(user.telegramId) || Math.floor(Math.random() * 9000000000) + 1000000000,
            username: user.username || "",
            firstName: user.firstName || "Без имени",
            balance: Number(user.balance) || 0,
            subscriptionType: user.subscriptionType || "payg",
            subscriptionExpiresAt: user.subscriptionExpiresAt || undefined,
            hasActiveSubscription: user.subscriptionType !== "payg" && !!user.subscriptionExpiresAt,
            createdAt: new Date().toISOString(),
            status: user.status || "ACTIVE"
        };

        db.users.push(newUser);

        // Auto-assign matching limit limit
        const limit: DeviceLimitStatus = {
            userId: newUser.telegramId,
            activeDevices: 0,
            maxDevices: newUser.subscriptionType === "payg" ? 1 : 3,
            planName: newUser.subscriptionType
        };
        db.limits.push(limit);

        saveLocalDB(db);
        return Promise.resolve(newUser);
    },

    updateUserFields: (telegramId: number, fields: Partial<UserResponse>): Promise<UserResponse> => {
        const db = getLocalDB();
        const index = db.users.findIndex(u => u.telegramId === telegramId);
        if (index === -1) return Promise.reject(new Error("Пользователь не найден"));

        const current = db.users[index];
        
        // Sync isBlocked and status
        let isBlocked = fields.isBlocked !== undefined ? fields.isBlocked : current.isBlocked;
        let status = fields.status || current.status;
        let subscriptionType = fields.subscriptionType || current.subscriptionType;

        if (status === "BANNED" || isBlocked) {
            isBlocked = true;
            status = "BANNED";
            subscriptionType = "BANNED";
        } else if (status === "ACTIVE" && current.status === "BANNED") {
            isBlocked = false;
            status = "ACTIVE";
            subscriptionType = current.subscriptionType === "BANNED" ? "payg" : current.subscriptionType;
        }

        const updated = {
            ...current,
            ...fields,
            status,
            isBlocked,
            subscriptionType,
            hasActiveSubscription: status !== "BANNED" && subscriptionType !== "payg" ? (fields.hasActiveSubscription ?? current.hasActiveSubscription) : false
        };

        db.users[index] = updated;

        // Sync limits
        const limIdx = db.limits.findIndex(l => l.userId === telegramId);
        if (limIdx !== -1) {
            db.limits[limIdx].planName = updated.subscriptionType;
            db.limits[limIdx].maxDevices = updated.status === "BANNED" ? 0 : (updated.subscriptionType === "payg" ? 1 : 3);
        }

        saveLocalDB(db);
        return Promise.resolve(updated);
    },

    addBalance: (telegramId: number, amount: number): Promise<any> => {
        if (!useMock) {
            return apiClient.post(`/admin/users/${telegramId}/add-balance?amount=${amount}`).then(res => res.data.data);
        }
        const db = getLocalDB();
        const user = db.users.find(u => u.telegramId === telegramId);
        if (user) {
            user.balance = user.balance + amount; // Allow both plus and minus
            saveLocalDB(db);
        }
        return Promise.resolve({ success: true, balance: user ? user.balance : 0 });
    },

    deactivateUser: (telegramId: number): Promise<void> => {
        if (!useMock) {
            return apiClient.post(`/admin/users/${telegramId}/deactivate`).then(res => res.data.data);
        }
        const db = getLocalDB();
        const user = db.users.find(u => u.telegramId === telegramId);
        if (user) {
            user.status = "BANNED";
            user.subscriptionType = "BANNED";
            user.hasActiveSubscription = false;
            saveLocalDB(db);
        }
        return Promise.resolve();
    },

    deleteUser: (telegramId: number): Promise<void> => {
        const db = getLocalDB();
        db.users = db.users.filter(u => u.telegramId !== telegramId);
        db.devices = db.devices.filter(d => d.userId !== telegramId);
        db.limits = db.limits.filter(l => l.userId !== telegramId);
        saveLocalDB(db);
        return Promise.resolve();
    },

    getUserDevices: (telegramId: number): Promise<DeviceResponse[]> => {
        if (!useMock) {
            return apiClient.get(`/admin/users/${telegramId}/devices`).then(res => res.data.data);
        }
        const db = getLocalDB();
        const list = db.devices.filter(d => d.userId === telegramId);
        return Promise.resolve(list);
    },

    getDeviceLimit: (userId: number): Promise<DeviceLimitStatus> => {
        if (!useMock) {
            return apiClient.get(`/admin/device-limits/${userId}`).then(res => res.data.data);
        }
        const db = getLocalDB();
        let limit = db.limits.find(l => l.userId === userId);
        if (!limit) {
            const user = db.users.find(u => u.telegramId === userId);
            limit = {
                userId,
                activeDevices: db.devices.filter(d => d.userId === userId).length,
                maxDevices: user ? (user.subscriptionType === "payg" ? 1 : 3) : 1,
                planName: user ? user.subscriptionType : "PAYG"
            };
        } else {
            // Recount active devices
            limit.activeDevices = db.devices.filter(d => d.userId === userId).length;
        }
        return Promise.resolve(limit);
    },

    registerDevice: (telegramId: number, data: { deviceName: string, deviceType: string }): Promise<DeviceResponse> => {
        if (!useMock) {
            return apiClient.post('/devices', data, { headers: { 'X-User-Id': telegramId.toString() } }).then(res => res.data.data);
        }
        const db = getLocalDB();
        const newId = db.devices.length > 0 ? Math.max(...db.devices.map(d => d.id)) + 1 : 101;
        const newDev: DeviceResponse = {
            id: newId,
            userId: telegramId,
            deviceName: data.deviceName,
            deviceType: data.deviceType,
            uuid: crypto.randomUUID ? crypto.randomUUID() : `f9bc3e88-${Math.floor(Math.random() * 9000) + 1000}-4d04-8feb-a9bcde${Math.floor(Math.random() * 900000) + 100000}`,
            isActive: true
        };

        db.devices.push(newDev);

        const limit = db.limits.find(l => l.userId === telegramId);
        if (limit) limit.activeDevices++;

        saveLocalDB(db);
        return Promise.resolve(newDev);
    },

    deleteDevice: (deviceId: number): Promise<void> => {
        const db = getLocalDB();
        const dev = db.devices.find(d => d.id === deviceId);
        if (dev) {
            const limit = db.limits.find(l => l.userId === dev.userId);
            if (limit) limit.activeDevices = Math.max(0, limit.activeDevices - 1);
            db.devices = db.devices.filter(d => d.id !== deviceId);
            saveLocalDB(db);
        }
        return Promise.resolve();
    },

    changePlan: (userId: number, planName: string): Promise<DeviceLimitStatus> => {
        if (!useMock) {
            return apiClient.put(`/admin/device-limits/${userId}/plan`, { planName }).then(res => res.data.data);
        }
        const db = getLocalDB();
        const user = db.users.find(u => u.telegramId === userId);
        if (user) {
            user.subscriptionType = planName;
            if (planName === "payg") {
                user.hasActiveSubscription = false;
                user.subscriptionExpiresAt = undefined;
            } else {
                user.hasActiveSubscription = true;
                user.subscriptionExpiresAt = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString();
            }
        }

        let limit = db.limits.find(l => l.userId === userId);
        if (!limit) {
            limit = { userId, activeDevices: 0, maxDevices: planName === "payg" ? 1 : 3, planName };
            db.limits.push(limit);
        } else {
            limit.planName = planName;
            limit.maxDevices = planName === "payg" ? 1 : 3;
        }

        saveLocalDB(db);
        return Promise.resolve(limit);
    },

    getServers: (): Promise<ServerDto[]> => {
        if (!useMock) {
            return apiClient.get('/servers').then(res => res.data.data);
        }
        const db = getLocalDB();
        return Promise.resolve(db.servers);
    },

    createServer: (payload: CreateServerRequest): Promise<ServerDto> => {
        if (!useMock) {
            return apiClient.post('/servers', payload).then(res => res.data.data);
        }
        const db = getLocalDB();
        const newId = db.servers.length > 0 ? Math.max(...db.servers.map(s => s.id)) + 1 : 1;
        const newSrv: ServerDto = {
            ...payload,
            id: newId,
            currentConnections: 0,
            healthScore: 100,
            avgLatencyMs: 40,
            isActive: true,
            lastHealthCheck: new Date().toISOString()
        };
        db.servers.push(newSrv);
        saveLocalDB(db);
        return Promise.resolve(newSrv);
    },

    updateServer: (id: number, payload: Partial<CreateServerRequest | { isActive: boolean }>): Promise<ServerDto> => {
        if (!useMock) {
            return apiClient.put(`/servers/${id}`, payload).then(res => res.data.data);
        }
        const db = getLocalDB();
        const srv = db.servers.find(s => s.id === id);
        if (srv) {
            Object.assign(srv, payload);
            saveLocalDB(db);
            return Promise.resolve(srv);
        }
        return Promise.reject(new Error("Сервер не найден"));
    },

    toggleServerStatus: (id: number, isActive: boolean): Promise<ServerDto> => {
        if (!useMock) {
            return apiClient.put(`/servers/${id}`, { isActive }).then(res => res.data.data);
        }
        return adminApi.updateServer(id, { isActive });
    },

    deleteServer: (id: number): Promise<void> => {
        if (!useMock) {
            return apiClient.delete(`/servers/${id}`).then(res => res.data.data);
        }
        const db = getLocalDB();
        db.servers = db.servers.filter(s => s.id !== id);
        saveLocalDB(db);
        return Promise.resolve();
    },

    getServerUsers: (serverId: number): Promise<XrayUserStat[]> => {
        if (!useMock) {
            return apiClient.get(`/servers/${serverId}/users`).then(res => res.data.data);
        }
        const db = getLocalDB();
        const activeUsersObj: XrayUserStat[] = [];
        db.devices.forEach(d => {
            if (d.isActive) {
                // Uplink
                activeUsersObj.push({
                    name: `user>>>${d.uuid}>>>vless>>>uplink`,
                    value: Math.floor(Math.random() * 500000000) + 100000
                });
                // Downlink
                activeUsersObj.push({
                    name: `user>>>${d.uuid}>>>vless>>>downlink`,
                    value: Math.floor(Math.random() * 1500000000) + 1000000
                });
            }
        });
        return Promise.resolve(activeUsersObj);
    },

    getServerTrafficStats: (serverId: number): Promise<UserTrafficStatsDto[]> => {
        if (!useMock) {
            return apiClient.get(`/traffic/servers/${serverId}/stats`).then(res => res.data.data);
        }
        const db = getLocalDB();
        const stats: UserTrafficStatsDto[] = db.devices.map(d => ({
            userId: d.uuid,
            totalUp: Math.floor(Math.random() * 50000000000) + 1024,
            totalDown: Math.floor(Math.random() * 120000000000) + 1024
        }));
        return Promise.resolve(stats);
    },

    kickUser: (serverId: number, email: string): Promise<boolean> => {
        if (!useMock) {
            return apiClient.post(`/servers/${serverId}/users/${email}/kick`).then(res => res.data.data);
        }
        return Promise.resolve(true);
    },

    getDeviceConfig: (deviceId: number): Promise<any> => {
        if (!useMock) {
            return apiClient.get(`/admin/devices/${deviceId}/config`).then(res => res.data.data);
        }
        const db = getLocalDB();
        const device = db.devices.find(d => d.id === deviceId);
        const user = device ? db.users.find(u => u.telegramId === device.userId) : null;
        return Promise.resolve({
            subscriptionUrl: `https://sub.geovpn.org/api/configs?token=${device ? device.uuid : "d3b07384"}`,
            configs: [
                { countryEmoji: "🇳🇱", serverName: "NL-Amsterdam-01", vlessLink: `vless://${device ? device.uuid : "uuid-xxx"}@193.104.33.100:443?type=tcp&security=reality&sni=eh.vk.com&pbk=0aOBytw04b#NL-Amsterdam-01` },
                { countryEmoji: "🇩🇪", serverName: "DE-Frankfurt-02", vlessLink: `vless://${device ? device.uuid : "uuid-xxx"}@185.220.101.44:443?type=tcp&security=reality&sni=eh.vk.com&pbk=f3a0937c#DE-Frankfurt-02` }
            ]
        });
    },

    regenerateConfig: (deviceId: number): Promise<any> => {
        if (!useMock) {
            return apiClient.put(`/configs/${deviceId}/regenerate`, { reason: "Admin reset" }).then(res => res.data.data);
        }
        const db = getLocalDB();
        const device = db.devices.find(d => d.id === deviceId);
        if (device) {
            device.uuid = crypto.randomUUID ? crypto.randomUUID() : `f9bc3e88-${Math.floor(Math.random() * 9000) + 1000}-4d04-8feb-a9bcde${Math.floor(Math.random() * 900000) + 100000}`;
            saveLocalDB(db);
        }
        return adminApi.getDeviceConfig(deviceId);
    },

    getVpnConfigurations: (): Promise<any[]> => {
        const db = getLocalDB();
        return Promise.resolve(db.configs || []);
    },

    updateVpnConfiguration: (id: number, fields: any): Promise<any> => {
        const db = getLocalDB();
        const configIndex = db.configs.findIndex((c: any) => c.id === id);
        if (configIndex === -1) {
            return Promise.reject(new Error("Конфигурация не найдена"));
        }

        const updatedConfig = {
            ...db.configs[configIndex],
            ...fields,
            updatedAt: new Date().toISOString()
        };
        db.configs[configIndex] = updatedConfig;

        // Propagate updates to the servers table if we modify the host, port, SNI, public key, or short ID parameters
        if (updatedConfig.serverId) {
            const server = db.servers.find((s: any) => s.id === updatedConfig.serverId);
            if (server) {
                // If the link changed, parse or use provided fields directly
                if (fields.vlessLink) {
                    // Simple parse of vless link to extract reality parameters
                    try {
                        // vless://uuid@host:port?type=tcp&security=reality&sni=xxx&pbk=xxx&sid=xxx#name
                        const linkStr = fields.vlessLink;
                        const matchHostPort = linkStr.match(/@([^:/?#]+):(\d+)/);
                        const matchSni = linkStr.match(/[?&]sni=([^&#\s]+)/);
                        const matchPbk = linkStr.match(/[?&]pbk=([^&#\s]+)/);
                        const matchSid = linkStr.match(/[?&]sid=([^&#\s]+)/);

                        if (matchHostPort) {
                            server.ipAddress = matchHostPort[1];
                            server.port = parseInt(matchHostPort[2]);
                        }
                        if (matchSni) server.realitySni = matchSni[1];
                        if (matchPbk) server.realityPublicKey = matchPbk[1];
                        if (matchSid) server.realityShortId = matchSid[1];
                    } catch (err) {
                        console.error("Error auto-syncing VLESS config parameters to Server:", err);
                    }
                }
                
                // If custom direct modifications were done to config fields
                if (fields.ipAddress) server.ipAddress = fields.ipAddress;
                if (fields.port) server.port = Number(fields.port);
                if (fields.realitySni) server.realitySni = fields.realitySni;
                if (fields.realityPublicKey) server.realityPublicKey = fields.realityPublicKey;
                if (fields.realityShortId) server.realityShortId = fields.realityShortId;
            }
        }

        saveLocalDB(db);
        return Promise.resolve(updatedConfig);
    },

    getBanLogs: (): Promise<any[]> => {
        const db = getLocalDB();
        return Promise.resolve(db.banLogs || []);
    },

    banUser: (userId: number, telegramId: number, username: string, reason: string, adminUsername = "admin"): Promise<any> => {
        const db = getLocalDB();
        
        // Find user by telegramId
        const user = db.users.find((u: any) => u.telegramId === telegramId);
        if (user) {
            user.status = "BANNED";
            user.isBlocked = true;
            user.subscriptionType = "BANNED";
            user.hasActiveSubscription = false;
        }

        // Deactivate their devices and config status if existing
        db.devices.forEach((d: any) => {
            if (d.userId === telegramId) {
                d.isActive = false;
            }
        });
        db.configs.forEach((c: any) => {
            if (c.userId === telegramId) {
                c.status = "REVOKED";
                c.revokedAt = new Date().toISOString();
            }
        });

        // Add to limits
        const limit = db.limits.find((l: any) => l.userId === telegramId);
        if (limit) {
            limit.planName = "BANNED";
            limit.maxDevices = 0;
            limit.activeDevices = 0;
        }

        // Add to ban logs
        const newLog = {
            id: db.banLogs.length > 0 ? Math.max(...db.banLogs.map((l: any) => l.id)) + 1 : 1,
            userId: user ? user.id : userId,
            telegramId,
            username: username || (user ? user.username : "unknown"),
            action: "BAN",
            reason: reason || "Нарушение правил использования сервиса",
            adminUsername,
            createdAt: new Date().toISOString()
        };
        db.banLogs.push(newLog);

        saveLocalDB(db);
        return Promise.resolve({ user, log: newLog });
    },

    unbanUser: (userId: number, telegramId: number, username: string, reason: string, adminUsername = "admin"): Promise<any> => {
        const db = getLocalDB();
        
        // Find user by telegramId
        const user = db.users.find((u: any) => u.telegramId === telegramId);
        if (user) {
            user.status = "ACTIVE";
            user.isBlocked = false;
            user.subscriptionType = "payg";
            user.hasActiveSubscription = false;
        }

        // Set limits to standard PAYG
        const limit = db.limits.find((l: any) => l.userId === telegramId);
        if (limit) {
            limit.planName = "PAYG";
            limit.maxDevices = 1;
            limit.activeDevices = 0;
        }

        // Add to logs
        const newLog = {
            id: db.banLogs.length > 0 ? Math.max(...db.banLogs.map((l: any) => l.id)) + 1 : 1,
            userId: user ? user.id : userId,
            telegramId,
            username: username || (user ? user.username : "unknown"),
            action: "UNBAN",
            reason: reason || "Разблокирован администратором",
            adminUsername,
            createdAt: new Date().toISOString()
        };
        db.banLogs.push(newLog);

        saveLocalDB(db);
        return Promise.resolve({ user, log: newLog });
    }
};
