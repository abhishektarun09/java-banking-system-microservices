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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
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

    private static final String TRANSACTION_INITIATED_TOPIC = "transaction.initiated";
    private static final String TRANSACTION_COMPLETED_TOPIC = "transaction.completed";
    private static final String TRANSACTION_REFUNDED_TOPIC = "transaction.refunded";

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
}