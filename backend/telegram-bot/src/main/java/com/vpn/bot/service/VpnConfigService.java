package com.vpn.bot.service;

import com.vpn.bot.client.VpnServiceClient;
import com.vpn.bot.core.MessageSender;
import com.vpn.bot.ui.KeyboardFactory;
import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.request.ConfigCreateRequest;
import com.vpn.common.dto.response.VpnConfigResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class VpnConfigService {

    private final VpnServiceClient vpnServiceClient;
    private final MessageSender sender;
    private final KeyboardFactory keyboardFactory;

    public void createConfig(long chatId, String countryCode) {
        try {
            ConfigCreateRequest request = ConfigCreateRequest.builder()
                    .preferredCountry(countryCode)
                    .userTelegramId(chatId)
                    .deviceId(1L)
                    .protocol("VLESS")
                    .build();

            ApiResponse<VpnConfigResponse> res = vpnServiceClient.createConfig(chatId, request);

            if (res != null && res.isSuccess() && res.getData() != null) {
                String text = "<b>✅ Конфиг создан!</b>\nКлюч доступен в Mini App.";
                SendMessage msg = new SendMessage(String.valueOf(chatId), text);
                msg.setParseMode("HTML");
                msg.setReplyMarkup(keyboardFactory.getMainReplyKeyboard());
                sender.execute(msg);
            } else {
                String error = (res != null) ? res.getMessage() : "Ошибка сервиса";
                sendSimpleError(chatId, "Не удалось создать конфиг: " + error);
            }
        } catch (Exception e) {
            log.error("Create config error", e);
            sendSimpleError(chatId, "Ошибка системы.");
        }
    }

    private void sendSimpleError(long chatId, String text) {
        SendMessage msg = new SendMessage(String.valueOf(chatId), text);
        msg.setParseMode("HTML");
        msg.setReplyMarkup(keyboardFactory.getMainReplyKeyboard());
        sender.execute(msg);
    }
}