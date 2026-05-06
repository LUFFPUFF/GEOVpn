package com.vpn.bot.service;

import com.vpn.bot.client.UserServiceClient;
import com.vpn.bot.core.MessageSender;
import com.vpn.bot.ui.KeyboardFactory;
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

        String text = "🛡 <b>GeoVPN — Ваш проводник в свободный интернет</b>\n\n" +
                "Рады видеть вас, <b>" + firstName + "</b>! 🤝\n\n" +
                "Мы используем передовые протоколы, которые работают стабильно и не садят батарею:\\n\\n" +
                "⚡️ <b>VLESS</b> — Стабильный VPN под обычный веб-серфинг. Надежно и безопасно.\n" +
                "🚀 <b>Hysteria2 (HY2)</b> — турбо-скорость даже на слабом интернете. Идеально для 4K-видео и игр.\n" +
                "🛡 <b>Smart Shield</b> — стабильное соединение в любых сетевых условиях.\n\n" +
                "<b>Почему выбирают нас:</b>\n" +
                "• Установка в 2 клика\n" +
                "• Безлимитный трафик\n" +
                "• Полная анонимность\n" +
                "• Первый месяц — бесплатно 🎁\n\n" +
                "<i>👇 Выберите действие в меню ниже, чтобы начать:</i>";

        InputStream img = getClass().getResourceAsStream("/header1.png");
        if (img != null) {
            SendPhoto photo = new SendPhoto(String.valueOf(chatId), new InputFile(img, "header.png"));
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
                "🟢 <b>Семья</b> — 700 ₽ 🔥 <i>Хит!</i>\n" +
                "└ 5 устройств | Для всех гаджетов\n\n" +
                "🟣 <b>Бизнес</b> — 2 000 ₽\n" +
                "└ 15 устройств | Для команд\n\n" +
                "🟡 <b>Безлимит</b> — 3 000 ₽\n" +
                "└ ∞ устройств | Полная свобода\n\n" +
                "🌟 <b>В каждый тариф включено:</b>\n" +
                "✓ Доступ к YouTube, Instagram и TikTok\n" +
                "✓ Минимальный пинг и стабильность\n" +
                "✓ 30+ премиум-локаций по всему миру";

        SendMessage msg = new SendMessage(String.valueOf(chatId), text);
        msg.setParseMode("HTML");
        msg.setReplyMarkup(keyboardFactory.getBuySubKeyboard());
        sender.execute(msg);
    }

    public void sendReferralStats(long chatId) {
        ApiResponse<UserResponse> res = userService.getMyProfile(chatId);
        UserResponse u = res != null ? res.getData() : null;
        long balance = (u != null && u.getBalance() != null) ? u.getBalance() / 100 : 0;

        String text = "🤝 <b>Партнерская программа</b>\n\n" +
                "Делитесь свободным интернетом с друзьями и получайте бонусы на баланс!\n\n" +
                "🔗 <b>Ваша пригласительная ссылка:</b>\n" +
                "<code>https://t.me/geovpn_bot?start=" + chatId + "</code>\n\n" +
                "📊 <b>Ваша статистика:</b>\n" +
                "👥 Приглашено друзей: 0\n" +
                "💳 Оплатили подписку: 0\n" +
                "🎁 Заработано: 0 ₽\n\n" +
                "💰 <b>Ваш текущий баланс:</b> " + balance + " ₽";

        SendMessage msg = new SendMessage(String.valueOf(chatId), text);
        msg.setParseMode("HTML");
        msg.setReplyMarkup(keyboardFactory.getReferralKeyboard());
        sender.execute(msg);
    }

    public void sendWebSiteInfo(long chatId) {
        String text = """
                🌐 <b>Личный кабинет</b>
                
                Для авторизации на сайте мы используем ваш email. Это безопасно и удобно.
                
                📩 Пожалуйста, отправьте ваш email ответным сообщением.
                
                <i>Пример: user@example.com</i>""";
        sendSimpleText(chatId, text, true);
    }

    public void sendInstructions(long chatId) {
        String text = "⚙️ <b>Как подключиться?</b>\n\n" +
                "Всё управление подпиской, настройка и получение ключей происходят в нашем удобном <b>Mini App</b>.\n\n" +
                "Нажмите кнопку <b>«Открыть приложение»</b> в меню бота, чтобы настроить VPN за пару кликов!";
        sendSimpleText(chatId, text, true);
    }

    public void sendSupport(long chatId) {
        String text = "👨‍💻 <b>Служба заботы</b>\n\n" +
                "Возникли трудности или есть вопросы? Мы всегда на связи и готовы помочь.\n\n" +
                "💬 Написать специалисту: @knyazheskyy";
        sendSimpleText(chatId, text, true);
    }

    public void sendNews(long chatId) {
        String text = "📰 <b>Следите за новостями</b>\n\n" +
                "Обновления серверов, скидки и важная информация о работе сервиса — в нашем официальном канале.\n\n" +
                "👉 <b>Присоединяйтесь:</b> <a href=\"https://t.me/+yuKUzLhYdJVjOWRi\">GEO NEWS</a>";
        sendSimpleText(chatId, text, true);
    }

    public void sendSimpleText(long chatId, String text, boolean withMenu) {
        SendMessage msg = new SendMessage(String.valueOf(chatId), text);
        msg.setParseMode("HTML");
        if (withMenu) msg.setReplyMarkup(keyboardFactory.getMainReplyKeyboard());
        sender.execute(msg);
    }
}