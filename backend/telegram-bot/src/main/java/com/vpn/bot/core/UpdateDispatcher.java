package com.vpn.bot.core;

import com.vpn.bot.handler.OsSelectionCallbackHandler;
import com.vpn.bot.handler.TelegramStartHandler;
import com.vpn.bot.service.BotBusinessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateDispatcher {

    private final BotBusinessService businessService;
    private final MessageSender sender;
    private final TelegramStartHandler startHandler;
    private final OsSelectionCallbackHandler osSelectionCallbackHandler;

    @Async("botTaskExecutor")
    public void dispatch(Update update) {
        try {
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
        }
        else if (text.equals("🏆 Рефералы")) {
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

        if (data != null && data.startsWith("os_select:")) {
            SendMessage response = osSelectionCallbackHandler.handle(update);
            sender.execute(response);
        }
    }
}
