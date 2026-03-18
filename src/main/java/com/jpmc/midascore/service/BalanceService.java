package com.jpmc.midascore.service;

import com.jpmc.midascore.foundation.Balance;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class BalanceService {

    @Autowired
    private UserRepository userRepository;

    public Balance getBalanceByUserId(String userId) {
        // Try to find user by name (userId is the user's name)
        Optional<UserRecord> userOpt = userRepository.findByName(userId);

        if (userOpt.isPresent()) {
            // User exists - return their balance
            UserRecord user = userOpt.get();
            return new Balance(user.getBalance());
        } else {
            // User doesn't exist - return balance of 0
            return new Balance(0f);
        }
    }
}
