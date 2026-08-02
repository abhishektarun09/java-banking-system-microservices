package com.banking.transaction_service.service;

import com.banking.transaction_service.client.AccountServiceClient;
import com.banking.transaction_service.dto.TransactionRequestDTO;
import com.banking.transaction_service.dto.TransactionResponseDTO;
import com.banking.transaction_service.entiity.TransactionEntity;
import com.banking.transaction_service.entiity.TransactionStatus;
import com.banking.transaction_service.entiity.TransactionType;
import com.banking.transaction_service.event.TransactionInitiatedEvent;
import com.banking.transaction_service.mapper.TransactionMapper;
import com.banking.transaction_service.repository.TransactionRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final TransactionMapper transactionMapper;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String TRANSACTION_INITIATED_TOPIC = "transaction.initiated";
    private static final String TRANSACTION_COMPLETED_TOPIC = "transaction.completed";
    private static final String TRANSACTION_REFUNDED_TOPIC = "transaction.refunded";
    private static final String FRAUD_DETECTED_TOPIC = "fraud.detected";

    /*
    Initiate transfer
    deduct from sender
    save transaction as PROCESSING
    publish event to kafka for fraud check
     */
    public TransactionResponseDTO transfer(@Valid TransactionRequestDTO requestDTO){

        log.info("SAGA START - Transfer: {} -> {} amount: {}",
                requestDTO.getSenderAccountNumber(),
                requestDTO.getReceiverAccountNumber(),
                requestDTO.getAmount());

//        Deduct from sender
        accountServiceClient.deductBalance(
                requestDTO.getSenderAccountNumber(),
                requestDTO.getAmount()
        );

//      save transaction as PROCESSING

        TransactionEntity transactionEntity = new TransactionEntity();
        transactionEntity.setSenderAccountNumber(requestDTO.getSenderAccountNumber());
        transactionEntity.setReceiverAccountNumber(requestDTO.getReceiverAccountNumber());
        transactionEntity.setAmount(requestDTO.getAmount());
        transactionEntity.setTransactionType(TransactionType.TRANSFER);
        transactionEntity.setTransactionStatus(TransactionStatus.PROCESSING);
        transactionEntity.setDescription(requestDTO.getDescription());
        transactionEntity.setReferenceNumber(UUID.randomUUID().toString());

        TransactionEntity savedTransaction = transactionRepository.save(transactionEntity);
        log.info("Transaction saved as Processing: {}", savedTransaction.getId());

//        publish event to kafka

        TransactionInitiatedEvent event = new TransactionInitiatedEvent(
                savedTransaction.getId(),
                savedTransaction.getSenderAccountNumber(),
                savedTransaction.getReceiverAccountNumber(),
                savedTransaction.getAmount(),
                savedTransaction.getDescription()
        );

        kafkaTemplate.send(TRANSACTION_INITIATED_TOPIC, savedTransaction.getId(), event);
        log.info("TransactionInitiatedEvent published");

        return transactionMapper.toDto(savedTransaction);
    }

    public TransactionResponseDTO getTransaction(String transactionId) {

        return transactionMapper.toDto(transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: "+transactionId)));
    }

    public List<TransactionResponseDTO> getTransactionHistory(String accountNumber) {

        return transactionRepository.findBySenderAccountNumberOrderByCreatedAtDesc(accountNumber)
                .stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
    }

    public TransactionResponseDTO verifyOTP(String transactionId, String otp) {
        log.info("OTP verification for the transaction: {}", transactionId);

        TransactionEntity transactionEntity = transactionRepository.findById(transactionId)
                .orElseThrow(()->new RuntimeException("Transaction not found "+ transactionId));

        String otpKey = "verification:otp:"+transactionId;
        String storedOtp = redisTemplate.opsForValue().get(otpKey);

        if(storedOtp == null){
            //otp expired
            log.warn("OTP expired for transaction: {}", transactionId);
            refundTransaction(transactionEntity);
            return transactionMapper.toDto(transactionEntity);
        }

        if(!storedOtp.equals(otp)){
            //BLOCK account and refund
            log.warn("Wrong OTP - blocking account and refunding: {}", transactionId);
            redisTemplate.delete(otpKey);
            blockAccountAndRefund(transactionEntity);
            return transactionMapper.toDto(transactionEntity);
        }

        log.info("OTP verified - completing transaction: {}", transactionId);
        redisTemplate.delete(otpKey);
        completeTransaction(transactionEntity);
        return transactionMapper.toDto(transactionEntity);
    }

    private void completeTransaction(TransactionEntity transactionEntity) {

        transactionEntity.setTransactionStatus(TransactionStatus.COMPLETED);
        transactionEntity.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(transactionEntity);

        //Publish completed event
        Map<String, Object> transactionCompletedEvent = new HashMap<>();
        transactionCompletedEvent.put("transactionId", transactionEntity.getId());
        transactionCompletedEvent.put("senderAccountNumber", transactionEntity.getSenderAccountNumber());
        transactionCompletedEvent.put("receiverAccountNumber", transactionEntity.getReceiverAccountNumber());
        transactionCompletedEvent.put("amount", transactionEntity.getAmount());
        transactionCompletedEvent.put("description", transactionEntity.getDescription());

        kafkaTemplate.send(TRANSACTION_COMPLETED_TOPIC, transactionEntity.getId(), transactionCompletedEvent);

        log.info("SAGA Completed - Transaction {} completed", transactionEntity.getId());

    }

    private void blockAccountAndRefund(TransactionEntity transactionEntity) {

        //Publish fraud detected event - account service will block account
        Map<String, Object> fraudEvent = new HashMap<>();
        fraudEvent.put("transactionId", transactionEntity.getId());
        fraudEvent.put("accountNumber", transactionEntity.getSenderAccountNumber());
        fraudEvent.put("reason", transactionEntity.getFailureReason());

        kafkaTemplate.send(FRAUD_DETECTED_TOPIC, transactionEntity.getSenderAccountNumber(), fraudEvent);

        log.warn("fraud.detected event published, account: {}", transactionEntity.getSenderAccountNumber());

        // SAGA Compensation and refund sender
        refundTransaction(transactionEntity);
    }

    private void refundTransaction(TransactionEntity transactionEntity) {
        log.warn("SAGA Compensation - refunding: {} amount: {}",
                transactionEntity.getSenderAccountNumber(),
                transactionEntity.getAmount());

        accountServiceClient.creditBalance(
                transactionEntity.getSenderAccountNumber(),
                transactionEntity.getAmount()
        );

        transactionEntity.setTransactionStatus(TransactionStatus.FLAGGED);
        transactionEntity.setFailureReason("SAGA compensation executed; amount refunded at "+ LocalDateTime.now());

        transactionRepository.save(transactionEntity);

        //Publish refund event
        Map<String, Object> refundEvent = new HashMap<>();
        refundEvent.put("transactionId", transactionEntity.getId());
        refundEvent.put("senderAccountNumber", transactionEntity.getSenderAccountNumber());
        refundEvent.put("amount", transactionEntity.getAmount());
        refundEvent.put("reason", transactionEntity.getFailureReason());

        kafkaTemplate.send(TRANSACTION_REFUNDED_TOPIC, transactionEntity.getId(), refundEvent);

        log.info("SAGA compensation completed - refunded to {}", transactionEntity.getSenderAccountNumber());
    }
}