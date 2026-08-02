package com.banking.notification_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    @KafkaListener(topics = "transaction.otp.generated")
    public void consumeOtpGenerated(
            @Payload Map<String, Object> payload
    ){
        try{
            String accountNumber = payload.get("accountNumber").toString();
            String otp = payload.get("otp").toString();
            String transactionId = payload.get("transactionId").toString();
            String amount = payload.get("amount").toString();
            String reason = payload.get("reason").toString();

            sendAlert(
                    accountNumber,
                    "Transaction Verification Required",
                    String.format(
                            "Suspicious activity detected on your account. "+
                                    "Reason: %s "+
                                    "Value: %s "+
                                    "OTP: %s ", reason, amount, otp
                    )
            );

        }
        catch (Exception e){
            log.error("Error sending OTP notification: {}", e.getMessage());
        }
    }

    private void sendAlert(String accountNumber, String subject, String message) {


    }

}
