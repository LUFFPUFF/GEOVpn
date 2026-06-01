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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class XUIServerApiClient {

    private final ConcurrentHashMap<String, String> sessionCookies    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> resolvedBaseUrls  = new ConcurrentHashMap<>();

    private final RestTemplate restTemplate;
    private final RestTemplate bulkRestTemplate;

    private static final Pattern XRAY_LOG_LINE   = Pattern.compile(">>(\\s*)([\\w.\\-]+):(\\d+)");
    private static final Pattern VALID_DOMAIN     = Pattern.compile("(?i)^(?:[a-z0-9](?:[a-z0-9\\-]{0,61}[a-z0-9])?\\.)+[a-z]{2,6}$");

    private static final int BULK_BATCH_SIZE = 500;

    public XUIServerApiClient(RestTemplateBuilder builder) {
        this.restTemplate     = createTrustAllRestTemplate(10_000);
        this.bulkRestTemplate = createTrustAllRestTemplate(120_000);
    }

    public static class ClientPanelInfo {
        public String       email;
        public List<Integer> inboundIds;

        public ClientPanelInfo(String email, List<Integer> inboundIds) {
            this.email      = email;
            this.inboundIds = inboundIds;
        }
    }

    private RestTemplate createTrustAllRestTemplate(int readTimeout) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }}, new SecureRandom());
            return new RestTemplate(getSimpleClientHttpRequestFactory(sslContext, readTimeout));
        } catch (Exception e) {
            log.error("Failed to initialize trust-all SSL context", e);
            return new RestTemplateBuilder().setReadTimeout(Duration.ofMillis(readTimeout)).build();
        }
    }

    private static SimpleClientHttpRequestFactory getSimpleClientHttpRequestFactory(
            SSLContext sslContext, int readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(java.net.HttpURLConnection connection, String httpMethod)
                    throws java.io.IOException {
                if (connection instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
                    ((HttpsURLConnection) connection).setHostnameVerifier((hostname, session) -> true);
                }
                connection.setInstanceFollowRedirects(false);
                super.prepareConnection(connection, httpMethod);
            }
        };
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(readTimeout);
        return requestFactory;
    }

    private String buildBaseUrl(ServerDto server) {
        String cacheKey = server.getIpAddress() + ":" + server.getPanelPort();
        return resolvedBaseUrls.computeIfAbsent(cacheKey, key -> {
            String httpsUrl = constructUrl(server, "https://");
            String httpUrl  = constructUrl(server, "http://");
            try {
                restTemplate.exchange(httpsUrl + "/login", HttpMethod.GET, null, String.class);
                return httpsUrl;
            } catch (org.springframework.web.client.ResourceAccessException e) {
                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (msg.contains("ssl") || msg.contains("unrecognized") || msg.contains("certificate")) {
                    log.info("Detected plain HTTP for panel on server {}", server.getName());
                    return httpUrl;
                }
                return httpsUrl;
            } catch (Exception e) {
                return httpsUrl;
            }
        });
    }

    private String constructUrl(ServerDto server, String scheme) {
        int    port = (server.getPanelPort() != null && server.getPanelPort() != 0) ? server.getPanelPort() : 8080;
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

    private boolean hasApiToken(ServerDto server) {
        return server.getApiToken() != null && !server.getApiToken().isBlank();
    }

    private void ensureAuthenticated(ServerDto server, String baseUrl) {
        if (hasApiToken(server)) return;
        if (!sessionCookies.containsKey(server.getIpAddress())) {
            login(server, baseUrl);
        }
    }

    /**
     * Скорректировано под новый API: поддержка двухшаговой авторизации
     */
    private void login(ServerDto server, String baseUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> jsonBody = new HashMap<>();
        jsonBody.put("username", server.getPanelUsername());
        jsonBody.put("password", server.getPanelPassword());
        jsonBody.put("twoFactorCode", "");

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(jsonBody, headers);

        String loginUrl = baseUrl + "/login";
        log.info(">>>> [XUI LOGIN] Server: {}, Trying login URL: {}, User: {}", server.getName(), loginUrl, server.getPanelUsername());

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(loginUrl, entity, String.class);
            String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
            if (cookie == null) throw new RuntimeException("No cookie returned from panel");

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
        ResponseEntity<Map> response = restTemplate.postForEntity(
                url, new HttpEntity<>(body, headers), Map.class);

        if (response.getStatusCode() == HttpStatus.FOUND ||
                response.getStatusCode() == HttpStatus.MOVED_PERMANENTLY) {
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Authentication failed (Redirected)");
        }

        Map<?, ?> responseBody = response.getBody();
        if (responseBody != null && Boolean.FALSE.equals(responseBody.get("success"))) {
            String msg = (String) responseBody.get("msg");
            log.error("Panel API error for {}: {}", url, msg);
            throw new RuntimeException("Panel API error: " + msg);
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

    public void addClientWithToken(ServerDto server, List<Integer> inboundIds, String uuid,
                                   String email, String flow, String apiToken) {
        if (inboundIds == null || inboundIds.isEmpty())
            throw new IllegalStateException("Target Inbound IDs list cannot be empty for server '" + server.getName() + "'");
        if (!hasApiToken(server) && (server.getPanelUsername() == null || server.getPanelUsername().isBlank()))
            throw new IllegalStateException("Server '" + server.getName() + "' has no panelUsername configured");

        String baseUrl = buildBaseUrl(server);
        ensureAuthenticated(server, baseUrl);

        Map<String, Object> clientFields = new HashMap<>();
        clientFields.put("email",   email);
        clientFields.put("id",      uuid);
        clientFields.put("flow",    flow == null ? "" : flow);
        clientFields.put("limitIp", 0);
        clientFields.put("enable",  true);

        Map<String, Object> body = new HashMap<>();
        body.put("client",     clientFields);
        body.put("inboundIds", inboundIds);

        executeWithRetry(server, baseUrl, baseUrl + "/panel/api/clients/add", body);
    }

    public void addClient(ServerDto server, String uuid, String email, int limitIp, String flow) {
        Integer targetInboundId = server.getTcpInboundId() != null ? server.getTcpInboundId() : server.getPanelInboundId();
        addClientWithToken(server, Collections.singletonList(targetInboundId), uuid, email, flow, server.getApiToken());
    }

    public void removeClient(ServerDto server, String uuid) {
        String baseUrl = buildBaseUrl(server);
        ensureAuthenticated(server, baseUrl);
        String email = findEmailByUuidWithRetry(server, baseUrl, uuid);
        if (email != null && !email.isBlank()) {
            executeWithRetry(server, baseUrl, baseUrl + "/panel/api/clients/del/" + email + "?keepTraffic=0", null);
            log.info("Successfully deleted client by email {} on server {}", email, server.getName());
        } else {
            log.warn("Client with UUID {} not found on server {}, skipping deletion", uuid, server.getName());
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllClients(ServerDto server) {
        String baseUrl = buildBaseUrl(server);
        ensureAuthenticated(server, baseUrl);

        HttpHeaders headers = buildAuthHeaders(server);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/panel/api/clients/list", HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null && Boolean.TRUE.equals(response.getBody().get("success"))) {
                return (List<Map<String, Object>>) response.getBody().get("obj");
            }
        } catch (Exception e) {
            log.error("Failed to fetch full clients list from server {}: {}", server.getName(), e.getMessage());
        }
        return Collections.emptyList();
    }

    public void bulkCreateClients(ServerDto server, List<Map<String, Object>> bulkPayload) {
        if (bulkPayload == null || bulkPayload.isEmpty()) return;

        String baseUrl = buildBaseUrl(server);
        ensureAuthenticated(server, baseUrl);

        for (int i = 0; i < bulkPayload.size(); i += BULK_BATCH_SIZE) {
            List<Map<String, Object>> batch = bulkPayload.subList(i,
                    Math.min(i + BULK_BATCH_SIZE, bulkPayload.size()));

            HttpHeaders headers = buildAuthHeaders(server);
            ResponseEntity<String> response = bulkRestTemplate.postForEntity(
                    baseUrl + "/panel/api/clients/bulkCreate",
                    new HttpEntity<>(batch, headers),
                    String.class);

            log.info("bulkCreate batch {}/{} response for {}: {}",
                    (i / BULK_BATCH_SIZE) + 1,
                    (int) Math.ceil((double) bulkPayload.size() / BULK_BATCH_SIZE),
                    server.getName(), response.getBody());
        }
    }

    @SuppressWarnings("unchecked")
    public void moveAllClients(ServerDto sourceServer, ServerDto targetServer, List<Integer> targetInboundIds) {
        log.info("Starting migration of clients from {} to {}", sourceServer.getName(), targetServer.getName());

        List<Map<String, Object>> sourceClients = getAllClients(sourceServer);
        if (sourceClients.isEmpty()) {
            log.warn("No clients found on source server {}, migration aborted.", sourceServer.getName());
            return;
        }

        List<Map<String, Object>> bulkPayload = new ArrayList<>();
        int skipped = 0;

        for (Map<String, Object> src : sourceClients) {
            Object uuidRaw = src.get("uuid");
            if (uuidRaw == null) uuidRaw = src.get("id");
            if (uuidRaw == null) {
                log.warn("Skipping client '{}': no uuid/id found", src.get("email"));
                skipped++;
                continue;
            }
            String uuid = uuidRaw.toString();

            Object tgIdRaw = src.get("tgId");
            long tgId = tgIdRaw instanceof Number ? ((Number) tgIdRaw).longValue() : 0L;

            Map<String, Object> clientObj = new HashMap<>();
            clientObj.put("id",         uuid);
            clientObj.put("email",      src.get("email"));
            clientObj.put("flow",       src.get("flow") != null ? src.get("flow") : "xtls-rprx-vision");
            clientObj.put("limitIp",    src.get("limitIp")    != null ? src.get("limitIp")    : 0);
            clientObj.put("totalGB",    src.get("totalGB")    != null ? src.get("totalGB")    : 0);
            clientObj.put("expiryTime", src.get("expiryTime") != null ? src.get("expiryTime") : 0);
            clientObj.put("enable",     src.get("enable")     != null ? src.get("enable")     : true);
            clientObj.put("tgId",       tgId);
            clientObj.put("subId",      src.get("subId") != null ? src.get("subId") : "");

            Map<String, Object> bulkItem = new HashMap<>();
            bulkItem.put("client",     clientObj);
            bulkItem.put("inboundIds", targetInboundIds);

            bulkPayload.add(bulkItem);
        }

        if (bulkPayload.isEmpty()) {
            log.warn("Migration aborted: all {} clients were skipped (no uuid/id)", skipped);
            return;
        }

        log.info("Prepared {} clients for migration ({} skipped), sending to {}",
                bulkPayload.size(), skipped, targetServer.getName());

        bulkCreateClients(targetServer, bulkPayload);
        log.info("Successfully migrated {} clients from {} to {}", bulkPayload.size(), sourceServer.getName(), targetServer.getName());
    }

    public String getXrayLogs(ServerDto server, int count, String emailFilter) {
        String      baseUrl = buildBaseUrl(server);
        ensureAuthenticated(server, baseUrl);

        String      url     = baseUrl + "/panel/api/server/xraylogs/" + count;
        HttpHeaders headers = buildAuthHeaders(server);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("filter",      emailFilter);
        form.add("showDirect",  "true");
        form.add("showBlocked", "true");
        form.add("showProxy",   "true");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getBody() != null && Boolean.TRUE.equals(response.getBody().get("success"))) {
                return (String) response.getBody().get("obj");
            }
        } catch (Exception e) {
            log.error("Failed to fetch Xray logs for filter={} from server {}: {}", emailFilter, server.getName(), e.getMessage());
        }
        return "";
    }

    public Set<String> extractDomainsFromLogs(String logs) {
        if (logs == null || logs.isBlank()) {
            log.debug("extractDomains: logs is blank");
            return Collections.emptySet();
        }

        log.debug("extractDomains: raw log sample:\n{}",
                logs.length() > 500 ? logs.substring(0, 500) : logs);

        Set<String> domains = new HashSet<>();
        Matcher matcher = XRAY_LOG_LINE.matcher(logs);
        while (matcher.find()) {
            String host = matcher.group(2).toLowerCase();
            if (host.startsWith(".")) host = host.substring(1);
            if (VALID_DOMAIN.matcher(host).matches() && !isWhiteNoiseDomain(host)) {
                domains.add(host);
            }
        }
        return domains;
    }

    public boolean isWhiteNoiseDomain(String d) {
        return d.contains("yastatic")     || d.contains("cloudfront")      || d.contains("cloudflare")
                || d.contains("geovp")        || d.contains("apple.com")       || d.contains("icloud")
                || d.contains("microsoft")    || d.contains("googleapis")      || d.contains("googleusercontent")
                || d.contains("windowsupdate")|| d.contains("gstatic")         || d.contains("ocsp")
                || d.contains("akamaitechnologies") || d.contains("akamaiedge")|| d.contains("fastly");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Map<String, Long>> getAllClientsTraffic(ServerDto server) {
        String      baseUrl = buildBaseUrl(server);
        ensureAuthenticated(server, baseUrl);

        HttpHeaders headers = buildAuthHeaders(server);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/panel/api/clients/list", HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null && Boolean.TRUE.equals(response.getBody().get("success"))) {
                List<Map<String, Object>> clients = (List<Map<String, Object>>) response.getBody().get("obj");
                if (clients != null) {
                    Map<String, Map<String, Long>> result = new HashMap<>(clients.size() * 2);
                    for (Map<String, Object> client : clients) {
                        String email = (String) client.get("email");
                        Map<String, Object> traffic = (Map<String, Object>) client.get("traffic");
                        if (email != null && traffic != null) {
                            Map<String, Long> stats = new HashMap<>(4);
                            stats.put("up",   ((Number) traffic.get("up")).longValue());
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

    public boolean checkAndRestoreOrAttachClient(ServerDto server, List<Integer> targetInboundIds,
                                                 String uuid, String expectedEmail, String flow) {
        if (targetInboundIds == null || targetInboundIds.isEmpty()) return false;

        String baseUrl = buildBaseUrl(server);
        ensureAuthenticated(server, baseUrl);

        ClientPanelInfo panelInfo = findClientInfoByUuidWithRetry(server, baseUrl, uuid);

        if (panelInfo == null || panelInfo.email == null || panelInfo.email.isBlank()) {
            log.info("Client UUID {} not found on server {}. Restoring to inbounds {}...", uuid, server.getName(), targetInboundIds);
            addClientWithToken(server, targetInboundIds, uuid, expectedEmail, flow, server.getApiToken());
            return true;
        }

        List<Integer> missingInbounds = new ArrayList<>();
        for (Integer targetId : targetInboundIds) {
            if (!panelInfo.inboundIds.contains(targetId)) missingInbounds.add(targetId);
        }

        if (!missingInbounds.isEmpty()) {
            log.info("Client UUID {} exists on server {}, but missing in inbounds {}. Attaching...",
                    uuid, server.getName(), missingInbounds);
            attachClientToInbounds(server, baseUrl, panelInfo.email, missingInbounds);
            return true;
        }

        return false;
    }

    private void attachClientToInbounds(ServerDto server, String baseUrl, String email, List<Integer> inboundsToAttach) {
        if (inboundsToAttach.isEmpty()) return;
        Map<String, Object> body = new HashMap<>();
        body.put("inboundIds", inboundsToAttach);
        executeWithRetry(server, baseUrl, baseUrl + "/panel/api/clients/" + email + "/attach", body);
        log.info("Successfully attached client '{}' to extra inbounds {} on server {}", email, inboundsToAttach, server.getName());
    }

    @SuppressWarnings("unchecked")
    private String findEmailByUuid(ServerDto server, String baseUrl, String uuid) {
        HttpEntity<?> entity = new HttpEntity<>(buildAuthHeaders(server));
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/panel/api/clients/list", HttpMethod.GET, entity, Map.class);
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

    private String findEmailByUuidWithRetry(ServerDto server, String baseUrl, String uuid) {
        try {
            return findEmailByUuid(server, baseUrl, uuid);
        } catch (HttpClientErrorException.Unauthorized e) {
            if (hasApiToken(server)) throw e;
            sessionCookies.remove(server.getIpAddress());
            login(server, baseUrl);
            return findEmailByUuid(server, baseUrl, uuid);
        }
    }

    @SuppressWarnings("unchecked")
    private ClientPanelInfo findClientInfoByUuid(ServerDto server, String baseUrl, String uuid) {
        HttpEntity<?> entity = new HttpEntity<>(buildAuthHeaders(server));
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/panel/api/clients/list", HttpMethod.GET, entity, Map.class);
            if (response.getBody() != null && Boolean.TRUE.equals(response.getBody().get("success"))) {
                List<Map<String, Object>> clients = (List<Map<String, Object>>) response.getBody().get("obj");
                if (clients != null) {
                    for (Map<String, Object> client : clients) {
                        Object idVal   = client.get("id");
                        Object uuidVal = client.get("uuid");
                        if ((idVal   != null && idVal.toString().equalsIgnoreCase(uuid)) ||
                                (uuidVal != null && uuidVal.toString().equalsIgnoreCase(uuid))) {
                            String email      = (String) client.get("email");
                            List<Integer> ids = (List<Integer>) client.get("inboundIds");
                            return new ClientPanelInfo(email, ids != null ? ids : Collections.emptyList());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch clients list from server {}: {}", server.getName(), e.getMessage());
        }
        return null;
    }

    private ClientPanelInfo findClientInfoByUuidWithRetry(ServerDto server, String baseUrl, String uuid) {
        try {
            return findClientInfoByUuid(server, baseUrl, uuid);
        } catch (HttpClientErrorException.Unauthorized e) {
            if (hasApiToken(server)) throw e;
            sessionCookies.remove(server.getIpAddress());
            login(server, baseUrl);
            return findClientInfoByUuid(server, baseUrl, uuid);
        }
    }

    private void validatePanelCredentials(ServerDto server) {
        if (server.getPanelInboundId() == null && server.getTcpInboundId() == null && server.getWsInboundId() == null)
            throw new IllegalStateException("Server '" + server.getName() + "' has no inbound ID configured");
        if (hasApiToken(server)) return;
        if (server.getPanelUsername() == null || server.getPanelUsername().isBlank())
            throw new IllegalStateException("Server '" + server.getName() + "' has no panelUsername configured");
    }
}