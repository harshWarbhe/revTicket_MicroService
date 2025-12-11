package com.revticket.review.repository;

import com.revticket.review.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserRepository for accessing the shared users table in MySQL.
 * This matches the monolithic backend's pattern.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
}
