package com.healthsuite.auth.repository;

import com.healthsuite.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    /** roles is EAGER @ManyToMany — join-fetch it here so listing users is one query instead of N+1. */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles ORDER BY u.createdAt DESC")
    List<User> findAllWithRoles();
}
