package com.banking.account_service.service;

import com.banking.account_service.dto.AccountRequestDTO;
import com.banking.account_service.dto.AccountResponseDTO;
import com.banking.account_service.entity.Account;
import com.banking.account_service.mapper.AccountMapper;
import com.banking.account_service.repository.AccountRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private static final SecureRandom secureRandom = new SecureRandom();

    public AccountResponseDTO createAccount(@Valid AccountRequestDTO requestDTO) {
        log.info("Creating account for: {}", requestDTO.getEmail());

        if (accountRepository.existsByEmail(requestDTO.getEmail())) {
            throw new RuntimeException("Account already exists for email: " + requestDTO.getEmail());
        }

        Account account = accountMapper.toEntity(requestDTO);
        account.setAccountNumber(generateAccountNumber());

        Account savedAccount = accountRepository.save(account);

        log.info("Account created: {}", savedAccount.getAccountNumber());

        return accountMapper.toDto(savedAccount);
    }

//    Generate unique 12 digit account number
    private String generateAccountNumber(){

        String accountNumber;

        do{
           long number = secureRandom.nextLong(1_000_000_000_000L);
           accountNumber = String.format("%012d", number);
        }while(accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }
}