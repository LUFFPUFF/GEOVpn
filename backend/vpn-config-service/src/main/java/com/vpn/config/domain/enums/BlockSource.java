package com.vpn.config.domain.enums;

public enum BlockSource {
    ROSKOMNADZOR,   // Официальный дамп
    USER_REPORT,    // Пользователи жалуются
    SYSTEM,         // Наша внутренняя эвристика
    MANUAL_ADMIN    // Добавлено админом вручную
}
