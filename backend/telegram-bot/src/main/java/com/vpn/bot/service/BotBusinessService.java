package com.vpn.bot.service;

import com.vpn.bot.client.UserServiceClient;
import com.vpn.bot.client.VpnServiceClient;
import com.vpn.bot.core.MessageSender;
import com.vpn.bot.ui.KeyboardFactory;
import com.vpn.bot.util.ImageDecoder;
import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.request.UserRegistrationRequest;
import com.vpn.common.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.InputStream;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotBusinessService {

    private final UserServiceClient userService;
    private final MessageSender sender;
    private final KeyboardFactory keyboardFactory;

    public void processStartCommand(long chatId, String firstName, String username) {
        try {
            userService.registerUser(UserRegistrationRequest.builder()
                    .telegramId(chatId)
                    .firstName(firstName)
                    .username(username)
                    .build());
        } catch (Exception e) {
            log.debug("User already exists or service down");
        }

        String text = "🛡 <b>GEOVpn — Твой безопасный интернет</b>\n\n" +
                "Привет, <b>" + firstName + "</b>! \n" +
                "Твой личный кабинет управления VPN уже готов.";

        InputStream img = getClass().getResourceAsStream("/birds.jpg");
        if (img != null) {
            SendPhoto photo = new SendPhoto(String.valueOf(chatId), new InputFile(img, "birds.jpg"));
            photo.setCaption(text);
            photo.setParseMode("HTML");
            photo.setReplyMarkup(keyboardFactory.getMainReplyKeyboard());
            sender.executePhoto(photo);
        } else {
            sendSimpleText(chatId, text, true);
        }
    }

    public void sendSubscriptionOptions(long chatId) {
        String text = "💎 <b>Тарифы GeoVPN</b>\n\n" +
                "⚪️ <b>Стандарт</b> — 150 ₽\n" +
                "└ 1 устройство | Базовый доступ\n\n" +
                "🔵 <b>Оптима</b> — 400 ₽\n" +
                "└ 3 устройства | Личное пользование\n\n" +
                "🟢 <b>Семья</b> — 700 ₽ 🔥 <b>Hit!</b>\n" +
                "└ 5 устройств | Для всех гаджетов\n\n" +
                "🟣 <b>Бизнес</b> — 2 000 ₽\n" +
                "└ 15 устройств | Для команд\n\n" +
                "🟡 <b>Безлимит</b> — 3 000 ₽\n" +
                "└ ∞ устройств | Полная свобода\n\n" +
                "🚀 <b>Все тарифы включают:</b>\n" +
                "✓ YouTube/TikTok без рекламы\n" +
                "✓ Минимальный пинг для игр\n" +
                "✓ 30+ локаций мира";

        SendMessage msg = new SendMessage(String.valueOf(chatId), text);
        msg.setParseMode("HTML");
        msg.setReplyMarkup(keyboardFactory.getBuySubKeyboard());
        sender.execute(msg);
    }

    public void sendReferralStats(long chatId) {
        ApiResponse<UserResponse> res = userService.getMyProfile(chatId);
        UserResponse u = res != null ? res.getData() : null;

        String text = "🏆 <b>Реферальная программа</b>\n\n" +
                "Приглашайте друзей и получайте бонусы!\n\n" +
                "🔗 <b>Ваша ссылка:</b>\n" +
                "<code>https://t.me/geovpn_bot?start=" + chatId + "</code>\n\n" +
                "📊 <b>Текущий месяц:</b>\n" +
                "👤 Приглашено: 0\n" +
                "✅ С покупкой: 0\n" +
                "💰 Заработано: 0 ₽\n\n" +
                "💳 Ваш баланс: " + (u != null ? u.getBalance()/100 : 0) + " ₽";

        SendMessage msg = new SendMessage(String.valueOf(chatId), text);
        msg.setParseMode("HTML");
        msg.setReplyMarkup(keyboardFactory.getReferralKeyboard());
        sender.execute(msg);
    }

    public void sendWebSiteInfo(long chatId) {
        String text = "🌐 <b>Личный кабинет на сайте</b>\n\n" +
                "Для входа на сайт используется ваш email.\n" +
                "Отправьте ваш email следующим сообщением:\n\n" +
                "<i>Например: user@mail.ru</i>";
        sendSimpleText(chatId, text, true);
    }

    public void sendInstructions(long chatId) {
        String text = "📖 <b>Инструкции по подключению</b>\n\n" +
                "Для настройки VPN на вашем устройстве воспользуйтесь Mini App (кнопка 'Открыть приложение') или выберите платформу ниже:";
        sendSimpleText(chatId, text, true);
    }

    public void sendSupport(long chatId) {
        sendSimpleText(chatId, "💬 <b>Техническая поддержка</b>\n\nНапишите нашему специалисту: @a1ecksa", true);
    }

    public void sendSimpleText(long chatId, String text, boolean withMenu) {
        SendMessage msg = new SendMessage(String.valueOf(chatId), text);
        msg.setParseMode("HTML");
        if (withMenu) msg.setReplyMarkup(keyboardFactory.getMainReplyKeyboard());
        sender.execute(msg);
    }

    public void sendNews(long chatId) {
        sendSimpleText(chatId, "💬 <b>Пока не работает", true);
    }
}