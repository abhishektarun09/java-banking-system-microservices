package com.banking.payment_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequestDTO {

    @NotBlank(message = "Account Number is required")
    private String accountNumber;

    @NotNull(message = "Amount is required")
    @Positive(message = "Invalid amount")
    private BigDecimal amount;

    private String description;

}
