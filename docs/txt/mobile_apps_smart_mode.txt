# Telegram VPN - Mobile Apps & Smart Mode Implementation

## 1. MOBILE APPLICATIONS

### iOS App (Swift + SwiftUI)

```swift
// VPNManager.swift
import NetworkExtension
import Combine

class VPNManager: ObservableObject {
    @Published var connectionState: ConnectionState = .disconnected
    @Published var selectedServer: Server?
    @Published var smartModeEnabled: Bool = true
    @Published var currentMode: VPNMode = .smart
    @Published var statistics: ConnectionStatistics?
    
    private var tunnelManager: NETunnelProviderManager?
    private let apiClient = APIClient()
    
    enum VPNMode {
        case smart      // Только заблокированное
        case full       // Весь трафик
        case gaming     // Низкая латентность
        case streaming  // Оптимизация видео
    }
    
    enum ConnectionState {
        case disconnected
        case connecting
        case connected
        case disconnecting
    }
    
    // MARK: - Initialization
    
    init() {
        setupVPN()
        observeVPNStatus()
    }
    
    private func setupVPN() {
        NETunnelProviderManager.loadAllFromPreferences { managers, error in
            if let error = error {
                print("Error loading VPN config: \\(error)")
                return
            }
            
            if let manager = managers?.first {
                self.tunnelManager = manager
            } else {
                self.createVPNConfiguration()
            }
        }
    }
    
    private func createVPNConfiguration() {
        let manager = NETunnelProviderManager()
        manager.localizedDescription = "Telegram VPN"
        
        let proto = NETunnelProviderProtocol()
        proto.providerBundleIdentifier = "com.yourvpn.networkextension"
        proto.serverAddress = "Automatic"
        
        manager.protocolConfiguration = proto
        manager.isEnabled = true
        
        manager.saveToPreferences { error in
            if let error = error {
                print("Error saving VPN config: \\(error)")
            } else {
                self.tunnelManager = manager
            }
        }
    }
    
    // MARK: - Connection Management
    
    func connect() async throws {
        guard let manager = tunnelManager else {
            throw VPNError.notConfigured
        }
        
        connectionState = .connecting
        
        // 1. Получаем лучший сервер
        let server = try await apiClient.getBestServer(
            mode: currentMode,
            previousServer: selectedServer?.id
        )
        selectedServer = server
        
        // 2. Получаем VLESS конфигурацию
        let config = try await apiClient.getVLESSConfig(
            serverId: server.id,
            deviceId: DeviceInfo.current.id
        )
        
        // 3. Настройка VPN с параметрами
        let options: [String: Any] = [
            "server_address": server.address,
            "server_port": server.port,
            "vless_uuid": config.uuid,
            "reality_public_key": config.realityPublicKey,
            "reality_short_id": config.realityShortId,
            "reality_sni": "www.google.com",
            "mode": currentMode.rawValue,
            "blocked_domains": smartModeEnabled ? 
                try await getBlockedDomains() : []
        ]
        
        // 4. Запуск VPN
        try manager.connection.startVPNTunnel(options: options)
        
        connectionState = .connected
        
        // 5. Запуск мониторинга статистики
        startStatisticsMonitoring()
    }
    
    func disconnect() {
        guard let manager = tunnelManager else { return }
        
        connectionState = .disconnecting
        manager.connection.stopVPNTunnel()
        connectionState = .disconnected
        
        stopStatisticsMonitoring()
    }
    
    // MARK: - Smart Mode
    
    private func getBlockedDomains() async throws -> [String] {
        let domains = try await apiClient.getBlockedDomains()
        
        // Сохраняем локально для offline работы
        try BlockedDomainsCache.shared.save(domains)
        
        return domains
    }
    
    func checkDomainBlocked(_ domain: String) -> Bool {
        return BlockedDomainsCache.shared.isBlocked(domain)
    }
    
    // MARK: - Statistics
    
    private var statisticsTimer: Timer?
    
    private func startStatisticsMonitoring() {
        statisticsTimer = Timer.scheduledTimer(
            withTimeInterval: 1.0,
            repeats: true
        ) { [weak self] _ in
            self?.updateStatistics()
        }
    }
    
    private func stopStatisticsMonitoring() {
        statisticsTimer?.invalidate()
        statisticsTimer = nil
    }
    
    private func updateStatistics() {
        guard let session = tunnelManager?.connection as? NETunnelProviderSession 
        else { return }
        
        // Получаем статистику от Network Extension
        try? session.sendProviderMessage("getStats".data(using: .utf8)!) { response in
            guard let data = response,
                  let stats = try? JSONDecoder().decode(
                    ConnectionStatistics.self,
                    from: data
                  ) else { return }
            
            DispatchQueue.main.async {
                self.statistics = stats
            }
        }
    }
    
    // MARK: - Server Selection
    
    func switchServer(to server: Server) async throws {
        let wasConnected = connectionState == .connected
        
        if wasConnected {
            disconnect()
        }
        
        selectedServer = server
        
        if wasConnected {
            try await connect()
        }
    }
    
    func selectBestServer() async throws -> Server {
        return try await apiClient.getBestServer(
            mode: currentMode,
            previousServer: nil
        )
    }
}

// MARK: - SwiftUI Views

// MainView.swift
struct MainView: View {
    @StateObject private var vpnManager = VPNManager()
    @StateObject private var userManager = UserManager()
    @State private var showSettings = false
    @State private var showServers = false
    
    var body: some View {
        NavigationView {
            ZStack {
                // Background gradient
                LinearGradient(
                    colors: [.blue.opacity(0.1), .purple.opacity(0.1)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 24) {
                        // User info
                        UserInfoCard(user: userManager.currentUser)
                        
                        // Connection button
                        ConnectionButton(vpnManager: vpnManager)
                        
                        // Server info
                        if let server = vpnManager.selectedServer {
                            ServerInfoCard(server: server)
                                .onTapGesture {
                                    showServers = true
                                }
                        }
                        
                        // Mode selector
                        ModeSelector(
                            selectedMode: $vpnManager.currentMode,
                            smartMode: $vpnManager.smartModeEnabled
                        )
                        
                        // Statistics
                        if let stats = vpnManager.statistics {
                            StatisticsCard(stats: stats)
                        }
                        
                        // Quick actions
                        QuickActionsGrid()
                    }
                    .padding()
                }
            }
            .navigationTitle("VPN")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        showSettings = true
                    } label: {
                        Image(systemName: "gear")
                    }
                }
            }
            .sheet(isPresented: $showSettings) {
                SettingsView()
            }
            .sheet(isPresented: $showServers) {
                ServerSelectionView(vpnManager: vpnManager)
            }
        }
    }
}

// ConnectionButton.swift
struct ConnectionButton: View {
    @ObservedObject var vpnManager: VPNManager
    
    var body: some View {
        Button {
            Task {
                do {
                    if vpnManager.connectionState == .connected {
                        vpnManager.disconnect()
                    } else {
                        try await vpnManager.connect()
                    }
                } catch {
                    print("Connection error: \\(error)")
                }
            }
        } label: {
            ZStack {
                Circle()
                    .fill(buttonColor)
                    .frame(width: 200, height: 200)
                    .shadow(color: buttonColor.opacity(0.5), radius: 20)
                
                VStack(spacing: 8) {
                    Image(systemName: buttonIcon)
                        .font(.system(size: 50))
                        .foregroundColor(.white)
                    
                    Text(buttonText)
                        .font(.headline)
                        .foregroundColor(.white)
                }
            }
        }
        .disabled(vpnManager.connectionState == .connecting || 
                  vpnManager.connectionState == .disconnecting)
    }
    
    private var buttonColor: Color {
        switch vpnManager.connectionState {
        case .connected:
            return .green
        case .connecting, .disconnecting:
            return .orange
        case .disconnected:
            return .blue
        }
    }
    
    private var buttonIcon: String {
        switch vpnManager.connectionState {
        case .connected:
            return "checkmark.shield.fill"
        case .connecting, .disconnecting:
            return "arrow.triangle.2.circlepath"
        case .disconnected:
            return "play.fill"
        }
    }
    
    private var buttonText: String {
        switch vpnManager.connectionState {
        case .connected:
            return "Подключено"
        case .connecting:
            return "Подключение..."
        case .disconnecting:
            return "Отключение..."
        case .disconnected:
            return "Подключиться"
        }
    }
}

// ServerInfoCard.swift
struct ServerInfoCard: View {
    let server: Server
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(server.flag)
                    .font(.largeTitle)
                
                VStack(alignment: .leading) {
                    Text(server.name)
                        .font(.headline)
                    Text(server.location)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                // Latency indicator
                HStack(spacing: 4) {
                    Circle()
                        .fill(latencyColor)
                        .frame(width: 8, height: 8)
                    
                    Text("\\(server.latency)ms")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            
            // Load indicator
            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    Rectangle()
                        .fill(Color.gray.opacity(0.2))
                        .frame(height: 4)
                        .cornerRadius(2)
                    
                    Rectangle()
                        .fill(loadColor)
                        .frame(
                            width: geometry.size.width * CGFloat(server.load),
                            height: 4
                        )
                        .cornerRadius(2)
                }
            }
            .frame(height: 4)
            
            Text("Загрузка: \\(Int(server.load * 100))%")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.1), radius: 10)
    }
    
    private var latencyColor: Color {
        if server.latency < 30 {
            return .green
        } else if server.latency < 60 {
            return .orange
        } else {
            return .red
        }
    }
    
    private var loadColor: Color {
        if server.load < 0.5 {
            return .green
        } else if server.load < 0.8 {
            return .orange
        } else {
            return .red
        }
    }
}

// ModeSelector.swift
struct ModeSelector: View {
    @Binding var selectedMode: VPNManager.VPNMode
    @Binding var smartMode: Bool
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Режим работы")
                .font(.headline)
            
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ModeButton(
                        icon: "brain",
                        title: "Smart",
                        subtitle: "Только заблокированное",
                        isSelected: selectedMode == .smart,
                        action: { selectedMode = .smart }
                    )
                    
                    ModeButton(
                        icon: "globe",
                        title: "Full VPN",
                        subtitle: "Весь трафик",
                        isSelected: selectedMode == .full,
                        action: { selectedMode = .full }
                    )
                    
                    ModeButton(
                        icon: "gamecontroller",
                        title: "Gaming",
                        subtitle: "Низкая латентность",
                        isSelected: selectedMode == .gaming,
                        action: { selectedMode = .gaming }
                    )
                    
                    ModeButton(
                        icon: "play.tv",
                        title: "Streaming",
                        subtitle: "Для видео",
                        isSelected: selectedMode == .streaming,
                        action: { selectedMode = .streaming }
                    )
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.1), radius: 10)
    }
}

// StatisticsCard.swift
struct StatisticsCard: View {
    let stats: ConnectionStatistics
    
    var body: some View {
        VStack(spacing: 16) {
            Text("Статистика")
                .font(.headline)
                .frame(maxWidth: .infinity, alignment: .leading)
            
            HStack(spacing: 20) {
                StatItem(
                    icon: "arrow.down",
                    title: "Получено",
                    value: formatBytes(stats.bytesReceived),
                    color: .blue
                )
                
                StatItem(
                    icon: "arrow.up",
                    title: "Отправлено",
                    value: formatBytes(stats.bytesSent),
                    color: .green
                )
            }
            
            HStack(spacing: 20) {
                StatItem(
                    icon: "clock",
                    title: "Время",
                    value: formatDuration(stats.duration),
                    color: .orange
                )
                
                StatItem(
                    icon: "chart.bar",
                    title: "Скорость",
                    value: "\\(stats.currentSpeed) Mbps",
                    color: .purple
                )
            }
            
            if stats.savedBySmartMode > 0 {
                HStack {
                    Image(systemName: "brain.head.profile")
                        .foregroundColor(.green)
                    
                    Text("Smart Mode сэкономил \\(formatBytes(stats.savedBySmartMode))")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.1), radius: 10)
    }
    
    private func formatBytes(_ bytes: Int64) -> String {
        let formatter = ByteCountFormatter()
        formatter.countStyle = .binary
        return formatter.string(fromByteCount: bytes)
    }
    
    private func formatDuration(_ seconds: Int) -> String {
        let hours = seconds / 3600
        let minutes = (seconds % 3600) / 60
        let secs = seconds % 60
        
        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, secs)
        } else {
            return String(format: "%d:%02d", minutes, secs)
        }
    }
}
```

---

## 2. ANDROID APP (Kotlin + Jetpack Compose)

```kotlin
// VpnManager.kt
package com.yourvpn.android

import android.content.Context
import android.net.VpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VpnManager(private val context: Context) {
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _selectedServer = MutableStateFlow<Server?>(null)
    val selectedServer: StateFlow<Server?> = _selectedServer.asStateFlow()
    
    private val _currentMode = MutableStateFlow(VpnMode.SMART)
    val currentMode: StateFlow<VpnMode> = _currentMode.asStateFlow()
    
    private val _statistics = MutableStateFlow<ConnectionStatistics?>(null)
    val statistics: StateFlow<ConnectionStatistics?> = _statistics.asStateFlow()
    
    private val apiClient = ApiClient()
    private var vpnService: CustomVpnService? = null
    
    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        object Disconnecting : ConnectionState()
    }
    
    enum class VpnMode {
        SMART,      // Только заблокированное
        FULL,       // Весь трафик
        GAMING,     // Низкая латентность
        STREAMING   // Оптимизация видео
    }
    
    suspend fun connect() {
        _connectionState.value = ConnectionState.Connecting
        
        try {
            // 1. Получаем лучший сервер
            val server = apiClient.getBestServer(
                mode = _currentMode.value,
                previousServerId = _selectedServer.value?.id
            )
            _selectedServer.value = server
            
            // 2. Получаем VLESS конфигурацию
            val config = apiClient.getVLESSConfig(
                serverId = server.id,
                deviceId = DeviceInfo.getDeviceId(context)
            )
            
            // 3. Запускаем VPN service
            val intent = VpnService.prepare(context)
            if (intent != null) {
                throw VpnException("VPN permission required")
            }
            
            val serviceIntent = Intent(context, CustomVpnService::class.java).apply {
                putExtra("server_address", server.address)
                putExtra("server_port", server.port)
                putExtra("vless_uuid", config.uuid)
                putExtra("reality_public_key", config.realityPublicKey)
                putExtra("reality_short_id", config.realityShortId)
                putExtra("mode", _currentMode.value.name)
                putExtra("blocked_domains", getBlockedDomains())
            }
            
            context.startService(serviceIntent)
            
            _connectionState.value = ConnectionState.Connected
            
            // 4. Запускаем мониторинг статистики
            startStatisticsMonitoring()
            
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Disconnected
            throw e
        }
    }
    
    fun disconnect() {
        _connectionState.value = ConnectionState.Disconnecting
        
        val intent = Intent(context, CustomVpnService::class.java)
        context.stopService(intent)
        
        _connectionState.value = ConnectionState.Disconnected
        stopStatisticsMonitoring()
    }
    
    private suspend fun getBlockedDomains(): List<String> {
        val domains = apiClient.getBlockedDomains()
        
        // Сохраняем локально
        BlockedDomainsCache.save(context, domains)
        
        return domains
    }
    
    private var statisticsJob: Job? = null
    
    private fun startStatisticsMonitoring() {
        statisticsJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val stats = vpnService?.getStatistics()
                _statistics.value = stats
                delay(1000)
            }
        }
    }
    
    private fun stopStatisticsMonitoring() {
        statisticsJob?.cancel()
    }
}

// CustomVpnService.kt
class CustomVpnService : VpnService() {
    
    private var vpnInterface: ParcelFileDescriptor? = null
    private var connectionJob: Job? = null
    private val statistics = MutableConnectionStatistics()
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val serverAddress = intent?.getStringExtra("server_address") ?: return START_NOT_STICKY
        val serverPort = intent.getIntExtra("server_port", 443)
        val vlessUuid = intent.getStringExtra("vless_uuid") ?: return START_NOT_STICKY
        val mode = VpnMode.valueOf(intent.getStringExtra("mode") ?: "SMART")
        val blockedDomains = intent.getStringArrayListExtra("blocked_domains") ?: emptyList()
        
        // Создаем notification для foreground service
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        // Настраиваем VPN
        val builder = Builder()
            .setSession("Telegram VPN")
            .addAddress("10.0.0.2", 24)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("1.1.1.1")
            .addDnsServer("8.8.8.8")
            .setMtu(1400)
            .setBlocking(false)
        
        // В Smart Mode добавляем только заблокированные домены
        if (mode == VpnMode.SMART) {
            // Добавляем маршруты только для заблокированных
            blockedDomains.forEach { domain ->
                // Резолвим IP и добавляем route
                val ips = resolveDomain(domain)
                ips.forEach { ip ->
                    builder.addRoute(ip, 32)
                }
            }
        }
        
        vpnInterface = builder.establish()
        
        // Запускаем обработку пакетов
        connectionJob = CoroutineScope(Dispatchers.IO).launch {
            processPackets(serverAddress, serverPort, vlessUuid)
        }
        
        return START_STICKY
    }
    
    private suspend fun processPackets(
        serverAddress: String,
        serverPort: Int,
        vlessUuid: String
    ) {
        val inputStream = FileInputStream(vpnInterface!!.fileDescriptor)
        val outputStream = FileOutputStream(vpnInterface!!.fileDescriptor)
        
        // Создаем соединение с сервером через VLESS
        val vlessConnection = VLESSConnection(serverAddress, serverPort, vlessUuid)
        vlessConnection.connect()
        
        coroutineScope {
            // Outgoing packets (от устройства к серверу)
            launch {
                val buffer = ByteArray(32767)
                while (isActive) {
                    val length = inputStream.read(buffer)
                    if (length > 0) {
                        val packet = buffer.copyOf(length)
                        
                        // Отправляем через VLESS
                        vlessConnection.sendPacket(packet)
                        
                        statistics.bytesSent += length
                    }
                }
            }
            
            // Incoming packets (от сервера к устройству)
            launch {
                while (isActive) {
                    val packet = vlessConnection.receivePacket()
                    if (packet != null) {
                        outputStream.write(packet)
                        
                        statistics.bytesReceived += packet.size
                    }
                }
            }
        }
    }
    
    fun getStatistics(): ConnectionStatistics {
        return statistics.toConnectionStatistics()
    }
    
    override fun onDestroy() {
        connectionJob?.cancel()
        vpnInterface?.close()
        super.onDestroy()
    }
}

// Compose UI
@Composable
fun MainScreen(vpnManager: VpnManager) {
    val connectionState by vpnManager.connectionState.collectAsState()
    val selectedServer by vpnManager.selectedServer.collectAsState()
    val currentMode by vpnManager.currentMode.collectAsState()
    val statistics by vpnManager.statistics.collectAsState()
    
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VPN") },
                actions = {
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User info card
            UserInfoCard()
            
            // Connection button
            ConnectionButton(
                connectionState = connectionState,
                onClick = {
                    coroutineScope.launch {
                        if (connectionState == VpnManager.ConnectionState.Connected) {
                            vpnManager.disconnect()
                        } else {
                            vpnManager.connect()
                        }
                    }
                }
            )
            
            // Server info
            selectedServer?.let { server ->
                ServerInfoCard(server = server)
            }
            
            // Mode selector
            ModeSelector(
                selectedMode = currentMode,
                onModeSelected = { mode ->
                    vpnManager.setMode(mode)
                }
            )
            
            // Statistics
            statistics?.let { stats ->
                StatisticsCard(stats = stats)
            }
        }
    }
}

@Composable
fun ConnectionButton(
    connectionState: VpnManager.ConnectionState,
    onClick: () -> Unit
) {
    val color = when (connectionState) {
        VpnManager.ConnectionState.Connected -> Color.Green
        VpnManager.ConnectionState.Connecting,
        VpnManager.ConnectionState.Disconnecting -> Color.Orange
        VpnManager.ConnectionState.Disconnected -> Color.Blue
    }
    
    val text = when (connectionState) {
        VpnManager.ConnectionState.Connected -> "Подключено"
        VpnManager.ConnectionState.Connecting -> "Подключение..."
        VpnManager.ConnectionState.Disconnecting -> "Отключение..."
        VpnManager.ConnectionState.Disconnected -> "Подключиться"
    }
    
    val icon = when (connectionState) {
        VpnManager.ConnectionState.Connected -> Icons.Default.CheckCircle
        VpnManager.ConnectionState.Connecting,
        VpnManager.ConnectionState.Disconnecting -> Icons.Default.Refresh
        VpnManager.ConnectionState.Disconnected -> Icons.Default.PlayArrow
    }
    
    Button(
        onClick = onClick,
        modifier = Modifier
            .size(200.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(backgroundColor = color),
        enabled = connectionState !is VpnManager.ConnectionState.Connecting &&
                  connectionState !is VpnManager.ConnectionState.Disconnecting
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
```

---

## 3. SMART MODE - ДЕТАЛЬНАЯ РЕАЛИЗАЦИЯ

### База заблокированных доменов

```python
# blocked_domains_updater.py
import asyncio
import aiohttp
from datetime import datetime
from sqlalchemy.ext.asyncio import AsyncSession

class BlockedDomainsUpdater:
    """
    Автоматическое обновление списка заблокированных доменов
    """
    
    SOURCES = [
        # Роскомнадзор dump
        "https://reestr.rublacklist.net/api/v2/domains/json",
        
        # Community-maintained списки
        "https://github.com/zapret-info/z-i/raw/master/dump.csv",
        
        # Анти-цензура проекты
        "https://antifilter.download/list/domains.lst"
    ]
    
    def __init__(self, db_session: AsyncSession):
        self.db = db_session
    
    async def update_domains(self):
        """
        Обновляет базу заблокированных доменов
        """
        all_domains = set()
        
        # Загружаем из всех источников
        for source in self.SOURCES:
            try:
                domains = await self.fetch_from_source(source)
                all_domains.update(domains)
                print(f"Loaded {len(domains)} domains from {source}")
            except Exception as e:
                print(f"Error loading from {source}: {e}")
        
        # Сохраняем в БД
        await self.save_to_database(all_domains)
        
        print(f"Total domains updated: {len(all_domains)}")
    
    async def fetch_from_source(self, url: str) -> set:
        """
        Загружает список доменов из источника
        """
        async with aiohttp.ClientSession() as session:
            async with session.get(url) as response:
                content = await response.text()
                
                # Парсинг зависит от формата источника
                domains = set()
                
                for line in content.split('\\n'):
                    line = line.strip()
                    if line and not line.startswith('#'):
                        # Извлекаем домен
                        domain = self.extract_domain(line)
                        if domain:
                            domains.add(domain)
                
                return domains
    
    def extract_domain(self, line: str) -> str:
        """
        Извлекает домен из строки
        """
        # Удаляем протоколы, пути и т.д.
        line = line.split('|')[0]  # Для dump.csv формата
        line = line.replace('http://', '').replace('https://', '')
        line = line.split('/')[0]  # Удаляем пути
        line = line.split(':')[0]  # Удаляем порты
        
        # Проверка валидности домена
        if '.' in line and len(line) > 3:
            return line.lower()
        
        return None
    
    async def save_to_database(self, domains: set):
        """
        Сохраняет домены в БД
        """
        # Обновляем существующие
        await self.db.execute(
            """
            UPDATE blocked_domains
            SET is_blocked = false
            WHERE domain NOT IN :domains
            """,
            {"domains": tuple(domains)}
        )
        
        # Добавляем новые
        for domain in domains:
            await self.db.execute(
                """
                INSERT INTO blocked_domains (domain, is_blocked, last_checked)
                VALUES (:domain, true, :now)
                ON CONFLICT (domain) DO UPDATE
                SET is_blocked = true, last_checked = :now
                """,
                {"domain": domain, "now": datetime.now()}
            )
        
        await self.db.commit()
    
    async def check_domain_blocked(self, domain: str) -> bool:
        """
        Проверяет заблокирован ли домен
        """
        result = await self.db.execute(
            """
            SELECT is_blocked
            FROM blocked_domains
            WHERE domain = :domain
            """,
            {"domain": domain}
        )
        
        row = result.first()
        if row:
            return row[0]
        
        # Если домена нет в базе, проверяем в реальном времени
        is_blocked = await self.real_time_check(domain)
        
        # Сохраняем результат
        await self.db.execute(
            """
            INSERT INTO blocked_domains (domain, is_blocked, last_checked, auto_detected)
            VALUES (:domain, :blocked, :now, true)
            """,
            {"domain": domain, "blocked": is_blocked, "now": datetime.now()}
        )
        await self.db.commit()
        
        return is_blocked
    
    async def real_time_check(self, domain: str) -> bool:
        """
        Реальное время проверка доступности домена
        """
        try:
            async with aiohttp.ClientSession() as session:
                async with session.get(
                    f"http://{domain}",
                    timeout=aiohttp.ClientTimeout(total=5)
                ) as response:
                    # Если получили ответ - домен доступен
                    return False
        except:
            # Если ошибка - вероятно заблокирован
            return True

# Автоматическое обновление каждые 6 часов
async def scheduled_update():
    while True:
        try:
            updater = BlockedDomainsUpdater(get_db_session())
            await updater.update_domains()
        except Exception as e:
            print(f"Update error: {e}")
        
        await asyncio.sleep(6 * 3600)  # 6 часов
```

### Machine Learning предсказание блокировок

```python
# blocking_prediction.py
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.preprocessing import LabelEncoder
import pandas as pd

class BlockingPredictor:
    """
    ML модель для предсказания будущих блокировок
    """
    
    def __init__(self):
        self.model = RandomForestClassifier(n_estimators=100)
        self.label_encoder = LabelEncoder()
        self.is_trained = False
    
    def train(self, historical_data: pd.DataFrame):
        """
        Обучение модели на исторических данных
        
        historical_data должен содержать:
        - domain: домен
        - category: категория (news, social, vpn, etc.)
        - tld: top-level domain (.com, .org, etc.)
        - popularity: популярность домена
        - previous_blocks: количество предыдущих блокировок
        - days_since_registration: дней с регистрации домена
        - blocked: 0/1 (таргет)
        """
        
        # Feature engineering
        features = self.extract_features(historical_data)
        
        X = features.drop('blocked', axis=1)
        y = features['blocked']
        
        # Обучение
        self.model.fit(X, y)
        self.is_trained = True
        
        # Оценка модели
        score = self.model.score(X, y)
        print(f"Model accuracy: {score:.2%}")
    
    def predict_blocking_probability(self, domain: str) -> float:
        """
        Предсказывает вероятность блокировки домена
        """
        if not self.is_trained:
            return 0.0
        
        # Собираем features для домена
        features = self.get_domain_features(domain)
        
        # Предсказание
        proba = self.model.predict_proba([features])[0][1]
        
        return proba
    
    def extract_features(self, data: pd.DataFrame) -> pd.DataFrame:
        """
        Извлечение признаков из данных
        """
        features = data.copy()
        
        # Encode категорий
        features['category_encoded'] = self.label_encoder.fit_transform(
            features['category']
        )
        
        # TLD features
        features['is_com'] = (features['tld'] == '.com').astype(int)
        features['is_org'] = (features['tld'] == '.org').astype(int)
        features['is_ru'] = (features['tld'] == '.ru').astype(int)
        
        # Popularity buckets
        features['high_popularity'] = (features['popularity'] > 10000).astype(int)
        
        return features
    
    def get_domain_features(self, domain: str) -> list:
        """
        Получение признаков для конкретного домена
        """
        # Здесь должна быть логика сбора фактических данных
        # Для примера возвращаем mock данные
        
        return [
            1,  # category_encoded
            0,  # is_com
            0,  # is_org
            1,  # is_ru
            5000,  # popularity
            0,  # previous_blocks
            365,  # days_since_registration
            1  # high_popularity
        ]
```

Готово! Создам финальный deployment документ?
