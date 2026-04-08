export type UserRole = 'USER' | 'ADMIN' | 'SERVICE';
export type SubscriptionType = 'PAYG' | 'BASIC' | 'STANDARD' | 'FAMILY' | 'BUSINESS' | 'UNLIMITED';
export type DeviceType = 'IOS' | 'ANDROID' | 'WINDOWS';

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

export interface DeviceResponse {
    id: number;
    deviceName: string;
    deviceType: DeviceType;
    uuid: string;
    isActive: boolean;
    lastConnectedAt: string | null;
    createdAt: string;
}

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

export interface DeviceLimitStatus {
    userId: number;
    maxDevices: number;
    activeDevices: number;
    remainingSlots: number;
    limitReached: boolean;
    planName: string;
}

export interface ApiResponse<T> {
    success: boolean;
    data: T;
    error?: {
        code: string;
        message: string;
        traceId: string;
    };
}

export interface UserData {
    active: boolean;
    daysLeft: number;
    trafficUsed: number;
    activeDevices: number;
    currentLocation: string;
}

export interface LeaderboardEntry {
    firstName: string;
    username: string | null;
    referralCount: number;
    isWinner: boolean;
}
