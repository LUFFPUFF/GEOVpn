package com.vpn.bot.ui;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;

import java.util.List;

@Component
public class KeyboardFactory {

    private final String miniAppUrl;

    public KeyboardFactory(@Value("${app.miniapp-url}") String miniAppUrl) {
        this.miniAppUrl = miniAppUrl.replaceAll("\\s", "");
    }

    public ReplyKeyboardMarkup getMainReplyKeyboard() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(false);

        KeyboardButton appBtn = new KeyboardButton("🚀 Открыть приложение");
        appBtn.setWebApp(new WebAppInfo(miniAppUrl));

        markup.setKeyboard(List.of(
                new KeyboardRow(List.of(appBtn)),
                new KeyboardRow(List.of(new KeyboardButton("💎 Купить"), new KeyboardButton("🏆 Рефералы"))),
                new KeyboardRow(List.of(new KeyboardButton("📰 Новости"), new KeyboardButton("📖 Инструкции"))),
                new KeyboardRow(List.of(new KeyboardButton("💬 Поддержка"), new KeyboardButton("🌐 Веб-сайт")))
        ));
        return markup;
    }

    public InlineKeyboardMarkup getBuySubKeyboard() {
        InlineKeyboardButton webAppBtn = new InlineKeyboardButton("💳 Перейти к оплате");
        webAppBtn.setWebApp(new WebAppInfo(miniAppUrl));
        return new InlineKeyboardMarkup(List.of(List.of(webAppBtn)));
    }

    public InlineKeyboardMarkup getReferralKeyboard() {
        InlineKeyboardButton share = new InlineKeyboardButton("📩 Поделиться");
        share.setSwitchInlineQuery("\nЗабирай лучший VPN! Летает даже 4K видео 🚀");
        return new InlineKeyboardMarkup(List.of(List.of(share)));
    }
}