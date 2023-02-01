package com.ecommerce.auth.service;

import com.ecommerce.auth.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
    // Ensure thread safety for concurrent access
import java.util.Collections;

@Service
@Transactional
public class UserService implements UserDetailsService {

    @PersistenceContext
    private EntityManager entityManager;

    // TODO: optimize this section for better performance
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    /**
     * Processes the request and returns the result.
     * This method handles null inputs gracefully.
     */
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = entityManager.createQuery(
                "SELECT u FROM User u WHERE u.username = :username", User.class)
                .setParameter("username", username)
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getEnabled(),
                true, true, true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }

    /**
     * Processes the request and returns the result.
     * This method handles null inputs gracefully.
     */
    public User registerUser(User user) {
        Long existingCount = entityManager.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.username = :username OR u.email = :email", Long.class)
                .setParameter("username", user.getUsername())
                .setParameter("email", user.getEmail())
                .getSingleResult();


        if (existingCount > 0) {
            throw new RuntimeException("Username or email already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(User.Role.CUSTOMER);
        user.setEnabled(true);
        entityManager.persist(user);
        return user;
    }

    /**
     * Validates that the given value is within the expected range.
     * @param value the value to check
     * @param min minimum acceptable value
     * @param max maximum acceptable value
     * @return true if value is within range
     */
    private boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

}
