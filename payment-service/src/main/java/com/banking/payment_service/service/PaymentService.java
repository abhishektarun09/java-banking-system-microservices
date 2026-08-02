package com.banking.payment_service.service;

import com.banking.payment_service.dto.CreatePaymentRequestDTO;
import com.banking.payment_service.dto.PaymentOrderResponseDTO;
import com.banking.payment_service.entity.PaymentEntity;
import com.banking.payment_service.entity.PaymentStatus;
import com.banking.payment_service.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;

    private static final String PAYMENT_FAILED_TOPIC = "payment.failed";
    private static final String PAYMENT_COMPLETED_TOPIC = "payment.completed";

    @Value("${razorpay.key}")
    private String razorpayKey;

    @Value("${razorpay.secret}")
    private String getRazorpaySecret;

    // create order in razorpay -> save payment -> return order details to frontend -> frontend show razorpay checkout page ->
    // user pays -> razorpay calls webhook
    public PaymentOrderResponseDTO createPaymentOrder(@Valid CreatePaymentRequestDTO requestDTO) throws RazorpayException {

        log.info("Creating order for account: {} amount: {}", requestDTO.getAccountNumber(), requestDTO.getAmount());

        RazorpayClient razorpayClient = new RazorpayClient(razorpayKey, getRazorpaySecret);

        //convert amount
        int convertedAmount = requestDTO.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .intValue();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", convertedAmount);
        orderRequest.put("currency", "USD");
        orderRequest.put("receipt", "rcpt_"+ UUID.randomUUID().toString()
                .replace("-","").substring(0,10)
                +System.currentTimeMillis());

        Order razorpayOrder = razorpayClient.orders.create(orderRequest);

        log.info("Razorpay order created: {}", razorpayOrder.get("id").toString());

        //save payment record
        PaymentEntity paymentEntity = new PaymentEntity();
        paymentEntity.setRazorpayOrderId(razorpayOrder.get("id").toString());
        paymentEntity.setAccountNumber(requestDTO.getAccountNumber());
        paymentEntity.setAmount(requestDTO.getAmount());
        paymentEntity.setCurrency("USD");
        paymentEntity.setStatus(PaymentStatus.CREATED);
        paymentEntity.setDescription(requestDTO.getDescription());

        PaymentEntity savedPayment = paymentRepository.save(paymentEntity);

        return new PaymentOrderResponseDTO(
                savedPayment.getId(),
                razorpayOrder.get("id").toString(),
                requestDTO.getAmount(),
                "USD",
                "CREATED",
                razorpayKey
        );
    }
}
