package com.vpn.config.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Клиент для взаимодействия с 3x-UI панелью.
 *
 * Поддерживает несколько серверов через реестр панелей.
 * Каждый сервер имеет свою сессию (cookie).
 *
 * Конфиг серверов задаётся через:
 *   vpn.panels[0].url / username / password / inboundId
 *   vpn.panels[1].url / username / password / inboundId
 */
@Service
@Slf4j
public class XUIServerApiClient {

    @Value("${vpn.panel.url}") private String nlUrl;
    @Value("${vpn.panel.username}") private String nlUser;
    @Value("${vpn.panel.password}") private String nlPass;
    @Value("${vpn.panel.inbound-id}") private int nlInbound;

    @Value("${vpn.relay.panel.url}") private String ruUrl;
    @Value("${vpn.relay.panel.username}") private String ruUser;
    @Value("${vpn.relay.panel.password}") private String ruPass;
    @Value("${vpn.relay.panel.inbound-id}") private int ruInbound;

    @Value("${vpn.relay.ru.ip}") private String ruIp;

    private final ConcurrentHashMap<String, String> sessionCookies = new ConcurrentHashMap<>();
    private final RestTemplate restTemplate = new RestTemplate();

    public void addClient(String serverIp, String uuid, String email, int limitIp) {
        PanelConfig panel = getPanelConfigByIp(serverIp);
        ensureAuthenticated(serverIp, panel);

        String url = panel.url + "/panel/api/inbounds/addClient";
        String clientJson = String.format(
                "{\"id\": \"%s\", \"email\": \"%s\", \"flow\": \"xtls-rprx-vision\", \"limitIp\": %d, \"enable\": true}",
                uuid, email, limitIp
        );

        Map<String, Object> body = new HashMap<>();
        body.put("id", panel.inboundId);
        body.put("settings", "{\"clients\": [" + clientJson + "]}");

        executeWithRetry(serverIp, panel, url, body);
    }

    public void removeClient(String serverIp, String uuid) {
        PanelConfig panel = getPanelConfigByIp(serverIp);
        ensureAuthenticated(serverIp, panel);
        String url = String.format("%s/panel/api/inbounds/%d/delClient/%s", panel.url, panel.inboundId, uuid);
        executeWithRetry(serverIp, panel, url, null);
    }

    private void ensureAuthenticated(String ip, PanelConfig panel) {
        if (!sessionCookies.containsKey(ip)) login(ip, panel);
    }

    private void login(String ip, PanelConfig panel) {
        String loginUrl = panel.url + "/login";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", panel.username);
        form.add("password", panel.password);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(loginUrl, new HttpEntity<>(form, headers), String.class);
            String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
            if (cookie == null) throw new RuntimeException("No cookie");
            sessionCookies.put(ip, cookie);
        } catch (Exception e) {
            log.error("XUI Login failed for {}: {}", ip, e.getMessage());
            throw new RuntimeException("Login failed");
        }
    }

    private void executeWithRetry(String ip, PanelConfig panel, String url, Object body) {
        try {
            sendRequest(ip, url, body);
        } catch (HttpClientErrorException.Unauthorized e) {
            sessionCookies.remove(ip);
            login(ip, panel);
            sendRequest(ip, url, body);
        }
    }

    private void sendRequest(String ip, String url, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, sessionCookies.get(ip));
        restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
    }

    private PanelConfig getPanelConfigByIp(String ip) {
        if (ip.equals(ruIp)) return new PanelConfig(ruUrl, ruUser, ruPass, ruInbound);
        return new PanelConfig(nlUrl, nlUser, nlPass, nlInbound);
    }

    private record PanelConfig(String url, String username, String password, int inboundId) {}
}
