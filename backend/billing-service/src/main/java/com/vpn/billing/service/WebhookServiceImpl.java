package com.vpn.billing.service;

import com.vpn.billing.client.UserServiceClient;
import com.vpn.billing.domain.entity.Transaction;
import com.vpn.billing.dto.platega.PlategaWebhookDto;
import com.vpn.billing.exception.PaymentException;
import com.vpn.billing.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private final TransactionRepository transactionRepository;
    private final UserServiceClient userServiceClient;

    @Value("${platega.merchant-id}")
    private String expectedMerchantId;

    @Value("${platega.secret-key}")
    private String expectedSecret;


    @Override
    @Transactional
    public void processWebhook(String merchantId, String secret, PlategaWebhookDto dto) {
        log.info("Получен вебхук от Platega: transactionId={}, status={}", dto.getTransactionId(), dto.getStatus());

        if (!expectedMerchantId.equals(merchantId) || !expectedSecret.equals(secret)) {
            log.error("Попытка взлома или неверные ключи от Platega! MerchantId={}, IP-address=???", merchantId);
            throw new PaymentException("Invalid credentials");
        }

        Long internalTxId;
        try {
            internalTxId = Long.parseLong(dto.getPayload());
        } catch (NumberFormatException | NullPointerException e) {
            log.error("Неверный payload в вебхуке: {}", dto.getPayload());
            return;
        }

        Transaction transaction = transactionRepository.findById(internalTxId)
                .orElseThrow(() -> new PaymentException("Транзакция не найдена: " + internalTxId));

        if (!"PENDING".equals(transaction.getStatus()) && !"PROCESSING".equals(transaction.getStatus())) {
            log.warn("Повторный вебхук для транзакции {}. Текущий статус: {}", internalTxId, transaction.getStatus());
            return;
        }

        switch (dto.getStatus().toUpperCase()) {
            case "CONFIRMED":
                handleSuccessfulPayment(transaction);
                break;
            case "CANCELED":
            case "FAILED":
                transaction.setStatus("FAILED");
                transaction.setCompletedAt(LocalDateTime.now());
                log.info("Транзакция {} отклонена/отменена клиентом.", internalTxId);
                break;
            case "CHARGEBACK":
                transaction.setStatus("REFUNDED");
                transaction.setCompletedAt(LocalDateTime.now());
                log.info("Чарджбэк по транзакции {}", internalTxId);
                break;
            default:
                log.warn("Неизвестный статус от Platega: {}", dto.getStatus());
                break;
        }

        transactionRepository.save(transaction);
    }

    private void handleSuccessfulPayment(Transaction transaction) {
        transaction.setStatus("COMPLETED");
        transaction.setCompletedAt(LocalDateTime.now());

        try {
            log.info("Начисление {} копеек пользователю {}", transaction.getAmount(), transaction.getUserId());

            userServiceClient.addBalance(transaction.getUserId(), transaction.getAmount());

            log.info("Баланс успешно начислен!");
        } catch (Exception e) {

            //todo Если user-service упал, нужно продумать логику (оставить PENDING или перевести в отдельный статус ERROR)
            // В идеале тут нужен брокер сообщений (RabbitMQ/Kafka) или Quartz для retry-запросов

            log.error("ОШИБКА НАЧИСЛЕНИЯ БАЛАНСА! Транзакция {}, Юзер {}", transaction.getId(), transaction.getUserId(), e);
            throw new RuntimeException("Failed to add balance", e);
        }
    }
}
