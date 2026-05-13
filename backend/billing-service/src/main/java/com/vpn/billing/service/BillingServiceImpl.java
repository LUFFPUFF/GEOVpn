package com.vpn.billing.service;

import com.vpn.billing.client.PlategaClient;
import com.vpn.billing.domain.entity.Transaction;
import com.vpn.billing.dto.platega.PlategaCreateRequest;
import com.vpn.billing.dto.platega.PlategaCreateResponse;
import com.vpn.billing.dto.request.DepositRequest;
import com.vpn.billing.dto.response.DepositResponse;
import com.vpn.billing.exception.PaymentException;
import com.vpn.billing.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;

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
}
