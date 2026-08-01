package com.banking.account_service.controller;

import com.banking.account_service.dto.AccountRequestDTO;
import com.banking.account_service.dto.AccountResponseDTO;
import com.banking.account_service.service.AccountService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/account")
@Slf4j
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping()
    public ResponseEntity<AccountResponseDTO> createAccount(@Valid @RequestBody AccountRequestDTO requestDTO){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(requestDTO));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponseDTO> getAccount(
            @PathVariable String accountNumber){

        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getBalance(
            @PathVariable String accountNumber){

        return ResponseEntity.ok(accountService.getBalance(accountNumber));
    }

    @PutMapping("/{accountNumber}/block")
    public ResponseEntity<String> blockAccount(
            @PathVariable String accountNumber){

        accountService.blockAccount(accountNumber);

        return ResponseEntity.ok("Account blocked successfully");
    }

//    SAGA Step 1 - Deduct Balance
//    Called by Transaction Service when transfer is initiated

    @PutMapping("/{accountNumber}/deduct")
    public ResponseEntity<String> deductBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount
    ){
        accountService.deductBalance(accountNumber, amount);
        return ResponseEntity.ok("Balance deducted successfully");
    }
//
////    SAGA STEP 4 - Compensating transaction endpoint
////    Called by Transaction service if:
////    1. Fraud detected - refund sender (undo step 1)
////    2. Transaction Completed - credit receiver
//
//    @PutMapping("/{accountNumber}/credit")
//    public ResponseEntity<String> creditBalance(
//            @PathVariable String accountNumber,
//            @RequestParam BigDecimal amount
//    ){
//        accountService.creditBalance(accountNumber, amount);
//        return ResponseEntity.ok("BALANCE CREDITED SUCCESSFULLY");
//    }

}