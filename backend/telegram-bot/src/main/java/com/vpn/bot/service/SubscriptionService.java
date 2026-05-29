package com.vpn.bot.service;

import com.vpn.bot.client.UserServiceClient;
import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.response.UserResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    @Getter
    @Value("${telegram.bot.channel-id}")
    private String channelId;

    private final UserServiceClient userServiceClient;
    private final List<String> ALLOWED_STATUSES = List.of("member", "administrator", "creator");

    public boolean isSubscribed(AbsSender sender, long userId) {
        try {
            ApiResponse<UserResponse> response = userServiceClient.getMyProfile(userId);
            if (response != null && response.getData() != null) {
                Boolean isMember = response.getData().getIsChannelMember();
                if (isMember != null) {
                    return isMember;
                }
            }
        } catch (Exception e) {
            log.warn("User {} not found in DB or DB is down. Falling back to Telegram API check.", userId);
        }

        log.info("Checking subscription via Telegram API for user {} (database fallback)", userId);
        boolean apiCheckedStatus = checkViaTelegramApi(sender, userId);

        try {
            userServiceClient.updateMembership(userId, apiCheckedStatus);
        } catch (Exception e) {
            log.error("Failed to save membership status to DB for user {}", userId, e);
        }

        return apiCheckedStatus;
    }

    private boolean checkViaTelegramApi(AbsSender sender, long userId) {
        try {
            GetChatMember getChatMember = new GetChatMember(channelId, userId);
            ChatMember member = sender.execute(getChatMember);
            return ALLOWED_STATUSES.contains(member.getStatus());
        } catch (Exception e) {
            log.error("Error checking subscription in TG API for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    public void updateMembershipInDb(long userId, boolean isMember) {
        try {
            userServiceClient.updateMembership(userId, isMember);
        } catch (Exception e) {
            log.error("Failed to update membership in DB on bot event for user {}", userId, e);
        }
    }

}
