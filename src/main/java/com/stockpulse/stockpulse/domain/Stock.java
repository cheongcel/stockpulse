package com.stockpulse.stockpulse.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "stocks")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ticker; // AAPL, 005930 등

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String email; // 구독자 이메일

    public Stock(String ticker, String companyName, String email) {
        this.ticker = ticker;
        this.companyName = companyName;
        this.email = email;
    }
}