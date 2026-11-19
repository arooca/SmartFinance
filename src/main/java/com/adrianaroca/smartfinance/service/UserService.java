package com.adrianaroca.smartfinance.service;

import com.adrianaroca.smartfinance.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    User save(User user);

    List<User> findAll();

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    void delete(Long id);

    User getCurrentUser();

    void updateProfile(
            User user,
            String name,
            String email,
            String newPassword
    );

    void deleteCurrentUser();
}