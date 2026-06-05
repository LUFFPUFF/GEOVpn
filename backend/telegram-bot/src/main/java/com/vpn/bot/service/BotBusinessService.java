package com.vpn.bot.service;

import com.vpn.bot.client.UserServiceClient;
import com.vpn.bot.client.VpnServiceClient;
import com.vpn.bot.core.MessageSender;
import com.vpn.bot.ui.KeyboardFactory;
import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.response.LeaderboardEntryDto;
import com.vpn.common.dto.response.UserResponse;
import com.vpn.common.dto.response.UserStatsResponse;
import com.vpn.common.dto.response.VpnConfigResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotBusinessService {

    private final UserServiceClient userService;
    private final VpnServiceClient vpnService;
    private final MessageSender sender;
    private final KeyboardFactory keyboardFactory;

    // ПРОФИЛЬ
    public void sendProfile(long chatId) {
        try {
            ApiResponse<UserResponse> res = userService.getMyProfile(chatId);
            UserResponse u = (res != null) ? res.getData() : null;
            if (u == null) {
                sendSimpleText(chatId, "Не удалось загрузить профиль.", false);
                return;
            }
            boolean hasActive = u.isHasActiveSubscription();
            String text = "<b>Ваш профиль</b>\n\nID: <code>" + chatId + "</code>\nБаланс: " + (u.getBalance()/100) + " ₽";
            SendMessage msg = new SendMessage(String.valueOf(chatId), text);
            msg.setParseMode("HTML");
            msg.setReplyMarkup(keyboardFactory.getProfileKeyboard(hasActive));
            sender.execute(msg);
        } catch (Exception e) { log.error("Profile error", e); }
    }

    // УСТРОЙСТВА
    public void sendDevices(long chatId) {
        try {
            ApiResponse<UserResponse> res = userService.getMyProfile(chatId);
            UserResponse u = (res != null) ? res.getData() : null;
            int limit = (u != null && u.getDeviceLimit() != null) ? u.getDeviceLimit() : 1;
            int used = (u != null && u.getDevicesCount() != null) ? u.getDevicesCount() : 0;
            boolean limitReached = used >= limit;

            String text = "<b>Мои устройства</b>\n\nСлоты: <b>" + used + " / " + limit + "</b>";
            SendMessage msg = new SendMessage(String.valueOf(chatId), text);
            msg.setParseMode("HTML");
            msg.setReplyMarkup(keyboardFactory.getDevicesKeyboard(limitReached));
            sender.execute(msg);
        } catch (Exception e) { log.error("Devices error", e); }
    }

    // КОНФИГИ
    public void sendConfigs(long chatId) {
        try {
            ApiResponse<List<VpnConfigResponse>> res = vpnService.getMyConfigs(chatId);
            List<VpnConfigResponse> list = (res != null && res.getData() != null) ? res.getData() : List.of();

            StringBuilder text = new StringBuilder("<b>Мои VPN-конфиги</b>\n\n");
            if (list.isEmpty()) {
                text.append("У вас нет активных конфигов.");
            } else {
                for (VpnConfigResponse c : list) {
                    String country = (c.getConfigs() != null && !c.getConfigs().isEmpty())
                            ? c.getConfigs().get(0).getCountryCode() : "—";
                    text.append("• ").append(flagEmoji(country)).append(" ").append(country)
                            .append(" (ID: ").append(c.getDeviceId()).append(")\n");
                }
            }
            SendMessage msg = new SendMessage(String.valueOf(chatId), text.toString());
            msg.setParseMode("HTML");
            msg.setReplyMarkup(keyboardFactory.getConfigsKeyboard());
            sender.execute(msg);
        } catch (Exception e) { log.error("Configs error", e); }
    }

    // ЛИДЕРБОРД
    public void sendLeaderboard(long chatId) {
        try {
            ApiResponse<List<LeaderboardEntryDto>> res = userService.getLeaderboard(chatId);
            List<LeaderboardEntryDto> entries = (res != null && res.getData() != null) ? res.getData() : List.of();
            StringBuilder text = new StringBuilder("<b>Топ рефералов</b>\n\n");
            for (int i = 0; i < Math.min(entries.size(), 10); i++) {
                LeaderboardEntryDto e = entries.get(i);
                text.append(i+1).append(". @").append(e.getUsername()).append(" — ").append(e.getReferralCount()).append("\n");
            }
            sendSimpleText(chatId, text.toString(), true);
        } catch (Exception e) { log.error("Leaderboard error", e); }
    }

    // ВСЕ ОСТАЛЬНЫЕ МЕТОДЫ ДЛЯ UPDATE DISPATCHER
    public void sendPromoRequest(long chatId) { sendSimpleText(chatId, "Введите промокод:", false); }
    public void applyPromoCode(long chatId, String code) {
        try { userService.applyPromo(chatId, code); sendSimpleText(chatId, "Применено!", true); }
        catch (Exception e) { sendSimpleText(chatId, "Ошибка.", true); }
    }
    public void sendReferralStats(long chatId) { sendSimpleText(chatId, "Ваша ссылка в Mini App", true); }
    public void updateReferralCode(long chatId, String code) { userService.updateReferralCode(chatId, code); sendSimpleText(chatId, "Изменено!", true); }
    public void sendSubscriptionOptions(long chatId) {
        SendMessage msg = new SendMessage(String.valueOf(chatId), "Выберите тариф:");
        msg.setReplyMarkup(keyboardFactory.getBuySubKeyboard());
        sender.execute(msg);
    }
    public void sendBuySlotConfirm(long chatId) {
        SendMessage msg = new SendMessage(String.valueOf(chatId), "Купить слот?");
        msg.setReplyMarkup(keyboardFactory.getConfirmSlotKeyboard());
        sender.execute(msg);
    }
    public void purchaseExtraSlot(long chatId) { userService.purchaseExtraSlot(chatId); sendSimpleText(chatId, "Слот куплен!", true); }
    public void sendChangeRefCodeRequest(long chatId) { sendSimpleText(chatId, "Введите новый реф. код:", false); }
    public void sendNews(long chatId) { sendSimpleText(chatId, "Новости: @geo_vpn_news", true); }
    public void sendInstructions(long chatId) { sendSimpleText(chatId, "Инструкции в приложении.", true); }
    public void sendSupport(long chatId) { sendSimpleText(chatId, "Поддержка: @geo_vpn_support", true); }
    public void sendWebSiteInfo(long chatId) { sendSimpleText(chatId, "Сайт: geovpn.com", true); }

    public void sendSimpleText(long chatId, String text, boolean withMenu) {
        SendMessage msg = new SendMessage(String.valueOf(chatId), text);
        msg.setParseMode("HTML");
        if (withMenu) msg.setReplyMarkup(keyboardFactory.getMainReplyKeyboard());
        sender.execute(msg);
    }

    private String flagEmoji(String code) {
        if (code == null) return "🌐";
        return switch (code.toUpperCase()) { case "FI" -> "🇫🇮"; case "DE" -> "🇩🇪"; default -> "🌐"; };
    }
}