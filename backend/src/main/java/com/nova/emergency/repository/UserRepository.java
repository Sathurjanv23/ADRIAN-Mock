package com.nova.emergency.repository;

import com.nova.emergency.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    /**
     * Returns all active, verified users with the given role.
     * Used to find rescue_team / hospital recipients for emergency notifications.
     */
    List<User> findByRoleAndIsActiveTrue(String role);

    /**
     * Returns all users with the given role regardless of active status.
     */
    List<User> findByRole(String role);
}

