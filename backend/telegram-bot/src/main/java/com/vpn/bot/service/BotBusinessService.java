package com.vpn.bot.service;

import com.vpn.bot.client.BillingServiceClient;
import com.vpn.bot.client.UserServiceClient;
import com.vpn.bot.client.VpnServiceClient;
import com.vpn.bot.core.MessageSender;
import com.vpn.bot.ui.KeyboardFactory;
import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.request.DepositRequest;
import com.vpn.common.dto.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotBusinessService {

    private final UserServiceClient userService;
    private final VpnServiceClient vpnService;
    private final MessageSender sender;
    private final KeyboardFactory keyboardFactory;
    private final BillingServiceClient billingServiceClient;

    public void generatePaymentLink(long chatId, int amount, Integer messageId) {
        try {
            DepositRequest request = DepositRequest.builder()
                    .amount(amount)
                    .build();

            ApiResponse<DepositResponse> res = billingServiceClient.createDeposit(chatId, request);

            if (res != null && res.isSuccess() && res.getData() != null) {
                String payUrl = res.getData().getPaymentUrl();

                InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                        .keyboardRow(List.of(
                                InlineKeyboardButton.builder()
                                        .text("💳 Оплатить " + amount + " ₽")
                                        .url(payUrl)
                                        .build()
                        ))
                        .keyboardRow(List.of(
                                InlineKeyboardButton.builder()
                                        .text("◀️ Назад")
                                        .callbackData("profile_refresh")
                                        .build()
                        ))
                        .build();

                String text = "🧾 <b>Счет на пополнение баланса сформирован</b>\n\n" +
                        "Сумма пополнения: <b>" + amount + " ₽</b>\n\n" +
                        "<i>После успешной оплаты баланс обновится автоматически, и вы сможете приобрести тариф.</i>";

                sendOrEdit(chatId, text, markup, messageId);
            } else {
                String error = (res != null && res.getMessage() != null) ? res.getMessage() : "Ошибка при создании счета";
                sendOrEdit(chatId, "❌ " + error, keyboardFactory.getProfileKeyboard(false, 0), messageId);
            }
        } catch (Exception e) {
            log.error("Payment link generation error", e);
            sendOrEdit(chatId, "❌ Временная ошибка сервиса оплаты. Пожалуйста, попробуйте позже.", null, messageId);
        }
    }

    public void purchaseSubscription(long chatId, String planId, boolean isPromo, Integer messageId) {
        try {
            ApiResponse<UserResponse> profileRes = userService.getMyProfile(chatId);
            UserResponse u = (profileRes != null) ? profileRes.getData() : null;

            if (u == null) {
                sendSimpleText(chatId, "❌ Не удалось загрузить данные профиля.", true);
                return;
            }

            int price = switch (planId) {
                case "DAILY" -> 6;
                case "BASIC" -> isPromo ? 0 : 100;
                case "STANDARD" -> 150;
                case "FAMILY" -> 350;
                default -> 0;
            };

            int priceKopecks = price * 100;

            if (u.getBalance() < priceKopecks) {
                String text = "❌ <b>Недостаточно средств на балансе!</b>\n\n" +
                        "Стоимость тарифа: <b>" + price + " ₽</b>\n" +
                        "Ваш баланс: <b>" + String.format("%.2f", (double) u.getBalance() / 100) + " ₽</b>\n\n" +
                        "Пожалуйста, пополните баланс на необходимую сумму:";

                sendOrEdit(chatId, text, keyboardFactory.getTopUpKeyboard(), messageId);
                return;
            }

            ApiResponse<UserResponse> res = userService.purchaseSubscription(chatId, planId, 1, isPromo);
            if (res != null && res.isSuccess()) {
                String successMsg = isPromo
                        ? "🎉 <b>Бесплатный период успешно активирован! Наслаждайтесь безопасным интернетом.</b>"
                        : "✅ <b>Тариф подписки успешно изменен/продлен!</b>";
                sendSimpleText(chatId, successMsg, true);
                sendProfile(chatId, null);
            } else {
                String error = (res != null) ? res.getMessage() : "Ошибка транзакции";
                sendOrEdit(chatId, "❌ Не удалось применить тариф: " + error, keyboardFactory.getBuySubKeyboard(false), messageId);
            }
        } catch (Exception e) {
            log.error("Error purchasing subscription", e);
            sendSimpleText(chatId, "❌ Ошибка системы при списании.", true);
        }
    }

    public void sendSubscriptionOptions(long chatId, Integer messageId) {
        try {
            ApiResponse<UserResponse> profileRes = userService.getMyProfile(chatId);
            UserResponse u = (profileRes != null) ? profileRes.getData() : null;

            boolean isPayg = u == null || u.getSubscriptionType() == null || "PAYG".equalsIgnoreCase(u.getSubscriptionType().name());
            boolean hasActive = u != null && u.isHasActiveSubscription();
            boolean promoAvailable = isPayg && !hasActive;

            String text = String.join("\n",
                    "🛒 <b>Доступные тарифы GeoVPN</b>",
                    "",
                    "⏱ <b>Пробный — 6 ₽ / день</b>",
                    "   1 устройство | Все локации | Проверка скорости",
                    "",
                    "📱 <b>Стандарт — 100 ₽ / мес</b>",
                    "   1 устройство | Все локации | Без ограничений трафика",
                    "",
                    "⚡️ <b>Премиум — 150 ₽ / мес</b>",
                    "   2 устройства | Все локации | Приоритетный канал связи",
                    "",
                    "👥 <b>Семья — 350 ₽ / мес</b>",
                    "   3 устройства | Все локации | Максимальная стабильность",
                    "",
                    promoAvailable
                            ? "🎁 <i>Вам доступен промо-тариф: 30 дней бесплатно! Заберите его кнопкой ниже.</i>"
                            : "💡 <i>Покупка тарифа происходит мгновенно при наличии средств на вашем балансе.</i>"
            );

            sendOrEdit(chatId, text, keyboardFactory.getBuySubKeyboard(promoAvailable), messageId);
        } catch (Exception e) {
            log.error("Error building subscription options", e);
        }
    }

    public void sendConfigs(long chatId, Integer messageId) {
        try {
            ApiResponse<UserResponse> profileRes = userService.getMyProfile(chatId);
            UserResponse u = (profileRes != null) ? profileRes.getData() : null;
            boolean hasActive = u != null && u.isHasActiveSubscription();

            ApiResponse<List<VpnConfigResponse>> res = vpnService.getMyConfigs(chatId);
            List<VpnConfigResponse> list = (res != null && res.getData() != null) ? res.getData() : List.of();

            StringBuilder text = new StringBuilder();
            String subscriptionUrl = (!list.isEmpty()) ? list.getFirst().getSubscriptionUrl() : null;

            if (!hasActive) {
                text.append("⚠️ <b>Доступ ограничен</b>\n\n<i>Для генерации вашей уникальной ссылки и импорта ключей в приложение необходима активная подписка.</i>");
            } else if (subscriptionUrl == null || subscriptionUrl.isBlank()) {
                text.append("⚙️ <b>Подключение GeoVPN</b>\n\n<i>Ваша подписка активна. Перейдите в Mini App, чтобы инициализировать ваше устройство.</i>");
            } else {
                String uuidStr = subscriptionUrl.substring(subscriptionUrl.lastIndexOf("/") + 1);
                java.util.UUID vlessUuid = java.util.UUID.fromString(uuidStr);

                String displayLink;
                try {
                    displayLink = vpnService.getEncryptedLink(vlessUuid);
                } catch (Exception e) {
                    log.warn("Failed to encrypt subscription link for bot UI, using plain fallback: {}", e.getMessage());
                    displayLink = subscriptionUrl;
                }

                text.append("⚙️ <b>Ваша подписка GeoVPN</b>\n\n")
                        .append("Ваша персональная защищенная ссылка подписки (Happ Proxy):\n\n")
                        .append("<code>").append(displayLink).append("</code>\n\n")
                        .append("👉 <b>Нажмите на ссылку выше</b>, чтобы мгновенно скопировать её в буфер обмена.\n\n")
                        .append("💡 <i>Используйте кнопку ниже для автоматического импорта ссылки в официальный клиент Happ Proxy.</i>");
            }

            sendOrEdit(chatId, text.toString(), keyboardFactory.getConfigsKeyboard(hasActive, subscriptionUrl), messageId);
        } catch (Exception e) {
            log.error("Configs rendering error", e);
        }
    }

    public void sendProfile(long chatId, Integer messageId) {
        try {
            ApiResponse<UserResponse> res = userService.getMyProfile(chatId);
            UserResponse u = (res != null) ? res.getData() : null;
            if (u == null) {
                sendOrEdit(chatId, "⚠️ Не удалось загрузить данные профиля.", null, messageId);
                return;
            }

            boolean hasActive = u.isHasActiveSubscription();
            String statusEmoji = hasActive ? "🟢" : "🔴";
            String statusText  = hasActive ? "Активна" : "Нет подписки (PAYG)";

            String expiryLine = "";
            if (hasActive && u.getSubscriptionExpiresAt() != null) {
                expiryLine = "\n📅 <b>Действует до:</b> <code>" + u.getSubscriptionExpiresAt().toLocalDate() + "</code>";
            }

            double balanceRub = (double) u.getBalance() / 100;

            String text = String.join("\n",
                    "👤 <b>Ваш личный кабинет GeoVPN</b>",
                    "",
                    "🔑 <b>Telegram ID:</b> <code>" + chatId + "</code>",
                    "💳 <b>Баланс:</b> <code>" + String.format("%.2f", balanceRub) + " ₽</code>",
                    "🛡 <b>Подписка:</b> " + statusEmoji + " <i>" + statusText + "</i>" + expiryLine,
                    "",
                    "📱 <i>Управляйте устройствами, ключами и тарифом через кнопки ниже.</i>"
            );

            sendOrEdit(chatId, text, keyboardFactory.getProfileKeyboard(hasActive, u.getBalance()), messageId);
        } catch (Exception e) {
            log.error("Profile rendering error", e);
        }
    }

    public void sendDevices(long chatId, Integer messageId) {
        try {
            ApiResponse<List<DeviceResponse>> devicesRes = userService.getMyDevices(chatId);
            List<DeviceResponse> devicesList = (devicesRes != null && devicesRes.getData() != null) ? devicesRes.getData() : List.of();
            int used = devicesList.size();

            ApiResponse<DeviceLimitStatus> limitRes = userService.getDeviceLimit(chatId);
            DeviceLimitStatus limitStatus = (limitRes != null) ? limitRes.getData() : null;
            int limit = (limitStatus != null) ? limitStatus.getMaxDevices() : 1;

            boolean limitReached = used >= limit;

            int filled = Math.min(10, (int) (((double) used / limit) * 10));
            String bar = "<code>[" + "█".repeat(filled) + "░".repeat(Math.max(0, 10 - filled)) + "]</code>";

            String hint = limitReached
                    ? "⚠️ <b>Лимит исчерпан.</b> Удалите старые сессии в Mini App или купите дополнительный слот."
                    : "🟢 <b>Свободные слоты есть!</b> Добавьте новое устройство через Mini App.";

            String text = String.join("\n",
                    "📱 <b>Менеджер устройств</b>",
                    "",
                    "Слотов использовано: <b>" + used + " / " + limit + "</b>",
                    bar,
                    "",
                    hint
            );

            sendOrEdit(chatId, text, keyboardFactory.getDevicesKeyboard(limitReached), messageId);
        } catch (Exception e) {
            log.error("Devices rendering error", e);
        }
    }

    public void sendReferralStats(long chatId, Integer messageId) {
        try {
            ApiResponse<UserResponse> res = userService.getMyProfile(chatId);
            UserResponse u = (res != null) ? res.getData() : null;
            String refCode = (u != null && u.getReferralCode() != null && !u.getReferralCode().isBlank())
                    ? u.getReferralCode()
                    : String.valueOf(chatId);

            String refLink = "https://t.me/geovpbot?start=" + refCode;

            ApiResponse<UserStatsResponse> statsRes = userService.getUserStats(chatId);
            UserStatsResponse stats = (statsRes != null) ? statsRes.getData() : null;

            long referralCount = (stats != null && stats.getTotalReferrals() != null) ? stats.getTotalReferrals() : 0L;
            int earnedKopecks = (stats != null && stats.getTotalReferralEarnings() != null) ? stats.getTotalReferralEarnings() : 0;
            double earnedRubles = earnedKopecks / 100.0;

            String text = String.join("\n",
                    "👥 <b>Реферальная программа GeoVPN</b>",
                    "",
                    "Приглашайте друзей — получайте бонусы:",
                    "• 🎁 <b>Ваш друг</b> получает 10 дней бесплатной подписки",
                    "• 💰 <b>Вы</b> получаете 50 ₽ на баланс за каждого",
                    "",
                    "━━━━━━━━━━━━━━━━━━━━━━",
                    "📊 <b>Статистика:</b>",
                    "Приглашено: <b>" + referralCount + "</b> чел.",
                    "Заработано бонусами: <b>" + String.format("%.2f", earnedRubles) + " ₽</b>",
                    "",
                    "🔗 <b>Ваша ссылка:</b>",
                    "<code>" + refLink + "</code>",
                    "",
                    "<i>Нажмите на ссылку, чтобы скопировать, или поделитесь кнопкой ниже.</i>"
            );

            sendOrEdit(chatId, text, keyboardFactory.getReferralKeyboard(chatId), messageId);
        } catch (Exception e) {
            log.error("Referral stats error", e);
            String refLink = "https://t.me/geovpbot?start=" + chatId;
            String text = "👥 <b>Реферальная программа</b>\n\n🔗 Ваша ссылка:\n<code>" + refLink + "</code>\n\n<i>Статистика временно недоступна.</i>";
            SendMessage msg = new SendMessage(String.valueOf(chatId), text);
            msg.setParseMode("HTML");
            msg.setReplyMarkup(keyboardFactory.getReferralKeyboard(chatId));
            sender.execute(msg);
        }
    }

    public void sendLeaderboard(long chatId, Integer messageId) {
        try {
            ApiResponse<List<LeaderboardEntryDto>> res = userService.getLeaderboard(chatId);
            List<LeaderboardEntryDto> entries = (res != null && res.getData() != null) ? res.getData() : List.of();
            StringBuilder text = new StringBuilder("🏆 <b>Топ рефералов GeoVPN</b>\n\n");

            String[] medals = {"🥇", "🥈", "🥉"};

            if (entries.isEmpty()) {
                text.append("<i>Список пуст. Станьте первым — пригласите друга!</i>");
            } else {
                for (int i = 0; i < Math.min(entries.size(), 10); i++) {
                    LeaderboardEntryDto e = entries.get(i);
                    String prefix = i < 3 ? medals[i] : (i + 1) + ".";

                    String winner = e.isWinner() ? " 👑" : "";

                    text.append(prefix).append(" @").append(e.getUsername())
                            .append(" — <b>").append(e.getReferralCount()).append("</b> приглашённых")
                            .append(winner).append("\n");
                }
                text.append("\n<i>Топ обновляется ежемесячно. Победитель получает приз!</i>");
            }

            sendOrEdit(chatId, text.toString(), keyboardFactory.getLeaderboardKeyboard(), messageId);
        } catch (Exception e) {
            log.error("Leaderboard rendering error", e);
        }
    }

    public void sendBuySlotConfirm(long chatId) {
        String text = String.join("\n",
                "➕ <b>Дополнительный слот устройства</b>",
                "",
                "Стоимость: <b>100 ₽</b> (спишется с баланса)",
                "Позволяет подключить ещё одно устройство к вашему аккаунту.",
                "",
                "Подтвердить покупку?"
        );
        SendMessage msg = new SendMessage(String.valueOf(chatId), text);
        msg.setParseMode("HTML");
        msg.setReplyMarkup(keyboardFactory.getConfirmSlotKeyboard());
        sender.execute(msg);
    }

    public void purchaseExtraSlot(long chatId) {
        try {
            userService.purchaseExtraSlot(chatId);
            sendSimpleText(chatId, "✅ <b>Слот куплен!</b> Теперь вы можете подключить ещё одно устройство.", true);
        } catch (Exception e) {
            log.error("Purchase slot error", e);
            sendSimpleText(chatId, "❌ Не удалось купить слот. Проверьте баланс или попробуйте позже.", true);
        }
    }

    public void sendPromoRequest(long chatId) {
        sendSimpleText(chatId, "🔑 <b>Введите промокод:</b>", false);
    }

    public void applyPromoCode(long chatId, String code) {
        try {
            userService.applyPromo(chatId, code);
            sendSimpleText(chatId, "✅ <b>Промокод применён!</b> Настройки тарифа обновлены.", true);
        } catch (Exception e) {
            sendSimpleText(chatId, "❌ <b>Ошибка.</b> Промокод недействителен или уже использован.", true);
        }
    }

    public void sendChangeRefCodeRequest(long chatId) {
        sendSimpleText(chatId, "✏️ <b>Введите новый реферальный код</b>\n\n<i>Код должен содержать только латинские буквы и цифры, от 3 до 16 символов.</i>", false);
    }

    public void updateReferralCode(long chatId, String code) {
        try {
            userService.updateReferralCode(chatId, code);
            String newLink = "https://t.me/geovpbot?start=" + code.toUpperCase();
            sendSimpleText(chatId,
                    "✅ <b>Реферальный код изменён!</b>\n\nНовая ссылка:\n<code>" + newLink + "</code>", true);
        } catch (Exception e) {
            log.error("Update referral code error", e);
            sendSimpleText(chatId, "❌ Не удалось изменить код. Возможно, он уже занят.", true);
        }
    }

    public void sendInstructions(long chatId) {
        String text = String.join("\n",
                "📖 <b>Инструкции по настройке GeoVPN</b>",
                "",
                "Поддерживаем автоимпорт ключей и ручную настройку на всех платформах:",
                "",
                "🤖 <b>Android</b> — клиент <i>Happ Proxy</i>",
                "🍎 <b>iOS / iPadOS</b> — клиент <i>Happ Plus</i>",
                "💻 <b>Windows / macOS</b> — клиент <i>Happ Desktop</i>",
                "📺 <b>Apple TV / Android TV</b> — клиент <i>Happ TV</i>",
                "",
                "👇 <i>Нажмите кнопку для пошаговых гайдов со скриншотами</i>"
        );

        SendMessage msg = new SendMessage(String.valueOf(chatId), text);
        msg.setParseMode("HTML");
        msg.setReplyMarkup(keyboardFactory.getInstructionsKeyboard());
        sender.execute(msg);
    }

    public void sendSupport(long chatId) {
        String text = String.join("\n",
                "💬 <b>Служба поддержки GeoVPN</b>",
                "",
                "Проблемы с оплатой, настройкой или соединением?",
                "Команда поддержки готова помочь.",
                "",
                "⏱ Время работы: <b>09:00 – 22:00 МСК</b>",
                "",
                "<i>Перед обращением загляните в «📖 Инструкции» — там ответы на 95% вопросов.</i>"
        );

        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder().text("💬 Написать в поддержку").url("https://t.me/geo_vpn_support").build()
                ))
                .build();

        SendMessage msg = new SendMessage(String.valueOf(chatId), text);
        msg.setParseMode("HTML");
        msg.setReplyMarkup(markup);
        sender.execute(msg);
    }

    public void sendNews(long chatId) {
        String text = String.join("\n",
                "📰 <b>Официальный канал GeoVPN</b>",
                "",
                "Подписывайтесь, чтобы первыми узнавать о:",
                "• Новых локациях и серверах",
                "• Акциях, промокодах и скидках",
                "• Техническом статусе сети"
        );

        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(InlineKeyboardButton.builder().text("📢 Перейти в канал").url("https://t.me/geo_vpn_news").build()))
                .build();

        SendMessage msg = new SendMessage(String.valueOf(chatId), text);
        msg.setParseMode("HTML");
        msg.setReplyMarkup(markup);
        sender.execute(msg);
    }

    public void sendTopUpOptions(long chatId, Integer messageId) {
        String text = String.join("\n",
                "💳 <b>Пополнение баланса</b>",
                "",
                "Выберите сумму или введите свою в поддержке:",
                "",
                "<i>После пополнения вы можете купить подписку или дополнительные слоты устройств.</i>"
        );
        sendOrEdit(chatId, text, keyboardFactory.getTopUpKeyboard(), messageId);
    }

    public void sendSimpleText(long chatId, String text, boolean withMenu) {
        SendMessage msg = new SendMessage(String.valueOf(chatId), text);
        msg.setParseMode("HTML");
        if (withMenu) msg.setReplyMarkup(keyboardFactory.getMainReplyKeyboard());
        sender.execute(msg);
    }

    private void sendOrEdit(long chatId, String text, InlineKeyboardMarkup markup, Integer messageId) {
        if (messageId != null) {
            EditMessageText edit = new EditMessageText();
            edit.setChatId(String.valueOf(chatId));
            edit.setMessageId(messageId);
            edit.setText(text);
            edit.setParseMode("HTML");
            if (markup != null) {
                edit.setReplyMarkup(markup);
            }
            sender.execute(edit);
        } else {
            SendMessage msg = new SendMessage(String.valueOf(chatId), text);
            msg.setParseMode("HTML");
            if (markup != null) {
                msg.setReplyMarkup(markup);
            }
            sender.execute(msg);
        }
    }
}