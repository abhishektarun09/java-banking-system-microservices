package com.banking.account_service.repository;

import com.banking.account_service.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {

    boolean existsByEmail(String email);

    boolean existsByAccountNumber(String accountNumber);
}
