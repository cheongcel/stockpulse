package com.stockpulse.stockpulse.repository;

import com.stockpulse.stockpulse.domain.NewsAnalysis;
import com.stockpulse.stockpulse.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NewsAnalysisRepository extends JpaRepository<NewsAnalysis, Long> {
    List<NewsAnalysis> findByStockOrderByPublishedAtDesc(Stock stock);
    List<NewsAnalysis> findTop5ByStockOrderByPublishedAtDesc(Stock stock);
}