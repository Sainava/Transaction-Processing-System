package com.jpmc.midascore.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "processed_event_ids", indexes = {
    @Index(name = "idx_transaction_id", columnList = "transaction_id", unique = true)
})
public class ProcessedEventId {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private String transactionId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProcessingStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "error_message")
    private String errorMessage;

    // Enum for processing status
    public enum ProcessingStatus {
        PROCESSING,
        COMPLETED,
        FAILED
    }

    // Default constructor (required by JPA)
    public ProcessedEventId() {
    }

    // Constructor for new processing event
    public ProcessedEventId(String transactionId) {
        this.transactionId = transactionId;
        this.status = ProcessingStatus.PROCESSING;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public ProcessingStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessingStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return String.format("ProcessedEventId[id=%d, transactionId=%s, status=%s, createdAt=%s, updatedAt=%s]",
                id, transactionId, status, createdAt, updatedAt);
    }
}
