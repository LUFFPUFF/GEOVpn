package com.vpn.bot.service;

import com.vpn.bot.dto.*;
import lombok.extern.slf4j.Slf4j;
import com.vpn.bot.client.UserServiceClient;
import com.vpn.bot.client.VpnServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final String miniAppUrl;
    private final UserServiceClient userService;
    private final VpnServiceClient vpnService;
    private final ImageDecoder imageDecoder;

    // Пул потоков для мгновенной обработки сотен юзеров
    private final ExecutorService executor = Executors.newFixedThreadPool(100);

    public TelegramBot(
            DefaultBotOptions options, // Добавлен параметр для прокси
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${app.miniapp-url}") String miniAppUrl,
            UserServiceClient userService,
            VpnServiceClient vpnService,
            ImageDecoder imageDecoder) {

        // Передаем настройки прокси и токен в родительский класс
        super(options, botToken);

        this.botUsername = botUsername;
        this.miniAppUrl = miniAppUrl.replaceAll("\\s", "");
        this.userService = userService;
        this.vpnService = vpnService;
        this.imageDecoder = imageDecoder;
        log.info("🚀 GEOVPN BOT READY. URL: {}", this.miniAppUrl);
    }

    @Override
    public String getBotUsername() { return botUsername; }

    @Override
    public void onUpdateReceived(Update update) {
        // Каждое входящее сообщение обрабатываем в отдельном потоке мгновенно
        executor.submit(() -> {
            try {
                if (update.hasMessage() && update.getMessage().hasText()) {
                    handleTextMessage(update);
                } else if (update.hasCallbackQuery()) {
                    handleCallback(update);
                }
            } catch (Exception e) {
                log.error("Error in thread pool: {}", e.getMessage());
            }
        });
    }

    private void handleTextMessage(Update update) {
        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();
        String firstName = update.getMessage().getFrom().getFirstName();

        log.info("📥 Message from {}: {}", chatId, text);

        if (text.startsWith("/start")) {
            // ФОНОВАЯ РЕГИСТРАЦИЯ
            CompletableFuture.runAsync(() -> {
                try {
                    userService.registerUser(UserRegistrationRequest.builder()
                            .telegramId(chatId)
                            .firstName(firstName)
                            .username(update.getMessage().getFrom().getUserName())
                            .build());
                } catch (Exception e) {
                    log.debug("User {} already registered", chatId);
                }
            }, executor);

            sendWelcomeMessage(chatId, firstName);
        }
        else if (text.contains("Профиль")) fetchAndSendProfile(chatId);
        else if (text.contains("Мои ключи")) fetchAndSendConfig(chatId);
        else if (text.contains("Новости")) sendNews(chatId);
        else if (text.contains("Поддержка")) sendSupport(chatId);
    }

    private void handleCallback(Update update) {
        String callData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        try {
            if ("show_profile".equals(callData)) fetchAndSendProfile(chatId);
            else if ("get_config".equals(callData)) fetchAndSendConfig(chatId);
            else if ("show_news".equals(callData)) sendNews(chatId);
            else if ("show_support".equals(callData)) sendSupport(chatId);
            execute(new AnswerCallbackQuery(update.getCallbackQuery().getId()));
        } catch (Exception e) { log.error("Callback error", e); }
    }

    private void sendWelcomeMessage(long chatId, String firstName) {
        String text = "🛡 <b>GEOVpn — Твой безопасный интернет</b>\n\n" +
                "Привет, <b>" + firstName + "</b>! \n" +
                "Твой личный кабинет управления VPN уже готов.";

        InputStream img = getClass().getResourceAsStream("/background.png");
        if (img != null) {
            SendPhoto photo = new SendPhoto();
            photo.setChatId(String.valueOf(chatId));
            photo.setCaption(text);
            photo.setParseMode("HTML");
            photo.setPhoto(new InputFile(img, "background.png"));
            photo.setReplyMarkup(getInlineKeyboard());
            try {
                execute(photo);
                sendText(chatId, "Клавиатура активирована 👇", true);
                return;
            } catch (Exception e) { log.error("Photo failed: {}", e.getMessage()); }
        }
        sendText(chatId, text, true);
    }

    private void sendNews(long chatId) {
        String text = "📰 <b>Новости сервиса</b>\n\n" +
                "📢 Инфо-канал: <a href='https://t.me/+yuKUzLhYdJVjOWRi'>GEOVpn News</a>\n\n" +
                "🔔 <b>В канале вы узнаете:</b>\n" +
                "- Обновления локаций\n" +
                "- Акции и скидки\n" +
                "- Технические работы";
        sendText(chatId, text, true);
    }

    private void sendSupport(long chatId) {
        String text = "👨‍💻 <b>Служба поддержки</b>\n\n" +
                "💬 Написать специалисту: @a1ecksa";
        sendText(chatId, text, true);
    }

    // --- КЛАВИАТУРЫ ---

    private ReplyKeyboardMarkup getReplyKeyboard() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(false);
        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow r1 = new KeyboardRow();
        KeyboardButton app = new KeyboardButton("🚀 Открыть приложение");
        app.setWebApp(new WebAppInfo(miniAppUrl));
        r1.add(app);

        KeyboardRow r2 = new KeyboardRow();
        r2.add(new KeyboardButton("👤 Профиль"));
        r2.add(new KeyboardButton("🔑 Мои ключи"));

        KeyboardRow r3 = new KeyboardRow();
        r3.add(new KeyboardButton("📰 Новости"));
        r3.add(new KeyboardButton("👨‍💻 Поддержка"));

        rows.add(r1); rows.add(r2); rows.add(r3);
        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardMarkup getInlineKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton web = new InlineKeyboardButton("🚀 Запустить Mini App");
        web.setWebApp(new WebAppInfo(miniAppUrl));

        List<InlineKeyboardButton> r1 = new ArrayList<>(); r1.add(web);
        List<InlineKeyboardButton> r2 = new ArrayList<>();
        r2.add(createBtn("🔑 Ключи", "get_config"));
        r2.add(createBtn("👤 Профиль", "show_profile"));

        rows.add(r1); rows.add(r2);
        markup.setKeyboard(rows);
        return markup;
    }

    private void fetchAndSendProfile(long chatId) {
        try {
            ApiResponse<UserResponse> res = userService.getMyProfile(chatId);
            if (res != null && res.isSuccess()) {
                UserResponse u = res.getData();
                String txt = "👤 <b>Ваш профиль</b>\n\n💰 Баланс: " + (u.getBalance()/100.0) + " ₽\n💎 Статус: " + (Boolean.TRUE.equals(u.getHasActiveSubscription()) ? "Active ✅" : "Inactive ❌");
                sendText(chatId, txt, true);
            }
        } catch (Exception e) { sendText(chatId, "❌ Ошибка профиля", true); }
    }

    private void fetchAndSendConfig(long chatId) {
        try {
            ApiResponse<List<VpnConfigResponse>> existing = vpnService.getMyConfigs(chatId);
            if (existing != null && existing.isSuccess() && !existing.getData().isEmpty()) {
                sendConfigMessage(chatId, existing.getData().get(0));
            } else {
                ApiResponse<VpnConfigResponse> res = vpnService.createConfig(chatId, ConfigCreateRequest.builder().userId(chatId).deviceId(1L).protocol("VLESS").build());
                if (res != null && res.isSuccess()) sendConfigMessage(chatId, res.getData());
                else sendText(chatId, "❌ Не удалось создать ключ", true);
            }
        } catch (Exception e) { sendText(chatId, "❌ Ошибка VPN", true); }
    }

    private void sendConfigMessage(long chatId, VpnConfigResponse config) {
        SendPhoto p = new SendPhoto();
        p.setChatId(String.valueOf(chatId));
        p.setCaption("✅ <b>Ваш ключ:</b>\n\n<code>" + config.getVlessLink() + "</code>");
        p.setParseMode("HTML");
        p.setReplyMarkup(getReplyKeyboard());
        if (config.getQrCodeDataUrl() != null) {
            InputStream is = imageDecoder.decodeBase64(config.getQrCodeDataUrl());
            if (is != null) p.setPhoto(new InputFile(is, "qr.png"));
        }
        try { execute(p); } catch (Exception e) { sendText(chatId, p.getCaption(), true); }
    }

    private void sendText(long chatId, String text, boolean withMenu) {
        SendMessage msg = new SendMessage(String.valueOf(chatId), text);
        msg.setParseMode("HTML");
        msg.setDisableWebPagePreview(true);
        if (withMenu) msg.setReplyMarkup(getReplyKeyboard());
        try { execute(msg); } catch (Exception e) { log.error("Send failed: {}", e.getMessage()); }
    }

    private InlineKeyboardButton createBtn(String text, String data) {
        InlineKeyboardButton b = new InlineKeyboardButton(text);
        b.setCallbackData(data);
        return b;
    }
}