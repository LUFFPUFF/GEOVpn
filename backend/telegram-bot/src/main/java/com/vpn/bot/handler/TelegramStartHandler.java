package com.vpn.bot.handler;

import com.vpn.bot.service.DeviceRegistrationBotService;
import com.vpn.bot.ui.KeyboardFactory;
import com.vpn.common.dto.response.VpnConfigResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

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
 *   Шаг 2: Пользователь нажимает кнопку → {@link OsSelectionCallbackHandler}
 *          создаёт устройство с выбранной ОС и генерирует конфиг.
 *
 * Дополнительный способ (более точный):
 *   Через Telegram Web App в inline-кнопке (type=web_app) передаётся
 *   window.Telegram.WebApp.platform ("ios" | "android" | "tdesktop" | "macos" | "web")
 *   Это самый надёжный способ — рекомендуется для продакшн.
 */
@Component
@RequiredArgsConstructor
public class TelegramStartHandler {

    private final DeviceRegistrationBotService deviceRegistrationService;

    public SendMessage handle(Update update) {
        User tgUser = update.getMessage().getFrom();
        Long chatId = update.getMessage().getChatId();

        deviceRegistrationService.registerUserIfAbsent(tgUser);

        String text = "👋 Привет, " + tgUser.getFirstName() + "!\n\nДля настройки выбери свою ОС:";

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        btn("📱 iOS", "os_select:iOS"),
                        btn("🤖 Android", "os_select:Android")
                ))
                .keyboardRow(List.of(
                        btn("🪟 Windows", "os_select:Windows"),
                        btn("🍏 macOS", "os_select:macOS")
                ))
                .build();

        return SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboard)
                .build();
    }

    private InlineKeyboardButton btn(String text, String data) {
        return InlineKeyboardButton.builder().text(text).callbackData(data).build();
    }
}


