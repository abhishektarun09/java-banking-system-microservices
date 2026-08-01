package com.banking.transaction_service.mapper;

import com.banking.transaction_service.dto.TransactionResponseDTO;
import com.banking.transaction_service.entiity.TransactionEntity;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponseDTO toDto(TransactionEntity transaction) {
        if (transaction == null) {
            return null;
        }

        TransactionResponseDTO responseDTO = new TransactionResponseDTO();

        responseDTO.setSenderAccountNumber(transaction.getSenderAccountNumber());
        responseDTO.setReceiverAccountNumber(transaction.getReceiverAccountNumber());
        responseDTO.setAmount(transaction.getAmount());
        responseDTO.setId(transaction.getId());
        responseDTO.setFailureReason(transaction.getFailureReason());
        responseDTO.setCreatedAt(transaction.getCreatedAt());
        responseDTO.setCompletedAt(transaction.getCompletedAt());
        responseDTO.setTransactionType(transaction.getTransactionType());
        responseDTO.setTransactionStatus(transaction.getTransactionStatus());
        responseDTO.setDescription(transaction.getDescription());
        responseDTO.setReferenceNumber(transaction.getReferenceNumber());

        return responseDTO;
    }
}
