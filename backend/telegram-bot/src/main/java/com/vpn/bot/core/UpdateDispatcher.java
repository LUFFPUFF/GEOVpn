package com.vpn.bot.core;

import com.vpn.bot.handler.TelegramStartHandler;
import com.vpn.bot.service.BotBusinessService;
import com.vpn.bot.service.DeviceRegistrationBotService;
import com.vpn.bot.service.VpnConfigService;
import com.vpn.bot.service.SubscriptionService;
import com.vpn.bot.ui.KeyboardFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateDispatcher {

    private final BotBusinessService businessService;
    private final MessageSender sender;
    private final TelegramStartHandler startHandler;
    private final SubscriptionService subscriptionService;
    private final DeviceRegistrationBotService registrationService;
    private final KeyboardFactory keyboardFactory;
    private final VpnConfigService vpnConfigService;

    private final Map<Long, String> userStates = new ConcurrentHashMap<>();

    private static final String STATE_AWAITING_PROMO = "AWAITING_PROMO";
    private static final String STATE_AWAITING_REF_CODE = "AWAITING_REF_CODE";

    @Value("${telegram.bot.channel-url}")
    private String channelUrl;

    @Async("botTaskExecutor")
    public void dispatch(Update update) {
        try {
            if (update.hasChatMember()) {
                handleChatMemberUpdate(update);
                return;
            }

            long userId = getUserId(update);
            if (userId == 0) return;

            boolean isSubscribed = subscriptionService.isSubscribed(sender.getAbsSender(), userId);

            if (!isSubscribed) {
                sendSubscriptionRequiredMessage(userId);
                return;
            }

            boolean isStartCommand = update.hasMessage() && update.getMessage().hasText()
                    && update.getMessage().getText().startsWith("/start");

            if (!isStartCommand) {
                if (update.hasMessage()) {
                    registrationService.registerUserIfAbsent(update.getMessage().getFrom());
                } else if (update.hasCallbackQuery()) {
                    registrationService.registerUserIfAbsent(update.getCallbackQuery().getFrom());
                }
            }

            if (update.hasMessage() && update.getMessage().hasText()) {
                handleTextMessage(update);
            } else if (update.hasCallbackQuery()) {
                handleCallback(update);
            }
        } catch (Exception e) {
            log.error("Dispatcher error in dispatch execution", e);
        }
    }

    private void handleChatMemberUpdate(Update update) {
        var chatMemberUpdated = update.getChatMember();
        if (chatMemberUpdated.getChat().getId().toString().equals(subscriptionService.getChannelId())) {
            long userId = chatMemberUpdated.getNewChatMember().getUser().getId();
            String status = chatMemberUpdated.getNewChatMember().getStatus();
            boolean isMember = List.of("member", "administrator", "creator").contains(status);

            log.info("[BOT EVENT] User {} membership changed to {}", userId, isMember);
            subscriptionService.updateMembershipInDb(userId, isMember);
        }
    }

    private void handleTextMessage(Update update) {
        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();

        log.debug("Received text message from chatId={}: '{}'", chatId, text);

        boolean isMenuCommand = text.equals("👤 Профиль") || text.equalsIgnoreCase("профиль")
                || text.equals("🔑 Конфиги") || text.equalsIgnoreCase("конфиги")
                || text.equals("📱 Устройства") || text.equalsIgnoreCase("устройства")
                || text.equals("💎 Купить") || text.equalsIgnoreCase("купить")
                || text.equals("🏆 Рефералы") || text.equalsIgnoreCase("рефералы")
                || text.equals("📖 Инструкции") || text.equalsIgnoreCase("инструкции")
                || text.equals("💬 Поддержка") || text.equalsIgnoreCase("поддержка")
                || text.equals("📰 Новости") || text.equalsIgnoreCase("новости")
                || text.equals("💳 Пополнить") || text.equalsIgnoreCase("пополнить")
                || text.startsWith("/start");

        if (isMenuCommand) {
            userStates.remove(chatId);
        } else if (userStates.containsKey(chatId)) {
            // Обработка ввода данных в активном состоянии
            String state = userStates.get(chatId);
            if (STATE_AWAITING_PROMO.equals(state)) {
                userStates.remove(chatId);
                businessService.applyPromoCode(chatId, text);
                return;
            } else if (STATE_AWAITING_REF_CODE.equals(state)) {
                userStates.remove(chatId);
                businessService.updateReferralCode(chatId, text);
                return;
            }
        }

        if (text.startsWith("/start")) {
            SendMessage response = startHandler.handle(update);
            response.setReplyMarkup(keyboardFactory.getMainReplyKeyboard());
            sender.execute(response);
        }
        else if (text.equals("👤 Профиль") || text.equalsIgnoreCase("профиль")) {
            businessService.sendProfile(chatId);
        }
        else if (text.equals("🔑 Конфиги") || text.equalsIgnoreCase("конфиги")) {
            businessService.sendConfigs(chatId);
        }
        else if (text.equals("📱 Устройства") || text.equalsIgnoreCase("устройства")) {
            businessService.sendDevices(chatId);
        }
        else if (text.equals("💎 Купить") || text.equalsIgnoreCase("купить")) {
            businessService.sendSubscriptionOptions(chatId);
        }
        else if (text.equals("🏆 Рефералы") || text.equalsIgnoreCase("рефералы")) {
            businessService.sendReferralStats(chatId);
        }
        else if (text.equals("📖 Инструкции") || text.equalsIgnoreCase("инструкции")) {
            businessService.sendInstructions(chatId);
        }
        else if (text.equals("💬 Поддержка") || text.equalsIgnoreCase("поддержка")) {
            businessService.sendSupport(chatId);
        }
        else if (text.equals("📰 Новости") || text.equalsIgnoreCase("новости")) {
            businessService.sendNews(chatId);
        }
        else if (text.equals("💳 Пополнить") || text.equalsIgnoreCase("пополнить")) {
            businessService.sendTopUpOptions(chatId);
        }
        else {
            businessService.sendSimpleText(chatId, "⚠️ Используйте меню ниже для навигации по сервису.", true);
        }
    }

    private void handleCallback(Update update) {
        String data = update.getCallbackQuery().getData();
        String callbackId = update.getCallbackQuery().getId();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        log.debug("Received callback query from chatId={} with data='{}'", chatId, data);

        switch (data) {
            case "check_sub" -> {
                if (subscriptionService.isSubscribed(sender.getAbsSender(), chatId)) {
                    answerCallback(callbackId, "✅ Спасибо за подписку!");
                    SendMessage startMsg = startHandler.handle(update);
                    startMsg.setReplyMarkup(keyboardFactory.getMainReplyKeyboard());
                    sender.execute(startMsg);
                } else {
                    sender.execute(AnswerCallbackQuery.builder()
                            .callbackQueryId(callbackId)
                            .text("❌ Вы всё еще не подписаны на инфо-канал!")
                            .showAlert(true).build());
                }
            }
            case "profile_refresh" -> {
                answerCallback(callbackId, "🔄 Обновлено");
                userStates.remove(chatId);
                businessService.sendProfile(chatId);
            }
            case "profile_subscribe" -> {
                answerCallback(callbackId, null);
                businessService.sendSubscriptionOptions(chatId);
            }
            case "devices_refresh" -> {
                answerCallback(callbackId, "🔄 Обновлено");
                businessService.sendDevices(chatId);
            }
            case "device_buy_slot" -> {
                answerCallback(callbackId, null);
                businessService.sendBuySlotConfirm(chatId);
            }
            case "device_slot_confirm" -> {
                answerCallback(callbackId, "⚡️ Покупка...");
                businessService.purchaseExtraSlot(chatId);
            }
            case "device_slot_cancel" -> {
                answerCallback(callbackId, "❌ Отменено");
                businessService.sendSimpleText(chatId, "❌ Покупка слота отменена.", true);
            }
            case "configs_refresh" -> {
                answerCallback(callbackId, "🔄 Обновлено");
                businessService.sendConfigs(chatId);
            }
            case "sub_1" -> {
                answerCallback(callbackId, "🧾 Формируем счет...");
                businessService.generatePaymentLink(chatId, 100);
            }
            case "sub_3" -> {
                answerCallback(callbackId, "🧾 Формируем счет...");
                businessService.generatePaymentLink(chatId, 250);
            }
            case "leaderboard_refresh", "show_leaderboard" -> {
                answerCallback(callbackId, null);
                businessService.sendLeaderboard(chatId);
            }
            case "referral_change_code" -> {
                answerCallback(callbackId, null);
                businessService.sendChangeRefCodeRequest(chatId);
                userStates.put(chatId, STATE_AWAITING_REF_CODE);
            }
            case "promo_apply_request" -> {
                answerCallback(callbackId, null);
                businessService.sendPromoRequest(chatId);
                userStates.put(chatId, STATE_AWAITING_PROMO);
            }
            case "topup_options", "top_up_balance" -> {
                answerCallback(callbackId, null);
                businessService.sendTopUpOptions(chatId);
            }
            case "topup_100" -> {
                answerCallback(callbackId, "🧾 Формируем счет...");
                businessService.generatePaymentLink(chatId, 100);
            }
            case "topup_250" -> {
                answerCallback(callbackId, "🧾 Формируем счет...");
                businessService.generatePaymentLink(chatId, 250);
            }
            case "topup_500" -> {
                answerCallback(callbackId, "🧾 Формируем счет...");
                businessService.generatePaymentLink(chatId, 500);
            }
            case "menu_configs" -> {
                answerCallback(callbackId, null);
                businessService.sendConfigs(chatId);
            }
            case "menu_devices" -> {
                answerCallback(callbackId, null);
                businessService.sendDevices(chatId);
            }
            case "menu_referrals" -> {
                answerCallback(callbackId, null);
                businessService.sendReferralStats(chatId);
            }
            case "menu_instructions" -> {
                answerCallback(callbackId, null);
                businessService.sendInstructions(chatId);
            }
            case "menu_support" -> {
                answerCallback(callbackId, null);
                businessService.sendSupport(chatId);
            }
            default -> {
                if (data.startsWith("config_create:")) {
                    answerCallback(callbackId, null);
                    String country = data.split(":")[1];
                    vpnConfigService.createConfig(chatId, country);
                } else if (data.startsWith("buy_sub_tariff:")) {
                    String[] parts = data.split(":");
                    if (parts.length == 3) {
                        String planId = parts[1];
                        boolean isPromo = Boolean.parseBoolean(parts[2]);
                        answerCallback(callbackId, "⏳ Обработка подписки...");
                        businessService.purchaseSubscription(chatId, planId, isPromo);
                    } else {
                        answerCallback(callbackId, "❌ Ошибка запроса");
                    }
                } else {
                    answerCallback(callbackId, null);
                    log.warn("Unhandled callback data: {}", data);
                }
            }
        }
    }

    private void sendSubscriptionRequiredMessage(long chatId) {
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(InlineKeyboardButton.builder().text("📢 Подписаться на канал").url(channelUrl).build()))
                .keyboardRow(List.of(InlineKeyboardButton.builder().text("✅ Я подписался").callbackData("check_sub").build()))
                .build();

        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text("🛡 <b>Доступ временно ограничен</b>\n\nДля использования GeoVPN, генерации ключей и открытия Mini App, необходимо подписаться на наш официальный новостной канал.")
                .parseMode("HTML")
                .replyMarkup(markup)
                .build();

        sender.execute(msg);
    }

    private long getUserId(Update update) {
        if (update.hasMessage()) return update.getMessage().getFrom().getId();
        if (update.hasCallbackQuery()) return update.getCallbackQuery().getFrom().getId();
        return 0;
    }

    private void answerCallback(String callbackId, String text) {
        try {
            AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackId);
            if (text != null) {
                answer.setText(text);
            }
            sender.execute(answer);
        } catch (Exception e) {
            log.warn("Failed to answer callback query {}", callbackId, e);
        }
    }
}