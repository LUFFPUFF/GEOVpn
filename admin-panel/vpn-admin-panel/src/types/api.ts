/**
 * Описание ролей пользователей
 */
export type UserRole = 'USER' | 'ADMIN' | 'SERVICE';

/**
 * Типы подписок
 */
export type SubscriptionType = 'PAYG' | 'BASIC' | 'STANDARD' | 'FAMILY' | 'BUSINESS' | 'UNLIMITED';

/**
 * Типы устройств
 */
export type DeviceType = 'IOS' | 'ANDROID' | 'WINDOWS';

/**
 * Ответ по пользователю (из UserController / AdminController)
 */
export interface UserResponse {
    id: number;
    telegramId: number;
    username: string | null;
    firstName: string;
    balance: number;
    subscriptionType: SubscriptionType;
    subscriptionExpiresAt: string | null;
    referralCode: string;
    createdAt: string;
    lastActiveAt: string | null;
    hasActiveSubscription: boolean;
    status?: 'ACTIVE' | 'INACTIVE' | 'BANNED';
}

/**
 * Подробная статистика пользователя для вкладки INFO
 */
export interface UserStatsResponse {
    telegramId: number;
    balance: number;
    totalDevices: number;
    activeDevices: number;
    totalReferrals: number;
    totalReferralEarnings: number;
    totalConnections: number;
    totalTrafficBytes: number;
}

/**
 * Ответ по устройству (из DeviceController / AdminController)
 */
export interface DeviceResponse {
    id: number;
    deviceName: string;
    deviceType: DeviceType;
    uuid: string;
    isActive: boolean;
    lastConnectedAt: string | null;
    createdAt: string;
}

/**
 * Полный конфиг VPN (из VpnConfigController)
 * Приходит, когда мы раскрываем карточку устройства
 */
export interface VpnConfigResponse {
    id: number;
    deviceId: number;
    userId: number;
    subscriptionUrl: string;
    subscriptionBase64: string;
    qrCode: string;
    status: string;
    configs: ServerConfig[];
    recommendedProtocol: string;
    selectionReason: string | null;
    serverScore: number | null;
    availableProtocols: string[];
}

/**
 * Конфигурация конкретного сервера внутри подписки
 */
export interface ServerConfig {
    serverId: number;
    serverName: string;
    countryCode: string;
    countryEmoji: string;
    type: 'STANDARD' | 'ANTIGLUSH' | 'WHITELIST';
    vlessLink: string;
    protocol: string;
    avgLatencyMs: number | null;
    healthScore?: number | null;
    isRelay: boolean;
}

/**
 * Статистика для главного дашборда (из AdminController)
 */
export interface AdminDashboardResponse {
    totalUsers: number;
    activeSubscriptions: number;
    totalServers: number;
    activeServers: number;
    totalTrafficGb: number;
    totalBalanceRub: number;
}

/**
 * Статус лимита устройств
 */
export interface DeviceLimitStatus {
    userId: number;
    maxDevices: number;
    activeDevices: number;
    remainingSlots: number;
    limitReached: boolean;
    planName: string;
}

/**
 * Универсальный оберточный формат ответа от твоего API
 */
export interface ApiResponse<T> {
    success: boolean;
    data: T;
    error?: {
        code: string;
        message: string;
        traceId: string;
    };
}