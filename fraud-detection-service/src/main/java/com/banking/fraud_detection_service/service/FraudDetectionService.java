package com.banking.fraud_detection_service.service;

import com.banking.fraud_detection_service.client.AccountServiceClient;
import com.banking.fraud_detection_service.model.FraudCheckResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionService {

    private final AccountServiceClient accountServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${fraud.max-transactions-per-minute}")
    private int maxTransactionsPerMinute;

    private static final String VERIFICATION_REQUIRED_TOPIC = "verification.required";
    private static final String FRAUD_CHECK_CLEAN_EVENT_TOPIC = "fraud.check.clean";

    public void checkTransaction(Map<String, Object> payload) {
        String transactionId = payload.get("transactionId").toString();
        String accountNumber = payload.get("senderAccountNumber").toString();
        BigDecimal amount = new BigDecimal(payload.get("amount").toString());

        // Fetch real balance from account service
        BigDecimal senderBalance = accountServiceClient.getBalance(accountNumber);

        log.info("Checking transaction: {} account {} amount {} balance {}",
                transactionId, accountNumber, amount, senderBalance);

        FraudCheckResult result = performFraudCheck(accountNumber, amount, senderBalance);

        if(result.isFraud()){
            log.info("Suspicious activity detected - account: {}"+
                    "reason: {} - requesting OTP verification",
                    accountNumber, result.getReason());

            Map<String, Object> verificationEvent = new HashMap<>();
            verificationEvent.put("transactionId", transactionId);
            verificationEvent.put("accountNumber", accountNumber);
            verificationEvent.put("amount", amount);
            verificationEvent.put("reason", result.getReason());

            kafkaTemplate.send(VERIFICATION_REQUIRED_TOPIC, transactionId, verificationEvent);
        }
        else{
            log.info("transaction Clean");

            Map<String, Object> transactionCleanEvent = new HashMap<>();
            transactionCleanEvent.put("transaction_id", transactionId);
            transactionCleanEvent.put("isFraud", false);
            transactionCleanEvent.put("reason", null);

            kafkaTemplate.send(FRAUD_CHECK_CLEAN_EVENT_TOPIC, transactionId, transactionCleanEvent);
        }
    }

    private FraudCheckResult performFraudCheck(String accountNumber, BigDecimal amount, BigDecimal senderBalance) {

        //3 Checks

        //1. Velocity check
        if(isVelocityExceeded(accountNumber)){
            return new FraudCheckResult(true, "Velocity check failed");
        }

        //2. Amount check
        if(isAmountSuspicious(accountNumber)){
            return new FraudCheckResult(true, "Amount check failed. Exceeds 3x average");
        }

        //3. balance Check
        if(senderBalance.compareTo(BigDecimal.ZERO) > 0
        && isBalanceCheckFailed(senderBalance, amount)){
            return new FraudCheckResult(true, "Balance check failed");
        }

        return new FraudCheckResult(false, null);
    }

    private boolean isVelocityExceeded(String accountNumber) {
        String key = "fraud:velocity:" + accountNumber;
        Long count = redisTemplate.opsForValue().increment(key);

        if(count != null && count == 1){
            redisTemplate.expire(key,60, TimeUnit.SECONDS);
        }

        log.info("Velocity check - account: {} count: {}/{}",
                accountNumber, count, maxTransactionsPerMinute);

        return count != null && count > maxTransactionsPerMinute;
    }
}
