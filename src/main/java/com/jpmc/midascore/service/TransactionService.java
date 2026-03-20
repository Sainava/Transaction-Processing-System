package com.jpmc.midascore.service;

import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.ProcessedEventId;
import com.jpmc.midascore.repository.UserRepository;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.ProcessedEventIdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRecordRepository transactionRecordRepository;

    @Autowired
    private ProcessedEventIdRepository processedEventIdRepository;

    @Autowired
    private IncentiveService incentiveService;

    /**
     * Main transaction processing method with idempotency and replay protection.
     * 
     * Idempotency Key Strategy:
     * 1. If transactionId is present and valid, use it as idempotency key
     * 2. If missing, generate a new UUID (for backward compatibility)
     * 3. Lock the event ID in DB before processing
     * 4. If lock fails (duplicate), return immediately (idempotent)
     * 5. On success, mark as COMPLETED
     * 6. On failure, mark as FAILED for manual review
     */
    @Transactional
    public void processTransaction(Transaction transaction) {
        // STEP 1: Validate or generate transactionId
        String transactionId = sanitizeTransactionId(transaction.getTransactionId());
        
        if (transactionId == null || transactionId.trim().isEmpty()) {
            transactionId = UUID.randomUUID().toString();
            System.out.println("Generated new transactionId for backward compatibility: " + transactionId);
        }

        // STEP 2: Attempt to acquire idempotency lock in DB
        Optional<ProcessedEventId> existingEvent = processedEventIdRepository.findByTransactionId(transactionId);
        
        if (existingEvent.isPresent()) {
            ProcessedEventId event = existingEvent.get();
            
            if (event.getStatus() == ProcessedEventId.ProcessingStatus.COMPLETED) {
                // This is a replay/duplicate of an already-processed transaction
                System.out.println("[IDEMPOTENCY] Transaction replay detected for transactionId=" + transactionId + 
                    ". Status: COMPLETED. Ignoring duplicate.");
                return;  // Idempotent return - no state change
            }
            
            if (event.getStatus() == ProcessedEventId.ProcessingStatus.PROCESSING) {
                // In-flight duplicate (concurrent delivery of same event)
                System.out.println("[IDEMPOTENCY] Concurrent duplicate detected for transactionId=" + transactionId + 
                    ". Status: PROCESSING. Ignoring.");
                return;  // Idempotent return - wait for first instance to complete
            }
            
            if (event.getStatus() == ProcessedEventId.ProcessingStatus.FAILED) {
                // Previously failed transaction
                System.out.println("[IDEMPOTENCY] Transaction previously failed for transactionId=" + transactionId + 
                    ". Error: " + event.getErrorMessage() + ". Blocking reprocessing.");
                return;  // Do not reprocess failed transactions (explicit policy)
            }
        }

        // STEP 3: Create new processing lock (atomic operation via DB unique constraint)
        ProcessedEventId processingLock = new ProcessedEventId(transactionId);
        
        try {
            processedEventIdRepository.save(processingLock);
            System.out.println("[IDEMPOTENCY] Acquired processing lock for transactionId=" + transactionId);
        } catch (DataIntegrityViolationException e) {
            // Unique constraint violation - another thread/instance is processing this event
            System.out.println("[IDEMPOTENCY] Lock acquisition failed (unique constraint) for transactionId=" + transactionId + 
                ". Another instance may be processing. Exception: " + e.getMessage());
            return;  // Idempotent return
        }

        // STEP 4: Proceed with business logic only if lock was successfully acquired
        try {
            performTransactionValidationAndProcessing(transaction, transactionId);
            
            // STEP 5a: Mark processing as COMPLETED
            processingLock.setStatus(ProcessedEventId.ProcessingStatus.COMPLETED);
            processedEventIdRepository.save(processingLock);
            System.out.println("[SUCCESS] Transaction completed and marked idempotent for transactionId=" + transactionId);
            
        } catch (Exception e) {
            // STEP 5b: On any error, mark as FAILED for observability
            System.err.println("[ERROR] Transaction processing failed for transactionId=" + transactionId + 
                ". Error: " + e.getMessage());
            processingLock.setStatus(ProcessedEventId.ProcessingStatus.FAILED);
            processingLock.setErrorMessage(e.getMessage());
            processedEventIdRepository.save(processingLock);
            
            // Rethrow to let caller handle and potentially retry
            throw e;
        }
    }

    /**
     * Sanitize and validate the transactionId from the incoming event.
     * Returns null if invalid format.
     */
    private String sanitizeTransactionId(String transactionId) {
        if (transactionId == null) {
            return null;
        }
        
        String trimmed = transactionId.trim();
        
        // Validate format: should be UUID-like or alphanumeric string
        if (trimmed.length() > 255) {
            System.err.println("[WARNING] transactionId exceeds max length. Truncating.");
            return trimmed.substring(0, 255);
        }
        
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Core business logic: validate and process the transaction.
     * This is separated so it only runs if idempotency lock is acquired.
     */
    @Transactional
    private void performTransactionValidationAndProcessing(Transaction transaction, String transactionId) {
        // VALIDATION STEP 1: Check if sender exists (valid senderId)
        Optional<UserRecord> senderOpt = userRepository.findById(transaction.getSenderId());
        if (!senderOpt.isPresent()) {
            System.out.println("[VALIDATION FAILED] Sender does not exist. senderId=" + transaction.getSenderId());
            return;  // Invalid - discard
        }

        // VALIDATION STEP 2: Check if recipient exists (valid recipientId)
        Optional<UserRecord> recipientOpt = userRepository.findById(transaction.getRecipientId());
        if (!recipientOpt.isPresent()) {
            System.out.println("[VALIDATION FAILED] Recipient does not exist. recipientId=" + transaction.getRecipientId());
            return;  // Invalid - discard
        }

        UserRecord sender = senderOpt.get();
        UserRecord recipient = recipientOpt.get();

        // VALIDATION STEP 3: Check if sender has sufficient balance
        if (sender.getBalance() < transaction.getAmount()) {
            System.out.println("[VALIDATION FAILED] Insufficient balance. sender=" + sender.getName() + 
                ", balance=" + sender.getBalance() + ", amount=" + transaction.getAmount());
            return;  // Invalid - discard
        }

        // ALL VALIDATIONS PASSED - Process the transaction

        // Call Incentive API to get incentive amount
        float incentiveAmount = incentiveService.getIncentiveAmount(transaction);

        // Update sender's balance (subtract transaction amount only)
        sender.setBalance(sender.getBalance() - transaction.getAmount());

        // Update recipient's balance (add transaction amount + incentive)
        recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentiveAmount);

        // Save updated user records to database
        userRepository.save(sender);
        userRepository.save(recipient);

        // Create and save transaction record with transactionId and incentive
        TransactionRecord record = new TransactionRecord(transactionId, sender, recipient, transaction.getAmount(), incentiveAmount);
        transactionRecordRepository.save(record);

        // Log for debugging
        System.out.println("Processed transaction: transactionId=" + transactionId + ", " + sender.getName() + " -> " +
                recipient.getName() + " Amount: " + transaction.getAmount() +
                " Incentive: " + incentiveAmount);

        // Debug wilbur's balance
        if (recipient.getName().equals("wilbur") || sender.getName().equals("wilbur")) {
            Optional<UserRecord> wilburOpt = userRepository.findByName("wilbur");
            if (wilburOpt.isPresent()) {
                System.out.println("WILBUR balance after transaction: " + wilburOpt.get().getBalance());
            }
        }
    }
}
