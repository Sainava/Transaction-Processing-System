package com.jpmc.midascore;

import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class TransactionListener {

    @Autowired
    private TransactionService transactionService;

    /**
     * Kafka listener for transaction events with idempotency protection.
     * 
     * This listener:
     * 1. Receives transaction events from Kafka
     * 2. Passes to service for idempotent processing
     * 3. Idempotency is enforced via transactionId + database lock
     * 4. Handles exceptions gracefully for observability
     */
    @KafkaListener(topics = "${general.kafka-topic}")
    public void listen(@Payload Transaction transaction) {
        
        String transactionId = transaction.getTransactionId() != null ? transaction.getTransactionId() : "UNKNOWN";
        
        // Log the incoming event
        System.out.println("[KAFKA_EVENT] Received transaction | transactionId=" + transactionId + 
                ", sender=" + transaction.getSenderId() + 
                ", recipient=" + transaction.getRecipientId() + 
                ", amount=" + transaction.getAmount());
        
        // Pass to service for idempotent processing
        try {
            transactionService.processTransaction(transaction);
        } catch (Exception e) {
            // Log handler exception for observability
            System.err.println("[KAFKA_ERROR] Error processing transaction | transactionId=" + transactionId + 
                    ", exception=" + e.getClass().getSimpleName() + 
                    ", message=" + e.getMessage());
            
            // Do not rethrow - let Kafka handle retry via rebalance or dead-letter topic
            // In production, you'd route to a dead-letter queue here
        }
    }
}
