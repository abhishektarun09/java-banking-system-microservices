package com.banking.transaction_service.dto;

import com.banking.transaction_service.entiity.TransactionStatus;
import com.banking.transaction_service.entiity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponseDTO {

    private String id;
    private String senderAccountNumber;
    private String receiverAccountNumber;
    private BigDecimal amount;
    private TransactionType transactionType;
    private TransactionStatus transactionStatus;
    private String description;
    private String referenceNumber;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
