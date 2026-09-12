package com.stockpulse.stockpulse.repository;

import com.stockpulse.stockpulse.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByKeywordAndEmail(String keyword, String email);
    boolean existsByKeywordAndEmail(String keyword, String email);
    Optional<Stock> findByToken(String token);
    java.util.List<Stock> findAllByConfirmedTrue();
}