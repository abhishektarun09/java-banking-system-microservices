package com.banking.account_service.service;

import com.banking.account_service.dto.AccountRequestDTO;
import com.banking.account_service.dto.AccountResponseDTO;
import com.banking.account_service.entity.Account;
import com.banking.account_service.entity.AccountStatus;
import com.banking.account_service.mapper.AccountMapper;
import com.banking.account_service.repository.AccountRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    public AccountResponseDTO getAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return accountMapper.toDto(account);
    }

    public BigDecimal getBalance(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return account.getBalance();
    }

    public void blockAccount(String accountNumber) {

        log.info("Blocking account: {}", accountNumber);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setAccountStatus(AccountStatus.BLOCKED);
        accountRepository.save(account);

        log.info("Account blocked: {}", accountNumber);
    }

    public void deductBalance(String accountNumber, BigDecimal amount) {

        log.info("Deducting balance from account: {}", accountNumber);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if(account.getAccountStatus() != AccountStatus.ACTIVE){
            throw new RuntimeException("Account is NOT active "+accountNumber);
        }

        if(account.getBalance().compareTo(amount) < 0){
            throw new RuntimeException("Insufficient balance in account: "+accountNumber);
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        log.info("Deducted balance from account: {}", accountNumber);

    }
}