package com.jpmc.midascore.service;

import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.repository.UserRepository;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class TransactionService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRecordRepository transactionRecordRepository;

    @Autowired
    private IncentiveService incentiveService;

    @Transactional
    public void processTransaction(Transaction transaction) {
        // VALIDATION STEP 1: Check if sender exists (valid senderId)
        Optional<UserRecord> senderOpt = userRepository.findById(transaction.getSenderId());
        if (!senderOpt.isPresent()) {
            return; // Invalid - discard
        }

        // VALIDATION STEP 2: Check if recipient exists (valid recipientId)
        Optional<UserRecord> recipientOpt = userRepository.findById(transaction.getRecipientId());
        if (!recipientOpt.isPresent()) {
            return; // Invalid - discard
        }

        UserRecord sender = senderOpt.get();
        UserRecord recipient = recipientOpt.get();

        // VALIDATION STEP 3: Check if sender has sufficient balance
        if (sender.getBalance() < transaction.getAmount()) {
            return; // Invalid - discard
        }

        // ALL VALIDATIONS PASSED - Process the transaction

        // NEW: Call Incentive API to get incentive amount
        float incentiveAmount = incentiveService.getIncentiveAmount(transaction);

        // Update sender's balance (subtract transaction amount only)
        sender.setBalance(sender.getBalance() - transaction.getAmount());

        // Update recipient's balance (add transaction amount + incentive)
        recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentiveAmount);

        // Save updated user records to database
        userRepository.save(sender);
        userRepository.save(recipient);

        // Create and save transaction record with incentive
        TransactionRecord record = new TransactionRecord(sender, recipient, transaction.getAmount(), incentiveAmount);
        transactionRecordRepository.save(record);

        // Log for debugging
        System.out.println("Processed transaction: " + sender.getName() + " -> " +
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
