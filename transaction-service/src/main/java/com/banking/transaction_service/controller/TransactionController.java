package com.banking.transaction_service.controller;

import com.banking.transaction_service.dto.TransactionRequestDTO;
import com.banking.transaction_service.dto.TransactionResponseDTO;
import com.banking.transaction_service.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/transaction")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponseDTO> transfer(
            @Valid @RequestBody TransactionRequestDTO requestDTO
            ){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.transfer(requestDTO));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponseDTO> getTransaction(
            @PathVariable String transactionId
    ){
        return ResponseEntity.status(HttpStatus.OK)
                .body(transactionService.getTransaction(transactionId));
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<TransactionResponseDTO>> getTransactionHistory(
            @PathVariable String accountNumber
    ){
        return ResponseEntity.status(HttpStatus.OK)
                .body(transactionService.getTransactionHistory(accountNumber));
    }

    @PostMapping("/{transactionId}/verify")
    public ResponseEntity<TransactionResponseDTO> verifyOTP(
            @PathVariable String transactionId,
            @RequestParam String otp
    ){
        log.info("OTP verification request - transaction: {}", transactionId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(transactionService.verifyOTP(transactionId, otp));
    }

}
