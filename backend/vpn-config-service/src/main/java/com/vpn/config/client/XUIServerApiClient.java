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
import java.util.HashMap;
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

    /**
     * Проверяет, что у сервера заполнены все поля, необходимые для работы с панелью.
     * Бросает IllegalStateException с понятным сообщением если что-то отсутствует.
     */
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

    private void ensureAuthenticated(ServerDto server, String baseUrl) {
        if (!sessionCookies.containsKey(server.getIpAddress())) {
            login(server, baseUrl);
        }
    }

    private void login(ServerDto server, String baseUrl) {
        String loginUrl = baseUrl + "/login";
        log.info(">>>> [XUI LOGIN] Server: {}, URL: {}, User: {}", server.getName(), loginUrl, server.getPanelUsername());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", server.getPanelUsername());
        form.add("password", server.getPanelPassword());

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(loginUrl, new HttpEntity<>(form, headers), String.class);
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
        String url = String.format("%s/panel/api/inbounds/%d/delClient/%s",
                baseUrl, server.getPanelInboundId(), uuid);
        executeWithRetry(server, baseUrl, url, null);
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