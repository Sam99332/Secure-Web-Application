package com.secureauth.repository;

import com.secureauth.entity.LoginHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    List<LoginHistory> findByUserIdOrderByLoginTimeDesc(Long userId, Pageable pageable);
    List<LoginHistory> findAllByOrderByLoginTimeDesc(Pageable pageable);
}
