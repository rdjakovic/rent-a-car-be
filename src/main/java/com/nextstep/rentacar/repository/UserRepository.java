package com.nextstep.rentacar.repository;

import com.nextstep.rentacar.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    Optional<User> findByUsernameWithRoles(@Param("username") String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    /** Sign-in lookup: the username or the email, ignoring case ("John.Doe@Example.com" finds "john.doe@example.com") */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles"
            + " WHERE LOWER(u.username) = LOWER(:usernameOrEmail) OR LOWER(u.email) = LOWER(:usernameOrEmail)")
    Optional<User> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail, @Param("usernameOrEmail") String usernameOrEmail2);
}
