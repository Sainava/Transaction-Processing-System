package com.jpmc.midascore.service;

import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.foundation.Incentive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class IncentiveService {

    private static final String INCENTIVE_API_URL = "http://localhost:8080/incentive";

    @Autowired
    private RestTemplate restTemplate;

    public float getIncentiveAmount(Transaction transaction) {
        try {
            // POST the transaction to the incentive API
            Incentive incentive = restTemplate.postForObject(
                    INCENTIVE_API_URL,
                    transaction,
                    Incentive.class
            );

            // Return the incentive amount (should be >= 0)
            if (incentive != null) {
                System.out.println("Received incentive: " + incentive.getAmount() + " for transaction");
                return incentive.getAmount();
            }

            return 0f;
        } catch (Exception e) {
            System.err.println("Error calling incentive API: " + e.getMessage());
            return 0f; // If API fails, return 0 incentive
        }
    }
}
