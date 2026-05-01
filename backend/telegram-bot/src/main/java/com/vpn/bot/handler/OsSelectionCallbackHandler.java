package com.vpn.bot.handler;

import com.vpn.bot.service.DeviceRegistrationBotService;
import com.vpn.bot.ui.KeyboardFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
@RequiredArgsConstructor
public class OsSelectionCallbackHandler {

    private final DeviceRegistrationBotService deviceRegistrationService;
    private final KeyboardFactory keyboardFactory;

    public SendMessage handle(Update update) {
        var query = update.getCallbackQuery();
        User tgUser = query.getFrom();
        Long chatId = query.getMessage().getChatId();
        String os = query.getData().replace("os_select:", "");

        deviceRegistrationService.registerDeviceOnly(tgUser.getId(), os);

        String text = """
                👋 Привет, <b>%s</b>!
                
                🎁 <b>Подключись к VPN бесплатно!</b>
                └ Дарим тебе месяц премиум доступа
                
                🤝 <b>Реферальная программа:</b>
                └ Приглашай друзей и получай:
                   • 50₽ за каждого пользователя
                   • 30%% со всех пополнений
                
                📱 <b>Поддерживаемые устройства:</b>
                └ iOS, Android, MacOS и Windows, AndroidTV
                
                ⬇️ <b>Жми кнопку для подключения!</b> ⬇️
                """.formatted(tgUser.getFirstName());

        return SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("HTML")
                .replyMarkup(keyboardFactory.getMainReplyKeyboard())
                .build();
    }
}