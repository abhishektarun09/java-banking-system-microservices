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

    @KafkaListener(topics = "transaction.completed")
    public void consumeTransactionCompleted(
            @Payload Map<String, Object> payload
    )
    {
        try{
            String senderAccount = payload.get("senderAccountNumber").toString();
            String receiverAccount = payload.get("receiverAccountNumber").toString();
            String amount = payload.get("amount").toString();

            //debit alert
            sendAlert(senderAccount,
                    "DEBIT ALERT",
                    String.format("%s debited from account %s", amount, senderAccount)
                    );

            //credit alert
            sendAlert(receiverAccount,
                    "CREDIT ALERT",
                    String.format("%s credit into account %s", amount, receiverAccount)
                    );
        }
        catch(Exception e){
            log.error("Error sending transaction alerts: {}", e.getMessage());

    }
    }

    @KafkaListener(topics = "fraud.detected")
    public void consumeFraudDetected(
            @Payload Map<String, Object> payload
    )
    {
        try{
            String accountNumber = payload.get("accountNumber").toString();
            String reason = payload.get("reason").toString();

            //fraud alert
            sendAlert(accountNumber,
                    "Suspicious activity detected",
                    String.format("Your account %s blocked. Reason: %s", accountNumber, reason)
            );
        }
        catch(Exception e){
            log.error("Error sending fraud alert: {}", e.getMessage());

        }

    }

    @KafkaListener(topics = "transaction.refunded")
    public void consumeTransactionRefunded(
            @Payload Map<String, Object> payload
    )
    {
        try{
            String accountNumber = payload.get("senderAccountNumber").toString();
            String reason = payload.get("reason").toString();
            String amount = payload.get("amount").toString();

            //fraud alert
            sendAlert(accountNumber,
                    "REFUND PROCESSED",
                    String.format("Your transaction from account number %s was unsuccessful. Refunded amount: %s. Reason: %s",
                            accountNumber, amount, reason)
            );
        }
        catch(Exception e){
            log.error("Error sending refund alert: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "payment.completed")
    public void consumePaymentCompleted(
            @Payload Map<String, Object> payload
    )
    {
        try{
            String accountNumber = payload.get("accountNumber").toString();
            String amount = payload.get("amount").toString();

            //sender alert
            sendAlert(accountNumber,
                    "PAYMENT SUCCESSFUL",
                    String.format("Your payment from account number %s was successful. Deducted amount: %s.", accountNumber, amount)
            );
        }
        catch(Exception e){
            log.error("Error sending payment alert: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "payment.failed")
    public void consumePaymentFailed(
            @Payload Map<String, Object> payload
    )
    {
        try{
            String accountNumber = payload.get("accountNumber").toString();
            String amount = payload.get("amount").toString();

            //payment failed alert
            sendAlert(accountNumber,
                    "PAYMENT FAILED",
                    String.format("Your payment from account number %s was unsuccessful of amount: %s.", accountNumber, amount)
            );
        }
        catch(Exception e){
            log.error("Error sending failed payment alert: {}", e.getMessage());
        }
    }

    private void sendAlert(String accountNumber, String subject, String message) {

        log.info("---------------------------");
        log.info("Account: {}", accountNumber);
        log.info("Subject: {}", subject);
        log.info("Message: {}", message);
        log.info("---------------------------");
    }

}
