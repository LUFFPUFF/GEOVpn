package com.vpn.bot.service;

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

    @Value("${telegram.bot.channel-id}")
    private String channelId;

    private final List<String> ALLOWED_STATUSES = List.of("member", "administrator", "creator");

    public boolean isSubscribed(AbsSender sender, long userId) {
        try {
            GetChatMember getChatMember = new GetChatMember(channelId, userId);
            ChatMember member = sender.execute(getChatMember);
            return ALLOWED_STATUSES.contains(member.getStatus());
        } catch (Exception e) {
            log.error("Error checking subscription for user {}: {}", userId, e.getMessage());
            return false;
        }
    }
}
