package com.vpn.bot.handler;

import com.vpn.bot.service.DeviceRegistrationBotService;
import com.vpn.bot.ui.KeyboardFactory;
import com.vpn.common.dto.response.VpnConfigResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;

import java.util.List;

/**
 * Обрабатывает команду /start.
 *
 * Проблема: Telegram Bot API не передаёт ОС пользователя напрямую.
 *
 * Решение — двухшаговое:
 *
 *   Шаг 1 (этот класс): При /start пытаемся определить платформу
 *          из языковых настроек, Telegram версии и других косвенных признаков.
 *          Если определить невозможно — показываем inline keyboard с выбором ОС.
 *
 * Дополнительный способ (более точный):
 *   Через Telegram Web App в inline-кнопке (type=web_app) передаётся
 *   window.Telegram.WebApp.platform ("ios" | "android" | "tdesktop" | "macos" | "web")
 *   Это самый надёжный способ — рекомендуется для продакшн.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramStartHandler {

    @Value("${app.miniapp-url}")
    private String miniAppUrl;

    public SendMessage handle(Update update) {
        Long chatId;
        String firstName;

        if (update.hasMessage()) {
            chatId = update.getMessage().getChatId();
            firstName = update.getMessage().getFrom().getFirstName();
        } else if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
            firstName = update.getCallbackQuery().getFrom().getFirstName();
        } else {
            log.warn("Получен неподдерживаемый тип Update в TelegramStartHandler");
            return null;
        }

        String text = "🛡 <b>GeoVPN — Ваш быстрый и свободный интернет</b>\n\n" +
                "Привет, <b>" + firstName + "</b>! 👋\n\n" +
                "Мы сделали всё, чтобы интернет работал стабильно и безопасно.\n\n" +
                "✨ <b>Что вы получаете:</b>\n" +
                "• YouTube, Instagram и TikTok без зависаний\n" +
                "• Простую настройку всего в 2 клика\n" +
                "• Стабильную работу (не садит батарею)\n" +
                "• <b>Первый месяц — абсолютно бесплатно!</b> 🎁\n\n" +
                "<i>\uD83D\uDC47 Воспользуйтесь меню ниже, чтобы начать:</i>";

        InlineKeyboardButton appBtn = new InlineKeyboardButton("🚀 Открыть GeoVPN");
        appBtn.setWebApp(new WebAppInfo(miniAppUrl));

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(appBtn))
                .build();

        return SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("HTML")
                .replyMarkup(keyboard)
                .build();
    }
}


