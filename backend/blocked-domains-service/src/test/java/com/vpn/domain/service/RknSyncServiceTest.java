package com.vpn.domain.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "service.vpn.rkn.api-url=https://reestr.rublacklist.net/api/v3/domains/",
        "service.vpn.rkn.batch-size=2000"
})
class RknSyncServiceTest {

    @Autowired
    private RknSyncService rknSyncService;

    @Autowired
    private BlockedDomainRepository domainRepository;

    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";

    @Test
    void testRealRknSync() {
        System.out.println(BLUE + "=========================================================" + RESET);
        System.out.println(BLUE + "ЗАПУСК ТЕСТА СИНХРОНИЗАЦИИ С РОСКОМСВОБОДОЙ" + RESET);
        System.out.println(BLUE + "=========================================================" + RESET);

        long countBefore = domainRepository.count();
        System.out.println(YELLOW + "Доменов в БД ДО синхронизации: " + countBefore + RESET);

        System.out.println(YELLOW + "Вызываем rknSyncService.syncBlockedDomains()..." + RESET);
        long startTime = System.currentTimeMillis();

        int addedCount = rknSyncService.syncBlockedDomains();

        long duration = System.currentTimeMillis() - startTime;
        System.out.println(GREEN + "Синхронизация завершена за " + duration + " мс!" + RESET);
        System.out.println(GREEN + "Добавлено новых доменов: " + addedCount + RESET);

        long countAfter = domainRepository.count();
        System.out.println(YELLOW + "Доменов в БД ПОСЛЕ синхронизации: " + countAfter + RESET);

        assertTrue(countAfter > 100000, "В базе должно быть больше 100 000 заблокированных доменов!");

        System.out.println(BLUE + "\n=========================================================" + RESET);
        System.out.println(BLUE + "ЗАПУСК ПОВТОРНОЙ СИНХРОНИЗАЦИИ (ПРОВЕРКА ДЕЛЬТЫ)" + RESET);
        System.out.println(BLUE + "=========================================================" + RESET);

        int addedCountSecond = rknSyncService.syncBlockedDomains();

        System.out.println(GREEN + "Вторая синхронизация добавила: " + addedCountSecond + " доменов" + RESET);

        assertEquals(0, addedCountSecond, "Дельта не работает! Во второй раз не должно было добавиться ничего.");
    }
}