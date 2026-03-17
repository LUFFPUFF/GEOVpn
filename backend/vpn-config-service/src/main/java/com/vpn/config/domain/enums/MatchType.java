package com.vpn.config.domain.enums;

public enum MatchType {
    EXACT,      // Строгое совпадение (например, "rutracker.org")
    WILDCARD,   // Все поддомены (например, "*.instagram.com")
    REGEX       // Сложное регулярное выражение (для специфичных правил)
}

