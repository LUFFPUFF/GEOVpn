export interface UserResponse {
    id: number;
    telegramId: number;
    username?: string;
    firstName?: string;
    balance: number;
    subscriptionType: string;
    subscriptionExpiresAt?: string;
    referralCode?: string;
    createdAt: string;
    lastActiveAt?: string;
    hasActiveSubscription: boolean;
    isBanned: boolean;
    banReason?: string;
    isChannelMember?: boolean;
}

export interface AdminDashboardResponse {
    totalUsers: number;
    activeSubscriptions: number;
    totalServers: number;
    activeServers: number;
    totalTrafficGb: number;
    totalBalanceRub: number;
}

export interface DeviceResponse {
    id: number;
    deviceName: string;
    deviceType: string;
    uuid: string;
    isActive: boolean;
    lastConnectedAt?: string;
    createdAt?: string;
}

export interface UserStatsResponse {
    telegramId: number;
    balance: number;
    totalDevices: number;
    activeDevices: number;
    totalReferrals: number;
    totalReferralEarnings: number;
    totalSpentKopecks: number;
    totalConnections: number;
    totalTrafficBytes: number;
}

export interface DeviceLimitStatus {
    userId: number;
    maxDevices: number;
    activeDevices: number;
    remainingSlots: number;
    limitReached: boolean;
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

export interface DirectLink {
    serverId: number;
    serverName: string;
    countryCode: string;
    link: string;
    avgLatencyMs?: number;
    healthScore?: number;
    displayName?: string;
}

export interface RelayLink {
    serverId: number;
    serverName: string;
    countryCode: string;
    link: string;
    relayPriority: number;
    description?: string;
}

export interface AdminConfigDetailResponse {
    id: number;
    deviceId: number;
    userId: number;
    vlessUuid: string;
    status: string;
    deviceOs: string;
    deviceName: string;
    vlessLinks: DirectLink[];
    relayLinks: RelayLink[];
    hy2Links: string[];
}

export interface TransactionResponse {
    id: number;
    userId: number;
    amount: number;
    transactionType: string;
    paymentMethod?: string;
    status: "PENDING" | "COMPLETED" | "SUCCESS" | "FAILED";
    externalTransactionId?: string;
    paymentProvider?: string;
    description?: string;
    createdAt: string;
    completedAt?: string;
}

export interface RevenueStat {
    date: string;
    amountRub: number;
}
