package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 确保返回值是 Optional<User>，不要改成 User
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    Optional<User> findByToken(String token);
}