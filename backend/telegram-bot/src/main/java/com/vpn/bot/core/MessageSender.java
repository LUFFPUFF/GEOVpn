package com.vpn.bot.core;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.Serializable;

@Slf4j
@Component
public class MessageSender {

    @Setter
    private AbsSender sender;

    public <T extends Serializable, Method extends BotApiMethod<T>> void execute(Method method) {
        if (sender == null) return;
        try {
            sender.execute(method);
        } catch (TelegramApiException e) {
            log.error("Error: {}", e.getMessage());
        }
    }

    public void executePhoto(SendPhoto sendPhoto) {
        if (sender == null) return;
        try {
            sender.execute(sendPhoto);
        } catch (TelegramApiException e) {
            log.error("Photo Error: {}", e.getMessage());
        }
    }

    public AbsSender getAbsSender() {
        return sender;
    }
}
