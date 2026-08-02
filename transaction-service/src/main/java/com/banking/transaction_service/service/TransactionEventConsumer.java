package com.banking.transaction_service.service;

import com.banking.transaction_service.entiity.TransactionEntity;
import com.banking.transaction_service.entiity.TransactionStatus;
import com.banking.transaction_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final long OTP_EXPIRY_MINUTES = 5;
    private static final String TRANSACTION_OTP_GENERATED_TOPIC = "transaction.otp.generated";

    //consume verification.required
    //generate otp and ask to verify
    @KafkaListener(topics = "verification.required")
    public void consumeVerificationRequired(
            @Payload Map<String, Object> payload
            ){

        try{
            String transactionId = payload.get("transactionId").toString();
            String accountNumber = payload.get("accountNumber").toString();
            String reason = payload.get("reason").toString();

            log.info("Verification required - transaction: {} reason: {}",
                    transactionId, reason);

            TransactionEntity transactionEntity = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));

            if(transactionEntity.getTransactionStatus() != TransactionStatus.PROCESSING){
                log.warn("Transaction {} not currently Processing - skipping verification", transactionId);
                return;
            }

            // Generate OTP
            String otp = String.format("%06d", (int) (Math.random() * 900000) + 100000);

            // Store otp in redis
            String otpKey = "verification:otp:" + transactionId;
            redisTemplate.opsForValue().set(otpKey, otp, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);

            // Update status
            transactionEntity.setTransactionStatus(TransactionStatus.PENDING_VERIFICATION);
            transactionRepository.save(transactionEntity);

            log.info("OTP generated for transaction: {}", transactionId);

            // Notify user - publish event
            Map<String, Object> otpEvent = new HashMap<>();
            otpEvent.put("transactionId", transactionId);
            otpEvent.put("accountNumber", accountNumber);
            otpEvent.put("reason", reason);
            otpEvent.put("otp", otp);
            otpEvent.put("amount", payload.get("amount"));

            kafkaTemplate.send(TRANSACTION_OTP_GENERATED_TOPIC, transactionId, otpEvent);

        } catch (Exception e) {
            log.error("Error handling verification required {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "fraud.check.clean")
    public void consumeFraudCheckCleanResult(
            @Payload Map<String, Object> payload
    ){
        try{

            String transactionId = payload.get("transaction_id").toString();
            transactionService.processCleanResult(transactionId);

        }
        catch (Exception e){
            log.error("Error processing fraud check result: {}", e.getMessage());

        }
    }
}
