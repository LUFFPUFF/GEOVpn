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

    public InlineKeyboardMarkup getProfileKeyboard(boolean hasActive) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(createButton("🔄 Обновить", "profile_refresh")));
        if (!hasActive) {
            rows.add(List.of(createButton("💳 Купить подписку", "profile_subscribe")));
        }
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup getDevicesKeyboard(boolean limitReached) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(createButton("🔄 Обновить", "devices_refresh")));
        if (limitReached) {
            rows.add(List.of(createButton("➕ Купить слот (+1)", "device_buy_slot")));
        }
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup getConfigsKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(
                createButton("🇫🇮 FI", "config_create:FI"),
                createButton("🇩🇪 DE", "config_create:DE"),
                createButton("🇳🇱 NL", "config_create:NL")
        ));
        rows.add(List.of(createButton("🔄 Обновить список", "configs_refresh")));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup getBuySubKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(createButton("Месяц — 100₽", "sub_1")));
        rows.add(List.of(createButton("3 Месяца — 250₽", "sub_3")));
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

    public InlineKeyboardMarkup getLeaderboardKeyboard() {
        return new InlineKeyboardMarkup(List.of(List.of(createButton("🔄 Обновить ТОП", "leaderboard_refresh"))));
    }

    public InlineKeyboardMarkup getReferralKeyboard() {
        return new InlineKeyboardMarkup(List.of(List.of(createButton("✏️ Изменить код", "referral_change_code"))));
    }

    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(text);
        btn.setCallbackData(callbackData);
        return btn;
    }
}