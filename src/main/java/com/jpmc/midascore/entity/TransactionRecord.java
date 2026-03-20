package com.jpmc.midascore.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_id", columnList = "transaction_id", unique = true)
})
public class TransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true)
    private String transactionId;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private UserRecord sender;

    @ManyToOne
    @JoinColumn(name = "recipient_id")
    private UserRecord recipient;

    private float amount;

    // NEW FIELD: incentive amount
    private float incentive;

    // Default constructor (required by JPA)
    public TransactionRecord() {
    }

    // Updated constructor to include transactionId and incentive
    public TransactionRecord(String transactionId, UserRecord sender, UserRecord recipient, float amount, float incentive) {
        this.transactionId = transactionId;
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
        this.incentive = incentive;
    }

    // Legacy constructor for backward compatibility
    public TransactionRecord(UserRecord sender, UserRecord recipient, float amount, float incentive) {
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
        this.incentive = incentive;
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

    public UserRecord getSender() {
        return sender;
    }

    public void setSender(UserRecord sender) {
        this.sender = sender;
    }

    public UserRecord getRecipient() {
        return recipient;
    }

    public void setRecipient(UserRecord recipient) {
        this.recipient = recipient;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    // NEW: Getter and Setter for incentive
    public float getIncentive() {
        return incentive;
    }

    public void setIncentive(float incentive) {
        this.incentive = incentive;
    }

    @Override
    public String toString() {
        return String.format("TransactionRecord[id=%d, transactionId=%s, sender=%s, recipient=%s, amount=%f, incentive=%f]",
                id, transactionId, sender != null ? sender.getName() : "null",
                recipient != null ? recipient.getName() : "null", amount, incentive);
    }
}
