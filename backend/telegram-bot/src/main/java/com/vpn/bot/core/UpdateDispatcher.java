package com.vpn.bot.core;


import com.vpn.bot.handler.TelegramStartHandler;
import com.vpn.bot.service.BotBusinessService;
import com.vpn.bot.service.DeviceRegistrationBotService;
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
import com.vpn.bot.service.SubscriptionService;

import java.util.List;

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

    @Value("${telegram.bot.channel-url}")
    private String channelUrl;

    @Async("botTaskExecutor")
    public void dispatch(Update update) {
        try {
            long userId = getUserId(update);
            if (userId == 0) return;

            boolean isSubscribed = subscriptionService.isSubscribed(sender.getAbsSender(), userId);

            if (!isSubscribed) {
                sendSubscriptionRequiredMessage(userId);
                return;
            }

            if (update.hasMessage()) {
                registrationService.registerUserIfAbsent(update.getMessage().getFrom());
            } else if (update.hasCallbackQuery()) {
                registrationService.registerUserIfAbsent(update.getCallbackQuery().getFrom());
            }

            if (update.hasMessage() && update.getMessage().hasText()) {
                handleTextMessage(update);
            } else if (update.hasCallbackQuery()) {
                handleCallback(update);
            }
        } catch (Exception e) {
            log.error("Dispatcher error", e);
        }
    }

    private void handleTextMessage(Update update) {
        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        if (text.startsWith("/start")) {
            SendMessage response = startHandler.handle(update);
            response.setReplyMarkup(keyboardFactory.getMainReplyKeyboard());
            sender.execute(response);
        }
        else if (text.equals("💎 Купить")) {
            businessService.sendSubscriptionOptions(chatId);
        } else if (text.equals("🏆 Рефералы")) {
            businessService.sendReferralStats(chatId);
        } else if (text.equals("📰 Новости")) {
            businessService.sendNews(chatId);
        } else if (text.equals("📖 Инструкции")) {
            businessService.sendInstructions(chatId);
        } else if (text.equals("💬 Поддержка")) {
            businessService.sendSupport(chatId);
        } else if (text.equals("🌐 Веб-сайт")) {
            businessService.sendWebSiteInfo(chatId);
        }
    }

    private void handleCallback(Update update) {
        String data = update.getCallbackQuery().getData();
        String callbackId = update.getCallbackQuery().getId();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        if ("check_sub".equals(data)) {
            if (subscriptionService.isSubscribed(sender.getAbsSender(), chatId)) {
                sender.execute(new AnswerCallbackQuery(callbackId));

                sender.execute(new SendMessage(String.valueOf(chatId), "✅ Спасибо за подписку! Теперь вам доступен весь функционал."));

                SendMessage startMsg = startHandler.handle(update);
                startMsg.setReplyMarkup(keyboardFactory.getMainReplyKeyboard());
                sender.execute(startMsg);
            } else {
                AnswerCallbackQuery alert = AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackId)
                        .text("❌ Вы всё еще не подписаны на канал!")
                        .showAlert(true)
                        .build();
                sender.execute(alert);
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
                .text("🛡 <b>Доступ заблокирован</b>\n\nДля использования GeoVPN и открытия Mini App, необходимо подписаться на наш информационный канал.")
                .parseMode("HTML")
                .replyMarkup(markup)
                .build();

        msg.setReplyMarkup(markup);

        sender.execute(msg);
    }

    private long getUserId(Update update) {
        if (update.hasMessage()) return update.getMessage().getFrom().getId();
        if (update.hasCallbackQuery()) return update.getCallbackQuery().getFrom().getId();
        return 0;
    }
}