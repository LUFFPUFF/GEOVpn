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

    @Value("${vpn.panel.url}")
    private String primaryPanelUrl;

    @Value("${vpn.panel.username}")
    private String primaryUsername;

    @Value("${vpn.panel.password}")
    private String primaryPassword;

    @Value("${vpn.panel.inbound-id:1}")
    private int primaryInboundId;

    @Value("${vpn.relay.panel.url:}")
    private String relayPanelUrl;

    @Value("${vpn.relay.panel.username:admin}")
    private String relayUsername;

    @Value("${vpn.relay.panel.password:admin}")
    private String relayPassword;

    @Value("${vpn.relay.panel.inbound-id:1}")
    private int relayInboundId;

    private final ConcurrentHashMap<Integer, String> sessionCookies =
            new ConcurrentHashMap<>();

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int PRIMARY_SERVER_ID = 1;
    private static final int RELAY_SERVER_ID   = 2;


    /**
     * Добавить клиента на сервер.
     *
     * @param serverId  ID сервера (1 = NL primary, 2 = RU relay)
     * @param uuid      VLESS UUID клиента
     * @param email     email/имя клиента (для идентификации в панели)
     * @param limitIp   лимит устройств (из DeviceLimitService)
     */
    public void addClient(int serverId, String uuid, String email, int limitIp) {
        PanelConfig panel = getPanelConfig(serverId);

        ensureAuthenticated(serverId, panel);

        String url = panel.url + "/panel/api/inbounds/addClient";

        String clientJson = buildClientJson(uuid, email, limitIp);

        Map<String, Object> body = new HashMap<>();
        body.put("id", panel.inboundId);
        body.put("settings", "{\"clients\": [" + clientJson + "]}");

        log.info("addClient → server={}, inbound={}, email={}, limitIp={}",
                serverId, panel.inboundId, email, limitIp);

        executeWithRetry(serverId, panel, url, body);
    }

    /**
     * Удалить клиента с сервера.
     *
     * @param serverId ID сервера
     * @param uuid     VLESS UUID клиента
     */
    public void removeClient(int serverId, String uuid) {
        PanelConfig panel = getPanelConfig(serverId);

        ensureAuthenticated(serverId, panel);

        String url = panel.url
                + "/panel/api/inbounds/"
                + panel.inboundId
                + "/delClient/"
                + uuid;

        log.info("removeClient → server={}, uuid={}",
                serverId, uuid.substring(0, 8) + "...");

        executeWithRetry(serverId, panel, url, null);
    }

    /**
     * Обновить лимит устройств у существующего клиента.
     */
    public void updateClientLimit(int serverId, String uuid, String email, int newLimit) {
        PanelConfig panel = getPanelConfig(serverId);
        ensureAuthenticated(serverId, panel);

        String url = panel.url + "/panel/api/inbounds/updateClient/" + uuid;

        String clientJson = buildClientJson(uuid, email, newLimit);

        Map<String, Object> body = new HashMap<>();
        body.put("id", panel.inboundId);
        body.put("settings", "{\"clients\": [" + clientJson + "]}");

        log.info("updateClientLimit → server={}, uuid={}, newLimit={}",
                serverId, uuid.substring(0, 8) + "...", newLimit);

        executeWithRetry(serverId, panel, url, body);
    }

    /**
     * Перезапустить Xray на сервере.
     */
    public void restartXray(int serverId) {
        PanelConfig panel = getPanelConfig(serverId);
        ensureAuthenticated(serverId, panel);

        String url = panel.url + "/panel/api/inbounds/restart";
        sendPost(serverId, url, null);

        log.info("Xray restarted on server={}", serverId);
    }

    /**
     * Перезапустить Xray на ВСЕХ серверах.
     */
    public void restartXray() {
        restartXray(PRIMARY_SERVER_ID);
        if (relayPanelUrl != null && !relayPanelUrl.isEmpty()) {
            restartXray(RELAY_SERVER_ID);
        }
    }

    /**
     * Логин вручную (для обратной совместимости).
     */
    public void login() {
        loginToServer(PRIMARY_SERVER_ID, getPanelConfig(PRIMARY_SERVER_ID));
    }

    /**
     * Проверяет наличие сессии, при необходимости логинится.
     */
    private void ensureAuthenticated(int serverId, PanelConfig panel) {
        if (!sessionCookies.containsKey(serverId)) {
            loginToServer(serverId, panel);
        }
    }

    private void loginToServer(int serverId, PanelConfig panel) {
        String loginUrl = panel.url + "/login";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", panel.username);
        form.add("password", panel.password);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(form, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(loginUrl, request, String.class);

            String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
            if (cookie == null || cookie.isEmpty()) {
                throw new RuntimeException("Login failed: no cookie in response");
            }

            sessionCookies.put(serverId, cookie);
            log.info("Logged into 3x-UI panel: serverId={}, url={}",
                    serverId, panel.url);

        } catch (Exception e) {
            log.error("Login failed for server={}, url={}: {}",
                    serverId, panel.url, e.getMessage());
            throw new RuntimeException("XUI login failed for server " + serverId, e);
        }
    }

    private void executeWithRetry(
            int serverId, PanelConfig panel, String url, Object body) {
        try {
            sendPost(serverId, url, body);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Session expired for server={}, re-authenticating...", serverId);
            sessionCookies.remove(serverId);
            loginToServer(serverId, panel);
            sendPost(serverId, url, body);
        }
    }

    private void sendPost(int serverId, String url, Object body) {
        String cookie = sessionCookies.get(serverId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (cookie != null) {
            headers.add(HttpHeaders.COOKIE, cookie);
        }

        HttpEntity<Object> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);

            log.debug("XUI response: status={}, body={}",
                    response.getStatusCode(), response.getBody());

        } catch (Exception e) {
            log.error("XUI request failed: url={}, error={}", url, e.getMessage());
            throw e;
        }
    }

    /**
     * Формирует JSON клиента для 3x-UI API
     */
    private String buildClientJson(String uuid, String email, int limitIp) {
        return String.format(
                "{" +
                        "\"id\": \"%s\", " +
                        "\"email\": \"%s\", " +
                        "\"flow\": \"xtls-rprx-vision\", " +
                        "\"limitIp\": %d, " +
                        "\"totalGB\": 0, " +
                        "\"expiryTime\": 0, " +
                        "\"enable\": true, " +
                        "\"tgId\": \"\", " +
                        "\"subId\": \"\"" +
                        "}",
                uuid,
                email,
                limitIp
        );
    }

    /**
     * Возвращает конфиг панели по ID сервера.
     */
    private PanelConfig getPanelConfig(int serverId) {
        if (serverId == RELAY_SERVER_ID && relayPanelUrl != null
                && !relayPanelUrl.isEmpty()) {
            return new PanelConfig(
                    relayPanelUrl, relayUsername,
                    relayPassword, relayInboundId);
        }
        return new PanelConfig(
                primaryPanelUrl, primaryUsername,
                primaryPassword, primaryInboundId);
    }

    /**
     * Конфиг одной панели
     */
    private record PanelConfig(
            String url,
            String username,
            String password,
            int inboundId) {}

}
