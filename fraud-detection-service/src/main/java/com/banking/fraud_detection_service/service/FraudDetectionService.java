package com.banking.fraud_detection_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionService {
    public void checkTransaction(Map<String, Object> payload) {
    }
}
