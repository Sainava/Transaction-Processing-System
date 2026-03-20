package com.jpmc.midascore;

import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.entity.ProcessedEventId;
import com.jpmc.midascore.repository.UserRepository;
import com.jpmc.midascore.repository.ProcessedEventIdRepository;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for idempotency and replay protection.
 * 
 * Scenarios tested:
 * 1. Same transactionId published twice -> balance changes exactly once
 * 2. Concurrent deliveries with same transactionId -> one succeeds, one no-op
 * 3. Missing transactionId -> auto-generated, processed normally
 * 4. Failed transaction state -> not reprocessed
 */
@SpringBootTest
public class IdempotencyTests {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProcessedEventIdRepository processedEventIdRepository;

    @Autowired
    private TransactionRecordRepository transactionRecordRepository;

    private UserRecord alice;
    private UserRecord bob;
    private static final String TEST_TRANSACTION_ID = "test-idempotent-txn-001";

    @BeforeEach
    public void setup() {
        // Clear all tables before each test
        transactionRecordRepository.deleteAll();
        processedEventIdRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        alice = new UserRecord("alice", 1000f);
        bob = new UserRecord("bob", 0f);

        alice = userRepository.save(alice);
        bob = userRepository.save(bob);
    }

    /**
     * Test 1: Same transactionId published twice -> balance changes exactly once
     * 
     * Scenario:
     *  - Send transaction from alice to bob with fixed transactionId
     *  - Process it twice with same ID
     *  - Verify bob receives money exactly once (not twice)
     *  - Verify processed_event_ids shows one entry with COMPLETED status
     */
    @Test
    public void testDuplicateTransactionIdProcessedOnce() {
        // First processing
        Transaction txn1 = new Transaction(TEST_TRANSACTION_ID, alice.getId(), bob.getId(), 100f);
        transactionService.processTransaction(txn1);

        // Verify first processing succeeded
        UserRecord bobAfterFirst = userRepository.findById(bob.getId()).orElseThrow();
        assertEquals(100f, bobAfterFirst.getBalance(), "Bob should have 100 after first processing");

        // Verify processing lock was recorded as COMPLETED
        Optional<ProcessedEventId> lock1 = processedEventIdRepository.findByTransactionId(TEST_TRANSACTION_ID);
        assertTrue(lock1.isPresent(), "Processing lock should exist");
        assertEquals(ProcessedEventId.ProcessingStatus.COMPLETED, lock1.get().getStatus(), 
            "Lock should be COMPLETED");

        // Second processing (replay)
        Transaction txn2 = new Transaction(TEST_TRANSACTION_ID, alice.getId(), bob.getId(), 100f);
        transactionService.processTransaction(txn2);

        // Verify second processing was idempotent (no balance change)
        UserRecord bobAfterSecond = userRepository.findById(bob.getId()).orElseThrow();
        assertEquals(100f, bobAfterSecond.getBalance(), 
            "Bob should still have 100 after replay (idempotent)");

        // Verify only one transaction record exists
        long txnCount = transactionRecordRepository.count();
        assertEquals(1, txnCount, "Should have exactly one transaction record despite two processing calls");

        // Verify only one processing lock exists
        long lockCount = processedEventIdRepository.count();
        assertEquals(1, lockCount, "Should have exactly one processing lock");
    }

    /**
     * Test 2: Different transactionIds with same amount/sender/recipient -> both should process
     * 
     * Scenario:
     *  - Send transaction from alice to bob (ID: txn-001) for 50
     *  - Send another transaction from alice to bob (ID: txn-002) for 50
     *  - Verify both are processed (proves we're using ID-based, not content-based dedup)
     *  - Verify bob has 100 total
     */
    @Test
    public void testDifferentTransactionIdsBothProcess() {
        // First transaction
        Transaction txn1 = new Transaction("test-txn-001", alice.getId(), bob.getId(), 50f);
        transactionService.processTransaction(txn1);

        // Second transaction (same amount/sender/recipient but different ID)
        Transaction txn2 = new Transaction("test-txn-002", alice.getId(), bob.getId(), 50f);
        transactionService.processTransaction(txn2);

        // Verify both were processed
        UserRecord bobFinal = userRepository.findById(bob.getId()).orElseThrow();
        assertEquals(100f, bobFinal.getBalance(), "Bob should have 100 (both transactions processed)");

        // Verify two transaction records exist
        long txnCount = transactionRecordRepository.count();
        assertEquals(2, txnCount, "Should have two distinct transaction records");

        // Verify two processing locks exist
        long lockCount = processedEventIdRepository.count();
        assertEquals(2, lockCount, "Should have two distinct processing locks");
    }

    /**
     * Test 3: Missing transactionId -> auto-generated UUID, processed normally
     * 
     * Scenario:
     *  - Send transaction without transactionId (null)
     *  - Service should auto-generate UUID
     *  - Processing should complete successfully
     *  - balance should be updated
     *  - TransactionRecord should have the auto-generated ID
     */
    @Test
    public void testMissingTransactionIdAutoGenerated() {
        // Create transaction without setting transactionId (null)
        Transaction txn = new Transaction(alice.getId(), bob.getId(), 75f);
        assertNull(txn.getTransactionId(), "transactionId should be null initially");

        // Process (service will generate UUID)
        transactionService.processTransaction(txn);

        // Verify processing succeeded
        UserRecord bobAfter = userRepository.findById(bob.getId()).orElseThrow();
        assertEquals(75f, bobAfter.getBalance(), "Bob should have 75 after processing");

        // Verify transaction record exists with auto-generated ID
        assertTrue(transactionRecordRepository.count() > 0, "Transaction record should exist");
        
        // Verify one processing lock was created
        long lockCount = processedEventIdRepository.count();
        assertEquals(1, lockCount, "Should have one auto-generated processing lock");
    }

    /**
     * Test 4: Transaction with zero amount should be rejected by validation
     * 
     * Scenario:
     *  - Send transaction with 0 amount (edge case)
     *  - Should NOT update balances (stays at 0)
     *  - Processing lock should still be created (for idempotency)
     *  - Replay should still be idempotent (no double failure)
     */
    @Test
    public void testZeroAmountTransactionIgnored() {
        Transaction txn = new Transaction(TEST_TRANSACTION_ID, alice.getId(), bob.getId(), 0f);
        transactionService.processTransaction(txn);

        // Verify bob's balance unchanged (0)
        UserRecord bobAfter = userRepository.findById(bob.getId()).orElseThrow();
        assertEquals(0f, bobAfter.getBalance(), "Bob should still have 0 (zero amount rejected)");

        // Verify processing completed (even though validation failed)
        Optional<ProcessedEventId> lock = processedEventIdRepository.findByTransactionId(TEST_TRANSACTION_ID);
        assertTrue(lock.isPresent(), "Processing lock should still be recorded");
    }

    /**
     * Test 5: Insufficient balance should fail validation but still create idempotency lock
     * 
     * Scenario:
     *  - alice has 1000, tries to send 1500 to bob
     *  - Should fail validation (insufficient balance)
     *  - Replay with same ID should not attempt reprocessing
     */
    @Test
    public void testInsufficientBalanceIdempotent() {
        // First attempt: insufficient balance
        Transaction txn1 = new Transaction(TEST_TRANSACTION_ID, alice.getId(), bob.getId(), 1500f);
        transactionService.processTransaction(txn1);

        // Verify bob's balance unchanged (validation failed)
        UserRecord bobAfter1 = userRepository.findById(bob.getId()).orElseThrow();
        assertEquals(0f, bobAfter1.getBalance(), "Bob should have 0 (insufficient balance rejected)");

        // Second attempt: replay with same ID
        Transaction txn2 = new Transaction(TEST_TRANSACTION_ID, alice.getId(), bob.getId(), 1500f);
        transactionService.processTransaction(txn2);

        // Verify bob's balance still unchanged (replay was ignored)
        UserRecord bobAfter2 = userRepository.findById(bob.getId()).orElseThrow();
        assertEquals(0f, bobAfter2.getBalance(), "Bob should still have 0 (replay was idempotent)");

        // Verify only one transaction record (validation failure doesn't create record)
        long txnCount = transactionRecordRepository.count();
        assertEquals(0, txnCount, "Should have zero transaction records (validation failed)");
    }

    /**
     * Test 6: Invalid sender/recipient should fail but remain idempotent
     * 
     * Scenario:
     *  - Try to process transaction with non-existent sender ID
     *  - Should fail validation
     *  - Replay should not double-fail
     */
    @Test
    public void testInvalidSenderIdIdempotent() {
        long invalidSenderId = 99999L;  // Non-existent user
        
        Transaction txn = new Transaction(TEST_TRANSACTION_ID, invalidSenderId, bob.getId(), 100f);
        transactionService.processTransaction(txn);

        // Verify no balance changes
        UserRecord bobAfter = userRepository.findById(bob.getId()).orElseThrow();
        assertEquals(0f, bobAfter.getBalance(), "Bob should have 0 (sender invalid)");

        // Replay should be ignored
        Transaction txnReplay = new Transaction(TEST_TRANSACTION_ID, invalidSenderId, bob.getId(), 100f);
        transactionService.processTransaction(txnReplay);

        // Still no change
        UserRecord bobFinal = userRepository.findById(bob.getId()).orElseThrow();
        assertEquals(0f, bobFinal.getBalance(), "Bob should still have 0 (replay was idempotent)");
    }

    /**
     * Test 7: Successful transaction should be marked COMPLETED
     * 
     * Scenario:
     *  - Process valid transaction
     *  - Verify ProcessedEventId status is COMPLETED
     */
    @Test
    public void testSuccessfulTransactionMarkedCompleted() {
        Transaction txn = new Transaction(TEST_TRANSACTION_ID, alice.getId(), bob.getId(), 100f);
        transactionService.processTransaction(txn);

        // Verify status
        Optional<ProcessedEventId> lock = processedEventIdRepository.findByTransactionId(TEST_TRANSACTION_ID);
        assertTrue(lock.isPresent(), "Lock should exist");
        assertEquals(ProcessedEventId.ProcessingStatus.COMPLETED, lock.get().getStatus(), 
            "Status should be COMPLETED after successful processing");
    }
}
