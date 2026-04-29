package com.vpn.bot.config;

import com.vpn.bot.core.GeoVpnBot;
import com.vpn.bot.core.MessageSender;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotInitializer {

    private final GeoVpnBot bot;
    private final MessageSender messageSender;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        messageSender.setSender(bot);
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            log.info("Попытка регистрации Telegram-бота...");
            botsApi.registerBot(bot);
            log.info("Telegram-бот УСПЕШНО зарегистрирован и начал слушать сообщения!");
        } catch (TelegramApiException e) {
            log.error("Ошибка при регистрации бота: {}", e.getMessage(), e);
        }
    }
}
