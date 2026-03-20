package com.jpmc.midascore.foundation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Transaction {
    private String transactionId;  // Unique idempotency key (UUID)
    private long senderId;
    private long recipientId;
    private float amount;

    public Transaction() {
    }

    public Transaction(long senderId, long recipientId, float amount) {
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.amount = amount;
        this.transactionId = null;  // Will be set by producer or generated if missing
    }

    public Transaction(String transactionId, long senderId, long recipientId, float amount) {
        this.transactionId = transactionId;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.amount = amount;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public long getSenderId() {
        return senderId;
    }

    public void setSenderId(long senderId) {
        this.senderId = senderId;
    }

    public long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(long recipientId) {
        this.recipientId = recipientId;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "Transaction {transactionId=" + transactionId + ", senderId=" + senderId + ", recipientId=" + recipientId + ", amount=" + amount + "}";
    }
}
