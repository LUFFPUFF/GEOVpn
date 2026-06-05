package com.vpn.bot.handler;

import com.vpn.bot.client.UserServiceClient;
import com.vpn.bot.ui.KeyboardFactory;
import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.request.UserRegistrationRequest;
import com.vpn.common.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramStartHandler {

    private final UserServiceClient userServiceClient;
    private final KeyboardFactory keyboardFactory;

    public SendMessage handle(Update update) {
        Message message = update.getMessage();
        if (message == null) {
            return buildFallbackWelcome(0L);
        }

        long chatId   = message.getChatId();
        User tgUser   = message.getFrom();
        String text   = message.getText();

        String referralCode = parseReferralCode(text);
        log.info("[START] chatId={} referralCode='{}'", chatId, referralCode != null ? referralCode : "none");

        boolean isNewUser = registerIfAbsent(chatId, tgUser, referralCode);

        return buildWelcomeMessage(chatId, tgUser, isNewUser, referralCode != null);
    }

    private String parseReferralCode(String text) {
        if (text == null) return null;
        String[] parts = text.trim().split("\\s+", 2);
        if (parts.length < 2 || parts[1].isBlank()) return null;
        return parts[1].trim();
    }

    private boolean registerIfAbsent(long chatId, User tgUser, String referralCode) {
        try {
            ApiResponse<UserResponse> existing = userServiceClient.getMyProfile(chatId);
            if (existing != null && existing.isSuccess() && existing.getData() != null) {
                log.info("[START] User already registered: chatId={}", chatId);
                return false;
            }
        } catch (Exception e) {
            log.debug("[START] Profile check failed (probably new user): chatId={} error={}", chatId, e.getMessage());
        }

        try {
            UserRegistrationRequest request = UserRegistrationRequest.builder()
                    .telegramId(chatId)
                    .username(tgUser != null ? tgUser.getUserName() : null)
                    .firstName(tgUser != null ? tgUser.getFirstName() : null)
                    .referralCode(referralCode)
                    .build();

            ApiResponse<UserResponse> res = userServiceClient.registerUser(request);

            if (res != null && res.isSuccess()) {
                log.info("[START] New user registered: chatId={} referral={}", chatId, referralCode);
                return true;
            } else {
                log.warn("[START] Registration returned non-success: chatId={} message={}", chatId,
                        res != null ? res.getMessage() : "null response");
            }
        } catch (Exception e) {
            log.error("[START] Registration error: chatId={}", chatId, e);
        }
        return false;
    }

    private SendMessage buildWelcomeMessage(long chatId, User tgUser, boolean isNewUser, boolean hasReferral) {
        String firstName = tgUser != null && tgUser.getFirstName() != null ? tgUser.getFirstName() : "пользователь";
        String text;

        if (isNewUser && hasReferral) {
            text = String.join("\n",
                    "🛡 <b>Добро пожаловать в GeoVPN, " + firstName + "!</b>",
                    "",
                    "🎁 <b>По приглашению друга вам начислена бесплатная подписка на 10 дней!</b>",
                    "Приятного использования — никаких логов, полная анонимность.",
                    "",
                    "━━━━━━━━━━━━━━━━━━━━━━",
                    "🌐 <b>Что умеет GeoVPN:</b>",
                    "",
                    "🔑 <b>Конфиги</b> — создавайте ключи доступа для серверов в Финляндии, Германии, Нидерландах. Импортируйте одним нажатием через Mini App.",
                    "",
                    "📱 <b>Устройства</b> — подключайте смартфон, ПК, планшет. Управляйте лимитами и сессиями.",
                    "",
                    "💎 <b>Подписка</b> — гибкие тарифы от 100 ₽/мес. Чем дольше — тем выгоднее.",
                    "",
                    "🏆 <b>Рефералы</b> — приглашайте друзей, получайте бонусы и участвуйте в топ-рейтинге.",
                    "",
                    "📖 <b>Инструкции</b> — пошаговые гайды для Android, iOS, Windows, macOS и TV.",
                    "",
                    "━━━━━━━━━━━━━━━━━━━━━━",
                    "👇 <i>Используйте меню ниже для навигации</i>"
            );
        } else if (isNewUser) {
            text = String.join("\n",
                    "🛡 <b>Добро пожаловать в GeoVPN, " + firstName + "!</b>",
                    "",
                    "Быстрый и надёжный VPN без ограничений по скорости.",
                    "",
                    "━━━━━━━━━━━━━━━━━━━━━━",
                    "🌐 <b>Что вы можете сделать прямо сейчас:</b>",
                    "",
                    "🔑 <b>Получить ключ доступа</b> — нажмите «🔑 Конфиги» → выберите страну сервера → ключ готов.",
                    "",
                    "📱 <b>Подключить устройство</b> — раздел «📱 Устройства» покажет, сколько слотов доступно.",
                    "",
                    "💎 <b>Выбрать тариф</b> — кнопка «💎 Купить» откроет список планов от 100 ₽/мес.",
                    "",
                    "🏆 <b>Пригласить друга</b> — раздел «🏆 Рефералы» даст вашу уникальную ссылку с бонусом.",
                    "",
                    "━━━━━━━━━━━━━━━━━━━━━━",
                    "👇 <i>Нажмите кнопку ниже или воспользуйтесь меню</i>"
            );
        } else {
            text = String.join("\n",
                    "👋 <b>С возвращением, " + firstName + "!</b>",
                    "",
                    "Ваш GeoVPN-аккаунт активен. Используйте меню для навигации:",
                    "",
                    "👤 <b>Профиль</b> — баланс, статус подписки",
                    "🔑 <b>Конфиги</b> — ключи доступа и выбор сервера",
                    "📱 <b>Устройства</b> — менеджер подключенных устройств",
                    "💎 <b>Купить</b> — пополнение и тарифы",
                    "🏆 <b>Рефералы</b> — ваша реферальная программа",
                    "📖 <b>Инструкции</b> — настройка клиентов"
            );
        }

        SendMessage msg = new SendMessage(String.valueOf(chatId), text);
        msg.setParseMode("HTML");
        msg.setReplyMarkup(keyboardFactory.getWelcomeKeyboard());
        return msg;
    }

    private SendMessage buildFallbackWelcome(long chatId) {
        SendMessage msg = new SendMessage(String.valueOf(chatId),
                "🛡 <b>GeoVPN</b> — используйте меню ниже для навигации.");
        msg.setParseMode("HTML");
        return msg;
    }
}