package com.banking.transaction_service.repository;

import com.banking.transaction_service.entiity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {

    List<TransactionEntity> findBySenderAccountNumberOrderByCreatedAtDesc(String accountNumber);
}
