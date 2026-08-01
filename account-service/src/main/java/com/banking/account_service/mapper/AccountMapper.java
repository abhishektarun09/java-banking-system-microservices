package com.banking.account_service.mapper;

import com.banking.account_service.dto.AccountRequestDTO;
import com.banking.account_service.dto.AccountResponseDTO;
import com.banking.account_service.entity.Account;
import com.banking.account_service.entity.AccountStatus;
import com.banking.account_service.entity.AccountType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AccountMapper {

    public AccountResponseDTO toDto(Account account) {
        if (account == null) {
            return null;
        }

        AccountResponseDTO responseDTO = new AccountResponseDTO();
        responseDTO.setAccountNumber(account.getAccountNumber());
        responseDTO.setAccountHolderName(account.getAccountHolderName());
        responseDTO.setEmail(account.getEmail());
        responseDTO.setPhone(account.getPhone());
        responseDTO.setAccountType(account.getAccountType());
        responseDTO.setAccountStatus(account.getAccountStatus());
        responseDTO.setBalance(account.getBalance());
        responseDTO.setDailyTransactionLimit(account.getDailyTransactionLimit());
        responseDTO.setCreatedAt(account.getCreatedAt());

        return responseDTO;
    }

    public Account toEntity(AccountRequestDTO requestDTO) {
        if (requestDTO == null) {
            return null;
        }

        return Account.builder()
                .accountHolderName(requestDTO.getAccountHolderName())
                .email(requestDTO.getEmail())
                .phone(requestDTO.getPhone())
                .accountType(requestDTO.getAccountType())
                .balance(requestDTO.getInitialDeposit())
                .accountStatus(AccountStatus.ACTIVE)
                .dailyTransactionLimit(calculateDailyLimit(requestDTO.getAccountType()))
                .build();
    }

    private BigDecimal calculateDailyLimit(AccountType accountType) {
        if (accountType == null) {
            return null;
        }
        return accountType == AccountType.SAVINGS
                ? new BigDecimal("1000")
                : new BigDecimal("5000");
    }
}