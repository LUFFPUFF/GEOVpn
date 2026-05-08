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

        String text = "🛡 <b>GeoVPN — Ваш быстрый и свободный интернет</b>\n\n" +
                "Привет, <b>" + firstName + "</b>! 👋\n\n" +
                "Мы сделали всё, чтобы интернет работал стабильно и безопасно.\n\n" +
                "✨ <b>Что вы получаете:</b>\n" +
                "• YouTube, Instagram и TikTok без зависаний\n" +
                "• Простую настройку всего в 2 клика\n" +
                "• Стабильную работу (не садит батарею)\n" +
                "• <b>Первый месяц — абсолютно бесплатно!</b> 🎁\n\n" +
                "<i>\uD83D\uDC47 Воспользуйтесь меню ниже, чтобы начать:</i>";

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
                "⚪️ <b>Стандарт</b> — 75 ₽ / мес\n" +
                "└ 1 устройство | 3 сервера \n\n" +
                "🔵 <b>Премиум</b> — 150 ₽ / мес 🔥 <i>Хит!</i>\n" +
                "└ 2 устройства | Все серверы и приоритет\n\n" +
                "🟢 <b>Семья</b> — 350 ₽ / мес\n" +
                "└ До 3 устройств | Для всей семьи\n\n" +
                "🌟 <b>В каждый тариф включено:</b>\n" +
                "✓ Безлимитный трафик\n" +
                "✓ Надежное шифрование AES-256\n" +
                "✓ Доступ к YouTube, Instagram и TikTok без потери скорости";

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
                "💬 Написать специалисту: @geo_vpn_support";
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