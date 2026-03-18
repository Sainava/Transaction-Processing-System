package com.jpmc.midascore.repository;

import com.jpmc.midascore.entity.UserRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserRecord, Long> {
    // Find user by their database ID
    Optional<UserRecord> findById(Long id);

    // Find user by their name (useful for debugging)
    Optional<UserRecord> findByName(String name);
}
