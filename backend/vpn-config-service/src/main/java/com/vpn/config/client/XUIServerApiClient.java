package com.vpn.config.client;

import com.vpn.common.dto.ServerDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class XUIServerApiClient {

    private final ConcurrentHashMap<String, String> sessionCookies = new ConcurrentHashMap<>();
    private final RestTemplate restTemplate;

    public XUIServerApiClient(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    private String buildBaseUrl(ServerDto server) {
        int port = (server.getPanelPort() != null && server.getPanelPort() != 0) ? server.getPanelPort() : 8080;
        String path = server.getPanelPath();
        String ip = server.getIpAddress();

        StringBuilder sb = new StringBuilder("http://").append(ip).append(":").append(port);

        if (path != null && !path.isBlank() && !path.equalsIgnoreCase("null") && !path.equalsIgnoreCase("<null>")) {
            if (!path.startsWith("/")) sb.append("/");
            sb.append(path);
        }

        String url = sb.toString();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private void validatePanelCredentials(ServerDto server) {
        if (server.getPanelUsername() == null || server.getPanelUsername().isBlank()) {
            throw new IllegalStateException(
                    "Server '" + server.getName() + "' (id=" + server.getId() + ") has no panelUsername configured in DB"
            );
        }
        if (server.getPanelPassword() == null || server.getPanelPassword().isBlank()) {
            throw new IllegalStateException(
                    "Server '" + server.getName() + "' (id=" + server.getId() + ") has no panelPassword configured in DB"
            );
        }
        if (server.getPanelInboundId() == null) {
            throw new IllegalStateException(
                    "Server '" + server.getName() + "' (id=" + server.getId() + ") has no panelInboundId configured in DB"
            );
        }
    }

    public void addClient(ServerDto server, String uuid, String email, int limitIp, String flow) {
        validatePanelCredentials(server);

        String baseUrl = buildBaseUrl(server);
        ensureAuthenticated(server, baseUrl);

        if (server.isRelay()) {
            String url = baseUrl + "/panel/api/clients/add";

            Map<String, Object> clientFields = new HashMap<>();
            clientFields.put("email", email);
            clientFields.put("id", uuid);
            clientFields.put("flow", (flow == null) ? "" : flow);
            clientFields.put("limitIp", limitIp);
            clientFields.put("enable", true);

            Map<String, Object> body = new HashMap<>();
            body.put("client", clientFields);
            body.put("inboundIds", Collections.singletonList(server.getPanelInboundId()));

            executeWithRetry(server, baseUrl, url, body);
        } else {
            String url = baseUrl + "/panel/api/inbounds/addClient";
            String effectiveFlow = (flow == null) ? "" : flow;

            String clientJson = String.format(
                    "{\"id\": \"%s\", \"email\": \"%s\", \"flow\": \"%s\", \"limitIp\": %d, \"enable\": true}",
                    uuid, email, effectiveFlow, limitIp
            );

            Map<String, Object> body = new HashMap<>();
            body.put("id", server.getPanelInboundId());
            body.put("settings", "{\"clients\": [" + clientJson + "]}");

            executeWithRetry(server, baseUrl, url, body);
        }
    }

    private void ensureAuthenticated(ServerDto server, String baseUrl) {
        if (!sessionCookies.containsKey(server.getIpAddress())) {
            login(server, baseUrl);
        }
    }

    private void login(ServerDto server, String baseUrl) {
        String loginUrl = baseUrl + "/login";
        log.info(">>>> [XUI LOGIN] Server: {}, URL: {}, User: {}, isRelay: {}",
                server.getName(), loginUrl, server.getPanelUsername(), server.isRelay());

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

            if (cookie == null) throw new RuntimeException("No cookie returned from " + server.getIpAddress());

            sessionCookies.put(server.getIpAddress(), cookie);
            log.info("<<<< [XUI LOGIN] SUCCESS for {}", server.getName());
        } catch (Exception e) {
            log.error("!!!! [XUI LOGIN] FAILED for {}: {}", server.getName(), e.getMessage());
            throw new RuntimeException("Login failed: " + e.getMessage());
        }
    }

    public void removeClient(ServerDto server, String uuid) {
        validatePanelCredentials(server);

        String baseUrl = buildBaseUrl(server);
        ensureAuthenticated(server, baseUrl);

        if (server.isRelay()) {
            String email = findEmailByUuidWithRetry(server, baseUrl, uuid);
            if (email != null && !email.isBlank()) {
                String url = baseUrl + "/panel/api/clients/del/" + email;
                executeWithRetry(server, baseUrl, url, null);
                log.info("Successfully deleted client by email {} on server {}", email, server.getName());
            } else {
                log.warn("Client with UUID {} not found on server {}, skipping deletion", uuid, server.getName());
            }
        } else {
            String url = String.format("%s/panel/api/inbounds/%d/delClient/%s",
                    baseUrl, server.getPanelInboundId(), uuid);
            executeWithRetry(server, baseUrl, url, null);
        }
    }

    /**
     * Выполняет поиск email клиента по его UUID на новой панели.
     */
    @SuppressWarnings("unchecked")
    private String findEmailByUuid(ServerDto server, String baseUrl, String uuid) {
        String url = baseUrl + "/panel/api/clients/list";
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, sessionCookies.get(server.getIpAddress()));
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getBody() != null && Boolean.TRUE.equals(response.getBody().get("success"))) {
                List<Map<String, Object>> clients = (List<Map<String, Object>>) response.getBody().get("obj");
                if (clients != null) {
                    for (Map<String, Object> client : clients) {
                        Object idVal = client.get("id");
                        Object uuidVal = client.get("uuid");
                        if ((idVal != null && idVal.toString().equalsIgnoreCase(uuid)) ||
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
            sessionCookies.remove(server.getIpAddress());
            login(server, baseUrl);
            return findEmailByUuid(server, baseUrl, uuid);
        }
    }

    private void executeWithRetry(ServerDto server, String baseUrl, String url, Object body) {
        try {
            sendRequest(server.getIpAddress(), url, body);
        } catch (HttpClientErrorException.Unauthorized e) {
            sessionCookies.remove(server.getIpAddress());
            login(server, baseUrl);
            sendRequest(server.getIpAddress(), url, body);
        }
    }

    private void sendRequest(String ip, String url, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, sessionCookies.get(ip));
        restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
    }
}