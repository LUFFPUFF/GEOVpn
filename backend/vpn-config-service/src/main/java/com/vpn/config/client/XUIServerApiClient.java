package com.vpn.config.client;

import com.vpn.common.dto.ServerDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class XUIServerApiClient {

    private final ConcurrentHashMap<String, String> sessionCookies = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> resolvedBaseUrls = new ConcurrentHashMap<>();

    private final RestTemplate restTemplate;

    public XUIServerApiClient(RestTemplateBuilder builder) {
        this.restTemplate = createTrustAllRestTemplate();
    }

    public static class ClientPanelInfo {
        public String email;
        public List<Integer> inboundIds;

        public ClientPanelInfo(String email, List<Integer> inboundIds) {
            this.email = email;
            this.inboundIds = inboundIds;
        }
    }

    private RestTemplate createTrustAllRestTemplate() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }}, new SecureRandom());

            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(java.net.HttpURLConnection connection, String httpMethod) throws java.io.IOException {
                    if (connection instanceof HttpsURLConnection) {
                        ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
                        ((HttpsURLConnection) connection).setHostnameVerifier((hostname, session) -> true);
                    }
                    connection.setInstanceFollowRedirects(false);
                    super.prepareConnection(connection, httpMethod);
                }
            };
            requestFactory.setConnectTimeout(5000);
            requestFactory.setReadTimeout(10000);

            return new RestTemplate(requestFactory);
        } catch (Exception e) {
            log.error("Failed to initialize trust-all SSL context", e);
            return new RestTemplateBuilder().setConnectTimeout(Duration.ofSeconds(5)).build();
        }
    }

    /**
     * Динамическое определение базового URL.
     * Пытается использовать HTTPS. Если сервер не поддерживает SSL, переключается на HTTP.
     */
    private String buildBaseUrl(ServerDto server) {
        String cacheKey = server.getIpAddress() + ":" + server.getPanelPort();

        return resolvedBaseUrls.computeIfAbsent(cacheKey, key -> {
            String httpsUrl = constructUrl(server, "https://");
            String httpUrl = constructUrl(server, "http://");

            try {
                restTemplate.exchange(httpsUrl + "/login", HttpMethod.GET, null, String.class);
                return httpsUrl;
            } catch (org.springframework.web.client.ResourceAccessException e) {
                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (msg.contains("ssl") || msg.contains("unrecognized") || msg.contains("certificate")) {
                    log.info("Detected plain HTTP (no SSL) for panel on server {}", server.getName());
                    return httpUrl;
                }
                return httpsUrl;
            } catch (Exception e) {
                return httpsUrl;
            }
        });
    }

    private String constructUrl(ServerDto server, String scheme) {
        int port = (server.getPanelPort() != null && server.getPanelPort() != 0)
                ? server.getPanelPort() : 8080;
        String path = server.getPanelPath();
        String ip   = server.getIpAddress();

        StringBuilder sb = new StringBuilder(scheme).append(ip).append(":").append(port);
        if (path != null && !path.isBlank()
                && !path.equalsIgnoreCase("null")
                && !path.equalsIgnoreCase("<null>")) {
            if (!path.startsWith("/")) sb.append("/");
            sb.append(path);
        }
        String url = sb.toString();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private void validatePanelCredentials(ServerDto server) {
        if (server.getPanelInboundId() == null && server.getTcpInboundId() == null && server.getWsInboundId() == null) {
            throw new IllegalStateException("Server '" + server.getName() + "' has no inbound ID configured");
        }
        if (hasApiToken(server)) return;
        if (server.getPanelUsername() == null || server.getPanelUsername().isBlank()) {
            throw new IllegalStateException("Server '" + server.getName() + "' has no panelUsername configured");
        }
    }

    private void validatePanelCredentialsForInbound(ServerDto server, Integer inboundId) {
        if (inboundId == null) {
            throw new IllegalStateException("Target Inbound ID cannot be null for server '" + server.getName() + "'");
        }
        if (hasApiToken(server)) return;
        if (server.getPanelUsername() == null || server.getPanelUsername().isBlank()) {
            throw new IllegalStateException("Server '" + server.getName() + "' has no panelUsername configured");
        }
    }

    private boolean hasApiToken(ServerDto server) {
        return server.getApiToken() != null && !server.getApiToken().isBlank();
    }

    private void ensureAuthenticated(ServerDto server, String baseUrl) {
        if (hasApiToken(server)) return;
        if (!sessionCookies.containsKey(server.getIpAddress())) {
            login(server, baseUrl);
        }
    }

    private void login(ServerDto server, String baseUrl) {
        String loginUrl = baseUrl + "/login";
        log.info(">>>> [XUI LOGIN] Server: {}, URL: {}, User: {}", server.getName(), loginUrl, server.getPanelUsername());

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<?> entity;

        if (server.isRelay()) {
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> jsonBody = new HashMap<>();
            jsonBody.put("username", server.getPanelUsername());
            jsonBody.put("password", server.getPanelPassword());
            entity = new HttpEntity<>(jsonBody, headers);
        } else {
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("username", server.getPanelUsername());
            form.add("password", server.getPanelPassword());
            entity = new HttpEntity<>(form, headers);
        }

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(loginUrl, entity, String.class);
            String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
            if (cookie == null) {
                throw new RuntimeException("No cookie returned from " + server.getIpAddress());
            }
            sessionCookies.put(server.getIpAddress(), cookie);
            log.info("<<<< [XUI LOGIN] SUCCESS for {}", server.getName());
        } catch (Exception e) {
            log.error("!!!! [XUI LOGIN] FAILED for {}: {}", server.getName(), e.getMessage());
            throw new RuntimeException("Login failed: " + e.getMessage());
        }
    }

    private HttpHeaders buildAuthHeaders(ServerDto server) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (hasApiToken(server)) {
            headers.setBearerAuth(server.getApiToken());
        } else {
            headers.add(HttpHeaders.COOKIE, sessionCookies.get(server.getIpAddress()));
        }
        return headers;
    }

    private void sendRequest(ServerDto server, String url, Object body) {
        HttpHeaders headers = buildAuthHeaders(server);
        ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);

        if (response.getStatusCode() == HttpStatus.FOUND || response.getStatusCode() == HttpStatus.MOVED_PERMANENTLY) {
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Authentication failed (Redirected)");
        }
    }

    private void executeWithRetry(ServerDto server, String baseUrl, String url, Object body) {
        try {
            sendRequest(server, url, body);
        } catch (HttpClientErrorException.Unauthorized e) {
            if (hasApiToken(server)) {
                log.error("API Token rejected (401/302) for server {}", server.getName());
                throw e;
            }
            sessionCookies.remove(server.getIpAddress());
            login(server, baseUrl);
            sendRequest(server, url, body);
        }
    }

    public void addClientWithToken(ServerDto server, List<Integer> inboundIds, String uuid, String email, String flow, String apiToken) {
        if (inboundIds == null || inboundIds.isEmpty()) {
            throw new IllegalStateException("Target Inbound IDs list cannot be empty for server '" + server.getName() + "'");
        }
        if (!hasApiToken(server) && (server.getPanelUsername() == null || server.getPanelUsername().isBlank())) {
            throw new IllegalStateException("Server '" + server.getName() + "' has no panelUsername configured");
        }

        String baseUrl = buildBaseUrl(server);
        ensureAuthenticated(server, baseUrl);

        String url = baseUrl + "/panel/api/clients/add";

        Map<String, Object> clientFields = new HashMap<>();
        clientFields.put("email", email);
        clientFields.put("id", uuid);
        clientFields.put("flow", (flow == null) ? "" : flow);
        clientFields.put("limitIp", 0);
        clientFields.put("enable", true);

        Map<String, Object> body = new HashMap<>();
        body.put("client", clientFields);
        body.put("inboundIds", inboundIds);

        executeWithRetry(server, baseUrl, url, body);
    }

    public void addClient(ServerDto server, String uuid, String email, int limitIp, String flow) {
        validatePanelCredentials(server);
        Integer targetInboundId = server.getTcpInboundId() != null ? server.getTcpInboundId() : server.getPanelInboundId();
        addClientWithToken(server, Collections.singletonList(targetInboundId), uuid, email, flow, server.getApiToken());
    }

    public void removeClient(ServerDto server, String uuid) {
        validatePanelCredentials(server);
        String baseUrl = buildBaseUrl(server);
        ensureAuthenticated(server, baseUrl);

        String email = findEmailByUuidWithRetry(server, baseUrl, uuid);
        if (email != null && !email.isBlank()) {
            String url = baseUrl + "/panel/api/clients/del/" + email;
            executeWithRetry(server, baseUrl, url, null);
            log.info("Successfully deleted client by email {} on server {}", email, server.getName());
        } else {
            log.warn("Client with UUID {} not found on server {}, skipping deletion", uuid, server.getName());
        }
    }

    @SuppressWarnings("unchecked")
    private String findEmailByUuid(ServerDto server, String baseUrl, String uuid) {
        String url = baseUrl + "/panel/api/clients/list";
        HttpHeaders headers = buildAuthHeaders(server);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getBody() != null && Boolean.TRUE.equals(response.getBody().get("success"))) {
                List<Map<String, Object>> clients = (List<Map<String, Object>>) response.getBody().get("obj");
                if (clients != null) {
                    for (Map<String, Object> client : clients) {
                        Object idVal   = client.get("id");
                        Object uuidVal = client.get("uuid");
                        if ((idVal   != null && idVal.toString().equalsIgnoreCase(uuid)) ||
                                (uuidVal != null && uuidVal.toString().equalsIgnoreCase(uuid))) {
                            return (String) client.get("email");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch clients list from server {}: {}", server.getName(), e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private ClientPanelInfo findClientInfoByUuid(ServerDto server, String baseUrl, String uuid) {
        String url = baseUrl + "/panel/api/clients/list";
        HttpHeaders headers = buildAuthHeaders(server);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getBody() != null && Boolean.TRUE.equals(response.getBody().get("success"))) {
                List<Map<String, Object>> clients = (List<Map<String, Object>>) response.getBody().get("obj");
                if (clients != null) {
                    for (Map<String, Object> client : clients) {
                        Object idVal   = client.get("id");
                        Object uuidVal = client.get("uuid");
                        if ((idVal != null && idVal.toString().equalsIgnoreCase(uuid)) ||
                                (uuidVal != null && uuidVal.toString().equalsIgnoreCase(uuid))) {

                            String email = (String) client.get("email");
                            List<Integer> inboundIds = (List<Integer>) client.get("inboundIds");

                            if (inboundIds == null) inboundIds = Collections.emptyList();

                            return new ClientPanelInfo(email, inboundIds);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch clients list from server {}: {}", server.getName(), e.getMessage());
        }
        return null;
    }

    /**
     * Получает статистику трафика для конкретного клиента по его Email.
     * Возвращает Map с ключами "up", "down", "total" в байтах.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Map<String, Long>> getAllClientsTraffic(ServerDto server) {
        String baseUrl = buildBaseUrl(server);
        ensureAuthenticated(server, baseUrl);

        String url = baseUrl + "/panel/api/clients/list";
        HttpHeaders headers = buildAuthHeaders(server);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getBody() != null && Boolean.TRUE.equals(response.getBody().get("success"))) {
                List<Map<String, Object>> clients = (List<Map<String, Object>>) response.getBody().get("obj");
                if (clients != null) {
                    Map<String, Map<String, Long>> result = new HashMap<>();
                    for (Map<String, Object> client : clients) {
                        String email = (String) client.get("email");
                        Map<String, Object> traffic = (Map<String, Object>) client.get("traffic");

                        if (email != null && traffic != null) {
                            Map<String, Long> stats = new HashMap<>();
                            stats.put("up", ((Number) traffic.get("up")).longValue());
                            stats.put("down", ((Number) traffic.get("down")).longValue());
                            result.put(email, stats);
                        }
                    }
                    return result;
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch bulk traffic list from server {}: {}", server.getName(), e.getMessage());
        }
        return Collections.emptyMap();
    }

    /**
     * Запрашивает последние логи Xray, отфильтрованные по email пользователя.
     * Возвращает сырой текстовый лог.
     */
    public String getXrayLogs(ServerDto server, int count, String emailFilter) {
        String baseUrl = buildBaseUrl(server);
        ensureAuthenticated(server, baseUrl);

        String url = baseUrl + "/panel/api/server/xraylogs/" + count;
        HttpHeaders headers = buildAuthHeaders(server);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("filter", emailFilter);
        form.add("showDirect", "false");
        form.add("showBlocked", "true");
        form.add("showProxy", "true");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getBody() != null && Boolean.TRUE.equals(response.getBody().get("success"))) {
                return (String) response.getBody().get("obj");
            }
        } catch (Exception e) {
            log.error("Failed to fetch Xray logs for filter {} from server {}: {}", emailFilter, server.getName(), e.getMessage());
        }
        return "";
    }

    private ClientPanelInfo findClientInfoByUuidWithRetry(ServerDto server, String baseUrl, String uuid) {
        try {
            return findClientInfoByUuid(server, baseUrl, uuid);
        } catch (HttpClientErrorException.Unauthorized e) {
            if (hasApiToken(server)) {
                log.error("API Token rejected (401) while looking up client on server {}", server.getIpAddress());
                throw e;
            }
            sessionCookies.remove(server.getIpAddress());
            login(server, baseUrl);
            return findClientInfoByUuid(server, baseUrl, uuid);
        }
    }

    /**
     * Привязывает уже существующего клиента к дополнительным инбаундам.
     * Использует эндпоинт POST /panel/api/clients/:email/attach
     */
    private void attachClientToInbounds(ServerDto server, String baseUrl, String email, List<Integer> inboundsToAttach) {
        if (inboundsToAttach.isEmpty()) return;

        String url = baseUrl + "/panel/api/clients/" + email + "/attach";

        Map<String, Object> body = new HashMap<>();
        body.put("inboundIds", inboundsToAttach);

        executeWithRetry(server, baseUrl, url, body);
        log.info("Successfully attached client '{}' to extra inbounds {} on server {}", email, inboundsToAttach, server.getName());
    }

    /**
     * ГЛАВНЫЙ МЕТОД ДЛЯ СВЕРКИ: Проверяет существование клиента и его инбаунды.
     * Если клиента нет — создает его (сразу во всех инбаундах).
     * Если клиент есть, но не во всех инбаундах — допривязывает его.
     * Возвращает true, если были внесены какие-то изменения.
     */
    public boolean checkAndRestoreOrAttachClient(ServerDto server, List<Integer> targetInboundIds, String uuid, String expectedEmail, String flow) {
        if (targetInboundIds == null || targetInboundIds.isEmpty()) return false;

        String baseUrl = buildBaseUrl(server);
        ensureAuthenticated(server, baseUrl);

        ClientPanelInfo panelInfo = findClientInfoByUuidWithRetry(server, baseUrl, uuid);

        if (panelInfo == null || panelInfo.email == null || panelInfo.email.isBlank()) {
            log.info("Client UUID {} not found on server {}. Restoring to inbounds {}...", uuid, server.getName(), targetInboundIds);
            addClientWithToken(server, targetInboundIds, uuid, expectedEmail, flow, server.getApiToken());
            return true;
        }

        List<Integer> currentInbounds = panelInfo.inboundIds;
        List<Integer> missingInbounds = new ArrayList<>();

        for (Integer targetId : targetInboundIds) {
            if (!currentInbounds.contains(targetId)) {
                missingInbounds.add(targetId);
            }
        }

        if (!missingInbounds.isEmpty()) {
            log.info("Client UUID {} exists on server {}, but missing in inbounds {}. Attaching...", uuid, server.getName(), missingInbounds);
            attachClientToInbounds(server, baseUrl, panelInfo.email, missingInbounds);
            return true;
        }

        return false;
    }


    private String findEmailByUuidWithRetry(ServerDto server, String baseUrl, String uuid) {
        try {
            return findEmailByUuid(server, baseUrl, uuid);
        } catch (HttpClientErrorException.Unauthorized e) {
            if (hasApiToken(server)) {
                log.error("API Token rejected (401) while looking up email on server {}", server.getName());
                throw e;
            }
            sessionCookies.remove(server.getIpAddress());
            login(server, baseUrl);
            return findEmailByUuid(server, baseUrl, uuid);
        }
    }
}