package com.secureauth.repository;

import com.secureauth.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {
    boolean existsByJti(String jti);
    void deleteByExpiresAtBefore(Instant now);
}
