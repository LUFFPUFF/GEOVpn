package com.vpn.bot.core;

import com.vpn.bot.handler.OsSelectionCallbackHandler;
import com.vpn.bot.handler.TelegramStartHandler;
import com.vpn.bot.service.BotBusinessService;
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
    private final OsSelectionCallbackHandler osSelectionCallbackHandler;
    private final SubscriptionService subscriptionService;

    @Value("${telegram.bot.channel-url}")
    private String channelUrl;

    @Async("botTaskExecutor")
    public void dispatch(Update update) {
        try {
            long userId = getUserId(update);
            if (userId == 0) return;

            if (!subscriptionService.isSubscribed(sender.getAbsSender(), userId)) {
                sendSubscriptionRequiredMessage(userId);
                return;
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
        String callbackId = update.getCallbackQuery().getId();
        String data = update.getCallbackQuery().getData();

        sender.execute(new AnswerCallbackQuery(callbackId));

        if (data == null) return;

        if (data.startsWith("os_select:")) {
            SendMessage response = osSelectionCallbackHandler.handle(update);
            sender.execute(response);
        }
        else if (data.equals("check_subscription")) {
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            SendMessage response = startHandler.handle(update);
            sender.execute(response);
        }
    }

    private void sendSubscriptionRequiredMessage(long chatId) {
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder()
                                .text("📢 Подписаться на канал")
                                .url(channelUrl)
                                .build()
                ))
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder()
                                .text("✅ Я подписался")
                                .callbackData("check_subscription")
                                .build()
                ))
                .build();

        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text("🛡 <b>Доступ заблокирован</b>\n\nЧтобы пользоваться VPN и открыть приложение, подпишитесь на наш канал. Там мы публикуем новости и новые серверы!")
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
}
