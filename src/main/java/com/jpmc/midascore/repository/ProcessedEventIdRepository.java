package com.jpmc.midascore.repository;

import com.jpmc.midascore.entity.ProcessedEventId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ProcessedEventIdRepository extends JpaRepository<ProcessedEventId, Long> {
    /**
     * Find a processed event by transaction ID.
     * @param transactionId the unique transaction identifier
     * @return Optional containing the ProcessedEventId if found
     */
    Optional<ProcessedEventId> findByTransactionId(String transactionId);

    /**
     * Check if a transaction ID has already been processed.
     * @param transactionId the unique transaction identifier
     * @return true if the transaction has been seen before
     */
    boolean existsByTransactionId(String transactionId);
}
