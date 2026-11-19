package com.adrianaroca.smartfinance.repository;

import com.adrianaroca.smartfinance.model.EmailActionToken;
import com.adrianaroca.smartfinance.model.EmailActionType;
import com.adrianaroca.smartfinance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailActionTokenRepository extends JpaRepository<EmailActionToken, Long> {
    Optional<EmailActionToken> findByTokenHashAndType(String tokenHash, EmailActionType type);
    void deleteAllByUserAndType(User user, EmailActionType type);
}
