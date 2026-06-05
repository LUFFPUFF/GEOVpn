package com.vpn.bot.ui;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Component
public class KeyboardFactory {

    public ReplyKeyboardMarkup getMainReplyKeyboard() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setSelective(true);
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("👤 Профиль"));
        row1.add(new KeyboardButton("🔑 Конфиги"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("📱 Устройства"));
        row2.add(new KeyboardButton("🏆 Рефералы"));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("💎 Купить"));
        row3.add(new KeyboardButton("📖 Инструкции"));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        markup.setKeyboard(keyboard);
        return markup;
    }

    public InlineKeyboardMarkup getProfileKeyboard(boolean hasActive, int balance) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if (hasActive) {
            rows.add(List.of(createButton("📋 Моя подписка/ключи", "menu_configs")));
            rows.add(List.of(createButton("📱 Устройства", "menu_devices")));
        } else {
            rows.add(List.of(createButton("💎 Купить подписку", "profile_subscribe")));
            rows.add(List.of(createButton("📱 Устройства", "menu_devices")));
        }

        rows.add(List.of(
                createButton("💳 Пополнить баланс", "top_up_balance"),
                createButton("🔄 Обновить", "profile_refresh")
        ));

        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup getDevicesKeyboard(boolean limitReached) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if (limitReached) {
            rows.add(List.of(createButton("➕ Добавить слот (+1 устройство)", "device_buy_slot")));
        }
        rows.add(List.of(
                createButton("🔑 Мои конфиги", "menu_configs"),
                createButton("🔄 Обновить", "devices_refresh")
        ));

        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup getConfigsKeyboard(boolean hasActive, String subscriptionUrl) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if (!hasActive || subscriptionUrl == null || subscriptionUrl.isBlank()) {
            rows.add(List.of(createButton("💎 Купить подписку для создания ключей", "profile_subscribe")));
        } else {
            String uuid = subscriptionUrl.substring(subscriptionUrl.lastIndexOf("/") + 1);
            String importUrl = "https://geovp.ru/api/v1/subscription/" + uuid + "/import-happ";

            rows.add(List.of(createUrlButton("⚡️ Авто-импорт в Happ Proxy", importUrl)));
        }

        rows.add(List.of(
                createButton("📱 Устройства", "menu_devices"),
                createButton("🔄 Обновить", "configs_refresh")
        ));

        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup getBuySubKeyboard(boolean promoAvailable) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if (promoAvailable) {
            rows.add(List.of(createButton("🎁 Забрать бесплатно (Промо 30д)", "buy_sub_tariff:BASIC:true")));
        }

        rows.add(List.of(createButton("⏱ Пробный (1 день) — 6 ₽", "buy_sub_tariff:DAILY:false")));
        rows.add(List.of(createButton("📱 Стандарт (1 мес) — 100 ₽", "buy_sub_tariff:BASIC:false")));
        rows.add(List.of(createButton("⚡️ Премиум (2 устр) — 150 ₽", "buy_sub_tariff:STANDARD:false")));
        rows.add(List.of(createButton("👥 Семья (3 устр) — 350 ₽", "buy_sub_tariff:FAMILY:false")));

        rows.add(List.of(createButton("◀️ Назад в профиль", "profile_refresh")));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup getTopUpKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(
                createButton("100 ₽", "topup_100"),
                createButton("250 ₽", "topup_250"),
                createButton("500 ₽", "topup_500")
        ));
        rows.add(List.of(createButton("◀️ Назад", "profile_refresh")));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup getConfirmSlotKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(
                createButton("✅ Подтвердить", "device_slot_confirm"),
                createButton("❌ Отмена", "device_slot_cancel")
        ));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup getReferralKeyboard(long chatId) {
        String shareLink = "https://t.me/share/url?url=https://t.me/geovpbot?start=" + chatId;
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(
                createUrlButton("📤 Поделиться ссылкой", shareLink)
        ));
        rows.add(List.of(
                createButton("🏆 Топ рефералов", "show_leaderboard"),
                createButton("✏️ Сменить код", "referral_change_code")
        ));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup getLeaderboardKeyboard() {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        createButton("🔄 Обновить", "leaderboard_refresh"),
                        createButton("◀️ Мои рефералы", "menu_referrals")
                )
        ));
    }

    public InlineKeyboardMarkup getWelcomeKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(createButton("👤 Мой профиль", "profile_refresh")));
        rows.add(List.of(
                createButton("💎 Купить подписку", "profile_subscribe"),
                createButton("📖 Инструкции", "menu_instructions")
        ));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup getInstructionsKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(createUrlButton("📖 Открыть инструкции", "https://geovp.ru/instructions")));
        rows.add(List.of(
                createButton("🔑 Моя подписка/ключи", "menu_configs"),
                createButton("💬 Поддержка", "menu_support")
        ));
        return new InlineKeyboardMarkup(rows);
    }

    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(text);
        btn.setCallbackData(callbackData);
        return btn;
    }

    private InlineKeyboardButton createUrlButton(String text, String url) {
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(text);
        btn.setUrl(url);
        return btn;
    }
}