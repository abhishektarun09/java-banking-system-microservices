package com.banking.transaction_service.entiity;

// Pending -> Processing -> Completed (clean tnx)
//                       -> Pending Verification (suspicious detected)
//                                  -> Completed (verified)
//                                  -> Flagged (saga refund)
//                       -> Failed
//                       -> Flagged

public enum TransactionStatus {
    PENDING,
    PROCESSING,
    PENDING_VERIFICATION,
    COMPLETED,
    FAILED,
    FLAGGED
}
