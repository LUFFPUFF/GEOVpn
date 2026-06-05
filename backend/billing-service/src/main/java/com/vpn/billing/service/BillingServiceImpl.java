package com.vpn.billing.service;

import com.vpn.billing.client.PlategaClient;
import com.vpn.billing.domain.entity.Transaction;
import com.vpn.billing.dto.platega.PlategaCreateRequest;
import com.vpn.billing.dto.platega.PlategaCreateResponse;
import com.vpn.common.dto.request.DepositRequest;
import com.vpn.common.dto.response.DepositResponse;
import com.vpn.billing.dto.response.RevenueStatDto;
import com.vpn.billing.exception.PaymentException;
import com.vpn.billing.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService{

    private final TransactionRepository transactionRepository;
    private final PlategaClient plategaClient;

    @Value("${platega.merchant-id}")
    private String merchantId;

    @Value("${platega.secret-key}")
    private String secretKey;

    @Value("${platega.return-url}")
    private String returnUrl;

    @Override
    public DepositResponse createDeposit(Long telegramId, DepositRequest request) {
        log.info("Создание заявки на пополнение: telegramId={}, amount={} RUB", telegramId, request.getAmount());

        Transaction transaction = Transaction.builder()
                .userId(telegramId)
                .amount(request.getAmount() * 100)
                .transactionType("DEPOSIT")
                .description("Пополнение баланса на " + request.getAmount() + " RUB")
                .metadata(new HashMap<>())
                .build();

        transaction = transactionRepository.save(transaction);
        log.debug("Сохранена транзакция PENDING с ID: {}", transaction.getId());

        Integer method = request.getPaymentMethod() != null ? request.getPaymentMethod() : 2;

        PlategaCreateRequest plategaRequest = PlategaCreateRequest.builder()
                .paymentMethod(method)
                .paymentDetails(PlategaCreateRequest.PaymentDetails.builder()
                        .amount(request.getAmount().doubleValue())
                        .currency("RUB")
                        .build())
                .description(transaction.getDescription())
                .returnUrl(returnUrl)
                .failedUrl(returnUrl)
                .payload(transaction.getId().toString())
                .build();

        try {
            PlategaCreateResponse plategaResponse;

            if (request.getPaymentMethod() != null) {
                plategaResponse = plategaClient.createTransaction(merchantId, secretKey, plategaRequest);
            } else {
                plategaResponse = plategaClient.createTransactionV2(merchantId, secretKey, plategaRequest);
            }

            log.info("Полный ответ от Platega: {}", plategaResponse);

            String paymentUrl = plategaResponse != null ? plategaResponse.getFinalUrl() : null;

            if (paymentUrl == null) {
                log.error("Platega не вернула ссылку ни в одном из полей (redirect/url)!");
                throw new PaymentException("Шлюз не вернул ссылку на оплату.");
            }

            transaction.setExternalTransactionId(plategaResponse.getTransactionId());
            transaction.setStatus("PENDING");
            transactionRepository.save(transaction);

            log.info("Платеж успешно создан. URL: {}", paymentUrl);

            return DepositResponse.builder()
                    .transactionId(transaction.getId())
                    .paymentUrl(paymentUrl)
                    .build();

        } catch (Exception e) {
            log.error("Ошибка при обращении к API Platega: {}", e.getMessage(), e);
            transaction.setStatus("FAILED");
            transaction.setDescription("Ошибка при создании платежа на стороне шлюза");
            transactionRepository.save(transaction);

            throw new PaymentException("Не удалось создать платеж. Попробуйте позже.");
        }
    }

    @Override
    public List<RevenueStatDto> getRevenueStats(int days) {
        log.info("Сбор статистики доходов за последние {} дней", days);

        LocalDateTime startDate = LocalDate.now().minusDays(days - 1).atStartOfDay();
        List<Transaction> transactions = transactionRepository.findAllCompletedDepositsAfter(startDate);

        Map<String, Long> aggregated = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCreatedAt().toLocalDate().toString(),
                        Collectors.summingLong(t -> t.getAmount() / 100)
                ));

        List<RevenueStatDto> result = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            String dateStr = LocalDate.now().minusDays(i).toString();
            result.add(new RevenueStatDto(dateStr, aggregated.getOrDefault(dateStr, 0L)));
        }

        return result;
    }

    @Override
    public List<Transaction> getUserTransactions(Long userId) {
        log.info("Получение истории транзакций для пользователя: {}", userId);
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
