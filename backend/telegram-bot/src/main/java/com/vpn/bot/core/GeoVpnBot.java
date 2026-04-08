package com.vpn.bot.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Component
public class GeoVpnBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final UpdateDispatcher dispatcher;

    public GeoVpnBot(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername,
            UpdateDispatcher dispatcher) {
        super(botToken);
        this.botUsername = botUsername;
        this.dispatcher = dispatcher;
    }

    @Override
    public String getBotUsername() { return botUsername; }

    @Override
    public void onUpdateReceived(Update update) {
        log.debug("Received update from Telegram...");
        dispatcher.dispatch(update);
    }
}
