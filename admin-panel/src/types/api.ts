export interface UserResponse {
    id: number;
    telegramId: number;
    username?: string;
    firstName?: string;
    balance: number;
    subscriptionType: string;
    subscriptionExpiresAt?: string;
    hasActiveSubscription: boolean;
    createdAt: string;
    status: "ACTIVE" | "BANNED" | "INACTIVE";
    isBlocked?: boolean;
}

export interface AdminDashboardResponse {
    totalUsers: number;
    activeSubscriptions: number;
    totalBalanceRub: number;
}

export interface DeviceResponse {
    id: number;
    userId: number;
    deviceName: string;
    deviceType: string;
    uuid: string;
    isActive: boolean;
    created_at?: string;
    trafficUsedGb?: number;
    trafficLimitGb?: number;
}

export interface UserStatsResponse {
    totalTrafficBytes: number;
    totalReferrals: number;
    totalConnections: number;
}

export interface DeviceLimitStatus {
    userId: number;
    activeDevices: number;
    maxDevices: number;
    planName: string;
}

export interface VpnConfigResponse {
    subscriptionUrl: string;
    configs: Array<{
        countryEmoji: string;
        serverName: string;
        vlessLink: string;
    }>;
}

export interface ServerDto {
    id: number;
    name: string;
    location: string;
    countryCode: string;
    ipAddress: string;
    port: number;
    grpcPort?: number;
    realityPublicKey?: string;
    realityShortId?: string;
    realitySni?: string;
    maxConnections: number;
    currentConnections: number;
    healthScore?: number;
    avgLatencyMs?: number;
    isActive: boolean;
    lastHealthCheck?: string;
}

export interface CreateServerRequest {
    name: string;
    location: string;
    countryCode: string;
    ipAddress: string;
    port: number;
    grpcPort: number;
    realityPublicKey: string;
    realityShortId: string;
    realitySni: string;
    maxConnections: number;
}

export interface SystemHealthDto {
    status: "UP" | "DEGRADED" | "DOWN";
    services: Record<string, {
        status: string;
        details?: string;
        cpuUsage?: number;
        memoryUsedMb?: number;
        memoryMaxMb?: number;
        uptime?: number;
    }>;
}

export interface XrayUserStat {
    name: string;
    value: number;
}

export interface UserTrafficStatsDto {
    userId: string;
    totalUp: number;
    totalDown: number;
}

export interface ApiResponse<T> {
    data: T;
}

export interface VpnConfiguration {
    id: number;
    deviceId: number;
    userId: number; // Telegram ID or User ID reference
    serverId: number;
    vlessUuid: string;
    vlessLink: string;
    qrCodeBase64?: string;
    protocol: string; // e.g. "VLESS" or "HY2"
    status: "ACTIVE" | "REVOKED";
    createdAt: string;
    updatedAt: string;
    revokedAt?: string;
    lastUsedAt?: string;
    vlessLinksJson?: string; // stringified JSON list of regional config strings
    relayLinksJson?: string;
    deviceOs?: string;
    device_name?: string;
    linksBuiltAt?: string;
    hy2LinksJson?: string;
}

export interface VpnBanLog {
    id: number;
    userId: number;
    telegramId: number;
    username: string;
    action: "BAN" | "UNBAN";
    reason: string;
    adminUsername: string;
    createdAt: string;
}
